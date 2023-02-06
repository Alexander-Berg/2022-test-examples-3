#pragma once

#include "printers.h"
#include <src/logic/job_finder.h>
#include <gmock/gmock.h>

namespace doberman {
namespace testing {

using namespace ::testing;

using logic::HostName;
using logic::FindJobParams;

struct JobFinderAccessMock {
    auto makeContext() { return int();}
    using Ctx = int;
    MOCK_METHOD(std::vector<ShardId>, getShards, (Ctx), (const));
    MOCK_METHOD(boost::optional<Job>, askForJob, (Ctx, ShardId, FindJobParams), (const));
};

} // namespace test

inline bool operator == (const Job& lhs, const Job& rhs) {
    return lhs.workerId == rhs.workerId && lhs.shardId == rhs.shardId;
}

} // namespace doberman
