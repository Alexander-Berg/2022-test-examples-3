package ru.yandex.market.clab.tms.enrich.requested;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.clab.common.test.stubs.ModelStorageServiceStub;
import ru.yandex.market.clab.db.jooq.generated.enums.RequestedGoodErrorType;
import ru.yandex.market.clab.db.jooq.generated.tables.records.RequestedGoodRecord;
import ru.yandex.market.clab.tms.enrich.GoodMsku;
import ru.yandex.market.clab.tms.service.ModelService;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.clab.common.test.ModelTestUtils.model;
import static ru.yandex.market.clab.common.test.ModelTestUtils.sku;

/**
 * @author anmalysh
 */
public class FetchRequestedMskuProcessorTest {

    private static final long GOOD_ID = 1077535551;
    private static final long MODEL_ID_1 = 2661788;
    private static final long NOT_FOUND_MODEL_ID = 77388810;

    private ModelStorageServiceStub modelStorageService = new ModelStorageServiceStub();

    private FetchRequestedMskuProcessor processor;

    @Before
    public void before() {
        processor = new FetchRequestedMskuProcessor(new ModelService(modelStorageService));
    }

    @Test
    public void processReturnsPopulatedRecords() {
        ModelStorage.Model.Builder sku = sku(MODEL_ID_1);

        putModel(sku);

        List<RequestedGoodProcessingResult> results = processor
            .process(Collections.singletonList(new GoodMsku(GOOD_ID, MODEL_ID_1)));
        assertThat(results).hasSize(1);
        RequestedGoodProcessingResult result = results.get(0);
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getFixErrorTypes()).containsExactly(
            RequestedGoodErrorType.MSKU_NOT_FOUND
        );
        RequestedGoodRecord record = result.getRecord();
        assertThat(record.getId()).isEqualTo(GOOD_ID);
        assertThat(record.getCategoryId()).isEqualTo(1L);
    }

    @Test
    public void failureIfRelationNotFound() {
        putModel(model(MODEL_ID_1));

        List<RequestedGoodProcessingResult> results = processor.process(Arrays.asList(
            new GoodMsku(1, MODEL_ID_1),
            new GoodMsku(2, NOT_FOUND_MODEL_ID)
        ));
        assertThat(results).hasSize(2);
        RequestedGoodProcessingResult failureResult = results.stream()
            .filter(RequestedGoodProcessingResult::isFailure)
            .findFirst().orElse(null);
        assertThat(failureResult).isNotNull();
        assertThat(failureResult.isFailure()).isTrue();
        assertThat(failureResult.getErrorType()).isEqualTo(RequestedGoodErrorType.MSKU_NOT_FOUND);
    }

    private void putModel(ModelStorage.Model.Builder... models) {
        for (ModelStorage.Model.Builder model : models) {
            modelStorageService.addModel(model.build());
        }
    }
}
