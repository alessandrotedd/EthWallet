package com.github.alessandrotedd.ethwallet.utils

import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

fun encryptString(string: String, key: String): String {
    val secretKey = generateSecretKey(key)
    val cipher = Cipher.getInstance("AES")
    cipher.init(Cipher.ENCRYPT_MODE, secretKey)
    val encrypted = cipher.doFinal(string.toByteArray())
    return Base64.getEncoder().encodeToString(encrypted)
}

fun decryptString(string: String, key: String): String {
    val cipher = Cipher.getInstance("AES")
    val secretKey = generateSecretKey(key)
    cipher.init(Cipher.DECRYPT_MODE, secretKey)
    val decrypted = cipher.doFinal(Base64.getDecoder().decode(string))
    return String(decrypted)
}

fun generateSecretKey(key: String): SecretKey {
    val keyBytes = key.toByteArray()
    val sha = MessageDigest.getInstance("SHA-256")
    val keySpec = sha.digest(keyBytes)
    return SecretKeySpec(keySpec, "AES")
}