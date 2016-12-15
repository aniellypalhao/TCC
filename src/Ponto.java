/** @file Point.java
 * Representation of an absolute-coordinate point on a 2D plane.
 * 
 * @author Team F(utility)
 */


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
     * Constructor, builds a Point object based on the provided coordinates.
     * 
     * @param x the x-coordinate
     * @param y the y-coordinate
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
    public final static Ponto Unknown() {
        return new Ponto(Double.NaN, Double.NaN);
    }
    
    /** 
     * Gets the angle between this Ponto and another Ponto object.
     * Uses the formula \f$angle = \arctan\left(\frac{y_2-y_1}{x_2-x_1}\right)\f$.
     * 
     * @param otherPoint the Ponto object to consider
     * @return the angle between this Ponto and otherPoint
     */
    public final double absoluteAngleTo(Ponto otherPoint) {
        double dx = this.deltaX(otherPoint);
        double dy = this.deltaY(otherPoint);
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
    public final Vector2D asVector() {
        return new Vector2D(this.x, this.y);
    }
    
    /**
     * Returns true if either of this point's x and y are unknown.
     * 
     * @return true if either of this point's x and y are unknown
     */
    public final boolean isUnknown() {
        return Double.isNaN(this.x) || Double.isNaN(this.y);
    }
    
    /**
     * Sets this Ponto's coordinates to the specified coordinates.
     * 
     * @param x the x-coordinate
     * @param y the y-coordinate
     */
    public void atualizar(double x, double y) {
        this.x = x;
        this.y = y;
    }
    
    /**
     * Copies the specified Ponto's coordinates to this Ponto.
     * 
     * @param point the Ponto object to copy coordinates from.
     */
    public void update(Ponto point) {
        this.x = point.getX();
        this.y = point.getY();
    }
    
    /**
     * Gets the x-coordinate.
     * 
     * @return the x-coordinate
     */
    public double getX() {
        return this.x;
    }

    /**
     * Gets the y-coordinate.
     * 
     * @return the y-coordinate
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
     * Determines if this Ponto is equivalent to a specified Ponto object.
     * 
     * @param otherPoint the Ponto to compare this Ponto to.
     * @return true if the two Points have the same coordinates, otherwise false.
     */
    public boolean isEqual(Ponto otherPoint) {
        return this.getX() == otherPoint.getX() && this.getY() == otherPoint.getY();
    }
    
    /**
     * Retrieves the difference in x-coordinates between this Ponto and
 another Ponto object. The formula used is \f$\Delta x = x_2 - x_1\f$.
     * 
     * @param otherPoint the Ponto object to compute against
     * @return Difference in x-coordinates
     */
    public double deltaX(Ponto otherPoint) {
        return otherPoint.getX() - this.getX();
    }
    
    /**
     * Retrieves the difference in x-coordinates between this Ponto and
 another Ponto object. The formula used is \f$\Delta y = y_2 - y_1\f$.
     * 
     * @param otherPoint the Ponto object to compute against
     * @return Difference in x-coordinates
     */
    public double deltaY(Ponto otherPoint) {
        return otherPoint.getY() - this.getY();
    }
    
    /**
     * Retrieves the distance between this Ponto and another Ponto object.
     * The formula used is \f$dist = \sqrt{(\Delta x)^2 + (\Delta y)^2}\f$.
     * 
     * @param otherPoint the Ponto object to compute against
     * @return Distance between Ponto objects
     */
    public double distanceTo(Ponto otherPoint) {
        return Math.hypot(this.deltaX(otherPoint), this.deltaY(otherPoint));
    }
    
    /**
     * Returns the midpoint between this point and another.
     * 
     * @param p the other point
     * @return the midpoint between this point and the other
     */
    public final Ponto midpointTo(Ponto p) {
        return new Ponto( (this.x + p.getX()) / 2.0, (this.y + p.getY()) / 2.0);
    }
    
    /**
     * Builds a formatted textual representation of this Ponto object.
     * 
     * @return a formatted string representation
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
