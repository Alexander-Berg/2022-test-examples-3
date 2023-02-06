package ru.yandex.market.mbo.classifier.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;
import org.apache.lucene.queryparser.classic.ParseException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.market.ir.http.Classifier;
import ru.yandex.market.mbo.classifier.dao.AssignType;
import ru.yandex.market.mbo.classifier.dao.ClassifierTaskManager;
import ru.yandex.market.mbo.classifier.dao.tree.CategoryTree;
import ru.yandex.market.mbo.classifier.model.CategoryChange;
import ru.yandex.market.mbo.classifier.model.DataPage;
import ru.yandex.market.mbo.classifier.model.Offer;
import ru.yandex.market.mbo.classifier.model.SearchConditions;
import ru.yandex.market.mbo.classifier.model.TaskType;
import ru.yandex.market.mbo.classifier.service.offers.OffersService;
import ru.yandex.market.mbo.http.MboClassifier;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyCollection;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author commince
 * <p>
 * <p>
 * Проверочные кейсы:
 * 1) Если в категории у пользователя достаточно заданий, то генерация не вызывается
 * 2) Если у пользователя недостаточно заданий, но лимит max_total_tasks еще не певышен, вызывается генерация
 * (причем так, чтобы не превысить лимит max_total_tasks)
 * 3) Если allow_task_generation == false, генерация не вызывается, даже если заданий недостаточно
 */
public class MboClassifierProtoServiceTest {
    private MboClassifierProtoService classifierProtoService = new MboClassifierProtoService();

    private OffersService tasksOffersService;
    private ClassifierTaskManager classifierTaskManager;
    private ClassificationService classificationService;
    private CategoryTree categoryTree;
    private PermissionService permissionService;

    private static final long CATEGORY_HID = 711711L;
    private static final long USER_ID = 1L;

    private static final int GET_TASKS_MAX_COUNT = 100;
    private static final int GET_TASKS_COUNT = 10;


    @Before
    public void initMock() {
        tasksOffersService = mock(OffersService.class);
        classifierTaskManager = mock(ClassifierTaskManager.class);
        classificationService = mock(ClassificationService.class);
        categoryTree = mock(CategoryTree.class);
        permissionService = mock(PermissionService.class);

        classifierProtoService.setTasksOffersService(tasksOffersService);
        classifierProtoService.setClassifierTaskManager(classifierTaskManager);
        classifierProtoService.setClassificationService(classificationService);
        classifierProtoService.setCategoryTree(categoryTree);
        classifierProtoService.setPermissionService(permissionService);

        when(permissionService.isRobot(anyLong())).thenReturn(true);
    }

    //1) Если в категории у пользователя достаточно заданий, то генерация не вызывается
    @Test
    public void testGetTasks1() throws IOException, ParseException {

        MboClassifier.GetTasksRequest request = makeGetRequest(GET_TASKS_COUNT, GET_TASKS_MAX_COUNT,
                MboClassifier.GetTasksRequest.TaskAssignType.AUTO,
                toSet(MboClassifier.ClassifierTaskType.GURU_TRASH,
                        MboClassifier.ClassifierTaskType.GURULIGHT_TRASH),
                true);

        SearchConditions searchConditions = MboClassifierProtoServiceHelper.getSearchCondition(CATEGORY_HID,
                USER_ID, GET_TASKS_COUNT, AssignType.AUTO, toSet(TaskType.GURU_TRASH, TaskType.GURULIGHT_TRASH),
                1L);

        when(tasksOffersService.getOffers(searchConditions)).thenReturn(getDataPage(5, 20));

        when(classificationService.classify(anyInt(), anyBoolean(), anyBoolean(), anyCollection()))
                .thenReturn(getClassifierProbableCategories(5));

        classifierProtoService.getTasks(request);

        verify(tasksOffersService, times(1)).getOffers(searchConditions);
        verify(classifierTaskManager, never()).refillOperatorTaskList(anyLong(), anyLong(), anyInt(), any(),
                anyBoolean());
        verify(classifierTaskManager, never()).refillOperatorTaskList(anyLong(), anyInt(), anyBoolean());

    }

    //2) Если у пользователя недостаточно заданий, но лимит max_total_tasks еще не певышен, вызывается генерация
    //   (причем так, чтобы не превысить лимит max_total_tasks)
    @Test
    public void testGetTasks2() throws IOException, ParseException {
        MboClassifier.GetTasksRequest request = makeGetRequest(GET_TASKS_COUNT, GET_TASKS_MAX_COUNT,
                MboClassifier.GetTasksRequest.TaskAssignType.AUTO,
                toSet(MboClassifier.ClassifierTaskType.GURU_TRASH, MboClassifier.ClassifierTaskType.GURULIGHT_TRASH),
                true);

        SearchConditions searchConditions = MboClassifierProtoServiceHelper.getSearchCondition(CATEGORY_HID, USER_ID,
                GET_TASKS_COUNT, AssignType.AUTO, toSet(TaskType.GURU_TRASH, TaskType.GURULIGHT_TRASH), 1L);

        SearchConditions countCondition = MboClassifierProtoServiceHelper.getCountInCategoryCondition(CATEGORY_HID,
                USER_ID, AssignType.AUTO);

        when(tasksOffersService.getOffers(searchConditions))
                .thenReturn(getDataPage(2, 2))
                .thenReturn(getDataPage(4, 4));

        when(classificationService.classify(anyInt(), anyBoolean(), anyBoolean(), anyCollection()))
                .thenReturn(getClassifierProbableCategories(4));

        when(tasksOffersService.getOffers(countCondition)).thenReturn(getDataPage(5, 98));

        classifierProtoService.getTasks(request);

        //Должен отправиться запрос на генерацию 2х заданий
        verify(classifierTaskManager, times(1)).refillOperatorTaskList(eq(USER_ID), eq(CATEGORY_HID),
                eq(2), eq(toSet(TaskType.GURU_TRASH, TaskType.GURULIGHT_TRASH)), eq(true));
    }

    //3) Если allow_task_generation == false, генерация не вызывается, даже если заданий недостаточно
    @Test
    public void testGetTasks3() throws IOException, ParseException {
        MboClassifier.GetTasksRequest request = makeGetRequest(GET_TASKS_COUNT, GET_TASKS_MAX_COUNT,
                MboClassifier.GetTasksRequest.TaskAssignType.AUTO,
                toSet(MboClassifier.ClassifierTaskType.GURU_TRASH, MboClassifier.ClassifierTaskType.GURULIGHT_TRASH),
                false);

        SearchConditions searchConditions = MboClassifierProtoServiceHelper.getSearchCondition(CATEGORY_HID, USER_ID,
                GET_TASKS_COUNT, AssignType.AUTO, toSet(TaskType.GURU_TRASH, TaskType.GURULIGHT_TRASH), 1L);

        SearchConditions countCondition = MboClassifierProtoServiceHelper.getCountInCategoryCondition(CATEGORY_HID,
                USER_ID, AssignType.AUTO);

        when(tasksOffersService.getOffers(searchConditions))
                .thenReturn(getDataPage(2, 2));

        when(classificationService.classify(anyInt(), anyBoolean(), anyBoolean(), anyCollection()))
                .thenReturn(getClassifierProbableCategories(2));

        when(tasksOffersService.getOffers(countCondition)).thenReturn(getDataPage(5, 98));

        classifierProtoService.getTasks(request);

        verify(classifierTaskManager, never()).refillOperatorTaskList(anyLong(), anyLong(), anyInt(), any(),
                anyBoolean());
        verify(classifierTaskManager, never()).refillOperatorTaskList(anyLong(), anyInt(), anyBoolean());
    }

    @Test
    public void testSubmitTasks() throws IOException {
        // Создаем запрос, 3 таски, 1 из которых - с не определенной категорией
        MboClassifier.SubmitCategoryChangesRequest request = makeSubmitRequest();

        MboClassifier.SubmitCategoryChangesResponse response = classifierProtoService.submitCategoryChanges(request);

        // 1 раз вызываются markTaskAsOnlyManual и removeTask
        // т.к. одна из задач пришла с результатом "категория не определена"
        verify(classifierTaskManager, times(1)).markTaskAsOnlyManual(anyString(), anyString());
        verify(tasksOffersService, times(1)).removeTask(anyString());

        ArgumentCaptor<List<CategoryChange>> peopleCaptor = ArgumentCaptor.forClass(List.class);
        verify(tasksOffersService, times(1)).modifyOffers(peopleCaptor.capture());

        // В modifyOffers должны уйти ровно 2 перекладывания
        Assert.assertEquals(2, peopleCaptor.getValue().size());

        // В результатах со статусом CATEGORY_RESOLVED должно быть 2 задачи
        Assert.assertEquals(2, response.getStatusesList().stream()
                .filter(o -> o.getStatus().equals(MboClassifier.Status.SUBMITED_CATEGORY_RESOLVED))
                .count());
        Assert.assertEquals(1, response.getStatusesList().stream()
                .filter(o -> o.getStatus().equals(MboClassifier.Status.SUBMITED_CATEGORY_UNRESOLVED))
                .count());
    }

    private <T> Set<T> toSet(T... elements) {
        return new HashSet<>(Arrays.asList(elements));
    }

    private DataPage<Offer> getDataPage(int count, int total) {
        DataPage<Offer> result = new DataPage<Offer>(generateOfferList(count), total, 0);
        return result;
    }

    private List<Offer> generateOfferList(int count) {
        List<Offer> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(new Offer("offer_id_" + i, "good_id_" + i));
        }

        return result;
    }

    private MboClassifier.GetTasksRequest makeGetRequest(int maxTaskCount, int maxTotalCount,
                                                         MboClassifier.GetTasksRequest.TaskAssignType assignType,
                                                         Set<MboClassifier.ClassifierTaskType> taskTypes,
                                                         boolean allowGeneration) {
        MboClassifier.GetTasksRequest.Builder result = MboClassifier.GetTasksRequest.newBuilder();
        result.setUserId(USER_ID);
        result.setCategoryId(CATEGORY_HID);
        result.setMaxTaskCount(maxTaskCount);
        result.setMaxTotalTasks(maxTotalCount);
        result.setStartClassifierTaskId(1);
        result.setAssignType(assignType);
        result.addAllTaskType(taskTypes);
        result.setAllowTaskGeneration(allowGeneration);

        return result.build();
    }

    private MboClassifier.SubmitCategoryChangesRequest makeSubmitRequest() {
        MboClassifier.SubmitCategoryChangesRequest.Builder result =
                MboClassifier.SubmitCategoryChangesRequest.newBuilder();
        result.setUserId(USER_ID);

        MboClassifier.OfferCategoryChangeData.Builder offerData1 =
                MboClassifier.OfferCategoryChangeData.newBuilder();
        offerData1.setCategoryId(1L);
        offerData1.setMagicId("offer_id_1");
        offerData1.setGoodId("good_id_1");
        result.addOffers(offerData1);

        MboClassifier.OfferCategoryChangeData.Builder offerData2 =
                MboClassifier.OfferCategoryChangeData.newBuilder();
        offerData2.setCategoryId(2L);
        offerData2.setMagicId("offer_id_1");
        offerData2.setGoodId("good_id_1");
        result.addOffers(offerData2);

        MboClassifier.OfferCategoryChangeData.Builder offerData3 = MboClassifier.OfferCategoryChangeData.newBuilder();
        offerData3.setMagicId("offer_id_1");
        offerData3.setGoodId("good_id_1");
        offerData3.setUnknown(true);
        result.addOffers(offerData3);


        return result.build();
    }

    private List<Classifier.ClassifiedOffer> getClassifierProbableCategories(int count) {
        List<Classifier.ClassifiedOffer> result = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            result.add(Classifier.ClassifiedOffer.newBuilder().build());
        }

        return result;
    }
}
