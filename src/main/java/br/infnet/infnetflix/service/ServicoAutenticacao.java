package br.infnet.infnetflix.service;

import br.infnet.infnetflix.model.Conta;
import br.infnet.infnetflix.model.Validacao;
import br.infnet.infnetflix.repository.ContaRepository;
import br.infnet.infnetflix.security.Senhas;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;

/** Centraliza as regras de cadastro e autenticação por e-mail e senha. */
public final class ServicoAutenticacao {
    private final ContaRepository contas;

    public ServicoAutenticacao(ContaRepository contas) { this.contas = contas; }

    /** Valida os dados e envia o hash da senha ao repositório para criar a conta. */
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
        if (contas.buscarPorEmail(email).isPresent()) {
            throw new IllegalArgumentException("E-mail já cadastrado.");
        }
        String senhaHash = Senhas.criar(senha);
        contas.cadastrar(nome, email, senhaHash, dataNascimento);
        return contas.buscarPorEmail(email).orElseThrow();
    }

    /** Normaliza o e-mail e retorna a conta apenas quando a senha confere. */
    public Optional<Conta> login(String email, String senha) throws SQLException {
        email = Validacao.obrigatorio(email, "E-mail").toLowerCase(Locale.ROOT);
        Optional<Conta> resultado = contas.buscarPorEmail(email);
        if (resultado.isEmpty()) {
            return Optional.empty();
        }
        Conta conta = resultado.get();
        if (!conta.validarSenha(senha)) {
            return Optional.empty();
        }
        return resultado;
    }
}
