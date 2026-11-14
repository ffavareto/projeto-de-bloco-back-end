package br.infnet.infnetflix.repository;

import br.infnet.infnetflix.model.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class VisualizacaoRepository extends Repositorio {
    private final CatalogoRepository catalogo;

    public VisualizacaoRepository(Database db) {
        super(db);
        catalogo = new CatalogoRepository(db);
    }

    public void salvar(int perfilId, Visualizacao visualizacao) throws SQLException {
        executar("""
                INSERT INTO visualizacoes (perfil_id, conteudo_id, episodio_id, progresso)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (perfil_id, conteudo_id, episodio_id) DO UPDATE
                SET progresso = excluded.progresso, atualizado_em = CURRENT_TIMESTAMP
                """, perfilId, visualizacao.item().conteudo().id(),
                visualizacao.item().episodioId(), visualizacao.progresso());
    }

    public List<Visualizacao> historico(int perfilId) throws SQLException {
        List<Visualizacao> itens = new ArrayList<>();
        Map<Integer, Conteudo> conteudos = catalogo.listar().stream()
                .collect(Collectors.toMap(Conteudo::id, Function.identity()));
        try (PreparedStatement p = preparar("""
                SELECT rowid AS visualizacao_id, conteudo_id, episodio_id, progresso, atualizado_em
                FROM visualizacoes WHERE perfil_id=? ORDER BY atualizado_em DESC, rowid DESC
                """, perfilId);
                ResultSet r = p.executeQuery()) {
            while (r.next()) {
                Conteudo conteudo = conteudos.get(r.getInt("conteudo_id"));
                itens.add(new Visualizacao(r.getInt("visualizacao_id"),
                        conteudo.obterItem(r.getInt("episodio_id")), r.getInt("progresso"),
                        LocalDateTime.parse(r.getString("atualizado_em"),
                                DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss"))));
            }
        }
        return itens;
    }
}
