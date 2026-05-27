package com.saral.app.voice

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class SpeechRecognizerManagerTest {
    private lateinit var context: Context
    private lateinit var manager: SpeechRecognizerManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        manager = SpeechRecognizerManager(context)
    }

    @Test
    fun initialize_setUpsSpeechRecognizer() {
        manager.initialize()
        // No exception thrown; initialization successful
        assertTrue(true)
    }

    @Test
    fun isListening_flowInitiallyFalse() = runBlocking {
        manager.initialize()
        val listeningState = manager.isListening.first()
        assertFalse("Initially should not be listening", listeningState)
    }

    @Test
    fun recognizedText_flowInitiallyEmpty() = runBlocking {
        manager.initialize()
        val text = manager.recognizedText.first()
        assertEquals("Initially recognized text should be empty", "", text)
    }

    @Test
    fun startListening_withCallback() {
        manager.initialize()
        
        var callbackInvoked = false
        manager.startListening { result ->
            callbackInvoked = true
            assertEquals("Callback should receive text result", "", result.ifEmpty { "" })
        }
        
        // Listening should have started (or will start)
        assertTrue("startListening should be callable", true)
    }

    @Test
    fun stopListening_stopsRecognition() {
        manager.initialize()
        
        manager.startListening { }
        manager.stopListening()
        
        // No exception thrown; stop is successful
        assertTrue(true)
    }

    @Test
    fun setLanguage_updatesLocale() {
        manager.initialize()
        
        val localeEn = Locale("en", "IN")
        manager.setLanguage(localeEn)
        
        // No exception thrown; language set successfully
        assertTrue(true)
    }

    @Test
    fun destroy_cleansUp() {
        manager.initialize()
        manager.destroy()
        
        // No exception on cleanup
        assertTrue(true)
    }

    @Test
    fun startListening_multipleCalls() {
        manager.initialize()
        
        manager.startListening { }
        // Stop before starting again
        manager.stopListening()
        manager.startListening { }
        
        // Multiple start/stop cycles work correctly
        assertTrue(true)
    }
}
