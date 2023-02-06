package ru.yandex.market.billing.tasks.bidding.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.mds.s3.client.service.api.DirectMdsS3Client;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ImportActiveVendorsExecutorTest extends FunctionalTest {

    @Autowired
    private DirectMdsS3Client directMdsS3Client;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @Autowired
    private ImportActiveVendorsExecutor importActiveVendorsExecutor;

    @BeforeEach
    void before() {
        MockitoAnnotations.initMocks(this);
        String payloadDataWithEmptyStrings = "4\tsomeString\t200\r\n" +
                "5\tsomeOtherString\t200\n" +
                "" +
                "6\tsomeAnotherString\t300\n";
        when(directMdsS3Client.download(any(), any()))
                .thenReturn(payloadDataWithEmptyStrings);
    }

    /**
     * Ожидания:
     * 1.данные в таблице по активынм вендорам(пересекается с тестом {@link ActiveVendorsDaoTest}):
     * - старые удаляются
     * - новые записиываются
     * 2. в файле получаемом из mds:
     * - корректно игнорируются поля TSV формаата в неиспользуемых  позициях
     * - корректно игнорятся пусыте строки
     */
    @DbUnitDataSet(
            before = "db/active_vendors_before.csv",
            after = "db/active_vendors_after.csv"
    )
    @Test
    void test_doJob_generalTest() {
        importActiveVendorsExecutor.doJob(jobExecutionContext);
    }
}
