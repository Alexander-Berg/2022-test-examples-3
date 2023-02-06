package ru.yandex.chemodan.app.orchestrator;


import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.orchestrator.dao.Container;
import ru.yandex.chemodan.app.orchestrator.dao.ContainerDbState;
import ru.yandex.chemodan.app.orchestrator.dao.ContainersDao;
import ru.yandex.chemodan.app.orchestrator.dao.SessionsDao;
import ru.yandex.chemodan.app.orchestrator.manager.OrchestratorControl;
import ru.yandex.chemodan.app.orchestrator.manager.StateManager;
import ru.yandex.chemodan.app.orchestrator.unistat.StateMetrics;
import ru.yandex.misc.ip.HostPort;
import ru.yandex.misc.test.Assert;

/**
 * @author shirankov
 */
public class MonicaMetricsTest extends AbstractOrchestratorCoreTest {
    @Autowired
    private SessionsDao sessionsDao;
    @Autowired
    private ContainersDao containersDao;
    @Autowired
    private OrchestratorControl orchestratorControl;

    @Test
    @SuppressWarnings("UnnecessaryLocalVariable")
    public void testSessionsByGroupMetrics() {
        Instant zero = new Instant(0);
        StateManager stateManager = new StateManager(sessionsDao, containersDao, orchestratorControl);

        final int groupsNum = 20;
        for (int i = 1; i <= groupsNum; i++) {
            int sessionsCount = i;
            Container container = new Container(Integer.toString(i), new HostPort("localhost", 80),
                    "nowhere", Option.of("group_id" + i), ContainerDbState.LOST, zero, zero, sessionsCount);
            containersDao.create(container);
        }

        MapF<String, Integer> sentData = ru.yandex.bolts.collection.Cf.hashMap();
        StateMetrics.sessionsByGroup.setRegistrar(((metricName, integerMetric) ->
                sentData.put(metricName.toString(), Integer.parseInt(integerMetric.getStatusText()))));

        stateManager.update();

        int topNGroups = 10; // StateManager.TOP_N_GROUPS;
        int topNum = 1;
        for (int i = groupsNum; i > groupsNum - topNGroups; i--) {
            int sessionsCount = i;
            String groupIdKey = String.format("group_id%d_max", i);
            String topNKey = String.format("top-%d_max", topNum++);

            Assert.assertContains(sentData.keySet(), groupIdKey);
            Assert.assertContains(sentData.keySet(), topNKey);

            Assert.assertEquals(sentData.getTs(groupIdKey).intValue(), sessionsCount);
            Assert.assertEquals(sentData.getTs(topNKey).intValue(), sessionsCount);
        }
    }
}
