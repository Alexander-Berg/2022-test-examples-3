package storage

import (
	"reflect"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/flightdata"
	dto "a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/DTO"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/flightboard"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/flightboard/format"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/flightstatus"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/threads"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/timezone"
	storageCache "a.yandex-team.ru/travel/avia/shared_flights/api/internal/storage"
	"a.yandex-team.ru/travel/avia/shared_flights/api/pkg/structs"
	"a.yandex-team.ru/travel/avia/shared_flights/lib/go/direction"
	"a.yandex-team.ru/travel/proto/dicts/rasp"
	"a.yandex-team.ru/travel/proto/shared_flights/snapshots"
)

type mockTimezoneProvider struct{}

const noFilter = ""

func (m mockTimezoneProvider) GetTimeZoneByStationID(int64) *time.Location {
	return time.UTC
}

func TestStorageService_GetFlightBoard_FailOnInvalidDirection(t *testing.T) {
	h := serviceTestHelper(t)
	_, err := h.Service.GetFlightBoard(h.Station.SVX, time.Date(2019, 12, 31, 23, 0, 0, 0, time.UTC), time.Date(2020, 1, 1, 1, 0, 0, 0, time.UTC), 0, true, 0, "", "", noFilter, time.Date(2021, 1, 1, 1, 0, 0, 0, time.UTC))
	assert.EqualError(t, err, "invalid direction: Unknown direction (0)", "should fail on invalid direction")
	_, err = h.Service.GetFlightBoard(h.Station.SVX, time.Date(2019, 12, 31, 23, 0, 0, 0, time.UTC), time.Date(2020, 1, 1, 1, 0, 0, 0, time.UTC), 0, true, 100, "", "", noFilter, time.Date(2021, 1, 1, 1, 0, 0, 0, time.UTC))
	assert.EqualError(t, err, "invalid direction: Unknown direction (100)", "should fail on invalid direction")
}

func TestStorageService_GetFlightBoard_FailOnInvalidDatesRange(t *testing.T) {
	h := serviceTestHelper(t)
	_, err := h.Service.GetFlightBoard(h.Station.SVX, time.Date(2020, 12, 31, 23, 0, 0, 0, time.UTC), time.Date(2020, 1, 1, 1, 0, 0, 0, time.UTC), 0, true, direction.DEPARTURE, "", "", noFilter, time.Date(2021, 1, 1, 1, 0, 0, 0, time.UTC))
	assert.EqualError(
		t, err,
		"invalid date range: (2020-12-31 23:00:00 +0000 UTC) - (2020-01-01 01:00:00 +0000 UTC)",
		"should fail on invalid date range",
	)
}

func TestStorageService_GetFlightBoard_FailOnInvalidRequestStationTimezone(t *testing.T) {
	h := serviceTestHelper(t)
	_, err := h.Service.GetFlightBoard(h.Station.InvalidTimezone, time.Date(2019, 12, 31, 23, 0, 0, 0, time.UTC), time.Date(2020, 1, 1, 1, 0, 0, 0, time.UTC), 0, true, direction.DEPARTURE, "", "", noFilter, time.Date(2021, 1, 1, 1, 0, 0, 0, time.UTC))
	assert.EqualError(
		t, err,
		"cannot load timezone for station  102",
		"should fail on invalid timezone",
	)
}

func TestStorageService_GetFlightBoard_DepartureSingleFlightNoCodeshareNoStatus(t *testing.T) {
	h := serviceTestHelper(t)
	expect := format.Response{
		Station:   100,
		Direction: "departure",
		Flights: []format.Flight{
			format.Flight{
				TitledFlight: dto.TitledFlight{
					FlightID: dto.FlightID{
						AirlineID: 26,
						Number:    "5555",
					},
					Title: "SU 5555",
				},
				Datetime:      "2020-01-01T05:00:00+05:00",
				StartDatetime: "2020-01-01T05:00:00+05:00",
				Terminal:      "A",
				Codeshares:    nil,
				Status: flightstatus.FlightStatus{
					Status:          "unknown",
					DepartureStatus: "unknown",
					ArrivalStatus:   "unknown",
					DepartureSource: "0",
					ArrivalSource:   "0",
				},
				Route:       dto.Route{100, 101},
				StationFrom: 100,
				StationTo:   101,
			},
		},
	}
	got, err := h.Service.GetFlightBoard(h.Station.SVX, time.Date(2019, 12, 31, 23, 0, 0, 0, time.UTC), time.Date(2020, 1, 1, 1, 0, 0, 0, time.UTC), 0, true, direction.DEPARTURE, "", "", noFilter, time.Date(2021, 1, 1, 1, 0, 0, 0, time.UTC))

	assert.NoError(t, err, "cannot get flight board")
	assert.Equal(t,
		expect,
		got,
		"incorrect flight board response",
	)
}

func TestStorageService_GetFlightBoard_ArrivalSingleFlightNoCodeshareNoStatus(t *testing.T) {
	h := serviceTestHelper(t)
	expect := format.Response{
		Station:   101,
		Direction: "arrival",
		Flights: []format.Flight{
			{
				TitledFlight: dto.TitledFlight{
					FlightID: dto.FlightID{
						AirlineID: 26,
						Number:    "5555",
					},
					Title: "SU 5555",
				},
				Datetime:      "2020-01-01T14:00:00-05:00",
				StartDatetime: "2020-01-01T05:00:00+05:00",
				Terminal:      "B",
				Codeshares:    nil,
				Status: flightstatus.FlightStatus{
					Status:          "unknown",
					DepartureStatus: "unknown",
					ArrivalStatus:   "unknown",
					DepartureSource: "0",
					ArrivalSource:   "0",
				},
				Route:       dto.Route{100, 101},
				StationFrom: 100,
				StationTo:   101,
			},
		},
	}
	got, err := h.Service.GetFlightBoard(h.Station.JFK, time.Date(2020, 1, 1, 18, 0, 0, 0, time.UTC), time.Date(2020, 1, 1, 20, 0, 0, 0, time.UTC), 0, true, direction.ARRIVAL, "", "", noFilter, time.Date(2021, 1, 1, 1, 0, 0, 0, time.UTC))

	assert.NoError(t, err, "cannot get flight board")
	assert.Equal(t,
		expect,
		got,
		"incorrect flight board response",
	)
}

func TestStorageService_GetFlightBoard_ArrivalWithLimit(t *testing.T) {
	h := serviceTestHelper(t)
	/*
		RASPTICKETS-19595: flight that departs earlier and arrives later pushes the valid flight out of the limit.
		For the bug to appear, the following conditions shall be met:
		+ JFKVKO departs on the previous day and on the same day as SVXVKO
		+ JFKVKO arrives earlier than SVXVKO and on the same day
		+ JFKVKO flies overnight
		It is important to keep these conditions, in case of any changes in the test flights.
	*/
	expect := format.Response{
		Station:   9600215,
		Direction: "arrival",
		Flights: []format.Flight{
			{
				TitledFlight: dto.TitledFlight{
					FlightID: dto.FlightID{
						AirlineID: 9144,
						Number:    "404",
					},
					Title: "DP 404",
				},
				Datetime:      "2020-04-06T13:45:00+03:00",
				StartDatetime: "2020-04-06T12:34:00+05:00",
				Terminal:      "C",
				Codeshares:    nil,
				Status: flightstatus.FlightStatus{
					Status:          "unknown",
					DepartureStatus: "unknown",
					ArrivalStatus:   "unknown",
					DepartureSource: "0",
					ArrivalSource:   "0",
				},
				Route:       dto.Route{100, 9600215},
				StationFrom: 100,
				StationTo:   9600215,
			},
		},
	}
	got, err := h.Service.GetFlightBoard(
		h.Station.VKO,
		time.Date(2020, 4, 6, 10, 0, 0, 0, time.UTC),
		time.Date(2020, 4, 6, 20, 0, 0, 0, time.UTC),
		1,
		true,
		direction.ARRIVAL,
		"",
		"",
		noFilter,
		time.Date(2020, 5, 1, 1, 0, 0, 0, time.UTC),
	)

	assert.NoError(t, err, "cannot get flight board")
	assert.Equal(t,
		expect,
		got,
		"incorrect flight board response",
	)
}

func TestStorageService_GetFlightBoard_DepartureSingleFlightNoCodeshareWithStatus(t *testing.T) {
	// Invalid test
	h := serviceTestHelper(t)
	expect := format.Response{
		Station:   100,
		Direction: "departure",
		Flights: []format.Flight{
			{
				TitledFlight: dto.TitledFlight{
					FlightID: dto.FlightID{
						AirlineID: 26,
						Number:    "5555",
					},
					Title: "SU 5555",
				},
				Datetime:      "2021-02-02T05:00:00+05:00",
				StartDatetime: "2021-02-02T05:00:00+05:00",
				Terminal:      "A",
				Codeshares:    nil,
				Status: flightstatus.FlightStatus{
					Status:            "delayed",
					DepartureDT:       "2021-02-02 05:20:00",
					DepartureStatus:   "delayed",
					ArrivalStatus:     "unknown",
					DepartureGate:     "111",
					DepartureTerminal: "CCC",
					DepartureSource:   "3",
					ArrivalSource:     "3",
					CreatedAtUTC:      "2021-01-20 13:00:00",
					UpdatedAtUTC:      "2021-01-01 07:35:46",
				},
				Route:       dto.Route{100, 101},
				StationFrom: 100,
				StationTo:   101,
			},
		},
	}
	got, err := h.Service.GetFlightBoard(h.Station.SVX, time.Date(2021, 2, 1, 23, 0, 0, 0, time.UTC), time.Date(2021, 2, 2, 1, 0, 0, 0, time.UTC), 0, true, direction.DEPARTURE, "", "", noFilter, time.Date(2021, 1, 1, 1, 0, 0, 0, time.UTC))

	assert.NoError(t, err, "cannot get flight board")
	assert.Equal(t,
		expect,
		got,
		"incorrect flight board response",
	)
}

func TestStorageService_GetFlightBoard_ArrivalDelayedWithScheduledInStatus(t *testing.T) {
	h := serviceTestHelper(t)
	now := time.Date(2020, 1, 20, 13, 40, 0, 0, time.UTC)
	got, err := h.Service.GetFlightBoard(h.Station.DME, now.Add(-time.Hour), now.Add(24*time.Hour), 0, true, direction.ARRIVAL, "", "", noFilter, now)
	assert.NoError(t, err)

	expected := flightstatus.FlightStatus{
		Status:              "on_time",
		ArrivalDT:           "2020-01-20 17:05:00",
		DepartureStatus:     "unknown",
		ArrivalStatus:       "early",
		DepartureSource:     "3",
		ArrivalSource:       "3",
		CreatedAtUTC:        "2020-01-17 21:21:10",
		UpdatedAtUTC:        "2020-01-20 14:00:00",
		ArrivalUpdatedAtUTC: "2020-01-20 14:00:01",
		Diverted:            false,
	}
	assert.Len(t, got.Flights, 1)
	assert.Equal(t, expected, got.Flights[0].Status)
}

func TestStorageService_GetFlightBoard_DepartureDelayedWithActualInStatus(t *testing.T) {
	h := serviceTestHelper(t)
	now := time.Date(2020, 7, 16, 9, 40, 0, 0, time.UTC)
	got, err := h.Service.GetFlightBoard(
		h.Station.LED,
		now.Add(-12*time.Hour), // midnight in LED
		now.Add(-11*time.Hour), // 1am in LED
		0,
		true,
		direction.DEPARTURE,
		"",
		"",
		noFilter,
		now,
	)
	assert.NoError(t, err)

	expected := format.Response{
		Station:   9600366,
		Direction: "departure",
		Flights: []format.Flight{
			{
				TitledFlight: dto.TitledFlight{
					FlightID: dto.FlightID{
						AirlineID: 26,
						Number:    "6011",
					},
					Title: "SU 6011",
				},
				Datetime:      "2020-07-15T05:50:00+03:00",
				StartDatetime: "2020-07-15T05:50:00+03:00",
				Codeshares:    nil,
				Status: flightstatus.FlightStatus{
					Status:              "arrived",
					DepartureDT:         "2020-07-16 00:45:00",
					ArrivalDT:           "2020-07-15 20:15:00",
					DepartureStatus:     "departed",
					ArrivalStatus:       "arrived",
					DepartureSource:     "3",
					ArrivalSource:       "3",
					CreatedAtUTC:        "2020-01-21 21:21:10",
					UpdatedAtUTC:        "2020-01-25 14:00:00",
					ArrivalUpdatedAtUTC: "2020-01-25 14:00:00",
				},
				Route:       dto.Route{9600366, 9600215},
				StationFrom: 9600366,
				StationTo:   9600215,
			},
		},
	}
	assert.Equal(t, expected, got)
}

func TestStorageService_GetFlightBoard_ArrivalOutOfSearchRangeButClose(t *testing.T) {
	h := serviceTestHelper(t)
	now := time.Date(2020, 1, 25, 02, 0, 0, 0, time.UTC)
	got, err := h.Service.GetFlightBoard(h.Station.VKO, now.Add(-time.Hour), now.Add(24*time.Hour), 0, true, direction.ARRIVAL, "", "", noFilter, now)
	assert.NoError(t, err, "no flights is okay")
	assert.Equal(
		t,
		format.Response{
			Station:   9600215,
			Direction: "arrival",
			Flights:   make([]format.Flight, 0),
		},
		got,
	)
}

func TestStorageService_GetFlightBoard_TwoInconsitentLegsOfTheSameFlight(t *testing.T) {
	h := serviceTestHelper(t)
	expect := format.Response{
		Station:   100,
		Direction: "departure",
		Flights: []format.Flight{
			{
				TitledFlight: dto.TitledFlight{
					FlightID: dto.FlightID{
						AirlineID: 9144,
						Number:    "999",
					},
					Title: "DP 999",
				},
				Datetime:      "2020-07-04T11:33:00+05:00",
				StartDatetime: "2020-07-04T11:33:00+05:00",
				Terminal:      "B",
				Codeshares:    nil,
				Status: flightstatus.FlightStatus{
					Status:          "unknown",
					DepartureStatus: "unknown",
					ArrivalStatus:   "unknown",
					DepartureSource: "0",
					ArrivalSource:   "0",
				},
				Route:       dto.Route{100, 9600366},
				StationFrom: 100,
				StationTo:   9600366,
			},
		},
	}
	got, err := h.Service.GetFlightBoard(
		h.Station.SVX,
		time.Date(2020, 7, 4, 5, 0, 0, 0, time.UTC),
		time.Date(2020, 7, 4, 23, 59, 0, 0, time.UTC),
		0,
		true,
		direction.DEPARTURE,
		"",
		"",
		noFilter,
		time.Date(2021, 1, 1, 1, 0, 0, 0, time.UTC),
	)

	assert.NoError(t, err, "cannot get flight board")
	assert.Equal(t,
		expect,
		got,
		"incorrect flight board response",
	)
}

func TestStorageService_DateTimeFilter(t *testing.T) {
	type fields struct {
		direction direction.Direction
		from      time.Time
		to        time.Time
	}
	type args struct {
		flightdata flightboard.FlightOnDateWithCodeshares
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   flightboard.DateTimeFilterResult
	}{
		{
			"valid on left edge of period",
			fields{
				direction: direction.DEPARTURE,
				from:      time.Date(2020, 1, 1, 12, 0, 0, 0, time.UTC),
				to:        time.Date(2020, 1, 1, 13, 0, 0, 0, time.UTC),
			},
			args{
				flightdata: flightboard.FlightOnDateWithCodeshares{
					FlightData: flightdata.NewFlightData(
						structs.FlightBase{
							DepartureTimeScheduled: 1200,
						},
						nil,
						nil,
						20200101,
						&mockTimezoneProvider{},
					),
				},
			},
			flightboard.FitsByScheduledTime,
		},
		{
			"invalid outside of left edge of period",
			fields{
				direction: direction.DEPARTURE,
				from:      time.Date(2020, 1, 1, 12, 0, 0, 0, time.UTC),
				to:        time.Date(2020, 1, 1, 13, 0, 0, 0, time.UTC),
			},
			args{
				flightdata: flightboard.FlightOnDateWithCodeshares{
					FlightData: flightdata.NewFlightData(
						structs.FlightBase{
							DepartureTimeScheduled: 1159,
						},
						nil,
						nil,
						20200101,
						&mockTimezoneProvider{},
					),
				},
			},
			flightboard.DoesNotFit,
		},
		{
			"valid on right edge of period",
			fields{
				direction: direction.DEPARTURE,
				from:      time.Date(2020, 1, 1, 12, 0, 0, 0, time.UTC),
				to:        time.Date(2020, 1, 1, 13, 0, 0, 0, time.UTC),
			},
			args{
				flightdata: flightboard.FlightOnDateWithCodeshares{
					FlightData: flightdata.NewFlightData(
						structs.FlightBase{
							DepartureTimeScheduled: 1300,
						},
						nil,
						nil,
						20200101,
						&mockTimezoneProvider{},
					),
				},
			},
			flightboard.FitsByScheduledTime,
		},
		{
			"invalid outside of right edge of period",
			fields{
				direction: direction.DEPARTURE,
				from:      time.Date(2020, 1, 1, 12, 0, 0, 0, time.UTC),
				to:        time.Date(2020, 1, 1, 13, 0, 0, 0, time.UTC),
			},
			args{
				flightdata: flightboard.FlightOnDateWithCodeshares{
					FlightData: flightdata.NewFlightData(
						structs.FlightBase{
							DepartureTimeScheduled: 1301,
						},
						nil,
						nil,
						20200101,
						&mockTimezoneProvider{},
					),
				},
			},
			flightboard.DoesNotFit,
		},
		{
			"next day",
			fields{
				direction: direction.DEPARTURE,
				from:      time.Date(2020, 1, 1, 12, 0, 0, 0, time.UTC),
				to:        time.Date(2020, 1, 1, 13, 0, 0, 0, time.UTC),
			},
			args{
				flightdata: flightboard.FlightOnDateWithCodeshares{
					FlightData: flightdata.NewFlightData(
						structs.FlightBase{
							DepartureTimeScheduled: 1300,
						},
						nil,
						nil,
						20200102,
						&mockTimezoneProvider{},
					),
				},
			},
			flightboard.DoesNotFit,
		},
		{
			"scheduled outside, actual inside period",
			fields{
				direction: direction.DEPARTURE,
				from:      time.Date(2020, 1, 1, 12, 0, 0, 0, time.UTC),
				to:        time.Date(2020, 1, 1, 13, 0, 0, 0, time.UTC),
			},
			args{
				flightdata: flightboard.FlightOnDateWithCodeshares{
					FlightData: flightdata.NewFlightData(
						structs.FlightBase{
							DepartureTimeScheduled: 1000,
						},
						nil,
						&structs.FlightStatus{
							DepartureTimeActual: "2020-01-01 12:00:00",
						},
						20200101,
						&mockTimezoneProvider{},
					),
				},
			},
			flightboard.FitsByActualTime,
		},
		{
			"scheduled yesterday, actual inside period",
			fields{
				direction: direction.DEPARTURE,
				from:      time.Date(2020, 1, 1, 12, 0, 0, 0, time.UTC),
				to:        time.Date(2020, 1, 1, 13, 0, 0, 0, time.UTC),
			},
			args{
				flightdata: flightboard.FlightOnDateWithCodeshares{
					FlightData: flightdata.NewFlightData(
						structs.FlightBase{
							DepartureTimeScheduled: 1000,
						},
						nil,
						&structs.FlightStatus{
							DepartureTimeActual: "2020-01-01 12:00:00",
						},
						20191231,
						&mockTimezoneProvider{},
					),
				},
			},
			flightboard.FitsByActualTime,
		},
		{
			"invalid outside of left edge of period, arrival doesnt affect",
			fields{
				direction: direction.DEPARTURE,
				from:      time.Date(2020, 1, 1, 12, 0, 0, 0, time.UTC),
				to:        time.Date(2020, 1, 1, 13, 0, 0, 0, time.UTC),
			},
			args{
				flightdata: flightboard.FlightOnDateWithCodeshares{
					FlightData: flightdata.NewFlightData(
						structs.FlightBase{
							DepartureTimeScheduled: 1100,
							ArrivalTimeScheduled:   1230,
						},
						nil,
						nil,
						20200101,
						&mockTimezoneProvider{},
					),
				},
			},
			flightboard.DoesNotFit,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			filter := flightboard.DateTimeFilter(tt.fields.direction, tt.fields.from, tt.fields.to)

			if got := filter(&tt.args.flightdata); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("DepartureTimezone() = %v, want %v", got, tt.want)
			}
		})
	}
}

type ServiceHelper struct {
	Service  *flightboard.FlightBoardServiceImpl
	Timezone struct {
		SVX *rasp.TTimeZone
		JFK *rasp.TTimeZone
		VKO *rasp.TTimeZone
		ASF *rasp.TTimeZone
		DME *rasp.TTimeZone
		LED *rasp.TTimeZone
	}
	Station struct {
		SVX             *snapshots.TStationWithCodes
		JFK             *snapshots.TStationWithCodes
		VKO             *snapshots.TStationWithCodes
		ASF             *snapshots.TStationWithCodes
		DME             *snapshots.TStationWithCodes
		LED             *snapshots.TStationWithCodes
		InvalidTimezone *snapshots.TStationWithCodes
	}
	FlightBase struct {
		SVXJFK structs.FlightBase
		SVXVKO structs.FlightBase
		JFKVKO structs.FlightBase
		ASFDME structs.FlightBase
		LEDVKO structs.FlightBase
		SVXLED structs.FlightBase
		DMELED structs.FlightBase
	}
	FlightPattern struct {
		SVXJFK struct {
			Operating *structs.FlightPattern
		}
		SVXJFK2 struct {
			Operating *structs.FlightPattern
		}
		SVXVKO struct {
			Operating *structs.FlightPattern
		}
		JFKVKO struct {
			Operating *structs.FlightPattern
		}
		ASFDME  *structs.FlightPattern
		LEDVKO  *structs.FlightPattern
		LEDVKO2 *structs.FlightPattern // Delayed over midnight
		SVXLED  struct {
			Operating *structs.FlightPattern
		}
		DMELED struct {
			Operating *structs.FlightPattern
		}
	}
	Status struct {
		SVXJFK2 structs.FlightStatus
		ASFDME  structs.FlightStatus
		LEDVKO  structs.FlightStatus
		LEDVKO2 structs.FlightStatus
	}
}

func serviceTestHelper(t *testing.T) *ServiceHelper {
	var h ServiceHelper
	storage := storageCache.NewStorageWithStartDate("2019-11-01")
	tz := timezone.NewTimeZoneUtil(storage.Timezones(), storage.Stations())
	trs := threads.NewThreadRouteService(storage, tz)
	h.Service = &flightboard.FlightBoardServiceImpl{
		Storage:            storage,
		TimeZoneUtil:       tz,
		ThreadRouteService: trs,
	}

	// TIMEZONES
	h.Timezone.SVX = &rasp.TTimeZone{Id: 200, Code: "Asia/Yekaterinburg"}
	storage.Timezones().PutTimezone(h.Timezone.SVX)

	h.Timezone.JFK = &rasp.TTimeZone{Id: 201, Code: "America/New_York"}
	storage.Timezones().PutTimezone(h.Timezone.JFK)

	h.Timezone.VKO = &rasp.TTimeZone{Id: 202, Code: "Europe/Moscow"}
	storage.Timezones().PutTimezone(h.Timezone.VKO)

	h.Timezone.ASF = &rasp.TTimeZone{Id: 203, Code: "Europe/Astrakhan"}
	storage.Timezones().PutTimezone(h.Timezone.ASF)

	h.Timezone.DME = &rasp.TTimeZone{Id: 204, Code: "Europe/Moscow"}
	storage.Timezones().PutTimezone(h.Timezone.DME)

	h.Timezone.LED = &rasp.TTimeZone{Id: 205, Code: "Europe/Moscow"}
	storage.Timezones().PutTimezone(h.Timezone.LED)

	// STATIONS
	h.Station.SVX = &snapshots.TStationWithCodes{
		Station: &rasp.TStation{
			Id:         100,
			TimeZoneId: 200,
		},
		IataCode: "SVX",
	}
	storage.PutStation(h.Station.SVX)

	h.Station.JFK = &snapshots.TStationWithCodes{
		Station: &rasp.TStation{
			Id:         101,
			TimeZoneId: 201,
		},
		IataCode: "JFK",
	}
	storage.PutStation(h.Station.JFK)

	h.Station.VKO = &snapshots.TStationWithCodes{
		Station: &rasp.TStation{
			Id:         9600215,
			TimeZoneId: 202,
		},
		IataCode: "VKO",
	}
	storage.PutStation(h.Station.VKO)

	h.Station.ASF = &snapshots.TStationWithCodes{
		Station: &rasp.TStation{
			Id:         9600172,
			TimeZoneId: 203,
		},
		IataCode: "ASF",
	}
	storage.PutStation(h.Station.ASF)

	h.Station.DME = &snapshots.TStationWithCodes{
		Station: &rasp.TStation{
			Id:         9600216,
			TimeZoneId: 202,
		},
		IataCode: "DME",
	}
	storage.PutStation(h.Station.DME)

	h.Station.InvalidTimezone = &snapshots.TStationWithCodes{
		Station: &rasp.TStation{
			Id:         102,
			TimeZoneId: 299,
		},
		IataCode: "ITZ",
	}
	storage.PutStation(h.Station.InvalidTimezone)

	h.Station.LED = &snapshots.TStationWithCodes{
		Station: &rasp.TStation{
			Id:         9600366,
			TimeZoneId: 205,
		},
		IataCode: "LED",
	}
	storage.PutStation(h.Station.LED)

	// FLIGHT BASE
	h.FlightBase.SVXJFK = structs.FlightBase{
		ID:                     300,
		OperatingCarrier:       26,
		OperatingCarrierCode:   "SU",
		OperatingFlightNumber:  "5555",
		DepartureStation:       100,
		DepartureStationCode:   "SVX",
		DepartureTimeScheduled: 500,
		DepartureTerminal:      "A",
		ArrivalStation:         101,
		ArrivalStationCode:     "JFK",
		ArrivalTimeScheduled:   1400,
		ArrivalTerminal:        "B",
		LegNumber:              1,
	}
	storage.PutFlightBase(h.FlightBase.SVXJFK)

	h.FlightBase.SVXVKO = structs.FlightBase{
		ID:                     301,
		OperatingCarrier:       9144,
		OperatingCarrierCode:   "DP",
		OperatingFlightNumber:  "404",
		DepartureStation:       100,
		DepartureStationCode:   "SVX",
		DepartureTimeScheduled: 1234,
		DepartureTerminal:      "B",
		ArrivalStation:         9600215,
		ArrivalStationCode:     "VKO",
		ArrivalTimeScheduled:   1345,
		ArrivalTerminal:        "C",
		LegNumber:              1,
	}
	storage.PutFlightBase(h.FlightBase.SVXVKO)

	h.FlightBase.JFKVKO = structs.FlightBase{
		ID:                     302,
		OperatingCarrier:       23,
		OperatingCarrierCode:   "S7",
		OperatingFlightNumber:  "404",
		DepartureStation:       101,
		DepartureStationCode:   "JFK",
		DepartureTimeScheduled: 900,
		DepartureTerminal:      "6",
		ArrivalStation:         9600215,
		ArrivalStationCode:     "VKO",
		ArrivalTimeScheduled:   1445,
		ArrivalTerminal:        "C",
		LegNumber:              1,
	}
	storage.PutFlightBase(h.FlightBase.JFKVKO)

	h.FlightBase.ASFDME = structs.FlightBase{
		ID:                     1872412,
		OperatingCarrier:       23,
		OperatingCarrierCode:   "S7",
		OperatingFlightNumber:  "2162",
		LegNumber:              1,
		DepartureStation:       9600172,
		DepartureStationCode:   "ASF",
		DepartureTimeScheduled: 1615,
		ArrivalStation:         9600216,
		ArrivalStationCode:     "DME",
		ArrivalTimeScheduled:   1735,
		ArrivalTerminal:        "C",
	}
	storage.PutFlightBase(h.FlightBase.ASFDME)

	h.FlightBase.LEDVKO = structs.FlightBase{
		ID:                     41828,
		OperatingCarrier:       26,
		OperatingCarrierCode:   "SU",
		OperatingFlightNumber:  "6011",
		LegNumber:              1,
		DepartureStation:       9600366,
		DepartureStationCode:   "LED",
		DepartureTimeScheduled: 550,
		ArrivalStation:         9600215,
		ArrivalStationCode:     "VKO",
		ArrivalTimeScheduled:   725,
		ArrivalTerminal:        "A",
	}
	storage.PutFlightBase(h.FlightBase.LEDVKO)

	// SVXLED, DMELED - two inconsistent legs for the same flight to test RASPTICKETS-18374
	h.FlightBase.SVXLED = structs.FlightBase{
		ID:                     1001,
		OperatingCarrier:       9144,
		OperatingCarrierCode:   "DP",
		OperatingFlightNumber:  "999",
		DepartureStation:       100,
		DepartureStationCode:   "SVX",
		DepartureTimeScheduled: 1133,
		DepartureTerminal:      "B",
		ArrivalStation:         9600366,
		ArrivalStationCode:     "LED",
		ArrivalTimeScheduled:   1233,
		ArrivalTerminal:        "C",
		LegNumber:              1,
	}
	storage.PutFlightBase(h.FlightBase.SVXLED)

	h.FlightBase.DMELED = structs.FlightBase{
		ID:                     1002,
		OperatingCarrier:       9144,
		OperatingCarrierCode:   "DP",
		OperatingFlightNumber:  "999",
		DepartureStation:       9600216,
		DepartureStationCode:   "DME",
		DepartureTimeScheduled: 1733,
		DepartureTerminal:      "B",
		ArrivalStation:         9600366,
		ArrivalStationCode:     "LED",
		ArrivalTimeScheduled:   1833,
		ArrivalTerminal:        "C",
		LegNumber:              2,
	}
	storage.PutFlightBase(h.FlightBase.DMELED)

	// FLIGHT PATTERN
	h.FlightPattern.SVXJFK.Operating = &structs.FlightPattern{
		ID:                    500,
		FlightBaseID:          300,
		OperatingFromDate:     "20191201",
		OperatingUntilDate:    "20200201",
		OperatingOnDays:       1234567,
		MarketingCarrier:      26,
		MarketingCarrierCode:  "SU",
		MarketingFlightNumber: "5555",
	}
	storage.PutFlightPattern(*h.FlightPattern.SVXJFK.Operating)

	h.FlightPattern.SVXJFK2.Operating = &structs.FlightPattern{
		ID:                    501,
		FlightBaseID:          300,
		OperatingFromDate:     "20201201",
		OperatingUntilDate:    "20210301",
		OperatingOnDays:       1234567,
		MarketingCarrier:      26,
		MarketingCarrierCode:  "SU",
		MarketingFlightNumber: "5555",
	}
	storage.PutFlightPattern(*h.FlightPattern.SVXJFK2.Operating)

	h.FlightPattern.SVXVKO.Operating = &structs.FlightPattern{
		ID:                    502,
		FlightBaseID:          301,
		OperatingFromDate:     "20200401",
		OperatingUntilDate:    "20200409",
		OperatingOnDays:       1357,
		MarketingCarrier:      9144,
		MarketingCarrierCode:  "DP",
		MarketingFlightNumber: "404",
	}
	storage.PutFlightPattern(*h.FlightPattern.SVXVKO.Operating)

	h.FlightPattern.JFKVKO.Operating = &structs.FlightPattern{
		ID:                    503,
		FlightBaseID:          302,
		OperatingFromDate:     "20200401",
		OperatingUntilDate:    "20200409",
		OperatingOnDays:       1357,
		MarketingCarrier:      23,
		MarketingCarrierCode:  "S7",
		MarketingFlightNumber: "404",
		ArrivalDayShift:       1,
	}
	storage.PutFlightPattern(*h.FlightPattern.JFKVKO.Operating)

	h.FlightPattern.JFKVKO.Operating = &structs.FlightPattern{
		ID:                    503,
		FlightBaseID:          302,
		OperatingFromDate:     "20200401",
		OperatingUntilDate:    "20200409",
		OperatingOnDays:       1357,
		MarketingCarrier:      23,
		MarketingCarrierCode:  "S7",
		MarketingFlightNumber: "404",
		ArrivalDayShift:       1,
	}
	storage.PutFlightPattern(*h.FlightPattern.JFKVKO.Operating)

	h.FlightPattern.ASFDME = &structs.FlightPattern{
		ID:                    3422794,
		FlightBaseID:          1872412,
		OperatingFromDate:     "20200113",
		OperatingUntilDate:    "20200322",
		OperatingOnDays:       1357,
		MarketingCarrier:      23,
		MarketingCarrierCode:  "S7",
		MarketingFlightNumber: "2162",
	}
	storage.PutFlightPattern(*h.FlightPattern.ASFDME)

	h.FlightPattern.LEDVKO = &structs.FlightPattern{
		ID:                    547681,
		FlightBaseID:          41828,
		OperatingFromDate:     "2020-01-08",
		OperatingUntilDate:    "2020-01-24",
		OperatingOnDays:       123456,
		MarketingCarrier:      26,
		MarketingCarrierCode:  "SU",
		MarketingFlightNumber: "6011",
	}
	storage.PutFlightPattern(*h.FlightPattern.LEDVKO)

	h.FlightPattern.LEDVKO2 = &structs.FlightPattern{
		ID:                    547682,
		FlightBaseID:          41828,
		OperatingFromDate:     "2020-07-15",
		OperatingUntilDate:    "2020-07-15",
		OperatingOnDays:       3,
		MarketingCarrier:      26,
		MarketingCarrierCode:  "SU",
		MarketingFlightNumber: "6011",
	}
	storage.PutFlightPattern(*h.FlightPattern.LEDVKO2)

	h.FlightPattern.SVXLED.Operating = &structs.FlightPattern{
		ID:                    1001,
		FlightBaseID:          1001,
		OperatingFromDate:     "20200701",
		OperatingUntilDate:    "20200709",
		OperatingOnDays:       1234567,
		MarketingCarrier:      9144,
		MarketingCarrierCode:  "DP",
		MarketingFlightNumber: "999",
	}
	storage.PutFlightPattern(*h.FlightPattern.SVXLED.Operating)

	h.FlightPattern.DMELED.Operating = &structs.FlightPattern{
		ID:                    1002,
		FlightBaseID:          1002,
		OperatingFromDate:     "20200701",
		OperatingUntilDate:    "20200709",
		OperatingOnDays:       1234567,
		MarketingCarrier:      9144,
		MarketingCarrierCode:  "DP",
		MarketingFlightNumber: "999",
	}
	storage.PutFlightPattern(*h.FlightPattern.DMELED.Operating)

	// FLIGHT STATUS
	h.Status.SVXJFK2 = structs.FlightStatus{
		AirlineID:              26,
		FlightNumber:           "5555",
		LegNumber:              1,
		FlightDate:             "2021-02-02",
		DepartureSourceID:      3,
		ArrivalSourceID:        3,
		CreatedAtUtc:           "2021-01-20 13:00:00",
		UpdatedAtUtc:           "2021-01-01 07:35:46",
		DepartureTimeActual:    "2021-02-02 05:20:00",
		DepartureTimeScheduled: "2021-02-02 05:10:00",
		DepartureStatus:        "задержан",
		DepartureGate:          "111",
		DepartureTerminal:      "CCC",
	}
	storage.StatusStorage().PutFlightStatus(h.Status.SVXJFK2)

	h.Status.ASFDME = structs.FlightStatus{
		AirlineID:            23,
		FlightNumber:         "2162",
		LegNumber:            1,
		FlightDate:           "2020-01-20",
		DepartureSourceID:    3,
		ArrivalSourceID:      3,
		CreatedAtUtc:         "2020-01-17 21:21:10",
		UpdatedAtUtc:         "2020-01-20 14:00:00",
		ArrivalStatus:        "unknown",
		ArrivalCreatedAtUtc:  "2020-01-17 21:21:10",
		ArrivalReceivedAtUtc: "2020-01-20 14:00:00",
		ArrivalUpdatedAtUtc:  "2020-01-20 14:00:01",
		ArrivalTimeActual:    "2020-01-20 17:05:00",
		ArrivalTimeScheduled: "2020-01-20 17:35:00",
	}
	storage.StatusStorage().PutFlightStatus(h.Status.ASFDME)

	h.Status.LEDVKO = structs.FlightStatus{
		AirlineID:            26,
		FlightNumber:         "6011",
		LegNumber:            1,
		FlightDate:           "2020-01-24",
		DepartureSourceID:    3,
		ArrivalSourceID:      3,
		CreatedAtUtc:         "2020-01-21 21:21:10",
		UpdatedAtUtc:         "2020-01-25 14:00:00",
		ArrivalStatus:        "cancelled",
		ArrivalCreatedAtUtc:  "2020-01-21 21:21:10",
		ArrivalReceivedAtUtc: "2020-01-25 13:59:59",
		ArrivalUpdatedAtUtc:  "2020-01-25 14:00:00",
		ArrivalTimeActual:    "",
		ArrivalTimeScheduled: "2020-01-24 07:35:00",
	}
	storage.StatusStorage().PutFlightStatus(h.Status.LEDVKO)

	h.Status.LEDVKO2 = structs.FlightStatus{
		AirlineID:              26,
		FlightNumber:           "6011",
		LegNumber:              1,
		FlightDate:             "2020-07-15",
		DepartureSourceID:      3,
		ArrivalSourceID:        3,
		CreatedAtUtc:           "2020-01-21 21:21:10",
		UpdatedAtUtc:           "2020-01-25 14:00:00",
		ArrivalStatus:          "delayed",
		ArrivalCreatedAtUtc:    "2020-01-21 21:21:10",
		ArrivalReceivedAtUtc:   "2020-01-25 13:59:59",
		ArrivalUpdatedAtUtc:    "2020-01-25 14:00:00",
		DepartureTimeActual:    "2020-07-16 00:45:00",
		DepartureTimeScheduled: "2020-07-15 19:00:00",
		ArrivalTimeActual:      "",
		ArrivalTimeScheduled:   "2020-07-15 20:15:00",
	}
	storage.StatusStorage().PutFlightStatus(h.Status.LEDVKO2)

	// FLIGHT BOARD
	err := storage.UpdateCacheDependentData()
	assert.NoError(t, err, "cannot update cache dependent data")

	return &h
}
