package ru.yandex.crm.tests.delivery.stageThree;

import com.jayway.jsonpath.DocumentContext;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.core.CrmApi;
import ru.yandex.core.DbProvider;

/**
 * Created by agroroza on 23.03.2016.
 */

public class AppInfoTests extends DbProvider {

    @Test
    public void appInfoTest() throws Exception {
        DocumentContext data = new CrmApi().getData("/info");
        Assert.assertTrue((Long)data.read("user.id") > 0);
        Assert.assertFalse(((String) data.read("user.login")).isEmpty());
        Assert.assertTrue((Boolean)data.read("testMode"));
    }
}