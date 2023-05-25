package com.github.alessandrotedd.ethwallet

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.github.alessandrotedd.ethwallet.utils.decryptString
import com.github.alessandrotedd.ethwallet.utils.encryptString
import com.github.alessandrotedd.ethwallet.utils.generatePrivateKey
import com.github.alessandrotedd.ethwallet.utils.hexize
import kotlinx.coroutines.Dispatchers.Main
import java.io.File
import java.security.InvalidParameterException

fun main(args: Array<String>) {
    try {
        handleArgs(args)
    } catch (e: IllegalArgumentException) {
        println(e.message)
        println("Use --help for more information")
    }
}

@Composable
@Preview
fun App() {
    var text by remember { mutableStateOf("Hello, World!") }

    MaterialTheme {
        Button(onClick = {
            text = "Hello, Desktop!"
        }) {
            Text(text)
        }
    }
}

fun composeApp() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
fun handleArgs(args: Array<String>) {
    val params = getArgsMap(args.toList())
    when (val command = args.firstOrNull()) {
        "gui" -> composeApp()
        "generate" -> {
            val prefix = params.getOrDefault("--prefix", null).let {
                val toHexize = params.getOrDefault("--hexize", "false").toBoolean()
                if (toHexize) it?.let { hexize(it) } else it
            }
            if (prefix == null) {
                println("Generating random private key")
            } else {
                if (prefix.isEmpty()) throw IllegalArgumentException("Missing prefix. Usage: generate --prefix <prefix>")
                println("Generating random private key with prefix: $prefix")
            }
            generatePrivateKey(prefix ?: "").also {
                println("Address: 0x${it.first}")
                println("Private key: ${it.second}")
            }
        }
        "generate-encrypted" -> {
            val prefix = params.getOrDefault("--prefix", null).let {
                val toHexize = params.getOrDefault("--hexize", "false").toBoolean()
                if (toHexize) it?.let { hexize(it) } else it
            }
            val key = params.getOrDefault("--key", "")
            if (key.isEmpty()) {
                throw IllegalArgumentException("Missing encryption key. Usage: generate-encrypted --prefix <prefix> --key <encryption-key>")
            }
            if (prefix == null) {
                println("Generating random private key, encrypted with key: $key")
            } else {
                if (prefix.isEmpty()) throw IllegalArgumentException("Missing prefix. Usage: generate-encrypted --prefix <prefix> --key <encryption-key>")
                println("Generating random private key with prefix: $prefix, encrypted with key: $key")
            }
            generatePrivateKey().also {
                println("Address: 0x${it.first}")
                println("Private key: ${encryptString(it.second, key)}")
            }
        }
        "decrypt" -> {
            if (params.isEmpty() || !params.containsKey("--string") || !params.containsKey("--key")) {
                throw IllegalArgumentException("Missing parameters. Usage: decrypt --string <encrypted-string> --key <encryption-key>")
            }
            println("Decrypted string:")
            println(decryptString(params["--string"]!!, params["--key"]!!))
        }
        "encrypt" -> {
            if (params.isEmpty() || !params.containsKey("--string") || !params.containsKey("--key")) {
                throw IllegalArgumentException("Missing parameters. Usage: encrypt --string <string> --key <encryption-key>")
            }
            println("Encrypted string:")
            println(encryptString(params["--string"]!!, params["--key"]!!))
        }
        "--version", "-v" -> {
            println("Version: 1.1.0") // TODO get version from gradle
        }
        "--help", null -> {
            showHelp()
        }
        else -> {
            throw InvalidParameterException(
                """
                Invalid command: "$command"
                Valid commands are: generate, generate-encrypted, decrypt, encrypt, --version, --help
                """.trimIndent()
            )
        }
    }
}

fun getJarFileName(): String {
    val path = Main::class.java.protectionDomain.codeSource.location.path
    val decodedPath = java.net.URI.create(path).path
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

    println("- Show version:")
    println("  java -jar $jarFileName --version or java -jar $jarFileName -v")

    println("- Adding --hexize to any command with --prefix will convert the prefix input to hex (example: 'hello' -> '4e770'):")
    println("  java -jar $jarFileName generate --prefix <prefix> --hexize")

    println("- Show help:")
    println("  java -jar $jarFileName --help")
}

fun getArgsMap(args: List<String>): Map<String, String> {
    val map = mutableMapOf<String, String>()
    val argsWithoutCommand = args.drop(1)
    val possibleSingleArgs = listOf("--version", "-v", "--help", "--hexize")
    val pairedArgs = if (argsWithoutCommand.any { it in possibleSingleArgs }) {
        map.putAll(argsWithoutCommand.filter { it in possibleSingleArgs }.associateWith { "true" })
        argsWithoutCommand.filter { it !in possibleSingleArgs }
    } else {
        argsWithoutCommand
    }
    for (i in pairedArgs.indices step 2) {
        val key = pairedArgs[i]
        val value = if (i + 1 < pairedArgs.size) pairedArgs[i + 1] else ""
        if (key.startsWith("--")) {
            map[key] = value
        }
    }
    return map
}