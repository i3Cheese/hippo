package li.nux.hippo

import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException
import java.text.SimpleDateFormat
import java.util.Date
import li.nux.hippo.helpers.prettyJson
import li.nux.hippo.model.ImageMetadata
import li.nux.hippo.model.ImageMetadata.Companion.fromResultSet
import mu.KotlinLogging

private const val DATABASE_URL = "jdbc:sqlite:hippo.db"
val log = KotlinLogging.logger {}

fun connect(): Connection {
    return try {
        DriverManager.getConnection(DATABASE_URL)
    } catch (e: SQLException) {
        log.error("Could not get connection to db: {}", e.message)
        throw e
    }
}

class StorageService {
    fun fetchAllImages(): List<ImageMetadata> {
        val images = mutableListOf<ImageMetadata>()
        val connection = connect()
        try {
            connection.createStatement().use { statement ->
                statement.executeQuery(FETCH_ALL).use { resultSet ->
                    getImagesFromResultSet(resultSet, images)
                }
            }
        } catch (e: SQLException) {
            log.error("Failed to fetch images: {}", e.message)
            throw StorageException("Failed to fetch images")
        }
        return images
    }

    private fun getImagesFromResultSet(resultSet: ResultSet, images: MutableList<ImageMetadata>) {
        while (resultSet.next()) {
            images.add(
                fromResultSet(resultSet)
            )
        }
    }

    fun updatePostedImage(id: Int, img: ImageMetadata) {
        val connection = connect()
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
        try {
            connection.prepareStatement(UPDATE_POSTED_IMAGE).use { prepped ->
                prepped.setString(1, img.path)
                prepped.setString(2, img.album)
                prepped.setString(3, img.filename)
                prepped.setInt(4, img.hashCode())
                prepped.setString(5, img.title)
                prepped.setString(6, img.description)
                prepped.setString(7, img.credit)
                prepped.setString(8, img.captureDate)
                prepped.setString(9, img.captureTime)
                prepped.setString(10, java.lang.String.join(", ", img.keywords.distinct()))
                prepped.setString(11, img.exposureDetails?.focalLength)
                prepped.setString(12, img.exposureDetails?.aperture)
                prepped.setString(13, img.exposureDetails?.exposureTime)
                prepped.setString(14, img.exposureDetails?.iso)
                prepped.setString(15, img.exposureDetails?.cameraMake)
                prepped.setString(16, img.exposureDetails?.cameraModel)
                prepped.setString(17, img.stockImageSite.firstOrNull())
                prepped.setString(18, img.extra.let { prettyJson.encodeToString(it) })
                prepped.setString(19, img.contentHash)
                prepped.setString(20, img.creator)
                prepped.setString(21, now)
                prepped.setInt(22, id)
                prepped.executeUpdate()
            }
        } catch (e: SQLException) {
            log.error("Failed to update posted image with id ${img.id}: {}", e.message)
            throw StorageException("Failed to update posted image with id ${img.id}")
        } finally {
            log.trace("updated posted image with id ${img.id}")
        }
    }

    fun insertPostedImage(img: ImageMetadata): Int {
        val connection = connect()
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
        try {
            connection.prepareStatement(INSERT_POSTED_IMAGE).use { prepped ->
                prepped.setString(1, img.getReference())
                prepped.setString(2, img.path)
                prepped.setString(3, img.album)
                prepped.setString(4, img.filename)
                prepped.setInt(5, img.hashCode())
                prepped.setString(6, img.title)
                prepped.setString(7, img.description)
                prepped.setString(8, img.credit)
                prepped.setString(9, img.captureDate)
                prepped.setString(10, img.captureTime)
                prepped.setString(11, java.lang.String.join(", ", img.keywords.distinct()))
                prepped.setString(12, img.exposureDetails?.focalLength)
                prepped.setString(13, img.exposureDetails?.aperture)
                prepped.setString(14, img.exposureDetails?.exposureTime)
                prepped.setString(15, img.exposureDetails?.iso)
                prepped.setString(16, img.exposureDetails?.cameraMake)
                prepped.setString(17, img.exposureDetails?.cameraModel)
                prepped.setString(18, img.stockImageSite.firstOrNull() )
                prepped.setString(19, img.extra.let { prettyJson.encodeToString(it) } )
                prepped.setString(20, img.contentHash)
                prepped.setString(21, img.creator)
                prepped.setString(22, now)
                prepped.setString(23, now)
                prepped.executeUpdate()
                return prepped.generatedKeys.getInt(1)
            }
        } catch (e: SQLException) {
            System.err.println(e.message)
            return -1
        } finally {
            log.trace(" inserted new posted image with reference ${img.getReference()}")
        }
    }

    fun updateContentHash(id: Int, contentHash: String?) {
        val connection = connect()
        try {
            connection.prepareStatement(UPDATE_CONTENT_HASH).use { prepped ->
                prepped.setString(1, contentHash)
                prepped.setInt(2, id)
                prepped.executeUpdate()
            }
        } catch (e: SQLException) {
            log.error("Failed to update content hash for id $id: {}", e.message)
            throw StorageException("Failed to update content hash for id $id")
        }
    }

    fun removePostedImage(id: Int, imageId: String) {
        val connection = connect()
        try {
            connection.prepareStatement(DELETE_POSTED_IMAGE).use { prepped ->
                prepped.setInt(1, id)
                prepped.setString(2, imageId)
                prepped.executeUpdate()
            }
        } catch (e: SQLException) {
            log.error("Failed to delete posted image with id $id: {}", e.message)
            throw StorageException("Failed to delete posted image with id $id")
        }
    }

    companion object {
        private const val CREATE_TABLE_POSTED_IMAGE = """
            CREATE TABLE IF NOT EXISTS posted_image (
                id INTEGER PRIMARY KEY, 
                image_id TEXT NOT NULL, 
                path TEXT NOT NULL, 
                album_name TEXT NOT NULL, 
                photo_filename TEXT NOT NULL, 
                hash_code INTEGER NOT NULL, 
                title TEXT, 
                description TEXT, 
                credit TEXT, 
                capture_date TEXT, 
                capture_time TEXT, 
                keywords TEXT, 
                focal_length TEXT,
                f_number TEXT,
                exposure_time TEXT,
                iso TEXT,
                camera_make TEXT,
                camera_model TEXT,
                stock_image_site TEXT NULL,
                extra_fields TEXT NOT NULL,
                content_hash TEXT,
                creator TEXT,
                created TEXT NOT NULL,
                updated TEXT NOT NULL
            );"""

        private const val ADD_CONTENT_HASH_COLUMN =
            "ALTER TABLE posted_image ADD COLUMN content_hash TEXT"

        private const val ADD_CREATOR_COLUMN =
            "ALTER TABLE posted_image ADD COLUMN creator TEXT"

        private const val UPDATE_POSTED_IMAGE = """
            UPDATE posted_image SET
                path = ?,
                album_name = ?,
                photo_filename = ?,
                hash_code = ?,
                title = ?,
                description = ?,
                credit = ?,
                capture_date = ?,
                capture_time = ?,
                keywords = ?,
                focal_length = ?,
                f_number = ?,
                exposure_time = ?,
                iso = ?,
                camera_make = ?,
                camera_model = ?,
                stock_image_site = ?,
                extra_fields = ?,
                content_hash = ?,
                creator = ?,
                updated = ?
            WHERE id = ?
        """
        private const val UPDATE_CONTENT_HASH = "UPDATE posted_image SET content_hash = ? WHERE id = ?"
        private const val FETCH_ALL = "SELECT * FROM posted_image ORDER BY image_id"
        private const val DELETE_POSTED_IMAGE = "DELETE FROM posted_image where id = ? and image_id = ?"
        private const val INSERT_POSTED_IMAGE = """
            INSERT INTO posted_image (
                image_id, 
                path, 
                album_name, 
                photo_filename, 
                hash_code, 
                title, 
                description, 
                credit, 
                capture_date, 
                capture_time, 
                keywords, 
                focal_length, 
                f_number, 
                exposure_time, 
                iso, 
                camera_make, 
                camera_model,
                stock_image_site,
                extra_fields,
                content_hash,
                creator,
                created,
                updated
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
            """

        fun createTable() {
            val connection = connect()
            try {
                connection.use { conn ->
                    conn.createStatement().use { stmt ->
                        stmt.execute(CREATE_TABLE_POSTED_IMAGE)
                    }
                    // Migrations for databases created before content_hash/creator existed.
                    listOf(ADD_CONTENT_HASH_COLUMN, ADD_CREATOR_COLUMN).forEach { migration ->
                        conn.createStatement().use { stmt ->
                            try {
                                stmt.execute(migration)
                            } catch (e: SQLException) {
                                if (e.message?.contains("duplicate column name") != true) throw e
                            }
                        }
                    }
                }
            } catch (e: SQLException) {
                println(e.message)
            }
        }
    }
}

class StorageException(message: String): RuntimeException(message)
