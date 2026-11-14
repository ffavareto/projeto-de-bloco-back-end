package br.infnet.infnetflix.model;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;

public final class Perfil {
    private final int id;
    private final int contaId;
    private String nome;
    private final LocalDate dataNascimento;
    private Plano plano = new Plano(0, TipoPlano.GRATUITO);
    private final List<Favorito> favoritos = new ArrayList<>();
    private final List<Visualizacao> historico = new ArrayList<>();

    public Perfil(int id, int contaId, String nome, LocalDate dataNascimento) {
        this.id = id;
        this.contaId = contaId;
        renomear(nome);
        Validacao.dataNascimento(dataNascimento);
        this.dataNascimento = dataNascimento;
    }

    public int id() { return id; }
    public int contaId() { return contaId; }
    public String nome() { return nome; }
    public LocalDate dataNascimento() { return dataNascimento; }
    public Plano plano() { return plano; }
    void usarPlano(Plano plano) { this.plano = Objects.requireNonNull(plano); }
    void renomear(String nome) { this.nome = Validacao.obrigatorio(nome, "Nome do perfil"); }

    public int idade() {
        Validacao.dataNascimento(dataNascimento);
        return Period.between(dataNascimento, LocalDate.now()).getYears();
    }

    public boolean podeAssistir(Conteudo conteudo) { return idade() >= conteudo.idadeMinima(); }

    public void exigirConteudoPermitido(Conteudo conteudo) {
        if (!podeAssistir(conteudo)) throw new IllegalArgumentException("Conteúdo não encontrado.");
    }

    public void restaurarDados(List<Favorito> favoritos, List<Visualizacao> historico) {
        this.favoritos.clear();
        this.favoritos.addAll(favoritos);
        this.historico.clear();
        this.historico.addAll(historico);
    }

    public boolean adicionarFavorito(Conteudo conteudo) {
        exigirConteudoPermitido(conteudo);
        if (favoritos.stream().anyMatch(f -> f.conteudo().id() == conteudo.id())) return false;
        favoritos.add(new Favorito(0, conteudo, LocalDate.now()));
        return true;
    }

    public boolean removerFavorito(int conteudoId) {
        return favoritos.removeIf(f -> f.conteudo().id() == conteudoId);
    }

    /** Editar a lista significa substituir um título mantendo a posição/data do favorito. */
    public boolean editarFavorito(int conteudoId, Conteudo novoConteudo) {
        exigirConteudoPermitido(novoConteudo);
        Favorito original = favoritos.stream().filter(f -> f.conteudo().id() == conteudoId)
                .findFirst().orElse(null);
        if (original == null) return false;
        if (conteudoId != novoConteudo.id()
                && favoritos.stream().anyMatch(f -> f.conteudo().id() == novoConteudo.id())) {
            throw new IllegalArgumentException("O novo conteúdo já está nos favoritos.");
        }
        favoritos.set(favoritos.indexOf(original),
                new Favorito(original.id(), novoConteudo, original.dataAdicao()));
        return true;
    }

    public List<Favorito> listarFavoritos() {
        return favoritos.stream().filter(f -> podeAssistir(f.conteudo())).toList();
    }

    public Visualizacao obterVisualizacao(Reproduzivel item) {
        return historico.stream().filter(v -> v.correspondeA(item)).findFirst().orElse(null);
    }

    public void registrarVisualizacao(Reproduzivel item, int progressoSegundos) {
        exigirConteudoPermitido(item.conteudo());
        Visualizacao visualizacao = obterVisualizacao(item);
        if (visualizacao == null) {
            historico.add(new Visualizacao(0, item, progressoSegundos, java.time.LocalDateTime.now(java.time.ZoneOffset.UTC)));
        } else {
            visualizacao.atualizarProgresso(progressoSegundos);
        }
    }

    public List<Visualizacao> listarHistorico() {
        return historico.stream().sorted(Comparator.comparing(Visualizacao::dataUltimoAcesso).reversed()).toList();
    }
}
