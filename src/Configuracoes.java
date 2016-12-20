import java.util.Arrays;
import java.util.HashSet;

/**
 * Classe que armazena todas as informações e parâmetros conhecidos sobre o simulador Robocup 2D.
 */

public class Configuracoes {
   
    public static final String HOSTNAME = "localhost";
    public static final int INIT_PORT = 6000;
    public static final String SOCCER_SERVER_VERSION = "15.0";
    public static final int MSG_SIZE = 4096;
    public static final double MARCACAO = 10;
    public static final String NOME_TIME = "TCC";
    public static final String NOME_OUTRO_TIME = "TCC2";
    public static final String ID_BOLA = "(b)";
    
    public static double ALTURA_GOL                             = 14.02;
    public static final double ALTURA_PEQUENA_AREA              = 16.0;
    public static final double COMPRIMENTO_PEQUENA_AREA         = 6.5;
    public static final double LARGURA_CAMPO                    = 105.0;
    public static final double ALTURA_CAMPO                     = 68.0;
    public static final double BUFFER_CAMPO                     = 5.0; //variavel que indica o tamanho do espaço entre as linhas da lateral do campo e as flags externas
    public static final double LARGURA_AREA_PENALTI             = 16.5; // 97.4% confirmed in robocup; based on size of actual field
    public static final double ALTURA_AREA_PENALTI              = 40.3; // 97.4% confirmed in robocup; based on size of actual field
    public static final double CONFIANCA_BOLA                   = 0.87;
    public static final double PENALTY_X                        = Configuracoes.LARGURA_CAMPO/2 - Configuracoes.LARGURA_AREA_PENALTI;//onde começa a area do penalti
    public static final double MAX_Y_PORCENTAGEM                = 0.8;
    public static final double VARIACAO_DE_INERCIA              = 5.0;
    public static final double ANGULO_PARA_USAR_TURN            = 7.0;
    public static final double VELOCIDADE_FINAL_PASSE           = 1.2;
    public static final double VELOCIDADE_FINAL_PASSE_RAPIDO    = 1.8;
    public static final double ANGULO_PARA_VIRAR_PESCOCO        = 7;
    // Other constants
    public static final char LEFT_SIDE  = 'l';
    public static final char RIGHT_SIDE = 'r';
    
    // Parametros do servidor    
    public static ConfiguracoesBola   BOLA_PARAMS           = new ConfiguracoesBola();
    public static ConfiguracoesJogador JOGADOR_PARAMS         = new ConfiguracoesJogador();
    public static double              DASH_POWER_RATE       = 0.006;
    public static final double        EFFORT_DEC            = 0.05;
    public static final double        PLAYER_ACCEL_MAX      = 1.0;
    public static final double        PLAYER_SPEED_MAX      = 1.0;
    public static final double        TEAM_FAR_LENGTH       = 40.0;
    public static final double        TEAM_TOO_FAR_LENGTH   = 60.0;
    
    // Inferencia
    public static final double DISTANCE_ESTIMATE = 0.333333 * TEAM_FAR_LENGTH + 0.666666 * TEAM_TOO_FAR_LENGTH;

    // Cordenadas
    public static final Ponto CENTRO_CAMPO = new Ponto(0, 0);
    
    /**
     * Constante strings representando os comandos que um cliente pode enviar
     * para o servidor.
     */
    public class Commands {
        public static final String BYE = "bye";
        public static final String DASH = "dash";
        public static final String INIT = "init";
        public static final String KICK = "kick";
        public static final String TURN = "turn";
        public static final String TURN_NECK = "turn_neck";
        public static final String MOVE = "move";
        public static final String CATCH = "catch";
    }
    
    public static enum RESPONSE {
        SEE,
        SENSE_BODY,
        NONE
    }
    
    /**
     * Obtém a taxa de potência do dash.
     * 
     * @return Obtém a taxa de potência do dash.
     */
    public static double getDashPowerRate() {
        return DASH_POWER_RATE;
    }
    
    /**
     * Define a taxa de potência do dash.
     * 
     * @param rate a taxa de potência do dash.
     */
    public static void setDashPowerRate(double rate) {
        DASH_POWER_RATE = rate;
    }
    

	/**
	 * Obtém a altura do gol.
	 * 
	 * @return a altura do gol.
	 */
	public static double getAlturaGol() {
		return ALTURA_GOL;
	}
	
	/**
	 * Define a altura do gol.
	 * 
	 * @param altura altura do gol.
	 */
	public static void setAlturaGol(double altura) {
		ALTURA_GOL = altura;
	}

	/**
	 * 
         * Recria todos os dados do parâmetro do servidor de acordo com as configurações 
         * do Construtor de cada objeto.
	 */
    public static void reconstruirParametros()
    {
    	BOLA_PARAMS = new ConfiguracoesBola();
    	JOGADOR_PARAMS = new ConfiguracoesJogador();
    }
	
	// area campo jogavel
    public static Retangulo CAMPO = new Retangulo(-ALTURA_CAMPO / 2.0, LARGURA_CAMPO / 2.0, ALTURA_CAMPO / 2.0, -LARGURA_CAMPO / 2.0);
    public static Retangulo CAMPO_L = new Retangulo(-ALTURA_CAMPO / 2.0, 0, ALTURA_CAMPO / 2.0, -LARGURA_CAMPO / 2.0);
    public static Retangulo CAMPO_R = new Retangulo(-ALTURA_CAMPO / 2.0, LARGURA_CAMPO / 2.0, ALTURA_CAMPO / 2.0, 0);
    
    
    public static Retangulo PEQUENA_AREA_L = new Retangulo(-ALTURA_PEQUENA_AREA / 2.0, -(LARGURA_CAMPO / 2.0) + COMPRIMENTO_PEQUENA_AREA, ALTURA_PEQUENA_AREA / 2.0 ,-(LARGURA_CAMPO / 2.0) + 1);
    public static Retangulo PEQUENA_AREA_R = new Retangulo(-ALTURA_PEQUENA_AREA / 2.0, (LARGURA_CAMPO / 2.0) - 1 , ALTURA_PEQUENA_AREA / 2.0 ,(LARGURA_CAMPO / 2.0) - COMPRIMENTO_PEQUENA_AREA);
    // Limite físico absoluto do espaço de jogo
    public static Retangulo BORDA_EXTERNA = new Retangulo(CAMPO.getSuperior() - BUFFER_CAMPO, CAMPO.getDireita() + BUFFER_CAMPO, CAMPO.getInferior() + BUFFER_CAMPO, CAMPO.getEsquerda() - BUFFER_CAMPO);
    
    // area de penalti
    public static Retangulo AREA_PENALTI_ESQUERDA = new Retangulo(-ALTURA_AREA_PENALTI / 2.0, CAMPO.getEsquerda() + LARGURA_AREA_PENALTI, ALTURA_AREA_PENALTI / 2.0, CAMPO.getEsquerda());
    public static Retangulo AREA_PENALTI_DIREITA = new Retangulo(-ALTURA_AREA_PENALTI / 2.0, CAMPO.getDireita(), ALTURA_AREA_PENALTI / 2.0, CAMPO.getDireita() - LARGURA_AREA_PENALTI);
    
 
    
    // Lista de todos os conhecidos play_modes do jogo
    public static final HashSet<String> PLAY_MODES = new HashSet<String>(Arrays.asList(
    		"before_kick_off",
    		"play_on",
    		"time_over",
    		"kick_off_l",
    		"kick_off_r",
    		"kick_in_l",
    		"kick_in_r",
    		"free_kick_l",
    		"free_kick_r",
    		"corner_kick_l",
    		"corner_kick_r",
    		"goal_l_",
    		"goal_r_",
    		"goal_kick_l",
    		"goal_kick_r",
    		"drop_ball",
    		"offside_l",
    		"offside_r"
    ));
    


    
    // Flags das bordas externas
    public static final String[][] GRUPO_FLAGS_EXTERNAS = {
            // top flags externas
            {
                "(f t l 50)",
                "(f t l 40)",
                "(f t l 30)",
                "(f t l 20)",
                "(f t l 10)",
                "(f t 0)",
                "(f t r 10)",
                "(f t r 20)",
                "(f t r 30)",
                "(f t r 40)",
                "(f t r 50)"
            },
            // flags da borda esquerda
            {
                "(f r t 30)",
                "(f r t 20)",
                "(f r t 10)",
                "(f r 0)",
                "(f r b 10)",
                "(f r b 20)",
                "(f r b 30)"
            },
            // flags da borda inferior
            {
                "(f b l 50)",
                "(f b l 40)",
                "(f b l 30)",
                "(f b l 20)",
                "(f b l 10)",
                "(f b 0)",
                "(f b r 10)",
                "(f b r 20)",
                "(f b r 30)",
                "(f b r 40)",
                "(f b r 50)"
            },
            // flags da borda esquerda
            {
                "(f l t 30)",
                "(f l t 20)",
                "(f l t 10)",
                "(f l 0)",
                "(f l b 10)",
                "(f l b 20)",
                "(f l b 30)"
            },
            
    };
    
    /**
     * List of known stationary objects.
     * Although they could theoretically be parsed on the fly, we think it's
     * probably more efficient to parse and store them in advance. They are
     * stationary, after all.
     */
    public static final ObjetoFixo[] OBJETOS_FIXOS = {
        // Physical boundary flags
        new ObjetoFixo("(f t l 50)", -50.0, BORDA_EXTERNA.getSuperior()),
        new ObjetoFixo("(f t l 40)", -40.0, BORDA_EXTERNA.getSuperior()),
        new ObjetoFixo("(f t l 30)", -30.0, BORDA_EXTERNA.getSuperior()),
        new ObjetoFixo("(f t l 20)", -20.0, BORDA_EXTERNA.getSuperior()),
        new ObjetoFixo("(f t l 10)", -10.0, BORDA_EXTERNA.getSuperior()),
        new ObjetoFixo("(f t 0)", 0.0, BORDA_EXTERNA.getSuperior()),
        new ObjetoFixo("(f t r 10)", 10.0, BORDA_EXTERNA.getSuperior()),
        new ObjetoFixo("(f t r 20)", 20.0, BORDA_EXTERNA.getSuperior()),
        new ObjetoFixo("(f t r 30)", 30.0, BORDA_EXTERNA.getSuperior()),
        new ObjetoFixo("(f t r 40)", 40.0, BORDA_EXTERNA.getSuperior()),
        new ObjetoFixo("(f t r 50)", 50.0, BORDA_EXTERNA.getSuperior()),
        new ObjetoFixo("(f r t 30)", BORDA_EXTERNA.getDireita(), -30.0),
        new ObjetoFixo("(f r t 20)", BORDA_EXTERNA.getDireita(), -20.0),
        new ObjetoFixo("(f r t 10)", BORDA_EXTERNA.getDireita(), -10.0),
        new ObjetoFixo("(f r 0)", BORDA_EXTERNA.getDireita(), 0.0),
        new ObjetoFixo("(f r b 10)", BORDA_EXTERNA.getDireita(), 10.0),
        new ObjetoFixo("(f r b 20)", BORDA_EXTERNA.getDireita(), 20.0),
        new ObjetoFixo("(f r b 30)", BORDA_EXTERNA.getDireita(), 30.0),
        new ObjetoFixo("(f b r 50)", 50.0, BORDA_EXTERNA.getInferior()),
        new ObjetoFixo("(f b r 40)", 40.0, BORDA_EXTERNA.getInferior()),
        new ObjetoFixo("(f b r 30)", 30.0, BORDA_EXTERNA.getInferior()),
        new ObjetoFixo("(f b r 20)", 20.0, BORDA_EXTERNA.getInferior()),
        new ObjetoFixo("(f b r 10)", 10.0, BORDA_EXTERNA.getInferior()),
        new ObjetoFixo("(f b 0)", 0.0, BORDA_EXTERNA.getInferior()),
        new ObjetoFixo("(f b l 10)", -10.0, BORDA_EXTERNA.getInferior()),
        new ObjetoFixo("(f b l 20)", -20.0, BORDA_EXTERNA.getInferior()),
        new ObjetoFixo("(f b l 30)", -30.0, BORDA_EXTERNA.getInferior()),
        new ObjetoFixo("(f b l 40)", -40.0, BORDA_EXTERNA.getInferior()),
        new ObjetoFixo("(f b l 50)", -50.0, BORDA_EXTERNA.getInferior()),
        new ObjetoFixo("(f l b 30)", BORDA_EXTERNA.getEsquerda(), 30.0),
        new ObjetoFixo("(f l b 20)", BORDA_EXTERNA.getEsquerda(), 20.0),
        new ObjetoFixo("(f l b 10)", BORDA_EXTERNA.getEsquerda(), 10.0),
        new ObjetoFixo("(f l 0)", BORDA_EXTERNA.getEsquerda(), 0.0),
        new ObjetoFixo("(f l t 10)", BORDA_EXTERNA.getEsquerda(), 10.0),
        new ObjetoFixo("(f l t 20)", BORDA_EXTERNA.getEsquerda(), 20.0),
        new ObjetoFixo("(f l t 30)", BORDA_EXTERNA.getEsquerda(), 30.0),
        
        // Field corner flags
        new ObjetoFixo("(f l t)", CAMPO.getEsquerda(), CAMPO.getSuperior()),
        new ObjetoFixo("(f r t)", CAMPO.getDireita(), CAMPO.getSuperior()),
        new ObjetoFixo("(f r b)", CAMPO.getDireita(), CAMPO.getInferior()),
        new ObjetoFixo("(f l b)", CAMPO.getEsquerda(), CAMPO.getInferior()),
        
        // Field center flags
        new ObjetoFixo("(f c t)", 0.0, CAMPO.getSuperior()),
        new ObjetoFixo("(f c)", 0.0, 0.0),
        new ObjetoFixo("(f c b)", 0.0, CAMPO.getInferior()),
        
        // Penalty area flags
        new ObjetoFixo("(f p l t)", AREA_PENALTI_ESQUERDA.getDireita(), AREA_PENALTI_ESQUERDA.getSuperior()),
        new ObjetoFixo("(f p l c)", AREA_PENALTI_ESQUERDA.getDireita(), 0.0),
        new ObjetoFixo("(f p l b)", AREA_PENALTI_ESQUERDA.getDireita(), AREA_PENALTI_ESQUERDA.getInferior()),
        new ObjetoFixo("(f p r t)", AREA_PENALTI_DIREITA.getEsquerda(), AREA_PENALTI_DIREITA.getSuperior()),
        new ObjetoFixo("(f p r c)", AREA_PENALTI_DIREITA.getEsquerda(), 0.0),
        new ObjetoFixo("(f p r b)", AREA_PENALTI_DIREITA.getEsquerda(), AREA_PENALTI_DIREITA.getInferior()),
        
        // Goalpost flags
        new ObjetoFixo("(f g l t)", CAMPO.getEsquerda(), ALTURA_GOL / 2),
        new ObjetoFixo("(f g l b)", CAMPO.getEsquerda(), -ALTURA_GOL / 2),
        new ObjetoFixo("(f g r t)", CAMPO.getDireita(), ALTURA_GOL / 2),
        new ObjetoFixo("(f g r b)", CAMPO.getDireita(), -ALTURA_GOL / 2),
        
        // Goals
        new ObjetoFixo("(g l)", CAMPO.getEsquerda(), 0.0),
        new ObjetoFixo("(g r)", CAMPO.getDireita(), 0.0)
    };
}
