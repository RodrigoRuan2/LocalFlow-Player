# Histórico de versões

## v0.4.0-beta — Favoritos e organização fluida

- A notificação e os controles de mídia do sistema passam a oferecer **Adicionar aos favoritos**; ao tocar, a ação é persistida no mesmo banco do app e alterna para remover.
- Em Músicas, um toque longo seguido de arrasto seleciona diversas faixas visíveis; a seleção permite adicionar todas a uma playlist, mover para pasta ou apagar com confirmação do Android.
- Pastas persistem os filtros Tudo, Áudios e Vídeos e a ordenação por Nome ou Mais arquivos entre aberturas do app.

## v0.3.1-beta — Players e pastas adaptáveis

- Abas de Músicas adaptadas para telas estreitas: Faixas, WhatsApp, Álbuns e Artistas não sobrepõem letras; busca foi para o cabeçalho.
- Botão de aleatório na biblioteca de músicas cria e toca a fila em modo shuffle.
- Player de áudio adapta o tamanho da capa à altura disponível e mantém todos os controles principais visíveis em telas menores.
- Tela cheia de vídeo agora entra em modo imersivo real, em paisagem e sem cabeçalho/barras do app; o Android pode exibir seu aviso educativo uma única vez.
- A tela de vídeo mostra os itens anterior e próximo da fila e abre a fila completa.
- Pastas oferecem filtros **Tudo**, **Áudios** e **Vídeos**, incluindo o filtro já escolhido ao abrir uma pasta.
- Títulos e artistas compostos só por `?` ou caracteres de substituição recebem fallback seguro para nome do arquivo ou rótulo amigável.

## v0.3.0-beta — Organização de arquivos

- Listas retornam ao primeiro item ao mudar abas, filtros ou ordenação.
- Modo de seleção múltipla em músicas, WhatsApp, vídeos, favoritos e coleções.
- Arquivos selecionados podem ser movidos para uma pasta nova ou apagados do dispositivo após confirmação do Android.
- Pastas mostram totais separados de músicas, áudios WhatsApp e vídeos; ao abrir, permitem filtrar esses tipos.
- Remover em playlist continua removendo somente a referência, sem apagar o arquivo do celular.

## v0.2.1-beta — Biblioteca organizada

- Áudios localizados em pastas do WhatsApp agora ficam na aba própria **WhatsApp**, fora de **Faixas**.
- A detecção cobre os caminhos antigos e atuais do WhatsApp, incluindo `Android/media` e WhatsApp Business.
- Músicas, áudios do WhatsApp e vídeos ganharam ordenações independentes.
- A tela de vídeos agora tem botão de ordenar por nome, artista, data adicionada ou duração.

## v0.2.0-beta — Midnight

- Tema Midnight em todas as telas, com grafite, violeta, novos cards e navegação inferior.
- Capas reais, thumbnails de vídeo, mosaicos de playlists e placeholders leves.
- Álbuns, artistas e conteúdo de pastas navegáveis; pesquisa local com filtros.
- Editor de playlists e seleção múltipla de músicas/vídeos, sem duplicar itens existentes.
- Fila editável com remoção e mudança de ordem, player de áudio e vídeo renovados.
- Controle explícito de somente áudio e da continuação em segundo plano.
- Equalizador do Android quando suportado, timer e restauração pausada da última fila.
- Cache limitado de imagens, permissões parciais de vídeo e testes de regras e persistência.

## v0.1.1-beta — Playlists e modo de vídeo

- Playlists abrem seus itens, permitem reproduzir a fila e remover uma mídia individualmente.
- Cada música e vídeo tem um botão textual “Lista” para adicionar à playlist escolhida.
- O modo somente áudio do vídeo agora é um interruptor explícito: pode desligar a imagem e voltar ao vídeo quando quiser.
- Ao sair do player de vídeo, a configuração de áudio em segundo plano passa a decidir corretamente entre continuar ou pausar.

## v0.1.0-beta — Beta inicial

- Biblioteca local de músicas, vídeos e pastas via MediaStore.
- Reprodução com Media3, MediaSession e serviço em segundo plano.
- Mini player, player completo de áudio e vídeo.
- Favoritos, playlists, busca e configurações locais.
- Novo visual Material 3 e ícone do LocalFlow.

> Versão de teste: valide em aparelhos reais e relate falhas antes da V1.0.
