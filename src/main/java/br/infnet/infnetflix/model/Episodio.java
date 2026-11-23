package br.infnet.infnetflix.model;

import java.util.Objects;

public final class Episodio implements Reproduzivel {
    private final int id;
    private final Serie serie;
    private final int temporada;
    private final int numero;
    private final String titulo;
    private final int duracao;
    private Reproducao reproducao;

    public Episodio(int id, Serie serie, int temporada, int numero, String titulo, int duracao) {
        if (temporada <= 0 || numero <= 0 || duracao <= 0) {
            throw new IllegalArgumentException("Episódio inválido.");
        }
        this.id = id;
        this.serie = Objects.requireNonNull(serie);
        this.temporada = temporada;
        this.numero = numero;
        this.titulo = Validacao.obrigatorio(titulo, "Título");
        this.duracao = duracao;
    }

    public int id() { return id; }
    public int conteudoId() { return serie.id(); }
    public int temporada() { return temporada; }
    public int numero() { return numero; }
    public String titulo() { return titulo; }
    @Override public int duracao() { return duracao; }
    @Override public Conteudo conteudo() { return serie; }
    @Override public int episodioId() { return id; }

    @Override
    public Reproducao reproduzir(Perfil perfil) {
        reproducao = Reproducao.iniciar(this, perfil, reproducao);
        return reproducao;
    }

    @Override
    public void pausar() {
        Reproducao.pausarAtual(reproducao);
    }
}
