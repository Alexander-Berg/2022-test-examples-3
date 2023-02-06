package ru.yandex.market.pricingmgmt.api.catstream

import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.api.ControllerTest
import ru.yandex.mj.generated.server.model.CatstreamDto
import ru.yandex.mj.generated.server.model.ErrorResponse
import java.util.*

internal class CatstreamApiServiceTest : ControllerTest() {

    @Test
    @DbUnitDataSet(
        before = ["CatstreamApiServiceTest.getCatstreams.csv"]
    )
    fun getCatstreams() {
        val expectedResponse = listOf(
            CatstreamDto().catstream("FMCG").catteam("FMCG"),
            CatstreamDto().catstream("Flowers").catteam("FMCG")
        )
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/catstreams")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().json(dtoToString(expectedResponse)))
    }

    @Test
    @DbUnitDataSet(
        before = ["CatstreamApiServiceTest.uploadDictionary_ok.before.csv"],
        after = ["CatstreamApiServiceTest.uploadDictionary_ok.after.csv"]
    )
    fun uploadDictionary_ok() {
        uploadDictionary("/xlsx-template/catstreams/dictionary.xlsx")
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DbUnitDataSet(
        before = ["CatstreamApiServiceTest.reloadDictionary_ok.before.csv"],
        after = ["CatstreamApiServiceTest.reloadDictionary_ok.after.csv"]
    )
    fun reloadDictionary_ok() {
        uploadDictionary("/xlsx-template/catstreams/dictionary.xlsx")
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    fun uploadDictionary_emptyFields() {
        val expectedResponse = ErrorResponse()
            .errorCode("CATSTREAM_DICTIONARY_INVALID")
            .message("Ошибка загрузки словаря катстримов. Заполнены не все департаменты и стримы.")
            .errorFields(Collections.emptyList())
        uploadDictionary("/xlsx-template/catstreams/dictionary-with-empty-column.xlsx")
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.content().json(dtoToString(expectedResponse)))
    }

    @Test
    fun uploadDictionary_catteamNotFound() {
        val expectedResponse = ErrorResponse()
            .errorCode("CATSTREAM_DICTIONARY_DEPARTMENTS_NOT_FOUND")
            .message(
                "Ошибка загрузки словаря катстримов. В системе не найдены следующие департаменты: " +
                    "FMCG, Товары для дома, Детские товары, ЭиБТ, DIY & Auto, Фарма, Fashion"
            )
            .errorFields(Collections.emptyList())
        uploadDictionary("/xlsx-template/catstreams/dictionary.xlsx")
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.content().json(dtoToString(expectedResponse)))
    }

    private fun uploadDictionary(filename: String): ResultActions {
        val bytes = javaClass.getResourceAsStream(filename)?.readAllBytes()
        val file = MockMultipartFile("excelFile", filename, "application/vnd.ms-excel", bytes)
        return mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/catstreams/upload").file(file))
    }
}
