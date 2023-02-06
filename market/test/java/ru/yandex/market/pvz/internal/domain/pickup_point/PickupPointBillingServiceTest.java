package ru.yandex.market.pvz.internal.domain.pickup_point;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.client.billing.dto.BillingPickupPointDto;
import ru.yandex.market.pvz.client.model.approve.ApproveStatus;
import ru.yandex.market.pvz.client.model.pickup_point.PickupPointBrandingType;
import ru.yandex.market.pvz.core.domain.delivery_service.DeliveryService;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartner;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.test.EmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestBrandRegionFactory;
import ru.yandex.market.pvz.core.test.factory.TestDeliveryServiceFactory;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.internal.PvzIntTest;

import static org.assertj.core.api.Assertions.assertThat;

@PvzIntTest
@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PickupPointBillingServiceTest {
    private final TestLegalPartnerFactory legalPartnerFactory;
    private final TestDeliveryServiceFactory deliveryServiceFactory;
    private final TestPickupPointFactory pickupPointFactory;
    private final TestBrandRegionFactory testBrandRegionFactory;
    private final PickupPointBillingService pickupPointBillingService;

    @Test
    void getAll() {
        DeliveryService deliveryService = deliveryServiceFactory.createDeliveryService();
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner(
                TestLegalPartnerFactory.LegalPartnerTestParamsBuilder.builder()
                        .deliveryService(deliveryService)
                        .build());
        legalPartner.changeApproveStatus(ApproveStatus.APPROVED);
        testBrandRegionFactory.createDefaults();
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .legalPartner(legalPartner)
                        .params(TestPickupPointFactory.PickupPointTestParams.builder()
                                .active(true)
                                .pvzMarketId(100L)
                                .returnAllowed(true)
                                .brandingType(PickupPointBrandingType.FULL)
                                .brandDate(LocalDate.now())
                                .build())
                        .build());

        List<BillingPickupPointDto> pickupPoints = pickupPointBillingService.getAll().stream()
                .filter(pp -> pp.getLegalPartnerId() == legalPartner.getId())
                .collect(Collectors.toList());

        assertThat(pickupPoints).hasSize(1);
        assertThat(pickupPoints)
                .usingElementComparatorIgnoringFields("transmissionReward")
                .isEqualTo(List.of(BillingPickupPointDto.builder()
                        .id(pickupPoint.getId())
                        .deliveryServiceId(deliveryService.getId())
                        .legalPartnerId(pickupPoint.getLegalPartner().getId())
                        .name(pickupPoint.getName())
                        .transmissionReward(pickupPoint.getTransmissionReward())
                        .cardOrderCompensationRate(pickupPoint.getCardOrderCompensationRate())
                        .cashOrderCompensationRate(pickupPoint.getCashOrderCompensationRate())
                        .active(false)
                        .returnAllowed(false)
                        .brandingType(pickupPoint.getBrandingType().name())
                        .brandRegionId(pickupPoint.getActualBrandData().getBrandRegion().getId())
                        .brandedSince(pickupPoint.getActualBrandData().getBrandedSince())
                        .pvzMarketId(pickupPoint.getPvzMarketId())
                        .build()
                ));
        BillingPickupPointDto pickupPointDto = pickupPoints.get(0);
        assertThat(pickupPointDto.getTransmissionReward())
                .isEqualByComparingTo(pickupPoint.getTransmissionReward());
    }
}
