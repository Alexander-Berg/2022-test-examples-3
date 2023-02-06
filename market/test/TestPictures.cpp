#include "PicturesConfig.h"

#include <market/library/pictures/pictures.h>
#include <market/library/base64_protos_serializer/base64_protos_serializer.h>

#include <util/stream/str.h>
#include <market/proto/indexer/indexarc.pb.h>

#include <library/cpp/testing/unittest/gtest.h>

using namespace Market::Pictures;
using namespace Market;

TEST(PicturesTest, TestGetRealThumbnail) {
    {
        auto rthumb = GetRealThumbnail({300, 300}, {800, 600});
        EXPECT_EQ(300, rthumb.actual_size.width);
        EXPECT_EQ(225, rthumb.actual_size.height);
        EXPECT_EQ(300, rthumb.container_size.width);
        EXPECT_EQ(300, rthumb.container_size.height);
    }
    {
        auto rthumb = GetRealThumbnail({300, 300}, {220, 337});
        EXPECT_EQ(195, rthumb.actual_size.width);
        EXPECT_EQ(300, rthumb.actual_size.height);
        EXPECT_EQ(300, rthumb.container_size.width);
        EXPECT_EQ(300, rthumb.container_size.height);
    }
    {
        auto rthumb = GetRealThumbnail({1, 1}, {800, 600});
        EXPECT_EQ(800, rthumb.actual_size.width);
        EXPECT_EQ(600, rthumb.actual_size.height);
        EXPECT_EQ(1, rthumb.container_size.width);
        EXPECT_EQ(1, rthumb.container_size.height);
    }
    {
        auto rthumb = GetRealThumbnail({0, 0}, {800, 600});
        EXPECT_EQ(0, rthumb.actual_size.width);
        EXPECT_EQ(0, rthumb.actual_size.height);
        EXPECT_EQ(0, rthumb.container_size.width);
        EXPECT_EQ(0, rthumb.container_size.height);
    }
    {
        auto rthumb = GetRealThumbnail({1024, 768}, {800, 600});
        EXPECT_EQ(0, rthumb.actual_size.width);
        EXPECT_EQ(0, rthumb.actual_size.height);
        EXPECT_EQ(0, rthumb.container_size.width);
        EXPECT_EQ(0, rthumb.container_size.height);
    }
}

namespace {
    NMarket::Pictures::Picture MakeProtoPicture(
        const TString& id="75F4CMtNACorGTu1zM60Lg",
        unsigned groupId=1234,
        uint64_t thumbMask = TH_50x50 | TH_100x100 | TH_300x300 | TH_1x1,
        unsigned width=800,
        unsigned height=600) {
        NMarket::Pictures::Picture protoPic;
        protoPic.set_md5(Base64DecodeUneven(id));
        protoPic.set_thumb_mask(thumbMask);
        protoPic.set_width(width);
        protoPic.set_height(height);
        protoPic.set_group_id(groupId);
        return protoPic;
    }

    void ModifyProtoPicture(
        NMarket::Pictures::Picture& protoPic) {
        protoPic.set_width(1024);
        protoPic.set_height(768);
        protoPic.set_group_id(4321);
        protoPic.set_md5(Base64DecodeUneven("iyC3nHslqLtqZJLygVAHeA"));
    }

    bool hasSize(const TVector<Size>& sizes, const Size& size) {
        for (const auto& s: sizes) {
            if (s.width==size.width && s.height==size.height) {
                return true;
            }
        }
        return false;
    }
}


TEST(PicturesTest, TestPictureConfigFindMask) {
    TPictureThumbsConfig cfg(ThumbsMetaInfo, false);
    EXPECT_EQ(1, cfg.FindMask(50, 50));
    EXPECT_EQ(1, cfg.FindMask("50x50"));
    EXPECT_EQ(1 << 6, cfg.FindMask("100x100"));
}


TEST(PicturesTest, TestPictureConfigGetThumbsBySize) {
    TPictureThumbsConfig cfg(ThumbsMetaInfo, false);
    EXPECT_EQ(19, cfg.GetThumbsBySize(Size(900, 1200)).size());

    TThumbMask mask = TH_50x50 | TH_100x100 | TH_300x300;
    auto thumbs = cfg.GetThumbsBySize(Size(800, 600), mask);
    EXPECT_EQ(3, thumbs.size());
    EXPECT_EQ(50, thumbs[0].container_size.width);
    EXPECT_EQ(50, thumbs[0].container_size.height);
    EXPECT_EQ(100, thumbs[1].container_size.width);
    EXPECT_EQ(100, thumbs[1].container_size.height);
    EXPECT_EQ(300, thumbs[2].container_size.width);
    EXPECT_EQ(300, thumbs[2].container_size.height);
}


TEST(PicturesTest, TestProtoPicToMarketPic) {
    TPictureThumbsConfig cfg(ThumbsMetaInfo, false);
    auto protoPic = MakeProtoPicture();

    auto marketPic = ProtoPicToMarketPic(protoPic, cfg);
    EXPECT_EQ("75F4CMtNACorGTu1zM60Lg", marketPic.id);
    EXPECT_EQ(1234, marketPic.group_id);
    EXPECT_EQ(18, marketPic.thumbs.size());
}


TEST(PicturesTest, TestProtoPicToMarketPic2) {
    TPictureThumbsConfig cfg(ThumbsMetaInfo, false);
    auto protoPic = MakeProtoPicture(
        "3gyqe_b19mtKn2C3alta4w",
        167558,
        4611686018427650047,
        100,
        100);

    auto marketPic = ProtoPicToMarketPic(protoPic, cfg);
    EXPECT_EQ("3gyqe_b19mtKn2C3alta4w", marketPic.id);
    EXPECT_EQ(167558, marketPic.group_id);
    for (const auto& t: marketPic.thumbs) {
        EXPECT_TRUE(t.container_size.width != 0);
        EXPECT_TRUE(t.container_size.height != 0);
    }
    EXPECT_EQ(7, marketPic.thumbs.size());
}


TEST(PicturesTest, TestProtoPics) {
    TStringStream ss;

    auto protoPic = MakeProtoPicture();
    TString protoPicSerialized;
    Y_PROTOBUF_SUPPRESS_NODISCARD protoPic.SerializeToString(&protoPicSerialized);
    ss << Base64EncodeUrl(protoPicSerialized);

    ModifyProtoPicture(protoPic);
    Y_PROTOBUF_SUPPRESS_NODISCARD protoPic.SerializeToString(&protoPicSerialized);
    ss << '|' << Base64EncodeUrl(protoPicSerialized);

    TStringStream ss2;
    ParseBase64Protos<NMarket::Pictures::Picture>(
        ss.Str(),
        [&](const NMarket::Pictures::Picture& pic) {
            TString tmp;
            Y_PROTOBUF_SUPPRESS_NODISCARD pic.SerializeToString(&tmp);
            if (!ss2.empty())
                ss2 << '|';
            ss2 << Base64EncodeUrl(tmp);
        }
        );
    EXPECT_EQ(ss.Str(), ss2.Str());
}

TEST(PicturesTest, TestInvalidDataProtoBase64) {
    {
        TString badPicStr = "";
        unsigned nPics = 0;
        ParseBase64Protos<NMarket::Pictures::Picture>(
            badPicStr,
            [&](const NMarket::Pictures::Picture&) {
                ++nPics;
            });
        EXPECT_EQ(0, nPics);
    }
    {
        TString badPicStr = "-1";
        unsigned nPics = 0;
        ParseBase64Protos<NMarket::Pictures::Picture>(
            badPicStr,
            [&](const NMarket::Pictures::Picture&) {
                ++nPics;
            });
        EXPECT_EQ(0, nPics);
    }
}


TEST(PicturesTest, TestProtoBase64PicturesGenerator) {
    TBase64ProtosGenerator<NMarket::Pictures::Picture> gen;

    auto protoPic = MakeProtoPicture();
    gen.Add(protoPic);
    ModifyProtoPicture(protoPic);
    gen.Add(protoPic);

    TStringStream ss;
    ParseBase64Protos<NMarket::Pictures::Picture>(
        gen.GetBase64Protos(),
        [&](const NMarket::Pictures::Picture& pic) {
            TString tmp;
            Y_PROTOBUF_SUPPRESS_NODISCARD pic.SerializeToString(&tmp);
            if (!ss.empty())
                ss << '|';
            ss << Base64EncodeUrl(tmp);
        });
    EXPECT_EQ(gen.GetBase64Protos(), ss.Str());
}

TEST(PicturesTest, SizesParser) {
    const TString sizesStr = "1600x1200,1500x1125,1400x1050,1300x975";
    const auto sizes = parseSizes(sizesStr);

    ASSERT_TRUE(hasSize(sizes, {1600, 1200}));
    ASSERT_TRUE(hasSize(sizes, {1500, 1125}));
    ASSERT_TRUE(hasSize(sizes, {1400, 1050}));
    ASSERT_TRUE(hasSize(sizes, {1300, 975}));
}
