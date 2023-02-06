#include <gmock/gmock.h>
#include <gtest/gtest.h>

#include <internal/db/adaptors/meta_cache_based_meta_master_provider.h>

#include <mail/sharpei/tests/mocks.h>
#include <mail/sharpei/tests/util.h>

#include <memory>

using namespace sharpei;
using namespace ::testing;

namespace {

TEST(MetaCacheBasedMetaMasterProviderTests, positive) {
    const auto sasHost = makeDatabase(Role::Replica, Status::Alive, State{.lag = 0}, "sas");
    const auto vlaHost = makeDatabase(Role::Master, Status::Alive, State{.lag = 0}, "vla");
    const auto metaCache = makeCache({sasHost, vlaHost});

    ProfilerPtr profiler(new StrictMock<MockProfiler>);
    Scribe scribe(profiler);
    const auto masterProvider = std::make_shared<db::MetaCacheBasedMetaMasterProvider>(metaCache, scribe);
    std::string result;
    ExplainedError error(Error::ok);
    masterProvider->getMaster([&](auto value) { result = value; }, [&](auto ec) { error = ec; });
    EXPECT_EQ(result, vlaHost.address().host);
    EXPECT_EQ(error, Error::ok);
}

TEST(MetaCacheBasedMetaMasterProviderTests, negative) {
    const auto sasHost = makeDatabase(Role::Replica, Status::Alive, State{.lag = 0}, "sas");
    const auto vlaHost = makeDatabase(Role::Replica, Status::Alive, State{.lag = 0}, "vla");
    const auto metaCache = makeCache({sasHost, vlaHost});

    ProfilerPtr profiler(new StrictMock<MockProfiler>);
    Scribe scribe(profiler);
    const auto masterProvider = std::make_shared<db::MetaCacheBasedMetaMasterProvider>(metaCache, scribe);
    std::string result;
    ExplainedError error(Error::ok);
    masterProvider->getMaster([&](auto value) { result = value; }, [&](auto ec) { error = ec; });
    EXPECT_EQ(result, std::string{});
    EXPECT_EQ(error, Error::metaMasterProviderError);
}

}  // namespace
