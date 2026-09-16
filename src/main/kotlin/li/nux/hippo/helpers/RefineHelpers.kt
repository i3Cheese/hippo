package li.nux.hippo.helpers

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import li.nux.hippo.FrontMatterFormat.JSON
import li.nux.hippo.FrontMatterFormat.TOML
import li.nux.hippo.FrontMatterFormat.YAML
import li.nux.hippo.HippoParams
import li.nux.hippo.HugoPaths
import li.nux.hippo.model.ImageFrontMatter
import li.nux.hippo.model.ImageFrontMatter.Companion.MARK_DOWN_FILE_EXTENSION
import li.nux.hippo.model.ImageMetadata.Companion.IMG_NAME_PREFIX

fun refine(
    hugoPaths: HugoPaths,
    params: HippoParams,
) {
    Files.walk(hugoPaths.content)
        .filter { path ->
            path.isRegularFile() &&
                path.fileName.toString().endsWith(MARK_DOWN_FILE_EXTENSION) &&
                path.fileName.toString().startsWith(IMG_NAME_PREFIX)
        }
        .forEach { path ->
            val imageFrontMatter: ImageFrontMatter = getImageDataFromFrontMatter(path)
            imageFrontMatter.extra["stock-url"]?.let { stockUrl ->
                StockImageServices.fromUrl(stockUrl)?.let {
                    val imf = imageFrontMatter.copy(stockImageSite = listOf(it.name.lowercase().replace("_", "-")))
                    writeFrontMatterToFile(params, imf, path)
                }
            }
        }
}

private fun writeFrontMatterToFile(params: HippoParams, imf: ImageFrontMatter, path: Path) {
    val frontMatter = when (params.frontMatterFormat) {
        JSON -> prettyJson.encodeToString(imf)
        TOML -> tomlWrap(lenientToml.encodeToString(ImageFrontMatter.serializer(), imf))
        YAML -> yamlWrap(lenientYaml.encodeToString(ImageFrontMatter.serializer(), imf))
    }
    Files.write(path, frontMatter.toByteArray())
}
