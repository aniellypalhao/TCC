/** @file Futil.java
 * F(utility) utilities.
 * 
 * @author Team F(utility)
 */



import java.util.LinkedList;
import java.util.List;

/**
 * Util: F(utility) utilities.
 */
public final class Util {
    
    /**
     * Extracts the arguments from a correctly-formatted object info string.
     * 
     * @param info correctly-formatted object info string
     * @return array of arguments as strings
     */
    public static final String[] extractArgs(String info) {
        if (!isCorrectlyFormatted(info)) {
            return new String[] {};
        }
        int beginIndex = 2 + extractId(info).length();  // account for '(' and space separator following id
        int endIndex = info.length() - 1;  // don't include final char, ')'
        return info.substring(beginIndex, endIndex).split("\\s");
    }
    
    /**
     * Extracts object info strings from a `see` message.
     * 
     * @param see `see` message
     * @return object info strings
     */
    public static final LinkedList<String> extractInfos(String see) {
        int beginIndex = see.indexOf("((");
        int endIndex = see.length() - 1 ;  // don't include final ')'
        if (beginIndex == -1) {
            return new LinkedList<String>();
        }
        return stringToList(see.substring(beginIndex, endIndex));
    }
    
    /**
     * Gets the object id from an ObjectInfo string.
     * 
     * @param info ObjectInfo string
     * @return the object id
     */
    public static final String extractId(String info) {
        if (!isCorrectlyFormatted(info)) {
            return "UNKNOWN";
        }
        int beginIndex = 1;  // the opening parenthesis of the object's id
        int endIndex = findClosingParenthesis(info, 1);
        String result = info.substring(beginIndex, endIndex + 1);
        // (B) and (b) are the same object so we force the use of (b) in order to correctly
        // associate the ball with a single field object in the HashMap.
        if (result.equals("(B)")) {
            result = "(b)";
        }
        return result; 
    }
    
    /**
     * Estimates the acceleration of a FieldObject.
     * 
     * @param obj the object to estimate the acceleration of
     * @param timeOffset time step offset from the current soccer server time step
     * @param currentTime the current soccer server time step
     * @return estimated acceleration of the player in the given time step offset by the given offset
     */
   /* public static VetorDeAceleracao estimateAccelerationOf(Objetos obj, int timeOffset, int currentTime) {
        if (timeOffset < -1) {
            return VetorDeAceleracao.ZeroVector();
        }
        else if (timeOffset == 1) {
            if (obj.hasBrain()) {
                Jogador player = (Jogador) obj;
                return player.brain.acceleration;
            }
            else {
                return VetorDeAceleracao.ZeroVector();
            }
        }
        else {
            return VetorDeAceleracao.ZeroVector();
        }
    }*/
    
    /**
     * Estimates the position of a FieldObject.
     * 
     * @param obj the object to estimate the position of
     * @param timeOffset time step offset from the current soccer server time step
     * @param currentTime the current soccer server time step
     * @return estimated position of the player in the given time step offset by the given offset
     */
   /* public static Posicao estimatePositionOf(Objetos obj, int timeOffset, int currentTime) {
        if (timeOffset < -1) {
            return Posicao.Desconhecido(currentTime);
        }
        else if (timeOffset == -1) {
            return obj.prevPosition;
        }
        else if (timeOffset == 0) {
            return obj.posicao;
        }
        else {
            Posicao est = new Posicao(obj.posicao);
            for (int i=0; i<timeOffset; i++) {
                VetorVelocidade v = Util.estimateVelocityOf(obj, i, currentTime);
                VetorDeAceleracao a = Util.estimateAccelerationOf(obj, i, currentTime);
                double x = est.getX() + v.getX() + a.getX();
                double y = est.getY() + v.getY() + a.getY();
                double confidence = est.getConfianca(currentTime + i) * 0.95;
                est.atualizar(x, y, confidence, currentTime + i);
            }
            return est;
        }
    }*/
    /**
     * Método para rotacionar um ponto --MELHORAR ESTE MÉTODO
     * @param pontoPrimitivo o ponto a ser rotacionado
     * @param angulo o ângulo (em graus) para realizar a rotação
     * @return um novo ponto rotacionado (angulo)º em relação ao ponto primitivo;
     */
    public static Ponto rotacionarPonto(Ponto pontoPrimitivo, double angulo){     
        return new Ponto(pontoPrimitivo.getX() * Math.cos(Math.toRadians(angulo)) - pontoPrimitivo.getY() * Math.sin(Math.toRadians(angulo)),
            pontoPrimitivo.getX() * Math.sin(Math.toRadians(angulo)) + pontoPrimitivo.getY() * Math.cos(Math.toRadians(angulo)));
    }
       
    /**
     * Estimates the velocity of a Objetos at some ciclo offset.
     * 
     * @param obj the Objetos to estimate the velocity for
     * @param timeOffset offset from the current ciclo step for the ciclo step to estimate for
     * @param currentTime the current soccer server ciclo step
     * @return the estimated velocity of the object
     */
    public static VetorVelocidade estimateVelocityOf(Objetos obj, int timeOffset, int currentTime) {
        if (timeOffset < 0) {
            return new VetorVelocidade();
        }
        else if (timeOffset == 0) {
            return obj.velocity();
        }
        else {
            // Assume for now the object retains its current velocity vector into the future
            return obj.velocity();
        }
    }
    
    public static double distanciaEntre2Objetos(Objetos objeto1, Objetos objeto2) {
        double anguloRadiano;
        
        if(objeto2 == null || objeto1 == null)
            return 1000d;
        
        anguloRadiano = Math.toRadians(Math.abs(objeto1.curInfo.direction - objeto2.curInfo.direction));
        
        return Math.sqrt(Math.pow(objeto1.curInfo.distance, 2) + Math.pow(objeto2.curInfo.distance, 2)
                - 2 * objeto1.curInfo.distance * objeto2.curInfo.distance * Math.cos(anguloRadiano));
    }
    
    /**
     * Given a correctly-formatted `see` or `sense_body` message, returns the soccer server ciclo
 contained within the message.
     * 
     * @param s correctly-formatted `see` or `sense_body` message
     * @return soccer server ciclo contained within the message
     */
    public static final int extractTime(String s) {
        if (!isCorrectlyFormatted(s)) {
            return -1;
        }
        if (!s.startsWith("(see ") && !s.startsWith("(sense_body ")) {
            System.out.println("`extractTime` expected  a message starting with '(see' or '(sense_body', got: " + s);
            return -1;
        }
        String timeArg = s.split("\\s")[1];
        if (timeArg.endsWith(")")) {
            timeArg = timeArg.substring(0, timeArg.length() - 1);  // remove ')'
        }
        return Integer.parseInt(timeArg);
    }
    
    /**
     * Returns the index of the matching closing parenthesis, given a string and the index of the opening parenthesis to match.
     * 
     * @param s correctly-formatted string
     * @param beginIndex index of the opening parenthesis to match
     * @return index of the matching closing parenthesis
     */
    public static final int findClosingParenthesis(String s, int beginIndex) {
        if (!isCorrectlyFormatted(s)) {
            return -1;
        }
        if (s.charAt(beginIndex) != '(') {
            System.out.println(s + " at index " + beginIndex + " is not '('.");
            return -1;
        }
        int numOpenParens = 0;
        int numOpenParensAtBeginIndex = -1;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '(') {
                if (i == beginIndex) {
                    numOpenParensAtBeginIndex = numOpenParens;
                }
                numOpenParens++;
            }
            else if (s.charAt(i) == ')') {
                numOpenParens--;
                if (numOpenParens == numOpenParensAtBeginIndex) {
                    return i;
                }
            }
        }
        System.out.println("Could not find matching closing ')' for the '(' at index " + beginIndex + " in '" + s + "'.");
        return -1;
    }
    
    /**
     * Returns true if an object is specific enough to identify a unique field object.
     * 
     * @param id the object id
     * @return true if the object is specific enough to identify a unique field object
     */
    public static final boolean isUniqueFieldObject(String id) {
        if (id.startsWith("(F") || id.startsWith("(G") || id.startsWith("(P") || id.startsWith("(l")) {
            return false;
        }
        return true;
    }
    
    /**
     * Tests that a string is correctly formatted--that is, it starts with an opening parenthesis,
     * ends with a closing parenthesis, has the same number of opening and closing parentheses, 
     * and that the number of currently-open parentheses is never less than 0.
     * 
     * @return true if the string is correctly-formatted
     */
    public static final boolean isCorrectlyFormatted(String s) {
        int numOpenParens = 0;
        if (s.charAt(0) != '(') {
            System.out.println("String starts with '" + s.charAt(0) + "', not '('. String: " + s);
            return false;
        }
        if (s.charAt(s.length() - 1) != ')') {
            System.out.println("String ends with '" + s.charAt(s.length() - 1) + "', not ')'. String: " + s);
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '(') {
                numOpenParens++;
            }
            else if (s.charAt(i) == ')') {
                numOpenParens--;
                if (numOpenParens < 0) {
                    System.out.println("String has less than 0 open parentheses at some index: " + s);
                    return false;
                }
            }
        }
        if (numOpenParens != 0) {
            System.out.println("String has unequal number of parentheses: " + s);
        }
        return true;
    }
    
    /**
     * Cleans up a received message from the soccer server.
     * 
     * @param s a raw message from the soccer server
     * @return a clean message (leading / trailing space removed, confirmed to start with '(' and end with ')')
     */
    public static final String sanitize(String s) {
        s = s.trim();
        int beginIndex = 0;
        int endIndex = s.length();
        while (s.charAt(endIndex - 1) != ')') {
            endIndex--;           
        }
        while (s.charAt(beginIndex) != '(') {
            beginIndex++;
        }
        String result = s.substring(beginIndex, endIndex);
        if (!Util.isCorrectlyFormatted(result)) {
            return "";
        }
        return result;
    }
    
    /**
     *  Método que calcula o angulo entre 2 catetos usando a fórmula tg x = catetoOposto /catetoAdjacente
     *  @param catetoAdjacente o comprimento do cateto adjacente
     *  @param catetoOposto o comprimento do catetoOposto
     *  @return o valor do ângulo em graus
     */
    public static final double anguloEntre2Catetos(double catetoAdjacente, double catetoOposto){
        return Math.toDegrees(Math.atan((catetoOposto/catetoAdjacente)));
    }
    
    public static final boolean isFlag(String id){
        return id.startsWith("(f");
    }
    
    /**
     * Turns a string of objects into a list of objects.
     * 
     * @param s string of objects
     */
    public static final LinkedList<String> stringToList(String s) {
        int numOpenParens = 0;
        int beginIndex = -1;  // beginning index of an object substring
        int endIndex = -1; // end index of an object substring
        LinkedList<String> objects = new LinkedList<String>();
        if (!isCorrectlyFormatted(s)) {
            return objects;
        }
        for (int i=0; i<s.length(); i++) {
            if (s.charAt(i) == '(') {
                numOpenParens++;
                if (numOpenParens == 1) {
                    beginIndex = i;
                }
            }
            else if (s.charAt(i) == ')') {
                numOpenParens--;
                if (numOpenParens == 0) {
                    endIndex = i + 1;
                    objects.add(s.substring(beginIndex, endIndex));
                }
            }
        }
        return objects;
    }
    
    public static double validarTurNeckAngle(double angulo, double neckAngulo){
        double head = neckAngulo;
        if(Math.abs(head + angulo) > 90){
            if(angulo < 0)
                return -90-head;
            else
                return 90-head;
        }
        return angulo;
    }
    
    /**
     * Simplifies an angle to within [-180, 180] degrees.
     * 
     * @param angle angle to simplify
     * @return an equivalent angle within [-180, 180] degrees
     */
    public static final double simplifyAngle(double angle) {
        while (angle > 180.0) {
            angle -= 360.0;
        }
        while (angle < -180.0) {
            angle += 360.0; 
        }
        return angle;
    }
    
    /**
     * Given a target angle, returns the closest similar angle within the minimum and maximum 
     * moment. Assumes maximum moment <= 180.0 degrees and minimum moment >= -180.0 degrees.
     * 
     * @param angle target angle
     * @return valid moment
     */
    public static final double toValidMoment(double angle) {
        angle = simplifyAngle(angle);
        if (angle > Configuracoes.JOGADOR_PARAMS.MOMENT_MAX) {
            angle = Configuracoes.JOGADOR_PARAMS.MOMENT_MAX;
        }
        else if (angle < Configuracoes.JOGADOR_PARAMS.MOMENT_MIN) {
            angle = Configuracoes.JOGADOR_PARAMS.MOMENT_MIN;
        }
        return angle;
    }
    
    /**
     * Given a target angle, returns the closest similar angle within the minimum and maximum 
     * moment. Assumes maximum moment 90.0 <= degrees and minimum moment >= -90.0 degrees.
     * 
     * @param angle target angle
     * @return valid moment
     */
    public static final double toValidMomentNeck(double angle) {
        angle = simplifyAngle(angle);
        if (angle > Configuracoes.JOGADOR_PARAMS.NECK_ANGLE_MAX) {
            angle = Configuracoes.JOGADOR_PARAMS.NECK_ANGLE_MAX;
        }
        else if (angle < Configuracoes.JOGADOR_PARAMS.NECK_ANGLE_MIN) {
            angle = Configuracoes.JOGADOR_PARAMS.NECK_ANGLE_MIN;
        }
        return angle;
    }

	/**
	 * Utility function, computes the radius of the player agent's kickable
	 * area. <br><br>
	 * Uses the formula:<br>
	 * \f$kickable_area = player_size + ball_size + kickable_margin\f$
	 * <br><br>As defined by the server parameters.
	 * 
	 * @return the kickable area radius.
	 */
	public static double kickable_radius() {
		return  (
			Configuracoes.JOGADOR_PARAMS.PLAYER_SIZE +
			Configuracoes.JOGADOR_PARAMS.KICKABLE_MARGIN +
                        Configuracoes.BOLA_PARAMS.BALL_SIZE
                );
	}
        
        public static double getPowerParaDash(Ponto ponto, double angulo, Vetor2D velocidade, double effort, int ciclos){
            double dist = ponto.umVetor().rotate(-angulo).getX();
            if (ciclos <= 0) ciclos = 1;
            double dAcc = getFirstSpeedFromDist(dist, ciclos, Configuracoes.JOGADOR_PARAMS.PLAYER_DECAY);
            
            if(dAcc > Configuracoes.PLAYER_SPEED_MAX)
                dAcc = Configuracoes.PLAYER_SPEED_MAX;
            
            dAcc -= velocidade.rotate(-angulo).getX();
            
            double dashPower = dAcc / (Configuracoes.DASH_POWER_RATE * effort);
            
            if(dashPower > Configuracoes.JOGADOR_PARAMS.DASH_POWER_MAX)
                dashPower = Configuracoes.JOGADOR_PARAMS.DASH_POWER_MAX;
            if(dashPower < Configuracoes.JOGADOR_PARAMS.DASH_POWER_MIN)
                dashPower = Configuracoes.JOGADOR_PARAMS.DASH_POWER_MIN;
            
            return dashPower;
        }
        
        public static double getFirstSpeedFromDist(double dist, int ciclos, double decay){
            if(decay < 0)
                decay = Configuracoes.BOLA_PARAMS.BALL_DECAY;
            
            return getFirstGeomSeries(dist, decay, ciclos);
        }
        
        public static Ponto predictlPosBolaDepoisNCiclos(Objetos o, int nCiclos){
            Ponto result;
            Ponto p      = new Ponto(o.posicao.getPosicao());            

            double dDist = getSumGeomSeries(o.velocity().magnitude(), Configuracoes.BOLA_PARAMS.BALL_DECAY, nCiclos);
            Vetor2D vet = new Vetor2D();
            
            vet.setCoordPolar(o.velocity().direction(), dDist);
            vet.mais(p.umVetor());
            result = vet.asPoint();    
            
            return result;
        }
        
        public static Posicao predictAgentePosDepoisNCiclos(Objetos o, double dashPower, int nCiclos, int time, InfoCorpo info){
            Posicao resultPos  = new Posicao();
            InfoCorpo stamina           = info;
            double dir                  = o.direction.getDirection();
            Ponto p                     = new Ponto(o.posicao.getPosicao());
            VetorVelocidade vel          = new VetorVelocidade(o.velocity());
            
            for (int i = 0; i < nCiclos; i++) {
                predictEstadoDepoisDoDash(p,vel, dashPower, time, stamina, dir);
            }
            
            resultPos.atualizar(p, Math.pow(0.95, nCiclos), time + nCiclos);
            
            return resultPos;
        }
        
        public static double getAnguloParaTurn(double anguloDesejado, double velocidade){
            double angulo = anguloDesejado * (1.0 + Configuracoes.VARIACAO_DE_INERCIA * velocidade);
            
            if(angulo > Configuracoes.JOGADOR_PARAMS.TURN_MAX_MOMENT)
                angulo = Configuracoes.JOGADOR_PARAMS.TURN_MAX_MOMENT;
            else if(angulo < Configuracoes.JOGADOR_PARAMS.TURN_MIN_MOMENT)
                angulo = Configuracoes.JOGADOR_PARAMS.TURN_MIN_MOMENT;
            
            return angulo;
        }
        
        public static double getAtualTurnAngulo( double angTurn,double dSpeed) {
            return angTurn / (1.0 + Configuracoes.VARIACAO_DE_INERCIA * dSpeed );
        }
        
        public static boolean isValorConfiancaAceitavel(double confianca){
            return confianca > 0.85;
        }
        
        public static Objetos maisProximoDoObjeto(List<Jogador> objetos, Objetos objeto){
            Jogador maisProximo      = null;
            double menorDistancia   = 1000d;
            Ponto p;
                        
            for (Jogador player : objetos) {
                if(!player.equals(objeto)){
                    p = player.posicao.getPosicao();
                    p.menos(objeto.posicao.getPosicao());
                    if(p.umVetor().magnitude() < menorDistancia){
                        maisProximo = player;
                        menorDistancia = p.umVetor().magnitude();
                    }
                }
            }
            
            return maisProximo;
        }
        
        public static boolean souOMaisProximoDoObjeto(List<Jogador> objetos, Objetos objeto, double distancia){
            double menorDistancia   = distancia;
            Ponto p;
                        
            for (Jogador player : objetos) {
                if(!player.equals(objeto)){
                    p = new Ponto(player.posicao.getPosicao());
                    p.menos(objeto.posicao.getPosicao());
                    if(p.umVetor().magnitude() < menorDistancia)                        
                        menorDistancia = p.umVetor().magnitude();
                    
                }
            }
            
            return menorDistancia == distancia;
        }
        
        public static Objetos maisRapidoAteObjeto( List<Jogador> todosPlayerNaVista, Objetos bola ){
            return null;
        }
        
        public static boolean isNossaPosseDeBola( List<Jogador> todosPlayerNaVista, Objetos bola, String time ){
            Objetos maisRapido = maisProximoDoObjeto(todosPlayerNaVista, bola);
            
            return (maisRapido != null) ? 
                    maisRapido.id.startsWith("(p \"") && (maisRapido.id.startsWith(time, 4))
                    :
                    false;
        }
        
        public static Ponto predictPosDeOutroJogador(Jogador player, int ciclos){
            double dDirection  = player.velocity().direction();
            Ponto pontoPlayer  = new Ponto(player.posicao.getPosicao());
            VetorVelocidade vel = new VetorVelocidade(player.velocity());
            
            for( int i = 0; i < ciclos ; i ++ ){
                double dAcc     = vel.magnitude();
                if( dAcc > 0 ){
                    Vetor2D aux = new Vetor2D();
                    aux.setCoordPolar(Math.toRadians(dDirection), dAcc);
                    vel.mais(aux);
                }else{
                    Vetor2D aux = new Vetor2D();
                    aux.setCoordPolar(Math.toRadians(simplifyAngle(dDirection + 180)), Math.abs(dAcc));
                    vel.mais(aux);
                }
                
                if(vel.magnitude() > Configuracoes.JOGADOR_PARAMS.PLAYER_SPEED_MAX)
                    vel = new VetorVelocidade(Configuracoes.JOGADOR_PARAMS.PLAYER_SPEED_MAX);
                
                pontoPlayer.mais(vel.asPoint());
               // p.setX(pos.getX());
               // p.setY(pos.getY());
            
                vel.vezesEscalar(Configuracoes.JOGADOR_PARAMS.PLAYER_DECAY);
                
            }
            return pontoPlayer;
        }
        
        
        public static void predictEstadoDepoisDoDash(Ponto p, Vetor2D vel, double dashPower, int time, InfoCorpo info, double dir){
            double effort   = info.effort;
            double dAcc     = dashPower * Configuracoes.DASH_POWER_RATE * effort;            
            Vetor2D pos    = p.umVetor();          
            
            
            if( dAcc > 0 ){
                Vetor2D aux = new Vetor2D();
                aux.setCoordPolar(Math.toRadians(dir), dAcc);
                vel.mais(aux);
            }else{
                Vetor2D aux = new Vetor2D();
                aux.setCoordPolar(Math.toRadians(simplifyAngle(dir + 180)), Math.abs(dAcc));
                vel.mais(aux);
            }
            
            if(vel.magnitude() > Configuracoes.JOGADOR_PARAMS.PLAYER_SPEED_MAX)
                vel = new VetorVelocidade(Configuracoes.JOGADOR_PARAMS.PLAYER_SPEED_MAX);
            
            predictStaminaDepoisDoDash(dashPower, info);
            
            pos.mais(vel);
            p.setX(pos.getX());
            p.setY(pos.getY());
            
            vel.vezesEscalar(Configuracoes.JOGADOR_PARAMS.PLAYER_DECAY);            
        }
        
        public static double getXImpedimento( List<Jogador> jogadoresInimigos, Objetos bola ){            
            double maiorX   = 0;
                        
            for (Jogador player : jogadoresInimigos) {                
                if(player.posicao.getPosicao().getX() > maiorX && !player.id.contains("goalie")){
                    maiorX = player.posicao.getPosicao().getX();
                }                
            }
            
            double x    = bola.posicao.getX();
            x           = Math.max(x, maiorX);
            
            return x;
        }
        
        public static boolean isGoalKick( String playMode, char lado){
            return playMode.equals("goal_kick_" + lado);
        }
        
        public static boolean isBeforeKickOff( String playMode){
            return playMode.equals("before_kick_off");
        }
        
        public static boolean isOffside( String playMode, char lado){
            return playMode.equals("offside_" + lado);
        }
        
        public static boolean isFreeKick( String playMode, char lado){
            return playMode.equals("free_kick_" + lado);
        }
        
        public static boolean isCornerKick( String playMode, char lado){
            return playMode.equals("corner_kick_" + lado);
        }
        
        public static boolean isKickIn( String playMode, char lado){
            return playMode.equals("kick_in_" + lado);
        }
        
        public static boolean isKickOff( String playMode, char lado){
            return playMode.equals("kick_off_" + lado);
        }
        
        
        public static void predictStaminaDepoisDoDash(double dashPower, InfoCorpo info){
            double stamina  = info.stamina;
            double effort   = info.effort;
            
             stamina -= ( dashPower > 0.0 ) ? dashPower : -2*dashPower ;
             
             if( stamina < 0 )
                 stamina = 0;
            
             if( stamina <= Configuracoes.JOGADOR_PARAMS.D_EFFORT_DEC_THR * Configuracoes.JOGADOR_PARAMS.STAMINA_MAX &&
                   effort > Configuracoes.JOGADOR_PARAMS.D_EFFORT_MIN )
                    effort -= Configuracoes.JOGADOR_PARAMS.D_EFFORT_DEC;
             
             if( stamina >= Configuracoes.JOGADOR_PARAMS.D_EFFORT_INC_THR * Configuracoes.JOGADOR_PARAMS.STAMINA_MAX && effort < 1.0){
                effort += Configuracoes.JOGADOR_PARAMS.D_EFFORT_INC;
                if ( effort > 1.0 )
                    effort = 1.0;
             }
             
             stamina += 0.6 * Configuracoes.JOGADOR_PARAMS.STAMINA_INC_MAX;
             if ( stamina >  Configuracoes.JOGADOR_PARAMS.STAMINA_MAX)
                stamina = Configuracoes.JOGADOR_PARAMS.STAMINA_MAX;

             info.effort = effort;
             info.stamina = stamina;            
        }
        
        public static Ponto predictPosFinalAgente( Ponto pos, Vetor2D vel ){
            Ponto posAgente = pos;
            Vetor2D velAgn = vel;
            
            double dist     = getSumInfGeomSeries(velAgn.magnitude(), Configuracoes.JOGADOR_PARAMS.PLAYER_DECAY);
            Vetor2D vetAux = new Vetor2D();
            vetAux.setCoordPolar(vel.direction(), dist);
            posAgente.mais(vetAux.asPoint());
            
            return posAgente;
        }
        
        public static double[] predictEstadoAfterTurn(double dSendAngle, Ponto pos, Vetor2D vel, double angBody, double angNeck, InfoCorpo sta){
            double dEffectiveAngle;
            dEffectiveAngle = getAtualTurnAngulo(dSendAngle, vel.magnitude() );
            
            angBody = Util.simplifyAngle(angBody + dEffectiveAngle );
            angNeck = Util.simplifyAngle(angNeck + dEffectiveAngle );
            
            predictEstadoDepoisDoDash(pos, vel, 0, sta.ciclo, sta, angBody);
            
            double[] result = new double[2];
            result[0] = angBody;
            result[1] = angNeck;
            
            return result;
        }
        
        
        
        public static double getForcaParaAtravessar(double distancia, double velFinal){
            
            if(velFinal < 0.0001)
                return getFirstInfGeomSeries( distancia , Configuracoes.BOLA_PARAMS.BALL_DECAY );
            
            double ciclos = getLengthGeomSeries(velFinal, 1.0/Configuracoes.BOLA_PARAMS.BALL_DECAY, distancia);
            return getForcaInicialParaVeloFinal(velFinal, (int) Math.rint(ciclos), -1);
        }
        
        //Calcula a força inicial necesária para que a bola chegue no determinado ponto com vel final desejada.
        public static double getForcaInicialParaVeloFinal( double velFinal, double ciclos, double decay  ) {
            if( decay < 0 )
                decay = Configuracoes.BOLA_PARAMS.BALL_DECAY;

            // geometric serie: s = a + a*r^1 + .. + a*r^n, now given endspeed = a*r^n ->
            // endspeed = firstspeed * ratio ^ length ->
            // firstpeed = endspeed * ( 1 / ratio ) ^ length
            return velFinal * Math.pow( 1 / decay, ciclos );
        }
        
        public static double getFirstGeomSeries(double dSum, double dRatio, double dLength){
            return dSum *  ( 1 - dRatio )/( 1 - Math.pow( dRatio, dLength ) ) ;
        }
        
        public static double getSumGeomSeries( double dFirst, double dRatio, double dLength){
            return dFirst * ( 1 - Math.pow( dRatio, dLength ) ) / ( 1 - dRatio ) ;
        }
        
        public static double getFirstInfGeomSeries( double dSum, double dRatio ){
            if( dRatio > 1 )
                System.out.println("SÉRIE NÃO CONVERGE");
            
            // s = a(1-r^n)/(1-r) with r->inf and 0<r<1 => r^n = 0 => a = s ( 1 - r)
            return dSum * ( 1 - dRatio );
        }
        
        public static double getLengthGeomSeries( double dFirst, double dRatio, double dSum ) {
            if( dRatio < 0 )
                System.out.println("ratio negativo");

            // s = a + ar + ar^2 + .. + ar^n-1 and thus sr = ar + ar^2 + .. + ar^n
            // subtract: sr - s = - a + ar^n) =>  s(1-r)/a + 1 = r^n = temp
            // log r^n / n = n log r / log r = n = length
            double temp = (dSum * ( dRatio - 1 ) / dFirst) + 1;
            if( temp <= 0 )
              return -1.0;
            return Math.log( temp ) / Math.log( dRatio ) ;
        }
        
        public static  double getSumInfGeomSeries( double dFirst, double dRatio ){
            if( dRatio > 1 )
                System.out.println("(Geometry:CalcLengthGeomSeries): series does not converge");
            
            // s = a(1-r^n)/(1-r) with n->inf and 0<r<1 => r^n = 0
            return dFirst / ( 1 - dRatio );
}
        
        public static double arcSenGraus( double x ){
            if( x >= 1 )
                return ( 90.0 );
            else if ( x <= -1 )
                return ( -90.0 );

            return ( Math.toDegrees(Math.asin( x ) ) );
        }
        
        public static Vetor2D somaVetores(Vetor2D vet1, Vetor2D vet2){
            Vetor2D result = new Vetor2D();
            result.setX(vet1.getX() + vet2.getX());
            result.setY(vet1.getY() + vet2.getY());
            return result;
        }
        
        public static boolean isBolaParadaParaNos( String playMode , char lado, char ladoInimigo){
            return (playMode.equals("kick_in_" + lado )
                || playMode.equals("kick_off_" + lado)
                || playMode.equals("offside_" + ladoInimigo)
                || playMode.equals("free_kick_" + lado)
                || playMode.equals("corner_kick_" + lado)
                || playMode.equals("goal_kick_" + lado)
                || playMode.equals("free_kick_fault_" + ladoInimigo)
                || playMode.equals("back_pass_" + ladoInimigo));
        }
        
        public static boolean isBolaParadaParaEles( String playMode , char lado, char ladoInimigo){
            return (playMode.equals("kick_in_" + ladoInimigo )
                || playMode.equals("kick_off_" + ladoInimigo)
                || playMode.equals("offside_" + lado)
                || playMode.equals("free_kick_" + ladoInimigo)
                || playMode.equals("corner_kick_" + ladoInimigo)
                || playMode.equals("goal_kick_" + ladoInimigo)
                || playMode.equals("back_pass_" + lado)
                || playMode.equals("free_kick_fault_" + lado));
        }
        
        public static void predictInfoBolaDepoisComando(String comando, Vetor2D pos, VetorVelocidade vel, double power, double angulo, double anguloGlobal, double kickRate){
            Vetor2D posBola        = pos;
            VetorVelocidade velBola  = vel;
            
            if(comando.equals("kick")){
                double ang      = Util.simplifyAngle(angulo + anguloGlobal); // graus
                Vetor2D nAux   = new Vetor2D();
                nAux.setCoordPolar(Math.toRadians(ang), kickRate * power);
                velBola.mais(nAux);
                
                if(velBola.magnitude() > Configuracoes.BOLA_PARAMS.BALL_SPEED_MAX)
                    velBola.setMagnitude(Configuracoes.BOLA_PARAMS.BALL_SPEED_MAX);
                
            } 
            
            posBola.mais(velBola);
            velBola.vezesEscalar(Configuracoes.BOLA_PARAMS.BALL_DECAY);
            
            pos.setX(posBola.getX());
            pos.setY(posBola.getY());
            
            vel.setX(velBola.getX());
            vel.setY(velBola.getY());
        }
        
        public static boolean isAnguloNoIntervalo(double ang, double angMin, double angMax){
            if( ( ang    + 360 ) < 360 ) ang    += 360;
            if( ( angMin + 360 ) < 360 ) angMin += 360;
            if( ( angMax + 360 ) < 360 ) angMax += 360;
            
            if( angMin < angMax )
                return angMin < ang && ang < angMax ;
            else                  
                return !( angMax < ang && ang < angMin );
        }
        
}
