package ru.yandex.market.axapta;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.axapta.AxaptaRealSupplierDao;
import ru.yandex.market.core.supplier.RealSupplierDao;
import ru.yandex.market.core.supplier.model.RealSupplierInfo;
import ru.yandex.market.logbroker.LogbrokerService;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.tms.util.FeatureFlagHelper;
import ru.yandex.market.request.trace.Module;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
class ImportRealSuppliersExecutorTest extends FunctionalTest {

    @Autowired
    private RealSupplierDao realSupplierDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private Module sourceModule;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    @Qualifier("mboPartnerExportLogbrokerService")
    private LogbrokerService mboPartnerExportLogbrokerService;

    private ImportRealSuppliersExecutor importRealSuppliersExecutor;

    private RealSupplierDao realSupplierDaoSpy;

    @BeforeEach
    void setUp() {
        realSupplierDaoSpy = spy(realSupplierDao);

        AxaptaRealSupplierDao axaptaRealSupplierDao = new AxaptaRealSupplierDaoMock(Stream.iterate(1, i -> i + 1)
                .limit(11)
                .map(i -> new RealSupplierInfo.Builder()
                        .setRealSupplierId(StringUtils.leftPad(i.toString(), 4, '0'))
                        .setName(i <= 10 ? "test " + StringUtils.leftPad(i.toString(), 4, '0') : null)
                        .setUpdatedAt(LocalDateTime.of(2018, 6, i, 0, 0)
                                .atZone(ZoneId.systemDefault()).toInstant())
                        .build())
                .collect(Collectors.toList()));

        importRealSuppliersExecutor = new ImportRealSuppliersExecutor(
                axaptaRealSupplierDao,
                realSupplierDaoSpy,
                transactionTemplate,
                3,
                sourceModule,
                environmentService,
                applicationEventPublisher
        );

        var seqRestart = jdbcTemplate.queryForObject(
                "select coalesce(max(id), 0) + 1 from shops_web.real_supplier",
                Long.class
        );
        jdbcTemplate.execute("alter sequence shops_web.s_datasource restart with " + seqRestart);
        FeatureFlagHelper.setShouldRunOnShopTms(importRealSuppliersExecutor, environmentService);
    }

    @DbUnitDataSet(after = "ImportRealSuppliersExecutorTest.all.after.csv")
    @Test
    void firstCall() {
        importRealSuppliersExecutor.doJob(null);
    }

    @DbUnitDataSet(
            before = "ImportRealSuppliersExecutorTest.fullUpdate.before.csv",
            after = "ImportRealSuppliersExecutorTest.all.after.csv"
    )
    @Test
    void fullUpdate() {
        importRealSuppliersExecutor.doJob(null);
    }

    @DbUnitDataSet(
            before = "ImportRealSuppliersExecutorTest.partUpdate.before.csv",
            after = "ImportRealSuppliersExecutorTest.all.after.csv"
    )
    @Test
    void partUpdate() {
        importRealSuppliersExecutor.doJob(null);
        verify(mboPartnerExportLogbrokerService, atLeast(1)).publishEvent(any());
    }

    @DbUnitDataSet(
            before = "ImportRealSuppliersExecutorTest.hasMissing.before.csv",
            after = "ImportRealSuppliersExecutorTest.hasMissing.after.csv"
    )
    @Test
    void hasMissing() {
        importRealSuppliersExecutor.doJob(null);
    }

    @DbUnitDataSet(
            before = "ImportRealSuppliersExecutorTest.all.after.csv",
            after = "ImportRealSuppliersExecutorTest.all.after.csv"
    )
    @Test
    void noMoreUpdates() {
        importRealSuppliersExecutor.doJob(null);
        verify(realSupplierDaoSpy, never()).saveRealSupplier(any());
    }

    private static class AxaptaRealSupplierDaoMock extends AxaptaRealSupplierDao {

        List<RealSupplierInfo> suppliers;

        AxaptaRealSupplierDaoMock(List<RealSupplierInfo> suppliers) {
            super(null);
            this.suppliers = suppliers;
        }

        @Override
        public List<RealSupplierInfo> readNext(Instant from, int batch) {
            return suppliers.stream()
                    .filter(s -> s.getUpdatedAt().isAfter(from))
                    .sorted(Comparator.comparing(RealSupplierInfo::getUpdatedAt))
                    .limit(batch)
                    .collect(Collectors.toList());
        }
    }


}
