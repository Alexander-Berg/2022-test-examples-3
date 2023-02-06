package ru.yandex.market.notifier.jobs.zk.processors;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.mockito.ArgumentCaptor;

import ru.yandex.market.notifier.application.AbstractWebTestBase;
import ru.yandex.market.orderservice.client.model.SummaryCompensatedDataDto;
import ru.yandex.market.orderservice.client.model.SummaryCompensatedOrdersDto;
import ru.yandex.market.orderservice.client.model.SummaryCompensatedOrdersResponse;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class WeeklyCompensatedOrdersProcessorsCommonTest  extends AbstractWebTestBase {

    protected SummaryCompensatedOrdersResponse singleResponse() {
        return new SummaryCompensatedOrdersResponse()
                .result(List.of(
                        new SummaryCompensatedOrdersDto()
                                .partnerId(3391L)
                                .orders(List.of(
                                        new SummaryCompensatedDataDto()
                                                .orderId(1321L)
                                                .orderAmount(3929100L)
                                ))
                ));
    }

    protected SummaryCompensatedOrdersResponse doubleResponse() {
        return new SummaryCompensatedOrdersResponse()
                .result(List.of(
                        new SummaryCompensatedOrdersDto()
                                .partnerId(3391L)
                                .orders(List.of(
                                        new SummaryCompensatedDataDto()
                                                .orderId(1321L)
                                                .orderAmount(3929100L),

                                        new SummaryCompensatedDataDto()
                                                .orderId(13251L)
                                                .orderAmount(131345L)
                                ))
                ));
    }

    protected void checkValue(ArgumentCaptor<String> dataCaptor, String fileName) throws IOException {
        assertThat(dataCaptor.getValue().trim()).isEqualTo(getFile(fileName));
    }

    private String getFile(String fileName) throws IOException {
        return IOUtils.toString(getClass()
                        .getResourceAsStream(fileName),
                StandardCharsets.UTF_8).trim();
    }
}
