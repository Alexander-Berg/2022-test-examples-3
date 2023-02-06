#include "processor_test_runner.h"
#include "test_utils.h"

#include <market/idx/feeds/qparser/lib/parser_context.h>
#include <market/idx/feeds/qparser/lib/price_calculator.h>
#include <market/idx/feeds/qparser/lib/processors/offer_required_fields_processor.h>

#include <google/protobuf/util/time_util.h>

#include <library/cpp/testing/common/env.h>

using namespace NMarket;

class TestOfferRequiredFieldsProcessor : public TestProcessor<TOfferRequiredFieldsProcessor> {
    void SetUp() override {
        TestProcessor::SetUp();
        NMarket::TFeedInfo feedInfo = GetDefaultWhiteFeedInfo(EFeedType::YML);
        NMarket::TFeedShopInfo feedShopInfo;
        TParserContext ctx(&feedInfo, &feedShopInfo, nullptr);
    }
};


TEST_F(TestOfferRequiredFieldsProcessor, Condition) {
    {
        auto offer = MakeAtomicShared<IFeedParser::TMsg>();
        offer->GetMutableOriginalSpecification()->mutable_condition()->set_type(Market::DataCamp::Condition_Type_LIKENEW);
        ASSERT_THROW(Process(
                         TFeedInfo{},
                         TFeedShopInfo{},
                         offer),
                     TOfferError);
    }
    {
        auto offer = MakeAtomicShared<IFeedParser::TMsg>();
        offer->GetMutableOriginalSpecification()->mutable_condition()->set_reason("ololo");
        offer= Process(
            TFeedInfo{},
            TFeedShopInfo{},
            offer);
        ASSERT_EQ((int)offer->GetMutableOriginalSpecification()->condition().type(), (int)Market::DataCamp::Condition_Type_UNKNOWN_TYPE);
    }
}

