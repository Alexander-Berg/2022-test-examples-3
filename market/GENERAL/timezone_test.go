package timex

import (
	"testing"
	"time"

	"github.com/stretchr/testify/require"
)

func TestLocationCache(t *testing.T) {
	cache := NewLocationCache()
	now := time.Now().Local()
	for i := 0; i < 3; i++ {
		offset := i * secondsInHour
		loc1 := cache.FindZone(offset)
		loc2 := time.FixedZone(loc1.String(), offset)
		t1 := now.In(loc1)
		t2 := now.In(loc2)
		require.Equal(t, t1, t2)
		require.Equal(t, i+1, cache.Size())
	}
	offset := 3600
	require.Equal(t, cache.FindZone(offset), cache.FindZone(offset))
}

func TestFixedZone(t *testing.T) {
	now := time.Now().Local()
	for i := 0; i < 24; i++ {
		offset := i * secondsInHour
		loc1 := FixedZone("", offset)
		loc2 := time.FixedZone(loc1.String(), offset)
		t1 := now.In(loc1)
		t2 := now.In(loc2)
		require.Equal(t, t1, t2)
	}
	cache := getLocationCache()
	require.Equal(t, 0, cache.Size()) // кеш пустой!
}
