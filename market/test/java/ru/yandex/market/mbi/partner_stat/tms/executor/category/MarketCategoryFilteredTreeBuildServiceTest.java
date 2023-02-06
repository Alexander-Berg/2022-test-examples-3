package ru.yandex.market.mbi.partner_stat.tms.executor.category;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.partner_stat.FunctionalTest;
import ru.yandex.market.mbi.partner_stat.entity.MarketCategoryFilteredEntity;
import ru.yandex.market.mbi.partner_stat.entity.NavigationTreeType;
import ru.yandex.market.mbi.partner_stat.repository.MarketCategoryFilteredRepository;
import ru.yandex.market.mbi.partner_stat.tms.executor.category.service.market.tree.MarketCategoryFilteredTreeBuildService;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Тесты на {@link MarketCategoryFilteredTreeBuildService}
 */
@DbUnitDataSet(before = "MarketCategoryTreeBuildService/before.csv")
class MarketCategoryFilteredTreeBuildServiceTest extends FunctionalTest {

    @Autowired
    private MarketCategoryFilteredTreeBuildService marketCategoryFilteredTreeBuildService;

    @Autowired
    private MarketCategoryFilteredRepository marketCategoryFilteredRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DbUnitDataSet(before = "MarketCategoryTreeBuildService/testBuildTrees/before.csv")
    void testBuildTrees() {
        marketCategoryFilteredTreeBuildService.buildTrees();

        Map<NavigationTreeType, NodeHid> trees = Map.of(
                NavigationTreeType.FMCG,
                NodeHid.of(1,
                        NodeHid.of(2, NodeHid.of(8)),
                        NodeHid.of(23),
                        NodeHid.of(24, NodeHid.of(25))),

                NavigationTreeType.BLUE,
                NodeHid.of(1,
                        NodeHid.of(7),
                        NodeHid.of(8),
                        NodeHid.of(23),
                        NodeHid.of(25))
        );
        // в транзакции, чтобы извлечь все LAZY поля
        transactionTemplate.execute(status -> {
            trees.forEach((treeId, expectedTree) -> {
                NodeHid actualTree = marketCategoryFilteredRepository.findByParentIsNullAndNavigationTreeCode(treeId)
                        .map(this::convertToTreeHid)
                        .orElseThrow(() -> new IllegalStateException("Root for tree " + treeId + " not found"));
                assertThat(actualTree, Matchers.is(expectedTree));
            });
            return true;
        });
    }

    private NodeHid convertToTreeHid(MarketCategoryFilteredEntity rootEntity) {
        return NodeHid.of(
                rootEntity.getMarketCategory().getHid(),
                rootEntity.getChildren().stream().map(this::convertToTreeHid).toArray(NodeHid[]::new)
        );
    }

    private static class NodeHid {
        private final long hid;
        private final Set<NodeHid> children;

        private NodeHid(long hid, Set<NodeHid> children) {
            this.hid = hid;
            this.children = children;
        }

        static NodeHid of(long hid, NodeHid... children) {
            return new NodeHid(hid, Set.of(children));
        }

        public long getHid() {
            return hid;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            NodeHid nodeHid = (NodeHid) o;
            return hid == nodeHid.hid &&
                    children.equals(nodeHid.children);
        }

        @Override
        public int hashCode() {
            return Objects.hash(hid, children);
        }
    }
}
