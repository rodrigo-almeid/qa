# LoanTrack — Especificação do Aplicativo

> Aplicativo Android pessoal para controle e acompanhamento de empréstimos informais.

---

## Visão Geral

| Item | Detalhe |
|---|---|
| **Nome** | LoanTrack |
| **Plataforma** | Android (mínimo API 26 / Android 8.0) |
| **Linguagem** | Kotlin |
| **UI** | Jetpack Compose + Material 3 |
| **Arquitetura** | MVVM + Clean Architecture (data / domain / ui) |
| **Backend** | Firebase Firestore + Firebase Auth |
| **DI** | Hilt |
| **Tarefas em background** | WorkManager |

---

## Stack Técnica

| Camada | Tecnologia |
|---|---|
| UI | Jetpack Compose, Material 3, Navigation Compose |
| ViewModel | AndroidViewModel, StateFlow, Hilt |
| Persistência remota | Firebase Firestore (Kotlin SDK) |
| Autenticação | Firebase Auth + Google Sign-In OAuth 2.0 |
| Background jobs | WorkManager + HiltWorker |
| Biometria | androidx.biometric:biometric |

---

## Arquitetura

```
app/
├── data/
│   ├── model/          # Loan, LoanStatus, PaymentType
│   └── repository/     # LoanRepository (interface) + LoanRepositoryImpl (Firestore)
├── domain/
│   └── usecase/        # GetLoans, SaveLoan, DeleteLoan, MarkPaid, PartialPayment, UpdateOverdueStatus
├── ui/
│   ├── dashboard/      # Lista de empréstimos + resumo financeiro
│   ├── loanform/       # Cadastro e edição de empréstimo
│   ├── loandetail/     # Detalhes + dar baixa
│   ├── login/          # Login Google + compliance LGPD
│   ├── profile/        # Tela de perfil / conta
│   ├── navigation/     # AppNavigation (NavHost)
│   └── theme/          # Color, Typography, Theme
├── util/               # DateUtils, CurrencyUtils, SecurityPrefs,
│                       # BiometricHelper, BrazilianPhoneTransformation,
│                       # CurrencyVisualTransformation
├── worker/             # LoanStatusWorker
└── di/                 # AppModule, FirebaseModule, WorkManagerModule
```

---

## Modelo de Dados — `Loan`

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | String | ID do documento Firestore |
| `userId` | String | UID do usuário Firebase |
| `debtorName` | String | Nome do devedor |
| `contact` | String | Telefone formatado `(XX) XXXXX-XXXX` |
| `loanDate` | String | Data do empréstimo — formato `YYYY-MM-DD` (UTC) |
| `dueDate` | String | Data de vencimento — formato `YYYY-MM-DD` (UTC); padrão: loanDate + 1 mês |
| `originalDueDate` | String | Vencimento original antes de edições — `YYYY-MM-DD` |
| `paymentDate` | String? | Data do pagamento — `YYYY-MM-DD` ou null |
| `amountLent` | Long | Valor emprestado em centavos (ex.: R$ 10,50 → `1050`) |
| `amountToReceive` | Long | Valor a receber em centavos |
| `amountPaid` | Long | Valor já pago em centavos |
| `paymentType` | String | Sempre `SINGLE` |
| `status` | String | `PENDING`, `PAID`, `OVERDUE`, `PARTIAL` |
| `notes` | String? | Observações livres |
| `createdAt` | Timestamp | Data de criação |
| `updatedAt` | Timestamp | Última atualização |

> **Datas como string `YYYY-MM-DD`:** evita deslocamento de dia por fuso horário em dispositivos com timezone diferente do servidor.
> **Valores em centavos (`Long`):** elimina erros de arredondamento de ponto flutuante em somatórios — converter para `BigDecimal` apenas na camada de UI/formatação.

### Subcoleção `payments` (histórico de pagamentos parciais)

Cada documento `Loan` possui uma subcoleção `payments` no Firestore:

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | String | ID do documento |
| `amount` | Long | Valor pago em centavos |
| `date` | String | Data do pagamento — `YYYY-MM-DD` |
| `notes` | String? | Observação livre |

Permite rastrear a evolução da dívida quando há múltiplos pagamentos parciais em datas distintas.

---

## Telas

### 1. Login

**Fluxo:**
- Primeiro acesso: exibe checkbox de aceite de Termos e Política de Privacidade; botão desabilitado até aceitar.
- Aceite gravado em SharedPreferences com timestamp.
- Login via Google OAuth 2.0.
- Após primeiro login: pergunta se quer ativar biometria.
- Acessos seguintes com biometria ativa: exibe prompt biométrico automaticamente.
- Rodapé com links clicáveis para Termos de Uso e Política de Privacidade.

**Rate limiting:**
- Máximo de 5 tentativas falhas (login Google ou biometria).
- Após o limite: bloqueio de 15 minutos com contagem regressiva na tela.
- Contador zerado automaticamente após o período.

**Componentes:**
- `LoginScreen.kt` — tela principal
- `LoginViewModel.kt` — estado + lógica
- `LegalDialog.kt` — modal com Termos e Política (LGPD)
- `SecurityPrefs.kt` — SharedPreferences de segurança
- `BiometricHelper.kt` — wrapper do `BiometricPrompt`

---

### 2. Dashboard (Lista de Empréstimos)

**Funcionalidades:**
- Cards com: nome do devedor, valor a receber, vencimento, status colorido.
- Abas de filtro: Todos / Pendentes / Atrasados / Pagos.
- Busca por nome ou contato.
- Ordenação: por vencimento, valor ou nome.
- Cards de resumo: total em aberto e recebido no mês.
- Swipe/botão "Dar Baixa" direto do card (confirmação via dialog).
- FAB para novo empréstimo.

---

### 3. Cadastro / Edição de Empréstimo

**Campos:**
| Campo | Comportamento |
|---|---|
| Nome do devedor | Texto livre |
| Contato | Máscara `(XX) XXXXX-XXXX` — estado guarda só dígitos (máx 11) |
| Data do empréstimo | DatePicker |
| Data de vencimento | DatePicker — padrão = data empréstimo + 1 mês; editável |
| Valor emprestado | Máscara `R$ 0,00` — entrada estilo calculadora (direita→esquerda) |
| Valor a receber | Idem |
| Observações | Texto livre opcional |

**Validações:**
- Nome obrigatório.
- Vencimento não pode ser anterior à data do empréstimo.
- Valor a receber deve ser > 0.
- Alerta se valor a receber < valor emprestado.

---

### 4. Detalhe do Empréstimo

- Exibe todos os campos do empréstimo.
- Dar Baixa Total.
- Pagamento Parcial (campo de valor).
- Reabrir empréstimo (reverter baixa).
- Excluir empréstimo (confirmação).
- Navegar para edição — **botão oculto quando status = PAID**.
- Aviso informativo exibido quando status = PAID: "Reabra o empréstimo para editar."

---

### 5. Perfil / Conta

**Acesso:** ícone `AccountCircle` no canto superior direito do TopAppBar do Dashboard.

**Informações:**
- Avatar: foto do perfil Google ou iniciais do nome.
- Nome: campo editável (atualizado via `FirebaseAuth.updateProfile`).
- E-mail: somente leitura (da conta Google autenticada).
- Biometria: Switch para ativar/desativar autenticação biométrica.
- Data de aceite LGPD: exibição informativa.

**Ações:**
- Botão "Salvar Alterações" — atualiza o Firebase Auth profile com feedback via Snackbar.
- Botão "Sair da Conta" — desconecta do Firebase Auth e redireciona para Login.

**Componentes:**
- `ProfileScreen.kt`, `ProfileViewModel.kt`
- `UpdateUserProfileUseCase.kt`

---

## Regra de Negócio — Bloqueio de Edição para Status PAID

| Ponto | Comportamento |
|---|---|
| `LoanDetailScreen` | Botão Editar oculto; aviso informativo exibido |
| `LoanFormScreen` | Formulário em modo somente leitura; botão Salvar oculto |
| `SaveLoanUseCase` | Bloqueia save via `IllegalStateException` se registro no Firestore for PAID |

---

## Funcionalidades de Background

### WorkManager

| Worker | Frequência | Função |
|---|---|---|
| `LoanStatusWorker` | Diária | Atualiza status para `OVERDUE` quando vencimento passou |

---

## Testabilidade

- `BiometricHelper`, `SecurityPrefs` e `DateUtils` expostos como **interfaces** injetadas via Hilt, permitindo Mocks em testes unitários e de UI.
- **Testes Unitários** de UseCases: validação de cálculo de baixa parcial, transição de status, somatórios em centavos.
- **Testes de UI** com `ComposeTestRule`: fluxos de cadastro, dar baixa, pagamento parcial.
- **Testes de transformação**: `CurrencyVisualTransformation` e `BrazilianPhoneTransformation` verificados com entradas de borda (vazio, máximo de dígitos, formatação).

---

## Segurança e Compliance

### Autenticação
- Google OAuth 2.0 via Firebase Auth.
- Tokens JWT gerenciados pelo Firebase (access token de curta duração + refresh token).
- Biometria local opcional (`BiometricPrompt` — BIOMETRIC_STRONG | DEVICE_CREDENTIAL).

### Rate Limiting (client-side)
- Máximo de 5 tentativas de autenticação falhas.
- Bloqueio temporário de 15 minutos após o limite.
- Implementado em `SecurityPrefs` via **`EncryptedSharedPreferences`** (`androidx.security:security-crypto`) — impede leitura dos contadores em dispositivos com root ou via backup ADB.

### Armazenamento
- Dados de empréstimos: Firebase Firestore (criptografado em trânsito via TLS e em repouso).
- Dados locais sensíveis (rate-limiting, aceite LGPD): **`EncryptedSharedPreferences`** com chave gerenciada pelo Android Keystore.
- Firestore offline persistence habilitada (`persistentCacheSettings`) — o app funciona sem conexão e sincroniza ao reconectar.

### LGPD (Lei 13.709/2018)
- Aceite explícito de Termos de Uso e Política de Privacidade no primeiro acesso.
- Timestamp de aceite gravado em SharedPreferences.
- Dialogs com conteúdo completo disponíveis a qualquer momento no rodapé da tela de login.
- Contato do DPO: `almeidainteligencia@gmail.com`.

---

## Ícone do App

- Fundo: verde escuro `#1B5E20` (Material Green 900).
- Foreground: nota de dinheiro branca com símbolo R$ e detalhes em verde.
- Formato: Android adaptive icon (vector drawable).

---

## Paleta de Cores

| Uso | Cor |
|---|---|
| Primary | Verde Material (definido no Theme.kt) |
| Background ícone | `#1B5E20` |
| Status Pendente | Amarelo/Laranja |
| Status Atrasado | Vermelho |
| Status Pago | Verde |
| Status Parcial | Azul |

---

## Branch de Desenvolvimento

```
repositório : rodrigo-almeid/qa
branch      : claude/android-loan-tracking-app-7jdc14
```
