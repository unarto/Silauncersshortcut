package com.silauncer.cepat.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

// [app/src/test/java/com/silauncer/cepat/home/MaterialGridItemAnimatorTest.kt]: Unit test untuk spesifikasi Material motion
// [Penjelasan]: Menguji durasi default dan konfigurasi Material motion untuk grid animator
class MaterialGridItemAnimatorTest {

    @Test
    fun testDefaultDurations() {
        val animator = MaterialGridItemAnimator()

        assertEquals(200L, animator.removeDuration)
        assertEquals(280L, animator.addDuration)
        assertEquals(320L, animator.moveDuration)
        assertEquals(240L, animator.changeDuration)
        assertFalse(animator.isRunning)
    }

    @Test
    fun testTransitionConstants() {
        // [app/src/test/java/com/silauncer/cepat/home/MaterialGridItemAnimatorTest.kt]: Verifikasi durasi material
        // [Penjelasan]: Memastikan durasi motion sesuai panduan Material Design 3
        val animator = MaterialGridItemAnimator()
        assertEquals(false, animator.supportsChangeAnimations)
    }
}
