# 📅 Studio Agendamentos — Sistema de Agendamento de Serviços com IA & Sincronização de Calendário

> **Trabalho Acadêmico de Pós-Graduação**  
> **Instituição:** FIAP  
> **Disciplina:** Engenharia de Software 2.0 — IA Assistant  

---

## 📖 1. Visão Geral do Projeto

O **Studio Agendamentos** é um MVP (Produto Mínimo Viável) desenvolvido como parte da disciplina **Engenharia de Software 2.0 - IA Assistant**. O sistema simula um aplicativo de agendamento de procedimentos estéticos e de bem-estar, focado em resolver problemas comuns de marcação de horários, detecção de conflitos de agenda e integração futura com calendários externos (Google Calendar e Microsoft Outlook).

O diferencial pedagógico deste projeto é que sua concepção arquitetural, modelagem de dados, regras de negócio e desenvolvimento Full Stack (Java/Spring Boot e Vanilla JS) foram guiados por **Arquivos de Contexto (`.ai-backend/` e `.ai-frontend/`)**, servindo como base de conhecimento padronizada para assistentes de Inteligência Artificial atuarem como Arquiteto de Software, Tech Lead e Desenvolvedor Sênior.

---

## 🎯 2. Principais Funcionalidades

### 🔹 Para o Cliente
* **Catálogo de Procedimentos:** Visualização de serviços disponíveis com duração em minutos, preço formatado e profissional responsável.
* **Reserva Rápida de Horários:** Escolha de data e horário de início com cálculo automático de término com base na duração do serviço.
* **Autenticação Leve (Login por E-mail):** Sessão mantida no navegador via `localStorage` sem complexidade de cadastro extenso no MVP.
* **Dashboard Pessoal ("Minha Agenda"):** Painel exclusivo exibindo os próximos atendimentos do cliente autenticado.
* **Ações de Autoatendimento:**
  * **Cancelar Reserva:** Libera imediatamente o horário na agenda do profissional.
  * **Reagendar Horário:** Permite escolher uma nova data/hora, validando se o profissional possui disponibilidade livre.

### 🔹 Regras de Negócio e Segurança
* **Detecção Inteligente de Conflito (HTTP 409 Conflict):** O sistema impede matematicamente que dois agendamentos ativos ocupem o mesmo intervalo de tempo para o mesmo profissional:
  $$\text{Novo.início} < \text{Existente.fim} \quad \text{e} \quad \text{Novo.fim} > \text{Existente.início}$$
* **Controle de Propriedade (Ownership - HTTP 403 Forbidden):** Um cliente só tem autorização para visualizar, cancelar ou reagendar suas próprias reservas.
* **Seed Automático de Dados:** A aplicação inicializa pronta para testes com 2 profissionais, 2 clientes, 4 serviços e um agendamento pré-cadastrado no banco em memória.

---

## 🛠️ 3. Tecnologias Utilizadas

### Backend
* **Java 21 (LTS):** Utilização de Virtual Threads (Project Loom), Records e Sequenced Collections.
* **Spring Boot 3.3.4:**
  * *Spring Web:* API RESTful e mapeamento de endpoints.
  * *Spring Data JPA / Hibernate:* Persistência relacional desacoplada.
  * *Jakarta Bean Validation:* Validação declarativa de entrada de dados.
* **H2 Database:** Banco de dados relacional em memória com console web (`/h2-console`).
* **Ports & Adapters (Arquitetura Hexagonal):** Interface `CalendarSyncPort` com implementação mock para desacoplar provedores externos de calendário.
* **Springdoc OpenAPI 3 (Swagger UI):** Documentação interativa e testes de API no navegador.
* **JUnit 5 / Spring Boot Test:** Suíte de testes automatizados de integração com 100% de sucesso.

### Frontend
* **Vanilla JavaScript (ES6+):** Zero-build, sem necessidade de Node.js, Webpack ou Babel.
* **Bootstrap 5.3 & Bootstrap Icons (via CDN):** Design responsivo mobile-first.
* **Fetch API Nativa:** Comunicação assíncrona com envio de headers de identificação (`X-User-Id`).
* **Design System Sofisticado:** Paleta de cores moderna em azul marinho profundo (`#00011d`), modais interativos e sistema de Toasts flutuantes para feedback visual imediato.

---

## 📂 4. Estrutura do Repositório

```text
├── .ai-backend/                     # Base de conhecimento e contexto da IA para o Backend
│   ├── architecture.md              # Camadas, Ports & Adapters e segurança
│   ├── business-rules.md            # Regras de negócio, conflito de horários e ownership
│   ├── standards.md                 # Padrões Java 21, DTOs e tratamento de exceções
│   └── tech-stack.md                # Justificativa técnica do backend
├── .ai-frontend/                    # Base de conhecimento e contexto da IA para o Frontend
│   ├── architecture.md              # Estrutura modular, Fetch API e localStorage
│   ├── tech-stack.md                # Especificação de tecnologias do frontend
│   └── ui-ux.md                     # Paleta de cores, mobile-first e componentes
├── src/main/java/com/app/scheduling/
│   ├── config/                      # Configurações de CORS, Swagger e Seed de dados
│   ├── controller/                  # Endpoints REST (/api/appointments, /api/services, /api/auth)
│   ├── dto/                         # Records imutáveis de entrada e saída
│   ├── exception/                   # Tratamento global de erros (@RestControllerAdvice)
│   ├── integration/                 # Ports & Adapters para calendários (MockAdapter)
│   ├── model/                       # Entidades JPA (User, Service, Appointment)
│   ├── repository/                  # Repositórios Spring Data JPA
│   └── service/                     # Lógica de negócio e validações
├── src/main/resources/static/       # Frontend servido diretamente pelo Spring Boot
│   ├── css/styles.css
│   ├── js/api.js
│   ├── js/app.js
│   └── index.html
├── pom.xml                          # Dependências Maven
└── README.md                        # Documentação do projeto
```

---

## 🚀 5. Como Executar o Projeto

### Pré-requisitos
* **Java 21** instalado na máquina.

### Executando a Aplicação Completa

1. Abra o terminal na raiz do projeto e execute o comando:
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```
   *(No Linux/macOS: `./mvnw spring-boot:run`)*

2. Após a mensagem de inicialização do Spring Boot, acesse no navegador:
   * 🌐 **Aplicação Web (Frontend):** [http://localhost:8080](http://localhost:8080)
   * 📚 **Documentação da API (Swagger UI):** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
   * 🗄️ **Console do Banco H2:** [http://localhost:8080/h2-console](http://localhost:8080/h2-console)  
     *(JDBC URL: `jdbc:h2:mem:schedulingdb` | Usuário: `admin` | Senha: `admin`)*

---

## 🧪 6. Cenários de Teste Rápido no Frontend

1. **Login de Teste:**
   * Clique em **"Entrar"** na barra superior e selecione uma das contas mockadas (ex.: `mariana.souza@email.com`).
   * A seção **"Minha Agenda"** será exibida com o agendamento pré-cadastrado.
2. **Teste de Conflito de Agenda (HTTP 409):**
   * Tente agendar o serviço *"Manicure Tradicional"* para **amanhã às 14:00**.
   * O sistema exibirá o **Toast Vermelho** avisando que a profissional já possui atendimento nesse período.
3. **Agendamento em Horário Livre:**
   * Selecione um horário vago (ex.: amanhã às 16:00).
   * O sistema exibirá o **Toast Verde**, fechará o modal e adicionará a reserva na sua agenda.
4. **Reagendamento ou Cancelamento:**
   * No card do seu agendamento, clique em **"Reagendar"** para testar a alteração de horário ou em **"Cancelar"** para liberar a vaga.

---

## 🎓 7. Conclusão Acadêmica

Este projeto demonstra na prática a aplicação de conceitos avançados de **Engenharia de Software 2.0**:
- Uso de **Context-Driven Development (CDD com LLMs)**, onde diretrizes arquiteturais claras orientam o desenvolvimento assistido por IA.
- Aderência aos princípios de **Clean Code**, arquitetura em camadas e **Ports & Adapters**.
- Desenvolvimento Full Stack coeso, desacoplado e pronto para evolução em nuvem.
