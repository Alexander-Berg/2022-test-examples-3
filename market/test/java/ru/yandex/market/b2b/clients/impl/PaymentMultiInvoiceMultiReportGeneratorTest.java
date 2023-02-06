package ru.yandex.market.b2b.clients.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.b2b.clients.CustomersStorageService;
import ru.yandex.mj.generated.server.model.CustomerDto;
import ru.yandex.mj.generated.server.model.OrderCustomerDto;
import ru.yandex.mj.generated.server.model.OrderDeliveryDto;
import ru.yandex.mj.generated.server.model.OrderDto;
import ru.yandex.mj.generated.server.model.OrderItemDto;
import ru.yandex.mj.generated.server.model.Vat;

import static ru.yandex.market.b2b.clients.impl.PaymentInvoiceFastPayQRGenerator.fastPaymentQR;

@ExtendWith(MockitoExtension.class)
public class PaymentMultiInvoiceMultiReportGeneratorTest {

    @Mock
    private CustomersStorageService customersStorageService;
    @Mock
    private SellerProps sellerProps;

    @InjectMocks
    private PaymentMultiInvoiceMultiReportGenerator reportGenerator;

    @Test
    public void getParameters_providesCorrectParameters() {
        // Given
        BigDecimal invoiceNumber = BigDecimal.valueOf(123);

        BigDecimal order1Id = BigDecimal.valueOf(1);
        OrderItemDto item1 = new OrderItemDto()
                .id(BigDecimal.valueOf(1))
                .name("Товар №1")
                .count(1)
                .price(BigDecimal.valueOf(10))
                .total(BigDecimal.valueOf(10))
                .vat(Vat.VAT_20);
        OrderDto order1 = new OrderDto()
                .id(order1Id)
                .customer(customer())
                .items(List.of(item1))
                .delivery(new OrderDeliveryDto().total(BigDecimal.valueOf(60)).vat(Vat.VAT_20));

        BigDecimal order2Id = BigDecimal.valueOf(2);
        OrderItemDto item2 = new OrderItemDto()
                .id(BigDecimal.valueOf(2))
                .name("Товар №2")
                .count(2)
                .price(BigDecimal.valueOf(20))
                .total(BigDecimal.valueOf(40))
                .vat(Vat.VAT_20);
        OrderDto order2 = new OrderDto()
                .id(order2Id)
                .customer(customer())
                .items(List.of(item2))
                .delivery(new OrderDeliveryDto().total(BigDecimal.valueOf(50)).vat(Vat.VAT_20));

        OrdersDtoWithDate context = new OrdersDtoWithDate(List.of(order1, order2), invoiceNumber);

        Mockito.doReturn(customerDto()).when(customersStorageService).getCustomer(customer().getId());

        setUpSeller();

        // When
        Map<String, Object> parameters = reportGenerator.getParameters(context);

        // Then
        List<Map<Object, Object>> items = new ArrayList<>(
                (Collection<Map<Object, Object>>) ((JRBeanCollectionDataSource) parameters.get("items")).getData()
        );
        org.assertj.core.api.Assertions.assertThat(items)
                .contains(
                        Map.of(
                                "price", BigDecimal.valueOf(10),
                                "vatTotal", BigDecimal.valueOf(1.67),
                                "number", 1,
                                "count", 1,
                                "name", "Товар №1",
                                "vatValue", "20%",
                                "total", BigDecimal.valueOf(10)),
                        Map.of(
                                "price", BigDecimal.valueOf(20),
                                "vatTotal", BigDecimal.valueOf(6.67),
                                "number", 2,
                                "count", 2,
                                "name", "Товар №2",
                                "vatValue", "20%",
                                "total", BigDecimal.valueOf(40)),
                        Map.of(
                                "price", BigDecimal.valueOf(60),
                                "vatTotal", BigDecimal.valueOf(1000, 2),
                                "number", 3,
                                "count", 1,
                                "name", "Услуги доставки по заказу № 1",
                                "vatValue", "20%",
                                "total", BigDecimal.valueOf(60)),
                        Map.of(
                                "price", BigDecimal.valueOf(50),
                                "vatTotal", BigDecimal.valueOf(8.34),
                                "number", 4,
                                "count", 1,
                                "name", "Услуги доставки по заказу № 2",
                                "vatValue", "20%",
                                "total", BigDecimal.valueOf(50))
                );

        Assertions.assertEquals(context.getOrderTotal(), parameters.get("orderTotal"));
        Assertions.assertEquals(context.getOrderVatTotal(), parameters.get("orderVatTotal"));

        Assertions.assertEquals(invoiceNumber.toString(), parameters.get("paymentInvoiceNumber"));
        Assertions.assertEquals(
                String.join(", ", List.of(order1Id.toString(), order2Id.toString())),
                parameters.get("orderIds"));
        Assertions.assertNotNull(parameters.get("paymentInvoiceDate"));
        Assertions.assertEquals(context.getPurposeOfPayment(), parameters.get("purposeOfPayment"));

        Assertions.assertEquals(customerDto().getName(), parameters.get("customerName"));
        Assertions.assertEquals(customer().getPhone(), parameters.get("customerPhone"));
        Assertions.assertEquals(customerDto().getInn(), parameters.get("customerInn"));
        Assertions.assertEquals(customerDto().getKpp(), parameters.get("customerKpp"));
        Assertions.assertEquals(customer().getBuyer(), parameters.get("customerBuyer"));
        Assertions.assertEquals(customerDto().getLegalAddress(), parameters.get("customerLegalAddress"));

        Assertions.assertEquals("Имя продавца", parameters.get("sellerName"));
        Assertions.assertEquals("ИНН продавца", parameters.get("sellerInn"));
        Assertions.assertEquals("КПП продавца", parameters.get("sellerKpp"));
        Assertions.assertEquals("Адрес продавца", parameters.get("sellerAddress"));
        Assertions.assertEquals("Телефон продавца", parameters.get("sellerPhone"));
        Assertions.assertEquals("Факс продавца", parameters.get("sellerFax"));
        Assertions.assertEquals("Банк продавца", parameters.get("bank"));
        Assertions.assertEquals("БИК продавца", parameters.get("bic"));
        Assertions.assertEquals("Корр счет продавца", parameters.get("correspondentAcc"));
        Assertions.assertEquals("Счет продавца", parameters.get("personalAcc"));
        Assertions.assertEquals("Имя директора продавца", parameters.get("director"));
        Assertions.assertEquals("Имя бухгалтера продавца", parameters.get("accountant"));

        Assertions.assertEquals(fastPaymentQR(context, sellerProps), parameters.get("fastPaymentQR"));
    }

    private void setUpSeller() {
        Mockito.doReturn("Имя продавца").when(sellerProps).getName();
        Mockito.doReturn("ИНН продавца").when(sellerProps).getInn();
        Mockito.doReturn("КПП продавца").when(sellerProps).getKpp();
        Mockito.doReturn("Адрес продавца").when(sellerProps).getAddress();
        Mockito.doReturn("Телефон продавца").when(sellerProps).getPhone();
        Mockito.doReturn("Факс продавца").when(sellerProps).getFax();
        Mockito.doReturn("Банк продавца").when(sellerProps).getBank();
        Mockito.doReturn("БИК продавца").when(sellerProps).getBic();
        Mockito.doReturn("Корр счет продавца").when(sellerProps).getCorrespondentAcc();
        Mockito.doReturn("Счет продавца").when(sellerProps).getPersonalAcc();
        Mockito.doReturn("Имя директора продавца").when(sellerProps).getDirector();
        Mockito.doReturn("Имя бухгалтера продавца").when(sellerProps).getAccountant();
    }

    private CustomerDto customerDto() {
        CustomerDto customer = new CustomerDto();
        customer.setName("Название покупателя");
        customer.setInn("ИНН покупателя");
        customer.setKpp("КПП покупателя");
        customer.setLegalAddress("Юридический адрес покупателя");
        return customer;
    }

    private OrderCustomerDto customer() {
        return new OrderCustomerDto()
                .id(2L)
                .buyer("Имя покупателя")
                .phone("Телефон покупателя");
    }
}
