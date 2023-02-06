#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include "wrap_yield.h"

#include <macs/envelope_factory.h>
#include <macs_pg/changelog/factory.h>

#include <src/access_impl/change_composer.h>

#include "envelope_cmp.h"
#include "label_cmp.h"
#include "labels.h"
#include "mailbox_mock.h"
#include "profiler_mock.h"

namespace {

using namespace ::testing;
using namespace ::doberman::testing;
using namespace ::doberman::testing::labels;
using ::doberman::Envelope;
using ::doberman::LabelSet;
using ::macs::pg::ChangeType;
using ::macs::MidList;
using ::macs::ChangeId;
using ::macs::Revision;
using ::macs::MimeParts;
using ::doberman::EnvelopeWithMimes;
using EnvelopeVector = std::vector<Envelope>;
using MidWithMimesVector = ::macs::MidsWithMimes;
using MidWithMimes = MidWithMimesVector::value_type;
using ::macs::ThreadId;
using doberman::logic::SharedFolderCoordinates;

const ChangeId someChangeId = 13342234232423;
const Revision someRevision = 87395033579534;
const std::string someChangedJson = R"json([
    {
        "mid": 123123123,
        "fid": 1, "tid": 1, "lids": [],
        "seen": false, "recent": true, "deleted": true,
        "hdr_message_id": "0@1"
    }])json";

const std::string someArgumentsJson = "{}";

struct SubscribedFolderMock: public ::doberman::logic::Change::LogicSubscribedFolder {
    MOCK_METHOD(const SharedFolderCoordinates&, coordinates, (), (const, override));
    MOCK_METHOD(void, put, (EnvelopeWithMimes, ::doberman::meta::labels::LabelsCache), (const, override));
    MOCK_METHOD(void, erase, (macs::MidVec, Revision), (const, override));
    MOCK_METHOD(void, mark, (Mid, Labels, Revision), (const, override));
    MOCK_METHOD(void, unmark, (Mid, Labels, Revision), (const, override));
    MOCK_METHOD(void, joinThreads, (ThreadId, std::vector<ThreadId>, Revision), (const, override));
};


struct ChangeComposerAccessImplTest : public Test {
    StrictMock<MailboxMock> mailboxMock;
    StrictMock<MailboxWithMimesMock> mailboxWithMimesMock;
    StrictMock<SubscribedFolderMock> subscribedFolderMock;
    NiceMock<ProfilerMock> profMock;
    const Fid fid = "42";

    using Profiler = ::doberman::profiling::Profiler<NiceMock<ProfilerMock>*>;
    ::doberman::access_impl::ChangeComposer<std::function<MailboxMock*(Uid)>, Profiler> composer{
        [&](auto) { return &mailboxMock;}, ::doberman::makeProfiler(&profMock)};

    auto change(ChangeId changeId, boost::optional<std::string> changed,
            boost::optional<std::string> arguments, Revision revision, ChangeType type) const {
        return ::macs::ChangeFactory()
            .changeId(changeId)
            .type(type)
            .revision(revision)
            .arguments(arguments)
            .changed(changed)
            .release();
    }
    auto change(std::string changed, ChangeType type) const {
        return change(someChangeId, changed, someArgumentsJson, someRevision, type);
    }
    auto change(std::string changed, std::string arguments, ChangeType type) const {
        return change(someChangeId, changed, arguments, someRevision, type);
    }
    auto envelope(macs::Mid mid) const {
        return macs::EnvelopeFactory().mid(mid).release();
    }
    auto midWithMimes(macs::Mid mid) const {
        return MidWithMimes{mid, {}, {}};
    }
};

TEST_F(ChangeComposerAccessImplTest, composeForPutChange_callGetByIdsForGivenMidsAndGetAllLabels) {
    const auto changedJson = R"json([{"mid": 10000}, {"mid": 20000}])json";
    InSequence s;
    EXPECT_CALL(mailboxMock, getAllLabels(_));
    EXPECT_CALL(mailboxMock, withMimes()).WillOnce(ReturnRef(mailboxWithMimesMock));
    EXPECT_CALL(mailboxWithMimesMock, byMids(MidList{"10000", "20000"})).WillOnce(ReturnRef(mailboxWithMimesMock));
    EXPECT_CALL(mailboxWithMimesMock, get(_)).Times(1);

    composer(change(changedJson, ChangeType::store), Yield());
}

TEST_F(ChangeComposerAccessImplTest, compose_ReturnsChangeWithRightIdAndRevision) {
    EXPECT_CALL(mailboxMock, getAllLabels(_));
    EXPECT_CALL(mailboxMock, withMimes()).WillOnce(ReturnRef(mailboxWithMimesMock));
    EXPECT_CALL(mailboxWithMimesMock, byMids(_)).WillOnce(ReturnRef(mailboxWithMimesMock));
    EXPECT_CALL(mailboxWithMimesMock, get(_)).Times(1);

    auto ret = composer(change(100, someChangedJson, someArgumentsJson, 3333, ChangeType::store), Yield());

    EXPECT_EQ(ret->revision(), 3333ul);
    EXPECT_EQ(ret->id(), 100);
}

TEST_F(ChangeComposerAccessImplTest, compose_ForPutChangeReturnsChangeWhichCallPut) {
    const auto e1 = envelope("1");
    const auto e2 = envelope("2");
    const auto envWithMimes1 = EnvelopeWithMimes(e1, {});
    const auto envWithMimes2 = EnvelopeWithMimes(e2, {});
    const auto lbls = makeSet(label("1"));
    const auto changedJson = R"json([{"mid": 10000}, {"mid": 20000}])json";

    EXPECT_CALL(mailboxMock, getAllLabels(_))
        .WillOnce(Return(lbls));
    EXPECT_CALL(mailboxMock, withMimes()).WillOnce(ReturnRef(mailboxWithMimesMock));
    EXPECT_CALL(mailboxWithMimesMock, byMids(_)).WillOnce(ReturnRef(mailboxWithMimesMock));
    EXPECT_CALL(mailboxWithMimesMock, get(_)).WillOnce(Return(
                std::vector<EnvelopeWithMimes>{envWithMimes1, envWithMimes2}));
    EXPECT_CALL(subscribedFolderMock, put(envWithMimes1, _));
    EXPECT_CALL(subscribedFolderMock, put(envWithMimes2, _));

    composer(change(changedJson, ChangeType::copy), Yield())->
        apply(subscribedFolderMock);
}

TEST_F(ChangeComposerAccessImplTest, composeForPutChange_callGetByIdsForGivenMids_withOutdatedLabelsCache_resetsCacheAndRetryQuery) {
    const auto changedJson = R"json([{"mid": 10000}, {"mid": 20000}])json";
    InSequence s;
    EXPECT_CALL(mailboxMock, getAllLabels(_));
    EXPECT_CALL(mailboxMock, withMimes()).WillOnce(ReturnRef(mailboxWithMimesMock));
    EXPECT_CALL(mailboxWithMimesMock, byMids(MidList{"10000", "20000"})).WillOnce(ReturnRef(mailboxWithMimesMock));
    EXPECT_CALL(mailboxWithMimesMock, get(_)).WillOnce(Invoke([](Yield yield){
        yield.error(macs::error_code{macs::error::noSuchLabel});
        return std::vector<EnvelopeWithMimes>{};
    }));
    EXPECT_CALL(mailboxMock, resetLabelsCache()).WillOnce(Return());
    EXPECT_CALL(mailboxWithMimesMock, get(_)).WillOnce(Return(std::vector<EnvelopeWithMimes>{}));

    composer(change(changedJson, ChangeType::store), Yield());
}

TEST_F(ChangeComposerAccessImplTest, composeForPutChange_callGetByIdsForGivenMids_withOutdatedLabelsCache_throwsOnUpdatedLabelsCacheMiss) {
    const auto changedJson = R"json([{"mid": 10000}, {"mid": 20000}])json";
    InSequence s;
    EXPECT_CALL(mailboxMock, getAllLabels(_));
    EXPECT_CALL(mailboxMock, withMimes()).WillOnce(ReturnRef(mailboxWithMimesMock));
    EXPECT_CALL(mailboxWithMimesMock, byMids(MidList{"10000", "20000"})).WillOnce(ReturnRef(mailboxWithMimesMock));
    EXPECT_CALL(mailboxWithMimesMock, get(_)).WillOnce(Invoke([](Yield yield){
        yield.error(macs::error_code{macs::error::noSuchLabel});
        return std::vector<EnvelopeWithMimes>{};
    }));
    EXPECT_CALL(mailboxMock, resetLabelsCache()).WillOnce(Return());
    EXPECT_CALL(mailboxWithMimesMock, get(_)).WillOnce(Invoke([](Yield yield){
        yield.error(macs::error_code{macs::error::noSuchLabel});
        return std::vector<EnvelopeWithMimes>{};
    }));

    EXPECT_THROW(composer(change(changedJson, ChangeType::store), Yield()), std::exception);
}

TEST_F(ChangeComposerAccessImplTest, composeForMoveChange_callGetByIdsForGivenMidsAndGetAllLabels) {
    const auto changedJson = R"json([{"mid": 10000}, {"mid": 20000}])json";
    const auto argsJson = R"json({"fid": 1})json";
    InSequence s;
    EXPECT_CALL(mailboxMock, getAllLabels(_));
    EXPECT_CALL(mailboxMock, withMimes()).WillOnce(ReturnRef(mailboxWithMimesMock));
    EXPECT_CALL(mailboxWithMimesMock, byMids(MidList{"10000", "20000"})).WillOnce(ReturnRef(mailboxWithMimesMock));
    EXPECT_CALL(mailboxWithMimesMock, get(_)).Times(1);

    composer(change(changedJson, argsJson, ChangeType::move), Yield());
}

TEST_F(ChangeComposerAccessImplTest, composeForMoveChange_withMoveToSharedFolder_returnsChangeWhichCallPut) {
    const auto e1 = envelope("1");
    const auto e2 = envelope("2");
    const auto envWithMimes1 = EnvelopeWithMimes(e1, {});
    const auto envWithMimes2 = EnvelopeWithMimes(e2, {});
    const auto lbls = makeSet(label("1"));
    const auto changedJson = R"json([{"mid": 10000}, {"mid": 20000}])json";
    const auto argsJson = R"json({"fid": 1})json";
    const SharedFolderCoordinates coords {{"owner"}, "1"};

    EXPECT_CALL(mailboxMock, getAllLabels(_))
        .WillOnce(Return(lbls));
    EXPECT_CALL(mailboxMock, withMimes()).WillOnce(ReturnRef(mailboxWithMimesMock));
    EXPECT_CALL(mailboxWithMimesMock, byMids(_)).WillOnce(ReturnRef(mailboxWithMimesMock));
    EXPECT_CALL(mailboxWithMimesMock, get(_)).WillOnce(Return(
                std::vector<EnvelopeWithMimes>{envWithMimes1, envWithMimes2}));
    EXPECT_CALL(subscribedFolderMock, coordinates()).WillOnce(ReturnRef(coords));
    EXPECT_CALL(subscribedFolderMock, put(envWithMimes1, _));
    EXPECT_CALL(subscribedFolderMock, put(envWithMimes2, _));

    composer(change(changedJson, argsJson, ChangeType::move), Yield())->
        apply(subscribedFolderMock);
}

TEST_F(ChangeComposerAccessImplTest, composeForMoveChange_withMoveFromSharedFolder_returnsChangeWhichCallErase) {
    const auto e1 = envelope("1");
    const auto e2 = envelope("2");
    const auto envWithMimes1 = EnvelopeWithMimes(e1, {});
    const auto envWithMimes2 = EnvelopeWithMimes(e2, {});
    const auto lbls = makeSet(label("1"));
    const auto changedJson = R"json([{"mid": 10000}, {"mid": 20000}])json";
    const auto argsJson = R"json({"fid": 1})json";
    const SharedFolderCoordinates coords {{"owner"}, "2"};

    EXPECT_CALL(mailboxMock, getAllLabels(_))
        .WillOnce(Return(lbls));
    EXPECT_CALL(mailboxMock, withMimes()).WillOnce(ReturnRef(mailboxWithMimesMock));
    EXPECT_CALL(mailboxWithMimesMock, byMids(_)).WillOnce(ReturnRef(mailboxWithMimesMock));
    EXPECT_CALL(mailboxWithMimesMock, get(_)).WillOnce(Return(
                std::vector<EnvelopeWithMimes>{envWithMimes1, envWithMimes2}));
    EXPECT_CALL(subscribedFolderMock, coordinates()).WillOnce(ReturnRef(coords));
    EXPECT_CALL(subscribedFolderMock, erase(macs::MidVec{e1.mid(), e2.mid()}, someRevision));

    composer(change(changedJson, argsJson, ChangeType::move), Yield())->
        apply(subscribedFolderMock);
}

TEST_F(ChangeComposerAccessImplTest, composeForMoveChange_callGetByIdsForGivenMids_withOutdatedLabelsCache_resetsCacheAndRetryQuery) {
    const auto changedJson = R"json([{"mid": 10000}, {"mid": 20000}])json";
    const auto argsJson = R"json({"fid": 1})json";
    InSequence s;
    EXPECT_CALL(mailboxMock, getAllLabels(_));
    EXPECT_CALL(mailboxMock, withMimes()).WillOnce(ReturnRef(mailboxWithMimesMock));
    EXPECT_CALL(mailboxWithMimesMock, byMids(MidList{"10000", "20000"})).WillOnce(ReturnRef(mailboxWithMimesMock));
    EXPECT_CALL(mailboxWithMimesMock, get(_)).WillOnce(Invoke([](Yield yield){
        yield.error(macs::error_code{macs::error::noSuchLabel});
        return std::vector<EnvelopeWithMimes>{};
    }));
    EXPECT_CALL(mailboxMock, resetLabelsCache()).WillOnce(Return());
    EXPECT_CALL(mailboxWithMimesMock, get(_)).WillOnce(Return(std::vector<EnvelopeWithMimes>{}));

    composer(change(changedJson, argsJson, ChangeType::move), Yield());
}

TEST_F(ChangeComposerAccessImplTest, composeForMoveChange_callGetByIdsForGivenMids_withOutdatedLabelsCache_throwsOnUpdatedLabelsCacheMiss) {
    const auto changedJson = R"json([{"mid": 10000}, {"mid": 20000}])json";
    const auto argsJson = R"json({"fid": 1})json";
    InSequence s;
    EXPECT_CALL(mailboxMock, getAllLabels(_));
    EXPECT_CALL(mailboxMock, withMimes()).WillOnce(ReturnRef(mailboxWithMimesMock));
    EXPECT_CALL(mailboxWithMimesMock, byMids(MidList{"10000", "20000"})).WillOnce(ReturnRef(mailboxWithMimesMock));
    EXPECT_CALL(mailboxWithMimesMock, get(_)).WillOnce(Invoke([](Yield yield){
        yield.error(macs::error_code{macs::error::noSuchLabel});
        return std::vector<EnvelopeWithMimes>{};
    }));
    EXPECT_CALL(mailboxMock, resetLabelsCache()).WillOnce(Return());
    EXPECT_CALL(mailboxWithMimesMock, get(_)).WillOnce(Invoke([](Yield yield){
        yield.error(macs::error_code{macs::error::noSuchLabel});
        return std::vector<EnvelopeWithMimes>{};
    }));

    EXPECT_THROW(composer(change(changedJson, argsJson, ChangeType::move), Yield()), std::exception);
}

TEST_F(ChangeComposerAccessImplTest, compose_ForEraseChangeReturnsChangeWhichCallErase) {
    const auto changedJson = R"json([{"mid": 10000}, {"mid": 20000}])json";
    EXPECT_CALL(subscribedFolderMock, erase(macs::MidVec{"10000", "20000"}, someRevision));

    composer(change(changedJson, ChangeType::delete_), Yield())->
        apply(subscribedFolderMock);
}


TEST_F(ChangeComposerAccessImplTest, compose_ForUpdateChangeReturnChangeWhichCallMarkAndUnmark) {
    const std::string changedJson = R"json([{"mid": 111}, {"mid": 222}])json";
    const std::string argumentsJson = R"json({
        "seen": true,
        "recent": false,
        "deleted": null,
        "lids_add": [42],
        "lids_del": []
    })json";
    const auto seenLabel = label("SEEN_LABEL", Symbol::seen_label);
    const auto recentLabel = label("RECENT_LABEL", Symbol::recent_label);
    const auto someUserLabel = label("42", "Main question.");
    const auto labelsDict = makeSet(
        seenLabel, recentLabel, someUserLabel,
        label("DELETED_LABEL", Symbol::deleted_label)
    );

    EXPECT_CALL(mailboxMock, getAllLabels(_)).WillOnce(Return(labelsDict));
    EXPECT_CALL(subscribedFolderMock, mark("111", UnorderedElementsAre(seenLabel, someUserLabel), someRevision));
    EXPECT_CALL(subscribedFolderMock, unmark("111", UnorderedElementsAre(recentLabel), someRevision));
    EXPECT_CALL(subscribedFolderMock, mark("222", UnorderedElementsAre(seenLabel, someUserLabel), someRevision));
    EXPECT_CALL(subscribedFolderMock, unmark("222", UnorderedElementsAre(recentLabel), someRevision));

    composer(change(someChangeId, changedJson, argumentsJson, someRevision, ChangeType::update), Yield())->
        apply(subscribedFolderMock);
}

TEST_F(ChangeComposerAccessImplTest, compose_ForUpdateChangeResetsLabelsCacheIfNotFound) {
    const std::string changedJson = R"json([{"mid": 111}])json";
    const std::string argumentsJson = R"json({
        "seen": null,
        "recent": null,
        "deleted": null,
        "lids_add": [42],
        "lids_del": []
    })json";
    const auto someUserLabel = label("42", "Main question.");
    const auto labelsDict = makeSet(someUserLabel);

    InSequence seq;
    EXPECT_CALL(mailboxMock, getAllLabels(_)).WillOnce(Return(::macs::LabelSet{}));
    EXPECT_CALL(mailboxMock, resetLabelsCache());
    EXPECT_CALL(mailboxMock, getAllLabels(_)).WillOnce(Return(labelsDict));
    EXPECT_CALL(subscribedFolderMock, mark("111", UnorderedElementsAre(someUserLabel), someRevision));

    composer(change(someChangeId, changedJson, argumentsJson, someRevision, ChangeType::update), Yield())->
        apply(subscribedFolderMock);
}

TEST_F(ChangeComposerAccessImplTest, compose_ForJoinThreadsChangeReturnsChangeWhichCallJoinThreads) {
    const std::string argumentsJson = R"json({
        "tid": 100,
        "join_tids": [200, 300]
    })json";
    EXPECT_CALL(subscribedFolderMock, joinThreads("100", UnorderedElementsAre("200", "300"), someRevision));

    composer(change(someChangeId, someChangedJson, argumentsJson, someRevision, ChangeType::threadsJoin), Yield())->
        apply(subscribedFolderMock);
}


TEST_F(ChangeComposerAccessImplTest, compose_ThrowWhenGotChangeWithUnsupportedType) {
    const auto c = change(someChangeId, someChangedJson, someArgumentsJson, someRevision, ChangeType::labelCreate);
    EXPECT_THROW(composer(c, Yield()), std::logic_error);
}

}
