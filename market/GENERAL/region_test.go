package geobase

import (
	"testing"

	"github.com/stretchr/testify/require"
)

func TestGoUpToRegion(t *testing.T) {
	regions := NewExample()
	{
		_, err := GoUpToRegion(nil)
		require.Error(t, err)
	}
	{
		_, err := GoUpToRegion(regions[RegionMoscow])
		require.Error(t, err)
	}
	{
		rid, err := GoUpToRegion(regions[RegionMoscow], RegionTypeRegion)
		require.NoError(t, err)
		require.Equal(t, RegionMoscowAndObl, rid)
	}
}

func TestGoUpToRegionOrDefault(t *testing.T) {
	regions := NewExample()
	{
		_, err := GoUpToRegionOrDefault(nil)
		require.Error(t, err)
	}
	{
		rid, err := GoUpToRegionOrDefault(regions[RegionMoscow])
		require.NoError(t, err)
		require.Equal(t, RegionMoscow, rid)
	}
	{
		rid, err := GoUpToRegionOrDefault(regions[RegionMoscow], RegionTypeRegion)
		require.NoError(t, err)
		require.Equal(t, RegionMoscowAndObl, rid)
	}
}
