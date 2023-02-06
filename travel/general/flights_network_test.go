package flightsnetwork

import (
	"sort"
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/flights_network/format"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/storage"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/utils"
	"a.yandex-team.ru/travel/avia/shared_flights/api/pkg/structs"
	"a.yandex-team.ru/travel/proto/dicts/rasp"
	"a.yandex-team.ru/travel/proto/shared_flights/snapshots"
)

func Test_FlightsNetworkService_GetFlightsNetwork(t *testing.T) {
	testStorage := PrepareTestStorage()
	tests := []struct {
		name             string
		carrierCode      string
		expectedResponse format.Response
		expectedErr      int
	}{
		{
			name:        "carrier not found",
			carrierCode: "YY",
			expectedErr: 404,
		},
		{
			name:        "valid flights network",
			carrierCode: "SU",
			expectedResponse: format.Response{
				Carriers: []int32{26},
				Stations: []format.Station{
					{
						AirportID:    100,
						SettlementID: 54,
						IataCode:     "SVX",
						SirenaCode:   "ЕКБ",
						IcaoCode:     "USSS",
					},
					{
						AirportID:    101,
						SettlementID: 1,
						IataCode:     "JFK",
						IcaoCode:     "KJFK",
					},
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			service := NewFlightsNetworkService(testStorage)
			response, err := service.GetFlightsNetwork([]string{tt.carrierCode})
			if tt.expectedErr != 0 {
				assert.Error(t, err)
				if err != nil {
					assert.IsType(t, &utils.ErrorWithHTTPCode{}, err)
					assert.Equal(t, tt.expectedErr, err.(*utils.ErrorWithHTTPCode).HTTPCode)
				}
			} else {
				sort.SliceStable(
					response.Stations,
					func(i, j int) bool { return response.Stations[i].AirportID < response.Stations[j].AirportID },
				)
				assert.Equal(t, tt.expectedResponse, response)
			}
		})
	}
}

type tMockCarrierService struct {
}

func (m tMockCarrierService) GetCarrierByCodeAndFlightNumber(carrierCode, flightNumber string) int32 {
	return 26
}

func PrepareTestStorage() *storage.Storage {
	testStorage := storage.NewStorageWithStartDate("2020-08-01")
	testStorage.Timezones().PutTimezone(&rasp.TTimeZone{Id: 200, Code: "Asia/Yekaterinburg"})
	testStorage.Timezones().PutTimezone(&rasp.TTimeZone{Id: 201, Code: "America/New_York"})

	testStorage.PutStation(&snapshots.TStationWithCodes{
		Station: &rasp.TStation{
			Id:           100,
			CountryId:    225,
			SettlementId: 54,
			TimeZoneId:   200,
		},
		IataCode:   "SVX",
		SirenaCode: "ЕКБ",
		IcaoCode:   "USSS",
	})

	testStorage.PutStation(&snapshots.TStationWithCodes{
		Station: &rasp.TStation{
			Id:           101,
			CountryId:    101,
			SettlementId: 1,
			TimeZoneId:   201,
		},
		IataCode: "JFK",
		IcaoCode: "KJFK",
	})

	testStorage.PutFlightBase(structs.FlightBase{
		ID:                     300,
		OperatingCarrier:       26,
		OperatingCarrierCode:   "SU",
		OperatingFlightNumber:  "5555",
		DepartureStation:       100,
		DepartureStationCode:   "SVX",
		DepartureTimeScheduled: 500,
		DepartureTerminal:      "A",
		ArrivalStation:         101,
		ArrivalStationCode:     "JFK",
		ArrivalTimeScheduled:   1400,
		ArrivalTerminal:        "B",
		AircraftTypeID:         123,
		LegNumber:              1,
	})

	testStorage.PutFlightPattern(structs.FlightPattern{
		ID:                    500,
		FlightBaseID:          300,
		OperatingFromDate:     "2020-07-06",
		OperatingUntilDate:    "2020-12-27",
		OperatingOnDays:       123567,
		MarketingCarrier:      26,
		MarketingCarrierCode:  "SU",
		MarketingFlightNumber: "55",
	})

	return testStorage
}
