#include <crypta/lib/native/range/packs.h>

#include <library/cpp/testing/unittest/registar.h>
#include <util/generic/vector.h>

Y_UNIT_TEST_SUITE(TPacks) {
    Y_UNIT_TEST(TwoIters) {
        TVector<size_t> v;
        for (size_t i = 0; i < 14; i++) {
            v.push_back(i);
        }

        const size_t packSize = 3;
        size_t packsCount = 0;
        for (auto pack : GetPacks(v.begin(), v.end(), packSize)) {
            size_t currPacksize = pack.size();
            size_t expectedCurrPacksize = (packsCount + 1) * packSize <= v.size() ? packSize : v.size() - packsCount * packSize;

            UNIT_ASSERT_EQUAL(packsCount * packSize, *pack.begin());
            UNIT_ASSERT_EQUAL(expectedCurrPacksize, currPacksize);
            packsCount++;
        }
    }

    Y_UNIT_TEST(Vector) {
        TVector<size_t> v;
        for (size_t i = 0; i < 280; i++) {
            v.push_back(i);
        }

        const size_t packSize = 17;
        size_t packsCount = 0;
        for (auto pack : GetPacks(v, packSize)) {
            size_t currPacksize = pack.size();
            size_t expectedCurrPacksize = (packsCount + 1) * packSize <= v.size() ? packSize : v.size() - packsCount * packSize;

            UNIT_ASSERT_EQUAL(packsCount * packSize, *pack.begin());
            UNIT_ASSERT_EQUAL(expectedCurrPacksize, currPacksize);
            packsCount++;
        }
    }

    Y_UNIT_TEST(IntegralPacksCount) {
        TVector<size_t> v;
        for (size_t i = 0; i < 100; i++) {
            v.push_back(i);
        }

        const size_t packSize = 10;
        size_t packsCount = 0;
        for (auto pack : GetPacks(v, packSize)) {
            size_t currPacksize = pack.size();
            size_t expectedCurrPacksize = (packsCount + 1) * packSize <= v.size() ? packSize : v.size() - packsCount * packSize;

            UNIT_ASSERT_EQUAL(packsCount * packSize, *pack.begin());
            UNIT_ASSERT_EQUAL(expectedCurrPacksize, currPacksize);
            packsCount++;
        }
    }

    Y_UNIT_TEST(PacksSizeEqVectorSize) {
        TVector<size_t> v;
        for (size_t i = 0; i < 100; i++) {
            v.push_back(i);
        }

        const size_t packSize = 10;
        size_t packsCount = 0;
        for (auto pack : GetPacks(v, packSize)) {
            size_t currPacksize = pack.size();
            size_t expectedCurrPacksize = (packsCount + 1) * packSize <= v.size() ? packSize : v.size() - packsCount * packSize;

            UNIT_ASSERT_EQUAL(packsCount * packSize, *pack.begin());
            UNIT_ASSERT_EQUAL(expectedCurrPacksize, currPacksize);
            packsCount++;
        }
    }

    Y_UNIT_TEST(PacksSizeGVectorSize) {
        TVector<size_t> v;
        for (size_t i = 0; i < 10; i++) {
            v.push_back(i);
        }

        const size_t packSize = 100;
        size_t packsCount = 0;
        for (auto pack : GetPacks(v, packSize)) {
            size_t currPacksize = pack.size();
            size_t expectedCurrPacksize = (packsCount + 1) * packSize <= v.size() ? packSize : v.size() - packsCount * packSize;

            UNIT_ASSERT_EQUAL(packsCount * packSize, *pack.begin());
            UNIT_ASSERT_EQUAL(expectedCurrPacksize, currPacksize);
            packsCount++;
        }
    }
}
