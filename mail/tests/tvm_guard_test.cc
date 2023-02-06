#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <boost/format.hpp>

#include <mail/tvm_guard/tests/test_tvm_module.h>
#include <mail/tvm_guard/tvm_api/error.h>

using namespace testing;


namespace tvm_guard {

std::string guardConfig(bool strongUidCheck) {
    const std::string cfg = R"(
bb_env: blackbox
default: reject
strong_uid_check: %s
root_client_id: 10
clients:
-   name: service1
    id: 1000
-   name: service2
    id: 1001
rules:
-   name: send
    paths: ['/default_reject']
    default_action: reject
    accept_by_service: ['service2']
    accept_by_user: ['service1']
-   name: compose
    paths: ['/default_accept']
    default_action: accept
    accept_by_service: []
    accept_by_user: ['service1']
-   name: reject_all
    paths: ['/reject_all']
    default_action: reject
    accept_by_service: []
    accept_by_user: []
)";
    const std::string uidOpt = strongUidCheck ? "true" : "false";
    return (boost::format(cfg) % uidOpt).str();
}

struct TvmGuardTest: public ::testing::Test {
    using Guard = tvm_guard::Guard<Tvm2Module>;

    TvmGuardTest()
        : ::testing::Test()
    { }

    virtual ~TvmGuardTest() { }

    void initGuard(bool strongUidCheck) {
        boost::property_tree::ptree node = yamlToPtree(guardConfig(strongUidCheck));

        guard = std::make_shared<Guard>(init(node, module));
    }

    void SetUp() override {
        module = std::make_shared<Tvm2Module>();
        initGuard(false);
    }

    std::shared_ptr<Tvm2Module> module;
    std::shared_ptr<Guard> guard;
};

const std::string uidFromArgs = "100500";
boost::optional<std::string> serviceTicket("service");
boost::optional<std::string> userTicket("user");
const boost::optional<std::string> none;

TEST_F(TvmGuardTest, shouldApplyDefaultActionOnUnknownPath) {
    EXPECT_EQ(guard->check(std::string("/ping"), uidFromArgs, none, none),
              ResponseFactory().action(Action::reject).reason(Reason::defaultPolicy).product());
}

TEST_F(TvmGuardTest, shouldReturnDefaultActionWithEmptyServiceTicket) {
    EXPECT_EQ(guard->check(std::string("/default_accept"), uidFromArgs, none, none),
              ResponseFactory().action(Action::accept).reason(Reason::ruleDefaultPolicy).product());

    EXPECT_EQ(guard->check(std::string("/default_accept"), uidFromArgs, boost::make_optional<std::string>(""), none),
              ResponseFactory().action(Action::accept).reason(Reason::ruleDefaultPolicy).product());

    EXPECT_EQ(guard->check(std::string("/default_reject"), uidFromArgs, none, none),
              ResponseFactory().action(Action::reject).reason(Reason::ruleDefaultPolicy).product());

    EXPECT_EQ(guard->check(std::string("/default_reject"), uidFromArgs, boost::make_optional<std::string>(""), none),
              ResponseFactory().action(Action::reject).reason(Reason::ruleDefaultPolicy).product());
}

TEST_F(TvmGuardTest, shouldAcceptEverythingWithRootServiceTicket) {
    module->ticketIsRootTicket();
    EXPECT_EQ(guard->check(std::string("/reject_all"), uidFromArgs, serviceTicket, none),
              ResponseFactory().action(Action::accept)
                               .reason(Reason::rootServiceTicket)
                               .source(module->sourceRootTicket())
                               .issuerUid(uidFromArgs)
                               .product());
}

TEST_F(TvmGuardTest, shouldRejectTicketFromRootServiceWithoutIssuerUid) {
    module->ticketIsRootTicketWithoutIssuerUid();
    EXPECT_EQ(guard->check(std::string("/reject_all"), uidFromArgs, serviceTicket, none),
              ResponseFactory().action(Action::reject)
                               .reason(Reason::rootServiceTicketWithoutUid)
                               .source(module->sourceRootTicket())
                               .product());
}

TEST_F(TvmGuardTest, shouldResponseWithRuleDefaultActionInCaseOfWrongServiceTicket) {
    module->ticketIsIncorrect();
    EXPECT_EQ(guard->check(std::string("/default_reject"), uidFromArgs, serviceTicket, none),
              ResponseFactory().action(Action::reject)
                               .reason(Reason::wrongServiceTicket)
                               .product());

    EXPECT_EQ(guard->check(std::string("/default_accept"), uidFromArgs, serviceTicket, none),
              ResponseFactory().action(Action::accept)
                               .reason(Reason::ruleDefaultPolicy)
                               .product());
}

TEST_F(TvmGuardTest, shouldResponseWithRuleDefaultActionInCaseOfWrongUserTicket) {
    module->ticketBelongsToService1();
    module->userTicketIsIncorrect();
    EXPECT_EQ(guard->check(std::string("/default_reject"), uidFromArgs, serviceTicket, userTicket),
              ResponseFactory().action(Action::reject)
                               .reason(Reason::wrongUserTicket)
                               .source(module->sourceService1())
                               .product());

    EXPECT_EQ(guard->check(std::string("/default_accept"), uidFromArgs, serviceTicket, userTicket),
              ResponseFactory().action(Action::accept)
                               .reason(Reason::ruleDefaultPolicy)
                               .source(module->sourceService1())
                               .product());
}

TEST_F(TvmGuardTest, shouldReturnDefaultActionForUnknownService) {
    module->ticketBelongsToUnknownService();
    EXPECT_EQ(guard->check(std::string("/default_accept"), uidFromArgs, serviceTicket, userTicket),
              ResponseFactory().action(Action::accept)
                               .reason(Reason::unknownService)
                               .source(module->sourceUnknownService())
                               .product());

    EXPECT_EQ(guard->check(std::string("/default_reject"), uidFromArgs, serviceTicket, userTicket),
              ResponseFactory().action(Action::reject)
                               .reason(Reason::unknownService)
                               .source(module->sourceUnknownService())
                               .product());
}

TEST_F(TvmGuardTest, shouldAcceptByRule) {
    std::vector<std::string> uids{std::to_string(module->userTicketUid())};

    module->ticketBelongsToService1();
    EXPECT_EQ(guard->check(std::string("/default_reject"), uidFromArgs, serviceTicket, userTicket),
              ResponseFactory().action(Action::accept)
                               .reason(Reason::rule)
                               .source(module->sourceService1())
                               .uidsFromUserTicket(uids)
                               .product());

    module->ticketBelongsToService2();
    EXPECT_EQ(guard->check(std::string("/default_reject"), uidFromArgs, serviceTicket, userTicket),
              ResponseFactory().action(Action::accept)
                               .reason(Reason::rule)
                               .source(module->sourceService2())
                               .product());

    module->ticketBelongsToService1();
    EXPECT_EQ(guard->check(std::string("/default_accept"), uidFromArgs, serviceTicket, userTicket),
              ResponseFactory().action(Action::accept)
                               .reason(Reason::rule)
                               .source(module->sourceService1())
                               .uidsFromUserTicket(uids)
                               .product());
}

TEST_F(TvmGuardTest, shouldRejectInCaseOfUidIsNotInTheTicketsUids) {
    initGuard(true);
    module->ticketBelongsToService1();
    module->userTicketWithUids({0});
    EXPECT_EQ(guard->check(std::string("/default_accept"), uidFromArgs, serviceTicket, userTicket),
              ResponseFactory().action(Action::reject)
                               .reason(Reason::uidsMismatch)
                               .uidsFromUserTicket(boost::make_optional(std::vector<std::string>{"0"}))
                               .source(module->sourceService1())
                               .product());
}

TEST_F(TvmGuardTest, shouldAcceptIfUidIsInTheTicketsUids) {
    initGuard(true);
    module->ticketBelongsToService1();
    module->userTicketWithUids({0ull, std::stoull(uidFromArgs)});
    EXPECT_EQ(guard->check(std::string("/default_accept"), uidFromArgs, serviceTicket, userTicket),
              ResponseFactory().action(Action::accept)
                               .reason(Reason::rule)
                               .uidsFromUserTicket(boost::make_optional(std::vector<std::string>{"0", uidFromArgs}))
                               .source(module->sourceService1())
                               .product());
}

TEST_F(TvmGuardTest, shouldResponseWithRuleDefaultActionInCaseOfServiceTicketWithErrorStatus) {
    module->ticketHasStatus(TA_EC_DEPRECATED);
    EXPECT_EQ(guard->check(std::string("/default_reject"), uidFromArgs, serviceTicket, none),
              ResponseFactory().action(Action::reject)
                               .reason(Reason::wrongServiceTicket)
                               .error(boost::system::error_code(TA_EC_DEPRECATED))
                               .product());

    EXPECT_EQ(guard->check(std::string("/default_accept"), uidFromArgs, serviceTicket, none),
              ResponseFactory().action(Action::accept)
                               .reason(Reason::ruleDefaultPolicy)
                               .error(boost::system::error_code(TA_EC_DEPRECATED))
                               .product());
}

TEST_F(TvmGuardTest, shouldResponseWithRuleDefaultActionInCaseOfUserTicketWithErrorStatus) {
    module->ticketBelongsToService1();
    module->userTicketHasStatus(TA_EC_DEPRECATED);
    EXPECT_EQ(guard->check(std::string("/default_reject"), uidFromArgs, serviceTicket, userTicket),
              ResponseFactory().action(Action::reject)
                               .reason(Reason::wrongUserTicket)
                               .source(module->sourceService1())
                               .error(boost::system::error_code(TA_EC_DEPRECATED))
                               .product());

    EXPECT_EQ(guard->check(std::string("/default_accept"), uidFromArgs, serviceTicket, userTicket),
              ResponseFactory().action(Action::accept)
                               .reason(Reason::ruleDefaultPolicy)
                               .source(module->sourceService1())
                               .error(boost::system::error_code(TA_EC_DEPRECATED))
                               .product());
}

}
