package ru.yandex.market.wms.receiving.service;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.wms.common.spring.dao.entity.Receipt;
import ru.yandex.market.wms.common.spring.dao.implementation.StorerDao;
import ru.yandex.market.wms.receiving.BaseReceivingTest;
import ru.yandex.market.wms.receiving.dao.TranslationDao;
import ru.yandex.market.wms.receiving.dao.entity.ReceiptAnomaly;
import ru.yandex.market.wms.receiving.repository.ReceiptSummaryRepository;
import ru.yandex.market.wms.receiving.repository.entity.ReceiptSummaryEntity;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(MockitoExtension.class)
public class AnomalyServiceTest extends BaseReceivingTest {

    @InjectMocks
    private AnomalyService anomalyService;
    @Mock
    private ReceiptSummaryRepository summaryRepository;
    @Mock
    private StorerDao storerDao;
    @Mock
    private TranslationDao translationDao;

    @Test
    public void pdfReport_skipExcessUnits() {
        //given
        String receiptKey = "rcpt234";
        Receipt receipt = receipt(receiptKey);
        List<ReceiptSummaryEntity> receiptSummaries = newArrayList(
                receiptSummary(2, 2, 0),
                receiptSummary(2, 1, 1),
                receiptSummary(1, 2, 2),
                receiptSummary(1, 0, 3),
                receiptSummary(0, 1, 4)
        );

        //when
        List<ReceiptAnomaly> discrepancies = anomalyService.getDiscrepancies(receipt, receiptSummaries);

        //then
        assertThat("Size of discrepancies", discrepancies.size(), equalTo(2));
        ReceiptAnomaly receiptAnomaly1 = discrepancies.get(0);
        ReceiptAnomaly receiptAnomaly2 = discrepancies.get(1);
        assertThat("sku", receiptAnomaly1.getSku(), equalTo("sku1"));
        assertThat("anomaly description", receiptAnomaly1.getAnomalyDescription(),
                equalTo("Ожидалось: 2. Принято: 1. Расхождение на сумму: 1"));
        assertThat("receipt type", receiptAnomaly1.getReceiptType(),
                equalTo("3P: UNKNOWN"));
        assertThat("extern receipt key", receiptAnomaly1.getExternalReceiptKey(),
                equalTo("extrcpt234"));
        assertThat("sku", receiptAnomaly2.getSku(), equalTo("sku3"));
        assertThat("anomaly description", receiptAnomaly2.getAnomalyDescription(),
                equalTo("Ожидалось: 1. Принято: 0. Расхождение на сумму: 3"));
        assertThat("receipt type", receiptAnomaly2.getReceiptType(),
                equalTo("3P: UNKNOWN"));
        assertThat("extern receipt key", receiptAnomaly2.getExternalReceiptKey(),
                equalTo("extrcpt234"));
    }

    private ReceiptSummaryEntity receiptSummary(int expected, int received, int i) {
        return ReceiptSummaryEntity.builder()
                .quantityExpected(BigDecimal.valueOf(expected))
                .quantityReceived(BigDecimal.valueOf(received))
                .quantityExcessed(BigDecimal.valueOf(received - expected))
                .discrepancy(true)
                .unitPrice(BigDecimal.valueOf(i))
                .manufacturerSku("mansku" + i)
                .sku("sku" + i)
                .susr6("susr")
                .build();
    }

}
