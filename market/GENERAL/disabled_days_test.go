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
	"a.yandex-team.ru/market/combinator/pkg/outlets"
	tr "a.yandex-team.ru/market/combinator/pkg/tarifficator"
	pb "a.yandex-team.ru/market/combinator/proto/grpc"
)

func createFFRouteRequest(
	beruWarehousePartner int64,
	day uint32,
	deliveryType pb.DeliveryType,
	startTime time.Time,
	regionID geobase.RegionID,
) *pb.DeliveryRequest {
	interval := pb.DeliveryInterval{
		From: &pb.Time{
			Hour:   10,
			Minute: 0,
		},
		To: &pb.Time{
			Hour:   22,
			Minute: 0,
		},
	}
	if startTime.IsZero() {
		startTime = time.Date(2020, 06, 2, 2, 4, 5, 0, time.UTC)
	}
	return MakeRequest(
		RequestWithPartner(beruWarehousePartner),
		RequestWithRegion(regionID),
		RequestWithDeliveryOption(
			&pb.Date{Day: day, Month: 6, Year: 2020},
			&pb.Date{Day: day, Month: 6, Year: 2020},
		),
		RequestWithStartTime(startTime),
		RequestWithDeliveryType(deliveryType),
		RequestWithInterval(&interval),
	)
}

func createRouteRequest(
	beruWarehousePartner int64,
	day uint32,
	deliveryType pb.DeliveryType,
	startTime time.Time,
) *pb.DeliveryRequest {
	interval := pb.DeliveryInterval{
		From: &pb.Time{
			Hour:   10,
			Minute: 0,
		},
		To: &pb.Time{
			Hour:   22,
			Minute: 0,
		},
	}
	if startTime.IsZero() {
		startTime = time.Date(2020, 06, 2, 2, 4, 5, 0, time.UTC)
	}
	return MakeRequest(
		RequestWithPartner(
			beruWarehousePartner,
		),
		RequestWithRegion(213),
		RequestWithDeliveryOption(
			&pb.Date{Day: day, Month: 6, Year: 2020},
			&pb.Date{Day: day, Month: 6, Year: 2020},
		),
		RequestWithStartTime(startTime),
		RequestWithDeliveryType(deliveryType),
		RequestWithInterval(&interval),
	)
}

func createOffersRequest(
	startTime time.Time,
	destRigs []uint32,
	sourcePartnerID int64,
) *pb.OffersDeliveryRequest {
	if startTime.IsZero() {
		startTime = time.Date(2020, 06, 2, 2, 4, 5, 0, time.UTC)
	}
	req := pb.OffersDeliveryRequest{
		StartTime: ToProtoTimestamp(startTime),
		Offers: []*pb.SkuOffer{{
			Weight: 10000,
			Dimensions: []uint32{
				20,
				20,
				15,
			},
			Offer: &pb.Offer{ShopSku: "322",
				ShopId:         1,
				PartnerId:      uint64(sourcePartnerID),
				AvailableCount: 1},
		}},
		Destination: &pb.PointIds{RegionId: destRigs[0]},
	}
	return &req
}

func createDropshipRouteRequest(
	dropshipPartner int64,
	day uint32,
	deliveryType pb.DeliveryType,
	startTime time.Time,
) *pb.DeliveryRequest {
	interval := pb.DeliveryInterval{
		From: &pb.Time{
			Hour:   10,
			Minute: 0,
		},
		To: &pb.Time{
			Hour:   22,
			Minute: 0,
		},
	}
	if startTime.IsZero() {
		startTime = time.Date(2020, 06, 2, 2, 4, 5, 0, time.UTC)
	}
	return MakeRequest(
		RequestWithPartner(dropshipPartner),
		RequestWithRegion(213),
		RequestWithDeliveryOption(
			&pb.Date{Day: day, Month: 6, Year: 2020},
			&pb.Date{Day: day, Month: 6, Year: 2020},
		),
		RequestWithStartTime(startTime),
		RequestWithDeliveryType(deliveryType),
		RequestWithInterval(&interval),
	)
}

func createPickupRequestGrouped(
	startTime time.Time,
	destRigs []uint32,
	sourcePartnerID int64,
	rearr string,
) *pb.PickupPointsRequest {
	req := pb.PickupPointsRequest{
		StartTime:    ToProtoTimestamp(startTime),
		RearrFactors: rearr,
		Items: []*pb.DeliveryRequestItem{
			{
				RequiredCount: 1,
				Weight:        10000,
				Dimensions: []uint32{
					20,
					20,
					15,
				},
				AvailableOffers: []*pb.Offer{
					{
						ShopSku:        "322",
						ShopId:         1,
						PartnerId:      uint64(sourcePartnerID),
						AvailableCount: 1,
					},
				},
			},
		},
		DestinationRegions: destRigs,
	}
	return &req
}

func createGenData(
	warehouseSchedule *graph.Schedule,
	movementSchedule *graph.Schedule,
	linehaulSchedule *graph.Schedule,
	addLinehaulShipment bool,
	warehouseDisabledDates []string,
	movementDisabledDates []string,
	linehaulDisabledDates []string,
) (*bg.GenerationData, *graph.Example1) {
	gb := graph.NewGraphBuilder()
	graphEx, daysOffGrouped := graph.BuildExampleSchedule(
		gb,
		warehouseSchedule,
		movementSchedule,
		linehaulSchedule,
		addLinehaulShipment,
		warehouseDisabledDates,
		movementDisabledDates,
		linehaulDisabledDates,
	)
	tariffsBuilder := tr.NewTariffsBuilder()
	_ = tariffsBuilder.MakeTariff(
		tr.TariffWithPartner(uint64(graphEx.Linehaul.PartnerLmsID)),
		tr.TariffWithRegion(geobase.RegionMoscowAndObl, geobase.RegionMoscow),
	)
	_ = tariffsBuilder.MakeTariff(
		tr.TariffWithPartner(uint64(graphEx.Linehaul.PartnerLmsID)),
		tr.TariffWithRegion(geobase.RegionMoscowAndObl, geobase.RegionMoscow),
		tr.TariffWithPickup(gb.PickupLmsID),
	)
	regionPairs := [][2]geobase.RegionID{
		{geobase.RegionEkaterinburg, geobase.RegionEkaterinburg},
		{geobase.RegionNovosibirsk, geobase.RegionNovosibirsk},
	}
	for _, regionPair := range regionPairs {
		_ = tariffsBuilder.MakeTariff(
			tr.TariffWithPartner(uint64(graphEx.LinehaulRussia.PartnerLmsID)),
			tr.TariffWithRegion(regionPair[0], regionPair[1]),
		)
		_ = tariffsBuilder.MakeTariff(
			tr.TariffWithPartner(uint64(graphEx.LinehaulRussia.PartnerLmsID)),
			tr.TariffWithRegion(regionPair[0], regionPair[1]),
			tr.TariffWithPickup(gb.PickupLmsID),
		)
	}
	regionMap := geobase.NewExample()
	genData := &bg.GenerationData{
		RegionMap:           regionMap,
		TariffsFinder:       tariffsBuilder.TariffsFinder,
		Graph:               graphEx.Graph,
		DisabledDatesHashed: daysOffGrouped,
		Outlets: outlets.Make([]outlets.Outlet{
			outlets.Outlet{
				ID:       gb.PickupLmsID,
				RegionID: geobase.RegionMoscow,
				Type:     outlets.PostTerm,
			},
		}, &regionMap, nil),
	}

	genData.AddServiceDaysOff(daysOffGrouped)

	return genData, graphEx
}

func GetActualStartTimes(
	points []*pb.Route_Point,
	graphEx *graph.Example1,
) (lastMovement, dsProcessing, middleProcessing, handing int64) {
	for _, p := range points {
		if p.SegmentType == graph.SegmentTypeMovement.String() &&
			p.Ids.PartnerId == uint64(graphEx.Linehaul.PartnerLmsID) {
			for _, s := range p.Services {
				if s.Code == enums.ServiceMovement.String() {
					lastMovement = s.StartTime.Seconds
				}
			}
		}
		if p.SegmentType == graph.SegmentTypeWarehouse.String() &&
			p.Ids.PartnerId != 172 &&
			p.Ids.PartnerId != 171 &&
			p.Ids.PartnerId != 300 &&
			p.Ids.PartnerId != 301 {
			for _, s := range p.Services {
				if s.Code == enums.ServiceProcessing.String() {
					dsProcessing = s.StartTime.Seconds
				}
			}
		}
		if p.SegmentType == graph.SegmentTypeWarehouse.String() &&
			(p.Ids.PartnerId == 172 || p.Ids.PartnerId == 171 || p.Ids.PartnerId == 300 || p.Ids.PartnerId == 301) {
			for _, s := range p.Services {
				if s.Code == enums.ServiceProcessing.String() || s.Code == enums.ServiceSort.String() {
					middleProcessing = s.StartTime.Seconds
				}
			}
		}
		if p.SegmentType == graph.SegmentTypePickup.String() ||
			p.SegmentType == graph.SegmentTypeHanding.String() {
			for _, s := range p.Services {
				if s.Code == enums.ServiceHanding.String() {
					handing = s.StartTime.Seconds
				}
			}
		}
		// Очень часто пригождается при проверке нового кода, оставлю под комментом
		/*fmt.Fprintf(os.Stderr, "Segment: %s, PartnerID: %d\n", p.SegmentType, p.Ids.PartnerId)
		for _, s := range p.Services {
			fmt.Fprintf(os.Stderr, "\tService: %s, %v\n", s.Code, time.Unix(s.StartTime.Seconds, 0))
		}*/
	}
	return
}

func checkNoOptions(
	firstDeliveryDay int,
	deliveryType pb.DeliveryType,
	t *testing.T,
	env *Env,
	graphEx *graph.Example1,
	startTime time.Time,
) {
	day := 2
	for ; day < firstDeliveryDay; day++ {
		var req = createDropshipRouteRequest(
			graphEx.WarehouseDSviaSC.PartnerLmsID,
			uint32(day),
			deliveryType,
			startTime,
		)
		_, err := env.Client.GetDeliveryRoute(env.Ctx, req)
		require.Error(t, err)
	}
}

func checkResult(
	t *testing.T,
	graphEx *graph.Example1,
	resp *pb.DeliveryRoute,
	expectedDsTime time.Time,
	expectedMiddleTime time.Time,
	expectedLastMovement time.Time,
	expectedHanding time.Time,
	expectedDayFrom *pb.Date,
	offersNum int,
) {
	lastMovement, dsProcessing, MiddleProcessing, handing := GetActualStartTimes(resp.Route.Points, graphEx)
	// сегмент склада поставщика
	if !expectedDsTime.IsZero() {
		require.Equal(t, expectedDsTime.Unix(), dsProcessing)
	}
	// FF or SC warehouse segment
	require.Equal(t, expectedMiddleTime.Unix(), MiddleProcessing)
	// Last movement segment (before linehaul)
	require.Equal(t, expectedLastMovement.Unix(), lastMovement)
	// Handing or Pickup segment
	require.Equal(t, expectedHanding.Unix(), handing)
	// Check dateFrom
	requirepb.Equal(t, expectedDayFrom, resp.Route.DateFrom)
	// Common data validation
	require.Len(t, resp.Offers, offersNum)
	// Length of points (for dropship we have WH -> MV -> WH -> MV -> LL -> H)
	numPoints := 6
	if expectedDsTime.IsZero() {
		numPoints = 4
	}
	require.Len(t, resp.Route.Points, numPoints)
}

func TestLinehaulSchedule(t *testing.T) {
	/*
		Проверяем склеивание disabledDays у смежных сегментов Movement'a и Linahaul'a,
		а также применение сдвоенного disabledDays.
	*/
	movementDisabledDates := []string{
		"2020-06-05",
		"2020-06-04",
		"2020-06-01", // disabledOffset
	}
	movementSchedule, _ := graph.CreateAroundTheClockSchedule(false)

	linehaulDisabledDates := []string{
		"2020-06-02",
		"2020-06-03",
	}
	linehaulSchedule, _ := graph.CreateAroundTheClockSchedule(false)
	genData, graphEx := createGenData(
		nil,
		movementSchedule,
		linehaulSchedule,
		false,
		nil,
		movementDisabledDates,
		linehaulDisabledDates,
	)
	env, cancel := NewEnv(t, genData, nil)
	defer cancel()
	for _, deliveryType := range []pb.DeliveryType{pb.DeliveryType_COURIER, pb.DeliveryType_PICKUP} {
		// FF (pickup, delivery route)
		firstDeliveryDay := 7
		checkNoOptions(
			firstDeliveryDay,
			deliveryType,
			t,
			env,
			graphEx,
			time.Time{},
		)

		// Находим опцию только для firstDeliveryDay
		req := createRouteRequest(
			graphEx.Warehouse172.PartnerLmsID,
			uint32(firstDeliveryDay),
			deliveryType,
			time.Time{},
		)
		resp, err := env.Client.GetDeliveryRoute(env.Ctx, req)
		require.NoError(t, err)
		checkResult(
			t,
			graphEx,
			resp,
			time.Time{},
			time.Date(2020, 6, 6, 13, 59, 5, 0, time.UTC),
			time.Date(2020, 6, 6, 14, 0, 5, 0, time.UTC),
			time.Date(2020, 6, 7, 7, 0, 0, 0, time.UTC),
			&pb.Date{Day: uint32(firstDeliveryDay), Month: 6, Year: 2020},
			1,
		)

		// Проверяем, что опция находится и тогда, когда сдвиг не нужен
		req = createRouteRequest(
			graphEx.Warehouse172.PartnerLmsID,
			uint32(firstDeliveryDay),
			deliveryType,
			time.Date(2020, 6, 6, 14, 4, 5, 0, time.UTC),
		)
		resp, err = env.Client.GetDeliveryRoute(env.Ctx, req)
		require.NoError(t, err)
		checkResult(
			t,
			graphEx,
			resp,
			time.Time{},
			time.Date(2020, 6, 6, 13, 59, 5, 0, time.UTC),
			time.Date(2020, 6, 6, 14, 0, 5, 0, time.UTC),
			time.Date(2020, 6, 7, 7, 0, 0, 0, time.UTC),
			&pb.Date{Day: uint32(firstDeliveryDay), Month: 6, Year: 2020},
			1,
		)
	}
}

func TestScheduleEKB(t *testing.T) {
	/*
		Проверяем, что для Екатеренбурга используем UTC+5 таймзону
	*/
	warehouseSchedule, _ := graph.NewSchedule(
		graph.CreateWarehouseSchedule(), // c 8:00 - по 20:00
		false,
	)
	genData, graphEx := createGenData(warehouseSchedule, nil, nil, false, nil, nil, nil)
	env, cancel := NewEnv(t, genData, nil)
	defer cancel()
	deliveryType := pb.DeliveryType_COURIER
	firstDeliveryDay := 3
	// Проверяем утро
	req := createFFRouteRequest(
		graphEx.Warehouse300.PartnerLmsID,
		uint32(firstDeliveryDay),
		deliveryType,
		time.Date(2020, 6, 2, 2, 4, 5, 0, time.UTC),
		geobase.RegionEkaterinburg,
	)
	resp, err := env.Client.GetDeliveryRoute(env.Ctx, req)
	require.NoError(t, err)
	checkResult(
		t,
		graphEx,
		resp,
		time.Time{},
		time.Date(2020, 6, 2, 15, 0, 0, 0, time.UTC),
		time.Date(2020, 6, 2, 15, 1, 0, 0, time.UTC),
		time.Date(2020, 6, 3, 5, 0, 0, 0, time.UTC),
		&pb.Date{Day: uint32(firstDeliveryDay), Month: 6, Year: 2020},
		1,
	)
	req = createFFRouteRequest(
		graphEx.Warehouse172.PartnerLmsID,
		uint32(firstDeliveryDay),
		deliveryType,
		time.Date(2020, 6, 2, 2, 4, 5, 0, time.UTC),
		geobase.RegionMoscow,
	)
	resp, err = env.Client.GetDeliveryRoute(env.Ctx, req)
	require.NoError(t, err)
	checkResult(
		t,
		graphEx,
		resp,
		time.Time{},
		time.Date(2020, 6, 2, 17, 0, 0, 0, time.UTC),
		time.Date(2020, 6, 2, 17, 1, 0, 0, time.UTC),
		time.Date(2020, 6, 3, 7, 0, 0, 0, time.UTC),
		&pb.Date{Day: uint32(firstDeliveryDay), Month: 6, Year: 2020},
		1,
	)

	// Проверяем вечер, тут у нас +1 день в Екатеринбурге
	req = createFFRouteRequest(
		graphEx.Warehouse300.PartnerLmsID,
		uint32(firstDeliveryDay+1),
		deliveryType,
		time.Date(2020, 6, 2, 15, 4, 5, 0, time.UTC),
		geobase.RegionEkaterinburg,
	)
	resp, err = env.Client.GetDeliveryRoute(env.Ctx, req)
	require.NoError(t, err)
	checkResult(
		t,
		graphEx,
		resp,
		time.Time{},
		time.Date(2020, 6, 3, 14, 59, 5, 0, time.UTC),
		time.Date(2020, 6, 3, 15, 0, 5, 0, time.UTC),
		time.Date(2020, 6, 4, 5, 0, 0, 0, time.UTC),
		&pb.Date{Day: uint32(firstDeliveryDay + 1), Month: 6, Year: 2020},
		1,
	)
	// Москва доставит в тот же день (так как успеваем до 20:00)
	req = createFFRouteRequest(
		graphEx.Warehouse172.PartnerLmsID,
		uint32(firstDeliveryDay),
		deliveryType,
		time.Date(2020, 6, 2, 15, 4, 5, 0, time.UTC),
		geobase.RegionMoscow,
	)
	resp, err = env.Client.GetDeliveryRoute(env.Ctx, req)
	require.NoError(t, err)
	checkResult(
		t,
		graphEx,
		resp,
		time.Time{},
		time.Date(2020, 6, 2, 14, 59, 5, 0, time.UTC),
		time.Date(2020, 6, 2, 15, 0, 5, 0, time.UTC),
		time.Date(2020, 6, 3, 7, 0, 0, 0, time.UTC),
		&pb.Date{Day: uint32(firstDeliveryDay), Month: 6, Year: 2020},
		1,
	)
}

func TestScheduleNovosib(t *testing.T) {
	/*
		Проверяем, что для Новосибирскаа используем UTC+7 таймзону
	*/
	warehouseSchedule, _ := graph.NewSchedule(
		graph.CreateWarehouseSchedule(), // c 8:00 - по 20:00
		false,
	)
	genData, graphEx := createGenData(warehouseSchedule, nil, nil, false, nil, nil, nil)
	env, cancel := NewEnv(t, genData, nil)
	defer cancel()
	deliveryType := pb.DeliveryType_COURIER
	firstDeliveryDay := 3
	// Проверяем утро
	req := createFFRouteRequest(
		graphEx.WarehouseNovosib.PartnerLmsID,
		uint32(firstDeliveryDay),
		deliveryType,
		time.Date(2020, 6, 2, 0, 4, 5, 0, time.UTC),
		geobase.RegionNovosibirsk,
	)
	resp, err := env.Client.GetDeliveryRoute(env.Ctx, req)
	require.NoError(t, err)
	checkResult(
		t,
		graphEx,
		resp,
		time.Time{},
		time.Date(2020, 6, 2, 13, 0, 0, 0, time.UTC), // 20 в Новосибирске
		time.Date(2020, 6, 2, 13, 1, 0, 0, time.UTC),
		time.Date(2020, 6, 3, 3, 0, 0, 0, time.UTC),
		&pb.Date{Day: uint32(firstDeliveryDay), Month: 6, Year: 2020},
		1,
	)
	// Проверяем вечер, тут у нас +1 день в Новосибирске
	req = createFFRouteRequest(
		graphEx.WarehouseNovosib.PartnerLmsID,
		uint32(firstDeliveryDay+1),
		deliveryType,
		time.Date(2020, 6, 2, 15, 4, 5, 0, time.UTC),
		geobase.RegionNovosibirsk,
	)
	resp, err = env.Client.GetDeliveryRoute(env.Ctx, req)
	require.NoError(t, err)
	checkResult(
		t,
		graphEx,
		resp,
		time.Time{},
		time.Date(2020, 6, 3, 13, 0, 0, 0, time.UTC), // 20 в Новосибирске
		time.Date(2020, 6, 3, 13, 1, 0, 0, time.UTC),
		time.Date(2020, 6, 4, 3, 0, 0, 0, time.UTC),
		&pb.Date{Day: uint32(firstDeliveryDay + 1), Month: 6, Year: 2020},
		1,
	)
}

func TestLinehaulScheduleOnly(t *testing.T) {
	/*
		Проверяем склеивание disabledDays у смежных сегментов Movement'a и Linahaul'a,
		а также применение сдвоенного disabledDays. Случай пустых disabledDates
		в Movement'e.
	*/
	linehaulDisabledDates := []string{
		"2020-06-01",
		"2020-06-02",
		"2020-06-03",
		"2020-06-04",
		"2020-06-05",
	}
	linehaulSchedule, _ := graph.CreateAroundTheClockSchedule(false)
	genData, graphEx := createGenData(nil, nil, linehaulSchedule, false, nil, nil, linehaulDisabledDates)
	env, cancel := NewEnv(t, genData, nil)
	defer cancel()
	for _, deliveryType := range []pb.DeliveryType{pb.DeliveryType_COURIER, pb.DeliveryType_PICKUP} {
		// FF (pickup, delivery route)
		firstDeliveryDay := 7
		checkNoOptions(
			firstDeliveryDay,
			deliveryType,
			t,
			env,
			graphEx,
			time.Time{},
		)

		// Находим опцию только для firstDeliveryDay
		req := createDropshipRouteRequest(
			graphEx.WarehouseDSviaSC.PartnerLmsID,
			uint32(firstDeliveryDay),
			deliveryType,
			time.Time{},
		)
		resp, err := env.Client.GetDeliveryRoute(env.Ctx, req)
		require.NoError(t, err)
		checkResult(
			t,
			graphEx,
			resp,
			time.Date(2020, 6, 6, 13, 59, 5, 0, time.UTC),
			time.Date(2020, 6, 6, 14, 0, 5, 0, time.UTC),
			time.Date(2020, 6, 6, 14, 1, 5, 0, time.UTC),
			time.Date(2020, 6, 7, 7, 0, 0, 0, time.UTC),
			&pb.Date{Day: uint32(firstDeliveryDay), Month: 6, Year: 2020},
			1,
		)

		// Проверяем, что опция находится и тогда, когда сдвиг не нужен
		req = createDropshipRouteRequest(
			graphEx.WarehouseDSviaSC.PartnerLmsID,
			uint32(firstDeliveryDay),
			deliveryType,
			time.Date(2020, 6, 6, 14, 4, 5, 0, time.UTC),
		)
		resp, err = env.Client.GetDeliveryRoute(env.Ctx, req)
		require.NoError(t, err)
		checkResult(
			t,
			graphEx,
			resp,
			time.Date(2020, 6, 6, 13, 59, 5, 0, time.UTC),
			time.Date(2020, 6, 6, 14, 0, 5, 0, time.UTC),
			time.Date(2020, 6, 6, 14, 1, 5, 0, time.UTC),
			time.Date(2020, 6, 7, 7, 0, 0, 0, time.UTC),
			&pb.Date{Day: uint32(firstDeliveryDay), Month: 6, Year: 2020},
			1,
		)
	}
}

func TestBadDisableDate(t *testing.T) {
	/*
		Если стартуем в 2020-06-02 в 5:05, то попадаем
		в Movement.Movement в 2020-06-01 логистические сутки,
		поэтому disabledDates не учитывается.
	*/
	movementDisabledDates := []string{
		"2020-06-02", // disabledOffset
	}
	movementSchedule, _ := graph.CreateAroundTheClockSchedule(false)
	genData, graphEx := createGenData(nil, movementSchedule, nil, false, nil, movementDisabledDates, nil)
	env, cancel := NewEnv(t, genData, nil)
	defer cancel()
	for _, deliveryType := range []pb.DeliveryType{pb.DeliveryType_COURIER, pb.DeliveryType_PICKUP} {
		firstDeliveryDay := 3
		checkNoOptions(
			firstDeliveryDay,
			deliveryType,
			t,
			env,
			graphEx,
			time.Time{},
		)
		// Находим опцию только для firstDeliveryDay
		req := createRouteRequest(
			graphEx.Warehouse172.PartnerLmsID,
			uint32(firstDeliveryDay),
			deliveryType,
			time.Time{},
		)
		resp, err := env.Client.GetDeliveryRoute(env.Ctx, req)
		require.NoError(t, err)
		checkResult(
			t,
			graphEx,
			resp,
			time.Time{},
			time.Date(2020, 6, 2, 1, 59, 5, 0, time.UTC),
			time.Date(2020, 6, 2, 2, 0, 5, 0, time.UTC),
			time.Date(2020, 6, 3, 7, 0, 0, 0, time.UTC),
			&pb.Date{Day: uint32(firstDeliveryDay), Month: 6, Year: 2020},
			1,
		)
	}
}

func TestDisableDateOptimization(t *testing.T) {
	/*
		Если стартуем в 2020-06-02 в 17:05, то попадаем
		в Movement.Movement в 2020-06-02 логистические сутки,
		поэтому только 2020-06-03 в 12:00 сможем закончить перемещение.
		При этом заказ пользователю приедет 2020-06-04 в 10:00.

		Понимаем, что нам лучше просто начать собирать не 2020-06-02, а
		2020-06-03 в 17:05, это время мы и проверяем в сегментах и Movement'a
	*/
	movementDisabledDates := []string{
		"2020-06-02", // disabledOffset
	}
	movementSchedule, _ := graph.CreateAroundTheClockSchedule(false)
	warehouseSchedule, _ := graph.NewSchedule(
		graph.CreateWarehouseSchedule(), // everyday from 08:00 to 20:00
		false,
	)
	genData, graphEx := createGenData(
		warehouseSchedule, movementSchedule, nil, false,
		nil, movementDisabledDates, nil,
	)
	env, cancel := NewEnv(t, genData, nil)
	defer cancel()
	for _, deliveryType := range []pb.DeliveryType{pb.DeliveryType_COURIER, pb.DeliveryType_PICKUP} {
		firstDeliveryDay := 4
		checkNoOptions(
			firstDeliveryDay,
			deliveryType,
			t,
			env,
			graphEx,
			time.Date(2020, 6, 2, 14, 4, 5, 0, time.UTC),
		)
		// Находим опцию только для firstDeliveryDay
		req := createDropshipRouteRequest(
			graphEx.WarehouseDSviaSC.PartnerLmsID,
			uint32(firstDeliveryDay),
			deliveryType,
			time.Date(2020, 6, 2, 14, 4, 5, 0, time.UTC),
		)
		resp, err := env.Client.GetDeliveryRoute(env.Ctx, req)
		require.NoError(t, err)
		checkResult(
			t,
			graphEx,
			resp,
			time.Date(2020, 6, 3, 13, 59, 5, 0, time.UTC), // Here we shift schedule up to right border
			time.Date(2020, 6, 3, 14, 0, 5, 0, time.UTC),
			time.Date(2020, 6, 3, 14, 1, 5, 0, time.UTC),
			time.Date(2020, 6, 4, 7, 0, 0, 0, time.UTC),
			&pb.Date{Day: uint32(firstDeliveryDay), Month: 6, Year: 2020},
			1,
		)
	}
}

func TestUnableStartTimeShiftDropshipGetPickupPointsGrouped(t *testing.T) {
	/*
	  Воспроизводим проблему, при которой заказ приезжает на промежуточный склад,
	  но не может быть отправлен сразу же (в данном случае смотрим на CD -> FF).

	  Интересен случай, когда отгрузку со склада поставщика (CD) не получается
	  "подвинуть" в право, чтобы оптимизировать загруженность склада из-за disabledDate'ов.
	*/
	movementDisabledDates := []string{
		"2020-06-01",
		"2020-06-02",
		"2020-06-03",
		"2020-06-04",
	}
	movementSchedule, _ := graph.CreateAroundTheClockSchedule(false)
	warehouseDisabledDates := []string{
		"2020-06-03",
		"2020-06-04",
		"2020-06-05",
		"2020-06-06",
	}
	warehouseSchedule, _ := graph.CreateAroundTheClockSchedule(false)
	genData, graphEx := createGenData(warehouseSchedule, movementSchedule, nil, false, warehouseDisabledDates, movementDisabledDates, nil)
	env, cancel := NewEnv(t, genData, nil)
	defer cancel()

	req := createPickupRequestGrouped(
		time.Date(2020, 6, 2, 2, 4, 5, 0, time.UTC),
		[]uint32{213},
		graphEx.WarehouseDSviaSC.PartnerLmsID,
		"",
	)
	resp, err := env.Client.GetPickupPointsGrouped(env.Ctx, req)
	require.NoError(t, err, "GetPickupPointsGrouped failed")
	require.Len(t, resp.Groups, 1)

	group := resp.Groups[0]
	require.Len(t, group.Points, 1)
	requirepb.Equal(t, []pb.PaymentMethod{pb.PaymentMethod_PREPAYMENT, pb.PaymentMethod_CARD}, group.PaymentMethods)

	requirepb.Equal(
		t,
		&pb.Date{Year: 2020, Month: 6, Day: 8},
		group.DateFrom,
	)
	requirepb.Equal(
		t,
		&pb.Date{Year: 2020, Month: 6, Day: 8},
		group.DateTo,
	)
}

func TestUnableStartTimeShiftDropshipGetCourierOptions(t *testing.T) {
	/*
	  Воспроизводим проблему, при которой заказ приезжает на промежуточный склад,
	  но не может быть отправлен сразу же (в данном случае смотрим на CD -> FF).

	  Интересен случай, когда отгрузку со склада поставщика (CD) не получается
	  "подвинуть" в право, чтобы оптимизировать загруженность склада из-за disabledDate'ов.
	*/
	movementDisabledDates := []string{
		"2020-06-01",
		"2020-06-02",
		"2020-06-03",
		"2020-06-04",
	}
	movementSchedule, _ := graph.CreateAroundTheClockSchedule(false)
	warehouseDisabledDates := []string{
		"2020-06-03",
		"2020-06-04",
		"2020-06-05",
		"2020-06-06",
	}
	warehouseSchedule, _ := graph.CreateAroundTheClockSchedule(false)
	genData, graphEx := createGenData(warehouseSchedule, movementSchedule, nil, false, warehouseDisabledDates, movementDisabledDates, nil)
	env, cancel := NewEnv(t, genData, nil)
	defer cancel()

	firstDeliveryDay := 8
	req := createDropshipRouteRequest(
		graphEx.WarehouseDSviaSC.PartnerLmsID,
		uint32(firstDeliveryDay),
		pb.DeliveryType_COURIER,
		time.Date(2020, 6, 2, 2, 4, 5, 0, time.UTC),
	)
	resp, err := env.Client.GetCourierOptions(env.Ctx, req)
	require.NoError(t, err, "GetCourierOptions failed")
	require.Len(t, resp.Options, 10)

	option := resp.Options[0]
	requirepb.Equal(
		t,
		[]pb.PaymentMethod{
			pb.PaymentMethod_PREPAYMENT,
			pb.PaymentMethod_CASH,
			pb.PaymentMethod_CARD,
		},
		option.PaymentMethods,
	)

	requirepb.Equal(
		t,
		&pb.Date{Year: 2020, Month: 6, Day: uint32(firstDeliveryDay)},
		option.DateFrom,
	)
	requirepb.Equal(
		t,
		&pb.Date{Year: 2020, Month: 6, Day: uint32(firstDeliveryDay)},
		option.DateTo,
	)
}

func TestUnableStartTimeShiftDropship(t *testing.T) {
	/*
	  Воспроизводим проблему, при которой заказ приезжает на промежуточный склад,
	  но не может быть отправлен сразу же (в данном случае смотрим на CD -> FF).

	  Интересен случай, когда отгрузку со склада поставщика (CD) не получается
	  "подвинуть" в право, чтобы оптимизировать загруженность склада из-за disabledDate'ов.
	*/
	movementDisabledDates := []string{
		"2020-06-01",
		"2020-06-02",
		"2020-06-03",
		"2020-06-04",
	}
	movementSchedule, _ := graph.CreateAroundTheClockSchedule(false)
	warehouseDisabledDates := []string{
		"2020-06-03",
		"2020-06-04",
		"2020-06-05",
		"2020-06-06",
	}
	warehouseSchedule, _ := graph.CreateAroundTheClockSchedule(false)
	genData, graphEx := createGenData(warehouseSchedule, movementSchedule, nil, false, warehouseDisabledDates, movementDisabledDates, nil)
	env, cancel := NewEnv(t, genData, nil)
	defer cancel()
	for _, deliveryType := range []pb.DeliveryType{pb.DeliveryType_COURIER, pb.DeliveryType_PICKUP} {
		firstDeliveryDay := 8
		checkNoOptions(
			firstDeliveryDay,
			deliveryType,
			t,
			env,
			graphEx,
			time.Time{},
		)
		req1 := createDropshipRouteRequest(
			graphEx.WarehouseDSviaSC.PartnerLmsID,
			uint32(firstDeliveryDay),
			deliveryType,
			time.Time{},
		)
		resp1, err := env.Client.GetDeliveryRoute(env.Ctx, req1)
		require.NoError(t, err)
		checkResult(
			t,
			graphEx,
			resp1,
			time.Date(2020, 6, 7, 13, 59, 5, 0, time.UTC),
			time.Date(2020, 6, 7, 14, 0, 5, 0, time.UTC),
			time.Date(2020, 6, 7, 14, 1, 5, 0, time.UTC),
			time.Date(2020, 6, 8, 7, 0, 0, 0, time.UTC),
			&pb.Date{Day: uint32(firstDeliveryDay), Month: 6, Year: 2020},
			1,
		)

		// Already good start no need to shrink paths
		req2 := createDropshipRouteRequest(
			graphEx.WarehouseDSviaSC.PartnerLmsID,
			uint32(firstDeliveryDay),
			deliveryType,
			time.Date(2020, 6, 7, 14, 4, 5, 0, time.UTC),
		)
		resp2, err := env.Client.GetDeliveryRoute(env.Ctx, req2)
		require.NoError(t, err)
		checkResult(
			t,
			graphEx,
			resp2,
			time.Date(2020, 6, 7, 13, 59, 5, 0, time.UTC),
			time.Date(2020, 6, 7, 14, 0, 5, 0, time.UTC),
			time.Date(2020, 6, 7, 14, 1, 5, 0, time.UTC),
			time.Date(2020, 6, 8, 7, 0, 0, 0, time.UTC),
			&pb.Date{Day: uint32(firstDeliveryDay), Month: 6, Year: 2020},
			1,
		)

		// Check that shrinked and non shrinked paths are equal
		requirepb.Equal(t, resp1.Offers, resp2.Offers)
		requirepb.Equal(t, resp1.Route.Points, resp2.Route.Points)
		requirepb.Equal(t, resp1.Route.Paths, resp2.Route.Paths)
		requirepb.Equal(t, resp1.Route.DeliveryType, resp2.Route.DeliveryType)
		requirepb.Equal(t, resp1.Route.TariffId, resp2.Route.TariffId)
		requirepb.Equal(t, resp1.Route.Cost, resp2.Route.Cost)
		requirepb.Equal(t, resp1.Route.CostForShop, resp2.Route.CostForShop)
		requirepb.Equal(t, resp1.Route.DateFrom, resp2.Route.DateFrom)
		requirepb.Equal(t, resp1.Route.DateTo, resp2.Route.DateTo)

	}
}

func TestDropshipUnableToShiftGetPickupPointsGrouped(t *testing.T) {
	/*
	  Воспроизводим проблему, при которой заказ приезжает на промежуточный склад,
	  но не может быть отправлен сразу же (в данном случае смотрим на CD -> FF).

	  Интересен случай, когда отгрузку со склада поставщика (CD) не получается
	  "подвинуть" в право, чтобы оптимизировать загруженность склада из-за disabledDate'ов.

	  Здесь мы проверяем, что если на 21 день вперёд не будет доставки, то отдадим
	  хотя бы какую-то опцию.
	*/
	movementDisabledDates := []string{
		"2020-06-01",
		"2020-06-02",
		"2020-06-03",
		"2020-06-04",
	}
	movementSchedule, _ := graph.CreateAroundTheClockSchedule(false)
	disbledStart := time.Date(2020, 6, 3, 5, 5, 5, 0, time.UTC)
	numDisabled := 32
	warehouseDisabledDates := createDisabledDatesSlice(disbledStart, numDisabled)
	warehouseSchedule, _ := graph.CreateAroundTheClockSchedule(false)
	genData, graphEx := createGenData(warehouseSchedule, movementSchedule, nil, false, warehouseDisabledDates, movementDisabledDates, nil)
	env, cancel := NewEnv(t, genData, nil)
	defer cancel()

	req := createPickupRequestGrouped(
		time.Date(2020, 6, 2, 2, 4, 5, 0, time.UTC),
		[]uint32{213},
		graphEx.WarehouseDSviaSC.PartnerLmsID,
		"",
	)
	resp, err := env.Client.GetPickupPointsGrouped(env.Ctx, req)
	require.NoError(t, err, "GetPickupPointsGrouped failed")
	require.Len(t, resp.Groups, 1)

	group := resp.Groups[0]
	require.Len(t, group.Points, 1)
	requirepb.Equal(t, []pb.PaymentMethod{pb.PaymentMethod_PREPAYMENT, pb.PaymentMethod_CARD}, group.PaymentMethods)

	// Из-за того, что хорошего пути (где мы не храним на СЦ) нет
	// Мы выбираем самый быстрый путь
	requirepb.Equal(
		t,
		&pb.Date{Year: 2020, Month: 6, Day: 6},
		group.DateFrom,
	)
	requirepb.Equal(
		t,
		&pb.Date{Year: 2020, Month: 6, Day: 6},
		group.DateTo,
	)

	// FF + CD (pickup, delivery route)
	firstDeliveryDay := 6
	checkNoOptions(
		firstDeliveryDay,
		pb.DeliveryType_PICKUP,
		t,
		env,
		graphEx,
		time.Date(2020, 6, 2, 2, 4, 5, 0, time.UTC),
	)
	req2 := createDropshipRouteRequest(
		graphEx.WarehouseDSviaSC.PartnerLmsID,
		uint32(firstDeliveryDay),
		pb.DeliveryType_PICKUP,
		time.Date(2020, 6, 2, 2, 4, 5, 0, time.UTC),
	)
	resp2, err := env.Client.GetDeliveryRoute(env.Ctx, req2)
	require.NoError(t, err)
	checkResult(
		t,
		graphEx,
		resp2,
		time.Date(2020, 6, 3, 1, 59, 5, 0, time.UTC), // выбрали максимально сжатый маршрут (в данном случае на 12 часов)
		time.Date(2020, 6, 3, 2, 0, 5, 0, time.UTC),
		time.Date(2020, 6, 5, 9, 0, 0, 0, time.UTC),
		time.Date(2020, 6, 6, 7, 0, 0, 0, time.UTC),
		&pb.Date{Day: uint32(firstDeliveryDay), Month: 6, Year: 2020},
		1,
	)
}

func TestPartnersListExperiment(t *testing.T) {
	/*
			Проверяем склеивание disabledDays у смежных сегментов Movement'a и Linahaul'a,
			а также применение сдвоенного disabledDays.
		    Только для партнёров. Копия TestLinehaulSchedule
	*/
	movementDisabledDates := []string{
		"2020-06-05",
		"2020-06-04",
		"2020-06-01", // disabledOffset
	}
	movementSchedule, _ := graph.CreateAroundTheClockSchedule(false)
	linehaulDisabledDates := []string{
		"2020-06-02",
		"2020-06-03",
	}
	linehaulSchedule, _ := graph.CreateAroundTheClockSchedule(false)
	genData, graphEx := createGenData(nil, movementSchedule, linehaulSchedule, false, nil, movementDisabledDates, linehaulDisabledDates)
	env, cancel := NewEnv(t, genData, nil)
	defer cancel()
	for _, deliveryType := range []pb.DeliveryType{pb.DeliveryType_COURIER, pb.DeliveryType_PICKUP} {
		// FF (pickup, delivery route)
		firstDeliveryDay := 7
		checkNoOptions(
			firstDeliveryDay,
			deliveryType,
			t,
			env,
			graphEx,
			time.Time{},
		)

		// Находим опцию только для firstDeliveryDay
		req := createRouteRequest(
			graphEx.Warehouse172.PartnerLmsID,
			uint32(firstDeliveryDay),
			deliveryType,
			time.Time{},
		)
		resp, err := env.Client.GetDeliveryRoute(env.Ctx, req)
		require.NoError(t, err)
		checkResult(
			t,
			graphEx,
			resp,
			time.Time{},
			time.Date(2020, 6, 6, 13, 59, 5, 0, time.UTC),
			time.Date(2020, 6, 6, 14, 0, 5, 0, time.UTC),
			time.Date(2020, 6, 7, 7, 0, 0, 0, time.UTC),
			&pb.Date{Day: uint32(firstDeliveryDay), Month: 6, Year: 2020},
			1,
		)

		// Проверяем, что опция находится и тогда, когда сдвиг не нужен
		req = createRouteRequest(
			graphEx.Warehouse172.PartnerLmsID,
			uint32(firstDeliveryDay),
			deliveryType,
			time.Date(2020, 6, 6, 14, 4, 5, 0, time.UTC),
		)
		resp, err = env.Client.GetDeliveryRoute(env.Ctx, req)
		require.NoError(t, err)
		checkResult(
			t,
			graphEx,
			resp,
			time.Time{},
			time.Date(2020, 6, 6, 13, 59, 5, 0, time.UTC),
			time.Date(2020, 6, 6, 14, 0, 5, 0, time.UTC),
			time.Date(2020, 6, 7, 7, 0, 0, 0, time.UTC),
			&pb.Date{Day: uint32(firstDeliveryDay), Month: 6, Year: 2020},
			1,
		)
	}
}
