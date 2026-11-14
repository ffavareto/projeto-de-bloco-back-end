@startuml
skinparam classAttributeIconSize 0

enum TipoPlano {
GRATUITO
PREMIUM
}

interface Reproduzivel {
+reproduzir(perfil: Perfil)
+pausar()
}

class Conta {
-contaId: int
-nome: String
-email: String
-senhaHash: String
-perfis: Perfil[1..3]
-plano: Plano
+criarPerfil(nome: String, dataNascimento: LocalDate): Perfil
+editarPerfil(perfilId: int, nome: String)
+excluirPerfil(perfilId: int)
+selecionarPerfil(perfilId: int): Perfil
+assinarPremium()
+validarSenha(senha: String): boolean
}

class Perfil {
-perfilId: int
-nome: String
-dataNascimento: LocalDate
-favoritos: Favorito[]
-historico: Visualizacao[]
+adicionarFavorito(conteudo: Conteudo)
+removerFavorito(conteudoId: int)
+listarFavoritos(): Favorito[]
+registrarVisualizacao(item: Reproduzivel, progressoSegundos: int)
+obterVisualizacao(item: Reproduzivel): Visualizacao
+listarHistorico(): Visualizacao[]
}

class Plano {
-planoId: int
-tipo: TipoPlano
-anunciosAtivos: boolean
-qualidadeConteudo: String
+ativarPremium()
+exibeAnuncios(): boolean
}

class Visualizacao {
-visualizacaoId: int
-item: Reproduzivel
-progressoSegundos: int
-dataUltimoAcesso: LocalDateTime
+atualizarProgresso(segundos: int)
}

class Favorito {
-favoritoId: int
-conteudo: Conteudo
-dataAdicao: LocalDate
}

class Catalogo {
-itens: Conteudo[]
+listar(perfil: Perfil): Conteudo[]
+buscar(termo: String, perfil: Perfil): Conteudo[]
+filtrar(genero: Genero, classificacao: String, perfil: Perfil): Conteudo[]
+ordenar(criterio: String, perfil: Perfil): Conteudo[]
+obterDetalhes(conteudoId: int, perfil: Perfil): Conteudo
}

abstract class Conteudo {
-conteudoId: int
-genero: Genero
-titulo: String
-sinopse: String
-classificacao: String
}

class Filme {
-duracaoSegundos: int
+reproduzir(perfil: Perfil)
+pausar()
}

class Serie {
-temporadas: Temporada[]
+listarEpisodios(): Episodio[]
}

class Temporada {
-temporadaId: int
-numero: int
-episodios: Episodio[]
}

class Episodio {
-episodioId: int
-numero: int
-titulo: String
-duracaoSegundos: int
+reproduzir(perfil: Perfil)
+pausar()
}

class Genero {
-generoId: int
-nome: String
}

class ServicoAutenticacao {
+cadastrar(nome: String, email: String, senha: String, dataNascimento: LocalDate): Conta
+login(email: String, senha: String): Conta
}

class ServicoRecomendacao {
+recomendar(perfil: Perfil, catalogo: Catalogo): Conteudo[]
}

Conta "1" *-- "1..3" Perfil
Conta "1" --> "1" Plano
Plano --> TipoPlano

Perfil "1" *-- "0..*" Favorito
Perfil "1" *-- "0..*" Visualizacao

Favorito --> "1" Conteudo
Visualizacao --> "1" Reproduzivel

Catalogo o-- "0..*" Conteudo

Conteudo <|-- Filme
Conteudo <|-- Serie
Reproduzivel <|.. Filme
Reproduzivel <|.. Episodio

Serie "1" *-- "1..*" Temporada
Temporada "1" *-- "1..*" Episodio

Conteudo --> "1" Genero

ServicoAutenticacao ..> Conta
ServicoRecomendacao ..> Perfil
ServicoRecomendacao ..> Catalogo
@enduml