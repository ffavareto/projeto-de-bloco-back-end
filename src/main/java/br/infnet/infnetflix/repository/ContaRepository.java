package br.infnet.infnetflix.repository;

import br.infnet.infnetflix.model.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public final class ContaRepository extends Repositorio {
    public ContaRepository(Database db) {
        super(db);
    }

    public Optional<Conta> buscarPorEmail(String email) throws SQLException {
        try (PreparedStatement p = preparar("SELECT * FROM contas WHERE email=?", email);
                ResultSet r = p.executeQuery()) {
            return r.next() ? Optional.of(conta(r)) : Optional.empty();
        }
    }

    public Optional<Conta> buscarPorId(int id) throws SQLException {
        try (PreparedStatement p = preparar("SELECT * FROM contas WHERE id=?", id);
                ResultSet r = p.executeQuery()) {
            return r.next() ? Optional.of(conta(r)) : Optional.empty();
        }
    }

    private Conta conta(ResultSet r) throws SQLException {
        int id = r.getInt("id");
        return new Conta(id, r.getString("nome"), r.getString("email"), r.getString("senha"),
                perfis(id), new Plano(id, TipoPlano.valueOf(r.getString("plano"))));
    }

    public void cadastrar(String nome, String email, String senhaHash, LocalDate dataNascimento) throws SQLException {
        conexao.setAutoCommit(false);
        try {
            executar("INSERT INTO contas(nome,email,senha) VALUES(?,?,?)", nome, email, senhaHash);
            executar(
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

    public List<Perfil> perfis(int contaId) throws SQLException {
        List<Perfil> perfis = new ArrayList<>();
        try (PreparedStatement p =
                        preparar(
                                "SELECT id,conta_id,nome,data_nascimento FROM perfis WHERE conta_id=? ORDER BY id",
                                contaId);
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
        executar("INSERT INTO perfis(conta_id,nome,data_nascimento) VALUES(?,?,?)", contaId, nome, dataNascimento.toString());
    }

    public int renomearPerfil(int contaId, int perfilId, String nome) throws SQLException {
        return executar(
                "UPDATE perfis SET nome=? WHERE id=? AND conta_id=?", nome, perfilId, contaId);
    }

    public int excluirPerfil(int contaId, int perfilId) throws SQLException {
        return executar("DELETE FROM perfis WHERE id=? AND conta_id=?", perfilId, contaId);
    }

    public void salvarPlano(Conta conta) throws SQLException {
        executar("UPDATE contas SET plano=? WHERE id=?", conta.plano().tipo().name(), conta.id());
    }
}
