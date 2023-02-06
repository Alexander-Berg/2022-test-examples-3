package ru.yandex.market.contentmapping.repository

import io.kotest.matchers.shouldBe
import org.assertj.core.api.Assertions
import org.jeasy.random.EasyRandom
import org.jeasy.random.EasyRandomParameters
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.contentmapping.dto.model.Shop
import ru.yandex.market.contentmapping.dto.model.ShopModel
import ru.yandex.market.contentmapping.testutils.BaseAppTestClass
import ru.yandex.market.mbo.lightmapper.criteria.FieldEqCriteria
import kotlin.streams.toList

class ShopModelLightRepositoryTest : BaseAppTestClass() {
    @Autowired
    lateinit var shopRepository: ShopRepository

    @Autowired
    lateinit var shopModelLightRepository: ShopModelLightRepository

    @Autowired
    lateinit var shopModelRepository: ShopModelRepository

    private val random = EasyRandom(EasyRandomParameters()
            .excludeType { it.isAssignableFrom(Lazy::class.java) }
            .seed(42)
    )

    @Test
    fun `test query doesn't fail`() {
        val shop = shopRepository.insert(Shop(100, "The shop"))
        val models = shopModelRepository.insertOrUpdateAll(
                random.objects(ShopModel::class.java, 100).map { it.copy(shopId = shop.id) }.toList())

        shopModelLightRepository.findLightModelsSeqBy(
                FieldEqCriteria.of(shop.id, "shop_id", ShopModel::shopId)) { seq ->
            val lightModels = seq.toList()
            Assertions.assertThat(lightModels).hasSize(100)

            val byId = lightModels.associateBy { it.id }
            byId[models[0].id]!!.isValid shouldBe false
            byId[models[1].id]!!.isValid shouldBe false
        }
    }
}
