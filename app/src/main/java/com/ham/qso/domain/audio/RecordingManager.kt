package com.ham.qso.domain.audio

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 录音文件信息实体
 */
data class RecordingFileInfo(
    val file: File,
    val fileName: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val formattedSize: String,
    val lastModified: Long,
    val formattedDate: String,
    val qsoCount: Int,
    val isOrphan: Boolean
)

/**
 * 录音文件全生命周期管理工具
 */
object RecordingManager {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    /**
     * 获取应用内所有可能的录音存放目录
     */
    fun getRecordingDirectories(context: Context): List<File> {
        val dirs = mutableListOf<File>()
        context.getExternalFilesDir(Environment.DIRECTORY_RECORDINGS)?.let {
            if (it.exists() || it.mkdirs()) dirs.add(it)
        }
        val internalRecordings = File(context.filesDir, "Recordings")
        if (internalRecordings.exists() || internalRecordings.mkdirs()) {
            dirs.add(internalRecordings)
        }
        return dirs.distinctBy { it.absolutePath }
    }

    /**
     * 扫描所有录音文件，并结合数据库 QSO 引用情况计算属性
     */
    fun scanRecordings(
        context: Context,
        activeDbPaths: Set<String>,
        pathCountMap: Map<String, Int> = emptyMap()
    ): List<RecordingFileInfo> {
        val dirs = getRecordingDirectories(context)
        val fileList = mutableListOf<File>()

        for (dir in dirs) {
            val files = dir.listFiles { f ->
                f.isFile && (f.name.endsWith(".m4a", ignoreCase = true) ||
                        f.name.endsWith(".aac", ignoreCase = true) ||
                        f.name.endsWith(".mp4", ignoreCase = true))
            }
            if (files != null) {
                fileList.addAll(files)
            }
        }

        return fileList.map { f ->
            val path = f.absolutePath
            val count = pathCountMap[path] ?: if (activeDbPaths.contains(path)) 1 else 0
            val isOrphan = count == 0

            RecordingFileInfo(
                file = f,
                fileName = f.name,
                filePath = path,
                fileSizeBytes = f.length(),
                formattedSize = formatBytes(f.length()),
                lastModified = f.lastModified(),
                formattedDate = dateFormat.format(Date(f.lastModified())),
                qsoCount = count,
                isOrphan = isOrphan
            )
        }.sortedByDescending { it.lastModified }
    }

    /**
     * 计算录音列表总占用字节数
     */
    fun getTotalStorageBytes(recordings: List<RecordingFileInfo>): Long {
        return recordings.sumOf { it.fileSizeBytes }
    }

    /**
     * 格式化文件大小为可读字符串 (B / KB / MB / GB)
     */
    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        if (kb < 1.0) return "$bytes B"
        val mb = kb / 1024.0
        if (mb < 1.0) return "%.1f KB".format(kb)
        val gb = mb / 1024.0
        if (gb < 1.0) return "%.1f MB".format(mb)
        return "%.2f GB".format(gb)
    }

    /**
     * 删除指定的单个录音文件
     */
    fun deleteRecording(file: File): Boolean {
        return try {
            if (file.exists()) file.delete() else true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 批量清理所有无 QSO 引用的孤儿录音文件
     * @return 成功清理的文件数量
     */
    fun deleteOrphanRecordings(recordings: List<RecordingFileInfo>): Int {
        var deletedCount = 0
        for (item in recordings) {
            if (item.isOrphan) {
                if (deleteRecording(item.file)) {
                    deletedCount++
                }
            }
        }
        return deletedCount
    }

    /**
     * 调用系统原生分享面板，分享 .m4a 录音文件
     */
    fun shareRecording(context: Context, file: File) {
        if (!file.exists()) return
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Field QSO 通联录音: ${file.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "分享通联录音文件 (${file.name})"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
