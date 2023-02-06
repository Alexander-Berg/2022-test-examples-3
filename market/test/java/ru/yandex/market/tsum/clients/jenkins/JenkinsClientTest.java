package ru.yandex.market.tsum.clients.jenkins;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.tsum.clients.pollers.Poller;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 06/06/2017
 */
public class JenkinsClientTest {
    @Test
    @Ignore
    public void test() throws Exception {
        JenkinsClient jenkinsClient = new JenkinsClient(
            "https://jenkins-load.yandex-team.ru", "robot-mrk-infra-tst", "83d92c607f4a8b6dc73a0330f41f9dd7"
        );

        Map<String, String> params = new HashMap<>();
        params.put("VHOST", "desktop-stress.market.fslb01ht.yandex.ru");
        params.put("TASK", "MARKETINFRA-1303-x5");
        params.put("RPS_SCHEDULE", "const(30,1m)");
        params.put("ssl", "");
        params.put("nanny_service", "testing_market_front_desktop_load_sas");
        params.put("LAUNCH_ID", "abc");

        String jobName = "MARKETVERSTKA-frontend-manual-runtime";

        JenkinsQueueReference queueReference = jenkinsClient.startBuild(jobName, params);

        JenkinsJobId jobId = Poller.poll(
                () -> jenkinsClient.getJobId(queueReference))
            .canStopWhen(Optional::isPresent)
            .run()
            .get();

        JenkinsJobId foundJobId1 = jenkinsClient.findBuild(jobName, Collections.singletonMap("LAUNCH_ID", "abc")).get();

        Assert.assertEquals(jobId, foundJobId1);

        JenkinsJobStatus status = jenkinsClient.getJobStatus(jobId);

    }


}
