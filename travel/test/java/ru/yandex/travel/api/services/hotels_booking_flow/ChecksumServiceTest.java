package ru.yandex.travel.api.services.hotels_booking_flow;

import org.junit.Test;

import ru.yandex.travel.hotels.common.refunds.RefundRules;
import ru.yandex.travel.hotels.models.booking_flow.HotelInfo;
import ru.yandex.travel.hotels.models.booking_flow.LocalizedPansionInfo;
import ru.yandex.travel.hotels.models.booking_flow.Offer;
import ru.yandex.travel.hotels.models.booking_flow.PartnerHotelInfo;
import ru.yandex.travel.hotels.models.booking_flow.Rate;
import ru.yandex.travel.hotels.models.booking_flow.RateInfo;
import ru.yandex.travel.hotels.models.booking_flow.RoomInfo;
import ru.yandex.travel.hotels.models.booking_flow.promo.PromoCampaignsInfo;
import ru.yandex.travel.hotels.models.booking_flow.promo.Taxi2020PromoCampaign;
import ru.yandex.travel.hotels.proto.EPansionType;

import static org.assertj.core.api.Assertions.assertThat;

public class ChecksumServiceTest {
    private ChecksumService service = new ChecksumService();

    @Test
    public void buildCheckSum_taxi2020promo() {
        Offer offer1 = defaultOfferBuilder()
                .promoCampaignsInfo(PromoCampaignsInfo.builder()
                        .taxi2020(new Taxi2020PromoCampaign(true))
                        .build())
                .build();
        String checkSum1 = service.buildCheckSum(offer1);
        assertThat(checkSum1).isNotEmpty();

        Offer offer2 = defaultOfferBuilder()
                .promoCampaignsInfo(PromoCampaignsInfo.builder()
                        .taxi2020(new Taxi2020PromoCampaign(false))
                        .build())
                .build();
        String checkSum2 = service.buildCheckSum(offer2);
        assertThat(checkSum2).isNotEmpty();
        assertThat(checkSum2).isNotEqualTo(checkSum1);

        Offer offer3 = defaultOfferBuilder()
                .promoCampaignsInfo(null)
                .build();
        String checkSum3 = service.buildCheckSum(offer3);
        assertThat(checkSum3).isNotEmpty();
        assertThat(checkSum3).isEqualTo(checkSum2);
    }

    private Offer.OfferBuilder defaultOfferBuilder() {
        return Offer.builder()
                .hotelInfo(HotelInfo.builder()
                        .name("Hotel A")
                        .address("Address A")
                        .build())
                .partnerHotelInfo(PartnerHotelInfo.builder()
                        .name("Hotel P")
                        .address("Address P")
                        .build())
                .roomInfo(RoomInfo.builder()
                        .name("Room A")
                        .pansionInfo(new LocalizedPansionInfo(EPansionType.PT_BB, "PT Name?"))
                        .build())
                .rateInfo(RateInfo.builder().totalRate(new Rate("6000", "RUB")).build())
                .refundRules(RefundRules.builder().build());
    }
}
