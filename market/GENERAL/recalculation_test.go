package recalculation

import (
	"testing"

	"github.com/stretchr/testify/require"
	"google.golang.org/protobuf/types/known/durationpb"

	"a.yandex-team.ru/market/combinator/pkg/enums"
	"a.yandex-team.ru/market/combinator/pkg/graph"
	"a.yandex-team.ru/market/combinator/pkg/timex"
	cr "a.yandex-team.ru/market/combinator/proto/grpc"
)

func TestRecalculate_ParseServices(t *testing.T) {
	services := []*cr.DeliveryService{
		{
			Id:   60579565,
			Type: cr.DeliveryService_INBOUND,
			Code: "INBOUND",
			Cost: 10,
			Duration: &durationpb.Duration{
				Seconds: 6000,
				Nanos:   0,
			},
			ServiceMeta: []*cr.DeliveryService_ServiceMeta{
				{
					Key:   graph.RightBorderTag,
					Value: "1",
				},
			},
			WorkingSchedule: []*cr.DeliveryService_ScheduleDay{
				{
					DaysOfWeek: []uint32{1, 2, 3, 4, 5, 6},
					TimeWindows: []*cr.DeliveryService_TimeWindow{
						{
							StartTime: 36000,
							EndTime:   50000,
						},
					},
				},
				{
					DaysOfWeek: []uint32{0},
					TimeWindows: []*cr.DeliveryService_TimeWindow{
						{
							StartTime: 50000,
							EndTime:   50400,
						},
					},
				},
			},
		},
	}

	wantServices := []*graph.LogisticService{
		{
			ID:             60579565,
			IsActive:       true,
			UseRightBorder: true,
			Code:           enums.ServiceInbound,
			Type:           graph.ServiceTypeInbound,
			DType:          graph.DeliveryTypeCourier,
			Duration:       100,
			Price:          10,
			Schedule: &graph.Schedule{
				Windows: [7][]graph.ScheduleWindow{
					{
						{
							From: timex.DayTime{
								Hour:   13,
								Minute: 53,
								Second: 20,
							},
							To: timex.DayTime{
								Hour:   14,
								Minute: 0,
								Second: 0,
							},
						},
					},
				},
			},
		},
	}

	for i := 1; i <= 6; i++ {
		wantServices[0].Schedule.Windows[i] = append(wantServices[0].Schedule.Windows[i],
			graph.ScheduleWindow{
				From: timex.DayTime{
					Hour:   10,
					Minute: 0,
					Second: 0,
				},
				To: timex.DayTime{
					Hour:   13,
					Minute: 53,
					Second: 20,
				},
			})
	}

	r := Recalculate{DeliveryType: graph.DeliveryTypeCourier}
	parsed, err := r.ParseServices(services)
	require.NoError(t, err)
	require.Equal(t, parsed, wantServices)
}
