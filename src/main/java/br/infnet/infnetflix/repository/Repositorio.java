package br.infnet.infnetflix.repository;

import java.sql.*;

abstract class Repositorio {
    protected final Connection conexao;

    Repositorio(Database db) {
        conexao = db.conexao();
    }

    protected PreparedStatement preparar(String sql, Object... parametros) throws SQLException {
        PreparedStatement p = conexao.prepareStatement(sql);
        try {
            for (int i = 0; i < parametros.length; i++) {
                p.setObject(i + 1, parametros[i]);
            }
            return p;
        } catch (SQLException e) {
            p.close();
            throw e;
        }
    }

    protected int executar(String sql, Object... parametros) throws SQLException {
        try (PreparedStatement p = preparar(sql, parametros)) {
            return p.executeUpdate();
        }
    }
}
