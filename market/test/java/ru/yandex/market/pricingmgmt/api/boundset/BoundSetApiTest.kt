package ru.yandex.market.pricingmgmt.api.boundset

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.api.ControllerTest
import ru.yandex.market.pricingmgmt.config.security.passport.PassportAuthenticationFilter
import ru.yandex.market.pricingmgmt.service.TimeService
import ru.yandex.market.pricingmgmt.util.DateTimeTestingUtil
import ru.yandex.mj.generated.server.model.BoundDto
import ru.yandex.mj.generated.server.model.BoundSetFilterDto
import ru.yandex.mj.generated.server.model.BoundType
import ru.yandex.mj.generated.server.model.CreateBoundSetDto
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@DbUnitDataBaseConfig(
    DbUnitDataBaseConfig.Entry(
        name = "datatypeFactory",
        value = "ru.yandex.market.pricingmgmt.pg.ExtendedPostgresqlDataTypeFactory"
    )
)
@WithMockUser(
    username = PassportAuthenticationFilter.LOCAL_DEV,
    roles = ["PRICING_MGMT_ACCESS"]
)
class BoundSetApiTest : ControllerTest() {

    @MockBean
    private lateinit var timeService: TimeService

    @Test
    @DbUnitDataSet(
        before = ["Bounds.dictionary.csv", "BoundSetApiTest.getBoundsTest.before.csv"]
    )
    fun testGetBounds() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/bound-sets/1/bounds")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].id").value(100L))
            .andExpect(jsonPath("$[0].type").value(BoundType.UPPER.value))
            .andExpect(jsonPath("$[0].action").value(2L))
            .andExpect(jsonPath("$[0].coefficient").value(45.67f))
            .andExpect(jsonPath("$[0].order").value(1))
            .andExpect(jsonPath("$[0].priceType").value(2L))
            .andExpect(jsonPath("$[0].rounding").value(99.0))
            .andExpect(jsonPath("$[1].id").value(203L))
            .andExpect(jsonPath("$[1].type").value(BoundType.LOWER.value))
            .andExpect(jsonPath("$[1].action").value(2L))
            .andExpect(jsonPath("$[1].coefficient").value(99.9956f))
            .andExpect(jsonPath("$[1].order").value(2))
            .andExpect(jsonPath("$[1].priceType").value(2L))
            .andExpect(jsonPath("$[1].rounding").value(0.009))
            .andExpect(jsonPath("$[2].id").value(34L))
            .andExpect(jsonPath("$[2].type").value(BoundType.UPPER.value))
            .andExpect(jsonPath("$[2].action").value(1L))
            .andExpect(jsonPath("$[2].coefficient").value(99.0f))
            .andExpect(jsonPath("$[2].order").value(3))
            .andExpect(jsonPath("$[2].priceType").value(1L))
            .andExpect(jsonPath("$[2].rounding").value(0.99))
    }

    @Test
    @DbUnitDataSet(
        before = ["Bounds.dictionary.csv", "BoundSetApiTest.boundSetCreationTest.before.csv"],
        after = ["BoundSetApiTest.boundSetCreationTest.after.csv"]
    )
    fun testCreateBoundSet() {
        Mockito.`when`(timeService.getNowDateTime())
            .thenReturn(LocalDateTime.of(2000, 1, 1, 10, 0))

        val bounds: List<BoundDto> = listOf(
            BoundDto().type(BoundType.UPPER).action(1).coefficient(45.67f).order(1).priceType(1).rounding(999.09),
            BoundDto().type(BoundType.UPPER).action(2).coefficient(99.9956f).order(2).priceType(2).rounding(0.009),
            BoundDto().type(BoundType.LOWER).action(1).coefficient(0.1f).order(3).priceType(2).rounding(9.0),
        )

        val filters: List<BoundSetFilterDto> = listOf(
            BoundSetFilterDto().categoryIds(listOf(4L, 5L)).excludedCategoryIds(listOf(1L, 17L))
                .vendorIds(listOf(2L, 6L)).excludedVendorIds(listOf(5L, 15L)).sskus(listOf("64", "65"))
                .excludedSskus(listOf("67")).excludedMskus(listOf(67L)),
            BoundSetFilterDto().categoryIds(listOf(5L, 6L)).excludedCategoryIds(listOf(2L, 18L))
                .vendorIds(listOf(3L, 7L)).excludedVendorIds(listOf(6L, 16L)).sskus(listOf("65", "66"))
                .excludedSskus(listOf("68")).mskus(listOf(65L, 66L)).excludedMskus(listOf(68L)),
        )

        val boundSetDto: CreateBoundSetDto = CreateBoundSetDto()
            .group("Новая Группа")
            .filters(filters)
            .bounds(bounds)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/bound-sets/")
                .contentType("application/json")
                .content(dtoToString(boundSetDto))
        ).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DbUnitDataSet(
        before = ["BoundSetApiTest.getBoundSetsTest.before.csv"]
    )
    fun testGetBoundSets() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/bound-sets"),
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(2L))
            .andExpect(jsonPath("$[0].group").value("Старая Группа"))
            .andExpect(
                jsonPath("$[0].changeDescription")
                    .value("Набор границ \"Старая группа\" создан.")
            )
            .andExpect(jsonPath("$[0].changedAt").value(DateTimeTestingUtil.createJsonDateTime(1986, 8, 22, 22, 22, 4)))
            .andExpect(jsonPath("$[1].id").value(7L))
            .andExpect(jsonPath("$[1].group").value("Зеленая Группа"))
            .andExpect(
                jsonPath("$[1].changeDescription")
                    .value("Набор границ \"Зеленая группа\" создан.")
            )
            .andExpect(jsonPath("$[1].changedAt").value(DateTimeTestingUtil.createJsonDateTime(2007, 7, 7, 10, 0, 10)))

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/bound-sets/2/filters")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1L))
            .andExpect(jsonPath("$[0].categoryIds.length()").value(3))
            .andExpect(jsonPath("$[0].categoryIds[0]").value(1L))
            .andExpect(jsonPath("$[0].categoryIds[1]").value(4L))
            .andExpect(jsonPath("$[0].categoryIds[2]").value(5L))
            .andExpect(jsonPath("$[0].excludedCategoryIds").doesNotExist())
            .andExpect(jsonPath("$[0].vendorIds.length()").value(2))
            .andExpect(jsonPath("$[0].vendorIds[0]").value(2L))
            .andExpect(jsonPath("$[0].vendorIds[1]").value(6L))
            .andExpect(jsonPath("$[0].excludedVendorIds.length()").value(1))
            .andExpect(jsonPath("$[0].excludedVendorIds[0]").value(5L))
            .andExpect(jsonPath("$[0].sskus.length()").value(1))
            .andExpect(jsonPath("$[0].sskus[0]").value("1111.1111"))
            .andExpect(jsonPath("$[0].excludedSskus").doesNotExist())
            .andExpect(jsonPath("$[0].mskus.length()").value(1))
            .andExpect(jsonPath("$[0].mskus[0]").value(1111L))
            .andExpect(jsonPath("$[0].excludedMskus").doesNotExist())
            .andExpect(jsonPath("$[1].id").value(2L))
            .andExpect(jsonPath("$[1].categoryIds.length()").value(2))
            .andExpect(jsonPath("$[1].categoryIds[0]").value(2L))
            .andExpect(jsonPath("$[1].categoryIds[1]").value(6L))
            .andExpect(jsonPath("$[1].excludedCategoryIds").doesNotExist())
            .andExpect(jsonPath("$[1].vendorIds.length()").value(1))
            .andExpect(jsonPath("$[1].vendorIds[0]").value(3L))
            .andExpect(jsonPath("$[1].excludedVendorIds").doesNotExist())
            .andExpect(jsonPath("$[1].sskus.length()").value(1))
            .andExpect(jsonPath("$[1].sskus[0]").value("2222.2222"))
            .andExpect(jsonPath("$[1].excludedSskus.length()").value(1))
            .andExpect(jsonPath("$[1].excludedSskus[0]").value("3333.3333"))
            .andExpect(jsonPath("$[1].mskus.length()").value(1))
            .andExpect(jsonPath("$[1].mskus[0]").value(2222L))
            .andExpect(jsonPath("$[1].excludedMskus.length()").value(1L))
            .andExpect(jsonPath("$[1].excludedMskus[0]").value(3333L))

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/bound-sets/7/filters")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(3L))
            .andExpect(jsonPath("$[0].categoryIds.length()").value(2))
            .andExpect(jsonPath("$[0].categoryIds[0]").value(4L))
            .andExpect(jsonPath("$[0].categoryIds[1]").value(5L))
            .andExpect(jsonPath("$[0].excludedCategoryIds.length()").value(1))
            .andExpect(jsonPath("$[0].excludedCategoryIds[0]").value(1L))
            .andExpect(jsonPath("$[0].vendorIds.length()").value(1))
            .andExpect(jsonPath("$[0].vendorIds[0]").value(5L))
            .andExpect(jsonPath("$[0].excludedVendorIds.length()").value(2))
            .andExpect(jsonPath("$[0].excludedVendorIds[0]").value(2L))
            .andExpect(jsonPath("$[0].excludedVendorIds[1]").value(6L))
            .andExpect(jsonPath("$[0].sskus.length()").value(1))
            .andExpect(jsonPath("$[0].sskus[0]").value("3333.3333"))
            .andExpect(jsonPath("$[0].excludedSskus.length()").value(1))
            .andExpect(jsonPath("$[0].excludedSskus[0]").value("1111.1111"))
            .andExpect(jsonPath("$[0].mskus.length()").value(1))
            .andExpect(jsonPath("$[0].mskus[0]").value(3333L))
            .andExpect(jsonPath("$[0].excludedMskus.length()").value(1))
            .andExpect(jsonPath("$[0].excludedMskus[0]").value(1111L))
            .andExpect(jsonPath("$[1].id").value(4L))
            .andExpect(jsonPath("$[1].categoryIds.length()").value(1))
            .andExpect(jsonPath("$[1].categoryIds[0]").value(17L))
            .andExpect(jsonPath("$[1].excludedCategoryIds.length()").value(1))
            .andExpect(jsonPath("$[1].excludedCategoryIds[0]").value(6L))
            .andExpect(jsonPath("$[1].vendorIds.length()").value(2))
            .andExpect(jsonPath("$[1].vendorIds[0]").value(7L))
            .andExpect(jsonPath("$[1].vendorIds[1]").value(15L))
            .andExpect(jsonPath("$[1].excludedVendorIds.length()").value(1))
            .andExpect(jsonPath("$[1].excludedVendorIds[0]").value(5L))
            .andExpect(jsonPath("$[1].sskus").doesNotExist())
            .andExpect(jsonPath("$[1].excludedSskus").doesNotExist())
            .andExpect(jsonPath("$[1].mskus").doesNotExist())
            .andExpect(jsonPath("$[1].excludedMskus").doesNotExist())
    }

    @Test
    @DbUnitDataSet(
        before = ["BoundSetApiTest.getBoundSetsTest.before.csv"]
    )
    fun testGetBoundSetsFilteredBySsku() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/bound-sets?ssku=1111.1111")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(2))
            .andExpect(jsonPath("$[0].group").value("Старая Группа"))

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/bound-sets?ssku=3333.3333")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(2))
            .andExpect(jsonPath("$[0].group").value("Старая Группа"))
            .andExpect(jsonPath("$[1].id").value(7))
            .andExpect(jsonPath("$[1].group").value("Зеленая Группа"))

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/bound-sets?group=Зеленая Группа&ssku=3333.3333")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(7))
            .andExpect(jsonPath("$[0].group").value("Зеленая Группа"))
    }

    @Test
    @DbUnitDataSet(
        before = ["BoundSetApiTest.getBoundSetsTest.before.csv"]
    )
    fun testGetBoundSetsFilteredByMsku() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/bound-sets?msku=1111")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(2))
            .andExpect(jsonPath("$[0].group").value("Старая Группа"))

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/bound-sets?msku=3333")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(2))
            .andExpect(jsonPath("$[0].group").value("Старая Группа"))
            .andExpect(jsonPath("$[1].id").value(7))
            .andExpect(jsonPath("$[1].group").value("Зеленая Группа"))

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/bound-sets?group=Зеленая Группа&msku=3333")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(7))
            .andExpect(jsonPath("$[0].group").value("Зеленая Группа"))
    }

    @Test
    @DbUnitDataSet(
        before = ["BoundSetApiTest.getBoundSetsTest.before.csv"]
    )
    fun testGetBoundSetsFilteredByPageCount() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/bound-sets?page=2&count=1")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(7))
            .andExpect(jsonPath("$[0].group").value("Зеленая Группа"))
    }

    @Test
    @DbUnitDataSet(
        before = ["BoundSetApiTest.getBoundSetsTest.before.csv"]
    )
    fun testGetBoundSetsFilteredByAll() {
        mockMvc.perform(
            MockMvcRequestBuilders.get(
                "/api/v1/bound-sets" +
                    "?group=Старая Группа" +
                    "&ssku=3333.3333" +
                    "&msku=3333" +
                    "&categoryId=4" +
                    "&vendorId=2" +
                    "&page=1" +
                    "&count=1"
            )
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(2))
            .andExpect(jsonPath("$[0].group").value("Старая Группа"))
    }

    @Test
    @DbUnitDataSet(
        before = ["Bounds.dictionary.csv", "BoundSetApiTest.boundSetUpdateTest.before.csv"],
        after = ["BoundSetApiTest.boundSetUpdateAllTest.after.csv"]
    )
    fun testUpdateBoundSetAll() {
        Mockito.`when`(timeService.getNowDateTime())
            .thenReturn(LocalDateTime.of(2020, 2, 20, 20, 20, 20))

        val bounds = listOf(
            BoundDto().id(14).type(BoundType.UPPER).action(1L).coefficient(45.67f).order(1).priceType(1L)
                .rounding(99.99),
            BoundDto().type(BoundType.LOWER).action(1L).coefficient(99.9956f).order(2).priceType(1L).rounding(9.009),
            BoundDto().type(BoundType.UPPER).action(2L).coefficient(0.1f).order(3)
                .priceType(1L).rounding(0.9),
        )

        val filters: List<BoundSetFilterDto> = listOf(
            BoundSetFilterDto().id(3).categoryIds(listOf(4L, 5L)).excludedCategoryIds(listOf(1L))
                .vendorIds(listOf(2L, 5L, 6L)).excludedVendorIds(listOf(8L, 16L)).sskus(listOf("67", "62"))
                .excludedSskus(listOf("65", "61")).mskus(listOf(67L, 62L)).excludedMskus(listOf(65L, 61L)),
            BoundSetFilterDto().categoryIds(listOf(5L, 6L)).excludedCategoryIds(listOf(2L))
                .vendorIds(listOf(3L, 6L, 7L)).excludedVendorIds(listOf(8L, 17L)).sskus(listOf("68", "63"))
                .excludedSskus(listOf("66", "62")).mskus(listOf(67L, 63L)).excludedMskus(listOf(66L, 62L)),
        )

        val boundSetDto: CreateBoundSetDto = CreateBoundSetDto()
            .group("Новая Группа")
            .filters(filters)
            .bounds(bounds)

        mockMvc.perform(
            MockMvcRequestBuilders.put("/api/v1/bound-sets/2")
                .contentType("application/json")
                .content(dtoToString(boundSetDto))
        ).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DbUnitDataSet(
        before = ["Bounds.dictionary.csv", "BoundSetApiTest.boundSetUpdateTest.before.csv"],
        after = ["BoundSetApiTest.boundSetUpdateDeletedTest.after.csv"]
    )
    fun testUpdateBoundSetDeleted() {
        Mockito.`when`(timeService.getNowDateTime())
            .thenReturn(LocalDateTime.of(2020, 2, 20, 20, 20, 20))

        val bounds = listOf<BoundDto>()

        val filters: List<BoundSetFilterDto> = listOf(
            BoundSetFilterDto().categoryIds(listOf(4L)).vendorIds(listOf(2L)),
        )

        val boundSetDto: CreateBoundSetDto = CreateBoundSetDto()
            .filters(filters)
            .bounds(bounds)

        mockMvc.perform(
            MockMvcRequestBuilders.put("/api/v1/bound-sets/2")
                .contentType("application/json")
                .content(dtoToString(boundSetDto))
        ).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DbUnitDataSet(
        before = ["Bounds.dictionary.csv", "BoundSetApiTest.boundSetUpdateTest.before.csv"],
        after = ["BoundSetApiTest.boundSetUpdateBoundDeletedTest.after.csv"]
    )
    fun testUpdateBoundSetBoundDeleted() {
        Mockito.`when`(timeService.getNowDateTime())
            .thenReturn(LocalDateTime.of(2020, 2, 20, 20, 20, 20))

        val bounds = listOf(
            BoundDto().id(13).type(BoundType.UPPER).action(1L).coefficient(45.67f).order(1).priceType(1)
                .rounding(99.99),
        )

        val filters: List<BoundSetFilterDto> = listOf(
            BoundSetFilterDto().categoryIds(listOf(1L, 5L)).excludedCategoryIds(listOf(4L)).vendorIds(listOf(2L))
                .excludedVendorIds(listOf(5L, 6L)).sskus(listOf("67")).excludedSskus(listOf("64"))
                .mskus(listOf(67L)).excludedMskus(listOf(64L)),
            BoundSetFilterDto().categoryIds(listOf(2L, 6L)).vendorIds(listOf(3L)),
        )

        val boundSetDto: CreateBoundSetDto = CreateBoundSetDto()
            .filters(filters)
            .bounds(bounds)

        mockMvc.perform(
            MockMvcRequestBuilders.put("/api/v1/bound-sets/2")
                .contentType("application/json")
                .content(dtoToString(boundSetDto))
        ).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DbUnitDataSet(
        before = ["Bounds.dictionary.csv", "BoundSetApiTest.boundSetUpdateTest.before.csv"]
    )
    fun testUpdateBoundSetFailedNonExistentBoundSet() {
        val bounds = listOf<BoundDto>()

        val filters: List<BoundSetFilterDto> = listOf(
            BoundSetFilterDto().categoryIds(listOf(1L)).vendorIds(listOf(2L))
        )

        val boundSetDto: CreateBoundSetDto = CreateBoundSetDto()
            .filters(filters)
            .bounds(bounds)

        mockMvc.perform(
            MockMvcRequestBuilders.put("/api/v1/bound-sets/3")
                .contentType("application/json")
                .content(dtoToString(boundSetDto))
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(jsonPath("$").value("Набор границ с указанным id: 3 не существует."))
    }

    @Test
    @DbUnitDataSet(
        before = ["BoundSetApiTest.searchBoundSetsTest.before.csv"]
    )
    fun testSearchShortBoundSets() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/bound-sets/search")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(7))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].group").value("Старая Группа"))
            .andExpect(jsonPath("$[1].id").value(13))
            .andExpect(jsonPath("$[1].group").value("Abracadabra"))
            .andExpect(jsonPath("$[2].id").value(32))
            .andExpect(jsonPath("$[2].group").value("aBra"))
            .andExpect(jsonPath("$[3].id").value(51))
            .andExpect(jsonPath("$[3].group").value("boop"))
            .andExpect(jsonPath("$[4].id").value(60))
            .andExpect(jsonPath("$[4].group").value("A"))
            .andExpect(jsonPath("$[5].id").value(78))
            .andExpect(jsonPath("$[5].group").value("ABRIKOS"))
            .andExpect(jsonPath("$[6].id").value(100))
            .andExpect(jsonPath("$[6].group").value("aBBraa"))
    }

    @Test
    @DbUnitDataSet(
        before = ["BoundSetApiTest.searchBoundSetsTest.before.csv"]
    )
    fun testSearchShortBoundSetsWithPrefix() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/bound-sets/search?group=abr")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].id").value(13))
            .andExpect(jsonPath("$[0].group").value("Abracadabra"))
            .andExpect(jsonPath("$[1].id").value(32))
            .andExpect(jsonPath("$[1].group").value("aBra"))
            .andExpect(jsonPath("$[2].id").value(78))
            .andExpect(jsonPath("$[2].group").value("ABRIKOS"))
    }


    @Test
    @DbUnitDataSet(
        before = ["Bounds.dictionary.csv", "BoundSetApiTest.boundSetUpdateTest.before.csv"],
        after = ["BoundSetApiTest.boundSetUpdateDeletedTest.after.csv"]
    )
    fun testDeleteBoundSet() {
        Mockito.`when`(timeService.getNowDateTime())
            .thenReturn(LocalDateTime.of(2020, 2, 20, 20, 20, 20))

        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/bound-sets/2")
        ).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DbUnitDataSet(
        before = ["BoundSetApiTest.getBoundSetHistoryTest.before.csv"]
    )
    fun testGetBoundSetHistory() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/bound-sets/3/history")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(4))
            .andExpect(jsonPath("$[0].id").value(7L))
            .andExpect(jsonPath("$[0].userId").value(1L))
            .andExpect(jsonPath("$[0].changedAt").value(DateTimeTestingUtil.createJsonDateTime(2000, 2, 2, 22, 22, 3)))
            .andExpect(jsonPath("$[0].changeDescription").value("N"))
            .andExpect(jsonPath("$[1].id").value(1L))
            .andExpect(jsonPath("$[1].userId").value(1L))
            .andExpect(jsonPath("$[1].changedAt").value(DateTimeTestingUtil.createJsonDateTime(2000, 2, 2, 22, 22, 2)))
            .andExpect(jsonPath("$[1].changeDescription").value("I"))
            .andExpect(jsonPath("$[2].id").value(14L))
            .andExpect(jsonPath("$[2].userId").value(1L))
            .andExpect(jsonPath("$[2].changedAt").value(DateTimeTestingUtil.createJsonDateTime(2000, 2, 2, 22, 22, 1)))
            .andExpect(jsonPath("$[2].changeDescription").value("C"))
            .andExpect(jsonPath("$[3].id").value(5L))
            .andExpect(jsonPath("$[3].userId").value(1L))
            .andExpect(jsonPath("$[3].changedAt").value(DateTimeTestingUtil.createJsonDateTime(2000, 2, 2, 22, 22, 0)))
            .andExpect(jsonPath("$[3].changeDescription").value("E"))
    }

    @Test
    @DbUnitDataSet(
        before = ["BoundSetApiTest.getBoundSetsTest.before.csv"]
    )
    fun testGetBoundSet() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/bound-sets/2")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.id").value(2L))
            .andExpect(jsonPath("$.group").value("Старая Группа"))
            .andExpect(
                jsonPath("$.changeDescription")
                    .value("Набор границ \"Старая группа\" создан.")
            )
            .andExpect(jsonPath("$.changedAt").value(DateTimeTestingUtil.createJsonDateTime(1986, 8, 22, 22, 22, 4)))

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/bound-sets/2/filters")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1L))
            .andExpect(jsonPath("$[0].categoryIds.length()").value(3))
            .andExpect(jsonPath("$[0].categoryIds[0]").value(1L))
            .andExpect(jsonPath("$[0].categoryIds[1]").value(4L))
            .andExpect(jsonPath("$[0].categoryIds[2]").value(5L))
            .andExpect(jsonPath("$[0].excludedCategoryIds").doesNotExist())
            .andExpect(jsonPath("$[0].vendorIds.length()").value(2))
            .andExpect(jsonPath("$[0].vendorIds[0]").value(2L))
            .andExpect(jsonPath("$[0].vendorIds[1]").value(6L))
            .andExpect(jsonPath("$[0].excludedVendorIds.length()").value(1))
            .andExpect(jsonPath("$[0].excludedVendorIds[0]").value(5L))
            .andExpect(jsonPath("$[0].sskus.length()").value(1))
            .andExpect(jsonPath("$[0].sskus[0]").value("1111.1111"))
            .andExpect(jsonPath("$[0].excludedSskus").doesNotExist())
            .andExpect(jsonPath("$[0].mskus.length()").value(1))
            .andExpect(jsonPath("$[0].mskus[0]").value(1111L))
            .andExpect(jsonPath("$[0].excludedMskus").doesNotExist())
            .andExpect(jsonPath("$[1].id").value(2L))
            .andExpect(jsonPath("$[1].categoryIds.length()").value(2))
            .andExpect(jsonPath("$[1].categoryIds[0]").value(2L))
            .andExpect(jsonPath("$[1].categoryIds[1]").value(6L))
            .andExpect(jsonPath("$[1].excludedCategoryIds").doesNotExist())
            .andExpect(jsonPath("$[1].vendorIds.length()").value(1))
            .andExpect(jsonPath("$[1].vendorIds[0]").value(3L))
            .andExpect(jsonPath("$[1].excludedVendorIds").doesNotExist())
            .andExpect(jsonPath("$[1].sskus.length()").value(1))
            .andExpect(jsonPath("$[1].sskus[0]").value("2222.2222"))
            .andExpect(jsonPath("$[1].excludedSskus.length()").value(1))
            .andExpect(jsonPath("$[1].excludedSskus[0]").value("3333.3333"))
            .andExpect(jsonPath("$[1].mskus.length()").value(1))
            .andExpect(jsonPath("$[1].mskus[0]").value(2222L))
            .andExpect(jsonPath("$[1].excludedMskus.length()").value(1L))
            .andExpect(jsonPath("$[1].excludedMskus[0]").value(3333L))
    }

    @Test
    @DbUnitDataSet(
        before = ["BoundSetApiTest.getBoundSetsCountTest.before.csv"]
    )
    fun testGetBoundSetsCount() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/bound-sets/count")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$").value(4))
    }

    @Test
    @DbUnitDataSet(
        before = ["BoundSetApiTest.getBoundSetsCountTest.before.csv"]
    )
    fun testGetBoundSetsCountFilteredBySsku() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/bound-sets/count?ssku=1111.1111")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$").value(2))
    }

    @Test
    @DbUnitDataSet(
        before = ["BoundSetApiTest.getBoundSetsCountTest.before.csv"]
    )
    fun testGetBoundSetsCountFilteredByMsku() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/bound-sets/count?msku=1111")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$").value(2))
    }

    @Test
    @DbUnitDataSet(
        before = ["BoundSetApiTest.getBoundSetsCountTest.before.csv"]
    )
    fun testGetBoundSetsCountFilteredByCategoryVendorId() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/bound-sets/count?categoryId=5&vendorId=6")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$").value(1))
    }

    @Test
    @DbUnitDataSet(
        before = ["BoundSetApiTest.getBoundSetsCountTest.before.csv"]
    )
    fun testGetBoundSetsCountFilteredByAll() {
        mockMvc.perform(
            MockMvcRequestBuilders.get(
                "/api/v1/bound-sets/count" +
                    "?group=Elephant" +
                    "&ssku=3333.3333" +
                    "&msku=3333" +
                    "&categoryId=4" +
                    "&vendorId=6"
            )
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$").value(1))
    }
}
