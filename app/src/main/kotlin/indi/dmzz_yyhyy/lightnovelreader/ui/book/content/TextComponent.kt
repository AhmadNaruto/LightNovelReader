package indi.dmzz_yyhyy.lightnovelreader.ui.book.content

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import coil.compose.AsyncImage
import coil.request.ImageRequest
import indi.dmzz_yyhyy.lightnovelreader.AppEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun ContentText(
    content: String,
    fontSize: TextUnit,
    fontLineHeight: TextUnit,
    readingProgress: Float,
    isUsingFlipPage: Boolean,
    isUsingVolumeKeyFlip: Boolean,
    onChapterReadingProgressChange: (Float) -> Unit,
    onClick: () -> Unit,
) {
    if (!isUsingFlipPage)
        ScrollContentTextComponent(
            modifier = Modifier
                .animateContentSize()
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            content = content,
            fontSize = fontSize,
            fontLineHeight = fontLineHeight,
            readingProgress = readingProgress,
            onChapterReadingProgressChange = onChapterReadingProgressChange
        )
    else
        SimpleFlipPageTextComponent(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            content = content,
            fontSize = fontSize,
            fontLineHeight = fontLineHeight,
            readingProgress = readingProgress,
            isUsingVolumeKeyFlip = isUsingVolumeKeyFlip,
            onChapterReadingProgressChange = onChapterReadingProgressChange
        )
}

@Composable
fun ScrollContentTextComponent(
    modifier: Modifier,
    content: String,
    fontSize: TextUnit,
    fontLineHeight: TextUnit,
    readingProgress: Float,
    onChapterReadingProgressChange: (Float) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val contentLazyColumnState = rememberLazyListState()
    LaunchedEffect(readingProgress) {
        coroutineScope.launch {
            contentLazyColumnState.scrollToItem(
                0,
                ((contentLazyColumnState.layoutInfo.visibleItemsInfo.sumOf { it.size } - contentLazyColumnState.layoutInfo.viewportSize.height) *
                        readingProgress).toInt()
            )
        }
    }
    LaunchedEffect(contentLazyColumnState.firstVisibleItemScrollOffset) {
        val visibleItemsHeight = contentLazyColumnState.layoutInfo.visibleItemsInfo.sumOf { it.size }
        val viewportHeight = contentLazyColumnState.layoutInfo.viewportSize.height
        val progress = contentLazyColumnState.firstVisibleItemScrollOffset.toFloat() /
                (visibleItemsHeight - viewportHeight)
        onChapterReadingProgressChange(progress)
    }
    LazyColumn(
        modifier = modifier,
        state = contentLazyColumnState,
    ) {
        items(
            content
                .split("[image]")
        ) {
            BasicContentComponent(
                text = it,
                fontSize = fontSize,
                fontLineHeight = fontLineHeight,
            )
        }
    }
}

@Composable
fun SimpleFlipPageTextComponent(
    modifier: Modifier,
    content: String,
    fontSize: TextUnit,
    fontLineHeight: TextUnit,
    readingProgress: Float,
    isUsingVolumeKeyFlip: Boolean,
    onChapterReadingProgressChange: (Float) -> Unit,
) {
    val textMeasurer = rememberTextMeasurer()
    val scope = rememberCoroutineScope()
    val current = LocalContext.current
    var slipTextJob by remember { mutableStateOf<Job?>(null) }
    var constraints by remember { mutableStateOf<Constraints?>(null) }
    var textStyle by remember { mutableStateOf<TextStyle?>(null) }
    var slippedTextList by remember { mutableStateOf(emptyList<String>()) }
    var pageState by remember { mutableStateOf(PagerState { 0 }) }
    var readingPageFistCharOffset by remember { mutableStateOf(0) }
    var resumedReadingProgress by remember { mutableStateOf(false) }
    LaunchedEffect(content, textStyle, fontLineHeight, fontSize, constraints?.maxHeight, constraints?.maxWidth) {
        if (constraints != null && textStyle != null) {
            slipTextJob?.cancel()
            slipTextJob = scope.launch(Dispatchers.IO) {
                readingPageFistCharOffset = slippedTextList
                    .subList(0, pageState.currentPage)
                    .sumOf { it.length }
                    .plus(1)
                slippedTextList = slipText(
                    textMeasurer = textMeasurer,
                    constraints = constraints!!,
                    text = content,
                    style = textStyle!!.copy(
                        fontSize = fontSize,
                        fontWeight = FontWeight.W400,
                        lineHeight = (fontLineHeight.value + fontSize.value).sp
                    )
                )
                pageState = PagerState { slippedTextList.size }
                println("reset state")
                scope.launch {
                    slippedTextList
                        .let {
                            var totalOffset = 0
                            it.forEachIndexed { index, s ->
                                totalOffset += s.length
                                if (totalOffset >= readingPageFistCharOffset)
                                    return@let index
                            }
                            return@let 0
                        }
                        .let {
                            if (it != 0) {
                                pageState.scrollToPage(it)
                            }
                        }
                    if (!resumedReadingProgress) {
                        println((readingProgress * pageState.pageCount).toInt())
                        pageState.scrollToPage((readingProgress * pageState.pageCount).toInt())
                        resumedReadingProgress = true
                    }
                }
            }
        }
    }
    LaunchedEffect(readingProgress) {
        resumedReadingProgress = false
    }
    LaunchedEffect(pageState.currentPage, pageState.pageCount) {
        if (pageState.pageCount != 1)
            onChapterReadingProgressChange(pageState.currentPage.toFloat() / (pageState.pageCount - 1))
        else onChapterReadingProgressChange(1F)
    }
    DisposableEffect(isUsingVolumeKeyFlip) {
        val localBroadcastManager = LocalBroadcastManager.getInstance(current)
        val keycodeVolumeUpReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (isUsingVolumeKeyFlip && pageState.pageCount != 0)
                    scope.launch {
                        pageState.animateScrollToPage(pageState.currentPage - 1)
                    }
            }
        }
        val keycodeVolumeDownReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (isUsingVolumeKeyFlip && pageState.pageCount - 1 != pageState.currentPage)
                    scope.launch {
                        pageState.animateScrollToPage(pageState.currentPage + 1)
                    }
            }
        }
        localBroadcastManager.registerReceiver(keycodeVolumeUpReceiver, IntentFilter(AppEvent.KEYCODE_VOLUME_UP))
        localBroadcastManager.registerReceiver(keycodeVolumeDownReceiver, IntentFilter(AppEvent.KEYCODE_VOLUME_DOWN))
        onDispose {
            localBroadcastManager.unregisterReceiver(keycodeVolumeUpReceiver)
            localBroadcastManager.unregisterReceiver(keycodeVolumeDownReceiver)
        }
    }
    LocalContext.current.resources.displayMetrics.let { displayMetrics ->
        constraints = Constraints(
            maxWidth = displayMetrics
                .widthPixels
                .minus(WindowInsets.displayCutout.getRight(LocalDensity.current, LayoutDirection.Ltr))
                .minus(WindowInsets.displayCutout.getLeft(LocalDensity.current, LayoutDirection.Ltr))
                .minus(
                    with(LocalDensity.current) {
                        36.dp.toPx()
                    }.toInt()
                ),
            maxHeight = displayMetrics
                .heightPixels
                .minus(WindowInsets.displayCutout.getTop(LocalDensity.current))
                .minus(WindowInsets.displayCutout.getBottom(LocalDensity.current))
                .minus(
                    with(LocalDensity.current) {
                        48.dp.toPx()
                    }.toInt()
                ),
        )
    }
    textStyle = MaterialTheme.typography.bodyMedium
    HorizontalPager(
        state = pageState,
        modifier = modifier,
    ) {
        BasicContentComponent(
            modifier = modifier.fillMaxSize(),
            text = slippedTextList[it],
            fontSize = fontSize,
            fontLineHeight = fontLineHeight,
        )
    }
}

@Suppress("HttpUrlsUsage")
@Composable
fun BasicContentComponent(
    modifier: Modifier = Modifier,
    text: String,
    fontSize: TextUnit,
    fontLineHeight: TextUnit,
) {
    if (text.startsWith("http://") || text.startsWith("https://"))
        AsyncImage(
            modifier = modifier.fillMaxWidth(),
            model = ImageRequest.Builder(LocalContext.current)
                .data(text)
                .crossfade(true)
                .build(),
            contentDescription = null
        )
    else Text(
        modifier = modifier
            .padding(18.dp, 8.dp),
        text = text,
        textAlign = TextAlign.Start,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.W400,
        fontSize = fontSize,
        lineHeight = (fontSize.value + fontLineHeight.value).sp
    )
}

fun slipText(
    textMeasurer: TextMeasurer,
    constraints: Constraints,
    text: String,
    style: TextStyle,
): List<String> {
    val result = mutableListOf<String>()
    textMeasurer
        .measure(
            text = text,
            style = style,
            constraints = constraints
        )
        .let { textLayoutResult ->
            val textEndIndex = text.length
            var lastTextIndex = 0
            var lastOffset = 0F
            var index = 1
            while (textEndIndex != lastTextIndex) {
                textLayoutResult
                    .getOffsetForPosition(Offset(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat() - 5 + lastOffset))
                    .let { offset ->
                        textLayoutResult.getLineForOffset(offset).let {
                            if (textLayoutResult.getLineBottom(it) > constraints.maxHeight.toFloat() + lastOffset) {
                                lastOffset = textLayoutResult.getLineBottom(it - 1)
                                textLayoutResult.getOffsetForPosition(
                                    Offset(
                                        constraints.maxWidth.toFloat(),
                                        textLayoutResult.getLineTop(it) - 2
                                    )
                                )
                            }
                            else {
                                lastOffset += constraints.maxHeight
                                offset
                            }
                        }
                    }
                    .let {
                        result.add(text.substring(lastTextIndex, it))
                        lastTextIndex = it
                        index++
                    }
            }
        }
    return result
}