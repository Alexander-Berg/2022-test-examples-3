package ru.yandex.market.adv.shop.integration.api.metrika.ApproveBusinessMetrikaDirect;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.adv.shop.integration.AbstractShopIntegrationMockServerTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@DisplayName("Тесты на endpoint POST /businesses/metrika/direct/approve.")
@MockServerSettings(ports = 12234)
public class ApproveBusinessMetrikaDirectTest extends AbstractShopIntegrationMockServerTest {

    private static final String USER_ID = "222";
    private static final String INVITATION_ID = "invitation1";

    @Autowired
    private MockMvc mvc;

    public ApproveBusinessMetrikaDirectTest(MockServerClient server) {
        super(server);
    }

    @DisplayName("Успешно подтвердили привязку бизнеса к аккаунту в Директе.")
    @DbUnitDataSet(
            before = {
                    "csv/approveBusinessMetrikaDirect_exist_BusinessMetrikaDirectBindingResponse.before.csv",
                    "csv/approveBusinessMetrikaDirect_Updater_empty.before.csv"
            },
            after = {
                    "csv/approveBusinessMetrikaDirect_exist_BusinessMetrikaDirectBindingResponse.after.csv",
                    "csv/approveBusinessMetrikaDirect_Updater_successOneRow.after.csv"
            }
    )
    @Test
    void approveBusinessMetrikaDirect_success_BusinessMetrikaDirectBindingResponse() throws Exception {

        mockPathNotification(200, "sendNotification_approved");
        check(status().isOk(),
                "approveBusinessMetrikaDirect_success_BusinessMetrikaDirectBindingResponse");
    }


    @DisplayName("Исключительная ситуация - подтверждение привязки просрочено.")
    @DbUnitDataSet(
            before = "csv/approveBusinessMetrikaDirect_expired_exception.before.csv"
    )
    @Test
    void approveBusinessMetrikaDirect_expired_exception() throws Exception {

        check(status().isGone(),
                "approveBusinessMetrikaDirect_expired_exception");
    }


    @DisplayName("Исключительная ситуация - информация по привязке не найдена.")
    @Test
    void approveBusinessMetrikaDirect_notFound_exception() throws Exception {

        check(status().isNotFound(),
                "approveBusinessMetrikaDirect_notFound_exception");
    }

    @DisplayName("Исключительная ситуация - запрос без user_id.")
    @Test
    void approveBusinessMetrikaDirect_withoutUserId_exception() throws Exception {

        mvc.perform(
                        post("/businesses/metrika/direct/approve")
                                .param("invitation_id", INVITATION_ID)
                )
                .andExpect(status().isBadRequest());
    }

    @DisplayName("Исключительная ситуация - ошибка отправки уведомления.")
    @DbUnitDataSet(
            before = "csv/approveBusinessMetrikaDirect_exist_BusinessMetrikaDirectBindingResponse.before.csv"
    )
    @Test
    void approveBusinessMetrikaDirect_notificationFailed_exception() throws Exception {

        mockPathNotification(500, "sendNotification_httpError");
        check(status().isBadRequest(), "approveBusinessMetrikaDirect_notificationFailed_exception");
    }


    private void check(ResultMatcher resultMatcher, String methodName) throws Exception {
        mvc.perform(
                        post("/businesses/metrika/direct/approve")
                                .param("user_id", USER_ID)
                                .param("invitation_id", INVITATION_ID)
                )
                .andExpect(resultMatcher)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(
                                loadFile("json/response/" + methodName + ".json"),
                                true
                        )
                );
    }

    private void mockPathNotification(int responseCode, String methodName) {
        mockServerPath("POST",
                "/notification/business",
                "json/request/" + methodName + ".json",
                Map.of(),
                responseCode,
                "json/response/" + methodName + "_response.json"
        );
    }
}
