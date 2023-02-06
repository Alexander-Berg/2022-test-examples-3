package ru.yandex.market.wms.picking.modules.service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageHeaders;

import ru.yandex.market.wms.common.spring.dao.entity.PickDetail;
import ru.yandex.market.wms.common.spring.dao.entity.TaskDetail;
import ru.yandex.market.wms.common.spring.dao.implementation.LotDao;
import ru.yandex.market.wms.common.spring.dao.implementation.PickDetailDao;
import ru.yandex.market.wms.common.spring.dao.implementation.TaskDetailDao;
import ru.yandex.market.wms.common.spring.dao.implementation.UserActivityDao;
import ru.yandex.market.wms.picking.modules.async.PickingLostAsyncService;
import ru.yandex.market.wms.picking.modules.model.PickingLostAsyncDto;
import ru.yandex.market.wms.picking.utils.TestcontainersConfiguration;
import ru.yandex.market.wms.shared.libs.authorization.SecurityDataProvider;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

class PickingUpdateLostServiceLargeTest extends TestcontainersConfiguration {
    @Autowired
    private PickingLostAsyncService pickingLostService;
    @Autowired
    private SecurityDataProvider securityDataProvider;
    @Autowired
    private PickDetailDao pickDetailDao;
    @Autowired
    private TaskDetailDao taskDetailDao;
    @Autowired
    private UserActivityDao userActivityDao;
    @Autowired
    private LotDao lotDao;

    @Test
    @DatabaseSetup(value = {
            "/testcontainers/pickingUpdateLostServiceLargeTest/lost/with-replacements/before.xml",
            "/testcontainers/pickingUpdateLostServiceLargeTest/lost/with-replacements/replacements.xml"
    }, connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/testcontainers/pickingUpdateLostServiceLargeTest/lost/with-replacements/after.xml",
            assertionMode = NON_STRICT_UNORDERED, connection = "wmwhseConnection")
    public void testLostWithReplacements() {
        receivePickingLost();
    }

    @Test
    @DatabaseSetup(value = {
            "/testcontainers/pickingUpdateLostServiceLargeTest/lost/with-replacements/before-at-building.xml",
            "/testcontainers/pickingUpdateLostServiceLargeTest/lost/with-replacements/replacements.xml"
    }, connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/testcontainers/pickingUpdateLostServiceLargeTest/lost/with-replacements/after-at-building.xml",
            assertionMode = NON_STRICT_UNORDERED, connection = "wmwhseConnection")
    public void testLostWithReplacementsAtBuilding() {
        receivePickingLost();
    }

    private void receivePickingLost() {
        String lostAssignmentNumber = "02";
        String pickPositionKey = "PDK3";
        PickingLostAsyncDto pickingLostAsyncDto = makePickingLost(lostAssignmentNumber, pickPositionKey);
        pickingLostService.receivePickingLost(pickingLostAsyncDto, new MessageHeaders(Collections.emptyMap()));
    }

    private PickingLostAsyncDto makePickingLost(String lostAssignmentNumber, String pickPositionKey) {
        TaskDetail taskDetail = taskDetailDao
                .findTaskDetailByPickDetailKeyOrNull(lostAssignmentNumber, pickPositionKey);
        List<PickDetail> lostPickDetails = pickDetailDao.findLostPickDetails(
                taskDetail.getSkuId(), taskDetail.getFromKey()
        );
        List<TaskDetail> lostTaskDetails = taskDetailDao.findLostTaskDetails(
                taskDetail.getSkuId(), taskDetail.getFromKey()
        );
        List<String> taskDetailKeys = StreamEx.of(lostTaskDetails).map(TaskDetail::getTaskDetailKey).toList();
        List<String> pickDetailKeys = StreamEx.of(lostPickDetails).map(PickDetail::getPickDetailKey).toList();
        String user = securityDataProvider.getUser();
        taskDetailDao.updateLost(taskDetailKeys, user);
        pickDetailDao.deleteLost(pickDetailKeys);
        userActivityDao.updateUserActivityForLost(taskDetailKeys, user);
        BigDecimal qtyUnLock = lostPickDetails.stream()
                .map(PickDetail::getQty)
                .reduce(BigDecimal::add)
                .get();
        lotDao.updateLotToDeallocateQty(
                qtyUnLock.intValue(),
                taskDetail.getLot(),
                taskDetail.getSku(),
                taskDetail.getStorerKey(),
                user);

        return PickingLostAsyncDto.builder()
                .lostPickDetails(lostPickDetails)
                .taskDetails(lostTaskDetails)
                .user(user)
                .build();
    }
}
