package lite

import (
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/test/requirepb"
	bg "a.yandex-team.ru/market/combinator/pkg/biggeneration"
	"a.yandex-team.ru/market/combinator/pkg/enums"
	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/graph"
	tr "a.yandex-team.ru/market/combinator/pkg/tarifficator"
	pb "a.yandex-team.ru/market/combinator/proto/grpc"
)

type deliveryFromPointRequestSettings struct {
	startTime          time.Time
	cost               uint32
	dest               *pb.PointIds
	dType              pb.DeliveryType
	weight             uint32
	cargoTypes         []uint32
	dServiceID         uint32
	dateFrom           *pb.Date
	dateTo             *pb.Date
	interval           *pb.DeliveryInterval
	partnerID          uint64
	logisticPointLmsID uint64
}

func PrepareDeliveryFromPointRequest(s *deliveryFromPointRequestSettings) *pb.DeliveryRouteFromPointRequest {
	req := pb.DeliveryRouteFromPointRequest{
		Items: []*pb.DeliveryRequestPackage{
			{
				RequiredCount: 1,
				Weight:        s.weight,
				CargoTypes:    s.cargoTypes,
				Price:         s.cost,
				Dimensions: []uint32{
					20,
					20,
					15,
				},
			}},
		Destination:        s.dest,
		DeliveryServiceId:  s.dServiceID,
		DateFrom:           s.dateFrom,
		DateTo:             s.dateTo,
		Interval:           s.interval,
		StartTime:          ToProtoTimestamp(s.startTime),
		DeliveryType:       s.dType,
		PartnerId:          s.partnerID,
		LogisticPointLmsId: s.logisticPointLmsID,
	}
	return &req
}

func TestGetCourierDeliveryFromPointRouteSimpleCourier(t *testing.T) {
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

	reqSettings := deliveryFromPointRequestSettings{
		startTime:          startTime,
		cost:               uint32(shopTariff.Cost),
		weight:             10000,
		dest:               &dest,
		dType:              pb.DeliveryType_COURIER,
		dateFrom:           ToProtoDate(startTime.Add(time.Duration(shopTariff.DaysMin) * time.Hour * 24)),
		dateTo:             ToProtoDate(startTime.Add(time.Duration(shopTariff.DaysMax) * time.Hour * 24)),
		interval:           &pb.DeliveryInterval{From: &pb.Time{Hour: 10}, To: &pb.Time{Hour: 12}},
		dServiceID:         106,
		partnerID:          145,
		logisticPointLmsID: 10000010749,
	}
	req := PrepareDeliveryFromPointRequest(&reqSettings)

	env, cancel := NewEnv(t, &genData, nil)
	defer cancel()

	wantDateFrom := startTime.Add(time.Hour * 24 * time.Duration(shopTariff.DaysMax))
	wantDateTo := startTime.Add(time.Hour * 24 * time.Duration(shopTariff.DaysMax))

	resp, err := env.Client.GetDeliveryRouteFromPoint(env.Ctx, req)

	require.NoError(t, err)
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
	for _, item := range req.Items {
		expectedWeight += item.Weight * item.RequiredCount
	}

	require.NotNil(t, resp.VirtualBox)
	require.Equal(t, expectedWeight, resp.VirtualBox.Weight)
	CheckIntervalsForExisting(t, resp)

	FashionTryingServiceTest(t, env, &startTime, &dest, &shopTariff, pb.DeliveryType_COURIER)
}

func TestGetCourierDeliveryFromPointRouteFromSCCourier(t *testing.T) {
	graphEx := graph.NewExampleWithSC()
	shopTariff := newShopTariff(&tariffSettings{
		deliveryMethod:    enums.DeliveryMethodCourier,
		deliveryServiceID: 171,
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

	// Старт из sc1
	{
		dest := pb.PointIds{
			RegionId:  213,
			PartnerId: 167,
		}

		reqSettings := deliveryFromPointRequestSettings{
			startTime:          startTime,
			cost:               uint32(shopTariff.Cost),
			weight:             10000,
			dest:               &dest,
			dType:              pb.DeliveryType_COURIER,
			dateFrom:           ToProtoDate(startTime.Add(time.Duration(shopTariff.DaysMin) * time.Hour * 24)),
			dateTo:             ToProtoDate(startTime.Add(time.Duration(shopTariff.DaysMax) * time.Hour * 24)),
			interval:           &pb.DeliveryInterval{From: &pb.Time{Hour: 10}, To: &pb.Time{Hour: 12}},
			dServiceID:         171,
			partnerID:          167,
			logisticPointLmsID: 10000010123,
		}
		req := PrepareDeliveryFromPointRequest(&reqSettings)

		env, cancel := NewEnv(t, &genData, nil)
		defer cancel()

		wantDateFrom := startTime.Add(time.Hour * 24 * time.Duration(shopTariff.DaysMax))
		wantDateTo := startTime.Add(time.Hour * 24 * time.Duration(shopTariff.DaysMax))

		resp, err := env.Client.GetDeliveryRouteFromPoint(env.Ctx, req)

		require.NoError(t, err)
		require.NotNil(t, resp.Route)
		require.Equal(t, int(shopTariff.Cost+11+22+33+33), int(resp.Route.CostForShop))
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
		require.Equal(t, 2, moveServices)

		var expectedWeight uint32
		for _, item := range req.Items {
			expectedWeight += item.Weight * item.RequiredCount
		}

		require.NotNil(t, resp.VirtualBox)
		require.Equal(t, expectedWeight, resp.VirtualBox.Weight)
		CheckIntervalsForExisting(t, resp)
	}

	// Старт из sc2
	{
		dest := pb.PointIds{
			RegionId:  213,
			PartnerId: 167,
		}

		reqSettings := deliveryFromPointRequestSettings{
			startTime:          startTime,
			cost:               uint32(shopTariff.Cost),
			weight:             10000,
			dest:               &dest,
			dType:              pb.DeliveryType_COURIER,
			dateFrom:           ToProtoDate(startTime.Add(time.Duration(shopTariff.DaysMin) * time.Hour * 24)),
			dateTo:             ToProtoDate(startTime.Add(time.Duration(shopTariff.DaysMax) * time.Hour * 24)),
			interval:           &pb.DeliveryInterval{From: &pb.Time{Hour: 10}, To: &pb.Time{Hour: 12}},
			dServiceID:         171,
			partnerID:          167,
			logisticPointLmsID: 10000010124,
		}
		req := PrepareDeliveryFromPointRequest(&reqSettings)

		env, cancel := NewEnv(t, &genData, nil)
		defer cancel()

		wantDateFrom := startTime.Add(time.Hour * 24 * time.Duration(shopTariff.DaysMax))
		wantDateTo := startTime.Add(time.Hour * 24 * time.Duration(shopTariff.DaysMax))

		resp, err := env.Client.GetDeliveryRouteFromPoint(env.Ctx, req)

		require.NoError(t, err)
		require.NotNil(t, resp.Route)
		require.Equal(t, int(shopTariff.Cost+11+22+33+33), int(resp.Route.CostForShop))
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
		require.Equal(t, 2, moveServices)

		var expectedWeight uint32
		for _, item := range req.Items {
			expectedWeight += item.Weight * item.RequiredCount
		}

		require.NotNil(t, resp.VirtualBox)
		require.Equal(t, expectedWeight, resp.VirtualBox.Weight)
		CheckIntervalsForExisting(t, resp)
	}

	// Старт из sc3
	{
		dest := pb.PointIds{
			RegionId:  213,
			PartnerId: 171,
		}

		reqSettings := deliveryFromPointRequestSettings{
			startTime:          startTime,
			cost:               uint32(shopTariff.Cost),
			weight:             10000,
			dest:               &dest,
			dType:              pb.DeliveryType_COURIER,
			dateFrom:           ToProtoDate(startTime.Add(time.Duration(shopTariff.DaysMin) * time.Hour * 24)),
			dateTo:             ToProtoDate(startTime.Add(time.Duration(shopTariff.DaysMax) * time.Hour * 24)),
			interval:           &pb.DeliveryInterval{From: &pb.Time{Hour: 10}, To: &pb.Time{Hour: 12}},
			dServiceID:         171,
			partnerID:          171,
			logisticPointLmsID: 10000010125,
		}
		req := PrepareDeliveryFromPointRequest(&reqSettings)

		env, cancel := NewEnv(t, &genData, nil)
		defer cancel()

		wantDateFrom := startTime.Add(time.Hour * 24 * time.Duration(shopTariff.DaysMax))
		wantDateTo := startTime.Add(time.Hour * 24 * time.Duration(shopTariff.DaysMax))

		resp, err := env.Client.GetDeliveryRouteFromPoint(env.Ctx, req)

		require.NoError(t, err)
		require.NotNil(t, resp.Route)
		require.Equal(t, int(shopTariff.Cost+11+22+33), int(resp.Route.CostForShop))
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

		require.NotNil(t, resp.VirtualBox)
		require.Equal(t, expectedWeight, resp.VirtualBox.Weight)
		CheckIntervalsForExisting(t, resp)
	}
}

func TestGetCourierDeliveryFromPointRouteSimplePickup(t *testing.T) {
	graphEx := graph.NewExample()
	shopTariff := newShopTariff(&tariffSettings{
		deliveryMethod:    enums.DeliveryMethodPickup,
		deliveryServiceID: 106,
		cost:              430,
		daysMin:           0,
		daysMax:           2,
		ruleType:          tr.RuleTypePayment,
	})
	shopTariffPickupForPoint := newShopTariff(&tariffSettings{
		deliveryMethod:    enums.DeliveryMethodPickup,
		deliveryServiceID: 106,
		pickupIDs:         []int64{10000971018},
		ruleType:          tr.RuleTypeForPoint,
	})
	tariffsFinder := tr.NewTariffsFinder()
	tariffsFinder.Add(&shopTariff)
	tariffsFinder.Add(&shopTariffPickupForPoint)

	genData := bg.GenerationData{
		RegionMap:     geobase.NewExample(),
		TariffsFinder: NewFinderSet(tariffsFinder),
		Graph:         graphEx.G,
	}

	startTime := time.Date(2020, 06, 02, 12, 4, 5, 0, time.UTC)

	dest := pb.PointIds{
		RegionId:        213,
		LogisticPointId: 10000971018,
	}

	reqSettings := deliveryFromPointRequestSettings{
		startTime:          startTime,
		cost:               uint32(shopTariff.Cost),
		weight:             10000,
		dest:               &dest,
		dType:              pb.DeliveryType_PICKUP,
		dateFrom:           ToProtoDate(startTime.Add(time.Duration(shopTariff.DaysMin) * time.Hour * 24)),
		dateTo:             ToProtoDate(startTime.Add(time.Duration(shopTariff.DaysMax) * time.Hour * 24)),
		dServiceID:         106,
		partnerID:          145,
		logisticPointLmsID: 10000010749,
	}
	req := PrepareDeliveryFromPointRequest(&reqSettings)

	env, cancel := NewEnv(t, &genData, nil)
	defer cancel()

	wantDateFrom := startTime.Add(time.Hour * 24 * time.Duration(shopTariff.DaysMax))
	wantDateTo := startTime.Add(time.Hour * 24 * time.Duration(shopTariff.DaysMax))

	resp, err := env.Client.GetDeliveryRouteFromPoint(env.Ctx, req)

	require.NoError(t, err)
	require.NoError(t, err)
	require.NotNil(t, resp.Route)
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

	require.NotNil(t, resp.VirtualBox)
	require.Equal(t, expectedWeight, resp.VirtualBox.Weight)
	CheckIntervalsForExisting(t, resp)
}

func TestGetCourierDeliveryFromPointRouteFromSCPickup(t *testing.T) {
	graphEx := graph.NewExampleWithSC()
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

	genData := bg.GenerationData{
		RegionMap:     geobase.NewExample(),
		TariffsFinder: NewFinderSet(tariffsFinder),
		Graph:         graphEx.G,
	}

	startTime := time.Date(2020, 06, 02, 12, 4, 5, 0, time.UTC)

	// Старт из sc1
	{
		dest := pb.PointIds{
			RegionId:        213,
			LogisticPointId: 10000971018,
		}

		reqSettings := deliveryFromPointRequestSettings{
			startTime:          startTime,
			cost:               uint32(shopTariff.Cost),
			weight:             10000,
			dest:               &dest,
			dType:              pb.DeliveryType_PICKUP,
			dateFrom:           ToProtoDate(startTime.Add(time.Duration(shopTariff.DaysMin) * time.Hour * 24)),
			dateTo:             ToProtoDate(startTime.Add(time.Duration(shopTariff.DaysMax) * time.Hour * 24)),
			dServiceID:         171,
			partnerID:          167,
			logisticPointLmsID: 10000010123,
		}
		req := PrepareDeliveryFromPointRequest(&reqSettings)

		env, cancel := NewEnv(t, &genData, nil)
		defer cancel()

		wantDateFrom := startTime.Add(time.Hour * 24 * time.Duration(shopTariff.DaysMax))
		wantDateTo := startTime.Add(time.Hour * 24 * time.Duration(shopTariff.DaysMax))

		resp, err := env.Client.GetDeliveryRouteFromPoint(env.Ctx, req)

		require.NoError(t, err)
		require.NotNil(t, resp.Route)
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
		require.Equal(t, 2, moveServices)

		var expectedWeight uint32
		for _, item := range req.Items {
			expectedWeight += item.Weight * item.RequiredCount
		}

		require.NotNil(t, resp.VirtualBox)
		require.Equal(t, expectedWeight, resp.VirtualBox.Weight)
		CheckIntervalsForExisting(t, resp)
	}

	// Старт из sc2
	{
		dest := pb.PointIds{
			RegionId:        213,
			PartnerId:       167,
			LogisticPointId: 10000971018,
		}

		reqSettings := deliveryFromPointRequestSettings{
			startTime:          startTime,
			cost:               uint32(shopTariff.Cost),
			weight:             10000,
			dest:               &dest,
			dType:              pb.DeliveryType_PICKUP,
			dateFrom:           ToProtoDate(startTime.Add(time.Duration(shopTariff.DaysMin) * time.Hour * 24)),
			dateTo:             ToProtoDate(startTime.Add(time.Duration(shopTariff.DaysMax) * time.Hour * 24)),
			interval:           &pb.DeliveryInterval{From: &pb.Time{Hour: 10}, To: &pb.Time{Hour: 12}},
			dServiceID:         171,
			partnerID:          167,
			logisticPointLmsID: 10000010124,
		}
		req := PrepareDeliveryFromPointRequest(&reqSettings)

		env, cancel := NewEnv(t, &genData, nil)
		defer cancel()

		wantDateFrom := startTime.Add(time.Hour * 24 * time.Duration(shopTariff.DaysMax))
		wantDateTo := startTime.Add(time.Hour * 24 * time.Duration(shopTariff.DaysMax))

		resp, err := env.Client.GetDeliveryRouteFromPoint(env.Ctx, req)

		require.NoError(t, err)
		require.NotNil(t, resp.Route)
		require.Equal(t, int(shopTariff.Cost+11+22+33+33), int(resp.Route.CostForShop))
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
		require.Equal(t, 2, moveServices)

		var expectedWeight uint32
		for _, item := range req.Items {
			expectedWeight += item.Weight * item.RequiredCount
		}

		require.NotNil(t, resp.VirtualBox)
		require.Equal(t, expectedWeight, resp.VirtualBox.Weight)
		CheckIntervalsForExisting(t, resp)
	}

	// Старт из sc3
	{
		dest := pb.PointIds{
			RegionId:        213,
			PartnerId:       171,
			LogisticPointId: 10000971018,
		}

		reqSettings := deliveryFromPointRequestSettings{
			startTime:          startTime,
			cost:               uint32(shopTariff.Cost),
			weight:             10000,
			dest:               &dest,
			dType:              pb.DeliveryType_PICKUP,
			dateFrom:           ToProtoDate(startTime.Add(time.Duration(shopTariff.DaysMin) * time.Hour * 24)),
			dateTo:             ToProtoDate(startTime.Add(time.Duration(shopTariff.DaysMax) * time.Hour * 24)),
			interval:           &pb.DeliveryInterval{From: &pb.Time{Hour: 10}, To: &pb.Time{Hour: 12}},
			dServiceID:         171,
			partnerID:          171,
			logisticPointLmsID: 10000010125,
		}
		req := PrepareDeliveryFromPointRequest(&reqSettings)

		env, cancel := NewEnv(t, &genData, nil)
		defer cancel()

		wantDateFrom := startTime.Add(time.Hour * 24 * time.Duration(shopTariff.DaysMax))
		wantDateTo := startTime.Add(time.Hour * 24 * time.Duration(shopTariff.DaysMax))

		resp, err := env.Client.GetDeliveryRouteFromPoint(env.Ctx, req)

		require.NoError(t, err)
		require.NotNil(t, resp.Route)
		require.Equal(t, int(shopTariff.Cost+11+22+33), int(resp.Route.CostForShop))
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

		require.NotNil(t, resp.VirtualBox)
		require.Equal(t, expectedWeight, resp.VirtualBox.Weight)
		CheckIntervalsForExisting(t, resp)
	}
}
