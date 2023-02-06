package graph

import (
	"fmt"
	"testing"
	"unsafe"

	"github.com/stretchr/testify/assert"
)

// Этот тест нужен для понимания сколько занимают структуры в памяти.
// Можно смело менять значения в случае fail)
func TestSizeof(t *testing.T) {
	assert.Equal(t, 56, int(unsafe.Sizeof(LogisticService{})), "LogisticService")
	assert.Equal(t, 184, int(unsafe.Sizeof(LogisticServiceYT{})), "LogisticServiceYT")

	assert.Equal(t, 24, int(unsafe.Sizeof(CargoTypes{})), "CargoTypes")
	assert.Equal(t, 88, int(unsafe.Sizeof(LogisticSegment{})), "LogisticSegment")
	assert.Equal(t, 160, int(unsafe.Sizeof(Node{})), "Node")

	assert.Equal(t, 24, int(unsafe.Sizeof(DSBSPickupSegmentFeatures{})), "DSBSPickupSegmentFeatures")

	assert.Equal(t, 184, int(unsafe.Sizeof(Path{})), "Path")
	assert.Equal(t, 120, int(unsafe.Sizeof(SortablePath{})), "SortablePath")
}

const featureCount = 3675779

func BenchmarkMap(b *testing.B) {
	m := make(map[int64]DSBSPickupSegmentFeatures)
	for i := int64(0); i < featureCount; i++ {
		key := i * i
		m[key] = DSBSPickupSegmentFeatures{
			ID: key,
		}
	}
}

func BenchmarkMapWithLen(b *testing.B) {
	m := make(map[int64]DSBSPickupSegmentFeatures, featureCount)
	for i := int64(0); i < featureCount; i++ {
		key := i * i
		m[key] = DSBSPickupSegmentFeatures{
			ID: key,
		}
	}
}

func BenchmarkSlice(b *testing.B) {
	type pair struct {
		key      int64
		features DSBSPickupSegmentFeatures
	}
	s := make([]pair, 0, featureCount)
	for i := int64(0); i < featureCount; i++ {
		key := i * i
		//nolint:SA4010
		s = append(
			s,
			pair{
				key: key,
				features: DSBSPickupSegmentFeatures{
					ID: key,
				},
			},
		)
	}
}

func TestB(t *testing.T) {
	type bf func(b *testing.B)
	bfs := map[string]bf{
		"map":          BenchmarkMap,
		"map with len": BenchmarkMapWithLen,
		"slice":        BenchmarkSlice,
	}
	for name, b := range bfs {
		res := testing.Benchmark(b)
		fmt.Printf("%s mem=%d allocs=%d\n    %s\n", name, res.MemBytes, res.MemAllocs, res.MemString())
	}
	//assert.Equal(t, 0, 1)
}
