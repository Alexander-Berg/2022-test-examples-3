package ru.yandex.canvas.service.html5;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.config.CanvasTest;
import ru.yandex.canvas.model.Size;
import ru.yandex.canvas.model.html5.Batch;
import ru.yandex.canvas.model.html5.Source;
import ru.yandex.canvas.repository.html5.BatchesRepository;
import ru.yandex.canvas.service.DirectService;
import ru.yandex.canvas.service.SessionParams;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.canvas.Html5Constants.VALID_SIZES_CPM_YNDX_FRONTPAGE;
import static ru.yandex.canvas.Html5Constants.VALID_SIZES_GENERAL;
import static ru.yandex.canvas.service.SessionParams.Html5Tag.HTML5_CPM_BANNER;
import static ru.yandex.canvas.service.SessionParams.Html5Tag.HTML5_CPM_YNDX_FRONTPAGE;
import static ru.yandex.canvas.steps.SourceSteps.defaultActiveSource;

@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class Html5BatchesServiceGetBatchesTest {
    private static final Size SIZE = VALID_SIZES_GENERAL.iterator().next();
    private static final int LIMIT = 50;
    private static final int OFFSET = 0;
    private static final Sort.Direction DIRECTION = Sort.Direction.ASC;
    private static final boolean ARCHIVE = false;

    @Autowired
    BatchesRepository batchesRepository;

    @Autowired
    Html5BatchesService service;

    @Autowired
    MongoOperations mongoOperations;

    //@MockBean
    @Autowired
    private SessionParams sessionParams;

    @MockBean
    private DirectService directService;

    private long clientId;
    private String batchName;

    @Before
    public void setUp() {
        clientId = new Random().nextLong();
        batchName = UUID.randomUUID().toString();
        Mockito.when(sessionParams.getHtml5SessionTag()).thenReturn(HTML5_CPM_BANNER);
    }

    @Test
    public void getSingleBatch_returnsOneBatch() {
        Source source = defaultActiveSource(clientId, SIZE);
        service.createBatchFromSources(clientId, batchName, List.of(source), HTML5_CPM_BANNER, null);

        List<Batch> batches = service.getBatches(clientId, LIMIT, OFFSET, DIRECTION, ARCHIVE,
                List.of(SIZE.toString()), batchName, HTML5_CPM_BANNER);

        assertEquals("Should return only one batch", 1, batches.size());
        assertEquals("Should return batch with correct name", batchName, batches.get(0).getName());
        assertTrue("Should mark batch as available", batches.get(0).getAvailable());
    }

    @Test
    public void getSingleNonGeneralBatch_returnsOneBatch() {
        Mockito.when(sessionParams.getHtml5SessionTag()).thenReturn(HTML5_CPM_YNDX_FRONTPAGE);
        Size size = VALID_SIZES_CPM_YNDX_FRONTPAGE.iterator().next();
        Source source = defaultActiveSource(clientId, size);
        service.createBatchFromSources(clientId, batchName, List.of(source), HTML5_CPM_YNDX_FRONTPAGE, null);

        List<Batch> batches = service.getBatches(clientId, LIMIT, OFFSET, DIRECTION, ARCHIVE,
                List.of(size.toString()), batchName, HTML5_CPM_YNDX_FRONTPAGE);

        assertEquals("Should return only one batch", 1, batches.size());
        assertEquals("Should return batch with correct name", batchName, batches.get(0).getName());
        assertTrue("Should mark batch as available", batches.get(0).getAvailable());
    }

    @Test
    public void getSingleBatchWithDifferentSize_returnsOneBatch() {
        Size differentSize = StreamEx.of(VALID_SIZES_GENERAL).remove(SIZE::equals).findFirst().orElse(SIZE);
        Source source = defaultActiveSource(clientId, SIZE);
        service.createBatchFromSources(clientId, batchName, List.of(source), HTML5_CPM_BANNER, null);

        List<Batch> batches = service.getBatches(clientId, LIMIT, OFFSET, DIRECTION, ARCHIVE,
                List.of(differentSize.toString()), batchName, HTML5_CPM_BANNER);

        assertEquals("Should return only one batch", 1, batches.size());
        assertEquals("Should return batch with correct name", batchName, batches.get(0).getName());
        assertFalse("Should mark batch as not available", batches.get(0).getAvailable());
    }

    @Test
    public void getSingleBatchOfTwoSources_returnsOneBatch() {
        service.createBatchFromSources(clientId, batchName,
                List.of(defaultActiveSource(clientId, SIZE), defaultActiveSource(clientId, SIZE)), HTML5_CPM_BANNER,
                null);

        List<Batch> batches = service.getBatches(clientId, LIMIT, OFFSET, DIRECTION, ARCHIVE,
                List.of(SIZE.toString()), batchName, HTML5_CPM_BANNER);

        assertEquals("Should return only one batch", 1, batches.size());
        assertEquals("Should return batch with correct name", batchName, batches.get(0).getName());
        assertEquals("Should return two creatives", 2, batches.get(0).getCreatives().size());
        assertTrue("Should mark batch as available", batches.get(0).getAvailable());
    }

    @Test
    public void getSingleBatch_dbHasOverLimitOtherBatches_returnsOneBatch() {
        int limit = 5;

        Size size = VALID_SIZES_CPM_YNDX_FRONTPAGE.iterator().next();
        for (int i = 0; i < limit + 5; i++) {
            Mockito.when(sessionParams.getHtml5SessionTag()).thenReturn(HTML5_CPM_YNDX_FRONTPAGE);
            service.createBatchFromSources(clientId, batchName,
                    List.of(defaultActiveSource(clientId, size)), HTML5_CPM_YNDX_FRONTPAGE, null);
        }

        Mockito.when(sessionParams.getHtml5SessionTag()).thenReturn(HTML5_CPM_BANNER);
        service.createBatchFromSources(clientId, batchName,
                List.of(defaultActiveSource(clientId, SIZE), defaultActiveSource(clientId, SIZE)), HTML5_CPM_BANNER,
                null);

        List<Batch> batches = service.getBatches(clientId, limit, OFFSET, DIRECTION, ARCHIVE,
                List.of(SIZE.toString()), batchName, HTML5_CPM_BANNER);

        assertEquals("Should return only one batch", 1, batches.size());
        assertEquals("Should return batch with correct name", batchName, batches.get(0).getName());
        assertTrue("Should mark batch as available", batches.get(0).getAvailable());
    }

    @Test
    public void getSingleBatchWithOffset_dbHasOverLimitOtherBatches_returnsOneBatch() {
        int limit = 5;

        Size size = VALID_SIZES_CPM_YNDX_FRONTPAGE.iterator().next();
        Mockito.when(sessionParams.getHtml5SessionTag()).thenReturn(HTML5_CPM_YNDX_FRONTPAGE);
        for (int i = 0; i < limit + 5; i++) {
            service.createBatchFromSources(clientId, batchName,
                    List.of(defaultActiveSource(clientId, size)), HTML5_CPM_YNDX_FRONTPAGE, null);
        }

        Mockito.when(sessionParams.getHtml5SessionTag()).thenReturn(HTML5_CPM_BANNER);
        service.createBatchFromSources(clientId, batchName,
                List.of(defaultActiveSource(clientId, SIZE), defaultActiveSource(clientId, SIZE)), HTML5_CPM_BANNER,
                null);

        service.createBatchFromSources(clientId, batchName,
                List.of(defaultActiveSource(clientId, SIZE), defaultActiveSource(clientId, SIZE)), HTML5_CPM_BANNER,
                null);

        List<Batch> batches = service.getBatches(clientId, limit, 1, DIRECTION, ARCHIVE,
                List.of(SIZE.toString()), batchName, HTML5_CPM_BANNER);

        assertEquals("Should return only one batch", 1, batches.size());
        assertEquals("Should return batch with correct name", batchName, batches.get(0).getName());
        assertTrue("Should mark batch as available", batches.get(0).getAvailable());
    }
}
