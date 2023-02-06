package ru.yandex.market.crm.operatorwindow.external;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.crm.domain.Phone;
import ru.yandex.market.crm.operatorwindow.external.platform.converter.PlatformUserIdsConverter;
import ru.yandex.market.crm.operatorwindow.jmf.TicketFirstLine;
import ru.yandex.market.crm.platform.commons.UserIds;
import ru.yandex.market.crm.util.CrmStrings;
import ru.yandex.market.ocrm.module.common.Customer;
import ru.yandex.market.ocrm.module.common.CustomerType;
import ru.yandex.market.ocrm.module.complaints.BeruComplaintsTicket;
import ru.yandex.market.ocrm.module.order.domain.Order;

import static org.mockito.Mockito.when;

public class PlatformUserIdsConverterTest {

    private static final Phone TEST_PHONE1 = Phone.fromRaw("+7 (922) 123-45-67");
    private static final String NORMALIZED_TEST_PHONE1 = "79221234567";
    private static final Phone TEST_PHONE2 = Phone.fromRaw("+7 (922) 123-45-68");
    private static final String NORMALIZED_TEST_PHONE2 = "79221234568";
    private static final String TEST_EMAIL1 = "test-email1@yandex.ru";
    private static final String TEST_EMAIL2 = "test-email2@yandex.ru";

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConvertFromTicketFirstLineWithOnlyPhone() {
        TicketFirstLine ticket = Mockito.mock(TicketFirstLine.class);
        when(ticket.getOrder()).thenReturn(null);
        when(ticket.getClientPhone()).thenReturn(TEST_PHONE1);
        when(ticket.getClientEmail()).thenReturn(null);

        UserIds usersIds = PlatformUserIdsConverter.convertFromTicket(ticket);
        assertUserIds(usersIds, 0, 0, NORMALIZED_TEST_PHONE1, CrmStrings.EMPTY_STRING);
    }

    @Test
    public void testConvertFromTicketBeruComplaintsWithOnlyEmail() {
        BeruComplaintsTicket ticket = Mockito.mock(BeruComplaintsTicket.class);
        when(ticket.getOrder()).thenReturn(null);
        when(ticket.getClientPhone()).thenReturn(null);
        when(ticket.getClientEmail()).thenReturn(TEST_EMAIL1);

        UserIds usersIds = PlatformUserIdsConverter.convertFromTicket(ticket);
        assertUserIds(usersIds, 0, 0, CrmStrings.EMPTY_STRING, TEST_EMAIL1);
    }

    @Test
    public void testConvertFromTicketFirstLineWithOrderUidAndPhone() {
        TicketFirstLine ticket = Mockito.mock(TicketFirstLine.class);
        long uid = 12345L;
        var customer = mockCustomer(CustomerType.YANDEX_TYPE);
        Order order = createOrder(uid, null);
        when(order.getCustomer()).thenReturn(customer);
        when(ticket.getOrder()).thenReturn(order);
        when(ticket.getClientPhone()).thenReturn(TEST_PHONE1);
        when(ticket.getClientEmail()).thenReturn(null);

        UserIds usersIds = PlatformUserIdsConverter.convertFromTicket(ticket);
        assertUserIds(usersIds, uid, 0, "", "");
    }

    @Test
    public void testConvertFromTicketFirstLineWithOrderMuidAndEmail() {
        TicketFirstLine ticket = Mockito.mock(TicketFirstLine.class);
        long muid = 12345L;
        var customer = mockCustomer(CustomerType.MUID_TYPE);
        Order order = createOrder(null, muid);
        when(order.getCustomer()).thenReturn(customer);
        when(ticket.getOrder()).thenReturn(order);
        when(ticket.getClientPhone()).thenReturn(null);
        when(ticket.getClientEmail()).thenReturn(TEST_EMAIL1);

        UserIds usersIds = PlatformUserIdsConverter.convertFromTicket(ticket);
        assertUserIds(usersIds, 0, muid, "", "");
    }

    private Order createOrder(Long uid, Long muid) {
        Order order = Mockito.mock(Order.class);
        when(order.getBuyerUid()).thenReturn(uid);
        when(order.getBuyerMuid()).thenReturn(muid);
        when(order.getBuyerPhone()).thenReturn(TEST_PHONE2);
        when(order.getBuyerEmail()).thenReturn(TEST_EMAIL2);
        return order;
    }

    private void assertUserIds(UserIds usersIds, long puid, long muid, String phone, String email) {
        Assertions.assertNotNull(usersIds);
        Assertions.assertEquals(puid, usersIds.getPuid());
        Assertions.assertEquals(muid, usersIds.getMuid());
        Assertions.assertEquals(phone, usersIds.getPhone());
        Assertions.assertEquals(email, usersIds.getEmail());
    }

    private Customer mockCustomer(String customerTypeKey) {
        var customerType = Mockito.mock(CustomerType.class);
        when(customerType.getCode()).thenReturn(customerTypeKey);

        var customer = Mockito.mock(Customer.class);
        when(customer.getType()).thenReturn(customerType);

        return customer;
    }

}
