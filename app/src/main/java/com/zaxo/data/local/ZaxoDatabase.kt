package com.zaxo.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ContactEntity::class,
        ChatEntity::class,
        MessageEntity::class,
        MessageFtsEntity::class,
        StatusEntity::class,
        CallRecordEntity::class,
        DeviceEntity::class,
        SettingEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class ZaxoDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun statusDao(): StatusDao
    abstract fun callRecordDao(): CallRecordDao
    abstract fun deviceDao(): DeviceDao
    abstract fun settingDao(): SettingDao

    companion object {
        @Volatile
        private var INSTANCE: ZaxoDatabase? = null

        fun getDatabase(context: Context): ZaxoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ZaxoDatabase::class.java,
                    "zaxo_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
