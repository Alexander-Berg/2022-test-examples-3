package routes

import (
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/enums"
	"a.yandex-team.ru/market/combinator/pkg/graph"
	"a.yandex-team.ru/market/combinator/pkg/timex"
	"a.yandex-team.ru/market/combinator/pkg/util"
)

func TestAddEarlyIntervals(t *testing.T) {
	createEveryDayIntervalSchedule := func(from, to []string, expectCutoff bool) *graph.Schedule {
		minLen := util.MinInt(len(from), len(to))
		s := make([]graph.DaySchedule, 7*minLen)
		for i := 0; i < 7; i++ {
			for j := 0; j < minLen; j++ {
				ds := graph.DaySchedule{
					DayFloat: float64(i + 1),
					From:     from[j],
					To:       to[j],
				}
				s[i*minLen+j] = ds
			}
		}
		schedule, _ := graph.NewSchedule(s, expectCutoff)
		return schedule
	}

	makePathWithIntevals := func(interval graph.Interval, intevalEndTime time.Time, endTime time.Time, pointLmsID int64) graph.PathWithInterval {
		return graph.PathWithInterval{
			SortablePath: &graph.SortablePath{
				Path: &graph.Path{
					Nodes: graph.Nodes{
						&graph.Node{
							LogisticSegment: graph.LogisticSegment{
								ID:           1001,
								PartnerLmsID: 2001,
								Type:         graph.SegmentTypeWarehouse,
							},
						},
						&graph.Node{
							LogisticSegment: graph.LogisticSegment{
								ID:           1002,
								PartnerLmsID: 2002,
								Type:         graph.SegmentTypeMovement,
							},
						},
						&graph.Node{
							LogisticSegment: graph.LogisticSegment{
								ID:           1003,
								PartnerLmsID: 2003,
								Type:         graph.SegmentTypeLinehaul,
							},
						},
						&graph.Node{
							LogisticSegment: graph.LogisticSegment{
								ID:           1004,
								PartnerLmsID: 2004,
								PointLmsID:   pointLmsID,
								Type:         graph.SegmentTypePickup,
							},
							CourierServices: []*graph.LogisticService{
								{
									ID:       3004,
									IsActive: true,
									Code:     enums.ServiceHanding,
									Type:     graph.ServiceTypeOutbound,
								},
							},
						},
						&graph.Node{
							LogisticSegment: graph.LogisticSegment{
								ID:           1005,
								PartnerLmsID: 2005,
								PointLmsID:   pointLmsID,
								Type:         graph.SegmentTypePickup,
							},
							CourierServices: []*graph.LogisticService{
								{
									ID:           3005,
									SegmentLmsID: 1005,
									IsActive:     true,
									Code:         enums.ServiceHanding,
									Type:         graph.ServiceTypeOutbound,
									Schedule: createEveryDayIntervalSchedule(
										[]string{"08:00:00", "09:00:00", "10:00:00", "11:00:00", "12:00:00", "13:00:00", "14:00:00", "15:00:00"},
										[]string{"09:00:00", "10:00:00", "11:00:00", "12:00:00", "13:00:00", "14:00:00", "15:00:00", "16:00:00"},
										false,
									),
								},
							},
						},
					},
					DeliveryServiceID: 342111,
					PaymentMethods:    enums.GetPaymentMethod(enums.ServicePrepayAllowed),
					ServiceTimeList:   graph.ServiceTimeList{graph.NewServiceTime(3005, intevalEndTime)},
				},
				EndTimeFrom: endTime,
				EndTimeTo:   endTime,
			},
			Tail: graph.EndTimeAndInterval{
				EndTime:  intevalEndTime,
				Interval: interval,
			},
		}
	}

	perDayIntervals := 3
	totoalIntervals := 8
	paths := make([]graph.PathWithInterval, 0)
	for pointLmsID := int64(100001); pointLmsID < 100005; pointLmsID++ {
		for i := 0; i < 5; i++ {
			for j := 0; j < perDayIntervals; j++ {
				paths = append(paths, makePathWithIntevals(
					graph.Interval{
						From: timex.DayTime{Hour: 13 + int8(j), Minute: 0, Second: 0},
						To:   timex.DayTime{Hour: 14 + int8(j), Minute: 0, Second: 0},
					},
					time.Date(2021, 11, 11+i, 13+j, 0, 0, 0, time.Local),
					time.Date(2021, 11, 11+i, 13+j, 0, 0, 0, time.Local),
					pointLmsID,
				))
			}
		}
	}

	req := make([]graph.PathWithInterval, 0)

	for pointLmsID := int64(100001); pointLmsID < 100005; pointLmsID++ {
		for j := 0; j < perDayIntervals; j++ {
			req = append(req, makePathWithIntevals(
				graph.Interval{
					From: timex.DayTime{Hour: 13 + int8(j), Minute: 0, Second: 0},
					To:   timex.DayTime{Hour: 14 + int8(j), Minute: 0, Second: 0},
				},
				time.Date(2021, 11, 11, 13+j, 0, 0, 0, time.Local),
				time.Date(2021, 11, 11, 13+j, 0, 0, 0, time.Local),
				pointLmsID,
			))
		}
		for i := 1; i < 5; i++ {
			for j := 0; j < totoalIntervals-perDayIntervals; j++ {
				req = append(req, makePathWithIntevals(
					graph.Interval{
						From: timex.DayTime{Hour: 8 + int8(j), Minute: 0, Second: 0},
						To:   timex.DayTime{Hour: 9 + int8(j), Minute: 0, Second: 0},
					},
					time.Date(2021, 11, 11+i, 13, 0, 0, 0, time.Local),
					time.Date(2021, 11, 11+i-1, 15, 0, 0, 0, time.Local),
					pointLmsID,
				))
			}
			for j := 0; j < perDayIntervals; j++ {
				req = append(req, makePathWithIntevals(
					graph.Interval{
						From: timex.DayTime{Hour: 13 + int8(j), Minute: 0, Second: 0},
						To:   timex.DayTime{Hour: 14 + int8(j), Minute: 0, Second: 0},
					},
					time.Date(2021, 11, 11+i, 13+j, 0, 0, 0, time.Local),
					time.Date(2021, 11, 11+i, 13+j, 0, 0, 0, time.Local),
					pointLmsID,
				))
			}
		}
	}
	paths = AddEarlyIntervals(paths)
	require.Len(t, paths, len(req))
	for i := 0; i < len(req); i++ {
		require.Equal(t, req[i], paths[i], i)
	}
}
