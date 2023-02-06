#include <library/cpp/testing/unittest/registar.h>
#include <market/report/src/factors/top_omm_factors_implementation.h>

using namespace NMarketReport::NFactors;

const int MODEL_ID = 1;
const int CATEGORY_ID = 2;

Y_UNIT_TEST_SUITE(TUnitTest) {
    Y_UNIT_TEST(TestFactorsWhenAllInitialized) {
        TOMMFactors ommFactors;
        ommFactors.AddFactorAtId(MODEL_ID, EOmmFactorId::HISTORY_MARKET_MODEL, 1);
        ommFactors.AddFactorAtId(CATEGORY_ID, EOmmFactorId::USER_COUNT_CATEGORY_VIEW, 2);
        ommFactors.AddFactorAtId(CATEGORY_ID, EOmmFactorId::USER_RATIO_CATEGORY_VIEW, 3);
        ommFactors.AddFactorAtId(CATEGORY_ID, EOmmFactorId::USER_PERIOD_CATEGORY_UNSEEN, 4);
        ommFactors.AddFactorAtId(MODEL_ID, EOmmFactorId::USER_PERIOD_MODEL_UNSEEN, 5);

        auto factors = ommFactors.GetFactors(MODEL_ID, CATEGORY_ID);
        TVector<int> factorsExpected = {1, 2, 3, 4, 5};

        UNIT_ASSERT_VALUES_EQUAL(factors.size(), factorsExpected.size());
        for (size_t i = 0; i < factors.size(); ++i) {
            UNIT_ASSERT_DOUBLES_EQUAL(factors[i], factorsExpected[i], 1e-9);
        }
    }

    Y_UNIT_TEST(TestFactorsWhenSomeUninitialized) {
        TOMMFactors ommFactors;
        ommFactors.AddFactorAtId(MODEL_ID, EOmmFactorId::HISTORY_MARKET_MODEL, 1);
        ommFactors.AddFactorAtId(CATEGORY_ID, EOmmFactorId::USER_PERIOD_CATEGORY_UNSEEN, 4);

        auto factors = ommFactors.GetFactors(MODEL_ID, CATEGORY_ID);
        TVector<int> factorsExpected = {1, 0, 0, 4, 0};

        UNIT_ASSERT_VALUES_EQUAL(factors.size(), factorsExpected.size());
        for (size_t i = 0; i < factors.size(); ++i) {
            UNIT_ASSERT_DOUBLES_EQUAL(factors[i], factorsExpected[i], 1e-9);
        }
    }

    Y_UNIT_TEST(TestFactorsWhenAllUninitialized) {
        TOMMFactors ommFactors;

        auto factors = ommFactors.GetFactors(MODEL_ID, CATEGORY_ID);
        TVector<int> factorsExpected = {0, 0, 0, 0, 0};

        UNIT_ASSERT_VALUES_EQUAL(factors.size(), factorsExpected.size());
        for (size_t i = 0; i < factors.size(); ++i) {
            UNIT_ASSERT_DOUBLES_EQUAL(factors[i], factorsExpected[i], 1e-9);
        }
    }
}