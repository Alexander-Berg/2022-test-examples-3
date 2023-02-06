package graph

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/timex"
)

type Ranger struct {
	pathsList []SPaths
}

func (r *Ranger) FindAllPaths(ctx context.Context, startTime time.Time, opts ...PathFinderOption) (SPaths, error) {
	var res SPaths
	if len(r.pathsList) != 0 {
		res, r.pathsList = r.pathsList[0], r.pathsList[1:]
	}
	return res, nil
}

func (r *Ranger) GetGraph() *Graph {
	return nil
}

func TestFindBestCourierPaths(t *testing.T) {
	specs := []TestPathSpec{
		// Базовые пути
		{Day: 0, HourFrom: 9, HourTo: 23, Price: 11},
		{Day: 0, HourFrom: 10, HourTo: 23}, // best
		{Day: 1, HourFrom: 11, HourTo: 23}, // best+1
		// Пути на следующие дни
		{Day: 1, HourFrom: 9, HourTo: 23, Price: 11},
		{Day: 2, HourFrom: 12, HourTo: 23}, // best+2
		{Day: 3, HourFrom: 13, HourTo: 23}, // best+3
		{Day: 4, HourFrom: 9, HourTo: 23},
		{Day: 4, HourFrom: 14, HourTo: 23, IsMarketCourier: true}, // best+4
		{Day: 4, HourFrom: 20, HourTo: 23, IsMarketCourier: true}, // не попадает как путь, но попадает как "путь+интервал"
		{Day: 5, HourFrom: 9, HourTo: 23},
	}
	routes := MakeTestPaths(specs)
	ranger := &Ranger{[]SPaths{routes[:3], routes[3:]}}
	res, err := FindBestCourierPaths(context.Background(), ranger, routes[0].EndTime, nil)
	require.NoError(t, err)
	top := res.SPaths
	require.Len(t, top, 5)
	require.Equal(t, 1, int(top[0].DeliveryServiceID))
	require.Equal(t, 2, int(top[1].DeliveryServiceID))
	require.Equal(t, 4, int(top[2].DeliveryServiceID))
	require.Equal(t, 5, int(top[3].DeliveryServiceID))
	require.Equal(t, 7, int(top[4].DeliveryServiceID))
	piList := res.PathsWithIntervals
	require.Len(t, piList, 6)
	require.Equal(t, 7, int(piList[4].Path.DeliveryServiceID))
	require.Equal(t, 8, int(piList[5].Path.DeliveryServiceID))
	{
		routes := MakeTestPaths(specs[1:2])
		ranger := &Ranger{[]SPaths{routes, nil}}
		_, err := FindBestCourierPaths(context.Background(), ranger, routes[0].EndTime, nil)
		require.NoError(t, err)
	}
}

func TestFindPathsUntilDate(t *testing.T) {
	specs := []TestPathSpec{
		{Day: 0, HourFrom: 9, HourTo: 23},
		//
		{Day: 1, HourFrom: 9, HourTo: 23},
		{Day: 2, HourFrom: 9, HourTo: 23},
		{Day: 3, HourFrom: 9, HourTo: 23},
		{Day: 4, HourFrom: 9, HourTo: 23},
		{Day: 5, HourFrom: 9, HourTo: 23},
		//
		{Day: 4, HourFrom: 9, HourTo: 23},
		{Day: 4, HourFrom: 9, HourTo: 23},
	}
	routes := MakeTestPaths(specs)
	ranger := &Ranger{[]SPaths{
		routes[1:6],
		routes[6:8],
	}}
	startTime := routes[0].EndTime
	lastDate := AddDaysAndShiftAtMidnight(routes[0].EndTime, 4)
	paths, err := findPathsUntilDate(context.Background(), ranger, startTime, lastDate, nil)
	require.NoError(t, err)
	require.Len(t, paths, 6)
}

func TestAddDaysAndShiftAtMidnight(t *testing.T) {
	t1 := time.Date(2020, 10, 2, 13, 14, 15, 0, time.Local)
	require.Equal(t, time.Date(t1.Year(), t1.Month(), t1.Day(), 0, 0, 0, 0, time.Local), AddDaysAndShiftAtMidnight(t1, 0))
	require.Equal(t, time.Date(t1.Year(), t1.Month(), t1.Day()+1, 0, 0, 0, 0, time.Local), AddDaysAndShiftAtMidnight(t1, 1))
	require.Equal(t, time.Date(t1.Year(), t1.Month()+1, t1.Day(), 0, 0, 0, 0, time.Local), AddDaysAndShiftAtMidnight(t1, 31))
}

func TestChooseBestCourierPathsForExpress(t *testing.T) {
	times := []struct {
		startTime time.Time
		endTime   time.Time
		interval  Interval
	}{
		// sorting and doubles removing
		// -> 13:00-13:40
		{
			time.Date(2022, 5, 28, 10, 0, 0, 0, time.Local),
			time.Date(2022, 5, 28, 13, 0, 0, 0, time.Local),
			Interval{
				From: timex.DayTime{Hour: 13, Minute: 0},
				To:   timex.DayTime{Hour: 13, Minute: 40},
			},
		},
		// -> 13:30-14:10
		{
			time.Date(2022, 5, 28, 10, 30, 0, 0, time.Local),
			time.Date(2022, 5, 28, 13, 30, 0, 0, time.Local),
			Interval{
				From: timex.DayTime{Hour: 13, Minute: 30},
				To:   timex.DayTime{Hour: 14, Minute: 10},
			},
		},
		// -> 14:00-14:40
		{
			time.Date(2022, 5, 28, 10, 30, 0, 0, time.Local),
			time.Date(2022, 5, 28, 14, 0, 0, 0, time.Local),
			Interval{
				From: timex.DayTime{Hour: 14, Minute: 0},
				To:   timex.DayTime{Hour: 14, Minute: 40},
			},
		},
		// -> 14:30-15:10
		{
			time.Date(2022, 5, 28, 11, 30, 0, 0, time.Local),
			time.Date(2022, 5, 28, 14, 30, 0, 0, time.Local),
			Interval{
				From: timex.DayTime{Hour: 14, Minute: 30},
				To:   timex.DayTime{Hour: 15, Minute: 10},
			},
		},
		// will be removed as a duplicate
		{
			time.Date(2022, 5, 28, 10, 0, 0, 0, time.Local),
			time.Date(2022, 5, 28, 13, 0, 0, 0, time.Local),
			Interval{
				From: timex.DayTime{Hour: 13, Minute: 0},
				To:   timex.DayTime{Hour: 13, Minute: 40},
			},
		},

		// -> 15:30-16:10
		{
			time.Date(2022, 5, 28, 13, 15, 0, 0, time.Local),
			time.Date(2022, 5, 28, 15, 25, 0, 0, time.Local),
			Interval{
				From: timex.DayTime{Hour: 15, Minute: 25},
				To:   timex.DayTime{Hour: 16, Minute: 5},
			},
		},

		// earliest interval lefts without rounding
		// -> 12:05-12:45
		{
			time.Date(2022, 5, 28, 8, 55, 0, 0, time.Local),
			time.Date(2022, 5, 28, 12, 5, 0, 0, time.Local),
			Interval{
				From: timex.DayTime{Hour: 12, Minute: 5},
				To:   timex.DayTime{Hour: 12, Minute: 45},
			},
		},
		// next ones will be as one path with startTime 9:15 and interval 12:30-13:10
		// rounding up to 30 minutes and chosing fastest
		// will be removed as it's have longer delivery duration for interval 12:30-13:10
		{
			time.Date(2022, 5, 28, 9, 0, 0, 0, time.Local),
			time.Date(2022, 5, 28, 12, 10, 0, 0, time.Local),
			Interval{
				From: timex.DayTime{Hour: 12, Minute: 10},
				To:   timex.DayTime{Hour: 12, Minute: 50},
			},
		},
		// will be removed as it's have longer delivery duration for interval 12:30-13:10
		{
			time.Date(2022, 5, 28, 9, 5, 0, 0, time.Local),
			time.Date(2022, 5, 28, 12, 15, 0, 0, time.Local),
			Interval{
				From: timex.DayTime{Hour: 12, Minute: 15},
				To:   timex.DayTime{Hour: 12, Minute: 55},
			},
		},
		// will be removed as it's have longer delivery duration for interval 12:30-13:10
		{
			time.Date(2022, 5, 28, 9, 10, 0, 0, time.Local),
			time.Date(2022, 5, 28, 12, 15, 0, 0, time.Local),
			Interval{
				From: timex.DayTime{Hour: 12, Minute: 25},
				To:   timex.DayTime{Hour: 13, Minute: 5},
			},
		},
		// -> 12:30-13:10
		{
			time.Date(2022, 5, 28, 9, 15, 0, 0, time.Local),
			time.Date(2022, 5, 28, 12, 25, 0, 0, time.Local),
			Interval{
				From: timex.DayTime{Hour: 12, Minute: 25},
				To:   timex.DayTime{Hour: 13, Minute: 5},
			},
		},
	}
	var sPaths SPaths
	for _, t := range times {
		pb := NewPathBuilder(withEndTime(t.endTime), WithInterval(t.interval))
		pb.AddWarehouse(pb.MakeCutoffService(pb.WithStartTime(t.startTime)))
		sPaths = append(sPaths, pb.GetSortablePath())
	}
	resultSPaths, resultPathWithInterval := ChooseBestCourierPathsForExpress(
		context.Background(),
		false,
		sPaths,
	)

	wantTimes := []struct {
		startTime time.Time
		endTime   time.Time
		interval  Interval
	}{
		{
			time.Date(2022, 5, 28, 8, 55, 0, 0, time.Local),
			time.Date(2022, 5, 28, 12, 5, 0, 0, time.Local),
			Interval{
				From: timex.DayTime{Hour: 12, Minute: 5},
				To:   timex.DayTime{Hour: 12, Minute: 45},
			},
		},
		{
			time.Date(2022, 5, 28, 9, 15, 0, 0, time.Local),
			time.Date(2022, 5, 28, 12, 25, 0, 0, time.Local),
			Interval{
				From: timex.DayTime{Hour: 12, Minute: 30},
				To:   timex.DayTime{Hour: 13, Minute: 10},
			},
		},
		{
			time.Date(2022, 5, 28, 10, 0, 0, 0, time.Local),
			time.Date(2022, 5, 28, 13, 0, 0, 0, time.Local),
			Interval{
				From: timex.DayTime{Hour: 13, Minute: 0},
				To:   timex.DayTime{Hour: 13, Minute: 40},
			},
		},
		{
			time.Date(2022, 5, 28, 10, 30, 0, 0, time.Local),
			time.Date(2022, 5, 28, 13, 30, 0, 0, time.Local),
			Interval{
				From: timex.DayTime{Hour: 13, Minute: 30},
				To:   timex.DayTime{Hour: 14, Minute: 10},
			},
		},
		{
			time.Date(2022, 5, 28, 10, 30, 0, 0, time.Local),
			time.Date(2022, 5, 28, 14, 0, 0, 0, time.Local),
			Interval{
				From: timex.DayTime{Hour: 14, Minute: 0},
				To:   timex.DayTime{Hour: 14, Minute: 40},
			},
		},
		{
			time.Date(2022, 5, 28, 11, 30, 0, 0, time.Local),
			time.Date(2022, 5, 28, 14, 30, 0, 0, time.Local),
			Interval{
				From: timex.DayTime{Hour: 14, Minute: 30},
				To:   timex.DayTime{Hour: 15, Minute: 10},
			},
		},
		{
			time.Date(2022, 5, 28, 13, 15, 0, 0, time.Local),
			time.Date(2022, 5, 28, 15, 25, 0, 0, time.Local),
			Interval{
				From: timex.DayTime{Hour: 15, Minute: 30},
				To:   timex.DayTime{Hour: 16, Minute: 10},
			},
		},
	}

	var wantSPaths SPaths
	for _, t := range wantTimes {
		pb := NewPathBuilder(withEndTime(t.endTime), WithInterval(t.interval))
		pb.AddWarehouse(pb.MakeCutoffService(pb.WithStartTime(t.startTime)))
		wantSPaths = append(wantSPaths, pb.GetSortablePath())
	}

	require.Equal(t, len(wantSPaths), len(resultSPaths))
	require.Equal(t, len(wantSPaths), len(resultPathWithInterval))
	for i := range wantSPaths {
		require.Equal(t, wantSPaths[i].Interval, resultPathWithInterval[i].Interval)
		require.Equal(t, wantSPaths[i].EndTime, resultPathWithInterval[i].EndTime)
		require.Equal(t, wantSPaths[i].Interval, resultSPaths[i].Interval)
		require.Equal(t, wantSPaths[i].EndTime, resultSPaths[i].EndTime)

	}
}

// special for d-guskov
func TestMergeOrdinaryAndWideIntervals(t *testing.T) {
	firstDay, _ := time.Parse("2006-01-02", "2022-01-01")
	ordinary := []PathWithInterval{
		{SortablePath: &SortablePath{}, Tail: EndTimeAndInterval{EndTime: firstDay,
			Interval: Interval{From: timex.DayTime{Hour: 10}, To: timex.DayTime{Hour: 11}},
		}},
		{SortablePath: &SortablePath{}, Tail: EndTimeAndInterval{EndTime: firstDay,
			Interval: Interval{From: timex.DayTime{Hour: 11}, To: timex.DayTime{Hour: 12}},
		}},
		{SortablePath: &SortablePath{}, Tail: EndTimeAndInterval{EndTime: firstDay,
			Interval: Interval{From: timex.DayTime{Hour: 12}, To: timex.DayTime{Hour: 13}},
		}},
	}
	wide := []PathWithInterval{
		{SortablePath: &SortablePath{}, Tail: EndTimeAndInterval{EndTime: firstDay,
			Interval: Interval{From: timex.DayTime{Hour: 10}, To: timex.DayTime{Hour: 14}},
		}},
		{SortablePath: &SortablePath{}, Tail: EndTimeAndInterval{EndTime: firstDay,
			Interval: Interval{From: timex.DayTime{Hour: 11}, To: timex.DayTime{Hour: 15}},
		}},
		{SortablePath: &SortablePath{}, Tail: EndTimeAndInterval{EndTime: firstDay,
			Interval: Interval{From: timex.DayTime{Hour: 12}, To: timex.DayTime{Hour: 16}}},
		},
	}
	result, err := mergeOrdinaryAndWideIntervals(
		FindBestCourierPathsResult{
			SPaths:             make(SPaths, len(ordinary)),
			PathsWithIntervals: ordinary,
		},
		FindBestCourierPathsResult{
			SPaths:             make(SPaths, len(wide)),
			PathsWithIntervals: wide,
		},
	)
	require.NoError(t, err)
	wantResult := []PathWithInterval{
		{SortablePath: &SortablePath{}, Tail: EndTimeAndInterval{EndTime: firstDay,
			Interval: Interval{From: timex.DayTime{Hour: 10}, To: timex.DayTime{Hour: 11}},
		}},
		{SortablePath: &SortablePath{}, Tail: EndTimeAndInterval{EndTime: firstDay,
			Interval: Interval{From: timex.DayTime{Hour: 10}, To: timex.DayTime{Hour: 14}},
		}},
		{SortablePath: &SortablePath{}, Tail: EndTimeAndInterval{EndTime: firstDay,
			Interval: Interval{From: timex.DayTime{Hour: 11}, To: timex.DayTime{Hour: 12}},
		}},
		{SortablePath: &SortablePath{}, Tail: EndTimeAndInterval{EndTime: firstDay,
			Interval: Interval{From: timex.DayTime{Hour: 11}, To: timex.DayTime{Hour: 15}},
		}},
		{SortablePath: &SortablePath{}, Tail: EndTimeAndInterval{EndTime: firstDay,
			Interval: Interval{From: timex.DayTime{Hour: 12}, To: timex.DayTime{Hour: 13}},
		}},
		{SortablePath: &SortablePath{}, Tail: EndTimeAndInterval{EndTime: firstDay,
			Interval: Interval{From: timex.DayTime{Hour: 12}, To: timex.DayTime{Hour: 16}}},
		},
	}
	require.Equal(t, wantResult, result.PathsWithIntervals)
}
