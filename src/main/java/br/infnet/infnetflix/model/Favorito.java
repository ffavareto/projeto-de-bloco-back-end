package br.infnet.infnetflix.model;

import java.time.LocalDate;
import java.util.Objects;

/** Título salvo nos favoritos de um perfil, com a data de inclusão. */
public record Favorito(int id, Conteudo conteudo, LocalDate dataAdicao) {
    public Favorito {
        Objects.requireNonNull(conteudo);
        Objects.requireNonNull(dataAdicao);
    }
}
