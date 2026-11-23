package br.infnet.infnetflix;

import br.infnet.infnetflix.controller.InfnetflixController;
import br.infnet.infnetflix.model.*;
import br.infnet.infnetflix.repository.*;
import br.infnet.infnetflix.security.Senhas;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ConformidadeTest {
    @TempDir Path dir;

    private InfnetflixController controller(Database db) {
        return new InfnetflixController(new ContaRepository(db), new CatalogoRepository(db),
                new VisualizacaoRepository(db));
    }

    private InfnetflixController iniciar(Database db) throws Exception {
        InfnetflixController c = controller(db);
        c.cadastrar("Ana", "ana@example.com", "senha123", LocalDate.of(1990, 1, 1));
        assertTrue(c.entrar("ana@example.com", "senha123"));
        c.selecionarPerfil(c.perfis().get(0).id());
        return c;
    }

    private List<Integer> ids(List<Conteudo> conteudos) {
        return conteudos.stream().map(Conteudo::id).toList();
    }

    @Test
    void buscaEncontraAcentosMaiusculasMinusculasETermosSemAcento() throws Exception {
        try (Database db = new Database("jdbc:sqlite::memory:")) {
            InfnetflixController c = iniciar(db);
            for (String titulo : List.of("Última", "última", "ULTIMA", "u\u0301LTIMA", "a última fronteira")) {
                assertEquals(List.of(1),
                        ids(c.buscar(new FiltroCatalogo(titulo, "", "", "", FiltroCatalogo.Ordem.TITULO))), titulo);
            }
            for (String genero : List.of("FICÇÃO CIENTÍFICA", "ficção científica", "Ficcao cientifica")) {
                assertEquals(java.util.Set.of(1, 4),
                        java.util.Set.copyOf(ids(c.buscar(new FiltroCatalogo("", "", genero, "", FiltroCatalogo.Ordem.TITULO)))), genero);
            }
            assertTrue(c.buscar(new FiltroCatalogo("' OR 1=1 --", "", "", "", null)).isEmpty());
            assertEquals(List.of(4), ids(c.buscar(new FiltroCatalogo("", "série", "FICÇÃO", "12", null))));
        }
    }

    @Test
    void edicaoDeFavoritoPreservaDadosEmFalhasEIsolaPerfis() throws Exception {
        try (Database db = new Database("jdbc:sqlite::memory:")) {
            InfnetflixController c = iniciar(db);
            int titular = c.perfis().get(0).id();
            c.adicionarFavorito(1);
            c.adicionarFavorito(2);
            assertThrows(IllegalArgumentException.class, () -> c.editarFavorito(1, 2));
            assertEquals(2, c.favoritos().size());
            assertTrue(c.editarFavorito(1, 4));
            assertTrue(ids(c.favoritos()).containsAll(List.of(2, 4)));
            assertFalse(c.editarFavorito(1, 5));
            c.criarPerfil("Criança", LocalDate.now().minusYears(8));
            int crianca = c.perfis().get(1).id();
            c.selecionarPerfil(crianca);
            assertFalse(c.editarFavorito(4, 5));
            assertTrue(c.adicionarFavorito(3));
            assertThrows(IllegalArgumentException.class, () -> c.editarFavorito(3, 2));
            assertEquals(List.of(3), ids(c.favoritos()));
            c.registrarReproducao(3, 1, false, 150);
            c.selecionarPerfil(titular);
            assertTrue(ids(c.favoritos()).containsAll(List.of(2, 4)));
            c.excluirPerfil(crianca);
            try (Statement s = db.conexao().createStatement();
                    ResultSet r = s.executeQuery("SELECT (SELECT COUNT(*) FROM favoritos), (SELECT COUNT(*) FROM visualizacoes)")) {
                assertTrue(r.next());
                assertEquals(2, r.getInt(1));
                assertEquals(0, r.getInt(2));
            }
        }
    }

    @Test
    void recomendacoesUsamConsumoPositivoDoPerfilEFiltramIdade() throws Exception {
        try (Database db = new Database("jdbc:sqlite::memory:")) {
            InfnetflixController c = iniciar(db);
            List<Integer> semHistorico = ids(c.recomendacoes());
            c.registrarReproducao(1, 0, false, 0);
            assertEquals(semHistorico, ids(c.recomendacoes()));
            c.registrarReproducao(2, 0, false, 100);
            assertEquals(6, c.recomendacoes().get(0).id());
            assertFalse(ids(c.recomendacoes()).contains(2));
            c.criarPerfil("Outro adulto", LocalDate.of(1990, 1, 1));
            c.selecionarPerfil(c.perfis().get(1).id());
            assertEquals(semHistorico, ids(c.recomendacoes()));
            c.criarPerfil("Criança", LocalDate.now().minusYears(8));
            c.selecionarPerfil(c.perfis().get(2).id());
            assertEquals(List.of(3, 5), ids(c.recomendacoes()));
        }
    }

    @Test
    void registroDiretoDeReproducaoValidaPlanoIdadeESessao() throws Exception {
        try (Database db = new Database("jdbc:sqlite::memory:")) {
            InfnetflixController c = iniciar(db);
            assertThrows(IllegalArgumentException.class, () -> c.registrarReproducao(1, 0, false, 1201));
            assertEquals(100, c.registrarReproducao(1, 0, false, 100));
            c.criarPerfil("Criança", LocalDate.now().minusYears(8));
            c.selecionarPerfil(c.perfis().get(1).id());
            assertThrows(IllegalArgumentException.class, () -> c.registrarReproducao(1, 0, false, 100));
            c.sair();
            assertThrows(IllegalStateException.class, () -> c.editarFavorito(1, 2));
            assertThrows(IllegalStateException.class, () -> c.registrarReproducao(1, 0, false, 100));
        }
    }

    @Test
    void reabrirBancoRestauraPlanoFavoritosETiposDoHistorico() throws Exception {
        String url = "jdbc:sqlite:" + dir.resolve("persistencia.db");
        int generoId;
        int temporadaId;
        try (Database db = new Database(url)) {
            InfnetflixController c = iniciar(db);
            c.renomearPerfil(c.perfis().get(0).id(), "Perfil renomeado");
            c.adicionarFavorito(1);
            c.editarFavorito(1, 4);
            c.ativarPremium();
            c.registrarReproducao(1, 0, false, 2500);
            c.registrarReproducao(4, 5, false, 2300);
            Serie serie = (Serie) c.conteudo(4);
            generoId = serie.genero().id();
            temporadaId = serie.temporadas().get(1).id();
        }
        try (Database db = new Database(url)) {
            InfnetflixController c = controller(db);
            assertTrue(c.entrar("ana@example.com", "senha123"));
            assertEquals("Perfil renomeado", c.perfis().get(0).nome());
            c.selecionarPerfil(c.perfis().get(0).id());
            assertTrue(c.premium());
            assertEquals(List.of(4), ids(c.favoritos()));
            assertEquals(2500, c.prepararReproducao(1, 0).progresso());
            assertEquals(2300, c.prepararReproducao(4, 5).progresso());
            assertTrue(c.historico().stream().anyMatch(v -> v.item() instanceof Filme));
            assertTrue(c.historico().stream().anyMatch(v -> v.item() instanceof Episodio));
            assertTrue(c.historico().stream().allMatch(v -> v.id() > 0 && v.dataUltimoAcesso() != null));
            Serie serie = (Serie) c.conteudo(4);
            assertEquals(generoId, serie.genero().id());
            assertEquals(temporadaId, serie.temporadas().get(1).id());
        }
    }

    @Test
    void bancoAnteriorMantemFavoritosEProgressoNaMigracao() throws Exception {
        String url = "jdbc:sqlite:" + dir.resolve("legado.db");
        try (Connection c = DriverManager.getConnection(url); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE contas(id INTEGER PRIMARY KEY, nome TEXT, email TEXT UNIQUE, senha TEXT, plano TEXT)");
            s.execute("CREATE TABLE perfis(id INTEGER PRIMARY KEY, conta_id INTEGER REFERENCES contas(id), nome TEXT, data_nascimento TEXT)");
            s.execute("CREATE TABLE conteudos(id INTEGER PRIMARY KEY, titulo TEXT, tipo TEXT, genero TEXT, classificacao TEXT, sinopse TEXT, duracao INTEGER)");
            s.execute("CREATE TABLE episodios(id INTEGER PRIMARY KEY, conteudo_id INTEGER REFERENCES conteudos(id), temporada INTEGER, numero INTEGER, titulo TEXT, duracao INTEGER)");
            s.execute("CREATE TABLE favoritos(perfil_id INTEGER REFERENCES perfis(id) ON DELETE CASCADE, conteudo_id INTEGER REFERENCES conteudos(id), criado_em TEXT DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(perfil_id,conteudo_id))");
            s.execute("CREATE TABLE visualizacoes(perfil_id INTEGER REFERENCES perfis(id) ON DELETE CASCADE, conteudo_id INTEGER REFERENCES conteudos(id), episodio_id INTEGER, progresso INTEGER, atualizado_em TEXT DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(perfil_id,conteudo_id,episodio_id))");
            try (PreparedStatement p = c.prepareStatement("INSERT INTO contas VALUES(1,'Ana','ana@example.com',?,'PREMIUM')")) {
                p.setString(1, Senhas.criar("senha123"));
                p.executeUpdate();
            }
            s.execute("INSERT INTO perfis VALUES(1,1,'Ana','1990-01-01')");
            s.execute("INSERT INTO conteudos VALUES(50,'Série antiga','SERIE','Drama','Livre','Sinopse',NULL)");
            s.execute("INSERT INTO episodios VALUES(50,50,3,1,'Episódio antigo',4000)");
            s.execute("INSERT INTO favoritos(perfil_id,conteudo_id) VALUES(1,50)");
            s.execute("INSERT INTO visualizacoes(perfil_id,conteudo_id,episodio_id,progresso) VALUES(1,50,50,2100)");
        }
        try (Database db = new Database(url)) {
            InfnetflixController c = controller(db);
            assertTrue(c.entrar("ana@example.com", "senha123"));
            c.selecionarPerfil(1);
            assertTrue(c.premium());
            assertEquals(List.of(50), ids(c.favoritos()));
            assertEquals(2100, c.prepararReproducao(50, 50).progresso());
            Serie serie = (Serie) c.conteudo(50);
            assertEquals(3, serie.temporadas().get(0).numero());
            assertTrue(serie.temporadas().get(0).id() > 0);
            assertTrue(serie.genero().id() > 0);
            assertEquals("Episódio antigo", c.historico().get(0).episodio());
        }
    }
}
