#include <market/idx/datacamp/miner/processors/offer_content_converter/converter.h>
#include <market/idx/datacamp/miner/processors/offer_content_converter/processor.h>

#include <market/idx/datacamp/miner/lib/test_utils.h>

#include <market/idx/datacamp/proto/offer/DataCampOffer.pb.h>

#include <google/protobuf/util/time_util.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace NMiner;

Y_UNIT_TEST_SUITE(ContentConverter) {
        Y_UNIT_TEST(PartnerContentCopier) {
            // проверяем, что параметры, задаваемые qparser-ом в original-части,
            // будут скопированы в actual-часть
            for (bool unitedCatalogStatus : {false, true}) {
                NMiner::TOfferContentConverterConfig config("");
                const auto& fixedTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589833910);
                i64 seconds = 327605478;
                const auto& oldTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(seconds);
                NMarket::TDatacampOfferBatchProcessingContext processingContext;


                NMiner::TDatacampOffer dcOffer = MakeDefault();
                dcOffer.mutable_status()->mutable_united_catalog()->set_flag(unitedCatalogStatus);

                auto& offer = unitedCatalogStatus ? dcOffer.GetBasic() : dcOffer.GetService();
                auto original = offer.mutable_content()->mutable_partner()->mutable_original();
                auto fillFieldTimestamp = [&oldTimestamp](auto& proto) {
                    proto.mutable_meta()->mutable_timestamp()->CopyFrom(oldTimestamp);
                    proto.mutable_meta()->set_source(Market::DataCamp::DataSource::PUSH_PARTNER_FEED);
                };

                // заполняем original-поля
                original->mutable_name()->set_value("name");
                fillFieldTimestamp(*(original->mutable_name()));
                original->mutable_vendor()->set_value("vendor");
                fillFieldTimestamp(*(original->mutable_vendor()));
                original->mutable_manufacturer_warranty()->set_flag(true);
                fillFieldTimestamp(*(original->mutable_manufacturer_warranty()));
                original->mutable_weight()->set_grams(200 * 1000);
                fillFieldTimestamp(*(original->mutable_weight()));
                original->mutable_dimensions()->set_length_mkm(10 * 10000);
                original->mutable_dimensions()->set_width_mkm(20 * 10000);
                original->mutable_dimensions()->set_height_mkm(30 * 10000);
                fillFieldTimestamp(*(original->mutable_dimensions()));
                original->mutable_type()->set_value(NMarket::EProductType::BOOKS);
                fillFieldTimestamp(*(original->mutable_type()));

                NMarket::NCapsLock::TCapsLockFixer capsFixer;
                auto converter = NBlue::MakeContentConverter(capsFixer);
                converter->Process(dcOffer, processingContext, config, fixedTimestamp);

                // проверяем, что поля скопировались в actual-часть
                auto actual = offer.content().partner().actual();
                auto checkMeta = [seconds](auto& proto) {
                    UNIT_ASSERT_EQUAL(seconds, proto.meta().timestamp().seconds());
                    UNIT_ASSERT_EQUAL(Market::DataCamp::DataSource::PUSH_PARTNER_FEED, proto.meta().source());
                };

                UNIT_ASSERT(!actual.has_title());
                UNIT_ASSERT_EQUAL("vendor", actual.vendor().value());
                checkMeta(actual.vendor());
                UNIT_ASSERT_EQUAL(true, actual.manufacturer_warranty().flag());
                checkMeta(actual.manufacturer_warranty());
                UNIT_ASSERT_EQUAL(200 * 1000, actual.weight().grams());
                checkMeta(actual.weight());
                UNIT_ASSERT_EQUAL(10 * 10000, actual.dimensions().length_mkm());
                UNIT_ASSERT_EQUAL(20 * 10000, actual.dimensions().width_mkm());
                UNIT_ASSERT_EQUAL(30 * 10000, actual.dimensions().height_mkm());
                checkMeta(actual.dimensions());
                UNIT_ASSERT_EQUAL(NMarket::EProductType::BOOKS, actual.type().value());
                checkMeta(actual.type());
            }
        }

}
