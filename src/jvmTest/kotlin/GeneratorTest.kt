import com.github.alessandrotedd.ethwallet.*
import com.github.alessandrotedd.ethwallet.utils.decryptString
import com.github.alessandrotedd.ethwallet.utils.encryptString
import com.github.alessandrotedd.ethwallet.utils.generatePrivateKey
import com.github.alessandrotedd.ethwallet.utils.hexize
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class GeneratorTest {
    @Test
    fun testArgsMapper() {
        val args = listOf(
            "generate",
            "generate --prefix a",
            "generate --prefix hello --hexize",
            "generate-encrypted --key a",
            "generate-encrypted --prefix a --key b",
            "encrypt --string a --key b",
            "decrypt --string a --key b",
            "--version",
            "-v",
            "generate-encrypted --key a --prefix b",
            "encrypt --key b --string a",
            "decrypt --key b --string a"
        )
        val expected = listOf(
            emptyMap(),
            mapOf("--prefix" to "a"),
            mapOf("--prefix" to "hello", "--hexize" to "true"),
            mapOf("--key" to "a"),
            mapOf("--prefix" to "a", "--key" to "b"),
            mapOf("--string" to "a", "--key" to "b"),
            mapOf("--string" to "a", "--key" to "b"),
            emptyMap(),
            emptyMap(),
            mapOf("--key" to "a", "--prefix" to "b"),
            mapOf("--key" to "b", "--string" to "a"),
            mapOf("--key" to "b", "--string" to "a")
        )
        args.forEachIndexed { index, arg ->
            println("Testing arg: $arg")
            assertEquals(expected[index], getArgsMap(arg.split(" ")))
        }
    }

    @Test
    fun testValidArgs() {
        val encryptedExample = "WYPWYKLXAaV2+08MQss01g=="
        val validArgs = listOf(
            "generate",
            "generate --prefix a",
            "generate --prefix hi --hexize",
            "generate-encrypted --key a",
            "generate-encrypted --prefix a --key b",
            "encrypt --string a --key b",
            "decrypt --string $encryptedExample --key b",
            "--version",
            "-v",
            "--help",
            "generate-encrypted --key a --prefix b",
            "encrypt --key b --string a",
            "decrypt --key b --string $encryptedExample"
        )
        validArgs.forEach { args ->
            println("Testing args: $args")
            assertDoesNotThrow { handleArgs(args.split(" ").toTypedArray()) }
        }
    }

    @Test
    fun testInvalidArgs() {
        val invalidArgs = listOf(
            "generate --prefix",
            "generate-encrypted --key",
            "decrypt --string",
            "encrypt --string",
            "generate-encrypted --prefix a",
            "decrypt --string a",
            "encrypt --string a",
            ""
        )
        invalidArgs.forEach { args ->
            println("Testing args: $args")
            assertThrows<IllegalArgumentException> { handleArgs(args.split(" ").toTypedArray()) }
        }
    }

    @Test
    fun testGeneratePrivateKey() {
        generatePrivateKey("a").let {
            println("Testing prefix: a")
            assertTrue(it.first.startsWith("a"))
            assertTrue(it.second.length == 64)
        }
    }

    @Test
    fun testEncryptDecrypt() {
        val key = "a"
        val privateKey = "b"
        val encrypted = encryptString(privateKey, key)
        assertEquals(privateKey, decryptString(encrypted, key))
        assertNotEquals(privateKey, encrypted)
    }

    @Test
    fun testHexize() {
        hexize("a").also {
            assertEquals("a", it)
        }
        hexize("hello").also {
            assertEquals("4e770", it)
        }
    }
}
