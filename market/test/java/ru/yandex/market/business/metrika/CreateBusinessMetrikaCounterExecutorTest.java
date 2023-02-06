package ru.yandex.market.business.metrika;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.business.metrika.api.MetrikaIntApi;
import ru.yandex.market.core.passport.PassportService;
import ru.yandex.market.core.passport.model.UserInfo;
import ru.yandex.market.shop.FunctionalTest;

public class CreateBusinessMetrikaCounterExecutorTest extends FunctionalTest {

    @Autowired
    CreateBusinessMetrikaCounterExecutor createBusinessMetrikaCounterExecutor;
    @Autowired
    MetrikaIntApi metrikaIntApi;
    @Autowired
    MockWebServer metrikaIntApiMockWebServer;
    @Autowired
    PassportService passportService;

    @BeforeEach
    void beforeEach() {
        Mockito.when(passportService.getUserInfo(Mockito.eq("cherdakov")))
                .thenReturn(new UserInfo(1, "", "", "cherdakov"));
        Mockito.when(passportService.getUserInfo(Mockito.eq("kirill")))
                .thenReturn(new UserInfo(2, "", "", "kirill"));
    }

    @Test
    @DbUnitDataSet(before = "CreateBusinessMetrikaCounterExecutorTest.before.csv",
            after = "CreateBusinessMetrikaCounterExecutorTest.after.csv")
    void test() throws InterruptedException {
        metrikaIntApiMockWebServer.enqueue(new MockResponse().setBody("{\"counter\":{\"id\":778}}"));
        metrikaIntApiMockWebServer.enqueue(new MockResponse().setBody("{\"counter\":{\"id\":777}}"));
        createBusinessMetrikaCounterExecutor.doJob(null);
        JsonTestUtil.compareJson(metrikaIntApiMockWebServer.takeRequest().getBody().readUtf8(),
                this.getClass(), "json/counter10.json");
        JsonTestUtil.compareJson(metrikaIntApiMockWebServer.takeRequest().getBody().readUtf8(),
                this.getClass(), "json/counter11.json");
    }
}
