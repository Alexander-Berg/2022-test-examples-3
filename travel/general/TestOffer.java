package ru.yandex.travel.api.models.hotels;

import lombok.Data;
import lombok.NoArgsConstructor;

import ru.yandex.travel.hotels.proto.EPansionType;

@Data
@NoArgsConstructor
public class TestOffer {
    private String offerName;
    private EPansionType pansionType;
    private boolean hasFreeCancellation;
    private int price;
    private String token;
    private String bookingPageUrl;
}
