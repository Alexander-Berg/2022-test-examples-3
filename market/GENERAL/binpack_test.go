package binpack

import (
	"testing"

	"github.com/stretchr/testify/require"
)

func Test0(t *testing.T) {
	box := Box{1, 2, 3}
	require.Equal(t, 6, box.Volume())

	box.Sort()
	require.Equal(t, Box{3, 2, 1}, box)
}

func TestSplit(t *testing.T) {
	{
		b1, b2 := SplitBoxByAxis(Box{2, 4, 6}, AxisZ, 5)
		require.Equal(t, Box{2, 4, 5}, b1)
		require.Equal(t, Box{2, 4, 1}, b2)
	}
	{
		box1, box2 := Box{10, 10, 10}, Box{7, 7, 7}
		result := SplitBox(box1, box2)
		want := []Box{
			Box{10, 10, 3},
			Box{10, 7, 3},
			Box{7, 7, 3},
		}
		require.Equal(t, want, result)
	}
	{
		box1, box2 := Box{10, 8, 5}, Box{8, 5, 1}
		result := SplitBox(box1, box2)
		require.Equal(t, box1.Volume(), box2.Volume()+BoxList(result).Volume())
		want := []Box{
			Box{10, 8, 4},
			Box{8, 2, 1},
			Box{8, 3, 1},
		}
		require.Equal(t, want, result)
	}
}

func TestCalcVirtualBox(t *testing.T) {
	descs := []struct {
		items ItemList
		want  Box
		full  bool
	}{
		{
			items: ItemList{
				{Box: Box{1, 1, 1}, Count: 8},
			},
			want: Box{2, 2, 2},
		},
		{
			items: ItemList{
				{Box: Box{3, 3, 1}, Count: 1},
				{Box: Box{2, 2, 2}, Count: 2},
				{Box: Box{1, 1, 1}, Count: 2},
			},
			want: Box{5, 3, 3},
		},
		{
			items: ItemList{
				{Box: Box{3, 3, 3}, Count: 3},
				{Box: Box{2, 2, 2}, Count: 10},
				{Box: Box{1, 1, 1}, Count: 19},
			},
			want: Box{6, 6, 5},
		},
		{
			items: ItemList{
				{Box: Box{3, 1, 1}, Count: 1},
				{Box: Box{2, 2, 1}, Count: 1},
				{Box: Box{1, 1, 1}, Count: 5},
			},
			want: Box{3, 2, 2},
			full: true,
		},
	}
	for i, desc := range descs {
		result := CalcVirtualBox(desc.items)
		require.Equal(t, desc.want, result.Box, i)
		if desc.full {
			require.Equal(t, desc.items.Volume(), result.Volume())
		}
		// require.True(t, false, "WTF")
	}
}

// Повторяем тесты синего репорта.
// https://a.yandex-team.ru/arc/trunk/arcadia/market/report/lite/test_actual_delivery.py?rev=7158315#L2815
func TestActualDelivery(t *testing.T) {
	make := func(count2 int) []Item {
		return []Item{
			{Box: Box{30, 20, 10}, Count: 1},
			{Box: Box{50, 15, 5}, Count: 1},
			{Box: Box{21, 8, 2}, Count: count2},
		}
	}
	type Desc struct {
		count int
		want  Box
	}
	descs := []Desc{
		{
			count: 4,
			want:  Box{50, 20, 17},
		},
		{
			count: 5,
			want:  Box{50, 20, 19},
		},
		{
			count: 25,
			want:  Box{50, 25, 24},
		},
		{
			count: 30,
			want:  Box{50, 25, 24},
		},
		{
			count: 31,
			want:  Box{50, 26, 25},
		},
	}
	for _, desc := range descs {
		result := CalcVirtualBox(make(desc.count))
		require.Equal(t, desc.want, result.Box)
	}
}

func TestSplitIntoBoxes(t *testing.T) {
	descs := []struct {
		items ItemList
		want  []PackingBox
	}{
		{
			items: ItemList{
				{Box: Box{1, 1, 1}, Count: 8, Weight: 1},
			},
			want: []PackingBox{
				PackingBox{Box: Box{2, 2, 2}, Weight: 8},
			},
		},
		{
			items: ItemList{
				{Box: Box{3, 3, 3}, Count: 1, Weight: 27},
				{Box: Box{2, 2, 2}, Count: 1, Weight: 8},
				{Box: Box{1, 1, 1}, Count: (27 - 8) + 1, Weight: 1},
			},
			want: []PackingBox{
				PackingBox{Box: Box{3, 3, 3}, Weight: 27},
				PackingBox{Box: Box{3, 3, 3}, Weight: 27},
				PackingBox{Box: Box{1, 1, 1}, Weight: 1},
			},
		},
		{
			items: ItemList{
				{Box: Box{3, 3, 3}, Count: 1, Weight: 27},
				{Box: Box{2, 2, 2}, Count: 2, Weight: 8},
				{Box: Box{1, 1, 1}, Count: (27 - 8), Weight: 1},
			},
			want: []PackingBox{
				PackingBox{Box: Box{3, 3, 3}, Weight: 27},
				PackingBox{Box: Box{3, 3, 3}, Weight: 27},
				PackingBox{Box: Box{2, 2, 2}, Weight: 8},
			},
		},
		{
			items: ItemList{
				{Box: Box{4, 4, 4}, Count: 1, Weight: 1},
				{Box: Box{3, 3, 3}, Count: 2, Weight: 2},
				{Box: Box{2, 2, 2}, Count: 3, Weight: 3},
				{Box: Box{1, 1, 1}, Count: 4, Weight: 4},
			},
			want: []PackingBox{
				PackingBox{Box: Box{4, 4, 4}, Weight: 1},
				PackingBox{Box: Box{3, 3, 3}, Weight: 2},
				PackingBox{Box: Box{3, 3, 3}, Weight: 2},
				PackingBox{Box: Box{3, 2, 2}, Weight: 19},
				PackingBox{Box: Box{2, 2, 2}, Weight: 3},
				PackingBox{Box: Box{2, 2, 2}, Weight: 3},
			},
		},
	}
	maxBoxSize := Box{3, 3, 3}
	for i, desc := range descs {
		result := SplitIntoBoxes(desc.items, &maxBoxSize)
		require.Equal(t, len(desc.want), len(result), i)
		for k := 0; k < len(desc.want); k++ {
			require.Equal(t, desc.want[k].Box, result[k].Box, i)
			require.Equal(t, desc.want[k].Weight, result[k].Weight, i)
		}
	}
}

func TestSplitIntoNonSquareBoxes(t *testing.T) {
	descs := []struct {
		items      ItemList
		maxBoxSize Box
		want       []PackingBox
	}{
		{
			items: ItemList{
				{Box: Box{11, 10, 10}, Count: 20, Weight: 1},
			},
			maxBoxSize: Box{1000, 10, 10},
			want: []PackingBox{
				PackingBox{Box: Box{220, 10, 10}, Weight: 20},
			},
		},
		{
			items: ItemList{
				{Box: Box{20, 10, 10}, Count: 20, Weight: 1}, // Длинный предмет
				{Box: Box{12, 12, 12}, Count: 20, Weight: 1}, // Широкий предмет
			},
			maxBoxSize: Box{1000, 12, 12},
			want: []PackingBox{
				PackingBox{Box: Box{640, 12, 12}, Weight: 40},
			},
		},
		{
			items: ItemList{
				{Box: Box{1, 1, 1}, Count: 9, Weight: 1},
			},
			maxBoxSize: Box{3, 3, 1}, // Приплюснутая коробка, как из-под пиццы
			want: []PackingBox{
				PackingBox{Box: Box{3, 3, 1}, Weight: 9},
			},
		},
	}
	for i, desc := range descs {
		result := SplitIntoBoxes(desc.items, &desc.maxBoxSize)
		require.Equal(t, len(desc.want), len(result), i)
		for k := 0; k < len(desc.want); k++ {
			require.Equal(t, desc.want[k].Box, result[k].Box, i)
			require.Equal(t, desc.want[k].Weight, result[k].Weight, i)
		}
	}
}

func TestSplitIntoRealBoxes(t *testing.T) {
	descs := []struct {
		items ItemList
		boxes []PackingBox
		want  []PackingBox
	}{
		{
			items: ItemList{
				{Box: Box{2, 2, 2}, Count: 2, Weight: 1},
			},
			boxes: []PackingBox{
				{Box: Box{1, 1, 1}, Weight: 1},
				{Box: Box{0, 0, 0}},
			},
			want: []PackingBox{
				PackingBox{Box: Box{2, 2, 2}, Weight: 1, ItemIDs: map[int]int{0: 1}, ID: 1},
				PackingBox{Box: Box{2, 2, 2}, Weight: 1, ItemIDs: map[int]int{0: 1}, ID: 1},
			},
		},
		{
			items: ItemList{
				{Box: Box{2, 2, 3}, Count: 1, Weight: 1},
				{Box: Box{2, 2, 3}, Count: 1, Weight: 1},
			},
			boxes: []PackingBox{
				{Box: Box{3, 3, 3}, Weight: 1},
				{Box: Box{2, 3, 3}, Weight: 1},
				{Box: Box{2, 2, 2}, Weight: 1},
				{Box: Box{1, 1, 1}, Weight: 1},
				{Box: Box{4, 2, 1}, Weight: 1},
				{Box: Box{0, 0, 0}},
			},
			want: []PackingBox{
				PackingBox{Box: Box{3, 2, 2}, Weight: 1, ItemIDs: map[int]int{0: 1}, ID: 1},
				PackingBox{Box: Box{3, 2, 2}, Weight: 1, ItemIDs: map[int]int{1: 1}, ID: 1},
			},
		},
		// Тестируем улучшенный алгоритм V2
		{
			items: ItemList{
				{Box: Box{1, 1, 1}, Count: 27, Weight: 1},
			},
			boxes: []PackingBox{
				{Box: Box{10, 10, 10}, Weight: 999}, // Предыдущая версия клала всё в эту коробку
				{Box: Box{9, 3, 1}, Weight: 999},    // Новая умеет подбирать эту
				{Box: Box{0, 0, 0}},
			},
			want: []PackingBox{
				PackingBox{Box: Box{9, 3, 1}, Weight: 27, ItemIDs: map[int]int{0: 27}, ID: 1},
			},
		},
	}
	for i, desc := range descs {
		result := SplitIntoRealBoxesV2(desc.items, desc.boxes)
		require.Equal(t, len(desc.want), len(result), i)
		for k := 0; k < len(desc.want); k++ {
			require.Equal(t, desc.want[k].Box, result[k].Box, i)
			require.Equal(t, desc.want[k].Weight, result[k].Weight, i)
			require.Equal(t, desc.want[k].ItemIDs, result[k].ItemIDs, i)
			require.Equal(t, desc.want[k].ID, result[k].ID, i)
		}
	}
}

func TestSplitIntoBoxesWithBeruSize(t *testing.T) {
	descs := []struct {
		items ItemList
		want  []PackingBox
	}{
		{
			items: ItemList{
				{Box: Box{51, 51, 25}, Count: 6, Weight: 1},
			},
			want: []PackingBox{
				PackingBox{Box: Box{75, 51, 51}, Weight: 3},
				PackingBox{Box: Box{75, 51, 51}, Weight: 3},
			},
		},
	}
	for i, desc := range descs {
		result := SplitIntoBoxes(desc.items, nil)
		require.Equal(t, len(desc.want), len(result), i)
		for k := 0; k < len(desc.want); k++ {
			require.Equal(t, desc.want[k].Box, result[k].Box, i)
			require.Equal(t, desc.want[k].Weight, result[k].Weight, i)
		}
	}
}
