package ru.yandex.market.markup3.mboc.moderation.generator

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.spy
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.markup3.api.Markup3ApiService
import ru.yandex.market.markup3.mboc.moderation.repository.TolokaActiveMappingModerationRepository
import ru.yandex.market.markup3.testutils.CommonTaskTest
import ru.yandex.market.mbo.storage.StorageKeyValueService
import ru.yandex.market.mboc.http.MboCategory
import ru.yandex.market.mboc.http.MboCategoryService

class TolokaMappingModerationGeneratorTest : CommonTaskTest() {

    @Autowired
    private lateinit var storageKeyValueService: StorageKeyValueService

    @Autowired
    private lateinit var tolokaActiveMappingModerationRepository: TolokaActiveMappingModerationRepository

    @Autowired
    private lateinit var mboCategoryService: MboCategoryService

    @Autowired
    private lateinit var markup3ApiService: Markup3ApiService

    private lateinit var tolokaMappingModerationGenerator: TolokaMappingModerationGenerator

    @Before
    fun setUp() {
        mboCategoryService = spy(mboCategoryService)
        tolokaMappingModerationGenerator = TolokaMappingModerationGenerator(
            mboCategoryService,
            taskGroupRegistry,
            storageKeyValueService,
            tolokaActiveMappingModerationRepository,
            TolokaMappingModerationGenerator.Constants(
                offersPerSupplier = 1,
                offersPerCategory = 1,
                taskLimitForToloka = 5,
                taskSize = 10,
                minTaskSize = 1,
            ),
            markup3ApiService
        )
    }

    @Test
    fun testGenerateNothing() {
        val response = getTaskOffersResponse(listOf(1,2,3,4))
        doReturn(response).`when`(mboCategoryService).getTaskOffers(any())
        tolokaMappingModerationGenerator.generate()

        taskDbService.findAll() shouldHaveSize 0
        tolokaActiveMappingModerationRepository.getActiveOfferIds() shouldHaveSize 0
    }

    @Test
    fun testGenerateOneTask() {
        val response = getTaskOffersResponse(nextRange(12))
        doReturn(response).`when`(mboCategoryService).getTaskOffers(any())
        tolokaMappingModerationGenerator.generate()

        taskDbService.findAll() shouldHaveSize 1
        tolokaActiveMappingModerationRepository.getActiveOfferIds() shouldHaveSize 10
    }

    @Test
    fun testNoGenerateDoubleCall() {
        val response = getTaskOffersResponse(nextRange(12))
        doReturn(response).`when`(mboCategoryService).getTaskOffers(any())
        tolokaMappingModerationGenerator.generate()
        tolokaMappingModerationGenerator.generate()

        taskDbService.findAll() shouldHaveSize 1
        tolokaActiveMappingModerationRepository.getActiveOfferIds() shouldHaveSize 10
    }

    @Test
    fun testGenerateNewOffers() {
        val response = getTaskOffersResponse(nextRange(12))
        doReturn(response).`when`(mboCategoryService).getTaskOffers(any())
        tolokaMappingModerationGenerator.generate()

        taskDbService.findAll() shouldHaveSize 1
        tolokaActiveMappingModerationRepository.getActiveOfferIds() shouldHaveSize 10

        val response2 = getTaskOffersResponse(nextRange(12))
        doReturn(response2).`when`(mboCategoryService).getTaskOffers(any())
        tolokaMappingModerationGenerator.generate()

        taskDbService.findAll() shouldHaveSize 2
        tolokaActiveMappingModerationRepository.getActiveOfferIds() shouldHaveSize 20
    }

    @Test
    fun `test ignore failed unique keys`() {
        val duplicatedIds = nextRange(10)
        val ids1 = nextRange(10)
        val response = getTaskOffersResponse(duplicatedIds + ids1)
        doReturn(response).`when`(mboCategoryService).getTaskOffers(any())
        tolokaMappingModerationGenerator.generate()

        val uniqKeys = taskDbService.findAll()
            .flatMap { taskDbService.findUniqKeys(it.id) }
            .toSet()
        val all1 = duplicatedIds.union(ids1).map { "$it.0" }
        uniqKeys shouldContainExactlyInAnyOrder all1

        taskDbService.findAll() shouldHaveSize 2
        tolokaActiveMappingModerationRepository.getActiveOfferIds() shouldHaveSize 20

        tolokaActiveMappingModerationRepository.deleteByOfferIds(duplicatedIds)

        val ids2 = nextRange(10)
        val response2 = getTaskOffersResponse(duplicatedIds + ids2)
        doReturn(response2).`when`(mboCategoryService).getTaskOffers(any())
        tolokaMappingModerationGenerator.generate()

        val uniqKeys1 = taskDbService.findAll()
            .flatMap { taskDbService.findUniqKeys(it.id) }
            .toSet()
        val all2 = duplicatedIds.union(ids1).union(ids2).map { "$it.0" }
        uniqKeys1 shouldContainExactlyInAnyOrder all2
    }

    @Test
    fun testGenerateTaskLimit() {
        val response = getTaskOffersResponse(nextRange(35))
        doReturn(response).`when`(mboCategoryService).getTaskOffers(any())
        tolokaMappingModerationGenerator.generate()

        taskDbService.findAll() shouldHaveSize 3
        tolokaActiveMappingModerationRepository.getActiveOfferIds() shouldHaveSize 30

        val response2 = getTaskOffersResponse(nextRange(35))
        doReturn(response2).`when`(mboCategoryService).getTaskOffers(any())
        tolokaMappingModerationGenerator.generate()

        taskDbService.findAll() shouldHaveSize 5
        tolokaActiveMappingModerationRepository.getActiveOfferIds() shouldHaveSize 50
    }

    private var previousRangeValue = 1L
    private fun nextRange(count: Long): List<Long> {
        return (previousRangeValue until previousRangeValue + count).toList().also { previousRangeValue += count }
    }

    private fun getTaskOffersResponse(ids: List<Long>) =
        MboCategory.GetTaskOffersResponse.newBuilder().apply {
            val taskOffers = ids.map { id ->
                MboCategory.GetTaskOffersResponse.TaskOffer.newBuilder().apply {
                    offerId = id
                    processingCounter = 0
                    categoryId = 0
                    businessId = 1
                }.build()
            }
            addAllOffers(taskOffers)
        }.build()
}
