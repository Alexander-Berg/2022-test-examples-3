#include <market/idx/partners/lib/utils.h>

#include <library/cpp/testing/unittest/registar.h>


Y_UNIT_TEST_SUITE(ShouldParsePartnerStocksFromFeed) {
    Y_UNIT_TEST(OriginalFFPartner) {
        // для обычного fulfillment поставщика мы не должны парсить стоки из фида
        // для fullfilment склада партенра типа кроссдок не парсим стоки из фида -
        // настройки склада совпадают с настройками обычного ff
        NMarket::NPartners::EPartnerProgramType partnerProgramType =  NMarket::NPartners::EPartnerProgramType::PROGRAM_TYPE_FULFILLMENT;
        UNIT_ASSERT(!NMarket::NPartners::ShouldParsePartnerStocksFromFeed(partnerProgramType));
    }
    Y_UNIT_TEST(ClickAndCollectPartner) {
        // для click & collect мы всегда игнорируем стоки (у них как будто бесконечный сток)
        NMarket::NPartners::EPartnerProgramType partnerProgramType = NMarket::NPartners::EPartnerProgramType::PROGRAM_TYPE_CLICK_AND_COLLECT;
        UNIT_ASSERT(!NMarket::NPartners::ShouldParsePartnerStocksFromFeed(partnerProgramType));
    }
    Y_UNIT_TEST(DropshipPartner) {
        // для всех дропшипов парсим стоки из фида
        NMarket::NPartners::EPartnerProgramType partnerProgramType = NMarket::NPartners::EPartnerProgramType::PROGRAM_TYPE_DROPSHIP;
        UNIT_ASSERT(NMarket::NPartners::ShouldParsePartnerStocksFromFeed(partnerProgramType));
    }
    Y_UNIT_TEST(DropshipAndSCPartner) {
        // для всех дропшипов парсим стоки из фида
        NMarket::NPartners::EPartnerProgramType partnerProgramType = NMarket::NPartners::EPartnerProgramType::PROGRAM_TYPE_DROPSHIP_AND_SC;
        UNIT_ASSERT(NMarket::NPartners::ShouldParsePartnerStocksFromFeed(partnerProgramType));
    }
    Y_UNIT_TEST(CrossDocPartner) {
        // партенра типа кроссдок парсим стоки из фида
        NMarket::NPartners::EPartnerProgramType partnerProgramType = NMarket::NPartners::EPartnerProgramType::PROGRAM_TYPE_CROSSDOCK;
        UNIT_ASSERT(NMarket::NPartners::ShouldParsePartnerStocksFromFeed(partnerProgramType));
    }
    Y_UNIT_TEST(UnknownPartner) {
        // для партнера неизвестного типа не парсим стоки из фида
        NMarket::NPartners::EPartnerProgramType partnerProgramType = NMarket::NPartners::EPartnerProgramType::PROGRAM_TYPE_UNKNOWN;
        UNIT_ASSERT(!NMarket::NPartners::ShouldParsePartnerStocksFromFeed(partnerProgramType));
    }
}
