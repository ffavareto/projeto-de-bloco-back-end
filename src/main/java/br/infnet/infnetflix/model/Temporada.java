package br.infnet.infnetflix.model;

import java.util.Comparator;
import java.util.List;

public record Temporada(int id, int numero, List<Episodio> episodios) {
    public Temporada {
        if (numero <= 0 || episodios.isEmpty()
                || episodios.stream().anyMatch(e -> e.temporada() != numero)
                || episodios.stream().map(Episodio::numero).distinct().count() != episodios.size()) {
            throw new IllegalArgumentException("Temporada inválida.");
        }
        episodios = episodios.stream().sorted(Comparator.comparingInt(Episodio::numero)).toList();
    }
}
