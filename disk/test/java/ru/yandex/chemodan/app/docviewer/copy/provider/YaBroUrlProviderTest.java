package ru.yandex.chemodan.app.docviewer.copy.provider;

import org.junit.Test;

import ru.yandex.chemodan.app.docviewer.copy.ActualUri;
import ru.yandex.chemodan.app.docviewer.copy.DocumentSourceInfo;
import ru.yandex.misc.ip.HostPort;
import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
public class YaBroUrlProviderTest {

    private YaBroUrlProvider provider = new YaBroUrlProvider("browser-tmp",
            new HostPort("storage-int.mdst.yandex.net", 80));
    private final String url = "http://storage-int.mdst.yandex.net/get-browser-tmp/4231/some_mds_key_value/part2";

    @Test
    public void rewriteUrl() {
        DocumentSourceInfo docInfo = DocumentSourceInfo.builder().originalUrl("ya-browser://4231/some_mds_key_value/part2?sign=123").build();
        Assert.equals(url, provider.rewriteUrl(docInfo));

        docInfo = DocumentSourceInfo.builder().originalUrl("ya-browser://4231/some_mds_key_value/part2").build();
        Assert.equals(url, provider.rewriteUrl(docInfo));

    }

    @Test
    public void isSupportedActualUri() {
        Assert.isTrue(provider.isSupportedActualUri(new ActualUri(url)));
    }

    @Test
    public void getDiskResourceId() {
        Assert.equals("4231/some_mds_key_value/part2",
                provider.getDiskResourceId(new ActualUri(url)).get().getServiceFileId());
    }
}
