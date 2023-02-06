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
	"a.yandex-team.ru/market/combinator/pkg/timex"
	pb "a.yandex-team.ru/market/combinator/proto/grpc"
)

func makeOption(year int, month time.Month, day, fromHour, fromMinute, toHour, toMinute int) *pb.DeliveryOption {
	if toHour == 0 && toMinute == 0 {
		toHour = 23
		toMinute = 59
	}
	return &pb.DeliveryOption{
		DateFrom: &pb.Date{Year: uint32(year), Month: uint32(month), Day: uint32(day)},
		DateTo:   &pb.Date{Year: uint32(year), Month: uint32(month), Day: uint32(day)},
		Interval: &pb.DeliveryInterval{
			From: &pb.Time{Hour: uint32(fromHour), Minute: uint32(fromMinute)},
			To:   &pb.Time{Hour: uint32(toHour), Minute: uint32(toMinute)},
		},
	}
}

func addFirstInterval(
	startTime time.Time,
	options []*pb.DeliveryOption,
	customAddTimeToDelivery int,
) []*pb.DeliveryOption {
	year, month, day := startTime.Date()
	fromHour, fromMinute, _ := startTime.Clock()
	toHour, toMinute, _ := startTime.Add(time.Duration(customAddTimeToDelivery) * time.Minute).Clock()
	return append(options, makeOption(year, month, day, fromHour, fromMinute, toHour, toMinute))
}

func fillIntervals(
	startTime time.Time,
	limitTime time.Time,
	options []*pb.DeliveryOption,
	customAddTimeToDelivery int,
	isTrivial bool,
) []*pb.DeliveryOption {
	for {
		year, month, day := startTime.Date()
		fromHour, fromMinute, _ := startTime.Clock()
		intervalTo := startTime.Add(time.Duration(customAddTimeToDelivery) * time.Minute)
		if (!isTrivial && limitTime.Before(intervalTo)) ||
			(isTrivial && limitTime.Before(startTime)) {
			break
		}
		toHour, toMinute, _ := intervalTo.Clock()
		options = append(options, makeOption(year, month, day, fromHour, fromMinute, toHour, toMinute))
		startTime = startTime.Add(30 * time.Minute)
	}
	return options
}

func makeWholeDayExpressOptions(from, to time.Time, isTrivial bool) []*pb.DeliveryOption {
	var options []*pb.DeliveryOption
	startTime := from.Add(20 * time.Minute) // {"delivery_duration":20.0,"radius":5000.0,"zone_id":1227.0}
	limitTime := to.Truncate(time.Hour)
	options = addFirstInterval(startTime, options, 40)
	startTime = startTime.Add(10 * time.Minute) // после первого интервала дальше идут с абсолютным шагом в полчаса
	if !isTrivial {
		limitTime = limitTime.Add(-40 * time.Minute)
	}
	options = fillIntervals(startTime, limitTime, options, 40, true)
	return options
}

func makeExpressOptionsByInterval(
	from time.Time,
	to time.Time,
	needFirstInterval bool,
	customAddTimeToDelivery int,
	isTrivial bool,
) []*pb.DeliveryOption {
	var options []*pb.DeliveryOption
	if customAddTimeToDelivery == 0 {
		customAddTimeToDelivery = 40
	}
	startTime := from
	if needFirstInterval {
		startTime = startTime.Add(20 * time.Minute) // {"delivery_duration":20.0,"radius":5000.0,"zone_id":1227.0}
		options = addFirstInterval(startTime, options, customAddTimeToDelivery)
		startTime = startTime.Add(10 * time.Minute) // после первого интервала дальше идут с абсолютным шагом в полчаса
	}
	options = fillIntervals(startTime, to, options, customAddTimeToDelivery, isTrivial)
	return options
}

func testExpressCourierOptions(
	t *testing.T,
	genData *bg.GenerationData,
	startTime time.Time,
	expressDaysCalcForward int,
	useExpressRoundTo5minutesEvolution bool,
	useCapacityForWholeRegion bool,
	wantOptions []*pb.DeliveryOption,
	shopID uint32,
	customAddTimeToDelivery int,
) {
	settings, _ := its.NewStringSettingsHolder(
		fmt.Sprintf(
			`{"express_days_calc_forward":%d,"use_express_round_to_5minutes_evolution_v2":%t,"express_capacity_for_all_partners_in_region":%t,"custom_add_time_to_delivery": %d}`,
			expressDaysCalcForward,
			useExpressRoundTo5minutesEvolution,
			useCapacityForWholeRegion,
			customAddTimeToDelivery,
		),
	)
	env, cancel := lite.NewEnv(t, genData, settings)
	defer cancel()
	req := lite.MakeRequest(
		lite.RequestWithStartTime(startTime),
		lite.RequestWithDeliveryType(pb.DeliveryType_COURIER),
		lite.RequestWithPartner(expressWarehouseID),
		lite.RequestWithRegion(geobase.RegionMoscow),
		lite.RequestWithGpsCoords(userLatitude, userLongitude),
		lite.RequestWithTotalPrice(totalPrice),
		lite.RequestWithShopID(shopID),
	)

	resp, err := env.Client.GetCourierOptions(env.Ctx, req)
	require.NoError(t, err)

	options := resp.GetOptions()
	//for _, opt := range options {
	//	fmt.Printf("->%d_%s\n", opt.GetDateFrom().GetDay(), opt.GetInterval())
	//}
	//require.False(t, true)
	require.Len(t, options, len(wantOptions))

	for i, opt := range options {
		compareDates(t, wantOptions[i].DateFrom, opt.DateFrom)
		compareDates(t, wantOptions[i].DateTo, opt.DateTo)
		compareIntervals(t, wantOptions[i].Interval, opt.Interval)
	}

	dsResp, dsErr := env.Client.GetCourierDeliveryStatistics(env.Ctx, req)
	require.NoError(t, dsErr)
	require.Len(t, dsResp.DeliveryMethods, 1)
	require.Equal(t, pb.DeliveryMethod_DM_EXPRESS, dsResp.DeliveryMethods[0])
}

func TestExpressCourierOptions(t *testing.T) {
	type optionBounds struct {
		startTime         time.Time
		endTime           time.Time
		needFirstInterval bool
		isTrivial         bool
	}
	// Набор тестовых кейсов для генерации соответствующих расписаний сервисов графа и startTime
	testData := []struct {
		fromSchedule              string
		toSchedule                string
		startTime                 time.Time
		wantOptions               [][]*pb.DeliveryOption // use_express_round_to_5minutes_evolution_v2 = false deprecated
		bounds                    []optionBounds         // use_express_round_to_5minutes_evolution_v2 = true
		shopID                    uint32
		useCapacityForWholeRegion bool
	}{
		// Круглосуточное расписание полностью заполненное интервалами
		{
			fromSchedule: "00:00:00",
			toSchedule:   "23:59:59",
			startTime:    time.Date(2022, 1, 26, 0, 0, 0, 0, lite.MskTZ),
			wantOptions: [][]*pb.DeliveryOption{
				makeWholeDayExpressOptions(
					time.Date(2022, 1, 26, 0, 0, 0, 0, lite.MskTZ),
					time.Date(2022, 1, 27, 0, 0, 0, 0, lite.MskTZ),
					true,
				),
				makeWholeDayExpressOptions(
					time.Date(2022, 1, 27, 0, 0, 0, 0, lite.MskTZ),
					time.Date(2022, 1, 28, 0, 0, 0, 0, lite.MskTZ),
					true,
				),
			}, // deprecated
			bounds: []optionBounds{
				{
					startTime:         time.Date(2022, 1, 26, 0, 0, 0, 0, lite.MskTZ),
					endTime:           time.Date(2022, 1, 27, 0, 0, 0, 0, lite.MskTZ),
					needFirstInterval: true,
					isTrivial:         true,
				},
				{
					startTime:         time.Date(2022, 1, 27, 0, 30, 0, 0, lite.MskTZ),
					endTime:           time.Date(2022, 1, 28, 0, 0, 0, 0, lite.MskTZ),
					needFirstInterval: false,
					isTrivial:         true,
				},
			},
			shopID:                    0,
			useCapacityForWholeRegion: false,
		},
		// Круглосуточное расписание с интервалами с определённого времени, второй и последующие дни(если есть) с начала
		{
			fromSchedule: "00:00:00",
			toSchedule:   "23:59:59",
			startTime:    time.Date(2022, 1, 26, 10, 0, 0, 0, lite.MskTZ),
			wantOptions: [][]*pb.DeliveryOption{
				makeWholeDayExpressOptions(
					time.Date(2022, 1, 26, 10, 0, 0, 0, lite.MskTZ),
					time.Date(2022, 1, 27, 0, 0, 0, 0, lite.MskTZ),
					true,
				),
				makeWholeDayExpressOptions(
					time.Date(2022, 1, 27, 0, 0, 0, 0, lite.MskTZ),
					time.Date(2022, 1, 28, 0, 0, 0, 0, lite.MskTZ),
					true,
				),
			}, // deprecated
			bounds: []optionBounds{
				{
					startTime:         time.Date(2022, 1, 26, 10, 0, 0, 0, lite.MskTZ),
					endTime:           time.Date(2022, 1, 27, 0, 0, 0, 0, lite.MskTZ),
					needFirstInterval: true,
					isTrivial:         true,
				},
				{
					startTime:         time.Date(2022, 1, 27, 0, 30, 0, 0, lite.MskTZ),
					endTime:           time.Date(2022, 1, 28, 0, 0, 0, 0, lite.MskTZ),
					needFirstInterval: false,
					isTrivial:         true,
				},
			},
			shopID:                    0,
			useCapacityForWholeRegion: false,
		},
		// Расписание от-до полностью заполненное интервалами
		{
			fromSchedule: "10:00:00",
			toSchedule:   "20:00:00",
			startTime:    time.Date(2022, 1, 26, 8, 0, 0, 0, lite.MskTZ),
			wantOptions: [][]*pb.DeliveryOption{
				makeWholeDayExpressOptions(
					time.Date(2022, 1, 26, 10, 0, 0, 0, lite.MskTZ),
					time.Date(2022, 1, 26, 20, 0, 0, 0, lite.MskTZ),
					false,
				),
				makeWholeDayExpressOptions(
					time.Date(2022, 1, 27, 10, 0, 0, 0, lite.MskTZ),
					time.Date(2022, 1, 27, 20, 0, 0, 0, lite.MskTZ),
					false,
				),
			}, // deprecated
			bounds: []optionBounds{
				{
					startTime:         time.Date(2022, 1, 26, 10, 0, 0, 0, lite.MskTZ),
					endTime:           time.Date(2022, 1, 26, 20, 0, 0, 0, lite.MskTZ),
					needFirstInterval: true,
				},
				{
					startTime:         time.Date(2022, 1, 27, 10, 30, 0, 0, lite.MskTZ),
					endTime:           time.Date(2022, 1, 27, 20, 0, 0, 0, lite.MskTZ),
					needFirstInterval: false,
				},
			},
			shopID:                    0,
			useCapacityForWholeRegion: false,
		},
		// Расписание от-до с интервалами с определённого времени, второй и последующие дни(если есть) с начала
		{
			fromSchedule: "10:00:00",
			toSchedule:   "20:00:00",
			startTime:    time.Date(2022, 1, 26, 11, 0, 0, 0, lite.MskTZ),
			wantOptions: [][]*pb.DeliveryOption{
				makeWholeDayExpressOptions(
					time.Date(2022, 1, 26, 11, 0, 0, 0, lite.MskTZ),
					time.Date(2022, 1, 26, 20, 0, 0, 0, lite.MskTZ),
					false,
				),
				makeWholeDayExpressOptions(
					time.Date(2022, 1, 27, 10, 0, 0, 0, lite.MskTZ),
					time.Date(2022, 1, 27, 20, 0, 0, 0, lite.MskTZ),
					false,
				),
			}, // deprecated
			bounds: []optionBounds{
				{
					startTime:         time.Date(2022, 1, 26, 11, 0, 0, 0, lite.MskTZ),
					endTime:           time.Date(2022, 1, 26, 20, 0, 0, 0, lite.MskTZ),
					needFirstInterval: true,
				},
				{
					startTime:         time.Date(2022, 1, 27, 10, 30, 0, 0, lite.MskTZ),
					endTime:           time.Date(2022, 1, 27, 20, 0, 0, 0, lite.MskTZ),
					needFirstInterval: false,
				},
			},
			shopID:                    0,
			useCapacityForWholeRegion: false,
		},
		// Круглосуточное расписание с интервалами с определённого времени, второй и последующие дни(если есть) с начала
		// Часть интевалов(11 и 13 call_courier) должны быть скрыты из-за переполнения капасити
		// Капасити здесь generation_sources_test.go:makeExpressIntervalValidator()
		{
			fromSchedule: "00:00:00",
			toSchedule:   "23:59:59",
			startTime:    time.Date(2022, 1, 26, 10, 0, 0, 0, lite.MskTZ),
			wantOptions:  nil, // deprecated
			bounds: []optionBounds{
				{
					startTime:         time.Date(2022, 1, 26, 10, 0, 0, 0, lite.MskTZ),
					endTime:           time.Date(2022, 1, 26, 11, 0, 0, 0, lite.MskTZ),
					needFirstInterval: true,
					isTrivial:         true,
				},
				{
					startTime:         time.Date(2022, 1, 26, 12, 30, 0, 0, lite.MskTZ),
					endTime:           time.Date(2022, 1, 26, 13, 0, 0, 0, lite.MskTZ),
					needFirstInterval: false,
					isTrivial:         true,
				},
				{
					startTime:         time.Date(2022, 1, 26, 14, 30, 0, 0, lite.MskTZ),
					endTime:           time.Date(2022, 1, 27, 0, 0, 0, 0, lite.MskTZ),
					needFirstInterval: false,
					isTrivial:         true,
				},
				{
					startTime:         time.Date(2022, 1, 27, 0, 30, 0, 0, lite.MskTZ),
					endTime:           time.Date(2022, 1, 28, 0, 0, 0, 0, lite.MskTZ),
					needFirstInterval: false,
					isTrivial:         true,
				},
			},
			shopID:                    shopWithCapacityCheckID,
			useCapacityForWholeRegion: true,
		},
		// Расписание от-до с интервалами с определённого времени, второй и последующие дни(если есть) с начала
		// Часть интевалов(11 и 13 call_courier) должны быть скрыты из-за переполнения капасити
		// Капасити здесь generation_sources_test.go:makeExpressIntervalValidator()
		{
			fromSchedule: "10:00:00",
			toSchedule:   "20:00:00",
			startTime:    time.Date(2022, 1, 26, 11, 0, 0, 0, lite.MskTZ),
			wantOptions:  nil, // deprecated
			bounds: []optionBounds{
				{
					startTime:         time.Date(2022, 1, 26, 12, 30, 0, 0, lite.MskTZ),
					endTime:           time.Date(2022, 1, 26, 13, 0, 0, 0, lite.MskTZ),
					needFirstInterval: false,
					isTrivial:         true, // HACK
				},
				{
					startTime:         time.Date(2022, 1, 26, 14, 30, 0, 0, lite.MskTZ),
					endTime:           time.Date(2022, 1, 26, 20, 0, 0, 0, lite.MskTZ),
					needFirstInterval: false,
				},
				{
					startTime:         time.Date(2022, 1, 27, 10, 30, 0, 0, lite.MskTZ),
					endTime:           time.Date(2022, 1, 27, 20, 0, 0, 0, lite.MskTZ),
					needFirstInterval: false,
				},
			},
			shopID:                    shopWithCapacityCheckID,
			useCapacityForWholeRegion: true,
		},

		// Расписание от-до startTime такой что невозможно посчитать хотя бы один интервал в текущий день
		// -> начинаем считать со следующего дня с начала окна

		// Расписание в два+ окна
		// Разные расписания на разных сервисах
		// На разные расстояния
		// Разные дистанции
		// Расчёт на основании статистики
		// Дейоффы
	}
	genData := &bg.GenerationData{
		RegionMap:                regionMap,
		Express:                  makeExpress(),
		TariffsFinder:            makeTariffsFinder(),
		ExpressIntervalValidator: makeExpressIntervalValidator(),
	}
	for _, test := range testData {
		genData.Graph = makeExpressGraph(test.fromSchedule, test.toSchedule)
		// Эмулируем работу флага express_days_calc_forward, который указывает на сколько дополнительных дней вперед считать
		// COMBINATOR-1887 На какое количество дней вперёд размножать интервалы доставки экспрессом
		// deprecated
		if test.shopID == 0 {
			for _, expressDaysCalcForward := range []int{0, 1} {
				var wantOptions []*pb.DeliveryOption
				for i := 0; i <= expressDaysCalcForward && i < len(test.wantOptions); i++ {
					wantOptions = append(wantOptions, test.wantOptions[i]...)
				}
				//for _, opt := range wantOptions {
				//	fmt.Printf("=>%d_%s\n", opt.GetDateFrom().GetDay(), opt.GetInterval())
				//}
				//require.False(t, true)
				testExpressCourierOptions(t, genData, test.startTime, expressDaysCalcForward, false, test.useCapacityForWholeRegion, wantOptions, 0, 0)
			}
		}

		// Опции с эволюцией
		// Эмулируем работу флага express_days_calc_forward, который указывает на сколько дополнительных дней вперед считать
		// COMBINATOR-1887 На какое количество дней вперёд размножать интервалы доставки экспрессом
		flags := []struct {
			customAddTimeToDelivery int
			expressDaysCalcForward  int
		}{
			{0, 0},
			{0, 1},
			{90, 0},
			{90, 1},
		}
		for _, flag := range flags {
			var wantOptions []*pb.DeliveryOption
			startTimeDayStart := timex.ReplaceClock(test.startTime, 0, 0, 0).Add(time.Duration(flag.expressDaysCalcForward) * 24 * time.Hour)
			for _, bounds := range test.bounds {
				boundStartTimeDayStart := timex.ReplaceClock(bounds.startTime, 0, 0, 0)
				if boundStartTimeDayStart.Before(startTimeDayStart) || boundStartTimeDayStart.Equal(startTimeDayStart) {
					wantOptions = append(wantOptions, makeExpressOptionsByInterval(
						bounds.startTime,
						bounds.endTime,
						bounds.needFirstInterval,
						flag.customAddTimeToDelivery,
						bounds.isTrivial,
					)...)
				}
			}
			//for _, opt := range wantOptions {
			//	fmt.Printf("=>%d_%s\n", opt.GetDateFrom().GetDay(), opt.GetInterval())
			//}
			//require.False(t, true)
			testExpressCourierOptions(
				t,
				genData,
				test.startTime,
				flag.expressDaysCalcForward,
				true,
				test.useCapacityForWholeRegion,
				wantOptions,
				test.shopID,
				flag.customAddTimeToDelivery,
			)
		}
	}
}
