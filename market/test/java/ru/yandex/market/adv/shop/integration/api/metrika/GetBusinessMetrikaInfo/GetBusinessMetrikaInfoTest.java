package ru.yandex.market.adv.shop.integration.api.metrika.GetBusinessMetrikaInfo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.adv.shop.integration.AbstractShopIntegrationTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Тесты на endpoint GET /businesses/{businessId}/metrika.")
public class GetBusinessMetrikaInfoTest extends AbstractShopIntegrationTest {

    private static final String BUSINESS_ID = "111";

    @Autowired
    private MockMvc mvc;

    @DisplayName("Вернули информацию по Яндекс Метрике бизнеса.")
    @DbUnitDataSet(
            before = "csv/getBusinessMetrikaInfo_exist_BusinessMetrikaInfoResponse.csv"
    )
    @Test
    void getBusinessMetrikaInfo_exist_BusinessMetrikaInfoResponse() throws Exception {

        check(BUSINESS_ID, status().isOk(), "getBusinessMetrikaInfo_exist_BusinessMetrikaInfoResponse");
    }

    @DisplayName("Информации по Яндекс Метрике бизнеса нет в БД. Ошибка 404.")
    @DbUnitDataSet(
            before = "csv/getBusinessMetrikaInfo_empty.csv"
    )
    @Test
    void getBusinessMetrikaInfo_notExist_notFound() throws Exception {

        check(BUSINESS_ID, status().isNotFound(), "getBusinessMetrikaInfo_notExist_notFound");
    }

    @DisplayName("Неверный businessId при получении информации по Яндекс Метрике бизнеса. Ошибка 400.")
    @DbUnitDataSet(
            before = "csv/getBusinessMetrikaInfo_empty.csv"
    )
    @Test
    void getBusinessMetrikaInfo_wrongBusinessId_badRequest() throws Exception {

        check("AAA", status().isBadRequest(), "getBusinessMetrikaInfo_wrongBusinessId_badRequest");
    }


    private void check(String businessId, ResultMatcher resultMatcher, String methodName) throws Exception {
        mvc.perform(
                        get("/businesses/" + businessId + "/metrika")
                )
                .andExpect(resultMatcher)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(
                                loadFile("json/response/" + methodName + ".json"),
                                true
                        )
                );
    }
}
