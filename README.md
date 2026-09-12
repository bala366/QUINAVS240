# Quina Campeã - Grupo Fixo de 40 - V2

Aplicativo Android com dashboard roxo e trevo branco.

## Grupo campeão fixo

04 05 06 09 10 11 13 14 15 21 23 24 26 29 30 31 33 36 38 39 43 44 48 49 51 52 54 56 57 63 64 65 66 67 68 69 71 72 73 77

Esse grupo foi congelado na V2 a partir do grupo campeão encontrado na V1 e conferido no histórico pelo usuário. Na base até o concurso 7114 ele aparecia com 340 ocorrências de 5/5. O app recalcula as estatísticas desse mesmo grupo quando uma base nova é carregada.

## Dois jogos

1. **Jogo 1 - Grupo Campeão Fixo:** gera C(40,5)=658.008 combinações dentro das 40 dezenas fixas e escolhe o melhor jogo após filtro e perímetro de duque.
2. **Jogo 2 - Grupo 40 Mais Atrasado:** procura, por busca determinística profunda, um grupo historicamente forte que esteja há mais concursos sem conter 5/5; depois gera as 658.008 combinações dentro dele e escolhe o jogo.

## Filtro padrão fixo

- repetidas: 0-1
- pares: 2-4
- primos: 0-2
- Fibonacci: 0-1
- dezenas 01-40: 2-4
- soma: 155-249

O filtro alternativo continua disponível para o usuário preencher.

## PDF

O PDF pode ter uma ou duas páginas, conforme os jogos já gerados:
- verde: jogo final de 5 dezenas;
- vermelho: 40 dezenas fora do grupo (falha);
- branco: demais 35 dezenas que pertencem ao grupo de 40.

## Compilação

O projeto usa Java 17, Android SDK 35 e Gradle 8.9 via GitHub Actions. O workflow está em `.github/workflows/build-apk.yml` e também em `FLUXO_DE_TRABALHO.txt`.
