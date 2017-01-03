
import java.util.logging.Level;
import java.util.logging.Logger;


/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Valderlei
 */
public class Teste {
    
    public void turnBodyParaPonto( Ponto ponto, int ciclos ){
        Ponto posGlobal            = new Ponto(0,0);
        Vetor2D vet               = ponto.umVetor();
        vet.menos(posGlobal.umVetor());
        double angulo              = Math.toDegrees(vet.direction());
        angulo                    -= 0;
        angulo                     = Util.simplifyAngle(angulo);
        //angulo                     = Util.getAnguloParaTurn(angulo, player.velocity().magnitude());
        System.out.println(angulo);
    }
    
    public void procurarBola(){
        int sinal           = 1;
        Ponto posBola       = new Ponto(1,-1);
        Ponto posAgente     = new Ponto(0,0);
        posBola.menos(posAgente);
        double angulo       = Math.toDegrees(posBola.umVetor().direction());
        double anguloAgente = 0;
                
        if(true)
             sinal = ( Util.isAnguloNoIntervalo(angulo, anguloAgente,Util.simplifyAngle(anguloAgente+180) ) ) ? 1 : -1;
        
        //timeUltimoProcuraBola   = time;
        Vetor2D angTurn        = new Vetor2D();
        angTurn.setCoordPolar(Math.toRadians(Util.simplifyAngle(anguloAgente + 60 * sinal)), 1);
                
        posAgente.mais(angTurn.asPoint());
        System.out.println(sinal);
        System.out.println(angTurn.asPoint().render());
        turnBodyParaPonto(posAgente, sinal);
    }
    
    public void infereVelocidade(){
        double distance = 5;
        double erx      = 4/distance;
        double ery      = 3/distance;
        double dChange  = 8.5;
        double distChan = -2.0;
        double vyo      = 0.3;
        double vxo      = 0.5;
        
        double vely     = (((erx*distance*dChange*Math.PI)/180) + (erx*erx*vyo) + (ery*ery*vyo) + (ery*distChan))/
                ((ery*ery) + (erx*erx));
        
        double velx     = (distChan + (erx*vxo + ery*vyo) - ery*vely)/erx;
        
        double vrx      = velx - vxo;
        double vry      = vely - vyo;
        double aux      = (vrx * erx) + (vry * ery);
        double daux     = ((-(vrx * ery) + (vry * erx))/distance) * (180/Math.PI);
        
        System.out.println("vel ( " + velx + " , " + vely + " )");
        System.out.println("distChange = " + distChan + " distAux = " + aux );
        System.out.println("dirchange = " + dChange + " dirAux = " + daux);
    }
    
    public Ponto predictPosDeOutroJogador(Jogador player, int ciclos){
        double dDirection  = 0;
        Ponto pontoPlayer  = new Ponto(0,0);
        VetorVelocidade vel = new VetorVelocidade(0,0);

        for( int i = 0; i < ciclos ; i ++ ){
            double dAcc     = 100 * Configuracoes.DASH_POWER_RATE;
            if( dAcc > 0 ){
                Vetor2D aux = new Vetor2D();
                aux.setCoordPolar(Math.toRadians(dDirection), dAcc);
                vel.mais(aux);
            }else{
                Vetor2D aux = new Vetor2D();
                aux.setCoordPolar(Math.toRadians(Util.simplifyAngle(dDirection + 180)), Math.abs(dAcc));
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
    
    public Ponto getPontoDeIntersecaoBola(){
        Objetos bola    = new ObjetoMovel();
        bola.posicao.atualizar(15, 0, 1, 1);
        bola.velocidade     = new VetorVelocidade();
        bola.velocidade.setCoordPolar(Math.toRadians(-135), 1);
        
        Ponto posAgente     = new Ponto(0,0);
        VetorVelocidade vel  = new VetorVelocidade(0,0);//player velocity
        double dSpeed, dDistExtra;
        Ponto posMe,posBall = null;
        double ang, angBody, angNeck;
        InfoCorpo sta       = new InfoCorpo();
        sta.effort          = 1;
        double dMaxDist;
        
        dMaxDist        = Util.kickable_radius();
        dSpeed          = 0;
        dDistExtra      = Util.getSumInfGeomSeries(dSpeed, Configuracoes.JOGADOR_PARAMS.PLAYER_DECAY);
        Vetor2D posAux = new Vetor2D();
        posAux.setCoordPolar(vel.direction(), dDistExtra);
        posAgente.mais(posAux.asPoint());
        
        for (int i = 0; i < 30; i++) {
            vel         = new VetorVelocidade(0,0);//player velocity
            angBody     = 45;
            angNeck     = 45;
            posBall     = Util.predictlPosBolaDepoisNCiclos(bola, i+1);
            posMe       = new Ponto(0,0);
            Ponto aux   = new Ponto(posBall);
            aux.menos(posAgente);
            ang         = Math.toDegrees(aux.umVetor().direction());
            ang         = Util.simplifyAngle(ang - angBody );
            int turn    = 0;
            
            while(Math.abs(ang) > 7 && turn < 5){
                turn++;
                double dirBodyENeck[] = Util.predictEstadoAfterTurn(Util.getAnguloParaTurn(ang, vel.magnitude()), posMe, vel, angBody, angNeck, sta);
                aux         = new Ponto(posBall);
                aux.menos(posAgente);
                angBody     = dirBodyENeck[0];
                angNeck     = dirBodyENeck[1];
                ang         = Math.toDegrees(aux.umVetor().direction());
                ang         = Util.simplifyAngle(ang - angBody);
            }
            
            for (; turn < i; turn++) {
                Util.predictEstadoDepoisDoDash(posMe, vel, Configuracoes.JOGADOR_PARAMS.DASH_POWER_MAX, 1, sta, angBody);
            }
            
            if (posMe.distanciaAoPonto( posBall ) < dMaxDist || (posMe.distanciaAoPonto( posAgente) > posBall.distanciaAoPonto( posAgente ) + dMaxDist) ){                
                return posBall;
            }
            
        }       
        return posBall;
    }
    
    public void turnNeckToDirection(double direction){
        double headDir      = 0;
        double angleToTurn  = direction - headDir;
        double relNeck      = 20;
        
        angleToTurn         = Util.validarTurNeckAngle(angleToTurn, relNeck);
        System.out.println(angleToTurn);
    }
    
    public static void main(String[] args) {
        long tempoInicial = System.currentTimeMillis();
        
        long tiaAnielly = System.nanoTime();
        for (int i = 0; i < 10; i++) {
            int a = 1;
        }
        long tiaAnielly2 = System.nanoTime();
        
        long tempoFinal = System.currentTimeMillis();
        
        System.out.println(tiaAnielly2 - tiaAnielly);
              
    }
    
    
}
