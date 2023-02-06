package ru.yandex.market.pers.qa.tms.transition;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;

import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.pers.qa.PersQaTmsTest;
import ru.yandex.market.pers.qa.model.ModState;
import ru.yandex.market.pers.qa.model.QaEntityType;
import ru.yandex.market.pers.qa.model.Question;
import ru.yandex.market.pers.qa.service.QuestionProductService;
import ru.yandex.market.pers.qa.service.QuestionService;
import ru.yandex.market.pers.qa.service.saas.SaasIndexingService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static ru.yandex.market.pers.qa.tms.transition.ModelTransitionTestHelper.generateSimpleModelRevertTransition;
import static ru.yandex.market.pers.qa.tms.transition.ModelTransitionTestHelper.generateSimpleModelTransition;
import static ru.yandex.market.pers.qa.tms.transition.ModelTransitionTestHelper.newEntityId;
import static ru.yandex.market.pers.qa.tms.transition.ModelTransitionTestHelper.oldEntityId;

class QaModelTransitionExecutorTest extends PersQaTmsTest {

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

    private static final String QUESTION_TEXT = "some question text";
    private static final String ANOTHER_QUESTION_TEXT = "another question text";
    private static final String YET_ANOTHER_QUESTION_TEXT = "yet another question text";

    private static final long USER_ID = 12345;

    @Autowired
    QaModelTransitionExecutor qaModelTransitionExecutor;
    @Autowired
    @Qualifier("pgJdbcTemplate")
    JdbcTemplate jdbcTemplate;
    @Autowired
    QuestionService questionService;
    @Autowired
    QuestionProductService questionProductService;
    @Autowired
    SaasIndexingService saasIndexingService;

    @BeforeEach
    public void resetConfiguration() {
        qaModelTransitionExecutor.updateLastProcessedTransition(0);
    }


    public void storeModelTransition(List<ModelStorage.ModelTransition> modelTransitions) {
        jdbcTemplate.batchUpdate(
            "insert into model_transition(id, action_id, cr_time, type, reason, entity_type, old_entity_id, old_entity_id_deleted, new_entity_id, primary_transition) " +
                "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING",
            modelTransitions, modelTransitions.size(), SAVE_PS_SETTER);
    }

    @Test
    public void testEmptyApplication() throws Exception {
        qaModelTransitionExecutor.activate();
        qaModelTransitionExecutor.runExecutor();
        assertEquals(0, qaModelTransitionExecutor.lastAppliedModelTransitionId());
    }

    private String getQuestionEntitySrcById(long id) {
        return jdbcTemplate.queryForObject("select entity_src from qa.question where id = ?", String.class, id);
    }

    private boolean ifLockExist(String lock) {
        return jdbcTemplate.queryForObject("select count(*) from qa.question_lock where id = ?", Long.class, lock) == 1;
    }

    private void testApplicationSkeleton(ModelStorage.ModelTransition.ModelType modelType, ModelStorage.ModelTransition.TransitionType transitionType, boolean primaryTransition) throws Exception {
        ModelStorage.ModelTransition modelTransition = ModelTransitionTestHelper.generateModelTransition(1,
            modelType,
            ModelStorage.ModelTransition.TransitionReason.CLUSTERIZATION,
            transitionType,
            primaryTransition);
        storeModelTransition(Collections.singletonList(modelTransition));

        Question question = questionService.createModelQuestion(12345, QUESTION_TEXT, modelTransition.getOldEntityId());
        String oldLock = questionService.generateLock(question);
        jdbcTemplate.update("update qa.question set entity_src = 12345 where id = ?", question.getId());
        Assertions.assertNotNull(getQuestionEntitySrcById(question.getId()));

        long contentCount = getContentCountByResourceId(modelTransition.getOldEntityId());
        assertEquals(1, contentCount);
        contentCount = getContentCountByResourceId(modelTransition.getNewEntityId());
        assertEquals(0, contentCount);

        qaModelTransitionExecutor.activate();
        qaModelTransitionExecutor.runExecutor();

        question.setEntityId(modelTransition.getNewEntityId());
        String newLock = questionService.generateLock(question);

        if (primaryTransition) {
            contentCount = getContentCountByResourceId(modelTransition.getOldEntityId());
            assertEquals(0, contentCount);
            contentCount = getContentCountByResourceId(modelTransition.getNewEntityId());
            assertEquals(1, contentCount);
            Assertions.assertNull(getQuestionEntitySrcById(question.getId()));

            Assertions.assertTrue(ifLockExist(newLock));
            Assertions.assertFalse(ifLockExist(oldLock));

            assertEquals(0, getWeakTransitionCount());

            assertEquals(1, getLogEntryCount(modelTransition, question.getId()));
        } else {
            contentCount = getContentCountByResourceId(modelTransition.getOldEntityId());
            assertEquals(1, contentCount);
            contentCount = getContentCountByResourceId(modelTransition.getNewEntityId());
            assertEquals(0, contentCount);

            Assertions.assertFalse(ifLockExist(newLock));
            Assertions.assertTrue(ifLockExist(oldLock));

            assertEquals(1, getWeakTransitionCount());

            assertEquals(0, getLogEntryCount(modelTransition, question.getId()));
        }
    }

    private Integer getLogEntryCount(ModelStorage.ModelTransition modelTransition, long questionId) {
        String sql = "SELECT count(*) FROM QA.MODEL_TRANSITION_HISTORY " +
            "WHERE transition = ? AND old_model = ? AND new_model = ? AND question_id = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, modelTransition.getId(),
            modelTransition.getOldEntityId(), modelTransition.getNewEntityId(), questionId);
    }

    @Test
    public void testClusterSplitApplicationPrimary() throws Exception {
        testApplicationSkeleton(ModelStorage.ModelTransition.ModelType.CLUSTER, ModelStorage.ModelTransition.TransitionType.SPLIT, true);
    }

    @Test
    public void testClusterDuplicateApplicationPrimary() throws Exception {
        testApplicationSkeleton(ModelStorage.ModelTransition.ModelType.CLUSTER, ModelStorage.ModelTransition.TransitionType.DUPLICATE, true);
    }

    @Test
    public void testModelSplitApplicationPrimary() throws Exception {
        testApplicationSkeleton(ModelStorage.ModelTransition.ModelType.MODEL, ModelStorage.ModelTransition.TransitionType.SPLIT, true);
    }

    @Test
    public void testModelDuplicateApplicationPrimary() throws Exception {
        testApplicationSkeleton(ModelStorage.ModelTransition.ModelType.MODEL, ModelStorage.ModelTransition.TransitionType.DUPLICATE, true);
    }

    @Test
    public void testClusterSplitApplicationNonPrimary() throws Exception {
        testApplicationSkeleton(ModelStorage.ModelTransition.ModelType.CLUSTER, ModelStorage.ModelTransition.TransitionType.SPLIT, false);
    }

    @Test
    public void testModelSplitApplicationNonPrimary() throws Exception {
        testApplicationSkeleton(ModelStorage.ModelTransition.ModelType.MODEL, ModelStorage.ModelTransition.TransitionType.SPLIT, false);
    }

    private List<ModelStorage.ModelTransition> generateAllNotUsefulTransitions() {
        List<ModelStorage.ModelTransition> modelTransitions = new ArrayList<>();
        int id = 1;
        for (ModelStorage.ModelTransition.TransitionType transitionType : ModelStorage.ModelTransition.TransitionType.values()) {
            for (ModelStorage.ModelTransition.ModelType modelType : ModelStorage.ModelTransition.ModelType.values()) {
                modelTransitions.add(ModelTransitionTestHelper.generateModelTransition(id++, modelType, ModelTransitionTestHelper.transitionReason, transitionType, false));
                modelTransitions.add(ModelTransitionTestHelper.generateModelTransition(id++, modelType, ModelTransitionTestHelper.transitionReason, transitionType, true));
            }
        }
        return modelTransitions.stream()
            .filter(modelTransition -> modelTransition.getType() != ModelStorage.ModelTransition.TransitionType.DUPLICATE)
            .filter(modelTransition -> modelTransition.getType() != ModelStorage.ModelTransition.TransitionType.SPLIT)
            .collect(Collectors.toList());
    }

    private long getWeakTransitionCount() {
        return jdbcTemplate.queryForObject("select count(*) from weak_transition_with_content", Long.class);
    }

    private long getContentCountByResourceId(long resourceId) {
        return jdbcTemplate.queryForObject("select count(*) from qa.question where entity_type = 0 and entity_id = ?", Long.class, String.valueOf(resourceId));
    }

    @Test
    public void testNotUsefulApplication() throws Exception {
        Question question = questionService.createModelQuestion(12345, QUESTION_TEXT, oldEntityId);

        assertEquals(0, getWeakTransitionCount());

        assertEquals(1, getContentCountByResourceId(oldEntityId));
        assertEquals(0, getContentCountByResourceId(ModelTransitionTestHelper.newEntityId));

        List<ModelStorage.ModelTransition> modelTransitions = generateAllNotUsefulTransitions();
        storeModelTransition(modelTransitions);

        saasIndexingService.cleanQueue();
        assertFalse(hasQuestionsInIndexQueue());

        qaModelTransitionExecutor.activate();
        qaModelTransitionExecutor.runExecutor();

        assertEquals(0, getWeakTransitionCount());

        assertEquals(1, getContentCountByResourceId(oldEntityId));
        assertEquals(0, getContentCountByResourceId(ModelTransitionTestHelper.newEntityId));

        Optional<Long> max = modelTransitions.stream().map(ModelStorage.ModelTransition::getId).max(Long::compareTo);
        assertEquals(max.get(), Long.valueOf(qaModelTransitionExecutor.lastAppliedModelTransitionId()));
        assertQuestionInIndexingQueue(Collections.emptyList());
    }

    /**
     * Создаем вопросы от пользователя на старый и новый idшники с одинаковым текстом -> переезд не применяется
     */
    @Test
    public void testComplexQuestionTransitionWithExistingQuestion() throws Exception {
        ModelStorage.ModelTransition modelTransition = ModelTransitionTestHelper.generateModelTransition(1,
            ModelStorage.ModelTransition.ModelType.MODEL,
            ModelStorage.ModelTransition.TransitionReason.CLUSTERIZATION,
            ModelStorage.ModelTransition.TransitionType.SPLIT,
            true);
        storeModelTransition(Collections.singletonList(modelTransition));

        Question oldQuestion = questionService.createModelQuestion(12345, QUESTION_TEXT, modelTransition.getOldEntityId());
        Question newQuestion = questionService.createModelQuestion(12345, QUESTION_TEXT, modelTransition.getNewEntityId());

        assertEquals(1, getContentCountByResourceId(oldEntityId));
        assertEquals(1, getContentCountByResourceId(ModelTransitionTestHelper.newEntityId));

        Assertions.assertTrue(ifLockExist(questionService.generateLock(oldQuestion)));
        Assertions.assertTrue(ifLockExist(questionService.generateLock(newQuestion)));

        saasIndexingService.cleanQueue();
        assertFalse(hasQuestionsInIndexQueue());

        qaModelTransitionExecutor.activate();
        qaModelTransitionExecutor.runExecutor();

        assertEquals(1, getContentCountByResourceId(oldEntityId));
        assertEquals(1, getContentCountByResourceId(ModelTransitionTestHelper.newEntityId));

        Assertions.assertTrue(ifLockExist(questionService.generateLock(oldQuestion)));
        Assertions.assertTrue(ifLockExist(questionService.generateLock(newQuestion)));
        assertQuestionInIndexingQueue(Collections.emptyList());
    }

    /**
     * Создаем вопросы от пользователя на старый и новый idшники с разным текстом -> переезд применяется
     */
    @Test
    public void testComplexQuestionTransition() throws Exception {
        ModelStorage.ModelTransition modelTransition = ModelTransitionTestHelper.generateModelTransition(1,
            ModelStorage.ModelTransition.ModelType.MODEL,
            ModelStorage.ModelTransition.TransitionReason.CLUSTERIZATION,
            ModelStorage.ModelTransition.TransitionType.SPLIT,
            true);
        storeModelTransition(Collections.singletonList(modelTransition));

        Question newQuestion = questionService.createModelQuestion(12345, QUESTION_TEXT, ModelTransitionTestHelper.newEntityId);
        Question oldQuestion = questionService.createModelQuestion(12345, ANOTHER_QUESTION_TEXT, oldEntityId);

        assertEquals(1, getContentCountByResourceId(oldEntityId));
        assertEquals(1, getContentCountByResourceId(ModelTransitionTestHelper.newEntityId));

        Assertions.assertTrue(ifLockExist(questionService.generateLock(oldQuestion)));
        Assertions.assertTrue(ifLockExist(questionService.generateLock(newQuestion)));

        saasIndexingService.cleanQueue();
        assertFalse(hasQuestionsInIndexQueue());

        qaModelTransitionExecutor.activate();
        qaModelTransitionExecutor.runExecutor();

        assertEquals(0, getContentCountByResourceId(oldEntityId));
        assertEquals(2, getContentCountByResourceId(ModelTransitionTestHelper.newEntityId));

        Assertions.assertFalse(ifLockExist(questionService.generateLock(oldQuestion)));
        Assertions.assertTrue(ifLockExist(questionService.generateLock(newQuestion)));

        oldQuestion.setEntityId(ModelTransitionTestHelper.newEntityId);
        Assertions.assertTrue(ifLockExist(questionService.generateLock(oldQuestion)));
        assertQuestionInIndexingQueue(Collections.singletonList(oldQuestion.getId()));
    }


    @Test
    public void testTransitionForNonPublicContent() throws Exception {
        ModelStorage.ModelTransition modelTransition = ModelTransitionTestHelper.generateModelTransition(1,
            ModelStorage.ModelTransition.ModelType.MODEL,
            ModelStorage.ModelTransition.TransitionReason.CLUSTERIZATION,
            ModelStorage.ModelTransition.TransitionType.SPLIT,
            true);
        storeModelTransition(Collections.singletonList(modelTransition));

        Question deletedQuestion = questionService.createModelQuestion(USER_ID, QUESTION_TEXT, oldEntityId);
        questionService.deleteQuestion(deletedQuestion.getId(), USER_ID);
        Question autoRejectedQuestion = questionService.createModelQuestion(USER_ID, ANOTHER_QUESTION_TEXT, oldEntityId);
        Question tolokaRejectedQuestion = questionService.createModelQuestion(USER_ID, YET_ANOTHER_QUESTION_TEXT, oldEntityId);
        List<Pair<Long, ModState>> results = new ArrayList<>();
        results.add(Pair.of(tolokaRejectedQuestion.getId(), ModState.TOLOKA_REJECTED));
        results.add(Pair.of(autoRejectedQuestion.getId(), ModState.AUTO_FILTER_REJECTED));
        questionService.updateModStates(results);

        assertEquals(3, getContentCountByResourceId(oldEntityId));
        assertEquals(0, getContentCountByResourceId(ModelTransitionTestHelper.newEntityId));

        saasIndexingService.cleanQueue();
        assertFalse(hasQuestionsInIndexQueue());

        qaModelTransitionExecutor.activate();
        qaModelTransitionExecutor.runExecutor();

        assertEquals(0, getContentCountByResourceId(oldEntityId));
        assertEquals(3, getContentCountByResourceId(ModelTransitionTestHelper.newEntityId));
        assertQuestionInIndexingQueue(
                List.of(deletedQuestion.getId(), autoRejectedQuestion.getId(), tolokaRejectedQuestion.getId()));
    }

    @Test
    public void testQuestionProductIdsTransition() throws Exception {
        ModelStorage.ModelTransition modelTransition = ModelTransitionTestHelper.generateModelTransition(1,
                ModelStorage.ModelTransition.ModelType.MODEL,
                ModelStorage.ModelTransition.TransitionReason.CLUSTERIZATION,
                ModelStorage.ModelTransition.TransitionType.SPLIT,
                true);
        storeModelTransition(Collections.singletonList(modelTransition));

        Question question = questionService.createModelQuestion(12345, QUESTION_TEXT, oldEntityId);
        questionProductService
                .saveProductsForQuestion(question.getId(), List.of(oldEntityId + 1, oldEntityId, oldEntityId + 3));

        saasIndexingService.cleanQueue();
        assertFalse(hasQuestionsInIndexQueue());

        qaModelTransitionExecutor.activate();
        qaModelTransitionExecutor.runExecutor();

        assertEquals(0, getContentCountByResourceId(oldEntityId));
        assertEquals(1, getContentCountByResourceId(ModelTransitionTestHelper.newEntityId));
        assertEquals(List.of(oldEntityId + 1, newEntityId, oldEntityId + 3), questionProductService
                .getProductIds(Collections.singletonList(question.getId())).get(question.getId()));
        assertQuestionInIndexingQueue(Collections.singletonList(question.getId()));
    }

    @Test
    public void testQuestionProductIdsWithUniqueIdsTransition() throws Exception {
        ModelStorage.ModelTransition modelTransition = ModelTransitionTestHelper.generateModelTransition(1,
                ModelStorage.ModelTransition.ModelType.MODEL,
                ModelStorage.ModelTransition.TransitionReason.CLUSTERIZATION,
                ModelStorage.ModelTransition.TransitionType.SPLIT,
                true);
        storeModelTransition(Collections.singletonList(modelTransition));

        Question question = questionService.createModelQuestion(12345, QUESTION_TEXT, oldEntityId);
        questionProductService
                .saveProductsForQuestion(question.getId(), List.of(newEntityId, oldEntityId, oldEntityId + 3));

        saasIndexingService.cleanQueue();
        assertFalse(hasQuestionsInIndexQueue());

        qaModelTransitionExecutor.activate();
        qaModelTransitionExecutor.runExecutor();

        assertEquals(0, getContentCountByResourceId(oldEntityId));
        assertEquals(1, getContentCountByResourceId(ModelTransitionTestHelper.newEntityId));
        assertEquals(List.of(newEntityId, oldEntityId + 3), questionProductService
                .getProductIds(Collections.singletonList(question.getId())).get(question.getId()));
        assertQuestionInIndexingQueue(Collections.singletonList(question.getId()));
    }

    @Test
    public void testRevertQuestionTransitionSimple() throws Exception {
        long firstModel = 12;
        long secondModel = 123;
        long thirdModel = 1234;

        // create questions on each model
        List<Question> modelQuestions = List.of(
            questionService.createModelQuestion(12345, QUESTION_TEXT, firstModel),
            questionService.createModelQuestion(123456, ANOTHER_QUESTION_TEXT, secondModel),
            questionService.createModelQuestion(1234567, QUESTION_TEXT
                + ANOTHER_QUESTION_TEXT, thirdModel)
        );

        // create direct transitions 1 -> 2 -> 3
        List<ModelStorage.ModelTransition> modelTransitions = List.of(
            generateSimpleModelTransition(1, firstModel, secondModel),
            generateSimpleModelTransition(2, secondModel, thirdModel)
        );

        storeModelTransition(modelTransitions);

        qaModelTransitionExecutor.activate();
        qaModelTransitionExecutor.runExecutor();

        assertEquals(0, getContentCountByResourceId(firstModel));
        assertEquals(0, getContentCountByResourceId(secondModel));
        assertEquals(3, getContentCountByResourceId(thirdModel));

        // create revert transition to firstModel
        storeModelTransition(List.of(
            generateSimpleModelRevertTransition(3, firstModel, thirdModel)
        ));
        qaModelTransitionExecutor.runExecutor();

        assertEquals(1, getContentCountByResourceId(firstModel));
        assertEquals(0, getContentCountByResourceId(secondModel));
        assertEquals(2, getContentCountByResourceId(thirdModel));

        // create revert transition to secondModel
        storeModelTransition(List.of(
            generateSimpleModelRevertTransition(4, secondModel, thirdModel)
        ));
        qaModelTransitionExecutor.runExecutor();

        assertEquals(1, getContentCountByResourceId(firstModel));
        assertEquals(1, getContentCountByResourceId(secondModel));
        assertEquals(1, getContentCountByResourceId(thirdModel));
    }

    @Test
    public void testRevertTransitionWithNonExistingDirectTransition() throws Exception {
        long oldModel = 12;
        long newModel = 123;

        // create questions on each model
        questionService.createModelQuestion(12345, QUESTION_TEXT, oldModel);
        questionService.createModelQuestion(123456, ANOTHER_QUESTION_TEXT, newModel);

        assertEquals(1, getContentCountByResourceId(oldModel));
        assertEquals(1, getContentCountByResourceId(newModel));

        // create revert transition to oldModel without direct transition to this model
        storeModelTransition(List.of(
            generateSimpleModelRevertTransition(1, oldModel, newModel))
        );
        qaModelTransitionExecutor.runExecutor();

        assertEquals(1, getContentCountByResourceId(oldModel));
        assertEquals(1, getContentCountByResourceId(newModel));
    }

    private boolean hasQuestionsInIndexQueue() {
        return saasIndexingService.hasItemsToIndex(Collections.singletonList(QaEntityType.QUESTION));
    }

    private void assertQuestionInIndexingQueue(List<Long> questionIds) {
        List<Long> expected = new ArrayList<>(questionIds);
        List<Long> actual = jdbcTemplate.queryForList(
            "select entity_id \n" +
                "from qa.saas_indexing_queue\n" +
                "where entity_type = ?",
            Long.class,
            QaEntityType.QUESTION.getValue());

        Collections.sort(expected);
        Collections.sort(actual);
        assertEquals(expected, actual);
    }

    @Test
    public void testRevertQuestionTransitionWithLoops() throws Exception {
        long old = 1234;
        long new1 = 12345;
        long new2 = 123456;
        long new3 = 1234567;

        // create questions on each model
        List<Question> modelQuestions = List.of(
            questionService.createModelQuestion(1, QUESTION_TEXT, old),
            questionService.createModelQuestion(1, ANOTHER_QUESTION_TEXT, new1),
            questionService.createModelQuestion(3, QUESTION_TEXT
                + ANOTHER_QUESTION_TEXT, new2)
        );

        assertEquals(1, getContentCountByResourceId(old));
        assertEquals(1, getContentCountByResourceId(new1));
        assertEquals(1, getContentCountByResourceId(new2));
        assertEquals(0, getContentCountByResourceId(new3));


        storeModelTransition(List.of(
            generateSimpleModelTransition(1, old, new1),
            generateSimpleModelTransition(2, new1, new2),
            generateSimpleModelTransition(3, new2, new3)
        ));

        qaModelTransitionExecutor.activate();
        qaModelTransitionExecutor.runExecutor();

        // видим, что все отзывы переехали на модель new3
        assertEquals(0, getContentCountByResourceId(old));
        assertEquals(0, getContentCountByResourceId(new1));
        assertEquals(0, getContentCountByResourceId(new2));
        assertEquals(3, getContentCountByResourceId(new3));

        storeModelTransition(List.of(
            generateSimpleModelRevertTransition(4, old, old)
        ));
        qaModelTransitionExecutor.runExecutor();

        assertEquals(1, getContentCountByResourceId(old));
        assertEquals(0, getContentCountByResourceId(new1));
        assertEquals(0, getContentCountByResourceId(new2));
        assertEquals(2, getContentCountByResourceId(new3));

        storeModelTransition(List.of(
            generateSimpleModelRevertTransition(5, new1, new1)
        ));
        qaModelTransitionExecutor.runExecutor();

        assertEquals(1, getContentCountByResourceId(old));
        assertEquals(1, getContentCountByResourceId(new1));
        assertEquals(0, getContentCountByResourceId(new2));
        assertEquals(1, getContentCountByResourceId(new3));

        // переезд old после revert old
        storeModelTransition(List.of(
            generateSimpleModelTransition(6, old, new2)
        ));
        qaModelTransitionExecutor.runExecutor();

        assertEquals(0, getContentCountByResourceId(old));
        assertEquals(1, getContentCountByResourceId(new1));
        assertEquals(1, getContentCountByResourceId(new2));
        assertEquals(1, getContentCountByResourceId(new3));

        // был в прошлом переезд old -> new1, проверяем, что откат не заденет old
        storeModelTransition(List.of(
            generateSimpleModelRevertTransition(7, new1, new1)
        ));
        qaModelTransitionExecutor.runExecutor();

        assertEquals(0, getContentCountByResourceId(old));
        assertEquals(1, getContentCountByResourceId(new1));
        assertEquals(1, getContentCountByResourceId(new2));
        assertEquals(1, getContentCountByResourceId(new3));

        // откатываем переезд на old
        storeModelTransition(List.of(
            generateSimpleModelRevertTransition(8, old, old)
        ));
        qaModelTransitionExecutor.runExecutor();

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

        // create questions on each model
        List<Question> modelQuestions = List.of(
            questionService.createModelQuestion(1, QUESTION_TEXT, old),
            questionService.createModelQuestion(1, ANOTHER_QUESTION_TEXT, new1),
            questionService.createModelQuestion(3, QUESTION_TEXT
                + ANOTHER_QUESTION_TEXT, new2)
        );

        assertEquals(1, getContentCountByResourceId(old));
        assertEquals(1, getContentCountByResourceId(new1));
        assertEquals(1, getContentCountByResourceId(new2));
        assertEquals(0, getContentCountByResourceId(new3));


        storeModelTransition(List.of(
            generateSimpleModelTransition(1, old, new1),
            generateSimpleModelTransition(2, new1, new2),
            generateSimpleModelTransition(3, new2, new3)
        ));

        qaModelTransitionExecutor.activate();
        qaModelTransitionExecutor.runExecutor();

        // видим, что все отзывы переехали на модель new3
        assertEquals(0, getContentCountByResourceId(old));
        assertEquals(0, getContentCountByResourceId(new1));
        assertEquals(0, getContentCountByResourceId(new2));
        assertEquals(3, getContentCountByResourceId(new3));

        storeModelTransition(List.of(
            generateSimpleModelRevertTransition(4, new1, new1)
        ));
        qaModelTransitionExecutor.runExecutor();

        assertEquals(0, getContentCountByResourceId(old));
        assertEquals(2, getContentCountByResourceId(new1));
        assertEquals(0, getContentCountByResourceId(new2));
        assertEquals(1, getContentCountByResourceId(new3));

        storeModelTransition(List.of(
            generateSimpleModelRevertTransition(5, old, old)
        ));
        qaModelTransitionExecutor.runExecutor();

        assertEquals(1, getContentCountByResourceId(old));
        assertEquals(1, getContentCountByResourceId(new1));
        assertEquals(0, getContentCountByResourceId(new2));
        assertEquals(1, getContentCountByResourceId(new3));

        // переезд old после revert old
        storeModelTransition(List.of(
            generateSimpleModelTransition(6, old, new2)
        ));
        qaModelTransitionExecutor.runExecutor();

        assertEquals(0, getContentCountByResourceId(old));
        assertEquals(1, getContentCountByResourceId(new1));
        assertEquals(1, getContentCountByResourceId(new2));
        assertEquals(1, getContentCountByResourceId(new3));

        // был в прошлом переезд old -> new1, проверяем, что откат не заденет old
        storeModelTransition(List.of(
            generateSimpleModelRevertTransition(7, new1, new1)
        ));
        qaModelTransitionExecutor.runExecutor();

        assertEquals(0, getContentCountByResourceId(old));
        assertEquals(1, getContentCountByResourceId(new1));
        assertEquals(1, getContentCountByResourceId(new2));
        assertEquals(1, getContentCountByResourceId(new3));

        // откатываем переезд на old
        storeModelTransition(List.of(
            generateSimpleModelRevertTransition(8, old, old)
        ));
        qaModelTransitionExecutor.runExecutor();

        assertEquals(1, getContentCountByResourceId(old));
        assertEquals(1, getContentCountByResourceId(new1));
        assertEquals(0, getContentCountByResourceId(new2));
        assertEquals(1, getContentCountByResourceId(new3));
    }
}
