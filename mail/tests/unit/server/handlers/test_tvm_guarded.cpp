#include <tests/unit/test_with_task_context.hpp>
#include <tests/unit/ymod_webserver_mocks.hpp>

#include <src/server/handlers/tvm_guarded.hpp>

namespace {

namespace hana = boost::hana;

using namespace testing;

using collie::expected;
using collie::make_expected_from_error;
using collie::TaskContextPtr;
using collie::error_code;
using collie::server::Error;
using collie::server::StreamPtr;
using collie::server::TvmGuarded;
using collie::server::Uid;
using collie::server::makeTvmGuarded;
using collie::tests::MockStream;
using tvm_guard::Action;
using tvm_guard::Reason;
using tvm_guard::Response;

struct TvmGuardMock {
    MOCK_METHOD(Response, check, (const std::string&, const std::string&,
        const std::optional<std::string_view>&, const std::optional<std::string_view>&), (const));
};

struct ContinuationMock {
    struct Impl {
        MOCK_METHOD(expected<int>, call, (hana::tuple<Uid>, const StreamPtr&, const TaskContextPtr&), ());
        MOCK_METHOD(expected<int>, call, (hana::tuple<>, const StreamPtr&, const TaskContextPtr&), ());
    };

    const std::shared_ptr<StrictMock<Impl>> impl {std::make_shared<StrictMock<Impl>>()};

    template<typename Parameters>
    expected<int> operator()(Parameters&& parameters, const StreamPtr& stream,
            const TaskContextPtr& context) {
        return impl->call(std::forward<Parameters>(parameters), stream, context);
    }
};

struct TestServerHandlersTvmGuarded : TestWithTaskContext {
    TestServerHandlersTvmGuarded() {
        std::string serviceTicketHeader;
        boost::to_lower_copy(std::back_inserter(serviceTicketHeader), tvm_guard::header::serviceTicket());
        request->headers[serviceTicketHeader] = serviceTicket;

        std::string userTicketHeader;
        boost::to_lower_copy(std::back_inserter(userTicketHeader), tvm_guard::header::userTicket());
        request->headers[userTicketHeader] = userTicket;
    }

    template<typename Parameters>
    void testTvmGuarded(const std::string& uid, Action action, Parameters&& parameters,
            const expected<int>& result) {
        withSpawn([&] (const auto& context) {
            const InSequence sequence;
            EXPECT_CALL(*stream, request()).WillOnce(Return(request));

            Response response;
            response.action = action;
            response.reason = Reason::rule;
            EXPECT_CALL(*tvmGuard, check(path, uid, std::optional<std::string_view>(serviceTicket),
                    std::optional<std::string_view>(userTicket))).WillOnce(
                            Return(response));

            if (action == Action::accept) {
                EXPECT_CALL(*continuation.impl, call(Matcher<std::decay_t<Parameters>>(_), _, context)).
                        WillOnce(Return(result));
            }

            EXPECT_EQ(tvmGuarded(continuation, std::forward<Parameters>(parameters), stream, context),
                    result);
        });
    }

    void testWithUid(Action action, const expected<int>& result) {
        std::string uid{"42"};
        testTvmGuarded(uid, action, hana::tuple<Uid>{Uid{uid}}, result);
    }

    void testWithoutUid(Action action, const expected<int>& result) {
        testTvmGuarded({}, action, hana::tuple<>{}, result);
    }

    const boost::shared_ptr<StrictMock<MockStream>> stream{boost::make_shared<StrictMock<MockStream>>()};
    const boost::shared_ptr<ymod_webserver::request> request{boost::make_shared<ymod_webserver::request>()};
    const std::shared_ptr<const StrictMock<const TvmGuardMock>> tvmGuard{
        std::make_shared<const StrictMock<const TvmGuardMock>>()};
    const std::string serviceTicket{"service_ticket"};
    const std::string userTicket{"user_ticket"};
    ContinuationMock continuation;
    const std::string path{"path"};
    const TvmGuarded<StrictMock<const TvmGuardMock>> tvmGuarded{makeTvmGuarded(tvmGuard, path)};
};

TEST_F(TestServerHandlersTvmGuarded, when_tvm_guard_check_return_accept_should_call_continuation) {
    const auto value{42};
    const expected<int> result{value};
    testWithUid(Action::accept, result);
    testWithoutUid(Action::accept, result);
}

TEST_F(TestServerHandlersTvmGuarded, when_tvm_guard_check_return_reject_should_return_error) {
    const auto result{make_expected_from_error<int>(error_code(Error::invalidTvm2Ticket))};
    testWithUid(Action::reject, result);
    testWithoutUid(Action::reject, result);
}

} // namespace
