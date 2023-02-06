#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/sendbernar/client/include/category.h>
#include <mail/sendbernar/client/include/response.h>
#include <yamail/data/reflection.h>


using namespace testing;

namespace sendbernar {
namespace tests {

const auto& boost_errc = boost::asio::error::get_system_category();

using namespace ::sendbernar::response;
namespace ycode = ymod_webserver::codes;

response::Response resp(mail_errors::error_code ec) {
    return Response(ec);
}

class MapDeliveryErrorCodeToHttpStatus: public ::testing::TestWithParam<DeliveryResult> { };
INSTANTIATE_TEST_SUITE_P(delivery, MapDeliveryErrorCodeToHttpStatus, ::testing::Values(
         DeliveryResult::ok,
         DeliveryResult::spam,
         DeliveryResult::strongSpam,
         DeliveryResult::virus,
         DeliveryResult::badRecipient,
         DeliveryResult::unknownError,
         DeliveryResult::badRequest,
         DeliveryResult::badKarmaBanTime,
         DeliveryResult::sendMessageFailed,
         DeliveryResult::serviceUnavaliable,
         DeliveryResult::failedToAuthSender,
         DeliveryResult::badSender,
         DeliveryResult::toManyRecipients,
         DeliveryResult::sizeLimitExceeded
    )
);
TEST_P(MapDeliveryErrorCodeToHttpStatus, delivery) {
    DeliveryResult v = GetParam();
    switch(v) {
        case DeliveryResult::ok:
            EXPECT_THROW(Response(make_error(v)), Exception);
            break;
        case DeliveryResult::spam:
            EXPECT_THROW(Response(make_error(v)), Exception);
            break;
        case DeliveryResult::strongSpam:
            EXPECT_THROW(Response(make_error(v)), Exception);
            break;
        case DeliveryResult::virus:
            EXPECT_THROW(Response(make_error(v)), Exception);
            break;
        case DeliveryResult::badRecipient:
            EXPECT_EQ(Response(make_error(v)).code, ycode::bad_request);
            break;
        case DeliveryResult::unknownError:
            EXPECT_EQ(Response(make_error(v)).code, ycode::internal_server_error);
            break;
        case DeliveryResult::badRequest:
            EXPECT_EQ(Response(make_error(v)).code, ycode::bad_request);
            break;
        case DeliveryResult::badKarmaBanTime:
            EXPECT_EQ(Response(make_error(v)).code, ycode::bad_request);
            break;
        case DeliveryResult::sendMessageFailed:
            EXPECT_EQ(Response(make_error(v)).code, ycode::internal_server_error);
            break;
        case DeliveryResult::serviceUnavaliable:
            EXPECT_EQ(Response(make_error(v)).code, ycode::internal_server_error);
            break;
        case DeliveryResult::failedToAuthSender:
            EXPECT_EQ(Response(make_error(v)).code, ycode::bad_request);
            break;
        case DeliveryResult::badSender:
            EXPECT_EQ(Response(make_error(v)).code, ycode::bad_request);
            break;
        case DeliveryResult::toManyRecipients:
            EXPECT_EQ(Response(make_error(v)).code, ycode::bad_request);
            break;
        case DeliveryResult::sizeLimitExceeded:
            EXPECT_EQ(Response(make_error(v)).code, ycode::bad_request);
            break;
    }
}

class MapComposeErrorCodeToHttpStatus: public ::testing::TestWithParam<ComposeResult> { };
INSTANTIATE_TEST_SUITE_P(compose, MapComposeErrorCodeToHttpStatus, ::testing::Values(
         ComposeResult::EMPTY,
         ComposeResult::DONE,
         ComposeResult::TO_INVALID,
         ComposeResult::CC_INVALID,
         ComposeResult::BCC_INVALID,
         ComposeResult::TO_CC_BCC_EMPTY,
         ComposeResult::ATTACHMENT_TOO_BIG,
         ComposeResult::MSG_TOO_BIG,
         ComposeResult::MAX_EMAIL_ADDR_REACHED,
         ComposeResult::CHARSET_INVALID,
         ComposeResult::ATTACHMENT_STORAGE_ERROR,
         ComposeResult::PARTS_JSON_INVALID,
         ComposeResult::SANITIZER_CALL_WAS_NOT_SUCCESS
    )
);
TEST_P(MapComposeErrorCodeToHttpStatus, compose) {
    ComposeResult v = GetParam();
    switch(v) {
        case ComposeResult::EMPTY:
            EXPECT_EQ(Response(make_error(v)).code, ycode::internal_server_error);
            break;
        case ComposeResult::DONE:
            EXPECT_THROW(Response(make_error(v)), Exception);
            break;
        case ComposeResult::TO_INVALID:
            EXPECT_EQ(Response(make_error(v)).code, ycode::bad_request);
            break;
        case ComposeResult::CC_INVALID:
            EXPECT_EQ(Response(make_error(v)).code, ycode::bad_request);
            break;
        case ComposeResult::BCC_INVALID:
            EXPECT_EQ(Response(make_error(v)).code, ycode::bad_request);
            break;
        case ComposeResult::TO_CC_BCC_EMPTY:
            EXPECT_EQ(Response(make_error(v)).code, ycode::bad_request);
            break;
        case ComposeResult::ATTACHMENT_TOO_BIG:
            EXPECT_EQ(Response(make_error(v)).code, ycode::bad_request);
            break;
        case ComposeResult::MSG_TOO_BIG:
            EXPECT_THROW(Response(make_error(v)), Exception);
            EXPECT_EQ(Response(make_error(v), "message_id").code, ycode::request_entity_too_large);
            break;
        case ComposeResult::MAX_EMAIL_ADDR_REACHED:
            EXPECT_EQ(Response(make_error(v)).code, ycode::bad_request);
            break;
        case ComposeResult::CHARSET_INVALID:
            EXPECT_EQ(Response(make_error(v)).code, ycode::bad_request);
            break;
        case ComposeResult::ATTACHMENT_STORAGE_ERROR:
            EXPECT_EQ(Response(make_error(v)).code, ycode::bad_request);
            break;
        case ComposeResult::PARTS_JSON_INVALID:
            EXPECT_EQ(Response(make_error(v)).code, ycode::bad_request);
            break;
        case ComposeResult::SANITIZER_CALL_WAS_NOT_SUCCESS:
            EXPECT_EQ(Response(make_error(v)).code, ycode::internal_server_error);
            break;
    }
}

class MapSendbernarErrorCodeToHttpStatus: public ::testing::TestWithParam<ErrorResult> { };
INSTANTIATE_TEST_SUITE_P(error, MapSendbernarErrorCodeToHttpStatus, ::testing::Values(
         ErrorResult::ok,
         ErrorResult::invalidParam,
         ErrorResult::bbError,
         ErrorResult::unexpectedCondition,
         ErrorResult::reminderError,
         ErrorResult::storageError,
         ErrorResult::wrongHttpMethod,
         ErrorResult::cannotSaveMessage,
         ErrorResult::fromCache
    )
);
TEST_P(MapSendbernarErrorCodeToHttpStatus, error) {
    ErrorResult v = GetParam();
    switch(v) {
        case ErrorResult::ok:
            EXPECT_THROW(Response(make_error(v, "")), Exception);
            break;
        case ErrorResult::invalidParam:
            EXPECT_EQ(Response(make_error(v, "")).code, ycode::bad_request);
            break;
        case ErrorResult::bbError:
            EXPECT_EQ(Response(make_error(v, "")).code, ycode::bad_request);
            break;
        case ErrorResult::unexpectedCondition:
            EXPECT_EQ(Response(make_error(v, "")).code, ycode::internal_server_error);
            break;
        case ErrorResult::reminderError:
            EXPECT_EQ(Response(make_error(v, "")).code, ycode::internal_server_error);
            break;
        case ErrorResult::storageError:
            EXPECT_EQ(Response(make_error(v, "")).code, ycode::internal_server_error);
            break;
        case ErrorResult::wrongHttpMethod:
            EXPECT_EQ(Response(make_error(v, "")).code, ycode::method_not_allowed);
            break;
        case ErrorResult::cannotSaveMessage:
            EXPECT_EQ(Response(make_error(v, "")).code, ycode::gone);
            break;
        case ErrorResult::fromCache:
            EXPECT_THROW(Response(make_error(v, "")), Exception);
            break;
    }
}

TEST(MapErrorCodeToHttpStatus, shouldBe200Ok) {
    EXPECT_EQ(Response("").code, ycode::ok);
    EXPECT_EQ(Response(DelayedMessageCallbackResponse()).code, ycode::ok);
    EXPECT_EQ(Response(RemindMessageCallbackResponse()).code, ycode::ok);
    EXPECT_EQ(Response(SendBarbetMessageResponse()).code, ycode::ok);
    EXPECT_EQ(Response(NoAnswerRemindCallbackResponse()).code, ycode::ok);
    EXPECT_EQ(Response(ComposeDraftResult()).code, ycode::ok);
    EXPECT_EQ(Response(ComposeMessageResult()).code, ycode::ok);
    EXPECT_EQ(Response(GenerateOperationIdResponse()).code, ycode::ok);
    EXPECT_EQ(Response(ListUnsubscribeResponse()).code, ycode::ok);
    EXPECT_EQ(Response(CancelSendDelayedResponse()).code, ycode::ok);
    EXPECT_EQ(Response(WriteAttachmentResponse()).code, ycode::ok);
    EXPECT_EQ(Response(MessageSavedResponse()).code, ycode::ok);
    EXPECT_EQ(Response(MessageSentResponse()).code, ycode::ok);
    EXPECT_EQ(Response(LimitsResponse()).code, ycode::ok);
}

TEST(MapErrorCodeToHttpStatus, callbackResponseError) {
    {
        Response code(CallbackResponseError{true});
        EXPECT_EQ(code.code, ycode::bad_request);
        EXPECT_THAT(code.additionalHeaders.headers.at("X-Ya-CallMeBack-Notify-Reject"), UnorderedElementsAre("yes"));
    }

    {
        Response code(CallbackResponseError{false});
        EXPECT_EQ(code.code, ycode::internal_server_error);
        EXPECT_EQ(code.additionalHeaders.headers.empty(), true);
    }
}

class SaveMessageWithOptionalCaptchaTest: public ::testing::TestWithParam<std::pair<DeliveryResult, ycode::code>> { };
INSTANTIATE_TEST_SUITE_P(test, SaveMessageWithOptionalCaptchaTest, ::testing::Values(
         std::make_pair(DeliveryResult::spam, ycode::payment_required),
         std::make_pair(DeliveryResult::strongSpam, ycode::conflict),
         std::make_pair(DeliveryResult::virus, ycode::conflict),
         std::make_pair(DeliveryResult::badKarmaBanTime, ycode::version_not_supported) // any other errorcode
    )
);
TEST_P(SaveMessageWithOptionalCaptchaTest, test) {
    const auto& [errorCode, httpCode] = GetParam();

    Response code(SaveMessageWithOptionalCaptcha{
        .result=errorCode
    });
    EXPECT_EQ(code.code, httpCode);
}

TEST(MapErrorCodeToHttpStatus, shouldThrowAnExceptionWhenCalledWithoutAnError) {
    EXPECT_THROW(Response(make_error(ErrorResult::ok, "")),
                 Exception);
    EXPECT_THROW(Response(mail_errors::error_code(boost::system::errc::success, boost_errc)),
                 Exception);
    EXPECT_THROW(resp(mail_errors::error_code()),
                 Exception);
}

}
}
