package expresslite

import (
	"time"

	"a.yandex-team.ru/market/combinator/pkg/timex"
	pb "a.yandex-team.ru/market/combinator/proto/grpc"
)

func NewIntervals(t time.Time, from, to timex.DayTime, duration, step time.Duration) []*pb.DeliveryOption {
	intervals := make([]*pb.DeliveryOption, 0)

	for from.Before(to) {
		toInterval := from.AddWithinDay(duration)

		if toInterval.Before(from) || to.Before(toInterval) {
			break
		}

		intervals = append(intervals, &pb.DeliveryOption{
			DateFrom: &pb.Date{Day: uint32(t.Day()), Month: uint32(t.Month()), Year: uint32(t.Year())},
			DateTo:   &pb.Date{Day: uint32(t.Day()), Month: uint32(t.Month()), Year: uint32(t.Year())},
			Interval: &pb.DeliveryInterval{
				From: &pb.Time{Hour: uint32(from.Hour), Minute: uint32(from.Minute)},
				To:   &pb.Time{Hour: uint32(toInterval.Hour), Minute: uint32(toInterval.Minute)},
			},
			IsWideExpress: duration == 4*time.Hour,
		})

		if len(intervals) == 1 && from.Minute != 0 && from.Minute != 30 {
			from.RoundUpTo30()
		} else {
			from = from.AddWithinDay(step)
		}
	}

	return intervals
}
