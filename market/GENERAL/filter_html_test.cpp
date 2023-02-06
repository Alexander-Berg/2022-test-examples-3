#include <market/idx/datacamp/miner/processors/offer_content_converter/converter.h>
#include <market/idx/datacamp/miner/processors/offer_content_converter/processor.h>

#include <market/idx/datacamp/miner/lib/test_utils.h>

#include <market/idx/datacamp/proto/offer/DataCampOffer.pb.h>

#include <market/proto/common/common.pb.h>

#include <google/protobuf/util/time_util.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace NMiner;

Y_UNIT_TEST_SUITE(FilterHtmlTestSuite) {
    void Convert(NMiner::TDatacampOffer &offer, const NMiner::TOfferContentConverterConfig &config,
                 const google::protobuf::Timestamp &fixedTimestamp) {
        NMarket::TDatacampOfferBatchProcessingContext processingContext;
        NMarket::NCapsLock::TCapsLockFixer capsFixer;
        auto converter = NWhite::MakeContentConverter(capsFixer);
        converter->Process(offer, processingContext, config, fixedTimestamp);
    }

    NMiner::TOfferContentConverterConfig config("");
    const auto &fixedTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589833910);
    const auto &oldTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(327605478); // 1980

    void TestDescriptionCore(const TString& originalDescription, const TString& actualExpectedDescription) {
        NMiner::TDatacampOffer dcOffer = MakeDefault();
        auto& offer = dcOffer.GetBasicByColor();

        auto original = offer.mutable_content()->mutable_partner()->mutable_original();
        offer.mutable_partner_info()->set_direct_product_mapping(true); // чтобы не получить disable из-за урла
        original->mutable_description()->set_value(originalDescription);

        Convert(dcOffer, config, fixedTimestamp);

        UNIT_ASSERT(offer.content().partner().actual().description().has_value());
        UNIT_ASSERT_EQUAL(offer.content().partner().actual().description().value(), actualExpectedDescription);
    }

    Y_UNIT_TEST(FilterHtmlTestDescriptionNonHtml) {
        TestDescriptionCore("Mobile    phone &lt; 0001 &gt;", "Mobile phone < 0001 >");
    }

    Y_UNIT_TEST(FilterHtmlTestDescriptionNonHtmlNewLineEnding) {
        TestDescriptionCore("Mobile phone\n", "Mobile phone");
    }

    Y_UNIT_TEST(FilterHtmlTestDescriptionHtml) {
        TestDescriptionCore("<p>Mobile phone &lt;\n 0001 &gt;</p>", "<p>Mobile phone <<br /> 0001 ></p>");
    }

    Y_UNIT_TEST(FilterHtmlTestCategory) {
        NMiner::TDatacampOffer dcOffer = MakeDefault();
        auto& offer = dcOffer.GetBasicByColor();

        auto original = offer.mutable_content()->mutable_partner()->mutable_original();
        auto actual = offer.mutable_content()->mutable_partner()->mutable_actual();
        auto originalCategory = original->mutable_category();
        auto actualCategory = actual->mutable_category();
        originalCategory->set_id(10);
        originalCategory->set_name("   <p> Some wierd name| yes </p>");
        originalCategory->set_parent_id(11);
        originalCategory->set_path_category_ids("<p>123 </p>\\88\\<p> 22</p>");
        originalCategory->set_path_category_names("<p>Some name | yes</p>\\Other name\\<p> Need strip name </p>");
        actualCategory->set_id(10);
        actualCategory->set_name("   <p> Some wierd name| yes </p>");
        actualCategory->set_parent_id(11);
        actualCategory->set_path_category_ids("<p>123 </p>\\88\\<p> 22</p>");
        actualCategory->set_path_category_names("<p>Some name | yes</p>\\Other name\\<p> Need strip name </p>");

        Convert(dcOffer, config, fixedTimestamp);

        UNIT_ASSERT(offer.content().partner().actual().has_category());
        const auto& actualCategory1 = offer.content().partner().actual().category();
        UNIT_ASSERT_EQUAL(actualCategory1.id(), 10);
        UNIT_ASSERT_EQUAL(actualCategory1.name(), "Some wierd name  yes");
        UNIT_ASSERT_EQUAL(actualCategory1.parent_id(), 11);
        UNIT_ASSERT_EQUAL(actualCategory1.path_category_ids(), "123\\88\\22");
        UNIT_ASSERT_EQUAL(actualCategory1.path_category_names(), "Some name   yes\\Other name\\Need strip name");
    }
}
