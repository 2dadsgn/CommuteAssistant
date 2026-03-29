package com.commuteassistant.di

import android.content.Context
import androidx.room.Room
import com.commuteassistant.data.db.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CommuteDatabase =
        Room.databaseBuilder(context, CommuteDatabase::class.java, "commute_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideRoutineDao(db: CommuteDatabase): CommuteRoutineDao = db.routineDao()

    @Provides
    fun provideSnapshotDao(db: CommuteDatabase): TrafficSnapshotDao = db.snapshotDao()
}
