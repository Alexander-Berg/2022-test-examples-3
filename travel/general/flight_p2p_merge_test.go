package flightp2p

import (
	"testing"

	"github.com/stretchr/testify/assert"

	dto "a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/DTO"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/flight_p2p/format"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/storage/flight"
	"a.yandex-team.ru/travel/avia/shared_flights/api/pkg/structs"
)

func TestStorageService_GetFlightsP2P_MergeFlights(t *testing.T) {
	mergeRuleStorage := flight.NewFlightMergeRuleStorage()
	mergeRuleStorage.UseLesserFlightNumberAsOperating(false)

	rule := structs.FlightMergeRule{
		ID:                    1,
		OperatingCarrier:      26,
		MarketingCarrier:      30,
		MarketingFlightRegexp: "272",
		IsActive:              true,
		ShouldMerge:           true,
	}
	mergeRuleStorage.AddRule(rule)

	result := mergeFlights([]format.Flight{}, mergeRuleStorage)
	assert.Empty(t, result, "merging empty list of flights should be empty")

	flightSU124 := format.Flight{
		TitledFlight: dto.TitledFlight{
			FlightID: dto.FlightID{
				AirlineID: 26,
				Number:    "124",
			},
			Title: "SU 124",
		},
		DepartureDatetime: "2020-04-07T12:34:00+05:00",
		DepartureStation:  123456,
		ArrivalDatetime:   "2020-04-08T13:45:00+03:00",
		ArrivalStation:    555555,
		Route:             dto.Route{123456, 555555},
	}

	flightSU125 := format.Flight{
		TitledFlight: dto.TitledFlight{
			FlightID: dto.FlightID{
				AirlineID: 26,
				Number:    "125",
			},
			Title: "SU 125",
		},
		DepartureDatetime: "2020-04-07T12:34:00+05:00",
		DepartureStation:  123456,
		ArrivalDatetime:   "2020-04-08T13:45:00+03:00",
		ArrivalStation:    654321,
		Route:             dto.Route{123456, 654321, 555555},
	}

	flights := []format.Flight{
		{
			// operating flight to be merged with the next one
			TitledFlight: dto.TitledFlight{
				FlightID: dto.FlightID{
					AirlineID: 26,
					Number:    "123",
				},
				Title: "SU 123",
			},
			DepartureDatetime: "2020-04-06T12:34:00+05:00",
			DepartureStation:  123456,
			ArrivalDatetime:   "2020-04-07T13:45:00+03:00",
			ArrivalStation:    654321,
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
			Route: dto.Route{123456, 654321},
		},
		{
			// should merge with the flight above
			TitledFlight: dto.TitledFlight{
				FlightID: dto.FlightID{
					AirlineID: 30,
					Number:    "272",
				},
				Title: "U6 272",
			},
			DepartureDatetime: "2020-04-06T12:34:00+05:00",
			DepartureStation:  123456,
			ArrivalDatetime:   "2020-04-07T13:45:00+03:00",
			ArrivalStation:    654321,
			Codeshares: []format.CodeshareFlight{
				format.CodeshareFlight{
					TitledFlight: dto.TitledFlight{
						FlightID: dto.FlightID{
							AirlineID: 96,
							Number:    "4272",
						},
						Title: "7R 4272",
					},
				},
			},
			Route: dto.Route{123456, 654321},
		},
		{
			// new departure date - should not merge with the flight above
			TitledFlight: dto.TitledFlight{
				FlightID: dto.FlightID{
					AirlineID: 30,
					Number:    "272",
				},
				Title: "U6 272",
			},
			DepartureDatetime: "2020-04-07T12:34:00+05:00",
			DepartureStation:  123456,
			ArrivalDatetime:   "2020-04-08T13:45:00+03:00",
			ArrivalStation:    654321,
			Codeshares: []format.CodeshareFlight{
				format.CodeshareFlight{
					TitledFlight: dto.TitledFlight{
						FlightID: dto.FlightID{
							AirlineID: 96,
							Number:    "5272",
						},
						Title: "7R 5272",
					},
				},
			},
			Route: dto.Route{123456, 654321},
		},
		// non-mergeable flight (arrival station is not the same),
		// departs at the same time, breaks the sequence of mergeable flights
		flightSU124,
		// non-mergeable flight (route is not the same),
		// departs at the same time
		flightSU125,
		{
			// should merge with the 30/172 flight three positions above
			TitledFlight: dto.TitledFlight{
				FlightID: dto.FlightID{
					AirlineID: 26,
					Number:    "126",
				},
				Title: "SU 126",
			},
			DepartureDatetime: "2020-04-07T12:34:00+05:00",
			DepartureStation:  123456,
			ArrivalDatetime:   "2020-04-08T13:45:00+03:00",
			ArrivalStation:    654321,
			Route:             dto.Route{123456, 654321},
		},
	}

	expected := []format.Flight{
		{
			// operating flight to be merged with the next one
			TitledFlight: dto.TitledFlight{
				FlightID: dto.FlightID{
					AirlineID: 26,
					Number:    "123",
				},
				Title: "SU 123",
			},
			DepartureDatetime: "2020-04-06T12:34:00+05:00",
			DepartureStation:  123456,
			ArrivalDatetime:   "2020-04-07T13:45:00+03:00",
			ArrivalStation:    654321,
			Codeshares: []format.CodeshareFlight{
				{
					TitledFlight: dto.TitledFlight{
						FlightID: dto.FlightID{
							AirlineID: 7,
							Number:    "1408",
						},
						Title: "B2 1408",
					},
				},
				{
					TitledFlight: dto.TitledFlight{
						FlightID: dto.FlightID{
							AirlineID: 30,
							Number:    "272",
						},
						Title: "U6 272",
					},
				},
				{
					TitledFlight: dto.TitledFlight{
						FlightID: dto.FlightID{
							AirlineID: 96,
							Number:    "4272",
						},
						Title: "7R 4272",
					},
				},
			},
			Route: dto.Route{123456, 654321},
		},
		{
			TitledFlight: dto.TitledFlight{
				FlightID: dto.FlightID{
					AirlineID: 26,
					Number:    "126",
				},
				Title: "SU 126",
			},
			DepartureDatetime: "2020-04-07T12:34:00+05:00",
			DepartureStation:  123456,
			ArrivalDatetime:   "2020-04-08T13:45:00+03:00",
			ArrivalStation:    654321,
			Route:             dto.Route{123456, 654321},
			Codeshares: []format.CodeshareFlight{
				{
					TitledFlight: dto.TitledFlight{
						FlightID: dto.FlightID{
							AirlineID: 30,
							Number:    "272",
						},
						Title: "U6 272",
					},
				},
				{
					TitledFlight: dto.TitledFlight{
						FlightID: dto.FlightID{
							AirlineID: 96,
							Number:    "5272",
						},
						Title: "7R 5272",
					},
				},
			},
		},
		flightSU124,
		flightSU125,
	}

	result = mergeFlights(flights, mergeRuleStorage)
	assert.Equal(t, expected, result, "incorrect result of merging p2p flights")
}
