#include <internal/envelope/store_message_transaction.h>
#include <internal/envelope/convert.h>

#include "mocks/transactional_mock.h"
#include "envelope-row.h"
#include "path_to_query_conf.h"

namespace macs {

bool operator ==(const Envelope& lhs, const Envelope& rhs) {
    return  lhs.mid() == rhs.mid() &&
            lhs.fid() == rhs.fid() &&
            lhs.threadId() == rhs.threadId();
}

} // namespace macs

namespace {

using namespace testing;
using namespace macs;
using namespace macs::pg;
using namespace macs::pg::query;
using namespace pgg::query;
using namespace tests;

inline auto defaultEnvelopeRowData() {
    reflection::Envelope envelope;
    fillDefaultEnvelopeRowData(envelope);
    return envelope;
}

inline auto defaultThreadMeta() {
    ThreadMeta tm;
    tm.referenceHashes.push_back("ref_hash");
    return tm;
}

struct OnUpdateEnvelopeMock {
    struct Impl {
        MOCK_METHOD(void, call, (error_code, UpdateEnvelopeResult), ());
    };

    std::shared_ptr<Impl> impl = std::make_shared<Impl>();

    void operator ()(error_code ec, UpdateEnvelopeResult res) {
        impl->call(std::move(ec), std::move(res));
    }
};

using StoreMessageTransactionMock = StoreMessageTransaction<ConnProviderFake, TransactionMock*>;

struct StoreMessageTest : public Test {
    using Options = macs::EnvelopesRepository::SaveOptions;
    using Notify = macs::EnvelopesRepository::NotificationMode;
    using StoreType = macs::EnvelopesRepository::StoreType;

    const RepositoryPtr queryRepository = readQueryConfFile(pathToQueryConf());
    OnUpdateEnvelopeMock hook;

    const std::string uid {"42"};
    const MimeParts mime;
    ThreadMeta tm = defaultThreadMeta();
    const LabelSet labels {};
    const FolderSet folders {};
    const reflection::Envelope envelopeRef = defaultEnvelopeRowData();
    const macs::Envelope envelope = makeEnvelope(labels, envelopeRef);
    const macs::EnvelopeDeleted envelopeDeleted = makeDeletedEnvelope(labels, envelopeRef);

    const pgg::RequestInfo requestInfo {{}, {}, {}, {}};
    const pgg::Milliseconds timeout {13};

    TransactionMock transaction;

    const LockUserDelivery lockUser = queryRepository->query<LockUserDelivery>();

    const FindDuplicates findDuplicates = queryRepository->query<FindDuplicates>(
            Subject( envelope.subject() ),
            MessageId( envelope.rfcId() ),
            HdrDate( envelope.date() ) );

    const FindThreadsByHash findThreadsByHash = queryRepository->query<FindThreadsByHash>(
            tm.limits, tm.hash );

    const FindThreadsByReferences findThreadsByReferences = queryRepository->query<FindThreadsByReferences>(
            MailIdVec( macs::MidVec({ envelope.rfcId() }) ),
            query::MailRefVec( macs::MailRefVec(tm.referenceHashes.begin(),
                                                tm.referenceHashes.end()) ) );

    JoinThreads joinThreads(std::vector<Tid> tids) const {
        auto res = tids.front();
        auto b = std::next(tids.begin());
        auto e = tids.end();
        return queryRepository->query<JoinThreads>( ThreadIdVector({b, e}), query::ThreadId( res ) );
    }

    StoreMessage storeMessage(Notify mode) {
        return queryRepository->query<StoreMessage>(requestInfo,
            PGEnvelope( envelope ), mime, tm,
            QuietFlag(mode == Notify::off));
    }

    const MailboxEntriesByIds getStoredMessage = queryRepository->query<MailboxEntriesByIds>(
            MailIdList( { envelope.mid() } ) );

    const MailboxEraseMessages deleteMessage = queryRepository->query<MailboxEraseMessages>(
            requestInfo, MailIdList( {envelope.mid()} ) );

    const StoreDeletedMessage storeDeletedMessage =
        queryRepository->query<StoreDeletedMessage>(requestInfo,
            PGEnvelopeDeleted( envelopeDeleted ), mime);

    const DeletedMessagesByIds getDeletedMessage = queryRepository->query<DeletedMessagesByIds>(
            MailIdList( { envelope.mid() } ) );

    auto operation(Options options,
                ThreadsMergeRules mergeRule = ThreadsMergeRules::forceNewThread) {
        tm.mergeRule = mergeRule;
        return boost::make_shared<StoreMessageTransactionMock>(
            ConnProviderFake(), &transaction, queryRepository, uid, requestInfo, envelope,
            mime, tm, labels, folders, TabSet{}, options, hook, timeout
        );
    }

    std::vector<tests::Row> duplicates {{
        tests::Row {{
            {"mid", std::int64_t(envelopeRef.mid)},
            {"fid", std::int64_t(envelopeRef.fid)},
        }},
    }};

    std::vector<tests::Row> stored {{
        tests::Row {{
            {"revision", std::int64_t(envelopeRef.revision)},
            {"mid", std::int64_t(envelopeRef.mid)},
        }},
    }};

    std::vector<tests::Row> result {{
        tests::Row {{
            {"mid", std::int64_t(envelopeRef.mid)},
            {"fid", std::int32_t(envelopeRef.fid)},
            {"tid", boost::optional<std::int64_t>(envelopeRef.tid)},
        }},
    }};

    static auto defResult() {
        return std::make_tuple(macs::Envelope(), EnvelopeKind::original);
    }

    void expectLockUser() {
        const InSequence s;

        EXPECT_CALL(transaction, beginImpl(_, _, timeout)).WillOnce(InvokeArgument<1>(pgg_error_code{}));

        EXPECT_CALL(transaction, executeImpl(Eq(ByRef(lockUser)), _))
                .WillOnce(InvokeArgument<1>(pgg_error_code{}));
    }
};

TEST_F(StoreMessageTest, when_error_on_begin_should_call_hook_with_same_error) {
    const InSequence s;
    auto oper = operation({false, Notify::on, StoreType::box});

    EXPECT_CALL(transaction, beginImpl(_, _, timeout)).WillOnce(InvokeArgument<1>(operationAborted));
    EXPECT_CALL(*hook.impl, call(error_code(boost::asio::error::operation_aborted), defResult()));

    (*oper)();
}

TEST_F(StoreMessageTest, when_error_on_lock_user_delivery_should_call_hook_with_same_error) {
    const InSequence s;
    auto oper = operation({false, Notify::on, StoreType::box});

    EXPECT_CALL(transaction, beginImpl(_, _, timeout)).WillOnce(InvokeArgument<1>(pgg_error_code{}));
    EXPECT_CALL(transaction, executeImpl(Eq(ByRef(lockUser)), _))
            .WillOnce(InvokeArgument<1>(operationAborted));
    EXPECT_CALL(*hook.impl, call(error_code(boost::asio::error::operation_aborted), defResult()));
    (*oper)();
}

TEST_F(StoreMessageTest, when_try_to_store_deleted_message_when_error_call_hook_with_same_error) {
    const InSequence s;
    auto oper = operation({true, Notify::on, StoreType::deleted});
    expectLockUser();

    EXPECT_CALL(transaction, fetchImpl(Eq(ByRef(storeDeletedMessage)), _))
            .WillOnce(InvokeArgument<1>(operationAborted, FakeDataRange()));
    EXPECT_CALL(*hook.impl, call(error_code(boost::asio::error::operation_aborted), defResult()));
    (*oper)();
}

TEST_F(StoreMessageTest, when_error_on_get_deleted_message_call_hook_with_same_error) {
    const InSequence s;
    auto oper = operation({true, Notify::on, StoreType::deleted});
    expectLockUser();

    EXPECT_CALL(transaction, fetchImpl(Eq(ByRef(storeDeletedMessage)), _))
            .WillOnce(InvokeArgument<1>(pgg_error_code{}, stored));
    EXPECT_CALL(transaction, fetchImpl(Eq(ByRef(getDeletedMessage)), _))
            .WillOnce(InvokeArgument<1>(operationAborted, FakeDataRange()));
    EXPECT_CALL(*hook.impl, call(error_code(boost::asio::error::operation_aborted), defResult()));
    (*oper)();
}

TEST_F(StoreMessageTest, when_store_deleted_call_commit_call_hook_with_original_envelope) {
    const InSequence s;
    auto oper = operation({true, Notify::on, StoreType::deleted});
    expectLockUser();

    EXPECT_CALL(transaction, fetchImpl(Eq(ByRef(storeDeletedMessage)), _))
            .WillOnce(InvokeArgument<1>(pgg_error_code{}, stored));
    EXPECT_CALL(transaction, fetchImpl(Eq(ByRef(getDeletedMessage)), _))
            .WillOnce(InvokeArgument<1>(pgg_error_code{}, result));
    EXPECT_CALL(transaction, commitImpl(_)).WillOnce(InvokeArgument<0>(pgg_error_code{}));
    EXPECT_CALL(*hook.impl, call(error_code(), std::make_tuple(envelope, EnvelopeKind::original)));
    (*oper)();
}

TEST_F(StoreMessageTest, when_error_on_check_duplicates_should_call_hook_with_same_error) {
    const InSequence s;
    auto oper = operation({false, Notify::on, StoreType::box});
    expectLockUser();

    EXPECT_CALL(transaction, fetchImpl(Eq(ByRef(findDuplicates)), _))
            .WillOnce(InvokeArgument<1>(operationAborted, FakeDataRange()));
    EXPECT_CALL(*hook.impl, call(error_code(boost::asio::error::operation_aborted), defResult()));
    (*oper)();
}

TEST_F(StoreMessageTest, when_found_duplicates_try_to_get_stored_message_when_error_call_hook_with_same_error) {
    const InSequence s;
    auto oper = operation({false, Notify::on, StoreType::box});
    expectLockUser();

    EXPECT_CALL(transaction, fetchImpl(Eq(ByRef(findDuplicates)), _))
            .WillOnce(InvokeArgument<1>(pgg_error_code{}, duplicates));
    EXPECT_CALL(transaction, fetchImpl(Eq(ByRef(getStoredMessage)), _))
            .WillOnce(InvokeArgument<1>(operationAborted, FakeDataRange()));
    EXPECT_CALL(*hook.impl, call(error_code(boost::asio::error::operation_aborted), defResult()));
    (*oper)();
}

TEST_F(StoreMessageTest, when_ignore_duplicates_try_to_store_message_when_error_call_hook_with_same_error) {
    const InSequence s;
    auto oper = operation({true, Notify::on, StoreType::box});
    expectLockUser();

    auto storeMessageQuery = storeMessage(Notify::on);
    EXPECT_CALL(transaction, fetchImpl(Eq(ByRef(storeMessageQuery)), _))
            .WillOnce(InvokeArgument<1>(operationAborted, FakeDataRange()));
    EXPECT_CALL(*hook.impl, call(error_code(boost::asio::error::operation_aborted), defResult()));
    (*oper)();
}

TEST_F(StoreMessageTest, when_hash_merge_rule_try_to_find_threads_when_error_call_hook_with_same_error) {
    const InSequence s;
    auto oper = operation({true, Notify::on, StoreType::box}, ThreadsMergeRules::hash);
    expectLockUser();

    EXPECT_CALL(transaction, fetchImpl(Eq(ByRef(findThreadsByHash)), _))
            .WillOnce(InvokeArgument<1>(operationAborted, FakeDataRange()));
    EXPECT_CALL(*hook.impl, call(error_code(boost::asio::error::operation_aborted), defResult()));
    (*oper)();
}

TEST_F(StoreMessageTest, when_reference_merge_rule_try_to_find_threads_when_error_call_hook_with_same_error) {
    const InSequence s;
    auto oper = operation({true, Notify::on, StoreType::box}, ThreadsMergeRules::references);
    expectLockUser();

    EXPECT_CALL(transaction, fetchImpl(Eq(ByRef(findThreadsByReferences)), _))
            .WillOnce(InvokeArgument<1>(operationAborted, FakeDataRange()));
    EXPECT_CALL(*hook.impl, call(error_code(boost::asio::error::operation_aborted), defResult()));
    (*oper)();
}

TEST_F(StoreMessageTest, when_threads_found_try_to_join_threads_when_error_call_hook_with_same_error) {
    const InSequence s;
    auto oper = operation({true, Notify::on, StoreType::box}, ThreadsMergeRules::hash);
    expectLockUser();

    std::vector<tests::Row> threads {{
        tests::Row {{
            {"tid", std::int64_t(1)},
            {"msg_count", std::int32_t(1)},
        }},
        tests::Row {{
            {"tid", std::int64_t(2)},
            {"msg_count", std::int32_t(1)},
        }},
        tests::Row {{
            {"tid", std::int64_t(3)},
            {"msg_count", std::int32_t(1)},
        }},
    }};
    EXPECT_CALL(transaction, fetchImpl(Eq(ByRef(findThreadsByHash)), _))
            .WillOnce(InvokeArgument<1>(pgg_error_code{}, threads));
    auto joinThreadsQuery = joinThreads({"1", "2", "3"});
    EXPECT_CALL(transaction, fetchImpl(Eq(ByRef(joinThreadsQuery)), _))
            .WillOnce(InvokeArgument<1>(operationAborted, FakeDataRange()));
    EXPECT_CALL(*hook.impl, call(error_code(boost::asio::error::operation_aborted), defResult()));
    (*oper)();
}

TEST_F(StoreMessageTest, when_error_on_get_stored_message_call_hook_with_same_error) {
    const InSequence s;
    auto oper = operation({true, Notify::on, StoreType::box});
    expectLockUser();

    auto storeMessageQuery = storeMessage(Notify::on);
    EXPECT_CALL(transaction, fetchImpl(Eq(ByRef(storeMessageQuery)), _))
            .WillOnce(InvokeArgument<1>(pgg_error_code{}, stored));
    EXPECT_CALL(transaction, fetchImpl(Eq(ByRef(getStoredMessage)), _))
            .WillOnce(InvokeArgument<1>(operationAborted, FakeDataRange()));
    EXPECT_CALL(*hook.impl, call(error_code(boost::asio::error::operation_aborted), defResult()));
    (*oper)();
}

TEST_F(StoreMessageTest, when_unhooked_error_call_hook_with_same_error) {
    const InSequence s;
    auto oper = operation({true, Notify::on, StoreType::box});
    expectLockUser();

    auto storeMessageQuery = storeMessage(Notify::on);
    EXPECT_CALL(transaction, fetchImpl(Eq(ByRef(storeMessageQuery)), _))
            .WillOnce(InvokeArgument<1>(pgg_error_code{}, FakeDataRange()));
    EXPECT_CALL(*hook.impl, call(error_code(pgg::error::noDataReceived), defResult()));
    (*oper)();
}

TEST_F(StoreMessageTest, when_stored_call_commit_call_hook_with_original_envelope) {
    const InSequence s;
    auto oper = operation({true, Notify::on, StoreType::box});
    expectLockUser();

    auto storeMessageQuery = storeMessage(Notify::on);
    EXPECT_CALL(transaction, fetchImpl(Eq(ByRef(storeMessageQuery)), _))
            .WillOnce(InvokeArgument<1>(pgg_error_code{}, stored));
    EXPECT_CALL(transaction, fetchImpl(Eq(ByRef(getStoredMessage)), _))
            .WillOnce(InvokeArgument<1>(pgg_error_code{}, result));
    EXPECT_CALL(transaction, commitImpl(_)).WillOnce(InvokeArgument<0>(pgg_error_code{}));
    EXPECT_CALL(*hook.impl, call(error_code(), std::make_tuple(envelope, EnvelopeKind::original)));
    (*oper)();
}

TEST_F(StoreMessageTest, when_duplicate_call_rollback_call_hook_with_duplicated_envelope) {
    const InSequence s;
    auto oper = operation({false, Notify::on, StoreType::box});
    expectLockUser();

    EXPECT_CALL(transaction, fetchImpl(Eq(ByRef(findDuplicates)), _))
            .WillOnce(InvokeArgument<1>(pgg_error_code{}, duplicates));
    EXPECT_CALL(transaction, fetchImpl(Eq(ByRef(getStoredMessage)), _))
            .WillOnce(InvokeArgument<1>(pgg_error_code{}, result));
    EXPECT_CALL(transaction, rollbackImpl(_)).WillOnce(InvokeArgument<0>(pgg_error_code{}));
    EXPECT_CALL(*hook.impl, call(error_code(), std::make_tuple(envelope, EnvelopeKind::duplicate)));
    (*oper)();
}

TEST(TabResolverTest, should_resolve_null_to_null) {
    StoreMessageTransactionMock::TabResolver resolve(TabSet{});
    ASSERT_EQ(std::nullopt, resolve(std::nullopt));
}

TEST(TabResolverTest, should_resolve_unexisting_tab_to_null_if_no_tabs_exist) {
    StoreMessageTransactionMock::TabResolver resolve(TabSet{});
    ASSERT_EQ(std::nullopt, resolve(std::make_optional(Tab::Type::news)));
}

TEST(TabResolverTest, should_resolve_unexisting_tab_to_relevant) {
    TabsMap map;
    map[Tab::Type::relevant] = TabFactory().type(Tab::Type::relevant).release();
    StoreMessageTransactionMock::TabResolver resolve(TabSet{std::move(map)});
    ASSERT_EQ(std::make_optional(Tab::Type::relevant), resolve(std::make_optional(Tab::Type::news)));
}

TEST(TabResolverTest, should_resolve_existing_tab_to_itself) {
    TabsMap map;
    map[Tab::Type::news] = TabFactory().type(Tab::Type::news).release();
    StoreMessageTransactionMock::TabResolver resolve(TabSet{std::move(map)});
    ASSERT_EQ(std::make_optional(Tab::Type::news), resolve(std::make_optional(Tab::Type::news)));
}

} // namespace
