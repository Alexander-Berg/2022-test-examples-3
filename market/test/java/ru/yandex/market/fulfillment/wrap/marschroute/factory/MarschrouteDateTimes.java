package ru.yandex.market.fulfillment.wrap.marschroute.factory;

import ru.yandex.market.fulfillment.wrap.marschroute.model.type.MarschrouteDateTime;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class MarschrouteDateTimes {

    public static MarschrouteDateTime marschrouteDateTime(LocalDate localDate) {
        return marschrouteDateTime(localDate.atStartOfDay());
    }

    public static MarschrouteDateTime marschrouteDateTime(LocalDateTime localDateTime) {
        return MarschrouteDateTime.create(localDateTime);
    }
}
