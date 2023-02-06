package ru.yandex.market.deepmind.tms.utils;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeStringNode;
import ru.yandex.market.mboc.common.infrastructure.util.UnstableInit;

import static org.mockito.Mockito.times;

@SuppressWarnings("checkstyle:MagicNumber")
public class RotateYtTablesTest {

    private UnstableInit<Yt> yt;
    private RotateYtTables rotateYtTables;
    private List<YTreeStringNode> folders = new ArrayList<>();
    private YPath ytExportFolder;

    @Before
    public void setUp() {
        yt = UnstableInit.simple(createYtMock());
        ytExportFolder = YPath.simple("//tmp/market/mboc/availabilities");
        rotateYtTables = new RotateYtTables(yt, ytExportFolder, 5);
    }

    @Test
    public void testEmpty() {
        rotateYtTables.run();

        List<YPath> yPaths = captureYtRemoveRequest(yt, 0);
        Assertions.assertThat(yPaths).isEmpty();
    }

    @Test
    public void test1() {
        folders.add(YTree.stringNode("recent"));
        folders.add(YTree.stringNode("20200101_0101"));

        rotateYtTables.run();

        List<YPath> yPaths = captureYtRemoveRequest(yt, 0);
        Assertions.assertThat(yPaths).isEmpty();
    }

    @Test
    public void testSkipBrokenFolder() {
        folders.add(YTree.stringNode("recent"));
        folders.add(YTree.stringNode("20200101_0101"));
        folders.add(YTree.stringNode("foo-bar"));

        rotateYtTables.run();

        List<YPath> yPaths = captureYtRemoveRequest(yt, 0);
        Assertions.assertThat(yPaths).isEmpty();
    }

    @Test
    public void test4() {
        folders.add(YTree.stringNode("recent"));
        folders.add(YTree.stringNode("20200101_0101"));
        folders.add(YTree.stringNode("20200101_0102"));
        folders.add(YTree.stringNode("20200102_0101"));
        folders.add(YTree.stringNode("20200202_0101"));

        rotateYtTables.run();

        List<YPath> yPaths = captureYtRemoveRequest(yt, 0);
        Assertions.assertThat(yPaths).isEmpty();
    }

    @Test
    public void test5() {
        folders.add(YTree.stringNode("recent"));
        folders.add(YTree.stringNode("20200101_0101"));
        folders.add(YTree.stringNode("20200101_0102"));
        folders.add(YTree.stringNode("20200102_0101"));
        folders.add(YTree.stringNode("20200202_0101"));
        folders.add(YTree.stringNode("20190202_0101"));

        rotateYtTables.run();

        List<YPath> yPaths = captureYtRemoveRequest(yt, 0);
        Assertions.assertThat(yPaths).isEmpty();
    }

    @Test
    public void test6() {
        folders.add(YTree.stringNode("recent"));
        folders.add(YTree.stringNode("20200101_0101"));
        folders.add(YTree.stringNode("20200101_0102"));
        folders.add(YTree.stringNode("20101212_1212"));
        folders.add(YTree.stringNode("20200202_0101"));
        folders.add(YTree.stringNode("20190202_0101"));
        folders.add(YTree.stringNode("20180202_0101"));

        rotateYtTables.run();

        List<YPath> yPaths = captureYtRemoveRequest(yt, 1);
        Assertions.assertThat(yPaths)
            .containsExactlyInAnyOrder(ytExportFolder.child("20101212_1212"));
    }

    @Test
    public void test9() {
        folders.add(YTree.stringNode("recent"));
        folders.add(YTree.stringNode("20200101_0101"));
        folders.add(YTree.stringNode("20200101_0102"));
        folders.add(YTree.stringNode("20101212_1212"));
        folders.add(YTree.stringNode("20200202_0101"));
        folders.add(YTree.stringNode("20190202_0101"));
        folders.add(YTree.stringNode("20111212_1212"));
        folders.add(YTree.stringNode("20121212_1212"));
        folders.add(YTree.stringNode("20131212_1212"));
        folders.add(YTree.stringNode("20141212_1212"));

        rotateYtTables.run();

        List<YPath> yPaths = captureYtRemoveRequest(yt, 4);
        Assertions.assertThat(yPaths)
            .containsExactlyInAnyOrder(
                ytExportFolder.child("20101212_1212"),
                ytExportFolder.child("20111212_1212"),
                ytExportFolder.child("20121212_1212"),
                ytExportFolder.child("20131212_1212")
            );

    }

    private Yt createYtMock() {
        Yt ytMock = Mockito.mock(Yt.class);
        Cypress cypress = Mockito.mock(Cypress.class);

        Mockito.when(cypress.list(Mockito.any(YPath.class))).thenAnswer(__ -> folders);

        Mockito.when(ytMock.cypress()).thenReturn(cypress);
        return ytMock;
    }

    private static List<YPath> captureYtRemoveRequest(UnstableInit<Yt> yt, int number) {
        return captureYtRemoveRequest(yt.get().cypress(), number);
    }

    private static List<YPath> captureYtRemoveRequest(Cypress cypress, int number) {
        ArgumentCaptor<YPath> requestCaptor = ArgumentCaptor.forClass(YPath.class);
        Mockito.verify(cypress, times(number)).remove(requestCaptor.capture());
        return requestCaptor.getAllValues();
    }
}
