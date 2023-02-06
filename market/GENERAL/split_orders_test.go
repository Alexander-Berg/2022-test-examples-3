package lite

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	bg "a.yandex-team.ru/market/combinator/pkg/biggeneration"
	"a.yandex-team.ru/market/combinator/pkg/binpack"
	"a.yandex-team.ru/market/combinator/pkg/enums"
	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/graph"
	partdel "a.yandex-team.ru/market/combinator/pkg/partial_delivery"
	tr "a.yandex-team.ru/market/combinator/pkg/tarifficator"
	pb "a.yandex-team.ru/market/combinator/proto/grpc"
)

func TestSplitOrders(t *testing.T) {
	graphEx := graph.NewCourierAlternativeExample()
	globalRuleYa := newShopTariff(&tariffSettings{
		deliveryMethod:    enums.DeliveryMethodCourier,
		deliveryServiceID: 106,
		regionTo:          1,
		ruleType:          tr.RuleTypeGlobal,
		daysMin:           4,
	})
	globalRuleYa.WeightMax = 10000
	globalRuleYa.IsMarketCourier = true
	shopTariffYa := tr.TariffRT{
		ID:                globalRuleYa.ID,
		DeliveryServiceID: 106,
		DeliveryMethod:    enums.DeliveryMethodCourier,
		ProgramTypeList:   tr.ProgramTypeList(tr.ProgramMarketDelivery),
		IsMarketCourier:   true,
		Option: tr.Option{
			Cost:    11,
			DaysMin: 4,
			DaysMax: 5,
		},
		RuleAttrs: tr.RuleAttrs{
			WeightMax: 10000,
		},
		FromToRegions: tr.FromToRegions{
			From: 1,
			To:   geobase.RegionHamovniki,
		},
		Type: tr.RuleTypePayment,
	}
	// Не наша служба доставки
	globalRule := newShopTariff(&tariffSettings{
		deliveryMethod:    enums.DeliveryMethodCourier,
		deliveryServiceID: 139,
		regionTo:          1,
		ruleType:          tr.RuleTypeGlobal,
	})
	shopTariff1 := tr.TariffRT{
		ID:                globalRule.ID,
		DeliveryServiceID: 139,
		DeliveryMethod:    enums.DeliveryMethodCourier,
		ProgramTypeList:   tr.ProgramTypeList(tr.ProgramMarketDelivery),
		IsMarketCourier:   false,
		Option: tr.Option{
			Cost:    11,
			DaysMin: 3,
			DaysMax: 5,
		},
		RuleAttrs: tr.RuleAttrs{
			WeightMax: 19000, // 1gk less than global
		},
		FromToRegions: tr.FromToRegions{
			From: 1,
			To:   geobase.RegionHamovniki,
		},
		Type: tr.RuleTypePayment,
	}
	// Не наша служба доставки
	shopTariff2 := tr.TariffRT{
		ID:                globalRule.ID,
		DeliveryServiceID: 139,
		DeliveryMethod:    enums.DeliveryMethodCourier,
		ProgramTypeList:   tr.ProgramTypeList(tr.ProgramMarketDelivery),
		IsMarketCourier:   false,
		Option: tr.Option{
			Cost:    11,
			DaysMin: 3,
			DaysMax: 5,
		},
		RuleAttrs: tr.RuleAttrs{
			WeightMax: 5000, // even less
		},
		FromToRegions: tr.FromToRegions{
			From: 1,
			To:   geobase.RegionHamovniki,
		},
		Type: tr.RuleTypePayment,
	}
	tariffsFinder := tr.NewTariffsFinder()
	tariffsFinder.Add(&globalRuleYa)
	tariffsFinder.Add(&shopTariffYa)
	tariffsFinder.Add(&globalRule)
	tariffsFinder.Add(&shopTariff1)
	tariffsFinder.Add(&shopTariff2)

	genData := bg.GenerationData{
		RegionMap:     geobase.NewExample(),
		TariffsFinder: NewFinderSet(tariffsFinder),
		Graph:         graphEx.G,
	}
	startTime := time.Date(2021, 8, 10, 12, 4, 5, 0, time.UTC)
	dest := pb.PointIds{
		RegionId: 213,
	}
	reqSettings := newRequestSettings(requestSettings{
		startTime: startTime,
		dest:      &dest,
		dType:     pb.DeliveryType_COURIER,
	})
	env, cancel := NewEnv(t, &genData, nil)
	defer cancel()
	{
		// Too few dimensions
		req := PrepareSplitRequest(reqSettings)
		req.Orders[0].Items[0].Dimensions = req.Orders[0].Items[0].Dimensions[:1]
		_, err := env.Client.SplitOrders(env.Ctx, req)
		require.Error(t, err)
	}
	{
		// No split; ignore our own delivery service
		req := PrepareSplitRequest(reqSettings)
		req.Orders[0].Items[1].Weight = 3000
		resp, err := env.Client.SplitOrders(env.Ctx, req)
		require.NoError(t, err)
		require.Len(t, req.Orders, 1)
		require.Equal(t, req.Orders[0].OrderId, resp.Orders[0].OrderId)
		require.Equal(t, pb.SplitStatus_NOTHING, resp.Orders[0].Status)
		require.Empty(t, resp.Orders[0].UnreachableItems)
		require.Len(t, resp.Orders[0].Baskets, 1)
		for i := range req.Orders[0].Items {
			require.Equal(t, req.Orders[0].Items[i].Weight, resp.Orders[0].Baskets[0].Items[i].Weight)
			require.Equal(t, req.Orders[0].Items[i].RequiredCount, resp.Orders[0].Baskets[0].Items[i].RequiredCount)
			// Измерения не проверяем, они сортируются
			//require.Equal(t, req.Orders[0].Items[i].Dimensions, resp.Orders[0].Baskets[0].Items[i].Dimensions)
			require.Equal(t, req.Orders[0].Items[i].AvailableOffers[0].ShopId, resp.Orders[0].Baskets[0].Items[i].AvailableOffers[0].ShopId)
			require.Equal(t, req.Orders[0].Items[i].AvailableOffers[0].ShopSku, resp.Orders[0].Baskets[0].Items[i].AvailableOffers[0].ShopSku)
		}
	}
	{
		// Split for our delivery service (10kg max) only when 100% need split
		req := PrepareSplitRequest(reqSettings)
		req.Orders[0].Items[1].Weight = 5000
		resp, err := env.Client.SplitOrders(env.Ctx, req)
		require.NoError(t, err)
		require.Len(t, req.Orders, 1)
		require.Equal(t, req.Orders[0].OrderId, resp.Orders[0].OrderId)
		require.Equal(t, pb.SplitStatus_SPLIT_OK, resp.Orders[0].Status)
		require.Empty(t, resp.Orders[0].UnreachableItems)
		require.Len(t, resp.Orders[0].Baskets, 3)
		// 2+5
		require.Len(t, resp.Orders[0].Baskets[0].Items, 2)
		require.Equal(t, 2000, int(resp.Orders[0].Baskets[0].Items[0].Weight))
		require.Equal(t, 1, int(resp.Orders[0].Baskets[0].Items[0].RequiredCount))
		require.Equal(t, 5000, int(resp.Orders[0].Baskets[0].Items[1].Weight))
		require.Equal(t, 1, int(resp.Orders[0].Baskets[0].Items[1].RequiredCount))
		// 5+5
		require.Len(t, resp.Orders[0].Baskets[1].Items, 1)
		require.Equal(t, 5000, int(resp.Orders[0].Baskets[1].Items[0].Weight))
		require.Equal(t, 2, int(resp.Orders[0].Baskets[1].Items[0].RequiredCount))
		// 5
		require.Len(t, resp.Orders[0].Baskets[2].Items, 1)
		require.Equal(t, 5000, int(resp.Orders[0].Baskets[2].Items[0].Weight))
		require.Equal(t, 1, int(resp.Orders[0].Baskets[2].Items[0].RequiredCount))
	}
	{
		// Split for other delivery service (19kg max)
		req := PrepareSplitRequest(reqSettings)
		req.Orders[0].Items[0].Weight = 15000
		resp, err := env.Client.SplitOrders(env.Ctx, req)
		require.NoError(t, err)
		require.Len(t, req.Orders, 1)
		require.Equal(t, req.Orders[0].OrderId, resp.Orders[0].OrderId)
		require.Equal(t, pb.SplitStatus_SPLIT_OK, resp.Orders[0].Status)
		require.Empty(t, resp.Orders[0].UnreachableItems)
		require.Len(t, resp.Orders[0].Baskets, 2)
		// 2+2+2+2
		require.Len(t, resp.Orders[0].Baskets[0].Items, 1)
		require.Equal(t, 2000, int(resp.Orders[0].Baskets[0].Items[0].Weight))
		require.Equal(t, 4, int(resp.Orders[0].Baskets[0].Items[0].RequiredCount))
		// 15
		require.Len(t, resp.Orders[0].Baskets[1].Items, 1)
		require.Equal(t, 15000, int(resp.Orders[0].Baskets[1].Items[0].Weight))
		require.Equal(t, 1, int(resp.Orders[0].Baskets[1].Items[0].RequiredCount))
	}
	{
		// Split for our delivery service (10kg max) with unreachable item
		req := PrepareSplitRequest(reqSettings)
		req.Orders[0].Items[0].Weight = 20000 // shouldn't fit into any tariff
		req.Orders[0].Items[1].Weight = 5000
		req.Orders[0].Items[1].RequiredCount = 6
		resp, err := env.Client.SplitOrders(env.Ctx, req)
		require.NoError(t, err)
		require.Len(t, req.Orders, 1)
		require.Equal(t, pb.SplitStatus_PART_SPLIT, resp.Orders[0].Status)
		require.Equal(t, req.Orders[0].OrderId, resp.Orders[0].OrderId)
		// unreachable item of 20kg: payment rule fits only 19kg!
		require.Len(t, resp.Orders[0].UnreachableItems, 1)
		require.Equal(t, 20000, int(resp.Orders[0].UnreachableItems[0].Weight))
		require.Equal(t, 1, int(resp.Orders[0].UnreachableItems[0].RequiredCount))
		// 3 baskets with 5kg+5kg items
		require.Len(t, resp.Orders[0].Baskets, 3)
		for i := range resp.Orders[0].Baskets {
			// 5+5
			require.Len(t, resp.Orders[0].Baskets[i].Items, 1)
			require.Equal(t, 5000, int(resp.Orders[0].Baskets[i].Items[0].Weight))
			require.Equal(t, 2, int(resp.Orders[0].Baskets[i].Items[0].RequiredCount))
		}
	}

	partdel.Warehouses[145] = true
	{
		// separate the 600 cargotype (fashion) COMBINATOR-1843
		req := PrepareSplitRequest(reqSettings)
		req.RearrFactors = "fashion_partial_msk_only=1;use_stats_partial_available_for_split=0"
		req.Orders[0].Items[1].Weight = 3000
		req.Orders[0].Items[1].CargoTypes = []uint32{600}
		resp, err := env.Client.SplitOrders(env.Ctx, req)
		require.NoError(t, err)
		require.Len(t, resp.Orders, 1)
		require.Equal(t, req.Orders[0].OrderId, resp.Orders[0].OrderId)
		require.Equal(t, pb.SplitStatus_SPLIT_OK, resp.Orders[0].Status)
		require.Empty(t, resp.Orders[0].UnreachableItems)
		require.Len(t, resp.Orders[0].Baskets, 2)
		require.Equal(t, req.Orders[0].Items[0].Weight, resp.Orders[0].Baskets[0].Items[0].Weight)
		require.Equal(t, req.Orders[0].Items[0].RequiredCount, resp.Orders[0].Baskets[0].Items[0].RequiredCount)

		require.Equal(t, req.Orders[0].Items[1].Weight, resp.Orders[0].Baskets[1].Items[0].Weight)
		require.Equal(t, req.Orders[0].Items[1].RequiredCount, resp.Orders[0].Baskets[1].Items[0].RequiredCount)
		require.True(t, resp.Orders[0].Baskets[1].PartialDeliveryAvailable)
	}
	{
		// separate the 80 cargotype (jewelry) COMBINATOR-3835
		req := PrepareSplitRequest(reqSettings)
		req.RearrFactors = "use_stats_partial_available_for_split=0"
		req.Orders[0].Items[1].Weight = 3000
		req.Orders[0].Items[1].CargoTypes = []uint32{80}
		resp, err := env.Client.SplitOrders(env.Ctx, req)
		require.NoError(t, err)
		require.Len(t, resp.Orders, 1)
		require.Equal(t, req.Orders[0].OrderId, resp.Orders[0].OrderId)
		require.Equal(t, pb.SplitStatus_SPLIT_OK, resp.Orders[0].Status)
		require.Empty(t, resp.Orders[0].UnreachableItems)
		require.Len(t, resp.Orders[0].Baskets, 2)
		require.Equal(t, req.Orders[0].Items[0].Weight, resp.Orders[0].Baskets[0].Items[0].Weight)
		require.Equal(t, req.Orders[0].Items[0].RequiredCount, resp.Orders[0].Baskets[0].Items[0].RequiredCount)

		require.Equal(t, req.Orders[0].Items[1].Weight, resp.Orders[0].Baskets[1].Items[0].Weight)
		require.Equal(t, req.Orders[0].Items[1].RequiredCount, resp.Orders[0].Baskets[1].Items[0].RequiredCount)
	}
	{
		// separate the 600 AND 80 cargotypes
		req := PrepareSplitRequest(reqSettings)
		req.RearrFactors = "fashion_partial_msk_only=1;use_stats_partial_available_for_split=0"
		req.Orders[0].Items[0].CargoTypes = []uint32{80}
		req.Orders[0].Items[1].CargoTypes = []uint32{600}
		resp, err := env.Client.SplitOrders(env.Ctx, req)
		require.NoError(t, err)
		require.Len(t, resp.Orders, 1)
		require.Equal(t, req.Orders[0].OrderId, resp.Orders[0].OrderId)
		require.Equal(t, pb.SplitStatus_SPLIT_OK, resp.Orders[0].Status)
		require.Empty(t, resp.Orders[0].UnreachableItems)
		require.Len(t, resp.Orders[0].Baskets, 2)
		require.Equal(t, req.Orders[0].Items[0].Weight, resp.Orders[0].Baskets[0].Items[0].Weight)
		require.Equal(t, req.Orders[0].Items[0].RequiredCount, resp.Orders[0].Baskets[0].Items[0].RequiredCount)

		require.Equal(t, req.Orders[0].Items[1].Weight, resp.Orders[0].Baskets[1].Items[0].Weight)
		require.Equal(t, req.Orders[0].Items[1].RequiredCount, resp.Orders[0].Baskets[1].Items[0].RequiredCount)
		require.True(t, resp.Orders[0].Baskets[1].PartialDeliveryAvailable)
	}
	{
		// separate the 600 AND 80 cargotypes with regular items
		req := PrepareSplitRequestWithNumItems(reqSettings, 3)
		req.RearrFactors = "fashion_partial_msk_only=1;use_stats_partial_available_for_split=0"
		req.Orders[0].Items[1].CargoTypes = []uint32{80}
		req.Orders[0].Items[2].CargoTypes = []uint32{600}
		resp, err := env.Client.SplitOrders(env.Ctx, req)
		require.NoError(t, err)
		require.Len(t, resp.Orders, 1)
		require.Equal(t, req.Orders[0].OrderId, resp.Orders[0].OrderId)
		require.Equal(t, pb.SplitStatus_SPLIT_OK, resp.Orders[0].Status)
		require.Empty(t, resp.Orders[0].UnreachableItems)
		require.Len(t, resp.Orders[0].Baskets, 3)
		require.Equal(t, req.Orders[0].Items[0].Weight, resp.Orders[0].Baskets[0].Items[0].Weight)
		require.Equal(t, req.Orders[0].Items[0].RequiredCount, resp.Orders[0].Baskets[0].Items[0].RequiredCount)

		require.Equal(t, req.Orders[0].Items[1].Weight, resp.Orders[0].Baskets[1].Items[0].Weight)
		require.Equal(t, req.Orders[0].Items[1].RequiredCount, resp.Orders[0].Baskets[1].Items[0].RequiredCount)

		require.Equal(t, req.Orders[0].Items[2].Weight, resp.Orders[0].Baskets[2].Items[0].Weight)
		require.Equal(t, req.Orders[0].Items[2].RequiredCount, resp.Orders[0].Baskets[2].Items[0].RequiredCount)
		require.True(t, resp.Orders[0].Baskets[2].PartialDeliveryAvailable)
		req.Orders[0].Items[0].CargoTypes = nil
	}
	{
		// COMBINATOR-2216 separate fashion no more than 10 items in basket
		req := PrepareSplitRequest(reqSettings)
		req.RearrFactors = "use_stats_partial_available_for_split=0"
		req.Orders[0].Items[1].Weight = 500
		req.Orders[0].Items[1].RequiredCount = 15
		req.Orders[0].Items[1].AvailableOffers[0].AvailableCount = 15
		req.Orders[0].Items[1].CargoTypes = []uint32{600}
		resp, err := env.Client.SplitOrders(env.Ctx, req)
		require.NoError(t, err)
		require.Len(t, resp.Orders, 1)
		require.Equal(t, req.Orders[0].OrderId, resp.Orders[0].OrderId)
		require.Equal(t, pb.SplitStatus_SPLIT_OK, resp.Orders[0].Status)
		require.Empty(t, resp.Orders[0].UnreachableItems)
		require.Len(t, resp.Orders[0].Baskets, 3)
		require.Equal(t, req.Orders[0].Items[0].Weight, resp.Orders[0].Baskets[0].Items[0].Weight)
		require.Equal(t, req.Orders[0].Items[0].RequiredCount, resp.Orders[0].Baskets[0].Items[0].RequiredCount)

		require.Equal(t, req.Orders[0].Items[1].Weight, resp.Orders[0].Baskets[1].Items[0].Weight)
		require.Equal(t, binpack.MaxFashionItemsInBasket, int(resp.Orders[0].Baskets[1].Items[0].RequiredCount))
		require.True(t, resp.Orders[0].Baskets[1].PartialDeliveryAvailable)

		require.Equal(t, req.Orders[0].Items[1].Weight, resp.Orders[0].Baskets[2].Items[0].Weight)
		require.Equal(t, req.Orders[0].Items[1].RequiredCount-binpack.MaxFashionItemsInBasket, resp.Orders[0].Baskets[2].Items[0].RequiredCount)
		require.True(t, resp.Orders[0].Baskets[2].PartialDeliveryAvailable)
	}
	{
		// No fashion separation for regions not in the list if fashion_partial_msk_and_spb=1
		req := PrepareSplitRequest(reqSettings)
		req.RearrFactors = "fashion_partial_msk_and_spb=1"
		tempMsk := partdel.Regions[0]
		partdel.Regions[0] = 666 // assuming first element to be Moscow
		req.Orders[0].Items[1].Weight = 3000
		req.Orders[0].Items[1].CargoTypes = []uint32{600}
		resp, err := env.Client.SplitOrders(env.Ctx, req)
		require.NoError(t, err)
		require.Len(t, req.Orders, 1)
		require.Equal(t, req.Orders[0].OrderId, resp.Orders[0].OrderId)
		require.Equal(t, pb.SplitStatus_NOTHING, resp.Orders[0].Status)
		require.Empty(t, resp.Orders[0].UnreachableItems)
		require.Len(t, resp.Orders[0].Baskets, 1)
		for i := range req.Orders[0].Items {
			require.Equal(t, req.Orders[0].Items[i].Weight, resp.Orders[0].Baskets[0].Items[i].Weight)
			require.Equal(t, req.Orders[0].Items[i].RequiredCount, resp.Orders[0].Baskets[0].Items[i].RequiredCount)
			require.Equal(t, req.Orders[0].Items[i].AvailableOffers[0].ShopId, resp.Orders[0].Baskets[0].Items[i].AvailableOffers[0].ShopId)
			require.Equal(t, req.Orders[0].Items[i].AvailableOffers[0].ShopSku, resp.Orders[0].Baskets[0].Items[i].AvailableOffers[0].ShopSku)
		}
		partdel.Regions[0] = tempMsk // assuming to be Moscow
	}
	partdel.Warehouses[145] = false
	{
		// Add PARTIAL_RETURN service to warehouse segment => fashion should be detectable
		req := PrepareSplitRequest(reqSettings)
		req.Orders[0].Items[1].Weight = 3000
		req.Orders[0].Items[1].CargoTypes = []uint32{600}

		req.RearrFactors = "combinator_read_partial_return_from_graph=1;use_stats_partial_available_for_split=0;switch_to_disable_partial_return_service=0"
		schedule, _ := graph.CreateAroundTheClockSchedule(false)
		graphEx.Warehouse.CourierServices = append(graphEx.Warehouse.CourierServices, &graph.LogisticService{
			ID:       11666,
			IsActive: true,
			Type:     graph.ServiceTypeInternal,
			Code:     enums.ServicePartialReturn,
			Schedule: schedule,
			Price:    11,
		})
		graphEx.G.Finish(context.Background())

		resp, err := env.Client.SplitOrders(env.Ctx, req)
		require.NoError(t, err)
		require.Len(t, resp.Orders, 1)
		require.Equal(t, req.Orders[0].OrderId, resp.Orders[0].OrderId)
		require.Equal(t, pb.SplitStatus_SPLIT_OK, resp.Orders[0].Status)
		require.Empty(t, resp.Orders[0].UnreachableItems)
		require.Len(t, resp.Orders[0].Baskets, 2)
		require.Equal(t, req.Orders[0].Items[0].Weight, resp.Orders[0].Baskets[0].Items[0].Weight)
		require.Equal(t, req.Orders[0].Items[0].RequiredCount, resp.Orders[0].Baskets[0].Items[0].RequiredCount)

		require.Equal(t, req.Orders[0].Items[1].Weight, resp.Orders[0].Baskets[1].Items[0].Weight)
		require.Equal(t, req.Orders[0].Items[1].RequiredCount, resp.Orders[0].Baskets[1].Items[0].RequiredCount)
		require.True(t, resp.Orders[0].Baskets[1].PartialDeliveryAvailable)
	}
}

func PrepareSplitRequestWithNumItems(s *requestSettings, numItems int) *pb.SplitRequest {
	req := PrepareSplitRequest(s)
	if numItems < 2 {
		req.Orders[0].Items = req.Orders[0].Items[:numItems]
	}
	if numItems > 2 {
		for i := 2; i < numItems; i++ {
			req.Orders[0].Items = append(req.Orders[0].Items,
				&pb.DeliveryRequestItem{
					RequiredCount: 1,
					Weight:        2000,
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
					CargoTypes: s.cargoTypes,
				},
			)
		}
	}
	return req
}

func PrepareSplitRequest(s *requestSettings) *pb.SplitRequest {
	req := pb.SplitRequest{
		StartTime: ToProtoTimestamp(s.startTime),
		Orders: []*pb.SplitOrderRequest{{
			Items: []*pb.DeliveryRequestItem{
				{
					RequiredCount: 1,
					Weight:        2000,
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
					CargoTypes: s.cargoTypes,
				},
				{
					RequiredCount: 4,
					Weight:        2000,
					Dimensions: []uint32{
						15,
						18,
						20,
					},
					AvailableOffers: []*pb.Offer{
						{
							ShopSku:        "432",
							ShopId:         3,
							PartnerId:      145,
							AvailableCount: 10,
						},
					},
					CargoTypes: s.cargoTypes,
				},
			},
			OrderId: 111,
		}},
		Destination:  s.dest,
		UserInfo:     nil,
		RearrFactors: "",
	}
	return &req
}
