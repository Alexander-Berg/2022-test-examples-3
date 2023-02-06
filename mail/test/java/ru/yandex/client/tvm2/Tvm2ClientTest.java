package ru.yandex.client.tvm2;

import java.util.Arrays;

import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpRequest;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.config.DnsConfigBuilder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.passport.tvmauth.BlackboxEnv;
import ru.yandex.test.util.TestBase;

public class Tvm2ClientTest extends TestBase {
    @Test
    public void testUserTicket() throws Exception {
        try (StaticServer tvm2 =
                new StaticServer(Configs.baseConfig("TVM2")))
        {
            Configs.setupTvmKeys(tvm2);
            tvm2.start();
            Tvm2ServiceConfigBuilder builder = new Tvm2ServiceConfigBuilder();
            builder.host(tvm2.host());
            builder.connections(2);
            builder.blackboxEnv(BlackboxEnv.TEST);
            Tvm2ServiceContextRenewalTask task =
                new Tvm2ServiceContextRenewalTask(
                    logger,
                    builder.build(),
                    new DnsConfigBuilder().build());
            BasicHttpRequest request =
                new BasicHttpRequest("GET", "/ping", HttpVersion.HTTP_1_1);
            request.addHeader(
                YandexHeaders.X_YA_USER_TICKET,
                "3:user:CA0Q__________9_GhQKBQiJ29UCEInb1QIg0oXYzAQoAQ:Gio3aix"
                + "JPSSzMmGM_MtaDkD3OGLIwPTEH9A8c8g8xaSs7XbdtqwQNCWduWaPvUEg1W"
                + "YQpT1MYT_rpdx1kp265E4T8daAWbxz-YJX6N1KPbj11yhx7piTiOt3BDHB3"
                + "9wqF5zU8QewV8B9IKU_CwQwHcHERSYUW1UVYgTU-NiYGRE");
            UserAuthResult result = task.checkUserAuthorization(
                request,
                YandexHeaders.X_YA_USER_TICKET);
            Assert.assertNull(result.errorDescription());
            Assert.assertEquals(
                "[5598601]",
                Arrays.toString(result.ticket().getUids()));
        }
    }
}

