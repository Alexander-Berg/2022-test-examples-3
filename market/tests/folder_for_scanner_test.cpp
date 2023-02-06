#include <market/idx/datacamp/routines/tasks/complete_commands/folder_for_scanner.h>
#include <market/idx/datacamp/routines/tasks/complete_commands/schema.pb.h>
#include <market/idx/datacamp/routines/tasks/complete_commands/output_tables_merger.h>

#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/protobuf/interop/cast.h>
#include <util/generic/algorithm.h>

using namespace NDataCamp::NCompleteCommands;

namespace {
void CreateTable(NYT::IClientPtr client, const NYT::TYPath& tablePath, const TVector<TGroupedOffers>& rows = {}) {
    client->Create(tablePath, NYT::NT_TABLE, NYT::TCreateOptions().Recursive(true).IgnoreExisting(true));
    auto writer = client->CreateTableWriter<TGroupedOffers>(tablePath);
    for (const auto& row : rows) {
        writer->AddRow(row);
    }
    writer->Finish();
}

static const TInstant NEW_TS = TInstant::Now();
static const TInstant OLD_TS = NEW_TS - TDuration::Minutes(5);

void Ensure(NYT::IClientPtr client, const TScannerDirContent& expected) {
    auto actual = ListDirrectoryTables(client, "//home/out_dir");
    EXPECT_EQ(expected.size(), actual.size());
    for (std::size_t i = 0; i < actual.size(); ++i) {
        if (expected[i].Name) {
            EXPECT_EQ(expected[i].Name, actual[i].Name);
        }
        EXPECT_EQ(expected[i].Position, actual[i].Position);
        EXPECT_EQ(expected[i].ScannedTimestamp, actual[i].ScannedTimestamp);
        EXPECT_EQ(expected[i].MinOfferTimestamp, actual[i].MinOfferTimestamp);
        EXPECT_EQ(expected[i].MaxOfferTimestamp, actual[i].MaxOfferTimestamp);
        EXPECT_NE(actual[i].CreationTime, TInstant{});
    }
}
void SetScannerProcessed(NYT::IClientPtr client, const TString& tablePath, const TString& ts) {
    client->Set(tablePath + "/@scanner_processed_ts", ts);
}

}

TEST(FolderForScanner, TestGetContent) {
    auto client = NYT::NTesting::CreateTestClient();
    const TString TABLE_1 = "//home/out_dir/table_1";
    const TString TABLE_2 = "//home/out_dir/table_2";
    CreateTable(client, TABLE_1);
    CreateTable(client, TABLE_2);
    Ensure(client, {TScannerTableInfo{.Name=TABLE_1}, TScannerTableInfo{.Name=TABLE_2}});

    SetPositionAttributes(client, TABLE_1, "pos1", OLD_TS, NEW_TS);
    SetPositionAttributes(client, TABLE_2, "pos2", OLD_TS, NEW_TS);
    Ensure(client, {
        TScannerTableInfo{.Name=TABLE_1, .Position="pos1", .MinOfferTimestamp=OLD_TS, .MaxOfferTimestamp=NEW_TS},
        TScannerTableInfo{.Name=TABLE_2, .Position="pos2", .MinOfferTimestamp=OLD_TS, .MaxOfferTimestamp=NEW_TS}
    });

    const auto scanned_ts_1 = TInstant::Now();
    const auto scanned_ts_2 = scanned_ts_1 - TDuration::Minutes(10);
    SetScannerProcessed(client, TABLE_1, scanned_ts_1.ToString());
    SetScannerProcessed(client, TABLE_2, scanned_ts_2.ToString());
    Ensure(client, {
        TScannerTableInfo{.Name=TABLE_1, .Position="pos1", .ScannedTimestamp=scanned_ts_1, .MinOfferTimestamp=OLD_TS, .MaxOfferTimestamp=NEW_TS},
        TScannerTableInfo{.Name=TABLE_2, .Position="pos2", .ScannedTimestamp=scanned_ts_2, .MinOfferTimestamp=OLD_TS, .MaxOfferTimestamp=NEW_TS}
    });

    RemoveStaledTables(client, {TABLE_1, "bad_table", TABLE_2});
    Ensure(client, {});
}

TEST(FolderForScanner, TestGetSummary) {
    TInstant base = TInstant::Now() - TDuration::Days(10);
    TScannerDirContent content = {
        TScannerTableInfo{.Name="table0", .Position="pos0", .ScannedTimestamp=TInstant::Seconds(100), .CreationTime=base-TDuration::Days(1)},
        TScannerTableInfo{.Name="table1", .Position="pos1", .ScannedTimestamp=TInstant::Seconds(200), .CreationTime=base+TDuration::Days(1)},
        TScannerTableInfo{.Name="table2", .Position="", .ScannedTimestamp=TInstant::Seconds(300), .CreationTime=base+TDuration::Days(3)},
        TScannerTableInfo{.Name="table3", .Position="pos3", .ScannedTimestamp=TInstant::Zero(), .CreationTime=base+TDuration::Days(4)},
        TScannerTableInfo{.Name="table4", .Position="pos4", .ScannedTimestamp=TInstant::Seconds(400), .CreationTime=base+TDuration::Days(5)},
        TScannerTableInfo{.Name="table5", .Position="", .ScannedTimestamp=TInstant::Seconds(500), .CreationTime=base+TDuration::Days(6)},
    };
    auto summary = GetSummary(content, 10);
    EXPECT_EQ(summary.ProcessedPosition, "pos4");
    EXPECT_EQ(summary.ScannedPosition, "pos1");
    EXPECT_EQ(summary.ToRemove.size(), 1);
    EXPECT_EQ(summary.ToRemove.front(), "table0");
}


TGroupedOffers MakeRow(ui32 bussiness, ui32 shop,  const TString& offerId, TInstant ts) {
    TGroupedOffers row;
    row.set_business_id(bussiness);
    row.set_shop_sku(offerId);
    auto& service = (*row.mutable_offer()->mutable_service())[shop];
    *service.mutable_status()->add_disabled()->mutable_meta()->mutable_timestamp() = NProtoInterop::CastToProto(ts);
    return row;
}

static TVector<TGroupedOffers> ReadTable(NYT::IClientPtr client, const TString& tablePath) {
    TVector<TGroupedOffers> result;
    auto reader = client->CreateTableReader<TGroupedOffers>(tablePath);
    for (; reader->IsValid(); reader->Next()) {
        result.push_back(reader->GetRow());
    }
    return result;
}

TEST(FolderForScanner, MergeTables) {
    auto client = NYT::NTesting::CreateTestClient();
    const TString TABLE_0 = "//home/out_dir/table_0";
    const TString TABLE_1 = "//home/out_dir/table_1";
    const TString TABLE_2 = "//home/out_dir/table_2";
    const TString TABLE_3 = "//home/out_dir/table_3";

    static const TInstant NOW_TS = TInstant::Now();
    CreateTable(client, TABLE_0, {});
    CreateTable(client, TABLE_1, { MakeRow(1, 2, "offer1", TInstant::Seconds(1))});
    CreateTable(client, TABLE_2, { MakeRow(1, 2, "offer1", TInstant::Seconds(10))});
    CreateTable(client, TABLE_3, { MakeRow(1, 2, "offer1", TInstant::Seconds(3)), MakeRow(1, 3, "offer1", TInstant::Seconds(3))});
    SetPositionAttributes(client, TABLE_0, "pos0", NOW_TS - TDuration::Minutes(100), NOW_TS - TDuration::Minutes(90));
    SetPositionAttributes(client, TABLE_1, "pos1", NOW_TS - TDuration::Minutes(40), NOW_TS - TDuration::Minutes(30));
    SetPositionAttributes(client, TABLE_2, "pos2", NOW_TS - TDuration::Minutes(20), NOW_TS - TDuration::Minutes(10));
    SetPositionAttributes(client, TABLE_3, "pos3", NOW_TS - TDuration::Minutes(5), NOW_TS);
    ui32 mergedCount = NDataCamp::NCompleteCommands::MergeOutputTables(client, "//home/out_dir", "//home/tmp_table", 0);
    EXPECT_EQ(mergedCount, 3);
    Ensure(client, {
        TScannerTableInfo{.Name=TABLE_0, .Position="pos0", .MinOfferTimestamp=NOW_TS - TDuration::Minutes(100), .MaxOfferTimestamp=NOW_TS - TDuration::Minutes(90)},
        TScannerTableInfo{.Position="pos3", .MinOfferTimestamp=NOW_TS - TDuration::Minutes(40), .MaxOfferTimestamp=NOW_TS},
    });
    auto actual = ListDirrectoryTables(client, "//home/out_dir");
    auto records = ReadTable(client, actual.back().Name);

    EXPECT_EQ(records.size(), 1);
    const auto& row = records.front();
    EXPECT_EQ(row.business_id(), 1);
    EXPECT_EQ(row.shop_sku(), "offer1");

    const auto& united = row.offer();
    EXPECT_TRUE(IsIn(united.service(), 2));
    EXPECT_TRUE(IsIn(united.service(), 3));
    EXPECT_EQ(united.service().at(2).status().disabled().at(0).meta().timestamp().seconds(), 10);
}
