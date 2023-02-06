package ru.yandex.chemodan.app.docviewer.copy.provider;

import org.junit.Test;

import ru.yandex.chemodan.app.docviewer.TestUser;
import ru.yandex.chemodan.app.docviewer.copy.DocumentSourceInfo;
import ru.yandex.misc.test.Assert;

/**
 * @author yashunsky
 */
public class TrackerUrlProviderTest {
    @Test
    public void rewriteUrl() {
        TrackerUrlProvider provider = new TrackerUrlProvider("st-api.test.yandex-team.ru");
        String result = provider.rewriteUrl(
                DocumentSourceInfo.builder().originalUrl("tracker://0/100").uid(TestUser.YA_TEAM_AKIRAKOZOV.uid).build());
        Assert.equals("https://st-api.test.yandex-team.ru/v2/attachments/100/storageLink?org-id=0&uid=1120000000000744", result);
    }
}
