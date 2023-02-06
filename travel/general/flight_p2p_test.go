package flightp2p

import (
	"fmt"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	dto "a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/DTO"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/flight_p2p/format"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/segment"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/timezone"
	storageCache "a.yandex-team.ru/travel/avia/shared_flights/api/internal/storage"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/storage/flight"
	"a.yandex-team.ru/travel/avia/shared_flights/api/pkg/structs"
	"a.yandex-team.ru/travel/avia/shared_flights/lib/go/dtutil"
	"a.yandex-team.ru/travel/proto/dicts/rasp"
	"a.yandex-team.ru/travel/proto/shared_flights/snapshots"
)

type mockTimezoneProvider struct{}

func (m mockTimezoneProvider) GetTimeZoneByStationID(int64) *time.Location {
	return time.UTC
}

var dontFilterByFlights = []FlightKey{}

func Test_TestStorageService_GetFlightsP2P_NewUtcTime(t *testing.T) {
	assert.Equal(t, "2020-12-31 23:00:00 +0000 UTC", fmt.Sprintf("%v", NewUtcTime("2020-12-31 23:00:00").Value), "unable to parse time")
}

func TestStorageService_GetFlightsP2P_FailOnInvalidDatesRange(t *testing.T) {
	h := serviceTestHelper(t)
	_, err := h.Service.GetFlightsP2P(
		[]*snapshots.TStationWithCodes{h.Station.SVX},
		[]*snapshots.TStationWithCodes{h.Station.SVX},
		NewUtcTime("2020-12-31 23:00:00"),
		NewUtcTime("2020-01-01 01:00:00"),
		"",
		true,
		dontFilterByFlights,
	)
	assert.EqualError(
		t, err,
		"invalid date range: (2020-12-31 23:00:00 +0000 UTC) - (2020-01-01 01:00:00 +0000 UTC)",
		"should fail on invalid date range",
	)
}

func TestStorageService_GetFlightsP2P_FailOnInvalidRequestStationTimezone(t *testing.T) {
	h := serviceTestHelper(t)
	_, err := h.Service.GetFlightsP2P(
		[]*snapshots.TStationWithCodes{h.Station.InvalidTimezone},
		[]*snapshots.TStationWithCodes{h.Station.SVX},
		NewUtcTime("2020-01-01 23:00:00"),
		NewUtcTime("2020-01-02 01:00:00"),
		"",
		true,
		dontFilterByFlights,
	)
	assert.EqualError(
		t, err,
		"cannot load timezone for station  102",
		"should fail on invalid timezone",
	)
}

func TestStorageService_GetFlightsP2P_SingleFlightNoCodeshare(t *testing.T) {
	h := serviceTestHelper(t)
	expect := format.Response{
		DepartureStations: []int32{100},
		ArrivalStations:   []int32{101},
		Flights: []format.Flight{
			format.Flight{
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
				ArrivalDatetime:   "2020-01-01T14:00:00-05:00",
				ArrivalTerminal:   "B",
				ArrivalStation:    101,
				StartDatetime:     "2020-01-01T05:00:00+05:00",
				Codeshares:        nil,
				Route:             dto.Route{100, 101},
			},
		},
	}
	got, err := h.Service.GetFlightsP2P(
		[]*snapshots.TStationWithCodes{h.Station.SVX},
		[]*snapshots.TStationWithCodes{h.Station.JFK},
		NewUtcTime("2019-12-31 23:00:00"),
		NewUtcTime("2020-01-01 01:00:00"),
		"",
		true,
		dontFilterByFlights,
	)

	assert.NoError(t, err, "cannot get flights p2p")
	assert.Equal(t,
		expect,
		got,
		"incorrect flights p2p response",
	)
}

func TestStorageService_GetFlightsP2P_DopFlight(t *testing.T) {
	h := serviceTestHelper(t)
	expect := format.Response{
		DepartureStations: []int32{9600215},
		ArrivalStations:   []int32{100},
		Flights: []format.Flight{
			format.Flight{
				TitledFlight: dto.TitledFlight{
					FlightID: dto.FlightID{
						AirlineID: 9144,
						Number:    "909",
					},
					Title: "DP 909",
				},
				DepartureDatetime: "2020-04-06T15:50:00+03:00",
				ArrivalDatetime:   "2020-04-06T22:01:00+05:00",
				ArrivalTerminal:   "A",
				DepartureStation:  9600215,
				ArrivalStation:    100,
				StartDatetime:     "2020-04-06T15:50:00+03:00",
				Codeshares:        nil,
				Route:             dto.Route{9600215, 100},
				Source:            "flight-board",
			},
		},
	}
	got, err := h.Service.GetFlightsP2P(
		[]*snapshots.TStationWithCodes{h.Station.VKO},
		[]*snapshots.TStationWithCodes{h.Station.SVX},
		NewUtcTime("2020-04-06 01:00:00"),
		NewUtcTime("2020-04-06 23:00:00"),
		"",
		true,
		dontFilterByFlights,
	)

	assert.NoError(t, err, "cannot get flights p2p")
	assert.Equal(t,
		expect,
		got,
		"incorrect flights p2p response",
	)
}

func TestStorageService_GetFlightsP2P_SingleFlightWithCodeshare(t *testing.T) {
	h := serviceTestHelper(t)
	expect := format.Response{
		DepartureStations: []int32{100},
		ArrivalStations:   []int32{9600215},
		Flights: []format.Flight{
			format.Flight{
				TitledFlight: dto.TitledFlight{
					FlightID: dto.FlightID{
						AirlineID: 9144,
						Number:    "408",
					},
					Title: "DP 408",
				},
				DepartureDatetime: "2020-04-06T12:34:00+05:00",
				DepartureTerminal: "B",
				DepartureStation:  100,
				ArrivalDatetime:   "2020-04-07T13:45:00+03:00",
				ArrivalTerminal:   "C",
				ArrivalStation:    9600215,
				StartDatetime:     "2020-04-06T12:34:00+05:00",
				Codeshares: []format.CodeshareFlight{
					format.CodeshareFlight{
						TitledFlight: dto.TitledFlight{
							FlightID: dto.FlightID{
								AirlineID: 7,
								Number:    "1408",
							},
							Title: "B2 1408",
						},
					},
				},
				Route: dto.Route{100, 9600215, 9600366, 9600381},
			},
		},
	}

	// Test flights filter
	tests := []struct {
		name          string
		flightsFilter []FlightKey
		expected      format.Response
	}{
		{
			"single flight with codeshare and no filter",
			dontFilterByFlights,
			expect,
		},
		{
			"single flight with codeshare that should pass a filter as operating",
			[]FlightKey{
				{
					9144,
					"408",
				},
			},
			expect,
		},
		{
			"single flight with codeshare that should pass a filter as codeshare",
			[]FlightKey{
				{
					7,
					"1408",
				},
			},
			expect,
		},
		{
			"single flight with codeshare that should not pass a filter",
			[]FlightKey{
				{
					7,
					"9999",
				},
			},
			format.Response{
				DepartureStations: []int32{100},
				ArrivalStations:   []int32{9600215},
				Flights:           []format.Flight{},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := h.Service.GetFlightsP2P(
				[]*snapshots.TStationWithCodes{h.Station.SVX},
				[]*snapshots.TStationWithCodes{h.Station.VKO},
				NewUtcTime("2020-04-06 01:00:00"),
				NewUtcTime("2020-04-06 23:00:00"),
				"",
				true,
				tt.flightsFilter,
			)

			assert.NoError(t, err, "cannot get flights p2p")
			assert.Equal(t, tt.expected, got, "incorrect flights p2p response")
		})
	}
}

func TestStorageService_GetFlightsP2P_TimeOutOfSearchRange(t *testing.T) {
	h := serviceTestHelper(t)
	got, err := h.Service.GetFlightsP2P(
		[]*snapshots.TStationWithCodes{h.Station.SVX},
		[]*snapshots.TStationWithCodes{h.Station.JFK},
		NewUtcTime("2020-04-10 00:00:00"),
		NewUtcTime("2020-04-11 00:00:00"),
		"",
		true,
		dontFilterByFlights,
	)
	assert.Equal(
		t,
		format.Response{
			DepartureStations: []int32{100},
			ArrivalStations:   []int32{101},
			Flights:           make([]format.Flight, 0),
		},
		got,
	)
	assert.NoError(t, err, "should be no error for no matching flights")
}

func TestStorageService_GetFlightsP2P_TwoLegs(t *testing.T) {
	h := serviceTestHelper(t)
	expect := format.Response{
		DepartureStations: []int32{100},
		ArrivalStations:   []int32{9600366},
		Flights: []format.Flight{
			format.Flight{
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
				ArrivalDatetime:   "2020-04-09T17:25:00+03:00",
				ArrivalTerminal:   "A",
				ArrivalStation:    9600366,
				StartDatetime:     "2020-04-08T12:34:00+05:00",
				Codeshares:        nil,
				Route:             dto.Route{100, 9600215, 9600366, 9600381},
			},
		},
	}
	got, err := h.Service.GetFlightsP2P(
		[]*snapshots.TStationWithCodes{h.Station.SVX},
		[]*snapshots.TStationWithCodes{h.Station.LED},
		NewUtcTime("2020-04-08 01:00:00"),
		NewUtcTime("2020-04-09 01:00:00"),
		"",
		true,
		dontFilterByFlights,
	)

	assert.NoError(t, err, "cannot get flights p2p")
	assert.Equal(t,
		expect,
		got,
		"incorrect flights p2p response",
	)
}

func TestStorageService_GetFlightsP2P_ThreeLegs(t *testing.T) {
	h := serviceTestHelper(t)
	expect := format.Response{
		DepartureStations: []int32{100},
		ArrivalStations:   []int32{9600381},
		Flights: []format.Flight{
			format.Flight{
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
				ArrivalDatetime:   "2020-04-09T23:25:00+05:00",
				ArrivalTerminal:   "A",
				ArrivalStation:    9600381,
				StartDatetime:     "2020-04-08T12:34:00+05:00",
				Codeshares:        nil,
				Route:             dto.Route{100, 9600215, 9600366, 9600381},
			},
		},
	}
	got, err := h.Service.GetFlightsP2P(
		[]*snapshots.TStationWithCodes{h.Station.SVX},
		[]*snapshots.TStationWithCodes{h.Station.PEE},
		NewUtcTime("2020-04-08 01:00:00"),
		NewUtcTime("2020-04-09 01:00:00"),
		"",
		true,
		dontFilterByFlights,
	)

	assert.NoError(t, err, "cannot get flights p2p")
	assert.Equal(t,
		expect,
		got,
		"incorrect flights p2p response",
	)
}

func TestStorageService_GetFlightsP2P_Overnight(t *testing.T) {
	h := serviceTestHelper(t)
	expect := format.Response{
		DepartureStations: []int32{9600366}, // LED
		ArrivalStations:   []int32{9600381}, // PEE
		Flights: []format.Flight{
			format.Flight{
				TitledFlight: dto.TitledFlight{
					FlightID: dto.FlightID{
						AirlineID: 9144,
						Number:    "408",
					},
					Title: "DP 408",
				},
				DepartureDatetime: "2020-04-09T19:50:00+03:00",
				DepartureStation:  9600366,
				ArrivalDatetime:   "2020-04-09T23:25:00+05:00",
				ArrivalTerminal:   "A",
				ArrivalStation:    9600381,
				StartDatetime:     "2020-04-08T12:34:00+05:00",
				Codeshares:        nil,
				Route:             dto.Route{100, 9600215, 9600366, 9600381},
			},
		},
	}
	got, err := h.Service.GetFlightsP2P(
		[]*snapshots.TStationWithCodes{h.Station.LED},
		[]*snapshots.TStationWithCodes{h.Station.PEE},
		NewUtcTime("2020-04-09 01:00:00"),
		NewUtcTime("2020-04-10 01:00:00"),
		"",
		true,
		dontFilterByFlights,
	)

	assert.NoError(t, err, "cannot get flights p2p")
	assert.Equal(t,
		expect,
		got,
		"incorrect flights p2p response",
	)
}

func TestStorageService_GetFlightsP2P_Multiroute(t *testing.T) {
	h := serviceTestHelper(t)
	expect := format.Response{
		DepartureStations: []int32{101},
		ArrivalStations:   []int32{9600215},
		Flights: []format.Flight{
			format.Flight{
				TitledFlight: dto.TitledFlight{
					FlightID: dto.FlightID{
						AirlineID: 9144,
						Number:    "707",
					},
					Title: "DP 707",
				},
				DepartureDatetime: "2020-05-04T20:30:00-04:00",
				DepartureStation:  101,
				ArrivalDatetime:   "2020-05-05T14:25:00+03:00",
				ArrivalTerminal:   "A",
				ArrivalStation:    9600215,
				StartDatetime:     "2020-05-04T20:30:00-04:00",
				Codeshares:        nil,
				Route:             dto.Route{101, 9600215, 9600366},
			},
		},
	}
	got, err := h.Service.GetFlightsP2P(
		[]*snapshots.TStationWithCodes{h.Station.JFK},
		[]*snapshots.TStationWithCodes{h.Station.VKO},
		NewUtcTime("2020-05-04 01:00:00"),
		NewUtcTime("2020-05-05 01:00:00"),
		"",
		true,
		dontFilterByFlights,
	)

	assert.NoError(t, err, "cannot get flights p2p")
	assert.Equal(t,
		expect,
		got,
		"incorrect flights p2p response",
	)
}

func TestStorageService_GetFlightsP2P_SortFlightPatterns(t *testing.T) {
	flightPatterns := flight.FlightLegs{
		flight.LegsList{&structs.FlightPattern{
			ID:        105,
			LegNumber: 5,
		}},
		flight.LegsList{&structs.FlightPattern{
			ID:        101,
			LegNumber: 1,
		}},
		flight.LegsList{&structs.FlightPattern{
			ID:        104,
			LegNumber: 4,
		}},
		flight.LegsList{&structs.FlightPattern{
			ID:        102,
			LegNumber: 2,
		}},
		flight.LegsList{&structs.FlightPattern{
			ID:        103,
			LegNumber: 3,
		}},
	}
	sortFlightPatterns(flightPatterns, 3)
	result := []int{}
	for _, legsList := range flightPatterns {
		result = append(result, int(legsList[0].LegNumber))
	}
	assert.Equal(
		t,
		[]int{3, 2, 1, 4, 5},
		result,
		"wrong sort order",
	)
}

type ServiceHelper struct {
	Service  *FlightP2PServiceImpl
	Timezone struct {
		SVX *rasp.TTimeZone
		JFK *rasp.TTimeZone
		VKO *rasp.TTimeZone
		LED *rasp.TTimeZone
	}
	Station struct {
		SVX             *snapshots.TStationWithCodes
		JFK             *snapshots.TStationWithCodes
		VKO             *snapshots.TStationWithCodes
		LED             *snapshots.TStationWithCodes
		PEE             *snapshots.TStationWithCodes
		IKT             *snapshots.TStationWithCodes
		InvalidTimezone *snapshots.TStationWithCodes
	}
	FlightBase struct {
		SVXJFK          structs.FlightBase
		SVXVKO          structs.FlightBase
		VKOLED          structs.FlightBase
		VKOLED2         structs.FlightBase
		LEDPEE          structs.FlightBase
		VKOSVXDop       structs.FlightBase
		JFKVKOMultiDest structs.FlightBase
		VKOSVXMultiDest structs.FlightBase
		VKOLEDMultiDest structs.FlightBase
		VKOJFKMultiDest structs.FlightBase
		SVXVKOMultiDest structs.FlightBase
		LEDVKOMultiDest structs.FlightBase
		PEEIKT          structs.FlightBase
		IKTSVX          structs.FlightBase
		ITZSVX          structs.FlightBase
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
			Codeshare *structs.FlightPattern
		}
		VKOLED          *structs.FlightPattern
		VKOLED2         *structs.FlightPattern
		LEDPEE          *structs.FlightPattern
		VKOSVXDop       *structs.FlightPattern
		JFKVKOMultiDest *structs.FlightPattern
		VKOSVXMultiDest *structs.FlightPattern
		VKOLEDMultiDest *structs.FlightPattern
		VKOJFKMultiDest *structs.FlightPattern
		SVXVKOMultiDest *structs.FlightPattern
		LEDVKOMultiDest *structs.FlightPattern
		PEEIKT          *structs.FlightPattern
		IKTSVX          *structs.FlightPattern
		ITZSVX          *structs.FlightPattern
	}
	StartScheduleDate dtutil.IntDate
}

func serviceTestHelper(t *testing.T) *ServiceHelper {
	var h ServiceHelper
	storage := storageCache.NewStorageWithStartDate("2019-11-01")
	segment.SetGlobalStartDateIndex(dtutil.DateCache.IndexOfStringDateP("2019-11-01"))
	tz := timezone.NewTimeZoneUtil(storage.Timezones(), storage.Stations())
	h.Service = &FlightP2PServiceImpl{
		Storage:      storage,
		TimeZoneUtil: tz,
	}
	h.StartScheduleDate = dtutil.IntDate(20191101)

	// TIMEZONES
	h.Timezone.SVX = &rasp.TTimeZone{Id: 200, Code: "Asia/Yekaterinburg"}
	storage.Timezones().PutTimezone(h.Timezone.SVX)

	h.Timezone.JFK = &rasp.TTimeZone{Id: 201, Code: "America/New_York"}
	storage.Timezones().PutTimezone(h.Timezone.JFK)

	h.Timezone.VKO = &rasp.TTimeZone{Id: 202, Code: "Europe/Moscow"}
	storage.Timezones().PutTimezone(h.Timezone.VKO)

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
			TimeZoneId: 202,
		},
		IataCode: "LED",
	}
	storage.PutStation(h.Station.LED)

	h.Station.PEE = &snapshots.TStationWithCodes{
		Station: &rasp.TStation{
			Id:         9600381,
			TimeZoneId: 200,
		},
		IataCode: "PEE",
	}
	storage.PutStation(h.Station.PEE)

	h.Station.IKT = &snapshots.TStationWithCodes{
		Station: &rasp.TStation{
			Id:         9600174,
			TimeZoneId: 200,
		},
		IataCode: "IKT",
	}
	storage.PutStation(h.Station.IKT)

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
		OperatingFlightNumber:  "408",
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

	h.FlightBase.VKOLED = structs.FlightBase{
		ID:                     302,
		OperatingCarrier:       9144,
		OperatingCarrierCode:   "DP",
		OperatingFlightNumber:  "408",
		LegNumber:              2,
		DepartureStation:       9600215,
		DepartureStationCode:   "VKO",
		DepartureTimeScheduled: 1550,
		ArrivalStation:         9600366,
		ArrivalStationCode:     "LED",
		ArrivalTimeScheduled:   1725,
		ArrivalTerminal:        "A",
	}
	storage.PutFlightBase(h.FlightBase.VKOLED)

	// Same as VKOLED, except the terminal -
	// this creates a variaton on the second leg, provoking GetFlightsP2PSchedule_SingleFlightWithCodeshare test to return
	// an extra route; fix for RASPTICKETS-18264 makes the test succeed even with the variation
	h.FlightBase.VKOLED2 = structs.FlightBase{
		ID:                     303,
		OperatingCarrier:       9144,
		OperatingCarrierCode:   "DP",
		OperatingFlightNumber:  "408",
		LegNumber:              2,
		DepartureStation:       9600215,
		DepartureStationCode:   "VKO",
		DepartureTimeScheduled: 1550,
		ArrivalStation:         9600366,
		ArrivalStationCode:     "LED",
		ArrivalTimeScheduled:   1725,
		ArrivalTerminal:        "B",
	}
	storage.PutFlightBase(h.FlightBase.VKOLED2)

	h.FlightBase.LEDPEE = structs.FlightBase{
		ID:                     304,
		OperatingCarrier:       9144,
		OperatingCarrierCode:   "DP",
		OperatingFlightNumber:  "408",
		LegNumber:              3,
		DepartureStation:       9600366,
		DepartureStationCode:   "LED",
		DepartureTimeScheduled: 1950,
		ArrivalStation:         9600381,
		ArrivalStationCode:     "PEE",
		ArrivalTimeScheduled:   2325,
		ArrivalTerminal:        "A",
	}
	storage.PutFlightBase(h.FlightBase.LEDPEE)

	h.FlightBase.VKOSVXDop = structs.FlightBase{
		ID:                     3001,
		OperatingCarrier:       9144,
		OperatingCarrierCode:   "DP",
		OperatingFlightNumber:  "909",
		LegNumber:              1,
		DepartureStation:       9600215,
		DepartureStationCode:   "VKO",
		DepartureTimeScheduled: 1550,
		ArrivalStation:         100,
		ArrivalStationCode:     "SVX",
		ArrivalTimeScheduled:   2201,
		ArrivalTerminal:        "A",
	}
	storage.FlightStorage().PutDopFlightBase(h.FlightBase.VKOSVXDop)

	// Flights for JFK-VKO-SVX/LED case when second leg has more than one destination
	h.FlightBase.JFKVKOMultiDest = structs.FlightBase{
		ID:                     401,
		OperatingCarrier:       9144,
		OperatingCarrierCode:   "DP",
		OperatingFlightNumber:  "707",
		LegNumber:              1,
		DepartureStation:       101,
		DepartureStationCode:   "JFK",
		DepartureTimeScheduled: 2030,
		ArrivalStation:         9600215,
		ArrivalStationCode:     "VKO",
		ArrivalTimeScheduled:   1425,
		ArrivalTerminal:        "A",
	}
	storage.PutFlightBase(h.FlightBase.JFKVKOMultiDest)

	h.FlightBase.VKOSVXMultiDest = structs.FlightBase{
		ID:                     402,
		OperatingCarrier:       9144,
		OperatingCarrierCode:   "DP",
		OperatingFlightNumber:  "707",
		LegNumber:              2,
		DepartureStation:       9600215,
		DepartureStationCode:   "VKO",
		DepartureTimeScheduled: 1930,
		ArrivalStation:         100,
		ArrivalStationCode:     "SVX",
		ArrivalTimeScheduled:   0025,
		ArrivalTerminal:        "A",
	}
	storage.PutFlightBase(h.FlightBase.VKOSVXMultiDest)

	h.FlightBase.VKOLEDMultiDest = structs.FlightBase{
		ID:                     403,
		OperatingCarrier:       9144,
		OperatingCarrierCode:   "DP",
		OperatingFlightNumber:  "707",
		LegNumber:              2,
		DepartureStation:       9600215,
		DepartureStationCode:   "VKO",
		DepartureTimeScheduled: 1940,
		ArrivalStation:         9600366,
		ArrivalStationCode:     "LED",
		ArrivalTimeScheduled:   2130,
		ArrivalTerminal:        "A",
	}
	storage.PutFlightBase(h.FlightBase.VKOLEDMultiDest)

	// Flights for SVX/LED-VKO-JFK case when first leg has more than one origin
	h.FlightBase.VKOJFKMultiDest = structs.FlightBase{
		ID:                     404,
		OperatingCarrier:       9144,
		OperatingCarrierCode:   "DP",
		OperatingFlightNumber:  "708",
		LegNumber:              2,
		DepartureStation:       9600215,
		DepartureStationCode:   "VKO",
		DepartureTimeScheduled: 1130,
		ArrivalStation:         101,
		ArrivalStationCode:     "JFK",
		ArrivalTimeScheduled:   1525,
		ArrivalTerminal:        "A",
	}
	storage.PutFlightBase(h.FlightBase.VKOJFKMultiDest)

	h.FlightBase.SVXVKOMultiDest = structs.FlightBase{
		ID:                     405,
		OperatingCarrier:       9144,
		OperatingCarrierCode:   "DP",
		OperatingFlightNumber:  "708",
		LegNumber:              1,
		DepartureStation:       100,
		DepartureStationCode:   "SVX",
		DepartureTimeScheduled: 630,
		ArrivalStation:         9600215,
		ArrivalStationCode:     "VKO",
		ArrivalTimeScheduled:   825,
		ArrivalTerminal:        "A",
	}
	storage.PutFlightBase(h.FlightBase.SVXVKOMultiDest)

	h.FlightBase.LEDVKOMultiDest = structs.FlightBase{
		ID:                     406,
		OperatingCarrier:       9144,
		OperatingCarrierCode:   "DP",
		OperatingFlightNumber:  "708",
		LegNumber:              1,
		DepartureStation:       9600366,
		DepartureStationCode:   "LED",
		DepartureTimeScheduled: 940,
		ArrivalStation:         9600215,
		ArrivalStationCode:     "VKO",
		ArrivalTimeScheduled:   1050,
		ArrivalTerminal:        "A",
	}
	storage.PutFlightBase(h.FlightBase.LEDVKOMultiDest)

	h.FlightBase.PEEIKT = structs.FlightBase{
		ID:                     451,
		OperatingCarrier:       9144,
		OperatingCarrierCode:   "DP",
		OperatingFlightNumber:  "876",
		LegNumber:              1,
		DepartureStation:       9600381,
		DepartureStationCode:   "PEE",
		DepartureTimeScheduled: 1950,
		ArrivalStation:         9600174,
		ArrivalStationCode:     "IKT",
		ArrivalTimeScheduled:   2325,
		ArrivalTerminal:        "A",
	}
	storage.PutFlightBase(h.FlightBase.PEEIKT)

	h.FlightBase.IKTSVX = structs.FlightBase{
		ID:                     452,
		OperatingCarrier:       9144,
		OperatingCarrierCode:   "DP",
		OperatingFlightNumber:  "876",
		LegNumber:              2,
		DepartureStation:       9600174,
		DepartureStationCode:   "IKT",
		DepartureTimeScheduled: 130,
		ArrivalStation:         100,
		ArrivalStationCode:     "SVX",
		ArrivalTimeScheduled:   925,
		ArrivalTerminal:        "A",
	}
	storage.PutFlightBase(h.FlightBase.IKTSVX)

	// Invalid timezone case
	h.FlightBase.ITZSVX = structs.FlightBase{
		ID:                     999,
		OperatingCarrier:       26,
		OperatingCarrierCode:   "SU",
		OperatingFlightNumber:  "999",
		DepartureStation:       102,
		DepartureStationCode:   "ITZ",
		DepartureTimeScheduled: 500,
		DepartureTerminal:      "A",
		ArrivalStation:         100,
		ArrivalStationCode:     "SVX",
		ArrivalTimeScheduled:   1400,
		ArrivalTerminal:        "B",
		LegNumber:              1,
	}
	storage.PutFlightBase(h.FlightBase.ITZSVX)

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
		LegNumber:             1,
	}
	storage.PutFlightPattern(*h.FlightPattern.SVXJFK.Operating)
	UpdateP2PCache(storage, h.Station.SVX.Station.Id, h.Station.JFK.Station.Id, 26, "5555", false)

	h.FlightPattern.SVXVKO.Operating = &structs.FlightPattern{
		ID:                    501,
		FlightBaseID:          301,
		OperatingFromDate:     "20200401",
		OperatingUntilDate:    "20200409",
		OperatingOnDays:       1357,
		MarketingCarrier:      9144,
		MarketingCarrierCode:  "DP",
		MarketingFlightNumber: "408",
		LegNumber:             1,
		ArrivalDayShift:       1,
	}
	storage.PutFlightPattern(*h.FlightPattern.SVXVKO.Operating)
	UpdateP2PCache(storage, h.Station.SVX.Station.Id, h.Station.VKO.Station.Id, 9144, "408", false)

	h.FlightPattern.SVXVKO.Codeshare = &structs.FlightPattern{
		ID:                       1501,
		FlightBaseID:             301,
		OperatingFromDate:        "20200401",
		OperatingUntilDate:       "20200409",
		OperatingOnDays:          13,
		MarketingCarrier:         7,
		MarketingCarrierCode:     "B2",
		MarketingFlightNumber:    "1408",
		LegNumber:                1,
		ArrivalDayShift:          1,
		OperatingFlightPatternID: 501,
		IsCodeshare:              true,
	}
	storage.PutFlightPattern(*h.FlightPattern.SVXVKO.Codeshare)
	UpdateP2PCache(storage, h.Station.SVX.Station.Id, h.Station.VKO.Station.Id, 7, "1408", true)

	h.FlightPattern.VKOLED = &structs.FlightPattern{
		ID:                    502,
		FlightBaseID:          302,
		OperatingFromDate:     "2020-04-01",
		OperatingUntilDate:    "2020-04-09",
		OperatingOnDays:       1246,
		MarketingCarrier:      9144,
		MarketingCarrierCode:  "DP",
		MarketingFlightNumber: "408",
		LegNumber:             2,
	}
	storage.PutFlightPattern(*h.FlightPattern.VKOLED)
	UpdateP2PCache(storage, h.Station.VKO.Station.Id, h.Station.LED.Station.Id, 9144, "408", false)
	UpdateP2PCache(storage, h.Station.SVX.Station.Id, h.Station.LED.Station.Id, 9144, "408", false)

	h.FlightPattern.VKOLED2 = &structs.FlightPattern{
		ID:                    503,
		FlightBaseID:          303,
		OperatingFromDate:     "2020-04-11",
		OperatingUntilDate:    "2020-04-11",
		OperatingOnDays:       6,
		MarketingCarrier:      9144,
		MarketingCarrierCode:  "DP",
		MarketingFlightNumber: "408",
		LegNumber:             2,
	}
	storage.PutFlightPattern(*h.FlightPattern.VKOLED2)

	h.FlightPattern.LEDPEE = &structs.FlightPattern{
		ID:                    504,
		FlightBaseID:          304,
		OperatingFromDate:     "2020-04-01",
		OperatingUntilDate:    "2020-04-09",
		OperatingOnDays:       1246,
		MarketingCarrier:      9144,
		MarketingCarrierCode:  "DP",
		MarketingFlightNumber: "408",
		LegNumber:             3,
	}
	storage.PutFlightPattern(*h.FlightPattern.LEDPEE)
	UpdateP2PCache(storage, h.Station.LED.Station.Id, h.Station.PEE.Station.Id, 9144, "408", false)
	UpdateP2PCache(storage, h.Station.SVX.Station.Id, h.Station.PEE.Station.Id, 9144, "408", false)
	UpdateP2PCache(storage, h.Station.VKO.Station.Id, h.Station.PEE.Station.Id, 9144, "408", false)

	h.FlightPattern.VKOSVXDop = &structs.FlightPattern{
		ID:                    30001,
		FlightBaseID:          3001,
		OperatingFromDate:     "2020-04-01",
		OperatingUntilDate:    "2020-04-09",
		OperatingOnDays:       1246,
		MarketingCarrier:      909,
		MarketingCarrierCode:  "DP",
		MarketingFlightNumber: "909",
		LegNumber:             1,
		IsDop:                 true,
	}
	storage.FlightStorage().PutDopFlightPattern(*h.FlightPattern.VKOSVXDop)

	h.FlightPattern.JFKVKOMultiDest = &structs.FlightPattern{
		ID:                    601,
		FlightBaseID:          401,
		OperatingFromDate:     "2020-05-01",
		OperatingUntilDate:    "2020-05-09",
		OperatingOnDays:       1234567,
		MarketingCarrier:      9144,
		MarketingCarrierCode:  "DP",
		MarketingFlightNumber: "707",
		LegNumber:             1,
		ArrivalDayShift:       1,
	}
	storage.PutFlightPattern(*h.FlightPattern.JFKVKOMultiDest)
	UpdateP2PCache(storage, h.Station.JFK.Station.Id, h.Station.VKO.Station.Id, 9144, "707", false)

	h.FlightPattern.VKOSVXMultiDest = &structs.FlightPattern{
		ID:                    602,
		FlightBaseID:          402,
		OperatingFromDate:     "2020-05-02",
		OperatingUntilDate:    "2020-05-10",
		OperatingOnDays:       134567,
		MarketingCarrier:      9144,
		MarketingCarrierCode:  "DP",
		MarketingFlightNumber: "707",
		LegNumber:             2,
		ArrivalDayShift:       1,
	}
	storage.PutFlightPattern(*h.FlightPattern.VKOSVXMultiDest)
	UpdateP2PCache(storage, h.Station.VKO.Station.Id, h.Station.SVX.Station.Id, 9144, "707", false)
	UpdateP2PCache(storage, h.Station.JFK.Station.Id, h.Station.SVX.Station.Id, 9144, "707", false)

	h.FlightPattern.VKOLEDMultiDest = &structs.FlightPattern{
		ID:                    603,
		FlightBaseID:          403,
		OperatingFromDate:     "2020-05-02",
		OperatingUntilDate:    "2020-05-10",
		OperatingOnDays:       1234567,
		MarketingCarrier:      9144,
		MarketingCarrierCode:  "DP",
		MarketingFlightNumber: "707",
		LegNumber:             2,
	}
	storage.PutFlightPattern(*h.FlightPattern.VKOLEDMultiDest)
	UpdateP2PCache(storage, h.Station.VKO.Station.Id, h.Station.LED.Station.Id, 9144, "707", false)
	UpdateP2PCache(storage, h.Station.JFK.Station.Id, h.Station.LED.Station.Id, 9144, "707", false)

	h.FlightPattern.VKOJFKMultiDest = &structs.FlightPattern{
		ID:                    604,
		FlightBaseID:          404,
		OperatingFromDate:     "2020-05-01",
		OperatingUntilDate:    "2020-05-09",
		OperatingOnDays:       1234567,
		MarketingCarrier:      9144,
		MarketingCarrierCode:  "DP",
		MarketingFlightNumber: "708",
		LegNumber:             2,
	}
	storage.PutFlightPattern(*h.FlightPattern.VKOJFKMultiDest)
	UpdateP2PCache(storage, h.Station.VKO.Station.Id, h.Station.JFK.Station.Id, 9144, "708", false)

	h.FlightPattern.SVXVKOMultiDest = &structs.FlightPattern{
		ID:                    605,
		FlightBaseID:          405,
		OperatingFromDate:     "2020-05-01",
		OperatingUntilDate:    "2020-05-09",
		OperatingOnDays:       1234567,
		MarketingCarrier:      9144,
		MarketingCarrierCode:  "DP",
		MarketingFlightNumber: "708",
		LegNumber:             1,
	}
	storage.PutFlightPattern(*h.FlightPattern.SVXVKOMultiDest)
	UpdateP2PCache(storage, h.Station.SVX.Station.Id, h.Station.VKO.Station.Id, 9144, "708", false)
	UpdateP2PCache(storage, h.Station.SVX.Station.Id, h.Station.JFK.Station.Id, 9144, "708", false)

	h.FlightPattern.LEDVKOMultiDest = &structs.FlightPattern{
		ID:                    606,
		FlightBaseID:          406,
		OperatingFromDate:     "2020-05-01",
		OperatingUntilDate:    "2020-05-09",
		OperatingOnDays:       1234567,
		MarketingCarrier:      9144,
		MarketingCarrierCode:  "DP",
		MarketingFlightNumber: "708",
		LegNumber:             1,
	}
	storage.PutFlightPattern(*h.FlightPattern.LEDVKOMultiDest)
	UpdateP2PCache(storage, h.Station.LED.Station.Id, h.Station.VKO.Station.Id, 9144, "708", false)
	UpdateP2PCache(storage, h.Station.LED.Station.Id, h.Station.JFK.Station.Id, 9144, "708", false)

	h.FlightPattern.PEEIKT = &structs.FlightPattern{
		ID:                    551,
		FlightBaseID:          451,
		OperatingFromDate:     "2020-10-01",
		OperatingUntilDate:    "2020-10-07",
		OperatingOnDays:       1246,
		MarketingCarrier:      9144,
		MarketingCarrierCode:  "DP",
		MarketingFlightNumber: "876",
		LegNumber:             1,
	}
	storage.PutFlightPattern(*h.FlightPattern.PEEIKT)
	UpdateP2PCache(storage, h.Station.PEE.Station.Id, h.Station.IKT.Station.Id, 9144, "876", false)

	h.FlightPattern.IKTSVX = &structs.FlightPattern{
		ID:                    552,
		FlightBaseID:          452,
		OperatingFromDate:     "2020-10-02",
		OperatingUntilDate:    "2020-10-08",
		OperatingOnDays:       2357,
		MarketingCarrier:      9144,
		MarketingCarrierCode:  "DP",
		MarketingFlightNumber: "876",
		DepartureDayShift:     1,
		LegNumber:             2,
	}
	storage.PutFlightPattern(*h.FlightPattern.IKTSVX)
	UpdateP2PCache(storage, h.Station.IKT.Station.Id, h.Station.SVX.Station.Id, 9144, "876", false)
	UpdateP2PCache(storage, h.Station.PEE.Station.Id, h.Station.SVX.Station.Id, 9144, "876", false)

	h.FlightPattern.ITZSVX = &structs.FlightPattern{
		ID:                    999,
		FlightBaseID:          999,
		OperatingFromDate:     "2020-09-01",
		OperatingUntilDate:    "2020-09-01",
		OperatingOnDays:       2,
		MarketingCarrier:      26,
		MarketingCarrierCode:  "SU",
		MarketingFlightNumber: "999",
		LegNumber:             1,
	}
	storage.PutFlightPattern(*h.FlightPattern.ITZSVX)
	UpdateP2PCache(storage, h.Station.InvalidTimezone.Station.Id, h.Station.SVX.Station.Id, 26, "999", false)

	// FLIGHT BOARD
	err := storage.UpdateCacheDependentData()
	assert.NoError(t, err, "cannot update cache dependent data")

	return &h
}

func UpdateP2PCache(storage *storageCache.Storage, depStation, arrStation, carrier int32, flightNumber string, isCodeshare bool) {
	storage.UpdateP2PCache(&snapshots.TP2PCacheEntry{
		DepartureStationId: depStation,
		ArrivalStationId:   arrStation,
		Flights: []*snapshots.TFlightKey{
			{
				MarketingCarrierId:    carrier,
				MarketingFlightNumber: flightNumber,
				IsCodeshare:           isCodeshare,
			},
		},
	})
}
