package ru.yandex.market.fulfillment.wrap.marschroute.factory;

import ru.yandex.market.fulfillment.wrap.marschroute.model.type.MarschrouteDate;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static ru.yandex.market.fulfillment.wrap.marschroute.model.type.MarschrouteTemporalType.getMarschrouteZoneOffset;

public class MarschrouteDates {

    private MarschrouteDates() {
    }

    public static List<MarschrouteDate> marschrouteDates(LocalDate... localDates) {
        return Arrays.stream(localDates)
                .map(MarschrouteDates::marschrouteDate)
                .collect(Collectors.toList());
    }

    public static MarschrouteDate marschrouteDate(LocalDate localDate) {
        OffsetDateTime offsetDateTime = localDate.atStartOfDay().atOffset(getMarschrouteZoneOffset());

        return MarschrouteDate.create(offsetDateTime);
    }
}
