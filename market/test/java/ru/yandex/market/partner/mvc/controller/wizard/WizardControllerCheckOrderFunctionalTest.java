package ru.yandex.market.partner.mvc.controller.wizard;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.abo.api.client.AboPublicRestClient;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderDTO;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioDTO;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioStatus;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.offer.mapping.MboMappingService;
import ru.yandex.market.core.offer.mapping.MboMappingServiceException;
import ru.yandex.market.core.offer.mapping.OfferProcessingStatus;
import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.core.wizard.SupplierCheckOrderAssortmentRequirement;
import ru.yandex.market.core.wizard.model.WizardStepStatus;
import ru.yandex.market.core.wizard.model.WizardStepType;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.partner.mvc.controller.wizard.utils.DatacampFlagResponseMocker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * Функциональные тесты для шага wizard'a "Шаг контрольный заказ".
 * См {@link ru.yandex.market.core.wizard.step.CheckOrderStatusCalculator}
 */
@DbUnitDataSet(before = {"csv/commonBlueWizardData.before.csv",
                         "csv/partnerOnboardingUseAssortmentCalculator.csv"})
class WizardControllerCheckOrderFunctionalTest extends AbstractWizardControllerFunctionalTest {
    static final long DS_SUPPLIER_ID = 2100L;
    static final long DS_NOT_IN_INDEX_ID = 2106L;

    @Autowired
    AboPublicRestClient aboPublicRestClient;

    @Autowired
    @Qualifier("environmentService")
    EnvironmentService environmentService;

    @Autowired
    MboMappingService mboMappingService;

    @Autowired
    SupplierCheckOrderAssortmentRequirement supplierCheckOrderAssortmentRequirement;

    @Autowired
    @Qualifier("dataCampShopClient")
    private DataCampClient dataCampShopClient;

    private DatacampFlagResponseMocker datacampMocker;

    @BeforeEach
    void setUp() {
        supplierCheckOrderAssortmentRequirement.close(); // clear cache
        datacampMocker = new DatacampFlagResponseMocker(dataCampShopClient);
    }

    @Test
    @DisplayName("Шаг недоступен для ФФ")
    void notAvailableForFulfillment() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> requestStep(FULFILLMENT_CAMPAIGN_ID, WizardStepType.TEST_ORDER)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    @DisplayName("Шаг недоступен для Click and collect")
    void notAvailableForClickAndCollect() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> requestStep(DS_CLICK_AND_COLLECT_CAMPAIGN_ID, WizardStepType.TEST_ORDER)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    @DisplayName("Кроссдок - Отсутствует подтвержденное заявление. КЗ - NONE")
    void crossdockCheckOrderNoApplicationStatusNone() {
        mockLastCheckOrder(DS_SUPPLIER_ID, CheckOrderScenarioStatus.NEW);
        var response = requestStep(CROSSDOCK_NO_APPLICATION_CAMPAIGN_ID, WizardStepType.TEST_ORDER);
        assertResponse(response, makeResponseStepStatus(Status.NONE));
    }

    @Test
    @DisplayName("DONT_WANT - статус фичи")
    void crossdockDontWant() {
        var response = requestStep(12604L, WizardStepType.TEST_ORDER);
        assertResponse(response, makeResponseStepStatus(Status.NONE));
    }

    @Test
    @DisplayName("Кроссдок - ограничение - слишком маленький ассортимент")
    @DbUnitDataSet(before = "csv/testCheckOrderFilled.csv")
    void crossdockCheckOrderLimitationAssortmentTooSmall() throws MboMappingServiceException {
        // given
        environmentService.setValue(SupplierCheckOrderAssortmentRequirement.VAR, "10");
        doReturn(Map.of(
                // просто какие-то статусы для разнообразия
                OfferProcessingStatus.NEED_MAPPING, 3,
                OfferProcessingStatus.READY, 3,
                OfferProcessingStatus.UNKNOWN, 3
        )).when(mboMappingService).getCountByOfferProcessingStatuses(anyLong(), any());
        mockSaaSWithStocks(1);

        // when
        var response = requestStep(CROSSDOCK_SUPPLIER_CAMPAIGN_ID, WizardStepType.TEST_ORDER);

        // then
        // просто для удобства
        assertResponse(response, WizardStepStatus.newBuilder()
                .withStep(WizardStepType.TEST_ORDER)
                .withDetails(Map.of(
                        "isPartnerInterface", false,
                        "attempts", 0,
                        "offersCurrent", 9,
                        "offersRequired", 10
                ))
                .withStatus(Status.NONE)
                .build());
    }

    @Test
    @DisplayName("Кроссдок - ограничение - достаточный ассортимент")
    @DbUnitDataSet(before = "csv/testCheckOrderFilled.csv")
    void crossdockCheckOrderLimitationAssortmentJustEnough() throws MboMappingServiceException {
        // given
        environmentService.setValue(SupplierCheckOrderAssortmentRequirement.VAR, "10");
        doReturn(Map.of(
                // просто какие-то статусы для разнообразия
                OfferProcessingStatus.IN_WORK, 1,
                OfferProcessingStatus.NEED_MAPPING, 3,
                OfferProcessingStatus.READY, 3,
                OfferProcessingStatus.UNKNOWN, 3
        )).when(mboMappingService).getCountByOfferProcessingStatuses(anyLong(), any());
        mockSaaSWithStocks(1);

        // when
        var response = requestStep(CROSSDOCK_SUPPLIER_CAMPAIGN_ID, WizardStepType.TEST_ORDER);

        // then
        // просто для удобства
        assertResponse(response, WizardStepStatus.newBuilder()
                .withStep(WizardStepType.TEST_ORDER)
                .withDetails(Map.of(
                        "isPartnerInterface", false,
                        "attempts", 0,
                        "offersCurrent", 10,
                        "offersRequired", 10
                ))
                .withStatus(Status.EMPTY)
                .build());
    }

    @Test
    @DisplayName("Кроссдок - фича в SUCCESS, нет эксперимента, нет ограничения на ассортимент - FULL")
    @DbUnitDataSet(before = "csv/testAllStepsForCrossdockSupplier.csv")
    void crossdockCheckOrderStatusFull() {
        mockMboMappingService(SupplierOffer.OfferProcessingStatus.READY);
        mockSaaSWithStocks(1);

        var response = requestStep(CROSSDOCK_SUPPLIER_CAMPAIGN_ID, WizardStepType.TEST_ORDER);
        assertResponse(response, makeResponseStepStatus(Status.FULL));
    }

    @Test
    @DisplayName("Кроссдок - АБО еще не убрал флаг эксперимента у фичи CROSSDOCK - EMPTY")
    @DbUnitDataSet(before = "csv/testCheckOrderFilled.csv")
    void crossdockSuccessUnderExperiment() {
        mockMboMappingService(SupplierOffer.OfferProcessingStatus.READY);
        mockSaaSWithStocks(1);

        var response = requestStep(CROSSDOCK_SUPPLIER_CAMPAIGN_ID, WizardStepType.TEST_ORDER);
        assertResponse(response, makeResponseStepStatus(Status.EMPTY));
    }

    @Test
    @DbUnitDataSet(before = "csv/crossdockTestOrderFailed.csv")
    @DisplayName("Кроссдок - Неудачное прохождение КЗ")
    void crossdockFailedStatus() {
        mockMboMappingService(SupplierOffer.OfferProcessingStatus.READY);
        mockSaaSWithStocks(1);
        when(aboPublicRestClient.getAvailableCheckOrderAttempts(1L)).thenReturn(5);

        var response = requestStep(111L, WizardStepType.TEST_ORDER);
        assertResponse(response, makeResponseStepStatus(Status.FAILED, false, 5));
    }

    @Test
    @DbUnitDataSet(before = "csv/crossdockTestOrderFailed.csv")
    @DisplayName("Кроссдок - Неудачное прохождение КЗ")
    void crossdockFailedNewStatus() {
        mockMboMappingService(SupplierOffer.OfferProcessingStatus.READY);
        mockSaaSWithStocks(1);
        when(aboPublicRestClient.getAvailableCheckOrderAttempts(2L)).thenReturn(3);

        var response = requestStep(222L, WizardStepType.TEST_ORDER);
        assertResponse(response, makeResponseStepStatus(Status.FAILED, false, 3));
    }

    private void mockLastCheckOrder(long shopId, CheckOrderScenarioStatus status) {
        when(aboPublicRestClient.getLastCheckOrder(shopId)).thenReturn(
                new CheckOrderDTO(shopId, CheckOrderScenarioDTO.builder(1L)
                        .withCreationTimestamp(Instant.now().getEpochSecond())
                        .withStatus(status)
                        .build())
        );
    }

    private static WizardStepStatus makeResponseStepStatus(Status status) {
        // просто для удобства
        return makeResponseStepStatus(status, false, 0);
    }

    private static WizardStepStatus makeResponseStepStatus(
            Status status,
            boolean isPartnerInterface,
            int attempts
    ) {
        return WizardStepStatus.newBuilder()
                .withStep(WizardStepType.TEST_ORDER)
                .withDetails(makeResponseDetails(isPartnerInterface, attempts))
                .withStatus(status)
                .build();
    }

    private static Map<String, Serializable> makeResponseDetails(
            boolean isPartnerInterface,
            int attempts
    ) {
        return Map.of(
                "isPartnerInterface", isPartnerInterface,
                "attempts", attempts,
                "offersCurrent", 0,
                "offersRequired", 0
        );
    }
}
