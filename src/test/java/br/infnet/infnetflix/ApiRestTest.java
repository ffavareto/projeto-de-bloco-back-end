package br.infnet.infnetflix;

import br.infnet.infnetflix.api.InfnetflixClient;
import br.infnet.infnetflix.api.ServidorRest;
import br.infnet.infnetflix.repository.Database;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ApiRestTest {
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void autorizaSomentePerfisDaContaDoTokenERevogaNoLogout() throws Exception {
        try (Database db = new Database("jdbc:sqlite::memory:"); ServidorRest api = new ServidorRest(db, 0)) {
            api.iniciar();
            int porta = api.porta();
            InfnetflixClient cliente = new InfnetflixClient(porta);
            cliente.cadastrar("Ana", "ana@example.com", "senha123", LocalDate.of(1990, 1, 1));
            cliente.cadastrar("Bia", "bia@example.com", "senha123", LocalDate.of(1992, 1, 1));

            HttpResponse<String> semToken = enviar(porta, "GET", "/perfis/1/catalogo", null, null);
            assertEquals(401, semToken.statusCode());
            assertEquals("Bearer", semToken.headers().firstValue("WWW-Authenticate").orElseThrow());
            assertEquals(401, enviar(porta, "POST", "/sessoes", null,
                    """
                    {"email":"ana@example.com","senha":"incorreta"}
                    """).statusCode());
            HttpResponse<String> login = enviar(porta, "POST", "/sessoes", null,
                    """
                    {"email":"ana@example.com","senha":"senha123"}
                    """);
            assertEquals(201, login.statusCode());
            String token = json.readTree(login.body()).get("token").asText();

            HttpResponse<String> titulos = enviar(porta, "GET", "/perfis/1/catalogo?titulo=%C3%9ALTIMA", token, null);
            assertEquals(200, titulos.statusCode());
            assertEquals("A Última Fronteira", json.readTree(titulos.body()).get(0).get("titulo").asText());
            assertEquals(400, enviar(porta, "GET", "/perfis/2/catalogo", token, null).statusCode());
            assertEquals(400, enviar(porta, "PUT", "/perfis/2/favoritos/1", token, null).statusCode());

            assertTrue(cliente.entrar("bia@example.com", "senha123"));
            cliente.selecionarPerfil(2);
            assertTrue(cliente.favoritos().isEmpty());

            assertEquals(204, enviar(porta, "DELETE", "/sessao", token, null).statusCode());
            assertEquals(401, enviar(porta, "GET", "/perfis/1/catalogo", token, null).statusCode());
            assertFalse(cliente.buscar("", "", "", "", "TITULO").isEmpty());
            cliente.sair();
        }
    }

    @Test
    void validaJsonEClassificacaoEtariaNaApi() throws Exception {
        try (Database db = new Database("jdbc:sqlite::memory:"); ServidorRest api = new ServidorRest(db, 0)) {
            api.iniciar();
            int porta = api.porta();
            assertEquals(400, enviar(porta, "POST", "/contas", null, "{").statusCode());
            assertEquals(415, enviar(porta, "POST", "/contas", null, null).statusCode());
            HttpResponse<String> metodoInvalido = enviar(porta, "PUT", "/contas", null, null);
            assertEquals(405, metodoInvalido.statusCode());
            assertEquals("POST", metodoInvalido.headers().firstValue("Allow").orElseThrow());

            InfnetflixClient cliente = new InfnetflixClient(porta);
            cliente.cadastrar("Bia", "bia@example.com", "senha123", LocalDate.now().minusYears(8));
            assertTrue(cliente.entrar("bia@example.com", "senha123"));
            cliente.selecionarPerfil(1);
            assertTrue(cliente.buscar("última", "", "", "", "TITULO").isEmpty());
            assertThrows(IllegalStateException.class, () -> cliente.conteudo(1));
            assertThrows(IllegalStateException.class, () -> cliente.prepararReproducao(1, 0));
            assertThrows(IllegalStateException.class, () -> cliente.registrarReproducao(1, 0, false, 50));
            assertTrue(cliente.historico().isEmpty());
            cliente.sair();
        }
    }

    private HttpResponse<String> enviar(int porta, String metodo, String rota, String token, String corpo)
            throws IOException, InterruptedException {
        HttpRequest.Builder pedido = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + porta + rota))
                .timeout(Duration.ofSeconds(10));
        if (token != null) {
            pedido.header("Authorization", "Bearer " + token);
        }
        if (corpo != null) {
            pedido.header("Content-Type", "application/json");
        }
        HttpRequest.BodyPublisher dados = corpo == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(corpo);
        return http.send(pedido.method(metodo, dados).build(), HttpResponse.BodyHandlers.ofString());
    }
}
