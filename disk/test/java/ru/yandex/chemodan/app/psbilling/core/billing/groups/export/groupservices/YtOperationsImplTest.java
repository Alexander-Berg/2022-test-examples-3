package ru.yandex.chemodan.app.psbilling.core.billing.groups.export.groupservices;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.concurrent.TimeUnit;

import net.jodah.failsafe.RetryPolicy;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.config.YtExportSettings;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServiceTransactionCalculationDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServiceTransactionsDao;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupService;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.CalculationStatus;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.GroupServiceTransaction;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;
import ru.yandex.chemodan.util.yt.YtHelper;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.YtUtils;

@Ignore
public class YtOperationsImplTest extends AbstractPsBillingCoreTest {

    @Autowired
    private GroupServiceTransactionsDao groupServiceTransactionsDao;
    @Autowired
    private GroupServiceTransactionCalculationDao groupServiceTransactionCalculationDao;

    private LocalDate today = LocalDate.now().minusDays(2);

    @Before
    public void init() {
        Group group = psBillingGroupsFactory.createGroup();
        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct();
        GroupService groupService = psBillingGroupsFactory.createGroupService(group, groupProduct);
        groupServiceTransactionCalculationDao.insertIfAbsent(today);
        groupServiceTransactionsDao.batchInsert(Cf.list(GroupServiceTransaction.builder()
            .currency(Currency.getInstance("RUB"))
            .amount(new BigDecimal("123344.19"))
            .groupServiceId(groupService.getId())
            .billingDate(today)
            .build()));

        groupServiceTransactionCalculationDao
            .updateStatus(today, CalculationStatus.STARTING, CalculationStatus.COMPLETED);
    }

    @Test
    public void testListFolders() {
        Yt http = YtUtils.http("hahn.yt.yandex.net", "");
        YtHelper helper = new YtHelper(http, new RetryPolicy().withMaxRetries(3).withDelay(10, TimeUnit.SECONDS));
        YtExportSettings settings =
            new YtExportSettings(true, YPath.simple("//home/disk-ps-billing/export_transactions/testing"), helper);
        YtOperationsGroupTransactionsImpl ytExporter = new YtOperationsGroupTransactionsImpl(settings, 671,  ()-> 1000);
        MapF<String, ListF<String>> exportFolders = ytExporter.findExportFolders();
        System.out.println(exportFolders);

    }

}
