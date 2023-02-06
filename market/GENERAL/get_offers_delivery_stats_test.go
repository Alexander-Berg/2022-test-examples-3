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

func TestGetOffersDeliveryStats(t *testing.T) {
	shopTariff := tr.TariffRT{
		DeliveryMethod:  enums.DeliveryMethodCourier,
		ProgramTypeList: tr.ProgramTypeList(tr.ProgramMarketDelivery),
		Option: tr.Option{
			Cost:    42,
			DaysMin: 5,
			DaysMax: 7,
		},
		Type: tr.RuleTypePayment,
	}
	tariffsFinder := tr.NewTariffsFinder()
	tariffsFinder.Add(&shopTariff)

	genData := bg.GenerationData{
		RegionMap:     geobase.NewExample(),
		TariffsFinder: NewFinderSet(tariffsFinder),
		Graph:         graph.NewGraphWithHintsV3(nil),
	}

	startTime := time.Date(2020, 06, 02, 12, 4, 5, 0, time.UTC)

	req := &pb.OffersDeliveryRequest{
		StartTime: ToProtoTimestamp(startTime),
		Destination: &pb.PointIds{
			RegionId: 213,
		},
		Offers: []*pb.SkuOffer{
			&pb.SkuOffer{
				Offer: &pb.Offer{
					ShopSku:        "ssku",
					AvailableCount: 1,
				},
				MarketSku: "msku",
			},
		},
	}

	env, cancel := NewEnv(t, &genData, nil)
	defer cancel()

	resp, err := env.Client.GetOffersDeliveryStats(env.Ctx, req)
	require.Equal(t, nil, err)
	require.Equal(t, 0, len(resp.OffersDelivery))
}

func TestGetOffersDeliveryStatsWithGraph(t *testing.T) {
	graphEx := graph.NewExample()
	shopTariffCour := tr.TariffRT{
		DeliveryServiceID: 106,
		DeliveryMethod:    enums.DeliveryMethodCourier,
		ProgramTypeList:   tr.ProgramTypeList(tr.ProgramMarketDelivery),
		Option: tr.Option{
			Cost:    42,
			DaysMin: 5,
			DaysMax: 7,
		},
		FromToRegions: tr.FromToRegions{
			From: 1,
			To:   213,
		},
		Type: tr.RuleTypePayment,
	}
	// Важно, что тарифф клеится по партнёру последнего сегмента!
	customerTariffCour := tr.TariffRT{
		DeliveryServiceID: 106,
		DeliveryMethod:    enums.DeliveryMethodCourier,
		ProgramTypeList:   tr.ProgramTypeList(tr.ProgramMarketDelivery),
		Option: tr.Option{
			Cost:    123,
			DaysMin: 5,
			DaysMax: 7,
		},
		RuleAttrs: tr.RuleAttrs{
			WeightMax: 20000,
			HeightMax: 30,
			LengthMax: 30,
			WidthMax:  30,
			DimSumMax: 100,
		},
		FromToRegions: tr.FromToRegions{
			From: 1,
			To:   213,
		},
		Type: tr.RuleTypePayment,
	}
	shopTariffPost1 := tr.TariffRT{
		DeliveryServiceID: 106,
		DeliveryMethod:    enums.DeliveryMethodPost,
		ProgramTypeList:   tr.ProgramTypeList(tr.ProgramMarketDelivery),
		Option: tr.Option{
			Cost:    52,
			DaysMin: 10,
			DaysMax: 11,
		},
		FromToRegions: tr.FromToRegions{
			From: 1,
			To:   213,
		},
		Type: tr.RuleTypePayment,
	}
	shopTariffPost2 := tr.TariffRT{
		DeliveryServiceID: 106,
		DeliveryMethod:    enums.DeliveryMethodPost,
		ProgramTypeList:   tr.ProgramTypeList(tr.ProgramMarketDelivery),
		Points: MakePoints([]int64{
			10000971021,
		}),
		FromToRegions: tr.FromToRegions{
			From: 1,
			To:   213,
		},
		Type: tr.RuleTypeForPoint,
	}
	// Важно, что тарифф клеится по партнёру последнего сегмента!
	customerTariffPost := tr.TariffRT{
		DeliveryServiceID: 106,
		DeliveryMethod:    enums.DeliveryMethodPost,
		ProgramTypeList:   tr.ProgramTypeList(tr.ProgramMarketDelivery),
		Option: tr.Option{
			Cost:    321,
			DaysMin: 10,
			DaysMax: 11,
		},
		RuleAttrs: tr.RuleAttrs{
			WeightMax: 20000,
			HeightMax: 30,
			LengthMax: 30,
			WidthMax:  30,
			DimSumMax: 100,
		},
		FromToRegions: tr.FromToRegions{
			From: 1,
			To:   213,
		},
		Type: tr.RuleTypePayment,
	}
	tariffsFinder := tr.NewTariffsFinder()
	tariffsFinder.Add(&shopTariffCour)
	tariffsFinder.Add(&customerTariffCour)
	tariffsFinder.Add(&shopTariffPost1)
	tariffsFinder.Add(&shopTariffPost2)
	tariffsFinder.Add(&customerTariffPost)

	genData := bg.GenerationData{
		RegionMap:     geobase.NewExample(),
		TariffsFinder: NewFinderSet(tariffsFinder),
		Graph:         graphEx.G,
	}

	startTime := time.Date(2020, 06, 02, 12, 4, 5, 0, time.UTC)

	req := &pb.OffersDeliveryRequest{
		StartTime: ToProtoTimestamp(startTime),
		Destination: &pb.PointIds{
			RegionId: 213,
		},
		Offers: []*pb.SkuOffer{
			&pb.SkuOffer{
				Offer: &pb.Offer{
					ShopSku:        "ssku1",
					AvailableCount: 1,
					PartnerId:      uint64(graphEx.Warehouse.PartnerLmsID),
				},
				MarketSku:  "msku",
				Weight:     1000,
				Dimensions: []uint32{11, 22, 33},
			},
			&pb.SkuOffer{
				Offer: &pb.Offer{
					ShopSku:        "ssku2",
					AvailableCount: 1,
					PartnerId:      uint64(graphEx.Warehouse.PartnerLmsID),
				},
				MarketSku:  "msku",
				Weight:     1_000_000, // одинаковый msku => вес игнорируется
				Dimensions: []uint32{111, 222, 333},
			},
		},
	}

	env, cancel := NewEnv(t, &genData, nil)
	defer cancel()

	resp, err := env.Client.GetOffersDeliveryStats(env.Ctx, req)
	require.Equal(t, nil, err)
	require.Equal(t, 2, len(resp.OffersDelivery))
	requirepb.Equal(t, resp.OffersDelivery[0].CourierStats, resp.OffersDelivery[1].CourierStats)
	requirepb.Equal(t, resp.OffersDelivery[0].PickupStats, resp.OffersDelivery[1].PickupStats)
	requirepb.Equal(t, resp.OffersDelivery[0].PostStats, resp.OffersDelivery[1].PostStats)

	od := resp.OffersDelivery[0]
	{
		wantDateFrom := startTime.Add(time.Hour * 24 * time.Duration(shopTariffCour.DaysMin))
		wantDateTo := startTime.Add(time.Hour * 24 * time.Duration(shopTariffCour.DaysMax))
		courierStats := od.CourierStats
		require.NotNil(t, courierStats)
		// require.Equal(t, int(customerTariffCour.Cost), int(courierStats.Cost))
		requirepb.Equal(t, ToProtoDate(wantDateFrom), courierStats.DateFrom)
		requirepb.Equal(t, ToProtoDate(wantDateTo), courierStats.DateTo)
	}
	require.Nil(t, od.PickupStats)
	require.Nil(t, od.AirshipDeliveryStats)
	{
		wantDateFrom := startTime.Add(time.Hour * 24 * time.Duration(shopTariffPost1.DaysMin))
		wantDateTo := startTime.Add(time.Hour * 24 * time.Duration(shopTariffPost1.DaysMax))
		postStats := od.PostStats
		require.NotNil(t, postStats)
		// require.Equal(t, int(customerTariffPost.Cost), int(postStats.Cost))
		requirepb.Equal(t, ToProtoDate(wantDateFrom), postStats.DateFrom)
		requirepb.Equal(t, ToProtoDate(wantDateTo), postStats.DateTo)
	}
	{
		// COMBINATOR-3356 возвращать признак самовывоза только для ДСБС
		require.False(t, od.SelfPickupAvailable)
	}
	{
		// COMBINATOR-2563 Возвращать признак примерки в ручке статистики
		require.False(t, od.PartialDeliveryAvailable)
		req.RearrFactors = "combinator_read_partial_return_from_graph=1"
		shopTariffCour.IsMarketCourier = true
		customerTariffCour.IsMarketCourier = true
		req.Offers[0].CargoTypes = []uint32{600}
		resp, err := env.Client.GetOffersDeliveryStats(env.Ctx, req)
		require.NoError(t, err)
		require.Len(t, resp.OffersDelivery, 2)
		for _, offer := range resp.OffersDelivery {
			require.True(t, offer.PartialDeliveryAvailable)
		}
	}
	{
		// COMBINATOR-1986 При неактивном флаге не возвращать опции авиа
		graphEx := graph.NewExampleWithDS(int64(graph.DSYandexGoAvia))
		customerTariffCour.DeliveryServiceID = graph.DSYandexGoAvia
		shopTariffCour.DeliveryServiceID = graph.DSYandexGoAvia
		shopTariffPost1.DeliveryServiceID = graph.DSYandexGoAvia
		shopTariffPost2.DeliveryServiceID = graph.DSYandexGoAvia
		customerTariffPost.DeliveryServiceID = graph.DSYandexGoAvia

		genData := bg.GenerationData{
			RegionMap:     geobase.NewExample(),
			TariffsFinder: NewFinderSet(tariffsFinder),
			Graph:         graphEx.G,
		}

		env, cancel := NewEnv(t, &genData, nil)
		defer cancel()

		resp, err := env.Client.GetOffersDeliveryStats(env.Ctx, req)
		require.NoError(t, err)
		require.Len(t, resp.OffersDelivery, 0)

		req.RearrFactors = "avia_delivery=1"
		resp, err = env.Client.GetOffersDeliveryStats(env.Ctx, req)
		require.NoError(t, err)
		require.Len(t, resp.OffersDelivery, 2)
		require.Nil(t, resp.OffersDelivery[0].CourierStats)
		require.NotNil(t, resp.OffersDelivery[0].AirshipDeliveryStats)
	}
}

func TestGetOffersDeliveryStatsMany(t *testing.T) {
	gb := graph.NewGraphBuilder()
	graphEx := graph.BuildExample1(gb)
	tariffsBuilder := tr.NewTariffsBuilder()
	tariffsBuilder.MakeTariff(
		tr.TariffWithPartner(uint64(graphEx.Linehaul.PartnerLmsID)),
		tr.TariffWithRegion(1, 213),
	)
	tariffsBuilder.MakeTariff(
		tr.TariffWithPartner(uint64(graphEx.Linehaul.PartnerLmsID)),
		tr.TariffWithRegion(1, 213),
		tr.TariffWithPickup(graphEx.Pickup.PointLmsID),
	)
	genData := bg.GenerationData{
		RegionMap:     geobase.NewExample(),
		TariffsFinder: tariffsBuilder.TariffsFinder,
		Graph:         graphEx.Graph,
	}
	env, cancel := NewEnv(t, &genData, nil)
	defer cancel()

	startTime := time.Date(2020, 06, 02, 12, 4, 5, 0, time.UTC)
	req := &pb.OffersDeliveryRequest{
		StartTime: ToProtoTimestamp(startTime),
		Destination: &pb.PointIds{
			RegionId: 213,
		},
		Offers: []*pb.SkuOffer{
			&pb.SkuOffer{
				Offer: &pb.Offer{
					PartnerId: uint64(graphEx.Warehouse172.PartnerLmsID),
				},
				MarketSku:  "",
				Weight:     1_000,
				Dimensions: []uint32{11, 22, 33},
			},
			&pb.SkuOffer{
				Offer: &pb.Offer{
					PartnerId: uint64(graphEx.Warehouse172.PartnerLmsID),
				},
				MarketSku:  "",
				Weight:     2_000,
				Dimensions: []uint32{11, 22, 33},
			},
		},
	}
	resp, err := env.Client.GetOffersDeliveryStats(env.Ctx, req)
	require.Equal(t, nil, err)
	require.Equal(t, 2, len(resp.OffersDelivery))
	require.NotNil(t, resp.OffersDelivery[0].CourierStats)
	require.NotNil(t, resp.OffersDelivery[0].PickupStats)
	require.Nil(t, resp.OffersDelivery[0].PostStats)
	requirepb.Equal(t, resp.OffersDelivery[0].CourierStats, resp.OffersDelivery[1].CourierStats)
	requirepb.Equal(t, resp.OffersDelivery[0].PickupStats, resp.OffersDelivery[1].PickupStats)
	requirepb.Equal(t, resp.OffersDelivery[0].PostStats, resp.OffersDelivery[1].PostStats)
}

func TestUnableStartTimeShiftDropshipGetPickupPointsGroupedForOffersDeliveryStats(t *testing.T) {
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

	req := createOffersRequest(
		time.Date(2020, 6, 2, 2, 4, 5, 0, time.UTC),
		[]uint32{213},
		graphEx.WarehouseDSviaSC.PartnerLmsID,
	)
	resp, err := env.Client.GetOffersDeliveryStats(env.Ctx, req)
	require.NoError(t, err, "GetPickupPointsGrouped failed")
	require.Len(t, resp.OffersDelivery, 1)
	g := resp.OffersDelivery[0]
	requirepb.Equal(
		t,
		&pb.Date{Year: 2020, Month: 6, Day: 8},
		g.PickupStats.DateFrom,
	)
	requirepb.Equal(
		t,
		&pb.Date{Year: 2020, Month: 6, Day: 8},
		g.PickupStats.DateTo,
	)
}

func TestUnableStartTimeShiftDropshipGetCourierOptionsForOffersDeliveryStats(t *testing.T) {
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
	req := createOffersRequest(
		time.Date(2020, 6, 2, 2, 4, 5, 0, time.UTC),
		[]uint32{213},
		graphEx.WarehouseDSviaSC.PartnerLmsID,
	)
	resp, err := env.Client.GetOffersDeliveryStats(env.Ctx, req)
	require.NoError(t, err, "GetCourierOptions failed")
	require.Greater(t, len(resp.OffersDelivery), 0)

	requirepb.Equal(
		t,
		&pb.Date{Year: 2020, Month: 6, Day: uint32(firstDeliveryDay)},
		resp.OffersDelivery[0].CourierStats.DateFrom,
	)
	requirepb.Equal(
		t,
		&pb.Date{Year: 2020, Month: 6, Day: uint32(firstDeliveryDay)},
		resp.OffersDelivery[0].CourierStats.DateTo,
	)
}

func TestUnableStartTimeShiftDropshipForOffersDeliveryStats(t *testing.T) {
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
		req1 := createOffersRequest(
			time.Time{},
			[]uint32{213},
			graphEx.WarehouseDSviaSC.PartnerLmsID,
		)
		resp1, err := env.Client.GetOffersDeliveryStats(env.Ctx, req1)
		require.NoError(t, err)
		require.Equal(t, &pb.Date{Year: 2020, Month: 6, Day: uint32(8)},
			resp1.OffersDelivery[0].CourierStats.DateTo)
		require.Equal(t, &pb.Date{Year: 2020, Month: 6, Day: uint32(8)},
			resp1.OffersDelivery[0].PickupStats.DateTo)
		require.Equal(t, &pb.Date{Year: 2020, Month: 6, Day: uint32(firstDeliveryDay)},
			resp1.OffersDelivery[0].CourierStats.DateFrom)
		require.Equal(t, &pb.Date{Year: 2020, Month: 6, Day: uint32(firstDeliveryDay)},
			resp1.OffersDelivery[0].PickupStats.DateFrom)

		// Already good start no need to shrink paths
		req2 := createOffersRequest(
			time.Date(2020, 6, 7, 14, 4, 5, 0, time.UTC),
			[]uint32{213},
			graphEx.WarehouseDSviaSC.PartnerLmsID,
		)
		resp2, err := env.Client.GetOffersDeliveryStats(env.Ctx, req2)
		require.NoError(t, err)
		require.Equal(t, &pb.Date{Year: 2020, Month: 6, Day: uint32(8)},
			resp2.OffersDelivery[0].CourierStats.DateTo)
		require.Equal(t, &pb.Date{Year: 2020, Month: 6, Day: uint32(8)},
			resp2.OffersDelivery[0].PickupStats.DateTo)
		require.Equal(t, &pb.Date{Year: 2020, Month: 6, Day: uint32(firstDeliveryDay)},
			resp2.OffersDelivery[0].CourierStats.DateFrom)
		require.Equal(t, &pb.Date{Year: 2020, Month: 6, Day: uint32(firstDeliveryDay)},
			resp2.OffersDelivery[0].PickupStats.DateFrom)

		// Check that shrinked and non shrinked paths are equal
		requirepb.Equal(t, resp1.OffersDelivery, resp2.OffersDelivery)
	}
}

func TestDropshipUnableToShiftGetPickupPointsGroupedForOffersDeliveryStats(t *testing.T) {
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

	req := createOffersRequest(
		time.Date(2020, 6, 2, 2, 4, 5, 0, time.UTC),
		[]uint32{213},
		graphEx.WarehouseDSviaSC.PartnerLmsID,
	)
	resp, err := env.Client.GetOffersDeliveryStats(env.Ctx, req)
	require.NoError(t, err, "GetPickupPointsGrouped failed")
	require.Len(t, resp.OffersDelivery, 1)

	g := resp.OffersDelivery[0]

	// Из-за того, что хорошего пути (где мы не храним на СЦ) нет
	// Мы выбираем самый быстрый путь
	requirepb.Equal(
		t,
		&pb.Date{Year: 2020, Month: 6, Day: 6},
		g.PickupStats.DateFrom,
	)
	requirepb.Equal(
		t,
		&pb.Date{Year: 2020, Month: 6, Day: 6},
		g.PickupStats.DateTo,
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
	req2 := createOffersRequest(
		time.Date(2020, 6, 2, 2, 4, 5, 0, time.UTC),
		[]uint32{213},
		graphEx.WarehouseDSviaSC.PartnerLmsID,
	)
	resp2, err := env.Client.GetOffersDeliveryStats(env.Ctx, req2)
	require.NoError(t, err)
	require.Equal(t, &pb.Date{Year: 2020, Month: 6, Day: uint32(6)},
		resp2.OffersDelivery[0].CourierStats.DateTo)
	require.Equal(t, &pb.Date{Year: 2020, Month: 6, Day: uint32(6)},
		resp2.OffersDelivery[0].PickupStats.DateTo)
	require.Equal(t, &pb.Date{Year: 2020, Month: 6, Day: uint32(firstDeliveryDay)},
		resp2.OffersDelivery[0].CourierStats.DateFrom)
	require.Equal(t, &pb.Date{Year: 2020, Month: 6, Day: uint32(firstDeliveryDay)},
		resp2.OffersDelivery[0].PickupStats.DateFrom)

}
