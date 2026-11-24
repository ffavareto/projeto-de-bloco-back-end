package br.infnet.infnetflix.model;

/** Título do catálogo que é reproduzido como um único item. */
public final class Filme extends Conteudo implements Reproduzivel {
    private final int duracao;
    private Reproducao reproducao;

    public Filme(int id, String titulo, Genero genero, String classificacao, String sinopse, int duracao) {
        super(id, titulo, genero, classificacao, sinopse);
        if (duracao <= 0) throw new IllegalArgumentException("Duração inválida.");
        this.duracao = duracao;
    }

    @Override public String tipo() { return "FILME"; }
    @Override public int duracao() { return duracao; }
    @Override public Conteudo conteudo() { return this; }
    @Override public int episodioId() { return 0; }

    @Override
    public Reproduzivel obterItem(int episodioId) {
        if (episodioId != 0) throw new IllegalArgumentException("Filme não possui episódios.");
        return this;
    }

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
