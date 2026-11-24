### Projeto de bloco - Back-End

- Aluno: Lucas Santos
- Supervisão: Professor Orlando Fonseca - Instituto Infnet
- Linguagem de Programação escolhida: Java
- Metodologia escolhida: Ágil - Scrum

---

#### Observação importante:

Utilizei Inteligência Artificial para o desenvolvimento do projeto, a parte da escrita de código especificamente.

Sei que para quem está aprendendo sobre desenvolvimento de software isso não é bom, mas esse não é o meu caso, atualmente eu conto com 7 anos de experiência com desenvolvimento de software, não é muito, mas eu já superei a fase do aprendizado mais básico, por isso, conscientemente, me dei o direito de delegar a escrita do código para a IA, a parte dos requisitos fui eu quem fiz (mesmo que nesse processo também tive ajuda da IA em alguns momentos), a maneira que fiz é bem parecida com a qual eu trabalho no dia a dia do meu emprego.

---

## Interface de linha de comando e API REST local

O INFNETFLIX roda em dois processos Java 17 ou superior. O console envia requisições HTTP e recebe respostas JSON da API REST. O servidor executa as regras da aplicação e acessa o SQLite via JDBC. O Maven gera um JAR executável que pode iniciar qualquer um dos dois processos.

Compile e execute os testes com o Maven.

```bash
mvn clean package
```

Em um terminal, inicie o servidor e aguarde a mensagem com o endereço da API.

```bash
java -jar target/infnetflix-1.0.0.jar servidor
```

Em outro terminal, inicie o console.

```bash
java -jar target/infnetflix-1.0.0.jar
```

A API escuta apenas em `127.0.0.1`, na porta `8080` por padrão. O servidor cria `infnetflix.db` no diretório de execução com um catálogo fictício. Para usar outro arquivo de banco e outra porta, execute os dois comandos em terminais separados.

```bash
java -jar target/infnetflix-1.0.0.jar servidor caminho/para/banco.db 8090
java -jar target/infnetflix-1.0.0.jar 8090
```

Use `mvn test` para executar só os testes. O Maven precisa obter as dependências na primeira compilação. Após isso, o servidor e o console podem rodar sem internet. O SQLite guarda contas e dados dos perfis entre execuções. Em um terminal interativo, a senha é digitada de forma oculta. Ao redirecionar a entrada, ela é lida como texto comum.

### Rotas da API

O cadastro e o login recebem JSON. As datas de nascimento na API usam `aaaa-MM-dd`. O login retorna um token que deve ser enviado no cabeçalho `Authorization: Bearer <token>` nas rotas da conta e do perfil. O servidor valida se o perfil indicado na URL pertence à conta autenticada.

| Método e caminho | Operação |
| --- | --- |
| `GET /saude` | Verificar se o servidor está acessível |
| `POST /contas` | Cadastrar uma conta com `nome`, `email`, `senha` e `dataNascimento` |
| `POST /sessoes` e `DELETE /sessao` | Receber um token com `email` e `senha` ou revogá-lo |
| `GET /perfis` e `POST /perfis` | Listar perfis ou criar um perfil com `nome` e `dataNascimento` |
| `GET /perfis/{perfil}`, `PATCH /perfis/{perfil}` e `DELETE /perfis/{perfil}` | Consultar, renomear com `nome` ou excluir um perfil |
| `GET /perfis/disponibilidade` e `GET /perfis/{perfil}/exclusao` | Validar a criação ou a exclusão antes de solicitar os dados no console |
| `GET /plano` e `PUT /plano/premium` | Consultar o plano ou ativar o Premium simulado |
| `GET /perfis/{perfil}/catalogo` | Buscar por `titulo`, `tipo`, `genero` e `classificacao`. O parâmetro `ordem` aceita `TITULO` ou `GENERO`. |
| `GET /perfis/{perfil}/catalogo/{titulo}` | Consultar os detalhes do título |
| `GET` e `POST /perfis/{perfil}/catalogo/{titulo}/itens/{episodio}/reproducao` | Consultar os limites e o progresso ou registrar um avanço com `reiniciar` e `segundos`. Use episódio `0` para filmes. |
| `GET /perfis/{perfil}/favoritos` | Listar favoritos |
| `PUT`, `PATCH` e `DELETE /perfis/{perfil}/favoritos/{titulo}` | Adicionar, substituir pelo `id` informado ou remover um favorito |
| `GET /perfis/{perfil}/historico` e `GET /perfis/{perfil}/recomendacoes` | Consultar o histórico ou as recomendações do perfil |

### Funcionalidades e decisões do protótipo

- Cadastro e login com senha de pelo menos oito caracteres, hash PBKDF2-HMAC-SHA256 com sal aleatório e consultas parametrizadas.
- Cada conta começa com um perfil; é possível manter de um a três, selecionar, renomear e excluir perfis. Cada perfil exige a data de nascimento do espectador no formato `dd/MM/aaaa`. A exclusão apaga seus favoritos e histórico.
- Catálogo demonstrativo de filmes e séries, busca por título, tipo, gênero e classificação, ordenação por título ou gênero e detalhes com temporadas e episódios. A busca aceita variações de maiúsculas, minúsculas e acentos (`última`, `ÚLTIMA` e `ultima` encontram o mesmo filme); a ordenação usa as regras de comparação do português. O catálogo, os detalhes e as recomendações respeitam a idade do perfil e a classificação do conteúdo.
- Favoritos e histórico independentes por perfil. Na lista de favoritos, informe um ID para remover ou digite `editar` para substituir um título por outro. A substituição preserva a data de adição e rejeita conteúdo duplicado ou incompatível com a idade.
- A reprodução é simulada: informe quantos segundos foram assistidos para salvar o progresso, por filme ou por episódio. Ao retornar, é possível continuar ou reiniciar. O plano gratuito exibe um anúncio fictício e limita cada sessão a 20 minutos; Premium remove essas limitações. No modelo, os avanços de uma mesma sessão são acumulados para aplicar esse limite.
- A ativação do Premium é apenas uma simulação, sem valor, cobrança ou gateway de pagamento. Recomendações priorizam gêneros do histórico com progresso positivo e excluem títulos já iniciados; sem histórico, seguem a ordem alfabética.

Este é um protótipo acadêmico com reprodução e assinatura simuladas. A API usa HTTP no endereço de loopback e registra erros no terminal do servidor. O uso de hash e consultas parametrizadas não estabelece por si só conformidade com a LGPD. Os requisitos originais para plataformas web, Android e TV e infraestrutura de produção permanecem fora do escopo desta versão de console.

### Organização do código

A interface de console acessa o controlador por meio da API REST.

- `model`: classes do domínio e suas regras, conforme o [diagrama de classes](spec/diagrama-de-classes.md). `Conteudo` é abstrato; `Filme` e `Serie` são suas subclasses. `Filme` e `Episodio` implementam `Reproduzivel`. Séries agrupam `Temporada` e `Episodio`, e cada conteúdo possui um `Genero`.
- `view` apresenta os menus, lê as entradas e exibe as respostas da API.
- `api` contém o servidor HTTP, o cliente do console e as representações JSON.
- `controller` coordena as operações do domínio com os serviços e repositórios. Revalida perfil, item e plano ao salvar uma reprodução.
- `service`: `ServicoAutenticacao` realiza cadastro/login; `ServicoRecomendacao` usa o histórico do perfil e o catálogo adequado à idade.
- `repository`: criação do banco e acesso aos dados SQLite com consultas parametrizadas.
- `security`: criação e verificação dos hashes de senha.
- `App` inicia o servidor ou a interface de console conforme os argumentos.

### Correspondência com os casos de uso

| Casos de uso | Implementação |
| --- | --- |
| UC1 e UC2 — cadastro e login | `ServicoAutenticacao`, `Conta.validarSenha` e `ContaRepository` |
| UC3 — gerenciar perfis | `Conta` mantém de um a três `Perfil`, valida seleção, edição e exclusão |
| UC4 — assinatura simulada | `Conta.assinarPremium`, `Plano` e `TipoPlano`; o plano é compartilhado pelos perfis da conta |
| UC5 — catálogo | `Catalogo` aplica filtros, ordenação e classificação etária |
| UC6 — reprodução simulada | `Reproduzivel`, `Reproducao` e `Visualizacao` controlam início, pausa e retomada |
| UC7 — favoritos | `Perfil` gerencia inclusão, listagem, substituição e remoção de `Favorito` |
| UC8 — recomendações | `ServicoRecomendacao` prioriza gêneros consumidos e exclui títulos já iniciados |

Em bancos existentes com data de nascimento nos perfis, gêneros e temporadas são identificados automaticamente, preservando contas, perfis, favoritos e progresso. Bancos sem a coluna ou com perfis sem data de nascimento não são compatíveis. Objetos carregados do banco usam suas identidades persistidas; o plano usa a identidade da conta, e favoritos e visualizações usam a identidade de suas linhas no SQLite.

Os testes verificam os fluxos do console através de requisições HTTP reais, a autorização por conta e perfil, as regras do domínio e a integração com SQLite temporário ou em memória. Incluem busca Unicode, limite acumulado de reprodução, recomendações por perfil, edição de favoritos, restrição etária, reabertura do banco e preservação de dados durante a identificação de gêneros e temporadas.
