package br.infnet.infnetflix.view;

import br.infnet.infnetflix.api.ApiDados.ConteudoDetalhes;
import br.infnet.infnetflix.api.ApiDados.EpisodioDetalhes;
import br.infnet.infnetflix.api.ApiDados.PerfilResumo;
import br.infnet.infnetflix.api.ApiDados.RegistroHistorico;
import br.infnet.infnetflix.api.ApiDados.ReproducaoAtual;
import br.infnet.infnetflix.api.InfnetflixClient;

import java.io.Console;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/** Mostra os menus e encaminha as escolhas do usuário pelo cliente HTTP. */
public final class ConsoleView {
    private final InfnetflixClient api;
    private final Console console = System.console();
    private final Scanner input = new Scanner(System.in);

    public ConsoleView(InfnetflixClient api) {
        this.api = api;
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

    /** Oculta a digitação no terminal interativo e aceita entrada redirecionada. */
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

    /** Exibe o menu correspondente à sessão até a opção de sair ou o fim da entrada. */
    public void iniciar() {
        System.out.println("=== INFNETFLIX ===");
        try {
            while (true) {
                try {
                    if (!api.autenticado()) {
                        menuInicial();
                    } else if (!api.perfilSelecionado()) {
                        menuConta();
                    } else {
                        menuPerfil();
                    }
                } catch (IllegalArgumentException | IllegalStateException e) {
                    System.out.println("Aviso: " + e.getMessage());
                }
            }
        } catch (FimEntrada e) {
            System.out.println("\nAté logo!");
        }
    }

    private void menuInicial() {
        System.out.println("\n1. Cadastrar | 2. Entrar | 0. Sair");
        switch (ler("> ")) {
            case "1" -> {
                String nome = ler("Nome: ");
                String email = ler("E-mail: ");
                String senha = senha();
                LocalDate nascimento = dataNascimento();
                api.cadastrar(nome, email, senha, nascimento);
                System.out.println("Conta criada com perfil inicial. Faça login para continuar.");
            }
            case "2" -> {
                String email = ler("E-mail: ");
                String senha = senha();
                boolean entrou = api.entrar(email, senha);
                System.out.println(entrou ? "Login realizado." : "E-mail ou senha inválidos.");
            }
            case "0" -> throw new FimEntrada();
            default -> System.out.println("Opção inválida.");
        }
    }

    private void listarPerfis() {
        for (PerfilResumo perfil : api.perfis()) {
            System.out.printf("%d - %s%n", perfil.id(), perfil.nome());
        }
    }

    private void menuConta() {
        System.out.println("\n1. Selecionar perfil | 2. Criar perfil | 3. Editar perfil"
                + " | 4. Excluir perfil | 5. Plano | 6. Sair da conta");
        switch (ler("> ")) {
            case "1" -> {
                listarPerfis();
                int id = numero("ID do perfil: ");
                api.selecionarPerfil(id);
            }
            case "2" -> {
                api.verificarDisponibilidadePerfil();
                String nome = ler("Nome do perfil: ");
                LocalDate nascimento = dataNascimento();
                api.criarPerfil(nome, nascimento);
                System.out.println("Perfil criado.");
            }
            case "3" -> {
                listarPerfis();
                int id = numero("ID do perfil: ");
                String nome = ler("Novo nome: ");
                api.renomearPerfil(id, nome);
                System.out.println("Perfil atualizado.");
            }
            case "4" -> {
                listarPerfis();
                int id = numero("ID do perfil: ");
                api.validarExclusaoPerfil(id);
                if (confirmar("Excluir perfil e seu histórico e favoritos? (s/n): ")) {
                    api.excluirPerfil(id);
                    System.out.println("Perfil excluído.");
                }
            }
            case "5" -> plano();
            case "6" -> {
                api.sair();
                System.out.println("Sessão encerrada.");
            }
            default -> System.out.println("Opção inválida.");
        }
    }

    private void plano() {
        if (api.premium()) {
            System.out.println("Plano atual: PREMIUM (simulado).");
            return;
        }
        System.out.println(
                "Plano atual: GRATUITO. Premium simulado: sem anúncios e sem limite de duração por"
                        + " sessão.");
        if (confirmar("Ativar Premium de demonstração, sem cobrança? (s/n): ")) {
            api.ativarPremium();
            System.out.println("Premium de demonstração ativado.");
        }
    }

    private void menuPerfil() {
        System.out.println("\n1. Catálogo e busca | 2. Detalhes e reprodução | 3. Favoritos"
                + " | 4. Histórico | 5. Recomendações | 6. Gerenciar perfil");
        switch (ler("> ")) {
            case "1" -> catalogo();
            case "2" -> detalhes();
            case "3" -> favoritos();
            case "4" -> historico();
            case "5" -> recomendacoes();
            case "6" -> api.trocarPerfil();
            default -> System.out.println("Opção inválida.");
        }
    }

    private void mostrarConteudos(List<ConteudoDetalhes> itens) {
        if (itens.isEmpty()) {
            System.out.println("Nenhum conteúdo encontrado.");
        }
        for (ConteudoDetalhes conteudo : itens) {
            System.out.printf(
                    "%d - %s | %s | %s | %s%n",
                    conteudo.id(),
                    conteudo.titulo(),
                    conteudo.tipo(),
                    conteudo.genero(),
                    conteudo.classificacao());
        }
    }

    private void catalogo() {
        String titulo = ler("Título (vazio para todos): ");
        String tipo = ler("Tipo FILME/SERIE (vazio para todos): ");
        String genero = ler("Gênero (vazio para todos): ");
        String classificacao = ler("Classificação (vazio para todas): ");
        String ordem =
                switch (ler("Ordenar por 1. título ou 2. gênero: ")) {
                    case "", "1" -> "TITULO";
                    case "2" -> "GENERO";
                    default -> throw new IllegalArgumentException("Ordenação inválida.");
                };
        mostrarConteudos(api.buscar(titulo, tipo, genero, classificacao, ordem));
    }

    private void detalhes() {
        int id = numero("ID do conteúdo: ");
        ConteudoDetalhes conteudo = api.conteudo(id);
        System.out.printf(
                "%s | %s | %s | %s%n%s%n",
                conteudo.titulo(), conteudo.tipo(), conteudo.genero(),
                conteudo.classificacao(), conteudo.sinopse());
        if (conteudo.duracao() != null) {
            System.out.println("Duração: " + conteudo.duracao() + " segundos.");
        } else {
            for (EpisodioDetalhes episodio : conteudo.episodios()) {
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
                boolean adicionado = api.adicionarFavorito(conteudo.id());
                System.out.println(adicionado ? "Adicionado aos favoritos." : "Já está nos favoritos.");
            }
            case "3" -> {
                boolean removido = api.removerFavorito(conteudo.id());
                System.out.println(removido ? "Removido dos favoritos." : "Não estava nos favoritos.");
            }
            case "0" -> {
                return;
            }
            default -> System.out.println("Opção inválida.");
        }
    }

    /** Apresenta as opções de retomada e coleta o tempo assistido na reprodução simulada. */
    private void reproduzir(ConteudoDetalhes conteudo) {
        int idEpisodio = 0;
        if (conteudo.tipo().equals("SERIE")) {
            idEpisodio = numero("ID do episódio: ");
        }
        ReproducaoAtual reproducao = api.prepararReproducao(conteudo.id(), idEpisodio);
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
                inicio, reiniciar ? reproducao.limiteReinicio() : reproducao.limiteRetomada());
        int segundosAssistidos = numero("Segundos assistidos até pausar: ");
        int progresso = api.registrarReproducao(
                conteudo.id(), idEpisodio, reiniciar, segundosAssistidos);
        System.out.println(
                progresso == duracao
                        ? "Conteúdo concluído."
                        : "Reprodução pausada em " + progresso + " segundos.");
    }

    private void favoritos() {
        List<ConteudoDetalhes> itens = api.favoritos();
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
            boolean atualizado = api.editarFavorito(id, novoId);
            System.out.println(atualizado ? "Favorito atualizado." : "Favorito não encontrado.");
            return;
        }
        int id;
        try {
            id = Integer.parseInt(escolha);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ID inválido.");
        }
        boolean removido = api.removerFavorito(id);
        System.out.println(removido ? "Favorito removido." : "Favorito não encontrado.");
    }

    private void historico() {
        List<RegistroHistorico> itens = api.historico();
        if (itens.isEmpty()) {
            System.out.println("Histórico vazio.");
        }
        for (RegistroHistorico visualizacao : itens) {
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

    private void recomendacoes() {
        System.out.println(
                "Sugestões por gêneros mais assistidos, excluindo títulos já iniciados:");
        mostrarConteudos(api.recomendacoes());
    }

    /** Sinaliza a saída solicitada no menu ou o fim da entrada de dados. */
    private static final class FimEntrada extends RuntimeException {}
}
