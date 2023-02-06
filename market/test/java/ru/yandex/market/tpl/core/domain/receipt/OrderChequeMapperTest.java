package ru.yandex.market.tpl.core.domain.receipt;

import java.math.BigDecimal;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.core.CoreTestV2;
import ru.yandex.market.tpl.core.domain.order.OrderChequeMapper;

@RequiredArgsConstructor

@CoreTestV2
public class OrderChequeMapperTest {
    private final OrderChequeMapper orderChequeMapper;

    @Test
    void shouldMapFiscalSignOfTheDocument() {
        ReceiptData receiptData = new ReceiptData();
        receiptData.setReceiptId("1234");
        receiptData.setCardAmount(BigDecimal.TEN);
        ReceiptFiscalData data = new ReceiptFiscalData();
        data.setReceiptData(receiptData);
        data.setFp("0730214557");
        data.setShiftNum("1234");
        data.setDocumentNum("234423");
        var mapped = orderChequeMapper.map(data);
        var res = mapped.getFiscalSignOfTheDocument();
        Assertions.assertThat(res.toString()).isEqualTo("730214557");

    }
}
