# Desenvolvimento — LocalFlow Player

## Fase atual
Beta 0.4.2: seleção por gesto de galeria com auto-rolagem e preferências de biblioteca persistentes implementadas; validação final em andamento.

## Concluído
- Midnight aplicado em biblioteca, álbuns, artistas, pastas, vídeos, favoritos, playlists, busca, players, fila e configurações.
- Capas reais, thumbnails, mosaicos e placeholders; cache 12 MB, 128/512 px, até dois decodificadores.
- Playlists com seleção múltipla, edição e remoção; transações Room preservam ordem e evitam duplicados.
- Uma instância de ExoPlayer no serviço; controles de vídeo/segundo plano, equalizador opcional, timer e restauração pausada.
- Aba WhatsApp separada de Faixas; filtros de música, WhatsApp e vídeo não compartilham mais estado.
- Modo de seleção múltipla e operações físicas via confirmação MediaStore; playlists continuam seguras.
- Pastas informam número de músicas, WhatsApp e vídeos, com filtro interno ao abrir.
- Abas compactas de Músicas, shuffle direto na biblioteca, título/fallback de metadados danificados e player de áudio adaptável à altura.
- Vídeo em tela cheia imersiva real, prévia de anterior/próximo e atalho à fila completa.
- Pastas filtráveis por tudo, áudios ou vídeos; o filtro é mantido ao abrir a pasta.
- Ação nativa de favorito adicionada à MediaSession/notificação, conectada diretamente ao banco Room de favoritos.
- Seleção por toque longo e arrasto para as faixas visíveis; ações em lote incluem playlist, mover e apagar.
- Filtro e ordenação de Pastas persistidos com DataStore.
- Seleção em lote sem botão: toque longo e arrasto em músicas, vídeos, favoritos e coleções; a rolagem normal continua usando LazyColumn nativo.
- Aba de Músicas e ordenações de músicas, WhatsApp e vídeos persistidas em DataStore entre execuções.
- Auto-rolagem durante a seleção: ao chegar às bordas da lista com o dedo pressionado, a biblioteca segue a direção e seleciona os itens atravessados.

## Decisões
- Room para coleções, DataStore para preferências; banco existente preservado.
- Consultas e imagens fora da main thread; busca com debounce; sem scanner permanente.
- Restauração por checkpoints, limitada a 500 itens; sem reprodução automática após reinício.

## Validação
- Build, cinco testes unitários e lint passaram (zero erros, 48 avisos).
- Compilação final: BUILD SUCCESSFUL em 55 s; assinatura APK validada com apksigner.
- `assembleDebug` e seis testes unitários passaram após a separação de biblioteca (4 min 28 s).
- `assembleDebug` e seis testes unitários passaram após as ações de biblioteca (5 min 30 s).
- `assembleDebug` e sete testes unitários passaram após os ajustes de player e biblioteca (validação visual em emulador Android 15).
- Serviço de mídia Android 15 validado: ação customizada **Adicionar aos favoritos** aparece nos controles da MediaSession.
- Build final `assembleDebug`, seis testes unitários e `lintDebug`: **BUILD SUCCESSFUL** (7 min 33 s; 0 erros no lint).
- Build final da beta 0.4.1: `assembleDebug`, sete testes unitários e `lintDebug` concluídos; APK assinado verificado (v2) e lint com 0 erros/6 avisos existentes.
- Build da beta 0.4.2: `assembleDebug` e sete testes unitários concluídos; APK v2 assinado, versão 9 confirmada.
- Android 15/API 35: instalação e reinstalação sem apagar dados, capas/thumbnail, áudio com tela bloqueada, notificação, vídeo normal/somente áudio e pausa quando background desligado.
- Interface: playlist criada, adição de áudio e vídeo, abertura; favorito, busca, pastas, álbuns, fila reordenada e timer ativado/cancelado.
- Runner Room em memória: criação, adição, duplicados, remoção, ordem, renomeação, favoritos e exclusão passaram.
- Sem registros de crash do app na sessão testada. Medição pontual no emulador: aproximadamente 140 MB PSS; não representa teste de bateria ou de aparelho de entrada.

## Problemas e limites
- Daemon Kotlin instável: compilação in-process com dois workers.
- Gerador de mídia de teste corrigido para atoms MP4 de 64 bits e pastas permitidas pelo MediaStore.
- Equalizador indisponível na saída do emulador: estado informativo conferido; efeito audível ainda requer celular.
- Biblioteca ainda mantém metadados completos em RAM; falta paginação para acervos muito grandes.

## Próximos passos
- Testar esta beta no celular: rolagem normal, toque longo + arrasto de seleção, filtros/ordenações persistentes e consumo prolongado.
- Testar em aparelho com áudios WhatsApp: confirmar caminhos antigos/novos e a independência das três ordenações.
- Testar em Android 11+: confirmação para apagar/mover, cancelamento, pastas de destino e atualização imediata da biblioteca.
- Perfil de memória/bateria em aparelho de entrada e testes adicionais de permissões revogadas/arquivos corrompidos.
