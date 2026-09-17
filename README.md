# ChatPlus

O ChatPlus é o plugin que cuida do chat do meu servidor.

Ele organiza os canais de chat, as cores das mensagens, a integração com cargos e algumas interações do jogador com o chat.

## Canais

### Chat Local

- `/l <mensagem>` envia mensagem para jogadores próximos.
- Mostra a identificação do chat local.
- A identificação possui descrição ao passar o mouse.

### Chat Global

- `/g <mensagem>` envia mensagem para o servidor inteiro.
- Mostra a identificação do chat global.
- A identificação possui descrição ao passar o mouse.

### Chat Staff

- `/s <mensagem>` envia mensagem para a equipe.
- Mostra a identificação do chat staff.
- A identificação possui descrição ao passar o mouse.

## Cores

- `/cor` abre o menu para escolher a cor das mensagens.
- A paleta de cores é configurável.
- A cor escolhida fica salva para o jogador.
- A cor preta não faz parte da paleta disponível.
- As informações de cargo e cor são integradas ao CargoPlus.

## Informações do jogador

Ao passar o mouse no nome de um jogador no chat, aparece uma ficha com informações do jogador, incluindo:

- Cargo.
- Clan.
- Dinheiro.
- KDR.
- Tempo online.

O nome do jogador também pode ser clicado para preencher o nome dele no campo de mensagem.

## Integração com CargoPlus

O ChatPlus usa o CargoPlus para pegar o cargo, prefixo, permissões e cores dos jogadores.

O prefixo animado do DEV não é usado no chat. A animação continua disponível onde ela deve aparecer, como TAB e acima da cabeça.

## Comandos

| Comando | O que faz |
|---|---|
| `/l <mensagem>` | Envia mensagem no chat local. |
| `/g <mensagem>` | Envia mensagem no chat global. |
| `/s <mensagem>` | Envia mensagem no chat da staff. |
| `/cor` | Abre o menu de cores. |
| `/chat reload` | Recarrega o ChatPlus. |

## Permissões

| Permissão | O que faz | Padrão |
|---|---|---|
| `chatplus.local` | Usar chat local | `true` |
| `chatplus.global` | Usar chat global | `true` |
| `chatplus.staff` | Usar chat da staff | `false` |
| `chatplus.cor` | Escolher a cor da mensagem | `false` |
| `chatplus.admin` | Administrar o ChatPlus | `false` |

## Comando não encontrado

O ChatPlus é responsável pela mensagem de comando não encontrado do servidor.

A mensagem usada é:

`ᴄʜᴀᴛ • Comando não encontrado.`

A ideia é evitar que vários plugins mostrem a mesma mensagem ao mesmo tempo.

## Integrações

- **LoginPlus:** usado para respeitar o estado de autenticação.
- **CargoPlus:** cargos, prefixos, permissões e cores.
- **UtilidadesPlus:** integração com recursos gerais do servidor.

## Plataforma

- Java 26
- Spigot API 26.2
- Maven

## Build

```bash
mvn -B clean package
```

O projeto possui build automático pelo GitHub Actions.

## Status

O ChatPlus está em desenvolvimento e é o responsável pelo sistema de chat do meu servidor. A ideia é deixar o chat com um padrão único e integrado com os outros plugins, sem cada plugin ficar mandando mensagem de um jeito diferente.
