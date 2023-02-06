package ru.yandex.market.mboc.common.services.msku.sync;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.assertj.core.internal.FieldByFieldComparator;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mboc.common.msku.KnownMboParams;
import ru.yandex.market.mboc.common.services.modelstorage.ModelConverter;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @since 25.11.2019
 */
@SuppressWarnings("checkstyle:magicnumber")
public class ImportMskuServiceFromMboTest extends BaseImportMskuServiceTest {
    private static final long CATEGORY_ID = 440;

    private static ModelStorage.Relation relationToSku(long skuId) {
        return ModelStorage.Relation.newBuilder()
            .setCategoryId(CATEGORY_ID)
            .setType(ModelStorage.RelationType.SKU_MODEL)
            .setId(skuId)
            .build();
    }

    private static ModelStorage.Relation relationToParent(long modelId) {
        return ModelStorage.Relation.newBuilder()
            .setCategoryId(CATEGORY_ID)
            .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
            .setId(modelId)
            .build();
    }

    private static ModelStorage.Model model(int modelId, String currentType, ModelStorage.Relation... relations) {
        return ModelStorage.Model.newBuilder()
            .setId(modelId)
            .setCategoryId(CATEGORY_ID)
            .setCurrentType(currentType)
            .addAllRelations(List.of(relations))
            .build();
    }

    @Test
    public void testInsertFromMbo() {
        // insert
        Instant modifiedTs = LocalDate.of(2019, Month.MARCH, 14).atStartOfDay()
            .toInstant(ZoneOffset.UTC);
        Model model = nexModel()
            .setCreatedTs(modifiedTs)
            .setModifiedTs(modifiedTs);

        mboModelsService.saveModels(List.of(ModelConverter.reverseConvert(model)));

        ImportMskuService.SyncStats stats = importMskuService.syncWithMbo(List.of(model.getId()));

        assertThat(stats.getRequested()).isEqualTo(1);
        assertThat(stats.getLoaded()).isEqualTo(1);
        assertThat(stats.getUpdated()).isEqualTo(0);
        assertThat(stats.getInserted()).isEqualTo(1);

        assertThat(getAllMskus()).containsExactly(model);
    }

    @Test
    public void testUpdateFromMbo() {
        // insert
        Instant modifiedTs = LocalDate.of(2019, Month.MARCH, 14).atStartOfDay()
            .toInstant(ZoneOffset.UTC);
        Model model = nexModel()
            .setCreatedTs(modifiedTs)
            .setModifiedTs(modifiedTs);

        mboModelsService.saveModels(List.of(ModelConverter.reverseConvert(model)));

        ImportMskuService.SyncStats stats = importMskuService.syncWithMbo(List.of(model.getId()));

        assertThat(stats.getRequested()).isEqualTo(1);
        assertThat(stats.getLoaded()).isEqualTo(1);
        assertThat(stats.getUpdated()).isEqualTo(0);
        assertThat(stats.getInserted()).isEqualTo(1);

        assertThat(getAllMskus()).containsExactly(model);

        // update
        Instant plus1Min = modifiedTs.plus(1, ChronoUnit.MINUTES);
        model = model.setModifiedTs(plus1Min);
        mboModelsService.saveModels(List.of(ModelConverter.reverseConvert(model)));

        stats = importMskuService.syncWithMbo(List.of(model.getId()));

        assertThat(stats.getRequested()).isEqualTo(1);
        assertThat(stats.getLoaded()).isEqualTo(1);
        assertThat(stats.getUpdated()).isEqualTo(1);
        assertThat(stats.getInserted()).isEqualTo(0);

        assertThat(getAllMskus()).containsExactly(model);

        // update
        Instant minus1Min = modifiedTs.minus(1, ChronoUnit.MINUTES);
        model = model.setModifiedTs(minus1Min);
        mboModelsService.saveModels(List.of(ModelConverter.reverseConvert(model)));

        stats = importMskuService.syncWithMbo(List.of(model.getId()));

        assertThat(stats.getRequested()).isEqualTo(1);
        assertThat(stats.getLoaded()).isEqualTo(1);
        assertThat(stats.getUpdated()).isEqualTo(1);
        assertThat(stats.getInserted()).isEqualTo(0);

        // not updated
        assertThat(getAllMskus()).containsExactly(model);
    }

    @Test
    public void testUpdateCategory() {
        // first run
        Model model = nexModel().setCategoryId(1);
        mboModelsService.saveModels(List.of(ModelConverter.reverseConvert(model)));

        importMskuService.syncWithMbo(List.of(model.getId()));

        assertThat(getAllMskus())
            .containsExactlyInAnyOrder(model);

        // second run
        model.setCategoryId(2);
        mboModelsService.saveModels(List.of(ModelConverter.reverseConvert(model)));

        importMskuService.syncWithMbo(List.of(model.getId()));

        assertThat(getAllMskus())
            .containsExactlyInAnyOrder(model);
    }

    @Test
    public void testUpdateTitle() {
        // first run
        Model model = nexModel().setTitle("title#1");
        mboModelsService.saveModels(List.of(ModelConverter.reverseConvert(model)));

        importMskuService.syncWithMbo(List.of(model.getId()));

        assertThat(getAllMskus())
            .containsExactlyInAnyOrder(model);

        // second run
        model.setTitle("title#2");
        mboModelsService.saveModels(List.of(ModelConverter.reverseConvert(model)));

        importMskuService.syncWithMbo(List.of(model.getId()));

        assertThat(getAllMskus()).containsExactlyInAnyOrder(model);
    }

    @Test
    public void testUpdateExpirDate() {
        // insert
        Instant modifiedTs = LocalDate.of(2019, Month.MARCH, 14).atStartOfDay()
            .toInstant(ZoneOffset.UTC);

        // first run
        ModelStorage.Model.Builder protoModel = ModelConverter.reverseConvert(
            nexModel()
                .setTitle("title#1")
                .setCreatedTs(modifiedTs)
                .setModifiedTs(modifiedTs)
        ).toBuilder();

        long paramId = 646313254L;
        ModelStorage.ParameterValue.Builder expirDate = ModelStorage.ParameterValue.newBuilder()
            .setUserId(1)
            .setParamId(paramId) // not same with cargotype
            .setBoolValue(true)
            .setXslName(KnownMboParams.EXPIR_DATE.mboXslName())
            .setValueType(MboParameters.ValueType.BOOLEAN)
            .setTypeId(MboParameters.ValueType.BOOLEAN_VALUE);

        protoModel.clearParameterValues();
        protoModel.addParameterValues(expirDate.build());

        mboModelsService.saveModels(List.of(protoModel.build()));

        ImportMskuService.SyncStats stats = importMskuService.syncWithMbo(List.of(protoModel.getId()));

        assertThat(stats.getRequested()).isEqualTo(1);
        assertThat(stats.getLoaded()).isEqualTo(1);
        assertThat(stats.getUpdated()).isEqualTo(0);
        assertThat(stats.getInserted()).isEqualTo(1);

        Model model1 = ModelConverter.convert(
            protoModel.build(), Set.of(KnownMboParams.EXPIR_DATE.mboXslName()), Set.of(paramId)
        );

        assertThat(getAllMskus()).usingFieldByFieldElementComparator().containsExactlyInAnyOrder(model1);

        // second run
        Instant plus1Min = modifiedTs.plus(1, ChronoUnit.MINUTES);
        expirDate.setBoolValue(!expirDate.getBoolValue()); // invert value
        expirDate.setModificationDate(plus1Min.toEpochMilli());
        protoModel.clearParameterValues();
        protoModel.addParameterValues(expirDate.build());

        Model model2 = ModelConverter.convert(
            protoModel.build(), Set.of(KnownMboParams.EXPIR_DATE.mboXslName()), Set.of(paramId)
        );
        assertThat(model1).usingComparator(new FieldByFieldComparator()).isNotEqualTo(model2);

        mboModelsService.saveModels(List.of(protoModel.build()));
        stats = importMskuService.syncWithMbo(List.of(protoModel.getId()));

        assertThat(stats.getRequested()).isEqualTo(1);
        assertThat(stats.getLoaded()).isEqualTo(1);
        assertThat(stats.getUpdated()).isEqualTo(1);
        assertThat(stats.getInserted()).isEqualTo(0);

        assertThat(getAllMskus()).usingFieldByFieldElementComparator().containsExactly(model2);
    }

    @Test
    public void testSyncModelsShouldDelegateToSku() {
        mboModelsService.saveModels(
            List.of(
                model(1, "SKU", relationToParent(101)),
                model(2, "SKU", relationToParent(3)),
                model(3, "GURU", relationToSku(2)),
                model(7, "SKU", relationToParent(9)),
                model(8, "GURU", relationToParent(101)),
                model(9, "PARTNER", relationToSku(7), relationToSku(10)),
                model(10, "SKU", relationToParent(9))
            )
        );

        importMskuService.syncWithMbo(List.of(1L, 3L, 9L));

        List<Model> allMskus = getAllMskus();
        assertThat(allMskus)
            .extracting(Model::getId)
            .containsExactlyInAnyOrder(1L, 2L, 7L, 10L);
    }

    @Test
    public void eventShouldBeExistOnlyIfUpdatePass() {
        NamedParameterJdbcTemplate mockTemplate = mock(NamedParameterJdbcTemplate.class);
        doThrow(new IllegalStateException("it's a trap!"))
            .when(mockTemplate).batchUpdate(anyString(), any(Map[].class));

        MskuConverter mskuConverter = new MskuConverter(cargoTypeCachingService, Collections.emptySet());
        ImportMskuService importMskuServiceBad = new ImportMskuService(
            mockTemplate, transactionHelper,
            storageKeyValueService, Set.of(), cargoTypeCachingService, mboModelsService,
            mappedMskuChangesQueueService, ytImportOperations, mskuConverter);

        // insert
        Instant modifiedTs = LocalDate.of(2019, Month.MARCH, 14).atStartOfDay()
            .toInstant(ZoneOffset.UTC);
        Model model = nexModel()
            .setCreatedTs(modifiedTs)
            .setModifiedTs(modifiedTs);

        mboModelsService.saveModels(List.of(ModelConverter.reverseConvert(model)));

        ImportMskuService.SyncStats stats = importMskuServiceBad.syncWithMbo(List.of(model.getId()));
        assertThat(stats.getRequested()).isEqualTo(1);
        assertThat(stats.getLoaded()).isEqualTo(1);
        assertThat(stats.getUpdated()).isEqualTo(0);
        assertThat(stats.getInserted()).isEqualTo(0);
    }
}
