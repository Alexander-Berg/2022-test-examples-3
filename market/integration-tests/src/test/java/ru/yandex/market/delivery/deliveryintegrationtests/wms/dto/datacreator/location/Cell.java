package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location;


import lombok.Getter;

public abstract class Cell {
    @Getter
    String prefix;
    @Getter
    LocationType locationType;
}
