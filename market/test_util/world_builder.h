#pragma once

#include <market/replenishment/algorithms/vicugna/domain/world.h>
#include <market/replenishment/algorithms/vicugna/util/rng.h>

using namespace NAlpaca;

class TTestWorldBuilder {
public:
    TTestWorldBuilder(TWorld& world)
        : World(world)
    {}

    TTestWorldBuilder& AddWarehouse(TWarehouseId warehouseId, TStringBuf warehouseName, TMaybe<TRegionId> regionId);

    TTestWorldBuilder& AddSupplier(TSupplierId id, TStringBuf rsId, TSupplier::EType type);

    TTestWorldBuilder& AddDeliveryOption(
        EDeliveryType deliveryType,
        size_t leadTime,
        TDayOfWeekSet allowedWeekDays,
        double minParty,
        TWarehouseId warehouseId,
        const TSupplier* supplier);

    TTestWorldBuilder& AddRegion(
        TRegionId regionId,
        TStringBuf name);

    TTestWorldBuilder& AddMsku(
        TMskuId mskuId,
        const TVector<TSupplierId>& supplierIds,
        const TVector<TVector<TMsku::TDailyForecast>>& forecastByDayByRegion,
        const TVector<TVector<TVector<TMsku::TStockItem>>> initialStockBySupplierByWarehouse,
        const TVector<TMsku::TDelivery>& deliveries,
        const TVector<TMskuDeliveryOptionBrief>& deliveryOptions,
        const TMsku::TWeightAndDimensions& weightAndDimensions,
        size_t lifetime=DateNPOS,
        const TVector<TVector<TString>> sskuMappings={},
        const TVector<TVector<TMsku::TDailyManualStockModel>>& manualStockModel = {}
        );

    TDeliveryOption& LastDeliveryOption();

    // Adds recommendation for last added delivery option
    TTestWorldBuilder& AddRecommendation(int date);

    TWorld& World;
    TRng Rng;
};
