package li.nux.hippo.helpers

enum class StockImageServices(
    val urlPart: String,
) {
    ADOBE("stock.adobe.com/"),
    ALAMY("alamy.com"),
    DEPOSITPHOTOS("depositphotos.com"),
    DREAMSTIME("dreamstime.com"),
    GETTY_IMAGES("gettyimages"),
    ISTOCK("istockphoto.com"),
    MOSTPHOTOS("mostphotos.com"),
    PICFAIR("picfair.com"),
    POND5("pond5.com"),
    SHUTTERSTOCK("shutterstock.com"),
    STOCKSY("stocksy.com");

    companion object {
        fun fromUrl(url: String): StockImageServices? {
            return entries.find { url.contains(it.urlPart) }
        }
    }
}