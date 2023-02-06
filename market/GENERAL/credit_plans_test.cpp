#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

#include <market/library/credit_plans/credit_plans.h>

#include <market/library/currency/currency.h>
#include <market/library/exception/exception.h>
#include <market/library/fixed_point_number/fixed_point_number.h>

using namespace NCreditPlans;

namespace {
    const TVector<TString> CreditPlansConfigs = {
        "market/svn-data/package-data/fast/credit_plans.testing.json",
        "market/svn-data/package-data/fast/credit_plans.production.json",
        "market/svn-data/package-data/credit_plans.testing.json",
        "market/svn-data/package-data/credit_plans.production.json"
    };
}

void CheckPriceRestrictions(const TGlobalRestrictions& restrictions) {
    TFixedPointNumber minPrice = restrictions.MinPrice ? restrictions.MinPrice->Value
                                                       : TFixedPointNumber(0);
    TFixedPointNumber maxPrice = restrictions.MaxPrice ? restrictions.MaxPrice->Value
                                                       : TFixedPointNumber(100000000);
    EXPECT_TRUE(minPrice < maxPrice);
}

TEST(TestCreditPlans, StaticLoad) {
    TGlobalRestrictions restrictions;
    TCreditPlans plans;
    for (const auto& config : CreditPlansConfigs) {
        const auto path = JoinFsPaths(
            ArcadiaSourceRoot(),
            config
        );
        EXPECT_NO_THROW(TCreditPlansReader::Load(path, restrictions, &plans));

        TPriceRange creditApprovalRange;
        for (const auto& [id, plan] : plans) {
            if (const auto& planPrice = plan.Restrictions.MinPrice) {
                if (auto& rangePrice = creditApprovalRange.MinPrice) {
                    if (rangePrice->Value < planPrice->Value) {
                        rangePrice->Value = planPrice->Value;
                    }
                } else {
                    rangePrice = TPrice{ planPrice->Value, DefaultCurrency };
                }
            }
            if (const auto& planPrice = plan.Restrictions.MaxPrice) {
                if (auto& rangePrice = creditApprovalRange.MaxPrice) {
                    if (rangePrice->Value > planPrice->Value) {
                        rangePrice->Value = planPrice->Value;
                    }
                } else {
                    rangePrice = TPrice{ planPrice->Value, DefaultCurrency };
                }
            }
        }
        if (creditApprovalRange.MinPrice && creditApprovalRange.MaxPrice) {
            EXPECT_TRUE(creditApprovalRange.MinPrice->Value <= creditApprovalRange.MaxPrice->Value);
        }

        CheckPriceRestrictions(restrictions);
    }
}

TEST(TestCreditPlans, NotStaticLoad) {
    for (const auto& config : CreditPlansConfigs) {
        const auto path = JoinFsPaths(
            ArcadiaSourceRoot(),
            config
        );
        TCreditPlansReader reader;
        EXPECT_NO_THROW(reader.Load(path));
        CheckPriceRestrictions(reader.GetGlobalRestrictions());
    }
}

TEST(TestCreditPlans, ValidJsonWithOneTermLoad) {
    TGlobalRestrictions restrictions;
    TCreditPlans plans;
    const auto path = SRC_("data/credit_plan_with_one_term.json");
    EXPECT_NO_THROW(TCreditPlansReader::Load(path, restrictions, &plans));

    UNIT_ASSERT(restrictions.MinPrice.Defined());
    UNIT_ASSERT_EQUAL(restrictions.MinPrice->Value.AsDouble(), 3000.0);
    UNIT_ASSERT_EQUAL(restrictions.MinPrice->Currency, TCurrency::Rur());
    UNIT_ASSERT(restrictions.MaxPrice.Defined());
    UNIT_ASSERT_EQUAL(restrictions.MaxPrice->Value.AsDouble(), 60000.0);
    UNIT_ASSERT_EQUAL(restrictions.MaxPrice->Currency, TCurrency::Rur());

    UNIT_ASSERT_EQUAL(plans.size(), 1);
    UNIT_ASSERT(plans.contains("1"));
    const auto& plan = plans["1"];
    UNIT_ASSERT_EQUAL(plan.Bank, "Тинькофф");
    UNIT_ASSERT_EQUAL(plan.Rate.AsDouble(), 49.9);
    UNIT_ASSERT_EQUAL(plan.InitialPaymentPercent.AsDouble(), 10.0);
    UNIT_ASSERT_EQUAL(plan.IsThirdPartyAllowed, true);
    UNIT_ASSERT_EQUAL(plan.Terms.size(), 1);
    UNIT_ASSERT_EQUAL(plan.Terms[0].Num, 24);
    UNIT_ASSERT_EQUAL(plan.Terms[0].IsDefault, true);
}

TEST(TestCreditPlans, ValidJsonWithSeveralTermLoad) {
    TGlobalRestrictions restrictions;
    TCreditPlans plans;
    const auto path = SRC_("data/credit_plan_with_several_terms.json");
    EXPECT_NO_THROW(TCreditPlansReader::Load(path, restrictions, &plans));

    UNIT_ASSERT(restrictions.MinPrice.Defined());
    UNIT_ASSERT_EQUAL(restrictions.MinPrice->Value.AsDouble(), 150000.0);
    UNIT_ASSERT_EQUAL(restrictions.MinPrice->Currency, TCurrency::Rur());
    UNIT_ASSERT(restrictions.MaxPrice.Defined());
    UNIT_ASSERT_EQUAL(restrictions.MaxPrice->Value.AsDouble(), 6000000.0);
    UNIT_ASSERT_EQUAL(restrictions.MaxPrice->Currency, TCurrency::Rur());

    UNIT_ASSERT_EQUAL(plans.size(), 1);
    UNIT_ASSERT(plans.contains("1"));
    const auto& plan = plans["1"];
    UNIT_ASSERT_EQUAL(plan.Bank, "Тинькофф");
    UNIT_ASSERT_EQUAL(plan.Rate.AsDouble(), 53.6);
    UNIT_ASSERT_EQUAL(plan.InitialPaymentPercent.AsDouble(),25.0);
    UNIT_ASSERT_EQUAL(plan.IsThirdPartyAllowed, true);
    UNIT_ASSERT_EQUAL(plan.Terms.size(), 3);
    UNIT_ASSERT_EQUAL(plan.Terms[0].Num, 3);
    UNIT_ASSERT_EQUAL(plan.Terms[0].IsDefault, false);
    UNIT_ASSERT_EQUAL(plan.Terms[1].Num, 6);
    UNIT_ASSERT_EQUAL(plan.Terms[1].IsDefault, false);
    UNIT_ASSERT_EQUAL(plan.Terms[2].Num, 12);
    UNIT_ASSERT_EQUAL(plan.Terms[2].IsDefault, true);
}

TEST(TestCreditPlans, InvalidJsonWithoutTermsLoad) {
    TGlobalRestrictions restrictions;
    TCreditPlans plans;
    const auto path = SRC_("data/credit_plan_without_terms.json");
    EXPECT_THROW(TCreditPlansReader::Load(path, restrictions, &plans), yexception);
}

TEST(TestCreditPlans, InvalidJsonWithoutTermInsideTermsLoad) {
    TGlobalRestrictions restrictions;
    TCreditPlans plans;
    const auto path = SRC_("data/credit_plan_with_invalid_terms_1.json");
    EXPECT_THROW(TCreditPlansReader::Load(path, restrictions, &plans), yexception);
}

TEST(TestCreditPlans, InvalidJsonWithoutIsDefaultInsideTermsLoad) {
    TGlobalRestrictions restrictions;
    TCreditPlans plans;
    const auto path = SRC_("data/credit_plan_with_invalid_terms_2.json");
    EXPECT_THROW(TCreditPlansReader::Load(path, restrictions, &plans), yexception);
}

TEST(TestCreditPlans, BlackCategoriesAndSuppliersRestrictions) {
    TGlobalRestrictions restrictions;
    TCreditPlans plans;
    const auto path = SRC_("data/credit_plan_with_black_restrictions.json");
    EXPECT_NO_THROW(TCreditPlansReader::Load(path, restrictions, &plans));

    UNIT_ASSERT(restrictions.MinPrice.Defined());
    UNIT_ASSERT_EQUAL(restrictions.MinPrice->Value.AsDouble(), 2500.0);
    UNIT_ASSERT_EQUAL(restrictions.MinPrice->Currency, TCurrency::Rur());
    UNIT_ASSERT(restrictions.MaxPrice.Defined());
    UNIT_ASSERT_EQUAL(restrictions.MaxPrice->Value.AsDouble(), 650650.0);
    UNIT_ASSERT_EQUAL(restrictions.MaxPrice->Currency, TCurrency::Rur());

    UNIT_ASSERT(restrictions.Categories);
    UNIT_ASSERT_EQUAL(restrictions.Categories->Blacklist, (THashSet<NCreditPlans::TCategoryId>{101, 2002, 30003}));
    UNIT_ASSERT(restrictions.Suppliers);
    UNIT_ASSERT_EQUAL(restrictions.Suppliers->Blacklist, (THashSet<NMarketReport::TShopId>{131, 1331, 13331}));

    UNIT_ASSERT_EQUAL(plans.size(), 1);
    UNIT_ASSERT(plans.contains("1"));
    const auto& plan = plans["1"];

    UNIT_ASSERT(plan.Restrictions.MinPrice.Defined());
    UNIT_ASSERT_EQUAL(plan.Restrictions.MinPrice->Value.AsDouble(), 1200.0);
    UNIT_ASSERT_EQUAL(plan.Restrictions.MinPrice->Currency, TCurrency::Rur());
    UNIT_ASSERT(plan.Restrictions.MaxPrice.Defined());
    UNIT_ASSERT_EQUAL(plan.Restrictions.MaxPrice->Value.AsDouble(), 1200000.0);
    UNIT_ASSERT_EQUAL(plan.Restrictions.MaxPrice->Currency, TCurrency::Rur());

    UNIT_ASSERT(plan.Restrictions.Categories);
    UNIT_ASSERT_EQUAL(plan.Restrictions.Categories->Blacklist, (THashSet<NCreditPlans::TCategoryId>{1101, 22002, 330003}));
    UNIT_ASSERT(plan.Restrictions.Suppliers);
    UNIT_ASSERT_EQUAL(plan.Restrictions.Suppliers->Blacklist, (THashSet<NMarketReport::TShopId>{1131, 11331, 113331}));
}

TEST(TestCreditPlans, WhiteCategoriesAndSuppliersRestrictions) {
    TGlobalRestrictions restrictions;
    TCreditPlans plans;
    const auto path = SRC_("data/credit_plan_with_white_restrictions.json");
    EXPECT_NO_THROW(TCreditPlansReader::Load(path, restrictions, &plans));

    UNIT_ASSERT(restrictions.MinPrice.Defined());
    UNIT_ASSERT_EQUAL(restrictions.MinPrice->Value.AsDouble(), 2500.0);
    UNIT_ASSERT_EQUAL(restrictions.MinPrice->Currency, TCurrency::Rur());
    UNIT_ASSERT(restrictions.MaxPrice.Defined());
    UNIT_ASSERT_EQUAL(restrictions.MaxPrice->Value.AsDouble(), 650650.0);
    UNIT_ASSERT_EQUAL(restrictions.MaxPrice->Currency, TCurrency::Rur());

    UNIT_ASSERT(restrictions.Categories);
    UNIT_ASSERT_EQUAL(restrictions.Categories->Whitelist, (THashSet<NCreditPlans::TCategoryId>{111, 2222, 33333}));
    UNIT_ASSERT(restrictions.Suppliers);
    UNIT_ASSERT_EQUAL(restrictions.Suppliers->Whitelist, (THashSet<NMarketReport::TShopId>{121, 1221, 12221}));

    UNIT_ASSERT_EQUAL(plans.size(), 1);
    UNIT_ASSERT(plans.contains("1"));
    const auto& plan = plans["1"];

    UNIT_ASSERT(plan.Restrictions.MinPrice.Defined());
    UNIT_ASSERT_EQUAL(plan.Restrictions.MinPrice->Value.AsDouble(), 1200.0);
    UNIT_ASSERT_EQUAL(plan.Restrictions.MinPrice->Currency, TCurrency::Rur());
    UNIT_ASSERT(plan.Restrictions.MaxPrice.Defined());
    UNIT_ASSERT_EQUAL(plan.Restrictions.MaxPrice->Value.AsDouble(), 1200000.0);
    UNIT_ASSERT_EQUAL(plan.Restrictions.MaxPrice->Currency, TCurrency::Rur());

    UNIT_ASSERT(plan.Restrictions.Categories);
    UNIT_ASSERT_EQUAL(plan.Restrictions.Categories->Whitelist, (THashSet<NCreditPlans::TCategoryId>{1111, 22222, 333333}));
    UNIT_ASSERT(plan.Restrictions.Suppliers);
    UNIT_ASSERT_EQUAL(plan.Restrictions.Suppliers->Whitelist, (THashSet<NMarketReport::TShopId>{1121, 11221, 112221}));
}

TEST(TestCreditPlans, WhiteAndBlabkSuppliersRestrictions) {
    TGlobalRestrictions restrictions;
    TCreditPlans plans;
    const auto path = SRC_("data/credit_plan_with_both_suppliers_restrictions.json");
    EXPECT_THROW(TCreditPlansReader::Load(path, restrictions, &plans), yexception);
}
