package ru.yandex.market.crm.triggers.services.order;


import java.io.IOException;
import java.util.ArrayList;

import javax.inject.Inject;
import javax.inject.Named;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.crm.core.domain.Color;
import ru.yandex.market.crm.core.util.CrmUrls;
import ru.yandex.market.crm.external.blackbox.BlackBoxClient;
import ru.yandex.market.crm.external.blackbox.response.UserInfo;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerInfoDTO;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.crm.triggers.utils.OrderUtils.getOrderLinkByColor;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {
        OrderInfoProviderTest.TestConfiguration.class
})
public class OrderInfoProviderTest {

    @Configuration
    @ImportResource("classpath:/WEB-INF/checkouter-client.xml")
    static class TestConfiguration {
    }

    private static final String PARTNER_NAME = "Рога и Копыта";
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Doe";
    private static final Long PUID = 123456789L;

    @Inject
    @Named("checkouterAnnotationObjectMapper")
    private ObjectMapper objectMapper;

    private OrderInfoProvider orderInfoProvider;

    @Before
    public void setUp() {
        var partnerName = new PartnerInfoDTO(123L, null, null, PARTNER_NAME, null,
                null, null, null, false, null);

        var mbiCLient = mock(MbiApiClient.class);
        when(mbiCLient.getPartnerInfo(anyLong()))
                .thenReturn(partnerName);

        var userInfo = new UserInfo();
        userInfo.setFirstName(FIRST_NAME);
        userInfo.setLastName(LAST_NAME);

        var blackBoxClient = mock(BlackBoxClient.class);
        when(blackBoxClient.getUserInfoByUid(anyLong()))
                .thenReturn(userInfo);

        orderInfoProvider = new OrderInfoProvider(mbiCLient, blackBoxClient);
    }

    @Test
    public void testOrderInfo() {
        var order = loadOrder();
        var uid = Uid.asPuid(PUID);

        var orderInfo = orderInfoProvider.getOrderInfo(uid, order);

        assertNotNull(orderInfo);
        assertEquals(order.getId(), orderInfo.getOrderId());
        assertEquals(order.getId().toString(), orderInfo.getOrderNumber());
        assertEquals(getOrderLinkByColor(order), orderInfo.getOrderLink());
        assertEquals(order.getSubstatus().name(), orderInfo.getOrderSubstatus());
        assertEquals(order.getRgb().name(), orderInfo.getColor());
        assertEquals("Name Last", orderInfo.getClientName());
        assertEquals(false, orderInfo.getCredit());
        assertEquals(false, orderInfo.getDsbs());
        assertEquals(PaymentType.PREPAID.name(), orderInfo.getPaymentType());
        assertEquals(false, orderInfo.getPaid());
        assertEquals(false, orderInfo.getShopDelivery());
        assertEquals(PARTNER_NAME, orderInfo.getShopName());
        assertEquals(1, orderInfo.getOrderItems().size());
        assertEquals("example@example.com", orderInfo.getBuyer().getEmail());
        assertEquals("77777777777", orderInfo.getBuyer().getPhone());
        assertEquals(987654321L, orderInfo.getBuyer().getUid().longValue());
        assertEquals(96L, orderInfo.getBuyer().getRegionId().longValue());
        assertEquals("Кузьмин Кузьма Кузьмич", orderInfo.getRecipientPerson());
        assertEquals("12332155", orderInfo.getRecipientPhone());
        assertEquals("kuzmin@kuzmin.ru", orderInfo.getRecipientEmail());
    }

    @Test
    public void testOrderInfoClientsNameFromPassport() {
        var order = loadOrder();
        order.getBuyer().setFirstName(null);
        order.getBuyer().setLastName(null);
        var uid = Uid.asPuid(PUID);

        var orderInfo = orderInfoProvider.getOrderInfo(uid, order);

        assertNotNull(orderInfo);
        assertEquals(orderInfo.getClientName(), String.format("%s %s", FIRST_NAME, LAST_NAME));
    }

    @Test
    public void testBlueOrderInfoModelInfo() {
        var order = loadOrder();
        var item = new ArrayList<>(order.getItems()).get(0);
        var uid = Uid.asPuid(PUID);

        var orderInfo = orderInfoProvider.getOrderInfo(uid, order);

        assertNotNull(orderInfo);
        assertNotNull(orderInfo.getOrderItems());
        assertEquals(1, orderInfo.getOrderItems().size());

        var modelInfo = orderInfo.getOrderItems().get(0);
        var modelId = item.getMsku().toString();
        assertEquals(modelId, modelInfo.getSku());
        assertEquals(CrmUrls.product(Color.BLUE, modelId), modelInfo.getItemLink());
    }

    @Test
    public void testGreenOrderInfoModelInfo() {
        var order = loadOrder();
        order.setRgb(ru.yandex.market.checkout.checkouter.order.Color.GREEN);
        var item = new ArrayList<>(order.getItems()).get(0);
        var uid = Uid.asPuid(PUID);

        var orderInfo = orderInfoProvider.getOrderInfo(uid, order);

        assertNotNull(orderInfo);
        assertNotNull(orderInfo.getOrderItems());
        assertEquals(1, orderInfo.getOrderItems().size());

        var modelInfo = orderInfo.getOrderItems().get(0);
        var modelId = item.getModelId();
        assertEquals(modelId, modelInfo.getModelId());
        assertEquals(CrmUrls.product(Color.GREEN, modelId.toString()), modelInfo.getItemLink());
    }

    @Test
    public void testItemServiceInfo() {
        var order = loadOrder();
        var uid = Uid.asPuid(PUID);

        var orderInfo = orderInfoProvider.getOrderInfo(uid, order);
        assertNotNull(orderInfo.getServices());
        assertEquals(1 ,orderInfo.getServices().size());

        var itemServiceInfo = orderInfo.getServices().get(0);
        assertEquals("service_1", itemServiceInfo.getTitle());
        assertEquals("service description", itemServiceInfo.getDescription());
        assertEquals("10", itemServiceInfo.getPrice());
    }

    @Test
    public void testPaymentInfo() {
        var order = loadOrder();
        var uid = Uid.asPuid(PUID);

        var orderInfo = orderInfoProvider.getOrderInfo(uid, order);
        assertNotNull(orderInfo.getPaymentMethod());
        assertEquals("TINKOFF_INSTALLMENTS", orderInfo.getPaymentMethod());
        assertEquals("10", orderInfo.getBuyerItemsTotalPrice());
        assertEquals("7", orderInfo.getBuyerItemsTotalPriceWithoutYaCashback());
        assertEquals(3, orderInfo.getTotalSpentYaCashback());
    }

    private Order loadOrder() {
        try {
            byte[] message = IOUtils.toByteArray(getClass().getResourceAsStream("ORDER.json"));
            return objectMapper.readValue(message, Order.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
