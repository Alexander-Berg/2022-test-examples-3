package ru.yandex.market.common.mds.s3.client.service.factory;

import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;

import ru.yandex.market.common.mds.s3.client.test.TestUtils;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link HttpClientFactory}.
 *
 * @author Vladislav Bauer
 */
public class HttpClientFactoryTest {

    @Test
    public void testConstructorContract() {
        TestUtils.checkConstructor(HttpClientFactory.class);
    }

    @Test
    public void testCreate() {
        final CloseableHttpClient httpClient = HttpClientFactory.create();
        assertThat(httpClient, notNullValue());
    }

}
