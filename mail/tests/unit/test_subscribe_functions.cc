#include <gtest/gtest.h>

#include "helper_context.h"
#include "helper_macs.h"
#include <internal/server/handlers/subscribe.h>
#include <macs/folders_repository.h>
#include <macs/io.h>
#include <macs_pg/subscribed_folders/factory.h>
#include <macs_pg/subscription/factory.h>

using namespace testing;

namespace std {
template <typename Stream>
Stream& operator<<(Stream& out, const york::server::handlers::FidPath& f) {
    return out << "{" << std::get<macs::Fid>(f) << ","
               << std::get<macs::Folder::Path>(f).toString() << "}";
}
}

namespace york {
namespace tests {

struct SubscribeFunctionsTest: public Test {
    ContextMock ctx;
    MacsMock<sync> macsOwner;
    MacsMock<sync> macsSubscriber;

    using State = macs::pg::SubscriptionState;
    macs::Subscription subscription(macs::SubscriptionId id, State state) {
        return macs::SubscriptionFactory()
                .subscriptionId(id)
                .state(state)
                .release();
    }
};

TEST_F(SubscribeFunctionsTest, getSharedSubfoldersPath_withNotSharedFid_returnsEmptyVector) {
    server::handlers::FidPathVec expected {
        std::make_tuple("fid", macs::Folder::Path(PathValue{"a", "b"}))
    };

    EXPECT_CALL(macsOwner, getAllSharedFolders(_)).WillOnce(Return(macs::FidVec{}));

    auto result = server::handlers::getSharedSubfoldersPath(macsOwner, "fid",
            macs::Folder::Path(PathValue{"a", "b"}), false, macs::io::use_sync);

    ASSERT_THAT(result, IsEmpty());
}

TEST_F(SubscribeFunctionsTest, getSharedSubfoldersPath_withRecursiveFalse_returnsOnlyRootFolder) {
    server::handlers::FidPathVec expected {
        std::make_tuple("fid", macs::Folder::Path(PathValue{"a", "b"}))
    };

    EXPECT_CALL(macsOwner, getAllSharedFolders(_)).WillOnce(Return(macs::FidVec{"fid"}));

    auto result = server::handlers::getSharedSubfoldersPath(macsOwner, "fid",
            macs::Folder::Path(PathValue{"a", "b"}), false, macs::io::use_sync);

    ASSERT_THAT(result, UnorderedElementsAreArray(expected));
}

TEST_F(SubscribeFunctionsTest, getSharedSubfoldersPath_withRecursiveTrue_returnsSubfoldersWitConvertedPaths) {
    macs::FoldersMap fs = {
        {"fid", macs::FolderFactory().fid("fid").name("bbs").parentId("")},
        {"fid1", macs::FolderFactory().fid("fid1").name("folder1").parentId("fid")},
        {"fid2", macs::FolderFactory().fid("fid2").name("child1").parentId("fid1")},
        {"fid3", macs::FolderFactory().fid("fid3").name("folder2").parentId("fid")},
        {"other", macs::FolderFactory().fid("other").name("other").parentId("")}};
    auto fidRange = fs | boost::adaptors::map_keys;
    macs::FidVec fids(fidRange.begin(), fidRange.end());
    fs.insert({"notshared", macs::FolderFactory().fid("notshared").name("notshared").parentId("")});

    server::handlers::FidPathVec expected {
        std::make_tuple("fid", macs::Folder::Path(PathValue{"a", "b"})),
        std::make_tuple("fid1", macs::Folder::Path(PathValue{"a", "b", "folder1"})),
        std::make_tuple("fid2", macs::Folder::Path(PathValue{"a", "b", "folder1", "child1"})),
        std::make_tuple("fid3", macs::Folder::Path(PathValue{"a", "b", "folder2"}))
    };

    EXPECT_CALL(macsOwner, getAllSharedFolders(_)).WillOnce(Return(fids));
    EXPECT_CALL(macsOwner, getAllFolders(_)).WillOnce(Return(fs));

    auto result = server::handlers::getSharedSubfoldersPath(macsOwner, "fid",
            macs::Folder::Path(PathValue{"a", "b"}), true, macs::io::use_sync);

    ASSERT_THAT(result, UnorderedElementsAreArray(expected));
}


TEST_F(SubscribeFunctionsTest, getExistingFids_returnsEmptyFids_ifFolderDoesNotExist) {
    std::vector<std::tuple<macs::Fid, macs::Folder::Path>> folders {
        std::make_tuple("fid", macs::Folder::Path(PathValue{"bbs"}))
    };

    EXPECT_THAT(server::handlers::getExistingFids(folders, {}, {}), IsEmpty());
}

TEST_F(SubscribeFunctionsTest, getExistingFids_returnsEmptyFids_ifFolderExistsAndEmpty) {
    macs::FoldersMap fs;
    fs.insert({"sfid", macs::FolderFactory().fid("sfid").name("bbs").parentId("").messages(0)});
    std::vector<std::tuple<macs::Fid, macs::Folder::Path>> folders {
        std::make_tuple("fid", macs::Folder::Path(PathValue{"bbs"}))
    };

    EXPECT_THAT(server::handlers::getExistingFids(folders, macs::FolderSet(fs), {}), IsEmpty());
}

TEST_F(SubscribeFunctionsTest, getExistingFids_returnsFids_ifFolderExistsAndNotEmptyAndNotSubscribed) {
    macs::FoldersMap fs;
    fs.insert({"sfid", macs::FolderFactory().fid("sfid").name("bbs").parentId("").messages(1)});
    std::vector<std::tuple<macs::Fid, macs::Folder::Path>> folders {
        std::make_tuple("fid", macs::Folder::Path(PathValue{"bbs"}))
    };

    EXPECT_THAT(server::handlers::getExistingFids(folders, macs::FolderSet(fs), {}),
                ElementsAre("sfid"));
}

TEST_F(SubscribeFunctionsTest, getExistingFids_returnsEmptyFids_ifFolderSubscribed) {
    macs::FoldersMap fs;
    fs.insert({"sfid", macs::FolderFactory().fid("sfid").name("bbs").parentId("").messages(0)});
    std::vector<std::tuple<macs::Fid, macs::Folder::Path>> folders {
        std::make_tuple("fid", macs::Folder::Path(PathValue{"bbs"}))
    };

    macs::SubscribedFolder subf = macs::SubscribedFolderFactory()
            .fid("fid").ownerUid("ownerUid").ownerFid("fid").release();

    EXPECT_THAT(server::handlers::getExistingFids(folders, macs::FolderSet(fs), {subf}), IsEmpty());
}

TEST_F(SubscribeFunctionsTest, getExistingFids_returnsTrue_ifAtLeastOneExistsAndNotEmptyAndNotSubscribed) {
    macs::FoldersMap fs = {
        {"sfid2", macs::FolderFactory().fid("sfid2").name("bbs2").parentId("").messages(1)},
        {"sfid3", macs::FolderFactory().fid("sfid3").name("bbs3").parentId("").messages(1)}};
    std::vector<std::tuple<macs::Fid, macs::Folder::Path>> folders {
        std::make_tuple("fid1", macs::Folder::Path(PathValue{"bbs1"})),
        std::make_tuple("fid2", macs::Folder::Path(PathValue{"bbs2"})),
        std::make_tuple("fid3", macs::Folder::Path(PathValue{"bbs3"})),
        std::make_tuple("fid4", macs::Folder::Path(PathValue{"bbs4"}))
    };

    std::vector<macs::SubscribedFolder> subfs {
        macs::SubscribedFolderFactory()
            .fid("sfid2").ownerUid("ownerUid").ownerFid("fid2").release(),
        macs::SubscribedFolderFactory()
            .fid("sfid4").ownerUid("ownerUid").ownerFid("fid4").release()
    };

    EXPECT_THAT(server::handlers::getExistingFids(folders, macs::FolderSet(fs), subfs),
                ElementsAre("sfid3"));
}


TEST_F(SubscribeFunctionsTest, needCreate_returnsFalse_ifFolderIsSubscribed) {
    const auto folder = std::make_tuple("fid", macs::Folder::Path(PathValue{"bbs"}));
    macs::SubscribedFolder subf = macs::SubscribedFolderFactory()
            .fid("fid").ownerUid("ownerUid").ownerFid("fid").release();

    EXPECT_FALSE(server::handlers::needCreate(folder, {subf}));
}

TEST_F(SubscribeFunctionsTest, needCreate_returnsTrue_ifFolderIsNotSubscribed) {
    const auto folder = std::make_tuple("fid", macs::Folder::Path(PathValue{"bbs"}));

    EXPECT_TRUE(server::handlers::needCreate(folder, {}));
}

TEST_F(SubscribeFunctionsTest, getTerminating_getsSubscriptions_returnsOnlyTerminating) {
    const std::vector<macs::Subscription> subs {
        subscription(1, State::init),
        subscription(2, State::sync),
        subscription(3, State::discontinued),
        subscription(4, State::terminated),
        subscription(5, State::clear),
        subscription(6, State::clearFail),
        subscription(7, State::initFail)
    };

    const std::vector<macs::Subscription> expected {
        subscription(3, State::discontinued),
        subscription(4, State::terminated),
        subscription(5, State::clear),
        subscription(6, State::clearFail),
    };

    server::handlers::FidPathVec folders {
        std::make_tuple("fid", macs::Folder::Path(PathValue{"a", "b"}))
    };

    EXPECT_CALL(macsOwner, getByFids("uid", macs::FidVec{"fid"}, _)).WillOnce(Return(subs));

    auto result = server::handlers::getTerminating(macsOwner, folders, "uid", macs::io::use_sync);
    ASSERT_THAT(result, UnorderedElementsAreArray(expected));
}

} //namespace tests
} //namespace york
