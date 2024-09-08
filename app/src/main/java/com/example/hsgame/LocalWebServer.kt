import android.content.Context
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.newFixedLengthResponse

class LocalWebServer(private val context: Context) : NanoHTTPD(8080) {

    override fun serve(session: IHTTPSession?): Response {
        val uri = session?.uri ?: return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found")

        // Remove leading slash if present
        val filePath = uri.trimStart('/')

        return try {
            // Open the file from the assets folder, including nested directories
            val inputStream = context.assets.open(filePath)
            val mimeType = getMimeType(filePath)
            newFixedLengthResponse(Response.Status.OK, mimeType, inputStream, inputStream.available().toLong())
        } catch (e: Exception) {
            newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "File Not Found: $filePath")
        }
    }

    // Determine MIME type based on file extension
    private fun getMimeType(path: String): String {
        return when {
            path.endsWith(".html") -> "text/html"
            path.endsWith(".js") -> "application/javascript"
            path.endsWith(".css") -> "text/css"
            path.endsWith(".json") -> "application/json"
            path.endsWith(".png") -> "image/png"
            path.endsWith(".jpg") || path.endsWith(".jpeg") -> "image/jpeg"
            path.endsWith(".webp") -> "image/webp"
            path.endsWith(".webm") -> "video/webm"  // For video/audio webm files
            path.endsWith(".mp3") -> "audio/mpeg"
            path.endsWith(".wav") -> "audio/wav"
            path.endsWith(".ogg") -> "audio/ogg"
            else -> "application/octet-stream" // Generic binary stream for unknown files
        }
    }
}