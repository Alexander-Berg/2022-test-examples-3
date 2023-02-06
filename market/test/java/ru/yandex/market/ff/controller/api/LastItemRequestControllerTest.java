package ru.yandex.market.ff.controller.api;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.ff.base.MvcIntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.ff.util.FileContentUtils.getFileContent;

@DatabaseSetup("classpath:controller/last-request-item-api/items.xml")
@ExpectedDatabase(value = "classpath:controller/last-request-item-api/items.xml", assertionMode = NON_STRICT)
class LastItemRequestControllerTest extends MvcIntegrationTest {

    @Test
    @DisplayName("Один поставщик, один артикул")
    void findItems() throws Exception {
        MvcResult result = mockMvc.perform(
                post("/suppliers/1/last-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"shopId\": 1," +
                                " \"articles\": [" +
                                "  \"abc\"" +
                                " ]" +
                                "}")
        ).andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        assertResultExpected(result, "one-item-success.json");
    }

    @Test
    @DisplayName("Один поставщик, один артикул, один склад")
    void testWithServiceId() throws Exception {
        MvcResult result = mockMvc.perform(
                post("/suppliers/1/last-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"shopId\": 1," +
                                " \"articles\": [" +
                                "  \"abc\"" +
                                " ]," +
                                " \"serviceIds\": [" +
                                "  100" +
                                " ]" +
                                "}")
        ).andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        assertResultExpected(result, "one-item-success.json");
    }

    @Test
    @DisplayName("Один поставщик, один артикул, склад по которому нет поставок")
    void testWithServiceIdNoSupplies() throws Exception {
        MvcResult result = mockMvc.perform(
                post("/suppliers/1/last-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"shopId\": 1," +
                                " \"articles\": [" +
                                "  \"abc\"" +
                                " ]," +
                                " \"serviceIds\": [" +
                                "  200" +
                                " ]" +
                                "}")
        ).andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        assertResultExpected(result, "empty-response.json");
    }

    @Test
    @DisplayName("Один поставщик, два артикула и 2 поставки. Взять свежую")
    void findItemsTwoSuccess() throws Exception {
        MvcResult result = mockMvc.perform(
                post("/suppliers/2/last-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"shopId\": 2," +
                                " \"articles\": [" +
                                "  \"def\"," +
                                "  \"ghk\"" +
                                " ]" +
                                "}")
        ).andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        assertResultExpected(result, "two-items-success.json");
    }

    @Test
    @DisplayName("Один поставщик, два артикула один артикул в двух поставках, один в одной")
    void findItemsTestLast() throws Exception {
        MvcResult result = mockMvc.perform(
                post("/suppliers/2/last-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"shopId\": 2," +
                                " \"articles\": [" +
                                "  \"abc\"," +
                                "  \"def\"" +
                                " ]" +
                                "}")
        ).andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        assertResultExpected(result, "two-items-one-last-success.json");
    }

    @Test
    @DisplayName("Конвенция на фильтр сапплаера по PATH соблюдена")
    void testBadRequest() throws Exception {
        MvcResult result = mockMvc.perform(
                post("/suppliers/1000/last-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"shopId\": 1," +
                                " \"articles\": [" +
                                "  \"abc\"" +
                                " ]" +
                                "}")
        ).andDo(print())
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    private void assertResultExpected(MvcResult result, String filename) throws Exception {
        String expected = getFileContent("controller/last-request-item-api/" + filename);
        JSONAssert.assertEquals(expected, result.getResponse().getContentAsString(), JSONCompareMode.LENIENT);
    }
}
