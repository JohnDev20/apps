# DevAI Chat (sem login) — pronto para Codemagic

Objetivo:
- App Android nativo (Kotlin) sem login
- Chat com seleção de IA: OpenAI / Claude (Anthropic) / Gemini
- API keys configuráveis no app
- Histórico local (Room)
- Instrução fixa (prompt) no topo do chat
- Anexo de arquivos via seletor do Android:
  - Imagens: enviadas como base64 (OpenAI/Claude/Gemini)
  - Texto pequeno: inclui conteúdo no prompt
  - Outros: envia só metadados (nome/tipo/tamanho)

## Como usar via Codemagic (sem Android Studio)
1) Faça upload desta pasta para um repositório no GitHub
2) No Codemagic, conecte o repo e selecione o workflow `android-debug`
3) Troque `EMAIL_NOTIFY` no `codemagic.yaml`
4) Rode o build e baixe o APK gerado em `artifacts`

Obs:
- Este repo NÃO inclui Gradle Wrapper (gradlew). O `codemagic.yaml` usa `gradle` diretamente.
  Se o seu ambiente exigir wrapper, siga a recomendação do Codemagic para gerar e commitar o wrapper. citeturn0search3turn0search11
