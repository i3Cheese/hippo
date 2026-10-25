package li.nux.hippo

import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import li.nux.hippo.helpers.addTaxonomiesToHugoConfig
import li.nux.hippo.helpers.clear
import li.nux.hippo.helpers.refine
import li.nux.hippo.helpers.regenerate
import li.nux.hippo.model.HugoSubfolder
import li.nux.hippo.model.WatermarkConfig
import li.nux.hippo.model.WatermarkPlacement

class Hippo : CliktCommand() {
    private val changeStrategy: String by option("-c", "--changes")
        .choice("front_matter", "image_metadata", "both")
        .default("both")
        .help("From which source should changes be accepted?")
    private val demo: Boolean by option("-d", "--demo")
        .flag(default = false)
        .help("If specified demo photos will be used")
    private val format: String by option("-f", "--format")
        .choice("json", "yaml") // , "toml"
        .default("yaml")
        .help("The format to be used for front matter segment")
    private val precedence: String by option("-p", "--precedence")
        .choice("front_matter", "image_metadata")
        .default("front_matter")
        .help("If both the Hugo front matter and the image metadata have \nchanged, which one takes precedence?")
    @Suppress("SpreadOperator")
    private val watermark: String? by option("-w", "--watermark")
        .choice(*WatermarkPlacement.entries.map { it.name }.toTypedArray())
        .help(
            "Specify this to add a watermark to the photos. Allowed variants: \n\n" +
                WatermarkPlacement.getDescriptions()
        )
    private val verbose: Boolean by option("--verbose")
        .flag(default = false)
        .help("Log detailed information")
    private val clear: Boolean by option("--clear")
        .flag(default = false)
        .help("Remove demo files if existing. Also removed the database. Use with \ncaution!")
    private val regenerate: Boolean by option("--regenerate")
        .flag(default = false)
        .help("Remove markdown files if existing. Also removed the database. Use \nwith caution!")
    private val refine: Boolean by option("--refine")
        .flag(default = false)
        .help("Refine front matter in existing markdown files.")
    private val writeWeights: Boolean by option("--write-weights")
        .flag(default = false)
        .help(
            "Write a Hugo `weight` front matter field for every image, sorted by \n" +
                "captureDateTime and numbered 10, 20, 30, ..."
        )
    private val albums: List<String> by option("-a", "--album")
        .multiple()
        .help(
            "Only process the album with this folder name (e.g. `france`). Can be \n" +
                "repeated. Other albums are left untouched. Default: all albums."
        )
    private val directory: String by argument()
        .help("Path to the root directory for your Hugo website project")
        .default("../..")

    override fun run() {
        echo(appHeader())
        val maybeHugoPaths = isHugoSiteDirectory(directory)
        val params = HippoParams(
            changeAcceptance = changeStrategy.toChangeAcceptance(),
            precedence = precedence.toPrecedence(),
            frontMatterFormat = format.toFrontMatterFormat(),
            watermark = watermark?.let { maybeHugoPaths?.let { p: HugoPaths -> WatermarkConfig.from(it, p) } },
            verbose = verbose,
            demo = demo,
            writeWeights = writeWeights,
            albums = albums.toSet(),
        )
        maybeHugoPaths?.let { paths ->
            if (regenerate) {
                regenerate(paths)
            }
            init()
            printIf(params, "HugoPaths: $paths")
            val unknownAlbums = params.albums - existingAlbumNames(paths)
            if (unknownAlbums.isNotEmpty()) {
                println("Unknown album(s): ${unknownAlbums.joinToString()}. No changes done.")
                return
            }
            addTaxonomiesToHugoConfig(paths)
            if (clear) {
                clear(paths)
            } else if (refine) {
                refine(paths, params)
            } else {
                execute(
                    paths,
                    params,
                )
            }
        } ?: {
            println(
                "Parameter was $directory. It should have been the path of the Hugo content folder. No changes done."
            )
        }
    }

    private fun existingAlbumNames(paths: HugoPaths): Set<String> =
        Files.walk(paths.albums, MAX_DIRECTORY_DEPTH).use { stream ->
            stream.filter(Files::isDirectory).map { it.fileName.toString() }.collect(Collectors.toSet())
        }

    private fun isHugoSiteDirectory(directory: String): HugoPaths? {
        val path: Path = Paths.get(directory)
        val subfolders = Files.walk(path, 1)
            .filter(Files::isDirectory)
            .collect(Collectors.toList())
            .map { it.fileName.toString() }
        return if (HugoSubfolder.entries.map { it.folderName }.map(subfolders::contains).none { false }) {
            HugoPaths(
                root = path.toRealPath(LinkOption.NOFOLLOW_LINKS),
                content = Paths.get(path.toString() + File.separator + HugoSubfolder.CONTENT.folderName)
                    .toRealPath(LinkOption.NOFOLLOW_LINKS),
                assets = Paths.get(path.toString() + File.separator + HugoSubfolder.ASSETS.folderName)
                    .toRealPath(LinkOption.NOFOLLOW_LINKS),
                theme = Paths.get("").toAbsolutePath(),
            )
        } else {
            null
        }
    }
}

fun main(args: Array<String>) {
    Hippo().main(args)
}
