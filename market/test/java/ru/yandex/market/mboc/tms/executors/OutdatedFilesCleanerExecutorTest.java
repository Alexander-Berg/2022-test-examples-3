package ru.yandex.market.mboc.tms.executors;

import java.util.NoSuchElementException;

import com.amazonaws.services.s3.AmazonS3Client;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mboc.common.services.offers.enrichment.EnrichedExcel;
import ru.yandex.market.mboc.common.services.offers.enrichment.EnrichedExcelRepositoryMock;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;

/**
 * @author galaev@yandex-team.ru
 * @since 31/07/2018.
 */
public class OutdatedFilesCleanerExecutorTest {

    private OutdatedFilesCleanerExecutor executor;
    private EnrichedExcelRepositoryMock repositoryMock;
    private AmazonS3Client s3Client;

    @Before
    public void setup() {
        repositoryMock = new EnrichedExcelRepositoryMock();
        s3Client = Mockito.mock(AmazonS3Client.class);
        executor = new OutdatedFilesCleanerExecutor(repositoryMock, s3Client, "bucket");
    }

    @Test(expected = NoSuchElementException.class)
    public void testOutdatedFileDeleted() {
        EnrichedExcel outdatedFile = new EnrichedExcel(2, "outdated file");
        outdatedFile.setCreated(DateTimeUtils.dateTimeNow().minusDays(2));
        repositoryMock.insert(outdatedFile);

        executor.execute();

        repositoryMock.findById(outdatedFile.getActionId()); // will throw exception
    }

    @Test(expected = NoSuchElementException.class)
    public void testYmlFileDeleted() {
        EnrichedExcel outdatedFile = new EnrichedExcel(2, "outdated_file.yml");
        outdatedFile.setCreated(DateTimeUtils.dateTimeNow().minusDays(2));
        repositoryMock.insert(outdatedFile);

        executor.execute();

        repositoryMock.findById(outdatedFile.getActionId()); // will throw exception
    }

    @Test
    public void testNewFileIsNotDeleted() {
        EnrichedExcel newFile = new EnrichedExcel(1, "new file");
        repositoryMock.insert(newFile);

        executor.execute();

        Assertions.assertThat(repositoryMock.findById(newFile.getActionId())).isNotNull();
    }
}
