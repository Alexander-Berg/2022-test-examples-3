package ru.yandex.util.ip;

import java.net.InetAddress;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.test.util.TestBase;

public class HbfParserTest extends TestBase {
    @Test
    public void test() throws Exception {
        final Long projectId = HbfParser.INSTANCE.parseProjectId(InetAddress.getByName("2a02:6b8:c10:24a5:0:5053:c59c:0"));
        Assert.assertNotNull(projectId);

        //https://racktables.yandex-team.ru/index.php?page=services&tab=projects&project_name=_PAYSYS_AWACS_TRUST_PROD_NETS_
        Assert.assertEquals("5053", Long.toHexString(projectId));
    }
}

