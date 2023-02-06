package ru.yandex.travel.api.services.hotels_booking_flow;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import ru.yandex.travel.commons.proto.ProtoUtils;
import ru.yandex.travel.credentials.UserCredentials;
import ru.yandex.travel.hotels.common.LocationType;
import ru.yandex.travel.hotels.common.token.Occupancy;
import ru.yandex.travel.hotels.common.token.TokenCodec;
import ru.yandex.travel.hotels.common.token.TravelToken;
import ru.yandex.travel.hotels.models.booking_flow.Coordinates;
import ru.yandex.travel.hotels.models.booking_flow.HotelInfo;
import ru.yandex.travel.hotels.models.booking_flow.LegalInfo;
import ru.yandex.travel.hotels.proto.EPartnerId;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BaseBookingFlowTest {
    protected TokenCodec tokenCodec = mock(TokenCodec.class);

    protected BookingFlowContext prepareContext(String offerId) {
        BookingFlowContext context = BookingFlowContext.builder()
                .createdAt(Instant.now())
                .userIp("127.0.0.1")
                .deduplicationKey(ProtoUtils.randomId())
                .userAgent("unitTest/1.0")
                .offerLabel("someLabel")
                .userCredentials(UserCredentials.builder().build())
                .build();

        if ("malformedToken".equals(offerId)) {
            context.setToken(offerId);
        } else {
            context.setToken("testToken");
        }
        context.setStage(BookingFlowContext.Stage.GET_OFFER);
        when(tokenCodec.decode("testToken")).thenReturn(mockedTokenResponse(offerId));
        return context;
    }

    protected TravelToken mockedTokenResponse(String offerId) {
        LocalDateTime generatedAt;
        if ("expiredToken".equals(offerId)) {
            generatedAt = LocalDateTime.now().minusDays(1);
        } else {
            generatedAt = LocalDateTime.now().minusMinutes(1);
        }

        return TravelToken.builder()
                .setTokenIdBytes(offerId.getBytes())
                .setOriginalId("someHotelId")
                .setPartnerId(EPartnerId.PI_UNUSED)
                .setPermalink(42L)
                .setOccupancy(Occupancy.fromString("2"))
                .setCheckInDate(LocalDate.now())
                .setCheckOutDate(LocalDate.now().plusDays(1))
                .setGeneratedAt(generatedAt)
                .setOfferId(offerId)
                .build();
    }

    protected HotelInfo mockedGeoHotelInfo() {
        return HotelInfo.builder()
                .permalink(1054982517L)
                .name("Radisson Славянская")
                .phone("+7 (495) 941-80-20")
                .address("Россия, Москва, площадь Европы, 2")
                .stars(4)
                .rating(4.7f)
                .coordinates(new Coordinates(37.568099, 55.741881))
                .imageUrlTemplate("https://avatars.mds.yandex.net" +
                        "/get-altay/374295/2a0000015b1dade88e53c818ef0951922ba0/%s")
                .workingHours("ежедневно, круглосуточно")
                .locationType(LocationType.MOSCOW)
                .build();
    }

    protected LegalInfo.LegalInfoItem mockedGeoLegalItem() {
        return LegalInfo.LegalInfoItem.builder()
                .name("ООО \"СЛАВЯНСКАЯ\"")
                .ogrn("1027739155620")
                .actualAddress("Россия, Москва, площадь Европы, 2")
                .legalAddress("121059 МОСКВА ГОРОД ПЛОЩАДЬ ЕВРОПЫ 2")
                .workingHours("ежедневно, круглосуточно")
                .build();
    }
}
