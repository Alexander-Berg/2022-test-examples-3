package lite

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	bg "a.yandex-team.ru/market/combinator/pkg/biggeneration"
	"a.yandex-team.ru/market/combinator/pkg/enums"
	"a.yandex-team.ru/market/combinator/pkg/express"
	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/graph"
	"a.yandex-team.ru/market/combinator/pkg/outlets"
	"a.yandex-team.ru/market/combinator/pkg/s3storage"
	tr "a.yandex-team.ru/market/combinator/pkg/tarifficator"
	pb "a.yandex-team.ru/market/combinator/proto/grpc"
)

func getMskReturnTariff() *tr.TariffRT {
	return &tr.TariffRT{
		ID:                1,
		DeliveryServiceID: 48,
		DeliveryMethod:    enums.DeliveryMethodPost,
		ProgramTypeList:   tr.ProgramTypeList(tr.ProgramBeruCrossdock | tr.ProgramMarketDelivery),
		RuleAttrs: tr.RuleAttrs{
			WeightMax: 20000,
			HeightMax: 30,
			LengthMax: 30,
			WidthMax:  30,
			DimSumMax: 90,
		},
		Points: MakePoints([]int64{
			10000971017,
			10000971018,
			10000971019,
			10000971020,
			10000971021,
			10000971022,
			10000971023,
		}),
		FromToRegions: tr.FromToRegions{
			From: 1,
			To:   213,
		},
		Type: tr.RuleTypeForPoint,
	}
}

func TestGetPickupPointsGroupedReturn(t *testing.T) {
	regionMap := geobase.NewExample()
	tariffsFinder := tr.NewTariffsFinder()
	tariff1, tariff2 := getMskFirst(), getMskReturnTariff()
	tariff1.DeliveryServiceID = 48 // this delivery service was selected for the PostPriceReturnLimitTest
	tariff1.ProgramTypeList = tr.ProgramTypeList(tr.ProgramBeruCrossdock | tr.ProgramMarketDelivery)
	tariffsFinder.Add(makeGlobalTariff(tariff1))
	tariffsFinder.Add(tariff1)
	tariffsFinder.Add(tariff2)
	tariffsFinder.Finish(&regionMap)
	genData := bg.GenerationData{
		RegionMap:     regionMap,
		TariffsFinder: NewFinderSet(tariffsFinder),
		Outlets: outlets.Make([]outlets.Outlet{
			outlets.Outlet{
				ID:            10000971018,
				Type:          outlets.Post,
				PostCode:      outlets.UnknownPostCode,
				ReturnAllowed: true,
			},
			outlets.Outlet{
				ID:            10000971019,
				Type:          outlets.Post,
				PostCode:      123321,
				RegionID:      geobase.RegionID(213),
				ReturnAllowed: true,
			},
			outlets.Outlet{
				ID:            10000971020,
				Type:          outlets.Depot,
				PostCode:      123321,
				RegionID:      geobase.RegionID(213),
				ReturnAllowed: true,
			},
			outlets.Outlet{
				ID:              10000971021,
				Type:            outlets.Depot,
				PostCode:        123321,
				RegionID:        geobase.RegionID(213),
				ReturnAllowed:   true,
				IsMarketBranded: true,
			},
			outlets.Outlet{
				ID:              10000971022,
				Type:            outlets.PostTerm,
				PostCode:        123321,
				RegionID:        geobase.RegionID(213),
				ReturnAllowed:   true,
				IsMarketBranded: true,
			},
		}, &regionMap, nil),
		S3: &s3storage.S3StorageData{
			DisablePostReturnsForPartners: make(map[uint64]struct{}),
		},
	}

	startTime := time.Date(2020, 06, 02, 12, 4, 5, 0, time.UTC)
	req := PreparePickupRequestGrouped(startTime, []uint32{213}, []uint32{1, 2, 3, 4}, nil)

	{
		// Tarniy is better than default, selecting 75735 over 172
		req.RearrFactors = "enable_dropship_return_through_market_outlets=0"
		graphEx := graph.NewReturnPickupExample(75735, 10001700279, 172, 10000010736)
		genData.Graph = graphEx.G
		env, cancel := NewEnv(t, &genData, nil)
		defer cancel()
		resp, err := env.Client.GetPickupPointsGroupedReturn(env.Ctx, req)
		require.NoError(t, err)
		require.Len(t, resp.Groups, 1)
		require.Len(t, resp.Groups[0].Points, 2)
	}
	{
		// Tarniy is not adjacent: selecting 172
		// Dropship is present but flag DropshipReturnThroughMarket is not set: returning only POST
		req.RearrFactors = "enable_dropship_return_through_market_outlets=0"
		graphEx := graph.NewReturnPickupExample(172, 10000010736, 12345, 12341234)
		genData.Graph = graphEx.G
		env, cancel := NewEnv(t, &genData, nil)
		defer cancel()
		resp, err := env.Client.GetPickupPointsGroupedReturn(env.Ctx, req)
		require.NoError(t, err)
		require.Len(t, resp.Groups, 1)
		require.Len(t, resp.Groups[0].Points, 2)
	}
	{
		// Dropship is present and flag is set but no Tarniy: returning POST only
		req.RearrFactors = "enable_dropship_return_through_market_outlets=1;enable_nonexpress_return_to_branded_depots=0"
		graphEx := graph.NewReturnPickupExample(172, 10000010736, 12345, 12341234)
		genData.Graph = graphEx.G
		env, cancel := NewEnv(t, &genData, nil)
		defer cancel()
		resp, err := env.Client.GetPickupPointsGroupedReturn(env.Ctx, req)
		require.NoError(t, err)
		require.Len(t, resp.Groups, 1)
		require.Len(t, resp.Groups[0].Points, 2)
	}
	{
		// Dropship is present and flag is set: returning POST and isMarketBranded outlets
		req.RearrFactors = "enable_dropship_return_through_market_outlets=1;enable_nonexpress_return_to_branded_depots=0"
		graphEx := graph.NewReturnPickupExample(75735, 10001700279, 172, 10000010736)
		genData.Graph = graphEx.G
		env, cancel := NewEnv(t, &genData, nil)
		defer cancel()
		resp, err := env.Client.GetPickupPointsGroupedReturn(env.Ctx, req)
		require.NoError(t, err)
		require.Len(t, resp.Groups, 2)
		require.Len(t, resp.Groups[0].Points, 1)
		require.Len(t, resp.Groups[1].Points, 2)
		PostPriceReturnLimitTest(t, &genData, env)
		PostReturnForDisabledPartnersTest(t, &genData, env)

		// COMBINATOR-3837 Не возвращать бПВЗ, если запретили ювелирку в ПВЗ
		ReturnJewelryToBrandedDepotsTest(t, &genData, env)
	}
	{
		// DSBS is present: always returning only POST
		req.RearrFactors = "enable_dropship_return_through_market_outlets=1;enable_nonexpress_return_to_branded_depots=0"
		graphEx := graph.NewReturnPickupExample(172, 10000010736, 75735, 10001700279)
		graphEx.Warehouse.PartnerType = enums.PartnerTypeDSBS // DSBS is present
		graphEx.G.Finish(context.Background())
		genData.Graph = graphEx.G
		env, cancel := NewEnv(t, &genData, nil)
		defer cancel()
		resp, err := env.Client.GetPickupPointsGroupedReturn(env.Ctx, req)
		require.NoError(t, err)
		require.Len(t, resp.Groups, 1)
		require.Len(t, resp.Groups[0].Points, 2)

		// COMBINATOR-3409 Ничего не возвращать, если запретили почту для DSBS
		PostReturnForDisabledDSBSTest(t, &genData, env)
	}
	{
		// No DSBS nor Dropship is present: returning all POST and DEPOT with return_allowed service
		req.RearrFactors = "enable_dropship_return_through_market_outlets=1;enable_nonexpress_return_to_branded_depots=0"
		graphEx := graph.NewReturnPickupExample(172, 10000010736, 12345, 12341234)
		graphEx.Warehouse.PartnerType = enums.PartnerTypeFulfillment // no dropship or DSBS present
		graphEx.G.Finish(context.Background())
		genData.Graph = graphEx.G
		env, cancel := NewEnv(t, &genData, nil)
		defer cancel()
		resp, err := env.Client.GetPickupPointsGroupedReturn(env.Ctx, req)
		require.NoError(t, err)
		require.Len(t, resp.Groups, 4)
		require.Len(t, resp.Groups[0].Points, 1)
		require.Len(t, resp.Groups[1].Points, 1)
		require.Len(t, resp.Groups[2].Points, 2)
		require.Len(t, resp.Groups[3].Points, 1)
	}
	{
		// branded depots must be returned for any nonexpress fbs under flag
		req.RearrFactors = "enable_dropship_return_through_market_outlets=1;enable_nonexpress_return_to_branded_depots=1"
		graphEx := graph.NewReturnPickupExample(172, 10000010736, 75735, 10001700279)
		genData.Graph = graphEx.G
		env, cancel := NewEnv(t, &genData, nil)
		defer cancel()
		resp, err := env.Client.GetPickupPointsGroupedReturn(env.Ctx, req)
		require.NoError(t, err)
		require.Len(t, resp.Groups, 2)
	}
	{
		// branded depots must not be returned for express fbs. In this case, only POST outlets must be returned
		req.RearrFactors = "enable_dropship_return_through_market_outlets=1;enable_nonexpress_return_to_branded_depots=1"
		graphEx := graph.NewReturnPickupExample(172, 10000010736, 75735, 10001700279)
		genData.Graph = graphEx.G
		genData.Express = &express.Express{Warehouses: map[int64]*express.Warehouse{145: {PartnerID: 145}}}
		env, cancel := NewEnv(t, &genData, nil)
		defer cancel()
		resp, err := env.Client.GetPickupPointsGroupedReturn(env.Ctx, req)
		require.NoError(t, err)
		require.Len(t, resp.Groups, 1)
		require.Equal(t, pb.PickupPointType_POST_OFFICE, resp.Groups[0].Type)
		genData.Express = nil
	}
	{
		// Dzerzhinsky is  chosen if there is the flag
		req.RearrFactors = "enable_dropship_return_through_market_outlets=1;enable_nonexpress_return_to_branded_depots=0"
		graphEx := graph.NewReturnPickupExample(101366, 10001804390, 172, 10000010736)
		genData.Graph = graphEx.G
		env, cancel := NewEnv(t, &genData, nil)
		defer cancel()
		resp, err := env.Client.GetPickupPointsGroupedReturn(env.Ctx, req)
		require.NoError(t, err)
		require.NotEmpty(t, resp.Groups)
	}
	{
		// Check that the pickup are properly filtered by Return Allowed flag
		req.RearrFactors = "enable_dropship_return_through_market_outlets=0;enable_nonexpress_return_to_branded_depots=0"
		graphEx := graph.NewReturnPickupExample(75735, 10001700279, 172, 10000010736)
		genData.Graph = graphEx.G
		index := genData.Outlets.Outlets[10000971019]
		genData.Outlets.OutletsSlice[index].ReturnAllowed = false
		env, cancel := NewEnv(t, &genData, nil)
		defer cancel()
		resp, err := env.Client.GetPickupPointsGroupedReturn(env.Ctx, req)
		require.NoError(t, err)
		require.Len(t, resp.Groups, 1)
		require.Len(t, resp.Groups[0].Points, 1)
	}
	{
		// No way for non-dropships
		// No Tarniy for dropships: will try to go to 172, but no pickup points there
		graphEx := graph.NewReturnPickupExample(75735, 10001700279, 172, 10000010736)
		graphEx.Warehouse.PartnerType = enums.PartnerTypeFulfillment // Not dropship
		graphEx.G.Finish(context.Background())
		genData.Graph = graphEx.G
		env, cancel := NewEnv(t, &genData, nil)
		defer cancel()
		resp, err := env.Client.GetPickupPointsGroupedReturn(env.Ctx, req)
		require.NoError(t, err)
		require.Empty(t, resp.Groups)
	}
}

func PostPriceReturnLimitTest(t *testing.T, genData *bg.GenerationData, env *Env) {

	// Post and outlets must be returned
	startTime := time.Date(2020, 06, 02, 12, 4, 5, 0, time.UTC)
	request := PreparePickupRequestGrouped(startTime, []uint32{213}, []uint32{1, 2, 3, 4}, nil)
	request.RearrFactors = "enable_dropship_return_through_market_outlets=1"
	request.TotalPrice = 10000
	resp, err := env.Client.GetPickupPointsGroupedReturn(env.Ctx, request)
	require.NoError(t, err)
	require.Len(t, resp.Groups, 2)
	require.Len(t, resp.Groups[0].Points, 1)
	require.Equal(t, pb.PickupPointType_SERVICE_POINT, resp.Groups[0].Type)
	require.Len(t, resp.Groups[1].Points, 2)
	require.Equal(t, pb.PickupPointType_POST_OFFICE, resp.Groups[1].Type)

	// Post is not returned when the price limit is exceeded
	request.TotalPrice = 300001
	resp, err = env.Client.GetPickupPointsGroupedReturn(env.Ctx, request)
	require.NoError(t, err)
	require.Len(t, resp.Groups, 1)
	require.Len(t, resp.Groups[0].Points, 1)
	require.Equal(t, pb.PickupPointType_SERVICE_POINT, resp.Groups[0].Type)
}

// PostReturnForDisabledPartnersTest COMBINATOR-3348 Не возвращаем опции почты для складов из списка
func PostReturnForDisabledPartnersTest(t *testing.T, genData *bg.GenerationData, env *Env) {
	// Должны вернуться почта и ПВЗ
	startTime := time.Date(2020, 06, 02, 12, 4, 5, 0, time.UTC)
	request := PreparePickupRequestGrouped(startTime, []uint32{213}, []uint32{1, 2, 3, 4}, nil)
	resp, err := env.Client.GetPickupPointsGroupedReturn(env.Ctx, request)
	require.NoError(t, err)
	require.Len(t, resp.Groups, 2)
	require.Len(t, resp.Groups[0].Points, 1)
	require.Equal(t, pb.PickupPointType_SERVICE_POINT, resp.Groups[0].Type)
	require.Len(t, resp.Groups[1].Points, 2)
	require.Equal(t, pb.PickupPointType_POST_OFFICE, resp.Groups[1].Type)

	// Почта не должна возвращаться, если партнёр в списке
	genData.S3.DisablePostReturnsForPartners[145] = struct{}{}
	resp, err = env.Client.GetPickupPointsGroupedReturn(env.Ctx, request)
	require.NoError(t, err)
	require.Len(t, resp.Groups, 1)
	require.Len(t, resp.Groups[0].Points, 1)
	require.Equal(t, pb.PickupPointType_SERVICE_POINT, resp.Groups[0].Type)
	delete(genData.S3.DisablePostReturnsForPartners, 145)
}

// COMBINATOR-3409 ничего не возвращаем, если для DSBS запретили почту
func PostReturnForDisabledDSBSTest(t *testing.T, genData *bg.GenerationData, env *Env) {

	startTime := time.Date(2020, 06, 02, 12, 4, 5, 0, time.UTC)
	request := PreparePickupRequestGrouped(startTime, []uint32{213}, []uint32{1, 2, 3, 4}, nil)

	// Вернули почту для DSBS
	resp, err := env.Client.GetPickupPointsGroupedReturn(env.Ctx, request)
	require.NoError(t, err)
	require.Len(t, resp.Groups, 1)
	require.Len(t, resp.Groups[0].Points, 2)
	require.Equal(t, pb.PickupPointType_POST_OFFICE, resp.Groups[0].Type)

	genData.S3.DisablePostReturnsForPartners[145] = struct{}{}
	// Ничего не вернули
	resp, err = env.Client.GetPickupPointsGroupedReturn(env.Ctx, request)
	require.NoError(t, err)
	require.Len(t, resp.Groups, 0)
	delete(genData.S3.DisablePostReturnsForPartners, 145)
}

// COMBINATOR-3837 не возвращаем бПВЗ, если для ювелирки возвраты через них запрещены
func ReturnJewelryToBrandedDepotsTest(t *testing.T, genData *bg.GenerationData, env *Env) {
	startTime := time.Date(2020, 06, 02, 12, 4, 5, 0, time.UTC)
	request := PreparePickupRequestGrouped(startTime, []uint32{213}, []uint32{80}, nil)

	// Под флагом вернули БПВЗ и почту для ювелирки
	request.RearrFactors = "enable_dropship_return_through_market_outlets=1;enable_jewelry_return_via_branded_depots=1"
	resp, err := env.Client.GetPickupPointsGroupedReturn(env.Ctx, request)
	require.NoError(t, err)
	require.Len(t, resp.Groups, 2)
	require.Len(t, resp.Groups[0].Points, 1)
	require.Len(t, resp.Groups[1].Points, 2)

	// Если флаг не посеччен, вернули только почту для ювелирки
	request.RearrFactors = "enable_dropship_return_through_market_outlets=1;enable_jewelry_return_via_branded_depots=0"
	resp, err = env.Client.GetPickupPointsGroupedReturn(env.Ctx, request)
	require.NoError(t, err)
	require.Len(t, resp.Groups, 1)
	require.Len(t, resp.Groups[0].Points, 2)
	require.Equal(t, pb.PickupPointType_POST_OFFICE, resp.Groups[0].Type)
}
