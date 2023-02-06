package graph

import (
	"testing"

	"github.com/stretchr/testify/require"

	pb "a.yandex-team.ru/market/combinator/proto/graph"
)

func TestNewKorobyteRestriction(t *testing.T) {
	repository := NewKorobyteRestrictionRepository()
	// 1. new different one => index is 0
	{
		korobyteProto := &pb.Service_KorobyteRestrictions{
			MinimumSizeCm:         []uint32{11, 22, 33},
			MaximumSizeCm:         []uint32{111, 222, 333},
			MinimumWeightG:        1024,
			MaximumWeightG:        2048,
			MinimumDimensionSumCm: 30,
			MaximumDimensionSumCm: 1000,
		}
		resultIndex := repository.NewKorobyteRestriction(korobyteProto)
		require.Equal(t, 0, resultIndex)
	}
	// 2. new different one => index is 1
	{
		korobyteProto := &pb.Service_KorobyteRestrictions{
			MinimumSizeCm:         []uint32{1, 2, 3},
			MaximumSizeCm:         []uint32{11, 22, 33},
			MinimumWeightG:        128,
			MaximumWeightG:        256,
			MinimumDimensionSumCm: 10,
			MaximumDimensionSumCm: 100,
		}
		resultIndex := repository.NewKorobyteRestriction(korobyteProto)
		require.Equal(t, 1, resultIndex)
	}
	// 3. same as first one => index is 0
	{
		korobyteProto := &pb.Service_KorobyteRestrictions{
			MinimumSizeCm:         []uint32{11, 22, 33},
			MaximumSizeCm:         []uint32{111, 222, 333},
			MinimumWeightG:        1024,
			MaximumWeightG:        2048,
			MinimumDimensionSumCm: 30,
			MaximumDimensionSumCm: 1000,
		}
		resultIndex := repository.NewKorobyteRestriction(korobyteProto)
		require.Equal(t, 0, resultIndex)
	}
	// 4. new different one => index is 2
	{
		korobyteYT := &korobyteRestrictionsYT{
			MinimumSizeCm:         []float64{5, 6, 7},
			MaximumSizeCm:         []float64{55, 66, 77},
			MinimumWeightG:        128,
			MaximumWeightG:        256,
			MinimumDimensionSumCm: 10,
			MaximumDimensionSumCm: 100,
		}
		resultIndex := repository.NewKorobyteRestriction(korobyteYT)
		require.Equal(t, 2, resultIndex)
	}
	// 5. same as second one => index is 1
	{
		korobyteYT := &korobyteRestrictionsYT{
			MinimumSizeCm:         []float64{1, 2, 3},
			MaximumSizeCm:         []float64{11, 22, 33},
			MinimumWeightG:        128,
			MaximumWeightG:        256,
			MinimumDimensionSumCm: 10,
			MaximumDimensionSumCm: 100,
		}
		resultIndex := repository.NewKorobyteRestriction(korobyteYT)
		require.Equal(t, 1, resultIndex)
	}
	// 6. uncompleted MinimumSizeCm => index is -1
	{
		korobyteYT := &korobyteRestrictionsYT{
			MinimumSizeCm:         []float64{1, 2},
			MaximumSizeCm:         []float64{11, 22, 33},
			MinimumWeightG:        128,
			MaximumWeightG:        256,
			MinimumDimensionSumCm: 10,
			MaximumDimensionSumCm: 100,
		}
		resultIndex := repository.NewKorobyteRestriction(korobyteYT)
		require.Equal(t, -1, resultIndex)
	}
	// 7. uncompleted MaximumSizeCm => index is -1
	{
		korobyteYT := &korobyteRestrictionsYT{
			MinimumSizeCm:         []float64{1, 2, 3},
			MaximumSizeCm:         []float64{11, 22},
			MinimumWeightG:        128,
			MaximumWeightG:        256,
			MinimumDimensionSumCm: 10,
			MaximumDimensionSumCm: 100,
		}
		resultIndex := repository.NewKorobyteRestriction(korobyteYT)
		require.Equal(t, -1, resultIndex)
	}
	// 8. empty => index is -1
	{
		korobyteYT := &korobyteRestrictionsYT{}
		resultIndex := repository.NewKorobyteRestriction(korobyteYT)
		require.Equal(t, -1, resultIndex)
	}
	require.Len(t, repository.korobytesMap, 3)
	require.Len(t, repository.korobytesSlice, 3)
	require.Equal(t, "calls: 8, valid: 3, duplicate: 2, invalid: 3, empty: 0", repository.Stats())
}

func TestGet(t *testing.T) {
	repository := NewKorobyteRestrictionRepository()
	{
		res, ok := repository.Get(-1)
		require.Equal(t, KorobyteRestriction{}, res)
		require.False(t, ok)
	}
	{
		res, ok := repository.Get(0)
		require.Equal(t, KorobyteRestriction{}, res)
		require.False(t, ok)
	}
	{
		res, ok := repository.Get(1)
		require.Equal(t, KorobyteRestriction{}, res)
		require.False(t, ok)
	}

	korobyteProto := &pb.Service_KorobyteRestrictions{
		MinimumSizeCm:         []uint32{11, 22, 33},
		MaximumSizeCm:         []uint32{111, 222, 333},
		MinimumWeightG:        1024,
		MaximumWeightG:        2048,
		MinimumDimensionSumCm: 30,
		MaximumDimensionSumCm: 1000,
	}
	repository.NewKorobyteRestriction(korobyteProto)

	korobyteYT := &korobyteRestrictionsYT{
		MinimumSizeCm:         []float64{5, 6, 7},
		MaximumSizeCm:         []float64{55, 66, 77},
		MinimumWeightG:        128,
		MaximumWeightG:        256,
		MinimumDimensionSumCm: 10,
		MaximumDimensionSumCm: 100,
	}
	repository.NewKorobyteRestriction(korobyteYT)

	wantKorobyte0 :=
		KorobyteRestriction{
			MinimumSizeCm:         [3]uint32{11, 22, 33},
			MaximumSizeCm:         [3]uint32{111, 222, 333},
			MinimumWeightG:        1024,
			MaximumWeightG:        2048,
			MinimumDimensionSumCm: 30,
			MaximumDimensionSumCm: 1000,
		}
	wantKorobyte1 := KorobyteRestriction{
		MinimumSizeCm:         [3]uint32{5, 6, 7},
		MaximumSizeCm:         [3]uint32{55, 66, 77},
		MinimumWeightG:        128,
		MaximumWeightG:        256,
		MinimumDimensionSumCm: 10,
		MaximumDimensionSumCm: 100,
	}

	korobyte0, ok0 := repository.Get(0)
	korobyte1, ok1 := repository.Get(1)

	require.Equal(t, wantKorobyte0, korobyte0)
	require.Equal(t, wantKorobyte1, korobyte1)
	require.True(t, ok0)
	require.True(t, ok1)
	require.Len(t, repository.korobytesMap, 2)
	require.Len(t, repository.korobytesSlice, 2)
	require.Equal(t, "calls: 2, valid: 2, duplicate: 0, invalid: 0, empty: 0", repository.Stats())

}
