package ru.yandex.market.mbo.tms.report;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.tms.report.OperatorResourcesReport.DbUnit;
import ru.yandex.market.mbo.tms.report.OperatorResourcesReport.ReportUnit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:MagicNumber")
public class OperatorResourcesReportTest {

    private static final int CREATE_SKU = 123;

    private static final int PICCOPY_SKU = 131;
    private static final int ADD_VENDOR_FRMLZ = 80;
    private static final int INHERIT_VENDOR_FRMLZ = 79;
    private static final int VENDOR_ALIAS_LOGS = 12;
    private static final int ANY_JUNK_LOGS = 9;
    private static final int SHOP_ERR_CLSSFR = 89;
    private static final int UNKNOWN_ACTION = 100500;

    private static final long BLUE_SKU_ID = 303030303L;
    private static final long SOME_SRC_ID = 101010101L;

    private BillingActionColourMock billingActionColour = new BillingActionColourMock();
    private OperatorResourcesReport report = new OperatorResourcesReport();
    private Map<String, Long> generatedStaffIds;

    @Before
    public void setup() {
        generatedStaffIds = new HashMap<>();
        report.setBillingActionColour(billingActionColour);
        billingActionColour.addBlueIds(Collections.singletonList(BLUE_SKU_ID));
    }

    @Test
    public void testSummarizeNoBillingData() {
        List<ReportUnit> data = report.summarizeReportData(Collections.emptyList());
        assertThat(data).isEmpty();
    }

    @Test
    public void testSummarizeOneRowPerUser() {
        List<DbUnit> dataFromBilling = Arrays.asList(
            imitateDbRow("Вася", CREATE_SKU, 13.0, 2, BLUE_SKU_ID),
            imitateDbRow("Петя", ANY_JUNK_LOGS, 4, 1, BLUE_SKU_ID),
            imitateDbRow("Таня", CREATE_SKU, 6.5, 1, SOME_SRC_ID)
        );
        List<ReportUnit> data = report.summarizeReportData(dataFromBilling);
        assertThat(data).containsExactlyInAnyOrder(
            reportRow("Вася", CREATE_SKU, 2, 0, 13.0, 0, 13.0),
            totalRow("Вася", CREATE_SKU, 2, 0, 13.0, 0, 13.0),
            overallRow("Вася", 2, 0, 13.0, 0, 13.0),

            reportRow("Петя", ANY_JUNK_LOGS, 0, 1, 0, 4.0, 4.0),
            totalRow("Петя", ANY_JUNK_LOGS, 0, 1, 0, 4.0, 4.0),
            overallRow("Петя", 0, 1, 0, 4.0, 4.0),

            reportRow("Таня", CREATE_SKU, 0, 1, 0, 6.5, 6.5),
            totalRow("Таня", CREATE_SKU, 0, 1, 0, 6.5, 6.5),
            overallRow("Таня", 0, 1, 0, 6.5, 6.5)
        );
    }

    @Test
    public void testSummarizeSeveralActionsOfSameClass() {
        List<DbUnit> dataFromBilling = Arrays.asList(
            imitateDbRow("Вася", CREATE_SKU, 13.0, 2, BLUE_SKU_ID),
            imitateDbRow("Вася", CREATE_SKU, 6.5, 1, SOME_SRC_ID),
            imitateDbRow("Вася", PICCOPY_SKU, 33.0, 3, BLUE_SKU_ID),

            imitateDbRow("Петя", ADD_VENDOR_FRMLZ, 0.5, 1, SOME_SRC_ID),
            imitateDbRow("Петя", INHERIT_VENDOR_FRMLZ, 29.5, 1, SOME_SRC_ID)
        );
        List<ReportUnit> data = report.summarizeReportData(dataFromBilling);

        //В totalRow в качестве операции передаём любую из вышезаюзанных, он по ней просто определит класс действий.
        assertThat(data).containsExactlyInAnyOrder(
            reportRow("Вася", CREATE_SKU, 2, 1, 13.0, 6.5, 52.5),
            reportRow("Вася", PICCOPY_SKU, 3, 0, 33.0, 0, 52.5),
            totalRow("Вася", CREATE_SKU, 5, 1, 46.0, 6.5, 52.5),
            overallRow("Вася", 5, 1, 46.0, 6.5, 52.5),

            reportRow("Петя", ADD_VENDOR_FRMLZ, 0, 1, 0, 0.5, 30.0),
            reportRow("Петя", INHERIT_VENDOR_FRMLZ, 0, 1, 0, 29.5, 30.0),
            totalRow("Петя", ADD_VENDOR_FRMLZ, 0, 2, 0, 30.0, 30.0),
            overallRow("Петя", 0, 2, 0, 30.0, 30.0)
        );
    }

    @Test
    public void testSummarizeSeveralActionsDifferentClasses() {
        List<DbUnit> dataFromBilling = Arrays.asList(
            imitateDbRow("Вася", CREATE_SKU, 13.0, 2, BLUE_SKU_ID),
            imitateDbRow("Вася", CREATE_SKU, 6.5, 1, SOME_SRC_ID),
            imitateDbRow("Вася", PICCOPY_SKU, 33.0, 3, BLUE_SKU_ID),
            imitateDbRow("Вася", VENDOR_ALIAS_LOGS, 7.0, 1, SOME_SRC_ID),
            imitateDbRow("Вася", ANY_JUNK_LOGS, 8.0, 2, SOME_SRC_ID),

            imitateDbRow("Петя", ADD_VENDOR_FRMLZ, 0.5, 1, SOME_SRC_ID),
            imitateDbRow("Петя", INHERIT_VENDOR_FRMLZ, 29.5, 1, SOME_SRC_ID)
        );
        List<ReportUnit> data = report.summarizeReportData(dataFromBilling);
        assertThat(data).containsExactlyInAnyOrder(
            reportRow("Вася", CREATE_SKU, 2, 1, 13.0, 6.5, 52.5 + 15),
            reportRow("Вася", PICCOPY_SKU, 3, 0, 33.0, 0, 52.5 + 15),
            totalRow("Вася", CREATE_SKU, 5, 1, 46.0, 6.5, 52.5 + 15),

            reportRow("Вася", VENDOR_ALIAS_LOGS, 0, 1, 0, 7, 15 + 52.5),
            reportRow("Вася", ANY_JUNK_LOGS, 0, 2, 0, 8, 15 + 52.5),
            totalRow("Вася", VENDOR_ALIAS_LOGS, 0, 3, 0, 15, 15 + 52.5),
            overallRow("Вася", 5, 4, 46.0, 21.5, 67.5),

            reportRow("Петя", ADD_VENDOR_FRMLZ, 0, 1, 0, 0.5, 30.0),
            reportRow("Петя", INHERIT_VENDOR_FRMLZ, 0, 1, 0, 29.5, 30.0),
            totalRow("Петя", ADD_VENDOR_FRMLZ, 0, 2, 0, 30.0, 30.0),
            overallRow("Петя", 0, 2, 0, 30.0, 30.0)
        );
    }

    @Test
    public void testAlternatingActionClass() {
        List<DbUnit> dataFromBilling = Arrays.asList(
            // Эти должны автосквошнуться и впоследствии разделиться 50/50 по цветам.
            imitateDbRow("Вася", SHOP_ERR_CLSSFR, 1.0, 1, SOME_SRC_ID),
            imitateDbRow("Вася", SHOP_ERR_CLSSFR, 1.0, 1, SOME_SRC_ID),
            imitateDbRow("Вася", SHOP_ERR_CLSSFR, 1.0, 1, SOME_SRC_ID),
            imitateDbRow("Вася", SHOP_ERR_CLSSFR, 3.0, 3, SOME_SRC_ID),

            // Эти не должны сквошиться до цветовой дифференциации и посчитаются отдельно.
            imitateDbRow("Вася", CREATE_SKU, 6.0, 1, BLUE_SKU_ID),
            imitateDbRow("Вася", CREATE_SKU, 8.0, 1, SOME_SRC_ID)
        );
        List<ReportUnit> data = report.summarizeReportData(dataFromBilling);
        assertThat(data).containsExactlyInAnyOrder(
            reportRow("Вася", SHOP_ERR_CLSSFR, 3, 3, 3.0, 3.0, 20.0),
            totalRow("Вася", SHOP_ERR_CLSSFR, 3, 3, 3.0, 3.0, 20.0),

            reportRow("Вася", CREATE_SKU, 1, 1, 6.0, 8.0, 20.0),
            totalRow("Вася", CREATE_SKU, 1, 1, 6.0, 8.0, 20.0),

            overallRow("Вася", 4, 4, 9.0, 11.0, 20.0)
        );
    }

    @Test
    public void testUnknownActionAlternates() {
        List<DbUnit> dataFromBilling = Arrays.asList(
            imitateDbRow("Вася", UNKNOWN_ACTION, 5.0, 1, BLUE_SKU_ID),
            imitateDbRow("Вася", UNKNOWN_ACTION, 10.0, 2, SOME_SRC_ID),
            imitateDbRow("Вася", UNKNOWN_ACTION, 15.0, 3, BLUE_SKU_ID)
        );
        List<ReportUnit> data = report.summarizeReportData(dataFromBilling);

        //В totalRow в качестве операции передаём любую из вышезаюзанных, он по ней просто определит класс действий.
        assertThat(data).containsExactlyInAnyOrder(
            reportRow("Вася", UNKNOWN_ACTION, 3, 3, 15.0, 15.0, 30.0),
            totalRow("Вася", UNKNOWN_ACTION, 3, 3, 15.0, 15.0, 30.0),
            overallRow("Вася", 3, 3, 15.0, 15.0, 30.0)
        );
    }

    private DbUnit imitateDbRow(String staffLogin,
                                int operationId,
                                double priceDouble,
                                int count,
                                long sourceId) {
        DbUnit row = new DbUnit();
        row.staffId = getOrGenerateStaffId(staffLogin);
        row.staffLogin = staffLogin;
        row.operationId = operationId;
        row.operationName = String.valueOf(operationId);
        row.price = price(priceDouble);
        row.sourceId = sourceId;
        row.count = count;
        row.login = "";
        return row;
    }

    private ReportUnit reportRow(String staffLogin,
                                 int operationId,
                                 int blueCount,
                                 int whiteCount,
                                 double blueMoney,
                                 double whiteMoney,
                                 double total) {
        ReportUnit row = new ReportUnit();
        row.staffId = getOrGenerateStaffId(staffLogin);
        row.operatorStaffLogin = staffLogin;
        row.action = String.valueOf(operationId);
        row.actionClass = OperatorResourcesReport.ACTION_CLASSES
            .getOrDefault(operationId, OperatorResourcesReport.OTHER);
        row.operatorMboLogin = "";
        row.blueCount = blueCount;
        row.whiteCount = whiteCount;
        row.blueMoney = price(blueMoney);
        row.whiteMoney = price(whiteMoney);
        BigDecimal overall = price(total);
        row.bluePercent = row.blueMoney.divide(overall, 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100L));
        row.whitePercent = row.whiteMoney.divide(overall, 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100L));
        return row;
    }

    private ReportUnit totalRow(String staffLogin,
                                int operationId,
                                int blueCount,
                                int whiteCount,
                                double blueMoney,
                                double whiteMoney,
                                double total) {
        ReportUnit row = reportRow(staffLogin, operationId, blueCount, whiteCount, blueMoney, whiteMoney, total);
        row.action = OperatorResourcesReport.TOTAL;
        return row;
    }

    private ReportUnit overallRow(String staffLogin,
                                  int blueCount,
                                  int whiteCount,
                                  double blueMoney,
                                  double whiteMoney,
                                  double total) {
        ReportUnit row = reportRow(staffLogin, -1, blueCount, whiteCount, blueMoney, whiteMoney, total);
        row.action = "-";
        row.actionClass = OperatorResourcesReport.OVERALL;
        return row;
    }

    private BigDecimal price(double value) {
        return BigDecimal.valueOf(value);
    }

    private long getOrGenerateStaffId(String login) {
        if (generatedStaffIds.isEmpty()) {
            generatedStaffIds.put(login, 0L);
            return 0L;
        }

        if (generatedStaffIds.containsKey(login)) {
            return generatedStaffIds.get(login);
        }
        long newId = Collections.max(generatedStaffIds.values()) + 1L;
        generatedStaffIds.put(login, newId);
        return newId;
    }
}
