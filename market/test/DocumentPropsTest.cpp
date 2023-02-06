#include <market/report/library/base_search_document_props/BaseSearchDocumentProps.h>

#include <market/library/fixed_point_number/fixed_point_number.h>
#include <market/library/glparams/legacy_gl_mbo_reader.h>
#include <market/library/currency_exchange/currencies.h>
#include <market/library/libpromo/reader.h>
#include <market/library/libsku/reader.h>
#include <market/library/exception/exception.h>

#include <fstream>
#include <util/generic/string.h>

#include <market/report/library/offer_to_cmagic/offer_to_cmagic.h>
#include <market/report/test/currency_exchange_fake.h>
#include <market/report/test/document_props_fake.h>

#include <kernel/groupattrs/docsattrs.h>
#include <kernel/dssm_applier/decompression/decompression.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

using namespace NMarketReport;
using namespace Market::NCurrency;
using namespace GuruLightSC3;


TBaseSearchDocumentPropsPtr CreateDocumentPropsFake() {
    const TString basePropsFbPath = SRC_("./TestData/base-offer-props.fb").data();
    const TString basePropsExtFbPath = SRC_("./TestData/base-offer-props-ext.fb64").data();
    const TString businessOffersPath = SRC_("./TestData/businnes_offers.fb64").data();
    const TString offersRegionsSourcePath = SRC_("./TestData/offer-orig-regions-literals-source.fb").data();
    const TString expressHyperlocalServiceOffersPath = SRC_("./TestData/express_hyperlocal_service_offers.fb64").data();
    const TString bidFlagsPath = SRC_("./TestData/bids.flags.report.binary").data();
    const TString autostrategiesPath = SRC_("./TestData/amore_data.fb").data();
    const TString vatPath = SRC_("./TestData/vat_props.values.binary").data();
    const TString glScPath = SRC_("./TestData/gl_sc.mmap").data();
    const TString wareMD5Path = SRC_("./TestData/ware_md5.values.binary").data();
    const TString vendorBidPath = SRC_("./TestData/vendor.values.binary").data();
    const TString dssmEmbeddingPath = SRC_("./TestData/dssm.values.binary").data();
    const TString hardDssmEmbeddingPath = SRC_("./TestData/hard_dssm.values.binary").data();
    const TString hard2DssmEmbeddingPath = SRC_("./TestData/hard2_dssm.values.binary").data();
    const TString reformulationDssmEmbeddingPath = SRC_("./TestData/reformulation_dssm.values.binary").data();
    const TString bertDssmEmbeddingPath = SRC_("./TestData/bert_dssm.values.binary").data();
    const TString assessmentBinaryEmbeddingPath = SRC_("./TestData/assessment_binary.values.binary").data();
    const TString picturesDssmPrefix = SRC_("./TestData/image_i2t_v12_dssm").data();
    const TString omniWadPath = SRC_("./TestData/omni.wad").data();
    const TString offersDeliveryInfoFbPath = SRC_("./TestData/offers-delivery-info.fb").data();
    const TString offersHashMappingFbPath = SRC_("./TestData/offers-hash-mapping.fb").data();
    const TString localDeliveryPath = SRC_("./TestData/local_delivery.mmap").data();
    const TString yndexPath = SRC_("./TestData/index").data();

    TCurrencyIdentities currencies = GetCurrenciesFake();

    const TestCurrencyExchange exch;
    const size_t precision = 2;
    GuruLightMBO2::TParams mboParams;
    THolder<NGlMbo::IReader> glMboReader = CreateLegacyGlMboReader(mboParams);
    static NMarketReport::TOfferToCMagicFactory offerToCMagicFactory;
    static TReportConfig::TIndexCollection fakeCollectionConfig(0, TReportConfig::TIndexCollection::EType::Unknown, "fake", "fake", "fake");
    return TBaseSearchDocumentPropsPtr(new TBaseSearchDocumentProps(
        exch,
        currencies,
        basePropsFbPath,
        basePropsExtFbPath,
        businessOffersPath,
        offersRegionsSourcePath,
        expressHyperlocalServiceOffersPath,
        bidFlagsPath,
        autostrategiesPath,
        vatPath,
        precision,
        wareMD5Path,
        vendorBidPath,
        dssmEmbeddingPath,
        hard2DssmEmbeddingPath,
        bertDssmEmbeddingPath,
        reformulationDssmEmbeddingPath,
        assessmentBinaryEmbeddingPath,
        picturesDssmPrefix,
        glScPath,
        Nothing(),
        Nothing(),
        *glMboReader,
        Market::CreateLocalDeliveryReader(localDeliveryPath),
        THolder<Market::NDelivery::IOfferDeliveryBucketsReader>(),
        Market::NDelivery::IOfferDeliveryInfoReader::Load(offersDeliveryInfoFbPath, offersHashMappingFbPath),
        THolder<NMarket::NPromo::IOfferPromoInfoReader>(),
        THolder<NMarket::NSKU::IOfferSKUReader>(),
        THolder<NMarket::NDimension::IOfferDimensionsReader>(),
        THolder<Market::NBookNow::IOfferBookingInfoReader>(),
        0, true, "", "", "", "", "", "", "", "", "",
        fakeCollectionConfig,
        nullptr,
        offerToCMagicFactory,
        "",
        yndexPath,
        false,
        omniWadPath,
        "",
        ""
    ));
}

TEST(DocumentPropsTest, TestDocumentProps) {
    auto propsFake = CreateDocumentPropsFake();
    TBaseSearchDocumentProps& props = *propsFake;

    const size_t currencyIdRur = 0UL;
    const size_t currencyIdKzt = 2UL;

    EXPECT_EQ(2U, props.GetPrecision());
    EXPECT_EQ(0U, props.GetCurrencyIndex(TCurrency::Rur()));
    EXPECT_EQ(1U, props.GetCurrencyIndex(TCurrency::Byr()));
    EXPECT_EQ(2U, props.GetCurrencyIndex(TCurrency::Kzt()));
    EXPECT_EQ(3U, props.GetCurrencyIndex(TCurrency::Uah()));
    EXPECT_EQ(4U, props.GetCurrencyIndex(TCurrency::Ue()));
    ASSERT_THROW(props.GetCurrencyIndex(TCurrency::Usd()), TReportException);

    TMaybe<TFixedPointNumber> price;

    // The first doc
    ASSERT_NO_THROW(price = props.GetPrice(0, currencyIdRur));
    EXPECT_EQ(1'000'000'000, price.GetRef().AsRaw());

    ASSERT_NO_THROW(price = props.GetPrice(0, currencyIdKzt));
    EXPECT_EQ(5'000'000'000, price.GetRef().AsRaw());

    ASSERT_NO_THROW(price = props.GetOldPrice(0, currencyIdRur));
    EXPECT_EQ(1'000'000'000, price.GetRef().AsRaw());

    ASSERT_NO_THROW(price = props.GetOldPrice(0, currencyIdKzt));
    EXPECT_EQ(5'000'000'000, price.GetRef().AsRaw());

    uint64_t flags = 0;
    uint64_t expected_flags = (1 << 29) + (1 << 24) + (1 << 3);
    ASSERT_NO_THROW(flags = props.GetFlags64(0));
    EXPECT_EQ(expected_flags, flags);

    auto dssmEmbedding = props.GetDefaultDssm().GetDssmEmbedding(0);
    auto hard2DssmEmbedding = props.GetHard2Dssm().GetDssmEmbedding(0);
    const auto& charToFloat = NDssmApplier::GetDecompression(EDssmCompression::MarketMiddleClick);
    const auto compressFactor = 127.0 / 0.96;
    for (size_t i = 0; i < DSSM_EMBEDDING_SIZE; ++i) {
        ui8 compressedValue = i + 1;
        // обычный dssmEmbedding проходит декомпрессию
        EXPECT_EQ(charToFloat[compressedValue], dssmEmbedding[i]);
        // Scale умножает на compressFactor, unscale делит на compressFactor
        EXPECT_NEAR(compressedValue/compressFactor, hard2DssmEmbedding[i], 0.01);
    }
}

TEST(DocumentPropsTest, TestOffersDeliveryInfo) {
    const auto propsFake = CreateDocumentPropsFake();
    TBaseSearchDocumentProps& props = *propsFake;

    const auto offerDeliveryInfo = props.GetOfferDeliveryInfoReader(false);
    EXPECT_TRUE(offerDeliveryInfo);

    NMarketReport::TBucketInfoList buckets;
    ASSERT_NO_THROW(buckets = offerDeliveryInfo->GetCourierInfo({0}));
    EXPECT_TRUE(buckets);
    EXPECT_EQ(10, buckets[0].BucketId);
    EXPECT_EQ(100, buckets[0].CostModifiers[0]);
    EXPECT_EQ(200, buckets[0].TimeModifiers[0]);
    EXPECT_EQ(300, buckets[0].ServicesModifiers[0]);
    EXPECT_EQ(400, buckets[0].RegionAvailabilityModifiers[0]);
    EXPECT_EQ(true, buckets[0].IsNewBucket);

    ASSERT_NO_THROW(buckets = offerDeliveryInfo->GetPickupInfo({0}));
    EXPECT_TRUE(buckets);
    EXPECT_EQ(11, buckets[0].BucketId);
    EXPECT_EQ(false, buckets[0].IsNewBucket);

    ASSERT_NO_THROW(buckets = offerDeliveryInfo->GetPostInfo({0}));
    EXPECT_TRUE(buckets);
    EXPECT_EQ(12, buckets[0].BucketId);
    EXPECT_EQ(false, buckets[0].IsNewBucket);

    const Market::NDelivery::TPickupOptionVec* options;
    ASSERT_NO_THROW(options = offerDeliveryInfo->GetPickupOptions({0}));
    EXPECT_TRUE(options);
    EXPECT_EQ(0, options->Get(0)->PriceValue());
    EXPECT_EQ(1, options->Get(0)->DaysMin());
    EXPECT_EQ(3, options->Get(0)->DaysMax());
    EXPECT_EQ(21, options->Get(0)->OrderBeforeHour());
}

TEST(DocumentPropsTest, TestOffersHashMapping) {
    const auto propsFake = CreateDocumentPropsFake();
    TBaseSearchDocumentProps& props = *propsFake;

    const auto offerDeliveryInfo = props.GetOfferDeliveryInfoReader(false);
    EXPECT_TRUE(offerDeliveryInfo);

    IOfferDeliveryInfoReader::TOfferOffset offset;
    ASSERT_NO_THROW(offset = offerDeliveryInfo->GetOfferOffset(1234));
    EXPECT_TRUE(offset.v.Defined());
    EXPECT_EQ(0, *offset.v);

    ASSERT_NO_THROW(offset = offerDeliveryInfo->GetOfferOffset(2345));
    EXPECT_TRUE(offset.v.Defined());
    EXPECT_EQ(1, *offset.v);

    ASSERT_NO_THROW(offset = offerDeliveryInfo->GetOfferOffset(100500));
    EXPECT_FALSE(offset.v.Defined());
}

TEST(DocumentPropsTest, TestDocumentPropsManager) {
    NMarketReport::TReportConfig::TIndexCollections collections;

    const TestCurrencyExchange exch {};
    TCurrencyIdentities currencies = GetCurrenciesFake();
    NMarketReport::TPriceConverter priceConverter(exch, currencies);
    GuruLightMBO2::TParams mboParams;
    THolder<NGlMbo::IReader> glMboReader = CreateLegacyGlMboReader(mboParams);
    NMarketReport::TOfferToCMagicFactory offerToCMagicFactory;
    TBaseSearchDocumentPropsManager manager(exch, priceConverter, collections, *glMboReader, false,
                                            SRC_("./TestData/regional_delivery.mmap"),
                                            SRC_("./TestData/regional_delivery_buckets2.fb"),
                                            SRC_("./TestData/regional_delivery_modifiers.fb"),
                                            offerToCMagicFactory, false);

    ASSERT_THROW(manager.GetProps(1), std::exception);
}

TEST(DocumentPropsTest, TestVatProps) {
    auto propsFake = CreateDocumentPropsFake();
    TBaseSearchDocumentProps& props = *propsFake;

    TMaybe<NMarket::NTaxes::EVat> vat;

    ASSERT_NO_THROW(vat = props.GetVat(0));
    EXPECT_FALSE(vat.Defined());

    ASSERT_NO_THROW(vat = props.GetVat(1));
    EXPECT_TRUE(vat.Defined());
    EXPECT_EQ(NMarket::NTaxes::EVat::VAT_10, *vat);
}
