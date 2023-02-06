package ru.yandex.market.checkout.checkouter.tasks.v2.personaldata.mappers;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkouter.jooq.Tables;
import ru.yandex.market.checkouter.jooq.tables.records.OrderBuyerRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class PhoneMapperTest {

    @Test
    public void normalize() {
        var mapper = new PhoneMapper<>(Tables.ORDER_BUYER.PERSONAL_PHONE_ID, Tables.ORDER_BUYER.PHONE);
        assertNull(mapper.map(phoneRecord(null)));
        assertNull(mapper.map(phoneRecord("")));
        assertNull(mapper.map(phoneRecord(" ")));
        assertNull(mapper.map(phoneRecord("abc")));
        assertEquals("+7", mapper.map(phoneRecord("7")).getPhone());
        assertEquals("+71234567890", mapper.map(phoneRecord("71234567890")).getPhone());
        assertEquals("+71234567890", mapper.map(phoneRecord("7(123)456-78-90")).getPhone());
        assertEquals("+71234567890", mapper.map(phoneRecord("+71234567890")).getPhone());
        assertEquals("+71234567890", mapper.map(phoneRecord("+7(123)456-78-90")).getPhone());
    }

    private OrderBuyerRecord phoneRecord(String phone) {
        OrderBuyerRecord record = new OrderBuyerRecord();
        record.setPhone(phone);
        return record;
    }

}
