package ru.yandex.travel.api.services.hotels_booking_flow;

import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.javamoney.moneta.Money;

import ru.yandex.travel.api.proto.test.Test;
import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.hotels.common.orders.HotelItinerary;
import ru.yandex.travel.hotels.common.refunds.RefundRule;
import ru.yandex.travel.hotels.common.refunds.RefundRules;
import ru.yandex.travel.hotels.common.refunds.RefundType;
import ru.yandex.travel.hotels.models.booking_flow.Amenity;
import ru.yandex.travel.hotels.models.booking_flow.BedGroupInfo;
import ru.yandex.travel.hotels.models.booking_flow.Coordinates;
import ru.yandex.travel.hotels.models.booking_flow.Image;
import ru.yandex.travel.hotels.models.booking_flow.LegalInfo;
import ru.yandex.travel.hotels.models.booking_flow.LocalizedPansionInfo;
import ru.yandex.travel.hotels.models.booking_flow.PartnerHotelInfo;
import ru.yandex.travel.hotels.models.booking_flow.Rate;
import ru.yandex.travel.hotels.models.booking_flow.RateInfo;
import ru.yandex.travel.hotels.models.booking_flow.RateStatus;
import ru.yandex.travel.hotels.models.booking_flow.RoomInfo;
import ru.yandex.travel.hotels.models.booking_flow.StayInfo;
import ru.yandex.travel.hotels.proto.EPansionType;
import ru.yandex.travel.hotels.proto.EPartnerId;
import ru.yandex.travel.hotels.proto.TOfferData;
import ru.yandex.travel.orders.commons.proto.EServiceType;


public class TestProvider extends AbstractPartnerBookingProvider<Test.TTestOffer> {
    final static String TEST_CONTENT_NOT_FOUND = "0";
    final static String TEST_PRICE_NOT_FOUND = "1";
    final static String TEST_PRICE_MISMATCH = "2";
    final static String TEST_SOLD_OUT = "3";


    @Override
    public EServiceType getServiceType() {
        return EServiceType.PT_UNKNOWN;
    }

    @Override
    public EPartnerId getPartnerId() {
        return EPartnerId.PI_EXPEDIA;
    }

    @Override
    Test.TTestOffer getPartnerOffer(TOfferData offerData) {
        if (offerData == null) {
            return null;
        } else {
            return Test.TTestOffer.newBuilder().build();
        }
    }

    @Override
    PartnerFutures getPartnerFutures(BookingFlowContext context, CompletableFuture<Test.TTestOffer> offerData) {
        String id = context.getDecodedToken().getOfferId();
        return new PartnerFutures() {
            @Override
            public CompletableFuture<PartnerHotelInfo> getPartnerHotelInfo() {
                return offerData.thenCompose(o -> {
                    if (o == null) {
                        return CompletableFuture.failedFuture(new RuntimeException("No offer data"));
                    }
                    if (TEST_CONTENT_NOT_FOUND.equals(id)) {
                        return CompletableFuture.failedFuture(new RuntimeException("No partner hotel content"));
                    }
                    var res = PartnerHotelInfo.builder()
                            .name("Отель \"У Погибшего Альпиниста\"")
                            .address("Россия, Москва, улица Кукуевская, 13к4")
                            .coordinates(new Coordinates(11.22, 33.44))
                            .stars(4)
                            .amenity(new Amenity("1", "Parking"))
                            .amenity(new Amenity("2", "Swimming pool"))
                            .description(new PartnerHotelInfo.TextBlock("Расположение", "Отель расположен в черте " +
                                    "города"))
                            .description(new PartnerHotelInfo.TextBlock("Питание",
                                    "В отеле два ресторана, рядом есть макдональдс и KFC"))
                            .image(new Image(null, null,
                                    "https://i.travelapi.com/hotels/1000000/30000/26200/26199/c447411f_b.jpg"))
                            .image(new Image(null, null,
                                    "https://i.travelapi.com/hotels/1000000/30000/26200/26199/4215a8ae_b.jpg"))
                            .build();
                    return CompletableFuture.completedFuture(res);
                });
            }

            @Override
            public CompletableFuture<RateInfo> getRateInfo() {
                return offerData.thenCompose(o -> {
                    if (o == null) {
                        return CompletableFuture.failedFuture(new RuntimeException("No offer data"));
                    }
                    if (TEST_PRICE_NOT_FOUND.equals(id)) {
                        return CompletableFuture.failedFuture(new RuntimeException("No partner price content"));
                    }
                    if (TEST_SOLD_OUT.equals(id)) {
                        var res = RateInfo.builder()
                                .status(RateStatus.SOLD_OUT)
                                .build();
                        return CompletableFuture.completedFuture(res);
                    }
                    var res = RateInfo.builder()
                            .baseRate(new Rate("100.00", "RUB"))
                            .baseRateBreakdown(
                                    List.of(new Rate("60.00", "RUB"),
                                            new Rate("40.00", "RUB")))
                            .taxesAndFees(new Rate("19.00", "RUB"))
                            .totalRate(new Rate("110.00", "RUB"))
                            .extraFees(null)
                            .status(TEST_PRICE_MISMATCH.equals(id) ? RateStatus.PRICE_MISMATCH : RateStatus.CONFIRMED)
                            .build();
                    return CompletableFuture.completedFuture(res);
                });
            }

            @Override
            public CompletableFuture<RoomInfo> getRoomInfo() {
                return offerData.thenCompose(o -> {
                    if (o == null) {
                        return CompletableFuture.failedFuture(new RuntimeException("No offer data"));
                    }
                    if (TEST_CONTENT_NOT_FOUND.equals(id)) {
                        return CompletableFuture.failedFuture(new RuntimeException("No partner hotel content"));
                    }
                    var res = RoomInfo.builder()
                            .name("Стандратный двухместный номер")
                            .description("Просторный номер для двоих с холодильником и феном")
                            .roomAmenities(List.of(
                                    new Amenity("1", "Холодильник"),
                                    new Amenity("2", "Фен")))
                            .image(new Image(null, null,
                                    "https://i.travelapi.com/hotels/1000000/30000/26200/26199/025f139e_b.jpg"))
                            .bedGroups(List.of(
                                    new BedGroupInfo(0, "Двуспальная кровать"),
                                    new BedGroupInfo(1, "Две односпальных кровати")
                            ))
                            .pansionInfo(new LocalizedPansionInfo(EPansionType.PT_BB, "Завтрак"))
                            .build();
                    return CompletableFuture.completedFuture(res);
                });
            }

            @Override
            public CompletableFuture<StayInfo> getStayInfo() {
                return offerData.thenCompose(o -> {
                    if (o == null) {
                        return CompletableFuture.failedFuture(new RuntimeException("No offer data"));
                    }
                    if (TEST_CONTENT_NOT_FOUND.equals(id)) {
                        return CompletableFuture.failedFuture(new RuntimeException("No partner hotel content"));
                    }
                    var res = StayInfo.builder()
                            .checkInStartTime("14:00")
                            .checkInEndTime("11:00")
                            .stayInstruction("Проживание с животными запрещено")
                            .stayInstruction("Дети до пяти лет проживают бесплатно")
                            .build();
                    return CompletableFuture.completedFuture(res);
                });
            }

            @Override
            public CompletableFuture<RefundRules> getRefundRules() {
                return offerData.thenCompose(o -> {
                    if (o == null) {
                        return CompletableFuture.failedFuture(new RuntimeException("No offer data"));
                    }
                    if (TEST_PRICE_NOT_FOUND.equals(id)) {
                        return CompletableFuture.failedFuture(new RuntimeException("No partner price content"));
                    }
                    var res = RefundRules.builder()
                            .rule(RefundRule.builder()
                                    .endsAt(context.getDecodedToken().getCheckInDate().minusDays(2).atStartOfDay().toInstant(ZoneOffset.UTC))
                                    .type(RefundType.FULLY_REFUNDABLE)
                                    .build())
                            .rule(RefundRule.builder()
                                    .startsAt(context.getDecodedToken().getCheckInDate().minusDays(2).atStartOfDay().toInstant(ZoneOffset.UTC))
                                    .endsAt(context.getDecodedToken().getCheckInDate().minusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC))
                                    .type(RefundType.REFUNDABLE_WITH_PENALTY)
                                    .penalty(Money.of(30, ProtoCurrencyUnit.RUB))
                                    .build())
                            .rule(RefundRule.builder()
                                    .startsAt(context.getDecodedToken().getCheckInDate().minusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC))
                                    .type(RefundType.NON_REFUNDABLE)
                                    .build())
                            .build();
                    return CompletableFuture.completedFuture(res);
                });
            }

            @Override
            public CompletableFuture<LegalInfo.LegalInfoItem> getPartnerLegalInfoItem() {
                return offerData.thenCompose(o -> {
                    if (o == null) {
                        return CompletableFuture.failedFuture(new RuntimeException("No offer data"));
                    }
                    return CompletableFuture.completedFuture(
                            LegalInfo.LegalInfoItem.builder()
                                    .legalAddress("Москва, Красная Роза, Морозов, подъезд 2")
                                    .ogrn("123456")
                                    .workingHours("Круглосуточно")
                                    .name("Тестовый партер Яндекс.Путешествий")
                                    .build());
                });
            }

            @Override
            public CompletableFuture<LegalInfo.LegalInfoItem> getHotelLegalInfoItem() {
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletableFuture<HotelItinerary> createHotelItinerary() {
                return CompletableFuture.completedFuture(null);
            }
        };
    }
}
