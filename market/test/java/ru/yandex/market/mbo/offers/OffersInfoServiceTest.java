package ru.yandex.market.mbo.offers;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.bolts.collection.Option;
import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.mbo.gwt.models.dashboard.YtClustersData;
import ru.yandex.market.mbo.gwt.models.dashboard.YtLogOffersData;
import ru.yandex.market.mbo.yt.TestYt;
import ru.yandex.yt.ytclient.proxy.request.CreateNode;

@RunWith(MockitoJUnitRunner.class)
public class OffersInfoServiceTest {

    public static final String OFFERS_PATH = "//home/mbo/offers";

    public static final String CURRENT_SESSION = "20220101_1520";
    public static final String NEW_SESSION = "20220202_2035";
    public static final List<String> OLD_SESSIONS = Arrays.asList("20220101_1230", "20220101_0515",
        "20220101_0214", "20211231_2020");

    private TestYt testYt;
    private TestYt testYtReplica;

    private OffersInfoService offersInfoService;

    @Before
    public void setUp() {
        testYt = new TestYt();
        testYtReplica = new TestYt();
        offersInfoService = new OffersInfoService(testYt, testYtReplica);

        ReflectionTestUtils.setField(offersInfoService, "mainCluster", "hahn");
        ReflectionTestUtils.setField(offersInfoService, "replicationCluster", "arnold");

        ReflectionTestUtils.setField(offersInfoService, "offersPath", OFFERS_PATH);
    }

    @Test
    public void testEmptyOffersDataAndBadClusters() {
        List<YtClustersData> clustersData = offersInfoService.getClustersData();
        for (YtClustersData clusterData : clustersData) {
            Assert.assertEquals(clusterData.getStatus(), YtClustersData.Status.UNAVAILABLE);
        }
        Assert.assertTrue(offersInfoService.getOffersData().isEmpty());
    }

    @Test
    public void testOkClusters() {
        prepareOffersDir();
        List<YtClustersData> clustersData = offersInfoService.getClustersData();
        for (YtClustersData clusterData : clustersData) {
            Assert.assertEquals(clusterData.getStatus(), YtClustersData.Status.OK);
        }
    }

    @Test
    public void testSessionParsingWithNewOldAndCurrent() {
        prepareOffersDir();
        prepareOldDirs();
        prepareNewDir();
        prepareCurrentDir();
        prepareSystemDir();

        List<YtLogOffersData> offersData = offersInfoService.getOffersData();
        Assert.assertEquals(OLD_SESSIONS.size() + 2, offersData.size());

        YtLogOffersData newOffersData = offersData.get(0);
        Assert.assertEquals(NEW_SESSION, newOffersData.getSessionId());
        Assert.assertEquals(YtLogOffersData.Status.NEW, newOffersData.getStatus());
        offersData.remove(0);

        YtLogOffersData currentOffersData = offersData.get(0);
        Assert.assertEquals(CURRENT_SESSION, currentOffersData.getSessionId());
        Assert.assertEquals(YtLogOffersData.Status.CURRENT, currentOffersData.getStatus());
        offersData.remove(0);

        for (int i = 0; i < OLD_SESSIONS.size(); i++) {
            YtLogOffersData oldOffersData = offersData.get(i);
            Assert.assertEquals(OLD_SESSIONS.get(i), oldOffersData.getSessionId());
            Assert.assertEquals(YtLogOffersData.Status.OLD, oldOffersData.getStatus());
        }
    }

    @Test
    public void testSessionParsingWithOldAndCurrent() {
        prepareOffersDir();
        prepareOldDirs();
        prepareCurrentDir();
        prepareSystemDir();

        List<YtLogOffersData> offersData = offersInfoService.getOffersData();
        Assert.assertEquals(OLD_SESSIONS.size() + 1, offersData.size());

        YtLogOffersData currentOffersData = offersData.get(0);
        Assert.assertEquals(CURRENT_SESSION, currentOffersData.getSessionId());
        Assert.assertEquals(YtLogOffersData.Status.CURRENT, currentOffersData.getStatus());
        offersData.remove(0);

        for (int i = 0; i < OLD_SESSIONS.size(); i++) {
            YtLogOffersData oldOffersData = offersData.get(i);
            Assert.assertEquals(OLD_SESSIONS.get(i), oldOffersData.getSessionId());
            Assert.assertEquals(YtLogOffersData.Status.OLD, oldOffersData.getStatus());
        }
    }

    private void prepareOffersDir() {
        YPath offersPath = YPath.simple(OFFERS_PATH);

        CreateNode req = new CreateNode(offersPath, CypressNodeType.MAP);
        req.setRecursive(true);
        testYt.cypress().create(req);
        testYtReplica.cypress().create(req);
    }

    private void prepareCurrentDir() {
        YPath sessionPath = YPath.simple(OFFERS_PATH).child(CURRENT_SESSION);
        CreateNode req = new CreateNode(sessionPath, CypressNodeType.MAP);
        req.setRecursive(true);
        testYt.cypress().create(req);
        testYt.cypress().set(Option.empty(), false, sessionPath.attribute("session"), true);

        YPath link = YPath.simple(OFFERS_PATH).child("recent");
        GUID guid = testYt.cypress().link(sessionPath, link);
        testYt.transactions().commit(guid);
    }

    private void prepareNewDir() {
        YPath sessionPath = YPath.simple(OFFERS_PATH).child(NEW_SESSION);

        CreateNode req = new CreateNode(sessionPath, CypressNodeType.MAP);
        req.setRecursive(true);
        testYt.cypress().create(req);
    }

    private void prepareSystemDir() {
        YPath tempPath = YPath.simple(OFFERS_PATH).child("temp");

        CreateNode req = new CreateNode(tempPath, CypressNodeType.MAP);
        req.setRecursive(true);
        testYt.cypress().create(req);
    }

    private void prepareOldDirs() {
        OLD_SESSIONS.forEach(this::prepareOldDirs);
    }

    private void prepareOldDirs(String oldSession) {
        YPath sessionPath = YPath.simple(OFFERS_PATH).child(oldSession);

        CreateNode req = new CreateNode(sessionPath, CypressNodeType.MAP);
        req.setRecursive(true);
        testYt.cypress().create(req);
        testYt.cypress().set(Option.empty(), false, sessionPath.attribute("session"), true);
    }
}
