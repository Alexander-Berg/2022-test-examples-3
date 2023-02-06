package flightboard

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/flightdata"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/threads"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/timezone"
	storageCache "a.yandex-team.ru/travel/avia/shared_flights/api/internal/storage"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/storage/flight"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/storage/station"
	"a.yandex-team.ru/travel/avia/shared_flights/api/pkg/structs"
	dir "a.yandex-team.ru/travel/avia/shared_flights/lib/go/direction"
)

func TestStorageService_expandFlights_NoData(t *testing.T) {
	storage := storageCache.NewStorage()
	tz := timezone.NewTimeZoneUtil(storage.Timezones(), storage.Stations())
	trs := threads.NewThreadRouteService(storage, tz)
	service := &FlightBoardServiceImpl{
		Storage:            storage,
		TimeZoneUtil:       tz,
		ThreadRouteService: trs,
	}
	got, err := service.expandSegments(
		[]*structs.FlightPattern{},
		[]FlightPatternAndSortKey{},
		make(map[int32]station.FlightPatternExtras),
		time.Time{},
		time.Time{},
		nil,
		nil,
		nil,
		"",
		0,
	)
	assert.NoError(t, err, "Should not fail on empty data")
	assert.Empty(t, got, "Should not have data")
}

func TestStorageService_mergeSegments(t *testing.T) {
	storage := storageCache.NewStorage()
	mergeRuleStorage := flight.NewFlightMergeRuleStorage()
	mergeRuleStorage.UseLesserFlightNumberAsOperating(false)
	rule := structs.FlightMergeRule{
		ID:                    1,
		OperatingCarrier:      26,
		OperatingFlightRegexp: "123",
		MarketingCarrier:      30,
		MarketingFlightRegexp: "272",
		ShouldMerge:           true,
		IsActive:              true,
	}
	mergeRuleStorage.AddRule(rule)
	storage.SetFlightMergeRuleStorage(mergeRuleStorage)

	tz := timezone.NewTimeZoneUtil(storage.Timezones(), storage.Stations())
	trs := threads.NewThreadRouteService(storage, tz)
	service := &FlightBoardServiceImpl{
		Storage:            storage,
		TimeZoneUtil:       tz,
		ThreadRouteService: trs,
	}
	got := service.mergeSegments([]FlightOnDateWithCodeshares{})
	assert.Empty(t, got, "Should not fail on empty data")

	got = service.mergeSegments(
		[]FlightOnDateWithCodeshares{
			{
				FlightData: &flightdata.FlightData{
					FlightDepartureDate: 20200310,
					FlightDataBase: &flightdata.FlightDataBase{
						FlightBase: structs.FlightBase{
							DepartureTimeScheduled: 1400,
							DepartureStation:       123456,
							ArrivalStation:         654321,
						},
						FlightPattern: &structs.FlightPattern{
							MarketingCarrier:      26,
							MarketingFlightNumber: "123",
						},
					},
				},
			},
			{
				// Should merge with the flight above
				FlightData: &flightdata.FlightData{
					FlightDepartureDate: 20200310,
					FlightDataBase: &flightdata.FlightDataBase{
						FlightBase: structs.FlightBase{
							DepartureTimeScheduled: 1400,
							DepartureStation:       123456,
							ArrivalStation:         654321,
						},
						FlightPattern: &structs.FlightPattern{
							MarketingCarrier:      30,
							MarketingFlightNumber: "272",
						},
					},
				},
			},
			{
				// New departure date - should not merge with the flight above
				FlightData: &flightdata.FlightData{
					FlightDepartureDate: 20200311,
					FlightDataBase: &flightdata.FlightDataBase{
						FlightBase: structs.FlightBase{
							DepartureTimeScheduled: 1400,
							DepartureStation:       123456,
							ArrivalStation:         654321,
						},
						FlightPattern: &structs.FlightPattern{
							MarketingCarrier:      30,
							MarketingFlightNumber: "272",
						},
					},
				},
			},
			{
				// Non-mergeable flight (arrival station is not the same),
				// departs at the same time, breaks the sequence of mergeable flights
				FlightData: &flightdata.FlightData{
					FlightDepartureDate: 20200311,
					FlightDataBase: &flightdata.FlightDataBase{
						FlightBase: structs.FlightBase{
							DepartureTimeScheduled: 1400,
							DepartureStation:       123456,
							ArrivalStation:         555555,
						},
						FlightPattern: &structs.FlightPattern{
							MarketingCarrier:      26,
							MarketingFlightNumber: "123",
						},
					},
				},
			},
			{
				// Should merge with the 30/172 flight two positions above
				FlightData: &flightdata.FlightData{
					FlightDepartureDate: 20200311,
					FlightDataBase: &flightdata.FlightDataBase{
						FlightBase: structs.FlightBase{
							DepartureTimeScheduled: 1400,
							DepartureStation:       123456,
							ArrivalStation:         654321,
						},
						FlightPattern: &structs.FlightPattern{
							MarketingCarrier:      26,
							MarketingFlightNumber: "123",
						},
					},
				},
			},
		},
	)
	expected := []FlightOnDateWithCodeshares{
		{
			FlightData: &flightdata.FlightData{
				FlightDepartureDate: 20200310,
				FlightDataBase: &flightdata.FlightDataBase{
					FlightBase: structs.FlightBase{
						DepartureTimeScheduled: 1400,
						DepartureStation:       123456,
						ArrivalStation:         654321,
					},
					FlightPattern: &structs.FlightPattern{
						MarketingCarrier:      26,
						MarketingFlightNumber: "123",
					},
				},
			},
			Codeshares: []*structs.FlightPattern{
				&structs.FlightPattern{
					MarketingCarrier:      30,
					MarketingFlightNumber: "272",
				},
			},
		},
		{
			FlightData: &flightdata.FlightData{
				FlightDepartureDate: 20200311,
				FlightDataBase: &flightdata.FlightDataBase{
					FlightBase: structs.FlightBase{
						DepartureTimeScheduled: 1400,
						DepartureStation:       123456,
						ArrivalStation:         654321,
					},
					FlightPattern: &structs.FlightPattern{
						MarketingCarrier:      26,
						MarketingFlightNumber: "123",
					},
				},
			},
			Codeshares: []*structs.FlightPattern{
				&structs.FlightPattern{
					MarketingCarrier:      30,
					MarketingFlightNumber: "272",
				},
			},
		},
		{
			FlightData: &flightdata.FlightData{
				FlightDepartureDate: 20200311,
				FlightDataBase: &flightdata.FlightDataBase{
					FlightBase: structs.FlightBase{
						DepartureTimeScheduled: 1400,
						DepartureStation:       123456,
						ArrivalStation:         555555,
					},
					FlightPattern: &structs.FlightPattern{
						MarketingCarrier:      26,
						MarketingFlightNumber: "123",
					},
				},
			},
		},
	}

	assert.Equal(t, expected, got, "flight merge results are not the ones we expected")
}

func TestTerminalFilter_NonEmptyValue(t *testing.T) {
	flight := FlightOnDateWithCodeshares{
		FlightData: flightdata.NewFlightData(
			structs.FlightBase{
				DepartureTerminal: "Y",
				ArrivalTerminal:   "Z",
			},
			nil,
			&structs.FlightStatus{
				DepartureTerminal: "A",
				ArrivalTerminal:   "B",
			},
			20200101,
			nil,
		),
	}
	filter1 := TerminalFilter(dir.DEPARTURE, "Y")
	filter2 := TerminalFilter(dir.DEPARTURE, "A")
	filter3 := TerminalFilter(dir.ARRIVAL, "Z")
	filter4 := TerminalFilter(dir.ARRIVAL, "B")
	filter5 := TerminalFilter(dir.DEPARTURE, "") // matches everything

	// status terminal has a priority
	assert.False(t, filter1(&flight))
	assert.True(t, filter2(&flight))
	assert.False(t, filter3(&flight))
	assert.True(t, filter4(&flight))
	assert.True(t, filter5(&flight))

	// try base terminal if the status terminal is empty
	flight.FlightDataBase.FlightStatus.DepartureTerminal = ""
	flight.FlightDataBase.FlightStatus.ArrivalTerminal = ""
	assert.True(t, filter1(&flight))
	assert.False(t, filter2(&flight))
	assert.True(t, filter3(&flight))
	assert.False(t, filter4(&flight))
	assert.True(t, filter5(&flight))

	flight.FlightDataBase.FlightStatus = nil
	assert.True(t, filter1(&flight))
	assert.False(t, filter2(&flight))
	assert.True(t, filter3(&flight))
	assert.False(t, filter4(&flight))
	assert.True(t, filter5(&flight))

	// empty terminal does not match anything but empty string
	flight.FlightDataBase.FlightBase.DepartureTerminal = ""
	flight.FlightDataBase.FlightBase.ArrivalTerminal = ""
	assert.False(t, filter1(&flight))
	assert.False(t, filter2(&flight))
	assert.False(t, filter3(&flight))
	assert.False(t, filter4(&flight))
	assert.True(t, filter5(&flight))
}

func TestDateFilter_ScheduledOrActual(t *testing.T) {
	flight := FlightOnDateWithCodeshares{
		FlightData: flightdata.NewFlightDataForTests(
			time.Date(2021, 3, 14, 23, 00, 00, 0, time.UTC), // departureTimeScheduled
			time.Date(2021, 3, 15, 01, 00, 00, 0, time.UTC), // actualDepartureTime
			time.Date(2021, 3, 15, 23, 00, 00, 0, time.UTC), // arrivalTimeScheduled
			time.Date(2021, 3, 16, 01, 00, 00, 0, time.UTC), // actualArrivalTime
		),
	}

	filter := DateTimeFilter(
		dir.DEPARTURE,
		time.Date(2021, 3, 14, 22, 00, 00, 0, time.UTC),
		time.Date(2021, 3, 14, 23, 59, 00, 0, time.UTC),
	)
	assert.Equal(t, FitsByScheduledTime, filter(&flight))

	filter = DateTimeFilter(
		dir.DEPARTURE,
		time.Date(2021, 3, 15, 00, 30, 00, 0, time.UTC),
		time.Date(2021, 3, 15, 01, 30, 00, 0, time.UTC),
	)
	assert.Equal(t, FitsByActualTime, filter(&flight))

	filter = DateTimeFilter(
		dir.ARRIVAL,
		time.Date(2021, 3, 15, 22, 30, 00, 0, time.UTC),
		time.Date(2021, 3, 15, 23, 30, 00, 0, time.UTC),
	)
	assert.Equal(t, FitsByScheduledTime, filter(&flight))

	filter = DateTimeFilter(
		dir.ARRIVAL,
		time.Date(2021, 3, 16, 00, 30, 00, 0, time.UTC),
		time.Date(2021, 3, 16, 01, 30, 00, 0, time.UTC),
	)
	assert.Equal(t, FitsByActualTime, filter(&flight))
}
