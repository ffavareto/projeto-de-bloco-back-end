package br.infnet.infnetflix.security;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class Senhas {
    private static final int ITERACOES = 210_000;

    public static String criar(String senha) {
        byte[] sal = new byte[16];
        new SecureRandom().nextBytes(sal);
        return ITERACOES
                + ":"
                + Base64.getEncoder().encodeToString(sal)
                + ":"
                + Base64.getEncoder().encodeToString(derivar(senha, sal, ITERACOES));
    }

    public static boolean verificar(String senha, String registro) {
        try {
            String[] partes = registro.split(":");
            return MessageDigest.isEqual(
                    Base64.getDecoder().decode(partes[2]),
                    derivar(
                            senha,
                            Base64.getDecoder().decode(partes[1]),
                            Integer.parseInt(partes[0])));
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            return false;
        }
    }

    private static byte[] derivar(String senha, byte[] sal, int iteracoes) {
        PBEKeySpec spec = new PBEKeySpec(senha.toCharArray(), sal, iteracoes, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException(e);
        } finally {
            spec.clearPassword();
        }
    }
}
