package ru.yandex.metrika.internal.client.http;

import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.metrika.internal.client.api.MetrikaInternalClient;
import ru.yandex.metrika.internal.client.model.CodeOptions;
import ru.yandex.metrika.internal.client.model.CounterBrief;
import ru.yandex.metrika.internal.client.model.CounterMirror;
import ru.yandex.metrika.internal.client.model.CounterRoot;
import ru.yandex.metrika.internal.client.model.Grant;
import ru.yandex.metrika.internal.client.model.GrantRoot;
import ru.yandex.metrika.internal.client.model.GrantType;
import ru.yandex.metrika.internal.client.model.WebvisorArchType;
import ru.yandex.metrika.internal.client.model.WebvisorOptions;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@MockServerSettings(ports = 12233)
class MetrikaInternalClientTest extends AbstractMetrikaInternalMockServerTest {

    private static final long COUNTER_ID = 1L;
    private static final String USER_LOGIN = "test_user";

    @Autowired
    private MetrikaInternalClient metrikaInternalClient;

    MetrikaInternalClientTest(MockServerClient server) {
        super(server);
    }

    @Test
    @DisplayName("Успешное создание счетчика метрики")
    void createCounter_correctData_success() {
        initMock("POST", "/yandexservices/add_counter", null,
                "createCounter_correctData_success",
                Map.of()
        );

        Assertions.assertThat(metrikaInternalClient.createCounter(getCounterRoot()))
                .isEqualTo(getCounterRootWithId());
    }

    @Test
    @DisplayName("Успешное обновление счетчика метрики")
    void editCounter_correctData_success() {
        initMock("PUT", "/yandexservices/edit_counter/" + COUNTER_ID, null,
                "editCounter_correctData_success",
                Map.of()
        );

        Assertions.assertThat(metrikaInternalClient.updateCounter(COUNTER_ID, getCounterRootWithId()))
                .isEqualTo(getUpdatedCounterRootWithId());
    }

    @Test
    @DisplayName("Успешное добавление прав доступа")
    void addGrant_correctData_success() {

        initMock("POST", "/yandexservices/counter/" + COUNTER_ID + "/grants", "addGrant_body",
                "addGrant_body",
                Map.of()
        );

        Assertions.assertThatNoException().isThrownBy(() -> metrikaInternalClient.addGrant(COUNTER_ID, getGrantRoot()));
    }

    @Test
    @DisplayName("Успешный отзыв прав доступа")
    void deleteGrant_correctData_success() {

        server
                .when(request()
                        .withMethod("DELETE")
                        .withPath("/yandexservices/counter/" + COUNTER_ID + "/grants")
                        .withQueryStringParameter("user_login", USER_LOGIN)
                )
                .respond(response()
                        .withStatusCode(200)
                );

        Assertions.assertThatNoException().isThrownBy(() -> metrikaInternalClient.deleteGrant(COUNTER_ID, USER_LOGIN));
    }

    @Test
    @DisplayName("Создание счетчика метрики возвращает неверный ответ и завершается исключением")
    void createCounter_errorResponse_exception() {

        server
                .when(request()
                        .withMethod("POST")
                        .withPath("/yandexservices/add_counter")
                )
                .respond(response()
                        .withStatusCode(500)
                        .withBody("error")
                );

        Assertions.assertThatThrownBy(() -> metrikaInternalClient.createCounter(getCounterRoot()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("MetrikaInternal response Internal Server Error error: error");
    }

    @Test
    @DisplayName("Обновление счетчика метрики возвращает неверный ответ и завершается исключением")
    void editCounter_errorResponse_exception() {

        server
                .when(request()
                        .withMethod("PUT")
                        .withPath("/yandexservices/edit_counter/" + COUNTER_ID)
                )
                .respond(response()
                        .withStatusCode(500)
                        .withBody("error")
                );

        Assertions.assertThatThrownBy(() -> metrikaInternalClient.updateCounter(
                                COUNTER_ID,
                                getCounterRootWithId()
                        )
                )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("MetrikaInternal response Internal Server Error error: error");
    }

    @Test
    @DisplayName("Добавление прав доступа возвращает неверный ответ и завершается исключением")
    void addGrant_errorResponse_exception() {

        server
                .when(request()
                        .withMethod("POST")
                        .withPath("/yandexservices/counter/" + COUNTER_ID + "/grants")
                )
                .respond(response()
                        .withStatusCode(500)
                        .withBody("error")
                );

        Assertions.assertThatThrownBy(() -> metrikaInternalClient.addGrant(COUNTER_ID, getGrantRoot()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("MetrikaInternal response Internal Server Error error: error");
    }

    @Test
    @DisplayName("Отзыв прав доступа возвращает неверный ответ и завершается исключением")
    void deleteGrant_errorResponse_exception() {

        server
                .when(request()
                        .withMethod("DELETE")
                        .withPath("/yandexservices/counter/" + COUNTER_ID + "/grants")
                        .withQueryStringParameter("user_login", USER_LOGIN)
                )
                .respond(response()
                        .withStatusCode(500)
                        .withBody("error")
                );

        Assertions.assertThatThrownBy(() -> metrikaInternalClient.deleteGrant(COUNTER_ID, USER_LOGIN))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("MetrikaInternal response Internal Server Error error: error");
    }

    private void initMock(String method, String path, String requestFile, String responseFile,
                          Map<String, List<String>> parameters) {
        mockServerPath(
                method,
                path,
                requestFile == null ? null : "json/" + requestFile + ".json",
                parameters,
                200,
                "json/" + responseFile + ".json"
        );
    }

    private CounterRoot getCounterRoot() {
        CounterRoot counterRoot = new CounterRoot();
        counterRoot.setCounter(getCounterBrief());
        return counterRoot;
    }

    private CounterRoot getCounterRootWithId() {
        CounterBrief counterBrief = getCounterBrief();
        counterBrief.setId(COUNTER_ID);

        CounterRoot counterRoot = new CounterRoot();
        counterRoot.setCounter(counterBrief);
        return counterRoot;
    }

    private CounterRoot getUpdatedCounterRootWithId() {
        CounterBrief counterBrief = getCounterBrief();
        counterBrief.setId(COUNTER_ID);
        counterBrief.setSite2(getCounterMirrorE("new_site"));

        CounterRoot counterRoot = new CounterRoot();
        counterRoot.setCounter(counterBrief);
        return counterRoot;
    }

    private CounterBrief getCounterBrief() {
        CounterBrief counterBrief = new CounterBrief();
        counterBrief.setName("test_name");
        counterBrief.setSite2(getCounterMirrorE("test_site"));
        counterBrief.setMirrors2(
                List.of(
                        getCounterMirrorE("mirror_site_1"),
                        getCounterMirrorE("mirror_site_2")
                )
        );
        counterBrief.setCodeOptions(getCodeOptionsE());
        counterBrief.setWebvisor(getWebvisorOptions());
        counterBrief.setGrants(List.of());
        counterBrief.setFilters(List.of());
        return counterBrief;
    }

    private WebvisorOptions getWebvisorOptions() {
        WebvisorOptions webvisorOptions = new WebvisorOptions();
        webvisorOptions.setArchEnabled(true);
        webvisorOptions.setArchType(WebvisorArchType.html);
        return webvisorOptions;
    }

    private CodeOptions getCodeOptionsE() {
        CodeOptions codeOptions = new CodeOptions();
        codeOptions.setEcommerce(true);
        return codeOptions;
    }

    private CounterMirror getCounterMirrorE(String counterMirrorSite) {
        CounterMirror counterMirror = new CounterMirror();
        counterMirror.setSite(counterMirrorSite);
        return counterMirror;
    }

    private GrantRoot getGrantRoot() {
        GrantRoot grantRoot = new GrantRoot();
        grantRoot.setGrant(getGrantE());
        return grantRoot;
    }

    private Grant getGrantE() {
        Grant grant = new Grant();
        grant.setUserLogin(USER_LOGIN);
        grant.setPerm(GrantType.view);
        return grant;
    }
}
