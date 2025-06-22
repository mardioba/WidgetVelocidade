# Instruções para Testar o Widget de Velocímetro

## 🔧 Correções Implementadas

### 1. Problema do GPS
- **Adicionado logs detalhados** para debugar o problema
- **Verificação se o GPS está habilitado** antes de solicitar atualizações
- **Fallback para cálculo de velocidade** baseado na distância entre pontos GPS
- **Filtro de ruído** para velocidades muito baixas (< 1 km/h)

### 2. Fundo Mais Claro
- **Gradiente atualizado** de azul escuro para azul mais claro
- **Cores**: `#4A90E2` → `#5DADE2`

## 📱 Como Testar

### 1. Instalar o APK
```bash
# O APK está em:
app/build/outputs/apk/debug/app-debug.apk
```

### 2. Configurar Permissões
1. **Abrir o app** pela primeira vez
2. **Conceder permissões de localização** quando solicitado
3. **Conceder permissão de overlay** nas configurações do sistema
4. **Ativar o GPS** nas configurações do dispositivo

### 3. Iniciar o Widget
1. **Tocar no botão** "Iniciar Widget de Velocímetro"
2. **Verificar se o widget aparece** na tela
3. **Arrastar o widget** para posição desejada

### 4. Testar a Velocidade
1. **Sair de casa/carro** e começar a se mover
2. **Verificar os logs** no Android Studio (Logcat):
   ```
   Tag: SpeedometerService
   ```
3. **Observar se a velocidade atualiza** no widget

## 🔍 Debugging

### Verificar Logs
No Android Studio, abra o Logcat e filtre por:
```
Tag: SpeedometerService
```

### Logs Esperados
- `Location changed: [latitude], [longitude]`
- `GPS Speed: [velocidade] m/s = [velocidade] km/h`
- `Calculated Speed: [distância] m in [tempo] s = [velocidade] km/h`

### Problemas Comuns

#### GPS não funciona
- Verificar se o GPS está ativado
- Verificar se as permissões foram concedidas
- Verificar se está ao ar livre (GPS funciona melhor fora de prédios)

#### Velocidade sempre zero
- Aguardar alguns segundos para o GPS estabilizar
- Verificar se está se movendo (GPS precisa de movimento para calcular velocidade)
- Verificar os logs para ver se há erros

#### Widget não aparece
- Verificar permissão de overlay
- Reiniciar o app
- Verificar se o serviço está rodando (notificação na barra de status)

## 🎯 Dicas para Teste

1. **Teste ao ar livre** - GPS funciona melhor fora de prédios
2. **Mova-se por alguns segundos** - GPS precisa de tempo para estabilizar
3. **Use um veículo** - velocidades mais altas são mais fáceis de detectar
4. **Verifique os logs** - eles mostram exatamente o que está acontecendo

## 📊 O que Esperar

- **Primeiros segundos**: Velocidade pode ficar em 0 enquanto o GPS estabiliza
- **Em movimento**: Velocidade deve atualizar a cada segundo
- **Parado**: Velocidade deve voltar para 0
- **Fundo**: Deve estar mais claro (azul claro em vez de azul escuro)

## 🐛 Se Ainda Não Funcionar

1. **Verificar logs** no Android Studio
2. **Reiniciar o app** completamente
3. **Verificar permissões** nas configurações do sistema
4. **Testar em outro dispositivo** se possível
5. **Verificar se o GPS funciona** em outros apps 