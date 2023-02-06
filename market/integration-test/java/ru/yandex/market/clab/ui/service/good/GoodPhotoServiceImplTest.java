package ru.yandex.market.clab.ui.service.good;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.clab.common.cache.CategoryInfoCache;
import ru.yandex.market.clab.common.service.ProcessingErrorType;
import ru.yandex.market.clab.common.service.ProcessingException;
import ru.yandex.market.clab.common.service.cart.CartRepository;
import ru.yandex.market.clab.common.service.category.CategoryRepository;
import ru.yandex.market.clab.common.service.good.GoodRepository;
import ru.yandex.market.clab.common.service.photo.EditedPhotoRepository;
import ru.yandex.market.clab.common.service.photo.RawPhotoRepository;
import ru.yandex.market.clab.common.service.user.ClabRole;
import ru.yandex.market.clab.common.service.user.User;
import ru.yandex.market.clab.common.service.user.UserService;
import ru.yandex.market.clab.common.test.ModelTestUtils;
import ru.yandex.market.clab.db.jooq.generated.enums.CartState;
import ru.yandex.market.clab.db.jooq.generated.enums.GoodState;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Cart;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Category;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Good;
import ru.yandex.market.clab.test.NasTestHelper;
import ru.yandex.market.clab.ui.BaseUiIntegrationTest;
import ru.yandex.market.clab.ui.service.model.GoodComment;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.ModelStorageService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author anmalysh
 * @since 1/24/2019
 */
@SuppressWarnings("checkstyle:magicnumber")
@Ignore("https://st.yandex-team.ru/MBO-23335#5e11f3ee2b10cd7237f8a26c")
public class GoodPhotoServiceImplTest extends BaseUiIntegrationTest {

    private static final long SEED = 100500666740L;
    private static final AtomicInteger USER_COUNTER = new AtomicInteger();

    private final Logger log = LogManager.getLogger();

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @MockBean(name = "modelStorageServiceUi")
    private ModelStorageService modelStorageServiceUi;

    @Autowired
    private GoodRepository goodRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private GoodPhotoService goodPhotoService;

    private EnhancedRandom random;

    @Autowired
    private NasTestHelper nasHelper;

    @Autowired
    private CategoryInfoCache categoryInfoCache;

    @Autowired
    private RawPhotoRepository rawPhotoRepository;

    @Autowired
    private EditedPhotoRepository editedPhotoRepository;

    @Autowired
    private UserService userService;

    private long verifierId;
    private long photoEditorId;
    private long photographerId;

    @Before
    public void before() {
        random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
            .stringLengthRange(20, 20)
            .seed(SEED)
            .build();

        verifierId = createUser("verifier", ClabRole.VERIFIER);
        photoEditorId = createUser("photoEditor", ClabRole.PHOTO_EDITOR);
        photographerId = createUser("photographer", ClabRole.PHOTOGRAPHER);
    }

    private long createUser(String login, ClabRole role) {
        User user = new User("integration-test-" + login + "-" + getClass().getSimpleName()
            + USER_COUNTER.incrementAndGet());
        user.addRole(role.name());
        User savedUser = userService.save(user);
        log.info("User info: {}", savedUser);
        return savedUser.getId();
    }

    @Test
    public void verifyAndAcceptRawPhotosNoCategory() {
        Cart cart = createAndSaveCart(CartState.PHOTO);
        Good good = createAndSaveGood(cart.getId(), GoodState.PHOTO);
        assertThatThrownBy(() -> goodPhotoService.verifyAndAcceptRawPhotos(cart.getId(), good.getWhBarcode(),
            photographerId))
            .isInstanceOf(ProcessingException.class)
            .matches(p -> ((ProcessingException) p).getType() == ProcessingErrorType.CATEGORY_CONFIG_MISSING);
    }

    @Test
    public void verifyAndAcceptRawPhotoWrongPhotos() {
        Cart cart = createAndSaveCart(CartState.PHOTO);
        Good good = createAndSaveGood(cart.getId(), GoodState.PHOTO);
        mockCategory();
        assertThatThrownBy(() -> goodPhotoService.verifyAndAcceptRawPhotos(cart.getId(), good.getWhBarcode(),
            photographerId))
            .isInstanceOf(ProcessingException.class)
            .matches(p -> ((ProcessingException) p).getType() == ProcessingErrorType.PHOTOS_DIRECTORY_MISSING);

        nasHelper.createRawPhotos(good, 1, false);
        assertThatThrownBy(() -> goodPhotoService.verifyAndAcceptRawPhotos(cart.getId(), good.getWhBarcode(),
            photographerId))
            .isInstanceOf(ProcessingException.class)
            .matches(p -> ((ProcessingException) p).getType() == ProcessingErrorType.PHOTOS_MISSING);

        nasHelper.createRawPhotos(good, 2, false);
        assertThatThrownBy(() -> goodPhotoService.verifyAndAcceptRawPhotos(cart.getId(), good.getWhBarcode(),
            photographerId))
            .isInstanceOf(ProcessingException.class)
            .matches(p -> ((ProcessingException) p).getType() == ProcessingErrorType.PHOTOS_DIRECTORY_MISSING);

        nasHelper.createEditedPhotos(good, 1, false);
        assertThatThrownBy(() -> goodPhotoService.verifyAndAcceptRawPhotos(cart.getId(), good.getWhBarcode(),
            photographerId))
            .isInstanceOf(ProcessingException.class)
            .matches(p -> ((ProcessingException) p).getType() == ProcessingErrorType.PHOTOS_MISSING);

        nasHelper.createEditedPhotos(good, 3, false);
        assertThatThrownBy(() -> goodPhotoService.verifyAndAcceptRawPhotos(cart.getId(), good.getWhBarcode(),
            photographerId))
            .isInstanceOf(ProcessingException.class)
            .matches(p -> ((ProcessingException) p).getType() == ProcessingErrorType.PHOTOS_INCONSISTENCY);
    }

    @Test
    public void verifyAndAcceptRawPhoto() {
        Cart cart = createAndSaveCart(CartState.PHOTO);
        Good good = createAndSaveGood(cart.getId(), GoodState.PHOTO);
        nasHelper.createRawPhotos(good, 2, false);
        nasHelper.createEditedPhotos(good, 2, false);
        Good good2 = createAndSaveGood(cart.getId(), GoodState.PHOTO, g -> g.setPhotoEditorId(photoEditorId));
        nasHelper.createRawPhotos(good2, 2, false);
        nasHelper.createEditedPhotos(good2, 2, false);
        mockCategory();

        goodPhotoService.verifyAndAcceptRawPhotos(cart.getId(), good.getWhBarcode(), photographerId);

        Good updatedGood = goodRepository.getById(good.getId());
        assertThat(updatedGood).extracting(Good::getState).isEqualTo(GoodState.PHOTOGRAPHED);

        Cart currentCart = cartRepository.getById(cart.getId());
        assertThat(currentCart).isEqualTo(cart);

        assertThat(nasHelper.getProcessedRawPhotos(good))
            .hasSize(2);
        assertThat(nasHelper.getProcessedEditedPhotos(good))
            .hasSize(2);

        assertThat(rawPhotoRepository.getNotUploadedPhotos())
            .hasSize(2);
        assertThat(editedPhotoRepository.getNotUploadedToMboPhotos())
            .isEmpty();

        goodPhotoService.verifyAndAcceptRawPhotos(cart.getId(), good2.getWhBarcode(), photographerId);

        Good updatedGood2 = goodRepository.getById(good2.getId());
        assertThat(updatedGood2).extracting(Good::getState).isEqualTo(GoodState.PHOTOGRAPHED);

        currentCart = cartRepository.getById(cart.getId());
        assertThat(currentCart).extracting(Cart::getState).isEqualTo(CartState.EDITOR);

        assertThat(rawPhotoRepository.getNotUploadedPhotos())
            .hasSize(4);
        assertThat(editedPhotoRepository.getNotUploadedToMboPhotos())
            .isEmpty();

        assertThat(nasHelper.getProcessedRawPhotos(good2))
            .hasSize(2);
        assertThat(nasHelper.getProcessedEditedPhotos(good2))
            .hasSize(2);
    }

    @Test
    public void verifyAndAcceptEditedPhotosNoCategory() {
        Good good = createAndSaveGood(GoodState.PHOTO);
        assertThatThrownBy(() -> goodPhotoService.verifyAndAcceptEditedPhotos(good.getId()))
            .isInstanceOf(ProcessingException.class)
            .matches(p -> ((ProcessingException) p).getType() == ProcessingErrorType.CATEGORY_CONFIG_MISSING);
    }

    @Test
    public void verifyAndAcceptEditedPhotoWrongPhotos() {
        Good good = createAndSaveGood(GoodState.PHOTO);
        mockCategory();
        assertThatThrownBy(() -> goodPhotoService.verifyAndAcceptEditedPhotos(good.getId()))
            .isInstanceOf(ProcessingException.class)
            .matches(p -> ((ProcessingException) p).getType() == ProcessingErrorType.PHOTOS_DIRECTORY_MISSING);

        nasHelper.createEditedPhotos(good, 1, false);
        assertThatThrownBy(() -> goodPhotoService.verifyAndAcceptEditedPhotos(good.getId()))
            .isInstanceOf(ProcessingException.class)
            .matches(p -> ((ProcessingException) p).getType() == ProcessingErrorType.PHOTOS_MISSING);
    }

    @Test
    public void verifyAndAcceptEditedPhoto() {
        Good good = createAndSaveGood(GoodState.PHOTOGRAPHED);
        nasHelper.createEditedPhotos(good, 2, true);
        Good good2 = createAndSaveGood(1L, GoodState.PHOTOGRAPHED, g -> g.setEditorId(2L));
        nasHelper.createEditedPhotos(good2, 2, true);
        Good good3 = createAndSaveGood(GoodState.PHOTOGRAPHED);
        nasHelper.createEditedPhotos(good3, 2, true);
        goodPhotoService.startPhotoEditing(
            Arrays.asList(good.getId(), good2.getId(), good3.getId()), photoEditorId, true);
        mockCategory();
        mockModelStorageValidateImage(ModelStorage.OperationStatus.newBuilder()
            .setType(ModelStorage.OperationType.UPLOAD_IMAGE)
            .setStatus(ModelStorage.OperationStatusType.OK)
            .build());

        goodPhotoService.verifyAndAcceptEditedPhotos(good.getId());
        goodPhotoService.verifyAndAcceptEditedPhotos(good2.getId());

        Good updatedGood = goodRepository.getById(good.getId());
        assertThat(updatedGood).extracting(Good::getState).isEqualTo(GoodState.PHOTO_EDITED);

        Good updatedGood2 = goodRepository.getById(good2.getId());
        assertThat(updatedGood2).extracting(Good::getState).isEqualTo(GoodState.EDITING);

        assertThat(editedPhotoRepository.getNotUploadedToMboPhotos())
            .hasSize(4);

        assertThat(nasHelper.getProcessedEditedPhotos(good))
            .hasSize(2);
        assertThat(nasHelper.checkInProcessDirectoryExists(good))
            .isFalse();
        assertThat(nasHelper.getProcessedEditedPhotos(good2))
            .hasSize(2);
        assertThat(nasHelper.checkInProcessDirectoryExists(good2))
            .isFalse();

        goodPhotoService.verifyAndAcceptEditedPhotos(good3.getId());

        Good updatedGood3 = goodRepository.getById(good3.getId());
        assertThat(updatedGood3).extracting(Good::getState).isEqualTo(GoodState.PHOTO_EDITED);

        assertThat(editedPhotoRepository.getNotUploadedToMboPhotos())
            .hasSize(6);

        assertThat(nasHelper.getProcessedEditedPhotos(good3))
            .hasSize(2);
        assertThat(nasHelper.checkInProcessDirectoryExists(good3))
            .isFalse();
    }

    @Test
    public void verifyAndAcceptEditedPhotoForGoodFromYangPipeline() {
        Good good = createAndSaveGood(GoodState.PHOTOGRAPHED);
        nasHelper.createEditedPhotos(good, 2, true);
        goodPhotoService.startPhotoEditing(Collections.singletonList(good.getId()), photoEditorId, true);
        Category category = mockCategory();
        category.setUseYangPipeline(true);
        categoryRepository.update(category);
        mockModelStorageValidateImage(ModelStorage.OperationStatus.newBuilder()
            .setType(ModelStorage.OperationType.UPLOAD_IMAGE)
            .setStatus(ModelStorage.OperationStatusType.OK)
            .build());

        goodPhotoService.verifyAndAcceptEditedPhotos(good.getId());

        Good updatedGood = goodRepository.getById(good.getId());
        assertThat(updatedGood).extracting(Good::getState).isEqualTo(GoodState.EDITED);

        assertThat(editedPhotoRepository.getNotUploadedToMboPhotos()).hasSize(2);
        assertThat(nasHelper.getProcessedEditedPhotos(good)).hasSize(2);
        assertThat(nasHelper.checkInProcessDirectoryExists(good)).isFalse();
    }

    @Test
    public void verifyAndAcceptEditedPhotoValidationFailed() {
        Good good = createAndSaveGood(1L, GoodState.PHOTOGRAPHED);
        nasHelper.createEditedPhotos(good, 2, true);
        mockCategory();
        mockModelStorageValidateImage(ModelStorage.OperationStatus.newBuilder()
            .setType(ModelStorage.OperationType.UPLOAD_IMAGE)
            .setStatus(ModelStorage.OperationStatusType.INTERNAL_ERROR)
            .build());

        goodPhotoService.startPhotoEditing(Collections.singletonList(good.getId()), photoEditorId, true);
        Good afterStartedEditing = goodRepository.getById(good.getId());
        assertThat(afterStartedEditing).isEqualToIgnoringGivenFields(good, "modifiedDate", "state", "photoEditorId");
        assertThat(afterStartedEditing.getPhotoEditorId()).isEqualTo(photoEditorId);

        assertThatThrownBy(() -> goodPhotoService.verifyAndAcceptEditedPhotos(good.getId()))
            .isInstanceOf(ProcessingException.class)
            .extracting(p -> ((ProcessingException) p).getType())
            .isEqualTo(ProcessingErrorType.PHOTO_VALIDATION_FAILED);

        Good currentGood = goodRepository.getById(good.getId());
        assertThat(currentGood).isEqualTo(afterStartedEditing);

        assertThat(editedPhotoRepository.getNotUploadedToMboPhotos())
            .isEmpty();
        assertThat(nasHelper.checkProcessedEditedDirectoryExists(good))
            .isFalse();
    }

    @Test
    public void startPhotoEditingSameBarcode() {
        Good good = createAndSaveGood(1L, GoodState.PHOTOGRAPHED, g -> {
            g.setPhotoEditorId(null);
        });
        Good sameWhGood = createAndSaveGood(2L, GoodState.PHOTO_EDITING, g -> {
            g.setWhBarcode(good.getWhBarcode());
            g.setPhotoEditorId(photoEditorId);
        });
        nasHelper.createEditedPhotos(good, 2, true);

        assertThatThrownBy(() ->
            goodPhotoService.startPhotoEditing(Collections.singletonList(good.getId()), photoEditorId, true))
            .isInstanceOf(ProcessingException.class)
            .extracting(p -> ((ProcessingException) p).getType())
            .isEqualTo(ProcessingErrorType.GOOD_DUPLICATE);
    }

    @Test
    public void returnToPhotographerWrongGoodState() {
        Good good = createAndSaveGood(GoodState.OUT);
        assertThatThrownBy(() -> goodPhotoService.returnToPhotographer(createComments(good), verifierId))
            .isInstanceOf(ProcessingException.class)
            .matches(p -> ((ProcessingException) p).getType() == ProcessingErrorType.GOOD_STATE_INVALID);

        Good good2 = createAndSaveGood(GoodState.PHOTO);
        assertThatThrownBy(() -> goodPhotoService.returnToPhotographer(createComments(good2), verifierId))
            .isInstanceOf(ProcessingException.class)
            .matches(p -> ((ProcessingException) p).getType() == ProcessingErrorType.GOOD_STATE_INVALID);

        Good good3 = createAndSaveGood(GoodState.PHOTO_EDITING);
        assertThatThrownBy(() -> goodPhotoService.returnToPhotographer(createComments(good3), verifierId))
            .isInstanceOf(ProcessingException.class)
            .matches(p -> ((ProcessingException) p).getType() == ProcessingErrorType.GOOD_STATE_INVALID);
    }

    @Test
    public void returnToPhotographerNoPhotos() {
        Good good = createAndSaveGood(GoodState.EDITED);
        assertThatThrownBy(() -> goodPhotoService.returnToPhotographer(createComments(good), verifierId))
            .isInstanceOf(ProcessingException.class)
            .matches(p -> ((ProcessingException) p).getType() == ProcessingErrorType.PHOTOS_MISSING);

        nasHelper.createEditedPhotos(good, 2, true);
        assertThatThrownBy(() -> goodPhotoService.returnToPhotographer(createComments(good), verifierId))
            .isInstanceOf(ProcessingException.class)
            .matches(p -> ((ProcessingException) p).getType() == ProcessingErrorType.PHOTOS_MISSING);
    }

    @Test
    public void returnToPhotographer() {
        Cart cart = createAndSaveCart(CartState.PHOTO);
        Good good = createAndSaveGood(cart.getId(), GoodState.PHOTO_EDITED);
        nasHelper.createRawPhotos(good, 2, true);
        nasHelper.createEditedPhotos(good, 2, true);
        nasHelper.createCustomFile(good, Collections.singleton(".DS_Store"), true);
        nasHelper.createCustomFile(good, Arrays.asList(".git", "index"), true);

        Cart cart2 = createAndSaveCart(CartState.EDITOR);
        Good good2 = createAndSaveGood(cart2.getId(), GoodState.EDITED);
        nasHelper.createRawPhotos(good2, 2, true);
        nasHelper.createEditedPhotos(good2, 2, true);
        nasHelper.createCustomFile(good2, Collections.singleton(".DS_Store"), true);
        nasHelper.createCustomFile(good2, Arrays.asList(".git", "index"), true);

        goodPhotoService.returnToPhotographer(createComments(good), verifierId);
        goodPhotoService.returnToPhotographer(createComments(good2), verifierId);

        Good updatedGood = goodRepository.getById(good.getId());
        assertThat(updatedGood).extracting(Good::getState).isEqualTo(GoodState.PHOTO);
        Good updatedGood2 = goodRepository.getById(good2.getId());
        assertThat(updatedGood2).extracting(Good::getState).isEqualTo(GoodState.PHOTO);

        Cart updatedCart1 = cartRepository.getById(cart.getId());
        assertThat(updatedCart1).extracting(Cart::getState).isEqualTo(CartState.PHOTO);
        Cart updatedCart2 = cartRepository.getById(cart2.getId());
        assertThat(updatedCart2).extracting(Cart::getState).isEqualTo(CartState.PHOTO);

        assertThat(nasHelper.checkProcessedRawDirectoryExists(good)).isFalse();
        assertThat(nasHelper.checkProcessedEditedDirectoryExists(good)).isFalse();
        assertThat(nasHelper.checkProcessedRawDirectoryExists(good2)).isFalse();
        assertThat(nasHelper.checkProcessedEditedDirectoryExists(good2)).isFalse();
        assertThat(nasHelper.getInProcessRawPhotos(good)).hasSize(2);
        assertThat(nasHelper.getInProcessEditedPhotos(good2)).hasSize(2);
        assertThat(nasHelper.getInProcessRawPhotos(good2)).hasSize(2);
        assertThat(nasHelper.getInProcessEditedPhotos(good2)).hasSize(2);

        assertThat(nasHelper.fileExists(good, Collections.singleton(".DS_Store"), false)).isTrue();
        assertThat(nasHelper.fileExists(good, Arrays.asList(".git", "index"), false)).isTrue();
        assertThat(nasHelper.fileExists(good2, Collections.singleton(".DS_Store"), false)).isTrue();
        assertThat(nasHelper.fileExists(good2, Arrays.asList(".git", "index"), false)).isTrue();
    }

    @Test
    public void returnToPhotoEditorWrongGoodState() {
        Good good = createAndSaveGood(GoodState.OUT);
        assertThatThrownBy(() -> goodPhotoService.returnToPhotoEditor(createComments(good), verifierId))
            .isInstanceOf(ProcessingException.class)
            .matches(p -> ((ProcessingException) p).getType() == ProcessingErrorType.GOOD_STATE_INVALID);

        Good good2 = createAndSaveGood(GoodState.PHOTO_EDITING);
        assertThatThrownBy(() -> goodPhotoService.returnToPhotoEditor(createComments(good2), verifierId))
            .isInstanceOf(ProcessingException.class)
            .matches(p -> ((ProcessingException) p).getType() == ProcessingErrorType.GOOD_STATE_INVALID);
    }

    private GoodComment createComments(Good good) {
        return new GoodComment(good.getId(), "test-comment");
    }

    @Test
    public void returnToPhotoEditorNoPhotos() {
        Good good = createAndSaveGood(GoodState.EDITED);
        assertThatThrownBy(() -> goodPhotoService.returnToPhotoEditor(createComments(good), verifierId))
            .isInstanceOf(ProcessingException.class)
            .matches(p -> ((ProcessingException) p).getType() == ProcessingErrorType.PHOTOS_MISSING);
    }

    @Test
    public void returnToPhotoEditor() {
        Cart cart = createAndSaveCart(CartState.OUT);
        Good good = createAndSaveGood(cart.getId(), GoodState.EDITED, g -> g.setPhotoEditorId(photoEditorId));
        nasHelper.createEditedPhotos(good, 2, true);
        Cart cart2 = createAndSaveCart(CartState.PHOTO);
        Good good2 = createAndSaveGood(cart2.getId(), GoodState.EDITING, g -> g.setPhotoEditorId(photoEditorId));
        nasHelper.createEditedPhotos(good2, 2, true);

        goodPhotoService.returnToPhotoEditor(createComments(good), verifierId);
        goodPhotoService.returnToPhotoEditor(createComments(good2), verifierId);

        Good updatedGood = goodRepository.getById(good.getId());
        assertThat(updatedGood).extracting(Good::getState).isEqualTo(GoodState.PHOTOGRAPHED);
        Good updatedGood2 = goodRepository.getById(good2.getId());
        assertThat(updatedGood2).extracting(Good::getState).isEqualTo(GoodState.PHOTOGRAPHED);

        Cart updatedCart1 = cartRepository.getById(cart.getId());
        assertThat(updatedCart1).extracting(Cart::getState).isEqualTo(CartState.EDITOR);
        Cart updatedCart2 = cartRepository.getById(cart2.getId());
        assertThat(updatedCart2).extracting(Cart::getState).isEqualTo(CartState.PHOTO);

        assertThat(nasHelper.checkProcessedEditedDirectoryExists(good)).isTrue();
        assertThat(nasHelper.checkProcessedEditedDirectoryExists(good2)).isTrue();
        assertThat(nasHelper.getProcessedEditedPhotos(good)).hasSize(2);
        assertThat(nasHelper.getProcessedEditedPhotos(good2)).hasSize(2);
    }

    private Category mockCategory() {
        Category category = new Category()
            .setId(ModelTestUtils.CATEGORY_ID)
            .setMinRawPhotos(2)
            .setMinProcessedPhotos(2)
            .setData(MboParameters.Category.newBuilder()
                .setHid(ModelTestUtils.CATEGORY_ID)
                .build()
                .toByteArray());
        return categoryRepository.create(category);
    }

    private Good createAndSaveGood(GoodState goodState) {
        return createAndSaveGood(1L, goodState);
    }

    private Good createAndSaveGood(long cartId, GoodState goodState) {
        return createAndSaveGood(cartId, goodState, g -> {
        });
    }

    private Good createAndSaveGood(long cartId, GoodState goodState, Consumer<Good> modifier) {
        Good good = new Good()
            .setWhBarcode(random.nextObject(String.class))
            .setCartId(cartId)
            .setSupplierId(10L)
            .setSupplierSkuId(random.nextObject(String.class))
            .setCategoryId(ModelTestUtils.CATEGORY_ID)
            .setState(goodState);
        modifier.accept(good);
        return goodRepository.save(good);
    }

    private Cart createAndSaveCart(CartState cartState) {
        Cart cart = new Cart()
            .setExternalId(random.nextObject(String.class))
            .setState(cartState);
        return cartRepository.save(cart);
    }

    private void mockModelStorageValidateImage(ModelStorage.OperationStatus status) {
        when(modelStorageServiceUi.validateImages(any(ModelStorage.ValidateImagesRequest.class)))
            .thenAnswer(i -> {
                ModelStorage.ValidateImagesRequest request = i.getArgument(0);
                ModelStorage.ValidateImagesResponse.Builder result = ModelStorage.ValidateImagesResponse.newBuilder();
                request.getImageDataList()
                    .forEach(req -> result.addValidationStatus(ModelStorage.ValidateImageStatus.newBuilder()
                        .setId(req.getId())
                        .setStatus(status)));
                return result.build();
            });
    }

    private String getLogin(long userId) {
        return userService.getUserInfo(userId).get().getLogin();
    }

    @After
    public void shutdown() {
        categoryInfoCache.invalidateCategory(ModelTestUtils.CATEGORY_ID);
        nasHelper.cleanTestDirs(Collections.singletonList(getLogin(photoEditorId)));
    }

}
