package ru.yandex.canvas.service.multitype.operation;

import java.util.List;
import java.util.Map;

import one.util.streamex.StreamEx;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.config.CanvasTest;
import ru.yandex.canvas.model.CreativeDocument;
import ru.yandex.canvas.model.CreativeDocumentBatch;
import ru.yandex.canvas.model.html5.Batch;
import ru.yandex.canvas.model.html5.Creative;
import ru.yandex.canvas.repository.html5.BatchesRepository;
import ru.yandex.canvas.service.CreativesService;
import ru.yandex.canvas.service.multitype.OnCreativeOperationResult;
import ru.yandex.canvas.service.multitype.request.AdminRejectOperationMultiTypeRequest;
import ru.yandex.canvas.steps.Html5BatchSteps;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.canvas.service.multitype.OnCreativeOperationResult.ok;
import static ru.yandex.canvas.steps.CreativeDocumentBatchSteps.createCreativeDocumentBatch;
import static ru.yandex.canvas.steps.CreativeDocumentSteps.createEmptyCreativeDocument;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdminRejectOperationTest {
    private static final long CLIENT_ID = 1L;
    private static final String REASON = "someReason";

    @Autowired
    public BatchesRepository repository;

    @Autowired
    public Html5BatchSteps html5BatchSteps;

    @Autowired
    public AdminRejectOperation operation;

    @Autowired
    public CreativesService creativesService;

    @Test
    public void adminReject_rejectOneHtml5Creative() {
        Batch batch = html5BatchSteps.createBatch(CLIENT_ID, html5BatchSteps.defaultBatch(CLIENT_ID));
        List<Long> creativeIds = mapList(batch.getCreatives(), Creative::getId);
        var request = AdminRejectOperationMultiTypeRequest.of(creativeIds, REASON);
        Map<Long, OnCreativeOperationResult> result = operation.run(request);

        result.forEach((id, r) -> assertThat(r, is(ok())));

        Batch changedBatch = html5BatchSteps.getBatch(CLIENT_ID, batch.getId(), batch.getProductType());
        changedBatch.getCreatives().forEach(creative -> assertThat(creative.getAdminRejectReason(), is(REASON)));
    }

    @Test
    public void adminReject_rejectOneHtml5Creative_liftRejection() {
        Batch batch = html5BatchSteps.createBatch(CLIENT_ID, html5BatchSteps.defaultBatch(CLIENT_ID));
        List<Long> creativeIds = mapList(batch.getCreatives(), Creative::getId);
        var request = AdminRejectOperationMultiTypeRequest.of(creativeIds, REASON);

        Map<Long, OnCreativeOperationResult> result = operation.run(request);
        result.forEach((id, r) -> assertThat(r, is(ok())));

        var liftRequest = AdminRejectOperationMultiTypeRequest.of(creativeIds, null);
        Map<Long, OnCreativeOperationResult> liftResult = operation.run(liftRequest);
        liftResult.forEach((id, r) -> assertThat(r, is(ok())));

        Batch changedBatch = html5BatchSteps.getBatch(CLIENT_ID, batch.getId(), batch.getProductType());
        changedBatch.getCreatives().forEach(creative -> assertThat(creative.getAdminRejectReason(), nullValue()));
    }

    @Test
    public void adminReject_rejectOneHtml5Creative_twoRejects() {
        Batch batch = html5BatchSteps.createBatch(CLIENT_ID, html5BatchSteps.defaultBatch(CLIENT_ID));
        List<Long> creativeIds = mapList(batch.getCreatives(), Creative::getId);
        var request = AdminRejectOperationMultiTypeRequest.of(creativeIds, REASON);
        Map<Long, OnCreativeOperationResult> resultFirst = operation.run(request);
        resultFirst.forEach((id, r) -> assertThat(r, is(ok())));

        Map<Long, OnCreativeOperationResult> resultSecond = operation.run(request);
        resultSecond.forEach((id, r) -> assertThat(r, is(ok())));

        Batch changedBatch = html5BatchSteps.getBatch(CLIENT_ID, batch.getId(), batch.getProductType());
        changedBatch.getCreatives().forEach(creative -> assertThat(creative.getAdminRejectReason(), is(REASON)));
    }

    @Test
    public void adminReject_rejectTwoHtml5Creatives() {
        List<Batch> batches = List.of(
                html5BatchSteps.createBatch(CLIENT_ID, html5BatchSteps.defaultBatch(CLIENT_ID)),
                html5BatchSteps.createBatch(CLIENT_ID, html5BatchSteps.defaultBatch(CLIENT_ID))
        );
        List<Long> creativeIds = StreamEx.of(batches)
                .flatMap(b -> b.getCreatives().stream())
                .map(Creative::getId)
                .toList();
        var request = AdminRejectOperationMultiTypeRequest.of(creativeIds, REASON);
        Map<Long, OnCreativeOperationResult> result = operation.run(request);

        result.forEach((id, r) -> assertThat(r, is(ok())));

        batches.forEach(batch -> {
            Batch changedBatch = html5BatchSteps.getBatch(CLIENT_ID, batch.getId(), batch.getProductType());
            changedBatch.getCreatives().forEach(creative -> assertThat(creative.getAdminRejectReason(), is(REASON)));
        });
    }

    @Test
    public void adminReject_rejectTwoHtml5Creatives_oneBatchOfFour() {
        List<Creative> creatives = List.of(
                html5BatchSteps.defaultCreative(),
                html5BatchSteps.defaultCreative(),
                html5BatchSteps.defaultCreative(),
                html5BatchSteps.defaultCreative()
        );
        List<Long> idsToStop = List.of(creatives.get(1).getId(), creatives.get(3).getId());

        Batch batchToSave = html5BatchSteps.defaultBatch(CLIENT_ID).setCreatives(creatives);
        Batch batch = html5BatchSteps.createBatch(CLIENT_ID, batchToSave);

        var request = AdminRejectOperationMultiTypeRequest.of(idsToStop, REASON);
        Map<Long, OnCreativeOperationResult> result = operation.run(request);

        result.forEach((id, r) -> assertThat(r, is(ok())));

        Batch changedBatch = html5BatchSteps.getBatch(CLIENT_ID, batch.getId(), batch.getProductType());
        changedBatch.getCreatives()
                .forEach(creative -> assertThat(creative.getAdminRejectReason(),
                        idsToStop.contains(creative.getId()) ? is(REASON) : nullValue()));
    }

    @Test
    public void adminReject_rejectOneAdBuilderCreative() {
        CreativeDocumentBatch batch = creativesService.createBatch(createCreativeDocumentBatch(CLIENT_ID), CLIENT_ID);
        List<Long> creativeIds = mapList(batch.getItems(), CreativeDocument::getId);
        var request = AdminRejectOperationMultiTypeRequest.of(creativeIds, REASON);
        Map<Long, OnCreativeOperationResult> result = operation.run(request);

        result.forEach((id, r) -> assertThat(r, is(ok())));

        CreativeDocumentBatch changedBatch = creativesService.getBatch(batch.getId(), CLIENT_ID);
        changedBatch.getItems().forEach(creative -> assertThat(creative.getAdminRejectReason(), is(REASON)));
    }

    @Test
    public void adminReject_rejectTwoAdBuilderCreatives() {
        List<CreativeDocumentBatch> batches = List.of(
                creativesService.createBatch(createCreativeDocumentBatch(CLIENT_ID), CLIENT_ID),
                creativesService.createBatch(createCreativeDocumentBatch(CLIENT_ID), CLIENT_ID)
        );
        List<Long> creativeIds = StreamEx.of(batches)
                .flatMap(b -> b.getItems().stream())
                .map(CreativeDocument::getId)
                .toList();
        var request = AdminRejectOperationMultiTypeRequest.of(creativeIds, REASON);
        Map<Long, OnCreativeOperationResult> result = operation.run(request);

        result.forEach((id, r) -> assertThat(r, is(ok())));

        batches.forEach(batch -> {
            CreativeDocumentBatch changedBatch = creativesService.getBatch(batch.getId(), CLIENT_ID);
            changedBatch.getItems().forEach(creative -> assertThat(creative.getAdminRejectReason(), is(REASON)));
        });
    }

    @Test
    public void adminReject_rejectTwoAdBuilderCreatives_oneBatchOfFour() {
        CreativeDocumentBatch batchToCreate = createCreativeDocumentBatch(CLIENT_ID);
        List<CreativeDocument> creatives = List.of(
                createEmptyCreativeDocument("testBundle", batchToCreate.getId(), batchToCreate.getName(), 1),
                createEmptyCreativeDocument("testBundle", batchToCreate.getId(), batchToCreate.getName(), 1),
                createEmptyCreativeDocument("testBundle", batchToCreate.getId(), batchToCreate.getName(), 1),
                createEmptyCreativeDocument("testBundle", batchToCreate.getId(), batchToCreate.getName(), 1)
        );
        batchToCreate.setItems(creatives);

        CreativeDocumentBatch batch = creativesService.createBatch(batchToCreate, CLIENT_ID);
        List<Long> idsToStop = List.of(creatives.get(1).getId(), creatives.get(3).getId());

        var request = AdminRejectOperationMultiTypeRequest.of(idsToStop, REASON);
        Map<Long, OnCreativeOperationResult> result = operation.run(request);

        result.forEach((id, r) -> assertThat(r, is(ok())));

        CreativeDocumentBatch changedBatch = creativesService.getBatch(batch.getId(), CLIENT_ID);
        changedBatch.getItems()
                .forEach(creative -> assertThat(creative.getAdminRejectReason(),
                        idsToStop.contains(creative.getId()) ? is(REASON) : nullValue()));
    }
}
