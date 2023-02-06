package ru.yandex.market.wms.packing;

import ru.yandex.market.wms.packing.dto.PackingTaskDto;
import ru.yandex.market.wms.packing.enums.PackingSourceType;
import ru.yandex.market.wms.packing.pojo.IdleTablesDto;
import ru.yandex.market.wms.packing.pojo.PackingTable;
import ru.yandex.market.wms.packing.pojo.TaskConsumer;

public class MockTaskConsumer implements TaskConsumer {
    private PackingTable table;
    private PackingTaskDto task;
    private String taskAssignmentError;
    private Boolean idle;
    private String user;

    public MockTaskConsumer(PackingTable table, String user) {
        this.table = table;
        this.user = user;
    }

    @Override
    public void acceptTask(PackingTaskDto task) {
        reset();
        this.task = task;
    }

    @Override
    public void acceptError(String error) {
        reset();
        this.taskAssignmentError = error;
    }

    @Override
    public void idle(IdleTablesDto idleTable) {
        reset();
        this.idle = true;
    }

    @Override
    public PackingTable getTable() {
        return table;
    }

    @Override
    public String getUser() {
        return user;
    }

    @Override
    public PackingSourceType getSourceType() {
        return PackingSourceType.NEW_PACKING;
    }

    @Override
    public void close(String reason) {

    }

    public PackingTaskDto getTask() {
        return task;
    }

    public String getTaskAssignmentError() {
        return taskAssignmentError;
    }

    public Boolean isIdle() {
        return idle;
    }

    private void reset() {
        this.task = null;
        this.taskAssignmentError = null;
        this.idle = null;
    }

    @Override
    public String toString() {
        return "MockTicketConsumer{" +
            "table=" + table +
            ", task=" + task +
            ", taskAssignmentError='" + taskAssignmentError + '\'' +
            ", idle=" + idle +
            '}';
    }
}
