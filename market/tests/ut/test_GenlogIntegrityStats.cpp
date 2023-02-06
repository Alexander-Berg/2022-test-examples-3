#include <library/cpp/testing/unittest/gtest.h>
#include <market/idx/offers/lib/iworkers/GenlogIntegrityStats.h>

TEST(TestGenlogIntegrityStats, Dummy)
{
    // arrange
    TGenlogIntegrityStats integrity;

    MarketIndexer::GenerationLog::Record normalRecord;
    normalRecord.set_downloadable(true);
    normalRecord.set_picture_url("http://some-url.jpg");
    normalRecord.set_feed_id(42);
    normalRecord.add_post_bucket_ids(1);

    MarketIndexer::GenerationLog::Record zeroRecord;
    zeroRecord.set_downloadable(false);
    zeroRecord.set_picture_url("");
    zeroRecord.set_feed_id(0);
    zeroRecord.add_post_bucket_ids(0);

    MarketIndexer::GenerationLog::Record emptyRecord;

    // act
    integrity.Accumulate(normalRecord, true);
    integrity.Accumulate(zeroRecord, true);
    integrity.Accumulate(emptyRecord, true);

    const auto *desc = MarketIndexer::GenerationLog::Record::descriptor();
    i32 downloadable_index = 0;
    i32 picture_url_index = 0;
    i32 feed_id_index = 0;
    i32 post_bucket_ids_index = 0;
    i32 age_unit_index = 0;
    for (i32 i = 0; i < desc->field_count(); i++) {
        auto name = desc->field(i)->name();
        if (name == "downloadable") {
            downloadable_index = i;
        } else if (name == "picture_url") {
            picture_url_index = i;
        } else if (name == "feed_id") {
            feed_id_index = i;
        } else if (name == "post_bucket_ids") {
            post_bucket_ids_index = i;
        } else if (name == "age_unit") {
            age_unit_index = i;
        }
    }

    // assert
    auto stats = integrity.GetStats();
    EXPECT_EQ(stats[downloadable_index].NullCount, 1);
    EXPECT_EQ(stats[downloadable_index].NonNullCount, 2);

    EXPECT_EQ(stats[picture_url_index].NullCount, 2);
    EXPECT_EQ(stats[picture_url_index].NonNullCount, 1);

    EXPECT_EQ(stats[feed_id_index].NullCount, 1);
    EXPECT_EQ(stats[feed_id_index].NonNullCount, 2);

    EXPECT_EQ(stats[post_bucket_ids_index].NullCount, 1);
    EXPECT_EQ(stats[post_bucket_ids_index].NonNullCount, 2);

    EXPECT_EQ(stats[age_unit_index].NullCount, 3);
    EXPECT_EQ(stats[age_unit_index].NonNullCount, 0);
}
