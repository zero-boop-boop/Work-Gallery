package com.workalbum.app.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.workalbum.app.model.WorkImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * 图片数据仓库
 * 负责图片文件的物理存储和数据库操作
 */
class ImageRepository(private val context: Context) {

    private val dao = ImageDatabase.getInstance(context).imageDao()

    /** APP 私有图片存储目录 */
    private val imageDir: File
        get() = File(context.getExternalFilesDir(null), "images").also {
            if (!it.exists()) it.mkdirs()
        }

    /** 获取所有图片（Flow 自动更新） */
    fun getAllImages(): Flow<List<WorkImage>> = dao.getAllImages()

    /** 按来源筛选 */
    fun getImagesBySource(source: String): Flow<List<WorkImage>> = dao.getImagesBySource(source)

    /** 搜索备注 */
    fun searchByNote(keyword: String): Flow<List<WorkImage>> = dao.searchByNote(keyword)

    /** 保存外部分享的图片到私有目录 */
    suspend fun saveSharedImage(uri: Uri, source: String): WorkImage = withContext(Dispatchers.IO) {
        val fileName = "${UUID.randomUUID()}.jpg"
        val destFile = File(imageDir, fileName)

        // 复制图片到私有目录
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }

        // 读取图片尺寸
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(destFile.absolutePath, options)

        // 创建数据库记录
        val image = WorkImage(
            fileName = fileName,
            source = source,
            fileSize = destFile.length(),
            width = options.outWidth,
            height = options.outHeight
        )
        val id = dao.insertImage(image)
        image.copy(id = id)
    }

    /** 获取图片文件 */
    fun getImageFile(fileName: String): File = File(imageDir, fileName)

    /** 更新备注 */
    suspend fun updateNote(id: Long, note: String) {
        dao.updateNote(id, note)
    }

    /** 删除图片（文件和数据库记录） */
    suspend fun deleteImage(image: WorkImage) = withContext(Dispatchers.IO) {
        val file = getImageFile(image.fileName)
        if (file.exists()) file.delete()
        dao.deleteImage(image)
    }

    /** 获取统计信息 */
    suspend fun getStats(): Pair<Int, Long> = withContext(Dispatchers.IO) {
        val count = dao.getCount()
        val size = dao.getTotalSize()
        Pair(count, size)
    }

    /** 格式化文件大小 */
    fun formatSize(bytes: Long): String = when {
        bytes < 1024 -> "${bytes}B"
        bytes < 1024 * 1024 -> "${bytes / 1024}KB"
        bytes < 1024 * 1024 * 1024 -> "%.1fMB".format(bytes / (1024.0 * 1024.0))
        else -> "%.2fGB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    }
}
