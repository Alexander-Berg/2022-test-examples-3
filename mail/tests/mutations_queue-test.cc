#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <internal/mutations_queue.h>

namespace {
using namespace testing;
using user_journal::MutationsQueueAlgo;

struct MutationsQueueTest : public Test {
    typedef MutationsQueueAlgo::TskvRow MutationArray;
    typedef MutationsQueueAlgo::TskvRowsChunk Chunk;

    MutationsQueueTest()
    : maxChunkSize(3), maxQueueSize(5), queue(chunks, maxChunkSize, maxQueueSize),
      fullChunk(maxChunkSize, batchMutation) {}

    std::size_t maxChunkSize;
    std::size_t maxQueueSize;
    MutationsQueueAlgo::Queue chunks;
    MutationsQueueAlgo queue;
    MutationArray mutation;
    MutationArray batchMutation;
    Chunk fullChunk;
};


TEST_F(MutationsQueueTest, MutationsQueueAlgo_constructor_startsNewChunk) {
    EXPECT_EQ(chunks.size(), 1u);
}

TEST_F(MutationsQueueTest, MutationsQueueAlgo_addIfSuccess_returnsTrue) {
    MutationArray dummy;
    EXPECT_TRUE(queue.add(dummy));
}

TEST_F(MutationsQueueTest, MutationsQueueAlgo_addMoreThanMaxChunkSizeEntries_startsNewChunk) {
    MutationArray dummy;
    queue.add(dummy);
    queue.add(dummy);
    queue.add(dummy);
    queue.add(dummy);
    EXPECT_EQ(chunks.size(), 2u);
}

TEST_F(MutationsQueueTest, MutationsQueueAlgo_addMaxChunkSizeEntries_doNotStartsNewChunk) {
    MutationArray dummy;
    queue.add(dummy);
    queue.add(dummy);
    queue.add(dummy);
    EXPECT_EQ(chunks.size(), 1u);
}

TEST_F(MutationsQueueTest, MutationsQueueAlgo_addWithFullLastChunkAndMaxQueueSize_returnsFalse) {
    chunks.push(fullChunk);
    chunks.push(fullChunk);
    chunks.push(fullChunk);
    chunks.push(fullChunk);
    EXPECT_FALSE(queue.add(mutation));
}

TEST_F(MutationsQueueTest, MutationsQueueAlgo_addWithChunksCountGetMaxQueueSize_doNotStartNewChunk) {
    chunks.push(fullChunk);
    chunks.push(fullChunk);
    chunks.push(fullChunk);
    chunks.push(fullChunk);
    queue.add(mutation);
    EXPECT_EQ(chunks.size(), maxQueueSize);
}

TEST_F(MutationsQueueTest, MutationsQueueAlgo_getWithChunksCountMoreThanOne_reducesCountOfChunksByOne) {
    chunks.push(Chunk());
    chunks.push(Chunk());
    EXPECT_EQ(chunks.size(), 3u);
    Chunk chunk;
    queue.get(chunk);
    EXPECT_EQ(chunks.size(), 2u);
}

TEST_F(MutationsQueueTest, MutationsQueueAlgo_getWithChunksCountMoreThanOne_returnsTrue) {
    chunks.push(Chunk());
    chunks.push(Chunk());
    Chunk chunk;
    EXPECT_TRUE(queue.get(chunk));
}

TEST_F(MutationsQueueTest, MutationsQueueAlgo_getWithOnlyOneChunkInQueue_createsNewOne) {
    Chunk chunk;
    queue.get(chunk);
    EXPECT_EQ(chunks.size(), 1u);
}

TEST_F(MutationsQueueTest, MutationsQueueAlgo_getWithOnlyOneChunkInQueue_returnsFalse) {
    Chunk chunk;
    EXPECT_FALSE(queue.get(chunk));
}

}
