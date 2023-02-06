package flightschedule

import (
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/flight"
	p2pformat "a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/flight_p2p/format"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/flight_schedule/format"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/segment"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/timezone"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/storage"
	flightStorage "a.yandex-team.ru/travel/avia/shared_flights/api/internal/storage/flight"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/utils"
	"a.yandex-team.ru/travel/avia/shared_flights/api/pkg/structs"
	"a.yandex-team.ru/travel/avia/shared_flights/lib/go/dtutil"
	"a.yandex-team.ru/travel/proto/dicts/rasp"
	"a.yandex-team.ru/travel/proto/shared_flights/snapshots"
)

func Test_FlightScheduleService_GetFlightSchedule(t *testing.T) {
	segment.SetGlobalStartDateIndex(dtutil.DateCache.IndexOfStringDateP("2020-06-01"))
	startScheduleDate := dtutil.IntDate(20200601)
	testStorage, testFlightService := PrepareTestStorage()
	tests := []struct {
		name             string
		carrierCode      string
		flightNumber     string
		showBanned       bool
		expectedResponse format.Response
		expectedErr      int
	}{
		{
			name:         "no flights",
			carrierCode:  "SU",
			flightNumber: "123",
			showBanned:   false,
			expectedErr:  404,
		},
		{
			name:         "single flight partially banned",
			carrierCode:  "SU",
			flightNumber: "55",
			showBanned:   false,
			expectedResponse: format.Response{
				Title:     "SU 55",
				CarrierID: 26,
				Schedules: []format.Schedule{
					{
						Route: []format.StopPoint{
							{
								AirportID:         100,
								AirportCode:       "SVX",
								DepartureTime:     "05:00:00",
								DepartureTerminal: "A",
							},
							{
								AirportID:       101,
								AirportCode:     "JFK",
								ArrivalTime:     "14:00:00",
								ArrivalTerminal: "B",
							},
						},
						Masks: []p2pformat.Mask{
							{
								From:  "2020-07-06",
								Until: "2020-09-06",
								On:    123567,
							},
							{
								From:  "2020-09-14",
								Until: "2020-12-27",
								On:    123567,
							},
						},
						TransportModelID: 123,
					},
				},
			},
		},
		{
			name:         "single flight show banned",
			carrierCode:  "SU",
			flightNumber: "55",
			showBanned:   true,
			expectedResponse: format.Response{
				Title:     "SU 55",
				CarrierID: 26,
				Schedules: []format.Schedule{
					{
						Route: []format.StopPoint{
							{
								AirportID:         100,
								AirportCode:       "SVX",
								DepartureTime:     "05:00:00",
								DepartureTerminal: "A",
							},
							{
								AirportID:       101,
								AirportCode:     "JFK",
								ArrivalTime:     "14:00:00",
								ArrivalTerminal: "B",
							},
						},
						Masks: []p2pformat.Mask{
							{
								From:  "2020-07-06",
								Until: "2020-12-27",
								On:    123567,
							},
						},
						TransportModelID: 123,
					},
				},
			},
		},
		{
			name:         "two legs flight",
			carrierCode:  "SU",
			flightNumber: "77",
			expectedResponse: format.Response{
				Title:     "SU 77",
				CarrierID: 26,
				Schedules: []format.Schedule{
					{
						Route: []format.StopPoint{
							{
								AirportID:         100,
								AirportCode:       "SVX",
								DepartureTime:     "23:00:00",
								DepartureTerminal: "A",
							},
							{
								AirportID:         102,
								AirportCode:       "PEE",
								ArrivalTime:       "01:00:00",
								ArrivalTerminal:   "B",
								ArrivalDayShift:   1,
								DepartureTime:     "04:00:00",
								DepartureDayShift: 1,
								DepartureTerminal: "A",
							},
							{
								AirportID:       101,
								AirportCode:     "JFK",
								ArrivalTime:     "15:00:00",
								ArrivalDayShift: 1,
								ArrivalTerminal: "B",
							},
						},
						Masks: []p2pformat.Mask{
							{
								From:  "2020-07-07",
								Until: "2020-10-19",
								On:    23,
							},
							{
								From:  "2020-11-17",
								Until: "2020-11-30",
								On:    23,
							},
						},
					},
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			service := NewFlightScheduleService(testStorage, testFlightService)
			response, err := service.GetFlightSchedule(tt.carrierCode, tt.flightNumber, "", tt.showBanned, startScheduleDate)
			if tt.expectedErr != 0 {
				assert.Error(t, err)
				if err != nil {
					assert.IsType(t, &utils.ErrorWithHTTPCode{}, err)
					assert.Equal(t, tt.expectedErr, err.(*utils.ErrorWithHTTPCode).HTTPCode)
				}
			} else {
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

func PrepareTestStorage() (testStorage *storage.Storage, testFlightService flight.FlightService) {
	testStorage = storage.NewStorageWithStartDate("2020-08-01")
	testStorage.Timezones().PutTimezone(&rasp.TTimeZone{Id: 200, Code: "Asia/Yekaterinburg"})
	testStorage.Timezones().PutTimezone(&rasp.TTimeZone{Id: 201, Code: "America/New_York"})

	testRule := snapshots.TBlacklistRule{
		Id:                    1,
		MarketingCarrierId:    26,
		MarketingFlightNumber: "55",
		ForceMode:             "FORCE_BAN",
		FlightDateSince:       "2020-09-07",
		FlightDateUntil:       "2020-09-13",
	}
	banRuleStorage := flightStorage.NewBlacklistRuleStorage(testStorage.Stations())
	banRuleStorage.AddRule(&testRule)
	testStorage.SetBlacklistRuleStorage(banRuleStorage)

	testStorage.PutStation(&snapshots.TStationWithCodes{
		Station: &rasp.TStation{
			Id:         100,
			TimeZoneId: 200,
		},
		IataCode: "SVX",
	})

	testStorage.PutStation(&snapshots.TStationWithCodes{
		Station: &rasp.TStation{
			Id:         101,
			TimeZoneId: 201,
		},
		IataCode: "JFK",
	})

	testStorage.PutStation(&snapshots.TStationWithCodes{
		Station: &rasp.TStation{
			Id:         102,
			TimeZoneId: 200,
		},
		IataCode: "PEE",
	})

	flightBaseSVXJFK := structs.FlightBase{
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
	}
	testStorage.PutFlightBase(flightBaseSVXJFK)

	flightBaseSVXPEE := structs.FlightBase{
		ID:                     401,
		OperatingCarrier:       26,
		OperatingCarrierCode:   "SU",
		OperatingFlightNumber:  "7777",
		DepartureStation:       100,
		DepartureStationCode:   "SVX",
		DepartureTimeScheduled: 2300,
		DepartureTerminal:      "A",
		ArrivalStation:         102,
		ArrivalStationCode:     "PEE",
		ArrivalTimeScheduled:   100,
		ArrivalTerminal:        "B",
		LegNumber:              1,
	}
	testStorage.PutFlightBase(flightBaseSVXPEE)

	flightBasePEEJFK := structs.FlightBase{
		ID:                     402,
		OperatingCarrier:       26,
		OperatingCarrierCode:   "SU",
		OperatingFlightNumber:  "7777",
		DepartureStation:       102,
		DepartureStationCode:   "PEE",
		DepartureTimeScheduled: 400,
		DepartureTerminal:      "A",
		ArrivalStation:         101,
		ArrivalStationCode:     "JFK",
		ArrivalTimeScheduled:   1500,
		ArrivalTerminal:        "B",
		LegNumber:              2,
	}
	testStorage.PutFlightBase(flightBasePEEJFK)

	testPatternSVXJFK := structs.FlightPattern{
		ID:                    500,
		FlightBaseID:          300,
		OperatingFromDate:     "2020-07-06",
		OperatingUntilDate:    "2020-12-27",
		OperatingOnDays:       123567,
		MarketingCarrier:      26,
		MarketingCarrierCode:  "SU",
		MarketingFlightNumber: "55",
	}
	testStorage.PutFlightPattern(testPatternSVXJFK)

	testPatternSVXPEE := structs.FlightPattern{
		ID:                    601,
		FlightBaseID:          401,
		OperatingFromDate:     "2020-07-06",
		OperatingUntilDate:    "2020-11-29",
		OperatingOnDays:       23,
		MarketingCarrier:      26,
		MarketingCarrierCode:  "SU",
		MarketingFlightNumber: "77",
		LegNumber:             1,
		ArrivalDayShift:       1,
	}
	testStorage.PutFlightPattern(testPatternSVXPEE)

	testPatternPEEJFK1 := structs.FlightPattern{
		ID:                    602,
		FlightBaseID:          402,
		OperatingFromDate:     "2020-07-06",
		OperatingUntilDate:    "2020-10-18",
		OperatingOnDays:       34,
		MarketingCarrier:      26,
		MarketingCarrierCode:  "SU",
		MarketingFlightNumber: "77",
		LegNumber:             2,
		DepartureDayShift:     1,
	}
	testStorage.PutFlightPattern(testPatternPEEJFK1)

	testPatternPEEJFK2 := structs.FlightPattern{
		ID:                    603,
		FlightBaseID:          402,
		OperatingFromDate:     "2020-11-16",
		OperatingUntilDate:    "2020-11-30",
		OperatingOnDays:       34,
		MarketingCarrier:      26,
		MarketingCarrierCode:  "SU",
		MarketingFlightNumber: "77",
		LegNumber:             2,
		DepartureDayShift:     1,
	}
	testStorage.PutFlightPattern(testPatternPEEJFK2)

	tz := timezone.NewTimeZoneUtil(testStorage.Timezones(), testStorage.Stations())
	testFlightService = flight.NewFlightService(testStorage, tz, tMockCarrierService{})

	return testStorage, testFlightService
}
