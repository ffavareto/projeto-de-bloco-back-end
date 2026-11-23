package br.infnet.infnetflix;

import br.infnet.infnetflix.controller.InfnetflixController;
import br.infnet.infnetflix.repository.CatalogoRepository;
import br.infnet.infnetflix.repository.ContaRepository;
import br.infnet.infnetflix.repository.Database;
import br.infnet.infnetflix.repository.VisualizacaoRepository;
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
            ContaRepository contas = new ContaRepository(db);
            CatalogoRepository catalogo = new CatalogoRepository(db);
            VisualizacaoRepository visualizacoes = new VisualizacaoRepository(db);
            InfnetflixController controller = new InfnetflixController(contas, catalogo, visualizacoes);
            ConsoleView console = new ConsoleView(controller);
            console.iniciar();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Não foi possível executar o sistema", e);
        }
    }
}
