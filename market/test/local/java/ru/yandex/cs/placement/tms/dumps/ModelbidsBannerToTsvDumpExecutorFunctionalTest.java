package ru.yandex.cs.placement.tms.dumps;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractCsPlacementTmsFunctionalTest;

import static ru.yandex.vendor.util.CsvTestUtils.verifyCsvWithDelimiter;

public class ModelbidsBannerToTsvDumpExecutorFunctionalTest
        extends AbstractCsPlacementTmsFunctionalTest {

    private static final String FILE_NAME = "market_modelbids_banner.tsv";

    private final ModelbidsBannerToTsvDumpExecutor executor;

    @Autowired
    public ModelbidsBannerToTsvDumpExecutorFunctionalTest(
            ModelbidsBannerToTsvDumpExecutor executor) {
        this.executor = executor;
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/placement/tms/dumps/ModelbidsBannerToTsvDumpExecutorFunctionalTest/testDumpfileUpload/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/cs/placement/tms/dumps/ModelbidsBannerToTsvDumpExecutorFunctionalTest/testDumpfileUpload/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    void testDumpfileUpload(@TempDir Path tempDir) throws IOException {
        Path filePath = tempDir.resolve(FILE_NAME);

        executor.doJob(null);

        try (Writer writer = Files.newBufferedWriter(filePath)) {
            executor.writeDumpData(writer);
        }

        verifyCsvWithDelimiter(
                getInputStreamResource("/testDumpfileUpload/expected.tsv").getInputStream(),
                Files.newInputStream(filePath),
                '\t'
        );
    }
}
