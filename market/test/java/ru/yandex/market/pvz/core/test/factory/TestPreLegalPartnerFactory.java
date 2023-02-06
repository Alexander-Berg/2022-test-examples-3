package ru.yandex.market.pvz.core.test.factory;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.mbi.api.client.entity.shops.SimpleShopRegistrationResponse;
import ru.yandex.market.pvz.client.model.approve.PreLegalPartnerApproveStatus;
import ru.yandex.market.pvz.client.model.partner.CollaborationForm;
import ru.yandex.market.pvz.client.model.partner.LegalForm;
import ru.yandex.market.pvz.client.model.partner.LegalPartnerType;
import ru.yandex.market.pvz.core.domain.approve.pre_legal_partner.PreLegalPartner;
import ru.yandex.market.pvz.core.domain.approve.pre_legal_partner.PreLegalPartnerCommandService;
import ru.yandex.market.pvz.core.domain.approve.pre_legal_partner.PreLegalPartnerParams;
import ru.yandex.market.pvz.core.domain.approve.pre_legal_partner.PreLegalPartnerParamsMapper;
import ru.yandex.market.pvz.core.domain.approve.pre_legal_partner.PreLegalPartnerQueryService;
import ru.yandex.market.pvz.core.domain.approve.pre_legal_partner.PreLegalPartnerRepository;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartner;
import ru.yandex.market.pvz.core.domain.legal_partner.offer.model.LegalPartnerOfferCommandService;
import ru.yandex.market.tpl.common.startrek.domain.TestTicketStateFactory;

import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.DEFAULT_DATASOURCE_ID;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParams.OrganizationTestParams.generateRandomINN;
import static ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory.LegalPartnerTestParamsBuilder;
import static ru.yandex.market.pvz.core.test.factory.TestPreLegalPartnerFactory.PreLegalPartnerTestParams.DEFAULT_REFUSAL_REASON;

@Transactional
public class TestPreLegalPartnerFactory {

    private static final String FILE_NAME = "file.pdf";
    private static final byte[] FILE_DATA = {4, 8, 15, 16, 23, 42};

    @Autowired
    private Clock clock;

    @Autowired
    private PreLegalPartnerCommandService preLegalPartnerCommandService;

    @Autowired
    private PreLegalPartnerRepository preLegalPartnerRepository;

    @Autowired
    private PreLegalPartnerQueryService preLegalPartnerQueryService;

    @Autowired
    private PreLegalPartnerParamsMapper mapper;

    @Autowired
    private TestLegalPartnerFactory legalPartnerFactory;

    @Autowired
    private LegalPartnerOfferCommandService legalPartnerOfferCommandService;

    @Autowired
    private TestTicketStateFactory ticketStateFactory;

    public PreLegalPartnerParams createPreLegalPartner(PreLegalPartnerTestParams params) {
        var shopInfo = new SimpleShopRegistrationResponse();
        shopInfo.setDatasourceId(DEFAULT_DATASOURCE_ID);
        shopInfo.setCampaignId(RandomUtils.nextInt(1, Integer.MAX_VALUE));
        shopInfo.setClientId(RandomUtils.nextLong(1, Long.MAX_VALUE));
        shopInfo.setOwnerId(RandomUtils.nextInt(1, Integer.MAX_VALUE));
        return preLegalPartnerCommandService.create(buildPreLegalPartner(params), shopInfo);
    }

    public PreLegalPartnerParams createPreLegalPartner() {
        return createPreLegalPartner(PreLegalPartnerTestParams.builder().build());
    }

    public PreLegalPartnerParams createApprovedPreLegalPartner() {
        var preLegalPartner = createPreLegalPartner();
        return preLegalPartnerCommandService.approveFromCrm(preLegalPartner.getId());
    }

    public PreLegalPartnerParams approve(long id) {
        var preLegalPartner = preLegalPartnerRepository.findByIdOrThrow(id);
        return preLegalPartnerCommandService.approveFromCrm(preLegalPartner.getId());
    }

    public PreLegalPartnerParams bindSecurityTicket(long id) {
        var ticket = ticketStateFactory.createTicket("PVZDD-" + RandomUtils.nextInt(0, 100_000));
        var preLegalPartner = preLegalPartnerRepository.findByIdOrThrow(id);
        preLegalPartner.setSecurityApprovalTicket(ticket);
        preLegalPartnerRepository.save(preLegalPartner);
        return preLegalPartnerQueryService.getById(id);
    }

    public PreLegalPartnerParams approveBySecurity(long id) {
        var preLegalPartner = preLegalPartnerRepository.findByIdOrThrow(id);
        return preLegalPartnerCommandService.approveBySecurity(preLegalPartner.getSecurityApprovalTicket().getKey());
    }

    public PreLegalPartnerParams offerSignatureRequired(long id) {
        var preLegalPartner = preLegalPartnerRepository.findByIdOrThrow(id);
        return preLegalPartnerCommandService.rejectOfferFromCrm(id);
    }

    public PreLegalPartnerParams uploadOffer(long id) {
        var preLegalPartner = preLegalPartnerRepository.findByIdOrThrow(id);
        legalPartnerOfferCommandService.saveOffer(preLegalPartner.getPartnerId(), "Оферта", new byte[0]);
        return preLegalPartnerQueryService.getById(id);
    }

    public PreLegalPartnerParams checkOffer(long id) {
        var preLegalPartner = preLegalPartnerRepository.findByIdOrThrow(id);
        return preLegalPartnerCommandService.checkOffer(preLegalPartner.getPartnerId());
    }

    public PreLegalPartnerParams activate(long id) {
        return preLegalPartnerCommandService.activateFromCrm(id, LocalDate.now(clock));
    }

    public PreLegalPartnerParams fullActivate(long id) {
        approve(id);
        bindSecurityTicket(id);
        approveBySecurity(id);
        offerSignatureRequired(id);
        uploadOffer(id);
        checkOffer(id);
        return activate(id);
    }

    public PreLegalPartnerParams createRejectedPreLegalPartner() {
        var preLegalPartner = createPreLegalPartner();
        return preLegalPartnerCommandService.rejectFromCrm(preLegalPartner.getId(), DEFAULT_REFUSAL_REASON);
    }

    private PreLegalPartnerParams buildPreLegalPartner(
            PreLegalPartnerTestParams params
    ) {
        return PreLegalPartnerParams.builder()
                .delegateName(params.getDelegateName())
                .delegateEmail(params.getDelegateEmail())
                .delegatePhone(params.getDelegatePhone())
                .legalType(params.getLegalType())
                .legalForm(params.getLegalForm())
                .organisationName(params.getOrganisationName())
                .taxpayerNumber(params.getTaxpayerNumber())
                .ogrn(params.getOgrn())
                .collaborationForm(params.getCollaborationForm())
                .wantBrand(params.isWantBrand())
                .pickupPointCount(params.getPickupPointCount())
                .pickupPointRegion(params.getPickupPointRegion())
                .pickupPointLocality(params.getPickupPointLocality())
                .pickupPointAddress(params.getPickupPointAddress())
                .pickupPointLatitude(params.getPickupPointLatitude())
                .pickupPointLongitude(params.getPickupPointLongitude())
                .pickupPointSquare(params.getPickupPointSquare())
                .pickupPointCeilingHeight(params.getPickupPointCeilingHeight())
                .pickupPointPhotoUrl(params.getPickupPointPhotoUrl())
                .pickupPointComment(params.getPickupPointComment())
                .pickupPointFloor(params.getPickupPointFloor())
                .pickupPointPolygonId(params.getPickupPointPolygonId())
                .hasWindows(params.getHasWindows())
                .hasSeparateEntrance(params.getHasSeparateEntrance())
                .hasStreetEntrance(params.getHasStreetEntrance())
                .warehouseArea(params.getWarehouseArea())
                .clientArea(params.getClientArea())
                .build();
    }

    public PreLegalPartnerParams forceChangeStatus(long preLegalPartnerId, PreLegalPartnerApproveStatus status) {
        PreLegalPartner preLegalPartner = preLegalPartnerRepository.findByIdOrThrow(preLegalPartnerId);
        preLegalPartner.setApproveStatus(status);
        return mapper.map(preLegalPartnerRepository.save(preLegalPartner));
    }

    public PreLegalPartnerParams createLegalPartnerForLead(long id) {
        PreLegalPartnerParams preLegalPartner = preLegalPartnerQueryService.getById(id);
        legalPartnerFactory.createLegalPartner(LegalPartnerTestParamsBuilder.builder()
                .preLegalPartner(preLegalPartner)
                .params(LegalPartnerTestParams.builder()
                        .partnerId(preLegalPartner.getPartnerId())
                        .build())
                .build());
        return preLegalPartnerQueryService.getById(id);
    }

    public PreLegalPartnerParams createLegalPartnerWithSignedOfferForLead(long id) {
        PreLegalPartnerParams preLegalPartner = preLegalPartnerQueryService.getById(id);
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner(LegalPartnerTestParamsBuilder.builder()
                .preLegalPartner(preLegalPartner)
                .params(LegalPartnerTestParams.builder()
                        .partnerId(preLegalPartner.getPartnerId())
                        .build())
                .build());

        legalPartnerOfferCommandService.saveOffer(legalPartner.getPartnerId(), FILE_NAME, FILE_DATA);
        return preLegalPartnerQueryService.getById(id);
    }

    @Data
    @Builder
    public static class PreLegalPartnerTestParams {

        public static final String DEFAULT_DELEGATE_NAME = "Ургант Иван Андреевич";
        public static final String DEFAULT_DELEGATE_EMAIL = "urgantia@yandex.ru";
        public static final String DEFAULT_DELEGATE_PHONE = "+79094516356";
        public static final LegalPartnerType DEFAULT_LEGAL_TYPE = LegalPartnerType.LEGAL_PERSON;
        public static final LegalForm DEFAULT_LEGAL_FORM = LegalForm.OOO;
        public static final String DEFAULT_ORGANISATION_NAME = "Логистический Ургант";
        public static final String DEFAULT_OGRN = "1190280074841";
        public static final CollaborationForm DEFAULT_COLLABORATION_FORM = CollaborationForm.EXISTENT_PICKUP_POINT;
        public static final boolean DEFAULT_WANT_BRAND = false;
        public static final int DEFAULT_PICKUP_POINT_COUNT = 1;
        public static final String DEFAULT_PICKUP_POINT_REGION = "Московская область";
        public static final String DEFAULT_PICKUP_POINT_LOCALITY = "Москва";
        public static final String DEFAULT_PICKUP_POINT_ADDRESS = "Рижский проезд, д.4, офис 5";
        public static final BigDecimal DEFAULT_PICKUP_POINT_LAT = BigDecimal.valueOf(55.77914);
        public static final BigDecimal DEFAULT_PICKUP_POINT_LON = BigDecimal.valueOf(37.577921);
        public static final BigDecimal DEFAULT_PICKUP_POINT_SQUARE = BigDecimal.valueOf(75.2);
        public static final BigDecimal DEFAULT_PICKUP_POINT_CEILING_HEIGHT = BigDecimal.valueOf(2.5);
        public static final BigDecimal DEFAULT_WAREHOUSE_AREA = BigDecimal.valueOf(150.75);
        public static final BigDecimal DEFAULT_CLIENT_AREA = BigDecimal.valueOf(30.3);
        public static final String DEFAULT_PICKUP_POINT_PHOTO_URL = "https://disk.yandex.ru/i/6KGvEhmW1u-efw";
        public static final String DEFAULT_PICKUP_POINT_COMMENT = "Вход с торца здания";
        public static final String DEFAULT_REFUSAL_REASON = "Отклонен безопасниками";
        public static final boolean DEFAULT_HAS_STREET_ENTRANCE = false;
        public static final boolean DEFAULT_HAS_SEPARATE_ENTRANCE = false;
        public static final boolean DEFAULT_HAS_WINDOWS = false;
        public static final Integer DEFAULT_FLOOR = 1;
        public static final String DEFAULT_POLYGON_ID = "A12345";

        @Builder.Default
        private Long partnerId = RandomUtils.nextLong();

        @Builder.Default
        private String delegateName = DEFAULT_DELEGATE_NAME;

        @Builder.Default
        private String delegateEmail = DEFAULT_DELEGATE_EMAIL;

        @Builder.Default
        private String delegatePhone = DEFAULT_DELEGATE_PHONE;

        @Builder.Default
        private LegalPartnerType legalType = DEFAULT_LEGAL_TYPE;

        @Builder.Default
        private LegalForm legalForm = DEFAULT_LEGAL_FORM;

        @Builder.Default
        private String organisationName = DEFAULT_ORGANISATION_NAME;

        @Builder.Default
        private String taxpayerNumber = generateRandomINN();

        @Builder.Default
        private String ogrn = DEFAULT_OGRN;

        @Builder.Default
        private CollaborationForm collaborationForm = DEFAULT_COLLABORATION_FORM;

        @Builder.Default
        private boolean wantBrand = DEFAULT_WANT_BRAND;

        @Builder.Default
        private int pickupPointCount = DEFAULT_PICKUP_POINT_COUNT;

        @Builder.Default
        private String pickupPointRegion = DEFAULT_PICKUP_POINT_REGION;

        @Builder.Default
        private String pickupPointLocality = DEFAULT_PICKUP_POINT_LOCALITY;

        @Builder.Default
        private String pickupPointAddress = DEFAULT_PICKUP_POINT_ADDRESS;

        @Builder.Default
        private BigDecimal pickupPointLatitude = DEFAULT_PICKUP_POINT_LAT;

        @Builder.Default
        private BigDecimal pickupPointLongitude = DEFAULT_PICKUP_POINT_LON;

        @Builder.Default
        private BigDecimal pickupPointSquare = DEFAULT_PICKUP_POINT_SQUARE;

        @Builder.Default
        private BigDecimal pickupPointCeilingHeight = DEFAULT_PICKUP_POINT_CEILING_HEIGHT;

        @Builder.Default
        private String pickupPointPhotoUrl = DEFAULT_PICKUP_POINT_PHOTO_URL;

        @Builder.Default
        private String pickupPointComment = DEFAULT_PICKUP_POINT_COMMENT;

        @Builder.Default
        private Boolean hasStreetEntrance = DEFAULT_HAS_STREET_ENTRANCE;

        @Builder.Default
        private Boolean hasSeparateEntrance = DEFAULT_HAS_SEPARATE_ENTRANCE;

        @Builder.Default
        private Boolean hasWindows = DEFAULT_HAS_WINDOWS;

        @Builder.Default
        private Integer pickupPointFloor = DEFAULT_FLOOR;

        @Builder.Default
        private String pickupPointPolygonId = DEFAULT_POLYGON_ID;

        @Builder.Default
        private BigDecimal clientArea = DEFAULT_CLIENT_AREA;

        @Builder.Default
        private BigDecimal warehouseArea = DEFAULT_WAREHOUSE_AREA;
    }
}
