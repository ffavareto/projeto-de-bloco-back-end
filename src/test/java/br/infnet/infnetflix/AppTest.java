package br.infnet.infnetflix;

import static org.junit.jupiter.api.Assertions.*;

import br.infnet.infnetflix.api.ServidorRest;
import br.infnet.infnetflix.repository.Database;
import br.infnet.infnetflix.security.Senhas;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.*;

class AppTest {
    @TempDir Path dir;

    @Test
    void senhaUsaSalEAceitaSomenteSenhaCorreta() {
        String a = Senhas.criar("senha segura");
        String b = Senhas.criar("senha segura");
        assertNotEquals(a, b);
        assertTrue(Senhas.verificar("senha segura", a));
        assertFalse(Senhas.verificar("senha errada", a));
    }

    @Test
    void fluxoPersistePerfisFavoritosEProgresso() throws Exception {
        Path arquivo = dir.resolve("teste.db");
        String comandos =
                String.join(
                        "\n",
                        "1",
                        "Lucas",
                        "lucas@example.com",
                        "senha123",
                        "01/01/1990",
                        "2",
                        "lucas@example.com",
                        "senha123",
                        "2",
                        "Familia",
                        "01/01/1990",
                        "2",
                        "Amigos",
                        "01/01/1990",
                        "2",
                        "1",
                        "1",
                        "2",
                        "1",
                        "2",
                        "2",
                        "1",
                        "1",
                        "300",
                        "6",
                        "1",
                        "2",
                        "3",
                        "",
                        "6",
                        "6",
                        "0",
                        "");
        InputStream anterior = System.in;
        PrintStream saida = System.out;
        ByteArrayOutputStream captura = new ByteArrayOutputStream();
        try (Database db = new Database("jdbc:sqlite:" + arquivo);
                ServidorRest api = new ServidorRest(db, 0)) {
            api.iniciar();
            System.setIn(new ByteArrayInputStream(comandos.getBytes(StandardCharsets.UTF_8)));
            System.setOut(new PrintStream(captura, true, StandardCharsets.UTF_8));
            App.main(new String[] {Integer.toString(api.porta())});
        } finally {
            System.setIn(anterior);
            System.setOut(saida);
        }
        String texto = captura.toString(StandardCharsets.UTF_8);
        assertTrue(texto.contains("A conta já possui três perfis"), texto);
        assertTrue(texto.contains("Nenhum conteúdo encontrado"), texto);
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + arquivo);
                Statement s = c.createStatement()) {
            try (ResultSet r =
                    s.executeQuery(
                            "SELECT (SELECT COUNT(*) FROM perfis), (SELECT COUNT(*) FROM"
                                    + " favoritos), (SELECT progresso FROM visualizacoes), (SELECT"
                                    + " senha FROM contas)")) {
                assertTrue(r.next());
                assertEquals(3, r.getInt(1));
                assertEquals(1, r.getInt(2));
                assertEquals(300, r.getInt(3));
                assertNotEquals("senha123", r.getString(4));
            }
        }
    }

    @Test
    void bancoImpedeQuartoPerfil() throws Exception {
        try (Database db = new Database("jdbc:sqlite::memory:");
                Statement s = db.conexao().createStatement()) {
            s.execute("INSERT INTO contas(nome,email,senha) VALUES('A','a@b.com','hash')");
            for (int i = 0; i < 3; i++) {
                s.execute("INSERT INTO perfis(conta_id,nome,data_nascimento) VALUES(1,'P','1990-01-01')");
            }
            assertThrows(
                    SQLException.class,
                    () -> s.execute("INSERT INTO perfis(conta_id,nome,data_nascimento) VALUES(1,'Extra','1990-01-01')"));
        }
    }
    @Test
    void bancoExigeDataDeNascimentoNoPerfil() throws Exception {
        try (Database db = new Database("jdbc:sqlite::memory:");
                Statement s = db.conexao().createStatement()) {
            s.execute("INSERT INTO contas(nome,email,senha) VALUES('A','a@b.com','hash')");
            assertThrows(SQLException.class,
                    () -> s.execute("INSERT INTO perfis(conta_id,nome) VALUES(1,'P')"));
        }
    }
}
