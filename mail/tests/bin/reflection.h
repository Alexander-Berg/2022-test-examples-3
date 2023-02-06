#pragma once

#include <yamail/data/reflection/reflection.h>
#include <mail/ymod_queuedb/include/types.h>

YREFLECTION_ADAPT_ENUM(ymod_queuedb::TaskState,
    pending,
    inProgress,
    complete,
    error
)

BOOST_FUSION_ADAPT_STRUCT(ymod_queuedb::Task,
    (std::int64_t, taskId)
    (std::int64_t, uid)
    (std::string, task)
    (std::string, worker)
    (ymod_queuedb::TaskState, state)
    (std::string, service)
    (std::string, taskArgs)
    (std::uint32_t, reassignmentCount)
    (std::uint32_t, tries)
    (std::vector<std::string>, tryNotices)
    (std::time_t, created)
    (std::time_t, processingDate)
)

namespace ymod_queuedb {

struct AddTaskResult {
    TaskId taskId;
};

}

BOOST_FUSION_ADAPT_STRUCT(ymod_queuedb::AddTaskResult,
    (std::int64_t, taskId)
)
