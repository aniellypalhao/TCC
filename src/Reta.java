/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Valderlei
 */
public class Reta {
    
    //Estas variáveis representam os valores na equação da reta ay+bx+c = 0
    private double a;
    private double b;
    private double c;
    private static final double EPISLON = 0.0001;
    
    public Reta(double a, double b, double c){
        this.a = a;
        this.b = b;
        this.c = c;
    }
    
    public Ponto getIntersecao(Reta reta2){
        Ponto p = new Ponto();
        double x, y;
        
        if( (this.a / this.b) == (reta2.a/reta2.b) ) // RETAS PARALELAS, SEM INTERSEÇÃO
            return p;
        else{
            if(a == 0){                          
                x = -this.c/this.b;                
                y = reta2.getValorY(x);
                
            }else if(reta2.a == 0){
                x = -reta2.c / reta2.b;
                y = this.getValorY(x);
                
            }else{
                x = (this.a*reta2.c - reta2.a*this.c) / (reta2.a*this.b - this.a*reta2.b);
                y = getValorY(x);
            }
            p.setX(x);
            p.setY(y);
        }
            
        return p;
    }
    
    public Reta tangente(Ponto p){
        // ay + bx + c = 0 -> y = (-b/a)x + (-c/a)
        // tangent: y = (a/b)*x + C1 -> by - ax + C2 = 0 => C2 = ax - by
        // with pos.y = y, pos.x = x  
        return new Reta(this.b, -this.a, this.a * p.getX() - this.b * p.getY());
    }
    
    public Ponto pontoNaRetaMaisProxDoPonto(Ponto p){
        Reta r2 = tangente(p);
        return this.getIntersecao(r2);
    }
    
    public static Reta criarRetaEntre2Pontos(Ponto p1, Ponto p2){
        double dA, dB, dC;
        double dTemp = p2.getX() - p1.getX();
        
        if(Math.abs(dTemp) < EPISLON){
            dA = 0.0;
            dB = 1.0;                   
        }else{
            dA = 1.0;
            dB = -(p2.getY() - p1.getY())/dTemp;
        }
        dC =  - dA * p2.getY()  - dB * p2.getX();
        
        return new Reta( dA, dB, dC );        
    }
    
    /**
     * @param angulo tem q ser radiano
     */
    public static Reta criarRetaAPartirDaPosicaoEAngulo(Ponto p, double angulo){
        Ponto p2        = p;
        Vector2D vet    = new Vector2D();
        vet.setCoordPolar(angulo, 1);
        p2.mais(vet.asPoint());
        
        return criarRetaEntre2Pontos(p, p2);
    }
    
    
    public double getValorY(double x){
        if( this.a == 0 )
            return 0;
        return -( this.b * x + this.c ) / this.a;        
    }
    
    
    
}
