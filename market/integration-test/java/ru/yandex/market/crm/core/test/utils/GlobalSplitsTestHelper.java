package ru.yandex.market.crm.core.test.utils;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.crm.core.domain.Color;
import ru.yandex.market.crm.core.yt.paths.CrmYtTables;
import ru.yandex.market.crm.util.Futures;
import ru.yandex.market.crm.yt.client.YtClient;

import static ru.yandex.market.crm.core.test.utils.UserTestHelper.CRYPTA_ID;
import static ru.yandex.market.crm.core.test.utils.UserTestHelper.EMAIL_MD5;

/**
 * @author apershukov
 */
@Component
public class GlobalSplitsTestHelper {

    public static YTreeMapNode splitEntry(String id, Color color, String cryptaId, boolean inTarget) {
        LocalDate splittingDate = LocalDate.now().minusMonths(1);
        LocalDate reportMonth = LocalDate.of(splittingDate.getYear(), splittingDate.getMonth(), 1);

        return YTree.mapBuilder()
                .key("id").value(id)
                .key("in_target").value(inTarget)
                .key("initial").value(true)
                .key("color").value(color.name())
                .key("crypta_id").value(cryptaId)
                .key("salt").value("123456")
                .key("control_percent").value(7)
                .key("bucket").value(0)
                .key("channel_overlap_flag").value(0)
                .key("splitting_date").value(splittingDate.toString())
                .key("report_month").value(reportMonth.toString())
                .buildMap();
    }

    public static YTreeMapNode uniformSplitEntry(String cryptaId, boolean inTarget) {
        LocalDate splittingDate = LocalDate.now().minusMonths(1);
        LocalDate reportMonth = LocalDate.of(splittingDate.getYear(), splittingDate.getMonth(), 1);

        return YTree.mapBuilder()
                .key("crypta_id").value(cryptaId)
                .key("in_target").value(inTarget)
                .key("initial").value(true)
                .key("salt").value("123456")
                .key("control_percent").value(7)
                .key("bucket").value(0)
                .key("splitting_date").value(splittingDate.toString())
                .key("report_month").value(reportMonth.toString())
                .buildMap();
    }

    public static YTreeMapNode splitEntry(String id, String cryptaId, boolean inTarget) {
        return splitEntry(id, Color.GREEN, cryptaId, inTarget);
    }

    public static YTreeMapNode splitEntry(String id, boolean inTarget) {
        return splitEntry(id, UUID.randomUUID().toString(), inTarget);
    }

    public static YTreeMapNode splitEntry(String id, Color color, boolean inTarget) {
        return splitEntry(id, color, UUID.randomUUID().toString(), inTarget);
    }

    public static YTreeMapNode cryptaIdEntry(long cryptaId, Color color, boolean inTarget) {
        return YTree.mapBuilder()
                .key("crypta_id").value(String.valueOf(cryptaId))
                .key("color").value(color.name())
                .key("in_target").value(inTarget)
                .buildMap();
    }

    public static YTreeMapNode cryptaMatchingEntry(String id, String idType, String cryptaId) {
        String normalizedId = EMAIL_MD5.equals(idType) ? emailToMD5(id) : id;

        return YTree.mapBuilder()
                .key("id").value(normalizedId)
                .key("id_type").value(idType)
                .key("target_id").value(cryptaId)
                .key("target_id_type").value(CRYPTA_ID)
                .buildMap();
    }

    private final YtClient ytClient;
    private final CrmYtTables ytTables;
    private final YtTestTables ytTestTables;

    public GlobalSplitsTestHelper(YtClient ytClient,
                                  CrmYtTables ytTables,
                                  YtTestTables ytTestTables) {
        this.ytClient = ytClient;
        this.ytTables = ytTables;
        this.ytTestTables = ytTestTables;
    }

    public void prepareGlobalControlSplits(YTreeMapNode... rows) {
        YPath globalSplitsTable = ytTables.getCurrentGlobalSplitsTable();
        // Пересоздаём таблицу сплитов ГК для вставки строк в статическую таблицу, чтобы данные быстрее доехали
        if (ytClient.exists(globalSplitsTable)) {
            ytClient.remove(globalSplitsTable);
        }
        ytClient.createTable(globalSplitsTable, "global_control/uniform_global_splits_table.yson");
        ytClient.write(globalSplitsTable, List.of(rows));
        ytClient.makeDynamic(globalSplitsTable);
    }

    public List<YTreeMapNode> getGlobalSplitsRows() {
        String query = String.format("* FROM [%s]", ytTables.getCurrentGlobalSplitsTable());
        return Futures.joinWait1M(ytClient.selectRowsAsync(query));
    }

    public void prepareCryptaMatchingEntries(String idType, YTreeMapNode... rows) {
        List<YTreeMapNode> entries = Arrays.stream(rows)
                .sorted(
                        Comparator.<YTreeMapNode, String>comparing(row -> row.getString("id"))
                                .thenComparing(row -> row.getString("id_type"))
                )
                .collect(Collectors.toList());

        YPath tablePath = ytTestTables.getCryptaMatchingDir().child(idType).child(CRYPTA_ID);
        ytClient.write(tablePath, YTableEntryTypes.YSON, entries);
    }

    private static String emailToMD5(String email) {
        var parts = email.split("@");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Incorrect email format");
        }
        return DigestUtils.md5Hex(parts[0].replace('.', '-') + "@" + parts[1]);
    }
}
