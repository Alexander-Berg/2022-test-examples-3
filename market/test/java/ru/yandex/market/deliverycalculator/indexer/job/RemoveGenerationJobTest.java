package ru.yandex.market.deliverycalculator.indexer.job;

import java.net.URL;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deliverycalculator.indexer.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тест для {@link RemoveGenerationJob}.
 */
class RemoveGenerationJobTest extends FunctionalTest {

    @Autowired
    private RemoveGenerationJob removeGenerationJob;

    @Autowired
    private MdsS3Client mdsS3Client;

    @Autowired
    private ResourceLocationFactory mdsS3LocationFactory;

    /**
     * Тест, проверяющий корректное удаление неактуальных поколений сваренных настроек сендера.
     */
    @Test
    @DbUnitDataSet(before = "removeSenderSettingsGenerations.before.csv",
            after = "removeSenderSettingsGenerations.after.csv")
    void testRemoveOutdatedShopSettingsGenerations() throws Exception {
        checkRemoveGenerationJob();
    }

    /**
     * Тест, проверяющий корректное удаление неактуальных поколений сваренных модификаторов MBI магазина.
     */
    @Test
    @DbUnitDataSet(before = "removeShopModifiersGenerations.before.csv",
            after = "removeShopModifiersGenerations.after.csv")
    void testRemoveOutdatedShopModifiersGenerations() throws Exception {
        checkRemoveGenerationJob();
    }

    private void checkRemoveGenerationJob() throws Exception {
        when(mdsS3LocationFactory.getBucketName()).thenReturn("bucket-name");
        when(mdsS3Client.getUrl(any())).thenReturn(new URL("https://bucket-name.s3.mock/file.txt"));

        removeGenerationJob.doJob(null);

        verify(mdsS3Client, times(2)).delete(any());
    }

}
