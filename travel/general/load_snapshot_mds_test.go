package loadsnapshot

import (
	"reflect"
	"testing"
	"time"

	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/storage/station"
	"a.yandex-team.ru/travel/avia/shared_flights/api/pkg/structs"
	"a.yandex-team.ru/travel/proto/dicts/rasp"
	"a.yandex-team.ru/travel/proto/shared_flights/snapshots"
)

type mockTimezoneProvider struct{}

func (m *mockTimezoneProvider) GetTimeZoneByStationID(int64) *time.Location {
	return time.UTC
}

func Test_CreateDopFlight(t *testing.T) {
	stations := station.NewStationStorage()
	stations.PutStation(&snapshots.TStationWithCodes{
		Station: &rasp.TStation{
			Id: 200,
		},
		IataCode: "YYY",
	})

	flightStatus := structs.FlightStatus{
		DepartureStation:       100,
		DepartureTimeScheduled: "2020-03-02 19:10:00",
		ArrivalStation:         200,
		ArrivalTimeScheduled:   "2020-03-03 05:10:00",
		AirlineID:              26,
		CarrierCode:            "QQQ",
		FlightNumber:           "1948",
		DepartureTerminal:      "D",
		ArrivalTerminal:        "A",
	}
	flightBase, flightPattern, ok := CreateDopFlight(
		flightStatus,
		14,
		&mockTimezoneProvider{},
		stations,
	)

	expectedFlightBase := structs.FlightBase{
		ID:                     14,
		OperatingCarrier:       26,
		OperatingCarrierCode:   "QQQ",
		OperatingFlightNumber:  "1948",
		DepartureStation:       100,
		DepartureTimeScheduled: 1910,
		DepartureTerminal:      "D",
		ArrivalStation:         200,
		ArrivalStationCode:     "YYY",
		ArrivalTimeScheduled:   510,
		ArrivalTerminal:        "A",
		LegNumber:              1,
		Source:                 structs.DopSource,
	}
	expectedFlightPattern := structs.FlightPattern{
		ID:                       14,
		FlightBaseID:             14,
		LegNumber:                1,
		OperatingFromDate:        "2020-03-02",
		OperatingUntilDate:       "2020-03-02",
		OperatingOnDays:          1,
		MarketingCarrier:         26,
		MarketingCarrierCode:     "QQQ",
		MarketingFlightNumber:    "1948",
		ArrivalDayShift:          1,
		OperatingFlightPatternID: 14,
		IsDop:                    true,
	}
	if !ok {
		t.Errorf("Unable to create dop flight for flight status %+v", flightStatus)
	}
	if !reflect.DeepEqual(flightBase, expectedFlightBase) {
		t.Errorf("Actual flightBase = %+v, expected = %+v", flightBase, expectedFlightBase)
	}
	if !reflect.DeepEqual(flightPattern, expectedFlightPattern) {
		t.Errorf("Actual flightPattern = %+v, expected = %+v", flightPattern, expectedFlightPattern)
	}
}
