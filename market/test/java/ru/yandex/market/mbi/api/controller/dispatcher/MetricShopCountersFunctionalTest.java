package ru.yandex.market.mbi.api.controller.dispatcher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.api.config.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Функциональные тесты ручки /metric-shop-counters.
 *
 * @author Vadim Lyalin
 */
public class MetricShopCountersFunctionalTest extends FunctionalTest {
    private static final String PAGEMATCH_URL_TEMPLATE = "http://localhost:{0}/metric-shop-counters";

    /**
     * Тест для пустого ответа, когда нет данных.
     */
    @Test
    public void testMetricShopCountersWithEmptyResponse() throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();

        HttpGet getMethod = new HttpGet(MessageFormat.format(PAGEMATCH_URL_TEMPLATE, Integer.toString(port)));

        CloseableHttpResponse response = httpClient.execute(getMethod);
        String responseBody = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
        assertEquals("<paged-result><shops/></paged-result>",
                responseBody);
    }

    /**
     * Тест для получения всех данных по всем магазинам.
     */
    @Test
    @DbUnitDataSet(before = "MetricShopCountersFunctionalTest.before.csv")
    public void testMetricShopCountersWithoutPager() throws IOException {
        var expectedXml =
                "<paged-result><shops>" +
                        "<shop shopId=\"2\"><counters>1</counters><counters>2</counters></shop>" +
                        "<shop shopId=\"1\"><counters>1</counters></shop>" +
                        "</shops></paged-result>";

        CloseableHttpClient httpClient = HttpClients.createDefault();

        HttpGet getMethod = new HttpGet(MessageFormat.format(PAGEMATCH_URL_TEMPLATE, Integer.toString(port)));

        CloseableHttpResponse response = httpClient.execute(getMethod);
        String responseBody = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
        assertThat(expectedXml).isEqualTo(responseBody);
    }

    /**
     * Тест с пейджером.
     */
    @Test
    @DbUnitDataSet(before = "MetricShopCountersFunctionalTest.before.csv")
    public void testMetricShopCountersWithPager() throws IOException {
        var expectedResponse =
                "<paged-result>" +
                        "<pager currentPage=\"1\" from=\"1\" pageSize=\"10\" pagesCount=\"1\" to=\"10\" total=\"2\"/>" +
                        "<shops>" +
                        "<shop shopId=\"2\"><counters>1</counters><counters>2</counters></shop>" +
                        "<shop shopId=\"1\"><counters>1</counters></shop>" +
                        "</shops></paged-result>";

        CloseableHttpClient httpClient = HttpClients.createDefault();

        HttpGet getMethod = new HttpGet(MessageFormat.format(PAGEMATCH_URL_TEMPLATE + "?page=1&page_size=10" +
                "&show_pager=true", Integer.toString(port)));

        CloseableHttpResponse response = httpClient.execute(getMethod);
        String responseBody = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
        assertThat(expectedResponse).isEqualTo(responseBody);
    }
}
