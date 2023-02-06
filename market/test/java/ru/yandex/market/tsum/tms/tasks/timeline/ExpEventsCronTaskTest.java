package ru.yandex.market.tsum.tms.tasks.timeline;

import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.yandex.market.tsum.clients.exp.ExperimentActivity;
import ru.yandex.market.tsum.event.TsumEventApiGrpc;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 09.08.17
 */
public class ExpEventsCronTaskTest {
    @Mock
    private TsumEventApiGrpc.TsumEventApiBlockingStub tsumEventApiClient;

    @InjectMocks
    private ExpEventsCronTask expEventsCronTask;

    private static final Gson GSON = new Gson();
    private static Date date = Date.from(Instant.parse("2018-10-17T10:08:02Z"));
    private static int configId = 1;
    private static int testId = 1;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void parseActivityCriteriaList() {
        assertCriteriaListsEqual(
            Arrays.asList(
                new ExpEventsCronTask.ActivityCriteria(47),
                new ExpEventsCronTask.ActivityCriteria(54, Arrays.asList(243, 445, 446))
            ),
            ExpEventsCronTask.parseActivityCriteriaList("47;54:243,445,446")
        );

        assertCriteriaListsEqual(
            Collections.singletonList(
                new ExpEventsCronTask.ActivityCriteria(47)
            ),
            ExpEventsCronTask.parseActivityCriteriaList("47")
        );

        assertCriteriaListsEqual(
            Arrays.asList(
                new ExpEventsCronTask.ActivityCriteria(47),
                new ExpEventsCronTask.ActivityCriteria(54)
            ),
            ExpEventsCronTask.parseActivityCriteriaList("47;54")
        );
    }

    @Test
    public void testShouldAddEvent() {
        checkShouldAddEvent("off", "off", 10.000000000000002, 10.000000000000002, true);
        checkShouldAddEvent("off", "on", 10.000000000000002, 10.000000000000002, true);
        checkShouldAddEvent("off", null, 10.000000000000002, 10.000000000000002, true);

        checkShouldAddEvent("on", "off", 10.000000000000002, 10.000000000000002, true);
        checkShouldAddEvent("on", "on", 10.000000000000002, 10.000000000000002, false);
        checkShouldAddEvent("on", null, 10.000000000000002, 10.000000000000002, true);

        checkShouldAddEvent("on", "on", 10.000000000000003, 10.000000000000002, false);
    }

    @Test
    public void testIsPercentageDiffer() {
        checkIsPercentageDiffer("off", 10.000000000000002, 10.000000000000002, false);
        checkIsPercentageDiffer("off", 10.000000000000002, 10.000000000000003, false);
        checkIsPercentageDiffer("off", 10.000000000000003, 10.000000000000002, false);

        checkIsPercentageDiffer("on", 10.000000000000002, 10.000000000000002, false);
        checkIsPercentageDiffer("on", 10.000000000000002, 10.000000000000003, true);
        checkIsPercentageDiffer("on", 10.000000000000003, 10.000000000000002, true);

        checkIsPercentageDiffer("on", 10.000000000000002, null, false);
    }

    private void checkIsPercentageDiffer(String newStatus, Double newPercent, Double oldPercent, boolean expected) {
        List<ExperimentActivity> allActivities = oldPercent != null ?
            getExperimentActivities("on", oldPercent) :
            Collections.emptyList();
        ExperimentActivity experimentActivity = getExperimentActivity(newStatus, newPercent);
        Assert.assertEquals(expected, expEventsCronTask.isPercentageDiffer(experimentActivity, allActivities));
    }

    private void checkShouldAddEvent(String newEventStatus, String lastEventStatus, double newPersentage,
                                     double lastPersantage, boolean expected) {

        List<ExperimentActivity> allActivities = getExperimentActivities(lastEventStatus, lastPersantage);
        ExperimentActivity experimentActivity = getExperimentActivity(newEventStatus, newPersentage);
        Assert.assertEquals(expected, expEventsCronTask.shouldAddEvent(experimentActivity, allActivities));
    }

    private ExperimentActivity getExperimentActivity(String newEventStatus, double newPersentage) {
        ExperimentActivity experimentActivity = new ExperimentActivity();
        experimentActivity.setStatus(newEventStatus);
        experimentActivity.setConfigId(configId);
        experimentActivity.setDate(date);
        experimentActivity.setFootprints(Collections.singletonList(
            new ExperimentActivity.Footprint(
                "EXPERIMENTS-24251", 255, false,  "08711b31db07d2c1190f834a83150b28",
                new ExperimentActivity.Restrictions(newPersentage)
            )
        ));
        return experimentActivity;
    }

    private List<ExperimentActivity> getExperimentActivities(String lastEventStatus, double lastPersantage) {
        return lastEventStatus != null ?
                GSON.fromJson("[" +
                        "{\n" +
                        "        \"testid\": " + testId + ",\n" +
                        "        \"config_id\": " + configId + ",\n" +
                        "        \"config_version\": 14342,\n" +
                        "        \"exclusive\": false,\n" +
                        "        \"tag\": \"online\",\n" +
                        "        \"status\": \"" + lastEventStatus + "\",\n" +
                        "        \"time\": \"2018-10-16T10:08:02Z\",\n" +
                        "        \"footprints\": [\n" +
                        "            {\n" +
                        "                \"ticket\": \"EXPERIMENTS-24251\",\n" +
                        "                \"dim_id\": 255,\n" +
                        "                \"exclusive\": false,\n" +
                        "                \"until\": \"2018-10-18\",\n" +
                        "                \"restrictions\": {\n" +
                        "                    \"services\": \"market\",\n" +
                        "                    \"regions\": \"10000\",\n" +
                        "                    \"platforms\": \"\",\n" +
                        "                    \"devices\": \"\",\n" +
                        "                    \"browsers\": \"\",\n" +
                        "                    \"networks\": \"\",\n" +
                        "                    \"percent\": " + lastPersantage + "\n" +
                        "                },\n" +
                        "                \"path_key\": \"08711b31db07d2c1190f834a83150b28\",\n" +
                        "                \"timestamp_granularities\": [],\n" +
                        "                \"hash_type_aggregated\": 2,\n" +
                        "                \"merged_from_config_id\": 47,\n" +
                        "                \"merged_from_config_version\": 2231\n" +
                        "            }\n" +
                        "        ],\n" +
                        "        \"deploy_info\": null\n" +
                        "    },\n" +
                        "    {\n" +
                        "        \"testid\": 98572,\n" +
                        "        \"config_id\": 13,\n" +
                        "        \"config_version\": 13897,\n" +
                        "        \"exclusive\": false,\n" +
                        "        \"tag\": \"online\",\n" +
                        "        \"status\": \"on\",\n" +
                        "        \"time\": \"2018-10-05T16:18:03Z\",\n" +
                        "        \"footprints\": [\n" +
                        "            {\n" +
                        "                \"ticket\": \"EXPERIMENTS-24251\",\n" +
                        "                \"dim_id\": 255,\n" +
                        "                \"exclusive\": false,\n" +
                        "                \"until\": \"2018-10-16\",\n" +
                        "                \"restrictions\": {\n" +
                        "                    \"services\": \"market\",\n" +
                        "                    \"regions\": \"10000\",\n" +
                        "                    \"platforms\": \"\",\n" +
                        "                    \"devices\": \"\",\n" +
                        "                    \"browsers\": \"\",\n" +
                        "                    \"networks\": \"\",\n" +
                        "                    \"percent\": 10.000000000000002\n" +
                        "                },\n" +
                        "                \"path_key\": \"08711b31db07d2c1190f834a83150b28\",\n" +
                        "                \"timestamp_granularities\": [],\n" +
                        "                \"hash_type_aggregated\": 2,\n" +
                        "                \"merged_from_config_id\": 47,\n" +
                        "                \"merged_from_config_version\": 2202\n" +
                        "            }\n" +
                        "        ],\n" +
                        "        \"deploy_info\": null\n" +
                        "    },\n" +
                        "    {\n" +
                        "        \"testid\": 98572,\n" +
                        "        \"config_id\": 13,\n" +
                        "        \"config_version\": 13750,\n" +
                        "        \"exclusive\": false,\n" +
                        "        \"tag\": \"online\",\n" +
                        "        \"status\": \"on\",\n" +
                        "        \"time\": \"2018-10-02T15:09:05Z\",\n" +
                        "        \"footprints\": [\n" +
                        "            {\n" +
                        "                \"ticket\": \"EXPERIMENTS-24251\",\n" +
                        "                \"dim_id\": 255,\n" +
                        "                \"exclusive\": false,\n" +
                        "                \"until\": \"2018-10-06\",\n" +
                        "                \"restrictions\": {\n" +
                        "                    \"services\": \"market\",\n" +
                        "                    \"regions\": \"10000\",\n" +
                        "                    \"platforms\": \"\",\n" +
                        "                    \"devices\": \"\",\n" +
                        "                    \"browsers\": \"\",\n" +
                        "                    \"networks\": \"\",\n" +
                        "                    \"percent\": 10.000000000000002\n" +
                        "                },\n" +
                        "                \"path_key\": \"08711b31db07d2c1190f834a83150b28\",\n" +
                        "                \"timestamp_granularities\": [],\n" +
                        "                \"hash_type_aggregated\": 2,\n" +
                        "                \"merged_from_config_id\": 47,\n" +
                        "                \"merged_from_config_version\": 2192\n" +
                        "            }\n" +
                        "        ],\n" +
                        "        \"deploy_info\": null\n" +
                        "    }\n" +
                        "]", new TypeToken<List<ExperimentActivity>>(){}.getType()) :
                    Collections.emptyList();
    }

    private void assertCriteriaListsEqual(List<ExpEventsCronTask.ActivityCriteria> expected,
                                          List<ExpEventsCronTask.ActivityCriteria> actual) {
        String message = String.format(
            "Expected: %s, actual: %s", Joiner.on(",").join(expected), Joiner.on(",").join(actual)
        );

        Assert.assertEquals(message, expected.size(), actual.size());

        for (int i = 0; i < expected.size(); ++i) {
            Assert.assertEquals(message, expected.get(i).configId, actual.get(i).configId);
            Assert.assertArrayEquals(message, expected.get(i).dimensions.toArray(), actual.get(i).dimensions.toArray());
        }
    }
}
