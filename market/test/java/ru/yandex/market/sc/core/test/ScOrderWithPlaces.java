package ru.yandex.market.sc.core.test;

import java.util.List;

import ru.yandex.market.sc.core.domain.order.repository.ScOrder;
import ru.yandex.market.sc.core.domain.place.repository.Place;

/**
 * @author valter
 */
public record ScOrderWithPlaces(ScOrder order, List<Place> place) {

    public Place place(String placeBarcode) {
        return place.stream().filter(p -> placeBarcode.equals(p.getMainPartnerCode())).findFirst().orElseThrow();
    }

}
