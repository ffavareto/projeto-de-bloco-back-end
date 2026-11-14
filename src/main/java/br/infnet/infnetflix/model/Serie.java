package br.infnet.infnetflix.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Serie extends Conteudo {
    private final List<Temporada> temporadas = new ArrayList<>();

    public Serie(int id, String titulo, Genero genero, String classificacao, String sinopse) {
        super(id, titulo, genero, classificacao, sinopse);
    }

    @Override public String tipo() { return "SERIE"; }
    public List<Temporada> temporadas() { return List.copyOf(temporadas); }

    @Override
    public Reproduzivel obterItem(int episodioId) {
        return listarEpisodios().stream().filter(e -> e.id() == episodioId).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Episódio não pertence a esta série."));
    }

    public void adicionarTemporada(Temporada temporada) {
        if (temporadas.stream().anyMatch(t -> t.numero() == temporada.numero())
                || temporada.episodios().stream().anyMatch(e -> e.conteudo().id() != id())) {
            throw new IllegalArgumentException("Temporada duplicada ou de outra série.");
        }
        temporadas.add(temporada);
        temporadas.sort(Comparator.comparingInt(Temporada::numero));
    }

    public List<Episodio> listarEpisodios() {
        return temporadas.stream().flatMap(t -> t.episodios().stream()).toList();
    }
}
