package binpack

import (
	"testing"

	"github.com/stretchr/testify/require"
)

func TestPutItemsInBoxes(t *testing.T) {
	maxBoxSize := Box{100, 100, 100}
	makeItem := func(size int, count int) Item {
		return Item{
			Box:    Box{size, size, size},
			Weight: 100,
			Count:  count,
		}
	}
	{
		items := []Item{
			makeItem(50, 1),
		}
		boxes := PourIntoBoxes(items, &maxBoxSize)
		require.Len(t, boxes, 1)
		require.Equal(t, items[0].Box, boxes[0].Box)
		require.Equal(t, items[0].Count*int(items[0].Weight), int(boxes[0].Weight))
	}
	{
		items := []Item{
			makeItem(50, 10),
		}
		boxes := PourIntoBoxes(items, &maxBoxSize)
		require.Len(t, boxes, 2)
		require.Equal(t, maxBoxSize, boxes[0].Box)
		require.Equal(t, 8*int(items[0].Weight), int(boxes[0].Weight))
		require.InEpsilon(t, float64(items[0].Volume()), float64(boxes[1].Volume()), float64(boxes[1].Volume())/1000.)
	}
	{
		items := []Item{
			makeItem(50, 10), // 2 boxes
			makeItem(100, 3), // 3 box
			makeItem(200, 5), // 5 box
		}
		boxes := PourIntoBoxes(items, &maxBoxSize)
		require.Len(t, boxes, 10)
		for i := 0; i < 5; i++ {
			require.Equal(t, Box{200, 200, 200}, boxes[i].Box)
		}
		for i := 0; i < 3; i++ {
			require.Equal(t, Box{100, 100, 100}, boxes[5+i].Box)
		}
		require.Equal(t, Box{100, 100, 100}, boxes[8].Box)
		require.Equal(t, Box{63, 63, 63}, boxes[9].Box)
	}
}
