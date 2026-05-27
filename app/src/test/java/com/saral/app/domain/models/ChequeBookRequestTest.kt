package com.saral.app.domain.models

import org.junit.Assert.*
import org.junit.Test

class ChequeBookRequestTest {

    @Test
    fun creation_withValidData() {
        val request = ChequeBookRequest(
            status = "REQUESTED",
            estimatedDeliveryDays = 5
        )
        assertEquals("REQUESTED", request.status)
        assertEquals(5, request.estimatedDeliveryDays)
    }

    @Test
    fun equality_sameValues() {
        val request1 = ChequeBookRequest("PENDING", 7)
        val request2 = ChequeBookRequest("PENDING", 7)
        assertEquals(request1, request2)
    }

    @Test
    fun inequality_differentStatus() {
        val request1 = ChequeBookRequest("REQUESTED", 5)
        val request2 = ChequeBookRequest("DELIVERED", 5)
        assertNotEquals(request1, request2)
    }

    @Test
    fun inequality_differentDeliveryDays() {
        val request1 = ChequeBookRequest("REQUESTED", 5)
        val request2 = ChequeBookRequest("REQUESTED", 7)
        assertNotEquals(request1, request2)
    }

    @Test
    fun copy_changesDeliveryDays() {
        val request = ChequeBookRequest("REQUESTED", 5)
        val copied = request.copy(estimatedDeliveryDays = 10)
        assertEquals("REQUESTED", copied.status)
        assertEquals(10, copied.estimatedDeliveryDays)
    }

    @Test
    fun toString_containsValues() {
        val request = ChequeBookRequest("REQUESTED", 5)
        val str = request.toString()
        assertTrue(str.contains("REQUESTED"))
    }
}
