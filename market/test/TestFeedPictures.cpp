#include "PicturesConfig.h"

#include <market/library/pictures/feed_pictures.h>

#include <library/cpp/string_utils/base64/base64.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

#include <string>


TEST(TestFeedPictures, JsonToProto) {
    TString from = SRC_("./tests_data/pictures.json");
    TString to = "pictures.pb.sn";
    Market::FeedPics::TPictures::ConvertJsonToPbsn(from, to);
    Market::FeedPics::TPictures pics;
    pics.LoadPbsnFile(to);

    auto p1 = pics.Find("awago.ru/upload/iblock/9d0/9d05fa8f6b7b56c127f4e88c902ddc5b.jpg");
    EXPECT_TRUE(p1 != nullptr);
    EXPECT_EQ("erTLvTWhkhT1Ixn5hnFceA", p1->id);
    EXPECT_EQ(360, p1->OriginalWidth);
    EXPECT_EQ(360, p1->OriginalHeight);
    EXPECT_EQ(0, p1->GroupId);

    auto p2 = pics.Find("awago.ru/upload/iblock/8ca/8ca92cbaced5874aab995357f7b9568a.jpg");
    EXPECT_TRUE(p2 != nullptr);
    EXPECT_EQ(4210, p2->GroupId);

    auto p3 = pics.Find("awago.ru/upload/iblock/8ca/does_not_exist.jpg");
    EXPECT_EQ(nullptr, p3);
}


TEST(TestFeedPictures, TestFeedPicToProtoPic) {
    Market::Pictures::TPictureThumbsConfig cfg(ThumbsMetaInfo, false);
    TString picsFile = SRC_("./tests_data/pictures.json");
    Market::FeedPics::TPictures pics;
    pics.LoadJsonFile(picsFile);

    auto pic = pics.Find("awago.ru/upload/iblock/9d0/9d05fa8f6b7b56c127f4e88c902ddc5b.jpg");

    auto protoPic = FeedPicToProtoPic(*pic, cfg);
    EXPECT_EQ("erTLvTWhkhT1Ixn5hnFceA", Base64EncodeUrl(protoPic.md5()).substr(0, 22));
    EXPECT_EQ(360, protoPic.width());
    EXPECT_EQ(360, protoPic.height());
    EXPECT_EQ(false, protoPic.has_group_id());
    EXPECT_EQ(278527, protoPic.thumb_mask());

    EXPECT_EQ(2, protoPic.signatures_size());
    EXPECT_EQ(3, protoPic.signatures(1).version());
    EXPECT_TRUE(protoPic.signatures(1).clothes_bin() - 22.2784 < 0.01);
    auto similar = protoPic.signatures(1).similar();
    EXPECT_TRUE(96 + 4  == similar.length());
    auto clothes = protoPic.signatures(1).clothes();
    EXPECT_TRUE(2*4 + 4  == clothes.length());

    auto pic2 = pics.Find("awago.ru/upload/iblock/8ca/8ca92cbaced5874aab995357f7b9568a.jpg");
    auto protoPic2 = FeedPicToProtoPic(*pic2, cfg);
    EXPECT_EQ(0, protoPic2.signatures_size());
}
