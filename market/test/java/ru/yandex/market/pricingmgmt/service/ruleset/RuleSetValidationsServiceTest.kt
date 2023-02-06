package ru.yandex.market.pricingmgmt.service.ruleset

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.AbstractFunctionalTest
import ru.yandex.market.pricingmgmt.exception.BadRequestException
import ru.yandex.market.pricingmgmt.model.dto.RuleSetQueryParams
import ru.yandex.market.pricingmgmt.service.RuleSetValidationsService
import ru.yandex.market.pricingmgmt.service.RuleSetValidationsService.RuExcMsgs
import ru.yandex.market.pricingmgmt.service.RuleSetValidationsService.RuExcMsgs.DOESNT_EXIST
import ru.yandex.market.pricingmgmt.service.RuleSetValidationsService.RuExcMsgs.DONT_EXIST
import ru.yandex.market.pricingmgmt.service.RuleSetValidationsService.RuExcMsgs.INCLUDED_EXCLUDED_INTERSECTION
import ru.yandex.market.pricingmgmt.service.RuleSetValidationsService.RuExcMsgs.WITH_ID_DOESNT_EXIST
import ru.yandex.market.pricingmgmt.service.RuleSetValidationsService.RuExcMsgs.WITH_ID_DONT_EXIST
import ru.yandex.market.pricingmgmt.util.ruleset.RuleSetFilterNormalizer
import ru.yandex.mj.generated.server.model.CreateRuleSetDto
import ru.yandex.mj.generated.server.model.RuleDto
import ru.yandex.mj.generated.server.model.RuleSetFilterDto

@DbUnitDataBaseConfig(
    DbUnitDataBaseConfig.Entry(
        name = "datatypeFactory",
        value = "ru.yandex.market.pricingmgmt.pg.ExtendedPostgresqlDataTypeFactory"
    )
)
class RuleSetValidationsServiceTest(
    @Autowired private val ruleSetValidationsService: RuleSetValidationsService,
) : AbstractFunctionalTest() {

    @Test
    @DbUnitDataSet(
        before = ["RuleSetValidationsServiceTest.ruleSetCreationTest.before.csv"],
    )
    fun testGroupNameValidationFailed() {
        val nullGroupName: String? = null
        val emptyGroupName = ""
        val notUniqueGroupName = "Старая Группа"

        Assertions.assertEquals(
            listOf(
                "Имя группы ${RuExcMsgs.NULL}.",
                "Имя группы ${RuExcMsgs.EMPTY}.",
                "Группа с названием \"Старая Группа\" уже существует.",
            ),
            listOf(
                assertThrows(BadRequestException::class.java) {
                    ruleSetValidationsService.groupNameValidation(nullGroupName)
                }.message,
                assertThrows(BadRequestException::class.java) {
                    ruleSetValidationsService.groupNameValidation(emptyGroupName)
                }.message,
                assertThrows(BadRequestException::class.java) {
                    ruleSetValidationsService.groupNameValidation(notUniqueGroupName)
                }.message
            ),
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["RuleSetValidationsServiceTest.ruleSetCreationTest.before.csv"],
    )
    fun testFilterValidationFailed(){
        // @NOTE: тут нужен этот маппинг, потому что иначе репозиторий падает с NPE, а ситуация, когда через первичную
        // обработку пролезают наллы, уже валидируется в тесте контроллера
        val filter = RuleSetFilterDto().id(1).categoryIds(listOf(1L))
        RuleSetFilterNormalizer.normalizeRuleSetFilter(filter)

        val nullFilters: List<RuleSetFilterDto> = emptyList()
        val duplicateFilters: List<RuleSetFilterDto> = listOf(filter)

        Assertions.assertEquals(
            listOf(
                "Набор фильтров ${RuExcMsgs.EMPTY}.",
                "Фильтр с id \"1\" уже сущетсвует."
            ),
            listOf(
                assertThrows(BadRequestException::class.java) {
                    ruleSetValidationsService.filterValidation(nullFilters, true)
                }.message,
                assertThrows(BadRequestException::class.java) {
                    ruleSetValidationsService.filterValidation(duplicateFilters, true)
                }.message
            ),
        )
    }

    @Test
    fun testProcessLists() {
        val filters: List<RuleSetFilterDto> = listOf(
            RuleSetFilterDto().categoryIds(listOf(11L,22L)).excludedCategoryIds(emptyList()).vendorIds(emptyList())
                .excludedVendorIds(emptyList()).sskus(emptyList()).excludedSskus(emptyList()).mskus(emptyList())
                .excludedMskus(emptyList()),
            RuleSetFilterDto().categoryIds(emptyList()).excludedCategoryIds(emptyList()).vendorIds(emptyList())
                .excludedVendorIds(emptyList()).sskus(emptyList()).excludedSskus(emptyList()).mskus(emptyList())
                .excludedMskus(emptyList()),
        )
        val ruleSet = CreateRuleSetDto()
            .filters(filters)

        Assertions.assertEquals(
            "Хотя бы один из списков включенных\\исключенных " +
                "категорий, брендов, SSKU, MSKU ${RuExcMsgs.NULL_OR_EMPTY}.",
            assertThrows(BadRequestException::class.java) {
                RuleSetValidationsService.processLists(ruleSet)
            }.message,
        )

        Assertions.assertEquals(
            true,
            ruleSet.filters[0].categoryIds.equals(listOf(11L,22L))
                && ruleSet.filters[0].excludedCategoryIds.isEmpty()
                && ruleSet.filters[0].vendorIds.isEmpty()
                && ruleSet.filters[0].excludedVendorIds.isEmpty()
                && ruleSet.filters[0].sskus.isEmpty()
                && ruleSet.filters[0].mskus.isEmpty()
                && ruleSet.filters[0].excludedSskus.isEmpty()
                && ruleSet.filters[0].excludedMskus.isEmpty()
        )

        Assertions.assertEquals(
            true,
            ruleSet.filters[1].categoryIds.isEmpty()
                && ruleSet.filters[1].excludedCategoryIds.isEmpty()
                && ruleSet.filters[1].vendorIds.isEmpty()
                && ruleSet.filters[1].excludedVendorIds.isEmpty()
                && ruleSet.filters[1].sskus.isEmpty()
                && ruleSet.filters[1].mskus.isEmpty()
                && ruleSet.filters[1].excludedSskus.isEmpty()
                && ruleSet.filters[1].excludedMskus.isEmpty()
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["RuleSetValidationsServiceTest.ruleSetCreationTest.before.csv"],
    )
    fun testCategoryIdsValidationFailed() {
        val ok = listOf(4L)
        val duplicates = listOf(1L, 5L, 1L)
        val nonExistent = listOf(1L, 5L, 16L, 7L, 9L)

        Assertions.assertEquals(
            listOf(
                "Список id категорий ${RuExcMsgs.DUPLICATES}.",
                "Список исключенных id категорий ${RuExcMsgs.DUPLICATES}.",
                WITH_ID_DONT_EXIST.format("Категории", "[16, 7, 9]"),
                INCLUDED_EXCLUDED_INTERSECTION.format("id категорий"),
            ),
            listOf(
                assertThrows(BadRequestException::class.java) {
                    ruleSetValidationsService.categoryIdsValidation(duplicates, emptyList())
                }.message,
                assertThrows(BadRequestException::class.java) {
                    ruleSetValidationsService.categoryIdsValidation(emptyList(), duplicates)
                }.message,
                assertThrows(BadRequestException::class.java) {
                    ruleSetValidationsService.categoryIdsValidation(nonExistent, emptyList())
                }.message,
                assertThrows(BadRequestException::class.java) {
                    ruleSetValidationsService.categoryIdsValidation(ok, ok)
                }.message,
            ),
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["RuleSetValidationsServiceTest.ruleSetCreationTest.before.csv"],
    )
    fun testVendorIdsValidationFailed() {
        val ok = listOf(5L)
        val duplicates = listOf(2L, 6L, 2L)
        val nonExistent = listOf(2L, 5L, 56L, 6L, 78L)

        Assertions.assertEquals(
            listOf(
                "Список id брендов ${RuExcMsgs.DUPLICATES}.",
                "Список исключенных id брендов ${RuExcMsgs.DUPLICATES}.",
                WITH_ID_DONT_EXIST.format("Бренды", "[56, 78]"),
                INCLUDED_EXCLUDED_INTERSECTION.format("id брендов"),
            ),
            listOf(
                assertThrows(BadRequestException::class.java) {
                    ruleSetValidationsService.vendorIdsValidation(duplicates, emptyList())
                }.message,
                assertThrows(BadRequestException::class.java) {
                    ruleSetValidationsService.vendorIdsValidation(emptyList(), duplicates)
                }.message,
                assertThrows(BadRequestException::class.java) {
                    ruleSetValidationsService.vendorIdsValidation(nonExistent, emptyList())
                }.message,
                assertThrows(BadRequestException::class.java) {
                    ruleSetValidationsService.vendorIdsValidation(ok, ok)
                }.message,
            ),
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["RuleSetValidationsServiceTest.ruleSetCreationTest.before.csv"],
    )
    fun testSskusValidationFailed() {
        val ok = listOf("61")
        val duplicates = listOf("62", "64", "67", "64")
        val nonExistent = listOf("62", "13", "67", "64", "666")

        Assertions.assertEquals(
            listOf(
                "Список SSKU ${RuExcMsgs.DUPLICATES}.",
                "Список исключенных SSKU ${RuExcMsgs.DUPLICATES}.",
                DONT_EXIST.format("SSKU: [13, 666]"),
                INCLUDED_EXCLUDED_INTERSECTION.format("SSKU"),
            ),
            listOf(
                assertThrows(BadRequestException::class.java) {
                    ruleSetValidationsService.sskusValidation(duplicates, emptyList())
                }.message,
                assertThrows(BadRequestException::class.java) {
                    ruleSetValidationsService.sskusValidation(emptyList(), duplicates)
                }.message,
                assertThrows(BadRequestException::class.java) {
                    ruleSetValidationsService.sskusValidation(nonExistent, emptyList())
                }.message,
                assertThrows(BadRequestException::class.java) {
                    ruleSetValidationsService.sskusValidation(ok, listOf("62", "64", "67", "61"))
                }.message,
            ),
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["RuleSetValidationsServiceTest.ruleSetCreationTest.before.csv"],
    )
    fun testMskusValidationFailed() {
        val ok = listOf(61L)
        val duplicates = listOf(62L, 64L, 67L, 64L)
        val nonExistent = listOf(62L, 13L, 67L, 64L, 666L)

        Assertions.assertEquals(
            listOf(
                "Список MSKU ${RuExcMsgs.DUPLICATES}.",
                "Список исключенных MSKU ${RuExcMsgs.DUPLICATES}.",
                DONT_EXIST.format("MSKU: [13, 666]"),
                INCLUDED_EXCLUDED_INTERSECTION.format("MSKU"),
            ),
            listOf(
                assertThrows(BadRequestException::class.java) {
                    ruleSetValidationsService.mskusValidation(duplicates, emptyList())
                }.message,
                assertThrows(BadRequestException::class.java) {
                    ruleSetValidationsService.mskusValidation(emptyList(), duplicates)
                }.message,
                assertThrows(BadRequestException::class.java) {
                    ruleSetValidationsService.mskusValidation(nonExistent, emptyList())
                }.message,
                assertThrows(BadRequestException::class.java) {
                    ruleSetValidationsService.mskusValidation(ok, listOf(62L, 64L, 67L, 61L))
                }.message,
            ),
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["RuleSetValidationsServiceTest.ruleSetCreationTest.before.csv"],
    )
    fun testRulesValidationFailed() {
        fun okRule() = RuleDto().id(1L).type(1L).action(1L).coefficient(1.0f).order(1).priceType(1L).rounding(99.99)
        val nuLL: List<RuleDto>? = null
        val nonExistentId = listOf(okRule().id(2L))
        val duplIds = listOf(okRule(), okRule().order(2))
        val noType = listOf(okRule().type(null))
        val nonExistentType = listOf(okRule().type(8L))
        val noAction = listOf(okRule().action(null))
        val nonExistentAction = listOf(okRule().action(3L))
        val noCoefficient = listOf(okRule().coefficient(null))
        val badCoefficient = listOf(okRule().coefficient(100.0001f))
        val noOrder = listOf(okRule().order(null))
        val noPriceType = listOf(okRule().priceType(null))
        val nonExistentPriceType = listOf(okRule().priceType(3L))

        Assertions.assertEquals(
            listOf(
                "Список правил ${RuExcMsgs.NULL_OR_EMPTY}.",
                WITH_ID_DONT_EXIST.format("Правила", "[2]"),
                "Список id обновляемых правил ${RuExcMsgs.DUPLICATES}.",
                "Тип правила ${RuExcMsgs.NULL}.",
                WITH_ID_DOESNT_EXIST.format("Тип правила", 8),
                "Действие правила ${RuExcMsgs.NULL}.",
                WITH_ID_DOESNT_EXIST.format("Действие правила", 3),
                "Коэффициент правила ${RuExcMsgs.NULL_OR_ZERO}.",
                "Коэффициенты правил могут принимать значения " +
                    "только от -100.0 до 100.0 включительно.",
                "Порядок правила ${RuExcMsgs.NULL}.",
                "Тип цены правила ${RuExcMsgs.NULL}.",
                WITH_ID_DOESNT_EXIST.format("Тип цены правила", 3),
            ),
            listOf(
                assertThrows(BadRequestException::class.java) {
                    ruleSetValidationsService.rulesValidation(nuLL, true)
                }.message,
                assertThrows(BadRequestException::class.java) {
                    ruleSetValidationsService.rulesValidation(nonExistentId, false)
                }.message,
                assertThrows(BadRequestException::class.java) {
                    ruleSetValidationsService.rulesValidation(duplIds, false)
                }.message,
                assertThrows(BadRequestException::class.java) {
                    ruleSetValidationsService.rulesValidation(noType, true)
                }.message,
                assertThrows(BadRequestException::class.java) {
                    ruleSetValidationsService.rulesValidation(nonExistentType, true)
                }.message,
                assertThrows(BadRequestException::class.java) {
                    ruleSetValidationsService.rulesValidation(noAction, true)
                }.message,
                assertThrows(BadRequestException::class.java) {
                    ruleSetValidationsService.rulesValidation(nonExistentAction, true)
                }.message,
                assertThrows(BadRequestException::class.java) {
                    ruleSetValidationsService.rulesValidation(noCoefficient, true)
                }.message,
                assertThrows(BadRequestException::class.java) {
                    ruleSetValidationsService.rulesValidation(badCoefficient, true)
                }.message,
                assertThrows(BadRequestException::class.java) {
                    ruleSetValidationsService.rulesValidation(noOrder, true)
                }.message,
                assertThrows(BadRequestException::class.java) {
                    ruleSetValidationsService.rulesValidation(noPriceType, true)
                }.message,
                assertThrows(BadRequestException::class.java) {
                    ruleSetValidationsService.rulesValidation(nonExistentPriceType, true)
                }.message,
            ),
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["RuleSetValidationsServiceTest.ruleSetCreationTest.before.csv"],
    )
    fun testRuleSetIdValidation() {
        Assertions.assertEquals(
            listOf(
                WITH_ID_DOESNT_EXIST.format("Набор правил", 4),
                WITH_ID_DOESNT_EXIST.format("Набор правил", 5),
            ),
            listOf(
                assertThrows(BadRequestException::class.java) {
                    ruleSetValidationsService.ruleSetIdValidation(4L)
                }.message,
                assertThrows(BadRequestException::class.java) {
                    ruleSetValidationsService.ruleSetIdValidation(5L)
                }.message,
            ),
        )

        ruleSetValidationsService.ruleSetIdValidation(2) // ok
    }

    @Test
    @DbUnitDataSet(
        before = ["RuleSetValidationsServiceTest.getRuleSetsFiltersTest.before.csv"],
    )
    fun testProcessQueryParams() {
        val nonExistentSsku = RuleSetQueryParams(ssku = "7777.7777")
        val conflictSskuCategoryId = RuleSetQueryParams(ssku = "1111.1111", categoryId = 4L)
        val conflictSskuVendorId = RuleSetQueryParams(ssku = "1111.1111", vendorId = 2L)
        val okSsku = RuleSetQueryParams(ssku = "1111.1111")

        val nonExistentMsku = RuleSetQueryParams(msku = 7777L)
        val conflictMskuCategoryId = RuleSetQueryParams(msku = 1111L, categoryId = 4L)
        val conflictMskuVendorId = RuleSetQueryParams(msku = 1111L, vendorId = 2L)
        val okMsku = RuleSetQueryParams(msku = 1111L)

        Assertions.assertEquals(
            listOf(
                DOESNT_EXIST.format("SSKU: 7777.7777"),
                "ID категории по SSKU не соответствует данному ID категории: 4.",
                "ID бренда по SSKU не соответствует данному ID бренда: 2.",
                DOESNT_EXIST.format("MSKU: 7777"),
                "ID категории по MSKU не соответствует данному ID категории: 4.",
                "ID бренда по MSKU не соответствует данному ID бренда: 2.",
            ),
            listOf(
                assertThrows(BadRequestException::class.java) {
                    ruleSetValidationsService.processQueryParams(nonExistentSsku)
                }.message,
                assertThrows(BadRequestException::class.java) {
                    ruleSetValidationsService.processQueryParams(conflictSskuCategoryId)
                }.message,
                assertThrows(BadRequestException::class.java) {
                    ruleSetValidationsService.processQueryParams(conflictSskuVendorId)
                }.message,
                assertThrows(BadRequestException::class.java) {
                    ruleSetValidationsService.processQueryParams(nonExistentMsku)
                }.message,
                assertThrows(BadRequestException::class.java) {
                    ruleSetValidationsService.processQueryParams(conflictMskuCategoryId)
                }.message,
                assertThrows(BadRequestException::class.java) {
                    ruleSetValidationsService.processQueryParams(conflictMskuVendorId)
                }.message,
            ),
        )

        ruleSetValidationsService.processQueryParams(okSsku)
        Assertions.assertEquals(okSsku.categoryId, 1L)
        Assertions.assertEquals(okSsku.vendorId, 6L)
        ruleSetValidationsService.processQueryParams(okMsku)
        Assertions.assertEquals(okMsku.categoryId, 1L)
        Assertions.assertEquals(okMsku.vendorId, 6L)
    }
}
