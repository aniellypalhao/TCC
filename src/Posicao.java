public class Posicao extends Estimativa {
    private Ponto posicao = new Ponto();
 
    public Posicao() {
    }
    
    /**
     * Crie uma estimativa copiando uma estimativa existente.
     * 
     * @param estimativa copia a estimativa.
     */
    public Posicao(Posicao estimativa) {
        this.posicao.update(estimativa.getPosicao());
        this.confiancaInicial = estimativa.getConfiancaInicial();
        this.cicloDaEstimativa = estimativa.getCicloDaEstimativa();
    }
    
    /**
     * Posição constrututor.
     * 
     * @param p ponto representando a posicao
     * @param confianca na posição
     * @param ciclo em que a posição esta sendo estimada
     */
    public Posicao(Ponto p, double confidence, int ciclo) {
        this.posicao.update(p);
        this.confiancaInicial = confidence;
        this.cicloDaEstimativa = ciclo;
    }
    
    /**
     * 
     * @param x coordenada x da posição(ponto)
     * @param y cordenada x da posição(ponto)
     * @param confianca confiança do valor
     * @param ciclo tempo estimado do ciclo
     */
    public Posicao(double x, double y, double confianca, int ciclo) {
        this.posicao.atualizar(x, y);
        this.confiancaInicial = confianca;
        this.cicloDaEstimativa = ciclo;
    }
    
    ///////////////////////////////////////////////////////////////////////////
    // STATIC FUNCTIONS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Get the confidence at a given time step.
     * 
     * @param ciclo ciclo atual do jogo
     * @return a confiança naquele ciclo
     */
    public final double getConfianca(int ciclo) { 
        if( cicloDaEstimativa == -1 )       
            return 0.0;
        double dConf = confiancaInicial;
        int dif      = (ciclo - cicloDaEstimativa);
        for (int i = 0; i < dif ; i++) {
            dConf *= 0.99;
        }
        if( dConf > 1.0 )
            return 0.0;
        return dConf;
    }
    
  
    public final Ponto getPosicao() {
        return this.posicao;
    }

    /**
     * Obtém a coordenada x da posicao.
     * 
     * @return a coordenada x da posicao.
     */
    public final double getX() {
    	return this.posicao.getX();
    }
    
    /**
     * Obtém a coordenada y da posicao.
     * 
     * @return a coordenada x da posicao.
     */
    public final double getY() {
    	return this.posicao.getY();
    }
    

    /**
     * Renders the estimate as a string (mainly useful for debugging.)
     * 
     * @param ciclo
     * @return a string representation of the estimate
     */
    public final String renderizar(int ciclo) {
        return this.posicao.render() + " com " + Double.toString(this.getConfianca(ciclo)) + " confiança.";
    }
    
    /**
     * @param p nova posição
     * @param confianca um novo valor para confiança.
     * @param ciclo um ciclo estimado
     */
    public final void atualizar(Ponto p, double confianca, int ciclo) {
        this.atualizar(p.getX(), p.getY(), confianca, ciclo);
    }
    
    /**
     * Updates the estimate.
     * 
     * @param x a coordenada x do nova posição
     * @param y a coordenada x do nova posição
     * @param confianca um novo valor de confiança
     * @param ciclo ciclo da estimativa.
     */
    public final void atualizar(double x, double y, double confianca, int ciclo) {
        Ponto oldPosition = new Ponto(this.posicao.getX(), this.posicao.getY());
        this.confiancaInicial = confianca;
        this.cicloDaEstimativa = ciclo;
        this.posicao.atualizar(x, y);
        double distancia = this.posicao.distanceTo(oldPosition);
    }
}
