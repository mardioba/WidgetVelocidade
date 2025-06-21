# Widget Velocímetro

Um widget de velocímetro flutuante para Android que mostra a velocidade em tempo real usando GPS.

## 🎯 Funcionalidades

### Velocímetro Analógico + Digital
- **Display Digital**: Mostra a velocidade atual no centro (ex: "42 km/h")
- **Velocímetro Analógico**: Anel/ponteiro que preenche ou gira conforme a velocidade aumenta
- **Escala Customizável**: Preenchimento de 0 km/h até 120 km/h

### GPS em Tempo Real
- Leitura de velocidade via GPS do dispositivo
- Atualização a cada segundo
- Permissões de localização solicitadas em tempo de execução

### Janela Flutuante (Always on Top)
- Widget exibido acima de todos os apps como overlay
- Funciona mesmo quando outros apps estão abertos
- Não requer abertura do app principal

### Arrastável com o Dedo
- Toque, segure e arraste o widget livremente pela tela
- Posicionamento personalizado em qualquer lugar

### Design Moderno
- Visual limpo com cores e gradientes suaves
- Botão de fechar integrado
- Interface responsiva e intuitiva

## 🧰 Requisitos Técnicos

- **Linguagem**: Kotlin
- **Plataforma**: Android (API 24+)
- **Arquitetura**: MVVM com Jetpack Compose
- **Localização**: LocationManager para GPS
- **Overlay**: SYSTEM_ALERT_WINDOW com TYPE_APPLICATION_OVERLAY
- **Arrastar**: GestureDetector com MotionEvent

## 📱 Permissões Necessárias

O app solicita automaticamente as seguintes permissões:

- `ACCESS_FINE_LOCATION` - Localização precisa para GPS
- `ACCESS_COARSE_LOCATION` - Localização aproximada
- `SYSTEM_ALERT_WINDOW` - Exibir sobre outros apps
- `FOREGROUND_SERVICE` - Serviço em primeiro plano
- `POST_NOTIFICATIONS` - Notificações (Android 13+)

## 🚀 Como Usar

1. **Instalar o App**: Compile e instale o APK no dispositivo
2. **Conceder Permissões**: 
   - Permissões de localização quando solicitadas
   - Permissão de overlay nas configurações do sistema
3. **Iniciar Widget**: Toque em "Iniciar Widget de Velocímetro"
4. **Posicionar**: Arraste o widget para a posição desejada
5. **Usar**: O widget mostrará sua velocidade em tempo real
6. **Fechar**: Toque no botão X vermelho para fechar

## 🏗️ Estrutura do Projeto

```
app/src/main/java/com/example/widgetvelocidade/
├── MainActivity.kt              # Tela principal com permissões
└── SpeedometerService.kt        # Serviço do widget flutuante

app/src/main/res/
├── values/
│   └── strings.xml              # Strings do app
└── AndroidManifest.xml          # Configurações e permissões
```

## 🔧 Configuração de Desenvolvimento

### Pré-requisitos
- Android Studio Arctic Fox ou superior
- Android SDK API 24+
- Kotlin 1.9+

### Dependências Principais
```kotlin
// Compose
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.activity:activity-compose")

// Localização
implementation("com.google.android.gms:play-services-location")
```

### Compilação
```bash
./gradlew assembleDebug
```

## 🎨 Personalização

### Cores do Widget
As cores podem ser personalizadas no `SpeedometerWidget`:
```kotlin
// Gradiente de fundo
Brush.radialGradient(
    colors = listOf(
        Color(0xFF2C3E50),  // Azul escuro
        Color(0xFF34495E)   // Cinza azulado
    )
)

// Cor do progresso
Color(0xFF3498DB)  // Azul
```

### Escala de Velocidade
A escala máxima pode ser alterada:
```kotlin
val maxSpeed = 120f  // Alterar para velocidade desejada
```

## 📋 Notas de Implementação

### Serviço em Primeiro Plano
O widget usa um serviço em primeiro plano para:
- Manter o GPS ativo
- Exibir notificação persistente
- Evitar que o sistema mate o processo

### Gestão de Permissões
- Permissões solicitadas em tempo de execução
- Fallback para configurações do sistema
- Tratamento de casos de permissão negada

### Performance
- Atualização otimizada (1 segundo)
- Uso eficiente de recursos GPS
- Limpeza adequada de listeners

## 🐛 Solução de Problemas

### Widget não aparece
- Verificar permissão de overlay
- Reiniciar o app
- Verificar se o GPS está ativo

### Velocidade não atualiza
- Verificar permissões de localização
- Sair e entrar em movimento
- Verificar se o GPS está funcionando

### Widget trava ao arrastar
- Reiniciar o serviço
- Verificar memória disponível
- Limpar cache do app

## 📄 Licença

Este projeto é de código aberto e está disponível sob a licença MIT.

## 🤝 Contribuições

Contribuições são bem-vindas! Por favor:
1. Faça um fork do projeto
2. Crie uma branch para sua feature
3. Commit suas mudanças
4. Abra um Pull Request

## 📞 Suporte

Para suporte ou dúvidas, abra uma issue no repositório do projeto. 