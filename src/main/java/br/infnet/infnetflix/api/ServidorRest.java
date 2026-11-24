package br.infnet.infnetflix.api;

import br.infnet.infnetflix.api.ApiDados.*;
import br.infnet.infnetflix.controller.InfnetflixController;
import br.infnet.infnetflix.model.Conta;
import br.infnet.infnetflix.model.Conteudo;
import br.infnet.infnetflix.model.FiltroCatalogo;
import br.infnet.infnetflix.model.Filme;
import br.infnet.infnetflix.model.Perfil;
import br.infnet.infnetflix.model.Reproducao;
import br.infnet.infnetflix.model.Serie;
import br.infnet.infnetflix.repository.CatalogoRepository;
import br.infnet.infnetflix.repository.ContaRepository;
import br.infnet.infnetflix.repository.Database;
import br.infnet.infnetflix.repository.VisualizacaoRepository;
import br.infnet.infnetflix.service.ServicoAutenticacao;
import com.fasterxml.jackson.core.JacksonException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static br.infnet.infnetflix.api.ApiDados.JSON;

/** Expõe as operações do INFNETFLIX por HTTP, com uma sessão por token. */
public final class ServidorRest implements AutoCloseable {
    public static final int PORTA_PADRAO = 8080;
    public static final String HOST_LOCAL = "127.0.0.1";

    private static final Logger LOG = Logger.getLogger(ServidorRest.class.getName());
    private static final String PERFIL = "/perfis/(?<perfil>-?[0-9]+)";
    private static final String TITULO = "/catalogo/(?<titulo>-?[0-9]+)";
    private static final String REPRODUCAO = TITULO + "/itens/(?<episodio>-?[0-9]+)/reproducao";
    private static final String FAVORITO = "/favoritos/(?<favorito>-?[0-9]+)";
    private static final Resposta SEM_CORPO = new Resposta(204, null);
    private static final Resposta CRIADO = new Resposta(201, null);

    private final HttpServer servidor;
    private final ContaRepository contas;
    private final CatalogoRepository catalogo;
    private final VisualizacaoRepository visualizacoes;
    private final ServicoAutenticacao autenticacao;
    private final Map<String, Integer> sessoes = new HashMap<>();

    private final List<Rota> rotas = List.of(
            new Rota("GET", "/saude", false, (http, caminho, sessao) -> SEM_CORPO),
            new Rota("POST", "/contas", false, (http, caminho, sessao) -> cadastrar(http)),
            new Rota("POST", "/sessoes", false, (http, caminho, sessao) -> entrar(http)),
            new Rota("DELETE", "/sessao", (http, caminho, sessao) -> {
                sessoes.remove(token(http));
                return SEM_CORPO;
            }),
            new Rota("GET", "/perfis", (http, caminho, sessao) -> ok(sessao.perfis().stream()
                    .map(ServidorRest::resumoPerfil).toList())),
            new Rota("GET", "/perfis/disponibilidade", (http, caminho, sessao) -> {
                sessao.verificarDisponibilidadePerfil();
                return SEM_CORPO;
            }),
            new Rota("POST", "/perfis", (http, caminho, sessao) -> {
                NovoPerfil perfil = ler(http, NovoPerfil.class);
                sessao.criarPerfil(perfil.nome(), data(perfil.dataNascimento()));
                return CRIADO;
            }),
            new Rota("GET", PERFIL, (http, caminho, sessao) ->
                    ok(resumoPerfil(sessao.perfil(id(caminho, "perfil"))))),
            new Rota("PATCH", PERFIL, (http, caminho, sessao) -> {
                sessao.renomearPerfil(id(caminho, "perfil"), ler(http, Nome.class).nome());
                return SEM_CORPO;
            }),
            new Rota("GET", PERFIL + "/exclusao", (http, caminho, sessao) -> {
                sessao.validarExclusaoPerfil(id(caminho, "perfil"));
                return SEM_CORPO;
            }),
            new Rota("DELETE", PERFIL, (http, caminho, sessao) -> {
                sessao.excluirPerfil(id(caminho, "perfil"));
                return SEM_CORPO;
            }),
            new Rota("GET", "/plano", (http, caminho, sessao) -> ok(new PlanoAtual(sessao.premium()))),
            new Rota("PUT", "/plano/premium", (http, caminho, sessao) -> {
                sessao.ativarPremium();
                return SEM_CORPO;
            }),
            noPerfil("GET", "/catalogo", (http, caminho, sessao) -> ok(sessao.buscar(filtro(http)).stream()
                    .map(ServidorRest::detalhes).toList())),
            noPerfil("GET", TITULO, (http, caminho, sessao) ->
                    ok(detalhes(sessao.conteudo(id(caminho, "titulo"))))),
            noPerfil("GET", REPRODUCAO, (http, caminho, sessao) -> {
                Reproducao reproducao = sessao.prepararReproducao(id(caminho, "titulo"), id(caminho, "episodio"));
                return ok(new ReproducaoAtual(reproducao.progresso(), reproducao.duracao(), reproducao.premium(),
                        reproducao.limite(reproducao.progresso()), reproducao.limite(0)));
            }),
            noPerfil("POST", REPRODUCAO, (http, caminho, sessao) -> {
                Avanco avanco = ler(http, Avanco.class);
                int progresso = sessao.registrarReproducao(
                        id(caminho, "titulo"), id(caminho, "episodio"), avanco.reiniciar(), avanco.segundos());
                return ok(new Progresso(progresso));
            }),
            noPerfil("GET", "/favoritos", (http, caminho, sessao) -> ok(sessao.favoritos().stream()
                    .map(ServidorRest::detalhes).toList())),
            noPerfil("PUT", FAVORITO, (http, caminho, sessao) ->
                    ok(new Resultado(sessao.adicionarFavorito(id(caminho, "favorito"))))),
            noPerfil("PATCH", FAVORITO, (http, caminho, sessao) ->
                    ok(new Resultado(sessao.editarFavorito(id(caminho, "favorito"), ler(http, Id.class).id())))),
            noPerfil("DELETE", FAVORITO, (http, caminho, sessao) ->
                    ok(new Resultado(sessao.removerFavorito(id(caminho, "favorito"))))),
            noPerfil("GET", "/historico", (http, caminho, sessao) -> ok(sessao.historico().stream()
                    .map(item -> new RegistroHistorico(item.titulo(), item.temporada(), item.numero(),
                            item.episodio(), item.progresso(), item.atualizadoEm()))
                    .toList())),
            noPerfil("GET", "/recomendacoes", (http, caminho, sessao) -> ok(sessao.recomendacoes().stream()
                    .map(ServidorRest::detalhes).toList()))
    );

    /** O executor padrão atende as requisições em série na conexão SQLite compartilhada. */
    public ServidorRest(Database db, int porta) throws IOException {
        contas = new ContaRepository(db);
        catalogo = new CatalogoRepository(db);
        visualizacoes = new VisualizacaoRepository(db);
        autenticacao = new ServicoAutenticacao(contas);
        servidor = HttpServer.create(new InetSocketAddress(HOST_LOCAL, porta), 0);
        servidor.createContext("/", this::atender);
    }

    public void iniciar() { servidor.start(); }
    public int porta() { return servidor.getAddress().getPort(); }
    @Override public void close() { servidor.stop(1); }

    private void atender(HttpExchange http) throws IOException {
        try (http) {
            Resposta resposta;
            try {
                resposta = rotear(http);
            } catch (ErroHttp e) {
                if (e.status == 401) {
                    http.getResponseHeaders().set("WWW-Authenticate", "Bearer");
                }
                resposta = new Resposta(e.status, new Erro(e.getMessage()));
            } catch (IllegalArgumentException e) {
                resposta = new Resposta(400, new Erro(e.getMessage()));
            } catch (IllegalStateException e) {
                resposta = new Resposta(409, new Erro(e.getMessage()));
            } catch (JacksonException e) {
                resposta = new Resposta(400, new Erro("Corpo JSON inválido."));
            } catch (SQLException e) {
                LOG.log(Level.WARNING, "Falha na operação de banco de dados", e);
                resposta = new Resposta(500, new Erro("Não foi possível concluir a operação."));
            } catch (RuntimeException e) {
                LOG.log(Level.WARNING, "Falha ao processar requisição", e);
                resposta = new Resposta(500, new Erro("Não foi possível concluir a operação."));
            }
            escrever(http, resposta);
        }
    }

    private Resposta rotear(HttpExchange http) throws SQLException, IOException {
        List<String> permitidos = new ArrayList<>();
        String caminho = http.getRequestURI().getPath();
        for (Rota rota : rotas) {
            Matcher partes = rota.caminho().matcher(caminho);
            if (!partes.matches()) {
                continue;
            }
            if (rota.metodo().equals(http.getRequestMethod())) {
                InfnetflixController sessao = rota.autenticada() ? sessao(http) : null;
                return rota.operacao().executar(http, partes, sessao);
            }
            permitidos.add(rota.metodo());
        }
        if (!permitidos.isEmpty()) {
            http.getResponseHeaders().set("Allow", String.join(", ", permitidos));
            throw new ErroHttp(405, "Método HTTP não permitido.");
        }
        throw new ErroHttp(404, "Rota não encontrada.");
    }

    private InfnetflixController sessao(HttpExchange http) {
        Integer contaId = sessoes.get(token(http));
        if (contaId == null) {
            throw new ErroHttp(401, "Entre na conta primeiro.");
        }
        return new InfnetflixController(contas, catalogo, visualizacoes, contaId);
    }

    private static String token(HttpExchange http) {
        String autenticacao = http.getRequestHeaders().getFirst("Authorization");
        return autenticacao != null && autenticacao.regionMatches(true, 0, "Bearer ", 0, 7)
                ? autenticacao.substring(7) : null;
    }

    private Resposta cadastrar(HttpExchange http) throws IOException, SQLException {
        Cadastro cadastro = ler(http, Cadastro.class);
        autenticacao.cadastrar(cadastro.nome(), cadastro.email(), cadastro.senha(),
                data(cadastro.dataNascimento()));
        return CRIADO;
    }

    private Resposta entrar(HttpExchange http) throws IOException, SQLException {
        Login login = ler(http, Login.class);
        Conta conta = autenticacao.login(login.email(), login.senha())
                .orElseThrow(() -> new ErroHttp(401, "E-mail ou senha inválidos."));
        String token = UUID.randomUUID().toString();
        sessoes.put(token, conta.id());
        return new Resposta(201, new Sessao(token));
    }

    /** Verifica se o perfil informado na URL pertence à conta autenticada. */
    private static Rota noPerfil(String metodo, String caminho, Operacao operacao) {
        return new Rota(metodo, PERFIL + caminho, (http, partes, sessao) -> {
            sessao.selecionarPerfil(id(partes, "perfil"));
            return operacao.executar(http, partes, sessao);
        });
    }

    private static FiltroCatalogo filtro(HttpExchange http) {
        Map<String, String> filtros = new HashMap<>();
        String query = http.getRequestURI().getRawQuery();
        if (query != null) {
            for (String campo : query.split("&")) {
                String[] partes = campo.split("=", 2);
                if (partes.length == 2) {
                    String chave = URLDecoder.decode(partes[0], StandardCharsets.UTF_8);
                    filtros.put(chave, URLDecoder.decode(partes[1], StandardCharsets.UTF_8));
                }
            }
        }

        FiltroCatalogo.Ordem ordem;
        try {
            ordem = FiltroCatalogo.Ordem.valueOf(filtros.getOrDefault("ordem", "TITULO")
                    .toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Ordenação inválida.");
        }
        return new FiltroCatalogo(filtros.get("titulo"), filtros.get("tipo"), filtros.get("genero"),
                filtros.get("classificacao"), ordem);
    }

    private static LocalDate data(String valor) {
        try {
            return valor == null ? null : LocalDate.parse(valor);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Data de nascimento inválida. Use aaaa-MM-dd.");
        }
    }

    private static ConteudoDetalhes detalhes(Conteudo conteudo) {
        Integer duracao = conteudo instanceof Filme filme ? filme.duracao() : null;
        List<EpisodioDetalhes> episodios = conteudo instanceof Serie serie
                ? serie.listarEpisodios().stream()
                        .map(e -> new EpisodioDetalhes(e.id(), e.temporada(), e.numero(), e.titulo(), e.duracao()))
                        .toList()
                : List.of();
        return new ConteudoDetalhes(conteudo.id(), conteudo.titulo(), conteudo.tipo(), conteudo.genero().nome(),
                conteudo.classificacao(), conteudo.sinopse(), duracao, episodios);
    }

    private static PerfilResumo resumoPerfil(Perfil perfil) { return new PerfilResumo(perfil.id(), perfil.nome()); }
    private static int id(Matcher caminho, String grupo) { return Integer.parseInt(caminho.group(grupo)); }
    private static Resposta ok(Object corpo) { return new Resposta(200, corpo); }

    private static <T> T ler(HttpExchange http, Class<T> tipo) throws IOException {
        String formato = http.getRequestHeaders().getFirst("Content-Type");
        if (formato == null || !formato.split(";", 2)[0].trim().equalsIgnoreCase("application/json")) {
            throw new ErroHttp(415, "Envie o corpo da requisição em application/json.");
        }
        T dados = JSON.readValue(http.getRequestBody(), tipo);
        if (dados == null) {
            throw new ErroHttp(400, "Corpo JSON inválido.");
        }
        return dados;
    }

    private static void escrever(HttpExchange http, Resposta resposta) throws IOException {
        http.getResponseHeaders().set("Cache-Control", "no-store");
        if (resposta.corpo() == null) {
            http.sendResponseHeaders(resposta.status(), -1);
            return;
        }
        byte[] json = JSON.writeValueAsBytes(resposta.corpo());
        http.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        http.sendResponseHeaders(resposta.status(), json.length);
        http.getResponseBody().write(json);
    }

    private record Resposta(int status, Object corpo) {}

    private record Rota(String metodo, Pattern caminho, boolean autenticada, Operacao operacao) {
        Rota(String metodo, String caminho, boolean autenticada, Operacao operacao) {
            this(metodo, Pattern.compile(caminho), autenticada, operacao);
        }

        Rota(String metodo, String caminho, Operacao operacao) {
            this(metodo, caminho, true, operacao);
        }
    }

    @FunctionalInterface
    private interface Operacao {
        Resposta executar(HttpExchange http, Matcher caminho, InfnetflixController sessao)
                throws SQLException, IOException;
    }

    private static final class ErroHttp extends RuntimeException {
        private final int status;

        ErroHttp(int status, String mensagem) {
            super(mensagem);
            this.status = status;
        }
    }
}
