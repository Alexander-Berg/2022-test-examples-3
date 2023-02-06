#include <mail/notsolitesrv/src/meta_save_op/meta_save_op.h>

#include <mail/notsolitesrv/tests/unit/fakes/context.h>

#include <mail/notsolitesrv/tests/unit/mocks/firstline.h>
#include <mail/notsolitesrv/tests/unit/mocks/furita_client.h>
#include <mail/notsolitesrv/tests/unit/mocks/mdbsave_client.h>
#include <mail/notsolitesrv/tests/unit/mocks/mthr.h>
#include <mail/notsolitesrv/tests/unit/mocks/tupita_client.h>

#include <gtest/gtest.h>

#include <utility>

namespace {

using namespace testing;

using NNotSoLiteSrv::EError;
using NNotSoLiteSrv::NFirstline::TFirstlineMock;
using NNotSoLiteSrv::NFirstline::TFirstlineResult;
using NNotSoLiteSrv::NFurita::TListResult;
using NNotSoLiteSrv::NMetaSaveOp::TMetaSaveOp;
using NNotSoLiteSrv::NMetaSaveOp::TMetaSaveOpCallback;
using NNotSoLiteSrv::NMetaSaveOp::TMetaSaveOpClients;
using NNotSoLiteSrv::NMetaSaveOp::TMetaSaveOpComponents;
using NNotSoLiteSrv::NMetaSaveOp::TMetaSaveOpPtr;
using NNotSoLiteSrv::NMetaSaveOp::TRecipientMap;
using NNotSoLiteSrv::NMetaSaveOp::TRequest;
using NNotSoLiteSrv::NMdbSave::TMdbSaveResult;
using NNotSoLiteSrv::NMthr::TMthrResult;
using NNotSoLiteSrv::NTupita::TCheckResult;
using NNotSoLiteSrv::NUser::TStorage;
using NNotSoLiteSrv::TContextPtr;
using NNotSoLiteSrv::TDeliveryId;
using NNotSoLiteSrv::TErrorCode;
using NNotSoLiteSrv::TUid;

using TFirstlineStrictMock = StrictMock<TFirstlineMock>;
using TFuritaClientStrictMock = StrictMock<TFuritaClientMock>;
using TMdbSaveClientStrictMock = StrictMock<TMdbSaveClientMock>;
using TMthrStrictMock = StrictMock<TMthrMock>;
using TTupitaClientStrictMock = StrictMock<TTupitaClientMock>;

struct TTestMetaSaveOp : Test {
    TMetaSaveOpClients MakeMetaSaveOpClients() const {
        return {FuritaClientMock, MdbSaveClientMock, TupitaClientMock};
    }

    TMetaSaveOpComponents MakeMetaSaveOpComponents() const {
        return {FirstlineMock, MthrMock};
    }

    TRequest MakeTestRequest() const {
        TRequest request;
        request.session_id = Context->GetFullSessionId();
        return request;
    }

    TRecipientMap MakeTestRecipientsWithFiltersInUse() const {
        return {{DeliveryId0, {.user{.uid = Uid0}}}, {DeliveryId1, {.user{.uid = Uid1}}}};
    }

    TRecipientMap MakeTestRecipientsWithoutFiltersInUse() const {
        return {
            {DeliveryId0, {.user{.uid = Uid0}, .params{.use_filters = false}}},
            {DeliveryId1, {.user{.uid = Uid1}, .params{.use_filters = false}}}
        };
    }

    TFirstlineResult MakeTestFirstlineResult() const {
        return {{.Firstline{"Firstline"}}};
    }

    TListResult MakeTestListResult(bool enabled) const {
        return {{.Rules{{.Enabled = enabled, .Actions{{.Verified = true}}}}}};
    }

    TCheckResult MakeTestCheckResult() const {
        return {{}};
    }

    TMthrResult MakeTestMthrResult() const {
        return {{}};
    }

    TMdbSaveResult MakeTestMdbSaveResult() const {
        return {{.Rcpts {
            {.Id{DeliveryId0}, .Rcpt{.Status = "ok", .Mid = "Mid0", .Duplicate = false}},
            {.Id{DeliveryId1}, .Rcpt{.Status = "ok", .Mid = "Mid1", .Duplicate = false}}
        }}};
    }

    void ExpectFirstlineSucceeded() {
        EXPECT_CALL(*FirstlineMock, Firstline(_, _, _, _)).WillOnce(WithArg<2>(
            [&](const auto& callback) {
                boost::asio::post(IoContext, [=]{callback(TErrorCode{}, MakeTestFirstlineResult());});
            }));
    }

    void ExpectFuritaSucceeded(TUid uid, bool ruleEnabled = true) {
        EXPECT_CALL(*FuritaClientMock, List(_, uid, _)).WillOnce(WithArg<2>(
            [=](const auto& callback) {
                boost::asio::post(IoContext, [=]{callback(TErrorCode{}, MakeTestListResult(ruleEnabled));});
            }));
    }

    void ExpectTupitaSucceeded(TUid uid) {
        EXPECT_CALL(*TupitaClientMock, Check(_, uid, Context->GetFullSessionId(), _, _)).WillOnce(WithArg<4>(
            [&](const auto& callback) {
                boost::asio::post(IoContext, [=]{callback(TErrorCode{}, MakeTestCheckResult());});
            }));
    }

    void ExpectMthrSucceeded() {
        EXPECT_CALL(*MthrMock, GetThreadInfo(_, _)).WillOnce(Return(MakeTestMthrResult()));
    }

    void ExpectMdbsaveSucceeded() {
        EXPECT_CALL(*MdbSaveClientMock, MdbSave(_, Context->GetFullSessionId(), _, _)).WillOnce(WithArg<3>(
            [&](const auto& callback) {
                boost::asio::post(IoContext, [=]{callback(TErrorCode{}, MakeTestMdbSaveResult());});
            }));
    }

    void CheckMetaSaveOpError(const TErrorCode& errorCode) const {
        ASSERT_TRUE(errorCode);
        EXPECT_EQ(EError::MetaSaveOpError, errorCode);
    }

    void CheckMetaSaveOpSuccess(const TErrorCode& errorCode) const {
        EXPECT_FALSE(errorCode);
    }

    void TestMetaSaveOp(TRequest request, TMetaSaveOpCallback callback) {
        auto clients{MakeMetaSaveOpClients()};
        auto components{MakeMetaSaveOpComponents()};
        auto storage{std::make_shared<TStorage>()};
        TMetaSaveOpPtr metaSaveOp{std::make_shared<TMetaSaveOp>(std::move(clients), std::move(components),
            Context, std::move(storage), IoContext)};
        metaSaveOp->SetOpParams(std::move(request), std::move(callback));
        boost::asio::post(IoContext, [metaSaveOp = std::move(metaSaveOp)]{yplatform::spawn(metaSaveOp);});
        IoContext.run();
    }

    const std::shared_ptr<TFirstlineStrictMock> FirstlineMock{
        std::make_shared<TFirstlineStrictMock>()};
    const std::shared_ptr<TFuritaClientStrictMock> FuritaClientMock{
        std::make_shared<TFuritaClientStrictMock>()};
    const std::shared_ptr<TMdbSaveClientStrictMock> MdbSaveClientMock{
        std::make_shared<TMdbSaveClientStrictMock>()};
    const std::shared_ptr<TMthrStrictMock> MthrMock{std::make_shared<TMthrStrictMock>()};
    const std::shared_ptr<TTupitaClientStrictMock> TupitaClientMock{
        std::make_shared<TTupitaClientStrictMock>()};
    const std::string SessionId{"SessionId"};
    const std::string EnvelopeId{"EnvelopeId"};
    TContextPtr Context{GetContext({{"meta_save_op_drop_async", "false"}}, SessionId, EnvelopeId)};
    const TDeliveryId DeliveryId0{"DeliveryId0"};
    const TDeliveryId DeliveryId1{"DeliveryId1"};
    const TUid Uid0{0};
    const TUid Uid1{1};
    boost::asio::io_context IoContext;
};

TEST_F(TTestMetaSaveOp, for_async_dlv_and_drop_async_set_must_return_error) {
    auto request{MakeTestRequest()};
    Context = GetContext({{"meta_save_op_drop_async", "true"}}, SessionId, EnvelopeId);
    TestMetaSaveOp(std::move(request), [&](auto errorCode){CheckMetaSaveOpError(errorCode);});
}

TEST_F(TTestMetaSaveOp, for_firstline_error_must_return_error) {
    EXPECT_CALL(*FirstlineMock, Firstline(_, _, _, _)).WillOnce(WithArg<2>(
        [&](const auto& callback) {
            boost::asio::post(IoContext, [=]{callback(EError::FirstlineError,
                TFirstlineResult{});});
        }));

    auto request{MakeTestRequest()};
    TestMetaSaveOp(std::move(request), [&](auto errorCode){CheckMetaSaveOpError(errorCode);});
}

TEST_F(TTestMetaSaveOp, for_mthr_error_must_return_error) {
    const InSequence sequence;
    ExpectFirstlineSucceeded();
    auto request{MakeTestRequest()};
    request.recipients = MakeTestRecipientsWithFiltersInUse();
    auto uid{Uid0};
    for (auto index{0u}; index < request.recipients.size(); ++index, ++uid) {
        ExpectFuritaSucceeded(uid);
        ExpectTupitaSucceeded(uid);
    }

    EXPECT_CALL(*MthrMock, GetThreadInfo(_, _)).WillOnce(Return(TMthrResult{}));
    TestMetaSaveOp(std::move(request), [&](auto errorCode){CheckMetaSaveOpError(errorCode);});
}

TEST_F(TTestMetaSaveOp, for_mdbsave_error_must_return_error) {
    const InSequence sequence;
    ExpectFirstlineSucceeded();
    ExpectMthrSucceeded();
    EXPECT_CALL(*MdbSaveClientMock, MdbSave(_, Context->GetFullSessionId(), _, _)).WillOnce(WithArg<3>(
        [&](const auto& callback) {
            boost::asio::post(IoContext, [=]{callback(EError::MdbSaveResponseParseError, TMdbSaveResult{});});
        }));

    auto request{MakeTestRequest()};
    request.recipients = MakeTestRecipientsWithoutFiltersInUse();
    TestMetaSaveOp(std::move(request), [&](auto errorCode){CheckMetaSaveOpError(errorCode);});
}

TEST_F(TTestMetaSaveOp, for_all_furita_errors_must_succeed) {
    const InSequence sequence;
    ExpectFirstlineSucceeded();
    auto request{MakeTestRequest()};
    request.recipients = MakeTestRecipientsWithFiltersInUse();
    auto uid{Uid0};
    for (auto index{0u}; index < request.recipients.size(); ++index, ++uid) {
        EXPECT_CALL(*FuritaClientMock, List(_, uid, _)).WillOnce(WithArg<2>(
            [&](const auto& callback) {
                boost::asio::post(IoContext, [=]{callback(EError::FuritaResponseParseError, TListResult{});});
            }));
    }

    TestMetaSaveOp(std::move(request), [&](auto errorCode){CheckMetaSaveOpSuccess(errorCode);});
}

TEST_F(TTestMetaSaveOp, for_enabled_rule_with_verified_action_unavailable_must_succeed) {
    const InSequence sequence;
    ExpectFirstlineSucceeded();
    auto request{MakeTestRequest()};
    request.recipients = MakeTestRecipientsWithFiltersInUse();
    auto uid{Uid0};
    for (auto index{0u}; index < request.recipients.size(); ++index, ++uid) {
        const auto ruleDisabled{false};
        ExpectFuritaSucceeded(uid, ruleDisabled);
    }

    ExpectMthrSucceeded();
    ExpectMdbsaveSucceeded();
    TestMetaSaveOp(std::move(request), [&](auto errorCode){CheckMetaSaveOpSuccess(errorCode);});
}

TEST_F(TTestMetaSaveOp, for_all_tupita_errors_must_succeed) {
    const InSequence sequence;
    ExpectFirstlineSucceeded();
    auto request{MakeTestRequest()};
    request.recipients = MakeTestRecipientsWithFiltersInUse();
    auto uid{Uid0};
    for (auto index{0u}; index < request.recipients.size(); ++index, ++uid) {
        ExpectFuritaSucceeded(uid);
        EXPECT_CALL(*TupitaClientMock, Check(_, uid, Context->GetFullSessionId(), _, _)).WillOnce(WithArg<4>(
            [&](const auto& callback) {
                boost::asio::post(IoContext, [=]{callback(EError::TupitaResponseParseError,
                    TCheckResult{});});
            }));
    }

    TestMetaSaveOp(std::move(request), [&](auto errorCode){CheckMetaSaveOpSuccess(errorCode);});
}

TEST_F(TTestMetaSaveOp, for_recipient_to_save_for_unavailable_must_succeed) {
    ExpectFirstlineSucceeded();
    auto request{MakeTestRequest()};
    TestMetaSaveOp(std::move(request), [&](auto errorCode){CheckMetaSaveOpSuccess(errorCode);});
}

TEST_F(TTestMetaSaveOp, for_no_errors_present_must_succeed) {
    const InSequence sequence;
    ExpectFirstlineSucceeded();
    ExpectMthrSucceeded();
    ExpectMdbsaveSucceeded();
    auto request{MakeTestRequest()};
    request.recipients = MakeTestRecipientsWithoutFiltersInUse();
    TestMetaSaveOp(std::move(request), [&](auto errorCode){CheckMetaSaveOpSuccess(errorCode);});
}

}
