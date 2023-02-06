#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <internal/chunk.hpp>

namespace {

using namespace ymod_taskmaster;
using namespace testing;

TEST(ChunkTest, parseNodeName_returnSplittedNameAndFilter) {
    const ChunkPathInfo chunkPathInfo("1", "xxx_101");
    EXPECT_EQ("xxx", chunkPathInfo.chunkId());
    EXPECT_EQ("101", chunkPathInfo.filter());
}

TEST(ChunkTest, chunkNodeName_concatNameAndFilter) {
    const ChunkPathInfo chunk("1", "xxx", "1010");
    EXPECT_EQ("xxx_1010", chunk.chunkNodeName());
}

} // namespace
