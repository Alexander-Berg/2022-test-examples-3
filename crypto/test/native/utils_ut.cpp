#include <crypta/lab/lib/native/utils.h>
#include <util/random/random.h>
#include <library/cpp/testing/unittest/registar.h>


Y_UNIT_TEST_SUITE(TFilterIdentifier) {
    Y_UNIT_TEST(TestRate) {
        double skipRate = 0.3;
        TFilterIdentifier filter(skipRate);
        ui64 size = 1000;
        ui64 estimatedFilteredCount = static_cast<ui64>(skipRate * size);
        ui64 filteredCount = 0;

        for (ui64 i = 0; i < size; ++i) {
           TString value = ToString(RandomNumber<ui64>());
           filteredCount += static_cast<ui64>(filter.Filter(value));
        }
        ui64 border = static_cast<ui64>(0.05 * size);
        UNIT_ASSERT(estimatedFilteredCount + border > filteredCount);
        UNIT_ASSERT(filteredCount + border > estimatedFilteredCount);
    }
}
