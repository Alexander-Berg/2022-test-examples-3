package nordstream

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/daysoff"
	"a.yandex-team.ru/market/combinator/pkg/enums"
	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/graph"
	"a.yandex-team.ru/market/combinator/pkg/its"
	"a.yandex-team.ru/market/combinator/pkg/outlets"
	partdel "a.yandex-team.ru/market/combinator/pkg/partial_delivery"
	"a.yandex-team.ru/market/combinator/pkg/settings"
	"a.yandex-team.ru/market/combinator/pkg/tarifficator"
	pbns "a.yandex-team.ru/market/combinator/proto/nordstream"
)

func TestCollapse(t *testing.T) {
	r1 := pbns.Restriction{
		MaxPhysicalWeight: 20000,
		MaxDimensionsSum:  200,
		MaxDimensions:     []uint32{70, 70, 70},
		MaxPaymentWeight:  20000,
		MinDays:           1,
		MaxDays:           1,
	}
	r2 := pbns.Restriction{
		MaxPhysicalWeight: 20000,
		MaxDimensionsSum:  400,
		MaxDimensions:     []uint32{90, 90, 90},
		MaxPaymentWeight:  20000,
		MinDays:           1,
		MaxDays:           1,
	}

	require.False(t, isRestrictionContainOther(&r1, &r2))
	require.True(t, isRestrictionContainOther(&r2, &r1))

	require.Len(t, collapseRestrictions(nil), 0)
	require.Len(t, collapseRestrictions([]*pbns.Restriction{&r1}), 1)
	require.Len(t, collapseRestrictions([]*pbns.Restriction{&r1, &r2}), 1)
	require.Len(t, collapseRestrictions([]*pbns.Restriction{&r2, &r1}), 1)
}

// COMBINATOR-1541
func TestCollapse1541(t *testing.T) {
	r1 := pbns.Restriction{
		MinPaymentWeight:  30000,
		MaxPaymentWeight:  100000,
		MaxPhysicalWeight: 100000,
		MaxDimensionsSum:  300,
		MaxDimensions:     []uint32{100, 100, 100},
	}
	r2 := pbns.Restriction{
		MaxPaymentWeight:  10000,
		MaxPhysicalWeight: 10000,
		MaxDimensionsSum:  30,
		MaxDimensions:     []uint32{10, 10, 10},
	}

	require.False(t, isRestrictionContainOther(&r1, &r2))
	require.False(t, isRestrictionContainOther(&r2, &r1))

	require.Len(t, collapseRestrictions([]*pbns.Restriction{&r1, &r2}), 2)
	require.Len(t, collapseRestrictions([]*pbns.Restriction{&r2, &r1}), 2)
}

func TestCalcDurationInDays(t *testing.T) {
	date := func(year, month, day, hour int) time.Time {
		return time.Date(year, time.Month(month), day, hour, 0, 0, 0, time.UTC)
	}
	require.Equal(t, 0, calcDurationInDays(
		date(2021, 7, 26, 14),
		date(2021, 7, 26, 20),
	))
	require.Equal(t, 1, calcDurationInDays(
		date(2021, 7, 26, 14),
		date(2021, 7, 27, 12),
	))
	require.Equal(t, 1, calcDurationInDays(
		date(2021, 7, 26, 14),
		date(2021, 7, 27, 16),
	))
	require.Equal(t, 1, calcDurationInDays(
		date(2021, 7, 31, 14),
		date(2021, 8, 1, 16),
	))
}

func TestFlow(t *testing.T) {
	graphEx := graph.NewExample()
	tariffsFinder := tarifficator.NewTariffsFinder()
	tariffsFinder.Add(&tarifficator.TariffRT{
		DeliveryServiceID: uint64(graphEx.LinehaulMoscow.PartnerLmsID),
		DeliveryMethod:    enums.DeliveryMethodCourier,
		FromToRegions:     tarifficator.FromToRegions{From: geobase.RegionMoscowAndObl, To: geobase.RegionMoscow},
		Type:              tarifficator.RuleTypeGlobal,
		RuleAttrs:         tarifficator.RuleAttrs{WeightMax: 10000},
		ProgramTypeList:   tarifficator.ProgramTypeList(tarifficator.ProgramMarketDelivery),
		IsMarketCourier:   true,
	})
	tariffsFinder.Add(&tarifficator.TariffRT{
		DeliveryServiceID: uint64(graphEx.LinehaulMoscow.PartnerLmsID),
		DeliveryMethod:    enums.DeliveryMethodCourier,
		FromToRegions:     tarifficator.FromToRegions{From: geobase.RegionMoscowAndObl, To: geobase.RegionMoscow},
		Type:              tarifficator.RuleTypePayment,
		ProgramTypeList:   tarifficator.ProgramTypeList(tarifficator.ProgramMarketDelivery),
	})
	geo := geobase.NewExample()
	outletStorage := outlets.NewOutletStorage()
	daysOffGrouped := daysoff.NewServicesHashed()

	partdel.Warehouses[145] = true
	defer delete(partdel.Warehouses, 145)

	settingsHolder, err := its.NewStringSettingsHolder("{}")
	require.NoError(t, err)
	ns := NewNordStream(
		graphEx.G,
		tariffsFinder,
		outletStorage,
		geo,
		settings.New(settingsHolder.GetSettings(), ""),
		daysOffGrouped,
		nil,
		nil,
	)
	calcFuncs := []func(ctx context.Context, options CalcFlowOptions) (*FinalResult, error){
		ns.calcFlowVer2,
		ns.calcFlowVer3,
	}
	options := NewCalcFlowOptions()
	const w145 uint64 = 145
	// for i := 0; i < 2; i++ {
	for i, calcFunc := range calcFuncs {
		result, err := calcFunc(context.Background(), options)
		require.NoError(t, err)

		require.Len(t, result.WarehouseRegionLimitsList, 1)
		entry := result.WarehouseRegionLimitsList[0]
		require.Equal(t, graphEx.Warehouse.ID, entry.WarehouseID)
		require.Len(t, entry.RegionLimits, 1)
		rlimits := entry.RegionLimits[geobase.RegionMoscow]
		require.Len(t, rlimits, 1)
		require.Equal(t, 10000, int(rlimits[0].GlobalRule.WeightMax))

		delivery := result.ConvertToCombinatorDelivery()
		// Warehouses
		require.NotNil(t, delivery)
		require.Len(t, delivery.Warehouses, 0)
		// WarehouseDeliveryList
		require.Len(t, delivery.WarehouseDeliveryList, 1)
		require.Equal(t, graphEx.Warehouse.ID, int64(delivery.WarehouseDeliveryList[0].WarehouseId))
		require.Len(t, delivery.WarehouseDeliveryList[0].RegionDeliveryList, 1)
		require.Len(t, delivery.WarehouseDeliveryList[0].RegionDeliveryList[0].Restrictions, 1)
		require.Equal(t,
			uint32(pbns.TariffUserMask_B2C),
			delivery.WarehouseDeliveryList[0].RegionDeliveryList[0].Restrictions[0].TariffUserMask,
		)
		require.True(t, delivery.WarehouseDeliveryList[0].RegionDeliveryList[0].Restrictions[0].IsTryingAvailable)
		// SupplierDeliveryList
		if i != 0 {
			require.Len(t, delivery.SupplierDeliveryList, 1)
			require.Equal(t, w145, delivery.SupplierDeliveryList[0].SupplierWarehouse)
			require.Equal(t, graphEx.Warehouse.ID, int64(delivery.SupplierDeliveryList[0].DeliveryWarehouse))
			require.Equal(t, w145, delivery.SupplierDeliveryList[0].FulfillmentWarehouse)
			require.Equal(t, uint32(geobase.RegionKotelniki), delivery.SupplierDeliveryList[0].FirstMarketRegionId)
			require.True(t, delivery.SupplierDeliveryList[0].IsTryingAvailable)
		}
	}
}

func TestIgnoreAvia(t *testing.T) { // COMBINATOR-2414 игнорировать авиа доставку
	graphEx := graph.NewCourierAlternativeExample()
	tariffsFinder := tarifficator.NewTariffsFinder()
	tariffOptions := [2]*tarifficator.TariffRT{
		{
			DeliveryServiceID: 106,
			RuleAttrs:         tarifficator.RuleAttrs{WeightMax: 10000},
			Option: tarifficator.Option{
				DaysMin: 11,
				DaysMax: 12,
			},
		},
		{
			DeliveryServiceID: 139,
			RuleAttrs:         tarifficator.RuleAttrs{WeightMax: 20000},
			Option: tarifficator.Option{
				DaysMin: 1,
				DaysMax: 2,
			},
		},
	}
	for i, to := range tariffOptions {
		tariffsFinder.Add(&tarifficator.TariffRT{
			ID:                uint64(i),
			DeliveryServiceID: to.DeliveryServiceID,
			DeliveryMethod:    enums.DeliveryMethodCourier,
			FromToRegions:     tarifficator.FromToRegions{From: geobase.RegionMoscowAndObl, To: geobase.RegionMoscow},
			Type:              tarifficator.RuleTypeGlobal,
			RuleAttrs:         to.RuleAttrs,
		})
		tariffsFinder.Add(&tarifficator.TariffRT{
			ID:                uint64(i),
			DeliveryServiceID: to.DeliveryServiceID,
			DeliveryMethod:    enums.DeliveryMethodCourier,
			FromToRegions:     tarifficator.FromToRegions{From: geobase.RegionMoscowAndObl, To: geobase.RegionMoscow},
			Type:              tarifficator.RuleTypePayment,
			Option:            to.Option,
		})
	}
	outlets := outlets.NewOutletStorage()
	geo := geobase.NewExample()
	daysOffGrouped := daysoff.NewServicesHashed()

	settingsHolder, err := its.NewStringSettingsHolder("{}")
	require.NoError(t, err)

	ignore := []bool{true, false}
	options := NewCalcFlowOptions()
	for _, ignoreAvia := range ignore {
		graph.GetFastDeliveryServices()[139] = ignoreAvia
		ns := NewNordStream(
			graphEx.G,
			tariffsFinder,
			outlets,
			geo,
			settings.New(settingsHolder.GetSettings(), ""),
			daysOffGrouped,
			nil,
			nil,
		)
		result, err := ns.calcFlowVer3(context.Background(), options)
		require.NoError(t, err)

		require.Len(t, result.WarehouseRegionLimitsList, 1)
		entry := result.WarehouseRegionLimitsList[0]
		require.Equal(t, graphEx.Warehouse.ID, entry.WarehouseID)
		require.Len(t, entry.RegionLimits, 1)
		rlimits := entry.RegionLimits[geobase.RegionMoscow]
		require.NotEmpty(t, rlimits)
		// лимиты для 106й службы в обоих случаях
		require.Equal(t, 10000, int(rlimits[0].GlobalRule.WeightMax))
		require.Equal(t, 106, int(rlimits[0].GlobalRule.DeliveryServiceID))
		require.Equal(t, 11, int(rlimits[0].DaysMin))
		require.Equal(t, 12, int(rlimits[0].DaysMax))
		if ignoreAvia {
			require.Len(t, rlimits, 1)
		} else {
			// Проверяем, что курьерка была бы доступна, если бы не была авиа
			require.Len(t, rlimits, 2)
			require.Equal(t, 20000, int(rlimits[1].GlobalRule.WeightMax))
			require.Equal(t, 139, int(rlimits[1].GlobalRule.DeliveryServiceID))
			require.Equal(t, 1, int(rlimits[1].DaysMin))
			require.Equal(t, 2, int(rlimits[1].DaysMax))
		}
	}
}
