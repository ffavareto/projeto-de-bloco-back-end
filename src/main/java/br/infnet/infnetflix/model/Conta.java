package br.infnet.infnetflix.model;

import br.infnet.infnetflix.security.Senhas;
import java.time.LocalDate;
import java.util.*;

public final class Conta {
    private final int id;
    private final String nome;
    private final String email;
    private final String senhaHash;
    private final List<Perfil> perfis;
    private final Plano plano;

    public Conta(int id, String nome, String email, String senhaHash, List<Perfil> perfis, Plano plano) {
        if (perfis.isEmpty() || perfis.size() > 3 || perfis.stream().anyMatch(p -> p.contaId() != id)) {
            throw new IllegalArgumentException("A conta deve possuir de um a três perfis próprios.");
        }
        this.id = id;
        this.nome = Validacao.obrigatorio(nome, "Nome");
        this.email = Validacao.obrigatorio(email, "E-mail");
        this.senhaHash = Objects.requireNonNull(senhaHash);
        this.perfis = new ArrayList<>(perfis);
        this.plano = Objects.requireNonNull(plano);
        this.perfis.forEach(p -> p.usarPlano(plano));
    }

    public int id() { return id; }
    public String nome() { return nome; }
    public String email() { return email; }
    public String senhaHash() { return senhaHash; }
    public List<Perfil> perfis() { return List.copyOf(perfis); }
    public Plano plano() { return plano; }

    public void verificarDisponibilidadePerfil() {
        if (perfis.size() >= 3) throw new IllegalArgumentException("A conta já possui três perfis.");
    }

    public Perfil criarPerfil(String nome, LocalDate dataNascimento) {
        verificarDisponibilidadePerfil();
        Validacao.dataNascimento(dataNascimento);
        // ID zero identifica um objeto novo, antes de receber a identidade do SQLite.
        Perfil perfil = new Perfil(0, id, nome, dataNascimento);
        perfil.usarPlano(plano);
        perfis.add(perfil);
        return perfil;
    }

    public Perfil obterPerfil(int perfilId) {
        return perfis.stream().filter(p -> p.id() == perfilId).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Perfil não encontrado."));
    }

    public Perfil selecionarPerfil(int perfilId) {
        return obterPerfil(perfilId);
    }

    public void editarPerfil(int perfilId, String nome) { obterPerfil(perfilId).renomear(nome); }

    public void validarExclusaoPerfil(int perfilId) {
        obterPerfil(perfilId);
        if (perfis.size() <= 1) throw new IllegalArgumentException("A conta precisa manter pelo menos um perfil.");
    }

    public void excluirPerfil(int perfilId) {
        validarExclusaoPerfil(perfilId);
        perfis.remove(obterPerfil(perfilId));
    }

    public void assinarPremium() { plano.ativarPremium(); }
    public boolean validarSenha(String senha) { return senha != null && Senhas.verificar(senha, senhaHash); }
}
