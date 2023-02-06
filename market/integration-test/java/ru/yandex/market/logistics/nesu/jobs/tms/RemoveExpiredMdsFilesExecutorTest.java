package ru.yandex.market.logistics.nesu.jobs.tms;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.mds.s3.client.exception.MdsS3Exception;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.service.mds.MdsFileService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Интеграционный тест для {@link RemoveExpiredMdsFilesExecutor}.
 */
class RemoveExpiredMdsFilesExecutorTest extends AbstractContextualTest {

    @Autowired
    private MdsS3Client mdsS3Client;

    @Autowired
    private MdsFileService mdsFileService;

    @Autowired
    private ResourceLocationFactory resourceLocationFactory;

    /**
     * Успешное удаление трех файлов, доступных для удаления.
     */
    @DatabaseSetup("/tms/remove-expired-mds-files/before.xml")
    @ExpectedDatabase(value = "/tms/remove-expired-mds-files/after_success.xml", assertionMode = NON_STRICT)
    @Test
    void successDelete() {
        new RemoveExpiredMdsFilesExecutor(mdsFileService).doJob(null);

        verify(mdsS3Client).delete(resourceLocationFactory.createLocation("feed_document_1.xml"));
        verify(mdsS3Client).delete(resourceLocationFactory.createLocation("feed_document_2.xml"));
        verify(mdsS3Client).delete(resourceLocationFactory.createLocation("feed_document_3.xml"));

        verifyNoMoreInteractions(mdsS3Client);
    }

    /**
     * В базе 3 файла, доступных для удаления. При попытке удалить второй из файлов выбрасывается исключение.
     * <p>
     * Первый файл должен удалиться, второй и третий остаться.
     */
    @DatabaseSetup("/tms/remove-expired-mds-files/before.xml")
    @ExpectedDatabase(value = "/tms/remove-expired-mds-files/after_mds_failed.xml", assertionMode = NON_STRICT)
    @Test
    void mdsFailed() {
        ResourceLocation location = resourceLocationFactory.createLocation("feed_document_2.xml");
        doThrow(new MdsS3Exception("Failed")).when(mdsS3Client).delete(Mockito.eq(location));

        softly.assertThatThrownBy(() -> new RemoveExpiredMdsFilesExecutor(mdsFileService).doJob(null))
            .isInstanceOf(MdsS3Exception.class);

        verify(mdsS3Client).delete(resourceLocationFactory.createLocation("feed_document_1.xml"));
        verify(mdsS3Client).delete(resourceLocationFactory.createLocation("feed_document_2.xml"));

        verifyNoMoreInteractions(mdsS3Client);
    }
}
