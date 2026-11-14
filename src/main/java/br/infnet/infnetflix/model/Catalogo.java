package br.infnet.infnetflix.model;

import java.text.Collator;
import java.util.*;

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

    public List<Conteudo> buscar(FiltroCatalogo filtro, Perfil perfil) {
        int idade = perfil.idade();
        Comparator<Conteudo> ordem = porTitulo();
        if (filtro.ordem() == FiltroCatalogo.Ordem.GENERO) {
            Collator comparador = comparadorPortugues();
            ordem = Comparator.comparing((Conteudo c) -> c.genero().nome(), comparador).thenComparing(ordem);
        }
        return itens.stream()
                .filter(c -> idade >= c.idadeMinima())
                .filter(c -> filtro.tipo().isEmpty() || c.tipo().equals(filtro.tipo()))
                .filter(c -> Textos.contem(c.titulo(), filtro.titulo()))
                .filter(c -> Textos.contem(c.genero().nome(), filtro.genero()))
                .filter(c -> Textos.contem(c.classificacao(), filtro.classificacao()))
                .sorted(ordem).toList();
    }

    public Conteudo obterDetalhes(int id, Perfil perfil) {
        return itens.stream().filter(c -> c.id() == id && perfil.podeAssistir(c)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Conteúdo não encontrado."));
    }

    private static Collator comparadorPortugues() {
        Collator comparador = Collator.getInstance(Locale.forLanguageTag("pt-BR"));
        comparador.setStrength(Collator.PRIMARY);
        return comparador;
    }

    public static Comparator<Conteudo> porTitulo() {
        return Comparator.comparing(Conteudo::titulo, comparadorPortugues()).thenComparingInt(Conteudo::id);
    }
}
