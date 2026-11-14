package br.infnet.infnetflix.model;

import java.util.Objects;

public abstract class Conteudo {
    private final int id;
    private final String titulo;
    private final Genero genero;
    private final String classificacao;
    private final String sinopse;

    protected Conteudo(int id, String titulo, Genero genero, String classificacao, String sinopse) {
        this.id = id;
        this.titulo = Validacao.obrigatorio(titulo, "Título");
        this.genero = Objects.requireNonNull(genero);
        this.classificacao = Validacao.obrigatorio(classificacao, "Classificação");
        this.sinopse = Validacao.obrigatorio(sinopse, "Sinopse");
    }

    public int id() { return id; }
    public String titulo() { return titulo; }
    public Genero genero() { return genero; }
    public String classificacao() { return classificacao; }
    public String sinopse() { return sinopse; }
    public abstract String tipo();
    public abstract Reproduzivel obterItem(int episodioId);

    public int idadeMinima() {
        return switch (Textos.normalizar(classificacao)) {
            case "livre" -> 0;
            case "10 anos" -> 10;
            case "12 anos" -> 12;
            case "14 anos" -> 14;
            case "16 anos" -> 16;
            case "18 anos" -> 18;
            default -> Integer.MAX_VALUE;
        };
    }
}
