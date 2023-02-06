package ru.yandex.market.mbo.db;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.mbo.gwt.models.navigation.SimpleTag;
import ru.yandex.market.mbo.gwt.models.navigation.Tag;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.models.visual.TreeNode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Anastasiya Emelianova / orphie@ / 10/28/21
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("checkstyle:MagicNumber")
public class TovarTreeTagServiceTest {
    private TovarTreeTagService tovarTreeTagService;

    @Before
    public void setUp() {
        tovarTreeTagService = Mockito.spy(
                new TovarTreeTagService(
                        Mockito.mock(JdbcTemplate.class),
                        Mockito.mock(TransactionTemplate.class)
                ));
    }

    @Test
    public void applyTagInheritanceBetweenTwoTovarNodes() {
        List<Tag> tags = new ArrayList<>();
        tags.add(createTag("abc", 1L, false));
        tags.add(createTag("qwe", 1L, true));
        tags.add(createTag("asd", 2L, false));

        Mockito.doReturn(tags).when(tovarTreeTagService).getAllTags();

        TreeNode<TovarCategory> node = createNode(1L);
        TreeNode<TovarCategory> child = createNode(2L);
        node.addChild(child);

        tovarTreeTagService.applyTovarTagsInheritance(node);

        assertEquals(2, extractTagsNames(node).size());
        assertTrue(extractTagsNames(node).contains("abc"));

        assertEquals(2, extractTagsNames(child).size());
        assertTrue(extractTagsNames(child).contains("asd"));
    }

    @Test
    public void applyInheritanceWithoutInheritedTags() {
        List<Tag> tags = new ArrayList<>();
        tags.add(createTag("abc", 1L, false));
        tags.add(createTag("qwe", 1L, false));
        tags.add(createTag("asd", 2L, false));

        Mockito.doReturn(tags).when(tovarTreeTagService).getAllTags();

        TreeNode<TovarCategory> node = createNode(1L);
        TreeNode<TovarCategory> child = createNode(2L);
        node.addChild(child);

        tovarTreeTagService.applyTovarTagsInheritance(node);

        assertEquals(2, extractTagsNames(node).size());
        assertTrue(extractTagsNames(node).contains("abc"));

        assertEquals(1, extractTagsNames(child).size());
        assertTrue(extractTagsNames(child).contains("asd"));
    }

    private Tag createTag(String name, long nodeId, boolean inherit) {
        Tag tag = new SimpleTag();
        tag.setName(name);
        tag.setNodeId(nodeId);
        tag.setInherit(inherit);
        return tag;
    }

    private TreeNode<TovarCategory> createNode(long id) {
        TovarCategory category = new TovarCategory();
        category.setHid(id);
        return new TreeNode<>(category);
    }

    private List<String> extractTagsNames(TreeNode<TovarCategory> node) {
        return node.getData().getTags().stream().map(Tag::getName).collect(Collectors.toList());
    }
}
