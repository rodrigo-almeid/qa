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
| E-mail | JavaMail for Android (smtp.gmail.com:587, STARTTLS) |
| Exportação | Apache POI (HSSFWorkbook — formato .xls) |
| Biometria | androidx.biometric:biometric |
| Compartilhamento | FileProvider + Android Share Sheet |

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
│   ├── settings/       # Configurações de e-mail
│   ├── navigation/     # AppNavigation (NavHost)
│   └── theme/          # Color, Typography, Theme
├── util/               # DateUtils, CurrencyUtils, EmailConfig, SecurityPrefs,
│                       # BiometricHelper, ExportUtils, BrazilianPhoneTransformation,
│                       # CurrencyVisualTransformation
├── worker/             # EmailReportWorker, LoanStatusWorker
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
| `loanDate` | Timestamp | Data do empréstimo |
| `dueDate` | Timestamp | Data de vencimento (padrão: loanDate + 1 mês) |
| `originalDueDate` | Timestamp | Vencimento original (antes de edições) |
| `paymentDate` | Timestamp? | Data do pagamento (null se não pago) |
| `amountLent` | Double | Valor emprestado em R$ |
| `amountToReceive` | Double | Valor a receber em R$ |
| `amountPaid` | Double | Valor já pago em R$ |
| `paymentType` | String | Sempre `SINGLE` |
| `status` | String | `PENDING`, `PAID`, `OVERDUE`, `PARTIAL` |
| `notes` | String? | Observações livres |
| `createdAt` | Timestamp | Data de criação |
| `updatedAt` | Timestamp | Última atualização |

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
- Botão de exportar (ícone de compartilhamento) → gera XLS e abre Share Sheet.
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
- Navegar para edição.

---

### 5. Configurações

- Campo de e-mail remetente (padrão: `almeidainteligencia@gmail.com`).
- Campo de senha de app do Gmail (armazenada em SharedPreferences — **nunca hardcoded**).
- Botão "Salvar configurações".
- Botão "Enviar relatório agora" (dispara `EmailReportWorker` via WorkManager).

---

## Funcionalidades de Background

### WorkManager

| Worker | Frequência | Função |
|---|---|---|
| `LoanStatusWorker` | Diária | Atualiza status para `OVERDUE` quando vencimento passou |
| `EmailReportWorker` | Diária | Envia relatório HTML por e-mail para o usuário logado |

### Relatório por E-mail
- Protocolo: SMTP Gmail (`smtp.gmail.com:587`, STARTTLS).
- Remetente: configurável nas Configurações.
- Destinatário: e-mail da conta Google do usuário logado.
- Conteúdo: resumo de empréstimos ativos, atrasados e pagos em HTML.

---

## Exportação XLS

- Botão na barra superior do Dashboard (ícone de compartilhamento).
- Bottom sheet com filtro: Todos / Pendentes / Atrasados / Pagos.
- Gera arquivo `.xls` (Apache POI — HSSF) com colunas:
  - Devedor, Contato, Valor Emprestado, Valor a Receber, Data Empréstimo, Vencimento, Status, Observações.
- Abre Share Sheet do Android: WhatsApp, Gmail, Drive, etc.
- Arquivo salvo temporariamente em `cacheDir/exports/` via `FileProvider`.

---

## Segurança e Compliance

### Autenticação
- Google OAuth 2.0 via Firebase Auth.
- Tokens JWT gerenciados pelo Firebase (access token de curta duração + refresh token).
- Biometria local opcional (`BiometricPrompt` — BIOMETRIC_STRONG | DEVICE_CREDENTIAL).

### Rate Limiting (client-side)
- Máximo de 5 tentativas de autenticação falhas.
- Bloqueio temporário de 15 minutos após o limite.
- Implementado em `SecurityPrefs` via SharedPreferences.

### Armazenamento
- Dados de empréstimos: Firebase Firestore (criptografado em trânsito via TLS e em repouso).
- Configurações do app: SharedPreferences local.
- Senha de app Gmail: SharedPreferences — **nunca** armazenada em texto no código-fonte.

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
