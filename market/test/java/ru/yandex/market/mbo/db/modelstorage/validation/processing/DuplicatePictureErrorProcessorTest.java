package ru.yandex.market.mbo.db.modelstorage.validation.processing;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.image.ModelImageSyncService;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError;
import ru.yandex.market.mbo.db.modelstorage.validation.context.ModelValidationContext;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.db.modelstorage.validation.ImageUniquenessValidator.createError;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getGuruBuilder;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getSkuBuilder;

/**
 * @author danfertev
 * @since 12.04.2019
 */
@SuppressWarnings("checkstyle:magicnumber")
public class DuplicatePictureErrorProcessorTest {
    private static final long MODEL_ID1 = 1L;
    private static final long MODEL_ID2 = 2L;
    private static final long UID1 = 11L;
    private static final long UID2 = 22L;

    private static final String XSL_NAME1 = "xslName1";
    private static final String XSL_NAME2 = "xslName2";
    private static final String URL1 = "url1";
    private static final String URL2 = "url2";

    private DuplicatePictureErrorProcessor processor;
    private ModelImageSyncService modelImageSyncService;
    private ModelValidationContext context;
    private Map<Long, CommonModel> modelStorage;

    @Before
    public void setUp() {
        modelImageSyncService = mock(ModelImageSyncService.class);
        processor = new DuplicatePictureErrorProcessor(modelImageSyncService);
        context = mock(ModelValidationContext.class);
        modelStorage = new HashMap<>();

        when(context.getModel(anyLong())).then(args -> {
            long modelId = args.getArgument(0);
            return Optional.ofNullable(modelStorage.get(modelId));
        });
    }

    @Test
    public void testNoModelInSaveGroup() {
        CommonModel model1 = getGuruBuilder().id(MODEL_ID1).getModel();
        CommonModel model2 = getGuruBuilder().id(2L).getModel();
        ModelValidationError error = createError(model1, "", 0L, "", false);
        ValidationErrorProcessorResult result = processor.process(
            context, ModelSaveGroup.fromModels(model2), error);

        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    public void testNoDuplicateModelId() {
        CommonModel model1 = getGuruBuilder().id(MODEL_ID1).getModel();

        ModelValidationError error = error(model1);
        ValidationErrorProcessorResult result = processor.process(
            context, ModelSaveGroup.fromModels(model1), error);

        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    public void testIncorrectDuplicateModelId() {
        CommonModel model1 = getGuruBuilder().id(MODEL_ID1).getModel();

        ModelValidationError error = error(model1).addParam(ModelStorage.ErrorParamName.MODEL_ID, "id");
        ValidationErrorProcessorResult result = processor.process(
            context, ModelSaveGroup.fromModels(model1), error);

        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    public void testNoDuplicateModel() {
        CommonModel model1 = getGuruBuilder().id(MODEL_ID1).getModel();

        ModelValidationError error = createError(model1, "", MODEL_ID2, "", false);
        ValidationErrorProcessorResult result = processor.process(
            context, ModelSaveGroup.fromModels(model1), error);

        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    public void testNoPicture() {
        CommonModel model1 = getGuruBuilder().id(MODEL_ID1).getModel();
        CommonModel model2 = getGuruBuilder().id(MODEL_ID2).getModel();
        addModels(model1, model2);

        ModelValidationError error = createError(model1, "xslName", MODEL_ID2, "", false);
        ValidationErrorProcessorResult result = processor.process(
            context, ModelSaveGroup.fromModels(model1), error);

        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    public void testNoDuplicatePicture() {
        CommonModel model1 = getGuruBuilder()
            .id(MODEL_ID1)
            .picture(XSL_NAME1, URL1)
            .getModel();
        CommonModel model2 = getGuruBuilder()
            .id(MODEL_ID2)
            .picture(XSL_NAME1, URL2)
            .getModel();
        addModels(model1, model2);

        ModelValidationError error = createError(model1, XSL_NAME1, MODEL_ID2, XSL_NAME2, false);
        ValidationErrorProcessorResult result = processor.process(
            context, ModelSaveGroup.fromModels(model1), error);

        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    public void testPictureAlreadyCopied() {
        CommonModel model1 = getGuruBuilder()
            .id(MODEL_ID1)
            .picture(XSL_NAME1, URL1, ModificationSource.AUTO)
            .getModel();
        CommonModel model2 = getGuruBuilder()
            .id(MODEL_ID2)
            .picture(XSL_NAME2, URL1, ModificationSource.COMPUTER_VISION)
            .getModel();
        addModels(model1, model2);

        ModelValidationError error = createError(model1, XSL_NAME1, MODEL_ID2, XSL_NAME2, false);
        ValidationErrorProcessorResult result = processor.process(
            context, ModelSaveGroup.fromModels(model1), error);

        assertThat(result.isSuccess()).isTrue();
        assertThat(model1.getPicture(XSL_NAME1).getModificationSource()).isEqualTo(ModificationSource.AUTO);
        assertThat(model2.getPicture(XSL_NAME2).getModificationSource()).isEqualTo(ModificationSource.COMPUTER_VISION);
    }

    @Test
    public void testSameUrlButOperatorFilledDupeIsReCopied() {
        CommonModel model1 = getGuruBuilder()
            .id(MODEL_ID1)
            .picture(XSL_NAME1, URL1, ModificationSource.OPERATOR_FILLED)
            .getModel();
        CommonModel model2 = getGuruBuilder()
            .id(MODEL_ID2)
            .picture(XSL_NAME2, URL1, ModificationSource.AUTO)
            .getModel();
        addModels(model1, model2);

        ModelValidationError error = createError(model1, XSL_NAME1, MODEL_ID2, XSL_NAME2, false);
        ValidationErrorProcessorResult result = processor.process(
            context, ModelSaveGroup.fromModels(model1), error);

        assertThat(result.isSuccess()).isTrue();
        assertThat(model1.getPicture(XSL_NAME1).getModificationSource()).isEqualTo(ModificationSource.OPERATOR_COPIED);
        assertThat(model2.getPicture(XSL_NAME2).getModificationSource()).isEqualTo(ModificationSource.AUTO);
    }

    @Test
    public void testPictureCopied() {
        CommonModel model1 = getGuruBuilder()
            .id(MODEL_ID1)
            .picture(XSL_NAME1, URL1, 100, 100, URL1, URL1, UID1, ModificationSource.AUTO, 1.0, 1.0, "1", true)
            .getModel();
        CommonModel model2 = getGuruBuilder()
            .id(MODEL_ID2)
            .picture(XSL_NAME2, URL2, 200, 200, URL2, URL2, UID2, ModificationSource.AUTO, 2.0, 2.0, "2", true)
            .getModel();
        addModels(model1, model2);

        ModelValidationError error = createError(model1, XSL_NAME1, MODEL_ID2, XSL_NAME2, false);
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(model1);
        saveGroup.addBeforeModels(Collections.emptyList());
        ValidationErrorProcessorResult result = processor.process(context, saveGroup, error);

        assertThat(result.isSuccess()).isTrue();
        Picture picture1 = model1.getPicture(XSL_NAME1);
        Picture picture2 = model2.getPicture(XSL_NAME2);
        assertPictures(picture1, picture2);
    }

    @Test
    public void testUseSkuPictureIndex() {
        CommonModel model1 = getGuruBuilder()
            .id(MODEL_ID1)
            .picture(XSL_NAME1, URL1, 100, 100, URL1, URL1, UID1, ModificationSource.AUTO, 1.0, 1.0, "1", true)
            .getModel();
        CommonModel model2 = getSkuBuilder(MODEL_ID1)
            .id(MODEL_ID2)
            .picture(URL2, 200, 200, URL2, URL2, UID2, ModificationSource.AUTO, 2.0, 2.0, "2", true)
            .getModel();
        addModels(model1, model2);

        ModelValidationError error = createError(model1, XSL_NAME1, MODEL_ID2, "0", false);
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(model1);
        saveGroup.addBeforeModels(Collections.emptyList());
        ValidationErrorProcessorResult result = processor.process(context, saveGroup, error);

        assertThat(result.isSuccess()).isTrue();
        Picture picture1 = model1.getPicture(XSL_NAME1);
        Picture picture2 = model2.getPictures().get(0);
        assertPictures(picture1, picture2);
    }

    private void addModels(CommonModel... models) {
        for (CommonModel model : models) {
            modelStorage.put(model.getId(), model);
        }
    }

    private static ModelValidationError error(CommonModel model) {
        return new ModelValidationError(model, ModelValidationError.ErrorType.INVALID_IMAGE,
            ModelValidationError.ErrorSubtype.DUPLICATE_PICTURES, true, true);
    }

    private static void assertPictures(Picture picture1, Picture picture2) {
        assertThat(picture1.getUrl()).isEqualTo(picture2.getUrl());
        assertThat(picture1.getUrlSource()).isEqualTo(picture2.getUrlSource());
        assertThat(picture1.getUrlOrig()).isEqualTo(picture2.getUrlOrig());
        assertThat(picture1.getHeight()).isEqualTo(picture2.getHeight());
        assertThat(picture1.getWidth()).isEqualTo(picture2.getWidth());
        assertThat(picture1.getColorness()).isEqualTo(picture2.getColorness());
        assertThat(picture1.getColornessAvg()).isEqualTo(picture2.getColornessAvg());
        assertThat(picture1.getOrigMd5()).isEqualTo(picture2.getOrigMd5());
        assertThat(picture1.isWhiteBackground()).isEqualTo(picture2.isWhiteBackground());
        assertThat(picture1.getLastModificationUid()).isEqualTo(UID1);
        assertThat(picture1.getModificationSource()).isEqualTo(ModificationSource.OPERATOR_COPIED);
    }
}
