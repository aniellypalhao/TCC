/**
 * Representação de classe de um ponto em um plano 2D, com funções auxiliares para
 encontrar distância e ângulos entre objetos de Ponto.
 */
public class Ponto {
    private double x;
    private double y;
    
    public Ponto() {
        x = Double.NaN;
        y = Double.NaN;
    }
    
    public Ponto(Ponto point) {
        this.x = point.getX();
        this.y = point.getY();
        
        
        
    }

    /**
     * Construtor, constrói um objeto Ponto baseado nas coordenadas fornecidas
     * 
     * @param x a cordenada x
     * @param y a cordenada y
     */
    public Ponto(double x, double y) {
        this.x = x;
        this.y = y;
    }
    
    ///////////////////////////////////////////////////////////////////////////
    // STATIC FUNCTIONS
    ///////////////////////////////////////////////////////////////////////////
    /**
     * Returns a Ponto with NaN values.
     * 
     * @return a Ponto with NaN values.
     */
    public final static Ponto Desconhecido() {
        return new Ponto(Double.NaN, Double.NaN);
    }
    
    /** 
     * Gets the angle between this Ponto and another Ponto object.
     * Uses the formula \f$angle = \arctan\left(\frac{y_2-y_1}{x_2-x_1}\right)\f$.
     * 
     * @param outroPonto the Ponto object to consider
     * @return the angle between this Ponto and otherPoint
     */
    public final double anguloAbsolutoAoPonto(Ponto outroPonto) {
        double dx = this.deltaX(outroPonto);
        double dy = this.deltaY(outroPonto);
        // Handle Math.atan() failure case
        if (dx == 0.0) {
            if (dy >= 0.0) {
                return 90.0;
            }
            else {
                return -90.0;
            }
        }
        double angle = Math.toDegrees(Math.atan(dy/dx));
        if (dx > 0) {
            return angle;
        }
        else {
            return 180.0 + angle;
        }
    }
    
    /**
     * Returns a 2D vector representing the point.
     * 
     * @return a 2D vector representing the point
     */
    public final Vetor2D umVetor() {
        return new Vetor2D(this.x, this.y);
    }
   
    public final boolean ehDesconhecido() {
        return Double.isNaN(this.x) || Double.isNaN(this.y);
    }
    
    
    public void atualizar(double x, double y) {
        this.x = x;
        this.y = y;
    }
    
    /**
     * Copia as coordenadas do Ponto especificadas para este Ponto.
     * 
     * @param ponto copia as cordenadas para este ponto.
     */
    public void atualizar(Ponto ponto) {
        this.x = ponto.getX();
        this.y = ponto.getY();
    }
    
    /**
     * Obtem a x-cordenada.
     * 
     * @return A cordenada X
     */
    public double getX() {
        return this.x;
    }

    /**
     * Obtem a cordenada Y
     * 
     * @return A cordenada Y
     */
    public double getY() {
        return this.y;
    }
    
    public void setX(double x){
        this.x = x;
    }
    
    public void setY(double y){
        this.y = y;
    }
    
    /**
     * Determina se este Ponto é equivalente a um objeto Ponto especificado.
     * 
     * @param outroPonto Ponto para ser comparado.
     * @return true caso as duas coordenadas forem iguais, caso contrário false.
     */
    public boolean ehIgual(Ponto outroPonto) {
        return this.getX() == outroPonto.getX() && this.getY() == outroPonto.getY();
    }
    
    /**
     * Recupera a diferença de coordenadas x entre este Ponto e
     * Outro objeto Ponto. 
     * 
     * @param otherPoint o ponto a ser calculado a diferença
     * @return Diferença entre as cordenadas X
     */
    public double deltaX(Ponto outroPonto) {
        return outroPonto.getX() - this.getX();
    }
    
    /**
     * @param outroPonto O objeto Ponto para calcular contra
     * @return Difetença entre a cordenada Y
     */
    public double deltaY(Ponto outroPonto) {
        return outroPonto.getY() - this.getY();
    }
    
    /**
     * Retrieves the distance between this Ponto and another Ponto object.
     * The formula used is \f$dist = \sqrt{(\Delta x)^2 + (\Delta y)^2}\f$.
     * 
     * @param outroPonto O objeto Ponto para calcular contra
     * @return Distância entre os objetos Ponto
     */
    public double distanciaAoPonto(Ponto outroPonto) {
        return Math.hypot(this.deltaX(outroPonto), this.deltaY(outroPonto));
    }
    
    /**
     * Retorna o ponto médio.
     * 
     * @param p o outro ponto
     * @return O ponto médio entre este ponto e o outro
     */
    public final Ponto pontoMedio(Ponto p) {
        return new Ponto( (this.x + p.getX()) / 2.0, (this.y + p.getY()) / 2.0);
    }
    
    /** 
     * @return a string formatada
     */
    public String render() {
        return String.format("(%f, %f)", this.x, this.y);
    }
    
    public final void mais(Ponto vet){
        this.x += vet.getX();
        this.y += vet.getY();
    }
    
    public final void menos(Ponto vet){
        this.x -= vet.getX();
        this.y -= vet.getY();
    }
    
    public final void vezesEscalar(double escalar){
        this.x *= escalar;
        this.y *= escalar;
    }
    
    public boolean aEsquerda( Ponto p ){
      return this.getY() < p.getY();
    }
    
    public boolean aDireita( Ponto p ){
      return this.getY() > p.getY();
    }
    
  
}
