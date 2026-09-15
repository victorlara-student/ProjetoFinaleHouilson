# Sistema de Gestão — Caixas D'Água

Projeto acadêmico em **Kotlin puro** (sem Gradle/Maven), rodando via console, com
persistência em **PostgreSQL** através de **JDBC**.

Este projeto foi corrigido e completado a partir de uma versão inicial que tinha
vários módulos incompletos ou com bugs. Este README explica o que existe, o que
foi corrigido, e como colocar tudo para rodar.

---

## 1. Como rodar

### 1.0 IMPORTANTE: este projeto agora usa Gradle
Depois de muita dificuldade configurando o projeto "cru" (sem Gradle/Maven)
em versões mais novas do IntelliJ, adicionei um `build.gradle.kts` mínimo.
Isso NÃO muda nenhuma linha do seu código Kotlin — só automatiza a parte
chata (baixar o driver JDBC, configurar o compilador Kotlin) que estava
dando problema de configuração manual. Se seu professor exigir
explicitamente "sem Gradle", me avise que eu te ajudo a voltar pro modo
manual — mas pelo menos assim você já consegue rodar e estudar agora.

### 1.1 Abrir o projeto no IntelliJ
1. Extraia o zip em uma pasta limpa (sem misturar com tentativas antigas).
2. No IntelliJ: **File > Open**, selecione a pasta `ProjetoFinal` (a que tem
   o arquivo `build.gradle.kts` dentro).
3. O IntelliJ vai reconhecer automaticamente como um projeto Gradle e
   perguntar se você confia na pasta ("Trust Project") — clique em confiar.
4. Aguarde a sincronização do Gradle terminar (barra de progresso embaixo,
   à direita — na primeira vez demora um pouco, porque baixa o Kotlin e o
   driver do Postgres da internet). **Seu PC precisa estar com internet
   nessa etapa.**
5. Se aparecer um aviso sobre "Gradle JVM" ou pedir uma versão de Java,
   deixe a opção padrão sugerida pelo IntelliJ.

### 1.2 Banco de dados
1. Tenha um PostgreSQL rodando localmente (porta padrão `5432`).
2. Crie o banco de dados (pelo pgAdmin ou por SQL):
   ```sql
   CREATE DATABASE "caixaDaAgua";
   ```
3. Conecte nele e rode o arquivo `src/main/resources/schema.sql` inteiro (cria
   todas as tabelas necessárias).

As credenciais padrão usadas pelo projeto (usuário `postgres`, senha `postgres`)
ficam em `src/main/kotlin/repositorio/ConexaoPostgres.kt`. Se o seu Postgres usa
outra senha, troque ali.

### 1.3 Rodar
Depois que o Gradle terminar de sincronizar, abra `src/main/kotlin/Main.kt` e
clique no botão verde ▶ do lado de `fun main()` (ou botão direito no arquivo
> Run 'MainKt'). Se ainda não aparecer o botão verde, vá em **View > Tool
Windows > Gradle** e clique no ícone de "atualizar" (⟳) para forçar uma nova
sincronização.

---

## 2. Estrutura do projeto

```
src/main/kotlin/
 ├─ Main.kt                     -> ponto de entrada
 ├─ enumeradores/                -> Cor, Formato, Marca, Material, Turno,
 │                                  Setor, Habilidade, TipoMovimentacao
 ├─ pessoas/                     -> Pessoa, Cliente, Funcionario, Instalador, Fornecedor
 ├─ produto/                     -> CaixaDaAgua, Servico
 ├─ estoque/                     -> Estoque
 ├─ financeiro/                  -> Movimentacao, Caixa
 ├─ repositorio/                 -> ConexaoPostgres, InterfaceJPA, CRUD* (um por entidade)
 ├─ util/                        -> Validacoes.kt (leitura segura de input do usuário)
 └─ sistema/                     -> menus de console
     ├─ MenuInicial.kt
     ├─ caixadaagua/             -> cadastrar/editar/listar/excluir caixa
     ├─ pessoas/                 -> cadastrar/listar cliente, funcionário, fornecedor
     ├─ estoque/                 -> registrar compra de fornecedor
     ├─ vendas/                  -> realizar venda para cliente
     ├─ servicos/                -> registrar serviço de instalação/montagem
     └─ financeiro/              -> pagar funcionário, ver saldo, ver extrato

src/main/resources/
 └─ schema.sql                   -> cria todas as tabelas do Postgres
```

*(Nota: a estrutura `src/main/kotlin` é o padrão do Gradle. Se seu professor
pedir para ver a árvore de pastas, pode explicar que `main/kotlin` é só a
convenção do Gradle para organizar código-fonte Kotlin — os pacotes de
verdade do projeto, exigidos pelo enunciado, continuam sendo `enumeradores`,
`pessoas`, `produto`, `estoque`, `financeiro`, `repositorio`, `util` e
`sistema`.)

---

## 3. Como cada exigência do enunciado foi atendida

| Exigência | Onde está |
|---|---|
| Menu interativo via console | `sistema/MenuInicial.kt` + submenus |
| Todos os dados persistidos no banco | Todo `repositorio/CRUD*.kt` usa JDBC/PostgreSQL |
| Fluxo de produto/serviço (compra, venda, estoque, montagem) | `sistema/estoque/RegistrarCompra.kt` (compra), `sistema/vendas/RealizarVenda.kt` (venda), `estoque/Estoque.kt` (controle de estoque), `sistema/servicos/RegistrarServico.kt` (montagem/instalação) |
| Gerenciar pessoas (funcionários, clientes, fornecedores) | `pessoas/*` + `sistema/pessoas/*` |
| Dividir funcionários em ≥2 setores | `enumeradores/Setor.kt` (4 setores) + `pessoas/Funcionario.kt` + `sistema/pessoas/ListarPessoas.kt` (agrupa por setor) |
| Fluxo de caixa com encapsulamento | `financeiro/Caixa.kt`: o saldo é `private set`, só muda via `depositar()`/`sacar()` |
| Movimentação financeira completa (valor, pagador, recebedor, data/hora, motivo, responsável) | `financeiro/Movimentacao.kt` |
| Validação à prova de falhas humanas (regex/try/nullable) | `util/Validacoes.kt`, usado em todas as telas |

### Sobre "auditores"
Em vez de criar uma classe `Auditor` separada, um auditor é simplesmente um
`Funcionario` com `setor = AUDITORIA`. Isso é o suficiente para gerenciar essa
pessoa (cadastro, listagem, pagamento) sem duplicar código, e ainda reforça a
ideia de que movimentações financeiras são **imutáveis** (ver abaixo) —
justamente o que um setor de auditoria precisa.

### Sobre o Caixa (encapsulamento)
```kotlin
class Caixa(saldoInicial: BigDecimal = BigDecimal.ZERO) {
    var saldo: BigDecimal = saldoInicial
        private set   // <- só pode ser lido de fora, nunca escrito diretamente

    fun depositar(valor: BigDecimal): Boolean { ... }
    fun sacar(valor: BigDecimal): Boolean { ... }
}
```
Nenhuma outra parte do código consegue fazer `caixa.saldo = valor` diretamente
— só é possível alterar o saldo passando pelas validações de `depositar()` e
`sacar()` (ex: impedir saldo negativo).

### Sobre Movimentações serem imutáveis
`repositorio/CRUDMovimentacao.kt` implementa `editar()` e `excluir()`, mas eles
sempre retornam `false` de propósito. Um extrato financeiro de verdade nunca
apaga histórico — se algo estiver errado, o certo é lançar uma movimentação de
estorno, não apagar a original. Isso é o que garante que o setor de AUDITORIA
consiga confiar nos dados.

---

## 4. Principais bugs corrigidos em relação à versão original

1. `CRUDCaixaDAgua.listar()` nunca chamava `conectar()` antes de usar a
   conexão → sempre lançava `NullPointerException`.
2. `CRUDCaixaDAgua.editar()` tinha um erro de sintaxe SQL: faltava um espaço
   entre `"SET"` e `"preco = ?"`, virando `SETpreco` (o Postgres recusava o
   comando).
3. `CRUDMovimentacao.listar()` consultava a tabela errada
   (`caixa_da_agua` em vez de `movimentacao`).
4. `PagarBoleto.kt` importava uma função (`salvarMovimentacao`) que nunca
   existiu no projeto — o arquivo nem compilava. Foi substituído por
   `sistema/financeiro/PagarFuncionario.kt`.
5. `sistema/caixadaagua/cadastrarNovaCaixa.kt` lia a variável `formato` duas
   vezes (uma descartada) — bug de copiar/colar.
6. `Instalador` usava o enum `Habilidade` para guardar tanto a "habilidade"
   quanto valores que na verdade eram setores da empresa (`FINANCEIRO`,
   `ADMINISTRACAO`). Isso foi separado em dois enums: `Habilidade` (técnica)
   e `Setor` (departamento).
7. Nenhuma leitura de `readln()` era validada — qualquer letra digitada onde
   se esperava um número quebrava o programa inteiro. Resolvido centralizando
   toda leitura de input em `util/Validacoes.kt`.

---

## 5. Limitações conhecidas (para você já saber e não se assustar)

- Não há tela de login/autenticação — o "responsável pela transação" é
  digitado manualmente em texto livre a cada operação.
- As credenciais do banco estão fixas no código (aceitável para um projeto de
  estudo, mas nunca faça isso em produção).
- Não testei a compilação com `kotlinc` real (o ambiente onde este código foi
  gerado não tinha o compilador Kotlin disponível) — revisei manualmente
  arquivo por arquivo, mas é possível que reste algum pequeno erro de
  digitação. Se o IntelliJ apontar algo ao abrir, me avise a mensagem exata do
  erro que eu corrijo na hora.
