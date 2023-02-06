package ru.yandex.market.billing.tasks.distribution;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Supplier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;

import org.springframework.retry.support.RetryTemplate;
import ru.yandex.market.logistics.util.client.ExternalServiceProperties;
import ru.yandex.market.logistics.util.client.HttpTemplateBuilder;
import ru.yandex.market.mbi.util.MbiAsserts;
import ru.yandex.market.mbi.web.converter.JsonHttpMessageConverter;
import ru.yandex.market.request.trace.Module;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DistributionClientTest {

    @Test
    public void testSerialization() throws JsonProcessingException {
        var request =
                new DistributionClient.DistributionRequest.Builder()
                        .withFilters(List.of("AND", List.of(List.of("tag_id", "=", String.valueOf(123L)))))
                        .withDimensions(List.of(DistributionClient.Field.CLID, DistributionClient.Field.CLID_TYPE_ID))
                        .build();
        ObjectMapper objectMapper = new ObjectMapper();
        byte[] jsonBytes = objectMapper.writeValueAsBytes(request);
        String expected = "{\"dimensions\":[\"clid\",\"clid_type_id\"],\"filters\":[\"AND\",[[\"tag_id\",\"=\",\"123\"]]]}";
        MbiAsserts.assertJsonEquals(expected, new String(jsonBytes, StandardCharsets.UTF_8));
    }

    @Test
    public void testDeserialization() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        DistributionClient.Response response =
                objectMapper.readerFor(DistributionClient.Response.class)
                        .readValue("{\"data\":" +
                                "{\"clids\":[" +
                                "[{\"name\":\"clid\",\"value\":2042899},{\"name\":\"clid_type_id\",\"value\":26}]," +
                                "[{\"name\":\"clid\",\"value\":2073130},{\"name\":\"clid_type_id\",\"value\":27}]," +
                                "[{\"name\":\"clid\",\"value\":2073131},{\"name\":\"clid_type_id\",\"value\":26}]," +
                                "[{\"name\":\"clid\",\"value\":2073132},{\"name\":\"clid_type_id\",\"value\":26}]," +
                                "[{\"name\":\"clid\",\"value\":2073133},{\"name\":\"clid_type_id\",\"value\":27}]]," +
                                "\"packs\":[" +
                                "[{\"name\":\"user_login\",\"value\":\"abbbb\"}]," +
                                "[{\"name\":\"user_login\",\"value\":\"bbbba\"}]]," +
                                "\"sets\":[" +
                                "[{\"name\":\"soft_id\",\"value\":1033}]," +
                                "[{\"name\":\"soft_id\",\"value\":1046}]]}," +
                                "\"result\":\"ok\"}");
        assertEquals(5, response.getData().getClids().size());
        assertEquals(2, response.getData().getClids().get(0).size());
        assertEquals(2, response.getData().getPacks().size());
        assertEquals(2, response.getData().getSets().size());
        assertEquals("ok", response.getResult());
    }

    @Ignore("Можно запустить локально, попробовав настоящее подключение")
//    @Test
    public void testRealConnection() {
        Supplier<String> tvmTicketProvider = () -> "xxx"; //your ticket here

        var props = new ExternalServiceProperties();
        props.setUrl("https://distribution.yandex.net/api/v2/products/clids/report/");
        props.setTvmServiceId(2001558);

        var httpTemplate = HttpTemplateBuilder.create(props, Module.DISTRIBUTION_REPORT)
                .withTicketProvider(tvmServiceId -> tvmTicketProvider.get())
                .withConverters(List.of(new JsonHttpMessageConverter()))
                .build();

        new DistributionClient(httpTemplate, new RetryTemplate())
                .getData(List.of(DistributionClient.Field.CLID),
                        List.of("AND", List.of(List.of("tag_id", "=", String.valueOf(123L)))));

    }
}
