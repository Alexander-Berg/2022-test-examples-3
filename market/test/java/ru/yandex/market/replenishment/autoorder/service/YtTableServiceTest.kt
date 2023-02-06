package ru.yandex.market.replenishment.autoorder.service

import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito
import ru.yandex.market.replenishment.autoorder.service.yt.YtCluster
import ru.yandex.market.replenishment.autoorder.service.yt.YtFactory
import ru.yandex.market.replenishment.autoorder.service.yt.YtTableService

class YtTableServiceTest {

    private lateinit var ytFactory: YtFactory

    @Test
    fun removeEndSlash_isOk() {
        ytFactory = Mockito.mock(YtFactory::class.java)
        val ytTableService: YtTableService = YtTableService(ytFactory, YtCluster.HAHN)
        val actualPath: String = YtTableService.removeEndSlash("//aaa/bbb/")
        Assert.assertEquals("//aaa/bbb", actualPath)
    }
}
