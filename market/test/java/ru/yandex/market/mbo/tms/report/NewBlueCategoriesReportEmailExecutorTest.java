package ru.yandex.market.mbo.tms.report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.thymeleaf.spring3.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import ru.yandex.bolts.collection.IterableF;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.tables.YTableEntryType;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.mbo.db.TovarTreeServiceMock;
import ru.yandex.market.mbo.db.navigation.NavigationTreeService;
import ru.yandex.market.mbo.gwt.models.navigation.NavigationNode;
import ru.yandex.market.mbo.gwt.models.navigation.NavigationTree;
import ru.yandex.market.mbo.gwt.models.navigation.SimpleNavigationNode;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.models.visual.TreeNode;
import ru.yandex.market.mbo.smtp.SmtpSender;
import ru.yandex.market.mbo.tms.health.YtHealthMapReduceService;
import ru.yandex.market.mbo.yt.TestCypress;
import ru.yandex.market.mbo.yt.TestYt;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * @author york
 * @since 23.08.2018
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("checkstyle:magicnumber")
public class NewBlueCategoriesReportEmailExecutorTest {
    private static final String ROOT = "//home/york/root";
    private static final String SKU_PATH = "//sku/mapping";
    private static final String STOCK = "//stock";
    private static final String BASE_MBO_URL = "http://mbo.url";
    private static final String MAP_REDUCE_POOL = "mapReducePool";

    private NewBlueCategoriesReportEmailExecutor emailExecutor;

    @Mock
    private SmtpSender smtpSender;
    @Mock
    private NavigationTreeService navigationTreeService;
    @Mock
    private JdbcTemplate yqlJdbcTemplate;
    @Mock
    private Yt yt;
    @Mock
    private YtHealthMapReduceService ytHealthMapReduceService;

    private TestCypress stub;
    private Map<String, List<Long>> ytSessions;
    private Map<Long, Integer> currentSession;
    private List<YtHealthMapReduceService.YPaths> paths;

    @Before
    public void init() {
        ytSessions = new HashMap<>();
        currentSession = new HashMap<>();
        paths = new ArrayList<>();

        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("reports/pages/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML");

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        doAnswer(invocation -> {
            RowCallbackHandler rch = invocation.getArgument(1);
            currentSession.forEach((hid, cnt) -> {
                ResultSet rs = Mockito.mock(ResultSet.class);
                try {
                    when(rs.getLong(Mockito.eq("hid"))).thenReturn(hid);
                    when(rs.getInt(Mockito.eq("cnt"))).thenReturn(cnt);
                    rch.processRow(rs);
                } catch (SQLException e) {
                }
            });
            return null;
        }).when(yqlJdbcTemplate).query(Mockito.anyString(), any(RowCallbackHandler.class));

        when(navigationTreeService.getNavigationTrees()).thenReturn(Collections.singletonList(createNT()));

        stub = new TestYt().cypress();
        stub.create(YPath.simple(STOCK), CypressNodeType.MAP);
        stub.create(YPath.simple(STOCK).child("recent"), CypressNodeType.MAP);
        when(yt.cypress()).thenReturn(stub);

        YtTables tables = Mockito.mock(YtTables.class);
        doAnswer(invocationOnMock -> {
            YPath path = invocationOnMock.getArgument(0);
            IterableF<YTreeMapNode> nodes = invocationOnMock.getArgument(2);
            ytSessions.put(path.toString(), nodes.iterator().stream()
                .map(t -> t.getLong(NewBlueCategoriesReportEmailExecutor.HID)).collect(Collectors.toList()));
            return null;
        }).when(tables).write(any(YPath.class), any(YTableEntryType.class), any(IterableF.class));

        doAnswer(invocationOnMock -> {
            YPath path = invocationOnMock.getArgument(0);
            Consumer<YTreeMapNode> consumer = invocationOnMock.getArgument(2);
            ytSessions.getOrDefault(path.toString(), Collections.emptyList()).forEach(hid -> {
                consumer.accept(YTree.mapBuilder().key(NewBlueCategoriesReportEmailExecutor.HID).value(hid).buildMap());
            });
            return null;
        }).when(tables).read(any(YPath.class), any(YTableEntryType.class), any(Consumer.class));
        when(yt.tables()).thenReturn(tables);

        when(ytHealthMapReduceService.listSessionPaths()).then(invocation -> paths.stream());

        TovarTreeServiceMock tovarTreeService = new TovarTreeServiceMock();
        initTovarTree(tovarTreeService);

        emailExecutor = new NewBlueCategoriesReportEmailExecutor(
            yt,
            yqlJdbcTemplate,
            ytHealthMapReduceService,
            STOCK,
            SKU_PATH,
            BASE_MBO_URL,
            ROOT,
            MAP_REDUCE_POOL,
            templateEngine,
            navigationTreeService,
            tovarTreeService,
            smtpSender,
            Collections.emptyList()
        );
    }

    @Test
    public void testNoSessions() throws Exception {
        emailExecutor.doRealJob(null);
        Mockito.verifyZeroInteractions(yqlJdbcTemplate);
        Mockito.verifyZeroInteractions(yt);
        Mockito.verifyZeroInteractions(smtpSender);
    }

    @Test
    public void testSessions() throws Exception {
        String session = "20180101_1234";
        paths.add(forSession(session));
        currentSession.put(1L, 10);
        currentSession.put(2L, 1);
        currentSession.put(10L, 5);
        currentSession.put(12L, 2);

        emailExecutor.doRealJob(null);
        YPath sessionPath = YPath.simple(ROOT).child(session);
        assertTrue(stub.exists(sessionPath));
        Assert.assertEquals(1, ytSessions.size());
        assertThat(ytSessions.get(sessionPath.toString()), containsInAnyOrder(12L));
        Mockito.verifyZeroInteractions(smtpSender);

        //new session no changes
        currentSession.clear();
        currentSession.put(10L, 1);
        currentSession.put(12L, 3);
        String session2 = "20180102_1534";
        paths.add(forSession(session2));
        emailExecutor.doRealJob(null);
        sessionPath = YPath.simple(ROOT).child(session2);
        assertTrue(ytSessions.containsKey(sessionPath.toString()));
        //no new categories
        Mockito.verifyZeroInteractions(smtpSender);

        //new session with changes
        currentSession.clear();
        currentSession.put(11L, 1); // new category
        currentSession.put(13L, 3);
        String session3 = "20180103_1134";
        paths.add(forSession(session3));
        emailExecutor.doRealJob(null);
        sessionPath = YPath.simple(ROOT).child(session3);
        assertTrue(ytSessions.containsKey(sessionPath.toString()));

        Mockito.verify(smtpSender).send(Mockito.argThat(message -> {
            List<String> attachments = message.getAttachements().stream().map(p -> {
                try {
                    return new String(Files.readAllBytes(p.second), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toList());

            Assertions.assertThat(attachments).hasSize(1);

            String fileContent = attachments.get(0);
            Assertions.assertThat(fileContent).contains("https://beru.ru/catalog/100011/");
            Assertions.assertThat(fileContent).contains("http://mbo.url/gwt/#tovarTree/hyperId=11");
            Assertions.assertThat(fileContent).contains("root/child2");
            return true;
        }));
    }

    @Test
    public void testSessionForSameDay() throws Exception {
        String session = "20180101_1234";
        paths.add(forSession(session));
        currentSession.put(12L, 2);

        emailExecutor.doRealJob(null);
        YPath sessionPath = YPath.simple(ROOT).child(session);
        assertTrue(stub.exists(sessionPath));
        Assert.assertEquals(1, ytSessions.size());
        assertThat(ytSessions.get(sessionPath.toString()), containsInAnyOrder(12L));
        Mockito.verifyZeroInteractions(smtpSender);

        String session2 = "20180101_1534";
        paths.add(forSession(session2));
        currentSession.put(11L, 2);
        emailExecutor.doRealJob(null);
        assertThat(ytSessions.keySet(), containsInAnyOrder(sessionPath.toString()));
    }

    @Test
    public void testEmptySession() throws Exception {
        String session = "20180101_1234";
        paths.add(forSession(session));

        emailExecutor.doRealJob(null);
        YPath sessionPath = YPath.simple(ROOT).child(session);
        assertTrue(stub.exists(sessionPath));
        assertThat(ytSessions.get(sessionPath.toString()), empty());
    }

    private YtHealthMapReduceService.YPaths forSession(String sessionId) {
        YPath base = YPath.simple("//b/" + sessionId);
        return new YtHealthMapReduceService.YPaths(sessionId,
            base,
            base.child("models"),
            base.child("skus"),
            base.child("partner_skus"));
    }

    /**
     * Tree with hids 10, 11 (blue), 12 (blue).
     *
     * @return
     */
    private NavigationTree createNT() {
        NavigationTree tree = new NavigationTree();
        tree.setCode(NewBlueCategoriesReportEmailExecutor.BLUE_TREE_CODE);
        tree.setRoot(createNode(null, true, null));

        createNode(10L, false, tree.getRoot());
        createNode(11L, true, tree.getRoot());
        createNode(12L, true, tree.getRoot());

        return tree;
    }

    private void initTovarTree(TovarTreeServiceMock tovarTreeService) {
        tovarTreeService.addCategory(new TovarCategory("root", 1L, 0L));
        tovarTreeService.addCategory(new TovarCategory("child", 10L, 1L));
        tovarTreeService.addCategory(new TovarCategory("child2", 11L, 1L));
        tovarTreeService.addCategory(new TovarCategory("child2", 12L, 10L));
        tovarTreeService.addCategory(new TovarCategory("child2", 13L, 10L));
    }

    private TreeNode<NavigationNode> createNode(Long hid, boolean publish, TreeNode<NavigationNode> parent) {
        NavigationNode node = new SimpleNavigationNode();
        if (hid != null) {
            node.setHid(hid);
            node.setId(hid + 100000);
        }
        node.setPublished(publish);
        node.setIsSkipped(false);
        TreeNode<NavigationNode> result = new TreeNode<>(node);
        if (parent != null) {
            parent.addChild(result);
        }
        return result;
    }
}
