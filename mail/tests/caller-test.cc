#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <pgg/database/fallback.h>
#include <pgg/query/ids.h>
#include <pgg/query/boundaries.h>
#include <pgg/request_executor.h>
#include <pgg/database/performer/error_handler_invoker.h>

namespace pgg::database {

bool operator==(const pgg::database::EndpointQuery& lhs, const pgg::database::EndpointQuery& rhs) {
    return lhs.type == rhs.type && lhs.forceMaster == rhs.forceMaster && lhs.number == rhs.number;
}

std::ostream& operator<<(std::ostream& ss, const pgg::database::EndpointQuery& e) {
    return ss << "type=" << e.type.toString() << " forceMaster=" << e.forceMaster << " number=" << e.number;
}

} // namespace pgg::database

namespace {

using namespace testing;
using namespace pgg;
using namespace pgg::query;
using namespace pgg::database;
using namespace pgg::database::fallback;
using namespace pgg::database::fallback::details;

using error_code = pgg::database::error_code;

struct DatabaseMock : public Database {
    MOCK_METHOD(void, request, (const query::Query&, RequestHandler), (const, override));
    MOCK_METHOD(void, fetch, (const query::Query&, FetchHandler), (const, override));
    MOCK_METHOD(void, update, (const query::Query&, UpdateHandler), (const, override));
    MOCK_METHOD(void, execute, (const query::Query&, ExecuteHandler), (const, override));
    MOCK_METHOD(void, withinConnection, (ConnectionHandler), (const, override));
};

struct QueryMock : public Query {
    MOCK_METHOD(Text, text, (), (const, override));
    MOCK_METHOD(void, mapValues, (const Mapper&), (const, override));
    MOCK_METHOD(const char*, name, (), (const, override));
    MOCK_METHOD(Milliseconds, timeout, (), (const, override));
    MOCK_METHOD(bool, debug, (), (const, override));
    MOCK_METHOD(EndpointType, endpointType, (), (const, override));
    MOCK_METHOD(QueryPtr, clone, (), (const, override));
};

struct ConnProviderMock {
    MOCK_METHOD(DatabasePtr, invoke, (EndpointQuery, LogError), (const));
};

struct ConnectionProviderMock {
    ConnectionProviderMock(): mock(std::make_shared<StrictMock<ConnProviderMock>>()) {}

    DatabasePtr operator()(EndpointQuery query, LogError logError) const {
        return mock->invoke(std::move(query), std::move(logError));
    }

    std::shared_ptr<StrictMock<ConnProviderMock>> mock;
};

struct DummyLog: public logging::Log {
    void log(const logging::Level, const logging::Method&, const logging::Message&, logging::Attributes) override {}
    bool applicable(const logging::Level) override {return false;};
};

struct CallerTest : Test {
    ConnectionProviderMock connectionProviderMock;
    Query::QueryPtr q = boost::make_shared<QueryMock>();
    LogPtr log = std::make_shared<DummyLog>();
    boost::shared_ptr<StrictMock<DatabaseMock>> db = boost::make_shared<StrictMock<DatabaseMock>>();
};

TEST_F(CallerTest, should_read_no_lag_replica_then_master_then_replica) {
    StrategyProvider strategyProvider((strategy::ReadNoLagReplicaThenMasterThenReplica()));
    Caller caller((strategy::ReadNoLagReplicaThenMasterThenReplica()));

    const auto& cp = connectionProviderMock.mock;

    const database::error_code brokenPipe{mail_errors::error_code(boost::asio::error::broken_pipe)};
    const database::error_code endpointNumberOverflow{pgg::error_code(error::endpointNumberOverflow, "")};

    InSequence s;

    EXPECT_CALL(*cp, invoke(EndpointQuery(EndpointType::noLagReplica, false, 0), _)).WillOnce(Return(db));
    EXPECT_CALL(*db, request(_, _)).WillOnce(InvokeArgument<1>(brokenPipe, apq::cursor()));
    EXPECT_CALL(*cp, invoke(EndpointQuery(EndpointType::noLagReplica, false, 1), _)).WillOnce(Return(db));
    EXPECT_CALL(*db, request(_, _)).WillOnce(InvokeArgument<1>(endpointNumberOverflow, apq::cursor()));

    EXPECT_CALL(*cp, invoke(EndpointQuery(EndpointType::master, false, 0), _)).WillOnce(Return(db));
    EXPECT_CALL(*db, request(_, _)).WillOnce(InvokeArgument<1>(brokenPipe, apq::cursor()));
    EXPECT_CALL(*cp, invoke(EndpointQuery(EndpointType::master, true, 0), _)).WillOnce(Return(db));
    EXPECT_CALL(*db, request(_, _)).WillOnce(InvokeArgument<1>(brokenPipe, apq::cursor()));

    EXPECT_CALL(*cp, invoke(EndpointQuery(EndpointType::lagReplica, false, 0), _)).WillOnce(Return(db));
    EXPECT_CALL(*db, request(_, _)).WillOnce(InvokeArgument<1>(brokenPipe, apq::cursor()));
    EXPECT_CALL(*cp, invoke(EndpointQuery(EndpointType::lagReplica, false, 1), _)).WillOnce(Return(db));
    EXPECT_CALL(*db, request(_, _)).WillOnce(InvokeArgument<1>(endpointNumberOverflow, apq::cursor()));

    caller.run<Request>(connectionProviderMock, q, [] (database::error_code, const auto&) {}, log);

    EXPECT_TRUE(testing::Mock::VerifyAndClearExpectations(q.get()));
    EXPECT_TRUE(testing::Mock::VerifyAndClearExpectations(db.get()));
    EXPECT_TRUE(testing::Mock::VerifyAndClearExpectations(connectionProviderMock.mock.get()));
}

TEST_F(CallerTest, should_read_master_then_replica) {
    StrategyProvider strategyProvider((strategy::ReadMasterThenReplica()));
    Caller caller((strategy::ReadMasterThenReplica()));

    const auto& cp = connectionProviderMock.mock;

    const database::error_code brokenPipe((mail_errors::error_code(boost::asio::error::broken_pipe)));
    const database::error_code endpointNumberOverflow((pgg::error_code(error::endpointNumberOverflow, "")));

    InSequence s;

    EXPECT_CALL(*cp, invoke(EndpointQuery(EndpointType::master, false, 0), _)).WillOnce(Return(db));
    EXPECT_CALL(*db, request(_, _)).WillOnce(InvokeArgument<1>(brokenPipe, apq::cursor()));
    EXPECT_CALL(*cp, invoke(EndpointQuery(EndpointType::master, true, 0), _)).WillOnce(Return(db));
    EXPECT_CALL(*db, request(_, _)).WillOnce(InvokeArgument<1>(brokenPipe, apq::cursor()));

    EXPECT_CALL(*cp, invoke(EndpointQuery(EndpointType::replica, false, 0), _)).WillOnce(Return(db));
    EXPECT_CALL(*db, request(_, _)).WillOnce(InvokeArgument<1>(brokenPipe, apq::cursor()));
    EXPECT_CALL(*cp, invoke(EndpointQuery(EndpointType::replica, false, 1), _)).WillOnce(Return(db));
    EXPECT_CALL(*db, request(_, _)).WillOnce(InvokeArgument<1>(endpointNumberOverflow, apq::cursor()));

    caller.run<Request>(connectionProviderMock, q, [] (database::error_code, const auto&) {}, log);

    EXPECT_TRUE(testing::Mock::VerifyAndClearExpectations(q.get()));
    EXPECT_TRUE(testing::Mock::VerifyAndClearExpectations(db.get()));
    EXPECT_TRUE(testing::Mock::VerifyAndClearExpectations(connectionProviderMock.mock.get()));
}

TEST_F(CallerTest, should_read_replica_then_master) {
    StrategyProvider strategyProvider((strategy::ReadReplicaThenMaster()));
    Caller caller((strategy::ReadReplicaThenMaster()));

    const auto& cp = connectionProviderMock.mock;

    const database::error_code brokenPipe((mail_errors::error_code(boost::asio::error::broken_pipe)));
    const database::error_code endpointNumberOverflow((pgg::error_code(error::endpointNumberOverflow, "")));

    InSequence s;

    EXPECT_CALL(*cp, invoke(EndpointQuery(EndpointType::replica, false, 0), _)).WillOnce(Return(db));
    EXPECT_CALL(*db, request(_, _)).WillOnce(InvokeArgument<1>(brokenPipe, apq::cursor()));
    EXPECT_CALL(*cp, invoke(EndpointQuery(EndpointType::replica, false, 1), _)).WillOnce(Return(db));
    EXPECT_CALL(*db, request(_, _)).WillOnce(InvokeArgument<1>(endpointNumberOverflow, apq::cursor()));

    EXPECT_CALL(*cp, invoke(EndpointQuery(EndpointType::master, false, 0), _)).WillOnce(Return(db));
    EXPECT_CALL(*db, request(_, _)).WillOnce(InvokeArgument<1>(brokenPipe, apq::cursor()));
    EXPECT_CALL(*cp, invoke(EndpointQuery(EndpointType::master, true, 0), _)).WillOnce(Return(db));
    EXPECT_CALL(*db, request(_, _)).WillOnce(InvokeArgument<1>(brokenPipe, apq::cursor()));

    caller.run<Request>(connectionProviderMock, q, [] (database::error_code, const auto&) {}, log);

    EXPECT_TRUE(testing::Mock::VerifyAndClearExpectations(q.get()));
    EXPECT_TRUE(testing::Mock::VerifyAndClearExpectations(db.get()));
    EXPECT_TRUE(testing::Mock::VerifyAndClearExpectations(connectionProviderMock.mock.get()));
}

TEST_F(CallerTest, should_read_only_master) {
    StrategyProvider strategyProvider((strategy::MasterOnly()));
    Caller caller((strategy::MasterOnly()));

    const auto& cp = connectionProviderMock.mock;

    const database::error_code brokenPipe((mail_errors::error_code(boost::asio::error::broken_pipe)));
    const database::error_code endpointNumberOverflow((pgg::error_code(error::endpointNumberOverflow, "")));

    InSequence s;

    EXPECT_CALL(*cp, invoke(EndpointQuery(EndpointType::master, false, 0), _)).WillOnce(Return(db));
    EXPECT_CALL(*db, request(_, _)).WillOnce(InvokeArgument<1>(brokenPipe, apq::cursor()));
    EXPECT_CALL(*cp, invoke(EndpointQuery(EndpointType::master, true, 0), _)).WillOnce(Return(db));
    EXPECT_CALL(*db, request(_, _)).WillOnce(InvokeArgument<1>(brokenPipe, apq::cursor()));

    caller.run<Request>(connectionProviderMock, q, [] (database::error_code, const auto&) {}, log);

    EXPECT_TRUE(testing::Mock::VerifyAndClearExpectations(q.get()));
    EXPECT_TRUE(testing::Mock::VerifyAndClearExpectations(db.get()));
    EXPECT_TRUE(testing::Mock::VerifyAndClearExpectations(connectionProviderMock.mock.get()));
}

TEST_F(CallerTest, should_read_only_replica) {
    StrategyProvider strategyProvider((strategy::ReplicaOnly()));
    Caller caller((strategy::ReplicaOnly()));

    const auto& cp = connectionProviderMock.mock;

    const database::error_code brokenPipe((mail_errors::error_code(boost::asio::error::broken_pipe)));
    const database::error_code endpointNumberOverflow((pgg::error_code(error::endpointNumberOverflow, "")));

    InSequence s;

    EXPECT_CALL(*cp, invoke(EndpointQuery(EndpointType::replica, false, 0), _)).WillOnce(Return(db));
    EXPECT_CALL(*db, request(_, _)).WillOnce(InvokeArgument<1>(brokenPipe, apq::cursor()));
    EXPECT_CALL(*cp, invoke(EndpointQuery(EndpointType::replica, false, 1), _)).WillOnce(Return(db));
    EXPECT_CALL(*db, request(_, _)).WillOnce(InvokeArgument<1>(endpointNumberOverflow, apq::cursor()));

    caller.run<Request>(connectionProviderMock, q, [] (database::error_code, const auto&) {}, log);

    EXPECT_TRUE(testing::Mock::VerifyAndClearExpectations(q.get()));
    EXPECT_TRUE(testing::Mock::VerifyAndClearExpectations(db.get()));
    EXPECT_TRUE(testing::Mock::VerifyAndClearExpectations(connectionProviderMock.mock.get()));
}

}
