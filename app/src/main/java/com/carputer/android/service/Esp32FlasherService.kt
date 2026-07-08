package com.carputer.android.service

import android.content.Context
import android.hardware.usb.UsbManager
import android.net.Uri
import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

enum class FlasherState {
    IDLE, DETECTING, CONNECTING, SYNCING, FLASHING, VERIFYING, DONE, ERROR
}

data class FlasherStatus(
    val state: FlasherState = FlasherState.IDLE,
    val progress: Float = 0f,
    val statusText: String = "",
    val deviceName: String = "",
)

class Esp32FlasherService(private val context: Context) {

    companion object {
        private const val TAG = "Esp32Flasher"
        private const val DEFAULT_BAUD = 115200
        private const val BLOCK_SIZE = 1024
        private const val SYNC_ATTEMPTS = 6

        // ESP ROM commands
        private const val CMD_SYNC = 0x08
        private const val CMD_CHIP_DETECT = 0x0D
        private const val CMD_SPI_ATTACH = 0x09
        private const val CMD_FLASH_BEGIN = 0x02
        private const val CMD_FLASH_DATA = 0x03
        private const val CMD_FLASH_END = 0x04

        // SLIP consts
        private const val SLIP_END = 0xC0
        private const val SLIP_ESC = 0xDB
        private const val SLIP_ESC_END = 0xDC
        private const val SLIP_ESC_ESC = 0xDD
    }

    private val _status = MutableStateFlow(FlasherStatus())
    val status: StateFlow<FlasherStatus> = _status.asStateFlow()

    private var port: UsbSerialPort? = null
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun findDevice(): List<UsbSerialDriver> {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        return UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
    }

    fun hasPermission(driver: UsbSerialDriver): Boolean {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        return usbManager.hasPermission(driver.device)
    }

    fun flashFromUri(driver: UsbSerialDriver, uri: Uri) {
        val ctx = context
        val stream = ctx.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open $uri")
        val size = stream.available()
        flash(driver, stream, size)
    }

    fun flashFromFile(driver: UsbSerialDriver, path: String) {
        val stream = FileInputStream(path)
        val size = stream.available()
        flash(driver, stream, size)
    }

    fun flashFromAsset(driver: UsbSerialDriver, assetPath: String) {
        val stream = context.assets.open(assetPath)
        val size = stream.available()
        flash(driver, stream, size)
    }

    private fun flash(driver: UsbSerialDriver, firmwareStream: InputStream, firmwareSize: Int) {
        Log.d(TAG, "Starting flash: size=$firmwareSize")
        _status.value = FlasherStatus(state = FlasherState.DETECTING, statusText = "Opening USB...",
            deviceName = driver.device.productName ?: "ESP32")
        job?.cancel()
        job = scope.launch {
            try {
                val usbPort = driver.ports[0]
                port = usbPort
                val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
                usbPort.open(usbManager.openDevice(driver.device))
                usbPort.setParameters(DEFAULT_BAUD, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
                usbPort.dtr = true
                usbPort.rts = false
                delay(100)

                _status.value = FlasherStatus(state = FlasherState.SYNCING, progress = 0f,
                    statusText = "Syncing...", deviceName = driver.device.productName ?: "ESP32")

                sync()
                delay(50)
                chipDetect()
                delay(50)
                spiAttach()
                delay(50)

                val totalBlocks = (firmwareSize + BLOCK_SIZE - 1) / BLOCK_SIZE
                flashBegin(0x0000, firmwareSize, totalBlocks, BLOCK_SIZE)

                val buffer = ByteArray(BLOCK_SIZE)
                var bytesSent = 0

                while (bytesSent < firmwareSize) {
                    val chunkSize = minOf(BLOCK_SIZE, firmwareSize - bytesSent)
                    val read = firmwareStream.read(buffer, 0, chunkSize)
                    if (read <= 0) break

                    flashData(0x0000 + bytesSent, buffer.copyOf(read))
                    bytesSent += read

                    val pct = bytesSent.toFloat() / firmwareSize
                    _status.value = FlasherStatus(state = FlasherState.FLASHING,
                        progress = pct, statusText = "Flashing... ${(pct * 100).toInt()}%",
                        deviceName = driver.device.productName ?: "ESP32")
                }

                flashEnd(true)

                _status.value = FlasherStatus(state = FlasherState.DONE,
                    progress = 1f, statusText = "Flash complete!",
                    deviceName = driver.device.productName ?: "ESP32")

            } catch (e: Exception) {
                Log.e(TAG, "Flash failed", e)
                _status.value = FlasherStatus(state = FlasherState.ERROR,
                    statusText = e.message ?: "Unknown error")
            } finally {
                try { port?.close() } catch (_: Exception) {}
                port = null
            }
        }
    }

    fun cancel() {
        job?.cancel()
        try { port?.close() } catch (_: Exception) {}
        port = null
        _status.value = FlasherStatus(state = FlasherState.IDLE, statusText = "Cancelled")
    }

    // ----- Protocol -----

    private suspend fun sync() {
        val syncPayload = byteArrayOf(0x07, 0x07, 0x12, 0x20) + ByteArray(32) { 0x55 }
        for (i in 0 until SYNC_ATTEMPTS) {
            sendCmd(CMD_SYNC, syncPayload)
            delay(20)
            val resp = readCmd(CMD_SYNC)
            if (resp != null && resp.isNotEmpty() && resp[0] == 0x00.toByte()) return
        }
        throw IllegalStateException("Sync failed - check connections (GPIO0 low on boot?)")
    }

    private suspend fun chipDetect() {
        sendCmd(CMD_CHIP_DETECT, byteArrayOf())
        delay(20)
        readCmd(CMD_CHIP_DETECT)
    }

    private suspend fun spiAttach() {
        val buf = ByteBuffer.wrap(ByteArray(4)).order(ByteOrder.LITTLE_ENDIAN).putInt(0).array()
        sendCmd(CMD_SPI_ATTACH, buf)
        delay(20)
        val resp = readCmd(CMD_SPI_ATTACH)
        if (resp == null || resp[0] != 0x00.toByte()) throw IllegalStateException("SPI attach failed")
    }

    private suspend fun flashBegin(address: Int, size: Int, blockCount: Int, blockSize: Int) {
        val buf = ByteBuffer.wrap(ByteArray(15)).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(size)
        buf.putInt(blockCount)
        buf.putInt(blockSize)
        buf.put(0x00.toByte())
        sendCmd(CMD_FLASH_BEGIN, buf.array())
        delay(20)
        val resp = readCmd(CMD_FLASH_BEGIN)
        if (resp == null || resp[0] != 0x00.toByte()) throw IllegalStateException("Flash begin failed")
    }

    private suspend fun flashData(address: Int, data: ByteArray) {
        val csum = computeChecksum(data)
        val hdr = ByteBuffer.wrap(ByteArray(16)).order(ByteOrder.LITTLE_ENDIAN)
        hdr.putInt(data.size)
        hdr.putInt(data.size)
        hdr.putInt(address)
        hdr.putInt(0) // sequence
        sendCmd(CMD_FLASH_DATA, hdr.array() + data)
        delay(10)
        val resp = readCmd(CMD_FLASH_DATA)
        if (resp == null || resp[0] != 0x00.toByte()) {
            throw IllegalStateException("Flash data failed at 0x${address.toString(16)}")
        }
    }

    private suspend fun flashEnd(reboot: Boolean) {
        val buf = ByteBuffer.wrap(ByteArray(4)).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(if (reboot) 1 else 0).array()
        sendCmd(CMD_FLASH_END, buf)
    }

    // ----- Serial helpers -----

    private suspend fun sendCmd(cmd: Int, payload: ByteArray) {
        val p = port ?: throw IllegalStateException("Port closed")
        val csum = computeChecksum(payload)
        val raw = ByteBuffer.wrap(ByteArray(1 + 1 + 2 + 1 + payload.size)).order(ByteOrder.LITTLE_ENDIAN)
        raw.put(0x00.toByte())           // dir = request
        raw.put(cmd.toByte())            // command
        raw.putShort(payload.size.toShort())
        raw.put(csum)
        raw.put(payload)
        p.write(slipEncode(raw.array()), 1000)
    }

    private suspend fun readCmd(expectedCmd: Int): ByteArray? {
        val p = port ?: throw IllegalStateException("Port closed")
        val buf = ByteArray(2048)
        var accumulated = ByteArray(0)

        for (i in 0 until 50) {
            val read = p.read(buf, 200)
            if (read > 0) {
                accumulated = accumulated + buf.copyOf(read)
                val decoded = slipDecode(accumulated)
                if (decoded != null && decoded.size >= 5) {
                    if (decoded[0] == 0x01.toByte() && decoded[1] == expectedCmd.toByte()) {
                        return if (decoded.size > 5) decoded.copyOfRange(5, decoded.size)
                        else byteArrayOf()
                    }
                }
            }
            delay(10)
        }
        return null
    }

    private fun computeChecksum(data: ByteArray): Byte {
        var sum = 0xEF
        for (b in data) sum = sum xor (b.toInt() and 0xFF)
        return (sum and 0xFF).toByte()
    }

    private fun slipEncode(data: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(SLIP_END)
        for (b in data) {
            val v = b.toInt() and 0xFF
            when (v) {
                SLIP_END -> { out.write(SLIP_ESC); out.write(SLIP_ESC_END) }
                SLIP_ESC -> { out.write(SLIP_ESC); out.write(SLIP_ESC_ESC) }
                else -> out.write(v)
            }
        }
        out.write(SLIP_END)
        return out.toByteArray()
    }

    private fun slipDecode(data: ByteArray): ByteArray? {
        if (data.size < 2 || data[0].toInt() != SLIP_END) return null
        val out = ByteArrayOutputStream()
        var i = 1
        while (i < data.size) {
            val b = data[i].toInt() and 0xFF
            if (b == SLIP_END) break
            if (b == SLIP_ESC) {
                i++
                if (i >= data.size) return null
                when (data[i].toInt() and 0xFF) {
                    SLIP_ESC_END -> out.write(SLIP_END)
                    SLIP_ESC_ESC -> out.write(SLIP_ESC)
                    else -> return null
                }
            } else {
                out.write(b)
            }
            i++
        }
        return if (out.size() > 0) out.toByteArray() else null
    }
}
