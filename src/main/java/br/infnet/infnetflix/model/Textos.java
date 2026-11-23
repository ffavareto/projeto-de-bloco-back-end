package br.infnet.infnetflix.model;

import java.text.Normalizer;
import java.util.Locale;

/** Busca em português, independente de caixa, acentos e forma Unicode. */
public final class Textos {
    private Textos() {}

    public static String normalizar(String texto) {
        String decomposto = Normalizer.normalize(texto, Normalizer.Form.NFD);
        String semAcentos = decomposto.replaceAll("\\p{M}+", "");
        return semAcentos.toLowerCase(Locale.ROOT);
    }

    public static boolean contem(String texto, String termo) {
        return normalizar(texto).contains(normalizar(termo));
    }
}
