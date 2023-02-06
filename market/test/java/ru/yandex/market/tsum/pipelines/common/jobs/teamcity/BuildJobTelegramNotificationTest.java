package ru.yandex.market.tsum.pipelines.common.jobs.teamcity;

import java.io.IOException;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.tsum.pipelines.common.resources.FixVersion;
import ru.yandex.market.tsum.pipelines.common.resources.ReleaseInfo;

public class BuildJobTelegramNotificationTest {

    @Test
    public void getTelegramMessage() throws IOException {
        String artifactsJson = Resources.toString(Resources.getResource("artifacts/package.json"), Charsets.UTF_8);
        PackageArtifact artifact = new Gson().fromJson(artifactsJson, PackageArtifact.class);

        ReleaseInfo releaseInfo = new ReleaseInfo(new FixVersion(1, "2018.4.11"), "MBO-13966");

        BuildJobTelegramNotification notification = new BuildJobTelegramNotification(artifact.getPackages(),
            releaseInfo);

        String expectedMessage = "\n" +
            "\n" +
            "\uD83D\uDCE6 Следующие пакеты собраны:\n" +
            "• [gurulight-ui](\"https://sandbox.yandex-team.ru/resources?type=MARKET_FRONT_MBO_GURULITE_UI_VIEW\") " +
            "версии 2.241-2018.4.11+1\n" +
            "• [yandex-mbo-http-exporter](\"https://c.yandex-team.ru/packages/yandex-mbo-http-exporter\") версии 2" +
            ".264-2018.4.11+1\n" +
            "• [yandex-mbo-offers-api](\"https://c.yandex-team.ru/packages/yandex-mbo-offers-api\") версии 1.32-2018" +
            ".4.11+1\n" +
            "• [yandex-mbo-card-api](\"https://c.yandex-team.ru/packages/yandex-mbo-card-api\") версии 1.257-2018.4" +
            ".11+1\n" +
            "• [yandex-mbo-lite](\"https://c.yandex-team.ru/packages/yandex-mbo-lite\") версии 3.257-2018.4.11+1\n" +
            "• [yandex-mbo-db](\"https://c.yandex-team.ru/packages/yandex-mbo-db\") версии 1.2.2018.4.11.1-0\n" +
            "• [yandex-mbo-db-2](\"https://c.yandex-team.ru/packages/yandex-mbo-db-2\") версии 1.3.2018.4.11.1-0\n" +
            "• [yandex-mbo-tms](\"https://c.yandex-team.ru/packages/yandex-mbo-tms\") версии 2.284-2018.4.11+1\n" +
            "• [verstka](\"https://sandbox.yandex-team.ru/resources?type=MARKET_FRONT_MBO_VIEW\") версии 2.242-2018.4" +
            ".11+1\n" +
            "• [yandex-mbo-audit](\"https://c.yandex-team.ru/packages/yandex-mbo-audit\") версии 2.246-2018.4.11+1\n" +
            "• [yandex-mbo-gwt](\"https://c.yandex-team.ru/packages/yandex-mbo-gwt\") версии 2.278-2018.4.11+1\n" +
            "\n" +
            "Релиз [2018.4.11](https://st.yandex-team.ru/null/filter?fixVersions=1), релизный тикет [MBO-13966]" +
            "(https://st.yandex-team.ru/MBO-13966)";
        String telegramMessage = notification.getTelegramMessage();
        Assert.assertEquals(expectedMessage, telegramMessage);
    }
}
