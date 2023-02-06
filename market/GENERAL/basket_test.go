package routes

import (
	"testing"

	"github.com/stretchr/testify/require"

	ffboxes "a.yandex-team.ru/market/combinator/pkg/fulfillmentboxes"
	tr "a.yandex-team.ru/market/combinator/pkg/tarifficator"
	combinator "a.yandex-team.ru/market/combinator/proto/grpc"
)

func TestBasketDimsHack(t *testing.T) {
	basket := Basket{
		PackingBoxes: []tr.WeightAndDim{
			tr.WeightAndDim{},
			tr.WeightAndDim{},
			tr.WeightAndDim{},
		},
	}
	basket.HackZeroDims()

	hackedDims := tr.WeightAndDim{
		Weight:     1,
		Dimensions: [3]uint32{1, 1, 1},
		DimSum:     1,
	}
	require.Equal(t, hackedDims, basket.VirtualBox)

	for _, v := range basket.PackingBoxes {
		require.Equal(t, hackedDims, v)
	}
}

func TestBasketFFBoxes(t *testing.T) {
	offer := combinator.Offer{
		PartnerId:      172,
		AvailableCount: 1,
	}
	basket := Basket{
		Items: []*Item{
			{
				DeliveryRequestItem: &combinator.DeliveryRequestItem{
					RequiredCount: 1,
					Weight:        1,
					Dimensions:    []uint32{10, 10, 10},
					AvailableOffers: []*combinator.Offer{
						&offer,
					},
					Price: 0,
				},
				Count: 1,
				Offer: &offer,
			},
		},
	}

	// кейс, когда есть
	ffPackingBoxes := basket.Items.calcRealBoxes(ffboxes.FFBoxes{
		ffboxes.FFBox{
			Box: [3]int{5, 5, 5},
		},
		ffboxes.FFBox{
			Box: [3]int{20, 20, 20},
		},
	})
	// должны выбрать коробку 20x20x20
	require.Len(t, ffPackingBoxes, 1)
	require.Equal(t, [3]uint32{20, 20, 20}, ffPackingBoxes[0].Dimensions)

	// кейс, когда нет коробки
	ffPackingBoxes = basket.Items.calcRealBoxes(ffboxes.FFBoxes{
		ffboxes.FFBox{
			Box: [3]int{5, 5, 5},
		},
		ffboxes.FFBox{
			Box: [3]int{9, 9, 9},
		},
	})
	// должно быть 10х10х10
	require.Len(t, ffPackingBoxes, 1)
	require.Equal(t, ffPackingBoxes[0].Dimensions, [3]uint32{10, 10, 10})

	// кейс, когда нет ни одной коробки
	ffPackingBoxes = basket.Items.calcRealBoxes(nil)
	// должно быть 10х10х10
	require.Len(t, ffPackingBoxes, 1)
	require.Equal(t, ffPackingBoxes[0].Dimensions, [3]uint32{10, 10, 10})
}
