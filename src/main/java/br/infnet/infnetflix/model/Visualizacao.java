package br.infnet.infnetflix.model;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/** Guarda a posição em segundos e o horário em UTC do último acesso a um filme ou episódio. */
public final class Visualizacao {
    private final int id;
    private final Reproduzivel item;
    private int progresso;
    private LocalDateTime dataUltimoAcesso;

    public Visualizacao(int id, Reproduzivel item, int progresso, LocalDateTime dataUltimoAcesso) {
        this.id = id;
        this.item = Objects.requireNonNull(item);
        validarProgresso(progresso);
        this.progresso = progresso;
        this.dataUltimoAcesso = Objects.requireNonNull(dataUltimoAcesso);
    }

    private void validarProgresso(int segundos) {
        if (segundos < 0 || segundos > item.duracao()) {
            throw new IllegalArgumentException("Progresso fora da duração do conteúdo.");
        }
    }

    /** Atualiza a posição absoluta dentro da duração do item e registra o horário do acesso. */
    public void atualizarProgresso(int segundos) {
        validarProgresso(segundos);
        progresso = segundos;
        dataUltimoAcesso = LocalDateTime.now(ZoneOffset.UTC);
    }

    /** Identifica o mesmo item pela combinação do ID do título e do ID do episódio. */
    public boolean correspondeA(Reproduzivel outro) {
        return item.conteudo().id() == outro.conteudo().id() && item.episodioId() == outro.episodioId();
    }

    public int id() { return id; }
    public Reproduzivel item() { return item; }
    public int progresso() { return progresso; }
    public LocalDateTime dataUltimoAcesso() { return dataUltimoAcesso; }
    public String titulo() { return item.conteudo().titulo(); }
    public Integer temporada() { return item instanceof Episodio e ? e.temporada() : null; }
    public Integer numero() { return item instanceof Episodio e ? e.numero() : null; }
    public String episodio() { return item instanceof Episodio e ? e.titulo() : null; }
    public String atualizadoEm() {
        return dataUltimoAcesso.format(DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss"));
    }
}
