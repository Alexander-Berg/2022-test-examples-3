#pragma once

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <ymod_tvm/module.h>

namespace NTvmAuthWrapper {

static std::ostream& operator <<(std::ostream& out, const NTvmAuthWrapper::TCheckedServiceTicket&) {
    return out;
}

static std::ostream& operator <<(std::ostream& out, const NTvmAuthWrapper::TCheckedUserTicket&) {
    return out;
}

}

namespace collie::tests {

using namespace ymod_tvm;
using namespace NTvmAuthWrapper;

struct MockTvm2Module: tvm2_module {
    MOCK_METHOD(void, subscribe_service_ticket, (const std::string&, const tickets_ready_callback&), (override));
    MOCK_METHOD(void, subscribe_all_tickets_are_ready, (const callback&), (override));
    MOCK_METHOD(void, subscribe_keys_loaded, (const callback&), (override));
    MOCK_METHOD(boost::system::error_code, check_service_ticket, (task_context_ptr, const std::string&), (override));
    MOCK_METHOD(boost::optional<tvm2::service_ticket>, get_native_service_ticket, (task_context_ptr, const std::string&), (override));
    MOCK_METHOD((boost::variant<boost::system::error_code, tvm2::service_ticket>), get_native_service_ticket_or_error, (const std::string&), (override));
    MOCK_METHOD(boost::system::error_code, get_service_ticket, (const std::string&, std::string&), (override));
    MOCK_METHOD(boost::system::error_code, check_user_ticket, (task_context_ptr,
            tvm2::blackbox_env, const std::string&), (override));
    MOCK_METHOD(boost::optional<tvm2::user_ticket>, get_native_user_ticket, (task_context_ptr,
            tvm2::blackbox_env, const std::string&), (override));
    MOCK_METHOD((boost::variant<boost::system::error_code, tvm2::user_ticket>), get_native_user_ticket_or_error, (tvm2::blackbox_env,
            const std::string&), (override));
};

} //collie::tests
