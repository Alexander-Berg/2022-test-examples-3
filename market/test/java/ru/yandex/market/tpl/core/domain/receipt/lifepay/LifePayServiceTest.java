package ru.yandex.market.tpl.core.domain.receipt.lifepay;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.tpl.api.model.receipt.ReceiptDataType;
import ru.yandex.market.tpl.api.model.receipt.ReceiptFfdVersion;
import ru.yandex.market.tpl.api.model.receipt.lifepay.LifePayProcessingResultDto;
import ru.yandex.market.tpl.api.model.receipt.lifepay.LifePayReceiptType;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.receipt.ReceiptData;
import ru.yandex.market.tpl.core.domain.receipt.ReceiptDataRepository;
import ru.yandex.market.tpl.core.domain.receipt.ReceiptDataRepositoryTest;
import ru.yandex.market.tpl.core.domain.receipt.ReceiptHelper;
import ru.yandex.market.tpl.core.domain.tracker.TrackerService;
import ru.yandex.market.tpl.core.external.lifepay.LifePayClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author valter
 */
@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@CoreTest
class LifePayServiceTest {

    public static final String LIFE_PAY_FISCAL_REQUEST_UUID = "my-uuid";
    public static final String NEW_LIFE_PAY_FISCAL_REQUEST_UUID = "new-my-uuid";
    private static final String SHIFT_NUM = UUID.randomUUID().toString();
    private static final String KKM_SN = UUID.randomUUID().toString();
    private static final String FN_SN = UUID.randomUUID().toString();
    private final ReceiptDataRepository receiptDataRepository;
    private final LifePayService lifePayService;
    private final LifePayFiscalRequestRepository lifePayFiscalRequestRepository;
    private final ReceiptHelper receiptHelper;
    @MockBean
    private LifePayClient lifePayClient;

    @MockBean
    private TrackerService trackerService;

    private ReceiptData receiptData;

    @BeforeEach
    void init() {
        doReturn(LIFE_PAY_FISCAL_REQUEST_UUID).when(lifePayClient).createReceipt(any());
        doReturn(LIFE_PAY_FISCAL_REQUEST_UUID).when(lifePayClient).createReceiptFfd12(any());
        receiptData = receiptDataRepository.save(ReceiptDataRepositoryTest.filledData(ReceiptDataType.INCOME,
                "Иванов\tИван Иваныч\n\r"));
    }

    @Test
    void makeLifePayRequest() {
        lifePayService.registerCheque(receiptData);
        verify(lifePayClient).createReceipt(
                argThat(argument -> argument.getExtId().equals(String.valueOf(receiptData.getId()))
                        && argument.getCustomerName().equals("Иванов Иван Иваныч"))
        );
        assertThat(lifePayFiscalRequestRepository.findByReceiptData(receiptData).isPresent()).isTrue();
    }

    @Test
    void makeLifePayRequest_ffd_1_2() {
        receiptHelper.setFfdVersion(receiptData, ReceiptFfdVersion.FFD_1_2);
        lifePayService.registerCheque(receiptData);
        verify(lifePayClient).createReceiptFfd12(
                argThat(argument -> argument.getExtId().equals(String.valueOf(receiptData.getId()))
                        && argument.getCustomerName().equals("Иванов Иван Иваныч"))
        );
        assertThat(lifePayFiscalRequestRepository.findByReceiptData(receiptData).isPresent()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(ReceiptFfdVersion.class)
    void createFiscalDataTwiceNoException(ReceiptFfdVersion ffdVersion) {
        receiptHelper.setFfdVersion(receiptData, ffdVersion);
        lifePayService.registerCheque(receiptData);
        LifePayProcessingResultDto fiscalData = receiptHelper.createLifePayProcessingResultDto(
                LifePayReceiptType.buy, "my-uuid");
        lifePayService.createFiscalData(fiscalData);
        lifePayService.createFiscalData(fiscalData);
    }

    @ParameterizedTest
    @EnumSource(ReceiptFfdVersion.class)
    void makeLifePayRequestTwiceNoException(ReceiptFfdVersion ffdVersion) {
        receiptHelper.setFfdVersion(receiptData, ffdVersion);
        lifePayService.registerCheque(receiptData);
        lifePayService.registerCheque(receiptData);
    }

    @Test
    void checkMonitoringXReportsNoDifferences_whenFnSnCanBeSkipped() {
        //given
        createIncomeFiscalData("", BigDecimal.valueOf(111));
        createIncomeFiscalData(FN_SN, BigDecimal.valueOf(222));
        BigDecimal expectedSum = BigDecimal.valueOf(333);

        //when
        lifePayService.processReport(createXReportResultDto(expectedSum));

        //then
        verify(trackerService, never()).createLifePayAlertTicket(any());
    }

    @Test
    void checkMonitoringZReportsNoDifferences_whenFnSnCanBeSkipped() {
        //given
        createIncomeFiscalData("", BigDecimal.valueOf(111));
        createIncomeFiscalData(FN_SN, BigDecimal.valueOf(222));
        BigDecimal expectedSum = BigDecimal.valueOf(333);

        //when
        lifePayService.processReport(createZReportResultDto(expectedSum));

        //then
        verify(trackerService, never()).createLifePayAlertTicket(any());
    }

    @Test
    void checkMonitoringXReports_whenNeedsAlert() {
        //given
        createIncomeFiscalData("", BigDecimal.valueOf(111));
        createIncomeFiscalData(FN_SN, BigDecimal.valueOf(222));
        BigDecimal notExpectedSum = BigDecimal.valueOf(555);

        //when
        lifePayService.processReport(createXReportResultDto(notExpectedSum));

        //then
        verify(trackerService, times(1)).createLifePayAlertTicket(any());
    }

    @Test
    void checkMonitoringZReports_whenNeedsAlert() {
        //given
        createIncomeFiscalData("", BigDecimal.valueOf(111));
        createIncomeFiscalData(FN_SN, BigDecimal.valueOf(222));
        BigDecimal notExpectedSum = BigDecimal.valueOf(555);

        //when
        lifePayService.processReport(createZReportResultDto(notExpectedSum));

        //then
        verify(trackerService, times(1)).createLifePayAlertTicket(any());
    }

    @DisplayName("При повторной регистрации чека приходит новый uuid чека и мы его сохраняем")
    @ParameterizedTest
    @EnumSource(ReceiptFfdVersion.class)
    void testChangeUuid(ReceiptFfdVersion ffdVersion) {
        receiptHelper.setFfdVersion(receiptData, ffdVersion);
        lifePayService.registerCheque(receiptData);
        doReturn(NEW_LIFE_PAY_FISCAL_REQUEST_UUID).when(lifePayClient).createReceipt(any());
        doReturn(NEW_LIFE_PAY_FISCAL_REQUEST_UUID).when(lifePayClient).createReceiptFfd12(any());

        lifePayService.registerCheque(receiptData);

        Optional<LifePayFiscalRequest> lifePayFiscalRequestOptional =
                lifePayFiscalRequestRepository.findByReceiptData(receiptData);
        assertThat(lifePayFiscalRequestOptional.isPresent()).isTrue();
        String uuidAfterUpdate = lifePayFiscalRequestOptional.get().getUuid();
        assertThat(uuidAfterUpdate).isEqualTo(NEW_LIFE_PAY_FISCAL_REQUEST_UUID);
    }

    private LifePayProcessingResultDto createXReportResultDto(BigDecimal cashTotal) {
        return createReportResultDto(cashTotal, LifePayProcessingResultDto.Command.X_REPORT);
    }

    private LifePayProcessingResultDto createZReportResultDto(BigDecimal cashTotal) {
        return createReportResultDto(cashTotal, LifePayProcessingResultDto.Command.Z_REPORT);
    }

    private LifePayProcessingResultDto createReportResultDto(BigDecimal cashTotal,
                                                             LifePayProcessingResultDto.Command command) {
        LifePayProcessingResultDto requestXReportDto = receiptHelper.createLifePayProcessingResultDto();
        requestXReportDto.setCommand(command);
        requestXReportDto.setFiscalData(createReportFiscalData(cashTotal));
        return requestXReportDto;
    }

    private void createIncomeFiscalData(String fnSn, BigDecimal cashTotal) {
        String uuid = UUID.randomUUID().toString();

        initRequestResponseLifePayData(uuid);

        initFiscalData(fnSn, cashTotal, uuid);
    }

    private void initFiscalData(String fnSn, BigDecimal cashTotal, String uuid) {
        LifePayProcessingResultDto lifePayProcessingResultDto = receiptHelper.createLifePayProcessingResultDto(
                LifePayReceiptType.buy, uuid);

        LifePayProcessingResultDto.FiscalData fiscalData = lifePayProcessingResultDto.getFiscalData();
        fiscalData.setCheckSession(SHIFT_NUM);
        fiscalData.setFnSn(fnSn);
        fiscalData.setKkmSn(KKM_SN);
        fiscalData.setFdNum(UUID.randomUUID().toString());
        fiscalData.setFdValue(UUID.randomUUID().toString());
        fiscalData.setSumm(cashTotal);

        lifePayService.createFiscalData(lifePayProcessingResultDto);
    }

    private void initRequestResponseLifePayData(String uuid) {
        ReceiptData nextReceiptData = receiptDataRepository.save(ReceiptDataRepositoryTest.filledData(uuid,
                ReceiptDataType.INCOME,
                receiptData.getServiceClient()));
        Mockito.when(lifePayClient.createReceipt(any())).thenReturn(uuid);
        lifePayService.registerCheque(nextReceiptData);
    }

    private LifePayProcessingResultDto.FiscalData createReportFiscalData(BigDecimal cashTotal) {
        LifePayProcessingResultDto.FiscalData fiscalData = new LifePayProcessingResultDto.FiscalData();
        fiscalData.setCheckSession(SHIFT_NUM);
        fiscalData.setKkmSn(KKM_SN);
        fiscalData.setFnSn(FN_SN);
        fiscalData.setFdNum(UUID.randomUUID().toString());
        fiscalData.setFdValue(UUID.randomUUID().toString());
        fiscalData.setIncomeTotal(new LifePayProcessingResultDto.SummTotal(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                cashTotal));

        fiscalData.setOutcomeTotal(new LifePayProcessingResultDto.SummTotal(BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO));

        fiscalData.setTaxSum(new LifePayProcessingResultDto.FiscalDataTaxSum());
        return fiscalData;
    }

}
