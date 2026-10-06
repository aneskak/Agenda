# Padrões e Convenções de Código (Standards)

Este documento define as diretrizes de codificação, padrões arquiteturais e boas práticas a serem seguidas no desenvolvimento backend com **Java 21** e **Spring Boot 3.x**.

---

## 1. Nomenclatura e Convenções

- **Classes e Interfaces:** `PascalCase` (ex.: `AppointmentController`, `UserService`, `CalendarSyncPort`).
- **Métodos e Variáveis:** `camelCase` (ex.: `findAvailableSlots`, `scheduledDateTime`, `appointmentRepository`).
- **Constantes:** `UPPER_SNAKE_CASE` (ex.: `DEFAULT_PAGE_SIZE`, `MAX_APPOINTMENT_DURATION_MINUTES`).
- **Pacotes:** `lowercase` sem separadores (ex.: `com.app.appointment.controller`, `com.app.appointment.dto`).
- **Endpoints REST:** `kebab-case`, substantivos no plural (ex.: `/api/v1/appointments`, `/api/v1/services/{serviceId}`).

---

## 2. Uso Obrigatório de DTOs (Data Transfer Objects)

- **Nunca expor entidades de domínio (`@Entity`) diretamente** em endpoints REST (nem como `@RequestBody`, nem como `@ResponseBody`).
- **Requisições:** Devem utilizar DTOs específicos de entrada (ex.: `CreateAppointmentRequest`, `UpdateServiceRequest`).
  - Utilizar anotações de Bean Validation (`@NotNull`, `@NotBlank`, `@Future`, `@Min`, etc.).
  - Os parâmetros no Controller devem ser validados com `@Valid`.
- **Respostas:** Devem utilizar DTOs específicos de saída ou Records (ex.: `AppointmentResponse`, `UserSummaryResponse`).
  - *Recomendação:* Utilizar Java `record` para DTOs imutáveis de resposta, aproveitando o *Record Patterns* do Java 21 para desestruturação quando necessário.
- **Mapeamento:** O mapeamento entre Entidades e DTOs deve ser feito de forma explícita via métodos de conversão, mappers dedicados ou bibliotecas (como MapStruct), isolando as camadas.

---

## 3. Tratamento de Exceções Global com `@ControllerAdvice`

- As exceções da aplicação devem ser centralizadas em uma classe anotada com `@RestControllerAdvice` (ex.: `GlobalExceptionHandler`).
- **Estrutura de Erro Padronizada:** Todo erro retornado pela API deve seguir um padrão claro (estilo RFC 7807 / Problem Details):
  ```json
  {
    "timestamp": "2026-10-04T19:30:00Z",
    "status": 409,
    "error": "Conflict",
    "message": "Profissional já possui um agendamento ativo no período selecionado.",
    "path": "/api/v1/appointments",
    "details": []
  }
  ```
- **Tratamentos Obrigatórios:**
  - `MethodArgumentNotValidException` (400 - Validação de campos da requisição).
  - Exceções de Domínio/Negócio (ex.: `BusinessException`, `ScheduleConflictException`) mapeadas para status apropriados (400, 409, 422).
  - `ResourceNotFoundException` (404 - Entidade não encontrada).
  - Exceções inesperadas (`Exception.class`) mapeadas para 500 com log estruturado sem vazar dados sensíveis da infraestrutura.

---

## 4. Manipulação de Datas e Horários

- **Proibido o uso de `java.util.Date` ou `java.util.Calendar`.**
- **Utilizar estritamente a API `java.time`:**
  - `LocalDateTime`: Para representação de horários locais quando o fuso horário for padronizado pela aplicação.
  - `ZonedDateTime` ou `OffsetDateTime`: **Obrigatório** para eventos que dependem de fuso horário e sincronização com calendários externos (ex.: Google Calendar, Microsoft Outlook).
  - `Duration`: Para representação de durações de serviços.
- **Formato de Serialização:** Todos os dados de data/hora na API devem seguir o formato **ISO-8601** (ex.: `2026-10-04T14:30:00Z` ou `yyyy-MM-dd'T'HH:mm:ssXXX`).

---

## 5. Boas Práticas e Clean Code

- **Injeção de Dependências:** Preferir injeção via construtor (utilizando `@RequiredArgsConstructor` do Lombok) em vez de `@Autowired` em atributos privados.
- **Imutabilidade:** Declarar dependências e atributos como `final` sempre que aplicável.
- **Camada Transacional:** Anotar métodos de escrita na camada de serviço com `@Transactional`, e métodos de apenas leitura com `@Transactional(readOnly = true)`.
