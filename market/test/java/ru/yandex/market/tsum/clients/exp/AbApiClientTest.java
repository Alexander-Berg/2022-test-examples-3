package ru.yandex.market.tsum.clients.exp;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 30/11/2016
 */
@Ignore
public class AbApiClientTest {

    private AbApiClient client;

    @Before
    public void setUp() throws Exception {
        client = new AbApiClient("https://ab.yandex-team.ru/api", "");
    }

    @Test
    public void test() throws Exception {
        List<ExperimentActivity> activityList = client.getActivity(new Date(), new Date(), 47, Collections.emptySet());
        List<Experiment> experiments = client.getExperiments(Arrays.asList(activityList.get(0).getTestId(),
            activityList.get(1).getTestId()));
        List<Experiment> otherExperiments = client.getProductionExperiments().get();
        List<ExperimentActivity> activities = client.getActivity(35259).get();
        List<ExperimentActivity> activities2 = client.getActivity(98572, 13).get();
        Experiment experiment = client.getExperiment(34823).get();
        List<ExperimentTask> tasks = client.getWeekTasksByAspect("market_wiz").get();
        ExperimentTask task = tasks.get(0);
    }
}
