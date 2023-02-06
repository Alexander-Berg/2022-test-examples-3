package ru.yandex.market.mbo.db.navigation;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.yandex.common.util.db.IdGenerator;
import ru.yandex.market.mbo.gwt.models.navigation.Filter;
import ru.yandex.market.mbo.gwt.models.navigation.NavigationNode;
import ru.yandex.market.mbo.gwt.models.navigation.SimpleFilter;
import ru.yandex.market.mbo.gwt.models.navigation.SimpleNavigationNode;
import ru.yandex.market.mbo.gwt.models.visual.TreeNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author kravchenko-aa
 * @date 25.09.18
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("checkstyle:MagicNumber")
public class NavigationTreeFilterServiceTest {

    private NavigationTreeFilterService navigationTreeFilterService;

    @Before
    public void setUp() {
        navigationTreeFilterService = Mockito.spy(
            new NavigationTreeFilterService(
                Mockito.mock(NamedParameterJdbcTemplate.class),
                Mockito.mock(JdbcTemplate.class),
                Mockito.mock(IdGenerator.class)));

        List<Filter> filters = new ArrayList<>();
        filters.add(createFilter(1L, 0L, 0L, true, Filter.Type.SIMPLE));
        filters.add(createFilter(2L, 1L, 1L, false, Filter.Type.BREAK_INHERITANCE));
        filters.add(createFilter(3L, 1L, 0L, true, Filter.Type.SIMPLE));
        filters.add(createFilter(4L, 11L, 1L, true, Filter.Type.SIMPLE));
        filters.add(createFilter(5L, 2L, 1L, true, Filter.Type.BREAK_INHERITANCE));
        filters.add(createFilter(6L, 2L, 0L, false, Filter.Type.SIMPLE));

        Mockito.doReturn(filters).when(navigationTreeFilterService).getNodeFilters();
    }

    @Test
    public void applyFilterInheritance() {
        TreeNode<NavigationNode> node = createNode(0L);
        TreeNode<NavigationNode> child1 = createNode(1L);
        node.addChild(child1);
        TreeNode<NavigationNode> child11 = createNode(11L);
        TreeNode<NavigationNode> child12 = createNode(12L);
        child1.addChild(child11);
        child1.addChild(child12);
        TreeNode<NavigationNode> child2 = createNode(2L);
        TreeNode<NavigationNode> child21 = createNode(21L);
        node.addChild(child2);
        child2.addChild(child21);

        navigationTreeFilterService.applyFilterInheritance(node);

        assertEquals(extractFiltersIds(node).size(), 1);
        assertTrue(extractFiltersIds(node).contains(1L));

        assertEquals(extractFiltersIds(child1).size(), 2);
        assertTrue(extractFiltersIds(child1).containsAll(Arrays.asList(2L, 3L)));

        assertEquals(extractFiltersIds(child11).size(), 2);
        assertTrue(extractFiltersIds(child11).containsAll(Arrays.asList(3L, 4L)));

        assertEquals(extractFiltersIds(child12).size(), 2);
        assertTrue(extractFiltersIds(child12).containsAll(Arrays.asList(1L, 3L)));

        assertEquals(extractFiltersIds(child2).size(), 2);
        assertTrue(extractFiltersIds(child2).containsAll(Arrays.asList(1L, 6L)));

        assertEquals(extractFiltersIds(child21).size(), 0);
    }

    private List<Long> extractFiltersIds(TreeNode<NavigationNode> node) {
        return node.getData().getFilters().stream().map(Filter::getId).collect(Collectors.toList());
    }

    private TreeNode<NavigationNode> createNode(long id) {
        NavigationNode node = new SimpleNavigationNode();
        node.setId(id);
        return new TreeNode<>(node);
    }

    private Filter createFilter(long id, long nodeId, long overriddenFilterId, boolean inherit, Filter.Type type) {
        Filter filter = new SimpleFilter();
        filter.setId(id);
        filter.setNodeId(nodeId);
        filter.setOverriddenFilterId(overriddenFilterId);
        filter.setInherit(inherit);
        filter.setType(type);
        return filter;
    }
}
