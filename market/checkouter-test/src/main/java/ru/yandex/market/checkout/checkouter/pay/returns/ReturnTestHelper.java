package ru.yandex.market.checkout.checkouter.pay.returns;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.cashier.model.PassParams;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.balance.service.TrustServiceFee;
import ru.yandex.market.checkout.checkouter.balance.trust.model.BasketMarkup;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.color.ColorConfig;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.SupplierType;
import ru.yandex.market.checkout.checkouter.pay.AbstractPaymentTestHelper;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;
import ru.yandex.market.checkout.checkouter.pay.PaymentTestHelper;
import ru.yandex.market.checkout.checkouter.pay.ReceiptTestHelper;
import ru.yandex.market.checkout.checkouter.pay.Refund;
import ru.yandex.market.checkout.checkouter.pay.RefundService;
import ru.yandex.market.checkout.checkouter.pay.TrustBasketKey;
import ru.yandex.market.checkout.checkouter.pay.compensation.ReturnItemCompensationInfo;
import ru.yandex.market.checkout.checkouter.pay.compensation.ReturnItemsUtils;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.returns.ReturnItem;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.checkouter.shop.ShopService;
import ru.yandex.market.checkout.helpers.RefundHelper;
import ru.yandex.market.checkout.util.Has;
import ru.yandex.market.checkout.util.balance.OneElementBackIterator;
import ru.yandex.market.checkout.util.balance.ResponseVariable;
import ru.yandex.market.checkout.util.balance.checkers.CreateBalanceOrderParams;
import ru.yandex.market.checkout.util.balance.checkers.CreateBasketParams;
import ru.yandex.market.checkout.util.balance.checkers.CreateRefundParams;

import static java.math.BigDecimal.ZERO;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.BigDecimalCloseTo.closeTo;
import static ru.yandex.common.util.ObjectUtils.avoidNull;
import static ru.yandex.market.checkout.checkouter.pay.PaymentTestHelper.DEFAULT_SUPPLIER_INN;
import static ru.yandex.market.checkout.checkouter.pay.PaymentTestHelper.MARKET_PARTNER_ID;
import static ru.yandex.market.checkout.util.GenericMockHelper.servedEvents;
import static ru.yandex.market.checkout.util.balance.BalanceMockHelper.BalanceXMLRPCMethod.FindClient;
import static ru.yandex.market.checkout.util.balance.BalanceMockHelper.BalanceXMLRPCMethod.GetClientContracts;
import static ru.yandex.market.checkout.util.balance.BalanceMockHelper.checkBalanceCall;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CREATE_ORDERS_STUB;
import static ru.yandex.market.checkout.util.balance.checkers.CreateBasketParams.createBasket;
import static ru.yandex.market.checkout.util.balance.checkers.CreateProductParams.product;
import static ru.yandex.market.checkout.util.balance.checkers.CreateRefundParams.refund;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkBatchServiceOrderCreationCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkCreateBasketCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkCreateRefundCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkDoRefundCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkLoadPartnerCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkOptionalCreateServiceProductCall;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.checkPayBasketCall;

public class ReturnTestHelper extends AbstractPaymentTestHelper {

    private final PaymentTestHelper paymentTestHelper;
    private ReceiptTestHelper receiptTestHelper;
    private final ColorConfig colorConfig;
    private static String passportId = ResponseVariable.PASSPORT_ID.defaultValue().toString();

    @Autowired
    private ShopService shopService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private RefundService refundService;
    @Autowired
    private RefundHelper refundHelper;

    public ReturnTestHelper(AbstractWebTestBase test, ReceiptTestHelper receiptTestHelper,
                            PaymentTestHelper paymentTestHelper,
                            Has<Order> order, Has<ShopMetaData> shopMetaData,
                            ColorConfig colorConfig) {
        super(test, order, shopMetaData);
        this.paymentTestHelper = paymentTestHelper;
        this.receiptTestHelper = receiptTestHelper;
        this.colorConfig = colorConfig;
    }


    public void checkReturnBallanceCalls(Return ret) throws Exception {
        OneElementBackIterator<ServeEvent> callIter = trustMockConfigurer.eventsIterator();

        Collection<Refund> returnRefunds = refundService.getReturnRefunds(ret);

        for (Refund refund : returnRefunds) {
            Payment payment = paymentService.getPayment(refund.getPaymentId(), ClientInfo.SYSTEM);
            //проверяем рефанды исходя из того, какой платеж они рефандили
            CreateRefundParams refundParams = refund(payment.getBasketKey())
                    .withUserIp("127.0.0.1")
                    .withReason(ret.getComment());

            switch (payment.getType()) {
                case ORDER_PREPAY:
                    if (isNewPrepayType()) {
                        //добавляем строки с учетом того что ордер баланса на каждый айтем и на доставку
                        ret.getItems().stream().filter(i -> !i.isDeliveryService())
                                .forEach(item -> refundParams.withRefundLine(
                                        order().getItem(item.getItemId()).getBalanceOrderId(),
                                        item.getQuantityIfExistsOrCount(),
                                        order().getItem(item.getItemId()).getQuantPriceIfExistsOrBuyerPrice()
                                                .multiply(item.getQuantityIfExistsOrCount())
                                ));
                        ret.getItems().stream().filter(ReturnItem::isDeliveryService)
                                .forEach(item -> refundParams.withRefundLine(
                                        order().getDelivery().getBalanceOrderId(),
                                        BigDecimal.ONE,
                                        order().getDelivery().getBuyerPrice()
                                ));
                        if (payment.getPartitions() != null) {
                            BasketMarkup expectedMarkup = new BasketMarkup();
                            ret.getItems().stream().filter(i -> !i.isDeliveryService())
                                    .forEach(item -> expectedMarkup.addBasketLineMarkup(order()
                                            .getItem(item.getItemId()).getBalanceOrderId(), null));
                            ret.getItems().stream().filter(ReturnItem::isDeliveryService)
                                    .forEach(item -> expectedMarkup.addBasketLineMarkup(order()
                                            .getDelivery().getBalanceOrderId(), null));
                            refundParams.withMarkup(expectedMarkup);
                        }
                    } else {
                        // был один ордер на все. смотрим исходя из ретерна, сколько денюжек вернули
                        BigDecimal sum = ret.getItems().stream().filter(i -> !i.isDeliveryService()).map(item ->
                                order().getItem(item.getItemId()).getQuantPriceIfExistsOrBuyerPrice()
                                        .multiply(item.getQuantityIfExistsOrCount()))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        sum = sum.add(order().getDelivery().getBuyerPrice().multiply(
                                new BigDecimal(ret.getItems().stream().filter(ReturnItem::isDeliveryService).count())));
                        refundParams.withRefundLine(order().getBalanceOrderId(), ZERO, sum);
                    }

                    String prepayRefundId = checkCreateRefundCall(callIter, payment.getUid(), refundParams);
                    checkDoRefundCall(callIter, prepayRefundId, payment.getUid());
                    break;
                case SUBSIDY:
                    refundParams.withRefundLine(order().getSubsidyBalanceOrderId(), BigDecimal.ONE,
                            order().getBuyerSubsidyTotal());

                    String subsidyRefundId = checkCreateRefundCall(callIter, payment.getUid(), refundParams);
                    checkDoRefundCall(callIter, subsidyRefundId, payment.getUid());
                    break;
                case ORDER_POSTPAY:
                case EXTERNAL_CERTIFICATE:
                    checkCashRefundBalanceCalls(callIter, refund);
                    break;
                default:
                    throw new IllegalArgumentException("Implement check for " + payment.getType());
            }
        }

        //если были доп, компенсации, то это отдельные платежи. проверяем их.
        if (ret.hasCompensation()) {
            checkCompensations(ret, callIter);
        }
    }

    void checkCompensations(Return ret) throws Exception {
        checkCompensations(ret, trustMockConfigurer.eventsIterator());
    }

    private void checkCompensations(Return ret,
                                    OneElementBackIterator<ServeEvent> callIter) throws Exception {
        List<Payment> returnPayments = new ArrayList<>(paymentService.getReturnPayments(ret));
        assertThat(returnPayments, is(not(empty())));

        List<ReturnItemCompensationInfo> returnItems = ReturnItemsUtils.calculateServiceFeeAmounts(ret, order());

        if (ret.hasUserCompensation()) {
            Payment userPayment = returnPayments.stream()
                    .filter(p -> p.getType() == PaymentGoal.USER_COMPENSATION)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("empty user compensation"));
            assertThat(userPayment.getBasketId(), notNullValue());
            assertThat(userPayment.getTotalAmount(), equalTo(ret.totalUserCompensation()));
            assertThat(userPayment.getReturnId(), equalTo(ret.getId()));
        }
        Payment shopPayment = returnPayments.stream()
                .filter(p -> p.getType() == PaymentGoal.MARKET_COMPENSATION)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("empty shop compensation"));
        assertThat(shopPayment.getBasketId(), notNullValue());
        assertThat(shopPayment.getTotalAmount(), equalTo(calcTotalSupplierSum(ret)));
        assertThat(shopPayment.getReturnId(), equalTo(ret.getId()));

        //хотя платежа два, в балансе все равно создается одна корзина и она один раз оплачивается. так задумано
        checkCompensationsBalanceCalls(ret, shopPayment, returnItems, callIter);
        receiptTestHelper.checkCompensationsReceipts(returnPayments);
    }


    private void checkCompensationsBalanceCalls(Return ret,
                                                Payment compensationPayment,
                                                List<ReturnItemCompensationInfo> returnItems,
                                                OneElementBackIterator<ServeEvent> callIter) throws Exception {
        List<BasketLine> lines = new ArrayList<>();
        for (ReturnItemCompensationInfo returnItem : returnItems) {
            OrderItem orderItem = order().getItem(returnItem.getReturnItem().getItemId());
            ShopMetaData meta = shopService.getMeta(orderItem.getSupplierId());
            for (TrustServiceFee serviceFee : returnItem.getServiceFeeValues().keySet()) {
                String serviceProduct = meta.getCampaignId() + "_" + meta.getClientId() + "_"
                        + colorConfig.getFor(order()).getServiceFeeId(serviceFee);

                checkOptionalCreateServiceProductCall(callIter,
                        product(meta.getClientId(), serviceProduct, serviceProduct,
                                colorConfig.getFor(order()).getServiceFeeId(serviceFee)));

                String serviceOrderId = order().getId() + "-item-" + returnItem.getReturnItem().getItemId()
                        + "-ret-" + ret.getId() + "-" + colorConfig.getFor(order()).getServiceFeeId(serviceFee);
                lines.add(BasketLine.compensationLine(
                        serviceOrderId,
                        BigDecimal.ONE,
                        returnItem.getServiceFeeValues().get(serviceFee),
                        0,
                        serviceProduct,
                        createDeveloperPayload(order().getId(), null, null),
                        order().getCreationDate()
                ));
            }
        }

        checkBatchServiceOrderCreationCall(callIter, uid(),
                lines.stream().map(BasketLine::toCreateBalanceOrderParams).collect(Collectors.toList()));

        checkCreateAndPayBasket(callIter,
                compensationPayment.getBasketKey(),
                createCompensationCreateBasketCallParams(lines)
        );
    }

    // для кешового возврата создается и оплачивается новая корзина. поэтому это не совсем рефанд
    private void checkCashRefundBalanceCalls(OneElementBackIterator<ServeEvent> callIter,
                                             Refund refund) throws Exception {

        OneElementBackIterator<ServeEvent> balanceCallsIterator =
                new OneElementBackIterator<>(servedEvents(trustMockConfigurer.balanceMock()));

        checkBalanceCall(FindClient, balanceCallsIterator.next().getRequest(),
                ImmutableMap.of("passport_id", passportId));
        checkBalanceCall(GetClientContracts, balanceCallsIterator.next().getRequest(),
                ImmutableMap.of("passport_id", passportId));

        List<BasketLine> lines;
        if (order().isFulfilment()) {
            lines = new ArrayList<>(checkBalanceCallsForFFItemsReturn(callIter, refund));
            BasketLine line = checkBalanceCallsForFFDeliveryReturn(callIter);
            if (line != null) {
                lines.add(line);
            }
        } else {
            throw new UnsupportedOperationException("Unimplemented. DIY");
        }

        if (refundHelper.isAsyncRefundStrategyEnabled(PaymentGoal.ORDER_POSTPAY)) {
            // skip extra async events
            var index = callIter.getSource()
                    .stream()
                    .map(ServeEvent::getStubMapping)
                    .map(StubMapping::getName)
                    .collect(Collectors.toList()).lastIndexOf(CREATE_ORDERS_STUB);
            var source = new ArrayList<>(callIter.getSource());
            callIter = new OneElementBackIterator<>(source.subList(index, source.size()));
        }

        checkBatchServiceOrderCreationCall(
                callIter,
                order().getBuyer().getUid(),
                lines.stream().map(BasketLine::toCreateBalanceOrderParams).collect(Collectors.toList()));

        checkCreateAndPayBasket(callIter, refund.getTrustRefundKey().getRefundBasketKey(),
                createCashReturnCreateBasketCallParams(lines, refund));
    }

    private void checkCreateAndPayBasket(OneElementBackIterator<ServeEvent> callIter, TrustBasketKey expectedBasketId,
                                         CreateBasketParams createBasketParams) {
        TrustBasketKey basketKey = checkCreateBasketCall(callIter, createBasketParams);
        assertThat(basketKey, equalTo(expectedBasketId));

        Long uid = !Boolean.TRUE.equals(order().isNoAuth()) ? order().getBuyer().getUid() : null;
        checkPayBasketCall(callIter, uid, basketKey);
    }

    private Collection<BasketLine> checkBalanceCallsForFFItemsReturn(OneElementBackIterator<ServeEvent> callIter,
                                                                     Refund refund) throws Exception {
        List<BasketLine> lines = new ArrayList<>();
        for (OrderItem item : order().getItems()) {
            //NB: clientId = shopId = campaignId см. ShopSettingsHelper.createCustomNewPrepayMeta
            Long shopId = item.getSupplierId();
            if (item.getSupplierType() != SupplierType.FIRST_PARTY) {
                checkLoadPartnerCall(callIter, shopId);

                String serviceProductId = shopId + "_" + shopId + "_"
                        + colorConfig.getFor(order()).getServiceFeeId(TrustServiceFee.FROM_SHOP_TO_USER_ITEM_UV);
                checkOptionalCreateServiceProductCall(
                        callIter, product(shopId, serviceProductId, serviceProductId, 1)
                );
                paymentTestHelper.checkCreateServiceProductCached(serviceProductId, serviceProductId, 1, shopId);
                lines.add(createBasketLineForCashRefund(item, serviceProductId, refund));
            } else {
                TrustServiceFee fee = TrustServiceFee.FROM_SHOP_TO_USER_ITEM_UV;
                String serviceProductId = colorConfig.getFor(order()).get1PProductId(fee);
                int serviceFeeId = colorConfig.getFor(order()).getServiceFeeId(fee);
                checkOptionalCreateServiceProductCall(
                        callIter, product(MARKET_PARTNER_ID, serviceProductId, serviceProductId, serviceFeeId)
                );

                paymentTestHelper.checkCreateServiceProductCached(
                        serviceProductId,
                        serviceProductId,
                        serviceFeeId,
                        MARKET_PARTNER_ID);
                lines.add(createBasketLineForCashRefund(item, serviceProductId, refund));
            }
        }

        return lines;
    }

    private BigDecimal calcTotalSupplierSum(Return ret) {
        return ret.getItems().stream()
                .map(ReturnItem::getSupplierCompensation)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BasketLine createBasketLineForCashRefund(OrderItem item, String serviceProductId, Refund refund) {
        String balanceOrderId = order().getId() + "-cash-" + refund.getId() + "-refund-" + item.getId() + "-" +
                colorConfig.getFor(order()).getServiceFeeId(TrustServiceFee.FROM_SHOP_TO_USER_ITEM);
        BigDecimal price = order().hasCertificate() ?
                order().getExternalCertificate().getPrice().setScale(2, RoundingMode.HALF_UP) :
                item.getQuantPriceIfExistsOrBuyerPrice();

        return BasketLine.cashRefundBasketLine(
                balanceOrderId, item.getQuantityIfExistsOrCount(), price, item.getOfferName(),
                item.getVat().getTrustId(), null, serviceProductId,
                createDeveloperPayload(order().getId(), item.getSupplierType(), null),
                order().getCreationDate(), DEFAULT_SUPPLIER_INN
        );
    }

    private BasketLine checkBalanceCallsForFFDeliveryReturn(OneElementBackIterator<ServeEvent> callIter)
            throws Exception {

        Delivery delivery = orderService.getOrder(order().getId()).getDelivery();
        if (delivery.isFree()) {
            return null;
        }
        if (delivery.getBalanceOrderId() == null) {
            return null;
        }
        ShopMetaData shop = shopService.getMeta(order().getShopId());
        Integer serviceFeeId = colorConfig.getFor(order()).getServiceFeeId(TrustServiceFee.FROM_SHOP_TO_USER_ITEM_UV);
        String serviceProductId = shop.getCampaignId() + "_" + shop.getClientId() + "_" + serviceFeeId;
        checkOptionalCreateServiceProductCall(
                callIter, product(shop.getClientId(), serviceProductId, serviceProductId, 1)
        );

        String serviceOrderId = order().getId() + "-cash-delivery-" + order().getInternalDeliveryId() + "-" +
                colorConfig.getFor(order()).getServiceFeeId(TrustServiceFee.FROM_SHOP_TO_USER_OTHER);
        return BasketLine.cashRefundBasketLine(
                serviceOrderId, BigDecimal.ONE, order().getDelivery().getBuyerPrice(), "Доставка",
                order().getDelivery().getVat().getTrustId(), shopMetaData().getAgencyCommission(), serviceProductId,
                createDeveloperPayload(order().getId(), null, null),
                order().getCreationDate(),
                order().isFulfilment() ? PaymentTestHelper.DEFAULT_MARKET_INN : PaymentTestHelper.DEFAULT_SUPPLIER_INN
        );
    }

    private CreateBasketParams createCompensationCreateBasketCallParams(List<BasketLine> lines) {
        CreateBasketParams basketParams = createBasket()
                .withUid(order().getUid())
                .withPayMethodId("cash")
                .withCurrency(order().getBuyerCurrency())
                .withDeveloperPayload(createDeveloperPayload(order().getId(), null, null));
        lines.forEach(
                line -> basketParams.withOrder(line.getServiceOrderId(), line.getQuantity(), line.getPrice())
        );

        return basketParams;
    }

    private CreateBasketParams createCashReturnCreateBasketCallParams(List<BasketLine> lines, Refund refund) {
        BigDecimal totalLinesSum = lines.stream()
                .map(line -> avoidNull(line.price, ZERO).multiply(line.getQuantity()))
                .reduce(ZERO, BigDecimal::add);
        assertThat(totalLinesSum, closeTo(refund.getAmount(), new BigDecimal(0.001)));
        CreateBasketParams basketParams = paymentTestHelper.generateBasketParamsBuilder(
                "cash", order(), null, endsWith("/refunds/notify"), false, false)
                .withDeveloperPayload(createDeveloperPayload(order().getId(), null, true))
                .withPassParams(null);
        lines.forEach(line ->
                basketParams.withOrder(
                        line.getServiceOrderId(),
                        line.getQuantity(),
                        line.getPrice(),
                        line.getTitle(),
                        line.getNds(),
                        line.getInn(),
                        null));

        return basketParams;
    }

    private static String createDeveloperPayload(long orderId, SupplierType supplierType, Boolean printReceipt) {
        return createCustomJsonField(orderId, supplierType, printReceipt);
    }

    private static PassParams createPassParams(Order order) {
        PassParams params = new PassParams();
        Boolean force3ds = order.getProperty(OrderPropertyType.FORCE_THREE_DS);
        params.setMarketBlue3dsPolicyFromBoolean(force3ds);
        return params;
    }

    private static String createCustomJsonField(long orderId, SupplierType supplierType, Boolean printReceipt) {
        StringBuilder stringBuilder = new StringBuilder("{");
        stringBuilder.append(String.format("\"external_id\":\"%d\"", orderId))
                .append(",")
                .append(String.format("\"orderId\":\"%d\"", orderId));
        if (supplierType != null) {
            stringBuilder.append(",")
                    .append(String.format("\"supplier_type\":\"%s\"", supplierType.getStringName()));
        }
        if (printReceipt != null) {
            stringBuilder.append(",")
                    .append(String.format("\"print_receipt\":%s", printReceipt));
        }
        return stringBuilder.append("}").toString();
    }

    private static class BasketLine {

        private String serviceOrderId;
        private BigDecimal quantity;
        private BigDecimal price;
        private String title;
        private String nds;

        private Integer agencyCommission;
        private String serviceProductId;
        private String lineDeveloperPayload;
        private Date orderCreateDate;
        private String inn;

        @SuppressWarnings("checkstyle:ParameterNumber")
        static BasketLine cashRefundBasketLine(String serviceOrderId, BigDecimal quantity,
                                               BigDecimal price, String title,
                                               String nds, Integer agencyCommission, String serviceProductId,
                                               String developerPayload,
                                               Date orderCreateDate, String inn) {
            BasketLine result = new BasketLine();

            result.serviceOrderId = serviceOrderId;
            result.quantity = quantity;
            result.price = price;
            result.agencyCommission = agencyCommission;
            result.serviceProductId = serviceProductId;
            result.lineDeveloperPayload = developerPayload;
            result.orderCreateDate = orderCreateDate;
            result.title = title;
            result.nds = nds;
            result.inn = inn;
            return result;
        }

        static BasketLine compensationLine(String serviceOrderId, BigDecimal quantity, BigDecimal price,
                                           Integer agencyCommission,
                                           String serviceProductId, String developerPayload,
                                           Date orderCreateDate) {
            BasketLine result = new BasketLine();

            result.serviceOrderId = serviceOrderId;
            result.quantity = quantity;
            result.price = price;
            result.agencyCommission = agencyCommission;
            result.serviceProductId = serviceProductId;
            result.lineDeveloperPayload = developerPayload;
            result.orderCreateDate = orderCreateDate;
            return result;
        }


        String getServiceOrderId() {
            return serviceOrderId;
        }

        public String getInn() {
            return inn;
        }

        BigDecimal getQuantity() {
            return quantity;
        }

        BigDecimal getPrice() {
            return price;
        }

        String getTitle() {
            return title;
        }

        String getNds() {
            return nds;
        }

        CreateBalanceOrderParams toCreateBalanceOrderParams() {
            return new CreateBalanceOrderParams(
                    agencyCommission,
                    serviceProductId,
                    lineDeveloperPayload,
                    notNullValue(PassParams.class),
                    serviceOrderId,
                    orderCreateDate
            );
        }
    }
}
