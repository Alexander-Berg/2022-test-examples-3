package ru.yandex.market.logistics.tarifficator.jobs.tms;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.mds.s3.client.exception.MdsS3Exception;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.exception.JobException;
import ru.yandex.market.logistics.tarifficator.service.mds.MdsFileService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.tarifficator.mds.MdsFactory.buildDatasetFilename;

@DisplayName("Интеграционный тест RemoveExpiredMdsFilesExecutor")
class RemoveExpiredMdsFilesExecutorTest extends AbstractContextualTest {

    @Autowired
    private MdsS3Client mdsS3Client;

    @Autowired
    private MdsFileService mdsFileService;

    @Autowired
    private ResourceLocationFactory resourceLocationFactory;

    @BeforeEach
    void init() {
        when(resourceLocationFactory.createLocation(anyString())).thenAnswer(invocation ->
            ResourceLocation.create("tarifficator", invocation.getArgument(0, String.class))
        );
    }

    @DisplayName("Успешное удаление трех файлов, доступных для удаления")
    @DatabaseSetup("/tms/remove-expired-mds-files/before.xml")
    @ExpectedDatabase(value = "/tms/remove-expired-mds-files/after_success.xml", assertionMode = NON_STRICT)
    @Test
    void successDelete() {
        new RemoveExpiredMdsFilesExecutor(mdsFileService).doJob(null);

        verify(mdsS3Client).delete(resourceLocationFactory.createLocation(buildDatasetFilename(1L)));
        verify(mdsS3Client).delete(resourceLocationFactory.createLocation(buildDatasetFilename(2L)));
        verify(mdsS3Client).delete(resourceLocationFactory.createLocation(buildDatasetFilename(3L)));

        verifyNoMoreInteractions(mdsS3Client);
    }

    @DisplayName("Частично успешное удаление батча файлов с ошибкой на файле 2")
    @DatabaseSetup("/tms/remove-expired-mds-files/before.xml")
    @ExpectedDatabase(value = "/tms/remove-expired-mds-files/after_mds_failed.xml", assertionMode = NON_STRICT)
    @Test
    void mdsFailed() {
        ResourceLocation location = resourceLocationFactory.createLocation(buildDatasetFilename(2L));
        String errorMessage = "Failed to delete file 2";
        doThrow(new MdsS3Exception(errorMessage)).when(mdsS3Client).delete(Mockito.eq(location));

        softly.assertThatThrownBy(() -> new RemoveExpiredMdsFilesExecutor(mdsFileService).doJob(null))
            .isInstanceOf(JobException.class)
            .hasMessage(errorMessage);

        verify(mdsS3Client).delete(resourceLocationFactory.createLocation(buildDatasetFilename(1L)));
        verify(mdsS3Client).delete(resourceLocationFactory.createLocation(buildDatasetFilename(2L)));
        verify(mdsS3Client).delete(resourceLocationFactory.createLocation(buildDatasetFilename(3L)));

        verifyNoMoreInteractions(mdsS3Client);
    }
}
