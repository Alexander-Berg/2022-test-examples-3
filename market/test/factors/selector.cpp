#include <library/cpp/testing/unittest/gtest.h>
#include <market/report/library/meta_factors/meta_factors.h>
#include <market/library/base_factors_enum/base_factors_enum.h>
#include <market/report/library/relevance/factors/factors_selector.h>

namespace {
    template <typename E>
    THashSet<E> Set(const TVector<E>& items) {
        THashSet<E> s(items.begin(), items.end());
        return s;
    }
}

namespace NMarketReport {
    namespace NFactors {

        TEST(Factors, SelectFactorsByNameMask_EBaseFactors) {

            using E = EBaseFactors::E;

            ASSERT_EQ(Set<E>({E::DSSM_HARD2}), SelectFactorsByNameMask<E>("DSSM_HARD2"));

            ASSERT_EQ(Set<E>({E::DELIVERY_DOWNLOADABLE, E::DELIVERY_EMPTY, E::DELIVERY_FREE,
                              E::DELIVERY_LOCAL, E::DELIVERY_NO, E::DELIVERY_PRICE, E::DELIVERY_SELF, E::DELIVERY_OFF}),
                      SelectFactorsByNameMask<E>("DELIVERY_.*"));

            ASSERT_EQ(Set<E>({E::CATEG_CTR_ADJ, E::GEO_IP_CTR_ADJ, E::GEO_SETTINGS_CTR_ADJ,
                              E::MODEL_CTR_ADJ, E::OFFER_CTR_ADJ, E::SHOP_CTR_ADJ, E::VENDOR_CTR_ADJ}),
                      SelectFactorsByNameMask<E>(".*_CTR_ADJ"));

            ASSERT_EQ(Set<E>({E::CATEGORY_PARENT_1_ID, E::CATEGORY_PARENT_2_ID}),
                      SelectFactorsByNameMask<E>("\\w+_PARENT_\\d_ID"));

            ASSERT_EQ(Set<E>({E::USER_OFFER_SHOWS_PROMO_PERS,
                              E::USER_OFFER_CLICKS_PROMO_PERS,
                              E::USER_OFFER_CTR_PROMO_PERS,
                              E::USER_OFFER_CTR_DIVERGENCE_PROMO_PERS}),
                      SelectFactorsByNameMask<E>("USER_OFFER_.*_PROMO_PERS"));
        }
    }
}
