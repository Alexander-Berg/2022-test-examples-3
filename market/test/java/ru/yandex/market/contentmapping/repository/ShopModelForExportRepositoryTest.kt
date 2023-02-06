package ru.yandex.market.contentmapping.repository

import io.kotest.assertions.asClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.assertj.core.api.Assertions
import org.jeasy.random.EasyRandom
import org.jeasy.random.EasyRandomParameters
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import ru.yandex.market.contentmapping.dto.model.Shop
import ru.yandex.market.contentmapping.dto.model.ShopModel
import ru.yandex.market.contentmapping.dto.model.ShopModelForExport
import ru.yandex.market.contentmapping.testutils.BaseAppTestClass
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.streams.toList


@Transactional(propagation = NOT_SUPPORTED)
class ShopModelForExportRepositoryTest : BaseAppTestClass() {

    @Autowired
    lateinit var shopRepository: ShopRepository

    @Autowired
    lateinit var shopModelRepository: ShopModelRepository

    @Autowired
    lateinit var shopModelForExportRepository: ShopModelForExportRepository

    @Autowired
    lateinit var transactionTemplate: TransactionTemplate

    private val random = EasyRandom(EasyRandomParameters()
            .excludeType { it.isAssignableFrom(Lazy::class.java) }
            .seed(42)
    )

    private lateinit var shop: Shop
    private lateinit var models: List<ShopModel>

    @Before
    fun setup() {
        clear()
        shop = shopRepository.insert(Shop(100, "The shop"))
        models = shopModelRepository.insertOrUpdateAll(
                random.objects(ShopModel::class.java, 10).map { it.copy(shopId = shop.id) }.toList())
        models.forEach {
            shopModelForExportRepository.insert(ShopModelForExport(it.id, it.shopId))
        }
    }

    @After
    fun clear() {
        shopModelForExportRepository.deleteAll()
        shopModelRepository.deleteAll()
        shopRepository.deleteAll()
    }

    @Test
    fun `test lock behavior`() {
        val limit = 3;
        var modelIds = shopModelForExportRepository.loadModelsForExport(limit)
        Assertions.assertThat(modelIds).hasSize(limit)

        //check no locking
        modelIds = shopModelForExportRepository.loadModelsForExport(models.size + 1)
        Assertions.assertThat(modelIds).hasSize(models.size)

        //check retrieval order equals to insertion
        for (i in modelIds.indices) {
            Assertions.assertThat(modelIds[i]).isEqualTo(models[i].id)
        }
    }

    @Test
    fun `test concurrent lock behavior`() {
        transactionTemplate.propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRED
        val es: ExecutorService = Executors.newFixedThreadPool(2)
        val startLatch = CountDownLatch(1)
        val stopLatch = CountDownLatch(1)

        //lock 5 items in first transaction
        val limit1 = 5
        val f1 = es.submit {
            transactionTemplate.execute {
                val shopModelIds : List<Long> = shopModelForExportRepository.loadModelsForExport(limit1)
                startLatch.countDown()
                Assertions.assertThat(shopModelIds).hasSize(limit1)

                //check retrieval order equals to insertion
                for (i in shopModelIds.indices) {
                    Assertions.assertThat(shopModelIds[i]).isEqualTo(models[i].id)
                }
                stopLatch.await()
            }
        }

        //lock remaining 5 items in second transaction
        val limit2 = models.size - limit1 + 1
        val f2 = es.submit {
            transactionTemplate.execute {
                startLatch.await()
                val shopModelIds : List<Long> = shopModelForExportRepository.loadModelsForExport(limit2)
                Assertions.assertThat(shopModelIds).hasSize(models.size - limit1)

                //check retrieval order equals to insertion
                for (i in shopModelIds.indices) {
                    Assertions.assertThat(shopModelIds[i]).isEqualTo(models[limit1 + i].id)
                }
                stopLatch.countDown()
            }
        }

        f1.get()
        f2.get()

        //check that all items are available after transactions finished
        val limit3 = models.size + 1
        val shopModelIds : List<Long> = shopModelForExportRepository.loadModelsForExport(limit3)
        Assertions.assertThat(shopModelIds).hasSize(models.size)

        //check retrieval order equals to insertion
        for (i in shopModelIds.indices) {
            Assertions.assertThat(shopModelIds[i]).isEqualTo(models[i].id)
        }
    }

    @Test
    fun `test retries count reset on update`() {
        val modelsExports = shopModelForExportRepository.findAll()
        modelsExports.filter { it.retriesCount > 0 } shouldHaveSize 0
        shopModelForExportRepository.updateRetriesCount(modelsExports.map { it.shopModelId })
        val modelsExportsNew = shopModelForExportRepository.findAll()
        modelsExportsNew.filter { it.retriesCount == 0 } shouldHaveSize 0
        modelsExportsNew.filter { it.retriesCount == 1 } shouldHaveSize modelsExportsNew.size
    }

    @Test
    fun `it should calculate stat and don't die`() {
        val stat = shopModelForExportRepository.getQueueStat()
        stat.asClue {
            it.totalCnt shouldBe 10
            it.old shouldBe 0
        }
    }
}
