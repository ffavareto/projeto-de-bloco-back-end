package br.infnet.infnetflix.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Inicialização do banco local e de um pequeno catálogo demonstrativo. */
public final class Database implements AutoCloseable {
    final Connection connection;

    public Database(String url) throws SQLException {
        connection = DriverManager.getConnection(url);

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");

            statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS contas (
                        id INTEGER PRIMARY KEY,
                        nome TEXT NOT NULL,
                        email TEXT NOT NULL COLLATE NOCASE UNIQUE,
                        senha TEXT NOT NULL,
                        plano TEXT NOT NULL DEFAULT 'GRATUITO'
                    )
                    """);
            statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS perfis (
                        id INTEGER PRIMARY KEY,
                        conta_id INTEGER NOT NULL REFERENCES contas(id) ON DELETE CASCADE,
                        nome TEXT NOT NULL,
                        data_nascimento TEXT NOT NULL
                    )
                    """);
            statement.execute(
                    """
                    CREATE TRIGGER IF NOT EXISTS limite_perfis
                    BEFORE INSERT ON perfis
                    WHEN (SELECT COUNT(*) FROM perfis WHERE conta_id = NEW.conta_id) >= 3
                    BEGIN
                        SELECT RAISE(ABORT, 'Limite de tres perfis');
                    END
                    """);
            statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS conteudos (
                        id INTEGER PRIMARY KEY,
                        titulo TEXT NOT NULL,
                        tipo TEXT NOT NULL,
                        genero TEXT NOT NULL,
                        classificacao TEXT NOT NULL,
                        sinopse TEXT NOT NULL,
                        duracao INTEGER
                    )
                    """);
            statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS episodios (
                        id INTEGER PRIMARY KEY,
                        conteudo_id INTEGER NOT NULL REFERENCES conteudos(id) ON DELETE CASCADE,
                        temporada INTEGER NOT NULL,
                        numero INTEGER NOT NULL,
                        titulo TEXT NOT NULL,
                        duracao INTEGER NOT NULL
                    )
                    """);
            statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS favoritos (
                        perfil_id INTEGER NOT NULL REFERENCES perfis(id) ON DELETE CASCADE,
                        conteudo_id INTEGER NOT NULL REFERENCES conteudos(id),
                        criado_em TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        PRIMARY KEY (perfil_id, conteudo_id)
                    )
                    """);

            // episodio_id = 0 representa um filme; séries são registradas por episódio.
            statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS visualizacoes (
                        perfil_id INTEGER NOT NULL REFERENCES perfis(id) ON DELETE CASCADE,
                        conteudo_id INTEGER NOT NULL REFERENCES conteudos(id),
                        episodio_id INTEGER NOT NULL DEFAULT 0,
                        progresso INTEGER NOT NULL,
                        atualizado_em TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        PRIMARY KEY (perfil_id, conteudo_id, episodio_id)
                    )
                    """);
            // Identidades de gêneros e temporadas, preservando o formato dos bancos anteriores.
            statement.execute("CREATE TABLE IF NOT EXISTS generos (id INTEGER PRIMARY KEY, nome TEXT NOT NULL UNIQUE)");
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS temporadas (
                        id INTEGER PRIMARY KEY,
                        conteudo_id INTEGER NOT NULL REFERENCES conteudos(id) ON DELETE CASCADE,
                        numero INTEGER NOT NULL,
                        UNIQUE (conteudo_id, numero)
                    )
                    """);
            statement.execute("INSERT OR IGNORE INTO generos(nome) SELECT DISTINCT genero FROM conteudos ORDER BY genero");
            statement.execute("INSERT OR IGNORE INTO temporadas(conteudo_id,numero) SELECT DISTINCT conteudo_id,temporada FROM episodios ORDER BY conteudo_id,temporada");
            statement.execute("""
                    CREATE TRIGGER IF NOT EXISTS registrar_genero AFTER INSERT ON conteudos
                    BEGIN INSERT OR IGNORE INTO generos(nome) VALUES(NEW.genero); END
                    """);
            statement.execute("""
                    CREATE TRIGGER IF NOT EXISTS atualizar_genero AFTER UPDATE OF genero ON conteudos
                    BEGIN INSERT OR IGNORE INTO generos(nome) VALUES(NEW.genero); END
                    """);
            statement.execute("""
                    CREATE TRIGGER IF NOT EXISTS registrar_temporada AFTER INSERT ON episodios
                    BEGIN INSERT OR IGNORE INTO temporadas(conteudo_id,numero) VALUES(NEW.conteudo_id,NEW.temporada); END
                    """);
            statement.execute("""
                    CREATE TRIGGER IF NOT EXISTS atualizar_temporada AFTER UPDATE OF conteudo_id,temporada ON episodios
                    BEGIN INSERT OR IGNORE INTO temporadas(conteudo_id,numero) VALUES(NEW.conteudo_id,NEW.temporada); END
                    """);
            statement.execute(
                    """
                    INSERT OR IGNORE INTO conteudos VALUES
                        (1, 'A Última Fronteira', 'FILME', 'Ficção científica', '12 anos',
                         'Uma viagem em busca de um novo lar.', 6300),
                        (2, 'O Mistério do Farol', 'FILME', 'Suspense', '14 anos',
                         'Uma investigadora retorna à cidade natal.', 5700),
                        (3, 'Sabores do Mundo', 'SERIE', 'Documentário', 'Livre',
                         'Uma jornada por cozinhas e culturas.', NULL),
                        (4, 'Além das Estrelas', 'SERIE', 'Ficção científica', '12 anos',
                         'Exploradores descobrem mundos desconhecidos.', NULL),
                        (5, 'Um Dia de Verão', 'FILME', 'Comédia', 'Livre',
                         'Amigos vivem aventuras durante as férias.', 5400),
                        (6, 'Arquivo Oculto', 'FILME', 'Suspense', '16 anos',
                         'Um jornalista descobre segredos antigos.', 6000)
                    """);
            statement.execute(
                    """
                    INSERT OR IGNORE INTO episodios VALUES
                        (1, 3, 1, 1, 'Origens', 1800),
                        (2, 3, 1, 2, 'Encontros', 1900),
                        (3, 4, 1, 1, 'Partida', 2400),
                        (4, 4, 1, 2, 'Novo Mundo', 2500),
                        (5, 4, 2, 1, 'Retorno', 2600)
                    """);
        }
    }

    public Connection conexao() {
        return connection;
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }
}
