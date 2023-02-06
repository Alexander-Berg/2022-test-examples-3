#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/gtest.h>

#include <market/idx/offers/processors/cms_promo_processor/cms_promo_processor.h>
#include <market/library/cms_promo/cms_promo.h>

#include <util/folder/path.h>

namespace {
    TString MakeCmsPromoPath() {
        return JoinFsPaths(
            ArcadiaSourceRoot(),
            "market/library/cms_promo/ut/data/cms_report_promo.pbsn"
        );
    }

    const static NMarket::NCmsPromo::TMskuPromos mskuPromos =
        NMarket::NCmsPromo::LoadCmsMskuPromosFromPbsnFile(MakeCmsPromoPath());

    const static NMarket::NCmsPromo::TModelPromos modelPromos =
        NMarket::NCmsPromo::LoadCmsModelPromosFromPbsnFile(MakeCmsPromoPath());

    const TCmsPromoProcessor& GetCmsPromoProcessor() {
        static auto processor = TCmsPromoProcessor(mskuPromos, modelPromos);
        return processor;
    }
}


TEST(CmsPromoProcessor, OfferFrameWithoutAnyModelIdSet) {
    TCmsPromoProcessor::TGlRecord glRecord;

    auto processor = GetCmsPromoProcessor();
    ASSERT_NO_THROW(processor.ProcessOffer(&glRecord));
    ASSERT_EQ(0, glRecord.model_promos().size());
}


TEST(CmsPromoProcessor, OfferFrameWithModelIdPromo) {
    TCmsPromoProcessor::TGlRecord glRecord;
    glRecord.set_model_id(657843127);

    auto processor = GetCmsPromoProcessor();
    ASSERT_NO_THROW(processor.ProcessOffer(&glRecord));
    ASSERT_EQ(1, glRecord.model_promos().size());
    ASSERT_STREQ("huawei-p40", glRecord.model_promos()[0]);
}


TEST(CmsPromoProcessor, OfferFrameWithParentModelIdAndModelIdPromo) {
    TCmsPromoProcessor::TGlRecord glRecord;
    glRecord.set_model_id(657843127);

    auto processor = GetCmsPromoProcessor();
    ASSERT_NO_THROW(processor.ProcessOffer(&glRecord));
    ASSERT_EQ(1, glRecord.model_promos().size());
    ASSERT_STREQ("huawei-p40", glRecord.model_promos()[0]);
}


TEST(CmsPromoProcessor, OfferFrameWithoutParentModelIdAndModelIdPromo) {
    TCmsPromoProcessor::TGlRecord glRecord;
    glRecord.set_model_id(1);

    auto processor = GetCmsPromoProcessor();
    ASSERT_NO_THROW(processor.ProcessOffer(&glRecord));
    ASSERT_EQ(0, glRecord.model_promos().size());
}


TEST(CmsPromoProcessor, FakeMskuOfferFrameWithMskuPromo) {
    TCmsPromoProcessor::TGlRecord glRecord;
    glRecord.set_market_sku(100906962092);
    glRecord.set_is_fake_msku_offer(true);
    glRecord.set_is_blue_offer(false);

    auto processor = GetCmsPromoProcessor();
    ASSERT_NO_THROW(processor.ProcessOffer(&glRecord));
    ASSERT_EQ(2, glRecord.msku_promos().size());
    ASSERT_STREQ("nb-koltsa", glRecord.msku_promos()[0]);
    ASSERT_STREQ("nebo10", glRecord.msku_promos()[1]);
}


TEST(CmsPromoProcessor, BlueOfferFrameWithMskuPromo) {
    TCmsPromoProcessor::TGlRecord glRecord;
    glRecord.set_market_sku(100906962092);
    glRecord.set_is_blue_offer(true);
    glRecord.set_is_fake_msku_offer(false);

    auto processor = GetCmsPromoProcessor();
    ASSERT_NO_THROW(processor.ProcessOffer(&glRecord));
    ASSERT_EQ(2, glRecord.msku_promos().size());
    ASSERT_STREQ("nb-koltsa", glRecord.msku_promos()[0]);
    ASSERT_STREQ("nebo10", glRecord.msku_promos()[1]);
}


TEST(CmsPromoProcessor, BlueOfferFrameWithoutMskuPromo) {
    TCmsPromoProcessor::TGlRecord glRecord;
    glRecord.set_market_sku(999996962092);
    glRecord.set_is_fake_msku_offer(false);
    glRecord.set_is_blue_offer(true);

    auto processor = GetCmsPromoProcessor();
    ASSERT_NO_THROW(processor.ProcessOffer(&glRecord));
    ASSERT_EQ(0, glRecord.msku_promos().size());
}


TEST(CmsPromoProcessor, WhiteOfferFrameWithoutMskuPromo) {
    TCmsPromoProcessor::TGlRecord glRecord;
    glRecord.set_market_sku(100906962092);
    glRecord.set_is_fake_msku_offer(false);
    glRecord.set_is_blue_offer(false);

    auto processor = GetCmsPromoProcessor();
    ASSERT_NO_THROW(processor.ProcessOffer(&glRecord));
    ASSERT_EQ(0, glRecord.msku_promos().size());
}


TEST(CmsPromoProcessor, BlueOfferFrameWithMskuAndParentModelIdAndModelIdPromo) {
    TCmsPromoProcessor::TGlRecord glRecord;
    glRecord.set_market_sku(100906962092);
    glRecord.set_is_fake_msku_offer(false);
    glRecord.set_is_blue_offer(true);
    glRecord.set_model_id(657843127);

    auto processor = GetCmsPromoProcessor();
    ASSERT_NO_THROW(processor.ProcessOffer(&glRecord));
    ASSERT_EQ(2, glRecord.msku_promos().size());
    ASSERT_STREQ("nb-koltsa", glRecord.msku_promos()[0]);
    ASSERT_STREQ("nebo10", glRecord.msku_promos()[1]);

    ASSERT_EQ(1, glRecord.model_promos().size());
    ASSERT_STREQ("huawei-p40", glRecord.model_promos()[0]);
}
