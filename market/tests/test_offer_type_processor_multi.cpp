#include "processor_test_runner.h"
#include "test_utils.h"

#include <market/idx/feeds/qparser/lib/parser_context.h>
#include <market/idx/feeds/qparser/lib/processors/offer_type_processor.h>

#include <google/protobuf/util/time_util.h>

#include <library/cpp/testing/common/env.h>

using namespace Market::DataCamp;
using namespace NMarket;

namespace {

class TestOfferTypeMultiProcessor : public TestProcessor<TOfferTypeProcessor> {
public:
    TFeedInfo FeedInfo;
    TFeedShopInfo FeedShopInfo;
    ui64 Seconds = 1000;
public:
    IWriter::TMsgPtr DoProcess(Offer offer) {
        return Process(
            FeedInfo,
            FeedShopInfo,
            MakeAtomicShared<IFeedParser::TMsg>(
                TOfferCarrier(FeedInfo)
                    .WithDataCampOffer(std::move(offer))
            )
        );
    }
private:
    void SetUp() override {
        TestProcessor::SetUp();
        FeedInfo = GetDefaultWhiteFeedInfo(EFeedType::YML);
        FeedInfo.PushFeedClass = Market::DataCamp::API::FeedClass::FEED_CLASS_ASSORTMENT_BASIC_PATCH_UPDATE_SALE_TERMS_SERVICE_FULL_COMPLETE;
        TParserContext ctx(&FeedInfo, &FeedShopInfo, nullptr);
        FeedInfo.Timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(Seconds);
    }
};

} // anonymous namespace

TEST_F(TestOfferTypeMultiProcessor, HideOfferWithNotSupportedType) {
    Offer datacampOffer;
    datacampOffer.mutable_content()->mutable_partner()->mutable_original()->mutable_type()->set_value(ALCO);
    auto offer = DoProcess(datacampOffer);

    ASSERT_TRUE(offer->IsBasicIgnored());
    ASSERT_FALSE(offer->IsServiceIgnored());
}

TEST_F(TestOfferTypeMultiProcessor, NotHideOfferWithSupportedTypeSimple) {
    Offer datacampOffer;
    datacampOffer.mutable_content()->mutable_partner()->mutable_original()->mutable_type()->set_value(SIMPLE);
    auto offer = DoProcess(datacampOffer);

    ASSERT_FALSE(offer->IsBasicIgnored());
    ASSERT_FALSE(offer->IsServiceIgnored());
}

TEST_F(TestOfferTypeMultiProcessor, NotHideOfferWithSupportedTypeMedicine) {
    Offer datacampOffer;
    datacampOffer.mutable_content()->mutable_partner()->mutable_original()->mutable_type()->set_value(MEDICINE);
    auto offer = DoProcess(datacampOffer);

    ASSERT_FALSE(offer->IsBasicIgnored());
    ASSERT_FALSE(offer->IsServiceIgnored());
}

TEST_F(TestOfferTypeMultiProcessor, HideOfferWithBooksType) {
    Offer datacampOffer;
    datacampOffer.mutable_content()->mutable_partner()->mutable_original()->mutable_type()->set_value(BOOKS);
    auto offer = DoProcess(datacampOffer);

    ASSERT_TRUE(offer->IsBasicIgnored());
    ASSERT_FALSE(offer->IsServiceIgnored());
}

TEST_F(TestOfferTypeMultiProcessor, OfferWithBooksType_AllowOnFeatureFlag) {
    FeedInfo.AllowBooksType = true;
    Offer datacampOffer;
    datacampOffer.mutable_content()->mutable_partner()->mutable_original()->mutable_type()->set_value(BOOKS);
    auto offer = DoProcess(datacampOffer);

    ASSERT_FALSE(offer->IsBasicIgnored());
    ASSERT_FALSE(offer->IsServiceIgnored());
}

TEST_F(TestOfferTypeMultiProcessor, HideOfferWithAudiobooksType) {
    Offer datacampOffer;
    datacampOffer.mutable_content()->mutable_partner()->mutable_original()->mutable_type()->set_value(AUDIOBOOKS);
    auto offer = DoProcess(datacampOffer);

    ASSERT_TRUE(offer->IsBasicIgnored());
    ASSERT_FALSE(offer->IsServiceIgnored());
}

TEST_F(TestOfferTypeMultiProcessor, OfferWithAudiobooksType_AllowOnFeatureFlag) {
    FeedInfo.AllowBooksType = true;
    Offer datacampOffer;
    datacampOffer.mutable_content()->mutable_partner()->mutable_original()->mutable_type()->set_value(AUDIOBOOKS);
    auto offer = DoProcess(datacampOffer);

    ASSERT_FALSE(offer->IsBasicIgnored());
    ASSERT_FALSE(offer->IsServiceIgnored());
}

TEST_F(TestOfferTypeMultiProcessor, OfferWithAlcoType_AllowOnFeatureFlag) {
    FeedInfo.AllowAlcoholType = true;
    Offer datacampOffer;
    datacampOffer.mutable_content()->mutable_partner()->mutable_original()->mutable_type()->set_value(ALCO);
    auto offer = DoProcess(datacampOffer);

    ASSERT_FALSE(offer->IsBasicIgnored());
    ASSERT_FALSE(offer->IsServiceIgnored());
}

TEST_F(TestOfferTypeMultiProcessor, HideOfferWithAlcoType) {
    Offer datacampOffer;
    datacampOffer.mutable_content()->mutable_partner()->mutable_original()->mutable_type()->set_value(ALCO);
    auto offer = DoProcess(datacampOffer);

    ASSERT_TRUE(offer->IsBasicIgnored());
    ASSERT_FALSE(offer->IsServiceIgnored());
}

TEST_F(TestOfferTypeMultiProcessor, HideOfferWithArtistTitleType) {
    Offer datacampOffer;
    datacampOffer.mutable_content()->mutable_partner()->mutable_original()->mutable_type()->set_value(ARTIST_TITLE);
    auto offer = DoProcess(datacampOffer);

    ASSERT_TRUE(offer->IsBasicIgnored());
    ASSERT_FALSE(offer->IsServiceIgnored());
}

TEST_F(TestOfferTypeMultiProcessor, OfferWithArtistTitleType_AllowOnFeatureFlag) {
    FeedInfo.AllowArtistTitleType = true;
    Offer datacampOffer;
    datacampOffer.mutable_content()->mutable_partner()->mutable_original()->mutable_type()->set_value(ARTIST_TITLE);
    auto offer = DoProcess(datacampOffer);

    ASSERT_FALSE(offer->IsBasicIgnored());
    ASSERT_FALSE(offer->IsServiceIgnored());
}

TEST_F(TestOfferTypeMultiProcessor, VendorModelToSimple) {
    Offer datacampOffer;
    auto* originalSpecification = datacampOffer.mutable_content()->mutable_partner()->mutable_original();
    originalSpecification->mutable_type()->set_value(VENDOR_MODEL);
    originalSpecification->mutable_type_prefix()->set_value("Smartphone");
    originalSpecification->mutable_vendor()->set_value("Apple");
    originalSpecification->mutable_model()->set_value("IPhone 12 Pro Max");
    originalSpecification->mutable_name()->set_value("Partner name");

    auto offer = DoProcess(datacampOffer);
    ASSERT_FALSE(offer->IsBasicIgnored());
    ASSERT_FALSE(offer->IsServiceIgnored());

    const auto& resultOriginalSpecification = offer->GetOriginalSpecification();
    ASSERT_TRUE(resultOriginalSpecification.type().value() == SIMPLE);
    ASSERT_EQ(resultOriginalSpecification.original_name().value(), "Partner name");

    ASSERT_EQ(resultOriginalSpecification.name().value(), "Smartphone Apple IPhone 12 Pro Max");
    ASSERT_EQ(resultOriginalSpecification.name().meta().timestamp().seconds(), Seconds);

    ASSERT_EQ(resultOriginalSpecification.name_no_vendor().value(), "Smartphone IPhone 12 Pro Max");
    ASSERT_EQ(resultOriginalSpecification.name_no_vendor().meta().timestamp().seconds(), Seconds);
}

TEST_F(TestOfferTypeMultiProcessor, AddAuthorToNameForBooks_FilledAuthor) {
    FeedInfo.AllowBooksType = true;

    Offer datacampOffer;
    auto* originalSpecification = datacampOffer.mutable_content()->mutable_partner()->mutable_original();
    originalSpecification->mutable_type()->set_value(BOOKS);
    originalSpecification->mutable_name()->set_value("?????? ????????????");

    auto offerCarrier = TOfferCarrier(FeedInfo)
        .WithDataCampOffer(std::move(datacampOffer))
        .WithTitlePrefixForSpecificTypes("??. ??. ??????????");

    auto offer = Process(
        FeedInfo,
        FeedShopInfo,
        MakeAtomicShared<IFeedParser::TMsg>(offerCarrier)
    );

    ASSERT_FALSE(offer->IsBasicIgnored());
    ASSERT_FALSE(offer->IsServiceIgnored());
    const auto& resultOriginalSpecification = offer->GetOriginalSpecification();
    ASSERT_TRUE(resultOriginalSpecification.type().value() == BOOKS);
    ASSERT_EQ(resultOriginalSpecification.name().value(), "??. ??. ?????????? \"?????? ????????????\"");
}

TEST_F(TestOfferTypeMultiProcessor, AddAuthorToNameForBooks_EmptyAuthor) {
    FeedInfo.AllowBooksType = true;

    Offer datacampOffer;
    auto* originalSpecification = datacampOffer.mutable_content()->mutable_partner()->mutable_original();
    originalSpecification->mutable_type()->set_value(BOOKS);
    originalSpecification->mutable_name()->set_value("???????????????? ?? ??????????????????");

    auto offerCarrier = TOfferCarrier(FeedInfo)
        .WithDataCampOffer(std::move(datacampOffer))
        .WithTitlePrefixForSpecificTypes("");

    auto offer = Process(
        FeedInfo,
        FeedShopInfo,
        MakeAtomicShared<IFeedParser::TMsg>(offerCarrier)
    );

    ASSERT_FALSE(offer->IsBasicIgnored());
    ASSERT_FALSE(offer->IsServiceIgnored());
    const auto& resultOriginalSpecification = offer->GetOriginalSpecification();
    ASSERT_TRUE(resultOriginalSpecification.type().value() == BOOKS);
    ASSERT_EQ(resultOriginalSpecification.name().value(), "???????????????? ?? ??????????????????");
}

TEST_F(TestOfferTypeMultiProcessor, AddAuthorToNameForBooks_NoAuthor) {
    FeedInfo.AllowBooksType = true;

    Offer datacampOffer;
    auto* originalSpecification = datacampOffer.mutable_content()->mutable_partner()->mutable_original();
    originalSpecification->mutable_type()->set_value(BOOKS);
    originalSpecification->mutable_name()->set_value("?????????????????? ???? ??????????????????");

    auto offerCarrier = TOfferCarrier(FeedInfo)
        .WithDataCampOffer(std::move(datacampOffer));

    auto offer = Process(
        FeedInfo,
        FeedShopInfo,
        MakeAtomicShared<IFeedParser::TMsg>(offerCarrier)
    );

    ASSERT_FALSE(offer->IsBasicIgnored());
    ASSERT_FALSE(offer->IsServiceIgnored());
    const auto& resultOriginalSpecification = offer->GetOriginalSpecification();
    ASSERT_TRUE(resultOriginalSpecification.type().value() == BOOKS);
    ASSERT_EQ(resultOriginalSpecification.name().value(), "?????????????????? ???? ??????????????????");
}

TEST_F(TestOfferTypeMultiProcessor, AddAuthorToNameForAudiobooks_FilledAuthor) {
    FeedInfo.AllowBooksType = true;

    Offer datacampOffer;
    auto* originalSpecification = datacampOffer.mutable_content()->mutable_partner()->mutable_original();
    originalSpecification->mutable_type()->set_value(AUDIOBOOKS);
    originalSpecification->mutable_name()->set_value("?????? ????????????");

    auto offerCarrier = TOfferCarrier(FeedInfo)
        .WithDataCampOffer(std::move(datacampOffer))
        .WithTitlePrefixForSpecificTypes("??. ??. ??????????");

    auto offer = Process(
        FeedInfo,
        FeedShopInfo,
        MakeAtomicShared<IFeedParser::TMsg>(offerCarrier)
    );

    ASSERT_FALSE(offer->IsBasicIgnored());
    ASSERT_FALSE(offer->IsServiceIgnored());
    const auto& resultOriginalSpecification = offer->GetOriginalSpecification();
    ASSERT_TRUE(resultOriginalSpecification.type().value() == AUDIOBOOKS);
    ASSERT_EQ(resultOriginalSpecification.name().value(), "??. ??. ?????????? \"?????? ????????????\"");
}

TEST_F(TestOfferTypeMultiProcessor, AddAuthorToNameForAudiobooks_EmptyAuthor) {
    FeedInfo.AllowBooksType = true;

    Offer datacampOffer;
    auto* originalSpecification = datacampOffer.mutable_content()->mutable_partner()->mutable_original();
    originalSpecification->mutable_type()->set_value(AUDIOBOOKS);
    originalSpecification->mutable_name()->set_value("???????????????? ?? ??????????????????");

    auto offerCarrier = TOfferCarrier(FeedInfo)
        .WithDataCampOffer(std::move(datacampOffer))
        .WithTitlePrefixForSpecificTypes("");

    auto offer = Process(
        FeedInfo,
        FeedShopInfo,
        MakeAtomicShared<IFeedParser::TMsg>(offerCarrier)
    );

    ASSERT_FALSE(offer->IsBasicIgnored());
    ASSERT_FALSE(offer->IsServiceIgnored());
    const auto& resultOriginalSpecification = offer->GetOriginalSpecification();
    ASSERT_TRUE(resultOriginalSpecification.type().value() == AUDIOBOOKS);
    ASSERT_EQ(resultOriginalSpecification.name().value(), "???????????????? ?? ??????????????????");
}

TEST_F(TestOfferTypeMultiProcessor, AddAuthorToNameForAudiobooks_NoAuthor) {
    FeedInfo.AllowBooksType = true;

    Offer datacampOffer;
    auto* originalSpecification = datacampOffer.mutable_content()->mutable_partner()->mutable_original();
    originalSpecification->mutable_type()->set_value(AUDIOBOOKS);
    originalSpecification->mutable_name()->set_value("?????????????????? ???? ??????????????????");

    auto offerCarrier = TOfferCarrier(FeedInfo)
        .WithDataCampOffer(std::move(datacampOffer));

    auto offer = Process(
        FeedInfo,
        FeedShopInfo,
        MakeAtomicShared<IFeedParser::TMsg>(offerCarrier)
    );

    ASSERT_FALSE(offer->IsIgnored());
    const auto& resultOriginalSpecification = offer->GetOriginalSpecification();
    ASSERT_TRUE(resultOriginalSpecification.type().value() == AUDIOBOOKS);
    ASSERT_EQ(resultOriginalSpecification.name().value(), "?????????????????? ???? ??????????????????");
}

TEST_F(TestOfferTypeMultiProcessor, AddArtistToNameForArtistTitle_FilledArtist) {
    FeedInfo.AllowArtistTitleType = true;

    Offer datacampOffer;
    auto* originalSpecification = datacampOffer.mutable_content()->mutable_partner()->mutable_original();
    originalSpecification->mutable_type()->set_value(ARTIST_TITLE);
    originalSpecification->mutable_name()->set_value("Never Gonna Give You Up");

    auto offerCarrier = TOfferCarrier(FeedInfo)
        .WithDataCampOffer(std::move(datacampOffer))
        .WithTitlePrefixForSpecificTypes("R. Astley");

    auto offer = Process(
        FeedInfo,
        FeedShopInfo,
        MakeAtomicShared<IFeedParser::TMsg>(offerCarrier)
    );

    ASSERT_FALSE(offer->IsBasicIgnored());
    ASSERT_FALSE(offer->IsServiceIgnored());
    const auto& resultOriginalSpecification = offer->GetOriginalSpecification();
    ASSERT_TRUE(resultOriginalSpecification.type().value() == ARTIST_TITLE);
    ASSERT_EQ(resultOriginalSpecification.name().value(), "R. Astley \"Never Gonna Give You Up\"");
}

TEST_F(TestOfferTypeMultiProcessor, AddArtistToNameForArtistTitle_EmptyArtist) {
    FeedInfo.AllowArtistTitleType = true;

    Offer datacampOffer;
    auto* originalSpecification = datacampOffer.mutable_content()->mutable_partner()->mutable_original();
    originalSpecification->mutable_type()->set_value(ARTIST_TITLE);
    originalSpecification->mutable_name()->set_value("Never Gonna Give You Up");

    auto offerCarrier = TOfferCarrier(FeedInfo)
        .WithDataCampOffer(std::move(datacampOffer))
        .WithTitlePrefixForSpecificTypes("");

    auto offer = Process(
        FeedInfo,
        FeedShopInfo,
        MakeAtomicShared<IFeedParser::TMsg>(offerCarrier)
    );

    ASSERT_FALSE(offer->IsBasicIgnored());
    ASSERT_FALSE(offer->IsServiceIgnored());
    const auto& resultOriginalSpecification = offer->GetOriginalSpecification();
    ASSERT_TRUE(resultOriginalSpecification.type().value() == ARTIST_TITLE);
    ASSERT_EQ(resultOriginalSpecification.name().value(), "Never Gonna Give You Up");
}

TEST_F(TestOfferTypeMultiProcessor, AddArtistToNameForArtistTitle_NoArtist) {
    FeedInfo.AllowArtistTitleType = true;

    Offer datacampOffer;
    auto* originalSpecification = datacampOffer.mutable_content()->mutable_partner()->mutable_original();
    originalSpecification->mutable_type()->set_value(ARTIST_TITLE);
    originalSpecification->mutable_name()->set_value("Never Gonna Give You Up");

    auto offerCarrier = TOfferCarrier(FeedInfo)
        .WithDataCampOffer(std::move(datacampOffer));

    auto offer = Process(
        FeedInfo,
        FeedShopInfo,
        MakeAtomicShared<IFeedParser::TMsg>(offerCarrier)
    );

    ASSERT_FALSE(offer->IsBasicIgnored());
    ASSERT_FALSE(offer->IsServiceIgnored());
    const auto& resultOriginalSpecification = offer->GetOriginalSpecification();
    ASSERT_TRUE(resultOriginalSpecification.type().value() == ARTIST_TITLE);
    ASSERT_EQ(resultOriginalSpecification.name().value(), "Never Gonna Give You Up");
}
