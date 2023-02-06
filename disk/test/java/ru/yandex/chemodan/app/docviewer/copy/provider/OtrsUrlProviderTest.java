package ru.yandex.chemodan.app.docviewer.copy.provider;

import org.junit.Test;

import ru.yandex.chemodan.app.docviewer.TestUser;
import ru.yandex.chemodan.app.docviewer.copy.DocumentSourceInfo;
import ru.yandex.misc.ip.HostPort;
import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
public class OtrsUrlProviderTest {

    private OtrsUrlProvider provider = new OtrsUrlProvider(
            new HostPort("storage-int.mdst.yandex.net", 80));

    @Test
    public void rewriteUrl() {
        String url = "otrs://MDS:858848/9746fff1-1d73-4cd1-bca0-fc9cd65faf93/";
        Assert.equals(
                "http://storage-int.mdst.yandex.net/get-otrs/858848/9746fff1-1d73-4cd1-bca0-fc9cd65faf93",
                provider.rewriteUrl(DocumentSourceInfo.builder().originalUrl(url).uid(TestUser.YA_TEAM_AKIRAKOZOV.uid).build()));
    }

}
