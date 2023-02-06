package ru.yandex.market.wms.picking.modules.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.messaging.MessageHeaders;

import ru.yandex.market.wms.common.model.enums.TaskStatus;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.PickDetail;
import ru.yandex.market.wms.common.spring.dao.entity.TaskDetail;
import ru.yandex.market.wms.common.spring.dao.implementation.LotDao;
import ru.yandex.market.wms.common.spring.dao.implementation.PickDetailDao;
import ru.yandex.market.wms.common.spring.dao.implementation.TaskDetailDao;
import ru.yandex.market.wms.common.spring.dao.implementation.UserActivityDao;
import ru.yandex.market.wms.inventorization.core.model.ShortInventoryTaskRequest;
import ru.yandex.market.wms.picking.modules.async.InventoryTaskProducer;
import ru.yandex.market.wms.picking.modules.async.PickingLostAsyncService;
import ru.yandex.market.wms.picking.modules.model.PickingLostAsyncDto;
import ru.yandex.market.wms.shared.libs.authorization.SecurityDataProvider;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

class PickingUpdateLostServiceTest extends IntegrationTest {
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
    @Autowired
    private PickingUpdateLostService pickingUpdateLostService;
    @SpyBean
    @Autowired
    private InventoryTaskProducer inventoryTaskProducer;


    @Test
    @DatabaseSetup(value = {"/controller/lost/happy/before.xml"}, connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/lost/happy/after.xml", assertionMode = NON_STRICT_UNORDERED,
            connection = "wmwhseConnection")
    public void testLostHappyPath() {
        receivePickingLost("02", "PDK3");
        Mockito.verify(inventoryTaskProducer, Mockito.atLeastOnce())
                .produceShortInventoryTask(Mockito.any(ShortInventoryTaskRequest.class));
    }

    @Test
    @DatabaseSetup(value = {"/controller/lost/happy/with-inventory-before.xml"}, connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/lost/happy/with-inventory-after.xml", assertionMode = NON_STRICT_UNORDERED,
            connection = "wmwhseConnection")
    public void testLostWithInventoryHappyPath() {
        receivePickingLost("02", "PDK3");
        Mockito.verify(inventoryTaskProducer, Mockito.atLeastOnce())
                .produceShortInventoryTask(Mockito.any(ShortInventoryTaskRequest.class));
    }

    @Test
    @DatabaseSetup(value = "/service/lost/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/service/lost/after.xml", assertionMode = NON_STRICT_UNORDERED,
            connection = "wmwhseConnection")
    public void processUpdateLostItemWhenNoSerialInventory() {
        PickingLostAsyncDto pickingLost = getPickingLostWhenNoSerialInventory();
        pickingUpdateLostService.processUpdateLostItem(pickingLost);
        Mockito.verify(inventoryTaskProducer, Mockito.atLeastOnce())
                .produceShortInventoryTask(Mockito.any(ShortInventoryTaskRequest.class));
    }

    @Test
    @DatabaseSetup(value = "/service/lost/affect-order/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/service/lost/affect-order/after.xml", assertionMode = NON_STRICT_UNORDERED,
            connection = "wmwhseConnection")
    public void processUpdateLostItemWhenNoSerialInventoryAffectOrder() {
        PickingLostAsyncDto pickingLost = getPickingLostWhenNoSerialInventory();
        pickingUpdateLostService.processUpdateLostItem(pickingLost);
    }


    private void receivePickingLost(String lostAssignmentNumber, String pickPositionKey) {
        PickingLostAsyncDto pickingLostAsyncDto = makePickingLost(lostAssignmentNumber, pickPositionKey);
        receivePickingLost(pickingLostAsyncDto);
    }

    private void receivePickingLost(PickingLostAsyncDto pickingLostAsyncDto) {
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

    private PickingLostAsyncDto getPickingLostWhenNoSerialInventory() {
        return PickingLostAsyncDto.builder()
                .lostPickDetails(Collections.singletonList(
                        PickDetail.builder()
                                .pickDetailKey("PDK1")
                                .orderKey("B000001001")
                                .orderLineNumber("03")
                                .storerKey("100")
                                .sku("ROV0000000000000000004")
                                .qty(BigDecimal.valueOf(5))
                                .cartonGroup("SHIPPABLE")
                                .cartonType("BC1")
                                .selectedCarton("BC1")
                                .assignmentNumber("01")
                                .editWho("test")
                                .build()
                ))
                .taskDetails(Collections.singletonList(
                        TaskDetail.builder()
                                .taskDetailKey("TDK0001")
                                .taskType("")
                                .storerKey("100")
                                .sku("ROV0000000000000000004")
                                .lot("L5")
                                .qty(BigDecimal.valueOf(5))
                                .fromLoc("C4-10-0001")
                                .fromId("fromId1")
                                .toLoc("toLoc1")
                                .toId("toId1")
                                .status(TaskStatus.PENDING)
                                .priority("5")
                                .userPosition("1")
                                .startTime(LocalDateTime.parse("2021-08-27T13:44:44.390856").toInstant(ZoneOffset.UTC))
                                .endTime(LocalDateTime.parse("2021-08-27T13:44:44.390857").toInstant(ZoneOffset.UTC))
                                .orderKey("B000001001")
                                .seqNo(99999)
                                .waveKey("W5")
                                .orderLineNumber("03")
                                .pickDetailKey("PDK1")
                                .door("")
                                .assignmentNumber("01")
                                .build()
                ))
                .user("test")
                .build();
    }
}
