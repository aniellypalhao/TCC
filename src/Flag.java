/**
 * Classe Flag que extende ObjetoFixo para representar uma flag
 */
public class Flag extends ObjetoFixo {
    
    /**
     * This constructor automatically assign's the flag's posicao based on its ObjectId.
     * 
     * @param id the flag's ObjectId
     */
    public Flag(String id) {
        this.id = id;
        this.posicao = this.setPosition();
    }

    /**
     * Returns this flag's posicao based on its ObjectId.
     * 
     * @return this flag's posicao based on its ObjectId
     */
    protected Posicao setPosition() {
    	//A switch{} would be better, but not sure if we will have JRE 1.7 available
    	if (this.id == "(f t l 50)") {
            return new Posicao(-50, -39, 1.0, -1);
        }
    	else if (this.id == "(f t l 40)") {
            return new Posicao(-40, -39, 1.0, -1);
        }
    	else if (this.id == "(f t l 30)") {
            return new Posicao(-30, -39, 1.0, -1);
        }
    	else if (this.id == "(f t l 20)") {
            return new Posicao(-20, -39, 1.0, -1);
        }
    	else if (this.id == "(f t l 10)") {
            return new Posicao(-10, -39, 1.0, -1);
        }
    	else if (this.id == "(f t 0)") {
            return new Posicao(0, -39, 1.0, -1);
        }
    	else if (this.id == "(f t r 10)") {
            return new Posicao(10, -39, 1.0, -1);
        }
    	else if (this.id == "(f t r 20)") {
            return new Posicao(20, -39, 1.0, -1);
        }
    	else if (this.id == "(f t r 30)") {
            return new Posicao(30, -39, 1.0, -1);
        }
    	else if (this.id == "(f t r 40)") {
            return new Posicao(40, -39, 1.0, -1);
        }
    	else if (this.id == "(f t r 50)") {
            return new Posicao(50, -39, 1.0, -1);
        }
    	else if (this.id == "(f r t 30)") {
            return new Posicao(57.5, -30, 1.0, -1);
        }
    	else if (this.id == "(f r t 20)") {
            return new Posicao(57.5, -20, 1.0, -1);
        }
    	else if (this.id == "(f r t 10)") {
            return new Posicao(57.5, -10, 1.0, -1);
        }
    	else if (this.id == "(f r 0)") {
            return new Posicao(57.5, 0, 1.0, -1);
        }
    	else if (this.id == "(f r b 10)") {
            return new Posicao(57.5, 10, 1.0, -1);
        }
    	else if (this.id == "(f r b 20)") {
            return new Posicao(57.5, 20, 1.0, -1);
        }
    	else if (this.id == "(f r b 30)") {
            return new Posicao(57.5, 30, 1.0, -1);
        }
    	else if (this.id == "(f b r 50)") {
            return new Posicao(50, 39, 1.0, -1);
        }
    	else if (this.id == "(f b r 40)") {
            return new Posicao(40, 39, 1.0, -1);
        }
    	else if (this.id == "(f b r 30)") {
            return new Posicao(30, 39, 1.0, -1);
        }
    	else if (this.id == "(f b r 20)") {
            return new Posicao(20, 39, 1.0, -1);
        }
    	else if (this.id == "(f b r 10)") {
            return new Posicao(10, 39, 1.0, -1);
        }
    	else if (this.id == "(f b 0)") {
            return new Posicao(0, 39, 1.0, -1);
        }
    	else if (this.id == "(f b l 10)") {
            return new Posicao(-10, 39, 1.0, -1);
        }
    	else if (this.id == "(f b l 20)") {
            return new Posicao(-20, 39, 1.0, -1);
        }
    	else if (this.id == "(f b l 30)") {
            return new Posicao(-30, 39, 1.0, -1);
        }
    	else if (this.id == "(f b l 40)") {
            return new Posicao(-40, 39, 1.0, -1);
        }
    	else if (this.id == "(f b l 50)") {
            return new Posicao(-50, 39, 1.0, -1);
        }
    	else if (this.id == "(f l b 30)") {
            return new Posicao(-57.5, 30, 1.0, -1);
        }
    	else if (this.id == "(f l b 20)") {
            return new Posicao(-57.5, 20, 1.0, -1);
        }
    	else if (this.id == "(f l b 10)") {
            return new Posicao(-57.5, 10, 1.0, -1);
        }
    	else if (this.id == "(f l 0)") {
            return new Posicao(-57.5, 0, 1.0, -1);
        }
    	else if (this.id == "(f l t 10)") {
            return new Posicao(-57.5, -10, 1.0, -1);
        }
    	else if (this.id == "(f l t 20)") {
            return new Posicao(-57.5, -20, 1.0, -1);
        }
    	else if (this.id == "(f l t 30)") {
            return new Posicao(-57.5, -30, 1.0, -1);
        }
    	else if (this.id == "(f l t)") {
            return new Posicao(-52.5, -34, 1.0, -1);
        }
    	else if (this.id == "(f r t)") {
            return new Posicao(52.5, -34, 1.0, -1);
        }
    	else if (this.id == "(f r b)") {
            return new Posicao(52.5, 34, 1.0, -1);
        }
    	else if (this.id == "(f l b)") {
            return new Posicao(-52.5, 34, 1.0, -1);
        }
    	else if (this.id == "(f c t)") {
            return new Posicao(0, -34, 1.0, -1);
        }
    	else if (this.id == "(f c)") {
            return new Posicao(0, 0, 1.0, -1);
        }
    	else if (this.id == "(f c b)") {
            return new Posicao(0, 34, 1.0, -1);
        }
    	else if (this.id == "(f p l t)") {
            return new Posicao(-36, -20.15, 1.0, -1);
        }
    	else if (this.id == "(f p l c)") {
            return new Posicao(-36, 0, 1.0, -1);
        }
    	else if (this.id == "(f p l b)") {
            return new Posicao(-36, 20.15, 1.0, -1);
        }
    	else if (this.id == "(f p r t)") {
            return new Posicao(36, -20.15, 1.0, -1);
        }
    	else if (this.id == "(f p r c)") {
            return new Posicao(36, 0, 1.0, -1);
        }
    	else if (this.id == "(f p r b)") {
            return new Posicao(36, 20.15, 1.0, -1);
        }
    	else if (this.id == "(f g l t)") {
            return new Posicao(-52.5, -7.01, 1.0, -1);
        }
    	else if (this.id == "(f g l b)") {
            return new Posicao(-52.5, 7.01, 1.0, -1);
        }
    	else if (this.id == "(f g r t)") {
            return new Posicao(52.5, -7.01, 1.0, -1);
        }
    	else if (this.id == "(f g r b)") {
            return new Posicao(52.5, 7.01, 1.0, -1);
        }
    	return new Posicao(Double.NaN, Double.NaN, 0.0, -1);
    }
}
