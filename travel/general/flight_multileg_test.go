package storage

import (
	"net/http"
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/flight"
	storageCache "a.yandex-team.ru/travel/avia/shared_flights/api/internal/storage"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/utils"
	"a.yandex-team.ru/travel/avia/shared_flights/api/pkg/structs"
	"a.yandex-team.ru/travel/proto/dicts/rasp"
	"a.yandex-team.ru/travel/proto/shared_flights/snapshots"
)

func TestGetFlight_RetrieveMultilegOvernightFlights(t *testing.T) {
	service := createTestFlightStorageWithOvernightMultilegs()
	doNotShowBanned := false
	nationalVersion = ""

	// for Thursday we should get both legs of the flight
	flightData, err := service.GetFlightData(
		flight.NewCarrierParamByText("30"), "365", "2019-12-19", nil, nationalVersion, doNotShowBanned, false, false)
	assert.NoError(t, err)
	assert.Equal(t, 2, len(flightData))

	if len(flightData) > 0 {
		assert.Equal(t, int32(1), flightData[0].FlightPattern.ID)
		assert.Equal(t, int32(1001), flightData[0].FlightBase.ID)
		assert.Nil(t, flightData[0].FlightStatus)
	}

	if len(flightData) > 1 {
		assert.Equal(t, int32(2), flightData[1].FlightPattern.ID)
		assert.Equal(t, int32(1002), flightData[1].FlightBase.ID)
		assert.Nil(t, flightData[1].FlightStatus)
	}

	// for Friday we should get no legs of the flight,
	// since if the first leg departs on Friday, the second one departs on Saturday and there is no Sat schedule for it
	_, err = service.GetFlightData(
		flight.NewCarrierParamByText("30"), "365", "2019-12-20", nil, nationalVersion, doNotShowBanned, false, false)
	assert.Error(t, err)
	if err != nil {
		assert.IsType(t, &utils.ErrorWithHTTPCode{}, err)
		assert.Equal(t, http.StatusNotFound, err.(*utils.ErrorWithHTTPCode).HTTPCode)
	}

	// but if "loose segments" are allowed in search, we should get one
	flightData, err = service.GetFlightData(
		flight.NewCarrierParamByText("30"), "365", "2019-12-20", nil, nationalVersion, doNotShowBanned, true, false)
	assert.NoError(t, err)
	assert.Equal(t, 1, len(flightData))
	if len(flightData) > 0 {
		assert.Equal(t, int32(2), flightData[0].FlightPattern.ID)
		assert.Equal(t, int32(1002), flightData[0].FlightBase.ID)
		assert.Nil(t, flightData[0].FlightStatus)
	}
}

func createTestFlightStorageWithOvernightMultilegs() *ServiceInstance {
	service := NewStorageService(storageCache.NewStorage()).Instance()

	// single leg flight
	service.Storage().Timezones().PutTimezone(&rasp.TTimeZone{Id: 201, Code: "Europe/Moscow"})
	service.Storage().Timezones().PutTimezone(&rasp.TTimeZone{Id: 202, Code: "Asia/Yekaterinburg"})
	service.Storage().Timezones().PutTimezone(&rasp.TTimeZone{Id: 203, Code: "Asia/Irkutsk"})
	service.Storage().PutStation(&snapshots.TStationWithCodes{
		Station: &rasp.TStation{
			Id:           101,
			TimeZoneId:   201,
			TimeZoneCode: "Europe/Moscow",
		},
		IataCode: "LED",
	})
	service.Storage().PutStation(&snapshots.TStationWithCodes{
		Station: &rasp.TStation{
			Id:           102,
			TimeZoneId:   202,
			TimeZoneCode: "Asia/Yekaterinburg",
		},
		IataCode: "SVX",
	})
	service.Storage().PutStation(&snapshots.TStationWithCodes{
		Station: &rasp.TStation{
			Id:           103,
			TimeZoneId:   203,
			TimeZoneCode: "Europe/Moscow",
		},
		IataCode: "IKT",
	})
	service.Storage().PutFlightBase(structs.FlightBase{
		ID:                     1001,
		OperatingCarrier:       30,
		OperatingCarrierCode:   "U6",
		OperatingFlightNumber:  "365",
		DepartureStation:       101,
		DepartureStationCode:   "LED",
		DepartureTimeScheduled: 2100,
		DepartureTerminal:      "A",
		ArrivalStation:         102,
		ArrivalStationCode:     "SVX",
		ArrivalTimeScheduled:   100,
		ArrivalTerminal:        "B",
	})
	service.Storage().PutFlightBase(structs.FlightBase{
		ID:                     1002,
		OperatingCarrier:       30,
		OperatingCarrierCode:   "U6",
		OperatingFlightNumber:  "365",
		DepartureStation:       102,
		DepartureStationCode:   "SVX",
		DepartureTimeScheduled: 300,
		DepartureTerminal:      "B",
		ArrivalStation:         103,
		ArrivalStationCode:     "IKT",
		ArrivalTimeScheduled:   800,
		ArrivalTerminal:        "C",
	})
	service.Storage().PutFlightPattern(structs.FlightPattern{
		ID:                    1,
		FlightBaseID:          1001,
		OperatingFromDate:     "20191201",
		OperatingUntilDate:    "20480201",
		OperatingOnDays:       4,
		MarketingCarrier:      30,
		MarketingCarrierCode:  "U6",
		MarketingFlightNumber: "365",
		LegNumber:             1,
		ArrivalDayShift:       1,
		IsCodeshare:           false,
		FlightDayShift:        0,
	})
	service.Storage().PutFlightPattern(structs.FlightPattern{
		ID:                    2,
		FlightBaseID:          1002,
		OperatingFromDate:     "20191201",
		OperatingUntilDate:    "20480201",
		OperatingOnDays:       5,
		MarketingCarrier:      30,
		MarketingCarrierCode:  "U6",
		MarketingFlightNumber: "365",
		LegNumber:             2,
		IsCodeshare:           false,
		FlightDayShift:        1,
	})

	return service
}
