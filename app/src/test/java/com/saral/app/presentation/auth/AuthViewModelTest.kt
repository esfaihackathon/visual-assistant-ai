package com.saral.app.presentation.auth

import org.junit.Assert.*
import org.junit.Test

class AuthViewModelTest {

    @Test
    fun initialState_isBiometricPending() {
        val vm = AuthViewModel()
        assertTrue(vm.authStep.value is AuthStep.BiometricPending)
    }

    @Test
    fun noSpeakCallback_doesNotCrash_onBiometricSuccess() {
        val vm = AuthViewModel()
        vm.onBiometricSuccess()
        assertTrue(vm.authStep.value is AuthStep.Success)
    }

    @Test
    fun onBiometricSuccess_setsSuccess() {
        val vm = AuthViewModel()
        vm.onBiometricSuccess()
        assertEquals(AuthStep.Success, vm.authStep.value)
    }

    @Test
    fun onBiometricSuccess_speaksWelcomeMessage() {
        val spoken = mutableListOf<String>()
        val vm = AuthViewModel()
        vm.setSpeakCallback { text -> spoken.add(text) }
        vm.onBiometricSuccess()
        assertTrue(spoken.any { it.contains("Welcome", ignoreCase = true) })
    }

    @Test
    fun onBiometricSuccess_speaksAllOptions() {
        val spoken = mutableListOf<String>()
        val vm = AuthViewModel()
        vm.setSpeakCallback { text -> spoken.add(text) }
        vm.onBiometricSuccess()
        val all = spoken.joinToString(" ").lowercase()
        assertTrue(all.contains("balance"))
        assertTrue(all.contains("transfer"))
        assertTrue(all.contains("cheque"))
        assertTrue(all.contains("transaction"))
        assertTrue(all.contains("help"))
    }

    @Test
    fun reset_returnsToBiometricPending() {
        val vm = AuthViewModel()
        vm.onBiometricSuccess()
        vm.reset()
        assertEquals(AuthStep.BiometricPending, vm.authStep.value)
    }
}
