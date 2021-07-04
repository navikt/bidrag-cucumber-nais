package no.nav.bidrag.cucumber.sikkerhet

import no.nav.bidrag.cucumber.Environment
import org.slf4j.LoggerFactory
import org.springframework.web.client.RestTemplate

internal object Sikkerhet {

    private val AZURE_AD_URL = "${Environment.AZURE_LOGIN_ENDPOINT}/${Environment.tenant}/oauth2/v2.0/token"
    private val LOGGER = LoggerFactory.getLogger(Sikkerhet::class.java)
    private val tokenProvider = TokenProvider(RestTemplate())

    internal fun fetchAzureBearerToken(): String {
        try {
            return "Bearer ${tokenProvider.fetchAzureToken(AZURE_AD_URL)}"
        } catch (e: RuntimeException) {
            val exception = "${e.javaClass.name}: ${e.message} - ${e.stackTrace.first { it.fileName != null && it.fileName!!.endsWith("kt") }}"
            LOGGER.error("Feil ved henting av online id token, $exception")
            throw e
            }
    }
}
