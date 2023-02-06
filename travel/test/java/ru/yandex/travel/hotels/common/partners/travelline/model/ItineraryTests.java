package ru.yandex.travel.hotels.common.partners.travelline.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import org.javamoney.moneta.Money;
import org.junit.Test;

import ru.yandex.travel.commons.proto.ProtoUtils;
import ru.yandex.travel.hotels.common.orders.Guest;
import ru.yandex.travel.hotels.common.LocationType;
import ru.yandex.travel.hotels.common.orders.OrderDetails;
import ru.yandex.travel.hotels.common.orders.TravellineHotelItinerary;
import ru.yandex.travel.hotels.common.partners.travelline.model.dto.HotelDto;
import ru.yandex.travel.hotels.common.partners.travelline.model.dto.OfferDto;
import ru.yandex.travel.hotels.common.partners.travelline.placements.PlacementSet;
import ru.yandex.travel.hotels.common.refunds.RefundRules;

public class ItineraryTests {
    @Test
    public void serializeTravellineItinerary() {
        Guest g1 = new Guest();
        Guest g2 = new Guest();
        g1.setFirstName("Alexander");
        g1.setLastName("Tivelkov");
        g2.setFirstName("John");
        g2.setLastName("Doe");

        var itinerary = new TravellineHotelItinerary();
        itinerary.setCustomerEmail("tivelkov@yandex-team.ru");
        itinerary.setCustomerPhone("+7-926-267-37-97");
        itinerary.setCustomerIp("127.0.0.1");
        itinerary.setCustomerUserAgent("ManualTest");
        itinerary.setAllowsSubscription(false);
        itinerary.setExpiresAtInstant(Instant.now().plus(365 * 10, ChronoUnit.DAYS));
        itinerary.setGuests(List.of(g1, g2));
        itinerary.setFiscalPrice(Money.of(19000, "RUB"));
        itinerary.setRefundRules(RefundRules.builder().build());
        itinerary.setOrderDetails(OrderDetails.builder()
                .permalink(0L)
                .roomName("Полулюкс")
                .hotelName("Yandex Санкт-Петербург")
                .originalId("5595")
                .checkinDate(LocalDate.of(2019, 10, 12))
                .checkoutDate(LocalDate.of(2019, 10, 14))
                .checkinBegin("14:00")
                .checkoutEnd("12:00")
                .locationType(LocationType.MOSCOW)
                .build());


        OfferDto offerDTO = OfferDto.builder()
                .hotel(HotelDto.builder()
                        .code("5595")
                        .build())
                .roomType(RoomType.builder()
                        .code("317333")
                        .build())
                .possiblePlacements(List.of(
                        PlacementSet.builder()
                                .placements(List.of(Placement.builder()
                                        .code("832321")
                                        .index(0)
                                        .kind(GuestPlacementKind.ADULT)
                                        .capacity(1)
                                        .build()))
                                .refundRules(RefundRules.builder().build())
                                .guestCountInfo(GuestCountInfo.builder().guestCount(
                                        GuestCount.builder()
                                                .placementIndex(0)
                                                .ageQualifyingCode("adult")
                                                .count(1)
                                                .build())
                                        .build())
                                .guestPlacementIndexes(List.of(0))
                                .build()))
                .ratePlan(RatePlan.builder().code("247286").build())
                .services(Collections.emptyMap())
                .build();
        itinerary.setOffer(offerDTO);
        var json = ProtoUtils.toTJson(itinerary);
        System.out.println(json.getValue());
    }
}
