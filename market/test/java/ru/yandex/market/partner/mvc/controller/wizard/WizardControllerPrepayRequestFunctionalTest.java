package ru.yandex.market.partner.mvc.controller.wizard;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.core.schedule.ScheduleDTO;
import ru.yandex.market.core.wizard.experiment.WizardExperimentService;
import ru.yandex.market.core.wizard.experiment.WizardExperimentsConfig;
import ru.yandex.market.core.wizard.model.WizardStepStatus;
import ru.yandex.market.core.wizard.model.WizardStepType;
import ru.yandex.market.core.wizard.step.PrepayRequestStepStatusCalculator;
import ru.yandex.market.core.wizard.step.dto.PartnerContractOptionWizardDto;
import ru.yandex.market.mbi.api.billing.client.MbiBillingClient;
import ru.yandex.market.mbi.api.billing.client.model.PayoutFrequencyDTO;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Функциональные тесты для шага wizard'a "плашка модель + способ подтверждения заказа".
 * См {@link PrepayRequestStepStatusCalculator}
 */
@DbUnitDataSet(before = "csv/commonBlueWizardData.before.csv")
class WizardControllerPrepayRequestFunctionalTest extends AbstractWizardControllerFunctionalTest {

    @Autowired
    private MbiBillingClient mbiBillingClient;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private WizardExperimentService disablePrepayStepExperiment;

    @BeforeEach
    void before() {
        environmentService.setValue(WizardExperimentsConfig.DISABLE_PREPAY_STEP_VAR, "1");
        disablePrepayStepExperiment.close();
    }

    @Test
    @DisplayName("Fulfillment")
    void testFulfillment() {
        var response = requestStep(FULFILLMENT_CAMPAIGN_ID, WizardStepType.SUPPLIER_INFO);
        JsonTestUtil.assertEquals(response, this.getClass(),
                "json/WizardControllerPrepayRequestFunctionalTest.testFulfillment.response.json");
    }

    @Test
    @DisplayName("Click and collect")
    void tesClickAndCollect() {
        var response = requestStep(DS_CLICK_AND_COLLECT_CAMPAIGN_ID, WizardStepType.SUPPLIER_INFO);
        JsonTestUtil.assertEquals(response, this.getClass(),
                "json/WizardControllerPrepayRequestFunctionalTest.testClickAndCollect.response.json");
    }

    @Test
    @DisplayName("Dropship с подтвержденной заявкой, но без назначенной выплаты")
    void testDropshipWithCompletedRequestAndNoPayout() {
        mockGetApplication(
                2108,
                "{\n" +
                        "  \"partnerId\": 2108,\n" +
                        "  \"phone\": \"89164490000\",\n" +
                        "  \"status\": \"DONE\",\n" +
                        "  \"inn\": \"1234567\",\n" +
                        "  \"fnsRequestId\": \"123456789\",\n" +
                        "  \"createdDate\":\"2022-05-16T21:00:00Z\"," +
                        "  \"updatedDate\":\"2022-05-16T21:00:00Z\"" +
                        "}"
        );
        when(mbiBillingClient.getCurrentAndNextMonthPayoutFrequencies(any()))
                .thenReturn(List.of(
                        createFrequencyDTO(21080, PayoutFrequencyDTO.DAILY, PayoutFrequencyDTO.DAILY, true),
                        createFrequencyDTO(21090, PayoutFrequencyDTO.WEEKLY, PayoutFrequencyDTO.DAILY, false)
                ));

        var response = requestStep(DS_WITH_COMPLETED_REQ, WizardStepType.SUPPLIER_INFO);

        //Несмотря на то, что выплата пришла только по контракту 21090, контракт 21080 тоже показывается в списке
        JsonTestUtil.assertEquals(response, this.getClass(),
                "json/WizardControllerPrepayRequestFunctionalTest.testDropshipWithCompletedRequestAndNoPayout.response.json");
    }

    @Test
    @DisplayName("Dropship с подтвержденной заявкой, но без контракта - для него выплата необязательна")
    @DbUnitDataSet(before = "csv/testDropshipWithCompletedRequestAndNoContract.before.csv")
    void testDropshipWithCompletedRequestAndNoContract() {
        var response = requestStep(DS_WITH_REQUEST_BUT_WITHOUT_CONTRACT,
                WizardStepType.SUPPLIER_INFO);

        JsonTestUtil.assertEquals(response, this.getClass(),
                "json/WizardControllerPrepayRequestFunctionalTest.testDropshipWithCompletedRequestAndNoContract.response.json");
    }

    @Test
    @DisplayName("Dropship с подтвержденной заявкой и назначенной выплатой")
    @DbUnitDataSet(before = "csv/testDropshipWithCompletedRequestAndPayout.before.csv")
    void testDropshipWithCompletedRequestAndPayout() {
        mockGetApplication(
                2108,
                "{\n" +
                        "  \"partnerId\": 2108,\n" +
                        "  \"phone\": \"89164490000\",\n" +
                        "  \"status\": \"DONE\",\n" +
                        "  \"inn\": \"1234567\",\n" +
                        "  \"fnsRequestId\": \"123456789\",\n" +
                        "  \"createdDate\":\"2022-05-16T21:00:00Z\"," +
                        "  \"updatedDate\":\"2022-05-16T21:00:00Z\"" +
                        "}"
        );
        when(mbiBillingClient.getCurrentAndNextMonthPayoutFrequencies(any()))
                .thenReturn(List.of(
                        createFrequencyDTO(21080, PayoutFrequencyDTO.DAILY, PayoutFrequencyDTO.WEEKLY, false),
                        createFrequencyDTO(21090, PayoutFrequencyDTO.MONTHLY, PayoutFrequencyDTO.BIWEEKLY, true)
                ));

        var response = requestStep(DS_WITH_COMPLETED_REQ, WizardStepType.SUPPLIER_INFO);

        //Когда шаг заполнен - contracts не присылаем
        JsonTestUtil.assertEquals(response, this.getClass(),
                "json/WizardControllerPrepayRequestFunctionalTest.testDropshipWithCompletedRequestAndPayout.response.json");
    }


    @Test
    @DisplayName("Принимает заказы через через партнерский интерфейс")
    void testCpa() {
        var response = requestStep(DROPSHIP_WITH_CPA_PARTNER_INTERFACE_CAMPAIGN_ID,
                WizardStepType.SUPPLIER_INFO);
        JsonTestUtil.assertEquals(response, this.getClass(),
                "json/WizardControllerPrepayRequestFunctionalTest.testCpa.response.json");
    }

    @Test
    @DisplayName("Проверка - заявка не создана")
    @DbUnitDataSet(before = "csv/testRequestStatus.before.csv")
    void testEmptyRequestNotFound() {
        var response = requestStep(13000L, WizardStepType.SUPPLIER_INFO);
        assertResponse(response, makeResponseStepStatus(WizardStepType.SUPPLIER_INFO, Status.EMPTY, "fulfillment",
                false, false));
    }

    @Test
    @DisplayName("Проверка - заявка не скачана")
    @DbUnitDataSet(before = "csv/testRequestStatus.before.csv")
    void testEmpty() {
        var response = requestStep(13001L, WizardStepType.SUPPLIER_INFO);
        JsonTestUtil.assertEquals(response, this.getClass(),
                "json/WizardControllerPrepayRequestFunctionalTest.testEmpty.response.json");
    }

    @Test
    @DisplayName("Проверка - заявка не подана")
    @DbUnitDataSet(before = "csv/testRequestStatus.before.csv")
    void testFilled() {
        var response = requestStep(13002L, WizardStepType.SUPPLIER_INFO);
        JsonTestUtil.assertEquals(response, this.getClass(),
                "json/WizardControllerPrepayRequestFunctionalTest.testFilled.response.json");
    }

    @Test
    @DisplayName("Проверка - заявка в процессе")
    @DbUnitDataSet(before = "csv/testRequestStatus.before.csv")
    void testEnabling() {
        var response = requestStep(13003L, WizardStepType.SUPPLIER_INFO);
        JsonTestUtil.assertEquals(response, this.getClass(),
                "json/WizardControllerPrepayRequestFunctionalTest.testEnabling.response.json");
    }

    @Test
    @DisplayName("Проверка - заявка отклонена")
    @DbUnitDataSet(before = "csv/testRequestStatus.before.csv")
    void testFailed() {
        var response = requestStep(13004L, WizardStepType.SUPPLIER_INFO);
        JsonTestUtil.assertEquals(response, this.getClass(),
                "json/WizardControllerPrepayRequestFunctionalTest.testFailed.response.json");
    }

    @Test
    @DisplayName("DBS - Проверка - заявка в процессе")
    @DbUnitDataSet(before = "csv/testRequestStatus.before.csv")
    void testEnablingForDsbs() {
        var response = requestStep(13010L, WizardStepType.PAYMENTS_REQUEST);
        JsonTestUtil.assertEquals(response, this.getClass(),
                "json/WizardControllerPrepayRequestFunctionalTest.testEnablingForDsbs.response.json");
    }

    @Test
    @DisplayName("самозанятый, Мой Налог не пройден")
    @DbUnitDataSet(before = "csv/testRequestStatus.before.csv")
    void testNoneSelfEmployedIsNotReady() {
        mockGetApplication(
                2108,
                "{\n" +
                        "  \"partnerId\": 2108,\n" +
                        "  \"phone\": \"89164490000\",\n" +
                        "  \"status\": \"NEW\",\n" +
                        "  \"inn\": \"1234567\",\n" +
                        "  \"fnsRequestId\": \"123456789\",\n" +
                        "  \"createdDate\":\"2022-05-16T21:00:00Z\"," +
                        "  \"updatedDate\":\"2022-05-16T21:00:00Z\"" +
                        "}"
        );
        var response = requestStep(12108L, WizardStepType.SUPPLIER_INFO);
        JsonTestUtil.assertEquals(
                response, this.getClass(),
                "json/WizardControllerPrepayRequestFunctionalTest.testNoneSelfEmployedNotReady.response.json"
        );
    }

    @Test
    @DisplayName("самозанятый, Мой Налог пройден")
    @DbUnitDataSet(before = "csv/testRequestStatus.before.csv")
    void testSelfEmployedIsReady() {
        mockGetApplication(
                2108,
                "{\n" +
                        "  \"partnerId\": 2108,\n" +
                        "  \"phone\": \"89164490000\",\n" +
                        "  \"status\": \"DONE\",\n" +
                        "  \"inn\": \"1234567\",\n" +
                        "  \"fnsRequestId\": \"123456789\",\n" +
                        "  \"createdDate\":\"2022-05-16T21:00:00Z\"," +
                        "  \"updatedDate\":\"2022-05-16T21:00:00Z\"" +
                        "}"
        );
        var response = requestStep(12108L, WizardStepType.SUPPLIER_INFO);
        JsonTestUtil.assertEquals(
                response, this.getClass(),
                "json/WizardControllerPrepayRequestFunctionalTest.testSelfEmployedIsReady.response.json"
        );
    }

    private static WizardStepStatus makeResponseStepStatus(
            WizardStepType stepType,
            Status status,
            String deliveryServiceType,
            boolean isCpaPartnerInterface,
            boolean isClickAndCollect
    ) {
        return makeResponseStepStatus(
                stepType,
                status,
                deliveryServiceType,
                isCpaPartnerInterface,
                isClickAndCollect,
                null,
                null,
                null,
                true
        );
    }

    private static WizardStepStatus makeResponseStepStatus(
            WizardStepType stepType,
            Status status,
            String deliveryServiceType,
            boolean isCpaPartnerInterface,
            boolean isClickAndCollect,
            @Nullable
                    Long currentContractId,
            @Nullable
                    List<PartnerContractOptionWizardDto> partnerContractOptions,
            @Nullable
                    ScheduleDTO scheduleDTO,
            boolean payoutFrequencyEnabled
    ) {
        Map<String, Object> details = new HashMap<>();

        details.put("deliveryServiceType", deliveryServiceType);
        details.put("isCpaPartnerInterface", isCpaPartnerInterface);
        details.put("isClickAndCollect", isClickAndCollect);
        details.put("contracts", Collections.emptyList());

        if (payoutFrequencyEnabled) {
            details.put("currentContractId", currentContractId);
        }
        if (partnerContractOptions != null) {
            details.put("contracts", partnerContractOptions);
        }

        if (scheduleDTO != null) {
            details.put("cpaSchedule", scheduleDTO);
        }

        details.put("payoutFrequencyEnabled", payoutFrequencyEnabled);

        return WizardStepStatus.newBuilder()
                .withStep(stepType)
                .withStatus(status)
                .withDetails(details)
                .build();
    }
}
