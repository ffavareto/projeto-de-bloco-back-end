package br.infnet.infnetflix.model;

import java.text.Normalizer;
import java.util.Locale;

/** Busca em português, independente de caixa, acentos e forma Unicode. */
public final class Textos {
    private Textos() {}

    public static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }

    public static boolean contem(String texto, String termo) {
        return normalizar(texto).contains(normalizar(termo));
    }
}
