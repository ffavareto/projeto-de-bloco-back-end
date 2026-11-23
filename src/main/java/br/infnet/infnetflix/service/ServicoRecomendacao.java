package br.infnet.infnetflix.service;

import br.infnet.infnetflix.model.Catalogo;
import br.infnet.infnetflix.model.Conteudo;
import br.infnet.infnetflix.model.Perfil;
import br.infnet.infnetflix.model.Textos;
import br.infnet.infnetflix.model.Visualizacao;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ServicoRecomendacao {
    public List<Conteudo> recomendar(Perfil perfil, Catalogo catalogo) {
        Map<String, Integer> afinidades = new HashMap<>();
        Set<Integer> iniciados = new HashSet<>();
        for (Visualizacao visualizacao : perfil.listarHistorico()) {
            if (visualizacao.progresso() <= 0) {
                continue;
            }
            Conteudo conteudo = visualizacao.item().conteudo();
            iniciados.add(conteudo.id());
            String genero = Textos.normalizar(conteudo.genero().nome());
            int total = afinidades.getOrDefault(genero, 0);
            afinidades.put(genero, total + 1);
        }

        List<Conteudo> sugestoes = new ArrayList<>();
        for (Conteudo conteudo : catalogo.listar(perfil)) {
            if (!iniciados.contains(conteudo.id())) {
                sugestoes.add(conteudo);
            }
        }

        Comparator<Conteudo> porTitulo = Catalogo.porTitulo();
        sugestoes.sort((primeiro, segundo) -> {
            String generoPrimeiro = Textos.normalizar(primeiro.genero().nome());
            String generoSegundo = Textos.normalizar(segundo.genero().nome());
            int afinidadePrimeiro = afinidades.getOrDefault(generoPrimeiro, 0);
            int afinidadeSegundo = afinidades.getOrDefault(generoSegundo, 0);
            int ordem = Integer.compare(afinidadeSegundo, afinidadePrimeiro);
            if (ordem != 0) {
                return ordem;
            }
            return porTitulo.compare(primeiro, segundo);
        });
        if (sugestoes.size() > 10) {
            return List.copyOf(sugestoes.subList(0, 10));
        }
        return List.copyOf(sugestoes);
    }
}
