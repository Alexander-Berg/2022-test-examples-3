#include <web/handlers/base.h>

#include <mocks/handler.h>
#include <mocks/http_stream.h>
#include <mocks/tvm_guard.h>
#include <types/context.h>

#include <gtest/gtest.h>

namespace {

using namespace testing;

using NMdb::NWeb::EHttpCode;
using NMdb::NWeb::EHttpMethod;
using NMdb::NWeb::NHandler::TBase;
using NMdb::NWeb::TInvalidArgument;
using NMdb::TContext;

using TRequest = ymod_webserver::request;
using TTvmGuardAction = tvm_guard::Action;
using TTvmGuardResponse = tvm_guard::Response;

struct TTestBase : public Test {
    TContext MakeContext(std::string sessionId) const {
        return TContext(
            std::move(sessionId),
            yplatform::log::source {YGLOBAL_LOG_SERVICE, "mdbsave"});
    }

    TRequest MakeRequest(EHttpMethod method) {
        TRequest request;
        request.method = method;
        return request;
    }

    const std::shared_ptr<TTvmGuardMock> TvmGuard{std::make_shared<TTvmGuardMock>()};
    const std::string Uri{"/uri"};
    const EHttpMethod Method{EHttpMethod::mth_post};
    const std::shared_ptr<THandlerMock> Handler{std::make_shared<THandlerMock>()};
    TBase<std::shared_ptr<TTvmGuardMock>, std::shared_ptr<THandlerMock>> Base{TvmGuard, Uri, Method, Handler};
    const boost::shared_ptr<THttpStreamMock> HttpStream{boost::make_shared<THttpStreamMock>()};
    const std::string SessionId{"ConnectionId-EnvelopeId"};
    const TContext Context{MakeContext(SessionId)};
};

TEST_F(TTestBase, for_incorrect_method_must_respond_with_error) {
    const InSequence sequence;
    EXPECT_CALL(*HttpStream, request()).WillOnce(Return(boost::make_shared<TRequest>(MakeRequest(
        EHttpMethod::mth_get))));
    EXPECT_CALL(*HttpStream, set_code(EHttpCode::method_not_allowed, std::string{}));
    EXPECT_CALL(*HttpStream, set_content_type("application/json"));
    EXPECT_CALL(*HttpStream, result_body(
        R"({"error":"MethodNotAllowed","message":"MethodNotAllowed"})"));
    Base(HttpStream, SessionId);
}

TEST_F(TTestBase, for_tvm_guard_reject_must_respond_with_error) {
    const InSequence sequence;
    EXPECT_CALL(*HttpStream, request()).WillOnce(Return(boost::make_shared<TRequest>(MakeRequest(
        EHttpMethod::mth_post))));
    EXPECT_CALL(*TvmGuard, check(Uri, std::string{}, std::optional<std::string_view>{},
        std::optional<std::string_view>{})).WillOnce(Return(TTvmGuardResponse{}));
    EXPECT_CALL(*HttpStream, set_code(EHttpCode::unauthorized, std::string{}));
    EXPECT_CALL(*HttpStream, set_content_type("application/json"));
    EXPECT_CALL(*HttpStream, result_body(
        R"({"error":"Unauthorized","message":"unknown_service"})"));
    Base(HttpStream, SessionId);
}

TEST_F(TTestBase, for_invalid_argument_exception_must_respond_with_error) {
    const InSequence sequence;
    EXPECT_CALL(*HttpStream, request()).WillOnce(Return(boost::make_shared<TRequest>(MakeRequest(
        EHttpMethod::mth_post))));
    TTvmGuardResponse tvmGuardResponse;
    tvmGuardResponse.action = TTvmGuardAction::accept;
    EXPECT_CALL(*TvmGuard, check(Uri, std::string{}, std::optional<std::string_view>{},
        std::optional<std::string_view>{})).WillOnce(Return(std::move(tvmGuardResponse)));
    EXPECT_CALL(*Handler, Call(Pointee(Context), _)).WillOnce(Throw(TInvalidArgument("what")));

    EXPECT_CALL(*HttpStream, set_code(EHttpCode::bad_request, std::string{}));
    EXPECT_CALL(*HttpStream, set_content_type("application/json"));
    EXPECT_CALL(*HttpStream, result_body(
        R"({"error":"BadRequest","message":"what"})"));
    Base(HttpStream, SessionId);
}

TEST_F(TTestBase, for_std_exception_must_respond_with_error) {
    const InSequence sequence;
    EXPECT_CALL(*HttpStream, request()).WillOnce(Return(boost::make_shared<TRequest>(MakeRequest(
        EHttpMethod::mth_post))));
    TTvmGuardResponse tvmGuardResponse;
    tvmGuardResponse.action = TTvmGuardAction::accept;
    EXPECT_CALL(*TvmGuard, check(Uri, std::string{}, std::optional<std::string_view>{},
        std::optional<std::string_view>{})).WillOnce(Return(std::move(tvmGuardResponse)));
    EXPECT_CALL(*Handler, Call(Pointee(Context), _)).WillOnce(Throw(std::runtime_error("what")));

    EXPECT_CALL(*HttpStream, set_code(EHttpCode::internal_server_error, std::string{}));
    EXPECT_CALL(*HttpStream, set_content_type("application/json"));
    EXPECT_CALL(*HttpStream, result_body(
        R"({"error":"InternalServerError","message":"what"})"));
    Base(HttpStream, SessionId);
}

TEST_F(TTestBase, for_unknown_exception_must_respond_with_error) {
    const InSequence sequence;
    EXPECT_CALL(*HttpStream, request()).WillOnce(Return(boost::make_shared<TRequest>(MakeRequest(
        EHttpMethod::mth_post))));
    TTvmGuardResponse tvmGuardResponse;
    tvmGuardResponse.action = TTvmGuardAction::accept;
    EXPECT_CALL(*TvmGuard, check(Uri, std::string{}, std::optional<std::string_view>{},
        std::optional<std::string_view>{})).WillOnce(Return(std::move(tvmGuardResponse)));
    struct CustomException{};
    EXPECT_CALL(*Handler, Call(Pointee(Context), _)).WillOnce(WithoutArgs([] {
        throw CustomException{};
    }));

    EXPECT_CALL(*HttpStream, set_code(EHttpCode::internal_server_error, std::string{}));
    EXPECT_CALL(*HttpStream, set_content_type("application/json"));
    EXPECT_CALL(*HttpStream, result_body(
        R"({"error":"InternalServerError","message":"unknown error occured"})"));
    Base(HttpStream, SessionId);
}

TEST_F(TTestBase, for_correct_preconditions_must_call_handler) {
    const InSequence sequence;
    EXPECT_CALL(*HttpStream, request()).WillOnce(Return(boost::make_shared<TRequest>(MakeRequest(
        EHttpMethod::mth_post))));
    TTvmGuardResponse tvmGuardResponse;
    tvmGuardResponse.action = TTvmGuardAction::accept;
    EXPECT_CALL(*TvmGuard, check(Uri, std::string{}, std::optional<std::string_view>{},
        std::optional<std::string_view>{})).WillOnce(Return(std::move(tvmGuardResponse)));
    EXPECT_CALL(*Handler, Call(Pointee(Context), _));
    Base(HttpStream, SessionId);
}

TEST_F(TTestBase, for_uri_request_must_return_uri) {
    EXPECT_EQ(Uri, Base.GetUri());
}

}
