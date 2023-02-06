package ru.yandex.market.checkout.checkouter.pay;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsCollectionWithSize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.order.ItemService;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptItem;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptService;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptStatus;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.helpers.ReceiptRepairHelper;
import ru.yandex.market.checkout.util.Has;
import ru.yandex.market.checkout.util.balance.TrustMockConfigurer;

import static java.math.BigDecimal.valueOf;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.common.util.ObjectUtils.avoidNull;
import static ru.yandex.market.checkout.checkouter.pay.RefundTestHelper.refundableItemsFromOrder;
import static ru.yandex.market.checkout.checkouter.receipt.ReceiptStatus.GENERATED;
import static ru.yandex.market.checkout.util.GenericMockHelper.withUserRole;
import static ru.yandex.market.checkout.util.matching.Matchers.matchesPattern;

/**
 * @author mkasumov
 */
public class ReceiptTestHelper extends AbstractPaymentTestHelper {

    @Autowired
    private ReceiptService receiptService;
    @Autowired
    private ReceiptRepairHelper receiptRepairHelper;

    private int expectedReceiptsCount;

    public ReceiptTestHelper(AbstractWebTestBase test, Has<Order> order, Has<ShopMetaData> shopMetaData) {
        super(test, order, shopMetaData);
        this.expectedReceiptsCount = 0;
    }

    void resetExpectedReceiptsCount() {
        expectedReceiptsCount = 0;
    }

    long checkNewReceiptForPayment(ReceiptType expectedReceiptType,
                                   ReceiptStatus expectedReceiptStatus) throws Exception {
        return checkNewReceipt(expectedReceiptType, null, null, expectedReceiptStatus, false);
    }

    void checkNewReceiptForRefund(ReceiptType expectedReceiptType, Long refundId, RefundableItems expectedReceiptItems,
                                  ReceiptStatus expectedReceiptStatus, boolean isPartialRefund) throws Exception {
        checkNewReceipt(expectedReceiptType, refundId, expectedReceiptItems, expectedReceiptStatus, isPartialRefund);
    }

    public void checkCompensationsReceipts(List<Payment> compensations) {
        for (Payment compensation : compensations) {
            List<Receipt> receipts = receiptService.findByPayment(compensation);
            assertThat(receipts, not(empty()));
            assertThat(receipts, IsCollectionWithSize.hasSize(1));
            Receipt receipt = receipts.get(0);
            assertThat(receipt.getStatus(), CoreMatchers.equalTo(GENERATED));
            List<BigDecimal> itemsPrices = receipt.getItems().stream()
                    .map(ReceiptItem::getPrice)
                    .collect(Collectors.toList());
            List<BigDecimal> itemsAmounts = receipt.getItems().stream()
                    .map(ReceiptItem::getAmount)
                    .collect(Collectors.toList());
            BigDecimal totalAmount = itemsAmounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);

            List<BigDecimal> itemsCount = receipt.getItems().stream()
                    .map(ReceiptItem::getPrice)
                    .collect(Collectors.toList());
            assertThat(totalAmount, CoreMatchers.equalTo(compensation.getTotalAmount()));
            assertThat(itemsPrices, everyItem(greaterThan(BigDecimal.ZERO)));
            assertThat(itemsCount, everyItem(greaterThan(BigDecimal.ZERO)));
        }
    }

    private long checkNewReceipt(ReceiptType expectedReceiptType, Long refundId, RefundableItems expectedReceiptItems,
                                 ReceiptStatus expectedReceiptStatus, boolean isPartial) throws Exception {
        expectedReceiptsCount++;
        return checkLastReceipt(null, refundId, expectedReceiptItems,
                expectedReceiptType, expectedReceiptStatus, isPartial);
    }

    void checkLastReceiptForPayment(Long expectedReceiptId,
                                    ReceiptType expectedReceiptType, ReceiptStatus expectedReceiptStatus)
            throws Exception {
        checkLastReceipt(expectedReceiptId, null, null, expectedReceiptType,
                expectedReceiptStatus, false);
    }

    private long checkLastReceipt(Long expectedReceiptId,
                                  Long refundId,
                                  RefundableItems expectedReceiptItems,
                                  ReceiptType expectedReceiptType,
                                  ReceiptStatus expectedReceiptStatus,
                                  boolean isPartial) throws Exception {
        List<Receipt> receipts = receiptService.findByOrder(order().getId(), expectedReceiptType);
        // Отсортируем здесь чеки так же, как в ручке
        receipts.sort(Comparator.comparing(Receipt::getCreatedAt));
        assertThat(receipts.size(), greaterThan(0));
        // И возьмём последний
        final Receipt receipt = receipts.get(receipts.size() - 1);
        assertThat(receipt.getType(), equalTo(expectedReceiptType));
        assertThat(receipt.getStatus(), equalTo(expectedReceiptStatus));
        if (refundId == null) {
            assertThat(receipt.getPaymentId(), equalTo(order().getPaymentId()));
        } else {
            assertThat(receipt.getRefundId(), equalTo(refundId));
        }
        if (expectedReceiptId != null) {
            assertThat(receipt.getId(), equalTo(expectedReceiptId));
        }

        final RefundableItems expectedRefundableItems = refundableItemsFromOrder(order());
        final RefundableItems refundableItems = avoidNull(expectedReceiptItems, expectedRefundableItems);
        final int expectedSize;
        if (expectedReceiptItems == null && !receipt.isPrintable() && receipt.getType() == ReceiptType.INCOME_RETURN) {
            expectedSize = 0;
        } else {
            expectedSize = refundableItems.getItems().size() + refundableItems.getItemServices().size() +
                    (refundableItems.getDelivery() != null && refundableItems.getDelivery().isRefundable() ? 1 : 0);
        }
        assertThat(receipt.getItems(), hasSize(expectedSize));
        checkReceiptItems(receipt, refundableItems, isPartial);
        if (expectedReceiptStatus == ReceiptStatus.PRINTED) {
            checkReceiptPDF(receipt.getId());
        }

        return receipt.getId();
    }

    private void checkReceiptItems(final Receipt receipt, RefundableItems expectedReceiptItems, boolean isPartial) {
        receipt.getItems().forEach(ri -> {
            assertThat(ri.getReceiptId(), is(receipt.getId()));
            assertThat(ri.getOrderId(), is(order().getId()));
            if (ri.isOrderItem()) {
                RefundableItem mi = expectedReceiptItems.getItems().stream().filter(i -> Objects.equals(i.getId(),
                        ri.getItemId())).findAny().orElse(null);
                assertThat(mi, notNullValue());
                assertThat(ri.getItemServiceId(), is(nullValue()));
                assertThat(ri.getDeliveryId(), is(nullValue()));
                assertThat(ri.getPrice(), comparesEqualTo(mi.getBuyerPrice()));
                assertThat(ri.getCount(), is(mi.getRefundableCount()));
                assertThat(ri.getQuantityIfExistsOrCount(),
                        comparesEqualTo(mi.getRefundableQuantityIfExistsOrRefundableCount()));
                assertThat(ri.getPrice(), comparesEqualTo(mi.getQuantPriceIfExistsOrBuyerPrice()));
                if (isPartial) {
                    // TODO Научиться сюда передавать сумму частичного рефанда.
                    assertTrue(ri.getAmount().compareTo(BigDecimal.ZERO) > 0);
                } else {
                    assertThat(ri.getAmount(),
                            comparesEqualTo(mi.getBuyerPrice().multiply(valueOf(mi.getRefundableCount()))));
                    assertThat(ri.getAmount(),
                            comparesEqualTo(mi.getQuantPriceIfExistsOrBuyerPrice()
                                    .multiply(mi.getRefundableQuantityIfExistsOrRefundableCount())));
                }
            } else if (ri.isItemService()) {
                RefundableItemService mi = expectedReceiptItems.getItemServices()
                        .stream()
                        .filter(i -> Objects.equals(i.getId(), ri.getItemServiceId()))
                        .findAny()
                        .orElse(null);
                assertThat(mi, notNullValue());
                assertThat(ri.getItemId(), is(nullValue()));
                assertThat(ri.getDeliveryId(), is(nullValue()));
                assertThat(ri.getPrice(), comparesEqualTo(mi.getPrice()));
                assertThat(ri.getCount(), is(mi.getRefundableCount()));
                if (isPartial) {
                    // TODO Научиться сюда передавать сумму частичного рефанда.
                    assertTrue(ri.getAmount().compareTo(BigDecimal.ZERO) > 0);
                } else {
                    assertThat(ri.getAmount(),
                            comparesEqualTo(mi.getPrice().multiply(valueOf(mi.getRefundableCount()))));
                }
            } else if (ri.isDelivery()) {
                RefundableDelivery dlv = expectedReceiptItems.getDelivery();
                assertThat(ri.getItemId(), is(nullValue()));
                assertThat(ri.getItemServiceId(), is(nullValue()));
                assertThat(ri.getPrice(), comparesEqualTo(dlv.getBuyerPrice()));
                assertThat(ri.getCount(), is(dlv.isRefundable() ? 1 : 0));
                assertThat(ri.getAmount(), comparesEqualTo(dlv.getBuyerPrice()));
            } else { //TBD support refund item services
                throw new AssertionError("Both itemId and deliveryId are null in ReceiptItem: " + ri);
            }
        });
    }

    void checkReceiptForOrders(List<Order> orders, ReceiptStatus expectedStatus) {
        // для каждого заказа получаем чек
        Map<Long, Receipt> orderIdToReceipt = orders.stream()
                .map(o -> Pair.of(o.getId(), receiptService.findByOrder(o.getId())))
                .collect(
                        Collectors.toMap(
                                Pair::getKey,
                                p -> {
                                    List<Receipt> receipts = p.getValue();
                                    assertThat(receipts, hasSize(1));
                                    return receipts.iterator().next();
                                }
                        )
                );

        Collection<Receipt> receipts = orderIdToReceipt.values();

        // смотрим, что для каждого заказа чек найден
        assertThat(
                receipts.stream().filter(Objects::nonNull).count(),
                equalTo((long) orders.size())
        );

        // убеждаемся, что на самом деле это один и тот же чек
        assertThat(
                receipts.stream().map(Receipt::getId).distinct().count(),
                equalTo(1L)
        );

        // берем какой-нибудь экземпляр этого чека и проверяем на адекватность
        Receipt receipt = receipts.iterator().next();

        // статуса
        assertThat(receipt.getStatus(), equalTo(expectedStatus));

        // смотрим какие есть товары
        List<Long> receiptOrderItemIds = receipt.getItems().stream()
                .map(ReceiptItem::getItemId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // и услуги
        List<Long> receiptItemServiceIds = receipt.getItems().stream()
                .map(ReceiptItem::getItemServiceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // и доставки
        List<Long> receiptDeliveryIds = receipt.getItems().stream()
                .map(ReceiptItem::getDeliveryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // и сверяем с ожидаемыми товарами во всех заказах
        assertThat(receiptOrderItemIds,
                Matchers.containsInAnyOrder(
                        orders.stream()
                                .flatMap(o -> o.getItems().stream())
                                .map(OrderItem::getId)
                                .toArray(Long[]::new)
                ));

        // и сверяем с ожидаемыми услугами во всех заказах
        assertThat(receiptItemServiceIds,
                Matchers.containsInAnyOrder(
                        orders.stream()
                                .flatMap(o -> o.getItems().stream())
                                .map(OrderItem::getServices)
                                .flatMap(Collection::stream)
                                .filter(itemService -> itemService.getPaymentType() == PaymentType.PREPAID)
                                .map(ItemService::getId)
                                .toArray(Long[]::new)
                ));

        // поскольку id доставкок наружу не торчат, проверим количество (цена != 0)
        assertThat(receiptDeliveryIds,
                Matchers.hasSize(
                        (int) orders.stream()
                                .map(Order::getDelivery)
                                .filter(Objects::nonNull)
                                .map(Delivery::getBuyerPrice)
                                .filter(bp -> bp != null && BigDecimal.ZERO.compareTo(bp) != 0)
                                .count()
                ));

    }

    private void checkReceiptPDF(long receiptId) throws Exception {
        byte[] pdf = mockMvc.perform(withUserRole(get("/orders/{orderId}/receipts/{receiptId}/pdf?",
                order().getId(), receiptId), order()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        matchesPattern("attachment; filename=receipt_.*[.]pdf")))
                .andReturn().getResponse().getContentAsByteArray();
        assertPDFMd5(pdf);
    }

    private void assertPDFMd5(byte[] bytes) throws NoSuchAlgorithmException, IOException {
        MessageDigest md5Digest = MessageDigest.getInstance("MD5");
        byte[] bytesMd5 = md5Digest.digest(bytes);
        byte[] fileMd5 = md5Digest.digest(toByteArray(TrustMockConfigurer.getMockReceiptPDF()));
        assertThat(bytesMd5, is(equalTo(fileMd5)));
    }

    public void repairReceipts() {
        receiptRepairHelper.repairReceipts();
    }

    public boolean paymentHasReceipt(Payment payment) {
        return receiptService.paymentHasReceipt(payment);
    }

    public void checkNoNewReceipts() {
        List<Receipt> receipts = receiptService.findByOrder(order().getId());
        assertThat(receipts, hasSize(expectedReceiptsCount));
    }
}
