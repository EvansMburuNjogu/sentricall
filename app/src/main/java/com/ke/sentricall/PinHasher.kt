// app/src/main/java/com/ke/sentricall/PinHasher.kt
package com.ke.sentricall

import java.security.MessageDigest

object PinHasher {

    private const val SALT = "sentricall_demo_salt"  // you can randomize per device later

    fun hash(pin: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest((pin + SALT).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}