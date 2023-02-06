package ru.yandex.market.contentmapping.services

import io.kotest.assertions.asClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.transaction.support.TransactionTemplate
import ru.yandex.market.contentmapping.dto.model.ShopModelSaveContext
import ru.yandex.market.contentmapping.dto.model.ShopModelSaveContext.Companion.saveContextWithSource
import ru.yandex.market.contentmapping.repository.ShopModelLightRepository
import ru.yandex.market.contentmapping.repository.ShopModelRepository
import ru.yandex.market.contentmapping.repository.ShopModelStatisticsQueueRepository
import ru.yandex.market.contentmapping.services.model_statistics.ShopModelStatisticsQueueService
import ru.yandex.market.contentmapping.testdata.TestDataUtils.testShopModel
import ru.yandex.market.mbo.solomon.SolomonPushService
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper
import java.time.LocalDateTime

class ShopModelServiceTest {
    private lateinit var repository: ShopModelRepository

    private lateinit var shopModelService: ShopModelService

    private lateinit var shopModelStatisticsQueueRepository: ShopModelStatisticsQueueRepository

    private lateinit var shopModelStatisticsQueueService: ShopModelStatisticsQueueService

    @Before
    fun init() {
        repository = Mockito.mock(ShopModelRepository::class.java)
        shopModelStatisticsQueueRepository = ShopModelStatisticsQueueRepository(
                Mockito.mock(NamedParameterJdbcTemplate::class.java),
                Mockito.mock(TransactionTemplate::class.java)
        )
        shopModelStatisticsQueueService = ShopModelStatisticsQueueService(
            shopModelStatisticsQueueRepository, Mockito.mock(SolomonPushService::class.java))
        shopModelService = ShopModelService(
                repository,
                Mockito.mock(ShopModelLightRepository::class.java),
                shopModelStatisticsQueueService,
                TransactionHelper.MOCK,
        )

        Mockito.`when`(repository.insertOrUpdateByShopSku(Mockito.anyList())).then { it.arguments[0] }
        Mockito.`when`(repository.updateBatch(Mockito.anyList())).then { it.arguments[0] }
    }

    @Test
    fun `save new updated dates`() {
        val model = testShopModel(id = 0)
        val context = saveContextWithSource(ShopModelSaveContext.Source.CT)
        val saved = shopModelService.saveAndUpdateStatistics(model, context)
        saved.created.shouldNotBeNull()
        saved.updated.shouldNotBeNull()
        Mockito.verify(repository, Mockito.times(1))?.insertOrUpdateByShopSku(listOf(saved))
    }

    @Test
    fun saveExistent() {
        val createdTime = LocalDateTime.MIN
        val model = testShopModel().copy(created = createdTime)
        val context = saveContextWithSource(ShopModelSaveContext.Source.CT)
        val saved = shopModelService.saveAndUpdateStatistics(model, context)
        createdTime shouldBe model.created
        model.updated.shouldNotBeNull()
        Mockito.verify(repository, Mockito.times(1)).insertOrUpdateByShopSku(listOf(saved))
    }

    @Test
    fun saveAllNew() {
        val model1 = testShopModel(
                id = 0,
                shopSku = "1",
        )
        val model2 = testShopModel(
                id = 0,
                shopSku = "2",
        )
        val context = saveContextWithSource(ShopModelSaveContext.Source.CT)
        val saved = shopModelService.saveAllAndUpdateStatistics(listOf(model1, model2), context)
        model1.updated.shouldNotBeNull()
        model2.updated.shouldNotBeNull()
        model1.created.shouldNotBeNull()
        model2.created.shouldNotBeNull()
        Mockito.verify(repository, Mockito.times(1))?.insertOrUpdateByShopSku(saved)
    }

    @Test
    fun `save all existing models should call updateBatch and not change created time`() {
        val createdTime = LocalDateTime.MIN
        val model1 = testShopModel(id = 1, shopSku = "1").copy(created = createdTime)
        val model2 = testShopModel(id = 2, shopSku = "2").copy(created = createdTime)

        val context = saveContextWithSource(ShopModelSaveContext.Source.CT)
        val saved = shopModelService.saveAllAndUpdateStatistics(listOf(model1, model2), context)

        model1.updated.shouldNotBeNull()
        model2.updated.shouldNotBeNull()
        model1.created shouldBe createdTime
        model2.created shouldBe createdTime

        Mockito.verify(repository, Mockito.times(1)).insertOrUpdateByShopSku(saved)
    }

    @Test
    fun `save some new some existing should use insertOrUpdateByShopSku`() {
        val createdTime = LocalDateTime.MIN
        val newModel = testShopModel(id = 0, shopSku = "1")
        val existentModel = testShopModel(id = 2, shopSku = "2").copy(created = createdTime)
        val models = listOf(newModel, existentModel)
        val context = saveContextWithSource(ShopModelSaveContext.Source.CT)
        val saved = shopModelService.saveAllAndUpdateStatistics(models, context)

        newModel.asClue {
            it.updated.shouldNotBeNull()
            it.created.shouldNotBeNull()
        }

        existentModel.asClue {
            it.updated.shouldNotBeNull()
            it.created shouldBe createdTime
        }

        Mockito.verify(repository, Mockito.times(1)).insertOrUpdateByShopSku(saved)
    }

    @Test
    fun fillExportInfo() {
        val createdTime = LocalDateTime.MIN
        val updatedTime = LocalDateTime.MAX
        val exportTime = LocalDateTime.now()
        val ticketId: Long = 5
        val model = testShopModel(id = 2).copy(
                created = createdTime,
                updated = updatedTime,
        )
        val updated = shopModelService.fillExportInfo(model, exportTime, ticketId)
        updated.asClue {
            it.created shouldBe createdTime
            it.updated shouldBe updatedTime
            it.exported shouldBe exportTime
            it.exportTicketId!!.toLong() shouldBe ticketId
        }
        Mockito.verify(repository, Mockito.times(1)).updateExportInfoAndResetProcessingStatus(updated)
    }
}
