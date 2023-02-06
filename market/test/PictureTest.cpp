#include <vector>
#include <algorithm>
#include <sstream>
#include <market/report/src/Picture.h>
#include <market/report/library/xml_simple_out/xml_simple_out.h>
#include <market/proto/indexer/indexarc.pb.h>
#include "ThumbsConfig.h"
#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/string_utils/base64/base64.h>
#include <util/stream/str.h>

using namespace NMarketReport;

using Children = const XmlOut::Tree::children_t;

TEST(Picture, Base64ProtoEncoded0x0) {
    NMarket::Pictures::Picture pic;

    const TString avatar_template = "http://avatars.mdst.yandex.net/get-marketpic/{{group_id}}/market_{{picture_id}}/{{thumb_name}}";

    TPictureTemplates tmpl(avatar_template);
    Market::Pictures::TPictureThumbsConfig cfg(NPrivate::GetFakeThumbsConfig(), false);

    NMarket::Pictures::Picture protoPic;
    protoPic.set_md5(Base64DecodeUneven("TEMI9WPKIpAVGOGParn8Iw"));
    protoPic.set_thumb_mask(278527);
    protoPic.set_width(220);
    protoPic.set_height(337);
    protoPic.set_group_id(1234);

    PictureDataGenerator gen(tmpl, cfg);
    TString protoPicSerialized;
    TStringStream ss;
    Y_PROTOBUF_SUPPRESS_NODISCARD protoPic.SerializeToString(&protoPicSerialized);
    ss << Base64EncodeUrl(protoPicSerialized);
    gen.AddProtobufBase64Pictures(ss.Str(), false);
    EXPECT_EQ(1, gen.GetPictures().size());

    TSet<TString> thumbUrls;
    for (const auto& pict: gen.GetPictures()) {
        for (const auto& thumb: pict.Thumbnails) {
            thumbUrls.insert(thumb.Url);
        }
    }

    TSet<TString> expectedUrls = {
        "http://avatars.mdst.yandex.net/get-marketpic/1234/market_TEMI9WPKIpAVGOGParn8Iw/50x50",
        "http://avatars.mdst.yandex.net/get-marketpic/1234/market_TEMI9WPKIpAVGOGParn8Iw/55x70",
        "http://avatars.mdst.yandex.net/get-marketpic/1234/market_TEMI9WPKIpAVGOGParn8Iw/60x80",
        "http://avatars.mdst.yandex.net/get-marketpic/1234/market_TEMI9WPKIpAVGOGParn8Iw/74x100",
        "http://avatars.mdst.yandex.net/get-marketpic/1234/market_TEMI9WPKIpAVGOGParn8Iw/75x75",
        "http://avatars.mdst.yandex.net/get-marketpic/1234/market_TEMI9WPKIpAVGOGParn8Iw/90x120",
        "http://avatars.mdst.yandex.net/get-marketpic/1234/market_TEMI9WPKIpAVGOGParn8Iw/100x100",
        "http://avatars.mdst.yandex.net/get-marketpic/1234/market_TEMI9WPKIpAVGOGParn8Iw/120x160",
        "http://avatars.mdst.yandex.net/get-marketpic/1234/market_TEMI9WPKIpAVGOGParn8Iw/150x150",
        "http://avatars.mdst.yandex.net/get-marketpic/1234/market_TEMI9WPKIpAVGOGParn8Iw/180x240",
        "http://avatars.mdst.yandex.net/get-marketpic/1234/market_TEMI9WPKIpAVGOGParn8Iw/190x250",
        "http://avatars.mdst.yandex.net/get-marketpic/1234/market_TEMI9WPKIpAVGOGParn8Iw/200x200",
        "http://avatars.mdst.yandex.net/get-marketpic/1234/market_TEMI9WPKIpAVGOGParn8Iw/240x320",
        "http://avatars.mdst.yandex.net/get-marketpic/1234/market_TEMI9WPKIpAVGOGParn8Iw/300x300",
        "http://avatars.mdst.yandex.net/get-marketpic/1234/market_TEMI9WPKIpAVGOGParn8Iw/orig"};

    EXPECT_EQ(expectedUrls, thumbUrls);
}
