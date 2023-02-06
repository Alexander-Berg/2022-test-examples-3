package ru.yandex.market.pvz.internal.controller.pi.pickup_point;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.client.model.pickup_point.PickupPointBrandingType;
import ru.yandex.market.pvz.core.domain.banner.Banner;
import ru.yandex.market.pvz.core.domain.banner.BannerPickupPointRepository;
import ru.yandex.market.pvz.core.domain.banner.BannerRepository;
import ru.yandex.market.pvz.core.domain.banner_information.BannerInformationParams;
import ru.yandex.market.pvz.core.domain.banner_information.CampaignType;
import ru.yandex.market.pvz.core.domain.banner_information.frontend_page.FrontendPage;
import ru.yandex.market.pvz.core.domain.banner_information.frontend_page.FrontendPageRepository;
import ru.yandex.market.pvz.core.domain.configuration.pickup_point.ConfigurationPickupPointCommandService;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartner;
import ru.yandex.market.pvz.core.domain.legal_partner.indebtedness.LegalPartnerIndebtednessCommandService;
import ru.yandex.market.pvz.core.domain.legal_partner.indebtedness.LegalPartnerIndebtednessParams;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointFeature;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointRepository;
import ru.yandex.market.pvz.core.domain.pickup_point.calendar.override.PickupPointCalendarManager;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.DeactivationReason;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.DeactivationReasonRepository;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.PickupPointDeactivationLog;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.PickupPointDeactivationLogRepository;
import ru.yandex.market.pvz.core.test.factory.TestBannerInformationFactory;
import ru.yandex.market.pvz.core.test.factory.TestBrandRegionFactory;
import ru.yandex.market.pvz.core.test.factory.TestDeliveryServiceFactory;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.internal.BaseShallowTest;
import ru.yandex.market.pvz.internal.WebLayerTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pvz.core.TestUtils.getFileContent;
import static ru.yandex.market.pvz.core.domain.pickup_point.PickupPointFeature.DBS;
import static ru.yandex.market.pvz.core.domain.pickup_point.PickupPointFeature.FASHION;
import static ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointScheduleDayTestParams.DEFAULT_TIME_FROM;
import static ru.yandex.market.tpl.common.util.StringFormatter.sf;

@WebLayerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PickupPointControllerTest extends BaseShallowTest {

    private static final LocalDate DAY = LocalDate.of(2021, 2, 15);

    private final TestableClock clock;

    private final TestDeliveryServiceFactory deliveryServiceFactory;
    private final TestLegalPartnerFactory legalPartnerFactory;
    private final TestBrandRegionFactory brandRegionFactory;
    private final TestPickupPointFactory pickupPointFactory;

    private final DeactivationReasonRepository deactivationReasonRepository;
    private final PickupPointDeactivationLogRepository pickupPointDeactivationLogRepository;
    private final PickupPointRepository pickupPointRepository;
    private final TestBannerInformationFactory bannerInformationFactory;
    private final LegalPartnerIndebtednessCommandService legalPartnerIndebtednessCommandService;
    private final FrontendPageRepository frontendPageRepository;
    private final BannerRepository bannerRepository;
    private final BannerPickupPointRepository bannerPickupPointRepository;
    private final ConfigurationPickupPointCommandService configurationPickupPointCommandService;

    @MockBean
    private PickupPointCalendarManager calendarManager;

    @Test
    void getStatusInfo() throws Exception {
        when(calendarManager.isWorkingTime(any(), any())).thenReturn(true);
        PickupPoint pickupPoint = createActivePickupPoint();
        ZoneOffset zone = ZoneOffset.ofHours(pickupPoint.getTimeOffset());
        LocalDateTime now = LocalDateTime.of(DAY, LocalTime.of(DEFAULT_TIME_FROM.plusHours(2).getHour(), 0));
        clock.setFixed(now.atZone(zone).toInstant(), zone);

        BannerInformationParams banner = bannerInformationFactory.createStartShiftBanner();
        var expected = String.format(
                getFileContent("pickup_point/response_get_status_info.json"),
                banner.getTitle(), banner.getBody(), banner.getButtonText()
        );

        mockMvc.perform(
                get("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(expected, false));
    }

    @Test
    void whenBannersEmpty() throws Exception {
        var pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.NONE, false, null, 5
        );

        mockMvc.perform(
                get("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent(
                        "pickup_point/banner_information/response_empty.json"), true));
    }

    @Test
    void checkStartShiftPopupAndCustomBanner() throws Exception {
        when(calendarManager.isWorkingTime(any(), any())).thenReturn(true);
        clock.setFixed(Instant.parse("2021-12-06T04:01:00Z"), ZoneOffset.ofHours(0));
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.FULL, false, null, 5
        );

        var page = frontendPageRepository.save(buildPage());
        var bannerCustom =
                buildBanner(false, null, null, CampaignType.TPL_OUTLET, null, null, "Тест", List.of(page.getId()));
        BannerInformationParams banner = bannerInformationFactory.createStartShiftBanner();

        var expected = String.format(
                getFileContent("pickup_point/banner_information/response_start_shift_custom.json"),
                bannerCustom.getMessageType(), bannerCustom.getTitle(), bannerCustom.getBody(),
                banner.getTitle(), banner.getBody(), banner.getButtonText()
        );
        mockMvc.perform(
                get("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("pageId", page.getPageName()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(expected, false));
    }

    @Test
    void checkCustomBannerSeveralTimes() throws Exception {
        clock.setFixed(Instant.parse("2021-12-06T04:01:00Z"), ZoneOffset.ofHours(0));
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.FULL, false, null, 5
        );

        var page = frontendPageRepository.save(buildPage());
        var bannerCustom = buildBanner(
                true, "0 0 0/3 * * ? *", 180, CampaignType.TPL_OUTLET, null, null, "Тест", Collections.emptyList()
        );

        var expected = String.format(
                getFileContent("pickup_point/banner_information/response_custom.json"),
                bannerCustom.getMessageType(), bannerCustom.getTitle(), bannerCustom.getBody()
        );
        mockMvc.perform(
                        get("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() + "/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("pageId", page.getPageName()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(expected, false));
        var bannerIds = bannerRepository.findAll().stream().map(Banner::getId).collect(Collectors.toList());
        assertThat(bannerIds).hasSize(1);

        mockMvc.perform(
                        get("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() + "/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("pageId", page.getPageName()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(expected, false));
        bannerIds = bannerRepository.findAll().stream().map(Banner::getId).collect(Collectors.toList());
        assertThat(bannerIds).hasSize(1);

        clock.setFixed(Instant.parse("2021-12-06T10:01:00Z"), ZoneOffset.ofHours(0));
        mockMvc.perform(
                        get("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() + "/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("pageId", page.getPageName()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(expected, false));
        bannerIds = bannerRepository.findAll().stream().map(Banner::getId).collect(Collectors.toList());
        assertThat(bannerIds).hasSize(2);
    }

    @Test
    void checkCloseCustomBanner() throws Exception {
        clock.setFixed(Instant.parse("2021-12-06T04:01:00Z"), ZoneOffset.ofHours(0));
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.FULL, false, null, 5
        );

        var page = frontendPageRepository.save(buildPage());
        var bannerCustom = buildBanner(
                true, "0 0 */3 * * ? *", 180, CampaignType.TPL_OUTLET, null, null, "Тест", Collections.emptyList()
        );

        var expected = String.format(
                getFileContent("pickup_point/banner_information/response_custom.json"),
                bannerCustom.getMessageType(), bannerCustom.getTitle(), bannerCustom.getBody()
        );
        mockMvc.perform(
                        get("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() + "/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("pageId", page.getPageName()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(expected, false));
        var bannerIds = bannerRepository.findAll().stream().map(Banner::getId).collect(Collectors.toList());

        mockMvc.perform(
                        put("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() + "/status/close")
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("id", String.valueOf(bannerIds.get(0)))
                )
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(
                        get("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() + "/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("pageId", page.getPageName()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(
                        getFileContent("pickup_point/banner_information/response_empty.json"), false)
                );

        PickupPoint pickupPoint2 = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.FULL, false, null, 5
        );
        mockMvc.perform(
                        get("/v1/pi/pickup-points/" + pickupPoint2.getPvzMarketId() + "/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("pageId", page.getPageName()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(expected, false));
    }

    @Test
    void checkIndebtednessBanner() throws Exception {
        clock.setFixed(Instant.now(), ZoneOffset.ofHours(3));
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.FULL, false, legalPartner, 3
        );

        BannerInformationParams pickupPointBanner =
                bannerInformationFactory.createIndebtednessBanner(CampaignType.TPL_OUTLET);
        setLegalPartnerIndebtedness(legalPartner, pickupPointBanner);

        ResultActions result = mockMvc.perform(
                get("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON));

        pickupPointBanner = bannerInformationFactory.parseFrequency(pickupPointBanner, OffsetDateTime.now(clock));
        Banner banner = bannerRepository.findBannerByBannerInformationIdAndStartShowTime(
                pickupPointBanner.getId(), pickupPointBanner.getDateTimeShowPrevious()
        );
        var expected = String.format(
                getFileContent("pickup_point/banner_information/response_indebtedness.json"),
                banner.getId(), pickupPointBanner.getTitle(), pickupPointBanner.getBody()
        );
        result
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(expected, true));

        assertThat(bannerPickupPointRepository.existsById(banner.getId())).isEqualTo(true);
    }

    private void setLegalPartnerIndebtedness(
            LegalPartner partner, BannerInformationParams pickupPointBanner
    ) {
        BigDecimal debtSum = new BigDecimal(44000);
        LegalPartnerIndebtednessParams legalPartnerIndebtednessParams = LegalPartnerIndebtednessParams.builder()
                .legalPartnerId(partner.getId())
                .debtSum(debtSum)
                .debtDate(LocalDate.now(clock))
                .build();
        legalPartnerIndebtednessCommandService.updateIndebtedness(legalPartnerIndebtednessParams);
        pickupPointBanner.setBody(String.format(pickupPointBanner.getBody(), debtSum));
    }

    private FrontendPage buildPage() {
        return FrontendPage.builder()
                .pageName(UUID.randomUUID().toString())
                .description("описание для л2")
                .build();
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
                        .buttonText(null)
                        .build()
        );
    }

    private PickupPoint createActivePickupPoint() {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint();
        return pickupPointFactory.updatePickupPoint(
                pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .active(true)
                        .build()
        );
    }

    @Test
    void updateSensitive() throws Exception {
        var deliveryService = deliveryServiceFactory.createNotSetupDeliveryService();
        var legalPartner = legalPartnerFactory.createLegalPartner(TestLegalPartnerFactory.LegalPartnerTestParamsBuilder
                .builder()
                .deliveryService(deliveryService)
                .build());
        var pickupPoint = pickupPointFactory.createPickupPointFromCrm(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .legalPartner(legalPartner)
                        .build());

        mockMvc.perform(
                put("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                getFileContent("pickup_point/request_update_sensitive_from_outlet.json"),
                                pickupPoint.getPvzMarketId())))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(
                        getFileContent("pickup_point/response_update_sensitive_from_outlet.json"),
                        pickupPoint.getId(),
                        pickupPoint.getPvzMarketId(),
                        deliveryService.getId(),
                        legalPartner.getId()), true));
    }

    @Test
    void tryToUpdateSensitiveNotExistentPickupPoint() throws Exception {
        var pickupPoint = pickupPointFactory.createPickupPoint();

        mockMvc.perform(
                put("/v1/pi/pickup-points/" + (pickupPoint.getPvzMarketId() + 1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                getFileContent("pickup_point/request_update_sensitive_from_outlet.json"),
                                pickupPoint.getPvzMarketId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void tryToUpdateSensitivePickupPointWithoutApprovedSubmission() throws Exception {
        var pickupPoint = pickupPointFactory.createPickupPoint();

        mockMvc.perform(
                put("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                getFileContent("pickup_point/request_update_sensitive_from_outlet.json"),
                                pickupPoint.getPvzMarketId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateInsensitive() throws Exception {
        var deliveryService = deliveryServiceFactory.createNotSetupDeliveryService();
        var legalPartner = legalPartnerFactory.createLegalPartner(TestLegalPartnerFactory.LegalPartnerTestParamsBuilder
                .builder()
                .deliveryService(deliveryService)
                .build());
        var pickupPoint = pickupPointFactory.createPickupPointFromCrm
                (TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .legalPartner(legalPartner)
                        .build());

        mockMvc.perform(
                patch("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                getFileContent("pickup_point/request_update_insensitive_from_outlet.json"),
                                pickupPoint.getPvzMarketId())))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(
                        getFileContent("pickup_point/response_update_insensitive_from_outlet.json"),
                        pickupPoint.getId(),
                        pickupPoint.getPvzMarketId(),
                        deliveryService.getId(),
                        legalPartner.getId()), true));
    }

    @Test
    void tryToUpdateInsensitiveNotExistentPickupPoint() throws Exception {
        var pickupPoint = pickupPointFactory.createPickupPoint();

        mockMvc.perform(
                patch("/v1/pi/pickup-points/" + (pickupPoint.getPvzMarketId() + 1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                getFileContent("pickup_point/request_update_insensitive_from_outlet.json"),
                                pickupPoint.getPvzMarketId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void tryToUpdateInsensitivePickupPointWithoutApprovedSubmission() throws Exception {
        var pickupPoint = pickupPointFactory.createPickupPoint();

        mockMvc.perform(
                patch("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                getFileContent("pickup_point/request_update_insensitive_from_outlet.json"),
                                pickupPoint.getPvzMarketId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getFashionAndDbsFeatures() throws Exception {
        brandRegionFactory.createDefaults();
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .params(TestPickupPointFactory.PickupPointTestParams.builder()
                                .brandingType(PickupPointBrandingType.FULL)
                                .build())
                        .build()
        );

        mockMvc.perform(get(sf("/v1/pi/pickup-points/{}/flags", pickupPoint.getPvzMarketId()))
                        .param("features", FASHION.name(), DBS.name()))
                .andExpect(content().json(getFileContent("pickup_point/response_fashion_and_dbs_feature.json")));
    }

    @ParameterizedTest
    @EnumSource(value = PickupPointBrandingType.class, names = {"PARTIAL", "FULL"}, mode = EnumSource.Mode.EXCLUDE)
    void getNoFeaturesUnbrandedPP(PickupPointBrandingType brandingType) throws Exception {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPoint(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .params(TestPickupPointFactory.PickupPointTestParams.builder()
                                .brandingType(brandingType)
                                .active(true)
                                .build())
                        .build());

        mockMvc.perform(get(sf("/v1/pi/pickup-points/{}/flags", pickupPoint.getPvzMarketId()))
                        .param("features", FASHION.name(), DBS.name()))
                .andExpect(content().json(getFileContent("pickup_point/response_no_features.json")));
    }

    @SneakyThrows
    @ParameterizedTest
    @EnumSource(value = PickupPointFeature.class, names = {"DBS", "E_TRANSFER_ACT"})
    void getFeatureFromConfig(PickupPointFeature feature) {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm(
                TestPickupPointFactory.CreatePickupPointBuilder.builder().build()
        );
        String featureName = feature.name();
        configurationPickupPointCommandService.setValue(pickupPoint.getId(), featureName, true);

        mockMvc.perform(get(sf("/v1/pi/pickup-points/{}/flags", pickupPoint.getPvzMarketId()))
                        .param("features", featureName))
                .andExpect(content().json(sf("{'{}': true}", featureName)));
    }

    @Test
    void getDeactivations() throws Exception {
        var initialDate = LocalDateTime.of(2021, 11, 30, 11, 15);
        clock.setFixed(OffsetDateTime.of(initialDate, ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
        PickupPoint pickupPoint = createActivePickupPointWithProperties(false, false, false, true, true, true);

        var firstDeactivation = pickupPointDeactivationLogRepository.findAll().iterator().next();

        var deactivationReason = DeactivationReason.builder()
                .reason("Причина")
                .details("Описание причины")
                .canBeCancelled(true)
                .fullDeactivation(true)
                .build();

        deactivationReason = deactivationReasonRepository.save(deactivationReason);

        var deactivationReasons = deactivationReasonRepository.findAll();

        var date = LocalDate.of(2021, 12, 1);

        var currentDeactivation = createDeactivation(pickupPoint.getPvzMarketId(),
                deactivationReason.getId(), date, null);
        var previousDeactivation = createDeactivation(pickupPoint.getPvzMarketId(),
                deactivationReason.getId(), date, date);

        pickupPointDeactivationLogRepository.save(currentDeactivation);
        pickupPointDeactivationLogRepository.save(previousDeactivation);

        mockMvc.perform(get("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() + "/change-active"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(
                        getFileContent("pickup_point/response_deactivation.json"),
                        deactivationReasons.get(0).getId(),
                        deactivationReasons.get(1).getId(),
                        currentDeactivation.getId(),
                        previousDeactivation.getId(),
                        firstDeactivation.getId()), true
                ));
    }

    @Test
    void deactivate() throws Exception {
        clock.setFixed(Instant.EPOCH, clock.getZone());
        PickupPoint pickupPoint = createActivePickupPointWithProperties(false, false, false, true, true, true);

        var deactivationReason = DeactivationReason.builder()
                .reason("Причина")
                .details("Описание причины")
                .canBeCancelled(true)
                .fullDeactivation(true)
                .build();

        deactivationReason = deactivationReasonRepository.save(deactivationReason);

        var deactivationReasons = deactivationReasonRepository.findAll();

        mockMvc.perform(post("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() + "/change-active")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(
                        getFileContent("pickup_point/request_deactivation_create.json"),
                        deactivationReason.getId(), LocalDate.now(clock))))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(
                        getFileContent("pickup_point/response_deactivation_create.json"),
                        deactivationReasons.get(1).getId(), deactivationReasons.get(0).getId(),
                        LocalDate.now(clock), LocalDate.now(clock))
                ));
    }

    @Test
    void cancelDeactivation() throws Exception {
        clock.setFixed(Instant.EPOCH, clock.getZone());
        PickupPoint pickupPoint = createActivePickupPointWithProperties(false, false, false, true, true, false);

        var firstDeactivation = pickupPointDeactivationLogRepository.findAll().iterator().next();

        mockMvc.perform(patch("/v1/pi/pickup-points/" + pickupPoint.getPvzMarketId() + "/change-active" +
                "?deactivationId=" + firstDeactivation.getId()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(
                        getFileContent("pickup_point/response_deactivation_cancel.json"),
                        firstDeactivation.getDeactivationReason().getId(),
                        firstDeactivation.getId(),
                        firstDeactivation.getDeactivationDate(),
                        LocalDate.now(clock)), true
                ));
    }

    private PickupPoint createActivePickupPointWithProperties(
            boolean cardAllowed, boolean cashAllowed, boolean returnAllowed, boolean prepayAllowed, boolean active,
            boolean activateImmediately
    ) {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .activateImmediately(activateImmediately)
                        .build()
        );
        return pickupPointFactory.updatePickupPoint(pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .cardAllowed(cardAllowed)
                        .cashAllowed(cashAllowed)
                        .returnAllowed(returnAllowed)
                        .prepayAllowed(prepayAllowed)
                        .active(active)
                        .build());
    }

    private PickupPointDeactivationLog createDeactivation(long pvzMarketId, long reasonId,
                                                          LocalDate deactivationDate, LocalDate activationDate) {
        var deactivation = PickupPointDeactivationLog.builder()
                .pickupPoint(pickupPointRepository.findByPvzMarketIdOrThrow(pvzMarketId))
                .deactivationReason(deactivationReasonRepository.findByIdOrThrow(reasonId))
                .deactivationDate(deactivationDate);

        if (activationDate != null) {
            return deactivation.activationDate(activationDate).build();
        }
        return deactivation.build();
    }
}
