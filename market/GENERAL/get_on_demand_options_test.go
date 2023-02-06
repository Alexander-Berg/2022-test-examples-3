package ondemandlite

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/test/requirepb"
	bg "a.yandex-team.ru/market/combinator/pkg/biggeneration"
	"a.yandex-team.ru/market/combinator/pkg/enums"
	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/graph"
	httpclient "a.yandex-team.ru/market/combinator/pkg/http_client"
	"a.yandex-team.ru/market/combinator/pkg/its"
	"a.yandex-team.ru/market/combinator/pkg/lite"
	"a.yandex-team.ru/market/combinator/pkg/ondemand"
	tr "a.yandex-team.ru/market/combinator/pkg/tarifficator"
	"a.yandex-team.ru/market/combinator/pkg/timex"
	"a.yandex-team.ru/market/combinator/pkg/units"
	pb "a.yandex-team.ru/market/combinator/proto/grpc"
)

const (
	cdWarehouseID          = 100
	ffWarehouseID          = 172
	nonOnDemandWarehouseID = 145

	middleMileServiceID = 239
	lastMileServiceID   = 48
	yTaxiServiceID      = 1005471

	userLatitude  = 55.753274
	userLongitude = 37.619402

	cdWarehouseSegmentID = 600000
	cdMovementSegmentID  = 600001

	ffWarehouseSegmentID       = 612910
	middleMileServiceSegmentID = 715541
	yTaxiWarehouseSegmentID    = 715539
	yTaxiMovementSegmentID     = 715538
	yTaxiLinehaulSegmentID     = 714916
	yTaxiHandingSegmentID      = 714915

	lastMileMovementSegmentID = 700000
	lastMileLinehaulSegmentID = 700001
	lastMileHandingSegmentID  = 700002

	nonOnDemandWarehouseSegmentID = 700003
	nonOnDemandMovementSegmentID  = 700004
	nonOnDemandLinehaulSegmentID  = 700005
)

var yTaxiAvailableOptions []int
var yTaxiAvailableOptionsDeliveryRoute []int
var yTaxiAvailableOptionsRaw []float64
var yTaxiAvailableOptionsDeliveryRouteRaw []float64

func init() {
	yTaxiAvailableOptions = []int{0, 3}
	yTaxiAvailableOptionsDeliveryRoute = []int{0}
	yTaxiAvailableOptionsRaw = []float64{0.0, 3.0}
	yTaxiAvailableOptionsDeliveryRouteRaw = []float64{0.0}
}

func MakeTariffsFinder() *tr.TariffsFinder {
	tf := tr.NewTariffsFinder()

	for _, deliveryServiceID := range []uint64{yTaxiServiceID, lastMileServiceID} {
		// shop tariff
		tf.Add(&tr.TariffRT{
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

func MakeOnDemandGraph() *graph.Graph {
	g := graph.NewGraphWithHintsV3(nil)
	defer g.Finish(context.Background())

	// CD warehouse
	cdWarehouse := graph.Node{
		LogisticSegment: graph.LogisticSegment{
			ID:           cdWarehouseSegmentID,
			PartnerLmsID: cdWarehouseID,
			LocationID:   geobase.RegionMoscowAndObl,
			PointLmsID:   10000000000,
			Type:         graph.SegmentTypeWarehouse,
			PartnerType:  enums.PartnerTypeDropship,
		},
		CourierServices: []*graph.LogisticService{
			{
				ID:           2000000,
				SegmentLmsID: cdWarehouseSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeInternal,
				Code:         enums.ServiceCutoff,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("00:00:00", "12:00:00", false),
			},
			{
				ID:           2000001,
				SegmentLmsID: cdWarehouseSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeInternal,
				Code:         enums.ServiceProcessing,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("08:00:00", "19:00:00", false),
			},
			{
				ID:           2000002,
				SegmentLmsID: cdWarehouseSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeOutbound,
				Code:         enums.ServiceShipment,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("08:00:00", "19:00:00", false),
			},
		},
	}
	g.AddNode(cdWarehouse)

	// CD movement
	cdMovement := graph.Node{
		LogisticSegment: graph.LogisticSegment{
			ID:           cdMovementSegmentID,
			PartnerLmsID: cdWarehouseID,
			Type:         graph.SegmentTypeMovement,
			PartnerType:  enums.PartnerTypeDropship,
		},
		CourierServices: []*graph.LogisticService{
			{
				ID:           2000003,
				SegmentLmsID: cdMovementSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeInternal,
				Code:         enums.ServiceMovement,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("10:00:00", "19:00:00", false),
			},
			{
				ID:           2000004,
				SegmentLmsID: cdMovementSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeOutbound,
				Code:         enums.ServiceShipment,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("10:00:00", "19:00:00", false),
			},
		},
	}
	g.AddNode(cdMovement)

	// FF warehouse
	ffWarehouse := graph.Node{
		LogisticSegment: graph.LogisticSegment{
			ID:           ffWarehouseSegmentID,
			PartnerLmsID: ffWarehouseID,
			LocationID:   geobase.RegionSofyno,
			PointLmsID:   10000004403,
			Type:         graph.SegmentTypeWarehouse,
			PartnerType:  enums.PartnerTypeFulfillment,
		},
		CourierServices: []*graph.LogisticService{
			{
				ID:           2792173,
				SegmentLmsID: ffWarehouseSegmentID,
				IsActive:     true,
				Duration:     180,
				Type:         graph.ServiceTypeInternal,
				Code:         enums.ServiceProcessing,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("00:00:00", "23:59:59", false),
			},
			{
				ID:           2827307,
				SegmentLmsID: ffWarehouseSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeInternal,
				Code:         enums.ServiceConsolidation,
				Price:        0,
				Schedule:     lite.CreateEmptySchedule(),
			},
			{
				ID:           2822637,
				SegmentLmsID: ffWarehouseSegmentID,
				IsActive:     true,
				Duration:     180,
				Type:         graph.ServiceTypeOutbound,
				Code:         enums.ServiceShipment,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("00:00:00", "23:59:59", false),
			},
		},
	}
	g.AddNode(ffWarehouse)

	// middle mile movement
	middleMileMovement := graph.Node{
		LogisticSegment: graph.LogisticSegment{
			ID:           middleMileServiceSegmentID,
			PartnerLmsID: middleMileServiceID,
			Type:         graph.SegmentTypeMovement,
			PartnerType:  enums.PartnerTypeDelivery,
		},
		CourierServices: []*graph.LogisticService{
			{
				ID:           3107651,
				SegmentLmsID: middleMileServiceSegmentID,
				IsActive:     true,
				Duration:     90,
				Type:         graph.ServiceTypeInbound,
				Code:         enums.ServiceInbound,
				Price:        0,
				Schedule:     lite.CreateEmptySchedule(),
			},
			{
				ID:           3107653,
				SegmentLmsID: middleMileServiceSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeInternal,
				Code:         enums.ServiceMovement,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("01:30:00", "02:30:00", false),
			},
			{
				ID:           3107652,
				SegmentLmsID: middleMileServiceSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeOutbound,
				Code:         enums.ServiceShipment,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("01:30:00", "02:30:00", false),
			},
		},
	}
	g.AddNode(middleMileMovement)

	// Yandex.Go warehouse
	yTaxiWarehouse := graph.Node{
		LogisticSegment: graph.LogisticSegment{
			ID:           yTaxiWarehouseSegmentID,
			PartnerLmsID: yTaxiServiceID,
			LocationID:   geobase.RegionMoscow,
			PointLmsID:   10000004403,
			Type:         graph.SegmentTypeWarehouse,
			PartnerType:  enums.PartnerTypeDelivery,
		},
		CourierServices: []*graph.LogisticService{
			{
				ID:           3107643,
				SegmentLmsID: yTaxiWarehouseSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeInbound,
				Code:         enums.ServiceInbound,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("09:00:00", "15:00:00", false),
			},
			{
				ID:           3107644,
				SegmentLmsID: yTaxiWarehouseSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeOutbound,
				Code:         enums.ServiceShipment,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("00:00:00", "23:59:59", false),
			},
			{
				ID:           3107645,
				SegmentLmsID: yTaxiWarehouseSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeInternal,
				Code:         enums.ServiceOnDemandYandexGo,
				Price:        0,
				Schedule:     lite.CreateEmptySchedule(),
			},
			{
				ID:           3107739,
				SegmentLmsID: yTaxiWarehouseSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeInternal,
				Code:         enums.ServiceSort,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("00:00:00", "23:59:59", false),
			},
		},
	}
	g.AddNode(yTaxiWarehouse)

	// Yandex.Go movement
	yTaxiMovement := graph.Node{
		LogisticSegment: graph.LogisticSegment{
			ID:           yTaxiMovementSegmentID,
			PartnerLmsID: yTaxiServiceID,
			Type:         graph.SegmentTypeMovement,
			PartnerType:  enums.PartnerTypeDelivery,
		},
		CourierServices: []*graph.LogisticService{
			{
				ID:           3107640,
				SegmentLmsID: yTaxiMovementSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeInbound,
				Code:         enums.ServiceInbound,
				Price:        0,
				Schedule:     lite.CreateEmptySchedule(),
			},
			{
				ID:           3107641,
				SegmentLmsID: yTaxiMovementSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeInternal,
				Code:         enums.ServiceMovement,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("00:00:00", "23:59:59", false), // trivial schedule
			},
			{
				ID:           3107642,
				SegmentLmsID: yTaxiMovementSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeOutbound,
				Code:         enums.ServiceShipment,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("00:00:00", "23:59:59", false), // trivial schedule
			},
		},
	}
	g.AddNode(yTaxiMovement)

	// Yandex.Go linehaul
	yTaxiLinehaul := graph.Node{
		LogisticSegment: graph.LogisticSegment{
			ID:           yTaxiLinehaulSegmentID,
			PartnerLmsID: yTaxiServiceID,
			LocationID:   geobase.RegionMoscow,
			Type:         graph.SegmentTypeLinehaul,
			PartnerType:  enums.PartnerTypeDelivery,
		},
		CourierServices: []*graph.LogisticService{
			{
				ID:           3105470,
				SegmentLmsID: yTaxiLinehaulSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeInternal,
				Code:         enums.ServiceDelivery,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("00:00:00", "23:59:59", false),
			},
			{
				ID:           3105471,
				SegmentLmsID: yTaxiLinehaulSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeInternal,
				Code:         enums.ServiceLastMile,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("11:00:00", "11:00:00", true),
			},
		},
	}
	g.AddNode(yTaxiLinehaul)

	// Yandex.Go handing
	yTaxiHanding := graph.Node{
		LogisticSegment: graph.LogisticSegment{
			ID:           yTaxiHandingSegmentID,
			PartnerLmsID: yTaxiServiceID,
			LocationID:   geobase.RegionMoscow,
			Type:         graph.SegmentTypeHanding,
			PartnerType:  enums.PartnerTypeDelivery,
		},
		CourierServices: []*graph.LogisticService{
			{
				ID:           3105469,
				SegmentLmsID: yTaxiHandingSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeOutbound,
				Code:         enums.ServiceHanding,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("10:00:00", "18:00:00", false),
			},
		},
	}
	g.AddNode(yTaxiHanding)

	// last mile movement
	lastMileMovement := graph.Node{
		LogisticSegment: graph.LogisticSegment{
			ID:           lastMileMovementSegmentID,
			PartnerLmsID: lastMileServiceID,
			Type:         graph.SegmentTypeMovement,
			PartnerType:  enums.PartnerTypeDelivery,
		},
		CourierServices: []*graph.LogisticService{
			{
				ID:           3000000,
				SegmentLmsID: lastMileMovementSegmentID,
				IsActive:     true,
				Duration:     90,
				Type:         graph.ServiceTypeInbound,
				Code:         enums.ServiceInbound,
				Price:        0,
				Schedule:     lite.CreateEmptySchedule(),
			},
			{
				ID:           3000001,
				SegmentLmsID: lastMileMovementSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeInternal,
				Code:         enums.ServiceMovement,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("01:30:00", "02:30:00", false),
			},
			{
				ID:           3000002,
				SegmentLmsID: lastMileMovementSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeOutbound,
				Code:         enums.ServiceShipment,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("01:30:00", "02:30:00", false),
			},
		},
	}
	g.AddNode(lastMileMovement)

	// last mile linehaul
	lastMileLinehaul := graph.Node{
		LogisticSegment: graph.LogisticSegment{
			ID:           lastMileLinehaulSegmentID,
			PartnerLmsID: lastMileServiceID,
			LocationID:   geobase.RegionMoscow,
			Type:         graph.SegmentTypeLinehaul,
			PartnerType:  enums.PartnerTypeDelivery,
		},
		CourierServices: []*graph.LogisticService{
			{
				ID:           3000003,
				SegmentLmsID: lastMileLinehaulSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeInternal,
				Code:         enums.ServiceDelivery,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("00:00:00", "23:59:59", false),
			},
			{
				ID:           3000004,
				SegmentLmsID: lastMileLinehaulSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeInternal,
				Code:         enums.ServiceLastMile,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("11:00:00", "11:00:00", true),
			},
		},
	}
	g.AddNode(lastMileLinehaul)

	// last mile handing
	lastMileHanding := graph.Node{
		LogisticSegment: graph.LogisticSegment{
			ID:           lastMileHandingSegmentID,
			PartnerLmsID: lastMileServiceID,
			LocationID:   geobase.RegionMoscow,
			Type:         graph.SegmentTypeHanding,
			PartnerType:  enums.PartnerTypeDelivery,
		},
		CourierServices: []*graph.LogisticService{
			{
				ID:           3000005,
				SegmentLmsID: lastMileHandingSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeOutbound,
				Code:         enums.ServiceHanding,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("16:00:00", "22:00:00", false),
			},
		},
	}
	g.AddNode(lastMileHanding)

	// FF warehouse with no on-demand delivery option
	nonOnDemandFFWarehouse := graph.Node{
		LogisticSegment: graph.LogisticSegment{
			ID:           nonOnDemandWarehouseSegmentID,
			PartnerLmsID: nonOnDemandWarehouseID,
			LocationID:   geobase.RegionKotelniki,
			PointLmsID:   20000000000,
			Type:         graph.SegmentTypeWarehouse,
			PartnerType:  enums.PartnerTypeFulfillment,
		},
		CourierServices: []*graph.LogisticService{
			{
				ID:           3000006,
				SegmentLmsID: nonOnDemandWarehouseSegmentID,
				IsActive:     true,
				Duration:     180,
				Type:         graph.ServiceTypeInternal,
				Code:         enums.ServiceProcessing,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("00:00:00", "23:59:59", false),
			},
			{
				ID:           3000007,
				SegmentLmsID: nonOnDemandWarehouseSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeInternal,
				Code:         enums.ServiceConsolidation,
				Price:        0,
				Schedule:     lite.CreateEmptySchedule(),
			},
			{
				ID:           3000008,
				SegmentLmsID: nonOnDemandWarehouseSegmentID,
				IsActive:     true,
				Duration:     180,
				Type:         graph.ServiceTypeOutbound,
				Code:         enums.ServiceShipment,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("00:00:00", "23:59:59", false),
			},
		},
	}
	g.AddNode(nonOnDemandFFWarehouse)

	// last mile movement from Marshroute
	nonOnDemandMovement := graph.Node{
		LogisticSegment: graph.LogisticSegment{
			ID:           nonOnDemandMovementSegmentID,
			PartnerLmsID: lastMileServiceID,
			Type:         graph.SegmentTypeMovement,
			PartnerType:  enums.PartnerTypeDelivery,
		},
		CourierServices: []*graph.LogisticService{
			{
				ID:           3000009,
				SegmentLmsID: nonOnDemandMovementSegmentID,
				IsActive:     true,
				Duration:     90,
				Type:         graph.ServiceTypeInbound,
				Code:         enums.ServiceInbound,
				Price:        0,
				Schedule:     lite.CreateEmptySchedule(),
			},
			{
				ID:           3000010,
				SegmentLmsID: nonOnDemandMovementSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeInternal,
				Code:         enums.ServiceMovement,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("01:30:00", "02:30:00", false),
			},
			{
				ID:           3000011,
				SegmentLmsID: nonOnDemandMovementSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeOutbound,
				Code:         enums.ServiceShipment,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("01:30:00", "02:30:00", false),
			},
		},
	}
	g.AddNode(nonOnDemandMovement)

	// last mile linehaul from Marshroute
	nonOnDemandLinehaul := graph.Node{
		LogisticSegment: graph.LogisticSegment{
			ID:           nonOnDemandLinehaulSegmentID,
			PartnerLmsID: lastMileServiceID,
			LocationID:   geobase.RegionMoscow,
			Type:         graph.SegmentTypeLinehaul,
			PartnerType:  enums.PartnerTypeDelivery,
		},
		CourierServices: []*graph.LogisticService{
			{
				ID:           3000012,
				SegmentLmsID: nonOnDemandLinehaulSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeInternal,
				Code:         enums.ServiceDelivery,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("00:00:00", "23:59:59", false),
			},
			{
				ID:           3000013,
				SegmentLmsID: nonOnDemandLinehaulSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeInternal,
				Code:         enums.ServiceLastMile,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("11:00:00", "11:00:00", true),
			},
		},
	}
	g.AddNode(nonOnDemandLinehaul)

	// On-demand route with Yandex.Go as a last mile delivery service
	_ = g.AddEdge(cdWarehouse.ID, cdMovement.ID)
	_ = g.AddEdge(cdMovement.ID, ffWarehouse.ID)
	_ = g.AddEdge(ffWarehouse.ID, middleMileMovement.ID)
	_ = g.AddEdge(middleMileMovement.ID, yTaxiWarehouse.ID)
	_ = g.AddEdge(yTaxiWarehouse.ID, yTaxiMovement.ID)
	_ = g.AddEdge(yTaxiMovement.ID, yTaxiLinehaul.ID)

	// Simple courier route starting at Sofyno
	_ = g.AddEdge(ffWarehouse.ID, lastMileMovement.ID)
	_ = g.AddEdge(lastMileMovement.ID, lastMileLinehaul.ID)

	// Simple courier route starting at Marshroute
	_ = g.AddEdge(nonOnDemandFFWarehouse.ID, nonOnDemandMovement.ID)
	_ = g.AddEdge(nonOnDemandMovement.ID, nonOnDemandLinehaul.ID)

	return g
}

func PrepareHTTPClient(env *lite.Env) {
	yTaxi := env.HTTPClient.YTaxi.(*lite.YTaxiClientMock)
	yTaxi.
		OnCheckAvailableIntervalsRequest(lite.MakeAvailableOptionsRequestBody(
			time.Date(2021, 3, 26, 0, 0, 0, 0, lite.MskTZ),
			5,
			graph.ScheduleWindow{
				// See INBOUND service schedule for yTaxiWarehouse segment
				From: timex.DayTime{Hour: 9, Minute: 0, Second: 0},
				To:   timex.DayTime{Hour: 15, Minute: 0, Second: 0},
			},
			graph.ScheduleWindow{
				// See HANDING service schedule for yTaxiHanding segment
				From: timex.DayTime{Hour: 10, Minute: 0, Second: 0},
				To:   timex.DayTime{Hour: 18, Minute: 0, Second: 0},
			},
			1024,
			[3]uint32{11, 22, 33},
			graph.CargoTypes{},
			units.GpsCoords{
				Latitude:  userLatitude,
				Longitude: userLongitude,
			},
		)).
		RespondWithAvailableIntervals(&httpclient.CheckAvailableOptionsResponse{
			AvailableOptions:    yTaxiAvailableOptions,
			AvailableOptionsRaw: yTaxiAvailableOptionsRaw,
		})
	yTaxi.
		OnCheckAvailableIntervalsRequest(lite.MakeAvailableOptionsRequestBody(
			time.Date(2021, 3, 26, 0, 0, 0, 0, lite.MskTZ),
			1,
			graph.ScheduleWindow{
				// See INBOUND service schedule for yTaxiWarehouse segment
				From: timex.DayTime{Hour: 9, Minute: 0, Second: 0},
				To:   timex.DayTime{Hour: 15, Minute: 0, Second: 0},
			},
			graph.ScheduleWindow{
				// See HANDING service schedule for yTaxiHanding segment
				From: timex.DayTime{Hour: 10, Minute: 0, Second: 0},
				To:   timex.DayTime{Hour: 18, Minute: 0, Second: 0},
			},
			1024,
			[3]uint32{11, 22, 33},
			graph.CargoTypes{},
			units.GpsCoords{
				Latitude:  userLatitude,
				Longitude: userLongitude,
			},
		)).
		RespondWithAvailableIntervals(&httpclient.CheckAvailableOptionsResponse{
			AvailableOptions:    yTaxiAvailableOptionsDeliveryRoute,
			AvailableOptionsRaw: yTaxiAvailableOptionsDeliveryRouteRaw,
		})

	yTaxi.
		OnCheckAvailableIntervalsRequest(lite.MakeAvailableOptionsRequestBody(
			time.Date(2021, 3, 29, 0, 0, 0, 0, lite.MskTZ),
			1,
			graph.ScheduleWindow{
				// See INBOUND service schedule for yTaxiWarehouse segment
				From: timex.DayTime{Hour: 9, Minute: 0, Second: 0},
				To:   timex.DayTime{Hour: 15, Minute: 0, Second: 0},
			},
			graph.ScheduleWindow{
				// See HANDING service schedule for yTaxiHanding segment
				From: timex.DayTime{Hour: 10, Minute: 0, Second: 0},
				To:   timex.DayTime{Hour: 18, Minute: 0, Second: 0},
			},
			1024,
			[3]uint32{11, 22, 33},
			graph.CargoTypes{},
			units.GpsCoords{
				Latitude:  userLatitude,
				Longitude: userLongitude,
			},
		)).
		RespondWithAvailableIntervals(&httpclient.CheckAvailableOptionsResponse{
			AvailableOptions:    yTaxiAvailableOptionsDeliveryRoute,
			AvailableOptionsRaw: yTaxiAvailableOptionsDeliveryRouteRaw,
		})
	yTaxi.
		OnCheckAvailableIntervalsRequest(lite.MakeAvailableOptionsRequestBody(
			time.Date(2021, 3, 26, 0, 0, 0, 0, lite.MskTZ),
			5,
			graph.ScheduleWindow{
				// See INBOUND service schedule for yTaxiWarehouse segment
				From: timex.DayTime{Hour: 9, Minute: 0, Second: 0},
				To:   timex.DayTime{Hour: 15, Minute: 0, Second: 0},
			},
			graph.ScheduleWindow{
				// See HANDING service schedule for yTaxiHanding segment
				From: timex.DayTime{Hour: 10, Minute: 0, Second: 0},
				To:   timex.DayTime{Hour: 18, Minute: 0, Second: 0},
			},
			1024,
			[3]uint32{11, 22, 33},
			graph.CargoTypes{},
			units.GpsCoords{
				Latitude:  userLatitude + 1,
				Longitude: userLongitude,
			},
		)).
		RespondWithError(ondemand.ErrTaxiRequestTimeout)
	yTaxi.
		OnCheckAvailableIntervalsRequest(lite.MakeAvailableOptionsRequestBody(
			time.Date(2021, 3, 29, 0, 0, 0, 0, lite.MskTZ),
			1,
			graph.ScheduleWindow{
				// See INBOUND service schedule for yTaxiWarehouse segment
				From: timex.DayTime{Hour: 9, Minute: 0, Second: 0},
				To:   timex.DayTime{Hour: 15, Minute: 0, Second: 0},
			},
			graph.ScheduleWindow{
				// See HANDING service schedule for yTaxiHanding segment
				From: timex.DayTime{Hour: 10, Minute: 0, Second: 0},
				To:   timex.DayTime{Hour: 18, Minute: 0, Second: 0},
			},
			1024,
			[3]uint32{11, 22, 33},
			graph.CargoTypes{},
			units.GpsCoords{
				Latitude:  userLatitude + 1,
				Longitude: userLongitude,
			},
		)).
		RespondWithError(ondemand.ErrTaxiRequestTimeout)
}

func TestGetOnDemandOptions(t *testing.T) {
	generationData := bg.GenerationData{
		RegionMap:     geobase.NewExample(),
		TariffsFinder: lite.NewFinderSet(MakeTariffsFinder()),
		Graph:         MakeOnDemandGraph(),
	}
	settings, _ := its.NewStringSettingsHolder("{}")
	env, cancel := lite.NewEnv(t, &generationData, settings)
	defer cancel()

	PrepareHTTPClient(env)

	request := lite.MakeRequest(
		lite.RequestWithStartTime(time.Date(2021, 3, 25, 12, 0, 0, 0, lite.MskTZ)),
		lite.RequestWithDeliveryType(pb.DeliveryType_COURIER),
		lite.RequestWithPartner(ffWarehouseID),
		lite.RequestWithRegion(geobase.RegionMoscow),
		lite.RequestWithGpsCoords(userLatitude, userLongitude),
		lite.RequestWithUserInfo(true),
	)
	response, err := env.Client.GetCourierOptions(env.Ctx, request)
	require.NoError(t, err)

	const courierOptionCount = 5
	onDemandOptionCount := len(yTaxiAvailableOptions)
	require.Len(t, response.Options, courierOptionCount+onDemandOptionCount)
	for i := 0; i < courierOptionCount; i++ {
		option := response.Options[i]
		requirepb.Equal(t, pb.DeliverySubtype_ORDINARY, option.DeliverySubtype)
		requirepb.Equal(t, &pb.Date{Year: 2021, Month: 3, Day: uint32(26 + i)}, option.DateFrom)
		requirepb.Equal(t, &pb.Date{Year: 2021, Month: 3, Day: uint32(26 + i)}, option.DateTo)
		requirepb.Equal(t, &pb.Time{Hour: 16}, option.Interval.From)
		requirepb.Equal(t, &pb.Time{Hour: 22}, option.Interval.To)
		require.Equal(t, uint32(lastMileServiceID), option.DeliveryServiceId)
	}
	for i, optionNumber := range yTaxiAvailableOptions {
		option := response.Options[courierOptionCount+i]
		requirepb.Equal(t, pb.DeliverySubtype_ON_DEMAND, option.DeliverySubtype)
		requirepb.Equal(t, &pb.Date{Year: 2021, Month: 3, Day: uint32(26 + optionNumber)}, option.DateFrom)
		requirepb.Equal(t, &pb.Date{Year: 2021, Month: 3, Day: uint32(26 + optionNumber)}, option.DateTo)
		requirepb.Equal(t, &pb.Time{Hour: 10}, option.Interval.From)
		requirepb.Equal(t, &pb.Time{Hour: 18}, option.Interval.To)
		require.Equal(t, uint32(yTaxiServiceID), option.DeliveryServiceId)
	}

	dsResp, dsErr := env.Client.GetCourierDeliveryStatistics(env.Ctx, request)
	require.NoError(t, dsErr)
	require.Len(t, dsResp.DeliveryMethods, 2)
	require.Equal(t, pb.DeliveryMethod_DM_COURIER, dsResp.DeliveryMethods[0])
	require.Equal(t, pb.DeliveryMethod_DM_ON_DEMAND, dsResp.DeliveryMethods[1])
}

func TestOnDemandStats(t *testing.T) {
	generationData := bg.GenerationData{
		RegionMap:     geobase.NewExample(),
		TariffsFinder: lite.NewFinderSet(MakeTariffsFinder()),
		Graph:         MakeOnDemandGraph(),
	}
	settings, _ := its.NewStringSettingsHolder("{}")
	env, cancel := lite.NewEnv(t, &generationData, settings)

	PrepareHTTPClient(env)

	startTime := time.Date(2021, 3, 25, 12, 0, 0, 0, lite.MskTZ)
	req := lite.MakeStatsRequest(
		lite.RequestWithStartTime(startTime),
		lite.RequestWithRegion(geobase.RegionMoscow),
		lite.RequestWithSkuOffer(&lite.SkuOffer{
			ShopSku:    "ffShopSku",
			PartnerID:  ffWarehouseID,
			Weight:     1000,
			Dimensions: [3]uint32{10, 20, 30},
			MarketSku:  "ffMarketSku",
		}),
		lite.RequestWithUserInfo(true),
	)
	resp, err := env.Client.GetOffersDeliveryStats(env.Ctx, req)

	require.NoError(t, err)
	require.Equal(t, 1, len(resp.OffersDelivery))

	courierStats := resp.OffersDelivery[0].CourierStats
	require.NotNil(t, courierStats)
	requirepb.Equal(t, &pb.Date{Year: 2021, Month: 3, Day: 26}, courierStats.DateTo)
	requirepb.Equal(t, &pb.Date{Year: 2021, Month: 3, Day: 26}, courierStats.DateFrom)

	onDemandStats := resp.OffersDelivery[0].OnDemandStats
	require.NotNil(t, onDemandStats)
	requirepb.Equal(t, &pb.Date{Year: 2021, Month: 3, Day: 26}, onDemandStats.DateTo)
	requirepb.Equal(t, &pb.Date{Year: 2021, Month: 3, Day: 26}, onDemandStats.DateFrom)

	cancel()
}
