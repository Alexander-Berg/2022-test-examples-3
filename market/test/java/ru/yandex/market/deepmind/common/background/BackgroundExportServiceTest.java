package ru.yandex.market.deepmind.common.background;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

import ru.yandex.market.deepmind.common.ExcelFileDownloader;
import ru.yandex.market.deepmind.common.assertions.DeepmindAssertions;
import ru.yandex.market.deepmind.common.mocks.ExcelS3ServiceMock;
import ru.yandex.market.deepmind.common.services.background.BackgroundServiceMock;
import ru.yandex.market.deepmind.common.utils.SecurityContextAuthenticationUtils;
import ru.yandex.market.mbo.excel.ExcelFile;

public class BackgroundExportServiceTest {

    private BackgroundExportService service;
    private ExcelFileDownloader excelFileDownloader;

    @Before
    public void setUp() throws Exception {
        var backgroundServiceMock = new BackgroundServiceMock();
        var excelS3Service = new ExcelS3ServiceMock();
        var transactionOperations = new TransactionOperations() {
            @Override
            public <T> T execute(TransactionCallback<T> action) throws TransactionException {
                var status = new DefaultTransactionStatus(null, false, false, false, false, null);
                return action.doInTransaction(status);
            }
        };
        service = new BackgroundExportService(backgroundServiceMock, transactionOperations, excelS3Service);
        excelFileDownloader = new ExcelFileDownloader(backgroundServiceMock, excelS3Service);
        SecurityContextAuthenticationUtils.setAuthenticationToken();
    }

    @After
    public void tearDown() throws Exception {
        SecurityContextAuthenticationUtils.clearAuthenticationToken();
    }

    @Test
    public void exportShouldNotProcessLastBatch() {
        int batchSize = 4;
        int rowsSize = batchSize - 1;

        var firstCall = new AtomicBoolean();
        var exportable = new BackgroundExportable<String>() {

            @Override
            public Stream<String> createCursor(Object o) {
                return Stream.generate(() -> "a").limit(batchSize);
            }

            @Override
            public ExcelFile.Builder processBatch(List<String> batch, ExcelFile.Builder builder) {
                if (firstCall.get()) {
                    throw new IllegalStateException("Second call is not allowed");
                }
                for (int i = 0; i < rowsSize; i++) {
                    builder.addLine(batch.get(i));
                }
                firstCall.set(true);
                return builder;
            }
        };

        var config = new BackgroundExportFileConfig<>("test", exportable);
        config.setBatchSize(batchSize);
        var id = service.exportToExcel(config);
        var excelFile = excelFileDownloader.downloadExport(id);

        DeepmindAssertions.assertThat(excelFile)
            .hasLastLine(rowsSize);
    }
}
