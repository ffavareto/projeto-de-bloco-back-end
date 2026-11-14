package br.infnet.infnetflix.model;

public interface Reproduzivel {
    Conteudo conteudo();
    int episodioId();
    int duracao();
    Reproducao reproduzir(Perfil perfil);
    void pausar();
}
