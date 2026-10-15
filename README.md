# FM Lab

Rádio FM e laboratório de recepção para **Galaxy S4 Mini GT-I9195 com LineageOS 14.1 / Android 7.1.2**, sem root.

O FM Lab usa o serviço do rádio nativo para controlar o sintonizador e consultar as métricas expostas pelo firmware. É um projeto experimental específico desse aparelho e dessa ROM; não é um receptor de rádio por internet.

## Recursos

- Sintonia de 87,5 a 108,0 MHz e favoritos com nomes.
- RSSI e SINR brutos, histórico temporal e indicador de qualidade ajustável.
- Leituras com alvo de 4, 10 ou 20 Hz; gráfico com alvo de 30 FPS.
- RDS: nome, texto, PI, PTY e ECC quando recebidos.
- Indicador de **recepção estéreo ou mono**, baseado em eventos reais do chip; estado desconhecido quando não há evento.
- Seleção de estéreo automático ou mono forçado para experimentar a recepção com ruído.
- **Varredura da faixa FM**: busca em passos de 0,2 ou 0,1 MHz, refinamento de até 12 candidatos e ranking por SINR médio, estabilidade e RSSI como desempate.
- Osciloscópio do áudio **direto da fonte FM**, sem usar o microfone.
- Espectro mono do áudio com FFT, RMS, pico e indicadores de áudio baixo/picos no limite digital.
- Medições nomeadas de 10 segundos para comparar cômodos e posições da antena.
- Registro contínuo de métricas e exportação de dados em CSV pelo seletor do Android.

<img src="docs/images/audio.png" width="320" alt="Osciloscópio e espectro funcionando com áudio real da fonte FM no S4 Mini">

*A captura ilustra a análise de áudio introduzida na versão 0.4, mantida na 0.5.*

## Instalação

Baixe o APK na seção **Releases** deste repositório e instale no aparelho. Builds distribuídas aqui são experimentais.

1. Mantenha o rádio nativo `com.caf.fmradio` instalado.
2. Conecte o fone com fio, usado como antena pelo telefone.
3. Ligue o FM pelo aplicativo nativo e abra o FM Lab. O nativo pode ficar em segundo plano.
4. Sintonize pelo FM Lab para identificar a frequência solicitada nos registros.
5. Para o osciloscópio e o espectro, ative a captura FM e permita o acesso a áudio solicitado pelo Android.

O app tem identificador próprio (`br.local.fmlab`) e não substitui o rádio nativo. Os dois compartilham o sintonizador: mudar a frequência ou desligar o FM afeta ambos.

## Encontrar os melhores sinais

Abra **Testes**, localize **Busca e comparação de emissoras** e toque em **Varrer FM**.

- **Rápida:** 87,5–107,9 MHz, passo de 0,2 MHz.
- **Completa:** 87,5–108,0 MHz, passo de 0,1 MHz.
- Mantenha o aparelho e o cabo do fone parados. A tela permanece ligada durante a busca.
- O áudio é temporariamente silenciado e o estéreo automático é habilitado.
- A busca inicial mede três amostras por frequência; os até 12 candidatos com melhor resultado são revisitados, com mais tempo de estabilização e oito amostras cada.
- O ranking usa SINR médio decrescente, menor variação do SINR em caso de empate e, depois, RSSI. Toque num resultado para sintonizar.
- Ao terminar, o app tenta restaurar a frequência solicitada antes da busca e desfaz o mudo que ele próprio ativou. O botão **Cancelar** encerra a busca; resultados incompletos são identificados como parciais.

São **candidatos de recepção**, não uma identificação garantida de todas as emissoras da cidade. RDS pode não ser recebido dentro do tempo de permanência em cada canal. A posição do cabo e interferências locais mudam o ranking. A seleção inicial considera SINR bruto médio a partir de 1; esse limiar não é uma calibração universal do chip.

## Estéreo e mono

O indicador usa `onAudioUpdate` do serviço nativo. Um comando aceito para permitir estéreo não prova que o receptor conseguiu decodificá-lo. **Mono recebido** também não prova que a emissora transmita somente em mono: sinal fraco, interferência ou configuração podem impedir o estéreo.

O protocolo original tem apenas um slot de callbacks. O FM Lab registra seu observador quando está em primeiro plano; o aplicativo nativo volta a registrar o dele ao ser aberto. O FM Lab não tenta apagar o observador do outro aplicativo ao sair. O observador usa referência fraca à Activity para não mantê-la viva desnecessariamente. Esta integração continua específica da ROM e pode interagir com as atualizações da interface nativa.

O estado estéreo da **recepção RF** é separado da captura PCM mono usada pelos gráficos. O analisador não usa diferença entre canais de áudio para adivinhar se o rádio abriu estéreo.

## Medir cobertura

Em **Testes → Medir local · 10 s**, nomeie o ponto, aguarde e repita em outra posição. **Comparar locais** agrupa por frequência e ordena por SINR médio. Use o mesmo aparelho, frequência e posição do cabo do fone para comparar apenas a mudança desejada.

Favoritos e até 50 medições ficam salvos localmente. O registro contínuo tem limite de 12.000 leituras e permanece em memória; exporte antes de encerrar ou atualizar o app. Sair da tela, desligar ou alterar a sintonia interrompe a coleta. CSV usa ponto e vírgula e ponto decimal.

## Precisão e limites

- Valores RF são **brutos do driver**, sem conversão calibrada para dBm. Não são estimativas de watts, potência do transmissor ou distância.
- Cores do sinal são estimativas com limites ajustáveis, não garantia de áudio limpo.
- O serviço não oferece consulta da frequência atual: a interface identifica a frequência como **solicitada** ou comando de restauração aceito.
- A fonte de áudio FM é a identificação de fabricante `1998`, validada no alvo. Não há fallback para microfone nem para a mistura geral de áudio.
- FFT de 1.024 pontos com janela Hann e remoção de DC, captura a 44,1 kHz. O gráfico é do **áudio**, não do espectro de RF. RMS e dBFS são níveis digitais, não volts ou intensidade do campo.
- O app ainda depende da inicialização do receptor pela interface nativa, evitando uma falha de Looper observada no comando remoto de ligar.
- O APK original, bibliotecas do fabricante, firmware, fontes descompilados e chaves de assinatura não são distribuídos neste repositório.

## Compilar no Windows

Requisitos utilizados: JDK 25.0.3, Android SDK Platform 34 e Build Tools 36.0.0. O Java é compilado para Java 8 e o DEX tem API mínima 25. Não há Gradle ou dependências externas de bibliotecas.

```powershell
.\build.ps1 -Sdk 'C:\Android\Sdk' -Java 'C:\Program Files\Android\Android Studio\jbr'
```

Saída: `dist/FM-Lab-0.5-teste.apk`. Intermediários e chave local de desenvolvimento ficam em `.build/`, ignorados pelo Git. Um clone gera sua própria chave de testes; essa chave não substitui a assinatura dos APKs distribuídos. Para uma assinatura local existente, informe `-KeyStore 'C:\caminho\test-key.p12'` com a configuração de testes esperada pelo script. Não publique chaves privadas.

## Testes e arquitetura

```powershell
.\test.ps1 -Sdk 'C:\Android\Sdk' -Java 'C:\Program Files\Android\Android Studio\jbr'
```

Os testes cobrem FFT com tom conhecido, amplitude, DC, ausência de áudio, estatísticas com amostras ausentes, CSV, limites de qualidade, taxa e ordenação da busca. Testes em JVM não substituem validação do sintonizador real.

- `RadioBridge`: cliente Binder do serviço nativo, com chamadas limitadas às operações utilizadas.
- `RadioEvents`: observador de eventos do receptor.
- `BandScanner`: varredura sequencial e ranking de candidatos.
- `AudioScope` / `AudioAnalysis`: captura exclusiva da fonte FM e análise do PCM.
- `LabFeatures` / `Measurement`: favoritos, coletas nomeadas, registro e CSV.
- `MainActivity`, `ScopeView`, `SpectrumView`, `SignalPlot`: interface e gráficos.

Este repositório não define uma licença de redistribuição neste momento. A publicação do código não transfere direitos sobre componentes do sistema ou do firmware.
