package ru.yandex.market.mbo.db.navigation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.mbo.gwt.models.navigation.NavigationNode;
import ru.yandex.market.mbo.gwt.models.navigation.SimpleNavigationNode;
import ru.yandex.market.mbo.gwt.models.navigation.SimpleTag;
import ru.yandex.market.mbo.gwt.models.navigation.Tag;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.models.visual.TreeNode;

import static org.junit.Assert.assertEquals;

/**
 * @author Anastasiya Emelianova / orphie@ / 9/29/21
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("checkstyle:MagicNumber")
public class NavigationTreeTagServiceTest {
    private NavigationTreeTagService navigationTreeTagService;

    @Before
    public void setUp() {
        navigationTreeTagService = Mockito.spy(
            new NavigationTreeTagService(
                Mockito.mock(JdbcTemplate.class),
                Mockito.mock(NamedParameterJdbcTemplate.class),
                Mockito.mock(TransactionTemplate.class)
            ));
    }

    @Test
    public void applyTagInheritanceWithTovarEmptyTags() {
        List<Tag> tags = new ArrayList<>();
        tags.add(createTag("abc", 1L, false));
        tags.add(createTag("qwe", 1L, false));
        tags.add(createTag("asd", 3L, false));

        Mockito.doReturn(tags).when(navigationTreeTagService).getAllTags();

        TovarCategory category = new TovarCategory();
        category.setHid(2L);
        TreeNode<TovarCategory> tovarNode = new TreeNode<>(category);
        TreeNode<NavigationNode> node = createNavigationNodeLinkedToHid(1L, tovarNode.getData(), true);

        navigationTreeTagService.applyTagsInheritanceFromTovarNode(node);
        assertEquals(2, extractTagsNames(node).size());
    }

    @Test
    public void applyTagInheritanceWithTovarTags() {
        List<Tag> tags = new ArrayList<>();
        tags.add(createTag("abc", 1L, false));
        tags.add(createTag("qwe", 1L, false));
        tags.add(createTag("asd", 3L, false));

        Mockito.doReturn(tags).when(navigationTreeTagService).getAllTags();

        TovarCategory category = new TovarCategory();
        List<Tag> tovarTags = new ArrayList<>();
        tovarTags.add(createTag("tovar", 2L, false));
        category.setTags(tovarTags);
        category.setHid(2L);
        TreeNode<TovarCategory> tovarNode = new TreeNode<>(category);
        TreeNode<NavigationNode> node = createNavigationNodeLinkedToHid(1L, tovarNode.getData(), true);

        navigationTreeTagService.applyTagsInheritanceFromTovarNode(node);
        assertEquals(3, extractTagsNames(node).size());
    }

    @Test
    public void applyInheritanceWithoutFlag() {
        List<Tag> tags = new ArrayList<>();
        tags.add(createTag("abc", 1L, false));
        tags.add(createTag("qwe", 1L, false));
        tags.add(createTag("asd", 3L, false));

        Mockito.doReturn(tags).when(navigationTreeTagService).getAllTags();

        TovarCategory category = new TovarCategory();
        List<Tag> tovarTags = new ArrayList<>();
        tovarTags.add(createTag("tovar", 2L, false));
        category.setTags(tovarTags);
        category.setHid(2L);
        TreeNode<TovarCategory> tovarNode = new TreeNode<>(category);

        TreeNode<NavigationNode> navigationNodeTreeNode = createNavigationNodeLinkedToHid(1L, category, false);

        navigationTreeTagService.applyTagsInheritanceFromTovarNode(navigationNodeTreeNode);
        assertEquals(2, extractTagsNames(navigationNodeTreeNode).size());
    }

    private Tag createTag(String name, long nodeId, boolean inherit) {
        Tag tag = new SimpleTag();
        tag.setName(name);
        tag.setNodeId(nodeId);
        tag.setInherit(inherit);
        return tag;
    }

    private TreeNode<NavigationNode> createNavigationNodeLinkedToHid(long id, TovarCategory category, boolean flag) {
        NavigationNode node = new SimpleNavigationNode();
        node.setId(id);
        node.setShouldUseTovarTags(flag);
        node.setHid(2L);
        node.setTovarCategory(category);
        return new TreeNode<>(node);
    }

    private List<String> extractTagsNames(TreeNode<NavigationNode> node) {
        return node.getData().getTagList().stream().map(Tag::getName).collect(Collectors.toList());
    }

}
