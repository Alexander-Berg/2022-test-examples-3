#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <internal/query/threads_meta.h>
#include "mapper_mock.h"

namespace boost {

template <typename T>
std::ostream & operator << ( std::ostream & s, const optional<std::vector<T>> & v ) {
    if(v) {
        for( const auto & i : *v ) {
            s << i << ",";
        }
    } else {
        s << "NIL";
    }
    return s;
}

}


namespace {

using namespace testing;

using namespace macs;
using namespace macs::pg::query;

struct ThreadMetaHelperTest : public Test {
    MockMapper mapper;
    struct Helper : public pgg::query::Helper<Helper, macs::ThreadMeta> {};
    using optString = boost::optional<std::string>;
    using optHash = boost::optional<Hash>;
    using optHashVec = boost::optional<std::vector<Hash>>;

    const optHashVec hashes = optHashVec({"10","20","30"});
};

#define EXPECT_CALL_ONCE_T( m, call ) EXPECT_CALL(m->mock(), call ).WillOnce(Return())

TEST_F(ThreadMetaHelperTest, threadMetaHelper_withNoThreadMetaSet_mapNullValues) {
    const auto strMock = mapper.mapValueMock<std::string>();
    const auto nullStrMock = mapper.mapValueMock<optString>();
    const auto optHashMock = mapper.mapValueMock<optHash>();
    const auto optHashVecMock = mapper.mapValueMock<optHashVec>();

    EXPECT_CALL_ONCE_T(strMock, mapValue("force-new-thread", "mergeRule"));
    EXPECT_CALL_ONCE_T(optHashVecMock, mapValue(optHashVec(), "referenceHashes"));
    EXPECT_CALL_ONCE_T(optHashMock, mapValue(optHash(), "inReplyToHash"));
    EXPECT_CALL_ONCE_T(optHashMock, mapValue(optHash(), "hashValue"));
    EXPECT_CALL_ONCE_T(nullStrMock, mapValue(optString(), "hashNamespace"));
    EXPECT_CALL_ONCE_T(optHashMock, mapValue(optHash(), "hashKey"));
    EXPECT_CALL_ONCE_T(strMock, mapValue("", "sortOptions"));
    Helper().map(mapper);
}

TEST_F(ThreadMetaHelperTest, threadMetaHelper_withThreadMetaSet_mapActualValues) {
    ThreadMeta meta;
    meta.mergeRule = ThreadsMergeRules::references;
    meta.referenceHashes = *hashes;
    meta.inReplyToHash = "2048";
    meta.hash.value = "42";
    meta.hash.ns = ThreadsHashNamespaces::from;
    meta.hash.key = "1024";
    meta.sortOptions = "opts";

    const auto strMock = mapper.mapValueMock<std::string>();
    const auto optStrMock = mapper.mapValueMock<optString>();
    const auto optHashMock = mapper.mapValueMock<optHash>();
    const auto optVecMock = mapper.mapValueMock<optHashVec>();

    EXPECT_CALL_ONCE_T(strMock, mapValue("references", "mergeRule"));
    EXPECT_CALL_ONCE_T(optVecMock, mapValue(hashes, "referenceHashes"));
    EXPECT_CALL_ONCE_T(optHashMock, mapValue(optHash("2048"), "inReplyToHash"));
    EXPECT_CALL_ONCE_T(optHashMock, mapValue(optHash("42"), "hashValue"));
    EXPECT_CALL_ONCE_T(optStrMock, mapValue(optString("from"), "hashNamespace"));
    EXPECT_CALL_ONCE_T(optHashMock, mapValue(optHash("1024"), "hashKey"));
    EXPECT_CALL_ONCE_T(strMock, mapValue("opts", "sortOptions"));
    Helper().threadMeta(meta).map(mapper);
}

TEST_F(ThreadMetaHelperTest, threadMetaHelper_withInReplyToHashEmpty_mapNullForInReplyToHash) {
    ThreadMeta meta;
    meta.mergeRule = ThreadsMergeRules::references;
    meta.referenceHashes = *hashes;
    meta.inReplyToHash = "";
    meta.hash.value = "42";
    meta.hash.ns = ThreadsHashNamespaces::from;
    meta.hash.key = "1024";
    meta.sortOptions = "opts";

    const auto strMock = mapper.mapValueMock<std::string>();
    const auto optStrMock = mapper.mapValueMock<optString>();
    const auto optHashMock = mapper.mapValueMock<optHash>();
    const auto optVecMock = mapper.mapValueMock<optHashVec>();

    EXPECT_CALL_ONCE_T(strMock, mapValue("references", "mergeRule"));
    EXPECT_CALL_ONCE_T(optVecMock, mapValue(hashes, "referenceHashes"));
    EXPECT_CALL_ONCE_T(optHashMock, mapValue(optHash(), "inReplyToHash"));
    EXPECT_CALL_ONCE_T(optHashMock, mapValue(optHash("42"), "hashValue"));
    EXPECT_CALL_ONCE_T(optStrMock, mapValue(optString("from"), "hashNamespace"));
    EXPECT_CALL_ONCE_T(optHashMock, mapValue(optHash("1024"), "hashKey"));
    EXPECT_CALL_ONCE_T(strMock, mapValue("opts", "sortOptions"));
    Helper().threadMeta(meta).map(mapper);
}

} // namespace
