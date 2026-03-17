package com.glasses.app.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 音频转换工具
 * 将PCM音频转换为WAV格式
 * 
 * WAV文件格式：
 * - RIFF Header (12 bytes)
 * - Format Chunk (24 bytes)
 * - Data Chunk (8 bytes + PCM data)
 */
object AudioConverter {
    
    private const val TAG = "AudioConverter"
    
    // WAV文件头常量
    private const val RIFF_HEADER_SIZE = 12
    private const val FORMAT_CHUNK_SIZE = 24
    private const val DATA_CHUNK_HEADER_SIZE = 8
    private const val WAV_HEADER_SIZE = RIFF_HEADER_SIZE + FORMAT_CHUNK_SIZE + DATA_CHUNK_HEADER_SIZE
    
    /**
     * 将PCM音频数据转换为WAV格式
     * 
     * @param pcmData PCM原始数据
     * @param sampleRate 采样率 (默认16000Hz)
     * @param channels 声道数 (默认1-单声道)
     * @param bitDepth 位深度 (默认16bit)
     * @return WAV格式的字节数组
     */
    fun pcmToWav(
        pcmData: ByteArray,
        sampleRate: Int = 16000,
        channels: Int = 1,
        bitDepth: Int = 16
    ): ByteArray {
        try {
            // 验证参数
            require(sampleRate > 0) { "Sample rate must be positive" }
            require(channels in 1..2) { "Channels must be 1 (mono) or 2 (stereo)" }
            require(bitDepth in listOf(8, 16, 24, 32)) { "Bit depth must be 8, 16, 24, or 32" }
            
            val pcmDataSize = pcmData.size
            val wavDataSize = WAV_HEADER_SIZE + pcmDataSize
            
            // 创建WAV数据缓冲区
            val wavData = ByteBuffer.allocate(wavDataSize)
            wavData.order(ByteOrder.LITTLE_ENDIAN)
            
            // 写入RIFF Header (12 bytes)
            writeRiffHeader(wavData, pcmDataSize)
            
            // 写入Format Chunk (24 bytes)
            writeFormatChunk(wavData, sampleRate, channels, bitDepth)
            
            // 写入Data Chunk (8 bytes + PCM data)
            writeDataChunk(wavData, pcmData)
            
            Log.d(TAG, "PCM to WAV conversion successful: ${pcmData.size} bytes -> ${wavData.array().size} bytes")
            return wavData.array()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert PCM to WAV", e)
            throw e
        }
    }

    /**
     * 将PCM文件转换为WAV文件
     * 
     * @param pcmFile PCM文件
     * @param wavFile 输出的WAV文件
     * @return Result<Unit> 成功或失败
     */
    suspend fun convertPcmToWav(
        pcmFile: File,
        wavFile: File,
        sampleRate: Int = 16000,
        channels: Int = 1,
        bitDepth: Int = 16
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 验证PCM文件存在
            require(pcmFile.exists()) { "PCM file does not exist: ${pcmFile.absolutePath}" }
            
            // 读取PCM数据
            val pcmData = FileInputStream(pcmFile).use { input ->
                input.readBytes()
            }
            
            Log.d(TAG, "Read PCM file: ${pcmFile.absolutePath}, size: ${pcmData.size} bytes")
            
            // 转换为WAV
            val wavData = pcmToWav(pcmData, sampleRate, channels, bitDepth)
            
            // 写入WAV文件
            FileOutputStream(wavFile).use { output ->
                output.write(wavData)
            }
            
            Log.d(TAG, "WAV file created: ${wavFile.absolutePath}, size: ${wavData.size} bytes")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert PCM file to WAV", e)
            Result.failure(e)
        }
    }
    
    /**
     * 验证音频参数
     * 
     * @param sampleRate 采样率
     * @param channels 声道数
     * @param bitDepth 位深度
     * @return true if valid, false otherwise
     */
    fun validateAudioParams(
        sampleRate: Int,
        channels: Int,
        bitDepth: Int
    ): Boolean {
        return try {
            require(sampleRate > 0) { "Sample rate must be positive" }
            require(channels in 1..2) { "Channels must be 1 (mono) or 2 (stereo)" }
            require(bitDepth in listOf(8, 16, 24, 32)) { "Bit depth must be 8, 16, 24, or 32" }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Invalid audio parameters", e)
            false
        }
    }
    
    /**
     * 写入RIFF Header (12 bytes)
     * 
     * Format:
     * - "RIFF" (4 bytes)
     * - File size - 8 (4 bytes, little-endian)
     * - "WAVE" (4 bytes)
     */
    private fun writeRiffHeader(buffer: ByteBuffer, pcmDataSize: Int) {
        // "RIFF" 标识
        buffer.put("RIFF".toByteArray())
        
        // 文件大小 - 8 (不包括RIFF标识和文件大小字段本身)
        val fileSize = WAV_HEADER_SIZE + pcmDataSize - 8
        buffer.putInt(fileSize)
        
        // "WAVE" 标识
        buffer.put("WAVE".toByteArray())
    }
    
    /**
     * 写入Format Chunk (24 bytes)
     * 
     * Format:
     * - "fmt " (4 bytes)
     * - Format chunk size: 16 (4 bytes)
     * - Audio format: 1 (PCM) (2 bytes)
     * - Number of channels (2 bytes)
     * - Sample rate (4 bytes)
     * - Byte rate (4 bytes)
     * - Block align (2 bytes)
     * - Bits per sample (2 bytes)
     */
    private fun writeFormatChunk(
        buffer: ByteBuffer,
        sampleRate: Int,
        channels: Int,
        bitDepth: Int
    ) {
        // "fmt " 标识
        buffer.put("fmt ".toByteArray())
        
        // Format chunk size (固定为16)
        buffer.putInt(16)
        
        // Audio format (1 = PCM)
        buffer.putShort(1)
        
        // Number of channels
        buffer.putShort(channels.toShort())
        
        // Sample rate
        buffer.putInt(sampleRate)
        
        // Byte rate (SampleRate * NumChannels * BitsPerSample/8)
        val byteRate = sampleRate * channels * bitDepth / 8
        buffer.putInt(byteRate)
        
        // Block align (NumChannels * BitsPerSample/8)
        val blockAlign = (channels * bitDepth / 8).toShort()
        buffer.putShort(blockAlign)
        
        // Bits per sample
        buffer.putShort(bitDepth.toShort())
    }
    
    /**
     * 写入Data Chunk (8 bytes + PCM data)
     * 
     * Format:
     * - "data" (4 bytes)
     * - Data size (4 bytes)
     * - PCM data (variable)
     */
    private fun writeDataChunk(buffer: ByteBuffer, pcmData: ByteArray) {
        // "data" 标识
        buffer.put("data".toByteArray())
        
        // Data size
        buffer.putInt(pcmData.size)
        
        // PCM data
        buffer.put(pcmData)
    }
    
    /**
     * 获取WAV文件信息
     * 
     * @param wavFile WAV文件
     * @return WAV文件信息，如果文件无效则返回null
     */
    fun getWavFileInfo(wavFile: File): WavFileInfo? {
        return try {
            FileInputStream(wavFile).use { input ->
                val header = ByteArray(WAV_HEADER_SIZE)
                input.read(header)
                
                val buffer = ByteBuffer.wrap(header)
                buffer.order(ByteOrder.LITTLE_ENDIAN)
                
                // 验证RIFF标识
                val riff = ByteArray(4)
                buffer.get(riff)
                if (String(riff) != "RIFF") return null
                
                // 读取文件大小
                val fileSize = buffer.int
                
                // 验证WAVE标识
                val wave = ByteArray(4)
                buffer.get(wave)
                if (String(wave) != "WAVE") return null
                
                // 验证fmt标识
                val fmt = ByteArray(4)
                buffer.get(fmt)
                if (String(fmt) != "fmt ") return null
                
                // 读取format chunk
                val formatChunkSize = buffer.int
                val audioFormat = buffer.short
                val channels = buffer.short
                val sampleRate = buffer.int
                val byteRate = buffer.int
                val blockAlign = buffer.short
                val bitDepth = buffer.short
                
                WavFileInfo(
                    sampleRate = sampleRate,
                    channels = channels.toInt(),
                    bitDepth = bitDepth.toInt(),
                    fileSize = fileSize + 8,
                    duration = calculateDuration(wavFile.length() - WAV_HEADER_SIZE, sampleRate, channels.toInt(), bitDepth.toInt())
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read WAV file info", e)
            null
        }
    }
    
    /**
     * 计算音频时长（毫秒）
     */
    private fun calculateDuration(
        dataSize: Long,
        sampleRate: Int,
        channels: Int,
        bitDepth: Int
    ): Long {
        val bytesPerSecond = sampleRate * channels * bitDepth / 8
        return (dataSize * 1000 / bytesPerSecond)
    }
}

/**
 * WAV文件信息
 */
data class WavFileInfo(
    val sampleRate: Int,
    val channels: Int,
    val bitDepth: Int,
    val fileSize: Int,
    val duration: Long  // 毫秒
)
