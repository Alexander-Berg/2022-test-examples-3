package ru.yandex.market.mbo.reactui.controller;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.ir.http.Markup;
import ru.yandex.market.ir.http.MarkupService;
import ru.yandex.market.mbo.catalogue.category.CategoryManagersManager;
import ru.yandex.market.mbo.catalogue.category.CategoryManagersManagerMock;
import ru.yandex.market.mbo.reactui.dto.FrozenOffersGroup;
import ru.yandex.market.mbo.reactui.dto.YangTaskMarkupState;
import ru.yandex.market.mbo.reactui.dto.YangTasksInfo;
import ru.yandex.market.mbo.reactui.dto.YangTasksMarkupStateByCategory;
import ru.yandex.market.mbo.reactui.dto.YangTasksMarkupStateRequest;
import ru.yandex.market.mbo.reactui.dto.YangTasksRequest;
import ru.yandex.market.mbo.statistic.model.TaskType;
import ru.yandex.market.mbo.user.MboUser;
import ru.yandex.market.mboc.http.MboCategory;
import ru.yandex.market.mboc.http.MboCategoryService;
import ru.yandex.market.mboc.http.SupplierOffer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("checkstyle:MagicNumber")
public class YangTasksControllerTest {
    private static final Long MANAGER_UID = 123456L;

    private MboCategoryService mboCategoryService;
    private CategoryManagersManagerMock categoryManagersManager;
    private MarkupService markupService;

    private YangTasksController controller;

    @Before
    public void setUp() throws Exception {
        categoryManagersManager = new CategoryManagersManagerMock();
        mboCategoryService = Mockito.mock(MboCategoryService.class);
        markupService = Mockito.mock(MarkupService.class);

        MboUser mboUser = new MboUser("login", MANAGER_UID, "full name", "email", "staff login");
        CategoryManagersManager.CategoryManagers categoryManagers =
            new CategoryManagersManager.CategoryManagers(237418, 123, null, mboUser);
        categoryManagersManager.addCategoryManagers(categoryManagers);

        MboCategory.GetTicketStatusesResponse.Builder builder = MboCategory.GetTicketStatusesResponse.newBuilder()
            .addTicketStatus(
                MboCategory.GetTicketStatusesResponse.TicketStatus.newBuilder()
                    .setSupplierId(1)
                    .setSupplierName("Supplier 1")
                    .setSupplierType(SupplierOffer.SupplierType.TYPE_FIRST_PARTY)
                    .setDeadlineDate(18411)
                    .setBaseStatus(SupplierOffer.Offer.InternalProcessingStatus.IN_PROCESS)
                    .setIdentifier("MCP-123")
                    .setProcessingTicketId(123)
                    .setTicketCritical(true)
                    .addCategories(
                        MboCategory.GetTicketStatusesResponse.CategoryInfo.newBuilder()
                            .setActiveOffers(4)
                            .setTotalOffers(5)
                            .setCategoryId(237418)
                    )
                    .addCategories(
                        MboCategory.GetTicketStatusesResponse.CategoryInfo.newBuilder()
                            .setActiveOffers(0)
                            .setTotalOffers(1)
                            .setCategoryId(1569931)
                    )
                    .addStuckOffer(
                        MboCategory.GetTicketStatusesResponse.TicketStatus.StuckOfferInfo.newBuilder()
                            .setCategoryId(237418)
                            .setOfferId(1)
                            .setShopSku("Shop sku")
                            .setOfferTitle("Offer title")
                            .setStuckErrorMessage("Super error")
                    )
            )
            .addTicketStatus(
                MboCategory.GetTicketStatusesResponse.TicketStatus.newBuilder()
                    .setSupplierId(2)
                    .setSupplierName("Supplier 2")
                    .setSupplierType(SupplierOffer.SupplierType.TYPE_THIRD_PARTY)
                    .setAutoDeadlineDate(17676)
                    .setBaseStatus(SupplierOffer.Offer.InternalProcessingStatus.IN_MODERATION)
                    .setIdentifier("NOTICKET-1")
                    .setProcessingTicketId(1)
                    .setTicketCritical(false)
                    .addCategories(
                        MboCategory.GetTicketStatusesResponse.CategoryInfo.newBuilder()
                            .setActiveOffers(4)
                            .setTotalOffers(15)
                            .setCategoryId(90821)
                    )
            );

        Mockito.when(markupService.getFrozenOffers(Mockito.any())).thenReturn(
            Markup.GetFrozenOffersResponse.newBuilder()
                .addFrozenGroup(Markup.FrozenGroup.newBuilder()
                    .setProcessingTicketId(123)
                    .setTicket("MCP-123")
                    .setProcessingStatus(SupplierOffer.Offer.InternalProcessingStatus.IN_PROCESS)
                    .setSupplierId(1L)
                    .setComment("comment1")
                    .setFreezeStartTime(100L)
                    .setFreezeFinishTime(2000L)
                    .build())
                .addFrozenGroup(Markup.FrozenGroup.newBuilder()
                    .setProcessingTicketId(1)
                    .setProcessingStatus(SupplierOffer.Offer.InternalProcessingStatus.IN_PROCESS)
                    .setSupplierId(2L)
                    .setCategoryId(100)
                    .setComment("comment2")
                    .setFreezeStartTime(1L)
                    .setFreezeFinishTime(2L)
                    .build())
                .addFrozenGroup(Markup.FrozenGroup.newBuilder()
                    .setProcessingTicketId(1)
                    .setProcessingStatus(SupplierOffer.Offer.InternalProcessingStatus.IN_PROCESS)
                    .setSupplierId(2L)
                    .setCategoryId(200)
                    .setComment("comment2")
                    .setFreezeStartTime(3L)
                    .setFreezeFinishTime(4L)
                    .build())
                .addFrozenGroup(Markup.FrozenGroup.newBuilder()
                    .setProcessingTicketId(5)
                    .setTicket("MCP-5")
                    .setProcessingStatus(SupplierOffer.Offer.InternalProcessingStatus.IN_PROCESS)
                    .setSupplierId(1L)
                    .setComment("comment5")
                    .setFreezeStartTime(1L)
                    .setFreezeFinishTime(2L)
                    .build())
                .build()
        );

        Mockito.when(mboCategoryService.getTicketStatuses(Mockito.any())).thenReturn(builder.build());

        Mockito.when(mboCategoryService.getShortOfferInfoById(Mockito.any())).thenReturn(
            MboCategory.GetShortOfferInfoByIdResponse.newBuilder().build()
        );

        controller = new YangTasksController(mboCategoryService, categoryManagersManager, markupService);
    }

    @Test
    @Ignore("закапываем м2 https://st.yandex-team.ru/MBOASSORT-2880")
    public void getYangTasksMarkupState() {
        YangTasksMarkupStateRequest request = new YangTasksMarkupStateRequest()
                .setTaskType(TaskType.BLUE_LOGS)
                .setProcessingTicketId(1L)
                .setCategoryIds(Arrays.asList(1L, 2L));
        Markup.GetTasksByTicketRequest markupRequest = Markup.GetTasksByTicketRequest
                .newBuilder()
                .setTaskType(YangTasksController.convertTaskType(request.getTaskType()))
                .setProcessingTicketId(request.getProcessingTicketId())
                .addAllCategoryIds(request.getCategoryIds())
                .build();

        Markup.GetTasksByTicketResponse markupResponse = Markup.GetTasksByTicketResponse
                .newBuilder()
                .addAllTasks(Arrays.asList(
                        createTask(Markup.TaskState.COMPLETED_TASK, 1L, 1L, 1, "title1", "1"),
                        createTask(Markup.TaskState.RUNNING_TASK, 2L, 2L, 2, "title2", "2"),
                        createTask(2, Markup.TaskState.RUNNING_TASK, 3L, 2L, 3, "title2", "2"),
                        createTask(Markup.TaskState.RUNNING_TASK, 4L, 1L, 4, "title4", "4"),
                        createTask(4, Markup.TaskState.CANCELED_TASK, 5L, 1L, 5, "title4", "4"),
                        createTask(Markup.TaskState.RUNNING_TASK, null, 1L, 6, "title5", "5"),
                        createTask(Markup.TaskState.FORCE_FINISHED_TASK, 6L, 2L, 7, "title6", "6"),
                        createTaskWithFailedProcessedRequests(2, Markup.TaskState.COMPLETED_TASK, 7L, 2L, 8,
                                "title7", "7")))
                .build();

        Mockito.when(markupService.getTasksByTicket(Mockito.eq(markupRequest))).thenReturn(markupResponse);

        Multimap<Long, YangTaskMarkupState> statesByCategoryId = ArrayListMultimap.create();
        for (YangTasksMarkupStateByCategory state : controller.getYangTasksMarkupState(request)) {
            statesByCategoryId.putAll(state.getCategoryId(), state.getMarkupStates());
        }

        assertTrue(statesByCategoryId.keySet().containsAll(Arrays.asList(1L, 2L)));
        assertByCategory(statesByCategoryId, 1L, Map.of(
                1, markupState -> {
                    assertDataItemInfo(markupState, "1", "title1");
                    assertTaskStatus(markupState.getStatus(), YangTaskMarkupState.TaskState.COMPLETED, 1, 1L);
                    assertNull(markupState.getStatusPostProcessing());
                },
                4, markupState -> {
                    assertDataItemInfo(markupState, "4", "title4");
                    assertTaskStatus(markupState.getStatus(), YangTaskMarkupState.TaskState.COMPLETED, 4, 4L);
                    assertNull(markupState.getStatusPostProcessing());
                },
                6, markupState -> {
                    assertDataItemInfo(markupState, "5", "title5");
                    assertTaskStatus(markupState.getStatus(), YangTaskMarkupState.TaskState.NEXT_IN_LINE, 6, 0L);
                    assertNull(markupState.getStatusPostProcessing());
                }
        ));
        assertByCategory(statesByCategoryId, 2L, Map.of(
                2, markupState -> {
                    assertDataItemInfo(markupState, "2", "title2");
                    assertTaskStatus(markupState.getStatus(), YangTaskMarkupState.TaskState.IN_PROCESS, 2, 2L);
                    assertTaskStatus(markupState.getStatusPostProcessing(), YangTaskMarkupState.TaskState.IN_PROCESS, 3,
                            3L);
                },
                7, markupState -> {
                    assertDataItemInfo(markupState, "6", "title6");
                    assertTaskStatus(markupState.getStatus(), YangTaskMarkupState.TaskState.CANCELED, 7, 6L);
                    assertNull(markupState.getStatusPostProcessing());
                },
                8, markupState -> {
                    assertDataItemInfo(markupState, "7", "title7");
                    assertTaskStatus(markupState.getStatus(), YangTaskMarkupState.TaskState.ERROR_PROCESS, 8, 7L);
                    assertNull(markupState.getStatusPostProcessing());
                }
        ));
    }

    @Test
    public void getYangTaskInfosTest() {
        YangTasksRequest request = new YangTasksRequest();

        request.setDeadlineStartDate("2019-05-26");
        request.setDeadlineFinishDate("2020-05-29");
        request.setTaskType(TaskType.BLUE_LOGS);

        List<YangTasksInfo> yangTasksInfos = controller.getYangTaskInfos(request);

        assertEquals(2, yangTasksInfos.size());
        assertThat(yangTasksInfos).extracting(YangTasksInfo::getTicket)
            .containsExactlyInAnyOrder("MCP-123", null);

        Map<String, YangTasksInfo> map = yangTasksInfos.stream()
            .collect(Collectors.toMap(YangTasksInfo::getTicket, Function.identity()));

        YangTasksInfo yangTasksInfo = map.get("MCP-123");
        assertThat(yangTasksInfo).extracting(
            YangTasksInfo::getDeadline, YangTasksInfo::getSupplierType, YangTasksInfo::getTitle,
            YangTasksInfo::getSupplierId, YangTasksInfo::getCritical, YangTasksInfo::isAutoDeadline
        ).containsExactly("2020-05-29", "1P", "Supplier 1", 1L, true, false);
        if (YangTasksController.LOAD_FROZEN_OFFERS) {
            assertThat(yangTasksInfo.getFrozenOffersGroups())
                .containsExactly(
                    new FrozenOffersGroup(123, "MCP-123", null, 1L,
                        new Date(100L), new Date(2000L), "comment1")
                );
        }

        Map<Long, YangTasksInfo.CategoryStatus> categoryStatuses = yangTasksInfo.getCategoryStatuses().stream()
            .collect(Collectors.toMap(YangTasksInfo.CategoryStatus::getCategoryId, Function.identity()));
        assertEquals(2, categoryStatuses.size());
        YangTasksInfo.CategoryStatus categoryStatus = categoryStatuses.get(237418L);
        assertThat(categoryStatus).extracting(
            YangTasksInfo.CategoryStatus::getActiveItems,
            YangTasksInfo.CategoryStatus::getTotalItems,
            YangTasksInfo.CategoryStatus::getManagerUid
        ).containsExactly(4, 5, MANAGER_UID);
        List<YangTasksInfo.CategoryStatus.StuckOffer> stuckOffers = categoryStatus.getStuckOffers();
        assertEquals(1, stuckOffers.size());
        YangTasksInfo.CategoryStatus.StuckOffer stuckOffer = stuckOffers.get(0);
        assertThat(stuckOffer).extracting(
            YangTasksInfo.CategoryStatus.StuckOffer::getOfferTitle,
            YangTasksInfo.CategoryStatus.StuckOffer::getShopSku,
            YangTasksInfo.CategoryStatus.StuckOffer::getOfferId,
            YangTasksInfo.CategoryStatus.StuckOffer::getStuckErrorMessage,
            YangTasksInfo.CategoryStatus.StuckOffer::getSupplierId
        ).containsExactly(
            "Offer title",
            "Shop sku",
            1L,
            "Super error",
            1L
        );
        YangTasksInfo.CategoryStatus categoryStatus2 = categoryStatuses.get(1569931L);
        assertThat(categoryStatus2).extracting(
            YangTasksInfo.CategoryStatus::getActiveItems,
            YangTasksInfo.CategoryStatus::getTotalItems,
            YangTasksInfo.CategoryStatus::getManagerUid
        ).containsExactly(0, 1, null);

        YangTasksInfo yangTasksInfo2 = map.get(null);
        assertThat(yangTasksInfo2).extracting(
            YangTasksInfo::getDeadline, YangTasksInfo::getSupplierType, YangTasksInfo::getTitle,
            YangTasksInfo::getSupplierId, YangTasksInfo::getCritical, YangTasksInfo::isAutoDeadline
        ).containsExactly("2018-05-25", "3P", "Supplier 2", 2L, false, true);
        if (YangTasksController.LOAD_FROZEN_OFFERS) {
            assertThat(yangTasksInfo2.getFrozenOffersGroups()).containsExactlyInAnyOrder(
                new FrozenOffersGroup(1, "", 100L, 2L, new Date(1L),
                    new Date(2L), "comment2"),
                new FrozenOffersGroup(1, "", 200L, 2L, new Date(3L),
                    new Date(4L), "comment2")
            );
        }
    }

    private void assertDataItemInfo(YangTaskMarkupState markupState, String id, String title) {
        assertEquals(markupState.getDataItemsInfo().size(), 1);
        assertEquals(markupState.getDataItemsInfo().get(0).getId(), id);
        assertEquals(markupState.getDataItemsInfo().get(0).getTitle(), title);
    }

    private void assertTaskStatus(YangTaskMarkupState.TaskStatus actual, YangTaskMarkupState.TaskState taskState,
                                  Integer taskId, Long userId) {
        assertEquals(actual.getTaskState(), taskState);
        assertEquals(actual.getTaskId(), taskId);
        assertEquals(actual.getUserId(), userId);
    }

    private void assertByCategory(Multimap<Long, YangTaskMarkupState> statesByCategoryId, long categoryId,
                                  Map<Integer, Consumer<YangTaskMarkupState>> markupStateCheckers) {
        Map<Integer, List<YangTaskMarkupState>> byTaskId = statesByCategoryId.get(categoryId).stream()
                .collect(Collectors.groupingBy(x -> x.getStatus().getTaskId()));
        assertTrue(byTaskId.keySet().containsAll(markupStateCheckers.keySet()));
        byTaskId.forEach((key, value) -> {
            assertEquals(value.size(), 1);
            markupStateCheckers.get(key).accept(value.get(0));
        });
    }

    private Markup.TaskInfo createTask(int headTaskId, Markup.TaskState taskState, Long userId, long categoryId,
                                       int taskId, String titleDataInfo, String idDataInfo) {
        return createTask(taskState, userId, categoryId, taskId, titleDataInfo, idDataInfo).toBuilder()
                .setHeadTaskId(headTaskId)
                .build();
    }

    private Markup.TaskInfo createTaskWithFailedProcessedRequests(int failedCount, Markup.TaskState taskState,
                                  Long userId, long categoryId, int taskId, String titleDataInfo, String idDataInfo) {
        Markup.TaskInfo taskInfo = createTask(taskState, userId, categoryId, taskId, titleDataInfo, idDataInfo);
        return taskInfo.toBuilder()
                .setFailedProcessingRequestsCount(failedCount)
                .build();
    }

    private Markup.TaskInfo createTask(Markup.TaskState taskState, Long userId, long categoryId, int taskId,
                                       String titleDataInfo, String idDataInfo) {
        Markup.TaskInfo.Builder taskInfo = Markup.TaskInfo
                .newBuilder()
                .setState(taskState)
                .setCategoryId(categoryId)
                .setTaskId(taskId)
                .addAllDataItemInfos(Arrays.asList(
                        Markup.TaskInfo.DataItemInfo
                                .newBuilder()
                                .setTitle(titleDataInfo)
                                .setId(idDataInfo)
                                .build()
                ));
        if (userId != null) {
            taskInfo.setUserId(userId);
        }
        return taskInfo.build();
    }

}
