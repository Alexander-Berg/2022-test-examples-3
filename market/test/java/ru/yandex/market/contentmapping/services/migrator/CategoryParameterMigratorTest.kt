package ru.yandex.market.contentmapping.services.migrator

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import ru.yandex.market.contentmapping.dto.mapping.MarketParam
import ru.yandex.market.contentmapping.dto.mapping.MarketParamValue
import ru.yandex.market.contentmapping.dto.mapping.ParamMapping
import ru.yandex.market.contentmapping.dto.mapping.ParamMappingRule
import ru.yandex.market.contentmapping.dto.mapping.ParamMappingType
import ru.yandex.market.contentmapping.dto.mapping.ShopParam
import ru.yandex.market.contentmapping.dto.model.ParametersMigration
import ru.yandex.market.contentmapping.dto.model.Shop
import ru.yandex.market.contentmapping.kotlin.typealiases.ParamId
import ru.yandex.market.contentmapping.modules.keyvalue.KeyValueService
import ru.yandex.market.contentmapping.repository.CategoryInfoRepository
import ru.yandex.market.contentmapping.repository.ParamMappingRepository
import ru.yandex.market.contentmapping.repository.ParamMappingRuleRepository
import ru.yandex.market.contentmapping.repository.ParametersMigrationRepository
import ru.yandex.market.contentmapping.repository.ShopRepository
import ru.yandex.market.contentmapping.services.VersionLockService
import ru.yandex.market.contentmapping.services.category.info.CategoryParameterMigrationLatchService
import ru.yandex.market.contentmapping.services.category.info.migration.CategoryParameterMigrationInfo
import ru.yandex.market.contentmapping.services.category.migrators.CategoryParametersMigrator
import ru.yandex.market.contentmapping.services.category.migrators.CategoryParametersMigratorImpl
import ru.yandex.market.contentmapping.services.category.migrators.ParamMappingDiffMigrator
import ru.yandex.market.contentmapping.services.shop.ShopService
import ru.yandex.market.contentmapping.testutils.BaseAppTestClass

@Transactional
class CategoryParameterMigratorTest : BaseAppTestClass() {
    //region beans
    @Autowired
    lateinit var paramMappingRepository: ParamMappingRepository

    @Autowired
    lateinit var paramMappingRuleRepository: ParamMappingRuleRepository

    @Autowired
    lateinit var parametersMigrationRepository: ParametersMigrationRepository

    @Autowired
    lateinit var shopRepository: ShopRepository

    lateinit var categoryParameterMigrator: CategoryParametersMigrator

    @Autowired
    lateinit var keyValueService: KeyValueService

    @Autowired
    lateinit var categoryInfoRepository: CategoryInfoRepository

    @Autowired
    lateinit var mappingVersionLockService: VersionLockService

    lateinit var shopServiceMock: ShopService
    //endregion

    //region initialize test data
    private val shopId1 = 123L
    private val shopId2 = 12345L
    private val businessId1 = 232323L
    private val businessId2 = 364462L

    private val categoryId = 99595L

    private val shopParam1 = ShopParam("delivery_weight", null)
    private val shopParam2 = ShopParam("Название модели*", null)
    private val shopParam3 = ShopParam("Бренд*", null)

    private val marketParam1 = MarketParam(10732698)
    private val marketParam2 = MarketParam(7351757)
    private val marketParam3 = MarketParam(7893318)

    private val paramMapping1 = ParamMapping(
        id = 1,
        shopId = shopId1,
        mappingType = ParamMappingType.MAPPING,
        shopParams = listOf(shopParam1),
        marketParams = listOf(marketParam1),
        isHypothesis = true,
        categoryId = null,
        isDeleted = false,
        rank = 0
    )
    private val paramMapping2 = ParamMapping(
        id = 2,
        shopId = shopId2,
        mappingType = ParamMappingType.MAPPING,
        shopParams = listOf(shopParam2),
        marketParams = listOf(marketParam2),
        isHypothesis = false,
        categoryId = null,
        isDeleted = false,
        rank = 0
    )
    private val paramMapping3 = ParamMapping(
        3,
        shopId1,
        ParamMappingType.MAPPING,
        listOf(shopParam3),
        listOf(marketParam3),
        isHypothesis = false,
        categoryId = null,
        isDeleted = false,
        rank = 0
    )

    private val shopValues1 = mapOf(shopParam1.name to "1.589")
    private val shopValues2 = mapOf(shopParam2.name to "Тестовая модель")
    private val shopValues3 = mapOf(shopParam3.name to "Testico")

    private val marketValues1: Map<ParamId, Set<MarketParamValue>> = mapOf(
        marketParam1.parameterId to setOf(
            MarketParamValue.NumericValue(1.589)
        )
    )

    private val marketValues2: Map<ParamId, Set<MarketParamValue>> = mapOf(
        marketParam2.parameterId to setOf(
            MarketParamValue.StringValue("Тестовая модель с маркета")
        )
    )

    private val marketValues3: Map<ParamId, Set<MarketParamValue>> = mapOf(
        marketParam3.parameterId to setOf(
            MarketParamValue.StringValue("Testico_from_market")
        )
    )

    private val paramMappingRule1 = ParamMappingRule(
        id = 1,
        paramMappingId = paramMapping1.id,
        shopValues = shopValues1,
        marketValues = marketValues1,
        isHypothesis = true,
        isDeleted = false
    )

    private val paramMappingRule2 = ParamMappingRule(
        id = 2,
        paramMappingId = paramMapping2.id,
        shopValues = shopValues2,
        marketValues = marketValues2,
        isHypothesis = false,
        isDeleted = false
    )

    private val paramMappingRule3 = ParamMappingRule(
        id = 3,
        paramMappingId = paramMapping3.id,
        shopValues = shopValues3,
        marketValues = marketValues3,
        isHypothesis = false,
        isDeleted = false
    )

    private val paramMappingRule4 = ParamMappingRule(
        id = 4,
        paramMappingId = paramMapping1.id,
        shopValues = shopValues3,
        marketValues = marketValues3,
        isHypothesis = true,
        isDeleted = false
    )

    //endregion

    @Before
    fun setup() {
        val categoryParameterMigrationLatchService = Mockito.mock(CategoryParameterMigrationLatchService::class.java)
        Mockito.`when`(categoryParameterMigrationLatchService.isMigrationEnabled()).thenReturn(true)
        val paramMappingDiffMigrator = ParamMappingDiffMigrator(
            shopRepository,
            paramMappingRepository,
            paramMappingRuleRepository,
            mappingVersionLockService
        )

        categoryParameterMigrator = CategoryParametersMigratorImpl(
            parametersMigrationRepository,
            listOf(paramMappingDiffMigrator),
            categoryInfoRepository,
            categoryParameterMigrationLatchService
        )

        shopRepository.insert(Shop(shopId1, "shop1"))
        shopRepository.insert(Shop(shopId2, "shop2"))

        Mockito.`when`(categoryInfoRepository.getAllLeafSubtreeCategoryIds(Mockito.anyLong()))
            .thenReturn(listOf(categoryId))

        shopServiceMock = mock {
            doReturn(Shop(id = shopId1, name = "Shop1", businessId = businessId1))
                .`when`(it).findById(eq(shopId1))
            doReturn(Shop(id = shopId2, name = "Shop2", businessId = businessId2))
                .`when`(it).findById(eq(shopId2))
        }
        paramMappingRepository.insert(paramMapping1)
        paramMappingRepository.insert(paramMapping2)
        paramMappingRepository.insert(paramMapping3)
        paramMappingRuleRepository.insert(paramMappingRule1)
        paramMappingRuleRepository.insert(paramMappingRule2)
        paramMappingRuleRepository.insert(paramMappingRule3)
        paramMappingRuleRepository.insert(paramMappingRule4)

        keyValueService["param_migration_enabled"] = true
    }

    @Test
    fun `can migrate`() {
        val migrationInfo = CategoryParameterMigrationInfo(
            mapOf(
                marketParam1.parameterId to CategoryParameterMigrationInfo.OptionsMigration(
                    100500,
                    null,
                    100L
                )
            )
        )
        var migration = ParametersMigration(1L, categoryId, migrationInfo)
        migration = parametersMigrationRepository.insert(migration)

        categoryParameterMigrator.migrateNextDiffWhileExists()

        assert(migration.status == ParametersMigration.Status.COMPLETE)
    }

    @Test
    fun `do nothing when there is no new migrations`() {
        val migrationInfo = CategoryParameterMigrationInfo(
            mapOf(
                marketParam1.parameterId to CategoryParameterMigrationInfo.OptionsMigration(
                    100500,
                    null,
                    100L
                )
            )
        )
        var migration = ParametersMigration(1L, categoryId, migrationInfo, ParametersMigration.Status.FAILED)
        migration = parametersMigrationRepository.insert(migration)

        categoryParameterMigrator.migrateNextDiffWhileExists()

        migration = parametersMigrationRepository.findById(migration.id)
        assert(migration.status == ParametersMigration.Status.FAILED)
    }

    @Test
    fun `should not affect other shops when process migration`() {
        //affected shop1
        val migrationInfo = CategoryParameterMigrationInfo(
            mapOf(
                marketParam1.parameterId to CategoryParameterMigrationInfo.OptionsMigration(
                    100500,
                    null,
                    100L
                )
            )
        )
        var migration = ParametersMigration(1L, categoryId, migrationInfo)
        val expectedShop2Params = paramMappingRepository.findByShopId(shopId2)
        val expectedShop2Rules = paramMappingRuleRepository.findByMappings(expectedShop2Params.map { it.id })

        migration = parametersMigrationRepository.insert(migration)

        categoryParameterMigrator.migrateNextDiffWhileExists()

        migration = parametersMigrationRepository.findById(migration.id)
        assert(migration.status == ParametersMigration.Status.COMPLETE)

        val actualShop2Params = paramMappingRepository.findByShopId(shopId2)
        val actualShop2Rules = paramMappingRuleRepository.findByMappings(actualShop2Params.map { it.id })

        Assertions.assertThat(expectedShop2Params).usingRecursiveComparison().isEqualTo(actualShop2Params)
        Assertions.assertThat(expectedShop2Rules).usingRecursiveComparison().isEqualTo(actualShop2Rules)
    }

    @Test
    fun `should change only migrated mappings`() {
        //affected shop1
        val migrationInfo = CategoryParameterMigrationInfo(
            mapOf(
                marketParam1.parameterId to CategoryParameterMigrationInfo.OptionsMigration(
                    to = 100500,
                    o = null,
                    ts = 100L
                )
            )
        )
        var migration = ParametersMigration(1L, 1, migrationInfo)
        val expectedShop1Params = paramMappingRepository.findByShopId(shopId1).map {
            return@map if (it.marketParams[0].parameterId == marketParam1.parameterId) {
                it.copy(marketParams = listOf(MarketParam(100500L)))
            } else {
                it
            }
        }

        migration = parametersMigrationRepository.insert(migration)

        categoryParameterMigrator.migrateNextDiffWhileExists()

        migration = parametersMigrationRepository.findById(migration.id)
        assert(migration.status == ParametersMigration.Status.COMPLETE)

        val actualShop1Params = paramMappingRepository.findByShopId(shopId1)

        Assertions.assertThat(actualShop1Params).containsExactlyInAnyOrderElementsOf(expectedShop1Params)
    }

    @Test
    fun `should change only migrated rules`() {
        //affected shop1
        val migrationInfo = CategoryParameterMigrationInfo(
            mapOf(
                marketParam1.parameterId to CategoryParameterMigrationInfo.OptionsMigration(
                    to = 100500,
                    o = null,
                    ts = 100L
                )
            )
        )
        var migration = ParametersMigration(1L, 1, migrationInfo)
        val marketValuesExpected = mapOf(
            100500L to setOf(
                MarketParamValue.NumericValue(1.589)
            )
        )
        val expectedShop1Rules = listOf(paramMappingRule1.copy(marketValues = marketValuesExpected), paramMappingRule4)

        migration = parametersMigrationRepository.insert(migration)

        categoryParameterMigrator.migrateNextDiffWhileExists()

        migration = parametersMigrationRepository.findById(migration.id)
        assert(migration.status == ParametersMigration.Status.COMPLETE)

        val actualShop1Rules = paramMappingRuleRepository.findByMappings(listOf(paramMapping1.id))

        Assertions.assertThat(expectedShop1Rules).containsExactlyInAnyOrderElementsOf(actualShop1Rules)
    }

    fun `when there is no suitable shops then ok`() {
        val migrationInfo = CategoryParameterMigrationInfo(
            mapOf(
                99999999L to CategoryParameterMigrationInfo.OptionsMigration(
                    to = 100500,
                    o = null,
                    ts = 100L
                )
            )
        )
        var migration = ParametersMigration(1L, 1, migrationInfo)

        migration = parametersMigrationRepository.insert(migration)

        categoryParameterMigrator.migrateNextDiffWhileExists()

        assert(migration.status == ParametersMigration.Status.COMPLETE)
    }
}


