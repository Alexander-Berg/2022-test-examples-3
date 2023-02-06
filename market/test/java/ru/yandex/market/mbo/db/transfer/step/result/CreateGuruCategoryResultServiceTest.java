package ru.yandex.market.mbo.db.transfer.step.result;


import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.category.mappings.CategoryMappingService;
import ru.yandex.market.mbo.core.guru.GuruCategoryService;
import ru.yandex.market.mbo.db.TovarTreeService;
import ru.yandex.market.mbo.db.transfer.step.GuruCategoryNameConfigService;
import ru.yandex.market.mbo.db.transfer.step.ModelTransferStepResultInfoService;
import ru.yandex.market.mbo.gwt.models.User;
import ru.yandex.market.mbo.gwt.models.transfer.ResultInfo;
import ru.yandex.market.mbo.gwt.models.transfer.step.GuruCategoryEntry;
import ru.yandex.market.mbo.gwt.models.transfer.step.GuruCategoryXslName;
import ru.yandex.market.mbo.gwt.models.transfer.step.GuruCategoryXslNameConfig;
import ru.yandex.market.mbo.gwt.models.transfer.step.ResultEntry;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author danfertev
 * @since 31.10.2018
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class CreateGuruCategoryResultServiceTest {
    private static final long CATEGORY1 = 1L;
    private static final long CATEGORY2 = 2L;
    private static final long GURU_CATEGORY1 = 101L;
    private static final long GURU_CATEGORY2 = 102L;

    private static final AtomicLong ID_GENERATOR = new AtomicLong(0L);

    private CreateGuruCategoryResultService resultService;
    private GuruCategoryEntryDAO guruCategoryEntryDAO;
    private TovarTreeService tovarTreeService;
    private GuruCategoryService guruCategoryService;
    private CategoryMappingService categoryMappingService;
    private GuruCategoryNameConfigService configService;
    private User user = new User(999L);

    @Before
    public void setUp() throws Exception {
        ModelTransferStepResultInfoService stepResultInfoService = mock(ModelTransferStepResultInfoService.class);
        guruCategoryEntryDAO = mock(GuruCategoryEntryDAO.class);
        tovarTreeService = mock(TovarTreeService.class);
        configService = mock(GuruCategoryNameConfigService.class);
        categoryMappingService = mock(CategoryMappingService.class);
        guruCategoryService = mock(GuruCategoryService.class);

        resultService = new CreateGuruCategoryResultService(stepResultInfoService, guruCategoryEntryDAO,
            tovarTreeService, configService, categoryMappingService, guruCategoryService);

        when(configService.getStepConfig(anyLong())).thenReturn(new GuruCategoryXslNameConfig(
            Collections.singletonList(new GuruCategoryXslName(CATEGORY1, "name", "xslName"))));

        when(categoryMappingService.getGuruCategoryByCategoryId(anyLong())).thenReturn(null);

        when(stepResultInfoService.create(any(), any())).then(args -> {
            ResultInfo resultInfo = args.getArgument(0);
            resultInfo.setId(ID_GENERATOR.incrementAndGet());
            return resultInfo;
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalEnqueueAction() {
        resultService.doAction(1L, ResultInfo.Action.ENQUEUE, "", user);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalCancelAction() {
        resultService.doAction(1L, ResultInfo.Action.CANCEL, "", user);
    }

    @Test(expected = IllegalStateException.class)
    public void testNullStepConfig() {
        when(configService.getStepConfig(anyLong())).thenReturn(null);
        resultService.doAction(1L, ResultInfo.Action.COMPLETE, "", user);
    }

    @Test(expected = IllegalStateException.class)
    public void testEmptyStepConfig() {
        when(configService.getStepConfig(anyLong())).thenReturn(new GuruCategoryXslNameConfig());
        resultService.doAction(1L, ResultInfo.Action.COMPLETE, "", user);
    }

    @Test
    public void testGuruAlreadyExists() {
        List<GuruCategoryEntry> resultEntries = new ArrayList<>();
        doAnswer(args -> resultEntries.addAll(args.getArgument(1)))
            .when(guruCategoryEntryDAO).add(anyLong(), anyList());

        when(categoryMappingService.getGuruCategoryByCategoryId(eq(CATEGORY1))).thenReturn(GURU_CATEGORY1);

        ResultInfo resultInfo = resultService.doAction(1L, ResultInfo.Action.COMPLETE, "", user);

        assertThat(resultInfo.getStatus()).isEqualTo(ResultInfo.Status.COMPLETED);
        assertThat(resultEntries).hasSize(1);
        assertGuruCategoryEntry(resultEntries.get(0), ResultEntry.Status.SUCCESS,
            "Guru category already exists");
    }

    @Test
    public void testGuruNotCreated() {
        List<GuruCategoryEntry> resultEntries = new ArrayList<>();
        doAnswer(args -> resultEntries.addAll(args.getArgument(1)))
            .when(guruCategoryEntryDAO).add(anyLong(), anyList());

        doThrow(new RuntimeException()).when(guruCategoryService)
            .createGuruCategory(eq(CATEGORY1), anyString(), anyString(), anyLong());

        ResultInfo resultInfo = resultService.doAction(1L, ResultInfo.Action.COMPLETE, "", user);

        assertThat(resultInfo.getStatus()).isEqualTo(ResultInfo.Status.FAILED);
        assertThat(resultEntries).hasSize(1);
        assertGuruCategoryEntry(resultEntries.get(0), ResultEntry.Status.FAILURE,
            "Unable to create guru category");
    }

    @Test
    public void testGuruCreatedNotLinked() {
        List<GuruCategoryEntry> resultEntries = new ArrayList<>();
        doAnswer(args -> resultEntries.addAll(args.getArgument(1)))
            .when(guruCategoryEntryDAO).add(anyLong(), anyList());

        when(guruCategoryService.createGuruCategory(eq(CATEGORY1), anyString(), anyString(), anyLong()))
            .thenReturn(GURU_CATEGORY1);
        doThrow(new RuntimeException()).when(tovarTreeService).loadCategoryByHid(eq(CATEGORY1));

        ResultInfo resultInfo = resultService.doAction(1L, ResultInfo.Action.COMPLETE, "", user);

        assertThat(resultInfo.getStatus()).isEqualTo(ResultInfo.Status.FAILED);
        assertThat(resultEntries).hasSize(1);
        assertGuruCategoryEntry(resultEntries.get(0), ResultEntry.Status.FAILURE,
            "Unable to link guru category and tovar category");
    }

    @Test
    public void testGuruCreatedLinked() {
        List<GuruCategoryEntry> resultEntries = new ArrayList<>();
        doAnswer(args -> resultEntries.addAll(args.getArgument(1)))
            .when(guruCategoryEntryDAO).add(anyLong(), anyList());

        when(guruCategoryService.createGuruCategory(eq(CATEGORY1), anyString(), anyString(), anyLong()))
            .thenReturn(GURU_CATEGORY1);
        TovarCategory tovarCategory = mock(TovarCategory.class);
        when(tovarCategory.getDeepCopy()).thenReturn(tovarCategory);
        when(tovarTreeService.loadCategoryByHid(eq(CATEGORY1))).thenReturn(tovarCategory);

        ResultInfo resultInfo = resultService.doAction(1L, ResultInfo.Action.COMPLETE, "", user);

        assertThat(resultInfo.getStatus()).isEqualTo(ResultInfo.Status.COMPLETED);
        assertThat(resultEntries).hasSize(1);
        assertGuruCategoryEntry(resultEntries.get(0), ResultEntry.Status.SUCCESS,
            "Guru category created and linked");
    }

    @Test
    public void testActionFailedIfAnyGuruFailed() {
        List<GuruCategoryEntry> resultEntries = new ArrayList<>();
        doAnswer(args -> resultEntries.addAll(args.getArgument(1)))
            .when(guruCategoryEntryDAO).add(anyLong(), anyList());

        when(configService.getStepConfig(anyLong())).thenReturn(new GuruCategoryXslNameConfig(Arrays.asList(
            new GuruCategoryXslName(CATEGORY1, "name1", "xslName1"),
            new GuruCategoryXslName(CATEGORY2, "name2", "xslName2"))));
        when(guruCategoryService.createGuruCategory(eq(CATEGORY1), anyString(), anyString(), anyLong()))
            .thenReturn(GURU_CATEGORY1);
        when(guruCategoryService.createGuruCategory(eq(CATEGORY2), anyString(), anyString(), anyLong()))
            .thenReturn(GURU_CATEGORY2);
        TovarCategory tovarCategory = mock(TovarCategory.class);
        when(tovarCategory.getDeepCopy()).thenReturn(tovarCategory);
        when(tovarTreeService.loadCategoryByHid(eq(CATEGORY1))).thenReturn(tovarCategory);
        doThrow(new RuntimeException()).when(tovarTreeService).loadCategoryByHid(eq(CATEGORY2));

        ResultInfo resultInfo = resultService.doAction(1L, ResultInfo.Action.COMPLETE, "", user);

        assertThat(resultInfo.getStatus()).isEqualTo(ResultInfo.Status.FAILED);
        assertThat(resultEntries).hasSize(2);
        assertGuruCategoryEntry(resultEntries.get(0), ResultEntry.Status.SUCCESS,
            "Guru category created and linked");
        assertGuruCategoryEntry(resultEntries.get(1), ResultEntry.Status.FAILURE,
            "Unable to link guru category and tovar category");
    }

    private void assertGuruCategoryEntry(GuruCategoryEntry entry, ResultEntry.Status status, String statusMesage) {
        assertThat(entry.getStatus()).isEqualTo(status);
        assertThat(entry.getStatusMessage()).isEqualTo(statusMesage);

    }
}
