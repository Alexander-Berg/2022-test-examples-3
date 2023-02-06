package storage

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/flight"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/flightboard/format"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/flightstatus"
	"a.yandex-team.ru/travel/avia/shared_flights/lib/go/direction"
)

func TestStorageService_GetStationFlights_SingleFlightNoCodeshareNoStatus(t *testing.T) {
	h := serviceTestHelper(t)
	expect := format.FlightStationResponse{
		Station:   100,
		Direction: "departure",
		Flights: []flight.FlightSegmentWithCodeshares{
			{
				FlightSegment: flight.FlightSegment{
					CompanyIata:       "SU",
					CompanyRaspID:     26,
					Number:            "5555",
					Title:             "SU 5555",
					AirportFromIata:   "SVX",
					AirportFromRaspID: 100,
					AirportToIata:     "JFK",
					AirportToRaspID:   101,
					DepartureDay:      "2020-01-01",
					DepartureTime:     "05:00:00",
					DepartureTzName:   "Asia/Yekaterinburg",
					DepartureUTC:      "2020-01-01 00:00:00",
					DepartureTerminal: "A",
					ArrivalDay:        "2020-01-01",
					ArrivalTime:       "14:00:00",
					ArrivalTzName:     "America/New_York",
					ArrivalUTC:        "2020-01-01 19:00:00",
					ArrivalTerminal:   "B",
					Status: flightstatus.FlightStatus{
						Status:          "unknown",
						DepartureStatus: "unknown",
						ArrivalStatus:   "unknown",
						DepartureSource: "0",
						ArrivalSource:   "0",
					},
				},
			},
		},
	}
	got, err := h.Service.GetStationFlights(
		h.Station.SVX,
		time.Date(2019, 12, 31, 23, 0, 0, 0, time.UTC),
		time.Date(2020, 1, 1, 1, 0, 0, 0, time.UTC),
		0,
		true,
		direction.DEPARTURE,
		"",
		"",
		time.Date(2021, 1, 1, 1, 0, 0, 0, time.UTC),
	)

	assert.NoError(t, err, "cannot get departing flights for an SVX station")
	assert.Equal(t,
		expect,
		got,
		"incorrect flight station response",
	)
}
