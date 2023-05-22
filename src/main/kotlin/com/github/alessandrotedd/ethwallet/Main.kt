package com.github.alessandrotedd.ethwallet

import kotlinx.coroutines.Dispatchers.Main
import org.web3j.crypto.Keys
import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import kotlin.concurrent.thread
import kotlin.math.pow

fun main(args: Array<String>) {
    var prefix = ""
    var key: String? = null
    var encryptionKey: String? = null
    var decryptionKey: String? = null

    args.forEachIndexed { index, arg ->
        when (arg) {
            "--prefix", "-p" -> args.getOrNull(index + 1)?.let { prefix = it } ?: run { showHelp(); return@main }
            "--key", "-k" -> args.getOrNull(index + 1)?.let { key = it } ?: run { showHelp(); return@main }
            "--encrypt", "-e" -> args.getOrNull(index + 1)?.let { encryptionKey = it } ?: run { showHelp(); return@main }
            "--decrypt", "-d" -> args.getOrNull(index + 1)?.let { decryptionKey = it } ?: run { showHelp(); return@main }
            "--help", "-h" -> { showHelp(); return@main }
        }
    }

    encryptionKey?.let { k ->
        println("Enter the string to encrypt:")
        val input = readlnOrNull()
        input?.let {
            println("Encrypted string: ${encryptString(it, k)}")
        } ?: run {
            println("Invalid input")
        }
        return@main
    }

    decryptionKey?.let { k ->
        println("Enter the string to decrypt:")
        val input = readlnOrNull()
        input?.let {
            println("Decrypted string: ${decryptString(it, k)}")
        } ?: run {
            println("Invalid input")
        }
        return@main
    }

    println(when {
        prefix.isEmpty() && key == null -> "Generating random private key"
        prefix.isEmpty() && key != null -> "Generating random private key and encrypting it with key \"$key\""
        prefix.isNotEmpty() && key == null -> "Generating private key for addresses starting with: $prefix"
        prefix.isNotEmpty() && key != null -> "Generating private key for addresses starting with: $prefix and encrypting it with key \"$key\""
        else -> ""
    })

    generatePrivateKey(hexize(prefix)).let { wallet ->
        val address = wallet.first
        val privateKey = wallet.second
        println("Address: 0x$address")
        key?.let {
            encryptString(privateKey, it)
        } ?: run {
            privateKey
        }
    }.also { privateKey ->
        println("Private key ${
            if (key != null) "encrypted with key \"$key\""
            else "not encrypted"
        }: $privateKey")
    }
}

fun getJarFileName(): String {
    val path = Main::class.java.protectionDomain.codeSource.location.path
    val decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8)
    val file = File(decodedPath)
    return file.name
}

fun showHelp() {
    val jarFileName = getJarFileName()

    println("Usage: java -jar $jarFileName <command> [options]")
    println("Available commands:")
    println("- Generate a random address not encrypted:")
    println("  java -jar $jarFileName generate")

    println("- Generate a random address encrypted using a key:")
    println("  java -jar $jarFileName generate-encrypted --key <encryption-key>")

    println("- Generate a random address with a prefix not encrypted:")
    println("  java -jar $jarFileName generate --prefix <prefix>")

    println("- Generate a random address with a prefix encrypted using a key:")
    println("  java -jar $jarFileName generate-encrypted --prefix <prefix> --key <encryption-key>")

    println("- Decrypt a string using a key:")
    println("  java -jar $jarFileName decrypt --string <encrypted-string> --key <encryption-key>")

    println("- Encrypt a string using a key:")
    println("  java -jar $jarFileName encrypt --string <string> --key <encryption-key>")

    println("- Show help:")
    println("  java -jar $jarFileName --help or java -jar $jarFileName -h")
}

fun generatePrivateKey(addressPrefix: String = ""): Pair<String, String> {
    val possibleChoices = 16.0.pow(addressPrefix.length)

    val queue = ConcurrentLinkedQueue<Pair<String, String>>()
    val numThreads = Runtime.getRuntime().availableProcessors()
    val mutex = ReentrantLock()

    var startTime = System.currentTimeMillis()
    val addressesGenerated = AtomicInteger(0)

    val threadPool = Array(numThreads) {
        thread {
            while (true) {
                val ecKeyPair = Keys.createEcKeyPair()
                val privateKey = ecKeyPair.privateKey.toString(16).padStart(64, '0')
                val address = Keys.getAddress(ecKeyPair.publicKey)

                addressesGenerated.incrementAndGet()
                if (System.currentTimeMillis() - startTime > 1000) {
                    mutex.lock()
                    if (System.currentTimeMillis() - startTime > 1000) {
                        val elapsedTime = System.currentTimeMillis() - startTime
                        val addressesPerSecond = addressesGenerated.get() * 1000 / elapsedTime
                        println("Addresses per second: $addressesPerSecond, total time estimate: ${possibleChoices / addressesPerSecond.toInt()} seconds, possible choices: $possibleChoices")
                        addressesGenerated.set(0)
                        startTime = System.currentTimeMillis()
                    }
                    mutex.unlock()
                }

                if (address.startsWith(addressPrefix)) {
                    val result = Pair(address, privateKey)
                    queue.add(result)
                    return@thread
                }
            }
        }
    }

    threadPool.forEach { it.join() }
    return queue.poll()!!
}

fun hexize(input: String): String {
    val substitutionMap = mapOf(
        'A' to 4,
        'B' to 8,
        'D' to 0,
        'E' to 3,
        'a' to 'a',
        'b' to 'b',
        'c' to 'c',
        'd' to 'd',
        'e' to 'e',
        'f' to 'f',
        'g' to '9',
        'h' to '4',
        'i' to '1',
        'j' to '7',
        'l' to '7',
        'o' to '0',
        'q' to '9',
        'r' to '2',
        's' to '5',
        't' to '7',
        'z' to '2'
    )

    val unreplaceableChars = setOf('k', 'm', 'n', 'p', 'u', 'v', 'w', 'x', 'y')
    val containsIrreplaceableChar = unreplaceableChars.any { input.contains(it) }
    if (containsIrreplaceableChar) {
        throw IllegalArgumentException("Input string contains an irreplaceable character: ${unreplaceableChars.find { input.contains(it) }}")
    }

    return input.map { substitutionMap[it] ?: it }.joinToString("")
}

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
