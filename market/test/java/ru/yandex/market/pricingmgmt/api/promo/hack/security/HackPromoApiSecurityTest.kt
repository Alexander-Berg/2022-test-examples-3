package ru.yandex.market.pricingmgmt.api.promo.hack.security

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.api.ControllerTest
import ru.yandex.market.pricingmgmt.service.TimeService
import ru.yandex.market.pricingmgmt.util.DateTimeTestingUtil
import java.time.LocalDateTime

@DbUnitDataBaseConfig(
    DbUnitDataBaseConfig.Entry(
        name = "datatypeFactory",
        value = "ru.yandex.market.pricingmgmt.pg.ExtendedPostgresqlDataTypeFactory"
    )
)
class HackPromoApiSecurityTest(
    @Autowired private val timeService: TimeService,
) : ControllerTest() {

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiSecurityTest.getHackPromosWithGrantsForCatman.before.csv"]
    )
    fun getHackPromosWithGrantsForCatman() {

        val expectedResult = mapOf(
            "$.[0].entity" to "promo",
            "$.[0].grants.length()" to "1",
            "$.[0].grants" to "DOWNLOAD",

            "$.[1].entity" to "promo_dep",
            "$.[1].grants.length()" to "2",
            "$.[1].grants.[0]" to "DOWNLOAD",
            "$.[1].grants.[1]" to "UPLOAD",

            "$.[2].entity" to "promo_list",
            "$.[2].grants.length()" to "3",
            "$.[2].grants.[0]" to "DOWNLOAD",
            "$.[2].grants.[1]" to "UPLOAD_WITH_REPLACEMENT",
            "$.[2].grants.[2]" to "REMOVE",

            "$.[3].entity" to "promo_dep",
            "$.[3].grants.length()" to "1",
            "$.[3].grants" to "DOWNLOAD",

            "$.[4].entity" to "promo_list",
            "$.[4].grants.length()" to "1",
            "$.[4].grants" to "DOWNLOAD"
        )

        var resultActions = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/hack/promos")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)

        expectedResult.forEach { entry ->
            resultActions = resultActions.andExpect(MockMvcResultMatchers.jsonPath(entry.key).value(entry.value))
        }
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiSecurityTest.getHackPromosWithGrantsForCatmanAfterDeadline.before.csv"]
    )
    fun getHackPromosWithGrantsForCatmanAfterDeadline() {

        val expectedResult = mapOf(
            "$.[0].entity" to "promo",
            "$.[0].grants.length()" to "1",
            "$.[0].grants" to "DOWNLOAD",

            "$.[1].entity" to "promo_dep",
            "$.[1].grants.length()" to "1",
            "$.[1].grants.[0]" to "DOWNLOAD",

            "$.[2].entity" to "promo_list",
            "$.[2].grants.length()" to "1",
            "$.[2].grants.[0]" to "DOWNLOAD",

            "$.[3].entity" to "promo_dep",
            "$.[3].grants.length()" to "1",
            "$.[3].grants" to "DOWNLOAD",

            "$.[4].entity" to "promo_list",
            "$.[4].grants.length()" to "1",
            "$.[4].grants" to "DOWNLOAD"
        )

        var resultActions = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/hack/promos")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)

        expectedResult.forEach { entry ->
            resultActions = resultActions.andExpect(MockMvcResultMatchers.jsonPath(entry.key).value(entry.value))
        }
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiSecurityTest.getHackPromosWithGrantsForCatdir.before.csv"]
    )
    fun getHackPromosWithGrantsForCatdir() {
        val expectedResult = mapOf(
            "$.[0].entity" to "promo",
            "$.[0].grants.length()" to "3",
            "$.[0].grants.[0]" to "DOWNLOAD",
            "$.[0].grants.[1]" to "APPROVE",
            "$.[0].grants.[2]" to "CREATE_PRICE_JOURNALS",

            "$.[1].entity" to "promo_dep",
            "$.[1].grants.length()" to "2",
            "$.[1].grants.[0]" to "DOWNLOAD",
            "$.[1].grants.[1]" to "UPLOAD",

            "$.[2].entity" to "promo_list",
            "$.[2].grants.length()" to "4",
            "$.[2].grants.[0]" to "DOWNLOAD",
            "$.[2].grants.[1]" to "UPLOAD_WITH_REPLACEMENT",
            "$.[2].grants.[2]" to "UPLOAD_TO_ALL_DEPARTMENTS",
            "$.[2].grants.[3]" to "REMOVE",

            "$.[3].entity" to "promo_dep",
            "$.[3].grants.length()" to "2",
            "$.[3].grants.[0]" to "DOWNLOAD",
            "$.[3].grants.[1]" to "UPLOAD",

            "$.[4].entity" to "promo_list",
            "$.[4].grants.length()" to "4",
            "$.[4].grants.[0]" to "DOWNLOAD",
            "$.[4].grants.[1]" to "UPLOAD_WITH_REPLACEMENT",
            "$.[4].grants.[2]" to "UPLOAD_TO_ALL_DEPARTMENTS",
            "$.[4].grants.[3]" to "REMOVE"
        )

        var resultActions = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/hack/promos")
                .contentType("application/json")
        ).andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)

        expectedResult.forEach { entry ->
            resultActions = resultActions.andExpect(MockMvcResultMatchers.jsonPath(entry.key).value(entry.value))
        }
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiSecurityTest.getHackPromosWithGrantsForCatdirAfterDeadline.before.csv"]
    )
    fun getHackPromosWithGrantsForCatdirAfterDeadline() {

        val expectedResult = mapOf(
            "$.[0].entity" to "promo",
            "$.[0].grants.length()" to "1",
            "$.[0].grants" to "DOWNLOAD",

            "$.[1].entity" to "promo_dep",
            "$.[1].grants.length()" to "2",
            "$.[1].grants.[0]" to "DOWNLOAD",
            "$.[1].grants.[1]" to "UPLOAD",

            "$.[2].entity" to "promo_list",
            "$.[2].grants.length()" to "4",
            "$.[2].grants.[0]" to "DOWNLOAD",
            "$.[2].grants.[1]" to "UPLOAD_WITH_REPLACEMENT",
            "$.[2].grants.[2]" to "UPLOAD_TO_ALL_DEPARTMENTS",
            "$.[2].grants.[3]" to "REMOVE",

            "$.[3].entity" to "promo_dep",
            "$.[3].grants.length()" to "2",
            "$.[3].grants.[0]" to "DOWNLOAD",
            "$.[3].grants.[1]" to "UPLOAD",

            "$.[4].entity" to "promo_list",
            "$.[4].grants.length()" to "4",
            "$.[4].grants.[0]" to "DOWNLOAD",
            "$.[4].grants.[1]" to "UPLOAD_WITH_REPLACEMENT",
            "$.[4].grants.[2]" to "UPLOAD_TO_ALL_DEPARTMENTS",
            "$.[4].grants.[3]" to "REMOVE"
        )

        var resultActions = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/hack/promos")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)

        expectedResult.forEach { entry ->
            resultActions = resultActions.andExpect(MockMvcResultMatchers.jsonPath(entry.key).value(entry.value))
        }
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiSecurityTest.uploadListItemsAsCatmentFromAnotherDept.before.csv"],
        after = ["HackPromoApiSecurityTest.uploadListItemsAsCatmentFromAnotherDept.after.csv"]
    )
    fun uploadListItemsAsCatmentFromAnotherDept() {
        uploadList(
            1L,
            "Детские вещи",
            null,
            "promo_dep",
            "/xlsx-template/hack-list.xlsx"
        ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiSecurityTest.reUploadListItemsAsCatman.before.csv"],
        after = ["HackPromoApiSecurityTest.reUploadListItemsAsCatman.after.csv"]
    )
    fun reUploadListItemsAsCatman() {
        Mockito.`when`(timeService.getNowOffsetDateTime()).thenReturn(
            DateTimeTestingUtil.createOffsetDateTime(2021, 12, 23, 8, 0, 0)
        )

        uploadList(
            1L,
            null,
            "обновление цен",
            "promo_list",
            "/xlsx-template/hack-list.xlsx"
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiSecurityTest.reUploadListItemsAsCatmanWithAnotherUser.before.csv"],
        after = ["HackPromoApiSecurityTest.reUploadListItemsAsCatmanWithAnotherUser.after.csv"]
    )
    fun reUploadListItemsAsCatmanWithAnotherUser() {
        uploadList(
            1L,
            null,
            "обновление цен",
            "promo_list",
            "/xlsx-template/hack-list-with-different-catman.xlsx"
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiSecurityTest.deleteListItemsAsCatmenFromAnotherDept.before.csv"],
        after = ["HackPromoApiSecurityTest.deleteListItemsAsCatmenFromAnotherDept.after.csv"]
    )
    fun deleteListItemsAsCatmenFromAnotherDept() {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/hack/promos/items/?id=1"))
            .andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiSecurityTest.deleteListItemsAsCatdir.before.csv"],
        after = ["HackPromoApiSecurityTest.deleteListItemsAsCatdir.after.csv"]
    )
    fun deleteListItemsAsCatdir() {
        `when`(timeService.getNowDateTime()).thenReturn(
            LocalDateTime.of(2022, 12, 23, 8, 0, 0)
        )

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/hack/promos/items/?id=1"))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoApiSecurityTest.getHackPromosWithGrantsForPromoCreator.before.csv"]
    )
    fun getHackPromosWithGrantsForPromoCreator() {
        val expectedResult = mapOf(
            "$.[0].entity" to "promo",
            "$.[0].grants.length()" to "1",
            "$.[0].grants" to "REMOVE",

            "$.[1].entity" to "promo_dep",
            "$.[1].grants.length()" to "1",
            "$.[1].grants.[0]" to "REMOVE",

            "$.[2].entity" to "promo_list",
            "$.[2].grants.length()" to "2",
            "$.[2].grants.[0]" to "UPLOAD_WITH_REPLACEMENT",
            "$.[2].grants.[1]" to "REMOVE",

            "$.[3].entity" to "promo_dep",
            "$.[3].grants.length()" to "1",
            "$.[3].grants.[0]" to "REMOVE",

            "$.[4].entity" to "promo_list",
            "$.[4].grants.length()" to "0"
        )

        var resultActions = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/hack/promos")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)

        expectedResult.forEach { entry ->
            resultActions = resultActions.andExpect(MockMvcResultMatchers.jsonPath(entry.key).value(entry.value))
        }
    }

    private fun uploadList(id: Long, name: String?, comment: String?, entity: String, filename: String): ResultActions {
        val bytes = javaClass.getResourceAsStream(filename)?.readAllBytes()
        val file = MockMultipartFile("excelFile", filename, "application/vnd.ms-excel", bytes)
        return mockMvc.perform(
            MockMvcRequestBuilders
                .multipart("/api/v1/hack/promos/items/")
                .file(file)
                .param("id", id.toString())
                .param("entity", entity)
                .param("name", name)
                .param("comment", comment)
        )
    }
}
