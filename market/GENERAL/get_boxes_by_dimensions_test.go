package lite

import (
	"testing"

	"github.com/stretchr/testify/require"

	bg "a.yandex-team.ru/market/combinator/pkg/biggeneration"
	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/graph"
	tr "a.yandex-team.ru/market/combinator/pkg/tarifficator"
	pb "a.yandex-team.ru/market/combinator/proto/grpc"
)

func TestEmptyBoxesByDimensions(t *testing.T) {
	gb := graph.NewGraphBuilder()
	graphEx := graph.BuildExample1(gb)
	tariffsBuilder := tr.NewTariffsBuilder()
	genData := bg.GenerationData{
		RegionMap:     geobase.NewExample(),
		TariffsFinder: tariffsBuilder.TariffsFinder,
		Graph:         graphEx.Graph,
	}
	env, cancel := NewEnv(t, &genData, nil)
	defer cancel()

	var req *pb.BinPackRequest
	resp, err := env.Client.GetBoxesByDimensions(env.Ctx, req)
	require.Error(t, err)
	require.Nil(t, resp)

	req = boxByDimensionsRequest()
	resp, err = env.Client.GetBoxesByDimensions(env.Ctx, req)
	require.NoError(t, err)
	require.NotNil(t, resp)
	require.Len(t, resp.GetRecommendations(), 2)
	for _, rec := range resp.Recommendations {
		require.Equal(t, "NONPACK", rec.BoxId)
		require.NotEmpty(t, req.GetItems())
		require.NotNil(t, req.Items[0].GetUnitId())
		require.Equal(t, "test_sku", req.Items[0].UnitId.ShopSku)
	}
}

func boxByDimensionsRequest() *pb.BinPackRequest {
	return &pb.BinPackRequest{
		Boxes: []*pb.BoxBinPack{
			{
				Id: "YML",
				Dimensions: &pb.DimensionsBinPack{
					Length:      1,
					Width:       1,
					Height:      1,
					WeightGross: 9999,
				},
			},
			{
				Id: "NONPACK",
				Dimensions: &pb.DimensionsBinPack{
					Length:      0,
					Width:       0,
					Height:      0,
					WeightGross: 9999,
				},
			},
		},
		Items: []*pb.ItemBinPack{{
			UnitId: &pb.UnitIDBinPack{
				ShopSku:    "test_sku",
				SupplierId: "test_supplier",
			},
			Amount: 2,
			Dimensions: &pb.DimensionsBinPack{
				Length:      2,
				Width:       2,
				Height:      2,
				WeightGross: 1,
			},
		}},
		Version: 0,
	}
}
