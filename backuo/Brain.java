import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Brain implements Runnable {

    
    public enum Strategy {

        PRE_KICK_OFF_POSITION,
        PRE_KICK_OFF_ANGLE,
        DRIBBLE_KICK,
        ACTIVE_INTERCEPT,
        DASH_TOWARDS_BALL_AND_KICK,
        LOOK_AROUND,
        GET_BETWEEN_BALL_AND_GOAL,
        PRE_FREE_KICK_POSITION,
        PRE_CORNER_KICK_POSITION,
        WING_POSITION,
        CLEAR_BALL,
        RUN_TO_STARTING_POSITION
    }

    public enum EstadoJogador {

        ESTOU_MARCADO,
        ESTOU_LIVRE,
        SEM_A_BOLA,
        MAIS_PROX_DA_BOLA,
        BOLA_FORA_DA_VISAO,
        BOLA_PARADA
    }

    public enum EstadoDoGoleiro {

        BOLA_NA_AREA_DE_CAPTURA_DENTRO_DA_AREA,
        BOLA_NA_AREA_DE_CAPTURA_FORA_DA_AREA,
        FORA_DE_POSICAO,
        MAIS_PROXIMO_DA_BOLA_NA_AREA,
        BOLA_NA_INTERMEDIARIA,
        BOLA_FORA_DE_VISAO,
        BOLA_PARADA         //representa estados que o goleiro pode usar a função move

    }

    ///////////////////////////////////////////////////////////////////////////
    // MEMBER VARIABLES
    ///////////////////////////////////////////////////////////////////////////    
    Client client;
    Player player;
    public int time;
    public int cicloUltimaVisao;
    public int cicloUltimaSenseInfo;
    public PlayerRole.Role role;
    public Point pontoDePosicionamento = null;
    public int timeUltimoProcuraBola = 0;

    // Self info & Play mode
    private String playMode;
    private int multiplicadorDoAngulo = 1;
    private SenseInfo curSenseInfo, lastSenseInfo;
    public VetorDeAceleracao acceleration;
    public VelocityVector velocity;
    private boolean isPositioned = false;
    private EstadoDoGoleiro estadoAtualDoGoleiro = EstadoDoGoleiro.BOLA_FORA_DE_VISAO;
    private EstadoJogador estadoAtualDoJogador = EstadoJogador.SEM_A_BOLA;
    private boolean repositioning = false;
    HashMap<String, FieldObject> fieldObjects = new HashMap<>(100);
    ArrayDeque<String> hearMessages = new ArrayDeque<>();
    LinkedList<Player> lastSeenOpponents = new LinkedList<>();
    LinkedList<Player> companheirosVisiveis = new LinkedList<>(); // variavel que guarda todos os companheiros visiveis para o jogador no ultimo ciclo
    LinkedList<Settings.RESPONSE> responseHistory = new LinkedList<>();
    private long timeLastSee = 0;
    private long timeLastSenseBody = 0;
    private int lastRan = -1;
    private double neckAngle = 0;
    private int noSeeBallCount = 0;
    private final int noSeeBallCountMax = 45;
    private MiniLogger logger;
    private Strategy currentStrategy = Strategy.LOOK_AROUND;
    private boolean updateStrategy = true;
    private Formacao formacao;
    

    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    ///////////////////////////////////////////////////////////////////////////
    /**
     * This is the primary constructor for the Brain class.
     *
     * @param player a back-reference to the invoking player
     * @param client the server client by which to send commands, etc.
     */
    public Brain(Player player, Client client) {
        this.player = player;
        this.client = client;
        this.curSenseInfo = new SenseInfo();
        this.lastSenseInfo = new SenseInfo();
        this.velocity = new VelocityVector();
        this.acceleration = new VetorDeAceleracao();
        // Load the HashMap
        for (int i = 0; i < Settings.STATIONARY_OBJECTS.length; i++) {
            StationaryObject object = Settings.STATIONARY_OBJECTS[i];
            //client.log(Log.DEBUG, String.format("Adding %s to my HashMap...", object.id));
            fieldObjects.put(object.id, object);
        }
        // Load the response history
        this.responseHistory.add(Settings.RESPONSE.NONE);
        this.responseHistory.add(Settings.RESPONSE.NONE);
        this.formacao = new Formacao();
    }

    ///////////////////////////////////////////////////////////////////////////
    // GAME LOGIC
    ///////////////////////////////////////////////////////////////////////////        
    /**
     * Returns the direction, in radians, of the player at the current time.
     */
    private final double dir() {
        return Math.toRadians(this.player.direction.getDirection());
    }

    /**
     * Assesses the utility of a strategy for the current time step.
     *
     * @param strategy the strategy to assess the utility of
     * @return an assessment of the strategy's utility in the range [0.0, 1.0]
     */
    private final double assessUtility(Strategy strategy) {
        FieldObject ball = this.getOrCreate(Ball.ID);
        double utility = 0;
        switch (strategy) {
            case PRE_FREE_KICK_POSITION:
            case PRE_CORNER_KICK_POSITION:
            case PRE_KICK_OFF_POSITION:
                // Check play mode and reposition as necessary.
                if (this.canUseMove()) {
                    utility = 1 - (isPositioned ? 1 : 0);
                }
                break;
            case PRE_KICK_OFF_ANGLE:
                if (this.isPositioned) {
                    utility = this.player.team.side == 'r'
                            ? (this.canSee(Ball.ID) ? 0.0 : 1.0) : 0.0;
                }
                break;
            case WING_POSITION:
                if (PlayerRole.isWing(this.role)) {
                    // A wing should use this strategy if another player on the wing's team
                    // is closer to the ball, or something like that.
                    utility = 0.70;
                }
                break;
            case DRIBBLE_KICK:
                Rectangle OPP_PENALTY_AREA = (this.player.team.side == 'l')
                        ? Settings.PENALTY_AREA_RIGHT : Settings.PENALTY_AREA_LEFT;
                // If the agent is a goalie, don't dribble!
                // If we're in the opponent's strike zone, don't dribble! Go for score!
                if (this.role == PlayerRole.Role.GOLEIRO || this.player.inRectangle(OPP_PENALTY_AREA)) {
                    utility = 0.0;
                } else {
                    utility = (this.canKickBall() && this.canSee(
                            this.player.getOpponentGoalId()))
                            ? 0.95 : 0.0;
                }
                break;
            case DASH_TOWARDS_BALL_AND_KICK:
                // The striker(s) should usually execute this strategy.
                // The wings, mid-fielders and defenders should generally execute this strategy when
                // they are close to the ball.
                if (ball.position.getPosition().isUnknown() || this.role == PlayerRole.Role.GOLEIRO) {
                    utility = 0.0;
                } else if (this.role == PlayerRole.Role.ZAGUEIRO_DIREITO) {
                    utility = 0.97;
                } else {
                    // Utility is high if the player is within ~ 5.0 meters of the ball
                    utility = Math.min(0.98, Math.pow(this.getOrCreate(Ball.ID).curInfo.distance / 10.0, -1.0));
                }
                break;
            case LOOK_AROUND:
                // This strategy is almost never necessary. It's used to re-orient players that have
                // become very confused about where they are. This might happen if they are at the edge
                // of the physical boundary, looking out.
                if (this.player.position.getPosition().isUnknown() || this.getOrCreate(Ball.ID).position.getPosition().isUnknown()) {
                    utility = 0.98;
                } else {
                    double playerPosConf = this.player.position.getConfidence(this.time);
                    double ballPosConf = this.getOrCreate(Ball.ID).position.getConfidence(this.time) / 2.0;
                    double overallConf = Math.min(playerPosConf, ballPosConf);
                    if (overallConf > 0.05) {
                        utility = 0.10;
                    } else {
                        utility = 1.0 - overallConf;
                    }
                }
                break;
            case GET_BETWEEN_BALL_AND_GOAL:
                // The sweeper(s) should usually execute this strategy. The goalie should also usually
                // execute this strategy, though it's implementation may be slightly different.
                if (this.role == PlayerRole.Role.ATACANTE_DIREITO || this.role == PlayerRole.Role.GOLEIRO) {
                    if (this.player.distanceTo(ball) > 5.0) {
                        utility = 0.97;
                    } else {
                        utility = 0.45;
                    }
                } else {
                    utility = 0.4;
                }

                break;
            case CLEAR_BALL:
                // Defenders should do this if the ball is in their penalty area.
                if (this.ownPenaltyArea().contains(ball)) {
                    return 0.99;
                } else {
                    utility = 0.45;
                }
                break;
            case RUN_TO_STARTING_POSITION:
                utility = noSeeBallCount / (noSeeBallCountMax + 2);
                break;
            default:
                utility = 0;
                break;
        }

        return utility;

    }

    /**
     * Checks if the play mode allows Move commands.
     *
     * @return true if move commands can be issued
     */
    private final boolean canUseMove() {
        return (playMode.equals("before_kick_off")
                || playMode.startsWith("goal_r_")
                || playMode.startsWith("goal_l_")
                || playMode.startsWith("free_kick_")
                || playMode.startsWith("corner_kick_"));
    }

    private boolean bolaRolando() {
        return playMode.equals("play_on");
    }

    /**
     * Returns an estimate of whether the player can kick the ball, dependent on
     * its distance to the ball and whether it is inside the playing field.
     *
     * @return true if the player is on the field and within kicking distance
     */
    public final boolean canKickBall() {
        FieldObject ball = this.getOrCreate(Ball.ID);
        return this.player.inRectangle(Settings.FIELD) && ball.curInfo.time >= this.time - 1
                && ball.curInfo.distance < Futil.kickable_radius();
    }

    /**
     * Returns an indication of whether a given ObjectId was seen in the current
     * time step.
     *
     * @return true if the given ObjectId was seen in the current soccer server
     * time step
     */
    public final boolean canSee(String id) {
        return this.getOrCreate(id).curInfo.time == this.time;
    }

    /**
     * Accelerates the player in the direction of its body.
     *
     * @param power the power of the acceleration (0 to 100)
     */
    private final void dash(double power) {
        // Update this player's acceleration
        
        this.acceleration.addPolar(this.dir(), this.effort());
        escreverNoLog("dash " + power);
        this.client.sendCommand(Settings.Commands.DASH, Double.toString(power));
    }

    /**
     * Accelerates the player in the direction of its body, offset by the given
     * angle.
     *
     * @param power the power of the acceleration (0 to 100)
     * @param offset an offset to be applied to the player's direction, yielding
     * the direction of acceleration
     */
    public final void dash(double power, double offset) {
        this.acceleration.addPolar(this.dir() + offset, this.edp(power));
        client.sendCommand(Settings.Commands.DASH, Double.toString(power), Double.toString(offset));
    }

    /**
     * Determines the strategy with the current highest utility.
     *
     * @return the strategy this brain thinks is optimal for the current time
     * step
     */
    private final Strategy determineOptimalStrategy() {
        Strategy optimalStrategy = this.currentStrategy;
        double bestUtility = 0;
        if (this.updateStrategy) {
            for (Strategy strategy : Strategy.values()) {
                double utility = this.assessUtility(strategy);
                if (utility > bestUtility) {
                    bestUtility = utility;
                    optimalStrategy = strategy;
                }
            }
        }
        Log.d("Picked strategy " + optimalStrategy + " with utility " + bestUtility);
        return optimalStrategy;
    }

    /**
     * Returns this player's effective dash power. Refer to the soccer server
     * manual for more information.
     *
     * @return this player's effective dash power
     */
    private final double edp(double power) {
        return this.effort() * Settings.DASH_POWER_RATE * power;
    }

    /**
     * Returns an effort value for this player. If one wasn't received this time
     * step, we guess.
     */
    private final double effort() {
        return this.curSenseInfo.effort;
    }

    /**
     * Executes a strategy for the player in the current time step.
     *
     * @param strategy the strategy to execute
     */
    private final void executeStrategy(Strategy strategy) {
        FieldObject ball = this.getOrCreate(Ball.ID);
        FieldObject opponentGoal = this.getOrCreate(this.player.getOpponentGoalId());
        FieldObject ownGoal = this.ownGoal();

        switch (strategy) {
            case PRE_FREE_KICK_POSITION:
                if (playMode.equals("free_kick_l")) {
                    this.move(Settings.FREE_KICK_L_FORMATION[player.number]);
                } else {
                    this.move(Settings.FREE_KICK_R_FORMATION[player.number]);
                }
                this.isPositioned = true;
                break;
            case PRE_CORNER_KICK_POSITION:
                if (playMode.equals("corner_kick_l")) {
                    this.move(Settings.CORNER_KICK_L_FORMATION[player.number]);
                } else {
                    this.move(Settings.CORNER_KICK_R_FORMATION[player.number]);
                }
                this.isPositioned = true;
                break;
            case PRE_KICK_OFF_POSITION:
                this.move(Settings.FORMATION[player.number]);
                this.isPositioned = true;
                break;
            case PRE_KICK_OFF_ANGLE:
                this.turn(30);
                break;
            case WING_POSITION:
                Point position = Futil.estimatePositionOf(ball, 3, this.time).getPosition();
                if (this.role == PlayerRole.Role.LATERAL_DIREITO) {
                    position.update(position.getX(), position.getY() + 4.0);
                } else {
                    position.update(position.getX(), position.getY() - 4.0);
                }
                this.dashTo(position);
                break;
            case DRIBBLE_KICK:
                /*
                 *  Find a dribble angle, weighted by presence of opponents.
                 *  Determine dribble velocity based on current velocity.
                 *  Dribble!
                 */

                // Predict next position:
                Vector2D v_new = Futil.estimatePositionOf(this.player, 2, this.time).getPosition().asVector();
                Vector2D v_target = v_new.add(findDribbleAngle());
                Vector2D v_ball = v_target.add(new Vector2D(-1 * ball.position.getX(),
                        -1 * ball.position.getY()));

                double traj_power = Math.min(Settings.PLAYER_PARAMS.POWER_MAX,
                        (v_ball.magnitude() / (1 + Settings.BALL_PARAMS.BALL_DECAY)) * 10); // values of 1 or 2 do not give very useful kicks.
                this.kick(traj_power, Futil.simplifyAngle(Math.toDegrees(v_ball.direction())));
                break;
            case DASH_TOWARDS_BALL_AND_KICK:
                if (this.canKickBall()) {
                    this.kick(100.0, this.player.relativeAngleTo(opponentGoal));
                } else {
                    double approachAngle = this.player.relativeAngleTo(ball);
                    double dashPower = Math.min(100.0, Math.max(40.0, 800.0 / ball.curInfo.distance));
                    double tolerance = Math.max(10.0, 100.0 / ball.curInfo.distance);
                    if (Math.abs(approachAngle) > tolerance) {
                        this.turn(approachAngle);
                    } else {
                        dash(dashPower, approachAngle);
                    }
                }
                break;
            case LOOK_AROUND:
                turn(90.0);
                break;
            case GET_BETWEEN_BALL_AND_GOAL:
                if (this.role == PlayerRole.Role.GOLEIRO) {
                    double targetFacingDir = 0.0;
                    double x = -(Settings.FIELD_WIDTH / 2.0 - 1.0);
                    if (this.player.team.side == 'r') {
                        targetFacingDir = -180.0;
                        x = x * -1.0;
                    }
                    if (Math.abs(Futil.simplifyAngle(this.player.direction.getDirection() - targetFacingDir)) > 10.0) {
                        this.turnTo(targetFacingDir);
                    } else {
                        double y = ball.position.getY() / (Settings.FIELD_HEIGHT / Settings.GOAL_HEIGHT);
                        Point target = new Point(x, y);
                        if (this.player.position.getPosition().distanceTo(target) > 1.0) {
                            this.dash(60.0, this.player.relativeAngleTo(target));
                        }
                    }
                } else {
                    Point midpoint = ownGoal.position.getPosition().midpointTo(ball.position.getPosition());
                    double distanceAway = this.player.position.getPosition().distanceTo(midpoint);
                    if (distanceAway > 5.0) {
                        this.dashTo(midpoint, Math.min(100.0, distanceAway * 10.0));
                    }
                }
                break;
            case CLEAR_BALL:
                if (canKickBall()) {
                    double kickDir;
                    if (this.player.position.getY() > 0.0) {
                        kickDir = this.player.relativeAngleTo(90.0);
                    } else {
                        kickDir = this.player.relativeAngleTo(-90.0);
                    }
                    this.kick(80.0, kickDir);
                } else {
                    Point target = Futil.estimatePositionOf(ball, 1, this.time).getPosition();
                    if (this.player.position.getPosition().distanceTo(target) > Futil.kickable_radius()) {
                        this.dashTo(target, 80.0);
                    }
                }
                break;
            case RUN_TO_STARTING_POSITION:
                //TODO remove this call
                System.out.println("player " + player.number + " running to starting point");
                if (noSeeBallCount > noSeeBallCountMax) {
                    //wall run bandaid
                    this.turn(180);
                    noSeeBallCount = 0;
                    break;
                }
                if (Settings.FORMATION[player.number].distanceTo(player.position.getPosition()) < 10) {
                    noSeeBallCount = 0;
                }
                this.dashTo(Settings.FORMATION[player.number]);
                break;
            default:
                break;
        }
    }

    private EstadoDoGoleiro extrairEstadoGoleiro(FieldObject bola) {
        double maiorResposta = Double.NEGATIVE_INFINITY;
        double respostaDoEstado = 0;
        EstadoDoGoleiro estadoReal = this.estadoAtualDoGoleiro;

        for (EstadoDoGoleiro estado : EstadoDoGoleiro.values()) {
            switch (estado) {
                case BOLA_FORA_DE_VISAO:
                    respostaDoEstado = 1 - (canSee(Ball.ID) ? 1 : 0.05);
                    break;
                case BOLA_NA_AREA_DE_CAPTURA_DENTRO_DA_AREA:
                    if (canSee(Ball.ID)) {
                        if (bola.curInfo.distance < 1 && getMyPenaltyArea().contains(bola)) {
                            respostaDoEstado = 1;
                        } else {
                            respostaDoEstado = 0;
                        }
                    } else {
                        respostaDoEstado = 0;
                    }
                    break;
                case BOLA_NA_AREA_DE_CAPTURA_FORA_DA_AREA:
                    if (canSee(Ball.ID)) {
                        if (bola.curInfo.distance < 1 && !getMyPenaltyArea().contains(bola)) {
                            respostaDoEstado = 1;
                        } else {
                            respostaDoEstado = 0;
                        }
                    } else {
                        respostaDoEstado = 0;
                    }
                    break;
                case BOLA_NA_INTERMEDIARIA:
                    if (canSee(Ball.ID)) {
                        if (getCampoDeDefesa().contains(bola) && !getMyPenaltyArea().contains(bola)) {
                            if (getPequenaArea().contains(player)) {
                                respostaDoEstado = 0.95;
                            } else {
                                respostaDoEstado = 0.45;
                            }
                        } else {
                            respostaDoEstado = 0.45;
                        }
                    } else {
                        respostaDoEstado = 0;
                    }
                    break;
                case BOLA_PARADA:
                    if (!isPositioned) {
                        respostaDoEstado = 1 - (canUseMove() ? 0 : 1);
                    } else {
                        respostaDoEstado = 0;
                    }
                    break;
                case FORA_DE_POSICAO:
                    if (canSee(Ball.ID)) {
                        if ((bolaDistanciando(bola) || getCampoDeAtaque().contains(bola)) && !getPequenaArea().contains(player)) {
                            respostaDoEstado = 0.96;
                        } else {
                            respostaDoEstado = 0;
                        }
                    } else {
                        if (!getPequenaArea().contains(player) && playMode.equals("play_on")) {
                            // System.out.println("não está dentro da area");
                            respostaDoEstado = 0.97;
                        } else {
                            respostaDoEstado = 0;
                        }
                    }
                    break;
                case MAIS_PROXIMO_DA_BOLA_NA_AREA:
                    if (canSee(Ball.ID)) {
                        if (getMyPenaltyArea().contains(bola) && bolaQuaseParando(bola)) {
                            if (souOJogadorDoTimeMaisProximoDoObjeto(bola)) {
                                respostaDoEstado = 0.98;
                            } else {
                                respostaDoEstado = 0;
                            }
                        } else {
                            respostaDoEstado = 0.1;
                        }
                    } else {
                        respostaDoEstado = 0;
                    }
                    break;
                default:
                    break;
            }
            if (respostaDoEstado > maiorResposta) {
                maiorResposta = respostaDoEstado;
                estadoReal = estado;
            }
        }
        this.estadoAtualDoGoleiro = estadoReal;
        // System.out.print("RESPOSTA = " + maiorResposta + " | ");
        return estadoReal;
    }

    private EstadoJogador extrairEstadoJogador(FieldObject bola) {
        double maiorResposta = Double.NEGATIVE_INFINITY;
        double respostaDoEstado = 0;
        EstadoJogador estadoReal = this.estadoAtualDoJogador;

        for (EstadoJogador estado : EstadoJogador.values()) {
            switch (estado) {
                case BOLA_FORA_DA_VISAO:
                    respostaDoEstado = 1 - (canSee(Ball.ID) ? 1 : 0);
                    //respostaDoEstado -= repositioning ? 6 : 0;
                    break;
                case ESTOU_LIVRE:
                    if (canKickBall()) {
                        if (!adversarioMarcando(player)) {
                            respostaDoEstado = 0.97;
                        } else {
                            respostaDoEstado = 0;
                        }
                    } else {
                        respostaDoEstado = 0;
                    }
                    break;
                case ESTOU_MARCADO:
                    if (canKickBall()) {
                        if (adversarioMarcando(player)) {
                            respostaDoEstado = 0.97;
                        } else {
                            respostaDoEstado = 0;
                        }
                    } else {
                        respostaDoEstado = 0;
                    }
                    break;
                case MAIS_PROX_DA_BOLA:
                    if (souOJogadorDoTimeMaisProximoDoObjeto(bola) && bolaRolando()) {
                        respostaDoEstado = 0.96;
                    } else {
                        respostaDoEstado = 0;
                    }
                    break;
                case SEM_A_BOLA:
                    if (!souOJogadorDoTimeMaisProximoDoObjeto(bola)) {
                        respostaDoEstado = 0.95;
                    } else {
                        respostaDoEstado = 0;
                    }
                    break;
                case BOLA_PARADA:
                    if (!isPositioned) {
                        respostaDoEstado = 1 - (canUseMove() ? 0 : 1);
                    } else {
                        respostaDoEstado = 0;
                    }
                default:
                    break;
            }
            if (respostaDoEstado > maiorResposta) {
                maiorResposta = respostaDoEstado;
                estadoReal = estado;
            }
        }
        this.estadoAtualDoJogador = estadoReal;
        return estadoReal;
    }

    private boolean bolaQuaseParando(FieldObject bola) {
        return bola.curInfo.distChange >= -0.5 && bola.curInfo.distChange <= 0;
    }

    private boolean bolaVindoRapida(FieldObject bola) {
        return bola.curInfo.distChange < -0.5;
    }

    private boolean bolaDistanciando(FieldObject bola) {
        return !bolaQuaseParando(bola) && !bolaVindoRapida(bola);
    }

    private boolean souOJogadorDoTimeMaisProximoDoObjeto(FieldObject objeto) {
        for (Player companheiro : companheirosVisiveis) {
            if (companheiro.distanceTo(objeto) < objeto.curInfo.distance) {
                return false;
            }
        }
        return true;
    }

    private boolean adversarioMarcando(Player player) {
        for (Player adversario : lastSeenOpponents) {
            if (adversario.distanceTo(player) < Settings.MARCACAO) {
                return true;
            }
        }
        return false;
    }

    private boolean estaImpedido(Player player) {
        for (Player adversario : lastSeenOpponents) {
            if (player.team.side == 'l') {
                if (player.position.getX() <= adversario.position.getX()) {
                    return false;
                }
            } else {
                if (player.position.getX() >= adversario.position.getX()) {
                    return false;
                }
            }
        }
        return true;
    }

    private Player melhorOpcaoDePasse() {
        Player melhorOpcao = null;
        double posX = player.team.side == 'l' ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        for (Player companheiro : companheirosVisiveis) {
            if (!estaImpedido(companheiro) && !adversarioMarcando(companheiro)) {
                if (player.goleiro) {
                    continue;
                }
                if (player.team.side == 'l' && companheiro.position.getX() > posX) {
                    melhorOpcao = companheiro;
                    posX = companheiro.position.getX();
                }
                if (player.team.side == 'r' && companheiro.position.getX() < posX) {
                    melhorOpcao = companheiro;
                    posX = companheiro.position.getX();
                }
            }
        }
        return melhorOpcao;
    }

    private void passarABola(FieldObject bola) {
        Player melhorOpcao = melhorOpcaoDePasse();

        if (melhorOpcao != null) {
            Point provavelPonto = melhorOpcao.position.getPosition();
            double anguloDoPasse = player.relativeAngleTo(provavelPonto);
            double forca = (melhorOpcao.curInfo.distance * 5.0 / 2.0) * (1 + 0.5 * Math.abs(bola.curInfo.direction / 180) + 0.5 * (bola.curInfo.distance / Futil.kickable_radius()));
            kick(forca, melhorOpcao.curInfo.direction);
            System.out.println(player.render() + " Tocou para = " + melhorOpcao.number + " angulo = " + anguloDoPasse);
            try {
                Thread.sleep(300);
            } catch (InterruptedException ex) {
                Logger.getLogger(Brain.class.getName()).log(Level.SEVERE, null, ex);
            }

        } else {
            /*System.out.println("Sem melhor opção");
             int r = new Random().nextInt(2);
             if (r == 0) {
             driblar();
             } else {*/
           turn(90);
            
            //}
        }

    }

   /* private boolean existeAdversarioMaisProximoQueEuDoObjeto(FieldObject objeto) {
        for (Player adversario : lastSeenOpponents) {
            if (adversario.distanceTo(objeto) < objeto.curInfo.distance) {
                return false;
            }
        }
        return true;
    }*/

    private boolean isBolaNaAreaDeCapturaDoGoleiro(FieldObject bola) {
        Rectangle areaDeCaptura = new Rectangle(player.position.getY() + (Settings.CATCHABLE_AREA_W / 2), //TOP
                player.position.getX() + Settings.CATCHABLE_AREA_L - Settings.PLAYER_PARAMS.PLAYER_SIZE, //RIGHT
                player.position.getY() - (Settings.CATCHABLE_AREA_W / 2), //BOTTOM
                player.position.getX());      //LEFT  

        System.out.println(player.render());
        System.out.println("top = " + areaDeCaptura.getTop());
        System.out.println("bottom = " + areaDeCaptura.getBottom());
        System.out.println("left = " + areaDeCaptura.getLeft());
        System.out.println("right = " + areaDeCaptura.getRight());

        FieldObject bolaRotacionada = new MobileObject();
        double anguloRotacao = player.direction.getDirection() + bola.direction.getDirection();
        anguloRotacao = anguloRotacao < 0 ? 360 + anguloRotacao : anguloRotacao;
        bolaRotacionada.position.update(Futil.rotacionarPonto(bola.position.getPosition(), anguloRotacao), 0.95, time);
        System.out.println("bola rt | x =  " + bolaRotacionada.position.getX() + " y = " + bolaRotacionada.position.getY());
        return areaDeCaptura.contains(bolaRotacionada);
    }

    private void jogadorEstrategia() {
        try {
            
            FieldObject bola     = this.getOrCreate(Ball.ID);
            Point posAgente      = player.position.getPosition();
            Point posBola        = bola.position.getPosition();
            Point posEstrategica = getPosicaoEstrategica(player.number, formacao.formacaoEmCurso);
            logger.log("player pos = " + player.position.getPosition().render());
            logger.log("player dir = " + player.direction.getDirection());
            //logger.log("bola dir = " + bola.curInfo.direction);
            //logger.log("bola pos = " + posBola.render());
            
            if(Futil.isBeforeKickOff(playMode)){

                if( !isPositioned ) {
                    formacao.setFormacao(Formacao.FORMACAO_INICIAL);
                    //logger.log("dist to pos Estrategica " + posAgente.distanceTo( posEstrategica ));
                    isPositioned = true;
                    logger.log("move");
                    move( posEstrategica );
                }
                else {
                    turnBodyParaPonto( new Point(0,0), 1 );
                    alinharPescocoECorpo();
                    //logger.log("beffore kick off turn to centro");
                }
            }else{
                turn(45);
            }
            
            /*else{
                if(canKickBall()){
                    chutarPara(new Point(50,10),Settings.BALL_PARAMS.BALL_SPEED_MAX);
                }else{
                    dashTo(bola.position.getPosition());
                }               
            }
            
            else if(Futil.isKickOff(playMode, player.team.side) && player.number == 9){

                if(canKickBall()){
                    Point p = new Point( 52.5 , 0 );
                    chutarPara(p, Settings.BALL_PARAMS.BALL_SPEED_MAX);
                    //logger.log("kick off us kicking ball");
                    
                }else{
                    dashParaPonto( posBola, 1 );
                    //logger.log("kick off us dashing to bola");
                    
                }
                //turnNeck(bola.curInfo.direction);

            }else{
                isPositioned = false;
                formacao.setFormacao(Formacao.FORMACAO_433_OFENSIVO);
                posEstrategica = getPosicaoEstrategica(player.number, formacao.formacaoEmCurso);
                
                if(!canSee(Ball.ID)){
                    procurarBola();
                    alinharPescocoECorpo();
                    
                    //logger.log("procurando a bola - playon");

                }else if(canKickBall()){
                    Point p = new Point(52.5,0);
                    chutarPara(p, Settings.BALL_PARAMS.BALL_SPEED_MAX);
                    //turnNeck(bola.curInfo.direction);
                    //logger.log("chutando a bola - playon");

                }else if( Futil.souOMaisProximoDoObjeto(companheirosVisiveis, bola, posAgente.distanceTo(posBola))){
                    dashTo(bola.position.getPosition());
                    //turnNeck(bola.curInfo.direction);
                    //logger.log("mais prox da bola indo ate ela - playon");

                }else if( posAgente.distanceTo( posEstrategica ) > 
                        1.5 + Math.abs(posAgente.getX()-posBola.getX())/10.0){

                    if( curSenseInfo.stamina > Settings.PLAYER_PARAMS.D_RECOVER_DEC_THR * 
                            Settings.PLAYER_PARAMS.STAMINA_MAX + 800){
                        moveToPos(posEstrategica, Settings.ANGULO_PARA_USAR_TURN, 0, false, 1);
                        //turnNeck(bola.curInfo.direction);
                        //logger.log("moving to pos estrategica = playon");
                        
                    }else{
                        if(curSenseInfo.headAngle != 0)
                            alinharPescocoECorpo();
                        else{
                            virarCorpoParaBola(bola);
                            //turnNeck(bola.curInfo.direction);
                        }
                        //logger.log("watch ball  - playon (fora da posicao estrategica) e sem stamina");
                        
                    }                
                }else if( Math.abs( bola.curInfo.direction ) > 1){  // watch ball{
                    if(curSenseInfo.headAngle != 0)
                        alinharPescocoECorpo();
                    else{
                        virarCorpoParaBola(bola);
                        //turnNeck(bola.curInfo.direction);
                    }
                    //logger.log("watch ball  - playon");
                }
                else{                                         // nothing to do
                    turnNeck(0);
                    //logger.log("nothing to do");
                }
            }*/
            logger.log("--------------------------------------------------------");               
        } catch (Exception e) {
            e.printStackTrace();
            logger.log(e.getMessage());
            Scanner s = new Scanner(System.in);
            s.next();
        }
        
        /*EstadoJogador estado = extrairEstadoJogador(bola);
        if(bola.curInfo.time == time){
            System.out.println(player.render() + " | x = " + bola.position.getX() + " y = " + bola.position.getY());
        }

        try {
            if (estado == EstadoJogador.BOLA_FORA_DA_VISAO) {
                turn(90);
            } else if (estado == EstadoJogador.ESTOU_LIVRE) {
                avancarComABola();
            } else if (estado == EstadoJogador.ESTOU_MARCADO) {
                passarABola(bola);
            } else if (estado == EstadoJogador.MAIS_PROX_DA_BOLA) {
                aproximarDaBola(bola);
            } else if (estado == EstadoJogador.SEM_A_BOLA) {
                acompanharBola(bola);
            } else if (estado == EstadoJogador.BOLA_PARADA) {
                move(Settings.FORMATION[player.number]);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Scanner s = new Scanner(System.in);
            s.next();
        }*/

    }
    
    

    private void acompanharBola(FieldObject bola) {
        Rectangle areaPosicao = gerarRetanguloDePosicionamento(bola.position.getX());
       // areaPosicao.render();
       // System.out.println(player.render() + " " + player.position.getX() + " | " + player.position.getY());

        if (pontoDePosicionamento != null) {
            //if (!areaPosicao.contains(player)) {
            if (!areaPosicao.contains(pontoDePosicionamento)) {
                pontoDePosicionamento = areaPosicao.gerarPontoDentroDoRetangulo();
            } else {
             //   System.out.println("AREA NAO MUDOU");
                return;
            }
            //}
            /*else{
             System.out.println("player dentro da area");
             return;
             }*/
        } else {
            pontoDePosicionamento = areaPosicao.gerarPontoDentroDoRetangulo();
        }

        /* if (this.role == PlayerRole.Role.ZAGUEIRO_ESQUERDO || this.role == PlayerRole.Role.ZAGUEIRO_ESQUERDO
         || this.role == PlayerRole.Role.LATERAL_ESQUERDO || this.role == PlayerRole.Role.LATERAL_DIREITO) {
         if (getCampoDeDefesa().contains(pontoDePosicionamento)) {
         dashTo(pontoDePosicionamento);
         }
         } else if (this.role == PlayerRole.Role.ATACANTE_DIREITO || this.role == PlayerRole.Role.ATACANTE_ESQUERDO) {
         if (getCampoDeAtaque().contains(pontoDePosicionamento)) {
         dashTo(pontoDePosicionamento);
         }
         } else {*/
        // dashTo(pontoDePosicionamento);
        if (bola.curInfo.direction > 20) {
            turn(bola.curInfo.direction);
        } else {
            dash(100, player.relativeAngleTo(pontoDePosicionamento));
        }
        
        //acompanharBola(bola);
        //}
    }

    private void posicionarJogador(FieldObject bola, double distaMax, double distMin) {
        if (player.distanceTo(bola) > distaMax) {
            dash(100);
        }
        if (player.distanceTo(bola) < distMin) {
            dash(-100);
        }
    }

    private void ordenarPorDistancia(List<Player> listaPlayers, FieldObject bola) {
        Player aux;
        for (int i = 0; i < listaPlayers.size(); i++) {
            for (int j = i + 1; j < listaPlayers.size(); j++) {
                Player iP = listaPlayers.get(i);
                Player jP = listaPlayers.get(j);
                if (iP.distanceTo(bola) > jP.distanceTo(bola)) {
                    aux = iP;
                    listaPlayers.set(i, jP);
                    listaPlayers.set(j, aux);
                }
            }
        }
    }

    private void aproximarDaBola(FieldObject bola) {
        if (canKickBall()) {
            kick(0);
        } else {
            //  PositionEstimate position = Futil.estimatePositionOf(bola, 1, time);
            double approachAngle = bola.curInfo.direction;
            double dashPower = Math.min(100.0, Math.max(40.0, 800.0 / bola.curInfo.distance));
            double tolerance = Math.max(5.0, 100.0 / bola.curInfo.distance);
            if (Math.abs(approachAngle) > tolerance) {
                this.turn(approachAngle);
            } else {
                dash(dashPower, approachAngle);
            }
        }
    }

    private void avancarComABola() {
        double targetFacingDir = 0.0;
        FieldObject golAdversario = this.getOrCreate(this.player.getOpponentGoalId());
        if (getEnemyPenaltyArea().contains(player)) {
            kick(100, player.relativeAngleTo(golAdversario));
        } else {
            if (Math.abs(player.position.getX()) > 40) {
                //cruzarParaArea();
            } else {
                if (this.player.team.side == 'r') {
                    targetFacingDir = -180.0;
                }

                if (Math.abs(Futil.simplifyAngle(this.player.direction.getDirection() - targetFacingDir)) > 5.0) {
                    this.turnTo(targetFacingDir);
                } else {
                    kick(15, 0);
                }
            }
        }
    }

    private void driblar() {
        FieldObject bola = this.getOrCreate(Ball.ID);
        Vector2D v_new = Futil.estimatePositionOf(this.player, 2, this.time).getPosition().asVector();
        Vector2D v_target = v_new.add(findDribbleAngle());
        Vector2D v_ball = v_target.add(new Vector2D(-1 * bola.position.getX(),
                -1 * bola.position.getY()));

        double traj_power = Math.min(Settings.PLAYER_PARAMS.POWER_MAX,
                (v_ball.magnitude() / (1 + Settings.BALL_PARAMS.BALL_DECAY)) * 10); // values of 1 or 2 do not give very useful kicks.
        this.kick(traj_power + 20, Futil.simplifyAngle(Math.toDegrees(v_ball.direction())));
    }

    private void goleiroEstrategia() {
        FieldObject bola = this.getOrCreate(Ball.ID);
        EstadoDoGoleiro estado = extrairEstadoGoleiro(bola);
        //System.out.println(player.render() + " " + estado.name() + " " + player.goleiro);

        if (estado == EstadoDoGoleiro.BOLA_FORA_DE_VISAO) {
            procurarBola();
        } else if (estado == EstadoDoGoleiro.BOLA_NA_AREA_DE_CAPTURA_DENTRO_DA_AREA) {
            capturarBola();
        } else if (estado == EstadoDoGoleiro.BOLA_NA_AREA_DE_CAPTURA_FORA_DA_AREA) {
            chutarParaLateral(bola);
        } else if (estado == EstadoDoGoleiro.BOLA_PARADA) {
            move(Settings.FORMATION[player.number]);
        } else if (estado == EstadoDoGoleiro.FORA_DE_POSICAO) {
            reposicionarGoleiroNoCentroDoGol();
        } else if (estado == EstadoDoGoleiro.MAIS_PROXIMO_DA_BOLA_NA_AREA) {
            dash(100, bola.curInfo.direction);
        } else if (estado == EstadoDoGoleiro.BOLA_NA_INTERMEDIARIA) {
            cercarAnguloChute();
        }

    }

    private void cercarAnguloChute() {
        FieldObject bola = this.getOrCreate(Ball.ID);
        if (!isOlhandoDentroDoAnguloEmRelacaoAFrente(90)) {
            olharParaFrente();
        } else if (Math.abs(bola.curInfo.direction) > 20 || !getPequenaArea().contains(player) || getCampoDeAtaque().contains(bola)) {
            turn(bola.curInfo.direction);
        } else {
            if (isValorProximo(Math.abs(player.direction.getDirection()), 90, 3)) {
                if (bola.curInfo.direction < 0) {
                    dash(100, -90);
                } else if (bola.curInfo.direction > 0) {
                    dash(100, 90);
                }
            } else if (player.direction.getDirection() > 0) {
                if (player.team.side == 'l') {
                    if (bola.curInfo.direction < 0) {
                        dash(100, -90 - player.direction.getDirection());
                    } else if (bola.curInfo.direction > 0) {
                        dash(100, 90 - player.direction.getDirection());
                    }
                } else {
                    if (bola.curInfo.direction > 0) {
                        dash(100, 90 - (180 - player.direction.getDirection()));
                    } else if (bola.curInfo.direction < 0) {
                        dash(100, -90 - (180 - player.direction.getDirection()));
                    }
                }
            } else if (player.direction.getDirection() < 0) {
                if (player.team.side == 'l') {
                    if (bola.curInfo.direction < 0) {
                        dash(100, -90 + player.direction.getDirection());
                    } else if (bola.curInfo.direction > 0) {
                        dash(100, 90 - player.direction.getDirection());
                    }
                } else {
                    if (bola.curInfo.direction > 0) {
                        dash(100, 90 - (180 + player.direction.getDirection()));
                    } else if (bola.curInfo.direction < 0) {
                        dash(100, -90 + (180 + player.direction.getDirection()));
                    }
                }
            }
        }
    }

    private void reposicionarGoleiroNoCentroDoGol() {
        Point p = player.team.side == 'l' ? new Point(-50, 0) : new Point(50, 0);
        dashTo(p, 60);
    }

    /*private void procurarBola() {
        if (!isOlhandoDentroDoAnguloEmRelacaoAFrente(40)) {
            olharParaFrente();
        } else {
            turn(multiplicadorDoAngulo * 70);
            multiplicadorDoAngulo *= -1;
        }
    }*/

    private void olharParaFrente() {
        double direcaoPF = direcaoPraFrente(player.team.side);
        double anguloTurn = Futil.simplifyAngle(direcaoPF - player.direction.getDirection());
        turn(anguloTurn);
    }

    /**
     * Método que retorno true se o jogador estiver olhando em uma direcao
     */
    private boolean isOlhandoDentroDoAnguloEmRelacaoAFrente(double angulo) {
        int direcaoPF = direcaoPraFrente(player.team.side);
        switch (direcaoPF) {
            case 0:
                return Math.abs(player.direction.getDirection()) < Math.abs(angulo);
            case 180:
                return Math.abs(player.direction.getDirection()) > Math.abs(direcaoPF - angulo);
        }
        return false;
    }

    private void chutarParaLateral(FieldObject bola) {
        if (canKickBall()) {
            double kickDir;
            if (this.player.position.getY() > 0.0) {
                kickDir = this.player.relativeAngleTo(90.0);
            } else {
                kickDir = this.player.relativeAngleTo(-90.0);
            }
            this.kick(80.0, kickDir);
        } else {
            Point target = Futil.estimatePositionOf(bola, 1, this.time).getPosition();
            if (this.player.position.getPosition().distanceTo(target) > Futil.kickable_radius()) {
                this.dashTo(target, 80.0);
            }
        }
    }

    private void cruzarParaArea() {
        FieldObject bola = this.getOrCreate(Ball.ID);
        if (canKickBall()) {
            double kickDir;
            if (this.player.position.getY() > 0.0) {
                kickDir = this.player.relativeAngleTo(-90.0);
            } else {
                kickDir = this.player.relativeAngleTo(90.0);
            }
            this.kick(kickDir);
        } else {
            Point target = Futil.estimatePositionOf(bola, 1, this.time).getPosition();
            if (this.player.position.getPosition().distanceTo(target) > Futil.kickable_radius()) {
                this.dashTo(target, 80.0);
            }
        }
    }

    public Rectangle gerarRetanguloDePosicionamento(double bola) {
        double yCima, yBaixo, xDireita, xEsquerda, xBola = bola;

        // GERANDO O Y 
        if (this.role == PlayerRole.Role.PONTA_ESQUERDA || this.role == PlayerRole.Role.LATERAL_ESQUERDO) {
            yCima = -(Settings.FIELD_HEIGHT / 2);
            yBaixo = -(Settings.FIELD_HEIGHT / 4);
        } else if (this.role == PlayerRole.Role.MEIA_ESQUERDA || this.role == PlayerRole.Role.ZAGUEIRO_ESQUERDO) {
            yCima = -(Settings.FIELD_HEIGHT / 4);
            yBaixo = 0;
        } else if (this.role == PlayerRole.Role.MEIA_DIREITA || this.role == PlayerRole.Role.ZAGUEIRO_DIREITO) {
            yCima = 0;
            yBaixo = (Settings.FIELD_HEIGHT / 4);
        } else if (this.role == PlayerRole.Role.PONTA_DIREITA || this.role == PlayerRole.Role.LATERAL_DIREITO) {
            yCima = (Settings.FIELD_HEIGHT / 4);
            yBaixo = (Settings.FIELD_HEIGHT / 2);
        } else if (this.role == PlayerRole.Role.ATACANTE_ESQUERDO) {
            yCima = -(Settings.FIELD_HEIGHT / 2);
            yBaixo = 0;
        } else {
            yCima = 0;
            yBaixo = (Settings.FIELD_HEIGHT / 2);
        }

        // GERANDO O X
        if (this.role == PlayerRole.Role.PONTA_DIREITA || this.role == PlayerRole.Role.PONTA_ESQUERDA
                || this.role == PlayerRole.Role.MEIA_DIREITA || this.role == PlayerRole.Role.MEIA_ESQUERDA) {

            xEsquerda = xBola - Settings.COMPRIMENTO_RETANGULO_DA_BOLA;
            xDireita = xBola + Settings.COMPRIMENTO_RETANGULO_DA_BOLA;

        } else if (this.role == PlayerRole.Role.LATERAL_ESQUERDO || this.role == PlayerRole.Role.LATERAL_DIREITO
                || this.role == PlayerRole.Role.ZAGUEIRO_DIREITO || this.role == PlayerRole.Role.ZAGUEIRO_ESQUERDO) {

            if (player.team.side == 'l') {
                xEsquerda = xBola - Settings.COMPRIMENTO_RETANGULO_DA_BOLA - Settings.COMPRIMENTO_RETANGULO_ANTERIOR_DA_BOLA;
                xDireita = xBola - Settings.COMPRIMENTO_RETANGULO_DA_BOLA;
            } else {
                xEsquerda = xBola + Settings.COMPRIMENTO_RETANGULO_DA_BOLA;
                xDireita = xBola - Settings.COMPRIMENTO_RETANGULO_DA_BOLA + Settings.COMPRIMENTO_RETANGULO_ANTERIOR_DA_BOLA;
            }

        } else {

            if (player.team.side == 'l') {
                xEsquerda = xBola + Settings.COMPRIMENTO_RETANGULO_DA_BOLA;
                xDireita = xBola + Settings.COMPRIMENTO_RETANGULO_DA_BOLA + Settings.COMPRIMENTO_RETANGULO_ANTERIOR_DA_BOLA;
            } else {
                xEsquerda = xBola - Settings.COMPRIMENTO_RETANGULO_DA_BOLA - Settings.COMPRIMENTO_RETANGULO_ANTERIOR_DA_BOLA;
                xDireita = xBola - Settings.COMPRIMENTO_RETANGULO_DA_BOLA;
            }

        }

        return new Rectangle(yCima, xDireita, yBaixo, xEsquerda);
    }

    /**
     * Finds the optimal angle to kick the ball toward within a kickable area.
     *
     * @param p Point to build angle from
     * @return the vector to dribble toward.
     */
    private final Vector2D findDribbleAngle() {
        // TODO STUB: Need algorithm for a weighted dribble angle.
        double d_length = Math.max(1.0, Futil.kickable_radius());

        // 5.0 is arbitrary in case nothing is visible; attempt to kick
        //   toward the lateral center of the field.
        double d_angle = 5.0 * -1.0 * Math.signum(this.player.position.getY());

        // If opponents are visible, try to kick away from them.
        if (!lastSeenOpponents.isEmpty()) {
            double weight = 0.0d;
            double w_angle = 0.0d;
            for (FieldObject i : lastSeenOpponents) {
                double i_angle = player.relativeAngleTo(i);
                double new_weight = Math.max(weight, Math.min(1.0,
                        1 / player.distanceTo(i) * Math.abs(
                                1 / (i_angle == 0.0 ? 1.0 : i_angle))));
                if (new_weight > weight) {
                    w_angle = i_angle;
                }
            }

            // Keep the angle within [-90,90]. Kick forward, not backward!
            d_angle = Math.max(Math.abs(w_angle) - 180, -90) * Math.signum(w_angle);
        } // Otherwise kick toward the goal.
        else if (this.canSee(this.player.getOpponentGoalId())) {
            d_angle += this.player.relativeAngleTo(
                    this.getOrCreate(this.player.getOpponentGoalId()));
        }
        Vector2D d_vec = new Vector2D(0.0, 0.0);
        d_vec = d_vec.addPolar(Math.toRadians(d_angle), d_length); // ?!
        return d_vec;

        /*
         * Proposed algorithm:
         * 
         * Finding highest weight opponent:
         *   W_i = ( 1 / opponent_distance ) * abs( 1 / opponent_angle ) ) 
         * 
         * Finding RELATIVE angle:
         *   d_angle = max( abs( Opp_w_relative_angle ) - 180, -90 )
         *              * signum( Opp_w_relative_angle )  
         */
    }

    /**
     * Gets the requested `FieldObject` from fieldObjects, or creates it if it
     * doesn't yet exist.
     *
     * @param id the object's id
     * @return the field object
     */
    private final FieldObject getOrCreate(String id) {
        if (this.fieldObjects.containsKey(id)) {
            return this.fieldObjects.get(id);
        } else {
            return FieldObject.create(id);
        }
    }

    private int direcaoPraFrente(char time) {
        if (time == Settings.LEFT_SIDE) {
            return 0;
        } else {
            return 180;
        }
    }

    private boolean isValorProximo(double valor1, double valor2, double limite) {
        return Math.abs(valor2 - valor1) <= limite;
    }

    /**
     * Infers the position and direction of this brain's associated player given
     * two boundary flags on the same side seen in the current time step.
     *
     * @param o1 the first flag
     * @param o2 the second flag
     */
    private final void inferPositionAndDirection(FieldObject o1, FieldObject o2) {
        // x1, x2, y1 and y2 are relative Cartesian coordinates to the flags
        double x1 = Math.cos(Math.toRadians(o1.curInfo.direction)) * o1.curInfo.distance;
        double y1 = Math.sin(Math.toRadians(o1.curInfo.direction)) * o1.curInfo.distance;
        double x2 = Math.cos(Math.toRadians(o2.curInfo.direction)) * o2.curInfo.distance;
        double y2 = Math.sin(Math.toRadians(o2.curInfo.direction)) * o2.curInfo.distance;
        double direction = -Math.toDegrees(Math.atan((y2 - y1) / (x2 - x1)));
        // Need to reverse the direction if looking closer to west and using horizontal boundary flags
        if (o1.position.getY() == o2.position.getY()) {
            if (Math.signum(o2.position.getX() - o1.position.getX()) != Math.signum(x2 - x1)) {
                direction += 180.0;
            }
        } // Need to offset the direction by +/- 90 degrees if using vertical boundary flags
        else if (o1.position.getX() == o2.position.getX()) {
            if (Math.signum(o2.position.getY() - o1.position.getY()) != Math.signum(x2 - x1)) {
                direction += 270.0;
            } else {
                direction += 90.0;
            }
        }
        
        double minusX = o1.curInfo.distance * Math.cos(Math.toRadians(direction + o1.curInfo.direction));
        double minusY = o1.curInfo.distance * Math.sin(Math.toRadians(direction + o1.curInfo.direction));
        
        if(o1.position.getX() < 0 && minusX > 0)
            minusX *= -1;
        
        this.player.direction.update(Futil.simplifyAngle(direction), 0.95, this.time);
        double x = o1.position.getX() - minusX;
        double y = o1.position.getY() - minusY;
        this.player.position.update(x, y, 0.95, this.time);
    }

    /**
     * Indication of if this player is a defender.
     *
     * @return true if this player is a defender
     */
    public boolean isDefender() {
        return this.role == PlayerRole.Role.ZAGUEIRO_DIREITO || this.role == PlayerRole.Role.ZAGUEIRO_ESQUERDO
                || this.role == PlayerRole.Role.LATERAL_DIREITO || this.role == PlayerRole.Role.LATERAL_ESQUERDO;
    }

    /**
     * Moves the player to the specified soccer server coordinates.
     *
     * @param p the Point object to pass coordinates with (must be in server
     * coordinates).
     */
    public void move(Point p) {
        move(p.getX(), p.getY());
    }

    /**
     * Moves the player to the specified soccer server coordinates.
     *
     * @param x x-coordinate
     * @param y y-coordinate
     */
    public void move(double x, double y) {
        client.sendCommand(Settings.Commands.MOVE, Double.toString(x), Double.toString(y));
        escreverNoLog("move (" + x + " , " + y + ")");
        this.player.position.update(x, y, 1.0, this.time);
    }

    public void turnNeck(double angulo) {
        escreverNoLog("turn_neck " + angulo);
        client.sendCommand(Settings.Commands.TURN_NECK, angulo);
    }

    /**
     * Overrides the strategy selection system. Used in tests.
     */
    public void overrideStrategy(Strategy strategy) {
        this.currentStrategy = strategy;
        this.updateStrategy = false;
    }

    /**
     * Kicks the ball in the direction of the player.
     *
     * @param power the level of power with which to kick (0 to 100)
     */
    public void kick(double power) {
        escreverNoLog("kick " + power);
        client.sendCommand(Settings.Commands.KICK, Double.toString(power));
    }

    /**
     * Captura a bola no angulo que ela está em relação ao goleiro. (Apenas
     * goleiros podem usar esta habilidade)
     */
    public void capturarBola() {
        FieldObject bola = getOrCreate(Ball.ID);
        escreverNoLog("catch " + Double.toString(bola.curInfo.direction));
        client.sendCommand(Settings.Commands.CATCH, Double.toString(bola.curInfo.direction));
    }

    /**
     * Kicks the ball in the player's direction, offset by the given angle.
     *
     * @param power the level of power with which to kick (0 to 100)
     * @param offset an angle in degrees to be added to the player's direction,
     * yielding the direction of the kick
     */
    public void kick(double power, double offset) {
        escreverNoLog("kick " + power + " , " + offset);
        client.sendCommand(Settings.Commands.KICK, Double.toString(power), Double.toString(offset));
    }

    public void escreverNoLog(String mensagem){
        logger.log(time + " - ( " +mensagem+ " )");
    }
    
    /**
     * Parses a message from the soccer server. This method is called whenever a
     * message from the server is received.
     *
     * @param message the message (string), exactly as it was received
     */
    public void parseMessage(String message) {
        long timeReceived = System.currentTimeMillis();
        message = Futil.sanitize(message);
        // Handle `sense_body` messages
        if (message.startsWith("(sense_body")) {
            //System.out.println(this.time);
            curSenseInfo.copy(lastSenseInfo);
            curSenseInfo.reset();

            this.timeLastSenseBody = timeReceived;
            curSenseInfo.time = Futil.extractTime(message);
            this.cicloUltimaSenseInfo = curSenseInfo.time;
            //this.time = curSenseInfo.time;

            String parts[] = message.split("\\(");
            // System.out.println(this.time);
            //System.out.println();
            for (String i : parts) // for each structured argument:
            {
                // Clean the string, and break it down into the base arguments.
                String nMsg = i.split("\\)")[0].trim();
                if (nMsg.isEmpty()) {
                    continue;
                }
                String nArgs[] = nMsg.split("\\s");

                // Check for specific argument types; ignore unknown arguments.
                if (nArgs[0].contains("view_mode")) { // Player's current view mode
                    curSenseInfo.viewQuality = nArgs[1];
                    curSenseInfo.viewWidth = nArgs[2];
                } else if (nArgs[0].contains("stamina")) { // Player's stamina data
                    curSenseInfo.stamina = Double.parseDouble(nArgs[1]);
                    curSenseInfo.effort = Double.parseDouble(nArgs[2]);
                    curSenseInfo.staminaCapacity = Double.parseDouble(nArgs[3]);
                } else if (nArgs[0].contains("speed")) { // Player's speed data
                    curSenseInfo.amountOfSpeed = Double.parseDouble(nArgs[1]);
                    curSenseInfo.directionOfSpeed = Double.parseDouble(nArgs[2]);
                    // Update velocity variable
                    double dir = this.dir() + Math.toRadians(curSenseInfo.directionOfSpeed);
                    this.velocity.setPolar(dir, curSenseInfo.amountOfSpeed);
                } else if (nArgs[0].contains("head_angle")) { // Player's head angle
                    curSenseInfo.headAngle = Double.parseDouble(nArgs[1]);
                } else if (nArgs[0].contains("ball") || nArgs[0].contains("player")
                        || nArgs[0].contains("post")) { // COLLISION flags; limitation of this loop approach is we
                    //   can't handle nested parentheses arguments well.
                    // Luckily these flags only occur in the collision structure.
                    curSenseInfo.collision = nArgs[0];
                }
            }

            // If the brain has responded to two see messages in a row, it's time to respond to a sense_body.
            if (this.responseHistory.get(0) == Settings.RESPONSE.SEE && this.responseHistory.get(1) == Settings.RESPONSE.SEE) {
                this.run();
                this.responseHistory.push(Settings.RESPONSE.SENSE_BODY);
                this.responseHistory.removeLast();
            }
        } // Handle `hear` messages
        else if (message.startsWith("(hear")) {
            String parts[] = message.split("\\s");
            this.time = Integer.parseInt(parts[1]);
            if (parts[2].startsWith("s") || parts[2].startsWith("o") || parts[2].startsWith("c")) {
                // TODO logic for self, on-line coach, and trainer coach.
                // Self could potentially be for feedback,
                // On-line coach will require coach language parsing,
                // And trainer likely will as well. Outside of Sprint #2 scope.
                return;
            } else {
                // Check for a referee message, otherwise continue.
                String nMsg = parts[3].split("\\)")[0];         // Retrieve the message.
                if (nMsg.startsWith("goal_l_")) {
                    nMsg = "goal_l_";
                } else if (nMsg.startsWith("goal_r_")) {
                    nMsg = "goal_r_";
                }
                if (parts[2].startsWith("r") // Referee;
                        && Settings.PLAY_MODES.contains(nMsg)) // Play Mode?
                {
                    playMode = nMsg;
                    this.isPositioned = false;
                } else {
                    hearMessages.add(nMsg);
                }
            }
        } // Handle `see` messages
        else if (message.startsWith("(see")) {
            long timeSee = System.currentTimeMillis();
            //System.out.println(player.render() + " Diferença entre ultimo ciclo e o atual :  "  + (timeSee - timeLastSee)/150 + " ciclos");
            this.timeLastSee = timeSee;
            this.time = Futil.extractTime(message);
            LinkedList<String> infos = Futil.extractInfos(message);
            lastSeenOpponents.clear();
            companheirosVisiveis.clear();
            for (String info : infos) {
                String id = Futil.extractId(info);
                if (Futil.isUniqueFieldObject(id)) {
                    FieldObject obj = this.getOrCreate(id);
                    obj.update(this.player, info, this.time);
                    this.fieldObjects.put(id, obj);
                    if (id.startsWith("(p \"") && !(id.startsWith(this.player.team.name, 4))) {
                        lastSeenOpponents.add((Player) obj);
                    }
                    if (id.startsWith("(p \"") && (id.startsWith(this.player.team.name, 4))) {
                        companheirosVisiveis.add((Player) obj);
                    }
                }
            }
            // Immediately run for the current step. Since our computations takes only a few
            // milliseconds, it's okay to start running over half-way into the 100ms cycle.
            // That means two out of every three time steps will be executed here.
            this.updatePositionAndDirection();
            this.run();
            // Make sure we stay in sync with the mid-way `see`s
            if (this.timeLastSee - this.timeLastSenseBody > 30) {
                this.responseHistory.clear();
                this.responseHistory.add(Settings.RESPONSE.SEE);
                this.responseHistory.add(Settings.RESPONSE.SEE);
            } else {
                this.responseHistory.add(Settings.RESPONSE.SEE);
                this.responseHistory.removeLast();
            }
            //Keep track of steps since the ball was last seen
            if (canSee(Ball.ID)) {
                noSeeBallCount = 0;
                //System.out.println(player.render() + " Não vejo a bola");
            } else {
                noSeeBallCount++;
            }

        } // Handle init messages
        else if (message.startsWith("(init")) {
            String[] parts = message.split("\\s");
            char teamSide = message.charAt(6);
            if (teamSide == Settings.LEFT_SIDE) {
                player.team.side = Settings.LEFT_SIDE;
                player.otherTeam.side = Settings.RIGHT_SIDE;
            } else if (teamSide == Settings.RIGHT_SIDE) {
                player.team.side = Settings.RIGHT_SIDE;
                player.otherTeam.side = Settings.LEFT_SIDE;
            } else {
                // Raise error
                Log.e("Could not parse teamSide.");
            }
            player.number = Integer.parseInt(parts[2]);
            if (role != PlayerRole.Role.GOLEIRO) {
                this.role = Settings.PLAYER_ROLES[this.player.number - 1];
            }
            playMode = parts[3].split("\\)")[0];
            logger = new MiniLogger(player.team.name + "_" + player.number + ".txt");
        } else if (message.startsWith("(server_param")) {
            parseServerParameters(message);
        }
    }

    /**
     * Parses the initial parameters received from the server.
     *
     * @param message the parameters message received from the server
     */
    public void parseServerParameters(String message) {
        String parts[] = message.split("\\(");
        for (String i : parts) // for each structured argument:
        {
            // Clean the string, and break it down into the base arguments.
            String nMsg = i.split("\\)")[0].trim();
            if (nMsg.isEmpty()) {
                continue;
            }
            String nArgs[] = nMsg.split("\\s");

            // Check for specific argument types; ignore unknown arguments.
            if (nArgs[0].startsWith("dash_power_rate")) {
                Settings.setDashPowerRate(Double.parseDouble(nArgs[1]));
            }
            if (nArgs[0].startsWith("goal_width")) {
                Settings.setGoalHeight(Double.parseDouble(nArgs[1]));
            } // Ball arguments:
            else if (nArgs[0].startsWith("ball")) {
                ServerParams_Ball.Builder.dataParser(nArgs);
            } // Player arguments:
            else if (nArgs[0].startsWith("player") || nArgs[0].startsWith("min")
                    || nArgs[0].startsWith("max")) {
                ServerParams_Player.Builder.dataParser(nArgs);
            }
        }

        // Rebuild all parameter objects with updated parameters.
        Settings.rebuildParams();
    }

    /**
     * Returns this player's team's goal.
     *
     * @return this player's team's goal
     */
    public final FieldObject ownGoal() {
        return this.getOrCreate(this.player.getGoalId());
    }

    /**
     * Returns the penalty area of this player's team's goal.
     *
     * @return the penalty area of this player's team's goal
     */
    public final Rectangle ownPenaltyArea() {
        if (this.player.team.side == 'l') {
            return Settings.PENALTY_AREA_LEFT;
        } else {
            return Settings.PENALTY_AREA_RIGHT;
        }
    }

    /**
     * Responds for the current time step.
     */
    public void run() {
        int expectedNextRun = this.lastRan + 1;
        if (this.time > this.lastRan + 1) {
            //Log.e("Brain for player " + this.player.render() + " did not run during time step " + expectedNextRun + ".");
        }
        this.lastRan = this.time;
        this.acceleration.reset();
        if (role != PlayerRole.Role.GOLEIRO) {
            //this.currentStrategy = this.determineOptimalStrategy();
            // this.executeStrategy(this.currentStrategy);
            this.jogadorEstrategia();
        } else {
            this.goleiroEstrategia();
        }
    }

    /**
     * Adds the given angle to the player's current direction.
     *
     * @param offset an angle in degrees to add to the player's current
     * direction
     */
    public final void turn(double offset) {
        double moment = Futil.toValidMoment(offset);
        escreverNoLog("turn "  + offset);
        client.sendCommand(Settings.Commands.TURN, moment);
        // TODO Potentially take magnitude of offset into account in the
        // determination of the new confidence in the player's position.
       // player.direction.update(player.direction.getDirection() + moment, 0.95 * player.direction.getConfidence(this.time), this.time);
    }

    /**
     * Updates the player's current direction to be the given direction.
     *
     * @param direction angle in degrees, assuming soccer server coordinate
     * system
     */
    public final void turnTo(double direction) {
        this.turn(this.player.relativeAngleTo(direction));
    }

    /**
     * Directs the player to dash to a given point, turning if necessary.
     *
     * @param point the point to dash to
     */
    private final void dashTo(Point point) {
        dashTo(point, 50.0);
    }

    /**
     * Directs the player to dash to a given point, turning if necessary.
     *
     * @param ponto the point to dash to
     * @param power the power at which to dash
     */
    private final void dashTo(Point ponto, double power) {
        double tolerance           = Math.max(10.0, 100.0 / this.player.position.getPosition().distanceTo(ponto));
        Point posGlobal            = player.position.getPosition();
        Vector2D vet               = ponto.asVector();
        vet.menos(posGlobal.asVector());
        double angulo              = Math.toDegrees(vet.direction());
        angulo                    -= player.direction.getDirection();
        angulo                     = Futil.simplifyAngle(angulo);
        
        if (Math.abs(angulo) > Settings.ANGULO_PARA_USAR_TURN) {
            turnBodyParaPonto(ponto, 1);
        } else {
            dashParaPonto(ponto, 1);
        }
    }

    /**
     * Updates this this brain's belief about the associated player's position
     * and direction at the current time step. This method should be called
     * immediately after parsing a `see` message, and only then.
     */
    private final void updatePositionAndDirection() {
        // Infer from the most-recent `see` if it happened in the current time-step
        for (int i = 0; i < 4; i++) {
            LinkedList<FieldObject> flagsOnSide = new LinkedList<FieldObject>();
            for (String id : Settings.BOUNDARY_FLAG_GROUPS[i]) {
                FieldObject flag = this.fieldObjects.get(id);
                if (flag.curInfo.time == this.time) {
                    flagsOnSide.add(flag);
                } else {
                    //Log.i("Flag " + id + "last updated at time " + flag.info.time + ", not " + this.time);
                }
                if (flagsOnSide.size() > 1) {
                    this.inferPositionAndDirection(flagsOnSide.poll(), flagsOnSide.poll());
                    return;
                }
            }
        }
    }

    /**
     * @return {@link Settings#PENALTY_AREA_LEFT} if player is on the left team,
     * or {@link Settings#PENALTY_AREA_RIGHT} if on the right team.
     */
    final public Rectangle getMyPenaltyArea() {
        if (player.team == null) {
            throw new NullPointerException("Player team not initialized while getting penelty area.");
        }
        return player.team.side == 'l' ? Settings.PENALTY_AREA_LEFT : Settings.PENALTY_AREA_RIGHT;
    }

    final public Rectangle getEnemyPenaltyArea() {
        if (player.team == null) {
            throw new NullPointerException("Player team not initialized while getting penelty area.");
        }
        return player.team.side == 'l' ? Settings.PENALTY_AREA_RIGHT : Settings.PENALTY_AREA_LEFT;
    }

    final public Rectangle getCampoDeDefesa() {
        if (player.team == null) {
            throw new NullPointerException("Player team not initialized while getting penelty area.");
        }
        return player.team.side == 'l' ? Settings.CAMPO_L : Settings.CAMPO_R;
    }

    final public Rectangle getCampoDeAtaque() {
        if (player.team == null) {
            throw new NullPointerException("Player team not initialized while getting penelty area.");
        }
        return player.team.side == 'l' ? Settings.CAMPO_R : Settings.CAMPO_L;
    }

    final public Rectangle getPequenaArea() {
        if (player.team == null) {
            throw new NullPointerException("Player team not initialized while getting penelty area.");
        }
        return player.team.side == 'l' ? Settings.PEQUENA_AREA_L : Settings.PEQUENA_AREA_R;
    }
    
    public void alinharPescocoECorpo(){
        turnNeck(-curSenseInfo.headAngle);
    }
    
    public void virarCorpoParaBola(FieldObject bola){
        turnBodyParaPonto(Futil.predictlPosBolaDepoisNCiclos(bola, 1), 1);
    }
    
    public void chutarPara(Point posAlvo, double velFinal){
        FieldObject bola = getOrCreate(Ball.ID);
        
        Point posBola           = bola.position.getPosition();
        VelocityVector velBola  = bola.velocity();
        Point posTrajetoria     = posAlvo;
        Point posAgente         = player.position.getPosition();
        
        posTrajetoria.menos(posBola);
        VelocityVector velDes   = new VelocityVector();
        velDes.setPolar(posTrajetoria.asVector().direction(),
                Futil.getForcaParaAtravessar(posTrajetoria.asVector().magnitude(), velFinal));
        
        double power;
        double angulo;
        
        PositionEstimate posEst     = Futil.predictAgentePosDepoisNCiclos(player, 0, 1, this.time, curSenseInfo);
        Point pointEst              = posEst.getPosition();
        Vector2D velDesMaisPosBola  = posBola.asVector();
        velDesMaisPosBola.mais(velDes);
        
        
        if( pointEst.distanceTo(velDesMaisPosBola.asPoint()) < 
                  Settings.BALL_PARAMS.BALL_SIZE + Settings.PLAYER_PARAMS.PLAYER_SIZE){
            
            Reta reta           = Reta.criarRetaEntre2Pontos(posBola, velDesMaisPosBola.asPoint());
            Point posAgenteProj = reta.pontoNaRetaMaisProxDoPonto( posAgente );
            double dist         = posBola.distanceTo(posAgenteProj);
            
            if( velDes.magnitude() < dist )
                dist -=  Settings.BALL_PARAMS.BALL_SIZE + Settings.PLAYER_PARAMS.PLAYER_SIZE;
            else
                dist +=  Settings.BALL_PARAMS.BALL_SIZE + Settings.PLAYER_PARAMS.PLAYER_SIZE;
            
            velDes.setPolar(velDes.direction(), dist);
        }
        
        FieldObject oponente    = Futil.maisProximoDoObjeto(lastSeenOpponents, bola);
        double distOponente     = oponente != null ? 
                oponente.position.getPosition().distanceTo(bola.position.getPosition()) : 100;
        
        if( velDes.magnitude() > Settings.BALL_PARAMS.BALL_SPEED_MAX){ // NÃO VAI CHEGAR NO PONTO
            power               = Settings.PLAYER_PARAMS.POWER_MAX;
            double dSpeed       = getKickPowerRateAtual( bola ) * power;
            double tmp          = velBola.rotate(-velDes.direction()).getY();
            angulo              = velDes.direction() - Futil.arcSenGraus(tmp / dSpeed);
            Vector2D aux        = new Vector2D();
            aux.setCoordPolar(angulo, dSpeed);
            aux.mais(bola.velocity());
            
            double dSpeedPred   = aux.magnitude();
            
            if( dSpeedPred > Settings.PLAYER_PARAMS.PLAYER_WHEN_TO_KICK * Settings.BALL_PARAMS.BALL_ACCEL_MAX){
                acelerarBolaAVelocidade( velDes );    // shoot nevertheless
            }
            else if( getKickPowerRateAtual( bola ) > Settings.PLAYER_PARAMS.PLAYER_WHEN_TO_KICK * Settings.PLAYER_PARAMS.KICK_POWER_RATE ){
                dominarBola();                          // freeze ball
            }
            else { 
                chutarBolaProximaAoCorpo( 0, 0.16 );            // else position ball better
            }
        }else{
            Vector2D velBolaAcele = velDes;
            velBolaAcele.menos(velBola);
            
            power = velBolaAcele.magnitude() / getKickPowerRateAtual( bola ); // with current ball speed
            if( power <= 1.05 * Settings.PLAYER_PARAMS.POWER_MAX || (distOponente < 2.0 && power <= 1.30 * Settings.PLAYER_PARAMS.POWER_MAX ) ){                               
                acelerarBolaAVelocidade( velDes );  // perform shooting action
                
            }else{
                chutarBolaProximaAoCorpo( 0 , 0.16 );
            }
        }        
    }
    
    public void acelerarBolaAVelocidade( Vector2D velDes ){
        FieldObject bola = getOrCreate(Ball.ID);
        
        double angBody          = player.direction.getDirection();
        VelocityVector velBall  = bola.velocity();
        Vector2D accDes         = velDes;
        accDes.menos(velBall);
        double      dPower;
        double      angActual;
        
        // if acceleration can be reached, create shooting vector
        if( accDes.magnitude() < Settings.BALL_PARAMS.BALL_ACCEL_MAX ){
            dPower    = ( accDes.magnitude() / getKickPowerRateAtual( bola ) );
            angActual = Futil.simplifyAngle( Math.toDegrees(accDes.direction()) - angBody );
            if( dPower <= Settings.PLAYER_PARAMS.POWER_MAX  ){
                kick( dPower, angActual );
                return;
            }
        }
        
        // else determine vector that is in direction 'velDes' (magnitude is lower)
         dPower           = Settings.PLAYER_PARAMS.POWER_MAX ;
         double dSpeed    = getKickPowerRateAtual( bola ) * dPower;
         double tmp       = velBall.rotate(-velDes.direction()).getY();
         angActual        = Math.toDegrees(velDes.direction() - Futil.arcSenGraus( tmp / dSpeed ));
         angActual        = Futil.simplifyAngle( angActual - angBody );
         kick( dPower, angActual );   
        
    }
    
    /**
     * Habilidade que permite o agente chutar a bola proxima a seu corpo
     * @param angulo relativo angulo em graus
     * @param taxaDeChute padrão 0.16
     */    
    public void chutarBolaProximaAoCorpo(double angulo, double taxaDeChute){ // taxa de chute
        FieldObject bola = getOrCreate(Ball.ID);
            
        double angAgente    = player.direction.getDirection(); // graus
        PositionEstimate p  = Futil.predictAgentePosDepoisNCiclos(player, 0, 1, time, curSenseInfo);
        Point point         = p.getPosition();
        double dist         = Settings.BALL_PARAMS.BALL_SIZE + Settings.PLAYER_PARAMS.PLAYER_SIZE 
                + Settings.PLAYER_PARAMS.KICKABLE_MARGIN * taxaDeChute;
        double angGlobal    = Futil.simplifyAngle(angAgente + angulo); // graus
        Vector2D posBall    = new Vector2D();
        posBall.setCoordPolar(Math.toRadians(angGlobal), dist);
        posBall.mais(point.asVector());
        
        if(Math.abs(posBall.getY()) > Settings.FIELD_HEIGHT/2 || Math.abs(posBall.getX()) > Settings.FIELD_WIDTH/2){
            Reta lineBody = Reta.criarRetaAPartirDaPosicaoEAngulo(point, Math.toRadians(angGlobal) );
            Reta lineSide;
            if( Math.abs( posBall.getY() ) > Settings.FIELD_HEIGHT/2 )
              lineSide = Reta.criarRetaAPartirDaPosicaoEAngulo( 
                  new Point( 0, Math.signum(posBall.getY() )* Settings.FIELD_HEIGHT/2.0 ), 0 );
            else
              lineSide = Reta.criarRetaAPartirDaPosicaoEAngulo(
                  new Point( 0, Math.signum(posBall.getX() )* Settings.FIELD_WIDTH/2.0 ),  Math.toRadians(90) );
            
            Point posIntersect = lineSide.getIntersecao( lineBody );
            posBall = point.asVector();
            Vector2D n = new Vector2D();
            n.setCoordPolar(Math.toRadians(angGlobal), posIntersect.distanceTo( point ) - 0.2);
            posBall.mais(n);
        }
        
         Vector2D vecDesired = posBall;
         vecDesired.menos(bola.position.getPosition().asVector());
         
         Vector2D vecShoot   = vecDesired;
         vecShoot.menos(bola.velocity());
         double dPower       = vecShoot.magnitude() / getKickPowerRateAtual( bola ) ;
         double angActual    = Math.toDegrees(vecShoot.direction()) - angAgente; // graus
         angActual           = Futil.simplifyAngle( angActual );
         
         if( dPower > Settings.PLAYER_PARAMS.POWER_MAX  && bola.velocity().magnitude() > 0.1 ){
             dominarBola();
             return;
         }else if( dPower > Settings.PLAYER_PARAMS.POWER_MAX ){
            if( Futil.isBolaParadaParaNos(playMode, player.team.side, player.otherTeam.side) ) {
                if( bola.curInfo.direction > 25 ) {	
                    virarCorpoParaBola( bola );
                    return;
                }
            }
            else{
                dPower = 100;
            }
        }     
        kick(dPower, angActual);
    }
    
    public void procurarBola(){
        FieldObject bola    = this.getOrCreate(Ball.ID);
        int sinal           = 1;
        Point posBola       = bola.position.getPosition();
        Point posAgente     = player.position.getPosition();
        posBola.menos(posAgente);
        double angulo       = Math.toDegrees(posBola.asVector().direction());
        double anguloAgente = player.direction.getDirection();
        
        if(time == timeUltimoProcuraBola)
            return;
        
        if(time - timeUltimoProcuraBola > 3)
             sinal = ( Futil.isAnguloNoIntervalo(angulo, anguloAgente,Futil.simplifyAngle(anguloAgente+180) ) ) ? 1 : -1  ;
        
        timeUltimoProcuraBola   = time;
        Vector2D angTurn        = new Vector2D();
        angTurn.setCoordPolar(Math.toRadians(Futil.simplifyAngle(anguloAgente + 60 * sinal)), 1);
        posAgente.mais(angTurn.asPoint());
        
        turnBodyParaPonto(posAgente, sinal);
    }
    
    public void dominarBola(){
        FieldObject bola    = this.getOrCreate(Ball.ID);        
        PositionEstimate pe = Futil.predictAgentePosDepoisNCiclos(player, 0 , 1 , time, curSenseInfo);
        double power        = bola.velocity().magnitude() / getKickPowerRateAtual( bola );
        
        if(power > Settings.PLAYER_PARAMS.POWER_MAX){
            power = Settings.PLAYER_PARAMS.POWER_MAX;
        }
        
        double angulo   = Math.toDegrees(bola.velocity().direction()) + 180 - player.direction.getDirection(); // graus
        angulo          = Futil.simplifyAngle(angulo);
        
        Vector2D posBola        = bola.position.getPosition().asVector();
        VelocityVector velBola  = bola.velocity();
        
        Futil.predictInfoBolaDepoisComando("kick", posBola, velBola, power, angulo, player.direction.getDirection() , getKickPowerRateAtual( bola ));
        
        if(posBola.asPoint().distanceTo(pe.getPosition()) < 0.8 * Futil.kickable_radius()){
            kick(power, angulo);
            return;
        }
        
        posBola = bola.position.getPosition().asVector();
        
        Vector2D auxDir = posBola;
        auxDir.menos(pe.getPosition().asVector());
        
        Vector2D posTo  = pe.getPosition().asVector();
        Vector2D posAux = new Vector2D();
        posAux.setCoordPolar(auxDir.direction(), Math.min( 0.7 * Futil.kickable_radius(), posBola.asPoint().distanceTo(pe.getPosition() ) - 0.1 ));
        
        Vector2D velDes = posTo;
        velDes.menos(posBola);
        
        acelerarBolaAVelocidade(velDes);
        
    }
    
    public void intercept( boolean isGoleiro ){
        
    }
    
    public Point getPosicaoEstrategica(int numero, Formacao.LayoutFormacao[] formacao){
        FieldObject bola            = this.getOrCreate(Ball.ID);
        Point pos, posBola          = bola.position.getPosition();
        List<Player> todosNaVista   = companheirosVisiveis;
        todosNaVista.addAll(lastSeenOpponents);
        boolean nossaBola           = Futil.isNossaPosseDeBola(todosNaVista, bola, player.team.name);
        double maxX                 = Futil.getXImpedimento(lastSeenOpponents, bola);
        maxX                        = Math.max(-0.5 , maxX - 1.5);
        
        if(Futil.isGoalKick(playMode, player.otherTeam.side))
            maxX = Math.min(Settings.PENALTY_X - 1, maxX);
        else if(Futil.isBeforeKickOff(playMode))
            maxX = Math.min(-2, maxX);
        else if(Futil.isOffside(playMode, player.team.side))
            maxX = bola.position.getX() - 0.5;
        
        
        if(Futil.isBeforeKickOff(playMode) || bola.position.getConfidence(time) < Settings.CONFIANCA_BOLA)
            posBola = new Point(0,0);
        
        else if(Futil.isGoalKick(playMode, player.team.side) || (Futil.isFreeKick(playMode, player.team.side) &&
                posBola.getX() < - Settings .PENALTY_X))            
            posBola.setX( - Settings.FIELD_WIDTH / 4 + 5);
        
        else if(Futil.isGoalKick(playMode, player.otherTeam.side) || (Futil.isFreeKick(playMode, player.otherTeam.side) &&
                posBola.getX() > Settings .PENALTY_X))            
            posBola.setX( Settings.PENALTY_X - 10);
        
        else if(Futil.isFreeKick(playMode, player.otherTeam.side))
            posBola.setX( posBola.getX() - 5);
        
        else if(nossaBola && !(Futil.isBolaParadaParaEles(playMode, player.team.side, player.otherTeam.side)
                || Futil.isBolaParadaParaNos(playMode, player.team.side, player.otherTeam.side)))
            posBola.setX( posBola.getX() + 5.0 );
        
        else if( posBola.getX() < - Settings.PENALTY_X + 5.0 )
             posBola = Futil.predictlPosBolaDepoisNCiclos( bola, 3);
        
        return this.formacao.getPontoEstrategico(numero, posBola, maxX, nossaBola, Settings.MAX_Y_PORCENTAGEM , formacao);
    }
    
    public void turnBodyParaPonto( Point ponto, int ciclos ){
        PositionEstimate posGlobal = Futil.predictAgentePosDepoisNCiclos(player, 0, ciclos, time, curSenseInfo);
        Vector2D vet               = ponto.asVector();
        vet.menos(posGlobal.getPosition().asVector());
        double angulo              = Math.toDegrees(vet.direction());
        angulo                    -= player.direction.getDirection();
        angulo                     = Futil.simplifyAngle(angulo);
        angulo                     = Futil.getAnguloParaTurn(angulo, player.velocity().magnitude());
        turn(angulo);
    }
    
     public void turnBackBodyParaPonto( Point ponto, int ciclos ){
        PositionEstimate posGlobal = Futil.predictAgentePosDepoisNCiclos(player, 0, ciclos, time, curSenseInfo);
        Vector2D vet               = ponto.asVector();
        vet.menos(posGlobal.getPosition().asVector());
        double angulo              = Math.toDegrees(vet.direction());
        angulo                    -= player.direction.getDirection() + 180;
        angulo                     = Futil.simplifyAngle(angulo);
        angulo                     = Futil.getAnguloParaTurn(angulo, player.velocity().magnitude());
        turn(angulo);
    }
    
    public void dashParaPonto(Point ponto, int ciclos){
        double power = 100;//Futil.getPowerParaDash(ponto, player.direction.getDirection() , player.velocity() , curSenseInfo.effort , ciclos);
        dash(power);
    }
    
    public void moveToPos(Point posTo, double angQuandoVirar, double distTras, boolean moverParaTras, int ciclos){
        Point posAgente         = player.position.getPosition();
        Point posFinalAgente    = Futil.predictPosFinalAgente(posAgente, player.velocity());
        
        double anguloBody       = player.direction.getDirection(); // graus
        Point posAux            = posTo;
        posAux.menos(posFinalAgente);
        double anguloPos        = Math.toDegrees(posAux.asVector().direction()); // graus
               anguloPos        = Futil.simplifyAngle(anguloPos - anguloBody); // graus
               
        double anguloAtras      = Futil.simplifyAngle(anguloPos + 180);
        double dist             = posAgente.distanceTo(posTo);      
        
        logger.log("moveToPos");
        logger.log(posTo.render());
        logger.log("angulo Pos " + anguloPos);
        logger.log("angulo qnd virar " + angQuandoVirar);
        if(moverParaTras){
            if( Math.abs( anguloAtras ) < angQuandoVirar ){
                dashParaPonto(posTo, ciclos );                
            }else{                
                turnBackBodyParaPonto( posTo , 1 );
            }
        }
        else if( Math.abs( anguloPos ) < angQuandoVirar || (Math.abs( anguloAtras ) < angQuandoVirar && dist < distTras )){
            dashParaPonto( posTo, ciclos );            
        }else{
            System.out.println(player.render() + "frente turn");
            dashTo(posTo, angQuandoVirar );
        }
        
    }
    
    public void directTowards(Point pontoTo, double angQuandoVirar){
        Vector2D velAgente  = player.velocity();
        Point posAgente     = player.position.getPosition();
        double angAgente    = player.direction.getDirection();  // graus
        Point posAux        = pontoTo;
        Point posPredAgente = Futil.predictPosFinalAgente(posAgente, player.velocity());
        posAux.menos(posPredAgente);
        
        double anguloFinal  = Math.toDegrees(posAux.asVector().direction()); // graus
        double angulo       = Futil.simplifyAngle(anguloFinal - angAgente); // graus
        double angPescoco   = 0;
        logger.log("Direct Towards");
        logger.log(pontoTo.render());
        logger.log("angulo = " + angulo);
        logger.log("ang Quando virar = " + angQuandoVirar);
        logger.log("--------------------------------------------------");
        
        int turns = 0;
        double[] result;
        while(Math.abs(angulo) > angQuandoVirar && turns < 5){
            turns++;
            result = Futil.predictEstadoAfterTurn(Futil.getAnguloParaTurn(angulo, velAgente.magnitude()),
                    posAgente, velAgente, angAgente, angPescoco, curSenseInfo);
            
            angAgente   = result[0];
            angPescoco  = result[1];
            angulo      = Futil.simplifyAngle( anguloFinal - angAgente );
        }   
       
        posAgente   = player.position.getPosition();       
        
        switch( turns ) {
            case 0:   
                System.out.println("erro directTowards turns == 0");
                return;
            case 1: 
            case 2:
                turnBodyParaPonto(pontoTo, 2 );
                break;
            default:
                dashParaPonto( posAgente, 1 );  // stop
                break;
        }
    }
    
    //calcula a taxa de ruído para dar um chute com precisão
    public static double getKickPowerRateAtual( FieldObject bola ) {
        // true indicates that relative angle to body should be returned
        double dir_diff      = Math.abs( bola.curInfo.direction );
        double dist          = bola.curInfo.distance - Settings.PLAYER_PARAMS.PLAYER_SIZE - Settings.BALL_PARAMS.BALL_SIZE;
        return Settings.PLAYER_PARAMS.KICK_POWER_RATE * ( 1 - 0.25 * dir_diff/180.0 - 0.25 * dist / Settings.PLAYER_PARAMS.KICKABLE_MARGIN);
    }
}
