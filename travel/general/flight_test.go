package flight

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	dto "a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/DTO"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/carrier"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/flightstatus"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/timezone"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/storage"
	"a.yandex-team.ru/travel/avia/shared_flights/api/pkg/structs"
	"a.yandex-team.ru/travel/proto/dicts/rasp"
	"a.yandex-team.ru/travel/proto/shared_flights/snapshots"
)

func Test_GetFlight_FromParameter(t *testing.T) {

	store := simpleStorage()
	tzstore := timezone.NewTimeZoneUtil(store.Timezones(), store.Stations())
	carrierService := carrier.NewCarrierService(store.CarrierStorage(), store.IataCorrector())

	type fields struct {
		Storage        *storage.Storage
		TimeZoneUtil   timezone.TimeZoneUtil
		CarrierService CarrierService
	}
	type args struct {
		carrierText         string
		flightNumber        string
		departureDateString string
		nowDate             time.Time
		fromStation         *snapshots.TStationWithCodes
	}
	SVX, _ := store.Stations().ByID(9600370)
	DME, _ := store.Stations().ByID(9600216)
	tests := []struct {
		name         string
		fields       fields
		args         args
		wantResponse FlightSegment
		wantErr      bool
	}{
		{
			name: "overnight: request first leg in multileg flight",
			fields: fields{
				Storage:        store,
				TimeZoneUtil:   tzstore,
				CarrierService: carrierService,
			},
			args: args{
				carrierText:         "SU",
				flightNumber:        "1234",
				departureDateString: "2020-02-20",
				nowDate:             time.Date(2020, 2, 1, 0, 0, 0, 0, time.UTC),
				fromStation:         SVX,
			},
			wantResponse: FlightSegment{
				CompanyIata:       "SU",
				CompanyRaspID:     26,
				Title:             "SU 1234",
				Number:            "1234",
				AirportFromIata:   "SVX",
				AirportFromRaspID: 9600370,
				DepartureDay:      "2020-02-20",
				DepartureTime:     "20:00:00",
				DepartureTzName:   "Asia/Yekaterinburg",
				DepartureUTC:      "2020-02-20 15:00:00",
				AirportToIata:     "AER",
				AirportToRaspID:   9623547,
				ArrivalDay:        "2020-02-21",
				ArrivalTime:       "08:20:00",
				ArrivalTzName:     "Europe/Moscow",
				ArrivalUTC:        "2020-02-21 05:20:00",
				Status: flightstatus.FlightStatus{
					Status:          "unknown",
					DepartureStatus: "unknown",
					ArrivalStatus:   "unknown",
					DepartureSource: "0",
					ArrivalSource:   "0",
				},
				FlightCodeshares: []dto.TitledFlight{},
				Segments: []*FlightSegment{
					{
						CompanyIata:       "SU",
						CompanyRaspID:     26,
						Title:             "SU 1234",
						Number:            "1234",
						AirportFromIata:   "SVX",
						AirportFromRaspID: 9600370,
						DepartureDay:      "2020-02-20",
						DepartureTime:     "20:00:00",
						DepartureTzName:   "Asia/Yekaterinburg",
						DepartureUTC:      "2020-02-20 15:00:00",
						AirportToIata:     "DME",
						AirportToRaspID:   9600216,
						ArrivalDay:        "2020-02-21",
						ArrivalTime:       "05:02:00",
						ArrivalTzName:     "Europe/Moscow",
						ArrivalUTC:        "2020-02-21 02:02:00",
						Segments:          []*FlightSegment{},
						Status: flightstatus.FlightStatus{
							DepartureStatus: "unknown",
							ArrivalStatus:   "unknown",
							Status:          "unknown",
						},
					},
					{
						CompanyIata:       "SU",
						CompanyRaspID:     26,
						Title:             "SU 1234",
						Number:            "1234",
						AirportFromIata:   "DME",
						AirportFromRaspID: 9600216,
						DepartureDay:      "2020-02-21",
						DepartureTime:     "06:10:00",
						DepartureTzName:   "Europe/Moscow",
						DepartureUTC:      "2020-02-21 03:10:00",
						AirportToIata:     "AER",
						AirportToRaspID:   9623547,
						ArrivalDay:        "2020-02-21",
						ArrivalTime:       "08:20:00",
						ArrivalTzName:     "Europe/Moscow",
						ArrivalUTC:        "2020-02-21 05:20:00",
						Segments:          []*FlightSegment{},
						Status: flightstatus.FlightStatus{
							DepartureStatus: "unknown",
							ArrivalStatus:   "unknown",
							Status:          "unknown",
						},
					},
				},
				Source: "",
			},
			wantErr: false,
		},
		{
			name: "overnight: request for mid-flight segment",
			fields: fields{
				Storage:        store,
				TimeZoneUtil:   tzstore,
				CarrierService: carrierService,
			},
			args: args{
				carrierText:         "SU",
				flightNumber:        "1234",
				departureDateString: "2020-02-20",
				nowDate:             time.Date(2020, 2, 1, 0, 0, 0, 0, time.UTC),
				fromStation:         DME,
			},
			wantResponse: FlightSegment{
				CompanyIata:       "SU",
				CompanyRaspID:     26,
				Title:             "SU 1234",
				Number:            "1234",
				AirportFromIata:   "SVX",
				AirportFromRaspID: 9600370,
				DepartureDay:      "2020-02-19",
				DepartureTime:     "20:00:00",
				DepartureTzName:   "Asia/Yekaterinburg",
				DepartureUTC:      "2020-02-19 15:00:00",
				AirportToIata:     "AER",
				AirportToRaspID:   9623547,
				ArrivalDay:        "2020-02-20",
				ArrivalTime:       "08:20:00",
				ArrivalTzName:     "Europe/Moscow",
				ArrivalUTC:        "2020-02-20 05:20:00",
				Status: flightstatus.FlightStatus{
					Status:          "unknown",
					DepartureStatus: "unknown",
					ArrivalStatus:   "unknown",
					DepartureSource: "0",
					ArrivalSource:   "0",
				},
				FlightCodeshares: []dto.TitledFlight{},
				Segments: []*FlightSegment{
					{
						CompanyIata:       "SU",
						CompanyRaspID:     26,
						Title:             "SU 1234",
						Number:            "1234",
						AirportFromIata:   "SVX",
						AirportFromRaspID: 9600370,
						DepartureDay:      "2020-02-19",
						DepartureTime:     "20:00:00",
						DepartureTzName:   "Asia/Yekaterinburg",
						DepartureUTC:      "2020-02-19 15:00:00",
						AirportToIata:     "DME",
						AirportToRaspID:   9600216,
						ArrivalDay:        "2020-02-20",
						ArrivalTime:       "05:02:00",
						ArrivalTzName:     "Europe/Moscow",
						ArrivalUTC:        "2020-02-20 02:02:00",
						Segments:          []*FlightSegment{},
						Status: flightstatus.FlightStatus{
							DepartureStatus: "unknown",
							ArrivalStatus:   "unknown",
							Status:          "unknown",
						},
					},
					{
						CompanyIata:       "SU",
						CompanyRaspID:     26,
						Title:             "SU 1234",
						Number:            "1234",
						AirportFromIata:   "DME",
						AirportFromRaspID: 9600216,
						DepartureDay:      "2020-02-20",
						DepartureTime:     "06:10:00",
						DepartureTzName:   "Europe/Moscow",
						DepartureUTC:      "2020-02-20 03:10:00",
						AirportToIata:     "AER",
						AirportToRaspID:   9623547,
						ArrivalDay:        "2020-02-20",
						ArrivalTime:       "08:20:00",
						ArrivalTzName:     "Europe/Moscow",
						ArrivalUTC:        "2020-02-20 05:20:00",
						Segments:          []*FlightSegment{},
						Status: flightstatus.FlightStatus{
							DepartureStatus: "unknown",
							ArrivalStatus:   "unknown",
							Status:          "unknown",
						},
					},
				},
				Source: "",
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			service := &flightServiceImpl{
				Storage:        tt.fields.Storage,
				TimeZoneUtil:   tt.fields.TimeZoneUtil,
				CarrierService: tt.fields.CarrierService,
			}
			gotResponse, err := service.GetFlight(
				tt.args.carrierText, tt.args.flightNumber, tt.args.departureDateString, tt.args.fromStation,
				"", false, tt.args.nowDate, false, false,
			)
			if (err != nil) != tt.wantErr {
				t.Errorf("GetFlight() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			assert.Equalf(t, tt.wantResponse, *gotResponse, "GetFlight() gotResponse = %v, want %v", gotResponse, tt.wantResponse)
		})
	}
}

func simpleStorage() *storage.Storage {
	store := storage.NewStorageWithStartDate("2020-02-01")

	store.Timezones().PutTimezone(&rasp.TTimeZone{
		Id:   5,
		Code: "Asia/Yekaterinburg",
	})
	store.Timezones().PutTimezone(&rasp.TTimeZone{
		Id:   0,
		Code: "Europe/Moscow",
	})
	store.PutStation(&snapshots.TStationWithCodes{
		Station: &rasp.TStation{
			Id:         9600370,
			TimeZoneId: 5,
		},
		IataCode:   "SVX",
		IcaoCode:   "USSS",
		SirenaCode: "ЕКБ",
	})

	store.PutStation(&snapshots.TStationWithCodes{
		Station: &rasp.TStation{
			Id:         9600216,
			TimeZoneId: 0,
		},
		IataCode:   "DME",
		IcaoCode:   "UDDD",
		SirenaCode: "ДМД",
	})

	store.PutStation(&snapshots.TStationWithCodes{
		Station: &rasp.TStation{
			Id:         9623547,
			TimeZoneId: 0,
		},
		IataCode:   "AER",
		IcaoCode:   "URSS",
		SirenaCode: "СОЧ",
	})

	fb1 := structs.FlightBase{
		ID:                     1000001,
		OperatingCarrier:       26,
		OperatingCarrierCode:   "SU",
		OperatingFlightNumber:  "1234",
		LegNumber:              1,
		DepartureStation:       9600370,
		DepartureStationCode:   "SVX",
		DepartureTimeScheduled: 2000,
		ArrivalStation:         9600216,
		ArrivalStationCode:     "DME",
		ArrivalTimeScheduled:   502,
	}

	fb2 := structs.FlightBase{
		ID:                     1000002,
		OperatingCarrier:       26,
		OperatingCarrierCode:   "SU",
		OperatingFlightNumber:  "1234",
		LegNumber:              2,
		DepartureStation:       9600216,
		DepartureStationCode:   "DME",
		DepartureTimeScheduled: 610,
		ArrivalStation:         9623547,
		ArrivalStationCode:     "AER",
		ArrivalTimeScheduled:   820,
	}
	store.PutFlightBase(fb2)
	store.PutFlightBase(fb1)

	store.PutFlightPattern(operatingFlightPattern(structs.FlightPattern{
		ID:                 20001,
		OperatingFromDate:  "2020-02-12",
		OperatingUntilDate: "2020-03-15",
		OperatingOnDays:    12345,
		ArrivalDayShift:    1,
		FlightDayShift:     0,
	}, fb1))

	store.PutFlightPattern(operatingFlightPattern(structs.FlightPattern{
		ID:                 20002,
		OperatingFromDate:  "2020-02-13",
		OperatingUntilDate: "2020-03-16",
		OperatingOnDays:    23456,
		ArrivalDayShift:    0,
		FlightDayShift:     1,
	}, fb2))

	return store
}

func operatingFlightPattern(pattern structs.FlightPattern, base structs.FlightBase) structs.FlightPattern {
	pattern.FlightBaseID = base.ID
	pattern.LegNumber = base.LegNumber
	pattern.MarketingCarrier = base.OperatingCarrier
	pattern.MarketingCarrierCode = base.OperatingCarrierCode
	pattern.MarketingFlightNumber = base.OperatingFlightNumber
	return pattern
}
