# Stack Tecnológica do Front-end MVP (Frontend Tech Stack)

Este documento especifica a stack tecnológica adotada para o desenvolvimento da interface web responsiva do **Aplicativo de Agendamentos de Serviços** (MVP). A arquitetura prioriza simplicidade, ausência de ferramentas de build pesadas e máxima portabilidade.

---

## 1. Tecnologias Principais

| Componente | Tecnologia | Finalidade / Justificativa |
| :--- | :--- | :--- |
| **Marcação** | **HTML5 Semântico** | Estruturação acessível e clara dos elementos da interface (`<header>`, `<main>`, `<section>`, `<article>`). |
| **Estilização** | **CSS3 Moderno** | Customizações refinadas de tema, CSS Custom Properties (variáveis) e transições suaves. |
| **Linguagem** | **JavaScript Moderno (ES6+)** | Lógica de interface, manipulação do DOM, controle de estado local e sintaxe moderna (`async/await`, desestruturação, arrow functions, modules). |
| **Framework CSS** | **Bootstrap 5.3 (via CDN)** | Design system completo, sistema de grid responsivo mobile-first, utilitários de espaçamento e componentes prontos sem dependência de jQuery. |
| **Ícones** | **Bootstrap Icons (via CDN)** | Biblioteca de ícones vetoriais leves para enriquecer ações, botões e badges informativos. |
| **Comunicação HTTP** | **Fetch API (Nativa)** | Execução de chamadas assíncronas REST aos endpoints do backend sem necessidade de bibliotecas externas como Axios. |

---

## 2. Arquitetura de Execução: Zero-Build / Vanilla JS

- **Sem Node.js, Webpack, Vite ou Babel:** O frontend roda nativamente no navegador web. Não há etapa de compilação ou transpilação.
- **Execução Imediata:** Para executar o projeto, basta abrir o arquivo `index.html` diretamente no navegador ou servi-lo através de qualquer servidor estático simples (como a extensão *Live Server* do VS Code, IntelliJ IDEA built-in server ou `python -m http.server`).
- **Baixo Acoplamento:** O frontend é uma SPA (Single Page Application) leve que se comunica com o backend exclusivamente via JSON sobre HTTP.

---

## 3. Inclusão dos Recursos via CDN (Template Base)

Para garantir facilidade de manutenção e carregamento rápido, os recursos do Bootstrap 5.3 e ícones são importados diretamente via CDN oficial:

```html
<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Agendamento de Serviços</title>

  <!-- Bootstrap 5.3 CSS -->
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
  
  <!-- Bootstrap Icons -->
  <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css" rel="stylesheet">

  <!-- Estilos Customizados -->
  <link rel="stylesheet" href="css/styles.css">
</head>
<body>

  <!-- Conteúdo da Aplicação -->

  <!-- Bootstrap 5.3 JS Bundle (inclui Popper) -->
  <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>

  <!-- Scripts da Aplicação -->
  <script type="module" src="js/app.js"></script>
</body>
</html>
```

---

## 4. Comunicação HTTP com Fetch API

- **Padronização:** Todas as requisições utilizam `async/await` com tratamento de respostas via `response.ok` e captura de exceções em blocos `try/catch`.
- **Headers:** Requisições `POST` e `PUT` enviam obrigatoriamente `Content-Type: application/json` e `Accept: application/json`.
- **Payloads de Erro:** Trata respostas de erro (400, 404, 409, 500) convertendo o body JSON estruturado pelo `GlobalExceptionHandler` do backend para exibir feedback imediato ao usuário.
