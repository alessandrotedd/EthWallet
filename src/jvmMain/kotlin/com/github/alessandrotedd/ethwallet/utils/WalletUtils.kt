package com.github.alessandrotedd.ethwallet.utils

import org.web3j.crypto.Keys
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.math.pow

fun generatePrivateKey(addressPrefix: String = ""): Pair<String, String> {
    val possibleChoices = 16.0.pow(addressPrefix.length).toInt()

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