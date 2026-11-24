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

    /** Pausa a sessão anterior, se ativa, e abre outra com o progresso registrado pelo perfil. */
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

    /** Calcula quantos segundos podem ser assistidos a partir da posição informada. */
    public int limite(int inicio) {
        int segundosAteOFim = duracao() - inicio;
        return Math.min(segundosAteOFim, segundosRestantes);
    }

    private void exigirAtiva() {
        if (pausada) {
            throw new IllegalStateException("A reprodução já foi pausada.");
        }
    }

    /** Volta ao início e mantém o saldo de tempo da sessão. */
    public void reiniciar() {
        exigirAtiva();
        progresso = 0;
    }

    /** Avança dentro da duração do item e consome o saldo da sessão no plano gratuito. */
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

    /** Encerra a sessão e registra a posição atual no histórico em memória do perfil. */
    public Visualizacao pausar() {
        exigirAtiva();
        Visualizacao visualizacao = perfil.registrarVisualizacao(item, progresso);
        pausada = true;
        return visualizacao;
    }
}
