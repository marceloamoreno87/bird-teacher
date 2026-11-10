---
name: project-analizer
description: Analisar a arquitetura, componentes e dependências do projeto, produzindo relatório fundamentado com procedimentos locais do scaffold.
---

# Análise do projeto

Skill autocontida; `project-analizer` preserva o identificador existente. Entrada: `$project-analizer architecture|component|dependencies [escopo]`.

- Arquitetura: [generate-architectural-report](references/upstream/commands/generate-architectural-report.md) e [architectural-analyzer](references/upstream/agents/architectural-analyzer.md).
- Componente: [component-deep-analyzer](references/upstream/agents/component-deep-analyzer.md).
- Dependências: [run-dependency-audit](references/upstream/commands/run-dependency-audit.md) e [dependency-auditor](references/upstream/agents/dependency-auditor.md).

Leia as referências selecionadas e execute com ferramentas do Codex na sessão atual. Execute as fases sequencialmente por padrão; agentes paralelos exigem autorização no pedido atual. Consultas atuais a documentação, versões e vulnerabilidades usam ferramentas disponíveis; registre informações não verificadas quando não houver acesso.

Analise o código do projeto, excluindo dependências geradas e templates do próprio harness das conclusões sobre o produto. Antes do scaffold, informe que somente o harness existe. Cite arquivos e relações observadas; diferencie problema confirmado de hipótese. Auditoria não instala ou atualiza dependências automaticamente. Entregue o relatório no destino solicitado e os comandos/evidências usados.
