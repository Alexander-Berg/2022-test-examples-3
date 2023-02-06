package ru.yandex.market.mboc.tms.service.acceptance.rule;

import java.text.SimpleDateFormat;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.mbo.taskqueue.TaskQueueRegistrator;
import ru.yandex.market.mbo.taskqueue.TaskQueueRepository;
import ru.yandex.market.mbo.taskqueue.TaskRecord;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.offers.acceptance.rule.CategoryRule;
import ru.yandex.market.mboc.common.offers.acceptance.rule.CategoryRuleRepository;
import ru.yandex.market.mboc.common.offers.acceptance.rule.CategoryRuleService;
import ru.yandex.market.mboc.common.offers.acceptance.rule.CategoryRuleStagingRepository;
import ru.yandex.market.mboc.common.offers.acceptance.rule.StagedCategoryRule;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.BIZ_ID_SUPPLIER;

public class CategoryRuleStagingExecutorTest extends BaseDbTestClass {
    @Value("${taskqueue.tables.schema}")
    private String taskqueueTablesSchema;

    @Autowired
    private CategoryRuleStagingRepository stagingRepository;
    @Autowired
    private CategoryRuleRepository mainRepository;

    private CategoryRuleDownloader categoryRuleDownloaderMock;
    private CategoryRuleService categoryRuleServiceMock;
    private TaskQueueRepository taskQueueRepository;

    private CategoryRuleStagingExecutor executor;

    @Before
    public void setUp() {
        categoryRuleDownloaderMock = mock(CategoryRuleDownloader.class);
        categoryRuleServiceMock = mock(CategoryRuleService.class);

        taskQueueRepository = new TaskQueueRepository(
            namedParameterJdbcTemplate,
            transactionTemplate,
            taskqueueTablesSchema,
            "mboc-tms-task-queue"
        );
        var taskQueueRegistrator = new TaskQueueRegistrator(
            taskQueueRepository,
            new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"))
        );
        executor = new CategoryRuleStagingExecutor(
            categoryRuleDownloaderMock,
            TransactionHelper.MOCK,
            stagingRepository,
            mainRepository,
            categoryRuleServiceMock,
            storageKeyValueService,
            taskQueueRegistrator
        );
    }

    @Test
    public void testStaging() {
        long category1 = 333333;
        long category2 = 333334;
        long category3 = 333335;
        long category4 = 333336;
        long biz1 = BIZ_ID_SUPPLIER;
        long biz2 = BIZ_ID_SUPPLIER + 1;
        int vendor = 123;

        mainRepository.insert(List.of(
            new CategoryRule(category1, biz2, 0),
            new CategoryRule(category3, biz1, 0),
            new CategoryRule(category4, biz1, 0)
        ));
        stagingRepository.insert(List.of(
            new StagedCategoryRule(category1, biz2, 0, false),
            new StagedCategoryRule(category3, biz1, 0, false)
        ));

        storageKeyValueService.putValue(CategoryRuleStagingExecutor.DOWNLOAD_BATCH_SIZE_KEY, 10);
        doAnswer(invocation -> {
            stagingRepository.insert(List.of(
                new StagedCategoryRule(category1, biz1, vendor, false),
                new StagedCategoryRule(category2, biz2, vendor, false)
            ));
            stagingRepository.delete(List.of(
                new StagedCategoryRule(category2, biz1, vendor, false),
                new StagedCategoryRule(category3, biz1, 0, false)
            ));
            return null;
        }).when(categoryRuleDownloaderMock).download(eq(10L));

        executor.execute();

        verify(categoryRuleDownloaderMock, times(1)).download(eq(10L));

        var tasks = taskQueueRepository.findAll();
        assertThat(tasks).hasSize(2);
        assertThat(tasks).extracting(TaskRecord::getTaskType).containsOnly("OfferProcessingSenderTask");
        assertThat(tasks).extracting(TaskRecord::getTaskData).containsExactlyInAnyOrder(
            "{\"filter\":{\"categoryIds\":[" + category1 + "],\"vendorIds\":[],\"businessIds\":[]}}",
            "{\"filter\":{\"categoryIds\":[" + category2 + "],\"vendorIds\":[],\"businessIds\":[]}}"
        );

        assertThat(mainRepository.findAll()).containsExactlyInAnyOrder(
            new CategoryRule(category1, biz1, vendor),
            new CategoryRule(category1, biz2, 0),
            new CategoryRule(category2, biz2, vendor)
        );
        assertThat(stagingRepository.findAll()).allMatch(StagedCategoryRule::isDoneStaging);
    }
}
