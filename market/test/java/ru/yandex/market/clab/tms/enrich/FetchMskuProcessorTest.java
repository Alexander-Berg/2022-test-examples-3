package ru.yandex.market.clab.tms.enrich;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.clab.common.mbo.ProtoUtils;
import ru.yandex.market.clab.common.service.category.CategoryRepository;
import ru.yandex.market.clab.common.test.stubs.ModelStorageServiceStub;
import ru.yandex.market.clab.db.jooq.generated.enums.GoodErrorType;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Category;
import ru.yandex.market.clab.db.jooq.generated.tables.records.GoodRecord;
import ru.yandex.market.clab.tms.service.ModelService;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.market.clab.common.test.ModelTestUtils.model;
import static ru.yandex.market.clab.common.test.ModelTestUtils.sku;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 12.11.2018
 */
@RunWith(MockitoJUnitRunner.class)
public class FetchMskuProcessorTest {

    private static final long GOOD_ID = 1077535551;
    private static final long MODEL_ID_1 = 2661788;
    private static final long MODEL_ID_2 = 825115000;
    private static final long MODEL_ID_3 = 1120205469;
    private static final long NOT_FOUND_MODEL_ID = 77388810;

    private ModelStorageServiceStub modelStorageService = new ModelStorageServiceStub();

    @Mock
    private CategoryRepository categoryRepository;

    private FetchMskuProcessor processor;

    @Before
    public void before() {
        processor = new FetchMskuProcessor(new ModelService(modelStorageService), categoryRepository);

        when(categoryRepository.getEffectiveCategoryTree()).thenReturn(
            Collections.singletonList(new Category()
                .setId(1L)
                .setGoodTypeId(2L))
        );
        processor.beforeStep(null);
    }

    @Test
    public void processReturnsPopulatedRecords() {
        ModelStorage.Model.Builder sku = sku(MODEL_ID_1)
            .addRelations(
                ModelStorage.Relation.newBuilder()
                    .setId(MODEL_ID_2)
                    .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                    .build()
            );
        ModelStorage.Model.Builder parentModel = model(MODEL_ID_2)
            .setParentId(MODEL_ID_3);
        ModelStorage.Model.Builder rootModel = model(MODEL_ID_3);

        putModel(sku, parentModel, rootModel);

        List<GoodProcessingResult> results = processor
            .process(Collections.singletonList(new GoodMsku(GOOD_ID, MODEL_ID_1)));
        assertThat(results).hasSize(1);
        GoodProcessingResult result = results.get(0);
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getFixErrorTypes()).containsExactlyInAnyOrder(
            GoodErrorType.MSKU_HIERARCHY_NOT_FOUND,
            GoodErrorType.GOOD_TYPE_NOT_FOUND
        );
        GoodRecord record = result.getRecord();
        assertThat(record.getId()).isEqualTo(GOOD_ID);
        assertThat(record.getStorageMsku()).isEqualTo(sku.build().toByteArray());
        assertThat(record.getStorageModification()).isEqualTo(parentModel.build().toByteArray());
        assertThat(record.getStorageModel()).isEqualTo(rootModel.build().toByteArray());
    }

    @Test
    public void failureIfRelationNotFound() {
        putModel(model(MODEL_ID_1));

        List<GoodProcessingResult> results = processor.process(Arrays.asList(
            new GoodMsku(1, MODEL_ID_1),
            new GoodMsku(2, NOT_FOUND_MODEL_ID)
        ));
        assertThat(results).hasSize(2);
        GoodProcessingResult failureResult = results.stream()
            .filter(GoodProcessingResult::isFailure)
            .findFirst().orElse(null);
        assertThat(failureResult).isNotNull();
        assertThat(failureResult.isFailure()).isTrue();
        assertThat(failureResult.getErrorType()).isEqualTo(GoodErrorType.MSKU_HIERARCHY_NOT_FOUND);
    }

    @Test
    public void failureIfRootModelNotFound() {
        putModel(model(MODEL_ID_1)
            .addParameterValues(
                ModelStorage.ParameterValue.newBuilder()
                    .setXslName(ProtoUtils.IS_SKU_PARAM)
                    .setBoolValue(true)
                    .build()
            )
            .setParentId(NOT_FOUND_MODEL_ID)
        );
        List<GoodProcessingResult> results = processor.process(Collections.singletonList(
            new GoodMsku(1, MODEL_ID_1)
        ));
        assertSingleFailure(results);
    }

    @Test
    public void failureIfParentModelNotFound() {
        putModel(sku(MODEL_ID_1)
            .addRelations(
                ModelStorage.Relation.newBuilder()
                    .setId(NOT_FOUND_MODEL_ID)
                    .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                    .build()
            )
        );

        List<GoodProcessingResult> results = processor.process(Collections.singletonList(
            new GoodMsku(1, MODEL_ID_1)
        ));
        assertSingleFailure(results);
    }

    @Test
    public void failureIfRootModelOfModificationNotFound() {
        putModel(sku(MODEL_ID_1)
            .addRelations(
                ModelStorage.Relation.newBuilder()
                    .setId(MODEL_ID_2)
                    .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                    .build()
            )
        );
        putModel(model(MODEL_ID_2)
            .setParentId(NOT_FOUND_MODEL_ID)
        );

        List<GoodProcessingResult> results = processor.process(Collections.singletonList(
                new GoodMsku(1, MODEL_ID_1)
        ));
        assertSingleFailure(results);
    }

    private void assertSingleFailure(List<GoodProcessingResult> results) {
        assertThat(results).hasSize(1);
        GoodProcessingResult result = results.get(0);
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getErrorType()).isEqualTo(GoodErrorType.MSKU_HIERARCHY_NOT_FOUND);
    }

    private void putModel(ModelStorage.Model.Builder... models) {
        for (ModelStorage.Model.Builder model : models) {
            modelStorageService.addModel(model.build());
        }
    }
}
