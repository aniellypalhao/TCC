/**
 * Classe Gol que extende ObjetoFixo para representar um Gol
 */
public class Gol extends ObjetoFixo {
    
    /**
     * Goal constructor. Automatically assigns the correct posicao.
     * 
     * @param id the ObjectId of the goal
     */
    public Gol(String id) {
        this.id = id;
        this.posicao = this.setPosition();
    }

    /**
     * Returns the posicao of a goal, using it's ObjectId.
     * 
     * @return the goal's posicao
     */
    protected Posicao setPosition() {
    	//A switch{} would be better, but not sure if we will have JRE 1.7 available
    	if (this.id == "(goal l)") {
            return new Posicao(-52.5, 0, 1.0, -1);
        }
    	else if (this.id == "(goal r)") {
            return new Posicao(52.5, 0, 1.0, -1);
        }
    	// Poor error handling
    	return new Posicao(-1.0, -1.0, 0.0, -1);
    }
}
