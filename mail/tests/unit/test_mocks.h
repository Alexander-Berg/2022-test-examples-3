#pragma once

#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <string>
#include <memory>
#include <internal/logger/logger.h>
#include <sharpei_client/sharpei_client.h>
#include "logger_mock.h"

namespace york {
namespace tests {

using Shard = ::sharpei::client::Shard;
using ShardId = Shard::Id;
using ShardMap = std::unordered_map<ShardId, Shard>;

template <typename SyncOrCoro>
struct SharpeiClientMock {
    MOCK_CONST_METHOD1_T(stat, ShardMap(SyncOrCoro));
};

struct RunStatusMock {
    MOCK_METHOD(bool, check, (), (const));
    operator bool() const {
        return check();
    }
};

} //namespace tests
} //namespace york
