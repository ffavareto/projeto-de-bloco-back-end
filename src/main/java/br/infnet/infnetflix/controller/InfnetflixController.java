package br.infnet.infnetflix.controller;

import br.infnet.infnetflix.model.*;
import br.infnet.infnetflix.repository.*;
import br.infnet.infnetflix.service.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

/** Coordena a sessão, os objetos de domínio e a persistência do console. */
public final class InfnetflixController {
    private final ContaRepository contas;
    private final CatalogoRepository catalogo;
    private final VisualizacaoRepository visualizacoes;
    private final ServicoAutenticacao autenticacao;
    private final ServicoRecomendacao recomendacao = new ServicoRecomendacao();
    private int contaId;
    private int perfilId;

    public InfnetflixController(ContaRepository contas, CatalogoRepository catalogo,
            VisualizacaoRepository visualizacoes) {
        this.contas = contas;
        this.catalogo = catalogo;
        this.visualizacoes = visualizacoes;
        this.autenticacao = new ServicoAutenticacao(contas);
    }

    public boolean autenticado() { return contaId != 0; }
    public boolean perfilSelecionado() { return perfilId != 0; }

    private Conta contaAtual() throws SQLException {
        if (!autenticado()) throw new IllegalStateException("Entre na conta primeiro.");
        return contas.buscarPorId(contaId).orElseThrow(() -> new IllegalStateException("Conta não encontrada."));
    }

    private Perfil perfilAtual() throws SQLException {
        Conta conta = contaAtual();
        if (!perfilSelecionado()) throw new IllegalStateException("Selecione um perfil primeiro.");
        return conta.selecionarPerfil(perfilId);
    }

    private Perfil perfilComDados() throws SQLException {
        Perfil perfil = perfilAtual();
        perfil.restaurarDados(catalogo.favoritos(perfilId), visualizacoes.historico(perfilId));
        return perfil;
    }

    public void cadastrar(String nome, String email, String senha, LocalDate dataNascimento) throws SQLException {
        autenticacao.cadastrar(nome, email, senha, dataNascimento);
    }

    public boolean entrar(String email, String senha) throws SQLException {
        sair();
        Optional<Conta> conta = autenticacao.login(email, senha);
        if (conta.isEmpty()) return false;
        contaId = conta.get().id();
        return true;
    }

    public void sair() { contaId = 0; perfilId = 0; }
    public void trocarPerfil() { perfilId = 0; }
    public List<Perfil> perfis() throws SQLException { return contaAtual().perfis(); }

    public void selecionarPerfil(int id) throws SQLException {
        contaAtual().selecionarPerfil(id);
        perfilId = id;
    }

    public void verificarDisponibilidadePerfil() throws SQLException {
        contaAtual().verificarDisponibilidadePerfil();
    }

    public void criarPerfil(String nome, LocalDate dataNascimento) throws SQLException {
        Perfil novo = contaAtual().criarPerfil(nome, dataNascimento);
        contas.criarPerfil(contaId, novo.nome(), novo.dataNascimento());
    }

    public void renomearPerfil(int id, String nome) throws SQLException {
        Conta conta = contaAtual();
        conta.editarPerfil(id, nome);
        if (contas.renomearPerfil(contaId, id, conta.obterPerfil(id).nome()) == 0) {
            throw new IllegalArgumentException("Perfil não encontrado.");
        }
    }

    public void validarExclusaoPerfil(int id) throws SQLException { contaAtual().validarExclusaoPerfil(id); }

    public void excluirPerfil(int id) throws SQLException {
        contaAtual().excluirPerfil(id);
        if (contas.excluirPerfil(contaId, id) == 0) throw new IllegalArgumentException("Perfil não encontrado.");
        if (perfilId == id) perfilId = 0;
    }

    public boolean premium() throws SQLException { return !contaAtual().plano().exibeAnuncios(); }

    public void ativarPremium() throws SQLException {
        Conta conta = contaAtual();
        conta.assinarPremium();
        contas.salvarPlano(conta);
    }

    public List<Conteudo> buscar(FiltroCatalogo filtro) throws SQLException {
        Perfil perfil = perfilAtual();
        return catalogo.carregar().buscar(filtro, perfil);
    }

    public Conteudo conteudo(int id) throws SQLException {
        Perfil perfil = perfilAtual();
        return catalogo.carregar().obterDetalhes(id, perfil);
    }

    public List<Episodio> episodios(int conteudoId) throws SQLException {
        Conteudo conteudo = conteudo(conteudoId);
        return conteudo instanceof Serie serie ? serie.listarEpisodios() : List.of();
    }

    public boolean adicionarFavorito(int id) throws SQLException {
        Perfil perfil = perfilComDados();
        Conteudo conteudo = catalogo.carregar().obterDetalhes(id, perfil);
        return perfil.adicionarFavorito(conteudo) && catalogo.adicionarFavorito(perfilId, id);
    }

    public boolean removerFavorito(int id) throws SQLException {
        Perfil perfil = perfilComDados();
        return perfil.removerFavorito(id) && catalogo.removerFavorito(perfilId, id);
    }

    public boolean editarFavorito(int id, int novoConteudoId) throws SQLException {
        Perfil perfil = perfilComDados();
        Conteudo novo = catalogo.carregar().obterDetalhes(novoConteudoId, perfil);
        return perfil.editarFavorito(id, novo) && catalogo.editarFavorito(perfilId, id, novoConteudoId);
    }

    public List<Conteudo> favoritos() throws SQLException {
        return perfilComDados().listarFavoritos().stream().map(Favorito::conteudo).toList();
    }

    public List<Conteudo> recomendacoes() throws SQLException {
        Perfil perfil = perfilComDados();
        return recomendacao.recomendar(perfil, catalogo.carregar());
    }

    public List<Visualizacao> historico() throws SQLException { return perfilComDados().listarHistorico(); }

    public Reproducao prepararReproducao(int conteudoId, int episodioId) throws SQLException {
        Perfil perfil = perfilComDados();
        Reproduzivel item = catalogo.carregar().obterDetalhes(conteudoId, perfil).obterItem(episodioId);
        return item.reproduzir(perfil);
    }

    public int registrarReproducao(Reproducao reproducao, boolean reiniciar, int avancar) throws SQLException {
        // Recarrega perfil, plano e item para não confiar nos dados enviados pela interface.
        Perfil perfil = perfilComDados();
        Reproduzivel item = catalogo.carregar().obterDetalhes(reproducao.conteudoId(), perfil)
                .obterItem(reproducao.episodioId());
        Reproducao atual = item.reproduzir(perfil);
        if (reiniciar) atual.reiniciar();
        atual.avancar(avancar);
        item.pausar();
        visualizacoes.salvar(perfilId, perfil.obterVisualizacao(item));
        return atual.progresso();
    }
}
