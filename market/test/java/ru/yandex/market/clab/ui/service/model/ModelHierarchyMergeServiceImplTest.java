package ru.yandex.market.clab.ui.service.model;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.market.clab.common.cache.CachedParamInfo;
import ru.yandex.market.clab.common.cache.CachedValueInfo;
import ru.yandex.market.clab.common.cache.CategoryInfoCache;
import ru.yandex.market.clab.common.cache.ValueType;
import ru.yandex.market.mbo.http.ModelEdit;
import ru.yandex.market.mbo.http.ModelStorage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static ru.yandex.market.clab.common.mbo.ProtoUtils.createHierarchy;
import static ru.yandex.market.clab.common.test.ModelTestUtils.alias;
import static ru.yandex.market.clab.common.test.ModelTestUtils.boolValue;
import static ru.yandex.market.clab.common.test.ModelTestUtils.clabAlias;
import static ru.yandex.market.clab.common.test.ModelTestUtils.clabBoolValue;
import static ru.yandex.market.clab.common.test.ModelTestUtils.clabEnumValue;
import static ru.yandex.market.clab.common.test.ModelTestUtils.clabPickerValue;
import static ru.yandex.market.clab.common.test.ModelTestUtils.clabPicture;
import static ru.yandex.market.clab.common.test.ModelTestUtils.clabSkuPicture;
import static ru.yandex.market.clab.common.test.ModelTestUtils.enumValue;
import static ru.yandex.market.clab.common.test.ModelTestUtils.model;
import static ru.yandex.market.clab.common.test.ModelTestUtils.picker;
import static ru.yandex.market.clab.common.test.ModelTestUtils.pickerValue;
import static ru.yandex.market.clab.common.test.ModelTestUtils.picture;
import static ru.yandex.market.clab.common.test.ModelTestUtils.sku;
import static ru.yandex.market.clab.common.test.ModelTestUtils.skuPicture;

/**
 * @author anmalysh
 * @since 11/29/2018
 */
public class ModelHierarchyMergeServiceImplTest {
    private static final long MODIFIED_TS_1 = 1L;
    private static final long MODIFIED_TS_2 = 2L;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    ModelHierarchyMergeService modelHierarchyMergeService;

    @Mock
    CategoryInfoCache categoryInfoCache;

    @Before
    public void setUp() {
        modelHierarchyMergeService = new ModelHierarchyMergeServiceImpl(categoryInfoCache);
    }

    @Test
    public void testStorageReturnedWithoutEdited() {
        ModelStorage.Model model = model(1L).build();
        ModelStorage.Model modif = model(2L).build();
        ModelStorage.Model sku = sku(3L).build();

        ModelEdit.Hierarchy storage = createHierarchy(model, modif, sku);

        ModelHierarchyMergeResult result = modelHierarchyMergeService.mergeHierarchies(storage, null, null);
        assertThat(result.getWarnings()).isEmpty();
        assertThat(result.getHierarchy()).isEqualTo(storage);
    }

    @Test
    public void testEditedReturnedIfNotChanged() {
        mockParamInfo("Param");
        ModelStorage.Model model = model(1L)
            .setModifiedTs(MODIFIED_TS_1)
            .build();
        ModelStorage.Model modif = model(2L)
            .setModifiedTs(MODIFIED_TS_1)
            .build();
        ModelStorage.Model sku = sku(3L)
            .setModifiedTs(MODIFIED_TS_1)
            .build();

        ModelStorage.Model baseModel = model(1L)
            .setModifiedTs(MODIFIED_TS_1)
            .build();
        ModelStorage.Model baseModif = model(2L)
            .setModifiedTs(MODIFIED_TS_1)
            .build();
        ModelStorage.Model baseSku = sku(3L)
            .setModifiedTs(MODIFIED_TS_1)
            .build();

        ModelStorage.Model editedModel = model(1L)
            .setModifiedTs(MODIFIED_TS_1)
            .addParameterValues(clabEnumValue(1L, "Param1", 2))
            .build();
        ModelStorage.Model editedModif = model(2L)
            .setModifiedTs(MODIFIED_TS_1)
            .build();
        ModelStorage.Model editedSku = sku(3L)
            .setModifiedTs(MODIFIED_TS_1)
            .addParameterValues(clabBoolValue(2L, "Param2", false))
            .build();

        ModelEdit.Hierarchy storage = createHierarchy(model, modif, sku);
        ModelEdit.Hierarchy base = createHierarchy(baseModel, baseModif, baseSku);
        ModelEdit.Hierarchy edited = createHierarchy(editedModel, editedModif, editedSku);

        ModelHierarchyMergeResult result = modelHierarchyMergeService.mergeHierarchies(storage, base, edited);
        assertThat(result.getWarnings()).isEmpty();
        assertThat(result.getHierarchy()).isEqualTo(edited);
    }

    @Test
    public void testMergeParemeters() {
        mockParamInfo("Param");
        ModelStorage.Model model = model(1L)
            .setModifiedTs(MODIFIED_TS_2)
            .addParameterValues(enumValue(1L, "Param1", 2))
            .build();
        ModelStorage.Model editedModel = model(1L)
            .setModifiedTs(MODIFIED_TS_1)
            .addParameterValues(enumValue(1L, "Param1", 2))
            .build();
        ModelStorage.Model baseModel = model(1L)
            .setModifiedTs(MODIFIED_TS_1)
            .addParameterValues(enumValue(1L, "Param1", 2))
            .build();

        ModelStorage.Model modif = model(2L)
            .setModifiedTs(MODIFIED_TS_2)
            .addParameterValues(boolValue(2L, "Param2", false))
            .build();
        ModelStorage.Model editedModif = model(2L)
            .setModifiedTs(MODIFIED_TS_1)
            .addParameterValues(enumValue(3L, "Param3", 2))
            .build();
        ModelStorage.Model baseModif = model(2L)
            .setModifiedTs(MODIFIED_TS_1)
            .addParameterValues(enumValue(3L, "Param3", 2))
            .build();

        ModelStorage.Model sku = sku(3L)
            .setModifiedTs(MODIFIED_TS_2)
            .addParameterValues(enumValue(4L, "Param4", 2))
            .addParameterValues(boolValue(5L, "Param5", true))
            .build();
        ModelStorage.Model editedSku = sku(3L)
            .setModifiedTs(MODIFIED_TS_1)
            .addParameterValues(clabEnumValue(4L, "Param4", 3))
            .addParameterValues(boolValue(5L, "Param5", false))
            .build();
        ModelStorage.Model baseSku = sku(3L)
            .setModifiedTs(MODIFIED_TS_1)
            .addParameterValues(enumValue(4L, "Param4", 4))
            .addParameterValues(boolValue(5L, "Param5", false))
            .build();

        ModelEdit.Hierarchy storage = createHierarchy(model, modif, sku);
        ModelEdit.Hierarchy edited = createHierarchy(editedModel, editedModif, editedSku);
        ModelEdit.Hierarchy base = createHierarchy(baseModel, baseModif, baseSku);

        ModelHierarchyMergeResult result = modelHierarchyMergeService.mergeHierarchies(storage, base, edited);

        assertThat(result.getWarnings())
            .containsExactlyInAnyOrder(ModelEdit.EditInfoWarning.newBuilder()
                .setType(ModelEdit.EditInfoWarningType.EDIT_CONCURRENT_MODIFICATION)
                .setMessage("Значение параметра Param4 изменено на пришедшее из хранилища моделей")
                .build());
        assertThat(result.getHierarchy().getModel().getParameterValuesList())
            .containsExactlyInAnyOrder(
                enumValue(1L, "Param1", 2).build()
            );
        assertThat(result.getHierarchy().getModification().getParameterValuesList())
            .containsExactlyInAnyOrder(
                boolValue(2L, "Param2", false).build()
            );
        assertThat(result.getHierarchy().getSku().getParameterValuesList())
            .containsExactlyInAnyOrder(
                enumValue(4L, "Param4", 2).build(),
                boolValue(5L, "Param5", true).build()
            );
    }

    @Test
    public void testMergeModelPictures() {
        ModelStorage.Model model = model(1L)
            .setModifiedTs(MODIFIED_TS_2)
            .addPictures(picture("puctureurl1", "XL-Picture"))
            .addPictures(picture("puctureurl3", "XL-Picture2"))
            .build();
        ModelStorage.Model editedModel = model(1L)
            .setModifiedTs(MODIFIED_TS_1)
            .addPictures(clabPicture("puctureurl2", "XL-Picture"))
            .addPictures(picture("puctureurl1", "XL-Picture2"))
            .build();
        ModelStorage.Model baseModel = model(1L)
            .setModifiedTs(MODIFIED_TS_1)
            .addPictures(picture("puctureurl1", "XL-Picture"))
            .addPictures(picture("puctureurl1", "XL-Picture2"))
            .build();

        ModelStorage.Model modif = model(2L)
            .setModifiedTs(MODIFIED_TS_2)
            .addPictures(picture("puctureurl4", "XL-Picture4"))
            .build();
        ModelStorage.Model editedModif = model(2L)
            .setModifiedTs(MODIFIED_TS_1)
            .addPictures(clabPicture("puctureurl1", "XL-Picture3"))
            .build();
        ModelStorage.Model baseModif = model(2L)
            .setModifiedTs(MODIFIED_TS_1)
            .addPictures(picture("puctureurl2", "XL-Picture3"))
            .addPictures(picture("puctureurl4", "XL-Picture4"))
            .build();

        ModelEdit.Hierarchy storage = createHierarchy(model, modif, null);
        ModelEdit.Hierarchy edited = createHierarchy(editedModel, editedModif, null);
        ModelEdit.Hierarchy base = createHierarchy(baseModel, baseModif, null);

        ModelHierarchyMergeResult result = modelHierarchyMergeService.mergeHierarchies(storage, base, edited);

        assertThat(result.getWarnings())
            .containsExactlyInAnyOrder(ModelEdit.EditInfoWarning.newBuilder()
                .setType(ModelEdit.EditInfoWarningType.EDIT_CONCURRENT_MODIFICATION)
                .setMessage("Картинка XL-Picture3 изменена на пришедшую из хранилища моделей")
                .build());
        assertThat(result.getHierarchy().getModel().getPicturesList())
            .containsExactlyInAnyOrder(
                clabPicture("puctureurl2", "XL-Picture").build(),
                picture("puctureurl3", "XL-Picture2").build()
            );
        assertThat(result.getHierarchy().getModification().getPicturesList()).isEmpty();
    }

    @Test
    public void testMergeSkuPicturesNotEdited() {
        ModelStorage.Model model = model(1L)
            .setModifiedTs(MODIFIED_TS_2)
            .build();
        ModelStorage.Model editedModel = model(1L)
            .setModifiedTs(MODIFIED_TS_1)
            .build();
        ModelStorage.Model baseModel = model(1L)
            .setModifiedTs(MODIFIED_TS_1)
            .build();

        ModelStorage.Model sku = sku(3L)
            .setModifiedTs(MODIFIED_TS_2)
            .addPictures(skuPicture("url1"))
            .build();
        ModelStorage.Model editedSku = sku(3L)
            .setModifiedTs(MODIFIED_TS_1)
            .addPictures(skuPicture("url2"))
            .addPictures(skuPicture("url3"))
            .build();
        ModelStorage.Model baseSku = sku(3L)
            .setModifiedTs(MODIFIED_TS_1)
            .addPictures(skuPicture("url1"))
            .build();

        ModelEdit.Hierarchy storage = createHierarchy(model, null, sku);
        ModelEdit.Hierarchy edited = createHierarchy(editedModel, null, editedSku);
        ModelEdit.Hierarchy base = createHierarchy(baseModel, null, baseSku);

        ModelHierarchyMergeResult result = modelHierarchyMergeService.mergeHierarchies(storage, base, edited);

        assertThat(result.getWarnings())
            .isEmpty();
        assertThat(result.getHierarchy().getSku().getPicturesList())
            .containsExactlyInAnyOrder(
                skuPicture("url2").build(),
                skuPicture("url3").build()
            );
    }

    @Test
    public void testMergeSkuPicturesEditedRemotelyNotLocally() {
        ModelStorage.Model model = model(1L)
            .setModifiedTs(MODIFIED_TS_2)
            .build();
        ModelStorage.Model editedModel = model(1L)
            .setModifiedTs(MODIFIED_TS_1)
            .build();
        ModelStorage.Model baseModel = model(1L)
            .setModifiedTs(MODIFIED_TS_1)
            .build();

        ModelStorage.Model sku = sku(3L)
            .setModifiedTs(MODIFIED_TS_2)
            .addPictures(skuPicture("url2"))
            .addPictures(skuPicture("url3"))
            .build();
        ModelStorage.Model editedSku = sku(3L)
            .setModifiedTs(MODIFIED_TS_1)
            .addPictures(skuPicture("url1"))
            .build();
        ModelStorage.Model baseSku = sku(3L)
            .setModifiedTs(MODIFIED_TS_1)
            .addPictures(skuPicture("url1"))
            .build();

        ModelEdit.Hierarchy storage = createHierarchy(model, null, sku);
        ModelEdit.Hierarchy edited = createHierarchy(editedModel, null, editedSku);
        ModelEdit.Hierarchy base = createHierarchy(baseModel, null, baseSku);

        ModelHierarchyMergeResult result = modelHierarchyMergeService.mergeHierarchies(storage, base, edited);

        assertThat(result.getWarnings())
            .isEmpty();
        assertThat(result.getHierarchy().getSku().getPicturesList())
            .containsExactlyInAnyOrder(
                skuPicture("url2").build(),
                skuPicture("url3").build()
            );
    }

    @Test
    public void testMergeSkuPicturesEditedRemotelyAndLocally() {
        ModelStorage.Model model = model(1L)
            .setModifiedTs(MODIFIED_TS_2)
            .build();
        ModelStorage.Model editedModel = model(1L)
            .setModifiedTs(MODIFIED_TS_1)
            .build();
        ModelStorage.Model baseModel = model(1L)
            .setModifiedTs(MODIFIED_TS_1)
            .build();

        ModelStorage.Model sku = sku(3L)
            .setModifiedTs(MODIFIED_TS_2)
            .addPictures(skuPicture("url2"))
            .addPictures(skuPicture("url3"))
            .build();
        ModelStorage.Model editedSku = sku(3L)
            .setModifiedTs(MODIFIED_TS_1)
            .addPictures(clabSkuPicture("url4"))
            .build();
        ModelStorage.Model baseSku = sku(3L)
            .setModifiedTs(MODIFIED_TS_1)
            .addPictures(skuPicture("url1"))
            .build();

        ModelEdit.Hierarchy storage = createHierarchy(model, null, sku);
        ModelEdit.Hierarchy edited = createHierarchy(editedModel, null, editedSku);
        ModelEdit.Hierarchy base = createHierarchy(baseModel, null, baseSku);

        ModelHierarchyMergeResult result = modelHierarchyMergeService.mergeHierarchies(storage, base, edited);

        assertThat(result.getWarnings())
            .containsExactlyInAnyOrder(ModelEdit.EditInfoWarning.newBuilder()
                .setType(ModelEdit.EditInfoWarningType.EDIT_CONCURRENT_MODIFICATION)
                .setMessage("Картинки SKU изменены на пришедшей из хранилища моделей")
                .build());
        assertThat(result.getHierarchy().getSku().getPicturesList())
            .containsExactlyInAnyOrder(
                skuPicture("url2").build(),
                skuPicture("url3").build()
            );
    }

    @Test
    public void testMergePickers() {
        mockOptionInfo("Option");
        ModelStorage.Model model = model(1L)
            .setModifiedTs(MODIFIED_TS_2)
            .addParameterValueLinks(pickerValue(1L, "Param1", 2)
                .setImagePicker(picker("url1")))
            .addParameterValueLinks(pickerValue(1L, "Param1", 3)
                .setImagePicker(picker("url2")))
            .addParameterValueLinks(pickerValue(1L, "Param1", 5)
                .setImagePicker(picker("url7")))
            .addParameterValueLinks(pickerValue(1L, "Param1", 6)
                .setImagePicker(picker("url8"))
                .setValueSource(ModelStorage.ModificationSource.CONTENT_LAB))
            .build();
        ModelStorage.Model editedModel = model(1L)
            .setModifiedTs(MODIFIED_TS_1)
            .addParameterValueLinks(pickerValue(1L, "Param1", 2)
                .setImagePicker(picker("url1")))
            .addParameterValueLinks(clabPickerValue(1L, "Param1", 4)
                .setImagePicker(picker("url3")))
            .addParameterValueLinks(clabPickerValue(1L, "Param1", 5)
                .setImagePicker(picker("url8")))
            .addParameterValueLinks(pickerValue(1L, "Param1", 6)
                .setImagePicker(picker("url9"))
                .setValueSource(ModelStorage.ModificationSource.CONTENT_LAB))
            .build();
        ModelStorage.Model baseModel = model(1L)
            .setModifiedTs(MODIFIED_TS_1)
            .addParameterValueLinks(pickerValue(1L, "Param1", 2)
                .setImagePicker(picker("url1")))
            .addParameterValueLinks(pickerValue(1L, "Param1", 5)
                .setImagePicker(picker("url9")))
            .build();

        ModelEdit.Hierarchy storage = createHierarchy(model, null, null);
        ModelEdit.Hierarchy edited = createHierarchy(editedModel, null, null);
        ModelEdit.Hierarchy base = createHierarchy(baseModel, null, null);

        ModelHierarchyMergeResult result = modelHierarchyMergeService.mergeHierarchies(storage, base, edited);

        assertThat(result.getWarnings())
            .containsExactlyInAnyOrder(ModelEdit.EditInfoWarning.newBuilder()
                .setType(ModelEdit.EditInfoWarningType.EDIT_CONCURRENT_MODIFICATION)
                .setMessage("Пикер для значения Option5 изменен на пришедший из хранилища моделей")
                .build());
        assertThat(result.getHierarchy().getModel().getParameterValueLinksList())
            .containsExactlyInAnyOrder(
                pickerValue(1L, "Param1", 2)
                    .setImagePicker(picker("url1")).build(),
                pickerValue(1L, "Param1", 3)
                    .setImagePicker(picker("url2")).build(),
                clabPickerValue(1L, "Param1", 4)
                    .setImagePicker(picker("url3")).build(),
                pickerValue(1L, "Param1", 5)
                    .setImagePicker(picker("url7")).build(),
                pickerValue(1L, "Param1", 6)
                    .setImagePicker(picker("url9"))
                    .setValueSource(ModelStorage.ModificationSource.CONTENT_LAB).build()
            );
    }

    @Test
    public void testMergeAliases() {
        mockOptionInfo("Option");
        ModelStorage.Model model = model(1L)
            .setModifiedTs(MODIFIED_TS_2)
            .addValueAliases(alias(1L, 1L, 2L))
            .addValueAliases(alias(1L, 4L, 5L)
                .setValueSource(ModelStorage.ModificationSource.OPERATOR_CONFIRMED))
            .build();
        ModelStorage.Model editedModel = model(1L)
            .setModifiedTs(MODIFIED_TS_1)
            .addValueAliases(alias(1L, 1L, 2L))
            .addValueAliases(clabAlias(1L, 2L, 3L))
            .addValueAliases(clabAlias(1L, 7L, 8L))
            .build();
        ModelStorage.Model baseModel = model(1L)
            .setModifiedTs(MODIFIED_TS_1)
            .addValueAliases(alias(1L, 1L, 2L))
            .addValueAliases(alias(1L, 2L, 3L))
            .addValueAliases(alias(1L, 4L, 5L))
            .build();

        ModelEdit.Hierarchy storage = createHierarchy(model, null, null);
        ModelEdit.Hierarchy edited = createHierarchy(editedModel, null, null);
        ModelEdit.Hierarchy base = createHierarchy(baseModel, null, null);

        ModelHierarchyMergeResult result = modelHierarchyMergeService.mergeHierarchies(storage, base, edited);

        assertThat(result.getWarnings())
            .containsExactlyInAnyOrder(
                ModelEdit.EditInfoWarning.newBuilder()
                    .setType(ModelEdit.EditInfoWarningType.EDIT_CONCURRENT_MODIFICATION)
                    .setMessage("Алиас Option5 для зачения Option4" +
                        " восстановлен, так как был изменен в хранилище моделей")
                    .build(),
                ModelEdit.EditInfoWarning.newBuilder()
                    .setType(ModelEdit.EditInfoWarningType.EDIT_CONCURRENT_MODIFICATION)
                    .setMessage("Алиас Option3 для зачения Option2" +
                        " удален, так как был удален в хранилище моделей")
                    .build());
        assertThat(result.getHierarchy().getModel().getValueAliasesList())
            .containsExactlyInAnyOrder(
                alias(1L, 1L, 2L).build(),
                alias(1L, 4L, 5L)
                    .setValueSource(ModelStorage.ModificationSource.OPERATOR_CONFIRMED)
                    .build(),
                clabAlias(1L, 7L, 8L).build()
            );
    }

    @Test
    public void testOtherFieldsFromStorageModel() {
        mockParamInfo("Param");
        ModelStorage.Model model = model(1L)
            .setModifiedTs(MODIFIED_TS_2)
            .setPublished(true)
            .build();
        ModelStorage.Model editedModel = model(1L)
            .setModifiedTs(MODIFIED_TS_1)
            .setPublished(false)
            .build();
        ModelStorage.Model baseModel = model(1L)
            .setModifiedTs(MODIFIED_TS_1)
            .setPublished(false)
            .build();

        ModelEdit.Hierarchy storage = createHierarchy(model, null, null);
        ModelEdit.Hierarchy edited = createHierarchy(editedModel, null, null);
        ModelEdit.Hierarchy base = createHierarchy(baseModel, null, null);

        ModelHierarchyMergeResult result = modelHierarchyMergeService.mergeHierarchies(storage, base, edited);

        assertThat(result.getWarnings()).isEmpty();
        assertThat(result.getHierarchy().getModel().getPublished()).isTrue();
        assertThat(result.getHierarchy().getModel().getModifiedTs()).isEqualTo(MODIFIED_TS_2);
    }

    private void mockParamInfo(String prefix) {
        when(categoryInfoCache.getParamInfo(anyLong(), anyLong()))
            .thenAnswer(i -> createParamInfo(prefix + i.getArgument(1)));
    }

    private void mockOptionInfo(String prefix) {
        when(categoryInfoCache.getValueInfo(anyLong(), anyLong()))
            .thenAnswer(i -> cachedValueInfo(prefix + i.getArgument(1)));
    }

    private CachedParamInfo createParamInfo(String name) {
        CachedParamInfo info = new CachedParamInfo();
        info.setName(name);
        info.setType(ValueType.ENUM);
        return info;
    }

    private CachedValueInfo cachedValueInfo(String name) {
        CachedValueInfo info = new CachedValueInfo();
        info.setName(name);
        return info;
    }
}
