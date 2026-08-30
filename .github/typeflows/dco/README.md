# DCO (dco.yml)

```mermaid
%%{init: {"flowchart": {"curve": "basis"}}}%%
flowchart TD
    pullrequest(["🔀 pull_request<br/>(opened, synchronize, reopened)"])
    subgraph dcoyml["DCO"]
        dcoyml_metadata[["🔧 Workflow Config<br/>🔐 custom permissions"]]
        dcoyml_dco["DCO<br/>🐧 ubuntu-latest<br/>⏱️ 5m timeout"]
    end
    pullrequest --> dcoyml_dco
```

## Job: DCO

| Job | OS | Dependencies | Config |
|-----|----|--------------|---------| 
| `dco` | 🐧 ubuntu-latest | - | ⏱️ 5m |

### Steps

```mermaid
%%{init: {"flowchart": {"curve": "basis"}}}%%
flowchart TD
    step1["Step 1: Checkout"]
    style step1 fill:#f8f9fa,stroke:#495057
    action1["🎬 actions<br/>checkout<br/><br/>📝 Inputs:<br/>• persist-credentials: false<br/>• fetch-depth: 0"]
    style action1 fill:#e1f5fe,stroke:#0277bd
    step1 -.-> action1
    step2["Step 2: Check sign-off<br/>💻 bash"]
    style step2 fill:#f3e5f5,stroke:#7b1fa2
    step1 --> step2
```

**Step Types Legend:**
- 🔘 **Step Nodes** (Gray): Workflow step execution
- 🔵 **Action Blocks** (Blue): External GitHub Actions
- 🔷 **Action Blocks** (Light Blue): Local repository actions
- 🟣 **Script Nodes** (Purple): Run commands/scripts
- **Solid arrows** (→): Step execution flow
- **Dotted arrows** (-.->): Action usage with inputs