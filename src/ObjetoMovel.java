/** @file MobileObject.java
* Represents a moving physical object positioned somewhere on the field.
* 
* @author Team F(utility)
*/



/**
 * Extension of Objetos representing an object that can move.
 */
public class ObjetoMovel extends Objetos{

	/**
	 * Default constructor, initializes with default Objetos values.
	 */
    public ObjetoMovel() {}
	
    /**
     * Default mobile object constructor.
     * 
     * @param x an x-coordinate for the object's initial position
     * @param y a y-coordinate for the object's initial position
     */
    public ObjetoMovel(double x, double y) {
    	super(x, y);
    }
}
