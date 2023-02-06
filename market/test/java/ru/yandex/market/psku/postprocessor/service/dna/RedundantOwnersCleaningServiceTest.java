package ru.yandex.market.psku.postprocessor.service.dna;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import ru.yandex.market.mbo.http.ModelStorage;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.psku.postprocessor.ModelsGenerator.generateModelWithRelations;

public class RedundantOwnersCleaningServiceTest {

    private static final int HID = 91491;
    private static final long EXISTING_MODEL_ID = 200501L;
    private static final long EXISTING_PSKU_ID1 = 100501L;
    private static final long EXISTING_PSKU_ID2 = 100502L;

    private static final long PARAM_ID_1 = 1L;
    private static final long PARAM_ID_2 = 2L;

    private static final int OWNER_ID_1 = 1;
    private static final int OWNER_ID_2 = 2;

    private static final String OPERATOR_MD5 = "dsjfldfku23423gkj3";
    private static final String MD5 = "du23423gkj3";

    private RedundantOwnersCleaningService service = new RedundantOwnersCleaningService();

    @Test
    public void whenRedundantOwnersExistThenCleanUp() {
        ModelStorage.Model sku1 = generateModelWithRelations(EXISTING_PSKU_ID1,
                ModelStorage.RelationType.SKU_PARENT_MODEL,
                Set.of(EXISTING_MODEL_ID), HID)
                .toBuilder()
                .addParameterValues(ModelStorage.ParameterValue
                        .newBuilder()
                        .setParamId(PARAM_ID_1)
                        .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                        .setOwnerId(OWNER_ID_1)
                        .build())
                .addPictures(ModelStorage.Picture
                        .newBuilder()
                        .setOrigMd5(OPERATOR_MD5)
                        .setOwnerId(OWNER_ID_2)
                        .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                        .build()
                )
                .setCurrentType(ModelStorage.ModelType.SKU.name())
                .setSourceType(ModelStorage.ModelType.PARTNER_SKU.name())
                .build();
        ModelStorage.Model sku2 = generateModelWithRelations(EXISTING_PSKU_ID2,
                ModelStorage.RelationType.SKU_PARENT_MODEL,
                Set.of(EXISTING_MODEL_ID), HID)
                .toBuilder()
                .addParameterValues(ModelStorage.ParameterValue
                        .newBuilder()
                        .setParamId(PARAM_ID_2)
                        .setValueSource(ModelStorage.ModificationSource.VENDOR_OFFICE)
                        .setOwnerId(OWNER_ID_2)
                        .build())
                .addPictures(ModelStorage.Picture
                        .newBuilder()
                        .setOrigMd5(MD5)
                        .setOwnerId(OWNER_ID_2)
                        .build()
                )
                .setCurrentType(ModelStorage.ModelType.SKU.name())
                .setSourceType(ModelStorage.ModelType.PARTNER_SKU.name())
                .build();
        ModelStorage.Model model = generateModelWithRelations(EXISTING_MODEL_ID,
                ModelStorage.RelationType.SKU_MODEL,
                Set.of(EXISTING_PSKU_ID1, EXISTING_PSKU_ID2), HID)
                .toBuilder()
                .addParameterValues(ModelStorage.ParameterValue
                        .newBuilder()
                        .setParamId(PARAM_ID_1)
                        .setValueSource(ModelStorage.ModificationSource.VENDOR_OFFICE)
                        .setOwnerId(OWNER_ID_1)
                        .build())
                .setCurrentType(ModelStorage.ModelType.GURU.name())
                .setSourceType(ModelStorage.ModelType.PARTNER.name())
                .build();
        List<ModelStorage.Model> models = List.of(model, sku1, sku2);
        List<ModelStorage.Model> result = service.cleanUpModelsHierarchy(models, Map.of(EXISTING_MODEL_ID,
                Set.of(OWNER_ID_1),
                EXISTING_PSKU_ID1, Set.of(OWNER_ID_1, OWNER_ID_2),
                EXISTING_PSKU_ID2, Set.of(OWNER_ID_2)));
        assertThat(result.get(0).getId()).isEqualTo(EXISTING_MODEL_ID);
        assertThat(result.get(0).getParameterValuesList()).isEmpty();

        assertThat(result.get(1).getId()).isEqualTo(EXISTING_PSKU_ID1);
        assertThat(result.get(1).getParameterValuesList()).hasSize(1);
        assertThat(result.get(1).getPicturesList()).hasSize(1);

        assertThat(result.get(2).getId()).isEqualTo(EXISTING_PSKU_ID2);
        assertThat(result.get(2).getParameterValuesList()).isEmpty();
        assertThat(result.get(2).getPicturesList()).isEmpty();
    }

    @Test
    public void whenMskuDoNotCleanUp() {
        ModelStorage.Model sku1 = generateModelWithRelations(EXISTING_PSKU_ID1,
                ModelStorage.RelationType.SKU_PARENT_MODEL,
                Set.of(EXISTING_MODEL_ID), HID)
                .toBuilder()
                .addParameterValues(ModelStorage.ParameterValue
                        .newBuilder()
                        .setParamId(PARAM_ID_1)
                        .setOwnerId(OWNER_ID_1)
                        .build())
                .addPictures(ModelStorage.Picture
                        .newBuilder()
                        .setOrigMd5(OPERATOR_MD5)
                        .setOwnerId(OWNER_ID_2)
                        .build()
                )
                .setCurrentType(ModelStorage.ModelType.SKU.name())
                .setSourceType(ModelStorage.ModelType.SKU.name())
                .build();
        ModelStorage.Model model = generateModelWithRelations(EXISTING_MODEL_ID,
                ModelStorage.RelationType.SKU_MODEL,
                Set.of(EXISTING_PSKU_ID1, EXISTING_PSKU_ID2), HID)
                .toBuilder()
                .addParameterValues(ModelStorage.ParameterValue
                        .newBuilder()
                        .setParamId(PARAM_ID_1)
                        .setValueSource(ModelStorage.ModificationSource.VENDOR_OFFICE)
                        .setOwnerId(OWNER_ID_1)
                        .build())
                .setCurrentType(ModelStorage.ModelType.GURU.name())
                .setSourceType(ModelStorage.ModelType.GURU.name())
                .build();
        List<ModelStorage.Model> models = List.of(model, sku1);
        List<ModelStorage.Model> result = service.cleanUpModelsHierarchy(models, Map.of(EXISTING_MODEL_ID,
                Set.of(OWNER_ID_1),
                EXISTING_PSKU_ID1, Set.of(OWNER_ID_1, OWNER_ID_2)));
        assertThat(result.get(0).getId()).isEqualTo(EXISTING_MODEL_ID);
        assertThat(result.get(0).getParameterValuesList()).hasSize(1);

        assertThat(result.get(1).getId()).isEqualTo(EXISTING_PSKU_ID1);
        assertThat(result.get(1).getParameterValuesList()).hasSize(1);
        assertThat(result.get(1).getPicturesList()).hasSize(1);
    }
}
