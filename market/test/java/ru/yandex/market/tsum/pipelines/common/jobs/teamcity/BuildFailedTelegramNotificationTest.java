package ru.yandex.market.tsum.pipelines.common.jobs.teamcity;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.tsum.clients.teamcity.BuildItem;
import ru.yandex.market.tsum.pipelines.common.resources.FixVersion;
import ru.yandex.market.tsum.pipelines.common.resources.ReleaseInfo;

public class BuildFailedTelegramNotificationTest {

    @Test
    public void getTelegramMessage() {
        BuildItem item = new BuildItem();
        item.setWebUrl("http://example.org");

        ReleaseInfo releaseInfo = new ReleaseInfo(new FixVersion(1, "2018.1.1"), "MBI-1234");

        BuildFailedTelegramNotification notification = new BuildFailedTelegramNotification(item, releaseInfo);

        String telegramMessage = notification.getTelegramMessage();

        Assert.assertEquals("\n" +
            "\uD83D\uDEE0 Сборка пакетов завершилась неудачно: [Ссылка на билд](http://example.org)\n" +
            "\n" +
            "Релиз [2018.1.1](https://st.yandex-team.ru/null/filter?fixVersions=1), релизный тикет [MBI-1234]" +
            "(https://st.yandex-team.ru/MBI-1234)", telegramMessage);
    }

}
