
import java.util.Random;

/**
 * Classe que representa uma area retangular no campo de jogo.
 */
public class Retangulo {

    private double inferior = -1.0;
    private double esquerda = -1.0;
    private double direita = -1.0;
    private double superior = -1.0;

    /**
     * Construtor, constrói um retângulo com base na posição de cada borda.
     *
     * @param superior Posição vertical superior da borda superior
     * @param direita Posição horizontal da borda direita
     * @param inferior Posição vertical da borda inferior
     * @param esquerda Posição horizontal do contorno esquerdo
     */
    public Retangulo(double superior, double direita, double inferior, double esquerda) {
        this.superior = superior;
        this.direita = direita;
        this.inferior = inferior;
        this.esquerda = esquerda;
    }

    /**
     * Verifica se um objeto está dentro da área desse retângulo.
     *
     * @param objeto O objeto de campo a ser testado
     * @return true if the object is inside this rectangle, otherwise false.
     */
    public boolean contains(Objetos objeto) {
        double x = objeto.posicao.getPosicao().getX();
        double y = objeto.posicao.getPosicao().getY();
        return x >= esquerda && x <= direita && y <= inferior && y >= superior;
    }
    
    public boolean contem(Ponto ponto) {
        if(ponto == null)
            return false;
        
        double x = ponto.getX();
        double y = ponto.getY();
        return x >= esquerda && x <= direita && y <= inferior && y >= superior;
    }

    /**
     * Obtém o ponto central deste retângulo.
     *
     * @return O centro do retângulo como um objeto Ponto.
     */
    public Ponto getCentro() {
        double centroX = (esquerda + direita) / 2;
        double centroY = (inferior + superior) / 2;
        return new Ponto(centroX, centroY);
    }

    public double getSuperior() {
        return this.superior;
    }

    public double getDireita() {
        return this.direita;
    }

    public double getInferior() {
        return this.inferior;
    }

    public double getEsquerda() {
        return this.esquerda;
    }
    
    public void render(){
        System.out.println("SUPERIOR = " + getSuperior());
        System.out.println("INFERIOR = " + getInferior());
        System.out.println("ESQUERDA = " + getEsquerda());
        System.out.println("DIREITA = " + getDireita());
    }

    public Ponto gerarPontoDentroDoRetangulo() {
        double x, y, margem;
        Random r = new Random();
        Double margemY = Math.abs(this.getInferior() - this.getSuperior());
        Double margemX = Math.abs(this.getEsquerda() - this.getDireita());

        y = getInferior() - r.nextInt(margemY.intValue());
        x = getEsquerda() + r.nextInt(margemX.intValue());
        
        return new Ponto(x,y);
    }

}
