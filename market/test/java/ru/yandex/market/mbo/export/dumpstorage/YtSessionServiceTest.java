package ru.yandex.market.mbo.export.dumpstorage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeMapNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeStringNode;
import ru.yandex.market.mbo.gwt.models.MboDumpSessionStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 17.12.2018
 */
@RunWith(MockitoJUnitRunner.class)
public class YtSessionServiceTest {

    private static final String ROOT_PATH = "//mbo/root";
    private static final long SEED = 97556260;
    private static final String SUCCESSFUL_REPLICATED_ATTR_NAME = "successful_replicated";
    private YtSessionService ytSessionService;
    private static final long SESSIONS_TO_LEAVE = 10L;

    private Random random;
    @Mock
    private Yt yt;
    @Mock
    private Cypress cypress;

    @Before
    public void before() {
        ytSessionService = new YtSessionService();
        ytSessionService.setYt(yt);
        ytSessionService.setYtRootPath(ROOT_PATH);

        random = new Random(SEED);
        when(yt.cypress()).thenReturn(cypress);
        when(cypress.exists(any(YPath.class))).thenReturn(true);
    }

    @Test
    public void dontRotateRecent() {
        List<String> sessionNames = Arrays.asList(
            "20181210_0759",
            "20181211_1907",
            "20181212_1806",
            "20181212_1457",
            "20181211_1257",
            "20181210_1901",
            "20181210_2220",
            "20181212_1113",
            "20181212_2327",
            "20181212_1926",
            "20181211_0439"
        );

        ListF<YTreeStringNode> ytSessions = Stream.concat(sessionNames.stream(), Stream.of("recent"))
            .map(YTree::stringNode)
            .collect(Collectors.toCollection(Cf::arrayList));

        when(cypress.list(YPath.simple(ROOT_PATH))).thenReturn(ytSessions);

        Map<Boolean, List<String>> sessionByAlive = sessionNames.stream()
            .collect(Collectors.groupingBy(sessionId -> random.nextBoolean()));

        List<SessionInfo> sessionsAtZk = sessionByAlive.get(true)
            .stream().map(YtSessionServiceTest::session).collect(Collectors.toList());

        assertThat(sessionByAlive.get(true)).isNotEmpty();
        assertThat(sessionByAlive.get(false)).isNotEmpty();


        ytSessionService.rotateYTSession("stuff", sessionsAtZk);


        ArgumentCaptor<YPath> deleteArguments = ArgumentCaptor.forClass(YPath.class);
        verify(cypress, times(sessionByAlive.get(false).size())).remove(deleteArguments.capture());

        assertThat(deleteArguments.getAllValues())
            .extracting(YPath::name)
            .containsExactlyElementsOf(sessionByAlive.get(false));
    }

    @Test
    public void testMarkAsSuccessfulReplicated() {
        String sessionName = "20191210_0759";

        ytSessionService.markSessionAsSuccessfulReplicated(sessionName);

        ArgumentCaptor<YPath> addAttribute = ArgumentCaptor.forClass(YPath.class);
        verify(cypress).set(addAttribute.capture(), eq(true));

        assertThat(addAttribute.getValue())
            .isEqualTo(YPath.simple(ROOT_PATH).child(sessionName).attribute(SUCCESSFUL_REPLICATED_ATTR_NAME));
    }

    @Test(expected = RuntimeException.class)
    public void testValidateNotReplicatedSession() {
        String sessionName = "20181210_0759";
        YTreeStringNode ytSession = YTree.stringNode(sessionName);

        when(cypress.get(any(YPath.class), eq(Cf.set(SUCCESSFUL_REPLICATED_ATTR_NAME)))).thenReturn(ytSession);

        ytSessionService.validateReplicatedSession("stuff", sessionName);
    }

    @Test
    @SuppressWarnings("MagicNumber")
    public void testGetLastSuccessYtSession() {
        List<Pair<String, LocalDateTime>> sessionNames = Arrays.asList(
            Pair.of("20210929_2002", LocalDateTime.of(2021, 9, 29, 20, 2)),
            Pair.of("20211003_0657", LocalDateTime.of(2021, 10, 3, 6, 57)),
            Pair.of("20211004_1852", LocalDateTime.of(2021, 10, 4, 18, 52)),
            Pair.of("20211006_1857", LocalDateTime.of(2021, 10, 6, 18, 57)),
            Pair.of("20211007_0257", LocalDateTime.of(2021, 10, 7, 2, 57)),
            Pair.of("20211007_0657", LocalDateTime.of(2021, 10, 7, 6, 57)),
            Pair.of("20211007_1057", LocalDateTime.of(2021, 10, 7, 10, 57)),
            Pair.of("20211007_1457", LocalDateTime.of(2021, 10, 7, 14, 57)),
            Pair.of("20211007_1854", LocalDateTime.of(2021, 10, 7, 18, 54)),
            Pair.of("20211007_2257", LocalDateTime.of(2021, 10, 7, 22, 57)),
            Pair.of("20211008_0257", LocalDateTime.of(2021, 10, 8, 2, 57)),
            Pair.of("20211008_0657", LocalDateTime.of(2021, 10, 8, 6, 57)),
            Pair.of("20211008_1057", LocalDateTime.of(2021, 10, 8, 10, 57)),
            Pair.of("20211008_1333", LocalDateTime.of(2021, 10, 8, 13, 33)),
            Pair.of("recent", LocalDateTime.of(2021, 10, 8, 13, 33))
        );

        Map<String, YTreeNode> ytSessions = sessionNames.stream()
            .map(Pair::getFirst)
            .collect(Collectors.toMap(k -> k, YTree::stringNode));

        Map<String, YTreeNode> attrs = sessionNames.stream()
            .collect(Collectors.toMap(Pair::getFirst, v -> {
                    YTreeStringNode node = YTree.stringNode(v.getFirst());
                    node.putAttribute("creation_time",
                        YTree.stringNode(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(v.getSecond()) + ".00Z"));
                    node.putAttribute("status", YTree.stringNode("ok"));
                    return node;
                }
            ));

        YTreeMapNode yTreeMapNode = new YTreeMapNodeImpl(attrs, Collections.emptyMap());

        when(cypress.get(eq(YPath.simple(ROOT_PATH)), anyCollection())).thenReturn(yTreeMapNode);

        List<String> sessions = ytSessionService.getLastSuccessYtSessions(SESSIONS_TO_LEAVE);

        Assert.assertEquals(Arrays.asList(
            "20211008_1333",
            "20211008_1057",
            "20211008_0657",
            "20211008_0257",
            "20211007_2257",
            "20211007_1854",
            "20211007_1457",
            "20211007_1057",
            "20211007_0657",
            "20211007_0257"
            ),
            sessions);
    }

    private static SessionInfo session(String sessionId) {
        return new SessionInfo(sessionId, MboDumpSessionStatus.OK);
    }

}
