package binpack

import (
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/tarifficator"
	cr "a.yandex-team.ru/market/combinator/proto/grpc"
)

func makePaymentRule(weight uint32) *tarifficator.TariffRT {
	tariff := new(tarifficator.TariffRT)
	tariff.WeightMax = float64(weight)
	return tariff
}

func makeGlobalRule(weight uint32) *tarifficator.TariffRT {
	tariff := new(tarifficator.TariffRT)
	tariff.WeightMax = float64(weight)
	tariff.DimSumMax = 180
	tariff.M3weight = 65
	tariff.SortedDimLimits = tarifficator.CreateSortedDims(60, 50, 80)
	return tariff
}

func makeConstraint(paymentWeight, globalWeight uint32) *SplitConstraints {
	sc := &SplitConstraints{
		MaxPaymentRule:  makePaymentRule(paymentWeight),
		GlobalRule:      makeGlobalRule(globalWeight),
		IsMarketCourier: false,
		MaxPrice:        0,
	}
	return sc
}

func makeConstraints(paymentWeight, globalWeight, paymentWeightMK, globalWeightMK uint32) []*SplitConstraints {
	scs := []*SplitConstraints{
		makeConstraint(paymentWeight, globalWeight),
		makeConstraint(paymentWeightMK, globalWeightMK),
	}
	scs[1].IsMarketCourier = true
	return scs
}

func TestSplitOrders(t *testing.T) {
	{
		// Several items, no split
		// Ignoring our own delivery service if it's already deliverable
		req := PrepareSplitRequest()
		resp, err := SplitOrder(req, makeConstraints(200000, 20000, 10000, 20000))
		require.NoError(t, err)
		require.Equal(t, req.OrderId, resp.OrderId)
		require.Empty(t, resp.UnreachableItems)
		require.Equal(t, req.Items, resp.Baskets[0].Items)
		require.Equal(t, cr.SplitStatus_NOTHING, resp.Status)
	}
	{
		// Several items, one is unreachable by weight
		req := PrepareSplitRequest()
		req.Items[0].Weight = 300000
		unreachableItem := req.Items[0]
		reachableItem := req.Items[1]
		resp, err := SplitOrder(req, makeConstraints(100000, 200000, 200000, 50000))
		require.NoError(t, err)
		require.Equal(t, req.OrderId, resp.OrderId)
		require.Len(t, resp.UnreachableItems, 1)
		require.Equal(t, unreachableItem, resp.UnreachableItems[0])
		require.Equal(t, cr.ItemIssue_TOO_HEAVY, resp.UnreachableItems[0].ItemIssues[0])
		require.Len(t, resp.Baskets, 1)
		require.Len(t, resp.Baskets[0].Items, 1)
		require.Equal(t, reachableItem, resp.Baskets[0].Items[0])
		require.Equal(t, cr.SplitStatus_PART_SPLIT, resp.Status)
	}
	{
		// Unreachable by length and dim sum
		req := PrepareSplitRequest()
		req.Items[1].Dimensions = []uint32{200, 10, 10}
		reachableItem := req.Items[0]
		unreachableItem := req.Items[1]
		resp, err := SplitOrder(req, makeConstraints(200000, 200000, 50000, 10000))
		require.NoError(t, err)
		require.Equal(t, req.OrderId, resp.OrderId)
		require.Len(t, resp.UnreachableItems, 1)
		require.Equal(t, unreachableItem, resp.UnreachableItems[0])
		require.Equal(t, cr.ItemIssue_EXCEEDS_DIM_SUM, resp.UnreachableItems[0].ItemIssues[0])
		require.Equal(t, cr.ItemIssue_TOO_LONG, resp.UnreachableItems[0].ItemIssues[1])
		require.Len(t, resp.Baskets, 1)
		require.Len(t, resp.Baskets[0].Items, 1)
		require.Equal(t, reachableItem, resp.Baskets[0].Items[0])
		require.Equal(t, cr.SplitStatus_PART_SPLIT, resp.Status)
	}
	{
		// 2*3 items, splitting in 3 baskets: (2), (1+1), (2)
		req := PrepareSplitRequest()
		req.Items[0].Weight = 75000
		req.Items[0].RequiredCount = 3
		req.Items[0].AvailableOffers[0].AvailableCount = 3
		req.Items[1].Weight = 80000
		req.Items[1].RequiredCount = 3
		resp, err := SplitOrder(req, makeConstraints(200000, 100000, 50000, 10000))
		require.NoError(t, err)
		require.Equal(t, cr.SplitStatus_SPLIT_OK, resp.Status)
		require.Equal(t, req.OrderId, resp.OrderId)
		require.Len(t, resp.UnreachableItems, 0)
		require.Len(t, resp.Baskets, 3)
		require.Len(t, resp.Baskets[0].Items, 1)
		require.Equal(t, 2, int(resp.Baskets[0].Items[0].RequiredCount))
		require.Len(t, resp.Baskets[1].Items, 2)
		require.Equal(t, 1, int(resp.Baskets[1].Items[0].RequiredCount))
		require.Equal(t, 1, int(resp.Baskets[1].Items[1].RequiredCount))
		require.Len(t, resp.Baskets[2].Items, 1)
		require.Equal(t, 2, int(resp.Baskets[2].Items[0].RequiredCount))
	}
	{
		// 2*3 items, splitting in 3 baskets: (2), (1+1), (2)
		req := PrepareSplitRequest()
		req.Items[0].Weight = 75000
		req.Items[0].RequiredCount = 3
		req.Items[0].AvailableOffers[0].AvailableCount = 3
		req.Items[1].Weight = 80000
		req.Items[1].RequiredCount = 3
		// no MK available shouldn't break algorithm (COMBINATOR-1992)
		constraints := makeConstraints(200000, 100000, 50000, 10000)
		constraints = constraints[:1]
		resp, err := SplitOrder(req, constraints)
		require.NoError(t, err)
		require.Equal(t, req.OrderId, resp.OrderId)
		require.Len(t, resp.UnreachableItems, 0)
		require.Len(t, resp.Baskets, 3)
		require.Equal(t, cr.SplitStatus_SPLIT_OK, resp.Status)
		require.Len(t, resp.Baskets[0].Items, 1)
		require.Equal(t, 2, int(resp.Baskets[0].Items[0].RequiredCount))
		require.Len(t, resp.Baskets[1].Items, 2)
		require.Equal(t, 1, int(resp.Baskets[1].Items[0].RequiredCount))
		require.Equal(t, 1, int(resp.Baskets[1].Items[1].RequiredCount))
		require.Len(t, resp.Baskets[2].Items, 1)
		require.Equal(t, 2, int(resp.Baskets[2].Items[0].RequiredCount))
	}
	{
		// splitting items for our own courier delivery
		// 3*5000 + 3*10000 into 20kg basket -> 3*5000 + 2*10000 + 1*10000
		req := PrepareSplitRequest()
		req.Items[0].Weight = 5000
		req.Items[0].RequiredCount = 3
		req.Items[0].AvailableOffers[0].AvailableCount = 3
		req.Items[1].Weight = 10000
		req.Items[1].RequiredCount = 3
		resp, err := SplitOrder(req, makeConstraints(40000, 30000, 20000, 10000))
		require.NoError(t, err)
		require.Equal(t, req.OrderId, resp.OrderId)
		require.Len(t, resp.UnreachableItems, 0)
		require.Len(t, resp.Baskets, 3)
		require.Len(t, resp.Baskets[0].Items, 1)
		require.Equal(t, 3, int(resp.Baskets[0].Items[0].RequiredCount))
		require.Equal(t, 5000, int(resp.Baskets[0].Items[0].Weight))
		require.Len(t, resp.Baskets[1].Items, 1)
		require.Equal(t, 2, int(resp.Baskets[1].Items[0].RequiredCount))
		require.Equal(t, 10000, int(resp.Baskets[1].Items[0].Weight))
		require.Len(t, resp.Baskets[2].Items, 1)
		require.Equal(t, 1, int(resp.Baskets[2].Items[0].RequiredCount))
		require.Equal(t, 10000, int(resp.Baskets[2].Items[0].Weight))
		require.Equal(t, cr.SplitStatus_SPLIT_OK, resp.Status)
	}
	{
		// splitting based on volume weight
		req := PrepareSplitRequest()
		req.Items[1].Weight = 10000
		req.Items[1].RequiredCount = 2
		req.Items[1].Dimensions = []uint32{100, 100, 60} // 0.6 m3 * 65 kg/m3 ==> 39 kg effective weight
		constraints := makeConstraints(40000, 30000, 20000, 10000)
		constraints[0].GlobalRule.SortedDimLimits = tarifficator.CreateSortedDims(150, 150, 80)
		constraints[0].GlobalRule.DimSumMax = 300
		resp, err := SplitOrder(req, constraints)
		require.NoError(t, err)
		require.Equal(t, req.OrderId, resp.OrderId)
		require.Len(t, resp.UnreachableItems, 0)
		require.Len(t, resp.Baskets, 3)
		require.Len(t, resp.Baskets[0].Items, 1)
		require.Equal(t, 1, int(resp.Baskets[0].Items[0].RequiredCount))
		require.Equal(t, 2000, int(resp.Baskets[0].Items[0].Weight))
		require.Len(t, resp.Baskets[1].Items, 1)
		require.Equal(t, 1, int(resp.Baskets[1].Items[0].RequiredCount))
		require.Equal(t, 10000, int(resp.Baskets[1].Items[0].Weight))
		require.Len(t, resp.Baskets[2].Items, 1)
		require.Equal(t, 1, int(resp.Baskets[2].Items[0].RequiredCount))
		require.Equal(t, 10000, int(resp.Baskets[2].Items[0].Weight))
		require.Equal(t, cr.SplitStatus_SPLIT_OK, resp.Status)
	}
	{
		// case with very long and very heavy items that go into separate DSs
		req := PrepareSplitRequest()
		// heavy one
		req.Items[0].Weight = 105000
		req.Items[0].RequiredCount = 1
		// long one
		req.Items[1].Weight = 10000
		req.Items[1].RequiredCount = 1
		req.Items[1].Dimensions = []uint32{300, 50, 60}
		constraints := makeConstraints(75000, 100000, 200000, 105000)
		constraints[0].GlobalRule.SortedDimLimits = tarifficator.CreateSortedDims(350, 70, 80)
		constraints[0].GlobalRule.DimSumMax = 410
		resp, err := SplitOrder(req, constraints)
		require.NoError(t, err)
		require.Equal(t, req.OrderId, resp.OrderId)
		require.Len(t, resp.UnreachableItems, 0)
		require.Len(t, resp.Baskets, 2)
		require.Len(t, resp.Baskets[0].Items, 1)
		require.Equal(t, 1, int(resp.Baskets[0].Items[0].RequiredCount))
		require.Equal(t, 10000, int(resp.Baskets[0].Items[0].Weight))
		require.Len(t, resp.Baskets[1].Items, 1)
		require.Equal(t, 1, int(resp.Baskets[1].Items[0].RequiredCount))
		require.Equal(t, 105000, int(resp.Baskets[1].Items[0].Weight))
		require.Equal(t, cr.SplitStatus_SPLIT_OK, resp.Status)
	}
	{
		// Check order of only fashion items
		req := PrepareSplitRequest()
		// fashion
		req.Items[0].CargoTypes = append(req.Items[0].CargoTypes, 600)
		req.Items[1].CargoTypes = append(req.Items[0].CargoTypes, 600)
		resp, err := SplitOrder(req, makeConstraints(200000, 20000, 10000, 20000))
		require.NoError(t, err)
		require.Equal(t, req.OrderId, resp.OrderId)
		require.Empty(t, resp.UnreachableItems)
		require.Equal(t, req.Items, resp.Baskets[0].Items)
		require.Equal(t, cr.SplitStatus_NOTHING, resp.Status)
	}
	{
		// split by price
		req := PrepareSplitRequest()
		req.Items[1].RequiredCount = 4
		constraints := makeConstraints(75000, 100000, 200000, 105000)
		constraints[0].MaxPrice = 80000
		constraints[1].MaxPrice = 50000
		resp, err := SplitOrder(req, constraints)
		require.NoError(t, err)
		require.Equal(t, cr.SplitStatus_SPLIT_OK, resp.Status)
		require.Equal(t, req.OrderId, resp.OrderId)
		require.Len(t, resp.UnreachableItems, 0)
		require.Len(t, resp.Baskets, 2)
		require.Len(t, resp.Baskets[0].Items, 2)
		// 10+20+20
		require.Equal(t, 1, int(resp.Baskets[0].Items[0].RequiredCount))
		require.Equal(t, 10000, int(resp.Baskets[0].Items[0].Price))
		require.Equal(t, 2, int(resp.Baskets[0].Items[1].RequiredCount))
		require.Equal(t, 20000, int(resp.Baskets[0].Items[1].Price))
		// 20+20
		require.Len(t, resp.Baskets[1].Items, 1)
		require.Equal(t, 2, int(resp.Baskets[1].Items[0].RequiredCount))
		require.Equal(t, 20000, int(resp.Baskets[1].Items[0].Price))
	}
	{
		// Empty order
		_, err := SplitOrder(nil, makeConstraints(40000, 30000, 20000, 10000))
		require.Error(t, err)
	}
	{
		// Too few dimensions
		req := PrepareSplitRequest()
		req.Items[0].Dimensions = req.Items[0].Dimensions[:1]
		_, err := SplitOrder(req, makeConstraints(200000, 40000, 50000, 20000))
		require.Error(t, err)
	}
}

func PrepareSplitRequest() *SplitOrderRequestPlus {
	req := SplitOrderRequestPlus{
		SplitOrderRequest: &cr.SplitOrderRequest{
			Items: []*cr.DeliveryRequestItem{
				{
					RequiredCount: 1,
					Weight:        2000,
					Dimensions: []uint32{
						20,
						20,
						15,
					},
					Price: 10000,
					AvailableOffers: []*cr.Offer{
						{
							ShopSku:        "322",
							ShopId:         1,
							PartnerId:      145,
							AvailableCount: 1,
						},
					},
					CargoTypes: nil,
				},
				{
					RequiredCount: 10,
					Weight:        4000,
					Dimensions: []uint32{
						15,
						18,
						20,
					},
					Price: 20000,
					AvailableOffers: []*cr.Offer{
						{
							ShopSku:        "",
							ShopId:         0,
							PartnerId:      145,
							AvailableCount: 3,
						},
						{
							ShopSku:        "",
							ShopId:         0,
							PartnerId:      145,
							AvailableCount: 10,
						},
					},
					CargoTypes: nil,
				},
			},
			OrderId: 111,
		},
	}
	return &req
}
