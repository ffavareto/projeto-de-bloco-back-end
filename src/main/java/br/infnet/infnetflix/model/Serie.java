package br.infnet.infnetflix.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Título do catálogo que agrupa episódios em temporadas. */
public final class Serie extends Conteudo {
    private final List<Temporada> temporadas = new ArrayList<>();

    public Serie(int id, String titulo, Genero genero, String classificacao, String sinopse) {
        super(id, titulo, genero, classificacao, sinopse);
    }

    @Override public String tipo() { return "SERIE"; }
    public List<Temporada> temporadas() { return List.copyOf(temporadas); }

    @Override
    public Reproduzivel obterItem(int episodioId) {
        for (Episodio episodio : listarEpisodios()) {
            if (episodio.id() == episodioId) {
                return episodio;
            }
        }
        throw new IllegalArgumentException("Episódio não pertence a esta série.");
    }

    /** Inclui uma temporada da própria série e mantém a ordem numérica das temporadas. */
    public void adicionarTemporada(Temporada temporada) {
        for (Temporada existente : temporadas) {
            if (existente.numero() == temporada.numero()) {
                throw new IllegalArgumentException("Temporada duplicada ou de outra série.");
            }
        }
        for (Episodio episodio : temporada.episodios()) {
            if (episodio.conteudo().id() != id()) {
                throw new IllegalArgumentException("Temporada duplicada ou de outra série.");
            }
        }
        temporadas.add(temporada);
        temporadas.sort(Comparator.comparingInt(Temporada::numero));
    }

    /** Lista todos os episódios em ordem de temporada e número. */
    public List<Episodio> listarEpisodios() {
        List<Episodio> episodios = new ArrayList<>();
        for (Temporada temporada : temporadas) {
            episodios.addAll(temporada.episodios());
        }
        return List.copyOf(episodios);
    }
}
