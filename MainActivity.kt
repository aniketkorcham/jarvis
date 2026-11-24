package com.example.jarvisrealme

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var transcript: TextView
    private lateinit var systemStatus: TextView
    private lateinit var languageSpinner: Spinner
    private lateinit var tts: TextToSpeech
    private val http = OkHttpClient()
    private val prefs by lazy { getSharedPreferences("jarvis_v2", MODE_PRIVATE) }
    // Replace with your deployed HTTPS backend endpoint.
    private val backendUrl = "https://YOUR_SERVER_DOMAIN/chat"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        requestNeededPermissions()
        renderInterface()
        handleWakeIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWakeIntent(intent)
    }

    private fun handleWakeIntent(intent: Intent?) {
        if (intent?.action == ACTION_WAKE_DETECTED) {
            systemStatus.text = "WAKE WORD DETECTED  •  READY"
            transcript.text = "JARVIS: I am listening. Tap TALK to give your command."
            speak("Yes. I am here.")
            intent.action = null
        }
    }

    private fun renderInterface() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(22, 24, 22, 28)
            setBackgroundColor(Color.rgb(3, 9, 20))
        }

        val title = TextView(this).apply {
            text = "J.A.R.V.I.S."
            textSize = 29f
            letterSpacing = 0.28f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(0, 225, 255))
        }
        systemStatus = TextView(this).apply {
            text = if (prefs.getBoolean("wake_enabled", false))
                "SYSTEM ONLINE  •  WAKE WORD ENABLED" else "SYSTEM ONLINE  •  STANDBY"
            textSize = 11f
            letterSpacing = 0.12f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(0, 175, 220))
            setPadding(0, 8, 0, 8)
        }
        val hud = ReactorView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(220)
            )
        }
        languageSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                listOf("English", "हिन्दी", "मराठी")
            )
        }
        transcript = TextView(this).apply {
            text = "GOOD EVENING.\nJARVIS SYSTEMS READY."
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(10, 14, 10, 14)
        }

        val scrollContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(hud)
            addView(languageSpinner)
            addView(transcript)
            addView(hudButton("◉  TALK TO JARVIS") { beginSpeechRecognition() })
            addView(hudButton("◎  ENABLE / DISABLE  HEY JARVIS") { toggleWakeService() })
            addView(sectionLabel("PHONE AUTOMATION"))
            addView(hudButton("OPEN CAMERA") { openCamera() })
            addView(hudButton("OPEN PHONE DIALER") { openDialer() })
            addView(hudButton("OPEN SMS") { openSms() })
            addView(hudButton("OPEN GOOGLE MAPS") { openMaps() })
            addView(hudButton("SET AN ALARM") { setAlarm() })
            addView(hudButton("WI-FI / BLUETOOTH SETTINGS") { openConnectivityPanel() })
            addView(hudButton("OPEN APP SETTINGS") {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:$packageName")))
            })
            addView(hudButton("REALME BATTERY SETTINGS") { openBatterySettings() })
            addView(sectionLabel("QUICK APP LAUNCH"))
            addView(hudButton("YOUTUBE") { launchApp("com.google.android.youtube", "https://youtube.com") })
            addView(hudButton("WHATSAPP") { launchApp("com.whatsapp", "https://whatsapp.com") })
            addView(hudButton("CHROME") { launchApp("com.android.chrome", "https://google.com") })
            addView(hudButton("STOP WAKE WORD SERVICE") { stopWakeService() })
        }
        val scroll = ScrollView(this).apply { addView(scrollContent) }
        page.addView(title)
        page.addView(systemStatus)
        page.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        setContentView(page)
    }

    private fun sectionLabel(text: String) = TextView(this).apply {
        this.text = "—  $text  —"
        textSize = 11f
        letterSpacing = 0.16f
        gravity = Gravity.CENTER
        setTextColor(Color.rgb(0, 175, 220))
        setPadding(0, 18, 0, 5)
    }

    private fun hudButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = 13f
        setTextColor(Color.rgb(0, 225, 255))
        setBackgroundColor(Color.rgb(7, 35, 54))
        setOnClickListener { action() }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun requestNeededPermissions() {
        val required = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            required.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val missing = required.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) ActivityCompat.requestPermissions(this, missing.toTypedArray(), 44)
    }

    private fun localeForSelection(): Locale = when (languageSpinner.selectedItemPosition) {
        1 -> Locale("hi", "IN")
        2 -> Locale("mr", "IN")
        else -> Locale.US
    }

    private fun beginSpeechRecognition() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            requestNeededPermissions()
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeForSelection().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, localeForSelection().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to JARVIS")
        }
        try { startActivityForResult(intent, REQUEST_SPEECH) }
        catch (_: Exception) { reply("Speech recognition is unavailable on this device.") }
    }

    @Deprecated("Activity result API retained for simple compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SPEECH && resultCode == RESULT_OK) {
            val words = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!words.isNullOrBlank()) {
                transcript.text = "YOU: $words\n\nJARVIS: PROCESSING..."
                processCommand(words)
            }
        }
    }

    private fun processCommand(raw: String) {
        val q = raw.lowercase(Locale.ROOT)
        when {
            q.contains("open camera") || q.contains("कैमरा") || q.contains("कॅमेरा") -> openCamera()
            q.contains("open dialer") || q.contains("phone kholo") || q.contains("फोन") -> openDialer()
            q.contains("open sms") || q.contains("message") || q.contains("संदेश") -> openSms()
            q.contains("open maps") || q.contains("navigate") || q.contains("रस्ता") ||
                q.contains("नकाशा") -> openMaps()
            q.contains("set alarm") || q.contains("अलार्म") || q.contains("गजर") -> setAlarm()
            q.contains("wifi") || q.contains("wi-fi") || q.contains("bluetooth") ||
                q.contains("वाईफाय") -> openConnectivityPanel()
            q.contains("battery settings") -> openBatterySettings()
            q.contains("open youtube") || q.contains("यूट्यूब") ->
                launchApp("com.google.android.youtube", "https://youtube.com")
            q.contains("open whatsapp") || q.contains("व्हाट्सअॅप") ->
                launchApp("com.whatsapp", "https://whatsapp.com")
            q.contains("time") || q.contains("समय") || q.contains("वेळ") -> {
                val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                reply("The current time is $time")
            }
            q.contains("date") || q.contains("तारीख") || q.contains("दिनांक") -> {
                val date = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())
                reply("Today is $date")
            }
            q.contains("open settings") || q.contains("सेटिंग") ->
                safeStart(Intent(Settings.ACTION_SETTINGS), "Opening system settings.")
            else -> askAssistant(raw)
        }
    }

    private fun openCamera() {
        val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) safeStart(intent, "Opening camera.")
        else reply("No camera app was found.")
    }

    private fun openDialer() {
        safeStart(Intent(Intent.ACTION_DIAL, Uri.parse("tel:")), "Opening phone dialer.")
    }

    private fun openSms() {
        safeStart(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))
            .putExtra("sms_body", ""), "Opening messages. Review before sending.")
    }

    private fun openMaps() {
        val uri = Uri.parse("geo:0,0?q=")
        safeStart(Intent(Intent.ACTION_VIEW, uri).setPackage("com.google.android.apps.maps"),
            "Opening Google Maps.")
    }

    private fun setAlarm() {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM)
            .putExtra(AlarmClock.EXTRA_SKIP_UI, false)
        if (intent.resolveActivity(packageManager) != null) safeStart(intent, "Opening alarm setup.")
        else reply("No compatible clock app was found.")
    }

    private fun openConnectivityPanel() {
        val intent = if (android.os.Build.VERSION.SDK_INT >= 29)
            Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
        else Intent(Settings.ACTION_WIFI_SETTINGS)
        safeStart(intent, "Opening connectivity controls.")
    }

    private fun openBatterySettings() {
        val candidates = listOf(
            Intent("android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS"),
            Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS),
            Intent(Settings.ACTION_SETTINGS)
        )
        val selected = candidates.firstOrNull { it.resolveActivity(packageManager) != null }
        if (selected != null) safeStart(selected, "Opening battery settings.")
        else reply("Battery settings could not be opened.")
    }

    private fun launchApp(packageName: String, fallbackUrl: String) {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        if (launch != null) safeStart(launch, "Opening app.")
        else safeStart(Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)), "Opening web version.")
    }

    private fun safeStart(intent: Intent, message: String) {
        try {
            if (intent.resolveActivity(packageManager) != null || intent.component != null) {
                startActivity(intent)
                reply(message)
            } else reply("That action is not available on this phone.")
        } catch (_: Exception) {
            reply("Android could not open that action.")
        }
    }

    private fun toggleWakeService() {
        if (prefs.getBoolean("wake_enabled", false)) {
            stopWakeService()
            return
        }
        if (BuildConfig.PICOVOICE_ACCESS_KEY.isBlank()) {
            reply("Wake word is not configured. Add your Picovoice key and Jarvis keyword model first.")
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            requestNeededPermissions()
            return
        }
        try {
            ContextCompat.startForegroundService(this, Intent(this, WakeWordService::class.java))
            prefs.edit().putBoolean("wake_enabled", true).apply()
            systemStatus.text = "WAKE SERVICE START REQUESTED"
            reply("Wake word service is starting. Keep its notification visible.")
        } catch (_: Exception) {
            reply("Android blocked microphone service startup. Open JARVIS and try again.")
        }
    }

    private fun stopWakeService() {
        stopService(Intent(this, WakeWordService::class.java))
        prefs.edit().putBoolean("wake_enabled", false).apply()
        systemStatus.text = "SYSTEM ONLINE  •  STANDBY"
        reply("Wake word service stopped.")
    }

    private fun askAssistant(question: String) {
        if (backendUrl.contains("YOUR_SERVER_DOMAIN")) {
            reply("Connect your secure OpenAI backend first. Phone commands available locally still work.")
            return
        }
        val payload = JSONObject()
            .put("message", question)
            .put("language", languageSpinner.selectedItem.toString())
        val request = Request.Builder().url(backendUrl)
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()
        http.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { reply("I cannot connect to the AI server right now.") }
            }
            override fun onResponse(call: Call, response: Response) {
                val rawBody = response.body?.string()
                val answer = try {
                    JSONObject(rawBody ?: "{}").optString("reply", "No answer received.")
                } catch (_: Exception) { "The AI server returned an invalid response." }
                response.close()
                runOnUiThread { reply(answer) }
            }
        })
    }

    private fun reply(message: String) {
        transcript.text = "JARVIS:\n$message"
        speak(message)
    }

    private fun speak(message: String) {
        if (!::tts.isInitialized) return
        val desired = localeForSelection()
        val available = tts.isLanguageAvailable(desired)
        tts.language = if (available >= TextToSpeech.LANG_AVAILABLE) desired else Locale.US
        tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "jarvis_response")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts.language = Locale.US
    }

    override fun onDestroy() {
        if (::tts.isInitialized) { tts.stop(); tts.shutdown() }
        http.dispatcher.cancelAll()
        super.onDestroy()
    }

    companion object {
        const val ACTION_WAKE_DETECTED = "com.example.jarvisrealme.WAKE_DETECTED"
        private const val REQUEST_SPEECH = 100
    }
}

class ReactorView(context: android.content.Context) : View(context) {
    private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    private var phase = 0f
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            phase += 0.035f
            if (phase > 6.28f) phase = 0f
            invalidate()
            handler.postDelayed(this, 40)
        }
    }

    init { handler.post(tick) }

    override fun onDraw(canvas: android.graphics.Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(width, height) * 0.34f
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = 2.2f
        paint.color = Color.rgb(0, 190, 235)
        canvas.drawCircle(cx, cy, radius, paint)
        paint.strokeWidth = 1.2f
        canvas.drawCircle(cx, cy, radius * 0.78f, paint)
        canvas.drawCircle(cx, cy, radius * 0.58f, paint)
        paint.color = Color.rgb(0, 235, 255)
        paint.strokeWidth = 4f
        val sweep = 75f
        canvas.drawArc(cx-radius, cy-radius, cx+radius, cy+radius,
            Math.toDegrees(phase.toDouble()).toFloat(), sweep, false, paint)
        paint.style = android.graphics.Paint.Style.FILL
        paint.color = Color.argb(35, 0, 205, 255)
        canvas.drawCircle(cx, cy, radius * (0.38f + 0.025f * kotlin.math.sin(phase * 2)), paint)
        paint.color = Color.rgb(0, 225, 255)
        paint.textAlign = android.graphics.Paint.Align.CENTER
        paint.textSize = radius * 0.25f
        canvas.drawText("J", cx, cy + radius * 0.09f, paint)
        paint.textSize = 10f
        canvas.drawText("CORE SYSTEM", cx, cy + radius + 19f, paint)
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(tick)
        super.onDetachedFromWindow()
    }
}
