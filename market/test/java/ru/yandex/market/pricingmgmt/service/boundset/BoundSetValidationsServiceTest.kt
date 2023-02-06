package ru.yandex.market.pricingmgmt.service.boundset

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.AbstractFunctionalTest
import ru.yandex.market.pricingmgmt.exception.BadRequestException
import ru.yandex.market.pricingmgmt.model.dto.RuleSetQueryParams
import ru.yandex.market.pricingmgmt.service.BoundSetValidationsService
import ru.yandex.market.pricingmgmt.service.RuleSetValidationsService.RuExcMsgs
import ru.yandex.market.pricingmgmt.service.RuleSetValidationsService.RuExcMsgs.DOESNT_EXIST
import ru.yandex.market.pricingmgmt.service.RuleSetValidationsService.RuExcMsgs.DONT_EXIST
import ru.yandex.market.pricingmgmt.service.RuleSetValidationsService.RuExcMsgs.INCLUDED_EXCLUDED_INTERSECTION
import ru.yandex.market.pricingmgmt.service.RuleSetValidationsService.RuExcMsgs.WITH_ID_DOESNT_EXIST
import ru.yandex.market.pricingmgmt.service.RuleSetValidationsService.RuExcMsgs.WITH_ID_DONT_EXIST
import ru.yandex.mj.generated.server.model.BoundDto
import ru.yandex.mj.generated.server.model.BoundSetFilterDto
import ru.yandex.mj.generated.server.model.BoundType
import ru.yandex.mj.generated.server.model.CreateBoundSetDto

@DbUnitDataBaseConfig(
    DbUnitDataBaseConfig.Entry(
        name = "datatypeFactory",
        value = "ru.yandex.market.pricingmgmt.pg.ExtendedPostgresqlDataTypeFactory"
    )
)
class BoundSetValidationsServiceTest(
    @Autowired private val boundSetValidationsService: BoundSetValidationsService,
) : AbstractFunctionalTest() {

    @Test
    @DbUnitDataSet(
        before = ["BoundSetValidationsServiceTest.boundSetCreationTest.before.csv"],
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
                Assertions.assertThrows(BadRequestException::class.java) {
                    boundSetValidationsService.groupNameValidation(nullGroupName)
                }.message,
                Assertions.assertThrows(BadRequestException::class.java) {
                    boundSetValidationsService.groupNameValidation(emptyGroupName)
                }.message,
                Assertions.assertThrows(BadRequestException::class.java) {
                    boundSetValidationsService.groupNameValidation(notUniqueGroupName)
                }.message
            ),
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["BoundSetValidationsServiceTest.boundSetCreationTest.before.csv"],
    )
    fun testFilterValidationFailed(){
        val nullFilters: List<BoundSetFilterDto> = emptyList()
        val duplicateFilters: List<BoundSetFilterDto> = listOf(
            BoundSetFilterDto().id(1).categoryIds(listOf(1L)),
        )

        Assertions.assertEquals(
            listOf(
               "Набор фильтров ${RuExcMsgs.EMPTY}.",
                "Фильтр с id \"1\" уже сущетсвует."
            ),
            listOf(
                Assertions.assertThrows(BadRequestException::class.java) {
                    boundSetValidationsService.filterValidation(nullFilters, true)
                }.message,
                Assertions.assertThrows(BadRequestException::class.java) {
                    boundSetValidationsService.filterValidation(duplicateFilters, true)
                }.message
            ),
        )
    }

    @Test
    fun testProcessLists() {
        val filters: List<BoundSetFilterDto> = listOf(
            BoundSetFilterDto().categoryIds(listOf(11L,22L)).excludedCategoryIds(emptyList()).vendorIds(emptyList())
                .excludedVendorIds(emptyList()).sskus(emptyList()).excludedSskus(emptyList()).mskus(emptyList())
                .excludedMskus(emptyList()),
            BoundSetFilterDto().categoryIds(emptyList()).excludedCategoryIds(emptyList()).vendorIds(emptyList())
                .excludedVendorIds(emptyList()).sskus(emptyList()).excludedSskus(emptyList()).mskus(emptyList())
                .excludedMskus(emptyList()),
        )
        val boundSet = CreateBoundSetDto()
            .filters(filters)

        Assertions.assertEquals(
            "Хотя бы один из списков включенных\\исключенных " +
                "категорий, брендов, SSKU, MSKU ${RuExcMsgs.NULL_OR_EMPTY}.",
            Assertions.assertThrows(BadRequestException::class.java) {
                boundSetValidationsService.processLists(boundSet)
            }.message,
        )

        Assertions.assertEquals(
            true,
            boundSet.filters[0].categoryIds.equals(listOf(11L,22L))
                && boundSet.filters[0].excludedCategoryIds.isEmpty()
                && boundSet.filters[0].vendorIds.isEmpty()
                && boundSet.filters[0].excludedVendorIds.isEmpty()
                && boundSet.filters[0].sskus.isEmpty()
                && boundSet.filters[0].mskus.isEmpty()
                && boundSet.filters[0].excludedSskus.isEmpty()
                && boundSet.filters[0].excludedMskus.isEmpty()
        )

        Assertions.assertEquals(
            true,
            boundSet.filters[1].categoryIds.isEmpty()
                && boundSet.filters[1].excludedCategoryIds.isEmpty()
                && boundSet.filters[1].vendorIds.isEmpty()
                && boundSet.filters[1].excludedVendorIds.isEmpty()
                && boundSet.filters[1].sskus.isEmpty()
                && boundSet.filters[1].mskus.isEmpty()
                && boundSet.filters[1].excludedSskus.isEmpty()
                && boundSet.filters[1].excludedMskus.isEmpty()
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["BoundSetValidationsServiceTest.boundSetCreationTest.before.csv"],
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
                Assertions.assertThrows(BadRequestException::class.java) {
                    boundSetValidationsService.categoryIdsValidation(duplicates, emptyList())
                }.message,
                Assertions.assertThrows(BadRequestException::class.java) {
                    boundSetValidationsService.categoryIdsValidation(emptyList(), duplicates)
                }.message,
                Assertions.assertThrows(BadRequestException::class.java) {
                    boundSetValidationsService.categoryIdsValidation(nonExistent, emptyList())
                }.message,
                Assertions.assertThrows(BadRequestException::class.java) {
                    boundSetValidationsService.categoryIdsValidation(ok, ok)
                }.message,
            ),
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["BoundSetValidationsServiceTest.boundSetCreationTest.before.csv"],
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
                Assertions.assertThrows(BadRequestException::class.java) {
                    boundSetValidationsService.vendorIdsValidation(duplicates, emptyList())
                }.message,
                Assertions.assertThrows(BadRequestException::class.java) {
                    boundSetValidationsService.vendorIdsValidation(emptyList(), duplicates)
                }.message,
                Assertions.assertThrows(BadRequestException::class.java) {
                    boundSetValidationsService.vendorIdsValidation(nonExistent, emptyList())
                }.message,
                Assertions.assertThrows(BadRequestException::class.java) {
                    boundSetValidationsService.vendorIdsValidation(ok, ok)
                }.message,
            ),
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["BoundSetValidationsServiceTest.boundSetCreationTest.before.csv"],
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
                Assertions.assertThrows(BadRequestException::class.java) {
                    boundSetValidationsService.sskusValidation(duplicates, emptyList())
                }.message,
                Assertions.assertThrows(BadRequestException::class.java) {
                    boundSetValidationsService.sskusValidation(emptyList(), duplicates)
                }.message,
                Assertions.assertThrows(BadRequestException::class.java) {
                    boundSetValidationsService.sskusValidation(nonExistent, emptyList())
                }.message,
                Assertions.assertThrows(BadRequestException::class.java) {
                    boundSetValidationsService.sskusValidation(ok, listOf("62", "64", "67", "61"))
                }.message,
            ),
        )
    }
    @Test
    @DbUnitDataSet(
        before = ["BoundSetValidationsServiceTest.boundSetCreationTest.before.csv"],
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
                Assertions.assertThrows(BadRequestException::class.java) {
                    boundSetValidationsService.mskusValidation(duplicates, emptyList())
                }.message,
                Assertions.assertThrows(BadRequestException::class.java) {
                    boundSetValidationsService.mskusValidation(emptyList(), duplicates)
                }.message,
                Assertions.assertThrows(BadRequestException::class.java) {
                    boundSetValidationsService.mskusValidation(nonExistent, emptyList())
                }.message,
                Assertions.assertThrows(BadRequestException::class.java) {
                    boundSetValidationsService.mskusValidation(ok, listOf(62L, 64L, 67L, 61L))
                }.message,
            ),
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["BoundSetValidationsServiceTest.boundSetCreationTest.before.csv"],
    )
    fun testBoundsValidationFailed() {
        fun okBound() = BoundDto().id(1L).type(BoundType.UPPER).action(1L).coefficient(1.0f).order(1)
            .priceType(1L).rounding(99.99)

        val nuLL: List<BoundDto>? = null
        val nonExistentId = listOf(okBound().id(2L))
        val duplIds = listOf(okBound(), okBound().order(2))
        val noType = listOf(okBound().type(null))
        val noAction = listOf(okBound().action(null))
        val nonExistentAction = listOf(okBound().action(3L))
        val noCoefficient = listOf(okBound().coefficient(null))
        val badCoefficient = listOf(okBound().coefficient(100.0001f))
        val noOrder = listOf(okBound().order(null))
        val nonExistentPriceType = listOf(okBound().priceType(3L))
        Assertions.assertEquals(
            listOf(
                "Список границ ${RuExcMsgs.NULL_OR_EMPTY}.",
                WITH_ID_DONT_EXIST.format("Границы", "[2]"),
                "Список id обновляемых границ ${RuExcMsgs.DUPLICATES}.",
                "Тип границы ${RuExcMsgs.NULL}.",
                "Действие границы ${RuExcMsgs.NULL}.",
                WITH_ID_DOESNT_EXIST.format("Действие границы", 3),
                "Коэффициент границы ${RuExcMsgs.NULL_OR_ZERO}.",
                "Коэффициенты границ могут принимать значения " +
                    "только от -100.0 до 100.0 включительно.",
                "Порядок границы ${RuExcMsgs.NULL}.",
                WITH_ID_DOESNT_EXIST.format("Тип цены границы", 3),
            ),
            listOf(
                Assertions.assertThrows(BadRequestException::class.java) {
                    boundSetValidationsService.boundsValidation(nuLL, true)
                }.message,
                Assertions.assertThrows(BadRequestException::class.java) {
                    boundSetValidationsService.boundsValidation(nonExistentId, false)
                }.message,
                Assertions.assertThrows(BadRequestException::class.java) {
                    boundSetValidationsService.boundsValidation(duplIds, false)
                }.message,
                Assertions.assertThrows(BadRequestException::class.java) {
                    boundSetValidationsService.boundsValidation(noType, true)
                }.message,
                Assertions.assertThrows(BadRequestException::class.java) {
                    boundSetValidationsService.boundsValidation(noAction, true)
                }.message,
                Assertions.assertThrows(BadRequestException::class.java) {
                    boundSetValidationsService.boundsValidation(nonExistentAction, true)
                }.message,
                Assertions.assertThrows(BadRequestException::class.java) {
                    boundSetValidationsService.boundsValidation(noCoefficient, true)
                }.message,
                Assertions.assertThrows(BadRequestException::class.java) {
                    boundSetValidationsService.boundsValidation(badCoefficient, true)
                }.message,
                Assertions.assertThrows(BadRequestException::class.java) {
                    boundSetValidationsService.boundsValidation(noOrder, true)
                }.message,
                Assertions.assertThrows(BadRequestException::class.java) {
                    boundSetValidationsService.boundsValidation(nonExistentPriceType, true)
                }.message,
            ),
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["BoundSetValidationsServiceTest.boundSetCreationTest.before.csv"],
    )
    fun testBoundSetIdValidation() {
        Assertions.assertEquals(
            listOf(
                WITH_ID_DOESNT_EXIST.format("Набор границ", 4),
                WITH_ID_DOESNT_EXIST.format("Набор границ", 5),
            ),
            listOf(
                Assertions.assertThrows(BadRequestException::class.java) {
                    boundSetValidationsService.boundSetIdValidation(4L)
                }.message,
                Assertions.assertThrows(BadRequestException::class.java) {
                    boundSetValidationsService.boundSetIdValidation(5L)
                }.message,
            ),
        )

        boundSetValidationsService.boundSetIdValidation(2) // ok
    }

    @Test
    @DbUnitDataSet(
        before = ["BoundSetValidationsServiceTest.getBoundSetsFiltersTest.before.csv"],
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
                Assertions.assertThrows(BadRequestException::class.java) {
                    boundSetValidationsService.processQueryParams(nonExistentSsku)
                }.message,
                Assertions.assertThrows(BadRequestException::class.java) {
                    boundSetValidationsService.processQueryParams(conflictSskuCategoryId)
                }.message,
                Assertions.assertThrows(BadRequestException::class.java) {
                    boundSetValidationsService.processQueryParams(conflictSskuVendorId)
                }.message,
                Assertions.assertThrows(BadRequestException::class.java) {
                    boundSetValidationsService.processQueryParams(nonExistentMsku)
                }.message,
                Assertions.assertThrows(BadRequestException::class.java) {
                    boundSetValidationsService.processQueryParams(conflictMskuCategoryId)
                }.message,
                Assertions.assertThrows(BadRequestException::class.java) {
                    boundSetValidationsService.processQueryParams(conflictMskuVendorId)
                }.message,
            ),
        )

        boundSetValidationsService.processQueryParams(okSsku)
        Assertions.assertEquals(okSsku.categoryId, 1L)
        Assertions.assertEquals(okSsku.vendorId, 6L)

        boundSetValidationsService.processQueryParams(okMsku)
        Assertions.assertEquals(okMsku.categoryId, 1L)
        Assertions.assertEquals(okMsku.vendorId, 6L)
    }
}
