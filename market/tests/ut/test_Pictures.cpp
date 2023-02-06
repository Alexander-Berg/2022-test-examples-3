#include <market/library/pictures/feed_pictures.h>
#include <util/stream/str.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

using namespace Market::FeedPics;

TEST(PictureTest, parsesFormat)
{
    const TString in = R"delimiter(
        {"url": "strkate.com", "id": "TNgVC3sCObT8dsObXVay6Q", "thumbnails": [[[90, 120], [90, 90]], [[190, 250], [190, 190]]]},
        {"id": "ycPPLzd9auHU8Jqk9-dGDw", "thumbnails": [[[90, 120], [90, 90]], [[190, 250], [190, 190]]], "url": "liubovd.ru"},
        {"thumbnails": [[[90, 120], [90, 90]]], "signatures": [{"version", 1, "clothes_bin": "0ikNPw==", "similar": "ZAAAADzfIE5=", "clothes": "AwAAAEgAAAC="}], "url": "irinka.net", "id": "DNd8-IVvlN0xuQoHq5zFHw"},
        {"id": "iVhkJvekULFLzevthAPfXQ", "thumbnails": [[[90, 120], [90, 90]], [[190, 250], [190, 190]]], "url": "http://идн-тест.яндекс.рф"},

    )delimiter";

    TStringInput input(in);
    TPictures pics;
    pics.LoadJsonStreamFast(input);
    auto strkate = pics.Find("strkate.com");
    ASSERT_TRUE(strkate);
    EXPECT_EQ("TNgVC3sCObT8dsObXVay6Q", strkate->id);
    EXPECT_EQ(0, strkate->signatures.size());
    EXPECT_EQ(2, strkate->thumbnails->size());
    EXPECT_EQ(90, strkate->thumbnails->at(0).rw);
    EXPECT_EQ(120, strkate->thumbnails->at(0).rh);
    EXPECT_EQ(90, strkate->thumbnails->at(0).w);
    EXPECT_EQ(90, strkate->thumbnails->at(0).h);
    EXPECT_EQ(190, strkate->thumbnails->at(1).rw);
    EXPECT_EQ(250, strkate->thumbnails->at(1).rh);
    EXPECT_EQ(190, strkate->thumbnails->at(1).w);
    EXPECT_EQ(190, strkate->thumbnails->at(1).h);

    auto liubovd = pics.Find("liubovd.ru");
    ASSERT_TRUE(liubovd);
    EXPECT_EQ("ycPPLzd9auHU8Jqk9-dGDw", liubovd->id);
    EXPECT_EQ(2, liubovd->thumbnails->size());

    // Cyrillic domain
    auto kir = pics.Find("http://идн-тест.яндекс.рф");
    ASSERT_TRUE(kir);
    EXPECT_EQ("iVhkJvekULFLzevthAPfXQ", kir->id);
    EXPECT_EQ(2, kir->thumbnails->size());

    auto irinka = pics.Find("irinka.net");
    ASSERT_TRUE(irinka);
    EXPECT_EQ("DNd8-IVvlN0xuQoHq5zFHw", irinka->id);
    EXPECT_EQ(1, irinka->thumbnails->size());
    EXPECT_EQ("ZAAAADzfIE5=", irinka->signatures[0].similar);
    EXPECT_EQ("AwAAAEgAAAC=", irinka->signatures[0].clothes);
    EXPECT_EQ("0ikNPw==", irinka->signatures[0].clothes_bin);
    EXPECT_EQ(1, irinka->signatures[0].version);
    EXPECT_EQ(1, irinka->signatures.size());
}
