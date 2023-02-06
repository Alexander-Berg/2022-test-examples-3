package ru.yandex.market.crm.external.contentapi;

import java.util.List;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.domain.categories.NavTreeNode;

import static org.hamcrest.Matchers.hasSize;

/**
 * @author apershukov
 */
public class NavTreeParserTest {

    private static void assertNode(long nid, int hid, String name, NavTreeNode node) {
        Assertions.assertEquals(nid, node.getNid());
        Assertions.assertEquals(name, node.getName());
        Assertions.assertEquals(hid, (int) node.getHid());
    }

    @Test
    public void testParseTree() {
        NavTreeParser parser = new NavTreeParser();

        NavTreeNode node = parser.parse(getClass().getResourceAsStream("nav-sub-categories.json"));

        Assertions.assertNotNull(node);
        assertNode(60969, 91650, "Дрели, шуруповерты, гайковерты", node);

        List<NavTreeNode> children = node.getChildren();
        MatcherAssert.assertThat(children, hasSize(5));
        assertNode(60988, 91651, "Дрели-шуруповерты", children.get(0));
        assertNode(60990, 91652, "Аккумуляторные дрели", children.get(1));
        assertNode(60992, 91653, "Сетевые дрели", children.get(2));
        assertNode(60994, 91654, "Ударные дрели", children.get(3));
        assertNode(60996, 91655, "Гайковерты", children.get(4));
    }
}
