#include <util/folder/path.h>

#include <library/cpp/testing/unittest/env.h>

#include <market/replenishment/algorithms/vicugna/test_util/world_fixture.h>
#include <market/replenishment/algorithms/vicugna/dao/readers.h>
#include <market/replenishment/algorithms/vicugna/dao/world_builder.h>

using namespace NAlpaca;


void TWorldFixture::BuildSimpleWorld() {
    Builder.AddWarehouse(0, "WarehouseTo 0", 0);
    Builder.AddWarehouse(1, "WarehouseFrom 1", 0);
    Builder.AddSupplier(0, "Supplier 0", TSupplier::EType::OneP);
    Builder.AddRegion(0, "Region 0");
    Builder.AddDeliveryOption(EDeliveryType::Direct, 0, TDayOfWeekSet(EDayOfWeek::Monday), 0, 0, World.GetSuppliers().GetSupplierById(0));
    Builder.AddMsku(
        0,
        {0},
        {{TMsku::TDailyForecast {0, 0}}},
        {
            {{TMsku::TStockItem{0, DateNPOS}}},
            {{TMsku::TStockItem{0, DateNPOS}}}
        },
        { {} },
        { TMskuDeliveryOptionBrief{ Builder.LastDeliveryOption(), 1, 1, 1 } },
        TMsku::TWeightAndDimensions(),
        DateNPOS);
    Builder.AddRecommendation(0);
}

void TWorldFixture::LoadFromYsonRealTables(const TString& dataPath) {
    const TFsPath& path = ArcadiaSourceRoot() + dataPath;
    TTablePaths paths;

    for (const auto& [etable, table] : TablePathsDefaults) {
        paths[etable] = path / (table + ".yson");
    }

    TReadersFactory readers(paths);
    TWorldBuilder builder(readers, World);
    builder.Build();
}

void TWorldFixture::LoadFromYson(const TString& dataPath) {
    const TFsPath& path = ArcadiaSourceRoot() + dataPath;
    TTablePaths paths;

    for (auto table : GetEnumAllValues<EInputTable>()) {
        paths[table] = path / (ToString(table) + ".yson");
    }

    TReadersFactory readers(paths);
    TWorldBuilder builder(readers, World);
    builder.Build();
}
