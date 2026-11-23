package br.infnet.infnetflix.repository;

import br.infnet.infnetflix.model.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public final class ContaRepository {
    private final Database db;

    public ContaRepository(Database db) {
        this.db = db;
    }

    public Optional<Conta> buscarPorEmail(String email) throws SQLException {
        return buscar("SELECT * FROM contas WHERE email=?", email);
    }

    public Optional<Conta> buscarPorId(int id) throws SQLException {
        return buscar("SELECT * FROM contas WHERE id=?", id);
    }

    private Optional<Conta> buscar(String sql, Object parametro) throws SQLException {
        try (PreparedStatement p = db.preparar(sql, parametro);
                ResultSet r = p.executeQuery()) {
            if (r.next()) {
                return Optional.of(conta(r));
            }
            return Optional.empty();
        }
    }

    private Conta conta(ResultSet r) throws SQLException {
        int id = r.getInt("id");
        String nome = r.getString("nome");
        String email = r.getString("email");
        String senhaHash = r.getString("senha");
        List<Perfil> perfisDaConta = perfis(id);
        TipoPlano tipoPlano = TipoPlano.valueOf(r.getString("plano"));
        Plano plano = new Plano(id, tipoPlano);
        return new Conta(id, nome, email, senhaHash, perfisDaConta, plano);
    }

    public void cadastrar(String nome, String email, String senhaHash, LocalDate dataNascimento) throws SQLException {
        Connection conexao = db.conexao();
        conexao.setAutoCommit(false);
        try {
            db.executar("INSERT INTO contas(nome,email,senha) VALUES(?,?,?)", nome, email, senhaHash);
            db.executar(
                    "INSERT INTO perfis(conta_id,nome,data_nascimento) SELECT id,nome,? FROM contas WHERE email=?",
                    dataNascimento.toString(),
                    email);
            conexao.commit();
        } catch (SQLException | RuntimeException e) {
            conexao.rollback();
            throw e;
        } finally {
            conexao.setAutoCommit(true);
        }
    }

    private List<Perfil> perfis(int contaId) throws SQLException {
        List<Perfil> perfis = new ArrayList<>();
        try (PreparedStatement p = db.preparar(
                "SELECT id,conta_id,nome,data_nascimento FROM perfis WHERE conta_id=? ORDER BY id", contaId);
                ResultSet r = p.executeQuery()) {
            while (r.next()) {
                String data = r.getString("data_nascimento");
                perfis.add(new Perfil(r.getInt("id"), r.getInt("conta_id"), r.getString("nome"),
                        LocalDate.parse(data)));
            }
        }
        return perfis;
    }

    public void criarPerfil(int contaId, String nome, LocalDate dataNascimento) throws SQLException {
        db.executar("INSERT INTO perfis(conta_id,nome,data_nascimento) VALUES(?,?,?)", contaId, nome, dataNascimento.toString());
    }

    public int renomearPerfil(int contaId, int perfilId, String nome) throws SQLException {
        return db.executar(
                "UPDATE perfis SET nome=? WHERE id=? AND conta_id=?", nome, perfilId, contaId);
    }

    public int excluirPerfil(int contaId, int perfilId) throws SQLException {
        return db.executar("DELETE FROM perfis WHERE id=? AND conta_id=?", perfilId, contaId);
    }

    public void salvarPlano(Conta conta) throws SQLException {
        db.executar("UPDATE contas SET plano=? WHERE id=?", conta.plano().tipo().name(), conta.id());
    }
}
