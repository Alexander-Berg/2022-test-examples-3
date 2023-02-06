package ru.yandex.market.pvz.core.domain.legal_partner.termination;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.client.model.partner.LegalPartnerTerminationDetails;
import ru.yandex.market.pvz.client.model.partner.LegalPartnerTerminationType;
import ru.yandex.market.pvz.client.model.partner.PickupPointTerminationDetails;
import ru.yandex.market.pvz.client.model.pickup_point.PickupPointBrandingType;
import ru.yandex.market.pvz.core.domain.configuration.global.ConfigurationGlobalCommandService;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartner;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartnerParams;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartnerQueryService;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointParams;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointQueryService;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.DeactivationReasonCommandService;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.DeactivationReasonRepository;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.PickupPointDeactivationCommandService;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestBrandRegionFactory;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerTerminationFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.tpl.common.db.exception.TplEntityNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.DEACTIVATION_WITH_REASONS;

@Slf4j
@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class LegalPartnerTerminationCommandServiceTest {

    private static final int UNKNOWN_LEGAL_PARTNER_ID = -1;
    private static final OffsetDateTime FROM_TIME = OffsetDateTime.now();

    private final TestLegalPartnerTerminationFactory terminationFactory;
    private final TestLegalPartnerFactory legalPartnerFactory;
    private final TestPickupPointFactory pickupPointFactory;
    private final TestBrandRegionFactory brandRegionFactory;

    private final LegalPartnerTerminationQueryService terminationQueryService;
    private final LegalPartnerQueryService legalPartnerQueryService;
    private final PickupPointQueryService pickupPointQueryService;
    private final DeactivationReasonCommandService deactivationReasonCommandService;
    private final PickupPointDeactivationCommandService deactivationCommandService;
    private final DeactivationReasonRepository deactivationReasonRepository;
    private final ConfigurationGlobalCommandService configurationGlobalCommandService;

    private LegalPartner legalPartner;
    private PickupPoint pickupPoint;
    private PickupPoint pickupPoint2;
    private PickupPoint pickupPoint3;
    private PickupPoint pickupPoint4;

    private LegalPartnerTerminationDetails expectedDetails;

    @BeforeEach
    void setup() {
        configurationGlobalCommandService.setValue(DEACTIVATION_WITH_REASONS, true);

        deactivationReasonCommandService.createDeactivationReason(LegalPartnerTerminationType.DEBT.getDescription(),
                "", false, false, null);
        deactivationReasonCommandService.createDeactivationReason(LegalPartnerTerminationType.CONTRACT_TERMINATED.getDescription(),
                "", false, false, null);

        brandRegionFactory.create(TestBrandRegionFactory.BrandRegionTestParams.builder()
                .region("Воронеж")
                .dailyTransmissionThreshold(5)
                .build());

        legalPartner = legalPartnerFactory.createLegalPartner();
        legalPartner = legalPartnerFactory
                .forceApprove(legalPartner.getId(), LocalDate.of(2021, 1, 1));

        pickupPoint = createPickupPoint(true, true, true, true, true, PickupPointBrandingType.FULL);
        pickupPoint2 = createPickupPoint(false, true, true, false, true, PickupPointBrandingType.FULL);
        pickupPoint3 = createPickupPoint(true, true, true, true, false, PickupPointBrandingType.NONE);
        pickupPoint4 = createPickupPoint(true, false, false, true, true, PickupPointBrandingType.NONE);

        expectedDetails = LegalPartnerTerminationDetails.builder()
                .pickupPointTerminationDetails(Map.of(
                        pickupPoint.getId(), buildExpectedDetails(true, true, true, true, true,
                                PickupPointBrandingType.FULL),
                        pickupPoint2.getId(), buildExpectedDetails(false, true, true, false, true,
                                PickupPointBrandingType.FULL),
                        pickupPoint3.getId(), buildExpectedDetails(true, true, true, true, false,
                                PickupPointBrandingType.NONE),
                        pickupPoint4.getId(), buildExpectedDetails(true, false, false, true, true,
                                PickupPointBrandingType.NONE)
                ))
                .build();
    }

    @Test
    void whenCreateDebtTypeTerminationThenSuccess() {
        assertThat(pickupPoint.getActualBrandData()).isNotNull();

        LegalPartnerTerminationParams termination = terminationFactory.createLegalPartnerTermination(
                TestLegalPartnerTerminationFactory.LegalPartnerTestParamsBuilder.builder()
                        .params(
                                TestLegalPartnerTerminationFactory.LegalPartnerTerminationTestParams
                                        .builder()
                                        .type(LegalPartnerTerminationType.DEBT)
                                        .fromTime(FROM_TIME)
                                        .legalPartnerId(legalPartner.getId())
                                        .build()
                        )
                        .build()
        );

        LegalPartnerTerminationParams terminationParamsDb = terminationQueryService.get(termination.getId());

        LegalPartnerTerminationParams expected = LegalPartnerTerminationParams.builder()
                .id(terminationParamsDb.getId())
                .legalPartnerId(legalPartner.getId())
                .type(LegalPartnerTerminationType.DEBT)
                .fromTime(FROM_TIME)
                .active(true)
                .details(expectedDetails)
                .build();
        assertThat(terminationParamsDb).isEqualTo(expected);

        LegalPartnerParams legalPartnerParamsDb = legalPartnerQueryService.get(legalPartner.getId());
        assertThat(legalPartnerParamsDb.getOfferTerminatedSince()).isNull();

        PickupPointParams pickupPointParams = pickupPointQueryService.getHeavy(pickupPoint.getId());
        assertDisabledWithBrandingType(pickupPointParams, PickupPointBrandingType.FULL);

        PickupPointParams pickupPointParams2 = pickupPointQueryService.getHeavy(pickupPoint2.getId());
        assertDisabledWithBrandingType(pickupPointParams2, PickupPointBrandingType.FULL);

        PickupPointParams pickupPointParams3 = pickupPointQueryService.getHeavy(pickupPoint3.getId());
        assertDisabledWithBrandingType(pickupPointParams3, PickupPointBrandingType.NONE);

        PickupPointParams pickupPointParams4 = pickupPointQueryService.getHeavy(pickupPoint4.getId());
        assertDisabledWithBrandingType(pickupPointParams4, PickupPointBrandingType.NONE);

    }

    @Test
    void whenCreateContractTerminatedTypeTerminationThenSuccess() {
        assertThat(pickupPoint.getActualBrandData()).isNotNull();

        LegalPartnerTerminationParams termination = terminationFactory.createLegalPartnerTermination(
                TestLegalPartnerTerminationFactory.LegalPartnerTestParamsBuilder.builder()
                        .params(
                                TestLegalPartnerTerminationFactory.LegalPartnerTerminationTestParams
                                        .builder()
                                        .type(LegalPartnerTerminationType.CONTRACT_TERMINATED)
                                        .fromTime(FROM_TIME)
                                        .legalPartnerId(legalPartner.getId())
                                        .build()
                        )
                        .build()
        );

        LegalPartnerTerminationParams terminationParamsDb = terminationQueryService.get(termination.getId());
        LegalPartnerTerminationParams expected = LegalPartnerTerminationParams.builder()
                .id(terminationParamsDb.getId())
                .legalPartnerId(legalPartner.getId())
                .type(LegalPartnerTerminationType.CONTRACT_TERMINATED)
                .fromTime(FROM_TIME)
                .active(true)
                .details(expectedDetails)
                .build();
        assertThat(terminationParamsDb).isEqualTo(expected);

        LegalPartnerParams legalPartnerParamsDb = legalPartnerQueryService.get(legalPartner.getId());
        assertThat(legalPartnerParamsDb.getOfferTerminatedSince()).isEqualTo(FROM_TIME.toLocalDate());

        PickupPointParams pickupPointParams = pickupPointQueryService.getHeavy(pickupPoint.getId());
        assertThat(pickupPointParams.getBrandingType()).isEqualTo(PickupPointBrandingType.FULL);
        assertThat(pickupPointParams.getBrandingData()).isNotNull();
    }

    @Test
    void whenCreateDebtTypeTerminationWithTheSameActiveTypeThenNothing() {
        terminationFactory.createLegalPartnerTermination(
                TestLegalPartnerTerminationFactory.LegalPartnerTestParamsBuilder.builder()
                        .params(
                                TestLegalPartnerTerminationFactory.LegalPartnerTerminationTestParams
                                        .builder()
                                        .type(LegalPartnerTerminationType.DEBT)
                                        .fromTime(FROM_TIME)
                                        .legalPartnerId(legalPartner.getId())
                                        .build()
                        )
                        .build()
        );

        assertThat(terminationFactory.createLegalPartnerTermination(
                TestLegalPartnerTerminationFactory.LegalPartnerTestParamsBuilder.builder()
                        .params(
                                TestLegalPartnerTerminationFactory.LegalPartnerTerminationTestParams
                                        .builder()
                                        .type(LegalPartnerTerminationType.DEBT)
                                        .legalPartnerId(legalPartner.getId())
                                        .build()
                        )
                        .build()
        )).isNull();
    }

    @Test
    void whenCreateContractTerminatedTypeTerminationWithTheSameActiveTypeThenNothing() {
        terminationFactory.createLegalPartnerTermination(
                TestLegalPartnerTerminationFactory.LegalPartnerTestParamsBuilder.builder()
                        .params(
                                TestLegalPartnerTerminationFactory.LegalPartnerTerminationTestParams
                                        .builder()
                                        .type(LegalPartnerTerminationType.CONTRACT_TERMINATED)
                                        .fromTime(FROM_TIME)
                                        .legalPartnerId(legalPartner.getId())
                                        .build()
                        )
                        .build()
        );

        assertThat(terminationFactory.createLegalPartnerTermination(
                TestLegalPartnerTerminationFactory.LegalPartnerTestParamsBuilder.builder()
                        .params(
                                TestLegalPartnerTerminationFactory.LegalPartnerTerminationTestParams
                                        .builder()
                                        .type(LegalPartnerTerminationType.CONTRACT_TERMINATED)
                                        .legalPartnerId(legalPartner.getId())
                                        .build()
                        )
                        .build()
        )).isNull();
    }

    @Test
    void whenCreateTerminationWithUnknownPartnerThenException() {
        Exception exception = assertThrows(TplEntityNotFoundException.class, () -> {
            terminationFactory.createLegalPartnerTermination(
                    TestLegalPartnerTerminationFactory.LegalPartnerTestParamsBuilder.builder()
                            .params(
                                    TestLegalPartnerTerminationFactory.LegalPartnerTerminationTestParams
                                            .builder()
                                            .type(LegalPartnerTerminationType.DEBT)
                                            .legalPartnerId(UNKNOWN_LEGAL_PARTNER_ID)
                                            .build()
                            )
                            .build()
            );
        });
        assertNotNull(exception.getMessage());
    }

    @Test
    void whenCancelTerminationThenSuccess() {
        LegalPartnerTerminationParams termination = terminationFactory.createLegalPartnerTermination(
                TestLegalPartnerTerminationFactory.LegalPartnerTestParamsBuilder.builder()
                        .params(
                                TestLegalPartnerTerminationFactory.LegalPartnerTerminationTestParams
                                        .builder()
                                        .type(LegalPartnerTerminationType.DEBT)
                                        .fromTime(FROM_TIME)
                                        .legalPartnerId(legalPartner.getId())
                                        .build()
                        )
                        .build()
        );

        PickupPointParams pickupPointParams = pickupPointQueryService.getHeavy(pickupPoint.getId());
        assertDisabledWithBrandingType(pickupPointParams, PickupPointBrandingType.FULL);

        PickupPointParams pickupPointParams2 = pickupPointQueryService.getHeavy(pickupPoint2.getId());
        assertDisabledWithBrandingType(pickupPointParams2, PickupPointBrandingType.FULL);

        PickupPointParams pickupPointParams3 = pickupPointQueryService.getHeavy(pickupPoint3.getId());
        assertDisabledWithBrandingType(pickupPointParams3, PickupPointBrandingType.NONE);

        PickupPointParams pickupPointParams4 = pickupPointQueryService.getHeavy(pickupPoint4.getId());
        assertDisabledWithBrandingType(pickupPointParams4, PickupPointBrandingType.NONE);

        terminationFactory.cancelPartnerTermination(
                TestLegalPartnerTerminationFactory.LegalPartnerTestParamsBuilder.builder()
                        .params(
                                TestLegalPartnerTerminationFactory.LegalPartnerTerminationTestParams
                                        .builder()
                                        .type(termination.getType())
                                        .active(false)
                                        .legalPartnerId(legalPartner.getId())
                                        .build()
                        )
                        .build()
        );

        pickupPointParams = pickupPointQueryService.getHeavy(pickupPoint.getId());
        assertPickupPointParams(pickupPointParams, PickupPointBrandingType.FULL, true, true, true, true, true);

        pickupPointParams2 = pickupPointQueryService.getHeavy(pickupPoint2.getId());
        assertPickupPointParams(pickupPointParams2, PickupPointBrandingType.FULL, true, true, true, true, true);

        pickupPointParams3 = pickupPointQueryService.getHeavy(pickupPoint3.getId());
        assertPickupPointParams(pickupPointParams3, PickupPointBrandingType.NONE, true, true, true, false, false);

        pickupPointParams4 = pickupPointQueryService.getHeavy(pickupPoint4.getId());
        assertPickupPointParams(pickupPointParams4, PickupPointBrandingType.NONE, false, false, true, true, false);
    }

    @Test
    void cancelContractTerminated() {
        assertThat(pickupPoint.getActualBrandData()).isNotNull();

        LegalPartnerTerminationParams termination = terminationFactory.createLegalPartnerTermination(
                TestLegalPartnerTerminationFactory.LegalPartnerTestParamsBuilder.builder()
                        .params(
                                TestLegalPartnerTerminationFactory.LegalPartnerTerminationTestParams
                                        .builder()
                                        .type(LegalPartnerTerminationType.CONTRACT_TERMINATED)
                                        .fromTime(FROM_TIME)
                                        .legalPartnerId(legalPartner.getId())
                                        .build()
                        )
                        .build()
        );

        terminationFactory.cancelPartnerTermination(
                TestLegalPartnerTerminationFactory.LegalPartnerTestParamsBuilder.builder()
                        .params(
                                TestLegalPartnerTerminationFactory.LegalPartnerTerminationTestParams
                                        .builder()
                                        .type(termination.getType())
                                        .active(false)
                                        .legalPartnerId(legalPartner.getId())
                                        .build()
                        )
                        .build()
        );

        LegalPartnerParams legalPartnerParamsDb = legalPartnerQueryService.get(legalPartner.getId());
        assertThat(legalPartnerParamsDb.getOfferTerminatedSince()).isNull();
    }

    private PickupPoint createPickupPoint(boolean active,
                                          boolean cardAllowed,
                                          boolean cashAllowed,
                                          boolean prepayAllowed,
                                          boolean returnAllowed,
                                          PickupPointBrandingType brandingType) {
        var pickupPoint = pickupPointFactory.createPickupPoint(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .legalPartner(legalPartner)
                        .build());
        var deactivationReason = deactivationReasonRepository
                .findFirstByReason(PickupPoint.DEFAULT_FIRST_DEACTIVATION_REASON).get();
        deactivationCommandService.cancelDeactivationManual(pickupPoint.getPvzMarketId(), deactivationReason.getId());
        return pickupPointFactory.updatePickupPoint(
                pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .active(active)
                        .cardAllowed(cardAllowed)
                        .cashAllowed(cashAllowed)
                        .prepayAllowed(prepayAllowed)
                        .returnAllowed(returnAllowed)
                        .brandingType(brandingType)
                        .brandDate(LocalDate.of(2021, 1, 5))
                        .brandRegion("Воронеж")
                        .build());
    }

    private PickupPointTerminationDetails buildExpectedDetails(boolean active,
                                                               boolean cardAllowed,
                                                               boolean cashAllowed,
                                                               boolean prepayAllowed,
                                                               boolean returnAllowed,
                                                               PickupPointBrandingType brandingType) {
        return PickupPointTerminationDetails.builder()
                .active(active)
                .cardAllowed(cardAllowed)
                .cashAllowed(cashAllowed)
                .prepayAllowed(prepayAllowed)
                .returnAllowed(returnAllowed)
                .onDemandAllowed(false)
                .dropOffFeature(false)
                .brandingType(brandingType)
                .build();
    }

    private void assertPickupPointParams(PickupPointParams actualParams,
                                         PickupPointBrandingType expectedBrandingType,
                                         boolean expectedCardAllowed,
                                         boolean expectedCashAllowed,
                                         boolean expectedPrepayAllowed,
                                         boolean expectedReturnAllowed,
                                         boolean isBrandingDataExists) {
        assertThat(actualParams.getBrandingType()).isEqualTo(expectedBrandingType);
        assertThat(actualParams.getCardAllowed()).isEqualTo(expectedCardAllowed);
        assertThat(actualParams.getCashAllowed()).isEqualTo(expectedCashAllowed);
        assertThat(actualParams.getPrepayAllowed()).isEqualTo(expectedPrepayAllowed);
        assertThat(actualParams.getReturnAllowed()).isEqualTo(expectedReturnAllowed);

        if (isBrandingDataExists) {
            assertThat(actualParams.getBrandingData()).isNotNull();
        } else {
            assertThat(actualParams.getBrandingData()).isNull();
        }
    }

    private void assertDisabledWithBrandingType(PickupPointParams actualParams, PickupPointBrandingType brandingType) {
        assertPickupPointParams(actualParams, brandingType, false, false, false, false, brandingType.isBrand());
    }

}
