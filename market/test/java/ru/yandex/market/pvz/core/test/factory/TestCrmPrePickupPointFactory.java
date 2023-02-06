package ru.yandex.market.pvz.core.test.factory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.pvz.client.model.approve.PrePickupPointApproveStatus;
import ru.yandex.market.pvz.client.model.pickup_point.PickupPointBrandingType;
import ru.yandex.market.pvz.core.domain.approve.crm_pre_pickup_point.CrmPrePickupPoint;
import ru.yandex.market.pvz.core.domain.approve.crm_pre_pickup_point.CrmPrePickupPointBrandingData;
import ru.yandex.market.pvz.core.domain.approve.crm_pre_pickup_point.CrmPrePickupPointCommandService;
import ru.yandex.market.pvz.core.domain.approve.crm_pre_pickup_point.CrmPrePickupPointParams;
import ru.yandex.market.pvz.core.domain.approve.crm_pre_pickup_point.CrmPrePickupPointParamsMapper;
import ru.yandex.market.pvz.core.domain.approve.crm_pre_pickup_point.CrmPrePickupPointQueryService;
import ru.yandex.market.pvz.core.domain.approve.crm_pre_pickup_point.CrmPrePickupPointRepository;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointLocationTestParams;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointScheduleDayTestParams;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory.PickupPointScheduleTestParams;

import static ru.yandex.market.pvz.core.test.factory.TestBrandRegionFactory.DEFAULT_REGIONS;

@Transactional
public class TestCrmPrePickupPointFactory {

    @Autowired
    private TestPickupPointFactory pickupPointFactory;

    @Autowired
    private TestLegalPartnerFactory legalPartnerFactory;

    @Autowired
    private CrmPrePickupPointRepository crmPrePickupPointRepository;

    @Autowired
    private CrmPrePickupPointParamsMapper mapper;

    @Autowired
    private CrmPrePickupPointCommandService crmPrePickupPointCommandService;

    @Autowired
    private CrmPrePickupPointQueryService crmPrePickupPointQueryService;

    public CrmPrePickupPointParams create() {
        return create(CrmPrePickupPointTestParamsBuilder.builder().build());
    }

    public CrmPrePickupPointParams create(CrmPrePickupPointTestParamsBuilder builder) {
        if (builder.getLegalPartnerId() == null) {
            builder.setLegalPartnerId(legalPartnerFactory.createLegalPartner().getId());
        }

        CrmPrePickupPointTestParams params = builder.getParams();
        return crmPrePickupPointCommandService.create(CrmPrePickupPointParams.builder()
                .status(params.getStatus())
                .name(params.getName())
                .phone(params.getPhone())
                .prepayAllowed(params.isPrepayAllowed())
                .cardAllowed(params.isCardAllowed())
                .cashAllowed(params.isCashAllowed())
                .instruction(params.getInstruction())
                .wantToBeBranded(params.isWantToBeBranded())
                .brandingData(params.getBrandingData())
                .legalPartnerId(builder.getLegalPartnerId())
                .location(pickupPointFactory.mapToCrmLocation(params.getLocation()))
                .scheduleDays(pickupPointFactory.mapScheduleDays(params.getScheduleDays()))
                .warehouseArea(params.getWarehouseArea())
                .clientArea(params.getClientArea())
                .cashCompensation(params.getCashCompensation())
                .cardCompensation(params.getCardCompensation())
                .orderTransmissionReward(params.getOrderTransmissionReward())
                .brandingType(params.getBrandingType())
                .brandedSince(params.getBrandedSince())
                .brandRegion(params.getBrandRegion())
                .build());
    }

    public CrmPrePickupPointParams approve(long id, CrmPrePickupPointTestParams params) {
        var crmPrePickupPoint = crmPrePickupPointQueryService.getById(id);
        crmPrePickupPoint.setCardCompensation(params.getCardCompensation());
        crmPrePickupPoint.setCashCompensation(params.getCashCompensation());
        crmPrePickupPoint.setOrderTransmissionReward(params.getOrderTransmissionReward());
        crmPrePickupPoint.setBrandingType(params.getBrandingType());
        crmPrePickupPoint.setBrandedSince(params.getBrandedSince());
        crmPrePickupPoint.setBrandRegion(params.getBrandRegion());
        return crmPrePickupPointCommandService.approveFromCrm(crmPrePickupPoint);
    }

    public CrmPrePickupPointParams forceChangeStatus(long id, PrePickupPointApproveStatus status) {
        CrmPrePickupPoint prePickupPoint = crmPrePickupPointRepository.findByIdOrThrow(id);
        prePickupPoint.setStatus(status);
        return mapper.map(crmPrePickupPointRepository.save(prePickupPoint));
    }

    @Data
    @Builder
    public static class CrmPrePickupPointTestParamsBuilder {

        @Builder.Default
        private CrmPrePickupPointTestParams params = CrmPrePickupPointTestParams.builder().build();

        private Long legalPartnerId;

    }

    @Data
    @Builder
    public static class CrmPrePickupPointTestParams {

        public static final String DEFAULT_NAME = "Подвал дома Колотушкина на улице Пушкина";
        public static final PrePickupPointApproveStatus DEFAULT_STATUS = PrePickupPointApproveStatus.CHECKING;
        public static final String DEFAULT_PHONE = "88005553535";
        public static final boolean DEFAULT_PREPAY_ALLOWED = PickupPoint.DEFAULT_PREPAY_ALLOWED;
        public static final boolean DEFAULT_CASH_ALLOWED = PickupPoint.DEFAULT_CASH_ALLOWED;
        public static final boolean DEFAULT_CARD_ALLOWED = PickupPoint.DEFAULT_CARD_ALLOWED;
        public static final String DEFAULT_INSTRUCTION = "Вход с торца здания";
        public static final boolean DEFAULT_WANT_TO_BE_BRANDED = true;

        public static final BigDecimal DEFAULT_SQUARE = BigDecimal.valueOf(75.2);
        public static final BigDecimal DEFAULT_CEILING_HEIGHT = BigDecimal.valueOf(2.5);
        public static final String DEFAULT_PHOTO_URL = "https://disk.yandex.ru/i/6KGvEhmW1u-efw";
        public static final String DEFAULT_COMMENT = "Вход с торца здания";
        public static final BigDecimal DEFAULT_SCORING = BigDecimal.valueOf(12.345);
        public static final boolean DEFAULT_HAS_STREET_ENTRANCE = false;
        public static final boolean DEFAULT_HAS_SEPARATE_ENTRANCE = false;
        public static final boolean DEFAULT_HAS_WINDOWS = false;
        public static final String DEFAULT_POLYGON_ID = "A12345";

        public static final CrmPrePickupPointBrandingData DEFAULT_BRANDING_DATA = new CrmPrePickupPointBrandingData(
                DEFAULT_SQUARE,
                DEFAULT_CEILING_HEIGHT,
                DEFAULT_PHOTO_URL,
                DEFAULT_COMMENT,
                DEFAULT_SCORING,
                DEFAULT_HAS_STREET_ENTRANCE,
                DEFAULT_HAS_SEPARATE_ENTRANCE,
                DEFAULT_HAS_WINDOWS,
                DEFAULT_POLYGON_ID
        );

        public static final PickupPointLocationTestParams DEFAULT_LOCATION =
                PickupPointLocationTestParams.builder().build();

        public static final List<PickupPointScheduleDayTestParams> DEFAULT_SCHEDULE_DAYS =
                PickupPointScheduleTestParams.DEFAULT_SCHEDULE_DAYS;

        public static final BigDecimal DEFAULT_WAREHOUSE_AREA = BigDecimal.valueOf(150.0);
        public static final BigDecimal DEFAULT_CLIENT_AREA = BigDecimal.valueOf(30.0);

        public static final BigDecimal DEFAULT_TRANSMISSION_REWARD = BigDecimal.valueOf(45.0);
        public static final BigDecimal DEFAULT_CASH_COMPENSATION_RATE = BigDecimal.valueOf(0.003);
        public static final BigDecimal DEFAULT_CARD_COMPENSATION_RATE = BigDecimal.valueOf(0.019);

        public static final PickupPointBrandingType DEFAULT_BRANDING_TYPE = PickupPoint.DEFAULT_BRANDING_TYPE;
        public static final String DEFAULT_BRAND_REGION = DEFAULT_REGIONS.get(0).getRegion();
        public static final LocalDate DEFAULT_BRAND_DATE = LocalDate.of(2020, 12, 23);

        @Builder.Default
        private PrePickupPointApproveStatus status = DEFAULT_STATUS;
        @Builder.Default
        private String name = DEFAULT_NAME;
        @Builder.Default
        private String phone = DEFAULT_PHONE;
        @Builder.Default
        private boolean prepayAllowed = DEFAULT_PREPAY_ALLOWED;
        @Builder.Default
        private boolean cashAllowed = DEFAULT_CASH_ALLOWED;
        @Builder.Default
        private boolean cardAllowed = DEFAULT_CARD_ALLOWED;
        @Builder.Default
        private String instruction = DEFAULT_INSTRUCTION;
        @Builder.Default
        private boolean wantToBeBranded = DEFAULT_WANT_TO_BE_BRANDED;
        @Builder.Default
        private CrmPrePickupPointBrandingData brandingData = DEFAULT_BRANDING_DATA;
        @Builder.Default
        private PickupPointLocationTestParams location = DEFAULT_LOCATION;
        @Builder.Default
        private List<PickupPointScheduleDayTestParams> scheduleDays = DEFAULT_SCHEDULE_DAYS;

        @Builder.Default
        private BigDecimal warehouseArea = DEFAULT_WAREHOUSE_AREA;

        @Builder.Default
        private BigDecimal clientArea = DEFAULT_CLIENT_AREA;

        @Builder.Default
        private BigDecimal cashCompensation = DEFAULT_CASH_COMPENSATION_RATE;

        @Builder.Default
        private BigDecimal cardCompensation = DEFAULT_CARD_COMPENSATION_RATE;

        @Builder.Default
        private BigDecimal orderTransmissionReward = DEFAULT_TRANSMISSION_REWARD;

        @Builder.Default
        private PickupPointBrandingType brandingType = DEFAULT_BRANDING_TYPE;

        @Builder.Default
        private LocalDate brandedSince = DEFAULT_BRAND_DATE;

        @Builder.Default
        private String brandRegion = DEFAULT_BRAND_REGION;

    }

}
