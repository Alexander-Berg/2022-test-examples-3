#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include "job_finder_mock.h"
#include "log_mock.h"
#include <src/logic/job_finder.h>

namespace {

using namespace ::testing;
using namespace ::doberman::testing;
using ::doberman::WorkerId;
using ::doberman::ShardId;
using ::doberman::Job;

struct JobFinderTest : public Test {
    JobFinderAccessMock mock;
    bool runStatus = true;
    auto makeFinder() {
        return doberman::logic::makeJobFinder(&mock, runStatus, ::logdog::none);
    }

    auto ReturnJob(macs::WorkerId w, ShardId s) const {
        Job job;
        job.workerId = std::move(w);
        job.shardId = std::move(s);
        return Return(boost::optional<Job>(job));
    }

    auto throwException() const {
        throw mail_errors::system_error(mail_errors::error_code{1, boost::system::system_category()});
    }
};

TEST_F(JobFinderTest, find_withJobFound_returnsJob) {
    auto finder = makeFinder();
    EXPECT_CALL(mock, getShards(_)).WillOnce(Return(std::vector<ShardId>{"shard"}));
    EXPECT_CALL(mock, askForJob(_, "shard", _)).WillOnce(ReturnJob("workerId", "shard"));
    Job job;
    job.workerId = "workerId";
    job.shardId = "shard";
    ASSERT_EQ(*(finder.find(FindJobParams())), job);
}

TEST_F(JobFinderTest, find_withNoJobFound_triesGetShardsAndFindJob) {
    auto finder = makeFinder();
    InSequence s;
    EXPECT_CALL(mock, getShards(_)).WillOnce(Return(std::vector<ShardId>{"shard"}));
    EXPECT_CALL(mock, askForJob(_, "shard", _)).WillOnce(Return(boost::none));
    EXPECT_CALL(mock, getShards(_)).WillOnce(Return(std::vector<ShardId>{"shard"}));
    EXPECT_CALL(mock, askForJob(_, "shard", _)).WillOnce(ReturnJob("workerId", "shard"));
    finder.find(FindJobParams());
}

TEST_F(JobFinderTest, find_withNoShards_triesGetShardsAndFindJob) {
    auto finder = makeFinder();
    InSequence s;
    EXPECT_CALL(mock, getShards(_)).WillOnce(Return(std::vector<ShardId>{}));
    EXPECT_CALL(mock, getShards(_)).WillOnce(Return(std::vector<ShardId>{"shard"}));
    EXPECT_CALL(mock, askForJob(_, "shard", _)).WillOnce(ReturnJob("workerId", "shard"));
    finder.find(FindJobParams());
}

TEST_F(JobFinderTest, find_withExceptionInGetShards_asksForShardsAgain) {
    auto finder = makeFinder();
    InSequence s;
    EXPECT_CALL(mock, getShards(_)).WillOnce(Invoke([&](JobFinderAccessMock::Ctx) {
        throwException();
        return std::vector<std::string>{};
    }));
    EXPECT_CALL(mock, getShards(_)).WillOnce(Return(std::vector<ShardId>{"shard"}));
    EXPECT_CALL(mock, askForJob(_, "shard", _)).WillOnce(ReturnJob("workerId", "shard"));
    Job job;
    job.workerId = "workerId";
    job.shardId = "shard";
    ASSERT_EQ(*(finder.find(FindJobParams())), job);
}

TEST_F(JobFinderTest, find_withExceptionInAskForJob_asksForAJobWithNextShard) {
    auto finder = makeFinder();
    InSequence s;
    EXPECT_CALL(mock, getShards(_)).WillOnce(Return(std::vector<ShardId>{"shard1", "shard2"}));
    EXPECT_CALL(mock, askForJob(_, "shard1", _))
        .WillOnce(Invoke([&](JobFinderAccessMock::Ctx, ShardId, FindJobParams) {
            throwException();
            return boost::optional<Job>{};
        }));
    EXPECT_CALL(mock, askForJob(_, "shard2", _)).WillOnce(ReturnJob("workerId", "shard"));
    Job job;
    job.workerId = "workerId";
    job.shardId = "shard";
    ASSERT_EQ(*(finder.find(FindJobParams())), job);
}

}
