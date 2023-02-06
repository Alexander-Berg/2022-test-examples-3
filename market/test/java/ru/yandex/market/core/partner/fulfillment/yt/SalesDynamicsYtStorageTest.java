package ru.yandex.market.core.partner.fulfillment.yt;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.core.yt.YtTablesMockUtils;
import ru.yandex.market.mbi.yt.YtUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Юнит тесты на {@link SalesDynamicsYtStorage}
 */
class SalesDynamicsYtStorageTest {

    private Cypress cypress;
    private SalesDynamicsYtStorage salesDynamicsYtStorage;

    @BeforeEach
    void setUp() {
        Yt yt = mock(Yt.class);
        cypress = mock(Cypress.class);
        when(yt.cypress()).thenReturn(cypress);

        salesDynamicsYtStorage = new SalesDynamicsYtStorage(yt, "//salesDynamicsYtPath", "//salesDynamicsYtPathLatest");
    }

    @Test
    void testGetAllAvailableSalesDynamicsData() {
        Map<String, YTreeNode> files = Cf.hashMap();
        files.put("result_total_3P_2021-07-15", YtTablesMockUtils.buildYtReportTableAttributes(true, 23));
        files.put("result_total_3P_2021-02-02", YtTablesMockUtils.buildYtReportTableAttributes(true, 2));
        files.put("result_total_3P_2021-02-03", YtTablesMockUtils.buildYtReportTableAttributes(false, 22));
        files.put("result_total_3P_2021-02-04", YtTablesMockUtils.buildYtReportTableAttributes(true, 0));
        files.put("result_total_3P_2021-02-05-bad-date", YtTablesMockUtils.buildYtReportTableAttributes(true, 33));
        files.put("result_total_3P_some-user-data-2021-02-05", YtTablesMockUtils.buildYtReportTableAttributes(true,
                33));
        files.put("result_total_3P_2021-02-06", YtTablesMockUtils.buildYtReportTableAttributes(null, 11));
        files.put("result_total_3P_2021-02-07", YtTablesMockUtils.buildYtReportTableAttributes(true, null));
        files.put("result_total_3P_2021-02-08", YtTablesMockUtils.buildYtReportTableAttributes(null, null));

        YTreeMapNode entries = YtUtil.treeMapNode(files);
        when(cypress.get(any(), any())).thenReturn(entries);

        List<LocalDate> result = salesDynamicsYtStorage.findAllAvailableReportDates();
        assertThat(result)
                .isSorted()
                .containsExactly(
                        LocalDate.parse("2021-02-01"),
                        LocalDate.parse("2021-07-14")
                );
    }
}
