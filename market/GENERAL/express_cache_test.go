package express

import (
	"testing"

	"github.com/stretchr/testify/require"
)

func TestCache(t *testing.T) {
	n1 := 0
	calc1 := func() CacheValue {
		n1 += 1
		return CacheValue(true)
	}

	cache := NewCache()
	key1 := CacheKey{PartnerID: 1, RegionID: 1}
	for i := 0; i < 2; i++ {
		val1 := cache.GetValue(key1, calc1)
		require.Equal(t, true, bool(val1))
		require.Equal(t, 1, cache.Size())
		require.Equal(t, 1, n1)
	}
}
