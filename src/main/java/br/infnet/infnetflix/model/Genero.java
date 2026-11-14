package br.infnet.infnetflix.model;

public record Genero(int id, String nome) {
    public Genero {
        nome = Validacao.obrigatorio(nome, "Gênero");
    }

    @Override
    public String toString() { return nome; }
}
