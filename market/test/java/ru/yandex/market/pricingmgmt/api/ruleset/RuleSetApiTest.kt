package ru.yandex.market.pricingmgmt.api.ruleset

import org.junit.jupiter.api.Test
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
import ru.yandex.mj.generated.server.model.CreateRuleSetDto
import ru.yandex.mj.generated.server.model.RuleDto
import ru.yandex.mj.generated.server.model.RuleSetFilterDto
import java.time.LocalDateTime

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
class RuleSetApiTest : ControllerTest() {

    @MockBean
    private lateinit var timeService: TimeService

    @Test
    @DbUnitDataSet(
        before = ["Rules.dictionary.csv", "RuleSetApiTest.getRulesTest.before.csv"]
    )
    fun testGetRules() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/rule-sets/1/rules")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].id").value(100L))
            .andExpect(jsonPath("$[0].type").value(4L))
            .andExpect(jsonPath("$[0].action").value(2L))
            .andExpect(jsonPath("$[0].coefficient").value(45.67f))
            .andExpect(jsonPath("$[0].order").value(1))
            .andExpect(jsonPath("$[0].priceType").value(2L))
            .andExpect(jsonPath("$[0].rounding").value(99.0))
            .andExpect(jsonPath("$[1].id").value(203L))
            .andExpect(jsonPath("$[1].type").value(6L))
            .andExpect(jsonPath("$[1].action").value(2L))
            .andExpect(jsonPath("$[1].coefficient").value(99.9956f))
            .andExpect(jsonPath("$[1].order").value(2))
            .andExpect(jsonPath("$[1].priceType").value(2L))
            .andExpect(jsonPath("$[1].rounding").value(0.009))
            .andExpect(jsonPath("$[2].id").value(34L))
            .andExpect(jsonPath("$[2].type").value(5L))
            .andExpect(jsonPath("$[2].action").value(1L))
            .andExpect(jsonPath("$[2].coefficient").value(99.0f))
            .andExpect(jsonPath("$[2].order").value(3))
            .andExpect(jsonPath("$[2].priceType").value(1L))
            .andExpect(jsonPath("$[2].rounding").value(0.99))
    }

    @Test
    @DbUnitDataSet(
        before = ["Rules.dictionary.csv", "RuleSetApiTest.ruleSetCreationTest.before.csv"],
        after = ["RuleSetApiTest.ruleSetCreationTest.after.csv"]
    )
    fun testCreateRuleSet() {
        Mockito.`when`(timeService.getNowDateTime())
            .thenReturn(LocalDateTime.of(2000, 1, 1, 10, 0))

        val rules: List<RuleDto> = listOf(
            RuleDto().type(1).action(1).coefficient(45.67f).order(1).priceType(1).rounding(999.09),
            RuleDto().type(2).action(2).coefficient(99.9956f).order(2).priceType(2).rounding(0.009),
            RuleDto().type(3).action(1).coefficient(0.1f).order(3).priceType(2).rounding(9.0),
        )

        val filters: List<RuleSetFilterDto> = listOf(
            RuleSetFilterDto().categoryIds(listOf(4L, 5L)).excludedCategoryIds(listOf(1L, 17L))
                .vendorIds(listOf(2L, 6L)).excludedVendorIds(listOf(5L, 15L)).sskus(listOf("64", "65"))
                .excludedSskus(listOf("67")).excludedMskus(listOf(67L)),
            RuleSetFilterDto().categoryIds(listOf(5L, 6L)).excludedCategoryIds(listOf(2L, 18L))
                .vendorIds(listOf(3L, 7L)).excludedVendorIds(listOf(6L, 16L)).sskus(listOf("65", "66"))
                .excludedSskus(listOf("68")).mskus(listOf(65L, 66L)).excludedMskus(listOf(68L)),
        )

        val ruleSetDto: CreateRuleSetDto = CreateRuleSetDto()
            .group("Новая Группа")
            .filters(filters)
            .rules(rules)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/rule-sets/")
                .contentType("application/json")
                .content(dtoToString(ruleSetDto))
        ).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DbUnitDataSet(before = ["RuleSetApiTest.testCollision.category.csv"])
    fun testCollisionOnCategories() {
        Mockito.`when`(timeService.getNowDateTime())
            .thenReturn(LocalDateTime.of(2000, 1, 1, 10, 0))

        val rules: List<RuleDto> = listOf(
            RuleDto().type(1).action(1).coefficient(45.67f).order(1).priceType(1).rounding(999.09),
            RuleDto().type(2).action(2).coefficient(99.9956f).order(2).priceType(2).rounding(0.009),
            RuleDto().type(3).action(1).coefficient(0.1f).order(3).priceType(2).rounding(9.0),
        )

        val filters: List<RuleSetFilterDto> = listOf(
            RuleSetFilterDto().categoryIds(listOf(65L)).excludedCategoryIds(listOf(64L))
        )

        val ruleSetDto: CreateRuleSetDto = CreateRuleSetDto()
            .group("Новая Группа")
            .filters(filters)
            .rules(rules)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/rule-sets/")
                .contentType("application/json")
                .content(dtoToString(ruleSetDto))
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.content().string("Фильтр №1 конфликтует со следующими наборами правил: 5"))

    }

    @Test
    @DbUnitDataSet(before = ["RuleSetApiTest.testCollision.category.csv"])
    fun testOkOnCategoriesExcludedMsku() {
        Mockito.`when`(timeService.getNowDateTime())
            .thenReturn(LocalDateTime.of(2000, 1, 1, 10, 0))

        val rules: List<RuleDto> = listOf(
            RuleDto().type(1).action(1).coefficient(45.67f).order(1).priceType(1).rounding(999.09),
            RuleDto().type(2).action(2).coefficient(99.9956f).order(2).priceType(2).rounding(0.009),
            RuleDto().type(3).action(1).coefficient(0.1f).order(3).priceType(2).rounding(9.0),
        )

        val filters: List<RuleSetFilterDto> = listOf(
            RuleSetFilterDto().categoryIds(listOf(65L)).excludedCategoryIds(listOf(64L)).excludedMskus(listOf(65L))
        )

        val ruleSetDto: CreateRuleSetDto = CreateRuleSetDto()
            .group("Новая Группа")
            .filters(filters)
            .rules(rules)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/rule-sets/")
                .contentType("application/json")
                .content(dtoToString(ruleSetDto))
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.content().string("Категории с указанными id: [65, 64] не существуют."))
    }

    @Test
    @DbUnitDataSet(before = ["RuleSetApiTest.testCollision.category.csv"])
    fun testOkOnCategoriesExcludedSsku() {
        Mockito.`when`(timeService.getNowDateTime())
            .thenReturn(LocalDateTime.of(2000, 1, 1, 10, 0))

        val rules: List<RuleDto> = listOf(
            RuleDto().type(1).action(1).coefficient(45.67f).order(1).priceType(1).rounding(999.09),
            RuleDto().type(2).action(2).coefficient(99.9956f).order(2).priceType(2).rounding(0.009),
            RuleDto().type(3).action(1).coefficient(0.1f).order(3).priceType(2).rounding(9.0),
        )

        val filters: List<RuleSetFilterDto> = listOf(
            RuleSetFilterDto().categoryIds(listOf(65L)).excludedCategoryIds(listOf(64L)).excludedSskus(listOf("1.65"))
        )

        val ruleSetDto: CreateRuleSetDto = CreateRuleSetDto()
            .group("Новая Группа")
            .filters(filters)
            .rules(rules)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/rule-sets/")
                .contentType("application/json")
                .content(dtoToString(ruleSetDto))
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.content().string("Категории с указанными id: [65, 64] не существуют."))
    }

    @Test
    @DbUnitDataSet(before = ["RuleSetApiTest.testCollision.category.csv"])
    fun testCollisionOnVendors() {
        Mockito.`when`(timeService.getNowDateTime())
            .thenReturn(LocalDateTime.of(2000, 1, 1, 10, 0))

        val rules: List<RuleDto> = listOf(
            RuleDto().type(1).action(1).coefficient(45.67f).order(1).priceType(1).rounding(999.09),
            RuleDto().type(2).action(2).coefficient(99.9956f).order(2).priceType(2).rounding(0.009),
            RuleDto().type(3).action(1).coefficient(0.1f).order(3).priceType(2).rounding(9.0),
        )

        val filters: List<RuleSetFilterDto> = listOf(
            RuleSetFilterDto().vendorIds(listOf(64L)).excludedVendorIds(listOf(65L))
        )

        val ruleSetDto: CreateRuleSetDto = CreateRuleSetDto()
            .group("Новая Группа")
            .filters(filters)
            .rules(rules)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/rule-sets/")
                .contentType("application/json")
                .content(dtoToString(ruleSetDto))
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.content().string("Фильтр №1 конфликтует со следующими наборами правил: 5"))

    }

    @Test
    @DbUnitDataSet(before = ["RuleSetApiTest.testCollision.category.csv"])
    fun testCollisionOnMsku() {
        Mockito.`when`(timeService.getNowDateTime())
            .thenReturn(LocalDateTime.of(2000, 1, 1, 10, 0))

        val rules: List<RuleDto> = listOf(
            RuleDto().type(1).action(1).coefficient(45.67f).order(1).priceType(1).rounding(999.09),
            RuleDto().type(2).action(2).coefficient(99.9956f).order(2).priceType(2).rounding(0.009),
            RuleDto().type(3).action(1).coefficient(0.1f).order(3).priceType(2).rounding(9.0),
        )

        val filters: List<RuleSetFilterDto> = listOf(
            RuleSetFilterDto().mskus(listOf(65L))
        )

        val ruleSetDto: CreateRuleSetDto = CreateRuleSetDto()
            .group("Новая Группа")
            .filters(filters)
            .rules(rules)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/rule-sets/")
                .contentType("application/json")
                .content(dtoToString(ruleSetDto))
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.content().string("Фильтр №1 конфликтует со следующими наборами правил: 5"))

    }

    @Test
    @DbUnitDataSet(before = ["RuleSetApiTest.testCollision.category.csv"])
    fun testCollisionOnSsku() {
        Mockito.`when`(timeService.getNowDateTime())
            .thenReturn(LocalDateTime.of(2000, 1, 1, 10, 0))

        val rules: List<RuleDto> = listOf(
            RuleDto().type(1).action(1).coefficient(45.67f).order(1).priceType(1).rounding(999.09),
            RuleDto().type(2).action(2).coefficient(99.9956f).order(2).priceType(2).rounding(0.009),
            RuleDto().type(3).action(1).coefficient(0.1f).order(3).priceType(2).rounding(9.0),
        )

        val filters: List<RuleSetFilterDto> = listOf(
            RuleSetFilterDto().sskus(listOf("1.65"))
        )

        val ruleSetDto: CreateRuleSetDto = CreateRuleSetDto()
            .group("Новая Группа")
            .filters(filters)
            .rules(rules)

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/rule-sets/")
                .contentType("application/json")
                .content(dtoToString(ruleSetDto))
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.content().string("Фильтр №1 конфликтует со следующими наборами правил: 5"))

    }

    @Test
    @DbUnitDataSet(
        before = ["RuleSetApiTest.getRuleSetsTest.before.csv"]
    )
    fun testGetRuleSets() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/rule-sets"),
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(2L))
            .andExpect(jsonPath("$[0].group").value("Старая Группа"))
            .andExpect(
                jsonPath("$[0].changeDescription")
                    .value("Группа правил \"Старая группа\" создана.")
            )
            .andExpect(jsonPath("$[0].changedAt").value(DateTimeTestingUtil.createJsonDateTime(1986, 8, 22, 22, 22, 4)))
            .andExpect(jsonPath("$[1].id").value(7L))
            .andExpect(jsonPath("$[1].group").value("Зеленая Группа"))
            .andExpect(
                jsonPath("$[1].changeDescription")
                    .value("Группа правил \"Зеленая группа\" создана.")
            )
            .andExpect(jsonPath("$[1].changedAt").value(DateTimeTestingUtil.createJsonDateTime(2007, 7, 7, 10, 0, 10)))

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/rule-sets/2/filters")
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
            MockMvcRequestBuilders.get("/api/v1/rule-sets/7/filters")
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
        before = ["RuleSetApiTest.getRuleSetsTest.before.csv"]
    )
    fun testGetRuleSetsFilteredBySsku() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/rule-sets?ssku=1111.1111")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(2))
            .andExpect(jsonPath("$[0].group").value("Старая Группа"))

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/rule-sets?ssku=3333.3333")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(2))
            .andExpect(jsonPath("$[0].group").value("Старая Группа"))
            .andExpect(jsonPath("$[1].id").value(7))
            .andExpect(jsonPath("$[1].group").value("Зеленая Группа"))

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/rule-sets?group=Зеленая Группа&ssku=3333.3333")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(7))
            .andExpect(jsonPath("$[0].group").value("Зеленая Группа"))
    }

    @Test
    @DbUnitDataSet(
        before = ["RuleSetApiTest.getRuleSetsTest.before.csv"]
    )
    fun testGetRuleSetsFilteredByMsku() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/rule-sets?msku=1111")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(2))
            .andExpect(jsonPath("$[0].group").value("Старая Группа"))

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/rule-sets?msku=3333")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(2))
            .andExpect(jsonPath("$[0].group").value("Старая Группа"))
            .andExpect(jsonPath("$[1].id").value(7))
            .andExpect(jsonPath("$[1].group").value("Зеленая Группа"))

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/rule-sets?group=Зеленая Группа&msku=3333")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(7))
            .andExpect(jsonPath("$[0].group").value("Зеленая Группа"))
    }

    @Test
    @DbUnitDataSet(
        before = ["RuleSetApiTest.getRuleSetsTest.before.csv"]
    )
    fun testGetRuleSetsFilteredByPageCount() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/rule-sets?page=2&count=1")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(7))
            .andExpect(jsonPath("$[0].group").value("Зеленая Группа"))
    }

    @Test
    @DbUnitDataSet(
        before = ["RuleSetApiTest.getRuleSetsTest.before.csv"]
    )
    fun testGetRuleSetsFilteredByAll() {
        mockMvc.perform(
            MockMvcRequestBuilders.get(
                "/api/v1/rule-sets" +
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
        before = ["Rules.dictionary.csv", "RuleSetApiTest.ruleSetUpdateTest.before.csv"],
        after = ["RuleSetApiTest.ruleSetUpdateAllTest.after.csv"]
    )
    fun testUpdateRuleSetAll() {
        Mockito.`when`(timeService.getNowDateTime())
            .thenReturn(LocalDateTime.of(2020, 2, 20, 20, 20, 20))

        val rules = listOf(
            RuleDto().id(14).type(1L).action(1L).coefficient(45.67f).order(1).priceType(1L).rounding(99.99),
            RuleDto().type(5L).action(1L).coefficient(99.9956f).order(2).priceType(1L).rounding(9.009),
            RuleDto().type(3L).action(2L).coefficient(0.1f).order(3).priceType(1L).rounding(0.9),
        )

        val filters: List<RuleSetFilterDto> = listOf(
            RuleSetFilterDto().id(3).categoryIds(listOf(4L, 5L)).excludedCategoryIds(listOf(1L))
                .vendorIds(listOf(2L, 5L, 6L)).excludedVendorIds(listOf(8L, 16L)).sskus(listOf("67", "62"))
                .excludedSskus(listOf("65", "61")).mskus(listOf(67L, 62L)).excludedMskus(listOf(65L, 61L)),
            RuleSetFilterDto().categoryIds(listOf(5L, 6L)).excludedCategoryIds(listOf(2L))
                .vendorIds(listOf(3L, 6L, 7L)).excludedVendorIds(listOf(8L, 17L)).sskus(listOf("68", "63"))
                .excludedSskus(listOf("66", "62")).mskus(listOf(67L, 63L)).excludedMskus(listOf(66L, 62L)),
        )

        val ruleSetDto: CreateRuleSetDto = CreateRuleSetDto()
            .group("Новая Группа")
            .filters(filters)
            .rules(rules)

        mockMvc.perform(
            MockMvcRequestBuilders.put("/api/v1/rule-sets/2")
                .contentType("application/json")
                .content(dtoToString(ruleSetDto))
        ).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DbUnitDataSet(
        before = ["Rules.dictionary.csv", "RuleSetApiTest.ruleSetUpdateTest.before.csv"],
        after = ["RuleSetApiTest.ruleSetUpdateDeletedTest.after.csv"]
    )
    fun testUpdateRuleSetDeleted() {
        Mockito.`when`(timeService.getNowDateTime())
            .thenReturn(LocalDateTime.of(2020, 2, 20, 20, 20, 20))

        val rules = listOf<RuleDto>()

        val filters: List<RuleSetFilterDto> = listOf(
            RuleSetFilterDto().categoryIds(listOf(4L)).vendorIds(listOf(2L)),
        )

        val ruleSetDto: CreateRuleSetDto = CreateRuleSetDto()
            .filters(filters)
            .rules(rules)

        mockMvc.perform(
            MockMvcRequestBuilders.put("/api/v1/rule-sets/2")
                .contentType("application/json")
                .content(dtoToString(ruleSetDto))
        ).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DbUnitDataSet(
        before = ["Rules.dictionary.csv", "RuleSetApiTest.ruleSetUpdateTest.before.csv"],
        after = ["RuleSetApiTest.ruleSetUpdateRuleDeletedTest.after.csv"]
    )
    fun testUpdateRuleSetRuleDeleted() {
        Mockito.`when`(timeService.getNowDateTime())
            .thenReturn(LocalDateTime.of(2020, 2, 20, 20, 20, 20))

        val rules = listOf(
            RuleDto().id(13).type(1L).action(1L).coefficient(45.67f).order(1).priceType(1).rounding(99.99),
        )

        val filters: List<RuleSetFilterDto> = listOf(
            RuleSetFilterDto().categoryIds(listOf(1L, 5L)).excludedCategoryIds(listOf(4L)).vendorIds(listOf(2L))
                .excludedVendorIds(listOf(5L, 6L)).sskus(listOf("67")).excludedSskus(listOf("64"))
                .mskus(listOf(67L)).excludedMskus(listOf(64L)),
            RuleSetFilterDto().categoryIds(listOf(2L, 6L)).vendorIds(listOf(3L)),
        )

        val ruleSetDto: CreateRuleSetDto = CreateRuleSetDto()
            .filters(filters)
            .rules(rules)

        mockMvc.perform(
            MockMvcRequestBuilders.put("/api/v1/rule-sets/2")
                .contentType("application/json")
                .content(dtoToString(ruleSetDto))
        ).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DbUnitDataSet(
        before = ["Rules.dictionary.csv", "RuleSetApiTest.ruleSetUpdateTest.before.csv"]
    )
    fun testUpdateRuleSetFailedNonExistentRuleSet() {
        val rules = listOf<RuleDto>()

        val filters: List<RuleSetFilterDto> = listOf(
            RuleSetFilterDto().categoryIds(listOf(1L)).vendorIds(listOf(2L))
        )

        val ruleSetDto: CreateRuleSetDto = CreateRuleSetDto()
            .filters(filters)
            .rules(rules)

        mockMvc.perform(
            MockMvcRequestBuilders.put("/api/v1/rule-sets/3")
                .contentType("application/json")
                .content(dtoToString(ruleSetDto))
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(jsonPath("$").value("Набор правил с указанным id: 3 не существует."))
    }

    @Test
    @DbUnitDataSet(
        before = ["RuleSetApiTest.searchRuleSetsTest.before.csv"]
    )
    fun testSearchShortRuleSets() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/rule-sets/search")
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
        before = ["RuleSetApiTest.searchRuleSetsTest.before.csv"]
    )
    fun testSearchShortRuleSetsWithPrefix() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/rule-sets/search?group=abr")
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
        before = ["Rules.dictionary.csv", "RuleSetApiTest.ruleSetUpdateTest.before.csv"],
        after = ["RuleSetApiTest.ruleSetUpdateDeletedTest.after.csv"]
    )
    fun testDeleteRuleSet() {
        Mockito.`when`(timeService.getNowDateTime())
            .thenReturn(LocalDateTime.of(2020, 2, 20, 20, 20, 20))

        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/rule-sets/2")
        ).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DbUnitDataSet(
        before = ["Rules.dictionary.csv"]
    )
    fun testGetRuleTypesFromDictionary() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/rule-sets/rule-types")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(7))
            .andExpect(jsonPath("$[0].id").value(1L))
            .andExpect(jsonPath("$[0].value").value("fix_price"))
            .andExpect(jsonPath("$[1].id").value(2L))
            .andExpect(jsonPath("$[1].value").value("ozon_min_price"))
            .andExpect(jsonPath("$[2].id").value(3L))
            .andExpect(jsonPath("$[2].value").value("purchase_price"))
            .andExpect(jsonPath("$[3].id").value(4L))
            .andExpect(jsonPath("$[3].value").value("ref_min_price"))
            .andExpect(jsonPath("$[4].id").value(5L))
            .andExpect(jsonPath("$[4].value").value("ref_min_regular_price"))
            .andExpect(jsonPath("$[5].id").value(6L))
            .andExpect(jsonPath("$[5].value").value("ref_min_soft_price"))
            .andExpect(jsonPath("$[6].id").value(7L))
            .andExpect(jsonPath("$[6].value").value("vendor_price"))
    }

    @Test
    @DbUnitDataSet(
        before = ["Rules.dictionary.csv"]
    )
    fun testGetRuleActionsFromDictionary() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/rule-sets/rule-actions")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1L))
            .andExpect(jsonPath("$[0].value").value("margin"))
            .andExpect(jsonPath("$[1].id").value(2L))
            .andExpect(jsonPath("$[1].value").value("markup"))
    }

    @Test
    @DbUnitDataSet(
        before = ["Rules.dictionary.csv"]
    )
    fun testGetRulePriceTypesFromDictionary() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/rule-sets/rule-price-types")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1L))
            .andExpect(jsonPath("$[0].value").value("crossed"))
            .andExpect(jsonPath("$[1].id").value(2L))
            .andExpect(jsonPath("$[1].value").value("sell"))
    }

    @Test
    @DbUnitDataSet(
        before = ["RuleSetApiTest.getRuleSetHistoryTest.before.csv"]
    )
    fun testGetRuleSetHistory() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/rule-sets/3/history")
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
        before = ["RuleSetApiTest.getRuleSetsTest.before.csv"]
    )
    fun testGetRuleSet() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/rule-sets/2")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.id").value(2L))
            .andExpect(jsonPath("$.group").value("Старая Группа"))
            .andExpect(
                jsonPath("$.changeDescription")
                    .value("Группа правил \"Старая группа\" создана.")
            )
            .andExpect(jsonPath("$.changedAt").value(DateTimeTestingUtil.createJsonDateTime(1986, 8, 22, 22, 22, 4)))

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/rule-sets/2/filters")
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
        before = ["RuleSetApiTest.getRuleSetsCountTest.before.csv"]
    )
    fun testGetRuleSetsCount() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/rule-sets/count")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$").value(4))
    }

    @Test
    @DbUnitDataSet(
        before = ["RuleSetApiTest.getRuleSetsCountTest.before.csv"]
    )
    fun testGetRuleSetsCountFilteredBySsku() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/rule-sets/count?ssku=1111.1111")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$").value(2))
    }

    @Test
    @DbUnitDataSet(
        before = ["RuleSetApiTest.getRuleSetsCountTest.before.csv"]
    )
    fun testGetRuleSetsCountFilteredByMsku() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/rule-sets/count?msku=1111")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$").value(2))
    }

    @Test
    @DbUnitDataSet(
        before = ["RuleSetApiTest.getRuleSetsCountTest.before.csv"]
    )
    fun testGetRuleSetsCountFilteredByCategoryVendorId() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/rule-sets/count?categoryId=5&vendorId=6")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$").value(1))
    }

    @Test
    @DbUnitDataSet(
        before = ["RuleSetApiTest.getRuleSetsCountTest.before.csv"]
    )
    fun testGetRuleSetsCountFilteredByAll() {
        mockMvc.perform(
            MockMvcRequestBuilders.get(
                "/api/v1/rule-sets/count" +
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
