package br.infnet.infnetflix.view;

import br.infnet.infnetflix.controller.InfnetflixController;
import br.infnet.infnetflix.model.*;

import java.io.Console;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Menus, entrada e apresentação; as regras ficam no controlador. */
public final class ConsoleView {
    private static final Logger LOG = Logger.getLogger(ConsoleView.class.getName());
    private final InfnetflixController controller;
    private final Console console = System.console();
    private final Scanner input = new Scanner(System.in);

    public ConsoleView(InfnetflixController controller) {
        this.controller = controller;
    }

    private String ler(String prompt) {
        if (console != null) {
            String linha = console.readLine("%s", prompt);
            if (linha == null) {
                throw new FimEntrada();
            }
            return linha.trim();
        }
        System.out.print(prompt);
        if (!input.hasNextLine()) {
            throw new FimEntrada();
        }
        return input.nextLine().trim();
    }

    private String senha() {
        if (console == null) {
            return ler("Senha: ");
        }
        char[] valor = console.readPassword("Senha: ");
        if (valor == null) {
            throw new FimEntrada();
        }
        try {
            return new String(valor);
        } finally {
            Arrays.fill(valor, '\0');
        }
    }

    private int numero(String prompt) {
        try {
            return Integer.parseInt(ler(prompt));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Informe um número válido.");
        }
    }

    private boolean confirmar(String prompt) {
        return ler(prompt).equalsIgnoreCase("s");
    }

    private LocalDate dataNascimento() {
        try {
            return LocalDate.parse(ler("Data de nascimento (dd/MM/aaaa): "),
                    DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Data de nascimento inválida. Use dd/MM/aaaa.");
        }
    }

    public void iniciar() {
        System.out.println("=== INFNETFLIX ===");
        try {
            while (true) {
                try {
                    if (!controller.autenticado()) {
                        menuInicial();
                    } else if (!controller.perfilSelecionado()) {
                        menuConta();
                    } else {
                        menuPerfil();
                    }
                } catch (IllegalArgumentException | IllegalStateException e) {
                    System.out.println("Aviso: " + e.getMessage());
                } catch (SQLException e) {
                    LOG.log(Level.WARNING, "Falha na operação de banco de dados", e);
                    System.out.println("Não foi possível concluir a operação.");
                }
            }
        } catch (FimEntrada e) {
            System.out.println("\nAté logo!");
        }
    }

    private void menuInicial() throws SQLException {
        System.out.println("\n1. Cadastrar | 2. Entrar | 0. Sair");
        switch (ler("> ")) {
            case "1" -> {
                controller.cadastrar(ler("Nome: "), ler("E-mail: "), senha(), dataNascimento());
                System.out.println("Conta criada com perfil inicial. Faça login para continuar.");
            }
            case "2" -> {
                boolean entrou = controller.entrar(ler("E-mail: "), senha());
                System.out.println(entrou ? "Login realizado." : "E-mail ou senha inválidos.");
            }
            case "0" -> throw new FimEntrada();
            default -> System.out.println("Opção inválida.");
        }
    }

    private void listarPerfis() throws SQLException {
        for (Perfil perfil : controller.perfis()) {
            System.out.printf("%d - %s%n", perfil.id(), perfil.nome());
        }
    }

    private void menuConta() throws SQLException {
        System.out.println(
                "\n"
                        + "1. Selecionar perfil | 2. Criar perfil | 3. Editar perfil | 4. Excluir"
                        + " perfil | 5. Plano | 6. Sair da conta");
        switch (ler("> ")) {
            case "1" -> {
                listarPerfis();
                int id = numero("ID do perfil: ");
                controller.selecionarPerfil(id);
            }
            case "2" -> {
                controller.verificarDisponibilidadePerfil();
                controller.criarPerfil(ler("Nome do perfil: "), dataNascimento());
                System.out.println("Perfil criado.");
            }
            case "3" -> {
                listarPerfis();
                int id = numero("ID do perfil: ");
                controller.renomearPerfil(id, ler("Novo nome: "));
                System.out.println("Perfil atualizado.");
            }
            case "4" -> {
                listarPerfis();
                int id = numero("ID do perfil: ");
                controller.validarExclusaoPerfil(id);
                if (confirmar("Excluir perfil e seu histórico e favoritos? (s/n): ")) {
                    controller.excluirPerfil(id);
                    System.out.println("Perfil excluído.");
                }
            }
            case "5" -> plano();
            case "6" -> {
                controller.sair();
                System.out.println("Sessão encerrada.");
            }
            default -> System.out.println("Opção inválida.");
        }
    }

    private void plano() throws SQLException {
        if (controller.premium()) {
            System.out.println("Plano atual: PREMIUM (simulado).");
            return;
        }
        System.out.println(
                "Plano atual: GRATUITO. Premium simulado: sem anúncios e sem limite de duração por"
                        + " sessão.");
        if (confirmar("Ativar Premium de demonstração, sem cobrança? (s/n): ")) {
            controller.ativarPremium();
            System.out.println("Premium de demonstração ativado.");
        }
    }

    private void menuPerfil() throws SQLException {
        System.out.println(
                "\n"
                    + "1. Catálogo e busca | 2. Detalhes e reprodução | 3. Favoritos | 4. Histórico"
                    + " | 5. Recomendações | 6. Gerenciar perfil");
        switch (ler("> ")) {
            case "1" -> catalogo();
            case "2" -> detalhes();
            case "3" -> favoritos();
            case "4" -> historico();
            case "5" -> recomendacoes();
            case "6" -> controller.trocarPerfil();
            default -> System.out.println("Opção inválida.");
        }
    }

    private void mostrarConteudos(List<Conteudo> itens) {
        if (itens.isEmpty()) {
            System.out.println("Nenhum conteúdo encontrado.");
        }
        for (Conteudo conteudo : itens) {
            System.out.printf(
                    "%d - %s | %s | %s | %s%n",
                    conteudo.id(),
                    conteudo.titulo(),
                    conteudo.tipo(),
                    conteudo.genero(),
                    conteudo.classificacao());
        }
    }

    private void catalogo() throws SQLException {
        String titulo = ler("Título (vazio para todos): ");
        String tipo = ler("Tipo FILME/SERIE (vazio para todos): ").toUpperCase(Locale.ROOT);
        String genero = ler("Gênero (vazio para todos): ");
        String classificacao = ler("Classificação (vazio para todas): ");
        FiltroCatalogo.Ordem ordem =
                switch (ler("Ordenar por 1. título ou 2. gênero: ")) {
                    case "", "1" -> FiltroCatalogo.Ordem.TITULO;
                    case "2" -> FiltroCatalogo.Ordem.GENERO;
                    default -> throw new IllegalArgumentException("Ordenação inválida.");
                };
        mostrarConteudos(
                controller.buscar(new FiltroCatalogo(titulo, tipo, genero, classificacao, ordem)));
    }

    private void detalhes() throws SQLException {
        Conteudo c = controller.conteudo(numero("ID do conteúdo: "));
        System.out.printf(
                "%s | %s | %s | %s%n%s%n",
                c.titulo(), c.tipo(), c.genero(), c.classificacao(), c.sinopse());
        if (c instanceof Filme filme) {
            System.out.println("Duração: " + filme.duracao() + " segundos.");
        } else {
            for (Episodio episodio : controller.episodios(c.id())) {
                System.out.printf(
                        "Episódio ID %d: T%d E%d - %s (%d s)%n",
                        episodio.id(),
                        episodio.temporada(),
                        episodio.numero(),
                        episodio.titulo(),
                        episodio.duracao());
            }
        }
        System.out.println(
                "1. Reproduzir | 2. Adicionar favorito | 3. Remover favorito | 0. Voltar");
        switch (ler("> ")) {
            case "1" -> reproduzir(c);
            case "2" ->
                    System.out.println(
                            controller.adicionarFavorito(c.id())
                                    ? "Adicionado aos favoritos."
                                    : "Já está nos favoritos.");
            case "3" ->
                    System.out.println(
                            controller.removerFavorito(c.id())
                                    ? "Removido dos favoritos."
                                    : "Não estava nos favoritos.");
            case "0" -> {}
            default -> System.out.println("Opção inválida.");
        }
    }

    private void reproduzir(Conteudo conteudo) throws SQLException {
        int episodio = conteudo.tipo().equals("SERIE") ? numero("ID do episódio: ") : 0;
        Reproducao r = controller.prepararReproducao(conteudo.id(), episodio);
        boolean reiniciar = false;
        if (r.progresso() > 0) {
            System.out.printf("Progresso salvo: %d de %d segundos.%n", r.progresso(), r.duracao());
            reiniciar = confirmar("Começar do início? (s/n): ");
        }
        int inicio = reiniciar ? 0 : r.progresso();
        if (!r.premium()) {
            System.out.println(
                    "[Anúncio de demonstração] Plano gratuito: até 20 minutos por sessão.");
        }
        System.out.printf(
                "Reprodução simulada no segundo %d; você pode avançar de 0 a %d segundos.%n",
                inicio, r.limite(inicio));
        int progresso =
                controller.registrarReproducao(
                        r, reiniciar, numero("Segundos assistidos até pausar: "));
        System.out.println(
                progresso == r.duracao()
                        ? "Conteúdo concluído."
                        : "Reprodução pausada em " + progresso + " segundos.");
    }

    private void favoritos() throws SQLException {
        List<Conteudo> itens = controller.favoritos();
        if (itens.isEmpty()) {
            System.out.println("Nenhum conteúdo encontrado.");
            return;
        }
        mostrarConteudos(itens);
        String escolha = ler("ID para remover, 'editar' para substituir um favorito ou Enter para voltar: ");
        if (escolha.equalsIgnoreCase("editar")) {
            int id = numero("ID do conteúdo favorito atual: ");
            int novoId = numero("ID do novo conteúdo: ");
            System.out.println(controller.editarFavorito(id, novoId)
                    ? "Favorito atualizado." : "Favorito não encontrado.");
            return;
        }
        if (!escolha.isEmpty()) {
            int id;
            try {
                id = Integer.parseInt(escolha);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("ID inválido.");
            }
            System.out.println(
                    controller.removerFavorito(id)
                            ? "Favorito removido."
                            : "Favorito não encontrado.");
        }
    }

    private void historico() throws SQLException {
        List<Visualizacao> itens = controller.historico();
        if (itens.isEmpty()) {
            System.out.println("Histórico vazio.");
        }
        for (Visualizacao v : itens) {
            String episodio =
                    v.episodio() == null
                            ? ""
                            : " - T" + v.temporada() + " E" + v.numero() + " " + v.episodio();
            System.out.printf(
                    "%s%s - %d s (UTC: %s)%n",
                    v.titulo(), episodio, v.progresso(), v.atualizadoEm());
        }
    }

    private void recomendacoes() throws SQLException {
        System.out.println(
                "Sugestões por gêneros mais assistidos, excluindo títulos já iniciados:");
        mostrarConteudos(controller.recomendacoes());
    }

    private static final class FimEntrada extends RuntimeException {}
}
