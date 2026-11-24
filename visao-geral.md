# Visão geral do INFNETFLIX

O INFNETFLIX é um protótipo de streaming em Java. O usuário interage com um console que acessa a API REST na mesma máquina. A conta reúne os perfis e o plano. Cada perfil tem sua própria lista de favoritos, seu histórico e recomendações adequadas à sua idade.

## Componentes e responsabilidades

```mermaid
flowchart TD
    Usuario["Usuário"]
    subgraph Cliente["Processo do console"]
        Console["ConsoleView<br/>Lê as escolhas e exibe os resultados"]
        Http["InfnetflixClient<br/>Envia requisições e lê respostas JSON"]
        Console --> Http
    end

    subgraph Servidor["Processo do servidor"]
        API["ServidorRest<br/>Valida o token e encaminha a operação"]
        Controlador["InfnetflixController<br/>Coordena as operações da conta e do perfil"]
        API --> Controlador
    end

    subgraph Regras["Regras da aplicação"]
        Autenticacao["ServicoAutenticacao e Senhas<br/>Cadastro, login e hash da senha"]
        Conta["Conta, Perfil e Plano<br/>Limite de perfis, idade, favoritos e benefícios do plano"]
        Catalogo["Catalogo, Filme e Serie<br/>Busca, filtros e detalhes de títulos"]
        Reproducao["Reproducao e Visualizacao<br/>Retomada, limite da sessão e progresso"]
        Recomendacao["ServicoRecomendacao<br/>Sugestões a partir dos gêneros assistidos"]
    end

    subgraph Dados["Persistência"]
        Contas["ContaRepository<br/>Contas, perfis e plano"]
        Titulos["CatalogoRepository<br/>Títulos, temporadas, episódios e favoritos"]
        Historico["VisualizacaoRepository<br/>Histórico e progresso"]
        Database["Database<br/>Conexão e consultas parametrizadas"]
        SQLite[("SQLite · infnetflix.db")]

        Contas --> Database
        Titulos --> Database
        Historico --> Database
        Database -->|JDBC| SQLite
    end

    Usuario --> Console
    Http -->|"HTTP em 127.0.0.1"| API
    Controlador --> Autenticacao
    Controlador --> Conta
    Controlador --> Catalogo
    Controlador --> Reproducao
    Controlador --> Recomendacao
    Controlador -->|Carrega ou grava dados| Dados
    Autenticacao --> Contas
    Recomendacao -->|Consulta títulos adequados ao perfil| Catalogo
```

## Navegação pelo console

```mermaid
flowchart TD
    Inicial["Menu inicial"]
    Cadastro["Cadastrar conta e criar o primeiro perfil"]
    Fim["Encerrar o programa"]
    Conta["Menu da conta"]
    Perfis["Criar, editar ou excluir perfis"]
    Plano["Consultar o plano ou ativar o Premium simulado"]
    Perfil["Menu do perfil selecionado"]
    Catalogo["Catálogo e busca<br/>Filtrar por título, tipo, gênero ou classificação<br/>Ordenar por título ou gênero"]
    Detalhes["Detalhes e reprodução<br/>Consultar sinopse, duração ou episódios pelo ID do título"]
    Reproduzir["Reproduzir filme ou episódio<br/>Continuar ou reiniciar"]
    MarcarFavorito["Adicionar ou remover este título dos favoritos"]
    Favoritos["Favoritos<br/>Listar, substituir ou remover títulos"]
    Historico["Histórico<br/>Ver o progresso por filme ou episódio"]
    Recomendacoes["Recomendações<br/>Ver sugestões para o perfil"]

    Inicial -->|Cadastrar| Cadastro
    Cadastro --> Inicial
    Inicial -->|Login válido| Conta
    Inicial -->|Sair| Fim
    Conta -->|Sair da conta| Inicial
    Conta --> Perfis
    Conta --> Plano
    Conta -->|Selecionar perfil| Perfil
    Perfil -->|Gerenciar perfil| Conta
    Perfil --> Catalogo
    Perfil --> Detalhes
    Perfil --> Favoritos
    Perfil --> Historico
    Perfil --> Recomendacoes
    Detalhes --> Reproduzir
    Detalhes --> MarcarFavorito
```

O catálogo mostra o ID que o usuário pode informar em **Detalhes e reprodução**. A idade do perfil limita quais títulos aparecem no catálogo e nas recomendações, e também quais detalhes podem ser abertos.

## Como o sistema retoma e salva um filme

```mermaid
sequenceDiagram
    actor Usuario as Usuário
    participant Console as ConsoleView e InfnetflixClient
    participant API as ServidorRest
    participant Controle as InfnetflixController
    participant Modelo as Perfil e Reproducao
    participant Repos as Repositórios
    participant Banco as SQLite

    Usuario->>Console: Seleciona Reproduzir nos detalhes do filme
    Console->>API: GET do recurso de reprodução com token e ID do perfil
    API->>Controle: prepararReproducao
    Controle->>Repos: Busca perfil, plano, filme e histórico
    Repos->>Banco: Executa as consultas
    Banco-->>Repos: Dados armazenados
    Repos-->>Controle: Perfil, filme e progresso salvo
    Controle->>Modelo: Verifica a idade e inicia a reprodução simulada
    Modelo-->>Controle: Sessão com progresso e limite disponível
    Controle-->>API: Dados da sessão
    API-->>Console: Progresso e limites em JSON
    Console-->>Usuario: Mostra o progresso salvo e oferece reiniciar

    Usuario->>Console: Escolhe se quer reiniciar e informa os segundos assistidos
    Console->>API: POST no recurso de reprodução com o avanço em JSON
    API->>Controle: registrarReproducao
    Controle->>Controle: Prepara a sessão novamente com os dados atuais
    Controle->>Modelo: Aplica a escolha e valida o avanço

    alt Avanço válido
        Modelo-->>Controle: Visualização atualizada ao pausar
        Controle->>Repos: Salva o progresso do perfil
        Repos->>Banco: Insere ou atualiza a visualização
        Controle-->>API: Novo ponto de parada
        API-->>Console: Progresso salvo em JSON
        Console-->>Usuario: Exibe o progresso ou a conclusão
    else Avanço fora do intervalo
        Modelo-->>Controle: Erro de validação
        Controle-->>API: Erro de validação
        API-->>Console: HTTP 400 e mensagem em JSON
        Console-->>Usuario: Exibe o aviso
    end
```

O plano gratuito exibe um anúncio simulado e permite até 20 minutos por sessão. O Premium simulado remove esse limite. O SQLite identifica cada progresso pelo perfil e pelo título. Para séries, registra também o episódio, de modo que cada episódio possa ser retomado separadamente.

[Casos de uso](spec/casos-de-uso.md) · [Diagrama de classes](spec/diagrama-de-classes.md)
