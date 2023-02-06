package ru.yandex.market.fulfillment.wrap.marschroute.model.converter;

import java.time.LocalDate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.fulfillment.wrap.marschroute.entity.AnomalyInfo;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.IntegrationTest;
import ru.yandex.market.fulfillment.wrap.marschroute.model.response.anomaly.AnomalyResponseData;
import ru.yandex.market.fulfillment.wrap.marschroute.model.type.MarschrouteDate;

class AnomalyInfoConverterTest extends IntegrationTest {

    @Autowired
    private AnomalyInfoConverter converter;

    @Test
    void checkConverter() {

        AnomalyInfo actual = converter.convert(getResponseData());
        AnomalyInfo expected = getAnomalyInfo();

        Assertions.assertEquals(expected, actual);

    }

    private AnomalyInfo getAnomalyInfo() {
        return AnomalyInfo.builder()
                .anomalyId(1)
                .taskId(1)
                .dateCreate(LocalDate.of(2021, 1, 21))
                .dateFinalStatus(LocalDate.of(2021, 1, 22))
                .status(10)
                .docId(1)
                .purpose(100)
                .build();
    }

    private AnomalyResponseData getResponseData() {
        return AnomalyResponseData.builder()
                .anomalyId(1)
                .taskId(1)
                .dateCreate(MarschrouteDate.create("21.01.2021"))
                .dateFinalStatus(MarschrouteDate.create("22.01.2021"))
                .status(10)
                .docId(1)
                .purpose(100)
                .build();
    }
}
