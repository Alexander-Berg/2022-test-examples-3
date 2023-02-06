package expresslite

import (
	"fmt"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	bg "a.yandex-team.ru/market/combinator/pkg/biggeneration"
	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/its"
	"a.yandex-team.ru/market/combinator/pkg/lite"
	pb "a.yandex-team.ru/market/combinator/proto/grpc"
)

func testExpressGetDeliveryRoute(
	t *testing.T,
	genData *bg.GenerationData,
	startTime time.Time,
	expressDaysCalcForward int,
	useExpressRoundTo5minutesEvolution bool,
	optionDate *pb.Date,
	optionInterval *pb.DeliveryInterval,
	wantErr *string,
) {
	settings, _ := its.NewStringSettingsHolder(
		fmt.Sprintf(
			`{"express_days_calc_forward":%d,"use_express_round_to_5minutes_evolution_v2":%t}`,
			expressDaysCalcForward,
			useExpressRoundTo5minutesEvolution,
		),
	)
	env, cancel := lite.NewEnv(t, genData, settings)
	defer cancel()
	req := lite.MakeRequest(
		lite.RequestWithStartTime(startTime),
		lite.RequestWithDeliveryType(pb.DeliveryType_COURIER),
		lite.RequestWithDeliverySubtype(pb.DeliverySubtype_ORDINARY),
		lite.RequestWithPartner(expressWarehouseID),
		lite.RequestWithRegion(geobase.RegionMoscow),
		lite.RequestWithGpsCoords(userLatitude, userLongitude),
		lite.RequestWithTotalPrice(totalPrice),
		lite.RequestWithDeliveryOption(optionDate, optionDate),
		lite.RequestWithInterval(optionInterval),
	)

	resp, err := env.Client.GetDeliveryRoute(env.Ctx, req)
	if wantErr != nil {
		require.Error(t, err)
		require.Equal(t, *wantErr, err.Error())
		return
	}
	require.NoError(t, err)
	compareDates(t, resp.GetRoute().GetDateFrom(), optionDate)
	compareDates(t, resp.GetRoute().GetDateTo(), optionDate)
	points := resp.GetRoute().GetPoints()
	require.Len(t, points, 4)
	handingSegment := points[3]
	require.Equal(t, "handing", handingSegment.GetSegmentType())
	var handingService *pb.DeliveryService
	for _, service := range handingSegment.GetServices() {
		if service.GetCode() == "HANDING" {
			handingService = service
			break
		}
	}
	require.NotNil(t, handingService.GetDeliveryIntervals())
	require.Len(t, handingService.GetDeliveryIntervals(), 1)
	compareIntervals(t, optionInterval, handingService.GetDeliveryIntervals()[0])
}

func TestExpressGetDeliveryRoute(t *testing.T) {
	makeInterval := func(hourFrom, minuteFrom, hourTo, minuteTo uint32) *pb.DeliveryInterval {
		return &pb.DeliveryInterval{
			From: &pb.Time{Hour: hourFrom, Minute: minuteFrom},
			To:   &pb.Time{Hour: hourTo, Minute: minuteTo},
		}
	}
	// Набор тестовых кейсов для генерации соответствующих расписаний сервисов графа и startTime
	testData := []struct {
		fromSchedule   string
		toSchedule     string
		startTime      time.Time
		optionDate     *pb.Date
		optionInterval *pb.DeliveryInterval
		err            []string
		errEvolution   []string
	}{
		// Круглосуточное расписание полностью заполненное интервалами
		{
			"00:00:00",
			"23:59:59",
			time.Date(2022, 1, 26, 0, 0, 0, 0, lite.MskTZ),
			&pb.Date{Year: 2022, Month: 1, Day: 26},
			makeInterval(12, 30, 13, 10),
			[]string{},
			[]string{},
		},
		// Круглосуточное расписание полностью заполненное интервалами. Поздний слот с переходом в следующие сутки COMBINATOR-3343
		{
			"00:00:00",
			"23:59:59",
			time.Date(2022, 1, 26, 20, 0, 0, 0, lite.MskTZ),
			&pb.Date{Year: 2022, Month: 1, Day: 26},
			makeInterval(23, 30, 0, 10),
			[]string{},
			[]string{},
		},
		// Круглосуточное расписание полностью заполненное интервалами. Поздний слот с 00:00 COMBINATOR-3343
		{
			"00:00:00",
			"23:59:59",
			time.Date(2022, 1, 26, 20, 0, 0, 0, lite.MskTZ),
			&pb.Date{Year: 2022, Month: 1, Day: 27},
			makeInterval(0, 0, 0, 40),
			[]string{},
			[]string{},
		},
		// Круглосуточное расписание полностью заполненное интервалами, некорректная опция(просроченный интервал)
		{
			"00:00:00",
			"23:59:59",
			time.Date(2022, 1, 26, 10, 0, 0, 0, lite.MskTZ),
			&pb.Date{Year: 2022, Month: 1, Day: 26},
			makeInterval(6, 30, 7, 10),
			[]string{
				"no courier route, t:1 i:24 ds:0",
				"no courier route, t:50 i:24 ds:0",
			},
			[]string{
				"no courier route, t:1 i:24 ds:0",
				"no courier route, t:49 i:24 ds:0",
			},
		},
		// Круглосуточное расписание полностью заполненное интервалами,
		// некорректная опция - дата для расчёта только на текущий день (express_days_calc_forward=0)
		// корректная при расчёте на +1 день (express_days_calc_forward=1)
		{
			"00:00:00",
			"23:59:59",
			time.Date(2022, 1, 26, 10, 0, 0, 0, lite.MskTZ),
			&pb.Date{Year: 2022, Month: 1, Day: 27},
			makeInterval(6, 30, 7, 10),
			[]string{
				"no courier route, t:24 i:1 ds:0",
			},
			[]string{
				"no courier route, t:24 i:1 ds:0",
			},
		},
		// Круглосуточное расписание полностью заполненное интервалами,
		// некорректная опция - дата для расчёта только на текущий день (express_days_calc_forward=0)
		// корректная при расчёте на +1 день (express_days_calc_forward=1)
		// Поздний слот с 00:00 COMBINATOR-3343
		{
			"00:00:00",
			"23:59:59",
			time.Date(2022, 1, 26, 10, 0, 0, 0, lite.MskTZ),
			&pb.Date{Year: 2022, Month: 1, Day: 28},
			makeInterval(0, 0, 0, 40),
			[]string{
				"no courier route, t:25 i:0 ds:0",
			},
			[]string{
				"no courier route, t:25 i:0 ds:0",
			},
		},
	}
	genData := &bg.GenerationData{
		RegionMap:                regionMap,
		Express:                  makeExpress(),
		TariffsFinder:            makeTariffsFinder(),
		ExpressIntervalValidator: makeExpressIntervalValidator(),
	}
	for _, test := range testData {
		// Эмулируем работу флага express_days_calc_forward, который указывает на сколько дополнительных дней вперед считать
		// COMBINATOR-1887 На какое количество дней вперёд размножать интервалы доставки экспрессом
		genData.Graph = makeExpressGraph(test.fromSchedule, test.toSchedule)
		for _, expressDaysCalcForward := range []int{0, 1} {
			var wantErr *string
			if len(test.err) >= expressDaysCalcForward+1 {
				plusDeferredErr := fmt.Sprintf(
					"rpc error: code = Unknown desc = deferred route error: DEFERRED_COURIER_DISABLED ordinary route error: %s ",
					test.err[expressDaysCalcForward],
				)
				wantErr = &plusDeferredErr
			}
			testExpressGetDeliveryRoute(
				t,
				genData,
				test.startTime,
				expressDaysCalcForward,
				false, // use_express_round_to_5minutes_evolution_v2
				test.optionDate,
				test.optionInterval,
				wantErr,
			)
		}

		// С эволюцией
		// Эмулируем работу флага express_days_calc_forward, который указывает на сколько дополнительных дней вперед считать
		// COMBINATOR-1887 На какое количество дней вперёд размножать интервалы доставки экспрессом
		for _, expressDaysCalcForward := range []int{0, 1} {
			var wantErr *string
			if len(test.errEvolution) >= expressDaysCalcForward+1 {
				plusDeferredErr := fmt.Sprintf(
					"rpc error: code = Unknown desc = deferred route error: DEFERRED_COURIER_DISABLED ordinary route error: %s ",
					test.errEvolution[expressDaysCalcForward],
				)
				wantErr = &plusDeferredErr
			}
			testExpressGetDeliveryRoute(
				t,
				genData,
				test.startTime,
				expressDaysCalcForward,
				true, // use_express_round_to_5minutes_evolution_v2
				test.optionDate,
				test.optionInterval,
				wantErr,
			)
		}
	}
}
