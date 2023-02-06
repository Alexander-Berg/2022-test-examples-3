package structs

import (
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/proto/shared_flights/snapshots"
)

func TestFlightStatusFromProto(t *testing.T) {
	flightStatusProto := &snapshots.TFlightStatus{
		AirlineId:                    1,
		CarrierCode:                  "CarrierCode",
		FlightNumber:                 "FlightNumber",
		LegNumber:                    2,
		FlightDate:                   "FlightDate",
		StatusSourceId:               3,
		CreatedAtUtc:                 "CreatedAtUTC",
		UpdatedAtUtc:                 "UpdatedAtUTC",
		DepartureTimeActual:          "DepartureTimeActual",
		DepartureTimeScheduled:       "DepartureTimeScheduled",
		DepartureStatus:              "DepartureStatus",
		DepartureGate:                "DepartureGate",
		DepartureTerminal:            "DepartureTerminal",
		DepartureDiverted:            true,
		DepartureDivertedAirportCode: "DepartureDivertedAirportCode",
		DepartureCreatedAtUtc:        "DepartureCreatedAtUTC",
		DepartureReceivedAtUtc:       "DepartureReceivedAtUTC",
		DepartureUpdatedAtUtc:        "DepartureUpdatedAtUTC",
		ArrivalTimeActual:            "ArrivalTimeActual",
		ArrivalTimeScheduled:         "ArrivalTimeScheduled",
		ArrivalStatus:                "ArrivalStatus",
		ArrivalGate:                  "ArrivalGate",
		ArrivalTerminal:              "ArrivalTerminal",
		ArrivalDiverted:              false,
		ArrivalDivertedAirportCode:   "ArrivalDivertedAirportCode",
		ArrivalCreatedAtUtc:          "ArrivalCreatedAtUTC",
		ArrivalReceivedAtUtc:         "ArrivalReceivedAtUTC",
		ArrivalUpdatedAtUtc:          "ArrivalUpdatedAtUTC",
		CheckInDesks:                 "CheckInDesks",
		BaggageCarousels:             "BaggageCarousels",
		DepartureStation:             4,
		ArrivalStation:               5,
		DepartureSourceId:            6,
		ArrivalSourceId:              7,
	}

	flightStatusInternalRepresentation := FlightStatus{}

	FlightStatusFromProto(flightStatusProto, &flightStatusInternalRepresentation)

	assert.Equal(t, flightStatusInternalRepresentation, FlightStatus{
		AirlineID:                    1,
		CarrierCode:                  "CarrierCode",
		FlightNumber:                 "FlightNumber",
		LegNumber:                    2,
		FlightDate:                   "FlightDate",
		StatusSourceID:               3,
		CreatedAtUtc:                 "CreatedAtUTC",
		UpdatedAtUtc:                 "UpdatedAtUTC",
		DepartureTimeActual:          "DepartureTimeActual",
		DepartureTimeScheduled:       "DepartureTimeScheduled",
		DepartureStatus:              "DepartureStatus",
		DepartureGate:                "DepartureGate",
		DepartureTerminal:            "DepartureTerminal",
		DepartureDiverted:            true,
		DepartureDivertedAirportCode: "DepartureDivertedAirportCode",
		DepartureCreatedAtUtc:        "DepartureCreatedAtUTC",
		DepartureReceivedAtUtc:       "DepartureReceivedAtUTC",
		DepartureUpdatedAtUtc:        "DepartureUpdatedAtUTC",
		ArrivalTimeActual:            "ArrivalTimeActual",
		ArrivalTimeScheduled:         "ArrivalTimeScheduled",
		ArrivalStatus:                "ArrivalStatus",
		ArrivalGate:                  "ArrivalGate",
		ArrivalTerminal:              "ArrivalTerminal",
		ArrivalDiverted:              false,
		ArrivalDivertedAirportCode:   "ArrivalDivertedAirportCode",
		ArrivalCreatedAtUtc:          "ArrivalCreatedAtUTC",
		ArrivalReceivedAtUtc:         "ArrivalReceivedAtUTC",
		ArrivalUpdatedAtUtc:          "ArrivalUpdatedAtUTC",
		CheckInDesks:                 "CheckInDesks",
		BaggageCarousels:             "BaggageCarousels",
		DepartureStation:             4,
		ArrivalStation:               5,
		DepartureSourceID:            6,
		ArrivalSourceID:              7,
	})
}

func TestFlightStatus_FillDivertedAirportIDs(t *testing.T) {
	type fields struct {
		DepartureDivertedAirportCode string
		DepartureDivertedAirportID   int32
		ArrivalDivertedAirportCode   string
		ArrivalDivertedAirportID     int32
	}
	type args struct {
		stationDecoders []stationDecoder
	}
	tests := []struct {
		name   string
		fields fields
		args   args
	}{
		{
			"arrival and departure codes; single decoder",
			fields{
				DepartureDivertedAirportCode: "WWW",
				DepartureDivertedAirportID:   111,
				ArrivalDivertedAirportCode:   "WWW",
				ArrivalDivertedAirportID:     111,
			},
			args{[]stationDecoder{func(string) (int32, bool) {
				return 111, true
			}}},
		},
		{
			"arrival and departure codes; two decoders, first always fails",
			fields{
				DepartureDivertedAirportCode: "WWW",
				DepartureDivertedAirportID:   111,
				ArrivalDivertedAirportCode:   "WWW",
				ArrivalDivertedAirportID:     111,
			},
			args{[]stationDecoder{func(string) (int32, bool) {
				return 0, false
			}, func(string) (int32, bool) {
				return 111, true
			}}},
		},
		{
			"departure code; single decoder",
			fields{
				DepartureDivertedAirportCode: "WWW",
				DepartureDivertedAirportID:   111,
				ArrivalDivertedAirportCode:   "",
				ArrivalDivertedAirportID:     0,
			},
			args{[]stationDecoder{func(string) (int32, bool) {
				return 111, true
			}}},
		},
		{
			"arrival code; single decoder",
			fields{
				DepartureDivertedAirportCode: "",
				DepartureDivertedAirportID:   0,
				ArrivalDivertedAirportCode:   "WWW",
				ArrivalDivertedAirportID:     111,
			},
			args{[]stationDecoder{func(string) (int32, bool) {
				return 111, true
			}}},
		},
		{
			"arrival and departure codes; no decoders",
			fields{
				DepartureDivertedAirportCode: "WWW",
				DepartureDivertedAirportID:   0,
				ArrivalDivertedAirportCode:   "WWW",
				ArrivalDivertedAirportID:     0,
			},
			args{[]stationDecoder{}},
		},
		{
			"arrival and departure codes; one failed decoder",
			fields{
				DepartureDivertedAirportCode: "WWW",
				DepartureDivertedAirportID:   0,
				ArrivalDivertedAirportCode:   "WWW",
				ArrivalDivertedAirportID:     0,
			},
			args{[]stationDecoder{func(string) (int32, bool) {
				return -1, false
			}}},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			fs := &FlightStatus{
				DepartureDivertedAirportCode: tt.fields.DepartureDivertedAirportCode,
				ArrivalDivertedAirportCode:   tt.fields.ArrivalDivertedAirportCode,
			}
			fs.FillDivertedAirportIDs(tt.args.stationDecoders...)
			assert.Equal(t, fs.ArrivalDivertedAirportID, tt.fields.ArrivalDivertedAirportID)
			assert.Equal(t, fs.DepartureDivertedAirportID, tt.fields.DepartureDivertedAirportID)
		})
	}
}
