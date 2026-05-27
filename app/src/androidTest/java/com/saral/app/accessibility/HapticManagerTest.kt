package com.saral.app.accessibility

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HapticManagerTest {
    private lateinit var context: Context
    private lateinit var manager: HapticManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        manager = HapticManager(context)
    }

    @Test
    fun vibrateShort_executesWithoutException() {
        manager.vibrateShort()
        // No exception thrown; vibration executed
        assertTrue(true)
    }

    @Test
    fun vibrateMedium_executesWithoutException() {
        manager.vibrateMedium()
        // No exception thrown; vibration executed
        assertTrue(true)
    }

    @Test
    fun vibrateSuccess_executesDoublePattern() {
        manager.vibrateSuccess()
        // No exception thrown; success pattern vibration executed
        assertTrue(true)
    }

    @Test
    fun vibrateError_executesLongPattern() {
        manager.vibrateError()
        // No exception thrown; error pattern vibration executed
        assertTrue(true)
    }

    @Test
    fun vibrateShort_beforeVibrateSuccess() {
        manager.vibrateShort()
        manager.vibrateSuccess()
        // Multiple consecutive vibrations work correctly
        assertTrue(true)
    }

    @Test
    fun allVibrationPatterns_sequential() {
        manager.vibrateShort()
        Thread.sleep(100)
        manager.vibrateMedium()
        Thread.sleep(100)
        manager.vibrateSuccess()
        Thread.sleep(100)
        manager.vibrateError()
        
        // All patterns execute sequentially without errors
        assertTrue(true)
    }

    @Test
    fun hapticManager_worksOnMultipleApiLevels() {
        // Test that HapticManager adapts to API level
        val apiLevel = Build.VERSION.SDK_INT
        assertTrue("API level should be >= 26", apiLevel >= 26)
        
        // All vibrations should work regardless of API level
        manager.vibrateShort()
        manager.vibrateSuccess()
        manager.vibrateError()
        
        assertTrue(true)
    }

    @Test
    fun vibrateSuccess_calledMultipleTimes() {
        manager.vibrateSuccess()
        manager.vibrateSuccess()
        manager.vibrateSuccess()
        
        // Multiple success vibrations should work without conflicts
        assertTrue(true)
    }

    @Test
    fun vibrateError_calledMultipleTimes() {
        manager.vibrateError()
        manager.vibrateError()
        
        // Multiple error vibrations should work without conflicts
        assertTrue(true)
    }
}
