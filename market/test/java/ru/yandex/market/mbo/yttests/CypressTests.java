package ru.yandex.market.mbo.yttests;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeStringNode;
import ru.yandex.market.mbo.yt.TestCypress;
import ru.yandex.market.mbo.yt.TestYt;

/**
 * @author york
 */
public class CypressTests extends BaseTests {
    @Test
    public void testCypress() {
        TestCypress testCypress = new TestYt().cypress();

        testCypress.create(YPath.simple("//home"), CypressNodeType.MAP);
        YPath baseDir = YPath.simple("//home/york");
        testCypress.create(baseDir, CypressNodeType.MAP);
        testCypress.create(baseDir.child("qq"), CypressNodeType.MAP,
            Cf.wrap(
                ImmutableMap.<String, YTreeNode>builder().put("ooo", YTree.booleanNode(false))
                    .put("aaa", YTree.stringNode("bbbb")).build()
            ));
        YTreeNode node = testCypress.get(Optional.of(GUID.create()), false, baseDir.child("qq"),
            Cf.wrap(new HashSet<>(Arrays.asList("aaa"))));

        testCypress.create(baseDir.child("rrr"), CypressNodeType.MAP);
        Exception ex = null;
        try {
            testCypress.create(baseDir.child("rrr"), CypressNodeType.MAP);
        } catch (Exception e) {
            ex = e;
        }
        Assert.assertNotNull(ex);
        testCypress.create(baseDir.child("tbl"), CypressNodeType.MAP);
        List<YTreeStringNode> children = testCypress.list(baseDir);
        Assert.assertEquals(3, children.size());
        testCypress.remove(baseDir.child("rrr"));
        children = testCypress.list(baseDir);
        Assert.assertEquals(2, children.size());
        testCypress.remove(baseDir);
        children = testCypress.list(YPath.simple("//home"));
        Assert.assertEquals(0, children.size());
    }

    @Test
    public void testCorrectPathCreation() {
        TestCypress testCypress = new TestYt().cypress();
        testCypress.create(YPath.simple("//home"), CypressNodeType.MAP);
        testCypress.create(YPath.simple("//home_2"), CypressNodeType.MAP);

        testCypress.create(YPath.simple("//home/mbo"), CypressNodeType.MAP);
        testCypress.create(YPath.simple("//home/mbo_table"), CypressNodeType.TABLE);

        testCypress.create(YPath.simple("//home/mbo/table"), CypressNodeType.TABLE);

        Assertions.assertThatThrownBy(() ->
            testCypress.create(YPath.simple("//home/mbo_table/table"), CypressNodeType.MAP))
            .hasMessageContaining("node '//home/mbo_table' is not map");
    }
}
