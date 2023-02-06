#pragma once

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/ymod_queuedb_worker/include/internal/run_loop.h>
#include <mail/ymod_queuedb_worker/include/internal/error.h>
#include <mail/ymod_queuedb_worker/include/task_control.h>

#include <mail/ymod_queuedb_worker/tests/mock_queue.h>

using namespace ::testing;
namespace ph = std::placeholders;

namespace ymod_queuedb {

struct MockedLoop: public Loop {
    using Loop::createRefreshTaskCoro;
    using Loop::acquireTasks;
    using Loop::processTask;
    using Loop::Loop;
};

struct BaseTest: public Test {
    const std::string requestId;
    const Timeout refreshTimeout;
    const Delay delay;
    const ymod_queuedb::Worker worker;
    const TasksLimit tasksLimit;
    std::shared_ptr<yplatform::reactor> reactor;

    BaseTest()
        : requestId("requestId")
        , refreshTimeout(std::chrono::seconds(1))
        , delay(std::chrono::seconds(2))
        , worker("worker")
        , tasksLimit(1)
    { }

    void SetUp() override {
        reactor = std::make_shared<yplatform::reactor>();
        reactor->init(1, 1);
    }

    template<class Fn>
    void spawn(Fn fn) {
        boost::asio::spawn(*reactor->io(), fn);
        reactor->io()->run();
    }
};

struct RunLoopBaseTest: public BaseTest {
    std::shared_ptr<StrictMock<MockQueue>> queue;
    TaskContextHolder contextHolder = TaskContextHolder();
    std::shared_ptr<TaskId> id;
    std::shared_ptr<MockedLoop> loop;

    RunLoopBaseTest()
        : id(std::make_shared<TaskId>(1))
    { }

    virtual TaskHandlersMap taskHandlersMap() const {
        return TaskHandlersMap();
    }

    void SetUp() override {
        BaseTest::SetUp();

        queue = std::make_shared<StrictMock<MockQueue>>();

        loop = std::make_shared<MockedLoop>(
            taskHandlersMap(), reactor, queue, std::chrono::milliseconds{100},
            getWorkerAccessLogger(""), contextHolder, getLogger(""), worker
        );
    }
};

struct WithTaskAndTypeTest: public RunLoopBaseTest {
    TaskType taskType;
    Task task;

    WithTaskAndTypeTest()
        : taskType("taskType")
        , task({ .taskId=*id, .task=taskType })
    { }
};

struct WithMockedRefreshCoroTest: public WithTaskAndTypeTest {
    yamail::expected<void> result;
    TaskHandlerInfo handler;

    WithMockedRefreshCoroTest()
        : handler(TaskHandlerInfo {
            .maxRetries=MaxRetries(3),
            .onFail=delay,
            .onDelay=delay,
            .handler=[this] (const auto&, auto, auto, auto) { return result; }
        })
    { }

    void SetUp() override {
        WithTaskAndTypeTest::SetUp();
        EXPECT_CALL(*queue, refreshTaskAsync(*id, worker, _, _))
        .WillRepeatedly(Invoke([] (auto, const auto&, const auto&, OnExecute cb) {
            cb();
        }));
    }
};

struct WithHandlersMapTest: public WithMockedRefreshCoroTest {
    TaskHandlersMap handlersMap;

    WithHandlersMapTest()
        : handlersMap({{ taskType, handler }})
    { }

    virtual TaskHandlersMap taskHandlersMap() const {
        return handlersMap;
    }
};

}
