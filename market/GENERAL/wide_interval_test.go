// Логика сортировок:
// Относится ко всем сплитам:
// 	- Если первая короткая опция успевает в рамках 2 часов с момента заказа
//	  	то в meta-информации сервиса CALL_COURIER будет тег "FASTEST_EXPRESS",
//    	и в опции будет флаг IsFastestExpress.
//	- Если экспресс заказ создан на широкий слот и
//		правая граница за N минут до закрытия склада или меньше,
//		то в meta-информации сервиса CALL_COURIER будет тег "WAREHOUSE_CLOSES_SOON",
//		(settings.Settings.MinutesBeforeExpressCloseForWide)
//	- Широкие интервалы заканчиваются за 1.5 часа до закрытия склада
//	- Первый широкий интервал имеет такую же левую границу как короткий ( 10:20 - 11:40, 10:20 - 14:20 )
//		исключение - когда короткая отсутствует из-за капасити.
//
//
// Сплит 1
// 	- Первая опция всегда коротка
//	- В случае если влезают широкие
// 		- После первой опции идут широкие
//		- После широких НЕТ коротких на этот день
//		- Широкие идут с шагом 30 минут ( то есть пересекаются )
//		Пример: Запрос в 09:00 | Склад работает 09:00 - 17:00 | Processing: 30 min, CallCourier: 50 min
//			- 10:20 - 11:00 [ordinary] isFastestExpress
//			- 10:20 - 14:20 [wide]
//			- 10:30 - 14:30 [wide]
//			- 11:00 - 15:00 [wide]
//			- 11:30 - 15:30 [wide] - тут будет тег closes_soon
//
//	- В случае если не влезают широкие
//		- До конца этого дня будут добавлены обычные слоты
//		Пример: Запрос в 09:00 | Склад работает 09:00 - 14:00 | Processing: 30 min, CallCourier: 50 min
//			- 10:20 - 11:00 [ordinary] isFastestExpress
//			- 10:30 - 11:10 [ordinary]
//			- 11:00 - 11:40 [ordinary]
//			- 11:30 - 12:10 [ordinary]
//			- 12:00 - 12:40 [ordinary]
//
//	- В случае если первый короткий слот отлетел из-за капасити
//		- Широкие слоты до следующего первого отлетают вместе с ним
//		Пример: Запрос в 09:00 | Склад работает 09:00 - 17:00 | Processing: 30 min, CallCourier: 50 min
//			слоты на 10 часов отлетели по капасити
//			- 11:00 - 11:40 [ordinary]
//			- 11:00 - 15:00 [wide]
//			- 11:30 - 15:30 [wide] - тут будет тег closes_soon
//
// Сплит 2
// 	- Первая опция коротка, но исключение если короткий слот отлетел по капасити
//	- В случае если влезают широкие
// 		- После первой опции идет ДО 2 широких слотов
//		- Широкие идут с шагом 4 часа ( то есть НЕ пересекаются )
// 		- После широких идут все оставшиеся короткие ( после 1 короткого )
//		Пример: Запрос в 09:00 | Склад работает 09:00 - 17:00 | Processing: 30 min, CallCourier: 50 min
//			- 10:20 - 11:00 [ordinary]  isFastestExpress
//			- 10:20 - 14:20 [wide]
//			- 10:30 - 11:10 [ordinary]
//			- 11:00 - 11:40 [ordinary]
//			- 11:30 - 12:10 [ordinary]
//			- 12:00 - 12:40 [ordinary]
//			- 12:30 - 13:10 [ordinary]
//			- 13:00 - 13:40 [ordinary]
//			- 13:30 - 14:10 [ordinary]
//			- 14:00 - 14:40 [ordinary]
//			- 15:30 - 15:10 [ordinary]
//			- 16:00 - 16:40 [ordinary]
//
//	- В случае если не влезают широкие
//		- До конца этого дня будут добавлены обычные слоты
//		Пример: Запрос в 09:00 | Склад работает 09:00 - 14:00 | Processing: 30 min, CallCourier: 50 min
//			- 10:20 - 11:00 [ordinary]  isFastestExpress
//			- 10:30 - 11:10 [ordinary]
//			- 11:00 - 11:40 [ordinary]
//			- 11:30 - 12:10 [ordinary]
//			- 12:00 - 12:40 [ordinary]
//			- 12:30 - 13:10 [ordinary]
//			- 13:00 - 13:40 [ordinary]
//
//	- В случае если первый короткий слот отлетел из-за капасити
//		- Первым может стать широкий слот если он начинается раньше
//		- Широкие слоты не отлетают по капасити
//		Пример: Запрос в 09:00 | Склад работает 09:00 - 17:00 | Processing: 30 min, CallCourier: 50 min
//			слоты на 10 часов отлетели по капасити
//			- 10:20 - 14:20 [wide]
//			- 11:00 - 11:40 [ordinary]
//			- 11:30 - 12:10 [ordinary]
//			- 12:00 - 12:40 [ordinary]
//			- 12:30 - 13:10 [ordinary]
//			- 13:00 - 13:40 [ordinary]
//			- 13:30 - 14:10 [ordinary]
//			- 14:00 - 14:40 [ordinary]
//			- 15:30 - 15:10 [ordinary]
//			- 16:00 - 16:40 [ordinary]
//
//
package expresslite

import (
	"fmt"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	bg "a.yandex-team.ru/market/combinator/pkg/biggeneration"
	"a.yandex-team.ru/market/combinator/pkg/enums"
	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/graph"
	"a.yandex-team.ru/market/combinator/pkg/its"
	"a.yandex-team.ru/market/combinator/pkg/lite"
	"a.yandex-team.ru/market/combinator/pkg/timex"
	pb "a.yandex-team.ru/market/combinator/proto/grpc"
)

func createEnv(t *testing.T, untilTheEndDay bool) (env *lite.Env, cancel func()) {
	scheduleFrom := &pb.Time{Hour: 10, Minute: 00}
	scheduleTo := &pb.Time{Hour: 20, Minute: 00}

	if untilTheEndDay {
		scheduleTo = &pb.Time{Hour: 23, Minute: 59}
	}

	g := makeExpressGraph(
		fmt.Sprintf("%02d:%02d:00", scheduleFrom.Hour, scheduleFrom.Minute),
		fmt.Sprintf("%02d:%02d:00", scheduleTo.Hour, scheduleTo.Minute),
	)

	generation := &bg.GenerationData{
		RegionMap:                regionMap,
		Graph:                    g,
		Express:                  makeExpress(),
		TariffsFinder:            makeTariffsFinder(),
		ExpressIntervalValidator: makeExpressIntervalValidator(),
	}

	settings, _ := its.NewStringSettingsHolder(`{"express_days_calc_forward": 1}`)
	return lite.NewEnv(t, generation, settings)
}

// TestWideIntervalSplitFirstBeforeClose тест на отлов паники
func TestWideIntervalSplitFirstBeforeClose(t *testing.T) {
	env, cancel := createEnv(t, true)
	defer cancel()

	startTime := time.Date(2022, 1, 27, 23, 55, 0, 0, lite.MskTZ)
	req := newWideExpressRequest(startTime, 1)

	resp, err := env.Client.GetCourierOptions(env.Ctx, req)
	require.NoError(t, err)
	require.NotNil(t, resp)
}

func TestWideIntervalSplitSecondWithCapacity(t *testing.T) {
	{
		env, cancel := createEnv(t, false)
		defer cancel()

		// Запрос 26.02 02:00
		startTime := time.Date(2022, 2, 26, 2, 0, 0, 0, lite.MskTZ)
		req := newWideExpressRequest(startTime, 2)

		resp, err := env.Client.GetCourierOptions(env.Ctx, req)
		require.NoError(t, err)
		require.NotNil(t, resp)

		// Интервалы должны быть следующими
		//	26.2 10:20 - 14:20 IsWideExpress
		//	26.2 14:30 - 18:30 IsWideExpress
		opts := NewIntervals(startTime,
			timex.DayTime{Hour: 10, Minute: 20}, timex.DayTime{Hour: 14, Minute: 20},
			4*time.Hour, 4*time.Hour,
		)
		opts = append(opts, NewIntervals(startTime,
			timex.DayTime{Hour: 14, Minute: 30}, timex.DayTime{Hour: 18, Minute: 30},
			4*time.Hour, 4*time.Hour,
		)...)

		//	26.2 11:30 - 12:10
		//	26.2 12:00 - 12:40
		//	26.2 12:30 - 13:10
		//	26.2 13:00 - 13:40
		//	26.2 13:30 - 14:10
		//	26.2 14:00 - 14:40
		//	26.2 14:30 - 15:10
		//	26.2 15:00 - 15:40
		//	26.2 15:30 - 16:10
		//	26.2 16:00 - 16:40
		//	26.2 16:30 - 17:10
		//	26.2 17:00 - 17:40
		//	26.2 17:30 - 18:10
		//	26.2 18:00 - 18:40
		//	26.2 18:30 - 19:10
		//	26.2 19:00 - 19:40
		opts = append(opts, NewIntervals(startTime,
			timex.DayTime{Hour: 11, Minute: 30}, timex.DayTime{Hour: 19, Minute: 40},
			40*time.Minute, 30*time.Minute,
		)...)

		//	27.2 10:30 - 14:30 IsWideExpress
		//	27.2 14:30 - 18:30 IsWideExpress
		opts = append(opts, NewIntervals(startTime.Add(24*time.Hour),
			timex.DayTime{Hour: 10, Minute: 30}, timex.DayTime{Hour: 18, Minute: 30},
			4*time.Hour, 4*time.Hour,
		)...)

		//	27.2 11:30 - 12:10
		//	27.2 12:00 - 12:40
		//	27.2 12:30 - 13:10
		//	27.2 13:00 - 13:40
		//	27.2 13:30 - 14:10
		//	27.2 14:00 - 14:40
		//	27.2 14:30 - 15:10
		//	27.2 15:00 - 15:40
		//	27.2 15:30 - 16:10
		//	27.2 16:00 - 16:40
		//	27.2 16:30 - 17:10
		//	27.2 17:00 - 17:40
		//	27.2 17:30 - 18:10
		//	27.2 18:00 - 18:40
		//	27.2 18:30 - 19:10
		//	27.2 19:00 - 19:40
		opts = append(opts, NewIntervals(startTime.Add(24*time.Hour),
			timex.DayTime{Hour: 11, Minute: 30}, timex.DayTime{Hour: 19, Minute: 40},
			40*time.Minute, 30*time.Minute,
		)...)
		require.NotNil(t, opts)
		require.Equal(t, len(opts), len(resp.GetOptions()))
		for i, option := range resp.GetOptions() {
			require.Equal(t, opts[i].Interval, option.Interval,
				fmt.Sprintf("[%d] need %d:%d - %d:%d has %d:%d - %d:%d", i,
					opts[i].Interval.From.Hour, opts[i].Interval.From.Minute,
					opts[i].Interval.To.Hour, opts[i].Interval.To.Minute,
					option.Interval.From.Hour, option.Interval.From.Minute,
					option.Interval.To.Hour, option.Interval.To.Minute,
				))
			require.Equal(t, opts[i].DateFrom, option.DateFrom,
				fmt.Sprintf("[%d] need %d.%d.%d has %d.%d.%d", i,
					opts[i].DateFrom.Day, opts[i].DateFrom.Month, opts[i].DateFrom.Year,
					option.DateFrom.Day, option.DateFrom.Month, option.DateFrom.Year,
				))
			require.Equal(t, opts[i].IsWideExpress, option.IsWideExpress,
				fmt.Sprintf("[%d] need IsWideExpress %v has %v", i,
					opts[i].IsWideExpress,
					option.IsWideExpress,
				))
			require.Equal(t, opts[i].IsFastestExpress, option.IsFastestExpress,
				fmt.Sprintf("[%d] need IsFastestExpress %v has %v", i,
					opts[i].IsFastestExpress,
					option.IsFastestExpress,
				))

			reqRoute := makeDeliveryRouteReq(startTime, option.Interval, option.DateTo,
				lite.RequestWithRearrFactors(fmt.Sprintf("enable_wide_express_courier_options=%d", 2)),
			)
			respRoute, err := env.Client.GetDeliveryRoute(env.Ctx, reqRoute)
			require.NoError(t, err)
			containsMeta(
				t,
				respRoute.Route.Points,
				enums.ServiceCallCourier,
				option.IsWideExpress,
				graph.IsWideExpressTag,
				"1",
			)
			containsMeta(
				t,
				respRoute.Route.Points,
				enums.ServiceCallCourier,
				option.IsFastestExpress,
				graph.IsFastestExpressTag,
				"1",
			)
		}
	}

	{
		env, cancel := createEnv(t, false)
		defer cancel()

		// Запрос 24.02 02:00
		startTime := time.Date(2022, 2, 24, 2, 0, 0, 0, lite.MskTZ)
		req := newWideExpressRequest(startTime, 2)

		resp, err := env.Client.GetCourierOptions(env.Ctx, req)
		require.NoError(t, err)
		require.NotNil(t, resp)

		// Интервалы должны быть следующими
		//	24.2 10:20 - 14:20 IsWideExpress
		//	24.2 14:30 - 18:30 IsWideExpress
		opts := NewIntervals(startTime,
			timex.DayTime{Hour: 10, Minute: 20}, timex.DayTime{Hour: 14, Minute: 20},
			4*time.Hour, 4*time.Hour,
		)
		opts = append(opts, NewIntervals(startTime,
			timex.DayTime{Hour: 14, Minute: 30}, timex.DayTime{Hour: 18, Minute: 30},
			4*time.Hour, 4*time.Hour,
		)...)

		//	24.2 15:30 - 16:10
		//	24.2 16:00 - 16:40
		//	24.2 16:30 - 17:10
		//	24.2 17:00 - 17:40
		//	24.2 17:30 - 18:10
		//	24.2 18:00 - 18:40
		//	24.2 18:30 - 19:10
		//	24.2 19:00 - 19:40
		opts = append(opts, NewIntervals(startTime,
			timex.DayTime{Hour: 15, Minute: 30}, timex.DayTime{Hour: 19, Minute: 40},
			40*time.Minute, 30*time.Minute,
		)...)

		//	25.2 10:30 - 11:10
		opts = append(opts, NewIntervals(startTime.Add(24*time.Hour),
			timex.DayTime{Hour: 10, Minute: 30}, timex.DayTime{Hour: 11, Minute: 10},
			40*time.Minute, 30*time.Minute,
		)...)

		//	25.2 10:30 - 14:30 IsWideExpress
		//	25.2 14:30 - 18:30 IsWideExpress
		opts = append(opts, NewIntervals(startTime.Add(24*time.Hour),
			timex.DayTime{Hour: 10, Minute: 30}, timex.DayTime{Hour: 18, Minute: 30},
			4*time.Hour, 4*time.Hour,
		)...)

		//	25.2 11:00 - 11:40
		//	25.2 11:30 - 12:10
		//	25.2 12:00 - 12:40
		//	25.2 12:30 - 13:10
		//	25.2 13:00 - 13:40
		//	25.2 13:30 - 14:10
		//	25.2 14:00 - 14:40
		//	25.2 14:30 - 15:10
		//	25.2 15:00 - 15:40
		//	25.2 15:30 - 16:10
		//	25.2 16:00 - 16:40
		//	25.2 16:30 - 17:10
		//	25.2 17:00 - 17:40
		//	25.2 17:30 - 18:10
		//	25.2 18:00 - 18:40
		//	25.2 18:30 - 19:10
		//	25.2 19:00 - 19:40
		opts = append(opts, NewIntervals(startTime.Add(24*time.Hour),
			timex.DayTime{Hour: 11, Minute: 00}, timex.DayTime{Hour: 19, Minute: 40},
			40*time.Minute, 30*time.Minute,
		)...)

		require.NotNil(t, opts)
		require.Equal(t, len(opts), len(resp.GetOptions()))
		for i, option := range resp.GetOptions() {
			require.Equal(t, opts[i].Interval, option.Interval,
				fmt.Sprintf("[%d] need %d:%d - %d:%d has %d:%d - %d:%d", i,
					opts[i].Interval.From.Hour, opts[i].Interval.From.Minute,
					opts[i].Interval.To.Hour, opts[i].Interval.To.Minute,
					option.Interval.From.Hour, option.Interval.From.Minute,
					option.Interval.To.Hour, option.Interval.To.Minute,
				))
			require.Equal(t, opts[i].DateFrom, option.DateFrom,
				fmt.Sprintf("[%d] need %d.%d.%d has %d.%d.%d", i,
					opts[i].DateFrom.Day, opts[i].DateFrom.Month, opts[i].DateFrom.Year,
					option.DateFrom.Day, option.DateFrom.Month, option.DateFrom.Year,
				))
			require.Equal(t, opts[i].IsWideExpress, option.IsWideExpress,
				fmt.Sprintf("[%d] need IsWideExpress %v has %v", i,
					opts[i].IsWideExpress,
					option.IsWideExpress,
				))
			require.Equal(t, opts[i].IsFastestExpress, option.IsFastestExpress,
				fmt.Sprintf("[%d] need IsFastestExpress %v has %v", i,
					opts[i].IsFastestExpress,
					option.IsFastestExpress,
				))

			reqRoute := makeDeliveryRouteReq(startTime, option.Interval, option.DateTo,
				lite.RequestWithRearrFactors(fmt.Sprintf("enable_wide_express_courier_options=%d", 2)),
			)
			respRoute, err := env.Client.GetDeliveryRoute(env.Ctx, reqRoute)
			require.NoError(t, err)
			containsMeta(
				t,
				respRoute.Route.Points,
				enums.ServiceCallCourier,
				option.IsWideExpress,
				graph.IsWideExpressTag,
				"1",
			)
			containsMeta(
				t,
				respRoute.Route.Points,
				enums.ServiceCallCourier,
				option.IsFastestExpress,
				graph.IsFastestExpressTag,
				"1",
			)
		}
	}
}

func TestWideSplitFirstIntervalWithCapacity(t *testing.T) {
	{
		env, cancel := createEnv(t, false)
		defer cancel()

		// Запрос 26.02 02:00
		startTime := time.Date(2022, 2, 26, 2, 0, 0, 0, lite.MskTZ)
		req := newWideExpressRequest(startTime, 1)

		resp, err := env.Client.GetCourierOptions(env.Ctx, req)
		require.NoError(t, err)
		require.NotNil(t, resp)

		// Интервалы должны быть следующими
		//	26.2 11:30 - 12:10
		opts := NewIntervals(startTime,
			timex.DayTime{Hour: 11, Minute: 30}, timex.DayTime{Hour: 12, Minute: 10},
			40*time.Minute, 30*time.Minute,
		)

		//	26.2 11:30 - 15:30 IsWideExpress
		//	26.2 12:00 - 16:00 IsWideExpress
		//	26.2 12:30 - 16:30 IsWideExpress
		//	26.2 13:00 - 17:00 IsWideExpress
		//	26.2 13:30 - 17:30 IsWideExpress
		//	26.2 14:00 - 18:00 IsWideExpress
		//	26.2 14:30 - 18:30 IsWideExpress
		opts = append(opts, NewIntervals(startTime,
			timex.DayTime{Hour: 11, Minute: 30}, timex.DayTime{Hour: 18, Minute: 30},
			4*time.Hour, 30*time.Minute,
		)...)

		//	27.2 11:30 - 12:10
		opts = append(opts, NewIntervals(startTime.Add(24*time.Hour),
			timex.DayTime{Hour: 11, Minute: 30}, timex.DayTime{Hour: 12, Minute: 10},
			40*time.Minute, 30*time.Minute,
		)...)

		//	27.2 11:30 - 15:30 IsWideExpress
		//	27.2 12:00 - 16:00 IsWideExpress
		//	27.2 12:30 - 16:30 IsWideExpress
		//	27.2 13:00 - 17:00 IsWideExpress
		//	27.2 13:30 - 17:30 IsWideExpress
		//	27.2 14:00 - 18:00 IsWideExpress
		//	27.2 14:30 - 18:30 IsWideExpress
		opts = append(opts, NewIntervals(startTime.Add(24*time.Hour),
			timex.DayTime{Hour: 11, Minute: 30}, timex.DayTime{Hour: 18, Minute: 30},
			4*time.Hour, 30*time.Minute,
		)...)
		require.NotNil(t, opts)
		require.Equal(t, len(opts), len(resp.GetOptions()))
		for i, option := range resp.GetOptions() {
			require.Equal(t, opts[i].Interval, option.Interval,
				fmt.Sprintf("[%d] need %d:%d - %d:%d has %d:%d - %d:%d", i,
					opts[i].Interval.From.Hour, opts[i].Interval.From.Minute,
					opts[i].Interval.To.Hour, opts[i].Interval.To.Minute,
					option.Interval.From.Hour, option.Interval.From.Minute,
					option.Interval.To.Hour, option.Interval.To.Minute,
				))
			require.Equal(t, opts[i].DateFrom, option.DateFrom,
				fmt.Sprintf("[%d] need %d.%d.%d has %d.%d.%d", i,
					opts[i].DateFrom.Day, opts[i].DateFrom.Month, opts[i].DateFrom.Year,
					option.DateFrom.Day, option.DateFrom.Month, option.DateFrom.Year,
				))
			require.Equal(t, opts[i].IsWideExpress, option.IsWideExpress,
				fmt.Sprintf("[%d] need IsWideExpress %v has %v", i,
					opts[i].IsWideExpress,
					option.IsWideExpress,
				))
			require.Equal(t, opts[i].IsFastestExpress, option.IsFastestExpress,
				fmt.Sprintf("[%d] need IsFastestExpress %v has %v", i,
					opts[i].IsFastestExpress,
					option.IsFastestExpress,
				))

			reqRoute := makeDeliveryRouteReq(startTime, option.Interval, option.DateTo,
				lite.RequestWithRearrFactors(fmt.Sprintf("enable_wide_express_courier_options=%d", 1)),
			)
			respRoute, err := env.Client.GetDeliveryRoute(env.Ctx, reqRoute)
			require.NoError(t, err)
			containsMeta(
				t,
				respRoute.Route.Points,
				enums.ServiceCallCourier,
				option.IsWideExpress,
				graph.IsWideExpressTag,
				"1",
			)
			containsMeta(
				t,
				respRoute.Route.Points,
				enums.ServiceCallCourier,
				option.IsFastestExpress,
				graph.IsFastestExpressTag,
				"1",
			)
		}
	}

	{
		env, cancel := createEnv(t, false)
		defer cancel()

		// Запрос 24.02 02:00
		startTime := time.Date(2022, 2, 24, 2, 0, 0, 0, lite.MskTZ)
		req := newWideExpressRequest(startTime, 1)

		resp, err := env.Client.GetCourierOptions(env.Ctx, req)
		require.NoError(t, err)
		require.NotNil(t, resp)

		// Интервалы должны быть следующими
		//	24.2 15:30 - 16:10
		//	24.2 16:00 - 16:40
		//	24.2 16:30 - 17:10
		//	24.2 17:00 - 17:40
		//	24.2 17:30 - 18:10
		//	24.2 18:00 - 18:40
		//	24.2 18:30 - 19:10
		//	24.2 19:00 - 19:40
		opts := NewIntervals(startTime,
			timex.DayTime{Hour: 15, Minute: 30}, timex.DayTime{Hour: 19, Minute: 40},
			40*time.Minute, 30*time.Minute,
		)
		//	25.2 10:30 - 11:10
		opts = append(opts, NewIntervals(startTime.Add(24*time.Hour),
			timex.DayTime{Hour: 10, Minute: 30}, timex.DayTime{Hour: 11, Minute: 10},
			40*time.Minute, 30*time.Minute,
		)...)
		//	25.2 10:30 - 14:30 IsWideExpress
		//	25.2 11:00 - 15:00 IsWideExpress
		//	25.2 11:30 - 15:30 IsWideExpress
		//	25.2 12:00 - 16:00 IsWideExpress
		//	25.2 12:30 - 16:30 IsWideExpress
		//	25.2 13:00 - 17:00 IsWideExpress
		//	25.2 13:30 - 17:30 IsWideExpress
		//	25.2 14:00 - 18:00 IsWideExpress
		//	25.2 14:30 - 18:30 IsWideExpress
		opts = append(opts, NewIntervals(startTime.Add(24*time.Hour),
			timex.DayTime{Hour: 10, Minute: 30}, timex.DayTime{Hour: 18, Minute: 30},
			4*time.Hour, 30*time.Minute,
		)...)
		require.NotNil(t, opts)
		require.Equal(t, len(opts), len(resp.GetOptions()))
		for i, option := range resp.GetOptions() {
			require.Equal(t, opts[i].Interval, option.Interval,
				fmt.Sprintf("[%d] need %d:%d - %d:%d has %d:%d - %d:%d", i,
					opts[i].Interval.From.Hour, opts[i].Interval.From.Minute,
					opts[i].Interval.To.Hour, opts[i].Interval.To.Minute,
					option.Interval.From.Hour, option.Interval.From.Minute,
					option.Interval.To.Hour, option.Interval.To.Minute,
				))
			require.Equal(t, opts[i].DateFrom, option.DateFrom,
				fmt.Sprintf("[%d] need %d.%d.%d has %d.%d.%d", i,
					opts[i].DateFrom.Day, opts[i].DateFrom.Month, opts[i].DateFrom.Year,
					option.DateFrom.Day, option.DateFrom.Month, option.DateFrom.Year,
				))
			require.Equal(t, opts[i].IsWideExpress, option.IsWideExpress,
				fmt.Sprintf("[%d] need IsWideExpress %v has %v", i,
					opts[i].IsWideExpress,
					option.IsWideExpress,
				))
			require.Equal(t, opts[i].IsFastestExpress, option.IsFastestExpress,
				fmt.Sprintf("[%d] need IsFastestExpress %v has %v", i,
					opts[i].IsFastestExpress,
					option.IsFastestExpress,
				))

			reqRoute := makeDeliveryRouteReq(startTime, option.Interval, option.DateTo,
				lite.RequestWithRearrFactors(fmt.Sprintf("enable_wide_express_courier_options=%d", 1)),
			)
			respRoute, err := env.Client.GetDeliveryRoute(env.Ctx, reqRoute)
			require.NoError(t, err)
			containsMeta(
				t,
				respRoute.Route.Points,
				enums.ServiceCallCourier,
				option.IsWideExpress,
				graph.IsWideExpressTag,
				"1",
			)
			containsMeta(
				t,
				respRoute.Route.Points,
				enums.ServiceCallCourier,
				option.IsFastestExpress,
				graph.IsFastestExpressTag,
				"1",
			)
		}
	}
}

func TestWideIntervalSplitFirstNight(t *testing.T) {
	env, cancel := createEnv(t, false)
	defer cancel()

	// Запрос 27.01 02:00
	startTime := time.Date(2022, 1, 27, 2, 0, 0, 0, lite.MskTZ)
	req := newWideExpressRequest(startTime, 1)

	resp, err := env.Client.GetCourierOptions(env.Ctx, req)
	require.NoError(t, err)
	require.NotNil(t, resp)

	// Интервалы должны быть следующими
	//	27.1 10:20 - 11:00
	opts := NewIntervals(startTime,
		timex.DayTime{Hour: 10, Minute: 20}, timex.DayTime{Hour: 11, Minute: 00},
		40*time.Minute, 30*time.Minute,
	)
	//	27.1 10:20 - 14:20 IsWideExpress
	//	27.1 10:30 - 14:30 IsWideExpress
	//	27.1 11:00 - 15:00 IsWideExpress
	//	27.1 11:30 - 15:30 IsWideExpress
	//	27.1 12:00 - 16:00 IsWideExpress
	//	27.1 12:30 - 16:30 IsWideExpress
	//	27.1 13:00 - 17:00 IsWideExpress
	//	27.1 13:30 - 17:30 IsWideExpress
	//	27.1 14:00 - 18:00 IsWideExpress
	//	27.1 14:30 - 18:30 IsWideExpress
	opts = append(opts, NewIntervals(startTime,
		timex.DayTime{Hour: 10, Minute: 20}, timex.DayTime{Hour: 18, Minute: 30},
		4*time.Hour, 30*time.Minute,
	)...)
	//	28.1 10:30 - 11:10
	opts = append(opts, NewIntervals(startTime.Add(24*time.Hour),
		timex.DayTime{Hour: 10, Minute: 30}, timex.DayTime{Hour: 11, Minute: 10},
		40*time.Minute, 30*time.Minute,
	)...)
	//	28.1 10:30 - 14:30 IsWideExpress
	//	28.1 11:00 - 15:00 IsWideExpress
	//	28.1 11:30 - 15:30 IsWideExpress
	//	28.1 12:00 - 16:00 IsWideExpress
	//	28.1 12:30 - 16:30 IsWideExpress
	//	28.1 13:00 - 17:00 IsWideExpress
	//	28.1 13:30 - 17:30 IsWideExpress
	//	28.1 14:00 - 18:00 IsWideExpress
	//	28.1 14:30 - 18:30 IsWideExpress
	opts = append(opts, NewIntervals(startTime.Add(24*time.Hour),
		timex.DayTime{Hour: 10, Minute: 30}, timex.DayTime{Hour: 18, Minute: 30},
		4*time.Hour, 30*time.Minute,
	)...)
	require.NotNil(t, opts)
	require.Equal(t, len(opts), len(resp.GetOptions()))
	for i, option := range resp.GetOptions() {
		require.Equal(t, opts[i].Interval, option.Interval,
			fmt.Sprintf("[%d] need %d:%d - %d:%d has %d:%d - %d:%d", i,
				opts[i].Interval.From.Hour, opts[i].Interval.From.Minute,
				opts[i].Interval.To.Hour, opts[i].Interval.To.Minute,
				option.Interval.From.Hour, option.Interval.From.Minute,
				option.Interval.To.Hour, option.Interval.To.Minute,
			))
		require.Equal(t, opts[i].DateFrom, option.DateFrom,
			fmt.Sprintf("[%d] need %d.%d.%d has %d.%d.%d", i,
				opts[i].DateFrom.Day, opts[i].DateFrom.Month, opts[i].DateFrom.Year,
				option.DateFrom.Day, option.DateFrom.Month, option.DateFrom.Year,
			))
		require.Equal(t, opts[i].IsWideExpress, option.IsWideExpress,
			fmt.Sprintf("[%d] need IsWideExpress %v has %v", i,
				opts[i].IsWideExpress,
				option.IsWideExpress,
			))
		require.Equal(t, opts[i].IsFastestExpress, option.IsFastestExpress,
			fmt.Sprintf("[%d] need IsFastestExpress %v has %v", i,
				opts[i].IsFastestExpress,
				option.IsFastestExpress,
			))

		reqRoute := makeDeliveryRouteReq(startTime, option.Interval, option.DateTo,
			lite.RequestWithRearrFactors(fmt.Sprintf("enable_wide_express_courier_options=%d", 1)),
		)
		respRoute, err := env.Client.GetDeliveryRoute(env.Ctx, reqRoute)
		require.NoError(t, err)
		containsMeta(
			t,
			respRoute.Route.Points,
			enums.ServiceCallCourier,
			option.IsWideExpress,
			graph.IsWideExpressTag,
			"1",
		)
		containsMeta(
			t,
			respRoute.Route.Points,
			enums.ServiceCallCourier,
			option.IsFastestExpress,
			graph.IsFastestExpressTag,
			"1",
		)
	}
}

func TestWideIntervalSplitFirstDay(t *testing.T) {
	env, cancel := createEnv(t, false)
	defer cancel()

	// Запрос 27.01 17:00
	startTime := time.Date(2022, 1, 27, 17, 0, 0, 0, lite.MskTZ)
	req := newWideExpressRequest(startTime, 1)

	resp, err := env.Client.GetCourierOptions(env.Ctx, req)
	require.NoError(t, err)
	require.NotNil(t, resp)

	// Интервалы должны быть следующими
	//	27.1 17:20 - 18:00 IsFastestExpress
	//	27.1 17:30 - 18:10
	//	27.1 18:00 - 18:40
	//  27.1 18:30 - 19:10 express_fit_intervals_to_wh_schedule = true
	//  27.1 19:00 - 19:40 express_fit_intervals_to_wh_schedule = true
	opts := NewIntervals(startTime,
		timex.DayTime{Hour: 17, Minute: 20}, timex.DayTime{Hour: 19, Minute: 40},
		40*time.Minute, 30*time.Minute,
	)
	opts[0].IsFastestExpress = true

	//	28.1 10:30 - 11:10
	opts = append(opts, NewIntervals(startTime.Add(24*time.Hour),
		timex.DayTime{Hour: 10, Minute: 30}, timex.DayTime{Hour: 11, Minute: 10},
		40*time.Minute, 30*time.Minute,
	)...)

	//	28.1 10:30 - 14:30 IsWideExpress
	//	28.1 11:00 - 15:00 IsWideExpress
	//	28.1 11:30 - 15:30 IsWideExpress
	//	28.1 12:00 - 16:00 IsWideExpress
	//	28.1 12:30 - 16:30 IsWideExpress
	//	28.1 13:00 - 17:00 IsWideExpress
	//	28.1 13:30 - 17:30 IsWideExpress
	//	28.1 14:00 - 18:00 IsWideExpress
	//	28.1 14:30 - 18:30 IsWideExpress
	opts = append(opts, NewIntervals(startTime.Add(24*time.Hour),
		timex.DayTime{Hour: 10, Minute: 30}, timex.DayTime{Hour: 18, Minute: 30},
		4*time.Hour, 30*time.Minute,
	)...)
	require.NotNil(t, opts)
	require.Equal(t, len(opts), len(resp.GetOptions()))
	for i, option := range resp.GetOptions() {
		require.Equal(t, opts[i].Interval, option.Interval,
			fmt.Sprintf("[%d] need %d:%d - %d:%d has %d:%d - %d:%d", i,
				opts[i].Interval.From.Hour, opts[i].Interval.From.Minute,
				opts[i].Interval.To.Hour, opts[i].Interval.To.Minute,
				option.Interval.From.Hour, option.Interval.From.Minute,
				option.Interval.To.Hour, option.Interval.To.Minute,
			))
		require.Equal(t, opts[i].DateFrom, option.DateFrom,
			fmt.Sprintf("[%d] need %d.%d.%d has %d.%d.%d", i,
				opts[i].DateFrom.Day, opts[i].DateFrom.Month, opts[i].DateFrom.Year,
				option.DateFrom.Day, option.DateFrom.Month, option.DateFrom.Year,
			))
		require.Equal(t, opts[i].IsWideExpress, option.IsWideExpress,
			fmt.Sprintf("[%d] need IsWideExpress %v has %v", i,
				opts[i].IsWideExpress,
				option.IsWideExpress,
			))
		require.Equal(t, opts[i].IsFastestExpress, option.IsFastestExpress,
			fmt.Sprintf("[%d] need IsFastestExpress %v has %v", i,
				opts[i].IsFastestExpress,
				option.IsFastestExpress,
			))

		reqRoute := makeDeliveryRouteReq(startTime, option.Interval, option.DateTo,
			lite.RequestWithRearrFactors(fmt.Sprintf("enable_wide_express_courier_options=%d", 1)),
		)
		respRoute, err := env.Client.GetDeliveryRoute(env.Ctx, reqRoute)
		require.NoError(t, err)
		containsMeta(
			t,
			respRoute.Route.Points,
			enums.ServiceCallCourier,
			option.IsWideExpress,
			graph.IsWideExpressTag,
			"1",
		)
		containsMeta(
			t,
			respRoute.Route.Points,
			enums.ServiceCallCourier,
			option.IsFastestExpress,
			graph.IsFastestExpressTag,
			"1",
		)
	}
}

func TestWideIntervalSplitFirstEvening(t *testing.T) {
	env, cancel := createEnv(t, false)
	defer cancel()

	// Запрос 27.01 20:00
	startTime := time.Date(2022, 1, 27, 20, 0, 0, 0, lite.MskTZ)
	req := newWideExpressRequest(startTime, 1)

	resp, err := env.Client.GetCourierOptions(env.Ctx, req)
	require.NoError(t, err)
	require.NotNil(t, resp)

	// Интервалы должны быть следующими
	//	28.1 10:20 - 11:00
	opts := NewIntervals(startTime.Add(24*time.Hour),
		timex.DayTime{Hour: 10, Minute: 20}, timex.DayTime{Hour: 11, Minute: 00},
		40*time.Minute, 30*time.Minute,
	)

	//	28.1 10:20 - 14:20 IsWideExpress
	//	28.1 10:30 - 14:30 IsWideExpress
	//	28.1 11:00 - 15:00 IsWideExpress
	//	28.1 11:30 - 15:30 IsWideExpress
	//	28.1 12:00 - 16:00 IsWideExpress
	//	28.1 12:30 - 16:30 IsWideExpress
	//	28.1 13:00 - 17:00 IsWideExpress
	//	28.1 13:30 - 17:30 IsWideExpress
	//	28.1 14:00 - 18:00 IsWideExpress
	//	28.1 14:30 - 18:30 IsWideExpress
	opts = append(opts, NewIntervals(startTime.Add(24*time.Hour),
		timex.DayTime{Hour: 10, Minute: 20}, timex.DayTime{Hour: 18, Minute: 30},
		4*time.Hour, 30*time.Minute,
	)...)

	//	29.1 10:30 - 11:10
	opts = append(opts, NewIntervals(startTime.Add(48*time.Hour),
		timex.DayTime{Hour: 10, Minute: 30}, timex.DayTime{Hour: 11, Minute: 10},
		40*time.Minute, 30*time.Minute,
	)...)

	//	29.1 10:30 - 14:30 IsWideExpress
	//	29.1 11:00 - 15:00 IsWideExpress
	//	29.1 11:30 - 15:30 IsWideExpress
	//	29.1 12:00 - 16:00 IsWideExpress
	//	29.1 12:30 - 16:30 IsWideExpress
	//	29.1 13:00 - 17:00 IsWideExpress
	//	29.1 13:30 - 17:30 IsWideExpress
	//	29.1 14:00 - 18:00 IsWideExpress
	//	29.1 14:30 - 18:30 IsWideExpress
	opts = append(opts, NewIntervals(startTime.Add(48*time.Hour),
		timex.DayTime{Hour: 10, Minute: 30}, timex.DayTime{Hour: 18, Minute: 30},
		4*time.Hour, 30*time.Minute,
	)...)
	require.NotNil(t, opts)
	require.Equal(t, len(opts), len(resp.GetOptions()))
	for i, option := range resp.GetOptions() {
		require.Equal(t, opts[i].Interval, option.Interval,
			fmt.Sprintf("[%d] need %d:%d - %d:%d has %d:%d - %d:%d", i,
				opts[i].Interval.From.Hour, opts[i].Interval.From.Minute,
				opts[i].Interval.To.Hour, opts[i].Interval.To.Minute,
				option.Interval.From.Hour, option.Interval.From.Minute,
				option.Interval.To.Hour, option.Interval.To.Minute,
			))
		require.Equal(t, opts[i].DateFrom, option.DateFrom,
			fmt.Sprintf("[%d] need %d.%d.%d has %d.%d.%d", i,
				opts[i].DateFrom.Day, opts[i].DateFrom.Month, opts[i].DateFrom.Year,
				option.DateFrom.Day, option.DateFrom.Month, option.DateFrom.Year,
			))
		require.Equal(t, opts[i].IsWideExpress, option.IsWideExpress,
			fmt.Sprintf("[%d] need IsWideExpress %v has %v", i,
				opts[i].IsWideExpress,
				option.IsWideExpress,
			))
		require.Equal(t, opts[i].IsFastestExpress, option.IsFastestExpress,
			fmt.Sprintf("[%d] need IsFastestExpress %v has %v", i,
				opts[i].IsFastestExpress,
				option.IsFastestExpress,
			))

		reqRoute := makeDeliveryRouteReq(startTime, option.Interval, option.DateTo,
			lite.RequestWithRearrFactors(fmt.Sprintf("enable_wide_express_courier_options=%d", 1)),
		)
		respRoute, err := env.Client.GetDeliveryRoute(env.Ctx, reqRoute)
		require.NoError(t, err)
		containsMeta(
			t,
			respRoute.Route.Points,
			enums.ServiceCallCourier,
			option.IsWideExpress,
			graph.IsWideExpressTag,
			"1",
		)
		containsMeta(
			t,
			respRoute.Route.Points,
			enums.ServiceCallCourier,
			option.IsFastestExpress,
			graph.IsFastestExpressTag,
			"1",
		)
	}
}

func TestWideIntervalSplitSecondNight(t *testing.T) {
	env, cancel := createEnv(t, false)
	defer cancel()

	// Запрос 27.01 02:00
	startTime := time.Date(2022, 1, 27, 2, 0, 0, 0, lite.MskTZ)
	req := newWideExpressRequest(startTime, 2)

	resp, err := env.Client.GetCourierOptions(env.Ctx, req)
	require.NoError(t, err)
	require.NotNil(t, resp)

	// Интервалы должны быть следующими
	//	27.1 10:20 - 11:00
	opts := NewIntervals(startTime,
		timex.DayTime{Hour: 10, Minute: 20}, timex.DayTime{Hour: 11, Minute: 00},
		40*time.Minute, 30*time.Minute,
	)

	//	27.1 10:20 - 14:20 IsWideExpress
	//	27.1 14:30 - 18:30 IsWideExpress
	opts = append(opts, NewIntervals(startTime,
		timex.DayTime{Hour: 10, Minute: 20}, timex.DayTime{Hour: 14, Minute: 20},
		4*time.Hour, 4*time.Hour,
	)...)
	opts = append(opts, NewIntervals(startTime,
		timex.DayTime{Hour: 14, Minute: 30}, timex.DayTime{Hour: 18, Minute: 30},
		4*time.Hour, 4*time.Hour,
	)...)

	//	27.1 10:30 - 11:10
	//	27.1 11:00 - 11:40
	//	27.1 11:30 - 12:10
	//	27.1 12:00 - 12:40
	//	27.1 12:30 - 13:10
	//	27.1 13:00 - 13:40
	//	27.1 13:30 - 14:10
	//	27.1 14:00 - 14:40
	//	27.1 14:30 - 15:10
	//	27.1 15:00 - 15:40
	//	27.1 15:30 - 16:10
	//	27.1 16:00 - 16:40
	//	27.1 16:30 - 17:10
	//	27.1 17:00 - 17:40
	//	27.1 17:30 - 18:10
	//	27.1 18:00 - 18:40
	//	27.1 18:30 - 19:10
	//	27.1 19:00 - 19:40
	opts = append(opts, NewIntervals(startTime,
		timex.DayTime{Hour: 10, Minute: 30}, timex.DayTime{Hour: 19, Minute: 40},
		40*time.Minute, 30*time.Minute,
	)...)

	//	28.1 10:30 - 11:10
	opts = append(opts, NewIntervals(startTime.Add(24*time.Hour),
		timex.DayTime{Hour: 10, Minute: 30}, timex.DayTime{Hour: 11, Minute: 10},
		40*time.Minute, 30*time.Minute,
	)...)

	//	28.1 10:30 - 14:30 IsWideExpress
	//	28.1 14:30 - 18:30 IsWideExpress
	opts = append(opts, NewIntervals(startTime.Add(24*time.Hour),
		timex.DayTime{Hour: 10, Minute: 30}, timex.DayTime{Hour: 18, Minute: 30},
		4*time.Hour, 4*time.Hour,
	)...)

	//	28.1 11:00 - 11:40
	//	28.1 11:30 - 12:10
	//	28.1 12:00 - 12:40
	//	28.1 12:30 - 13:10
	//	28.1 13:00 - 13:40
	//	28.1 13:30 - 14:10
	//	28.1 14:00 - 14:40
	//	28.1 14:30 - 15:10
	//	28.1 15:00 - 15:40
	//	28.1 15:30 - 16:10
	//	28.1 16:00 - 16:40
	//	28.1 16:30 - 17:10
	//	28.1 17:00 - 17:40
	//	28.1 17:30 - 18:10
	//	28.1 18:00 - 18:40
	//	28.1 18:30 - 19:10
	//	28.1 19:00 - 19:40
	opts = append(opts, NewIntervals(startTime.Add(24*time.Hour),
		timex.DayTime{Hour: 11, Minute: 00}, timex.DayTime{Hour: 19, Minute: 40},
		40*time.Minute, 30*time.Minute,
	)...)
	require.NotNil(t, opts)
	require.Equal(t, len(opts), len(resp.GetOptions()))
	for i, option := range resp.GetOptions() {
		require.Equal(t, opts[i].Interval, option.Interval,
			fmt.Sprintf("[%d] need %d:%d - %d:%d has %d:%d - %d:%d", i,
				opts[i].Interval.From.Hour, opts[i].Interval.From.Minute,
				opts[i].Interval.To.Hour, opts[i].Interval.To.Minute,
				option.Interval.From.Hour, option.Interval.From.Minute,
				option.Interval.To.Hour, option.Interval.To.Minute,
			))
		require.Equal(t, opts[i].DateFrom, option.DateFrom,
			fmt.Sprintf("[%d] need %d.%d.%d has %d.%d.%d", i,
				opts[i].DateFrom.Day, opts[i].DateFrom.Month, opts[i].DateFrom.Year,
				option.DateFrom.Day, option.DateFrom.Month, option.DateFrom.Year,
			))
		require.Equal(t, opts[i].IsWideExpress, option.IsWideExpress,
			fmt.Sprintf("[%d] need IsWideExpress %v has %v", i,
				opts[i].IsWideExpress,
				option.IsWideExpress,
			))
		require.Equal(t, opts[i].IsFastestExpress, option.IsFastestExpress,
			fmt.Sprintf("[%d] need IsFastestExpress %v has %v", i,
				opts[i].IsFastestExpress,
				option.IsFastestExpress,
			))

		reqRoute := makeDeliveryRouteReq(startTime, option.Interval, option.DateTo,
			lite.RequestWithRearrFactors(fmt.Sprintf("enable_wide_express_courier_options=%d", 2)),
		)
		respRoute, err := env.Client.GetDeliveryRoute(env.Ctx, reqRoute)
		require.NoError(t, err)
		containsMeta(
			t,
			respRoute.Route.Points,
			enums.ServiceCallCourier,
			option.IsWideExpress,
			graph.IsWideExpressTag,
			"1",
		)
		containsMeta(
			t,
			respRoute.Route.Points,
			enums.ServiceCallCourier,
			option.IsFastestExpress,
			graph.IsFastestExpressTag,
			"1",
		)
	}
}

func TestWideIntervalSplitSecondDay(t *testing.T) {
	env, cancel := createEnv(t, false)
	defer cancel()

	// Запрос 27.01 17:00
	startTime := time.Date(2022, 1, 27, 17, 0, 0, 0, lite.MskTZ)
	req := newWideExpressRequest(startTime, 2)

	resp, err := env.Client.GetCourierOptions(env.Ctx, req)
	require.NoError(t, err)
	require.NotNil(t, resp)

	// Интервалы должны быть следующими
	//	27.1 17:20 - 18:00 IsFastestExpress
	//	27.1 17:30 - 18:10
	//	27.1 18:00 - 18:40
	//  27.1 18:30 - 19:10 express_fit_intervals_to_wh_schedule = true
	//  27.1 19:00 - 19:40 express_fit_intervals_to_wh_schedule = true
	opts := NewIntervals(startTime,
		timex.DayTime{Hour: 17, Minute: 20}, timex.DayTime{Hour: 19, Minute: 40},
		40*time.Minute, 30*time.Minute,
	)
	opts[0].IsFastestExpress = true

	//	28.1 10:30 - 11:10
	opts = append(opts, NewIntervals(startTime.Add(24*time.Hour),
		timex.DayTime{Hour: 10, Minute: 30}, timex.DayTime{Hour: 11, Minute: 10},
		40*time.Minute, 30*time.Minute,
	)...)
	//	28.1 10:30 - 14:30 IsWideExpress
	//	28.1 14:30 - 18:30 IsWideExpress
	opts = append(opts, NewIntervals(startTime.Add(24*time.Hour),
		timex.DayTime{Hour: 10, Minute: 30}, timex.DayTime{Hour: 18, Minute: 30},
		4*time.Hour, 4*time.Hour,
	)...)

	//	28.1 11:00 - 11:40
	//	28.1 11:30 - 12:10
	//	28.1 12:00 - 12:40
	//	28.1 12:30 - 13:10
	//	28.1 13:00 - 13:40
	//	28.1 13:30 - 14:10
	//	28.1 14:00 - 14:40
	//	28.1 14:30 - 15:10
	//	28.1 15:00 - 15:40
	//	28.1 15:30 - 16:10
	//	28.1 16:00 - 16:40
	//	28.1 16:30 - 17:10
	//	28.1 17:00 - 17:40
	//	28.1 17:30 - 18:10
	//	28.1 18:00 - 18:40
	//  28.1 18:30 - 19:10 express_fit_intervals_to_wh_schedule = true
	//  28.1 19:00 - 19:40 express_fit_intervals_to_wh_schedule = true
	opts = append(opts, NewIntervals(startTime.Add(24*time.Hour),
		timex.DayTime{Hour: 11, Minute: 0}, timex.DayTime{Hour: 19, Minute: 40},
		40*time.Minute, 30*time.Minute,
	)...)
	require.NotNil(t, opts)
	require.Equal(t, len(opts), len(resp.GetOptions()))
	for i, option := range resp.GetOptions() {
		require.Equal(t, opts[i].Interval, option.Interval,
			fmt.Sprintf("[%d] need %d:%d - %d:%d has %d:%d - %d:%d", i,
				opts[i].Interval.From.Hour, opts[i].Interval.From.Minute,
				opts[i].Interval.To.Hour, opts[i].Interval.To.Minute,
				option.Interval.From.Hour, option.Interval.From.Minute,
				option.Interval.To.Hour, option.Interval.To.Minute,
			))
		require.Equal(t, opts[i].DateFrom, option.DateFrom,
			fmt.Sprintf("[%d] need %d.%d.%d has %d.%d.%d", i,
				opts[i].DateFrom.Day, opts[i].DateFrom.Month, opts[i].DateFrom.Year,
				option.DateFrom.Day, option.DateFrom.Month, option.DateFrom.Year,
			))
		require.Equal(t, opts[i].IsWideExpress, option.IsWideExpress,
			fmt.Sprintf("[%d] need IsWideExpress %v has %v", i,
				opts[i].IsWideExpress,
				option.IsWideExpress,
			))
		require.Equal(t, opts[i].IsFastestExpress, option.IsFastestExpress,
			fmt.Sprintf("[%d] need IsFastestExpress %v has %v", i,
				opts[i].IsFastestExpress,
				option.IsFastestExpress,
			))

		reqRoute := makeDeliveryRouteReq(startTime, option.Interval, option.DateTo,
			lite.RequestWithRearrFactors(fmt.Sprintf("enable_wide_express_courier_options=%d", 2)),
		)
		respRoute, err := env.Client.GetDeliveryRoute(env.Ctx, reqRoute)
		require.NoError(t, err)
		containsMeta(
			t,
			respRoute.Route.Points,
			enums.ServiceCallCourier,
			option.IsWideExpress,
			graph.IsWideExpressTag,
			"1",
		)
		containsMeta(
			t,
			respRoute.Route.Points,
			enums.ServiceCallCourier,
			option.IsFastestExpress,
			graph.IsFastestExpressTag,
			"1",
		)
	}
}

func TestWideIntervalSplitSecondEvening(t *testing.T) {
	env, cancel := createEnv(t, false)
	defer cancel()

	// Запрос 27.01 20:00
	startTime := time.Date(2022, 1, 27, 20, 0, 0, 0, lite.MskTZ)
	req := newWideExpressRequest(startTime, 2)

	resp, err := env.Client.GetCourierOptions(env.Ctx, req)
	require.NoError(t, err)
	require.NotNil(t, resp)

	// Интервалы должны быть следующими
	//	28.1 10:20 - 11:00
	opts := NewIntervals(startTime.Add(24*time.Hour),
		timex.DayTime{Hour: 10, Minute: 20}, timex.DayTime{Hour: 11, Minute: 00},
		40*time.Minute, 30*time.Minute,
	)

	//	28.1 10:20 - 14:20 IsWideExpress
	//	28.1 14:20 - 18:20 IsWideExpress
	opts = append(opts, NewIntervals(startTime.Add(24*time.Hour),
		timex.DayTime{Hour: 10, Minute: 20}, timex.DayTime{Hour: 14, Minute: 20},
		4*time.Hour, 4*time.Hour,
	)...)
	opts = append(opts, NewIntervals(startTime.Add(24*time.Hour),
		timex.DayTime{Hour: 14, Minute: 30}, timex.DayTime{Hour: 18, Minute: 30},
		4*time.Hour, 4*time.Hour,
	)...)

	//	28.1 10:30 - 11:10
	//	28.1 11:00 - 11:40
	//	28.1 11:30 - 12:10
	//	28.1 12:00 - 12:40
	//	28.1 12:30 - 13:10
	//	28.1 13:00 - 13:40
	//	28.1 13:30 - 14:10
	//	28.1 14:00 - 14:40
	//	28.1 14:30 - 15:10
	//	28.1 15:00 - 15:40
	//	28.1 15:30 - 16:10
	//	28.1 16:00 - 16:40
	//	28.1 16:30 - 17:10
	//	28.1 17:00 - 17:40
	//	28.1 17:30 - 18:10
	//	28.1 18:00 - 18:40
	//  28.1 18:30 - 19:10 express_fit_intervals_to_wh_schedule = true
	//  28.1 19:00 - 19:40 express_fit_intervals_to_wh_schedule = true
	opts = append(opts, NewIntervals(startTime.Add(24*time.Hour),
		timex.DayTime{Hour: 10, Minute: 30}, timex.DayTime{Hour: 19, Minute: 40},
		40*time.Minute, 30*time.Minute,
	)...)

	//	29.1 10:30 - 11:00
	opts = append(opts, NewIntervals(startTime.Add(48*time.Hour),
		timex.DayTime{Hour: 10, Minute: 30}, timex.DayTime{Hour: 11, Minute: 10},
		40*time.Minute, 30*time.Minute,
	)...)

	//	29.1 10:30 - 14:30 IsWideExpress
	//	29.1 14:30 - 18:30 IsWideExpress
	opts = append(opts, NewIntervals(startTime.Add(48*time.Hour),
		timex.DayTime{Hour: 10, Minute: 30}, timex.DayTime{Hour: 18, Minute: 30},
		4*time.Hour, 4*time.Hour,
	)...)
	//	29.1 11:00 - 11:40
	//	29.1 11:30 - 12:10
	//	29.1 12:00 - 12:40
	//	29.1 12:30 - 13:10
	//	29.1 13:00 - 13:40
	//	29.1 13:30 - 14:10
	//	29.1 14:00 - 14:40
	//	29.1 14:30 - 15:10
	//	29.1 15:00 - 15:40
	//	29.1 15:30 - 16:10
	//	29.1 16:00 - 16:40
	//	29.1 16:30 - 17:10
	//	29.1 17:00 - 17:40
	//	29.1 17:30 - 18:10
	//	29.1 18:00 - 18:40
	//  29.1 18:30 - 19:10 express_fit_intervals_to_wh_schedule = true
	//  29.1 19:00 - 19:40 express_fit_intervals_to_wh_schedule = true
	opts = append(opts, NewIntervals(startTime.Add(48*time.Hour),
		timex.DayTime{Hour: 11, Minute: 0}, timex.DayTime{Hour: 19, Minute: 40},
		40*time.Minute, 30*time.Minute,
	)...)
	require.NotNil(t, opts)
	require.Equal(t, len(opts), len(resp.GetOptions()))
	for i, option := range resp.GetOptions() {
		require.Equal(t, opts[i].Interval, option.Interval,
			fmt.Sprintf("[%d] need %d:%d - %d:%d has %d:%d - %d:%d", i,
				opts[i].Interval.From.Hour, opts[i].Interval.From.Minute,
				opts[i].Interval.To.Hour, opts[i].Interval.To.Minute,
				option.Interval.From.Hour, option.Interval.From.Minute,
				option.Interval.To.Hour, option.Interval.To.Minute,
			))
		require.Equal(t, opts[i].DateFrom, option.DateFrom,
			fmt.Sprintf("[%d] need %d.%d.%d has %d.%d.%d", i,
				opts[i].DateFrom.Day, opts[i].DateFrom.Month, opts[i].DateFrom.Year,
				option.DateFrom.Day, option.DateFrom.Month, option.DateFrom.Year,
			))
		require.Equal(t, opts[i].IsWideExpress, option.IsWideExpress,
			fmt.Sprintf("[%d] need IsWideExpress %v has %v", i,
				opts[i].IsWideExpress,
				option.IsWideExpress,
			))
		require.Equal(t, opts[i].IsFastestExpress, option.IsFastestExpress,
			fmt.Sprintf("[%d] need IsFastestExpress %v has %v", i,
				opts[i].IsFastestExpress,
				option.IsFastestExpress,
			))

		reqRoute := makeDeliveryRouteReq(startTime, option.Interval, option.DateTo,
			lite.RequestWithRearrFactors(fmt.Sprintf("enable_wide_express_courier_options=%d", 2)),
		)
		respRoute, err := env.Client.GetDeliveryRoute(env.Ctx, reqRoute)
		require.NoError(t, err)
		containsMeta(
			t,
			respRoute.Route.Points,
			enums.ServiceCallCourier,
			option.IsWideExpress,
			graph.IsWideExpressTag,
			"1",
		)
		containsMeta(
			t,
			respRoute.Route.Points,
			enums.ServiceCallCourier,
			option.IsFastestExpress,
			graph.IsFastestExpressTag,
			"1",
		)
	}
}

func newWideExpressRequest(startTime time.Time, split int) *pb.DeliveryRequest {
	return lite.MakeRequest(
		lite.RequestWithStartTime(startTime),
		lite.RequestWithDeliveryType(pb.DeliveryType_COURIER),
		lite.RequestWithPartner(expressWarehouseID),
		lite.RequestWithRegion(geobase.RegionMoscow),
		lite.RequestWithGpsCoords(userLatitude, userLongitude),
		lite.RequestWithTotalPrice(totalPrice),
		lite.RequestWithShopID(0),
		lite.RequestWithRearrFactors(fmt.Sprintf("enable_wide_express_courier_options=%d", split)),
	)
}
