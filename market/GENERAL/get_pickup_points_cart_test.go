package lite

import (
	"fmt"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	bg "a.yandex-team.ru/market/combinator/pkg/biggeneration"
	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/graph"
	"a.yandex-team.ru/market/combinator/pkg/outlets"
	tr "a.yandex-team.ru/market/combinator/pkg/tarifficator"
	pb "a.yandex-team.ru/market/combinator/proto/grpc"
)

func TestHideDarkStroresInGetPickupPointsCart(t *testing.T) {
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
			outlets.Outlet{
				ID:          10000971021,
				Type:        outlets.Post,
				PostCode:    100,
				IsDarkStore: false,
			},
		}, &regionMap, nil),
		Graph: graphEx.G,
	}

	startTime := time.Date(2020, 06, 02, 12, 4, 5, 0, time.UTC)

	env, cancel := NewEnv(t, &genData, nil)
	defer cancel()

	// Ожидаем, что скрыли Дарк сторы
	req := PreparePickupRequestCart(startTime, []uint64{10000971018, 10000971019, 10000971020, 10000971021}, []uint32{1, 2, 3, 4}, nil)
	resp, err := env.Client.GetPickupPointsCart(env.Ctx, req)

	require.NoError(t, err)
	require.Equal(t, 2, len(resp.Groups))
	require.Equal(t, 1, len(resp.Groups[0].Points))
	require.Equal(t, uint64(10000971021), resp.Groups[0].Points[0].LogisticPointId)
	require.Equal(t, 1, len(resp.Groups[1].Points))
	require.Equal(t, uint64(10000971019), resp.Groups[1].Points[0].LogisticPointId)

	// Ожидаем, что успешно нашли "ничего"
	req = PreparePickupRequestCart(startTime, []uint64{10000971020}, []uint32{0}, nil)
	resp, err = env.Client.GetPickupPointsCart(env.Ctx, req)
	fmt.Println(resp)

	require.NoError(t, err)
	require.Equal(t, 0, len(resp.Groups))
}

func PreparePickupRequestCart(
	startTime time.Time,
	lmsIDs []uint64,
	cargoTypes []uint32,
	postCodes []uint32,
) *pb.PickupPointsCartRequest {
	req := pb.PickupPointsCartRequest{
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
		LogisticPointIds: lmsIDs,
		PostCodes:        postCodes,
	}
	return &req
}
