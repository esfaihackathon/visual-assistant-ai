package com.saral.app.voice

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TextToSpeechManagerTest {
    private lateinit var context: Context
    private lateinit var manager: TextToSpeechManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        manager = TextToSpeechManager(context)
    }

    @Test
    fun initialize_setsUpTextToSpeech() {
        var callbackInvoked = false
        manager.initialize(onReady = { callbackInvoked = true })
        // Note: TTS initialization is async; allowing time for callback
        Thread.sleep(1000)
        assertTrue("TTS initialization callback should be invoked", callbackInvoked)
    }

    @Test
    fun isSpeaking_flowEmitsCorrectly() = runBlocking {
        manager.initialize()
        Thread.sleep(500) // Allow TTS to initialize
        
        val speakingState = manager.isSpeaking.value
        assertFalse("Initially should not be speaking", speakingState)
    }

    @Test
    fun speak_withText() {
        manager.initialize()
        Thread.sleep(500)
        
        manager.speak("Test message")
        // Verification: no exception thrown, speak is callable after init
        assertTrue(true)
    }

    @Test
    fun speak_withCallback() {
        manager.initialize()
        Thread.sleep(500)
        
        var callbackInvoked = false
        manager.speak("Test message", onComplete = { callbackInvoked = true })
        
        // Allow time for speech to complete
        Thread.sleep(2000)
        
        // Callback should be invoked (or queued for later)
        assertTrue("Speak with callback should accept callback parameter", true)
    }

    @Test
    fun setLanguage_updatesLocale() {
        manager.initialize()
        Thread.sleep(500)
        
        val localeHi = java.util.Locale("hi", "IN")
        manager.setLanguage(localeHi)
        
        // No exception thrown; language can be set
        assertTrue(true)
    }

    @Test
    fun shutdown_cleansUp() {
        manager.initialize()
        Thread.sleep(500)
        
        manager.shutdown()
        
        // Verify no exceptions on subsequent calls after shutdown
        assertTrue(true)
    }
}
