package ru.yandex.market.pvz.tms.command.migration;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.pvz.core.domain.banner.Banner;
import ru.yandex.market.pvz.core.domain.banner.BannerLegalPartner;
import ru.yandex.market.pvz.core.domain.banner.BannerPickupPoint;
import ru.yandex.market.pvz.core.domain.banner.BannerRepository;
import ru.yandex.market.pvz.core.domain.banner.DisplayType;
import ru.yandex.market.pvz.core.domain.banner.MessageType;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartner;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pvz.tms.command.migration.FillBannerInformationIdForOldBanners.COMMAND_NAME;

@TransactionlessEmbeddedDbTest
@Import({
        FillBannerInformationIdForOldBanners.class,
})
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class FillBannerInformationIdForOldBannersTest {

    private final FillBannerInformationIdForOldBanners command;
    private final TestPickupPointFactory pickupPointFactory;
    private final TestLegalPartnerFactory legalPartnerFactory;
    private final BannerRepository bannerRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TestableClock clock;

    @MockBean
    private Terminal terminal;

    @MockBean
    private PrintWriter printWriter;

    @Disabled
    @Test
    void checkCommand() {
        var legalPartner = createApprovedPartner();
        var pickupPoint = createActivePickupPointWithPartner(legalPartner);

        createBanner(pickupPoint, null, MessageType.START_SHIFT);
        createBanner(pickupPoint, null, MessageType.INDEBTEDNESS);
        createBanner(pickupPoint, null, MessageType.DISABLED);

        createBanner(null, legalPartner, MessageType.INDEBTEDNESS);
        createBanner(null, legalPartner, MessageType.DISABLED);
        createBanner(null, legalPartner, MessageType.BRANDED_GREETING);
        createBanner(null, legalPartner, MessageType.MULTI_GREETING);
        createBanner(null, legalPartner, MessageType.DEACTIVATION_NEXT_TO_BRAND);
        createBanner(null, legalPartner, MessageType.DEACTIVATION_UNPROFITABLE);

        String sql = "SELECT id FROM banner WHERE banner_information_id IS NULL";
        List<Long> ids = jdbcTemplate.queryForList(sql, Collections.emptyMap(), Long.class);
        assertThat(ids.size()).isEqualTo(9);

        when(terminal.getWriter()).thenReturn(printWriter);
        command.executeCommand(new CommandInvocation(COMMAND_NAME, new String[]{}, Collections.emptyMap()), terminal);

        ids = jdbcTemplate.queryForList(sql, Collections.emptyMap(), Long.class);
        assertThat(ids.size()).isEqualTo(0);
    }

    private LegalPartner createApprovedPartner() {
        var legalPartner = legalPartnerFactory.createLegalPartner();
        return legalPartnerFactory
                .forceApprove(legalPartner.getId(), LocalDate.of(2021, 1, 1));
    }

    private PickupPoint createActivePickupPointWithPartner(LegalPartner legalPartner) {
        var pickupPoint = pickupPointFactory.createPickupPoint(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .legalPartner(legalPartner)
                        .build()
        );
        return pickupPointFactory.updatePickupPoint(
                pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .active(true)
                        .build());
    }

    private void createBanner(PickupPoint pickupPoint, LegalPartner legalPartner, MessageType messageType) {
        BannerLegalPartner bannerLegalPartner =
                legalPartner == null
                ? null
                : BannerLegalPartner.builder().legalPartner(legalPartner).build();
        BannerPickupPoint bannerPickupPoint =
                pickupPoint == null
                ? null
                : BannerPickupPoint.builder().pickupPoint(pickupPoint).build();
        Banner banner = Banner.builder()
                .bannerLegalPartner(bannerLegalPartner)
                .bannerPickupPoint(bannerPickupPoint)
                .displayType(DisplayType.POPUP)
                .messageType(messageType.name())
                .closedAt(OffsetDateTime.now(clock))
                .date(LocalDate.now(clock))
                .build();
        banner.save();
        bannerRepository.save(banner);
    }

}
