#include <market/library/pictures/picmeta.h>
#include <market/proto/content/pictures.pb.h>
#include <market/idx/datacamp/proto/offer/OfferPictures.pb.h>
#include <library/cpp/testing/unittest/gtest.h>


TEST(PICMETA, CHECKOK) {
    TString picmeta;
    NMarket::Pictures::AppendPictureMetaInfo(
            picmeta,
            "//avatars.mds.yandex.net/get-mpic/195452/img_id7906161947181029277/orig",
            500,
            600
    );

    ui32 size = *(ui32*)picmeta.data();
    const void* data = ((ui32*)picmeta.data() + 1);

    NMarket::Pictures::Picture pic;
    Y_PROTOBUF_SUPPRESS_NODISCARD pic.ParseFromArray(data, size);

    ASSERT_EQ(pic.width(), 500);
    ASSERT_EQ(pic.height(), 600);
    ASSERT_EQ(pic.group_id(), 195452);
    ASSERT_EQ(pic.imagename(), "img_id7906161947181029277");
    ASSERT_EQ(pic.namespace_(), "mpic");
}


TEST(PICMETA, NEGATIVE) {
    TString picmeta;
    NMarket::Pictures::AppendPictureMetaInfo(
            picmeta,
            "//avatars.mds.yandex.net/get-mpic/195452/img_id7906161947181029277/something wrong",
            500,
            600
    );
    ASSERT_TRUE(picmeta.empty());

    NMarket::Pictures::AppendPictureMetaInfo(
            picmeta,
            "//avatars.mds.yandex.net/get-mpic/group_id_is_incorrect/img_id7906161947181029277/orig",
            500,
            600
    );
    ASSERT_TRUE(picmeta.empty());
}


TEST(PICMETA, LAVKAURL) {
    const TString picUrl = "https://images.grocery.yandex.net/2805921/ba1c9ba1c58547d7b973f9e84dea498f";
    const TVector<Market::Pictures::Size> sizes = { {100,100}, {200, 200}};

    const TString expectedImageName = "ba1c9ba1c58547d7b973f9e84dea498f";
    const TString expectedNamespace = "grocery-goods";
    const TString expectedOrigUrl = "//avatars.mds.yandex.net/get-grocery-goods/2805921/ba1c9ba1c58547d7b973f9e84dea498f/orig";
    const TString expectedMdsHost = "avatars.mds.yandex.net";
    const ui32 expectedGroupId = 2805921;

    Market::DataCamp::MarketPicture pic = NMarket::Pictures::LavkaPic2Meta(picUrl, sizes);

    ASSERT_EQ(pic.id(), expectedImageName);
    ASSERT_EQ(pic.namespace_(), expectedNamespace);
    ASSERT_TRUE(pic.status() == Market::DataCamp::MarketPicture::AVAILABLE);
    ASSERT_EQ(pic.group_id(), expectedGroupId);
    ASSERT_EQ(pic.mds_host(), expectedMdsHost);
    ASSERT_EQ(pic.original().url(), expectedOrigUrl);

    ASSERT_EQ(pic.thumbnailsSize(), sizes.size());

    for (auto i=0; i<pic.thumbnails_size(); i++) {
        ASSERT_EQ(pic.thumbnails(i).width(), sizes[i].width);
        ASSERT_EQ(pic.thumbnails(i).height(), sizes[i].height);
        ASSERT_EQ(pic.thumbnails(i).containerwidth(), sizes[i].width);
        ASSERT_EQ(pic.thumbnails(i).containerheight(), sizes[i].height);
    }
}

TEST(PICMETA, LAVKAURL_IDX) {
    const TString picUrl = "https://images.grocery.yandex.net/2805921/ba1c9ba1c58547d7b973f9e84dea498f";
    NMarket::Pictures::TPictureSpec spec = {.OrigWidth = 1600, .OrigHeight = 1200, .ThumbMask = 255};

    const TString expectedImageName = "ba1c9ba1c58547d7b973f9e84dea498f";
    const TString expectedNamespace = "grocery-goods";
    const ui32 expectedGroupId = 2805921;

    const TVector<NMarket::Pictures::Picture> pictures = NMarket::Pictures::LavkaPic2MetaIdx(picUrl, spec);
    ASSERT_EQ(pictures.size(), 1);

    auto& picture = pictures[0];
    ASSERT_EQ(picture.imagename(), expectedImageName);
    ASSERT_EQ(picture.group_id(), expectedGroupId);
    ASSERT_EQ(picture.namespace_(), expectedNamespace);
    ASSERT_EQ(picture.width(), spec.OrigWidth);
    ASSERT_EQ(picture.height(), spec.OrigHeight);
    ASSERT_EQ(picture.thumb_mask(), spec.ThumbMask);
}

TEST(PICMETA, LAVKAURL_EXTRA) {
    const TString picUrl = "https://tst.images.grocery.yandex.net/2805921/ba1c9ba1c58547d7b973f9e84dea498f/{w}x{h}.png";
    const TVector<Market::Pictures::Size> sizes = { {100,100}, {200, 200}};

    const TString expectedImageName = "ba1c9ba1c58547d7b973f9e84dea498f";
    const TString expectedNamespace = "grocery-goods";
    const TString expectedOrigUrl = "//avatars.mds.yandex.net/get-grocery-goods/2805921/ba1c9ba1c58547d7b973f9e84dea498f/orig";
    const TString expectedMdsHost = "avatars.mds.yandex.net";
    const ui32 expectedGroupId = 2805921;

    Market::DataCamp::MarketPicture pic = NMarket::Pictures::LavkaPic2Meta(picUrl, sizes);

    ASSERT_EQ(pic.id(), expectedImageName);
    ASSERT_EQ(pic.namespace_(), expectedNamespace);
    ASSERT_TRUE(pic.status() == Market::DataCamp::MarketPicture::AVAILABLE);
    ASSERT_EQ(pic.group_id(), expectedGroupId);
    ASSERT_EQ(pic.mds_host(), expectedMdsHost);
    ASSERT_EQ(pic.original().url(), expectedOrigUrl);

    ASSERT_EQ(pic.thumbnailsSize(), sizes.size());

    for (auto i=0; i<pic.thumbnails_size(); i++) {
        ASSERT_EQ(pic.thumbnails(i).width(), sizes[i].width);
        ASSERT_EQ(pic.thumbnails(i).height(), sizes[i].height);
        ASSERT_EQ(pic.thumbnails(i).containerwidth(), sizes[i].width);
        ASSERT_EQ(pic.thumbnails(i).containerwidth(), sizes[i].width);
    }
}

TEST(PICMETA, EDA_URL) {
    const TString picUrl = "https://testing.eda.tst.yandex.net/get-eda/69745/5ee5d174d76fc739ec0eec7ffcce2a8a/orig";
    const TVector<Market::Pictures::Size> sizes = {{100, 100}, {200, 200}};

    const TString expectedImageName = "5ee5d174d76fc739ec0eec7ffcce2a8a";
    const TString expectedNamespace = "eda";
    const TString expectedOrigUrl = "//avatars.mds.yandex.net/get-eda/69745/5ee5d174d76fc739ec0eec7ffcce2a8a/orig";

    Market::DataCamp::MarketPicture pic = NMarket::Pictures::EdaPic2Meta(picUrl, sizes);

    EXPECT_EQ(pic.id(), expectedImageName);
    EXPECT_EQ(pic.namespace_(), expectedNamespace);
    EXPECT_TRUE(pic.status() == Market::DataCamp::MarketPicture::AVAILABLE);
    EXPECT_EQ(pic.original().url(), expectedOrigUrl);

    ASSERT_EQ(pic.thumbnailsSize(), sizes.size());

    for (auto i=0; i<pic.thumbnails_size(); i++) {
        ASSERT_EQ(pic.thumbnails(i).width(), sizes[i].width);
        ASSERT_EQ(pic.thumbnails(i).height(), sizes[i].height);
        ASSERT_EQ(pic.thumbnails(i).containerwidth(), sizes[i].width);
        ASSERT_EQ(pic.thumbnails(i).containerwidth(), sizes[i].width);
    }
}

TEST(PICMETA, LAVKAURL_MALFORMED) {
    const TString picUrl = "https://images.grocery.yandex.net/2805921";
    const TVector<Market::Pictures::Size> sizes = { {100,100}, {200, 200}};

    Market::DataCamp::MarketPicture pic = NMarket::Pictures::LavkaPic2Meta(picUrl, sizes);

    EXPECT_TRUE(pic.status() == Market::DataCamp::MarketPicture::UNDEFINED);
}

TEST(PICMETA, EDA_URL_IDX) {
    const TString picUrl = "https://testing.eda.tst.yandex.net/get-eda/69745/5ee5d174d76fc739ec0eec7ffcce2a8a/orig";
    NMarket::Pictures::TPictureSpec spec = {.OrigWidth = 1600, .OrigHeight = 1200, .ThumbMask = 255};

    const TString expectedImageName = "5ee5d174d76fc739ec0eec7ffcce2a8a";
    const TString expectedNamespace = "eda";
    const ui32 expectedGroupId = 69745;

    const TVector<NMarket::Pictures::Picture> pictures = NMarket::Pictures::EdaPic2MetaIdx(picUrl, spec);
    ASSERT_EQ(pictures.size(), 1);

    auto& picture = pictures[0];
    ASSERT_EQ(picture.imagename(), expectedImageName);
    ASSERT_EQ(picture.group_id(), expectedGroupId);
    ASSERT_EQ(picture.namespace_(), expectedNamespace);
    ASSERT_EQ(picture.width(), spec.OrigWidth);
    ASSERT_EQ(picture.height(), spec.OrigHeight);
    ASSERT_EQ(picture.thumb_mask(), spec.ThumbMask);

}

TEST(PICMETA, AVATARS_URL) {
    const TString picUrl = "https://avatars.mdst.yandex.net/get-ns/3525402/775f72d0150a80c9749de6e788ecb704/orig";
    const TVector<Market::Pictures::Size> sizes = {{100, 100}, {200, 200}};

    const TString expectedImageName = "775f72d0150a80c9749de6e788ecb704";
    const TString expectedNamespace = "ns";
    const TString expectedOrigUrl = "//avatars.mdst.yandex.net/get-ns/3525402/775f72d0150a80c9749de6e788ecb704/orig";

    Market::DataCamp::MarketPicture pic = NMarket::Pictures::AvatarsPic2Meta(picUrl, sizes);

    EXPECT_EQ(pic.id(), expectedImageName);
    EXPECT_EQ(pic.namespace_(), expectedNamespace);
    EXPECT_TRUE(pic.status() == Market::DataCamp::MarketPicture::AVAILABLE);
    EXPECT_EQ(pic.original().url(), expectedOrigUrl);

    ASSERT_EQ(pic.thumbnailsSize(), sizes.size());

    for (auto i=0; i<pic.thumbnails_size(); i++) {
        EXPECT_EQ(pic.thumbnails(i).width(), sizes[i].width);
        EXPECT_EQ(pic.thumbnails(i).height(), sizes[i].height);
        EXPECT_EQ(pic.thumbnails(i).containerwidth(), sizes[i].width);
        EXPECT_EQ(pic.thumbnails(i).containerheight(), sizes[i].height);
    }
}

TEST(PICMETA, AVATARS_URL_IDX) {
    const TString picUrl = "https://avatars.mds.yandex.net/get-ns/3525402/775f72d0150a80c9749de6e788ecb704/orig";
    NMarket::Pictures::TPictureSpec spec = {.OrigWidth = 1600, .OrigHeight = 1200, .ThumbMask = 255};

    const TString expectedImageName = "775f72d0150a80c9749de6e788ecb704";
    const TString expectedNamespace = "ns";
    const ui32 expectedGroupId = 3525402;

    const TVector<NMarket::Pictures::Picture> pictures = NMarket::Pictures::AvatarsPic2MetaIdx(picUrl, spec);
    ASSERT_EQ(pictures.size(), 1);

    auto& picture = pictures[0];
    ASSERT_EQ(picture.imagename(), expectedImageName);
    ASSERT_EQ(picture.group_id(), expectedGroupId);
    ASSERT_EQ(picture.namespace_(), expectedNamespace);
    ASSERT_EQ(picture.width(), spec.OrigWidth);
    ASSERT_EQ(picture.height(), spec.OrigHeight);
    ASSERT_EQ(picture.thumb_mask(), spec.ThumbMask);
}

TEST(PICMETA, MALFORMED_AVATARS_URL) {
    const TString picUrl = "https://avatars.mdst.yandex.net/";
    const TVector<Market::Pictures::Size> sizes = {{100, 100}};

    Market::DataCamp::MarketPicture pic = NMarket::Pictures::AvatarsPic2Meta(picUrl, sizes);

    EXPECT_TRUE(pic.status() == Market::DataCamp::MarketPicture::UNDEFINED);
}
