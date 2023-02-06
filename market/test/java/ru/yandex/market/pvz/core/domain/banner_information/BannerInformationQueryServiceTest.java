package ru.yandex.market.pvz.core.domain.banner_information;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.client.model.partner.LegalPartnerTerminationType;
import ru.yandex.market.pvz.client.model.pickup_point.PickupPointBrandingType;
import ru.yandex.market.pvz.client.model.survey.PartnerSurveyStatus;
import ru.yandex.market.pvz.core.domain.banner.BannerCommandService;
import ru.yandex.market.pvz.core.domain.banner.DisplayType;
import ru.yandex.market.pvz.core.domain.banner.MessageType;
import ru.yandex.market.pvz.core.domain.banner_information.frontend_page.FrontendPage;
import ru.yandex.market.pvz.core.domain.banner_information.frontend_page.FrontendPageRepository;
import ru.yandex.market.pvz.core.domain.configuration.global.ConfigurationGlobalCommandService;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartner;
import ru.yandex.market.pvz.core.domain.legal_partner.indebtedness.LegalPartnerIndebtednessCommandService;
import ru.yandex.market.pvz.core.domain.legal_partner.indebtedness.LegalPartnerIndebtednessParams;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointRepository;
import ru.yandex.market.pvz.core.domain.pickup_point.change_active.ChangeActiveReason;
import ru.yandex.market.pvz.core.domain.pickup_point.change_active.ChangeActiveType;
import ru.yandex.market.pvz.core.domain.pickup_point.change_active.PickupPointChangeActiveCommandService;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.DeactivationReasonCommandService;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.PickupPointDeactivationCommandService;
import ru.yandex.market.pvz.core.domain.survey.SurveyParams;
import ru.yandex.market.pvz.core.domain.survey.SurveyPartnerCommandService;
import ru.yandex.market.pvz.core.domain.survey.SurveyPartnerParams;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestBannerInformationFactory;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerTerminationFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.core.test.factory.TestSurveyFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.DAYS_FOR_DEACTIVATION_BANNER;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.DEACTIVATION_WITH_REASONS;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.SURVEY_BANNERS_ENABLED;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class BannerInformationQueryServiceTest {

    private static final String REASON_NAME = "Очередная причина деактивации";
    private static final String REASON_DETAILS = "Снова причины деактивации создаю";

    private final TestableClock clock;
    private final TestPickupPointFactory pickupPointFactory;
    private final TestLegalPartnerFactory legalPartnerFactory;
    private final BannerInformationQueryService bannerInformationQueryService;
    private final PickupPointRepository pickupPointRepository;
    private final BannerCommandService bannerCommandService;
    private final FrontendPageRepository frontendPageRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final LegalPartnerIndebtednessCommandService legalPartnerIndebtednessCommandService;
    private final TestLegalPartnerTerminationFactory terminationFactory;
    private final PickupPointChangeActiveCommandService pickupPointChangeActiveCommandService;
    private final TestBannerInformationFactory bannerInformationFactory;
    private final BannerInformationParamsMapper bannerInformationParamsMapper;
    private final PickupPointDeactivationCommandService pickupPointDeactivationCommandService;
    private final DeactivationReasonCommandService deactivationReasonCommandService;
    private final TestSurveyFactory surveyFactory;
    private final SurveyPartnerCommandService surveyPartnerCommandService;
    private final ConfigurationGlobalCommandService configurationGlobalCommandService;

    @BeforeEach
    void setUp() {
        configurationGlobalCommandService.setValue(SURVEY_BANNERS_ENABLED, true);
    }

    @Test
    void checkPickupPointFeatures() {
        var pickupPointEntity = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.NONE, false, null, null
        );
        var pickupPoint =
                pickupPointRepository.findPickupPointForBannerByPvzMarketId(pickupPointEntity.getPvzMarketId());
        var pickupPointFeatures = bannerInformationQueryService.getPickupPointFeatures(pickupPoint.get());
        assertThat(pickupPointFeatures).isEqualTo(Set.of(BannerCampaignFeatures.NOT_BRANDED.name()));

        pickupPointEntity = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.FULL, true, null, null
        );
        pickupPoint = pickupPointRepository.findPickupPointForBannerByPvzMarketId(pickupPointEntity.getPvzMarketId());
        pickupPointFeatures = bannerInformationQueryService.getPickupPointFeatures(pickupPoint.get());
        assertThat(pickupPointFeatures).isEqualTo(
                Set.of(BannerCampaignFeatures.BRANDED.name(), BannerCampaignFeatures.DROP_OFF.name())
        );
    }

    @Test
    void checkLegalPartnerFeatures() {
        var partner = legalPartnerFactory.createLegalPartner();
        var pickupPointEntity = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.NONE, false, partner, null
        );
        var pickupPoint = pickupPointRepository.findPickupPointForBannerByPvzMarketId(pickupPointEntity.getPvzMarketId());
        var partnerFeatures = bannerInformationQueryService.getPartnerFeatures(List.of(pickupPoint.get()));
        assertThat(partnerFeatures).isEqualTo(Set.of(BannerCampaignFeatures.NOT_BRANDED.name()));

        pickupPointEntity = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.FULL, true, partner, null
        );
        var pickupPoint2 = pickupPointRepository.findPickupPointForBannerByPvzMarketId(pickupPointEntity.getPvzMarketId());
        partnerFeatures =
                bannerInformationQueryService.getPartnerFeatures(List.of(pickupPoint.get(), pickupPoint2.get()));
        assertThat(partnerFeatures).isEqualTo(
                Set.of(BannerCampaignFeatures.BRANDED.name(), BannerCampaignFeatures.DROP_OFF.name())
        );
    }

    @Test
    void checkLegalPartnerTimeOffset() {
        var partner = legalPartnerFactory.createLegalPartner();
        var pickupPointEntity = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.NONE, false, partner, null
        );
        var pickupPoint = pickupPointRepository.findPickupPointForBannerByPvzMarketId(pickupPointEntity.getPvzMarketId());
        int timeOffset = bannerInformationQueryService.getLegalPartnerTimeOffset(List.of(pickupPoint.get()));
        assertThat(timeOffset).isEqualTo(BannerInformationQueryService.DEFAULT_TIME_OFFSET);

        pickupPointEntity = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.NONE, false, partner, 5
        );
        var pickupPoint2 = pickupPointRepository.findPickupPointForBannerByPvzMarketId(pickupPointEntity.getPvzMarketId());
        timeOffset =
                bannerInformationQueryService.getLegalPartnerTimeOffset(List.of(pickupPoint.get(), pickupPoint2.get()));
        assertThat(timeOffset).isEqualTo(BannerInformationQueryService.DEFAULT_TIME_OFFSET);

        pickupPointEntity = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.NONE, false, partner, 2
        );
        var pickupPoint3 = pickupPointRepository.findPickupPointForBannerByPvzMarketId(pickupPointEntity.getPvzMarketId());
        timeOffset = bannerInformationQueryService.getLegalPartnerTimeOffset(
                List.of(pickupPoint.get(), pickupPoint2.get(), pickupPoint3.get())
        );
        assertThat(timeOffset).isEqualTo(2);

        pickupPointEntity = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.NONE, false, partner, 5
        );
        var pickupPoint4 = pickupPointRepository.findPickupPointForBannerByPvzMarketId(pickupPointEntity.getPvzMarketId());
        timeOffset = bannerInformationQueryService.getLegalPartnerTimeOffset(
                List.of(pickupPoint.get(), pickupPoint2.get(), pickupPoint3.get(), pickupPoint4.get())
        );
        assertThat(timeOffset).isEqualTo(5);
    }

    @Test
    void whenPickupPointBannersEmpty() {
        var pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.NONE, false, null, 5
        );
        var banners = bannerInformationQueryService.getPickupPointBanners(pickupPoint.getPvzMarketId(), "");
        assertThat(banners.size()).isZero();
    }

    @Test
    void whenLegalPartnerBannersEmpty() {
        var partner = legalPartnerFactory.createLegalPartner();
        var banners = bannerInformationQueryService.getLegalPartnerBanners(partner.getPartnerId(), "");
        assertThat(banners.size()).isZero();

        pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.NONE, false, partner, 5
        );
        banners = bannerInformationQueryService.getLegalPartnerBanners(partner.getPartnerId(), "");
        assertThat(banners.size()).isZero();
    }

    @Test
    void checkParsingUnusualFrequencyExpression() {
        var banner = buildBannerInformationForFrequencyExpressionCheck(false, null, null);
        banner.parseFrequencyExpression(OffsetDateTime.now(clock));
        assertThat(banner.isNeedShow()).isTrue();

        banner = buildBannerInformationForFrequencyExpressionCheck(true, null, null);
        banner.parseFrequencyExpression(OffsetDateTime.now(clock));
        assertThat(banner.isNeedShow()).isTrue();

        banner = buildBannerInformationForFrequencyExpressionCheck(true, "incorrect expression", 10);
        banner.parseFrequencyExpression(OffsetDateTime.now(clock));
        assertThat(banner.isNeedShow()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("getFrequencyExpression")
    void checkParsingFrequencyExpression(
            String startDate, int timeOffset, int testCount, String frequency, int duration, int dateIncrease, int dateDecrease,
            ChronoUnit chronoUnitIncrease, ChronoUnit chronoUnitDecrease
            ) {
        ZoneOffset zone = ZoneOffset.ofHours(timeOffset);
        OffsetDateTime start = OffsetDateTime.parse(startDate).withOffsetSameInstant(zone);
        for (int i = 0; i < testCount; i++) {
            clock.setFixed(start.toInstant(), zone);
            var banner = buildBannerInformationForFrequencyExpressionCheck(
                    true, frequency, duration
            );
            banner.parseFrequencyExpression(start);

            Instant expected = start.minus(dateDecrease, chronoUnitDecrease).toInstant();
            assertThat(banner.isNeedShow()).isTrue();
            assertThat(banner.getDateTimeShowPrevious().compareTo(expected)).isZero();

            start = start.plus(dateIncrease, chronoUnitIncrease);
        }
    }

    private static Stream<Arguments> getFrequencyExpression() {
        return Stream.of(
                Arguments.of(
                        "2021-12-31T11:15:00Z", 1, 48, "0 */30 * * * ? *", 30, 30, 15, ChronoUnit.MINUTES,
                        ChronoUnit.MINUTES
                ),
                Arguments.of(
                        "2021-12-31T08:30:00Z", 4, 24, "0 0 * * * ? *", 60, 60, 30, ChronoUnit.MINUTES,
                        ChronoUnit.MINUTES
                ),
                Arguments.of(
                        "2021-12-31T05:30:00Z", 7, 12, "0 0 0/2 * * ? *", 120, 2, 30, ChronoUnit.HOURS,
                        ChronoUnit.MINUTES
                ),
                Arguments.of(
                        "2021-12-31T10:00:00Z", 2, 4, "0 0 2/6 * * ? *", 360, 6, 4, ChronoUnit.HOURS,
                        ChronoUnit.HOURS
                ),
                Arguments.of(
                        "2021-12-13T10:00:00Z", 5, 1, "0 0 9 ? * MON *", 600, 1, 6, ChronoUnit.WEEKS,
                        ChronoUnit.HOURS
                ),
                Arguments.of(
                        "2021-12-02T00:00:00Z", 3, 2, "0 0 10 1 * ? *", 4320, 1, 17, ChronoUnit.MONTHS,
                        ChronoUnit.HOURS
                ),
                Arguments.of(
                        "2022-03-08T10:00:00Z", 3, 4, "0 0 10 8 3 ? *", 600, 1, 3, ChronoUnit.YEARS,
                        ChronoUnit.HOURS
                )
        );
    }

    @Test
    void whenDurationEnded() {
        ZoneOffset zone = ZoneOffset.ofHours(5);
        OffsetDateTime start = OffsetDateTime.parse("2021-12-31T11:15:00Z").withOffsetSameInstant(zone);
        clock.setFixed(start.toInstant(), zone);
        var banner = buildBannerInformationForFrequencyExpressionCheck(
                true, "0 0 14 * * ? *", 60
        );
        banner.parseFrequencyExpression(start);

        assertThat(banner.isNeedShow()).isFalse();
    }

    @Test
    void checkStartShift() {
        // воскресенье
        clock.setFixed(Instant.parse("2021-12-05T12:00:00Z"), ZoneOffset.ofHours(0));

        var pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.NONE, false, null, 5
        );
        var banner = bannerInformationFactory.createStartShiftBanner();

        var banners = bannerInformationQueryService.getPickupPointBanners(pickupPoint.getPvzMarketId(), "");
        assertThat(banners.size()).isZero();

        clock.setFixed(Instant.parse("2021-12-06T03:59:00Z"), ZoneOffset.ofHours(0));
        banners = bannerInformationQueryService.getPickupPointBanners(pickupPoint.getPvzMarketId(), "");
        assertThat(banners.size()).isZero();

        clock.setFixed(Instant.parse("2021-12-06T04:01:00Z"), ZoneOffset.ofHours(0));
        banners = bannerInformationQueryService.getPickupPointBanners(pickupPoint.getPvzMarketId(), "");
        assertThat(banners.size()).isEqualTo(1);

        var startShiftBanner = banners.get(0);
        var expectedBanner = setDateTimeShowPrevious(banner, "2021-12-06T04:00:00Z");

        assertThat(startShiftBanner).isEqualTo(expectedBanner);

        bannerCommandService.pickupPointCloseBanner(pickupPoint.getPvzMarketId(), MessageType.START_SHIFT,
                DisplayType.POPUP);
        clock.setFixed(Instant.parse("2021-12-06T06:52:00Z"), ZoneOffset.ofHours(0));

        banners = bannerInformationQueryService.getPickupPointBanners(pickupPoint.getPvzMarketId(), "");
        assertThat(banners.size()).isZero();
    }

    @Test
    void checkIndebtednessBanner() {
        clock.setFixed(Instant.now(), ZoneOffset.ofHours(3));
        var partner = legalPartnerFactory.createLegalPartner();
        var pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.NONE, false, partner, 3
        );
        var pickupPointBanner = bannerInformationFactory.createIndebtednessBanner(CampaignType.TPL_OUTLET);
        var partnerBanner = bannerInformationFactory.createIndebtednessBanner(CampaignType.TPL_PARTNER);

        setLegalPartnerIndebtedness(partner, partnerBanner, pickupPointBanner);

        var pickupPointBanners = bannerInformationQueryService.getPickupPointBanners(pickupPoint.getPvzMarketId(), "");
        var partnerBanners = bannerInformationQueryService.getLegalPartnerBanners(partner.getPartnerId(), "");
        assertThat(pickupPointBanners.size()).isEqualTo(1);
        assertThat(partnerBanners.size()).isEqualTo(1);

        var expectedPickupPointBanner = setDateTimeShowPrevious(
                pickupPointBanner, clock.instant().toString()
        );
        var expectedPartnerBanner = setDateTimeShowPrevious(partnerBanner, clock.instant().toString());
        assertThat(expectedPickupPointBanner).isEqualTo(pickupPointBanners.get(0));
        assertThat(expectedPartnerBanner).isEqualTo(partnerBanners.get(0));

        bannerCommandService.pickupPointCloseBanner(pickupPoint.getPvzMarketId(), MessageType.INDEBTEDNESS,
                DisplayType.POPUP);
        clock.setFixed(Instant.now(clock).plusSeconds(600), ZoneOffset.ofHours(0));
        pickupPointBanners = bannerInformationQueryService.getPickupPointBanners(pickupPoint.getPvzMarketId(), "");
        partnerBanners = bannerInformationQueryService.getLegalPartnerBanners(partner.getPartnerId(), "");
        assertThat(pickupPointBanners.size()).isEqualTo(1);
        assertThat(partnerBanners.size()).isEqualTo(1);
    }

    @Test
    void checkDebtDisabledBanner() {
        clock.setFixed(Instant.now(), ZoneOffset.ofHours(3));
        var partner = legalPartnerFactory.createLegalPartner();
        var pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.NONE, false, partner, 3
        );
        buildBanner(
                false, null, null, CampaignType.TPL_OUTLET, null, null, MessageType.INDEBTEDNESS.name(), null
        );
        var pickupPointDebtBanner =
                buildBanner(false, null, null, CampaignType.TPL_OUTLET, null, null, MessageType.DISABLED.name(), null);
        buildBanner(false, null, null, CampaignType.TPL_PARTNER, null, null, MessageType.INDEBTEDNESS.name(), null);
        var partnerDebtBanner =
                buildBanner(false, null, null, CampaignType.TPL_PARTNER, null, null, MessageType.DISABLED.name(), null);

        setLegalPartnerIndebtedness(partner, partnerDebtBanner, pickupPointDebtBanner);
        createPartnerDebtDisabled(partner);

        var pickupPointBanners = bannerInformationQueryService.getPickupPointBanners(pickupPoint.getPvzMarketId(), "");
        var partnerBanners = bannerInformationQueryService.getLegalPartnerBanners(partner.getPartnerId(), "");
        assertThat(pickupPointBanners.size()).isEqualTo(1);
        assertThat(partnerBanners.size()).isEqualTo(1);

        var expectedPickupPointBanner = setDateTimeShowPrevious(
                pickupPointDebtBanner, clock.instant().toString()
        );
        var expectedPartnerBanner = setDateTimeShowPrevious(partnerDebtBanner, clock.instant().toString());
        assertThat(expectedPickupPointBanner).isEqualTo(pickupPointBanners.get(0));
        assertThat(expectedPartnerBanner).isEqualTo(partnerBanners.get(0));

        bannerCommandService.pickupPointCloseBanner(pickupPoint.getPvzMarketId(), MessageType.DISABLED,
                DisplayType.POPUP);
        clock.setFixed(Instant.now(clock).plusSeconds(600), ZoneOffset.ofHours(0));
        pickupPointBanners = bannerInformationQueryService.getPickupPointBanners(pickupPoint.getPvzMarketId(), "");
        partnerBanners = bannerInformationQueryService.getLegalPartnerBanners(partner.getPartnerId(), "");
        assertThat(pickupPointBanners.size()).isEqualTo(1);
        assertThat(partnerBanners.size()).isEqualTo(1);
    }

    private void setLegalPartnerIndebtedness(
            LegalPartner partner, BannerInformationParams partnerBanner, BannerInformationParams pickupPointBanner
    ) {
        BigDecimal debtSum = new BigDecimal(44000);
        LegalPartnerIndebtednessParams legalPartnerIndebtednessParams = LegalPartnerIndebtednessParams.builder()
                .legalPartnerId(partner.getId())
                .debtSum(debtSum)
                .debtDate(LocalDate.now(clock))
                .build();
        legalPartnerIndebtednessCommandService.updateIndebtedness(legalPartnerIndebtednessParams);
        partnerBanner.setBody(String.format(partnerBanner.getBody(), debtSum));
        pickupPointBanner.setBody(String.format(pickupPointBanner.getBody(), debtSum));
    }

    private void createPartnerDebtDisabled(LegalPartner partner) {
        terminationFactory.createLegalPartnerTermination(
                TestLegalPartnerTerminationFactory.LegalPartnerTestParamsBuilder.builder()
                        .params(
                                TestLegalPartnerTerminationFactory.LegalPartnerTerminationTestParams
                                        .builder()
                                        .type(LegalPartnerTerminationType.DEBT)
                                        .fromTime(OffsetDateTime.now(clock))
                                        .legalPartnerId(partner.getId())
                                        .build()
                        )
                        .build()
        );
    }

    @ParameterizedTest
    @MethodSource("getGreetingData")
    void checkGreetingBanner(
            PickupPointBrandingType brandingType, BannerCampaignFeatures feature, MessageType messageType
    ) {
        clock.setFixed(Instant.parse("2021-12-06T04:01:00Z"), ZoneOffset.ofHours(0));
        var partner = legalPartnerFactory.createLegalPartner();
        var timeOffset = 5;
        pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                brandingType, false, partner, timeOffset
        );

        var banner = buildBanner(
                true, "0 0 8 * * ? *", 1440, CampaignType.TPL_PARTNER, List.of(feature.name()), null,
                messageType.name(), null
        );
        var banners = bannerInformationQueryService.getLegalPartnerBanners(partner.getPartnerId(), "");
        assertThat(banners.size()).isEqualTo(1);
        var expectedBanner = setDateTimeShowPrevious(banner, "2021-12-06T03:00:00Z");
        assertThat(expectedBanner).isEqualTo(banners.get(0));
        clock.setFixed(Instant.now(clock).plusSeconds(600), ZoneOffset.ofHours(0));
        banners = bannerInformationQueryService.getLegalPartnerBanners(partner.getPartnerId(), "");
        assertThat(banners.size()).isEqualTo(1);

        banner = bannerInformationFactory.parseFrequency(
                banner, OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.ofHours(timeOffset))
        );
    }

    private static Stream<Arguments> getGreetingData() {
        return Stream.of(
                Arguments.of(
                        PickupPointBrandingType.FULL, BannerCampaignFeatures.BRANDED, MessageType.BRANDED_GREETING
                ),
                Arguments.of(
                        PickupPointBrandingType.NONE, BannerCampaignFeatures.NOT_BRANDED, MessageType.MULTI_GREETING
                )
        );
    }

    @ParameterizedTest
    @MethodSource("getDeactivationData")
    void checkDeactivationBanner(MessageType messageType, ChangeActiveReason reason) {
        clock.setFixed(Instant.parse("2021-12-06T04:01:00Z"), ZoneOffset.ofHours(0));
        configurationGlobalCommandService.setValue(DAYS_FOR_DEACTIVATION_BANNER, 999999);
        var partner = legalPartnerFactory.createLegalPartner();
        var timeOffset = 5;
        var pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.FULL, false, partner, timeOffset
        );

        var banner = bannerInformationFactory.createDeactivationBanner(messageType);

        pickupPointChangeActiveCommandService.saveToChangeLog(Collections.singletonList(pickupPoint.getPvzMarketId()),
                LocalDate.now(clock), reason, ChangeActiveType.DEACTIVATE);
        pickupPointChangeActiveCommandService.changeActive();

        var banners = bannerInformationQueryService.getLegalPartnerBanners(partner.getPartnerId(), "");
        assertThat(banners.size()).isEqualTo(1);
        var expectedBanner = setDateTimeShowPrevious(banner, "2021-12-06T04:00:00Z");
        expectedBanner.setBody(String.format(expectedBanner.getBody(), pickupPoint.getName()));
        assertThat(expectedBanner).isEqualTo(banners.get(0));

        var pickupPoint2 = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.FULL, false, partner, timeOffset
        );
        pickupPointChangeActiveCommandService.saveToChangeLog(Collections.singletonList(pickupPoint2.getPvzMarketId()),
                LocalDate.now(clock), reason, ChangeActiveType.DEACTIVATE);
        pickupPointChangeActiveCommandService.changeActive();

        banners = bannerInformationQueryService.getLegalPartnerBanners(partner.getPartnerId(), "");
        assertThat(banners.size()).isEqualTo(1);
        expectedBanner = setDateTimeShowPrevious(banner, "2021-12-06T04:00:00Z");
        expectedBanner.setBody(
                String.format(expectedBanner.getBody(), pickupPoint.getName() + ", " + pickupPoint2.getName())
        );
        assertThat(expectedBanner).isEqualTo(banners.get(0));

        banner = bannerInformationFactory.parseFrequency(
                banner, OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.ofHours(timeOffset))
        );
    }

    private static Stream<Arguments> getDeactivationData() {
        return Stream.of(
                Arguments.of(MessageType.DEACTIVATION_NEXT_TO_BRAND, ChangeActiveReason.NEXT_TO_BRAND),
                Arguments.of(MessageType.DEACTIVATION_UNPROFITABLE, ChangeActiveReason.UNPROFITABLE)
        );
    }

    @Test
    void checkDeactivationBanner() {
        clock.setFixed(Instant.parse("2021-12-06T04:01:00Z"), ZoneOffset.ofHours(0));
        configurationGlobalCommandService.setValue(DAYS_FOR_DEACTIVATION_BANNER, 999999);
        configurationGlobalCommandService.setValue(DEACTIVATION_WITH_REASONS, true);
        var partner = legalPartnerFactory.createLegalPartner();
        var timeOffset = 5;
        var pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.FULL, false, partner, timeOffset
        );

        var banner = bannerInformationFactory.createDeactivationBanner(MessageType.DEACTIVATION);
        var deactivationReason = deactivationReasonCommandService.createDeactivationReason(REASON_NAME,
                REASON_DETAILS, true, true, null);

        pickupPointDeactivationCommandService.deactivate(pickupPoint.getPvzMarketId(),
                deactivationReason.getId(), LocalDate.now(clock));

        var banners = bannerInformationQueryService.getLegalPartnerBanners(partner.getPartnerId(), "");
        assertThat(banners.size()).isEqualTo(1);
        var expectedBanner = setDateTimeShowPrevious(banner, "2021-12-06T04:00:00Z");
        expectedBanner.setBody(String.format(expectedBanner.getBody(), pickupPoint.getName()));
        assertThat(expectedBanner).isEqualTo(banners.get(0));

        banner = bannerInformationFactory.parseFrequency(
                banner, OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.ofHours(timeOffset))
        );
    }

    @Test
    void checkNotClosableBanner() {
        String now = "2021-12-05T12:10:00Z";
        clock.setFixed(Instant.parse(now), ZoneOffset.ofHours(0));

        int timeOffset = 5;
        var pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.NONE, false, null, timeOffset
        );
        var banner =
                buildBanner(false, null, null, CampaignType.TPL_OUTLET, null, null, UUID.randomUUID().toString(), null);

        compareBanners(pickupPoint, banner, now);

        banner = bannerInformationFactory.parseFrequency(
                banner, OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.ofHours(timeOffset))
        );
        closeBanner(
                banner.getDisplayType().name(), banner.getMessageType(), LocalDate.now(clock),
                banner.getId(), Timestamp.from(banner.getDateTimeShowPrevious())
        );

        compareBanners(pickupPoint, banner, now);
    }

    private void compareBanners(PickupPoint pickupPoint, BannerInformationParams banner, String now) {
        var banners = bannerInformationQueryService.getPickupPointBanners(pickupPoint.getPvzMarketId(), "");
        assertThat(banners.size()).isEqualTo(1);
        var actualBanner = banners.get(0);
        var expectedBanner = setDateTimeShowPrevious(banner, now);
        assertThat(actualBanner).isEqualTo(expectedBanner);
    }

    private void closeBanner(
            String displayType, String messageType, LocalDate date, Long bannerInformationId, Timestamp startShowTime
    ) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("display_type", displayType);
        paramMap.put("message_type", messageType);
        paramMap.put("date", date);
        paramMap.put("banner_information_id", bannerInformationId);
        paramMap.put("start_show_time", startShowTime);
        paramMap.put("closed_at", startShowTime);
        String sql = "INSERT INTO banner " +
                "    (display_type, message_type, date, banner_information_id, " +
                "    start_show_time, closed_at, created_at, updated_at) " +
                "VALUES " +
                "    (:display_type, :message_type, :date, :banner_information_id, " +
                "    :start_show_time, :closed_at, now(), now()) ";
        jdbcTemplate.update(sql, paramMap);
    }

    @Test
    void checkClosableBanner() {
        String now = "2021-12-05T12:10:00Z";
        clock.setFixed(Instant.parse(now), ZoneOffset.ofHours(0));

        int timeOffset = 5;
        var pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.NONE, false, null, timeOffset
        );
        var banner =
                buildBanner(true, null, null, CampaignType.TPL_OUTLET, null, null, UUID.randomUUID().toString(), null);

        compareBanners(pickupPoint, banner, now);

        banner = bannerInformationFactory.parseFrequency(
                banner, OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.ofHours(timeOffset))
        );
    }

    @Test
    void checkBannersCustomType() {
        clock.setFixed(Instant.parse("2021-12-06T03:59:00Z"), ZoneOffset.ofHours(0));

        var page = frontendPageRepository.save(buildPage());
        var page2 = frontendPageRepository.save(buildPage());
        var banner = createBannerForNotBrandedCampaignWithPages(page);
        createBannerForBrandedAndDropOffCampaignWithoutPages();

        var partner = legalPartnerFactory.createLegalPartner();
        var pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.NONE, false, partner, 5
        );

        checkPickupPointBannersByPageAndCurrentTime(
                banner, pickupPoint.getPvzMarketId(), page.getPageName(), "2021-12-06T03:59:00Z", "2021-12-06T03:50:00Z"
        );

        var bannersPickupPoint = bannerInformationQueryService.getPickupPointBanners(
                pickupPoint.getPvzMarketId(), page2.getPageName()
        );
        assertThat(bannersPickupPoint.size()).isZero();

        checkPickupPointBannersByPageAndCurrentTime(
                banner, pickupPoint.getPvzMarketId(), page.getPageName(), "2021-12-06T04:25:00Z", "2021-12-06T04:20:00Z"
        );

        var bannerPartners = bannerInformationQueryService.getLegalPartnerBanners(
                partner.getPartnerId(), null
        );
        assertThat(bannerPartners.size()).isZero();
    }

    private BannerInformationParams createBannerForNotBrandedCampaignWithPages(FrontendPage page) {
        List<String> features = new ArrayList<>();
        features.add(BannerCampaignFeatures.NOT_BRANDED.name());
        List<Long> pageIds = new ArrayList<>();
        pageIds.add(page.getId());
        pageIds.add(page.getId());
        return buildBanner(
                true, "0 */10 * * * ? *", 10, CampaignType.TPL_OUTLET, features, null, UUID.randomUUID().toString(), pageIds
        );
    }

    private void createBannerForBrandedAndDropOffCampaignWithoutPages() {
        List<String> features2 = new ArrayList<>();
        features2.add(BannerCampaignFeatures.BRANDED.name());
        features2.add(BannerCampaignFeatures.DROP_OFF.name());
        buildBanner(
                false, "0 */20 * * * ? *", 20, CampaignType.TPL_PARTNER, features2, null, UUID.randomUUID().toString(), null
        );
    }

    private void checkPickupPointBannersByPageAndCurrentTime(
            BannerInformationParams existingBanner, Long pvzMarketId, String pageName,
            String currTime, String dateTimeShowPreviousExpected
    ) {
        clock.setFixed(Instant.parse(currTime), ZoneOffset.ofHours(0));
        var bannersPickupPoint = bannerInformationQueryService.getPickupPointBanners(
                pvzMarketId, pageName
        );
        assertThat(bannersPickupPoint.size()).isEqualTo(1);
        var expectedBanner = setDateTimeShowPrevious(existingBanner, dateTimeShowPreviousExpected);
        assertThat(bannersPickupPoint.get(0)).isEqualTo(expectedBanner);
    }

    @Test
    void checkBannersCustomTypeWithoutPages() {
        clock.setFixed(Instant.parse("2021-12-06T03:59:00Z"), ZoneOffset.ofHours(0));

        var partner = legalPartnerFactory.createLegalPartner();
        var pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.FULL, false, partner, 3
        );
        var pickupPoint2 = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.NONE, true, partner, 6
        );

        var banner = createBannerForPartnerWithoutPages(List.of(BannerCampaignFeatures.BRANDED.name()));
        var banner2 = createBannerForPartnerWithoutPages(List.of(BannerCampaignFeatures.DROP_OFF.name()));

        var banner3 = createBannerForPicupPointWithoutPagesAndWithCampaignIds(
                List.of(BannerCampaignFeatures.BRANDED.name()), pickupPoint2.getId()
        );
        var banner4 = createBannerForPicupPointWithoutPagesAndWithCampaignIds(List.of(), pickupPoint2.getId());
        var banner5 = createBannerForPicupPointWithoutPagesAndWithCampaignIds(List.of(), null);

        var page = frontendPageRepository.save(buildPage());

        var partnerBanners = bannerInformationQueryService.getLegalPartnerBanners(
                partner.getPartnerId(), page.getPageName()
        );
        assertThat(partnerBanners.size()).isEqualTo(2);

        List<BannerInformationParams> expected = new ArrayList<>();
        expected.add(setDateTimeShowPrevious(banner, "2021-12-06T03:59:00Z"));
        expected.add(setDateTimeShowPrevious(banner2, "2021-12-06T03:59:00Z"));
        assertThat(expected).isEqualTo(partnerBanners);

        var pickupPointBanners = bannerInformationQueryService.getPickupPointBanners(
                pickupPoint.getPvzMarketId(), page.getPageName()
        );
        assertThat(pickupPointBanners.size()).isEqualTo(2);
        List<BannerInformationParams> expected2 = new ArrayList<>();
        expected2.add(setDateTimeShowPrevious(banner3, "2021-12-06T03:30:00Z"));
        expected2.add(setDateTimeShowPrevious(banner5, "2021-12-06T03:30:00Z"));
        assertThat(expected2).isEqualTo(pickupPointBanners);

        pickupPointBanners = bannerInformationQueryService.getPickupPointBanners(
                pickupPoint2.getPvzMarketId(), page.getPageName()
        );
        assertThat(pickupPointBanners.size()).isEqualTo(3);
        expected2 = new ArrayList<>();
        expected2.add(setDateTimeShowPrevious(banner3, "2021-12-06T03:30:00Z"));
        expected2.add(setDateTimeShowPrevious(banner4, "2021-12-06T03:30:00Z"));
        expected2.add(setDateTimeShowPrevious(banner5, "2021-12-06T03:30:00Z"));
    }

    private BannerInformationParams createBannerForPartnerWithoutPages(List<String> features) {
        return buildBanner(
                false, "0 */20 * * * ? *", 20, CampaignType.TPL_PARTNER, features, null, UUID.randomUUID().toString(), null
        );
    }

    private BannerInformationParams createBannerForPicupPointWithoutPagesAndWithCampaignIds(
            List<String> features, Long pickupPointId
    ) {
        List<Long> campaignIds = new ArrayList<>();
        if (pickupPointId != null) {
            campaignIds.add(pickupPointId);
        }
        return buildBanner(
                true, "0 */30 * * * ? *", 30, CampaignType.TPL_OUTLET, features, campaignIds, UUID.randomUUID().toString(), null
        );
    }

    private BannerInformationParams setDateTimeShowPrevious(
            BannerInformationParams banner, String dateTimeShowPrevious
    ) {
        banner.setNeedShow(true);
        banner.setDateTimeShowPrevious(Instant.parse(dateTimeShowPrevious));
        return banner;
    }

    private BannerInformation buildBannerInformationForFrequencyExpressionCheck(
            boolean closable, String frequency, Integer durationInMinutes
    ) {
        BannerInformationParams banner = bannerInformationFactory.create(
                TestBannerInformationFactory.BannerInformationTestParams.builder()
                        .closable(closable)
                        .frequency(frequency)
                        .durationInMinutes(durationInMinutes)
                        .messageType(UUID.randomUUID().toString())
                        .build()
        );
        return bannerInformationParamsMapper.map(banner);
    }

    private BannerInformationParams buildBanner(
            boolean closable, String frequency, Integer durationInMinutes, CampaignType campaignType,
            List<String> bannerCampaignFeatures, List<Long> campaignIds, String messageType,
            List<Long> bannerPageIds
    ) {
        return bannerInformationFactory.create(
                TestBannerInformationFactory.BannerInformationTestParams.builder()
                        .closable(closable)
                        .frequency(frequency)
                        .durationInMinutes(durationInMinutes)
                        .campaignType(campaignType)
                        .bannerCampaignFeatures(bannerCampaignFeatures)
                        .campaignIds(campaignIds)
                        .messageType(messageType)
                        .bannerPageIds(bannerPageIds)
                        .build()
        );
    }

    private FrontendPage buildPage() {
        return FrontendPage.builder()
                .pageName(UUID.randomUUID().toString())
                .description("описание для л2")
                .build();
    }

    @Test
    void checkNewPartnerSurveyBanner() {
        clock.setFixed(Instant.parse("2021-12-06T03:59:00Z"), ZoneOffset.ofHours(0));

        var newPartnerSurveyBanner = buildNewPartnerSurveyBanner();
        var expectedBanner = setDateTimeShowPrevious(newPartnerSurveyBanner, "2021-12-06T03:00:00Z");
        buildInvalidPartnerSurveyBanner();

        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();
        createSurveyPartner(legalPartner, PartnerSurveyStatus.REGISTERED);

        var partnerBanners = bannerInformationQueryService.getLegalPartnerBanners(
                legalPartner.getPartnerId(), null
        );
        assertThat(partnerBanners).hasSize(1);
        assertThat(partnerBanners).isEqualTo(List.of(expectedBanner));
    }

    @Test
    void checkInvalidPartnerSurveyBanner() {
        clock.setFixed(Instant.parse("2021-12-06T03:59:00Z"), ZoneOffset.ofHours(0));

        buildNewPartnerSurveyBanner();
        var invalidPartnerSurveyBanner = buildInvalidPartnerSurveyBanner();
        var expectedBanner = setDateTimeShowPrevious(invalidPartnerSurveyBanner, "2021-12-06T03:00:00Z");


        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();
        createSurveyPartner(legalPartner, PartnerSurveyStatus.WAITING_RESPONSE);

        var partnerBanners = bannerInformationQueryService.getLegalPartnerBanners(
                legalPartner.getPartnerId(), null
        );
        assertThat(partnerBanners).hasSize(1);
        assertThat(partnerBanners).isEqualTo(List.of(expectedBanner));
    }

    @Test
    void checkTwoPartnerSurveyBanner() {
        clock.setFixed(Instant.parse("2021-12-06T03:59:00Z"), ZoneOffset.ofHours(0));

        var newPartnerSurveyBanner = buildNewPartnerSurveyBanner();
        var expectedBanner = setDateTimeShowPrevious(newPartnerSurveyBanner, "2021-12-06T03:00:00Z");
        buildInvalidPartnerSurveyBanner();

        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();
        createSurveyPartner(legalPartner, PartnerSurveyStatus.REGISTERED);
        createSurveyPartner(legalPartner, PartnerSurveyStatus.WAITING_RESPONSE);

        var partnerBanners = bannerInformationQueryService.getLegalPartnerBanners(
                legalPartner.getPartnerId(), null
        );
        assertThat(partnerBanners).hasSize(1);
        assertThat(partnerBanners).isEqualTo(List.of(expectedBanner));
    }

    private BannerInformationParams buildNewPartnerSurveyBanner() {
        return buildBanner(
                true, "0 0 0/3 ? * * *", 180, CampaignType.TPL_PARTNER, null, null,
                MessageType.NEW_PARTNER_SURVEY.name(), null
        );
    }

    private BannerInformationParams buildInvalidPartnerSurveyBanner() {
        return buildBanner(
                true, "0 0 0/3 ? * * *", 180, CampaignType.TPL_PARTNER, null, null,
                MessageType.INVALID_PARTNER_SURVEY.name(), null
        );
    }

    private void createSurveyPartner(LegalPartner legalPartner, PartnerSurveyStatus owStatus) {
        SurveyParams survey = surveyFactory.create();
        surveyPartnerCommandService.create(
                SurveyPartnerParams.builder()
                        .surveyId(survey.getId())
                        .legalPartnerId(legalPartner.getId())
                        .showDateTime(clock.instant())
                        .owId(1234L)
                        .owStatus(owStatus)
                        .build()
        );
    }

}
