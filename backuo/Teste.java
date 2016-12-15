
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
    
    public void turnBodyParaPonto( Point ponto, int ciclos ){
        Point posGlobal            = new Point(0,0);
        Vector2D vet               = ponto.asVector();
        vet.menos(posGlobal.asVector());
        double angulo              = Math.toDegrees(vet.direction());
        angulo                    -= 0;
        angulo                     = Futil.simplifyAngle(angulo);
        //angulo                     = Futil.getAnguloParaTurn(angulo, player.velocity().magnitude());
        System.out.println(angulo);
    }
    
    public void procurarBola(){
        int sinal           = 1;
        Point posBola       = new Point(1,-1);
        Point posAgente     = new Point(0,0);
        posBola.menos(posAgente);
        double angulo       = Math.toDegrees(posBola.asVector().direction());
        double anguloAgente = 0;
                
        if(true)
             sinal = ( Futil.isAnguloNoIntervalo(angulo, anguloAgente,Futil.simplifyAngle(anguloAgente+180) ) ) ? 1 : -1;
        
        //timeUltimoProcuraBola   = time;
        Vector2D angTurn        = new Vector2D();
        angTurn.setCoordPolar(Math.toRadians(Futil.simplifyAngle(anguloAgente + 60 * sinal)), 1);
                
        posAgente.mais(angTurn.asPoint());
        System.out.println(sinal);
        System.out.println(angTurn.asPoint().render());
        turnBodyParaPonto(posAgente, sinal);
    }
    
    public static void main(String[] args) {
        Teste t  = new Teste ();
        t.procurarBola();
    }
    
    
}
