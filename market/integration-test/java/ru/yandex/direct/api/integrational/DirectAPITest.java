package ru.yandex.direct.api.integrational;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.direct.api.AsyncContext;
import ru.yandex.direct.api.DirectAPI;
import ru.yandex.direct.api.impl.DirectPublicApiAsyncImpl;
import ru.yandex.direct.api.impl.v4.model.Account;
import ru.yandex.direct.api.impl.v5.model.Client;

public class DirectAPITest {

    private static final Logger LOG = LoggerFactory.getLogger(DirectAPITest.class);

    private static String TOKEN;
    private static String BASE_URL;
    private static String TEST_LOGIN;
    private static final int ACCOUNT_ID = 226383283;
    private static final String PROPERTIED_FILE_NAME = "/test.properties";
    private static final Properties properties = new Properties();

    @BeforeClass
    public static void onBeforeClass() throws IOException {
        try (InputStream is = DirectAPITest.class.getResourceAsStream(PROPERTIED_FILE_NAME)) {
            properties.load(is);
            BASE_URL = properties.getProperty("direct.api.url");
            TEST_LOGIN = properties.getProperty("direct.api.login");
            TOKEN = properties.getProperty("direct.api.token");
            if (StringUtils.isBlank(TOKEN)) {
                throw new IllegalStateException("TOKEN must be present");
            }
        }
    }

    @Test
    public void testAccountManagementGet() {
        DirectAPI api = new DirectPublicApiAsyncImpl(TOKEN, BASE_URL);
        AsyncContext async = AsyncContext.context();
        async.execute(() -> api.getAccountInfo(TEST_LOGIN, event -> {
            async.assertFalse(event.failed());
            assertAccountInfo(async, event.result());
            async.complete();
        }));

        async.await();
    }

    @Test
    public void testAccountManagementUpdate() {
        DirectAPI api = new DirectPublicApiAsyncImpl(TOKEN, BASE_URL);
        AsyncContext async = AsyncContext.context();
        async.execute(() ->
                api.updateAccountDayBudget(ACCOUNT_ID, 10, event -> {
                    async.assertFalse(event.failed());
                    async.complete();
                }));

        async.await();
    }

    @Test
    public void testClientsGet() {
        DirectAPI api = new DirectPublicApiAsyncImpl(TOKEN, BASE_URL);
        AsyncContext async = AsyncContext.context();
        async.execute(() ->
                api.getClientInfo(TEST_LOGIN, event -> {
                    async.assertFalse(event.failed());
                    assertClientVat(async, event.result());
                    async.complete();
                }));

        async.await();
    }

    private void assertAccountInfo(AsyncContext context, Account result) {
        LOG.debug("Account Info {}", result);
        context.assertNotNull(result);
        context.assertEquals(ACCOUNT_ID, result.getAccountId());
        context.assertEquals("RUB", result.getCurrency());
        context.assertNotNull(result.getAmount());
        context.assertEquals(TEST_LOGIN, result.getLogin());
    }

    private void assertClientVat(AsyncContext context, Client result) {
        context.assertNotNull(result);
        context.assertNotNull(result.getVatRate());
    }
}
