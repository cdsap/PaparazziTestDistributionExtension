package io.github.cdsap.td.paparazzi

import app.cash.paparazzi.Snapshot
import app.cash.paparazzi.SnapshotHandler
import app.cash.paparazzi.SnapshotHandler.FrameHandler

import com.google.common.base.CharMatcher
import okio.BufferedSink
import okio.HashingSink
import okio.blackholeSink
import okio.buffer
import okio.sink
import okio.source
import org.jcodec.api.awt.AWTSequenceEncoder
import java.awt.image.BufferedImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.imageio.ImageIO

/**
 * Paparazzi HtmlReportWriter custom implementation to support individual html reports.
 * The individual reports is used by Gradle Enterprise Test Distribution feature.
 * Because we are executing the tests in different remote agents we need to merge the results
 * once the tests are finished. To avoid the conflicting fragments errors we are generating
 * individual reports.
 *
 *
 *

 */
class TDHtmlReportWriter @JvmOverloads constructor(
    private val runName: String = defaultRunName(),
    private val rootDirectory: File = File(
        "${
            System.getProperty(
                "paparazzi.build.dir",
                "build"
            )
        }/reports/paparazzi/td-${System.currentTimeMillis()}"
    ),
    snapshotRootDirectory: File = File("src/test/snapshots")
) : SnapshotHandler {
    private val runsDirectory: File = File(rootDirectory, "runs")
    private val imagesDirectory: File = File(rootDirectory, "images")
    private val videosDirectory: File = File(rootDirectory, "videos")

    private val goldenImagesDirectory = File(snapshotRootDirectory, "images")
    private val goldenVideosDirectory = File(snapshotRootDirectory, "videos")

    private val shots = mutableListOf<Snapshot>()

    private val isRecording: Boolean =
        System.getProperty("paparazzi.test.record")?.toBoolean() == true

    init {
        runsDirectory.mkdirs()
        imagesDirectory.mkdirs()
        videosDirectory.mkdirs()
        writeStaticFiles()
        writeRunJs()
        writeIndexJs()
    }

    override fun newFrameHandler(
        snapshot: Snapshot,
        frameCount: Int,
        fps: Int
    ): FrameHandler {
        return object : FrameHandler {
            val hashes = mutableListOf<String>()

            override fun handle(image: BufferedImage) {
                hashes += writeImage(image)
            }

            override fun close() {
                if (hashes.isEmpty()) return

                val shot = if (hashes.size == 1) {
                    val original = File(imagesDirectory, "${hashes[0]}.png")
                    if (isRecording) {
                        val goldenFile = File(goldenImagesDirectory, snapshot.toFileName("_", "png"))
                        original.copyTo(goldenFile, overwrite = true)
                    }
                    snapshot.copy(file = original.toJsonPath())
                } else {
                    val hash = writeVideo(hashes, fps)

                    if (isRecording) {
                        for ((index, frameHash) in hashes.withIndex()) {
                            val originalFrame = File(imagesDirectory, "$frameHash.png")
                            val frameSnapshot = snapshot.copy(name = "${snapshot.name} $index")
                            val goldenFile = File(goldenImagesDirectory, frameSnapshot.toFileName("_", "png"))
                            if (!goldenFile.exists()) {
                                originalFrame.copyTo(goldenFile)
                            }
                        }
                    }
                    val original = File(videosDirectory, "$hash.mov")
                    if (isRecording) {
                        val goldenFile = File(goldenVideosDirectory, snapshot.toFileName("_", "mov"))
                        if (!goldenFile.exists()) {
                            original.copyTo(goldenFile)
                        }
                    }
                    snapshot.copy(file = original.toJsonPath())
                }

                shots += shot
            }
        }
    }

    /** Returns the hash of the image. */
    private fun writeImage(image: BufferedImage): String {
        val hash = hash(image)
        val file = File(imagesDirectory, "$hash.png")
        if (!file.exists()) {
            file.writeAtomically(image)
        }
        return hash
    }

    /** Returns a SHA-1 hash of the pixels of [image]. */
    private fun hash(image: BufferedImage): String {
        val hashingSink = HashingSink.sha1(blackholeSink())
        hashingSink.buffer().use { sink ->
            for (y in 0 until image.height) {
                for (x in 0 until image.width) {
                    sink.writeInt(image.getRGB(x, y))
                }
            }
        }
        return hashingSink.hash.hex()
    }

    private fun writeVideo(
        frameHashes: List<String>,
        fps: Int
    ): String {
        val hash = hash(frameHashes)
        val file = File(videosDirectory, "$hash.mov")
        if (!file.exists()) {
            val tmpFile = File(videosDirectory, "$hash.mov.tmp")
            val encoder = AWTSequenceEncoder.createSequenceEncoder(tmpFile, fps)
            for (frameHash in frameHashes) {
                val frame = ImageIO.read(File(imagesDirectory, "$frameHash.png"))
                encoder.encodeImage(frame)
            }
            encoder.finish()
            tmpFile.renameTo(file)
        }
        return hash
    }

    /** Returns a SHA-1 hash of [lines]. */
    private fun hash(lines: List<String>): String {
        val hashingSink = HashingSink.sha1(blackholeSink())
        hashingSink.buffer().use { sink ->
            for (hash in lines) {
                sink.writeUtf8(hash)
                sink.writeUtf8("\n")
            }
        }
        return hashingSink.hash.hex()
    }

    /** Release all resources and block until everything has been written to the file system. */
    override fun close() {
        writeRunJs()
    }

    /**
     * Emits the all runs index, which reads like JSON with an executable header.
     *
     * ```
     * window.all_runs = [
     *   "20190319153912aaab",
     *   "20190319153917bcfe"
     * ];
     * ```
     */
    private fun writeIndexJs() {
        val runNames = mutableListOf<String>()
        val runs = runsDirectory.list().sorted()
        for (run in runs) {
            if (run.endsWith(".js")) {
                runNames += run.substring(0, run.length - 3)
            }
        }

        File(rootDirectory, "index.js").writeAtomically {
            writeUtf8("window.all_runs = ")
            TDPaparazziJson.listOfStringsAdapter.toJson(this, runNames)
            writeUtf8(";")
        }
    }

    /**
     * Emits a run index, which reads like JSON with an executable header.
     *
     * ```
     * window.runs["20190319153912aaab"] = [
     *   {
     *     "name": "loading",
     *     "testName": "app.cash.CelebrityTest#testSettings",
     *     "timestamp": "2019-03-20T10:27:43Z",
     *     "tags": ["redesign"],
     *     "file": "loading.png"
     *   },
     *   {
     *     "name": "error",
     *     "testName": "app.cash.CelebrityTest#testSettings",
     *     "timestamp": "2019-03-20T10:27:43Z",
     *     "tags": ["redesign"],
     *     "file": "error.png"
     *   }
     * ];
     * ```
     */
    private fun writeRunJs() {
        val runJs = File(runsDirectory, "${runName.sanitizeForFilename()}.js")
        runJs.writeAtomically {
            writeUtf8("window.runs[\"$runName\"] = ")
            TDPaparazziJson.listOfShotsAdapter.toJson(this, shots)
            writeUtf8(";")
        }
    }

    private fun writeStaticFiles() {
        for (staticFile in listOf("index.html", "paparazzi.js")) {
            File(rootDirectory, staticFile).writeAtomically {
                writeAll(TDHtmlReportWriter::class.java.classLoader.getResourceAsStream(staticFile).source())
            }
        }
    }

    private fun File.writeAtomically(bufferedImage: BufferedImage) {
        val tmpFile = File(parentFile, "$name.tmp")
        ImageIO.write(bufferedImage, "PNG", tmpFile)
        delete()
        tmpFile.renameTo(this)
    }

    private fun File.writeAtomically(writerAction: BufferedSink.() -> Unit) {
        val tmpFile = File(parentFile, "$name.tmp")
        tmpFile.sink()
            .buffer()
            .use { sink ->
                sink.writerAction()
            }
        delete()
        tmpFile.renameTo(this)
    }

    private fun File.toJsonPath(): String = relativeTo(rootDirectory).invariantSeparatorsPath
}

internal fun defaultRunName(): String {
    val now = Date()
    val timestamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(now)
    val token = UUID.randomUUID().toString().substring(0, 6)
    return "${timestamp}_$token"
}

internal val filenameSafeChars = CharMatcher.inRange('a', 'z')
    .or(CharMatcher.inRange('0', '9'))
    .or(CharMatcher.anyOf("_-.~@^()[]{}:;,"))

internal fun String.sanitizeForFilename(): String? {
    return filenameSafeChars.negate().replaceFrom(toLowerCase(Locale.US), '_')
}
internal fun Snapshot.toFileName(
    delimiter: String = "_",
    extension: String
): String {
    val formattedLabel = if (name != null) {
        "$delimiter${name!!.toLowerCase(Locale.US).replace("\\s".toRegex(), delimiter)}"
    } else {
        ""
    }
    return "${testName.packageName}${delimiter}${testName.className}${delimiter}${testName.methodName}$formattedLabel.$extension"
}
