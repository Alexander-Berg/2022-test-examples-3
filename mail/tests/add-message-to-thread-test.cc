#include "mocks/transactional_mock.h"
#include "path_to_query_conf.h"

#include <internal/reflection/thread_label.h>
#include <internal/thread/add_message_to_thread.h>
#include <internal/thread/query.h>

#include <macs/label_factory.h>
#include <macs/folder_factory.h>

namespace {

using namespace testing;
using namespace macs;
using namespace macs::pg;
using namespace macs::pg::query;
using namespace pgg::query;
using namespace tests;

Label makeLabel(Label::Symbol symbol) {
    static int lid = 0;
    ++lid;

    LabelFactory factory;
    factory.lid(std::to_string(lid));
    factory.symbol(symbol);
    return factory.product();
}

Folder makeFolder(const Folder::Symbol& symbol) {
    static int fid = 0;
    ++fid;
    FolderFactory factory;
    factory.fid(std::to_string(fid));
    factory.symbol(symbol);
    return factory.product();
}

struct AddMessageToThreadTest : public Test {

    auto operation() {
        auto coro = boost::make_shared<AddMessageToThread<TransactionMock*>>(&transaction, queryRepository,
            uid, requestInfo, threadMeta, messageId, allLabels, allFolders, envelopeLabels, envelopeFid,
            receivedDate, [this](auto ec, auto res) { err = ec; result = res; });
        return coro;
    }

    void SetUp() override {
        threads = {{
            tests::Row {{
                {"tid", std::int64_t(std::stol(targetThreadId))},
                {"msg_count", std::int32_t(5)},
            }}
        }};

        threadMeta.mergeRule = ThreadsMergeRules::hash;

        threadMeta.referenceHashes.clear();
        threadMeta.referenceHashes.push_back("ref_hash");

        allLabels.clear();
        envelopeLabels.clear();

        auto foldersMap = std::make_shared<FoldersMap>();
        foldersMap->insert({inbox.fid(), inbox});
        foldersMap->insert({outbox.fid(), outbox});
        foldersMap->insert({drafts.fid(), drafts});
        foldersMap->insert({sent.fid(), sent});
        allFolders = FolderSet(foldersMap);

        envelopeFid = inbox.fid();
    }

    const RepositoryPtr queryRepository = readQueryConfFile(pathToQueryConf());
    const std::string uid {"42"};
    const pgg::RequestInfo requestInfo {{}, {}, {}, {}};
    const RfcMessageId messageId {"123456-789-abc"};
    const macs::ThreadId targetThreadId {"13"};
    const FolderSet folders = {};

    const Label muteLabel = makeLabel(Label::Symbol::mute_label);
    const Label seenLabel = makeLabel(Label::Symbol::seen_label);
    const Label remindNoAnswerLabel = makeLabel(Label::Symbol::remindNoAnswer_label);


    const Folder inbox = makeFolder(Folder::Symbol::inbox);
    const Folder outbox = makeFolder(Folder::Symbol::outbox);
    const Folder drafts = makeFolder(Folder::Symbol::drafts);
    const Folder sent = makeFolder(Folder::Symbol::sent);

    std::vector<tests::Row> threads;
    LabelSet allLabels;
    FolderSet allFolders;
    std::vector<Lid> envelopeLabels;
    Fid envelopeFid;
    std::time_t receivedDate = 1534860163;
    ThreadMeta threadMeta;
    TransactionMock transaction;
    error_code err;
    JoinThreadResult result;

    JoinThreads joinThreads(std::vector<Tid> tids) const {
        auto res = tids.front();
        auto b = std::next(tids.begin());
        auto e = tids.end();
        return queryRepository->query<JoinThreads>( ThreadIdVector({b, e}), query::ThreadId( res ) );
    }

    auto threadLabels(macs::ThreadId tid) {
        return queryRepository->query<macs::pg::ThreadLabels>(ThreadIdVector({ tid }));
    }

    auto getNewestMessageInThread(macs::ThreadId tid) {
        return queryRepository->query<GetNewestMessageInThread>(query::ThreadId(tid), OutgoingFolders());
    }

    auto getNotAnsweredMids(macs::ThreadId tid){
        return queryRepository->query<GetNotAnsweredMids>(query::ThreadId(tid), RemindNoAnswerLid(remindNoAnswerLabel.lid()),
            ReceivedDate(receivedDate));
    }

    auto removeLabels(const std::list<Mid>& mids, const Lid& lid) {
        return queryRepository->query<RemoveLabels>(MailIdList(mids), LabelIdList({lid}));
    }

    const FindThreadsByHash findThreadsByHash = queryRepository->query<FindThreadsByHash>(
            threadMeta.limits, threadMeta.hash );

    const FindThreadsByReferences findThreadsByReferences = queryRepository->query<FindThreadsByReferences>(
            MailIdVec( macs::MidVec({ messageId }) ),
            query::MailRefVec( macs::MailRefVec(threadMeta.referenceHashes.begin(),
                                                threadMeta.referenceHashes.end()) ) );
};


TEST_F(AddMessageToThreadTest, find_thread_by_hash) {
    auto op = operation();

    EXPECT_CALL(transaction, fetchImpl(Eq(ByRef(findThreadsByHash)), _))
            .WillOnce(InvokeArgument<1>(pgg_error_code{}, threads));

    (*op)();

    EXPECT_EQ(error_code(), err);
    EXPECT_EQ(targetThreadId, result.tid);
    EXPECT_TRUE(result.removeLids.empty());
    EXPECT_TRUE(result.addLids.empty());
}

TEST_F(AddMessageToThreadTest, find_thread_by_references) {
    threadMeta.mergeRule = ThreadsMergeRules::references;
    auto op = operation();

    EXPECT_CALL(transaction, fetchImpl(Eq(ByRef(findThreadsByReferences)), _))
            .WillOnce(InvokeArgument<1>(pgg_error_code{}, threads));

    (*op)();

    EXPECT_EQ(error_code(), err);
    EXPECT_EQ(targetThreadId, result.tid);
    EXPECT_TRUE(result.addLids.empty());
    EXPECT_TRUE(result.removeLids.empty());
}

TEST_F(AddMessageToThreadTest, join_threads) {
    threads.push_back(tests::Row {{
        {"tid", std::int64_t(14)},
        {"msg_count", std::int32_t(3)},
    }});
    auto op = operation();

    EXPECT_CALL(transaction, fetchImpl(Eq(ByRef(findThreadsByHash)), _))
            .WillOnce(InvokeArgument<1>(pgg_error_code{}, threads));

    std::vector<tests::Row> joinResult {{
        tests::Row {{
            {"revision", std::int64_t(228)},
            {"mids", std::vector<std::int32_t>()},
        }}
    }};
    auto joinThreadsQuery = joinThreads({targetThreadId, "666"});
    EXPECT_CALL(transaction, fetchImpl(Eq(ByRef(joinThreadsQuery)), _))
            .WillOnce(InvokeArgument<1>(pgg_error_code{}, joinResult));

    (*op)();

    EXPECT_EQ(error_code(), err);
    EXPECT_EQ(targetThreadId, result.tid);
    EXPECT_TRUE(result.addLids.empty());
}


TEST_F(AddMessageToThreadTest, when_add_to_muted_thread_add_mute_and_seen_labels) {
    allLabels[muteLabel.lid()] = muteLabel;
    allLabels[seenLabel.lid()] = seenLabel;

    auto op = operation();

    EXPECT_CALL(transaction, fetchImpl(Eq(ByRef(findThreadsByHash)), _))
            .WillOnce(InvokeArgument<1>(pgg_error_code{}, threads));

    std::vector<tests::Row> threadLabelsResult = {{
        tests::Row {{
            {"tid", std::int64_t(std::stol(targetThreadId))},
            {"revision", std::int64_t(228)},
            {"labels", std::vector<reflection::ThreadLabel> {
                { /*lid*/std::int32_t(std::stoi(muteLabel.lid())), /*msg_count*/std::int32_t(1) }
            }}
        }}
    }};

    auto threadLabelsQuery = threadLabels(targetThreadId);
    EXPECT_CALL(transaction, fetchImpl(Eq(ByRef(threadLabelsQuery)), _))
        .WillOnce(InvokeArgument<1>(pgg_error_code{}, threadLabelsResult));

    (*op)();

    EXPECT_EQ(error_code(), err);
    EXPECT_EQ(targetThreadId, result.tid);
    EXPECT_TRUE(boost::count(result.addLids, muteLabel.lid()));
    EXPECT_TRUE(boost::count(result.addLids, seenLabel.lid()));
    EXPECT_TRUE(result.removeLids.empty());
}

TEST_F(AddMessageToThreadTest, when_thread_have_newer_message_remove_remindNoAnswer_label) {
    allLabels[remindNoAnswerLabel.lid()] = remindNoAnswerLabel;
    envelopeLabels.push_back(remindNoAnswerLabel.lid());
    envelopeFid = sent.fid();

    auto op = operation();

    EXPECT_CALL(transaction, fetchImpl(Eq(ByRef(findThreadsByHash)), _))
            .WillOnce(InvokeArgument<1>(pgg_error_code{}, threads));

    std::vector<tests::Row> newestMessage {{
        tests::Row {{
            {"mid", std::int64_t(1)},
            {"received_date", receivedDate + 1}
        }}
    }};
    auto getNewestMessageInThreadQuery = getNewestMessageInThread("13");
    EXPECT_CALL(transaction, fetchImpl(Eq(ByRef(getNewestMessageInThreadQuery)), _))
            .WillOnce(InvokeArgument<1>(pgg_error_code{}, newestMessage));

    (*op)();

    EXPECT_EQ(error_code(), err);
    EXPECT_EQ(targetThreadId, result.tid);
    EXPECT_TRUE(result.addLids.empty());
    EXPECT_TRUE(boost::count(result.removeLids, remindNoAnswerLabel.lid()));
}

TEST_F(AddMessageToThreadTest, unmark_answered_messages_in_thread) {
    allLabels[remindNoAnswerLabel.lid()] = remindNoAnswerLabel;

    auto op = operation();

    EXPECT_CALL(transaction, fetchImpl(Eq(ByRef(findThreadsByHash)), _))
            .WillOnce(InvokeArgument<1>(pgg_error_code{}, threads));

    std::vector<tests::Row> notAnsweredMids {{
        tests::Row {{
            {"mid", std::int64_t(1)}
        }}
    }};
    auto getNotAnsweredMidsQuery = getNotAnsweredMids("13");
    EXPECT_CALL(transaction, fetchImpl(Eq(ByRef(getNotAnsweredMidsQuery)), _))
            .WillOnce(InvokeArgument<1>(pgg_error_code{}, notAnsweredMids));

    auto removeLabelsQuery = removeLabels({"1"}, remindNoAnswerLabel.lid());
    EXPECT_CALL(transaction, executeImpl(Eq(ByRef(removeLabelsQuery)), _))
            .WillOnce(InvokeArgument<1>(pgg_error_code{}));

    (*op)();

    EXPECT_EQ(error_code(), err);
    EXPECT_EQ(targetThreadId, result.tid);
    EXPECT_TRUE(result.addLids.empty());
    EXPECT_TRUE(result.removeLids.empty());
}

} // namespace
