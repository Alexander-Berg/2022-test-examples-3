package ru.yandex.market.deliverycalculator.indexer.job;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deliverycalculator.indexer.FunctionalTest;
import ru.yandex.market.deliverycalculator.storage.model.MdsFileHistoryEntity;
import ru.yandex.market.deliverycalculator.storage.repository.MdsFileHistoryRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CleanObsoleteTariffMdsFilesJobTest extends FunctionalTest {

    private static final MdsFileHistoryEntity OBSOLETE_FILE_1 = new MdsFileHistoryEntity(1, "https://bucket-name.s3.mock/tariffs/1/old1.xml",
            Instant.EPOCH);
    private static final MdsFileHistoryEntity OBSOLETE_FILE_2 = new MdsFileHistoryEntity(1, "https://bucket-name.s3.mock/tariffs/1/old2.xml",
            Instant.EPOCH.plus(1, ChronoUnit.DAYS));
    private static final MdsFileHistoryEntity OBSOLETE_FILE_3 = new MdsFileHistoryEntity(3, "https://bucket-name.s3.mock/tariffs/3/old3.xml",
            Instant.EPOCH);

    private static final MdsFileHistoryEntity ACTUAL_FILE_1 = new MdsFileHistoryEntity(1, "https://bucket-name.s3.mock/tariffs/1/new1.xml",
            Instant.now());
    private static final MdsFileHistoryEntity ACTUAL_FILE_2 = new MdsFileHistoryEntity(1, "https://bucket-name.s3.mock/tariffs/1/new2.xml",
            Instant.now());
    private static final MdsFileHistoryEntity ACTUAL_FILE_3 = new MdsFileHistoryEntity(3, "https://bucket-name.s3.mock/tariffs/3/new3.xml",
            Instant.now());

    @Autowired
    private MdsFileHistoryRepository mdsFileHistoryRepository;

    @Autowired
    private MdsS3Client mdsS3Client;

    @Autowired
    private CleanObsoleteTariffMdsFilesJob job;

    @Autowired
    private ResourceLocationFactory mdsS3LocationFactory;

    @BeforeEach
    void init() throws MalformedURLException {
        when(mdsS3LocationFactory.getBucketName()).thenReturn("bucket-name");
        when(mdsS3Client.getUrl(any())).thenReturn(new URL("https://bucket-name.s3.mock/file.txt"));
    }

    @Test
    @DbUnitDataSet(after = "clean-old-tariff-files/testCleanOldMdsTariffFiles.after.csv")
    void testCleanOldMdsTariffFiles() {
        mdsFileHistoryRepository.saveAll(Arrays.asList(OBSOLETE_FILE_1, OBSOLETE_FILE_2, OBSOLETE_FILE_3,
                ACTUAL_FILE_1, ACTUAL_FILE_2, ACTUAL_FILE_3));
        job.doJob(null);
        verify(mdsS3Client, times(3)).delete(any());
    }

    @Test
    @DbUnitDataSet(before = "clean-old-tariff-files/testDontCleanUsedByUrlTariffFile.before.csv",
            after = "clean-old-tariff-files/testDontCleanUsedByUrlTariffFile.after.csv")
    void testDontCleanUsedByUrlTariffFile() {
        mdsFileHistoryRepository.saveAll(Arrays.asList(OBSOLETE_FILE_1, ACTUAL_FILE_1));
        job.doJob(null);
        verify(mdsS3Client, never()).delete(any());
    }

    @Test
    @DbUnitDataSet(after = "clean-old-tariff-files/testDontCleanActualFiles.after.csv")
    void testDontCleanActualFiles() {
        mdsFileHistoryRepository.saveAll(Arrays.asList(ACTUAL_FILE_1, ACTUAL_FILE_2));
        job.doJob(null);
        verify(mdsS3Client, never()).delete(any());
    }
}
