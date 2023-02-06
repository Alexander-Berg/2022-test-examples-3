package ru.yandex.market.core.partner;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.util.yt.YtCluster;
import ru.yandex.market.billing.util.yt.YtUtilTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

public class PartnerProgramTypeImportServiceTest extends FunctionalTest {
    private static final Object NULL = new Object();
    private static final String HAHN = "hahn.yt.yandex.net";
    private static final Function<Object, YTreeNode> LONG_NODE =
            v -> v == NULL ? YtUtilTest.nullNode() : YtUtilTest.longNode(((Integer) (v)).longValue());
    private static final Function<Object, YTreeNode> STRING_NODE = v -> YtUtilTest.stringNode((String) v);
    private static final Function<Object, YTreeNode> BOOLEAN_NODE = v -> YtUtilTest.booleanNode((Boolean) v);
    private static final Map<String, Function<Object, YTreeNode>> PARTNER_PROGRAM_TYPE_YT_FIELDS = Map.of(
            "partner_id", LONG_NODE,
            "program", STRING_NODE,
            "update_at", STRING_NODE,
            "created_at", STRING_NODE,
            "status", STRING_NODE,
            "ever_activated", BOOLEAN_NODE
    );
    @Autowired
    private PartnerProgramTypeImportService partnerProgramTypeImportService;

    private YtCluster ytCluster;

    private static List<YTreeMapNode> toYtNodes(List<Map<String, Object>> orderReturns) {
        return orderReturns.stream()
                .map(orderReturnItem -> YtUtilTest.treeMapNode(
                                PARTNER_PROGRAM_TYPE_YT_FIELDS.entrySet().stream()
                                        .filter(entry -> orderReturnItem.containsKey(entry.getKey()))
                                        .collect(Collectors.toMap(Map.Entry::getKey, entry ->
                                                        entry.getValue().apply(orderReturnItem.get(entry.getKey()))
                                                )
                                        )
                        )
                ).collect(Collectors.toList());
    }

    void setYtData(List<YTreeMapNode> ytData) {
        final Yt hahn = YtUtilTest.mockYt(ytData);
        ytCluster = new YtCluster(HAHN, hahn);
    }

    private List<YTreeMapNode> getYtPartnerProgramTypeData() {
        return toYtNodes(List.of(
                Map.of(
                        "partner_id", 5040418,
                        "program", "DROPSHIP_BY_SELLER",
                        "update_at", "2022-03-14",
                        "created_at", "2022-03-09",
                        "status", "FAIL",
                        "ever_activated", false
                ),
                Map.of(
                        "partner_id", 5040889,
                        "program", "DROPSHIP",
                        "update_at", "2022-03-09",
                        "created_at", "2022-03-09",
                        "status", "CONFIGURE",
                        "ever_activated", false
                ),
                Map.of(
                        "partner_id", 5046121,
                        "program", "FULFILLMENT",
                        "update_at", "2022-03-10",
                        "created_at", "2022-03-10",
                        "status", "SUCCESS",
                        "ever_activated", true
                ),
                Map.of(
                        "partner_id", 4868830,
                        "program", "CPC",
                        "update_at", "2022-03-11",
                        "created_at", "2022-03-02",
                        "status", "SUCCESS",
                        "ever_activated", true
                )
        ));
    }

    @Test
    @DbUnitDataSet(after = "PartnerProgramTypeImportServiceTest.common.after.csv")
    void testPartnerProgramTypeImport() {
        setYtData(getYtPartnerProgramTypeData());

        partnerProgramTypeImportService.runAtCluster(ytCluster);
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerProgramTypeImportServiceTest.testPartnerProgramTypeUpdate.before.csv",
            after = "PartnerProgramTypeImportServiceTest.common.after.csv"
    )
    void testPartnerProgramTypeUpdate() {
        setYtData(getYtPartnerProgramTypeData());

        partnerProgramTypeImportService.runAtCluster(ytCluster);
    }
}
