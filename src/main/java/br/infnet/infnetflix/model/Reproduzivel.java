package br.infnet.infnetflix.model;

/** Contrato de reprodução compartilhado por filmes e episódios. */
public interface Reproduzivel {
    /** Retorna o título do catálogo ao qual o item pertence. */
    Conteudo conteudo();

    /** Retorna o ID do episódio ou zero para filmes. */
    int episodioId();

    /** Retorna a duração total em segundos. */
    int duracao();

    /** Abre uma sessão para o perfil a partir do progresso registrado. */
    Reproducao reproduzir(Perfil perfil);

    /** Pausa a sessão atual e registra o progresso no perfil. */
    void pausar();
}
