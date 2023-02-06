package lite

import (
	"testing"
	"time"

	"github.com/golang/protobuf/ptypes/timestamp"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/test/requirepb"
	bg "a.yandex-team.ru/market/combinator/pkg/biggeneration"
	"a.yandex-team.ru/market/combinator/pkg/enums"
	"a.yandex-team.ru/market/combinator/pkg/fashion"
	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/graph"
	"a.yandex-team.ru/market/combinator/pkg/its"
	"a.yandex-team.ru/market/combinator/pkg/outlets"
	partdel "a.yandex-team.ru/market/combinator/pkg/partial_delivery"
	tr "a.yandex-team.ru/market/combinator/pkg/tarifficator"
	"a.yandex-team.ru/market/combinator/pkg/timex"
	pb "a.yandex-team.ru/market/combinator/proto/grpc"
)

func CheckIntervalsForExisting(t *testing.T, resp *pb.DeliveryRoute) {
	for _, point := range resp.Route.Points {
		for _, serv := range point.Services {
			if enums.GetLmsServiceCode(serv.Code) == enums.ServiceHanding &&
				graph.GetSegmentType(point.SegmentType) == graph.SegmentTypeHanding {
				require.NotEqual(t, 0, len(serv.DeliveryIntervals))
				interval := serv.DeliveryIntervals[0]
				require.NotNil(t, interval)
				require.NotNil(t, interval.From)
				require.NotNil(t, interval.To)
				require.NotEqual(t, &pb.DeliveryInterval{}, interval)
				require.NotEqual(t, &pb.Time{}, interval.From)
				require.NotEqual(t, &pb.Time{}, interval.To)
			}
		}
	}
}

func TestGetCourierDeliveryRoute(t *testing.T) {
	graphEx := graph.NewExample()
	shopTariff := newShopTariff(&tariffSettings{
		deliveryMethod:    enums.DeliveryMethodCourier,
		deliveryServiceID: 106,
		cost:              420,
		daysMin:           5,
		daysMax:           7,
		ruleType:          tr.RuleTypePayment,
	})
	shopTariff.IsMarketCourier = true
	tariffsFinder := tr.NewTariffsFinder()
	tariffsFinder.Add(&shopTariff)

	genData := bg.GenerationData{
		RegionMap:     geobase.NewExample(),
		TariffsFinder: NewFinderSet(tariffsFinder),
		Graph:         graphEx.G,
	}

	startTime := time.Date(2020, 06, 02, 12, 4, 5, 0, time.UTC)

	dest := pb.PointIds{
		RegionId: 213,
	}

	reqSettings := newRequestSettings(requestSettings{
		startTime:  startTime,
		cost:       uint32(shopTariff.Cost),
		dest:       &dest,
		dType:      pb.DeliveryType_COURIER,
		dayTo:      2 + shopTariff.DaysMax,
		cargoTypes: []uint32{200},
	})
	req := PrepareDeliveryRequest(reqSettings)

	env, cancel := NewEnv(t, &genData, nil)
	defer cancel()

	wantDateFrom := startTime.Add(time.Hour * 24 * time.Duration(shopTariff.DaysMax))
	wantDateTo := startTime.Add(time.Hour * 24 * time.Duration(shopTariff.DaysMax))

	resp, err := env.Client.GetDeliveryRoute(env.Ctx, req)

	require.NoError(t, err)
	require.Equal(t, pb.DeliverySubtype_ORDINARY, resp.DeliverySubtype)
	require.NotNil(t, resp.Route)
	require.Equal(t, int(shopTariff.Cost+11+22+333), int(resp.Route.CostForShop))
	requirepb.Equal(t, ToProtoDate(wantDateFrom), resp.Route.DateFrom)
	requirepb.Equal(t, ToProtoDate(wantDateTo), resp.Route.DateTo)
	var moveServices int
	for _, seg := range resp.Route.Points {
		for _, s := range seg.Services {
			if s.Code == "MOVEMENT" {
				moveServices += 1
			}
		}
	}
	require.Equal(t, 1, moveServices)

	var expectedWeight uint32
	for i, item := range req.Items {
		expectedWeight += item.Weight * item.RequiredCount
		requirepb.Equal(t, item.CargoTypes, resp.Offers[i].CargoTypes) // COMBINATOR-3589 Добавляем карготипы в ответ
	}
	expectedDimensions := []uint32{uint32(20), uint32(30), uint32(35)}

	require.NotNil(t, resp.VirtualBox)
	require.Equal(t, expectedWeight, resp.VirtualBox.Weight)
	require.Equal(t, expectedDimensions, resp.VirtualBox.Dimensions)
	CheckIntervalsForExisting(t, resp)

	FashionTryingServiceTest(t, env, &startTime, &dest, &shopTariff, pb.DeliveryType_COURIER)
}

func TestGetPickupDeliveryRoute(t *testing.T) {
	graphEx := graph.NewExample()
	shopTariff1 := newShopTariff(&tariffSettings{
		deliveryMethod:    enums.DeliveryMethodPickup,
		deliveryServiceID: 106,
		cost:              430,
		daysMin:           0,
		daysMax:           0,
		ruleType:          tr.RuleTypePayment,
	})
	shopTariff2 := newShopTariff(&tariffSettings{
		deliveryMethod:    enums.DeliveryMethodPickup,
		deliveryServiceID: 106,
		pickupIDs:         []int64{10000971018},
		ruleType:          tr.RuleTypeForPoint,
	})
	tariffsFinder := tr.NewTariffsFinder()
	tariffsFinder.Add(&shopTariff1)
	tariffsFinder.Add(&shopTariff2)

	regionMap := geobase.NewExample()
	genData := bg.GenerationData{
		RegionMap:     regionMap,
		TariffsFinder: NewFinderSet(tariffsFinder),
		Outlets: outlets.Make([]outlets.Outlet{
			outlets.Outlet{
				ID:                        10000971018,
				RegionID:                  geobase.RegionID(213), // Needed to deduce region
				Type:                      outlets.Depot,
				IsMarketBranded:           true,
				DeliveryServiceOutletCode: "test", // COMBINATOR-2290: ignore unless dsbs
			},
		}, &regionMap, nil),
		Graph: graphEx.G,
	}

	startTime := time.Date(2020, 06, 02, 12, 4, 5, 0, time.UTC)

	dest := pb.PointIds{
		LogisticPointId: 10000971018,
	}
	reqSettings := newRequestSettings(requestSettings{
		startTime: startTime,
		cost:      uint32(shopTariff1.Cost),
		dest:      &dest,
		dType:     pb.DeliveryType_PICKUP,
		dayTo:     2 + shopTariff1.DaysMax,
	})
	req := PrepareDeliveryRequest(reqSettings)

	env, cancel := NewEnv(t, &genData, nil)
	defer cancel()

	wantDateFrom := startTime.Add(time.Hour * 24 * time.Duration(shopTariff1.DaysMax))
	wantDateTo := startTime.Add(time.Hour * 24 * time.Duration(shopTariff1.DaysMax))

	resp, err := env.Client.GetDeliveryRoute(env.Ctx, req)

	require.NoError(t, err)
	require.Equal(t, pb.DeliverySubtype_ORDINARY, resp.DeliverySubtype)
	require.NotNil(t, resp.Route)
	require.Equal(t, int(shopTariff1.Cost+11+222+33), int(resp.Route.CostForShop))
	requirepb.Equal(t, ToProtoDate(wantDateFrom), resp.Route.DateFrom)
	requirepb.Equal(t, ToProtoDate(wantDateTo), resp.Route.DateTo)
	var moveServices int
	for _, seg := range resp.Route.Points {
		require.Empty(t, seg.Ids.DsbsPointId) // COMBINATOR-2290: ignore unless dsbs
		for _, s := range seg.Services {
			if s.Code == "MOVEMENT" {
				moveServices += 1
			}
		}
	}
	require.Equal(t, 1, moveServices)

	var expectedWeight uint32
	for _, item := range req.Items {
		expectedWeight += item.Weight * item.RequiredCount
	}
	expectedDimensions := []uint32{uint32(20), uint32(30), uint32(35)}

	require.NotNil(t, resp.VirtualBox)
	require.Equal(t, expectedWeight, resp.VirtualBox.Weight)
	require.Equal(t, expectedDimensions, resp.VirtualBox.Dimensions)
	CheckIntervalsForExisting(t, resp)

	// B2B user unsupported
	reqSettings.b2b = true
	req = PrepareDeliveryRequest(reqSettings)
	resp, err = env.Client.GetDeliveryRoute(env.Ctx, req)
	require.Error(t, err)
	require.Nil(t, resp)

	reqSettings = newRequestSettings(requestSettings{
		startTime: startTime,
		cost:      uint32(shopTariff1.Cost),
		dest:      &dest,
		dType:     pb.DeliveryType_PICKUP,
		pMethods:  []pb.PaymentMethod{pb.PaymentMethod_CASH},
		dayTo:     2 + shopTariff1.DaysMax,
	})
	// No suitable payment method
	req = PrepareDeliveryRequest(reqSettings)
	resp, err = env.Client.GetDeliveryRoute(env.Ctx, req)

	require.Error(t, err)
	require.Nil(t, resp)

	reqSettings = newRequestSettings(requestSettings{
		startTime: startTime,
		dest:      &dest,
		dType:     pb.DeliveryType_PICKUP,
		dayTo:     2 + shopTariff1.DaysMax,
	})
	// cost == 0 leads to choosing the cheapest option
	req = PrepareDeliveryRequest(reqSettings)
	resp, err = env.Client.GetDeliveryRoute(env.Ctx, req)

	require.NoError(t, err)
	require.Equal(t, pb.DeliverySubtype_ORDINARY, resp.DeliverySubtype)
	require.NotNil(t, resp.Route)
	CheckIntervalsForExisting(t, resp)

	FashionTryingServiceTest(t, env, &startTime, &dest, &shopTariff2, pb.DeliveryType_PICKUP)

	reqSettings = newRequestSettings(requestSettings{
		startTime: startTime,
		dest:      &dest,
		dType:     pb.DeliveryType_PICKUP,
		dayTo:     3 + shopTariff1.DaysMax,
	})
	// cost == 0 leads to choosing the cheapest option
	req = PrepareDeliveryRequest(reqSettings)
	resp, err = env.Client.GetDeliveryRoute(env.Ctx, req)

	require.NoError(t, err)
	require.Equal(t, pb.DeliverySubtype_ORDINARY, resp.DeliverySubtype)
	require.NotNil(t, resp.Route)
	CheckIntervalsForExisting(t, resp)
}

func TestGetDeliveryRoute2WavesCollapsedSchedule(t *testing.T) {
	graphEx := graph.NewExampleWith2SchedSC()
	shopTariff := newShopTariff(&tariffSettings{
		deliveryMethod:    enums.DeliveryMethodPickup,
		deliveryServiceID: 171,
		cost:              430,
		daysMin:           0,
		daysMax:           2,
		ruleType:          tr.RuleTypePayment,
	})
	shopTariffPickupForPoint := newShopTariff(&tariffSettings{
		deliveryMethod:    enums.DeliveryMethodPickup,
		deliveryServiceID: 171,
		pickupIDs:         []int64{10000971018},
		ruleType:          tr.RuleTypeForPoint,
	})
	tariffsFinder := tr.NewTariffsFinder()
	tariffsFinder.Add(&shopTariff)
	tariffsFinder.Add(&shopTariffPickupForPoint)

	regionMap := geobase.NewExample()
	genData := bg.GenerationData{
		RegionMap:     regionMap,
		TariffsFinder: NewFinderSet(tariffsFinder),
		Graph:         graphEx.G,
	}

	startTime := time.Date(2020, 06, 02, 12, 4, 5, 0, time.UTC)
	windows := graphEx.G.GetNodeByID(413836).GetServicesByType(enums.ServiceMovement)[0].Schedule.Windows[0]

	env, cancel := NewEnv(t, &genData, nil)
	defer cancel()

	for i := range windows {
		startTime = startTime.Add(time.Duration(2*i) * time.Hour)
		dest := pb.PointIds{
			LogisticPointId: 10000971018,
		}
		reqSettings := newRequestSettings(requestSettings{
			startTime: startTime,
			cost:      shopTariff.Cost,
			dest:      &dest,
			dType:     pb.DeliveryType_PICKUP,
			dayTo:     2 + shopTariff.DaysMax,
		})
		req := PrepareDeliveryRequest(reqSettings)

		wantDateFrom := startTime.Add(time.Hour * 24 * time.Duration(shopTariff.DaysMax))
		wantDateTo := startTime.Add(time.Hour * 24 * time.Duration(shopTariff.DaysMax))

		resp, err := env.Client.GetDeliveryRoute(env.Ctx, req)

		require.NoError(t, err)
		require.Equal(t, pb.DeliverySubtype_ORDINARY, resp.DeliverySubtype)
		require.NotNil(t, resp.Route)
		require.Equal(t, int(shopTariff.Cost+11+11+22+22+33), int(resp.Route.CostForShop))
		requirepb.Equal(t, ToProtoDate(wantDateFrom), resp.Route.DateFrom)
		requirepb.Equal(t, ToProtoDate(wantDateTo), resp.Route.DateTo)

		CheckIntervalsForExisting(t, resp)

		// check schedule time
		require.NotEmpty(t, resp.Route.Points)
		var mv1Point *pb.Route_Point
		for _, point := range resp.Route.Points {
			if point.SegmentId == 413836 {
				mv1Point = point
				break
			}
		}
		require.NotNil(t, mv1Point)
		require.NotEmpty(t, mv1Point.Services)

		mv1mvService := mv1Point.Services[0]
		loc := time.FixedZone("", int(mv1mvService.TzOffset))
		sst := timex.NewDayTimeFromTime(time.Unix(mv1mvService.ScheduleStartTime.Seconds, 0).In(loc))
		set := timex.NewDayTimeFromTime(time.Unix(mv1mvService.ScheduleEndTime.Seconds, 0).In(loc))
		require.Equal(t, windows[i].From, sst)
		require.Equal(t, windows[i].To, set)
	}
}

func TestGetPostDeliveryRoute(t *testing.T) {
	graphEx := graph.NewExample()
	shopTariff0 := newShopTariff(&tariffSettings{
		deliveryMethod:    enums.DeliveryMethodPickup,
		deliveryServiceID: 106,
		ruleType:          tr.RuleTypeGlobal,
	})
	shopTariff1 := newShopTariff(&tariffSettings{
		deliveryMethod:    enums.DeliveryMethodPickup,
		deliveryServiceID: 106,
		cost:              430,
		daysMin:           0,
		daysMax:           0,
		pickupIDs:         []int64{10000971021, 10000971022},
		ruleType:          tr.RuleTypePayment,
	})
	shopTariff2 := newShopTariff(&tariffSettings{
		deliveryMethod:    enums.DeliveryMethodPickup,
		deliveryServiceID: 106,
		pickupIDs:         []int64{10000971021, 10000971022},
		ruleType:          tr.RuleTypeForPoint,
	})
	regionMap := geobase.NewExample()
	tariffsFinder := tr.NewTariffsFinder()
	tariffsFinder.Add(&shopTariff0)
	tariffsFinder.Add(&shopTariff1)
	tariffsFinder.Add(&shopTariff2)
	tariffsFinder.Finish(&regionMap)

	genData := bg.GenerationData{
		RegionMap:     regionMap,
		TariffsFinder: NewFinderSet(tariffsFinder),
		Outlets: outlets.Make([]outlets.Outlet{
			outlets.Outlet{
				ID:       10000971021,
				RegionID: geobase.RegionID(213), // Needed to deduce region
				PostCode: 322223,
				Type:     outlets.Post, // Post delivery
			},
			outlets.Outlet{
				ID:       10000971022,
				RegionID: geobase.RegionID(213), // Needed to deduce region
				PostCode: 322224,
				Type:     outlets.PostTerm, // Pickup delivery
			},
		}, &regionMap, nil),
		Graph: graphEx.G,
	}

	startTime := time.Date(2020, 06, 02, 12, 4, 5, 0, time.UTC)

	dest := pb.PointIds{
		PostCode: 322223,
	}
	reqSettings := newRequestSettings(requestSettings{
		startTime: startTime,
		cost:      uint32(shopTariff1.Cost),
		dest:      &dest,
		dType:     pb.DeliveryType_PICKUP,
		dayTo:     2 + shopTariff1.DaysMax,
	})
	req := PrepareDeliveryRequest(reqSettings)

	env, cancel := NewEnv(t, &genData, nil)
	defer cancel()

	wantDateFrom := startTime.Add(time.Hour * 24 * time.Duration(shopTariff1.DaysMax))
	wantDateTo := startTime.Add(time.Hour * 24 * time.Duration(shopTariff1.DaysMax))

	resp, err := env.Client.GetDeliveryRoute(env.Ctx, req)

	require.NoError(t, err)
	require.Equal(t, pb.DeliverySubtype_ORDINARY, resp.DeliverySubtype)
	require.NotNil(t, resp.Route)
	require.Equal(t, int(shopTariff1.Cost+11+222+33), int(resp.Route.CostForShop))
	requirepb.Equal(t, ToProtoDate(wantDateFrom), resp.Route.DateFrom)
	requirepb.Equal(t, ToProtoDate(wantDateTo), resp.Route.DateTo)
	var moveServices int
	for _, seg := range resp.Route.Points {
		for _, s := range seg.Services {
			if s.Code == "MOVEMENT" {
				moveServices += 1
			}
		}
	}
	require.Equal(t, 1, moveServices)

	var expectedWeight uint32
	for _, item := range req.Items {
		expectedWeight += item.Weight * item.RequiredCount
	}
	expectedDimensions := []uint32{uint32(20), uint32(30), uint32(35)}

	require.NotNil(t, resp.VirtualBox)
	require.Equal(t, expectedWeight, resp.VirtualBox.Weight)
	require.Equal(t, expectedDimensions, resp.VirtualBox.Dimensions)

	// Only pickup point for this post code
	dest = pb.PointIds{
		PostCode: 322224,
	}
	reqSettings = newRequestSettings(requestSettings{
		startTime: startTime,
		cost:      uint32(shopTariff1.Cost),
		dest:      &dest,
		dType:     pb.DeliveryType_PICKUP,
		dayTo:     2 + shopTariff1.DaysMax,
	})
	req = PrepareDeliveryRequest(reqSettings)
	resp, err = env.Client.GetDeliveryRoute(env.Ctx, req)
	require.Error(t, err)
	require.Nil(t, resp)

	// Post code is absent
	dest = pb.PointIds{
		PostCode: 322225,
	}
	reqSettings = newRequestSettings(requestSettings{
		startTime: startTime,
		cost:      uint32(shopTariff1.Cost),
		dest:      &dest,
		dType:     pb.DeliveryType_PICKUP,
		dayTo:     2 + shopTariff1.DaysMax,
	})
	req = PrepareDeliveryRequest(reqSettings)
	resp, err = env.Client.GetDeliveryRoute(env.Ctx, req)
	require.Error(t, err)
	require.Nil(t, resp)

}

func TestEndAtPreviousDay(t *testing.T) {
	graphEx := graph.NewLateInboundExample()
	shopTariff := newShopTariff(&tariffSettings{
		deliveryMethod:    enums.DeliveryMethodCourier,
		deliveryServiceID: 106,
		cost:              420,
		daysMin:           5,
		daysMax:           7,
		ruleType:          tr.RuleTypePayment,
	})
	tariffsFinder := tr.NewTariffsFinder()
	tariffsFinder.Add(&shopTariff)

	genData := bg.GenerationData{
		RegionMap:     geobase.NewExample(),
		TariffsFinder: NewFinderSet(tariffsFinder),
		Graph:         graphEx.G,
	}

	startTime := time.Date(2020, 06, 02, 12, 4, 5, 0, time.UTC)

	dest := pb.PointIds{
		RegionId: 213,
	}
	reqSettings := newRequestSettings(requestSettings{
		startTime: startTime,
		cost:      uint32(shopTariff.Cost),
		dest:      &dest,
		dType:     pb.DeliveryType_COURIER,
		dayTo:     2 + shopTariff.DaysMax,
	})
	req := PrepareDeliveryRequest(reqSettings)

	env, cancel := NewEnv(t, &genData, nil)
	defer cancel()

	wantDateFrom := startTime.Add(time.Hour * 24 * time.Duration(shopTariff.DaysMax))
	wantDateTo := startTime.Add(time.Hour * 24 * time.Duration(shopTariff.DaysMax))

	resp, err := env.Client.GetDeliveryRoute(env.Ctx, req)

	require.NoError(t, err)
	require.Equal(t, pb.DeliverySubtype_ORDINARY, resp.DeliverySubtype)
	require.NotNil(t, resp.Route)
	//require.Equal(t, int(customerTariff.Cost), int(resp.Route.Cost))
	require.Equal(t, int(shopTariff.Cost+11+22+33+44), int(resp.Route.CostForShop))
	requirepb.Equal(t, ToProtoDate(wantDateFrom), resp.Route.DateFrom)
	requirepb.Equal(t, ToProtoDate(wantDateTo), resp.Route.DateTo)
	CheckIntervalsForExisting(t, resp)
}

func TestSchedule(t *testing.T) {
	graphEx := graph.NewLateInboundExample()
	shopTariff := newShopTariff(&tariffSettings{
		deliveryMethod:    enums.DeliveryMethodCourier,
		deliveryServiceID: 106,
		cost:              420,
		daysMin:           5,
		daysMax:           7,
		ruleType:          tr.RuleTypePayment,
	})
	tariffsFinder := tr.NewTariffsFinder()
	tariffsFinder.Add(&shopTariff)

	genData := bg.GenerationData{
		RegionMap:     geobase.NewExample(),
		TariffsFinder: NewFinderSet(tariffsFinder),
		Graph:         graphEx.G,
	}

	startTime := time.Date(2020, 06, 02, 12, 4, 5, 0, time.UTC)

	dest := pb.PointIds{
		RegionId: 213,
	}
	reqSettings := newRequestSettings(requestSettings{
		startTime: startTime,
		dest:      &dest,
		dType:     pb.DeliveryType_COURIER,
		dayTo:     2 + shopTariff.DaysMax,
	})
	req := PrepareDeliveryRequest(reqSettings)

	env, cancel := NewEnv(t, &genData, nil)
	defer cancel()

	resp, err := env.Client.GetDeliveryRoute(env.Ctx, req)

	require.NoError(t, err)
	require.Equal(t, pb.DeliverySubtype_ORDINARY, resp.DeliverySubtype)
	require.NotNil(t, resp.Route)

	var counter int
	for _, p := range resp.Route.Points {
		for _, s := range p.Services {
			if s.ScheduleStartTime != nil && s.ScheduleEndTime != nil {
				require.True(t, s.StartTime.Seconds >= s.ScheduleStartTime.Seconds)
				require.True(t, s.StartTime.Seconds <= s.ScheduleEndTime.Seconds)
				require.True(t, s.ScheduleEndTime.Seconds-s.ScheduleStartTime.Seconds <= 86400)
				counter++
			}
		}
	}

	require.Equal(t, 5, counter)
	CheckIntervalsForExisting(t, resp)
}

func CheckDeliveryRouteCourier(
	t *testing.T,
	env *Env,
	startTime *time.Time,
	dest *pb.PointIds,
	reqCost uint32,
	reqTariff *tr.TariffRT,
) {
	reqSettings := newRequestSettings(requestSettings{
		startTime: *startTime,
		cost:      reqCost,
		dest:      dest,
		dType:     pb.DeliveryType_COURIER,
		dayTo:     2 + reqTariff.DaysMax,
	})
	req := PrepareDeliveryRequest(reqSettings)
	resp, err := env.Client.GetDeliveryRoute(env.Ctx, req)
	require.NoError(t, err)
	require.Equal(t, pb.DeliverySubtype_ORDINARY, resp.DeliverySubtype)
	require.NotNil(t, resp.Route)
	CheckIntervalsForExisting(t, resp)
}

func TestCourierDeliveryRouteZeroCost(t *testing.T) {
	graphEx := graph.NewCourierAlternativeExample()
	shopTariffFastExpensive := newShopTariff(&tariffSettings{
		deliveryMethod:    enums.DeliveryMethodCourier,
		deliveryServiceID: 106,
		cost:              350,
		daysMin:           1,
		daysMax:           1,
		ruleType:          tr.RuleTypePayment,
	})

	shopTariffCheap := newShopTariff(&tariffSettings{
		deliveryMethod:    enums.DeliveryMethodCourier,
		deliveryServiceID: 139,
		cost:              230,
		daysMin:           2,
		daysMax:           2,
		ruleType:          tr.RuleTypePayment,
	})

	shopTariffExpensive := newShopTariff(&tariffSettings{
		deliveryMethod:    enums.DeliveryMethodCourier,
		deliveryServiceID: 107,
		cost:              270,
		daysMin:           2,
		daysMax:           2,
		ruleType:          tr.RuleTypePayment,
	})
	tariffsFinder := tr.NewTariffsFinder()
	tariffsFinder.Add(&shopTariffFastExpensive)
	tariffsFinder.Add(&shopTariffCheap)
	tariffsFinder.Add(&shopTariffExpensive)

	genData := bg.GenerationData{
		RegionMap:     geobase.NewExample(),
		TariffsFinder: NewFinderSet(tariffsFinder),
		Graph:         graphEx.G,
	}

	startTime := time.Date(2020, 06, 02, 12, 4, 5, 0, time.UTC)
	dest := pb.PointIds{
		RegionId: 213,
	}
	env, cancel := NewEnv(t, &genData, nil)
	defer cancel()

	// Fastest delivery route is found for cost == tariff.Cost (deliveryService = 106)
	CheckDeliveryRouteCourier(t, env, &startTime, &dest,
		shopTariffCheap.Cost, &shopTariffCheap)

	// When considering two similar tariffs, we choose cheapest tariff (deliveryService = 139)
	CheckDeliveryRouteCourier(t, env, &startTime, &dest, shopTariffExpensive.Cost, &shopTariffCheap)

	// Cheapest delivery route is found for cost == 0 (deliveryService = 139)
	CheckDeliveryRouteCourier(t, env, &startTime, &dest, 0, &shopTariffCheap)
}

func countServices(segments []*pb.Route_Point, segmentType graph.SegmentType, code enums.LmsServiceCode) int {
	count := 0
	for _, segment := range segments {
		if segment.SegmentType != segmentType.String() {
			continue
		}
		for _, service := range segment.Services {
			if service.Code == code.String() {
				count++
			}
		}
	}
	return count
}

func FashionTryingServiceTest(
	t *testing.T,
	env *Env,
	startTime *time.Time,
	dest *pb.PointIds,
	reqTariff *tr.TariffRT,
	dType pb.DeliveryType,
) {
	reqSettings := newRequestSettings(requestSettings{
		startTime:  *startTime,
		dest:       dest,
		dType:      dType,
		dayTo:      2 + reqTariff.DaysMax,
		cargoTypes: []uint32{fashion.CargoType},
	})
	req := PrepareDeliveryRequest(reqSettings)
	req.RearrFactors = "fashion_partial_msk_only=1;add_partial_return_service=1;combinator_fashion_sofino_only=1;partial_delivery_flag_courier_options_v2=0;partial_delivery_flag_pickup_points_v2=0"
	fashion.GetPartialDeliveryServices(nil)[106] = true
	partdel.Warehouses[145] = true
	resp, err := env.Client.GetDeliveryRoute(env.Ctx, req)
	require.NoError(t, err)
	require.Equal(t, pb.DeliverySubtype_ORDINARY, resp.DeliverySubtype)
	require.NotNil(t, resp.Route)
	// check segment with TRYING service is present
	countTrying, countPartial := 0, 0
	if dType == pb.DeliveryType_COURIER {
		countTrying = countServices(resp.Route.Points, graph.SegmentTypeHanding, enums.ServiceTrying)
	} else {
		countTrying = countServices(resp.Route.Points, graph.SegmentTypePickup, enums.ServiceTrying)
	}
	countPartial = countServices(resp.Route.Points, graph.SegmentTypeWarehouse, enums.ServicePartialReturn)
	require.Equal(t, 1, countTrying)
	require.Equal(t, 1, countPartial)

	// For partially deliverable orders there's no route for non-market delivery services
	reqTariff.IsMarketCourier = false
	resp, err = env.Client.GetDeliveryRoute(env.Ctx, req)
	if dType == pb.DeliveryType_COURIER {
		require.Error(t, err)
	} else {
		require.NoError(t, err)
		require.Equal(t, pb.DeliverySubtype_ORDINARY, resp.DeliverySubtype)
		require.NotNil(t, resp.Route)
	}

	// For destinations not in the list no partial delivery available
	reqTariff.IsMarketCourier = true
	tempMsk := partdel.Regions[0]
	partdel.Regions[0] = 2 // first is assumed to be Moscow
	resp, err = env.Client.GetDeliveryRoute(env.Ctx, req)
	require.NoError(t, err)
	require.Equal(t, pb.DeliverySubtype_ORDINARY, resp.DeliverySubtype)
	require.NotNil(t, resp.Route)
	// check segment with TRYING service is NOT present
	countTrying, countPartial = 0, 0
	if dType == pb.DeliveryType_COURIER {
		countTrying = countServices(resp.Route.Points, graph.SegmentTypeHanding, enums.ServiceTrying)
	} else {
		countTrying = countServices(resp.Route.Points, graph.SegmentTypePickup, enums.ServiceTrying)
	}
	countPartial = countServices(resp.Route.Points, graph.SegmentTypeWarehouse, enums.ServicePartialReturn)
	require.Equal(t, 0, countTrying)
	require.Equal(t, 0, countPartial)
	partdel.Regions[0] = tempMsk
	fashion.GetPartialDeliveryServices(nil)[106] = false
	partdel.Warehouses[145] = false
}

func TestDeliveryRouteIsMarketCourier(t *testing.T) {
	graphEx := graph.NewExample()
	shopTariffCourier := newShopTariff(&tariffSettings{
		deliveryMethod:    enums.DeliveryMethodCourier,
		deliveryServiceID: 106,
		cost:              420,
		daysMin:           5,
		daysMax:           7,
		ruleType:          tr.RuleTypePayment,
	})
	shopTariffPickupForPayment := newShopTariff(&tariffSettings{
		deliveryMethod:    enums.DeliveryMethodPickup,
		deliveryServiceID: 106,
		cost:              430,
		daysMin:           0,
		daysMax:           0,
		ruleType:          tr.RuleTypePayment,
	})
	shopTariffPickupForPoint := newShopTariff(&tariffSettings{
		deliveryMethod:    enums.DeliveryMethodPickup,
		deliveryServiceID: 106,
		pickupIDs:         []int64{10000971018},
		ruleType:          tr.RuleTypeForPoint,
	})
	shopTariffCourier.IsMarketCourier = true
	shopTariffPickupForPayment.IsMarketCourier = true
	tariffsFinder := tr.NewTariffsFinder()
	tariffsFinder.Add(&shopTariffCourier)
	tariffsFinder.Add(&shopTariffPickupForPayment)
	tariffsFinder.Add(&shopTariffPickupForPoint)

	regionMap := geobase.NewExample()
	genData := bg.GenerationData{
		RegionMap:     regionMap,
		TariffsFinder: NewFinderSet(tariffsFinder),
		Outlets: outlets.Make([]outlets.Outlet{
			outlets.Outlet{
				ID:                        10000971018,
				RegionID:                  geobase.RegionID(213),
				Type:                      outlets.Depot,
				IsMarketBranded:           true,
				DeliveryServiceOutletCode: "test", // COMBINATOR-2290: ignore unless dsbs
			},
		}, &regionMap, nil),
		Graph: graphEx.G,
	}

	startTime := time.Date(2020, 06, 02, 12, 4, 5, 0, time.UTC)

	type RequestParams struct {
		dType  pb.DeliveryType
		dest   *pb.PointIds
		tariff *tr.TariffRT
	}

	for _, params := range []RequestParams{
		{pb.DeliveryType_PICKUP, &pb.PointIds{LogisticPointId: 10000971018}, &shopTariffPickupForPayment},
		{pb.DeliveryType_COURIER, &pb.PointIds{RegionId: 213}, &shopTariffCourier},
	} {
		reqSettings := newRequestSettings(requestSettings{
			startTime: startTime,
			cost:      uint32(params.tariff.Cost),
			dest:      params.dest,
			dType:     params.dType,
			dayTo:     2 + params.tariff.DaysMax,
		})
		req := PrepareDeliveryRequest(reqSettings)

		env, cancel := NewEnv(t, &genData, nil)
		defer cancel()

		resp, err := env.Client.GetDeliveryRoute(env.Ctx, req)
		require.NoError(t, err)
		require.Equal(t, pb.DeliverySubtype_ORDINARY, resp.DeliverySubtype)
		require.NotNil(t, resp.Route)
		require.Equal(t, resp.Route.IsMarketCourier, true)

		params.tariff.IsMarketCourier = false
		resp, err = env.Client.GetDeliveryRoute(env.Ctx, req)

		require.NoError(t, err)
		require.Equal(t, pb.DeliverySubtype_ORDINARY, resp.DeliverySubtype)
		require.NotNil(t, resp.Route)
		require.Equal(t, resp.Route.IsMarketCourier, false)
	}
}

func TestCutOffAfterProcessing(t *testing.T) {
	graphEx := graph.NewCourierCutoffAfterProcessingExample()
	shopTariff := newShopTariff(&tariffSettings{
		deliveryMethod:    enums.DeliveryMethodCourier,
		deliveryServiceID: 106,
		cost:              350,
		daysMin:           2,
		daysMax:           2,
		ruleType:          tr.RuleTypePayment,
	})
	tariffsFinder := tr.NewTariffsFinder()
	tariffsFinder.Add(&shopTariff)

	genData := bg.GenerationData{
		RegionMap:     geobase.NewExample(),
		TariffsFinder: NewFinderSet(tariffsFinder),
		Graph:         graphEx.G,
	}

	startTime := time.Date(2020, 06, 02, 16, 59, 5, 0, time.UTC)
	dest := pb.PointIds{
		RegionId: 213,
	}
	st, _ := its.NewStringSettingsHolder("{\"skip_processing_duration\":true}")
	env, cancel := NewEnv(t, &genData, st)

	// Расписание Processing до 20:00
	// тут проверяем что при попадании в окно работы Processing в текущий день в 19:59, есть маршрут длительностью 5 дней
	reqSettings := newRequestSettings(requestSettings{
		startTime: startTime,
		cost:      0,
		dest:      &dest,
		dType:     pb.DeliveryType_COURIER,
		dayTo:     3 + shopTariff.DaysMin,
	})
	req := PrepareDeliveryRequest(reqSettings)
	resp, err := env.Client.GetDeliveryRoute(env.Ctx, req)
	require.NoError(t, err)
	require.Equal(t, pb.DeliverySubtype_ORDINARY, resp.DeliverySubtype)
	require.NotNil(t, resp.Route)

	// Проверяем наличие маршрута в 20:59, не попали в окно работы Processing
	// в этом кейсе должна игнорироваться длительность Processing и доставка должна быть также через 5 дней
	reqSettings = newRequestSettings(requestSettings{
		startTime: startTime.Add(1 * time.Hour),
		cost:      0,
		dest:      &dest,
		dType:     pb.DeliveryType_COURIER,
		dayTo:     3 + shopTariff.DaysMin,
	})
	req = PrepareDeliveryRequest(reqSettings)
	resp, err = env.Client.GetDeliveryRoute(env.Ctx, req)
	require.NoError(t, err)
	require.Equal(t, pb.DeliverySubtype_ORDINARY, resp.DeliverySubtype)
	require.NotNil(t, resp.Route)
	cancel()

	st, _ = its.NewStringSettingsHolder("{\"skip_processing_duration\":false}")
	env, cancel = NewEnv(t, &genData, st)
	defer cancel()
	// С выключенным флагом нет 5 дневных маршрутов
	_, err = env.Client.GetDeliveryRoute(env.Ctx, req)
	require.Error(t, err)

	// Есть маршрут с доставкой через 6 дней
	reqSettings = newRequestSettings(requestSettings{
		startTime: startTime.Add(1 * time.Hour),
		cost:      0,
		dest:      &dest,
		dType:     pb.DeliveryType_COURIER,
		dayTo:     4 + shopTariff.DaysMin,
	})
	req = PrepareDeliveryRequest(reqSettings)
	resp, err = env.Client.GetDeliveryRoute(env.Ctx, req)
	require.NoError(t, err)
	require.Equal(t, pb.DeliverySubtype_ORDINARY, resp.DeliverySubtype)
	require.NotNil(t, resp.Route)
}

func TestUseRightBorderTag(t *testing.T) {
	graphEx := graph.NewCourierCutoffAfterProcessingExample()
	shopTariff := newShopTariff(&tariffSettings{
		deliveryMethod:    enums.DeliveryMethodCourier,
		deliveryServiceID: 106,
		cost:              350,
		daysMin:           2,
		daysMax:           2,
		ruleType:          tr.RuleTypePayment,
	})
	tariffsFinder := tr.NewTariffsFinder()
	tariffsFinder.Add(&shopTariff)

	genData := bg.GenerationData{
		RegionMap:     geobase.NewExample(),
		TariffsFinder: NewFinderSet(tariffsFinder),
		Graph:         graphEx.G,
	}

	startTime := time.Date(2020, 06, 02, 16, 59, 5, 0, time.UTC)
	dest := pb.PointIds{
		RegionId: 213,
	}

	// Не используем тег START_AT_RIGHT_BORDER. Сервисы movement проходят по левой границе.
	// Нет сдвига.
	{
		st, _ := its.NewStringSettingsHolder("{\"skip_processing_duration\":true, \"use_right_border_tag\":false}")
		env, cancel := NewEnv(t, &genData, st)

		reqSettings := newRequestSettings(requestSettings{
			startTime: startTime,
			cost:      0,
			dest:      &dest,
			dType:     pb.DeliveryType_COURIER,
			dayTo:     3 + shopTariff.DaysMin,
		})
		req := PrepareDeliveryRequest(reqSettings)
		resp, err := env.Client.GetDeliveryRoute(env.Ctx, req)
		require.NoError(t, err)
		require.Equal(t, pb.DeliverySubtype_ORDINARY, resp.DeliverySubtype)
		require.NotNil(t, resp.Route)

		reqSettings = newRequestSettings(requestSettings{
			startTime: startTime.Add(1 * time.Hour),
			cost:      0,
			dest:      &dest,
			dType:     pb.DeliveryType_COURIER,
			dayTo:     3 + shopTariff.DaysMin,
		})
		req = PrepareDeliveryRequest(reqSettings)
		resp, err = env.Client.GetDeliveryRoute(env.Ctx, req)
		require.NoError(t, err)
		require.Equal(t, pb.DeliverySubtype_ORDINARY, resp.DeliverySubtype)
		require.NotNil(t, resp.Route)
		cancel()
		var linehaulStartTime *timestamp.Timestamp
		for _, point := range resp.Route.Points {
			if point.SegmentType == graph.SegmentTypeLinehaul.String() {
				for _, service := range point.Services {
					if service.Code == enums.ServiceLastMile.String() {
						linehaulStartTime = service.StartTime
					}
				}
			}
		}
		require.NotNil(t, linehaulStartTime)
		requirepb.Equal(t, ToProtoTimestamp(time.Date(2020, 06, 03, 05, 00, 00, 00, time.UTC)), linehaulStartTime)
	}
	// Используем тег START_AT_RIGHT_BORDER. Сервисы movement проходят по правой границе.
	// Сдвиг времени после movement на конец расписания - 20:00 МСК
	{
		st, _ := its.NewStringSettingsHolder("{\"skip_processing_duration\":true, \"use_right_border_tag\":true}")
		env, cancel := NewEnv(t, &genData, st)

		reqSettings := newRequestSettings(requestSettings{
			startTime: startTime,
			cost:      0,
			dest:      &dest,
			dType:     pb.DeliveryType_COURIER,
			dayTo:     3 + shopTariff.DaysMin,
		})
		req := PrepareDeliveryRequest(reqSettings)
		resp, err := env.Client.GetDeliveryRoute(env.Ctx, req)
		require.NoError(t, err)
		require.Equal(t, pb.DeliverySubtype_ORDINARY, resp.DeliverySubtype)
		require.NotNil(t, resp.Route)

		reqSettings = newRequestSettings(requestSettings{
			startTime: startTime.Add(1 * time.Hour),
			cost:      0,
			dest:      &dest,
			dType:     pb.DeliveryType_COURIER,
			dayTo:     3 + shopTariff.DaysMin,
		})
		req = PrepareDeliveryRequest(reqSettings)
		resp, err = env.Client.GetDeliveryRoute(env.Ctx, req)
		require.NoError(t, err)
		require.Equal(t, pb.DeliverySubtype_ORDINARY, resp.DeliverySubtype)
		require.NotNil(t, resp.Route)
		cancel()
		var linehaulStartTime *timestamp.Timestamp
		for _, point := range resp.Route.Points {
			if point.SegmentType == graph.SegmentTypeLinehaul.String() {
				for _, service := range point.Services {
					if service.Code == enums.ServiceLastMile.String() {
						linehaulStartTime = service.StartTime
					}
				}
			}
		}
		require.NotNil(t, linehaulStartTime)
		requirepb.Equal(t, ToProtoTimestamp(time.Date(2020, 06, 03, 17, 00, 00, 00, time.UTC)), linehaulStartTime)
	}
}
