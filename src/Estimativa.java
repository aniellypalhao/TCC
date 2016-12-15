/**
 * Uma estimativa de um valor
 */
public class Estimativa {
    protected double confiancaInicial;
    protected int cicloDaEstimativa;
    
    /**
     * Obtém a confiança como originalmente.
     * 
     * @return a confiança como original.
     */
    public double getConfiancaInicial() {
        return this.confiancaInicial;
    }
    
    /**
     * Retorna a confiança para sempre.
     * Por convenção, quando o cicloDaEstimativa < 0.
     * 
     * @return estimativa mantém sua confiança para sempre.
     */
    public boolean mantemConfiancaParaSempre() {
        return this.cicloDaEstimativa < 0;
    }
    
    public int getCicloDaEstimativa() {
        return this.cicloDaEstimativa;
    }
}
