import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class Brain implements Runnable {
   

    ///////////////////////////////////////////////////////////////////////////
    // MEMBER VARIABLES
    ///////////////////////////////////////////////////////////////////////////    
    Cliente client;
    Jogador player;
    public int time;
    public int cicloUltimaVisao;
    public int cicloUltimaSenseInfo;
    public int timeUltimoProcuraBola = 0;

    // Self info & Play mode
    private String playMode;
    private InfoCorpo curSenseInfo, lastSenseInfo;
    //public VetorDeAceleracao acceleration;
    //public VetorVelocidade velocity;
    private boolean isPositioned = false;      
    HashMap<String, Objetos> fieldObjects = new HashMap<>(100);
    ArrayDeque<String> hearMessages = new ArrayDeque<>();
    LinkedList<Jogador> lastSeenOpponents = new LinkedList<>();
    LinkedList<Jogador> companheirosVisiveis = new LinkedList<>(); // variavel que guarda todos os companheiros visiveis para o jogador no ultimo ciclo
    LinkedList<Configuracoes.RESPONSE> responseHistory = new LinkedList<>();
    private long timeLastSee = 0;
    private long timeLastSenseBody = 0;
    private int lastRan = -1;   
    private int noSeeBallCount = 0;
    private final int noSeeBallCountMax = 45;
    private MiniLogger logger; 
    private Formacao formacao;
    private double timePasse = 0;
    
    
    

    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    ///////////////////////////////////////////////////////////////////////////
    /**
     * This is the primary constructor for the Brain class.
     *
     * @param player a back-reference to the invoking player
     * @param client the server cliente by which to send commands, etc.
     */
    public Brain(Jogador player, Cliente client) {
        this.player = player;
        this.client = client;
        this.curSenseInfo = new InfoCorpo();
        this.lastSenseInfo = new InfoCorpo();       
        // Load the HashMap
        for (int i = 0; i < Configuracoes.OBJETOS_FIXOS.length; i++) {
            ObjetoFixo object = Configuracoes.OBJETOS_FIXOS[i];
            //client.log(Log.DEBUG, String.format("Adding %s to my HashMap...", object.id));
            fieldObjects.put(object.id, object);
        }
        // Load the response history
        this.responseHistory.add(Configuracoes.RESPONSE.NONE);
        this.responseHistory.add(Configuracoes.RESPONSE.NONE);
        this.formacao = new Formacao();
    }

    ///////////////////////////////////////////////////////////////////////////
    // GAME LOGIC
    ///////////////////////////////////////////////////////////////////////////        
    /**
     * Returns the direction, in radians, of the player at the current ciclo.
     */
    private final double dir() {
        return Math.toRadians(this.player.direction.getDirection());
    }  

    /**
     * Checks if the play mode allows Move commands.
     *
     * @return true if move commands can be issued
     */
    private final boolean canUseMove() {
        return (playMode.equals("before_kick_off")
                || playMode.startsWith("goal_")
                || playMode.startsWith("free_kick_")
                || playMode.startsWith("corner_kick_"));
    }

    private boolean bolaRolando() {
        return playMode.equals("play_on");
    }
    
    public Jogador melhorOpcaoDePasse(){
        Jogador melhorOp     = null;
        double dist         = 1000d;
        double d;
        for (Jogador cpnher : companheirosVisiveis) {
            if(melhorOp == null){
                melhorOp = cpnher;
            }else{
                d               = Util.distanciaEntre2Objetos(cpnher, player);
                if(player.time.side == 'l')
                    if(cpnher.posicao.getX() >= melhorOp.posicao.getX() && d < dist){
                        melhorOp    = cpnher;
                        dist        = d;
                    }
                else
                    if(cpnher.posicao.getX() <= melhorOp.posicao.getX() && d < dist){
                        melhorOp    = cpnher;
                        dist        = d;
                    }
                }
         }
        return melhorOp;
    }
    
    public boolean acabeiDeEfetuarPasse(){
        if(timePasse == 0)
            return false;
        else{
            if(this.time - this.timePasse < 10)
                return true;
            return false;
        }
    }

    /**
     * Returns an estimate of whether the player can kick the ball, dependent on
     * its distance to the ball and whether it is inside the playing field.
     *
     * @return true if the player is on the field and within kicking distance
     */
    public final boolean canKickBall() {
        Objetos ball = this.getOrCreate(Bola.ID);
        return this.player.inRectangle(Configuracoes.CAMPO) && ball.curInfo.ciclo >= this.time - 1
                && ball.curInfo.distance < Util.kickable_radius();
    }

    /**
     * Returns an indication of whether a given ObjectId was seen in the current
 ciclo step.
     *
     * @return true if the given ObjectId was seen in the current soccer server
 ciclo step
     */
    public final boolean canSee(String id) {
        return this.getOrCreate(id).curInfo.ciclo == this.time;
    }

    /**
     * Accelerates the player in the direction of its body.
     *
     * @param power the power of the acceleration (0 to 100)
     */
    private final void dash(double power) {
        // Update this player's acceleration           
        escreverNoLog("dash " + power);
        this.client.enviaComando(Configuracoes.Commands.DASH, Double.toString(power));
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
        client.enviaComando(Configuracoes.Commands.DASH, Double.toString(power), Double.toString(offset));
    }

    /**
     * Returns this player's effective dash power. Refer to the soccer server
     * manual for more information.
     *
     * @return this player's effective dash power
     */
    private final double edp(double power) {
        System.out.println(this.effort());
        return this.effort() * Configuracoes.DASH_POWER_RATE * power;
    }

    /**
     * Returns an effort value for this player. If one wasn't received this ciclo
 step, we guess.
     */
    private final double effort() {
        return this.curSenseInfo.effort;
    }   

    private boolean souOJogadorDoTimeMaisProximoDoObjeto(Objetos objeto) {
        double dist = objeto.curInfo.distance;
        for (Jogador companheiro : companheirosVisiveis) {
            if (Util.distanciaEntre2Objetos(companheiro, objeto) < dist) {
                return false;
            }
        }       
        return true;
    }
    
    private Jogador adversarioMaisProximoDoObjeto(Objetos objeto) {
        Jogador maisProximo  = null;
        double dist         = 1000d;
        for (Jogador adversario : lastSeenOpponents) {
            if (Util.distanciaEntre2Objetos(adversario, objeto) < dist) {
                maisProximo = adversario;
            }
        }       
        return maisProximo;
    }

    private boolean adversarioMarcando(Jogador player) {
        Ponto posAgente       = player.posicao.getPosicao();
        Retangulo areaMarcacao;
        if(player.time.side == 'l')
            areaMarcacao = new Retangulo(posAgente.getY() - 7 ,posAgente.getX() + 10, posAgente.getY() +7 ,posAgente.getX());
        else
            areaMarcacao = new Retangulo(posAgente.getY() - 7 ,posAgente.getX(), posAgente.getY() + 7 ,posAgente.getX() - 10);
        
        for (Jogador adversario : lastSeenOpponents) {
            if (areaMarcacao.contains(adversario)) {
                return true;
            }
        }
        return false;
    }   

    private void passarABola(Objetos bola , Ponto parca) {
        double angulo           = calcularAnguloRelativoAoBody(parca, 1);
        double anguloCorpoBola  = bola.curInfo.direction + curSenseInfo.headAngle;
        
        double forca = (player.posicao.getPosicao().distanciaAoPonto(parca) * 7.0 / 2.0) * (1 + 0.5 * Math.abs(anguloCorpoBola / 180) + 0.5 * (bola.curInfo.distance / Util.kickable_radius()));
        forca        = forca > Configuracoes.JOGADOR_PARAMS.POWER_MAX ? Configuracoes.JOGADOR_PARAMS.POWER_MAX : forca;
        kick(forca, angulo);
        timePasse = this.time;
              
    }
    
    private void chutarProGol(){
        
        double angulo       = calcularAnguloRelativoAoBody(player.getPontoGolOponente(), 1);
        //int anguloRand      = new Random().nextInt(4);
        //int sinal           = new Random().nextInt(2) == 1 ? 1 : -1;
        //angulo             += sinal * anguloRand;
        //chutarPara(player.getPontoGolOponente(), Configuracoes.BOLA_PARAMS.BALL_SPEED_MAX);
        kick(100, angulo);       
    }
    
    public boolean devoPassarABola(){
        Formacao.POSICAO posicao = formacao.getPosicao(player.numero);
        double posX              = formacao.getXmax(player.numero, player.time.side);
        boolean foraPosicao      = false;
        
        if(posX < player.posicao.getX() && player.time.side == 'l')
            foraPosicao = true;
        
        if(posX > player.posicao.getX() && player.time.side == 'R')
            foraPosicao = true;
        
        Jogador p = melhorOpcaoDePasse();
        
        switch(posicao){
            case ATACANTE:
                return adversarioMarcando(player) && p != null;                
            case LATERAL:
            case MEIO_CAMPISTA:
            case MEIO_CAMPISTA_LATERAL:
            case PONTA_ATACANTE:
            case VOLANTE:
            case ZAGUEIRO:               
                return (p != null && foraPosicao) || (adversarioMarcando(player) && p != null);                
            default:
                return false;                
        }
    }
    
    private void determinarEstrategia() {
        Formacao.POSICAO posicao = formacao.getPosicao(player.numero);
        
        //Adicionar métodos caso deseje implementar estrategias diferentes para cada posição;
        switch(posicao){
            case GOLEIRO:
            case ATACANTE:
            case LATERAL:
            case MEIO_CAMPISTA:
            case MEIO_CAMPISTA_LATERAL:
            case PONTA_ATACANTE:
            case VOLANTE:
            case ZAGUEIRO:
                estrategiaPadrao();
                break;
        }
    }
     
    public void estrategiaPadrao(){
        try {
            
            Objetos bola     = this.getOrCreate(Bola.ID);
            Ponto posAgente      = player.posicao.getPosicao();
            Ponto posBola        = bola.posicao.getPosicao();
            Ponto posEstrategica;
                        
            if(canUseMove()){
                
                if( !isPositioned ) {
                    formacao.setFormacao(Formacao.FORMACAO_INICIAL);
                    isPositioned = true;
                    posEstrategica = getPosicaoEstrategica(player.numero, formacao.formacaoEmCurso);
                    if(player.time.side == 'r'){
                        posEstrategica.setX(-posEstrategica.getX());
                        posEstrategica.setY(-posEstrategica.getY());
                    }
                    move( posEstrategica );
                    System.out.println(player.renderizar());
                }
                else {
                    alinharPescocoECorpo();
                }
                
            }else if(bolaRolando()){
                /*if(player.posicao.getPosicao().distanciaAoPonto(new Ponto(50,0)) > 2)
                    correrProPontoVirandoOPescoco(new Ponto(50,0), 1);*/
                                    
                isPositioned = false;
                //get
                               
                if(bola.posicao.getConfianca(time) < Configuracoes.CONFIANCA_BOLA){
                    procurarBola();
                    alinharPescocoECorpo();
                                        
                }else if(canKickBall()){                    
                    Jogador p = adversarioMaisProximoDoObjeto(player);
                    if(bola.velocity().magnitude() > 1){
                        dominarBola();
                        System.out.println("dominando a bola");
                    }else if(getEnemyPenaltyArea().contains(player)){ 
                        chutarProGol();                       
                        System.out.println("chutar para o gol");
                    }else if(Math.abs(player.posicao.getX()) > 40){
                        chutarPara(getEnemyPenaltyArea().getCentro(), Configuracoes.BOLA_PARAMS.BALL_SPEED_MAX);
                        
                    }else if(devoPassarABola()){
                        Jogador c = melhorOpcaoDePasse();
                        if(c == null)
                            procurarBola();
                        else{
                            Ponto pontoParceiro = Util.predictPosDeOutroJogador(c, 2);
                            passeDireto(pontoParceiro, true);
                            //System.out.println("passando a bola");
                        }
                        //System.out.println("marcado");
                    }else{
                        System.out.println("avançando");
                        double dire = Util.simplifyAngle(direcaoPraFrente(player.time.side) - bodyDirection());
                        if(Math.abs(dire) > 90){
                              if(bola.velocity().magnitude() > 1){
                                dominarBola();
                              }else{
                                  turnBodyParaDirecao(direcaoPraFrente(player.time.side));
                              }
                        }
                        chutarBolaProximaAoCorpo(Util.simplifyAngle(direcaoPraFrente(player.time.side) - bodyDirection()), 0.4);
                    }                    
                }else if(souOJogadorDoTimeMaisProximoDoObjeto(bola) && !acabeiDeEfetuarPasse()){
                    if(!colidirComABola()){
                      //alinharPescocoECorpo();
                      Ponto pontoIntersecao = getPontoDeIntersecaoBola();
                      if(pontoIntersecao == null)
                        correrProPontoVirandoOPescoco(Util.predictlPosBolaDepoisNCiclos(bola, 1), 1);
                      else
                        correrProPontoVirandoOPescoco(pontoIntersecao, 1);  
                      //System.out.println("corendo atras da bola");     
                    }                           
                }
                
                else{
                    formacao.setFormacao(Formacao.FORMACAO_433_OFENSIVO);
                    posEstrategica = getPosicaoEstrategica(player.numero, formacao.formacaoEmCurso);
                    //System.out.println(posEstrategica.renderizar());
                    if(player.posicao.getPosicao().distanciaAoPonto(posEstrategica) > 2)
                        correrProPontoVirandoOPescoco(posEstrategica , 1);
                    else{
                        alinharPescocoECorpo();
                        virarCorpoParaBola(bola);
                    }
                        
                    //System.out.println("indo para formação");
                }             
            }
                        
        } catch (Exception e) {
            e.printStackTrace();
            Scanner s = new Scanner(System.in);
            s.next();
        }      
    }

    private void ordenarPorDistancia(List<Jogador> listaPlayers, Objetos bola) {
        Jogador aux;
        for (int i = 0; i < listaPlayers.size(); i++) {
            for (int j = i + 1; j < listaPlayers.size(); j++) {
                Jogador iP = listaPlayers.get(i);
                Jogador jP = listaPlayers.get(j);
                if (iP.distanceTo(bola) > jP.distanceTo(bola)) {
                    aux = iP;
                    listaPlayers.set(i, jP);
                    listaPlayers.set(j, aux);
                }
            }
        }
    }
    
    

    private void avancarComABola() {
        double targetFacingDir = 0.0;
        Objetos golAdversario = this.getOrCreate(this.player.getIdGolOponente());
        if (getEnemyPenaltyArea().contains(player)) {
            kick(100, player.relativeAngleTo(golAdversario));
        } else {
            if (Math.abs(player.posicao.getX()) > 40) {
                //cruzarParaArea();
            } else {
                if (this.player.time.side == 'r') {
                    targetFacingDir = -180.0;
                }
                if (Math.abs(Util.simplifyAngle(targetFacingDir - bodyDirection())) > Configuracoes.ANGULO_PARA_USAR_TURN) {
                    this.turnBodyParaDirecao(targetFacingDir);
                } else {
                    kick(15, 0);
                }
            }
        }
    }

    /*private void driblar() {
        Objetos bola = this.getOrCreate(Bola.ID);
        Vetor2D v_new = Util.estimatePositionOf(this.player, 2, this.ciclo).getPosicao().umVetor();
        Vetor2D v_target = v_new.add(findDribbleAngle());
        Vetor2D v_ball = v_target.add(new Vetor2D(-1 * bola.posicao.getX(),
                -1 * bola.posicao.getY()));

        double traj_power = Math.min(Configuracoes.JOGADOR_PARAMS.POWER_MAX,
                (v_ball.magnitude() / (1 + Configuracoes.BOLA_PARAMS.BALL_DECAY)) * 10); // values of 1 or 2 do not give very useful kicks.
        this.kick(traj_power + 20, Util.simplifyAngle(Math.toDegrees(v_ball.direction())));
    }*/

    private void goleiroEstrategia() {
//       trycatch (Exception e) {
//            e.printStackTrace();
//            Scanner s = new Scanner(System.in);
//            s.next();
//        } 
    }

    

    private void olharParaFrente() {
        double direcaoPF = direcaoPraFrente(player.time.side);
        double anguloTurn = Util.simplifyAngle(direcaoPF - player.direction.getDirection());
        turn(anguloTurn);
    }

   
    /**
     * Finds the optimal angle to kick the ball toward within a kickable area.
     *
     * @param p Ponto to build angle from
     * @return the vector to dribble toward.
     */
   /* private final Vetor2D findDribbleAngle() {
        // TODO STUB: Need algorithm for a weighted dribble angle.
        double d_length = Math.max(1.0, Util.kickable_radius());

        // 5.0 is arbitrary in case nothing is visible; attempt to kick
        //   toward the lateral center of the field.
        double d_angle = 5.0 * -1.0 * Math.signum(this.player.posicao.getY());

        // If opponents are visible, try to kick away from them.
        if (!lastSeenOpponents.isEmpty()) {
            double weight = 0.0d;
            double w_angle = 0.0d;
            for (Objetos i : lastSeenOpponents) {
                double i_angle = player.relativeAngleTo(i);
                double new_weight = Math.max(weight, Math.min(1.0,
                        1 / player.distanciaAoPonto(i) * Math.abs(
                                1 / (i_angle == 0.0 ? 1.0 : i_angle))));
                if (new_weight > weight) {
                    w_angle = i_angle;
                }
            }

            // Keep the angle within [-90,90]. Kick forward, not backward!
            d_angle = Math.max(Math.abs(w_angle) - 180, -90) * Math.signum(w_angle);
        } // Otherwise kick toward the goal.
        else if (this.canSee(this.player.getIdGolOponente())) {
            d_angle += this.player.relativeAngleTo(
                    this.getOrCreate(this.player.getIdGolOponente()));
        }
        Vetor2D d_vec = new Vetor2D(0.0, 0.0);
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
    //}

    /**
     * Gets the requested `Objetos` from fieldObjects, or creates it if it
 doesn't yet exist.
     *
     * @param id the object's id
     * @return the field object
     */
    private final Objetos getOrCreate(String id) {
        if (this.fieldObjects.containsKey(id)) {
            return this.fieldObjects.get(id);
        } else {
            return Objetos.create(id);
        }
    }

    private int direcaoPraFrente(char time) {
        if (time == Configuracoes.LEFT_SIDE) {
            return 0;
        } else {
            return -180;
        }
    }

    private boolean isValorProximo(double valor1, double valor2, double limite) {
        return Math.abs(valor2 - valor1) <= limite;
    }

    /**
     * Infers the posicao and direction of this brain's associated player given
 two boundary flags on the same side seen in the current ciclo step.
     *
     * @param o1 the first flag
     * @param o2 the second flag
     */
    private final void inferPositionAndDirection(Objetos o1, Objetos o2) {
        // x1, x2, y1 and y2 are relative Cartesian coordinates to the flags
        double x1 = Math.cos(Math.toRadians(o1.curInfo.direction)) * o1.curInfo.distance;
        double y1 = Math.sin(Math.toRadians(o1.curInfo.direction)) * o1.curInfo.distance;
        double x2 = Math.cos(Math.toRadians(o2.curInfo.direction)) * o2.curInfo.distance;
        double y2 = Math.sin(Math.toRadians(o2.curInfo.direction)) * o2.curInfo.distance;
        double direction = -Math.toDegrees(Math.atan((y2 - y1) / (x2 - x1)));
        // Need to reverse the direction if looking closer to west and using horizontal boundary flags
                
        if (o1.posicao.getY() == o2.posicao.getY()) {
            if (Math.signum(o2.posicao.getX() - o1.posicao.getX()) != Math.signum(x2 - x1)) {
                direction += 180.0;
            }
        } // Need to offset the direction by +/- 90 degrees if using vertical boundary flags
        else if (o1.posicao.getX() == o2.posicao.getX()) {
            if (Math.signum(o2.posicao.getY() - o1.posicao.getY()) != Math.signum(x2 - x1)) {
                direction += 270.0;
            } else {
                direction += 90.0;
            }
        }
        
        double minusX = o1.curInfo.distance * Math.cos(Math.toRadians(direction + o1.curInfo.direction));
        double minusY = o1.curInfo.distance * Math.sin(Math.toRadians(direction + o1.curInfo.direction));
                
        this.player.direction.update(Util.simplifyAngle(direction), 0.95, this.time);
        double x = o1.posicao.getX() - minusX;
        double y = o1.posicao.getY() - minusY;
        
        if(Math.abs(x) > 57 || Math.abs(y) > 40){            
            return;
        }
        
        this.player.posicao.atualizar(x, y, 0.95, this.time);
    }
    
    public void infereVelocidadeAgente(){
        double magnitude    = curSenseInfo.amountOfSpeed;
        double angulo       = curSenseInfo.directionOfSpeed;
        
        player.velocidade.setCoordPolar(Math.toRadians(angulo) + this.dir(), magnitude);
    }

   

    /**
     * Moves the player to the specified soccer server coordinates.
     *
     * @param p the Ponto object to pass coordinates with (must be in server
 coordinates).
     */
    public void move(Ponto p) {
        move(p.getX(), p.getY());
    }

    /**
     * Moves the player to the specified soccer server coordinates.
     *
     * @param x x-coordinate
     * @param y y-coordinate
     */
    public void move(double x, double y) {
        client.enviaComando(Configuracoes.Commands.MOVE, Double.toString(x), Double.toString(y));
        escreverNoLog("move (" + x + " , " + y + ")");
        this.player.posicao.atualizar(x, y, 1.0, this.time);
    }

    public void turnNeck(double angulo) {
        escreverNoLog("turn_neck " + angulo);
        client.enviaComando(Configuracoes.Commands.TURN_NECK, angulo);
    }

    /**
     * Kicks the ball in the direction of the player.
     *
     * @param power the level of power with which to kick (0 to 100)
     */
    public void kick(double power) {
        escreverNoLog("kick " + power);
        client.enviaComando(Configuracoes.Commands.KICK, Double.toString(power));
    }

    /**
     * Captura a bola no angulo que ela está em relação ao goleiro. (Apenas
     * goleiros podem usar esta habilidade)
     */
    public void capturarBola() {
        Objetos bola = getOrCreate(Bola.ID);
        escreverNoLog("catch " + Double.toString(bola.curInfo.direction));
        client.enviaComando(Configuracoes.Commands.CATCH, Double.toString(bola.curInfo.direction));
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
        client.enviaComando(Configuracoes.Commands.KICK, Double.toString(power), Double.toString(offset));
    }

    public void escreverNoLog(String mensagem){
        //logger.log(ciclo + " - ( " +mensagem+ " )");
    }
    
    /**
     * Parses a message from the soccer server. This method is called whenever a
     * message from the server is received.
     *
     * @param message the message (string), exactly as it was received
     */
    public void parseMessage(String message) {
        long timeReceived = System.currentTimeMillis();
        message = Util.sanitize(message);
        // Handle `sense_body` messages
        if (message.startsWith("(sense_body")) {
            //System.out.println(this.ciclo);
            curSenseInfo.copy(lastSenseInfo);
            curSenseInfo.reset();

            this.timeLastSenseBody = timeReceived;
            curSenseInfo.ciclo = Util.extractTime(message);
            logger.log(curSenseInfo.ciclo + " : sense_body ");
            this.cicloUltimaSenseInfo = curSenseInfo.ciclo;
            //this.ciclo = curSenseInfo.ciclo;

            String parts[] = message.split("\\(");
            // System.out.println(this.ciclo);
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
                if (nArgs[0].contains("view_mode")) { // Jogador's current view mode
                    curSenseInfo.viewQuality = nArgs[1];
                    curSenseInfo.viewWidth = nArgs[2];
                } else if (nArgs[0].contains("stamina")) { // Jogador's stamina data
                    curSenseInfo.stamina = Double.parseDouble(nArgs[1]);
                    curSenseInfo.effort = Double.parseDouble(nArgs[2]);
                    curSenseInfo.staminaCapacity = Double.parseDouble(nArgs[3]);
                } else if (nArgs[0].contains("speed")) { // Jogador's speed data
                    curSenseInfo.amountOfSpeed = Double.parseDouble(nArgs[1]);
                    curSenseInfo.directionOfSpeed = Double.parseDouble(nArgs[2]);
                    // Update velocity variable
                    this.infereVelocidadeAgente();
                } else if (nArgs[0].contains("head_angle")) { // Jogador's head angle
                    curSenseInfo.headAngle = Double.parseDouble(nArgs[1]);
                } else if (nArgs[0].contains("ball") || nArgs[0].contains("player")
                        || nArgs[0].contains("post")) { // COLLISION flags; limitation of this loop approach is we
                    //   can't handle nested parentheses arguments well.
                    // Luckily these flags only occur in the collision structure.
                    curSenseInfo.collision = nArgs[0];
                }
            }
            
            

            // If the brain has responded to two see messages in a row, it's ciclo to respond to a sense_body.
            if (this.responseHistory.get(0) == Configuracoes.RESPONSE.SEE && this.responseHistory.get(1) == Configuracoes.RESPONSE.SEE) {
                this.run();
                this.responseHistory.push(Configuracoes.RESPONSE.SENSE_BODY);
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
                        && Configuracoes.PLAY_MODES.contains(nMsg)) // Play Mode?
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
            //System.out.println(player.renderizar() + " Diferença entre ultimo ciclo e o atual :  "  + (timeSee - timeLastSee)/150 + " ciclos");
            this.timeLastSee = timeSee;
            this.time = Util.extractTime(message);
            logger.log(time + " : see ");
            LinkedList<String> infos = Util.extractInfos(message);
            lastSeenOpponents.clear();
            companheirosVisiveis.clear();
            for (String info : infos) {
                String id = Util.extractId(info);
                if (Util.isUniqueFieldObject(id)) {
                    if(Util.isFlag(id)){
                        //System.out.println(id);
                        Objetos obj = this.getOrCreate(id);
                        obj.update(this.player, info, this.time);
                        this.fieldObjects.put(id, obj);
                   }
                }
            }
            // Immediately run for the current step. Since our computations takes only a few
            // milliseconds, it's okay to start running over half-way into the 100ms cycle.
            // That means two out of every three ciclo steps will be executed here.
            this.updatePositionAndDirection();
            
            for (String info : infos) {
                String id = Util.extractId(info);
                if (Util.isUniqueFieldObject(id)) {
                    if(!Util.isFlag(id)){
                        Objetos obj = this.getOrCreate(id);
                        obj.update(this.player, info, this.time);
                        this.fieldObjects.put(id, obj);
                        if (id.startsWith("(p \"") && !(id.startsWith(this.player.time.nome, 4))) {
                            lastSeenOpponents.add((Jogador) obj);
                        }
                        if (id.startsWith("(p \"") && (id.startsWith(this.player.time.nome, 4))) {
                            companheirosVisiveis.add((Jogador) obj);
                        }
                    }
                }
            }
            
            
            this.run();
            // Make sure we stay in sync with the mid-way `see`s
            if (this.timeLastSee - this.timeLastSenseBody > 30) {
                this.responseHistory.clear();
                this.responseHistory.add(Configuracoes.RESPONSE.SEE);
                this.responseHistory.add(Configuracoes.RESPONSE.SEE);
            } else {
                this.responseHistory.add(Configuracoes.RESPONSE.SEE);
                this.responseHistory.removeLast();
            }
            //Keep track of steps since the ball was last seen
            if (canSee(Bola.ID)) {
                noSeeBallCount = 0;
                //System.out.println(player.renderizar() + " Não vejo a bola");
            } else {
                noSeeBallCount++;
            }

        } // Handle init messages
        else if (message.startsWith("(init")) {
            String[] parts = message.split("\\s");
            char teamSide = message.charAt(6);
            if (teamSide == Configuracoes.LEFT_SIDE) {
                player.time.side = Configuracoes.LEFT_SIDE;
                player.outroTime.side = Configuracoes.RIGHT_SIDE;
            } else if (teamSide == Configuracoes.RIGHT_SIDE) {
                player.time.side = Configuracoes.RIGHT_SIDE;
                player.outroTime.side = Configuracoes.LEFT_SIDE;
            } else {
                // Raise error
                System.out.println("Could not parse teamSide.");
            }
            player.numero = Integer.parseInt(parts[2]);
            playMode = parts[3].split("\\)")[0];
            logger = new MiniLogger(player.time.nome + "_" + player.numero + ".txt");
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
                Configuracoes.setDashPowerRate(Double.parseDouble(nArgs[1]));
            }
            if (nArgs[0].startsWith("goal_width")) {
                Configuracoes.setAlturaGol(Double.parseDouble(nArgs[1]));
            } // Bola arguments:
            else if (nArgs[0].startsWith("ball")) {
                ConfiguracoesBola.Builder.dataParser(nArgs);
            } // Jogador arguments:
            else if (nArgs[0].startsWith("player") || nArgs[0].startsWith("min")
                    || nArgs[0].startsWith("max")) {
                ConfiguracoesJogador.Builder.dataParser(nArgs);
            }
        }

        // Rebuild all parameter objects with updated parameters.
        Configuracoes.reconstruirParametros();
    }

    /**
     * Returns this player's ciclo's goal.
     *
     * @return this player's ciclo's goal
     */
    public final Objetos ownGoal() {
        return this.getOrCreate(this.player.getIdGol());
    }

    /**
     * Returns the penalty area of this player's ciclo's goal.
     *
     * @return the penalty area of this player's ciclo's goal
     */
    public final Retangulo ownPenaltyArea() {
        if (this.player.time.side == 'l') {
            return Configuracoes.AREA_PENALTI_ESQUERDA;
        } else {
            return Configuracoes.AREA_PENALTI_DIREITA;
        }
    }

    /**
     * Responds for the current ciclo step.
     */
    public void run() {
        int expectedNextRun = this.lastRan + 1;
        if (this.time > this.lastRan + 1) {
            //Log.e("Brain for player " + this.player.renderizar() + " did not run during ciclo step " + expectedNextRun + ".");
        }
        this.lastRan = this.time;
        
        if (!player.goleiro) {
            //this.currentStrategy = this.determineOptimalStrategy();
            // this.executeStrategy(this.currentStrategy);
            this.determinarEstrategia();
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
        double moment = Util.toValidMoment(offset);
        escreverNoLog("turn "  + offset);
        client.enviaComando(Configuracoes.Commands.TURN, moment);
        // TODO Potentially take magnitude of offset into account in the
        // determination of the new confidence in the player's posicao.
       // player.direction.atualizar(player.direction.getDirection() + moment, 0.95 * player.direction.getConfianca(this.ciclo), this.ciclo);
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
     * 
     */
    public double bodyDirection(){
        return player.direction.getDirection() - curSenseInfo.headAngle;
    }
    
    /**
     * Directs the player to dash to a given point, turning if necessary.
     *
     * @param point the point to dash to
     */
    private final void dashTo(Ponto point) {
        dashTo(point, 1);
    }

    /**
     * Directs the player to dash to a given point, turning if necessary.
     *
     * @param ponto the point to dash to
     * @param power the power at which to dash
     */
    private final void dashTo(Ponto ponto, int ciclos) {
                
        Ponto posGlobal            = Util.predictAgentePosDepoisNCiclos(player, 0, ciclos, time, curSenseInfo).getPosicao();
        Vetor2D vet               = ponto.umVetor();
        vet.menos(posGlobal.umVetor());
        double angulo              = Math.toDegrees(vet.direction());
        angulo                    -= bodyDirection();
        angulo                     = Util.simplifyAngle(angulo);
        
        if (Math.abs(angulo) > Configuracoes.ANGULO_PARA_USAR_TURN) {
            turnBodyParaPonto(ponto, ciclos);
        } else {
            dashParaPonto(ponto, ciclos);
        }
    }
    
    public void correrProPontoVirandoOPescoco(Ponto ponto, int ciclos){
       // Objetos bola           = this.getOrCreate(Bola.ID);
        
        Ponto posGlobal            = Util.predictAgentePosDepoisNCiclos(player, 0, ciclos, time, curSenseInfo).getPosicao();
        Vetor2D vet               = ponto.umVetor();
        vet.menos(posGlobal.umVetor());
        double angulo              = Math.toDegrees(vet.direction());
        angulo                    -= bodyDirection();
        angulo                     = Util.simplifyAngle(angulo);
        
        double angPescoco          = calcularAnguloRelativoACabeca(new Ponto(0,-32), 1);
        System.out.println(angPescoco);
        
        if (Math.abs(angulo) > Configuracoes.ANGULO_PARA_USAR_TURN) {
            double angPesc   = turnBodyParaPonto(ponto, ciclos);
            //double angulos[] = Util.predictEstadoAfterTurn(angPesc, ponto, vet, angPesc, angPesc, curSenseInfo);
           // angPesc         = Util.toValidMomentNeck(angPesc);
            turnNeck(-angPesc);
        } else {
            
           /* if(Math.abs(angPescoco) > Configuracoes.ANGULO_PARA_VIRAR_PESCOCO){
                double relNeck      = curSenseInfo.headAngle;
                double angleToTurn  = Util.validarTurNeckAngle(angPescoco, relNeck);
                turnNeck(angleToTurn);
            }  */         
            
            dashParaPonto(ponto, ciclos);
        }
    }
    
    public void virarPescocoDepoisTurn(String comando,double angulo, double power){
        double angulosPosTurn[];
        Ponto posAgente = new Ponto(player.posicao.getPosicao());
        if(comando.equals("turn")){
            
        }
    }

    /**
     * Updates this this brain's belief about the associated player's posicao
 and direction at the current ciclo step. This method should be called
     * immediately after parsing a `see` message, and only then.
     */
    private final void updatePositionAndDirection() {
        // Infer from the most-recent `see` if it happened in the current ciclo-step
        for (int i = 0; i < 4; i++) {
            LinkedList<Objetos> flagsOnSide = new LinkedList<Objetos>();
            for (String id : Configuracoes.GRUPO_FLAGS_EXTERNAS[i]) {
                Objetos flag = this.fieldObjects.get(id);
                if (flag.curInfo.ciclo == this.time) {
                    flagsOnSide.add(flag);
                } else {
                    //Log.i("Flag " + id + "last updated at ciclo " + flag.info.ciclo + ", not " + this.ciclo);
                }
                if (flagsOnSide.size() > 1) {
                    this.inferPositionAndDirection(flagsOnSide.poll(), flagsOnSide.poll());
                    return;
                }
            }
        }
    }

    /**
     * @return {@link Configuracoes#AREA_PENALTI_ESQUERDA} if player is on the left ciclo,
 or {@link Configuracoes#AREA_PENALTI_DIREITA} if on the right ciclo.
     */
    final public Retangulo getMyPenaltyArea() {
        if (player.time == null) {
            throw new NullPointerException("Player team not initialized while getting penelty area.");
        }
        return player.time.side == 'l' ? Configuracoes.AREA_PENALTI_ESQUERDA : Configuracoes.AREA_PENALTI_DIREITA;
    }

    final public Retangulo getEnemyPenaltyArea() {
        if (player.time == null) {
            throw new NullPointerException("Player team not initialized while getting penelty area.");
        }
        return player.time.side == 'l' ? Configuracoes.AREA_PENALTI_DIREITA : Configuracoes.AREA_PENALTI_ESQUERDA;
    }

    final public Retangulo getCampoDeDefesa() {
        if (player.time == null) {
            throw new NullPointerException("Player team not initialized while getting penelty area.");
        }
        return player.time.side == 'l' ? Configuracoes.CAMPO_L : Configuracoes.CAMPO_R;
    }

    final public Retangulo getCampoDeAtaque() {
        if (player.time == null) {
            throw new NullPointerException("Player team not initialized while getting penelty area.");
        }
        return player.time.side == 'l' ? Configuracoes.CAMPO_R : Configuracoes.CAMPO_L;
    }

    final public Retangulo getPequenaAreaInimiga() {
        if (player.time == null) {
            throw new NullPointerException("Player team not initialized while getting penelty area.");
        }
        return player.time.side == 'l' ? Configuracoes.PEQUENA_AREA_R : Configuracoes.PEQUENA_AREA_L;
    }
    
    final public Retangulo getPequenaArea() {
        if (player.time == null) {
            throw new NullPointerException("Player team not initialized while getting penelty area.");
        }
        return player.time.side == 'l' ? Configuracoes.PEQUENA_AREA_L : Configuracoes.PEQUENA_AREA_R;
    }
    
    public void alinharPescocoECorpo(){
        turnNeck(-curSenseInfo.headAngle);
    }
    
    public void virarCorpoParaBola(Objetos bola){
        turnBodyParaPonto(Util.predictlPosBolaDepoisNCiclos(bola, 1), 1);
    }
    
    public void chutarPara(Ponto posAlvo, double velFinal){
        Objetos bola = getOrCreate(Bola.ID);
        
        Ponto posBola           = new Ponto(bola.posicao.getPosicao());
        VetorVelocidade velBola  = new VetorVelocidade(bola.velocity());
        Ponto posTrajetoria     = new Ponto(posAlvo);
        Ponto posAgente         = new Ponto(player.posicao.getPosicao());
        
        posTrajetoria.menos(posBola);
        VetorVelocidade velDes   = new VetorVelocidade();
        velDes.setPolar(posTrajetoria.umVetor().direction(),
                Util.getForcaParaAtravessar(posTrajetoria.umVetor().magnitude(), velFinal));
        
        double power;
        double angulo;
        
        Posicao posEst     = Util.predictAgentePosDepoisNCiclos(player, 0, 1, this.time, curSenseInfo);
        Ponto pointEst              = posEst.getPosicao();
        Vetor2D velDesMaisPosBola  = posBola.umVetor();
        velDesMaisPosBola.mais(velDes);
                
        if( pointEst.distanciaAoPonto(velDesMaisPosBola.asPoint()) < 
                  Configuracoes.BOLA_PARAMS.BALL_SIZE + Configuracoes.JOGADOR_PARAMS.PLAYER_SIZE){
            
            Reta reta           = Reta.criarRetaEntre2Pontos(posBola, velDesMaisPosBola.asPoint());
            Ponto posAgenteProj = reta.pontoNaRetaMaisProxDoPonto( posAgente );
            double dist         = posBola.distanciaAoPonto(posAgenteProj);
            
            if( velDes.magnitude() < dist )
                dist -=  Configuracoes.BOLA_PARAMS.BALL_SIZE + Configuracoes.JOGADOR_PARAMS.PLAYER_SIZE;
            else
                dist +=  Configuracoes.BOLA_PARAMS.BALL_SIZE + Configuracoes.JOGADOR_PARAMS.PLAYER_SIZE;
            
            velDes.setPolar(velDes.direction(), dist);
        }
        
        Objetos oponente    = Util.maisProximoDoObjeto(lastSeenOpponents, bola);
        double distOponente     = oponente != null ? 
                oponente.posicao.getPosicao().distanciaAoPonto(bola.posicao.getPosicao()) : 100;
        
        if( velDes.magnitude() > Configuracoes.BOLA_PARAMS.BALL_SPEED_MAX){ // NÃO VAI CHEGAR NO PONTO
            power               = Configuracoes.JOGADOR_PARAMS.POWER_MAX;
            double dSpeed       = getKickPowerRateAtual( bola, curSenseInfo.headAngle ) * power;
            double tmp          = velBola.rotate(-velDes.direction()).getY();
            angulo              = velDes.direction() - Util.arcSenGraus(tmp / dSpeed);
            Vetor2D aux        = new Vetor2D();
            aux.setCoordPolar(angulo, dSpeed);
            aux.mais(bola.velocity());
            
            double dSpeedPred   = aux.magnitude();
            
            if( dSpeedPred > Configuracoes.JOGADOR_PARAMS.PLAYER_WHEN_TO_KICK * Configuracoes.BOLA_PARAMS.BALL_ACCEL_MAX){
                acelerarBolaAVelocidade( velDes );    // shoot nevertheless
            }
            else if( getKickPowerRateAtual( bola, curSenseInfo.headAngle ) > Configuracoes.JOGADOR_PARAMS.PLAYER_WHEN_TO_KICK * Configuracoes.JOGADOR_PARAMS.KICK_POWER_RATE ){
                dominarBola();                          // freeze ball
            }
            else { 
                chutarBolaProximaAoCorpo( 0, 0.16 );            // else posicao ball better
            }
        }else{
            Vetor2D velBolaAcele = new Vetor2D(velDes);
            velBolaAcele.menos(velBola);
            
            power = velBolaAcele.magnitude() / getKickPowerRateAtual( bola, curSenseInfo.headAngle ); // with current ball speed
            if( power <= 1.05 * Configuracoes.JOGADOR_PARAMS.POWER_MAX || (distOponente < 2.0 && power <= 1.30 * Configuracoes.JOGADOR_PARAMS.POWER_MAX ) ){                               
                acelerarBolaAVelocidade( velDes );  // perform shooting action
                
            }else{
                chutarBolaProximaAoCorpo( 0 , 0.16 );
            }
        }        
    }
    
    public void acelerarBolaAVelocidade( Vetor2D velDes ){
        Objetos bola = getOrCreate(Bola.ID);
        
        double angBody          = bodyDirection();
        VetorVelocidade velBall  = new VetorVelocidade(bola.velocity());
        Vetor2D accDes         = new Vetor2D(velDes);
        accDes.menos(velBall);
        double      dPower;
        double      angActual;
        
        // if acceleration can be reached, create shooting vector
        if( accDes.magnitude() < Configuracoes.BOLA_PARAMS.BALL_ACCEL_MAX ){
            dPower    = ( accDes.magnitude() / getKickPowerRateAtual( bola, curSenseInfo.headAngle ) );
            angActual = Util.simplifyAngle( Math.toDegrees(accDes.direction()) - angBody );
            if( dPower <= Configuracoes.JOGADOR_PARAMS.POWER_MAX  ){
                kick( dPower, angActual );
                return;
            }
        }
        
        // else determine vector that is in direction 'velDes' (magnitude is lower)
         dPower           = Configuracoes.JOGADOR_PARAMS.POWER_MAX ;
         double dSpeed    = getKickPowerRateAtual( bola, curSenseInfo.headAngle ) * dPower;
         double tmp       = velBall.rotate(-velDes.direction()).getY();
         angActual        = Math.toDegrees(velDes.direction() - Util.arcSenGraus( tmp / dSpeed ));
         angActual        = Util.simplifyAngle( angActual - angBody );
         kick( dPower, angActual );   
        
    }
    
    /**
     * Habilidade que permite o agente chutar a bola proxima a seu corpo
     * @param angulo relativo angulo em graus
     * @param taxaDeChute padrão 0.16
     */    
    public void chutarBolaProximaAoCorpo(double angulo, double taxaDeChute){ // taxa de chute
        Objetos bola    = getOrCreate(Bola.ID);
        InfoCorpo sense     = new InfoCorpo();
        curSenseInfo.copy(sense);
        
        double angAgente    = bodyDirection(); // graus
        Posicao p  = Util.predictAgentePosDepoisNCiclos(player, 0, 1, time, sense);
        Ponto point         = p.getPosicao();
        double dist         = Configuracoes.BOLA_PARAMS.BALL_SIZE + Configuracoes.JOGADOR_PARAMS.PLAYER_SIZE 
                + Configuracoes.JOGADOR_PARAMS.KICKABLE_MARGIN * taxaDeChute;
        double angGlobal    = Util.simplifyAngle(angAgente + angulo); // graus
        Vetor2D posBall    = new Vetor2D();
        posBall.setCoordPolar(Math.toRadians(angGlobal), dist);       
        posBall.mais(point.umVetor());
               
        if(Math.abs(posBall.getY()) > Configuracoes.ALTURA_CAMPO/2 || Math.abs(posBall.getX()) > Configuracoes.LARGURA_CAMPO/2){
            Reta lineBody = Reta.criarRetaAPartirDaPosicaoEAngulo(point, Math.toRadians(angGlobal) );
            Reta lineSide;
            if( Math.abs( posBall.getY() ) > Configuracoes.ALTURA_CAMPO/2 )
              lineSide = Reta.criarRetaAPartirDaPosicaoEAngulo(new Ponto( 0, Math.signum(posBall.getY() )* Configuracoes.ALTURA_CAMPO/2.0 ), 0 );
            else
              lineSide = Reta.criarRetaAPartirDaPosicaoEAngulo(new Ponto( 0, Math.signum(posBall.getX() )* Configuracoes.LARGURA_CAMPO/2.0 ),  Math.toRadians(90) );
            
            Ponto posIntersect = lineSide.getIntersecao( lineBody );
            posBall = point.umVetor();
            Vetor2D n = new Vetor2D();
            n.setCoordPolar(Math.toRadians(angGlobal), posIntersect.distanciaAoPonto( point ) - 0.2);
            posBall.mais(n);
        }
        
         Vetor2D vecDesired = posBall;
         vecDesired.menos(point.umVetor());         
         
         Vetor2D vecShoot   = vecDesired;
         vecShoot.menos(bola.velocity());
         
         double dPower       = vecShoot.magnitude() / getKickPowerRateAtual( bola, curSenseInfo.headAngle ) ;        
         double angActual    = Math.toDegrees(vecDesired.direction()) - angAgente;
         angActual           = Util.simplifyAngle( angActual );
         
         if( dPower > Configuracoes.JOGADOR_PARAMS.POWER_MAX  && bola.velocity().magnitude() > 0.1 ){
             dominarBola();
             return;
         }else if( dPower > Configuracoes.JOGADOR_PARAMS.POWER_MAX ){
            if( Util.isBolaParadaParaNos(playMode, player.time.side, player.outroTime.side) ) {
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
        Objetos bola    = this.getOrCreate(Bola.ID);
        int sinal           = 1;
        Ponto posBola       = new Ponto(bola.posicao.getPosicao());
        Ponto posAgente     = new Ponto(player.posicao.getPosicao());
        posBola.menos(posAgente);
        double angulo       = Math.toDegrees(posBola.umVetor().direction());
        double anguloAgente = player.direction.getDirection();
        
        if(time == timeUltimoProcuraBola)
            return;
        
        if(time - timeUltimoProcuraBola > 3)
             sinal = ( Util.isAnguloNoIntervalo(angulo, anguloAgente,Util.simplifyAngle(anguloAgente+180) ) ) ? 1 : -1  ;
        
        timeUltimoProcuraBola   = time;
        Vetor2D angTurn        = new Vetor2D();
        angTurn.setCoordPolar(Math.toRadians(Util.simplifyAngle(anguloAgente + 60 * sinal)), 1);
        posAgente.mais(angTurn.asPoint());
        turnBodyParaPonto(posAgente, sinal);
    }
    
    public void dominarBola(){
        Objetos bola    = this.getOrCreate(Bola.ID);        
        Posicao pe = Util.predictAgentePosDepoisNCiclos(player, 0 , 1 , time, curSenseInfo);
        double power        = bola.velocity().magnitude() / getKickPowerRateAtual( bola , curSenseInfo.headAngle );
        
        if(power > Configuracoes.JOGADOR_PARAMS.POWER_MAX){
            power = Configuracoes.JOGADOR_PARAMS.POWER_MAX;
        }
        
        double angulo   = Math.toDegrees(bola.velocity().direction()) + 180 - bodyDirection(); // graus
        angulo          = Util.simplifyAngle(angulo);
        
        kick(power, angulo);
        //Vector2D posBola        = bola.posicao.getPosicao().umVetor();
        //VetorVelocidade velBola  = bola.velocity();
        
        //Futil.predictInfoBolaDepoisComando("kick", posBola, velBola, power, angulo, player.direction.getDirection() , getKickPowerRateAtual( bola, curSenseInfo.headAngle ));
        
        
        /*if(posBola.asPoint().distanciaAoPonto(pe.getPosicao()) < 0.8 * Util.kickable_radius()){
            kick(power, angulo);
            return;
        }
        posBola = bola.posicao.getPosicao().umVetor();
        
        Vetor2D auxDir = posBola;
        auxDir.menos(pe.getPosicao().umVetor());
        
        Vetor2D posTo  = pe.getPosicao().umVetor();
        Vetor2D posAux = new Vetor2D();
        posAux.setCoordPolar(auxDir.direction(), Math.min( 0.7 * Util.kickable_radius(), posBola.asPoint().distanciaAoPonto(pe.getPosicao() ) - 0.1 ));
        
        Vetor2D velDes = posTo;
        velDes.menos(posBola);
        
        acelerarBolaAVelocidade(velDes);*/
        
    }
    
    public void intercept( boolean isGoleiro ){
        
    }
    
    public Ponto getPosicaoEstrategica(int numero, Formacao.LayoutFormacao[] formacao){
        Objetos bola            = this.getOrCreate(Bola.ID);
        Ponto pos;
        Ponto posBola = bola.posicao.getPosicao();
        List<Jogador> todosNaVista   = new LinkedList<>();
        todosNaVista.addAll(companheirosVisiveis);
        todosNaVista.addAll(lastSeenOpponents);
        boolean nossaBola           = Util.isNossaPosseDeBola(todosNaVista, bola, player.time.nome);
        double maxX                 = Util.getXImpedimento(lastSeenOpponents, bola);
        maxX                        = Math.max(-0.5 , maxX - 1.5);
        
        if(Util.isGoalKick(playMode, player.outroTime.side))
            maxX = Math.min(Configuracoes.PENALTY_X - 1, maxX);
        else if(Util.isBeforeKickOff(playMode))
            maxX = Math.min(-2, maxX);
        else if(Util.isOffside(playMode, player.time.side))
            maxX = bola.posicao.getX() - 0.5;
        
        
        if(Util.isBeforeKickOff(playMode) || bola.posicao.getConfianca(time) < Configuracoes.CONFIANCA_BOLA)
            posBola = new Ponto(0,0);
        
        else if(Util.isGoalKick(playMode, player.time.side) || (Util.isFreeKick(playMode, player.time.side) &&
                posBola.getX() < - Configuracoes .PENALTY_X))            
            posBola.setX(- Configuracoes.LARGURA_CAMPO / 4 + 5);
        
        else if(Util.isGoalKick(playMode, player.outroTime.side) || (Util.isFreeKick(playMode, player.outroTime.side) &&
                posBola.getX() > Configuracoes .PENALTY_X))            
            posBola.setX(Configuracoes.PENALTY_X - 10);
        
        else if(Util.isFreeKick(playMode, player.outroTime.side))
            posBola.setX( posBola.getX() - 5);
        
        else if(nossaBola && !(Util.isBolaParadaParaEles(playMode, player.time.side, player.outroTime.side)
                || Util.isBolaParadaParaNos(playMode, player.time.side, player.outroTime.side)))
            posBola.setX( posBola.getX() + 5.0 );
        
       // else if( posBola.getX() < - Configuracoes.PENALTY_X + 5.0 )
         //    posBola = Util.predictlPosBolaDepoisNCiclos( bola, 3);
        
                
        return this.formacao.getPontoEstrategico(numero, posBola, maxX, nossaBola, Configuracoes.MAX_Y_PORCENTAGEM , formacao, player.time.side);
    }
    
    public double turnBodyParaDirecao( double direcao ){
        double angulo              = direcao;
        angulo                    -= bodyDirection();
        angulo                     = Util.simplifyAngle(angulo);
        angulo                     = Util.getAnguloParaTurn(angulo, player.velocity().magnitude());
        turn(angulo);
        return angulo;
    }
    
    public double calcularAnguloRelativoAoBody( Ponto ponto, int ciclos ){
        Posicao posGlobal = Util.predictAgentePosDepoisNCiclos(player, 0, ciclos, time, curSenseInfo);
        Vetor2D vet               = ponto.umVetor();
        vet.menos(posGlobal.getPosicao().umVetor());
        double angulo              = Math.toDegrees(vet.direction());
        angulo                    -= bodyDirection();
        angulo                     = Util.simplifyAngle(angulo);
        return angulo;
    }
    
    public double calcularAnguloRelativoACabeca( Ponto ponto, int ciclos ){
        Posicao posGlobal = Util.predictAgentePosDepoisNCiclos(player, 0, ciclos, time, curSenseInfo);
        Vetor2D vet               = ponto.umVetor();
        vet.menos(posGlobal.getPosicao().umVetor());
        double angulo              = Math.toDegrees(vet.direction());
        angulo                    -= player.direction.getDirection();
        angulo                     = Util.simplifyAngle(angulo);
        return angulo;
    }
    
    public double calcularAnguloParaTurn( Ponto ponto, int ciclos ){
        Posicao posGlobal = Util.predictAgentePosDepoisNCiclos(player, 0, ciclos, time, curSenseInfo);
        Vetor2D vet               = ponto.umVetor();
        vet.menos(posGlobal.getPosicao().umVetor());
        double angulo              = Math.toDegrees(vet.direction());
        angulo                    -= bodyDirection();
        angulo                     = Util.simplifyAngle(angulo);
        angulo                     = Util.getAnguloParaTurn(angulo, player.velocity().magnitude());
        return angulo;
    }
    
    public double turnBodyParaPonto( Ponto ponto, int ciclos ){
        double angulo = calcularAnguloParaTurn(ponto, ciclos);
        turn(angulo);
        return angulo;
    }
    
     public void turnBackBodyParaPonto( Ponto ponto, int ciclos ){
        Posicao posGlobal = Util.predictAgentePosDepoisNCiclos(player, 0, ciclos, time, curSenseInfo);
        Vetor2D vet               = ponto.umVetor();
        vet.menos(posGlobal.getPosicao().umVetor());
        double angulo              = Math.toDegrees(vet.direction());
        angulo                    -= player.direction.getDirection() + 180;
        angulo                     = Util.simplifyAngle(angulo);
        angulo                     = Util.getAnguloParaTurn(angulo, player.velocity().magnitude());
        turn(angulo);
    }
    
    public void dashParaPonto(Ponto ponto, int ciclos){
        double power = 100;//Futil.getPowerParaDash(ponto, player.direction.getDirection() , player.velocity() , curSenseInfo.effort , ciclos);
        dash(power);
    }
    
    public void moveToPos(Ponto posTo, double angQuandoVirar, double distTras, boolean moverParaTras, int ciclos){
        Ponto posAgente         = player.posicao.getPosicao();
        Ponto posFinalAgente    = Util.predictPosFinalAgente(posAgente, player.velocity());
        
        double anguloBody       = player.direction.getDirection(); // graus
        Ponto posAux            = posTo;
        posAux.menos(posFinalAgente);
        double anguloPos        = Math.toDegrees(posAux.umVetor().direction()); // graus
               anguloPos        = Util.simplifyAngle(anguloPos - anguloBody); // graus
               
        double anguloAtras      = Util.simplifyAngle(anguloPos + 180);
        double dist             = posAgente.distanciaAoPonto(posTo);      
        
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
            dashTo(posTo, 1 );
        }
        
    }
    
    public void directTowards(Ponto pontoTo, double angQuandoVirar){
        Vetor2D velAgente  = player.velocity();
        Ponto posAgente     = player.posicao.getPosicao();
        double angAgente    = player.direction.getDirection();  // graus
        Ponto posAux        = pontoTo;
        Ponto posPredAgente = Util.predictPosFinalAgente(posAgente, player.velocity());
        posAux.menos(posPredAgente);
        
        double anguloFinal  = Math.toDegrees(posAux.umVetor().direction()); // graus
        double angulo       = Util.simplifyAngle(anguloFinal - angAgente); // graus
        double angPescoco   = 0;
        
        int turns = 0;
        double[] result;
        while(Math.abs(angulo) > angQuandoVirar && turns < 5){
            turns++;
            result = Util.predictEstadoAfterTurn(Util.getAnguloParaTurn(angulo, velAgente.magnitude()),
                    posAgente, velAgente, angAgente, angPescoco, curSenseInfo);
            
            angAgente   = result[0];
            angPescoco  = result[1];
            angulo      = Util.simplifyAngle( anguloFinal - angAgente );
        }   
       
        posAgente   = player.posicao.getPosicao();       
        
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
    
    public boolean possoCapturarBola(Objetos bola){
        return getMyPenaltyArea().contains(bola) && bola.curInfo.distance < 1;
    }
    
    public void segurarBola(){
        Objetos bola        = this.getOrCreate(Bola.ID);
        
        Ponto   posAgente       = new Ponto(player.posicao.getPosicao());
        Jogador  advProximo      = adversarioMaisProximoDoObjeto(player);
        Ponto   posAdversario;
        double  anguloAdversario;
        double  angulo          = direcaoPraFrente(player.time.side) - bodyDirection();
        angulo                  = Util.simplifyAngle(angulo);
        
        if(advProximo != null){
            posAdversario       = advProximo.posicao.getPosicao();
            anguloAdversario    = advProximo.curInfo.bodyFacingDir;
            if(Util.distanciaEntre2Objetos(advProximo, bola) < 5){
                Ponto pAux      = new Ponto(posAgente);
                pAux.menos(posAdversario);
                angulo          = pAux.umVetor().direction();
                double sinal    = -Math.signum(anguloAdversario - angulo);
                angulo         += sinal * 45 - bodyDirection();
                angulo          = Util.simplifyAngle(angulo);
            }
        }
        
        Vetor2D vetAux         = new Vetor2D();
        vetAux.setCoordPolar(angulo, 0.7);
        posAgente.mais(vetAux.asPoint());
        
        if(bola.posicao.getPosicao().distanciaAoPonto(posAgente) < 0.3){
            Ponto bolaPred      = Util.predictlPosBolaDepoisNCiclos(bola, 1);
            double angGol       = calcularAnguloParaTurn(player.getPontoGolOponente(), 1);
            Ponto posAtual      = new Ponto(player.posicao.getPosicao());
            Vetor2D vel        = new Vetor2D(player.velocity());
            InfoCorpo sense     = new InfoCorpo();
            curSenseInfo.copy(sense);
            
            Util.predictEstadoAfterTurn(angGol,posAtual, vel, bodyDirection(), player.direction.getDirection(), sense);
            if(posAtual.distanciaAoPonto(bolaPred) < 0.85 * Util.kickable_radius()){
                turnBodyParaPonto(player.getPontoGolOponente(), 1);
            }
        }
        
        chutarBolaProximaAoCorpo(angulo, 0.1);
    }
    
    public void passeDireto(Ponto posAlvo, boolean normal){
        if(normal){
            chutarPara(posAlvo, Configuracoes.VELOCIDADE_FINAL_PASSE);
        }else{
            chutarPara(posAlvo, Configuracoes.VELOCIDADE_FINAL_PASSE_RAPIDO);
        }
    }
        
    public boolean colidirComABola(){
        Objetos bola = this.getOrCreate(Bola.ID);
        
        if(bola.curInfo.distance > bola.velocity().magnitude() + Configuracoes.JOGADOR_PARAMS.PLAYER_SPEED_MAX)
            return false;
        
        Ponto posBolaPred   = Util.predictlPosBolaDepoisNCiclos(bola, 1);
        Ponto posGlobalAux  = new Ponto(player.posicao.getPosicao());
        Vetor2D vet        = new Vetor2D();       // Usado apenas para acrescentar no ponto global;
        vet.setCoordPolar(0, 1);
        posGlobalAux.mais(vet.asPoint());
               
        Ponto posAgentePred = new Ponto(player.posicao.getPosicao());
        Vetor2D vel        = new VetorVelocidade(player.velocity());        
        InfoCorpo sense     = new InfoCorpo();
        curSenseInfo.copy(sense);
        
        Util.predictEstadoAfterTurn(calcularAnguloParaTurn(posGlobalAux, 1), posAgentePred, vel , bodyDirection(), player.direction.getDirection(), sense);
        
        if( posAgentePred.distanciaAoPonto( posBolaPred ) < Configuracoes.BOLA_PARAMS.BALL_SIZE + Configuracoes.JOGADOR_PARAMS.PLAYER_SIZE ){
            turnBodyParaPonto(posGlobalAux, 1);
            return true;
        }
        
        posAgentePred       = new Ponto(player.posicao.getPosicao());
        vel                 = new VetorVelocidade(player.velocity());
        curSenseInfo.copy(sense);
        //Arrumar método getPower for Dash
        Util.predictEstadoDepoisDoDash(posAgentePred, vel, 100, time, sense, bodyDirection());
        
        if( posAgentePred.distanciaAoPonto( posBolaPred ) < Configuracoes.BOLA_PARAMS.BALL_SIZE + Configuracoes.JOGADOR_PARAMS.PLAYER_SIZE ){
            turnBodyParaPonto(posGlobalAux, 1);
            return true;        
        }       
        
        return false;
    }
    
    public void turnComABola(){
        Objetos bola    = this.getOrCreate(Bola.ID);
        Ponto posGlobal     = new Ponto(player.posicao.getPosicao());
        Ponto posBola       = new Ponto(bola.posicao.getPosicao());
        double direcBody    = bodyDirection();
        Jogador adverPerto   = adversarioMaisProximoDoObjeto(player);
        double distancia    = Util.distanciaEntre2Objetos(adverPerto, player);
    }
    
    public Ponto getPontoDeIntersecaoBola(){
        Objetos bola    = this.getOrCreate(Bola.ID);
        Ponto posAgente     = new Ponto(player.posicao.getPosicao());
        VetorVelocidade vel  = new VetorVelocidade(player.velocity());
        double dSpeed, dDistExtra;
        Ponto posMe;
        Ponto posBall = null;
        double ang, angBody, angNeck;
        InfoCorpo sta       = new InfoCorpo();
        double dMaxDist;
        
        dMaxDist        = Util.kickable_radius();
        dSpeed          = player.velocidade.magnitude();
        dDistExtra      = Util.getSumInfGeomSeries(dSpeed, Configuracoes.JOGADOR_PARAMS.PLAYER_DECAY);
        Vetor2D posAux = new Vetor2D();
        posAux.setCoordPolar(vel.direction(), dDistExtra);
        posAgente.mais(posAux.asPoint());
        
        for (int i = 0; i < Configuracoes.JOGADOR_PARAMS.NUMERO_MAX_DE_CICLOS_PARA_INTERECEPTAR_BOLA; i++) {
            vel         = new VetorVelocidade(player.velocity());
            angBody     = bodyDirection();
            angNeck     = player.direction.getDirection();
            posBall     = Util.predictlPosBolaDepoisNCiclos(bola, i+1);
            posMe       = new Ponto(player.posicao.getPosicao());
            Ponto aux   = new Ponto(posBall);
            aux.menos(posAgente);
            ang         = Math.toDegrees(aux.umVetor().direction());
            ang         = Util.simplifyAngle(ang - angBody );
            curSenseInfo.copy(sta);
            int turn    = 0;
            
            while(Math.abs(ang) > Configuracoes.ANGULO_PARA_USAR_TURN && turn < 5){
                turn++;
                double dirBodyENeck[] = Util.predictEstadoAfterTurn(Util.getAnguloParaTurn(ang, vel.magnitude()), posMe, vel, angBody, angNeck, sta);
                aux   = new Ponto(posBall);
                aux.menos(posAgente);
                angBody     = dirBodyENeck[0];
                angNeck     = dirBodyENeck[1];
                ang         = Math.toDegrees(aux.umVetor().direction());
                ang         = Util.simplifyAngle(ang - angBody);
            }
            
            for (; turn < i; turn++) {
                Util.predictEstadoDepoisDoDash(posMe, vel, Configuracoes.JOGADOR_PARAMS.DASH_POWER_MAX, time, sta, angBody);
            }
            
            if (posMe.distanciaAoPonto( posBall ) < dMaxDist  ||(posMe.distanciaAoPonto( posAgente) > posBall.distanciaAoPonto( posAgente ) + dMaxDist) )            
               return posBall;
                       
        }
        
        return null;
    }
    
    
    
    //calcula a taxa de ruído para dar um chute com precisão
    public static double getKickPowerRateAtual( Objetos bola , double headAngulo ) {
        double dir_diff      = Math.abs( bola.curInfo.direction + headAngulo );
        double dist          = bola.curInfo.distance - Configuracoes.JOGADOR_PARAMS.PLAYER_SIZE - Configuracoes.BOLA_PARAMS.BALL_SIZE;
        return Configuracoes.JOGADOR_PARAMS.KICK_POWER_RATE * ( 1 - 0.25 * dir_diff/180.0 - 0.25 * dist / Configuracoes.JOGADOR_PARAMS.KICKABLE_MARGIN);
    }
}
