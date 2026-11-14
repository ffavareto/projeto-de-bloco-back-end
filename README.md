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

## Aplicação de console

O protótipo INFNETFLIX usa Java 17 ou superior e SQLite, acessado pelo driver JDBC `sqlite-jdbc`. Maven é usado para baixar as dependências, executar os testes e gerar um JAR executável. Não é necessário instalar o programa de linha de comando do SQLite.

```bash
mvn clean package
java -jar target/infnetflix-1.0.0.jar
```

Na primeira execução é criado `infnetflix.db` no diretório atual, com um catálogo fictício. Para escolher outro arquivo de banco:

```bash
java -jar target/infnetflix-1.0.0.jar caminho/para/banco.db
```

Para executar os testes: `mvn test`. Na primeira compilação, Maven precisa de acesso à internet para obter suas dependências. O arquivo SQLite guarda contas e dados dos perfis entre execuções; mantenha cópias de segurança se precisar preservar esses dados. Em um terminal interativo, a senha não aparece durante a digitação. Ao redirecionar a entrada, ela é lida como texto comum.

### Funcionalidades e decisões do protótipo

- Cadastro e login com senha de pelo menos oito caracteres, hash PBKDF2-HMAC-SHA256 com sal aleatório e consultas parametrizadas.
- Cada conta começa com um perfil; é possível manter de um a três, selecionar, renomear e excluir perfis. Cada perfil exige a data de nascimento do espectador no formato `dd/MM/aaaa`. A exclusão apaga seus favoritos e histórico.
- Catálogo demonstrativo de filmes e séries, busca por título, tipo, gênero e classificação, ordenação por título ou gênero e detalhes com temporadas e episódios. A busca aceita variações de maiúsculas, minúsculas e acentos (`última`, `ÚLTIMA` e `ultima` encontram o mesmo filme); a ordenação usa as regras de comparação do português. O catálogo, os detalhes e as recomendações respeitam a idade do perfil e a classificação do conteúdo.
- Favoritos e histórico independentes por perfil. Na lista de favoritos, informe um ID para remover ou digite `editar` para substituir um título por outro. A substituição preserva a data de adição e rejeita conteúdo duplicado ou incompatível com a idade.
- A reprodução é simulada: informe quantos segundos foram assistidos para salvar o progresso, por filme ou por episódio. Ao retornar, é possível continuar ou reiniciar. O plano gratuito exibe um anúncio fictício e limita cada sessão a 20 minutos; Premium remove essas limitações. No modelo, os avanços de uma mesma sessão são acumulados para aplicar esse limite.
- A ativação do Premium é apenas uma simulação, sem valor, cobrança ou gateway de pagamento. Recomendações priorizam gêneros do histórico com progresso positivo e excluem títulos já iniciados; sem histórico, seguem a ordem alfabética.

Este é um protótipo local acadêmico: não reproduz vídeo real, não implementa pagamento, API, HTTPS, limitação de requisições ou monitoramento de serviço, e não oferece garantias de disponibilidade, tempo de resposta ou concorrência. Há logs de erros no console. O uso de hash e consultas parametrizadas não estabelece por si só conformidade com a LGPD; a aplicação não implementa todos os processos necessários para isso. Os requisitos originais para plataformas web, Android e TV e infraestrutura de produção permanecem fora do escopo desta versão de console.

### Organização do código

A aplicação segue MVC adaptado ao console:

- `model`: classes do domínio e suas regras, conforme o [diagrama de classes](spec/diagrama-de-classes.md). `Conteudo` é abstrato; `Filme` e `Serie` são suas subclasses. `Filme` e `Episodio` implementam `Reproduzivel`. Séries agrupam `Temporada` e `Episodio`, e cada conteúdo possui um `Genero`.
- `view`: menus, leitura das entradas e apresentação dos resultados; não executa SQL.
- `controller`: coordena a sessão e as operações entre interface, domínio, serviços e repositórios. Revalida perfil, item e plano ao salvar uma reprodução.
- `service`: `ServicoAutenticacao` realiza cadastro/login; `ServicoRecomendacao` usa o histórico do perfil e o catálogo adequado à idade.
- `repository`: criação do banco e acesso aos dados SQLite com consultas parametrizadas.
- `security`: criação e verificação dos hashes de senha.
- `App`: ponto de entrada, que monta as dependências e inicia a interface.

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

Os testes verificam os fluxos do console, as regras do domínio e a integração com SQLite temporário ou em memória. Incluem busca Unicode, limite acumulado de reprodução, recomendações por perfil, edição de favoritos, restrição etária, reabertura do banco e preservação de dados durante a identificação de gêneros e temporadas.
