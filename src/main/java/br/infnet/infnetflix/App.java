package br.infnet.infnetflix;

import br.infnet.infnetflix.controller.InfnetflixController;
import br.infnet.infnetflix.repository.*;
import br.infnet.infnetflix.view.ConsoleView;

import java.util.logging.Level;
import java.util.logging.Logger;

/** Ponto de entrada e composição das dependências. */
public final class App {
    private static final Logger LOG = Logger.getLogger(App.class.getName());

    private App() {}

    public static void main(String[] args) {
        String arquivo = args.length > 0 ? args[0] : "infnetflix.db";
        try (Database db = new Database("jdbc:sqlite:" + arquivo)) {
            var controller =
                    new InfnetflixController(
                            new ContaRepository(db),
                            new CatalogoRepository(db),
                            new VisualizacaoRepository(db));
            new ConsoleView(controller).iniciar();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Não foi possível executar o sistema", e);
        }
    }
}
