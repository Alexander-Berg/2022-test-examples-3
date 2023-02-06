package ru.yandex.market.checkout.checkouter.returns;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.balance.model.NotificationMode;
import ru.yandex.market.checkout.checkouter.balance.model.notifications.TrustRefundNotification;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackStatus;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.pay.BankDetails;
import ru.yandex.market.checkout.checkouter.pay.RefundService;
import ru.yandex.market.checkout.checkouter.pay.ReturnService;
import ru.yandex.market.checkout.checkouter.trust.service.TrustPaymentService;
import ru.yandex.market.checkout.checkouter.viewmodel.ReturnableItemViewModel;
import ru.yandex.market.checkout.helpers.OrderHistoryEventsTestHelper;
import ru.yandex.market.checkout.helpers.ReturnHelper;
import ru.yandex.market.checkout.test.providers.AddressProvider;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNot.not;

public class AbstractReturnTestBase extends AbstractWebTestBase {

    public static final Long DELIVERY_SERVICE_ID = 777L;
    static final BankDetails DEFAULT_BANK_DETAILS = new BankDetails("00000000000000000000",
            "corraccount", "000000000", "bank",
            "bankCity", "lastName", "firstName", "middleName", "0123456789abcdef0123456789abcdef",
            "paymentPurpose");

    static final BankDetails NEW_BANK_DETAILS = new BankDetails("00000000000000000001",
            "corraccount1", "000000001", "bank1",
            "bankCity1", "lastName1", "firstName1", "middleName1", "2dc44069f156ae3d0c2b9baf73e15887",
            "paymentPurpose1");

    static final BankDetails FULL_NAME_BANK_DETAILS = new BankDetails(null, null, null, null, null,
            "Иванов", "Иван", "Иванович", "f9e205e8da23cb461bd5779c8a31460f",
            null);

    static final BankDetails NEW_FULL_NAME_BANK_DETAILS = new BankDetails(null, null, null, null, null,
            "Петров", "Петр", "Петрович", "0f9bd8230a9527567138eeacdc144bf6",
            null);

    @Autowired
    protected OrderHistoryEventsTestHelper eventsTestHelper;
    @Autowired
    protected ReturnService returnService;
    @Autowired
    protected RefundService refundService;
    @Autowired
    protected ReturnHelper returnHelper;
    @Autowired
    private TrustPaymentService trustPaymentService;

    protected ReturnItem toReturnItem(ReturnableItemViewModel item) {
        ReturnItem returnItem = new ReturnItem();
        returnItem.setCount(item.getCount());
        returnItem.setValidIntQuantity(item.getCount());
        returnItem.setItemId(item.getItemId());
        returnItem.setReasonType(ReturnReasonType.BAD_QUALITY);
        return returnItem;
    }

    protected ReturnItem toReturnItem(OrderItem item) {
        ReturnItem returnItem = new ReturnItem();
        returnItem.setCount(item.getCount());
        returnItem.setQuantity(item.getQuantity());
        returnItem.setItemId(item.getId());
        returnItem.setReasonType(ReturnReasonType.BAD_QUALITY);
        return returnItem;
    }

    protected ReturnItem toReturnItem(OrderItem item, BigDecimal supplierCompensation) {
        ReturnItem returnItem = new ReturnItem();
        returnItem.setCount(item.getCount());
        returnItem.setQuantity(item.getQuantity());
        returnItem.setItemId(item.getId());
        returnItem.setReasonType(ReturnReasonType.BAD_QUALITY);
        returnItem.setSupplierCompensation(supplierCompensation);
        return returnItem;
    }

    protected ReturnItem toReturnItemDropCount(OrderItem item) {
        ReturnItem returnItem = new ReturnItem();
        returnItem.setCount(1);
        returnItem.setQuantity(BigDecimal.ONE);
        returnItem.setItemId(item.getId());
        returnItem.setReasonType(ReturnReasonType.BAD_QUALITY);
        return returnItem;
    }

    protected Return prepareReturnRequestOfOneItem(Order order, DeliveryType deliveryType) {
        Return request = new Return();
        List<ReturnItem> returnItems = Collections.singletonList(
                order.getItems().stream().findAny()
                        .map(this::toReturnItemDropCount)
                        .orElseThrow(() -> new RuntimeException("Map return item error"))
        );
        request.setItems(returnItems);
        setDefaultDelivery(order, request, deliveryType);
        request.setBankDetails(ReturnHelper.createDummyBankDetails());
        return request;
    }

    protected Return prepareDefaultReturnRequest(Order order, DeliveryType deliveryType) {
        Return request = new Return();
        BigDecimal compensation = BigDecimal.valueOf(100);
        request.setUserCompensationSum(compensation);
        List<ReturnItem> returnItems = order.getItems().stream().map(this::toReturnItem).collect(toList());
        returnItems.get(0).setSupplierCompensation(compensation);
        request.setItems(returnItems);
        setDefaultDelivery(order, request, deliveryType);
        request.setBankDetails(ReturnHelper.createDummyBankDetails());
        return ReturnHelper.copyWithRandomizeItemComments(request);
    }

    protected void setDefaultDelivery(Order order, Return request, DeliveryType deliveryType) {
        setDefaultDelivery(order, request, deliveryType, true);
    }

    protected void setDefaultDelivery(Order order, Return request, DeliveryType deliveryType, boolean withMock) {
        if (withMock) {
            returnHelper.mockActualDelivery(request, order);
        }
        ReturnOptionsResponse returnOptions = getReturnOptions(order);
        ReturnDelivery option = returnOptions.getDeliveryOptions().stream().filter(returnDelivery -> returnDelivery
                        .getType() == deliveryType)
                .findAny()
                .orElseThrow(() -> new RuntimeException("chosen DeliveryType unavailable; please change report " +
                        "actualDelivery configs"));
        request.setDelivery(convertOptionToDelivery(option));
    }

    protected void addCompensation(Return returnOptions) {
        returnOptions.setUserCompensationSum(BigDecimal.valueOf(100));
        returnOptions.getItems().get(0).setSupplierCompensation(BigDecimal.valueOf(100));
    }

    @Nonnull
    protected void addBankDetails(Return request) {
        request.setBankDetails(DEFAULT_BANK_DETAILS);
    }

    protected ReturnDelivery convertOptionToDelivery(ReturnDelivery deliveryOption) {
        ReturnDelivery delivery = new ReturnDelivery();
        delivery.setType(deliveryOption.getType());
        delivery.setDeliveryServiceId(deliveryOption.getDeliveryServiceId());
        if (deliveryOption.getOutletIds() != null) {
            delivery.setOutletId(deliveryOption.getOutletIds().get(0));
        }
        if (deliveryOption.getType() == DeliveryType.DELIVERY) {
            delivery.setDates(deliveryOption.getDates());
            SenderAddress senderAddress = AddressProvider.getSenderAddress();
            delivery.setSenderAddress(senderAddress);
        }
        delivery.setOutletIds(null);
        delivery.setPrice(deliveryOption.getPrice());
        return delivery;
    }

    protected ReturnOptionsResponse getReturnOptions(Order order) {
        return client.returns().getReturnOptions(
                order.getId(),
                ClientRole.SYSTEM,
                DELIVERY_SERVICE_ID,
                order.getItems().stream().map(this::toReturnItem).collect(toList())
        );
    }

    protected void compensationProcessed(Return returnResp) {
        compensationProcessed(returnResp, true);
    }

    protected void compensationProcessed(Return returnResp, boolean withContract) {
        assertThat(returnResp.getCompensationClientId(), not(nullValue()));
        assertThat(returnResp.getCompensationPersonId(), not(nullValue()));
        if (withContract) {
            assertThat(returnResp.getCompensationContractId(), not(nullValue()));
        } else {
            assertThat(returnResp.getCompensationContractId(), nullValue());
        }
    }

    @Nonnull
    protected Return getReturnById(Long retId) {
        return returnService.findReturnById(
                retId,
                false,
                ClientInfo.SYSTEM
        );
    }

    protected void notifyRefundReceipts(Return ret) {
        refundService.getReturnRefunds(ret).forEach(r -> {
            refundService.notifyRefund(new TrustRefundNotification(
                    NotificationMode.receipt, r.getTrustRefundId(), "success", false,
                    trustPaymentService.getReceiptUrl(r.getTrustRefundId(), r.getTrustRefundId())));

        });
    }

    void assertTrackStatusEquals(final Order order, final Return ret) {
        assertTrackStatusEquals(order, ret, TrackStatus.STARTED);
    }

    void assertTrackStatusEquals(final Order order, final Return ret, final TrackStatus trackStatus) {
        Return returnResp = client.returns().getReturn(order.getId(),
                ret.getId(), false, ClientRole.SYSTEM, 123L);
        assertThat(returnResp.getDelivery().getTrack().getStatus(), equalTo(trackStatus));
    }
}
