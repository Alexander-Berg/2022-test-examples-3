#include "wrap_yield.h"
#include "timer.h"
#include "cache_mocks.h"
#include "job_finder_mock.h"
#include <src/access_impl/job_finder.h>

namespace {

using namespace ::testing;
using namespace ::doberman::testing;
using ::doberman::ShardId;
using ::doberman::WorkerId;
using ::doberman::Job;
using ::doberman::logic::FindJobParams;

struct ShardMock {
    auto& subscriptions() const { return *this;}
    auto operator() (ShardId id) const { return getShard(id);}
    MOCK_METHOD(boost::optional<::macs::WorkerId>, findAJob, (std::chrono::seconds,
            std::string, std::string, std::string, Yield), (const));
    MOCK_METHOD(const ShardMock*, getShard, (ShardId), (const));
};

struct Shard {};

using ShardMap = std::map<std::string, Shard>;

struct SharpeiClientMock {
    MOCK_METHOD(ShardMap, stat, (Yield), (const));
};


struct JobFinderAcessImplTest : public Test {
    ShardMock shard;
    SharpeiClientMock sharpei;
    NiceMock<ClockMock> clock;

    auto makeFinder() {
        return doberman::access_impl::makeJobFinder(&sharpei, &shard, 0, clock);
    }
};

TEST_F(JobFinderAcessImplTest, getShards_withShardsFromSharpei_returnsShardsId) {
    auto finder = makeFinder();
    auto ctx = finder.makeContext();
    EXPECT_CALL(sharpei, stat(_)).WillOnce(Return(ShardMap{{"shard1", {}}, {"shard2", {}}}));
    EXPECT_THAT(finder.getShards(ctx, Yield()), ElementsAre("shard1", "shard2"));
}

TEST_F(JobFinderAcessImplTest, askForJob_withShardIdAndWorkerIdFormShard_returnsJob) {
    auto finder = makeFinder();
    auto ctx = finder.makeContext();
    EXPECT_CALL(shard, getShard("shard1")).WillOnce(Return(&shard));
    EXPECT_CALL(shard, findAJob(_, _, _, _, _))
        .WillOnce(Return(boost::optional<::macs::WorkerId>("WorkerId")));
    Job job;
    job.workerId = "WorkerId";
    job.shardId = "shard1";
    EXPECT_EQ(finder.askForJob(ctx, "shard1", FindJobParams(), Yield()), job);
}


TEST_F(JobFinderAcessImplTest, askForJob_withShardIdAndNoWorkerIdFormShard_returnsNone) {
    auto finder = makeFinder();
    auto ctx = finder.makeContext();
    EXPECT_CALL(shard, getShard("shard1")).WillOnce(Return(&shard));
    EXPECT_CALL(shard, findAJob(_, _, _, _, _))
        .WillOnce(Return(boost::none));
    EXPECT_EQ(finder.askForJob(ctx, "shard1", FindJobParams(), Yield()), boost::none);
}

}
