package li.nux.hippo.helpers

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

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
