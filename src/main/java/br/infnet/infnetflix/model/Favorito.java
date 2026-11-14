package br.infnet.infnetflix.model;

import java.time.LocalDate;
import java.util.Objects;

public record Favorito(int id, Conteudo conteudo, LocalDate dataAdicao) {
    public Favorito {
        Objects.requireNonNull(conteudo);
        Objects.requireNonNull(dataAdicao);
    }
}
