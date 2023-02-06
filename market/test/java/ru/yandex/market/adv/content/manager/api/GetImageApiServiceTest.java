package ru.yandex.market.adv.content.manager.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.adv.content.manager.AbstractContentManagerTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Тесты на endpoint GET /v1/image.")
class GetImageApiServiceTest extends AbstractContentManagerTest {

    @Autowired
    private MockMvc mvc;

    @DisplayName("Вернули список сохраненных изображений.")
    @DbUnitDataSet(
            before = "ImageApi/Get/csv/v1ImageGet_exist_imageResponse.before.csv"
    )
    @Test
    void v1ImageGet_exist_imageResponse() throws Exception {
        v1ImageGet("v1ImageGet_exist_imageResponse", status().isOk(), "443");
    }

    @DisplayName("Вернули пустой список сохраненных изображений.")
    @Test
    void v1ImageGet_notExist_imageResponse() throws Exception {
        v1ImageGet("v1ImageGet_notExist_imageResponse", status().isOk(), "453");
    }

    private void v1ImageGet(String methodName, ResultMatcher statusMatcher,
                            String businessId) throws Exception {
        mvc.perform(
                        get("/v1/image?business_id=" + businessId)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(statusMatcher)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(
                                loadFile("ImageApi/Get/json/response/" + methodName + ".json"),
                                true
                        )
                );
    }
}
