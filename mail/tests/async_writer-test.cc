#include <internal/async_writer.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <internal/async_writer.h>
#include <user_journal/connection_info.h>

namespace {
using namespace testing;

using user_journal::AsyncWriter;
using user_journal::AccessTraits;
using user_journal::MutationsQueue;
using user_journal::noProfiling;
using user_journal::noLogging;
using user_journal::Date;
using user_journal::Target;
using user_journal::Operation;
using user_journal::logging::LogPtr;

struct MutationsQueueMock : public MutationsQueue {
    MOCK_METHOD(bool, add, ( TskvRow mutations ), (override));
    MOCK_METHOD(bool, get, ( TskvRowsChunk & batch ), (override));
};

struct LogMock : public user_journal::logging::Log {
    MOCK_METHOD(void, warning, ( const std::string & uid, const std::string & method,
            const std::string & message, int code ), (override));
    MOCK_METHOD(void, error, ( const std::string & uid, const std::string & method,
            const std::string & message, int code ), (override));
    MOCK_METHOD(void, notice, ( const std::string & uid, const std::string & method,
            const std::string & message ), (override));
    MOCK_METHOD(void, debug, ( const std::string & uid, const std::string & method,
            const std::string & message ), (override));
};

struct AsyncWriterTest : public Test {
    AsyncWriterTest() :
        queue(new MutationsQueueMock()),
        accessTraits(noProfiling, noLogging, "tableName", "tskv_format"),
        writer(queue, accessTraits, std::locale()) {
    }

    static Date date() {
        return Date(Date::duration(42042));
    }

    struct Entry : public user_journal::Entry {
        void map(const user_journal::Mapper &m) const override {
            m.mapValue(AsyncWriterTest::date(), "date");
            m.mapValue(std::string("zzz"), "module");
            m.mapValue(Target(Target::message), "target");
            m.mapValue(Operation(Operation::deletee), "operation");
            m.mapValue(111, "affected");
            m.mapValue(true, "hidden");
            m.mapValue(std::vector<std::string>{"11", "12"}, "fids");
            m.mapValue(user_journal::unixtime(AsyncWriterTest::date()), "unixtime");
        }
    };

    boost::shared_ptr<MutationsQueueMock> queue;
    AccessTraits accessTraits;
    AsyncWriter writer;
};

TEST_F(AsyncWriterTest, write_withLoggerHandleOverflowQueue_withLoggingEntry) {
    EXPECT_CALL(*queue, add(_)).WillOnce(Return(false));
    boost::shared_ptr<LogMock> logger(new LogMock());
    EXPECT_CALL(*logger, error("1", "AsyncWriter::write", _, 0));
    AsyncWriter writer(queue, {noProfiling, logger, "", ""}, std::locale());
    writer.write("1", Entry() );
}

TEST_F(AsyncWriterTest, write_withoutLoggerHandleOverflowQueue_throwsException) {
    EXPECT_CALL(*queue, add(_)).WillOnce(Return(false));
    ASSERT_THROW(writer.write("1", Entry() ), std::overflow_error);
}

TEST_F(AsyncWriterTest, write_callsQueueAdd_withRightMutations) {
    std::ostringstream s;
    s << ytskv::utf_value(ytskv::with_endl)
        << ytskv::attr("uid", "00000000000000000001")
        << ytskv::attr("tskv_format", "tskv_format")
        << ytskv::attr("tableName", "tableName")
        << ytskv::attr("date", "42042")
        << ytskv::attr("module", "zzz")
        << ytskv::attr("target", "message")
        << ytskv::attr("operation", "delete")
        << ytskv::attr("affected", "111")
        << ytskv::attr("hidden", "1")
        << ytskv::attr("fids", "11,12")
        << ytskv::attr("unixtime", "42");

    EXPECT_CALL(*queue, add(s.str())).WillOnce(Return(true));

    writer.write("1", Entry());
}

}
