package ru.yandex.market.b2b.clients;

import java.math.BigDecimal;

import ru.yandex.mj.generated.server.model.ConsigneeDto;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConsigneeDtos {

    private ConsigneeDtos() {
    }

    public static ConsigneeDto getNewConsignee() {
        return new ConsigneeDto().customerId(new BigDecimal(123)).consigneeId(new BigDecimal(0)).
                name("тест").inn("1234567890").kpp("123456789");
    }

    public static void assertEqualsConsigneeDto(ConsigneeDto a, ConsigneeDto b) {
        assertEquals(a.getCustomerId(), b.getCustomerId());
        assertEquals(a.getConsigneeId(), b.getConsigneeId());
        assertEquals(a.getName(), b.getName());
        assertEquals(a.getInn(), b.getInn());
        assertEquals(a.getKpp(), b.getKpp());
    }


}
