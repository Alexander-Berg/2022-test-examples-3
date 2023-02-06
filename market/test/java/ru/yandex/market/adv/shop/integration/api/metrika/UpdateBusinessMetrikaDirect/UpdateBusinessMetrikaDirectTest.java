package ru.yandex.market.adv.shop.integration.api.metrika.UpdateBusinessMetrikaDirect;

import java.util.List;
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

import static org.mockserver.model.MediaType.APPLICATION_XML;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@DisplayName("Тесты на endpoint POST /businesses/{businessId}/metrika/direct.")
@MockServerSettings(ports = {12233, 12234})
public class UpdateBusinessMetrikaDirectTest extends AbstractShopIntegrationMockServerTest {

    private static final String BUSINESS_ID = "111";
    private static final String EUID_ENABLED = "222";
    private static final String EUID_PENDING = "333";
    private static final String LOGIN = "login1";
    private static final String BAD_LOGIN = "BAD_LOGIN";

    @Autowired
    private MockMvc mvc;

    public UpdateBusinessMetrikaDirectTest(MockServerClient server) {
        super(server);
    }


    @DisplayName("Обновили привязку бизнеса к аккаунту в Директе - статус ENABLED.")
    @DbUnitDataSet(
            before = {
                    "csv/updateBusinessMetrikaDirect_exist_BusinessMetrikaDirectBindingResponse.before.csv",
                    "csv/updateBusinessMetrikaDirect_Updater_empty.before.csv"
            },
            after = {
                    "csv/updateBusinessMetrikaDirect_enabled_BusinessMetrikaDirectBindingResponse.after.csv",
                    "csv/updateBusinessMetrikaDirect_Updater_successOneRow.after.csv"
            }
    )
    @Test
    void updateBusinessMetrikaDirect_withoutNotification_BusinessMetrikaDirectBindingResponse() throws Exception {

        check(BUSINESS_ID, EUID_ENABLED, LOGIN, status().isOk(),
                "updateBusinessMetrikaDirect_withoutNotification_BusinessMetrikaDirectBindingResponse");
    }

    @DisplayName("Обновили привязку бизнеса к аккаунту в Директе - статус PENDING.")
    @DbUnitDataSet(
            before = {
                    "csv/updateBusinessMetrikaDirect_exist_BusinessMetrikaDirectBindingResponse.before.csv",
                    "csv/updateBusinessMetrikaDirect_Updater_empty.before.csv"
            },
            after = {
                    "csv/updateBusinessMetrikaDirect_pending_BusinessMetrikaDirectBindingResponse.after.csv",
                    "csv/updateBusinessMetrikaDirect_Updater_empty.before.csv"
            }
    )
    @Test
    void updateBusinessMetrikaDirect_withNotification_BusinessMetrikaDirectBindingResponse() throws Exception {

        mockPathNotification(200, "sendNotification_invite");
        check(BUSINESS_ID, EUID_PENDING, LOGIN, status().isOk(),
                "updateBusinessMetrikaDirect_withNotification_BusinessMetrikaDirectBindingResponse");
    }

    @DisplayName("Создали новую привязку бизнеса к аккаунту в Директе - статус PENDING.")
    @DbUnitDataSet(
            before = {
                    "csv/updateBusinessMetrikaDirect_notExist_BusinessMetrikaDirectBindingResponse.before.csv",
                    "csv/updateBusinessMetrikaDirect_Updater_empty.before.csv"
            },
            after = {
                    "csv/updateBusinessMetrikaDirect_pending_BusinessMetrikaDirectBindingResponse.after.csv",
                    "csv/updateBusinessMetrikaDirect_Updater_empty.before.csv"
            }
    )
    @Test
    void updateBusinessMetrikaDirect_notExist_BusinessMetrikaDirectBindingResponse() throws Exception {

        mockPathNotification(200, "sendNotification_invite");
        check(BUSINESS_ID, EUID_PENDING, LOGIN, status().isOk(),
                "updateBusinessMetrikaDirect_withNotification_BusinessMetrikaDirectBindingResponse");
    }

    @DisplayName("Не создали новую привязку со статусом DISABLED.")
    @DbUnitDataSet(
            before = {
                    "csv/updateBusinessMetrikaDirect_notExist_BusinessMetrikaDirectBindingResponse.before.csv",
                    "csv/updateBusinessMetrikaDirect_Updater_empty.before.csv"
            },
            after = {
                    "csv/updateBusinessMetrikaDirect_notExistDisabled_BusinessMetrikaDirectBindingResponse.after.csv",
                    "csv/updateBusinessMetrikaDirect_Updater_empty.before.csv"
            }
    )
    @Test
    void updateBusinessMetrikaDirect_notExistDisabled_BusinessMetrikaDirectBindingResponse() throws Exception {

        checkWithoutLogin(BUSINESS_ID, EUID_PENDING, status().isOk(),
                "updateBusinessMetrikaDirect_notExistDisabled_BusinessMetrikaDirectBindingResponse");
    }

    @DisplayName("Обновили привязку бизнеса к аккаунту в Директе - статус DISABLED.")
    @DbUnitDataSet(
            before = {
                    "csv/updateBusinessMetrikaDirect_exist_BusinessMetrikaDirectBindingResponse.before.csv",
                    "csv/updateBusinessMetrikaDirect_Updater_empty.before.csv"
            },
            after = {
                    "csv/updateBusinessMetrikaDirect_disabled_BusinessMetrikaDirectBindingResponse.after.csv",
                    "csv/updateBusinessMetrikaDirect_Updater_successOneRow.after.csv"
            }
    )
    @Test
    void updateBusinessMetrikaDirect_withoutLogin_BusinessMetrikaDirectBindingResponse() throws Exception {

        checkWithoutLogin(BUSINESS_ID, EUID_PENDING, status().isOk(),
                "updateBusinessMetrikaDirect_withoutLogin_BusinessMetrikaDirectBindingResponse");
    }

    @DisplayName("Исключительная ситуация при запросе без euid.")
    @DbUnitDataSet(
            before = "csv/updateBusinessMetrikaDirect_exist_BusinessMetrikaDirectBindingResponse.before.csv"
    )
    @Test
    void updateBusinessMetrikaDirect_withoutEuid_Exception() throws Exception {

        mvc.perform(
                        post("/businesses/" + BUSINESS_ID + "/metrika/direct")
                )
                .andExpect(status().isBadRequest());
    }

    @DisplayName("Исключительная ситуация - юзер не найден по логину.")
    @Test
    void updateBusinessMetrikaDirect_userNotFound_exception() throws Exception {

        mockPathBlackbox(BAD_LOGIN);
        mvc.perform(
                        post("/businesses/" + BUSINESS_ID + "/metrika/direct")
                                .param("euid", EUID_ENABLED)
                                .param("login", BAD_LOGIN)
                )
                .andExpect(status().isNotFound());
    }

    @DisplayName("Исключительная ситуация - ошибка отправки уведомления.")
    @DbUnitDataSet(
            before = "csv/updateBusinessMetrikaDirect_exist_BusinessMetrikaDirectBindingResponse.before.csv"
    )
    @Test
    void updateBusinessMetrikaDirect_notificationFailed_exception() throws Exception {

        mockPathNotification(500, "sendNotification_httpError");
        check(BUSINESS_ID, EUID_PENDING, LOGIN, status().isBadRequest(),
                "updateBusinessMetrikaDirect_notificationFailed_exception");
    }

    private void check(String businessId, String euid, String login, ResultMatcher resultMatcher, String methodName)
            throws Exception {

        mockPathBlackbox(login);
        mvc.perform(
                        post("/businesses/" + businessId + "/metrika/direct")
                                .param("euid", euid)
                                .param("login", login)
                )
                .andExpect(resultMatcher)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(
                                loadFile("json/response/" + methodName + ".json"),
                                true
                        )
                );
    }

    private void checkWithoutLogin(String businessId, String euid, ResultMatcher resultMatcher, String methodName)
            throws Exception {
        mvc.perform(
                        post("/businesses/" + businessId + "/metrika/direct")
                                .param("euid", euid)
                )
                .andExpect(resultMatcher)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(
                                loadFile("json/response/" + methodName + ".json"),
                                true
                        )
                );
    }

    private void mockPathBlackbox(String login) {

        String responseFile = login.equals(BAD_LOGIN)
                ? "json/response/blackbox_notExist_answer.xml"
                : "json/response/blackbox_exist_answer.xml";

        mockServerPath("GET",
                "/blackbox",
                () -> null,
                Map.of(
                            "dbfields", List.of("accounts.login.uid"),
                            "method", List.of("userinfo"),
                            "emails", List.of("getdefault"),
                            "userip", List.of("127.0.0.1"),
                            "login", List.of()
                ),
                200,
                responseFile,
                APPLICATION_XML
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
