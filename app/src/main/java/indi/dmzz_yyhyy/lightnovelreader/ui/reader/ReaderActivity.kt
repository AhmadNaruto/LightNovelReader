package indi.dmzz_yyhyy.lightnovelreader.ui.reader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import indi.dmzz_yyhyy.lightnovelreader.data.ReaderRepository
import indi.dmzz_yyhyy.lightnovelreader.ui.home.ReadingViewModel
import indi.dmzz_yyhyy.lightnovelreader.ui.theme.LightNovelReaderTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReaderActivity : ComponentActivity() {
    @Inject
    lateinit var readerRepository: ReaderRepository

    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        val bookId = intent.getIntExtra("bookId", 0)
        scope.launch {
            readerRepository.load(bookId)
        }

        setContent {
            val navController = rememberNavController()
            LightNovelReaderTheme {
        NavHost(
            navController = navController,
            startDestination = "chapter",
        ) {
            composable(route = "chapter") {
                val chapterViewModel: ChapterViewModel = hiltViewModel<ChapterViewModel>()
                ChapterScreen(navController, chapterViewModel)
            }
            composable(route = "reader"){

            }
        }
    }}}

    override fun onDestroy() {
        super.onDestroy()
        // 在Activity销毁时取消协程作用域，避免内存泄漏
        scope.cancel()
    }
}


