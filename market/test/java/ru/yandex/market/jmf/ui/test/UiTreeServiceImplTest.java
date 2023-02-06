package ru.yandex.market.jmf.ui.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.MetadataService;
import ru.yandex.market.jmf.metadata.metaclass.Attribute;
import ru.yandex.market.jmf.ui.UiConstants;
import ru.yandex.market.jmf.ui.UiTreeService;
import ru.yandex.market.jmf.ui.impl.GetTreeNodeChildrenContext;
import ru.yandex.market.jmf.ui.impl.SearchTreeNodesContext;

@Transactional
@SpringJUnitConfig(classes = InternalUiTestConfiguration.class)
@TestPropertySource({"classpath:/do_not_require_getters_for_all_attributes.properties"})
public class UiTreeServiceImplTest {

    private static final Node node_0 = new Node("root_0", "selfParent$t0");
    private static final Node node_1 = new Node("root_1", "selfParent$t0");
    private static final Node node_2 = new Node("root_2", "selfParent$t0");
    private static final Node node_3 = new Node("prefix_3 simpleSearchTitle", "simpleParent$t0");
    private static final Node node_0_0 = new Node("test department", "selfParent$t0", node_0);
    private static final Node node_0_1 = new Node("child_0_1", "selfParent$t0", node_0);
    private static final Node node_0_2 = new Node("child_0_2", "selfParent$t0", node_0);
    private static final Node node_0_3 = new Node("other_child_0_3", "otherParent", node_0);
    private static final Node node_1_0 = new Node("child_1_0", "selfParent$t0", node_1);
    private static final Node node_1_1 = new Node("child_1_1", "selfParent$t0", node_1);
    private static final Node node_1_2 = new Node("child_1_2", "selfParent$t0", node_1);
    private static final Node node_1_3 = new Node("child_1_3", "selfParent$t1", node_1);
    private static final Node node_1_4 = new Node("child_1_4", "selfParent$t2", node_1);
    private static final Node node_3_0 = new Node("prefix_3_0 simpleSearchTitle", "simpleChild$t0", node_3,
            "simpleParent");
    private static final Node node_0_0_0 = new Node("other_child_0_0_0", "otherParent", node_0_0);
    private static final Node node_0_1_0 = new Node("test employee", "selfParent$t0", node_0_1);
    private static final Node node_4 = new Node("two_parent_4", "withTwoParents");
    private static final Node node_0_4 = new Node("two_parent_0_4", "withTwoParents", node_0, "secondParent");
    private static final Node node_0_4_0 = new Node("two_parent_0_4_0", "withTwoParents", node_0_4, "firstParent");
    private static final Node node_0_4_1 = new Node("two_parent_0_4_1", "withTwoParents", node_0_4, "firstParent");
    private static final Node node_5 = new Node("three_parent_5", "withThreeParents");
    private static final Node node_5_0 = new Node("three_parent_5_0", "withThreeParents", node_5, "firstParent");
    private static final Node node_4_0 = new Node("three_parent_4_0", "withThreeParents", node_4, "secondParent");
    private static final Node node_0_5 = new Node("three_parent_0_5", "withThreeParents", node_0, "thirdParent");
    @Inject
    UiTreeService treeService;
    @Inject
    MetadataService metadataService;
    @Inject
    BcpService bcpService;

    private static Stream<Arguments> containsNodesCases() {
        return Stream.of(
                Arguments.of("otherParentRelated", "treeAttr", node_0, List.of(node_0_0, node_0_1, node_0_2,
                        node_0_3)),
                Arguments.of("selfParent", "parent", node_0_1, List.of(node_0_1_0)),
                Arguments.of("selfParent", "parent", node_2, List.of()),
                Arguments.of("otherParent", "parent", node_2, List.of()),
                Arguments.of("selfParent", "parent", null, List.of(node_0, node_1, node_2)),
                Arguments.of("otherParent", "parent", null, List.of(node_0, node_1, node_2)),
                Arguments.of("withTwoParents", "firstParent", null, List.of(node_0, node_1, node_2, node_4)),
                Arguments.of("withTwoParents", "firstParent", node_0_4, List.of(node_0_4_0, node_0_4_1)),
                Arguments.of("withTwoParents", "firstParent", node_0, List.of(node_0_0, node_0_1, node_0_2, node_0_4)),
                Arguments.of("withThreeParents", "firstParent", node_5, List.of(node_5_0)),
                Arguments.of("withThreeParents", "firstParent", node_4, List.of(node_4_0)),
                Arguments.of("withThreeParents", "firstParent", node_0, List.of(node_0_0, node_0_1, node_0_2,
                        node_0_4, node_0_5)),
                Arguments.of("withThreeParents", "firstParent", null, List.of(node_0, node_1, node_2, node_4, node_5))
        );
    }

    private static SearchTreeNodesContext makeSearchTreeNodesContext(Attribute attribute, String filter) {
        return new SearchTreeNodesContext(attribute, filter, null, true, 0, Integer.MAX_VALUE);
    }

    private static Stream<Arguments> expandableNodesCases() {
        return Stream.of(
                Arguments.of("selfParent", "parent", node_0, List.of(node_0_0, node_0_1, node_0_2)),
                Arguments.of("otherParent", "parent", node_0, List.of(node_0_0, node_0_1, node_0_2)),
                Arguments.of("otherParentRelated", "treeAttr", node_0, List.of(node_0_0, node_0_1, node_0_2))
        );
    }

    private static Stream<Arguments> selectableNodesCases() {
        return Stream.of(
                Arguments.of("selfParent$t2", "t2", node_1, List.of(node_1_4)),
                Arguments.of("selfParent$t2", "t1", node_1, List.of(node_1_3, node_1_4)),
                Arguments.of("selfParent$t2", "parent", node_1, List.of(node_1_0, node_1_1, node_1_2, node_1_3,
                        node_1_4)),
                Arguments.of("selfParent", "parent", node_0_0_0, List.of()),
                Arguments.of("otherParent", "parent", node_0_0_0, List.of()),
                Arguments.of("otherParentRelated", "treeAttr", node_0_0, List.of(node_0_0_0)),
                Arguments.of("selfParent", "parent", node_0, List.of(node_0_0, node_0_1, node_0_2)),
                Arguments.of("otherParent", "parent", node_0, List.of(node_0_0, node_0_1, node_0_2)),
                Arguments.of("otherParentRelated", "treeAttr", node_0, List.of(node_0_3))
        );
    }

    @BeforeEach
    public void createContext() {
        for (Node root : List.of(node_0, node_1, node_2, node_3, node_4, node_5)) {
            createNodeTree(root);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "\n"})
    @NullAndEmptySource
    public void searchNodeChildren_ByEmptyFilter_ShouldReturnEmpty(String filter) {
        Attribute attribute = getAttribute("selfParent", "parent");

        List<Map<String, Object>> hierarchy = treeService.searchNodeChildren(makeSearchTreeNodesContext(attribute,
                filter));

        Assertions.assertEquals(0, hierarchy.size());
    }

    @ParameterizedTest
    @MethodSource("containsNodesCases")
    public void getNodeChildren_ByFqnAndAttributeName_ShouldContainExpectedNodes(String fqn,
                                                                                 String attributeCode,
                                                                                 @Nullable Node root,
                                                                                 List<Node> expectedNodes) {
        var gid = root != null ? root.entity.getGid() : null;
        List<Map<String, Object>> nodes = treeService.getNodeChildren(createTreeContext(fqn, attributeCode, gid));

        Assertions.assertEquals(expectedNodes.size(), nodes.size());
        for (Node expectedNode : expectedNodes) {
            assertContains(expectedNode.entity, nodes);
        }
    }

    @ParameterizedTest
    @MethodSource("selectableNodesCases")
    public void getNodeChildren_ByFqnAndAttributeName_FindSelectableNodes(String fqn,
                                                                          String attributeCode,
                                                                          Node root,
                                                                          List<Node> expectedNodes) {
        List<Map<String, Object>> nodes = treeService
                .getNodeChildren(createTreeContext(fqn, attributeCode, root.entity.getGid()))
                .stream()
                .filter(x -> (Boolean) x.get(UiConstants.SELECTABLE))
                .collect(Collectors.toList());

        Assertions.assertEquals(expectedNodes.size(), nodes.size());
        for (Node expectedNode : expectedNodes) {
            assertContains(expectedNode.entity, nodes);
        }
    }

    @ParameterizedTest
    @MethodSource("expandableNodesCases")
    public void getNodeChildren_ByFqnAndAttributeName_FindExpandableNodes(String fqn,
                                                                          String attributeCode,
                                                                          Node root,
                                                                          List<Node> expectedNodes) {
        List<Map<String, Object>> nodes = treeService
                .getNodeChildren(createTreeContext(fqn, attributeCode, root.entity.getGid()))
                .stream()
                .filter(x -> (Boolean) x.get(UiConstants.EXPANDABLE))
                .collect(Collectors.toList());

        Assertions.assertEquals(expectedNodes.size(), nodes.size());
        for (Node expectedNode : expectedNodes) {
            assertContains(expectedNode.entity, nodes);
        }
    }

    @Test
    public void searchNodeChildren_ByTitlePrefix_ShouldReturnSelectableHierarchy() {
        Attribute attribute = getAttribute("selfParent", "parent");

        List<Map<String, Object>> hierarchy = treeService.searchNodeChildren(makeSearchTreeNodesContext(attribute,
                "test"));

        Assertions.assertEquals(1, hierarchy.size());
        assertContains(node_0.entity, hierarchy);

        hierarchy = ((List<Map<String, Object>>) hierarchy.get(0).get("children"));

        Assertions.assertEquals(2, hierarchy.size());
        assertContains(node_0_0.entity, hierarchy);
        assertContains(node_0_1.entity, hierarchy);

        var node_0_1_children = (List<Map<String, Object>>) hierarchy.stream()
                .filter(x -> x.get("gid") == node_0_1.entity.getGid())
                .findFirst()
                .orElse(Map.of())
                .get("children");

        Assertions.assertEquals(1, node_0_1_children.size());
        assertContains(node_0_1_0.entity, node_0_1_children);
    }

    @Test
    public void searchNodeChildren_ForSimpleParent_ShouldReturnSingle() {
        var attribute = getAttribute("simpleChild", "simpleParent");

        List<Map<String, Object>> hierarchy = treeService.searchNodeChildren(makeSearchTreeNodesContext(attribute,
                "simpleSearchTitle"));

        Assertions.assertEquals(1, hierarchy.size());
        assertContains(node_3.entity, hierarchy);

        hierarchy = ((List<Map<String, Object>>) hierarchy.get(0).get("children"));

        Assertions.assertEquals(0, hierarchy.size());
    }

    @Test
    public void searchNodeChildren_ForSimpleChild_ShouldReturnSingle() {
        var attribute = getAttribute("simpleParent$t0", "supervisors");

        List<Map<String, Object>> hierarchy = treeService.searchNodeChildren(makeSearchTreeNodesContext(attribute,
                "simpleSearchTitle"));

        Assertions.assertEquals(1, hierarchy.size());
        assertContains(node_3.entity, hierarchy);
        Assertions.assertFalse((boolean) hierarchy.get(0).get(UiConstants.SELECTABLE));

        hierarchy = ((List<Map<String, Object>>) hierarchy.get(0).get("children"));

        Assertions.assertEquals(1, hierarchy.size());
        assertContains(node_3_0.entity, hierarchy);
        Assertions.assertTrue((boolean) hierarchy.get(0).get(UiConstants.SELECTABLE));
    }

    private void assertContains(Entity expected, List<Map<String, Object>> actual) {
        Assertions.assertTrue(actual.stream().anyMatch(a -> expected.getGid().equals(a.get("gid"))));
    }

    private void createNodeTree(Node node) {
        var properties = new HashMap<String, Object>(Map.of("title", node.title));
        node.parent.ifPresent(parent -> properties.put(node.parentAttributeName, parent.entity));

        node.entity = bcpService.create(node.fqn, properties);

        for (Node child : node.children) {
            createNodeTree(child);
        }
    }

    private Attribute getAttribute(String fqn, String attributeCode) {
        return metadataService.getMetaclassOrError(Fqn.of(fqn)).getAttributeOrError(attributeCode);
    }

    private GetTreeNodeChildrenContext createTreeContext(String fqn, String attributeCode, String rootGid) {
        return new GetTreeNodeChildrenContext(
                metadataService.getMetaclassOrError(Fqn.of(fqn)).getAttributeOrError(attributeCode),
                rootGid,
                Map.of(),
                false,
                0,
                Integer.MAX_VALUE);
    }

    static class Node {
        private final String title;
        private final Fqn fqn;
        private final Optional<Node> parent;
        private final String parentAttributeName;
        private final List<Node> children = new ArrayList<>();
        private Entity entity;

        Node(String title, String fqn) {
            this(title, fqn, null);
        }

        Node(String title, String fqn, Node parent) {
            this(title, fqn, parent, "parent");
        }

        Node(String title, String fqn, @Nullable Node parent, String parentAttributeName) {
            this.title = title;
            this.fqn = Fqn.of(fqn);
            this.parent = Optional.ofNullable(parent);
            this.parentAttributeName = parentAttributeName;

            this.parent.ifPresent(x -> x.children.add(this));
        }

        @Override
        public String toString() {
            return title;
        }
    }

}
