package status

import (
	"reflect"
	"sort"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/appconst"
	"a.yandex-team.ru/travel/avia/shared_flights/api/pkg/structs"
	"a.yandex-team.ru/travel/proto/dicts/rasp"
	"a.yandex-team.ru/travel/proto/shared_flights/snapshots"
)

type mockTimezoneProvider struct{}

func (m *mockTimezoneProvider) GetTimeZoneByStationID(int64) *time.Location {
	return time.UTC
}

type mockStationStorage struct {
	MockedID map[string]int32
}

func (m mockStationStorage) ByIcao(string) (*snapshots.TStationWithCodes, bool) {
	return nil, false
}

func (m mockStationStorage) BySirena(code string) (*snapshots.TStationWithCodes, bool) {
	return &snapshots.TStationWithCodes{
		Station: &rasp.TStation{
			Id: m.MockedID[code],
		},
	}, true
}

func (m mockStationStorage) ByIata(string) (*snapshots.TStationWithCodes, bool) {
	return nil, false
}

func (m mockStationStorage) ByID(int64) (*snapshots.TStationWithCodes, bool) {
	return nil, false
}

func (m mockStationStorage) GetCode(id int64) string {
	return ""
}

func (m mockStationStorage) PutStation(*snapshots.TStationWithCodes) {
	panic("mock for tests, unexpected method call")
}

func TestGetFlightStatusKey_Basic(t *testing.T) {
	flightStatus := structs.FlightStatus{
		AirlineID:    14,
		FlightNumber: "1483S",
		LegNumber:    2,
		FlightDate:   "2019-12-11",
	}

	assert.Equal(t, "14.1483S.2", GetFlightStatusKey(&flightStatus))
}

func TestPutFlightStatus_ValidValue(t *testing.T) {
	s := NewStatusStorage(&mockTimezoneProvider{})

	got := structs.FlightStatus{
		AirlineID:    11,
		FlightNumber: "2468",
		LegNumber:    2,
		FlightDate:   "2019-12-11",
	}

	s.PutFlightStatus(got)
	statuses, _ := s.GetFlightStatuses("11.2468.2")
	expected, _ := statuses.GetStatus(20191211)
	assert.Equal(t, &got, expected)
}

func TestPutFlightStatus_UpdateWithPriority(t *testing.T) {
	lowPriority := structs.FlightStatus{
		AirlineID:              11,
		FlightNumber:           "2468",
		LegNumber:              2,
		FlightDate:             "2019-12-11",
		DepartureStatus:        "on-time",
		DepartureGate:          "1A",
		DepartureTerminal:      "C",
		DepartureTimeActual:    "09:27:00",
		DepartureTimeScheduled: "09:26:00",
		CheckInDesks:           "101-102",
		DepartureStation:       101,
		DepartureSourceID:      1,
		ArrivalTimeActual:      "10:27:00",
		ArrivalTimeScheduled:   "10:26:00",
		ArrivalStatus:          "waiting",
		ArrivalGate:            "2B",
		ArrivalTerminal:        "E",
		BaggageCarousels:       "7,8",
		ArrivalStation:         102,
		ArrivalSourceID:        1,
	}

	higherPriorityDep := structs.FlightStatus{
		AirlineID:              11,
		FlightNumber:           "2468",
		LegNumber:              2,
		FlightDate:             "2019-12-11",
		DepartureStatus:        "updated",
		DepartureGate:          "11A",
		DepartureTerminal:      "D",
		DepartureTimeActual:    "19:27:00",
		DepartureTimeScheduled: "19:26:00",
		CheckInDesks:           "201-202",
		DepartureStation:       201,
		DepartureSourceID:      2,
	}

	higherPriorityArr := structs.FlightStatus{
		AirlineID:            11,
		FlightNumber:         "2468",
		LegNumber:            2,
		FlightDate:           "2019-12-11",
		ArrivalTimeActual:    "20:27:00",
		ArrivalTimeScheduled: "20:26:00",
		ArrivalStatus:        "arrived",
		ArrivalGate:          "12B",
		ArrivalTerminal:      "X",
		BaggageCarousels:     "17,18",
		ArrivalStation:       202,
		ArrivalSourceID:      2,
	}

	// half-updated departure
	s := createStatusStorage()
	s.PutFlightStatus(lowPriority)
	s.PutFlightStatus(higherPriorityDep)
	statuses, _ := s.GetFlightStatuses("11.2468.2")
	got, _ := statuses.GetStatus(20191211)
	assert.Equal(t, &structs.FlightStatus{
		AirlineID:              11,
		FlightNumber:           "2468",
		LegNumber:              2,
		FlightDate:             "2019-12-11",
		DepartureStatus:        "updated",
		DepartureGate:          "11A",
		DepartureTerminal:      "D",
		DepartureTimeActual:    "19:27:00",
		DepartureTimeScheduled: "19:26:00",
		CheckInDesks:           "201-202",
		DepartureStation:       201,
		DepartureSourceID:      2,
		ArrivalTimeActual:      "10:27:00",
		ArrivalTimeScheduled:   "10:26:00",
		ArrivalStatus:          "waiting",
		ArrivalGate:            "2B",
		ArrivalTerminal:        "E",
		BaggageCarousels:       "7,8",
		ArrivalStation:         102,
		ArrivalSourceID:        1,
	}, got)

	// half-updated arrival
	s = createStatusStorage()
	s.PutFlightStatus(lowPriority)
	s.PutFlightStatus(higherPriorityArr)
	statuses, _ = s.GetFlightStatuses("11.2468.2")
	got, _ = statuses.GetStatus(20191211)
	assert.Equal(t, &structs.FlightStatus{
		AirlineID:              11,
		FlightNumber:           "2468",
		LegNumber:              2,
		FlightDate:             "2019-12-11",
		DepartureStatus:        "on-time",
		DepartureGate:          "1A",
		DepartureTerminal:      "C",
		DepartureTimeActual:    "09:27:00",
		DepartureTimeScheduled: "09:26:00",
		CheckInDesks:           "101-102",
		DepartureStation:       101,
		DepartureSourceID:      1,
		ArrivalTimeActual:      "20:27:00",
		ArrivalTimeScheduled:   "20:26:00",
		ArrivalStatus:          "arrived",
		ArrivalGate:            "12B",
		ArrivalTerminal:        "X",
		BaggageCarousels:       "17,18",
		ArrivalStation:         202,
		ArrivalSourceID:        2,
	}, got)

	// updated both departure and arrival
	s = createStatusStorage()
	s.PutFlightStatus(lowPriority)
	s.PutFlightStatus(higherPriorityDep)
	s.PutFlightStatus(higherPriorityArr)
	statuses, _ = s.GetFlightStatuses("11.2468.2")
	got, _ = statuses.GetStatus(20191211)
	assert.Equal(t, &structs.FlightStatus{
		AirlineID:              11,
		FlightNumber:           "2468",
		LegNumber:              2,
		FlightDate:             "2019-12-11",
		DepartureStatus:        "updated",
		DepartureGate:          "11A",
		DepartureTerminal:      "D",
		DepartureTimeActual:    "19:27:00",
		DepartureTimeScheduled: "19:26:00",
		CheckInDesks:           "201-202",
		DepartureStation:       201,
		DepartureSourceID:      2,
		ArrivalTimeActual:      "20:27:00",
		ArrivalTimeScheduled:   "20:26:00",
		ArrivalStatus:          "arrived",
		ArrivalGate:            "12B",
		ArrivalTerminal:        "X",
		BaggageCarousels:       "17,18",
		ArrivalStation:         202,
		ArrivalSourceID:        2,
	}, got)
}

func TestPutFlightStatus_UpdateWithPriorityCornerCases(t *testing.T) {
	lowPriority := structs.FlightStatus{
		AirlineID:         11,
		FlightNumber:      "2468",
		LegNumber:         2,
		FlightDate:        "2019-12-11",
		DepartureStatus:   "low-p",
		DepartureSourceID: 1,
		ArrivalStatus:     "low-p",
		ArrivalSourceID:   1,
	}

	higherPriorityDep := structs.FlightStatus{
		AirlineID:         11,
		FlightNumber:      "2468",
		LegNumber:         2,
		FlightDate:        "2019-12-11",
		DepartureStatus:   "high-p",
		DepartureSourceID: 2,
	}

	higherPriorityArr := structs.FlightStatus{
		AirlineID:       11,
		FlightNumber:    "2468",
		LegNumber:       2,
		FlightDate:      "2019-12-11",
		ArrivalStatus:   "high-p",
		ArrivalSourceID: 2,
	}

	higherPriorityDepAndArr := structs.FlightStatus{
		AirlineID:         11,
		FlightNumber:      "2468",
		LegNumber:         2,
		FlightDate:        "2019-12-11",
		DepartureStatus:   "high-p",
		DepartureSourceID: 2,
		ArrivalStatus:     "high-p",
		ArrivalSourceID:   2,
	}

	// updating half-filled high-priority status gets us the other half
	s := createStatusStorage()
	s.PutFlightStatus(higherPriorityDep)
	s.PutFlightStatus(lowPriority)
	statuses, _ := s.GetFlightStatuses("11.2468.2")
	got, _ := statuses.GetStatus(20191211)
	assert.Equal(t, &structs.FlightStatus{
		AirlineID:         11,
		FlightNumber:      "2468",
		LegNumber:         2,
		FlightDate:        "2019-12-11",
		DepartureStatus:   "high-p",
		DepartureSourceID: 2,
		ArrivalStatus:     "low-p",
		ArrivalSourceID:   1,
	}, got)

	// same for the half-updated arrival case
	s = createStatusStorage()
	s.PutFlightStatus(higherPriorityArr)
	s.PutFlightStatus(lowPriority)
	statuses, _ = s.GetFlightStatuses("11.2468.2")
	got, _ = statuses.GetStatus(20191211)
	assert.Equal(t, &structs.FlightStatus{
		AirlineID:         11,
		FlightNumber:      "2468",
		LegNumber:         2,
		FlightDate:        "2019-12-11",
		DepartureStatus:   "low-p",
		DepartureSourceID: 1,
		ArrivalStatus:     "high-p",
		ArrivalSourceID:   2,
	}, got)

	// but when both departure and arrival are already high priority, updating with low does not change anything
	s = createStatusStorage()
	s.PutFlightStatus(higherPriorityDepAndArr)
	s.PutFlightStatus(lowPriority)
	statuses, _ = s.GetFlightStatuses("11.2468.2")
	got, _ = statuses.GetStatus(20191211)
	assert.Equal(t, &higherPriorityDepAndArr, got)

	// verify that high-priority updates don't conflict
	s = createStatusStorage()
	s.PutFlightStatus(higherPriorityDep)
	s.PutFlightStatus(higherPriorityArr)
	s.PutFlightStatus(lowPriority)
	statuses, _ = s.GetFlightStatuses("11.2468.2")
	got, _ = statuses.GetStatus(20191211)
	assert.Equal(t, &higherPriorityDepAndArr, got)

	// "unknown" and "no-data" imply ignoring the priority
	s = createStatusStorage()
	s.PutFlightStatus(structs.FlightStatus{
		AirlineID:         11,
		FlightNumber:      "2468",
		LegNumber:         2,
		FlightDate:        "2019-12-11",
		DepartureStatus:   "unknown",
		DepartureSourceID: 2,
		ArrivalStatus:     "no-data",
		ArrivalSourceID:   2,
	})
	s.PutFlightStatus(lowPriority)
	statuses, _ = s.GetFlightStatuses("11.2468.2")
	got, _ = statuses.GetStatus(20191211)
	assert.Equal(t, &lowPriority, got)

	// any update gets into the storage when the data source value is not specified
	s = createStatusStorage()
	s.PutFlightStatus(structs.FlightStatus{
		AirlineID:       11,
		FlightNumber:    "2468",
		LegNumber:       2,
		FlightDate:      "2019-12-11",
		DepartureStatus: "high",
		ArrivalStatus:   "high",
	})
	s.PutFlightStatus(lowPriority)
	statuses, _ = s.GetFlightStatuses("11.2468.2")
	got, _ = statuses.GetStatus(20191211)
	assert.Equal(t, &lowPriority, got)
}

func extractScheduled(fs structs.FlightStatus) (structs.FlightStatus, string, string) {
	return fs, fs.DepartureTimeScheduled, fs.ArrivalTimeScheduled
}

func TestPutCanceledFlights_CheckBoundaries(t *testing.T) {
	s := NewStatusStorage(&mockTimezoneProvider{})
	s.(*statusStorageImpl).ReferenceTime = time.Date(2019, 12, 10, 12, 0, 0, 0, time.UTC)

	// about 23 hours in the future / by departure
	flightStatus1d := structs.FlightStatus{
		AirlineID:              11,
		CarrierCode:            "ZZ",
		FlightNumber:           "1001",
		LegNumber:              1,
		FlightDate:             "2019-12-10",
		DepartureSourceID:      appconst.AirportFlightStatusSource,
		DepartureStatus:        string(appconst.FlightStatusCancelled),
		DepartureTimeScheduled: "2019-12-11 11:15:00",
		DepartureStation:       100,
	}

	// about 2 hours in the past / by departure
	flightStatus2d := structs.FlightStatus{
		AirlineID:              11,
		CarrierCode:            "ZZ",
		FlightNumber:           "1002",
		LegNumber:              1,
		FlightDate:             "2019-12-10",
		DepartureSourceID:      appconst.AirportFlightStatusSource,
		DepartureStatus:        string(appconst.FlightStatusCancelled),
		DepartureTimeScheduled: "2019-12-10 10:15:00",
		DepartureStation:       100,
	}

	// more than 24 hours in the future / by departure
	flightStatus3d := structs.FlightStatus{
		AirlineID:              11,
		CarrierCode:            "ZZ",
		FlightNumber:           "1003",
		LegNumber:              1,
		FlightDate:             "2019-12-10",
		DepartureSourceID:      appconst.AirportFlightStatusSource,
		DepartureStatus:        string(appconst.FlightStatusCancelled),
		DepartureTimeScheduled: "2019-12-11 12:15:00",
		DepartureStation:       100,
	}

	// more than 2 hours in the past / by departure
	flightStatus4d := structs.FlightStatus{
		AirlineID:              11,
		CarrierCode:            "ZZ",
		FlightNumber:           "1004",
		LegNumber:              1,
		FlightDate:             "2019-12-10",
		DepartureSourceID:      appconst.AirportFlightStatusSource,
		DepartureStatus:        string(appconst.FlightStatusCancelled),
		DepartureTimeScheduled: "2019-12-10 09:15:00",
		DepartureStation:       100,
	}

	// wrong source / by departure
	flightStatus5d := structs.FlightStatus{
		AirlineID:              11,
		CarrierCode:            "ZZ",
		FlightNumber:           "1005",
		LegNumber:              1,
		FlightDate:             "2019-12-10",
		DepartureSourceID:      appconst.AirportFlightStatusSource - 1,
		DepartureStatus:        string(appconst.FlightStatusCancelled),
		DepartureTimeScheduled: "2019-12-10 10:15:00",
		DepartureStation:       100,
	}

	// wrong status / by departure
	flightStatus6d := structs.FlightStatus{
		AirlineID:              11,
		CarrierCode:            "ZZ",
		FlightNumber:           "1006",
		LegNumber:              1,
		FlightDate:             "2019-12-10",
		DepartureSourceID:      appconst.AirportFlightStatusSource,
		DepartureStatus:        string(appconst.FlightStatusDeparted),
		DepartureTimeScheduled: "2019-12-10 10:15:00",
		DepartureStation:       100,
	}

	// about 23 hours in the future / by arrival
	flightStatus1a := structs.FlightStatus{
		AirlineID:            11,
		CarrierCode:          "ZZ",
		FlightNumber:         "2001",
		LegNumber:            1,
		FlightDate:           "2019-12-10",
		ArrivalSourceID:      appconst.AirportFlightStatusSource,
		ArrivalStatus:        string(appconst.FlightStatusCancelled),
		ArrivalTimeScheduled: "2019-12-11 11:15:00",
		ArrivalStation:       100,
	}

	// about 2 hours in the past / by arrival
	flightStatus2a := structs.FlightStatus{
		AirlineID:            11,
		CarrierCode:          "ZZ",
		FlightNumber:         "2002",
		LegNumber:            1,
		FlightDate:           "2019-12-10",
		ArrivalSourceID:      appconst.AirportFlightStatusSource,
		ArrivalStatus:        string(appconst.FlightStatusCancelled),
		ArrivalTimeScheduled: "2019-12-10 10:15:00",
		ArrivalStation:       100,
	}

	// more than 24 hours in the future / by arrival
	flightStatus3a := structs.FlightStatus{
		AirlineID:            11,
		CarrierCode:          "ZZ",
		FlightNumber:         "2003",
		LegNumber:            1,
		FlightDate:           "2019-12-10",
		ArrivalSourceID:      appconst.AirportFlightStatusSource,
		ArrivalStatus:        string(appconst.FlightStatusCancelled),
		ArrivalTimeScheduled: "2019-12-11 12:15:00",
		ArrivalStation:       100,
	}

	// more than 2 hours in the past / by arrival
	flightStatus4a := structs.FlightStatus{
		AirlineID:            11,
		CarrierCode:          "ZZ",
		FlightNumber:         "2004",
		LegNumber:            1,
		FlightDate:           "2019-12-10",
		ArrivalSourceID:      appconst.AirportFlightStatusSource,
		ArrivalStatus:        string(appconst.FlightStatusCancelled),
		ArrivalTimeScheduled: "2019-12-10 09:15:00",
		ArrivalStation:       100,
	}

	// wrong source / by departure
	flightStatus5a := structs.FlightStatus{
		AirlineID:            11,
		CarrierCode:          "ZZ",
		FlightNumber:         "2005",
		LegNumber:            1,
		FlightDate:           "2019-12-10",
		ArrivalSourceID:      appconst.AirportFlightStatusSource - 1,
		ArrivalStatus:        string(appconst.FlightStatusCancelled),
		ArrivalTimeScheduled: "2019-12-10 10:15:00",
		ArrivalStation:       100,
	}

	// wrong status / by departure
	flightStatus6a := structs.FlightStatus{
		AirlineID:            11,
		CarrierCode:          "ZZ",
		FlightNumber:         "2006",
		LegNumber:            1,
		FlightDate:           "2019-12-10",
		ArrivalSourceID:      appconst.AirportFlightStatusSource,
		ArrivalStatus:        string(appconst.FlightStatusArrived),
		ArrivalTimeScheduled: "2019-12-10 10:15:00",
		ArrivalStation:       100,
	}

	s.UpdateFlightDelays(extractScheduled(flightStatus1d))
	s.UpdateFlightDelays(extractScheduled(flightStatus2d))
	s.UpdateFlightDelays(extractScheduled(flightStatus3d))
	s.UpdateFlightDelays(extractScheduled(flightStatus4d))
	s.UpdateFlightDelays(extractScheduled(flightStatus5d))
	s.UpdateFlightDelays(extractScheduled(flightStatus6d))
	s.UpdateFlightDelays(extractScheduled(flightStatus1a))
	s.UpdateFlightDelays(extractScheduled(flightStatus2a))
	s.UpdateFlightDelays(extractScheduled(flightStatus3a))
	s.UpdateFlightDelays(extractScheduled(flightStatus4a))
	s.UpdateFlightDelays(extractScheduled(flightStatus5a))
	s.UpdateFlightDelays(extractScheduled(flightStatus6a))
	cancelledFlights, hasCancelled := s.GetCancelledFlights(int64(100))
	assert.True(t, hasCancelled)
	cancelledList := make([]string, 0, len(cancelledFlights))
	for key := range cancelledFlights {
		cancelledList = append(cancelledList, key)
	}
	sort.Slice(cancelledList, func(pos1, pos2 int) bool {
		return cancelledList[pos1] < cancelledList[pos2]
	})
	expected := []string{
		"ZZ.1001",
		"ZZ.1002",
		"ZZ.2001",
		"ZZ.2002",
	}
	assert.Equal(t, expected, cancelledList)

	_, hasDelayed := s.GetDelayedFlights(int64(100))
	assert.False(t, hasDelayed)
}

func TestPutDelayedFlights_CheckBoundaries(t *testing.T) {
	s := NewStatusStorage(&mockTimezoneProvider{})
	s.(*statusStorageImpl).ReferenceTime = time.Date(2019, 12, 10, 12, 0, 0, 0, time.UTC)

	// scheduled in the past, actual in the future / by departure
	flightStatus1d := structs.FlightStatus{
		AirlineID:              11,
		CarrierCode:            "ZZ",
		FlightNumber:           "1001",
		LegNumber:              1,
		FlightDate:             "2019-12-10",
		DepartureSourceID:      appconst.AirportFlightStatusSource,
		DepartureStatus:        string(appconst.FlightStatusOnTime),
		DepartureTimeScheduled: "2019-12-10 11:55:00",
		DepartureTimeActual:    "2019-12-10 12:35:00",
		DepartureStation:       100,
	}

	// scheduled and actual are both in the future / by departure
	flightStatus2d := structs.FlightStatus{
		AirlineID:              11,
		CarrierCode:            "ZZ",
		FlightNumber:           "1002",
		LegNumber:              1,
		FlightDate:             "2019-12-10",
		DepartureSourceID:      appconst.AirportFlightStatusSource,
		DepartureStatus:        string(appconst.FlightStatusOnTime),
		DepartureTimeScheduled: "2019-12-10 12:20:00",
		DepartureTimeActual:    "2019-12-10 12:55:00",
		DepartureStation:       100,
	}

	// scheduled and actual are both in the future but too close / by departure
	flightStatus3d := structs.FlightStatus{
		AirlineID:              11,
		CarrierCode:            "ZZ",
		FlightNumber:           "1003",
		LegNumber:              1,
		FlightDate:             "2019-12-10",
		DepartureSourceID:      appconst.AirportFlightStatusSource,
		DepartureStatus:        string(appconst.FlightStatusOnTime),
		DepartureTimeScheduled: "2019-12-10 12:20:00",
		DepartureTimeActual:    "2019-12-10 12:40:00",
		DepartureStation:       100,
	}

	// scheduled in the past, actual in the future, but too close / by departure
	flightStatus4d := structs.FlightStatus{
		AirlineID:              11,
		CarrierCode:            "ZZ",
		FlightNumber:           "1004",
		LegNumber:              1,
		FlightDate:             "2019-12-10",
		DepartureSourceID:      appconst.AirportFlightStatusSource,
		DepartureStatus:        string(appconst.FlightStatusOnTime),
		DepartureTimeScheduled: "2019-12-10 11:55:00",
		DepartureTimeActual:    "2019-12-10 12:15:00",
		DepartureStation:       100,
	}

	// wrong source / by departure
	flightStatus5d := structs.FlightStatus{
		AirlineID:              11,
		CarrierCode:            "ZZ",
		FlightNumber:           "1005",
		LegNumber:              1,
		FlightDate:             "2019-12-10",
		DepartureSourceID:      appconst.AirportFlightStatusSource - 1,
		DepartureStatus:        string(appconst.FlightStatusOnTime),
		DepartureTimeScheduled: "2019-12-10 11:55:00",
		DepartureTimeActual:    "2019-12-10 12:35:00",
		DepartureStation:       100,
	}

	// wrong status / by departure
	flightStatus6d := structs.FlightStatus{
		AirlineID:              11,
		CarrierCode:            "ZZ",
		FlightNumber:           "1006",
		LegNumber:              1,
		FlightDate:             "2019-12-10",
		DepartureSourceID:      appconst.AirportFlightStatusSource,
		DepartureStatus:        string(appconst.FlightStatusCancelled),
		DepartureTimeScheduled: "2019-12-10 11:55:00",
		DepartureTimeActual:    "2019-12-10 12:35:00",
		DepartureStation:       100,
	}

	// scheduled and actual are both in the past / by departure
	flightStatus7d := structs.FlightStatus{
		AirlineID:              11,
		CarrierCode:            "ZZ",
		FlightNumber:           "1007",
		LegNumber:              1,
		FlightDate:             "2019-12-10",
		DepartureSourceID:      appconst.AirportFlightStatusSource,
		DepartureStatus:        string(appconst.FlightStatusOnTime),
		DepartureTimeScheduled: "2019-12-10 11:05:00",
		DepartureTimeActual:    "2019-12-10 11:45:00",
		DepartureStation:       100,
	}

	// scheduled and actual are too far in the future / by departure
	flightStatus8d := structs.FlightStatus{
		AirlineID:              11,
		CarrierCode:            "ZZ",
		FlightNumber:           "1008",
		LegNumber:              1,
		FlightDate:             "2019-12-10",
		DepartureSourceID:      appconst.AirportFlightStatusSource,
		DepartureStatus:        string(appconst.FlightStatusOnTime),
		DepartureTimeScheduled: "2019-12-10 12:35:00",
		DepartureTimeActual:    "2019-12-10 14:45:00",
		DepartureStation:       100,
	}

	// scheduled in the past, actual in the future / by arrival
	flightStatus1a := structs.FlightStatus{
		AirlineID:            11,
		CarrierCode:          "ZZ",
		FlightNumber:         "2001",
		LegNumber:            1,
		FlightDate:           "2019-12-10",
		ArrivalSourceID:      appconst.AirportFlightStatusSource,
		ArrivalStatus:        string(appconst.FlightStatusOnTime),
		ArrivalTimeScheduled: "2019-12-10 11:55:00",
		ArrivalTimeActual:    "2019-12-10 12:35:00",
		ArrivalStation:       100,
	}

	// scheduled and actual are both in the future / by arrival
	flightStatus2a := structs.FlightStatus{
		AirlineID:            11,
		CarrierCode:          "ZZ",
		FlightNumber:         "2002",
		LegNumber:            1,
		FlightDate:           "2019-12-10",
		ArrivalSourceID:      appconst.AirportFlightStatusSource,
		ArrivalStatus:        string(appconst.FlightStatusOnTime),
		ArrivalTimeScheduled: "2019-12-10 13:20:00",
		ArrivalTimeActual:    "2019-12-10 15:55:00",
		ArrivalStation:       100,
	}

	// scheduled and actual are both in the future but too close / by arrival
	flightStatus3a := structs.FlightStatus{
		AirlineID:            11,
		CarrierCode:          "ZZ",
		FlightNumber:         "2003",
		LegNumber:            1,
		FlightDate:           "2019-12-10",
		ArrivalSourceID:      appconst.AirportFlightStatusSource,
		ArrivalStatus:        string(appconst.FlightStatusOnTime),
		ArrivalTimeScheduled: "2019-12-10 12:20:00",
		ArrivalTimeActual:    "2019-12-10 12:40:00",
		ArrivalStation:       100,
	}

	// scheduled in the past, actual in the future, but too close / by arrival
	flightStatus4a := structs.FlightStatus{
		AirlineID:            11,
		CarrierCode:          "ZZ",
		FlightNumber:         "2004",
		LegNumber:            1,
		FlightDate:           "2019-12-10",
		ArrivalSourceID:      appconst.AirportFlightStatusSource,
		ArrivalStatus:        string(appconst.FlightStatusOnTime),
		ArrivalTimeScheduled: "2019-12-10 11:55:00",
		ArrivalTimeActual:    "2019-12-10 12:15:00",
		ArrivalStation:       100,
	}

	// wrong source / by arrival
	flightStatus5a := structs.FlightStatus{
		AirlineID:            11,
		CarrierCode:          "ZZ",
		FlightNumber:         "2005",
		LegNumber:            1,
		FlightDate:           "2019-12-10",
		ArrivalSourceID:      appconst.AirportFlightStatusSource - 1,
		ArrivalStatus:        string(appconst.FlightStatusOnTime),
		ArrivalTimeScheduled: "2019-12-10 11:55:00",
		ArrivalTimeActual:    "2019-12-10 12:35:00",
		ArrivalStation:       100,
	}

	// wrong status / by arrival
	flightStatus6a := structs.FlightStatus{
		AirlineID:            11,
		CarrierCode:          "ZZ",
		FlightNumber:         "2006",
		LegNumber:            1,
		FlightDate:           "2019-12-10",
		ArrivalSourceID:      appconst.AirportFlightStatusSource,
		ArrivalStatus:        string(appconst.FlightStatusCancelled),
		ArrivalTimeScheduled: "2019-12-10 11:55:00",
		ArrivalTimeActual:    "2019-12-10 12:35:00",
		ArrivalStation:       100,
	}

	// scheduled and actual are both in the past / by arrival
	flightStatus7a := structs.FlightStatus{
		AirlineID:            11,
		CarrierCode:          "ZZ",
		FlightNumber:         "2007",
		LegNumber:            1,
		FlightDate:           "2019-12-10",
		ArrivalSourceID:      appconst.AirportFlightStatusSource,
		ArrivalStatus:        string(appconst.FlightStatusOnTime),
		ArrivalTimeScheduled: "2019-12-10 11:05:00",
		ArrivalTimeActual:    "2019-12-10 11:45:00",
		ArrivalStation:       100,
	}

	// scheduled and actual are too far in the future / by arrival
	flightStatus8a := structs.FlightStatus{
		AirlineID:            11,
		CarrierCode:          "ZZ",
		FlightNumber:         "2008",
		LegNumber:            1,
		FlightDate:           "2019-12-10",
		ArrivalSourceID:      appconst.AirportFlightStatusSource,
		ArrivalStatus:        string(appconst.FlightStatusOnTime),
		ArrivalTimeScheduled: "2019-12-10 14:05:00",
		ArrivalTimeActual:    "2019-12-10 15:45:00",
		ArrivalStation:       100,
	}

	s.UpdateFlightDelays(extractScheduled(flightStatus1d))
	s.UpdateFlightDelays(extractScheduled(flightStatus2d))
	s.UpdateFlightDelays(extractScheduled(flightStatus3d))
	s.UpdateFlightDelays(extractScheduled(flightStatus4d))
	s.UpdateFlightDelays(extractScheduled(flightStatus5d))
	s.UpdateFlightDelays(extractScheduled(flightStatus6d))
	s.UpdateFlightDelays(extractScheduled(flightStatus7d))
	s.UpdateFlightDelays(extractScheduled(flightStatus8d))
	s.UpdateFlightDelays(extractScheduled(flightStatus1a))
	s.UpdateFlightDelays(extractScheduled(flightStatus2a))
	s.UpdateFlightDelays(extractScheduled(flightStatus3a))
	s.UpdateFlightDelays(extractScheduled(flightStatus4a))
	s.UpdateFlightDelays(extractScheduled(flightStatus5a))
	s.UpdateFlightDelays(extractScheduled(flightStatus6a))
	s.UpdateFlightDelays(extractScheduled(flightStatus7a))
	s.UpdateFlightDelays(extractScheduled(flightStatus8a))
	delayedFlights, hasDelayed := s.GetDelayedFlights(int64(100))
	assert.True(t, hasDelayed)
	delayedList := make([]string, 0, len(delayedFlights))
	for key := range delayedFlights {
		delayedList = append(delayedList, key)
	}
	sort.Slice(delayedList, func(pos1, pos2 int) bool {
		return delayedList[pos1] < delayedList[pos2]
	})
	expected := []string{
		"ZZ.1001",
		"ZZ.1002",
		"ZZ.2001",
		"ZZ.2002",
	}
	assert.Equal(t, expected, delayedList)

	_, hasCancelled := s.GetCancelledFlights(int64(100))
	assert.True(t, hasCancelled)
}

func createStatusStorage() StatusStorage {
	s := NewStatusStorage(&mockTimezoneProvider{})
	s.PutStatusSource(structs.FlightStatusSource{
		ID:       1,
		Name:     "low priority",
		Priority: 10,
	})
	s.PutStatusSource(structs.FlightStatusSource{
		ID:       2,
		Name:     "high priority",
		Priority: 20,
	})
	return s
}

func Test_statusStorageImpl_UpdateDivertedIDs(t *testing.T) {
	statusStorage := NewStatusStorage(&mockTimezoneProvider{})
	stationStorage := &mockStationStorage{map[string]int32{"ABC": 123, "DEF": 456}}

	status := structs.FlightStatus{
		AirlineID:                    100,
		FlightNumber:                 "100",
		LegNumber:                    1,
		FlightDate:                   "2020-06-01",
		DepartureDivertedAirportCode: "ABC",
		ArrivalDivertedAirportCode:   "DEF",
	}
	statusStorage.PutFlightStatus(status)

	statusStorage.UpdateDivertedIDs(stationStorage)

	statuses, exist := statusStorage.GetFlightStatuses(GetFlightStatusKey(&status))
	assert.True(t, exist, "One status should exist")

	assert.Len(t, statuses.statuses, 1, "Single status should exist")

	for _, v := range statuses.statuses {
		assert.Equal(t, int32(123), v.DepartureDivertedAirportID, "ID should be resolved to 123")
		assert.Equal(t, int32(456), v.ArrivalDivertedAirportID, "ID should be resolved to 456")
	}

}

func Test_StationID(t *testing.T) {
	type args struct {
		fn func(code string) (*snapshots.TStationWithCodes, bool)
	}
	doesntMatter := ""
	tests := []struct {
		name     string
		args     args
		wantInt  int32
		wantBool bool
	}{
		{
			"working station resolver",
			args{func(code string) (*snapshots.TStationWithCodes, bool) {
				return &snapshots.TStationWithCodes{
					Station: &rasp.TStation{
						Id: 123,
					},
				}, true
			}},
			123,
			true,
		},
		{
			"station resolver with unfilled rasp.TStation",
			args{func(code string) (*snapshots.TStationWithCodes, bool) {
				return &snapshots.TStationWithCodes{}, true
			}},
			0,
			false,
		},
		{
			"station resolver with nil sUpdateDivertedIDstation",
			args{func(code string) (*snapshots.TStationWithCodes, bool) {
				return nil, true
			}},
			0,
			false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			gotInt, gotBool := stationID(tt.args.fn)(doesntMatter)
			if !(reflect.DeepEqual(gotInt, tt.wantInt) && reflect.DeepEqual(gotBool, tt.wantBool)) {
				t.Errorf("stationID() = %v %v, want %v %v", gotInt, gotBool, tt.wantInt, tt.wantBool)
			}
		})
	}
}
