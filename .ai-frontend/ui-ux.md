# Diretrizes de UI/UX e Design System (Frontend UI/UX)

Este documento estabelece a identidade visual, padrões de experiência do usuário (UX), especificações de componentes e boas práticas de acessibilidade para o **Aplicativo de Agendamentos de Serviços**, incluindo os fluxos de **Login de Cliente** e **Painel "Meus Agendamentos" (Minha Agenda)**.

---

## 1. Filosofia de Design e Abordagem Mobile-First

- **Mobile-First por Padrão:** Toda a interface é desenhada primariamente para telas compactas de smartphones (360px - 480px), priorizando toque com um polegar (*thumb-friendly*), botões com área de toque mínima de **44x44px** e tipografia legível sem necessidade de zoom.
- **Escalabilidade com Bootstrap Grid:** 
  - Celulares (< 768px): Coluna única (`col-12`).
  - Tablets (≥ 768px): Grid de 2 colunas (`col-md-6`).
  - Desktops (≥ 992px): Grid de 3 ou 4 colunas (`col-lg-4` ou `col-xl-3`).
- **Minimalismo e Elegância:** Interface limpa, com respiro visual abundante (padding/margin consistentes) e ausência de ruídos visuais.

---

## 2. Paleta de Cores e Tokens Visuais

O design adota uma paleta sofisticada com forte contraste e tons corporativos/premium:

| Token / Papel | Código Hex | Descrição e Aplicação |
| :--- | :--- | :--- |
| **Primary / Deep Navy** | `#00011d` | **Azul marinho profundo.** Utilizado na Navbar superior, títulos de destaque (H1, H2), botões principais e estados ativos. |
| **Secondary Background** | `#f4f6fa` | **Azul acinzentado claro.** Fundo geral da página (`body`), áreas de respiro e cabeçalhos de tabelas/cards secundários. |
| **Surface / Card White** | `#ffffff` | **Branco puro.** Superfície dos cards de serviços, modais e caixas de diálogo. |
| **Text Primary** | `#0f172a` | Cinza ardósia escuro para texto principal e títulos internos, garantindo legibilidade perfeita (contraste WCAG AAA). |
| **Text Muted** | `#64748b` | Cinza intermediário para descrições, rótulos secundários e metadados (duração, profissional). |
| **Accent / CTA** | `#1e40af` | Azul vibrante para foco, hover de botões e links de navegação. |
| **Success Feedback** | `#059669` | Verde esmeralda para confirmações de agendamento e status `CONFIRMED`. |
| **Danger / Conflict** | `#dc2626` | Vermelho carmesim para toasts de conflito de horário (HTTP 409), botão "Cancelar" e validações com falha. |

### Variáveis CSS Sugeridas (`css/styles.css`):
```css
:root {
  --color-primary: #00011d;
  --color-bg-secondary: #f4f6fa;
  --color-surface: #ffffff;
  --color-text-main: #0f172a;
  --color-text-muted: #64748b;
  --color-accent: #1e40af;
  --color-success: #059669;
  --color-danger: #dc2626;
  --border-radius-card: 16px;
  --box-shadow-card: 0 4px 20px rgba(0, 1, 29, 0.06);
}

body {
  background-color: var(--color-bg-secondary);
  color: var(--color-text-main);
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
}
```

---

## 3. Novos Componentes e Seções do MVP Evoluído

### 3.1. Header Dinâmico (Controle de Sessão)
- **Estado Deslogado:** Exibe o botão "Entrar" com ícone de login (`btn-outline-light rounded-pill`), abrindo o modal de login.
- **Estado Autenticado:** Exibe o nome do cliente logado ("Olá, Mariana") com avatar/ícone circular e botão "Sair" (`btn-sm btn-outline-light rounded-pill`), limpando o `localStorage` e recarregando o estado da interface.

### 3.2. Modal de Login (`#loginModal`)
- **Objetivo:** Autenticação rápida de clientes no MVP apenas informando o e-mail cadastrado.
- **Estrutura:**
  - Input para e-mail com autofocus e validação HTML5.
  - Atalhos visuais para login rápido com contas de teste mockadas (`Mariana Souza` e `Lucas Pereira`).
  - Botão de submissão com estado de carregamento (*spinner*).

### 3.3. Painel "Meus Agendamentos" / "Minha Agenda"
- **Comportamento:** Seção destacada exibida acima ou abaixo do catálogo de serviços **apenas quando o usuário estiver autenticado**.
- **Cards de Agendamento do Usuário:**
  - Identificação clara do serviço, profissional responsável, data e horário de início/término.
  - Badge visual de status (`Agendado`, `Confirmado`).
  - **Botões de Ação Direta:**
    - **Cancelar (Vermelho - `btn-outline-danger`):** Dispara `confirm()` no navegador e invoca `DELETE /api/appointments/{id}`.
    - **Reagendar (Azul - `btn-outline-primary`):** Abre o modal de reagendamento para escolher nova data/hora.

### 3.4. Modal de Reagendamento (`#rescheduleModal`)
- **Objetivo:** Ajustar data e horário de um agendamento existente sem necessidade de recadastrá-lo.
- **Campos:**
  - Exibição de resumo do agendamento atual (serviço e profissional).
  - Nova data (`type="date"`) com validação de data futura.
  - Novo horário de início (`type="time"`) com recálculo automático do novo término.
- **Feedback:**
  - Conflito (409): Exibe **Toast Vermelho** com o aviso de indisponibilidade e mantém o modal aberto.
  - Sucesso (200): Exibe **Toast Verde** e atualiza imediatamente a lista de agendamentos.

---

## 4. Sistema de Notificações Toast (`.toast`)
- **Posicionamento:** Canto superior direito (`position-fixed top-0 end-0 p-3`) com `z-index: 1090`.
- **Casos de Uso:**
  - **Login com Sucesso:** "Bem-vindo(a) de volta, Mariana!"
  - **Cancelamento Efetuado:** "Agendamento cancelado com sucesso."
  - **Reagendamento Confirmado:** "Horário atualizado com sucesso!"
  - **Conflito no Reagendamento (HTTP 409):** "O profissional já possui compromisso no novo horário selecionado."
