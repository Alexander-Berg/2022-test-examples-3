#include "processor_test_runner.h"
#include "test_utils.h"
#include <market/idx/feeds/qparser/lib/processors/repeated_fields_limit_processor.h>
#include <library/cpp/testing/common/env.h>

#include <google/protobuf/util/time_util.h>

using namespace NMarket;

using TestRepeatedFieldsLimitProcessor = TestProcessor<TRepeatedFieldsLimitProcessor>;

TEST_F(TestRepeatedFieldsLimitProcessor, OfferParams) {
    TOfferCarrier initial(TFeedInfo{});

    const int paramsCount = 50;
    for(int i=0; i<paramsCount; ++i) {
        auto* param = initial.GetMutableOriginalSpecification()->mutable_offer_params()->add_param();
        auto num = std::to_string(i);
        param->set_value("value-" + num);
        param->set_name("name-" + num);
        param->set_unit("unit-" + num);
    }

    NMarket::TFeedShopInfo feedShopInfo;

    // Проверяем, что при превышении лимита количество параметров ограничивается
    NMarket::TFeedInfo feedInfoExceed = GetDefaultWhiteFeedInfo(EFeedType::YML);
    feedInfoExceed.OfferParamsLimit = paramsCount - 10;
    const auto offerExceed = Process(feedInfoExceed, feedShopInfo, MakeAtomicShared<IFeedParser::TMsg>(initial));
    ASSERT_EQ(offerExceed->GetOriginalSpecification().offer_params().param().size(), feedInfoExceed.OfferParamsLimit);

    // Если лимит не превышен - количество параметров не меняется
    NMarket::TFeedInfo feedInfoNotExceed = GetDefaultWhiteFeedInfo(EFeedType::YML);
    feedInfoNotExceed.OfferParamsLimit = paramsCount + 10;
    const auto offerNotExceed = Process(feedInfoNotExceed, feedShopInfo, MakeAtomicShared<IFeedParser::TMsg>(initial));
    ASSERT_EQ(offerNotExceed->GetOriginalSpecification().offer_params().param().size(), paramsCount);
}

TEST_F(TestRepeatedFieldsLimitProcessor, OfferPictures) {
    TOfferCarrier initial(TFeedInfo{});

    const int picturesCount = 20;
    for(int i=0; i<picturesCount; ++i) {
        initial.DataCampOffer.mutable_pictures()->mutable_partner()->mutable_original()->add_source();
    }

    NMarket::TFeedShopInfo feedShopInfo;

    // Проверяем, что при превышении лимита количество картинок ограничивается
    NMarket::TFeedInfo feedInfoExceed = GetDefaultWhiteFeedInfo(EFeedType::YML);
    feedInfoExceed.OfferPicturesLimit = picturesCount - 10;
    const auto offerExceed = Process(feedInfoExceed, feedShopInfo, MakeAtomicShared<IFeedParser::TMsg>(initial));
    ASSERT_EQ(offerExceed->DataCampOffer.pictures().partner().original().source().size(), feedInfoExceed.OfferPicturesLimit);

    // Если лимит не превышен - количество картинок не меняется
    NMarket::TFeedInfo feedInfoNotExceed = GetDefaultWhiteFeedInfo(EFeedType::YML);
    feedInfoNotExceed.OfferPicturesLimit = picturesCount + 10;
    const auto offerNotExceed = Process(feedInfoNotExceed, feedShopInfo, MakeAtomicShared<IFeedParser::TMsg>(initial));
    ASSERT_EQ(offerNotExceed->DataCampOffer.pictures().partner().original().source().size(), picturesCount);
}
