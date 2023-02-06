package ru.yandex.market.tsum.clients.app_sec;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.request.netty.HttpClientConfig;
import ru.yandex.market.request.netty.NettyHttpClientContext;
import ru.yandex.market.tsum.clients.app_sec.model.AppSecScanInfo;
import ru.yandex.market.tsum.clients.app_sec.model.AppSecScanStart;
import ru.yandex.market.tsum.clients.app_sec.model.AppSecScanStatus;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 21/12/2018
 */
@Ignore
public class AppSecClientIntegrationTest {
    @Test
    public void test() {
        AppSecClient client = new AppSecClient(
            "https://api.appsec-dashboard.yandex-team.ru",
            "",
            new NettyHttpClientContext(new HttpClientConfig())
        );

        AppSecScanStart response = client.start(177L);
        AppSecScanInfo info = client.poll(response.getData().getScanId());
        Assert.assertNotNull(info);
        Assert.assertEquals(200L, (long) info.getCode());
        Assert.assertEquals(AppSecScanStatus.CREATED, info.getData().getStatus());
    }
}
