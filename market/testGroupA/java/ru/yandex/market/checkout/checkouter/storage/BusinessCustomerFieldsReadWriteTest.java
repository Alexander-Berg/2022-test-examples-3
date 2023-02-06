package ru.yandex.market.checkout.checkouter.storage;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.BuyerType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderNotFoundException;
import ru.yandex.market.checkout.helpers.OrderInsertHelper;
import ru.yandex.market.checkout.test.providers.BusinessRecipientProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BusinessCustomerFieldsReadWriteTest extends AbstractServicesTestBase {

    private static final long ORDER_ID = Integer.MAX_VALUE + 1L;
    private static final BuyerType BUYER_TYPE = BuyerType.BUSINESS;
    private static final long BUSINESS_BALANCE_ID = 111L;
    private static final long CONTRACT_ID = 222L;

    @Autowired
    private OrderReadingDao orderReadingDao;
    @Autowired
    private OrderInsertHelper orderInsertHelper;

    @BeforeEach
    void setUp() {
        final Order order = OrderProvider.getBlueOrder();

        order.getBuyer().setBusinessBalanceId(BUSINESS_BALANCE_ID);
        order.getBuyer().setContractId(CONTRACT_ID);
        order.getDelivery().setBusinessRecipient(BusinessRecipientProvider.getDefaultBusinessRecipient());

        orderInsertHelper.insertOrder(ORDER_ID, order);
    }

    @Test
    void shouldWriteAndReadBusinessCustomerFieldsToDb() {
        String expectedJson =
                "{\"inn\": \"123_test_inn_321\", \"kpp\": \"123_test_kpp_321\", \"name\": \"ООО Рога и Копыта (c)\"}";

        JdbcTemplate jdbcTemplate = getRandomWritableJdbcTemplate();

        // смотрим, что оно записало в базу при записи ордера

        Map<String, Object> resultBuyer = jdbcTemplate.queryForMap(
                "select business_balance_id, contract_id from order_buyer where order_id = ?",
                ORDER_ID
        );

        assertEquals(BUSINESS_BALANCE_ID, resultBuyer.get("business_balance_id"));
        assertEquals(CONTRACT_ID, resultBuyer.get("contract_id"));

        Map<String, Object> resultDelivery = jdbcTemplate.queryForMap(
                "select business_recipient from delivery_address " +
                        "where business_recipient is not null and order_id = ?",
                ORDER_ID
        );

        assertEquals(expectedJson, resultDelivery.get("business_recipient").toString());

        // смотрим, что оно записало в объекты при чтении из базы

        var order = orderReadingDao.getOrder(ORDER_ID, ClientInfo.SYSTEM)
                .orElseThrow(() -> new OrderNotFoundException(ORDER_ID));
        var buyer = order.getBuyer();
        var recipientFromDb = order.getDelivery().getBusinessRecipient();
        var expectedRecipient = BusinessRecipientProvider.getDefaultBusinessRecipient();

        assertEquals(BUYER_TYPE, buyer.getType());
        assertEquals(BUSINESS_BALANCE_ID, buyer.getBusinessBalanceId());
        assertEquals(CONTRACT_ID, buyer.getContractId());
        assertEquals(expectedRecipient, recipientFromDb);
    }
}
