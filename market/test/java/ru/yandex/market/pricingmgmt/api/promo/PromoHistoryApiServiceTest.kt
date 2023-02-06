package ru.yandex.market.pricingmgmt.api.promo

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.*
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.api.ControllerTest
import ru.yandex.market.pricingmgmt.client.promo.api.PromoApiClient
import ru.yandex.market.pricingmgmt.config.security.passport.PassportAuthenticationFilter
import ru.yandex.market.pricingmgmt.exception.NotFoundException
import ru.yandex.mj.generated.client.promoservice.model.*
import ru.yandex.mj.generated.server.model.*
import java.util.*
import kotlin.collections.ArrayList

@DbUnitDataBaseConfig(
    DbUnitDataBaseConfig.Entry(
        name = "datatypeFactory", value = "ru.yandex.market.pricingmgmt.pg.ExtendedPostgresqlDataTypeFactory"
    )
)
@DbUnitDataSet(before = ["PromoApiTest.csv"])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WithMockUser(username = PassportAuthenticationFilter.LOCAL_DEV, roles = ["PRICING_MGMT_ACCESS", "PROMO_USER"])
class PromoHistoryApiServiceTest : ControllerTest() {
    @MockBean
    private lateinit var promoApiClient: PromoApiClient

    companion object {
        const val PROMO_ID = "cf_100001"
    }

    @Test
    fun getHistory_invalidPage() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/promos/${PROMO_ID}/history")
                .param("page", "0")
                .param("limit", "10")
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    fun getHistory_invalidLimit() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/promos/${PROMO_ID}/history")
                .param("page", "10")
                .param("limit", "0")
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    fun getEmptyHistory() {
        val expectedResponseDto = ru.yandex.mj.generated.server.model.PromoHistoryResponse().promoId(PROMO_ID)
            .changes(Collections.emptyList()).changesCount(0)

        doReturn(
            PromoHistory()
                .changes(Collections.emptyList())
                .totalCount(0)
        ).`when`(promoApiClient).getHistory(PROMO_ID, null, null)

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/promos/${PROMO_ID}/history")
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(expectedResponseDto))
            )

    }

    @Test
    fun getHistory_promoNotFound() {
        doThrow(
            NotFoundException("Promo not found")
        ).`when`(promoApiClient).getHistory(PROMO_ID, null, null)

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/promos/${PROMO_ID}/history")
        ).andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    fun getHistory_success() {
        val expectedResponseDto = ru.yandex.mj.generated.server.model.PromoHistoryResponse()
            .promoId(PROMO_ID)
            .changes(
                listOf(
                    PromoChangeDto()
                        .updatedAt(123456789L)
                        .updatedBy("appuser")
                        .dbUser("dbuser")
                        .requestId("reqid")
                        .source("CATEGORYIFACE")
                        .transactionId("123")
                        .fields(
                            listOf(
                                PromoFieldChangeDto()
                                    .field(PromoFieldChangeDto.FieldEnum.ACTIVE)
                                    .operationType(PromoFieldChangeOperationType.UPDATE)
                                    .addValuesItem("true"),
                                PromoFieldChangeDto()
                                    .field(PromoFieldChangeDto.FieldEnum.PROMOCODEBUDGET)
                                    .operationType(PromoFieldChangeOperationType.DELETE)
                                    .addValuesItem("123456789"),
                                PromoFieldChangeDto()
                                    .field(PromoFieldChangeDto.FieldEnum.MEDIAPLANFILENAME)
                                    .operationType(PromoFieldChangeOperationType.INSERT)
                                    .addValuesItem("filename.txt")
                            )
                        )
                        .addPromotionItem(
                            PromoPromotionChangeDto()
                                .operationType(PromoFieldChangeOperationType.INSERT)
                                .addValuesItem(
                                    PromoChannelDto()
                                        .id("1")
                                        .name("channel")
                                        .catteamName("catteam1")
                                        .categoryName("category1")
                                        .period(1)
                                        .unit("нед.")
                                        .plan(100)
                                        .fact(150)
                                        .canEdit(false)
                                        .comment("comment")
                                )
                        )
                        .restrictions(
                            listOf(
                                PromoRestrictionChangeDto()
                                    .name(PromoRestrictionChangeDto.NameEnum.PARTNER)
                                    .operationType(PromoFieldChangeOperationType.INSERT)
                                    .addValuesItem(PromoRestrictionChangeValueDto().id("1")),
                                PromoRestrictionChangeDto()
                                    .name(PromoRestrictionChangeDto.NameEnum.WAREHOUSE)
                                    .operationType(PromoFieldChangeOperationType.DELETE)
                                    .addValuesItem(PromoRestrictionChangeValueDto().id("1")),
                                PromoRestrictionChangeDto()
                                    .name(PromoRestrictionChangeDto.NameEnum.CATEGORY)
                                    .operationType(PromoFieldChangeOperationType.INSERT)
                                    .addValuesItem(PromoRestrictionChangeValueDto().id("1").percent(55)),
                                PromoRestrictionChangeDto()
                                    .name(PromoRestrictionChangeDto.NameEnum.VENDOR)
                                    .operationType(PromoFieldChangeOperationType.DELETE)
                                    .addValuesItem(PromoRestrictionChangeValueDto().id("1")),
                                PromoRestrictionChangeDto()
                                    .name(PromoRestrictionChangeDto.NameEnum.MSKU)
                                    .operationType(PromoFieldChangeOperationType.INSERT)
                                    .addValuesItem(PromoRestrictionChangeValueDto().id("1"))
                            )
                        )
                )
            )
            .changesCount(1)

        doReturn(
            PromoHistory()
                .changes(
                    listOf(
                        PromoChange()
                            .updatedAt(123456789000L)
                            .updatedBy("appuser")
                            .dbUser("dbuser")
                            .requestId("reqid")
                            .source(SourceType.CATEGORYIFACE)
                            .transactionId("123")
                            .fields(
                                listOf(
                                    PromoMainFieldChange()
                                        .field(PromoMainFieldChange.FieldEnum.ACTIVE)
                                        .operationType(ChangeOperationType.UPDATE)
                                        .addValuesItem("true"),
                                    PromoMainFieldChange()
                                        .field(PromoMainFieldChange.FieldEnum.PROMOCODEBUDGET)
                                        .operationType(ChangeOperationType.DELETE)
                                        .addValuesItem("123456789")
                                )
                            ).srcFields(
                                PromoChangeSrcFields().ciface(
                                    PromoChangeSrcFieldsCiface().addFieldsItem(
                                        PromoSrcCifaceFieldChange()
                                            .field(PromoSrcCifaceFieldChange.FieldEnum.MEDIAPLANS3FILENAME)
                                            .operationType(ChangeOperationType.INSERT)
                                            .addValuesItem("filename.txt")
                                    ).addPromotionItem(
                                        PromoSrcCifacePromotionChange()
                                            .operationType(ChangeOperationType.INSERT)
                                            .addValuesItem(
                                                Promotion()
                                                    .id("1")
                                                    .channel("channel")
                                                    .catteam("catteam1")
                                                    .category("category1")
                                                    .count(1)
                                                    .countUnit("нед.")
                                                    .budgetPlan(100)
                                                    .budgetFact(150)
                                                    .isCustomBudgetPlan(false)
                                                    .comment("comment")
                                            )
                                    )
                                )
                            ).constraints(
                                listOf(
                                    PromoConstraintChange()
                                        .name(PromoConstraintChange.NameEnum.SUPPLIERS)
                                        .operationType(ChangeOperationType.INSERT)
                                        .addValuesItem(mapOf("supplier_id" to "1")),
                                    PromoConstraintChange()
                                        .name(PromoConstraintChange.NameEnum.WAREHOUSES)
                                        .operationType(ChangeOperationType.DELETE)
                                        .addValuesItem(mapOf("warehouse_id" to "1")),
                                    PromoConstraintChange()
                                        .name(PromoConstraintChange.NameEnum.CATEGORIES)
                                        .operationType(ChangeOperationType.INSERT)
                                        .addValuesItem(mapOf("category_id" to "1", "percent" to "55")),
                                    PromoConstraintChange()
                                        .name(PromoConstraintChange.NameEnum.VENDORS)
                                        .operationType(ChangeOperationType.DELETE)
                                        .addValuesItem(mapOf("vendor_id" to "1")),
                                    PromoConstraintChange()
                                        .name(PromoConstraintChange.NameEnum.MSKUS)
                                        .operationType(ChangeOperationType.INSERT)
                                        .addValuesItem(mapOf("msku_id" to "1"))
                                )
                            )
                    )
                )
                .totalCount(1)
        ).`when`(promoApiClient).getHistory(PROMO_ID, null, null)

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/promos/${PROMO_ID}/history")
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(expectedResponseDto))
            )
    }

    @Test
    fun getHistory_pagingSuccess() {
        val expectedResponseDto = ru.yandex.mj.generated.server.model.PromoHistoryResponse().promoId(PROMO_ID)
            .changes(Collections.emptyList()).changesCount(0)

        doReturn(
            PromoHistory()
                .changes(Collections.emptyList())
                .totalCount(0)
        ).`when`(promoApiClient).getHistory(PROMO_ID, 10, 5)

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/promos/${PROMO_ID}/history")
                .param("page", "5")
                .param("limit", "10")
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(expectedResponseDto))
            )

        verify(promoApiClient).getHistory(PROMO_ID, 10, 5)
    }


    @Test
    fun getHistory_allBaseFieldsProcessed_success() {
        val fields = ArrayList<PromoMainFieldChange>()
        for (fieldName in PromoMainFieldChange.FieldEnum.values()) {
            fields.add(
                PromoMainFieldChange()
                    .field(fieldName)
                    .operationType(ChangeOperationType.INSERT)
                    .addValuesItem("val")
            )
        }

        doReturn(
            PromoHistory()
                .changes(
                    listOf(
                        PromoChange()
                            .updatedAt(123456789L)
                            .updatedBy("appuser")
                            .dbUser("dbuser")
                            .requestId("reqid")
                            .source(SourceType.CATEGORYIFACE)
                            .transactionId("123")
                            .fields(fields)
                    )
                )
                .totalCount(1)
        ).`when`(promoApiClient).getHistory(PROMO_ID, null, null)

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/promos/${PROMO_ID}/history")
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].fields.length()").value(fields.count()))
    }

    @Test
    fun getHistory_allCifaceFieldsProcessed_success() {
        val fields = ArrayList<PromoSrcCifaceFieldChange>()
        for (fieldName in PromoSrcCifaceFieldChange.FieldEnum.values()) {
            fields.add(
                PromoSrcCifaceFieldChange()
                    .field(fieldName)
                    .operationType(ChangeOperationType.INSERT)
                    .addValuesItem("val")
            )
        }

        doReturn(
            PromoHistory()
                .changes(
                    listOf(
                        PromoChange()
                            .updatedAt(123456789L)
                            .updatedBy("appuser")
                            .dbUser("dbuser")
                            .requestId("reqid")
                            .source(SourceType.CATEGORYIFACE)
                            .transactionId("123")
                            .srcFields(PromoChangeSrcFields().ciface(PromoChangeSrcFieldsCiface().fields(fields)))
                    )
                )
                .totalCount(1)
        ).`when`(promoApiClient).getHistory(PROMO_ID, null, null)

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/promos/${PROMO_ID}/history")
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].fields.length()").value(fields.count()))
    }

    @Test
    fun getHistory_allOperationTypesProcessed_success() {
        val fields = ArrayList<PromoSrcCifaceFieldChange>()
        for (operationType in ChangeOperationType.values()) {
            fields.add(
                PromoSrcCifaceFieldChange()
                    .field(PromoSrcCifaceFieldChange.FieldEnum.AUTHOR)
                    .operationType(operationType)
                    .addValuesItem("John")
            )
        }

        doReturn(
            PromoHistory()
                .changes(
                    listOf(
                        PromoChange()
                            .updatedAt(123456789L)
                            .updatedBy("appuser")
                            .dbUser("dbuser")
                            .requestId("reqid")
                            .source(SourceType.CATEGORYIFACE)
                            .transactionId("123")
                            .srcFields(PromoChangeSrcFields().ciface(PromoChangeSrcFieldsCiface().fields(fields)))
                    )
                )
                .totalCount(1)
        ).`when`(promoApiClient).getHistory(PROMO_ID, null, null)

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/promos/${PROMO_ID}/history")
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.jsonPath("$.changes[0].fields.length()").value(fields.count()))
    }


    @Test
    fun getHistory_allConstraintsTypesProcessed_success() {
        val constraints = ArrayList<PromoConstraintChange>()
        for (constraintName in PromoConstraintChange.NameEnum.values()) {
            constraints.add(
                PromoConstraintChange()
                    .name(constraintName)
                    .operationType(ChangeOperationType.INSERT)
                    .addValuesItem(mapOf("param" to "value"))
            )
        }

        doReturn(
            PromoHistory()
                .changes(
                    listOf(
                        PromoChange()
                            .updatedAt(123456789L)
                            .updatedBy("appuser")
                            .dbUser("dbuser")
                            .requestId("reqid")
                            .source(SourceType.CATEGORYIFACE)
                            .transactionId("123")
                            .constraints(constraints)
                    )
                )
                .totalCount(1)
        ).`when`(promoApiClient).getHistory(PROMO_ID, null, null)

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/promos/${PROMO_ID}/history")
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.changes[0].restrictions.length()")
                    .value(constraints.count())
            )
    }

    @DbUnitDataSet(before = ["PromoHistoryApiServiceTest.getHistory_withConstraintsNames_success.before.csv"])
    @Test
    fun getHistory_withConstraintsNames_success() {
        val expectedResponseDto = ru.yandex.mj.generated.server.model.PromoHistoryResponse()
            .promoId(PROMO_ID)
            .changes(
                listOf(
                    PromoChangeDto()
                        .updatedAt(123456789L)
                        .updatedBy("appuser")
                        .dbUser("dbuser")
                        .requestId("reqid")
                        .source("CATEGORYIFACE")
                        .transactionId("123")
                        .fields(Collections.emptyList())
                        .promotion(Collections.emptyList())
                        .restrictions(
                            listOf(
                                PromoRestrictionChangeDto()
                                    .name(PromoRestrictionChangeDto.NameEnum.PARTNER)
                                    .operationType(PromoFieldChangeOperationType.INSERT)
                                    .addValuesItem(
                                        PromoRestrictionChangeValueDto()
                                            .id("21").name("supplier21")
                                    )
                                    .addValuesItem(
                                        PromoRestrictionChangeValueDto()
                                            .id("22").name("supplier22")
                                    ),
                                PromoRestrictionChangeDto()
                                    .name(PromoRestrictionChangeDto.NameEnum.WAREHOUSE)
                                    .operationType(PromoFieldChangeOperationType.DELETE)
                                    .addValuesItem(
                                        PromoRestrictionChangeValueDto()
                                            .id("21").name("warehouse21")
                                    )
                                    .addValuesItem(
                                        PromoRestrictionChangeValueDto()
                                            .id("22").name("warehouse22")
                                    ),
                                PromoRestrictionChangeDto()
                                    .name(PromoRestrictionChangeDto.NameEnum.CATEGORY)
                                    .operationType(PromoFieldChangeOperationType.INSERT)
                                    .addValuesItem(
                                        PromoRestrictionChangeValueDto()
                                            .id("31").percent(55).name("category31")
                                    )
                                    .addValuesItem(
                                        PromoRestrictionChangeValueDto()
                                            .id("32").name("category32")
                                    ),
                                PromoRestrictionChangeDto()
                                    .name(PromoRestrictionChangeDto.NameEnum.VENDOR)
                                    .operationType(PromoFieldChangeOperationType.DELETE)
                                    .addValuesItem(PromoRestrictionChangeValueDto().id("21").name("vendor21"))
                                    .addValuesItem(PromoRestrictionChangeValueDto().id("22").name("vendor22")),
                                PromoRestrictionChangeDto()
                                    .name(PromoRestrictionChangeDto.NameEnum.MSKU)
                                    .operationType(PromoFieldChangeOperationType.INSERT)
                                    .addValuesItem(PromoRestrictionChangeValueDto().id("11").name("msku11"))
                                    .addValuesItem(PromoRestrictionChangeValueDto().id("12").name("msku12"))
                            )
                        )
                )
            )
            .changesCount(1)

        doReturn(
            PromoHistory()
                .changes(
                    listOf(
                        PromoChange()
                            .updatedAt(123456789000L)
                            .updatedBy("appuser")
                            .dbUser("dbuser")
                            .requestId("reqid")
                            .source(SourceType.CATEGORYIFACE)
                            .transactionId("123")
                            .constraints(
                                listOf(
                                    PromoConstraintChange()
                                        .name(PromoConstraintChange.NameEnum.SUPPLIERS)
                                        .operationType(ChangeOperationType.INSERT)
                                        .values(listOf(mapOf("supplier_id" to "21"), mapOf("supplier_id" to "22"))),
                                    PromoConstraintChange()
                                        .name(PromoConstraintChange.NameEnum.WAREHOUSES)
                                        .operationType(ChangeOperationType.DELETE)
                                        .values(listOf(mapOf("warehouse_id" to "21"), mapOf("warehouse_id" to "22"))),
                                    PromoConstraintChange()
                                        .name(PromoConstraintChange.NameEnum.CATEGORIES)
                                        .operationType(ChangeOperationType.INSERT)
                                        .values(
                                            listOf(
                                                mapOf("category_id" to "31", "percent" to "55"),
                                                mapOf("category_id" to "32")
                                            )
                                        ),
                                    PromoConstraintChange()
                                        .name(PromoConstraintChange.NameEnum.VENDORS)
                                        .operationType(ChangeOperationType.DELETE)
                                        .values(listOf(mapOf("vendor_id" to "21"), mapOf("vendor_id" to "22"))),
                                    PromoConstraintChange()
                                        .name(PromoConstraintChange.NameEnum.MSKUS)
                                        .operationType(ChangeOperationType.INSERT)
                                        .values(listOf(mapOf("msku_id" to "11"), mapOf("msku_id" to "12")))
                                )
                            )
                    )
                )
                .totalCount(1)
        ).`when`(promoApiClient).getHistory(PROMO_ID, null, null)

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/promos/${PROMO_ID}/history")
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(expectedResponseDto))
            )
    }

    @Test
    fun getHistory_severalChanges_success() {
        val expectedResponseDto = ru.yandex.mj.generated.server.model.PromoHistoryResponse()
            .promoId(PROMO_ID)
            .changes(
                listOf(
                    PromoChangeDto()
                        .updatedAt(123456789L)
                        .updatedBy("appuser")
                        .dbUser("dbuser")
                        .requestId("reqid")
                        .source("CATEGORYIFACE")
                        .transactionId("123")
                        .fields(
                            listOf(
                                PromoFieldChangeDto().field(PromoFieldChangeDto.FieldEnum.AUTHOR)
                                    .operationType(PromoFieldChangeOperationType.UPDATE).addValuesItem("Ben")
                            )
                        )
                        .promotion(Collections.emptyList())
                        .restrictions(Collections.emptyList()),
                    PromoChangeDto()
                        .updatedAt(234567891L)
                        .updatedBy(null)
                        .dbUser("dbuser")
                        .requestId(null)
                        .source(null)
                        .transactionId("123")
                        .fields(
                            listOf(
                                PromoFieldChangeDto().field(PromoFieldChangeDto.FieldEnum.STARTDATE)
                                    .operationType(PromoFieldChangeOperationType.UPDATE).addValuesItem("123")
                            )
                        )
                        .promotion(Collections.emptyList())
                        .restrictions(Collections.emptyList())
                )
            )
            .changesCount(2)

        doReturn(
            PromoHistory()
                .changes(
                    listOf(
                        PromoChange()
                            .updatedAt(123456789000L)
                            .updatedBy("appuser")
                            .dbUser("dbuser")
                            .requestId("reqid")
                            .source(SourceType.CATEGORYIFACE)
                            .transactionId("123")
                            .srcFields(
                                PromoChangeSrcFields().ciface(
                                    PromoChangeSrcFieldsCiface().addFieldsItem(
                                        PromoSrcCifaceFieldChange().field(PromoSrcCifaceFieldChange.FieldEnum.AUTHOR)
                                            .operationType(ChangeOperationType.UPDATE).addValuesItem("Ben")
                                    )
                                )
                            ),
                        PromoChange()
                            .updatedAt(234567891000L)
                            .updatedBy(null)
                            .dbUser("dbuser")
                            .requestId(null)
                            .source(null)
                            .transactionId("123")
                            .fields(
                                listOf(
                                    PromoMainFieldChange().field(PromoMainFieldChange.FieldEnum.STARTAT)
                                        .operationType(ChangeOperationType.UPDATE).addValuesItem("123")
                                )
                            )
                    )
                )
                .totalCount(2)
        ).`when`(promoApiClient).getHistory(PROMO_ID, null, null)

        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/promos/${PROMO_ID}/history")
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(expectedResponseDto))
            )
    }
}
