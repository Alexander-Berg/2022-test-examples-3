package ru.yandex.market.logistics.iris.domain.converter;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import ru.yandex.market.logistics.iris.converter.PartnerResponseConverter;
import ru.yandex.market.logistics.iris.entity.partner.FulfillmentPartner;
import ru.yandex.market.logistics.iris.service.lms.LmsPartnerStatus;
import ru.yandex.market.logistics.iris.service.lms.LmsPartnerType;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

public class PartnerResponseConverterTest {

    @Test
    public void convertSinglePartnerResponse() {
        PartnerResponse partnerResponse = PartnerResponse.newBuilder()
            .id(2L)
            .partnerType(PartnerType.FULFILLMENT)
            .name("MARKET_ROSTOV")
            .status(PartnerStatus.ACTIVE)
            .build();

        FulfillmentPartner fulfillmentPartner = PartnerResponseConverter.convert(partnerResponse);

        assertSoftly(assertions -> {
                assertions.assertThat(fulfillmentPartner.getId()).isEqualTo(2);
                assertions.assertThat(fulfillmentPartner.getName()).isEqualTo("MARKET_ROSTOV");
                assertions.assertThat(fulfillmentPartner.getPartnerType()).isEqualTo(LmsPartnerType.FULFILLMENT);
                assertions.assertThat(fulfillmentPartner.getStatus()).isEqualTo(LmsPartnerStatus.ACTIVE);
            }
        );
    }

    @Test
    public void convertMultiPartnerResponse() {
        List<PartnerResponse> partnerResponses = Arrays.asList(
            PartnerResponse.newBuilder()
                .id(2L)
                .partnerType(PartnerType.FULFILLMENT)
                .name("MARKET_ROSTOV")
                .status(PartnerStatus.ACTIVE)
                .build(),
            PartnerResponse.newBuilder()
                .id(3L)
                .partnerType(PartnerType.FULFILLMENT)
                .name("MARKET_TOM")
                .status(PartnerStatus.ACTIVE)
                .build()
        );

        List<FulfillmentPartner> fulfillmentPartners = PartnerResponseConverter.convert(partnerResponses);

        assertSoftly(assertions ->
            assertions.assertThat(fulfillmentPartners.size()).isEqualTo(2)
        );

        FulfillmentPartner fulfillmentPartner1 = fulfillmentPartners.get(0);
        assertSoftly(assertions -> {
            assertions.assertThat(fulfillmentPartner1.getId()).isEqualTo(2);
            assertions.assertThat(fulfillmentPartner1.getName()).isEqualTo("MARKET_ROSTOV");
            assertions.assertThat(fulfillmentPartner1.getPartnerType()).isEqualTo(LmsPartnerType.FULFILLMENT);
            assertions.assertThat(fulfillmentPartner1.getStatus()).isEqualTo(LmsPartnerStatus.ACTIVE);
        });

        FulfillmentPartner fulfillmentPartner2 = fulfillmentPartners.get(1);
        assertSoftly(assertions -> {
            assertions.assertThat(fulfillmentPartner2.getId()).isEqualTo(3);
            assertions.assertThat(fulfillmentPartner2.getName()).isEqualTo("MARKET_TOM");
            assertions.assertThat(fulfillmentPartner2.getPartnerType()).isEqualTo(LmsPartnerType.FULFILLMENT);
            assertions.assertThat(fulfillmentPartner2.getStatus()).isEqualTo(LmsPartnerStatus.ACTIVE);
        });
    }

    @Test
    public void shouldReturnWithStatusUnknownIfPartnerResponseHasStatusNull() {
        PartnerResponse partnerResponse = PartnerResponse.newBuilder()
            .id(2L)
            .partnerType(PartnerType.FULFILLMENT)
            .name("MARKET_ROSTOV")
            .readableName("Яндекс.Маркет Ростов")
            .build();

        FulfillmentPartner fulfillmentPartner = PartnerResponseConverter.convert(partnerResponse);

        assertSoftly(assertions ->
            assertions.assertThat(fulfillmentPartner.getStatus()).isEqualTo(LmsPartnerStatus.UNKNOWN)
        );
    }

    @Test
    public void shouldReturnWithPartnerTypeUnknownIfPartnerResponseHasPartnerTypeNull() {
        PartnerResponse partnerResponse = PartnerResponse.newBuilder()
            .id(2L)
            .name("MARKET_ROSTOV")
            .readableName("Яндекс.Маркет Ростов")
            .status(PartnerStatus.ACTIVE)
            .build();

        FulfillmentPartner fulfillmentPartner = PartnerResponseConverter.convert(partnerResponse);

        assertSoftly(assertions ->
            assertions.assertThat(fulfillmentPartner.getPartnerType()).isEqualTo(LmsPartnerType.UNKNOWN)
        );
    }

    @Test
    public void shouldSetTrueToReferenceItemsSyncEnabledIfItIsSetTrueInPartnerResponse() {
        PartnerResponse partnerResponse = PartnerResponse.newBuilder()
            .id(2L)
            .name("MARKET_ROSTOV")
            .readableName("Яндекс.Маркет Ростов")
            .status(PartnerStatus.ACTIVE)
            .korobyteSyncEnabled(true)
            .build();

        FulfillmentPartner fulfillmentPartner = PartnerResponseConverter.convert(partnerResponse);

        assertSoftly(assertions ->
            assertions.assertThat(fulfillmentPartner.isReferenceItemsSyncEnabled()).isEqualTo(true)
        );
    }
}
