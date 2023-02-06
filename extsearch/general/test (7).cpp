#include <extsearch/images/robot/tools/price_filtering/price_clusters.h>

#include <library/cpp/testing/unittest/registar.h>

using NImages::PriceClusters;

void Test() {
    UNIT_ASSERT(PriceClusters.size() == 255);

    for (ui8 i = 0; i < 254; ++i)
        UNIT_ASSERT(PriceClusters[i] < PriceClusters[i + 1]);
}

Y_UNIT_TEST_SUITE(PriceCLusters) {
    Y_UNIT_TEST(CorrectPriceClusters) {
        Test();
    }
}
