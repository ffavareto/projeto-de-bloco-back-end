package br.infnet.infnetflix.api;

import br.infnet.infnetflix.api.ApiDados.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static br.infnet.infnetflix.api.ApiDados.JSON;

/** Cliente HTTP local que conserva o token e o estado de navegação do console. */
public final class InfnetflixClient {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final URI base;
    private String token;
    private int perfilId;

    public InfnetflixClient(int porta) {
        base = URI.create("http://" + ServidorRest.HOST_LOCAL + ":" + porta);
    }

    public void verificarConexao() { requisitar("GET", "/saude", null, null); }
    public boolean autenticado() { return token != null; }
    public boolean perfilSelecionado() { return perfilId != 0; }

    public void cadastrar(String nome, String email, String senha, LocalDate dataNascimento) {
        requisitar("POST", "/contas", new Cadastro(nome, email, senha, dataNascimento.toString()), null);
    }

    public boolean entrar(String email, String senha) {
        sair();
        try {
            Sessao sessao = requisitar("POST", "/sessoes", new Login(email, senha), JSON.readerFor(Sessao.class));
            token = sessao.token();
            return true;
        } catch (FalhaApi e) {
            if (e.status == 401) {
                return false;
            }
            throw e;
        }
    }

    public void sair() {
        if (autenticado()) {
            requisitar("DELETE", "/sessao", null, null);
            limparSessao();
        }
    }

    public void selecionarPerfil(int id) {
        PerfilResumo perfil = obter("/perfis/" + id, PerfilResumo.class);
        perfilId = perfil.id();
    }

    public void trocarPerfil() { perfilId = 0; }

    public List<PerfilResumo> perfis() { return listar("/perfis", PerfilResumo.class); }

    public void verificarDisponibilidadePerfil() { requisitar("GET", "/perfis/disponibilidade", null, null); }

    public void criarPerfil(String nome, LocalDate dataNascimento) {
        requisitar("POST", "/perfis", new NovoPerfil(nome, dataNascimento.toString()), null);
    }

    public void renomearPerfil(int id, String nome) {
        requisitar("PATCH", "/perfis/" + id, new Nome(nome), null);
    }

    public void validarExclusaoPerfil(int id) { requisitar("GET", "/perfis/" + id + "/exclusao", null, null); }
    public void excluirPerfil(int id) {
        requisitar("DELETE", "/perfis/" + id, null, null);
        if (perfilId == id) {
            trocarPerfil();
        }
    }
    public boolean premium() { return obter("/plano", PlanoAtual.class).premium(); }
    public void ativarPremium() { requisitar("PUT", "/plano/premium", null, null); }

    public List<ConteudoDetalhes> buscar(String titulo, String tipo, String genero, String classificacao,
            String ordem) {
        String consulta = "?titulo=" + codificar(titulo) + "&tipo=" + codificar(tipo)
                + "&genero=" + codificar(genero) + "&classificacao=" + codificar(classificacao)
                + "&ordem=" + codificar(ordem);
        return listar(noPerfil("/catalogo") + consulta, ConteudoDetalhes.class);
    }

    public ConteudoDetalhes conteudo(int id) { return obter(noPerfil("/catalogo/" + id), ConteudoDetalhes.class); }
    public List<ConteudoDetalhes> favoritos() { return listar(noPerfil("/favoritos"), ConteudoDetalhes.class); }

    public boolean adicionarFavorito(int id) { return alterarFavorito("PUT", id, null); }
    public boolean editarFavorito(int id, int novoConteudoId) {
        return alterarFavorito("PATCH", id, new Id(novoConteudoId));
    }
    public boolean removerFavorito(int id) { return alterarFavorito("DELETE", id, null); }

    public List<RegistroHistorico> historico() { return listar(noPerfil("/historico"), RegistroHistorico.class); }
    public List<ConteudoDetalhes> recomendacoes() { return listar(noPerfil("/recomendacoes"), ConteudoDetalhes.class); }

    public ReproducaoAtual prepararReproducao(int conteudoId, int episodioId) {
        return obter(rotaReproducao(conteudoId, episodioId), ReproducaoAtual.class);
    }

    public int registrarReproducao(int conteudoId, int episodioId, boolean reiniciar, int segundos) {
        Progresso progresso = requisitar("POST", rotaReproducao(conteudoId, episodioId),
                new Avanco(reiniciar, segundos), JSON.readerFor(Progresso.class));
        return progresso.segundos();
    }

    private boolean alterarFavorito(String metodo, int id, Id corpo) {
        Resultado resultado = requisitar(metodo, noPerfil("/favoritos/" + id), corpo, JSON.readerFor(Resultado.class));
        return resultado.alterado();
    }

    private String rotaReproducao(int conteudoId, int episodioId) {
        return noPerfil("/catalogo/" + conteudoId + "/itens/" + episodioId + "/reproducao");
    }

    private String noPerfil(String rota) {
        if (!perfilSelecionado()) {
            throw new IllegalStateException("Selecione um perfil primeiro.");
        }
        return "/perfis/" + perfilId + rota;
    }

    private static String codificar(String valor) { return URLEncoder.encode(valor, StandardCharsets.UTF_8); }

    private <T> T obter(String rota, Class<T> tipo) { return requisitar("GET", rota, null, JSON.readerFor(tipo)); }

    private <T> List<T> listar(String rota, Class<T> tipo) {
        return requisitar("GET", rota, null, JSON.readerForListOf(tipo));
    }

    private void limparSessao() {
        token = null;
        trocarPerfil();
    }

    /** Envia JSON e traduz falhas da API em mensagens para o console. */
    private <T> T requisitar(String metodo, String rota, Object corpo, ObjectReader leitor) {
        try {
            HttpRequest.Builder pedido = HttpRequest.newBuilder(base.resolve(rota))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json");
            if (token != null) {
                pedido.header("Authorization", "Bearer " + token);
            }
            HttpRequest.BodyPublisher dados = HttpRequest.BodyPublishers.noBody();
            if (corpo != null) {
                pedido.header("Content-Type", "application/json");
                dados = HttpRequest.BodyPublishers.ofByteArray(JSON.writeValueAsBytes(corpo));
            }

            HttpResponse<String> resposta = http.send(pedido.method(metodo, dados).build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = resposta.statusCode();
            if (status == 401) {
                limparSessao();
            }
            if (status < 200 || status >= 300) {
                Erro erro = JSON.readValue(resposta.body(), Erro.class);
                throw new FalhaApi(status, erro.mensagem());
            }
            return leitor == null ? null : leitor.readValue(resposta.body());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("A API em " + base + " retornou uma resposta inválida.", e);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível acessar a API em " + base + ".", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("A requisição à API foi interrompida.", e);
        }
    }

    private static final class FalhaApi extends IllegalStateException {
        private final int status;

        FalhaApi(int status, String mensagem) {
            super(mensagem);
            this.status = status;
        }
    }
}
