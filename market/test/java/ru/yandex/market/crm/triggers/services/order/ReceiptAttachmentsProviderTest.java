package ru.yandex.market.crm.triggers.services.order;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.tools.ant.filters.StringInputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.checkout.checkouter.receipt.Receipts;
import ru.yandex.market.crm.core.services.yandexsender.LetterAttachment;
import ru.yandex.market.crm.triggers.services.checkouter.CheckouterService;
import ru.yandex.market.crm.triggers.services.receipt.ReceiptService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ReceiptAttachmentsProviderTest {
    private static final Long ORDER_ID = 5856665L;
    private static final long RECEIPT_ID = 12345L;
    private static final String MULTIORDER_ID = "666-666";

    @Mock
    private CheckouterService checkouterService;

    private ReceiptAttachmentsProvider receiptAttachmentsProvider;

    @Before
    public void setUp() {
        ReceiptService receiptService = new ReceiptService(checkouterService);
        receiptAttachmentsProvider = new ReceiptAttachmentsProvider(receiptService);
    }

    @Test
    public void testIncomeOrderReceipt() throws IOException {
        var receipt = incomeReceipt();

        prepareReceiptMocks(receipt);
        List<LetterAttachment> mailAttachments = receiptAttachmentsProvider.getReceiptAttachments(
                Set.of(ORDER_ID), Set.of(RECEIPT_ID), MULTIORDER_ID
        );
        assertReceiptAttachment(mailAttachments, receipt);

        prepareReceiptMocks(receipt);
        mailAttachments = receiptAttachmentsProvider.getReceiptAttachments(ORDER_ID, Set.of(RECEIPT_ID));
        assertReceiptAttachment(mailAttachments, receipt);
    }

    private void assertReceiptAttachment(List<LetterAttachment> attachments, Receipt receipt) {
        assertEquals(1, attachments.size());

        String attachmentName = String.format("market_receipt_income_%d.pdf", receipt.getId());
        Optional<LetterAttachment> receiptAttachment =
                attachments.stream().filter(x -> attachmentName.equals(x.getFileName())).findFirst();

        assertTrue(receiptAttachment.isPresent());
        assertEquals(Base64.getEncoder().encodeToString("receipt".getBytes(StandardCharsets.UTF_8)),
                Base64.getEncoder().encodeToString(receiptAttachment.get().getData()));
    }

    private Receipt incomeReceipt() {
        Receipt receipt = new Receipt();
        receipt.setType(ReceiptType.INCOME);
        receipt.setId(RECEIPT_ID);
        return receipt;
    }

    private void prepareReceiptMocks(Receipt receipt) throws IOException {
        Receipts receipts = new Receipts(Collections.singletonList(receipt));
        when(checkouterService.getOrderReceipts(eq(ORDER_ID), any(ClientRole.class), any(), any()))
                .thenReturn(receipts);
        when(checkouterService.getOrderReceiptPdf(eq(ORDER_ID), eq(receipt.getId()), any(ClientRole.class), any(), any()))
                .thenReturn(new StringInputStream("receipt"));
    }
}
