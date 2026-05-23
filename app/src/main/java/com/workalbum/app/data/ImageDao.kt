package com.workalbum.app.data

import androidx.room.*
import com.workalbum.app.model.WorkImage
import kotlinx.coroutines.flow.Flow

/**
 * 图片数据访问对象
 */
@Dao
interface ImageDao {

    /** 按时间倒序获取所有图片 */
    @Query("SELECT * FROM work_images ORDER BY createdAt DESC")
    fun getAllImages(): Flow<List<WorkImage>>

    /** 按来源筛选 */
    @Query("SELECT * FROM work_images WHERE source = :source ORDER BY createdAt DESC")
    fun getImagesBySource(source: String): Flow<List<WorkImage>>

    /** 搜索备注 */
    @Query("SELECT * FROM work_images WHERE note LIKE '%' || :keyword || '%' ORDER BY createdAt DESC")
    fun searchByNote(keyword: String): Flow<List<WorkImage>>

    /** 根据ID获取单张图片 */
    @Query("SELECT * FROM work_images WHERE id = :id")
    suspend fun getImageById(id: Long): WorkImage?

    /** 插入图片记录 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: WorkImage): Long

    /** 更新备注 */
    @Query("UPDATE work_images SET note = :note WHERE id = :id")
    suspend fun updateNote(id: Long, note: String)

    /** 删除图片记录 */
    @Delete
    suspend fun deleteImage(image: WorkImage)

    /** 获取图片总数 */
    @Query("SELECT COUNT(*) FROM work_images")
    suspend fun getCount(): Int

    /** 获取总占用空间 */
    @Query("SELECT COALESCE(SUM(fileSize), 0) FROM work_images")
    suspend fun getTotalSize(): Long
}
