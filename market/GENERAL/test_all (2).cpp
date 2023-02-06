#include <market/yt2binfile/lib/processors/processors.h>
#include <market/yt2binfile/lib/reader/reader.h>
#include <market/yt2binfile/proto/options.pb.h>

#include <market/library/flat_helpers/flat_helpers.h>
#include <market/library/mmap_versioned/mmap.h>

#include <library/cpp/testing/common/env.h>
#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/yson/node/node.h>

#include <util/stream/file.h>

using namespace NMarket::NYt2Binfile;

struct TYt2BinfileTestSuite: ::testing::Test {
};

TEST_F(TYt2BinfileTestSuite, TestYt2Binfile) {

    ///// WRITE
    TFsPath dstDir = TFsPath(GetOutputPath()) / "yt2binfile_resource";
    TFsPath schemaPath = TFsPath(ArcadiaSourceRoot()) / "market/yt2binfile/ut/flat/schema.fbs";
    dstDir.MkDirs();

    TOptions opts;
    opts.SetResourceName("yt2binfile");
    opts.AddIndexName("index");
    opts.AddKey("key");
    opts.SetDstDir(dstDir);
    opts.SetSchemaPath(schemaPath);
    opts.SetDataPrefix("values");

    TProcRunner runner(opts);
    NYT::TNode node;
    node["key"] = 123;
    node["value_str"] = "value";
    node["value_int"] = 31337;
    node["value_double"] = 1.1;
    runner.ProcessRow(node);

    runner.Finish();
    runner.WriteManifest();

    //// READ
    TYt2BinfileReader reader(dstDir);
    EXPECT_EQ(reader.GetIndexNames(), TVector<TString>{"index"});
    EXPECT_EQ(reader.GetIndex("no such index"), nullptr);

    auto index = reader.GetIndex("index");
    EXPECT_NE(index, nullptr);

    EXPECT_EQ(index->Get("no such key"), Nothing());
    auto offset = index->Get("123");
    EXPECT_EQ(offset, TMaybe<size_t>(0));

    auto mmap = MakeHolder<Market::MMap>(reader.FlatbufFilename().c_str(), PROT_READ, MAP_PRIVATE, 0);
    auto fbData = NMarket::NFlatbufferHelpers::GetTTestValues(*mmap);
    auto value = fbData->values()->Get(*offset);
    EXPECT_EQ(value->key(), 123u);
    EXPECT_STREQ(value->value_str()->c_str(), "value");
    EXPECT_EQ(value->value_int(), 31337u);
    EXPECT_EQ(value->value_double(), 1.1);
}

TEST_F(TYt2BinfileTestSuite, TestCompositeIndex) {

    ///// WRITE
    TFsPath dstDir = TFsPath(GetOutputPath()) / "yt2binfile_resource";
    TFsPath schemaPath = TFsPath(ArcadiaSourceRoot()) / "market/yt2binfile/ut/flat/schema.fbs";
    dstDir.MkDirs();

    TOptions opts;
    opts.SetResourceName("yt2binfile");
    opts.SetDstDir(dstDir);
    opts.SetSchemaPath(schemaPath);
    opts.SetDataPrefix("values");
    opts.AddFormattedIndexName("index_composite");
    opts.AddFormattedKey("{key}_{value_str}"); // fmt format string for key

    TProcRunner runner(opts);
    NYT::TNode node;
    node["key"] = 123;
    node["value_str"] = "value";
    node["value_int"] = 31337;
    node["value_double"] = 1.1;
    runner.ProcessRow(node);

    runner.Finish();
    runner.WriteManifest();

    //// READ
    TYt2BinfileReader reader(dstDir);
    auto index = reader.GetIndex("index_composite");
    EXPECT_NE(index, nullptr);

    auto offset = index->Get("123_value");
    EXPECT_EQ(offset, TMaybe<size_t>(0));

    auto mmap = MakeHolder<Market::MMap>(reader.FlatbufFilename().c_str(), PROT_READ, MAP_PRIVATE, 0);
    auto fbData = NMarket::NFlatbufferHelpers::GetTTestValues(*mmap);
    auto value = fbData->values()->Get(*offset);
    EXPECT_EQ(value->key(), 123u);
    EXPECT_STREQ(value->value_str()->c_str(), "value");
    EXPECT_EQ(value->value_int(), 31337u);
    EXPECT_EQ(value->value_double(), 1.1);
}
