#include "settings_base_handle_mocks.h"
#include "settings_response_mocks.h"
#include "test_with_task_context.h"

#include <yplatform/application/config/yaml_to_ptree.h>
#include <yplatform/ptree.h>
#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <internal/tvm2/tvm_guard.h>
#include <internal/http/detail/handlers/utils.h>
#include <tvm2/impl.h>


namespace {

using namespace testing;
using namespace settings;
using namespace settings::tvm2;
using namespace settings::test;
using namespace tvm_guard;

using Methods = ymod_webserver::methods::http_method;
using Code = ymod_webserver::codes::code;
using TmvModuleImpl = ymod_tvm::tvm2::impl;
using SharpeiErrors = sharpei::client::Errors;

struct TestBaseHandle: public TestWithTaskContext {
    TestBaseHandle() {
        yplatform::ptree node;
        utils::config::yaml_to_ptree::convert_str(cfg, node);
        guard = makeTvmGuard(node, std::static_pointer_cast<TvmModule>(module));
    }

    const std::string cfg =
        "bb_env: blackbox\n"
        "default: reject\n"
        "strong_uid_check: false\n"
        "rules:\n"
        "-   name: handler\n"
        "    paths: ['/allowed']\n"
        "    default_action: accept\n"
        "    accept_by_service: []";

    MockBaseHandle baseHandle {"logic_module"};
    RequestPtr request {
        boost::make_shared<ymod_webserver::request>(
            boost::make_shared<ymod_webserver::context>()
        )
    };
    boost::shared_ptr<StrictMock<MockedResponse>> response {
        boost::make_shared<StrictMock<MockedResponse>>()
    };
    std::shared_ptr<TmvModuleImpl> module;
    TvmGuardPtr guard;
};

TEST_F(TestBaseHandle,
        for_request_with_not_allowed_method_should_be_added_not_allowed_code_to_response) {
    withSpawn([this](const auto& context) {
        request->method = Methods::mth_head;
        EXPECT_CALL(baseHandle, method()).WillOnce(Return(Methods::mth_get));
        EXPECT_CALL(*response, set_code(Code::method_not_allowed, _));
        EXPECT_CALL(*response, result_body(_));
        EXPECT_NO_THROW(baseHandle.process(request, response, guard, context));
    });
}

TEST_F(TestBaseHandle,
        for_request_that_was_rejected_should_be_added_unauthorized_code_to_response) {
    withSpawn([this](const auto& context) {
        request->method = Methods::mth_get;
        EXPECT_CALL(baseHandle, method()).WillOnce(Return(Methods::mth_get));
        EXPECT_CALL(baseHandle, uri()).WillOnce(Return("/not_allowed"));
        EXPECT_CALL(*response, set_code(Code::unauthorized, _));
        EXPECT_CALL(*response, result_body(_));
        EXPECT_NO_THROW(baseHandle.process(request, response, guard, context));
    });
}

TEST_F(TestBaseHandle, for_successful_request_should_no_throw_exception) {
    withSpawn([this](const auto& context) {
        request->method = Methods::mth_get;
        EXPECT_CALL(baseHandle, method()).WillOnce(Return(Methods::mth_get));
        EXPECT_CALL(baseHandle, uri()).WillOnce(Return("/allowed"));
        EXPECT_CALL(baseHandle, invoke(_, _, _));
        EXPECT_NO_THROW(baseHandle.process(request, response, guard, context));
    });
}

TEST_F(TestBaseHandle,
        for_request_that_throw_std_exception_should_added_internal_server_error_code_to_response) {
    withSpawn([this](const auto& context) {
        request->method = Methods::mth_get;
        EXPECT_CALL(baseHandle, method()).WillOnce(Return(Methods::mth_get));
        EXPECT_CALL(baseHandle, uri()).WillOnce(Return("/allowed"));
        EXPECT_CALL(baseHandle, invoke(_, _, _)).WillOnce(Throw(std::exception()));
        EXPECT_CALL(*response, set_code(Code::internal_server_error, _));
        EXPECT_CALL(*response, result_body(_));
        EXPECT_CALL(*response, set_content_type(_));
        EXPECT_NO_THROW(baseHandle.process(request, response, guard, context));
    });
}

TEST_F(TestBaseHandle,
        for_request_that_return_sharpei_not_found_user_error_should_added_not_found_error_code_to_response) {
    withSpawn([this](const auto& context) {
        request->method = Methods::mth_get;
        EXPECT_CALL(baseHandle, method()).WillOnce(Return(Methods::mth_get));
        EXPECT_CALL(baseHandle, uri()).WillOnce(Return("/allowed"));
        EXPECT_CALL(baseHandle, invoke(_, _, _))
            .WillOnce(Return(make_unexpected(error_code(make_error_code(SharpeiErrors::UidNotFound)))));
        EXPECT_CALL(*response, set_code(Code::not_found, _));
        EXPECT_CALL(*response, result_body(_));
        EXPECT_CALL(*response, set_content_type(_));
        EXPECT_NO_THROW(baseHandle.process(request, response, guard, context));
    });
}

TEST_F(TestBaseHandle,
        for_request_that_return_black_box_user_error_should_added_not_found_error_code_to_response) {
    withSpawn([this](const auto& context) {
        request->method = Methods::mth_get;
        EXPECT_CALL(baseHandle, method()).WillOnce(Return(Methods::mth_get));
        EXPECT_CALL(baseHandle, uri()).WillOnce(Return("/allowed"));
        EXPECT_CALL(baseHandle, invoke(_, _, _))
            .WillOnce(Return(make_unexpected(error_code(make_error_code(Error::blackBoxUserError)))));
        EXPECT_CALL(*response, set_code(Code::not_found, _));
        EXPECT_CALL(*response, result_body(_));
        EXPECT_CALL(*response, set_content_type(_));
        EXPECT_NO_THROW(baseHandle.process(request, response, guard, context));
    });
}

TEST_F(TestBaseHandle,
        for_request_that_return_nill_value_error_should_added_internal_server_error_code_to_response) {
    withSpawn([this](const auto& context) {
        request->method = Methods::mth_get;
        EXPECT_CALL(baseHandle, method()).WillOnce(Return(Methods::mth_get));
        EXPECT_CALL(baseHandle, uri()).WillOnce(Return("/allowed"));
        EXPECT_CALL(baseHandle, invoke(_, _, _))
            .WillOnce(Return(make_unexpected(error_code(make_error_code(Error::nullValue)))));
        EXPECT_CALL(*response, set_code(Code::internal_server_error, _));
        EXPECT_CALL(*response, result_body(_));
        EXPECT_CALL(*response, set_content_type(_));
        EXPECT_NO_THROW(baseHandle.process(request, response, guard, context));
    });
}


TEST_F(TestBaseHandle,
        for_request_that_return_invalid_parameter_error_should_added_bad_request_error_code_to_response) {
    withSpawn([this](const auto& context) {
        request->method = Methods::mth_get;
        EXPECT_CALL(baseHandle, method()).WillOnce(Return(Methods::mth_get));
        EXPECT_CALL(baseHandle, uri()).WillOnce(Return("/allowed"));
        EXPECT_CALL(baseHandle, invoke(_, _, _))
            .WillOnce(Return(make_unexpected(error_code(make_error_code(Error::invalidParameterError)))));
        EXPECT_CALL(*response, set_code(Code::bad_request, _));
        EXPECT_CALL(*response, result_body(_));
        EXPECT_CALL(*response, set_content_type(_));
        EXPECT_NO_THROW(baseHandle.process(request, response, guard, context));
    });
}

}
