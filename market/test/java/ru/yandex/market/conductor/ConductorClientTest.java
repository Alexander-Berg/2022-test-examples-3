package ru.yandex.market.conductor;

import java.util.Collection;
import java.util.Date;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.conductor.model.ConductorTicket;

/**
 * @author kukabara
 */
@Ignore
public class ConductorClientTest {

    private ConductorClient conductorClient;

    @Before
    public void init() throws Exception {

        conductorClient = new ConductorClient();
        conductorClient.setUrl("https://c.yandex-team.ru");

        conductorClient.afterPropertiesSet();
    }

    @Test
    public void testTasksFilter() throws Exception {
        Date startTime = new Date(115, 9, 20);
        Date endTime = new Date(115, 9, 21);
        Collection<ConductorTicket> tickets = conductorClient.tasksFilter(startTime, endTime, "cs", null);
        for (ConductorTicket ticket : tickets) {
            System.out.println(ticket);
        }
    }

}
