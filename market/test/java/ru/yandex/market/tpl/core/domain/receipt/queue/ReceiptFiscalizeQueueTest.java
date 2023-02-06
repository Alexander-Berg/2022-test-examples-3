package ru.yandex.market.tpl.core.domain.receipt.queue;

import java.time.Duration;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import ru.yandex.market.tpl.api.model.receipt.FiscalReceiptStatus;
import ru.yandex.market.tpl.api.model.receipt.ReceiptDataType;
import ru.yandex.market.tpl.api.model.receipt.lifepay.LifePayReceiptType;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.receipt.ReceiptData;
import ru.yandex.market.tpl.core.domain.receipt.ReceiptFiscalDataRepository;
import ru.yandex.market.tpl.core.domain.receipt.ReceiptHelper;
import ru.yandex.market.tpl.core.domain.receipt.ReceiptProcessorType;
import ru.yandex.market.tpl.core.domain.receipt.ReceiptServiceClient;
import ru.yandex.market.tpl.core.domain.receipt.ReceiptServiceClientRepository;
import ru.yandex.market.tpl.core.domain.receipt.lifepay.LifePayService;
import ru.yandex.market.tpl.core.external.lifepay.LifePayClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author valter
 */
@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@CoreTest
@Import(ReceiptFiscalizeQueueTest.Config.class)
class ReceiptFiscalizeQueueTest {

    private static final String KKT_SN = "92387492364169";

    private final ReceiptHelper receiptHelper;
    private final DbQueueTestUtil dbQueueTestUtil;

    private final ReceiptServiceClientRepository receiptServiceClientRepository;
    private final ReceiptFiscalDataRepository receiptFiscalDataRepository;
    private final LifePayService lifePayService;

    @MockBean
    private LifePayClient lifePayClient;

    @TestConfiguration
    static class Config {

        @Bean
        Duration receiptFiscalizeReenqueueDelay() {
            return Duration.ofSeconds(1);
        }

    }

    private ReceiptServiceClient receiptServiceClient;

    @BeforeEach
    void init() {
        receiptServiceClient = receiptServiceClientRepository.save(new ReceiptServiceClient(
                "ReceiptHelper-test-client", "1234567890", ReceiptProcessorType.LIFE_PAY, KKT_SN
        ));
    }

    @Test
    void singleReceipt() {
        var receiptData = createReceiptData();
        checkReceiptFiscalization(receiptData);
    }

    @Test
    void receiptAndReturn() {
        var receiptDataIncome = createReceiptData(ReceiptDataType.INCOME);
        checkReceiptFiscalization(receiptDataIncome, "1");
        var receiptDataReturn = createReturnReceiptData(receiptDataIncome);
        checkReceiptFiscalization(receiptDataReturn, "2");
    }

    @Test
    void returnWaitsForBaseFiscalization() {
        var receiptDataIncome = createReceiptData(ReceiptDataType.CHARGE);
        initFiscalizationMock(receiptDataIncome);
        processQueue(); // send income fiscalization request

        var receiptDataReturn = createReturnReceiptData(receiptDataIncome);
        initFiscalizationMock(receiptDataReturn);
        processQueue(); // try to send income return fiscalization request

        checkLifePayRequestSent(receiptDataIncome);
        checkLifePayRequestNotSent(receiptDataReturn);

        assertThatThrownBy(() -> receiveCallbackFromLifePay("1", receiptDataReturn))
                .isInstanceOf(RuntimeException.class);

        receiveCallbackFromLifePay("2", receiptDataIncome);
        checkReceiptDataFiscalized(receiptDataIncome);
        checkReceiptDataProcessing(receiptDataReturn);
    }

    private void checkReceiptFiscalization(ReceiptData receiptData) {
        checkReceiptFiscalization(receiptData, "99990789463");
    }

    private void checkReceiptFiscalization(ReceiptData receiptData, String fn) {
        initFiscalizationMock(receiptData);
        processQueue(); // send requests to life pay, receive fiscalization uuid in response
        checkLifePayRequestSent(receiptData);
        receiveCallbackFromLifePay(fn, receiptData);
        checkReceiptDataFiscalized(receiptData);
    }

    private ReceiptData createReceiptData() {
        return createReceiptData(ReceiptDataType.INCOME);
    }

    private ReceiptData createReceiptData(@SuppressWarnings("SameParameterValue") ReceiptDataType type) {
        return receiptHelper.createReceiptData(type, receiptServiceClient);
    }

    private ReceiptData createReturnReceiptData(ReceiptData receiptData) {
        return receiptHelper.createReturnReceiptData(receiptData, receiptServiceClient);
    }

    private void initFiscalizationMock(ReceiptData... receiptDatum) {
        for (ReceiptData receiptData : receiptDatum) {
            doReturn(lifePayUuid(receiptData)).when(lifePayClient).createReceipt(
                    argThat(arg -> Objects.equals(Objects.toString(receiptData.getId()), arg.getExtId())
                            && Objects.equals(KKT_SN, arg.getTargetSerial()))
            );
        }
    }

    private void processQueue() {
        dbQueueTestUtil.executeAllQueueItems(QueueType.RECEIPT_FISCALIZE);
    }

    private void checkLifePayRequestNotSent(ReceiptData... receiptDatum) {
        for (ReceiptData receiptData : receiptDatum) {
            verify(lifePayClient, never()).createReceipt(
                    argThat(arg -> Objects.equals(Objects.toString(receiptData.getId()), arg.getExtId()))
            );
        }
    }

    private void checkLifePayRequestSent(ReceiptData... receiptDatum) {
        for (ReceiptData receiptData : receiptDatum) {
            verify(lifePayClient).createReceipt(
                    argThat(arg -> Objects.equals(Objects.toString(receiptData.getId()), arg.getExtId()))
            );
        }
    }

    private void receiveCallbackFromLifePay(String fn, ReceiptData... receiptDatum) {
        for (ReceiptData receiptData : receiptDatum) {
            lifePayService.createFiscalData(receiptHelper.createLifePayProcessingResultDto(
                    LifePayReceiptType.getByReceiptDataType(receiptData.getType()), lifePayUuid(receiptData), fn
            ));
        }
    }

    private void checkReceiptDataFiscalized(ReceiptData... receiptDatum) {
        for (ReceiptData receiptData : receiptDatum) {
            assertThat(receiptData.getStatus()).isEqualTo(FiscalReceiptStatus.OK);
            assertThat(receiptFiscalDataRepository.findByReceiptData(receiptData)).isPresent();
        }
    }

    private void checkReceiptDataProcessing(ReceiptData... receiptDatum) {
        for (ReceiptData receiptData : receiptDatum) {
            assertThat(receiptData.getStatus()).isEqualTo(FiscalReceiptStatus.PROCESSING);
            assertThat(receiptFiscalDataRepository.findByReceiptData(receiptData)).isNotPresent();
        }
    }

    private String lifePayUuid(ReceiptData receiptData) {
        return "uuid-test-" + receiptData.getId();
    }

}
