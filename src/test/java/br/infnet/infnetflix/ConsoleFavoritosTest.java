package br.infnet.infnetflix;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;

class ConsoleFavoritosTest {
    @TempDir Path dir;

    @Test
    void editaFavoritoPeloMenuEPersisteSubstituicao() throws Exception {
        Path arquivo = dir.resolve("console.db");
        String comandos = String.join("\n", "1", "Ana", "ana@example.com", "senha123", "01/01/1990",
                "2", "ana@example.com", "senha123", "1", "1", "2", "1", "2",
                "3", "editar", "1", "4", "3", "", "6", "6", "0", "");
        InputStream entrada = System.in;
        PrintStream saida = System.out;
        ByteArrayOutputStream captura = new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream(comandos.getBytes(StandardCharsets.UTF_8)));
            System.setOut(new PrintStream(captura, true, StandardCharsets.UTF_8));
            App.main(new String[] {arquivo.toString()});
        } finally {
            System.setIn(entrada);
            System.setOut(saida);
        }
        String texto = captura.toString(StandardCharsets.UTF_8);
        assertTrue(texto.contains("Favorito atualizado."), texto);
        assertTrue(texto.contains("4 - Além das Estrelas"), texto);
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + arquivo);
                Statement s = c.createStatement();
                ResultSet r = s.executeQuery("SELECT conteudo_id FROM favoritos")) {
            assertTrue(r.next());
            assertEquals(4, r.getInt(1));
            assertFalse(r.next());
        }
    }
}
