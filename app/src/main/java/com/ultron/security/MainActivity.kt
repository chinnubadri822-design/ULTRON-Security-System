package com.ultron.security

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private val bg = Color.rgb(5, 10, 22)
    private val panel = Color.rgb(13, 23, 43)
    private val cyan = Color.rgb(0, 220, 255)
    private lateinit var messages: LinearLayout
    private lateinit var input: EditText
    private lateinit var status: TextView
    private lateinit var tts: TextToSpeech
    private var speech: SpeechRecognizer? = null
    private var listening = false
    private val prefs by lazy { getSharedPreferences("ultron_settings", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        tts = TextToSpeech(this, this)
        showHome()
    }

    override fun onInit(result: Int) {
        if (result == TextToSpeech.SUCCESS) tts.language = Locale.getDefault()
    }

    private fun rounded(color: Int, radius: Float = 18f): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = radius }

    private fun label(text: String, size: Float, color: Int = Color.WHITE, bold: Boolean = false): TextView =
        TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun showHome() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 22, 22, 16)
            setBackgroundColor(bg)
        }
        val top = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
        }
        top.addView(label("ULTRON", 25f, cyan, true), LinearLayout.LayoutParams(0, -2, 1f))
        val settings = Button(this).apply {
            text = "SETTINGS"
            textSize = 11f
            setTextColor(cyan)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { settingsDialog() }
        }
        top.addView(settings)
        root.addView(top)
        root.addView(label("PERSONAL AI • ANDROID EDITION", 10f, Color.LTGRAY))
        val orb = OrbView(this)
        root.addView(orb, LinearLayout.LayoutParams(-1, 210).apply { topMargin = 10; bottomMargin = 4 })
        status = label("SYSTEM ONLINE  •  READY", 12f, cyan, true).apply { gravity = Gravity.CENTER }
        root.addView(status)
        root.addView(label("How can I help you?", 21f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            setPadding(0, 14, 0, 14)
        })
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            background = rounded(panel, 18f)
            setPadding(12, 12, 12, 12)
        }
        messages = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(messages)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        addMessage("ULTRON", "Hello. I am online. Add your Gemini API key in Settings to enable AI replies.")
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 12, 0, 0)
        }
        input = EditText(this).apply {
            hint = "Message ULTRON..."
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            textSize = 15f
            setSingleLine(true)
            setPadding(14, 4, 14, 4)
            background = rounded(panel, 24f)
        }
        controls.addView(input, LinearLayout.LayoutParams(0, 52, 1f))
        val mic = Button(this).apply {
            text = "🎙"
            textSize = 18f
            setTextColor(cyan)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { startVoiceInput() }
        }
        controls.addView(mic)
        val send = Button(this).apply {
            text = "SEND"
            setTextColor(bg)
            textSize = 12f
            setBackground(rounded(cyan, 22f))
            setOnClickListener { sendMessage() }
        }
        controls.addView(send, LinearLayout.LayoutParams(-2, 48))
        root.addView(controls)
        setContentView(root)
    }

    private fun addMessage(who: String, text: String) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(if (who == "YOU") Color.rgb(18, 48, 68) else Color.rgb(20, 32, 54), 14f)
            setPadding(14, 10, 14, 10)
        }
        card.addView(label(who, 10f, if (who == "YOU") cyan else Color.rgb(100, 230, 255), true))
        card.addView(label(text, 14f, Color.WHITE).apply { setPadding(0, 4, 0, 0) })
        val lp = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 9 }
        messages.addView(card, lp)
    }

    private fun sendMessage() {
        val prompt = input.text.toString().trim()
        if (prompt.isEmpty()) return
        input.setText("")
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(input.windowToken, 0)
        addMessage("YOU", prompt)
        val key = prefs.getString("gemini_key", "") ?: ""
        if (key.isBlank()) {
            addMessage("ULTRON", "AI is not configured yet. Open Settings and add your Gemini API key.")
            status.text = "WAITING FOR API CONFIGURATION"
            return
        }
        status.text = "THINKING..."
        thread {
            val answer = try {
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$key")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15000
                    readTimeout = 30000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }
                val body = JSONObject().put("contents", JSONArray().put(
                    JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                )).toString()
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
                val response = stream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val json = JSONObject(response)
                if (json.has("candidates")) json.getJSONArray("candidates")
                    .getJSONObject(0).getJSONObject("content").getJSONArray("parts")
                    .getJSONObject(0).optString("text", "I could not read that response.")
                else "AI request failed. Check your API key and internet connection."
            } catch (e: Exception) {
                "I couldn't connect. Check your internet and API key, then try again."
            }
            runOnUiThread {
                addMessage("ULTRON", answer)
                status.text = "SYSTEM ONLINE  •  READY"
                if (prefs.getBoolean("voice_reply", true)) tts.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "ultron_reply")
            }
        }
    }

    private fun settingsDialog() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 8, 20, 4)
        }
        box.addView(label("Gemini API key", 14f, Color.BLACK, true))
        val keyInput = EditText(this).apply {
            hint = "Paste your own API key"
            setText(prefs.getString("gemini_key", ""))
            inputType = 129
        }
        box.addView(keyInput)
        val voice = CheckBox(this).apply {
            text = "Speak AI replies"
            isChecked = prefs.getBoolean("voice_reply", true)
        }
        box.addView(voice)
        AlertDialogBuilder(this).show(box, {
            prefs.edit().putString("gemini_key", keyInput.text.toString().trim())
                .putBoolean("voice_reply", voice.isChecked).apply()
            Toast.makeText(this, "Settings saved on this device.", Toast.LENGTH_SHORT).show()
        })
    }

    private fun startVoiceInput() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition is unavailable on this device.", Toast.LENGTH_LONG).show()
            return
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 41)
            return
        }
        speech?.destroy()
        speech = SpeechRecognizer.createSpeechRecognizer(this)
        speech?.setRecognitionListener(object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { status.text = "LISTENING..." }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { status.text = "PROCESSING VOICE..." }
            override fun onError(error: Int) { status.text = "SYSTEM ONLINE  •  READY" }
            override fun onResults(results: Bundle?) {
                val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!spoken.isNullOrBlank()) {
                    input.setText(spoken)
                    sendMessage()
                } else status.text = "SYSTEM ONLINE  •  READY"
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to ULTRON")
        }
        speech?.startListening(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 41 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) startVoiceInput()
    }

    override fun onDestroy() {
        speech?.destroy()
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }

    private class OrbView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var phase = 0f
        init {
            setLayerType(LAYER_TYPE_SOFTWARE, null)
            post(object : Runnable {
                override fun run() {
                    phase += 0.035f
                    invalidate()
                    postDelayed(this, 40)
                }
            })
        }
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f
            val radius = 66f + kotlin.math.sin(phase.toDouble()).toFloat() * 4f
            paint.shader = RadialGradient(cx, cy, radius * 1.7f,
                intArrayOf(Color.argb(90, 0, 220, 255), Color.argb(25, 0, 100, 255), Color.TRANSPARENT),
                null, Shader.TileMode.CLAMP)
            canvas.drawCircle(cx, cy, radius * 1.7f, paint)
            paint.shader = RadialGradient(cx - 20, cy - 22, radius,
                intArrayOf(Color.WHITE, Color.rgb(0, 210, 255), Color.rgb(0, 35, 90)),
                null, Shader.TileMode.CLAMP)
            canvas.drawCircle(cx, cy, radius, paint)
            paint.shader = null
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            paint.color = Color.argb(190, 0, 220, 255)
            canvas.drawCircle(cx, cy, radius + 12f + kotlin.math.sin(phase.toDouble() * 1.3).toFloat() * 3f, paint)
            paint.style = Paint.Style.FILL
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
            paint.textSize = 13f
            paint.color = Color.WHITE
            canvas.drawText("U", cx, cy + 5f, paint)
        }
    }

    private class AlertDialogBuilder(private val activity: Activity) {
        fun show(view: View, save: () -> Unit) {
            android.app.AlertDialog.Builder(activity)
                .setTitle("ULTRON SETTINGS")
                .setView(view)
                .setPositiveButton("SAVE") { _, _ -> save() }
                .setNegativeButton("CANCEL", null)
                .show()
        }
    }
}
