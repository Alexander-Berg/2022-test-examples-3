package ru.yandex.market.crm.platform.mappers;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.crm.external.personal.PersonalService;
import ru.yandex.market.crm.platform.commons.Uid;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.Order;
import ru.yandex.market.crm.util.ResourceHelpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.crm.platform.commons.UidType.MUID;
import static ru.yandex.market.crm.platform.commons.UidType.PUID;
import static ru.yandex.market.crm.platform.commons.UidType.UUID;
import static ru.yandex.market.crm.platform.commons.UidType.YANDEXUID;

/**
 * @author Alexander Novikov <a href="mailto:hronos@yandex-team.ru"></a>
 * @date 23.12.2019
 */
public class OrderAntifraudMapperTest {
    private List<Order> orders;

    @Test
    public void checkOrdersCount() {
        assertEquals(4, orders.size());
        Set<UidType> uidsSet = orders.stream()
            .map(Order::getKeyUid)
            .map(Uid::getType)
            .collect(Collectors.toSet());
        assertTrue(uidsSet.contains(UUID));
        assertTrue(uidsSet.contains(MUID));
        assertTrue(uidsSet.contains(PUID));
        assertTrue(uidsSet.contains(YANDEXUID));
    }

    @Before
    public void setUp() {
        byte[] resource = ResourceHelpers.getResource("order.json");
        var personalService = Mockito.mock(PersonalService.class);
        var ordersEnricher = new OrdersEnricher(personalService);
        orders = new OrderAntifraudMapper(ordersEnricher).apply(resource);
    }
}
