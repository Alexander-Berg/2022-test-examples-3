package ru.yandex.chemodan.app.docviewer.copy;

import java.net.URI;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.chemodan.app.docviewer.DocviewerSpringTestBase;
import ru.yandex.chemodan.app.docviewer.copy.resourcemanagers.StorageResourceInfoManager;
import ru.yandex.chemodan.app.docviewer.utils.UriUtils;
import ru.yandex.misc.io.http.UrlUtils;
import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
public class StorageResourceInfoManagerTest extends DocviewerSpringTestBase {
    private final static URI MAIL_URL = UrlUtils.uri(
            "http://web-tst1j.yandex.ru:9090/mimes?uid=4006336590&mid=161848111608628001&part=1.2");

    @Autowired
    private StorageResourceInfoManager manager;

    // DOCVIEWER-2408
    @Test
    public void getYaDiskCopierResponseForUnderlineHost() {
        URI uri = UriUtils.toUri(
                "http://dep_geometry.pnzgu.ru/files/dep_geometry.pnzgu.ru/mnogomernaya_geometriya.pdf");
        Assert.none(manager.getStorageResourceInfoResponse(uri));
    }

}
