#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <ymod_taskmaster/marshalling.hpp>
#include <yamail/data/serialization/json_writer.h>

namespace {

using namespace testing;
using namespace yamail::data::serialization;

TEST(TaskInfo, serializeTest) {
    const auto actual = JsonWriter(ymod_taskmaster::ChunkData{}).result();
    const auto expected = R"json({"chunk":{"id":"","mids":[]},"task":{"version":2,"taskData":{"commonParams":{"uid":"","login":"","karma":"",)json"
                          R"json("karmaStatus":"","remoteIp":"","userAgent":"","sessionInfo":"","yandexUidCookie":"","iCookie":"","source":"",)json"
                          R"json("clientType":"","clientVersion":"","connectionId":"","requestId":"","testBuckets":"","enabledTestBuckets":""},)json"
                          R"json("type":-1,"id":"","taskGroupId":"","source":{"mids":[],"tids":[],"fid":"","lid":"","age":{"days":0},"subject":"",)json"
                          R"json("from":"","tabName":"","fromMid":"","limit":""},"resolveMids":false,"nestedTaskId":""},"creationSecs":0,"chunksCount":0}})json";

    EXPECT_EQ(actual, expected) << "\n"
        "It seems you've changed TaskInfo or it's members.\n"
        "Please make sure that you've updated <dataVersion>.\n"
        "Otherwise it can lead to errors: old tasks will stuck forever, because of wrong TaskInfo format.\n\n";
}

}
