# Stack Tecnológica do Backend MVP (Tech Stack)

Este documento especifica a stack tecnológica adotada para o MVP do backend do **Aplicativo de Agendamentos de Serviços**. A seleção prioriza agilidade no desenvolvimento, facilidade de execução local sem dependências de infraestrutura externa e aderência a padrões modernos do ecossistema Java.

---

## 1. Tecnologias Principais

| Componente | Versão / Ferramenta | Finalidade |
| :--- | :--- | :--- |
| **Linguagem** | **Java 21 (LTS)** | Threads Virtuais (Project Loom), Record Patterns, Pattern Matching for switch e Sequenced Collections. |
| **Framework Base** | **Spring Boot 3.x** | Base para microsserviços e APIs corporativas, alinhado com Jakarta EE e suporte nativo a Virtual Threads. |
| **Apresentação REST** | **Spring Boot Starter Web** | Construção de APIs RESTful utilizando Spring MVC e servidor Tomcat embarcado. |
| **Persistência** | **Spring Boot Starter Data JPA** | Abstração de persistência relacional com Hibernate e Spring Data. |
| **Validação** | **Spring Boot Starter Validation** | Validação declarativa de DTOs e entidades via Jakarta Bean Validation (`@Valid`, `@NotNull`, etc.). |
| **Banco de Dados** | **H2 Database (In-Memory)** | Banco de dados relacional em memória para execução local imediata sem necessidade de Docker/instalações. |
| **Documentação da API** | **Springdoc OpenAPI (Swagger UI)** | Documentação automática dos endpoints e interface web interativa para testes no navegador. |
| **Redução de Boilerplate** | **Project Lombok** | Geração automática de getters, setters, construtores e builders via anotações. |
| **Build & Dependências** | **Maven** | Gerenciamento de ciclo de vida e dependências do projeto. |

---

## 2. Detalhamento e Justificativas de Decisão

### 2.1. Java 21 (LTS)
- **Motivação Arquitetural:** O Java 21 é a versão LTS mais moderna suportada pelo ecossistema Spring Boot 3. Ele traz inovações de linguagem e runtime que transformam a produtividade e a escalabilidade da aplicação:
  - **Virtual Threads (Project Loom - JEP 444):** Permite alta escalabilidade para operações bloqueantes de I/O (consultas ao banco de dados JPA e chamadas HTTP a APIs externas como Google Calendar e Microsoft Graph) com um modelo de programação síncrono e limpo, sem o overhead de threads de plataforma e sem a complexidade reativa do WebFlux (habilitável no Spring Boot via `spring.threads.virtual.enabled=true`).
  - **Record Patterns (JEP 440) & Pattern Matching for `switch` (JEP 441):** Facilita a desestruturação de DTOs imutáveis (`records`) e tratamento exaustivo de status e regras de domínio (ex.: estados de agendamento `SCHEDULED`, `CONFIRMED`, `CANCELLED`).
  - **Sequenced Collections (JEP 431):** Fornece interfaces padronizadas (`getFirst()`, `getLast()`, `reversed()`) para manipulação de listas e ordenação de slots de horários na agenda.
  - **Longevidade e Suporte (LTS):** Garante ciclo de suporte oficial de longo prazo e perfeita sinergia com o Spring Boot 3.2+.

### 2.2. Spring Boot 3.x
- **Motivação:** Framework de mercado padrão para APIs corporativas em Java. Fornece auto-configuração, injeção de dependência avançada, gerenciamento transacional simplificado, suporte nativo ao Java 21 e ecossistema maduro.

### 2.3. H2 Database (In-Memory)
- **Motivação:** Elimina o atrito de configuração de containers ou servidores de banco de dados locais para o MVP.
- **Acesso ao Console:**
  - Habilitado no `application.yml`:
    ```yaml
    spring:
      h2:
        console:
          enabled: true
          path: /h2-console
      datasource:
        url: jdbc:h2:mem:schedulingdb
        driverClassName: org.h2.Driver
        username: admin
        password: admin
    ```

### 2.4. Springdoc OpenAPI / Swagger UI
- **Motivação:** Permite a inspeção e os testes dos endpoints REST diretamente pelo navegador web sem necessidade inicial de clientes externos como Postman ou Insomnia.
- **Acesso Web:**
  - Swagger UI: `http://localhost:8080/swagger-ui.html`
  - OpenAPI Spec JSON: `http://localhost:8080/v3/api-docs`

### 2.5. Project Lombok
- **Motivação:** Redução de código boilerplate repetitivo em classes de entidade e configuração.
- **Anotaçoes Chave:**
  - `@Getter` / `@Setter`: Acesso encapsulado.
  - `@RequiredArgsConstructor`: Construtor com campos `final` para injeção de dependência.
  - `@Builder`: Construção expressiva de objetos de teste e domínio.
  - `@NoArgsConstructor` / `@AllArgsConstructor`: Requisitos do Hibernate para entidades JPA.

---

## 3. Estrutura do `pom.xml` (Maven)

```xml
<properties>
    <java.version>21</java.version>
</properties>

<dependencies>
    <!-- Web & REST -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- JPA & Persistência -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <!-- Bean Validation -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- H2 Database -->
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Swagger / OpenAPI -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>2.5.0</version>
    </dependency>

    <!-- Testes -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```
