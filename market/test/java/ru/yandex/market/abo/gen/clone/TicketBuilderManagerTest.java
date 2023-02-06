package ru.yandex.market.abo.gen.clone;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.gen.TicketBuilderManager;

/**
 * @author Olga Bolshakova
 *         @date 09-Oct-2007
 *         Time: 16:20:37
 */
public class TicketBuilderManagerTest extends EmptyTest {

    @Autowired
    private TicketBuilderManager ticketBuilderManager;

    @Test
    public void testBuildComplain() {
        ticketBuilderManager.generateTickets();
    }
}
