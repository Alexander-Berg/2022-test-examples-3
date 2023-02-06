#include <market/idx/feeds/qparser/tests/white_yml_test_runner.h>
#include <market/idx/feeds/qparser/src/feed_parsers/white/yml/feed_parser.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>


using namespace NMarket;

static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
<yml_catalog date="2019-12-03 00:00">
  <shop>
    <offers>
      <offer id="yml-with-video">
        <price>7</price>
        <video>http://test.video.example.ru</video>
      </offer>
      <offer id="yml-without-video">
        <price>77</price>
      </offer>
      <offer id="yml-with-many-videos">
        <price>7</price>
        <video>http://test.video.example.ru/1</video>
        <video>https://test.video.example.ru/2</video>
      </offer>
      <offer id="yml-with-wrong-video">
        <price>7</price>
        <video>file://wrong-video</video>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "yml-with-video",
        "Picture": "{ source { meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } videos { url: \"test.video.example.ru/\" } } }"
    },
    {
        "OfferId": "yml-without-video",
        "Picture": "{  }",
    },
    {
        "OfferId": "yml-with-many-videos",
        "Picture": "{ source { meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } videos { url: \"test.video.example.ru/1\" } videos { url: \"https://test.video.example.ru/2\" } } }"
    },
    {
        "OfferId": "yml-with-wrong-video",
        "Picture": "{ source { meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } } }"
    }
]
)wrap");


TEST(WhiteYmlParser, Video) {
    const auto actual = RunWhiteYmlFeedParser<TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            result["Picture"] = ToString(item->DataCampOffer.pictures().videos());
            return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::YML)
);
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
