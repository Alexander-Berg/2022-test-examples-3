package ru.yandex.market.adv.shop.integration.api.metrika.GetBusinessMetrikaDirect;

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

@DisplayName("Тесты на endpoint GET /businesses/{businessId}/metrika/direct.")
public class GetBusinessMetrikaDirectTest extends AbstractShopIntegrationTest {

    private static final String BUSINESS_ID = "111";

    @Autowired
    private MockMvc mvc;

    @DisplayName("Вернули информацию по привязанному аккаунту бизнеса в Директе.")
    @DbUnitDataSet(
            before = "csv/getBusinessMetrikaDirect_exist_BusinessMetrikaDirectBindingResponse.csv"
    )
    @Test
    void getBusinessMetrikaDirect_exist_BusinessMetrikaDirectBindingResponse() throws Exception {

        check(BUSINESS_ID, status().isOk(), "getBusinessMetrikaDirect_exist_BusinessMetrikaDirectBindingResponse");
    }

    @DisplayName("Информации по привязанному аккаунту бизнеса в Директе нет в БД. Ошибка 404.")
    @DbUnitDataSet(
            before = "csv/getBusinessMetrikaDirect_empty.csv"
    )
    @Test
    void getBusinessMetrikaDirect_notExist_notFound() throws Exception {

        check(BUSINESS_ID, status().isNotFound(), "getBusinessMetrikaDirect_notExist_notFound");
    }

    @DisplayName("Неверный businessId при получении информации по привязанному аккаунту в Директе. Ошибка 400.")
    @DbUnitDataSet(
            before = "csv/getBusinessMetrikaDirect_empty.csv"
    )
    @Test
    void getBusinessMetrikaDirect_wrongBusinessId_badRequest() throws Exception {

        check("AAA", status().isBadRequest(),
                "getBusinessMetrikaDirect_wrongBusinessId_badRequest");
    }

    private void check(String businessId, ResultMatcher resultMatcher, String methodName) throws Exception {
        mvc.perform(
                        get("/businesses/" + businessId + "/metrika/direct")
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
