#pragma once

// COPIED FROM mail/notsolitesrv/tests/unit/mocks/ymod_tvm.h@r9343422

#include <ymod_tvm/module.h>
#include <gmock/gmock.h>

namespace NTvmAuthWrapper {

inline std::ostream& operator<<(std::ostream& os, const TCheckedServiceTicket& ticket) {
    os << ticket.DebugInfo();
    return os;
}

inline std::ostream& operator<<(std::ostream& os, const TCheckedUserTicket& ticket) {
    os << ticket.DebugInfo();
    return os;
}

} // namespace NTvmAuthWrapper

struct TTvmClientMock: public ymod_tvm::tvm2_module {
    using TReadyCb = ymod_tvm::tvm2_module::tickets_ready_callback;
    using TCb = ymod_tvm::tvm2_module::callback;
    using TTaskCtxPtr = ymod_tvm::task_context_ptr;
    using TErrorCode = boost::system::error_code;
    using TSvcTicket = ymod_tvm::tvm2::service_ticket;
    using TOptSvcTicket = boost::optional<TSvcTicket>;
    using TErrOrSvcTicket = boost::variant<ymod_tvm::error_code, TSvcTicket>;
    using TCheckedUserTicket = ymod_tvm::tvm2::user_ticket;
    using TOptUserTicket = boost::optional<TCheckedUserTicket>;
    using TErrOrUserTicket = boost::variant<ymod_tvm::error_code, TCheckedUserTicket>;
    using TBBEnv = ymod_tvm::tvm2::blackbox_env;

    MOCK_METHOD(void, subscribe_service_ticket, (const std::string&, const TReadyCb&), (override));
    MOCK_METHOD(void, subscribe_all_tickets_are_ready, (const TCb&), (override));

    MOCK_METHOD(TErrorCode, check_service_ticket, (TTaskCtxPtr, const std::string&), (override));

    MOCK_METHOD(TOptSvcTicket, get_native_service_ticket, (TTaskCtxPtr, const std::string&), (override));
    MOCK_METHOD(TErrOrSvcTicket, get_native_service_ticket_or_error, (const std::string&), (override));
    MOCK_METHOD(TErrorCode, get_service_ticket, (const std::string&, std::string&), (override));
    MOCK_METHOD(TErrorCode, get_service_ticket_for_host, (const std::string&, std::string&), ());

    MOCK_METHOD(TErrorCode, check_user_ticket, (TTaskCtxPtr, TBBEnv, const std::string&), (override));

    MOCK_METHOD(TOptUserTicket, get_native_user_ticket, (TTaskCtxPtr, TBBEnv, const std::string&), (override));
    MOCK_METHOD(TErrOrUserTicket, get_native_user_ticket_or_error, (TBBEnv, const std::string&), (override));

    MOCK_METHOD(void, subscribe_keys_loaded, (const TCb&), (override));
};
