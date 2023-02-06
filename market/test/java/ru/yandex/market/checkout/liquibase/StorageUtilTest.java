package ru.yandex.market.checkout.liquibase;

import java.net.URISyntaxException;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;

public class StorageUtilTest {

    @Test
    public void testPatchSingleUrl() throws URISyntaxException {
        String url = StorageUtil.patchUrl("jdbc:postgresql://pgaas.mail.yandex" +
                ".net:12000/market_checkouter_prod?ssl=true&prepareThreshold=0&preparedStatementCacheQueries=0" +
                "&connectTimeout=10&socketTimeout=50&loginTimeout=10", 0);

        assertThat(url, CoreMatchers.equalTo("jdbc:postgresql://pgaas.mail.yandex" +
                ".net:12000/market_checkouter_prod?ssl=true&prepareThreshold=0&preparedStatementCacheQueries=0" +
                "&connectTimeout=10&loginTimeout=10&socketTimeout=0"));
    }

    @Test
    public void testPatchMultipleUrl() throws URISyntaxException {
        String url = StorageUtil.patchUrl("jdbc:postgresql://market-checkouter-test01f.db.yandex.net:6432," +
                "market-checkouter-test01i.db.yandex.net:6432,market-checkouter-test01h.db.yandex" +
                ".net:6432/market_checkouter_test?targetServerType=master&ssl=true&prepareThreshold=0" +
                "&preparedStatementCacheQueries=0&socketTimeout=50&loginTimeout=10", 0);

        assertThat(url, CoreMatchers.equalTo("jdbc:postgresql://market-checkouter-test01f.db.yandex.net:6432," +
                "market-checkouter-test01i.db.yandex.net:6432,market-checkouter-test01h.db.yandex" +
                ".net:6432/market_checkouter_test?targetServerType=master&ssl=true&prepareThreshold=0" +
                "&preparedStatementCacheQueries=0&loginTimeout=10&socketTimeout=0"));
    }
}
