package br.infnet.infnetflix.model;

/** Sessão de reprodução simulada, com retomada e limite acumulado do plano gratuito. */
public final class Reproducao {
    private final Reproduzivel item;
    private final Perfil perfil;
    private final boolean premium;
    private int progresso;
    private int segundosRestantes;
    private boolean pausada;

    private Reproducao(Reproduzivel item, Perfil perfil) {
        perfil.exigirConteudoPermitido(item.conteudo());
        Visualizacao anterior = perfil.obterVisualizacao(item);
        this.item = item;
        this.perfil = perfil;
        Plano plano = perfil.plano();
        this.premium = !plano.exibeAnuncios();
        this.segundosRestantes = plano.limiteSegundos();
        this.progresso = anterior == null ? 0 : anterior.progresso();
    }

    static Reproducao iniciar(Reproduzivel item, Perfil perfil, Reproducao atual) {
        if (atual != null && !atual.pausada) {
            atual.pausar();
        }
        return new Reproducao(item, perfil);
    }

    static void pausarAtual(Reproducao atual) {
        if (atual == null) {
            throw new IllegalStateException("Inicie a reprodução primeiro.");
        }
        atual.pausar();
    }

    public int duracao() { return item.duracao(); }
    public int progresso() { return progresso; }
    public boolean premium() { return premium; }

    public int limite(int inicio) {
        int segundosAteOFim = duracao() - inicio;
        return Math.min(segundosAteOFim, segundosRestantes);
    }

    private void exigirAtiva() {
        if (pausada) {
            throw new IllegalStateException("A reprodução já foi pausada.");
        }
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
        if (!premium) {
            segundosRestantes -= segundos;
        }
    }

    public Visualizacao pausar() {
        exigirAtiva();
        Visualizacao visualizacao = perfil.registrarVisualizacao(item, progresso);
        pausada = true;
        return visualizacao;
    }
}
