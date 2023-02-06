#include <market/idx/datacamp/routines/tasks/lib/complete_command.h>

#include <library/cpp/testing/unittest/gtest.h>

#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>

namespace {
    class TCompleteCommandTestReducer: public NMarket::NRoutines::ICompleteCommandReducer {
    public:
        using ICompleteCommandReducer::ICompleteCommandReducer;

    protected:
        std::any ParseExternalNode(const NYT::TNode& node) override {
            const TString& data = node["data"].AsString();
            return data ? std::make_any<TString>(data) : std::any{};
        }

        void ProcessData(TWriter* writer,
                         ui64 /* businessId */,
                         ui64 /* shopId */,
                         const TString& offerId,
                         const THashMap<ui32, Market::DataCamp::Offer>& warehouseToDataCampOffer,
                         const TVector<std::any>& externalData) override {
            if (externalData.empty()) {
                if (warehouseToDataCampOffer.empty()) {
                    writer->AddRow(NYT::TNode()("result", "no data"));
                } else {
                    writer->AddRow(NYT::TNode()("result", "no external data"));
                }
            } else {
                if (warehouseToDataCampOffer.empty()) {
                    writer->AddRow(NYT::TNode()("result", "no inner data"));
                } else {
                    if (any_cast<TString>(externalData.front()) != offerId) {
                        writer->AddRow(NYT::TNode()("result", "nonequal external data"));
                    } else {
                        writer->AddRow(NYT::TNode()("result", "data exists"));
                    }
                }
            }
        }
    };


    void CreateTable(NYT::IClientPtr client, const NYT::TYPath& tablePath, const TVector<NYT::TNode>& data) {
        client->Create(tablePath, NYT::NT_TABLE, NYT::TCreateOptions().Recursive(true).IgnoreExisting(true));
        auto writer = client->CreateTableWriter<NYT::TNode>(tablePath);
        for (const auto& node : data) {
            writer->AddRow(node);
        }
        writer->Finish();
        client->Sort(tablePath, tablePath, {"business_id", "offer_id", "shop_id"});
    }

    const TVector<NYT::TNode> DataCampFirstTableData{
        NYT::TNode()("business_id", 1u)("shop_id", 2u)("warehouse_id", 3u)("offer", "")("offer_id", "OnlyDataCamp"),
        NYT::TNode()("business_id", 1u)("shop_id", 2u)("warehouse_id", 3u)("offer", "")("offer_id", "BothWithWrongData"),
    };

    const TVector<NYT::TNode> DataCampSecondTableData{
        NYT::TNode()("business_id", 1u)("shop_id", 2u)("warehouse_id", 3u)("offer", "")("offer_id", "Both"),
    };

    const TVector<NYT::TNode> ExternalTableData{
        NYT::TNode()("business_id", 1u)("shop_id", 2u)("warehouse_id", 3u)("data", "Both")("offer_id", "Both"),
        NYT::TNode()("business_id", 1u)("shop_id", 2u)("warehouse_id", 3u)("data", "BothMissmatch")("offer_id", "BothWithWrongData"),
        NYT::TNode()("business_id", 1u)("shop_id", 2u)("warehouse_id", 3u)("data", "OnlyExternal")("offer_id", "OnlyExternal"),
        NYT::TNode()("business_id", 1u)("shop_id", 2u)("warehouse_id", 3u)("data", "")("offer_id", "EmptyData"),
    };
}
REGISTER_REDUCER(TCompleteCommandTestReducer)

TEST(CompleteCommand, RunCompleteCommand) {
    auto client = NYT::NTesting::CreateTestClient();
    NYT::TYPath firstDataCampTable = "//datacamp/first_table";
    NYT::TYPath secondDataCampTable = "//datacamp/second_table";
    NYT::TYPath externalTable = "//external/table";
    NYT::TYPath outputTable = "//output/table";

    CreateTable(client, firstDataCampTable, DataCampFirstTableData);
    CreateTable(client, secondDataCampTable, DataCampSecondTableData);
    CreateTable(client, externalTable, ExternalTableData);

    RunCompleteCommand(
        client,
        new TCompleteCommandTestReducer(),
        {firstDataCampTable, secondDataCampTable},
        {externalTable},
        outputTable);

    TVector<NYT::TNode> rawResult = NYT::NTesting::ReadTable(client, outputTable);
    EXPECT_EQ(5, rawResult.size());

    THashSet<TString> result;
    for (const NYT::TNode& node : rawResult) {
        result.insert(node["result"].AsString());
    }

    THashSet<TString> expected{
        "no data",
        "no external data",
        "no inner data",
        "nonequal external data",
        "data exists",
    };

    EXPECT_EQ(expected, result);
}
