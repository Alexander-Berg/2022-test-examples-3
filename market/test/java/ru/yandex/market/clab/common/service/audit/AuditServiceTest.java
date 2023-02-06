package ru.yandex.market.clab.common.service.audit;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.clab.common.service.audit.wrapper.CartWrapper;
import ru.yandex.market.clab.common.service.audit.wrapper.CategoryWrapper;
import ru.yandex.market.clab.common.service.audit.wrapper.GoodTypeWrapper;
import ru.yandex.market.clab.common.service.audit.wrapper.GoodWrapper;
import ru.yandex.market.clab.common.service.audit.wrapper.MovementWrapper;
import ru.yandex.market.clab.common.service.audit.wrapper.RequestedGoodWrapper;
import ru.yandex.market.clab.common.service.audit.wrapper.RequestedMovementWrapper;
import ru.yandex.market.clab.db.jooq.generated.enums.ActionType;
import ru.yandex.market.clab.db.jooq.generated.enums.CartState;
import ru.yandex.market.clab.db.jooq.generated.enums.EntityType;
import ru.yandex.market.clab.db.jooq.generated.enums.GoodState;
import ru.yandex.market.clab.db.jooq.generated.enums.MovementDirection;
import ru.yandex.market.clab.db.jooq.generated.enums.MovementState;
import ru.yandex.market.clab.db.jooq.generated.enums.RequestedGoodMbocState;
import ru.yandex.market.clab.db.jooq.generated.enums.RequestedGoodState;
import ru.yandex.market.clab.db.jooq.generated.enums.RequestedMovementState;
import ru.yandex.market.clab.db.jooq.generated.enums.SupplierType;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.AuditAction;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Cart;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Category;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Good;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.GoodType;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Movement;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.RequestedGood;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.RequestedMovement;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author anmalysh
 * @since 11/1/2018
 */
@SuppressWarnings("checkstyle:magicnumber")
public class AuditServiceTest {

    private AuditService auditService = new AuditService(null, "robot-mrk-clab-itest");

    private EnhancedRandom random;

    private static final long SEED = 9005089642L;

    @Before
    public void before() {
        random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder().seed(SEED).build();
    }

    @Test
    public void testMovementAddActions() {
        Movement movement = random.nextObject(Movement.class);

        List<AuditAction> actions = auditService.createAuditActions(null, new MovementWrapper(movement));

        assertDateExistsAndClean(actions);
        assertThat(actions).containsExactlyInAnyOrder(
            createMovementAction(ActionType.CREATE, movement),
            createMovementAction(ActionType.UPDATE, movement)
                .setPropertyName("Состояние")
                .setNewValue(String.valueOf(movement.getState())),
            createMovementAction(ActionType.UPDATE, movement)
                .setPropertyName("Тип поставщика")
                .setNewValue(String.valueOf(movement.getSupplierType())),
            createMovementAction(ActionType.UPDATE, movement)
                .setPropertyName("Дата создания")
                .setNewValue(String.valueOf(movement.getCreatedDate())),
            createMovementAction(ActionType.UPDATE, movement)
                .setPropertyName("Направление")
                .setNewValue(String.valueOf(movement.getDirection()))
        );
    }

    @Test
    public void testMovementUpdateActions() {
        Movement movement = random.nextObject(Movement.class);
        Movement updated = new Movement(movement);
        movement.setState(MovementState.NEW);
        updated.setState(MovementState.PROCESSED);
        movement.setDirection(MovementDirection.INCOMING);
        updated.setDirection(MovementDirection.OUTGOING);
        movement.setSupplierType(SupplierType.FIRST_PARTY);
        updated.setSupplierType(SupplierType.THIRD_PARTY);
        movement.setSupplierType(SupplierType.FIRST_PARTY);
        updated.setCreatedDate(movement.getCreatedDate().plusDays(1));

        List<AuditAction> actions = auditService.createAuditActions(
            new MovementWrapper(movement), new MovementWrapper(updated));

        assertDateExistsAndClean(actions);
        assertThat(actions).containsExactlyInAnyOrder(
            createMovementAction(ActionType.UPDATE, movement)
                .setPropertyName("Состояние")
                .setOldValue(String.valueOf(movement.getState()))
                .setNewValue(String.valueOf(updated.getState())),
            createMovementAction(ActionType.UPDATE, movement)
                .setPropertyName("Тип поставщика")
                .setOldValue(String.valueOf(movement.getSupplierType()))
                .setNewValue(String.valueOf(updated.getSupplierType())),
            createMovementAction(ActionType.UPDATE, movement)
                .setPropertyName("Дата создания")
                .setOldValue(String.valueOf(movement.getCreatedDate()))
                .setNewValue(String.valueOf(updated.getCreatedDate())),
            createMovementAction(ActionType.UPDATE, movement)
                .setPropertyName("Направление")
                .setOldValue(String.valueOf(movement.getDirection()))
                .setNewValue(String.valueOf(updated.getDirection()))
        );
    }

    @Test
    public void testMovementDeleteActions() {
        Movement movement = random.nextObject(Movement.class);

        List<AuditAction> actions = auditService.createAuditActions(new MovementWrapper(movement), null);

        assertDateExistsAndClean(actions);
        assertThat(actions).containsExactlyInAnyOrder(
            createMovementAction(ActionType.DELETE, movement)
        );
    }

    @Test
    public void testCartAddActions() {
        Cart cart = random.nextObject(Cart.class);

        List<AuditAction> actions = auditService.createAuditActions(null, new CartWrapper(cart));

        assertDateExistsAndClean(actions);
        assertThat(actions).containsExactlyInAnyOrder(
            createCartAction(ActionType.CREATE, cart),
            createCartAction(ActionType.UPDATE, cart)
                .setPropertyName("Состояние")
                .setNewValue(String.valueOf(cart.getState())),
            createCartAction(ActionType.UPDATE, cart)
                .setPropertyName("ID типа товара")
                .setNewValue(String.valueOf(cart.getGoodTypeId())),
            createCartAction(ActionType.UPDATE, cart)
                .setPropertyName("Дата создания")
                .setNewValue(String.valueOf(cart.getCreatedDate()))
        );
    }

    @Test
    public void testCartUpdateActions() {
        Cart cart = random.nextObject(Cart.class);
        Cart updated = new Cart(cart);
        cart.setState(CartState.NEW);
        updated.setState(CartState.ASSEMBLY);
        cart.setGoodTypeId(1L);
        updated.setGoodTypeId(2L);
        updated.setCreatedDate(cart.getCreatedDate().plusDays(1));

        List<AuditAction> actions = auditService.createAuditActions(
            new CartWrapper(cart), new CartWrapper(updated));

        assertDateExistsAndClean(actions);
        assertThat(actions).containsExactlyInAnyOrder(
            createCartAction(ActionType.UPDATE, cart)
                .setPropertyName("Состояние")
                .setOldValue(String.valueOf(cart.getState()))
                .setNewValue(String.valueOf(updated.getState())),
            createCartAction(ActionType.UPDATE, cart)
                .setPropertyName("ID типа товара")
                .setOldValue(String.valueOf(cart.getGoodTypeId()))
                .setNewValue(String.valueOf(updated.getGoodTypeId())),
            createCartAction(ActionType.UPDATE, cart)
                .setPropertyName("Дата создания")
                .setOldValue(String.valueOf(cart.getCreatedDate()))
                .setNewValue(String.valueOf(updated.getCreatedDate()))
        );
    }

    @Test
    public void testCartDeleteActions() {
        Cart cart = random.nextObject(Cart.class);

        List<AuditAction> actions = auditService.createAuditActions(new CartWrapper(cart), null);

        assertDateExistsAndClean(actions);
        assertThat(actions).containsExactlyInAnyOrder(
            createCartAction(ActionType.DELETE, cart)
        );
    }

    @Test
    public void testGoodAddActions() {
        Good good = random.nextObject(Good.class);

        List<AuditAction> actions = auditService.createAuditActions(null, new GoodWrapper(good));

        assertDateExistsAndClean(actions);
        assertThat(actions).containsExactlyInAnyOrder(
            createGoodAction(ActionType.CREATE, good),
            createGoodAction(ActionType.UPDATE, good)
                .setPropertyName("Состояние")
                .setNewValue(String.valueOf(good.getState())),
            createGoodAction(ActionType.UPDATE, good)
                .setPropertyName("ID типа товара")
                .setNewValue(String.valueOf(good.getGoodTypeId())),
            createGoodAction(ActionType.UPDATE, good)
                .setPropertyName("ID поставщика")
                .setNewValue(String.valueOf(good.getSupplierId())),
            createGoodAction(ActionType.UPDATE, good)
                .setPropertyName("ID SKU поставщика")
                .setNewValue(String.valueOf(good.getSupplierSkuId())),
            createGoodAction(ActionType.UPDATE, good)
                .setPropertyName("ID телеги")
                .setNewValue(String.valueOf(good.getCartId())),
            createGoodAction(ActionType.UPDATE, good)
                .setPropertyName("ID SKU маркета")
                .setNewValue(String.valueOf(good.getMskuId())),
            createGoodAction(ActionType.UPDATE, good)
                .setPropertyName("ID категории")
                .setNewValue(String.valueOf(good.getCategoryId())),
            createGoodAction(ActionType.UPDATE, good)
                .setPropertyName("Штрихкод")
                .setNewValue(String.valueOf(good.getWhBarcode())),
            createGoodAction(ActionType.UPDATE, good)
                .setPropertyName("ID исходящего перемещения")
                .setNewValue(String.valueOf(good.getOutgoingMovementId())),
            createGoodAction(ActionType.UPDATE, good)
                .setPropertyName("ID входящего перемещения")
                .setNewValue(String.valueOf(good.getIncomingMovementId())),
            createGoodAction(ActionType.UPDATE, good)
                .setPropertyName("ID редактора")
                .setNewValue(String.valueOf(good.getEditorId())),
            createGoodAction(ActionType.UPDATE, good)
                .setPropertyName("Имя SKU маркета")
                .setNewValue(String.valueOf(good.getMskuTitle())),
            createGoodAction(ActionType.UPDATE, good)
                .setPropertyName("ID ретушера")
                .setNewValue(String.valueOf(good.getPhotoEditorId())),
            createGoodAction(ActionType.UPDATE, good)
                .setPropertyName("ID проверяющего")
                .setNewValue(String.valueOf(good.getVerifierId())));
    }

    @Test
    public void testGoodUpdateActions() {
        Good good = random.nextObject(Good.class);
        Good updated = new Good(good);
        good.setState(GoodState.NEW);
        updated.setState(GoodState.EDITED);
        good.setGoodTypeId(1L);
        updated.setGoodTypeId(2L);
        good.setSupplierId(1L);
        updated.setSupplierId(2L);
        good.setSupplierSkuId("asd");
        updated.setSupplierSkuId("qwe");
        good.setCartId(1L);
        updated.setCartId(2L);
        good.setMskuId(1L);
        updated.setMskuId(2L);
        good.setCategoryId(1L);
        updated.setCategoryId(2L);
        good.setOutgoingMovementId(1L);
        updated.setOutgoingMovementId(2L);
        good.setIncomingMovementId(1L);
        updated.setIncomingMovementId(2L);
        good.setWhBarcode("erty");
        updated.setWhBarcode("wert");
        good.setEditorId(1L);
        updated.setEditorId(2L);
        good.setMskuTitle("qweqw");
        updated.setMskuTitle("qweqweq");
        good.setPhotoEditorId(1L);
        updated.setPhotoEditorId(2L);

        List<AuditAction> actions = auditService.createAuditActions(
            new GoodWrapper(good), new GoodWrapper(updated));

        assertDateExistsAndClean(actions);
        assertThat(actions).containsExactlyInAnyOrder(
            createGoodAction(ActionType.UPDATE, updated)
                .setPropertyName("Состояние")
                .setOldValue(String.valueOf(good.getState()))
                .setNewValue(String.valueOf(updated.getState())),
            createGoodAction(ActionType.UPDATE, updated)
                .setPropertyName("ID типа товара")
                .setOldValue(String.valueOf(good.getGoodTypeId()))
                .setNewValue(String.valueOf(updated.getGoodTypeId())),
            createGoodAction(ActionType.UPDATE, updated)
                .setPropertyName("ID поставщика")
                .setOldValue(String.valueOf(good.getSupplierId()))
                .setNewValue(String.valueOf(updated.getSupplierId())),
            createGoodAction(ActionType.UPDATE, updated)
                .setPropertyName("ID SKU поставщика")
                .setOldValue(String.valueOf(good.getSupplierSkuId()))
                .setNewValue(String.valueOf(updated.getSupplierSkuId())),
            createGoodAction(ActionType.UPDATE, updated)
                .setPropertyName("ID телеги")
                .setOldValue(String.valueOf(good.getCartId()))
                .setNewValue(String.valueOf(updated.getCartId())),
            createGoodAction(ActionType.UPDATE, updated)
                .setPropertyName("ID SKU маркета")
                .setOldValue(String.valueOf(good.getMskuId()))
                .setNewValue(String.valueOf(updated.getMskuId())),
            createGoodAction(ActionType.UPDATE, updated)
                .setPropertyName("ID категории")
                .setOldValue(String.valueOf(good.getCategoryId()))
                .setNewValue(String.valueOf(updated.getCategoryId())),
            createGoodAction(ActionType.UPDATE, updated)
                .setPropertyName("Штрихкод")
                .setOldValue(String.valueOf(good.getWhBarcode()))
                .setNewValue(String.valueOf(updated.getWhBarcode())),
            createGoodAction(ActionType.UPDATE, updated)
                .setPropertyName("ID исходящего перемещения")
                .setOldValue(String.valueOf(good.getOutgoingMovementId()))
                .setNewValue(String.valueOf(updated.getOutgoingMovementId())),
            createGoodAction(ActionType.UPDATE, updated)
                .setPropertyName("ID входящего перемещения")
                .setOldValue(String.valueOf(good.getIncomingMovementId()))
                .setNewValue(String.valueOf(updated.getIncomingMovementId())),
            createGoodAction(ActionType.UPDATE, updated)
                .setPropertyName("ID редактора")
                .setOldValue(String.valueOf(good.getEditorId()))
                .setNewValue(String.valueOf(updated.getEditorId())),
            createGoodAction(ActionType.UPDATE, updated)
                .setPropertyName("Имя SKU маркета")
                .setOldValue(String.valueOf(good.getMskuTitle()))
                .setNewValue(String.valueOf(updated.getMskuTitle())),
            createGoodAction(ActionType.UPDATE, updated)
                .setPropertyName("ID ретушера")
                .setOldValue(String.valueOf(good.getPhotoEditorId()))
                .setNewValue(String.valueOf(updated.getPhotoEditorId()))
        );
    }

    @Test
    public void testGoodDeleteActions() {
        Good good = random.nextObject(Good.class);

        List<AuditAction> actions = auditService.createAuditActions(new GoodWrapper(good), null);

        assertDateExistsAndClean(actions);
        assertThat(actions).containsExactlyInAnyOrder(
            createGoodAction(ActionType.DELETE, good)
        );
    }

    @Test
    public void testRequestedGoodAddActions() {
        RequestedGood good = random.nextObject(RequestedGood.class);

        List<AuditAction> actions = auditService.createAuditActions(null, new RequestedGoodWrapper(good));

        assertDateExistsAndClean(actions);
        assertThat(actions).containsExactlyInAnyOrder(
            createRequestedGoodAction(ActionType.CREATE, good),
            createRequestedGoodAction(ActionType.UPDATE, good)
                .setPropertyName("Состояние")
                .setNewValue(String.valueOf(good.getState())),
            createRequestedGoodAction(ActionType.UPDATE, good)
                .setPropertyName("Состояние в MBOC")
                .setNewValue(String.valueOf(good.getMbocState())),
            createRequestedGoodAction(ActionType.UPDATE, good)
                .setPropertyName("ID поставщика")
                .setNewValue(String.valueOf(good.getSupplierId())),
            createRequestedGoodAction(ActionType.UPDATE, good)
                .setPropertyName("ID SKU поставщика")
                .setNewValue(String.valueOf(good.getSupplierSkuId())),
            createRequestedGoodAction(ActionType.UPDATE, good)
                .setPropertyName("ID SKU маркета")
                .setNewValue(String.valueOf(good.getMskuId())),
            createRequestedGoodAction(ActionType.UPDATE, good)
                .setPropertyName("Имя SKU маркета")
                .setNewValue(String.valueOf(good.getMskuTitle())),
            createRequestedGoodAction(ActionType.UPDATE, good)
                .setPropertyName("ID категории")
                .setNewValue(String.valueOf(good.getCategoryId())),
            createRequestedGoodAction(ActionType.UPDATE, good)
                .setPropertyName("ID планируемого перемещения")
                .setNewValue(String.valueOf(good.getRequestedMovementId())),
            createRequestedGoodAction(ActionType.UPDATE, good)
                .setPropertyName("ID товара")
                .setNewValue(String.valueOf(good.getGoodId())),
            createRequestedGoodAction(ActionType.UPDATE, good)
                .setPropertyName("Детали состояния")
                .setNewValue(good.getComment()));
    }

    @Test
    public void testRequestedGoodUpdateActions() {
        RequestedGood good = random.nextObject(RequestedGood.class);
        RequestedGood updated = new RequestedGood(good);
        good.setState(RequestedGoodState.NEW);
        updated.setState(RequestedGoodState.PLANNED);
        good.setMbocState(RequestedGoodMbocState.CL_PLANNING);
        updated.setMbocState(RequestedGoodMbocState.CL_FAILED);
        good.setSupplierId(1L);
        updated.setSupplierId(2L);
        good.setSupplierSkuId("asd");
        updated.setSupplierSkuId("qwe");
        good.setMskuId(1L);
        updated.setMskuId(2L);
        good.setCategoryId(1L);
        updated.setCategoryId(2L);
        good.setRequestedMovementId(1L);
        updated.setRequestedMovementId(2L);
        good.setMskuTitle("qweqw");
        updated.setMskuTitle("qweqweq");
        good.setGoodId(1L);
        updated.setGoodId(2L);
        good.setComment("qweqw");
        updated.setComment("qweqweq");

        List<AuditAction> actions = auditService.createAuditActions(
            new RequestedGoodWrapper(good), new RequestedGoodWrapper(updated));

        assertDateExistsAndClean(actions);
        assertThat(actions).containsExactlyInAnyOrder(
            createRequestedGoodAction(ActionType.UPDATE, updated)
                .setPropertyName("Состояние")
                .setOldValue(String.valueOf(good.getState()))
                .setNewValue(String.valueOf(updated.getState())),
            createRequestedGoodAction(ActionType.UPDATE, updated)
                .setPropertyName("Состояние в MBOC")
                .setOldValue(String.valueOf(good.getMbocState()))
                .setNewValue(String.valueOf(updated.getMbocState())),
            createRequestedGoodAction(ActionType.UPDATE, updated)
                .setPropertyName("ID поставщика")
                .setOldValue(String.valueOf(good.getSupplierId()))
                .setNewValue(String.valueOf(updated.getSupplierId())),
            createRequestedGoodAction(ActionType.UPDATE, updated)
                .setPropertyName("ID SKU поставщика")
                .setOldValue(String.valueOf(good.getSupplierSkuId()))
                .setNewValue(String.valueOf(updated.getSupplierSkuId())),
            createRequestedGoodAction(ActionType.UPDATE, updated)
                .setPropertyName("ID SKU маркета")
                .setOldValue(String.valueOf(good.getMskuId()))
                .setNewValue(String.valueOf(updated.getMskuId())),
            createRequestedGoodAction(ActionType.UPDATE, updated)
                .setPropertyName("ID категории")
                .setOldValue(String.valueOf(good.getCategoryId()))
                .setNewValue(String.valueOf(updated.getCategoryId())),
            createRequestedGoodAction(ActionType.UPDATE, updated)
                .setPropertyName("ID планируемого перемещения")
                .setOldValue(String.valueOf(good.getRequestedMovementId()))
                .setNewValue(String.valueOf(updated.getRequestedMovementId())),
            createRequestedGoodAction(ActionType.UPDATE, updated)
                .setPropertyName("Имя SKU маркета")
                .setOldValue(String.valueOf(good.getMskuTitle()))
                .setNewValue(String.valueOf(updated.getMskuTitle())),
            createRequestedGoodAction(ActionType.UPDATE, updated)
                .setPropertyName("ID товара")
                .setOldValue(String.valueOf(good.getGoodId()))
                .setNewValue(String.valueOf(updated.getGoodId())),
            createRequestedGoodAction(ActionType.UPDATE, updated)
                .setPropertyName("Детали состояния")
                .setOldValue(good.getComment())
                .setNewValue(updated.getComment())
        );
    }

    @Test
    public void testRequestedGoodDeleteActions() {
        RequestedGood good = random.nextObject(RequestedGood.class);

        List<AuditAction> actions = auditService.createAuditActions(new RequestedGoodWrapper(good), null);

        assertDateExistsAndClean(actions);
        assertThat(actions).containsExactlyInAnyOrder(
            createRequestedGoodAction(ActionType.DELETE, good)
        );
    }

    @Test
    public void testCategoryAddActions() {
        Category category = random.nextObject(Category.class);

        List<AuditAction> actions = auditService.createAuditActions(null, new CategoryWrapper(category));

        assertDateExistsAndClean(actions);
        assertThat(actions).containsExactlyInAnyOrder(
            createCategoryAction(ActionType.CREATE, category),
            createCategoryAction(ActionType.UPDATE, category)
                .setPropertyName("ID типа товара")
                .setNewValue(String.valueOf(category.getGoodTypeId())),
            createCategoryAction(ActionType.UPDATE, category)
                .setPropertyName("Минимальное колличество не обработанных фотографий")
                .setNewValue(String.valueOf(category.getMinRawPhotos())),
            createCategoryAction(ActionType.UPDATE, category)
                .setPropertyName("Минимальное колличество обработанных фотографий")
                .setNewValue(String.valueOf(category.getMinProcessedPhotos())),
            createCategoryAction(ActionType.UPDATE, category)
                .setPropertyName("Инструкция фотографа")
                .setNewValue(ArrayUtils.isEmpty(category.getPhotoInstruction()) ? null : "+"),
            createCategoryAction(ActionType.UPDATE, category)
                .setPropertyName("Инструкция ретушера")
                .setNewValue(ArrayUtils.isEmpty(category.getPhotoEditInstruction()) ? null : "+"),
            createCategoryAction(ActionType.UPDATE, category)
                .setPropertyName("Инструкция редактора")
                .setNewValue(ArrayUtils.isEmpty(category.getEditorInstruction()) ? null : "+")
        );
    }

    @Test
    public void testCategoryUpdateActions() {
        Category category = random.nextObject(Category.class);
        Category updated = new Category(category);
        category.setGoodTypeId(1L);
        updated.setGoodTypeId(2L);
        category.setMinRawPhotos(1);
        updated.setMinRawPhotos(2);
        category.setMinProcessedPhotos(3);
        updated.setMinProcessedPhotos(4);
        category.setPhotoInstruction(new byte[] {1, 2, 3, 4});
        updated.setPhotoInstruction();
        category.setPhotoEditInstruction(new byte[] {2});
        updated.setPhotoEditInstruction(new byte[] {5});
        category.setEditorInstruction();
        updated.setEditorInstruction(new byte[] {1, 2, 3});

        List<AuditAction> actions = auditService.createAuditActions(
            new CategoryWrapper(category), new CategoryWrapper(updated));

        assertDateExistsAndClean(actions);
        assertThat(actions).containsExactlyInAnyOrder(
            createCategoryAction(ActionType.UPDATE, category)
                .setPropertyName("ID типа товара")
                .setOldValue(String.valueOf(category.getGoodTypeId()))
                .setNewValue(String.valueOf(updated.getGoodTypeId())),
            createCategoryAction(ActionType.UPDATE, category)
                .setPropertyName("Минимальное колличество не обработанных фотографий")
                .setOldValue(String.valueOf(category.getMinRawPhotos()))
                .setNewValue(String.valueOf(updated.getMinRawPhotos())),
            createCategoryAction(ActionType.UPDATE, category)
                .setPropertyName("Минимальное колличество обработанных фотографий")
                .setOldValue(String.valueOf(category.getMinProcessedPhotos()))
                .setNewValue(String.valueOf(updated.getMinProcessedPhotos())),
            createCategoryAction(ActionType.UPDATE, category)
                .setPropertyName("Инструкция фотографа")
                .setOldValue(ArrayUtils.isEmpty(category.getPhotoInstruction()) ? null : "+")
                .setNewValue(ArrayUtils.isEmpty(updated.getPhotoInstruction()) ? null : "+"),
            createCategoryAction(ActionType.UPDATE, category)
                .setPropertyName("Инструкция ретушера")
                .setOldValue(ArrayUtils.isEmpty(category.getPhotoEditInstruction()) ? null : "+")
                .setNewValue(ArrayUtils.isEmpty(updated.getPhotoEditInstruction()) ? null : "+"),
            createCategoryAction(ActionType.UPDATE, category)
                .setPropertyName("Инструкция редактора")
                .setOldValue(ArrayUtils.isEmpty(category.getEditorInstruction()) ? null : "+")
                .setNewValue(ArrayUtils.isEmpty(updated.getEditorInstruction()) ? null : "+")
        );
    }

    @Test
    public void testGoodTypeAddActions() {
        GoodType goodType = random.nextObject(GoodType.class);

        List<AuditAction> actions = auditService.createAuditActions(null, new GoodTypeWrapper(goodType));

        assertDateExistsAndClean(actions);
        assertThat(actions).containsExactlyInAnyOrder(
            createGoodTypeAction(ActionType.CREATE, goodType),
            createGoodTypeAction(ActionType.UPDATE, goodType)
                .setPropertyName("Название")
                .setNewValue(String.valueOf(goodType.getDisplayName()))
        );
    }

    @Test
    public void testGoodTypeUpdateActions() {
        GoodType goodType = random.nextObject(GoodType.class);
        GoodType updated = new GoodType(goodType);
        goodType.setDisplayName("before name");
        updated.setDisplayName("after name");

        List<AuditAction> actions = auditService.createAuditActions(
            new GoodTypeWrapper(goodType), new GoodTypeWrapper(updated));

        assertDateExistsAndClean(actions);
        assertThat(actions).containsExactlyInAnyOrder(
            createGoodTypeAction(ActionType.UPDATE, updated)
                .setPropertyName("Название")
                .setOldValue(String.valueOf(goodType.getDisplayName()))
                .setNewValue(String.valueOf(updated.getDisplayName()))
        );
    }

    @Test
    public void testGoodTypeRemoveActions() {
        GoodType goodType = random.nextObject(GoodType.class);

        List<AuditAction> actions = auditService.createAuditActions(new GoodTypeWrapper(goodType), null);

        assertDateExistsAndClean(actions);
        assertThat(actions).containsExactlyInAnyOrder(
            createGoodTypeAction(ActionType.DELETE, goodType)
        );
    }

    @Test
    public void testRequestedMovementAddActions() {
        RequestedMovement movement = random.nextObject(RequestedMovement.class);

        List<AuditAction> actions = auditService.createAuditActions(null, new RequestedMovementWrapper(movement));

        assertDateExistsAndClean(actions);
        assertThat(actions).containsExactlyInAnyOrder(
            createRequestedMovementAction(ActionType.CREATE, movement),
            createRequestedMovementAction(ActionType.UPDATE, movement)
                .setPropertyName("Состояние")
                .setNewValue(String.valueOf(movement.getState())),
            createRequestedMovementAction(ActionType.UPDATE, movement)
                .setPropertyName("Направление")
                .setNewValue(String.valueOf(movement.getDirection())),
            createRequestedMovementAction(ActionType.UPDATE, movement)
                .setPropertyName("Детали состояния")
                .setNewValue(movement.getStateMessage()),
            createRequestedMovementAction(ActionType.UPDATE, movement)
                .setPropertyName("ID запросов в ERP")
                .setNewValue(movement.getErpRequestIds()),
            createRequestedMovementAction(ActionType.UPDATE, movement)
                .setPropertyName("ID запросов в FF")
                .setNewValue(movement.getFfRequestIds()),
            createRequestedMovementAction(ActionType.UPDATE, movement)
                .setPropertyName("ID склада источника")
                .setNewValue(String.valueOf(movement.getWarehouseFromId())),
            createRequestedMovementAction(ActionType.UPDATE, movement)
                .setPropertyName("ID склада приемника")
                .setNewValue(String.valueOf(movement.getWarehouseToId()))
        );
    }

    @Test
    public void testRequestedMovementUpdateActions() {
        RequestedMovement movement = random.nextObject(RequestedMovement.class);
        RequestedMovement updated = new RequestedMovement(movement);
        movement.setState(RequestedMovementState.NEW);
        updated.setState(RequestedMovementState.REQUESTED);
        movement.setDirection(MovementDirection.INCOMING);
        updated.setDirection(MovementDirection.OUTGOING);
        updated.setStateMessage(movement.getStateMessage() + "qwe");
        updated.setErpRequestIds(movement.getErpRequestIds() + "qwe");
        updated.setFfRequestIds(movement.getFfRequestIds() + "qwe");
        movement.setWarehouseToId(145L);
        updated.setWarehouseToId(163L);
        movement.setWarehouseFromId(163L);
        updated.setWarehouseFromId(145L);

        List<AuditAction> actions = auditService.createAuditActions(
            new RequestedMovementWrapper(movement), new RequestedMovementWrapper(updated));

        assertDateExistsAndClean(actions);
        assertThat(actions).containsExactlyInAnyOrder(
            createRequestedMovementAction(ActionType.UPDATE, updated)
                .setPropertyName("Состояние")
                .setOldValue(String.valueOf(movement.getState()))
                .setNewValue(String.valueOf(updated.getState())),
            createRequestedMovementAction(ActionType.UPDATE, updated)
                .setPropertyName("Направление")
                .setOldValue(String.valueOf(movement.getDirection()))
                .setNewValue(String.valueOf(updated.getDirection())),
            createRequestedMovementAction(ActionType.UPDATE, updated)
                .setPropertyName("Детали состояния")
                .setOldValue(movement.getStateMessage())
                .setNewValue(updated.getStateMessage()),
            createRequestedMovementAction(ActionType.UPDATE, updated)
                .setPropertyName("ID запросов в ERP")
                .setOldValue(movement.getErpRequestIds())
                .setNewValue(updated.getErpRequestIds()),
            createRequestedMovementAction(ActionType.UPDATE, updated)
                .setPropertyName("ID запросов в FF")
                .setOldValue(movement.getFfRequestIds())
                .setNewValue(updated.getFfRequestIds()),
            createRequestedMovementAction(ActionType.UPDATE, updated)
                .setPropertyName("ID склада источника")
                .setOldValue(String.valueOf(movement.getWarehouseFromId()))
                .setNewValue(String.valueOf(updated.getWarehouseFromId())),
            createRequestedMovementAction(ActionType.UPDATE, updated)
                .setPropertyName("ID склада приемника")
                .setOldValue(String.valueOf(movement.getWarehouseToId()))
                .setNewValue(String.valueOf(updated.getWarehouseToId()))
        );
    }

    @Test
    public void testRequestedMovementDeleteActions() {
        RequestedMovement movement = random.nextObject(RequestedMovement.class);

        List<AuditAction> actions = auditService.createAuditActions(new RequestedMovementWrapper(movement), null);

        assertDateExistsAndClean(actions);
        assertThat(actions).containsExactlyInAnyOrder(
            createRequestedMovementAction(ActionType.DELETE, movement)
        );
    }

    private AuditAction createMovementAction(ActionType type, Movement movement) {
        return new AuditAction()
            .setActionType(type)
            .setEntityType(EntityType.MOVEMENT)
            .setStaffLogin("robot-mrk-clab-itest")
            .setEntityInternalId(movement.getId())
            .setEntityExternalId(movement.getExternalId())
            .setEntityName(movement.getExternalId());
    }

    private AuditAction createRequestedMovementAction(ActionType type, RequestedMovement movement) {
        return new AuditAction()
            .setActionType(type)
            .setEntityType(EntityType.REQUESTED_MOVEMENT)
            .setStaffLogin("robot-mrk-clab-itest")
            .setEntityInternalId(movement.getId())
            .setEntityExternalId(String.valueOf(movement.getId()));
    }

    private AuditAction createCartAction(ActionType type, Cart cart) {
        return new AuditAction()
            .setActionType(type)
            .setEntityType(EntityType.CART)
            .setStaffLogin("robot-mrk-clab-itest")
            .setEntityInternalId(cart.getId())
            .setEntityExternalId(cart.getExternalId())
            .setEntityName(cart.getExternalId());
    }

    private AuditAction createGoodAction(ActionType type, Good good) {
        return new AuditAction()
            .setActionType(type)
            .setEntityType(EntityType.GOOD)
            .setStaffLogin("robot-mrk-clab-itest")
            .setEntityInternalId(good.getId())
            .setEntityExternalId("" + good.getMskuId())
            .setEntityName(good.getMskuTitle());
    }

    private AuditAction createRequestedGoodAction(ActionType type, RequestedGood good) {
        return new AuditAction()
            .setActionType(type)
            .setEntityType(EntityType.REQUESTED_GOOD)
            .setStaffLogin("robot-mrk-clab-itest")
            .setEntityInternalId(good.getId())
            .setEntityExternalId("" + good.getMskuId())
            .setEntityName(good.getMskuTitle());
    }

    private AuditAction createCategoryAction(ActionType type, Category category) {
        return new AuditAction()
            .setActionType(type)
            .setEntityType(EntityType.CATEGORY)
            .setStaffLogin("robot-mrk-clab-itest")
            .setEntityInternalId(category.getId())
            .setEntityExternalId("" + category.getId())
            .setEntityName(category.getName());
    }

    private AuditAction createGoodTypeAction(ActionType type, GoodType goodType) {
        return new AuditAction()
            .setActionType(type)
            .setEntityType(EntityType.GOOD_TYPE)
            .setStaffLogin("robot-mrk-clab-itest")
            .setEntityInternalId(goodType.getId())
            .setEntityExternalId("" + goodType.getId())
            .setEntityName(goodType.getDisplayName());
    }

    private void assertDateExistsAndClean(List<AuditAction> actions) {
        assertThat(actions).noneMatch(a -> a.getActionDate() == null);
        actions.forEach(a -> a.setActionDate(null));
    }
}
