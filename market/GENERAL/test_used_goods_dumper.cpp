#include <market/idx/generation/genlog_dumper/dumpers/used_goods_dumper.h>
#include <market/library/used_goods/src/reader.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <util/folder/tempdir.h>
#include <util/generic/maybe.h>
#include <util/stream/file.h>

using namespace MarketIndexer::GenerationLog;
using namespace NMarket::NUsedGoods;

struct TTestData {
    uint64_t MarketSku = 0;
    TMaybe<EVisualCondition> Condition;
};

TVector<Record> ToGenlogRecords(const TVector<TTestData>& notes) {
    TVector<Record> result;
    for (size_t i = 0; i < notes.size(); ++i) {
        Record record;
        record.set_market_sku(notes[i].MarketSku);
        if (notes[i].Condition) {
            uint64_t paramValue = 0;
            switch (*notes[i].Condition) {
            case EVisualCondition::AsNew:
                paramValue = 29641910;
                break;
            case EVisualCondition::Excellent:
                paramValue = 29641911;
                break;
            case EVisualCondition::Good:
                paramValue = 29641912;
            case EVisualCondition::None:
                break;
            }
            if (paramValue) {
                // несколько ненужных параметров
                record.mutable_mbo_params()->Add()->set_id(12345);
                record.mutable_mbo_params()->Add()->set_id(54321);
                auto* visualConditionParam = record.mutable_mbo_params()->Add();
                visualConditionParam->set_id(29641890);
                visualConditionParam->mutable_values()->Add(paramValue);
            }
        }
        result.push_back(record);
    }
    return result;
}

void GenUsedGoodsSkusFile(const TVector<TString>& lines, const TString& filepath) {
    TFileOutput out(filepath);
    for (const auto& line: lines) {
        out << line << Endl;
    }
    out << "100500 as-new" << Endl;
    out << "100501 excellent" << Endl;
    out << "100502 good" << Endl;
    out << "100503 abc" << Endl;
    out.Finish();
}

TString RunDumper(const TTempDir& dir, const TString& usedGoodsSkusFile, const TVector<TTestData>& notes) {
    NDumpers::TDumperContext context(dir.Name(), false);
    auto dumper = NDumpers::MakeUsedGoodsDumper(context, usedGoodsSkusFile);

    const auto records = ToGenlogRecords(notes);
    for (size_t i = 0; i < records.size(); ++i) {
        dumper->ProcessGenlogRecord(records[i], i);
    }
    dumper->Finish();
    return dir.Path() / "used-goods.fb";
}

TEST(TestUsedGoodsDumper, TestBase) {
    TTempDir dir;
    const TString usedGoodsSkusPath = dir.Path() / "used_goods_skus.txt";
    GenUsedGoodsSkusFile(
        {"1000000", "невалидное значение", "3000000", "5000000"},
        usedGoodsSkusPath
    );
    const TVector<TTestData> notes = {
        {.Condition = EVisualCondition::AsNew}, // оффер без скю
        {.MarketSku = 123}, // не б/у скю (нет и в файле used_goods_skus.txt)
        {.MarketSku = 456, .Condition = EVisualCondition::None}, // еще одно не б/у скю
        // один оффер из скю "как новый"
        {.MarketSku = 1000000, .Condition = EVisualCondition::AsNew},
        // два оффера из скю "отличный"
        {.MarketSku = 3000000, .Condition = EVisualCondition::Excellent},
        {.MarketSku = 3000000, .Condition = EVisualCondition::Excellent},
        // один оффер из скю "хороший"
        {.MarketSku = 5000000, .Condition = EVisualCondition::Good},
        {.MarketSku = 100500}, // есть в файле used_goods_skus.txt
        {.MarketSku = 100501},
        {.MarketSku = 100502},
        {.MarketSku = 100503}, // есть в файле used_goods_skus.txt, но с невалидным значением
    };
    NMarket::NUsedGoods::TFbReader reader(RunDumper(dir, usedGoodsSkusPath, notes));
    ASSERT_EQ(reader.DocsCount(), 7);
    ASSERT_TRUE(reader.Get(0) == EVisualCondition::None);
    ASSERT_TRUE(reader.Get(1) == EVisualCondition::None);
    ASSERT_TRUE(reader.Get(2) == EVisualCondition::None);
    ASSERT_TRUE(reader.Get(3) == EVisualCondition::AsNew);
    ASSERT_TRUE(reader.Get(4) == EVisualCondition::Excellent);
    ASSERT_TRUE(reader.Get(5) == EVisualCondition::Excellent);
    ASSERT_TRUE(reader.Get(6) == EVisualCondition::Good);
    ASSERT_TRUE(reader.Get(7) == EVisualCondition::AsNew);
    ASSERT_TRUE(reader.Get(8) == EVisualCondition::Excellent);
    ASSERT_TRUE(reader.Get(9) == EVisualCondition::Good);
    ASSERT_TRUE(reader.Get(100500) == EVisualCondition::None);
}

TEST(TestUsedGoodsDumper, TestEmptyUsedGoodsSkus) {
    TTempDir dir;
    const TString usedGoodsSkusPath = dir.Path() / "used_goods_skus.txt";
    GenUsedGoodsSkusFile({}, usedGoodsSkusPath);
    const TVector<TTestData> notes = {
        {.MarketSku = 1000000, .Condition = EVisualCondition::AsNew},
    };
    NMarket::NUsedGoods::TFbReader reader(RunDumper(dir, usedGoodsSkusPath, notes));
    ASSERT_EQ(reader.DocsCount(), 0);
}

TEST(TestUsedGoodsDumper, TestBadUsedGoodsSkus) {
    TTempDir dir;
    const TVector<TTestData> notes = {
        {.MarketSku = 1000000, .Condition = EVisualCondition::AsNew},
    };
    NMarket::NUsedGoods::TFbReader reader(RunDumper(dir, "abc.txt", notes));
    ASSERT_EQ(reader.DocsCount(), 0);
}
