#pragma once

#include <iostream>

#include <ymod_webserver/response.h>
#include <ymod_webserver/request.h>
#include <yamail/data/serialization/json_writer.h>

#include <mail/ymod_queuedb/include/queue.h>
#include "reflection.h"


namespace queuedb_api {
using namespace ymod_queuedb;

inline std::optional<std::string> parseOptionalArg(ymod_webserver::request_ptr req, const std::string& name) {
    if (auto iter = req->url.params.find(name); iter != req->url.params.end()) {
        return std::make_optional(iter->second);
    } else {
        return std::nullopt;
    }
}

inline std::string parseArg(ymod_webserver::request_ptr req, const std::string& name) {
    if (auto iter = req->url.params.find(name); iter != req->url.params.end()) {
        return iter->second;
    }
    throw std::runtime_error(name + " param missing");
}


struct PingParams { };

inline std::string ping(PingParams, boost::asio::yield_context) {
    return R"({"ping": "ok"})";
}


struct AcquireTasksParams {
    QueuePtr queuedb;
    Worker worker;
    TasksLimit tasksLimit;
};

inline AcquireTasksParams getAcquireTasksParams(QueuePtr queuedb, ymod_webserver::response_ptr s) {
    auto req = s->request();
    return AcquireTasksParams {
        .queuedb = queuedb,
        .worker = Worker(parseArg(req, "worker")),
        .tasksLimit = TasksLimit(std::stoi(parseArg(req, "tasks_limit")))
    };
}

inline std::string acquireTasks(AcquireTasksParams params, boost::asio::yield_context yield) {
    const auto yy = io_result::make_yield_context(yield);
    auto tasks = params.queuedb->acquireTasks(params.worker, params.tasksLimit, ymod_queuedb::RequestId("test"), yy);
    return yamail::data::serialization::toJson(tasks).str();
}


struct AddTaskParams {
    QueuePtr queuedb;
    Uid uid;
    TaskType task;
    TaskArgs taskArgs;
    Timeout timeout;
};

inline AddTaskParams getAddTaskParams(QueuePtr queuedb, ymod_webserver::response_ptr s) {
    auto req = s->request();
    return AddTaskParams {
        .queuedb = queuedb,
        .uid = Uid(std::stoll(parseArg(req, "uid"))),
        .task = TaskType(parseArg(req, "task")),
        .taskArgs = TaskArgs(parseOptionalArg(req, "task_args")),
        .timeout = Timeout(std::chrono::seconds(stoll(parseArg(req, "timeout"))))
    };
}

inline std::string addTask(AddTaskParams params, boost::asio::yield_context yield) {
    const auto yy = io_result::make_yield_context(yield);
    auto taskId = params.queuedb->addTask(params.uid, params.task, params.taskArgs, params.timeout, ymod_queuedb::RequestId("test"), yy);
    return yamail::data::serialization::toJson(AddTaskResult{taskId}).str();
}


struct CompleteTaskParams {
    QueuePtr queuedb;
    TaskId taskId;
    Worker worker;
};

inline CompleteTaskParams getCompleteTaskParams(QueuePtr queuedb, ymod_webserver::response_ptr s) {
    auto req = s->request();
    return CompleteTaskParams {
        .queuedb = queuedb,
        .taskId = TaskId(std::stoll(parseArg(req, "task_id"))),
        .worker = Worker(parseArg(req, "worker"))
    };
}

inline std::string completeTask(CompleteTaskParams params, boost::asio::yield_context yield) {
    const auto yy = io_result::make_yield_context(yield);
    params.queuedb->completeTask(params.taskId, params.worker, ymod_queuedb::RequestId("test"), yy);
    return R"({"result": "ok"})";
}


struct FailTaskParams {
    QueuePtr queuedb;
    TaskId taskId;
    Worker worker;
    Reason reason;
    MaxRetries maxRetries;
    Delay delay;
};

inline FailTaskParams getFailTaskParams(QueuePtr queuedb, ymod_webserver::response_ptr s) {
    auto req = s->request();
    return FailTaskParams {
        .queuedb = queuedb,
        .taskId = TaskId(std::stoll(parseArg(req, "task_id"))),
        .worker = Worker(parseArg(req, "worker")),
        .reason = Reason(parseArg(req, "reason")),
        .maxRetries = MaxRetries(std::stoi(parseArg(req, "max_retries"))),
        .delay = Delay(std::chrono::seconds(stoll(parseArg(req, "delay"))))
    };
}

inline std::string failTask(FailTaskParams params, boost::asio::yield_context yield) {
    const auto yy = io_result::make_yield_context(yield);
    params.queuedb->failTask(params.taskId, params.worker, params.reason, params.maxRetries, params.delay, ymod_queuedb::RequestId("test"), yy);
    return R"({"result": "ok"})";
}


struct DelayTaskParams {
    QueuePtr queuedb;
    TaskId taskId;
    Worker worker;
    Delay delay;
};

inline DelayTaskParams getDelayTaskParams(QueuePtr queuedb, ymod_webserver::response_ptr s) {
    auto req = s->request();
    return DelayTaskParams {
        .queuedb = queuedb,
        .taskId = TaskId(std::stoll(parseArg(req, "task_id"))),
        .worker = Worker(parseArg(req, "worker")),
        .delay = Delay(std::chrono::seconds(stoll(parseArg(req, "delay"))))
    };
}

inline std::string delayTask(DelayTaskParams params, boost::asio::yield_context yield) {
    const auto yy = io_result::make_yield_context(yield);
    params.queuedb->delayTask(params.taskId, params.worker, params.delay, ymod_queuedb::RequestId("test"), yy);
    return R"({"result": "ok"})";
}


struct RefreshTaskParams {
    QueuePtr queuedb;
    TaskId taskId;
    Worker worker;
};

inline RefreshTaskParams getRefreshTaskParams(QueuePtr queuedb, ymod_webserver::response_ptr s) {
    auto req = s->request();
    return RefreshTaskParams {
        .queuedb = queuedb,
        .taskId = TaskId(std::stoll(parseArg(req, "task_id"))),
        .worker = Worker(parseArg(req, "worker"))
    };
}

inline std::string refreshTask(RefreshTaskParams params, boost::asio::yield_context yield) {
    const auto yy = io_result::make_yield_context(yield);
    params.queuedb->refreshTask(params.taskId, params.worker, ymod_queuedb::RequestId("test"), yy);
    return R"({"result": "ok"})";
}

}
