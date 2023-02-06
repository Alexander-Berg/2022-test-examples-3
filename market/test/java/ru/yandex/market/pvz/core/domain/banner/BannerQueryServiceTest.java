package ru.yandex.market.pvz.core.domain.banner;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.core.domain.configuration.global.ConfigurationGlobalCommandService;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartner;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.change_active.ChangeActiveReason;
import ru.yandex.market.pvz.core.domain.pickup_point.change_active.ChangeActiveType;
import ru.yandex.market.pvz.core.domain.pickup_point.change_active.PickupPointChangeActiveCommandService;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.DAYS_FOR_DEACTIVATION_BANNER;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.PICKUP_POINT_DEACTIVATION_ENABLED;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class BannerQueryServiceTest {

    private final TestableClock clock;

    private final TestPickupPointFactory pickupPointFactory;
    private final TestLegalPartnerFactory legalPartnerFactory;
    private final BannerQueryService bannerQueryService;
    private final PickupPointChangeActiveCommandService pickupPointChangeActiveCommandService;
    private final ConfigurationGlobalCommandService configurationGlobalCommandService;

    @Test
    void getPickupPointBannersTest() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        var bannerResponseParams = bannerQueryService.getPickupPointBanners(pickupPoint.getPvzMarketId());

        assertThat(bannerResponseParams.getPopups()).isEmpty();
    }

    @Test
    void getLegalPartnerBannersTest() {
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();
        var bannerResponseParams = bannerQueryService.getLegalPartnerBanners(legalPartner.getPartnerId());

        assertThat(bannerResponseParams.getPopups()).isEmpty();
    }

    @Test
    void deactivationBannerTest() {
        checkDeactivationBanner(ChangeActiveReason.NEXT_TO_BRAND, MessageType.DEACTIVATION_NEXT_TO_BRAND);
    }

    @Test
    void deactivationUnprofitableBannerTest() {
        checkDeactivationBanner(ChangeActiveReason.UNPROFITABLE, MessageType.DEACTIVATION_UNPROFITABLE);
    }

    private void checkDeactivationBanner(ChangeActiveReason reason, MessageType messageType) {
        configurationGlobalCommandService.setValue(PICKUP_POINT_DEACTIVATION_ENABLED, true);

        var legalPartner = legalPartnerFactory.createLegalPartner();
        legalPartner = legalPartnerFactory
                .forceApprove(legalPartner.getId(), LocalDate.of(2021, 1, 1));
        var pickupPoint = pickupPointFactory.createPickupPoint(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .legalPartner(legalPartner)
                        .build()
        );
        pickupPointFactory.updatePickupPoint(
                pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .active(true)
                        .build());

        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        OffsetDateTime dateTime = OffsetDateTime.of(LocalDateTime.now(), zone);
        clock.setFixed(dateTime.toInstant(), zone);

        pickupPointChangeActiveCommandService.saveToChangeLog(Collections.singletonList(pickupPoint.getPvzMarketId()),
                LocalDate.now(clock), reason, ChangeActiveType.DEACTIVATE);
        pickupPointChangeActiveCommandService.changeActive();

        var bannerResponseParams = bannerQueryService.getLegalPartnerBanners(legalPartner.getPartnerId());

        assertThat(bannerResponseParams.getPopups()).isEmpty();
        var banner = bannerResponseParams.getBanners().get(0);
        assertThat(banner.isClosable()).isTrue();
        assertThat(banner.getMessageType()).isEqualTo(messageType.name());
    }

    @Test
    public void noBannerIfTooManyDaysPassed() {
        int daysForDeactivationBanner = 5;
        configurationGlobalCommandService.setValue(PICKUP_POINT_DEACTIVATION_ENABLED, true);
        configurationGlobalCommandService.setValue(DAYS_FOR_DEACTIVATION_BANNER, daysForDeactivationBanner);

        var legalPartner = legalPartnerFactory.createLegalPartner();
        legalPartner = legalPartnerFactory
                .forceApprove(legalPartner.getId(), LocalDate.of(2021, 1, 1));
        var pickupPoint = pickupPointFactory.createPickupPoint(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .legalPartner(legalPartner)
                        .build()
        );
        pickupPointFactory.updatePickupPoint(
                pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .active(true)
                        .build());

        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        OffsetDateTime dateTime = OffsetDateTime.of(LocalDateTime.of(2021, 12, 1, 14, 35), zone);
        clock.setFixed(dateTime.toInstant(), zone);

        pickupPointChangeActiveCommandService.saveToChangeLog(Collections.singletonList(pickupPoint.getPvzMarketId()),
                LocalDate.now(clock), ChangeActiveReason.NEXT_TO_BRAND, ChangeActiveType.DEACTIVATE);
        pickupPointChangeActiveCommandService.changeActive();

        dateTime = dateTime.plusDays(daysForDeactivationBanner + 1);
        clock.setFixed(dateTime.toInstant(), zone);
        var bannerResponseParams = bannerQueryService.getLegalPartnerBanners(legalPartner.getPartnerId());

        assertThat(bannerResponseParams.getPopups()).isEmpty();
    }
}
