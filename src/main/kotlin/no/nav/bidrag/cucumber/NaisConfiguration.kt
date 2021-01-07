package no.nav.bidrag.cucumber

import com.google.gson.Gson
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

private val LOGGER = LoggerFactory.getLogger(NaisConfiguration::class.java)
private val JSON_FILE_PER_APPLICATION: MutableMap<String, String> = HashMap()

internal class NaisConfiguration {

    fun read(applicationName: String): SecurityToken {
        val applfolder = File("${Environment.naisProjectFolder}/$applicationName")
        val naisFolder = File("${Environment.naisProjectFolder}/$applicationName/nais")
        val jsonFile = fetchJsonByEnvironmentOrNamespace(applicationName)

        LOGGER.info("> applFolder exists: ${applfolder.exists()}, path: $applfolder")
        LOGGER.info("> naisFolder exists: ${naisFolder.exists()}, path: $naisFolder")
        LOGGER.info("> jsonFile   exists: ${jsonFile.exists()}, path: $jsonFile")

        val canReadNaisJson = applfolder.exists() && naisFolder.exists() && jsonFile.exists()

        if (canReadNaisJson) {
            JSON_FILE_PER_APPLICATION[applicationName] = jsonFile.absolutePath
        } else {
            throw IllegalStateException("Unable to read json configuration for $applicationName")
        }

        return hentAzureSomSecurityToken(jsonFile.parent) ?: SecurityToken.NONE
    }

    private fun hentAzureSomSecurityToken(naisFolder: String): SecurityToken? {
        val naisYamlReader = File(naisFolder, "nais.yaml").bufferedReader()
        val pureYaml = mutableListOf<String>()
        naisYamlReader.useLines { lines -> lines.forEach { if (!it.contains("{{")) pureYaml.add(it) } }
        val yamlMap = Yaml().load<Map<String, Any>>(pureYaml.joinToString("\n"))

        return if (isEnabled(yamlMap, mutableListOf("spec", "azure", "application", "enabled"))) SecurityToken.AZURE else null
    }

    private fun isEnabled(map: Map<String, Any>, keys: MutableList<String>): Boolean {
        println("${keys[0]}=${map[keys[0]]}")

        if (map.containsKey(keys[0])) {
            if (keys.size == 1) return map.getValue(keys[0]) as Boolean
            else {
                @Suppress("UNCHECKED_CAST")
                val childMap = map[keys[0]] as Map<String, Any>
                keys.removeAt(0)
                return isEnabled(childMap, keys)
            }
        }

        return false
    }

    private fun fetchJsonByEnvironmentOrNamespace(applicationName: String): File {
        val miljoJson = File("${Environment.naisProjectFolder}/$applicationName/nais/${Environment.miljo}.json")

        if (miljoJson.exists()) {
            return miljoJson
        } else {
            LOGGER.warn("Unable to find ${Environment.miljo}.json, using ${Environment.namespace}.json")
        }

        return File("${Environment.naisProjectFolder}/$applicationName/nais/${Environment.namespace}.json")
    }

    internal fun hentApplicationHostUrl(naisApplication: String): String {
        val nameSpaceJsonFile = JSON_FILE_PER_APPLICATION[naisApplication]
            ?: throw IllegalStateException("no path for $naisApplication in $JSON_FILE_PER_APPLICATION")

        val jsonFileAsMap = readWithGson(nameSpaceJsonFile)

        for (json in jsonFileAsMap) {
            println(json)
        }

        @Suppress("UNCHECKED_CAST") val ingresses = jsonFileAsMap["ingresses"] as List<String>
        return fetchIngress(ingresses).replace("//", "/").replace("https:/", "https://")
    }

    private fun fetchIngress(ingresses: List<String?>): String {
        for (ingress in ingresses) {
            if (ingress?.contains(Regex("dev.adeo"))!!) {
                return ingress
            }
        }

        throw IllegalStateException("Kunne ikke fastslå ingress til tjeneste!")
    }

    private fun readWithGson(jsonPath: String): Map<String, Any> {
        val bufferedReader = BufferedReader(FileReader(jsonPath))
        val gson = Gson()

        @Suppress("UNCHECKED_CAST")
        return gson.fromJson(bufferedReader, Map::class.java) as Map<String, Any>
    }
}
