#include <market/idx/generation/genlog_dumper/dumpers/BidsTimestampsDumper.h>

#include <market/library/flat_guards/flatbuffers_guard.h>
#include <market/qpipe/qbid/qbidengine/qbidfile.h>

#include <library/cpp/testing/unittest/gtest.h>

#include <util/folder/path.h>
#include <util/folder/tempdir.h>


using namespace NMarket::NIndexer::NOfferCollection;


class TBidsTimestampsDumperTest : public ::NTesting::TTest {
public:
    static constexpr size_t FRAMES_FOR_SHARD = 11;
    TTempDir Dir;

public:
    explicit TBidsTimestampsDumperTest() = default;
    virtual ~TBidsTimestampsDumperTest() = default;

    TString GetFileName() {
        return Dir.Path() / "bids-timestamps.fb";
    }

protected:
    virtual void SetUp() override;
};

void TBidsTimestampsDumperTest::SetUp() {
    NDumpers::TDumperContext context(Dir.Name(), false);

    auto bidsTsDumper = NDumpers::MakeBidsTimestampsDumper(context, false);

    MarketIndexer::GenerationLog::Record record00;
    {
        record00.mutable_bids_timestamps()->set_bid_ts(0);
        record00.mutable_bids_timestamps()->set_fee_ts(0);
        record00.mutable_bids_timestamps()->set_dont_pull_up_bids_ts(0);
    }

    MarketIndexer::GenerationLog::Record record02;
    {
        record02.mutable_bids_timestamps()->set_bid_ts(2);
        record02.mutable_bids_timestamps()->set_fee_ts(2);
        record02.mutable_bids_timestamps()->set_dont_pull_up_bids_ts(2);

        record02.mutable_before_qbids_calculated()->set_ybid(2);
        record02.mutable_before_qbids_calculated()->set_cbid(3);
        record02.mutable_before_qbids_calculated()->set_mbid(0);
        record02.mutable_before_qbids_calculated()->set_fee(4);
        record02.mutable_before_qbids_calculated()->set_flag_dont_pullup_bids(5);
    }

    MarketIndexer::GenerationLog::Record record04;
    {
        record04.mutable_bids_timestamps()->set_bid_ts(4);
        record04.mutable_bids_timestamps()->set_fee_ts(4);
        record04.mutable_bids_timestamps()->set_dont_pull_up_bids_ts(4);

        record04.set_bid(4);
        record04.set_cbid(5);
        record04.set_fee(6);
        record04.set_dont_pull_up_bids(7);
    }

    MarketIndexer::GenerationLog::Record record11;
    {
        record11.mutable_bids_timestamps()->set_bid_ts(11);
        record11.mutable_bids_timestamps()->set_fee_ts(11);
        record11.mutable_bids_timestamps()->set_dont_pull_up_bids_ts(11);

        record11.mutable_before_qbids_calculated()->set_ybid(11);
        record11.mutable_before_qbids_calculated()->set_cbid(12);
        record11.mutable_before_qbids_calculated()->set_mbid(0);
        record11.mutable_before_qbids_calculated()->set_fee(13);
        record11.mutable_before_qbids_calculated()->set_flag_dont_pullup_bids(14);

        record11.set_bid(15);
        record11.set_cbid(16);
        record11.set_fee(17);
        record11.set_dont_pull_up_bids(18);
    }

    MarketIndexer::GenerationLog::Record record13;
    {
        record13.mutable_bids_timestamps()->set_bid_ts(13);
        record13.mutable_bids_timestamps()->set_fee_ts(13);
        record13.mutable_bids_timestamps()->set_dont_pull_up_bids_ts(13);

        record13.mutable_before_qbids_calculated()->set_ybid(13);
        record13.mutable_before_qbids_calculated()->set_cbid(13);
        record13.mutable_before_qbids_calculated()->set_mbid(0);
        record13.mutable_before_qbids_calculated()->set_fee(13);
        record13.mutable_before_qbids_calculated()->set_flag_dont_pullup_bids(13);

        record13.set_bid(13);
        record13.set_cbid(13);
        record13.set_fee(13);
        record13.set_dont_pull_up_bids(13);
    }

    MarketIndexer::GenerationLog::Record record22;
    {
        record22.mutable_bids_timestamps()->set_bid_ts(22);
        record22.mutable_bids_timestamps()->set_fee_ts(22);
        record22.mutable_bids_timestamps()->set_dont_pull_up_bids_ts(22);

        record22.mutable_before_qbids_calculated()->set_ybid(22);
        record22.mutable_before_qbids_calculated()->set_cbid(22);
        record22.mutable_before_qbids_calculated()->set_mbid(0);
        record22.mutable_before_qbids_calculated()->set_fee(22);
        record22.mutable_before_qbids_calculated()->set_flag_dont_pullup_bids(22);

        record22.set_bid(22);
        record22.set_cbid(22);
        record22.set_fee(22);
        record22.set_dont_pull_up_bids(22);
    }

    bidsTsDumper->ProcessGenlogRecord(record00, 0);
    bidsTsDumper->ProcessGenlogRecord(record02, 2);
    bidsTsDumper->ProcessGenlogRecord(record04, 4);
    bidsTsDumper->ProcessGenlogRecord(record11, 6);
    bidsTsDumper->ProcessGenlogRecord(record13, 8);
    bidsTsDumper->ProcessGenlogRecord(record22, 11);

    bidsTsDumper->Finish();
}

TEST_F(TBidsTimestampsDumperTest, CheckTmpDir) {
    ASSERT_TRUE(TBidsTimestampsDumperTest::Dir.Path().Exists());
}

TEST_F(TBidsTimestampsDumperTest, ExistentDumpedFiles) {
    ASSERT_TRUE(TFsPath(GetFileName()).Exists());
}

TEST_F(TBidsTimestampsDumperTest, CheckSequencedNumberedData) {
    auto Check = [this](size_t sequenceNum, const NQBid::TBidsTimestamps& ts, const NQBid::TTimestampsBidInfo &slowBid, const NQBid::TTimestampsBidInfo &fastBid) {
        NQBid::TBidsTimestampsFlatbufferFile file(GetFileName());
        ASSERT_TRUE(file);
        ASSERT_TRUE(file.HasOffset(sequenceNum));
        const auto data = file.GetTimestamps(sequenceNum);
        ASSERT_NE(data, nullptr);
        ASSERT_EQ(ts.Bid, data->Bid);
        ASSERT_EQ(ts.Fee, data->Fee);
        ASSERT_EQ(ts.DontPullUpBids, data->DontPullUpBids);
        ASSERT_EQ(slowBid.Bid, data->SlowBid.Bid);
        ASSERT_EQ(slowBid.Fee, data->SlowBid.Fee);
        ASSERT_EQ(fastBid.Bid, data->FastBid.Bid);
        ASSERT_EQ(fastBid.Fee, data->FastBid.Fee);
    };

    Check(0, NQBid::TBidsTimestamps{.Bid =  0, .Fee =  0, .DontPullUpBids =  0}, NQBid::TTimestampsBidInfo{ .Bid = 0, .Fee = 0 }, NQBid::TTimestampsBidInfo{ .Bid = 0, .Fee = 0 } );
    Check(2, NQBid::TBidsTimestamps{.Bid =  2, .Fee =  2, .DontPullUpBids =  2}, NQBid::TTimestampsBidInfo{ .Bid = 3, .Fee = 4 }, NQBid::TTimestampsBidInfo{ .Bid = 0, .Fee = 0 });
    Check(4, NQBid::TBidsTimestamps{.Bid =  4, .Fee =  4, .DontPullUpBids =  4}, NQBid::TTimestampsBidInfo{ .Bid = 0, .Fee = 0 }, NQBid::TTimestampsBidInfo{ .Bid = 4, .Fee = 6 });
    Check(6, NQBid::TBidsTimestamps{.Bid = 11, .Fee = 11, .DontPullUpBids = 11}, NQBid::TTimestampsBidInfo{ .Bid = 12, .Fee = 13 }, NQBid::TTimestampsBidInfo{ .Bid = 15, .Fee = 17 });
    Check(7, NQBid::TBidsTimestamps{.Bid =  0, .Fee =  0, .DontPullUpBids =  0}, NQBid::TTimestampsBidInfo{ .Bid = 0, .Fee = 0 }, NQBid::TTimestampsBidInfo{ .Bid = 0, .Fee = 0 });
    Check(8, NQBid::TBidsTimestamps{.Bid = 13, .Fee = 13, .DontPullUpBids = 13}, NQBid::TTimestampsBidInfo{ .Bid = 13, .Fee = 13 }, NQBid::TTimestampsBidInfo{ .Bid = 13, .Fee = 13 });
    Check(11, NQBid::TBidsTimestamps{.Bid = 22, .Fee = 22, .DontPullUpBids = 22}, NQBid::TTimestampsBidInfo{ .Bid = 22, .Fee = 22 }, NQBid::TTimestampsBidInfo{ .Bid = 22, .Fee = 22 });
}

TEST_F(TBidsTimestampsDumperTest, ShardsHits) {
    NQBid::TBidsTimestampsFlatbufferFile file(GetFileName());
    ASSERT_TRUE(file);
    ASSERT_EQ(file.GetTimestamps(FRAMES_FOR_SHARD + 1), nullptr);
}
