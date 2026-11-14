package br.infnet.infnetflix;

import static org.junit.jupiter.api.Assertions.*;

import br.infnet.infnetflix.controller.InfnetflixController;
import br.infnet.infnetflix.model.*;
import br.infnet.infnetflix.repository.*;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

class ControllerTest {
    private InfnetflixController controller(Database db) {
        return new InfnetflixController(
                new ContaRepository(db),
                new CatalogoRepository(db),
                new VisualizacaoRepository(db));
    }

    @Test
    void regrasDePerfisProtegemDadosEntreContas() throws Exception {
        try (Database db = new Database("jdbc:sqlite::memory:")) {
            InfnetflixController c = controller(db);
            assertThrows(IllegalStateException.class, c::perfis);
            c.cadastrar("Ana", "ana@example.com", "senha123", LocalDate.of(1990, 1, 1));
            c.cadastrar("Bia", "bia@example.com", "senha123", LocalDate.of(1990, 1, 1));
            assertFalse(c.entrar("ana@example.com", "incorreta"));
            assertTrue(c.entrar("ana@example.com", "senha123"));
            int perfilAna = c.perfis().get(0).id();
            assertThrows(IllegalArgumentException.class, () -> c.excluirPerfil(perfilAna));
            c.criarPerfil("Outro", LocalDate.of(1990, 1, 1));
            c.selecionarPerfil(perfilAna);
            c.adicionarFavorito(1);
            c.sair();
            assertTrue(c.entrar("bia@example.com", "senha123"));
            assertThrows(IllegalArgumentException.class, () -> c.selecionarPerfil(perfilAna));
            c.selecionarPerfil(c.perfis().get(0).id());
            assertTrue(c.favoritos().isEmpty());
        }
    }

    @Test
    void reproduzEpisodioRetomaELimitaPlanoGratuito() throws Exception {
        try (Database db = new Database("jdbc:sqlite::memory:")) {
            InfnetflixController c = controller(db);
            c.cadastrar("Ana", "ana@example.com", "senha123", LocalDate.of(1990, 1, 1));
            c.entrar("ana@example.com", "senha123");
            c.selecionarPerfil(c.perfis().get(0).id());
            assertThrows(IllegalArgumentException.class, () -> c.prepararReproducao(3, 3));
            Reproducao r = c.prepararReproducao(3, 1);
            assertThrows(
                    IllegalArgumentException.class, () -> c.registrarReproducao(r, false, 1201));
            assertEquals(600, c.registrarReproducao(r, false, 600));
            assertEquals(600, c.prepararReproducao(3, 1).progresso());
            assertEquals("Origens", c.historico().get(0).episodio());
            assertEquals(0, c.prepararReproducao(3, 2).progresso());
            assertTrue(c.recomendacoes().stream().noneMatch(item -> item.id() == 3));
            c.ativarPremium();
            assertEquals(1800, c.registrarReproducao(c.prepararReproducao(3, 1), true, 1800));
        }
    }

    @Test
    void buscaComFiltrosEOrdenacao() throws Exception {
        try (Database db = new Database("jdbc:sqlite::memory:")) {
            InfnetflixController c = controller(db);
            c.cadastrar("Ana", "ana@example.com", "senha123", LocalDate.of(1990, 1, 1));
            c.entrar("ana@example.com", "senha123");
            c.selecionarPerfil(c.perfis().get(0).id());

            FiltroCatalogo filtro =
                    new FiltroCatalogo("", "FILME", "Suspense", "", FiltroCatalogo.Ordem.TITULO);
            var resultados = c.buscar(filtro);
            assertEquals(2, resultados.size());
            assertEquals("Arquivo Oculto", resultados.get(0).titulo());
            assertEquals("O Mistério do Farol", resultados.get(1).titulo());
        }
    }
    @Test
    void classificacaoSegueIdadeDoPerfilEmTodosOsAcessos() throws Exception {
        try (Database db = new Database("jdbc:sqlite::memory:")) {
            InfnetflixController c = controller(db);
            assertThrows(IllegalArgumentException.class, () -> c.cadastrar("Ana", "ana@example.com", "senha123", LocalDate.now().plusDays(1)));
            c.cadastrar("Ana", "ana@example.com", "senha123", LocalDate.now().minusYears(12).plusDays(1));
            c.entrar("ana@example.com", "senha123");
            c.selecionarPerfil(c.perfis().get(0).id());
            try (var s = db.conexao().createStatement()) {
                for (int i = 0; i < 12; i++) {
                    s.execute("INSERT INTO conteudos VALUES(" + (100 + i) + ", 'A restrito " + i
                            + "', 'FILME', 'Teste', '18 anos', 'Teste', 100)");
                }
                s.execute("INSERT INTO conteudos VALUES(200, 'Classificação desconhecida', 'FILME', 'Teste', 'indefinida', 'Teste', 100)");
            }
            FiltroCatalogo todos = new FiltroCatalogo("", "", "", "", FiltroCatalogo.Ordem.TITULO);
            assertEquals(java.util.Set.of(3, 5), c.buscar(todos).stream().map(Conteudo::id).collect(java.util.stream.Collectors.toSet()));
            assertEquals(java.util.Set.of(3, 5), c.recomendacoes().stream().map(Conteudo::id).collect(java.util.stream.Collectors.toSet()));
            assertThrows(IllegalArgumentException.class, () -> c.conteudo(1));
            assertThrows(IllegalArgumentException.class, () -> c.episodios(4));
            assertThrows(IllegalArgumentException.class, () -> c.prepararReproducao(1, 0));
            c.criarPerfil("Doze", LocalDate.now().minusYears(12));
            c.selecionarPerfil(c.perfis().get(1).id());
            assertTrue(c.buscar(todos).stream().anyMatch(item -> item.id() == 1));
            assertTrue(c.recomendacoes().stream().anyMatch(item -> item.id() == 1));
            assertFalse(c.buscar(todos).stream().anyMatch(item -> item.id() == 2));
        }
    }
}
