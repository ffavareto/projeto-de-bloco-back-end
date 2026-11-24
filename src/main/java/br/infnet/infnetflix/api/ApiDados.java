package br.infnet.infnetflix.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.util.List;

/** Representações JSON trocadas entre o console e a API. */
public final class ApiDados {
    static final ObjectMapper JSON = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .build();

    private ApiDados() {}

    public record Cadastro(String nome, String email, String senha, String dataNascimento) {}
    public record Login(String email, String senha) {}
    public record Sessao(String token) {}
    public record Id(int id) {}
    public record PerfilResumo(int id, String nome) {}
    public record NovoPerfil(String nome, String dataNascimento) {}
    public record Nome(String nome) {}
    public record PlanoAtual(boolean premium) {}

    public record ConteudoDetalhes(int id, String titulo, String tipo, String genero, String classificacao,
            String sinopse, Integer duracao, List<EpisodioDetalhes> episodios) {}

    public record EpisodioDetalhes(int id, int temporada, int numero, String titulo, int duracao) {}

    public record ReproducaoAtual(int progresso, int duracao, boolean premium, int limiteRetomada,
            int limiteReinicio) {}

    public record Avanco(boolean reiniciar, int segundos) {}
    public record Progresso(int segundos) {}
    public record Resultado(boolean alterado) {}

    public record RegistroHistorico(String titulo, Integer temporada, Integer numero, String episodio,
            int progresso, String atualizadoEm) {}

    public record Erro(String mensagem) {}
}
