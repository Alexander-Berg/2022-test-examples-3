#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <ymod_taskmaster/marshalling.hpp>
#include <internal/chunks_storage_pg.hpp>
#include <mail/mops/ymod_taskmaster/tests/include/sequence_response.h>
#include <mail/mops/ymod_taskmaster/ymod_db/include/internal/errors.h>

namespace {

using namespace ymod_taskmaster;
using namespace ymod_taskmaster::testing;
using namespace ::testing;

class MockRepository: public Repository {
public:
    MOCK_METHOD(void, asyncAddTask, (const User& u, const ChunksData& c, const TaskInfo& t, const ContextPtr& ctx, onExecute cb), (const, override));
    MOCK_METHOD(void, asyncReadChunks, (const User& u, const ChunkIds& chIds, const ContextPtr& ctx, onReadChunks cb), (const, override));
    MOCK_METHOD(void, asyncRemoveChunk, (const User& u, const ChunkId& chId, const ContextPtr& ctx, onExecute cb), (const, override));
    MOCK_METHOD(void, asyncCanAddTask, (const User& u, size_t tasksLimit, const ContextPtr& ctx, onBoolean cb), (const, override));
    MOCK_METHOD(void, asyncChooseChunkIds, (const User& u, size_t limit, const ContextPtr& ctx, onChooseChunkIds cb), (const, override));
    MOCK_METHOD(void, asyncReadChunksByMids, (const User& u, const Mids& mids, const ContextPtr& ctx, onReadChunks cb), (const, override));
    MOCK_METHOD(void, asyncChooseUsers, (size_t limit, const ContextPtr& ctx, onChooseUsers cb), (const, override));
    MOCK_METHOD(void, asyncHasTasks, (const User& u, const ContextPtr& ctx, onBoolean cb), (const, override));
    MOCK_METHOD(void, asyncUserStat, (const User& u, const ContextPtr& ctx, onReadTasks cb), (const, override));
    MOCK_METHOD(void, asyncReadTask, (const User& u, const TaskId& t, const ContextPtr& ctx, onReadTasks cb), (const, override));
    MOCK_METHOD(void, asyncAcquireLock, (const User& u, const std::chrono::microseconds& timeout, const std::string& launchId, const ContextPtr& ctx, onBoolean cb), (const, override));
    MOCK_METHOD(void, asyncReleaseLock, (const User& u, const std::string& launchId, const ContextPtr& ctx, onBoolean cb), (const, override));
};

class FakeHandler {
public:
    FakeHandler()
        : handledChunkId_(std::make_shared<ChunkId>())
    {}

    FakeHandler(const FakeHandler&) = default;

    FakeHandler& operator=(const FakeHandler&) = default;

    void operator()(const ChunkData& chunk) {
        *handledChunkId_ = chunk.chunk.id;
    }

    const ChunkId& handledChunkId() const {
        return *handledChunkId_;
    }
private:
    std::shared_ptr<ChunkId> handledChunkId_;
};

class ChunksStoragePgTest: public Test {
public:
    ChunksStoragePgTest()
        : pgRepository_(std::make_shared<StrictMock<MockRepository>>()) {
        actualTaskInfo.version = dataVersion;
        actualTaskInfo.chunksCount = 0;
        actualTaskInfo.creationSecs = 0;
    }
protected:
    void SetUp() override {
        handler_ = FakeHandler();
        ctx = boost::make_shared<Context>();
    }

    std::shared_ptr<StrictMock<MockRepository>> pgRepository_;
    FakeHandler handler_;
    boost::asio::io_service ios;
    TaskInfo actualTaskInfo;
    ContextPtr ctx;
};

TEST_F(ChunksStoragePgTest, processChunk_emptyChunks_handlerNotCalled) {
    boost::asio::io_context io;
    boost::asio::spawn(io, [&](boost::asio::yield_context yield) {
        ChunksStoragePg storage(pgRepository_, ctx, 3, Milliseconds(100), Seconds(0));

        EXPECT_CALL(*pgRepository_, asyncReadChunks(User("uid"), ChunkIds{"chunk-id"}, ctx, _))
            .WillOnce(WithArg<3>(
                Invoke([=] (onReadChunks cb) {
                    responseAsSequence(Chunks(), cb);
                })
            ));

        const auto result = storage.processChunk(User("uid"), "chunk-id", handler_, yield);
        EXPECT_EQ("", handler_.handledChunkId());
        EXPECT_EQ(Chunk(), boost::get<Chunk>(result));
    });
    io.run();
}

TEST_F(ChunksStoragePgTest, processChunk_singleChunk_handleAndRemove) {
    boost::asio::io_context io;
    boost::asio::spawn(io, [&](boost::asio::yield_context yield) {
        ChunksStoragePg storage(pgRepository_, ctx, 3, Milliseconds(100), Seconds(0));

        EXPECT_CALL(*pgRepository_, asyncReadChunks(User("uid"), ChunkIds{"chunk-id"}, ctx, _))
            .WillOnce(WithArg<3>(
                Invoke([=] (onReadChunks cb) {
                    responseAsSequence(Chunks{ Chunk{ ChunkInfo{"chunk-id", Mids()}, actualTaskInfo } }, cb);
                })
            ));
        EXPECT_CALL(*pgRepository_, asyncRemoveChunk(User("uid"), "chunk-id", ctx, _))
            .WillOnce(InvokeArgument<3>());
        const auto result = storage.processChunk(User("uid"), "chunk-id", handler_, yield);
        Chunk expected{ ChunkInfo{ "chunk-id", Mids() }, actualTaskInfo };

        EXPECT_EQ("chunk-id", handler_.handledChunkId());
        EXPECT_EQ(expected, boost::get<Chunk>(result));
    });
    io.run();
}

TEST_F(ChunksStoragePgTest, processChunk_manyChunks_handleFirstAndRemoveIt) {
    boost::asio::io_context io;
    boost::asio::spawn(io, [&](boost::asio::yield_context yield) {
        ChunksStoragePg storage(pgRepository_, ctx, 3, Milliseconds(100), Seconds(0));

        EXPECT_CALL(*pgRepository_, asyncReadChunks(User("uid"), ChunkIds{"chunk-id"}, ctx, _))
            .WillOnce(WithArg<3>(
                Invoke([=] (onReadChunks cb) {
                            responseAsSequence(Chunks{
                        Chunk{ ChunkInfo{ "chunk-id", Mids() }, actualTaskInfo },
                        Chunk{ ChunkInfo{ "chunk-id2", Mids() }, actualTaskInfo }
                    }, cb);
                })
            ));
        EXPECT_CALL(*pgRepository_, asyncRemoveChunk(User("uid"), "chunk-id", ctx, _))
            .WillOnce(InvokeArgument<3>());
        const auto result = storage.processChunk(User("uid"), "chunk-id", handler_, yield);
        Chunk expected{ ChunkInfo{ "chunk-id", Mids() }, actualTaskInfo };

        EXPECT_EQ("chunk-id", handler_.handledChunkId());
        EXPECT_EQ(expected, boost::get<Chunk>(result));
    });
    io.run();
}

TEST_F(ChunksStoragePgTest, processChunk_exceptionInReadChunk_throwPgException) {
    boost::asio::io_context io;
    boost::asio::spawn(io, [&](boost::asio::yield_context yield) {
        ChunksStoragePg storage(pgRepository_, ctx, 3, Milliseconds(100), Seconds(0));

        EXPECT_CALL(*pgRepository_, asyncReadChunks(User("uid"), ChunkIds{"chunk-id"}, ctx, _))
            .WillOnce(Throw(PgException("error")));

        EXPECT_THROW(storage.processChunk(User("uid"), "chunk-id", handler_, yield), PgException);
        EXPECT_EQ("", handler_.handledChunkId());
    });
    io.run();
}

TEST_F(ChunksStoragePgTest, processChunk_exceptionInHandler_throwPgException) {
    boost::asio::io_context io;
    boost::asio::spawn(io, [&](boost::asio::yield_context yield) {
        ChunksStoragePg storage(pgRepository_, ctx, 3, Milliseconds(100), Seconds(0));
        auto handler = [] (const ChunkData&) { throw PgException("error"); };

        EXPECT_CALL(*pgRepository_, asyncReadChunks(User("uid"), ChunkIds{"chunk-id"}, ctx, _))
            .WillOnce(WithArg<3>(
                Invoke([=] (onReadChunks cb) {
                    responseAsSequence(Chunks{ Chunk{ ChunkInfo{ "chunk-id", Mids() }, actualTaskInfo } }, cb);
                })
            ));

        EXPECT_THROW(storage.processChunk(User("uid"), "chunk-id", handler, yield), PgException);
    });
    io.run();
}

TEST_F(ChunksStoragePgTest, processChunk_exceptionInRemoveChunk_retry) {
    boost::asio::io_context io;
    boost::asio::spawn(io, [&](boost::asio::yield_context yield) {
        ChunksStoragePg storage(pgRepository_, ctx, 3, Milliseconds(0), Seconds(0));

        EXPECT_CALL(*pgRepository_, asyncReadChunks(User("uid"), ChunkIds{"chunk-id"}, ctx, _))
            .WillOnce(WithArg<3>(
                Invoke([=] (onReadChunks cb) {
                    responseAsSequence(Chunks{ Chunk{ ChunkInfo{ "chunk-id", Mids() }, actualTaskInfo } }, cb);
                })
            ));
        EXPECT_CALL(*pgRepository_, asyncRemoveChunk(User("uid"), "chunk-id", ctx, _))
            .WillOnce(Throw(PgException("error")))
            .WillOnce(InvokeArgument<3>());

        const auto actual = storage.processChunk(User("uid"), "chunk-id", handler_, yield);
        Chunk expected{ ChunkInfo{ "chunk-id", Mids() }, actualTaskInfo };
        EXPECT_EQ(expected, boost::get<Chunk>(actual));
    });
    io.run();
}

TEST_F(ChunksStoragePgTest, processChunk_exceptionInRemoveChunkMoreThanRetries_returnEmptyChunk) {
    boost::asio::io_context io;
    boost::asio::spawn(io, [&](boost::asio::yield_context yield) {
        ChunksStoragePg storage(pgRepository_, ctx, 3, Milliseconds(0), Seconds(0));

        EXPECT_CALL(*pgRepository_, asyncReadChunks(User("uid"), ChunkIds{"chunk-id"}, ctx, _))
            .WillOnce(WithArg<3>(
                Invoke([=] (onReadChunks cb) {
                    responseAsSequence(Chunks{ Chunk{ ChunkInfo{ "chunk-id", Mids() }, actualTaskInfo } }, cb);
                })
            ));
        EXPECT_CALL(*pgRepository_, asyncRemoveChunk(User("uid"), "chunk-id", ctx, _))
            .Times(3).WillRepeatedly(Throw(PgException("error")));

        const auto actual = storage.processChunk(User("uid"), "chunk-id", handler_, yield);
        Chunk expected{ ChunkInfo{ "chunk-id", Mids() }, actualTaskInfo };
        EXPECT_EQ(expected, boost::get<Chunk>(actual));
    });
    io.run();
}

TEST_F(ChunksStoragePgTest, processChunk_elderUnexpiredChunk_returnTryNextChunk) {
    boost::asio::io_context io;
    boost::asio::spawn(io, [&](boost::asio::yield_context yield) {
        ChunksStoragePg storage(pgRepository_, ctx, 3, Milliseconds(100), Seconds(100));

        TaskInfo taskInfo;
        taskInfo.version = 0;
        taskInfo.creationSecs = toSeconds(now()).count();

        EXPECT_CALL(*pgRepository_, asyncReadChunks(User("uid"), ChunkIds{ "chunk-id" }, ctx, _))
            .WillOnce(WithArg<3>(
                Invoke([=] (onReadChunks cb) {
                    responseAsSequence(Chunks{ Chunk{ ChunkInfo{ "chunk-id", Mids() }, taskInfo } }, cb);
                })
            ));

        const auto result = storage.processChunk(User("uid"), "chunk-id", handler_, yield);
        EXPECT_NO_THROW(boost::get<TryNextChunk>(result));
    });
    io.run();
}

TEST_F(ChunksStoragePgTest, processChunk_newerUnexpiredChunk_returnTryNextChunk) {
    boost::asio::io_context io;
    boost::asio::spawn(io, [&](boost::asio::yield_context yield) {
        ChunksStoragePg storage(pgRepository_, ctx, 3, Milliseconds(100), Seconds(100));

        TaskInfo taskInfo;
        taskInfo.version = 100500;
        taskInfo.creationSecs = toSeconds(now()).count();

        EXPECT_CALL(*pgRepository_, asyncReadChunks(User("uid"), ChunkIds{ "chunk-id" }, ctx, _))
            .WillOnce(WithArg<3>(
                Invoke([=] (onReadChunks cb) {
                    responseAsSequence(Chunks{ Chunk{ ChunkInfo{ "chunk-id", Mids() }, taskInfo } }, cb);
                })
            ));

        const auto result = storage.processChunk(User("uid"), "chunk-id", handler_, yield);
        EXPECT_NO_THROW(boost::get<TryNextChunk>(result));
    });
    io.run();
}

TEST_F(ChunksStoragePgTest, processChunk_elderExpiredChunk_returnTryNextChunk) {
    boost::asio::io_context io;
    boost::asio::spawn(io, [&](boost::asio::yield_context yield) {
        ChunksStoragePg storage(pgRepository_, ctx, 3, Milliseconds(100), Seconds(0));

        TaskInfo taskInfo;
        taskInfo.version = 0;
        taskInfo.creationSecs = 0;

        EXPECT_CALL(*pgRepository_, asyncReadChunks(User("uid"), ChunkIds{ "chunk-id" }, ctx, _))
            .WillOnce(WithArg<3>(
                Invoke([=] (onReadChunks cb) {
                    responseAsSequence(Chunks{ Chunk{ ChunkInfo{ "chunk-id", Mids() }, taskInfo } }, cb);
                })
            ));
        EXPECT_CALL(*pgRepository_, asyncRemoveChunk(User("uid"), "chunk-id", ctx, _))
            .WillOnce(InvokeArgument<3>());

        const auto result = storage.processChunk(User("uid"), "chunk-id", handler_, yield);
        EXPECT_NO_THROW(boost::get<TryNextChunk>(result));
    });
    io.run();
}

TEST_F(ChunksStoragePgTest, processChunk_newerExpiredChunk_returnTryNextChunk) {
    boost::asio::io_context io;
    boost::asio::spawn(io, [&](boost::asio::yield_context yield) {
        ChunksStoragePg storage(pgRepository_, ctx, 3, Milliseconds(100), Seconds(100));

        TaskInfo taskInfo;
        taskInfo.version = 100500;
        taskInfo.creationSecs = 0;

        EXPECT_CALL(*pgRepository_, asyncReadChunks(User("uid"), ChunkIds{ "chunk-id" }, ctx, _))
            .WillOnce(WithArg<3>(
                Invoke([=] (onReadChunks cb) {
                    responseAsSequence(Chunks{ Chunk{ ChunkInfo{ "chunk-id", Mids() }, taskInfo } }, cb);
                })
            ));

        const auto result = storage.processChunk(User("uid"), "chunk-id", handler_, yield);
        EXPECT_NO_THROW(boost::get<TryNextChunk>(result));
    });
    io.run();
}

} // namespace
