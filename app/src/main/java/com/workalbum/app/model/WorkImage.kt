package com.workalbum.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 工作图片数据模型
 */
@Entity(tableName = "work_images")
data class WorkImage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 图片在 APP 私有目录中的文件名 */
    val fileName: String,

    /** 图片来源：wechat / dingtalk / screenshot / browser / camera / other */
    val source: String = "other",

    /** 用户添加的文字备注 */
    val note: String = "",

    /** 图片大小（字节） */
    val fileSize: Long = 0,

    /** 图片宽度 */
    val width: Int = 0,

    /** 图片高度 */
    val height: Int = 0,

    /** 创建时间戳（毫秒） */
    val createdAt: Long = System.currentTimeMillis()
)
