package ru.yandex.market.business.metrika;

import java.util.List;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.passport.PassportService;
import ru.yandex.market.core.passport.model.UserInfo;
import ru.yandex.market.shop.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;

public class UpdateBusinessMetrikaCounterExecutorTest extends FunctionalTest {

    @Autowired
    UpdateBusinessMetrikaCounterExecutor updateBusinessMetrikaCounterExecutor;
    @Autowired
    MockWebServer metrikaIntApiMockWebServer;
    @Autowired
    PassportService passportService;

    @Test
    @DbUnitDataSet(before = "UpdateBusinessMetrikaCounterExecutorTest.before.csv",
            after = "UpdateBusinessMetrikaCounterExecutorTest.after.csv")
    void testSingle() {
        metrikaIntApiMockWebServer.enqueue(new MockResponse().setBody("{\"counter\":{\"id\":101}}"));
        updateBusinessMetrikaCounterExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(before = "UpdateBusinessMetrikaCounterExecutorTest.mailTail.yaru.before.csv",
            after = "UpdateBusinessMetrikaCounterExecutorTest.after.csv")
    void testMailTailYaRu() throws InterruptedException {
        Mockito.when(passportService.getUsersByEmail(Mockito.eq("yandexdomain@ya.ru"), Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(List.of(new UserInfo(3, "", "", "yandexdomain")));
        metrikaIntApiMockWebServer.enqueue(new MockResponse().setBody("{\"counter\":{\"id\":101}}"));
        updateBusinessMetrikaCounterExecutor.doJob(null);
        JsonTestUtil.compareJson(metrikaIntApiMockWebServer.takeRequest().getBody().readUtf8(),
                this.getClass(), "json/counter_yandexdomain.json");
    }

    @Test
    @DbUnitDataSet(before = "UpdateBusinessMetrikaCounterExecutorTest.mailTail.yandexby.before.csv",
            after = "UpdateBusinessMetrikaCounterExecutorTest.after.csv")
    void testMailTailYandexBy() throws InterruptedException {
        Mockito.when(passportService.getUsersByEmail(Mockito.eq("yandexdomain@yandex.by"), Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(List.of(new UserInfo(3, "", "", "yandexdomain")));
        metrikaIntApiMockWebServer.enqueue(new MockResponse().setBody("{\"counter\":{\"id\":101}}"));
        updateBusinessMetrikaCounterExecutor.doJob(null);
        JsonTestUtil.compareJson(metrikaIntApiMockWebServer.takeRequest().getBody().readUtf8(),
                this.getClass(), "json/counter_yandexdomain.json");
    }

    @Test
    @DbUnitDataSet(before = "UpdateBusinessMetrikaCounterExecutorTest.mailTail.anotherdomain.before.csv",
            after = "UpdateBusinessMetrikaCounterExecutorTest.after.csv")
    void testMailTailAnotherDomain() throws InterruptedException {
        metrikaIntApiMockWebServer.enqueue(new MockResponse().setBody("{\"counter\":{\"id\":101}}"));
        updateBusinessMetrikaCounterExecutor.doJob(null);
        JsonTestUtil.compareJson(metrikaIntApiMockWebServer.takeRequest().getBody().readUtf8(),
                this.getClass(), "json/counter_anotherdomain.json");
    }

    @Test
    @DbUnitDataSet(before = "UpdateBusinessMetrikaCounterExecutorTest.loginIgnoreCase.before.csv",
            after = "UpdateBusinessMetrikaCounterExecutorTest.after.csv")
    void testLoginIgnoreCase() throws InterruptedException {
        metrikaIntApiMockWebServer.enqueue(new MockResponse().setBody("{\"counter\":{\"id\":101}}"));
        updateBusinessMetrikaCounterExecutor.doJob(null);
        JsonTestUtil.compareJson(metrikaIntApiMockWebServer.takeRequest().getBody().readUtf8(),
                this.getClass(), "json/counter_login_ignore_case.json");
    }

    @Test
    @DbUnitDataSet(before = "UpdateBusinessMetrikaCounterExecutorTest.counters.before.csv",
            after = "UpdateBusinessMetrikaCounterExecutorTest.counters.testBatchWithBadRequests.after.csv")
    void testBatchWithBadRequests() {
        metrikaIntApiMockWebServer.enqueue(
                new MockResponse().setBody(
                        "{\"errors\":[{" +
                                "\"error_type\":\"invalid_parameter\"," +
                                "\"location\":\"User with login mashulechcka not found\"}]," +
                                "\"message\":\"Такой пользователь не существует.\"" +
                                "}")
                        .setResponseCode(400)
        );
        metrikaIntApiMockWebServer.enqueue(new MockResponse().setBody("{\"errors\":10}").setResponseCode(400));
        metrikaIntApiMockWebServer.enqueue(new MockResponse().setBody("{\"errors\":111}").setResponseCode(400));
        metrikaIntApiMockWebServer.enqueue(new MockResponse().setResponseCode(400));
        try {
            updateBusinessMetrikaCounterExecutor.doJob(null);
        } catch (RuntimeException e) {
            Assertions.assertEquals(3, e.getSuppressed().length);
        }
    }

    @Test
    @DbUnitDataSet(before = "UpdateBusinessMetrikaCounterExecutorTest.counters.before.csv",
            after = "UpdateBusinessMetrikaCounterExecutorTest.counters.after.csv")
    void testCountersInBatch() {
        metrikaIntApiMockWebServer.enqueue(new MockResponse().setBody("{\"counter\":{\"id\":10}}"));
        metrikaIntApiMockWebServer.enqueue(new MockResponse().setBody("{\"counter\":{\"id\":20}}"));
        metrikaIntApiMockWebServer.enqueue(new MockResponse().setBody("{\"counter\":{\"id\":30}}"));
        metrikaIntApiMockWebServer.enqueue(new MockResponse().setBody("{\"counter\":{\"id\":40}}"));
        updateBusinessMetrikaCounterExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(before = "UpdateBusinessMetrikaCounterExecutorTest.additionalLogins.before.csv",
            after = "UpdateBusinessMetrikaCounterExecutorTest.after.csv")
    void testAdditionalLogins() throws InterruptedException {
        Mockito.when(passportService.getUsersByLogins(Mockito.anyList()))
                .thenReturn(List.of(
                        new UserInfo(3, "", "", "additional_login_1"),
                        new UserInfo(4, "", "", "additional_login_2")
                ));
        metrikaIntApiMockWebServer.enqueue(new MockResponse().setBody("{\"counter\":{\"id\":101}}"));

        updateBusinessMetrikaCounterExecutor.doJob(null);

        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(passportService).getUsersByLogins(captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder("additional_login_1", "additional_login_2");
        JSONAssert.assertEquals(
                metrikaIntApiMockWebServer.takeRequest().getBody().readUtf8(),
                StringTestUtil.getString(this.getClass(), "json/counter_additional_logins.json"),
                JSONCompareMode.NON_EXTENSIBLE);
    }
}
