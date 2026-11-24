package br.infnet.infnetflix;

import br.infnet.infnetflix.api.InfnetflixClient;
import br.infnet.infnetflix.api.ServidorRest;
import br.infnet.infnetflix.repository.Database;
import br.infnet.infnetflix.view.ConsoleView;

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Ponto de entrada do servidor REST e do console. */
public final class App {
    private static final Logger LOG = Logger.getLogger(App.class.getName());

    private App() {}

    /** Inicia o console ou, com o argumento servidor, abre a API local. */
    public static void main(String[] args) {
        try {
            if (args.length > 0 && args[0].equals("servidor")) {
                String arquivo = args.length > 1 ? args[1] : "infnetflix.db";
                executarServidor(arquivo, porta(args, 2));
            } else {
                InfnetflixClient api = new InfnetflixClient(porta(args, 0));
                api.verificarConexao();
                new ConsoleView(api).iniciar();
                api.sair();
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.err.println(e.getMessage());
        } catch (IOException | SQLException e) {
            LOG.log(Level.SEVERE, "Não foi possível iniciar o servidor REST", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static int porta(String[] args, int posicao) {
        if (args.length <= posicao) {
            return ServidorRest.PORTA_PADRAO;
        }
        try {
            int porta = Integer.parseInt(args[posicao]);
            if (porta > 0 && porta <= 65535) {
                return porta;
            }
        } catch (NumberFormatException e) {
            // A mensagem abaixo também cobre argumentos que não são números.
        }
        throw new IllegalArgumentException("Informe uma porta entre 1 e 65535.");
    }

    private static void executarServidor(String arquivo, int porta)
            throws IOException, SQLException, InterruptedException {
        CountDownLatch parar = new CountDownLatch(1);
        CountDownLatch concluido = new CountDownLatch(1);
        Thread desligamento = new Thread(() -> {
            parar.countDown();
            try {
                concluido.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "encerrar-api");
        try (Database db = new Database("jdbc:sqlite:" + arquivo);
                ServidorRest servidor = new ServidorRest(db, porta)) {
            Runtime.getRuntime().addShutdownHook(desligamento);
            servidor.iniciar();
            System.out.printf("INFNETFLIX API em http://%s:%d%n", ServidorRest.HOST_LOCAL, servidor.porta());
            parar.await();
        } finally {
            concluido.countDown();
        }
    }
}
