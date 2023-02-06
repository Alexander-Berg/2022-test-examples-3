package storage

import (
	"fmt"
	"net/http"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	dto "a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/DTO"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/flight"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/flightstatus"
	storageCache "a.yandex-team.ru/travel/avia/shared_flights/api/internal/storage"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/utils"
	"a.yandex-team.ru/travel/avia/shared_flights/api/pkg/structs"
	"a.yandex-team.ru/travel/proto/dicts/rasp"
	"a.yandex-team.ru/travel/proto/shared_flights/snapshots"
)

var dontShowBanned = false
var nationalVersion = ""

func TestGetFlight_ValidateParams(t *testing.T) {
	service := createTestFlightStorage()
	dateNow := time.Date(2019, 12, 1, 19, 0, 0, 0, time.UTC)

	// invalid date
	_, err := service.GetFlight("26", "1", "20191496", nil, nationalVersion, dontShowBanned, dateNow, false, false)
	assert.IsType(t, &utils.ErrorWithHTTPCode{}, err)
	assert.Equal(t, err.(*utils.ErrorWithHTTPCode).HTTPCode, http.StatusBadRequest)
	assert.Contains(t, fmt.Sprintf("%v", err), "departure date 20191496")

	// non-existing carrier
	_, err = service.GetFlight("11", "1", "20191205", nil, nationalVersion, dontShowBanned, dateNow, false, false)
	assert.IsType(t, &utils.ErrorWithHTTPCode{}, err)
	assert.Equal(t, err.(*utils.ErrorWithHTTPCode).HTTPCode, http.StatusNotFound)
	assert.Contains(t, fmt.Sprintf("%v", err), "non-existing carrier 11")

	// non-existing carrier code
	_, err = service.GetFlight("AB", "1", "20191205", nil, nationalVersion, dontShowBanned, dateNow, false, false)
	assert.IsType(t, &utils.ErrorWithHTTPCode{}, err)
	assert.Equal(t, err.(*utils.ErrorWithHTTPCode).HTTPCode, http.StatusNotFound)
	assert.Contains(t, fmt.Sprintf("%v", err), "unknown carrier AB")

	// flight number too long
	_, err = service.GetFlight("SU", "123456", "20191205", nil, nationalVersion, dontShowBanned, dateNow, false, false)
	assert.IsType(t, &utils.ErrorWithHTTPCode{}, err)
	assert.Equal(t, err.(*utils.ErrorWithHTTPCode).HTTPCode, http.StatusBadRequest)
	assert.Contains(t, fmt.Sprintf("%v", err), "(too long) flight number 123456")

	// no error for an existing flight
	_, err = service.GetFlight("SU", "1", "2019-12-05", nil, nationalVersion, dontShowBanned, dateNow, false, false)
	assert.NoError(t, err)
}

func TestGetFlight_RetrieveFlights(t *testing.T) {
	service := createTestFlightStorage()

	// get operating flight by carrier ID
	flightData, err := service.GetFlightData(
		flight.NewCarrierParamByText("26"), "1", "2019-12-05", nil, nationalVersion, dontShowBanned, false, false)
	assert.NoError(t, err)
	assert.Equal(t, 1, len(flightData))
	assert.Equal(t, int32(1), flightData[0].FlightPattern.ID)
	assert.Equal(t, int32(1001), flightData[0].FlightBase.ID)
	assert.Nil(t, flightData[0].FlightStatus)

	// get same flight by carrier IATA
	flightData, err = service.GetFlightData(
		flight.NewCarrierParamByText("SU"), "1", "2019-12-05", nil, nationalVersion, dontShowBanned, false, false)
	assert.NoError(t, err)
	assert.Equal(t, 1, len(flightData))
	assert.Equal(t, int32(1), flightData[0].FlightPattern.ID)
	assert.Equal(t, int32(1001), flightData[0].FlightBase.ID)
	assert.Nil(t, flightData[0].FlightStatus)

	// get marketing flight pattern and operating flight base by carrier ID
	flightData, err = service.GetFlightData(
		flight.NewCarrierParamByText("14"), "147", "2019-12-05", nil, nationalVersion, dontShowBanned, false, false)
	assert.NoError(t, err)
	assert.Equal(t, 1, len(flightData))
	assert.Equal(t, int32(2), flightData[0].FlightPattern.ID)
	assert.Equal(t, int32(1001), flightData[0].FlightBase.ID)
	assert.Nil(t, flightData[0].FlightStatus)

	// get marketing flight pattern and operating flight base by carrier IATA
	flightData, err = service.GetFlightData(
		flight.NewCarrierParamByText("B2"), "147", "2019-12-05", nil, nationalVersion, dontShowBanned, false, false)
	assert.NoError(t, err)
	assert.Equal(t, 1, len(flightData))
	assert.Equal(t, int32(2), flightData[0].FlightPattern.ID)
	assert.Equal(t, int32(1001), flightData[0].FlightBase.ID)
	assert.Nil(t, flightData[0].FlightStatus)

	// get flight with status
	flightData, err = service.GetFlightData(
		flight.NewCarrierParamByText("26"), "1", "2019-12-14", nil, nationalVersion, dontShowBanned, false, false)
	assert.NoError(t, err)
	assert.Equal(t, 1, len(flightData))
	assert.Equal(t, int32(1), flightData[0].FlightPattern.ID)
	assert.Equal(t, int32(1001), flightData[0].FlightBase.ID)
	assert.NotNil(t, flightData[0].FlightStatus)
	assert.Equal(t, "05:20", flightData[0].FlightStatus.DepartureTimeActual)

	// get multi-leg flight
	flightData, err = service.GetFlightData(
		flight.NewCarrierParamByText("26"), "2", "2019-12-05", nil, nationalVersion, dontShowBanned, false, false)
	assert.NoError(t, err)
	assert.Equal(t, 2, len(flightData))

	assert.Equal(t, int32(21), flightData[0].FlightPattern.ID)
	assert.Equal(t, int32(1002), flightData[0].FlightBase.ID)
	assert.Nil(t, flightData[0].FlightStatus)

	assert.Equal(t, int32(22), flightData[1].FlightPattern.ID)
	assert.Equal(t, int32(1003), flightData[1].FlightBase.ID)
	assert.Nil(t, flightData[1].FlightStatus)
}

func TestGetFlight_TestSingleLegConversionToFlightSegments(t *testing.T) {
	service := createTestFlightStorage()

	// it's easier to get flight data from test storage than to specify one;
	// and then we verify that we got what we expected to get
	flightData, err := service.GetFlightData(
		flight.NewCarrierParamByText("26"), "1", "2019-12-05", nil, nationalVersion, dontShowBanned, false, false)
	assert.NoError(t, err)
	assert.Equal(t, 1, len(flightData))
	assert.Equal(t, int32(1), flightData[0].FlightPattern.ID)
	assert.Equal(t, int32(1001), flightData[0].FlightBase.ID)
	assert.Nil(t, flightData[0].FlightStatus)

	dateNow := time.Date(2019, 12, 3, 19, 0, 0, 0, time.UTC)
	flightSegment, err := flight.Convert(flightData, dateNow, "")
	assert.NoError(t, err)
	assert.Equal(t, flight.FlightSegment{
		CompanyIata:       "SU",
		CompanyRaspID:     26,
		Number:            "1",
		Title:             "SU 1",
		AirportFromIata:   "SVX",
		AirportFromRaspID: 100,
		DepartureDay:      "2019-12-05",
		DepartureTime:     "05:00:00",
		DepartureTzName:   "Asia/Yekaterinburg",
		DepartureUTC:      "2019-12-05 00:00:00",
		AirportToIata:     "JFK",
		AirportToRaspID:   101,
		ArrivalDay:        "2019-12-05",
		ArrivalTime:       "14:00:00",
		ArrivalTzName:     "America/New_York",
		ArrivalUTC:        "2019-12-05 19:00:00",
		ArrivalTerminal:   "B",
		DepartureTerminal: "A",
		Status: flightstatus.FlightStatus{
			Status:          "unknown",
			DepartureDT:     "",
			ArrivalDT:       "",
			DepartureStatus: "unknown",
			ArrivalStatus:   "unknown",
			ArrivalGate:     "",
			DepartureGate:   "",
			DepartureSource: "0",
			ArrivalSource:   "0",
		},
		Segments:         []*flight.FlightSegment{},
		FlightCodeshares: []dto.TitledFlight{},
	}, *flightSegment)
}

func TestGetFlight_TestCodesharesForOperating(t *testing.T) {
	service := createTestFlightStorage()

	// Last parameter is "true" to include codeshares into the output
	flightData, err := service.GetFlightData(
		flight.NewCarrierParamByText("26"), "1", "2019-12-05", nil, nationalVersion, dontShowBanned, false, true)
	assert.NoError(t, err)
	assert.Equal(t, 1, len(flightData))
	assert.Equal(t, int32(1), flightData[0].FlightPattern.ID)
	assert.Equal(t, int32(1001), flightData[0].FlightBase.ID)
	assert.Nil(t, flightData[0].FlightStatus)

	dateNow := time.Date(2019, 12, 3, 19, 0, 0, 0, time.UTC)
	flightSegment, err := flight.Convert(flightData, dateNow, "")
	assert.NoError(t, err)
	assert.Equal(t, flight.FlightSegment{
		CompanyIata:       "SU",
		CompanyRaspID:     26,
		Number:            "1",
		Title:             "SU 1",
		AirportFromIata:   "SVX",
		AirportFromRaspID: 100,
		DepartureDay:      "2019-12-05",
		DepartureTime:     "05:00:00",
		DepartureTzName:   "Asia/Yekaterinburg",
		DepartureUTC:      "2019-12-05 00:00:00",
		AirportToIata:     "JFK",
		AirportToRaspID:   101,
		ArrivalDay:        "2019-12-05",
		ArrivalTime:       "14:00:00",
		ArrivalTzName:     "America/New_York",
		ArrivalUTC:        "2019-12-05 19:00:00",
		ArrivalTerminal:   "B",
		DepartureTerminal: "A",
		Status: flightstatus.FlightStatus{
			Status:          "unknown",
			DepartureDT:     "",
			ArrivalDT:       "",
			DepartureStatus: "unknown",
			ArrivalStatus:   "unknown",
			ArrivalGate:     "",
			DepartureGate:   "",
			DepartureSource: "0",
			ArrivalSource:   "0",
		},
		Segments: []*flight.FlightSegment{},
		FlightCodeshares: []dto.TitledFlight{
			{
				FlightID: dto.FlightID{
					AirlineID: 14,
					Number:    "147",
				},
				Title: "B2 147",
			},
			{
				FlightID: dto.FlightID{
					AirlineID: 14,
					Number:    "254",
				},
				Title: "B2 254",
			},
		},
	}, *flightSegment)
}

func TestGetFlight_TestCodesharesForNonOperating(t *testing.T) {
	service := createTestFlightStorage()

	// Last parameter is "true" to include codeshares into the output
	flightData, err := service.GetFlightData(
		flight.NewCarrierParamByText("14"), "147", "2019-12-05", nil, nationalVersion, dontShowBanned, false, true)
	assert.NoError(t, err)
	assert.Equal(t, 1, len(flightData))
	assert.Equal(t, int32(2), flightData[0].FlightPattern.ID)
	assert.Equal(t, int32(1001), flightData[0].FlightBase.ID)
	assert.Nil(t, flightData[0].FlightStatus)

	dateNow := time.Date(2019, 12, 3, 19, 0, 0, 0, time.UTC)
	flightSegment, err := flight.Convert(flightData, dateNow, "")
	assert.NoError(t, err)
	assert.Equal(t, flight.FlightSegment{
		CompanyIata:       "B2",
		CompanyRaspID:     14,
		Number:            "147",
		Title:             "B2 147",
		AirportFromIata:   "SVX",
		AirportFromRaspID: 100,
		DepartureDay:      "2019-12-05",
		DepartureTime:     "05:00:00",
		DepartureTzName:   "Asia/Yekaterinburg",
		DepartureUTC:      "2019-12-05 00:00:00",
		AirportToIata:     "JFK",
		AirportToRaspID:   101,
		ArrivalDay:        "2019-12-05",
		ArrivalTime:       "14:00:00",
		ArrivalTzName:     "America/New_York",
		ArrivalUTC:        "2019-12-05 19:00:00",
		ArrivalTerminal:   "B",
		DepartureTerminal: "A",
		Status: flightstatus.FlightStatus{
			Status:          "unknown",
			DepartureDT:     "",
			ArrivalDT:       "",
			DepartureStatus: "unknown",
			ArrivalStatus:   "unknown",
			ArrivalGate:     "",
			DepartureGate:   "",
			DepartureSource: "0",
			ArrivalSource:   "0",
		},
		Segments: []*flight.FlightSegment{},
		// the requested flight itself  is not included in codeshares
		FlightCodeshares: []dto.TitledFlight{
			{
				FlightID: dto.FlightID{
					AirlineID: 14,
					Number:    "254",
				},
				Title: "B2 254",
			},
		},
		Operating: &dto.TitledFlight{
			FlightID: dto.FlightID{
				AirlineID: 26,
				Number:    "1",
			},
			Title: "SU 1",
		},
	}, *flightSegment)
}

func TestGetFlight_TestMultiLegConversionToFlightSegments(t *testing.T) {
	service := createTestFlightStorage()
	// it's easier to get flight data from test storage than to specify one;
	// and then we verify that we got what we expected to get
	flightData, err := service.GetFlightData(
		flight.NewCarrierParamByText("26"), "2", "2019-12-05", nil, nationalVersion, dontShowBanned, false, false)
	assert.NoError(t, err)
	assert.Equal(t, 2, len(flightData))
	assert.Equal(t, int32(21), flightData[0].FlightPattern.ID)
	assert.Equal(t, int32(1002), flightData[0].FlightBase.ID)
	assert.Nil(t, flightData[0].FlightStatus)

	assert.Equal(t, int32(22), flightData[1].FlightPattern.ID)
	assert.Equal(t, int32(1003), flightData[1].FlightBase.ID)
	assert.Nil(t, flightData[1].FlightStatus)

	dateNow := time.Date(2019, 12, 3, 19, 0, 0, 0, time.UTC)
	flightSegment, err := flight.Convert(flightData, dateNow, "")
	assert.NoError(t, err)
	assert.Equal(t, flight.FlightSegment{
		CompanyIata:       "SU",
		CompanyRaspID:     26,
		Number:            "2",
		Title:             "SU 2",
		AirportFromIata:   "SVX",
		AirportFromRaspID: 100,
		DepartureDay:      "2019-12-05",
		DepartureTime:     "05:00:00",
		DepartureTzName:   "Asia/Yekaterinburg",
		DepartureUTC:      "2019-12-05 00:00:00",
		AirportToIata:     "JFK",
		AirportToRaspID:   101,
		ArrivalDay:        "2019-12-05",
		ArrivalTime:       "17:00:00",
		ArrivalTzName:     "America/New_York",
		ArrivalUTC:        "2019-12-05 22:00:00",
		ArrivalTerminal:   "B",
		DepartureTerminal: "A",
		Status: flightstatus.FlightStatus{
			Status:          "unknown",
			DepartureStatus: "unknown",
			ArrivalStatus:   "unknown",
			DepartureSource: "0",
			ArrivalSource:   "0",
		},
		FlightCodeshares: []dto.TitledFlight{},
		Segments: []*flight.FlightSegment{
			{
				CompanyIata:       "SU",
				CompanyRaspID:     26,
				Number:            "2",
				Title:             "SU 2",
				AirportFromIata:   "SVX",
				AirportFromRaspID: 100,
				DepartureDay:      "2019-12-05",
				DepartureTime:     "05:00:00",
				DepartureTzName:   "Asia/Yekaterinburg",
				DepartureUTC:      "2019-12-05 00:00:00",
				DepartureTerminal: "A",
				AirportToIata:     "PEE",
				AirportToRaspID:   102,
				ArrivalDay:        "2019-12-05",
				ArrivalTime:       "11:00:00",
				ArrivalTzName:     "Asia/Yekaterinburg",
				ArrivalUTC:        "2019-12-05 06:00:00",
				ArrivalTerminal:   "B",
				Segments:          []*flight.FlightSegment{},
				Status: flightstatus.FlightStatus{
					DepartureStatus: "unknown",
					ArrivalStatus:   "unknown",
					Status:          "unknown",
				},
			},
			{
				CompanyIata:       "SU",
				CompanyRaspID:     26,
				Number:            "2",
				Title:             "SU 2",
				AirportFromIata:   "PEE",
				AirportFromRaspID: 102,
				DepartureDay:      "2019-12-05",
				DepartureTime:     "13:00:00",
				DepartureTzName:   "Asia/Yekaterinburg",
				DepartureUTC:      "2019-12-05 08:00:00",
				DepartureTerminal: "A",
				AirportToIata:     "JFK",
				AirportToRaspID:   101,
				ArrivalDay:        "2019-12-05",
				ArrivalTime:       "17:00:00",
				ArrivalTzName:     "America/New_York",
				ArrivalUTC:        "2019-12-05 22:00:00",
				ArrivalTerminal:   "B",
				Segments:          []*flight.FlightSegment{},
				Status: flightstatus.FlightStatus{
					DepartureStatus: "unknown",
					ArrivalStatus:   "unknown",
					Status:          "unknown",
				},
			},
		},
	}, *flightSegment)
}

func TestGetFlight_TestMultiLegWithDepartureDayShift(t *testing.T) {
	service := createTestFlightStorage()
	// it's easier to get flight data from test storage than to specify one;
	// and then we verify that we got what we expected to get
	flightData, err := service.GetFlightData(
		flight.NewCarrierParamByText("26"), "2", "2019-12-16", nil, nationalVersion, dontShowBanned, false, false)
	assert.Error(t, err)
	assert.Equal(t, 0, len(flightData))

	flightData, err = service.GetFlightData(
		flight.NewCarrierParamByText("26"), "2", "2019-12-15", nil, nationalVersion, dontShowBanned, false, false)
	assert.NoError(t, err)
	assert.Equal(t, 2, len(flightData))
	assert.Equal(t, int32(31), flightData[0].FlightPattern.ID)
	assert.Equal(t, int32(2002), flightData[0].FlightBase.ID)
	assert.Nil(t, flightData[0].FlightStatus)

	assert.Equal(t, int32(32), flightData[1].FlightPattern.ID)
	assert.Equal(t, int32(2003), flightData[1].FlightBase.ID)
	assert.Nil(t, flightData[1].FlightStatus)
}

func createTestFlightStorage() *ServiceInstance {
	service := NewStorageService(storageCache.NewStorageWithStartDate("2019-11-01")).Instance()

	// single leg flight
	service.Storage().Timezones().PutTimezone(&rasp.TTimeZone{Id: 200, Code: "Asia/Yekaterinburg"})
	service.Storage().Timezones().PutTimezone(&rasp.TTimeZone{Id: 201, Code: "America/New_York"})
	service.Storage().PutStation(&snapshots.TStationWithCodes{
		Station: &rasp.TStation{
			Id:           100,
			TimeZoneId:   200,
			TimeZoneCode: "Asia/Yekaterinburg",
		},
		IataCode: "SVX",
	})
	service.Storage().PutStation(&snapshots.TStationWithCodes{
		Station: &rasp.TStation{
			Id:           101,
			TimeZoneId:   201,
			TimeZoneCode: "America/New_York",
		},
		IataCode: "JFK",
	})
	service.Storage().PutFlightBase(structs.FlightBase{
		ID:                     1001,
		OperatingCarrier:       26,
		OperatingCarrierCode:   "SU",
		OperatingFlightNumber:  "1",
		DepartureStation:       100,
		DepartureStationCode:   "SVX",
		DepartureTimeScheduled: 500,
		DepartureTerminal:      "A",
		ArrivalStation:         101,
		ArrivalStationCode:     "JFK",
		ArrivalTimeScheduled:   1400,
		ArrivalTerminal:        "B",
		LegNumber:              1,
	})
	service.Storage().PutFlightPattern(structs.FlightPattern{
		ID:                    1,
		FlightBaseID:          1001,
		OperatingFromDate:     "20191201",
		OperatingUntilDate:    "20480201",
		OperatingOnDays:       1234567,
		MarketingCarrier:      26,
		MarketingCarrierCode:  "SU",
		MarketingFlightNumber: "1",
		IsCodeshare:           false,
	})
	service.Storage().PutFlightPattern(structs.FlightPattern{
		ID:                    2,
		FlightBaseID:          1001,
		OperatingFromDate:     "20191201",
		OperatingUntilDate:    "20480201",
		OperatingOnDays:       1234567,
		MarketingCarrier:      14,
		MarketingCarrierCode:  "B2",
		MarketingFlightNumber: "147",
		IsCodeshare:           true,
	})
	service.Storage().PutFlightPattern(structs.FlightPattern{
		ID:                    3,
		FlightBaseID:          1001,
		OperatingFromDate:     "20191201",
		OperatingUntilDate:    "20480201",
		OperatingOnDays:       1234567,
		MarketingCarrier:      14,
		MarketingCarrierCode:  "B2",
		MarketingFlightNumber: "254",
		IsCodeshare:           true,
	})
	service.Storage().StatusStorage().PutFlightStatus(structs.FlightStatus{
		AirlineID:           26,
		FlightNumber:        "1",
		LegNumber:           1,
		FlightDate:          "2019-12-14",
		StatusSourceID:      3,
		CreatedAtUtc:        "2021-01-20",
		UpdatedAtUtc:        "2021-01-01",
		DepartureTimeActual: "05:20",
		DepartureStatus:     "on-time",
		DepartureGate:       "111",
		DepartureTerminal:   "C",
	})

	// multi-leg flight
	service.Storage().PutStation(&snapshots.TStationWithCodes{
		Station: &rasp.TStation{
			Id:           102,
			TimeZoneId:   200,
			TimeZoneCode: "Asia/Yekaterinburg",
		},
		IataCode: "PEE",
	})
	service.Storage().PutFlightBase(structs.FlightBase{
		ID:                     1002,
		OperatingCarrier:       26,
		OperatingCarrierCode:   "SU",
		OperatingFlightNumber:  "2",
		LegNumber:              1,
		DepartureStation:       100,
		DepartureStationCode:   "SVX",
		DepartureTimeScheduled: 500,
		DepartureTerminal:      "A",
		ArrivalStation:         102,
		ArrivalStationCode:     "PEE",
		ArrivalTimeScheduled:   1100,
		ArrivalTerminal:        "B",
	})
	service.Storage().PutFlightBase(structs.FlightBase{
		ID:                     1003,
		OperatingCarrier:       26,
		OperatingCarrierCode:   "SU",
		OperatingFlightNumber:  "2",
		LegNumber:              2,
		DepartureStation:       102,
		DepartureStationCode:   "PEE",
		DepartureTimeScheduled: 1300,
		DepartureTerminal:      "A",
		ArrivalStation:         101,
		ArrivalStationCode:     "JFK",
		ArrivalTimeScheduled:   1700,
		ArrivalTerminal:        "B",
	})
	service.Storage().PutFlightPattern(structs.FlightPattern{
		ID:                    21,
		FlightBaseID:          1002,
		LegNumber:             1,
		OperatingFromDate:     "20191201",
		OperatingUntilDate:    "20191209",
		OperatingOnDays:       1234567,
		MarketingCarrier:      26,
		MarketingCarrierCode:  "SU",
		MarketingFlightNumber: "2",
		IsCodeshare:           false,
	})
	service.Storage().PutFlightPattern(structs.FlightPattern{
		ID:                    22,
		FlightBaseID:          1003,
		LegNumber:             2,
		OperatingFromDate:     "20191201",
		OperatingUntilDate:    "20191209",
		OperatingOnDays:       1234567,
		MarketingCarrier:      26,
		MarketingCarrierCode:  "SU",
		MarketingFlightNumber: "2",
		IsCodeshare:           false,
	})

	// multi-leg flight with midnight between legs
	service.Storage().PutFlightBase(structs.FlightBase{
		ID:                     2002,
		OperatingCarrier:       26,
		OperatingCarrierCode:   "SU",
		OperatingFlightNumber:  "2",
		LegNumber:              1,
		DepartureStation:       100,
		DepartureStationCode:   "SVX",
		DepartureTimeScheduled: 1500,
		DepartureTerminal:      "A",
		ArrivalStation:         102,
		ArrivalStationCode:     "PEE",
		ArrivalTimeScheduled:   1800,
		ArrivalTerminal:        "B",
	})
	service.Storage().PutFlightBase(structs.FlightBase{
		ID:                     2003,
		OperatingCarrier:       26,
		OperatingCarrierCode:   "SU",
		OperatingFlightNumber:  "2",
		LegNumber:              2,
		DepartureStation:       102,
		DepartureStationCode:   "PEE",
		DepartureTimeScheduled: 100,
		DepartureTerminal:      "A",
		ArrivalStation:         101,
		ArrivalStationCode:     "JFK",
		ArrivalTimeScheduled:   700,
		ArrivalTerminal:        "B",
	})
	service.Storage().PutFlightPattern(structs.FlightPattern{
		ID:                    31,
		FlightBaseID:          2002,
		LegNumber:             1,
		OperatingFromDate:     "20191215",
		OperatingUntilDate:    "20191230",
		OperatingOnDays:       27,
		MarketingCarrier:      26,
		MarketingCarrierCode:  "SU",
		MarketingFlightNumber: "2",
		IsCodeshare:           false,
	})
	service.Storage().PutFlightPattern(structs.FlightPattern{
		ID:                    32,
		FlightBaseID:          2003,
		LegNumber:             2,
		OperatingFromDate:     "20191216",
		OperatingUntilDate:    "20191231",
		OperatingOnDays:       13,
		MarketingCarrier:      26,
		MarketingCarrierCode:  "SU",
		MarketingFlightNumber: "2",
		DepartureDayShift:     1,
		IsCodeshare:           false,
	})

	return service
}
