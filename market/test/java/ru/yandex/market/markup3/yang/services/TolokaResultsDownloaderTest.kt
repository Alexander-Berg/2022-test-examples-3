package ru.yandex.market.markup3.yang.services

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.util.ReflectionTestUtils
import ru.yandex.market.markup3.core.TolokaSource
import ru.yandex.market.markup3.testutils.CommonTaskTest
import ru.yandex.market.markup3.yang.TolokaClientMock
import ru.yandex.market.markup3.yang.dto.TolokaPoolInfo
import ru.yandex.market.markup3.yang.repositories.TolokaOperationRepository
import ru.yandex.toloka.client.v1.pool.Pool
import ru.yandex.toloka.client.v1.pool.dynamicoverlap.BasicDynamicOverlapConfig
import java.math.BigDecimal
import java.util.Date

class TolokaResultsDownloaderTest : CommonTaskTest() {

    @Autowired
    lateinit var tolokaClientMock: TolokaClientMock

    @Autowired
    lateinit var yangResultsDownloader: TolokaResultsDownloader

    @Autowired
    lateinit var tolokaOperationRepository: TolokaOperationRepository

    @Test
    fun testDynamicDownloadWithOperations() {
        val pool = Pool(
            "prj",
            "pr_name",
            true,
            Date(),
            BigDecimal.ONE,
            1,
            true,
            null
        )
        val basicDynamicOverlapConfig = BasicDynamicOverlapConfig()
        basicDynamicOverlapConfig.answerWeightSkillId = "1"
        basicDynamicOverlapConfig.fields = listOf()
        ReflectionTestUtils.setField(pool, "dynamicOverlapConfig", basicDynamicOverlapConfig)
        tolokaClientMock.createPool(pool).result

        tolokaClientMock.setPoolClosed(pool.id)

        tolokaPoolInfoRepository.insert(
            TolokaPoolInfo(
                source = TolokaSource.TOLOKA,
                poolGroupId = "group id",
                externalPoolId = pool.id,
                dynamic = true,
            )
        )

        //start operation
        yangResultsDownloader.downloadAllDynamicPools()
        var operations = tolokaOperationRepository.findAll()
        operations shouldHaveSize 1
        operations[0].externalPoolId shouldBe pool.id
        operations[0].finished shouldBe null

        var poolInfo = getOnlyOnePool()
        poolInfo.fullyDownloaded shouldBe false

        //not finished
        yangResultsDownloader.downloadAllDynamicPools()
        poolInfo = getOnlyOnePool()
        tolokaOperationRepository.findAll()[0].finished shouldBe null
        poolInfo.fullyDownloaded shouldBe false

        tolokaClientMock.finishAllOperations()

        //finished operation
        yangResultsDownloader.downloadAllDynamicPools()
        poolInfo = getOnlyOnePool()
        poolInfo.fullyDownloaded shouldBe true

        operations = tolokaOperationRepository.findAll()
        operations shouldHaveSize 1
        operations[0].externalPoolId shouldBe pool.id
        operations[0].finished shouldNotBe null
    }
}
