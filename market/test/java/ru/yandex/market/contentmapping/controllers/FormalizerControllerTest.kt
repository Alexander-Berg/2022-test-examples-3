package ru.yandex.market.contentmapping.controllers

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.contentmapping.controllers.FormalizerController.ParamFormalizerRequest
import ru.yandex.market.contentmapping.controllers.helper.ControllerAccessHelper
import ru.yandex.market.contentmapping.dto.data.category.CategoryInfo
import ru.yandex.market.contentmapping.dto.data.category.parameter.CategoryParameterValue
import ru.yandex.market.contentmapping.dto.mapping.MarketParam
import ru.yandex.market.contentmapping.dto.mapping.MarketParamValue
import ru.yandex.market.contentmapping.dto.mapping.ParamMapping
import ru.yandex.market.contentmapping.dto.mapping.ParamMappingRule
import ru.yandex.market.contentmapping.dto.mapping.ShopParam
import ru.yandex.market.contentmapping.dto.model.Shop
import ru.yandex.market.contentmapping.kotlin.typealiases.CategoryId
import ru.yandex.market.contentmapping.repository.ParamMappingRepository
import ru.yandex.market.contentmapping.repository.ParamMappingRuleRepository
import ru.yandex.market.contentmapping.repository.ShopModelForExportRepository
import ru.yandex.market.contentmapping.repository.ShopRepository
import ru.yandex.market.contentmapping.services.OfferProcessingDataService
import ru.yandex.market.contentmapping.services.ShopModelRatingService
import ru.yandex.market.contentmapping.services.ShopModelService
import ru.yandex.market.contentmapping.services.ShopModelValidationService
import ru.yandex.market.contentmapping.services.ShopModelViewMaker
import ru.yandex.market.contentmapping.services.ShopModelViewService
import ru.yandex.market.contentmapping.services.category.info.CategoryInfoService
import ru.yandex.market.contentmapping.services.category.info.CategoryParameterInfoService
import ru.yandex.market.contentmapping.services.category.info.CategoryParameterMigrationLatchService
import ru.yandex.market.contentmapping.services.category.info.migration.CategoryParameterMigrationInfo
import ru.yandex.market.contentmapping.services.category.info.migration.ParameterIdOnlineTransformerTool
import ru.yandex.market.contentmapping.services.image.ReversibleEncryptService
import ru.yandex.market.contentmapping.services.mapping.ParamMappingService
import ru.yandex.market.contentmapping.services.model_statistics.ShopModelStatisticsQueueService
import ru.yandex.market.contentmapping.services.rules.MarketValuesResolver
import ru.yandex.market.contentmapping.services.rules.RulesLoadService
import ru.yandex.market.contentmapping.services.rules.v2.RuleEngineService
import ru.yandex.market.contentmapping.testdata.CategoryParameterInfoTestInstance
import ru.yandex.market.contentmapping.testutils.BaseAppTestClass
import ru.yandex.market.mbo.export.MboParameters

/**
 * @author yuramalinov
 * @created 11.03.2020
 */
class FormalizerControllerTest : BaseAppTestClass() {
    @Autowired
    private lateinit var shopRepository: ShopRepository

    @Autowired
    private lateinit var mappingRepository: ParamMappingRepository

    @Autowired
    private lateinit var ruleRepository: ParamMappingRuleRepository

    @Autowired
    private lateinit var paramMappingService: ParamMappingService

    private lateinit var formalizerController: FormalizerController

    private lateinit var categoryInfoService: CategoryInfoService
    private lateinit var categoryParameterInfoService: CategoryParameterInfoService
    private lateinit var paramMapping: ParamMapping
    private lateinit var shopModelRatingService: ShopModelRatingService

    @Before
    fun setup() {
        val (id) = shopRepository.insert(Shop(1, "Shop1"))
        paramMapping = mappingRepository.insert(
            ParamMapping(
                shopId = id,
                shopParams = listOf(ShopParam("test")),
                marketParams = listOf(MarketParam(123)),
            )
        )
        categoryInfoService = Mockito.mock(CategoryInfoService::class.java)
        categoryParameterInfoService = Mockito.mock(CategoryParameterInfoService::class.java)
        shopModelRatingService = Mockito.mock(ShopModelRatingService::class.java)
        formalizerController = FormalizerController(
            Mockito.mock(ControllerAccessHelper::class.java),
            paramMappingService,
            categoryInfoService,
            categoryParameterInfoService,
            mappingRepository,
            ruleRepository,
            Mockito.mock(RulesLoadService::class.java),
            ShopModelViewMaker(
                Mockito.mock(RuleEngineService::class.java),
                Mockito.mock(ShopModelService::class.java),
                ShopModelViewService(
                    categoryInfoService,
                    Mockito.mock(OfferProcessingDataService::class.java),
                    Mockito.mock(ShopModelForExportRepository::class.java),
                    Mockito.mock(ReversibleEncryptService::class.java),
                    MarketValuesResolver(),
                    Mockito.mock(ShopModelValidationService::class.java),
                    shopModelRatingService,
                    ParameterIdOnlineTransformerTool(
                        categoryInfoService,
                        object : CategoryParameterMigrationLatchService {
                            override fun isMigrationEnabled() = false
                        }
                    )
                ),
                shopModelRatingService,
                Mockito.mock(ShopModelStatisticsQueueService::class.java),
            )
        )
    }

    @After
    fun clear() {
        categoryInfoService.clear()
        categoryParameterInfoService.clear()
    }

    @Test
    fun testFormalize() {
        mockCategoryData()
        assertThat(ruleRepository.findAll()).isEmpty()
        val response = formalizerController.formalizeAndUpdateMapping(
            ParamFormalizerRequest(
                MARKET_CATEGORY_ID,
                paramMapping.id,
                listOf("test", "option")
            )
        )
        assertThat(response.allParamMappingRules).hasSize(1)
        assertThat(ruleRepository.findAll()).hasSize(1)
    }

    @Test
    fun testUpdate() {
        ruleRepository.insert(
            ParamMappingRule(
                id = ruleRepository.nextId(),
                paramMappingId = paramMapping.id,
                shopValues = mapOf("test" to "some-value"),
                marketValues = mapOf(PARAM_ID to setOf(MarketParamValue.StringValue("str"))),
            )
        )
        ruleRepository.insert(
            ParamMappingRule(
                id = ruleRepository.nextId(),
                paramMappingId = paramMapping.id,
                shopValues = mapOf("test" to "other-value"),
                marketValues = mapOf(PARAM_ID to setOf(MarketParamValue.StringValue("str"))),
            )
        )

        mockCategoryData()
        assertThat(ruleRepository.findAll()).hasSize(2)
        val response = formalizerController.formalizeAndUpdateMapping(
            ParamFormalizerRequest(
                MARKET_CATEGORY_ID,
                paramMapping.id,
                listOf("test", "option", "some-value", "unknown")
            )
        )
        assertThat(response.allParamMappingRules).hasSize(3)
        assertThat(ruleRepository.findAll())
            .hasSize(3)
            .extracting(ParamMappingRule::shopValues, ParamMappingRule::isHypothesis)
            .containsExactlyInAnyOrderElementsOf(
                listOf(
                    tuple(mapOf("test" to "option"), false),
                    tuple(mapOf("test" to "other-value"), false),
                    tuple(mapOf("test" to "some-value"), false)
                )
            )
    }

    private fun mockCategoryData() {
        Mockito.`when`(categoryInfoService.getCategoryInfo(Mockito.eq(MARKET_CATEGORY_ID)))
            .thenReturn(CategoryInfoTestInstance().copy(isLeaf = true))
        val options = Int2ObjectOpenHashMap<CategoryParameterValue>()
        options[2001] = CategoryParameterValue.createValue(2001, arrayOf("option"))
        Mockito.`when`(categoryParameterInfoService.getCategoryParameters(Mockito.eq(MARKET_CATEGORY_ID)))
            .thenReturn(
                mapOf(
                    MARKET_CATEGORY_ID to
                            CategoryParameterInfoTestInstance().copy(
                                parameterId = PARAM_ID,
                                valueType = MboParameters.ValueType.ENUM,
                                options = options,
                            )
                )
            )
    }

    companion object {
        const val MARKET_CATEGORY_ID: CategoryId = 1001
        const val PARAM_ID: Long = 123
    }
}

fun CategoryInfoTestInstance() = CategoryInfo(
    hid = 0,
    parentHid = 0,
    name = "Test",
    uniqueName = null,
    isNotUsed = false,
    isPublished = true,
    isAcceptGoodContent = false,
    inCategory = null,
    outOfCategory = null,
    isLeaf = true,
    parametersMapping = CategoryParameterMigrationInfo(emptyMap())
)
