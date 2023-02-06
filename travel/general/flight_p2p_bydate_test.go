package flightp2p

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	dto "a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/DTO"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/flight_p2p/format"
	"a.yandex-team.ru/travel/avia/shared_flights/api/pkg/structs"
	"a.yandex-team.ru/travel/avia/shared_flights/lib/go/dtutil"
	"a.yandex-team.ru/travel/proto/shared_flights/snapshots"
)

func TestStorageService_GetFlightsP2PByDate_FailOnInvalidDatesRange(t *testing.T) {
	h := serviceTestHelper(t)
	_, err := h.Service.GetFlightsP2PByDate(
		[]*snapshots.TStationWithCodes{h.Station.SVX},
		[]*snapshots.TStationWithCodes{h.Station.SVX},
		NewLocalTime("2020-12-31 23:00:00"),
		20,
		"",
		true,
		false,
	)
	assert.EqualError(
		t, err,
		"invalid days count: 20",
		"should fail on invalid date range",
	)
}

func TestStorageService_GetFlightsP2PByDate_FailOnInvalidRequestStationTimezone(t *testing.T) {
	h := serviceTestHelper(t)
	_, err := h.Service.GetFlightsP2PByDate(
		[]*snapshots.TStationWithCodes{h.Station.InvalidTimezone},
		[]*snapshots.TStationWithCodes{h.Station.SVX},
		NewLocalTime("2020-01-01 23:00:00"),
		2,
		"",
		true,
		false,
	)
	assert.EqualError(
		t, err,
		"cannot load timezone for station  102",
		"should fail on invalid timezone",
	)
}

func TestStorageService_GetFlightsP2PByDate_SingleFlight(t *testing.T) {
	h := serviceTestHelper(t)
	expect := format.DirectDatesAndFlights{
		DepartureStations: []int32{100},
		ArrivalStations:   []int32{101},
		FlightDepartures: []format.FlightDeparture{
			{
				TitledFlight: dto.TitledFlight{
					FlightID: dto.FlightID{
						AirlineID: 26,
						Number:    "5555",
					},
					Title: "SU 5555",
				},
				DepartureDatetime: "2019-12-30T05:00:00+05:00",
				DepartureTerminal: "A",
				DepartureStation:  100,
				ArrivalStation:    101,
				Source:            structs.UnknownSource,
			},
			{
				TitledFlight: dto.TitledFlight{
					FlightID: dto.FlightID{
						AirlineID: 26,
						Number:    "5555",
					},
					Title: "SU 5555",
				},
				DepartureDatetime: "2019-12-31T05:00:00+05:00",
				DepartureTerminal: "A",
				DepartureStation:  100,
				ArrivalStation:    101,
				Source:            structs.UnknownSource,
			},
			{
				TitledFlight: dto.TitledFlight{
					FlightID: dto.FlightID{
						AirlineID: 26,
						Number:    "5555",
					},
					Title: "SU 5555",
				},
				DepartureDatetime: "2020-01-01T05:00:00+05:00",
				DepartureTerminal: "A",
				DepartureStation:  100,
				ArrivalStation:    101,
				Source:            structs.UnknownSource,
			},
		},
		Dates: []string{"2019-12-30", "2019-12-31", "2020-01-01"},
	}
	got, err := h.Service.GetFlightsP2PByDate(
		[]*snapshots.TStationWithCodes{h.Station.SVX},
		[]*snapshots.TStationWithCodes{h.Station.JFK},
		NewLocalTime("2019-12-31 23:00:00"),
		1,
		"",
		true,
		true,
	)

	assert.NoError(t, err, "cannot get flights p2p")
	assert.Equal(t,
		expect,
		got,
		"incorrect flights p2p response",
	)
}

func TestStorageService_GetFlightsP2PByDate_DopFlight(t *testing.T) {
	h := serviceTestHelper(t)
	expect := format.DirectDatesAndFlights{
		DepartureStations: []int32{9600215},
		ArrivalStations:   []int32{100},
		Dates:             []string{"2020-04-06", "2020-04-07"},
		FlightDepartures: []format.FlightDeparture{
			{
				TitledFlight: dto.TitledFlight{
					FlightID: dto.FlightID{
						AirlineID: 909,
						Number:    "909",
					},
					Title: "DP 909",
				},
				DepartureDatetime: "2020-04-06T15:50:00+03:00",
				DepartureStation:  9600215,
				ArrivalStation:    100,
				Source:            structs.UnknownSource,
			},
			{
				TitledFlight: dto.TitledFlight{
					FlightID: dto.FlightID{
						AirlineID: 909,
						Number:    "909",
					},
					Title: "DP 909",
				},
				DepartureDatetime: "2020-04-07T15:50:00+03:00",
				DepartureStation:  9600215,
				ArrivalStation:    100,
				Source:            structs.UnknownSource,
			},
		},
	}
	got, err := h.Service.GetFlightsP2PByDate(
		[]*snapshots.TStationWithCodes{h.Station.VKO},
		[]*snapshots.TStationWithCodes{h.Station.SVX},
		NewLocalTime("2020-04-06 01:00:00"),
		1,
		"",
		true,
		true,
	)

	assert.NoError(t, err, "cannot get flights p2p")
	assert.Equal(t,
		expect,
		got,
		"incorrect flights p2p response",
	)
}

func TestStorageService_GetFlightsP2PByDate_TimeOutOfSearchRange(t *testing.T) {
	h := serviceTestHelper(t)
	got, err := h.Service.GetFlightsP2PByDate(
		[]*snapshots.TStationWithCodes{h.Station.SVX},
		[]*snapshots.TStationWithCodes{h.Station.JFK},
		NewLocalTime("2020-04-10 00:00:00"),
		1,
		"",
		true,
		true,
	)
	assert.Equal(
		t,
		format.DirectDatesAndFlights{
			DepartureStations: []int32{100},
			ArrivalStations:   []int32{101},
			Dates:             []string{},
			FlightDepartures:  make([]format.FlightDeparture, 0),
		},
		got,
	)
	assert.NoError(t, err, "should be no error for no matching flights")
}

/*
   Этот тест иллюстрирует дыру в эвристике, описанную в заголовке функции GetFlightsP2PByDate: неточность предположения,
   что маршрутка, вылетевшая из пункта А в день D прилетит в пункт Б, если она хотя бы иногда туда прилетает.
   Поэтому здесь станция прилёта рейса не соответствует станции прилёта запроса.
   Т.к. эти станции выводятся только при debug==true, для эксплуатанта сервиса это не будет заметно.
   Как чинить дыру в эвристике, см. в комментарии к функции GetFlightsP2PByDate.
*/
func TestStorageService_GetFlightsP2PByDate_TwoLegs(t *testing.T) {
	h := serviceTestHelper(t)
	expect := format.DirectDatesAndFlights{
		DepartureStations: []int32{100},
		ArrivalStations:   []int32{9600366},
		Dates:             []string{"2020-04-08"},
		FlightDepartures: []format.FlightDeparture{
			{
				TitledFlight: dto.TitledFlight{
					FlightID: dto.FlightID{
						AirlineID: 9144,
						Number:    "408",
					},
					Title: "DP 408",
				},
				DepartureDatetime: "2020-04-08T12:34:00+05:00",
				DepartureTerminal: "B",
				DepartureStation:  100,
				ArrivalStation:    9600215,
			},
		},
	}
	got, err := h.Service.GetFlightsP2PByDate(
		[]*snapshots.TStationWithCodes{h.Station.SVX},
		[]*snapshots.TStationWithCodes{h.Station.LED},
		NewLocalTime("2020-04-08 01:00:00"),
		1,
		"",
		true,
		true,
	)

	assert.NoError(t, err, "cannot get flights p2p")
	assert.Equal(t,
		expect,
		got,
		"incorrect flights p2p response",
	)
}

func TestStorageService_GetFlightsP2PByDate_Multiroute(t *testing.T) {
	h := serviceTestHelper(t)
	expect := format.DirectDatesAndFlights{
		DepartureStations: []int32{101},
		ArrivalStations:   []int32{9600215},
		Dates:             []string{"2020-05-03", "2020-05-04", "2020-05-05"},
		FlightDepartures: []format.FlightDeparture{
			{
				TitledFlight: dto.TitledFlight{
					FlightID: dto.FlightID{
						AirlineID: 9144,
						Number:    "707",
					},
					Title: "DP 707",
				},
				DepartureDatetime: "2020-05-03T20:30:00-04:00",
				DepartureStation:  101,
				ArrivalStation:    9600215,
			},
			{
				TitledFlight: dto.TitledFlight{
					FlightID: dto.FlightID{
						AirlineID: 9144,
						Number:    "707",
					},
					Title: "DP 707",
				},
				DepartureDatetime: "2020-05-04T20:30:00-04:00",
				DepartureStation:  101,
				ArrivalStation:    9600215,
			},
			{
				TitledFlight: dto.TitledFlight{
					FlightID: dto.FlightID{
						AirlineID: 9144,
						Number:    "707",
					},
					Title: "DP 707",
				},
				DepartureDatetime: "2020-05-05T20:30:00-04:00",
				DepartureStation:  101,
				ArrivalStation:    9600215,
			},
		},
	}
	got, err := h.Service.GetFlightsP2PByDate(
		[]*snapshots.TStationWithCodes{h.Station.JFK},
		[]*snapshots.TStationWithCodes{h.Station.VKO},
		NewLocalTime("2020-05-04 01:00:00"),
		1,
		"",
		true,
		true,
	)

	assert.NoError(t, err, "cannot get flights p2p")
	assert.Equal(t,
		expect,
		got,
		"incorrect flights p2p response",
	)
}

func Test_AtLeastOneBackwardDateIsEqualOrAfterForwardDate(t *testing.T) {
	assert.False(t, AtLeastOneBackwardDateIsEqualOrAfterForwardDate([]string{}, []string{"2022-05-05"}))
	assert.False(t, AtLeastOneBackwardDateIsEqualOrAfterForwardDate([]string{"2022-05-06"}, []string{}))

	assert.False(t, AtLeastOneBackwardDateIsEqualOrAfterForwardDate([]string{"2022-05-05"}, []string{"2022-05-04"}))
	assert.True(t, AtLeastOneBackwardDateIsEqualOrAfterForwardDate([]string{"2022-05-05"}, []string{"2022-05-05"}))
	assert.True(t, AtLeastOneBackwardDateIsEqualOrAfterForwardDate([]string{"2022-05-05"}, []string{"2022-05-06"}))

	assert.True(
		t,
		AtLeastOneBackwardDateIsEqualOrAfterForwardDate(
			[]string{"2022-05-05", "2022-05-06"},
			[]string{"2022-05-05"},
		),
	)

	assert.False(
		t,
		AtLeastOneBackwardDateIsEqualOrAfterForwardDate(
			[]string{"2022-05-05", "2022-05-06", "2022-05-07"},
			[]string{"2022-05-03", "2022-05-04"},
		),
	)
}

func NewLocalTime(timeString string) TimeParam {
	value, _ := time.ParseInLocation(dtutil.IsoDateTime, timeString, time.UTC)
	return TimeParam{
		Value:   value,
		IsLocal: true,
	}
}
