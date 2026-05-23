package com.workalbum.app

import android.app.Application
import com.workalbum.app.data.ImageDatabase
import com.workalbum.app.data.ImageRepository

/**
 * Application 类，初始化全局依赖
 */
class WorkAlbumApplication : Application() {

    lateinit var imageRepository: ImageRepository
        private set

    override fun onCreate() {
        super.onCreate()
        // 初始化数据库（触发创建）
        ImageDatabase.getInstance(this)
        // 初始化数据仓库
        imageRepository = ImageRepository(this)
    }
}
