package ru.yandex.market.tsum.pipe.engine.runtime.helpers;

import org.bson.types.ObjectId;
import ru.yandex.market.tsum.pipe.engine.runtime.LaunchUrlProvider;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 12.04.18
 */
public class TestLaunchUrlProvider implements LaunchUrlProvider {
    @Override
    public String getPipeLaunchUrl(ObjectId pipeLaunchId) {
        return "https://tsum.yandex-team.ru/pipe/launch/" + pipeLaunchId.toString();
    }

    @Override
    public String getJobLaunchDetailsUrl(ObjectId pipeLaunchId, String jobId, int jobLaunchNumber) {
        return "https://tsum.yandex-team.ru/pipe/launch/" +
            pipeLaunchId.toString() + "/job/" + jobId + "/" + jobLaunchNumber;
    }
}
