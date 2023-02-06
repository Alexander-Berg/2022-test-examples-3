package ru.yandex.market.loyalty.core.model;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertSame;

/**
 * @author <a href="mailto:maratik@yandex-team.ru">Marat Bukharov</a>
 */
public class CategoryTreeTest {

    @Test
    public void shouldCreateCategoryTree() {
        CategoryTree categoryTree = new CategoryTree(ImmutableList.of(
                new CategoryTreeRecord(2, 1L, "child1"),
                new CategoryTreeRecord(1, null, "root"),
                new CategoryTreeRecord(3, 1L, "child2")
        ));

        CategoryTree.Node root = categoryTree.findNodeByHid(1)
                .orElseThrow(() -> new AssertionError("No root found"));

        assertSame(categoryTree.getRoot(), root);

        assertThat(root.getChildren(), containsInAnyOrder(
                allOf(
                        hasProperty("hid", equalTo(2L)),
                        hasProperty("name", equalTo("child1"))
                ),
                allOf(
                        hasProperty("hid", equalTo(3L)),
                        hasProperty("name", equalTo("child2"))
                )
        ));
    }

    @Test
    public void shouldFindHidInTree() {
        CategoryTree categoryTree = new CategoryTree(ImmutableList.of(
                new CategoryTreeRecord(2, 1L, "child1"),
                new CategoryTreeRecord(1, null, "root"),
                new CategoryTreeRecord(3, 1L, "child2")
        ));

        assertThat(
                categoryTree.findNodeByHid(3)
                        .orElseThrow(() -> new AssertionError("No node found")),
                allOf(
                        hasProperty("hid", equalTo(3L)),
                        hasProperty("name", equalTo("child2"))
                )
        );
    }

    @Test
    public void shouldNotThrowNPE() {
        CategoryTree categoryTree = new CategoryTree(ImmutableList.of(
                new CategoryTreeRecord(2, 1L, "child1"),
                new CategoryTreeRecord(1, null, "root"),
                new CategoryTreeRecord(3, 1L, "child2")
        ));
        Stream<Integer> hids = Stream.of(null, null, null);
        categoryTree.enumAllHids((Collection<Integer>) hids.collect(Collectors.toCollection(ArrayList::new)));
    }

    @Test
    public void shouldFindAncestor() {
        CategoryTree categoryTree = new CategoryTree(ImmutableList.of(
                new CategoryTreeRecord(2, 1L, "child1"),
                new CategoryTreeRecord(1, null, "root"),
                new CategoryTreeRecord(3, 1L, "child2")
        ));

        assertThat(
                categoryTree.findNodeByHid(3)
                        .orElseThrow(() -> new AssertionError("No node found"))
                        .findAnyAncestorIncludingSelf(Collections.singleton(1L))
                        .orElseThrow(() -> new AssertionError("No ancestor found")),
                allOf(
                        hasProperty("hid", equalTo(1L)),
                        hasProperty("name", equalTo("root"))
                )
        );
    }

    @Test
    public void shouldPlaneTree() {
        CategoryTree categoryTree = new CategoryTree(ImmutableList.of(
                new CategoryTreeRecord(2, 1L, "child1"),
                new CategoryTreeRecord(1, null, "root"),
                new CategoryTreeRecord(3, 1L, "child2"),
                new CategoryTreeRecord(4, 2L, "child4")
        ));

        assertThat(
                categoryTree.findNodeByHid(1)
                        .orElseThrow(() -> new AssertionError("No node found"))
                        .getPlane(),
                containsInAnyOrder(
                        allOf(
                                hasProperty("hid", equalTo(1L)),
                                hasProperty("name", equalTo("root"))
                        ),
                        allOf(
                                hasProperty("hid", equalTo(2L)),
                                hasProperty("name", equalTo("child1"))
                        ),
                        allOf(
                                hasProperty("hid", equalTo(3L)),
                                hasProperty("name", equalTo("child2"))
                        ),
                        allOf(
                                hasProperty("hid", equalTo(4L)),
                                hasProperty("name", equalTo("child4"))
                        )
                )
        );

        assertThat(
                categoryTree.findNodeByHid(2)
                        .orElseThrow(() -> new AssertionError("No node found"))
                        .getPlane(),
                containsInAnyOrder(
                        allOf(
                                hasProperty("hid", equalTo(2L)),
                                hasProperty("name", equalTo("child1"))
                        ),
                        allOf(
                                hasProperty("hid", equalTo(4L)),
                                hasProperty("name", equalTo("child4"))
                        )
                )
        );
    }
}
