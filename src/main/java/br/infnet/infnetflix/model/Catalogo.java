package br.infnet.infnetflix.model;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Consulta e ordena os títulos disponíveis para a idade do perfil. */
public final class Catalogo {
    private final List<Conteudo> itens;

    public Catalogo(List<Conteudo> itens) { this.itens = List.copyOf(itens); }

    public List<Conteudo> listar(Perfil perfil) {
        return buscar(new FiltroCatalogo("", "", "", "", FiltroCatalogo.Ordem.TITULO), perfil);
    }

    public List<Conteudo> buscar(String termo, Perfil perfil) {
        return buscar(new FiltroCatalogo(termo, "", "", "", FiltroCatalogo.Ordem.TITULO), perfil);
    }

    public List<Conteudo> filtrar(Genero genero, String classificacao, Perfil perfil) {
        return buscar(new FiltroCatalogo("", "", genero == null ? "" : genero.nome(),
                classificacao, FiltroCatalogo.Ordem.TITULO), perfil);
    }

    public List<Conteudo> ordenar(String criterio, Perfil perfil) {
        FiltroCatalogo.Ordem ordem = switch (Textos.normalizar(criterio)) {
            case "titulo" -> FiltroCatalogo.Ordem.TITULO;
            case "genero" -> FiltroCatalogo.Ordem.GENERO;
            default -> throw new IllegalArgumentException("Ordenação inválida.");
        };
        return buscar(new FiltroCatalogo("", "", "", "", ordem), perfil);
    }

    /** Combina os filtros preenchidos e usa o título como desempate na ordenação por gênero. */
    public List<Conteudo> buscar(FiltroCatalogo filtro, Perfil perfil) {
        int idade = perfil.idade();
        Comparator<Conteudo> ordem = porTitulo();
        if (filtro.ordem() == FiltroCatalogo.Ordem.GENERO) {
            Comparator<Conteudo> porGenero = Comparator.comparing(
                    conteudo -> conteudo.genero().nome(), comparadorPortugues());
            ordem = porGenero.thenComparing(ordem);
        }

        List<Conteudo> encontrados = new ArrayList<>();
        for (Conteudo conteudo : itens) {
            if (idade >= conteudo.idadeMinima()
                    && (filtro.tipo().isEmpty() || conteudo.tipo().equals(filtro.tipo()))
                    && Textos.contem(conteudo.titulo(), filtro.titulo())
                    && Textos.contem(conteudo.genero().nome(), filtro.genero())
                    && Textos.contem(conteudo.classificacao(), filtro.classificacao())) {
                encontrados.add(conteudo);
            }
        }
        encontrados.sort(ordem);
        return List.copyOf(encontrados);
    }

    /** Busca pelo ID entre os títulos que o perfil pode assistir. */
    public Conteudo obterDetalhes(int id, Perfil perfil) {
        for (Conteudo conteudo : itens) {
            if (conteudo.id() == id && perfil.podeAssistir(conteudo)) {
                return conteudo;
            }
        }
        throw new IllegalArgumentException("Conteúdo não encontrado.");
    }

    private static Collator comparadorPortugues() {
        Collator comparador = Collator.getInstance(Locale.forLanguageTag("pt-BR"));
        comparador.setStrength(Collator.PRIMARY);
        return comparador;
    }

    /** Ordena títulos pelas regras do português brasileiro e desempata pelo ID. */
    public static Comparator<Conteudo> porTitulo() {
        return Comparator.comparing(Conteudo::titulo, comparadorPortugues()).thenComparingInt(Conteudo::id);
    }
}
