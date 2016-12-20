/**
 * Representação de um jogador no campo.
 */

public class Jogador extends ObjetoMovel {
    public Brain cerebro = null;
    public Cliente cliente = null;
    public int numero;
    public boolean goleiro;
    public Time outroTime = new Time();
    public Time time = new Time();

    /**
     * Construtor padrão, constrói um jogador sem dados de lógica central ou
     * informações conhecidas.
     */
    public Jogador() {
    }

    /**
     * Inicia um jogador a partir de Id.
     *
     * @param id do jogador
     */
    public Jogador(String id){
    	String[] parcial = id.substring(1, id.length() - 1).split(" ");
    	switch(parcial.length){
    	case 5:
    		//is kicking/tackling? 
    	case 4:
    		this.goleiro = true;
    	case 3:
    		numero = Integer.valueOf(parcial[2]);
    	case 2:
    		time.nome = parcial[1];
    	case 1:
    		
    		break;
    	default:
    		System.out.println("Erro ao analisar um número inesperado de atributos de um jogador. " + id);
    	}
    }
    
    /**
     * Crie um objeto de jogador no campo de jogo com o numero uniforme fornecido.
     * 
     * @param numero do uniforme do jogador.
     */
    public Jogador(int numero) {
        this.numero = numero;
    }
    
    /**
     * Associa um cliente a este jogador.
     * 
     * @param cliente a classe de comunição com o servidor.
     */
    public Jogador(Cliente cliente) {
        cerebro = new Brain(this, cliente);
        this.cliente = cliente;
    }
    
    /**
     * Retorna o id do gol oponente.
     * 
     * @return o id do gol do time oponente.
     */
    public String getIdGolOponente() {
        return new String("(g " + (this.time.side == 'r' ? "l" : "r") + ")");
    }
    
    /**
     * Retorna o ID do gol.
     * 
     * @return o ID do gol do time.
     */
    public String getIdGol() {
        return new String("(g " + this.time.side + ")");
    }
    
    public Ponto getPontoGolOponente(){
        return time.side == 'l' ? new Ponto(52,0) : new Ponto(-52,0);
    }
    
    public Ponto getPontoMeuGol(){
        return time.side == 'l' ? new Ponto(-52,0) : new Ponto(52,0);
    }
    
    /**
     * Returns true if this Jogador has a brain associated with it.
     * 
     * @return true if this Jogador has a brain associated with it
     */
    public boolean hasBrain() {
        return this.cerebro != null;
    }
    
    /**
     * Returns a string representing this player.
     * 
     * @return a string representing this player
     */
    public final String renderizar() {
        return "Jogador " + String.valueOf(this.numero) + " no time " + this.time.nome;
    }
    
    /**
     * Retorna a velocidade estimada deste Jogador.
     * 
     * @return a velocidade estimada deste Jogador
     */
    public VetorVelocidade velocity() {
        return super.velocity();       
    }
}
