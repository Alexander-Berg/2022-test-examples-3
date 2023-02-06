#include <market/idx/partners/lib/partners_program_types.h>

#include <library/cpp/testing/unittest/registar.h>


Y_UNIT_TEST_SUITE(PartnerProgramType) {
    Y_UNIT_TEST(OriginalFFPartner) {
        // fulfillment склад
        TString fulfillmentProgram = "REAL";
        bool directShipping = true;
        bool ignoreStocks = false;
        bool isBlue = true;
        bool isDsbs = false;

        NMarket::NPartners::EPartnerProgramType partnerProgramType =
            NMarket::NPartners::PartnerProgramType(fulfillmentProgram, directShipping, ignoreStocks, isBlue, isDsbs);
        UNIT_ASSERT_EQUAL(partnerProgramType, NMarket::NPartners::EPartnerProgramType::PROGRAM_TYPE_FULFILLMENT);
    }
    Y_UNIT_TEST(ClickAndCollectPartner) {
        // click & collect
        TString fulfillmentProgram = "NO";
        bool directShipping = true;
        bool ignoreStocks = true;
        bool isBlue = true;
        bool isDsbs = false;

        NMarket::NPartners::EPartnerProgramType partnerProgramType =
            NMarket::NPartners::PartnerProgramType(fulfillmentProgram, directShipping, ignoreStocks, isBlue, isDsbs);
        UNIT_ASSERT_EQUAL(partnerProgramType, NMarket::NPartners::EPartnerProgramType::PROGRAM_TYPE_CLICK_AND_COLLECT);
    }
    Y_UNIT_TEST(DropshipPartner) {
        // дропшип
        TString fulfillmentProgram = "NO";
        bool directShipping = true;
        bool ignoreStocks = false;
        bool isBlue = true;
        bool isDsbs = false;

        NMarket::NPartners::EPartnerProgramType partnerProgramType =
            NMarket::NPartners::PartnerProgramType(fulfillmentProgram, directShipping, ignoreStocks, isBlue, isDsbs);
        UNIT_ASSERT_EQUAL(partnerProgramType, NMarket::NPartners::EPartnerProgramType::PROGRAM_TYPE_DROPSHIP);
    }
    Y_UNIT_TEST(DropshipAndSCPartner) {
        // дропшип с отгрузкой в сортировочный центр
        TString fulfillmentProgram ="NO";
        bool directShipping = false;
        bool ignoreStocks = false;
        bool isBlue = true;
        bool isDsbs = false;

        NMarket::NPartners::EPartnerProgramType partnerProgramType =
            NMarket::NPartners::PartnerProgramType(fulfillmentProgram, directShipping, ignoreStocks, isBlue, isDsbs);
        UNIT_ASSERT_EQUAL(partnerProgramType, NMarket::NPartners::EPartnerProgramType::PROGRAM_TYPE_DROPSHIP_AND_SC);
    }
    Y_UNIT_TEST(CrossDocPartner) {
        // кроссдок
        TString fulfillmentProgram = "REAL";
        bool directShipping = false;
        bool ignoreStocks = false;
        bool isBlue = true;
        bool isDsbs = false;

        NMarket::NPartners::EPartnerProgramType partnerProgramType =
            NMarket::NPartners::PartnerProgramType(fulfillmentProgram, directShipping, ignoreStocks, isBlue, isDsbs);
        UNIT_ASSERT_EQUAL(partnerProgramType, NMarket::NPartners::EPartnerProgramType::PROGRAM_TYPE_CROSSDOCK);
    }
    Y_UNIT_TEST(UnknownPartner) {
        // неизвестный партнер
        TString fulfillmentProgram = "SBX";
        bool directShipping = false;
        bool ignoreStocks = false;
        bool isBlue = true;
        bool isDsbs = false;

        NMarket::NPartners::EPartnerProgramType partnerProgramType =
            NMarket::NPartners::PartnerProgramType(fulfillmentProgram, directShipping, ignoreStocks, isBlue, isDsbs);
        UNIT_ASSERT_EQUAL(partnerProgramType, NMarket::NPartners::EPartnerProgramType::PROGRAM_TYPE_UNKNOWN);
    }
    Y_UNIT_TEST(DSBSPartner) {
        // белый dsbs партнер (с нормальными конечными стоками)
        TString fulfillmentProgram = "NO";
        bool directShipping = false;
        bool ignoreStocks = false;
        bool isBlue = false;
        bool isDsbs = true;

        NMarket::NPartners::EPartnerProgramType partnerProgramType = NMarket::NPartners::PartnerProgramType(fulfillmentProgram, directShipping, ignoreStocks, isBlue, isDsbs);
        UNIT_ASSERT_EQUAL(partnerProgramType, NMarket::NPartners::EPartnerProgramType::PROGRAM_TYPE_DROPSHIP_BY_SALER);
    }
    Y_UNIT_TEST(DSBSPartnerWithIgnoreStocks) {
        // белый dsbs партнер (с бесконечными стоками)
        TString fulfillmentProgram = "NO";
        bool directShipping = false;
        bool ignoreStocks = true;
        bool isBlue = false;
        bool isDsbs = true;

        NMarket::NPartners::EPartnerProgramType partnerProgramType = NMarket::NPartners::PartnerProgramType(fulfillmentProgram, directShipping, ignoreStocks, isBlue, isDsbs);
        UNIT_ASSERT_EQUAL(partnerProgramType, NMarket::NPartners::EPartnerProgramType::PROGRAM_TYPE_CLICK_AND_COLLECT);
    }
    Y_UNIT_TEST(WhitePartner) {
        // белый партнер
        TString fulfillmentProgram = "NO";
        bool directShipping = false;
        bool ignoreStocks = false;
        bool isBlue = false;
        bool isDsbs = false;

        NMarket::NPartners::EPartnerProgramType partnerProgramType = NMarket::NPartners::PartnerProgramType(fulfillmentProgram, directShipping, ignoreStocks, isBlue, isDsbs);
        UNIT_ASSERT_EQUAL(partnerProgramType, NMarket::NPartners::EPartnerProgramType::PROGRAM_TYPE_UNKNOWN);
    }
}
