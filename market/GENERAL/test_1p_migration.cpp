#include "test_1p_migration.h"

#include <market/idx/datacamp/lib/proto_helpers/datacamp_offer.h>

#include <library/cpp/logger/global/global.h>

using namespace Market::DataCamp;

namespace {
    void FillMeta(Market::DataCamp::UpdateMeta* meta, google::protobuf::Timestamp timestamp) {
        meta->mutable_timestamp()->CopyFrom(timestamp);
        meta->set_source(MARKET_IDX);
        meta->set_applier(NMarketIndexer::Common::DATACAMP_FIXER);
    }
}

namespace NMarket::NDataCamp::NFixer {

    TMaybe<UnitedOffer> UpdateUnitedCatalogFor1pTest(const UnitedOffer& sourceOffer) {
        static const auto timestamp = google::protobuf::util::TimeUtil::GetCurrentTime();

        if (sourceOffer.service().size() != 1) {
            INFO_LOG << "Ignored: expected only one service offer" << Endl;
            return Nothing();
        }

        TString originalName;
        Market::DataCamp::SupplyPlan::Variation supplyPlanValue;
        i64 market_sku_id = sourceOffer.basic().content().binding().blue_uc_mapping().market_sku_id();

        UnitedOffer result;
        for (const auto& [id, offerOriginal]: sourceOffer.service()) {
            if (IsFirstPartyBusiness(offerOriginal)) {
                auto& offerUpdate = (*result.mutable_service())[id];
                offerUpdate.mutable_identifiers()->set_business_id(offerOriginal.identifiers().business_id());
                offerUpdate.mutable_identifiers()->set_offer_id(offerOriginal.identifiers().offer_id());
                offerUpdate.mutable_identifiers()->set_shop_id(offerOriginal.identifiers().shop_id());
                offerUpdate.mutable_meta()->set_scope(SERVICE);

                // Turn on UnitedCatalog flag
                offerUpdate.mutable_status()->mutable_united_catalog()->set_flag(true);
                FillMeta(offerUpdate.mutable_status()->mutable_united_catalog()->mutable_meta(), timestamp);

                originalName = offerOriginal.content().partner().original().name().value();
                supplyPlanValue = offerOriginal.content().partner().original_terms().supply_plan().value();
            }
        }

        if (result.service().empty()) {
            return Nothing();
        }

        for (const auto& [shopId, actuals]: sourceOffer.actual()) {
            for (const auto& [warehouseId, actual]: actuals.warehouse()) {
                auto& offerUpdate = (*((*result.mutable_actual())[shopId]).mutable_warehouse())[warehouseId];
                offerUpdate.mutable_identifiers()->set_business_id(actual.identifiers().business_id());
                offerUpdate.mutable_identifiers()->set_offer_id(actual.identifiers().offer_id());
                offerUpdate.mutable_identifiers()->set_shop_id(actual.identifiers().shop_id());
                offerUpdate.mutable_identifiers()->set_warehouse_id(actual.identifiers().warehouse_id());
                offerUpdate.mutable_meta()->set_scope(SERVICE);

                // Clear legacy stock_info.weight_and_dimensions
                if (actual.stock_info().has_weight_and_dimensions()) {
                    INFO_LOG << "Clear actual stock_info.weight_and_dimensions: " << Endl;
                    FillMeta(offerUpdate.mutable_stock_info()->mutable_weight_and_dimensions()->mutable_meta(), timestamp);
                }

                // Should be in the basic part
                if (actual.content().binding().has_uc_mapping()) {
                    INFO_LOG << "Clear actual content.binding: " << Endl;
                    FillMeta(offerUpdate.mutable_content()->mutable_binding()->mutable_uc_mapping()->mutable_meta(), timestamp);
                }

                // Should be in the basic part
                if (actual.content().has_market()) {
                    INFO_LOG << "Clear actual content.market: " << Endl;
                    FillMeta(offerUpdate.mutable_content()->mutable_market()->mutable_meta(), timestamp);
                }

                // Clear legacy partner.master_data
                if (actual.content().partner().has_master_data()) {
                    INFO_LOG << "Clear actual content.partner.master_data: " << Endl;
                    FillMeta(offerUpdate.mutable_content()->mutable_partner()->mutable_master_data()->mutable_meta(), timestamp);
                }
            }
        }

        result.mutable_basic()->mutable_identifiers()->set_business_id(sourceOffer.basic().identifiers().business_id());
        result.mutable_basic()->mutable_identifiers()->set_offer_id(sourceOffer.basic().identifiers().offer_id());
        result.mutable_basic()->mutable_meta()->set_scope(BASIC);

        {
            INFO_LOG << "Set original Name: " << originalName << Endl;

            auto* originalNameMsg = result.mutable_basic()->mutable_content()->mutable_partner()->mutable_original()->mutable_name();
            originalNameMsg->set_value(originalName);
            FillMeta(originalNameMsg->mutable_meta(), timestamp);
        }

        {
            INFO_LOG << "Supply plan: " << (int)supplyPlanValue << Endl;

            auto* supplyPlan = result.mutable_basic()->mutable_content()->mutable_partner()->mutable_original_terms()->mutable_supply_plan();
            supplyPlan->set_value(supplyPlanValue);
            FillMeta(supplyPlan->mutable_meta(), timestamp);
        }

        {
            INFO_LOG << "Market SKU id: " << market_sku_id << Endl;

            auto* approved = result.mutable_basic()->mutable_content()->mutable_binding()->mutable_approved();
            approved->set_market_sku_id(market_sku_id);
            FillMeta(approved->mutable_meta(), timestamp);

        }

        {
            INFO_LOG << "Fill in partner original dimensions and weight for MDM" << Endl;
            auto* originalDimensions = result.mutable_basic()->mutable_content()->mutable_partner()->mutable_original()->mutable_dimensions();

            originalDimensions->set_height_mkm(120000);
            originalDimensions->set_length_mkm(180000);
            originalDimensions->set_width_mkm(21000);
            FillMeta(originalDimensions->mutable_meta(), timestamp);

            auto* originalWeight = result.mutable_basic()->mutable_content()->mutable_partner()->mutable_original()->mutable_weight();

            originalWeight->set_value_mg(2500000);
            FillMeta(originalWeight->mutable_meta(), timestamp);
        }

        return result;
    }

}
