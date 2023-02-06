#include <tests/unit/logger_mock.hpp>
#include <tests/unit/ymod_webserver_mocks.hpp>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <src/server/router/router.hpp>
#include <src/server/error_category.hpp>
#include <src/server/server.hpp>
#include <src/error_code.hpp>

namespace {

using namespace testing;

using collie::error_code;
using collie::expected;
using collie::make_expected_from_error;
using collie::TaskContextPtr;
using collie::server::Error;
using collie::server::StreamPtr;
using collie::server::getYmodWebserverCode;
using collie::server::makeHandler;
using collie::tests::ErrorAttribute;
using collie::tests::ExceptionAttribute;
using collie::tests::LoggerMock;
using collie::tests::LoggerMockWrapper;
using collie::tests::MessageAttribute;
using collie::tests::MockStream;

TEST(TestServerGetYmodWebserverCode, for_ok_should_return_ok) {
    EXPECT_EQ(getYmodWebserverCode(Error::ok), ymod_webserver::codes::ok);
}

TEST(TestServerGetYmodWebserverCode, for_route_error_should_return_bad_request) {
    EXPECT_EQ(getYmodWebserverCode(Error::routeError), ymod_webserver::codes::bad_request);
}

TEST(TestServerGetYmodWebserverCode, for_not_implemented_should_return_bad_request) {
    EXPECT_EQ(getYmodWebserverCode(Error::notImplemented), ymod_webserver::codes::bad_request);
}

TEST(TestServerGetYmodWebserverCode, for_invalid_parameter_should_return_bad_request) {
    EXPECT_EQ(getYmodWebserverCode(Error::invalidParameter), ymod_webserver::codes::bad_request);
}

TEST(TestServerGetYmodWebserverCode, for_unclassified_error_should_return_internal_server_error) {
    EXPECT_EQ(
        getYmodWebserverCode(Error::unclassifiedError),
        ymod_webserver::codes::internal_server_error
    );
}

TEST(TestServerGetYmodWebserverCode, for_invalid_value_should_return_internal_server_error) {
    EXPECT_EQ(
        getYmodWebserverCode(static_cast<Error>(100500)),
        ymod_webserver::codes::internal_server_error
    );
}

TEST(TestServerGetYmodWebserverCode, for_sharpei_uidnotfound_must_return_bad_request) {
    EXPECT_EQ(ymod_webserver::codes::bad_request, getYmodWebserverCode(sharpei::client::Errors::UidNotFound));
}

TEST(TestServerGetYmodWebserverCode, for_sharpei_other_than_uidnotfound_must_return_internal_server_error) {
    EXPECT_EQ(ymod_webserver::codes::internal_server_error, getYmodWebserverCode(
            sharpei::client::Errors::RetriesLimit));
}

struct TestServerGetYmodWebserverCodeForResourceTreeError : Test {};

TEST(TestServerGetYmodWebserverCodeForResourceTreeError, for_location_not_found_should_return_not_found) {
    EXPECT_EQ(
        getYmodWebserverCode(router::Error::location_not_found),
        ymod_webserver::codes::not_found
    );
}

TEST(TestServerGetYmodWebserverCodeForResourceTreeError, for_method_not_found_should_return_method_not_allowed) {
    EXPECT_EQ(
        getYmodWebserverCode(router::Error::method_not_found),
        ymod_webserver::codes::method_not_allowed
    );
}

TEST(TestServerGetYmodWebserverCodeForResourceTreeError, for_invalid_value_should_return_internal_server_error) {
    EXPECT_EQ(
        getYmodWebserverCode(static_cast<router::Error>(100500)),
        ymod_webserver::codes::internal_server_error
    );
}

struct RequestHandlerMock {
    MOCK_METHOD(expected<void>, call, (const StreamPtr&, const TaskContextPtr&), (const));
};

struct RequestHandlerMockWrapper {
    std::shared_ptr<StrictMock<RequestHandlerMock>> impl = std::make_shared<StrictMock<RequestHandlerMock>>();

    expected<void> operator ()(const StreamPtr& stream, const TaskContextPtr& context) const {
        return impl->call(stream, context);
    }
};

template <ymod_webserver::methods::http_method name>
constexpr auto method = router::method<
    std::integral_constant<ymod_webserver::methods::http_method, name>>;

auto makeApi(RequestHandlerMockWrapper handler) {
    using namespace router::literals;
    return "handler"_l / method<ymod_webserver::methods::mth_get>(handler);
}

struct TestServerHandler : Test {
    using Api = decltype(makeApi(RequestHandlerMockWrapper {}));
    using HandlerPtr = boost::shared_ptr<ymod_webserver::handler>;

    RequestHandlerMockWrapper requestHandler;
    Api api = makeApi(requestHandler);
    LoggerMockWrapper logger;
    boost::asio::io_context io;
    boost::shared_ptr<StrictMock<MockStream>> stream = boost::make_shared<StrictMock<MockStream>>();
    std::function<boost::asio::io_context* ()> getIo = [&] { return &io; };
    boost::shared_ptr<ymod_webserver::request> request = boost::make_shared<ymod_webserver::request>();
    boost::shared_ptr<ymod_webserver::context> context = boost::make_shared<ymod_webserver::context>();
    HandlerPtr handler = makeHandler(api, 1048576, logger, getIo);

    TestServerHandler() {
        request->url.path = ymod_webserver::uri_path_t({"handler"});
        request->method = ymod_webserver::methods::mth_get;
    }
};

TEST_F(TestServerHandler, for_empty_path_and_default_method_should_set_code_405_and_body_with_error_status) {
    request->url.path.clear();

    const InSequence s;
    EXPECT_CALL(*stream, request()).WillOnce(Return(request));
    EXPECT_CALL(*stream, ctx()).WillOnce(Return(context));
    EXPECT_CALL(*stream, request()).WillOnce(Return(request));
    EXPECT_CALL(*logger.impl, applicable(logdog::error)).WillOnce(Return(true));
    EXPECT_CALL(*logger.impl, write(logdog::error, _, _, _, A<ErrorAttribute>())).WillOnce(Return());
    EXPECT_CALL(*stream, set_code(ymod_webserver::codes::method_not_allowed, "")).WillOnce(Return());
    EXPECT_CALL(*stream, set_content_type("application/json")).WillOnce(Return());
    EXPECT_CALL(*stream, result_body(R"json({"status":"error","code":1,"message":"method is not found"})json"))
        .WillOnce(Return());
    handler->execute(ymod_webserver::request_ptr(), stream);
    io.run();
}

TEST_F(TestServerHandler, call_handler_by_path_and_method) {
    const InSequence s;
    EXPECT_CALL(*stream, request()).WillOnce(Return(request));
    EXPECT_CALL(*stream, ctx()).WillOnce(Return(context));
    EXPECT_CALL(*stream, request()).WillOnce(Return(request));
    EXPECT_CALL(*requestHandler.impl, call(_, _)).WillOnce(Return(expected<void>()));
    handler->execute(ymod_webserver::request_ptr(), stream);
    io.run();
}

TEST_F(TestServerHandler, when_handler_return_invalid_parameter_error_should_log_and_set_code_400_and_body_with_error_status) {
    const InSequence s;
    EXPECT_CALL(*stream, request()).WillOnce(Return(request));
    EXPECT_CALL(*stream, ctx()).WillOnce(Return(context));
    EXPECT_CALL(*stream, request()).WillOnce(Return(request));
    EXPECT_CALL(*requestHandler.impl, call(_, _))
        .WillOnce(Return(make_expected_from_error<void>(error_code(Error::invalidParameter))));
    EXPECT_CALL(*logger.impl, applicable(logdog::error)).WillOnce(Return(true));
    EXPECT_CALL(*logger.impl, write(logdog::error, _, _, _, A<ErrorAttribute>())).WillOnce(Return());
    EXPECT_CALL(*stream, set_code(ymod_webserver::codes::bad_request, "")).WillOnce(Return());
    EXPECT_CALL(*stream, set_content_type("application/json")).WillOnce(Return());
    EXPECT_CALL(*stream, result_body(R"json({"status":"error","code":4,"message":"invalid parameter"})json"))
        .WillOnce(Return());
    handler->execute(ymod_webserver::request_ptr(), stream);
    io.run();
}

TEST_F(TestServerHandler, when_handler_throw_exception_should_log_and_set_code_500_and_body_with_error_status) {
    const InSequence s;
    EXPECT_CALL(*stream, request()).WillOnce(Return(request));
    EXPECT_CALL(*stream, ctx()).WillOnce(Return(context));
    EXPECT_CALL(*stream, request()).WillOnce(Return(request));
    EXPECT_CALL(*requestHandler.impl, call(_, _)).WillOnce(Throw(std::runtime_error("error")));
    EXPECT_CALL(*logger.impl, applicable(logdog::error)).WillOnce(Return(true));
    EXPECT_CALL(*logger.impl, write(logdog::error, _, _, A<ExceptionAttribute>())).WillOnce(Return());
    EXPECT_CALL(*stream, set_code(ymod_webserver::codes::internal_server_error, "")).WillOnce(Return());
    EXPECT_CALL(*stream, set_content_type("application/json")).WillOnce(Return());
    EXPECT_CALL(*stream, result_body(R"json({"status":"error","code":2,"message":"error"})json"))
        .WillOnce(Return());
    handler->execute(ymod_webserver::request_ptr(), stream);
    io.run();
}

TEST_F(TestServerHandler, when_handler_throw_not_exception_should_log_and_set_code_500_and_body_with_error_status) {
    const InSequence s;
    EXPECT_CALL(*stream, request()).WillOnce(Return(request));
    EXPECT_CALL(*stream, ctx()).WillOnce(Return(context));
    EXPECT_CALL(*stream, request()).WillOnce(Return(request));
    EXPECT_CALL(*requestHandler.impl, call(_, _)).WillOnce(Throw(int()));
    EXPECT_CALL(*logger.impl, applicable(logdog::error)).WillOnce(Return(true));
    EXPECT_CALL(*logger.impl, write(logdog::error, _, _, A<MessageAttribute>())).WillOnce(Return());
    EXPECT_CALL(*stream, set_code(ymod_webserver::codes::internal_server_error, "")).WillOnce(Return());
    EXPECT_CALL(*stream, set_content_type("application/json")).WillOnce(Return());
    EXPECT_CALL(*stream, result_body(R"json({"status":"error","code":2,"message":"unknown error"})json"))
        .WillOnce(Return());
    handler->execute(ymod_webserver::request_ptr(), stream);
    io.run();
}

} // namespace
