package geobase

import (
	"bytes"
	"sort"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

var testGeobaseXML = `
<regions>
	<region id="10000" parent="0" name="Земля" type="0" tz_offset="0"/>
	<region id="10001" parent="10000" name="Евразия" type="1" tz_offset="0"/>
	<region id="225" parent="10001" name="Россия" type="3" tz_offset="0"/>
	<region id="3" parent="225" name="Центральный федеральный округ" type="4" tz_offset="10800"/>
	<region id="1" parent="3" name="Москва и Московская область" type="5" tz_offset="10800"/>
	<region id="213" parent="1" name="Москва" type="6" tz_offset="10800"/>
</regions>
`

func TestRead(t *testing.T) {
	buf := bytes.NewBufferString(testGeobaseXML)
	geobase, err := Read(buf)
	assert.NoError(t, err)

	reg, err := geobase.FindRegion(RegionMoscow)
	assert.NoError(t, err)
	assert.Equal(t, reg.Name, "Москва")
	assert.Equal(t, reg.TzOffset, 10800)

	_, err = geobase.FindRegion(4)
	assert.Error(t, err, ErrBadID)

	path := geobase.FindPath(RegionMoscow)
	assert.Equal(t, path, []RegionID{213, 1, 3, 225, 10001, 10000})
}

func TestRegionMatches(t *testing.T) {
	regions := NewExample()
	for _, rpath1 := range regions {
		for _, rpath2 := range regions {
			r1 := RegionMatchesSlow(rpath1, rpath2)
			r2 := RegionMatches(rpath1, rpath2)
			require.Equal(t, r1, r2)
		}
	}
}

func BenchmarkRegionMatches(b *testing.B) {
	regions := NewExample()
	doit := func(match func(RegionChain, RegionChain) bool) {
		for _, rpath1 := range regions {
			for _, rpath2 := range regions {
				_ = match(rpath1, rpath2)
			}
		}
	}
	b.ResetTimer()

	b.Run("Slow", func(b *testing.B) {
		for i := 0; i < b.N; i++ {
			doit(RegionMatchesSlow)
		}
	})
	b.Run("Fast", func(b *testing.B) {
		for i := 0; i < b.N; i++ {
			doit(RegionMatches)
		}
	})
}

func TestRegionDistance(t *testing.T) {
	regions := NewExample()
	{
		d1, d2, ok := RegionDistance(regions[21651], regions[213])
		require.True(t, ok)
		require.Equal(t, 2, d1)
		require.Equal(t, 1, d2)
	}
	{
		d1, d2, ok := RegionDistance(regions[21651], regions[213])
		require.True(t, ok)
		require.Equal(t, 2, d1)
		require.Equal(t, 1, d2)
	}
	{
		d1, d2, ok := RegionDistance(regions[2], regions[213])
		require.True(t, ok)
		require.Equal(t, 3, d1)
		require.Equal(t, 3, d2)
	}
	{
		_, _, ok := RegionDistance(regions[322], regions[213])
		require.False(t, ok)
	}
}

func TestRegionDistance1(t *testing.T) {
	regions := NewExample()
	tests := []struct {
		from RegionID
		to   RegionID
		dist int
		ok   bool
	}{
		{
			from: 213,
			to:   213,
			dist: 0,
			ok:   true,
		},
		{
			from: 213,
			to:   1,
			dist: 1,
			ok:   true,
		},
		{
			from: 213,
			to:   2,
			ok:   false,
		},
		{
			from: 213,
			to:   120542,
			ok:   false,
		},
	}
	for _, test := range tests {
		d, ok := RegionDistance1(regions[test.from], regions[test.to])
		if test.ok {
			require.True(t, ok)
			require.Equal(t, test.dist, d)
		} else {
			require.False(t, ok)
		}
	}
	checkFalse := func(path RegionChain, region RegionChain) {
		_, ok := RegionDistance1(path, region)
		require.False(t, ok)
	}
	checkFalse(regions[213], nil)
	checkFalse(nil, regions[213])
	checkFalse(nil, nil)
}

func TestEnrichRegionUpTheTree(t *testing.T) {
	regions := NewExample()
	{
		paths, _ := EnrichRegionUpTheTree(regions[120013], false)
		require.Len(t, paths, 3)
		require.Equal(t, 120013, int(paths[0][0].ID)) // type: 7
		require.Equal(t, 161541, int(paths[1][0].ID)) // type: 15
		require.Equal(t, 98605, int(paths[2][0].ID))  // type: 10
	}
	{
		paths, _ := EnrichRegionUpTheTree(regions[21651], false)
		require.Len(t, paths, 2)
		require.Equal(t, 21651, int(paths[0][0].ID))  // type: 6
		require.Equal(t, 121006, int(paths[1][0].ID)) // type: 10
	}
	{
		paths, _ := EnrichRegionUpTheTree(regions[213], false) // type: 6
		require.Len(t, paths, 1)
		require.Equal(t, 213, int(paths[0][0].ID))
	}
	{
		paths, _ := EnrichRegionUpTheTree(regions[1], false) // type: 5
		require.Len(t, paths, 1)
		require.Equal(t, 1, int(paths[0][0].ID))
	}
	{
		paths, _ := EnrichRegionUpTheTree(nil, false)
		require.Len(t, paths, 0)
	}
}

func TestGetTown(t *testing.T) {
	example := NewExample()

	require.Equal(t, RegionMoscow, example[RegionHamovniki].GetTown().ID)

	for _, region := range []RegionID{RegionMoscow, RegionSofyno, RegionKotelniki} {
		require.Equal(t, region, example[region].GetTown().ID)
	}
}

func TestCollapse(t *testing.T) {
	example := NewExample()

	collapse := func(regions ...RegionID) []RegionID {
		res := GetLeafs(Collapse(example.MakeChains(regions)))
		sort.Slice(res, func(i, j int) bool {
			return res[i] < res[j]
		})
		return res
	}

	{
		res := collapse()
		require.Len(t, res, 0)
	}
	{
		res := collapse(RegionMoscow, RegionSaintPetersburg)
		require.Len(t, res, 2)
		require.Equal(t, RegionSaintPetersburg, res[0])
		require.Equal(t, RegionMoscow, res[1])
	}
	{
		res := collapse(RegionMoscow, RegionSaintPetersburg, RegionMoscowAndObl, RegionSofyno, 65635)
		require.Len(t, res, 2)
		require.Equal(t, RegionMoscowAndObl, res[0])
		require.Equal(t, RegionSaintPetersburg, res[1])
	}
	{
		var regions []RegionID
		for region := range example {
			regions = append(regions, region)
		}
		res := collapse(regions...)
		require.Equal(t, []RegionID{11, RegionRussia}, res)
	}
}
