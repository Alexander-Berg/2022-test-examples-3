package ru.yandex.market.billing.factoring.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.util.yt.YtCluster;
import ru.yandex.market.billing.util.yt.YtUtilTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

public class FactorRejectYtImportServiceTest extends FunctionalTest {
    private static final String HAHN = "hahn.yt.yandex.net";
    private static final Function<Object, YTreeNode> STRING_NODE = YtUtilTest::stringNode;
    private static final Map<String, Function<Object, YTreeNode>> REJECT_FACTOR_RESULT_YT_FIELDS = Map.of(
            "name", STRING_NODE,
            "inn", STRING_NODE,
            "ogrn", STRING_NODE,
            "status", STRING_NODE,
            "registry_date", STRING_NODE
    );

    @Value("${market.billing.factoring.factor_reject_alfa_result.yt.path}")
    private String factorRejectAlfaYtPath;

    @Value("${market.billing.factoring.factor_reject_sber_result.yt.path}")
    private String factorRejectSberYtPath;

    @Autowired
    private FactorRejectYtImportService factorRejectAlfaYtImportService;

    @Autowired
    private FactorRejectYtImportService factorRejectSberYtImportService;


    private YtCluster ytCluster;

    private static List<YTreeMapNode> toYtNodes(List<Map<String, Object>> factorRejects) {
        return factorRejects.stream()
                .map(factorReject -> YtUtilTest.treeMapNode(
                                REJECT_FACTOR_RESULT_YT_FIELDS.entrySet().stream()
                                        .filter(entry -> factorReject.containsKey(entry.getKey()))
                                        .collect(Collectors.toMap(
                                                        Map.Entry::getKey,
                                                        entry -> entry.getValue().apply(
                                                                factorReject.get(entry.getKey())
                                                        )
                                                )
                                        )
                        )
                ).collect(Collectors.toList());
    }

    @DbUnitDataSet(
            before = "FactorRejectAlfaYtImportServiceTest.testImport.before.csv",
            after = "FactorRejectAlfaYtImportServiceTest.testImport.after.csv"
    )
    @DisplayName("Проверяем, что импортируются и обновляются данные для Альфа-Банк")
    @Test
    void testImportFactorRejectAlfaResult() {
        setYtData(getYtFactorRejectAlfaResult());
        factorRejectAlfaYtImportService.runImport(
                ytCluster,
                YPath.simple(factorRejectAlfaYtPath)
        );
    }


    @DbUnitDataSet(
            before = "FactorRejectSberYtImportServiceTest.testImport.before.csv",
            after = "FactorRejectSberYtImportServiceTest.testImport.after.csv"
    )
    @DisplayName("Проверяем, что импортируются и обновляются данные для Сбербанка")
    @Test
    void testImportFactorRejectSberResult() {
        setYtData(getYtFactorRejectSberResult());
        factorRejectSberYtImportService.runImport(
                ytCluster,
                YPath.simple(factorRejectSberYtPath)
        );
    }

    private List<YTreeMapNode> getYtFactorRejectSberResult() {
        return toYtNodes(List.of(
                        Map.of(
                                "name", "Billing-3",
                                "inn", "11111111113",
                                "ogrn", "9876666666663",
                                "status", "rejected",
                                "registry_date", "2022-04-24"
                        ),
                        Map.of(
                                "name", "Billing-6",
                                "inn", "11111111116",
                                "ogrn", "9876666666667",
                                "status", "approved",
                                "registry_date", "2022-04-24"
                        )
                )
        );
    }

    private List<YTreeMapNode> getYtFactorRejectAlfaResult() {
        return toYtNodes(List.of(
                        Map.of(
                                "name", "Billing-1",
                                "inn", "11111111111",
                                "ogrn", "9876666666661",
                                "status", "sent_to_check",
                                "registry_date", "2022-04-21"
                        ),
                        Map.of(
                                "name", "Billing-5",
                                "inn", "11111111115",
                                "ogrn", "9876666666665",
                                "status", "approved",
                                "registry_date", "2022-04-21"
                        ),
                        Map.of(
                                "name", "Billing-6",
                                "inn", "11111111116",
                                "ogrn", "9876666666667",
                                "status", "rejected",
                                "registry_date", "2022-04-21"
                        )
                )
        );
    }

    private void setYtData(List<YTreeMapNode> ytData) {
        final Yt hahn = YtUtilTest.mockYt(ytData);
        ytCluster = new YtCluster(HAHN, hahn);
    }
}
