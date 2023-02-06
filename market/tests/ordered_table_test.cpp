#include <market/idx/datacamp/routines/tasks/complete_commands/ordered_table_reader.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/iterator/concatenate.h>

#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>
#include <mapreduce/yt/util/wait_for_tablets_state.h>

using namespace NMarket::NDataCamp::NOrderedTableUtils;

namespace {
    void CreateTable(NYT::IClientPtr client, const NYT::TYPath& tablePath) {
        auto schema = NYT::TTableSchema()
            .AddColumn("business_id", NYT::EValueType::VT_UINT64)
            .AddColumn("offer_id", NYT::EValueType::VT_STRING);
        client->Create(tablePath, NYT::NT_TABLE, NYT::TCreateOptions().Force(true).Attributes(
            NYT::TNode()
                ("dynamic", true)
                ("schema", schema.ToNode())));
    }

    using TMockRows = TVector<std::pair<ui64, TString>>;
    TVector<NYT::TNode> ToNodeRows(const TMockRows& input) {
        TVector<NYT::TNode> result;
        for (auto r : input) {
            result.push_back(NYT::TNode()("business_id", r.first)("offer_id", r.second));
        }
        return result;
    }
    // Flash rows from dynstore to disk chunks
    void FlushTable(NYT::IClientPtr client, const NYT::TYPath& tablePath) {
        client->FreezeTable(tablePath);
        NYT::WaitForTabletsState(client, tablePath, NYT::TS_FROZEN);
        client->UnfreezeTable(tablePath);
        NYT::WaitForTabletsState(client, tablePath, NYT::TS_MOUNTED);
    }

    void InsertRows(NYT::IClientPtr client, const NYT::TYPath& tablePath, const TVector<NYT::TNode>& rows) {
        client->InsertRows(tablePath, rows);
        FlushTable(client, tablePath);
    }

    void EnsureTableRows(NYT::IClientPtr client, const NYT::TYPath& tablePath, const auto& expected) {
        TVector<NYT::TNode> rawResult = NYT::NTesting::ReadTable(client, tablePath);
        THashSet<std::pair<ui64, TString>> index;
        for (const NYT::TNode& node : rawResult) {
            index.insert({ node["business_id"].AsUint64(), node["offer_id"].AsString() });
        }
        EXPECT_EQ(std::distance(expected.begin(), expected.end()), index.size());
        for (auto e : expected) {
            EXPECT_TRUE(IsIn(index, e));
        }
    }

} // anonymous namespace

TEST(OrderedTableUtils, TestTableOffsetRange) {
    TTableOffset lower{.Tablets = {{.TabletId = 0, .RowId = 1}, {.TabletId = 1, .RowId = 21} }};
    TTableOffset upper{.Tablets = {{.TabletId = 0, .RowId = 10}, {.TabletId = 1, .RowId = 21}, {.TabletId = 2, .RowId = 46} }};

    auto r = MakeOffsetRange(lower, upper);
    EXPECT_EQ(r.Tablets.size(), 2);
    EXPECT_EQ(r.Tablets[0].TabletId, 0);
    EXPECT_EQ(r.Tablets[0].LowerRowId, 1);
    EXPECT_EQ(r.Tablets[0].UpperRowId, 10);

    EXPECT_EQ(r.Tablets[1].TabletId, 2);
    EXPECT_EQ(r.Tablets[1].LowerRowId, 0);
    EXPECT_EQ(r.Tablets[1].UpperRowId, 46);
}

TEST(OrderedTableUtils, TestTableOffsetSerialize) {
    auto empty = TTableOffset::FromString("");
    EXPECT_EQ(empty.Tablets.size(), 0);
    const TTableOffset input{.Tablets = {{.TabletId = 0, .RowId = 10}, {.TabletId = 1, .RowId = 21}, {.TabletId = 2, .RowId = 46} }};
    auto output = TTableOffset::FromString(input.Serialize());
    EXPECT_EQ(output.Tablets.size(), 3);
    EXPECT_EQ(output.Tablets[0].RowId, 10);
    EXPECT_EQ(output.Tablets[2].RowId, 46);
    EXPECT_EQ(output.Tablets[2].TabletId, 2);
}

TEST(OrderedTableUtils, TestMaxOffset) {
    TTableOffset lower{.Tablets = {{.TabletId = 0, .RowId = 11}, {.TabletId = 1, .RowId = 21} }};
    TTableOffset upper{.Tablets = {{.TabletId = 0, .RowId = 2}, {.TabletId = 1, .RowId = 21}, {.TabletId = 2, .RowId = 46} }};

    auto r = GetMaxOffset(upper, lower);
    EXPECT_EQ(r.Tablets[0].RowId, 11);
    EXPECT_EQ(r.Tablets[0].TabletId, 0);

    EXPECT_EQ(r.Tablets[1].RowId, 21);
    EXPECT_EQ(r.Tablets[1].TabletId, 1);

    EXPECT_EQ(r.Tablets[2].RowId, 46);
    EXPECT_EQ(r.Tablets[2].TabletId, 2);
}


TEST(OrderedTableUtils, TestTableOffsetPosition) {
    auto client = NYT::NTesting::CreateTestClient();
    TString tablePath = "//home/ordered_table";
    CreateTable(client, tablePath);
    TOrderedTable table(client, tablePath);

    table.Reshard(10);

    auto lower = table.RequestUpperOffset();
    EXPECT_EQ(lower.Tablets.size(), 10);
    EXPECT_EQ(lower.Tablets[0].TabletId, 0);
    EXPECT_EQ(lower.Tablets[0].RowId, 0);

    // Write junk data
    TMockRows firstPart = { {1, "offer1"}, {1, "offer2"}, {3, "offer3"} };
    InsertRows(client, tablePath, ToNodeRows(firstPart));
    auto upper = table.RequestUpperOffset();

    // Export data
    TString exportTablePath = "//home/static_export";
    table.ExportRangeToStaticTable(MakeOffsetRange(lower, upper), exportTablePath);
    EnsureTableRows(client, exportTablePath, firstPart);

    // Write data new part
    TMockRows secondPart = { {1, "offer3"}, {1, "offer4"}, {2, "offer5"}, {3, "offer53"} };
    InsertRows(client, tablePath, ToNodeRows(secondPart));

    // Export second part from last position
    lower = upper;
    upper = table.RequestUpperOffset();
    table.ExportRangeToStaticTable(MakeOffsetRange(lower, upper), exportTablePath);
    EnsureTableRows(client, exportTablePath, secondPart);

    // Export full table
    table.ExportRangeToStaticTable(MakeOffsetRange({}, upper), exportTablePath);
    EnsureTableRows(client, exportTablePath, Concatenate(firstPart, secondPart));
}

// TODO: test schema change and export to sorted table.
