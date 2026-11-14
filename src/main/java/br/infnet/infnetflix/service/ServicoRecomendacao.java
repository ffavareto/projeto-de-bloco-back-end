package br.infnet.infnetflix.service;

import br.infnet.infnetflix.model.*;
import java.util.*;

public final class ServicoRecomendacao {
    public List<Conteudo> recomendar(Perfil perfil, Catalogo catalogo) {
        Map<String, Integer> afinidades = new HashMap<>();
        Set<Integer> iniciados = new HashSet<>();
        for (Visualizacao visualizacao : perfil.listarHistorico()) {
            if (visualizacao.progresso() > 0) {
                Conteudo conteudo = visualizacao.item().conteudo();
                iniciados.add(conteudo.id());
                afinidades.merge(Textos.normalizar(conteudo.genero().nome()), 1, Integer::sum);
            }
        }
        return catalogo.listar(perfil).stream()
                .filter(c -> !iniciados.contains(c.id()))
                .sorted(Comparator.<Conteudo>comparingInt(
                        c -> afinidades.getOrDefault(Textos.normalizar(c.genero().nome()), 0))
                        .reversed().thenComparing(Catalogo.porTitulo()))
                .limit(10).toList();
    }
}
