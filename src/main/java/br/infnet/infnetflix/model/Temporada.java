package br.infnet.infnetflix.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Agrupa episódios da mesma temporada em ordem numérica e com números únicos. */
public record Temporada(int id, int numero, List<Episodio> episodios) {
    public Temporada {
        if (numero <= 0 || episodios.isEmpty()) {
            throw new IllegalArgumentException("Temporada inválida.");
        }
        Set<Integer> numeros = new HashSet<>();
        for (Episodio episodio : episodios) {
            if (episodio.temporada() != numero) {
                throw new IllegalArgumentException("Temporada inválida.");
            }
            numeros.add(episodio.numero());
        }
        if (numeros.size() != episodios.size()) {
            throw new IllegalArgumentException("Temporada inválida.");
        }
        List<Episodio> ordenados = new ArrayList<>(episodios);
        ordenados.sort(Comparator.comparingInt(Episodio::numero));
        episodios = List.copyOf(ordenados);
    }
}
