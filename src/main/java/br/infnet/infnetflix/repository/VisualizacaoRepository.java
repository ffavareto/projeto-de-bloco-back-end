package br.infnet.infnetflix.repository;

import br.infnet.infnetflix.model.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class VisualizacaoRepository {
    private final Database db;
    private final CatalogoRepository catalogo;

    public VisualizacaoRepository(Database db) {
        this.db = db;
        catalogo = new CatalogoRepository(db);
    }

    public void salvar(int perfilId, Visualizacao visualizacao) throws SQLException {
        db.executar("""
                INSERT INTO visualizacoes (perfil_id, conteudo_id, episodio_id, progresso)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (perfil_id, conteudo_id, episodio_id) DO UPDATE
                SET progresso = excluded.progresso, atualizado_em = CURRENT_TIMESTAMP
                """, perfilId, visualizacao.item().conteudo().id(),
                visualizacao.item().episodioId(), visualizacao.progresso());
    }

    public List<Visualizacao> historico(int perfilId) throws SQLException {
        List<Visualizacao> itens = new ArrayList<>();
        Map<Integer, Conteudo> conteudosPorId = new HashMap<>();
        for (Conteudo conteudo : catalogo.listar()) {
            conteudosPorId.put(conteudo.id(), conteudo);
        }

        DateTimeFormatter formatoData = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
        try (PreparedStatement p = db.preparar("""
                SELECT rowid AS visualizacao_id, conteudo_id, episodio_id, progresso, atualizado_em
                FROM visualizacoes WHERE perfil_id=? ORDER BY atualizado_em DESC, rowid DESC
                """, perfilId);
                ResultSet r = p.executeQuery()) {
            while (r.next()) {
                Conteudo conteudo = conteudosPorId.get(r.getInt("conteudo_id"));
                int id = r.getInt("visualizacao_id");
                Reproduzivel item = conteudo.obterItem(r.getInt("episodio_id"));
                int progresso = r.getInt("progresso");
                LocalDateTime atualizadoEm = LocalDateTime.parse(r.getString("atualizado_em"), formatoData);
                itens.add(new Visualizacao(id, item, progresso, atualizadoEm));
            }
        }
        return itens;
    }
}
