package br.infnet.infnetflix.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Perfil de espectador com data de nascimento, favoritos e histórico próprios. */
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

    /** Rejeita a operação quando o perfil está abaixo da idade mínima do título. */
    public void exigirConteudoPermitido(Conteudo conteudo) {
        if (!podeAssistir(conteudo)) throw new IllegalArgumentException("Conteúdo não encontrado.");
    }

    public void restaurarFavoritos(List<Favorito> favoritos) {
        this.favoritos.clear();
        this.favoritos.addAll(favoritos);
    }

    public void restaurarHistorico(List<Visualizacao> historico) {
        this.historico.clear();
        this.historico.addAll(historico);
    }

    /** Adiciona um título permitido e retorna falso quando ele já está na lista. */
    public boolean adicionarFavorito(Conteudo conteudo) {
        exigirConteudoPermitido(conteudo);
        if (indiceFavorito(conteudo.id()) >= 0) {
            return false;
        }
        favoritos.add(new Favorito(0, conteudo, LocalDate.now()));
        return true;
    }

    public boolean removerFavorito(int conteudoId) {
        return favoritos.removeIf(favorito -> favorito.conteudo().id() == conteudoId);
    }

    /** Editar a lista significa substituir um título mantendo a posição/data do favorito. */
    public boolean editarFavorito(int conteudoId, Conteudo novoConteudo) {
        exigirConteudoPermitido(novoConteudo);
        int indice = indiceFavorito(conteudoId);
        if (indice < 0) {
            return false;
        }
        if (conteudoId != novoConteudo.id() && indiceFavorito(novoConteudo.id()) >= 0) {
            throw new IllegalArgumentException("O novo conteúdo já está nos favoritos.");
        }
        Favorito original = favoritos.get(indice);
        favoritos.set(indice, new Favorito(original.id(), novoConteudo, original.dataAdicao()));
        return true;
    }

    private int indiceFavorito(int conteudoId) {
        for (int indice = 0; indice < favoritos.size(); indice++) {
            if (favoritos.get(indice).conteudo().id() == conteudoId) {
                return indice;
            }
        }
        return -1;
    }

    /** Retorna os favoritos compatíveis com a idade atual do perfil. */
    public List<Favorito> listarFavoritos() {
        List<Favorito> permitidos = new ArrayList<>();
        for (Favorito favorito : favoritos) {
            if (podeAssistir(favorito.conteudo())) {
                permitidos.add(favorito);
            }
        }
        return List.copyOf(permitidos);
    }

    /** Recupera a visualização do filme ou episódio, ou retorna {@code null} se ainda não houver registro. */
    public Visualizacao obterVisualizacao(Reproduzivel item) {
        for (Visualizacao visualizacao : historico) {
            if (visualizacao.correspondeA(item)) {
                return visualizacao;
            }
        }
        return null;
    }

    /** Cria ou atualiza o progresso do item no histórico em memória. */
    public Visualizacao registrarVisualizacao(Reproduzivel item, int progressoSegundos) {
        exigirConteudoPermitido(item.conteudo());
        Visualizacao visualizacao = obterVisualizacao(item);
        if (visualizacao == null) {
            visualizacao = new Visualizacao(0, item, progressoSegundos, LocalDateTime.now(ZoneOffset.UTC));
            historico.add(visualizacao);
        } else {
            visualizacao.atualizarProgresso(progressoSegundos);
        }
        return visualizacao;
    }

    /** Retorna o histórico do acesso mais recente ao mais antigo. */
    public List<Visualizacao> listarHistorico() {
        List<Visualizacao> ordenado = new ArrayList<>(historico);
        ordenado.sort(Comparator.comparing(Visualizacao::dataUltimoAcesso).reversed());
        return Collections.unmodifiableList(ordenado);
    }
}
