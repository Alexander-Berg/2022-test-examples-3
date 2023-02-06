package ru.yandex.autotests.market.partner.api;

import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.autotests.market.partner.beans.PartnerApiRequestData;
import ru.yandex.autotests.market.partner.api.data.b2b.StatisticsRequestData;

import java.net.URI;

import static com.jayway.restassured.RestAssured.with;

/**
 * User: jkt
 * Date: 15.05.13
 * Time: 11:35
 */
public class StorageUtilsTest {

    private static final Logger LOG = Logger.getLogger(StorageUtilsTest.class);

    @Ignore
    @Test
    public void testNameGeneration() throws Exception  {
        with().body("ololo").post("http://jkt-nb-w7.ld.yandex.ru:4322/cart");
        PartnerApiRequestData requestData = StatisticsRequestData.monthlyStatisticsRequest();
        String urlString = requestData.asLink();
        URI url = new URI(urlString);
        url.getHost();
        LOG.debug(url.getHost());
        LOG.debug(requestData.asLink());
    }
}
