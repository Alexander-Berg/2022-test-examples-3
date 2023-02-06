#include <market/idx/generation/genlog_dumper/dumpers/BidsTimestampsDumper.h>

#include <market/library/flat_guards/flatbuffers_guard.h>
#include <market/qpipe/qbid/qbidengine/qbidfile.h>

#include <library/cpp/testing/unittest/gtest.h>

#include <util/folder/path.h>
#include <util/folder/tempdir.h>


using namespace NMarket::NIndexer::NOfferCollection;


class TSaveFeeAsBidTest : public ::NTesting::TTest {
public:
    static constexpr size_t FRAMES_FOR_SHARD = 7;
    TTempDir Dir;

public:
    explicit TSaveFeeAsBidTest() = default;
    virtual ~TSaveFeeAsBidTest() = default;

    TString GetFileName() {
        return Dir.Path() / "bids-timestamps.fb";
    }

protected:
    virtual void SetUp() override;
};


/// Тест проверяет работу флага save_fee_as_bid
/// По умолчанию он выключен, но если он вклчен, то
/// Вместо Bid, должен сохраняться Fee.
/// @see https://st.yandex-team.ru/MARKETOUT-35046
void TSaveFeeAsBidTest::SetUp() {
    NDumpers::TDumperContext context(
        Dir.Name(),
        false
    );

    auto bidsTsDumper = NDumpers::MakeBidsTimestampsDumper(context, true);

    MarketIndexer::GenerationLog::Record recordBlue;
    {
        recordBlue.set_is_blue_offer(true);

        recordBlue.mutable_bids_timestamps()->set_bid_ts(0);
        recordBlue.mutable_bids_timestamps()->set_fee_ts(0);
        recordBlue.mutable_bids_timestamps()->set_dont_pull_up_bids_ts(0);

        recordBlue.mutable_before_qbids_calculated()->set_ybid(0);
        recordBlue.mutable_before_qbids_calculated()->set_cbid(1);
        recordBlue.mutable_before_qbids_calculated()->set_mbid(0);
        recordBlue.mutable_before_qbids_calculated()->set_fee(2);
        recordBlue.mutable_before_qbids_calculated()->set_flag_dont_pullup_bids(0);

        recordBlue.set_bid(3);
        recordBlue.set_cbid(0);
        recordBlue.set_fee(4);
        recordBlue.set_dont_pull_up_bids(0);
    }

    MarketIndexer::GenerationLog::Record recordDsbs;
    {
        recordDsbs.set_cpa(4);

        recordDsbs.mutable_bids_timestamps()->set_bid_ts(0);
        recordDsbs.mutable_bids_timestamps()->set_fee_ts(0);
        recordDsbs.mutable_bids_timestamps()->set_dont_pull_up_bids_ts(0);

        recordDsbs.mutable_before_qbids_calculated()->set_ybid(0);
        recordDsbs.mutable_before_qbids_calculated()->set_cbid(4);
        recordDsbs.mutable_before_qbids_calculated()->set_mbid(0);
        recordDsbs.mutable_before_qbids_calculated()->set_fee(3);
        recordDsbs.mutable_before_qbids_calculated()->set_flag_dont_pullup_bids(0);

        recordDsbs.set_bid(2);
        recordDsbs.set_cbid(0);
        recordDsbs.set_fee(1);
        recordDsbs.set_dont_pull_up_bids(0);
    }

    MarketIndexer::GenerationLog::Record recordWhite;
    {
        recordWhite.mutable_bids_timestamps()->set_bid_ts(2);
        recordWhite.mutable_bids_timestamps()->set_fee_ts(2);
        recordWhite.mutable_bids_timestamps()->set_dont_pull_up_bids_ts(2);

        recordWhite.mutable_before_qbids_calculated()->set_ybid(0);
        recordWhite.mutable_before_qbids_calculated()->set_cbid(5);
        recordWhite.mutable_before_qbids_calculated()->set_mbid(0);
        recordWhite.mutable_before_qbids_calculated()->set_fee(6);
        recordWhite.mutable_before_qbids_calculated()->set_flag_dont_pullup_bids(0);

        recordWhite.set_bid(7);
        recordWhite.set_cbid(0);
        recordWhite.set_fee(8);
        recordWhite.set_dont_pull_up_bids(0);
    }

    bidsTsDumper->ProcessGenlogRecord(recordBlue, 1);
    bidsTsDumper->ProcessGenlogRecord(recordDsbs, 3);
    bidsTsDumper->ProcessGenlogRecord(recordWhite, 5);

    bidsTsDumper->Finish();
}

TEST_F(TSaveFeeAsBidTest, CheckData) {
    auto Check = [this](size_t sequenceNum, const NQBid::TBidsTimestamps& ts, const NQBid::TTimestampsBidInfo &slowBid, const NQBid::TTimestampsBidInfo &fastBid) {
        NQBid::TBidsTimestampsFlatbufferFile file(this->GetFileName());
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

    Check(1, NQBid::TBidsTimestamps{.Bid =  0, .Fee =  0, .DontPullUpBids =  0}, NQBid::TTimestampsBidInfo{ .Bid = 2, .Fee = 2 }, NQBid::TTimestampsBidInfo{ .Bid = 4, .Fee = 4 } );
    Check(3, NQBid::TBidsTimestamps{.Bid =  0, .Fee =  0, .DontPullUpBids =  0}, NQBid::TTimestampsBidInfo{ .Bid = 3, .Fee = 3 }, NQBid::TTimestampsBidInfo{ .Bid = 1, .Fee = 1 } );
    Check(5, NQBid::TBidsTimestamps{.Bid =  2, .Fee =  2, .DontPullUpBids =  2}, NQBid::TTimestampsBidInfo{ .Bid = 5, .Fee = 6 }, NQBid::TTimestampsBidInfo{ .Bid = 7, .Fee = 8 } );
}
