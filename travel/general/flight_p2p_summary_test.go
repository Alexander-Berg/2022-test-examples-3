package flightp2p

import (
	"sort"
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/flight_p2p/format"
)

func TestStorageService_GetFlightsP2PSummary(t *testing.T) {
	h := serviceTestHelper(t)
	expect := format.SummaryResponse{
		Flights: []format.SummaryResponseEntry{
			{
				DepartureStation:  100,
				ArrivalStation:    101,
				FlightsCount:      2,
				TotalFlightsCount: 2,
			},
			{
				DepartureStation:  100,
				ArrivalStation:    9600215,
				FlightsCount:      2,
				TotalFlightsCount: 2,
			},
			{
				DepartureStation:  100,
				ArrivalStation:    9600366,
				FlightsCount:      1,
				TotalFlightsCount: 1,
			},
			{
				DepartureStation:  100,
				ArrivalStation:    9600381,
				FlightsCount:      1,
				TotalFlightsCount: 1,
			},
			{
				DepartureStation:  101,
				ArrivalStation:    100,
				FlightsCount:      1,
				TotalFlightsCount: 1,
			},
			{
				DepartureStation:  101,
				ArrivalStation:    9600215,
				FlightsCount:      1,
				TotalFlightsCount: 1,
			},
			{
				DepartureStation:  101,
				ArrivalStation:    9600366,
				FlightsCount:      1,
				TotalFlightsCount: 1,
			},
			{
				DepartureStation:  102,
				ArrivalStation:    100,
				FlightsCount:      1,
				TotalFlightsCount: 1,
			},
			{
				DepartureStation:  9600174,
				ArrivalStation:    100,
				FlightsCount:      1,
				TotalFlightsCount: 1,
			},
			{
				DepartureStation:  9600215,
				ArrivalStation:    100,
				FlightsCount:      1,
				DopFlightsCount:   1,
				TotalFlightsCount: 2,
			},
			{
				DepartureStation:  9600215,
				ArrivalStation:    101,
				FlightsCount:      1,
				TotalFlightsCount: 1,
			},
			{
				DepartureStation:  9600215,
				ArrivalStation:    9600366,
				FlightsCount:      2,
				TotalFlightsCount: 2,
			},
			{
				DepartureStation:  9600215,
				ArrivalStation:    9600381,
				FlightsCount:      1,
				TotalFlightsCount: 1,
			},
			{
				DepartureStation:  9600366,
				ArrivalStation:    101,
				FlightsCount:      1,
				TotalFlightsCount: 1,
			},
			{
				DepartureStation:  9600366,
				ArrivalStation:    9600215,
				FlightsCount:      1,
				TotalFlightsCount: 1,
			},
			{
				DepartureStation:  9600366,
				ArrivalStation:    9600381,
				FlightsCount:      1,
				TotalFlightsCount: 1,
			},
			{
				DepartureStation:  9600381,
				ArrivalStation:    100,
				FlightsCount:      1,
				TotalFlightsCount: 1,
			},
			{
				DepartureStation:  9600381,
				ArrivalStation:    9600174,
				FlightsCount:      1,
				TotalFlightsCount: 1,
			},
		},
	}
	actual, _ := h.Service.GetFlightsP2PSummary(false)
	sort.Slice(actual.Flights, func(i, j int) bool {
		if actual.Flights[i].DepartureStation != actual.Flights[j].DepartureStation {
			return actual.Flights[i].DepartureStation < actual.Flights[j].DepartureStation
		}
		return actual.Flights[i].ArrivalStation < actual.Flights[j].ArrivalStation
	})

	assert.Equal(t,
		expect,
		actual,
		"incorrect flights p2p summary response",
	)
}
