package indi.dmzz_yyhyy.lightnovelreader.data.web.wenku8.exploration

import indi.dmzz_yyhyy.lightnovelreader.data.exploration.ExplorationBooksRow
import indi.dmzz_yyhyy.lightnovelreader.data.exploration.ExplorationDisplayBook
import indi.dmzz_yyhyy.lightnovelreader.data.exploration.ExplorationPage
import indi.dmzz_yyhyy.lightnovelreader.data.web.exploration.ExplorationPageDataSource
import indi.dmzz_yyhyy.lightnovelreader.utils.autoReconnectionGet
import indi.dmzz_yyhyy.lightnovelreader.data.web.wenku8.wenku8Cookie
import java.net.URLEncoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object Wenku8TagsExplorationPage: ExplorationPageDataSource {
    private var lock = false
    private val explorationBooksRows: MutableStateFlow<List<ExplorationBooksRow>> = MutableStateFlow(emptyList())

    override fun getExplorationPage(): ExplorationPage  {
        if (!lock) {
            lock = true
            CoroutineScope(Dispatchers.IO).launch {
                Jsoup
                    .connect("https://www.wenku8.cc/modules/article/tags.php")
                    .wenku8Cookie()
                    .autoReconnectionGet()
                    ?.select("a[href~=tags\\.php\\?t=.*]")
                    ?.slice(0..48)
                    ?.map { "https://www.wenku8.cc/modules/article/" + it.attr("href") }
                    ?.map {url ->
                        val soup = Jsoup
                            .connect(url.split("=")[0] + "=" +
                                    URLEncoder.encode(url.split("=")[1], "gb2312"))
                            .wenku8Cookie()
                            .autoReconnectionGet()
                        explorationBooksRows.update {
                            it + getExplorationBookRow(
                                soup = soup,
                                title = url.split("=")[1]
                            )
                        }
                    }
            }
        }

        return ExplorationPage("分类", explorationBooksRows)
    }

    private fun getExplorationBookRow(title: String, soup: Document?): ExplorationBooksRow {
        soup ?: return ExplorationBooksRow(
            "",
            emptyList(),
            false,
            ""
        )
        val idlList = soup.select("#content > table > tbody > tr:nth-child(2) > td > div > div:nth-child(1) > a")
            .map { it.attr("href").replace("/book/", "").replace(".htm", "").toInt() }
        val titleList = soup.select("#content > table > tbody > tr:nth-child(2) > td > div > div:nth-child(2) > b > a")
            .map { it.text().split("(").getOrNull(0) ?: "" }
        val coverUrlList = soup.select("#content > table > tbody > tr:nth-child(2) > td > div > div:nth-child(1) > a > img")
            .map { it.attr("src") }
        return ExplorationBooksRow(
            title = title,
            bookList = (0..5).map {
                ExplorationDisplayBook(
                    id = idlList[it],
                    title = titleList[it],
                    coverUrl = coverUrlList[it],
                )
            },
            expandable = true,
            expandedPageDataSourceId = title
        )
    }
}