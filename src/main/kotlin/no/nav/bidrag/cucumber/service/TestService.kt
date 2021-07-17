package no.nav.bidrag.cucumber.service

import io.cucumber.core.cli.Main
import no.nav.bidrag.cucumber.ABSOLUTE_CLOUD_PATH
import no.nav.bidrag.cucumber.Environment
import no.nav.bidrag.cucumber.model.CucumberTests
import no.nav.bidrag.cucumber.model.SuppressStackTraceText
import no.nav.bidrag.cucumber.model.TestFailedException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.Charset

@Service
class TestService(private val suppressStackTraceText: SuppressStackTraceText) {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(TestService::class.java)
    }

    internal fun run(cucumberTests: CucumberTests) : String {
        cucumberTests.initTestEnvironment()

        val tags = cucumberTests.fetchTags()
        val sysOut = ByteArrayOutputStream()

        System.setOut(PrintStream(sysOut))
        val result = runCucumberTests(tags)

        Environment.resetTestEnvironment()
        val suppressedStackText = suppressStackTraceText.suppress(sysOut.toString(Charset.defaultCharset()))

        if (result != 0.toByte()) {
            val message = "Kjøring av cucumber var mislykket (tags: $tags)!"
            LOGGER.error(message)
            throw TestFailedException(message, suppressedStackText)
        }

        return suppressedStackText
    }

    private fun runCucumberTests(tags: String): Byte {
        if (tags.isBlank()) throw IllegalStateException("Ingen tags som kan brukes")

        return Main.run(
            ABSOLUTE_CLOUD_PATH, "--glue", "no.nav.bidrag.cucumber.cloud", "--tags", tags
        )
    }
}
