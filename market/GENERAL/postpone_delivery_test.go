package routes

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/daysoff"
	"a.yandex-team.ru/market/combinator/pkg/enums"
	"a.yandex-team.ru/market/combinator/pkg/graph"
	"a.yandex-team.ru/market/combinator/pkg/its"
	"a.yandex-team.ru/market/combinator/pkg/settings"
	"a.yandex-team.ru/market/combinator/pkg/timex"
	cr "a.yandex-team.ru/market/combinator/proto/grpc"
)

func TestIsWideInterval(t *testing.T) {
	startTime := time.Now()
	// Корнер кейсы
	{
		resIsWide, resOk := isWideInterval(nil, startTime)
		require.False(t, resIsWide)
		require.False(t, resOk)
	}
	{
		resIsWide, resOk := isWideInterval(&cr.DeliveryRoute{}, startTime)
		require.False(t, resIsWide)
		require.False(t, resOk)
	}
	// Сервису будем подменять слайс интервалов
	handingService := &cr.DeliveryService{
		Code: "HANDING",
	}
	route := &cr.DeliveryRoute{
		Route: &cr.Route{
			Points: []*cr.Route_Point{
				{
					SegmentType: "warehouse",
				},
				{
					SegmentType: "movement",
				},
				{
					SegmentType: "linehaul",
				},
				{
					SegmentType: "pickup",
				},
				{
					SegmentType: "go_platform",
					Services: []*cr.DeliveryService{
						{
							Code: "PROCESSING",
						},
						handingService,
					},
				},
			},
		},
	}
	// Сервис есть, интервала нет
	{
		resIsWide, resOk := isWideInterval(route, startTime)
		require.False(t, resIsWide)
		require.False(t, resOk)
	}
	// Узкий интервал
	{
		handingService.DeliveryIntervals = []*cr.DeliveryInterval{
			{
				From: &cr.Time{Hour: 10},
				To:   &cr.Time{Hour: 11},
			},
		}
		resIsWide, resOk := isWideInterval(route, startTime)
		require.False(t, resIsWide)
		require.True(t, resOk)
	}
	// Широкий интервал
	{
		handingService.DeliveryIntervals = []*cr.DeliveryInterval{
			{
				From: &cr.Time{Hour: 10},
				To:   &cr.Time{Hour: 20},
			},
		}
		resIsWide, resOk := isWideInterval(route, startTime)
		require.True(t, resIsWide)
		require.True(t, resOk)
	}
}

func TestPostponeDeferredDeliveryOptions(t *testing.T) {
	intervalMatchingSettings, _ := its.NewStringSettingsHolder("{\"use_deferred_wide_interval_matching\": true}")
	ctx := settings.ContextWithSettings(context.Background(), settings.New(intervalMatchingSettings.GetSettings(), ""))
	req := &cr.PostponeDeliveryRequest{
		DateTo: &cr.Date{Year: 2022, Month: 3, Day: 29},
		Interval: &cr.DeliveryInterval{
			To: &cr.Time{Hour: 10},
		},
	}
	goPlatformNodeNoSchedule := &graph.Node{
		CourierServices: []*graph.LogisticService{
			{
				Code: enums.ServiceHanding,
			},
		},
	}
	goPlatformNode := &graph.Node{
		CourierServices: []*graph.LogisticService{
			{
				ID:   666,
				Code: enums.ServiceHanding,
				Schedule: &graph.Schedule{
					Windows: [7][]graph.ScheduleWindow{
						2: {
							// Часовые слоты
							{From: timex.DayTime{Hour: 10}, To: timex.DayTime{Hour: 11}},
							{From: timex.DayTime{Hour: 11}, To: timex.DayTime{Hour: 12}},
							{From: timex.DayTime{Hour: 12}, To: timex.DayTime{Hour: 13}},
							{From: timex.DayTime{Hour: 13}, To: timex.DayTime{Hour: 14}},
							// Широкие слоты
							{From: timex.DayTime{Hour: 10}, To: timex.DayTime{Hour: 14}},
							{From: timex.DayTime{Hour: 14}, To: timex.DayTime{Hour: 20}},
							{From: timex.DayTime{Hour: 10}, To: timex.DayTime{Hour: 20}},
						},
						3: {
							// Часовые слоты
							{From: timex.DayTime{Hour: 10}, To: timex.DayTime{Hour: 11}},
							{From: timex.DayTime{Hour: 11}, To: timex.DayTime{Hour: 12}},
							// Широкие слоты
							{From: timex.DayTime{Hour: 10}, To: timex.DayTime{Hour: 20}},
						},
					},
				},
			},
		},
	}
	// Сервису будем подменять слайс интервалов
	handingService := &cr.DeliveryService{
		Code: "HANDING",
	}
	route := &cr.DeliveryRoute{
		Route: &cr.Route{
			Points: []*cr.Route_Point{
				{
					SegmentType: "warehouse",
				},
				{
					SegmentType: "movement",
				},
				{
					SegmentType: "linehaul",
				},
				{
					SegmentType: "pickup",
				},
				{
					SegmentType: "go_platform",
					Services: []*cr.DeliveryService{
						{
							Code: "PROCESSING",
						},
						handingService,
					},
				},
			},
		},
	}
	wantOptionsNarrow2 := []*cr.DeliveryOption{
		{
			Interval: &cr.DeliveryInterval{From: &cr.Time{Hour: 10}, To: &cr.Time{Hour: 11}},
		},
		{
			Interval: &cr.DeliveryInterval{From: &cr.Time{Hour: 11}, To: &cr.Time{Hour: 12}},
		},
		{
			Interval: &cr.DeliveryInterval{From: &cr.Time{Hour: 12}, To: &cr.Time{Hour: 13}},
		},
		{
			Interval: &cr.DeliveryInterval{From: &cr.Time{Hour: 13}, To: &cr.Time{Hour: 14}},
		},
	}
	wantOptionsNarrow3 := []*cr.DeliveryOption{
		{
			Interval: &cr.DeliveryInterval{From: &cr.Time{Hour: 10}, To: &cr.Time{Hour: 11}},
		},
		{
			Interval: &cr.DeliveryInterval{From: &cr.Time{Hour: 11}, To: &cr.Time{Hour: 12}},
		},
	}
	wantOptionsWide2 := []*cr.DeliveryOption{
		{
			Interval: &cr.DeliveryInterval{From: &cr.Time{Hour: 10}, To: &cr.Time{Hour: 14}},
		},
		{
			Interval: &cr.DeliveryInterval{From: &cr.Time{Hour: 14}, To: &cr.Time{Hour: 20}},
		},
		{
			Interval: &cr.DeliveryInterval{From: &cr.Time{Hour: 10}, To: &cr.Time{Hour: 20}},
		},
	}
	wantOptionsWide3 := []*cr.DeliveryOption{
		{
			Interval: &cr.DeliveryInterval{From: &cr.Time{Hour: 10}, To: &cr.Time{Hour: 20}},
		},
	}

	compareInterval := func(option1, option2 *cr.DeliveryOption) {
		require.Equal(t, option1.Interval.From.Hour, option2.Interval.From.Hour)
		require.Equal(t, option1.Interval.From.Minute, option2.Interval.From.Minute)
		require.Equal(t, option1.Interval.To.Hour, option2.Interval.To.Hour)
		require.Equal(t, option1.Interval.To.Minute, option2.Interval.To.Minute)
	}
	// Ошибка: Нет сервиса
	{
		_, err := postponeDeferredDeliveryOptions(
			ctx,
			req,
			nil,
			nil,
			route,
		)
		require.EqualError(t, err, "service is nil")
	}
	// Ошибка: Нет расписания
	{
		_, err := postponeDeferredDeliveryOptions(
			ctx,
			req,
			goPlatformNodeNoSchedule,
			nil,
			route,
		)
		require.EqualError(t, err, "schedule is nil")
	}
	// Интервал не передали - все слоты
	{
		options, err := postponeDeferredDeliveryOptions(
			ctx,
			req,
			goPlatformNode,
			nil,
			route,
		)
		require.NoError(t, err)
		var want []*cr.DeliveryOption
		want = append(want, wantOptionsNarrow2...)
		want = append(want, wantOptionsWide2...)
		want = append(want, wantOptionsNarrow3...)
		want = append(want, wantOptionsWide3...)
		require.Len(t, options, len(want))
		for i, option := range options {
			compareInterval(want[i], option)
		}
	}
	// Узкие слоты
	{
		handingService.DeliveryIntervals = []*cr.DeliveryInterval{
			{
				From: &cr.Time{Hour: 10},
				To:   &cr.Time{Hour: 11},
			},
		}
		options, err := postponeDeferredDeliveryOptions(
			ctx,
			req,
			goPlatformNode,
			nil,
			route,
		)
		require.NoError(t, err)
		var want []*cr.DeliveryOption
		want = append(want, wantOptionsNarrow2...)
		want = append(want, wantOptionsNarrow3...)
		require.Len(t, options, len(want))
		for i, option := range options {
			compareInterval(want[i], option)
		}
	}
	// Широкие слоты
	{
		handingService.DeliveryIntervals = []*cr.DeliveryInterval{
			{
				From: &cr.Time{Hour: 14},
				To:   &cr.Time{Hour: 21},
			},
		}
		options, err := postponeDeferredDeliveryOptions(
			ctx,
			req,
			goPlatformNode,
			nil,
			route,
		)
		require.NoError(t, err)
		var want []*cr.DeliveryOption
		want = append(want, wantOptionsWide2...)
		want = append(want, wantOptionsWide3...)
		require.Len(t, options, len(want))
		for i, option := range options {
			compareInterval(want[i], option)
		}
	}
	// DaysOff.IsHoliday = true. Опции только на второй день
	{
		handingService.DeliveryIntervals = []*cr.DeliveryInterval{}
		holidays := &daysoff.ServiceDaysOff{
			Services: map[int64]daysoff.DisabledDatesMap{
				666: map[int]daysoff.DisabledDate{
					88: {IsHoliday: true},
				},
			},
		}
		options, err := postponeDeferredDeliveryOptions(
			ctx,
			req,
			goPlatformNode,
			holidays,
			route,
		)
		require.NoError(t, err)
		var want []*cr.DeliveryOption
		want = append(want, wantOptionsNarrow3...)
		want = append(want, wantOptionsWide3...)
		require.Len(t, options, len(want))
		for i, option := range options {
			compareInterval(want[i], option)
		}
	}
}
