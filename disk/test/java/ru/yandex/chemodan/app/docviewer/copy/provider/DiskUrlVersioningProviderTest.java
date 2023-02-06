package ru.yandex.chemodan.app.docviewer.copy.provider;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.chemodan.app.docviewer.copy.DocumentSourceInfo;
import ru.yandex.chemodan.app.docviewer.disk.mpfs.MpfsUrlHelper;

public class DiskUrlVersioningProviderTest {

    @Test
    public void rewriteUrl() {
        MpfsUrlHelper mpfsUrlHelper = new MpfsUrlHelper();
        mpfsUrlHelper.setMpfsHost("mpfs.ru");
        DiskUrlVersioningProvider provider = new DiskUrlVersioningProvider(mpfsUrlHelper);
        String rewriteUrl = provider.rewriteUrl(DocumentSourceInfo.builder()
                .originalUrl("ya-disk-version://version:/home/:dir").build());
        Assert.assertNotNull(rewriteUrl);
        Assert.assertEquals(rewriteUrl, "https://mpfs.ru/service/dv_data?uid=0&version_id=version&path=%2Fhome%2F%3Adir");
    }
}
