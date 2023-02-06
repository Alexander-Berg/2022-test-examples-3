package ru.yandex.direct.ytwrapper.model;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.direct.ytwrapper.client.YtClusterConfig;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class YtOperatorTest {
    private static final String TEST_ATTR_NAME = "test_attr";

    private Yt yt;
    private YtOperator ytOperator;
    private YtTable table = new YtTable("//tmp/test");

    @Before
    public void before() {
        yt = mock(Yt.class, RETURNS_DEEP_STUBS);
        ytOperator = new YtOperator(
                mock(YtClusterConfig.class), yt, mock(YtProvider.class), YtCluster.MARKOV, YtSQLSyntaxVersion.SQLv0
        );
    }

    @Test
    public void testReadAttribute() {
        YTreeNode node = YTree.booleanNode(false);
        node.putAttribute(TEST_ATTR_NAME, YTree.stringNode("test"));

        when(yt.cypress().get(eq(YPath.simple("//tmp/test")), eq(Cf.set(TEST_ATTR_NAME)))).thenReturn(node);
        YTreeNode result = ytOperator.readTableAttribute(table, TEST_ATTR_NAME);

        assertThat("Получили верное значение", result.stringValue(), equalTo("test"));
    }
}
