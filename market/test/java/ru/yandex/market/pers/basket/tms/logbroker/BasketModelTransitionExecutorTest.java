package ru.yandex.market.pers.basket.tms.logbroker;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;

import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.pers.basket.PersBasketTest;
import ru.yandex.market.pers.basket.model.BasketReferenceItem;
import ru.yandex.market.pers.basket.service.BasketService;
import ru.yandex.market.pers.list.model.BasketOwner;
import ru.yandex.market.pers.list.model.v2.enums.ReferenceType;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.pers.basket.controller.AbstractBasketControllerTest.generateItem;
import static ru.yandex.market.pers.basket.tms.logbroker.ModelTransitionTestHelper.generateModelTransition;
import static ru.yandex.market.pers.basket.tms.logbroker.ModelTransitionTestHelper.generateSimpleModelRevertTransition;
import static ru.yandex.market.pers.basket.tms.logbroker.ModelTransitionTestHelper.generateSimpleModelTransition;
import static ru.yandex.market.pers.list.model.v2.enums.MarketplaceColor.WHITE;

/**
 * @author Damir Shagaev / damir-vvlpx@ / 25.03.2022
 */
public class BasketModelTransitionExecutorTest extends PersBasketTest {
    @Autowired
    BasketModelTransitionExecutor basketModelTransitionExecutor;

    @Autowired
    private BasketService basketService;

    @Before
    public void resetConfiguration() {
        basketModelTransitionExecutor.updateLastProcessedTransition(0);
    }

    @Test
    public void testEmptyApplication() throws Exception {
        basketModelTransitionExecutor.activate();
        basketModelTransitionExecutor.doRealJob();
        Assert.assertEquals(0, basketModelTransitionExecutor.lastAppliedModelTransitionId());
    }

    @Test
    public void testComplexGradeTransitionWithExistingItem() throws Exception {
        ModelStorage.ModelTransition modelTransition = generateModelTransition(1,
            ModelStorage.ModelTransition.ModelType.MODEL,
            ModelStorage.ModelTransition.TransitionReason.CLUSTERIZATION,
            ModelStorage.ModelTransition.TransitionType.SPLIT,
            true);
        storeModelTransition(Collections.singletonList(modelTransition));

        createBasketItem(100, modelTransition.getOldEntityId());
        createBasketItem(100, modelTransition.getNewEntityId());

        Assert.assertEquals(1, getContentCountByResourceId(modelTransition.getOldEntityId()));
        Assert.assertEquals(1, getContentCountByResourceId(modelTransition.getNewEntityId()));

        basketModelTransitionExecutor.activate();
        basketModelTransitionExecutor.doRealJob();

        Assert.assertEquals(1, getContentCountByResourceId(modelTransition.getOldEntityId()));
        Assert.assertEquals(1, getContentCountByResourceId(modelTransition.getNewEntityId()));
    }

    @Test
    public void testUniqueViolation() throws Exception {
        ModelStorage.ModelTransition modelTransition = ModelTransitionTestHelper.generateModelTransition(1,
            ModelStorage.ModelTransition.ModelType.SKU,
            ModelStorage.ModelTransition.TransitionReason.CLUSTERIZATION,
            ModelStorage.ModelTransition.TransitionType.DUPLICATE,
            true);

        storeModelTransition(Collections.singletonList(modelTransition));

        basketModelTransitionExecutor.activate();
        basketModelTransitionExecutor.doRealJob();

        Assert.assertEquals(1, basketModelTransitionExecutor.lastAppliedModelTransitionId());
    }

    private void testApplicationSkeleton(ModelStorage.ModelTransition.TransitionType transitionType,
                                         ModelStorage.ModelTransition.ModelType modelType,
                                         int refType,
                                         int oldEntityItemsCount,
                                         int newEntityItemsCount,
                                         int transitionCount) throws Exception {
        ModelStorage.ModelTransition modelTransition = ModelTransitionTestHelper.generateModelTransition(1,
            modelType,
            ModelStorage.ModelTransition.TransitionReason.CLUSTERIZATION,
            transitionType,
            true);

        storeModelTransition(List.of(modelTransition));

        long basketItemId = createBasketItem(1234, modelTransition.getOldEntityId());

        assertEquals(1, getContentCountByResourceId(modelTransition.getOldEntityId()));
        assertEquals(0, getContentCountByResourceId(modelTransition.getNewEntityId()));

        basketModelTransitionExecutor.activate();
        basketModelTransitionExecutor.doRealJob();

        assertEquals(oldEntityItemsCount, getContentCountByResourceId(modelTransition.getOldEntityId()));
        assertEquals(newEntityItemsCount, getContentCountByResourceId(modelTransition.getNewEntityId()));
        Assert.assertEquals(transitionCount, getLogEntryCount(modelTransition, basketItemId));
        Assert.assertEquals(1, basketModelTransitionExecutor.lastAppliedModelTransitionId());
    }

    private int getLogEntryCount(ModelStorage.ModelTransition modelTransition, long itemId) {
        String sql = "SELECT count(*) FROM MODEL_TRANSITION_HISTORY " +
            "WHERE transition = ? AND old_model = ? AND new_model = ? AND entity_id = ?";
        return pgaasJdbcTemplate.queryForObject(sql, Integer.class, modelTransition.getId(),
            modelTransition.getOldEntityId(), modelTransition.getNewEntityId(), itemId);
    }

    @Test
    public void testSkuSplitApplication() throws Exception {
        testApplicationSkeleton(ModelStorage.ModelTransition.TransitionType.SPLIT, ModelStorage.ModelTransition.ModelType.SKU, 0, 1 ,0, 0);
    }

    @Test
    public void testSkuDuplicateApplication() throws Exception {
        testApplicationSkeleton(ModelStorage.ModelTransition.TransitionType.DUPLICATE, ModelStorage.ModelTransition.ModelType.SKU, 0, 1 ,0, 0);
    }

    @Test
    public void testModelSplitApplication() throws Exception {
        testApplicationSkeleton(ModelStorage.ModelTransition.TransitionType.SPLIT, ModelStorage.ModelTransition.ModelType.MODEL, 2, 0 ,1, 1);
    }

    @Test
    public void testModelDuplicateApplication() throws Exception {
        testApplicationSkeleton(ModelStorage.ModelTransition.TransitionType.DUPLICATE, ModelStorage.ModelTransition.ModelType.MODEL, 2, 0 ,1, 1);
    }

    @Test
    public void testClusterSplitApplication() throws Exception {
        testApplicationSkeleton(ModelStorage.ModelTransition.TransitionType.SPLIT, ModelStorage.ModelTransition.ModelType.CLUSTER, 2, 0 ,1, 1);
    }

    @Test
    public void testClusterDuplicateApplication() throws Exception {
        testApplicationSkeleton(ModelStorage.ModelTransition.TransitionType.DUPLICATE, ModelStorage.ModelTransition.ModelType.CLUSTER, 2, 0 ,1, 1);
    }

    @Test
    public void testRevertQuestionTransitionSimple() throws Exception {
        long old = 12;
        long new1 = 123;
        long new2 = 1234;

        createBasketItem(100, old);
        createBasketItem(200, new1);
        createBasketItem(300, new2);

        basketModelTransitionExecutor.activate();

        storeModelTransition(List.of(
            generateSimpleModelTransition(1, old, new1),
            generateSimpleModelTransition(2, new1, new2)
        ));
        basketModelTransitionExecutor.doRealJob();

        assertEquals(0, getContentCountByResourceId(old));
        assertEquals(0, getContentCountByResourceId(new1));
        assertEquals(3, getContentCountByResourceId(new2));


        // create revert transition to old
        storeModelTransition(List.of(
            generateSimpleModelRevertTransition(3, old, old)
        ));
        basketModelTransitionExecutor.doRealJob();

        assertEquals(1, getContentCountByResourceId(old));
        assertEquals(0, getContentCountByResourceId(new1));
        assertEquals(2, getContentCountByResourceId(new2));

        storeModelTransition(List.of(
            generateSimpleModelRevertTransition(4, new1, new1)
        ));
        basketModelTransitionExecutor.doRealJob();

        assertEquals(1, getContentCountByResourceId(old));
        assertEquals(1, getContentCountByResourceId(new1));
        assertEquals(1, getContentCountByResourceId(new2));
    }

    @Test
    public void testRevertQuestionTransitionWithLoops() throws Exception {
        long old = 1234;
        long new1 = 12345;
        long new2 = 123456;
        long new3 = 1234567;

        createBasketItem(100, old);
        createBasketItem(200, new1);
        createBasketItem(300, new2);

        assertEquals(1, getContentCountByResourceId(old));
        assertEquals(1, getContentCountByResourceId(new1));
        assertEquals(1, getContentCountByResourceId(new2));
        assertEquals(0, getContentCountByResourceId(new3));

        basketModelTransitionExecutor.activate();

        storeModelTransition(List.of(
            generateSimpleModelTransition(1, old, new1),
            generateSimpleModelTransition(2, new1, new2),
            generateSimpleModelTransition(3, new2, new3)
        ));
        basketModelTransitionExecutor.doRealJob();

        // видим, что все отзывы переехали на модель new3
        assertEquals(0, getContentCountByResourceId(old));
        assertEquals(0, getContentCountByResourceId(new1));
        assertEquals(0, getContentCountByResourceId(new2));
        assertEquals(3, getContentCountByResourceId(new3));

        storeModelTransition(List.of(
            generateSimpleModelRevertTransition(4, old, old)
        ));
        basketModelTransitionExecutor.doRealJob();

        assertEquals(1, getContentCountByResourceId(old));
        assertEquals(0, getContentCountByResourceId(new1));
        assertEquals(0, getContentCountByResourceId(new2));
        assertEquals(2, getContentCountByResourceId(new3));

        storeModelTransition(List.of(
            generateSimpleModelRevertTransition(5, new1, new1)
        ));
        basketModelTransitionExecutor.doRealJob();

        assertEquals(1, getContentCountByResourceId(old));
        assertEquals(1, getContentCountByResourceId(new1));
        assertEquals(0, getContentCountByResourceId(new2));
        assertEquals(1, getContentCountByResourceId(new3));

        // переезд old после revert old
        storeModelTransition(List.of(
            generateSimpleModelTransition(6, old, new2)
        ));
        basketModelTransitionExecutor.doRealJob();

        assertEquals(0, getContentCountByResourceId(old));
        assertEquals(1, getContentCountByResourceId(new1));
        assertEquals(1, getContentCountByResourceId(new2));
        assertEquals(1, getContentCountByResourceId(new3));

        // был в прошлом переезд old -> new1, проверяем, что откат не заденет old
        storeModelTransition(List.of(
            generateSimpleModelRevertTransition(7, new1, new1)
        ));
        basketModelTransitionExecutor.doRealJob();

        assertEquals(0, getContentCountByResourceId(old));
        assertEquals(1, getContentCountByResourceId(new1));
        assertEquals(1, getContentCountByResourceId(new2));
        assertEquals(1, getContentCountByResourceId(new3));

        // откатываем переезд на old
        storeModelTransition(List.of(
            generateSimpleModelRevertTransition(8, old, old)
        ));
        basketModelTransitionExecutor.doRealJob();

        assertEquals(1, getContentCountByResourceId(old));
        assertEquals(1, getContentCountByResourceId(new1));
        assertEquals(0, getContentCountByResourceId(new2));
        assertEquals(1, getContentCountByResourceId(new3));
    }

    @Test
    public void testRevertQuestionTransitionWithLoops2() throws Exception {
        long old = 1234;
        long new1 = 12345;
        long new2 = 123456;
        long new3 = 1234567;

        createBasketItem(100, old);
        createBasketItem(200, new1);
        createBasketItem(300, new2);

        assertEquals(1, getContentCountByResourceId(old));
        assertEquals(1, getContentCountByResourceId(new1));
        assertEquals(1, getContentCountByResourceId(new2));
        assertEquals(0, getContentCountByResourceId(new3));

        basketModelTransitionExecutor.activate();

        storeModelTransition(List.of(
            generateSimpleModelTransition(1, old, new1),
            generateSimpleModelTransition(2, new1, new2),
            generateSimpleModelTransition(3, new2, new3)
        ));
        basketModelTransitionExecutor.doRealJob();

        // видим, что все отзывы переехали на модель new3
        assertEquals(0, getContentCountByResourceId(old));
        assertEquals(0, getContentCountByResourceId(new1));
        assertEquals(0, getContentCountByResourceId(new2));
        assertEquals(3, getContentCountByResourceId(new3));

        storeModelTransition(List.of(
            generateSimpleModelRevertTransition(4, new1, new1)
        ));
        basketModelTransitionExecutor.doRealJob();

        assertEquals(0, getContentCountByResourceId(old));
        assertEquals(2, getContentCountByResourceId(new1));
        assertEquals(0, getContentCountByResourceId(new2));
        assertEquals(1, getContentCountByResourceId(new3));

        storeModelTransition(List.of(
            generateSimpleModelRevertTransition(5, old, old)
        ));
        basketModelTransitionExecutor.doRealJob();

        assertEquals(1, getContentCountByResourceId(old));
        assertEquals(1, getContentCountByResourceId(new1));
        assertEquals(0, getContentCountByResourceId(new2));
        assertEquals(1, getContentCountByResourceId(new3));

        // переезд old после revert old
        storeModelTransition(List.of(
            generateSimpleModelTransition(6, old, new2)
        ));
        basketModelTransitionExecutor.doRealJob();

        assertEquals(0, getContentCountByResourceId(old));
        assertEquals(1, getContentCountByResourceId(new1));
        assertEquals(1, getContentCountByResourceId(new2));
        assertEquals(1, getContentCountByResourceId(new3));

        // был в прошлом переезд old -> new1, проверяем, что откат не заденет old
        storeModelTransition(List.of(
            generateSimpleModelRevertTransition(7, new1, new1)
        ));
        basketModelTransitionExecutor.doRealJob();

        assertEquals(0, getContentCountByResourceId(old));
        assertEquals(1, getContentCountByResourceId(new1));
        assertEquals(1, getContentCountByResourceId(new2));
        assertEquals(1, getContentCountByResourceId(new3));

        // откатываем переезд на old
        storeModelTransition(List.of(
            generateSimpleModelRevertTransition(8, old, old)
        ));
        basketModelTransitionExecutor.doRealJob();

        assertEquals(1, getContentCountByResourceId(old));
        assertEquals(1, getContentCountByResourceId(new1));
        assertEquals(0, getContentCountByResourceId(new2));
        assertEquals(1, getContentCountByResourceId(new3));
    }

    private long getContentCountByResourceId(long referenceId) {
        return pgaasJdbcTemplate.queryForObject("select count(*) from basket_items where reference_id = ?", Long.class, String.valueOf(referenceId));
    }

    private long createBasketItem(long uid, long referenceId) {
        BasketOwner owner = BasketOwner.fromUid(uid);
        Long ownerId = basketService.getOrAddOwnerId(owner);
        BasketReferenceItem item = generateItem(WHITE, ReferenceType.PRODUCT, String.valueOf(referenceId));
        item.setOwnerId(ownerId);
        return basketService.addItem(item, owner).getId();
    }

    private void storeModelTransition(List<ModelStorage.ModelTransition> modelTransitions) {
        pgaasJdbcTemplate.batchUpdate(
            "insert into model_transition(id, action_id, cr_time, type, reason, entity_type, old_entity_id, old_entity_id_deleted, new_entity_id, primary_transition) " +
                "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING",
            modelTransitions, modelTransitions.size(), SAVE_PS_SETTER);
    }

    private static final ParameterizedPreparedStatementSetter<ModelStorage.ModelTransition> SAVE_PS_SETTER =
        (ps, argument) -> {
            int k = 1;
            ps.setLong(k++, argument.getId());
            ps.setLong(k++, argument.getActionId());
            ps.setTimestamp(k++, new Timestamp(argument.getDate()));
            ps.setInt(k++, argument.getType().getNumber());
            ps.setInt(k++, argument.getReason().getNumber());
            ps.setInt(k++, argument.getModelType().getNumber());
            ps.setLong(k++, argument.getOldEntityId());
            ps.setBoolean(k++, argument.getOldEntityDeleted());
            ps.setLong(k++, argument.getNewEntityId());
            ps.setBoolean(k, argument.getPrimaryTransition());
        };
}
