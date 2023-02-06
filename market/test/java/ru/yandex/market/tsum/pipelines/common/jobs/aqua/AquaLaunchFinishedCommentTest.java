package ru.yandex.market.tsum.pipelines.common.jobs.aqua;

import org.junit.Test;

import ru.yandex.aqua.beans.Launch;
import ru.yandex.aqua.beans.LaunchPack;
import ru.yandex.misc.test.Assert;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 13.06.17
 */
public class AquaLaunchFinishedCommentTest {
    @Test
    public void getStartrekComment() throws Exception {
        Launch launch = createLaunch();
        AquaJob.AquaLaunchFinishedComment sut = new AquaJob.AquaLaunchFinishedComment(launch);

        String message = sut.getStartrekComment();

        System.out.println(message);

        Assert.assertContains(message, launch.getLaunchUrl());
        Assert.assertContains(message, launch.getReportRequestUrl());
    }

    static Launch createLaunch() {
        Launch launch = new Launch();
        LaunchPack pack = new LaunchPack();
        pack.setName("Тестики");
        launch.setPack(pack);
        launch.setLaunchUrl("http://aqua/launch");
        launch.setReportRequestUrl("http://aqua/report");
        return launch;
    }

}
