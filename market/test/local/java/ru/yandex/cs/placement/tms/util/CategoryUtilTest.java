package ru.yandex.cs.placement.tms.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import ru.yandex.cs.placement.tms.mbo.MboCategory;
import ru.yandex.cs.placement.tms.util.CategoryUtil.*;

import java.util.List;

import static com.google.common.collect.Sets.*;
import static java.util.Arrays.*;
import static java.util.Collections.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static ru.yandex.cs.placement.tms.util.CategoryUtil.*;

public class CategoryUtilTest {

    @Rule
    public final ExpectedException expectedExceptionRule = ExpectedException.none();

    @Test
    public void link_categories_for_single_element_with_no_parent() throws Exception {
        MboCategory root = cat(42, null, "qwe");
        List<CategoryWithParent> categoryWithParents = linkCategories(singletonList(root));
        assertThat(categoryWithParents, hasSize(1));
        CategoryWithParent categoryWithParent = categoryWithParents.get(0);
        assertThat(categoryWithParent.getParent(), nullValue());
        assertThat(categoryWithParent.getCategory(), sameInstance(root));
        assertThat(categoryWithParent.getPath(), is("qwe"));
    }

    @Test
    public void link_categories_join_category_to_parent() throws Exception {
        MboCategory root = cat(42, null, "qwe");
        MboCategory child = cat(43, 42L, "rty");
        List<CategoryWithParent> categoryWithParents = linkCategories(asList(root, child));
        assertThat(categoryWithParents, hasSize(2));
        // nodes for categories are received in the same order
        CategoryWithParent rootNode = categoryWithParents.get(0);
        CategoryWithParent childNode = categoryWithParents.get(1);
        // and nodes are linked properly as children to parents
        assertThat(rootNode.getParent(), nullValue());
        assertThat(rootNode.getCategory(), sameInstance(root));
        assertThat(rootNode.getPath(), is("qwe"));
        assertThat(childNode.getParent(), sameInstance(rootNode));
        assertThat(childNode.getCategory(), sameInstance(child));
        assertThat(childNode.getPath(), is("qwe{^path^}rty"));
    }

    @Test
    public void link_categories_does_not_depend_on_initial_order() throws Exception {
        MboCategory root = cat(42, null, "qwe");
        MboCategory child = cat(43, 42L, "rty");
        // when categories are specified in reverse order
        List<CategoryWithParent> categoryWithParents = linkCategories(asList(child, root));
        assertThat(categoryWithParents, hasSize(2));
        // then nodes are in reverse order
        CategoryWithParent childNode = categoryWithParents.get(0);
        CategoryWithParent rootNode = categoryWithParents.get(1);
        // but they're still linked properly
        assertThat(childNode.getParent(), sameInstance(rootNode));
        assertThat(childNode.getCategory(), sameInstance(child));
        assertThat(childNode.getPath(), is("qwe{^path^}rty"));
        assertThat(rootNode.getParent(), nullValue());
        assertThat(rootNode.getCategory(), sameInstance(root));
        assertThat(rootNode.getPath(), is("qwe"));
    }

    @Test
    public void link_categories_joins_grand_children() throws Exception {
        MboCategory root = cat(42, null, "qwe");
        MboCategory child = cat(43, 42L, "rty");
        MboCategory grandChild = cat(44, 43L, "qaz");
        List<CategoryWithParent> categoryWithParents = linkCategories(asList(grandChild, root, child));
        assertThat(categoryWithParents, hasSize(3));
        CategoryWithParent grandChildNode = categoryWithParents.get(0);
        CategoryWithParent rootNode = categoryWithParents.get(1);
        CategoryWithParent childNode = categoryWithParents.get(2);
        assertThat(grandChildNode.getCategory(), sameInstance(grandChild));
        assertThat(grandChildNode.getParent(), sameInstance(childNode));
        assertThat(grandChildNode.getParent().getParent(), sameInstance(rootNode));
        assertThat(grandChildNode.getPath(), is("qwe{^path^}rty{^path^}qaz"));
    }

    @Test
    public void link_categories_joins_parallel_branches() throws Exception {
        MboCategory root = cat(42, null, "qwe");
        MboCategory child1 = cat(43, 42L, "rty");
        MboCategory child2 = cat(33, 42L, "pop");
        MboCategory grandChild1 = cat(44, 43L, "qaz");
        MboCategory grandChild2 = cat(34, 33L, "lol");
        List<CategoryWithParent> categoryWithParents = linkCategories(asList(child1, grandChild1, child2, grandChild2, root));
        assertThat(categoryWithParents, hasSize(5));
        CategoryWithParent child1Node = categoryWithParents.get(0);
        CategoryWithParent grandChild1Node = categoryWithParents.get(1);
        CategoryWithParent child2Node = categoryWithParents.get(2);
        CategoryWithParent grandChild2Node = categoryWithParents.get(3);
        CategoryWithParent rootNode = categoryWithParents.get(4);
        assertThat(grandChild1Node.getCategory(), sameInstance(grandChild1));
        assertThat(grandChild1Node.getParent(), sameInstance(child1Node));
        assertThat(grandChild1Node.getParent().getParent(), sameInstance(rootNode));
        assertThat(grandChild1Node.getPath(), is("qwe{^path^}rty{^path^}qaz"));
        assertThat(grandChild2Node.getCategory(), sameInstance(grandChild2));
        assertThat(grandChild2Node.getParent(), sameInstance(child2Node));
        assertThat(grandChild2Node.getParent().getParent(), sameInstance(rootNode));
        assertThat(grandChild2Node.getPath(), is("qwe{^path^}pop{^path^}lol"));
    }

    @Test
    public void link_categories_throws_exception_if_root_not_found() throws Exception {
        expectedExceptionRule.expect(IllegalArgumentException.class);
        expectedExceptionRule.expectMessage("Categories not found: [42]");
        MboCategory child = cat(43, 42L, "rty");
        linkCategories(singletonList(child));
    }

    @Test
    public void link_categories_throws_exception_if_middle_node_not_found() throws Exception {
        expectedExceptionRule.expect(IllegalArgumentException.class);
        expectedExceptionRule.expectMessage("Categories not found: [43]");
        MboCategory root = cat(42, null, "qwe");
        MboCategory grandChild = cat(44, 43L, "qaz");
        linkCategories(asList(root, grandChild));
    }

    @Test
    public void link_categories_throws_exception_if_multiple_nodes_not_found() throws Exception {
        expectedExceptionRule.expect(IllegalArgumentException.class);
        expectedExceptionRule.expectMessage("Categories not found: [33, 43]");
        MboCategory root = cat(42, null, "qwe");
        MboCategory grandChild1 = cat(44, 43L, "qaz");
        MboCategory grandChild2 = cat(34, 33L, "lol");
        linkCategories(asList(root, grandChild1, grandChild2));
    }

    @Test
    public void link_categories_counts_children() throws Exception {
        MboCategory root = cat(42, null, "qwe");
        MboCategory child = cat(43, 42L, "rty");
        List<CategoryWithParent> categoryWithParents = linkCategories(asList(root, child));
        CategoryWithParent rootNode = categoryWithParents.get(0);
        CategoryWithParent childNode = categoryWithParents.get(1);
        assertThat(rootNode.getChildren(), is(singleton(childNode)));
        assertThat(rootNode.isLeaf(), is(false));
        assertThat(childNode.getChildren(), is(emptySet()));
        assertThat(childNode.isLeaf(), is(true));
    }

    @Test
    public void link_categories_does_not_count_descendants() throws Exception {
        MboCategory root = cat(42, null, "qwe");
        MboCategory child1 = cat(43, 42L, "rty");
        MboCategory grandChild1 = cat(44, 43L, "qaz");
        MboCategory child2 = cat(33, 42L, "pop");
        MboCategory grandChild2 = cat(34, 33L, "lol");
        List<CategoryWithParent> categoryWithParents = linkCategories(asList(root, child1, child2, grandChild1, grandChild2));
        CategoryWithParent rootNode = categoryWithParents.get(0);
        CategoryWithParent child1Node = categoryWithParents.get(1);
        CategoryWithParent child2Node = categoryWithParents.get(2);
        CategoryWithParent grandChild1Node = categoryWithParents.get(3);
        CategoryWithParent grandChild2Node = categoryWithParents.get(4);
        assertThat(rootNode.getChildren(), is(newHashSet(child1Node, child2Node)));
        assertThat(child1Node.getChildren(), is(singleton(grandChild1Node)));
        assertThat(child2Node.getChildren(), is(singleton(grandChild2Node)));
        assertThat(rootNode.isLeaf(), is(false));
        assertThat(child1Node.isLeaf(), is(false));
        assertThat(child2Node.isLeaf(), is(false));
        assertThat(grandChild1Node.isLeaf(), is(true));
        assertThat(grandChild2Node.isLeaf(), is(true));
    }

    private static MboCategory cat(long id, Long parentId, String name) {
        MboCategory category = new MboCategory();
        category.setHyperId(id);
        category.setParentId(parentId);
        category.setName(name);
        return category;
    }
}