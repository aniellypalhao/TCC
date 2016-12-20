
/**
 * Classe pai para as classes infoCorpo e infoVisao, 
 * que contém os atributos em comum entre as duas infos citadas anteriormente.
 */
public abstract class Info {

	int ciclo;
	
	/**
	 * This default constructor uses the reset method to initialize member variables.
	 */
	public Info(){
		reset();
	}
	
	/**
	 * Resets member variables in preparation for being updated.
	 */
	public void reset(){
		this.ciclo = -1;
	}
	
}
