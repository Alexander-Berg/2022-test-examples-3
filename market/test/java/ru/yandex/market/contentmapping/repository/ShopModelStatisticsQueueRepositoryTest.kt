package ru.yandex.market.contentmapping.repository

import io.kotest.assertions.asClue
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
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
import ru.yandex.market.contentmapping.dto.model.ShopModelForQueue
import ru.yandex.market.contentmapping.kotlin.typealiases.ModelId
import ru.yandex.market.contentmapping.kotlin.typealiases.ShopId
import ru.yandex.market.contentmapping.repository.ShopModelStatisticsQueueRepository.StatisticsQueueStat
import ru.yandex.market.contentmapping.testutils.BaseAppTestClass
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.streams.toList


@Transactional(propagation = NOT_SUPPORTED)
class ShopModelStatisticsQueueRepositoryTest : BaseAppTestClass() {

    @Autowired
    lateinit var shopRepository: ShopRepository

    @Autowired
    lateinit var shopModelRepository: ShopModelRepository

    @Autowired
    lateinit var shopModelStatisticsQueueRepository: ShopModelStatisticsQueueRepository

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
                random.objects(ShopModel::class.java, 10)
                        .map { it.copy(shopId = shop.id) }
                        .toList()
        )
        models.forEach {
            shopModelStatisticsQueueRepository.insert(ShopModelForQueue(it.id, it.shopId))
        }
    }

    @After
    fun clear() {
        shopModelStatisticsQueueRepository.deleteAll()
        shopModelRepository.deleteAll()
        shopRepository.deleteAll()
    }

    @Test
    fun `test lock behavior`() {
        val limit = 3
        var modelIdsByShopId : Map<ShopId, List<ModelId>> = shopModelStatisticsQueueRepository.getFromQueueWithTransactionLock(limit)
        modelIdsByShopId shouldHaveSize 1
        modelIdsByShopId[shop.id]?.size shouldBe limit

        //check no locking
        modelIdsByShopId = shopModelStatisticsQueueRepository.getFromQueueWithTransactionLock(models.size + 1)
        modelIdsByShopId shouldHaveSize 1
        modelIdsByShopId[shop.id]?.size shouldBe models.size

        //check retrieval order equals to insertion
        modelIdsByShopId[shop.id]?.let {
            for (i in it.indices) {
                modelIdsByShopId[shop.id]?.get(i) shouldBe models[i].id
            }
        }
    }

    @Test
    fun `test concurent lock behavior`() {
        transactionTemplate.propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRED
        val es: ExecutorService = Executors.newFixedThreadPool(2)
        val startLatch = CountDownLatch(1)
        val stopLatch = CountDownLatch(1)

        //lock 5 items in first transaction
        val limit1 = 5
        val f1 = es.submit {
            transactionTemplate.execute {
                val modelIdsByShopId : Map<ShopId, List<ModelId>> = shopModelStatisticsQueueRepository.getFromQueueWithTransactionLock(limit1)
                startLatch.countDown()
                modelIdsByShopId shouldHaveSize 1
                modelIdsByShopId[shop.id]?.size shouldBe limit1

                //check retrieval order equals to insertion
                modelIdsByShopId[shop.id]?.let {
                    for (i in it.indices) {
                        modelIdsByShopId[shop.id]?.get(i) shouldBe models[i].id
                    }
                }
                stopLatch.await()
            }
        }

        //lock remaining 5 items in second transaction
        val limit2 = models.size - limit1 + 1
        val f2 = es.submit {
            transactionTemplate.execute {
                startLatch.await()
                val modelIdsByShopId : Map<ShopId, List<ModelId>> = shopModelStatisticsQueueRepository.getFromQueueWithTransactionLock(limit2)
                modelIdsByShopId shouldHaveSize 1
                modelIdsByShopId[shop.id]?.size shouldBe models.size - limit1

                //check retrieval order equals to insertion
                modelIdsByShopId[shop.id]?.let {
                    for (i in it.indices) {
                        modelIdsByShopId[shop.id]?.get(i) shouldBe models[limit1 + i].id
                    }
                }
                stopLatch.countDown()
            }
        }

        f1.get()
        f2.get()

        //check that all items are available after transactions finished
        val limit3 = models.size + 1
        val modelIdsByShopId : Map<ShopId, List<ModelId>> = shopModelStatisticsQueueRepository.getFromQueueWithTransactionLock(limit3)
        modelIdsByShopId shouldHaveSize 1
        modelIdsByShopId[shop.id]?.size shouldBe models.size

        //check retrieval order equals to insertion
        modelIdsByShopId[shop.id]?.let {
            for (i in it.indices) {
                modelIdsByShopId[shop.id]?.get(i) shouldBe models[i].id
            }
        }
    }

    @Test
    fun `it should calculate stat and don't die`() {
        val stat: StatisticsQueueStat = shopModelStatisticsQueueRepository.getQueueStat()
        stat.asClue {
            it.totalCnt shouldBe 10
        }
    }
}
