package stationschedule

import (
	"reflect"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/flightdata"
	dto "a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/DTO"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/stationschedule/format"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/threads"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/timezone"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/storage"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/storage/flight"
	"a.yandex-team.ru/travel/avia/shared_flights/api/pkg/structs"
	"a.yandex-team.ru/travel/avia/shared_flights/lib/go/direction"
	"a.yandex-team.ru/travel/proto/dicts/rasp"
	"a.yandex-team.ru/travel/proto/shared_flights/snapshots"
)

func Test_groupsFlights(t *testing.T) {
	tests := []struct {
		name        string
		groups      map[dto.FlightID]FlightBaseGroup
		wantFlights []format.Flight
	}{
		{
			"no flights - no group",
			map[dto.FlightID]FlightBaseGroup{},
			[]format.Flight{},
		},
		{
			"single flight - single group",
			map[dto.FlightID]FlightBaseGroup{
				dto.FlightID{
					AirlineID: 26,
					Number:    "1202",
				}: {
					FlightBase: structs.FlightBase{
						OperatingCarrier:      26,
						OperatingCarrierCode:  "SU",
						OperatingFlightNumber: "1202",
					},
					FlightTitle: "SU 1202",
					Map: map[timeTerminal][]RouteMask{
						timeTerminal{
							Time:      1234,
							Terminal:  "A",
							Departure: 123,
							Arrival:   456,
						}: {
							{
								Route: dto.Route{123, 456},
								Masks: format.Mask{
									From:  "2020-01-02",
									Until: "2020-01-03",
									On:    1234567,
								},
							},
						},
					},
				},
			},
			[]format.Flight{
				{
					TitledFlight: dto.TitledFlight{
						FlightID: dto.FlightID{
							AirlineID: 26,
							Number:    "1202",
						},
						Title: "SU 1202",
					},
					Schedules: []format.Schedule{
						{
							Time:      "12:34:00",
							Terminal:  "A",
							StartTime: "00:00:00",
							Route:     dto.Route{123, 456},
							Masks: []format.Mask{
								{
									From:  "2020-01-02",
									Until: "2020-01-03",
									On:    1234567,
								},
							},
						},
					},
				},
			},
		},

		{
			"complex schedule with varying route",
			map[dto.FlightID]FlightBaseGroup{
				dto.FlightID{
					AirlineID: 26,
					Number:    "1202",
				}: {
					FlightBase: structs.FlightBase{
						OperatingCarrier:      26,
						OperatingCarrierCode:  "SU",
						OperatingFlightNumber: "1202",
					},
					FlightTitle: "SU 1202",
					Map: map[timeTerminal][]RouteMask{
						timeTerminal{
							Time:      1234,
							Terminal:  "A",
							Departure: 123,
							Arrival:   456,
						}: {
							{
								Route: dto.Route{123, 456},
								Masks: format.Mask{
									From:  "2020-01-02",
									Until: "2020-01-03",
									On:    1234567,
								},
							},
							{
								Route: dto.Route{123, 456},
								Masks: format.Mask{
									From:  "2020-01-04",
									Until: "2020-01-05",
									On:    1234567,
								},
							},
							{
								Route: dto.Route{123, 457},
								Masks: format.Mask{
									From:  "2020-01-06",
									Until: "2020-01-07",
									On:    1234567,
								},
							},
						},
					},
				},
			},
			[]format.Flight{
				{
					TitledFlight: dto.TitledFlight{
						FlightID: dto.FlightID{
							AirlineID: 26,
							Number:    "1202",
						},
						Title: "SU 1202",
					},
					Schedules: []format.Schedule{
						{
							Time:      "12:34:00",
							Terminal:  "A",
							StartTime: "00:00:00",
							Route:     dto.Route{123, 456},
							Masks: []format.Mask{
								{
									From:  "2020-01-02",
									Until: "2020-01-03",
									On:    1234567,
								},
								{
									From:  "2020-01-04",
									Until: "2020-01-05",
									On:    1234567,
								},
							},
						},
						{
							Time:      "12:34:00",
							Terminal:  "A",
							StartTime: "00:00:00",
							Route:     dto.Route{123, 457},
							Masks: []format.Mask{
								{
									From:  "2020-01-06",
									Until: "2020-01-07",
									On:    1234567,
								},
							},
						},
					},
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			gotFlights := flightsFromGroup(tt.groups)
			assert.Equal(t, tt.wantFlights, gotFlights)
		})
	}
}

func Test_schedules(t *testing.T) {
	type args struct {
	}
	tests := []struct {
		name          string
		group         map[timeTerminal][]RouteMask
		wantSchedules []format.Schedule
	}{
		{
			"nil group = nil schedule",
			nil,
			nil,
		},
		{
			"single group single item",
			map[timeTerminal][]RouteMask{
				timeTerminal{
					Time:      1234,
					Terminal:  "A",
					Departure: 123,
					Arrival:   456,
				}: {
					{
						Route: dto.Route{123, 456},
						Masks: format.Mask{
							From:  "2020-01-02",
							Until: "2020-01-03",
							On:    1256,
						},
					},
				},
			},
			[]format.Schedule{
				format.Schedule{
					Time:      "12:34:00",
					Terminal:  "A",
					StartTime: "00:00:00",
					Route:     dto.Route{123, 456},
					Masks: []format.Mask{
						format.Mask{
							From:  "2020-01-02",
							Until: "2020-01-03",
							On:    1256,
						},
					},
				},
			},
		},
		{
			"two groups with one mask each",
			map[timeTerminal][]RouteMask{
				timeTerminal{
					Time:          1234,
					Terminal:      "A",
					StartTime:     123,
					StartDayShift: -1,
					Departure:     123,
					Arrival:       456,
				}: {
					{
						Route: dto.Route{123, 456},
						Masks: format.Mask{
							From:  "2020-01-02",
							Until: "2020-01-03",
							On:    1256,
						},
					},
				},
				timeTerminal{
					Time:      2210,
					Terminal:  "A",
					Departure: 123,
					Arrival:   456,
				}: {
					{
						Route: dto.Route{123, 456},
						Masks: format.Mask{
							From:  "2020-01-02",
							Until: "2020-01-03",
							On:    1256,
						},
					},
				},
			},
			[]format.Schedule{
				format.Schedule{
					Time:          "12:34:00",
					Terminal:      "A",
					StartTime:     "01:23:00",
					StartDayShift: -1,
					Route:         dto.Route{123, 456},
					Masks: []format.Mask{
						format.Mask{
							From:  "2020-01-02",
							Until: "2020-01-03",
							On:    1256,
						},
					},
				},
				format.Schedule{
					Time:      "22:10:00",
					Terminal:  "A",
					StartTime: "00:00:00",
					Route:     dto.Route{123, 456},
					Masks: []format.Mask{
						format.Mask{
							From:  "2020-01-02",
							Until: "2020-01-03",
							On:    1256,
						},
					},
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			gotSchedules := schedules(tt.group)
			assert.Equal(t, tt.wantSchedules, gotSchedules)
		})
	}
}

func Test_stationScheduleServiceImpl_GetStationSchedule(t *testing.T) {
	type fields struct {
		Storage            *storage.Storage
		TimeZoneUtil       timezone.TimeZoneUtil
		ThreadRouteService threads.ThreadRouteService
	}
	type args struct {
		station   *snapshots.TStationWithCodes
		direction direction.Direction
		now       time.Time
	}
	tests := []struct {
		name         string
		fields       fields
		args         args
		wantResponse format.Response
		wantErr      bool
	}{
		// TODO: Add test cases.
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			service := &stationScheduleServiceImpl{
				Storage:            tt.fields.Storage,
				TimeZoneUtil:       tt.fields.TimeZoneUtil,
				ThreadRouteService: tt.fields.ThreadRouteService,
			}
			gotResponse, err := service.GetStationSchedule(tt.args.station, tt.args.direction, "", tt.args.now)
			if (err != nil) != tt.wantErr {
				t.Errorf("GetStationSchedule() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(gotResponse, tt.wantResponse) {
				t.Errorf("GetStationSchedule() gotResponse = %v, want %v", gotResponse, tt.wantResponse)
			}
		})
	}
}

func Test_stationScheduleServiceImpl_attachFlightBases(t *testing.T) {
	type fields struct {
		Storage            *storage.Storage
		TimeZoneUtil       timezone.TimeZoneUtil
		ThreadRouteService threads.ThreadRouteService
	}
	tests := []struct {
		name      string
		fields    fields
		patterns  []*structs.FlightPattern
		wantBases []*flightdata.FlightDataBase
		wantErr   bool
	}{
		// TODO: Add test cases.
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			service := &stationScheduleServiceImpl{
				Storage:            tt.fields.Storage,
				TimeZoneUtil:       tt.fields.TimeZoneUtil,
				ThreadRouteService: tt.fields.ThreadRouteService,
			}

			bases, err := service.attachFlightBases(tt.patterns, "")
			assert.NoError(t, err)
			assert.Equal(t, tt.wantBases, bases)
		})
	}
}

func Test_stationScheduleServiceImpl_attachFlightBasesWithBannedFlight(t *testing.T) {
	testStorage := storage.NewStorageWithStartDate("2020-08-01")
	tz := timezone.NewTimeZoneUtil(testStorage.Timezones(), testStorage.Stations())
	trs := threads.NewThreadRouteService(testStorage, tz)

	testStorage.Timezones().PutTimezone(&rasp.TTimeZone{Id: 200, Code: "Asia/Yekaterinburg"})
	testStorage.Timezones().PutTimezone(&rasp.TTimeZone{Id: 201, Code: "America/New_York"})

	testRule := snapshots.TBlacklistRule{
		Id:                    1,
		MarketingCarrierId:    26,
		MarketingFlightNumber: "55",
		ForceMode:             "FORCE_BAN",
		FlightDateSince:       "2020-09-01",
		FlightDateUntil:       "2020-09-30",
	}
	banRuleStorage := flight.NewBlacklistRuleStorage(testStorage.Stations())
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
		LegNumber:              1,
	}
	testStorage.PutFlightBase(flightBaseSVXJFK)

	testPattern := structs.FlightPattern{
		ID:                    500,
		FlightBaseID:          300,
		OperatingFromDate:     "2020-07-01",
		OperatingUntilDate:    "2020-12-31",
		OperatingOnDays:       123567,
		MarketingCarrier:      26,
		MarketingCarrierCode:  "SU",
		MarketingFlightNumber: "55",
	}
	testStorage.PutFlightPattern(testPattern)

	tests := []struct {
		name             string
		patterns         []*structs.FlightPattern
		expectedBases    []structs.FlightBase
		expectedPatterns []structs.FlightPattern
	}{
		{
			name: "split flight pattern",
			patterns: []*structs.FlightPattern{
				&testPattern,
			},
			expectedBases: []structs.FlightBase{
				flightBaseSVXJFK,
				flightBaseSVXJFK,
			},
			expectedPatterns: []structs.FlightPattern{
				{
					ID:                    500,
					FlightBaseID:          300,
					OperatingFromDate:     "2020-07-01",
					OperatingUntilDate:    "2020-08-31",
					OperatingOnDays:       123567,
					MarketingCarrier:      26,
					MarketingCarrierCode:  "SU",
					MarketingFlightNumber: "55",
				},
				{
					ID:                    500,
					FlightBaseID:          300,
					OperatingFromDate:     "2020-10-01",
					OperatingUntilDate:    "2020-12-31",
					OperatingOnDays:       123567,
					MarketingCarrier:      26,
					MarketingCarrierCode:  "SU",
					MarketingFlightNumber: "55",
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			service := &stationScheduleServiceImpl{
				Storage:            testStorage,
				TimeZoneUtil:       tz,
				ThreadRouteService: trs,
			}

			bases, err := service.attachFlightBases(tt.patterns, "")
			assert.NoError(t, err)

			resultBases := make([]structs.FlightBase, 0)
			resultPatterns := make([]structs.FlightPattern, 0)
			for _, base := range bases {
				resultBases = append(resultBases, base.FlightBase)
				resultPatterns = append(resultPatterns, *base.FlightPattern)
			}
			assert.Equal(t, tt.expectedPatterns, resultPatterns)
			assert.Equal(t, tt.expectedBases, resultBases)
		})
	}
}

func Test_stationScheduleServiceImpl_groupFlights(t *testing.T) {
	type fields struct {
		Storage            *storage.Storage
		TimeZoneUtil       timezone.TimeZoneUtil
		ThreadRouteService threads.ThreadRouteService
	}
	type args struct {
		bases     []*flightdata.FlightDataBase
		groups    map[dto.FlightID]FlightBaseGroup
		direction direction.Direction
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		wantErr bool
	}{
		// TODO: Add test cases.
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			service := &stationScheduleServiceImpl{
				Storage:            tt.fields.Storage,
				TimeZoneUtil:       tt.fields.TimeZoneUtil,
				ThreadRouteService: tt.fields.ThreadRouteService,
			}
			now := time.Date(2020, 6, 1, 0, 0, 0, 0, time.UTC)
			if err := service.flightIDGroup(tt.args.bases, tt.args.groups, tt.args.direction, now); (err != nil) != tt.wantErr {
				t.Errorf("flightIDGroup() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}
