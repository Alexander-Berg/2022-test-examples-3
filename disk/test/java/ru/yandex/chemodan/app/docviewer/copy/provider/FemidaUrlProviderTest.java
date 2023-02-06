package ru.yandex.chemodan.app.docviewer.copy.provider;

import org.junit.Test;

import ru.yandex.chemodan.app.docviewer.TestUser;
import ru.yandex.chemodan.app.docviewer.copy.DocumentSourceInfo;
import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
public class FemidaUrlProviderTest {

    @Test
    public void rewriteUrl() {
        FemidaUrlProvider provider = new FemidaUrlProvider("femida.yandex-team.ru");
        String result = provider.rewriteUrl(
                DocumentSourceInfo.builder().originalUrl("ya-femida://12345").uid(TestUser.YA_TEAM_AKIRAKOZOV.uid).build());
        Assert.equals("https://femida.yandex-team.ru/api/attachments/12345", result);
    }
}
