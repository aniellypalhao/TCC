
public class Main {

    /**
     * Esta função principal é a primeira função a ser executada. Ela lê os
     * argumentos da linha de comando e realiza a conexão dos clientes no
     * servidor. Modo de uso: GOLEIRO = -g TIME = -t "nomeTime"
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        //initClient(args);
        Cliente client = new Cliente(args);
        client.init();
        client.jogar();
    }

}
