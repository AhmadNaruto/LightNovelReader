package indi.dmzz_yyhyy.lightnovelreader.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import indi.dmzz_yyhyy.lightnovelreader.data.local.room.LightNovelReaderDatabase
import indi.dmzz_yyhyy.lightnovelreader.data.local.room.dao.BookInformationDao
import indi.dmzz_yyhyy.lightnovelreader.data.local.room.dao.BookVolumesDao
import indi.dmzz_yyhyy.lightnovelreader.data.local.room.dao.BookshelfDao
import indi.dmzz_yyhyy.lightnovelreader.data.local.room.dao.ChapterContentDao
import indi.dmzz_yyhyy.lightnovelreader.data.local.room.dao.ReadingStatisticsDao
import indi.dmzz_yyhyy.lightnovelreader.data.local.room.dao.UserDataDao
import indi.dmzz_yyhyy.lightnovelreader.data.local.room.dao.UserReadingDataDao
import indi.dmzz_yyhyy.lightnovelreader.data.statistics.StatisticsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DaoModule {
    @Singleton
    @Provides
    fun provideBookInformationDao(db: LightNovelReaderDatabase): BookInformationDao =
        db.bookInformationDao()

    @Singleton
    @Provides
    fun provideBookVolumesDao(db: LightNovelReaderDatabase): BookVolumesDao =
        db.bookVolumesDao()

    @Singleton
    @Provides
    fun provideChapterContentDao(db: LightNovelReaderDatabase): ChapterContentDao =
        db.chapterContentDao()

    @Singleton
    @Provides
    fun provideUserReadingDataDao(db: LightNovelReaderDatabase): UserReadingDataDao =
        db.userReadingDataDao()

    @Singleton
    @Provides
    fun provideUserDataDao(db: LightNovelReaderDatabase): UserDataDao =
        db.userDataDao()

    @Singleton
    @Provides
    fun provideBookshelfDao(db: LightNovelReaderDatabase): BookshelfDao =
        db.bookshelfDao()

    @Provides
    @Singleton
    fun provideReadingStatisticsDao(database: LightNovelReaderDatabase): ReadingStatisticsDao {
        return database.readingStatisticsDao()
    }

    @Provides
    @Singleton
    fun provideStatisticsRepository(readingStatisticsDao: ReadingStatisticsDao): StatisticsRepository {
        return StatisticsRepository(readingStatisticsDao)
    }
}