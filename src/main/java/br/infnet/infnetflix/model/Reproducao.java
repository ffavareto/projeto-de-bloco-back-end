package br.infnet.infnetflix.model;

/** Sessão de reprodução simulada, com retomada e limite acumulado do plano gratuito. */
public final class Reproducao {
    private final int conteudoId;
    private final int episodioId;
    private final int duracao;
    private final Plano plano;
    private int progresso;
    private int avancados;
    private boolean pausada;
    private Reproduzivel item;
    private Perfil perfil;

    /** Representação dos dados enviados à interface; o controlador sempre os revalida. */
    public Reproducao(int conteudoId, int episodioId, int duracao, int progresso, boolean premium) {
        this.conteudoId = conteudoId;
        this.episodioId = episodioId;
        this.duracao = duracao;
        this.progresso = progresso;
        this.plano = new Plano(0, premium ? TipoPlano.PREMIUM : TipoPlano.GRATUITO);
    }

    static Reproducao iniciar(Reproduzivel item, Perfil perfil) {
        perfil.exigirConteudoPermitido(item.conteudo());
        Visualizacao anterior = perfil.obterVisualizacao(item);
        Reproducao r = new Reproducao(item.conteudo().id(), item.episodioId(), item.duracao(),
                anterior == null ? 0 : anterior.progresso(), !perfil.plano().exibeAnuncios());
        r.item = item;
        r.perfil = perfil;
        return r;
    }

    public int conteudoId() { return conteudoId; }
    public int episodioId() { return episodioId; }
    public int duracao() { return duracao; }
    public int progresso() { return progresso; }
    public boolean premium() { return !plano.exibeAnuncios(); }
    public boolean pausada() { return pausada; }

    public int limite(int inicio) {
        return Math.min(duracao - inicio, premium() ? duracao : plano.limiteSegundos() - avancados);
    }

    private void exigirAtiva() {
        if (item == null || pausada) throw new IllegalStateException("Inicie a reprodução primeiro.");
    }

    public void reiniciar() {
        exigirAtiva();
        progresso = 0;
    }

    public void avancar(int segundos) {
        exigirAtiva();
        if (segundos < 0 || segundos > limite(progresso)) {
            throw new IllegalArgumentException("O avanço está fora do intervalo permitido.");
        }
        progresso += segundos;
        if (!premium()) avancados += segundos;
    }

    public void pausar() {
        exigirAtiva();
        perfil.registrarVisualizacao(item, progresso);
        pausada = true;
    }
}
