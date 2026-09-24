# LocalFlow Player (Beta)

Player Android local, offline e leve para áudio e vídeo armazenados no aparelho. Esta é uma versão beta inicial: não faz downloads, não usa backend e não pede acesso amplo ao armazenamento.

## Download beta

Baixe sempre a versão mais recente na página de [Releases do LocalFlow Player](https://github.com/RodrigoRuan2/LocalFlow-Player/releases).

Versão atual: **v0.3.0-beta — Organização de arquivos** — o APK estará disponível na página de [Releases](https://github.com/RodrigoRuan2/LocalFlow-Player/releases).

## Midnight

Interface escura em grafite e violeta, com alternativas clara e sistema. Início, músicas, álbuns, artistas, vídeos, pastas, favoritos, busca, playlists, player completo, fila editável e configurações usam o mesmo tema.

Capas vêm do MediaStore ou dos metadados do arquivo; vídeos usam frames reais. Arquivos sem imagem recebem uma arte vetorial leve. O cache é limitado a 12 MB, as imagens a 128/512 px e a decodificação a duas operações simultâneas. Não buscamos imagens na internet.

Playlists permitem criar, renomear, selecionar várias mídias, abrir, reproduzir e remover itens. O painel de áudio oferece equalizador do Android quando disponível e timer por minutos ou fim da faixa.

Áudios cujo caminho no MediaStore pertence ao WhatsApp são mostrados na aba **WhatsApp** dentro de Músicas, sem aparecer em Faixas. Músicas, WhatsApp e Vídeos guardam ordenações independentes.

O modo **Selecionar arquivos** permite mover ou apagar vários arquivos locais. Apagar remove o arquivo físico somente após a confirmação nativa do Android. Mover cria uma pasta de destino compatível em `Music` para áudios e `Movies` para vídeos. Em playlists, a ação Remover nunca apaga o arquivo. Estas operações requerem Android 11 ou superior, pois versões anteriores não disponibilizam a autorização segura em lote.

### Organização de versões

- `v0.1.0-beta`, `v0.1.1-beta`, `v0.2.0-beta`: versões de teste, podem ter falhas.
- `v1.0.0`: primeira versão estável.
- `v1.0.1`, `v1.1.0` e seguintes: correções e novos recursos estáveis.

Cada release terá o APK, data, lista de mudanças e uma tag Git correspondente. Para instalar, baixe o APK no celular, abra-o no gerenciador de arquivos e permita a instalação para a origem usada.

## Tecnologias

Kotlin, Jetpack Compose/Material 3, Navigation Compose, Media3 ExoPlayer, `MediaSessionService`, MediaStore, Room, DataStore, Coroutines e StateFlow. O `minSdk` é 26 (Android 8) e o `targetSdk` é 35.

## Arquitetura

`UI → ViewModel → MediaStore/Room` carrega a biblioteca fora da main thread. A reprodução é independente: `UI → MediaController → LocalFlowPlaybackService → ExoPlayer`. Há uma única instância de ExoPlayer no serviço; Activity e telas não a possuem.

`MediaStoreRepository` consulta `MediaStore.Audio` e `MediaStore.Video`, então mídias criadas por navegador, mensageiros ou download managers aparecem sem acoplamento a pastas específicas. Room persiste favoritos e playlists; DataStore persiste preferências.

## Como compilar

1. Instale JDK 17 e Android SDK Platform 35 (Android Studio é a maneira mais simples).
2. Abra a pasta no Android Studio ou execute `gradle :app:assembleDebug` com Gradle 8.11.1.
3. Instale `app/build/outputs/apk/debug/app-debug.apk` no aparelho.

## Permissões e background

Em Android 13+, o app solicita `READ_MEDIA_AUDIO`, `READ_MEDIA_VIDEO` e permissão de notificação. Android 14+ também permite seleção parcial de vídeos. Em versões anteriores usa `READ_EXTERNAL_STORAGE`. Não usa `MANAGE_EXTERNAL_STORAGE` nem permissão de internet. `MODIFY_AUDIO_SETTINGS` atende o equalizador; `WAKE_LOCK` é usado pelo gerenciamento de reprodução local do Media3.

`LocalFlowPlaybackService` é um foreground service do tipo `mediaPlayback` e hospeda a `MediaSession`; Media3 disponibiliza notificação, lock screen, headset e Bluetooth. Quando o `PlayerView` deixa a tela, sua conexão ao player é liberada e a faixa de vídeo é desabilitada. A faixa de áudio continua somente conforme a preferência de segundo plano, sem conversão para MP3. O modo somente áudio também pode ser alternado explicitamente no player.

## Estrutura

- `model`: modelos de mídia
- `repository`: leitura do MediaStore
- `data`: Room e DataStore
- `playback`: MediaSessionService e MediaController
- `ui`: ViewModel, telas Compose e tema

## Limitações conhecidas

Esta é uma beta com assinatura de desenvolvimento, não uma distribuição de produção pela Play Store. Formatos dependem dos decodificadores disponíveis no aparelho. Equalizador depende da saída de áudio e pode ficar indisponível. Layout e visibilidade dos controles de tela bloqueada dependem do sistema.

A restauração é pausada e usa checkpoints em transições, pausa e saída do app; uma morte abrupta do processo pode perder segundos recentes. Para limitar o armazenamento, restaura até 500 itens em torno da mídia atual. Bibliotecas muito grandes ainda carregam seus metadados em memória; paginação e medição de bateria/RAM em aparelhos de entrada continuam como trabalho futuro.

Testes: `gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest`. O runner de dispositivo usa Room em memória, sem alterar playlists do usuário: `adb shell am instrument -w com.localflow.player.test/com.localflow.player.LocalFlowTestRunner`. A opção de teste `-e fixtures true` gera arquivos locais de QA nas pastas Music/Movies; não faz parte do APK principal.
