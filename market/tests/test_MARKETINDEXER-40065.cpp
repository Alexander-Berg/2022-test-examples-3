#include <market/idx/feeds/qparser/tests/test_utils.h>
#include <market/idx/feeds/qparser/tests/blue_yml_test_runner.h>
#include <market/idx/feeds/qparser/tests/white_yml_test_runner.h>

#include <market/idx/feeds/qparser/src/feed_parsers/white/yml/feed_parser.h>
#include <market/idx/feeds/qparser/src/feed_parsers/blue/yml/feed_parser.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/generic/maybe.h>
#include <util/folder/path.h>
#include <util/stream/file.h>

#include <google/protobuf/text_format.h>

using namespace NMarket;


namespace {

const auto fname = JoinFsPaths(
    ArcadiaSourceRoot(),
    "market/idx/feeds/qparser/tests/data/MARKETINDEXER-40065-feed.xml"
);
auto fStream = TUnbufferedFileInput(fname);
const auto input_xml = fStream.ReadAll();

}

TEST(WhiteYmlParser, MARKETINDEXER_40065) {
    const auto actual = RunWhiteYmlFeedParser<TYmlFeedParser>(
        input_xml,
        [](const TQueueItem& item) {
            if (item->DataCampOffer.identifiers().offer_id() != "0") {
                return TMaybe<NSc::TValue>{};
            }
            TString s;
            google::protobuf::TextFormat::PrintToString(item->DataCampOffer, &s);
            Cerr << "DataCampOffer: " << s << Endl;

            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            result["Name"] = item->DataCampOffer.content().partner().original().name().value();
            result["Description"] = item->DataCampOffer.content().partner().original().description().value();
            result["Picture"] = item->DataCampOffer.pictures().partner().original().source()[0].url();
            return TMaybe<NSc::TValue>{result};
        },
        GetDefaultWhiteFeedInfo(EFeedType::YML)
    );
    Y_UNUSED(actual);
}


TEST(BlueYmlParser, MARKETINDEXER_40065) {
    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
        input_xml,
        [](const TQueueItem& item) {
            if (item->DataCampOffer.identifiers().offer_id() != "0") {
                return TMaybe<NSc::TValue>{};
            }
            TString s;
            google::protobuf::TextFormat::PrintToString(item->DataCampOffer, &s);
            Cerr << "DataCampOffer: " << s << Endl;

            NSc::TValue result;
            result["Name"] = item->DataCampOffer.content().partner().original().name().value();
            result["Description"] = item->DataCampOffer.content().partner().original().description().value();
            result["Picture"] = item->DataCampOffer.pictures().partner().original().source()[0].url();
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            return TMaybe<NSc::TValue>{result};
        },
        GetDefaultBlueFeedInfo(EFeedType::YML)
    );
    Y_UNUSED(actual);
}
