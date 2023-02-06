package ru.yandex.market.pvz.core.domain.pickup_point;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.domain.approve.delivery_service.model.DropOffCreateParams;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestDeliveryServiceFactory;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint.DEFAULT_DROP_OFF_FEATURE;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PickupPointQueryServiceTest {

    private static final long COURIER_DS_ID = 4876980L;

    private final TestDeliveryServiceFactory deliveryServiceFactory;
    private final TestLegalPartnerFactory legalPartnerFactory;
    private final TestPickupPointFactory pickupPointFactory;

    private final PickupPointQueryService pickupPointQueryService;

    @Test
    void getPickupPointForDropOffCreation() {
        var deliveryService = deliveryServiceFactory.createDeliveryService();
        var legalPartner = legalPartnerFactory.createLegalPartner(TestLegalPartnerFactory.LegalPartnerTestParamsBuilder
                .builder()
                .deliveryService(deliveryService)
                .build());
        var pickupPoint = pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder.builder()
                .legalPartner(legalPartner)
                .build());

        var actual = pickupPointQueryService.getDropOff(pickupPoint.getId(), COURIER_DS_ID);

        var expected = DropOffCreateParams.builder()
                .pickupPointId(pickupPoint.getId())
                .lmsId(String.valueOf(pickupPoint.getLmsId()))
                .dropOffDsId(deliveryService.getId())
                .address(pickupPoint.getLocation().getAddress())
                .campaignId(pickupPoint.getPvzMarketId())
                .legalPartnerName(legalPartner.getOrganization().getFullName())
                .dropOffName(pickupPoint.getName())
                .courierDsId(COURIER_DS_ID)
                .alreadyCreatedAsDropOff(DEFAULT_DROP_OFF_FEATURE)
                .token(deliveryService.getToken())
                .build();

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void getPickupPointForDropOffCreationWhenItHadBeenCreatedBefore() {
        var deliveryService = deliveryServiceFactory.createDeliveryService();
        var legalPartner = legalPartnerFactory.createLegalPartner(TestLegalPartnerFactory.LegalPartnerTestParamsBuilder
                .builder()
                .deliveryService(deliveryService)
                .build());
        var pickupPoint = pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder.builder()
                .legalPartner(legalPartner)
                .build());

        pickupPointFactory.createDropOff(pickupPoint.getId());

        var actual = pickupPointQueryService.getDropOff(pickupPoint.getId(), COURIER_DS_ID);

        var expected = DropOffCreateParams.builder()
                .pickupPointId(pickupPoint.getId())
                .lmsId(String.valueOf(pickupPoint.getLmsId()))
                .dropOffDsId(deliveryService.getId())
                .address(pickupPoint.getLocation().getAddress())
                .campaignId(pickupPoint.getPvzMarketId())
                .legalPartnerName(legalPartner.getOrganization().getFullName())
                .dropOffName(pickupPoint.getName())
                .courierDsId(COURIER_DS_ID)
                .alreadyCreatedAsDropOff(!DEFAULT_DROP_OFF_FEATURE)
                .token(deliveryService.getToken())
                .build();

        assertThat(actual).isEqualTo(expected);
    }
}
