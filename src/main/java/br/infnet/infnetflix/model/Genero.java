package br.infnet.infnetflix.model;

/** Categoria usada para classificar os títulos do catálogo. */
public record Genero(int id, String nome) {
    public Genero {
        nome = Validacao.obrigatorio(nome, "Gênero");
    }

    @Override
    public String toString() { return nome; }
}
