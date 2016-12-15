
import java.util.Random;

/**
 * @file Rectangle.java Represents a rectangular area on the playing field.
 *
 * @author Team F(utility)
 */
/**
 * Class representation of a rectangular area on the playing field.
 */
public class Rectangle {

    private double bottom = -1.0;
    private double left = -1.0;
    private double right = -1.0;
    private double top = -1.0;

    /**
     * Constructor, builds a rectangle based on position of each border.
     *
     * @param top vertical position of the top border
     * @param right horizontal position of the right border
     * @param bottom vertical position of the bottom border
     * @param left horizontal position of the left border
     */
    public Rectangle(double top, double right, double bottom, double left) {
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.left = left;
    }

    /**
     * Checks if an object lies within the area of this rectangle.
     *
     * @param object the field object to test for
     * @return true if the object is inside this rectangle, otherwise false.
     */
    public boolean contains(FieldObject object) {
        double x = object.position.getPosicao().getX();
        double y = object.position.getPosicao().getY();
        return x >= left && x <= right && y <= bottom && y >= top;
    }
    
    public boolean contains(Point ponto) {
        if(ponto == null)
            return false;
        
        double x = ponto.getX();
        double y = ponto.getY();
        return x >= left && x <= right && y <= bottom && y >= top;
    }

    /**
     * Gets the center point of this rectangle.
     *
     * @return the center of the rectangle as a Point object.
     */
    public Point getCenter() {
        double centerX = (left + right) / 2;
        double centerY = (bottom + top) / 2;
        return new Point(centerX, centerY);
    }

    /**
     * Gets the vertical position of the top border.
     *
     * @return vertical position
     */
    public double getTop() {
        return this.top;
    }

    /**
     * Gets the horizontal position of the right border.
     *
     * @return horizontal position
     */
    public double getRight() {
        return this.right;
    }

    /**
     * Gets the vertical position of the bottom border.
     *
     * @return vertical position
     */
    public double getBottom() {
        return this.bottom;
    }

    /**
     * Gets the horizontal position of the left border.
     *
     * @return horizontal position
     */
    public double getLeft() {
        return this.left;
    }
    
    public void render(){
        System.out.println("TOP = " + getTop());
        System.out.println("BOTTOM = " + getBottom());
        System.out.println("LEFT = " + getLeft());
        System.out.println("RIGHT = " + getRight());
    }

    public Point gerarPontoDentroDoRetangulo() {
        double x, y, margem;
        Random r = new Random();
        Double margemY = Math.abs(this.getBottom() - this.getTop());
        Double margemX = Math.abs(this.getLeft() - this.getRight());

        y = getBottom() - r.nextInt(margemY.intValue());
        x = getLeft() + r.nextInt(margemX.intValue());
        
        return new Point(x,y);
    }

}
