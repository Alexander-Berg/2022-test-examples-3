package ru.yandex.market.checkout.providers;

import java.math.BigDecimal;
import java.net.URL;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.pay.BankDetails;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.returns.ReturnDelivery;
import ru.yandex.market.checkout.checkouter.returns.ReturnItem;
import ru.yandex.market.checkout.checkouter.returns.ReturnReasonType;
import ru.yandex.market.checkout.checkouter.returns.ReturnStatus;
import ru.yandex.market.checkout.common.util.UrlBuilder;
import ru.yandex.market.checkout.test.providers.AddressProvider;

/**
 * @author sergeykoles
 * Created on: 20.02.18
 */
public abstract class ReturnProvider {

    public static final BigDecimal DEFAULT_USER_COMPENSATION = new BigDecimal("20.31");
    public static final BigDecimal DEFAULT_SUPPLIER_COMPENSATION = new BigDecimal("51.13");
    public static final BigDecimal DEFAULT_USER_CREDIT_COMPENSATION = BigDecimal.ZERO;
    private static final String DEFAULT_COMMENT = "Some comment";

    private ReturnProvider() {
    }

    @Nonnull
    public static ReturnItem generateReturnItem(Return re, OrderItem item) {
        ReturnItem rie = new ReturnItem();
        rie.setCount(1);
        rie.setQuantity(BigDecimal.ONE.setScale(3));
        rie.setItemId(item.getId());
        rie.setReturnId(re.getId());
        rie.setSupplierCompensation(new BigDecimal("144.13").add(new BigDecimal(item.getId())));
        rie.setDefective(false);
        rie.setReturnReason("захотелось");
        rie.setReasonType(ReturnReasonType.BAD_QUALITY);
        rie.setPicturesUrls(getDefaultUrls());
        rie.setReturnAddressDisplayed(true);
        return rie;
    }

    public static Return generateReturn(Order order) {
        Return ret = new Return();
        ret.setUserCompensationSum(DEFAULT_USER_COMPENSATION);
        ret.setUserCreditCompensationSum(DEFAULT_USER_CREDIT_COMPENSATION);
        ret.setOrderId(order.getId());
        ret.setStatus(ReturnStatus.REFUND_IN_PROGRESS);
        ret.setComment(DEFAULT_COMMENT);
        ret.setBankDetails(getDefaultBankDetails());
        ret.setItems(order.getItems().stream()
                .map(ReturnProvider::makeReturnItemFromOrderItem)
                .collect(Collectors.toList())
        );
        ret.setUserId(order.getBuyer().getUid());
        Instant now = Instant.now();
        ret.setCreatedAt(now);
        ret.setStatusUpdatedAt(now);
        ret.setUpdatedAt(now);
        ret.setLargeSize(false);
        ret.setFastReturn(false);
        ret.setUserId(order.getBuyer().getUid());
        ret.setPayOffline(false);
        return ret;
    }

    public static Return generateFullReturn(Order order) {
        Return ret = generateReturn(order);
        if (!order.getDelivery().isFree()) {
            ret.getItems().add(makeReturnItemFromOrderDelivery(order));
        }
        return ret;
    }

    public static Return generateReturnWithoutCompensation(Order order) {
        Return ret = new Return();
        ret.setOrderId(order.getId());
        ret.setStatus(ReturnStatus.REFUND_IN_PROGRESS);
        ret.setComment(DEFAULT_COMMENT);
        ret.setBankDetails(getDefaultBankDetails());
        ret.setItems(order.getItems().stream()
                .map(ReturnProvider::makeReturnItemFromOrderItem)
                .peek(i -> i.setSupplierCompensation(null))
                .collect(Collectors.toList())
        );
        return ret;
    }

    private static Return generatePartialReturn(Order order, int num) {
        Return ret = new Return();
        ret.setUserCompensationSum(DEFAULT_USER_COMPENSATION);
        ret.setOrderId(order.getId());
        ret.setStatus(ReturnStatus.REFUND_IN_PROGRESS);
        ret.setComment(DEFAULT_COMMENT);
        ret.setBankDetails(getDefaultBankDetails());
        ret.setItems(order.getItems().stream()
                .limit(num)
                .map(ReturnProvider::makeReturnItemFromOrderItem)
                .collect(Collectors.toList())
        );
        return ret;
    }

    public static Return generatePartialReturn(Order order, OrderItem item, int returnCount) {
        Return ret = new Return();
        ret.setUserCompensationSum(DEFAULT_USER_COMPENSATION);
        ret.setOrderId(order.getId());
        ret.setStatus(ReturnStatus.REFUND_IN_PROGRESS);
        ret.setComment(DEFAULT_COMMENT);
        ret.setBankDetails(getDefaultBankDetails());
        ReturnItem returnItem = makeReturnItemFromOrderItem(item);
        returnItem.setCount(returnCount);
        returnItem.setValidIntQuantity(returnCount);
        ret.setItems(Collections.singletonList(returnItem));
        return ret;
    }

    public static Return generateCourierReturn(Order order) {
        Return ret = generateReturn(order);
        ret.setDelivery(getDefaultReturnDelivery(order.getDelivery().getDeliveryServiceId()));
        return ret;
    }

    public static Return generateReturnWithDelivery(Order order, long deliveryServiceId) {
        Return ret = generateReturn(order);
        ret.getItems().add(makeReturnItemFromOrderDelivery(order));
        ret.setDelivery(getDefaultReturnDelivery(deliveryServiceId));
        return ret;
    }

    public static Return generateReturnWithDelivery(Order order, long deliveryServiceId, boolean refundDelivery) {
        Return ret = generateReturn(order);
        if (refundDelivery) {
            ret.getItems().add(makeReturnItemFromOrderDelivery(order));
        }
        ret.setDelivery(getDefaultReturnDelivery(deliveryServiceId));
        return ret;
    }

    public static Return generatePartialReturnWithDelivery(Order order, long deliveryServiceId, int num) {
        Return ret = generatePartialReturn(order, num);
        ret.setDelivery(getDefaultReturnDelivery(deliveryServiceId));
        return ret;
    }

    public static Return generatePartialReturnWithDelivery(Order order, long deliveryServiceId, int num,
                                                           boolean payOffline) {
        Return ret = generatePartialReturn(order, num);
        ret.setDelivery(getDefaultReturnDelivery(deliveryServiceId));
        ret.setPayOffline(payOffline);
        return ret;
    }

    public static ReturnItem makeReturnItemFromOrderItem(OrderItem orderItem) {
        return makeReturnItemFromOrderItem(orderItem, 2);
    }

    public static ReturnItem makeReturnItemFromOrderItem(OrderItem orderItem, int times) {
        ReturnItem returnItem = ReturnItem.initReturnOrderItem(orderItem.getId(), ReturnReasonType.BAD_QUALITY,
                orderItem.getCount(), orderItem.getQuantityIfExistsOrCount());
        returnItem.setSupplierCompensation(DEFAULT_SUPPLIER_COMPENSATION.add(new BigDecimal(orderItem.getId())));
        returnItem.setDefective(false);
        returnItem.setReturnReason(UUID.randomUUID().toString());
        returnItem.setPicturesUrls(getDefaultUrls());
        if (orderItem.getId() != null) {
            returnItem.setItemId(orderItem.getId());
        }
        return returnItem;
    }

    private static ReturnItem makeReturnItemFromOrderDelivery(Order order) {
        ReturnItem returnItem = new ReturnItem();
        returnItem.setOrderDeliveryId(order.getInternalDeliveryId());
        returnItem.setItemId(null);
        returnItem.setCount(1);
        returnItem.setQuantity(BigDecimal.ONE);
        returnItem.setSupplierCompensation(BigDecimal.ZERO);
        return returnItem;
    }

    public static BankDetails getDefaultBankDetails() {
        BankDetails bankDetails = new BankDetails();
        bankDetails.setAccount("10111110101001101222");
        bankDetails.setCorraccount("10101110100000000111");
        bankDetails.setBik("000000000");
        bankDetails.setFirstName("Покупатель");
        bankDetails.setLastName("Недовольный");
        bankDetails.setMiddleName("Совсем");
        bankDetails.setBank("АО 'РазводБанк'");
        bankDetails.setBankCity("г.Москва");
        return bankDetails;
    }

    public static ReturnDelivery getDefaultReturnDelivery() {
        return getDefaultReturnDelivery(123L);
    }

    private static ReturnDelivery getDefaultReturnDelivery(long deliveryServiceId) {
        ReturnDelivery delivery = new ReturnDelivery();
        delivery.setDeliveryServiceId(deliveryServiceId);
        delivery.setType(DeliveryType.DELIVERY);
        delivery.setSenderAddress(AddressProvider.getSenderAddress());
        return delivery;
    }

    private static List<URL> getDefaultUrls() {
        return List.of(
                UrlBuilder.fromString("http://gravicapa1.market.yandex.net:39777").toUrl(),
                UrlBuilder.fromString("http://gravicapa2.market.yandex.net:39666").toUrl()
        );
    }
}
