package ru.yandex.market.pvz.core.domain.approve.crm_pre_pickup_point;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.client.model.approve.ApproveStatus;
import ru.yandex.market.pvz.client.model.approve.PrePickupPointApproveStatus;
import ru.yandex.market.pvz.client.model.pickup_point.PickupPointBrandingType;
import ru.yandex.market.pvz.core.domain.configuration.global.ConfigurationGlobalCommandService;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartner;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestCrmPrePickupPointFactory;
import ru.yandex.market.pvz.core.test.factory.TestCrmPrePickupPointFactory.CrmPrePickupPointTestParams;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.exception.TplInvalidParameterException;
import ru.yandex.market.tpl.common.web.exception.TplInvalidActionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.pvz.client.model.approve.PrePickupPointApproveStatus.APPROVED;
import static ru.yandex.market.pvz.client.model.approve.PrePickupPointApproveStatus.CHECKING;
import static ru.yandex.market.pvz.client.model.approve.PrePickupPointApproveStatus.REJECTED;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.BRAND_PVZ_TO_BRAND_PVZ_MIN_DIST;
import static ru.yandex.market.pvz.core.test.factory.TestCrmPrePickupPointFactory.CrmPrePickupPointTestParams.DEFAULT_LOCATION;
import static ru.yandex.market.pvz.core.test.factory.TestCrmPrePickupPointFactory.CrmPrePickupPointTestParams.DEFAULT_SCHEDULE_DAYS;
import static ru.yandex.market.pvz.core.test.factory.TestCrmPrePickupPointFactory.CrmPrePickupPointTestParamsBuilder;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class CrmPrePickupPointCommandServiceTest {

    private static final LocalDate RENOVATION_START_DATE = LocalDate.of(2021, 5, 1);
    private static final LocalDate RENOVATION_FINISH_DATE = LocalDate.of(2021, 5, 19);
    private static final String COLLIDED_NAME = "COLLIDED_NAME";

    private final TestableClock clock;

    private final CrmPrePickupPointCommandService crmPrePickupPointCommandService;
    private final CrmPrePickupPointQueryService crmPrePickupPointQueryService;
    private final TestCrmPrePickupPointFactory crmPrePickupPointFactory;
    private final TestPickupPointFactory pickupPointFactory;
    private final TestLegalPartnerFactory legalPartnerFactory;
    private final ConfigurationGlobalCommandService configurationGlobalCommandService;

    @Test
    void testCreate() {
        CrmPrePickupPointParams params = crmPrePickupPointFactory.create();

        assertThat(params.getName()).isNotBlank();
        assertThat(params.getStatus()).isEqualTo(CrmPrePickupPointTestParams.DEFAULT_STATUS);
        assertThat(params.getPhone()).isEqualTo(CrmPrePickupPointTestParams.DEFAULT_PHONE);
        assertThat(params.isPrepayAllowed()).isEqualTo(CrmPrePickupPointTestParams.DEFAULT_PREPAY_ALLOWED);
        assertThat(params.isCashAllowed()).isEqualTo(CrmPrePickupPointTestParams.DEFAULT_CASH_ALLOWED);
        assertThat(params.isCardAllowed()).isEqualTo(CrmPrePickupPointTestParams.DEFAULT_CARD_ALLOWED);
        assertThat(params.getInstruction()).isEqualTo(CrmPrePickupPointTestParams.DEFAULT_INSTRUCTION);
        assertThat(params.isWantToBeBranded()).isEqualTo(CrmPrePickupPointTestParams.DEFAULT_WANT_TO_BE_BRANDED);

        assertThat(params.getBrandingData().getSquare()).isEqualTo(CrmPrePickupPointTestParams.DEFAULT_SQUARE);
        assertThat(params.getBrandingData().getCeilingHeight())
                .isEqualTo(CrmPrePickupPointTestParams.DEFAULT_CEILING_HEIGHT);
        assertThat(params.getBrandingData().getPhotoUrl()).isEqualTo(CrmPrePickupPointTestParams.DEFAULT_PHOTO_URL);
        assertThat(params.getBrandingData().getComment()).isEqualTo(CrmPrePickupPointTestParams.DEFAULT_COMMENT);

        assertThat(params.getLocation()).isEqualTo(pickupPointFactory.mapToCrmLocation(DEFAULT_LOCATION));
        assertThat(params.getScheduleDays()).isEqualTo(pickupPointFactory.mapScheduleDays(DEFAULT_SCHEDULE_DAYS));
    }

    @Test
    void testCreatePrePickupPointForRejectedPartner() {
        LegalPartner partner = legalPartnerFactory.createLegalPartner();
        LegalPartner rejectedPartner = legalPartnerFactory.forceApproveStatus(partner.getId(),
                ApproveStatus.REJECTED_WITHOUT_RETRY);

        assertThatThrownBy(
                () -> crmPrePickupPointFactory.create(CrmPrePickupPointTestParamsBuilder.builder()
                        .legalPartnerId(rejectedPartner.getId())
                        .build()))
                .hasMessage("Для данного партнёра отправка повторных заявок запрещена")
                .isExactlyInstanceOf(TplInvalidActionException.class);
    }

    @Test
    void testCreatePrePickupPointWithPrePickupPointNameCollision() {
        LegalPartner mutualLegalPartner = legalPartnerFactory.createLegalPartner();

        crmPrePickupPointFactory.create(CrmPrePickupPointTestParamsBuilder.builder()
                .legalPartnerId(mutualLegalPartner.getId())
                .params(CrmPrePickupPointTestParams.builder().name(COLLIDED_NAME).build())
                .build());

        assertThatThrownBy(
                () -> crmPrePickupPointFactory.create(CrmPrePickupPointTestParamsBuilder.builder()
                        .legalPartnerId(mutualLegalPartner.getId())
                        .params(CrmPrePickupPointTestParams.builder().name(COLLIDED_NAME).build())
                        .build()))
                .hasMessage("ПВЗ с таким именем уже зарегистрирован")
                .isExactlyInstanceOf(TplInvalidParameterException.class);
    }

    @ParameterizedTest
    @EnumSource(PickupPointBrandingType.class)
    void testApproveFromCrmOverridesBrandingIfSet(PickupPointBrandingType brandingType) {
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();
        LocalDate expectedBrandedSince = LocalDate.now();
        String expectedBrandRegion = "Узбекистан";

        CrmPrePickupPointParams rejectedPickupPoint =
                crmPrePickupPointFactory.create(CrmPrePickupPointTestParamsBuilder.builder()
                        .legalPartnerId(legalPartner.getId())
                        .params(CrmPrePickupPointTestParams.builder()
                                .status(REJECTED)
                                .brandingType(PickupPointBrandingType.FULL)
                                .brandedSince(expectedBrandedSince)
                                .brandRegion(expectedBrandRegion)
                                .build())
                        .build());
        crmPrePickupPointCommandService.changeStatus(rejectedPickupPoint.getId(), CHECKING, false);

        rejectedPickupPoint.setStatus(APPROVED);
        rejectedPickupPoint.setBrandingType(brandingType);

        CrmPrePickupPointParams pickupPointParams = crmPrePickupPointCommandService.approveFromCrm(rejectedPickupPoint);

        assertThat(pickupPointParams.getBrandingType()).isEqualTo(brandingType);

        if (pickupPointParams.getBrandingType() == PickupPointBrandingType.NONE) {
            assertThat(pickupPointParams.getBrandedSince()).isNull();
            assertThat(pickupPointParams.getBrandRegion()).isNull();
        } else {
            assertThat(pickupPointParams.getBrandedSince()).isEqualTo(expectedBrandedSince);
            assertThat(pickupPointParams.getBrandRegion()).isEqualTo(expectedBrandRegion);
        }
    }

    @ParameterizedTest
    @EnumSource(PickupPointBrandingType.class)
    void testApproveFromCrmNotOverridesBrandingIfNotSet(PickupPointBrandingType brandingTypeBefore) {
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();
        LocalDate expectedBrandedSince = brandingTypeBefore.isBrand() ? LocalDate.now() : null;
        String expectedBrandRegion = brandingTypeBefore.isBrand() ? "Узбекистан" : null;

        CrmPrePickupPointParams rejectedPickupPoint =
                crmPrePickupPointFactory.create(CrmPrePickupPointTestParamsBuilder.builder()
                        .legalPartnerId(legalPartner.getId())
                        .params(CrmPrePickupPointTestParams.builder()
                                .status(REJECTED)
                                .brandingType(brandingTypeBefore)
                                .brandedSince(expectedBrandedSince)
                                .brandRegion(expectedBrandRegion)
                                .build())
                        .build());
        crmPrePickupPointCommandService.changeStatus(rejectedPickupPoint.getId(), CHECKING, false);

        rejectedPickupPoint.setStatus(APPROVED);
        rejectedPickupPoint.setBrandingType(null);
        rejectedPickupPoint.setBrandedSince(LocalDate.EPOCH);
        rejectedPickupPoint.setBrandRegion("Бермудские острова");

        CrmPrePickupPointParams notBrandedPickupPoint =
                crmPrePickupPointCommandService.approveFromCrm(rejectedPickupPoint);

        assertThat(notBrandedPickupPoint.getBrandingType()).isEqualTo(brandingTypeBefore);
        assertThat(notBrandedPickupPoint.getBrandedSince()).isEqualTo(expectedBrandedSince);
        assertThat(notBrandedPickupPoint.getBrandRegion()).isEqualTo(expectedBrandRegion);
    }

    @Test
    void testCreatePrePickupPointWithPickupPointNameCollision() {
        LegalPartner mutualLegalPartner = legalPartnerFactory.createLegalPartner();

        pickupPointFactory.createPickupPoint(TestPickupPointFactory.CreatePickupPointBuilder.builder()
                .legalPartner(mutualLegalPartner)
                .params(TestPickupPointFactory.PickupPointTestParams.builder()
                        .name(COLLIDED_NAME)
                        .build())
                .build());

        assertThatThrownBy(
                () -> crmPrePickupPointFactory.create(CrmPrePickupPointTestParamsBuilder.builder()
                        .legalPartnerId(mutualLegalPartner.getId())
                        .params(CrmPrePickupPointTestParams.builder().name(COLLIDED_NAME).build())
                        .build()))
                .hasMessage("ПВЗ с таким именем уже зарегистрирован")
                .isExactlyInstanceOf(TplInvalidParameterException.class);
    }

    @Test
    void testCreatePickupPointsNextToEachOther() {
        configurationGlobalCommandService.setValue(BRAND_PVZ_TO_BRAND_PVZ_MIN_DIST, 500);

        // ~100 meters
        BigDecimal lat1 = BigDecimal.valueOf(55.752538600041085);
        BigDecimal lng1 = BigDecimal.valueOf(37.617569067099765);
        BigDecimal lat2 = BigDecimal.valueOf(55.751464203009824);
        BigDecimal lng2 = BigDecimal.valueOf(37.61785960625385);

        var crmPrePickupPoint1 = crmPrePickupPointFactory.create(CrmPrePickupPointTestParamsBuilder.builder()
                .params(CrmPrePickupPointTestParams.builder()
                        .location(TestPickupPointFactory.PickupPointLocationTestParams.builder()
                                .lat(lat1)
                                .lng(lng1)
                                .build())
                        .brandingType(PickupPointBrandingType.FULL)
                        .build())
                .build());

        var crmPrePickupPoint2 = crmPrePickupPointFactory.create(CrmPrePickupPointTestParamsBuilder.builder()
                .params(CrmPrePickupPointTestParams.builder()
                        .location(TestPickupPointFactory.PickupPointLocationTestParams.builder()
                                .lat(lat2)
                                .lng(lng2)
                                .build())
                        .build())
                .build());

        assertThat(crmPrePickupPoint1.getLocation().getLocationWarning()).isNull();
        assertThat(crmPrePickupPoint2.getLocation().getLocationWarning())
                .isEqualTo("Нужно проверить локацию - рядом находится брендированный ПВЗ");
    }

    @Test
    void testUpdatePickupPointNextToAnother() {
        configurationGlobalCommandService.setValue(BRAND_PVZ_TO_BRAND_PVZ_MIN_DIST, 500);

        // ~100 meters
        BigDecimal lat1 = BigDecimal.valueOf(55.752538600041085);
        BigDecimal lng1 = BigDecimal.valueOf(37.617569067099765);
        BigDecimal lat2 = BigDecimal.valueOf(55.751464203009824);
        BigDecimal lng2 = BigDecimal.valueOf(37.61785960625385);

        var crmPrePickupPoint1 = crmPrePickupPointFactory.create(CrmPrePickupPointTestParamsBuilder.builder()
                .params(CrmPrePickupPointTestParams.builder()
                        .location(TestPickupPointFactory.PickupPointLocationTestParams.builder()
                                .lat(lat1)
                                .lng(lng1)
                                .build())
                        .brandingType(PickupPointBrandingType.FULL)
                        .build())
                .build());

        var crmPrePickupPoint2 = crmPrePickupPointFactory.create(CrmPrePickupPointTestParamsBuilder.builder()
                .params(CrmPrePickupPointTestParams.builder()
                        .status(REJECTED)
                        .location(TestPickupPointFactory.PickupPointLocationTestParams.builder()
                                .lat(BigDecimal.valueOf(50.0))
                                .lng(BigDecimal.valueOf(50.0))
                                .build())
                        .build())
                .build());

        assertThat(crmPrePickupPoint1.getLocation().getLocationWarning()).isNull();
        assertThat(crmPrePickupPoint2.getLocation().getLocationWarning()).isNull();

        crmPrePickupPoint2.getLocation().setLat(lat2);
        crmPrePickupPoint2.getLocation().setLng(lng2);

        crmPrePickupPoint2 = crmPrePickupPointCommandService.update(crmPrePickupPoint2);

        assertThat(crmPrePickupPoint2.getLocation().getLocationWarning())
                .isEqualTo("Нужно проверить локацию - рядом находится брендированный ПВЗ");
    }

    @Test
    void testUpdateBrandingDataParams() {
        CrmPrePickupPointParams crmPrePickupPoint = crmPrePickupPointFactory.create();
        crmPrePickupPointCommandService.changeStatus(crmPrePickupPoint.getId(), REJECTED, false);

        BigDecimal oldScoring = crmPrePickupPoint.getBrandingData().getScoring();
        CrmPrePickupPointBrandingData newBrandingData = new CrmPrePickupPointBrandingData(
                BigDecimal.valueOf(1234),
                BigDecimal.valueOf(56),
                "http://photo.url/asdasdasdasdasd",
                "some new comment",
                BigDecimal.valueOf(999),
                true, true, true,
                "POLYGON77"
        );

        crmPrePickupPoint.setBrandingData(newBrandingData);
        crmPrePickupPoint = crmPrePickupPointCommandService.update(crmPrePickupPoint);

        assertThat(crmPrePickupPoint.getBrandingData()).isEqualToIgnoringGivenFields(newBrandingData, "scoring");
        assertThat(crmPrePickupPoint.getBrandingData().getScoring()).isEqualTo(oldScoring);
    }

    @Test
    void testChangeStatusProperly() {
        var crmPrePickupPoint = crmPrePickupPointFactory.create(CrmPrePickupPointTestParamsBuilder.builder()
                .params(CrmPrePickupPointTestParams.builder()
                        .status(PrePickupPointApproveStatus.CHECKING)
                        .build())
                .build());

        crmPrePickupPoint = crmPrePickupPointCommandService.changeStatus(
                crmPrePickupPoint.getId(),
                PrePickupPointApproveStatus.APPROVED,
                false
        );

        assertThat(crmPrePickupPoint.getStatus()).isEqualTo(PrePickupPointApproveStatus.APPROVED);
        assertThat(crmPrePickupPointQueryService.getById(crmPrePickupPoint.getId()).getStatus())
                .isEqualTo(PrePickupPointApproveStatus.APPROVED);
    }

    @Test
    void testChangeStatusImproperly() {
        var crmPrePickupPoint = crmPrePickupPointFactory.create(CrmPrePickupPointTestParamsBuilder.builder()
                .params(CrmPrePickupPointTestParams.builder()
                        .status(PrePickupPointApproveStatus.CHECKING)
                        .build())
                .build());

        assertThatThrownBy(() -> crmPrePickupPointCommandService.changeStatus(
                crmPrePickupPoint.getId(),
                PrePickupPointApproveStatus.VERIFYING_RENOVATION,
                false
        ));
    }

    @Test
    void testStartRenovation() {
        var crmPrePickupPoint = crmPrePickupPointFactory.create(CrmPrePickupPointTestParamsBuilder.builder()
                .params(CrmPrePickupPointTestParams.builder()
                        .status(PrePickupPointApproveStatus.LOCATION_BOOKED)
                        .build())
                .build());

        clock.setFixed(DateTimeUtil.atStartOfDay(RENOVATION_START_DATE), DateTimeUtil.DEFAULT_ZONE_ID);

        CrmPrePickupPointParams params = crmPrePickupPointCommandService.startRenovation(crmPrePickupPoint.getId());
        assertThat(params.getRenovationStartDate()).isEqualTo(RENOVATION_START_DATE);
        assertThat(params.getStatus()).isEqualTo(PrePickupPointApproveStatus.UNDER_RENOVATION);

        CrmPrePickupPointParams saved = crmPrePickupPointQueryService.getById(crmPrePickupPoint.getId());
        assertThat(saved.getRenovationStartDate()).isEqualTo(RENOVATION_START_DATE);
        assertThat(saved.getStatus()).isEqualTo(PrePickupPointApproveStatus.UNDER_RENOVATION);
    }

    @Test
    void testNotStartRenovationFromInvalidStatus() {
        var crmPrePickupPoint = crmPrePickupPointFactory.create(CrmPrePickupPointTestParamsBuilder.builder()
                .params(CrmPrePickupPointTestParams.builder()
                        .status(PrePickupPointApproveStatus.CHECKING)
                        .build())
                .build());

        assertThatThrownBy(() -> crmPrePickupPointCommandService.startRenovation(crmPrePickupPoint.getId()));
    }

    @Test
    void testVerifyRenovation() {
        var crmPrePickupPoint = crmPrePickupPointFactory.create(CrmPrePickupPointTestParamsBuilder.builder()
                .params(CrmPrePickupPointTestParams.builder()
                        .status(PrePickupPointApproveStatus.UNDER_RENOVATION)
                        .build())
                .build());

        clock.setFixed(DateTimeUtil.atStartOfDay(RENOVATION_FINISH_DATE), DateTimeUtil.DEFAULT_ZONE_ID);

        CrmPrePickupPointParams params = crmPrePickupPointCommandService.verifyRenovation(crmPrePickupPoint.getId());
        assertThat(params.getRenovationFinishDate()).isEqualTo(RENOVATION_FINISH_DATE);
        assertThat(params.getStatus()).isEqualTo(PrePickupPointApproveStatus.VERIFYING_RENOVATION);

        CrmPrePickupPointParams saved = crmPrePickupPointQueryService.getById(crmPrePickupPoint.getId());
        assertThat(saved.getRenovationFinishDate()).isEqualTo(RENOVATION_FINISH_DATE);
        assertThat(saved.getStatus()).isEqualTo(PrePickupPointApproveStatus.VERIFYING_RENOVATION);
    }

    @Test
    void testNotVerifyRenovationFromInvalidStatus() {
        var crmPrePickupPoint = crmPrePickupPointFactory.create(CrmPrePickupPointTestParamsBuilder.builder()
                .params(CrmPrePickupPointTestParams.builder()
                        .status(PrePickupPointApproveStatus.LOCATION_BOOKED)
                        .build())
                .build());

        assertThatThrownBy(() -> crmPrePickupPointCommandService.verifyRenovation(crmPrePickupPoint.getId()));
    }
}
