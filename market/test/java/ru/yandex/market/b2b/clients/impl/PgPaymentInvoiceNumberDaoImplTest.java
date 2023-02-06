package ru.yandex.market.b2b.clients.impl;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.b2b.clients.AbstractFunctionalTest;
import ru.yandex.market.b2b.clients.PaymentInvoiceNumberDao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PgPaymentInvoiceNumberDaoImplTest extends AbstractFunctionalTest {
    private JdbcTemplate jdbcTemplate;

    private PaymentInvoiceNumberDaoImpl dao;

    @Autowired
    public PgPaymentInvoiceNumberDaoImplTest(JdbcTemplate jdbcTemplate, PaymentInvoiceNumberDaoImpl dao) {
        this.jdbcTemplate = jdbcTemplate;
        this.dao = dao;
    }

    @Test
    public void invoiceNumberForManyOrders() {
        String multiOrderId = "multiOrderIdForManyPaymentInvoices";
        BigDecimal orderId1 = BigDecimal.valueOf(1);
        BigDecimal orderId2 = BigDecimal.valueOf(2);

        BigDecimal number = dao.getInvoiceNumberForManyOrders(multiOrderId, orderId1, 2);

        assertNotNull(number);

        assertEquals(number, dao.getInvoiceNumberForManyOrders(multiOrderId, orderId2, 2));
        assertEquals(number, dao.getInvoiceNumberForManyOrders(multiOrderId, orderId1, 2));

        InvoiceNumberDto invoiceNumberDto = dao.getInvoiceNumberItem(multiOrderId, null);

        assertEquals(number, invoiceNumberDto.getInvoiceId());
        assertEquals(multiOrderId, invoiceNumberDto.getMultiOrderId());
        assertEquals(2, invoiceNumberDto.getMultiOrderParts());
        assertTrue(List.of(orderId1, orderId2).containsAll(invoiceNumberDto.getOrderIds()) && invoiceNumberDto.getOrderIds().size() == 2);
    }

    @Test
    public void ordersIdForMultiOrder() {
        String multiOrderId = "multiOrderIdForManyPaymentInvoices";
        BigDecimal orderId1 = BigDecimal.valueOf(1);
        BigDecimal orderId2 = BigDecimal.valueOf(2);

        dao.getInvoiceNumberForManyOrders(multiOrderId, orderId1, 2);
        dao.getInvoiceNumberForManyOrders(multiOrderId, orderId2, 2);

        List<BigDecimal> ordersId = dao.getOrdersIdForMultiOrder(multiOrderId);
        assertTrue(List.of(orderId1, orderId2).containsAll(ordersId) && ordersId.size() == 2);
    }

    @Test
    public void getInvoiceNumberItemSingleOrder() {
        List<BigDecimal> orders = List.of(BigDecimal.ONE);
        BigDecimal number = dao.getInvoiceNumber(null, orders);
        assertNotNull(number);

        InvoiceNumberDto invoiceNumberDto = dao.getInvoiceNumberItem(null, orders);

        assertEquals(number, invoiceNumberDto.getInvoiceId());
        assertEquals(orders.get(0).toString(), invoiceNumberDto.getMultiOrderId());
        assertEquals(1, invoiceNumberDto.getMultiOrderParts());
        assertEquals(orders, invoiceNumberDto.getOrderIds());
    }

    @Test
    public void getInvoiceNumberItemMultiOrder() {
        List<BigDecimal> orders = List.of(BigDecimal.ONE, BigDecimal.TEN);
        String multiOrderId = "getInvoiceNumberItemMultiOrder";
        BigDecimal number = dao.getInvoiceNumber(multiOrderId, orders);
        assertNotNull(number);

        InvoiceNumberDto invoiceNumberDto = dao.getInvoiceNumberItem(multiOrderId, orders);

        assertEquals(number, invoiceNumberDto.getInvoiceId());
        assertEquals(multiOrderId, invoiceNumberDto.getMultiOrderId());
        assertEquals(2, invoiceNumberDto.getMultiOrderParts());
        assertTrue(orders.containsAll(invoiceNumberDto.getOrderIds()) && invoiceNumberDto.getOrderIds().size() == 2);
    }

    @Test
    public void getInvoiceNumber() {
        assertThrows(PaymentInvoiceNumberDao.PaymentInvoiceNumberException.class,
                () -> dao.getInvoiceNumber("aaa-aaa", List.of()));

        assertThrows(PaymentInvoiceNumberDao.PaymentInvoiceNumberException.class,
                () -> dao.getInvoiceNumber(null, List.of(BigDecimal.ONE, BigDecimal.TEN)));

        BigDecimal number = dao.getInvoiceNumber(null, List.of(BigDecimal.ONE));
        assertNotNull(number);
        assertEquals(number, dao.getInvoiceNumber(null, List.of(BigDecimal.ONE)));

        BigDecimal multiNumber = dao.getInvoiceNumber("aaa-bbb-ccc-ddd", List.of(BigDecimal.TEN));
        assertNotNull(multiNumber);
        assertEquals(multiNumber, dao.getInvoiceNumber("aaa-bbb-ccc-ddd", List.of(BigDecimal.TEN, BigDecimal.ONE)));
    }
}
