package graph

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/daysoff"
	"a.yandex-team.ru/market/combinator/pkg/timex"
)

func TestCalcExpressStartTimes(t *testing.T) {
	{
		schedule, _ := newSimpleSchedule(CreateSimpleSchedule())
		startTime := time.Date(2021, 7, 20, 10, 11, 12, 0, time.UTC)
		endTime := time.Date(2021, 7, 20, 19, 40, 0, 0, time.UTC)
		processingDuration := 10
		callCourierDuration := 15
		pb := NewPathBuilder(
			withEndTime(
				startTime,
			),
		)
		pb.AddWarehouse(
			pb.WithPartnerTypeDropship(),
			pb.MakeCutoffService(
				pb.WithStartTime(startTime),
				pb.WithServiceID(-100),
				pb.WithSchedule(*schedule),
			),
			pb.MakeProcessingService(
				pb.WithStartTime(startTime),
				pb.WithDuration(int32(processingDuration)),
				pb.WithServiceID(-101),
				pb.WithSchedule(*schedule),
			),
			pb.MakeShipmentService(
				pb.WithStartTime(startTime),
				pb.WithServiceID(-102),
				pb.WithSchedule(*schedule),
			),
		)
		pb.AddMovement(
			pb.MakeMovementService(
				pb.WithStartTime(startTime),
				pb.WithServiceID(-103),
				pb.WithSchedule(*schedule),
			),
			pb.MakeShipmentService(
				pb.WithStartTime(startTime),
				pb.WithServiceID(-104),
				pb.WithSchedule(*schedule),
			),
		)
		pb.AddLinehaul(
			pb.MakeDeliveryService(
				pb.WithStartTime(startTime),
				pb.WithServiceID(-105),
				pb.WithSchedule(*schedule),
			),
			pb.MakeLastMileService(
				pb.WithStartTime(startTime),
				pb.WithServiceID(-106),
				pb.WithSchedule(*schedule),
			),
		)
		pb.AddHanding(
			pb.MakeCallCourierService(
				pb.WithStartTime(startTime),
				pb.WithDuration(int32(callCourierDuration)),
				pb.WithServiceID(-107),
				pb.WithSchedule(*schedule),
			),
			pb.MakeHandingService(
				pb.WithStartTime(startTime),
				pb.WithServiceID(-108),
				pb.WithSchedule(*schedule),
			),
			pb.WithLocation(225),
		)
		path := pb.GetSortablePath()
		err := path.CalcExpressStartTimes(
			context.Background(),
			Interval{
				From: timex.NewDayTimeFromTime(endTime),
				To:   timex.NewDayTimeFromTime(endTime.Add(40 * time.Minute)),
			},
			daysoff.NewServicesHashed(),
		)
		require.NoError(t, err)
		endTime = endTime.Add(time.Duration(callCourierDuration) * time.Minute)
		wantServiceTimeList := ServiceTimeList{
			NewServiceTime(
				-100,
				endTime.Add(-1*time.Duration(callCourierDuration+processingDuration)*time.Minute),
			),
			NewServiceTime(
				-101,
				endTime.Add(-1*time.Duration(callCourierDuration+processingDuration)*time.Minute),
			),
			NewServiceTime(
				-102,
				endTime.Add(-1*time.Duration(callCourierDuration)*time.Minute),
			),
			NewServiceTime(
				-103,
				endTime.Add(-1*time.Duration(callCourierDuration)*time.Minute),
			),
			NewServiceTime(
				-104,
				endTime.Add(-1*time.Duration(callCourierDuration)*time.Minute),
			),
			NewServiceTime(
				-105,
				endTime.Add(-1*time.Duration(callCourierDuration)*time.Minute),
			),
			NewServiceTime(
				-106,
				endTime.Add(-1*time.Duration(callCourierDuration)*time.Minute),
			),
			NewServiceTime(
				-107,
				endTime.Add(-1*time.Duration(callCourierDuration)*time.Minute),
			),
			NewServiceTime(
				-108,
				endTime,
			),
		}
		require.Equal(t, wantServiceTimeList, path.ServiceTimeList)
	}
	// Переходной этап. Сервис Call Courier есть и в movement и в handing сегментах. Должен учитываться только в movement, если есть
	// COMBINATOR-2576
	{
		schedule, _ := newSimpleSchedule(CreateSimpleSchedule())
		startTime := time.Date(2021, 7, 20, 10, 11, 12, 0, time.UTC)
		endTime := time.Date(2021, 7, 20, 19, 40, 0, 0, time.UTC)
		processingDuration := 10
		callCourierDuration := 15
		pb := NewPathBuilder(
			withEndTime(
				startTime,
			),
		)
		pb.AddWarehouse(
			pb.WithPartnerTypeDropship(),
			pb.MakeCutoffService(
				pb.WithStartTime(startTime),
				pb.WithServiceID(-100),
				pb.WithSchedule(*schedule),
			),
			pb.MakeProcessingService(
				pb.WithStartTime(startTime),
				pb.WithDuration(int32(processingDuration)),
				pb.WithServiceID(-101),
				pb.WithSchedule(*schedule),
			),
			pb.MakeShipmentService(
				pb.WithStartTime(startTime),
				pb.WithServiceID(-102),
				pb.WithSchedule(*schedule),
			),
		)
		pb.AddMovement(
			pb.MakeMovementService(
				pb.WithStartTime(startTime),
				pb.WithServiceID(-103),
				pb.WithSchedule(*schedule),
			),
			pb.MakeCallCourierService(
				pb.WithStartTime(startTime),
				pb.WithDuration(int32(callCourierDuration)),
				pb.WithServiceID(-104),
				pb.WithSchedule(*schedule),
			),
			pb.MakeShipmentService(
				pb.WithStartTime(startTime),
				pb.WithServiceID(-105),
				pb.WithSchedule(*schedule),
			),
		)
		pb.AddLinehaul(
			pb.MakeDeliveryService(
				pb.WithStartTime(startTime),
				pb.WithServiceID(-106),
				pb.WithSchedule(*schedule),
			),
			pb.MakeLastMileService(
				pb.WithStartTime(startTime),
				pb.WithServiceID(-107),
				pb.WithSchedule(*schedule),
			),
		)
		pb.AddHanding(
			pb.MakeCallCourierService(
				pb.WithStartTime(startTime),
				pb.WithDuration(int32(callCourierDuration)),
				pb.WithServiceID(-108),
				pb.WithSchedule(*schedule),
			),
			pb.MakeHandingService(
				pb.WithStartTime(startTime),
				pb.WithServiceID(-109),
				pb.WithSchedule(*schedule),
			),
			pb.WithLocation(225),
		)
		path := pb.GetSortablePath()
		err := path.CalcExpressStartTimes(
			context.Background(),
			Interval{
				From: timex.NewDayTimeFromTime(endTime),
				To:   timex.NewDayTimeFromTime(endTime.Add(40 * time.Minute)),
			},
			daysoff.NewServicesHashed(),
		)
		require.NoError(t, err)
		endTime = endTime.Add(time.Duration(callCourierDuration) * time.Minute)
		wantServiceTimeList := ServiceTimeList{
			NewServiceTime(
				-100,
				endTime.Add(-1*time.Duration(callCourierDuration+processingDuration)*time.Minute),
			),
			NewServiceTime(
				-101,
				endTime.Add(-1*time.Duration(callCourierDuration+processingDuration)*time.Minute),
			),
			NewServiceTime(
				-102,
				endTime.Add(-1*time.Duration(callCourierDuration)*time.Minute),
			),
			NewServiceTime(
				-103,
				endTime.Add(-1*time.Duration(callCourierDuration)*time.Minute),
			),
			NewServiceTime(
				-104,
				endTime.Add(-1*time.Duration(callCourierDuration)*time.Minute),
			),
			NewServiceTime(
				-105,
				endTime,
			),
			NewServiceTime(
				-106,
				endTime,
			),
			NewServiceTime(
				-107,
				endTime,
			),
			NewServiceTime(
				-108,
				endTime,
			),
			NewServiceTime(
				-109,
				endTime,
			),
		}
		require.Equal(t, wantServiceTimeList, path.ServiceTimeList)
	}
}
