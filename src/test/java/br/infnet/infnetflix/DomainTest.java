package br.infnet.infnetflix;

import br.infnet.infnetflix.model.*;
import br.infnet.infnetflix.repository.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DomainTest {
    private Perfil adulto() { return new Perfil(1, 1, "Ana", LocalDate.of(1990, 1, 1)); }

    private Conta conta(Perfil perfil) {
        return new Conta(1, "Ana", "ana@example.com", "hash", List.of(perfil),
                new Plano(1, TipoPlano.GRATUITO));
    }

    @Test
    void contaMantemRegrasDePerfisECompartilhaPlano() {
        Perfil perfil = adulto();
        Conta conta = conta(perfil);
        assertThrows(IllegalArgumentException.class, () -> conta.excluirPerfil(1));
        assertThrows(IllegalArgumentException.class, () -> conta.selecionarPerfil(99));
        assertThrows(IllegalArgumentException.class,
                () -> conta.criarPerfil("Futuro", LocalDate.now().plusDays(1)));
        conta.editarPerfil(1, "Novo nome");
        assertEquals("Novo nome", conta.selecionarPerfil(1).nome());
        Perfil novo = conta.criarPerfil("Segundo", LocalDate.of(2000, 1, 1));
        conta.criarPerfil("Terceiro", LocalDate.of(2000, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> conta.criarPerfil("Quarto", LocalDate.of(2000, 1, 1)));
        assertThrows(UnsupportedOperationException.class, () -> conta.perfis().clear());
        conta.assinarPremium();
        assertSame(conta.plano(), novo.plano());
        assertFalse(perfil.plano().exibeAnuncios());
        assertEquals("HD", conta.plano().qualidadeConteudo());
    }

    @Test
    void filmeRetomaAoPausarELimiteGratuitoAcumulaAvancos() {
        Perfil perfil = adulto();
        Conta conta = conta(perfil);
        Filme filme = new Filme(1, "Filme", new Genero(1, "Drama"), "Livre", "Sinopse", 6000);
        assertThrows(IllegalStateException.class, filme::pausar);
        Reproducao r = filme.reproduzir(perfil);
        r.avancar(700);
        r.avancar(500);
        assertThrows(IllegalArgumentException.class, () -> r.avancar(1));
        filme.pausar();
        assertEquals(1200, perfil.obterVisualizacao(filme).progresso());
        assertThrows(IllegalStateException.class, () -> r.avancar(1));

        Reproducao retomada = filme.reproduzir(perfil);
        assertEquals(1200, retomada.progresso());
        retomada.avancar(1200);
        retomada.reiniciar();
        assertThrows(IllegalArgumentException.class, () -> retomada.avancar(1));
        filme.pausar();
        assertEquals(0, perfil.obterVisualizacao(filme).progresso());

        conta.assinarPremium();
        Reproducao premium = filme.reproduzir(perfil);
        premium.avancar(6000);
        filme.pausar();
        assertEquals(6000, perfil.obterVisualizacao(filme).progresso());
        assertThrows(IllegalArgumentException.class,
                () -> perfil.obterVisualizacao(filme).atualizarProgresso(6001));
    }

    @Test
    void repositorioMontaHerancaComposicaoEHistoricoPorEpisodio() throws Exception {
        try (Database db = new Database("jdbc:sqlite::memory:")) {
            CatalogoRepository repository = new CatalogoRepository(db);
            assertInstanceOf(Filme.class, repository.buscarPorId(1).orElseThrow());
            Serie serie = assertInstanceOf(Serie.class, repository.buscarPorId(4).orElseThrow());
            assertEquals(List.of(1, 2), serie.temporadas().stream().map(Temporada::numero).toList());
            assertTrue(serie.genero().id() > 0);
            assertTrue(serie.temporadas().stream().allMatch(t -> t.id() > 0));
            assertEquals(List.of(3, 4, 5), serie.listarEpisodios().stream().map(Episodio::id).toList());
            Perfil perfil = adulto();
            Reproduzivel primeiro = serie.obterItem(3);
            Reproduzivel segundo = serie.obterItem(4);
            assertSame(serie, primeiro.conteudo());
            primeiro.reproduzir(perfil).avancar(100);
            primeiro.pausar();
            assertEquals(0, segundo.reproduzir(perfil).progresso());
            segundo.pausar();
            assertEquals(100, primeiro.reproduzir(perfil).progresso());
            assertEquals("Partida", perfil.obterVisualizacao(primeiro).episodio());
            assertEquals(2, perfil.listarHistorico().size());
            assertThrows(IllegalArgumentException.class, () -> serie.obterItem(1));
            Perfil crianca = new Perfil(2, 1, "Criança", LocalDate.now().minusYears(8));
            assertThrows(IllegalArgumentException.class, () -> primeiro.reproduzir(crianca));
        }
    }

    @Test
    void catalogoOrdenaPortuguesEBuscaUnicodeCombinado() {
        Genero genero = new Genero(1, "Ficção científica");
        Filme arvore = new Filme(1, "Árvore", genero, "Livre", "Sinopse", 3000);
        Filme zebra = new Filme(2, "Zebra", genero, "18 anos", "Sinopse", 3000);
        Catalogo catalogo = new Catalogo(List.of(zebra, arvore));
        Perfil perfil = adulto();
        assertEquals(List.of(arvore, zebra), catalogo.ordenar("título", perfil));
        assertEquals(List.of(arvore), catalogo.buscar("a\u0301RVORE", perfil));
        assertEquals(List.of(arvore, zebra), catalogo.filtrar(new Genero(0, "FICCAO CIENTIFICA"), "", perfil));
        Perfil crianca = new Perfil(2, 1, "Criança", LocalDate.now().minusYears(8));
        assertEquals(List.of(arvore), catalogo.listar(crianca));
        assertThrows(IllegalArgumentException.class, () -> catalogo.obterDetalhes(2, crianca));
    }
}
