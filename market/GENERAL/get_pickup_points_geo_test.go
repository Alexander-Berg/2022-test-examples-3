package lite

import (
	"sort"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	bg "a.yandex-team.ru/market/combinator/pkg/biggeneration"
	"a.yandex-team.ru/market/combinator/pkg/dsbs"
	"a.yandex-team.ru/market/combinator/pkg/enums"
	"a.yandex-team.ru/market/combinator/pkg/extradata"
	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/graph"
	"a.yandex-team.ru/market/combinator/pkg/outlets"
	tr "a.yandex-team.ru/market/combinator/pkg/tarifficator"
	"a.yandex-team.ru/market/combinator/pkg/units"
	pb "a.yandex-team.ru/market/combinator/proto/grpc"
)

func ValidatePickupPointGeoGroup(
	t *testing.T,
	group *pb.PickupPointGeoGroup,
	outletsCount int,
	total uint64,
	latCenter float64,
	lonCenter float64,
) {
	require.Equal(t, outletsCount, len(group.Outlets))
	require.Equal(t, total, group.Total)
	require.Equal(t, latCenter, group.GroupCenter.Lat)
	require.Equal(t, lonCenter, group.GroupCenter.Lon)
}

func ValidatePickupPointGeoStatistic(
	t *testing.T,
	statistic *pb.PickupPointsGeoStatistic,
	flags uint32,
	count uint32,
) {
	require.Equal(t, flags, statistic.Flags)
	require.Equal(t, count, statistic.Count)
}

type GenOutletsFunc func() []outlets.Outlet

func generateOutletsTestRequestPickupPointsGeoClusteringByTiles() []outlets.Outlet {
	return []outlets.Outlet{
		{
			ID:       10000971019,
			Type:     outlets.Depot,
			RegionID: geobase.RegionID(213),
			IsActive: true,
			GpsCoords: units.GpsCoords{
				Latitude:  10.0,
				Longitude: 10.0,
			},
		},
		{
			ID:       10000971018,
			Type:     outlets.Depot,
			PostCode: 1,
			RegionID: geobase.RegionID(213),
			IsActive: true,
			GpsCoords: units.GpsCoords{
				Latitude:  20.0,
				Longitude: 20.0,
			},
		},
		{
			ID:       10000971020,
			Type:     outlets.Depot,
			PostCode: 2,
			RegionID: geobase.RegionID(213),
			IsActive: true,
			GpsCoords: units.GpsCoords{
				Latitude:  30.0,
				Longitude: 30.0,
			},
		},
	}
}

// TestRequestPickupPointsGeoClusteringByTiles проверка работы распределения аутлетов по тайлам
// в зависимости от зума
func TestRequestPickupPointsGeoClusteringByTiles(t *testing.T) {
	genData := generateGenerationData(generateOutletsTestRequestPickupPointsGeoClusteringByTiles)
	startTime := time.Date(2020, 06, 02, 12, 4, 5, 0, time.UTC)

	env, cancel := NewEnv(t, genData, nil)
	defer cancel()

	req := PreparePickupPointsGeoRequest(
		startTime,
		0,
		0,
		2, // Берем маленький зум, чтобы все аутлеты попали в один тайл
		units.GpsCoords{
			Latitude:  -90.0,
			Longitude: -180.0,
		},
		units.GpsCoords{
			Latitude:  90.0,
			Longitude: 180.0,
		},
		[]uint32{0},
		[]uint32{},
		"")
	resp, err := env.Client.GetPickupPointsGeo(env.Ctx, req)
	require.NoError(t, err)
	require.Equal(t, 1, len(resp.Groups))
	ValidatePickupPointGeoGroup(t, resp.Groups[0], 3, uint64(3), 20.0, 20.0)

	req = PreparePickupPointsGeoRequest(
		startTime,
		0,
		0,
		20, // Берем большой зум, чтобы все аутлеты распределились по разным тайлам
		units.GpsCoords{
			Latitude:  -90.0,
			Longitude: -180.0,
		},
		units.GpsCoords{
			Latitude:  90.0,
			Longitude: 180.0,
		},
		[]uint32{0},
		[]uint32{0},
		"")
	resp, err = env.Client.GetPickupPointsGeo(env.Ctx, req)
	require.NoError(t, err)
	require.Equal(t, 3, len(resp.Groups))

	require.Equal(t, 2, len(resp.Statistic))
	ValidatePickupPointGeoGroup(t, resp.Groups[0], 1, uint64(1), 20.0, 20.0)
	ValidatePickupPointGeoGroup(t, resp.Groups[1], 1, uint64(1), 10.0, 10.0)
	ValidatePickupPointGeoGroup(t, resp.Groups[2], 1, uint64(1), 30.0, 30.0)

	ValidatePickupPointGeoStatistic(t, resp.Statistic[0], uint32(pb.OutletFilters_PICKUP), uint32(1))
	ValidatePickupPointGeoStatistic(t, resp.Statistic[1], uint32(pb.OutletFilters_PICKUP)|uint32(pb.OutletFilters_AROUND_THE_CLOCK)|uint32(pb.OutletFilters_EVERY_DAY), uint32(2))
}

func generateOutletsTestRequestPickupPointsGeoFiltersAndStatistics() []outlets.Outlet {
	// TODO: Нужно будет увеличить разнообразие тестируемых флажков
	// 1 Market branded
	// 1 Every day & Around the clock
	// 1 Postamat & Every day & Around the clock
	// 1 Market branded & Every day & Around the clock
	// 1 Postamat & Market branded & Every day & Around the clock
	return []outlets.Outlet{
		{
			ID:              10000971018,
			Type:            outlets.PostTerm,
			PostCode:        1,
			RegionID:        geobase.RegionMoscow,
			IsActive:        true,
			IsMarketBranded: true,
			GpsCoords: units.GpsCoords{
				Latitude:  10.0,
				Longitude: 10.0,
			},
		},
		{
			ID:              10000971019,
			Type:            outlets.Depot,
			RegionID:        geobase.RegionMoscow,
			IsActive:        true,
			IsMarketBranded: true,
			GpsCoords: units.GpsCoords{
				Latitude:  20.0,
				Longitude: 20.0,
			},
		},
		{
			ID:       10000971020,
			Type:     outlets.PostTerm,
			PostCode: 2,
			RegionID: geobase.RegionSaintPetersburg,
			IsActive: true,
			GpsCoords: units.GpsCoords{
				Latitude:  30.0,
				Longitude: 30.0,
			},
		},
		{
			ID:              10000971021,
			Type:            outlets.Depot,
			PostCode:        2,
			RegionID:        geobase.RegionMoscow,
			IsActive:        true,
			IsMarketBranded: true,
			GpsCoords: units.GpsCoords{
				Latitude:  40.0,
				Longitude: 40.0,
			},
		},
		{
			ID:       10000971022,
			Type:     outlets.Post,
			PostCode: 2,
			RegionID: geobase.RegionMoscow,
			IsActive: true,
			GpsCoords: units.GpsCoords{
				Latitude:  50.0,
				Longitude: 50.0,
			},
		},
	}
}

// TestRequestPickupPointsGeoFiltersAndStatistics - проверка фильтрации ПВЗ по переданным флагам и корректности подсчета
// итоговой статистики
func TestRequestPickupPointsGeoFiltersAndStatistics(t *testing.T) {
	genData := generateGenerationData(generateOutletsTestRequestPickupPointsGeoFiltersAndStatistics)
	startTime := time.Date(2020, 06, 02, 12, 4, 5, 0, time.UTC)

	env, cancel := NewEnv(t, genData, nil)
	defer cancel()

	req := PreparePickupPointsGeoRequest(
		startTime,
		0,
		0,
		2, // Берем маленький зум, чтобы все аутлеты попали в один тайл
		units.GpsCoords{
			Latitude:  -90.0,
			Longitude: -180.0,
		},
		units.GpsCoords{
			Latitude:  90.0,
			Longitude: 180.0,
		},
		[]uint32{0},
		[]uint32{0},
		"")
	resp, err := env.Client.GetPickupPointsGeo(env.Ctx, req)
	require.NoError(t, err)
	require.Equal(t, 1, len(resp.Groups))
	ValidatePickupPointGeoGroup(t, resp.Groups[0], 4, uint64(4), 25.0, 25.0)

	req = PreparePickupPointsGeoRequest(
		startTime,
		0,
		0,
		20, // Берем большой зум, чтобы все аутлеты распределились по разным тайлам
		units.GpsCoords{
			Latitude:  -90.0,
			Longitude: -180.0,
		},
		units.GpsCoords{
			Latitude:  90.0,
			Longitude: 180.0,
		},
		[]uint32{0},
		[]uint32{0},
		"")
	resp, err = env.Client.GetPickupPointsGeo(env.Ctx, req)
	require.NoError(t, err)
	require.Equal(t, 4, len(resp.Groups))
	ValidatePickupPointGeoGroup(t, resp.Groups[0], 1, uint64(1), 10.0, 10.0)
	ValidatePickupPointGeoGroup(t, resp.Groups[1], 1, uint64(1), 20.0, 20.0)
	ValidatePickupPointGeoGroup(t, resp.Groups[2], 1, uint64(1), 40.0, 40.0)
	ValidatePickupPointGeoGroup(t, resp.Groups[3], 1, uint64(1), 30.0, 30.0)
	require.Equal(t, 4, len(resp.Statistic))
	ValidatePickupPointGeoStatistic(t, resp.Statistic[0], uint32(pb.OutletFilters_POSTAMAT)|uint32(pb.OutletFilters_AROUND_THE_CLOCK)|uint32(pb.OutletFilters_EVERY_DAY), uint32(1))
	ValidatePickupPointGeoStatistic(t, resp.Statistic[1], uint32(pb.OutletFilters_POSTAMAT)|uint32(pb.OutletFilters_MARKET_BRANDED)|uint32(pb.OutletFilters_AROUND_THE_CLOCK)|uint32(pb.OutletFilters_EVERY_DAY), uint32(1))
	ValidatePickupPointGeoStatistic(t, resp.Statistic[2], uint32(pb.OutletFilters_MARKET_BRANDED)|uint32(pb.OutletFilters_PICKUP)|uint32(pb.OutletFilters_IS_TRYING_AVAILABLE), uint32(1))
	ValidatePickupPointGeoStatistic(t, resp.Statistic[3], uint32(pb.OutletFilters_MARKET_BRANDED)|uint32(pb.OutletFilters_AROUND_THE_CLOCK)|uint32(pb.OutletFilters_EVERY_DAY)|uint32(pb.OutletFilters_PICKUP)|uint32(pb.OutletFilters_IS_TRYING_AVAILABLE), uint32(1))
}

func generateOutletsTestRequestPickupPointsGeoPost() []outlets.Outlet {
	return []outlets.Outlet{
		{
			ID:       10000971020,
			Type:     outlets.Post,
			PostCode: 2,
			RegionID: geobase.RegionSaintPetersburg,
			IsActive: true,
			GpsCoords: units.GpsCoords{
				Latitude:  30.0,
				Longitude: 30.0,
			},
		},
		{
			ID:       10000971022,
			Type:     outlets.Post,
			PostCode: 2,
			RegionID: geobase.RegionMoscow,
			IsActive: true,
			GpsCoords: units.GpsCoords{
				Latitude:  50.0,
				Longitude: 50.0,
			},
		},
		{
			ID:                        10000971023,
			Type:                      outlets.Post,
			PostCode:                  2,
			RegionID:                  geobase.RegionEkaterinburg,
			IsActive:                  true,
			DeliveryServiceOutletCode: "1",
			GpsCoords: units.GpsCoords{
				Latitude:  60.0,
				Longitude: 60.0,
			},
		},
	}
}

// TestRequestPickupPointsGeoPost - проверка фильтрации ПВЗ почты
func TestRequestPickupPointsGeoPost(t *testing.T) {
	genData := generateGenerationData(generateOutletsTestRequestPickupPointsGeoPost)
	startTime := time.Date(2020, 06, 02, 12, 4, 5, 0, time.UTC)

	env, cancel := NewEnv(t, genData, nil)
	defer cancel()
	// Отбрасываются почтовые ПВЗ Москвы и Спб, Екб отдается
	req := PreparePickupPointsGeoRequest(
		startTime,
		0,
		0,
		20, // Берем большой зум, чтобы все аутлеты распределились по разным тайлам
		units.GpsCoords{
			Latitude:  -90.0,
			Longitude: -180.0,
		},
		units.GpsCoords{
			Latitude:  90.0,
			Longitude: 180.0,
		},
		[]uint32{0},
		[]uint32{0},
		"")
	resp, err := env.Client.GetPickupPointsGeo(env.Ctx, req)
	require.NoError(t, err)
	sort.Slice(
		resp.Groups,
		func(i, j int) bool {
			if resp.Groups[i].GroupCenter.Lat < resp.Groups[j].GroupCenter.Lat {
				return true
			}
			if resp.Groups[i].GroupCenter.Lon < resp.Groups[j].GroupCenter.Lon {
				return true
			}
			return false
		},
	)
	require.Equal(t, 1, len(resp.Groups))
	ValidatePickupPointGeoGroup(t, resp.Groups[0], 1, uint64(1), 60.0, 60.0)
	require.Equal(t, 10000971023, int(resp.Groups[0].Outlets[0].LogisticPointId))
	require.Equal(t, 1, len(resp.Statistic))
	ValidatePickupPointGeoStatistic(t, resp.Statistic[0], uint32(pb.OutletFilters_POST)|uint32(pb.OutletFilters_EVERY_DAY)|uint32(pb.OutletFilters_AROUND_THE_CLOCK), uint32(1))
}

func generateOutletsTestRequestPickupPointsGeoRegionRestrictions() []outlets.Outlet {
	gpsCoords := units.GpsCoords{
		Latitude:  10.0,
		Longitude: 10.0,
	}
	return []outlets.Outlet{
		{
			ID:        10000971018,
			Type:      outlets.Depot,
			PostCode:  1,
			RegionID:  geobase.RegionID(213),
			IsActive:  true,
			GpsCoords: gpsCoords,
		},
		{
			ID:              10000971019,
			Type:            outlets.Depot,
			RegionID:        geobase.RegionID(213),
			IsActive:        true,
			IsMarketBranded: true,
			GpsCoords:       gpsCoords,
		},
		{
			ID:        10000971020,
			Type:      outlets.Depot,
			PostCode:  2,
			RegionID:  geobase.RegionID(213),
			IsActive:  true,
			GpsCoords: gpsCoords,
		},
		{
			ID:              10000971021,
			Type:            outlets.Depot,
			PostCode:        2,
			RegionID:        geobase.RegionID(120542),
			IsActive:        true,
			IsMarketBranded: true,
			GpsCoords:       gpsCoords,
		},
		{
			ID:        10000971022,
			Type:      outlets.Depot,
			PostCode:  2,
			RegionID:  geobase.RegionID(20279),
			IsActive:  true,
			GpsCoords: gpsCoords,
		},
	}
}

// TestRequestPickupPointsGeoRegionRestrictions - проверяем работу ограничения на аутлеты по регионам
func TestRequestPickupPointsGeoRegionRestrictions(t *testing.T) {
	genData := generateGenerationData(generateOutletsTestRequestPickupPointsGeoRegionRestrictions)
	startTime := time.Date(2020, 06, 02, 12, 4, 5, 0, time.UTC)

	env, cancel := NewEnv(t, genData, nil)
	defer cancel()

	req := PreparePickupPointsGeoRequest(
		startTime,
		0,
		0,
		20,
		units.GpsCoords{
			Latitude:  -90.0,
			Longitude: -180.0,
		},
		units.GpsCoords{
			Latitude:  90.0,
			Longitude: 180.0,
		},
		[]uint32{0},
		[]uint32{20279},
		"")
	// В геобазе: market/combinator/pkg/geobase/geobase_example.go
	// 120542 -> 20279 -> 213
	// с ограничением 20279 должны отфильтроваться 20279 и 120542
	resp, err := env.Client.GetPickupPointsGeo(env.Ctx, req)
	require.NoError(t, err)
	require.Equal(t, 1, len(resp.Groups))
	ValidatePickupPointGeoGroup(t, resp.Groups[0], 3, uint64(3), 10.0, 10.0)

	req = PreparePickupPointsGeoRequest(
		startTime,
		0,
		0,
		20,
		units.GpsCoords{
			Latitude:  -90.0,
			Longitude: -180.0,
		},
		units.GpsCoords{
			Latitude:  90.0,
			Longitude: 180.0,
		},
		[]uint32{0},
		[]uint32{213},
		"")
	resp, err = env.Client.GetPickupPointsGeo(env.Ctx, req)
	require.NoError(t, err)
	require.Equal(t, 0, len(resp.Groups))

	req = PreparePickupPointsGeoRequest(
		startTime,
		0,
		0,
		20,
		units.GpsCoords{
			Latitude:  -90.0,
			Longitude: -180.0,
		},
		units.GpsCoords{
			Latitude:  90.0,
			Longitude: 180.0,
		},
		[]uint32{0},
		[]uint32{120542},
		"")
	resp, err = env.Client.GetPickupPointsGeo(env.Ctx, req)
	require.NoError(t, err)
	require.Equal(t, 1, len(resp.Groups))
	ValidatePickupPointGeoGroup(t, resp.Groups[0], 4, uint64(4), 10.0, 10.0)
}

func PreparePickupPointsGeoRequest(
	startTime time.Time,
	filters uint32,
	maxOutlets uint64,
	zoom uint32,
	leftBottomGps units.GpsCoords,
	rightTopGps units.GpsCoords,
	cargoTypes []uint32,
	unavailableRegions []uint32,
	rearrFactors string,
) *pb.PickupPointsGeoRequest {
	req := pb.PickupPointsGeoRequest{
		StartTime: ToProtoTimestamp(startTime),
		Parcels: []*pb.DeliveryRequestParcel{
			{
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
				TotalPrice: uint32(10000),
			},
		},
		Filters:    filters,
		MaxOutlets: maxOutlets,
		Zoom:       zoom,
		LeftBottom: &pb.GpsCoords{
			Lat: leftBottomGps.Latitude,
			Lon: leftBottomGps.Longitude,
		},
		RightTop: &pb.GpsCoords{
			Lat: rightTopGps.Latitude,
			Lon: rightTopGps.Longitude,
		},
		UnavailableRegions: unavailableRegions,
		RearrFactors:       rearrFactors,
	}
	return &req
}

func getEkbFirst() *tr.TariffRT {
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
			To:   geobase.RegionEkaterinburg,
		},
		Type: tr.RuleTypePayment,
	}
}

func getEkbSecond() *tr.TariffRT {
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
			10000971023,
		}),
		FromToRegions: tr.FromToRegions{
			From: 1,
			To:   geobase.RegionEkaterinburg,
		},
		Type: tr.RuleTypeForPoint,
	}
}

func generateGenerationData(genOutletsFunc GenOutletsFunc) *bg.GenerationData {
	graphEx := graph.NewPickupPointsGeoExample()
	regionMap := geobase.NewExample()
	tariffsFinder := tr.NewTariffsFinder()
	tariffsFinder.Add(getMskUnused())
	tariffsFinder.Add(makeGlobalTariff(getMskFirst()))
	tariffsFinder.Add(getMskFirst())
	tariffsFinder.Add(getMskSecond())
	tariffsFinder.Add(makeGlobalTariff(getSpbFirst()))
	tariffsFinder.Add(getSpbFirst())
	tariffsFinder.Add(getSpbSecond())
	tariffsFinder.Add(makeGlobalTariff(getEkbFirst()))
	tariffsFinder.Add(getEkbFirst())
	tariffsFinder.Add(getEkbSecond())
	tariffsFinder.Finish(&regionMap)
	genData := &bg.GenerationData{
		RegionMap:     regionMap,
		TariffsFinder: NewFinderSet(tariffsFinder),
		Graph:         graphEx.G,
	}
	genData.Outlets = outlets.Make(genOutletsFunc(), &genData.RegionMap, genData.TariffsFinder.Common.PointRegionMap)
	genData.OutletsAndTariffs = extradata.NewOutletsAndTariffs(genData.Outlets, genData.TariffsFinder.Common.PointRegionMap)
	genData.OutletsAndDSBS = extradata.NewOutletsAndDSBS(genData.Outlets, nil)
	return genData
}

func PreparePickupPointsGeoRequestDSBS(
	startTime time.Time,
	filters uint32,
	maxOutlets uint64,
	zoom uint32,
	leftBottomGps units.GpsCoords,
	rightTopGps units.GpsCoords,
	cargoTypes []uint32,
	unavailableRegions []uint32,
	rearrFactors string,
) *pb.PickupPointsGeoRequest {
	req := pb.PickupPointsGeoRequest{
		StartTime: ToProtoTimestamp(startTime),
		Parcels: []*pb.DeliveryRequestParcel{
			{
				Items: []*pb.DeliveryRequestItem{
					{
						RequiredCount: 1,
						Weight:        1000,
						Dimensions: []uint32{
							20,
							20,
							15,
						},
						AvailableOffers: []*pb.Offer{
							{
								ShopSku:        "322",
								ShopId:         322,
								PartnerId:      4321,
								AvailableCount: 3,
							},
						},
						Price: 1000,
					},
				},
				TotalPrice: uint32(1000),
			},
		},
		Filters:    filters,
		MaxOutlets: maxOutlets,
		Zoom:       zoom,
		LeftBottom: &pb.GpsCoords{
			Lat: leftBottomGps.Latitude,
			Lon: leftBottomGps.Longitude,
		},
		RightTop: &pb.GpsCoords{
			Lat: rightTopGps.Latitude,
			Lon: rightTopGps.Longitude,
		},
		UnavailableRegions: unavailableRegions,
		RearrFactors:       rearrFactors,
	}
	return &req
}

// TestRequestPickupPointsGeoDSBS - проверяем поиск для DSBS ПВЗ
func TestRequestPickupPointsGeoDSBS(t *testing.T) {
	genData := generateGenerationDataDBSB()
	startTime := time.Date(2020, 06, 02, 12, 4, 5, 0, time.UTC)

	env, cancel := NewEnv(t, genData, nil)
	defer cancel()

	req := PreparePickupPointsGeoRequestDSBS(
		startTime,
		0,
		0,
		20,
		units.GpsCoords{
			Latitude:  -90.0,
			Longitude: -180.0,
		},
		units.GpsCoords{
			Latitude:  90.0,
			Longitude: 180.0,
		},
		[]uint32{},
		[]uint32{},
		"")
	resp, err := env.Client.GetPickupPointsGeo(env.Ctx, req)
	require.NoError(t, err)
	require.Equal(t, 1, len(resp.Groups))

	require.Equal(t, 1, len(resp.Statistic))
	ValidatePickupPointGeoGroup(t, resp.Groups[0], 1, uint64(1), 40.0, 30.0)
	require.Equal(t, 1234567, int(resp.Groups[0].Outlets[0].LogisticPointId))

	ValidatePickupPointGeoStatistic(t, resp.Statistic[0], uint32(pb.OutletFilters_PICKUP|pb.OutletFilters_EVERY_DAY), uint32(1))
}

func generateTestRequestPickupPointsGeoDSBS(regionMap *geobase.RegionMap) *outlets.OutletStorage {
	return outlets.Make([]outlets.Outlet{
		outlets.Outlet{
			ID:                        102030,
			Type:                      outlets.Depot,
			PostCode:                  123321,
			RegionID:                  geobase.RegionMytishchi,
			DeliveryServiceOutletCode: "test102030",
		},
		outlets.Outlet{
			ID:                        102040,
			Type:                      outlets.Depot,
			PostCode:                  123321,
			RegionID:                  geobase.RegionMytishchi,
			DeliveryServiceOutletCode: "test102040",
		},
		outlets.Outlet{
			ID:                        203040,
			Type:                      outlets.Depot,
			PostCode:                  123321,
			RegionID:                  geobase.RegionMytishchi,
			DeliveryServiceOutletCode: "test203040",
		},
		outlets.Outlet{
			ID:                        304050,
			Type:                      outlets.Depot,
			PostCode:                  123321,
			RegionID:                  geobase.RegionMytishchi,
			DeliveryServiceOutletCode: "1234567",
			DsbsPointID:               1234567,
			IsActive:                  true,
			GpsCoords: units.GpsCoords{
				Latitude:  40.0,
				Longitude: 30.0,
			},
		},
		outlets.Outlet{
			ID:                        404050,
			Type:                      outlets.Depot,
			PostCode:                  123321,
			RegionID:                  geobase.RegionMoscow,
			DeliveryServiceOutletCode: "test404050",
			IsMarketBranded:           true,
			IsActive:                  true,
			GpsCoords: units.GpsCoords{
				Latitude:  55.705468,
				Longitude: 37.669585,
			},
		},
	}, regionMap, nil)
}

func generateGenerationDataDBSB() *bg.GenerationData {
	gb := graph.NewGraphBuilder()
	graphExampleDSBS := graph.BuildExamplePickupDSBS(gb)
	tariffsBuilder := tr.NewTariffsBuilder()
	_ = tariffsBuilder.MakeTariff(
		tr.TariffWithPartner(uint64(graphExampleDSBS.Linehaul.PartnerLmsID)),
		tr.TariffWithRegion(geobase.RegionMoscowAndObl, geobase.RegionMoscow),
		tr.TariffWithDays(uint32(1), uint32(2)),
	)
	regionMap := geobase.NewExample()
	genData := bg.GenerationData{
		RegionMap:              regionMap,
		Graph:                  graphExampleDSBS.Graph,
		DsbsCourierTariffs:     getDsbsCourierTariffs(),
		DsbsPickupPointTariffs: getDsbsPickupPointTariffs(),
		DsbsToOutletShops: dsbs.ShopSet{
			322: struct{}{},
			505: struct{}{},
		},
		Outlets:       generateTestRequestPickupPointsGeoDSBS(&regionMap),
		TariffsFinder: tariffsBuilder.TariffsFinder,
	}
	genData.OutletsAndTariffs = extradata.NewOutletsAndTariffs(genData.Outlets, genData.TariffsFinder.Common.PointRegionMap)
	genData.OutletsAndDSBS = extradata.NewOutletsAndDSBS(genData.Outlets, genData.DsbsPickupPointTariffs)
	return &genData
}
