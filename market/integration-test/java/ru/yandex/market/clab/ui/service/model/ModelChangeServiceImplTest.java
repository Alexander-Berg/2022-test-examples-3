package ru.yandex.market.clab.ui.service.model;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import ru.yandex.market.clab.common.db.good.GoodUtils;
import ru.yandex.market.clab.common.mbo.ProtoUtils;
import ru.yandex.market.clab.common.service.cart.CartRepository;
import ru.yandex.market.clab.common.service.category.CategoryRepository;
import ru.yandex.market.clab.common.service.good.GoodRepository;
import ru.yandex.market.clab.common.service.movement.MovementRepository;
import ru.yandex.market.clab.common.service.photo.EditedPhotoRepository;
import ru.yandex.market.clab.common.service.user.User;
import ru.yandex.market.clab.common.service.user.UserService;
import ru.yandex.market.clab.common.test.ModelTestUtils;
import ru.yandex.market.clab.common.test.PhotoTestUtils;
import ru.yandex.market.clab.db.jooq.generated.enums.CartState;
import ru.yandex.market.clab.db.jooq.generated.enums.GoodState;
import ru.yandex.market.clab.db.jooq.generated.enums.MovementDirection;
import ru.yandex.market.clab.db.jooq.generated.enums.MovementState;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Cart;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Category;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.EditedPhoto;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Good;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Movement;
import ru.yandex.market.clab.ui.BaseUiIntegrationTest;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelCardApi;
import ru.yandex.market.mbo.http.ModelCardApi.SaveModelsGroupRequest;
import ru.yandex.market.mbo.http.ModelEdit;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.ModelStorageService;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.clab.common.mbo.ProtoUtils.createWarning;
import static ru.yandex.market.clab.common.mbo.ProtoUtils.toByteArray;
import static ru.yandex.market.clab.common.test.ModelTestUtils.clabEnumValue;
import static ru.yandex.market.clab.common.test.ModelTestUtils.enumOption;
import static ru.yandex.market.clab.common.test.ModelTestUtils.enumParam;
import static ru.yandex.market.clab.common.test.ModelTestUtils.model;
import static ru.yandex.market.clab.common.test.ModelTestUtils.sku;
import static ru.yandex.market.clab.common.test.ModelTestUtils.skuPicture;
import static ru.yandex.market.clab.common.test.PhotoTestUtils.editedPhoto;
import static ru.yandex.market.clab.common.test.PhotoTestUtils.toUploadedToMbo;
import static ru.yandex.market.clab.common.test.assertions.GoodAssert.assertThatGood;

/**
 * @author anmalysh
 * @since 1/18/2019
 */
@SuppressWarnings("checkstyle:magicnumber")
public class ModelChangeServiceImplTest extends BaseUiIntegrationTest {

    private static final long SEED = 12321321321L;

    private static final long MODIFIED_TS_1 = 1L;
    private static final long MODIFIED_TS_2 = 2L;
    private static final long MODIFIED_TS_3 = 3L;
    private static final long VERIFIER_ID = 52881110;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @MockBean(name = "modelStorageServiceWriteUi")
    private ModelStorageService modelStorageServiceMock;

    @Autowired
    private ModelEditService modelEditService;

    @Autowired
    private GoodRepository goodRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private MovementRepository movementRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private EditedPhotoRepository editedPhotoRepository;

    @Autowired
    private UserService userService;

    private EnhancedRandom random;

    private long editorId;

    @Captor
    protected ArgumentCaptor<SaveModelsGroupRequest> saveModelsRequestCaptor;

    @Before
    public void before() {
        random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder().seed(SEED).build();
        createAndSaveCategory(ModelTestUtils.CATEGORY_ID, null);
        User user = new User("integration-test-editor-" + getClass().getSimpleName());
        editorId = userService.save(user).getId();
    }

    @Test
    public void testGetEditInfoNotSavedBefore() {
        ModelEdit.Hierarchy hierarchy = createHierarchy(MODIFIED_TS_1, 1);
        Good good = createAndSaveGood(GoodState.EDITING, hierarchy, null, null, null);
        createAndSavePictures(good, 3, 3);

        ModelEdit.EditInfo editInfo = modelEditService.getEditInfo(good);

        assertThat(editInfo.getHierarchy()).isEqualTo(hierarchy);
        assertThat(editInfo.getDetachedPictureList()).containsExactlyInAnyOrder(
            skuPicture("qwerty").build(),
            skuPicture("qwerty").build(),
            skuPicture("qwerty").build()
        );
        assertThat(editInfo.getWarningList()).isEmpty();

        Good updatedGood = goodRepository.getById(good.getId());

        assertThatGood(updatedGood).hasBaseLastReadHierarchy(hierarchy);

        editInfo = modelEditService.getEditInfo(updatedGood);

        assertThat(editInfo.getHierarchy()).isEqualTo(hierarchy);
        assertThat(editInfo.getWarningList()).isEmpty();
    }

    @Test
    public void testGetEditInfoEditedRemoteNotChanged() {
        ModelEdit.Hierarchy hierarchy1 = createHierarchy(MODIFIED_TS_1, 1);
        ModelEdit.Hierarchy hierarchy2 = createHierarchy(MODIFIED_TS_1, 2);
        Good good = createAndSaveGood(GoodState.EDITING, hierarchy1, hierarchy1, hierarchy1, hierarchy2);
        createAndSavePictures(good, 3, 3);

        ModelEdit.EditInfo editInfo = modelEditService.getEditInfo(good);

        assertThat(editInfo.getHierarchy()).isEqualTo(hierarchy2);
        assertThat(editInfo.getWarningList()).isEmpty();
    }

    @Test
    public void testGetEditInfoEditedRemoteChangedNoConflicts() {
        ModelEdit.Hierarchy hierarchy1 = createHierarchy(MODIFIED_TS_1, 1);
        ModelEdit.Hierarchy hierarchy2 = createHierarchy(MODIFIED_TS_1, 1, 2, 3);
        ModelEdit.Hierarchy hierarchy3 = createHierarchy(MODIFIED_TS_2, 2, 1, 3);
        Good good = createAndSaveGood(GoodState.EDITING, hierarchy3, hierarchy1, hierarchy1, hierarchy2);
        createAndSavePictures(good, 3, 3);

        ModelEdit.EditInfo editInfo = modelEditService.getEditInfo(good);

        ModelEdit.Hierarchy mergedHierarchy = createHierarchy(MODIFIED_TS_2, 2, 2, 3);
        assertThat(editInfo.getHierarchy()).isEqualTo(mergedHierarchy);
        assertThat(editInfo.getWarningList()).isEmpty();
    }

    @Test
    public void testGetEditInfoEditedRemoteChangedConflicts() {
        ModelEdit.Hierarchy hierarchy1 = createHierarchy(MODIFIED_TS_1, 1);
        ModelEdit.Hierarchy hierarchy2 = createHierarchy(MODIFIED_TS_1, 1, 2, 3);
        ModelEdit.Hierarchy hierarchy3 = createHierarchy(MODIFIED_TS_2, 2, 1, 4);
        Good good = createAndSaveGood(GoodState.EDITING, hierarchy3, hierarchy1, hierarchy1, hierarchy2);
        createAndSavePictures(good, 3, 3);

        ModelEdit.EditInfo editInfo = modelEditService.getEditInfo(good);

        ModelEdit.Hierarchy mergedHierarchy = createHierarchy(MODIFIED_TS_2, 2, 2, 4);
        assertThat(editInfo.getHierarchy()).isEqualTo(mergedHierarchy);
        assertThat(editInfo.getWarningList()).hasSize(1);
    }

    @Test
    public void testGetEditInfoNoUploadedPictures() {
        ModelEdit.Hierarchy hierarchy = createHierarchy(MODIFIED_TS_1, 1);
        Good good = createAndSaveGood(GoodState.EDITING, hierarchy, null, null, null);

        ModelEdit.EditInfo editInfo = modelEditService.getEditInfo(good);

        assertThat(editInfo.getHierarchy()).isEqualTo(hierarchy);
        assertThat(editInfo.getWarningList()).containsExactly(
            createWarning("Товар " + GoodUtils.getErrorDesc(good) +
                ": Отретушированные фотографии пока недоступны"));
    }

    @Test
    public void testGetEditInfoNotAllUploadedPictures() {
        ModelEdit.Hierarchy hierarchy = createHierarchy(MODIFIED_TS_1, 1);
        Good good = createAndSaveGood(GoodState.EDITING, hierarchy, null, null, null);
        createAndSavePictures(good, 3, 1);

        ModelEdit.EditInfo editInfo = modelEditService.getEditInfo(good);

        assertThat(editInfo.getHierarchy()).isEqualTo(hierarchy);
        assertThat(editInfo.getWarningList()).containsExactly(
            createWarning("Товар " + GoodUtils.getErrorDesc(good) +
                ": Загружены не все отретушированные фотографии"));
    }

    @Test
    public void testSaveModelNoConflicts() {
        ModelEdit.Hierarchy hierarchy = createHierarchy(MODIFIED_TS_1, 1);
        ModelEdit.Hierarchy hierarchy2 = createHierarchy(MODIFIED_TS_1, 1, 2, 3);
        ModelEdit.Hierarchy hierarchy3 = createHierarchy(MODIFIED_TS_2, 2, 1, 3);
        Good good = createAndSaveGood(GoodState.EDITING, hierarchy3, null, hierarchy, null);
        createAndSavePictures(good, 3, 3);

        ModelEdit.SaveResult result = modelEditService.saveModel(editorId, good, createSaveRequest(hierarchy2));

        ModelEdit.Hierarchy mergedHierarchy = createHierarchy(MODIFIED_TS_2, 2, 2, 3);
        assertThat(result.getHierarchy()).isEqualTo(mergedHierarchy);
        assertThat(result.getType()).isEqualTo(ModelEdit.SaveResultType.SAVE_OK);
        assertThat(result.getStatusesList()).isEmpty();

        Good updatedGood = goodRepository.getById(good.getId());

        assertThatGood(updatedGood).hasStorageHierarchy(hierarchy3);
        assertThatGood(updatedGood).hasBaseHierarchy(hierarchy3);
        assertThatGood(updatedGood).hasEditedHierarchy(mergedHierarchy);
    }

    @Test
    public void testSaveModelConflicts() {
        ModelEdit.Hierarchy hierarchy = createHierarchy(MODIFIED_TS_1, 1);
        ModelEdit.Hierarchy hierarchy2 = createHierarchy(MODIFIED_TS_1, 1, 2, 3);
        ModelEdit.Hierarchy hierarchy3 = createHierarchy(MODIFIED_TS_2, 2, 1, 4);
        Good good = createAndSaveGood(GoodState.EDITING, hierarchy3, hierarchy, hierarchy, null);
        createAndSavePictures(good, 3, 3);

        ModelEdit.SaveResult result = modelEditService.saveModel(editorId, good, createSaveRequest(hierarchy2));

        ModelEdit.Hierarchy mergedHierarchy = createHierarchy(MODIFIED_TS_2, 2, 2, 4);
        assertThat(result.getHierarchy()).isEqualTo(mergedHierarchy);
        assertThat(result.getType()).isEqualTo(ModelEdit.SaveResultType.SAVE_ERROR);
        assertThat(result.getStatusesList()).hasSize(1);

        Good updatedGood = goodRepository.getById(good.getId());

        assertThatGood(updatedGood).hasStorageHierarchy(hierarchy3);
        assertThatGood(updatedGood).hasBaseHierarchy(hierarchy);
        assertThatGood(updatedGood).hasBaseLastReadHierarchy(hierarchy3);
    }

    @Test
    public void testSaveWithWrongModelId() {
        ModelEdit.Hierarchy hierarchy = createHierarchy(MODIFIED_TS_1, 1);
        ModelEdit.Hierarchy hierarchy2 = createHierarchy(MODIFIED_TS_1, 1, 2, 3);
        ModelEdit.Hierarchy hierarchy3 = createHierarchy(MODIFIED_TS_2, 2, 1, 4);
        Good good = createAndSaveGood(GoodState.EDITING, hierarchy3, hierarchy, hierarchy, null);
        createAndSavePictures(good, 3, 3);

        // change id of model, to validate test
        ModelEdit.Hierarchy nonvalid = hierarchy2.toBuilder()
            .setModel(hierarchy2.getModel().toBuilder()
                .setId(100500).build())
            .build();

        Assertions.assertThatThrownBy(() -> {
            modelEditService.saveModel(editorId, good, createSaveRequest(nonvalid));
        })
            .hasMessageContaining("SYSTEM ERROR! Edited model-id (100500) doesn't equal to storage model-id (1)");
    }

    @Test
    public void testFinishByVerifierModelNoConflicts() {
        Movement movement = createAndSaveMovement();
        Cart cart = createAndSaveCart();
        ModelEdit.Hierarchy hierarchy = createHierarchy(MODIFIED_TS_1, 1);
        ModelEdit.Hierarchy hierarchy2 = createHierarchy(MODIFIED_TS_1, 1, 2, 3);
        ModelEdit.Hierarchy hierarchy3 = createHierarchy(MODIFIED_TS_2, 2, 1, 3);
        Good good = createAndSaveGood(GoodState.VERIFYING, movement.getId(), cart.getId(),
            hierarchy3, hierarchy, hierarchy, hierarchy2);
        createAndSavePictures(good, 3, 3);
        mockModelStorageSuccess();

        ModelEdit.SaveResult result = modelEditService.finishByVerifier(good, false, VERIFIER_ID);

        ModelEdit.Hierarchy mergedHierarchy = createHierarchy(MODIFIED_TS_2, 2, 2, 3);
        assertThat(result.getHierarchy()).isEqualTo(mergedHierarchy);
        assertThat(result.getType()).isEqualTo(ModelEdit.SaveResultType.SAVE_OK);
        assertThat(result.getStatusesList()).isEmpty();

        Good updatedGood = goodRepository.getById(good.getId());

        assertThatGood(updatedGood).hasStorageHierarchy(hierarchy3);
        assertThatGood(updatedGood).hasBaseHierarchy(hierarchy3);
        assertThatGood(updatedGood).hasEditedHierarchy(mergedHierarchy);

        Cart updatedCart = cartRepository.getById(cart.getId());
        assertThat(updatedCart.getState()).isEqualTo(CartState.OUT);

        Movement updatedMovement = movementRepository.getById(movement.getId());
        assertThat(updatedMovement.getState()).isEqualTo(MovementState.PROCESSED);

        SaveModelsGroupRequest groupRequest = saveModelsRequestCaptor.getValue();
        assertThat(groupRequest.getModelsRequestList()).hasSize(1);
        ModelStorage.SaveModelsRequest request = groupRequest.getModelsRequestList().get(0);
        ModelEdit.Hierarchy mergedAndPublishedHierarchy =
            createPublishedHierarchy(MODIFIED_TS_2, 2, 2, 3);
        assertThat(request.getModelsList()).containsExactlyInAnyOrder(
            mergedAndPublishedHierarchy.getModel(),
            mergedAndPublishedHierarchy.getModification(),
            mergedAndPublishedHierarchy.getSku()
        );
    }

    @Test
    public void testFinishByEditor() {
        Movement movement = createAndSaveMovement();
        Cart cart = createAndSaveCart();
        ModelEdit.Hierarchy hierarchy1 = createHierarchy(MODIFIED_TS_1, 1);
        ModelEdit.Hierarchy hierarchy2 = createHierarchy(MODIFIED_TS_1, 1, 2, 3);
        ModelEdit.Hierarchy hierarchy3 = createHierarchy(MODIFIED_TS_2, 2, 1, 3);
        Good good = createAndSaveGood(GoodState.EDITING, movement.getId(), cart.getId(),
            hierarchy3, hierarchy1, hierarchy1, hierarchy2);
        createAndSavePictures(good, 3, 3);
        mockModelStorageSuccess();

        ModelEdit.SaveResult result = modelEditService.finishByEditor(good, false);
        assertThat(result.getType()).isEqualTo(ModelEdit.SaveResultType.SAVE_OK);
        ArgumentCaptor<SaveModelsGroupRequest> requestCaptor = ArgumentCaptor.forClass(SaveModelsGroupRequest.class);
        verify(modelStorageServiceMock).saveModelsGroup(requestCaptor.capture());

        assertThat(requestCaptor.getValue().getModelsRequestList()).hasSize(1);
        assertThat(requestCaptor.getValue().getModelsRequestList()).allMatch(r -> !r.getWriteChanges());
    }

    @Test
    public void testReturnToEditor() {
        ModelEdit.Hierarchy hierarchy = createHierarchy(MODIFIED_TS_1, 1);
        Good good = createAndSaveGood(GoodState.VERIFYING, -1, -1,
            hierarchy, hierarchy, hierarchy, hierarchy);
        final String comment = "don't!";

        modelEditService.returnToEditor(Collections.singleton(new GoodComment(good.getId(), comment)), VERIFIER_ID);

        Good updated = goodRepository.getById(good.getId());
        assertThat(updated.getState()).isEqualTo(GoodState.EDITING);
        verify(modelStorageServiceMock, never()).saveModelsGroup(any());
        verify(modelStorageServiceMock, never()).saveModels(any());

        ModelEdit.EditInfo editInfo = modelEditService.getEditInfo(updated);
        assertThat(editInfo.getComment()).isEqualTo(comment);
    }

    @Test
    public void testFinishByVerifierModelConflicts() {
        Movement movement = createAndSaveMovement();
        Cart cart = createAndSaveCart();
        ModelEdit.Hierarchy hierarchy = createHierarchy(MODIFIED_TS_1, 1);
        ModelEdit.Hierarchy hierarchy2 = createHierarchy(MODIFIED_TS_1, 1, 2, 3);
        ModelEdit.Hierarchy hierarchy3 = createHierarchy(MODIFIED_TS_2, 2, 1, 4);
        Good good = createAndSaveGood(GoodState.EDITING, movement.getId(), cart.getId(),
            hierarchy3, hierarchy, hierarchy, hierarchy2);
        createAndSavePictures(good, 3, 3);

        ModelEdit.SaveResult result = modelEditService.finishByVerifier(good, false, VERIFIER_ID);

        ModelEdit.Hierarchy mergedHierarchy = createHierarchy(MODIFIED_TS_2, 2, 2, 4);
        assertThat(result.getHierarchy()).isEqualTo(mergedHierarchy);
        assertThat(result.getType()).isEqualTo(ModelEdit.SaveResultType.SAVE_ERROR);
        assertThat(result.getStatusesList()).hasSize(1);

        Good currentGood = goodRepository.getById(good.getId());

        assertThatGood(currentGood).hasStorageHierarchy(hierarchy3);
        assertThatGood(currentGood).hasBaseHierarchy(hierarchy3);
        assertThatGood(currentGood).hasBaseLastReadHierarchy(hierarchy3);
        assertThatGood(currentGood).hasEditedHierarchy(mergedHierarchy);

        Cart updatedCart = cartRepository.getById(cart.getId());
        assertThat(updatedCart.getState()).isEqualTo(CartState.EDITOR);

        Movement updatedMovement = movementRepository.getById(movement.getId());
        assertThat(updatedMovement.getState()).isEqualTo(MovementState.SORTED_TO_CARTS);
    }

    @Test
    public void testFinishByVerifierModelMboSaveError() {
        Movement movement = createAndSaveMovement();
        Cart cart = createAndSaveCart();
        ModelEdit.Hierarchy hierarchy = createHierarchy(MODIFIED_TS_1, 1);
        ModelEdit.Hierarchy hierarchy2 = createHierarchy(MODIFIED_TS_1, 1, 2, 3);
        ModelEdit.Hierarchy hierarchy3 = createHierarchy(MODIFIED_TS_2, 2, 1, 3);
        Good good = createAndSaveGood(GoodState.VERIFYING,
            movement.getId(), cart.getId(), hierarchy3, hierarchy, hierarchy, hierarchy2);
        createAndSavePictures(good, 3, 3);
        ModelStorage.OperationStatus failureStatus = ModelStorage.OperationStatus.newBuilder()
            .setModelId(1L)
            .setType(ModelStorage.OperationType.CHANGE)
            .setStatus(ModelStorage.OperationStatusType.VALIDATION_ERROR)
            .addValidationError(ModelStorage.ValidationError.newBuilder()
                .setModelId(1L)
                .setType(ModelStorage.ValidationErrorType.INVALID_PARAMETER_VALUE)
                .setCritical(true)
                .setAllowForce(false)
                .addLocalizedMessage(ProtoUtils.defaultLocalizedString("Секая ошибка")))
            .setStatusMessage("Some validation error occurred")
            .build();
        mockModelStorageFailure(failureStatus);

        ModelEdit.SaveResult result = modelEditService.finishByVerifier(good, false, VERIFIER_ID);

        ModelEdit.Hierarchy mergedHierarchy = createHierarchy(MODIFIED_TS_2, 2, 2, 3);
        assertThat(result.getHierarchy()).isEqualTo(mergedHierarchy);
        assertThat(result.getType()).isEqualTo(ModelEdit.SaveResultType.SAVE_ERROR);
        assertThat(result.getStatusesList()).containsExactly(ModelEdit.SaveModelStatus.newBuilder()
            .setStatus(failureStatus)
            .setMessage("Товар " + GoodUtils.getErrorDesc(good) +
                ": Ошибка валидации модели 1. Секая ошибка.")
            .build());

        Good currentGood = goodRepository.getById(good.getId());

        assertThatGood(currentGood).hasStorageHierarchy(hierarchy3);
        assertThatGood(currentGood).hasBaseHierarchy(hierarchy);
        assertThatGood(currentGood).hasBaseLastReadHierarchy(hierarchy3);
        assertThatGood(currentGood).hasEditedHierarchy(hierarchy2);

        Cart updatedCart = cartRepository.getById(cart.getId());
        assertThat(updatedCart.getState()).isEqualTo(CartState.EDITOR);

        Movement updatedMovement = movementRepository.getById(movement.getId());
        assertThat(updatedMovement.getState()).isEqualTo(MovementState.SORTED_TO_CARTS);
    }

    @Test
    public void testFinishByVerifierModelMboConfirmationRequired() {
        Movement movement = createAndSaveMovement();
        Cart cart = createAndSaveCart();
        ModelEdit.Hierarchy hierarchy = createHierarchy(MODIFIED_TS_1, 1);
        ModelEdit.Hierarchy hierarchy2 = createHierarchy(MODIFIED_TS_1, 1, 2, 3);
        ModelEdit.Hierarchy hierarchy3 = createHierarchy(MODIFIED_TS_2, 2, 1, 3);
        Good good = createAndSaveGood(GoodState.EDITING, movement.getId(), cart.getId(),
            hierarchy3, hierarchy, hierarchy, hierarchy2);
        createAndSavePictures(good, 3, 3);
        ModelStorage.OperationStatus failureStatus = ModelStorage.OperationStatus.newBuilder()
            .setModelId(1L)
            .setType(ModelStorage.OperationType.CHANGE)
            .setStatus(ModelStorage.OperationStatusType.VALIDATION_ERROR)
            .addValidationError(ModelStorage.ValidationError.newBuilder()
                .setModelId(1L)
                .setType(ModelStorage.ValidationErrorType.ALIAS_HAS_EOL)
                .setCritical(true)
                .setAllowForce(true)
                .addLocalizedMessage(ProtoUtils.defaultLocalizedString("Какая то ошибка")))
            .setStatusMessage("Some validation error occurred")
            .build();
        mockModelStorageFailure(failureStatus);

        ModelEdit.SaveResult result = modelEditService.finishByVerifier(good, false, VERIFIER_ID);

        ModelEdit.Hierarchy mergedHierarchy = createHierarchy(MODIFIED_TS_2, 2, 2, 3);
        assertThat(result.getHierarchy()).isEqualTo(mergedHierarchy);
        assertThat(result.getType()).isEqualTo(ModelEdit.SaveResultType.SAVE_REQUIRES_CONFIRM);
        assertThat(result.getStatusesList()).containsExactly(ModelEdit.SaveModelStatus.newBuilder()
            .setStatus(failureStatus)
            .setMessage("Товар " + GoodUtils.getErrorDesc(good) +
                ": Предупреждение по модели 1. Какая то ошибка.")
            .build());

        Good currentGood = goodRepository.getById(good.getId());

        assertThatGood(currentGood).hasStorageHierarchy(hierarchy3);
        assertThatGood(currentGood).hasBaseHierarchy(hierarchy);
        assertThatGood(currentGood).hasBaseLastReadHierarchy(hierarchy3);
        assertThatGood(currentGood).hasEditedHierarchy(hierarchy2);

        Cart updatedCart = cartRepository.getById(cart.getId());
        assertThat(updatedCart.getState()).isEqualTo(CartState.EDITOR);

        Movement updatedMovement = movementRepository.getById(movement.getId());
        assertThat(updatedMovement.getState()).isEqualTo(MovementState.SORTED_TO_CARTS);
    }

    @Test
    public void testFinishByVerifierModelMboConcurrentModification() {
        Movement movement = createAndSaveMovement();
        Cart cart = createAndSaveCart();
        ModelEdit.Hierarchy hierarchy = createHierarchy(MODIFIED_TS_1, 1);
        ModelEdit.Hierarchy hierarchy2 = createHierarchy(MODIFIED_TS_1, 1, 2, 3);
        ModelEdit.Hierarchy hierarchy3 = createHierarchy(MODIFIED_TS_2, 2, 1, 3);
        Good good = createAndSaveGood(GoodState.VERIFYING, movement.getId(), cart.getId(),
            hierarchy3, hierarchy, hierarchy, hierarchy2);
        createAndSavePictures(good, 3, 3);
        ModelEdit.Hierarchy newStorageHierarchy =
            createHierarchy(MODIFIED_TS_3, 4, 2, 3);
        mockModelStorageConcurrentModification(
            newStorageHierarchy,
            ModelStorage.OperationStatus.newBuilder()
                .setStatus(ModelStorage.OperationStatusType.OK)
                .setType(ModelStorage.OperationType.CHANGE)
                .build()
        );

        ModelEdit.SaveResult result = modelEditService.finishByVerifier(good, false, VERIFIER_ID);
        verify(modelStorageServiceMock, times(2)).saveModelsGroup(any());

        ModelEdit.Hierarchy mergedHierarchy = createHierarchy(MODIFIED_TS_3, 4, 2, 3);
        assertThat(result.getHierarchy()).isEqualTo(mergedHierarchy);
        assertThat(result.getType()).isEqualTo(ModelEdit.SaveResultType.SAVE_OK);

        Good updatedGood = goodRepository.getById(good.getId());

        assertThatGood(updatedGood).hasStorageHierarchy(newStorageHierarchy);
        assertThatGood(updatedGood).hasBaseHierarchy(newStorageHierarchy);
        assertThatGood(updatedGood).hasEditedHierarchy(mergedHierarchy);

        Cart updatedCart = cartRepository.getById(cart.getId());
        assertThat(updatedCart.getState()).isEqualTo(CartState.OUT);

        Movement updatedMovement = movementRepository.getById(movement.getId());
        assertThat(updatedMovement.getState()).isEqualTo(MovementState.PROCESSED);
    }

    @Test
    public void testFinishByVerifierModelMboConcurrentModificationConflict() {
        Movement movement = createAndSaveMovement();
        Cart cart = createAndSaveCart();
        ModelEdit.Hierarchy hierarchy = createHierarchy(MODIFIED_TS_1, 1);
        ModelEdit.Hierarchy hierarchy2 = createHierarchy(MODIFIED_TS_1, 1, 2, 3);
        ModelEdit.Hierarchy hierarchy3 = createHierarchy(MODIFIED_TS_2, 2, 1, 3);
        Good good = createAndSaveGood(GoodState.EDITING, movement.getId(), cart.getId(),
            hierarchy3, hierarchy, hierarchy, hierarchy2);
        createAndSavePictures(good, 3, 3);
        ModelEdit.Hierarchy newStorageHierarchy =
            createHierarchy(MODIFIED_TS_3, 2, 3, 3);
        mockModelStorageConcurrentModification(
            newStorageHierarchy,
            ModelStorage.OperationStatus.newBuilder()
                .setType(ModelStorage.OperationType.CHANGE)
                .setStatus(ModelStorage.OperationStatusType.OK)
                .build()
        );

        ModelEdit.SaveResult result = modelEditService.finishByVerifier(good, false, VERIFIER_ID);
        verify(modelStorageServiceMock, times(1)).saveModelsGroup(any());

        ModelEdit.Hierarchy mergedHierarchy = createHierarchy(MODIFIED_TS_3, 2, 3, 3);
        assertThat(result.getHierarchy()).isEqualTo(mergedHierarchy);
        assertThat(result.getType()).isEqualTo(ModelEdit.SaveResultType.SAVE_ERROR);
        assertThat(result.getStatusesList()).hasSize(1);

        Good currentGood = goodRepository.getById(good.getId());

        assertThatGood(currentGood).hasStorageHierarchy(newStorageHierarchy);
        assertThatGood(currentGood).hasBaseHierarchy(newStorageHierarchy);
        assertThatGood(currentGood).hasBaseLastReadHierarchy(newStorageHierarchy);
        assertThatGood(currentGood).hasEditedHierarchy(mergedHierarchy);

        Cart updatedCart = cartRepository.getById(cart.getId());
        assertThat(updatedCart.getState()).isEqualTo(CartState.EDITOR);

        Movement updatedMovement = movementRepository.getById(movement.getId());
        assertThat(updatedMovement.getState()).isEqualTo(MovementState.SORTED_TO_CARTS);
    }

    @Test
    public void testFinishByVerifierModelMboConcurrentModificationThenError() {
        Movement movement = createAndSaveMovement();
        Cart cart = createAndSaveCart();
        ModelEdit.Hierarchy hierarchy = createHierarchy(MODIFIED_TS_1, 1);
        ModelEdit.Hierarchy hierarchy2 = createHierarchy(MODIFIED_TS_1, 1, 2, 3);
        ModelEdit.Hierarchy hierarchy3 = createHierarchy(MODIFIED_TS_2, 2, 1, 3);
        Good good = createAndSaveGood(GoodState.EDITING, movement.getId(), cart.getId(),
            hierarchy3, hierarchy, hierarchy, hierarchy2);
        createAndSavePictures(good, 3, 3);
        ModelEdit.Hierarchy newStorageHierarchy =
            createHierarchy(MODIFIED_TS_3, 4, 2, 3);
        ModelStorage.OperationStatus errorStatus = ModelStorage.OperationStatus.newBuilder()
            .setType(ModelStorage.OperationType.CHANGE)
            .setStatus(ModelStorage.OperationStatusType.INTERNAL_ERROR)
            .setStatusMessage("Some internal error occurred")
            .addLocalizedMessage(ProtoUtils.defaultLocalizedString("Внутренняя ошибка"))
            .build();
        mockModelStorageConcurrentModification(newStorageHierarchy, errorStatus);

        ModelEdit.SaveResult result = modelEditService.finishByVerifier(good, false, VERIFIER_ID);
        verify(modelStorageServiceMock, times(2)).saveModelsGroup(any());

        ModelEdit.Hierarchy mergedHierarchy = createHierarchy(MODIFIED_TS_3, 4, 2, 3);
        assertThat(result.getHierarchy()).isEqualTo(mergedHierarchy);
        assertThat(result.getType()).isEqualTo(ModelEdit.SaveResultType.SAVE_ERROR);
        assertThat(result.getStatusesList()).containsExactly(ModelEdit.SaveModelStatus.newBuilder()
            .setStatus(errorStatus)
            .setMessage("Товар " + GoodUtils.getErrorDesc(good) +
                ": Внутренняя ошибка.")
            .build());

        Good currentGood = goodRepository.getById(good.getId());

        ModelEdit.Hierarchy firstMergeHierarchy = createHierarchy(MODIFIED_TS_2, 2, 2, 3);
        assertThatGood(currentGood).hasStorageHierarchy(newStorageHierarchy);
        assertThatGood(currentGood).hasBaseHierarchy(hierarchy3);
        assertThatGood(currentGood).hasBaseLastReadHierarchy(newStorageHierarchy);
        assertThatGood(currentGood).hasEditedHierarchy(firstMergeHierarchy);

        Cart updatedCart = cartRepository.getById(cart.getId());
        assertThat(updatedCart.getState()).isEqualTo(CartState.EDITOR);

        Movement updatedMovement = movementRepository.getById(movement.getId());
        assertThat(updatedMovement.getState()).isEqualTo(MovementState.SORTED_TO_CARTS);
    }

    @Test
    public void testSendToYangTaskModelNoConflicts() {
        Movement movement = createAndSaveMovement();
        Cart cart = createAndSaveCart();
        Category category = createAndSaveCategory(random.nextLong(), true);
        ModelEdit.Hierarchy hierarchy = createHierarchy(MODIFIED_TS_1, 1);
        ModelEdit.Hierarchy hierarchy2 = createHierarchy(MODIFIED_TS_1, 1, 2, 3);
        ModelEdit.Hierarchy hierarchy3 = createHierarchy(MODIFIED_TS_2, 2, 1, 3);
        Good good = createAndSaveGood(GoodState.VERIFYING, movement.getId(), cart.getId(), category.getId(),
            hierarchy3, hierarchy, hierarchy, hierarchy2);
        createAndSavePictures(good, 3, 3);
        mockModelStorageSuccess();

        ModelEdit.SaveResult result = modelEditService.sendToYangTask(good, false, VERIFIER_ID);
        ModelEdit.Hierarchy mergedHierarchy = createHierarchy(MODIFIED_TS_2, 2, 2, 3);
        assertThat(result.getHierarchy()).isEqualTo(mergedHierarchy);
        assertThat(result.getType()).isEqualTo(ModelEdit.SaveResultType.SAVE_OK);
        assertThat(result.getStatusesList()).isEmpty();

        Good updatedGood = goodRepository.getById(good.getId());
        assertThat(updatedGood.getState()).isEqualTo(GoodState.YANG_TASK_READY);
        assertThatGood(updatedGood).hasStorageHierarchy(hierarchy3);
        assertThatGood(updatedGood).hasBaseHierarchy(hierarchy3);
        assertThatGood(updatedGood).hasEditedHierarchy(mergedHierarchy);

        Cart updatedCart = cartRepository.getById(cart.getId());
        assertThat(updatedCart.getState()).isEqualTo(CartState.EDITOR);

        Movement updatedMovement = movementRepository.getById(movement.getId());
        assertThat(updatedMovement.getState()).isEqualTo(MovementState.SORTED_TO_CARTS);
    }

    @Test
    public void testSendToYangTaskGoodFromInvalidCategory() {
        Movement movement = createAndSaveMovement();
        Cart cart = createAndSaveCart();
        ModelEdit.Hierarchy hierarchy = createHierarchy(MODIFIED_TS_1, 1);
        Good good = createAndSaveGood(GoodState.VERIFYING, movement.getId(), cart.getId(),
            hierarchy, hierarchy, hierarchy, hierarchy);
        createAndSavePictures(good, 3, 3);
        mockModelStorageSuccess();

        ModelEdit.SaveResult result = modelEditService.sendToYangTask(good, false, VERIFIER_ID);

        assertThat(result.getType()).isEqualTo(ModelEdit.SaveResultType.SAVE_ERROR);
        assertThat(result.getStatusesList().size()).isEqualTo(1);
        assertThat(result.getStatuses(0).getMessage())
            .isEqualTo("Товар (" + good.getId() + "): Товар принадлежащий категории " +
                good.getCategoryId() + " не может быть отправлен в задание Янга");
    }

    private ModelEdit.Hierarchy createHierarchy(Long modifiedTs, int value) {
        return createHierarchy(modifiedTs, value, value, value);
    }

    private ModelEdit.Hierarchy createPublishedHierarchy(
        Long modifiedTs, int modelValue, int modifValue, int skuValue) {

        return createHierarchy(modifiedTs, modelValue, modifValue, skuValue, true);
    }

    private ModelEdit.Hierarchy createHierarchy(
        Long modifiedTs, int modelValue, int modifValue, int skuValue) {

        return createHierarchy(modifiedTs, modelValue, modifValue, skuValue, false);
    }

    private ModelEdit.Hierarchy createHierarchy(
        Long modifiedTs, int modelValue, int modifValue, int skuValue, boolean published) {

        ModelStorage.Model model = model(1L)
            .setModifiedTs(modifiedTs)
            .setBluePublished(published)
            .addParameterValues(clabEnumValue(1L, "Param1", 10 + modelValue))
            .build();
        ModelStorage.Model modif = model(2L)
            .setModifiedTs(modifiedTs)
            .setPublished(published)
            .addParameterValues(clabEnumValue(2L, "Param2", 20 + modifValue))
            .build();
        ModelStorage.Model sku = sku(3L)
            .setModifiedTs(modifiedTs)
            .setPublished(published)
            .addParameterValues(clabEnumValue(3L, "Param3", 30 + skuValue))
            .build();
        return ProtoUtils.createHierarchy(model, modif, sku);
    }

    private Good createAndSaveGood(GoodState state, ModelEdit.Hierarchy storageHierarchy,
                                   ModelEdit.Hierarchy baseHierarchy,
                                   ModelEdit.Hierarchy baseLastReadHierarchy,
                                   ModelEdit.Hierarchy editedHierarchy) {
        return createAndSaveGood(state, 3L, 2L,
            storageHierarchy, baseHierarchy, baseLastReadHierarchy, editedHierarchy);
    }

    private Good createAndSaveGood(GoodState state, long movementId,
                                   long cartId,
                                   ModelEdit.Hierarchy storageHierarchy,
                                   ModelEdit.Hierarchy baseHierarchy,
                                   ModelEdit.Hierarchy baseLastReadHierarchy,
                                   ModelEdit.Hierarchy editedHierarchy) {
        return createAndSaveGood(state, movementId, cartId, ModelTestUtils.CATEGORY_ID, storageHierarchy,
            baseHierarchy, baseLastReadHierarchy, editedHierarchy);
    }

    private Good createAndSaveGood(GoodState state, long movementId,
                                   long cartId, long categoryId,
                                   ModelEdit.Hierarchy storageHierarchy,
                                   ModelEdit.Hierarchy baseHierarchy,
                                   ModelEdit.Hierarchy baseLastReadHierarchy,
                                   ModelEdit.Hierarchy editedHierarchy) {
        Good good = createGood(
            movementId, cartId, storageHierarchy, baseHierarchy, baseLastReadHierarchy, editedHierarchy, state);
        good.setCategoryId(categoryId);
        return goodRepository.save(good);
    }

    private void createAndSavePictures(Good good, int total, int uploaded) {
        List<EditedPhoto> photos = Stream.generate(() -> createEditedPhoto(good))
            .limit(total)
            .map(PhotoTestUtils::toNotUploaded)
            .collect(Collectors.toList());

        editedPhotoRepository.createProcessedPhotos(photos);

        List<EditedPhoto> uploadedPhotos = editedPhotoRepository.getNotUploadedToMboPhotos().stream()
            .limit(uploaded)
            .map(photo -> toUploadedToMbo(photo, skuPicture("qwerty").build()))
            .collect(Collectors.toList());

        editedPhotoRepository.saveProcessedPhotos(uploadedPhotos);
    }

    private EditedPhoto createEditedPhoto(Good good) {
        return editedPhoto()
            .setGoodId(good.getId())
            .setBarcode(good.getWhBarcode())
            .setUploadedPicture(toByteArray(skuPicture("qwerty").build()));
    }

    private Good createGood(long movementId,
                            long cartId,
                            ModelEdit.Hierarchy storageHierarchy,
                            ModelEdit.Hierarchy baseHierarchy,
                            ModelEdit.Hierarchy baseLastReadHierarchy,
                            ModelEdit.Hierarchy editedHierarchy, GoodState state) {
        Good good = new Good()
            .setWhBarcode("123456")
            .setCartId(cartId)
            .setIncomingMovementId(movementId)
            .setSupplierId(10L)
            .setSupplierSkuId("10-20")
            .setState(state);
        if (storageHierarchy != null) {
            ProtoUtils.setStorageHierarchy(good, storageHierarchy);
        }
        if (baseHierarchy != null) {
            ProtoUtils.setBaseHierarchy(good, baseHierarchy);
        }
        if (baseLastReadHierarchy != null) {
            ProtoUtils.setBaseLastReadHierarchy(good, baseLastReadHierarchy);
        }
        if (editedHierarchy != null) {
            ProtoUtils.setEditedHierarchy(good, editedHierarchy);
        }
        return good;
    }

    private Cart createAndSaveCart() {
        return cartRepository.save(new Cart()
            .setState(CartState.EDITOR));
    }

    private Movement createAndSaveMovement() {
        return movementRepository.save(new Movement()
            .setDirection(MovementDirection.INCOMING)
            .setState(MovementState.SORTED_TO_CARTS));
    }

    private ModelEdit.SaveRequest createSaveRequest(ModelEdit.Hierarchy hierarchy) {
        return ModelEdit.SaveRequest.newBuilder()
            .setHierarchy(hierarchy)
            .build();
    }

    private void mockModelStorageSuccess() {
        when(modelStorageServiceMock.saveModelsGroup(saveModelsRequestCaptor.capture()))
            .thenReturn(ModelCardApi.SaveModelsGroupResponse.newBuilder()
                .addResponse(ModelCardApi.SaveModelsGroupOperationResponse.newBuilder()
                    .setStatus(ModelStorage.OperationStatusType.OK)
                    .build())
                .build());
    }

    private void mockModelStorageFailure(ModelStorage.OperationStatus failureStatus) {
        when(modelStorageServiceMock.saveModelsGroup(saveModelsRequestCaptor.capture()))
            .thenReturn(ModelCardApi.SaveModelsGroupResponse.newBuilder()
                .addResponse(ModelCardApi.SaveModelsGroupOperationResponse.newBuilder()
                    .setStatus(ModelStorage.OperationStatusType.VALIDATION_ERROR)
                    .addRequestedModelsStatuses(failureStatus)
                    .build())
                .build());
    }

    private void mockModelStorageConcurrentModification(ModelEdit.Hierarchy newHierarchy,
                                                        ModelStorage.OperationStatus nextStatus) {
        when(modelStorageServiceMock.saveModelsGroup(saveModelsRequestCaptor.capture()))
            .thenAnswer(i -> {
                SaveModelsGroupRequest request = i.getArgument(0);
                ModelStorage.Model model = request.getModelsRequestList().get(0).getModelsList().get(0);
                if (model.getModifiedTs() == newHierarchy.getModel().getModifiedTs()) {
                    return ModelCardApi.SaveModelsGroupResponse.newBuilder()
                        .addResponse(ModelCardApi.SaveModelsGroupOperationResponse.newBuilder()
                            .setStatus(nextStatus.getStatus())
                            .addRequestedModelsStatuses(nextStatus)
                            .build())
                        .build();
                } else {
                    return ModelCardApi.SaveModelsGroupResponse.newBuilder()
                        .addResponse(ModelCardApi.SaveModelsGroupOperationResponse.newBuilder()
                            .setStatus(ModelStorage.OperationStatusType.MODEL_MODIFIED)
                            .addRequestedModelsStatuses(ModelStorage.OperationStatus.newBuilder()
                                .setType(ModelStorage.OperationType.CHANGE)
                                .setStatus(ModelStorage.OperationStatusType.MODEL_MODIFIED)
                                .setStatusMessage("Concurrent modification occurred")
                                .setModel(newHierarchy.getModel())
                                .setModelId(newHierarchy.getModel().getId())
                                .build())
                            .addRequestedModelsStatuses(ModelStorage.OperationStatus.newBuilder()
                                .setType(ModelStorage.OperationType.CHANGE)
                                .setStatus(ModelStorage.OperationStatusType.MODEL_MODIFIED)
                                .setStatusMessage("Concurrent modification occurred")
                                .setModel(newHierarchy.getModification())
                                .setModelId(newHierarchy.getModification().getId())
                                .build())
                            .addRequestedModelsStatuses(ModelStorage.OperationStatus.newBuilder()
                                .setType(ModelStorage.OperationType.CHANGE)
                                .setStatus(ModelStorage.OperationStatusType.MODEL_MODIFIED)
                                .setStatusMessage("Concurrent modification occurred")
                                .setModel(newHierarchy.getSku())
                                .setModelId(newHierarchy.getSku().getId())
                                .build())
                            .build())
                        .build();
                }
            });
    }

    private Category createAndSaveCategory(long categoryId, Boolean useYangPipeline) {
        MboParameters.Category categoryData = MboParameters.Category.newBuilder()
            .setHid(categoryId)
            .addParameter(enumParam(1L, "Param1", "Param1 name")
                .addOption(enumOption(11L, "Option1"))
                .addOption(enumOption(12L, "Option2"))
                .addOption(enumOption(13L, "Option3"))
                .addOption(enumOption(14L, "Option4")))
            .addParameter(enumParam(2L, "Param2", "Param2 name")
                .addOption(enumOption(21L, "Option1"))
                .addOption(enumOption(22L, "Option2"))
                .addOption(enumOption(23L, "Option3"))
                .addOption(enumOption(24L, "Option4")))
            .addParameter(enumParam(3L, "Param3", "Param3 name")
                .addOption(enumOption(31L, "Option1"))
                .addOption(enumOption(32L, "Option2"))
                .addOption(enumOption(33L, "Option3"))
                .addOption(enumOption(34L, "Option4")))
            .build();

        Category category = new Category()
            .setId(categoryId)
            .setData(toByteArray(categoryData))
            .setUseYangPipeline(useYangPipeline);
        return categoryRepository.create(category);
    }
}
