package ru.yandex.market.pvz.core.domain.banner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.client.model.partner.LegalPartnerTerminationType;
import ru.yandex.market.pvz.client.model.pickup_point.PickupPointBrandingType;
import ru.yandex.market.pvz.core.domain.banner.params.BannerDisabledParams;
import ru.yandex.market.pvz.core.domain.banner.params.BannerIndebtednessParams;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartner;
import ru.yandex.market.pvz.core.domain.legal_partner.indebtedness.LegalPartnerIndebtednessCommandService;
import ru.yandex.market.pvz.core.domain.legal_partner.indebtedness.LegalPartnerIndebtednessParams;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.DeactivationReasonCommandService;
import ru.yandex.market.pvz.core.test.EmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestBrandRegionFactory;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerTerminationFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;

import static org.assertj.core.api.Assertions.assertThat;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class BannerCommandServiceTest {

    private final TestPickupPointFactory pickupPointFactory;
    private final BannerCommandService bannerCommandService;
    private final BannerQueryService bannerQueryService;
    private final DeactivationReasonCommandService deactivationReasonCommandService;
    private final LegalPartnerIndebtednessCommandService legalPartnerIndebtednessCommandService;
    private final TestBrandRegionFactory brandRegionFactory;
    private final TestLegalPartnerFactory legalPartnerFactory;
    private final TestLegalPartnerTerminationFactory terminationFactory;
    private final TestableClock clock;

    @BeforeEach
    void setup() {
        clock.clearFixed();
    }

    @Test
    void pickupPointCloseBannerTest() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        bannerCommandService.pickupPointCloseBanner(pickupPoint.getPvzMarketId(), MessageType.START_SHIFT,
                DisplayType.POPUP);
        var bannerResponseParams = bannerQueryService.getPickupPointBanners(pickupPoint.getPvzMarketId());

        assertThat(bannerResponseParams.getPopups()).isEmpty();
    }

    @Test
    void legalPartnerCloseBannerTest() {
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();
        bannerCommandService.legalPartnerCloseBanner(legalPartner.getPartnerId(), MessageType.START_SHIFT,
                DisplayType.POPUP);
        var bannerResponseParams = bannerQueryService.getLegalPartnerBanners(legalPartner.getPartnerId());

        assertThat(bannerResponseParams.getPopups()).isEmpty();
    }

    @Test
    void whenCloseNotClosableBanner() {
        brandRegionFactory.create(TestBrandRegionFactory.BrandRegionTestParams.builder()
                .region("Воронеж")
                .dailyTransmissionThreshold(5)
                .build());

        deactivationReasonCommandService.createDeactivationReason(LegalPartnerTerminationType.DEBT.getDescription(),
                "", false, false, null);

        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();
        legalPartner = legalPartnerFactory
                .forceApprove(legalPartner.getId(), LocalDate.of(2021, 1, 1));

        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .legalPartner(legalPartner)
                        .build());
        pickupPoint = pickupPointFactory.updatePickupPoint(
                pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .active(true)
                        .returnAllowed(true)
                        .brandingType(PickupPointBrandingType.FULL)
                        .brandDate(LocalDate.of(2021, 1, 5))
                        .brandRegion("Воронеж")
                        .build());

        BigDecimal debtSum = new BigDecimal(44000);
        LegalPartnerIndebtednessParams legalPartnerIndebtednessParams = LegalPartnerIndebtednessParams.builder()
                .legalPartnerId(legalPartner.getId())
                .debtSum(debtSum)
                .debtDate(LocalDate.now(clock))
                .build();
        legalPartnerIndebtednessCommandService.updateIndebtedness(legalPartnerIndebtednessParams);

        terminationFactory.createLegalPartnerTermination(
                TestLegalPartnerTerminationFactory.LegalPartnerTestParamsBuilder.builder()
                        .params(
                                TestLegalPartnerTerminationFactory.LegalPartnerTerminationTestParams
                                        .builder()
                                        .type(LegalPartnerTerminationType.DEBT)
                                        .fromTime(OffsetDateTime.now(clock))
                                        .legalPartnerId(legalPartner.getId())
                                        .build()
                        )
                        .build()
        );

        Long pvzMarketId = pickupPoint.getPvzMarketId();
        bannerCommandService.pickupPointCloseBanner(pvzMarketId, MessageType.DISABLED, DisplayType.BANNER);

        BannerParams expected = BannerParams
                .builder()
                .messageType(MessageType.DISABLED.name())
                .params(
                        BannerDisabledParams
                                .builder()
                                .debtSum(debtSum)
                                .reason(BannerQueryService.DEFAULT_DEBT_DISABLED_REASON)
                                .build()
                )
                .build();
        BannerParams notExpected = BannerParams
                .builder()
                .messageType(MessageType.INDEBTEDNESS.name())
                .params(
                        BannerIndebtednessParams
                                .builder()
                                .debtSum(debtSum)
                                .build()
                )
                .build();
        BannerResponseParams bannerResponseParams =
                bannerQueryService.getPickupPointBanners(pickupPoint.getPvzMarketId());
        assertThat(bannerResponseParams.getBanners().contains(expected)).isTrue();
        assertThat(bannerResponseParams.getBanners().contains(notExpected)).isFalse();
    }
}
