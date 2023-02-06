package graph

import (
	"fmt"
	"math"
	"testing"

	"github.com/stretchr/testify/require"
)

func TestMakePathsWithIntervalsAndMixVer2(t *testing.T) {
	type Want struct {
		Hour int
		Ds   int
	}
	type Spec struct {
		specs []TestPathSpec
		wants []Want
	}
	specs := []Spec{
		{},
		{
			specs: []TestPathSpec{
				{HourFrom: 10, HourTo: 23, IsMarketCourier: true},
				{HourFrom: 15, HourTo: 23, IsMarketCourier: true},
				{HourFrom: 20, HourTo: 23, IsMarketCourier: true},
			},
			wants: []Want{
				{Hour: 10, Ds: 0},
				{Hour: 15, Ds: 1},
				{Hour: 20, Ds: 2},
			},
		},
		{
			specs: []TestPathSpec{
				{HourFrom: 10, HourTo: 23, IsMarketCourier: true},
				{HourFrom: 10, HourTo: 23, IsMarketCourier: true},
			},
			wants: []Want{
				{Hour: 10, Ds: 0},
			},
		},
	}
	for _, spec := range specs {
		routes := MakeTestPaths(spec.specs)
		result := makePathsWithIntervalsAndMixV2(routes)
		require.Len(t, result, len(spec.wants))
		for i, want := range spec.wants {
			require.Equal(t, want.Hour, int(result[i].Tail.Interval.From.Hour))
			require.Equal(t, want.Ds, int(result[i].Path.DeliveryServiceID))
		}
	}
	require.Len(t, makePathsWithIntervalsAndMixV2(nil), 0)
}

// TestMakePathWithIntervalsAndMixDeferredCourier проверяет логику слияния
// обычных курьерских и часовых интервалов
//
// Важно: МК 2-ой волны имеют больший приоритет, чем все остальные СД
func TestMakePathWithIntervalsAndMixDeferredCourier(t *testing.T) {
	type deliveryServiceInterval struct {
		hourFrom          int8
		hourTo            int8
		isMarketCourier   bool
		servicePriority   int
		isDeferredCourier bool
	}
	testCases := []struct {
		specs    []TestPathSpec
		expected []deliveryServiceInterval
	}{
		{
			specs: []TestPathSpec{
				// Важно: здесь порядок опций как при соритровке путей
				// с использование компаратора SPaths.Less
				{HourFrom: 12, HourTo: 13, IsDeferredCourier: true},
				{HourFrom: 13, HourTo: 14, IsDeferredCourier: true},
				{HourFrom: 14, HourTo: 15, IsDeferredCourier: true},
				{HourFrom: 15, HourTo: 16, IsDeferredCourier: true},

				{HourFrom: 14, HourTo: 20, IsMarketCourier: true, IsPriorityCourier: true},
				{HourFrom: 14, HourTo: 20, IsMarketCourier: true},

				{HourFrom: 14, HourTo: 20},
			},
			expected: []deliveryServiceInterval{
				{hourFrom: 14, hourTo: 20, isMarketCourier: true, servicePriority: 2},

				{hourFrom: 12, hourTo: 13, isDeferredCourier: true},
				{hourFrom: 13, hourTo: 14, isDeferredCourier: true},
				{hourFrom: 14, hourTo: 15, isDeferredCourier: true},
				{hourFrom: 15, hourTo: 16, isDeferredCourier: true},
			},
		},
	}

	for i, tc := range testCases {
		tcMsg := fmt.Sprintf("test case %d", i)

		routes := MakeTestPaths(tc.specs)
		pathsWithIntervals := makePathWithIntervalsAndMixDeferredCourier(routes)
		require.Len(t, pathsWithIntervals, len(tc.expected), tcMsg)
		for j, expectedInterval := range tc.expected {
			if expectedInterval.servicePriority == 0 {
				expectedInterval.servicePriority = math.MaxInt32
			}
			intMsg := fmt.Sprintf("expected interval %d", j)

			pj := pathsWithIntervals[j]
			require.Equal(t, expectedInterval.hourFrom, pj.Tail.Interval.From.Hour, tcMsg, intMsg)
			require.Equal(t, expectedInterval.hourTo, pj.Tail.Interval.To.Hour, tcMsg, intMsg)
			require.Equal(t, expectedInterval.isMarketCourier, pj.ShopTariff.IsMarketCourier, tcMsg, intMsg)
			require.Equal(t, expectedInterval.servicePriority, pj.ShopTariff.ServicePriority(), tcMsg, intMsg)
			require.Equal(t, expectedInterval.isDeferredCourier, pj.ShopTariff.IsDeferredCourier(), tcMsg, intMsg)
		}
	}

	require.Len(t, makePathWithIntervalsAndMixDeferredCourier(nil), 0)
}
