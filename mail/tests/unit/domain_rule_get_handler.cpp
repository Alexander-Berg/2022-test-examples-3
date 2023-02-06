#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/furita/src/api/domain_rules_get/handler_impl.h>
#include <mail/furita/src/api/domain_rules_get/utils.h>
#include <mail/furita/src/api/domain_rules_get/types.h>
#include <mail/furita/ymod_db/tests/mock_repository.h>
#include <mail/pgg/include/pgg/error.h>

#include <yplatform/json.h>

using namespace testing;
namespace utils = furita::domain_rules_get::utils;

const std::string rulesJson = R"({
        "rules": [
            {
                "terminal": true, 
                "condition": {"foo": "bar"}, 
                "condition_query": "condq1",
                "actions": [
                    {
                        "action": "drop"
                    }
                ]
            },
            {
                "terminal": false, 
                "condition": {"bar": "foo"}, 
                "actions": [
                    {
                        "action": "forward",
                        "data": { "email": "foo" }
                    }
                ]
            },
            {
                "terminal": false, 
                "condition": {"bar": "foo"}, 
                "condition_query": "condq2",
                "actions": [
                    {
                        "action": "forward",
                        "data": { "email": "foo" }
                    }
                ]
            }
        ]
    })";

struct TTestDomainRulesGetHandler : public Test {
    TTestDomainRulesGetHandler() 
        : Ctx(std::make_shared<furita::TContext>())
        , Repo(std::make_shared<StrictMock<furita::MockRepository>>())
        , Handler(std::make_shared<furita::domain_rules_get::THandlerImpl>(Repo, IoContext)) {}

    furita::domain_rules_get::TRequest MakeRequest() {
        return furita::domain_rules_get::TRequest {
            .OrgId = 42
        };
    }

    furita::TContextPtr Ctx;
    std::shared_ptr<StrictMock<furita::MockRepository>> Repo;
    boost::asio::io_context IoContext;
    std::shared_ptr<furita::domain_rules_get::IHandler> Handler;
};

TEST_F(TTestDomainRulesGetHandler, for_valid_request_and_repository_ok_should_return_ok) {
    EXPECT_CALL(*Repo, asyncGetDomainRules(_, _)).WillOnce(InvokeArgument<1>(
        pgg::error::make_error_code(pgg::error::CommonErrors::ok),
        furita::DomainRules {
            .orgId = furita::OrgId{42},
            .revision = furita::Revision{1},
            .rules = furita::Rules{rulesJson}
        }
    ));
    Handler->Run(Ctx, MakeRequest(), [](boost::system::error_code ec, furita::domain_rules_get::TResponse resp) {
        EXPECT_FALSE(ec);
        EXPECT_TRUE(resp.Rules);
        EXPECT_EQ(utils::FromJson(*resp.Rules), utils::FromJson(rulesJson));
    });
    IoContext.run();
}

TEST_F(TTestDomainRulesGetHandler, for_valid_request_and_repository_failed_should_return_error) {
    EXPECT_CALL(*Repo, asyncGetDomainRules(_, _)).WillOnce(InvokeArgument<1>(
        pgg::error::make_error_code(pgg::error::CommonErrors::noDataReceived),
        furita::DomainRules{}
    ));
    Handler->Run(Ctx, MakeRequest(), [](boost::system::error_code ec, furita::domain_rules_get::TResponse) {
        EXPECT_EQ(ec, make_error_code(pgg::error::CommonErrors::noDataReceived));
    });
    IoContext.run();
}
