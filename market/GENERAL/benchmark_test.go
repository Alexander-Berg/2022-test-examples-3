package binpack

import (
	"testing"

	"github.com/stretchr/testify/require"

	cr "a.yandex-team.ru/market/combinator/proto/grpc"
)

func prepareBenchmarkData() ItemList {
	return ItemList{
		{Box: Box{11, 12, 13}, Count: 32, Weight: 1},
		{Box: Box{21, 22, 23}, Count: 16, Weight: 1},
		{Box: Box{31, 32, 33}, Count: 8, Weight: 1},
		{Box: Box{41, 42, 43}, Count: 4, Weight: 1},
		{Box: Box{51, 52, 53}, Count: 2, Weight: 1},
		{Box: Box{61, 62, 63}, Count: 1, Weight: 1},
	}
}

func TestBenchmarkData(t *testing.T) {
	items := prepareBenchmarkData()
	r1 := SplitIntoBoxes(items, nil)
	r2 := PourIntoBoxes(items, nil)
	v1, v2 := r1.TotalVolume(), r2.TotalVolume()
	require.Equal(t, 8, len(r1))
	require.Equal(t, 5, len(r2))
	require.True(t, r1.TotalWeight()-r2.TotalWeight() <= 1)
	require.Less(t, float64(v1-v2)/float64(v2), 0.17)
}

func BenchmarkPackInBoxes(b *testing.B) {
	items := prepareBenchmarkData()
	b.ResetTimer()
	b.Run("putIntoBoxes", func(b *testing.B) {
		for i := 0; i < b.N; i++ {
			_ = SplitIntoBoxes(items, nil)
		}
	})
	b.Run("pourIntoBoxes", func(b *testing.B) {
		for i := 0; i < b.N; i++ {
			_ = PourIntoBoxes(items, nil)
		}
	})
	b.Run("calcVirtualBox", func(b *testing.B) {
		for i := 0; i < b.N; i++ {
			_ = CalcVirtualBox(items)
		}
	})
}

func prepareCombinator913() ItemList {
	count := 99_999
	weight := 2
	return ItemList{Item{
		Box:    Box{2, 21, 29},
		Count:  count,
		Weight: weight,
	}}
}

func TestCombinator913(t *testing.T) {
	items := prepareCombinator913()
	{
		pbox := CalcVirtualBox(items)
		require.Equal(t, items.Weight(), pbox.Weight)
	}
	{
		pboxList := SplitIntoBoxes(items, nil)
		require.LessOrEqual(t, len(pboxList), 935)
		require.Equal(t, items.Weight(), pboxList.TotalWeight())
		// require.Equal(t, 935*MaxBoxSize.Volume(), pboxList.TotalVolume()) // TODO
	}
}

func BenchmarkCombinator913(b *testing.B) {
	items := prepareCombinator913()
	b.Run("SplitIntoBoxes", func(b *testing.B) {
		for i := 0; i < b.N; i++ {
			_ = SplitIntoBoxes(items, nil)
		}
	})
	b.Run("CalcVirtualBox", func(b *testing.B) {
		for i := 0; i < b.N; i++ {
			_ = CalcVirtualBox(items)
		}
	})
}

func prepareOrderSplitRequest() (*SplitOrderRequestPlus, []*SplitConstraints) {
	req := PrepareSplitRequest()
	req.Items[0].Weight = 5000
	req.Items[0].RequiredCount = 100
	req.Items[0].AvailableOffers[0].AvailableCount = 100
	req.Items[1].Weight = 10000
	req.Items[1].RequiredCount = 100
	req.Items[1].AvailableOffers[0].AvailableCount = 100
	return req, makeConstraints(40000, 40000, 20000, 20000)
}

func TestOrderSplit(t *testing.T) {
	req, constraints := prepareOrderSplitRequest()
	{
		resp, err := SplitOrder(req, constraints)
		require.NoError(t, err)
		require.Equal(t, req.OrderId, resp.OrderId)
		require.Len(t, resp.UnreachableItems, 0)
		require.Len(t, resp.Baskets, 75)
		require.Len(t, resp.Baskets[0].Items, 1)
		require.Equal(t, 4, int(resp.Baskets[0].Items[0].RequiredCount))
		require.Equal(t, 5000, int(resp.Baskets[0].Items[0].Weight))
		require.Len(t, resp.Baskets[50].Items, 1)
		require.Equal(t, 2, int(resp.Baskets[50].Items[0].RequiredCount))
		require.Equal(t, 10000, int(resp.Baskets[50].Items[0].Weight))
		require.Equal(t, cr.SplitStatus_SPLIT_OK, resp.Status)
	}
}

func BenchmarkOrderSplit(b *testing.B) {
	req, constraints := prepareOrderSplitRequest()
	b.Run("SplitOrder", func(b *testing.B) {
		for i := 0; i < b.N; i++ {
			_, _ = SplitOrder(req, constraints)
		}
	})
}
