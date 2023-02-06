package ru.yandex.market.ir.tms.utils;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeStringNode;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author amaslak
 */
public class SessionUtilsTest {

    private static final YPath OFFERS_PATH = YPath.simple("//tmp/market/mbo/development/SessionUtilsTest");

    final Set<String> paths = new LinkedHashSet<>();

    private Cypress cypress;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        cypress = Mockito.mock(Cypress.class);

        Answer createAnswer = invocation -> {
            YPath path = (YPath) invocation.getArguments()[0];
            paths.add(path.toString());
            return null;
        };
        Mockito.doAnswer(createAnswer)
                .when(cypress)
                .create(Mockito.isA(YPath.class), Mockito.isA(CypressNodeType.class));

        Answer existsAnswer = invocation -> {
            String path = invocation.getArguments()[0].toString();
            return paths.stream().anyMatch(node -> node.startsWith(path));
        };
        Mockito.doAnswer(existsAnswer).when(cypress).exists(Mockito.isA(YPath.class));

        Answer listAnswer = invocation -> {
            String path = invocation.getArguments()[0].toString();
            Collection<YTreeStringNode> list = paths.stream()
                    .filter(node -> node.startsWith(path))
                    .map(node -> node.replaceFirst(Pattern.quote(path) + "/?", ""))
                    .map(node -> node.contains("/") ? node.substring(0, node.indexOf('/')) : node)
                    .filter(s -> !s.isEmpty())
                    .map(YTree::stringNode)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            return Cf.toArrayList(list);
        };
        Mockito.doAnswer(listAnswer).when(cypress).list(Mockito.isA(YPath.class));
        Mockito.doAnswer(listAnswer).when(cypress).list(Mockito.isA(YPath.class), Mockito.isA(SetF.class));

    }

    @Test
    public void testCypress() throws Exception {
        Assert.assertTrue(cypress.list(YPath.cypressRoot()).isEmpty());

        createPath(OFFERS_PATH);
        Assert.assertFalse(cypress.list(YPath.cypressRoot()).isEmpty());
        Assert.assertTrue(cypress.list(OFFERS_PATH).isEmpty());

        createPath(OFFERS_PATH.child("test"));
        Assert.assertEquals(Cf.list(YTree.stringNode("test")), cypress.list(OFFERS_PATH));

        createPath(OFFERS_PATH.child("test").child("1"));
        Assert.assertEquals(Cf.list(YTree.stringNode("test")), cypress.list(OFFERS_PATH));

        createPath(OFFERS_PATH.child("test2").child("1"));
        Assert.assertEquals(Cf.list(YTree.stringNode("test"), YTree.stringNode("test2")), cypress.list(OFFERS_PATH));

        createPath(OFFERS_PATH.child("test2").child("1"));
        Assert.assertTrue(cypress.exists(OFFERS_PATH));
        Assert.assertTrue(cypress.exists(OFFERS_PATH.child("test2")));
        Assert.assertTrue(cypress.exists(OFFERS_PATH.child("test2").child("1")));
    }

    private void createPath(YPath path) {
        cypress.create(path, CypressNodeType.MAP);
    }

    private void createTable(YPath path) {
        cypress.create(path, CypressNodeType.TABLE);
    }

    @Test
    public void testFindLastSession() throws Exception {
        SessionUtils.findLastSession(cypress, OFFERS_PATH);
    }

    @Test
    public void testMissingLastSessionEmptyPath() {
        Assert.assertNull(SessionUtils.findLastSession(cypress, OFFERS_PATH));
    }

    @Test
    public void testMissingLastSessionNoSessionCatalogs() {

        createPath(OFFERS_PATH.child("test1"));
        createPath(OFFERS_PATH.child("test2"));
        createPath(OFFERS_PATH.child("test3"));

        Assert.assertNull(SessionUtils.findLastSession(cypress, OFFERS_PATH));
    }

    @Test
    public void testMissingLastSessionEmptySessionCatalogs() {
        createPath(OFFERS_PATH.child("aaa"));
        createPath(OFFERS_PATH.child("20171004_1910"));
        createPath(OFFERS_PATH.child("20171004_1955"));
        createPath(OFFERS_PATH.child("zzz"));

        Assert.assertNull(SessionUtils.findLastSession(cypress, OFFERS_PATH));
    }

    @Test
    public void testLastSessionSingleSessionCase() throws InterruptedException {
        createPath(OFFERS_PATH.child("test1"));
        createTable(OFFERS_PATH.child("20171004_1910").child(SessionUtils.YT_GENERATION_DATA));
        createPath(OFFERS_PATH.child("20171004_1955"));

        Assert.assertEquals("20171004_1910", SessionUtils.findLastSession(cypress, OFFERS_PATH));
    }

    @Test
    public void testLastSessionTwoSessionCase() throws InterruptedException {
        createPath(OFFERS_PATH.child("test1"));
        createTable(OFFERS_PATH.child("20171003_1910").child(SessionUtils.YT_GENERATION_DATA));
        createTable(OFFERS_PATH.child("20171004_1910").child(SessionUtils.YT_GENERATION_DATA));
        createPath(OFFERS_PATH.child("20171104_1955"));

        Assert.assertEquals("20171004_1910", SessionUtils.findLastSession(cypress, OFFERS_PATH));
    }

    @Test
    public void testLastSessionThreeSessionCase() throws InterruptedException {
        createPath(OFFERS_PATH.child("test1"));
        createTable(OFFERS_PATH.child("20171003_1910").child(SessionUtils.YT_GENERATION_DATA));
        createTable(OFFERS_PATH.child("20171004_1910").child(SessionUtils.YT_GENERATION_DATA));
        createTable(OFFERS_PATH.child("20171004_2030").child(SessionUtils.YT_GENERATION_DATA));
        createPath(OFFERS_PATH.child("20171104_1955"));

        Assert.assertEquals("20171004_2030", SessionUtils.findLastSession(cypress, OFFERS_PATH));
    }

    @Test
    public void testLastSessionMultiTableCase() throws InterruptedException {
        createPath(OFFERS_PATH.child("test1"));
        createTable(OFFERS_PATH.child("20171003_1910").child(SessionUtils.YT_GENERATION_DATA));
        createTable(OFFERS_PATH.child("20171004_1910").child(SessionUtils.YT_GENERATION_DATA));
        createTable(OFFERS_PATH.child("20171004_2030").child("some_table"));
        createPath(OFFERS_PATH.child("20171104_1955"));

        Assert.assertEquals("20171004_1910", SessionUtils.findLastSession(cypress, OFFERS_PATH));
    }

    @Test
    public void testGetYtTableSessionId() {
        Assert.assertEquals("20181003_1451",
            SessionUtils.getYtTableSessionId("//home/market/development/ir/sc/20181003_1451/mbo_offers_mr"));

        Assertions.assertThatThrownBy(() -> {
            SessionUtils.getYtTableSessionId("folder/mbo_offers_mr");
        }).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @SuppressWarnings("checkstyle:LineLength")
    public void testGetYtComplexTableSessionId() {
        Assert.assertEquals("20181003_1451",
            SessionUtils.getYtComplexTableSessionId("//home/market/development/mbo/sc_generation_processed/20181003_1451/mbo_offers_mr"));
        Assert.assertEquals("20181003_1451_20181002_1050",
            SessionUtils.getYtComplexTableSessionId("//home/market/development/mbo/sc_generation_diff/20181003_1451_20181002_1050/mbo_offers_mr"));

        Assertions.assertThatThrownBy(() -> {
            SessionUtils.getYtTableSessionId("folder/mbo_offers_mr");
        }).isInstanceOf(IllegalArgumentException.class);
    }
}
