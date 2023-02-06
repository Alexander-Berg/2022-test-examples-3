package ru.yandex.market.communication.proxy.api;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.direct.telephony.client.TelephonyClient;
import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.Recipient;
import ru.yandex.market.checkout.checkouter.delivery.RecipientPerson;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.communication.proxy.AbstractCommunicationProxyTest;
import ru.yandex.market.communication.proxy.service.PersonalMarketService;
import ru.yandex.mj.generated.server.model.CreateRedirectRequest;
import ru.yandex.mj.generated.server.model.CreateRedirectResponse;
import ru.yandex.mj.generated.server.model.LightCreateRedirectRequest;
import ru.yandex.mj.generated.server.model.RedirectInfoResponse;
import ru.yandex.telephony.backend.lib.proto.telephony_platform.ServiceNumber;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DbUnitDataSet(
        before = "environment.csv"
)
public class RedirectApiTest extends AbstractCommunicationProxyTest {

    private static final String CUSTOMER_PHONE_ID = "11111";
    private static final String CUSTOMER_PHONE = "+78005553535";
    private static final String PROXY_PHONE = "+79100050648";
    @Autowired
    private RedirectApiService redirectApiService;
    @Autowired
    private TelephonyClient telephonyClient;
    @Autowired
    private PersonalMarketService personalMarketService;

    @Mock
    private CheckouterClient checkouterClient;

    @Test
    @DbUnitDataSet(before = "getActiveRedirects.before.csv")
    void testGetRedirect() {
        ResponseEntity<RedirectInfoResponse> existing = redirectApiService.getRedirect(PROXY_PHONE);
        Assertions.assertEquals(HttpStatus.OK, existing.getStatusCode());
        Assertions.assertEquals(CUSTOMER_PHONE_ID, existing.getBody().getTargetNumberId());

        ResponseEntity<RedirectInfoResponse> nonExisting = redirectApiService.getRedirect("+79100050600");
        Assertions.assertEquals(HttpStatus.NOT_FOUND, nonExisting.getStatusCode());
    }

    @Test
    @DbUnitDataSet(after = "lightRedirectCreation.after.csv")
    void lightRedirectCreation() {
        when(telephonyClient.getServiceNumber(null))
                .thenReturn(ServiceNumber.newBuilder()
                        .setNum(PROXY_PHONE)
                        .setServiceNumberID("serviceNumberId")
                        .build());

        ResponseEntity<CreateRedirectResponse> existing = redirectApiService.lightRedirectCreation(
                new LightCreateRedirectRequest()
                        .sourcePhoneNumber("+77777777777")
                        .partnerId(1000L)
                        .orderId(3000L)
        );
        org.assertj.core.api.Assertions.assertThat(existing)
                .isNotNull()
                .matches(response -> response.getStatusCode() == HttpStatus.OK)
                .extracting(ResponseEntity::getBody)
                .isNotNull()
                .matches(body -> PROXY_PHONE.equals(((CreateRedirectResponse) body).getProxyNumber()));
    }

    @Test
    @DbUnitDataSet(after = "redirectCreation.after.csv")
    void redirectCreation() {
        ReflectionTestUtils.setField(redirectApiService, "checkouterClient", checkouterClient);
        var orderId = 12345943L;
        var order = createOrder(orderId, "shop_12345943", false);

        when(checkouterClient.getOrder(any(), any())).thenReturn(order);
        when(telephonyClient.getServiceNumber(null))
                .thenReturn(ServiceNumber.newBuilder()
                        .setNum(PROXY_PHONE)
                        .setServiceNumberID("y7yvzfYQOF8")
                        .build());
        when(personalMarketService.getPhoneNumberById(CUSTOMER_PHONE_ID)).thenReturn(CUSTOMER_PHONE);

        ResponseEntity<CreateRedirectResponse> redirect = redirectApiService.createRedirect(new CreateRedirectRequest()
                .orderId(orderId)
                .partnerId(1L)
        );

        Assertions.assertEquals(PROXY_PHONE, redirect.getBody().getProxyNumber());
    }

    @Test
    @DbUnitDataSet(
            before = "alreadyExists.before.csv",
            after = "alreadyExists.before.csv"
    )
    void redirectAlreadyExists() {
        ReflectionTestUtils.setField(redirectApiService, "checkouterClient", checkouterClient);
        var orderId = 12345943L;
        var order = createOrder(orderId, "shop_12345943", false);

        when(checkouterClient.getOrder(any(), any())).thenReturn(order);
        when(telephonyClient.getServiceNumber(null))
                .thenReturn(ServiceNumber.newBuilder()
                        .setNum("+77277272772")
                        .setServiceNumberID("y7yvzfYQOF8")
                        .build());
        when(personalMarketService.getPhoneNumberById(CUSTOMER_PHONE_ID)).thenReturn(CUSTOMER_PHONE);

        ResponseEntity<CreateRedirectResponse> redirect = redirectApiService.createRedirect(new CreateRedirectRequest()
                .orderId(orderId)
                .partnerId(1L)
        );

        Assertions.assertEquals(PROXY_PHONE, redirect.getBody().getProxyNumber());
    }

    @Test
    void redirectCreationTelephonyReturnedNull() {
        ReflectionTestUtils.setField(redirectApiService, "checkouterClient", checkouterClient);
        var orderId = 12345943L;
        var order = createOrder(orderId, "shop_12345943", false);

        when(checkouterClient.getOrder(any(), any())).thenReturn(order);
        when(telephonyClient.getServiceNumber(any())).thenReturn(null);
        when(personalMarketService.getPhoneNumberById(CUSTOMER_PHONE_ID)).thenReturn(CUSTOMER_PHONE);

        ResponseEntity<CreateRedirectResponse> redirect = redirectApiService.createRedirect(new CreateRedirectRequest()
                .orderId(orderId)
                .partnerId(1L)
        );

        Assertions.assertNull(redirect.getBody().getProxyNumber());
        Assertions.assertEquals(CUSTOMER_PHONE_ID, redirect.getBody().getRealNumberId());
    }

    private static Order createOrder(long orderId, String shopId, boolean fake) {
        ParcelBox parcelBox1 = new ParcelBox();
        parcelBox1.setId(1L);
        parcelBox1.setWeight(500L);
        parcelBox1.setFulfilmentId("FF100");

        ParcelBox parcelBox2 = new ParcelBox();
        parcelBox2.setId(2L);
        parcelBox2.setWeight(10500L);
        parcelBox2.setFulfilmentId("EXT123454152");

        ParcelBox parcelBox3 = new ParcelBox();
        parcelBox3.setId(3L);
        parcelBox3.setWeight(100000L);
        parcelBox3.setFulfilmentId("100");

        Parcel parcel = new Parcel();
        parcel.setId(1L);
        parcel.setBoxes(Arrays.asList(parcelBox2, parcelBox3, parcelBox1));

        Delivery delivery = new Delivery();
        delivery.setDeliveryServiceId(48L);
        delivery.setParcels(Collections.singletonList(parcel));
        delivery.setRecipient(new Recipient(
                new RecipientPerson("Сидр", "Сидорович", "Сидоров"),
                "+7 666-555-4433", "sidorov@ya.ru"
        ));

        Buyer buyer = new Buyer();
        buyer.setPersonalPhoneId(CUSTOMER_PHONE_ID);
        buyer.setPhone(CUSTOMER_PHONE);

        Order order = new Order();
        order.setId(orderId);
        order.setShopOrderId(shopId);
        order.setBuyer(buyer);
        order.setDelivery(delivery);
        order.setFake(fake);
        return order;
    }
}
