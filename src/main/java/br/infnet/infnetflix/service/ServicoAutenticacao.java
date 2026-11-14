package br.infnet.infnetflix.service;

import br.infnet.infnetflix.model.*;
import br.infnet.infnetflix.repository.ContaRepository;
import br.infnet.infnetflix.security.Senhas;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;

public final class ServicoAutenticacao {
    private final ContaRepository contas;

    public ServicoAutenticacao(ContaRepository contas) { this.contas = contas; }

    public Conta cadastrar(String nome, String email, String senha, LocalDate dataNascimento) throws SQLException {
        nome = Validacao.obrigatorio(nome, "Nome");
        email = Validacao.obrigatorio(email, "E-mail").toLowerCase(Locale.ROOT);
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("E-mail inválido.");
        }
        if (senha == null || senha.length() < 8) {
            throw new IllegalArgumentException("A senha deve ter pelo menos 8 caracteres.");
        }
        Validacao.dataNascimento(dataNascimento);
        if (contas.buscarPorEmail(email).isPresent()) throw new IllegalArgumentException("E-mail já cadastrado.");
        contas.cadastrar(nome, email, Senhas.criar(senha), dataNascimento);
        return contas.buscarPorEmail(email).orElseThrow();
    }

    public Optional<Conta> login(String email, String senha) throws SQLException {
        email = Validacao.obrigatorio(email, "E-mail").toLowerCase(Locale.ROOT);
        return contas.buscarPorEmail(email).filter(c -> c.validarSenha(senha));
    }
}
