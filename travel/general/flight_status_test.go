package loadsnapshot

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"

	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/storage/flight"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/utils"
	"a.yandex-team.ru/travel/avia/shared_flights/api/pkg/structs"
	"a.yandex-team.ru/travel/proto/shared_flights/snapshots"
)

type MockedStatusLoaderDeps struct {
	mock.Mock
	statuses []structs.FlightStatus
}

func (m *MockedStatusLoaderDeps) PutFlightStatus(flightStatus structs.FlightStatus) structs.FlightStatus {
	m.Called(flightStatus)
	m.statuses = append(m.statuses, flightStatus)
	return flightStatus
}

func (m *MockedStatusLoaderDeps) IsTrusted(stationID int64, statusSourceID int64) bool {
	args := m.Called(stationID, statusSourceID)
	return args.Bool(0)
}

func (m *MockedStatusLoaderDeps) GetCarriersByCode(code string) []int32 {
	args := m.Called(code)
	return args.Get(0).([]int32)
}

func (m *MockedStatusLoaderDeps) FindLegInfo(carrierID int32, flightNumber, flightDate string, departureStation, arrivalStation int64, useArrivalShift bool) (legInfo flight.FlightPatternAndBase, err error) {
	args := m.Called(carrierID, flightNumber, flightDate, departureStation, arrivalStation, useArrivalShift)
	return args.Get(0).(flight.FlightPatternAndBase), args.Error(1)
}

func (m *MockedStatusLoaderDeps) GetCarrierByCodeAndFlightNumber(carrierCode, flightNumber string) int32 {
	args := m.Called(carrierCode, flightNumber)
	return int32(args.Int(0))
}

func (m *MockedStatusLoaderDeps) GetTimeZoneByStationID(stationID int64) *time.Location {
	args := m.Called(stationID)
	return args.Get(0).(*time.Location)
}

func (m *MockedStatusLoaderDeps) ByID(id int64) (*snapshots.TStationWithCodes, bool) {
	args := m.Called(id)
	return args.Get(0).(*snapshots.TStationWithCodes), args.Bool(1)
}

func Test_flightStatusLoader_LoadStatusThatMatchesFlightButNotRoutePoints(t *testing.T) {
	Mock := new(MockedStatusLoaderDeps)
	statusLoader := flightStatusLoader{
		flightStatusLoaderConfig: flightStatusLoaderConfig{
			startScheduleDate:          "2020-01-01",
			stations:                   Mock,
			tzutil:                     Mock,
			carrierService:             Mock,
			flightStorage:              Mock,
			carrierStorage:             Mock,
			stationStatusSourceStorage: Mock,
			statusStorage:              Mock,
		},
		flightStatusLoaderStats: flightStatusLoaderStats{
			flightStatusesSkipped:     0,
			flightStatusesLoadedCount: 0,
		},
		flightStatusLoaderArtifacts: flightStatusLoaderArtifacts{
			flightStatusesToProcess: make(map[string]FlightStatusInfo),
			dopFlightsToCreate:      make(map[string]flight.FlightPatternAndBase),
			dopFlightsCounts:        make(utils.StringListMap),
			errorCounters:           make(map[string]int),
		},
		nextDopFlightID: 100,
	}
	Mock.On("GetCarrierByCodeAndFlightNumber", "U6", "300").Return(30).Twice()
	Mock.On("PutFlightStatus", mock.AnythingOfType("structs.FlightStatus")).Return()
	Mock.On(
		"FindLegInfo",
		int32(30), "300", "2020-01-01",
		mock.AnythingOfType("int64"), int64(0), mock.AnythingOfType("bool"),
	).Return(
		flight.FlightPatternAndBase{
			FlightBase: structs.FlightBase{
				ID:                     1010,
				OperatingCarrier:       30,
				OperatingCarrierCode:   "U6",
				OperatingFlightNumber:  "300",
				LegNumber:              1,
				DepartureStation:       100,
				DepartureStationCode:   "BSQ",
				DepartureTimeScheduled: 1200,
				ArrivalStation:         200,
				ArrivalStationCode:     "SVX",
				ArrivalTimeScheduled:   1300,
				AircraftTypeID:         0,
			},
			FlightPattern: structs.FlightPattern{
				ID:                    2010,
				FlightBaseID:          1010,
				MarketingCarrier:      30,
				MarketingCarrierCode:  "U6",
				MarketingFlightNumber: "300",
				LegNumber:             1,
				OperatingFromDate:     "2020-01-01",
				OperatingUntilDate:    "2020-01-01",
				OperatingOnDays:       1234567,
			},
		},
		nil,
	).Twice()
	Mock.On("FindLegInfo", int32(30), "300", "2020-01-01", int64(0), int64(300), true).
		Return(flight.FlightPatternAndBase{}, flight.SegmentNotFoundError).Twice()
	Mock.On("IsTrusted", mock.AnythingOfType("int64"), int64(3)).Return(true).Twice()
	Mock.On("GetTimeZoneByStationID", mock.AnythingOfType("int64")).Return(time.UTC).Times(3)
	Mock.On("GetCarriersByCode", "U6").Return([]int32{30}).Once()
	// Load status for valid segment
	statusLoader.LoadStatus(&snapshots.TFlightStatus{
		AirlineId:              30,
		CarrierCode:            "U6",
		FlightNumber:           "300",
		LegNumber:              1,
		FlightDate:             "2020-01-01",
		CreatedAtUtc:           "2020-01-01 12:20:00",
		UpdatedAtUtc:           "2020-01-01 12:22:00",
		DepartureTimeActual:    "2020-01-01 12:23:00",
		DepartureTimeScheduled: "2020-01-01 12:21:00",
		DepartureStatus:        "no status",
		DepartureCreatedAtUtc:  "2020-01-01 12:20:00",
		DepartureReceivedAtUtc: "2020-01-01 12:22:00",
		DepartureUpdatedAtUtc:  "2020-01-01 12:22:00",
		DepartureStation:       100,
		ArrivalStation:         200,
		DepartureSourceId:      3,
		ArrivalSourceId:        3,
	})

	// Load status for invalid segment on same date
	statusLoader.LoadStatus(&snapshots.TFlightStatus{
		AirlineId:            30,
		CarrierCode:          "U6",
		FlightNumber:         "300",
		LegNumber:            0,
		FlightDate:           "2020-01-01",
		CreatedAtUtc:         "2020-01-01 12:20:00",
		UpdatedAtUtc:         "2020-01-01 12:22:00",
		ArrivalTimeActual:    "2020-01-01 12:23:00",
		ArrivalTimeScheduled: "2020-01-01 12:21:00",
		ArrivalStatus:        "cancelled",
		ArrivalCreatedAtUtc:  "2020-01-01 12:20:00",
		ArrivalReceivedAtUtc: "2020-01-01 12:22:00",
		ArrivalUpdatedAtUtc:  "2020-01-01 12:22:00",
		DepartureStation:     100,
		ArrivalStation:       300,
		DepartureSourceId:    3,
		ArrivalSourceId:      3,
	})

	Mock.AssertNumberOfCalls(t, "PutFlightStatus", 1)

}

func Test_flightStatusLoader_LoadArrivalStatus(t *testing.T) {
	Mock := new(MockedStatusLoaderDeps)
	statusLoader := flightStatusLoader{
		flightStatusLoaderConfig: flightStatusLoaderConfig{
			startScheduleDate:          "2020-01-01",
			stations:                   Mock,
			tzutil:                     Mock,
			carrierService:             Mock,
			flightStorage:              Mock,
			carrierStorage:             Mock,
			stationStatusSourceStorage: Mock,
			statusStorage:              Mock,
		},
		flightStatusLoaderStats: flightStatusLoaderStats{
			flightStatusesSkipped:     0,
			flightStatusesLoadedCount: 0,
		},
		flightStatusLoaderArtifacts: flightStatusLoaderArtifacts{
			flightStatusesToProcess: make(map[string]FlightStatusInfo),
			dopFlightsToCreate:      make(map[string]flight.FlightPatternAndBase),
			dopFlightsCounts:        make(utils.StringListMap),
			errorCounters:           make(map[string]int),
		},
		nextDopFlightID: 100,
	}

	Mock.On("GetCarrierByCodeAndFlightNumber", "U6", "24").Return(30)
	Mock.On("FindLegInfo", int32(30), "24", "2020-09-08", int64(0), int64(200), true).Return(
		flight.FlightPatternAndBase{
			FlightBase: structs.FlightBase{
				ID:                     1010,
				OperatingCarrier:       30,
				OperatingCarrierCode:   "U6",
				OperatingFlightNumber:  "24",
				LegNumber:              1,
				DepartureStation:       100,
				DepartureStationCode:   "SIP",
				DepartureTimeScheduled: 2310,
				ArrivalStation:         200,
				ArrivalStationCode:     "SVX",
				ArrivalTimeScheduled:   150,
			},
			FlightPattern: structs.FlightPattern{
				ID:                    2010,
				FlightBaseID:          1010,
				MarketingCarrier:      30,
				MarketingCarrierCode:  "U6",
				MarketingFlightNumber: "24",
				LegNumber:             1,
				OperatingFromDate:     "2020-01-01",
				OperatingUntilDate:    "2021-01-01",
				OperatingOnDays:       1234567,
				ArrivalDayShift:       1,
			},
		},
		nil,
	)
	Mock.On("IsTrusted", int64(100), int64(3)).Return(true)
	Mock.On("IsTrusted", int64(200), int64(3)).Return(true)
	Mock.On("PutFlightStatus", mock.AnythingOfType("structs.FlightStatus")).Return()
	Mock.On("GetTimeZoneByStationID", int64(100)).Return(time.UTC)
	Mock.On("GetTimeZoneByStationID", int64(200)).Return(time.UTC)
	// It's a real flight from database, so I kept all of the irrelevant fields here
	statusLoader.LoadStatus(&snapshots.TFlightStatus{
		AirlineId:                  30,
		CarrierCode:                "U6",
		FlightNumber:               "24",
		LegNumber:                  1,
		FlightDate:                 "2020-09-06",
		CreatedAtUtc:               "2020-09-06 03:01:07",
		UpdatedAtUtc:               "2020-09-08 07:27:08",
		ArrivalStatus:              "arrived",
		ArrivalGate:                "",
		ArrivalTerminal:            "A",
		ArrivalDiverted:            false,
		ArrivalDivertedAirportCode: "",

		ArrivalCreatedAtUtc:  "2020-09-06 03:01:07",
		ArrivalReceivedAtUtc: "2020-09-08 07:27:07",
		ArrivalUpdatedAtUtc:  "2020-09-08 07:27:08",
		BaggageCarousels:     "9",
		ArrivalTimeActual:    "2020-09-08 01:40:00",
		ArrivalTimeScheduled: "2020-09-08 01:40:00",

		DepartureStation:  100,
		ArrivalStation:    200,
		DepartureSourceId: 3,
		ArrivalSourceId:   3,
	})

	Mock.AssertNumberOfCalls(t, "PutFlightStatus", 1)

	expect := assert.New(t)
	expect.Len(Mock.statuses, 1, "Expected to load a single flight status")
	status := Mock.statuses[0]
	expect.Equal("2020-09-07", status.FlightDate,
		"Expected that arrival flight status date is shifted for an overnight leg",
	)
}

func Test_flightStatusLoader_LoadHalfStatusSetsOnlyHalfOfSourceID(t *testing.T) {
	Mock := new(MockedStatusLoaderDeps)
	statusLoader := flightStatusLoader{
		flightStatusLoaderConfig: flightStatusLoaderConfig{
			startScheduleDate:          "2020-01-01",
			stations:                   Mock,
			tzutil:                     Mock,
			carrierService:             Mock,
			flightStorage:              Mock,
			carrierStorage:             Mock,
			stationStatusSourceStorage: Mock,
			statusStorage:              Mock,
		},
		flightStatusLoaderStats: flightStatusLoaderStats{
			flightStatusesSkipped:     0,
			flightStatusesLoadedCount: 0,
		},
		flightStatusLoaderArtifacts: flightStatusLoaderArtifacts{
			flightStatusesToProcess: make(map[string]FlightStatusInfo),
			dopFlightsToCreate:      make(map[string]flight.FlightPatternAndBase),
			dopFlightsCounts:        make(utils.StringListMap),
			errorCounters:           make(map[string]int),
		},
		nextDopFlightID: 100,
	}

	Mock.On("GetCarrierByCodeAndFlightNumber", "U6", "24").Return(30)
	Mock.On("FindLegInfo", int32(30), "24", "2020-09-08", int64(0), int64(200), true).Return(
		flight.FlightPatternAndBase{
			FlightBase: structs.FlightBase{
				ID:                     1010,
				OperatingCarrier:       30,
				OperatingCarrierCode:   "U6",
				OperatingFlightNumber:  "24",
				LegNumber:              1,
				DepartureStation:       100,
				DepartureStationCode:   "SIP",
				DepartureTimeScheduled: 2310,
				ArrivalStation:         200,
				ArrivalStationCode:     "SVX",
				ArrivalTimeScheduled:   150,
			},
			FlightPattern: structs.FlightPattern{
				ID:                    2010,
				FlightBaseID:          1010,
				MarketingCarrier:      30,
				MarketingCarrierCode:  "U6",
				MarketingFlightNumber: "24",
				LegNumber:             1,
				OperatingFromDate:     "2020-01-01",
				OperatingUntilDate:    "2021-01-01",
				OperatingOnDays:       1234567,
				ArrivalDayShift:       1,
			},
		},
		nil,
	)
	Mock.On("IsTrusted", int64(200), int64(3)).Return(true)
	Mock.On("PutFlightStatus", mock.AnythingOfType("structs.FlightStatus")).Return()
	Mock.On("GetTimeZoneByStationID", mock.AnythingOfType("int64")).Return(time.UTC).Times(3)

	statusLoader.LoadStatus(&snapshots.TFlightStatus{
		AirlineId:                  30,
		CarrierCode:                "U6",
		FlightNumber:               "24",
		LegNumber:                  0,
		FlightDate:                 "2020-09-07",
		CreatedAtUtc:               "2020-09-06 03:01:07",
		UpdatedAtUtc:               "2020-09-08 07:27:08",
		ArrivalStatus:              "arrived",
		ArrivalGate:                "",
		ArrivalTerminal:            "A",
		ArrivalDiverted:            false,
		ArrivalDivertedAirportCode: "",

		ArrivalCreatedAtUtc:  "2020-09-06 03:01:07",
		ArrivalReceivedAtUtc: "2020-09-08 07:27:07",
		ArrivalUpdatedAtUtc:  "2020-09-08 07:27:08",
		BaggageCarousels:     "9",
		ArrivalTimeActual:    "2020-09-08 01:40:00",
		ArrivalTimeScheduled: "2020-09-08 01:40:00",

		DepartureStation:  100,
		ArrivalStation:    200,
		DepartureSourceId: 3,
		ArrivalSourceId:   3,
	})
	expect := assert.New(t)
	expect.Len(Mock.statuses, 1, "Expected to load a single flight status")
	status := Mock.statuses[0]
	expect.Equal(int32(0), status.DepartureSourceID, "Expected not to fill departure status source id for arrival status")
	expect.Equal(int32(3), status.ArrivalSourceID, "Expected to fill arrival status source id for arrival status")

}
