package br.infnet.infnetflix.view;

import br.infnet.infnetflix.controller.InfnetflixController;
import br.infnet.infnetflix.model.Conteudo;
import br.infnet.infnetflix.model.Episodio;
import br.infnet.infnetflix.model.FiltroCatalogo;
import br.infnet.infnetflix.model.Filme;
import br.infnet.infnetflix.model.Perfil;
import br.infnet.infnetflix.model.Reproducao;
import br.infnet.infnetflix.model.Serie;
import br.infnet.infnetflix.model.Visualizacao;

import java.io.Console;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Mostra os menus e encaminha as escolhas do usuário ao controlador. */
public final class ConsoleView {
    private static final Logger LOG = Logger.getLogger(ConsoleView.class.getName());
    private final InfnetflixController controller;
    private final Console console = System.console();
    private final Scanner input = new Scanner(System.in);

    public ConsoleView(InfnetflixController controller) {
        this.controller = controller;
    }

    private String ler(String prompt) {
        String linha;
        if (console != null) {
            linha = console.readLine("%s", prompt);
        } else {
            System.out.print(prompt);
            linha = input.hasNextLine() ? input.nextLine() : null;
        }
        if (linha == null) {
            throw new FimEntrada();
        }
        return linha.trim();
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
            String data = ler("Data de nascimento (dd/MM/aaaa): ");
            DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/uuuu")
                    .withResolverStyle(ResolverStyle.STRICT);
            return LocalDate.parse(data, formato);
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
                String nome = ler("Nome: ");
                String email = ler("E-mail: ");
                String senha = senha();
                LocalDate nascimento = dataNascimento();
                controller.cadastrar(nome, email, senha, nascimento);
                System.out.println("Conta criada com perfil inicial. Faça login para continuar.");
            }
            case "2" -> {
                String email = ler("E-mail: ");
                String senha = senha();
                boolean entrou = controller.entrar(email, senha);
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
        System.out.println("\n1. Selecionar perfil | 2. Criar perfil | 3. Editar perfil"
                + " | 4. Excluir perfil | 5. Plano | 6. Sair da conta");
        switch (ler("> ")) {
            case "1" -> {
                listarPerfis();
                int id = numero("ID do perfil: ");
                controller.selecionarPerfil(id);
            }
            case "2" -> {
                controller.verificarDisponibilidadePerfil();
                String nome = ler("Nome do perfil: ");
                LocalDate nascimento = dataNascimento();
                controller.criarPerfil(nome, nascimento);
                System.out.println("Perfil criado.");
            }
            case "3" -> {
                listarPerfis();
                int id = numero("ID do perfil: ");
                String nome = ler("Novo nome: ");
                controller.renomearPerfil(id, nome);
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
        System.out.println("\n1. Catálogo e busca | 2. Detalhes e reprodução | 3. Favoritos"
                + " | 4. Histórico | 5. Recomendações | 6. Gerenciar perfil");
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
        String tipo = ler("Tipo FILME/SERIE (vazio para todos): ");
        String genero = ler("Gênero (vazio para todos): ");
        String classificacao = ler("Classificação (vazio para todas): ");
        FiltroCatalogo.Ordem ordem =
                switch (ler("Ordenar por 1. título ou 2. gênero: ")) {
                    case "", "1" -> FiltroCatalogo.Ordem.TITULO;
                    case "2" -> FiltroCatalogo.Ordem.GENERO;
                    default -> throw new IllegalArgumentException("Ordenação inválida.");
                };
        FiltroCatalogo filtro = new FiltroCatalogo(titulo, tipo, genero, classificacao, ordem);
        mostrarConteudos(controller.buscar(filtro));
    }

    private void detalhes() throws SQLException {
        int id = numero("ID do conteúdo: ");
        Conteudo conteudo = controller.conteudo(id);
        System.out.printf(
                "%s | %s | %s | %s%n%s%n",
                conteudo.titulo(), conteudo.tipo(), conteudo.genero(),
                conteudo.classificacao(), conteudo.sinopse());
        if (conteudo instanceof Filme filme) {
            System.out.println("Duração: " + filme.duracao() + " segundos.");
        } else if (conteudo instanceof Serie serie) {
            for (Episodio episodio : serie.listarEpisodios()) {
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
            case "1" -> reproduzir(conteudo);
            case "2" -> {
                boolean adicionado = controller.adicionarFavorito(conteudo.id());
                System.out.println(adicionado ? "Adicionado aos favoritos." : "Já está nos favoritos.");
            }
            case "3" -> {
                boolean removido = controller.removerFavorito(conteudo.id());
                System.out.println(removido ? "Removido dos favoritos." : "Não estava nos favoritos.");
            }
            case "0" -> {
                return;
            }
            default -> System.out.println("Opção inválida.");
        }
    }

    private void reproduzir(Conteudo conteudo) throws SQLException {
        int idEpisodio = 0;
        if (conteudo instanceof Serie) {
            idEpisodio = numero("ID do episódio: ");
        }
        Reproducao reproducao = controller.prepararReproducao(conteudo.id(), idEpisodio);
        int progressoSalvo = reproducao.progresso();
        int duracao = reproducao.duracao();
        boolean reiniciar = false;
        if (progressoSalvo > 0) {
            System.out.printf("Progresso salvo: %d de %d segundos.%n", progressoSalvo, duracao);
            reiniciar = confirmar("Começar do início? (s/n): ");
        }
        int inicio = reiniciar ? 0 : progressoSalvo;
        if (!reproducao.premium()) {
            System.out.println(
                    "[Anúncio de demonstração] Plano gratuito: até 20 minutos por sessão.");
        }
        System.out.printf(
                "Reprodução simulada no segundo %d; você pode avançar de 0 a %d segundos.%n",
                inicio, reproducao.limite(inicio));
        int segundosAssistidos = numero("Segundos assistidos até pausar: ");
        int progresso = controller.registrarReproducao(
                conteudo.id(), idEpisodio, reiniciar, segundosAssistidos);
        System.out.println(
                progresso == duracao
                        ? "Conteúdo concluído."
                        : "Reprodução pausada em " + progresso + " segundos.");
    }

    private void favoritos() throws SQLException {
        List<Conteudo> itens = controller.favoritos();
        mostrarConteudos(itens);
        if (itens.isEmpty()) {
            return;
        }
        String escolha = ler("ID para remover, 'editar' para substituir um favorito ou Enter para voltar: ");
        if (escolha.isEmpty()) {
            return;
        }
        if (escolha.equalsIgnoreCase("editar")) {
            int id = numero("ID do conteúdo favorito atual: ");
            int novoId = numero("ID do novo conteúdo: ");
            boolean atualizado = controller.editarFavorito(id, novoId);
            System.out.println(atualizado ? "Favorito atualizado." : "Favorito não encontrado.");
            return;
        }
        int id;
        try {
            id = Integer.parseInt(escolha);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ID inválido.");
        }
        boolean removido = controller.removerFavorito(id);
        System.out.println(removido ? "Favorito removido." : "Favorito não encontrado.");
    }

    private void historico() throws SQLException {
        List<Visualizacao> itens = controller.historico();
        if (itens.isEmpty()) {
            System.out.println("Histórico vazio.");
        }
        for (Visualizacao visualizacao : itens) {
            String episodio = "";
            if (visualizacao.episodio() != null) {
                episodio = " - T" + visualizacao.temporada() + " E" + visualizacao.numero()
                        + " " + visualizacao.episodio();
            }
            System.out.printf(
                    "%s%s - %d s (UTC: %s)%n",
                    visualizacao.titulo(), episodio, visualizacao.progresso(), visualizacao.atualizadoEm());
        }
    }

    private void recomendacoes() throws SQLException {
        System.out.println(
                "Sugestões por gêneros mais assistidos, excluindo títulos já iniciados:");
        mostrarConteudos(controller.recomendacoes());
    }

    private static final class FimEntrada extends RuntimeException {}
}
