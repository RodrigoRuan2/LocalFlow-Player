# Desenvolvimento — LocalFlow Player

## Fase atual
Beta 0.2.0 Midnight: implementação e testes no emulador concluídos; compilação final passou e APK pronto para distribuição.

## Concluído
- Midnight aplicado em biblioteca, álbuns, artistas, pastas, vídeos, favoritos, playlists, busca, players, fila e configurações.
- Capas reais, thumbnails, mosaicos e placeholders; cache 12 MB, 128/512 px, até dois decodificadores.
- Playlists com seleção múltipla, edição e remoção; transações Room preservam ordem e evitam duplicados.
- Uma instância de ExoPlayer no serviço; controles de vídeo/segundo plano, equalizador opcional, timer e restauração pausada.

## Decisões
- Room para coleções, DataStore para preferências; banco existente preservado.
- Consultas e imagens fora da main thread; busca com debounce; sem scanner permanente.
- Restauração por checkpoints, limitada a 500 itens; sem reprodução automática após reinício.

## Validação
- Build, cinco testes unitários e lint passaram (zero erros, 48 avisos).
- Compilação final: BUILD SUCCESSFUL em 55 s; assinatura APK validada com apksigner.
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
- Testar esta beta no celular: Bluetooth, chamadas, formatos variados, orientação e consumo prolongado.
- Perfil de memória/bateria em aparelho de entrada e testes adicionais de permissões revogadas/arquivos corrompidos.
