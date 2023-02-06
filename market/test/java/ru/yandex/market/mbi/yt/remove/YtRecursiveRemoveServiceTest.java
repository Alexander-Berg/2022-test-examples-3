package ru.yandex.market.mbi.yt.remove;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeStringNode;
import ru.yandex.market.core.FunctionalTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;

/**
 * Тесты для {@link YtRecursiveRemoveService}
 */
@ExtendWith(MockitoExtension.class)
class YtRecursiveRemoveServiceTest extends FunctionalTest {

    private static final String ROOT_PATH = "//home/some/path";
    private static final String PATH1 = ROOT_PATH + "/1";

    private static final MapF<String, YTreeNode> TABLE_ATTRIBUTE = Cf.map(
            "type",
            new YTreeStringNodeImpl("table", null)
    );
    private static final MapF<String, YTreeNode> PATH_ATTRIBUTE = Cf.map(
            "type",
            new YTreeStringNodeImpl("map_node", null)
    );
    private static final YtStorageInterval STORAGE_INTERVAL = new YtStorageInterval(6, ChronoUnit.DAYS);

    @Mock
    private Yt yt;

    @Mock
    private Cypress cypress;

    @Mock
    private YtRemoveService ytRemoveService;

    private YtRecursiveRemoveService ytRecursiveRemoveService;

    @BeforeEach
    void setUp() {
        setUpMockTables();
        this.ytRecursiveRemoveService = new YtRecursiveRemoveService(
                yt,
                ytRemoveService
        );
    }

    @Test
    void testRecursiveRemoveTables() {
        ytRecursiveRemoveService.recursiveRemoveExpiredTables(List.of(ROOT_PATH), STORAGE_INTERVAL);
        ArgumentCaptor<String> pathsToCleanCaptor = ArgumentCaptor.forClass(String.class);

        var expectedPaths = new String[]{PATH1, ROOT_PATH};
        Mockito.verify(ytRemoveService, Mockito.times(expectedPaths.length))
                .removeExpiredTables(pathsToCleanCaptor.capture(), any(), any());

        assertThat(pathsToCleanCaptor.getAllValues(), Matchers.contains(expectedPaths));
    }

    void setUpMockTables() {
        Mockito.when(yt.cypress()).thenReturn(cypress);

        Mockito.doAnswer(invocation -> {
            YPath path = invocation.getArgument(0);
            return getPathListNode(path.toString());
        }).when(cypress).list(any(), anyCollection());

        Mockito.doAnswer(invocation -> {
            YPath path = invocation.getArgument(0);
            return getPathInfoNode(path.toString());
        }).when(cypress).get(any(), anyCollection());
    }

    private YTreeNode getPathInfoNode(String path) {
        var attr = PATH_ATTRIBUTE;
        if (path.contains("table")) {
            attr = TABLE_ATTRIBUTE;
        }
        return new YTreeStringNodeImpl(path, attr);
    }

    private List<YTreeStringNode> getPathListNode(String path) {
        switch (path) {
            case ROOT_PATH:
                return Arrays.asList(
                        new YTreeStringNodeImpl("1", PATH_ATTRIBUTE),
                        new YTreeStringNodeImpl("table", TABLE_ATTRIBUTE)
                );
            case PATH1:
                return Collections.singletonList(new YTreeStringNodeImpl("table", TABLE_ATTRIBUTE));
        }
        throw new IllegalStateException();
    }
}
