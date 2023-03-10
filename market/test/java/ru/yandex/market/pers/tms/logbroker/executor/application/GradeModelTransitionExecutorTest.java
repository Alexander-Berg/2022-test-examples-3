package ru.yandex.market.pers.tms.logbroker.executor.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.tms.MockedPersTmsTest;
import ru.yandex.market.pers.tms.logbroker.consumer.ModelTransitionGradeConsumer;
import ru.yandex.market.pers.tms.logbroker.executor.ModelTransitionTestHelper;

import static ru.yandex.market.pers.tms.logbroker.executor.ModelTransitionTestHelper.generateModelTransition;
import static ru.yandex.market.pers.tms.logbroker.executor.ModelTransitionTestHelper.generateSimpleModelRevertTransition;
import static ru.yandex.market.pers.tms.logbroker.executor.ModelTransitionTestHelper.generateSimpleModelTransition;
import static ru.yandex.market.pers.tms.logbroker.executor.ModelTransitionTestHelper.id;

public class GradeModelTransitionExecutorTest extends MockedPersTmsTest {

    @Autowired
    private GradeModelTransitionExecutor gradeModelTransitionExecutor;
    @Autowired
    private ModelTransitionGradeConsumer modelTransitionGradeConsumer;
    @Autowired
    private GradeCreator gradeCreator;

    @Test
    public void testEmptyApplication() throws Exception {
        gradeModelTransitionExecutor.activate();
        gradeModelTransitionExecutor.runTmsJob();
        Assert.assertEquals(0, gradeModelTransitionExecutor.lastAppliedModelTransitionId());
    }

    private void testApplicationSkeleton(ModelStorage.ModelTransition.ModelType modelType,
                                         ModelStorage.ModelTransition.TransitionType transitionType,
                                         boolean primaryTransition) throws Exception {
        ModelStorage.ModelTransition modelTransition = generateModelTransition(1,
            modelType,
            ModelStorage.ModelTransition.TransitionReason.CLUSTERIZATION,
            transitionType,
            primaryTransition);
        modelTransitionGradeConsumer.accept(Collections.singletonList(modelTransition));

        long gradeId = gradeCreator.createModelGrade(modelTransition.getOldEntityId(), 12345L);

        long contentCount = getContentCountByResourceId(modelTransition.getOldEntityId());
        Assert.assertEquals(1, contentCount);
        contentCount = getContentCountByResourceId(modelTransition.getNewEntityId());
        Assert.assertEquals(0, contentCount);

        gradeModelTransitionExecutor.activate();
        gradeModelTransitionExecutor.runTmsJob();

        if (primaryTransition) {
            contentCount = getContentCountByResourceId(modelTransition.getOldEntityId());
            Assert.assertEquals(0, contentCount);
            contentCount = getContentCountByResourceId(modelTransition.getNewEntityId());
            Assert.assertEquals(1, contentCount);

            Assert.assertEquals(0, getWeakTransitionCount());

            Assert.assertEquals(Integer.valueOf(1), getLogEntryCount(modelTransition, gradeId));
        } else {
            contentCount = getContentCountByResourceId(modelTransition.getOldEntityId());
            Assert.assertEquals(1, contentCount);
            contentCount = getContentCountByResourceId(modelTransition.getNewEntityId());
            Assert.assertEquals(0, contentCount);

            Assert.assertEquals(1, getWeakTransitionCount());

            Assert.assertEquals(Integer.valueOf(0), getLogEntryCount(modelTransition, gradeId));
        }
    }

    private Integer getLogEntryCount(ModelStorage.ModelTransition modelTransition, long gradeId) {
        String sql = "SELECT count(*) FROM GRADE_TRANSITION_HISTORY " +
            "WHERE transition = ? AND old_resource = ? AND new_resource = ? AND grade_id = ?";
        return pgJdbcTemplate.queryForObject(sql, Integer.class, modelTransition.getId(),
            modelTransition.getOldEntityId(), modelTransition.getNewEntityId(), gradeId);
    }

    @Test
    public void testClusterSplitApplicationPrimary() throws Exception {
        testApplicationSkeleton(ModelStorage.ModelTransition.ModelType.CLUSTER,
            ModelStorage.ModelTransition.TransitionType.SPLIT,
            true);
    }

    @Test
    public void testClusterDuplicateApplicationPrimary() throws Exception {
        testApplicationSkeleton(ModelStorage.ModelTransition.ModelType.CLUSTER,
            ModelStorage.ModelTransition.TransitionType.DUPLICATE,
            true);
    }

    @Test
    public void testModelSplitApplicationPrimary() throws Exception {
        testApplicationSkeleton(ModelStorage.ModelTransition.ModelType.MODEL,
            ModelStorage.ModelTransition.TransitionType.SPLIT,
            true);
    }

    @Test
    public void testModelDuplicateApplicationPrimary() throws Exception {
        testApplicationSkeleton(ModelStorage.ModelTransition.ModelType.MODEL,
            ModelStorage.ModelTransition.TransitionType.DUPLICATE,
            true);
    }

    @Test
    public void testClusterSplitApplicationNonPrimary() throws Exception {
        testApplicationSkeleton(ModelStorage.ModelTransition.ModelType.CLUSTER,
            ModelStorage.ModelTransition.TransitionType.SPLIT,
            false);
    }

    @Test
    public void testModelSplitApplicationNonPrimary() throws Exception {
        testApplicationSkeleton(ModelStorage.ModelTransition.ModelType.MODEL,
            ModelStorage.ModelTransition.TransitionType.SPLIT,
            false);
    }

    private List<ModelStorage.ModelTransition> generateAllNotUsefulTransitions() {
        List<ModelStorage.ModelTransition> modelTransitions = new ArrayList<>();
        int id = 1;
        for (ModelStorage.ModelTransition.TransitionType transitionType :
            ModelStorage.ModelTransition.TransitionType.values()) {
            for (ModelStorage.ModelTransition.ModelType modelType : ModelStorage.ModelTransition.ModelType.values()) {
                modelTransitions.add(generateModelTransition(id++,
                    modelType,
                    ModelTransitionTestHelper.transitionReason,
                    transitionType,
                    false));
                modelTransitions.add(generateModelTransition(id++,
                    modelType,
                    ModelTransitionTestHelper.transitionReason,
                    transitionType,
                    true));
            }
        }
        return modelTransitions.stream()
            .filter(modelTransition -> modelTransition.getType() != ModelStorage.ModelTransition.TransitionType.DUPLICATE)
            .filter(modelTransition -> modelTransition.getType() != ModelStorage.ModelTransition.TransitionType.SPLIT)
            .collect(Collectors.toList());
    }

    private long getWeakTransitionCount() {
        return Optional.ofNullable(
            pgJdbcTemplate.queryForObject("select count(*) from model_transition_weak", Long.class)
        ).orElse(0L);
    }

    private long getContentCountByResourceId(long resourceId) {
        return Optional.ofNullable(
            pgJdbcTemplate.queryForObject("select count(*) from grade where resource_id = ?",
                Long.class,
                resourceId)
        ).orElse(0L);
    }

    @Test
    public void testNotUsefulApplication() throws Exception {
        for (ModelStorage.ModelTransition modelTransition : generateAllNotUsefulTransitions()) {
            initMocks();
            cleanDatabase();
            notUsefulApplicationSkeleton(modelTransition);
        }
    }

    public void notUsefulApplicationSkeleton(ModelStorage.ModelTransition modelTransition) throws Exception {
        gradeCreator.createModelGrade(modelTransition.getOldEntityId(), 12345L);

        Assert.assertEquals(0, getWeakTransitionCount());

        Assert.assertEquals(1, getContentCountByResourceId(modelTransition.getOldEntityId()));
        Assert.assertEquals(0, getContentCountByResourceId(modelTransition.getNewEntityId()));

        modelTransitionGradeConsumer.accept(List.of(modelTransition));

        gradeModelTransitionExecutor.activate();
        gradeModelTransitionExecutor.runTmsJob();

        Assert.assertEquals(0, getWeakTransitionCount());

        Assert.assertEquals(1, getContentCountByResourceId(modelTransition.getOldEntityId()));
        Assert.assertEquals(0, getContentCountByResourceId(modelTransition.getNewEntityId()));

        Assert.assertEquals(modelTransition.getId(), gradeModelTransitionExecutor.lastAppliedModelTransitionId());
    }

    /**
     * ?????????????? ???????????? ???? ???????????????????????? ???? ???????????? ?? ?????????? id?????????? -> ?????????????? ???? ??????????????????????
     */
    @Test
    public void testComplexGradeTransitionWithExistingGrade() throws Exception {
        ModelStorage.ModelTransition modelTransition = generateModelTransition(1,
            ModelStorage.ModelTransition.ModelType.MODEL,
            ModelStorage.ModelTransition.TransitionReason.CLUSTERIZATION,
            ModelStorage.ModelTransition.TransitionType.SPLIT,
            true);
        modelTransitionGradeConsumer.accept(Collections.singletonList(modelTransition));

        gradeCreator.createModelGrade(modelTransition.getOldEntityId(), 12345L);
        gradeCreator.createModelGrade(modelTransition.getNewEntityId(), 12345L);

        Assert.assertEquals(1, getContentCountByResourceId(modelTransition.getOldEntityId()));
        Assert.assertEquals(1, getContentCountByResourceId(modelTransition.getNewEntityId()));

        gradeModelTransitionExecutor.activate();
        gradeModelTransitionExecutor.runTmsJob();

        Assert.assertEquals(1, getContentCountByResourceId(modelTransition.getOldEntityId()));
        Assert.assertEquals(1, getContentCountByResourceId(modelTransition.getNewEntityId()));
    }

    /**
     * ?????????????? ???????????? ???? ???????????????????????? ???? ???????????? ?? ?????????? id??????????, ?????? ???????? ???? ?????????? ???????????? ???? ?????????????????? (state in (1,
     * 2)) -> ?????????????? ??????????????????????
     */
    @Test
    public void testComplexGradeTransition() throws Exception {
        ModelStorage.ModelTransition modelTransition = generateModelTransition(1,
            ModelStorage.ModelTransition.ModelType.MODEL,
            ModelStorage.ModelTransition.TransitionReason.CLUSTERIZATION,
            ModelStorage.ModelTransition.TransitionType.SPLIT,
            true);
        modelTransitionGradeConsumer.accept(Collections.singletonList(modelTransition));

        // ?????????????? ???? ?????????? ???????????? ????????????, ?????????? ???????????? ??????????????,
        long authorId = GradeCreator.rndUid();
        long gradeIdOld = gradeCreator.createModelGrade(modelTransition.getNewEntityId(), authorId);
        long gradeIdDeleted = gradeCreator.createModelGrade(modelTransition.getNewEntityId(), authorId);
        pgJdbcTemplate.update("update grade set state = 2 where id = ?", gradeIdDeleted);
        Assert.assertNotEquals(gradeIdOld, gradeIdDeleted);

        // ?? ?????????????? ???????????? ???? ???????????? ????????????, ?????????????? ?????????????? ??????????????????
        gradeCreator.createModelGrade(modelTransition.getOldEntityId(), authorId);

        Assert.assertEquals(1, getContentCountByResourceId(modelTransition.getOldEntityId()));
        Assert.assertEquals(2, getContentCountByResourceId(modelTransition.getNewEntityId()));

        gradeModelTransitionExecutor.activate();
        gradeModelTransitionExecutor.runTmsJob();

        Assert.assertEquals(0, getContentCountByResourceId(modelTransition.getOldEntityId()));
        Assert.assertEquals(3, getContentCountByResourceId(modelTransition.getNewEntityId()));
    }

    /**
     * ?????????????? ???????????? ???? ???????????????????????? ???? ???????????? ?? ?????????? id??????????, ?????? ???????? ???? ???????????? ???????????? ???? ?????????????????? (state in (1,
     * 2)) -> ?????????????? ??????????????????????
     */
    @Test
    public void testAnotherComplexGradeTransition() throws Exception {
        ModelStorage.ModelTransition modelTransition = generateModelTransition(1,
            ModelStorage.ModelTransition.ModelType.MODEL,
            ModelStorage.ModelTransition.TransitionReason.CLUSTERIZATION,
            ModelStorage.ModelTransition.TransitionType.SPLIT,
            true);
        modelTransitionGradeConsumer.accept(Collections.singletonList(modelTransition));

        // ?????????????? ???? ???????????? ???????????? state = 1 ?? 2 ???????????? (???????????? ?? ??????????????????)
        long authorId = GradeCreator.rndUid();
        gradeCreator.createModelGrade(modelTransition.getOldEntityId(), authorId);
        long gradeIdDeleted = gradeCreator.createModelGrade(modelTransition.getOldEntityId(), authorId);
        pgJdbcTemplate.update("update grade set state = 2 where id = ?", gradeIdDeleted);

        // ?? ???? ?????????? ???????????? ????????????????????
        gradeCreator.createModelGrade(modelTransition.getNewEntityId(), authorId);

        Assert.assertEquals(2, getContentCountByResourceId(modelTransition.getOldEntityId()));
        Assert.assertEquals(1, getContentCountByResourceId(modelTransition.getNewEntityId()));

        gradeModelTransitionExecutor.activate();
        gradeModelTransitionExecutor.runTmsJob();

        // ???????????? ?????????????????????? ??????
        Assert.assertEquals(0, getContentCountByResourceId(modelTransition.getOldEntityId()));
        Assert.assertEquals(3, getContentCountByResourceId(modelTransition.getNewEntityId()));
    }

    @Test
    public void testGradeTransitionWithRerun() throws Exception {
        // ?????????????????? ?????????????????? ?????????????? ??????????????
        // ???????????????? (0 ?????????????? ???????? ?????? ??????????????, 1-3 ???? ?????????????? ?????? ????????????):
        // 0. ???????????????? (?? ?????????????????? ???? ?????????? ????????????????) - ???? ??????????????????
        // 1. ???????????????? ??????????????, ???? ?? ???????????? ???????????? ???????????????????? ?????????????? - ???? ??????????????????
        // 2. ???????????????? (???????? ???????????? ?????? ????????????????) - ??????????????????
        // 3. ???????????????? (???????????? ????????????????????) - ????????????

        List<ModelStorage.ModelTransition> allTransitions = IntStream.range(0, 4)
            .mapToObj(ModelTransitionTestHelper::generateSimpleModelTransition)
            .collect(Collectors.toList());

        long uid = GradeCreator.rndUid();
        gradeCreator.createModelGrade(allTransitions.get(0).getOldEntityId(), uid);
        gradeCreator.createModelGrade(allTransitions.get(1).getOldEntityId(), uid);
        gradeCreator.createModelGrade(allTransitions.get(2).getOldEntityId(), uid);

        // ?????????????????? ?????????????? 1 (???????? ???? ???????????? 0 ?? 2, 3)
        modelTransitionGradeConsumer.accept(Collections.singletonList(allTransitions.get(1)));

        gradeModelTransitionExecutor.activate();
        gradeModelTransitionExecutor.runTmsJob();

        Assert.assertEquals(1, getContentCountByResourceId(allTransitions.get(0).getOldEntityId()));
        Assert.assertEquals(0, getContentCountByResourceId(allTransitions.get(0).getNewEntityId()));
        Assert.assertEquals(0, getContentCountByResourceId(allTransitions.get(1).getOldEntityId()));
        Assert.assertEquals(1, getContentCountByResourceId(allTransitions.get(1).getNewEntityId()));
        Assert.assertEquals(1, getContentCountByResourceId(allTransitions.get(2).getOldEntityId()));
        Assert.assertEquals(0, getContentCountByResourceId(allTransitions.get(2).getNewEntityId()));
        Assert.assertEquals(0, getContentCountByResourceId(allTransitions.get(3).getOldEntityId()));
        Assert.assertEquals(0, getContentCountByResourceId(allTransitions.get(3).getNewEntityId()));

        modelTransitionGradeConsumer.accept(allTransitions);

        gradeCreator.createModelGrade(allTransitions.get(1).getOldEntityId(), uid + 1);

        // ??????????????, ?????? ?????? ???????????????? ????????????????????
        gradeModelTransitionExecutor.updateLastProcessedTransition(4);

        // ???????? ?????? ?????????????? ?????????????? ????????????, ???????????? ???? ???????????? ????????????????????
        gradeModelTransitionExecutor.runTmsJob();

        Assert.assertEquals(1, getContentCountByResourceId(allTransitions.get(0).getOldEntityId()));
        Assert.assertEquals(0, getContentCountByResourceId(allTransitions.get(0).getNewEntityId()));
        Assert.assertEquals(1, getContentCountByResourceId(allTransitions.get(1).getOldEntityId()));
        Assert.assertEquals(1, getContentCountByResourceId(allTransitions.get(1).getNewEntityId()));
        Assert.assertEquals(1, getContentCountByResourceId(allTransitions.get(2).getOldEntityId()));
        Assert.assertEquals(0, getContentCountByResourceId(allTransitions.get(2).getNewEntityId()));
        Assert.assertEquals(0, getContentCountByResourceId(allTransitions.get(3).getOldEntityId()));
        Assert.assertEquals(0, getContentCountByResourceId(allTransitions.get(3).getNewEntityId()));

        // ?????????????????????? ?????????????? ????????????????, ???????????? ?????? ???????????????????????? ?????????? rerun
        pgJdbcTemplate.update("update model_transition set cr_time = now() - interval '2' hour where id > 0");
        pgJdbcTemplate.update("update model_transition set cr_time = now() - interval '13' hour where id = 0");
        gradeModelTransitionExecutor.runTmsJob();

        Assert.assertEquals(1, getContentCountByResourceId(allTransitions.get(0).getOldEntityId()));
        Assert.assertEquals(0, getContentCountByResourceId(allTransitions.get(0).getNewEntityId()));
        Assert.assertEquals(1, getContentCountByResourceId(allTransitions.get(1).getOldEntityId()));
        Assert.assertEquals(1, getContentCountByResourceId(allTransitions.get(1).getNewEntityId()));
        Assert.assertEquals(0, getContentCountByResourceId(allTransitions.get(2).getOldEntityId()));
        Assert.assertEquals(1, getContentCountByResourceId(allTransitions.get(2).getNewEntityId()));
        Assert.assertEquals(0, getContentCountByResourceId(allTransitions.get(3).getOldEntityId()));
        Assert.assertEquals(0, getContentCountByResourceId(allTransitions.get(3).getNewEntityId()));
    }

    @Test
    public void testGradeTransitionithNewEqMinusOne() throws Exception {
        ModelStorage.ModelTransition modelTransition = ModelStorage.ModelTransition.newBuilder()
            .setActionId(2)
            .setPrimaryTransition(true)
            .setModelType(ModelStorage.ModelTransition.ModelType.MODEL)
            .setDate(new Date().getTime())
            .setId(1)
            .setNewEntityId(-1L)
            .setOldEntityDeleted(false)
            .setOldEntityId(123)
            .setReason(ModelStorage.ModelTransition.TransitionReason.DUPLICATE_REMOVAL)
            .setType(ModelStorage.ModelTransition.TransitionType.DUPLICATE)
            .build();


        modelTransitionGradeConsumer.accept(Collections.singletonList(modelTransition));

        gradeCreator.createModelGrade(modelTransition.getOldEntityId(), GradeCreator.rndUid());

        Assert.assertEquals(1, getContentCountByResourceId(modelTransition.getOldEntityId()));
        Assert.assertEquals(0, getContentCountByResourceId(modelTransition.getNewEntityId()));

        gradeModelTransitionExecutor.activate();
        gradeModelTransitionExecutor.runTmsJob();

        Assert.assertEquals(1, getContentCountByResourceId(modelTransition.getOldEntityId()));
        Assert.assertEquals(0, getContentCountByResourceId(modelTransition.getNewEntityId()));
    }

    @Test
    public void testModelTransitionRevert() throws Exception {
        // ?????????????? 3 transition
        List<ModelStorage.ModelTransition> allTransitions = IntStream.range(1, 4)
            .mapToObj(id -> generateModelTransition(id,
                ModelStorage.ModelTransition.ModelType.MODEL,
                ModelStorage.ModelTransition.TransitionReason.CLUSTERIZATION,
                ModelStorage.ModelTransition.TransitionType.DUPLICATE))
            .collect(Collectors.toList());

        modelTransitionGradeConsumer.accept(allTransitions);

        // ?????????????? ???? ???????????? transition ????????????
        long uid = GradeCreator.rndUid();
        IntStream.range(0, 3).boxed()
            .forEach(i -> gradeCreator.createModelGrade(allTransitions.get(i).getOldEntityId(), uid));
        gradeCreator.createModelGrade(allTransitions.get(2).getOldEntityId(), uid);
        gradeCreator.createModelGrade(allTransitions.get(2).getNewEntityId(), uid + 1);

        // ??????????????????, ?????????????? ?????????????? ???? ???????????? ???????????? ?????????? ?????????????????????? ??????????????????
        Assert.assertEquals(2, getContentCountByResourceId(allTransitions.get(2).getOldEntityId()));
        Assert.assertEquals(1, getContentCountByResourceId(allTransitions.get(2).getNewEntityId()));
        Assert.assertEquals(1, getContentCountByResourceId(allTransitions.get(1).getOldEntityId()));
        Assert.assertEquals(0, getContentCountByResourceId(allTransitions.get(1).getNewEntityId()));
        Assert.assertEquals(1, getContentCountByResourceId(allTransitions.get(0).getOldEntityId()));
        Assert.assertEquals(0, getContentCountByResourceId(allTransitions.get(0).getNewEntityId()));

        // ?????????????????? ??????????????
        gradeModelTransitionExecutor.activate();
        gradeModelTransitionExecutor.runTmsJob();

        // ???????????????? ??????????????????, ???? ???????????? ?????????????? ?????? ??????????????
        Assert.assertEquals(0, getContentCountByResourceId(allTransitions.get(2).getOldEntityId()));
        Assert.assertEquals(3, getContentCountByResourceId(allTransitions.get(2).getNewEntityId()));
        Assert.assertEquals(0, getContentCountByResourceId(allTransitions.get(1).getOldEntityId()));
        Assert.assertEquals(1, getContentCountByResourceId(allTransitions.get(1).getNewEntityId()));
        Assert.assertEquals(0, getContentCountByResourceId(allTransitions.get(0).getOldEntityId()));
        Assert.assertEquals(1, getContentCountByResourceId(allTransitions.get(0).getNewEntityId()));


        // ?????????????? ?????? transition ???? ???????????? 2 ??????????????????
        modelTransitionGradeConsumer.accept(IntStream.range(1, 3)
            .mapToObj(i -> generateModelTransition(i + 50,
                ModelStorage.ModelTransition.ModelType.MODEL,
                ModelStorage.ModelTransition.TransitionReason.CLUSTERIZATION,
                ModelStorage.ModelTransition.TransitionType.REVERT,
                ModelTransitionTestHelper.primaryTransition,
                allTransitions.get(i).getOldEntityId(),
                allTransitions.get(i).getOldEntityId()))
            .collect(Collectors.toList())
        );

        // ?????????????????? ??????????
        gradeModelTransitionExecutor.activate();
        gradeModelTransitionExecutor.runTmsJob();

        // ??????????????????, ?????? ???????????? ?????????????????? ??????????????
        Assert.assertEquals(2, getContentCountByResourceId(allTransitions.get(2).getOldEntityId()));
        Assert.assertEquals(1, getContentCountByResourceId(allTransitions.get(2).getNewEntityId()));
        Assert.assertEquals(1, getContentCountByResourceId(allTransitions.get(1).getOldEntityId()));
        Assert.assertEquals(0, getContentCountByResourceId(allTransitions.get(1).getNewEntityId()));
        Assert.assertEquals(0, getContentCountByResourceId(allTransitions.get(0).getOldEntityId()));
        Assert.assertEquals(1, getContentCountByResourceId(allTransitions.get(0).getNewEntityId()));
    }

    @Test
    public void testModelTransitionRevertWithNonePrevious() throws Exception {
        long sourceModel = 1234;
        long firstChainModel = 12345;
        long secondChainModel = 123456;

        // ?????????????? 3 ????????????
        gradeCreator.createModelGrade(sourceModel, id + 100);
        gradeCreator.createModelGrade(firstChainModel, id + 101);
        gradeCreator.createModelGrade(secondChainModel, id + 102);

        // ??????????, ??????  ???????????? ???????????? ???? ????????????
        Assert.assertEquals(1, getContentCountByResourceId(sourceModel));
        Assert.assertEquals(1, getContentCountByResourceId(firstChainModel));
        Assert.assertEquals(1, getContentCountByResourceId(secondChainModel));

        // MBO ?????? ???? ???????????????? ???????????? ???????????????? ?? ???????????? sourceModel -> firstChainModel -> secondChainModel,
        // ?? ?????????????? ?????????? REVERT, ?????????????? ???? ????????????????????, ???????????? ?????? ???????????? ?????????????????? ?????????????? ???? ???????? ?????? ???????? ??????????????
        gradeModelTransitionExecutor.activate();
        modelTransitionGradeConsumer.accept(List.of(
            generateSimpleModelRevertTransition(1, sourceModel, firstChainModel),
            generateSimpleModelRevertTransition(2, firstChainModel, secondChainModel)
        ));
        gradeModelTransitionExecutor.runTmsJob();

        // ??????????, ??????  ???????????? ???????????? ???? ????????????, ?????????????? ?????????????????? ???? ??????????????????
        Assert.assertEquals(1, getContentCountByResourceId(sourceModel));
        Assert.assertEquals(1, getContentCountByResourceId(firstChainModel));
        Assert.assertEquals(1, getContentCountByResourceId(secondChainModel));
    }

    @Test
    public void testModelTransitionRevertOneMore() throws Exception {
        long old = 1234;
        long newFirst = 12345;
        long newSecond = 123456;

        // ?????????????? ???? 1 ???????????? ???? ???????????? ????????????
        gradeCreator.createModelGrade(old, id + 100);
        gradeCreator.createModelGrade(newFirst, id + 101);
        gradeCreator.createModelGrade(newSecond, id + 102);

        // ?????????????? 2 transition ?????? ???????????????? ?????????????? ???? newSecond
        List<ModelStorage.ModelTransition> allTransitions = List.of(
            generateSimpleModelTransition(1, old, newSecond),
            generateSimpleModelTransition(2, newFirst, newSecond)
        );

        modelTransitionGradeConsumer.accept(allTransitions);

        // ??????????????????, ?????? ???? ???????????? ???????????? ?????????? ???? 1 ????????????
        Assert.assertEquals(1, getContentCountByResourceId(old));
        Assert.assertEquals(1, getContentCountByResourceId(newFirst));
        Assert.assertEquals(1, getContentCountByResourceId(newSecond));

        // ?????????????????? ??????????????
        gradeModelTransitionExecutor.activate();
        gradeModelTransitionExecutor.runTmsJob();

        // ??????????, ?????? ?????? ???????????? ?????????????????? ???? ???????????? newSecond
        Assert.assertEquals(0, getContentCountByResourceId(old));
        Assert.assertEquals(0, getContentCountByResourceId(newFirst));
        Assert.assertEquals(3, getContentCountByResourceId(newSecond));

        // ???????????? ?????????? ?????? ???????? ??????????????, ?????????????? ???????????? ?? old
        modelTransitionGradeConsumer.accept(List.of(
            generateModelTransition(3,
                ModelStorage.ModelTransition.ModelType.MODEL,
                ModelStorage.ModelTransition.TransitionReason.CLUSTERIZATION,
                ModelStorage.ModelTransition.TransitionType.REVERT,
                ModelTransitionTestHelper.primaryTransition,
                old,
                old)
        ));

        gradeModelTransitionExecutor.activate();
        gradeModelTransitionExecutor.runTmsJob();

        // ??????????, ?????? ?????????????? ???????????????? ???????????? ?????? ???????????? old
        Assert.assertEquals(1, getContentCountByResourceId(old));
        Assert.assertEquals(0, getContentCountByResourceId(newFirst));
        Assert.assertEquals(2, getContentCountByResourceId(newSecond));
    }

    @Test
    public void testModelTransitionRevertChain() throws Exception {
        long sourceModel = 1234;
        long firstChainModel = 12345;
        long secondChainModel = 123456;

        // ?????????????? 3 ????????????
        gradeCreator.createModelGrade(sourceModel, id + 100);
        gradeCreator.createModelGrade(firstChainModel, id + 101);
        gradeCreator.createModelGrade(secondChainModel, id + 102);

        // ??????????, ?????? ?????? ???????????? ???? sourceModel
        Assert.assertEquals(1, getContentCountByResourceId(sourceModel));
        Assert.assertEquals(1, getContentCountByResourceId(firstChainModel));
        Assert.assertEquals(1, getContentCountByResourceId(secondChainModel));

        // ?????????????????? ??????????????
        modelTransitionGradeConsumer.accept(List.of(
            generateSimpleModelTransition(1, sourceModel, firstChainModel),
            generateSimpleModelTransition(2, firstChainModel, secondChainModel)
        ));
        gradeModelTransitionExecutor.activate();
        gradeModelTransitionExecutor.runTmsJob();

        // ??????????, ?????? ?????? ???????????? ?????????????????? ???? ???????????? secondChainModel
        Assert.assertEquals(0, getContentCountByResourceId(sourceModel));
        Assert.assertEquals(0, getContentCountByResourceId(firstChainModel));
        Assert.assertEquals(3, getContentCountByResourceId(secondChainModel));

        modelTransitionGradeConsumer.accept(List.of(
            generateSimpleModelRevertTransition(3, sourceModel, secondChainModel)
        ));
        gradeModelTransitionExecutor.runTmsJob();

        // ??????????, ?????? ???????????? ???? ??????????, ???????????? ?????? ?????? ???????????? ?? ??????????????????
        Assert.assertEquals(1, getContentCountByResourceId(sourceModel));
        Assert.assertEquals(0, getContentCountByResourceId(firstChainModel));
        Assert.assertEquals(2, getContentCountByResourceId(secondChainModel));

        modelTransitionGradeConsumer.accept(List.of(
            generateSimpleModelRevertTransition(4, firstChainModel, secondChainModel)
        ));
        gradeModelTransitionExecutor.runTmsJob();

        Assert.assertEquals(1, getContentCountByResourceId(sourceModel));
        Assert.assertEquals(1, getContentCountByResourceId(firstChainModel));
        Assert.assertEquals(1, getContentCountByResourceId(secondChainModel));
    }

    @Test
    public void testModelTransitionRevertChain2() throws Exception {
        long old = 1234;
        long new1 = 12345;
        long new2 = 123456;
        long new3 = 1234567;

        // ?????????????? 3 ????????????
        gradeCreator.createModelGrade(old, id + 100);
        gradeCreator.createModelGrade(new1, id + 101);
        gradeCreator.createModelGrade(new2, id + 102);

        Assert.assertEquals(1, getContentCountByResourceId(old));
        Assert.assertEquals(1, getContentCountByResourceId(new1));
        Assert.assertEquals(1, getContentCountByResourceId(new2));
        Assert.assertEquals(0, getContentCountByResourceId(new3));

        // ?????????????????? ??????????????
        modelTransitionGradeConsumer.accept(List.of(
            generateSimpleModelTransition(1, old, new1),
            generateSimpleModelTransition(2, new1, new2),
            generateSimpleModelTransition(3, new2, new3)
        ));
        gradeModelTransitionExecutor.activate();
        gradeModelTransitionExecutor.runTmsJob();

        // ??????????, ?????? ?????? ???????????? ?????????????????? ???? ???????????? new3
        Assert.assertEquals(0, getContentCountByResourceId(old));
        Assert.assertEquals(0, getContentCountByResourceId(new1));
        Assert.assertEquals(0, getContentCountByResourceId(new2));
        Assert.assertEquals(3, getContentCountByResourceId(new3));

        modelTransitionGradeConsumer.accept(List.of(
            generateSimpleModelRevertTransition(4, new1, new1)
        ));
        gradeModelTransitionExecutor.runTmsJob();

        Assert.assertEquals(0, getContentCountByResourceId(old));
        Assert.assertEquals(2, getContentCountByResourceId(new1));
        Assert.assertEquals(0, getContentCountByResourceId(new2));
        Assert.assertEquals(1, getContentCountByResourceId(new3));

        modelTransitionGradeConsumer.accept(List.of(
            generateSimpleModelRevertTransition(5, old, old)
        ));
        gradeModelTransitionExecutor.runTmsJob();

        Assert.assertEquals(1, getContentCountByResourceId(old));
        Assert.assertEquals(1, getContentCountByResourceId(new1));
        Assert.assertEquals(0, getContentCountByResourceId(new2));
        Assert.assertEquals(1, getContentCountByResourceId(new3));

        // ?????????????? old ?????????? revert old
        modelTransitionGradeConsumer.accept(List.of(
            generateSimpleModelTransition(6, old, new2)
        ));
        gradeModelTransitionExecutor.runTmsJob();

        Assert.assertEquals(0, getContentCountByResourceId(old));
        Assert.assertEquals(1, getContentCountByResourceId(new1));
        Assert.assertEquals(1, getContentCountByResourceId(new2));
        Assert.assertEquals(1, getContentCountByResourceId(new3));

        // ?????? ?? ?????????????? ?????????????? old -> new1, ??????????????????, ?????? ?????????? ???? ?????????????? old
        modelTransitionGradeConsumer.accept(List.of(
            generateSimpleModelRevertTransition(7, new1, new1)
        ));
        gradeModelTransitionExecutor.runTmsJob();

        Assert.assertEquals(0, getContentCountByResourceId(old));
        Assert.assertEquals(1, getContentCountByResourceId(new1));
        Assert.assertEquals(1, getContentCountByResourceId(new2));
        Assert.assertEquals(1, getContentCountByResourceId(new3));

        // ???????????????????? ?????????????? ???? old
        modelTransitionGradeConsumer.accept(List.of(
            generateSimpleModelRevertTransition(8, old, old)
        ));
        gradeModelTransitionExecutor.runTmsJob();

        Assert.assertEquals(1, getContentCountByResourceId(old));
        Assert.assertEquals(1, getContentCountByResourceId(new1));
        Assert.assertEquals(0, getContentCountByResourceId(new2));
        Assert.assertEquals(1, getContentCountByResourceId(new3));
    }

    @Test
    public void testModelTransitionRevertChain3() throws Exception {
        long old = 1234;
        long new1 = 12345;
        long new2 = 123456;
        long new3 = 1234567;

        // ?????????????? 3 ????????????
        gradeCreator.createModelGrade(old, id + 100);
        gradeCreator.createModelGrade(new1, id + 101);
        gradeCreator.createModelGrade(new2, id + 102);


        Assert.assertEquals(1, getContentCountByResourceId(old));
        Assert.assertEquals(1, getContentCountByResourceId(new1));
        Assert.assertEquals(1, getContentCountByResourceId(new2));
        Assert.assertEquals(0, getContentCountByResourceId(new3));

        // ?????????????????? ??????????????
        modelTransitionGradeConsumer.accept(List.of(
            generateSimpleModelTransition(1, old, new1),
            generateSimpleModelTransition(2, new1, new2),
            generateSimpleModelTransition(3, new2, new3)
        ));
        gradeModelTransitionExecutor.activate();
        gradeModelTransitionExecutor.runTmsJob();

        // ??????????, ?????? ?????? ???????????? ?????????????????? ???? ???????????? new3
        Assert.assertEquals(0, getContentCountByResourceId(old));
        Assert.assertEquals(0, getContentCountByResourceId(new1));
        Assert.assertEquals(0, getContentCountByResourceId(new2));
        Assert.assertEquals(3, getContentCountByResourceId(new3));

        modelTransitionGradeConsumer.accept(List.of(
            generateSimpleModelRevertTransition(4, old, old)
        ));
        gradeModelTransitionExecutor.runTmsJob();

        Assert.assertEquals(1, getContentCountByResourceId(old));
        Assert.assertEquals(0, getContentCountByResourceId(new1));
        Assert.assertEquals(0, getContentCountByResourceId(new2));
        Assert.assertEquals(2, getContentCountByResourceId(new3));

        modelTransitionGradeConsumer.accept(List.of(
            generateSimpleModelRevertTransition(5, new1, new1)
        ));
        gradeModelTransitionExecutor.runTmsJob();

        Assert.assertEquals(1, getContentCountByResourceId(old));
        Assert.assertEquals(1, getContentCountByResourceId(new1));
        Assert.assertEquals(0, getContentCountByResourceId(new2));
        Assert.assertEquals(1, getContentCountByResourceId(new3));

        // ?????????????? old ?????????? revert old
        modelTransitionGradeConsumer.accept(List.of(
            generateSimpleModelTransition(6, old, new2)
        ));
        gradeModelTransitionExecutor.runTmsJob();

        Assert.assertEquals(0, getContentCountByResourceId(old));
        Assert.assertEquals(1, getContentCountByResourceId(new1));
        Assert.assertEquals(1, getContentCountByResourceId(new2));
        Assert.assertEquals(1, getContentCountByResourceId(new3));

        // ?????? ?? ?????????????? ?????????????? old -> new1, ??????????????????, ?????? ?????????? ???? ?????????????? old
        modelTransitionGradeConsumer.accept(List.of(
            generateSimpleModelRevertTransition(7, new1, new1)
        ));
        gradeModelTransitionExecutor.runTmsJob();

        Assert.assertEquals(0, getContentCountByResourceId(old));
        Assert.assertEquals(1, getContentCountByResourceId(new1));
        Assert.assertEquals(1, getContentCountByResourceId(new2));
        Assert.assertEquals(1, getContentCountByResourceId(new3));

        // ???????????????????? ?????????????? ???? old
        modelTransitionGradeConsumer.accept(List.of(
            generateSimpleModelRevertTransition(8, old, old)
        ));
        gradeModelTransitionExecutor.runTmsJob();

        Assert.assertEquals(1, getContentCountByResourceId(old));
        Assert.assertEquals(1, getContentCountByResourceId(new1));
        Assert.assertEquals(0, getContentCountByResourceId(new2));
        Assert.assertEquals(1, getContentCountByResourceId(new3));
    }

}
