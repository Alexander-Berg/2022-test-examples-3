package ru.yandex.market.contentmapping.repository

import io.kotest.assertions.asClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import ru.yandex.market.contentmapping.dto.model.CategoryRestriction
import ru.yandex.market.contentmapping.dto.model.CategoryRestrictionType
import ru.yandex.market.contentmapping.dto.model.ShopModel
import ru.yandex.market.contentmapping.kotlin.typealiases.ShopSku
import ru.yandex.market.contentmapping.testdata.TestDataUtils
import ru.yandex.market.contentmapping.testutils.BaseAppTestClass
import ru.yandex.market.mbo.lightmapper.criteria.FieldEqCriteria
import java.util.*
import kotlin.random.Random

class ShopModelRepositoryTest : BaseAppTestClass() {
    @Autowired
    lateinit var shopModelRepository: ShopModelRepository

    @Autowired
    lateinit var shopRepository: ShopRepository

    @Autowired
    lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    @Before
    fun setup() {
        shopRepository.insert(TestDataUtils.testShop())
    }

    @Test
    fun `test it can load empty row (we don't fail on default or legacy values)`() {
        jdbcTemplate.update(
            "insert into ${ShopModelRepository.TABLE_NAME} " +
                    "(shop_id, shop_sku, name, pictures, shop_values, market_values) " +
                    "values (${TestDataUtils.TEST_SHOP_ID}, 'sku', 'test model', '[]'::jsonb, '{}'::jsonb, '{}'::jsonb)",
            MapSqlParameterSource()
        )

        val models = shopModelRepository.findAll()
        models shouldHaveSize 1
        models[0].asClue {
            it.shopSku shouldBe "sku"
            it.categoryRestriction shouldBe CategoryRestriction.ALLOW_ANY
        }
    }

    @Test
    fun `test category restrictions are saved and loaded`() {
        val categoryRestriction = CategoryRestriction(CategoryRestrictionType.ALLOW_SINGLE, categoryId = 1L)
        shopModelRepository.insert(TestDataUtils.nextShopModel().copy(categoryRestriction = categoryRestriction))

        val models = shopModelRepository.findAll()
        models shouldHaveSize 1
        models[0].asClue {
            it.categoryRestriction shouldBe categoryRestriction
        }
    }

    @Test
    fun `test delete all shop models`() {
        jdbcTemplate.update("delete from cm.shop_models_deleted;", MapSqlParameterSource())
        val shopId = TestDataUtils.TEST_SHOP_ID
        val shopIdCriteria = FieldEqCriteria.of(shopId, "shop_id", ShopModel::shopId)
        var models = shopModelRepository.findBy(shopIdCriteria)
        models shouldHaveSize 0
        val n = 10
        for (i in 1..n) {
            shopModelRepository.insert(TestDataUtils.nextShopModel())
        }
        models = shopModelRepository.findBy(shopIdCriteria)
        models shouldHaveSize n
        val nDeleted = shopModelRepository.delete(shopId)
        nDeleted shouldBeExactly n
        models = shopModelRepository.findBy(shopIdCriteria)
        models shouldHaveSize 0

        //language=postgresql
        val sql = "select count(*) from cm.shop_models_deleted smd where (smd.row).shop_id = :shop_id"
        val savedDeletedCount = jdbcTemplate.queryForObject(sql, mapOf("shop_id" to shopId), Int::class.java)
        savedDeletedCount!! shouldBeExactly nDeleted
    }

    @Test
    fun `test delete models`() {
        val shopId = TestDataUtils.TEST_SHOP_ID
        val shopIdCriteria = FieldEqCriteria.of(shopId, "shop_id", ShopModel::shopId)
        for (j in 1..100) {
            var models = shopModelRepository.findBy(shopIdCriteria)
            models shouldHaveSize 0
            val n = 10
            val nToDelete = Random.nextInt(0, n)
            val skus = HashSet<ShopSku>()

            for (i in 1..n) {
                val m = shopModelRepository.insert(TestDataUtils.nextShopModel())
                skus.add(m.shopSku)
            }
            skus shouldHaveSize n
            models = shopModelRepository.findBy(shopIdCriteria)
            models shouldHaveSize n

            val skusToRetain = TreeSet<ShopSku>()
            skusToRetain.addAll(skus.shuffled().drop(nToDelete))
            val skusToDelete = HashSet(skus - skusToRetain)
            nToDelete shouldBe skusToDelete.size
            skus.size shouldBe skusToRetain.size + skusToDelete.size

            val batchSize = Random.nextInt(3, n + 1)
            var nDeleted = 0
            var lastSku: ShopSku? = null
            skusToRetain.chunked(batchSize).forEach {
                nDeleted += shopModelRepository.delete(shopId, after = lastSku, before = it.first())
                nDeleted += shopModelRepository.delete(shopId, it, it.first(), it.last())
                lastSku = it.last()
            }
            if (lastSku != null) {
                nDeleted += shopModelRepository.delete(shopId, after = lastSku)
            }
            nDeleted shouldBeExactly skusToDelete.size

            models = shopModelRepository.findBy(shopIdCriteria)
            models shouldHaveSize skusToRetain.size
            models.map { it.shopSku }.toSet() shouldContainExactlyInAnyOrder skusToRetain
            shopModelRepository.delete(shopId)
        }
    }

    @Test
    fun `should store deleted models after delete`(){
        jdbcTemplate.update("delete from cm.shop_models_deleted;", MapSqlParameterSource())
        val shopId = TestDataUtils.TEST_SHOP_ID
        val shopIdCriteria = FieldEqCriteria.of(shopId, "shop_id", ShopModel::shopId)
        var totalDeleted: Int = 0;
        for (j in 1..100) {
            var models = shopModelRepository.findBy(shopIdCriteria)
            models shouldHaveSize 0
            val n = 10
            val nToDelete = Random.nextInt(0, n)
            val skus = HashSet<ShopSku>()

            for (i in 1..n) {
                val m = shopModelRepository.insert(TestDataUtils.nextShopModel())
                skus.add(m.shopSku)
            }
            skus shouldHaveSize n
            models = shopModelRepository.findBy(shopIdCriteria)
            models shouldHaveSize n

            val skusToRetain = TreeSet<ShopSku>()
            skusToRetain.addAll(skus.shuffled().drop(nToDelete))
            val skusToDelete = HashSet(skus - skusToRetain)
            nToDelete shouldBe skusToDelete.size
            skus.size shouldBe skusToRetain.size + skusToDelete.size

            val batchSize = Random.nextInt(3, n + 1)
            var nDeleted = 0
            var lastSku: ShopSku? = null
            skusToRetain.chunked(batchSize).forEach {
                nDeleted += shopModelRepository.delete(shopId, after = lastSku, before = it.first())
                nDeleted += shopModelRepository.delete(shopId, it, it.first(), it.last())
                lastSku = it.last()
            }
            if (lastSku != null) {
                nDeleted += shopModelRepository.delete(shopId, after = lastSku)
            }
            nDeleted shouldBeExactly skusToDelete.size
            totalDeleted+= nDeleted

            models = shopModelRepository.findBy(shopIdCriteria)
            models shouldHaveSize skusToRetain.size
            models.map { it.shopSku }.toSet() shouldContainExactlyInAnyOrder skusToRetain
            totalDeleted += shopModelRepository.delete(shopId)
        }

        //language=postgresql
        val sql = "select count(*) from cm.shop_models_deleted smd where (smd.row).shop_id = :shop_id"
        val savedDeletedCount = jdbcTemplate.queryForObject(sql, mapOf("shop_id" to shopId), Int::class.java)
        savedDeletedCount!! shouldBeExactly totalDeleted
    }

}
