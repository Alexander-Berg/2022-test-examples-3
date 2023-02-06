package ru.yandex.market.markup3.yang;

import ru.yandex.toloka.client.v1.aggregatedsolutions.AggregatedSolutionClient;
import ru.yandex.toloka.client.v1.assignment.AssignmentClient;
import ru.yandex.toloka.client.v1.impl.TolokaClientFactoryImpl;
import ru.yandex.toloka.client.v1.operation.OperationClient;
import ru.yandex.toloka.client.v1.pool.PoolClient;
import ru.yandex.toloka.client.v1.skill.SkillClient;
import ru.yandex.toloka.client.v1.task.TaskClient;
import ru.yandex.toloka.client.v1.tasksuite.TaskSuiteClient;

public class TolokaClientFactoryImplMock extends TolokaClientFactoryImpl {
    private final TolokaClientMock clientMock;

    public TolokaClientFactoryImplMock() {
        this(new TolokaClientMock());
    }

    public TolokaClientFactoryImplMock(TolokaClientMock clientMock) {
        super("test_oauth");
        this.clientMock = clientMock;
    }

    @Override
    public PoolClient getPoolClient() {
        return clientMock;
    }

    @Override
    public TaskSuiteClient getTaskSuiteClient() {
        return clientMock;
    }

    @Override
    public OperationClient getOperationClient() {
        return clientMock;
    }

    @Override
    public AssignmentClient getAssignmentClient() {
        return clientMock;
    }

    @Override
    public SkillClient getSkillClient() {
        return clientMock;
    }

    @Override
    public TaskClient getTaskClient() {
        return clientMock;
    }

    @Override
    public AggregatedSolutionClient getAggregatedSolutionClient() {
        return clientMock;
    }

    public TolokaClientMock getClientMock() {
        return clientMock;
    }
}
