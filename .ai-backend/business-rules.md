# Regras de Negócio e Modelo de Domínio (Business Rules)

Este documento descreve o modelo de domínio, entidades e as regras de negócio essenciais que governam o comportamento do **Aplicativo de Agendamentos de Serviços**.

---

## 1. Entidades Principais do Domínio

### 1.1. `User` (Usuário)
Representa as pessoas cadastradas na plataforma. Pode assumir o papel de **Cliente** ou de **Profissional Prestador de Serviços**.
- **Atributos principais:**
  - `id`: Identificador único (Long ou UUID).
  - `name`: Nome completo do usuário.
  - `email`: Endereço de e-mail (único no sistema).
  - `phone`: Número de telefone para contato.
  - `role`: Perfil do usuário (`CLIENT`, `PROFESSIONAL` ou `ADMIN`).
  - `active`: Indicador se o usuário está ativo no sistema.

### 1.2. `Service` (Serviço Oferecido)
Representa um tipo de serviço prestado por um profissional (ex.: "Nail Design", "Corte de Cabelo", "Tratamento Capilar", "Manicure Tradicional").
- **Atributos principais:**
  - `id`: Identificador único.
  - `name`: Nome do serviço.
  - `description`: Descrição detalhada do que está incluso.
  - `durationMinutes`: Duração do serviço em minutos (ex.: 30, 45, 60, 90).
  - `price`: Valor financeiro cobrado (`BigDecimal`).
  - `professional`: Referência ao `User` responsável pela prestação do serviço.
  - `active`: Indicador se o serviço está disponível para novos agendamentos.

### 1.3. `Appointment` (Agendamento)
Representa uma reserva de horário entre um Cliente e um Profissional para um Serviço específico.
- **Atributos principais:**
  - `id`: Identificador único.
  - `client`: Referência ao `User` que contratou o serviço.
  - `professional`: Referência ao `User` que executará o serviço.
  - `service`: Referência ao `Service` agendado.
  - `startDateTime`: Data e hora de início do agendamento (`ZonedDateTime` ou `LocalDateTime`).
  - `endDateTime`: Data e hora de término do agendamento (calculada automaticamente a partir de `startDateTime + service.durationMinutes`).
  - `status`: Estado atual do agendamento (`SCHEDULED`, `CONFIRMED`, `COMPLETED`, `CANCELLED`).
  - `externalCalendarEventId`: Identificador do evento gerado no calendário sincronizado (Mock/Google/Microsoft).
  - `notes`: Observações adicionais informadas pelo cliente.

---

## 2. Regras de Negócio Obrigatórias

### Regra 1: Conflito de Horários do Profissional
> **Enunciado:** Um horário não pode ser agendado se já houver outro agendamento ativo no mesmo período para o mesmo profissional.

- **Definição de Conflito:**
  Existe sobreposição de horários se, para o mesmo profissional e em agendamentos que **não** estejam com status `CANCELLED`, ocorrer a condição:
  $$\text{Novo.startDateTime} < \text{Existente.endDateTime} \quad \text{e} \quad \text{Novo.endDateTime} > \text{Existente.startDateTime}$$
- **Ação em caso de conflito:**
  - A requisição de criação ou reagendamento deve ser abortada.
  - Lançar uma exceção de domínio (ex.: `ScheduleConflictException`).
  - O `GlobalExceptionHandler` deve responder com código **HTTP 409 Conflict**, retornando uma mensagem clara ao usuário.

---

### Regra 2: Carga Inicial de Dados (Seed / Mocking para MVP)
> **Enunciado:** O MVP deve iniciar com dados mockados (profissionais e serviços já cadastrados no banco ao subir a aplicação) usando um arquivo `data.sql` ou um `CommandLineRunner`.

- **Objetivo:** Permitir que o desenvolvedor ou avaliador execute o projeto e possa imediatamente consultar serviços disponíveis e criar agendamentos pelo Swagger UI sem passos manuais de pré-cadastro.
- **Implementação Recomendada:**
  - Utilizar um `@Component` que implemente `CommandLineRunner` (ex.: `DataInitializerRunner`) ou arquivo `src/main/resources/data.sql`.
  - Inserir ao menos:
    - 2 Profissionais (ex.: "Ana Silva - Manicure e Nail Design", "Carlos Oliveira - Cabeleireiro").
    - 2 Clientes (ex.: "Mariana Souza", "Lucas Pereira").
    - 4 Serviços com durações distintas (ex.: 30 min e 60 min) associados aos profissionais.

---

### Regra 3: Cálculo Automático do Término do Agendamento
- O cliente/solicitante envia apenas o `serviceId` e o `startDateTime` (e opcionalmente `endDateTime`).
- A camada de serviço calcula automaticamente o `endDateTime`:
  $$\text{endDateTime} = \text{startDateTime} + \text{service.durationMinutes}$$
- Isso garante que a duração do atendimento respeite exatamente as características do serviço cadastrado.

---

### Regra 4: Cancelamento e Liberação de Horário
- Um agendamento cancelado tem seu status alterado para `CANCELLED`.
- Agendamentos com status `CANCELLED` deixam de ser considerados na verificação de conflito de horários (Regra 1), liberando a agenda do profissional para novas marcações.
- Ao cancelar, a porta de integração de calendário (`CalendarSyncPort`) deve ser acionada para remover ou atualizar o evento correspondente no calendário externo.

---

### Regra 5: Autenticação de Usuário (Login por E-mail no MVP)
> **Enunciado:** Um usuário deve poder fazer login fornecendo seu e-mail cadastrado.

- O endpoint `POST /api/auth/login` recebe o e-mail do usuário.
- O sistema valida se o usuário existe na base e se está ativo (`active = true`).
- Retorna os dados do usuário (`id`, `name`, `email`, `role`) juntamente com um identificador de sessão/token JWT simplificado para o frontend armazenar localmente (ex.: `localStorage` ou `sessionStorage`).
- Caso o e-mail não seja encontrado ou o usuário esteja inativo, responde com **HTTP 401 Unauthorized** ou **HTTP 404 Not Found**.

---

### Regra 6: Isolamento de Agendamentos e Propriedade (Ownership)
> **Enunciado:** Um usuário só pode visualizar, cancelar ou reagendar os SEUS PRÓPRIOS agendamentos.

- O dashboard do cliente consulta os agendamentos via `GET /api/appointments/me`, retornando apenas agendamentos futuros associados ao seu `userId`.
- Nas operações de cancelamento (`DELETE /api/appointments/{id}`) e reagendamento (`PUT /api/appointments/{id}/reschedule`), caso o contexto do usuário solicitante seja informado, o sistema valida se `appointment.getClient().getId().equals(authenticatedUserId)`.
- Se a reserva pertencer a outro usuário, a operação deve ser rejeitada com código **HTTP 403 Forbidden** (ou `UnauthorizedOperationException`).

---

### Regra 7: Reagendamento de Horário (Reschedule)
> **Enunciado:** O reagendamento (alterar data/hora) só é permitido se o novo horário estiver livre e for no futuro.

- O cliente envia `PUT /api/appointments/{id}/reschedule` com a nova data/hora de início (`newStartTime`) e opcionalmente de término (`newEndTime`).
- **Validações Obrigatórias:**
  1. O novo horário de início deve ser estritamente no futuro (`newStartTime > LocalDateTime.now()`).
  2. O agendamento não pode estar com status `CANCELLED`.
  3. Verificação de conflito: o profissional responsável não pode ter outro agendamento ativo que sobreponha o novo intervalo `[newStartTime, newEndTime]`, **desconsiderando o próprio agendamento atual que está sendo modificado**.
- **Resultado:**
  - Se houver conflito: Lança `ScheduleConflictException` e responde com **HTTP 409 Conflict**.
  - Se estiver livre: Atualiza `startDateTime` e `endDateTime`, notifica o calendário externo (`calendarSyncPort.updateEvent(...)`), persiste e retorna o agendamento atualizado com status **HTTP 200 OK**.
