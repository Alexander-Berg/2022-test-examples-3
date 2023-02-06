package lite

import (
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/test/requirepb"
	bg "a.yandex-team.ru/market/combinator/pkg/biggeneration"
	"a.yandex-team.ru/market/combinator/pkg/enums"
	ffboxes "a.yandex-team.ru/market/combinator/pkg/fulfillmentboxes"
	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/graph"
	"a.yandex-team.ru/market/combinator/pkg/outlets"
	partdel "a.yandex-team.ru/market/combinator/pkg/partial_delivery"
	tr "a.yandex-team.ru/market/combinator/pkg/tarifficator"
	pb "a.yandex-team.ru/market/combinator/proto/grpc"
)

func PreparePickupRequestGrouped(
	startTime time.Time,
	destRigs []uint32,
	cargoTypes []uint32,
	postCodes []uint32,
) *pb.PickupPointsRequest {
	req := pb.PickupPointsRequest{
		StartTime: ToProtoTimestamp(startTime),
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
						PartnerId:      145,
						AvailableCount: 1,
					},
				},
				CargoTypes: cargoTypes,
			},
			{
				RequiredCount: 2,
				Weight:        4000,
				Dimensions: []uint32{
					15,
					18,
					20,
				},
				AvailableOffers: []*pb.Offer{
					{
						ShopSku:        "",
						ShopId:         0,
						PartnerId:      145,
						AvailableCount: 3,
					},
				},
			},
		},
		DestinationRegions: destRigs,
		PostCodes:          postCodes,
	}
	return &req
}

func getMskUnused() *tr.TariffRT {
	return &tr.TariffRT{
		ID:                1,
		DeliveryServiceID: 106,
		DeliveryMethod:    enums.DeliveryMethodPickup,
		ProgramTypeList:   tr.ProgramTypeList(tr.ProgramMarketDelivery),
		Option: tr.Option{
			Cost:    13,
			DaysMin: 3,
			DaysMax: 5,
		},
		RuleAttrs: tr.RuleAttrs{
			WeightMax: 15000,
		},
		FromToRegions: tr.FromToRegions{
			From: 1,
			To:   213,
		},
		Type: tr.RuleTypePayment,
	}
}

func makeGlobalTariff(t *tr.TariffRT) *tr.TariffRT {
	res := *t
	res.Type = tr.RuleTypeGlobal
	return &res
}

func getMskFirst() *tr.TariffRT {
	return &tr.TariffRT{
		ID:                1,
		DeliveryServiceID: 106,
		DeliveryMethod:    enums.DeliveryMethodPost,
		ProgramTypeList:   tr.ProgramTypeList(tr.ProgramMarketDelivery),
		Option: tr.Option{
			Cost:    43,
			DaysMin: 5,
			DaysMax: 7,
		},
		RuleAttrs: tr.RuleAttrs{
			WeightMax: 20000,
		},
		FromToRegions: tr.FromToRegions{
			From: 1,
			To:   213,
		},
		Type: tr.RuleTypePayment,
	}
}

func getMskSecond() *tr.TariffRT {
	return &tr.TariffRT{
		ID:                1,
		DeliveryServiceID: 106,
		DeliveryMethod:    enums.DeliveryMethodPost,
		ProgramTypeList:   tr.ProgramTypeList(tr.ProgramMarketDelivery),
		RuleAttrs: tr.RuleAttrs{
			WeightMax: 20000,
			HeightMax: 30,
			LengthMax: 30,
			WidthMax:  30,
			DimSumMax: 90,
		},
		Points: MakePoints([]int64{
			10000971018,
			10000971019,
			10000971021,
			10000971022,
		}),
		FromToRegions: tr.FromToRegions{
			From: 1,
			To:   213,
		},
		Type: tr.RuleTypeForPoint,
	}
}

func getMskThird() *tr.TariffRT {
	return &tr.TariffRT{
		ID:                1,
		DeliveryServiceID: 106,
		DeliveryMethod:    enums.DeliveryMethodPost,
		ProgramTypeList:   tr.ProgramTypeList(tr.ProgramMarketDelivery),
		RuleAttrs: tr.RuleAttrs{
			WeightMax:       20000,
			HeightMax:       20,
			LengthMax:       20,
			WidthMax:        20,
			DimSumMax:       60,
			SortedDimLimits: [3]uint32{20, 20, 20},
		},
		Points: MakePoints([]int64{
			10000971023,
		}),
		FromToRegions: tr.FromToRegions{
			From: 1,
			To:   213,
		},
		Type: tr.RuleTypeForPoint,
	}
}

func getSpbFirst() *tr.TariffRT {
	return &tr.TariffRT{
		ID:                1,
		DeliveryServiceID: 106,
		DeliveryMethod:    enums.DeliveryMethodPickup,
		ProgramTypeList:   tr.ProgramTypeList(tr.ProgramMarketDelivery),
		Option: tr.Option{
			Cost:    90,
			DaysMin: 3,
			DaysMax: 4,
		},
		RuleAttrs: tr.RuleAttrs{
			WeightMax: 25000,
		},
		FromToRegions: tr.FromToRegions{
			From: 1,
			To:   geobase.RegionSaintPetersburg,
		},
		Type: tr.RuleTypePayment,
	}
}

func getSpbSecond() *tr.TariffRT {
	return &tr.TariffRT{
		ID:                1,
		DeliveryServiceID: 106,
		DeliveryMethod:    enums.DeliveryMethodPickup,
		ProgramTypeList:   tr.ProgramTypeList(tr.ProgramMarketDelivery),
		RuleAttrs: tr.RuleAttrs{
			WeightMax: 30000,
			HeightMax: 35,
			LengthMax: 30,
			WidthMax:  40,
			DimSumMax: 110,
		},
		Points: MakePoints([]int64{
			10000971020,
		}),
		FromToRegions: tr.FromToRegions{
			From: 1,
			To:   geobase.RegionSaintPetersburg,
		},
		Type: tr.RuleTypeForPoint,
	}
}

func checkGoodRequest(
	t *testing.T,
	req *pb.PickupPointsRequest,
	resp *pb.PickupPointsGrouped,
	startTime time.Time,
) {
	require.Equal(t, 2, len(resp.Groups))

	wantDateFrom := startTime.Add(time.Hour * 24 * time.Duration(getMskFirst().DaysMax))

	group := resp.Groups[0]
	require.Equal(t, 2, len(group.Points))
	requirepb.Equal(
		t,
		&pb.PointIds{
			PartnerId:       139,
			LogisticPointId: uint64(10000971018),
			RegionId:        uint32(geobase.RegionMoscow),
			PostCode:        outlets.UnknownPostCode,
		},
		group.Points[0],
	)
	requirepb.Equal(
		t,
		&pb.PointIds{
			PartnerId:       139,
			LogisticPointId: uint64(10000971019),
			RegionId:        uint32(geobase.RegionMoscow),
			PostCode:        uint32(123321),
		},
		group.Points[1],
	)
	require.Equal(t, pb.PickupPointType_POST_OFFICE, group.Type)
	require.Equal(t, []pb.PaymentMethod{pb.PaymentMethod_CARD}, group.PaymentMethods)
	requirepb.Equal(t, ToProtoDate(wantDateFrom), group.DateFrom)
	requirepb.Equal(t, ToProtoDate(wantDateFrom), group.DateTo)

	wantDateFrom = startTime.Add(time.Hour * 24 * time.Duration(getSpbFirst().DaysMax))
	group = resp.Groups[1]
	require.Equal(t, 1, len(group.Points))
	requirepb.Equal(
		t,
		&pb.PointIds{
			PartnerId:       140,
			LogisticPointId: uint64(10000971020),
			RegionId:        uint32(geobase.RegionSaintPetersburg),
			PostCode:        uint32(322),
		},
		group.Points[0],
	)
	require.Equal(t, pb.PickupPointType_PARCEL_LOCKER, group.Type)
	require.Equal(t, []pb.PaymentMethod{pb.PaymentMethod_CASH, pb.PaymentMethod_CARD}, group.PaymentMethods)
	requirepb.Equal(t, ToProtoDate(wantDateFrom), group.DateFrom)
	requirepb.Equal(t, ToProtoDate(wantDateFrom), group.DateTo)

	var expectedWeight uint32
	for _, item := range req.Items {
		expectedWeight += item.Weight * item.RequiredCount
	}
	expectedDimensions := []uint32{uint32(20), uint32(30), uint32(35)}

	require.NotNil(t, resp.VirtualBox)
	require.Equal(t, expectedWeight, resp.VirtualBox.Weight)
	require.Equal(t, expectedDimensions, resp.VirtualBox.Dimensions)
}

func checkGoodPostCodeRequest(
	t *testing.T,
	req *pb.PickupPointsRequest,
	resp *pb.PickupPointsGrouped,
	startTime time.Time,
) {
	require.Equal(t, 1, len(resp.Groups))

	wantDateFrom := startTime.Add(time.Hour * 24 * time.Duration(getMskFirst().DaysMax))

	group := resp.Groups[0]
	require.Equal(t, 1, len(group.Points))
	requirepb.Equal(
		t,
		&pb.PointIds{
			PartnerId:       139,
			LogisticPointId: uint64(10000971019),
			RegionId:        uint32(geobase.RegionMoscow),
			PostCode:        uint32(123321),
		},
		group.Points[0],
	)
	require.Equal(t, pb.PickupPointType_POST_OFFICE, group.Type)
	require.Equal(t, []pb.PaymentMethod{pb.PaymentMethod_CARD}, group.PaymentMethods)
	requirepb.Equal(t, ToProtoDate(wantDateFrom), group.DateFrom)
	requirepb.Equal(t, ToProtoDate(wantDateFrom), group.DateTo)
}

func TestGetPickupPointsGrouped(t *testing.T) {
	graphEx := graph.NewExample()
	regionMap := geobase.NewExample()
	tariffsFinder := tr.NewTariffsFinder()
	tariffsFinder.Add(getMskUnused())
	tariffsFinder.Add(makeGlobalTariff(getMskFirst()))
	tariffsFinder.Add(getMskFirst())
	tariffsFinder.Add(getMskSecond())
	tariffsFinder.Add(makeGlobalTariff(getSpbFirst()))
	tariffsFinder.Add(getSpbFirst())
	tariffsFinder.Add(getSpbSecond())
	tariffsFinder.Finish(&regionMap)
	genData := bg.GenerationData{
		RegionMap:     regionMap,
		TariffsFinder: NewFinderSet(tariffsFinder),
		Outlets: outlets.Make([]outlets.Outlet{
			outlets.Outlet{
				ID:                        10000971019,
				Type:                      outlets.Post,
				PostCode:                  123321,
				RegionID:                  geobase.RegionID(213),
				DeliveryServiceOutletCode: "test" + "10000971019", // COMBINATOR-2290: should ignore for non-DSBS
			},
			outlets.Outlet{
				ID:                        10000971018,
				Type:                      outlets.Post,
				PostCode:                  outlets.UnknownPostCode,
				DeliveryServiceOutletCode: "test" + "10000971018",
			},
			outlets.Outlet{
				ID:                        10000971020,
				Type:                      outlets.PostTerm,
				PostCode:                  322,
				DeliveryServiceOutletCode: "test" + "10000971020",
			},
		}, &regionMap, nil),
		Graph: graphEx.G,
	}

	startTime := time.Date(2020, 06, 02, 12, 4, 5, 0, time.UTC)

	req := PreparePickupRequestGrouped(startTime, []uint32{213, 2}, []uint32{1, 2, 3, 4}, nil)

	env, cancel := NewEnv(t, &genData, nil)
	defer cancel()

	resp, err := env.Client.GetPickupPointsGrouped(env.Ctx, req)

	require.NoError(t, err)
	checkGoodRequest(t, req, resp, startTime)

	// No data for this region
	req = PreparePickupRequestGrouped(startTime, []uint32{322}, []uint32{0}, nil)

	resp, err = env.Client.GetPickupPointsGrouped(env.Ctx, req)

	var expectedWeight uint32
	for _, item := range req.Items {
		expectedWeight += item.Weight * item.RequiredCount
	}
	expectedDimensions := []uint32{uint32(20), uint32(30), uint32(35)}

	require.NoError(t, err)
	require.Equal(t, 0, len(resp.Groups))
	require.NotNil(t, resp.VirtualBox)
	require.Equal(t, expectedWeight, resp.VirtualBox.Weight)
	require.Equal(t, expectedDimensions, resp.VirtualBox.Dimensions)

	// Unknown region, but good post_code for Moscow
	req = PreparePickupRequestGrouped(startTime, nil, []uint32{1, 2, 3, 4}, []uint32{123321}) // Пустой список регионов.

	env, cancel = NewEnv(t, &genData, nil)
	defer cancel()

	resp, err = env.Client.GetPickupPointsGrouped(env.Ctx, req)

	require.NoError(t, err)
	checkGoodPostCodeRequest(t, req, resp, startTime)

	// Bad cargo type 323
	req = PreparePickupRequestGrouped(startTime, []uint32{213, 2}, []uint32{111, 321, 323, 400}, nil)

	resp, err = env.Client.GetPickupPointsGrouped(env.Ctx, req)

	require.NoError(t, err)
	require.Equal(t, 0, len(resp.Groups))

	// Bad cargo type 322322 in pickup segment
	req = PreparePickupRequestGrouped(startTime, []uint32{213, 2}, []uint32{111, 321, 323, 400}, nil)

	resp, err = env.Client.GetPickupPointsGrouped(env.Ctx, req)

	require.NoError(t, err)
	require.Equal(t, 0, len(resp.Groups))
}

func TestHideDarkStroresInGetPickupPointsGrouped(t *testing.T) {
	graphEx := graph.NewExample()
	regionMap := geobase.NewExample()
	tariffsFinder := tr.NewTariffsFinder()
	tariffsFinder.Add(getMskUnused())
	tariffsFinder.Add(makeGlobalTariff(getMskFirst()))
	tariffsFinder.Add(getMskFirst())
	tariffsFinder.Add(getMskSecond())
	tariffsFinder.Add(makeGlobalTariff(getSpbFirst()))
	tariffsFinder.Add(getSpbFirst())
	tariffsFinder.Add(getSpbSecond())
	tariffsFinder.Finish(&regionMap)
	genData := bg.GenerationData{
		RegionMap:     regionMap,
		TariffsFinder: NewFinderSet(tariffsFinder),
		Outlets: outlets.Make([]outlets.Outlet{
			outlets.Outlet{
				ID:       10000971019,
				Type:     outlets.Depot,
				PostCode: 123321,
				RegionID: geobase.RegionID(213),
			},
			outlets.Outlet{
				ID:          10000971018,
				Type:        outlets.Depot,
				PostCode:    122,
				RegionID:    geobase.RegionID(213),
				IsDarkStore: true,
			},
			outlets.Outlet{
				ID:          10000971020,
				Type:        outlets.Mixed,
				PostCode:    322,
				RegionID:    geobase.RegionID(2),
				IsDarkStore: true,
			},
		}, &regionMap, nil),
		Graph: graphEx.G,
	}

	startTime := time.Date(2020, 06, 02, 12, 4, 5, 0, time.UTC)

	env, cancel := NewEnv(t, &genData, nil)
	defer cancel()

	// Ожидаем, что скрыли Дарк сторы
	req := PreparePickupRequestGrouped(startTime, []uint32{213, 2}, []uint32{1, 2, 3, 4}, nil)
	resp, err := env.Client.GetPickupPointsGrouped(env.Ctx, req)

	require.NoError(t, err)
	require.Equal(t, 1, len(resp.Groups))
	require.Equal(t, 1, len(resp.Groups[0].Points))
	require.Equal(t, uint64(10000971019), resp.Groups[0].Points[0].LogisticPointId)

	// Ожидаем, что успешно нашли "ничего"
	req = PreparePickupRequestGrouped(startTime, []uint32{322}, []uint32{0}, nil)
	resp, err = env.Client.GetPickupPointsGrouped(env.Ctx, req)

	require.NoError(t, err)
	require.Equal(t, 0, len(resp.Groups))
}

func TestShowDarkStroresInGetPickupPointsGrouped(t *testing.T) {
	graphEx := graph.NewExample()
	regionMap := geobase.NewExample()
	tariffsFinder := tr.NewTariffsFinder()
	tariffsFinder.Add(getMskUnused())
	tariffsFinder.Add(makeGlobalTariff(getMskFirst()))
	tariffsFinder.Add(getMskFirst())
	tariffsFinder.Add(getMskSecond())
	tariffsFinder.Add(makeGlobalTariff(getSpbFirst()))
	tariffsFinder.Add(getSpbFirst())
	tariffsFinder.Add(getSpbSecond())
	tariffsFinder.Finish(&regionMap)
	genData := bg.GenerationData{
		RegionMap:     regionMap,
		TariffsFinder: NewFinderSet(tariffsFinder),
		Outlets: outlets.Make([]outlets.Outlet{
			outlets.Outlet{
				ID:       10000971019,
				Type:     outlets.Depot,
				PostCode: 123321,
				RegionID: geobase.RegionID(213),
			},
			outlets.Outlet{
				ID:          10000971018,
				Type:        outlets.Depot,
				PostCode:    122,
				RegionID:    geobase.RegionID(213),
				IsDarkStore: true,
			},
			outlets.Outlet{
				ID:          10000971020,
				Type:        outlets.Mixed,
				PostCode:    322,
				RegionID:    geobase.RegionID(2),
				IsDarkStore: true,
			},
		}, &regionMap, nil),
		Graph: graphEx.G,
	}

	startTime := time.Date(2020, 06, 02, 12, 4, 5, 0, time.UTC)

	env, cancel := NewEnv(t, &genData, nil)
	defer cancel()

	// Ожидаем, что НЕ скрыли Дарксторы
	req := PreparePickupRequestGrouped(startTime, []uint32{213, 2}, []uint32{1, 2, 3, 4}, nil)
	req.AdditionalInfo = &pb.AdditionalInfo{ShowDarkstore: true}
	resp, err := env.Client.GetPickupPointsGrouped(env.Ctx, req)

	require.NoError(t, err)
	require.Equal(t, 1, len(resp.Groups))
	require.Equal(t, 2, len(resp.Groups[0].Points))
	require.Equal(t, uint64(10000971018), resp.Groups[0].Points[0].LogisticPointId)
	require.Equal(t, uint64(10000971019), resp.Groups[0].Points[1].LogisticPointId)

	// Ожидаем, что успешно нашли "ничего"
	req = PreparePickupRequestGrouped(startTime, []uint32{322}, []uint32{0}, nil)
	resp, err = env.Client.GetPickupPointsGrouped(env.Ctx, req)
	req.AdditionalInfo = &pb.AdditionalInfo{ShowDarkstore: true}

	require.NoError(t, err)
	require.Equal(t, 0, len(resp.Groups))
}

func TestFashionGetPickupPointsGrouped(t *testing.T) {
	graphEx := graph.NewExample()
	regionMap := geobase.NewExample()
	tariffsFinder := tr.NewTariffsFinder()
	tariffsFinder.Add(getMskUnused())
	tariffsFinder.Add(makeGlobalTariff(getMskFirst()))
	tariffsFinder.Add(getMskFirst())
	tariffsFinder.Add(getMskSecond())
	tariffsFinder.Add(makeGlobalTariff(getSpbFirst()))
	tariffsFinder.Add(getSpbFirst())
	tariffsFinder.Add(getSpbSecond())
	tariffsFinder.Finish(&regionMap)
	genData := bg.GenerationData{
		RegionMap:     regionMap,
		TariffsFinder: NewFinderSet(tariffsFinder),
		Outlets: outlets.Make([]outlets.Outlet{
			{
				ID:       10000971019,
				Type:     outlets.Post,
				PostCode: 123321,
				RegionID: geobase.RegionID(213),
			},
			{
				ID:       10000971018,
				Type:     outlets.Depot,
				PostCode: outlets.UnknownPostCode,
			},
			{
				ID:              10000971021,
				Type:            outlets.Depot,
				PostCode:        outlets.UnknownPostCode,
				IsMarketBranded: true,
			},
			{
				ID:              10000971022,
				Type:            outlets.PostTerm,
				PostCode:        outlets.UnknownPostCode,
				IsMarketBranded: true,
			},
			{
				ID:              10000971020,
				Type:            outlets.Depot,
				PostCode:        outlets.UnknownPostCode,
				RegionID:        geobase.RegionID(213),
				IsMarketBranded: true,
			},
		}, &regionMap, nil),
		Graph: graphEx.G,
	}

	startTime := time.Date(2020, 06, 02, 12, 4, 5, 0, time.UTC)

	req := PreparePickupRequestGrouped(startTime, []uint32{213, 2}, []uint32{600}, nil)
	req.Items[1].CargoTypes = []uint32{600}

	env, cancel := NewEnv(t, &genData, nil)
	defer cancel()
	// Fashion orders: return only branded depots of Moscow and Petersburg
	req.RearrFactors = "partial_delivery_flag_courier_options_v2=0;partial_delivery_flag_pickup_points_v2=0"
	partdel.Warehouses[145] = true
	resp, err := env.Client.GetPickupPointsGrouped(env.Ctx, req)

	require.NoError(t, err)
	require.Len(t, resp.Groups, 1)
	require.Len(t, resp.Groups[0].Points, 1)
	require.Equal(t, uint64(10000971020), resp.Groups[0].Points[0].LogisticPointId)

	// Disable fashion options if the flag is set
	req.DisablePartialDelivery = true
	resp, err = env.Client.GetPickupPointsGrouped(env.Ctx, req)
	require.NoError(t, err)
	require.Len(t, resp.Groups, 5)

	partdel.Warehouses[145] = false

}

func TestJewelryGetPickupPointsGrouped(t *testing.T) {
	graphEx := graph.NewExample()
	regionMap := geobase.NewExample()
	tariffsFinder := tr.NewTariffsFinder()
	tariffsFinder.Add(getMskUnused())
	tariffsFinder.Add(makeGlobalTariff(getMskFirst()))
	tariffsFinder.Add(getMskFirst())
	tariffsFinder.Add(getMskSecond())
	tariffsFinder.Add(makeGlobalTariff(getSpbFirst()))
	tariffsFinder.Add(getSpbFirst())
	tariffsFinder.Add(getSpbSecond())
	tariffsFinder.Finish(&regionMap)
	genData := bg.GenerationData{
		RegionMap:     regionMap,
		TariffsFinder: NewFinderSet(tariffsFinder),
		Outlets: outlets.Make([]outlets.Outlet{
			{
				ID:       10000971019,
				Type:     outlets.Post,
				PostCode: 123321,
				RegionID: geobase.RegionID(213),
			},
			{
				ID:       10000971018,
				Type:     outlets.Depot,
				PostCode: outlets.UnknownPostCode,
			},
			{
				ID:              10000971021,
				Type:            outlets.Depot,
				PostCode:        outlets.UnknownPostCode,
				IsMarketBranded: true,
			},
			{
				ID:              10000971022,
				Type:            outlets.PostTerm,
				PostCode:        outlets.UnknownPostCode,
				IsMarketBranded: true,
			},
			{
				ID:              10000971020,
				Type:            outlets.Depot,
				PostCode:        outlets.UnknownPostCode,
				RegionID:        geobase.RegionID(213),
				IsMarketBranded: true,
			},
		}, &regionMap, nil),
		Graph: graphEx.G,
	}

	startTime := time.Date(2020, 06, 02, 12, 4, 5, 0, time.UTC)

	req := PreparePickupRequestGrouped(startTime, []uint32{213, 2}, []uint32{80}, nil)
	req.Items[1].CargoTypes = []uint32{80}

	env, cancel := NewEnv(t, &genData, nil)
	defer cancel()
	// Jewelry orders: return only Post outlets
	req.RearrFactors = "split_jewelry_on_combinator=1;disable_pickup_for_jewelry=1"
	resp, err := env.Client.GetPickupPointsGrouped(env.Ctx, req)

	require.NoError(t, err)
	require.Len(t, resp.Groups, 1)
	require.Len(t, resp.Groups[0].Points, 1)
	require.Equal(t, uint64(10000971019), resp.Groups[0].Points[0].LogisticPointId)
}

func TestFulfillmentBoxesGetPickupPointsGrouped(t *testing.T) {
	graphEx := graph.NewExample()
	regionMap := geobase.NewExample()
	tariffsFinder := tr.NewTariffsFinder()
	tariffsFinder.Add(getMskUnused())
	tariffsFinder.Add(makeGlobalTariff(getMskFirst()))
	tariffsFinder.Add(getMskFirst())
	tariffsFinder.Add(getMskSecond())
	tariffsFinder.Add(getMskThird())
	tariffsFinder.Add(makeGlobalTariff(getSpbFirst()))
	tariffsFinder.Add(getSpbFirst())
	tariffsFinder.Add(getSpbSecond())
	tariffsFinder.Finish(&regionMap)
	genData := bg.GenerationData{
		RegionMap:     regionMap,
		TariffsFinder: NewFinderSet(tariffsFinder),
		Outlets: outlets.Make([]outlets.Outlet{
			{
				ID:       10000971019,
				Type:     outlets.Post,
				PostCode: 123321,
				RegionID: geobase.RegionID(213),
			},
			{
				ID:       10000971018,
				Type:     outlets.Depot,
				PostCode: outlets.UnknownPostCode,
			},
			{
				ID:              10000971021,
				Type:            outlets.Depot,
				PostCode:        outlets.UnknownPostCode,
				IsMarketBranded: true,
			},
			{
				ID:              10000971022,
				Type:            outlets.PostTerm,
				PostCode:        outlets.UnknownPostCode,
				IsMarketBranded: true,
			},
			{
				ID:              10000971020,
				Type:            outlets.Depot,
				PostCode:        outlets.UnknownPostCode,
				RegionID:        geobase.RegionID(213),
				IsMarketBranded: true,
			},
			{
				ID:              10000971023,
				Type:            outlets.PostTerm,
				PostCode:        outlets.UnknownPostCode,
				RegionID:        geobase.RegionID(213),
				IsMarketBranded: true,
			},
		}, &regionMap, nil),
		Graph: graphEx.G,
		FulfillmentBoxes: ffboxes.FulfillmentBoxesMap{
			145: ffboxes.FFBoxes{
				ffboxes.FFBox{
					Box: [3]int{5, 5, 5},
				},
				ffboxes.FFBox{
					Box: [3]int{21, 21, 21},
				},
			},
		},
	}

	startTime := time.Date(2020, 06, 02, 12, 4, 5, 0, time.UTC)

	req := PreparePickupRequestGrouped(startTime, []uint32{213, 2}, nil, nil)
	req.Items = req.Items[:1]

	// Товар 20x20x15 пролезает в постамат без флага
	env, cancel := NewEnv(t, &genData, nil)
	defer cancel()
	resp, err := env.Client.GetPickupPointsGrouped(env.Ctx, req)
	require.NoError(t, err)
	require.Len(t, resp.Groups, 6)
	require.Equal(t, []uint32{15, 20, 20}, resp.VirtualBox.Dimensions)

	// Товар 20x20x15 не пролезает в постамат с флагом
	req.RearrFactors = "check_fulfillment_box_lockers=1"
	resp, err = env.Client.GetPickupPointsGrouped(env.Ctx, req)
	require.NoError(t, err)
	require.Len(t, resp.Groups, 5)
	require.Equal(t, []uint32{21, 21, 21}, resp.VirtualBox.Dimensions)

	// Товар 22x20x15 virtual box как есть, потому что не влазит в коробку
	req.Items[0].Dimensions[0] = 22
	req.Items[0].Dimensions[1] = 22
	req.Items[0].Dimensions[2] = 5
	req.RearrFactors = "check_fulfillment_box_lockers=1"
	resp, err = env.Client.GetPickupPointsGrouped(env.Ctx, req)
	require.NoError(t, err)
	require.Len(t, resp.Groups, 5)
	require.Equal(t, []uint32{5, 22, 22}, resp.VirtualBox.Dimensions)
}
