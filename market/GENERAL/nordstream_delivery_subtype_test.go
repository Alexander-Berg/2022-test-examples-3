package nordstream

import (
	"context"
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/daysoff"
	"a.yandex-team.ru/market/combinator/pkg/enums"
	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/graph"
	"a.yandex-team.ru/market/combinator/pkg/its"
	"a.yandex-team.ru/market/combinator/pkg/outlets"
	"a.yandex-team.ru/market/combinator/pkg/settings"
	tr "a.yandex-team.ru/market/combinator/pkg/tarifficator"
	cr "a.yandex-team.ru/market/combinator/proto/grpc"
)

const (
	ffWarehouseID     = 172
	lastMileServiceID = 48
	yTaxiServiceID    = 1005471
)

func MakeOnDemandTariffsFinder() *tr.TariffsFinder {
	tf := tr.NewTariffsFinder()
	tarifID := 0
	for _, deliveryServiceID := range []uint64{yTaxiServiceID, lastMileServiceID} {
		// shop tariff
		tarifID += 1
		tf.Add(&tr.TariffRT{
			ID:                uint64(tarifID),
			DeliveryServiceID: deliveryServiceID,
			DeliveryMethod:    enums.DeliveryMethodCourier,
			ProgramTypeList:   tr.ProgramTypeList(tr.ProgramMarketDelivery),
			Option: tr.Option{
				Cost:    50,
				DaysMin: 1,
				DaysMax: 1,
			},
			RuleAttrs: tr.RuleAttrs{
				WeightMax: 15000,
				HeightMax: 100,
				LengthMax: 100,
				WidthMax:  100,
				DimSumMax: 400,
			},
			FromToRegions: tr.FromToRegions{
				From: geobase.RegionMoscowAndObl,
				To:   geobase.RegionMoscow,
			},
			Type: tr.RuleTypePayment,
		})

		// global rule
		tf.Add(&tr.TariffRT{
			ID:                uint64(tarifID),
			DeliveryServiceID: deliveryServiceID,
			DeliveryMethod:    enums.DeliveryMethodCourier,
			FromToRegions: tr.FromToRegions{
				From: geobase.RegionMoscowAndObl,
				To:   geobase.RegionMoscow,
			},
			Type: tr.RuleTypeGlobal,
			RuleAttrs: tr.RuleAttrs{
				WeightMax: 10000,
			},
		})
	}

	return tf
}

func makeMinimalOndemandDeliveryGraph(t *testing.T) *graph.Graph {
	pDefaultSchedule, err := graph.CreateAroundTheClockSchedule(false)
	require.NoError(t, err)
	pb := graph.NewPathBuilder()
	pGraph := pb.GetGraph()

	ffWarehouse := pb.AddWarehouse(
		pb.MakeProcessingService(
			pb.WithSchedule(*pDefaultSchedule),
		),
		pb.WithPartnerTypeFulfillment(),
		pb.WithPartnerLmsID(ffWarehouseID),
		pb.WithLocation(geobase.RegionMoscow),
	)
	ffMovement := pb.AddMovement(
		pb.MakeMovementService(
			pb.WithSchedule(*pDefaultSchedule),
		),
	)
	warehouseYTaxi := pb.AddWarehouse(
		pb.MakeProcessingService(
			pb.WithSchedule(*pDefaultSchedule),
		),
		pb.MakeOnDemandYandexGoService(
			pb.WithSchedule(*pDefaultSchedule),
		),
		pb.WithPartnerTypeDelivery(),
		pb.WithLocation(geobase.RegionMoscow),
		pb.WithPartnerLmsID(yTaxiServiceID),
	)
	movementYTaxi := pb.AddMovement(
		pb.MakeMovementService(
			pb.WithSchedule(*pDefaultSchedule),
		),
		pb.WithPartnerLmsID(yTaxiServiceID),
	)
	linehaulYTaxi := pb.AddLinehaul(
		pb.MakeDeliveryService(
			pb.WithSchedule(*pDefaultSchedule),
		),
		pb.WithLocation(geobase.RegionMoscow),
		pb.WithPartnerLmsID(yTaxiServiceID),
	)

	_ = pb.AddHanding(
		pb.MakeHandingService(
			pb.WithSchedule(*pDefaultSchedule),
		),
		pb.WithLocation(geobase.RegionMoscow),
		pb.WithPartnerLmsID(yTaxiServiceID),
	)
	pb.AddEdge(ffWarehouse, ffMovement)
	pb.AddEdge(ffMovement, warehouseYTaxi)
	pb.AddEdge(warehouseYTaxi, movementYTaxi)
	pb.AddEdge(movementYTaxi, linehaulYTaxi)

	pGraph.Finish(context.Background())
	return pGraph
}

func makeOnDemandNordstream(t *testing.T) *NordStream {
	pGraph := makeMinimalOndemandDeliveryGraph(t)

	tariffsBuilder := tr.NewTariffsBuilder()
	tariffsBuilder.MakeTariff(
		tr.TariffWithPartner(uint64(yTaxiServiceID)),
	)

	settingsHolder, err := its.NewStringSettingsHolder("{}")
	require.NoError(t, err)
	return NewNordStream(
		pGraph,
		MakeOnDemandTariffsFinder(),
		outlets.NewOutletStorage(),
		geobase.NewExample(),
		settings.New(settingsHolder.GetSettings(), ""),
		daysoff.NewServicesHashed(),
		nil,
		nil,
	)
}

func checkSubtypeInResult(t *testing.T, result *FinalResult, expected cr.DeliverySubtype) {
	require.Len(t, result.WarehouseRegionLimitsList, 1)
	entry := result.WarehouseRegionLimitsList[0]
	require.Len(t, entry.RegionLimits, 1)
	rlimits := entry.RegionLimits[geobase.RegionMoscow]
	require.Len(t, rlimits, 1)

	atcualSubtype := rlimits[0].DeliverySubtype
	require.Equal(t, expected, atcualSubtype)
}

func TestCalcFlowVer3_OnDemandDeliverySupported(t *testing.T) {
	ns := makeOnDemandNordstream(t)
	options := NewCalcFlowOptions()
	result, err := ns.calcFlowVer3(context.Background(), options)
	require.NoError(t, err)
	checkSubtypeInResult(t, result, cr.DeliverySubtype_ON_DEMAND)
}

func TestCalcFlow_OnDemandDeliverySupported(t *testing.T) {
	ns := makeOnDemandNordstream(t)
	result, err := ns.CalcFlow(context.Background())
	require.NoError(t, err)
	checkSubtypeInResult(t, result, cr.DeliverySubtype_ON_DEMAND)
}

func TestCalcFlowVer2_OnDemandDeliverySupported(t *testing.T) {
	ns := makeOnDemandNordstream(t)
	options := NewCalcFlowOptions()

	result, err := ns.calcFlowVer2(context.Background(), options)
	require.NoError(t, err)
	checkSubtypeInResult(t, result, cr.DeliverySubtype_ON_DEMAND)
}
