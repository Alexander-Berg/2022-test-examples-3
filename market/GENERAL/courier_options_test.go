package routes

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/data"
	"a.yandex-team.ru/market/combinator/pkg/enums"
	"a.yandex-team.ru/market/combinator/pkg/fashion"
	"a.yandex-team.ru/market/combinator/pkg/graph"
	"a.yandex-team.ru/market/combinator/pkg/settings"
	tr "a.yandex-team.ru/market/combinator/pkg/tarifficator"
	"a.yandex-team.ru/market/combinator/pkg/timex"
	cr "a.yandex-team.ru/market/combinator/proto/grpc"
)

func TestIntersectors(t *testing.T) {
	makePathWithInterval := func(day, hour int, paymentsMask enums.PaymentMethodsMask, isMarketCourier bool) graph.PathWithInterval {
		return graph.PathWithInterval{
			SortablePath: &graph.SortablePath{
				Path: &graph.Path{
					PaymentMethods: paymentsMask,
				},
				ShopTariff: &tr.OptionResult{
					IsMarketCourier: isMarketCourier,
				},
			},
			Tail: graph.EndTimeAndInterval{
				EndTime: time.Date(2021, 2, day, hour, 0, 0, 0, time.UTC),
			},
		}
	}
	pathIntervalList := []graph.PathWithInterval{
		makePathWithInterval(24, 10, enums.MethodPrepayAllowed|enums.MethodCardAllowed, true),
		makePathWithInterval(24, 11, enums.MethodPrepayAllowed|enums.MethodCashAllowed, false),
		makePathWithInterval(25, 12, enums.MethodPrepayAllowed|enums.MethodCardAllowed, true),
		makePathWithInterval(26, 13, enums.MethodPrepayAllowed|enums.MethodCardAllowed, false),
	}
	intersector := makeIntersectors(pathIntervalList, nil)
	require.Equal(t, enums.MethodPrepayAllowed, intersector.getPaymentsMask(pathIntervalList[0]))
	require.Equal(t, enums.MethodPrepayAllowed, intersector.getPaymentsMask(pathIntervalList[1]))
	require.Equal(t, enums.MethodPrepayAllowed|enums.MethodCardAllowed, intersector.getPaymentsMask(pathIntervalList[2]))
	require.Equal(t, false, intersector.getLeaveAtTheDoor(pathIntervalList[0]))
	require.Equal(t, false, intersector.getLeaveAtTheDoor(pathIntervalList[1]))
	require.Equal(t, true, intersector.getLeaveAtTheDoor(pathIntervalList[2]))
	require.Equal(t, false, intersector.getLeaveAtTheDoor(pathIntervalList[3]))
	require.Equal(t, false, intersector.getDoNotCall(pathIntervalList[0]))
	require.Equal(t, false, intersector.getDoNotCall(pathIntervalList[1]))
	require.Equal(t, true, intersector.getDoNotCall(pathIntervalList[2]))
	require.Equal(t, false, intersector.getDoNotCall(pathIntervalList[3]))
}

func TestLeaveAtTheDoor(t *testing.T) {
	makePathWithInterval := func(day, hour int, isMarketCourier bool, deliveryServiceID uint64) graph.PathWithInterval {
		return graph.PathWithInterval{
			SortablePath: &graph.SortablePath{
				Path: &graph.Path{
					PaymentMethods: enums.MethodPrepayAllowed,
				},
				ShopTariff: tr.NewTestOptionResult(isMarketCourier, deliveryServiceID, nil),
			},
			Tail: graph.EndTimeAndInterval{
				EndTime: time.Date(2021, time.August, day, hour, 0, 0, 0, time.UTC),
			},
		}
	}
	getRandomLeaveAtTheDoorDS := func() uint64 {
		for id := range data.LeaveAtTheDoorDeliveryServices {
			return id
		}
		return 0
	}

	pathIntervalList := []graph.PathWithInterval{
		makePathWithInterval(10, 10, true, 1),
		makePathWithInterval(10, 12, false, getRandomLeaveAtTheDoorDS()),

		makePathWithInterval(11, 10, false, getRandomLeaveAtTheDoorDS()),
		makePathWithInterval(11, 12, false, getRandomLeaveAtTheDoorDS()),

		makePathWithInterval(12, 10, false, getRandomLeaveAtTheDoorDS()),
		makePathWithInterval(12, 12, false, 1),

		makePathWithInterval(13, 10, false, 1),
		makePathWithInterval(13, 12, false, 1),
	}
	Intersector := makeIntersectors(pathIntervalList, nil)

	// Market Courier + hardcoded delivery services
	require.Equal(t, true, Intersector.getLeaveAtTheDoor(pathIntervalList[0]))
	require.Equal(t, true, Intersector.getLeaveAtTheDoor(pathIntervalList[1]))

	// 2 options with hardcoded delivery services
	require.Equal(t, true, Intersector.getLeaveAtTheDoor(pathIntervalList[2]))
	require.Equal(t, true, Intersector.getLeaveAtTheDoor(pathIntervalList[3]))

	// Hardcoded delivery service + ordinary delivery service
	require.Equal(t, false, Intersector.getLeaveAtTheDoor(pathIntervalList[4]))
	require.Equal(t, false, Intersector.getLeaveAtTheDoor(pathIntervalList[5]))

	// 2 options with ordinary delivery services
	require.Equal(t, false, Intersector.getLeaveAtTheDoor(pathIntervalList[6]))
	require.Equal(t, false, Intersector.getLeaveAtTheDoor(pathIntervalList[7]))

	// COMBINATOR-2091 Disable leave-at-the-door when basket is partially deliverable and market courier
	for i := 0; i < 4; i++ {
		fashion.GetPartialDeliveryServices(nil)[pathIntervalList[i].DeliveryServiceID] = true
	}
	Intersector = makeIntersectors(
		pathIntervalList,
		&DeliverySettings{
			Settings:                   &settings.Settings{},
			BasketPartiallyDeliverable: true,
		},
	)
	for i := 0; i < 4; i++ {
		fashion.GetPartialDeliveryServices(nil)[pathIntervalList[i].DeliveryServiceID] = false
	}

	// Market Courier + hardcoded delivery services
	require.Equal(t, false, Intersector.getLeaveAtTheDoor(pathIntervalList[0]))
	require.Equal(t, false, Intersector.getLeaveAtTheDoor(pathIntervalList[1]))
	// 2 options with hardcoded delivery services
	require.Equal(t, true, Intersector.getLeaveAtTheDoor(pathIntervalList[2]))
	require.Equal(t, true, Intersector.getLeaveAtTheDoor(pathIntervalList[3]))
}

func TestWithCanLeaveAtTheDoor(t *testing.T) {
	{
		result := withCanLeaveAtTheDoor(true)
		want := &cr.DeliveryOption_Customizer{
			Key:  "leave_at_the_door",
			Name: "Оставить у двери",
			Type: "boolean",
		}
		require.Equal(t, want, result)
	}
	{
		result := withCanLeaveAtTheDoor(false)
		require.Nil(t, result)
	}
}

func TestWithDoNotCall(t *testing.T) {
	{
		result := withDoNotCall(true)
		want := &cr.DeliveryOption_Customizer{
			Key:  "not_call",
			Name: "Не звонить",
			Type: "boolean",
		}
		require.Equal(t, want, result)
	}
	{
		result := withDoNotCall(false)
		require.Nil(t, result)
	}
}

func TestMakeCustomizers(t *testing.T) {
	{
		result := makeCustomizers(
			withCanLeaveAtTheDoor(true),
			withDoNotCall(true),
		)
		want := []*cr.DeliveryOption_Customizer{
			{
				Key:  "leave_at_the_door",
				Name: "Оставить у двери",
				Type: "boolean",
			},
			{
				Key:  "not_call",
				Name: "Не звонить",
				Type: "boolean",
			},
		}
		require.Equal(t, want, result)
	}
	{
		result := makeCustomizers(
			withCanLeaveAtTheDoor(true),
			withDoNotCall(false),
		)
		want := []*cr.DeliveryOption_Customizer{
			{
				Key:  "leave_at_the_door",
				Name: "Оставить у двери",
				Type: "boolean",
			},
		}
		require.Equal(t, want, result)
	}
	{
		result := makeCustomizers(
			withCanLeaveAtTheDoor(false),
			withDoNotCall(true),
		)
		want := []*cr.DeliveryOption_Customizer{
			{
				Key:  "not_call",
				Name: "Не звонить",
				Type: "boolean",
			},
		}
		require.Equal(t, want, result)
	}
	{
		result := makeCustomizers(
			withCanLeaveAtTheDoor(false),
			withDoNotCall(false),
		)
		want := make([]*cr.DeliveryOption_Customizer, 0)
		require.Equal(t, want, result)
	}
}

func TestCreateDeliveryOptionsByIntervals(t *testing.T) {
	leaveCustomizer := &cr.DeliveryOption_Customizer{
		Key:  "leave_at_the_door",
		Name: "Оставить у двери",
		Type: "boolean",
	}
	notCallCustomizer := &cr.DeliveryOption_Customizer{
		Key:  "not_call",
		Name: "Не звонить",
		Type: "boolean",
	}
	type testDataParams struct {
		deliveryServiceID     uint64
		wantDeliveryServiceID uint32
		isMarketCourier       bool
		leaveAtTheDoor        bool
		doNotCall             bool
		customizers           []*cr.DeliveryOption_Customizer
		paymentMethods        enums.PaymentMethodsMask
		wantPaymentMethods    []cr.PaymentMethod
		y                     int
		m                     int
		d                     int
		fromH                 int8
		fromM                 int8
		toH                   int8
		toM                   int8
		airshipDelivery       bool
	}
	makeTestData := func(param *testDataParams) (graph.PathWithInterval, *cr.DeliveryOption) {
		pathInterval := graph.PathWithInterval{
			SortablePath: &graph.SortablePath{
				Path: &graph.Path{
					DeliveryServiceID: param.deliveryServiceID,
					PaymentMethods:    param.paymentMethods,
				},
				ShopTariff: &tr.OptionResult{
					IsMarketCourier: param.isMarketCourier,
				},
			},
			Tail: graph.EndTimeAndInterval{
				EndTime: time.Date(param.y, time.Month(param.m), param.d, 0, 0, 0, 0, time.UTC),
				Interval: graph.Interval{
					From: timex.DayTime{Hour: param.fromH, Minute: param.fromM},
					To:   timex.DayTime{Hour: param.toH, Minute: param.toM},
				},
			},
		}
		want := &cr.DeliveryOption{
			DateFrom: &cr.Date{Year: uint32(param.y), Month: uint32(param.m), Day: uint32(param.d)},
			DateTo:   &cr.Date{Year: uint32(param.y), Month: uint32(param.m), Day: uint32(param.d)},
			Interval: &cr.DeliveryInterval{
				From: &cr.Time{Hour: uint32(param.fromH), Minute: uint32(param.fromM)},
				To:   &cr.Time{Hour: uint32(param.toH), Minute: uint32(param.toM)},
			},
			DeliveryServiceId: param.wantDeliveryServiceID,
			PaymentMethods:    param.wantPaymentMethods,
			DeliverySubtype:   1,
			LeaveAtTheDoor:    param.leaveAtTheDoor,
			DoNotCall:         param.doNotCall,
			Customizers:       param.customizers,
			AirshipDelivery:   param.airshipDelivery,
			IsMarketCourier:   param.isMarketCourier,
		}
		return pathInterval, want
	}

	startTime := time.Date(2021, 9, 24, 0, 0, 0, 0, time.UTC)
	// isMarketCourier и PaymentMethods интервалов пересекаются в рамках дня
	{
		settings := &DeliverySettings{
			Settings: &settings.Settings{
				EnableFlatCourierOptions:  false, // isMarketCourier и PaymentMethods интервалов пересекаются в рамках дня
				UseWriteDeliveryServiceID: true,
			},
		}
		params := []*testDataParams{
			{
				deliveryServiceID:     100,
				wantDeliveryServiceID: 100,
				isMarketCourier:       true,
				leaveAtTheDoor:        true,
				doNotCall:             true,
				customizers:           []*cr.DeliveryOption_Customizer{leaveCustomizer, notCallCustomizer},
				paymentMethods:        enums.MethodCashAllowed | enums.MethodCardAllowed,
				wantPaymentMethods:    []cr.PaymentMethod{cr.PaymentMethod_CASH, cr.PaymentMethod_CARD},
				y:                     2021,
				m:                     9,
				d:                     21,
				fromH:                 10,
				fromM:                 30,
				toH:                   14,
				toM:                   0,
			},
			{
				deliveryServiceID:     100,
				wantDeliveryServiceID: 100,
				isMarketCourier:       false,
				leaveAtTheDoor:        false,
				doNotCall:             false,
				customizers:           []*cr.DeliveryOption_Customizer{},
				paymentMethods:        enums.MethodCashAllowed,
				wantPaymentMethods:    []cr.PaymentMethod{cr.PaymentMethod_CASH},
				y:                     2021,
				m:                     9,
				d:                     22,
				fromH:                 10,
				fromM:                 30,
				toH:                   14,
				toM:                   0,
			},
			// Следующие два интервала в один день, а значит isMarketCourier и PaymentMethods пересекаются
			{
				deliveryServiceID:     100,
				wantDeliveryServiceID: 100,
				isMarketCourier:       true,
				leaveAtTheDoor:        false,
				doNotCall:             false,
				customizers:           []*cr.DeliveryOption_Customizer{},
				paymentMethods:        enums.MethodCashAllowed | enums.MethodCardAllowed,
				wantPaymentMethods:    []cr.PaymentMethod{cr.PaymentMethod_CASH},
				y:                     2021,
				m:                     9,
				d:                     23,
				fromH:                 10,
				fromM:                 30,
				toH:                   14,
				toM:                   0,
			},
			{
				deliveryServiceID:     100,
				wantDeliveryServiceID: 100,
				isMarketCourier:       false,
				leaveAtTheDoor:        false,
				doNotCall:             false,
				customizers:           []*cr.DeliveryOption_Customizer{},
				paymentMethods:        enums.MethodCashAllowed,
				wantPaymentMethods:    []cr.PaymentMethod{cr.PaymentMethod_CASH},
				y:                     2021,
				m:                     9,
				d:                     23,
				fromH:                 14,
				fromM:                 30,
				toH:                   18,
				toM:                   0,
			},
			// Следующие два интервала в один день, а значит isMarketCourier и PaymentMethods пересекаются
			{
				deliveryServiceID:     100,
				wantDeliveryServiceID: 100,
				isMarketCourier:       true,
				leaveAtTheDoor:        true,
				doNotCall:             true,
				customizers:           []*cr.DeliveryOption_Customizer{leaveCustomizer, notCallCustomizer},
				paymentMethods:        enums.MethodCashAllowed | enums.MethodCardAllowed,
				wantPaymentMethods:    []cr.PaymentMethod{cr.PaymentMethod_CASH, cr.PaymentMethod_CARD},
				y:                     2021,
				m:                     9,
				d:                     24,
				fromH:                 10,
				fromM:                 30,
				toH:                   14,
				toM:                   0,
			},
			{
				deliveryServiceID:     100,
				wantDeliveryServiceID: 100,
				isMarketCourier:       true,
				leaveAtTheDoor:        true,
				doNotCall:             true,
				customizers:           []*cr.DeliveryOption_Customizer{leaveCustomizer, notCallCustomizer},
				paymentMethods:        enums.MethodCashAllowed | enums.MethodCardAllowed,
				wantPaymentMethods:    []cr.PaymentMethod{cr.PaymentMethod_CASH, cr.PaymentMethod_CARD},
				y:                     2021,
				m:                     9,
				d:                     24,
				fromH:                 14,
				fromM:                 30,
				toH:                   18,
				toM:                   0,
			},
		}

		pathIntervalList := make([]graph.PathWithInterval, 0, len(params))
		wantList := make([]*cr.DeliveryOption, 0, len(params))
		for _, param := range params {
			pathInterval, want := makeTestData(param)
			pathIntervalList = append(pathIntervalList, pathInterval)
			wantList = append(wantList, want)
		}
		result := createDeliveryOptionsByIntervals(
			context.Background(),
			startTime,
			pathIntervalList,
			1,
			settings,
			false,
		)
		require.Equal(t, wantList, result)

		// COMBINATOR-2091 Для корзин с примеркой и market courier не возвращать опцию leave at the door
		fashion.GetPartialDeliveryServices(nil)[100] = true
		settings.BasketPartiallyDeliverable = true
		for i := range wantList {
			if params[i].leaveAtTheDoor && params[i].isMarketCourier {
				wantList[i].LeaveAtTheDoor = false
				wantList[i].Customizers = []*cr.DeliveryOption_Customizer{notCallCustomizer}
			}
		}
		result = createDeliveryOptionsByIntervals(
			context.Background(),
			startTime,
			pathIntervalList,
			1,
			settings,
			false,
		)
		require.Equal(t, wantList, result)
		fashion.GetPartialDeliveryServices(nil)[100] = false

		// COMBINATOR-1986 Для авиа курьерки помечать опцию как авиа.
		settings.BasketPartiallyDeliverable = false
		pathIntervalList = make([]graph.PathWithInterval, 0, len(params))
		wantList = make([]*cr.DeliveryOption, 0, len(params))
		for _, param := range params {
			param.deliveryServiceID = graph.DSYandexGoAvia
			param.wantDeliveryServiceID = uint32(graph.DSYandexGoAvia)
			param.airshipDelivery = true
			pathInterval, want := makeTestData(param)
			pathIntervalList = append(pathIntervalList, pathInterval)
			wantList = append(wantList, want)
		}
		result = createDeliveryOptionsByIntervals(
			context.Background(),
			startTime,
			pathIntervalList,
			1,
			settings,
			false,
		)
		require.Equal(t, wantList, result)

	}

	// isMarketCourier и PaymentMethods собственные для каждых интервалов
	{
		settings := &DeliverySettings{
			Settings: &settings.Settings{
				EnableFlatCourierOptions:  true, // isMarketCourier и PaymentMethods собственные для каждых интервалов
				UseWriteDeliveryServiceID: false,
			},
		}
		params := []*testDataParams{
			// Следующие два интервала в один день, а значит isMarketCourier и PaymentMethods пересекаются
			{
				deliveryServiceID:     100,
				wantDeliveryServiceID: 0,
				isMarketCourier:       true,
				leaveAtTheDoor:        true,
				doNotCall:             true,
				customizers:           []*cr.DeliveryOption_Customizer{leaveCustomizer, notCallCustomizer},
				paymentMethods:        enums.MethodCashAllowed | enums.MethodCardAllowed,
				wantPaymentMethods:    []cr.PaymentMethod{cr.PaymentMethod_CASH, cr.PaymentMethod_CARD},
				y:                     2021,
				m:                     9,
				d:                     22,
				fromH:                 10,
				fromM:                 30,
				toH:                   14,
				toM:                   0,
			},
			{
				deliveryServiceID:     100,
				wantDeliveryServiceID: 0,
				isMarketCourier:       true,
				leaveAtTheDoor:        true,
				doNotCall:             true,
				customizers:           []*cr.DeliveryOption_Customizer{leaveCustomizer, notCallCustomizer},
				paymentMethods:        enums.MethodCashAllowed,
				wantPaymentMethods:    []cr.PaymentMethod{cr.PaymentMethod_CASH},
				y:                     2021,
				m:                     9,
				d:                     22,
				fromH:                 14,
				fromM:                 30,
				toH:                   18,
				toM:                   0,
			},
			// Следующие два интервала в один день, а значит isMarketCourier и PaymentMethods но не пересекаются
			{
				deliveryServiceID:     100,
				wantDeliveryServiceID: 0,
				isMarketCourier:       true,
				leaveAtTheDoor:        true,
				doNotCall:             true,
				customizers:           []*cr.DeliveryOption_Customizer{leaveCustomizer, notCallCustomizer},
				paymentMethods:        enums.MethodCashAllowed | enums.MethodCardAllowed,
				wantPaymentMethods:    []cr.PaymentMethod{cr.PaymentMethod_CASH, cr.PaymentMethod_CARD},
				y:                     2021,
				m:                     9,
				d:                     23,
				fromH:                 10,
				fromM:                 30,
				toH:                   14,
				toM:                   0,
			},
			{
				deliveryServiceID:     100,
				wantDeliveryServiceID: 0,
				isMarketCourier:       false,
				leaveAtTheDoor:        false,
				doNotCall:             false,
				customizers:           []*cr.DeliveryOption_Customizer{},
				paymentMethods:        enums.MethodCashAllowed | enums.MethodCardAllowed,
				wantPaymentMethods:    []cr.PaymentMethod{cr.PaymentMethod_CASH, cr.PaymentMethod_CARD},
				y:                     2021,
				m:                     9,
				d:                     23,
				fromH:                 14,
				fromM:                 30,
				toH:                   18,
				toM:                   0,
			},
			// Следующие два интервала в один день, а значит isMarketCourier и PaymentMethods но не пересекаются
			{
				deliveryServiceID:     100,
				wantDeliveryServiceID: 0,
				isMarketCourier:       false,
				leaveAtTheDoor:        false,
				doNotCall:             false,
				customizers:           []*cr.DeliveryOption_Customizer{},
				//paymentMethods:      default(enums.MethodUnknown),
				//wantPaymentMethods:  default(nil),
				y:     2021,
				m:     9,
				d:     24,
				fromH: 10,
				fromM: 30,
				toH:   14,
				toM:   0,
			},
			{
				deliveryServiceID:     100,
				wantDeliveryServiceID: 0,
				isMarketCourier:       false,
				leaveAtTheDoor:        false,
				doNotCall:             false,
				customizers:           []*cr.DeliveryOption_Customizer{},
				paymentMethods:        enums.MethodCashAllowed | enums.MethodCardAllowed | enums.MethodPrepayAllowed,
				wantPaymentMethods:    []cr.PaymentMethod{cr.PaymentMethod_PREPAYMENT, cr.PaymentMethod_CASH, cr.PaymentMethod_CARD},
				y:                     2021,
				m:                     9,
				d:                     24,
				fromH:                 14,
				fromM:                 30,
				toH:                   18,
				toM:                   0,
			},
		}

		pathIntervalList := make([]graph.PathWithInterval, 0, len(params))
		wantList := make([]*cr.DeliveryOption, 0, len(params))
		for _, param := range params {
			pathInterval, want := makeTestData(param)
			pathIntervalList = append(pathIntervalList, pathInterval)
			wantList = append(wantList, want)
		}
		result := createDeliveryOptionsByIntervals(
			context.Background(),
			startTime,
			pathIntervalList,
			1,
			settings,
			false,
		)
		require.Equal(t, wantList, result)

		// COMBINATOR-2091 Для корзин с примеркой и market courier не возвращать опцию leave at the door
		settings.BasketPartiallyDeliverable = true
		fashion.GetPartialDeliveryServices(nil)[100] = true
		for i := range wantList {
			if params[i].leaveAtTheDoor && params[i].isMarketCourier {
				wantList[i].LeaveAtTheDoor = false
				wantList[i].Customizers = []*cr.DeliveryOption_Customizer{notCallCustomizer}
			}
		}
		result = createDeliveryOptionsByIntervals(
			context.Background(),
			startTime,
			pathIntervalList,
			1,
			settings,
			false,
		)
		require.Equal(t, wantList, result)
		fashion.GetPartialDeliveryServices(nil)[100] = false
	}
}

func TestSortByDateTimeFrom(t *testing.T) {
	options := []*cr.DeliveryOption{
		{
			DateFrom: CreateDate(time.Date(2020, 12, 1, 0, 0, 0, 0, time.UTC)),
			Interval: &cr.DeliveryInterval{
				From: &cr.Time{
					Hour:   13,
					Minute: 10,
				},
			},
		},
		{
			DateFrom: CreateDate(time.Date(2020, 12, 1, 0, 0, 0, 0, time.UTC)),
			Interval: &cr.DeliveryInterval{
				From: &cr.Time{
					Hour:   12,
					Minute: 10,
				},
			},
		},
		{
			DateFrom: CreateDate(time.Date(2020, 12, 1, 0, 0, 0, 0, time.UTC)),
			Interval: &cr.DeliveryInterval{
				From: &cr.Time{
					Hour:   10,
					Minute: 10,
				},
			},
		},
		{
			DateFrom: CreateDate(time.Date(2020, 12, 1, 0, 0, 0, 0, time.UTC)),
			Interval: &cr.DeliveryInterval{
				From: &cr.Time{
					Hour:   11,
					Minute: 10,
				},
			},
		},
		{
			DateFrom: CreateDate(time.Date(2020, 12, 1, 0, 0, 0, 0, time.UTC)),
			Interval: &cr.DeliveryInterval{
				From: &cr.Time{
					Hour:   12,
					Minute: 20,
				},
			},
		},
		{
			DateFrom: CreateDate(time.Date(2020, 12, 2, 0, 0, 0, 0, time.UTC)),
			Interval: &cr.DeliveryInterval{
				From: &cr.Time{
					Hour:   12,
					Minute: 30,
				},
			},
		},
		{
			DateFrom: CreateDate(time.Date(2020, 12, 2, 0, 0, 0, 0, time.UTC)),
			Interval: &cr.DeliveryInterval{
				From: &cr.Time{
					Hour:   12,
					Minute: 20,
				},
			},
		},
	}

	sortResult := sortByDateTimeFrom(options)

	var prevTime *time.Time
	for _, option := range sortResult {
		optionTime := time.Date(int(option.DateFrom.Year), time.Month(option.DateFrom.Month), int(option.DateFrom.Day),
			int(option.Interval.From.Hour), int(option.Interval.From.Minute), 0, 0, time.UTC)

		if prevTime != nil {
			require.Greater(t, optionTime.Unix(), prevTime.Unix())
		}

		prevTime = &optionTime
	}
}

func TestExpressSubtractOneMinuteFromLastDayInterval(t *testing.T) {
	intervals := []struct {
		src graph.Interval
		dst graph.Interval
	}{
		// 22:30-00:00 -> 22:30-23:59
		{
			src: graph.Interval{
				From: timex.DayTime{Hour: 22, Minute: 30},
				To:   timex.DayTime{Hour: 0, Minute: 0},
			},
			dst: graph.Interval{
				From: timex.DayTime{Hour: 22, Minute: 30},
				To:   timex.DayTime{Hour: 23, Minute: 59},
			},
		},
		// 23:00-00:30 -> 23:00-00:30
		{
			src: graph.Interval{
				From: timex.DayTime{Hour: 23, Minute: 0},
				To:   timex.DayTime{Hour: 0, Minute: 30},
			},
			dst: graph.Interval{
				From: timex.DayTime{Hour: 23, Minute: 0},
				To:   timex.DayTime{Hour: 0, Minute: 30},
			},
		},
		// 22:00-23:30 -> 22:00-23:30
		{
			src: graph.Interval{
				From: timex.DayTime{Hour: 22, Minute: 0},
				To:   timex.DayTime{Hour: 23, Minute: 30},
			},
			dst: graph.Interval{
				From: timex.DayTime{Hour: 22, Minute: 0},
				To:   timex.DayTime{Hour: 23, Minute: 30},
			},
		},
		// 23:20-00:00 -> 23:20-23:59
		{
			src: graph.Interval{
				From: timex.DayTime{Hour: 23, Minute: 20},
				To:   timex.DayTime{Hour: 0, Minute: 0},
			},
			dst: graph.Interval{
				From: timex.DayTime{Hour: 23, Minute: 20},
				To:   timex.DayTime{Hour: 23, Minute: 59},
			},
		},
	}

	pathsWithIntervals := make([]graph.PathWithInterval, len(intervals))
	for i, interval := range intervals {
		pb := graph.NewPathBuilder(graph.WithInterval(interval.src))
		pathsWithIntervals[i] = graph.PathWithInterval{
			SortablePath: pb.GetSortablePath(),
			Tail: graph.EndTimeAndInterval{
				Interval: interval.src,
			},
		}
	}
	resultPathsWithIntervals := expressSubtractOneMinuteFromLastDayInterval(pathsWithIntervals)

	require.Equal(t, len(pathsWithIntervals), len(resultPathsWithIntervals))
	for i := range intervals {
		require.Equal(t, resultPathsWithIntervals[i].Tail.Interval, intervals[i].dst)
	}
}

func TestExpressFilterToReserveTimeForReturn(t *testing.T) {
	aroundTheClockSchedule, _ := graph.CreateAroundTheClockSchedule(false)
	everyday10to19Schedule, _ := graph.CreateEveryDaySchedule("10:00:00", "19:00:00", false)
	fiveDays10to19Schedule, _ := graph.CreateScheduleForSeveralDays("10:00:00", "19:00:00", 5, false)
	type pathOptions struct {
		interval  graph.Interval
		startTime time.Time
		isValid   bool
	}
	cases := []struct {
		scheduleShipment   *graph.Schedule
		scheduleProcessing *graph.Schedule
		paths              []pathOptions
	}{
		{
			scheduleShipment: everyday10to19Schedule,
			// Interval is inside the warehouse.SHIPMENT's schedule -> valid
			paths: []pathOptions{
				{
					interval:  graph.Interval{From: timex.DayTime{Hour: 15}, To: timex.DayTime{Hour: 16, Minute: 30}},
					startTime: time.Date(2022, 6, 15, 10, 0, 0, 0, time.UTC),
					isValid:   true,
				},
				// Interval is outside the warehouse.SHIPMENT's schedule -> invalid
				{
					interval:  graph.Interval{From: timex.DayTime{Hour: 19}, To: timex.DayTime{Hour: 20, Minute: 30}},
					startTime: time.Date(2022, 6, 15, 10, 0, 0, 0, time.UTC),
					isValid:   false,
				},
			},
		},
		// There is trivial(round the clock) schedule -> valid
		{
			scheduleShipment: aroundTheClockSchedule,
			paths: []pathOptions{
				{
					interval:  graph.Interval{From: timex.DayTime{Hour: 19}, To: timex.DayTime{Hour: 20, Minute: 30}},
					startTime: time.Date(2022, 6, 15, 10, 0, 0, 0, time.UTC),
					isValid:   true,
				},
			},
		},
		// There is no the warehouse.SHIPMENT's schedule -> fallback to warehouse.PROCESSING's schedule
		{
			scheduleShipment:   nil,
			scheduleProcessing: everyday10to19Schedule,
			// Interval is inside the warehouse.PROCESSING's schedule -> valid
			paths: []pathOptions{
				{
					interval:  graph.Interval{From: timex.DayTime{Hour: 15}, To: timex.DayTime{Hour: 16, Minute: 30}},
					startTime: time.Date(2022, 6, 15, 10, 0, 0, 0, time.UTC),
					isValid:   true,
				},
				// Interval is outside the warehouse.PROCESSING's schedule -> invalid
				{
					interval:  graph.Interval{From: timex.DayTime{Hour: 19}, To: timex.DayTime{Hour: 20, Minute: 30}},
					startTime: time.Date(2022, 6, 15, 10, 0, 0, 0, time.UTC),
					isValid:   false,
				},
			},
		},
		// There is no any schedule -> can't filter -> valid
		{
			scheduleShipment:   nil,
			scheduleProcessing: nil,
			paths: []pathOptions{
				{
					interval:  graph.Interval{From: timex.DayTime{Hour: 19}, To: timex.DayTime{Hour: 20, Minute: 30}},
					startTime: time.Date(2022, 6, 15, 10, 0, 0, 0, time.UTC),
					isValid:   true,
				},
			},
		},
		// There is no appropriate for weekday schedule -> can't filter -> valid
		{
			scheduleShipment: fiveDays10to19Schedule,
			// Interval is inside the warehouse.PROCESSING's schedule day -> valid
			paths: []pathOptions{
				{
					interval:  graph.Interval{From: timex.DayTime{Hour: 15}, To: timex.DayTime{Hour: 16, Minute: 30}},
					startTime: time.Date(2022, 6, 17, 10, 0, 0, 0, time.UTC),
					isValid:   true,
				},
				// Interval is outside the warehouse.PROCESSING's schedule day -> can't check -> valid
				{
					interval:  graph.Interval{From: timex.DayTime{Hour: 19}, To: timex.DayTime{Hour: 20, Minute: 30}},
					startTime: time.Date(2022, 6, 18, 10, 0, 0, 0, time.UTC),
					isValid:   true,
				},
			},
		},
		// Slice with paths is empty
		{},
	}
	for _, c := range cases {
		var pathsWithIntervals []graph.PathWithInterval
		var wantPathsWithIntervals []graph.PathWithInterval
		for _, path := range c.paths {
			pb := graph.NewPathBuilder(graph.WithInterval(path.interval))
			if c.scheduleShipment != nil {
				pb.AddWarehouse(
					pb.MakeShipmentService(
						pb.WithStartTime(path.startTime),
						pb.WithSchedule(*c.scheduleShipment),
					),
				)
			} else if c.scheduleProcessing != nil {
				pb.AddWarehouse(
					pb.MakeProcessingService(
						pb.WithStartTime(path.startTime),
						pb.WithSchedule(*c.scheduleProcessing),
					),
				)
			} else {
				pb.AddWarehouse(
					pb.MakeShipmentService(
						pb.WithStartTime(path.startTime),
					),
				)
			}

			pathWithInterval := graph.PathWithInterval{
				SortablePath: pb.GetSortablePath(),
				Tail: graph.EndTimeAndInterval{
					Interval: path.interval,
				},
			}
			pathWithInterval.EndTime = path.startTime.Add(2 * time.Hour)
			pathsWithIntervals = append(pathsWithIntervals, pathWithInterval)
			if path.isValid {
				wantPathsWithIntervals = append(wantPathsWithIntervals, pathWithInterval)
			}
		}
		resultPathsWithIntervals := expressFilterToReserveTimeForReturn(
			context.Background(),
			nil,
			pathsWithIntervals,
			nil,
		)
		require.Equal(t, len(wantPathsWithIntervals), len(resultPathsWithIntervals))
		for i := range wantPathsWithIntervals {
			require.Equal(t, wantPathsWithIntervals[i].Tail.Interval, resultPathsWithIntervals[i].Tail.Interval)
		}
	}
}
