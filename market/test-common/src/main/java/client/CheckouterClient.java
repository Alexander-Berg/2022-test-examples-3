package client;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import api.CheckouterApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import dto.requests.checkouter.Address;
import dto.requests.checkouter.CancellationRequest;
import dto.requests.checkouter.DeliveryDtoRequest;
import dto.requests.checkouter.RearrFactor;
import dto.requests.checkouter.UpdateDeliveryRequest;
import dto.requests.checkouter.cart.CartItem;
import dto.requests.checkouter.cart.CartRequest;
import dto.requests.checkouter.checkout.Buyer;
import dto.requests.checkouter.checkout.CheckoutRequest;
import dto.requests.report.OfferItem;
import dto.responses.checkouter.ReturnResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Assertions;
import retrofit2.Response;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import ru.yandex.market.checkout.checkouter.balance.model.BalanceStatus;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.config.CheckouterAnnotationJsonConfig;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditOptions;
import ru.yandex.market.checkout.checkouter.order.OrderEditOptionsRequest;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.viewmodel.OrderViewModel;

import static toolkit.Retrofits.RETROFIT;

@Resource.Classpath({"delivery/checkouter.properties"})
@Slf4j
public class CheckouterClient {

    private static final String CLIENT_ROLE_SYSTEM = "SYSTEM";
    private static final String CLIENT_ROLE_USER = "USER";
    private static final String CLIENT_ROLE_CALL_CENTER_OPERATOR = "CALL_CENTER_OPERATOR";
    private static final ObjectMapper OBJECT_MAPPER = CheckouterAnnotationJsonConfig
        .objectMapperPrototype(new SimpleFilterProvider().setFailOnUnknownId(false));
    private final CheckouterApi checkouterApi;
    @Property("checkouter.host")
    private String host;
    @Property("checkouter.uid")
    private long uid;
    @Property("checkouter.returnPath")
    private String returnPath;
    @Property("checkouter.username")
    private String username;
    @Property("checkouter.password")
    private String password;
    @Property("checkouter.clientId")
    private long clientId;

    public CheckouterClient() {
        PropertyLoader.newInstance().populate(this);
        checkouterApi = RETROFIT.getRetrofit(host).create(CheckouterApi.class);
    }

    public void setUid(Long newUid) {
        this.uid = newUid;
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    @SneakyThrows
    public MultiCart cart(
        List<OfferItem> items,
        Long regionId,
        PaymentType paymentType,
        PaymentMethod paymentMethod,
        EnumSet<RearrFactor> experiments,
        Long forceDeliveryId,
        Address address,
        Color rgb
    ) {
        log.debug("Calling checkouter cart...");
        experiments.add(RearrFactor.GLOBAL);

        String finalExpString = experiments
            .stream()
            .map(RearrFactor::getValue)
            .collect(Collectors.joining(";"));

        DeliveryDtoRequest delivery = DeliveryDtoRequest.builder()
            .regionId(regionId)
            .address(address != null ? address.getAddress() : null)
            .build();

        List<CartItem> cartItems = items
            .stream()
            .collect(Collectors.groupingBy(OfferItem::getShopId))
            .entrySet().stream()
            .map(item -> new CartItem(
                    item.getKey(),
                    item.getValue().stream().flatMap(offerItem -> offerItem.getItems().stream())
                        .collect(Collectors.toList()),
                    delivery,
                    null
                )
            )
            .collect(Collectors.toList());

        CartRequest cartRequest = new CartRequest(regionId, false, "RUR", paymentType, paymentMethod, cartItems);

        Response<ResponseBody> cartResponse = checkouterApi.cart(
            TVM.INSTANCE.getServiceTicket(TVM.CHECKOUTER),
            finalExpString,
            true,
            uid,
            "1",
            rgb.name(),
            true,
            forceDeliveryId,
            cartRequest
        ).execute();
        Assertions.assertTrue(cartResponse.isSuccessful(), "Не удалось выполнить запрос к cart");
        Assertions.assertNotNull(cartResponse.body(), "Не удалось получить объект cart");
        return OBJECT_MAPPER.readValue(cartResponse.body().string(), MultiCart.class);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    @SneakyThrows
    public MultiOrder checkout(
        List<OfferItem> items,
        Long regionId,
        DeliveryDtoRequest delivery,
        final PaymentType paymentType,
        final PaymentMethod paymentMethod,
        EnumSet<RearrFactor> experiments,
        Long forceDeliveryId,
        Boolean fakeOrder,
        String comment,
        Buyer buyer,
        Color rgb
    ) {
        log.debug("Checking out...");
        experiments.add(RearrFactor.GLOBAL);

        String finalExpString = experiments
            .stream()
            .map(RearrFactor::getValue)
            .collect(Collectors.joining(";"));

        List<CartItem> cartItems = items
            .stream()
            .collect(Collectors.groupingBy(OfferItem::getShopId))
            .entrySet().stream()
            .map(item -> new CartItem(
                    item.getKey(),
                    item.getValue().stream().flatMap(offerItem -> offerItem.getItems().stream())
                        .collect(Collectors.toList()),
                    delivery,
                    comment
                )
            )
            .collect(Collectors.toList());

        CheckoutRequest checkoutRequest = new CheckoutRequest(
            regionId,
            false,
            "RUR",
            paymentType,
            paymentMethod,
            cartItems,
            buyer
        );

        Response<ResponseBody> checkoutResponse = checkouterApi.checkout(
            TVM.INSTANCE.getServiceTicket(TVM.CHECKOUTER),
            finalExpString,
            true,
            uid,
            "1",
            rgb.name(),
            true,
            forceDeliveryId,
            fakeOrder,
            fakeOrder ? "SANDBOX" : null,
            checkoutRequest
        ).execute();
        Assertions.assertTrue(checkoutResponse.isSuccessful(), "Не удалось выполнить запрос к checkout");
        Assertions.assertNotNull(checkoutResponse.body(), "Не удалось получить объект checkout");
        return OBJECT_MAPPER.readValue(checkoutResponse.body().string(), MultiOrder.class);

    }

    @SneakyThrows
    public String pay(Order order) {
        log.debug("Paying for order...");
        boolean fake = order.getPaymentType().equals(PaymentType.PREPAID);
        Response<ResponseBody> payResponse = checkouterApi.pay(
            TVM.INSTANCE.getServiceTicket(TVM.CHECKOUTER),
            "UNLIMIT",
            Credentials.basic(username, password),
            order.getId(),
            uid,
            returnPath,
            fake
        ).execute();
        Assertions.assertTrue(payResponse.isSuccessful(), "Не удалось выполнить запрос к pay");
        Assertions.assertNotNull(payResponse.body(), "Не удалось получить объект pay");
        Payment payment = OBJECT_MAPPER.readValue(payResponse.body().string(), Payment.class);
        return fake ? payment.getId().toString() : payment.getBasketKey().getPurchaseToken();
    }

    @SneakyThrows
    public void notifyFake(String paymentId) {
        log.debug("Paying for order...");
        Response<ResponseBody> payResponse = checkouterApi.notifyFake(
            TVM.INSTANCE.getServiceTicket(TVM.CHECKOUTER),
            Credentials.basic(username, password),
            "UNLIMIT",
            paymentId,
            true,
            BalanceStatus.success
        ).execute();
        Assertions.assertTrue(payResponse.isSuccessful(), "Не удалось выполнить запрос к pay");
        Assertions.assertNotNull(payResponse.body(), "Не удалось получить объект pay");
    }

    public void cancellationRequestByCallCenterOperator(Long orderId, Long shopId) {
        cancellationRequest(orderId, shopId, ClientRole.CALL_CENTER_OPERATOR, clientId);
    }

    public void cancellationRequestByUser(Long orderId, Long shopId) {
        cancellationRequest(orderId, shopId, ClientRole.USER, uid);
    }

    @SneakyThrows
    private void cancellationRequest(Long orderId, Long shopId, ClientRole clientRole, Long clientId) {
        log.debug("Cancelling order {}...", orderId);
        Response<ResponseBody> ordersItemResponse = checkouterApi.cancellationRequest(
            TVM.INSTANCE.getServiceTicket(TVM.CHECKOUTER),
            orderId,
            clientRole.name(),
            clientId,
            shopId,
            new CancellationRequest("Я передумал", "USER_CHANGED_MIND", "Расхотелось покупать товар")
        )
            .execute();
        Assertions.assertTrue(ordersItemResponse.isSuccessful(), "Не удалось отменить заказ " + orderId);
        Assertions.assertNotNull(ordersItemResponse.body(), "Пустое тело после отмены заказа " + orderId);
        Order order = OBJECT_MAPPER.readValue(ordersItemResponse.body().string(), Order.class);
        Assertions.assertNotNull(order.getCancellationRequest(), "Не пришел объект CancellationRequest " + orderId);
    }

    public void checkCancellationRequestExits(Long orderId) {
        Order order = getOrder(orderId);
        Assertions.assertNotNull(order.getCancellationRequest(), "Не пришел объект CancellationRequest " + orderId);
    }

    @SneakyThrows
    public void changeOrderStatus(Long orderId, Long shopId, OrderStatus status, OrderSubstatus substatus) {
        changeOrderStatus(CLIENT_ROLE_SYSTEM, clientId, orderId, shopId, status, substatus);
    }

    @SneakyThrows
    public void changeOrderStatus(
        String clientRole,
        Long clientId,
        Long orderId,
        Long shopId,
        OrderStatus status,
        OrderSubstatus substatus
    ) {
        Response<ResponseBody> ordersItemResponse = checkouterApi.changeOrderStatus(
            TVM.INSTANCE.getServiceTicket(TVM.CHECKOUTER),
            orderId,
            clientRole,
            clientId,
            shopId,
            status,
            substatus
        ).execute();
        Assertions.assertTrue(
            ordersItemResponse.isSuccessful(),
            String.format("Не удалось перевести заказ %d в статус %s, подстатус %s", orderId, status, substatus)
        );
        Assertions.assertNotNull(
            ordersItemResponse.body(),
            String.format("Пустое тело после перевода заказа %d в статус %s, подстатус %s", orderId, status, substatus)
        );
        Order order = OBJECT_MAPPER.readValue(ordersItemResponse.body().string(), Order.class);
        Assertions.assertEquals(status, order.getStatus(), "Не изменился статус orderId=" + orderId);
        Assertions.assertEquals(substatus, order.getSubstatus(), "Не изменился substatus orderId=" + orderId);
    }

    @SneakyThrows
    public Order getOrder(long orderId) {
        return getOrder(orderId, null);
    }

    @SneakyThrows
    public Order getOrder(long orderId, OptionalOrderPart[] partials) {
        return OBJECT_MAPPER.readValue(performGetOrder(orderId, partials), Order.class);
    }

    @SneakyThrows
    public OrderViewModel getOrderViewModel(long orderId, OptionalOrderPart[] partials) {
        return OBJECT_MAPPER.readValue(performGetOrder(orderId, partials), OrderViewModel.class);
    }

    @SneakyThrows
    private String performGetOrder(long orderId, OptionalOrderPart[] partials) {
        Response<ResponseBody> response = checkouterApi.getOrder(
            TVM.INSTANCE.getServiceTicket(TVM.CHECKOUTER),
            orderId,
            CLIENT_ROLE_SYSTEM,
            clientId,
            partials
        ).execute();
        Assertions.assertTrue(response.isSuccessful(), "Не удалось получить заказ " + orderId);
        Assertions.assertNotNull(response.body(), "Пустое тело после получения заказа " + orderId);
        return response.body().string();
    }

    @SneakyThrows
    public Order updateDeliveryService(Long orderId, Long shopId, Long deliveryServiceId, Color rgb) {
        Response<ResponseBody> ordersItemResponse = checkouterApi.updateDelivery(
            TVM.INSTANCE.getServiceTicket(TVM.CHECKOUTER),
            orderId,
            CLIENT_ROLE_SYSTEM,
            clientId,
            rgb,
            shopId,
            new UpdateDeliveryRequest(deliveryServiceId)
        ).execute();
        Assertions
            .assertTrue(ordersItemResponse.isSuccessful(), "Не удалось изменить службу доставки в заказе: " + orderId);
        Assertions.assertNotNull(
            ordersItemResponse.body(),
            "Пустое тело после обновления службы доставки в ордере: " + orderId
        );
        return OBJECT_MAPPER.readValue(ordersItemResponse.body().string(), Order.class);
    }

    @SneakyThrows
    public List<ChangeRequest> edit(
        Long orderId,
        Color rgb,
        Long shopId,
        Long businessId,
        OrderEditRequest orderEditRequest
    ) {
        Response<ResponseBody> ordersItemResponse = checkouterApi.edit(
            TVM.INSTANCE.getServiceTicket(TVM.CHECKOUTER),
            orderId,
            CLIENT_ROLE_SYSTEM,
            clientId,
            rgb,
            shopId,
            businessId,
            orderEditRequest
        ).execute();
        Assertions.assertTrue(ordersItemResponse.isSuccessful(), "Не удалось изменить заказ: " + orderId);
        Assertions.assertNotNull(ordersItemResponse.body(), "Пустое тело после обновления заказа: " + orderId);
        return Arrays.asList(OBJECT_MAPPER.readValue(ordersItemResponse.body().string(), ChangeRequest[].class));
    }

    @SneakyThrows
    public OrderEditOptions getEditOptions(
        Long orderId,
        Color rgb,
        Long shopId,
        Long businessId,
        OrderEditOptionsRequest orderEditOptionsRequest
    ) {
        Response<ResponseBody> ordersItemResponse = checkouterApi.getEditOptions(
            TVM.INSTANCE.getServiceTicket(TVM.CHECKOUTER),
            orderId,
            CLIENT_ROLE_SYSTEM,
            clientId,
            rgb,
            shopId,
            businessId,
            orderEditOptionsRequest
        ).execute();
        Assertions.assertTrue(
            ordersItemResponse.isSuccessful(),
            "Не удалось получить доступные опции изменения заказа: " + orderId
        );
        Assertions.assertNotNull(
            ordersItemResponse.body(),
            "Пустое тело после получения доступных опций изменения заказа: " + orderId
        );
        return OBJECT_MAPPER.readValue(ordersItemResponse.body().string(), OrderEditOptions.class);
    }

    @SneakyThrows
    public ReturnResponse initReturn(
        Return returnOrder,
        long orderId
    ) {
        Response<ReturnResponse> initReturnResponse = checkouterApi.initReturn(
            TVM.INSTANCE.getServiceTicket(TVM.CHECKOUTER),
            orderId,
            CLIENT_ROLE_USER,
            clientId,
            Color.BLUE,
            uid,
            returnOrder
        ).execute();
        Assertions.assertTrue(initReturnResponse.isSuccessful(), "Не удалось создать возврат заказа: " + orderId);
        Assertions.assertNotNull(initReturnResponse.body(), "Пустое тело ответа после создания возврата: " + orderId);
        return initReturnResponse.body();
    }
}
