package indi.dmzz_yyhyy.lightnovelreader.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import indi.dmzz_yyhyy.lightnovelreader.ui.Screen
import indi.dmzz_yyhyy.lightnovelreader.ui.home.bookshelf.BookShelfScreen
import indi.dmzz_yyhyy.lightnovelreader.ui.home.bookshelf.BookshelfScreenInfo
import indi.dmzz_yyhyy.lightnovelreader.ui.home.exploration.Exploration
import indi.dmzz_yyhyy.lightnovelreader.ui.home.exploration.ExplorationScreenInfo
import indi.dmzz_yyhyy.lightnovelreader.ui.home.reading.ReadingScreen
import indi.dmzz_yyhyy.lightnovelreader.ui.home.reading.ReadingScreenInfo
import indi.dmzz_yyhyy.lightnovelreader.ui.home.settings.SettingsScreen
import indi.dmzz_yyhyy.lightnovelreader.ui.home.settings.SettingsScreenInfo

@OptIn(ExperimentalAnimationGraphicsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onClickBook: (Int) -> Unit,
    onClickContinueReading: (Int, Int) -> Unit,
    checkUpdate: () -> Unit,
    requestAddBookToBookshelf: (Int) -> Unit,
) {
    val enterAlwaysScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val pinnedScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val navController = rememberNavController()
    var topBar : @Composable (TopAppBarScrollBehavior, TopAppBarScrollBehavior) -> Unit by remember { mutableStateOf( @Composable { _, _ -> }) }
    var dialog : @Composable () -> Unit by remember { mutableStateOf(@Composable {}) }
    var selectedItem by remember { mutableIntStateOf(0) }
    Scaffold(
        modifier = Modifier
            .nestedScroll(enterAlwaysScrollBehavior.nestedScrollConnection)
            .nestedScroll(pinnedScrollBehavior.nestedScrollConnection),
        topBar = {
            AnimatedContent(topBar, label = "TopBarAnimated") { topBar ->
                topBar(enterAlwaysScrollBehavior, pinnedScrollBehavior)
            }
        },
        bottomBar = {
            val entry by navController.currentBackStackEntryAsState()
            val destination = entry?.destination
            NavigationBar {
                for (navItem in listOf(
                    ReadingScreenInfo,
                    BookshelfScreenInfo,
                    ExplorationScreenInfo,
                    SettingsScreenInfo)
                ) {
                    val selected = destination?.hierarchy?.any {
                        it.route == navItem.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(navItem.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                painter = rememberAnimatedVectorPainter(
                                    AnimatedImageVector.animatedVectorResource(navItem.drawable),
                                    selected
                                ),
                                contentDescription = null
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(navItem.label),
                                maxLines = 1
                            )
                        }
                    )
                }
            }
        }
    ) {
        Box(Modifier.padding(it).padding(top = 4.dp)) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.Reading.route,
                enterTransition = { fadeIn() },
                exitTransition = { fadeOut() }
            ) {
                composable(route = Screen.Home.Reading.route) {
                    selectedItem = 0
                    ReadingScreen(
                        onClickBook = onClickBook,
                        onClickContinueReading = onClickContinueReading,
                        onClickJumpToExploration = { navController.navigate(Screen.Home.Exploration.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        } },
                        topBar = { newTopBar -> topBar = newTopBar }
                    )
                }
                composable(route = Screen.Home.Bookshelf.route) {
                    selectedItem = 1
                    BookShelfScreen(
                        topBar = { newTopBar -> topBar = newTopBar }
                    )
                }
                composable(route = Screen.Home.Exploration.route) {
                    selectedItem = 2
                    Exploration(
                        topBar = { newTopBar -> topBar = newTopBar },
                        dialog = { newDialog -> dialog = newDialog },
                        onClickBook = onClickBook
                    )
                }
                composable(route = Screen.Home.Settings.route) {
                    selectedItem = 3
                    SettingsScreen(
                        topBar = { newTopBar -> topBar = newTopBar },
                        checkUpdate = checkUpdate
                    )
                }
            }
        }
    }
    dialog.invoke()
}