package flightp2p

import (
	"testing"

	"github.com/stretchr/testify/assert"

	dto "a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/DTO"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/flight_p2p/format"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/storage/flight"
	"a.yandex-team.ru/travel/proto/shared_flights/snapshots"
)

func TestStorageService_GetFlightsP2PSchedule_FailOnInvalidRequestStationTimezone(t *testing.T) {
	h := serviceTestHelper(t)
	_, err := h.Service.GetFlightsP2PSchedule(
		[]*snapshots.TStationWithCodes{h.Station.InvalidTimezone},
		[]*snapshots.TStationWithCodes{h.Station.InvalidTimezone},
		"",
		true,
		h.StartScheduleDate,
	)
	assert.EqualError(
		t, err,
		"cannot load timezone for station  102",
		"should fail on invalid timezone",
	)
}

func TestStorageService_GetFlightsP2PSchedule_SingleFlightNoCodeshare(t *testing.T) {
	h := serviceTestHelper(t)
	expect := format.ScheduleResponse{
		DepartureStations: []int32{100},
		ArrivalStations:   []int32{101},
		Flights: []format.ScheduleFlight{
			{
				TitledFlight: dto.TitledFlight{
					FlightID: dto.FlightID{
						AirlineID: 26,
						Number:    "5555",
					},
					Title: "SU 5555",
				},
				DepartureTime:     "05:00",
				DepartureTimezone: "+0500",
				DepartureTerminal: "A",
				DepartureStation:  100,
				ArrivalTime:       "14:00",
				ArrivalTimezone:   "-0500",
				ArrivalTerminal:   "B",
				ArrivalStation:    101,
				StartTime:         "05:00",
				Codeshares:        nil,
				Route:             dto.Route{100, 101},
				Masks: []format.Mask{
					format.Mask{
						From:  "2019-12-01",
						Until: "2020-02-01",
						On:    1234567,
					},
				},
			},
			{
				TitledFlight: dto.TitledFlight{
					FlightID: dto.FlightID{
						AirlineID: 9144,
						Number:    "708",
					},
					Title: "DP 708",
				},
				DepartureTime:     "06:30",
				DepartureTimezone: "+0500",
				DepartureTerminal: "",
				DepartureStation:  100,
				ArrivalTime:       "15:25",
				ArrivalTimezone:   "-0400",
				ArrivalTerminal:   "A",
				ArrivalStation:    101,
				StartTime:         "06:30",
				Codeshares:        nil,
				Route:             dto.Route{100, 9600215, 101},
				Masks: []format.Mask{
					format.Mask{
						From:  "2020-05-01",
						Until: "2020-05-07",
						On:    1234567,
					},
					format.Mask{
						From:  "2020-05-08",
						Until: "2020-05-14",
						On:    56,
					},
				},
			},
		},
	}
	got, err := h.Service.GetFlightsP2PSchedule(
		[]*snapshots.TStationWithCodes{h.Station.SVX},
		[]*snapshots.TStationWithCodes{h.Station.JFK},
		"",
		true,
		h.StartScheduleDate,
	)

	assert.NoError(t, err, "cannot get flights p2p-schedule")
	assert.Equal(t,
		expect,
		got,
		"incorrect flights p2p response",
	)
}

func TestStorageService_GetFlightsP2PSchedule_OldDaysInMasks(t *testing.T) {
	h := serviceTestHelper(t)
	expect := format.ScheduleResponse{
		DepartureStations: []int32{100},
		ArrivalStations:   []int32{101},
		Flights: []format.ScheduleFlight{
			{
				TitledFlight: dto.TitledFlight{
					FlightID: dto.FlightID{
						AirlineID: 9144,
						Number:    "708",
					},
					Title: "DP 708",
				},
				DepartureTime:     "06:30",
				DepartureTimezone: "+0500",
				DepartureTerminal: "",
				DepartureStation:  100,
				ArrivalTime:       "15:25",
				ArrivalTimezone:   "-0400",
				ArrivalTerminal:   "A",
				ArrivalStation:    101,
				StartTime:         "06:30",
				Codeshares:        nil,
				Route:             dto.Route{100, 9600215, 101},
				Masks: []format.Mask{
					format.Mask{
						From:  "2020-05-07",
						Until: "2020-05-13",
						On:    456,
					},
				},
			},
		},
	}
	got, err := h.Service.GetFlightsP2PSchedule(
		[]*snapshots.TStationWithCodes{h.Station.SVX},
		[]*snapshots.TStationWithCodes{h.Station.JFK},
		"",
		true,
		20200507,
	)

	assert.NoError(t, err, "cannot get flights p2p-schedule")
	assert.Equal(t,
		expect,
		got,
		"incorrect flights p2p response",
	)
}

func TestStorageService_GetFlightsP2PSchedule_DopFlight(t *testing.T) {
	h := serviceTestHelper(t)
	expect := format.ScheduleResponse{
		DepartureStations: []int32{9600215},
		ArrivalStations:   []int32{100},
		Flights: []format.ScheduleFlight{
			{
				TitledFlight: dto.TitledFlight{
					FlightID: dto.FlightID{
						AirlineID: 9144,
						Number:    "909",
					},
					Title: "DP 909",
				},
				DepartureTime:     "15:50",
				DepartureTimezone: "+0300",
				ArrivalTime:       "22:01",
				ArrivalTimezone:   "+0500",
				ArrivalTerminal:   "A",
				DepartureStation:  9600215,
				ArrivalStation:    100,
				StartTime:         "15:50",
				Codeshares:        nil,
				Route:             dto.Route{9600215, 100},
				Masks: []format.Mask{
					format.Mask{
						From:  "2020-04-02",
						Until: "2020-04-08",
						On:    1246,
					},
					format.Mask{
						From:  "2020-04-09",
						Until: "2020-04-09",
						On:    4,
					},
				},
			},
			{
				TitledFlight: dto.TitledFlight{
					FlightID: dto.FlightID{
						AirlineID: 9144,
						Number:    "707",
					},
					Title: "DP 707",
				},
				DepartureTime:     "19:30",
				DepartureTimezone: "+0300",
				ArrivalTime:       "00:21",
				ArrivalTimezone:   "+0500",
				ArrivalTerminal:   "A",
				DepartureStation:  9600215,
				ArrivalStation:    100,
				ArrivalDayShift:   1,
				StartTime:         "20:30",
				StartDayShift:     -1,
				Codeshares:        nil,
				Route:             dto.Route{101, 9600215, 100},
				Masks: []format.Mask{
					format.Mask{
						From:  "2020-05-02",
						Until: "2020-05-08",
						On:    134567,
					},
					format.Mask{
						From:  "2020-05-09",
						Until: "2020-05-15",
						On:    67,
					},
				},
			},
		},
	}
	got, err := h.Service.GetFlightsP2PSchedule(
		[]*snapshots.TStationWithCodes{h.Station.VKO},
		[]*snapshots.TStationWithCodes{h.Station.SVX},
		"",
		true,
		h.StartScheduleDate,
	)

	assert.NoError(t, err, "cannot get flights p2p schedule")
	assert.Equal(t,
		expect,
		got,
		"incorrect flights p2p schedule response",
	)
}

func TestStorageService_GetFlightsP2PSchedule_SingleFlightWithCodeshare(t *testing.T) {
	h := serviceTestHelper(t)
	expect := format.ScheduleResponse{
		DepartureStations: []int32{100},
		ArrivalStations:   []int32{9600215},
		Flights: []format.ScheduleFlight{
			{
				TitledFlight: dto.TitledFlight{
					FlightID: dto.FlightID{
						AirlineID: 9144,
						Number:    "708",
					},
					Title: "DP 708",
				},
				DepartureTime:     "06:30",
				DepartureTimezone: "+0500",
				DepartureStation:  100,
				ArrivalTime:       "08:25",
				ArrivalTimezone:   "+0300",
				ArrivalTerminal:   "A",
				ArrivalStation:    9600215,
				StartTime:         "06:30",
				Codeshares:        nil,
				Route:             dto.Route{100, 9600215, 101},
				Masks: []format.Mask{
					format.Mask{
						From:  "2020-05-01",
						Until: "2020-05-07",
						On:    1234567,
					},
					format.Mask{
						From:  "2020-05-08",
						Until: "2020-05-14",
						On:    56,
					},
				},
			},
			{
				TitledFlight: dto.TitledFlight{
					FlightID: dto.FlightID{
						AirlineID: 9144,
						Number:    "408",
					},
					Title: "DP 408",
				},
				DepartureTime:     "12:34",
				DepartureTimezone: "+0500",
				DepartureTerminal: "B",
				DepartureStation:  100,
				ArrivalTime:       "13:45",
				ArrivalTimezone:   "+0300",
				ArrivalTerminal:   "C",
				ArrivalStation:    9600215,
				StartTime:         "12:34",
				Codeshares: []format.CodeshareFlight{
					format.CodeshareFlight{
						TitledFlight: dto.TitledFlight{
							FlightID: dto.FlightID{
								AirlineID: 7,
								Number:    "1408",
							},
							Title: "B2 1408",
						},
						Masks: []format.Mask{
							format.Mask{
								From:  "2020-04-01",
								Until: "2020-04-07",
								On:    13,
							},
							format.Mask{
								From:  "2020-04-08",
								Until: "2020-04-08",
								On:    3,
							},
						},
					},
				},
				ArrivalDayShift: 1,
				Route:           dto.Route{100, 9600215, 9600366, 9600381},
				Masks: []format.Mask{
					format.Mask{
						From:  "2020-04-01",
						Until: "2020-04-07",
						On:    1357,
					},
					format.Mask{
						From:  "2020-04-08",
						Until: "2020-04-08",
						On:    3,
					},
				},
			},
		},
	}
	got, err := h.Service.GetFlightsP2PSchedule(
		[]*snapshots.TStationWithCodes{h.Station.SVX},
		[]*snapshots.TStationWithCodes{h.Station.VKO},
		"",
		true,
		h.StartScheduleDate,
	)

	assert.NoError(t, err, "cannot get flights p2p schedule")
	assert.Equal(t,
		expect,
		got,
		"incorrect flights p2p schedule response",
	)
}

func TestStorageService_GetFlightsP2PSchedule_Multileg(t *testing.T) {
	h := serviceTestHelper(t)
	expect := format.ScheduleResponse{
		DepartureStations: []int32{100},
		ArrivalStations:   []int32{9600366},
		Flights: []format.ScheduleFlight{
			{
				TitledFlight: dto.TitledFlight{
					FlightID: dto.FlightID{
						AirlineID: 9144,
						Number:    "408",
					},
					Title: "DP 408",
				},
				DepartureTime:     "12:34",
				DepartureTimezone: "+0500",
				DepartureTerminal: "B",
				DepartureStation:  100,
				ArrivalTime:       "17:25",
				ArrivalTimezone:   "+0300",
				ArrivalTerminal:   "A",
				ArrivalStation:    9600366,
				StartTime:         "12:34",
				Codeshares:        nil,
				ArrivalDayShift:   1,
				Route:             dto.Route{100, 9600215, 9600366, 9600381},
				Masks: []format.Mask{
					format.Mask{
						From:  "2020-04-01",
						Until: "2020-04-07",
						On:    1357,
					},
					format.Mask{
						From:  "2020-04-08",
						Until: "2020-04-08",
						On:    3,
					},
				},
			},
		},
	}
	got, err := h.Service.GetFlightsP2PSchedule(
		[]*snapshots.TStationWithCodes{h.Station.SVX},
		[]*snapshots.TStationWithCodes{h.Station.LED},
		"",
		true,
		h.StartScheduleDate,
	)

	assert.NoError(t, err, "cannot get flights p2p schedule")
	assert.Equal(t,
		expect,
		got,
		"incorrect flights p2p schedule response",
	)
}

func TestStorageService_GetFlightsP2PSchedule_BannedOperatingFlight(t *testing.T) {
	h := serviceTestHelper(t)

	blacklistRuleStorage := flight.NewBlacklistRuleStorage(h.Service.Storage.Stations())
	rule1 := snapshots.TBlacklistRule{
		Id:                    1,
		MarketingCarrierId:    26,
		MarketingFlightNumber: "5555",
		ForceMode:             "FORCE_BAN",
		FlightDateSince:       "2020-01-05",
		FlightDateUntil:       "2020-01-11",
	}
	rule2 := snapshots.TBlacklistRule{
		Id:                    2,
		MarketingCarrierId:    9144,
		MarketingFlightNumber: "708",
		ForceMode:             "FORCE_BAN",
	}
	blacklistRuleStorage.AddRule(&rule1)
	blacklistRuleStorage.AddRule(&rule2)
	h.Service.Storage.SetBlacklistRuleStorage(blacklistRuleStorage)

	expect := format.ScheduleResponse{
		DepartureStations: []int32{100},
		ArrivalStations:   []int32{101},
		Flights: []format.ScheduleFlight{
			{
				TitledFlight: dto.TitledFlight{
					FlightID: dto.FlightID{
						AirlineID: 26,
						Number:    "5555",
					},
					Title: "SU 5555",
				},
				DepartureTime:     "05:00",
				DepartureTimezone: "+0500",
				DepartureTerminal: "A",
				DepartureStation:  100,
				ArrivalTime:       "14:00",
				ArrivalTimezone:   "-0500",
				ArrivalTerminal:   "B",
				ArrivalStation:    101,
				StartTime:         "05:00",
				Codeshares:        nil,
				Route:             dto.Route{100, 101},
				Banned: []format.Mask{
					format.Mask{
						From:  "2020-01-05",
						Until: "2020-01-11",
						On:    1234567,
					},
				},
				Masks: []format.Mask{
					format.Mask{
						From:  "2019-12-01",
						Until: "2020-01-04",
						On:    1234567,
					},
					format.Mask{
						From:  "2020-01-12",
						Until: "2020-02-01",
						On:    1234567,
					},
				},
			},
			{
				TitledFlight: dto.TitledFlight{
					FlightID: dto.FlightID{
						AirlineID: 9144,
						Number:    "708",
					},
					Title: "DP 708",
				},
				DepartureTime:     "06:30",
				DepartureTimezone: "+0500",
				DepartureTerminal: "",
				DepartureStation:  100,
				ArrivalTime:       "15:25",
				ArrivalTimezone:   "-0400",
				ArrivalTerminal:   "A",
				ArrivalStation:    101,
				StartTime:         "06:30",
				Codeshares:        nil,
				Route:             dto.Route{100, 9600215, 101},
				Banned: []format.Mask{
					format.Mask{
						From:  "2020-05-01",
						Until: "2020-05-07",
						On:    1234567,
					},
					format.Mask{
						From:  "2020-05-08",
						Until: "2020-05-14",
						On:    56,
					},
				},
			},
		},
	}
	got, err := h.Service.GetFlightsP2PSchedule(
		[]*snapshots.TStationWithCodes{h.Station.SVX},
		[]*snapshots.TStationWithCodes{h.Station.JFK},
		"",
		true,
		h.StartScheduleDate,
	)

	assert.NoError(t, err, "cannot get flights p2p-schedule")
	assert.Equal(t,
		expect,
		got,
		"incorrect flights p2p response",
	)
}

func TestStorageService_GetFlightsP2PSchedule_MultiRouteForward(t *testing.T) {
	h := serviceTestHelper(t)
	// Expecting to get both routes: JFK-VKO-SVX and JFK-VKO-LED
	expect := format.ScheduleResponse{
		DepartureStations: []int32{101},
		ArrivalStations:   []int32{9600215},
		Flights: []format.ScheduleFlight{
			{
				TitledFlight: dto.TitledFlight{
					FlightID: dto.FlightID{
						AirlineID: 9144,
						Number:    "707",
					},
					Title: "DP 707",
				},
				DepartureTime:     "20:30",
				DepartureTimezone: "-0400",
				DepartureStation:  101,
				ArrivalTime:       "14:25",
				ArrivalTimezone:   "+0300",
				ArrivalTerminal:   "A",
				ArrivalStation:    9600215,
				StartTime:         "20:30",
				Codeshares:        nil,
				ArrivalDayShift:   1,
				Route:             dto.Route{101, 9600215, 100},
				Masks: []format.Mask{
					format.Mask{
						From:  "2020-05-01",
						Until: "2020-05-07",
						On:    1234567,
					},
					format.Mask{
						From:  "2020-05-08",
						Until: "2020-05-14",
						On:    56,
					},
				},
			},
			{
				TitledFlight: dto.TitledFlight{
					FlightID: dto.FlightID{
						AirlineID: 9144,
						Number:    "707",
					},
					Title: "DP 707",
				},
				DepartureTime:     "20:30",
				DepartureTimezone: "-0400",
				DepartureStation:  101,
				ArrivalTime:       "14:25",
				ArrivalTimezone:   "+0300",
				ArrivalTerminal:   "A",
				ArrivalStation:    9600215,
				StartTime:         "20:30",
				Codeshares:        nil,
				ArrivalDayShift:   1,
				Route:             dto.Route{101, 9600215, 9600366},
				Masks: []format.Mask{
					format.Mask{
						From:  "2020-05-01",
						Until: "2020-05-07",
						On:    1234567,
					},
					format.Mask{
						From:  "2020-05-08",
						Until: "2020-05-14",
						On:    56,
					},
				},
			},
		},
	}
	got, err := h.Service.GetFlightsP2PSchedule(
		[]*snapshots.TStationWithCodes{h.Station.JFK},
		[]*snapshots.TStationWithCodes{h.Station.VKO},
		"",
		true,
		h.StartScheduleDate,
	)

	assert.NoError(t, err, "cannot get flights p2p schedule")
	assert.Equal(t,
		expect,
		got,
		"incorrect flights p2p schedule response",
	)
}

func TestStorageService_GetFlightsP2PSchedule_MultiRouteBackward(t *testing.T) {
	h := serviceTestHelper(t)
	// Expecting to get both routes: SVX-VKO-JFK and LED-VKO-JFK
	expect := format.ScheduleResponse{
		DepartureStations: []int32{9600215},
		ArrivalStations:   []int32{101},
		Flights: []format.ScheduleFlight{
			{
				TitledFlight: dto.TitledFlight{
					FlightID: dto.FlightID{
						AirlineID: 9144,
						Number:    "708",
					},
					Title: "DP 708",
				},
				DepartureTime:     "11:30",
				DepartureTimezone: "+0300",
				DepartureStation:  9600215,
				ArrivalTime:       "15:25",
				ArrivalTimezone:   "-0400",
				ArrivalTerminal:   "A",
				ArrivalStation:    101,
				StartTime:         "06:30",
				Codeshares:        nil,
				Route:             dto.Route{100, 9600215, 101},
				Masks: []format.Mask{
					format.Mask{
						From:  "2020-05-01",
						Until: "2020-05-07",
						On:    1234567,
					},
					format.Mask{
						From:  "2020-05-08",
						Until: "2020-05-14",
						On:    56,
					},
				},
			},
			{
				TitledFlight: dto.TitledFlight{
					FlightID: dto.FlightID{
						AirlineID: 9144,
						Number:    "708",
					},
					Title: "DP 708",
				},
				DepartureTime:     "11:30",
				DepartureTimezone: "+0300",
				DepartureStation:  9600215,
				ArrivalTime:       "15:25",
				ArrivalTimezone:   "-0400",
				ArrivalTerminal:   "A",
				ArrivalStation:    101,
				StartTime:         "09:40",
				Codeshares:        nil,
				Route:             dto.Route{9600366, 9600215, 101},
				Masks: []format.Mask{
					format.Mask{
						From:  "2020-05-01",
						Until: "2020-05-07",
						On:    1234567,
					},
					format.Mask{
						From:  "2020-05-08",
						Until: "2020-05-14",
						On:    56,
					},
				},
			},
		},
	}
	got, err := h.Service.GetFlightsP2PSchedule(
		[]*snapshots.TStationWithCodes{h.Station.VKO},
		[]*snapshots.TStationWithCodes{h.Station.JFK},
		"",
		true,
		h.StartScheduleDate,
	)

	assert.NoError(t, err, "cannot get flights p2p schedule")
	assert.Equal(t,
		expect,
		got,
		"incorrect flights p2p schedule response",
	)
}

func TestStorageService_GetFlightsP2PSchedule_DepartureDayShift(t *testing.T) {
	h := serviceTestHelper(t)
	expect := format.ScheduleResponse{
		DepartureStations: []int32{9600174},
		ArrivalStations:   []int32{100},
		Flights: []format.ScheduleFlight{
			{
				TitledFlight: dto.TitledFlight{
					FlightID: dto.FlightID{
						AirlineID: 9144,
						Number:    "876",
					},
					Title: "DP 876",
				},
				DepartureTime:     "01:30",
				DepartureTimezone: "+0500",
				DepartureStation:  9600174,
				ArrivalTime:       "09:25",
				ArrivalTimezone:   "+0500",
				ArrivalTerminal:   "A",
				ArrivalStation:    100,
				StartTime:         "19:50",
				StartDayShift:     -1,
				Codeshares:        nil,
				Route:             dto.Route{9600381, 9600174, 100},
				Masks: []format.Mask{
					format.Mask{
						From:  "2020-10-02",
						Until: "2020-10-08",
						On:    2357,
					},
				},
			},
		},
	}
	got, err := h.Service.GetFlightsP2PSchedule(
		[]*snapshots.TStationWithCodes{h.Station.IKT},
		[]*snapshots.TStationWithCodes{h.Station.SVX},
		"",
		true,
		h.StartScheduleDate,
	)

	assert.NoError(t, err, "cannot get flights p2p-schedule with departure day shift")
	assert.Equal(t,
		expect,
		got,
		"incorrect flights p2p response for departure day shift",
	)
}
