#include <market/idx/datacamp/lib/conversion/category_conversions.h>

#include <google/protobuf/util/time_util.h>

#include <library/cpp/testing/unittest/gtest.h>

TEST(ConversionsTest, TestCategoriesQparser)
{
    TCategoryInfo categoryYml, categoryCsv;
    categoryYml.Id = 200;
    categoryYml.ParentId = 100;
    categoryYml.Name = "mobile phones";
    categoryCsv.Id = 300;
    categoryCsv.Name = "tables";
    categoryCsv.FromCsvFeed = true;

    google::protobuf::Timestamp timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1);
    uint64_t businessId = 12345;

    for (const auto& category: {categoryYml, categoryCsv}) {
        const auto& datacampCategory = NMarket::NDataCamp::QparserCategory2DataCamp(category, businessId, timestamp, Market::DataCamp::DataSource::PUSH_PARTNER_FEED);
        ASSERT_EQ(datacampCategory.id(), category.Id);
        ASSERT_EQ(datacampCategory.name(), category.Name);
        if (category.ParentId) {
            ASSERT_EQ(datacampCategory.parent_id(), *category.ParentId);
        } else {
            ASSERT_FALSE(datacampCategory.has_parent_id());
        }
        if (category.FromCsvFeed) {
            ASSERT_TRUE(datacampCategory.auto_generated_id());
        } else {
            ASSERT_FALSE(datacampCategory.has_auto_generated_id());
        }
        ASSERT_EQ(datacampCategory.business_id(), businessId);
        ASSERT_EQ(datacampCategory.meta().timestamp().seconds(), timestamp.seconds());
        ASSERT_EQ((ui32)datacampCategory.meta().source(), (ui32)Market::DataCamp::DataSource::PUSH_PARTNER_FEED);
    }
}

TEST(ConversionsTest, TestCategoriesFeedparser)
{
    NMarket::TFeedCategory categoryYml, categoryCsv;
    categoryYml.id = "500";
    categoryYml.parent_id = "300";
    categoryYml.name = "notebooks";
    categoryCsv.id = "700";
    categoryCsv.name = "toys";
    categoryCsv.fromCsvFeed = true;
    google::protobuf::Timestamp timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(10);
    uint64_t businessId = 100500;

    for (const auto& category: {categoryYml, categoryCsv}) {
        const auto& datacampCategory = NMarket::NDataCamp::FeedparserCategory2DataCamp(category, businessId, timestamp, Market::DataCamp::DataSource::PUSH_PARTNER_FEED);
        ASSERT_EQ(ToString(datacampCategory.id()), TString(category.id));
        ASSERT_EQ(datacampCategory.name(), TString(category.name));
        if (category.parent_id.empty()) {
            ASSERT_FALSE(datacampCategory.has_parent_id());
        } else {
            ASSERT_EQ(ToString(datacampCategory.parent_id()), TString(category.parent_id));
        }
        if (category.fromCsvFeed) {
            ASSERT_TRUE(datacampCategory.auto_generated_id());
        } else {
            ASSERT_FALSE(datacampCategory.has_auto_generated_id());
        }
        ASSERT_EQ(datacampCategory.business_id(), businessId);
        ASSERT_EQ(datacampCategory.meta().timestamp().seconds(), timestamp.seconds());
        ASSERT_EQ((ui32)datacampCategory.meta().source(), (ui32)Market::DataCamp::DataSource::PUSH_PARTNER_FEED);
    }
}

TEST(ConversionsTest, TestAbsentIds)
{
    NMarket::TFeedCategory category;
    category.name = "category without ids";
    google::protobuf::Timestamp timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(10);
    uint64_t businessId = 100500;

    const auto& datacampCategory = NMarket::NDataCamp::FeedparserCategory2DataCamp(category, businessId, timestamp, Market::DataCamp::DataSource::PUSH_PARTNER_FEED);
    ASSERT_FALSE(datacampCategory.has_id());
    ASSERT_EQ(datacampCategory.name(), TString(category.name));
    ASSERT_FALSE(datacampCategory.has_parent_id());
}
