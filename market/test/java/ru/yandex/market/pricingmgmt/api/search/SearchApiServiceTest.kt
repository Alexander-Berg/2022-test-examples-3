package ru.yandex.market.pricingmgmt.api.search

import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.api.ControllerTest
import ru.yandex.mj.generated.server.model.SearchResult

@DbUnitDataBaseConfig(
    DbUnitDataBaseConfig.Entry(
        name = "datatypeFactory",
        value = "ru.yandex.market.pricingmgmt.pg.ExtendedPostgresqlDataTypeFactory"
    )
)
open class SearchApiServiceTest : ControllerTest() {

    companion object {
        const val MSKU_SEARCH_ENDPOINT = "/api/v1/msku/search"
        const val PARTNER_SEARCH_ENDPOINT = "/api/v1/partners/search"
    }

    @Test
    @DbUnitDataSet(
        before = ["SearchApiServiceTest.searchAssortmentTest.csv"]
    )
    fun searchAssortmentByMskuSuccessTest() {
        val expectedResponseDto = listOf(
            SearchResult()
                .id(41)
                .name("msku_41")
        )

        mockMvc.perform(
            MockMvcRequestBuilders.get(MSKU_SEARCH_ENDPOINT)
                .param("searchString", "41")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(expectedResponseDto))
            )
    }

    @Test
    @DbUnitDataSet(
        before = ["SearchApiServiceTest.searchAssortmentTest.csv"]
    )
    fun searchAssortmentByNameSuccessTest() {
        val expectedResponseDto = listOf(
            SearchResult()
                .id(41)
                .name("msku_41"),
            SearchResult()
                .id(42)
                .name("mskU_42")
        )

        mockMvc.perform(
            MockMvcRequestBuilders.get(MSKU_SEARCH_ENDPOINT)
                .param("searchString", "ku_4")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(expectedResponseDto))
            )
    }

    @Test
    @DbUnitDataSet(
        before = ["SearchApiServiceTest.searchAssortmentTest.csv"]
    )
    fun searchAssortmentByNameCyrillicSuccessTest() {
        val expectedResponseDto = listOf(
            SearchResult()
                .id(44)
                .name("Мскю_44"),
            SearchResult()
                .id(45)
                .name("мСкю_45")
        )

        mockMvc.perform(
            MockMvcRequestBuilders.get(MSKU_SEARCH_ENDPOINT)
                .param("searchString", "мсКю_")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(expectedResponseDto))
            )
    }

    @Test
    @DbUnitDataSet(
        before = ["SearchApiServiceTest.searchAssortmentTest.csv"]
    )
    fun searchAssortmentByNameWithLimitSuccessTest() {
        val expectedResponseDto = listOf(
            SearchResult()
                .id(41)
                .name("msku_41")
        )

        mockMvc.perform(
            MockMvcRequestBuilders.get(MSKU_SEARCH_ENDPOINT)
                .param("searchString", "ku_4")
                .param("limit", "1")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(expectedResponseDto))
            )
    }

    @Test
    @DbUnitDataSet(
        before = ["SearchApiServiceTest.searchAssortmentByNameWithoutLimitALotOfRecordsSuccessTest.csv"]
    )
    fun searchAssortmentByNameWithoutLimitALotOfRecordsSuccessTest() {
        val expectedResponseDto = mutableListOf<SearchResult>()

        for (i in 1L..20L) {
            expectedResponseDto.add(SearchResult().id(i).name("msku_$i"))
        }

        mockMvc.perform(
            MockMvcRequestBuilders.get(MSKU_SEARCH_ENDPOINT)
                .param("searchString", "msku")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(expectedResponseDto))
            )
    }

    @Test
    @DbUnitDataSet(
        before = ["SearchApiServiceTest.searchAssortmentTest.csv"]
    )
    fun searchAssortmentByMskuEmptyResultTest() {
        mockMvc.perform(
            MockMvcRequestBuilders.get(MSKU_SEARCH_ENDPOINT)
                .param("searchString", "100")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(
                        objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(emptyList<SearchResult>())
                    )
            )
    }

    @Test
    @DbUnitDataSet(
        before = ["SearchApiServiceTest.searchAssortmentTest.csv"]
    )
    fun searchAssortmentByNameEmptyResultTest() {
        mockMvc.perform(
            MockMvcRequestBuilders.get(MSKU_SEARCH_ENDPOINT)
                .param("searchString", "kak_by_msku")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(
                        objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(emptyList<SearchResult>())
                    )
            )
    }

    @Test
    fun searchAssortmentWithoutParamTest() {
        mockMvc.perform(
            MockMvcRequestBuilders.get(MSKU_SEARCH_ENDPOINT)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    fun searchAssortmentWithEmptySearchStringParamTest() {
        mockMvc.perform(
            MockMvcRequestBuilders.get(MSKU_SEARCH_ENDPOINT)
                .param("searchString", "")
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.content()
                    .string("Передано пустое значение для поиска")
            )
    }

    @Test
    @DbUnitDataSet(
        before = ["SearchApiServiceTest.searchPartnersTest.csv"]
    )
    fun searchPartnersByIdSuccessTest() {
        val expectedResponseDto = listOf(
            SearchResult()
                .id(41)
                .name("shop_41")
        )

        mockMvc.perform(
            MockMvcRequestBuilders.get(PARTNER_SEARCH_ENDPOINT)
                .param("searchString", "41")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(expectedResponseDto))
            )
    }

    @Test
    @DbUnitDataSet(
        before = ["SearchApiServiceTest.searchPartnersTest.csv"]
    )
    fun searchPartnersByNameSuccessTest() {
        val expectedResponseDto = listOf(
            SearchResult()
                .id(41)
                .name("shop_41"),
            SearchResult()
                .id(42)
                .name("shoP_42")
        )

        mockMvc.perform(
            MockMvcRequestBuilders.get(PARTNER_SEARCH_ENDPOINT)
                .param("searchString", "op_4")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(expectedResponseDto))
            )
    }

    @Test
    @DbUnitDataSet(
        before = ["SearchApiServiceTest.searchPartnersTest.csv"]
    )
    fun searchPartnersByNameCyrillicSuccessTest() {
        val expectedResponseDto = listOf(
            SearchResult()
                .id(43)
                .name("Партнер_43"),
            SearchResult()
                .id(44)
                .name("пАртнер_44")
        )

        mockMvc.perform(
            MockMvcRequestBuilders.get(PARTNER_SEARCH_ENDPOINT)
                .param("searchString", "паРтнер")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(expectedResponseDto))
            )
    }

    @Test
    @DbUnitDataSet(
        before = ["SearchApiServiceTest.searchPartnersTest.csv"]
    )
    fun searchPartnersByNameWithLimitSuccessTest() {
        val expectedResponseDto = listOf(
            SearchResult()
                .id(41)
                .name("shop_41")
        )

        mockMvc.perform(
            MockMvcRequestBuilders.get(PARTNER_SEARCH_ENDPOINT)
                .param("searchString", "op_4")
                .param("limit", "1")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(expectedResponseDto))
            )
    }

    @Test
    @DbUnitDataSet(
        before = ["SearchApiServiceTest.searchPartnersByNameWithoutLimitALotOfRecordsSuccessTest.csv"]
    )
    fun searchPartnersByNameWithoutLimitALotOfRecordsSuccessTest() {
        val expectedResponseDto = mutableListOf<SearchResult>()

        for (i in 1L..20L) {
            expectedResponseDto.add(SearchResult().id(i).name("shop_$i"))
        }

        mockMvc.perform(
            MockMvcRequestBuilders.get(PARTNER_SEARCH_ENDPOINT)
                .param("searchString", "shop")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(expectedResponseDto))
            )
    }

    @Test
    @DbUnitDataSet(
        before = ["SearchApiServiceTest.searchPartnersTest.csv"]
    )
    fun searchPartnersByIdEmptyResultTest() {
        mockMvc.perform(
            MockMvcRequestBuilders.get(PARTNER_SEARCH_ENDPOINT)
                .param("searchString", "100")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(
                        objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(emptyList<SearchResult>())
                    )
            )
    }

    @Test
    @DbUnitDataSet(
        before = ["SearchApiServiceTest.searchPartnersTest.csv"]
    )
    fun searchPartnersByNameEmptyResultTest() {
        mockMvc.perform(
            MockMvcRequestBuilders.get(PARTNER_SEARCH_ENDPOINT)
                .param("searchString", "kak_by_shop")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(
                        objectMapper!!.writer().withDefaultPrettyPrinter().writeValueAsString(emptyList<SearchResult>())
                    )
            )
    }

    @Test
    fun searchPartnersWithoutParamTest() {
        mockMvc.perform(
            MockMvcRequestBuilders.get(PARTNER_SEARCH_ENDPOINT)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    fun searchPartnersWithEmptySearchStringParamTest() {
        mockMvc.perform(
            MockMvcRequestBuilders.get(PARTNER_SEARCH_ENDPOINT)
                .param("searchString", "")
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.content()
                    .string("Передано пустое значение для поиска")
            )
    }


    @DbUnitDataSet(before = ["SearchApiServiceTest.searchWarehouseById.before.csv"])
    @Test
    fun searchWarehouseById() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/warehouses-for-promo/search?searchString=111")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").value(111))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].name").value("Яндекс.Маркет (Москва)"))
    }

    @DbUnitDataSet(before = ["SearchApiServiceTest.searchWarehousesByName.before.csv"])
    @Test
    fun searchWarehousesByName() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/warehouses-for-promo/search?searchString=Москва")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").value(111))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].name").value("Яндекс.Маркет (Москва)"))
    }

    @DbUnitDataSet(before = ["SearchApiServiceTest.searchWarehousesByName.before.csv"])
    @Test
    fun searchWarehousesByName_severalResults() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/warehouses-for-promo/search?searchString=маркет")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(2))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").value(111))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].name").value("Яндекс.Маркет (Москва)"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].id").value(113))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].name").value("Яндекс.Маркет (Иваново)"))
    }

    @Test
    fun searchWarehouseById_emptyResult() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/warehouses-for-promo/search?searchString=111")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(0))
    }

    @Test
    fun searchWarehousesByName_emptyResult() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/warehouses-for-promo/search?searchString=Москва")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(0))
    }

    @Test
    fun searchWarehousesByName_emptyRequest() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/warehouses-for-promo/search?searchString=")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(
                MockMvcResultMatchers.content()
                    .string("Передано пустое значение для поиска")
            )
    }
}
