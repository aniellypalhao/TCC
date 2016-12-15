import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Classe responsável por manuzear a conexão UDP com o RoboCup 2D soccer server.
 */
public class Cliente {

   
    public Jogador player;
    public InetAddress soccerServerHost;
    public int soccerServerPort = Configuracoes.INIT_PORT;
    public DatagramSocket soccerServerSocket;

    /**
     * Configura um cliente para o jogo.
     *
     * @param args os mesmos argumentos passados para o Main.
     */
    public Cliente(String[] args) {
        this.player = new Jogador(this);  // Inicializa um jogador a este cliente.
        //Processa os argumentos da linha de comando
        for (int i = 0; i < args.length; i++) {
            try {
                if (args[i].equals("-g")) {          
                    this.player.goleiro = true;
                }

                if (args[i].equals("-t") || args[i].equals("--team")) {
                    this.player.time.nome = args[i + 1];
                }
            } catch (Exception e) {
                Log.e("Invalid command-line parameters.");
            }
        }
    }

    /**
     * Inicializa um cliente. Conecta no servidor.
     */
    public void init() {
        try {
            //Configuração de conexão.
            soccerServerHost = InetAddress.getByName(Configuracoes.HOSTNAME);
            soccerServerSocket = new DatagramSocket();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        /**
         * Conecta os jogadores no simulador, enviando a mensagem de INIT 
         * O comando INIT comunica o servidor para iniciar um jogador e gerar suas informações básicas(numero,lado do campo)
         */
        String version = String.format("(version %s)", Configuracoes.SOCCER_SERVER_VERSION);
        if (!this.player.goleiro) {
            enviaComando(Configuracoes.Commands.INIT, player.time.nome, version);
            player.goleiro = false;
        } else {
            enviaComando(Configuracoes.Commands.INIT, player.time.nome, version, "(goalie)");
            player.goleiro = true;
        }
        // Começa a leitura do INIT no servidor 
    }

    /**
     * Coloca o jogador num loop infinito.
     */
    public final void jogar() {
        while (true) {
            player.cerebro.parseMessage(recebeMensagem());
            
        }        
    }

    /**
     * Disconecta o cliente do servidor.
     */
    public final void fecharConexao() {
        enviaComando(Configuracoes.Commands.BYE);
        soccerServerSocket.close();
    }

    /**
     * Escuta mensagem do servidor.
     *
     * @return mensagem que o servidor nos enviou.
     */
    public String recebeMensagem() {
        byte[] buffer = new byte[Configuracoes.MSG_SIZE];
        DatagramPacket pacote = new DatagramPacket(buffer, Configuracoes.MSG_SIZE);
        try {
            soccerServerSocket.receive(pacote);
            if (soccerServerPort == Configuracoes.INIT_PORT) {
                soccerServerPort = pacote.getPort();
            }
        } catch (IOException e) {
            System.err.println("erro de socket " + e);
        }       
        return new String(buffer);
    }

    /**
     * Envia uma mensagem formatada para o servidor.
     *
     * @param comando o comando para enviar.
     * @param args uma amotoado de argumentoss
     */
    public final void enviaComando(String comando, Object... args) {
        String parcial = String.format("(%s", comando);
        for (Object arg : args) {
            parcial += ' ' + arg.toString();
        }
        parcial += ")\0";
        this.enviarMensagem(parcial);
    }

    /**
     * Envia mensagem para o servidor.
     *
     * @param mensagem para ser enviada
     */
    private void enviarMensagem(String mensagem) {
        byte[] buffer = mensagem.getBytes();
        DatagramPacket pacote = new DatagramPacket(buffer, buffer.length, soccerServerHost, soccerServerPort);
        try {
            soccerServerSocket.send(pacote);
        } catch (IOException e) {
            System.err.println("Erro no servidor: " + e);
        }
    }
}
