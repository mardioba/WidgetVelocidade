# Debug do Problema do GPS

## 🔍 Como Debugar

### 1. Instalar o APK Atualizado
```bash
# O APK está em:
app/build/outputs/apk/debug/app-debug.apk
```

### 2. Verificar Logs no Android Studio

1. **Abrir Android Studio**
2. **Conectar o dispositivo** via USB
3. **Abrir Logcat** (View → Tool Windows → Logcat)
4. **Filtrar por tag**: `SpeedometerService`

### 3. Logs Esperados

#### Quando o serviço inicia:
```
SpeedometerService onCreate started
ACCESS_FINE_LOCATION permission granted
ACCESS_COARSE_LOCATION permission granted
WakeLock acquired
GPS provider is enabled, requesting location updates...
Location updates requested successfully
SpeedometerService onCreate completed
```

#### Quando o GPS funciona:
```
Location changed: [latitude], [longitude]
Location accuracy: [X]m
Location time: [timestamp]
Location has speed: true
Location speed: [X] m/s
GPS Speed: [X] m/s = [Y] km/h
Final speed: [Y] km/h
```

#### Quando o GPS não tem velocidade:
```
Location changed: [latitude], [longitude]
Location has speed: false
Location speed: 0.0 m/s
GPS does not have speed data or speed is 0
Distance to last location: [X]m
Time difference: [Y]s
Calculated Speed: [X] m in [Y] s = [Z] km/h
Final speed: [Z] km/h
```

#### Quando o GPS perde sinal (novo):
```
GPS timeout - no updates received for 5000ms
```

### 4. Sistema de Timeout do GPS (NOVO)

O widget agora detecta automaticamente quando:
- **Não há atualizações de GPS por 5 segundos**
- **GPS está temporariamente indisponível**
- **GPS está fora de serviço**
- **GPS foi desabilitado**

**Aviso "SEM GPS" aparece quando:**
- ✅ GPS desabilitado nas configurações
- ✅ GPS temporariamente indisponível
- ✅ GPS fora de serviço
- ✅ **NOVO: Sem atualizações por 5 segundos** (útil para túneis, prédios, etc.)

### 5. Problemas Comuns e Soluções

#### Problema: "ACCESS_FINE_LOCATION permission not granted"
**Solução**: 
- Ir em Configurações → Apps → Widget Velocímetro → Permissões
- Ativar "Localização"

#### Problema: "GPS provider is disabled"
**Solução**:
- Ir em Configurações → Localização
- Ativar "Localização"
- Ativar "GPS"

#### Problema: "Location has speed: false"
**Causa**: Alguns dispositivos não fornecem velocidade diretamente
**Solução**: O app calcula velocidade baseada na distância (deve funcionar)

#### Problema: "Distance to last location: 0m"
**Causa**: Dispositivo parado ou GPS impreciso
**Solução**: Mover o dispositivo por alguns metros

#### Problema: "Speed filtered out as noise"
**Causa**: Velocidade muito baixa (< 0.5 km/h)
**Solução**: Mover mais rápido ou ajustar o filtro

#### Problema: "GPS timeout - no updates received for 5000ms"
**Causa**: GPS perdendo sinal (túneis, prédios, etc.)
**Solução**: Aguardar ou sair da área com bloqueio de sinal

### 6. Teste Específico

1. **Instalar o app**
2. **Conceder todas as permissões**
3. **Iniciar o widget**
4. **Sair ao ar livre** (importante!)
5. **Caminhar por 10-20 metros**
6. **Entrar em um túnel ou prédio** (para testar timeout)
7. **Verificar se o aviso "SEM GPS" aparece**
8. **Sair do túnel/prédio** e verificar se o aviso some

### 7. Comparação com Outro App

Se outro app funciona, verifique:
- **Qual app funciona?** (nome do app)
- **Que velocidade ele mostra?**
- **Os logs do nosso app mostram o quê?**
- **O outro app também perde sinal em túneis/prédios?**

### 8. Informações para Debug

Por favor, forneça:
1. **Marca e modelo do dispositivo**
2. **Versão do Android**
3. **Logs completos** do SpeedometerService
4. **Nome do app que funciona** para comparação
5. **Se o aviso "SEM GPS" aparece** quando você entra em túneis/prédios

### 9. Comandos Úteis

#### Verificar se o GPS está funcionando:
```bash
adb shell dumpsys location
```

#### Verificar permissões:
```bash
adb shell dumpsys package com.example.widgetvelocidade | grep permission
```

### 10. Próximos Passos

Se os logs mostrarem que:
- ✅ Permissões estão OK
- ✅ GPS está habilitado
- ✅ Localizações estão chegando
- ✅ Aviso "SEM GPS" aparece quando deve
- ❌ Mas velocidade continua 0

Então o problema pode ser:
1. **Filtro muito restritivo** (0.5 km/h)
2. **Dispositivo parado** durante o teste
3. **GPS impreciso** no local
4. **Necessidade de mais tempo** para estabilizar

### 11. Ajustes Possíveis

Se necessário, posso:
- **Reduzir o filtro** de velocidade mínima
- **Aumentar a frequência** de atualizações
- **Usar outros providers** de localização
- **Implementar média móvel** para suavizar
- **Ajustar o timeout** do GPS (atualmente 5 segundos)

### 12. Funcionalidades Implementadas

✅ **Aviso "SEM GPS"** quando:
- GPS desabilitado
- GPS temporariamente indisponível
- GPS fora de serviço
- **Sem atualizações por 5 segundos**

✅ **Logs detalhados** para debug
✅ **Timeout automático** do GPS
✅ **Aviso piscando** para chamar atenção
✅ **Recuperação automática** quando GPS volta

**Por favor, teste e me envie os logs para análise!** 