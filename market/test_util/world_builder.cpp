#include <market/replenishment/algorithms/vicugna/test_util/world_builder.h>

#include <sstream>

using namespace NAlpaca;


TTestWorldBuilder& TTestWorldBuilder::AddWarehouse(TWarehouseId warehouseId, TStringBuf warehouseName, TMaybe<TRegionId> regionId) {
    World.GetWarehouses().Add(TWarehouse(warehouseId, warehouseName, regionId));
    return *this;
}

TTestWorldBuilder& TTestWorldBuilder::AddSupplier(TSupplierId id, TStringBuf rsId, TSupplier::EType type) {
    World.GetSuppliers().push_back(TSupplier(id, rsId, type));
    World.GetSuppliers().Init();
    return *this;
}

TTestWorldBuilder& TTestWorldBuilder::AddDeliveryOption(
    EDeliveryType deliveryType,
    size_t leadTime,
    TDayOfWeekSet allowedWeekDays,
    double minParty,
    TWarehouseId warehouseId,
    const TSupplier* supplier)
{
    World.GetDeliveryOptions().push_back(
        TDeliveryOption(
            deliveryType,
            leadTime,
            allowedWeekDays, 
            minParty, 
            World.GetWarehouses().FindIndex(warehouseId), 
            supplier,
            Nothing(),
            Nothing(),
            Nothing(),
            Nothing(),
            Nothing(),
            false));
    return *this;
}

TTestWorldBuilder& TTestWorldBuilder::AddRegion(
    TRegionId regionId,
    TStringBuf name)
{
    World.GetRegions().Add(TRegion(regionId, name, World.GetWarehouses()));
    return *this;
}

TTestWorldBuilder& TTestWorldBuilder::AddMsku(
    TMskuId mskuId,
    const TVector<TSupplierId>& supplierIds,
    const TVector<TVector<TMsku::TDailyForecast>>& forecastByDayByRegion,
    const TVector<TVector<TVector<TMsku::TStockItem>>> initialStockBySupplierByWarehouse,
    const TVector<TMsku::TDelivery>& deliveries,
    const TVector<TMskuDeliveryOptionBrief>& deliveryOptions,
    const TMsku::TWeightAndDimensions& weightAndDimensions,
    size_t lifetime,
    const TVector<TVector<TString>> sskuMappings,
    const TVector<TVector<TMsku::TDailyManualStockModel>>& manualStockModel
    )
{
    TVector<const TSupplier*> suppliers;
    for (const auto supplierId: supplierIds) {
        const auto supplierPtr = World.GetSuppliers().GetSupplierById(supplierId);
        if (supplierPtr == nullptr) {
            ythrow yexception() << "Supplier with id " << supplierId << " doesn't exist!";
        }
        suppliers.push_back(supplierPtr);
    }

    const TVector<double> emptyMarketShare;

    World.GetMskus().emplace_back(MakeHolder<TMsku>(
        mskuId,
        World.GetRegions(),
        suppliers,
        forecastByDayByRegion,
        0.,
        initialStockBySupplierByWarehouse,
        deliveries,
        manualStockModel,
        emptyMarketShare,
        deliveryOptions,
        sskuMappings,
        lifetime,
        weightAndDimensions,
        EAbc::Undefined,
        true));
    return *this;
}

TDeliveryOption& TTestWorldBuilder::LastDeliveryOption() {
    if (World.GetDeliveryOptions().size() == 0) {
        ythrow yexception() << "Delivery options list is empty! Msku delivery option is added to last added delivery option, so it must exist.";
    }
    return World.GetDeliveryOptions().back();
}

// Adds recommendation for last added delivery option
TTestWorldBuilder& TTestWorldBuilder::AddRecommendation(int date)
{
    const auto& lastDeliveryOption = LastDeliveryOption();
    TMskuDeliveryOptionPtrList mskuDeliveryOptionPtrList;
    for (auto& msku: World.GetMskus()) {
        for (auto& mskuOption: msku->GetDeliveryOptions()) {
            if (mskuOption.DeliveryOption == lastDeliveryOption) {
                mskuDeliveryOptionPtrList.push_back(&mskuOption);
            }
        }
    }

    TVector<TDeliveryLimiterGroup*> limiters;

    auto *limiter = World.GetDeliveryLimiter().GetGroup(
        lastDeliveryOption.WarehouseIndex,
        {},
        lastDeliveryOption.Supplier->Type,
        World.GetStartDate() + date
    );

    if (limiter != nullptr) {
        limiters.push_back(limiter);
    }

    World.GetRecommendations().push_back(MakeHolder<TRecommendation>(
        date, 
        date,
        World.GetPlanningPeriod(), 
        lastDeliveryOption, 
        mskuDeliveryOptionPtrList,
        limiters
    ));

    const auto& recommendation = World.GetRecommendations().back();
    for (const auto& line: recommendation->GetLines()) {
        World.GetRecommendationLines().push_back(line.Get());
        World.GetAllowedRecommendationLines().push_back(line.Get());
    }
    return *this;
}
