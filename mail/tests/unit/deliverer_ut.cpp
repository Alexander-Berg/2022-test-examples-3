#include <mail/notsolitesrv/src/deliverer.h>

#include <yamail/data/deserialization/json_reader.h>
#include <yamail/data/deserialization/yajl.h>
#include <yamail/data/serialization/json_writer.h>

#include <mail/notsolitesrv/tests/unit/nsls_test.h>
#include <mail/notsolitesrv/tests/unit/fakes/context.h>
#include <mail/notsolitesrv/tests/unit/mocks/domain_rules.h>
#include <mail/notsolitesrv/tests/unit/mocks/meta_save_op.h>
#include <mail/notsolitesrv/tests/unit/mocks/msearch_client.h>
#include <mail/notsolitesrv/tests/unit/mocks/msettings_client.h>
#include <mail/notsolitesrv/tests/unit/mocks/mulcagate_client.h>
#include <mail/notsolitesrv/tests/unit/mocks/user_processor.h>

#include <boost/fusion/adapted/struct/adapt_struct.hpp>
#include <boost/fusion/adapted/struct/define_struct.hpp>
#include <boost/range/adaptor/transformed.hpp>
#include <boost/range/iterator_range.hpp>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <algorithm>
#include <chrono>

BOOST_FUSION_ADAPT_STRUCT(NNotSoLiteSrv::NMSearch::TSubscriptionStatusRequest,
    Uids,
    OptInSubsUids,
    SubscriptionEmail
)

BOOST_FUSION_ADAPT_STRUCT(NNotSoLiteSrv::NMSettings::TParamsRequest,
    Uid,
    Params
)

using namespace testing;
using namespace NNotSoLiteSrv;

struct TSyncMessageProcessorMock {
    MOCK_METHOD(void, Do, (TMessagePtr), ());
};

struct TAsyncNewEmailsSenderMock {
    MOCK_METHOD(void, Do, (TContextPtr, const NNewEmails::TDataProvider&,
        NNewEmails::TProcessor::TSenderCallback), ());
};

struct TUJWriterMock: public user_journal::Writer {
    MOCK_METHOD(void, write, (const std::string&, const user_journal::Entry&), (const, override));
};

struct TUJMock: public NTskv::TUserJournalPtr::element_type {
public:
    TUJMock() = default;

    user_journal::WriterPtr writer() const override {
        return Writer;
    }

    std::shared_ptr<StrictMock<TUJWriterMock>> Writer = std::make_shared<StrictMock<TUJWriterMock>>();
};

namespace NNotSoLiteSrv::NMSearch {

using boost::fusion::operators::operator==;

inline std::ostream& operator<<(std::ostream& os, const TSubscriptionStatusRequest& request) {
    return os << yamail::data::serialization::JsonWriter<TSubscriptionStatusRequest>(request).result();
}

} // namespace NNotSoLiteSrv::NMSearch

namespace NNotSoLiteSrv::NMSettings {

using boost::fusion::operators::operator==;

inline std::ostream& operator<<(std::ostream& os, const TParamsRequest& request) {
    return os << yamail::data::serialization::JsonWriter<TParamsRequest>(request).result();
}

} // namespace NNotSoLiteSrv::NMSettings

using ESubscriptionStatus = NNotSoLiteSrv::NMSearch::ESubscriptionStatus;
using TMSearchClientMock = NNotSoLiteSrv::NMSearch::TMSearchClientMock;
using TMSettingsClientMock = NNotSoLiteSrv::NMSettings::TMSettingsClientMock;
using TSubscriptionStatusRequest = NNotSoLiteSrv::NMSearch::TSubscriptionStatusRequest;
using TSubscriptionStatusResponse = NNotSoLiteSrv::NMSearch::TSubscriptionStatusResponse;
using TParamsRequest = NNotSoLiteSrv::NMSettings::TParamsRequest;
using TParamsResponse = NNotSoLiteSrv::NMSettings::TParamsResponse;

struct TDelivererTest: public TNslsTest {

    void SetUp() override {
        Ctx = GetContext();
        Envelope.Lhlo = "localhost";
        Envelope.Hostname = "localhost";
        Envelope.RemoteIp = "127.0.0.1";
        Envelope.RemoteHost = "localhost";
        Envelope.MailFrom = "devnull@ya.ru";

        MgPtr = std::make_shared<StrictMock<TMulcagateClientMock>>();
        MSearchPtr = std::make_shared<StrictMock<TMSearchClientMock>>();
        MSettingsPtr = std::make_shared<StrictMock<TMSettingsClientMock>>();

        EXPECT_CALL(*MgPtr, Del(_, _, _)).Times(0);
    }

    void TearDown() override {
        IoContext.stop();
    }

    auto GetUserProcessor() const {
        return
            [this](TContextPtr, NBlackbox::TUserGetter, NBlackWhiteList::TListsLoader,
                const std::string& e, bool m, bool s, NUser::TUser& u, NUser::TCallback cb)
            {
                return Up.ProcessUser(e, m, s, u, cb);
            };
    }

    void SetMocks(TDelivererPtr dlv) {
        dlv->SetUserProcessor(GetUserProcessor());
        dlv->SetMulcagateClient(MgPtr);
        dlv->SetMSearchClient(MSearchPtr);
        dlv->SetMSettingsClient(MSettingsPtr);
        dlv->SetUserJournalWriter(UJMock);
    }

    auto GetDeliverer(std::shared_ptr<std::string> message) {
        return [this, message](auto cb) {
            auto dlv = std::make_shared<TDeliverer>(
                &IoContext,
                Ctx,
                DomainRulesMock,
                MetaSaveOpMock,
                message,
                Envelope,
                UserStorage,
                NTimeTraits::Now(),
                [this](TContextPtr ctx, const NNewEmails::TDataProvider& provider,
                    NNewEmails::TProcessor::TSenderCallback cb)
                {
                    return AsyncSendNewEmails.Do(ctx, provider, cb);
                },
                cb);
            SetMocks(dlv);
            yplatform::spawn(dlv);
            IoContext.run();
        };
    }

    auto GetDeliverer(const std::string& stid) {
        return [this, stid](auto cb) {
            auto dlv = std::make_shared<TDeliverer>(
                &IoContext,
                Ctx,
                DomainRulesMock,
                MetaSaveOpMock,
                stid,
                Envelope,
                UserStorage,
                NTimeTraits::Now(),
                [this](auto msg) { return SyncMsgProcessor.Do(msg); },
                [this](TContextPtr ctx, const NNewEmails::TDataProvider& provider,
                    NNewEmails::TProcessor::TSenderCallback cb)
                {
                    return AsyncSendNewEmails.Do(ctx, provider, cb);
                },
                cb);
            SetMocks(dlv);
            yplatform::spawn(dlv);
            IoContext.run();
        };
    }

    TContextPtr Ctx;
    StrictMock<TUserProcessorMock> Up;
    const std::shared_ptr<StrictMock<TDomainRulesMock>> DomainRulesMock{
        std::make_shared<StrictMock<TDomainRulesMock>>()};
    const std::shared_ptr<StrictMock<TMetaSaveOpMock>> MetaSaveOpMock{
        std::make_shared<StrictMock<TMetaSaveOpMock>>()};
    StrictMock<TSyncMessageProcessorMock> SyncMsgProcessor;
    StrictMock<TAsyncNewEmailsSenderMock> AsyncSendNewEmails;
    std::shared_ptr<StrictMock<TMulcagateClientMock>> MgPtr;
    std::shared_ptr<StrictMock<TMSearchClientMock>> MSearchPtr;
    std::shared_ptr<StrictMock<TMSettingsClientMock>> MSettingsPtr;
    std::shared_ptr<TUJMock> UJMock = std::make_shared<TUJMock>();
    NUser::TStoragePtr UserStorage = std::make_shared<NUser::TStorage>();
    NNotSoLiteSrv::TEnvelope Envelope;
    boost::asio::io_context IoContext;
    const std::vector<std::string> Emails{"a@a.ru", "b@b.ru"};
    const std::vector<std::string> Uids{"17", "19"};
    const std::string OrgId{"0"};
    const std::vector<std::string> DomainRuleForwards{"forward0@domain0.ru", "forward1@domai01.ru"};
};

TEST_F(TDelivererTest, EmptyMessage) {
    auto msg = std::make_shared<std::string>();
    EXPECT_CALL(Up, ProcessUser(_, _, _, _, _)).Times(0);

    ExpectCallbackCalled(
        [](auto ec, auto) {
            EXPECT_EQ(ec, EError::MessageParse);
        },
        1,
        GetDeliverer(msg)
    );
}

TEST_F(TDelivererTest, NoRecipients) {
    auto msg = std::make_shared<std::string>("Hdr: 1\r\n\r\nbody");
    EXPECT_CALL(Up, ProcessUser(_, _, _, _, _)).Times(0);

    ExpectCallbackCalled(
        [](auto ec, auto) {
            EXPECT_EQ(ec, EError::DeliveryNoRecipients);
        },
        1,
        GetDeliverer(msg)
    );
}

TEST_F(TDelivererTest, TwoRecipientOneNotFound) {
    UserStorage->AddUser("a@a.ru", true, true);
    UserStorage->AddUser("b@b.ru", true, true);
    auto msg = std::make_shared<std::string>("Hdr: 1\r\n\r\nbody");

    EXPECT_CALL(Up, ProcessUser("a@a.ru", _, _, _, _))
        .WillOnce(DoAll(
            WithArg<3>(
                Invoke([](auto& u) {
                    u.Status = NUser::ELoadStatus::Loaded;
                    u.DeliveryResult.ErrorCode = EError::UserNotFound;
                })
            ),
            InvokeArgument<4>(EError::UserNotFound)
        ));
    EXPECT_CALL(Up, ProcessUser("b@b.ru", _, _, _, _))
        .WillOnce(DoAll(
            WithArg<3>(
                Invoke([](auto& u) {
                    u.Status = NUser::ELoadStatus::Found;
                    u.DeliveryResult.ErrorCode = EError::Ok;
                    u.Uid = "1";
                })
            ),
            InvokeArgument<4>(EError::Ok)
        ));
    EXPECT_CALL(*MgPtr, Put(_, "mail:1", _, _, _))
        .WillOnce(InvokeArgument<4>(EError::Ok, "1.mail:1.2"));
    EXPECT_CALL(*DomainRulesMock, SetParams(_, _, _)).WillOnce(WithArg<2>(
        [&](const auto& callback){DomainRulesMock->SetCallback(callback);}));
    EXPECT_CALL(*DomainRulesMock, Call(_, _, _)).WillOnce([&]{DomainRulesMock->GetCallback()(EError::Ok);});
    EXPECT_CALL(*MetaSaveOpMock, SetOpParams(_, _)).WillOnce(WithArg<1>(
        [&](const auto& callback){MetaSaveOpMock->SetCallback(callback);}));
    EXPECT_CALL(*MetaSaveOpMock, Call(_, _, _)).WillOnce([&]{MetaSaveOpMock->GetCallback()(EError::Ok);});
    EXPECT_CALL(*UJMock->Writer, write("1", _)).Times(1);

    ExpectCallbackCalled(
        [this](auto ec, auto) {
            EXPECT_FALSE(ec);
            EXPECT_EQ(ec, EError::Ok);
            auto userA = UserStorage->GetUserByEmail("a@a.ru");
            ASSERT_TRUE(userA);
            EXPECT_EQ(userA->Status, NUser::ELoadStatus::Loaded);
            EXPECT_EQ(userA->DeliveryResult.ErrorCode, EError::UserNotFound);
            EXPECT_TRUE(userA->Uid.empty());

            auto userB = UserStorage->GetUserByEmail("b@b.ru");
            ASSERT_TRUE(userB);
            EXPECT_EQ(userB->Status, NUser::ELoadStatus::Found);
            EXPECT_FALSE(userB->DeliveryResult.ErrorCode);
            EXPECT_EQ(userB->Uid, "1");
        },
        1,
        GetDeliverer(msg)
    );
}

TEST_F(TDelivererTest, HttpNoSuchStid) {
    EXPECT_CALL(*MgPtr, Get(_, "nonexistent_stid", _))
        .WillOnce(InvokeArgument<2>(NMds::EError::StorageMailNotFound, ""));
    EXPECT_CALL(SyncMsgProcessor, Do(_)).Times(0);
    EXPECT_CALL(Up, ProcessUser(_, _, _, _, _)).Times(0);

    ExpectCallbackCalled(
        [](auto ec, auto) {
            EXPECT_EQ(ec, NMds::EError::StorageMailNotFound);
        },
        1,
        GetDeliverer("nonexistent_stid")
    );
}

TEST_F(TDelivererTest, HttpEmptyStid) {
    EXPECT_CALL(*MgPtr, Get(_, _, _)).Times(0);
    EXPECT_CALL(SyncMsgProcessor, Do(_)).Times(0);
    EXPECT_CALL(Up, ProcessUser(_, _, _, _, _)).Times(0);

    ExpectCallbackCalled(
        [](auto ec, auto) {
            EXPECT_EQ(ec, EError::MessageParse);
        },
        1,
        GetDeliverer("")
    );
}

TEST_F(TDelivererTest, HttpEmptyMessage) {
    EXPECT_CALL(*MgPtr, Get(_, "empty", _))
        .WillOnce(InvokeArgument<2>(NMds::EError::Ok, ""));
    EXPECT_CALL(SyncMsgProcessor, Do(_)).Times(0);
    EXPECT_CALL(Up, ProcessUser(_, _, _, _, _)).Times(0);

    ExpectCallbackCalled(
        [](auto ec, auto) {
            EXPECT_EQ(ec, EError::MessageParse);
        },
        1,
        GetDeliverer("empty")
    );
}

TEST_F(TDelivererTest, HttpNoRecipients) {
    EXPECT_CALL(*MgPtr, Get(_, "stid", _))
        .WillOnce(InvokeArgument<2>(NMds::EError::Ok, "Hdr: 1\r\n\r\nbody"));
    EXPECT_CALL(SyncMsgProcessor, Do(_)).Times(1);
    EXPECT_CALL(Up, ProcessUser(_, _, _, _, _)).Times(0);

    ExpectCallbackCalled(
        [](auto ec, auto) {
            EXPECT_EQ(ec, EError::DeliveryNoRecipients);
        },
        1,
        GetDeliverer("stid")
    );
}

TEST_F(TDelivererTest, StoreWithoutErrors) {
    UserStorage->AddUser("a@a.ru", true, true);

    EXPECT_CALL(*MgPtr, Get(_, "stid", _))
        .WillOnce(InvokeArgument<2>(NMds::EError::Ok, "Hdr: 1\r\n\r\nbody"));
    EXPECT_CALL(SyncMsgProcessor, Do(_))
        .WillOnce(
            WithArg<0>(
                Invoke([](auto msg) {
                    TXYandexHint hint;
                    hint.filters = false;
                    msg->AddParsedXYHint(std::move(hint));
                })
            )
        );
    EXPECT_CALL(Up, ProcessUser("a@a.ru", _, _, _, _))
        .WillOnce(DoAll(
            WithArg<3>(
                Invoke([](auto& u) {
                    u.Status = NUser::ELoadStatus::Found;
                    u.DeliveryResult.ErrorCode = EError::Ok;
                    u.Uid = "17";
                })
            ),
            InvokeArgument<4>(EError::Ok)
        ));
    EXPECT_CALL(*MgPtr, Put(_, "mail:1", _, _, _)).Times(0);
    EXPECT_CALL(*DomainRulesMock, SetParams(_, _, _)).WillOnce(WithArg<2>(
        [&](const auto& callback){DomainRulesMock->SetCallback(callback);}));
    EXPECT_CALL(*DomainRulesMock, Call(_, _, _)).WillOnce([&]{DomainRulesMock->GetCallback()(EError::Ok);});
    EXPECT_CALL(*MetaSaveOpMock, SetOpParams(_, _)).WillOnce(WithArg<1>(
        [&](const auto& callback){MetaSaveOpMock->SetCallback(callback);}));
    EXPECT_CALL(*MetaSaveOpMock, Call(_, _, _)).WillOnce([&]{MetaSaveOpMock->GetCallback()(EError::Ok);});
    EXPECT_CALL(*UJMock->Writer, write("17", _)).Times(1);

    ExpectCallbackCalled(
        [this](auto ec, auto) {
            EXPECT_FALSE(ec);
            auto userA = UserStorage->GetUserByEmail("a@a.ru");
            ASSERT_TRUE(userA);
            EXPECT_EQ(userA->Status, NUser::ELoadStatus::Found);
            EXPECT_EQ(userA->DeliveryResult.ErrorCode, EError::Ok);
            EXPECT_EQ(userA->Uid, "17");
        },
        1,
        GetDeliverer("stid")
    );
}

TEST_F(TDelivererTest, StopProcessingForDomainRuleErrorsForAllUsers) {
    Ctx->GetConfig()->MSearch->MessageTypes = {NMail::MT_NEWS};
    std::for_each(Emails.cbegin(), Emails.cend(), [&](const auto& email){UserStorage->AddUser(email);});

    InSequence sequence;
    EXPECT_CALL(*MgPtr, Get(_, "stid", _)).WillOnce(WithArg<2>([](const auto& callback){
        callback(NMds::EError::Ok, "From: subscription@a.ru\r\n\r\nbody");}));
    EXPECT_CALL(SyncMsgProcessor, Do(_)).WillOnce(WithArg<0>([&](const auto& msg) {
        const auto updateMessage{[](const auto& email, const auto& msg) {
            TXYandexHint hint;
            hint.label = {"SystMetkaSO:news"};
            hint.email = email;
            hint.replace_so_labels = true;
            msg->AddParsedXYHint(std::move(hint));
            msg->SetSpamType(ESpamType::Ham);
        }};

        std::for_each(Emails.cbegin(), Emails.cend(), [&](const auto& email){updateMessage(email, msg);});
    }));

    const auto processUser{[&](auto& user, const auto& uid, const auto& callback) {
        user.Status = NUser::ELoadStatus::Found;
        user.Uid = uid;
        user.OrgId = OrgId;
        user.DeliveryResult.ErrorCode = EError::Ok;
        callback({});
    }};

    EXPECT_CALL(Up, ProcessUser(Emails[0], _, _, _, _)).WillOnce(WithArgs<3, 4>([&](auto& user,
        const auto& callback){processUser(user, Uids[0], callback);}));
    EXPECT_CALL(Up, ProcessUser(Emails[1], _, _, _, _)).WillOnce(WithArgs<3, 4>([&](auto& user,
        const auto& callback){processUser(user, Uids[1], callback);}));
    EXPECT_CALL(*DomainRulesMock, SetParams(_, _, _)).WillOnce(WithArg<2>(
        [&](const auto& callback){DomainRulesMock->SetCallback(callback);}));
    EXPECT_CALL(*DomainRulesMock, Call(_, _, _)).WillOnce([&] {
        std::for_each(Emails.cbegin(), Emails.cend(), [&](const auto& email){UserStorage->GetUserByEmail(
            email)->DeliveryResult.ErrorCode = EError::DomainRulesIncorrectResult;});
        DomainRulesMock->GetCallback()({});
    });

    EXPECT_CALL(*MetaSaveOpMock, SetOpParams(_, _)).WillOnce(WithArgs<0, 1>(
        [&](const auto& request, const auto& callback)
    {
        ASSERT_EQ(0u, request.recipients.size());
        MetaSaveOpMock->SetCallback(callback);
    }));

    EXPECT_CALL(*MetaSaveOpMock, Call(_, _, _)).WillOnce([&]{MetaSaveOpMock->GetCallback()({});});
    const auto callCount{1};
    ExpectCallbackCalled([](auto ec, auto){EXPECT_EQ(EError::Ok, ec);}, callCount, GetDeliverer("stid"));
}

TEST_F(TDelivererTest, SendForDomainRuleForwardsAvailable) {
    std::for_each(Emails.cbegin(), Emails.cend(), [&](const auto& email){UserStorage->AddUser(email);});

    InSequence sequence;
    EXPECT_CALL(*MgPtr, Get(_, "stid", _)).WillOnce(WithArg<2>([](const auto& callback){
        callback(NMds::EError::Ok, "From: subscription@a.ru\r\n\r\nbody");}));
    EXPECT_CALL(SyncMsgProcessor, Do(_));

    const auto processUser{[&](auto& user, const auto& uid, const auto& callback) {
        user.Status = NUser::ELoadStatus::Found;
        user.Uid = uid;
        user.OrgId = OrgId;
        user.DeliveryResult.ErrorCode = EError::Ok;
        callback({});
    }};

    EXPECT_CALL(Up, ProcessUser(Emails[0], _, _, _, _)).WillOnce(WithArgs<3, 4>([&](auto& user,
        const auto& callback){processUser(user, Uids[0], callback);}));
    EXPECT_CALL(Up, ProcessUser(Emails[1], _, _, _, _)).WillOnce(WithArgs<3, 4>([&](auto& user,
        const auto& callback){processUser(user, Uids[1], callback);}));
    EXPECT_CALL(*DomainRulesMock, SetParams(_, _, _)).WillOnce(WithArg<2>(
        [&](const auto& callback){DomainRulesMock->SetCallback(callback);}));
    EXPECT_CALL(*DomainRulesMock, Call(_, _, _)).WillOnce([&] {
        for (auto index{0u}; index < Emails.size(); ++index) {
            UserStorage->GetUserByEmail(Emails[index])->DeliveryResult.DomainRuleForwards =
                DomainRuleForwards;
        }

        DomainRulesMock->GetCallback()({});
    });

    EXPECT_CALL(*MetaSaveOpMock, SetOpParams(_, _)).WillOnce(WithArgs<0, 1>(
        [&](const auto& request, const auto& callback)
    {
        ASSERT_EQ(2u, request.recipients.size());
        MetaSaveOpMock->SetCallback(callback);
    }));

    EXPECT_CALL(*MetaSaveOpMock, Call(_, _, _)).WillOnce([&]{MetaSaveOpMock->GetCallback()({});});
    const auto checkForwards{[&](const auto& forwards, const auto& recipients, const auto& callback) {
        EXPECT_TRUE(std::equal(forwards.cbegin(), forwards.cend(), recipients.cbegin(), recipients.cend(),
            [](const auto& forward, const auto& recipient){return (forward == recipient.Email);}));
        callback({});
    }};

    EXPECT_CALL(AsyncSendNewEmails, Do(_, _, _)).WillOnce(WithArgs<1, 2>([&](const auto& dataProvider,
        auto callback){checkForwards(DomainRuleForwards, dataProvider.GetRecipients(), callback);}));
    EXPECT_CALL(AsyncSendNewEmails, Do(_, _, _)).WillOnce(WithArgs<1, 2>([&](const auto& dataProvider,
        auto callback){checkForwards(DomainRuleForwards, dataProvider.GetRecipients(), callback);}));
    EXPECT_CALL(*UJMock->Writer, write(Uids[0], _)).Times(1);
    EXPECT_CALL(*UJMock->Writer, write(Uids[1], _)).Times(1);
    const auto callCount{1};
    ExpectCallbackCalled([](auto ec, auto){EXPECT_EQ(EError::Ok, ec);}, callCount, GetDeliverer("stid"));
}

TEST_F(TDelivererTest, DoNotApplySubscriptionForEmptyFromAddress) {
    Ctx->GetConfig()->MSearch->MessageTypes = { NMail::MT_NEWS };

    UserStorage->AddUser("a@a.ru", true, true);

    InSequence s;

    EXPECT_CALL(*MgPtr, Get(_, "stid", _))
        .WillOnce(InvokeArgument<2>(NMds::EError::Ok, "Hdr: 1\r\n\r\nbody"));
    EXPECT_CALL(SyncMsgProcessor, Do(_))
        .WillOnce(
            WithArg<0>(
                Invoke([](auto msg) {
                    TXYandexHint hint;
                    hint.label = { "SystMetkaSO:news" };
                    hint.email = "a@a.ru";
                    hint.replace_so_labels = true;
                    msg->AddParsedXYHint(std::move(hint));
                })
            )
        );
    EXPECT_CALL(Up, ProcessUser("a@a.ru", _, _, _, _))
        .WillOnce(DoAll(
            WithArg<3>(
                Invoke([](auto& u) {
                    u.Status = NUser::ELoadStatus::Found;
                    u.DeliveryResult.ErrorCode = EError::Ok;
                    u.Uid = "17";
                })
            ),
            InvokeArgument<4>(EError::Ok)
        ));

    EXPECT_CALL(*DomainRulesMock, SetParams(_, _, _)).WillOnce(WithArg<2>(
        [&](const auto& callback){DomainRulesMock->SetCallback(callback);}));
    EXPECT_CALL(*DomainRulesMock, Call(_, _, _)).WillOnce([&]{DomainRulesMock->GetCallback()(EError::Ok);});
    EXPECT_CALL(*MetaSaveOpMock, SetOpParams(_, _))
        .WillOnce(
            WithArgs<0, 1>([&](const auto& request, const auto& callback) {
                ASSERT_EQ(1u, request.recipients.size());
                const auto& recipient{request.recipients.begin()->second};

                EXPECT_EQ(17u, recipient.user.uid);
                EXPECT_TRUE(recipient.params.use_filters);
                EXPECT_EQ(recipient.params.folder->path->path, "\\Inbox");

                MetaSaveOpMock->SetCallback(callback);
            })
        );

    EXPECT_CALL(*MetaSaveOpMock, Call(_, _, _)).WillOnce([&]{MetaSaveOpMock->GetCallback()(EError::Ok);});
    EXPECT_CALL(*UJMock->Writer, write("17", _)).Times(1);

    ExpectCallbackCalled(
        [](auto ec, auto) {
            EXPECT_EQ(ec, EError::Ok);
        },
        1,
        GetDeliverer("stid")
    );
}

TEST_F(TDelivererTest, MSettingsNotAvailable) {
    Ctx->GetConfig()->MSearch->MessageTypes = { NMail::MT_NEWS };

    UserStorage->AddUser("a@a.ru", true, true);

    InSequence s;

    EXPECT_CALL(*MgPtr, Get(_, "stid", _))
        .WillOnce(InvokeArgument<2>(NMds::EError::Ok, "From: opt-in@a.ru\r\n\r\nbody"));
    EXPECT_CALL(SyncMsgProcessor, Do(_))
        .WillOnce(
            WithArg<0>(
                Invoke([](auto msg) {
                    TXYandexHint hint;
                    hint.label = { "SystMetkaSO:news" };
                    hint.email = "a@a.ru";
                    hint.replace_so_labels = true;
                    msg->AddParsedXYHint(std::move(hint));
                })
            )
        );
    EXPECT_CALL(Up, ProcessUser("a@a.ru", _, _, _, _))
        .WillOnce(DoAll(
            WithArg<3>(
                Invoke([](auto& user) {
                    user.Status = NUser::ELoadStatus::Found;
                    user.DeliveryResult.ErrorCode = EError::Ok;
                    user.Uid = "17";
                })
            ),
            InvokeArgument<4>(EError::Ok)
        ));
    EXPECT_CALL(*DomainRulesMock, SetParams(_, _, _)).WillOnce(WithArg<2>(
        [&](const auto& callback){DomainRulesMock->SetCallback(callback);}));
    EXPECT_CALL(*DomainRulesMock, Call(_, _, _)).WillOnce([&]{DomainRulesMock->GetCallback()(EError::Ok);});
    EXPECT_CALL(*MSettingsPtr, GetParams(_, TParamsRequest{ .Uid = 17U, .Params = { "mail_b2c_can_use_opt_in_subs", "opt_in_subs_enabled" } }, _))
        .WillOnce(InvokeArgument<2>(EError::HttpRetriesExceeded, std::nullopt));

    ExpectCallbackCalled(
        [](auto ec, auto) {
            EXPECT_EQ(ec, EError::HttpRetriesExceeded);
        },
        1,
        GetDeliverer("stid")
    );
}

TEST_F(TDelivererTest, MSeachNotAvailable) {
    Ctx->GetConfig()->MSearch->MessageTypes = { NMail::MT_NEWS };

    UserStorage->AddUser("a@a.ru", true, true);

    InSequence s;

    EXPECT_CALL(*MgPtr, Get(_, "stid", _))
        .WillOnce(InvokeArgument<2>(NMds::EError::Ok, "From: subscription@a.ru\r\n\r\nbody"));
    EXPECT_CALL(SyncMsgProcessor, Do(_))
        .WillOnce(
            WithArg<0>(
                Invoke([](auto msg) {
                    TXYandexHint hint;
                    hint.label = { "SystMetkaSO:news" };
                    hint.email = "a@a.ru";
                    hint.replace_so_labels = true;
                    msg->AddParsedXYHint(std::move(hint));
                })
            )
        );
    EXPECT_CALL(Up, ProcessUser("a@a.ru", _, _, _, _))
        .WillOnce(DoAll(
            WithArg<3>(
                Invoke([](auto& u) {
                    u.Status = NUser::ELoadStatus::Found;
                    u.DeliveryResult.ErrorCode = EError::Ok;
                    u.Uid = "17";
                })
            ),
            InvokeArgument<4>(EError::Ok)
        ));
    EXPECT_CALL(*DomainRulesMock, SetParams(_, _, _)).WillOnce(WithArg<2>(
        [&](const auto& callback){DomainRulesMock->SetCallback(callback);}));
    EXPECT_CALL(*DomainRulesMock, Call(_, _, _)).WillOnce([&]{DomainRulesMock->GetCallback()(EError::Ok);});
    EXPECT_CALL(*MSettingsPtr, GetParams(_, TParamsRequest{ .Uid = 17U, .Params = { "mail_b2c_can_use_opt_in_subs", "opt_in_subs_enabled" } }, _))
        .WillOnce(InvokeArgument<2>(EError::Ok, TParamsResponse{}));
    EXPECT_CALL(*MSearchPtr, SubscriptionStatus(_, TSubscriptionStatusRequest{ .Uids = { 17U }, .SubscriptionEmail = "subscription@a.ru" }, _))
        .WillOnce(InvokeArgument<2>(EError::MSearchError, std::nullopt));

    ExpectCallbackCalled(
        [](auto ec, auto) {
            EXPECT_EQ(ec, EError::MSearchError);
        },
        1,
        GetDeliverer("stid")
    );
}

struct TDelivererBySubscriptionStatusTest
    : TDelivererTest
    , WithParamInterface<std::tuple<ESubscriptionStatus, ESpamType, std::function<void(const NMetaSaveOp::TParams&)>>> {
};

INSTANTIATE_TEST_SUITE_P(ChangeRecipientParamsBySubscriptionStatus, TDelivererBySubscriptionStatusTest, Values(
    std::make_tuple(ESubscriptionStatus::pending, ESpamType::Ham, [](const NMetaSaveOp::TParams& params) {
        EXPECT_FALSE(params.use_filters);
        EXPECT_EQ(params.folder->path->path, "\\Pending");
        EXPECT_EQ(params.no_such_folder_action, NMetaSaveOp::ENoSuchFolderAction::Create);
    }),
    std::make_tuple(ESubscriptionStatus::pending, ESpamType::Spam, [](const NMetaSaveOp::TParams& params) {
        EXPECT_TRUE(params.use_filters);
        EXPECT_EQ(params.folder->path->path, "\\Spam");
    }),
    std::make_tuple(ESubscriptionStatus::hidden, ESpamType::Ham, [](const NMetaSaveOp::TParams& params) {
        EXPECT_FALSE(params.use_filters);
        EXPECT_EQ(params.folder->path->path, "\\Trash");
    }),
    std::make_tuple(ESubscriptionStatus::hidden, ESpamType::Spam, [](const NMetaSaveOp::TParams& params) {
        EXPECT_FALSE(params.use_filters);
        EXPECT_EQ(params.folder->path->path, "\\Trash");
    }),
    std::make_tuple(ESubscriptionStatus::active, ESpamType::Ham, [](const NMetaSaveOp::TParams& params) {
        EXPECT_TRUE(params.use_filters);
        EXPECT_EQ(params.folder->path->path, "\\Inbox");
    }),
    std::make_tuple(ESubscriptionStatus::active, ESpamType::Spam, [](const NMetaSaveOp::TParams& params) {
        EXPECT_TRUE(params.use_filters);
        EXPECT_EQ(params.folder->path->path, "\\Spam");
    })
));

TEST_P(TDelivererBySubscriptionStatusTest, SubscriptionByStatus) {
    auto [status, spamType, checkParamsFunc] = GetParam();

    Ctx->GetConfig()->MSearch->MessageTypes = { NMail::MT_NEWS };

    UserStorage->AddUser("a@a.ru", true, true);

    InSequence s;

    EXPECT_CALL(*MgPtr, Get(_, "stid", _))
        .WillOnce(InvokeArgument<2>(NMds::EError::Ok, "From: subscription@a.ru\r\n\r\nbody"));
    EXPECT_CALL(SyncMsgProcessor, Do(_))
        .WillOnce(
            WithArg<0>(
                Invoke([spamType = spamType](auto msg) {
                    TXYandexHint hint;
                    hint.label = { "SystMetkaSO:news" };
                    hint.email = "a@a.ru";
                    hint.replace_so_labels = true;
                    msg->AddParsedXYHint(std::move(hint));

                    msg->SetSpamType(spamType);
                })
            )
        );
    EXPECT_CALL(Up, ProcessUser("a@a.ru", _, _, _, _))
        .WillOnce(DoAll(
            WithArg<3>(
                Invoke([](auto& user) {
                    user.Status = NUser::ELoadStatus::Found;
                    user.DeliveryResult.ErrorCode = EError::Ok;
                    user.Uid = "17";
                })
            ),
            InvokeArgument<4>(EError::Ok)
        ));
    EXPECT_CALL(*DomainRulesMock, SetParams(_, _, _)).WillOnce(WithArg<2>(
        [&](const auto& callback){DomainRulesMock->SetCallback(callback);}));
    EXPECT_CALL(*DomainRulesMock, Call(_, _, _)).WillOnce([&]{DomainRulesMock->GetCallback()(EError::Ok);});
    EXPECT_CALL(*MSettingsPtr, GetParams(_, TParamsRequest{ .Uid = 17U, .Params = { "mail_b2c_can_use_opt_in_subs", "opt_in_subs_enabled" } }, _))
        .WillOnce(InvokeArgument<2>(EError::Ok, TParamsResponse{ .CanUseOptInSubs = true, .OptInSubsEnabled = true }));
    EXPECT_CALL(*MSearchPtr, SubscriptionStatus(_, TSubscriptionStatusRequest{ .Uids = { 17U }, .OptInSubsUids = { 17U }, .SubscriptionEmail = "subscription@a.ru" }, _))
        .WillOnce(
            InvokeArgument<2>(
                EError::Ok,
                TSubscriptionStatusResponse{ .Subscriptions = {{ 17U, "subscription@a.ru", status }} }
            )
        );

    EXPECT_CALL(*MetaSaveOpMock, SetOpParams(_, _))
        .WillOnce(
            WithArgs<0, 1>([&, checkParamsFunc = std::move(checkParamsFunc)](const auto& request, const auto& callback) {
                ASSERT_EQ(1u, request.recipients.size());
                const auto& recipient{request.recipients.begin()->second};
                EXPECT_EQ(17u, recipient.user.uid);
                checkParamsFunc(recipient.params);

                MetaSaveOpMock->SetCallback(callback);
            })
        );

    EXPECT_CALL(*MetaSaveOpMock, Call(_, _, _)).WillOnce([&]{MetaSaveOpMock->GetCallback()(EError::Ok);});
    EXPECT_CALL(*UJMock->Writer, write("17", _)).Times(1);

    ExpectCallbackCalled(
        [](auto ec, auto) {
            EXPECT_EQ(ec, EError::Ok);
        },
        1,
        GetDeliverer("stid")
    );
}

TEST_F(TDelivererTest, DoNotApplySubscriptionForSenderWithSaveToSentHint) {
    Ctx->GetConfig()->MSearch->MessageTypes = { NMail::MT_NEWS };

    UserStorage->AddUser("a@a.ru", true, true);
    UserStorage->AddUser("subscription@a.ru", true, true);

    InSequence s;

    EXPECT_CALL(*MgPtr, Get(_, "stid", _))
        .WillOnce(InvokeArgument<2>(NMds::EError::Ok, "From: subscription@a.ru\r\n\r\nbody"));
    EXPECT_CALL(SyncMsgProcessor, Do(_))
        .WillOnce(
            WithArg<0>(
                Invoke([](auto msg) {
                    TXYandexHint hint1;
                    hint1.email = "a@a.ru";
                    hint1.label = { "SystMetkaSO:news" };
                    hint1.replace_so_labels = true;

                    TXYandexHint hint2;
                    hint2.email = "subscription@a.ru";
                    hint2.label = { "SystMetkaSO:news" };
                    hint2.replace_so_labels = true;
                    hint2.save_to_sent = true;

                    msg->AddParsedXYHint(std::move(hint1));
                    msg->AddParsedXYHint(std::move(hint2));
                })
            )
        );
    EXPECT_CALL(Up, ProcessUser("a@a.ru", _, _, _, _))
        .WillOnce(DoAll(
            WithArg<3>(
                Invoke([](auto& user) {
                    user.Status = NUser::ELoadStatus::Found;
                    user.DeliveryResult.ErrorCode = EError::Ok;
                    user.Uid = "17";
                })
            ),
            InvokeArgument<4>(EError::Ok)
        ));
    EXPECT_CALL(Up, ProcessUser("subscription@a.ru", _, _, _, _))
        .WillOnce(DoAll(
            WithArg<3>(
                Invoke([](auto& user) {
                    user.Status = NUser::ELoadStatus::Found;
                    user.DeliveryResult.ErrorCode = EError::Ok;
                    user.Uid = "19";
                })
            ),
            InvokeArgument<4>(EError::Ok)
        ));
    EXPECT_CALL(*DomainRulesMock, SetParams(_, _, _)).WillOnce(WithArg<2>(
        [&](const auto& callback){DomainRulesMock->SetCallback(callback);}));
    EXPECT_CALL(*DomainRulesMock, Call(_, _, _)).WillOnce([&]{DomainRulesMock->GetCallback()(EError::Ok);});
    EXPECT_CALL(*MSettingsPtr, GetParams(_, TParamsRequest{ .Uid = 17U, .Params = { "mail_b2c_can_use_opt_in_subs", "opt_in_subs_enabled" } }, _))
        .WillOnce(InvokeArgument<2>(EError::Ok, TParamsResponse{}));
    EXPECT_CALL(*MSearchPtr, SubscriptionStatus(_, TSubscriptionStatusRequest{ .Uids = { 17U }, .SubscriptionEmail = "subscription@a.ru" }, _))
        .WillOnce(
            InvokeArgument<2>(
                EError::Ok,
                TSubscriptionStatusResponse{ .Subscriptions = {{ 17U, "subscription@a.ru", ESubscriptionStatus::hidden }} }
            )
        );
    EXPECT_CALL(*MetaSaveOpMock, SetOpParams(_, _))
        .WillOnce(
            WithArgs<0, 1>([&](const auto& request, const auto& callback) {
                ASSERT_EQ(2U, request.recipients.size());

                const auto& recipient1 = request.recipients.begin()->second;
                const auto& recipient2 = (++request.recipients.begin())->second;

                EXPECT_EQ(17U, recipient1.user.uid);
                EXPECT_FALSE(recipient1.params.use_filters);
                EXPECT_EQ(recipient1.params.folder->path->path, "\\Trash");

                EXPECT_EQ(19U, recipient2.user.uid);
                EXPECT_FALSE(recipient2.params.use_filters);
                EXPECT_EQ(recipient2.params.folder->path->path, "\\Sent");

                MetaSaveOpMock->SetCallback(callback);
            })
        );

    EXPECT_CALL(*MetaSaveOpMock, Call(_, _, _)).WillOnce([&]() { MetaSaveOpMock->GetCallback()(EError::Ok); });
    EXPECT_CALL(*UJMock->Writer, write("17", _));
    EXPECT_CALL(*UJMock->Writer, write("19", _));

    ExpectCallbackCalled(
        [](auto ec, auto) {
            EXPECT_EQ(ec, EError::Ok);
        },
        1,
        GetDeliverer("stid")
    );
}

TEST_F(TDelivererTest, DoNotApplySubscriptionForSenderWithSaveToSentAndCopyToInboxHint) {
    Ctx->GetConfig()->MSearch->MessageTypes = { NMail::MT_NEWS };

    UserStorage->AddUser("a@a.ru", true, true);
    UserStorage->AddUser("subscription@a.ru", true, true);

    InSequence s;

    EXPECT_CALL(*MgPtr, Get(_, "stid", _))
        .WillOnce(InvokeArgument<2>(NMds::EError::Ok, "From: subscription@a.ru\r\n\r\nbody"));
    EXPECT_CALL(SyncMsgProcessor, Do(_))
        .WillOnce(
            WithArg<0>(
                Invoke([](auto msg) {
                    TXYandexHint hint1;
                    hint1.email = "a@a.ru";
                    hint1.label = { "SystMetkaSO:news" };
                    hint1.replace_so_labels = true;

                    TXYandexHint hint2;
                    hint2.email = "subscription@a.ru";
                    hint2.label = { "SystMetkaSO:news" };
                    hint2.replace_so_labels = true;
                    hint2.save_to_sent = true;
                    hint2.copy_to_inbox = true;

                    msg->AddParsedXYHint(std::move(hint1));
                    msg->AddParsedXYHint(std::move(hint2));
                })
            )
        );
    EXPECT_CALL(Up, ProcessUser("a@a.ru", _, _, _, _))
        .WillOnce(DoAll(
            WithArg<3>(
                Invoke([](auto& user) {
                    user.Status = NUser::ELoadStatus::Found;
                    user.DeliveryResult.ErrorCode = EError::Ok;
                    user.Uid = "17";
                })
            ),
            InvokeArgument<4>(EError::Ok)
        ));
    EXPECT_CALL(Up, ProcessUser("subscription@a.ru", _, _, _, _))
        .WillOnce(DoAll(
            WithArg<3>(
                Invoke([](auto& user) {
                    user.Status = NUser::ELoadStatus::Found;
                    user.DeliveryResult.ErrorCode = EError::Ok;
                    user.Uid = "19";
                })
            ),
            InvokeArgument<4>(EError::Ok)
        ));
    EXPECT_CALL(*DomainRulesMock, SetParams(_, _, _)).WillOnce(WithArg<2>(
        [&](const auto& callback){DomainRulesMock->SetCallback(callback);}));
    EXPECT_CALL(*DomainRulesMock, Call(_, _, _)).WillOnce([&]{DomainRulesMock->GetCallback()(EError::Ok);});
    EXPECT_CALL(*MSettingsPtr, GetParams(_, TParamsRequest{ .Uid = 17U, .Params = { "mail_b2c_can_use_opt_in_subs", "opt_in_subs_enabled" } }, _))
        .WillOnce(InvokeArgument<2>(EError::Ok, TParamsResponse{}));
    EXPECT_CALL(*MSettingsPtr, GetParams(_, TParamsRequest{ .Uid = 19U, .Params = { "mail_b2c_can_use_opt_in_subs", "opt_in_subs_enabled" } }, _))
        .WillOnce(InvokeArgument<2>(EError::Ok, TParamsResponse{}));
    EXPECT_CALL(*MSearchPtr, SubscriptionStatus(_, TSubscriptionStatusRequest{ .Uids = { 17U, 19U }, .SubscriptionEmail = "subscription@a.ru" }, _))
        .WillOnce(
            InvokeArgument<2>(
                EError::Ok,
                TSubscriptionStatusResponse{
                    .Subscriptions = {
                        { 17U, "subscription@a.ru", ESubscriptionStatus::hidden },
                        { 19U, "subscription@a.ru", ESubscriptionStatus::hidden }
                    }
                }
            )
        );
    EXPECT_CALL(*MetaSaveOpMock, SetOpParams(_, _))
        .WillOnce(
            WithArgs<0, 1>([&](const auto& request, const auto& callback) {
                using namespace boost::adaptors;
                const auto& recipients = boost::copy_range<std::vector<const NMetaSaveOp::TRecipient*>>(
                    request.recipients | transformed([](const auto& item) { return &item.second; })
                );

                ASSERT_EQ(3U, recipients.size());

                EXPECT_EQ(17U, recipients[0]->user.uid);
                EXPECT_FALSE(recipients[0]->params.use_filters);
                EXPECT_EQ(recipients[0]->params.folder->path->path, "\\Trash");

                EXPECT_EQ(19U, recipients[1]->user.uid);
                EXPECT_FALSE(recipients[1]->params.use_filters);
                EXPECT_EQ(recipients[1]->params.folder->path->path, "\\Sent");

                EXPECT_EQ(19U, recipients[2]->user.uid);
                EXPECT_FALSE(recipients[2]->params.use_filters);
                EXPECT_EQ(recipients[2]->params.folder->path->path, "\\Trash");

                MetaSaveOpMock->SetCallback(callback);
            })
        );

    EXPECT_CALL(*MetaSaveOpMock, Call(_, _, _)).WillOnce([&]() { MetaSaveOpMock->GetCallback()(EError::Ok); });
    EXPECT_CALL(*UJMock->Writer, write("17", _));
    EXPECT_CALL(*UJMock->Writer, write("19", _)).Times(2);

    ExpectCallbackCalled(
        [](auto ec, auto) {
            EXPECT_EQ(ec, EError::Ok);
        },
        1,
        GetDeliverer("stid")
    );
}
