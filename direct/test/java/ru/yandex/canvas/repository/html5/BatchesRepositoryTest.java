package ru.yandex.canvas.repository.html5;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.IntStream;

import com.mongodb.client.result.UpdateResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.config.CanvasTest;
import ru.yandex.canvas.model.html5.Batch;
import ru.yandex.canvas.service.html5.Html5BatchesService;
import ru.yandex.canvas.service.html5.Html5SourcesService;
import ru.yandex.canvas.steps.Html5BatchSteps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.domain.Sort.Direction.DESC;
import static ru.yandex.canvas.service.SessionParams.SessionTag.CPM_BANNER;
import static ru.yandex.canvas.service.SessionParams.SessionTag.CPM_YNDX_FRONTPAGE;

@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BatchesRepositoryTest {

    @Autowired
    Html5BatchSteps batchSteps;
    @Autowired
    BatchesRepository repository;
    @Autowired
    Html5BatchesService batchService;
    @Autowired
    Html5SourcesService sourceService;

    private long clientId;

    @Before
    public void setUp() {
        clientId = new Random().nextLong();
    }

    @Test
    public void createBatchWithDefaultType_defaultTypeInDb() {
        Batch batch = batchSteps.createBatch(clientId, batchSteps.defaultBatch(clientId));
        Batch result = repository.getBatchById(clientId, batch.getId(), null);
        assertThat(result).isNotNull();
        assertThat(result.getProductType()).isEqualTo(CPM_BANNER);
    }

    @Test
    public void createBatchWithNullType_noTypeInDb() {
        Batch batch = batchSteps.createBatch(clientId, batchSteps.defaultBatch(clientId).setProductType(null));
        Batch result = repository.getBatchById(clientId, batch.getId(), null);
        assertThat(result).isNotNull();
        assertThat(result.getProductType()).isNull();
    }

    @Test
    public void createBatchWithCustomType_typeInDbIsSet() {
        Batch batch = batchSteps.createBatch(clientId, batchSteps.defaultYndxFrontpageBatch(clientId));
        Batch result = repository.getBatchById(clientId, batch.getId(), CPM_YNDX_FRONTPAGE);
        assertThat(result).isNotNull();
        assertThat(result.getProductType()).isEqualTo(CPM_YNDX_FRONTPAGE);
    }

    @Test
    public void updateDefaultTypeBatchName_CorrectType_BatchIsUpdated() {
        Batch batch = batchSteps.createBatch(clientId, batchSteps.defaultBatch(clientId));
        String oldName = batch.getName();
        String newName = oldName + "_1";

        UpdateResult result = repository.updateBatchName(batch.getId(), clientId, null, newName);
        assertThat(result.getMatchedCount() > 0).isTrue();

        Batch modified = batchSteps.getBatch(clientId, batch.getId(), null);
        assertThat(modified.getName()).isEqualTo(newName);
    }

    @Test
    public void updateDefaultTypeBatchName_IncorrectType_BatchIsNotUpdated() {
        Batch batch = batchSteps.createBatch(clientId, batchSteps.defaultBatch(clientId));
        String oldName = batch.getName();
        String newName = oldName + "_1";

        UpdateResult result = repository.updateBatchName(batch.getId(), clientId, CPM_YNDX_FRONTPAGE, newName);
        assertThat(result.getMatchedCount() > 0).isFalse();

        Batch modified = batchSteps.getBatch(clientId, batch.getId(), null);
        assertThat(modified.getName()).isEqualTo(oldName);
    }

    @Test
    public void updateCustomTypeBatchName_CorrectType_BatchIsUpdated() {
        Batch batch = batchSteps.createBatch(clientId, batchSteps.defaultYndxFrontpageBatch(clientId));
        String oldName = batch.getName();
        String newName = oldName + "_1";

        UpdateResult result = repository.updateBatchName(batch.getId(), clientId, CPM_YNDX_FRONTPAGE, newName);
        assertThat(result.getMatchedCount() > 0).isTrue();

        Batch modified = batchSteps.getBatch(clientId, batch.getId(), CPM_YNDX_FRONTPAGE);
        assertThat(modified.getName()).isEqualTo(newName);
    }

    @Test
    public void updateCustomTypeBatchName_IncorrectType_BatchIsNotUpdated() {
        Batch batch = batchSteps.createBatch(clientId, batchSteps.defaultYndxFrontpageBatch(clientId));
        String oldName = batch.getName();
        String newName = oldName + "_1";

        UpdateResult result
                = repository.updateBatchName(batch.getId(), clientId, null, newName);
        assertThat(result.getMatchedCount() > 0).isFalse();

        Batch modified = batchSteps.getBatch(clientId, batch.getId(), CPM_YNDX_FRONTPAGE);
        assertThat(modified.getName()).isEqualTo(oldName);
    }

    @Test
    public void archiveDefaultTypeBatch_CorrectType_BatchIsArchived() {
        Batch batch = batchSteps.createBatch(clientId, batchSteps.defaultBatch(clientId));

        UpdateResult result = repository.archiveBatch(batch.getId(), clientId, null);
        assertThat(result.getMatchedCount() > 0).isTrue();

        Optional<Batch> modified = batchSteps.getArchiveBatch(clientId, batch.getId());
        assertThat(modified).isPresent();
        assertThat(modified.get().getArchive()).isTrue();
    }

    @Test
    public void getBatchesByQuery_limitTest_returnsLimit() {
        int total = 10;
        int toFetch = total / 2;

        IntStream.range(0, total).forEach(i -> batchSteps.createBatch(clientId, batchSteps.defaultBatch(clientId)));
        List<Batch> result = repository.getBatchesByQuery(clientId, toFetch, 0, DESC, false, null);
        assertThat(result.size()).isEqualTo(toFetch);
    }

    @Test
    public void getBatchesByQuery_offsetTest_returnsTotalMinusOffset() {
        int total = 10;
        int toFetch = 6;
        int offset = 6;
        int expected = 4;

        IntStream.range(0, total).forEach(i -> batchSteps.createBatch(clientId, batchSteps.defaultBatch(clientId)));
        List<Batch> result = repository.getBatchesByQuery(clientId, toFetch, offset, DESC, false, null);
        assertThat(result.size()).isEqualTo(expected);
    }

    @Test
    public void getBatchesByQuery_nameFilterTest_onlyFilteredReturned() {
        int total = 10;
        int expected = 2;
        String nameToFilter = "BatchToGet";
        IntStream.range(0, total).forEach(i ->
                batchSteps.createBatch(clientId, batchSteps.defaultBatch(clientId).setName("BatchToLeave")));
        IntStream.range(0, expected).forEach(i ->
                batchSteps.createBatch(clientId, batchSteps.defaultBatch(clientId).setName(nameToFilter)));

        List<Batch> result = repository.getBatchesByQuery(clientId, 50, 0, DESC, false, nameToFilter);
        assertThat(result.size()).isEqualTo(expected);
    }
}
