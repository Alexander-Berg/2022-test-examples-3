package ffboxes

import (
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/binpack"
)

func TestPickFulfillmentBox(t *testing.T) {
	ffBoxesMap := FulfillmentBoxesMap{
		172: FFBoxes{
			FFBox{
				Box: [3]int{20, 20, 20},
			},
			FFBox{
				Box: [3]int{5, 5, 5},
			},
		},
	}
	for _, boxes := range ffBoxesMap {
		SortBoxes(boxes)
	}

	box := ffBoxesMap.PickFulfillmentBox(172, binpack.Box{10, 10, 10})
	require.Equal(t, binpack.Box{20, 20, 20}, box)

	box = ffBoxesMap.PickFulfillmentBox(172, binpack.Box{20, 20, 20})
	require.Equal(t, binpack.Box{20, 20, 20}, box)

	box = ffBoxesMap.PickFulfillmentBox(172, binpack.Box{21, 20, 20})
	require.Equal(t, binpack.Box{21, 20, 20}, box)

	box = ffBoxesMap.PickFulfillmentBox(172, binpack.Box{1, 2, 5})
	require.Equal(t, binpack.Box{5, 5, 5}, box)

	box = ffBoxesMap.PickFulfillmentBox(171, binpack.Box{1, 2, 5})
	require.Equal(t, binpack.Box{1, 2, 5}, box)
}
