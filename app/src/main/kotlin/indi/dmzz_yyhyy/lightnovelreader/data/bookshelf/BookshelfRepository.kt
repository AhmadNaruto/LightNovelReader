package indi.dmzz_yyhyy.lightnovelreader.data.bookshelf

import indi.dmzz_yyhyy.lightnovelreader.data.loacltion.room.converter.LocalDataTimeConverter.dateToString
import indi.dmzz_yyhyy.lightnovelreader.data.local.room.dao.BookInformationDao
import indi.dmzz_yyhyy.lightnovelreader.data.local.room.dao.BookshelfDao
import indi.dmzz_yyhyy.lightnovelreader.data.local.room.entity.BookshelfEntity
import java.time.Instant
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class BookshelfRepository @Inject constructor(
    private val bookshelfDao: BookshelfDao,
    private val bookInformationDao: BookInformationDao
) {
    fun getAllBookshelfIds(): List<Int> = bookshelfDao.getAllBookshelfIds()

    @Suppress("DuplicatedCode")
    fun getBookshelf(id: Int): MutableBookshelf? = MutableBookshelf().apply {
        val bookshelfEntity = bookshelfDao.getBookShelf(id) ?: return null
        this.id = id
        this.name = bookshelfEntity.name
        this.sortType = BookshelfSortType.entries.first { it.key == bookshelfEntity.sortType }
        this.autoCache = bookshelfEntity.autoCache
        this.systemUpdateReminder = bookshelfEntity.systemUpdateReminder
        this.allBookIds = bookshelfEntity.allBookIds
        this.pinnedBookIds = bookshelfEntity.pinnedBookIds
        this.updatedBookIds = bookshelfEntity.updatedBookIds
    }

    @Suppress("DuplicatedCode")
    fun getBookshelfFlow(id: Int): Flow<MutableBookshelf?> = bookshelfDao
        .getBookShelfFlow(id)
        .map { bookshelfEntity ->
            bookshelfEntity ?: return@map null
            MutableBookshelf().apply {
                this.id = id
                this.name = bookshelfEntity.name
                this.sortType = BookshelfSortType.entries.first { it.key == bookshelfEntity.sortType }
                this.autoCache = bookshelfEntity.autoCache
                this.systemUpdateReminder = bookshelfEntity.systemUpdateReminder
                this.allBookIds = bookshelfEntity.allBookIds
                this.pinnedBookIds = bookshelfEntity.pinnedBookIds
                this.updatedBookIds = bookshelfEntity.updatedBookIds
            }
        }

    fun crateBookShelf(
        name: String,
        sortType: BookshelfSortType,
        autoCache: Boolean,
        systemUpdateReminder: Boolean,
    ): Int {
        bookshelfDao.createBookshelf(BookshelfEntity(
            id = Instant.now().epochSecond.hashCode(),
            name = name,
            sortType = sortType.key,
            autoCache = autoCache,
            systemUpdateReminder = systemUpdateReminder,
            allBookIds = emptyList(),
            pinnedBookIds = emptyList(),
            updatedBookIds = emptyList(),
        ))
        return Instant.now().epochSecond.hashCode()
    }

    fun deleteBookshelf(bookshelfId: Int) {
        bookshelfDao.getBookShelf(bookshelfId)?.let { bookshelf ->
            bookshelf.allBookIds.forEach { bookId ->
                clearBookshelfIdFromBookshelfBookMetadata(bookshelfId, bookId)
            }
        }
        bookshelfDao.deleteBookshelf(bookshelfId)
    }

    suspend fun addBookIntoBookShelf(bookshelfId: Int, bookId: Int) {
        val bookshelf = bookshelfDao.getBookShelf(bookshelfId) ?: return
        bookshelfDao.addBookshelfMetadata(
            id = bookId,
            lastUpdate = dateToString(bookInformationDao.get(bookId)?.lastUpdated ?: LocalDateTime.MIN) ?: "",
            bookshelfIds = listOf(bookshelfId)
        )
        (bookshelf.allBookIds + listOf(bookId)).let {
            bookshelfDao.updateBookshelfEntity(
                bookshelf.copy(
                    allBookIds = it.distinct(),
                )
            )
        }
    }

    suspend fun addFixedBooksIntoBookShelf(bookShelfId: Int, bookId: Int) {
        val bookshelf = bookshelfDao.getBookShelf(bookShelfId) ?: return
        (bookshelf.pinnedBookIds + listOf(bookId)).let {
            addBookIntoBookShelf(bookShelfId, bookId)
            bookshelfDao.updateBookshelfEntity(
                bookshelf.copy(
                    pinnedBookIds = it.distinct(),
                )
            )
        }
    }

    suspend fun addUpdatedBooksIntoBookShelf(bookShelfId: Int, bookId: Int) {
        val bookshelf = bookshelfDao.getBookShelf(bookShelfId) ?: return
        (bookshelf.updatedBookIds + listOf(bookId)).let {
            addBookIntoBookShelf(bookShelfId, bookId)
            bookshelfDao.updateBookshelfEntity(
                bookshelf.copy(
                    updatedBookIds = it.distinct(),
                )
            )
        }
    }

    fun updateBookshelf(bookshelfId: Int, updater: (MutableBookshelf) -> Bookshelf) {
        this.getBookshelf(bookshelfId)?.let { oldBookshelf ->
            updater(oldBookshelf).let { newBookshelf ->
                bookshelfDao.updateBookshelfEntity(
                    BookshelfEntity(
                        bookshelfId,
                        newBookshelf.name,
                        newBookshelf.sortType.key,
                        newBookshelf.autoCache,
                        newBookshelf.systemUpdateReminder,
                        newBookshelf.allBookIds,
                        newBookshelf.pinnedBookIds,
                        newBookshelf.updatedBookIds,
                    )
                )
            }
        }
    }

    fun getAllBookshelfBooksMetadataFlow(): Flow<List<BookshelfBookMetadata>> = bookshelfDao
        .getAllBookshelfBookEntitiesFlow()
        .map { allBookshelfBookEntities ->
            allBookshelfBookEntities.map {
                BookshelfBookMetadata(
                    id = it.id,
                    lastUpdate = it.lastUpdate,
                    bookShelfIds = it.bookShelfIds
                )
            }
        }

    fun getAllBookshelfBookIdsFlow(): Flow<List<Int>> = bookshelfDao.getAllBookshelfBookIdsFlow()

    fun getBookshelfBookMetadata(id: Int): BookshelfBookMetadata? = bookshelfDao.getBookshelfBookMetadata(id)

    private fun clearBookshelfIdFromBookshelfBookMetadata(bookshelfId: Int, bookId: Int) {
        bookshelfDao.getBookshelfBookMetadata(bookId)?.let { bookshelfBookMetadata ->
            bookshelfBookMetadata.bookShelfIds
                .toMutableList()
                .apply { removeAll { bookshelfId == it } }
                .let { bookshelfIds ->
                    if (bookshelfIds.isEmpty()) bookshelfDao.deleteBookshelfBookMetadata(bookId)
                    else dateToString(bookshelfBookMetadata.lastUpdate)?.let {
                        bookshelfDao.updateBookshelfBookMetadataEntity(
                            bookId,
                            it,
                            bookshelfIds.joinToString(",")
                        )
                    }
                }
        }
    }

    fun deleteBookFromBookshelf(bookshelfId: Int, bookId: Int) {
        clearBookshelfIdFromBookshelfBookMetadata(bookshelfId, bookId)
        updateBookshelf(bookshelfId) { oldBookshelf ->
            oldBookshelf.apply {
                this.allBookIds = allBookIds.toMutableList().apply { removeAll { it == bookId } }
                this.pinnedBookIds = pinnedBookIds.toMutableList().apply { removeAll { it == bookId } }
                this.updatedBookIds = updatedBookIds.toMutableList().apply { removeAll { it == bookId } }
            }
        }
    }
}