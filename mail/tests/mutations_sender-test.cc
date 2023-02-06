#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <internal/mutations_sender.h>
#include <user_journal/connection_info.h>

namespace {
using namespace testing;
using user_journal::MutationsQueue;
using user_journal::MutationsSender;


struct MutationsQueueMock : public MutationsQueue {
    MOCK_METHOD(bool, add, ( TskvRow mutations ), (override));
    MOCK_METHOD(bool, get, ( TskvRowsChunk & batch ), (override));
};

struct FileWriterMock {
    MOCK_METHOD(void, doWrite, ( const std::string& ), ());
};

struct MutationsSenderTest : public Test {
    MutationsSenderTest()
    : queue(new MutationsQueueMock),
      sender(queue,
             user_journal::noLogging,
             user_journal::FileWriter([this](const std::string& s){ fileWriter.doWrite(s); })) {
    }
    typedef MutationsSender::TskvRow MutationArray;
    typedef MutationsSender::TskvRowsChunk Chunk;

    boost::shared_ptr<MutationsQueueMock> queue;
    FileWriterMock fileWriter;
    MutationsSender sender;
};


TEST_F(MutationsSenderTest, poll_withEmptyQueue_callNothing) {
    EXPECT_CALL(*queue, get(_)).WillOnce(Return(false));
    sender.poll();
}

TEST_F(MutationsSenderTest, poll_withQueueWithEmptyChunk_callNothing) {
    EXPECT_CALL(*queue, get(_)).WillOnce( DoAll(
            SetArgReferee<0>(Chunk()),
            Return(false)));
    sender.poll();
}

TEST_F(MutationsSenderTest, poll_withQueueWithSingleChunk_callMutateRows) {
    Chunk chunk;
    MutationArray batch;
    chunk.push_back(batch);
    EXPECT_CALL(*queue, get(_)).WillOnce( DoAll(
            SetArgReferee<0>(chunk),
            Return(false)));
    EXPECT_CALL(fileWriter, doWrite(_)).WillOnce(Return());
    sender.poll();
}

TEST_F(MutationsSenderTest, poll_withQueueWithTwoChunks_callMutateRowsTwice) {
    Chunk chunk;
    MutationArray batch;
    chunk.push_back(batch);
    Sequence s;
    EXPECT_CALL(*queue, get(_)).InSequence(s).WillOnce( DoAll(
            SetArgReferee<0>(chunk),
            Return(true)));
    EXPECT_CALL(*queue, get(_)).InSequence(s).WillOnce( DoAll(
            SetArgReferee<0>(chunk),
            Return(false)));
    EXPECT_CALL(fileWriter, doWrite(_)).Times(2).WillRepeatedly(Return());
    sender.poll();
}

}
