# LocalFlow Player (Beta)

Player Android local, offline e leve para áudio e vídeo armazenados no aparelho. Esta é uma versão beta inicial: não faz downloads, não usa backend e não pede acesso amplo ao armazenamento.

## Download beta

Baixe sempre a versão mais recente na página de [Releases do LocalFlow Player](https://github.com/RodrigoRuan2/LocalFlow-Player/releases).

Versão atual: **v0.1.1-beta** — [baixar APK](https://github.com/RodrigoRuan2/LocalFlow-Player/releases/download/v0.1.1-beta/app-debug.apk).

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
2. Abra a pasta no Android Studio ou execute `gradle assembleDebug` após instalar o Gradle 8.9+.
3. Instale `app/build/outputs/apk/debug/app-debug.apk` no aparelho.

## Permissões e background

Em Android 13+, o app solicita `READ_MEDIA_AUDIO`, `READ_MEDIA_VIDEO` e permissão de notificação. Em versões anteriores usa `READ_EXTERNAL_STORAGE`. Não usa `MANAGE_EXTERNAL_STORAGE`.

`LocalFlowPlaybackService` é um foreground service do tipo `mediaPlayback` e hospeda a `MediaSession`; Media3 disponibiliza notificação, lock screen, headset e Bluetooth automaticamente. Quando o `PlayerView` deixa a tela de vídeo, o Surface é liberado e o ExoPlayer mantém a faixa de áudio do arquivo, sem conversão para MP3.

## Estrutura

- `model`: modelos de mídia
- `repository`: leitura do MediaStore
- `data`: Room e DataStore
- `playback`: MediaSessionService e MediaController
- `ui`: ViewModel, telas Compose e tema

## Limitações conhecidas

Esta é uma beta: recomenda-se validar a reprodução de formatos variados, Bluetooth e lock screen no aparelho real. O projeto usa dependências estáveis declaradas no catálogo de versões.
