package br.infnet.infnetflix.model;

import java.time.LocalDate;

public final class Validacao {
    private Validacao() {}

    public static String obrigatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " não pode ficar vazio.");
        }
        return valor.trim();
    }

    public static void dataNascimento(LocalDate data) {
        if (data == null || data.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Informe uma data de nascimento válida, que não seja futura.");
        }
    }
}
