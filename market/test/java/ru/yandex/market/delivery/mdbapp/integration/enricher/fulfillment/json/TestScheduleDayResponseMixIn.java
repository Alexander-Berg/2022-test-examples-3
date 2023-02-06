package ru.yandex.market.delivery.mdbapp.integration.enricher.fulfillment.json;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;

public class TestScheduleDayResponseMixIn extends ScheduleDayResponse {

    @JsonCreator
    public TestScheduleDayResponseMixIn(@JsonProperty("id") Long id,
                                        @JsonProperty("day") Integer day,
                                        @JsonProperty("timeFrom") LocalTime timeFrom,
                                        @JsonProperty("timeTo") LocalTime timeTo,
                                        @JsonProperty("isMain") Boolean isMain) {
        super(id, day, timeFrom, timeTo, isMain);
    }
}
