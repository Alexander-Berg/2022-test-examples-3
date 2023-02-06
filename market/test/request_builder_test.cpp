#include <kernel/qtree/richrequest/richnode.h>

#include <market/report/library/request_builder/request_builder.h>
#include <market/report/library/search_literal_filters/search_literal_filters.h>

#include <library/cpp/testing/unittest/gtest.h>

using namespace NMarketReport;

namespace {
    TRichTreePtr CreateTestTree(const TString& query) {
        TCreateTreeOptions opts(LI_DEFAULT_REQUEST_LANGUAGES);
        TRichTreePtr tree = CreateRichTree(UTF8ToWide(query), opts);
        return tree;
    }
}

TEST(RequestBuilder, TSearchLiteralFilters) {
    {
        TSearchLiteralFilters filters;
        EXPECT_TRUE(filters.IsEmpty());
    }
    {
        TRequestBuilder builder(CreateTestTree("собачка"));
        TSearchLiteralFilters filters;
        filters.AddFilter("ware_md5", "93485934859", true);
        builder.AppendSearchLiteralFilters(filters);

        EXPECT_FALSE(filters.IsEmpty());
        EXPECT_EQ("собачка ware_md5:\"93485934859\"", builder.GetText());
    }
    {
        TRequestBuilder builder;
        TSearchLiteralFilters filters;
        filters.AddFilter("ware_md5", "93485934859", true);
        builder.AppendSearchLiteralFilters(filters);

        EXPECT_FALSE(filters.IsEmpty());
        EXPECT_EQ("ware_md5:\"93485934859\"", builder.GetText());
    }
    {
        TRequestBuilder builder;
        TSearchLiteralFilters filters;
        filters.AddFilter("ware_md5", "93485934859", false);
        builder.AppendSearchLiteralFilters(filters);

        EXPECT_TRUE(filters.IsEmpty());
        EXPECT_EQ(true, builder.IsEmpty());
    }
    {
        TRequestBuilder builder;
        TSearchLiteralFilters filters;
        filters.AddFilter("ware_md5", "93485934859", false);
        filters.AddFilter("ware_md5", "13", true);
        builder.AppendSearchLiteralFilters(filters);
        EXPECT_FALSE(filters.IsEmpty());
        EXPECT_EQ("ware_md5:\"13\" ~~ ware_md5:\"93485934859\"", builder.GetText());
    }
    {
        TRequestBuilder builder;
        TSearchLiteralFilters filters;
        filters.AddFilter("ware_md5", "11", true);
        filters.AddFilter("ware_md5", "12", true);
        filters.AddFilter("ware_md5", "21", false);
        filters.AddFilter("ware_md5", "22", false);
        builder.AppendSearchLiteralFilters(filters);

        EXPECT_FALSE(filters.IsEmpty());
        EXPECT_EQ("(ware_md5:\"11\" | ware_md5:\"12\") ~~ (ware_md5:\"21\" | ware_md5:\"22\")", builder.GetText());

        TRequestBuilder builder2(CreateTestTree("super"));
        builder2.AppendSearchLiteralFilters(filters);
        EXPECT_EQ("super ~~ (ware_md5:\"21\" | ware_md5:\"22\") << (ware_md5:\"11\" | ware_md5:\"12\")", builder2.GetText());
    }
    {
        TRequestBuilder builder(CreateTestTree("text"));
        TSearchLiteralFilters filters;
        filters.AddFilter("ware_md5", "93485934859", false);
        filters.AddFilter("ware_md5", "13", true);
        filters.AddFilter("ware_md5", "14", false);
        filters.AddFilter("ware_md5", "15", true);
        builder.AppendSearchLiteralFilters(filters);

        EXPECT_FALSE(filters.IsEmpty());
        EXPECT_EQ("text ~~ (ware_md5:\"93485934859\" | ware_md5:\"14\") << (ware_md5:\"13\" | ware_md5:\"15\")", builder.GetText());
    }
    {
        TRequestBuilder builder;
        TSearchLiteralFilters filters;
        filters.AddFilter("ware_md5", "93485934859", false);
        filters.AddFilter("ware_md5", "13", true);
        builder.AppendSearchLiteralFilters(filters);

        EXPECT_FALSE(filters.IsEmpty());
        EXPECT_EQ("ware_md5:\"13\" ~~ ware_md5:\"93485934859\"", builder.GetText());
    }

    {
        TRequestBuilder builder(CreateTestTree("previous"));
        TSearchLiteralFilters filters;
        filters.AddFilter("ware_md5", "93485934859", false);
        filters.AddFilter("ware_md5", "13", false);
        builder.AppendSearchLiteralFilters(filters);

        EXPECT_TRUE(filters.IsEmpty());
        EXPECT_EQ("previous ~~ (ware_md5:\"93485934859\" | ware_md5:\"13\")", builder.GetText());
    }
    {
        TRequestBuilder builder(CreateTestTree("previous"));
        builder.Or("ware_md5", "1");
        builder.Or("ware_md5", "2");
        builder.Or("ware_md5", "3");
        builder.DocLimit("fesh", "100500");

        EXPECT_EQ("(previous | ware_md5:\"1\" | ware_md5:\"2\" | ware_md5:\"3\") fesh:\"100500\"", builder.GetText());
    }
    { // this type of scenario is used in recommendations
        TRequestBuilder builder;

        for (size_t i = 1; i < 4; ++i) {
            TRequestBuilder inner;
            inner.Or("hyper_id", ToString(i * 10 + 1));
            inner.Or("hyper_id", ToString(i * 10 + 2));
            inner.And("yx_ds_id", ToString(i));
            builder.Or(inner);
        }

        EXPECT_EQ("(((hyper_id:\"11\" | hyper_id:\"12\") &/(-64 64) yx_ds_id:\"1\") | ((hyper_id:\"21\" | hyper_id:\"22\") &/(-64 64) yx_ds_id:\"2\") | ((hyper_id:\"31\" | hyper_id:\"32\") &/(-64 64) yx_ds_id:\"3\"))", builder.GetText());
    }
}
