package ru.yandex.market.mbo.db.modelstorage.compatibility.validation;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.common.processing.ProcessingResult;
import ru.yandex.market.mbo.db.modelstorage.validation.CompatibilitiesValidator;
import ru.yandex.market.mbo.db.modelstorage.validation.context.ModelValidationContext;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError;
import ru.yandex.market.mbo.gwt.models.compatibility.CompatibilityModel;
import ru.yandex.market.mbo.gwt.models.compatibility.CompatibilityModelBuilder;
import ru.yandex.market.mbo.gwt.models.compatibility.DuplicateCompatibilitiesError;
import ru.yandex.market.mbo.gwt.models.compatibility.EmptyDirectionError;
import ru.yandex.market.mbo.gwt.models.compatibility.SelfCompatibilitiesError;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class ModelCompatibilityValidationServiceTest {

    private static final CommonModel COMMON_MODEL = CommonModelBuilder.newBuilder().id(555).getModel();
    private static final CommonModel MODEL_TO_DELETE = CommonModelBuilder.newBuilder().id(555).deleted(true).getModel();

    private static final CompatibilityModel NONE_COMPATIBILITY_MODEL = CompatibilityModelBuilder.newBuilder()
            .setModel(101, "Model 101")
            .setDirection(CompatibilityModel.Direction.NONE)
            .create();

    private static final CompatibilityModel FORWARD_COMPATIBILITY_MODEL = CompatibilityModelBuilder.newBuilder()
            .setModel(102, "Model 102")
            .setDirection(CompatibilityModel.Direction.FORWARD)
            .create();

    private static final CompatibilityModel BACKWARD_COMPATIBILITY_MODEL = CompatibilityModelBuilder.newBuilder()
            .setModel(103, "Model 103")
            .setDirection(CompatibilityModel.Direction.BACKWARD)
            .create();

    private static final CompatibilityModel BOTH_COMPATIBILITY_MODEL = CompatibilityModelBuilder.newBuilder()
            .setModel(104, "Model 104")
            .setDirection(CompatibilityModel.Direction.BOTH)
            .create();

    private ModelCompatibilityValidationService modelCompatibilityValidationService;

    @Before
    public void setUp() throws Exception {
        modelCompatibilityValidationService = new ModelCompatibilityValidationService();
    }

    @Test
    public void validateCorrectModelCompatibilities() throws Exception {
        List<CompatibilityModel> compatibilityModels = Arrays.asList(FORWARD_COMPATIBILITY_MODEL,
                BACKWARD_COMPATIBILITY_MODEL, BOTH_COMPATIBILITY_MODEL);

        List<ProcessingResult> errors = modelCompatibilityValidationService
                .validateModelCompatibilities(COMMON_MODEL, compatibilityModels);

        assertTrue(errors.isEmpty());
    }

    @Test
    public void validateEmptyModelCompatibilities() throws Exception {
        List<CompatibilityModel> models = Arrays.asList(NONE_COMPATIBILITY_MODEL, BOTH_COMPATIBILITY_MODEL);

        List<ProcessingResult> errors = modelCompatibilityValidationService
                .validateModelCompatibilities(COMMON_MODEL, models);

        assertEquals(1, errors.size());
        assertThat(errors.get(0), instanceOf(EmptyDirectionError.class));

        EmptyDirectionError error = (EmptyDirectionError) errors.get(0);
        assertEquals(COMMON_MODEL.getId(), error.getModelId1());
        assertEquals(NONE_COMPATIBILITY_MODEL.getModelId(), error.getModelId2());
    }

    @Test
    public void validateDuplicateModelCompatibilities() throws Exception {
        List<CompatibilityModel> models = Arrays.asList(BOTH_COMPATIBILITY_MODEL, BOTH_COMPATIBILITY_MODEL);

        List<ProcessingResult> errors = modelCompatibilityValidationService
                .validateModelCompatibilities(COMMON_MODEL, models);

        assertEquals(1, errors.size());
        assertThat(errors.get(0), instanceOf(DuplicateCompatibilitiesError.class));

        DuplicateCompatibilitiesError error = (DuplicateCompatibilitiesError) errors.get(0);
        assertEquals(COMMON_MODEL.getId(), error.getModelId1());
        assertEquals(BOTH_COMPATIBILITY_MODEL.getModelId(), error.getModelId2());
    }

    @Test
    public void validateSelfLinkModelCompatibilities() throws Exception {
        List<CompatibilityModel> compatibilityModels = Collections.singletonList(CompatibilityModelBuilder.newBuilder()
                .setModel(COMMON_MODEL.getId(), COMMON_MODEL.getTitle())
                .setDirection(CompatibilityModel.Direction.BACKWARD)
                .create());

        List<ProcessingResult> errors = modelCompatibilityValidationService
                .validateModelCompatibilities(COMMON_MODEL, compatibilityModels);

        assertEquals(1, errors.size());
        assertThat(errors.get(0), instanceOf(SelfCompatibilitiesError.class));

        SelfCompatibilitiesError error = (SelfCompatibilitiesError) errors.get(0);
        assertEquals(COMMON_MODEL.getId(), error.getModelId());
    }

    @Test
    public void validateSuccessfulModelDeletion() throws Exception {
        ModelValidationContext validationContext = mock(ModelValidationContext.class);
        when(validationContext.usedInCompatibilities(anyLong())).thenReturn(false);

        CompatibilitiesValidator compatibilitiesValidator = new CompatibilitiesValidator();

        ModelChanges modelChanges = new ModelChanges(COMMON_MODEL, MODEL_TO_DELETE);

        List<ModelValidationError> errors = compatibilitiesValidator.validate(
            validationContext,
            modelChanges,
            Collections.emptyList());

        assertEquals(0, errors.size());
    }

    @Test
    public void validateModelDeletionWithCompatibilityLink() throws Exception {
        ModelValidationContext validationContext = mock(ModelValidationContext.class);
        when(validationContext.usedInCompatibilities(eq(COMMON_MODEL.getId()))).thenReturn(true);

        CompatibilitiesValidator compatibilitiesValidator = new CompatibilitiesValidator();

        ModelChanges modelChanges = new ModelChanges(COMMON_MODEL, MODEL_TO_DELETE);

        List<ModelValidationError> errors = compatibilitiesValidator.validate(
            validationContext,
            modelChanges,
            Collections.emptyList());

        assertEquals(1, errors.size());
        assertEquals(errors.get(0).getType(), ModelValidationError.ErrorType.MODEL_HAS_COMPATIBILITIES);
    }
}
