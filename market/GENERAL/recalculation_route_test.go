package lite

import (
	"fmt"
	"testing"
	"time"

	"github.com/stretchr/testify/require"
	"google.golang.org/protobuf/types/known/timestamppb"

	bg "a.yandex-team.ru/market/combinator/pkg/biggeneration"
	"a.yandex-team.ru/market/combinator/pkg/daysoff"
	"a.yandex-team.ru/market/combinator/pkg/enums"
	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/graph"
	"a.yandex-team.ru/market/combinator/pkg/outlets"
	"a.yandex-team.ru/market/combinator/pkg/recalculation"
	tr "a.yandex-team.ru/market/combinator/pkg/tarifficator"
	pb "a.yandex-team.ru/market/combinator/proto/grpc"
)

func TestRecalculationRoute(t *testing.T) {
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
				ID:       10000971018,
				RegionID: geobase.RegionID(213), // Needed to deduce region
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
		dest:      &dest,
		dType:     pb.DeliveryType_PICKUP,
		dayTo:     2 + shopTariff1.DaysMax,
	})
	req := PrepareDeliveryRequest(reqSettings)

	env, cancel := NewEnv(t, &genData, nil)
	defer cancel()

	resp, err := env.Client.GetDeliveryRoute(env.Ctx, req)

	require.Equal(t, nil, err)
	require.NotNil(t, resp.Route)

	startTime = startTime.Add(-300 * time.Second)
	for diff := 0; diff < len(resp.Route.Points)-2; diff++ {
		recalcReq := &pb.RecalculationRequest{
			StartTime:    timestamppb.New(startTime),
			Route:        resp,
			DeliveryType: req.DeliveryType,
			SegmentId:    resp.Route.Points[diff].SegmentId,
			ServiceCode:  resp.Route.Points[diff].Services[0].Code,
		}

		recalcResp, err := env.Client.RecalculationRoute(env.Ctx, recalcReq)

		if err != nil {
			t.Errorf("err %s then segment id %d and service code %s", err.Error(), recalcReq.SegmentId, recalcReq.ServiceCode)
		}

		require.Equal(t, nil, err)
		require.NotNil(t, recalcResp.Route)
		require.Equal(t, len(resp.Route.Points), len(recalcResp.Route.Route.Points))

		for i := range recalcResp.Route.Route.Points {
			aPoint := resp.Route.Points[i]
			bPoint := recalcResp.Route.Route.Points[i]

			for j := range aPoint.Services {
				require.Equal(t, aPoint.Services[j].StartTime.Seconds, bPoint.Services[j].StartTime.Seconds)
			}
		}

		recalcReq.StartTime = timestamppb.New(startTime.Add(2 * time.Hour))
		recalcResp, err = env.Client.RecalculationRoute(env.Ctx, recalcReq)

		if err != nil {
			t.Errorf("err then segment id %d and service code %s with +2 hours", recalcReq.SegmentId, recalcReq.ServiceCode)
		}

		require.Equal(t, nil, err)
		require.NotNil(t, recalcResp.Route)
		require.Equal(t, len(resp.Route.Points), len(recalcResp.Route.Route.Points))

		for i := range recalcResp.Route.Route.Points[diff:] {
			aPoint := resp.Route.Points[i+diff]
			bPoint := recalcResp.Route.Route.Points[i+diff]

			for j := range aPoint.Services {
				if aPoint.SegmentId == recalcReq.SegmentId && aPoint.Services[j].Code == recalcReq.ServiceCode {
					require.Equal(t, aPoint.Services[j].StartTime.Seconds, bPoint.Services[j].StartTime.Seconds,
						fmt.Sprintf("not equal when segment: %d, service: %s",
							recalcReq.SegmentId,
							recalcReq.ServiceCode,
						))

				} else {
					require.Equal(t, aPoint.Services[j].StartTime.Seconds+7200, bPoint.Services[j].StartTime.Seconds,
						fmt.Sprintf("not equal when segment: %d, service: %s",
							recalcReq.SegmentId,
							recalcReq.ServiceCode,
						))
				}
			}
		}
	}
}

func TestHolidaysRecalculation(t *testing.T) {
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

	var service *graph.LogisticService
	for i := range graphEx.G.NodesSlice {
		n := &graphEx.G.NodesSlice[i]
		if n.ID == 411850 {
			service = n.PickupServices[1]
			break
		}
	}

	DisabledDatesHashed := daysoff.NewServicesHashed()
	daysOff := daysoff.NewHolidayFromStrings([]string{
		"2020-06-02",
		"2020-06-03",
		"2020-06-04",
	})
	daysOff.DaysOffMap[157] = daysoff.DisabledDate{} // 05.06.2020
	DisabledDatesHashed.DaysOffGrouped[service.ID] = daysOff

	regionMap := geobase.NewExample()
	genData := bg.GenerationData{
		RegionMap:     regionMap,
		TariffsFinder: NewFinderSet(tariffsFinder),
		Outlets: outlets.Make([]outlets.Outlet{
			outlets.Outlet{
				ID:       10000971018,
				RegionID: geobase.RegionID(213), // Needed to deduce region
			},
		}, &regionMap, nil),
		Graph:               graphEx.G,
		DisabledDatesHashed: DisabledDatesHashed,
	}
	genData.AddServiceDaysOff(genData.DisabledDatesHashed)

	startTime := time.Date(2020, 06, 02, 12, 4, 5, 0, time.UTC)

	dest := pb.PointIds{
		LogisticPointId: 10000971018,
	}
	reqSettings := newRequestSettings(requestSettings{
		startTime: startTime,
		dest:      &dest,
		dType:     pb.DeliveryType_PICKUP,
		dayTo:     2 + shopTariff1.DaysMax + 4,
	})
	req := PrepareDeliveryRequest(reqSettings)

	env, cancel := NewEnv(t, &genData, nil)
	defer cancel()

	resp, err := env.Client.GetDeliveryRoute(env.Ctx, req)

	require.Equal(t, nil, err)
	require.NotNil(t, resp.Route)

	recalcReq := &pb.RecalculationRequest{
		StartTime:    timestamppb.New(startTime),
		Route:        resp,
		DeliveryType: req.DeliveryType,
		SegmentId:    resp.Route.Points[0].SegmentId,
		ServiceCode:  resp.Route.Points[0].Services[0].Code,
	}

	recalcResp, err := env.Client.RecalculationRoute(env.Ctx, recalcReq)

	if err != nil {
		t.Errorf("err then segment id %d and service code %s", recalcReq.SegmentId, recalcReq.ServiceCode)
	}

	require.Equal(t, nil, err)
	require.NotNil(t, recalcResp.Route)
	require.Equal(t, len(resp.Route.Points), len(recalcResp.Route.Route.Points))

	for i := range recalcResp.Route.Route.Points {
		aPoint := resp.Route.Points[i]
		bPoint := recalcResp.Route.Route.Points[i]

		for j := range aPoint.Services {
			if i == 0 && j == 0 {
				require.Equal(t, aPoint.Services[j].StartTime.Seconds, bPoint.Services[j].StartTime.Seconds)
			} else {
				require.Equal(t, aPoint.Services[j].StartTime.Seconds-86400, bPoint.Services[j].StartTime.Seconds)
			}
		}
	}
}

func TestTariffDays(t *testing.T) {
	s, err := graph.NewSchedule(
		graph.CreateSchedule("11:00:00", "11:00:00"),
		true,
	)

	require.Equal(t, nil, err)
	require.NotNil(t, s)

	info := recalculation.Recalculate{
		SegmentID:    0,
		ServiceCode:  0,
		DeliveryType: graph.DeliveryTypePickup,
		CurrentPoints: []*pb.Route_Point{
			{
				SegmentType: graph.SegmentTypeLinehaul.String(),
				Services: []*pb.DeliveryService{
					{
						Code:      enums.ServiceLastMile.String(),
						StartTime: timestamppb.New(time.Date(2021, 01, 01, 10, 59, 59, 00, time.UTC)),
					},
				},
			},
			{
				SegmentType: graph.SegmentTypePickup.String(),
				Services: []*pb.DeliveryService{
					{
						Code:      enums.ServiceHanding.String(),
						StartTime: timestamppb.New(time.Date(2021, 01, 02, 10, 59, 59, 00, time.UTC)),
					},
				},
			},
		},
		ResultPoints: nil,
		DstRegionID:  0,
		Nodes: []*graph.Node{
			{
				LogisticSegment: graph.LogisticSegment{
					Type: graph.SegmentTypeLinehaul,
				},
				PickupServices: []*graph.LogisticService{
					{
						Code:     enums.ServiceLastMile,
						Schedule: s,
					},
				},
			},
		},
	}

	res, err := info.TariffDays()
	require.Equal(t, nil, err)
	require.Equal(t, 2, int(res))

	info.CurrentPoints[0].Services[0].StartTime = timestamppb.New(time.Date(2021, 01, 01, 11, 00, 00, 00, time.UTC))
	info.CurrentPoints[1].Services[0].StartTime = timestamppb.New(time.Date(2021, 01, 02, 11, 00, 00, 00, time.UTC))

	res, err = info.TariffDays()
	require.Equal(t, nil, err)
	require.Equal(t, 1, int(res))

	info.CurrentPoints[0].Services[0].StartTime = timestamppb.New(time.Date(2021, 01, 01, 12, 00, 00, 00, time.UTC))
	info.CurrentPoints[1].Services[0].StartTime = timestamppb.New(time.Date(2021, 01, 02, 12, 00, 00, 00, time.UTC))

	res, err = info.TariffDays()
	require.Equal(t, nil, err)
	require.Equal(t, 1, int(res))
}

func TestNodeSafety(t *testing.T) {
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
				ID:       10000971018,
				RegionID: geobase.RegionID(213), // Needed to deduce region
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
		dest:      &dest,
		dType:     pb.DeliveryType_PICKUP,
		dayTo:     2 + shopTariff1.DaysMax,
	})
	req := PrepareDeliveryRequest(reqSettings)

	env, cancel := NewEnv(t, &genData, nil)
	defer cancel()

	resp, err := env.Client.GetDeliveryRoute(env.Ctx, req)

	require.Equal(t, nil, err)
	require.NotNil(t, resp.Route)

	recalcReq := &pb.RecalculationRequest{
		StartTime:    timestamppb.New(startTime),
		Route:        resp,
		DeliveryType: req.DeliveryType,
		SegmentId:    resp.Route.Points[0].SegmentId,
		ServiceCode:  resp.Route.Points[0].Services[1].Code,
	}

	recalcResp, err := env.Client.RecalculationRoute(env.Ctx, recalcReq)

	require.Equal(t, nil, err)
	require.NotNil(t, resp.Route)
	require.Equal(t, len(resp.Route.Points[0].Services), len(recalcResp.Route.Route.Points[0].Services))

	recalcReq = &pb.RecalculationRequest{
		StartTime:    timestamppb.New(startTime),
		Route:        resp,
		DeliveryType: req.DeliveryType,
		SegmentId:    resp.Route.Points[0].SegmentId,
		ServiceCode:  resp.Route.Points[0].Services[0].Code,
	}

	recalcResp, err = env.Client.RecalculationRoute(env.Ctx, recalcReq)

	require.Equal(t, nil, err)
	require.NotNil(t, resp.Route)
	require.Equal(t, len(resp.Route.Points[0].Services), len(recalcResp.Route.Route.Points[0].Services))
}

func TestLogisticDateRecalculation(t *testing.T) {
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
				ID:       10000971018,
				RegionID: geobase.RegionID(213), // Needed to deduce region
			},
		}, &regionMap, nil),
		Graph: graphEx.G,
	}

	startTime := time.Date(2020, 06, 02, 4, 4, 5, 0, time.UTC)

	dest := pb.PointIds{
		LogisticPointId: 10000971018,
	}
	reqSettings := newRequestSettings(requestSettings{
		startTime: startTime,
		dest:      &dest,
		dType:     pb.DeliveryType_PICKUP,
		dayTo:     2 + shopTariff1.DaysMax,
	})
	req := PrepareDeliveryRequest(reqSettings)

	env, cancel := NewEnv(t, &genData, nil)
	defer cancel()

	resp, err := env.Client.GetDeliveryRoute(env.Ctx, req)

	require.Equal(t, nil, err)
	require.NotNil(t, resp.Route)

	startTime = startTime.Add(-300 * time.Second)
	recalcReq := &pb.RecalculationRequest{
		StartTime:    timestamppb.New(startTime),
		Route:        resp,
		DeliveryType: req.DeliveryType,
		SegmentId:    resp.Route.Points[0].SegmentId,
		ServiceCode:  resp.Route.Points[0].Services[0].Code,
	}

	recalcResp, err := env.Client.RecalculationRoute(env.Ctx, recalcReq)

	if err != nil {
		t.Errorf("err then segment id %d and service code %s", recalcReq.SegmentId, recalcReq.ServiceCode)
	}

	require.Equal(t, nil, err)
	require.NotNil(t, recalcResp.Route)
	require.Equal(t, len(resp.Route.Points), len(recalcResp.Route.Route.Points))

	for i := range recalcResp.Route.Route.Points {
		aPoint := resp.Route.Points[i]
		bPoint := recalcResp.Route.Route.Points[i]

		for j := range aPoint.Services {
			require.Equal(t, aPoint.Services[j].StartTime.Seconds, bPoint.Services[j].StartTime.Seconds)
			if aPoint.SegmentType == graph.SegmentTypeMovement.String() || aPoint.SegmentType == graph.SegmentTypeWarehouse.String() {
				require.Equal(t, uint32(1), aPoint.Services[j].LogisticDate.Day)
				require.Equal(t, uint32(1), bPoint.Services[j].LogisticDate.Day)
			}
		}
	}
}
