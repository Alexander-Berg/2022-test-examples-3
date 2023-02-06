package ru.yandex.market.crm.platform.mappers;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.crm.platform.models.OrderWasLost;
import ru.yandex.market.crm.util.CrmStrings;

/**
 * @see <a href="https://st.yandex-team.ru/LILUCRM-1307">LILUCRM-1307</a>
 */
public class OrderWasLostMapperTest {

    @Test
    public void check() {
        String msg = "{\n" +
                "  \"version\":\"1\",\n" +
                "  \"method\": \"orderWasLost\",\n" +
                "  \"params\":{\"orderId\":\"123\"}\n" +
                "}";

        List<OrderWasLost> facts = new OrderWasLostMapper().apply(CrmStrings.getBytes(msg));

        OrderWasLost fact = facts.get(0);
        Assert.assertEquals("Идентификатор факта должен быть номером заказа", 123, fact.getId());
    }
}
