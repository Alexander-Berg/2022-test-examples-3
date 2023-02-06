package ru.yandex.market.crm.triggers.services.order;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;

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
import ru.yandex.market.checkout.checkouter.order.VatType;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.crm.external.blackbox.BlackBoxClient;
import ru.yandex.market.crm.external.blackbox.response.UserInfo;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.triggers.services.bpm.variables.BusinessOrderInfo;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerInfoDTO;
import ru.yandex.mj.generated.client.b2bclients.model.CustomerDto;
import ru.yandex.mj.generated.client.b2bclients.model.DocumentDto;
import ru.yandex.mj.generated.client.b2bclients.model.DocumentResponseDto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.crm.triggers.utils.OrderUtils.getOrderLinkByColor;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {
        BusinessOrderInfoProviderTest.TestConfiguration.class
})
public class BusinessOrderInfoProviderTest {
    @Configuration
    @ImportResource("classpath:/WEB-INF/checkouter-client.xml")
    static class TestConfiguration {
    }

    private static final String PARTNER_NAME = "Рога и Копыта";
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Doe";
    private static final Long PUID = 123456789L;
    private static final String CUSTOMER_NAME = "ООО Наша фирма";
    private static final String CUSTOMER_INN = "1234567890";
    private static final String CUSTOMER_KPP = "123456789";
    private static final String PAYMENT_INVOICE_URL = "https://example.com";

    @Inject
    @Named("checkouterAnnotationObjectMapper")
    private ObjectMapper objectMapper;

    private BusinessOrderInfoProvider businessOrderInfoProvider;

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

        businessOrderInfoProvider = new BusinessOrderInfoProvider(mbiCLient, blackBoxClient);
    }

    @Test
    public void testOrderInfo() {
        var order = loadOrder();
        var uid = Uid.asPuid(PUID);

        BusinessOrderInfo orderInfo = businessOrderInfoProvider.getBusinessOrderInfo(uid, order, mockCustomer(),
                PAYMENT_INVOICE_URL, LocalDate.of(2022, 1, 31), LocalDate.of(2022, 2, 2));

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
        assertEquals("ООО Ромашка", orderInfo.getBusinessRecipientName());
        assertEquals("1234567890", orderInfo.getBusinessRecipientInn());
        assertEquals("123456789", orderInfo.getBusinessRecipientKpp());
        assertEquals("13", orderInfo.getAddress().getFloor());
        assertEquals("", orderInfo.getAddress().getApartment());
        assertEquals("0", orderInfo.getTotalNds());
        assertEquals(CUSTOMER_NAME, orderInfo.getBusinessCustomerName());
        assertEquals(CUSTOMER_INN, orderInfo.getBusinessCustomerInn());
        assertEquals(CUSTOMER_KPP, orderInfo.getBusinessCustomerKpp());
        assertEquals(PAYMENT_INVOICE_URL, orderInfo.getPaymentInvoiceUrl());
        assertEquals("2 февраля", orderInfo.getPaymentDate());
        assertEquals("31 января", orderInfo.getRecPaymentDate());
    }

    @Test
    public void testNonZeroVat() {
        var order = loadOrder();
        var uid = Uid.asPuid(PUID);
        for (var item : order.getItems()) {
            item.setBuyerPrice(BigDecimal.valueOf(100.30));
            item.setCount(2);
            item.setVat(VatType.VAT_20);
        }

        BusinessOrderInfo orderInfo = businessOrderInfoProvider.getBusinessOrderInfo(uid, order, mockCustomer(),
                PAYMENT_INVOICE_URL, LocalDate.of(2022, 1, 31), LocalDate.of(2022, 2, 2));
        assertEquals("33,44", orderInfo.getTotalNds());
    }

    @Test
    public void testOrderInfoClientsNameFromPassport() {
        var order = loadOrder();
        order.getBuyer().setFirstName(null);
        order.getBuyer().setLastName(null);
        var uid = Uid.asPuid(PUID);

        var orderInfo = businessOrderInfoProvider.getBusinessOrderInfo(uid, order, mockCustomer(),
                PAYMENT_INVOICE_URL, LocalDate.of(2022, 1, 31), LocalDate.of(2022, 2, 2));

        assertNotNull(orderInfo);
        assertEquals(orderInfo.getClientName(), String.format("%s %s", FIRST_NAME, LAST_NAME));
    }

    private Order loadOrder() {
        try {
            byte[] message = IOUtils.toByteArray(getClass().getResourceAsStream("BusinessOrder.json"));
            return objectMapper.readValue(message, Order.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private CustomerDto mockCustomer() {
        var customerDto = mock(CustomerDto.class);
        when(customerDto.getName()).thenReturn(CUSTOMER_NAME);
        when(customerDto.getInn()).thenReturn(CUSTOMER_INN);
        when(customerDto.getKpp()).thenReturn(CUSTOMER_KPP);
        return customerDto;
    }
}
