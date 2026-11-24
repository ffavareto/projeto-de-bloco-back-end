package br.infnet.infnetflix.model;

/** Critérios opcionais de busca, com ordenação por título como padrão. */
public record FiltroCatalogo(
        String titulo, String tipo, String genero, String classificacao, Ordem ordem) {
    public FiltroCatalogo {
        titulo = titulo == null ? "" : titulo.trim();
        tipo = tipo == null ? "" : Textos.normalizar(tipo.trim()).toUpperCase(java.util.Locale.ROOT);
        genero = genero == null ? "" : genero.trim();
        classificacao = classificacao == null ? "" : classificacao.trim();
        ordem = ordem == null ? Ordem.TITULO : ordem;
        if (!tipo.isEmpty() && !tipo.equals("FILME") && !tipo.equals("SERIE")) {
            throw new IllegalArgumentException("Tipo inválido.");
        }
    }

    /** Critério principal de ordenação dos resultados. */
    public enum Ordem {
        TITULO,
        GENERO
    }
}
