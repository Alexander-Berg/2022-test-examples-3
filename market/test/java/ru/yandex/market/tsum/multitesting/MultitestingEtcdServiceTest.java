package ru.yandex.market.tsum.multitesting;

import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.tsum.clients.etc.EtcdClient;
import ru.yandex.market.tsum.pipelines.common.jobs.datasource.DataSourceProperty;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 22.05.2018
 */
@Ignore
public class MultitestingEtcdServiceTest {
    private static final String MULTITESTING_ID = "EtcdClientTest";
    private static final String MULTITESTING_PREFIX = "/multitestings/";
    private static final String MARKET_ENVIRONMENT = "testing";
    private static final String PROPERTY_TEST_1_KEY = "etcd.client.test1";
    private static final String PROPERTY_TEST_2_KEY = "etcd.client.test2";
    private static final String PROPERTY_TEST_3_KEY = "etcd.client.test3";
    private static final String PROPERTY_TEST_DEFAULT = "test";
    private static final String PROPERTY_TEST_1_VALUE = "test1";
    private static final String PROPERTY_TEST_2_VALUE = "test2";
    private static final String PROPERTY_TEST_3_VALUE = "test3";
    private static final String PROPERTY_TEST_SECTION = "tsumTest";

    // пароль есть в секретнице: https://yav.yandex-team.ru/secret/sec-01d4mwzv17f5a7ba3y3sgcm4r4/explore/versions
    private final EtcdClient client = new EtcdClient(
        "etcd.tst.vs.market.yandex.net",
        3379,
        "market-infra",
        "*****"
    );
    private final MultitestingEtcdService sut = new MultitestingEtcdService(client, MULTITESTING_PREFIX);

    @Test
    public void createMultitestingPrefix() {
        sut.deleteMultitestingPrefix(MULTITESTING_ID);
        assertEquals(0, client.getKeysPrefixedBy(MULTITESTING_PREFIX + MULTITESTING_ID + "/datasources/").size());

        sut.recreateMultitestingPrefix(MULTITESTING_ID);
        assertThat(client.getKeysPrefixedBy(MULTITESTING_PREFIX + MULTITESTING_ID + "/datasources/").entrySet(),
            hasSize(greaterThan(1000)));
    }

    @Test
    public void overrideProperty() {
        String test1prefix = MULTITESTING_PREFIX + MULTITESTING_ID + "/datasources/" + MARKET_ENVIRONMENT +
            "/yandex/market-datasources/datasources.properties/" + PROPERTY_TEST_1_KEY;
        String test2prefix = MULTITESTING_PREFIX + MULTITESTING_ID + "/datasources/" + PROPERTY_TEST_SECTION +
            "/" + MARKET_ENVIRONMENT + "/" + PROPERTY_TEST_2_KEY;
        String test3prefix = MULTITESTING_PREFIX + MULTITESTING_ID + "/datasources/" + MARKET_ENVIRONMENT +
            "/yandex/market-datasources/datasources.properties/" + PROPERTY_TEST_3_KEY;

        client.put(test1prefix, PROPERTY_TEST_DEFAULT);
        client.put(test2prefix, PROPERTY_TEST_DEFAULT);
        client.put(test3prefix, PROPERTY_TEST_DEFAULT);

        DataSourceProperty dataSourceProperty1 = new DataSourceProperty(
            DataSourceProperty.Type.JAVA,
            null,
            PROPERTY_TEST_1_KEY,
            PROPERTY_TEST_1_VALUE
        );

        DataSourceProperty dataSourceProperty2 = new DataSourceProperty(
            DataSourceProperty.Type.JAVA,
            PROPERTY_TEST_SECTION,
            PROPERTY_TEST_2_KEY,
            PROPERTY_TEST_2_VALUE
        );

        DataSourceProperty dataSourceProperty3 = new DataSourceProperty(
            DataSourceProperty.Type.JAVA,
            DataSourceProperty.COMMON_SECTION,
            PROPERTY_TEST_3_KEY,
            PROPERTY_TEST_3_VALUE
        );

        Map<String, String> keys = client.getKeysPrefixedBy(test1prefix);
        assertEquals(PROPERTY_TEST_DEFAULT, keys.get(test1prefix));

        keys = client.getKeysPrefixedBy(test2prefix);
        assertEquals(PROPERTY_TEST_DEFAULT, keys.get(test2prefix));

        keys = client.getKeysPrefixedBy(test3prefix);
        assertEquals(PROPERTY_TEST_DEFAULT, keys.get(test3prefix));

        sut.overrideProperty(MULTITESTING_ID, MARKET_ENVIRONMENT, dataSourceProperty1.getKey(),
            dataSourceProperty1.getValue());
        sut.overrideProperty(MULTITESTING_ID, MARKET_ENVIRONMENT, dataSourceProperty2.getSection(),
            dataSourceProperty2.getKey(), dataSourceProperty2.getValue());
        sut.overrideProperty(MULTITESTING_ID, MARKET_ENVIRONMENT, null,
            dataSourceProperty3.getKey(), dataSourceProperty3.getValue());

        keys = client.getKeysPrefixedBy(test1prefix);
        assertEquals(PROPERTY_TEST_1_VALUE, keys.get(test1prefix));

        keys = client.getKeysPrefixedBy(test2prefix);
        assertEquals(PROPERTY_TEST_2_VALUE, keys.get(test2prefix));

        keys = client.getKeysPrefixedBy(test3prefix);
        assertEquals(PROPERTY_TEST_3_VALUE, keys.get(test3prefix));
    }
}
