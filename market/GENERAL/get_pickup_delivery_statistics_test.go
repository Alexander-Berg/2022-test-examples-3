package lite

import (
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	bg "a.yandex-team.ru/market/combinator/pkg/biggeneration"
	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/graph"
	"a.yandex-team.ru/market/combinator/pkg/outlets"
	partdel "a.yandex-team.ru/market/combinator/pkg/partial_delivery"
	tr "a.yandex-team.ru/market/combinator/pkg/tarifficator"
	pb "a.yandex-team.ru/market/combinator/proto/grpc"
)

func TestGetPickupDeliveryStatistics(t *testing.T) {
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

	resp, err := env.Client.GetPickupDeliveryStatistics(env.Ctx, req)

	require.NoError(t, err)
	require.Len(t, resp.DeliveryMethods, 2)
	require.Equal(t, pb.DeliveryMethod_DM_PICKUP, resp.DeliveryMethods[0])
	require.Equal(t, pb.DeliveryMethod_DM_POST, resp.DeliveryMethods[1])

	// No data for this region
	req = PreparePickupRequestGrouped(startTime, []uint32{322}, []uint32{0}, nil)

	resp, err = env.Client.GetPickupDeliveryStatistics(env.Ctx, req)

	require.NoError(t, err)
	require.Len(t, resp.DeliveryMethods, 0)

	// Unknown region, but good post_code for Moscow
	req = PreparePickupRequestGrouped(startTime, nil, []uint32{1, 2, 3, 4}, []uint32{123321}) // Пустой список регионов.

	env, cancel = NewEnv(t, &genData, nil)
	defer cancel()

	resp, err = env.Client.GetPickupDeliveryStatistics(env.Ctx, req)

	require.NoError(t, err)
	require.Len(t, resp.DeliveryMethods, 1)
	require.Equal(t, pb.DeliveryMethod_DM_POST, resp.DeliveryMethods[0])

	// Bad cargo type 323
	req = PreparePickupRequestGrouped(startTime, []uint32{213, 2}, []uint32{111, 321, 323, 400}, nil)

	resp, err = env.Client.GetPickupDeliveryStatistics(env.Ctx, req)

	require.NoError(t, err)
	require.Equal(t, 0, len(resp.DeliveryMethods))

	// Bad cargo type 322322 in pickup segment
	req = PreparePickupRequestGrouped(startTime, []uint32{213, 2}, []uint32{111, 321, 323, 400}, nil)

	resp, err = env.Client.GetPickupDeliveryStatistics(env.Ctx, req)

	require.NoError(t, err)
	require.Len(t, resp.DeliveryMethods, 0)
}

func TestHideDarkStroresInGetPickupDeliveryStatistics(t *testing.T) {
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
	resp, err := env.Client.GetPickupDeliveryStatistics(env.Ctx, req)

	require.NoError(t, err)
	require.Len(t, resp.DeliveryMethods, 1)
	require.Equal(t, pb.DeliveryMethod_DM_PICKUP, resp.DeliveryMethods[0])

	// Ожидаем, что успешно нашли "ничего"
	req = PreparePickupRequestGrouped(startTime, []uint32{322}, []uint32{0}, nil)
	resp, err = env.Client.GetPickupDeliveryStatistics(env.Ctx, req)

	require.NoError(t, err)
	require.Len(t, resp.DeliveryMethods, 0)
}

func TestFashionGetPickupDeliveryStatistics(t *testing.T) {
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
				Type:     outlets.Post,
				PostCode: 123321,
				RegionID: geobase.RegionID(213),
			},
			outlets.Outlet{
				ID:       10000971018,
				Type:     outlets.Depot,
				PostCode: outlets.UnknownPostCode,
			},
			outlets.Outlet{
				ID:              10000971021,
				Type:            outlets.Depot,
				PostCode:        outlets.UnknownPostCode,
				IsMarketBranded: true,
			},
			outlets.Outlet{
				ID:              10000971022,
				Type:            outlets.PostTerm,
				PostCode:        outlets.UnknownPostCode,
				IsMarketBranded: true,
			},
			outlets.Outlet{
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
	resp, err := env.Client.GetPickupDeliveryStatistics(env.Ctx, req)

	require.NoError(t, err)
	require.Len(t, resp.DeliveryMethods, 1)
	require.Equal(t, pb.DeliveryMethod_DM_PICKUP, resp.DeliveryMethods[0])

	// Disable fashion options if the flag is set
	req.DisablePartialDelivery = true
	resp, err = env.Client.GetPickupDeliveryStatistics(env.Ctx, req)
	require.NoError(t, err)
	require.Len(t, resp.DeliveryMethods, 2)

	partdel.Warehouses[145] = false
}
