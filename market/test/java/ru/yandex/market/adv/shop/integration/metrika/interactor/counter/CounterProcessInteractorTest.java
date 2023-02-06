package ru.yandex.market.adv.shop.integration.metrika.interactor.counter;

import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.shop.integration.AbstractShopIntegrationMockServerTest;
import ru.yandex.market.adv.shop.integration.metrika.exception.BusinessInfoNotFoundException;
import ru.yandex.market.adv.shop.integration.metrika.yt.entity.ContactInfo;
import ru.yandex.market.adv.shop.integration.metrika.yt.entity.Shopsdat;
import ru.yandex.market.adv.shop.integration.metrika.yt.entity.YtBusinessInfo;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.metrika.internal.client.exception.BadRequestException;

import static org.mockserver.model.MediaType.APPLICATION_XML;

@DisplayName("Тесты на интерактор CounterProcessInteractorImpl")
@MockServerSettings(ports = {12233, 12235})
class CounterProcessInteractorTest extends AbstractShopIntegrationMockServerTest {

    @Autowired
    private CounterProcessInteractor counterProcessInteractor;

    CounterProcessInteractorTest(MockServerClient server) {
        super(server);
    }

    @DisplayName("Успешно обработаны запросы типов CREATE, UPDATE, REMOVE")
    @DbUnitDataSet(
            before = {
                    "CounterProcessInteractorTest/csv/businessMetrikaCounter_threeRows_success.csv",
                    "CounterProcessInteractorTest/csv/businessMetrikaDirect_threeRows_success.csv",
                    "CounterProcessInteractorTest/csv/businessMetrikaUpdater_fourRows_success.csv"
            },
            after = {
                    "CounterProcessInteractorTest/csv/businessMetrikaCounter_threeRows_success.after.csv",
                    "CounterProcessInteractorTest/csv/businessMetrikaDirect_twoRows_success.csv",
                    "CounterProcessInteractorTest/csv/businessMetrikaUpdater_empty.csv"
            })
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = YtBusinessInfo.class,
                    path = "//tmp/business_process_allTypes_success_business"
            ),
            before = "CounterProcessInteractorTest/json/yt_businessInfo.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ContactInfo.class,
                    path = "//tmp/business_process_allTypes_success_mbi_contact_all_info"
            ),
            before = "CounterProcessInteractorTest/json/mbi_contact.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = Shopsdat.class,
                    path = "//tmp/business_process_allTypes_success_shopsdat"
            ),
            before = "CounterProcessInteractorTest/json/shopsdat.before.json"
    )
    @Test
    void process_allTypes_success() {

        mockPathMetrika("PUT", "/yandexservices/edit_counter/1", "updateCounter_correctData_11",
                "counter_11_success", Map.of(), 200);
        mockPathMetrika("PUT", "/yandexservices/edit_counter/2", "updateCounter_correctData_12",
                "counter_12_success", Map.of(), 200);
        mockPathMetrika("POST", "/yandexservices/add_counter", "createCounter_correctData_13",
                "counter_13_success", Map.of(), 200);

        mockPathBlackbox("user1", "accounts.login.uid", "blackbox_user1_answer");
        mockPathBlackbox("user4", "accounts.login.uid", "blackbox_user4_answer");

        run("business_process_allTypes_success_",
                () -> counterProcessInteractor.process()
        );
    }

    @DisplayName("Успешно обработаны запросы c email'ом вместо логина")
    @DbUnitDataSet(
            before = {
                    "CounterProcessInteractorTest/csv/businessMetrikaCounter_threeRows_success.csv",
                    "CounterProcessInteractorTest/csv/businessMetrikaDirect_withEmails_success.csv",
                    "CounterProcessInteractorTest/csv/businessMetrikaUpdater_fourRows_success.csv"
            },
            after = {
                    "CounterProcessInteractorTest/csv/businessMetrikaCounter_threeRows_success.after.csv",
                    "CounterProcessInteractorTest/csv/businessMetrikaDirect_withEmails_success.after.csv",
                    "CounterProcessInteractorTest/csv/businessMetrikaUpdater_empty.csv"
            })
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = YtBusinessInfo.class,
                    path = "//tmp/business_process_withEmails_success_business"
            ),
            before = "CounterProcessInteractorTest/json/yt_businessInfo.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ContactInfo.class,
                    path = "//tmp/business_process_withEmails_success_mbi_contact_all_info"
            ),
            before = "CounterProcessInteractorTest/json/mbi_contact.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = Shopsdat.class,
                    path = "//tmp/business_process_withEmails_success_shopsdat"
            ),
            before = "CounterProcessInteractorTest/json/shopsdat.before.json"
    )
    @Test
    void process_withEmails_success() {

        mockPathMetrika("PUT", "/yandexservices/edit_counter/1", "updateCounter_test_11",
                "counter_11_success", Map.of(), 200);
        mockPathMetrika("PUT", "/yandexservices/edit_counter/2", "updateCounter_test1_12",
                "counter_12_success", Map.of(), 200);
        mockPathMetrika("POST", "/yandexservices/add_counter", "createCounter_correctData_13",
                "counter_13_success", Map.of(), 200);

        mockPathBlackbox("test@yandex.ru", "account_info.fio.uid", "blackbox_test_answer");
        mockPathBlackbox("test1@yandex.ru", "account_info.fio.uid", "blackbox_test1_answer");
        mockPathBlackbox("user1", "accounts.login.uid", "blackbox_user1_answer");
        mockPathBlackbox("user4", "accounts.login.uid", "blackbox_user4_answer");

        run("business_process_withEmails_success_",
                () -> counterProcessInteractor.process()
        );
    }

    @DisplayName("Успешно обработаны запросы c additionalLogins")
    @DbUnitDataSet(
            before = {
                    "CounterProcessInteractorTest/csv/businessMetrikaCounter_withAdditionalLogins_success.csv",
                    "CounterProcessInteractorTest/csv/businessMetrikaDirect_withAdditionalLogins_success.csv",
                    "CounterProcessInteractorTest/csv/businessMetrikaUpdater_twoRows_success.csv"
            },
            after = {
                    "CounterProcessInteractorTest/csv/businessMetrikaCounter_withAdditionalLogins_success.csv",
                    "CounterProcessInteractorTest/csv/businessMetrikaDirect_withAdditionalLogins_success.csv",
                    "CounterProcessInteractorTest/csv/businessMetrikaUpdater_empty.csv"
            })
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = YtBusinessInfo.class,
                    path = "//tmp/business_process_withAdditionalLogins_success_business"
            ),
            before = "CounterProcessInteractorTest/json/yt_businessInfo.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ContactInfo.class,
                    path = "//tmp/business_process_withAdditionalLogins_success_" +
                            "mbi_contact_all_info"
            )
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = Shopsdat.class,
                    path = "//tmp/business_process_withAdditionalLogins_success_shopsdat"
            )
    )
    @Test
    void process_withAdditionalLogins_success() {

        mockPathMetrika("PUT", "/yandexservices/edit_counter/1",
                "updateCounter_withAdditionalLogins_11",
                "counter_11_withAdditionalLogins", Map.of(), 200);
        mockPathMetrika("PUT", "/yandexservices/edit_counter/2",
                "updateCounter_withAdditionalLogins_12",
                "counter_12_withAdditionalLogins", Map.of(), 200);

        mockPathBlackbox("test@yandex.ru", "account_info.fio.uid", "blackbox_test_answer");
        mockPathBlackbox("test1@yandex.ru", "account_info.fio.uid", "blackbox_test1_answer");
        mockPathBlackbox("test2", "accounts.login.uid", "blackbox_test2_answer");
        mockPathBlackbox("test3", "accounts.login.uid", "blackbox_test3_answer");
        mockPathBlackbox("test4", "accounts.login.uid", "blackbox_test4_answer");

        run("business_process_withAdditionalLogins_success_",
                () -> counterProcessInteractor.process()
        );
    }

    @DisplayName("Успешно обработаны запросы c дублями в логинах")
    @DbUnitDataSet(
            before = {
                    "CounterProcessInteractorTest/csv/businessMetrikaCounter_withAdditionalLogins_success.csv",
                    "CounterProcessInteractorTest/csv/businessMetrikaDirect_withAdditionalLoginsDuplicates_success.csv",
                    "CounterProcessInteractorTest/csv/businessMetrikaUpdater_twoRows_success.csv"
            },
            after = {
                    "CounterProcessInteractorTest/csv/businessMetrikaCounter_withAdditionalLogins_success.csv",
                    "CounterProcessInteractorTest/csv/businessMetrikaDirect_withAdditionalLoginsDuplicates_success.csv",
                    "CounterProcessInteractorTest/csv/businessMetrikaUpdater_empty.csv"
            })
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = YtBusinessInfo.class,
                    path = "//tmp/business_process_withAdditionalLoginsDuplicates_success_business"
            ),
            before = "CounterProcessInteractorTest/json/yt_businessInfo.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ContactInfo.class,
                    path = "//tmp/business_process_withAdditionalLoginsDuplicates_success_" +
                            "mbi_contact_all_info"
            )
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = Shopsdat.class,
                    path = "//tmp/business_process_withAdditionalLoginsDuplicates_success_shopsdat"
            )
    )
    @Test
    void process_withAdditionalLoginsDuplicates_success() {

        mockPathMetrika("PUT", "/yandexservices/edit_counter/1",
                "updateCounter_withAdditionalLoginsDuplicates_11",
                "counter_11_withAdditionalLoginsDuplicates", Map.of(), 200);
        mockPathMetrika("PUT", "/yandexservices/edit_counter/2",
                "updateCounter_withAdditionalLoginsDuplicates_12",
                "counter_12_withAdditionalLoginsDuplicates", Map.of(), 200);

        mockPathBlackbox("test2@yandex.ru", "account_info.fio.uid", "blackbox_test2Email_answer");
        mockPathBlackbox("test3@yandex.ru", "account_info.fio.uid", "blackbox_test3Email_answer");
        mockPathBlackbox("test2", "accounts.login.uid", "blackbox_test2_answer");
        mockPathBlackbox("test3", "accounts.login.uid", "blackbox_test3_answer");
        mockPathBlackbox("test4", "accounts.login.uid", "blackbox_test4_answer");

        run("business_process_withAdditionalLoginsDuplicates_success_",
                () -> counterProcessInteractor.process()
        );
    }

    @DisplayName("Исключительная ситуация - один из счетчиков не найден")
    @DbUnitDataSet(
            before = {
                    "CounterProcessInteractorTest/csv/businessMetrikaCounter_threeRows_success.csv",
                    "CounterProcessInteractorTest/csv/businessMetrikaDirect_withEmails_success.csv",
                    "CounterProcessInteractorTest/csv/businessMetrikaUpdater_fourRows_success.csv"
            },
            after = {
                    "CounterProcessInteractorTest/csv/businessMetrikaCounter_threeRows_success.after.csv",
                    "CounterProcessInteractorTest/csv/businessMetrikaDirect_withEmails_success.after.csv",
                    "CounterProcessInteractorTest/csv/businessMetrikaUpdater_oneRow.csv"
            })
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = YtBusinessInfo.class,
                    path = "//tmp/business_process_counterNotFound_exception_business"
            ),
            before = "CounterProcessInteractorTest/json/yt_businessInfo.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ContactInfo.class,
                    path = "//tmp/business_process_counterNotFound_exception_mbi_contact_all_info"
            ),
            before = "CounterProcessInteractorTest/json/mbi_contact.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = Shopsdat.class,
                    path = "//tmp/business_process_counterNotFound_exception_shopsdat"
            ),
            before = "CounterProcessInteractorTest/json/shopsdat.before.json"
    )
    @Test
    void process_counterNotFound_exception() {

        mockPathMetrika("PUT", "/yandexservices/edit_counter/1", "updateCounter_test_11",
                "counter_11_notExist", Map.of(), 400);
        mockPathMetrika("PUT", "/yandexservices/edit_counter/2", "updateCounter_test1_12",
                "counter_12_success", Map.of(), 200);
        mockPathMetrika("POST", "/yandexservices/add_counter", "createCounter_correctData_13",
                "counter_13_success", Map.of(), 200);

        mockPathBlackbox("test@yandex.ru", "account_info.fio.uid", "blackbox_test_answer");
        mockPathBlackbox("test1@yandex.ru", "account_info.fio.uid", "blackbox_test1_answer");
        mockPathBlackbox("user1", "accounts.login.uid", "blackbox_user1_answer");
        mockPathBlackbox("user4", "accounts.login.uid", "blackbox_user4_answer");

        run("business_process_counterNotFound_exception_",
                () -> Assertions.assertThatThrownBy(() -> counterProcessInteractor.process())
                        .isInstanceOf(BadRequestException.class)
        );
    }

    @DisplayName("Исключительная ситуация - информация о бизнесе не найдена")
    @DbUnitDataSet(
            before = {
                    "CounterProcessInteractorTest/csv/businessMetrikaCounter_threeRows_success.csv",
                    "CounterProcessInteractorTest/csv/businessMetrikaDirect_threeRows_success.csv",
                    "CounterProcessInteractorTest/csv/businessMetrikaUpdater_fourRows_success.csv"
            },
            after = {
                    "CounterProcessInteractorTest/csv/businessMetrikaCounter_twoRows_success.after.csv",
                    "CounterProcessInteractorTest/csv/businessMetrikaDirect_twoRows_success.csv",
                    "CounterProcessInteractorTest/csv/businessMetrikaUpdater_oneRow13.after.csv"
            })
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = YtBusinessInfo.class,
                    path = "//tmp/business_process_businessInfoNotFound_exception_business"
            ),
            before = "CounterProcessInteractorTest/json/yt_businessInfo_exception.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ContactInfo.class,
                    path = "//tmp/business_process_businessInfoNotFound_exception_" +
                            "mbi_contact_all_info"
            ),
            before = "CounterProcessInteractorTest/json/mbi_contact.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = Shopsdat.class,
                    path = "//tmp/business_process_businessInfoNotFound_exception_shopsdat"
            ),
            before = "CounterProcessInteractorTest/json/shopsdat.before.json"
    )
    @Test
    void process_businessInfoNotFound_exception() {

        mockPathMetrika("PUT", "/yandexservices/edit_counter/1", "updateCounter_correctData_11",
                "counter_11_success", Map.of(), 200);
        mockPathMetrika("PUT", "/yandexservices/edit_counter/2", "updateCounter_correctData_12",
                "counter_12_success", Map.of(), 200);
        mockPathMetrika("POST", "/yandexservices/add_counter", "createCounter_correctData_13",
                "counter_13_success", Map.of(), 200);

        mockPathBlackbox("user1", "accounts.login.uid", "blackbox_user1_answer");
        mockPathBlackbox("user4", "accounts.login.uid", "blackbox_user4_answer");

        run("business_process_businessInfoNotFound_exception_",
                () -> Assertions.assertThatThrownBy(
                        () -> counterProcessInteractor.process()
                ).isInstanceOf(BusinessInfoNotFoundException.class)
        );
    }

    private void mockPathMetrika(String method, String path, String requestFile, String responseFile,
                                 Map<String, List<String>> parameters, int responseCode) {
        mockServerPath(
                method,
                path,
                requestFile == null ? null : "CounterProcessInteractorTest/json/request/" + requestFile + ".json",
                parameters,
                responseCode,
                "CounterProcessInteractorTest/json/response/" + responseFile + ".json"
        );
    }

    private void mockPathBlackbox(String login, String dbfields, String responseFile) {
        mockServerPath("GET",
                "/blackbox",
                () -> null,
                Map.of(
                        "dbfields", List.of(dbfields),
                        "method", List.of("userinfo"),
                        "emails", List.of("getdefault"),
                        "userip", List.of("127.0.0.1"),
                        "login", List.of(login)
                ),
                200,
                "CounterProcessInteractorTest/xml/response/" + responseFile + ".xml",
                APPLICATION_XML
        );
    }
}
