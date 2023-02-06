package ru.yandex.crm.tests.delivery.stageThree;

import com.jayway.jsonpath.DocumentContext;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.core.CrmApi;
import ru.yandex.core.DbProvider;
import ru.yandex.core.CrmTestRunner;

/**
 * Created by nasyrov on 18.04.2016.
 */
@RunWith(CrmTestRunner.class)
public class TicketListTests extends DbProvider {

    @Test
    public void myListTest() throws Exception {
        CrmApi crm = new CrmApi();
        DocumentContext data = crm.getData("/view/support/ticket/list/my");

        Assert.assertTrue((Integer) data.read("ticketAvailableCount") >= 0);

        Assert.assertTrue((Integer) data.read("items[0].ticketId") > 0);
        Assert.assertTrue(new DateTime(2015,1,1,0,0,0).isBefore(DateTime.parse(data.read("items[0].date"))));
        Assert.assertEquals(crm.user().id, (long) data.read("items[0].owner.id"));
    }
}
