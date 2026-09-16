package li.nux.hippo.helpers

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import li.nux.hippo.HugoPaths
import li.nux.hippo.model.ImageFrontMatter
import li.nux.hippo.model.KeywordItem

// Front matter is hand-edited by users (e.g. to add custom fields under `extra`), so all three
// formats are configured to ignore unrecognized keys instead of throwing on the next run.
val lenientYaml = Yaml(configuration = YamlConfiguration(strictMode = false))
val lenientToml = Toml(inputConfig = TomlInputConfig(ignoreUnknownNames = true))

@OptIn(ExperimentalSerializationApi::class)
val prettyJson = Json { // this returns the JsonBuilder
    prettyPrint = true
    encodeDefaults = false
    prettyPrintIndent = "    "
    explicitNulls = false
    ignoreUnknownKeys = true
}

private const val NUMBER_OF_BUCKETS = 9

fun createDatafileWithAllImages(
    hugoPaths: HugoPaths,
    imfMap: MutableMap<String, ImageFrontMatter>
) {
    val dataDirectory = hugoPaths.root.toAbsolutePath().toString() + File.separator + "data" + File.separator + "foto"
    Files.createDirectories(Paths.get(dataDirectory).toAbsolutePath())
    val datafile = Paths.get(
        hugoPaths.root.toAbsolutePath().toString() +
            File.separator + "data" + File.separator + "foto" + File.separator + "images.json"
    )
    val content = prettyJson.encodeToString(imfMap)
    Files.write(datafile, content.toByteArray())
}

fun createDatafileWithAllKeywords(
    hugoPaths: HugoPaths,
    imfMap: MutableMap<String, ImageFrontMatter>
) {
    val datafile = Paths.get(
        hugoPaths.root.toAbsolutePath().toString() +
            File.separator + "data" + File.separator + "foto" + File.separator + "keywords.json"
    )
    val keywordItems = imfMap.values.map { it.keywords }
        .flatten()
        .groupBy { it }
        .map { KeywordItem(it.key.replace("\"", ""), it.value.size) }

    val keywords = assignBuckets(keywordItems)
    val content = prettyJson.encodeToString(keywords)
    Files.write(datafile, content.toByteArray())
}

fun assignBuckets(keywords: List<KeywordItem>): List<KeywordItem> {
    val min = keywords.minOfOrNull { it.count }
    val max = keywords.maxOfOrNull { it.count }

    return if (min == null || max == null) {
        emptyList()
    } else if (min == max) {
        keywords.map { it.copy(weight = 1) }
    } else {
        keywords.map { keyword ->
            val relativePos = (keyword.count - min).toDouble() / (max - min)
            val bucket = (relativePos * (NUMBER_OF_BUCKETS - 1)).toInt() + 1
            keyword.copy(weight = bucket)
        }
    }
}
