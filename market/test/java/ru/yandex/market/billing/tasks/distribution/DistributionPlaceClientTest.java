package ru.yandex.market.billing.tasks.distribution;

import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.springframework.retry.support.RetryTemplate;

import ru.yandex.market.billing.distribution.share.model.DistributionPartner;
import ru.yandex.market.billing.distribution.share.model.DistributionPartnerSegment;
import ru.yandex.market.logistics.util.client.ExternalServiceProperties;
import ru.yandex.market.logistics.util.client.HttpTemplateBuilder;
import ru.yandex.market.mbi.web.converter.JsonHttpMessageConverter;
import ru.yandex.market.request.trace.Module;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

public class DistributionPlaceClientTest {

    @Test
    public void testGetClids() throws Exception {
        DistributionPlaceClient.Response response = new ObjectMapper().readerFor(DistributionPlaceClient.Response.class)
                .readValue(this.getClass().getResource("distributionPlaceResponse.json"));
        var result = DistributionPlaceClient.getClids(response);
        assertThat(result, containsInAnyOrder(
                DistributionPartner.builder()
                        .setClid(101)
                        .setPartnerSegment(DistributionPartnerSegment.CLOSER)
                        .setPlaceType("Не определено")
                        .setName("Группа в социальной сети \"Вконтакте\" с товар")
                        .setUrl("Группа в социальной сети \"Вконтакте\" с товар")
                        .setUniqueVisitors("не определено")
                        .setStatus(3)
                        .build(),
                DistributionPartner.builder()
                        .setClid(102)
                        .setPartnerSegment(DistributionPartnerSegment.CLOSER)
                        .setPlaceType("Не определено")
                        .setName("Группа в социальной сети \"Вконтакте\" с товар")
                        .setUrl("Группа в социальной сети \"Вконтакте\" с товар")
                        .setUniqueVisitors("не определено")
                        .setStatus(3)
                        .build(),
                DistributionPartner.builder()
                        .setClid(103)
                        .setPartnerSegment(DistributionPartnerSegment.CLOSER)
                        .setPlaceType("Не определено")
                        .setName("Группа в социальной сети \"Вконтакте\" с товар")
                        .setUrl("Группа в социальной сети \"Вконтакте\" с товар")
                        .setUniqueVisitors("не определено")
                        .setStatus(3)
                        .build(),
                DistributionPartner.builder()
                        .setClid(200)
                        .setPlaceType("Instagram блог")
                        .setName("1bluetooth.ru")
                        .setUrl("1bluetooth.ru")
                        .setUniqueVisitors("не определено")
                        .setStatus(3)
                        .build()
        ));
    }

    @Ignore("Можно запустить локально, попробовав настоящее подключение. Ручка отвечает около минуты!")
 //   @Test
    public void testRealConnection() {
        Supplier<String> tvmTicketProvider = () -> "xxx"; //your ticket here

        var props = new ExternalServiceProperties();
        props.setUrl("https://distribution.yandex.net/api/v2/places/");
        props.setTvmServiceId(2001558);
        props.setReadTimeout(600000);

        var httpTemplate = HttpTemplateBuilder.create(props, Module.DISTRIBUTION_REPORT)
                .withTicketProvider(tvmServiceId -> tvmTicketProvider.get())
                .withConverters(List.of(new JsonHttpMessageConverter()))
                .build();

        new DistributionPlaceClient(httpTemplate, new RetryTemplate()).getAllClids();
    }
}
