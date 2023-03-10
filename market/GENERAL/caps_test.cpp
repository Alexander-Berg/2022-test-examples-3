#include <market/idx/datacamp/miner/lib/cms_promo_reader.h>
#include <market/idx/datacamp/miner/lib/test_utils.h>
#include <market/idx/datacamp/miner/processors/offer_content_converter/processor.h>
#include <market/idx/datacamp/miner/processors/offer_content_converter/config.h>
#include <market/idx/datacamp/proto/offer/DataCampOffer.pb.h>

#include <contrib/libs/protobuf/src/google/protobuf/util/time_util.h>

#include <kernel/common_proxy/server/config.h>
#include <kernel/daemon/config/config_constructor.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/folder/path.h>
#include <util/stream/fwd.h>


namespace {
    NMarket::TDatacampOfferBatchProcessingContext context {};

    const auto& timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589423910);
    const auto& fixedTimestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(1589833910);

    TString CONFIG_PATH = "config";
    TString CAPSLOCK_EXCEPTIONS_PATH = JoinFsPaths(
        ArcadiaSourceRoot(),
        "market/idx/datacamp/miner/processors/offer_content_converter/ut/data/caps-exceptions.txt"
    );
    TString CAPSLOCK_MUST_FIX_PATH = JoinFsPaths(
        ArcadiaSourceRoot(),
        "market/idx/datacamp/miner/processors/offer_content_converter/ut/data/caps-must-fix.txt"
    );

    TString MakeTestConfig(const TString& caps_exceptions, const TString& caps_must_fix) {
        return
            "<DaemonConfig>\n"
            "    LogLevel: 6\n"
            "</DaemonConfig>\n"
            "<UnistatSignals>\n"
            "    Prefix: EMPTY\n"
            "</UnistatSignals>\n"
            "<Proxy>\n"
            "    <Processors>\n"
            "        <TestOfferContentConverter>\n"
            "            Type: OFFER_CONTENT_CONVERTER\n"
            "            Threads: 1\n"
            "            MaxQueue: 50\n"
            "            Color: white\n"
            "            CapslockExceptionsPath: " + caps_exceptions + "\n"
            "            CapslockMustFixPath: " + caps_must_fix + "\n"
            "        </TestOfferContentConverter>\n"
            "    </Processors>\n"
            "</Proxy>\n";
    };

    Market::DataCamp::Offer& AddOfferToBatch(NMiner::TDatacampOfferBatch& batch) {
        auto& offer = batch.add_offer()->GetBasicByUnitedCatalogStatus();
        offer.mutable_partner_info()->set_direct_product_mapping(true); // ?????????? ???? ???????????????? disable ????-???? ????????
        return offer;
    }

    Market::DataCamp::Offer& AddOfferToBatchWithName(NMiner::TDatacampOfferBatch& batch, const TString& name) {
        auto& offer = AddOfferToBatch(batch);
        offer.mutable_content()->mutable_partner()->mutable_original()->mutable_name()->set_value(name);
        return offer;
    }

    Market::DataCamp::Offer& AddOfferToBatchWithDescription(NMiner::TDatacampOfferBatch& batch, const TString& description) {
        auto& offer = AddOfferToBatch(batch);
        offer.mutable_content()->mutable_partner()->mutable_original()->mutable_description()->set_value(description);
        return offer;
    }

    Market::DataCamp::Offer& AddOfferToBatchWithSalesNotes(NMiner::TDatacampOfferBatch& batch, const TString& salesNotes) {
        auto& offer = AddOfferToBatch(batch);
        offer.mutable_content()->mutable_partner()->mutable_original_terms()->mutable_sales_notes()->set_value(salesNotes);
        return offer;
    }
}


class OfferContentConverterTest: public NUnitTest::TBaseFixture {
public:
    OfferContentConverterTest() {
        const auto config = MakeTestConfig(CAPSLOCK_EXCEPTIONS_PATH, CAPSLOCK_MUST_FIX_PATH);
        TServerConfigConstructorParams params(config.data());
        Config_ = MakeHolder<NCommonProxy::TServerConfig>(params);

        OfferContentConvertProcessor_ = MakeHolder<NMiner::TOfferContentConvertProcessor>(
            "TestOfferContentConverter",
            Config_->GetProccessorsConfigs()
        );
    }

protected:
    THolder<NMiner::TOfferContentConvertProcessor> OfferContentConvertProcessor_;
    THolder<NCommonProxy::TServerConfig> Config_;
};


Y_UNIT_TEST_SUITE_F(TestOfferContentConverter, OfferContentConverterTest) {
    Y_UNIT_TEST(EmptyBatch) {
        auto batch = MakeDefault<Market::DataCamp::OffersBatch, NMiner::TDatacampOfferBatch>();

        ASSERT_TRUE(OfferContentConvertProcessor_->ConvertDatacampBatch(batch, nullptr, context));
    }
    Y_UNIT_TEST(SimpleOffer) {
        auto batch = MakeDefault<Market::DataCamp::OffersBatch, NMiner::TDatacampOfferBatch>();

        auto& offer = AddOfferToBatch(batch);
        auto* original = offer.mutable_content()->mutable_partner()->mutable_original();
        original->mutable_name()->set_value("title");
        original->mutable_description()->set_value("description");
        auto* original_terms = offer.mutable_content()->mutable_partner()->mutable_original_terms();
        original_terms->mutable_sales_notes()->set_value("sales notes");

        ASSERT_TRUE(OfferContentConvertProcessor_->ConvertDatacampBatch(batch, nullptr, context));

        ASSERT_STREQ(offer.content().partner().actual().title().value(), "title");
        ASSERT_STREQ(offer.content().partner().actual().description().value(), "description");
        ASSERT_STREQ(offer.content().partner().actual().sales_notes().value(), "sales notes");
    }
    // tests from feeds/feedparser/tests/yatf/positive/test_caps_lock.py
    Y_UNIT_TEST(CapsTitle) {
        auto batch = MakeDefault<Market::DataCamp::OffersBatch, NMiner::TDatacampOfferBatch>();

        auto& offer1 = AddOfferToBatchWithName(batch, "???????????????? ?????????? ??????????????????");
        auto& offer2 = AddOfferToBatchWithName(batch, "??????????????????????????????????????????");
        auto& offer3 = AddOfferToBatchWithName(batch, "Another... ????????????????!");
        auto& offer4 = AddOfferToBatchWithName(batch, "?????????????? TEFAL TW 3786 RA");
        auto& offer5 = AddOfferToBatchWithName(batch, "Wing TM ?????????????? ??????????");
        auto& offer6 = AddOfferToBatchWithName(batch, "???????????????? ???????? ?????????????????? ?????? ????");

        ASSERT_TRUE(OfferContentConvertProcessor_->ConvertDatacampBatch(batch, nullptr, context));
        ASSERT_STREQ(offer1.content().partner().actual().title().value(), "???????????????? ?????????? ??????????????????");
        ASSERT_STREQ(offer2.content().partner().actual().title().value(), "??????????????????????????????????????????");
        ASSERT_STREQ(offer3.content().partner().actual().title().value(), "Another... ????????????????!");
        ASSERT_STREQ(offer4.content().partner().actual().title().value(), "?????????????? TEFAL TW 3786 RA");
        ASSERT_STREQ(offer5.content().partner().actual().title().value(), "Wing TM ?????????????? ??????????");
        ASSERT_STREQ(offer6.content().partner().actual().title().value(), "???????????????? ???????? ?????????????????? ?????? ????");
    }
    Y_UNIT_TEST(CapsDescription) {
        auto batch = MakeDefault<Market::DataCamp::OffersBatch, NMiner::TDatacampOfferBatch>();

        auto& offer1 = AddOfferToBatchWithDescription(batch, "?????????? ???????????? ??????????!");
        auto& offer2 = AddOfferToBatchWithDescription(batch, "?????? ???????????????? ????. ???? ??????????????");
        auto& offer3 = AddOfferToBatchWithDescription(batch, "?????????? ???????????? ??????????????????????!! ?????? ?????? - ????????????");

        ASSERT_TRUE(OfferContentConvertProcessor_->ConvertDatacampBatch(batch, nullptr, context));
        ASSERT_STREQ(offer1.content().partner().actual().description().value(), "?????????? ???????????? ??????????!");
        ASSERT_STREQ(offer2.content().partner().actual().description().value(), "?????? ???????????????? ????. ???? ??????????????");
        ASSERT_STREQ(offer3.content().partner().actual().description().value(), "?????????? ???????????? ??????????????????????!! ?????? ?????? - ????????????");
    }
    Y_UNIT_TEST(CapsSalesNotes) {
        auto batch = MakeDefault<Market::DataCamp::OffersBatch, NMiner::TDatacampOfferBatch>();

        auto& offer1 = AddOfferToBatchWithSalesNotes(batch, "?????????? ???????????? ??????????");
        auto& offer2 = AddOfferToBatchWithSalesNotes(batch, "?? ?????????????????????? ????????");
        auto& offer3 = AddOfferToBatchWithSalesNotes(batch, "IS THIS the first SENTENCE?! YES, it IS!");

        ASSERT_TRUE(OfferContentConvertProcessor_->ConvertDatacampBatch(batch, nullptr, context));
        ASSERT_STREQ(offer1.content().partner().actual().sales_notes().value(), "?????????? ???????????? ??????????");
        ASSERT_STREQ(offer2.content().partner().actual().sales_notes().value(), "?? ?????????????????????? ????????");
        ASSERT_STREQ(offer3.content().partner().actual().sales_notes().value(), "IS THIS the first SENTENCE?! YES, it IS!");
    }
}
