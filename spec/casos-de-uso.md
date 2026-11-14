## Casos de uso | Use Case (UC)

### UC1. Realizar Cadastro
- Usuário principal: Não autenticado.
- Objetivo: Acessar o sistema.
- Pré-condição: Não possuir uma conta ativa.
---
### UC2. Realizar Login
- Usuário principal: Não autenticado.
- Objetivo: Acessar o sistema.
- Pré-condição: O usuário precisa estar cadastrado no sistema.
---
### UC3. Gerenciar Perfis de Acesso
- Usuário principal: Titular da conta.
- Objetivo: Possibilitar que usuários espectadores acessem o sistema.
- Cada perfil deve informar a data de nascimento do espectador.
- Pré-condição: Estar autenticado como titular da conta.
---
### UC4. Assinar Plano Premium
- Usuário principal: Titular da conta.
- Objetivo: Possibilitar o consumo de conteúdo sem anúncios.
- Pré-condição: Estar autenticado como titular da conta.
---
### UC5. Acessar Catálogo
- Usuário principal: Espectador.
- Objetivo: Possibilitar a pesquisa (com filtros) de filmes e séries para assistir.
- Resultado: Apresentar apenas conteúdos adequados à idade do perfil.
- Pré-condição: Estar com um perfil selecionado.
---
### UC6. Reproduzir Conteúdo
- Usuário principal: Espectador.
- Objetivo: Assistir ao filme/série selecionado (iniciando do zero ou de onde parou)
- Pré-condição: O usuário precisa estar autenticado, estar com um perfil selecionado e ter escolhido um conteúdo disponível.
---
### UC7. Gerenciar Lista de Favoritos
- Usuário principal: Espectador.
- Objetivo: Possibilitar a inclusão, listagem, edição e exclusão de conteúdos da lista de favoritos.
- Pré-condição: Estar com um perfil selecionado.
---
### UC8. Obter Recomendações
- Usuário principal: Espectador.
- Objetivo: Receber sugestões personalizadas.
- Resultado: Recomendar apenas conteúdos adequados à idade do perfil.
- Pré-condição: Estar com um perfil selecionado.
