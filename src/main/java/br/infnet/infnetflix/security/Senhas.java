package br.infnet.infnetflix.security;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Gera e verifica hashes de senha com PBKDF2-HMAC-SHA256. */
public final class Senhas {
    private static final int ITERACOES = 210_000;

    /** Gera um sal aleatório e retorna o registro no formato {@code iteracoes:sal:hash}. */
    public static String criar(String senha) {
        byte[] sal = new byte[16];
        new SecureRandom().nextBytes(sal);
        Base64.Encoder codificador = Base64.getEncoder();
        String salCodificado = codificador.encodeToString(sal);
        byte[] hash = derivar(senha, sal, ITERACOES);
        String hashCodificado = codificador.encodeToString(hash);
        return ITERACOES + ":" + salCodificado + ":" + hashCodificado;
    }

    /** Recalcula o hash com os parâmetros salvos e o compara com o hash do registro. */
    public static boolean verificar(String senha, String registro) {
        try {
            String[] partes = registro.split(":");
            Base64.Decoder decodificador = Base64.getDecoder();
            byte[] hashSalvo = decodificador.decode(partes[2]);
            byte[] sal = decodificador.decode(partes[1]);
            int iteracoes = Integer.parseInt(partes[0]);
            byte[] hashDigitado = derivar(senha, sal, iteracoes);
            return MessageDigest.isEqual(hashSalvo, hashDigitado);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            return false;
        }
    }

    private static byte[] derivar(String senha, byte[] sal, int iteracoes) {
        PBEKeySpec spec = new PBEKeySpec(senha.toCharArray(), sal, iteracoes, 256);
        try {
            SecretKeyFactory fabrica = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return fabrica.generateSecret(spec).getEncoded();
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException(e);
        } finally {
            spec.clearPassword();
        }
    }
}
