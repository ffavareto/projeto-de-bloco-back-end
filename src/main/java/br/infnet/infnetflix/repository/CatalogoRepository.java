package br.infnet.infnetflix.repository;

import br.infnet.infnetflix.model.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public final class CatalogoRepository extends Repositorio {
    private static final String CONTEUDOS = """
            SELECT c.*, g.id AS genero_id
            FROM conteudos c JOIN generos g ON g.nome = c.genero
            """;

    public CatalogoRepository(Database db) { super(db); }

    private Conteudo conteudo(ResultSet r) throws SQLException {
        Genero genero = new Genero(r.getInt("genero_id"), r.getString("genero"));
        return switch (r.getString("tipo")) {
            case "FILME" -> new Filme(r.getInt("id"), r.getString("titulo"), genero,
                    r.getString("classificacao"), r.getString("sinopse"), r.getInt("duracao"));
            case "SERIE" -> {
                Serie serie = new Serie(r.getInt("id"), r.getString("titulo"), genero,
                        r.getString("classificacao"), r.getString("sinopse"));
                carregarTemporadas(serie);
                yield serie;
            }
            default -> throw new SQLException("Tipo de conteúdo desconhecido.");
        };
    }

    public Catalogo carregar() throws SQLException { return new Catalogo(listar()); }

    public List<Conteudo> listar() throws SQLException {
        List<Conteudo> itens = new ArrayList<>();
        try (PreparedStatement p = preparar(CONTEUDOS + " ORDER BY c.id");
                ResultSet r = p.executeQuery()) {
            while (r.next()) itens.add(conteudo(r));
        }
        return itens;
    }

    public Optional<Conteudo> buscarPorId(int id) throws SQLException {
        try (PreparedStatement p = preparar(CONTEUDOS + " WHERE c.id=?", id);
                ResultSet r = p.executeQuery()) {
            return r.next() ? Optional.of(conteudo(r)) : Optional.empty();
        }
    }

    private void carregarTemporadas(Serie serie) throws SQLException {
        Map<Integer, List<Episodio>> temporadas = new LinkedHashMap<>();
        try (PreparedStatement p = preparar("""
                SELECT e.*, t.id AS temporada_id
                FROM episodios e JOIN temporadas t
                  ON t.conteudo_id=e.conteudo_id AND t.numero=e.temporada
                WHERE e.conteudo_id=? ORDER BY e.temporada,e.numero
                """, serie.id());
                ResultSet r = p.executeQuery()) {
            while (r.next()) {
                temporadas.computeIfAbsent(r.getInt("temporada_id"), id -> new ArrayList<>())
                        .add(new Episodio(r.getInt("id"), serie, r.getInt("temporada"),
                                r.getInt("numero"), r.getString("titulo"), r.getInt("duracao")));
            }
        }
        temporadas.forEach((id, episodios) ->
                serie.adicionarTemporada(new Temporada(id, episodios.get(0).temporada(), episodios)));
    }

    public List<Favorito> favoritos(int perfilId) throws SQLException {
        List<Favorito> favoritos = new ArrayList<>();
        try (PreparedStatement p = preparar("""
                SELECT c.*, g.id AS genero_id, f.rowid AS favorito_id, f.criado_em
                FROM favoritos f JOIN conteudos c ON c.id=f.conteudo_id
                JOIN generos g ON g.nome=c.genero
                WHERE f.perfil_id=? ORDER BY f.criado_em DESC, f.rowid DESC
                """, perfilId);
                ResultSet r = p.executeQuery()) {
            while (r.next()) {
                favoritos.add(new Favorito(r.getInt("favorito_id"), conteudo(r),
                        LocalDate.parse(r.getString("criado_em").substring(0, 10))));
            }
        }
        return favoritos;
    }

    public boolean adicionarFavorito(int perfilId, int conteudoId) throws SQLException {
        return executar("INSERT OR IGNORE INTO favoritos(perfil_id,conteudo_id) VALUES(?,?)",
                perfilId, conteudoId) > 0;
    }

    public boolean removerFavorito(int perfilId, int conteudoId) throws SQLException {
        return executar("DELETE FROM favoritos WHERE perfil_id=? AND conteudo_id=?", perfilId, conteudoId) > 0;
    }

    public boolean editarFavorito(int perfilId, int conteudoId, int novoConteudoId) throws SQLException {
        return executar("UPDATE favoritos SET conteudo_id=? WHERE perfil_id=? AND conteudo_id=?",
                novoConteudoId, perfilId, conteudoId) > 0;
    }
}
