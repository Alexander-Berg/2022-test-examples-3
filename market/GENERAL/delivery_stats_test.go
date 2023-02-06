package routes

import (
	"sort"
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/graph"
	"a.yandex-team.ru/market/combinator/pkg/outlets"
	tr "a.yandex-team.ru/market/combinator/pkg/tarifficator"
)

func TestOfferSearchData(t *testing.T) {
	osdList := []OfferSearchData{
		// [6]
		OfferSearchData{
			FromPartnerID: 2,
			WeightAndDim:  tr.NewWeightAndDim(1000, 111, 222, 333),
		},
		// [4, 5]
		OfferSearchData{
			FromPartnerID: 1,
			WeightAndDim:  tr.NewWeightAndDim(1000*2, 111*2, 222*2, 333*2),
			MarketSku:     "msku",
		},
		OfferSearchData{
			FromPartnerID: 1,
			WeightAndDim:  tr.NewWeightAndDim(1000, 111, 222, 333),
			MarketSku:     "msku",
		},
		// [3]
		OfferSearchData{
			FromPartnerID: 1,
			WeightAndDim:  tr.NewWeightAndDim(1000, 111, 222, 444),
		},
		// [1, 2]
		OfferSearchData{
			FromPartnerID: 1,
			WeightAndDim:  tr.NewWeightAndDim(1000, 111, 222, 333),
		},
		OfferSearchData{
			FromPartnerID: 1,
			WeightAndDim:  tr.NewWeightAndDim(1000-25, 111-5, 222-10, 333+15),
		},
		// [0]
		OfferSearchData{
			FromPartnerID: 1,
			WeightAndDim:  tr.NewWeightAndDim(900, 111, 222, 333),
		},
	}
	refs := make([]OfferSearchData, len(osdList))
	copy(refs, osdList)
	sort.Slice(osdList, func(i, j int) bool {
		return osdList[i].Less(&osdList[j])
	})

	// После сортировки получаем обратный порядок.
	require.Equal(t, refs[6], osdList[0])
	require.Equal(t, refs[5], osdList[1])
	require.Equal(t, refs[4], osdList[2])
	require.Equal(t, refs[3], osdList[3])
	require.Equal(t, refs[2], osdList[4])
	require.Equal(t, refs[1], osdList[5])
	require.Equal(t, refs[0], osdList[6])

	require.False(t, osdList[0].Equal(&osdList[1]))
	require.True(t, osdList[1].Equal(&osdList[2]))
	require.False(t, osdList[1].Equal(&osdList[3]))
	require.False(t, osdList[3].Equal(&osdList[4]))
	require.True(t, osdList[4].Equal(&osdList[5]))
	require.False(t, osdList[5].Equal(&osdList[6]))
}

func TestCalcGroupByWad(t *testing.T) {
	offers := []*OfferSearchData{
		// group1
		&OfferSearchData{
			FromPartnerID: 1,
			WeightAndDim:  tr.NewWeightAndDim(1000, 10, 10, 10),
		},
		&OfferSearchData{
			FromPartnerID: 1,
			WeightAndDim:  tr.NewWeightAndDim(2000, 20, 20, 20),
		},
		// group2
		&OfferSearchData{
			FromPartnerID: 2,
			WeightAndDim:  tr.NewWeightAndDim(1000, 10, 10, 10),
		},
		// group3
		&OfferSearchData{
			FromPartnerID: 1,
			WeightAndDim:  tr.NewWeightAndDim(10_000, 10, 10, 10),
		},
		&OfferSearchData{
			FromPartnerID: 1,
			WeightAndDim:  tr.NewWeightAndDim(9_990, 10, 10, 10),
		},
	}
	groups := calcGroupByWad(offers, calcGroupByWadOptions{
		m3weight:       60,
		maxSmallWeight: 4000,
		maxSmallSize:   30,
		roundingWeight: 100,
		roundingSize:   2,
	})
	require.Len(t, groups, 3)
}

func TestRoundInt(t *testing.T) {
	require.Equal(t, 10, int(roundInt(10, 0)))
	require.Equal(t, 10, int(roundInt(10, 1)))
	require.Equal(t, 10, int(roundInt(10, 2)))
	require.Equal(t, 12, int(roundInt(10, 3)))
	require.Equal(t, 12, int(roundInt(10, 4)))
	require.Equal(t, 10, int(roundInt(10, 5)))
	require.Equal(t, 100, int(roundInt(10, 100)))

	require.Equal(t, 9, int(roundInt(9, 0)))
	require.Equal(t, 9, int(roundInt(9, 1)))
	require.Equal(t, 10, int(roundInt(9, 2)))
	require.Equal(t, 9, int(roundInt(9, 3)))
	require.Equal(t, 12, int(roundInt(9, 4)))
	require.Equal(t, 10, int(roundInt(9, 5)))
}

func TestSplitMarketAndExternal(t *testing.T) {
	paths := graph.SPaths{
		{
			Path: &graph.Path{
				Nodes: graph.Nodes{
					{
						LogisticSegment: graph.LogisticSegment{
							PointLmsID: 0,
						},
					},
				},
			},
		},
		{
			Path: &graph.Path{
				Nodes: graph.Nodes{
					{
						LogisticSegment: graph.LogisticSegment{
							PointLmsID: 1,
						},
					},
				},
			},
		},
		{
			Path: &graph.Path{
				Nodes: graph.Nodes{
					{
						LogisticSegment: graph.LogisticSegment{
							PointLmsID: 2,
						},
					},
				},
			},
		},
		{
			Path: &graph.Path{
				Nodes: graph.Nodes{
					{
						LogisticSegment: graph.LogisticSegment{
							PointLmsID: 3,
						},
					},
				},
			},
		},
		{
			Path: &graph.Path{
				Nodes: graph.Nodes{
					{
						LogisticSegment: graph.LogisticSegment{
							PointLmsID: 4,
						},
					},
				},
			},
		},
	}
	regionMap := geobase.NewExample()
	ous := outlets.Make([]outlets.Outlet{
		{
			ID:              3,
			IsMarketBranded: false,
		},
		{
			ID:              1,
			IsMarketBranded: true,
		},
		{
			ID:              2,
			IsMarketBranded: true,
		},
	}, &regionMap, nil)

	market, external := splitMarketAndExternal(paths, ous, true)
	require.Len(t, market, 2)
	require.Len(t, external, 3)
	require.Equal(t, market[0], paths[1])
	require.Equal(t, market[1], paths[2])
	require.Equal(t, external[0], paths[0])
	require.Equal(t, external[1], paths[3])
	require.Equal(t, external[2], paths[4])
}

func TestSplitMarketAndExternalWithTariff(t *testing.T) {
	paths := graph.SPaths{
		{
			Path: &graph.Path{
				Nodes: graph.Nodes{
					{
						LogisticSegment: graph.LogisticSegment{
							PointLmsID: 0,
						},
					},
				},
			},
			ShopTariff: tr.NewTestOptionResult(true, 1212, []int64{0, 1, 5}),
		},
		{
			Path: &graph.Path{
				Nodes: graph.Nodes{
					{
						LogisticSegment: graph.LogisticSegment{
							PointLmsID: 1,
						},
					},
				},
			},
		},
		{
			Path: &graph.Path{
				Nodes: graph.Nodes{
					{
						LogisticSegment: graph.LogisticSegment{
							PointLmsID: 2,
						},
					},
				},
			},
		},
		{
			Path: &graph.Path{
				Nodes: graph.Nodes{
					{
						LogisticSegment: graph.LogisticSegment{
							PointLmsID: 3,
						},
					},
				},
			},
		},
		{
			Path: &graph.Path{
				Nodes: graph.Nodes{
					{
						LogisticSegment: graph.LogisticSegment{
							PointLmsID: 4,
						},
					},
				},
			},
		},
	}
	regionMap := geobase.NewExample()
	ous := outlets.Make([]outlets.Outlet{
		{
			ID:              0,
			IsMarketBranded: false,
			IsActive:        true,
		},
		{
			ID:              1,
			IsMarketBranded: true,
			IsActive:        true,
		},
		{
			ID:              2,
			IsMarketBranded: true,
			IsActive:        true,
		},
	}, &regionMap, nil)

	market, external := splitMarketAndExternal(paths, ous, true)
	require.Len(t, market, 3)
	require.Len(t, external, 3)
	require.Equal(t, market[0], paths[0])
	require.Equal(t, market[1], paths[1])
	require.Equal(t, market[2], paths[2])
	require.Equal(t, external[0], paths[0])
	require.Equal(t, external[1], paths[3])
	require.Equal(t, external[2], paths[4])
}
