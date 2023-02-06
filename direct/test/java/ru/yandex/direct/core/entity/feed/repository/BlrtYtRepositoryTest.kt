package ru.yandex.direct.core.entity.feed.repository

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isA
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.util.function.Consumer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.ytwrapper.client.YtProvider
import ru.yandex.inside.yt.kosher.Yt
import ru.yandex.inside.yt.kosher.cypress.YPath
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class BlrtYtRepositoryTest {
    @Autowired
    private lateinit var blrtYtRepository: BlrtYtRepository

    @Autowired
    private lateinit var ytProvider: YtProvider

    private lateinit var yt: Yt

    private val pathCaptor = argumentCaptor<YPath>()

    @Before
    fun setUp() {
        yt = mock(defaultAnswer = RETURNS_DEEP_STUBS)
        doReturn(yt).whenever(ytProvider).get(any())
    }

    @Test
    fun getDynamicTasksBusinessIdsAndShopIds() {
        blrtYtRepository.getDynamicTasksBusinessIdsAndShopIds()

        checkPath("<\"columns\"=[\"BusinessID\";\"ShopID\"]>//home/blrt/task/dyn/FeedToTasks")
    }

    @Test
    fun getPerformanceTasksBusinessIdsAndShopIds() {
        blrtYtRepository.getPerformanceTasksBusinessIdsAndShopIds()

        checkPath("<\"columns\"=[\"BusinessID\";\"ShopID\"]>//home/blrt/task/perf/FeedToTasks")
    }

    private fun checkPath(expectedPath: String) {
        verify(yt.tables()).read(pathCaptor.capture(), eq(YTableEntryTypes.YSON), isA<Consumer<YTreeMapNode>>())
        val path = pathCaptor.lastValue.toString()
        assertThat(path).isEqualTo(expectedPath)
    }
}
