#include "parser_test_runner.h"
#include "test_utils.h"
#include "white_yml_test_runner.h"

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/folder/tempdir.h>
#include <util/system/fs.h>

using namespace NMarket;

static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
<yml_catalog>
  <shop>
    <offers>
      <offer id="white-yml-with-unknown-tag">
        <price>7</price>
        <unknown_tag>surprise mazafaka</unknown_tag>
      </offer>
      <offer id="white-yml-with-unsupported-tag-seller-warranty">
        <price>7</price>
        <seller_warranty>true</seller_warranty>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


TEST(WhiteYmlParser, FeedErrors) {
    TTempDir tempDir;
    TString explanationErrorsFilepath = JoinFsPaths(tempDir.Path(), "feed_errors.pbuf.sn");

    const auto actual = RunWhiteYmlFeedParserWithExplanation<TYmlFeedParser>(
            INPUT_XML,
            [](const TQueueItem& /*item*/) {
                return NSc::TValue();
            },
            GetDefaultWhiteFeedInfo(EFeedType::YML),
            explanationErrorsFilepath
    );

    ASSERT_TRUE(NFs::Exists(explanationErrorsFilepath));

    Market::DataCamp::ExplanationBatch explanationBatch;
    NMarket::TSnappyProtoReader reader(explanationErrorsFilepath, NMarket::NExplanationLog::ExplanationLogMagic);
    reader.Load(explanationBatch);

    ASSERT_EQ(explanationBatch.explanation().size(), 2);
    ASSERT_EQ(explanationBatch.explanation(0).code(), "421");
    ASSERT_EQ(explanationBatch.explanation(0).level(), Market::DataCamp::Explanation::ERROR);

    ASSERT_EQ(explanationBatch.explanation(1).code(), "421");
    ASSERT_EQ(explanationBatch.explanation(1).level(), Market::DataCamp::Explanation::ERROR);
}

