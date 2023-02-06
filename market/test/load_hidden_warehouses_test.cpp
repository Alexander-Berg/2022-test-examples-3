#include <market/library/hidden_warehouses/hidden_warehouses.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>
 
TEST(TestHiddenWarehouses, LoadHiddenWarehousesConfig) {
    using namespace NMarket::NHiddenWarehouses;

    TVector<TWarehouseId> hiddenWarehouses;
 
    const auto path = SRC_("./../../svn-data/package-data/hidden-warehouses.json");
 
    EXPECT_NO_THROW(LoadHiddenWarehouses(path, hiddenWarehouses));
}
