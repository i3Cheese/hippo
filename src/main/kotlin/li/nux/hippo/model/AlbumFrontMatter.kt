package li.nux.hippo.model

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.Locale
import kotlinx.serialization.Serializable

private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

@Serializable
data class Album(
    val pageType: String = "album",
    val albumId: String,
    val controlCode: String,
    var title: String = "",
    var albumTitle: String = "",
    var description: String = "",
    var coverImage: String? = null,
    val lastUpdated: String = formatter.format(LocalDateTime.now()),
) {
    companion object {
        fun from(path: String, images: List<ImageMetadata>): Album {
            return Album(
                albumId = images.first().getAlbumId(),
                controlCode = listOf(
                    path,
                    images.first().album,
                ).joinToString(",").let { Base64.getEncoder().encodeToString(it.encodeToByteArray()) },
                title = capitalize(images.first().album),
                albumTitle = capitalize(images.first().album),
                description = "Insert album description here",
                coverImage = images.first().getReference(),
                lastUpdated = formatter.format(
                    images.map { it.created }.sortedByDescending { it }.first() ?: LocalDateTime.now()
                )
            )
        }

        fun rootFolder(contentDirectory: String): Album {
            return Album(
                pageType = "albumIndex",
                albumId = "ROOT",
                controlCode = listOf(
                    contentDirectory,
                    "/",
                ).joinToString(",").let { Base64.getEncoder().encodeToString(it.encodeToByteArray()) },
                title = "Albums",
                albumTitle = "Albums",
                description = "Insert description here",
                coverImage = null,
            )
        }

        private fun capitalize(name: String) =
            name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                .replace('_', ' ')
    }
}

@Serializable
data class GalleryImage(
    val imageId: String,
    val thumbnail: String,
    val smallImage: String,
    val featuredImage: String,
    val largeImage: String,
    val originalSize: String,
) {
    companion object {
        private const val IMAGE_ROOT = "images/"

        fun from(path: String, imageId: String): GalleryImage {
            val modifiedPath = path.split("content/albums/").last()
            val imagePath = IMAGE_ROOT + modifiedPath + File.separator
            return GalleryImage(
                imageId = imageId,
                thumbnail = imagePath + ConvertedImage.THUMBNAIL.getFilename(imageId),
                smallImage = imagePath + ConvertedImage.SMALL.getFilename(imageId),
                featuredImage = imagePath + ConvertedImage.MEDIUM.getFilename(imageId),
                largeImage = imagePath + ConvertedImage.LARGE.getFilename(imageId),
                originalSize = imagePath + ConvertedImage.ORIGINAL.getFilename(imageId),
            )
        }
    }
}
