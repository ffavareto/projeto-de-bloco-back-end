package br.infnet.infnetflix.repository;

import br.infnet.infnetflix.model.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/** Carrega filmes e séries do SQLite e persiste os favoritos de cada perfil. */
public final class CatalogoRepository {
    private static final String CONTEUDOS = """
            SELECT c.*, g.id AS genero_id
            FROM conteudos c JOIN generos g ON g.nome = c.genero
            """;
    private final Database db;

    public CatalogoRepository(Database db) {
        this.db = db;
    }

    private Conteudo conteudo(ResultSet r) throws SQLException {
        Genero genero = new Genero(r.getInt("genero_id"), r.getString("genero"));
        switch (r.getString("tipo")) {
            case "FILME":
                return new Filme(r.getInt("id"), r.getString("titulo"), genero,
                        r.getString("classificacao"), r.getString("sinopse"), r.getInt("duracao"));
            case "SERIE":
                Serie serie = new Serie(r.getInt("id"), r.getString("titulo"), genero,
                        r.getString("classificacao"), r.getString("sinopse"));
                carregarTemporadas(serie);
                return serie;
            default:
                throw new SQLException("Tipo de conteúdo desconhecido.");
        }
    }

    public Catalogo carregar() throws SQLException {
        return new Catalogo(listar());
    }

    public List<Conteudo> listar() throws SQLException {
        List<Conteudo> itens = new ArrayList<>();
        try (PreparedStatement p = db.preparar(CONTEUDOS + " ORDER BY c.id");
                ResultSet r = p.executeQuery()) {
            while (r.next()) {
                itens.add(conteudo(r));
            }
        }
        return itens;
    }

    public Optional<Conteudo> buscarPorId(int id) throws SQLException {
        try (PreparedStatement p = db.preparar(CONTEUDOS + " WHERE c.id=?", id);
                ResultSet r = p.executeQuery()) {
            if (r.next()) {
                return Optional.of(conteudo(r));
            }
            return Optional.empty();
        }
    }

    /** Agrupa os episódios pelo ID de temporada persistido no banco. */
    private void carregarTemporadas(Serie serie) throws SQLException {
        Map<Integer, List<Episodio>> episodiosPorTemporada = new LinkedHashMap<>();
        try (PreparedStatement p = db.preparar("""
                SELECT e.*, t.id AS temporada_id
                FROM episodios e JOIN temporadas t
                  ON t.conteudo_id=e.conteudo_id AND t.numero=e.temporada
                WHERE e.conteudo_id=? ORDER BY e.temporada,e.numero
                """, serie.id());
                ResultSet r = p.executeQuery()) {
            while (r.next()) {
                int temporadaId = r.getInt("temporada_id");
                List<Episodio> episodios = episodiosPorTemporada.get(temporadaId);
                if (episodios == null) {
                    episodios = new ArrayList<>();
                    episodiosPorTemporada.put(temporadaId, episodios);
                }

                Episodio episodio = new Episodio(r.getInt("id"), serie, r.getInt("temporada"),
                        r.getInt("numero"), r.getString("titulo"), r.getInt("duracao"));
                episodios.add(episodio);
            }
        }

        for (int temporadaId : episodiosPorTemporada.keySet()) {
            List<Episodio> episodios = episodiosPorTemporada.get(temporadaId);
            int numero = episodios.get(0).temporada();
            serie.adicionarTemporada(new Temporada(temporadaId, numero, episodios));
        }
    }

    /** Carrega os favoritos do perfil em ordem decrescente de inclusão. */
    public List<Favorito> favoritos(int perfilId) throws SQLException {
        List<Favorito> favoritos = new ArrayList<>();
        try (PreparedStatement p = db.preparar("""
                SELECT c.*, g.id AS genero_id, f.rowid AS favorito_id, f.criado_em
                FROM favoritos f JOIN conteudos c ON c.id=f.conteudo_id
                JOIN generos g ON g.nome=c.genero
                WHERE f.perfil_id=? ORDER BY f.criado_em DESC, f.rowid DESC
                """, perfilId);
                ResultSet r = p.executeQuery()) {
            while (r.next()) {
                int id = r.getInt("favorito_id");
                Conteudo conteudoFavoritado = conteudo(r);
                LocalDate data = LocalDate.parse(r.getString("criado_em").substring(0, 10));
                favoritos.add(new Favorito(id, conteudoFavoritado, data));
            }
        }
        return favoritos;
    }

    public boolean adicionarFavorito(int perfilId, int conteudoId) throws SQLException {
        return db.executar("INSERT OR IGNORE INTO favoritos(perfil_id,conteudo_id) VALUES(?,?)",
                perfilId, conteudoId) > 0;
    }

    public boolean removerFavorito(int perfilId, int conteudoId) throws SQLException {
        return db.executar("DELETE FROM favoritos WHERE perfil_id=? AND conteudo_id=?", perfilId, conteudoId) > 0;
    }

    /** Troca o título na mesma linha para preservar a identidade e a data de inclusão. */
    public boolean editarFavorito(int perfilId, int conteudoId, int novoConteudoId) throws SQLException {
        return db.executar("UPDATE favoritos SET conteudo_id=? WHERE perfil_id=? AND conteudo_id=?",
                novoConteudoId, perfilId, conteudoId) > 0;
    }
}
