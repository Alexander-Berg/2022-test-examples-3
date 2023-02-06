package ru.yandex.crm.tests.delivery.stageTwo;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.core.MailProvider;
import ru.yandex.crm.tests.support.SupportRequestStorage;
import ru.yandex.crm.tests.support.RequestRow;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Created by agroroza on 17.03.2016.
 */
public class requestSimpleTest extends MailProvider {
    public requestSimpleTest() throws IOException {
    }

    @Test
    public void requestSimpleTest() throws SQLException {
        String requestSimpleTest = getStageMessageId("requestSimple");

        RequestRow request = SupportRequestStorage.getRequestByMessageId(requestSimpleTest);
        Assert.assertNotNull(request);
        Assert.assertEquals(10304, request.queueId);
        System.out.println("queue id 10304 and "+request.queueId);
        Assert.assertEquals(1128, request.categoryId);
        System.out.println("category id 1128 and "+request.categoryId);
    }

}