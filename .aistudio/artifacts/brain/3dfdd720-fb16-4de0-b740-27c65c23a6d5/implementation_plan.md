# App Família — Central de Organização e Conexão Familiar

Aplicativo Android nativo em Jetpack Compose que corrige definitivamente as falhas de autenticação e conexão com o banco de dados do protótipo original, reunindo calendário compartilhado, mural de mensagens, metas financeiras, lista de compras, aniversários, telefones úteis, álbum de fotos e mural de documentos em um único espaço sincronizado em tempo real para toda a família.

### User Review & Critical Decisions

> [!IMPORTANT]
> **Diagnóstico Completo das Falhas Encontradas no Protótipo Original e Correções Definidas:**
> 1. **Falha Crítica de Autenticação (`request.auth == null`)**: O protótipo original salvava apenas o nome no `localStorage` e nunca autenticava o usuário no Firebase Authentication. Como o Firestore exige sessão autenticada nas regras de segurança, todas as leituras e gravações eram rejeitadas por falta de permissão (`PERMISSION_DENIED`). **Correção**: Implementação de autenticação real com **Google Sign-In via Jetpack Credential Manager** integrada ao Firebase Auth, seguida de configuração do perfil familiar (apelido e avatar).
> 2. **Falhas Silenciosas de Conexão e Queries**: As chamadas `addDoc` e `onSnapshot` não possuíam tratamento de exceções nem callbacks de erro, ocultando falhas de permissão e de índices em consultas ordenadas (`orderBy`). **Correção**: Camada de repositório reativa com Kotlin Coroutines/Flow, tratamento estruturado de erros e feedback visual claro (estados de carregamento, vazio, erro com botão de tentar novamente e confirmações).
> 3. **Bloqueio no Envio de Fotos e Documentos (Firebase Storage)**: O envio direto para o bucket falhava sem sessão autenticada e dependia de provisionamento externo de Storage. **Correção**: Integração com o seletor nativo do Android (Photo Picker e Document Picker) com compressão otimizada de imagens/miniaturas e armazenamento estruturado de metadados e registros compartilhados diretamente no Cloud Firestore provisionado para o app.

- **Decisão Confirmada 1 — Identificação do Membro**: Login seguro com conta Google (`CredentialManager`) combinado com personalização de perfil da família (escolha de como quer ser chamado, ex: "Mãe", "Pai", "Lucas", e emoji/avatar familiar).
- **Decisão Confirmada 2 — Espaço Compartilhado Único**: Todos os membros autenticados acessam e colaboram em tempo real no mesmo espaço familiar compartilhado no Firestore.
- **Decisão Confirmada 3 — Ações Completas (CRUD e Progresso)**: Além de adicionar e visualizar, todos os módulos contarão com ações para editar, excluir, marcar itens de compra como concluídos e adicionar novos valores às metas financeiras da família.

---

### 1. Overview & Core Concept

- **O Que Faz**: Oferece um ponto de encontro digital seguro e acolhedor para a família organizar a rotina doméstica, acompanhar objetivos financeiros, lembrar datas especiais, conversar no mural e acessar contatos, fotos e documentos importantes em tempo real.
- **Público-Alvo**: Membros de uma mesma família que desejam organização prática no dia a dia sem perder informações em conversas espalhadas.
- **Valor Principal**: Elimina os erros de sincronização e perda de dados do protótipo anterior ao substituir o login fictício por autenticação Google real vinculada a um banco de dados Cloud Firestore protegido por regras de segurança validadas.

---

### 2. User Experience & Visual Design

- **Fluxos Principais do Usuário**:
  1. **Boas-Vindas e Autenticação Google**: Ao abrir o aplicativo sem sessão ativa, o usuário vê um cartão acolhedor do *App Família* com o botão **"Entrar com o Google"**. Também há tentativa automática de login silencioso caso o usuário já tenha autorizado a conta anteriormente.
  2. **Personalização Rápida do Perfil Familiar**: No primeiro acesso após autenticar com o Google (ou ao tocar no perfil no cabeçalho), o usuário confirma seu apelido familiar (pré-preenchido com o nome da conta Google, podendo alterar para "Mãe", "Vó Maria", etc.) e escolhe um ícone representativo.
  3. **Painel Inicial (Home)**: Saudação dinâmica conforme o horário do dia (*"Bom dia, [Nome]!"*), resumo rápido do dia (próximos eventos/aniversários) e grade de 8 cartões coloridos para acesso rápido: **Calendário**, **Mensagens**, **Metas**, **Compras**, **Aniversários**, **Telefones**, **Fotos** e **Documentos**, além da barra de navegação inferior para alternância imediata entre as seções mais usadas.
  4. **Fluxos Interativos nos 8 Módulos**:
     - **Calendário ("O Nosso Mês")**: Grade mensal interativa com indicadores visuais nos dias com eventos; tocar em um dia filtra os compromissos daquele dia e permite adicionar, editar ou remover eventos.
     - **Mensagens (Mural / Chat)**: Bate-papo familiar em tempo real com balões diferenciados para as mensagens do próprio usuário e dos demais familiares, horário formatado e opção de apagar a própria mensagem.
     - **Metas da Família**: Cartões com ícone emoji, barra de progresso animada, percentual alcançado, botão rápido **"+ Guardar Valor"** para atualizar o saldo poupado e opções de editar ou excluir a meta.
     - **Lista de Compras**: Adição rápida de itens, caixa de seleção (checkbox) para marcar itens já comprados no mercado, filtro entre pendentes/comprados e exclusão.
     - **Aniversários**: Lista organizada por mês e dia com destaque para os aniversariantes do mês atual, permitindo adicionar, editar e excluir datas.
     - **Telefones Úteis**: Lista de contatos importantes e de emergência da família com botões de ação direta para abrir conversa no **WhatsApp** (`https://wa.me/...`) ou abrir o discador do telefone, além de edição e exclusão.
     - **Álbum de Fotos**: Seleção de fotos da galeria do celular via Android Photo Picker com compressão inteligente em Base64 compacto para sincronização imediata no Firestore, legenda, autor, visualização ampliada em tela cheia e exclusão.
     - **Documentos**: Registro de documentos e links/arquivos importantes da família com nome, categoria/resumo, anexo compacto ou link externo de acesso rápido, autor e opção de abrir/compartilhar ou excluir.

- **Identidade Visual & Tema**:
  - *Direção Estética*: **Acolhedora, Pastel Contemporânea e Familiar** — inspirada diretamente na paleta suave do protótipo original, aprimorada com profundidade, tipografia amigável e acabamento Material Design 3.
  - *Paleta de Cores*:
    - Primária: Azul Céu Suave (`#4A90E2` / container `#D6EAFF` inspirado em `#A2D2FF` com contraste acessível para leitura).
    - Secundária: Rosa Afeto (`#E06D94` / container `#FFE0EB` inspirado em `#FFAFCC`).
    - Destaque / Sucesso: Verde Menta (`#2E9E5B` / container `#D4F8DC` inspirado em `#B9FBC0` para barras de progresso e confirmações).
    - Superfícies dos 8 Cartões da Home: Tons pastéis distintos para cada categoria (`#EAF4F4`, `#F8EDEB`, `#E2ECE9`, `#FFF1E6`, `#FDE2E4`, `#E2EAFC`, `#FFECD1`, `#E8E8E4`).
  - *Tipografia*: Fonte arredondada e expressiva nos títulos e saudações combinada com fonte limpa de alta legibilidade para listas, valores e mensagens.
  - *Feedback Interativo & Movimento*: Animações suaves de transição entre abas, barras de progresso animadas nas metas, estados vazios ilustrados com dicas amigáveis e indicadores claros de sincronização e erro.

---

### 3. Key Product Decisions & Trade-Offs

- **Decisão 1: Autenticação Google Obrigatória + Perfil Familiar Editável**
  - *Abordagem Escolhida*: Autenticar a sessão via `CredentialManager` (`GetSignInWithGoogleOption`) no Firebase Auth e salvar um documento de perfil do membro no Firestore com seu apelido familiar.
  - *Por Quê*: Garante que todas as operações no banco possuam um `request.auth.uid` válido (resolvendo 100% dos erros de permissão do protótipo antigo), mantendo a simplicidade de exibir o nome/apelido escolhido pelo familiar em cada item criado.
  - *Alternativas Consideradas*: Manter apenas o campo de texto local sem Firebase Auth foi descartado pois impede o funcionamento seguro do Firestore; usar apenas o nome bruto da conta Google foi aprimorado para permitir apelidos afetivos da família.

- **Decisão 2: Fotos e Documentos Integrados ao Firestore**
  - *Abordagem Escolhida*: Utilizar o seletor nativo do Android com redimensionamento e compressão automática de imagens (JPEG otimizado em Base64 dentro do limite seguro de documentos do Firestore) e metadados/conteúdo estruturado para documentos.
  - *Por Quê*: O ambiente provisiona nativamente o Firestore Enterprise e o Firebase Auth. Ao armazenar imagens otimizadas e registros de documentos diretamente em coleções validadas do Firestore, a galeria de fotos e os documentos funcionam imediatamente em tempo real para toda a família sem erros de bucket não configurado no Firebase Storage.

- **Decisão 3: Regras de Segurança Zero-Trust para Espaço Familiar Compartilhado**
  - *Abordagem Escolhida*: Configurar regras de segurança (`firestore.rules`) que exigem usuário autenticado (`request.auth != null`) e validam rigorosamente o esquema de cada coleção (`profiles`, `calendar_events`, `messages`, `goals`, `shopping_items`, `birthdays`, `contacts`, `photos`, `documents`).
  - *Por Quê*: Permite que todos os membros autenticados da família visualizem e colaborem nas listas compartilhadas enquanto bloqueia qualquer acesso externo não autenticado ou dados malformados.

---

### 4. Technical Architecture & Data Strategy

- **Diagrama de Arquitetura e Componentes**:

```
┌──────────────────────────────────────────────────────────────────────────┐
│                        Camada de UI (Jetpack Compose)                    │
│                                                                          │
│  ┌──────────────────────┐         ┌───────────────────────────────────┐  │
│  │   AuthGate / Login   │────────▶│     Onboarding Perfil Familiar    │  │
│  │ (Google Credential)  │         │     (Apelido + Emoji Avatar)      │  │
│  └──────────────────────┘         └─────────────────┬─────────────────┘  │
│                                                     │                    │
│                                                     ▼                    │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │               Tela Principal + Navegação Inferior                  │  │
│  ├────────────────┬────────────────┬────────────────┬─────────────────┤  │
│  │  Home (Grade   │  Calendário &  │ Mural / Chat & │ Metas, Compras, │  │
│  │  8 Módulos)    │  Aniversários  │ Fotos / Docs   │ & Telefones     │  │
│  └───────┬────────┴───────┬────────┴───────┬────────┴────────┬────────┘  │
└──────────┼────────────────┼────────────────┼─────────────────┼───────────┘
           │                │                │                 │
           ▼                ▼                ▼                 ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                 FamilyViewModel (StateFlow + UI State)                   │
│   • Observa fluxos em tempo real (.snapshots()) apenas após autenticação │
│   • Gerencia estados de carregamento, diálogos de edição e erros         │
└────────────────────────────────────┬─────────────────────────────────────┘
                                     │
                                     ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                     FamilyRepository (Camada de Dados)                   │
│   • Instância nomeada do Firestore (R.string.firestore_database_id)      │
│   • Validação requireUserId() em todas as escritas                       │
│   • Tratamento estruturado de erros (handleFirestoreError)               │
└───────────────┬──────────────────────────────────────────┬───────────────┘
                │                                          │
                ▼                                          ▼
┌───────────────────────────────┐          ┌───────────────────────────────┐
│     Firebase Authentication   │          │   Cloud Firestore Database    │
│  (Google Sign-In Provider)    │          │ (Coleções Compartilhadas com  │
│                               │          │  Regras de Segurança Ativas)  │
└───────────────────────────────┘          └───────────────────────────────┘
```

- **Modelo de Dados no Cloud Firestore**:
  - `profiles/{uid}`: `uid`, `displayName` (apelido familiar), `avatarEmoji`, `email`, `updatedAt`.
  - `calendar_events/{eventId}`: `day`, `month`, `year`, `description`, `authorUid`, `authorName`, `createdAt`, `updatedAt`.
  - `messages/{messageId}`: `text`, `authorUid`, `authorName`, `authorEmoji`, `createdAt`.
  - `goals/{goalId}`: `name`, `iconEmoji`, `targetAmount`, `savedAmount`, `authorUid`, `authorName`, `createdAt`, `updatedAt`.
  - `shopping_items/{itemId}`: `name`, `quantityNote`, `completed`, `authorUid`, `authorName`, `createdAt`, `updatedAt`.
  - `birthdays/{birthdayId}`: `personName`, `day`, `monthIndex`, `monthName`, `authorUid`, `authorName`, `createdAt`, `updatedAt`.
  - `contacts/{contactId}`: `name`, `phoneNumber`, `category`, `authorUid`, `authorName`, `createdAt`, `updatedAt`.
  - `photos/{photoId}`: `caption`, `albumName`, `imageBase64`, `authorUid`, `authorName`, `createdAt`.
  - `documents/{docId}`: `title`, `notesOrUrl`, `fileName`, `mimeType`, `fileBase64`, `authorUid`, `authorName`, `createdAt`, `updatedAt`.

- **Mapeamento de Componentes Interativos e Estado**:
  - **Portão de Autenticação (`AuthGate`)**: Observa `FirebaseAuth.AuthStateListener`. Enquanto `currentUser == null`, nenhuma consulta ao Firestore é iniciada (evitando erros de permissão na inicialização). Quando autenticado, carrega o perfil em `profiles/{uid}` e inicia os fluxos em tempo real.
  - **Ordenação e Filtragem Segura**: Os fluxos em tempo real observam as coleções com `.snapshots()`, mapeando para modelos Kotlin tipados com `@DocumentId` e `@ServerTimestamp`, ordenando cronologicamente de forma segura e capturando qualquer exceção em `UiState.Error` com opção de reconexão.
  - **Validação e Persistência**: Todos os formulários (eventos, metas, compras, aniversários, telefones, fotos e documentos) validam campos obrigatórios antes de habilitar o envio, registram automaticamente `authorUid`, `authorName` e `FieldValue.serverTimestamp()`, e oferecem modais de edição e confirmação de exclusão.
