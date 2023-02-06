package ru.yandex.market.mbo.image;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import ru.yandex.market.mbo.db.modelstorage.ModelSaveContext;
import ru.yandex.market.mbo.db.modelstorage.StatsModelStorageService;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatusType;
import ru.yandex.market.mbo.db.modelstorage.data.OperationType;
import ru.yandex.market.mbo.db.modelstorage.data.group.GroupOperationStatus;
import ru.yandex.market.mbo.db.modelstorage.image.ModelImageSyncService;
import ru.yandex.market.mbo.db.modelstorage.params.ParameterViewBuilder;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError;
import ru.yandex.market.mbo.export.client.CategoryParametersServiceClientStub;
import ru.yandex.market.mbo.gwt.models.ImageType;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;
import ru.yandex.market.mbo.gwt.models.param.ParameterView;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.history.EntityHistoryHelperProxy;
import ru.yandex.market.mbo.history.model.FieldValue;
import ru.yandex.market.mbo.history.model.Snapshot;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("checkstyle:MagicNumber")
@RunWith(MockitoJUnitRunner.Silent.class)
public class ModelImageServiceCallRegisterActionTest {

    private static final Long USER_ID = 1L;
    private static final Long MODEL_ID = 2L;
    private static final Long PARAM_ID = 3L;
    private static final String PARAM_NAME = "XL-Picture_2";
    private static final String IMAGE_VALUE = "image_value";
    private static final String NEW_IMAGE_VALUE = "new_image_value";
    private static final Long CATEGORY_ID = 123L;
    private static final Long ENTITY_MODIFICATION_TS = 123L;

    @Mock
    private ImageProcessingService imageProcessingService;

    @Mock
    private StatsModelStorageService modelStorageService;

    @Mock
    private EntityHistoryHelperProxy historyHelper;

    private CommonModel model = new CommonModel();

    private ModelImageService imageService;

    private Word word = new Word(Word.DEFAULT_LANG_ID, IMAGE_VALUE);
    private ParameterValue originalParameterValue = new ParameterValue(
            PARAM_ID,
            PARAM_NAME,
            Param.Type.STRING,
            null,
            null,
            null,
            Collections.singletonList(word),
            null
    );

    private List<CategoryParam> categoryParams = new ArrayList<>();

    private ModificationSource modificationSource = ModificationSource.OPERATOR_FILLED;

    @Before
    public void startUp() {
        model.setCurrentType(CommonModel.Source.GURU);
        model.setCategoryId(CATEGORY_ID);
        model.setId(MODEL_ID);
        model.addParameterValue(originalParameterValue);

        setParameter();
        ModelImageSyncService modelImageSyncService = new ModelImageSyncService(CategoryParametersServiceClientStub
            .ofCategory(CATEGORY_ID, categoryParams.stream()
                .map(p -> ParameterViewBuilder.toProto(ParameterView.of(p)))
                .collect(Collectors.toList())
            ));
        imageService = spy(new ModelImageService());
        imageService.setImageProcessingService(imageProcessingService);
        imageService.setStorageService(modelStorageService);
        imageService.setImageSyncService(modelImageSyncService);

        when(imageProcessingService.prepareAndUploadPicture(
            any(ImageUploadContext.class),
            any(),
            anyList()
        )).thenAnswer((Answer<Picture>) invocation -> {
            Picture picture = new Picture();
            picture.setUrl(NEW_IMAGE_VALUE);
            picture.setWidth(100);
            picture.setHeight(100);
            picture.setUrlOrig("http://url.com4");
            picture.setUrlSource("http://url.com4");
            return picture;
        });

        when(modelStorageService.getModelWithParent(anyLong(), anyLong(), any())).thenReturn(Optional.of(model));
        GroupOperationStatus status = new GroupOperationStatus(
            new OperationStatus(OperationStatusType.OK, OperationType.UPLOAD_IMAGE, 1L));
        when(modelStorageService.saveModel(any(CommonModel.class), any(ModelSaveContext.class)))
            .thenReturn(status);

    }

    @Test
    public void testCallRegisterActionNoOparatorFilled() {
        model.removeAllParameterValues(PARAM_NAME);
        modificationSource = ModificationSource.GENERALIZATION;
        setImage();
        verifyRegisterActionNotCalled();
    }

    @Test
    public void testCallRegisterActionIfImageNotSaved() {
        when(imageProcessingService.prepareAndUploadPicture(
                any(ImageUploadContext.class),
                any(),
                anyList()
        )).thenAnswer((Answer<Picture>) invocation -> {
            List<ModelValidationError> errors = invocation.getArgument(2);
            errors.add(
                new ModelValidationError(0L, ModelValidationError.ErrorType.UNKNOWN_IMAGE_ERROR)
                    .addParam(ModelStorage.ErrorParamName.DESCRIPTION, "qwe"));

            return null;
        });
        setImage();
        verifyRegisterActionNotCalled();
    }

    @Test
    public void testToGetModelWithoutSolr() {
        setImage();
        verify(modelStorageService, atLeastOnce()).getModelWithParent(eq(CATEGORY_ID), eq(MODEL_ID), any());
    }

    protected void setImage() {
        ImageUploadContext context = new ImageUploadContext()
            .setUserId(USER_ID)
            .setCategoryId(CATEGORY_ID)
            .setEntityId(MODEL_ID)
            .setModificationSource(modificationSource)
            .setEntityModificationTs(ENTITY_MODIFICATION_TS);
        imageService.setImage(
            context,
            PARAM_NAME,
            new byte[]{},
            "image/png",
            "http://url.com",
            new OperationStatus(OperationStatusType.INTERNAL_ERROR, OperationType.UPLOAD_IMAGE, MODEL_ID)
        );
    }

    protected void setParameter() {
        ImageType type = ImageType.getImageType(PARAM_NAME);
        List<String> xslNames = ImageType.getAllImageParamNames(PARAM_NAME);

        Collection<String> numeric = Arrays.asList(
            type.getWidthParamName(PARAM_NAME),
            type.getHeightParamName(PARAM_NAME)
        );

        for (String xslName : xslNames) {
            Parameter parameter = new Parameter();
            parameter.setId(PARAM_ID);
            parameter.setXslName(xslName);
            parameter.setType(numeric.contains(xslName) ? Param.Type.NUMERIC : Param.Type.STRING);

            categoryParams.add(parameter);
        }
    }

    protected void verifyRegisterActionNotCalled() {
        verify(historyHelper, never())
                .registerAction(anyLong(), any(), anyLong(), any(), any(), any());
    }

    protected ArgumentMatcher<Snapshot> getSnapshotMatcherForMap(String paramName, String imageValue) {
        return argument -> {
            FieldValue fieldValue = argument.get(paramName);
            if (fieldValue == null && imageValue == null) {
                return true;
            }
            if (fieldValue != null) {
                return Objects.equals(fieldValue.getValue(), imageValue);
            }
            return false;
        };
    }


}
