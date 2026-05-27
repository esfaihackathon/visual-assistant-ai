package com.saral.app.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextToSpeechManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var tts: TextToSpeech? = null
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    private var isInitialized = false
    private var currentLocale: Locale = Locale("en", "IN")

    fun initialize(onReady: () -> Unit = {}) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = currentLocale
                tts?.setSpeechRate(0.9f)
                tts?.setPitch(1.0f)
                isInitialized = true

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    @Deprecated("Deprecated in API")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                })

                onReady()
            }
        }
    }

    fun speak(text: String, onComplete: (() -> Unit)? = null) {
        if (!isInitialized) return

        val utteranceId = "saral_${System.currentTimeMillis()}"

        if (onComplete != null) {
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    onComplete()
                }

                @Deprecated("Deprecated in API")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }
            })
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun setLanguage(locale: Locale) {
        currentLocale = locale
        tts?.language = locale
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
