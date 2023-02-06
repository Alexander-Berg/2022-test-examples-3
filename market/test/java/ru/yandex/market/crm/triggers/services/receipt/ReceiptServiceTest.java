package ru.yandex.market.crm.triggers.services.receipt;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptStatus;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.checkout.checkouter.receipt.Receipts;
import ru.yandex.market.crm.triggers.services.checkouter.CheckouterService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * @author vtarasoff
 * @since 14.04.2022
 */
@ExtendWith(MockitoExtension.class)
public class ReceiptServiceTest {
    private static final long ORDER_ID = 123;

    private static final long INCOME_RECEIPT_ID = 11;

    private static final long INCOME_RECEIPT_ID_2 = 12;
    private static final long INCOME_RETURN_RECEIPT_ID = 2;
    private static final long OFFSET_ADVANCE_ON_DELIVERED_RECEIPT_ID = 3;
    @Mock
    private CheckouterService checkouterService;

    private ReceiptService receiptService;

    @BeforeEach
    public void setUp() {
        receiptService = new ReceiptService(checkouterService);

        when(
                checkouterService.getOrderReceipts(ORDER_ID, ClientRole.SYSTEM, null, null)
        ).thenReturn(
                new Receipts(List.of(
                        receipt(INCOME_RECEIPT_ID, ReceiptType.INCOME, ReceiptStatus.PRINTED),
                        receipt(INCOME_RECEIPT_ID_2, ReceiptType.INCOME, ReceiptStatus.GENERATED),
                        receipt(INCOME_RETURN_RECEIPT_ID, ReceiptType.INCOME_RETURN, ReceiptStatus.PRINTED),
                        receipt(
                                OFFSET_ADVANCE_ON_DELIVERED_RECEIPT_ID,
                                ReceiptType.OFFSET_ADVANCE_ON_DELIVERED,
                                ReceiptStatus.PRINTED
                        )
                ))
        );
    }

    private Receipt receipt(long id, ReceiptType type, ReceiptStatus status) {
        var receipt = new Receipt();
        receipt.setId(id);
        receipt.setType(type);
        receipt.setStatus(status);
        return receipt;
    }

    @Test
    public void shouldReturnReceiptIdsByTypesAndStatuses() {
        Set<Receipt> receipts = receiptService.getReceipts(
                ORDER_ID,
                Set.of(ReceiptType.INCOME, ReceiptType.INCOME_RETURN),
                Set.of(ReceiptStatus.PRINTED)
        );
        assertEquals(
                Set.of(
                        receipt(INCOME_RECEIPT_ID, ReceiptType.INCOME, ReceiptStatus.PRINTED),
                        receipt(INCOME_RETURN_RECEIPT_ID, ReceiptType.INCOME_RETURN, ReceiptStatus.PRINTED)
                ),
                receipts
        );
    }

    @Test
    public void shouldReturnAllReceipts() {
        Set<Receipt> receipts = receiptService.getReceipts(ORDER_ID, Set.of(), Set.of());
        assertEquals(
                Set.of(
                        receipt(INCOME_RECEIPT_ID, ReceiptType.INCOME, ReceiptStatus.PRINTED),
                        receipt(INCOME_RECEIPT_ID_2, ReceiptType.INCOME, ReceiptStatus.GENERATED),
                        receipt(INCOME_RETURN_RECEIPT_ID, ReceiptType.INCOME_RETURN, ReceiptStatus.PRINTED),
                        receipt(
                                OFFSET_ADVANCE_ON_DELIVERED_RECEIPT_ID,
                                ReceiptType.OFFSET_ADVANCE_ON_DELIVERED,
                                ReceiptStatus.PRINTED
                        )
                ),
                receipts
        );
    }

    @Test
    public void shouldReturnReceiptById() {
        Receipt receipt = receiptService.getReceipt(ORDER_ID, INCOME_RECEIPT_ID);
        assertEquals(receipt(INCOME_RECEIPT_ID, ReceiptType.INCOME, ReceiptStatus.PRINTED), receipt);
    }

    @Test
    public void shouldThrowsExceptionIfReceiptNotFoundById() {
        assertThrows(
                RuntimeException.class,
                () -> receiptService.getReceipt(ORDER_ID, 1000L)
        );
    }
}
