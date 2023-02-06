package ru.yandex.market.billing.imports.shopsku;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Sets;
import one.util.streamex.IntStreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandExecutor;
import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Range;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.market.FunctionalTest;
import ru.yandex.market.TestTerminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.yql_test.YqlTablePathConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.billing.imports.shopsku.ImportAllShopSkuCommand.OPTION_CHUNK;
import static ru.yandex.market.billing.imports.shopsku.ImportAllShopSkuCommand.OPTION_CLUSTER;
import static ru.yandex.market.billing.imports.shopsku.ImportAllShopSkuCommand.OPTION_START_ROW;
import static ru.yandex.market.billing.imports.shopsku.ImportAllShopSkuCommand.OPTION_TABLE;
import static ru.yandex.market.billing.imports.shopsku.ImportAllShopSkuCommand.OPTION_THREADS;

public class ImportAllShopSkuCommandTest extends FunctionalTest {

    private static final String OPTION_VALUE_CLUSTER = "hahn";
    private static final String OPTION_VALUE_TABLE = "//home/market/users/mexicano/sku_info_2022_04_25";
    private static final String OPTION_VALUE_THREADS = "2";
    private static final String OPTION_VALUE_CHUNK = "2";

    @Autowired
    ShopSkuInfoDao shopSkuInfoDao;

    @Autowired
    CommandExecutor commandExecutor;

    @Autowired
    YqlTablePathConverter yqlTablePathConverter;

    YtTables ytTables;
    Yt hahnYt;
    ImportAllShopSkuCommand command;

    @BeforeEach
    public void beforeEach() {
        prepareMocks();
        command = new ImportAllShopSkuCommand(hahnYt, null, shopSkuInfoDao, commandExecutor);
    }

    private void prepareMocks() {
        ytTables = mock(YtTables.class);
        hahnYt = mock(Yt.class);
        when(hahnYt.tables()).thenReturn(ytTables);
    }

    @Test
    @DbUnitDataSet(
            before = "ImportAllShopSkuCommandTest.common.before.csv",
            after = "ImportAllShopSkuCommandTest.addAndUpdate.after.csv"
    )
    public void addAndUpdate() {
        mockYtTable(List.of(
                row(465232, "1076314", "200000", "300000", "400000", "5600000"),
                row(465232, "abc-1", "100000", "200000", "300000", "4500000"),
                row(465232, "abc-2", "110000", "210000", "310000", "5500000"),
                row(475443, "aaa", "1100000", "2100000", "3100000", "50500000"),
                row(475443, "bbb", "1200000", "2200000", "3200000", "5800000"),
                row(475444, "a1", "4200000", "71000000", "34000000", "38000000"),
                row(475444, "a2", "4200000", "71000000", "39000000", "41000000"),
                row(475444, "a3", "4500000", "71000000", "39000000", "42000000"),
                row(475445, "a3", "45000000", "7800000", "3400000", "12000000")
        ));
        command.executeCommand(createCommandInvocation(), new TestTerminal());
    }

    @Test
    @DbUnitDataSet(
            before = "ImportAllShopSkuCommandTest.common.before.csv",
            after = "ImportAllShopSkuCommandTest.addAndUpdateWithStartRow.after.csv"
    )
    // импорт с определенной строки
    public void addAndUpdateWithStartRow() {
        mockYtTable(List.of(
                row(465232, "1076314", "200000", "300000", "400000", "5600000"),
                row(465232, "abc-1", "100000", "200000", "300000", "4500000"),
                row(465232, "abc-2", "110000", "210000", "310000", "5500000"),
                row(475443, "aaa", "1100000", "2100000", "3100000", "50500000"),
                row(475443, "bbb", "1200000", "2200000", "3200000", "5800000"),
                row(475444, "a1", "4200000", "71000000", "34000000", "38000000"),
                row(475444, "a2", "4200000", "71000000", "39000000", "41000000"),
                row(475444, "a3", "4500000", "71000000", "39000000", "42000000"),
                row(475445, "a3", "45000000", "7800000", "3400000", "12000000")
        ));
        command.executeCommand(createCommandInvocation(3), new TestTerminal());
    }

    @Test
    @DbUnitDataSet(
            before = "ImportAllShopSkuCommandTest.common.before.csv",
            after = "ImportAllShopSkuCommandTest.numberOfRowsIsMultipleOfChunk.after.csv"
    )
    // количество строк кратно размеру чанка
    public void numberOfRowsIsMultipleOfChunk() {
        mockYtTable(List.of(
                row(465232, "1076314", "200000", "300000", "400000", "5600000"),
                row(465232, "abc-1", "100000", "200000", "300000", "4500000"),
                row(465232, "abc-2", "110000", "210000", "310000", "5500000"),
                row(475443, "aaa", "1100000", "2100000", "3100000", "50500000"),
                row(475443, "bbb", "1200000", "2200000", "3200000", "5800000"),
                row(475444, "a1", "4200000", "71000000", "34000000", "38000000")
        ));
        command.executeCommand(createCommandInvocation(), new TestTerminal());
    }

    @Test
    @DbUnitDataSet(
            before = "ImportAllShopSkuCommandTest.common.before.csv",
            after = "ImportAllShopSkuCommandTest.numberOfRowsIsNotMultipleOfChunk.after.csv"
    )
    // количество строк не кратно размеру чанка
    public void numberOfRowsIsNotMultipleOfChunk() {
        mockYtTable(List.of(
                row(465232, "1076314", "200000", "300000", "400000", "5600000"),
                row(465232, "abc-1", "100000", "200000", "300000", "4500000"),
                row(465232, "abc-2", "110000", "210000", "310000", "5500000"),
                row(475443, "aaa", "1100000", "2100000", "3100000", "50500000"),
                row(475443, "bbb", "1200000", "2200000", "3200000", "5800000"),
                row(475444, "a1", "4200000", "71000000", "34000000", "38000000"),
                row(475444, "a2", "4200000", "71000000", "39000000", "41000000")
        ));
        command.executeCommand(createCommandInvocation(), new TestTerminal());
    }

    @Test
    // не читает строки повторно
    public void dontRepeatReadingEachRow() {
        Map<Integer, Integer> rowToReadCountMap = new HashMap<>();
        Consumer<Integer> repeatedRowsConsumer =
                repeatedRow -> rowToReadCountMap.put(repeatedRow, rowToReadCountMap.getOrDefault(repeatedRow, 2));
        mockYtTable(List.of(
                row(465232, "1076314", "200000", "300000", "400000", "5600000"),
                row(465232, "abc-1", "100000", "200000", "300000", "4500000"),
                row(465232, "abc-2", "110000", "210000", "310000", "5500000"),
                row(475443, "aaa", "1100000", "2100000", "3100000", "50500000"),
                row(475443, "bbb", "1200000", "2200000", "3200000", "5800000"),
                row(475444, "a1", "4200000", "71000000", "34000000", "38000000"),
                row(475444, "a2", "4200000", "71000000", "39000000", "41000000"),
                row(475444, "a3", "4500000", "71000000", "39000000", "42000000"),
                row(475445, "a3", "45000000", "7800000", "3400000", "12000000")
        ), repeatedRowsConsumer);
        command.executeCommand(createCommandInvocation(), new TestTerminal());
        assertThat(rowToReadCountMap).describedAs("номера строк -> количество чтений").isEmpty();
    }

    private CommandInvocation createCommandInvocation() {
        return createCommandInvocation(null);
    }

    private CommandInvocation createCommandInvocation(@Nullable Integer startRow) {
        Map<String, String> options = new HashMap<>();
        options.put(OPTION_CLUSTER, OPTION_VALUE_CLUSTER);
        options.put(OPTION_TABLE, OPTION_VALUE_TABLE);
        options.put(OPTION_THREADS, OPTION_VALUE_THREADS);
        options.put(OPTION_CHUNK, OPTION_VALUE_CHUNK);
        if (startRow != null) {
            options.put(OPTION_START_ROW, String.valueOf(startRow));
        }
        return new CommandInvocation("", new String[0], options);
    }

    @SuppressWarnings("unchecked")
    private void mockYtTable(List<JsonNode> ytRowsList) {
        mockYtTable(ytRowsList, null);
    }

    private void mockYtTable(List<JsonNode> ytRowsList, @Nullable Consumer<Integer> repeatedRowsConsumer) {
        Set<Integer> readRows = new HashSet<>();
        doAnswer(invocation -> {
            YPath yPath = invocation.getArgument(0);
            Range range = (Range) yPath.getRanges().get(0);
            int from = (int) range.lower.rowIndex;
            int to = (int) range.upper.rowIndex;

            if (repeatedRowsConsumer != null) {
                synchronized (this) {
                    Set<Integer> currentRows = IntStreamEx.range(from, to).boxed().toSet();
                    Set<Integer> repeatedRows = Sets.intersection(readRows, currentRows);
                    repeatedRows.forEach(repeatedRowsConsumer);
                    readRows.addAll(currentRows);
                }
            }

            if (from >= ytRowsList.size()) {
                return null;
            }

            Consumer<JsonNode> consumer = invocation.getArgument(2);
            ytRowsList.subList(from, Math.min(to, ytRowsList.size())).forEach(consumer);
            return null;
        }).when(ytTables).read(any(), any(), (Consumer<JsonNode>) any());
    }

    private JsonNode row(long supplierId, String shopSku, String length, String width, String height, String weight) {
        ObjectNode row = new ObjectNode(JsonNodeFactory.instance);
        row.set("supplier_id", new LongNode(supplierId));
        row.set("shop_sku", new TextNode(shopSku));
        row.set("lengthMicrometer", new TextNode(length));
        row.set("widthMicrometer", new TextNode(width));
        row.set("heightMicrometer", new TextNode(height));
        row.set("weightGrossMg", new TextNode(weight));
        return row;
    }
}
