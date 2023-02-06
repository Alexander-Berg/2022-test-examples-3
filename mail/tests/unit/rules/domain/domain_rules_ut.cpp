#include <mail/notsolitesrv/src/rules/domain/domain_rules.h>

#include <mail/notsolitesrv/src/message/parser.h>

#include <mail/notsolitesrv/tests/unit/fakes/context.h>

#include <mail/notsolitesrv/tests/unit/mocks/furita_client.h>
#include <mail/notsolitesrv/tests/unit/mocks/tupita_client.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <boost/algorithm/string/join.hpp>

#include <memory>
#include <stdexcept>
#include <string>
#include <utility>
#include <vector>

namespace {

using namespace testing;

using NNotSoLiteSrv::EError;
using NNotSoLiteSrv::make_error_code;
using NNotSoLiteSrv::NFurita::TGetResult;
using NNotSoLiteSrv::NMetaSaveOp::TRecipientMap;
using NNotSoLiteSrv::NMetaSaveOp::TRequest;
using NNotSoLiteSrv::NRules::TDomainRules;
using NNotSoLiteSrv::NRules::TDomainRulesCallback;
using NNotSoLiteSrv::NRules::TDomainRulesClients;
using NNotSoLiteSrv::NRules::TDomainRulesPtr;
using NNotSoLiteSrv::NTupita::TCheckResult;
using NNotSoLiteSrv::NUser::ELoadStatus;
using NNotSoLiteSrv::NUser::TStorage;
using NNotSoLiteSrv::NUser::TStoragePtr;
using NNotSoLiteSrv::ParseMessage;
using NNotSoLiteSrv::TContextPtr;
using NNotSoLiteSrv::TErrorCode;
using NNotSoLiteSrv::TMessagePtr;
using NNotSoLiteSrv::TOrgId;
using NNotSoLiteSrv::TXYandexHint;

using TFuritaClientStrictMock = StrictMock<TFuritaClientMock>;
using TTupitaClientStrictMock = StrictMock<TTupitaClientMock>;

const std::string DOMAIN_ACTION_DROP{"drop"};
const std::string DOMAIN_ACTION_FORWARD{"forward"};

class TTestDomainRules : public Test {
protected:
    TDomainRulesClients MakeDomainRulesClients() const {
        return {FuritaClientMock, TupitaClientMock};
    }

    TStoragePtr MakeUserStorage() const {
        auto storage{std::make_shared<TStorage>()};
        auto uid{0ull};
        for (const auto& email : Emails) {
            storage->AddUser(email);
            const auto user{storage->GetUserByEmail(email)};
            user->Status = ELoadStatus::Found;
            user->Uid = std::to_string(uid++);
            user->DeliveryParams.NeedDelivery = true;
        }

        const auto initDomainRuleRelatedParams{[&](const auto& email, const auto& orgId,
            bool useDomainRules = true)
        {
            const auto user{storage->GetUserByEmail(email)};
            user->OrgId = orgId;
            user->DeliveryParams.UseDomainRules = useDomainRules;
        }};

        const auto dontUseDomainRules{false};
        initDomainRuleRelatedParams(Emails[1], OrgIds[0], dontUseDomainRules);
        initDomainRuleRelatedParams(Emails[2], OrgIds[0]);
        initDomainRuleRelatedParams(Emails[3], OrgIds[1]);
        initDomainRuleRelatedParams(Emails[4], OrgIds[1]);
        return storage;
    }

    TMessagePtr MakeMessage() const {
        const std::string separator{","};
        const std::string messageData{"From: local@domain.ru\r\nTo: " + boost::algorithm::join(Emails,
            separator) + "\r\nSubject: subject\r\n\r\nbody\r\n"};
        return ParseMessage(messageData, Context);
    }

    TRecipientMap MakeTestRecipientsWithoutDomainRulesInUse() const {
        return {{{}, {}}};
    }

    TRecipientMap MakeTestRecipientsWithDomainRulesInUse() const {
        TRecipientMap recipients;
        return {
            {"DeliveryId0", {}},
            {"DeliveryId1", {.user{.org_id{OrgIds[0]}}, .params{.use_domain_rules = true}}},
            {"DeliveryId2", {.user{.org_id{OrgIds[0]}}, .params{.use_domain_rules = true}}},
            {"DeliveryId3", {.user{.org_id{OrgIds[1]}}, .params{.use_domain_rules = true}}},
            {"DeliveryId4", {.user{.org_id{OrgIds[1]}}, .params{.use_domain_rules = true}}}
        };
    }

    TGetResult MakeTestGetResultWithDefaultRules() const {
        return {{.Rules{{}, {}}, .Revision = 0}};
    }

    TGetResult MakeTestGetResultWithIncorrectRule() const {
        return {{.Rules{{.Actions{{.Action{"incorrect"}}}}}, .Revision = 0}};
    }

    TGetResult MakeTestGetResult(const TOrgId& orgId) const {
        if (orgId == OrgIds[0]) {
            return {{
                .Rules {
                    {
                        .Terminal = false,
                        .Actions {
                            {.Action{DOMAIN_ACTION_FORWARD}, .Data{{.Email{DomainRuleForwards[0][0]}}}},
                            {.Action{DOMAIN_ACTION_DROP}}
                        }
                    }, {
                        .Terminal = true,
                        .Actions {
                            {.Action{DOMAIN_ACTION_FORWARD}, .Data{{.Email{DomainRuleForwards[0][1]}}}}
                        }
                    }, {
                        .Terminal = false,
                        .Actions {
                            {.Action{DOMAIN_ACTION_FORWARD}, .Data{{.Email{"forward2@domain2.ru"}}}}
                        }
                    }
                },
                .Revision = 0
            }};
        } else if (orgId == OrgIds[1]) {
            return {{
                .Rules {
                    {
                        .Terminal = false,
                        .Actions {
                            {.Action{DOMAIN_ACTION_FORWARD}, .Data{{.Email{DomainRuleForwards[1][0]}}}}
                        }
                    }
                },
                .Revision = 1
            }};
        }

        throw std::invalid_argument("Invalid orgId (" + orgId + ")");
    }

    TCheckResult MakeTestCheckResultWithNonnumericQueries() const {
        return {{.Result{{.MatchedQueries{"a"}}}}};
    }

    TCheckResult MakeTestCheckResultWithIncorrectIndices() const {
        return {{.Result{{.MatchedQueries{"2"}}}}};
    }

    TCheckResult MakeTestCheckResultWithIndexOfIncorrectRule() const {
        return {{.Result{{.MatchedQueries{"0"}}}}};
    }

    TCheckResult MakeTestCheckResult(const TOrgId& orgId) const {
        if (orgId == OrgIds[0]) {
            return {{.Result{{.MatchedQueries{DomainRuleIds[0][0], DomainRuleIds[0][1], "2"}}}}};
        } else if (orgId == OrgIds[1]) {
            return {{.Result{{.MatchedQueries{DomainRuleIds[1][0]}}}}};
        }

        throw std::invalid_argument("Invalid orgId (" + orgId + ")");
    }

    void CheckDomainRulesSuccess(const TErrorCode& errorCode) const {
        EXPECT_FALSE(errorCode);
    }

    bool XYandexHintForDroppedMessage(const TXYandexHint& hint) const {
        return
            (hint.folder.empty() &&
             (hint.folder_path == "\\inbox") &&
             (hint.folder_spam_path == "\\inbox") &&
             hint.fid.empty() &&
             !hint.filters &&
             !hint.copy_to_inbox &&
             !hint.save_to_sent &&
             hint.store_as_deleted);
    }

    void TestDomainRules(TRequest request, TDomainRulesCallback callback) {
        TDomainRulesPtr domainRules{std::make_shared<TDomainRules>(MakeDomainRulesClients(), Context,
            UserStorage, IoContext)};
        domainRules->SetParams(Message, std::move(request), std::move(callback));
        boost::asio::post(IoContext, [domainRules = std::move(domainRules)]{yplatform::spawn(domainRules);});
        IoContext.run();
    }

    const std::shared_ptr<TFuritaClientStrictMock> FuritaClientMock{
        std::make_shared<TFuritaClientStrictMock>()};
    const std::shared_ptr<TTupitaClientStrictMock> TupitaClientMock{
        std::make_shared<TTupitaClientStrictMock>()};
    const std::string SessionId{"SessionId"};
    const std::string EnvelopeId{"EnvelopeId"};
    TContextPtr Context{GetContext({}, SessionId, EnvelopeId)};
    const std::vector<std::string> Emails{"local0@domain0.ru", "local1@domain1.ru", "local2@domain2.ru",
        "local3@domain3.ru", "local4@domain4.ru"};
    const std::vector<TOrgId> OrgIds{"0", "1"};
    const std::vector<std::vector<std::string>> DomainRuleIds{{"0", "1"}, {"0"}};
    const std::vector<std::vector<std::string>> DomainRuleForwards{
        {"forward00@domain00.ru", "forward01@domain01.ru"},
        {"forward10@domain10.ru"}
    };

    const TStoragePtr UserStorage{MakeUserStorage()};
    boost::asio::io_context IoContext;
    const TMessagePtr Message{MakeMessage()};
    const std::string ExpectedSessionId{"SessionId"};
};

TEST_F(TTestDomainRules, for_domain_rules_not_in_use_must_succeed) {
    TRequest request;
    request.recipients = MakeTestRecipientsWithoutDomainRulesInUse();
    TestDomainRules(std::move(request), [&](auto errorCode){CheckDomainRulesSuccess(errorCode);});
}

TEST_F(TTestDomainRules, for_all_furita_get_errors_must_set_user_errors_and_succeed) {
    const InSequence sequence;
    EXPECT_CALL(*FuritaClientMock, Get(_, OrgIds[0], _)).WillOnce(WithArg<2>(
        [&](const auto& callback) {
            boost::asio::post(IoContext, [=]{callback(EError::FuritaResponseParseError, TGetResult{});});
        }));
    EXPECT_CALL(*FuritaClientMock, Get(_, OrgIds[1], _)).WillOnce(WithArg<2>(
        [&](const auto& callback) {
            boost::asio::post(IoContext, [=]{callback(TErrorCode{}, TGetResult{});});
        }));

    TRequest request;
    request.recipients = MakeTestRecipientsWithDomainRulesInUse();
    TestDomainRules(std::move(request), [&](auto errorCode){CheckDomainRulesSuccess(errorCode);});
    EXPECT_EQ((TErrorCode{}), UserStorage->GetUserByEmail(Emails[0])->DeliveryResult.ErrorCode);
    EXPECT_EQ((TErrorCode{}), UserStorage->GetUserByEmail(Emails[1])->DeliveryResult.ErrorCode);
    EXPECT_EQ(make_error_code(EError::FuritaResponseParseError), UserStorage->GetUserByEmail(
        Emails[2])->DeliveryResult.ErrorCode);
    EXPECT_EQ(make_error_code(EError::DomainRulesIncorrectResult), UserStorage->GetUserByEmail(
        Emails[3])->DeliveryResult.ErrorCode);
    EXPECT_EQ(make_error_code(EError::DomainRulesIncorrectResult), UserStorage->GetUserByEmail(
        Emails[4])->DeliveryResult.ErrorCode);
}

TEST_F(TTestDomainRules, for_furita_organizations_not_found_must_succeed) {
    const InSequence sequence;
    EXPECT_CALL(*FuritaClientMock, Get(_, OrgIds[0], _)).WillOnce(WithArg<2>(
        [&](const auto& callback) {
            boost::asio::post(IoContext, [=]{callback(EError::FuritaOrgNotFound, TGetResult{});});
        }));
    EXPECT_CALL(*FuritaClientMock, Get(_, OrgIds[1], _)).WillOnce(WithArg<2>(
        [&](const auto& callback) {
            boost::asio::post(IoContext, [=]{callback(EError::FuritaOrgNotFound, TGetResult{});});
        }));

    TRequest request;
    request.recipients = MakeTestRecipientsWithDomainRulesInUse();
    TestDomainRules(std::move(request), [&](auto errorCode){CheckDomainRulesSuccess(errorCode);});
    EXPECT_EQ((TErrorCode{}), UserStorage->GetUserByEmail(Emails[0])->DeliveryResult.ErrorCode);
    EXPECT_EQ((TErrorCode{}), UserStorage->GetUserByEmail(Emails[1])->DeliveryResult.ErrorCode);
    EXPECT_EQ((TErrorCode{}), UserStorage->GetUserByEmail(Emails[2])->DeliveryResult.ErrorCode);
    EXPECT_EQ((TErrorCode{}), UserStorage->GetUserByEmail(Emails[3])->DeliveryResult.ErrorCode);
    EXPECT_EQ((TErrorCode{}), UserStorage->GetUserByEmail(Emails[4])->DeliveryResult.ErrorCode);
}

TEST_F(TTestDomainRules, for_all_tupita_check_errors_must_set_user_errors_and_succeed) {
    const InSequence sequence;
    EXPECT_CALL(*FuritaClientMock, Get(_, OrgIds[0], _)).WillOnce(WithArg<2>(
        [&](const auto& callback) {
            boost::asio::post(IoContext, [=]{callback(TErrorCode{}, MakeTestGetResultWithDefaultRules());});
        }));
    EXPECT_CALL(*TupitaClientMock, Check(_, std::stoull(OrgIds[0]), ExpectedSessionId, _, _)).WillOnce(
        WithArg<4>([&](const auto& callback) {
            boost::asio::post(IoContext, [=]{callback(EError::TupitaResponseParseError, TCheckResult{});});
        }));

    EXPECT_CALL(*FuritaClientMock, Get(_, OrgIds[1], _)).WillOnce(WithArg<2>(
        [&](const auto& callback) {
            boost::asio::post(IoContext, [=]{callback(TErrorCode{}, MakeTestGetResultWithDefaultRules());});
        }));
    EXPECT_CALL(*TupitaClientMock, Check(_, std::stoull(OrgIds[1]), ExpectedSessionId, _, _)).WillOnce(
        WithArg<4>([&](const auto& callback) {
            boost::asio::post(IoContext, [=]{callback(TErrorCode{}, TCheckResult{});});
        }));

    TRequest request;
    request.recipients = MakeTestRecipientsWithDomainRulesInUse();
    request.session_id = ExpectedSessionId;
    TestDomainRules(std::move(request), [&](auto errorCode){CheckDomainRulesSuccess(errorCode);});
    EXPECT_EQ((TErrorCode{}), UserStorage->GetUserByEmail(Emails[0])->DeliveryResult.ErrorCode);
    EXPECT_EQ((TErrorCode{}), UserStorage->GetUserByEmail(Emails[1])->DeliveryResult.ErrorCode);
    EXPECT_EQ(make_error_code(EError::TupitaResponseParseError),
        UserStorage->GetUserByEmail(Emails[2])->DeliveryResult.ErrorCode);
    EXPECT_EQ(make_error_code(EError::DomainRulesIncorrectResult),
        UserStorage->GetUserByEmail(Emails[3])->DeliveryResult.ErrorCode);
    EXPECT_EQ(make_error_code(EError::DomainRulesIncorrectResult),
        UserStorage->GetUserByEmail(Emails[4])->DeliveryResult.ErrorCode);
}

TEST_F(TTestDomainRules, for_furita_get_and_tupita_check_errors_must_set_user_errors_and_succeed) {
    const InSequence sequence;
    EXPECT_CALL(*FuritaClientMock, Get(_, OrgIds[0], _)).WillOnce(WithArg<2>(
        [&](const auto& callback) {
            boost::asio::post(IoContext, [=]{callback(EError::FuritaResponseParseError, TGetResult{});});
        }));
    EXPECT_CALL(*FuritaClientMock, Get(_, OrgIds[1], _)).WillOnce(WithArg<2>(
        [&](const auto& callback) {
            boost::asio::post(IoContext, [=]{callback(TErrorCode{}, MakeTestGetResultWithDefaultRules());});
        }));

    EXPECT_CALL(*TupitaClientMock, Check(_, std::stoull(OrgIds[1]), ExpectedSessionId, _, _)).WillOnce(
        WithArg<4>([&](const auto& callback) {
            boost::asio::post(IoContext, [=]{callback(EError::TupitaResponseParseError, TCheckResult{});});
        }));

    TRequest request;
    request.recipients = MakeTestRecipientsWithDomainRulesInUse();
    request.session_id = ExpectedSessionId;
    TestDomainRules(std::move(request), [&](auto errorCode){CheckDomainRulesSuccess(errorCode);});
    EXPECT_EQ((TErrorCode{}), UserStorage->GetUserByEmail(Emails[0])->DeliveryResult.ErrorCode);
    EXPECT_EQ((TErrorCode{}), UserStorage->GetUserByEmail(Emails[1])->DeliveryResult.ErrorCode);
    EXPECT_EQ(make_error_code(EError::FuritaResponseParseError), UserStorage->GetUserByEmail(
        Emails[2])->DeliveryResult.ErrorCode);
    EXPECT_EQ(make_error_code(EError::TupitaResponseParseError), UserStorage->GetUserByEmail(
        Emails[3])->DeliveryResult.ErrorCode);
    EXPECT_EQ(make_error_code(EError::TupitaResponseParseError), UserStorage->GetUserByEmail(
        Emails[4])->DeliveryResult.ErrorCode);
}

TEST_F(TTestDomainRules, for_nonnumeric_matched_queries_must_succeed) {
    const InSequence sequence;
    EXPECT_CALL(*FuritaClientMock, Get(_, OrgIds[0], _)).WillOnce(WithArg<2>(
        [&](const auto& callback) {
            boost::asio::post(IoContext, [=]{callback(EError::FuritaResponseParseError, TGetResult{});});
        }));
    EXPECT_CALL(*FuritaClientMock, Get(_, OrgIds[1], _)).WillOnce(WithArg<2>(
        [&](const auto& callback) {
            boost::asio::post(IoContext, [=]{callback(TErrorCode{}, MakeTestGetResultWithDefaultRules());});
        }));

    EXPECT_CALL(*TupitaClientMock, Check(_, std::stoull(OrgIds[1]), ExpectedSessionId, _, _)).WillOnce(
        WithArg<4>([&](const auto& callback) {
            boost::asio::post(IoContext, [=]{callback(TErrorCode{},
                MakeTestCheckResultWithNonnumericQueries());});
        }));

    TRequest request;
    request.recipients = MakeTestRecipientsWithDomainRulesInUse();
    request.session_id = ExpectedSessionId;
    TestDomainRules(std::move(request), [&](auto errorCode){CheckDomainRulesSuccess(errorCode);});
    EXPECT_EQ((TErrorCode{}), UserStorage->GetUserByEmail(Emails[0])->DeliveryResult.ErrorCode);
    EXPECT_EQ((TErrorCode{}), UserStorage->GetUserByEmail(Emails[1])->DeliveryResult.ErrorCode);
    EXPECT_EQ(make_error_code(EError::FuritaResponseParseError), UserStorage->GetUserByEmail(
        Emails[2])->DeliveryResult.ErrorCode);
    EXPECT_EQ((TErrorCode{}), UserStorage->GetUserByEmail(Emails[3])->DeliveryResult.ErrorCode);
    EXPECT_EQ((TErrorCode{}), UserStorage->GetUserByEmail(Emails[4])->DeliveryResult.ErrorCode);
}

TEST_F(TTestDomainRules, for_incorrect_matched_domain_rule_indices_must_succeed) {
    const InSequence sequence;
    EXPECT_CALL(*FuritaClientMock, Get(_, OrgIds[0], _)).WillOnce(WithArg<2>(
        [&](const auto& callback) {
            boost::asio::post(IoContext, [=]{callback(EError::FuritaResponseParseError, TGetResult{});});
        }));
    EXPECT_CALL(*FuritaClientMock, Get(_, OrgIds[1], _)).WillOnce(WithArg<2>(
        [&](const auto& callback) {
            boost::asio::post(IoContext, [=]{callback(TErrorCode{}, MakeTestGetResultWithDefaultRules());});
        }));

    EXPECT_CALL(*TupitaClientMock, Check(_, std::stoull(OrgIds[1]), ExpectedSessionId, _, _)).WillOnce(
        WithArg<4>([&](const auto& callback) {
            boost::asio::post(IoContext, [=]{callback(TErrorCode{},
                MakeTestCheckResultWithIncorrectIndices());});
        }));

    TRequest request;
    request.recipients = MakeTestRecipientsWithDomainRulesInUse();
    request.session_id = ExpectedSessionId;
    TestDomainRules(std::move(request), [&](auto errorCode){CheckDomainRulesSuccess(errorCode);});
    EXPECT_EQ((TErrorCode{}), UserStorage->GetUserByEmail(Emails[0])->DeliveryResult.ErrorCode);
    EXPECT_EQ((TErrorCode{}), UserStorage->GetUserByEmail(Emails[1])->DeliveryResult.ErrorCode);
    EXPECT_EQ(make_error_code(EError::FuritaResponseParseError), UserStorage->GetUserByEmail(
        Emails[2])->DeliveryResult.ErrorCode);
    EXPECT_EQ((TErrorCode{}), UserStorage->GetUserByEmail(Emails[3])->DeliveryResult.ErrorCode);
    EXPECT_EQ((TErrorCode{}), UserStorage->GetUserByEmail(Emails[4])->DeliveryResult.ErrorCode);
}

TEST_F(TTestDomainRules, for_incorrect_matched_domain_rules_must_succeed) {
    const InSequence sequence;
    EXPECT_CALL(*FuritaClientMock, Get(_, OrgIds[0], _)).WillOnce(WithArg<2>(
        [&](const auto& callback) {
            boost::asio::post(IoContext, [=]{callback(EError::FuritaResponseParseError, TGetResult{});});
        }));
    EXPECT_CALL(*FuritaClientMock, Get(_, OrgIds[1], _)).WillOnce(WithArg<2>(
        [&](const auto& callback) {
            boost::asio::post(IoContext, [=]{callback(TErrorCode{}, MakeTestGetResultWithIncorrectRule());});
        }));

    EXPECT_CALL(*TupitaClientMock, Check(_, std::stoull(OrgIds[1]), ExpectedSessionId, _, _)).WillOnce(
        WithArg<4>([&](const auto& callback) {
            boost::asio::post(IoContext, [=]{callback(TErrorCode{},
                MakeTestCheckResultWithIndexOfIncorrectRule());});
        }));

    TRequest request;
    request.recipients = MakeTestRecipientsWithDomainRulesInUse();
    request.session_id = ExpectedSessionId;
    TestDomainRules(std::move(request), [&](auto errorCode){CheckDomainRulesSuccess(errorCode);});
    EXPECT_EQ((TErrorCode{}), UserStorage->GetUserByEmail(Emails[0])->DeliveryResult.ErrorCode);
    EXPECT_EQ((TErrorCode{}), UserStorage->GetUserByEmail(Emails[1])->DeliveryResult.ErrorCode);
    EXPECT_EQ(make_error_code(EError::FuritaResponseParseError), UserStorage->GetUserByEmail(
        Emails[2])->DeliveryResult.ErrorCode);
    EXPECT_EQ((TErrorCode{}), UserStorage->GetUserByEmail(Emails[3])->DeliveryResult.ErrorCode);
    EXPECT_EQ((TErrorCode{}), UserStorage->GetUserByEmail(Emails[4])->DeliveryResult.ErrorCode);
}

TEST_F(TTestDomainRules, for_correct_input_data_must_drop_message_and_add_forwards) {
    const InSequence sequence;
    EXPECT_CALL(*FuritaClientMock, Get(_, OrgIds[0], _)).WillOnce(WithArg<2>(
        [&](const auto& callback) {
            boost::asio::post(IoContext, [=]{callback(TErrorCode{}, MakeTestGetResult(OrgIds[0]));});
        }));
    EXPECT_CALL(*TupitaClientMock, Check(_, std::stoull(OrgIds[0]), ExpectedSessionId, _, _)).WillOnce(
        WithArg<4>([&](const auto& callback) {
            boost::asio::post(IoContext, [=]{callback(TErrorCode{}, MakeTestCheckResult(OrgIds[0]));});
        }));

    EXPECT_CALL(*FuritaClientMock, Get(_, OrgIds[1], _)).WillOnce(WithArg<2>(
        [&](const auto& callback) {
            boost::asio::post(IoContext, [=]{callback(TErrorCode{}, MakeTestGetResult(OrgIds[1]));});
        }));
    EXPECT_CALL(*TupitaClientMock, Check(_, std::stoull(OrgIds[1]), ExpectedSessionId, _, _)).WillOnce(
        WithArg<4>([&](const auto& callback) {
            boost::asio::post(IoContext, [=]{callback(TErrorCode{}, MakeTestCheckResult(OrgIds[1]));});
        }));

    TRequest request;
    request.recipients = MakeTestRecipientsWithDomainRulesInUse();
    request.session_id = ExpectedSessionId;
    TestDomainRules(std::move(request), [&](auto errorCode){CheckDomainRulesSuccess(errorCode);});
    const auto& user0{*UserStorage->GetUserByEmail(Emails[0])};
    EXPECT_EQ((TErrorCode{}), user0.DeliveryResult.ErrorCode);
    const auto& user1{*UserStorage->GetUserByEmail(Emails[1])};
    EXPECT_EQ((TErrorCode{}), user1.DeliveryResult.ErrorCode);
    const auto& user2{*UserStorage->GetUserByEmail(Emails[2])};
    EXPECT_EQ((TErrorCode{}), user2.DeliveryResult.ErrorCode);
    const auto& user3{*UserStorage->GetUserByEmail(Emails[3])};
    EXPECT_EQ((TErrorCode{}), user3.DeliveryResult.ErrorCode);
    const auto& user4{*UserStorage->GetUserByEmail(Emails[4])};
    EXPECT_EQ((TErrorCode{}), user4.DeliveryResult.ErrorCode);

    EXPECT_TRUE(user0.DeliveryResult.DomainRuleIds.empty());
    EXPECT_TRUE(user1.DeliveryResult.DomainRuleIds.empty());
    EXPECT_EQ(DomainRuleIds[0], user2.DeliveryResult.DomainRuleIds);
    EXPECT_EQ(DomainRuleIds[1], user3.DeliveryResult.DomainRuleIds);
    EXPECT_EQ(DomainRuleIds[1], user4.DeliveryResult.DomainRuleIds);

    EXPECT_TRUE(user0.DeliveryResult.DomainRuleForwards.empty());
    EXPECT_TRUE(user1.DeliveryResult.DomainRuleForwards.empty());
    EXPECT_EQ(DomainRuleForwards[0], user2.DeliveryResult.DomainRuleForwards);
    EXPECT_EQ(DomainRuleForwards[1], user3.DeliveryResult.DomainRuleForwards);
    EXPECT_EQ(DomainRuleForwards[1], user4.DeliveryResult.DomainRuleForwards);

    EXPECT_FALSE(XYandexHintForDroppedMessage(Message->GetXYHintByUid(user0.Uid)));
    EXPECT_FALSE(XYandexHintForDroppedMessage(Message->GetXYHintByUid(user1.Uid)));
    EXPECT_TRUE(XYandexHintForDroppedMessage(Message->GetXYHintByUid(user2.Uid)));
    EXPECT_FALSE(XYandexHintForDroppedMessage(Message->GetXYHintByUid(user3.Uid)));
    EXPECT_FALSE(XYandexHintForDroppedMessage(Message->GetXYHintByUid(user3.Uid)));
}

}
