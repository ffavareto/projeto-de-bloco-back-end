package br.infnet.infnetflix.controller;

import br.infnet.infnetflix.model.Catalogo;
import br.infnet.infnetflix.model.Conta;
import br.infnet.infnetflix.model.Conteudo;
import br.infnet.infnetflix.model.Favorito;
import br.infnet.infnetflix.model.FiltroCatalogo;
import br.infnet.infnetflix.model.Perfil;
import br.infnet.infnetflix.model.Reproducao;
import br.infnet.infnetflix.model.Reproduzivel;
import br.infnet.infnetflix.model.Visualizacao;
import br.infnet.infnetflix.repository.CatalogoRepository;
import br.infnet.infnetflix.repository.ContaRepository;
import br.infnet.infnetflix.repository.VisualizacaoRepository;
import br.infnet.infnetflix.service.ServicoAutenticacao;
import br.infnet.infnetflix.service.ServicoRecomendacao;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    /** Recarrega do banco a conta associada à sessão ativa. */
    private Conta contaAtual() throws SQLException {
        if (!autenticado()) {
            throw new IllegalStateException("Entre na conta primeiro.");
        }
        Optional<Conta> conta = contas.buscarPorId(contaId);
        if (conta.isEmpty()) {
            throw new IllegalStateException("Conta não encontrada.");
        }
        return conta.get();
    }

    private Perfil perfilAtual() throws SQLException {
        Conta conta = contaAtual();
        if (!perfilSelecionado()) {
            throw new IllegalStateException("Selecione um perfil primeiro.");
        }
        return conta.selecionarPerfil(perfilId);
    }

    private Perfil perfilComFavoritos() throws SQLException {
        Perfil perfil = perfilAtual();
        List<Favorito> favoritos = catalogo.favoritos(perfil.id());
        perfil.restaurarFavoritos(favoritos);
        return perfil;
    }

    private Perfil perfilComHistorico() throws SQLException {
        Perfil perfil = perfilAtual();
        List<Visualizacao> historico = visualizacoes.historico(perfil.id());
        perfil.restaurarHistorico(historico);
        return perfil;
    }

    public void cadastrar(String nome, String email, String senha, LocalDate dataNascimento) throws SQLException {
        autenticacao.cadastrar(nome, email, senha, dataNascimento);
    }

    /** Encerra a sessão anterior antes de validar as credenciais da nova conta. */
    public boolean entrar(String email, String senha) throws SQLException {
        sair();
        Optional<Conta> conta = autenticacao.login(email, senha);
        if (conta.isEmpty()) {
            return false;
        }
        contaId = conta.get().id();
        return true;
    }

    public void sair() {
        contaId = 0;
        perfilId = 0;
    }

    /** Limpa a seleção do perfil e mantém a conta autenticada. */
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
        Perfil perfil = conta.editarPerfil(id, nome);
        int perfisRenomeados = contas.renomearPerfil(contaId, id, perfil.nome());
        if (perfisRenomeados == 0) {
            throw new IllegalArgumentException("Perfil não encontrado.");
        }
    }

    public void validarExclusaoPerfil(int id) throws SQLException { contaAtual().validarExclusaoPerfil(id); }

    /** Remove o perfil do banco e limpa a seleção caso ele esteja ativo. */
    public void excluirPerfil(int id) throws SQLException {
        contaAtual().excluirPerfil(id);
        int perfisExcluidos = contas.excluirPerfil(contaId, id);
        if (perfisExcluidos == 0) {
            throw new IllegalArgumentException("Perfil não encontrado.");
        }
        if (perfilId == id) {
            trocarPerfil();
        }
    }

    public boolean premium() throws SQLException { return !contaAtual().plano().exibeAnuncios(); }

    public void ativarPremium() throws SQLException {
        Conta conta = contaAtual();
        conta.assinarPremium();
        contas.salvarPlano(conta);
    }

    public List<Conteudo> buscar(FiltroCatalogo filtro) throws SQLException {
        Perfil perfil = perfilAtual();
        Catalogo catalogoAtual = catalogo.carregar();
        return catalogoAtual.buscar(filtro, perfil);
    }

    public Conteudo conteudo(int id) throws SQLException {
        return conteudoPermitido(id, perfilAtual());
    }

    /** Resolve o título no catálogo e aplica a restrição etária do perfil. */
    private Conteudo conteudoPermitido(int id, Perfil perfil) throws SQLException {
        Optional<Conteudo> resultado = catalogo.buscarPorId(id);
        if (resultado.isEmpty()) {
            throw new IllegalArgumentException("Conteúdo não encontrado.");
        }
        Conteudo conteudo = resultado.get();
        perfil.exigirConteudoPermitido(conteudo);
        return conteudo;
    }

    public boolean adicionarFavorito(int id) throws SQLException {
        Perfil perfil = perfilComFavoritos();
        Conteudo conteudo = conteudoPermitido(id, perfil);
        if (!perfil.adicionarFavorito(conteudo)) {
            return false;
        }
        return catalogo.adicionarFavorito(perfilId, id);
    }

    public boolean removerFavorito(int id) throws SQLException {
        Perfil perfil = perfilComFavoritos();
        if (!perfil.removerFavorito(id)) {
            return false;
        }
        return catalogo.removerFavorito(perfilId, id);
    }

    public boolean editarFavorito(int id, int novoConteudoId) throws SQLException {
        Perfil perfil = perfilComFavoritos();
        Conteudo novo = conteudoPermitido(novoConteudoId, perfil);
        if (!perfil.editarFavorito(id, novo)) {
            return false;
        }
        return catalogo.editarFavorito(perfilId, id, novoConteudoId);
    }

    public List<Conteudo> favoritos() throws SQLException {
        Perfil perfil = perfilComFavoritos();
        List<Conteudo> conteudos = new ArrayList<>();
        for (Favorito favorito : perfil.listarFavoritos()) {
            conteudos.add(favorito.conteudo());
        }
        return List.copyOf(conteudos);
    }

    public List<Conteudo> recomendacoes() throws SQLException {
        Perfil perfil = perfilComHistorico();
        return recomendacao.recomendar(perfil, catalogo.carregar());
    }

    public List<Visualizacao> historico() throws SQLException { return perfilComHistorico().listarHistorico(); }

    /** Carrega o progresso salvo e abre a sessão simulada com o plano atual da conta. */
    public Reproducao prepararReproducao(int conteudoId, int episodioId) throws SQLException {
        Perfil perfil = perfilComHistorico();
        Conteudo conteudo = conteudoPermitido(conteudoId, perfil);
        Reproduzivel item = conteudo.obterItem(episodioId);
        return item.reproduzir(perfil);
    }

    /** Revalida a sessão e o conteúdo, grava o avanço e retorna a nova posição em segundos. */
    public int registrarReproducao(int conteudoId, int episodioId, boolean reiniciar, int segundos)
            throws SQLException {
        Reproducao reproducao = prepararReproducao(conteudoId, episodioId);
        if (reiniciar) {
            reproducao.reiniciar();
        }
        reproducao.avancar(segundos);
        Visualizacao visualizacao = reproducao.pausar();
        visualizacoes.salvar(perfilId, visualizacao);
        return visualizacao.progresso();
    }
}
