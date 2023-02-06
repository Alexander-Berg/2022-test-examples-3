package ru.yandex.market.mbi.feed.processor.feed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client
import ru.yandex.market.mbi.feed.processor.FunctionalTest
import java.net.URL

internal class ProxyStorageServiceTest : FunctionalTest() {

    @Autowired
    private lateinit var tempStorageService: TempStorageService

    @Autowired
    private lateinit var mdsS3Client: MdsS3Client

    @Test
    fun smoke() {
        mockMdsGetUrl()
        val url = "http://www.yandex.ru/c"
        val urlMds = tempStorageService.createTempUrl(url)
        assertThat(url).isNotEqualTo(urlMds)
    }

    private fun mockMdsGetUrl() {
        doReturn(URL("http://www.yandex.ru/proxy"))
            .`when`(mdsS3Client)
            .getUrl(any())
    }
}
