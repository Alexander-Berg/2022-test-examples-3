#include <market/idx/datacamp/miner/processors/offer_price_converter/converter.h>

#include <market/idx/datacamp/miner/lib/test_utils.h>

#include <market/idx/datacamp/proto/offer/DataCampOffer.pb.h>

#include <google/protobuf/util/time_util.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace NMiner;

namespace {
    const auto fixedTimestampSeconds = 1589833910;
    const auto fixedTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(fixedTimestampSeconds);
    const auto oldTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(327605478); // 1980
}

Y_UNIT_TEST_SUITE(PriceConverterTestSuite) {
    NMiner::TOfferPriceConverterConfig config("");
    auto converter = MakePriceConverter();

    Y_UNIT_TEST(ConvertOriginalVatToActual) {
        // проверяем, что хороший original_price_fields.vat появится в actual_price_fields, скрытий нет
        NMarket::TDatacampOfferBatchProcessingContext processingContext;
        auto dcOffer = MakeDefault();
        auto& offer = dcOffer.GetService();
        offer.mutable_price()->mutable_original_price_fields()->mutable_vat()->set_value(Market::DataCamp::VAT_18);
        auto converter = MakePriceConverter();
        converter->Process(dcOffer, processingContext, config, fixedTimestamp, Nothing());

        UNIT_ASSERT_EQUAL(offer.price().actual_price_fields().vat().value(), Market::DataCamp::VAT_18);
        CheckFlag(dcOffer, Market::DataCamp::DataSource::MARKET_IDX, false);
    }

    Y_UNIT_TEST(DoNotTouchActualIfEmpty) {
        // Важно не ставить .meta для actual_price_fields -- на время миграции
        NMarket::TDatacampOfferBatchProcessingContext processingContext;
        auto dcOffer = MakeDefault();
        auto& offer = dcOffer.GetService();
        auto converter = MakePriceConverter();
        converter->Process(dcOffer, processingContext, config, fixedTimestamp, Nothing());

        UNIT_ASSERT(not offer.price().actual_price_fields().vat().has_meta());
        CheckFlag(dcOffer, Market::DataCamp::DataSource::MARKET_IDX, false);
    }

    Y_UNIT_TEST(UseShopsDatIfAvailable) {
        // Если в original_price_fields есть meta, но нет данных, берем vat из shopsDat
        NMarket::TDatacampOfferBatchProcessingContext processingContext;
        auto dcOffer = MakeDefault();
        auto& offer = dcOffer.GetService();
        offer.mutable_price()->mutable_original_price_fields()->mutable_vat()->mutable_meta()->mutable_timestamp()->CopyFrom(fixedTimestamp);
        NMarket::NDataCamp::TSupplierRecord mbiInfo;
        mbiInfo.Vat = ::Market::DataCamp::Vat::VAT_10_110;
        auto converter = MakePriceConverter();
        TMaybe<NMarket::NDataCamp::TSupplierRecord> mbiInfoMaybe(mbiInfo);
        converter->Process(dcOffer, processingContext, config, fixedTimestamp, mbiInfoMaybe);

        UNIT_ASSERT_EQUAL(offer.price().actual_price_fields().vat().value(), ::Market::DataCamp::Vat::VAT_10_110);
        UNIT_ASSERT(offer.price().actual_price_fields().vat().has_meta());
        CheckFlag(dcOffer, Market::DataCamp::DataSource::MARKET_IDX, false);
    }

    Y_UNIT_TEST(CheckUSNWithOriginalVat) {
        // Если в original_price_fields для USN-магазина vat !=  NO_VAT, то меняем на NO_VAT и кидаем предупреждение
        NMarket::TDatacampOfferBatchProcessingContext processingContext;
        auto dcOffer = MakeDefault();
        auto& offer = dcOffer.GetService();
        offer.mutable_price()->mutable_original_price_fields()->mutable_vat()->set_value(::Market::DataCamp::Vat::VAT_20);
        offer.mutable_price()->mutable_original_price_fields()->mutable_vat()->mutable_meta()->mutable_timestamp()->CopyFrom(fixedTimestamp);
        NMarket::NDataCamp::TSupplierRecord mbiInfo;
        mbiInfo.Vat = ::Market::DataCamp::Vat::VAT_10_110;
        mbiInfo.TaxSystem = NMarket::NTaxes::EShopTaxSystem::USN;
        auto converter = MakePriceConverter();
        TMaybe<NMarket::NDataCamp::TSupplierRecord> mbiInfoMaybe(mbiInfo);
        converter->Process(dcOffer, processingContext, config, fixedTimestamp, mbiInfoMaybe);

        UNIT_ASSERT_EQUAL(offer.price().actual_price_fields().vat().value(), ::Market::DataCamp::Vat::NO_VAT);
        UNIT_ASSERT(offer.price().actual_price_fields().vat().has_meta());
    }

    Y_UNIT_TEST(CheckUSNWithShopsDatVat) {
        // Если в vat шопсдаты для USN-магазина vat !=  NO_VAT, то меняем на NO_VAT и кидаем предупреждение
        NMarket::TDatacampOfferBatchProcessingContext processingContext;
        auto dcOffer = MakeDefault();
        auto& offer = dcOffer.GetService();
        offer.mutable_price()->mutable_original_price_fields()->mutable_vat()->mutable_meta()->mutable_timestamp()->CopyFrom(fixedTimestamp);
        NMarket::NDataCamp::TSupplierRecord mbiInfo;
        mbiInfo.Vat = ::Market::DataCamp::Vat::VAT_10_110;
        mbiInfo.TaxSystem = NMarket::NTaxes::EShopTaxSystem::USN;
        auto converter = MakePriceConverter();
        TMaybe<NMarket::NDataCamp::TSupplierRecord> mbiInfoMaybe(mbiInfo);
        converter->Process(dcOffer, processingContext, config, fixedTimestamp, mbiInfoMaybe);

        UNIT_ASSERT_EQUAL(offer.price().actual_price_fields().vat().value(), ::Market::DataCamp::Vat::NO_VAT);
        UNIT_ASSERT(offer.price().actual_price_fields().vat().has_meta());
    }


    Y_UNIT_TEST(CheckUSNWithoutAnyVat) {
        // Кейс, когда нет вата ни в original-части, ни в шопсдате
        NMarket::TDatacampOfferBatchProcessingContext processingContext;
        auto dcOffer = MakeDefault();
        auto& offer = dcOffer.GetService();
        offer.mutable_price()->mutable_original_price_fields()->mutable_vat()->mutable_meta()->mutable_timestamp()->CopyFrom(fixedTimestamp);
        NMarket::NDataCamp::TSupplierRecord mbiInfo;
        mbiInfo.TaxSystem = NMarket::NTaxes::EShopTaxSystem::USN;
        auto converter = MakePriceConverter();
        TMaybe<NMarket::NDataCamp::TSupplierRecord> mbiInfoMaybe(mbiInfo);
        converter->Process(dcOffer, processingContext, config, fixedTimestamp, mbiInfoMaybe);

        UNIT_ASSERT_VALUES_UNEQUAL(offer.price().actual_price_fields().vat().meta().timestamp().seconds(), fixedTimestampSeconds);
    }

    Y_UNIT_TEST(UpdateOnTimestamp) {
        // Если в original_price_fields есть meta, но нет данных, берем vat из shopsDat
        NMarket::TDatacampOfferBatchProcessingContext processingContext;
        auto dcOffer = MakeDefault();
        auto& offer = dcOffer.GetService();
        offer.mutable_price()->mutable_actual_price_fields()->mutable_vat()->set_value(::Market::DataCamp::Vat::VAT_20);
        offer.mutable_price()->mutable_actual_price_fields()->mutable_vat()->mutable_meta()->mutable_timestamp()->CopyFrom(oldTimestamp);
        offer.mutable_price()->mutable_original_price_fields()->mutable_vat()->mutable_meta()->mutable_timestamp()->CopyFrom(fixedTimestamp);
        NMarket::NDataCamp::TSupplierRecord mbiInfo;
        mbiInfo.Vat = ::Market::DataCamp::Vat::VAT_10_110;
        auto converter = MakePriceConverter();
        TMaybe<NMarket::NDataCamp::TSupplierRecord> mbiInfoMaybe(mbiInfo);
        converter->Process(dcOffer, processingContext, config, fixedTimestamp, mbiInfoMaybe);

        UNIT_ASSERT_EQUAL(offer.price().actual_price_fields().vat().value(), ::Market::DataCamp::Vat::VAT_10_110);
        UNIT_ASSERT(offer.price().actual_price_fields().vat().has_meta());
        CheckFlag(dcOffer, Market::DataCamp::DataSource::MARKET_IDX, false);
    }

    Y_UNIT_TEST(CheckUSNWithValidVat) {
        // Если в vat шопсдаты для USN-магазина vat = NO_VAT, то оставляем NO_VAT
        NMarket::TDatacampOfferBatchProcessingContext processingContext;
        auto dcOffer = MakeDefault();
        auto& offer = dcOffer.GetService();
        offer.mutable_price()->mutable_original_price_fields()->mutable_vat()->mutable_meta()->mutable_timestamp()->CopyFrom(fixedTimestamp);
        NMarket::NDataCamp::TSupplierRecord mbiInfo;
        mbiInfo.Vat = ::Market::DataCamp::Vat::NO_VAT;
        mbiInfo.TaxSystem = NMarket::NTaxes::EShopTaxSystem::USN;
        auto converter = MakePriceConverter();
        TMaybe<NMarket::NDataCamp::TSupplierRecord> mbiInfoMaybe(mbiInfo);
        converter->Process(dcOffer, processingContext, config, fixedTimestamp, mbiInfoMaybe);

        UNIT_ASSERT_EQUAL(offer.price().actual_price_fields().vat().value(), ::Market::DataCamp::Vat::NO_VAT);
        UNIT_ASSERT(offer.price().actual_price_fields().vat().has_meta());
    }

    Y_UNIT_TEST(CheckUSN_MINUS_COSTWithOriginalVat) {
        // Если в original_price_fields для USN_MINUS_COST-магазина vat !=  NO_VAT, то меняем на NO_VAT и кидаем предупреждение
        NMarket::TDatacampOfferBatchProcessingContext processingContext;
        auto dcOffer = MakeDefault();
        auto& offer = dcOffer.GetService();
        offer.mutable_price()->mutable_original_price_fields()->mutable_vat()->set_value(::Market::DataCamp::Vat::VAT_20);
        offer.mutable_price()->mutable_original_price_fields()->mutable_vat()->mutable_meta()->mutable_timestamp()->CopyFrom(fixedTimestamp);
        NMarket::NDataCamp::TSupplierRecord mbiInfo;
        mbiInfo.Vat = ::Market::DataCamp::Vat::VAT_10_110;
        mbiInfo.TaxSystem = NMarket::NTaxes::EShopTaxSystem::USN_MINUS_COST;
        auto converter = MakePriceConverter();
        TMaybe<NMarket::NDataCamp::TSupplierRecord> mbiInfoMaybe(mbiInfo);
        converter->Process(dcOffer, processingContext, config, fixedTimestamp, mbiInfoMaybe);

        UNIT_ASSERT_EQUAL(offer.price().actual_price_fields().vat().value(), ::Market::DataCamp::Vat::NO_VAT);
        UNIT_ASSERT(offer.price().actual_price_fields().vat().has_meta());
    }

    Y_UNIT_TEST(CheckUSN_MINUS_COSTWithShopsDatVat) {
        // Если в vat шопсдаты для USN_MINUS_COST-магазина vat !=  NO_VAT, то меняем на NO_VAT и кидаем предупреждение
        NMarket::TDatacampOfferBatchProcessingContext processingContext;
        auto dcOffer = MakeDefault();
        auto& offer = dcOffer.GetService();
        offer.mutable_price()->mutable_original_price_fields()->mutable_vat()->mutable_meta()->mutable_timestamp()->CopyFrom(fixedTimestamp);
        NMarket::NDataCamp::TSupplierRecord mbiInfo;
        mbiInfo.Vat = ::Market::DataCamp::Vat::VAT_10_110;
        mbiInfo.TaxSystem = NMarket::NTaxes::EShopTaxSystem::USN_MINUS_COST;
        auto converter = MakePriceConverter();
        TMaybe<NMarket::NDataCamp::TSupplierRecord> mbiInfoMaybe(mbiInfo);
        converter->Process(dcOffer, processingContext, config, fixedTimestamp, mbiInfoMaybe);

        UNIT_ASSERT_EQUAL(offer.price().actual_price_fields().vat().value(), ::Market::DataCamp::Vat::NO_VAT);
        UNIT_ASSERT(offer.price().actual_price_fields().vat().has_meta());
    }


    Y_UNIT_TEST(CheckUSN_MINUS_COSTWithoutAnyVat) {
        // Кейс, когда нет вата ни в original-части, ни в шопсдате (USN_MINUS_COST)
        NMarket::TDatacampOfferBatchProcessingContext processingContext;
        auto dcOffer = MakeDefault();
        auto& offer = dcOffer.GetService();
        offer.mutable_price()->mutable_original_price_fields()->mutable_vat()->mutable_meta()->mutable_timestamp()->CopyFrom(fixedTimestamp);
        NMarket::NDataCamp::TSupplierRecord mbiInfo;
        mbiInfo.TaxSystem = NMarket::NTaxes::EShopTaxSystem::USN_MINUS_COST;
        auto converter = MakePriceConverter();
        TMaybe<NMarket::NDataCamp::TSupplierRecord> mbiInfoMaybe(mbiInfo);
        converter->Process(dcOffer, processingContext, config, fixedTimestamp, mbiInfoMaybe);

        UNIT_ASSERT_VALUES_UNEQUAL(offer.price().actual_price_fields().vat().meta().timestamp().seconds(), fixedTimestampSeconds);
    }


    Y_UNIT_TEST(CheckUSN_MINUS_COSTWithValidVat) {
        // Если в vat шопсдаты для USN_MINUS_COST-магазина vat = NO_VAT, то оставляем NO_VAT
        NMarket::TDatacampOfferBatchProcessingContext processingContext;
        auto dcOffer = MakeDefault();
        auto& offer = dcOffer.GetService();
        offer.mutable_price()->mutable_original_price_fields()->mutable_vat()->mutable_meta()->mutable_timestamp()->CopyFrom(fixedTimestamp);
        NMarket::NDataCamp::TSupplierRecord mbiInfo;
        mbiInfo.Vat = ::Market::DataCamp::Vat::NO_VAT;
        mbiInfo.TaxSystem = NMarket::NTaxes::EShopTaxSystem::USN_MINUS_COST;
        auto converter = MakePriceConverter();
        TMaybe<NMarket::NDataCamp::TSupplierRecord> mbiInfoMaybe(mbiInfo);
        converter->Process(dcOffer, processingContext, config, fixedTimestamp, mbiInfoMaybe);

        UNIT_ASSERT_EQUAL(offer.price().actual_price_fields().vat().value(), ::Market::DataCamp::Vat::NO_VAT);
        UNIT_ASSERT(offer.price().actual_price_fields().vat().has_meta());
    }

    Y_UNIT_TEST(CreateOriginalPriceFieldsIfNoBasicVat) {
        // Миграция: ставим original_price_fields.meta, если price.basic.vat пустой
        NMarket::TDatacampOfferBatchProcessingContext processingContext;
        auto dcOffer = MakeDefault();
        auto& offer = dcOffer.GetService();
        offer.mutable_price()->mutable_basic()->mutable_binary_price()->set_price(12340000);
        auto converter = MakePriceConverter();
        converter->Process(dcOffer, processingContext, config, fixedTimestamp, Nothing());

        UNIT_ASSERT(offer.price().original_price_fields().vat().meta().has_timestamp());
        UNIT_ASSERT(offer.price().original_price_fields().vat().meta().has_applier());
    }
}
