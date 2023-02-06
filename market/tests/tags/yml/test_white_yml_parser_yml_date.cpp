#include <market/idx/feeds/qparser/tests/test_utils.h>
#include <market/idx/feeds/qparser/tests/white_yml_test_runner.h>

#include <market/idx/feeds/qparser/src/feed_parsers/white/yml/feed_parser.h>

#include <market/library/date/date.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <google/protobuf/util/time_util.h>

using namespace NMarket;

using ::google::protobuf::util::TimeUtil;

// специальная дата в формате "%Y-%m-%d", чтобы пройти проверки на будущее и устаревшесть
TDate YESTERDAY = TDate::TodayLocal() - 1;
TString YESTERDAY_STR = YESTERDAY.ToString();

TVector<TString> INPUT_YML_DATES = {
  YESTERDAY_STR + " 16:00:00Z",
  YESTERDAY_STR + " 16:00:00z",
  YESTERDAY_STR + " 16:00:00+02",
  YESTERDAY_STR + " 16:00:00-03:30",
  YESTERDAY_STR + " 16:00:00-0330",
  YESTERDAY_STR,
  YESTERDAY_STR + "00000"
  "2120-01-01 00:00",
  "1999-01-01 00:00"
};

TVector<TString> EXPECTED_YML_DATES = {
  YESTERDAY_STR + "T16:00:00Z",
  YESTERDAY_STR + "T16:00:00Z",
  YESTERDAY_STR + "T14:00:00Z",
  YESTERDAY_STR + "T19:30:00Z",
  YESTERDAY_STR + "T19:30:00Z",
  (YESTERDAY - 1).ToString() + "T21:00:00Z",
  "",
  "",
  ""
};


static const TString INPUT_XML_WITHOUT_DATE(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
<yml_catalog>
  <shop>
    <offers>
      <offer id="yml-white-without-date">
        <price>2</price>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");

static const TString EXPECTED_JSON_WITHOUT_DATE = TString(R"wrap(
[
    {
        "OfferId": "yml-white-without-date",
        "Price": 2,
        "TimeStampSec": "1000"
    }
]
)wrap");

NSc::TValue GetOfferPart(const TQueueItem& item) {
    NSc::TValue result;
    result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
    if (item->RawPrice) {
        result["Price"] = *item->RawPrice;
    }
    result["TimeStampSec"] = ToString(item->DataCampOffer.price().basic().meta().timestamp().seconds());
    return result;
}

TEST(WhiteYmlParser, YmlDate) {
    for (uint i = 0; i < INPUT_YML_DATES.size(); ++i) {
      const auto inputXml = R"wrap(<?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
        <yml_catalog date=")wrap" + INPUT_YML_DATES[i] +  R"wrap(">
          <shop>
            <offers>
              <offer id="yml-white-with-date">
                <price>2</price>
              </offer>
            </offers>
          </shop>
        </yml_catalog>)wrap";

      THolder<IFeedParser> feedParser;
      const auto actual = RunWhiteYmlFeedParser<TYmlFeedParser>(
          inputXml,
          GetOfferPart,
          feedParser,
          GetDefaultWhiteFeedInfo(EFeedType::YML));

      TString expectedTs = "1000";
      if (not EXPECTED_YML_DATES[i].Empty()) {
          time_t ts;
          ParseISO8601DateTime(EXPECTED_YML_DATES[i].c_str(), ts);
          expectedTs = ToString(TInstant::Seconds(ts).Seconds());
      }

      const auto expected = NSc::TValue::FromJson(TString(R"wrap(
        [
            {
                "OfferId": "yml-white-with-date",
                "Price": 2,
                "TimeStampSec":
        )wrap" + expectedTs.Quote() + R"wrap(
            }
        ]
        )wrap"));

      EXPECT_EQ(expected, actual);
      if (feedParser->GetFeedShopInfo().YmlDateStr) {
          // YmlDate should be passed to FeedInfo.Timestamp
          EXPECT_EQ(TimeUtil::ToString(feedParser->GetFeedInfo().Timestamp), EXPECTED_YML_DATES[i]);
      }
    }
}

TEST(WhiteYmlParser, EmptyYmlDate) {
    const auto actual = RunWhiteYmlFeedParser<TYmlFeedParser>(
        INPUT_XML_WITHOUT_DATE,
        GetOfferPart,
        GetDefaultWhiteFeedInfo(EFeedType::YML));
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON_WITHOUT_DATE);
    EXPECT_EQ(expected, actual);
}
