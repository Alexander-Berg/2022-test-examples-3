#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <macs_pg/changelog/factory.h>
#include <src/meta/changed.h>
#include "macs_change_io.h"

namespace {

using namespace ::testing;
using namespace ::doberman::meta::changed;
using namespace ::doberman::meta::changed::detail;

using OptString = boost::optional<std::string>;
const ::doberman::Fid someFid = "42";

struct ChangedTest : public Test {
    auto makeChange(ChangeType type, OptString changed, OptString arguments) const {
        return ::macs::ChangeFactory().
            changeId(42).
            type(type).
            changed(changed).
            arguments(arguments).
            release();
    }
    auto makeChangeWithChanged(OptString changed) const {
        return makeChange(
            ChangeType::unknown,
            changed,
            boost::none
        );
    }
    auto makeChangeWithArguments(OptString arguments) const {
        return makeChange(
            ChangeType::unknown,
            boost::none,
            arguments
        );
    }
    auto makeChangeWithTypeAndArguments(ChangeType type, OptString arguments) const {
        return makeChange(
            type,
            boost::none,
            arguments
        );
    }
    auto makeChangeWithType(ChangeType type) const {
        return makeChange(
            type,
            boost::none,
            std::string("{}")
        );
    }

};

TEST_F(ChangedTest, isStoreChange_forStoreIsTrue) {
    EXPECT_TRUE(isStoreChange(ChangeType::store));
}

TEST_F(ChangedTest, isStoreChange_forCopyIsTrue) {
    EXPECT_TRUE(isStoreChange(ChangeType::copy));
}

TEST_F(ChangedTest, isStoreChange_forSyncStoreIsTrue) {
    EXPECT_TRUE(isStoreChange(ChangeType::syncStore));
}

TEST_F(ChangedTest, isStoreChange_forLabelCreateIsFalse) {
    EXPECT_FALSE(isStoreChange(ChangeType::labelCreate));
}

TEST_F(ChangedTest, isEraseChange_forDeleteIsTrue) {
    EXPECT_TRUE(isEraseChange(ChangeType::delete_));
}

TEST_F(ChangedTest, isEraseChange_forSyncDeleteIsTrue) {
    EXPECT_TRUE(isEraseChange(ChangeType::syncDelete));
}

TEST_F(ChangedTest, isEraseChange_forStoreIsFalse) {
    EXPECT_FALSE(isEraseChange(ChangeType::store));
}

TEST_F(ChangedTest, isUpdateChange_forUpdateIsTrue) {
    EXPECT_TRUE(isUpdateChange(ChangeType::update));
}

TEST_F(ChangedTest, isUpdateChange_forSyncUpdateIsTrue) {
    EXPECT_TRUE(isUpdateChange(ChangeType::syncUpdate));
}

TEST_F(ChangedTest, isUpdateChange_forStoreIsFalse) {
    EXPECT_FALSE(isUpdateChange(ChangeType::store));
}

TEST_F(ChangedTest, isJoinThreadsChange_forThreadsJoinIsTrue) {
    EXPECT_TRUE(isJoinThreadsChange(ChangeType::threadsJoin));
}

TEST_F(ChangedTest, isJoinThreadsChange_forSyncThreadsJoinIsTrue) {
    EXPECT_TRUE(isJoinThreadsChange(ChangeType::syncThreadsJoin));
}

TEST_F(ChangedTest, isJoinThreadsChange_forStoreIsFalse) {
    EXPECT_FALSE(isJoinThreadsChange(ChangeType::store));
}

TEST_F(ChangedTest, getMidsFromChange_ForGoodJsonReturnGivenMids) {
    using ::macs::Mid;

    const std::string storeChanged = R"json(
    [{
        "mid": 10, "fid": 1,
        "tid": 1, "lids": [],
        "src_fid": 3,
        "seen": false, "recent": true, "deleted": true
     }, {
        "mid": 20, "fid": 1,
        "src_fid": 2,
        "tid": null, "lids": [1, 2, 3],
        "seen": true, "recent": true, "deleted": false
     }])json";
    auto ret = getMidsFromChange(makeChangeWithChanged(storeChanged));
    EXPECT_THAT(ret, ElementsAre(Mid("10"), Mid("20")));
}

TEST_F(ChangedTest, getMidsFromChange_ThrowForChangeWithoutChanged) {
    EXPECT_THROW(getMidsFromChange(makeChangeWithChanged(boost::none)), std::domain_error);
}

TEST_F(ChangedTest, getMidsFromChange_ThrowForChangeWithEmptyChanged) {
    EXPECT_THROW(getMidsFromChange(makeChangeWithChanged(std::string("[]"))), std::domain_error);
}

TEST_F(ChangedTest, getArguments_WithMoveChangeArgumentsReturnGivenFid) {
    const std::string argsJson = R"json({"fid": 42})json";
    auto ret = getArguments<MoveArguments>(makeChangeWithArguments(argsJson));
    EXPECT_THAT(ret.fid, 42);
}

TEST_F(ChangedTest, getArguments_ThrowForChangeWithoutArguments) {
    auto c = makeChangeWithArguments(boost::none);
    EXPECT_THROW(getArguments<MoveArguments>(c), std::domain_error);
}

TEST_F(ChangedTest, getJoinedThreads_ForGoodJsonReturnGivenTids) {
    using ::macs::ThreadId;
    const std::string joinArguments = R"json(
    {
        "tid": 100,
        "join_tids": [200, 300]
    })json";
    auto ret = getJoinedThreads(makeChangeWithArguments(joinArguments));
    EXPECT_EQ(std::get<0>(ret), ThreadId("100"));
    EXPECT_THAT(std::get<1>(ret), ElementsAre(ThreadId("200"), ThreadId("300")));

}

TEST_F(ChangedTest, getActionByChange_ForStoreChangeReturnPut) {
    const auto c = makeChangeWithType(ChangeType::store);
    EXPECT_THAT(getActionByChange(c), ChangeAction::put);
}

TEST_F(ChangedTest, getActionByChange_ForDeleteChangeReturnErase) {
    const auto c = makeChangeWithType(ChangeType::delete_);
    EXPECT_THAT(getActionByChange(c), ChangeAction::erase);
}

TEST_F(ChangedTest, getActionByChange_ForMoveChangeReturnMove) {
    const auto c = makeChangeWithType(ChangeType::move);
    EXPECT_THAT(getActionByChange(c), ChangeAction::move);
}

TEST_F(ChangedTest, getActionByChange_ForUpdateChangeReturnUpdate) {
    const auto c = makeChangeWithType(ChangeType::update);
    EXPECT_THAT(getActionByChange(c), ChangeAction::update);
}

TEST_F(ChangedTest, getActionByChange_ForJoinThreadsChangeReturnJoinThreads) {
    const auto c = makeChangeWithType(ChangeType::threadsJoin);
    EXPECT_THAT(getActionByChange(c), ChangeAction::joinThreads);
}

}
