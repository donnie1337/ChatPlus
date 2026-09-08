# ChatPlus

Sistema de chat do servidor, com canais **local, global e staff** e integração com o CargoPlus.

## ✨ Funcionalidades

### 💬 Canais de chat
- **Chat local** para comunicação próxima entre jogadores.
- **Chat global** para comunicação com o servidor inteiro.
- **Chat staff** para comunicação exclusiva da equipe.
- Formatação e mensagens configuráveis.

### 🎨 Cores das mensagens
- `/cor` abre uma interface gráfica para escolher a cor das mensagens.
- Paleta de cores configurável.
- A cor escolhida fica associada ao jogador.
- A cor preta não faz parte da paleta disponível.
- O sistema utiliza os dados de cores fornecidos pelo CargoPlus.

### 🔗 Integração com CargoPlus
O ChatPlus utiliza o CargoPlus para obter informações de cargo, permissões e cores, permitindo que a aparência das mensagens seja integrada ao sistema de hierarquia do servidor.

### 🔐 Permissões
Permissões principais:
- `chatplus.local`
- `chatplus.global`
- `chatplus.staff`
- `chatplus.cor`
- `chatplus.admin`

## 🎮 Comandos

| Comando | Função |
|---|---|
| `/l <mensagem>` | Envia mensagem pelo chat local. |
| `/g <mensagem>` | Envia mensagem pelo chat global. |
| `/s <mensagem>` | Envia mensagem pelo chat da staff. |
| `/cor` | Abre o menu para escolher a cor da mensagem. |
| `/chat reload` | Recarrega o ChatPlus. |

## 🔑 Permissões

| Permissão | Função | Padrão |
|---|---|---|
| `chatplus.local` | Usar chat local | `true` |
| `chatplus.global` | Usar chat global | `true` |
| `chatplus.staff` | Usar chat da staff | `false` |
| `chatplus.cor` | Escolher cor das mensagens | `false` |
| `chatplus.admin` | Administrar o ChatPlus | `false` |

## 🔗 Dependências

- LoginPlus
- CargoPlus
- UtilidadesPlus

## 🏗️ Plataforma

- Java 26
- Spigot API 26.2
- Maven

## 🧪 Build

```bash
mvn -B clean package
```

O projeto possui workflow de build no GitHub Actions.
