package com.saral.app.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
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
                selectBestVoice()
                tts?.setSpeechRate(0.88f)
                tts?.setPitch(0.9f)
                isInitialized = true

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) { _isSpeaking.value = true }
                    override fun onDone(utteranceId: String?) { _isSpeaking.value = false }
                    @Deprecated("Deprecated in API")
                    override fun onError(utteranceId: String?) { _isSpeaking.value = false }
                })

                onReady()
            }
        }
    }

    // Pick the highest-quality installed English voice.
    // Google TTS neural voices score QUALITY_HIGH (300) or QUALITY_VERY_HIGH (400).
    private fun selectBestVoice() {
        val voices = tts?.voices ?: run { tts?.language = currentLocale; return }

        val best = voices
            .filter { voice ->
                voice.locale.language == "en" &&
                !voice.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) &&
                !voice.isNetworkConnectionRequired
            }
            .maxByOrNull { it.quality }

        // Fall back to any English network voice if nothing installed beats normal quality
        val fallback = voices
            .filter { it.locale.language == "en" &&
                      !it.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) }
            .maxByOrNull { it.quality }

        val chosen = if ((best?.quality ?: 0) >= Voice.QUALITY_HIGH) best
                     else (fallback ?: best)

        if (chosen != null) {
            tts?.voice = chosen
        } else {
            tts?.language = currentLocale
        }
    }

    fun speak(text: String, onComplete: (() -> Unit)? = null) {
        if (!isInitialized) return

        val utteranceId = "saral_${System.currentTimeMillis()}"

        if (onComplete != null) {
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) { _isSpeaking.value = true }
                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    onComplete()
                }
                @Deprecated("Deprecated in API")
                override fun onError(utteranceId: String?) { _isSpeaking.value = false }
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
        selectBestVoice()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
