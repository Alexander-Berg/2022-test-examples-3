#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <src/access_impl/revision_cache.h>
#include <library/cpp/testing/gtest_boost_extensions/extensions.h>

namespace {
using namespace ::testing;
using Uid = ::doberman::Uid;
using Fid = ::doberman::Fid;
using Coords = ::doberman::logic::SharedFolderCoordinates;
using Revision = ::doberman::Revision;

using RevisionCache = ::doberman::access_impl::RevisionCache;

struct RevisionCacheTest : public Test {
    RevisionCache cache;

    const Uid uid = "suid";
    const Coords coords = Coords{{"ouid"}, "fid"};
};

TEST_F(RevisionCacheTest, get_EmptyRevisionCacheReturnsNone) {
    EXPECT_EQ(cache.get(uid, coords), boost::none);
}

TEST_F(RevisionCacheTest, get_RevisionCacheReturnsInserted) {
    EXPECT_EQ(cache.get(uid, coords), boost::none);

    cache.set(uid, coords, Revision(100));
    EXPECT_EQ(cache.get(uid, coords), boost::optional<Revision>(100));
}

TEST_F(RevisionCacheTest, update_UpdatesRevisionCacheIfNewRevisionIsGreater) {
    EXPECT_EQ(cache.get(uid, coords), boost::none);

    cache.set(uid, coords, Revision(100));
    EXPECT_EQ(cache.get(uid, coords), boost::optional<Revision>(100));

    cache.set(uid, coords, Revision(200));
    EXPECT_EQ(cache.get(uid, coords), boost::optional<Revision>(200));
}

TEST_F(RevisionCacheTest, update_DoesNotUpdateRevisionCacheIfNewRevisionIsLess) {
    EXPECT_EQ(cache.get(uid, coords), boost::none);

    cache.set(uid, coords, Revision(100));
    EXPECT_EQ(cache.get(uid, coords), boost::optional<Revision>(100));

    cache.set(uid, coords, Revision(50));
    EXPECT_EQ(cache.get(uid, coords), boost::optional<Revision>(100));
}

}
