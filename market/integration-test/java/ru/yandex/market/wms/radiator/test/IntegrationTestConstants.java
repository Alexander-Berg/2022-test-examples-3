package ru.yandex.market.wms.radiator.test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import ru.yandex.market.logistic.api.utils.DateTime;

public interface IntegrationTestConstants {
    String WH_1_KEY = "wh1";
    String WH_1_ID = "101";
    String WH_1_TOKEN = "wh1_token";

    String WH_2_KEY = "wh2";
    String WH_2_ID = "102";
    String WH_2_TOKEN = "wh2_token";

    LocalDateTime LOCAL_DATE_TIME = LocalDateTime.parse("2020-07-01T10:00:00");
    DateTime DATE_TIME = DateTime.fromOffsetDateTime(LOCAL_DATE_TIME.atOffset(ZoneOffset.UTC));
}
