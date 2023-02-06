package ru.yandex.market.orders.returns;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.io.ByteStreams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.yt.YtHttpFactory;
import ru.yandex.market.mbi.yt.YtCluster;
import ru.yandex.market.mbi.yt.YtTemplate;
import ru.yandex.market.mbi.yt.YtUtilTest;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.Mockito.when;

public class ImportOrderReturnItemsCommandTest extends FunctionalTest {

    private static final String HAHN = "hahn.yt.yandex.net";
    private static final Object NULL = new Object();

    private static final Function<Object, YTreeNode> LONG_NODE =
            v -> v == NULL ? YtUtilTest.nullNode() : YtUtilTest.longNode(((Integer) (v)).longValue());
    private static final Function<Object, YTreeNode> INT_NODE =
            v -> v == NULL ? YtUtilTest.nullNode() : YtUtilTest.intNode((Integer) v);
    private static final Function<Object, YTreeNode> STRING_NODE = v -> YtUtilTest.stringNode((String) v);
    private static final Function<Object, YTreeNode> DOUBLE_NODE = v -> YtUtilTest.floatNode(((Double) v).floatValue());
    private static final Function<Object, YTreeNode> STRING_LIST_NODE =
            v -> YtUtilTest.treeListNode(((List<String>) v).stream()
                    .map(YtUtilTest::stringNode)
                    .collect(Collectors.toList()));

    private static final Map<String, Function<Object, YTreeNode>> returnItemYtFields = Map.of(
            "id", LONG_NODE,
            "return_id", LONG_NODE,
            "order_id", LONG_NODE,
            "item_id", LONG_NODE,
            "count", INT_NODE,
            "supplier_compensation", DOUBLE_NODE,
            "return_reason", STRING_NODE,
            "reason_type", INT_NODE,
            "subreason_type", INT_NODE,
            "pictures_urls", STRING_LIST_NODE
    );

    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private YtHttpFactory ytHttpFactory;

    @Autowired
    private ImportOrderReturnItemsService importOrderReturnItemsService;

    private ImportOrderReturnItemsCommand importOrderReturnItemsCommand;
    private Terminal terminal;
    private YtTemplate resuppliesYtTemplate;

    @BeforeEach
    void setUp() {
        terminal = Mockito.mock(Terminal.class);
        mockYt();
        Mockito.when(terminal.getWriter()).thenReturn(new PrintWriter(ByteStreams.nullOutputStream()));
        importOrderReturnItemsCommand = new ImportOrderReturnItemsCommand(importOrderReturnItemsService,
                resuppliesYtTemplate, transactionTemplate);
    }

    @Test
    @DbUnitDataSet(before = "ImportOrderReturnItemsCommandTest.before.csv",
            after = "ImportOrderReturnItemsCommandTest.after.csv")
    void testReimport() {
        importOrderReturnItemsCommand.executeCommand(command(), terminal);
    }

    private static CommandInvocation command() {
        return new CommandInvocation(
                "import-order-return-items",
                new String[]{"path"},
                Map.of()
        );
    }

    private void mockYt() {
        final Yt hahn = YtUtilTest.mockYt(getYtReturnItems());
        when(ytHttpFactory.getYt(HAHN)).thenReturn(hahn);
        resuppliesYtTemplate = new YtTemplate(new YtCluster[]{new YtCluster(HAHN, hahn)});
    }

    private List<YTreeMapNode> getYtReturnItems() {
        return toYtNodes(List.of(
                Map.of(
                        "id", 7777,
                        "count", 2,
                        "item_id", 30,
                        "order_id", 92,
                        "reason_type", 0,
                        "return_id", 8888,
                        "return_reason", "Не подошел",
                        "supplier_compensation", 1111.0,
                        "pictures_urls", List.of("single url")
                ),
                Map.of(
                        "id", 7779,
                        "count", 1,
                        "item_id", 10,
                        "order_id", 91,
                        "reason_type", 0,
                        "return_id", 8890,
                        "return_reason", "Не подошел",
                        "supplier_compensation", 1111.0,
                        "pictures_urls", List.of("single url")
                )));
    }

    private static List<YTreeMapNode> toYtNodes(List<Map<String, Object>> orderReturns) {
        return orderReturns.stream()
                .map(orderReturnItem -> YtUtilTest.treeMapNode(
                                returnItemYtFields.entrySet().stream()
                                        .filter(entry -> orderReturnItem.containsKey(entry.getKey()))
                                        .collect(Collectors.toMap(
                                                        Map.Entry::getKey,
                                                        entry -> entry.getValue().apply(orderReturnItem.get(entry.getKey()))
                                                )
                                        )
                        )
                ).collect(Collectors.toList());
    }
}
