#pragma once

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/ymod_queuedb/include/queue.h>


namespace ymod_queuedb {

struct MockQueue: public Queue {
    MOCK_METHOD(void, acquireTasksAsync, (const Worker&, TasksLimit, const RequestId&, OnAcquireTasks), (const, override));
    MOCK_METHOD(void, addTaskAsync, (Uid, const TaskType&, const TaskArgs&, Timeout, const RequestId&,  OnAddTask), (const, override));
    MOCK_METHOD(void, completeTaskAsync, (TaskId, const Worker&, const RequestId&, OnExecute), (const, override));
    MOCK_METHOD(void, failTaskAsync, (TaskId, const Worker&, const Reason&, MaxRetries, Delay, const RequestId&, OnExecute), (const, override));
    MOCK_METHOD(void, delayTaskAsync, (TaskId, const Worker&, Delay, const RequestId&, OnExecute), (const, override));
    MOCK_METHOD(void, refreshTaskAsync, (TaskId, const Worker&, const RequestId&, OnExecute), (const, override));
};

}
