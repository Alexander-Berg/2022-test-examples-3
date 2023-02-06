package ru.yandex.market.sc.core.test;

import ru.yandex.market.sc.core.domain.order.repository.ScOrder;
import ru.yandex.market.sc.core.domain.place.repository.Place;

/**
 * @author valter
 */
public record ScOrderWithPlace(ScOrder order, Place place) {

}
