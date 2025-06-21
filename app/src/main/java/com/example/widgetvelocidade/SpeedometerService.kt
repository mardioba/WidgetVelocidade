package com.example.widgetvelocidade

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat
import kotlin.math.*

class SpeedometerService : Service() {
    
    private lateinit var windowManager: WindowManager
    private lateinit var speedometerView: View
    private lateinit var locationManager: LocationManager
    private var currentSpeed = 0f
    private var isDragging = false
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    
    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "SpeedometerChannel"
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        
        setupSpeedometerView()
        setupLocationListener()
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
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
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
                val speedMps = location.speed // velocidade em m/s
                val speedKmh = speedMps * 3.6f // converter para km/h
                currentSpeed = speedKmh
                (speedometerView as SpeedometerWidgetView).updateSpeed(currentSpeed)
            }
            
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }
        
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L, // atualizar a cada 1 segundo
                0f, // qualquer mudança de distância
                locationListener
            )
        } catch (e: SecurityException) {
            // Permissões não concedidas
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
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
        private val speedometerDrawable: SpeedometerDrawable
        
        init {
            // Configurar layout
            layoutParams = LayoutParams(200.dpToPx(), 200.dpToPx())
            setPadding(16.dpToPx(), 16.dpToPx(), 16.dpToPx(), 16.dpToPx())
            
            // Background com gradiente
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
            
            // Texto da velocidade
            speedText = TextView(context).apply {
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.CENTER
                }
                textSize = 24f
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
        }
        
        fun updateSpeed(speed: Float) {
            speedText.text = speed.toInt().toString()
            speedometerDrawable.updateProgress(speed / 120f) // 120 km/h máximo
            invalidate()
        }
        
        private fun createBackgroundDrawable(): ShapeDrawable {
            return ShapeDrawable().apply {
                shape = object : android.graphics.drawable.shapes.RectShape() {
                    override fun draw(canvas: Canvas, paint: Paint) {
                        val rect = rect()
                        val gradient = LinearGradient(
                            0f, 0f, 0f, height.toFloat(),
                            Color.parseColor("#2C3E50"),
                            Color.parseColor("#34495E"),
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
        private val strokeWidth = 8f
        
        init {
            shape = object : android.graphics.drawable.shapes.OvalShape() {
                override fun draw(canvas: Canvas, paint: Paint) {
                    val rect = rect()
                    val centerX = rect.centerX().toFloat()
                    val centerY = rect.centerY().toFloat()
                    val radius = (rect.width() - strokeWidth) / 2
                    
                    // Desenhar arco de fundo
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = strokeWidth
                    paint.color = Color.GRAY
                    paint.alpha = 80
                    canvas.drawArc(centerX - radius, centerY - radius, 
                                 centerX + radius, centerY + radius, 
                                 -135f, 270f, false, paint)
                    
                    // Desenhar arco de progresso
                    paint.color = Color.parseColor("#3498DB")
                    canvas.drawArc(centerX - radius, centerY - radius, 
                                 centerX + radius, centerY + radius, 
                                 -135f, 270f * progress, false, paint)
                    
                    // Desenhar ponteiro
                    val angle = progress * 270f - 135f
                    val pointerLength = radius * 0.8f
                    val endX = centerX + pointerLength * cos(Math.toRadians(angle.toDouble())).toFloat()
                    val endY = centerY + pointerLength * sin(Math.toRadians(angle.toDouble())).toFloat()
                    
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 3f
                    paint.color = Color.WHITE
                    canvas.drawLine(centerX, centerY, endX, endY, paint)
                    
                    // Desenhar ponto central
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(centerX, centerY, 4f, paint)
                }
            }
        }
        
        fun updateProgress(newProgress: Float) {
            progress = newProgress.coerceIn(0f, 1f)
        }
    }
} 