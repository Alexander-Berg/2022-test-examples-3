package graph

import (
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/timex"
)

func TestGetLogisticDayStart(t *testing.T) {
	{
		require.Equal(t, Midnight, getLogisticDayStart(timex.DayTime{}))
	}
	{
		paramDayTime := timex.DayTime{Hour: 1}
		require.Equal(t, paramDayTime, getLogisticDayStart(paramDayTime))
	}
}

func TestFindStartTimeAndFillAdditionalData(t *testing.T) {
	{
		stl := ServiceTimeList{

			NewServiceTime(
				0,
				time.Date(2021, 5, 29, 10, 23, 00, 00, time.UTC),
			),
			NewServiceTime(
				1,
				time.Date(2021, 5, 29, 23, 25, 00, 00, time.UTC),
			),
			NewServiceTime(
				2,
				time.Date(2021, 5, 29, 23, 25, 00, 00, time.UTC),
			),
			NewServiceTime(
				3,
				time.Date(2021, 5, 29, 23, 25, 00, 00, time.UTC),
			),
			NewServiceTime(
				4,
				time.Date(2021, 5, 29, 23, 25, 00, 00, time.UTC),
			),
			NewServiceTime(
				5,
				time.Date(2021, 5, 29, 10, 26, 00, 00, time.UTC),
			),
			NewServiceTime(
				6,
				time.Date(2021, 5, 29, 5, 31, 00, 00, time.UTC),
			),
		}
		path := Path{
			ServiceTimeList: stl,
		}

		logisticDates := make([]time.Time, 7)
		// Смещение по часовому поясу оставляет startTime в той же дате
		logisticDates[0], _ = CalcLogisticDate(path.ServiceTimeList, 0, 10800, Midnight)
		wantService0LogisticDate := time.Date(2021, 5, 29, 0, 0, 0, 0, timex.FixedZone("CUSTOM_TZ", 10800))
		// Смещение по часовому поясу смещает startTime на следующий день
		logisticDates[1], _ = CalcLogisticDate(path.ServiceTimeList, 1, 10800, Midnight)
		wantService1LogisticDate := time.Date(2021, 5, 30, 0, 0, 0, 0, timex.FixedZone("CUSTOM_TZ", 10800))
		// Без смещения по часовому поясу startTime остаётся в той же дате
		logisticDates[2], _ = CalcLogisticDate(path.ServiceTimeList, 2, 0, Midnight)
		wantService2LogisticDate := time.Date(2021, 5, 29, 0, 0, 0, 0, time.UTC)
		// Смещение по часовому поясу смещает startTime на следующий день,
		// но это время оказывается ПОСЛЕ начала новых логистических суток -> logisticDate на следуюший день
		logisticDates[3], _ = CalcLogisticDate(path.ServiceTimeList, 3, 10800, timex.DayTime{Hour: 1})
		wantService3LogisticDate := time.Date(2021, 5, 30, 0, 0, 0, 0, timex.FixedZone("CUSTOM_TZ", 10800))
		// Смещение по часовому поясу смещает startTime на следующий день,
		// но это время оказывается ДО начала новых логистических суток -> logisticDate на тот же день
		logisticDates[4], _ = CalcLogisticDate(path.ServiceTimeList, 4, 10800, timex.DayTime{Hour: 4})
		wantService4LogisticDate := time.Date(2021, 5, 29, 0, 0, 0, 0, timex.FixedZone("CUSTOM_TZ", 10800))
		// Смещение по часовому поясу НЕ смещает startTime на следующий день,
		// но это время оказывается ПОСЛЕ начала новых логистических суток -> logisticDate на тот же день
		logisticDates[5], _ = CalcLogisticDate(path.ServiceTimeList, 5, 10800, timex.DayTime{Hour: 10})
		wantService5LogisticDate := time.Date(2021, 5, 29, 0, 0, 0, 0, timex.FixedZone("CUSTOM_TZ", 10800))
		// Смещение по часовому поясу смещает startTime на следующий день,
		// но это время оказывается ДО начала новых логистических суток -> logisticDate на предыдущий день
		logisticDates[6], _ = CalcLogisticDate(path.ServiceTimeList, 6, 10800, timex.DayTime{Hour: 10})
		wantService6LogisticDate := time.Date(2021, 5, 28, 0, 0, 0, 0, timex.FixedZone("CUSTOM_TZ", 10800))

		require.Equal(t, wantService0LogisticDate, logisticDates[0])
		require.Equal(t, wantService1LogisticDate, logisticDates[1])
		require.Equal(t, wantService2LogisticDate, logisticDates[2])
		require.Equal(t, wantService3LogisticDate, logisticDates[3])
		require.Equal(t, wantService4LogisticDate, logisticDates[4])
		require.Equal(t, wantService5LogisticDate, logisticDates[5])
		require.Equal(t, wantService6LogisticDate, logisticDates[6])
	}
}

func TestCalcLogisticDatesForSinglePath(t *testing.T) {
	{
		wantServiceTimeList := ServiceTimeList{
			// Warehouse
			// Не попадает в предыдущие логистические сутки
			// -> 10.06.2021
			NewServiceTime(
				-100,
				time.Date(2021, 06, 10, 13, 40, 0, 0, time.UTC),
			),
			// Movement
			// Не попадает в предыдущие логистические сутки предшествующего склада
			// -> 10.06.2021
			NewServiceTime(
				-101,
				time.Date(2021, 06, 10, 15, 40, 0, 0, time.UTC),
			),
			// Warehouse
			// Попадает в предыдущие логистические сутки
			// -> 10.06.2021
			NewServiceTime(
				-102,
				time.Date(2021, 06, 11, 02, 40, 0, 0, time.UTC),
			),
			// Movement
			// Попадает в предыдущие логистические сутки предшествующего склада
			// -> 10.06.2021
			NewServiceTime(
				-103,
				time.Date(2021, 06, 11, 04, 40, 0, 0, time.UTC),
			),
			// Warehouse
			// Попадает в предыдущие логистические сутки
			// -> 11.06.2021
			NewServiceTime(
				-104,
				time.Date(2021, 06, 12, 2, 40, 0, 0, time.UTC),
			),
			// Movement
			// НЕ попадает в предыдущие логистические сутки предшествующего склада
			// -> 12.06.2021
			NewServiceTime(
				-105,
				time.Date(2021, 06, 12, 07, 40, 0, 0, time.UTC),
			),
			// Warehouse
			// НЕ попадает в предыдущие логистические сутки
			// -> 12.06.2021
			NewServiceTime(
				-106,
				time.Date(2021, 06, 12, 12, 40, 0, 0, time.UTC),
			),
			// Movement
			// Попадает в предыдущие логистические сутки предшествующего склада
			// -> 12.06.2021
			NewServiceTime(
				-107,
				time.Date(2021, 06, 13, 01, 40, 0, 0, time.UTC),
			),
			// Linehaul
			// С учётом часового пояса с предшествующего склада дата остаётся той же
			// -> 13.06.2021
			NewServiceTime(
				-108,
				time.Date(2021, 06, 13, 17, 40, 0, 0, time.UTC),
			),
			// Linehaul
			// С учётом часового пояса с предшествующего склада дата уходит на день вперёд
			// -> 15.06.2021
			NewServiceTime(
				-109,
				time.Date(2021, 06, 14, 22, 40, 0, 0, time.UTC),
			),
			// Handing
			// С учётом часового пояса региона сегмента дата остаётся той же
			// -> 15.06.2021
			NewServiceTime(
				-110,
				time.Date(2021, 06, 15, 20, 40, 0, 0, time.UTC),
			),
			// Handing
			// С учётом часового пояса региона сегмента дата уходит на день вперёд
			// -> 17.06.2021
			NewServiceTime(
				-111,
				time.Date(2021, 06, 16, 23, 40, 0, 0, time.UTC),
			),
		}

		regionMap := geobase.RegionMap{

			geobase.RegionSofyno: {
				{
					ID:       120013,
					Parent:   161541,
					Name:     "Софьино",
					Type:     7,
					TzOffset: 10800,
				},
				{
					ID:       161541,
					Parent:   98605,
					Name:     "Софьинское сельское поселение",
					Type:     15,
					TzOffset: 10800,
				},
				{
					ID:       98605,
					Parent:   1,
					Name:     "Раменский район",
					Type:     10,
					TzOffset: 10800,
				},
				{
					ID:       1,
					Parent:   3,
					Name:     "Москва и Московская область",
					Type:     5,
					TzOffset: 10800,
				},
				{
					ID:       3,
					Parent:   225,
					Name:     "Центральный федеральный округ",
					Type:     4,
					TzOffset: 10800,
				},
				{
					ID:       225,
					Parent:   10001,
					Name:     "Россия",
					Type:     3,
					TzOffset: 10800,
				},
			},
			geobase.RegionMoscow: {
				{
					ID:       213,
					Parent:   1,
					Name:     "Москва",
					Type:     6,
					TzOffset: 10800,
				},
				{
					ID:       1,
					Parent:   3,
					Name:     "Москва и Московская область",
					Type:     5,
					TzOffset: 10800,
				},
				{
					ID:       3,
					Parent:   225,
					Name:     "Центральный федеральный округ",
					Type:     4,
					TzOffset: 10800,
				},
				{
					ID:       225,
					Parent:   10001,
					Name:     "Россия",
					Type:     3,
					TzOffset: 10800,
				},
			},
		}
		{
			wantedLogisticDates := []time.Time{
				time.Date(2021, 06, 10, 0, 0, 0, 0, timex.FixedZone("CUSTOM_TZ", 10800)),
				time.Date(2021, 06, 10, 0, 0, 0, 0, timex.FixedZone("CUSTOM_TZ", 10800)),
				time.Date(2021, 06, 10, 0, 0, 0, 0, timex.FixedZone("CUSTOM_TZ", 10800)),
				time.Date(2021, 06, 10, 0, 0, 0, 0, timex.FixedZone("CUSTOM_TZ", 10800)),
				time.Date(2021, 06, 11, 0, 0, 0, 0, timex.FixedZone("CUSTOM_TZ", 10800)),
				time.Date(2021, 06, 12, 0, 0, 0, 0, timex.FixedZone("CUSTOM_TZ", 10800)),
				time.Date(2021, 06, 12, 0, 0, 0, 0, timex.FixedZone("CUSTOM_TZ", 10800)),
				time.Date(2021, 06, 12, 0, 0, 0, 0, timex.FixedZone("CUSTOM_TZ", 10800)),
				time.Date(2021, 06, 13, 0, 0, 0, 0, timex.FixedZone("CUSTOM_TZ", 10800)),
				time.Date(2021, 06, 15, 0, 0, 0, 0, timex.FixedZone("CUSTOM_TZ", 10800)),
				time.Date(2021, 06, 15, 0, 0, 0, 0, timex.FixedZone("CUSTOM_TZ", 10800)),
				time.Date(2021, 06, 17, 0, 0, 0, 0, timex.FixedZone("CUSTOM_TZ", 10800)),
			}

			pb := NewPathBuilder()

			pb.AddWarehouse(
				pb.MakeShipmentService(
					// Не попадает в предыдущие логистические сутки
					// -> 10.06.2021
					pb.WithStartTime(wantServiceTimeList[0].StartTime()),
					pb.WithServiceID(wantServiceTimeList[0].ServiceID),
				),
				pb.WithLogisticDayStart(timex.DayTime{Hour: 8}),
				pb.WithLocation(geobase.RegionSofyno),
			)
			pb.AddMovement(
				pb.MakeMovementService(
					// Не попадает в предыдущие логистические сутки предшествующего склада
					// -> 10.06.2021
					pb.WithStartTime(wantServiceTimeList[1].StartTime()),
					pb.WithServiceID(wantServiceTimeList[1].ServiceID),
				),
				pb.WithLocation(geobase.RegionSofyno),
			)

			pb.AddWarehouse(
				pb.MakeShipmentService(
					// Попадает в предыдущие логистические сутки
					// -> 10.06.2021
					pb.WithStartTime(wantServiceTimeList[2].StartTime()),
					pb.WithServiceID(wantServiceTimeList[2].ServiceID),
				),
				pb.WithLogisticDayStart(timex.DayTime{Hour: 8}),
				pb.WithLocation(geobase.RegionMoscow),
			)
			pb.AddMovement(
				pb.MakeMovementService(
					// Попадает в предыдущие логистические сутки предшествующего склада
					// -> 10.06.2021
					pb.WithStartTime(wantServiceTimeList[3].StartTime()),
					pb.WithServiceID(wantServiceTimeList[3].ServiceID),
				),
				pb.WithLocation(geobase.RegionMoscow),
			)

			pb.AddWarehouse(
				pb.MakeShipmentService(
					// Попадает в предыдущие логистические сутки
					// -> 11.06.2021
					pb.WithStartTime(wantServiceTimeList[4].StartTime()),
					pb.WithServiceID(wantServiceTimeList[4].ServiceID),
				),
				pb.WithLogisticDayStart(timex.DayTime{Hour: 9}),
				pb.WithLocation(geobase.RegionMoscow),
			)
			pb.AddMovement(
				pb.MakeMovementService(
					// НЕ попадает в предыдущие логистические сутки предшествующего склада
					// -> 12.06.2021
					pb.WithStartTime(wantServiceTimeList[5].StartTime()),
					pb.WithServiceID(wantServiceTimeList[5].ServiceID),
				),
				pb.WithLocation(geobase.RegionMoscow),
			)

			pb.AddWarehouse(
				pb.MakeShipmentService(
					// НЕ попадает в предыдущие логистические сутки
					// -> 12.06.2021
					pb.WithStartTime(wantServiceTimeList[6].StartTime()),
					pb.WithServiceID(wantServiceTimeList[6].ServiceID),
				),
				pb.WithLogisticDayStart(timex.DayTime{Hour: 9}),
				pb.WithLocation(geobase.RegionMoscow),
			)
			pb.AddMovement(
				pb.MakeMovementService(
					// Попадает в предыдущие логистические сутки предшествующего склада
					// -> 12.06.2021
					pb.WithStartTime(wantServiceTimeList[7].StartTime()),
					pb.WithServiceID(wantServiceTimeList[7].ServiceID),
				),
				pb.WithLocation(geobase.RegionMoscow),
			)

			pb.AddLinehaul(
				pb.MakeDeliveryService(
					// С учётом часового пояса с предшествующего склада дата остаётся той же
					// -> 13.06.2021
					pb.WithStartTime(wantServiceTimeList[8].StartTime()),
					pb.WithServiceID(wantServiceTimeList[8].ServiceID),
				),

				pb.MakeLastMileService(
					// С учётом часового пояса с предшествующего склада дата уходит на день вперёд
					// -> 15.06.2021
					pb.WithStartTime(wantServiceTimeList[9].StartTime()),
					pb.WithServiceID(wantServiceTimeList[9].ServiceID),
				),
			)

			pb.AddHanding(
				pb.MakeHandingService(
					// С учётом часового пояса региона сегмента дата остаётся той же
					// -> 15.06.2021
					pb.WithStartTime(wantServiceTimeList[10].StartTime()),
					pb.WithServiceID(wantServiceTimeList[10].ServiceID),
					pb.WithDeliveryType(DeliveryTypeCourier),
				),
				pb.WithLocation(geobase.RegionMoscow),
			)

			pb.AddHanding(
				pb.MakeHandingService(
					// С учётом часового пояса региона сегмента дата уходит на день вперёд
					// -> 17.06.2021
					pb.WithStartTime(wantServiceTimeList[11].StartTime()),
					pb.WithServiceID(wantServiceTimeList[11].ServiceID),
					pb.WithDeliveryType(DeliveryTypeCourier),
				),
				pb.WithLocation(geobase.RegionMoscow),
			)

			path := pb.GetSortablePath()

			var previousLogisticDayStart = timex.DayTime{}
			previousTZOffset := 0
			for i := 0; i < 12; i++ {
				var tzOffset int
				var logisticDate time.Time
				//8 и 9 сервисы находятся в одном сегменте, поэтому после i == 8 нужно брать сегмент [i-1]
				if i < 9 {
					tzOffset, logisticDate, previousTZOffset, previousLogisticDayStart = CalcLogisticDateAndTZOffset(
						regionMap,
						int64(-100-i),
						previousLogisticDayStart,
						previousTZOffset,
						path.Nodes[i],
						path.ServiceTimeList,
						213,
					)
				} else {
					tzOffset, logisticDate, previousTZOffset, previousLogisticDayStart = CalcLogisticDateAndTZOffset(
						regionMap,
						int64(-100-i),
						previousLogisticDayStart,
						previousTZOffset,
						path.Nodes[i-1],
						path.ServiceTimeList,
						213,
					)
				}
				require.Equal(t, wantedLogisticDates[i], logisticDate, i)
				require.Equal(t, 10800, tzOffset, i)
			}
		}
	}
}
