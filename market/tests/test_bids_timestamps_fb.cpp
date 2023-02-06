#include <market/qpipe/qbid/qbidengine/qbidfile.h>
#include <market/qpipe/qbid/qbidengine/qbid.h>

#include <market/library/flat_guards/flatbuffers_guard.h>

#include "util.h"
#include "legacy_for_test.h"
#include <util/string/vector.h>
#include <library/cpp/testing/unittest/gtest.h>

using namespace NMarket::NIndexer::NOfferCollection;

void RemoveFileIfExists(const TString& filename) {
    if (NFs::Exists(filename)) {
        NFs::Remove(filename);
    }
}

class TSimpleBidsTimestampsWriter final {
public:
    explicit TSimpleBidsTimestampsWriter(const TString& output)
        : Output(MakeHolder<TFileOutput>(output))
    {}

    ~TSimpleBidsTimestampsWriter() {
        Dump();
    }

    void Add(const NQBid::TBidsTimestamps& times) {
        Vec.emplace_back(CreateTBidsTimestamps(Builder, times.Bid, times.Fee, times.DontPullUpBids));
    }

private:
    void Dump() {
        Builder.Finish(CreateTBidsTimestampsVec(Builder, Builder.CreateVector(Vec)), TBidsTimestampsVecIdentifier());
        Output->Write(Builder.GetBufferPointer(), Builder.GetSize());
    }

private:
    THolder<TFileOutput> Output;
    flatbuffers::FlatBufferBuilder Builder;
    TVector<flatbuffers::Offset<NMarket::NIndexer::NOfferCollection::TBidsTimestamps>> Vec;
};


class TBidsTimestampsFileTest : public ::testing::Test {
public:
    static constexpr char TEST_FILE_NAME[] = "bids-timestamps.fb";

    TBidsTimestampsFileTest() = default;
    virtual ~TBidsTimestampsFileTest() {};

protected:
    void DoSetUp() {
        TSimpleBidsTimestampsWriter writer(TEST_FILE_NAME);
        writer.Add(NQBid::TBidsTimestamps{1,1,1}); // 0
        writer.Add(NQBid::TBidsTimestamps{2,2,2}); // 1
        writer.Add(NQBid::TBidsTimestamps{3,3,3}); // 2
        writer.Add(NQBid::TBidsTimestamps{4,4,4}); // 3
        writer.Add(NQBid::TBidsTimestamps{5,5,5}); // 4
    }

    virtual void SetUp() override {
        DoSetUp();
    }

    virtual void TearDown() override {
        RemoveFileIfExists(TEST_FILE_NAME);
    }
};

TEST_F(TBidsTimestampsFileTest, NonExistentFile) {
    NQBid::TBidsTimestampsFlatbufferFile tsFile("non-existent.fb");
    ASSERT_FALSE(tsFile);
    ASSERT_FALSE(tsFile.IsValid());
    ASSERT_FALSE(tsFile.HasOffset(0));
    ASSERT_EQ(tsFile.GetTimestamps(0), nullptr);
}

TEST_F(TBidsTimestampsFileTest, ReadOffsets) {
    NQBid::TBidsTimestampsFlatbufferFile tsFile(TBidsTimestampsFileTest::TEST_FILE_NAME);
    ASSERT_TRUE(tsFile);
    ASSERT_TRUE(tsFile.HasOffset(4));
    auto ts = tsFile.GetTimestamps(0);
    ASSERT_EQ(ts->Bid, 1);
    ASSERT_EQ(ts->Fee, 1);
    ASSERT_EQ(ts->DontPullUpBids, 1);
}
