package com.example.widgetvelocidade

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationProvider
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Handler
import android.os.PowerManager
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat
import kotlin.math.*

class SpeedometerService : Service() {
    
    private lateinit var windowManager: WindowManager
    private lateinit var speedometerView: View
    private lateinit var locationManager: LocationManager
    private lateinit var powerManager: PowerManager
    private lateinit var wakeLock: PowerManager.WakeLock
    private var currentSpeed = 0f
    private var isDragging = false
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var lastLocation: Location? = null
    private var lastUpdateTime = 0L
    private var gpsSignalLost = false
    private var lastGpsUpdateTime = 0L
    private var gpsTimeoutHandler: Handler? = null
    private var gpsTimeoutRunnable: Runnable? = null
    
    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "SpeedometerChannel"
        private const val TAG = "SpeedometerService"
        private const val MAX_SPEED = 200f // Velocidade máxima em km/h
        private const val GPS_TIMEOUT = 5000L // 5 segundos sem GPS = sinal perdido
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "SpeedometerService onCreate started")
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        
        // Verificar permissões
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "ACCESS_FINE_LOCATION permission not granted")
        } else {
            Log.d(TAG, "ACCESS_FINE_LOCATION permission granted")
        }
        
        if (checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "ACCESS_COARSE_LOCATION permission not granted")
        } else {
            Log.d(TAG, "ACCESS_COARSE_LOCATION permission granted")
        }
        
        // Criar WakeLock para manter a tela ligada
        wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "SpeedometerService::WakeLock"
        )
        wakeLock.acquire()
        Log.d(TAG, "WakeLock acquired")
        
        setupSpeedometerView()
        setupLocationListener()
        
        Log.d(TAG, "SpeedometerService onCreate completed")
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.speedometer_widget),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.speedometer_active)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.speedometer_widget))
            .setContentText(getString(R.string.speedometer_active))
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun setupSpeedometerView() {
        speedometerView = SpeedometerWidgetView(this)
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }
        
        windowManager.addView(speedometerView, params)
        
        // Configurar detecção de toque para arrastar
        speedometerView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDragging = true
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isDragging) {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(speedometerView, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    isDragging = false
                    true
                }
                else -> false
            }
        }
    }
    
    private fun setupLocationListener() {
        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                Log.d(TAG, "Location changed: ${location.latitude}, ${location.longitude}")
                Log.d(TAG, "Location accuracy: ${location.accuracy}m")
                Log.d(TAG, "Location time: ${location.time}")
                Log.d(TAG, "Location has speed: ${location.hasSpeed()}")
                Log.d(TAG, "Location speed: ${location.speed} m/s")
                Log.d(TAG, "Location provider: ${location.provider}")
                
                // Marcar que temos sinal do GPS
                gpsSignalLost = false
                lastGpsUpdateTime = System.currentTimeMillis()
                
                // Cancelar timeout anterior
                gpsTimeoutRunnable?.let { gpsTimeoutHandler?.removeCallbacks(it) }
                
                // Agendar novo timeout
                scheduleGpsTimeout()
                
                var speedKmh = 0f
                
                // Tentar obter velocidade do GPS
                if (location.hasSpeed() && location.speed > 0) {
                    val speedMps = location.speed
                    speedKmh = speedMps * 3.6f
                    Log.d(TAG, "GPS Speed: $speedMps m/s = $speedKmh km/h")
                } else {
                    Log.d(TAG, "GPS does not have speed data or speed is 0")
                }
                
                // Se não tem velocidade do GPS, calcular baseado na distância
                if (speedKmh <= 0f && lastLocation != null) {
                    val distance = location.distanceTo(lastLocation!!)
                    val timeDiff = (location.time - lastUpdateTime) / 1000f // segundos
                    
                    Log.d(TAG, "Distance to last location: ${distance}m")
                    Log.d(TAG, "Time difference: ${timeDiff}s")
                    
                    if (timeDiff > 0 && distance > 0) {
                        speedKmh = (distance / 1000f) / (timeDiff / 3600f) // km/h
                        Log.d(TAG, "Calculated Speed: $distance m in $timeDiff s = $speedKmh km/h")
                    } else {
                        Log.d(TAG, "Cannot calculate speed: timeDiff=$timeDiff, distance=$distance")
                    }
                } else if (lastLocation == null) {
                    Log.d(TAG, "First location received, cannot calculate speed yet")
                }
                
                // Filtrar velocidades muito baixas (ruído) mas permitir velocidades baixas
                if (speedKmh < 0.2f) { // Reduzido de 0.5f para 0.2f
                    speedKmh = 0f
                    Log.d(TAG, "Speed filtered out as noise: $speedKmh km/h")
                }
                
                Log.d(TAG, "Final speed: $speedKmh km/h")
                
                currentSpeed = speedKmh
                lastLocation = location
                lastUpdateTime = location.time
                
                (speedometerView as SpeedometerWidgetView).updateSpeed(currentSpeed, gpsSignalLost)
            }
            
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                Log.d(TAG, "Provider status changed: $provider = $status")
                when (status) {
                    LocationProvider.AVAILABLE -> {
                        gpsSignalLost = false
                        Log.d(TAG, "GPS provider is available")
                        // Cancelar timeout quando GPS volta
                        gpsTimeoutRunnable?.let { gpsTimeoutHandler?.removeCallbacks(it) }
                    }
                    LocationProvider.TEMPORARILY_UNAVAILABLE -> {
                        gpsSignalLost = true
                        Log.w(TAG, "GPS provider is temporarily unavailable")
                    }
                    LocationProvider.OUT_OF_SERVICE -> {
                        gpsSignalLost = true
                        Log.w(TAG, "GPS provider is out of service")
                    }
                }
                (speedometerView as SpeedometerWidgetView).updateSpeed(currentSpeed, gpsSignalLost)
            }
            
            override fun onProviderEnabled(provider: String) {
                Log.d(TAG, "Provider enabled: $provider")
                gpsSignalLost = false
                // Cancelar timeout quando GPS volta
                gpsTimeoutRunnable?.let { gpsTimeoutHandler?.removeCallbacks(it) }
                (speedometerView as SpeedometerWidgetView).updateSpeed(currentSpeed, gpsSignalLost)
            }
            
            override fun onProviderDisabled(provider: String) {
                Log.d(TAG, "Provider disabled: $provider")
                gpsSignalLost = true
                (speedometerView as SpeedometerWidgetView).updateSpeed(currentSpeed, gpsSignalLost)
            }
        }
        
        try {
            // Verificar se o GPS está habilitado
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Log.w(TAG, "GPS provider is disabled")
                gpsSignalLost = true
                return
            }
            
            Log.d(TAG, "GPS provider is enabled, requesting location updates...")
            
            // Inicializar handler para timeout
            gpsTimeoutHandler = Handler(android.os.Looper.getMainLooper())
            
            // Solicitar atualizações com configurações mais agressivas
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                100L, // atualizar a cada 0.1 segundos (muito mais frequente)
                0f, // qualquer mudança de distância
                locationListener
            )
            
            // Também solicitar do provider de rede como backup
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                Log.d(TAG, "Network provider is enabled, requesting as backup...")
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    500L,
                    0f,
                    locationListener
                )
            }
            
            // Tentar obter última localização conhecida
            try {
                val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (lastKnownLocation != null) {
                    Log.d(TAG, "Last known location found: ${lastKnownLocation.latitude}, ${lastKnownLocation.longitude}")
                    lastLocation = lastKnownLocation
                    lastUpdateTime = lastKnownLocation.time
                } else {
                    Log.d(TAG, "No last known location available")
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Security exception when getting last known location", e)
            }
            
            Log.d(TAG, "Location updates requested successfully")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception when requesting location updates", e)
            gpsSignalLost = true
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting location updates", e)
            gpsSignalLost = true
        }
    }
    
    private fun scheduleGpsTimeout() {
        gpsTimeoutRunnable = Runnable {
            Log.w(TAG, "GPS timeout - no updates received for ${GPS_TIMEOUT}ms")
            gpsSignalLost = true
            (speedometerView as SpeedometerWidgetView).updateSpeed(currentSpeed, gpsSignalLost)
        }
        
        gpsTimeoutHandler?.postDelayed(gpsTimeoutRunnable!!, GPS_TIMEOUT)
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Cancelar timeout
        gpsTimeoutRunnable?.let { gpsTimeoutHandler?.removeCallbacks(it) }
        
        // Liberar WakeLock
        if (::wakeLock.isInitialized && wakeLock.isHeld) {
            wakeLock.release()
        }
        
        if (::windowManager.isInitialized && ::speedometerView.isInitialized) {
            windowManager.removeView(speedometerView)
        }
        if (::locationManager.isInitialized) {
            locationManager.removeUpdates(object : LocationListener {
                override fun onLocationChanged(location: Location) {}
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            })
        }
    }
    
    private inner class SpeedometerWidgetView(context: Context) : FrameLayout(context) {
        private val speedText: TextView
        private val gpsStatusText: TextView
        private val speedometerDrawable: SpeedometerDrawable
        
        init {
            // Configurar layout
            layoutParams = LayoutParams(200.dpToPx(), 200.dpToPx())
            setPadding(16.dpToPx(), 16.dpToPx(), 16.dpToPx(), 16.dpToPx())
            
            // Background com gradiente mais claro
            background = createBackgroundDrawable()
            
            // Botão de fechar
            val closeButton = ImageButton(context).apply {
                layoutParams = LayoutParams(32.dpToPx(), 32.dpToPx()).apply {
                    gravity = Gravity.TOP or Gravity.END
                }
                setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                setColorFilter(Color.WHITE)
                background = ShapeDrawable(OvalShape()).apply {
                    paint.color = Color.parseColor("#E74C3C")
                }
                setOnClickListener { stopSelf() }
            }
            addView(closeButton)
            
            // Velocímetro
            speedometerDrawable = SpeedometerDrawable()
            val speedometerView = View(context).apply {
                layoutParams = LayoutParams(160.dpToPx(), 160.dpToPx()).apply {
                    gravity = Gravity.CENTER
                }
                background = speedometerDrawable
            }
            addView(speedometerView)
            
            // Texto da velocidade (fonte maior)
            speedText = TextView(context).apply {
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.CENTER
                }
                textSize = 32f // Fonte aumentada de 24f para 32f
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
                text = "0"
            }
            addView(speedText)
            
            // Texto "km/h"
            val unitText = TextView(context).apply {
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.CENTER
                    topMargin = 30.dpToPx()
                }
                textSize = 12f
                setTextColor(Color.WHITE)
                text = getString(R.string.kmh)
            }
            addView(unitText)
            
            // Status do GPS
            gpsStatusText = TextView(context).apply {
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.BOTTOM or Gravity.CENTER
                    bottomMargin = 8.dpToPx()
                }
                textSize = 10f
                setTextColor(Color.YELLOW)
                typeface = Typeface.DEFAULT_BOLD
                text = getString(R.string.gps_ok)
                visibility = View.GONE // Inicialmente oculto
            }
            addView(gpsStatusText)
        }
        
        fun updateSpeed(speed: Float, gpsLost: Boolean) {
            speedText.text = speed.toInt().toString()
            speedometerDrawable.updateProgress(speed / MAX_SPEED) // Usar MAX_SPEED em vez de 120f
            
            // Atualizar status do GPS
            if (gpsLost) {
                gpsStatusText.text = getString(R.string.no_gps)
                gpsStatusText.setTextColor(Color.RED)
                gpsStatusText.visibility = View.VISIBLE
                
                // Piscar o texto para chamar atenção
                gpsStatusText.alpha = 0.5f
                gpsStatusText.animate()
                    .alpha(1f)
                    .setDuration(500)
                    .withEndAction {
                        gpsStatusText.animate()
                            .alpha(0.5f)
                            .setDuration(500)
                            .start()
                    }
                    .start()
            } else {
                gpsStatusText.visibility = View.GONE
                gpsStatusText.clearAnimation()
            }
            
            invalidate()
        }
        
        private fun createBackgroundDrawable(): ShapeDrawable {
            return ShapeDrawable().apply {
                shape = object : android.graphics.drawable.shapes.RectShape() {
                    override fun draw(canvas: Canvas, paint: Paint) {
                        val rect = rect()
                        val gradient = LinearGradient(
                            0f, 0f, 0f, height.toFloat(),
                            Color.parseColor("#4A90E2"), // Azul mais claro
                            Color.parseColor("#5DADE2"), // Azul ainda mais claro
                            Shader.TileMode.CLAMP
                        )
                        paint.shader = gradient
                        canvas.drawRoundRect(rect.left.toFloat(), rect.top.toFloat(), 
                                           rect.right.toFloat(), rect.bottom.toFloat(), 
                                           16.dpToPx().toFloat(), 16.dpToPx().toFloat(), paint)
                    }
                }
            }
        }
        
        private fun Int.dpToPx(): Int {
            return (this * resources.displayMetrics.density).toInt()
        }
    }
    
    private inner class SpeedometerDrawable : ShapeDrawable() {
        private var progress = 0f
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val strokeWidth = 12f // Aumentado para melhor visualização
        
        init {
            shape = object : android.graphics.drawable.shapes.OvalShape() {
                override fun draw(canvas: Canvas, paint: Paint) {
                    val rect = rect()
                    val centerX = rect.centerX().toFloat()
                    val centerY = rect.centerY().toFloat()
                    val radius = (rect.width() - strokeWidth) / 2
                    
                    // Desenhar arco de fundo (cinza)
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = strokeWidth
                    paint.color = Color.GRAY
                    paint.alpha = 80
                    paint.strokeCap = Paint.Cap.ROUND
                    canvas.drawArc(centerX - radius, centerY - radius, 
                                 centerX + radius, centerY + radius, 
                                 -135f, 270f, false, paint)
                    
                    // Desenhar arco de progresso (azul)
                    paint.color = Color.parseColor("#3498DB")
                    paint.alpha = 255
                    canvas.drawArc(centerX - radius, centerY - radius, 
                                 centerX + radius, centerY + radius, 
                                 -135f, 270f * progress, false, paint)
                    
                    // Desenhar borda externa para melhor definição
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 2f
                    paint.color = Color.WHITE
                    paint.alpha = 100
                    canvas.drawCircle(centerX, centerY, radius + strokeWidth/2, paint)
                    
                    // Desenhar ponto central
                    paint.style = Paint.Style.FILL
                    paint.color = Color.WHITE
                    paint.alpha = 255
                    canvas.drawCircle(centerX, centerY, 6f, paint)
                    
                    // Desenhar borda do ponto central
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 2f
                    paint.color = Color.parseColor("#3498DB")
                    canvas.drawCircle(centerX, centerY, 6f, paint)
                }
            }
        }
        
        fun updateProgress(newProgress: Float) {
            progress = newProgress.coerceIn(0f, 1f)
        }
    }
} 