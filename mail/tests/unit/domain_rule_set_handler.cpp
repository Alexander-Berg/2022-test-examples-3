#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/furita/src/api/domain_rules_set/handler_impl.h>
#include <mail/furita/src/api/domain_rules_set/error_code.h>
#include <mail/furita/src/api/domain_rules_set/utils.h>
#include <mail/furita/src/api/domain_rules_set/types.h>
#include <mail/furita/ymod_db/tests/mock_repository.h>
#include <mail/pgg/include/pgg/error.h>

#include <yplatform/json.h>

#include <cstdint>

using namespace testing;
namespace utils = furita::domain_rules_set::utils;

struct TTupitaClientMock : furita::tupita::ITupitaClient {
    MOCK_METHOD(void, DoRequest,
        (furita::TContextPtr ctx, const furita::tupita::TTupitaRequest& request, furita::tupita::TTupitaClientCallback callback)
    );
};

struct TBlackBoxClientMock : furita::blackbox::IBlackBoxClient {
    MOCK_METHOD(void, DoRequest,
        (furita::TContextPtr ctx, furita::blackbox::TBlackBoxRequest, furita::blackbox::TBlackBoxClientCallback callback)
    );
};

struct TTestDomainRulesSetHandler : Test {
    TTestDomainRulesSetHandler() 
        : Ctx(std::make_shared<furita::TContext>())
        , Repo(std::make_shared<StrictMock<furita::MockRepository>>())
        , TupitaClient(std::make_shared<StrictMock<TTupitaClientMock>>())
        , BlackBoxClient(std::make_shared<StrictMock<TBlackBoxClientMock>>())
        , Handler(std::make_shared<furita::domain_rules_set::THandlerImpl>(Repo, TupitaClient, BlackBoxClient, IoContext)) {}

    furita::domain_rules_set::TRequest MakeRequest() {
        return furita::domain_rules_set::TRequest {
            .OrgId = 42,
            .Rules = {
                {
                    .Terminal = true,
                    .Condition = utils::FromJson(R"({"foo": "bar"})"),
                    .ConditionQuery = "foofoo"
                }
            }
        };
    }

    furita::domain_rules_set::TRequest MakeRequestWithLongCondition() {
        std::string longString(1024 * 1024 * 5, 'a');
        return furita::domain_rules_set::TRequest {
            .OrgId = 42,
            .Rules = {
                {
                    .Terminal = true,
                    .Condition = utils::FromJson("{\"foo\": \"" + longString + "\"}"),
                    .ConditionQuery = "foofoo"
                }
            }
        };
    }

    furita::domain_rules_set::TRequest MakeRequestWithTooManyActions() {
        std::vector<furita::domain_rules_set::TAction> actions{200,
            furita::domain_rules_set::TAction{.Action = "drop"}};
        return furita::domain_rules_set::TRequest {
            .OrgId = 42,
            .Rules = {
                {
                    .Terminal = true,
                    .Condition = utils::FromJson("{\"foo\": \"bar\"}"),
                    .ConditionQuery = "foofoo",
                    .Actions = std::move(actions)
                }
            }
        };
    }

    furita::domain_rules_set::TRequest MakeRequestWithTooManyRules() {
        return furita::domain_rules_set::TRequest {
            .OrgId = 42,
            .Rules = {
                300,
                {
                    .Terminal = true,
                    .Condition = utils::FromJson("{\"foo\": \"bar\"}"),
                    .ConditionQuery = "foofoo"
                }
            }
        };
    }

    furita::TContextPtr Ctx;
    std::shared_ptr<StrictMock<furita::MockRepository>> Repo;
    std::shared_ptr<StrictMock<TTupitaClientMock>> TupitaClient;
    std::shared_ptr<StrictMock<TBlackBoxClientMock>> BlackBoxClient;
    boost::asio::io_context IoContext;
    std::shared_ptr<furita::domain_rules_set::IHandler> Handler;
};

TEST_F(TTestDomainRulesSetHandler, for_request_with_too_long_condition_should_return_error) {
    Handler->Run(Ctx, MakeRequestWithLongCondition(), [](boost::system::error_code ec, furita::domain_rules_set::TResponse) {
        EXPECT_TRUE(ec);
        EXPECT_EQ(ec, make_error_code(furita::domain_rules_set::EError::LimitsViolation));
    });
    IoContext.run();
}

TEST_F(TTestDomainRulesSetHandler, for_request_with_too_many_actions_should_return_error) {
    Handler->Run(Ctx, MakeRequestWithTooManyActions(), [](boost::system::error_code ec, furita::domain_rules_set::TResponse) {
        EXPECT_TRUE(ec);
        EXPECT_EQ(ec, make_error_code(furita::domain_rules_set::EError::LimitsViolation));
    });
    IoContext.run();
}

TEST_F(TTestDomainRulesSetHandler, for_request_with_too_many_rules_should_return_error) {
    Handler->Run(Ctx, MakeRequestWithTooManyRules(), [](boost::system::error_code ec, furita::domain_rules_set::TResponse) {
        EXPECT_TRUE(ec);
        EXPECT_EQ(ec, make_error_code(furita::domain_rules_set::EError::LimitsViolation));
    });
    IoContext.run();
}

TEST_F(TTestDomainRulesSetHandler, for_valid_request_and_blackbox_failed_should_return_error) {
    InSequence s;
    EXPECT_CALL(*BlackBoxClient, DoRequest(_, _, _)).WillOnce(InvokeArgument<2>(
        make_error_code(furita::blackbox::EError::Unknown),
        furita::blackbox::TBlackBoxResponse {}
    ));
    Handler->Run(Ctx, MakeRequest(), [](boost::system::error_code ec, furita::domain_rules_set::TResponse) {
        EXPECT_TRUE(ec);
        EXPECT_EQ(ec, make_error_code(furita::blackbox::EError::Unknown));
    });
    IoContext.run();
}

TEST_F(TTestDomainRulesSetHandler, for_valid_request_and_blackbox_return_different_orgids_should_return_error) {
    InSequence s;
    EXPECT_CALL(*BlackBoxClient, DoRequest(_, _, _)).WillOnce(InvokeArgument<2>(
        make_error_code(furita::blackbox::EError::Ok),
        furita::blackbox::TBlackBoxResponse {
            .OrgIds = {"42", "45"}
        }
    ));
    Handler->Run(Ctx, MakeRequest(), [](boost::system::error_code ec, furita::domain_rules_set::TResponse) {
        EXPECT_TRUE(ec);
        EXPECT_EQ(ec, make_error_code(furita::domain_rules_set::EError::EmailDoesntBelongToOrganization));
    });
    IoContext.run();
}

TEST_F(TTestDomainRulesSetHandler, for_valid_request_and_blackbox_ok_and_tupita_ok_and_repository_ok_should_return_ok) {
    InSequence s;
    EXPECT_CALL(*BlackBoxClient, DoRequest(_, _, _)).WillOnce(InvokeArgument<2>(
        make_error_code(furita::blackbox::EError::Ok),
        furita::blackbox::TBlackBoxResponse {
            .OrgIds = {"42"}
        }
    ));
    EXPECT_CALL(*TupitaClient, DoRequest(_, _, _)).WillOnce(InvokeArgument<2>(
            make_error_code(furita::tupita::EError::Ok),
            furita::tupita::TTupitaResponse {
                .Queries = {"some_cond_query"}
            }
        )
    );
    EXPECT_CALL(*Repo, asyncSetDomainRules(_, _)).WillOnce(InvokeArgument<1>(
        pgg::error::make_error_code(pgg::error::CommonErrors::ok),
        furita::Revision{25})
    );
    Handler->Run(Ctx, MakeRequest(), [](boost::system::error_code ec, furita::domain_rules_set::TResponse resp) {
        EXPECT_FALSE(ec);
        EXPECT_EQ(resp.Message, "Revision id: 25");
    });
    IoContext.run();
}

TEST_F(TTestDomainRulesSetHandler, for_valid_request_and_blackbox_ok_and_tupita_ok_but_different_number_of_queries_should_return_error) {
    InSequence s;
    EXPECT_CALL(*BlackBoxClient, DoRequest(_, _, _)).WillOnce(InvokeArgument<2>(
        make_error_code(furita::blackbox::EError::Ok),
        furita::blackbox::TBlackBoxResponse {
            .OrgIds = {"42"}
        }
    ));
    EXPECT_CALL(*TupitaClient, DoRequest(_, _, _)).WillOnce(InvokeArgument<2>(
            make_error_code(furita::tupita::EError::Ok),
            furita::tupita::TTupitaResponse {
                .Queries = {"some_cond_query", "another_cond_query"}
            }
        )
    );
    Handler->Run(Ctx, MakeRequest(), [](boost::system::error_code ec, furita::domain_rules_set::TResponse) {
        EXPECT_EQ(ec, make_error_code(furita::domain_rules_set::EError::ConditionQueriesCountError));
    });
    IoContext.run();
}

TEST_F(TTestDomainRulesSetHandler, for_valid_request_and_blackbox_ok_but_tupita_failed_should_return_error) {
    InSequence s;
    EXPECT_CALL(*BlackBoxClient, DoRequest(_, _, _)).WillOnce(InvokeArgument<2>(
        make_error_code(furita::blackbox::EError::Ok),
        furita::blackbox::TBlackBoxResponse {
            .OrgIds = {"42"}
        }
    ));
    EXPECT_CALL(*TupitaClient, DoRequest(_, _, _)).WillOnce(InvokeArgument<2>(
            make_error_code(furita::tupita::EError::RequestParseError),
            furita::tupita::TTupitaResponse{}
        )
    );
    Handler->Run(Ctx, MakeRequest(), [](boost::system::error_code ec, furita::domain_rules_set::TResponse) {
        EXPECT_EQ(ec, make_error_code(furita::tupita::EError::RequestParseError));
    });
    IoContext.run();
}

TEST_F(TTestDomainRulesSetHandler, for_valid_request_and_blackbox_ok_and_tupita_ok_but_repository_failed_should_return_error) {
    InSequence s;
    EXPECT_CALL(*BlackBoxClient, DoRequest(_, _, _)).WillOnce(InvokeArgument<2>(
        make_error_code(furita::blackbox::EError::Ok),
        furita::blackbox::TBlackBoxResponse {
            .OrgIds = {"42"}
        }
    ));
    EXPECT_CALL(*TupitaClient, DoRequest(_, _, _)).WillOnce(InvokeArgument<2>(
            make_error_code(furita::tupita::EError::Ok),
            furita::tupita::TTupitaResponse {
                .Queries = {"some_cond_query"}
            }
        )
    );
    EXPECT_CALL(*Repo, asyncSetDomainRules(_, _)).WillOnce(InvokeArgument<1>(
        make_error_code(pgg::error::CommonErrors::noDataReceived),
        furita::Revision{0})
    );
    Handler->Run(Ctx, MakeRequest(), [](boost::system::error_code ec, furita::domain_rules_set::TResponse resp) {
        EXPECT_EQ(ec, make_error_code(pgg::error::CommonErrors::noDataReceived));
        EXPECT_EQ(resp.Message, "Database error occurred");
    });
    IoContext.run();
}
