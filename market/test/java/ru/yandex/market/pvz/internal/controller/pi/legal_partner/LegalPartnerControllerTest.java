package ru.yandex.market.pvz.internal.controller.pi.legal_partner;

import java.math.BigDecimal;
import java.net.URLConnection;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.client.model.partner.LegalPartnerTerminationType;
import ru.yandex.market.pvz.client.model.pickup_point.PickupPointBrandingType;
import ru.yandex.market.pvz.core.domain.banner.Banner;
import ru.yandex.market.pvz.core.domain.banner.BannerLegalPartnerRepository;
import ru.yandex.market.pvz.core.domain.banner.BannerRepository;
import ru.yandex.market.pvz.core.domain.banner.DisplayType;
import ru.yandex.market.pvz.core.domain.banner.MessageType;
import ru.yandex.market.pvz.core.domain.banner_information.BannerInformationParams;
import ru.yandex.market.pvz.core.domain.banner_information.CampaignType;
import ru.yandex.market.pvz.core.domain.banner_information.frontend_page.FrontendPage;
import ru.yandex.market.pvz.core.domain.banner_information.frontend_page.FrontendPageRepository;
import ru.yandex.market.pvz.core.domain.configuration.global.ConfigurationGlobalCommandService;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartner;
import ru.yandex.market.pvz.core.domain.legal_partner.indebtedness.LegalPartnerIndebtednessCommandService;
import ru.yandex.market.pvz.core.domain.legal_partner.indebtedness.LegalPartnerIndebtednessParams;
import ru.yandex.market.pvz.core.domain.legal_partner.offer.LegalPartnerOfferParams;
import ru.yandex.market.pvz.core.domain.legal_partner.offer.model.LegalPartnerOffer;
import ru.yandex.market.pvz.core.domain.legal_partner.offer.model.LegalPartnerOfferManager;
import ru.yandex.market.pvz.core.domain.order.OrderAdditionalInfoCommandService;
import ru.yandex.market.pvz.core.domain.order.OrderReportRepository;
import ru.yandex.market.pvz.core.domain.order.model.Order;
import ru.yandex.market.pvz.core.domain.order.model.OrderDeliveryType;
import ru.yandex.market.pvz.core.domain.order.model.OrderPaymentStatus;
import ru.yandex.market.pvz.core.domain.order.model.OrderPaymentType;
import ru.yandex.market.pvz.core.domain.order.model.OrderType;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointRepository;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.DeactivationReasonCommandService;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.PickupPointDeactivationCommandService;
import ru.yandex.market.pvz.core.test.factory.TestBannerInformationFactory;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerTerminationFactory;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory;
import ru.yandex.market.pvz.internal.BaseShallowTest;
import ru.yandex.market.pvz.internal.WebLayerTest;
import ru.yandex.market.tpl.common.db.exception.TplEntityNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pvz.core.TestUtils.getFileContent;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.DAYS_FOR_DEACTIVATION_BANNER;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.DEACTIVATION_WITH_REASONS;

@WebLayerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class LegalPartnerControllerTest extends BaseShallowTest {

    private static final long PARTNER_ID = 1;
    private static final long EXTERNAL_PARTNER_ID = 1000;
    private static final String FILE_INPUT_NAME = "file";
    private static final byte[] FILE_DATA = {4, 8, 15, 16, 23, 42};
    private static final String REASON_NAME = "Очередная причина деактивации";
    private static final String REASON_DETAILS = "Снова причины деактивации создаю";

    private final TestPreLegalPartnerFactory preLegalPartnerFactory;
    private final TestLegalPartnerFactory legalPartnerFactory;
    private final TestPickupPointFactory pickupPointFactory;
    private final TestOrderFactory testOrderFactory;

    private final OrderReportRepository orderReportRepository;
    private final PickupPointRepository pickupPointRepository;
    private final TestBannerInformationFactory bannerInformationFactory;
    private final LegalPartnerIndebtednessCommandService legalPartnerIndebtednessCommandService;
    private final TestableClock clock;
    private final FrontendPageRepository frontendPageRepository;
    private final TestLegalPartnerTerminationFactory terminationFactory;
    private final BannerRepository bannerRepository;
    private final BannerLegalPartnerRepository bannerLegalPartnerRepository;
    private final PickupPointDeactivationCommandService pickupPointDeactivationCommandService;
    private final DeactivationReasonCommandService deactivationReasonCommandService;
    private final OrderAdditionalInfoCommandService orderAdditionalInfoCommandService;
    private final ConfigurationGlobalCommandService configurationGlobalCommandService;

    private LegalPartner partner;
    private PickupPoint pickupPoint;
    private PickupPoint pickupPoint2;
    private Order cashPaidOrder;
    private Order arrivedOrder;
    private Order deliveredOrder;
    private Order onDemandOrder;
    private Order fashionOrder;
    private List<Object> orderIds;

    @MockBean
    private LegalPartnerOfferManager offerManager;

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.EPOCH, clock.getZone());
        orderReportRepository.deleteAll();
        pickupPointRepository.deleteAll();

        partner = legalPartnerFactory.createLegalPartner();
        pickupPoint = pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder.builder()
                .legalPartner(partner)
                .build());
        pickupPoint2 = pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder.builder()
                .legalPartner(partner)
                .build());
        orderIds = new ArrayList<>();

        cashPaidOrder = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .paymentType(OrderPaymentType.CASH)
                        .paymentStatus(OrderPaymentStatus.PAID)
                        .externalId("2")
                        .build())
                .pickupPoint(pickupPoint)
                .build());
        orderIds.add(cashPaidOrder.getId());
        orderIds.add(pickupPoint.getPvzMarketId());

        arrivedOrder = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .paymentType(OrderPaymentType.PREPAID)
                        .paymentStatus(OrderPaymentStatus.PAID)
                        .externalId("3")
                        .build())
                .pickupPoint(pickupPoint)
                .build());
        testOrderFactory.receiveOrder(arrivedOrder.getId());
        orderIds.add(arrivedOrder.getId());
        orderIds.add(pickupPoint.getPvzMarketId());

        deliveredOrder = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .paymentType(OrderPaymentType.PREPAID)
                        .paymentStatus(OrderPaymentStatus.PAID)
                        .externalId("4")
                        .build())
                .pickupPoint(pickupPoint)
                .build());
        testOrderFactory.receiveOrder(deliveredOrder.getId());
        testOrderFactory.deliverOrder(
                deliveredOrder.getId(),
                OrderDeliveryType.UNKNOWN,
                OrderPaymentType.PREPAID
        );
        orderAdditionalInfoCommandService.updateDeliveredBy(deliveredOrder.getId(), "dora_the_explorer");
        orderIds.add(deliveredOrder.getId());
        orderIds.add(pickupPoint.getPvzMarketId());

        onDemandOrder = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .type(OrderType.ON_DEMAND)
                        .externalId("5")
                        .build())
                .pickupPoint(pickupPoint2)
                .build());
        orderIds.add(onDemandOrder.getId());
        orderIds.add(pickupPoint2.getPvzMarketId());

        fashionOrder = testOrderFactory.createSimpleFashionOrder(false, pickupPoint);
        orderIds.add(fashionOrder.getId());
        orderIds.add(fashionOrder.getExternalId());
        orderIds.add(pickupPoint.getPvzMarketId());
    }

    @Test
    void getLegalPartnerBanners() throws Exception {
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();
        mockMvc.perform(
                get("/v1/pi/partners/" + legalPartner.getPartnerId() + "/info"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("partner/response_get_banners.json")));
    }

    @Test
    void whenBannersEmpty() throws Exception {
        var legalPartner = legalPartnerFactory.createLegalPartner();

        mockMvc.perform(
                        get("/v1/pi/partners/" + legalPartner.getPartnerId() + "/info")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent(
                        "partner/banner_information/response_empty.json"), true));
    }

    @Test
    void checkIndebtednessBanner() throws Exception {
        clock.setFixed(Instant.now(), ZoneOffset.ofHours(3));
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();
        pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.FULL, false, legalPartner, 3
        );

        BannerInformationParams partnerBanner =
                bannerInformationFactory.createIndebtednessBanner(CampaignType.TPL_PARTNER);
        setLegalPartnerIndebtedness(legalPartner, partnerBanner);

        ResultActions result = mockMvc.perform(
                        get("/v1/pi/partners/" + legalPartner.getPartnerId() + "/info")
                                .contentType(MediaType.APPLICATION_JSON));

        partnerBanner = bannerInformationFactory.parseFrequency(partnerBanner, OffsetDateTime.now(clock));
        Banner banner = bannerRepository.findBannerByBannerInformationIdAndStartShowTime(
                partnerBanner.getId(), partnerBanner.getDateTimeShowPrevious()
        );
        var expected = String.format(
                getFileContent("partner/banner_information/response_indebtedness.json"),
                banner.getId(), partnerBanner.getTitle(), partnerBanner.getBody()
        );
        result
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(expected, true));

        assertThat(bannerLegalPartnerRepository.existsById(banner.getId())).isTrue();
    }

    private void setLegalPartnerIndebtedness(
            LegalPartner partner, BannerInformationParams partnerBanner
    ) {
        BigDecimal debtSum = new BigDecimal(44000);
        LegalPartnerIndebtednessParams legalPartnerIndebtednessParams = LegalPartnerIndebtednessParams.builder()
                .legalPartnerId(partner.getId())
                .debtSum(debtSum)
                .debtDate(LocalDate.now(clock))
                .build();
        legalPartnerIndebtednessCommandService.updateIndebtedness(legalPartnerIndebtednessParams);
        partnerBanner.setBody(String.format(partnerBanner.getBody(), debtSum));
    }

    @Test
    void checkDebtDisabledBanner() throws Exception {
        clock.setFixed(Instant.now(), ZoneOffset.ofHours(3));
        var legalPartner = legalPartnerFactory.createLegalPartner();
        pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.NONE, false, legalPartner, 3
        );

        bannerInformationFactory.createIndebtednessBanner(CampaignType.TPL_PARTNER);
        var debtDisabledBanner = bannerInformationFactory.createDebtDisabledBanner(CampaignType.TPL_PARTNER);

        setLegalPartnerIndebtedness(legalPartner, debtDisabledBanner);
        createPartnerDebtDisabled(legalPartner);

        ResultActions result = mockMvc.perform(
                get("/v1/pi/partners/" + legalPartner.getPartnerId() + "/info")
                        .contentType(MediaType.APPLICATION_JSON));

        debtDisabledBanner = bannerInformationFactory.parseFrequency(debtDisabledBanner, OffsetDateTime.now(clock));
        Banner banner = bannerRepository.findBannerByBannerInformationIdAndStartShowTime(
                debtDisabledBanner.getId(), debtDisabledBanner.getDateTimeShowPrevious()
        );
        var expected = String.format(
                getFileContent("partner/banner_information/response_debt_disabled.json"),
                banner.getId(), debtDisabledBanner.getTitle(), debtDisabledBanner.getBody()
        );
        result
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(expected, true));

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

    @Test
    void checkCustomPopup() throws Exception {
        clock.setFixed(Instant.parse("2021-12-06T04:15:00Z"), ZoneOffset.ofHours(0));
        var legalPartner = legalPartnerFactory.createLegalPartner();

        var page = frontendPageRepository.save(buildPage());
        var banner = buildBanner(
                true, "0 0 0/1 * * ? *", 60, CampaignType.TPL_PARTNER, null, null, "Тест", List.of(page.getId())
        );
        var banner2 = buildBanner(
                true, "0 0 7 * * ? *", 600, CampaignType.TPL_PARTNER, null, null, "Тест2", Collections.emptyList()
        );
        var banner3 = buildBanner(
                true, "0 0 0/3 * * ? *", 180, CampaignType.TPL_PARTNER, null, null, "Тест3", null
        );

        var expected = String.format(
                getFileContent("partner/banner_information/response_custom.json"),
                banner3.getMessageType(), banner3.getTitle(), banner3.getBody(), banner3.getButtonText(),
                banner2.getMessageType(), banner2.getTitle(), banner2.getBody(), banner2.getButtonText(),
                banner.getMessageType(), banner.getTitle(), banner.getBody(), banner.getButtonText()
        );
        mockMvc.perform(
                        get("/v1/pi/partners/" + legalPartner.getPartnerId() + "/info")
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("pageId", page.getPageName()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(expected, false));
        var bannerIds = bannerRepository.findAll().stream().map(Banner::getId).collect(Collectors.toList());

        mockMvc.perform(
                        get("/v1/pi/partners/" + legalPartner.getPartnerId() + "/info")
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("pageId", page.getPageName()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(expected, false));
        var bannerIds2 = bannerRepository.findAll().stream().map(Banner::getId).collect(Collectors.toList());

        assertThat(bannerIds).isEqualTo(bannerIds2);
    }

    @Test
    void checkCloseCustomPopup() throws Exception {
        clock.setFixed(Instant.parse("2021-12-06T04:15:00Z"), ZoneOffset.ofHours(0));
        var legalPartner = legalPartnerFactory.createLegalPartner();

        var page = frontendPageRepository.save(buildPage());
        var banner = buildBanner(
                true, "0 0 0/1 * * ? *", 60, CampaignType.TPL_PARTNER, null, null, "Тест", List.of(page.getId())
        );

        var expected = String.format(
                getFileContent("partner/banner_information/response_close_custom.json"),
                banner.getMessageType(), banner.getTitle(), banner.getBody(), banner.getButtonText()
        );
        mockMvc.perform(
                        get("/v1/pi/partners/" + legalPartner.getPartnerId() + "/info")
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("pageId", page.getPageName()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(expected, false));

        var bannerIds = bannerRepository.findAll().stream().map(Banner::getId).collect(Collectors.toList());
        mockMvc.perform(
                        put("/v1/pi/partners/" + legalPartner.getPartnerId() + "/info/close")
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("id", String.valueOf(bannerIds.get(0)))
                )
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(
                        get("/v1/pi/partners/" + legalPartner.getPartnerId() + "/info")
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("pageId", page.getPageName()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(
                        getFileContent("partner/banner_information/response_empty.json"), false)
                );

        var legalPartner2 = legalPartnerFactory.createLegalPartner();
        mockMvc.perform(
                        get("/v1/pi/partners/" + legalPartner2.getPartnerId() + "/info")
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("pageId", page.getPageName()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(expected, false));
    }

    @Test
    void checkCloseSystemBanner() throws Exception {
        clock.setFixed(Instant.parse("2021-12-06T04:01:00Z"), ZoneOffset.ofHours(0));
        configurationGlobalCommandService.setValue(DAYS_FOR_DEACTIVATION_BANNER, 999999);
        configurationGlobalCommandService.setValue(DEACTIVATION_WITH_REASONS, true);

        var partner = legalPartnerFactory.createLegalPartner();
        var timeOffset = 5;
        var pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.FULL, false, partner, timeOffset
        );

        var banner = bannerInformationFactory.createDeactivationBanner(MessageType.DEACTIVATION);
        banner.setBody(String.format(banner.getBody(), pickupPoint.getName()));
        var deactivationReason = deactivationReasonCommandService.createDeactivationReason(REASON_NAME,
                REASON_DETAILS, true, true, null);

        pickupPointDeactivationCommandService.deactivate(pickupPoint.getPvzMarketId(),
                deactivationReason.getId(), LocalDate.now(clock));

        var expected = String.format(
                getFileContent("partner/banner_information/response_close_system.json"),
                banner.getMessageType(), banner.getTitle(), banner.getBody()
        );
        mockMvc.perform(
                        get("/v1/pi/partners/" + partner.getPartnerId() + "/info")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(expected, false));
        var bannerIds = bannerRepository.findAll().stream().map(Banner::getId).collect(Collectors.toList());
        mockMvc.perform(
                        put("/v1/pi/partners/" + partner.getPartnerId() + "/info/close")
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("id", String.valueOf(bannerIds.get(0)))
                )
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(
                        get("/v1/pi/partners/" + partner.getPartnerId() + "/info")
                                .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(
                        getFileContent("partner/banner_information/response_empty.json"), false)
                );
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
                        .displayType(DisplayType.POPUP)
                        .build()
        );
    }

    @Test
    @SneakyThrows
    void testUpdateInsensitive() {
        LegalPartner legalPartner =
                legalPartnerFactory.createLegalPartner(TestLegalPartnerFactory.LegalPartnerTestParamsBuilder.builder()
                        .params(TestLegalPartnerFactory.LegalPartnerTestParams.builder()
                                .organization(TestLegalPartnerFactory.LegalPartnerTestParams.OrganizationTestParams.builder()
                                        .name("ВАСИЛИЙ ДЕЛИВЕРИ СОЛЮШЕНС АНЛИМИТЕД")
                                        .taxpayerNumber("8606601100")
                                        .build())
                                .build())
                .build(), 123L);

        mockMvc.perform(
                patch("/v1/pi/partners/" + legalPartner.getPartnerId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("partner/request_update_insensitive.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("partner/response_update_insensitive.json")));
    }

    @Test
    @SneakyThrows
    void testUploadCorrectOffer() {
        var preLegalPartner = preLegalPartnerFactory.createPreLegalPartner();
        var legalPartner = legalPartnerFactory.createLegalPartner(
                TestLegalPartnerFactory.LegalPartnerTestParamsBuilder.builder()
                        .preLegalPartner(preLegalPartner)
                        .approvePreLegalPartner(false)
                        .build()
        );
        preLegalPartnerFactory.bindSecurityTicket(preLegalPartner.getId());
        preLegalPartnerFactory.approveBySecurity(preLegalPartner.getId());
        preLegalPartnerFactory.offerSignatureRequired(preLegalPartner.getId());
        mockMvc.perform(
                multipart("/v1/pi/partners/" + legalPartner.getPartnerId() + "/offer")
                        .file(buildFile("file.pdf")))
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(
                multipart("/v1/pi/partners/" + legalPartner.getPartnerId() + "/offer")
                        .file(buildFile("file.png")))
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(
                multipart("/v1/pi/partners/" + legalPartner.getPartnerId() + "/offer")
                        .file(buildFile("file.jpg")))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @SneakyThrows
    void testUploadWithIncorrectExtension() {
        var legalPartner = legalPartnerFactory.createLegalPartner();
        mockMvc.perform(
                multipart("/v1/pi/partners/" + legalPartner.getPartnerId() + "/offer")
                        .file(buildFile("file.exe")))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(
                multipart("/v1/pi/partners/" + legalPartner.getPartnerId() + "/offer")
                        .file(buildFile("")))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(
                multipart("/v1/pi/partners/" + legalPartner.getPartnerId() + "/offer")
                        .file(buildFile(null)))
                .andExpect(status().is4xxClientError());
    }

    private MockMultipartFile buildFile(String filename) {
        String contentType = filename != null ? URLConnection.getFileNameMap().getContentTypeFor(filename) : null;
        return new MockMultipartFile(FILE_INPUT_NAME, filename, contentType, FILE_DATA);
    }

    @Test
    @SneakyThrows
    void testDownloadExistingPdf() {
        when(offerManager.getOffer(eq(PARTNER_ID)))
                .thenReturn(LegalPartnerOfferParams.builder()
                        .legalPartnerExternalId(EXTERNAL_PARTNER_ID)
                        .filename("file.pdf")
                        .data(FILE_DATA)
                        .build());

        mockMvc.perform(get("/v1/pi/partners/1/offer"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    @SneakyThrows
    void testDownloadExistingPng() {
        when(offerManager.getOffer(eq(PARTNER_ID)))
                .thenReturn(LegalPartnerOfferParams.builder()
                        .legalPartnerExternalId(EXTERNAL_PARTNER_ID)
                        .filename("file.png")
                        .data(FILE_DATA)
                        .build());

        mockMvc.perform(get("/v1/pi/partners/1/offer"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }

    @Test
    @SneakyThrows
    void testDownloadExistingJpg() {
        when(offerManager.getOffer(eq(PARTNER_ID)))
                .thenReturn(LegalPartnerOfferParams.builder()
                        .legalPartnerExternalId(EXTERNAL_PARTNER_ID)
                        .filename("file.jpg")
                        .data(FILE_DATA)
                        .build());

        mockMvc.perform(get("/v1/pi/partners/1/offer"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG));
    }

    @Test
    @SneakyThrows
    void testDownloadNonExistingFile() {
        when(offerManager.getOffer(eq(PARTNER_ID)))
                .thenThrow(new TplEntityNotFoundException(LegalPartnerOffer.class));

        mockMvc.perform(get("/v1/pi/partners/1/offer"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getOrders() throws Exception {
        var expectedJson = String.format(getFileContent("partner/response_get_orders.json"), orderIds.toArray());

        mockMvc.perform(get("/v1/pi/partners/" + partner.getPartnerId() + "/orders")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(expectedJson));
    }

    @Test
    void getOrdersFiltered() throws Exception {
        var expectedJson = String.format(getFileContent("partner/response_get_orders_filtered.json"),
                onDemandOrder.getId(),
                pickupPoint2.getPvzMarketId()
        );

        mockMvc.perform(get("/v1/pi/partners/" + partner.getPartnerId() + "/orders?types=ON_DEMAND")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(expectedJson));
    }

    @Test
    void getOrdersByPvzMarketIds() throws Exception {
        var expectedJson = String.format(getFileContent("partner/response_get_orders_filtered.json"),
                onDemandOrder.getId(),
                pickupPoint2.getPvzMarketId()
        );

        mockMvc.perform(get("/v1/pi/partners/" + partner.getPartnerId() +
                "/orders?pvzMarketIds=" + pickupPoint2.getPvzMarketId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(expectedJson));
    }

    @Test
    void getFeatures() throws Exception {
        mockMvc.perform(get("/v1/pi/partners/" + partner.getPartnerId() + "/flags?features=BANNER"))
                .andExpect(content().json(getFileContent("partner/response_banner_feature.json")));
    }
}
