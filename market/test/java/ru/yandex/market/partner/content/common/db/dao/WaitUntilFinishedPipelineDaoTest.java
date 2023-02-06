package ru.yandex.market.partner.content.common.db.dao;

import io.qameta.allure.Issue;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.partner.content.common.BaseDbCommonTest;
import ru.yandex.market.partner.content.common.db.jooq.enums.PipelineType;
import ru.yandex.market.partner.content.common.engine.parameter.RequestProcessFileData;

import java.sql.Timestamp;
import java.time.Instant;

@Issue("MARKETIR-9852")
public class WaitUntilFinishedPipelineDaoTest extends BaseDbCommonTest {
    private static final int SOURCE_ID = 4323927;
    private static final int OTHER_SOURCE_ID = 249872;
    private static final int PARTNER_SHOP_ID = 174611;
    private static final int OTHER_PARTNER_SHOP_ID = 98574;
    private static final long CATEGORY_ID = 756789;
    private static final long OTHER_CATEGORY_ID = 36756453;

    @Autowired
    private PipelineService pipelineService;
    @Autowired
    private WaitUntilFinishedPipelineDao waitUntilFinishedPipelineDao;

    @Test
    public void requestIsNotBlockedOnEmptyTable() {
        createSource(SOURCE_ID, PARTNER_SHOP_ID);
        Long requestId = createFileDataProcessRequest(SOURCE_ID);

        // ---

        boolean isBlocked = waitUntilFinishedPipelineDao.isRequestBlockedInCategory(requestId, CATEGORY_ID);

        // ---

        Assert.assertFalse(isBlocked);
    }

    @Test
    public void requestIsNotBlockedByLaterRequests() {
        createSource(SOURCE_ID, PARTNER_SHOP_ID);
        Long requestId = createFileDataProcessRequest(SOURCE_ID);

        createRequestWithWait(SOURCE_ID);

        // ---

        boolean isBlocked = waitUntilFinishedPipelineDao.isRequestBlockedInCategory(requestId, CATEGORY_ID);

        // ---

        Assert.assertFalse(isBlocked);
    }

    @Test
    public void requestIsBlockedByRequestsWithoutCategory() {
        createSource(SOURCE_ID, PARTNER_SHOP_ID);

        createRequestWithWait(SOURCE_ID);

        Long requestId = createFileDataProcessRequest(SOURCE_ID);

        // ---

        boolean isBlocked = waitUntilFinishedPipelineDao.isRequestBlockedInCategory(requestId, CATEGORY_ID);

        // ---

        Assert.assertTrue(isBlocked);
    }

    @Test
    public void requestIsNotBlockedByRequestsInOtherCategories() {
        createSource(SOURCE_ID, PARTNER_SHOP_ID);

        createRequestWithWaitInCategory(SOURCE_ID, OTHER_CATEGORY_ID);

        createRequestWithWaitInCategory(SOURCE_ID, OTHER_CATEGORY_ID);

        Long requestId = createFileDataProcessRequest(SOURCE_ID);

        // ---

        boolean isBlocked = waitUntilFinishedPipelineDao.isRequestBlockedInCategory(requestId, CATEGORY_ID);

        // ---

        Assert.assertFalse(isBlocked);
    }

    @Test
    public void requestIsBlockedByRequestsInSameCategory() {
        createSource(SOURCE_ID, PARTNER_SHOP_ID);

        createRequestWithWaitInCategory(SOURCE_ID, CATEGORY_ID);
        createRequestWithWaitInCategory(SOURCE_ID, OTHER_CATEGORY_ID);

        Long requestId = createFileDataProcessRequest(SOURCE_ID);

        // ---

        boolean isBlocked = waitUntilFinishedPipelineDao.isRequestBlockedInCategory(requestId, CATEGORY_ID);

        // ---

        Assert.assertTrue(isBlocked);
    }

    @Test
    public void requestIsNotBlockedByRequestsOfOtherSources() {
        createSource(SOURCE_ID, PARTNER_SHOP_ID);
        createSource(OTHER_SOURCE_ID, OTHER_PARTNER_SHOP_ID);

        createRequestWithWaitInCategory(OTHER_SOURCE_ID, CATEGORY_ID);
        createRequestWithWaitInCategory(OTHER_SOURCE_ID, OTHER_CATEGORY_ID);

        Long requestId = createFileDataProcessRequest(SOURCE_ID);

        // ---

        boolean isBlocked = waitUntilFinishedPipelineDao.isRequestBlockedInCategory(requestId, CATEGORY_ID);

        // ---

        Assert.assertFalse(isBlocked);
    }

    private Long createRequestWithWait(Integer sourceId) {
        Long requestId = createFileDataProcessRequest(sourceId);

        Long pipelineId = pipelineService.createPipeline(
            new RequestProcessFileData(requestId),
            PipelineType.GOOD_CONTENT_SINGLE_XLS,
            1
        );

        waitUntilFinishedPipelineDao.addPipeline(pipelineId, requestId);

        return requestId;
    }

    private void createRequestWithWaitInCategory(Integer sourceId, Long categoryId) {
        Long requestId = createRequestWithWait(sourceId);
        Long processId = createFileProcessId(requestId);

        dataBucketDao.getOrCreateDataBucket(categoryId, processId, sourceId, Timestamp.from(Instant.now()));
    }
}