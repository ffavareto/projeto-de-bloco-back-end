# INFNETFLIX

Plataforma de streaming que funciona no Navegador / WEB, Smartphone / Sistema Android e Smart TVs, 
com possibilidade de uso gratuito ou premium (pago). No INFNETFLIX, o usuário pode criar uma conta 
com até 3 perfis, procurar por filmes e séries por meio de filtros e ordenações deste catálogo, e, 
ao começar assistir um conteúdo, se o usuário parar de assistir, ele pode voltar mais tarde e continuar 
de onde parou, e conforme consome mais conteúdos as recomendações que o INFNETFLIX vão ficando mais 
de acordo com o gosto do usuário.

## Requisitos Funcionais

- O usuário, para que possa fazer uso do sistema, poderá realizar um cadastro;
- O usuário poderá realizar login em sua conta cadastrada anteriormente;
- O usuário poderá adicionar até 3 perfis diferentes na conta cadastrada;
- O cadastro e a criação de perfis devem receber a data de nascimento do respectivo espectador;
- O catálogo e as recomendações devem apresentar apenas conteúdos compatíveis com a idade do perfil selecionado;
- O usuário poderá selecionar um dos perfis criados na conta cadastrada;
- O usuário poderá excluir ou editar os perfis cadastrados na conta;
- O usuário poderá visualizar um catálogo de filmes e séries;
- O usuário poderá fazer buscas de filmes e séries através de filtros e ordenações;
- O usuário poderá visualizar detalhes de um filme ou série antes de reproduzir o conteúdo selecionado;
- O usuário poderá assistir filmes e séries de forma gratuita, porém com limitações e anúncios;
- O usuário poderá assistir filmes e séries por meio de uma assinatura paga, sem anúncios ou limitações;
- O usuário poderá continuar assistindo um filme ou série de onde parou anteriormente;
- O usuário poderá salvar um filme ou série como favorito;
- O usuário poderá visualizar uma lista de filmes e séries que foram favoritados por ele;
- O usuário poderá remover filmes e séries da lista de favoritos;
- O usuário poderá receber recomendações de filmes e séries com base em seu histórico de consumo;

## Requisitos Não Funcionais

- O sistema deve estar em conformidade com a LGPD;
- O sistema deve ter certa disponibilidade, ao menos um budget de 99% de disponibilidade ao ano;
- O sistema deve armazenar as senhas dos usuários utilizando um algoritmo seguro de hash;
- O sistema deve contar com mecanismos de proteção contra SQL Injection;
- O sistema deve conter limitação de requisições para preservar a API contra abuso de sobrecargas;
- O sistema deve conter mecanismos mínimos de observabilidade (logs, monitoramento de erros);
- O sistema deve utilizar algum gateway de pagamento homologado no Brasil;
- O sistema deve responder às operações de navegação e consulta em um tempo máximo definido, por exemplo, até 3 segundos em condições normais;
- O sistema deve utilizar comunicação segura por meio de HTTPS;
- O sistema deve ser capaz de suportar vários usuários simultâneos sem comprometer muito seu desempenho.
