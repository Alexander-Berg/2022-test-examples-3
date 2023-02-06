package graph

import (
	"math/rand"
	"sort"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/data"
	"a.yandex-team.ru/market/combinator/pkg/daysoff"
	"a.yandex-team.ru/market/combinator/pkg/deferredcourier"
	"a.yandex-team.ru/market/combinator/pkg/geobase"
	tr "a.yandex-team.ru/market/combinator/pkg/tarifficator"
	"a.yandex-team.ru/market/combinator/pkg/timex"
)

type TestPathSpec struct {
	Price              int
	Day                int
	HourFrom           int
	HourTo             int
	IsMarketCourier    bool
	IsPriorityCourier  bool
	IsDeferredCourier  bool
	Rating             int
	LinehaulRegionPath geobase.RegionChain
}

func makeTestDeliveryServiceID(spec *TestPathSpec, defaultValue int) uint64 {
	if spec.IsPriorityCourier {
		for id, v := range data.BestDservicesForSameInterval {
			// чтобы стабильно работало
			if v != 2 {
				continue
			}
			return id
		}
	}
	if spec.IsDeferredCourier {
		return uint64(deferredcourier.GetDeferredCourierID())
	}
	return uint64(defaultValue)
}

func MakeTestPath(spec *TestPathSpec, deliveryServiceID uint64) *SortablePath {
	startTime := time.Date(2020, 12, spec.Day, spec.HourFrom, 0, 0, 0, time.UTC)
	endTime := time.Date(2020, 12, spec.Day+1, spec.HourFrom, 0, 0, 0, time.UTC)
	endTimeTo := time.Date(2020, 12, spec.Day+1, spec.HourTo, 0, 0, 0, time.UTC)
	interval := Interval{
		From: timex.DayTime{Hour: int8(spec.HourFrom)},
		To:   timex.DayTime{Hour: int8(spec.HourTo)},
	}
	var path Path
	path.ServiceTimeList = append(path.ServiceTimeList, NewServiceTime(0, startTime))
	var route SortablePath
	route.Path = &path
	route.Price = uint32(spec.Price)
	route.ServiceRating = uint32(spec.Rating)
	route.EndTime = endTime
	route.EndTimeTo = endTimeTo
	route.Tails = append(route.Tails, EndTimeAndInterval{EndTime: endTime, Interval: interval})
	route.ShopTariff = tr.NewTestOptionResult(spec.IsMarketCourier, deliveryServiceID, nil)
	route.DeliveryServiceID = deliveryServiceID
	if spec.LinehaulRegionPath != nil {
		route.Path.Nodes = append(route.Path.Nodes,
			&Node{
				LogisticSegment: LogisticSegment{
					Type:       SegmentTypeLinehaul,
					RegionPath: spec.LinehaulRegionPath,
				},
			})
	}
	return &route
}

func MakeTestPaths(specs []TestPathSpec) SPaths {
	routes := make(SPaths, len(specs))
	for i, spec := range specs {
		routes[i] = MakeTestPath(&spec, makeTestDeliveryServiceID(&spec, i))
	}
	return routes
}

func TestPathCmp(t *testing.T) {
	specs := []TestPathSpec{
		{IsMarketCourier: true, HourFrom: 23, HourTo: 23},
		{HourFrom: 8, HourTo: 23, Rating: 2},
		{HourFrom: 8, HourTo: 23},
		{HourFrom: 9, HourTo: 23},

		{Price: 0, Day: 1, IsMarketCourier: true, HourFrom: 22, HourTo: 23},
		{Price: 0, Day: 1, IsMarketCourier: false, HourFrom: 7, HourTo: 23},
		{Price: 1, Day: 1, IsMarketCourier: true, HourFrom: 21, HourTo: 23},
		{Price: 1, Day: 1, IsMarketCourier: false, HourFrom: 6, HourTo: 23},

		{Price: 11, Day: 111},
		{Price: 111, Day: 11},
	}
	routes := MakeTestPaths(specs)
	rand.Shuffle(len(routes), func(i, j int) {
		routes[i], routes[j] = routes[j], routes[i]
	})
	// Используется PathCmpCheap
	sort.Stable(routes)
	for i, path := range routes {
		require.Equal(t, i, int(path.DeliveryServiceID))
	}
}

func TestIsBetterPickupRouteThan(t *testing.T) {
	regionRussia, regionTula := geobase.Region{ID: 225}, geobase.Region{ID: 15}
	chainRussia, chainTula := geobase.RegionChain{regionRussia}, geobase.RegionChain{regionTula, regionRussia}
	// deepest region > end time to > price
	specs := []TestPathSpec{
		{LinehaulRegionPath: chainTula, Price: 0, Day: 1, HourTo: 21},
		{LinehaulRegionPath: chainTula, Price: 1, Day: 1, HourTo: 21},
		{LinehaulRegionPath: chainTula, Price: 0, Day: 1, HourTo: 23},
		{LinehaulRegionPath: chainTula, Price: 1, Day: 1, HourTo: 23},
		{LinehaulRegionPath: chainRussia, Price: 0, Day: 1, HourTo: 21},
		{LinehaulRegionPath: chainRussia, Price: 1, Day: 1, HourTo: 21},
		{LinehaulRegionPath: chainRussia, Price: 0, Day: 1, HourTo: 23},
		{LinehaulRegionPath: chainRussia, Price: 1, Day: 1, HourTo: 23},
	}
	routes := MakeTestPaths(specs)
	rand.Shuffle(len(routes), func(i, j int) {
		routes[i], routes[j] = routes[j], routes[i]
	})
	sort.Slice(routes, func(i, j int) bool {
		return routes[i].IsBetterPickupRouteThan(routes[j], CmpOptions{})
	})
	for i, path := range routes {
		require.Equal(t, i, int(path.DeliveryServiceID))
	}
}

func TestAddDaysWithDisabled(t *testing.T) {
	start := time.Date(2020, 6, 1, 10, 0, 0, 0, time.UTC)
	{
		disabledDates := daysoff.NewDaysOffGroupedFromStrings(
			[]string{
				"2020-06-01",
				"2020-06-02",
				"2020-06-03",
				"2020-06-04",
				"2020-06-05",
			})
		// Add one day, but current day and next 4 days are disabled
		finish := addDaysWithDisabled(start, 1, disabledDates.DaysOffMap)
		expected := time.Date(2020, 6, 7, 10, 0, 0, 0, time.UTC)
		timesAreEqual(t, expected, finish)
	}
	{
		//disabledDates := daysoff.NewDaysOffGroupedFromStrings(nil)
		// Add one day, but current day and next 4 days are disabled
		finish := addDaysWithDisabled(start, 1, nil)
		expected := time.Date(2020, 6, 2, 10, 0, 0, 0, time.UTC)
		timesAreEqual(t, expected, finish)
	}
	// TODO(nickderev) write bench here
	{
		disabledDates := daysoff.NewDaysOffGroupedFromStrings(
			[]string{
				"2020-06-01",
				"2020-06-02",
			})
		finish := addDaysWithDisabled(start, 10000, disabledDates.DaysOffMap)
		expected := time.Date(2047, 10, 18, 10, 0, 0, 0, time.UTC)
		timesAreEqual(t, expected, finish)
	}
	// Today is ok but next 2 days are disabled
	// tariff is 1 day so we add 1 + 2 = 3 days
	{
		disabledDates := daysoff.NewDaysOffGroupedFromStrings(
			[]string{
				"2020-06-02",
				"2020-06-03",
			})
		finish := addDaysWithDisabled(start, 1, disabledDates.DaysOffMap)
		expected := time.Date(2020, 6, 4, 10, 0, 0, 0, time.UTC)
		timesAreEqual(t, expected, finish)
	}
	// the same but today and next days are ok
	// add only 1 day from tariff
	{
		disabledDates := daysoff.NewDaysOffGroupedFromStrings(
			[]string{
				"2020-06-03",
				"2020-06-04",
			})
		finish := addDaysWithDisabled(start, 1, disabledDates.DaysOffMap)
		expected := time.Date(2020, 6, 2, 10, 0, 0, 0, time.UTC)
		timesAreEqual(t, expected, finish)
	}
}
