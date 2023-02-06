package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.scheduler;

import io.qameta.allure.Step;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.DatacreatorClient;
import ru.yandex.market.wms.common.model.enums.TaskStatus;
import ru.yandex.market.wms.common.model.enums.TaskType;
import ru.yandex.market.wms.common.spring.dao.entity.LotLocIdKey;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.common.spring.dao.entity.TaskDetail;
import ru.yandex.market.wms.common.spring.enums.ReplenishmentMoveSubTask;
import ru.yandex.market.wms.common.spring.pojo.repl.SupplyTask;
import ru.yandex.market.wms.common.spring.utils.uuid.TimeBasedGenerator;
import ru.yandex.market.wms.common.spring.utils.uuid.UuidGenerator;

@Resource.Classpath({"wms/infor.properties"})
public class SchedulerSteps {

    @Property("infor.username")
    private String username;

    private static final DatacreatorClient dataCreator = new DatacreatorClient();
    private static final UuidGenerator uuidGenerator = new TimeBasedGenerator();

    public SchedulerSteps() {
        PropertyLoader.newInstance().populate(this);
    }
    /**
     * @see ru.yandex.market.wms.common.spring.service.replenishment.ReplenishmentService метод toTaskDetails(java.util.List, java.util.Map)
     * @param qty число УИТ на пополнение
     */
    @Step("Создание задания на пополнение (эмуляция части труда scheduler-а)")
    public void createReplenishmentTasks(String areaKey, String storageLoc, String pallete,
                                          SkuId skuObject, String lot, int qty,
                                          boolean returnPalleteToStorage){
        String groupId = uuidGenerator.generate().toString();
        List<TaskDetail> taskDetails = new ArrayList<>();

        TaskDetail downTask = createMoveDownTaskDetail(groupId, storageLoc, pallete);
        taskDetails.add(downTask);

        SupplyTask supplyTask = new SupplyTask(skuObject,
                new LotLocIdKey(lot, storageLoc, pallete),
                areaKey,
                BigDecimal.valueOf(qty),
                false,
                false);
        taskDetails.add(createPickTaskDetail(groupId, supplyTask));

        if (returnPalleteToStorage) {
            taskDetails.add(createMoveUpTaskDetail(groupId, storageLoc, pallete));
        }

        for (TaskDetail tdc: taskDetails){
            dataCreator.createTaskDetail(tdc);
        }
    }

    /**
     * Общий вид построения задания взят из кода scheduler.
     * При изменении логики, визуально будет проще копипастить сюда.
     */
    private TaskDetail.TaskDetailBuilder newTaskDetailBuilder(String groupId) {
        String user = username;
        return TaskDetail.builder()
                .taskDetailKey("")
                .groupId(groupId)
                .status(TaskStatus.HELD_BY_SYSTEM)
                .addWho(user)
                .editWho(user);
    }

    private TaskDetail createMoveDownTaskDetail(String groupId, String fromLoc, String id) {
        return newTaskDetailBuilder(groupId)
                .taskType(TaskType.REPLENISHMENT_MOVE.getValue())
                .subTask(ReplenishmentMoveSubTask.DOWN.name())
                .fromLoc(fromLoc)
                .fromId(id)
                .status(TaskStatus.PENDING)
                .build();
    }

    private TaskDetail createMoveUpTaskDetail(String groupId, String toLoc, String id) {
        return newTaskDetailBuilder(groupId)
                .taskType(TaskType.REPLENISHMENT_MOVE.getValue())
                .subTask(ReplenishmentMoveSubTask.UP.name())
                .fromId(id)
                .toLoc(toLoc)
                .build();
    }

    private TaskDetail createPickTaskDetail(String groupId, SupplyTask supplyTask) {
        return newTaskDetailBuilder(groupId)
                .taskType(TaskType.REPLENISHMENT_PICK.getValue())
                .sku(supplyTask.getSkuId().getSku())
                .storerKey(supplyTask.getSkuId().getStorerKey())
                .fromLoc(supplyTask.getLotLocIdKey().getLoc())
                .fromId(supplyTask.getLotLocIdKey().getId())
                .lot(supplyTask.getLotLocIdKey().getLot())
                .qty(supplyTask.getQty())
                .originalQty(supplyTask.getQty())
                .message01(supplyTask.getIsVirtualUit() ? "БезУИТ" : "")
                .build();
    }
}
