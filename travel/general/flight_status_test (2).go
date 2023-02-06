package flightstatus

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/appconst"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/flightdata"
	"a.yandex-team.ru/travel/avia/shared_flights/api/pkg/structs"
)

func TestGetFlightStatusText_NoData(t *testing.T) {
	now := time.Date(2019, 12, 15, 12, 0, 0, 0, time.UTC)
	departure, arrival, global := getFlightStatusText(nil, nil, now)
	assert.Equal(t, appconst.FlightStatusUnknown, departure)
	assert.Equal(t, appconst.FlightStatusUnknown, arrival)
	assert.Equal(t, appconst.FlightStatusUnknown, global)
}

func TestGetFlightStatusText_HalfCancelled(t *testing.T) {
	now := time.Date(2019, 12, 15, 12, 0, 0, 0, time.UTC)
	leg := flightdata.FlightData{
		FlightDataBase: &flightdata.FlightDataBase{
			FlightStatus: &structs.FlightStatus{DepartureStatus: string(appconst.FlightStatusCancelled)},
		},
	}
	departure, arrival, global := getFlightStatusText(&leg, nil, now)

	assert.Equal(t, appconst.FlightStatusCancelled, departure)
	assert.Equal(t, appconst.FlightStatusUnknown, arrival)
	assert.Equal(t, appconst.FlightStatusCancelled, global)

	leg = flightdata.FlightData{
		FlightDataBase: &flightdata.FlightDataBase{
			FlightStatus: &structs.FlightStatus{DepartureStatus: string(appconst.FlightStatusCancelled), DepartureSourceIsTrusted: true},
		},
	}
	departure, arrival, global = getFlightStatusText(&leg, nil, now)

	assert.Equal(t, appconst.FlightStatusCancelled, departure)
	assert.Equal(t, appconst.FlightStatusCancelled, arrival)
	assert.Equal(t, appconst.FlightStatusCancelled, global)

	leg = flightdata.FlightData{
		FlightDataBase: &flightdata.FlightDataBase{
			FlightStatus: &structs.FlightStatus{ArrivalStatus: string(appconst.FlightStatusCancelled)},
		},
	}
	departure, arrival, global = getFlightStatusText(nil, &leg, now)

	assert.Equal(t, appconst.FlightStatusUnknown, departure)
	assert.Equal(t, appconst.FlightStatusCancelled, arrival)
	assert.Equal(t, appconst.FlightStatusCancelled, global)

	leg = flightdata.FlightData{
		FlightDataBase: &flightdata.FlightDataBase{
			FlightStatus: &structs.FlightStatus{ArrivalStatus: string(appconst.FlightStatusCancelled), ArrivalSourceIsTrusted: true},
		},
	}
	departure, arrival, global = getFlightStatusText(nil, &leg, now)

	assert.Equal(t, appconst.FlightStatusCancelled, departure)
	assert.Equal(t, appconst.FlightStatusCancelled, arrival)
	assert.Equal(t, appconst.FlightStatusCancelled, global)

}

func Test_calculateHalfOfFlightStatus(t *testing.T) {
	type args struct {
		scheduled time.Time
		actual    time.Time
		now       time.Time
		cancelled bool
	}
	tests := []struct {
		name string
		args args
		want appconst.FlightStatusText
	}{
		{
			"cancelled",
			args{time.Time{}, time.Time{}, time.Time{}, true},
			appconst.FlightStatusCancelled,
		},
		{
			"empty scheduled and actual time",
			args{time.Time{}, time.Time{}, time.Time{}, false},
			appconst.FlightStatusUnknown,
		},
		{
			"on time in future",
			args{
				time.Date(2020, 1, 1, 12, 0, 0, 0, time.UTC),
				time.Date(2020, 1, 1, 12, 0, 0, 0, time.UTC),
				time.Date(2020, 1, 1, 11, 0, 0, 0, time.UTC),
				false,
			},
			appconst.FlightStatusOnTime,
		},
		{
			"on time in past",
			args{
				time.Date(2020, 1, 1, 12, 0, 0, 0, time.UTC),
				time.Date(2020, 1, 1, 12, 0, 0, 0, time.UTC),
				time.Date(2020, 1, 1, 13, 0, 0, 0, time.UTC),
				false,
			},
			appconst.FlightStatusFinal,
		},
		{
			"early in future",
			args{
				time.Date(2020, 1, 1, 12, 0, 0, 0, time.UTC),
				time.Date(2020, 1, 1, 11, 0, 0, 0, time.UTC),
				time.Date(2020, 1, 1, 10, 0, 0, 0, time.UTC),
				false,
			},
			appconst.FlightStatusEarly,
		},
		{
			"on time within 15 minute bound in future before scheduled",
			args{
				time.Date(2020, 1, 1, 12, 0, 0, 0, time.UTC),
				time.Date(2020, 1, 1, 11, 45, 0, 0, time.UTC),
				time.Date(2020, 1, 1, 10, 0, 0, 0, time.UTC),
				false,
			},
			appconst.FlightStatusOnTime,
		},
		{
			"on time within 15 minute bound in future after scheduled",
			args{
				time.Date(2020, 1, 1, 12, 0, 0, 0, time.UTC),
				time.Date(2020, 1, 1, 12, 15, 0, 0, time.UTC),
				time.Date(2020, 1, 1, 10, 0, 0, 0, time.UTC),
				false,
			},
			appconst.FlightStatusOnTime,
		},
		{
			"delayed in future",
			args{
				time.Date(2020, 1, 1, 12, 0, 0, 0, time.UTC),
				time.Date(2020, 1, 1, 14, 0, 0, 0, time.UTC),
				time.Date(2020, 1, 1, 10, 0, 0, 0, time.UTC),
				false,
			},
			appconst.FlightStatusDelayed,
		},
		{
			"final",
			args{
				time.Date(2020, 1, 1, 12, 0, 0, 0, time.UTC),
				time.Date(2020, 1, 1, 14, 0, 0, 0, time.UTC),
				time.Date(2020, 1, 1, 14, 0, 0, 0, time.UTC),
				false,
			},
			appconst.FlightStatusFinal,
		},
		{
			"now << actual << scheduled = EARLY",
			args{
				time.Date(2020, 2, 2, 12, 0, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 10, 0, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 8, 0, 0, 0, time.UTC),
				false,
			},
			appconst.FlightStatusEarly,
		},
		{
			"now << scheduled << actual = DELAYED",
			args{
				time.Date(2020, 2, 2, 10, 0, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 12, 0, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 8, 0, 0, 0, time.UTC),
				false,
			},
			appconst.FlightStatusDelayed,
		},
		{
			"actual << now << scheduled = FINAL",
			args{
				time.Date(2020, 2, 2, 12, 0, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 8, 0, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 10, 0, 0, 0, time.UTC),
				false,
			},
			appconst.FlightStatusFinal,
		},
		{
			"actual << scheduled << now = FINAL",
			args{
				time.Date(2020, 2, 2, 10, 0, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 8, 0, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 12, 0, 0, 0, time.UTC),
				false,
			},
			appconst.FlightStatusFinal,
		},
		{
			"scheduled << now << actual = DELAYED",
			args{
				time.Date(2020, 2, 2, 8, 0, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 12, 0, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 10, 0, 0, 0, time.UTC),
				false,
			},
			appconst.FlightStatusDelayed,
		},
		{
			"scheduled << actual << now = FINAL",
			args{
				time.Date(2020, 2, 2, 8, 0, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 10, 0, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 12, 0, 0, 0, time.UTC),
				false,
			},
			appconst.FlightStatusFinal,
		},

		{
			"now < actual < scheduled = ONTIME",
			args{
				time.Date(2020, 2, 2, 12, 3, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 12, 2, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 12, 1, 0, 0, time.UTC),
				false,
			},
			appconst.FlightStatusOnTime,
		},
		{
			"now < scheduled < actual = ONTIME",
			args{
				time.Date(2020, 2, 2, 12, 2, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 12, 3, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 12, 1, 0, 0, time.UTC),
				false,
			},
			appconst.FlightStatusOnTime,
		},
		{
			"actual < now < scheduled = FINAL",
			args{
				time.Date(2020, 2, 2, 12, 3, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 12, 1, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 12, 2, 0, 0, time.UTC),
				false,
			},
			appconst.FlightStatusFinal,
		},
		{
			"actual < scheduled < now = FINAL",
			args{
				time.Date(2020, 2, 2, 12, 2, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 12, 1, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 12, 3, 0, 0, time.UTC),
				false,
			},
			appconst.FlightStatusFinal,
		},
		{
			"scheduled < now < actual = ONTIME",
			args{
				time.Date(2020, 2, 2, 12, 2, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 12, 3, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 12, 1, 0, 0, time.UTC),
				false,
			},
			appconst.FlightStatusOnTime,
		},
		{
			"scheduled < actual < now = FINAL",
			args{
				time.Date(2020, 2, 2, 12, 1, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 12, 2, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 12, 3, 0, 0, time.UTC),
				false,
			},
			appconst.FlightStatusFinal,
		},
		//
		{
			"scheduled = actual < now = FINAL",
			args{
				time.Date(2020, 2, 2, 12, 1, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 12, 1, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 12, 3, 0, 0, time.UTC),
				false,
			},
			appconst.FlightStatusFinal,
		},
		{
			"now < scheduled = actual = ONTIME",
			args{
				time.Date(2020, 2, 2, 12, 3, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 12, 3, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 12, 1, 0, 0, time.UTC),
				false,
			},
			appconst.FlightStatusOnTime,
		},
		//

		{
			"Cancelled is always cancelled",
			args{
				time.Date(2020, 2, 2, 12, 1, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 12, 2, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 12, 3, 0, 0, time.UTC),
				true,
			},
			appconst.FlightStatusCancelled,
		},

		// Zero time means unknown
		{
			"scheduled is undefined = unknown",
			args{
				time.Time{},
				time.Date(2020, 2, 2, 12, 2, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 12, 3, 0, 0, time.UTC),
				false,
			},
			appconst.FlightStatusUnknown,
		},
		{
			"actual is undefined = unknown",
			args{
				time.Date(2020, 2, 2, 12, 1, 0, 0, time.UTC),
				time.Time{},
				time.Date(2020, 2, 2, 12, 3, 0, 0, time.UTC),
				false,
			},
			appconst.FlightStatusUnknown,
		},
		{
			"now is undefined = unknown",
			args{
				time.Date(2020, 2, 2, 12, 1, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 12, 2, 0, 0, time.UTC),
				time.Time{},
				false,
			},
			appconst.FlightStatusUnknown,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := calculateHalfOfFlightStatus(tt.args.scheduled, tt.args.actual, tt.args.now, tt.args.cancelled); got != tt.want {
				t.Errorf("calculateHalfOfFlightStatus() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_calculateGlobalFlightStatus(t *testing.T) {
	type args struct {
		departureStatus    appconst.FlightStatusText
		arrivalStatus      appconst.FlightStatusText
		now                time.Time
		scheduledDeparture time.Time
		actualDeparture    time.Time
		scheduledArrival   time.Time
		actualArrival      time.Time
	}
	tests := []struct {
		name string
		args args
		want appconst.FlightStatusText
	}{
		{
			"departure cancel = global cancel",
			args{
				appconst.FlightStatusCancelled,
				"dont care",
				time.Date(2020, 2, 2, 12, 0, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 12, 0, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 12, 0, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 12, 0, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 12, 0, 0, 0, time.UTC),
			},
			appconst.FlightStatusCancelled,
		},
		{
			"arrival cancel = global cancel",
			args{
				"dont care",
				appconst.FlightStatusCancelled,
				time.Date(2020, 2, 2, 12, 0, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 12, 0, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 12, 0, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 12, 0, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 12, 0, 0, 0, time.UTC),
			},
			appconst.FlightStatusCancelled,
		},

		// zero values = unknown
		{
			"now is undefined = unknown",
			args{
				"dont care",
				"dont care",
				time.Time{},
				time.Date(2020, 2, 2, 12, 0, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 12, 0, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 12, 0, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 12, 0, 0, 0, time.UTC),
			},
			appconst.FlightStatusUnknown,
		},
		{
			"no actual info = unknown",
			args{
				"dont care",
				"dont care",
				time.Date(2020, 2, 2, 12, 0, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 12, 0, 0, 0, time.UTC),
				time.Time{},
				time.Date(2020, 2, 2, 12, 0, 0, 0, time.UTC),
				time.Time{},
			},
			appconst.FlightStatusUnknown,
		},
		//
		{
			"before departure, early departure",
			args{
				"dont care",
				"dont care",
				time.Date(2020, 2, 2, 12, 0, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 14, 0, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 13, 0, 0, 0, time.UTC),
				time.Time{},
				time.Time{},
			},
			appconst.FlightStatusEarly,
		},
		{
			"before departure, on-time departure",
			args{
				"dont care",
				"dont care",
				time.Date(2020, 2, 2, 12, 0, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 13, 0, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 13, 0, 0, 0, time.UTC),
				time.Time{},
				time.Time{},
			},
			appconst.FlightStatusOnTime,
		},
		{
			"before departure, delayed departure",
			args{
				"dont care",
				"dont care",
				time.Date(2020, 2, 2, 12, 0, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 13, 0, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 14, 0, 0, 0, time.UTC),
				time.Time{},
				time.Time{},
			},
			appconst.FlightStatusDelayed,
		},
		{
			"no departure info, before arrival, early arrival",
			args{
				"dont care",
				"dont care",
				time.Date(2020, 2, 2, 12, 0, 0, 0, time.UTC),
				time.Time{},
				time.Time{},
				time.Date(2020, 2, 2, 14, 0, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 13, 0, 0, 0, time.UTC),
			},
			appconst.FlightStatusOnTime,
		},
		{
			"after departure, before arrival, early arrival",
			args{
				"dont care",
				"dont care",
				time.Date(2020, 2, 2, 12, 0, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 10, 0, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 11, 0, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 14, 0, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 13, 0, 0, 0, time.UTC),
			},
			appconst.FlightStatusOnTime,
		},
		{
			"no departure info, before arrival, on-time arrival",
			args{
				"dont care",
				"dont care",
				time.Date(2020, 2, 2, 12, 0, 0, 0, time.UTC),
				time.Time{},
				time.Time{},
				time.Date(2020, 2, 2, 14, 0, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 14, 0, 0, 0, time.UTC),
			},
			appconst.FlightStatusOnTime,
		},
		{
			"no departure info, before arrival, delayed arrival",
			args{
				"dont care",
				"dont care",
				time.Date(2020, 2, 2, 12, 0, 0, 0, time.UTC),
				time.Time{},
				time.Time{},
				time.Date(2020, 2, 2, 14, 0, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 15, 0, 0, 0, time.UTC),
			},
			appconst.FlightStatusDelayed,
		},
		{
			"no departure info, after arrival",
			args{
				"dont care",
				"dont care",
				time.Date(2020, 2, 2, 16, 0, 0, 0, time.UTC),
				time.Time{},
				time.Time{},
				time.Date(2020, 2, 2, 14, 0, 0, 0, time.UTC),
				time.Date(2020, 2, 2, 15, 0, 0, 0, time.UTC),
			},
			appconst.FlightStatusArrived,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := calculateGlobalFlightStatus(tt.args.departureStatus, tt.args.arrivalStatus, tt.args.now, tt.args.scheduledDeparture, tt.args.actualDeparture, tt.args.scheduledArrival, tt.args.actualArrival); got != tt.want {
				t.Errorf("calculateGlobalFlightStatus() = %v, want %v", got, tt.want)
			}
		})
	}
}
