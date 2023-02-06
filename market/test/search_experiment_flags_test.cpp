#include <market/report/library/search_experiment_flags/experiments.h>
#include <market/report/library/relevance/Utils.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <market/report/library/global/formula/formula.h>
#include <market/report/library/experiment_flags_delegate_impl/experiment_flags_delegate_impl.h>
#include <market/report/library/formula_calc/formula_calc.h>
#include <market/report/library/global/experiment_flags_getter/experiment_flags_getter.h>

#include <market/report/test/global_mock.h>
#include <util/generic/ptr.h>

namespace {

TAtomicSharedPtr<NMarketReport::TSearchExperimentFlagsDelegateImpl> experimentsDelegate = MakeAtomicShared<NMarketReport::TSearchExperimentFlagsDelegateImpl>();

void SetDefaultExperimentFlags(const TString& str) {
    const_cast<TString&>(Static::reportConfig().DefaultSearchExperimentFlags) = str;
    const_cast<NMarketReport::TExperimentFlags&>(Static::reportConfig().ParsedDefaultSearchExperimentFlags) = NMarketReport::ParseExperimentFlags(Static::reportConfig().DefaultSearchExperimentFlags);
}

NMarketReport::TExperimentFlagWithStringsPtr GetRemoteExperimentFlags() {
    return NMarketReport::NGlobal::GetRemoteExperimentFlags(NMarketReport::NGlobal::GetExpFlagsPresetByConfig());
}

}  // namespace

TEST(ExperimentsTest, ParseEmptySearchExperimentFlags) {
    NMarketReport::TSearchExperimentFlags flags("", experimentsDelegate, GetRemoteExperimentFlags(), new NMarketReport::TEmergencyFlags(), NMarketReport::NGlobal::Contex());

    ASSERT_FALSE(flags.AddCvredirectToWizards);
}

TEST(ExperimentsTest, ParsesDefaultSearchExperimentFlags) {
    SetDefaultExperimentFlags("market_direct_off=1;market_wiz_offer_titleredir=1");

    NMarketReport::TSearchExperimentFlags flags("", experimentsDelegate, GetRemoteExperimentFlags(), MakeAtomicShared<NMarketReport::TEmergencyFlags>(), NMarketReport::NGlobal::Contex());

    ASSERT_TRUE(flags.AddCvredirectToWizards);
}

TEST(ExperimentsTest, ParsesRemoteFlags) {
    SetDefaultExperimentFlags("");
    auto experimentFlags = MakeAtomicShared<NMarketReport::TExperimentFlagWithStrings>();
    experimentFlags->Set("market_wiz_offer_titleredir", "1");
    NMarketReport::TSearchExperimentFlags flags("", experimentsDelegate, experimentFlags, MakeAtomicShared<NMarketReport::TEmergencyFlags>(), NMarketReport::NGlobal::Contex());

    ASSERT_TRUE(flags.AddCvredirectToWizards);
}

TEST(ExperimentsTest, NotParsesStopKranFlagsInSafeMode) {
    SetDefaultExperimentFlags("");
    auto emergencyFlags = MakeAtomicShared<NMarketReport::TEmergencyFlags>();
    emergencyFlags->ignore_remote_experiment_flags = {0, "true"};
    auto experimentFlags = MakeAtomicShared<NMarketReport::TExperimentFlagWithStrings>();
    experimentFlags->Set("market_direct_off", "1");
    experimentFlags->Set("market_wiz_offer_titleredir", "1");
    NMarketReport::TSearchExperimentFlags flags("", experimentsDelegate, experimentFlags, emergencyFlags, NMarketReport::NGlobal::Contex());

    ASSERT_FALSE(flags.AddCvredirectToWizards);
}

TEST(ExperimentsTest, NotParsesRearFlagsInSafeMode) {
    SetDefaultExperimentFlags("");
    auto emergencyFlags = MakeAtomicShared<NMarketReport::TEmergencyFlags>();
    emergencyFlags->ignore_request_rearr_flags = {0, "true"};
    NMarketReport::TSearchExperimentFlags flags("market_wiz_offer_titleredir=1", experimentsDelegate, GetRemoteExperimentFlags(), emergencyFlags, NMarketReport::NGlobal::Contex());

    ASSERT_FALSE(flags.AddCvredirectToWizards);
}

TEST(ExperimentsTest, ParsesExplicitlySpecifiedSearchExperimentFlags) {
    NMarketReport::TGlobalTestScope testGlobal;
    SetDefaultExperimentFlags("market_direct_off=1;market_wiz_offer_titleredir=1");

    NMarketReport::TSearchExperimentFlags flags("market_direct_off=0", experimentsDelegate, GetRemoteExperimentFlags(), MakeAtomicShared<NMarketReport::TEmergencyFlags>(), NMarketReport::NGlobal::Contex());

    ASSERT_TRUE(flags.AddCvredirectToWizards);
}

TEST(ExperimentsTest, ParsesParallelCalcFactorsAggr) {
    NMarketReport::TGlobalTestScope testGlobal;
    {
        NMarketReport::TSearchExperimentFlags flags("market_parallel_calc_factors_aggr=0", experimentsDelegate, GetRemoteExperimentFlags(), MakeAtomicShared<NMarketReport::TEmergencyFlags>(), NMarketReport::NGlobal::Contex());

        ASSERT_FALSE(flags.ParallelCalcDeliveryFactors);
        ASSERT_FALSE(flags.ParallelCalcRegionFactors);
        ASSERT_FALSE(flags.ParallelCalcCategoryDssmFactors);
        ASSERT_FALSE(flags.ParallelCalcNormalizedCtrFactors);
        ASSERT_FALSE(flags.FastCalcDssmFactorCategoryWizard);
        ASSERT_FALSE(flags.ParallelFillModelFactors);
    }
    {
        NMarketReport::TSearchExperimentFlags flags("", experimentsDelegate, GetRemoteExperimentFlags(), MakeAtomicShared<NMarketReport::TEmergencyFlags>(), NMarketReport::NGlobal::Contex());

        ASSERT_TRUE(flags.ParallelCalcDeliveryFactors);
        ASSERT_TRUE(flags.ParallelCalcRegionFactors);
        ASSERT_TRUE(flags.ParallelCalcCategoryDssmFactors);
        ASSERT_TRUE(flags.ParallelCalcNormalizedCtrFactors);
        ASSERT_TRUE(flags.FastCalcDssmFactorCategoryWizard);
        ASSERT_TRUE(flags.ParallelFillModelFactors);
    }
}

TEST(ExperimentsTest, ParsesParallelCollapsingAggr) {
    NMarketReport::TGlobalTestScope testGlobal;
    {
        NMarketReport::TSearchExperimentFlags flags("", experimentsDelegate, GetRemoteExperimentFlags(), MakeAtomicShared<NMarketReport::TEmergencyFlags>(), NMarketReport::NGlobal::Contex());

        ASSERT_TRUE(flags.ParallelUseCollapsing);
        ASSERT_EQ(flags.ParallelCollapsingCount, 10);
        ASSERT_EQ(flags.ModelWizardCollapsingTopModelThreshold, TMaybe<double>(-100));
        ASSERT_EQ(flags.ModelWizardCollapsingMetaThreshold, TMaybe<double>(0.295));
        ASSERT_EQ(flags.ImplicitModelWizardCollapsingTopModelsThreshold, TMaybe<double>(-100));
        ASSERT_EQ(flags.ImplicitModelWizardCollapsingMetaThreshold, TMaybe<double>(0.26));
    }
    {
        NMarketReport::TSearchExperimentFlags flags("market_parallel_collapsing_aggr=0,11,1.1,1.2,1.3,1.4", experimentsDelegate, GetRemoteExperimentFlags(), MakeAtomicShared<NMarketReport::TEmergencyFlags>(), NMarketReport::NGlobal::Contex());

        ASSERT_FALSE(flags.ParallelUseCollapsing);
        ASSERT_EQ(flags.ParallelCollapsingCount, 11);
        ASSERT_EQ(flags.ModelWizardCollapsingTopModelThreshold, TMaybe<double>(1.1));
        ASSERT_EQ(flags.ModelWizardCollapsingMetaThreshold, TMaybe<double>(1.2));
        ASSERT_EQ(flags.ImplicitModelWizardCollapsingTopModelsThreshold, TMaybe<double>(1.3));
        ASSERT_EQ(flags.ImplicitModelWizardCollapsingMetaThreshold, TMaybe<double>(1.4));
    }
    {
        NMarketReport::TSearchExperimentFlags flags("market_parallel_collapsing_aggr=,11,1.1,,1.3,", experimentsDelegate, GetRemoteExperimentFlags(), MakeAtomicShared<NMarketReport::TEmergencyFlags>(), NMarketReport::NGlobal::Contex());

        ASSERT_TRUE(flags.ParallelUseCollapsing);
        ASSERT_EQ(flags.ParallelCollapsingCount, 11);
        ASSERT_EQ(flags.ModelWizardCollapsingTopModelThreshold, TMaybe<double>(1.1));
        ASSERT_EQ(flags.ModelWizardCollapsingMetaThreshold, TMaybe<double>(0.295));
        ASSERT_EQ(flags.ImplicitModelWizardCollapsingTopModelsThreshold, TMaybe<double>(1.3));
        ASSERT_EQ(flags.ImplicitModelWizardCollapsingMetaThreshold, TMaybe<double>(0.26));
    }
    {
        // 5 fields < 6
        NMarketReport::TSearchExperimentFlags flags("market_parallel_collapsing_aggr=1,2,3,4,5", experimentsDelegate, GetRemoteExperimentFlags(), MakeAtomicShared<NMarketReport::TEmergencyFlags>(), NMarketReport::NGlobal::Contex());

        ASSERT_TRUE(flags.ParallelUseCollapsing);
        ASSERT_EQ(flags.ParallelCollapsingCount, 10);
        ASSERT_EQ(flags.ModelWizardCollapsingTopModelThreshold, TMaybe<double>(-100));
        ASSERT_EQ(flags.ModelWizardCollapsingMetaThreshold, TMaybe<double>(0.295));
        ASSERT_EQ(flags.ImplicitModelWizardCollapsingTopModelsThreshold, TMaybe<double>(-100));
        ASSERT_EQ(flags.ImplicitModelWizardCollapsingMetaThreshold, TMaybe<double>(0.26));
    }
    {
        // 7 fields > 6
        NMarketReport::TSearchExperimentFlags flags("market_parallel_collapsing_aggr=1,2,3,4,5,6,7", experimentsDelegate, GetRemoteExperimentFlags(), MakeAtomicShared<NMarketReport::TEmergencyFlags>(), NMarketReport::NGlobal::Contex());

        ASSERT_TRUE(flags.ParallelUseCollapsing);
        ASSERT_EQ(flags.ParallelCollapsingCount, 10);
        ASSERT_EQ(flags.ModelWizardCollapsingTopModelThreshold, TMaybe<double>(-100));
        ASSERT_EQ(flags.ModelWizardCollapsingMetaThreshold, TMaybe<double>(0.295));
        ASSERT_EQ(flags.ImplicitModelWizardCollapsingTopModelsThreshold, TMaybe<double>(-100));
        ASSERT_EQ(flags.ImplicitModelWizardCollapsingMetaThreshold, TMaybe<double>(0.26));
    }
}

TEST(ExperimentsTest, ParsesParallelOfferModelFormulasAggr) {
    NMarketReport::TGlobalTestScope testGlobal;
    NMarketReport::NGlobal::GetBaseFormulasManagerForTesting().Add("MNA_1", nullptr);
    NMarketReport::NGlobal::GetBaseFormulasManagerForTesting().Add("MNA_2", nullptr);
    NMarketReport::NGlobal::GetBaseFormulasManagerForTesting().Add("MNA_3", nullptr);
    NMarketReport::NGlobal::GetBaseFormulasManagerForTesting().Add("MNA_4", nullptr);
    NMarketReport::NGlobal::GetBaseFormulasManagerForTesting().Add("MNA_5", nullptr);

    {
        NMarketReport::TSearchExperimentFlags flags("", experimentsDelegate, GetRemoteExperimentFlags(), MakeAtomicShared<NMarketReport::TEmergencyFlags>(), NMarketReport::NGlobal::Contex());

        ASSERT_EQ(flags.ModelSearchMNAlgo, Nothing());
        ASSERT_EQ(flags.ModelWizardMetaThreshold, TMaybe<double>(100));
        ASSERT_EQ(flags.ImplicitModelWizardMetaThreshold, TMaybe<double>(100));
        ASSERT_EQ(flags.SearchMNAlgo, Nothing());
        ASSERT_EQ(flags.OffersIncutMetaMNAlgo, Nothing());
        ASSERT_EQ(flags.OffersIncutMetaThreshold, TMaybe<double>(0.78));
        ASSERT_EQ(flags.OffersWizardTopOffersThreshold, TMaybe<double>(1.8));
        ASSERT_EQ(flags.OffersWizardTopOffersMetaThreshold, TMaybe<double>(-100.0));
        ASSERT_EQ(flags.ModelWizardMetaMNAlgo, Nothing());
        ASSERT_EQ(flags.ImplicitModelWizardMetaMNAlgo, Nothing());
        ASSERT_EQ(flags.ModelWizardTopModelThreshold, TMaybe<double>(-100));
        ASSERT_EQ(flags.RelevanceFormulaThresholdOnParallelForOffers, Nothing());
    }
    {
        NMarketReport::TSearchExperimentFlags flags("market_parallel_offer_model_formulas_aggr=MNA_1,0.1,0.2,MNA_2,MNA_3,0.3,0.4,0.5,MNA_4,MNA_5,0.6,0.7", experimentsDelegate, GetRemoteExperimentFlags(), MakeAtomicShared<NMarketReport::TEmergencyFlags>(), NMarketReport::NGlobal::Contex());

        ASSERT_EQ(flags.ModelSearchMNAlgo, TMaybe<NMarketReport::MatrixNetAlgorithm>("MNA_1"));
        ASSERT_EQ(flags.ModelWizardMetaThreshold, TMaybe<double>(0.1));
        ASSERT_EQ(flags.ImplicitModelWizardMetaThreshold, TMaybe<double>(0.2));
        ASSERT_EQ(flags.SearchMNAlgo, TMaybe<NMarketReport::MatrixNetAlgorithm>("MNA_2"));
        ASSERT_EQ(flags.OffersIncutMetaMNAlgo, TMaybe<NMarketReport::MatrixNetAlgorithm>("MNA_3"));
        ASSERT_EQ(flags.OffersIncutMetaThreshold, TMaybe<double>(0.3));
        ASSERT_EQ(flags.OffersWizardTopOffersThreshold, TMaybe<double>(0.4));
        ASSERT_EQ(flags.OffersWizardTopOffersMetaThreshold, TMaybe<double>(0.5));
        ASSERT_EQ(flags.ModelWizardMetaMNAlgo, TMaybe<NMarketReport::MatrixNetAlgorithm>("MNA_4"));
        ASSERT_EQ(flags.ImplicitModelWizardMetaMNAlgo, TMaybe<NMarketReport::MatrixNetAlgorithm>("MNA_5"));
        ASSERT_EQ(flags.ModelWizardTopModelThreshold, TMaybe<double>(0.6));
        ASSERT_EQ(flags.RelevanceFormulaThresholdOnParallelForOffers, TMaybe<float>(0.7));
    }
    {
        NMarketReport::TSearchExperimentFlags flags("market_parallel_offer_model_formulas_aggr=,0.1,,,MNA_3,0.3,,0.5,MNA_4,,0.6,", experimentsDelegate, GetRemoteExperimentFlags(), MakeAtomicShared<NMarketReport::TEmergencyFlags>(), NMarketReport::NGlobal::Contex());

        ASSERT_EQ(flags.ModelSearchMNAlgo, Nothing());
        ASSERT_EQ(flags.ModelWizardMetaThreshold, TMaybe<double>(0.1));
        ASSERT_EQ(flags.ImplicitModelWizardMetaThreshold, TMaybe<double>(100));
        ASSERT_EQ(flags.SearchMNAlgo, Nothing());
        ASSERT_EQ(flags.OffersIncutMetaMNAlgo, TMaybe<NMarketReport::MatrixNetAlgorithm>("MNA_3"));
        ASSERT_EQ(flags.OffersIncutMetaThreshold, TMaybe<double>(0.3));
        ASSERT_EQ(flags.OffersWizardTopOffersThreshold, TMaybe<double>(1.8));
        ASSERT_EQ(flags.OffersWizardTopOffersMetaThreshold, TMaybe<double>(0.5));
        ASSERT_EQ(flags.ModelWizardMetaMNAlgo, TMaybe<NMarketReport::MatrixNetAlgorithm>("MNA_4"));
        ASSERT_EQ(flags.ImplicitModelWizardMetaMNAlgo, Nothing());
        ASSERT_EQ(flags.ModelWizardTopModelThreshold, TMaybe<double>(0.6));
        ASSERT_EQ(flags.RelevanceFormulaThresholdOnParallelForOffers, Nothing());
    }
    {
        // incorrect formula name
        NMarketReport::TSearchExperimentFlags flags("market_parallel_offer_model_formulas_aggr=MNA_nonexistent,0.1,0.2,MNA_2,MNA_3,0.3,0.4,0.5,MNA_4,MNA_5,0.6,0.7", experimentsDelegate, GetRemoteExperimentFlags(), MakeAtomicShared<NMarketReport::TEmergencyFlags>(), NMarketReport::NGlobal::Contex());

        ASSERT_EQ(flags.ModelSearchMNAlgo, Nothing());
        ASSERT_EQ(flags.ModelWizardMetaThreshold, TMaybe<double>(100));
        ASSERT_EQ(flags.ImplicitModelWizardMetaThreshold, TMaybe<double>(100));
        ASSERT_EQ(flags.SearchMNAlgo, Nothing());
        ASSERT_EQ(flags.OffersIncutMetaMNAlgo, Nothing());
        ASSERT_EQ(flags.OffersIncutMetaThreshold, TMaybe<double>(0.78));
        ASSERT_EQ(flags.OffersWizardTopOffersThreshold, TMaybe<double>(1.8));
        ASSERT_EQ(flags.OffersWizardTopOffersMetaThreshold, TMaybe<double>(-100.0));
        ASSERT_EQ(flags.ModelWizardMetaMNAlgo, Nothing());
        ASSERT_EQ(flags.ImplicitModelWizardMetaMNAlgo, Nothing());
        ASSERT_EQ(flags.ModelWizardTopModelThreshold, TMaybe<double>(-100));
        ASSERT_EQ(flags.RelevanceFormulaThresholdOnParallelForOffers, Nothing());
    }
    {
        // 11 fields < 12
        NMarketReport::TSearchExperimentFlags flags("market_parallel_offer_model_formulas_aggr=MNA_1,0.1,0.2,MNA_2,MNA_3,0.3,0.4,0.5,MNA_4,MNA_5,0.6", experimentsDelegate, GetRemoteExperimentFlags(), MakeAtomicShared<NMarketReport::TEmergencyFlags>(), NMarketReport::NGlobal::Contex());

        ASSERT_EQ(flags.ModelSearchMNAlgo, Nothing());
        ASSERT_EQ(flags.ModelWizardMetaThreshold, TMaybe<double>(100));
        ASSERT_EQ(flags.ImplicitModelWizardMetaThreshold, TMaybe<double>(100));
        ASSERT_EQ(flags.SearchMNAlgo, Nothing());
        ASSERT_EQ(flags.OffersIncutMetaMNAlgo, Nothing());
        ASSERT_EQ(flags.OffersIncutMetaThreshold, TMaybe<double>(0.78));
        ASSERT_EQ(flags.OffersWizardTopOffersThreshold, TMaybe<double>(1.8));
        ASSERT_EQ(flags.OffersWizardTopOffersMetaThreshold, TMaybe<double>(-100.0));
        ASSERT_EQ(flags.ModelWizardMetaMNAlgo, Nothing());
        ASSERT_EQ(flags.ImplicitModelWizardMetaMNAlgo, Nothing());
        ASSERT_EQ(flags.ModelWizardTopModelThreshold, TMaybe<double>(-100));
        ASSERT_EQ(flags.RelevanceFormulaThresholdOnParallelForOffers, Nothing());
    }
    {
        // 13 fields > 12
        NMarketReport::TSearchExperimentFlags flags("market_parallel_offer_model_formulas_aggr=MNA_1,0.1,0.2,MNA_2,MNA_3,0.3,0.4,0.5,MNA_4,MNA_5,0.6,0.7,0.8", experimentsDelegate, GetRemoteExperimentFlags(), MakeAtomicShared<NMarketReport::TEmergencyFlags>(), NMarketReport::NGlobal::Contex());

        ASSERT_EQ(flags.ModelSearchMNAlgo, Nothing());
        ASSERT_EQ(flags.ModelWizardMetaThreshold, TMaybe<double>(100));
        ASSERT_EQ(flags.ImplicitModelWizardMetaThreshold, TMaybe<double>(100));
        ASSERT_EQ(flags.SearchMNAlgo, Nothing());
        ASSERT_EQ(flags.OffersIncutMetaMNAlgo, Nothing());
        ASSERT_EQ(flags.OffersIncutMetaThreshold, TMaybe<double>(0.78));
        ASSERT_EQ(flags.OffersWizardTopOffersThreshold, TMaybe<double>(1.8));
        ASSERT_EQ(flags.OffersWizardTopOffersMetaThreshold, TMaybe<double>(-100.0));
        ASSERT_EQ(flags.ModelWizardMetaMNAlgo, Nothing());
        ASSERT_EQ(flags.ImplicitModelWizardMetaMNAlgo, Nothing());
        ASSERT_EQ(flags.ModelWizardTopModelThreshold, TMaybe<double>(-100));
        ASSERT_EQ(flags.RelevanceFormulaThresholdOnParallelForOffers, Nothing());
    }
}

TEST(ExperimentsTest, ParsesParallelSplitOffersAggr) {
    NMarketReport::TGlobalTestScope testGlobal;
    {
        NMarketReport::TSearchExperimentFlags flags("", experimentsDelegate, GetRemoteExperimentFlags(), MakeAtomicShared<NMarketReport::TEmergencyFlags>(), NMarketReport::NGlobal::Contex());

        ASSERT_FALSE(flags.EnableOffersWizardRightIncut);
        ASSERT_TRUE(flags.EnableOffersWizardCenterIncut);
        ASSERT_EQ(flags.OffersCenterIncutMetaThreshold, Nothing());
    }
    {
        NMarketReport::TSearchExperimentFlags flags("market_parallel_split_offers_aggr=,1,1,1,2.3", experimentsDelegate, GetRemoteExperimentFlags(), MakeAtomicShared<NMarketReport::TEmergencyFlags>(), NMarketReport::NGlobal::Contex());

        ASSERT_TRUE(flags.EnableOffersWizardRightIncut);
        ASSERT_TRUE(flags.EnableOffersWizardCenterIncut);
        ASSERT_EQ(flags.OffersCenterIncutMetaThreshold, TMaybe<double>(2.3));
    }
    {
        NMarketReport::TSearchExperimentFlags flags("market_parallel_split_offers_aggr=,0,,1,", experimentsDelegate, GetRemoteExperimentFlags(), MakeAtomicShared<NMarketReport::TEmergencyFlags>(), NMarketReport::NGlobal::Contex());

        ASSERT_FALSE(flags.EnableOffersWizardRightIncut);
        ASSERT_TRUE(flags.EnableOffersWizardCenterIncut);
        ASSERT_EQ(flags.OffersCenterIncutMetaThreshold, Nothing());
    }
    {
        // 4 fields < 5
        NMarketReport::TSearchExperimentFlags flags("market_parallel_split_offers_aggr=,1,1,1", experimentsDelegate, GetRemoteExperimentFlags(), MakeAtomicShared<NMarketReport::TEmergencyFlags>(), NMarketReport::NGlobal::Contex());

        ASSERT_FALSE(flags.EnableOffersWizardRightIncut);
        ASSERT_TRUE(flags.EnableOffersWizardCenterIncut);
        ASSERT_EQ(flags.OffersCenterIncutMetaThreshold, Nothing());
    }
    {
        // 6 fields > 5
        NMarketReport::TSearchExperimentFlags flags("market_parallel_split_offers_aggr=,1,1,1,1,1", experimentsDelegate, GetRemoteExperimentFlags(), MakeAtomicShared<NMarketReport::TEmergencyFlags>(), NMarketReport::NGlobal::Contex());

        ASSERT_FALSE(flags.EnableOffersWizardRightIncut);
        ASSERT_TRUE(flags.EnableOffersWizardCenterIncut);
        ASSERT_EQ(flags.OffersCenterIncutMetaThreshold, Nothing());
    }
}

TEST(ExperimentsTest, ParseParallelCpcOffersWizardAggr) {
    NMarketReport::TGlobalTestScope testGlobal;
    {
        NMarketReport::TSearchExperimentFlags flags("", experimentsDelegate, GetRemoteExperimentFlags(), MakeAtomicShared<NMarketReport::TEmergencyFlags>(), NMarketReport::NGlobal::Contex());

        ASSERT_EQ(flags.ParallelSliderOffersRequestCount, 0ul);
        ASSERT_EQ(flags.ParallelSliderExtraOffers, 1);
        ASSERT_EQ(flags.ParallelSliderOffersMaxCount, 35ul);
        ASSERT_EQ(flags.OffersWizardIncutUrlType, NMarketReport::TSearchExperimentFlags::EOfferUrlType::External);
        ASSERT_FALSE(flags.OffersIncutSupplierShopName);
        ASSERT_FALSE(flags.OffersWizardModelRating);
    }
    {
        NMarketReport::TSearchExperimentFlags flags("market_parallel_cpc_offers_wizard_aggr=7,1,35,External,0,1", experimentsDelegate, GetRemoteExperimentFlags(), MakeAtomicShared<NMarketReport::TEmergencyFlags>(), NMarketReport::NGlobal::Contex());

        ASSERT_EQ(flags.ParallelSliderOffersRequestCount, 7);
        ASSERT_EQ(flags.ParallelSliderExtraOffers, 1);
        ASSERT_EQ(flags.ParallelSliderOffersMaxCount, TMaybe<size_t>(35));
        ASSERT_EQ(flags.OffersWizardIncutUrlType, NMarketReport::TSearchExperimentFlags::EOfferUrlType::External);
        ASSERT_FALSE(flags.OffersIncutSupplierShopName);
        ASSERT_TRUE(flags.OffersWizardModelRating);
    }
    {
        NMarketReport::TSearchExperimentFlags flags("market_parallel_cpc_offers_wizard_aggr=,,35,External,,1", experimentsDelegate, GetRemoteExperimentFlags(), MakeAtomicShared<NMarketReport::TEmergencyFlags>(), NMarketReport::NGlobal::Contex());

        ASSERT_EQ(flags.ParallelSliderOffersRequestCount, 0);
        ASSERT_EQ(flags.ParallelSliderExtraOffers, 1ul);
        ASSERT_EQ(flags.ParallelSliderOffersMaxCount, TMaybe<size_t>(35));
        ASSERT_EQ(flags.OffersWizardIncutUrlType, NMarketReport::TSearchExperimentFlags::EOfferUrlType::External);
        ASSERT_FALSE(flags.OffersIncutSupplierShopName);
        ASSERT_TRUE(flags.OffersWizardModelRating);
    }
    {
        // 3 fields < 6
        NMarketReport::TSearchExperimentFlags flags("market_parallel_cpc_offers_wizard_aggr=7,1,35", experimentsDelegate, GetRemoteExperimentFlags(), MakeAtomicShared<NMarketReport::TEmergencyFlags>(), NMarketReport::NGlobal::Contex());

        ASSERT_EQ(flags.ParallelSliderOffersRequestCount, 0);
        ASSERT_EQ(flags.ParallelSliderExtraOffers, 1ul);
        ASSERT_EQ(flags.ParallelSliderOffersMaxCount, 35ul);
        ASSERT_EQ(flags.OffersWizardIncutUrlType, NMarketReport::TSearchExperimentFlags::EOfferUrlType::External);
        ASSERT_FALSE(flags.OffersIncutSupplierShopName);
        ASSERT_FALSE(flags.OffersWizardModelRating);
    }
    {
        // 7 fields > 6
        NMarketReport::TSearchExperimentFlags flags("market_parallel_cpc_offers_wizard_aggr=7,1,35,External,0,1,1", experimentsDelegate, GetRemoteExperimentFlags(), MakeAtomicShared<NMarketReport::TEmergencyFlags>(), NMarketReport::NGlobal::Contex());

        ASSERT_EQ(flags.ParallelSliderOffersRequestCount, 0);
        ASSERT_EQ(flags.ParallelSliderExtraOffers, 1ul);
        ASSERT_EQ(flags.ParallelSliderOffersMaxCount, 35ul);
        ASSERT_EQ(flags.OffersWizardIncutUrlType, NMarketReport::TSearchExperimentFlags::EOfferUrlType::External);
        ASSERT_FALSE(flags.OffersIncutSupplierShopName);
        ASSERT_FALSE(flags.OffersWizardModelRating);
    }
}
