#pragma once

#include <tvm_guard/tvm_guard.h>
#include <boost/algorithm/string/join.hpp>


namespace tvm_guard {

struct LibTicketParser2Category : public boost::system::error_category {
    const char* name() const noexcept override {
        return "libticket-parser2";
    }

    std::string message(int ev) const override {
        return TA_ErrorCodeToString(static_cast<TA_EErrorCode>(ev));
    }

    static const LibTicketParser2Category& instance() {
        static const LibTicketParser2Category category;
        return category;
    }
};

}

namespace boost::system {

template<>
struct is_error_code_enum<TA_EErrorCode> {
    static constexpr bool value = true;
};

}

inline boost::system::error_code make_error_code(TA_EErrorCode err) {
    return boost::system::error_code(err, tvm_guard::LibTicketParser2Category::instance());
}



inline std::ostream& operator << (std::ostream& out, const tvm_guard::ResponseFactory& factory) {
    out << static_cast<tvm_guard::Response>(factory.product());

    return out;
}

namespace tvm_guard {

boost::property_tree::ptree yamlToPtree(const std::string& yaml);

inline bool operator==(const Response& r1, const Response& r2) {
    return r1.action == r2.action &&
           r1.reason == r2.reason &&
           r1.uidsFromUserTicket == r2.uidsFromUserTicket &&
           r1.source == r2.source &&
           r1.issuerUid == r2.issuerUid &&
           r1.error == r2.error;
}

inline bool operator!=(const Response& r1, const Response& r2) {
    return !(r1 == r2);
}

struct ServiceTicket {
    u_int32_t src;
    u_int64_t uid;
    TA_EErrorCode status;

    u_int32_t GetSrc() const {
        return src;
    }

    u_int64_t GetIssuerUid() const {
        return uid;
    }

    TA_EErrorCode GetStatus() const {
        return status;
    }
};

struct UserTicket {
    std::vector<u_int64_t> uids;
    TA_EErrorCode status;

    std::vector<u_int64_t> GetUids() const {
        return uids;
    }

    TA_EErrorCode GetStatus() const {
        return status;
    }
};

class Tvm2Module {
    boost::variant<ServiceTicket, boost::system::error_code> service;
    boost::variant<UserTicket, boost::system::error_code> user;

public:
    Tvm2Module() {
        service = ServiceTicket{0, 0, TA_EC_OK};
        user = UserTicket{{userTicketUid()}, TA_EC_OK};
    }

    u_int64_t userTicketUid() {
        return 0;
    }

    u_int32_t sourceUnknownService() const {
        return 100;
    }

    u_int32_t emptyIssuerUid() const {
        return 0;
    }

    void ticketBelongsToUnknownService() {
        service = ServiceTicket{sourceUnknownService(), emptyIssuerUid(), TA_EC_OK};
    }

    u_int32_t sourceService1() const {
        return 1000;
    }

    void ticketBelongsToService1() {
        service = ServiceTicket{sourceService1(), emptyIssuerUid(), TA_EC_OK};
    }

    u_int32_t sourceService2() const {
        return 1001;
    }

    void ticketBelongsToService2() {
        service = ServiceTicket{sourceService2(), emptyIssuerUid(), TA_EC_OK};
    }

    u_int32_t sourceRootTicket() const {
        return 10;
    }

    u_int32_t rootIssuerUid() const {
        return 100500;
    }

    void ticketIsRootTicket() {
        service = ServiceTicket{sourceRootTicket(), rootIssuerUid(), TA_EC_OK};
    }

    void ticketIsRootTicketWithoutIssuerUid() {
        service = ServiceTicket{sourceRootTicket(), emptyIssuerUid(), TA_EC_OK};
    }

    void ticketHasStatus(TA_EErrorCode status) {
        service = ServiceTicket{sourceRootTicket(), rootIssuerUid(), status};
    }

    void ticketIsIncorrect() {
        service = boost::system::error_code();
    }

    void userTicketIsIncorrect() {
        user = boost::system::error_code();
    }

    void userTicketWithUids(const std::vector<u_int64_t>& uids) {
        user = UserTicket{uids, TA_EC_OK};
    }

    void userTicketHasStatus(TA_EErrorCode status) {
        user = UserTicket{{}, status};
    }

    boost::variant<ServiceTicket, boost::system::error_code> get_native_service_ticket_or_error(
                                                       const std::string&) {
        return service;
    }

    boost::variant<UserTicket, boost::system::error_code> get_native_user_ticket_or_error(
                                                       TA_EBlackboxEnv, const std::string&) {
        return user;
    }
};

namespace detail {
template<>
inline bool TicketTraits<UserTicket>::ticketIsSuccessful(const UserTicket& ticket) {
    return ticket.GetStatus() == TA_EC_OK;
}

template<>
inline boost::optional<std::string> TicketTraits<UserTicket>::defaultUid(const UserTicket&) {
    return boost::none;
}

template<>
inline bool TicketTraits<ServiceTicket>::ticketIsSuccessful(const ServiceTicket& ticket) {
    return ticket.GetStatus() == TA_EC_OK;
}

template<>
inline std::string TicketTraits<ServiceTicket>::issuerUid(const ServiceTicket& ticket) {
    return ticket.GetIssuerUid() ? std::to_string(ticket.GetIssuerUid()) : "none";
}

template<>
inline bool TicketTraits<ServiceTicket>::issuerUidIsEmpty(const ServiceTicket& ticket) {
    constexpr u_int64_t emptyIssuerUid = 0;

    return ticket.GetIssuerUid() == emptyIssuerUid;
}
}

}
