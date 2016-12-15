/** @file VelocityVector.java
 * Vector representing a velocity.
 * 
 * @author Team F(utility)
 */



/**
 * Velocity vector class.
 */
public class VetorVelocidade extends Vector2D {

    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    ///////////////////////////////////////////////////////////////////////////
    /**
     * Empty constructor.
     */
    public VetorVelocidade() {
        super();
    }
    
    /**
     * Constructor taking only a magnitude.
     * 
     * @param magnitude the desired magnitude of the vector.
     */
    public VetorVelocidade(double magnitude) {
        super(magnitude);
    }
    
    public VetorVelocidade(Vector2D vec) {
        super(vec);
    }
    
    /**
     * Constructor taking x and y parameters.
     * 
     * @param x x-coordinate
     * @param y y-coordinate
     */
    public VetorVelocidade(double x, double y) {
        super(x, y);
    }
    
    ///////////////////////////////////////////////////////////////////////////
    // METHODS
    ///////////////////////////////////////////////////////////////////////////
    /**
     * Normalizes this vector's magnitude.
     */
    private final void normalize() {
        double ratio = this.magnitude() / Configuracoes.PLAYER_SPEED_MAX;
        if (ratio > 1.0) {
            this.setX(this.getX() / ratio);
            this.setY(this.getY() / ratio);
        }
    }
    
    /**
     * Updates this vector using polar coordinates.
     * 
     * @param dir direction in radians
     * @param mag magnitude
     */
    public final void setPolar(double dir, double mag) {
        this.setX(mag * Math.cos(dir));
        this.setY(mag * Math.sin(dir));
    }
}
