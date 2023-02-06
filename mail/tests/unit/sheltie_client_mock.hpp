#pragma once

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <src/services/sheltie/sheltie_client.hpp>

namespace collie::tests {

using namespace testing;
using namespace collie::services::sheltie;

struct SheltieClientMock : SheltieClient {
    SheltieClientMock() = default;

    MOCK_METHOD(MapUriVcardRfc, toVcard, (const TaskContextPtr&, const Uid&, const MapUriVcardJson&), (const, override));
    MOCK_METHOD(std::string, fromVcard, (const TaskContextPtr&, const Uid&, std::string), (const, override));
};

} //collie::tests
