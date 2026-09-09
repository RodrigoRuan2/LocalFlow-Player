# Desenvolvimento — LocalFlow Player

## Fase atual

Correções de uso em validação para a beta `0.1.1`.

## Funcionalidades concluídas

- Projeto Kotlin/Compose com catálogo Gradle, minSdk 26 e permissões modernas.
- Consulta real de áudio, vídeo e pastas pelo MediaStore em `Dispatchers.IO`.
- Biblioteca com músicas, vídeos, pastas, favoritos, busca local e playlists persistidas.
- MediaSessionService com uma única instância de ExoPlayer, MediaController, áudio focus e tratamento de headset becoming noisy.
- Mini player, telas de áudio/vídeo, queue, shuffle/repeat, liberação de Surface no vídeo e configurações persistidas.
- Playlists agora podem ser abertas, reproduzidas e ter itens removidos; o botão “Lista” em cada mídia adiciona itens à playlist escolhida.
- Modo somente áudio do vídeo é uma escolha explícita no player; ao sair, a preferência de reprodução em segundo plano define se o áudio continua ou pausa.
- Teste unitário inicial de transformação de biblioteca.

## Decisões importantes

- Room apenas para favoritos/playlists; DataStore para preferências.
- Não há scanner ou polling de MediaStore: a biblioteca só é consultada ao abrir/atualizar.
- Thumbnails usam `ContentResolver.loadThumbnail` em 96x96, fora da main thread, com LRU limitado a 8 MB e placeholder para arquivos sem imagem/API antiga.

## Problemas encontrados

- A primeira compilação revelou incompatibilidade de alvo JVM (Java 8 vs. Kotlin/KSP 17) e imports Compose ausentes; foram corrigidos configurando Java/Kotlin 17 e os imports adequados.

## Próximos passos

1. Compilar e instalar a beta `0.1.1` para validar os ajustes de playlist e vídeo em aparelho real.
2. Validar a persistência de queue em interrupção real do processo.
3. Fazer perfil de memória/bateria em aparelho de entrada.

## Validação mais recente

- JDK 17, Gradle 8.11.1 e Android SDK Platform 35 foram provisionados após autorização do usuário.
- `test assembleDebug`: **BUILD SUCCESSFUL** (53 s; 72 tarefas, 12 executadas).
- APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Atualização visual

- Interface atualizada com navegação inferior, cards de mídia, mini player e player completo refinados.
- Ícone moderno do LocalFlow aplicado ao manifesto em `res/drawable/localflow_app_icon.png`.
- `:app:assembleDebug`: **BUILD SUCCESSFUL** (57 s; APK atualizado instalado e iniciado no emulador Android 15).
