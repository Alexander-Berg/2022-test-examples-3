package ru.yandex.chemodan.app.psbilling.core.billing.distributionplatform;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Iterator;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.SneakyThrows;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.chemodan.app.psbilling.core.PsBillingTransactionsFactory;
import ru.yandex.chemodan.app.psbilling.core.billing.groups.export.distributionplatform.DistributionPlatformTransactionsExportService;
import ru.yandex.chemodan.app.psbilling.core.config.YtExportSettings;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.DistributionPlatformCalculationDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.DistributionPlatformTransactionsDao;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.CalculationStatus;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.DistributionServiceTransactionCalculation;
import ru.yandex.chemodan.app.psbilling.core.utils.AssertHelper;
import ru.yandex.commune.dynproperties.DynamicProperty;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.ytree.YTreeStringNode;

public class DistributionPlatformTransactionsExportServiceTest extends BaseDistributionPlatformTest {
    private LocalDate calcMonth;
    private Group group1;
    private Group group2;
    @Value("${distribution_platform.product_id}")
    private String DpProductId;
    @Autowired
    private DistributionPlatformTransactionsExportService distributionPlatformTransactionsExportService;
    @Autowired
    private DistributionPlatformCalculationDao distributionPlatformCalculationDao;
    @Autowired
    private DistributionPlatformTransactionsDao distributionPlatformTransactionsDao;
    @Autowired
    @Qualifier("distributionPlatformPrimaryExportSettings")
    private YtExportSettings dpPrimarySettings;

    @Autowired
    @Qualifier("distributionPlatformSecondaryExportSettings")
    private YtExportSettings dpSecondarySettings;

    @Before
    public void init() {
        bazingaTaskManagerStub.suppressNewTasksAdd();
        group1 = createGroup("clid1", 1L, Cf.list(
                new PsBillingTransactionsFactory.Service("code1").createdAt(now.minusMonths(2))), 1);
        group2 = createGroup("clid2", 2L, Cf.list(
                new PsBillingTransactionsFactory.Service("code2").createdAt(now.minusMonths(2))), 1);

        calcMonth = now.minusMonths(1);
        insertCalculation(group1, calcMonth, true); // 2021-06
        insertCalculation(group2, calcMonth.minusMonths(1), true); // 2021-05

        Mockito.reset(dpPrimarySettings.getYt());
        Mockito.reset(dpSecondarySettings.getYt());
    }

    @Test
    // got paid acts for 6/2021 and 5/2021 at 15/7/2021
    // so got data for clids for calc month = 6/2021 and 5/2021
    // so should export data for 6/2021 to folder 1/7/2021
    // and export data for 5/2021 to folder 1/6/2021
    public void export() {
        distributionPlatformTransactionsExportService.setExportMonthsCount(new DynamicProperty<Integer>("", 2));
        distributionPlatformTransactionsExportService.export();
        String exportPath = "//home/disk-ps-billing/distribution_platform/development/2021-07/Export_2021-07-15_00";
        String currentPath = "//home/disk-ps-billing/distribution_platform/development/current";

        // expect 2 months in export  2021-07, 2021-06
        assertMonthFilled(now, exportPath, true);
        assertMonthFilled(now.minusMonths(1), exportPath, true);

        verifyLinkCreated(exportPath, currentPath, dpPrimarySettings);
        verifyLinkCreated(exportPath, currentPath, dpSecondarySettings);
    }


    @Test
    public void export_monthsCount() {
        int monthsToExport = 5;
        distributionPlatformTransactionsExportService.setExportMonthsCount(new DynamicProperty<>("",
                monthsToExport));
        distributionPlatformTransactionsExportService.export();
        String exportPath = "//home/disk-ps-billing/distribution_platform/development/2021-07/Export_2021-07-15_00";
        String currentPath = "//home/disk-ps-billing/distribution_platform/development/current";

        for (int i = 0; i < monthsToExport; i++) {
            assertMonthFilled(now.minusMonths(i), exportPath, i <= 1/* got data only for first 2 months*/);
        }

        verifyLinkCreated(exportPath, currentPath, dpPrimarySettings);
        verifyLinkCreated(exportPath, currentPath, dpSecondarySettings);
    }

    private void verifyLinkCreated(String exportPath, String currentPath, YtExportSettings setting) {
        Mockito.verify(setting.getYt().cypress(), Mockito.times(1))
                .link(Mockito.eq(YPath.simple(exportPath)),
                        Mockito.eq(YPath.simple(currentPath)),
                        Mockito.eq(true));
    }

    private boolean verifyData(Iterator<Object> data, LocalDate billingDate) {
        ListF<JsonNode> nodes = Cf.arrayList();
        data.forEachRemaining(x -> nodes.add((JsonNode) x));
        AssertHelper.assertSize(nodes, 1);
        JsonNode node = nodes.single();
        return node.get("billing_period").asText().equals(billingDate.toString("YYYY-MM-dd"))
                && node.get("clid").asText().equals("clid")
                && node.get("currency").asText().equals("RUB")
                && node.get("money").asText().equals("500.1")
                && node.get("product_id").asText().equals(DpProductId)
                && node.get("count").asText().equals("1");
    }

    private void assertMonthFilled(LocalDate month, String exportPath, boolean gotData) {
        // assert months are filled with empty tables CHEMODAN-82951
        for (int day = 1; day < month.dayOfMonth().getMaximumValue(); day++) {
            // data stored only on 1st day of month
            boolean verifyData = day == 1 && gotData;
            validateDateCreated(dpPrimarySettings, exportPath, month.withDayOfMonth(day), verifyData);
            validateDateCreated(dpSecondarySettings, exportPath, month.withDayOfMonth(day), verifyData);
        }
    }

    @Test
    public void exportNotCompleteCalculation() {
        distributionPlatformCalculationDao.setCalculationObsolete(calcMonth);

        insertCalculation(group1, calcMonth, true);
        insertCalculation(group2, calcMonth, false);

        distributionPlatformTransactionsExportService.export();
        Mockito.verify(dpSecondarySettings.getYt().cypress(), Mockito.never()).link(Mockito.any(), Mockito.any());
    }

    @Test
    @SneakyThrows
    public void testRemoveOldExports() {
        // folders order is random
        mockList(dpPrimarySettings, dpPrimarySettings.getRootExportPath(), "2021-10", "2021-12", "2021-11");
        mockExportList(dpPrimarySettings, dpPrimarySettings.getRootExportPath(), new LocalDate(2021, 10, 1),
                "Export_2021-10-29_01",
                "Export_2021-10-31_11",
                "Export_2021-10-31_10",
                "Export_2021-10-31_09",
                "Export_2021-10-30_01");
        // last export is not valid
        setExportCompleted(dpPrimarySettings, dpPrimarySettings.getRootExportPath().child("2021-10").child(
                "Export_2021-10-31_11"), false);

        mockExportList(dpPrimarySettings, dpPrimarySettings.getRootExportPath(), new LocalDate(2021, 11, 1),
                "Export_2021-11-19_01",
                "Export_2021-11-21_02",
                "Export_2021-11-21_01",
                "Export_2021-11-21_03",
                "Export_2021-11-20_01");

        mockExportList(dpPrimarySettings, dpPrimarySettings.getRootExportPath(), new LocalDate(2021, 12, 1),
                "Export_2021-12-31_02",
                "Export_2021-12-31_01");

        distributionPlatformTransactionsExportService.removeOldExports();
        assertDeleted(dpPrimarySettings, "2021-10", "Export_2021-10-29_01");
        assertDeleted(dpPrimarySettings, "2021-10", "Export_2021-10-30_01");
        assertDeleted(dpPrimarySettings, "2021-10", "Export_2021-10-31_09");
        assertDeleted(dpPrimarySettings, "2021-10", "Export_2021-10-31_11");

        assertDeleted(dpPrimarySettings, "2021-11", "Export_2021-11-19_01");
        assertDeleted(dpPrimarySettings, "2021-11", "Export_2021-11-20_01");
        assertDeleted(dpPrimarySettings, "2021-11", "Export_2021-11-21_01");
        assertDeleted(dpPrimarySettings, "2021-11", "Export_2021-11-21_02");

        mockMonthCleared(dpPrimarySettings, new LocalDate(2021, 12, 1), "Export_2021-12-31_01");

        Mockito.verify(dpPrimarySettings.getYt().cypress(), Mockito.times(8 + 30)).remove(Mockito.<YPath>any());

    }

    private void insertCalculation(Group group, LocalDate calcMonth, boolean completed) {
        DistributionServiceTransactionCalculation calculation =
                distributionPlatformCalculationDao.initCalculation(calcMonth, 1L, UUID.randomUUID(), "job");

        DistributionPlatformTransactionsDao.InsertData data = createData(calcMonth, group, x ->
                x.actId("act" + UUID.randomUUID())
                        .amount(BigDecimal.valueOf(500.100))
                        .paidUserCount(BigDecimal.valueOf(100500.123))
                        .clid("clid")
                        .currency(Currency.getInstance("RUR")));
        distributionPlatformTransactionsDao.batchInsert(Cf.list(data));

        if (completed) {
            distributionPlatformCalculationDao.updateStatus(calculation.getTaskId(), CalculationStatus.STARTING,
                    CalculationStatus.COMPLETED);
        }
    }

    private void mockList(YtExportSettings settings, YPath root, String... children) {
        ListF<YTreeStringNode> list = Cf.list(children).map(c -> createNode(settings, root.child(c)));
        Mockito.when(settings.getYt().cypress().list(Mockito.eq(root))).thenReturn(list);
        list.forEach(n -> setExportCompleted(settings, root.child(n.getValue()), true));
    }

    private void mockExportList(YtExportSettings settings, YPath root, LocalDate month, String... children) {
        YPath monthPath = root.child(month.toString("yyyy-MM"));
        ListF<YTreeStringNode> list = Cf.arrayList();
        for (String exportFolder : children) {
            YPath exportPath = monthPath.child(exportFolder);
            YTreeStringNode exportNode = createNode(settings, exportPath);

            list.add(exportNode);
            setExportCompleted(settings, exportPath, true);
            mockMonthFilled(settings, month, exportPath);
        }
        Mockito.when(settings.getYt().cypress().list(Mockito.eq(monthPath))).thenReturn(list);
    }

    private void mockMonthFilled(YtExportSettings settings, LocalDate month, YPath exportFolder) {
        ListF<YTreeStringNode> list = Cf.arrayList();
        for (int day = 1; day <= month.dayOfMonth().getMaximumValue(); day++) {
            YPath datePath = exportFolder.child(month.withDayOfMonth(day).toString());
            list.add(createNode(settings, datePath, day == 1 ? 1 : 0));
        }
        Mockito.when(settings.getYt().cypress().list(Mockito.eq(exportFolder))).thenReturn(list);
    }

    private void mockMonthCleared(YtExportSettings settings, LocalDate month, String exportFolderName) {
        YPath exportFolder = settings.getRootExportPath().child(month.toString("YYYY-MM")).child(exportFolderName);
        for (int day = 1; day <= month.dayOfMonth().getMaximumValue(); day++) {
            YPath tablePath = exportFolder.child(month.withDayOfMonth(day).toString("yyyy-MM-dd"));
            Mockito.verify(settings.getYt().cypress(), Mockito.times(day == 1 ? 0 : 1)).remove(Mockito.eq(tablePath));
        }
    }

    private YTreeStringNode createNode(YtExportSettings settings, YPath path) {
        return createNode(settings, path, 0);
    }

    YTreeStringNode createNode(YtExportSettings settings, YPath path, long rowSize) {
        YTreeStringNode mock = Mockito.mock(YTreeStringNode.class);
        Mockito.when(mock.getValue()).thenReturn(path.name());
        Mockito.when(settings.getYt().getRowCount(path)).thenReturn(rowSize);
        return mock;
    }

    private void assertDeleted(YtExportSettings settings, String month, String export) {
        YPath path = settings.getRootExportPath().child(month).child(export);
        Mockito.verify(settings.getYt().cypress(), Mockito.times(1)).remove(Mockito.eq(path));
    }

    private void assertDeleted(YtExportSettings settings, String month, String export, String table) {
        YPath path = settings.getRootExportPath().child(month).child(export).child(table);
        Mockito.verify(settings.getYt().cypress(), Mockito.times(1)).remove(Mockito.eq(path));
    }

    private void setExportCompleted(YtExportSettings settings, YPath path, boolean value) {
        Mockito.when(settings.getYt().cypress().exists(Mockito.eq(path.attribute("ACTIVE_FROM"))))
                .thenReturn(value);
    }

    private void validateDateCreated(YtExportSettings settings, String currentPath, LocalDate date,
                                     boolean verifyData) {
        YPath path = YPath.simple(currentPath).child(date.toString("YYYY-MM-dd")).append(true);
        Mockito.verify(settings.getYt().cypress(), Mockito.times(1))
                .create(Mockito.any(), Mockito.anyBoolean(), Mockito.eq(path), Mockito.eq(CypressNodeType.TABLE),
                        Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.any());

        if (verifyData) {
            Mockito.verify(settings.getYt().tables(), Mockito.times(1))
                    .write(Mockito.any(), Mockito.anyBoolean(), Mockito.eq(path), Mockito.any(),
                            Mockito.argThat(x -> verifyData(x, date.withDayOfMonth(1))));
        }
    }
}
