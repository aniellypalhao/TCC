/**
 * Data structure extension of Objetos that represents a stationary object
 on the playing field.
 */
public class ObjetoFixo extends Objetos {
    
	/**
	 * Default constructor; builds a StationaryObject with default Objetos
 values.
	 */
    public ObjetoFixo() {
    }

    /**
     * Primary constructor.
     * 
     * @param id identifying string literal
     * @param x the x-coordinate
     * @param y the y-coordinate
     */
    public ObjetoFixo(String id, double x, double y) {
        if (!Util.isCorrectlyFormatted(id)) {
            System.out.println("id sent to stationary object constructor: " + id);
            return;
        }
        this.id = id;
        this.posicao = new Posicao(x, y, 1.0, -1);
    }
    
    /**
     * Returns true if this is a stationary object.
     * 
     * @return true
     */
    public boolean isStationaryObject() {
        return true;
    }
}
