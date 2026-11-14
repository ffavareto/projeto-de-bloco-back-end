package br.infnet.infnetflix.model;

import java.util.Objects;

/** O plano é parte da conta; a ativação é simulada neste trabalho. */
public final class Plano {
    private final int id;
    private TipoPlano tipo;

    public Plano(int id, TipoPlano tipo) {
        this.id = id;
        this.tipo = Objects.requireNonNull(tipo);
    }

    public int id() { return id; }
    public TipoPlano tipo() { return tipo; }
    public boolean exibeAnuncios() { return tipo == TipoPlano.GRATUITO; }
    public String qualidadeConteudo() { return exibeAnuncios() ? "SD" : "HD"; }
    public int limiteSegundos() { return exibeAnuncios() ? 1200 : Integer.MAX_VALUE; }
    public void ativarPremium() { tipo = TipoPlano.PREMIUM; }
}
