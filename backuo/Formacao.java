
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Valderlei
 */
public class Formacao {
    
    public enum POSICAO{
        GOLEIRO,
        ZAGUEIRO,
        LATERAL,
        VOLANTE,
        MEIO_CAMPISTA,
        MEIO_CAMPISTA_LATERAL,
        PONTA_ATACANTE,
        ATACANTE        
    }
    
    public LayoutFormacao[] formacaoEmCurso = this.FORMACAO_INICIAL;
    
    public Point getPontoEstrategico(int numero, Point posBola, double maxX, boolean isNossaBola, double maxYPorcentagem, LayoutFormacao[] formacao){
        LayoutFormacao[] aux = formacao;
        
        if(formacaoEmCurso == null){
            System.out.println("formação nula");
        }
        
        if(formacao != null){
            setFormacao(formacao);
        }
        
        if(formacaoEmCurso == null){
            System.out.println("formação nula");
        }
                
        Point pontoOriginal;
        double x,y;
                
        pontoOriginal = this.posOriginal(numero);
        y = pontoOriginal.getY() + posBola.getY() * this.getAncoraY(numero);
        x = pontoOriginal.getX() + posBola.getX() * this.getAncoraX(numero);
        
        if( Math.abs( y ) > 0.5* maxYPorcentagem * Settings.FIELD_HEIGHT )
            y = Math.signum(y) * 0.5 * maxYPorcentagem * Settings.FIELD_HEIGHT;
        
        if( this.isAtrasBola(numero)== true && x > posBola.getX() )
            x = posBola.getX();
        
        if( x > this.getXmax(numero) )
            x = this.getXmax(numero);
        else if( x < this.getXmin(numero) )
            x = this.getXmin(numero);
        
        if( x > maxX )
            x = maxX;        
        
        this.setFormacao(aux);
        
        return new Point(x,y);
    }   
    
    public void setFormacao(LayoutFormacao[] f){
        formacaoEmCurso = f;
    }
    
    public Point posOriginal(int numero){
        return new Point(formacaoEmCurso[numero - 1].getX() , formacaoEmCurso[numero - 1].getY());
    }
    
    public boolean isPosicao(POSICAO p, int numero){
        return formacaoEmCurso[numero - 1].getPosicao() == p;
    }
    
    public double getXmin(int numero){
        return formacaoEmCurso[numero - 1].getxMin();
    }
    
    public double getXmax(int numero){
        return formacaoEmCurso[numero - 1].getxMax();
    }
    
    public double getAncoraX(int numero){
        return formacaoEmCurso[numero - 1].getAncoraX();
    }
    
    public double getAncoraY(int numero){
        return formacaoEmCurso[numero - 1].getAncoraY();
    }
    
    public boolean isAtrasBola(int numero){
        return formacaoEmCurso[numero - 1].isAtrasBola();
    }
    
    public POSICAO getPosicao(int numero){
        return formacaoEmCurso[numero - 1].getPosicao();
    }

    public static LayoutFormacao[] FORMACAO_INICIAL = {
        new LayoutFormacao( -50 , 0 , 0 , 0 , false , -49 , 0 ,POSICAO.GOLEIRO),//1
        new LayoutFormacao( -16 , 16 , 0 , 0 , false , -45 , 0 , POSICAO.LATERAL),//4
        new LayoutFormacao( -17 , 5 , 0 , 0 , false , -45 , 0 , POSICAO.ZAGUEIRO),//2
        new LayoutFormacao( -17 , -5 , 0 , 0 , false , -45 , 0 , POSICAO.ZAGUEIRO),
        new LayoutFormacao( -16 , -16 , 0 , 0 , false , -45 , 0 , POSICAO.LATERAL),
        new LayoutFormacao( -8 , 0 , 0 , 0 , false , -45 , 0 , POSICAO.MEIO_CAMPISTA),//5
        new LayoutFormacao( -5 , 10 , 0 , 0 , false , -45 , 0 , POSICAO.MEIO_CAMPISTA_LATERAL),//6
        new LayoutFormacao( -5 , -10 , 0 , 0 , false , -45 , 0 ,POSICAO.MEIO_CAMPISTA_LATERAL),
        new LayoutFormacao( -2 , 0 , 0 , 0 , false , -45 , 0 , POSICAO.ATACANTE),//8
        new LayoutFormacao( -1 , 22 , 0 , 0 , false , -40 , 0 , POSICAO.PONTA_ATACANTE),//7
        new LayoutFormacao( -1 , -22 , 0 , 0 , false , -40 , 0 , POSICAO.PONTA_ATACANTE)
    };
    
    public static LayoutFormacao[] FORMACAO_433_OFENSIVO = {
        new LayoutFormacao( -50 , 0 , 0 , 0 , false , 0 , 0 , POSICAO.GOLEIRO),
        new LayoutFormacao( -16.5 , 10 , 0.1 , 0.1 , true , -50 , -30 , POSICAO.LATERAL),
        new LayoutFormacao( -21 , 0 , 0.7 , 0.2 , true , -42 , 0, POSICAO.VOLANTE ),
        new LayoutFormacao( -15 , 0 , 0.65 , 0.4 , true , -47 , 2, POSICAO.ZAGUEIRO ),
        new LayoutFormacao( -16.5 , -10 , 0.7 , 0.25 , false , -45 , 2 ,POSICAO.LATERAL),
        new LayoutFormacao( 0 , -11 , 0.65 , 0.3 , false , -36 , 42, POSICAO.MEIO_CAMPISTA_LATERAL ),
        new LayoutFormacao( 0 , 11 , 0.7 , 0.25 , false , -36 , 42, POSICAO.MEIO_CAMPISTA_LATERAL ),
        new LayoutFormacao( -3 , 0.5 , 0.5 , 0.3 , false , -2 , 44, POSICAO.MEIO_CAMPISTA),
        new LayoutFormacao( 15 , -0.5 , 0.6 , 0.25 , false , -2 , 44 , POSICAO.ATACANTE),
        new LayoutFormacao( 18 , 19 , 0 , 0 , false , -10 , 50 , POSICAO.PONTA_ATACANTE),
        new LayoutFormacao( 18 , -19 , 0 , 0 , false , -10 , 50 , POSICAO.PONTA_ATACANTE )
    };
    
    public static LayoutFormacao[] FORMACAO_442_DEFENSIVO = {
        new LayoutFormacao( -45 , 0 , 0 , 0 , false , 0 , 0 , POSICAO.GOLEIRO),
        new LayoutFormacao( -12 , 21 , 0.1 , 0.1 , true , -52.5 , -30 , POSICAO.LATERAL),
        new LayoutFormacao( -14 , 5 , 0.5 , 0.25 , true , -45 , 0 , POSICAO.ZAGUEIRO),
        new LayoutFormacao( -14 , -5 , 0.5 , 0.25 , true , -45 , 2 , POSICAO.ZAGUEIRO),
        new LayoutFormacao( -12 , -21 , 0.4 , 0.2 , false , -45 , 35 ,POSICAO.LATERAL),
        new LayoutFormacao( 1 , 24 , 0.6 , 0.3 , false , -30 , 42, POSICAO.MEIO_CAMPISTA_LATERAL ),
        new LayoutFormacao( -5 , 8 , 0.5 , 0.15 , false , -30 , 42, POSICAO.MEIO_CAMPISTA ),
        new LayoutFormacao( -5 , -8 , 0.5 , 0.25 , false , -45 , 47, POSICAO.MEIO_CAMPISTA),
        new LayoutFormacao( 1 , -24.0 , 0.6 , 0.3 , false , 10 , 47, POSICAO.MEIO_CAMPISTA_LATERAL ),
        new LayoutFormacao( 18 , 7 , 0 , 0 , false , -10 , 50, POSICAO.ATACANTE ),
        new LayoutFormacao( 18 , -7 , 0 , 0 , false , -10 , 50, POSICAO.ATACANTE )
    };

    public static class LayoutFormacao {

        private double x;
        private double y;
        private double ancoraX;
        private double ancoraY;
        private boolean atrasBola;
        private double xMin;
        private double xMax;
        private Formacao.POSICAO posicao;

        public LayoutFormacao(double x, double y, double ancoraX, double ancoraY, boolean isBehind, double xMin, double xMax, Formacao.POSICAO posicao) {
            this.x = x;
            this.y = y;
            this.ancoraX = ancoraX;
            this.ancoraY = ancoraY;
            this.atrasBola = isBehind;
            this.xMin = xMin;
            this.xMax = xMax;
            this.posicao = posicao;
        }

        public double getX() {
            return x;
        }

        public void setX(double x) {
            this.x = x;
        }

        public double getY() {
            return y;
        }

        public void setY(double y) {
            this.y = y;
        }

        public double getAncoraX() {
            return ancoraX;
        }

        public void setAncoraX(double ancoraX) {
            this.ancoraX = ancoraX;
        }

        public double getAncoraY() {
            return ancoraY;
        }

        public void setAncoraY(double ancoraY) {
            this.ancoraY = ancoraY;
        }

        public boolean isAtrasBola() {
            return atrasBola;
        }

        public void setAtrasBola(int isBehind) {

            this.atrasBola = isBehind == 1;
        }

        public double getxMin() {
            return xMin;
        }

        public void setxMin(double xMin) {
            this.xMin = xMin;
        }

        public double getxMax() {
            return xMax;
        }

        public void setxMax(double xMax) {
            this.xMax = xMax;
        }        
        
        public POSICAO getPosicao() {
            return posicao;
        }

        public void setPosicao(POSICAO posicao) {
            this.posicao = posicao;
        }

    }

}
