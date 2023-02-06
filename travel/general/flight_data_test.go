package flightdata

import (
	"reflect"
	"testing"
	"time"

	"a.yandex-team.ru/travel/avia/shared_flights/api/pkg/structs"
	"a.yandex-team.ru/travel/avia/shared_flights/lib/go/dtutil"
)

type mockTimezoneProvider struct{}

func (m *mockTimezoneProvider) GetTimeZoneByStationID(int64) *time.Location {
	return time.UTC
}

func TestFlightData_ActualArrival(t *testing.T) {
	type fields struct {
		FlightBase          structs.FlightBase
		FlightPattern       *structs.FlightPattern
		FlightStatus        *structs.FlightStatus
		FlightDepartureDate dtutil.IntDate
	}
	type args struct {
		tzProvider TimezoneProvider
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   time.Time
	}{
		{
			"no status",
			fields{
				FlightBase: structs.FlightBase{},
			},
			args{&mockTimezoneProvider{}},
			time.Time{},
		},
		{
			"status with valid datetime",
			fields{
				FlightBase: structs.FlightBase{},
				FlightStatus: &structs.FlightStatus{
					ArrivalTimeActual: "2020-02-03 04:05:06",
				},
			},
			args{&mockTimezoneProvider{}},
			time.Date(2020, 2, 3, 4, 5, 6, 0, time.UTC),
		},
		{
			"status with valid datetime invalid format",
			fields{
				FlightBase: structs.FlightBase{},
				FlightStatus: &structs.FlightStatus{
					ArrivalTimeActual: "20200203040506",
				},
			},
			args{&mockTimezoneProvider{}},
			time.Time{},
		},
		{
			"status with invalid datetime valid format",
			fields{
				FlightBase: structs.FlightBase{},
				FlightStatus: &structs.FlightStatus{
					ArrivalTimeActual: "2020-02-31 04:05:06",
				},
			},
			args{&mockTimezoneProvider{}},
			time.Time{},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			fd := &FlightData{
				FlightDataBase: &FlightDataBase{
					FlightBase:    tt.fields.FlightBase,
					FlightPattern: tt.fields.FlightPattern,
					FlightStatus:  tt.fields.FlightStatus,
					tzProvider:    tt.args.tzProvider,
				},
				FlightDepartureDate: tt.fields.FlightDepartureDate,
			}
			if got := fd.ActualArrival(); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("ActualArrival() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestFlightData_ActualDeparture(t *testing.T) {
	type fields struct {
		FlightBase          structs.FlightBase
		FlightPattern       *structs.FlightPattern
		FlightStatus        *structs.FlightStatus
		FlightDepartureDate dtutil.IntDate
	}
	type args struct {
		tzProvider TimezoneProvider
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   time.Time
	}{
		{
			"no status",
			fields{
				FlightBase: structs.FlightBase{},
			},
			args{&mockTimezoneProvider{}},
			time.Time{},
		},
		{
			"status with valid datetime",
			fields{
				FlightBase: structs.FlightBase{},
				FlightStatus: &structs.FlightStatus{
					DepartureTimeActual: "2020-02-03 04:05:06",
				},
			},
			args{&mockTimezoneProvider{}},
			time.Date(2020, 2, 3, 4, 5, 6, 0, time.UTC),
		},
		{
			"status with valid datetime invalid format",
			fields{
				FlightBase: structs.FlightBase{},
				FlightStatus: &structs.FlightStatus{
					DepartureTimeActual: "20200203040506",
				},
			},
			args{&mockTimezoneProvider{}},
			time.Time{},
		},
		{
			"status with invalid datetime valid format",
			fields{
				FlightBase: structs.FlightBase{},
				FlightStatus: &structs.FlightStatus{
					DepartureTimeActual: "2020-02-31 04:05:06",
				},
			},
			args{&mockTimezoneProvider{}},
			time.Time{},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			fd := &FlightData{
				FlightDataBase: &FlightDataBase{
					FlightBase:    tt.fields.FlightBase,
					FlightPattern: tt.fields.FlightPattern,
					FlightStatus:  tt.fields.FlightStatus,
					tzProvider:    tt.args.tzProvider,
				},
				FlightDepartureDate: tt.fields.FlightDepartureDate,
			}
			if got := fd.ActualDeparture(); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("ActualDeparture() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestFlightData_ScheduledArrival(t *testing.T) {
	type fields struct {
		FlightBase             structs.FlightBase
		FlightPattern          *structs.FlightPattern
		FlightStatus           *structs.FlightStatus
		FlightDepartureDate    dtutil.IntDate
		departureTimeScheduled time.Time
		actualDepartureTime    time.Time
		arrivalTimeScheduled   time.Time
		actualArrivalTime      time.Time
		departureTimezone      *time.Location
		arrivalTimezone        *time.Location
	}
	type args struct {
		tzProvider TimezoneProvider
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   time.Time
	}{
		{
			"valid scheduled arrival",
			fields{
				FlightBase: structs.FlightBase{
					DepartureTimeScheduled: 500,
					ArrivalTimeScheduled:   1234,
				},
				FlightDepartureDate: 20000101,
			},
			args{&mockTimezoneProvider{}},
			time.Date(2000, 1, 1, 12, 34, 0, 0, time.UTC),
		},
		{
			"arrives on next day",
			fields{
				FlightBase: structs.FlightBase{
					DepartureTimeScheduled: 1300,
					ArrivalTimeScheduled:   1234,
				},
				FlightDepartureDate: 20000101,
			},
			args{&mockTimezoneProvider{}},
			time.Date(2000, 1, 2, 12, 34, 0, 0, time.UTC),
		},
		{
			"timezone NPE",
			fields{
				FlightBase:          structs.FlightBase{},
				FlightDepartureDate: 20000101,
			},
			args{},
			time.Time{},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			fd := &FlightData{
				FlightDataBase: &FlightDataBase{
					FlightBase:        tt.fields.FlightBase,
					FlightPattern:     tt.fields.FlightPattern,
					FlightStatus:      tt.fields.FlightStatus,
					departureTimezone: tt.fields.departureTimezone,
					arrivalTimezone:   tt.fields.arrivalTimezone,
					tzProvider:        tt.args.tzProvider,
				},
				FlightDepartureDate:    tt.fields.FlightDepartureDate,
				departureTimeScheduled: tt.fields.departureTimeScheduled,
				actualDepartureTime:    tt.fields.actualDepartureTime,
				arrivalTimeScheduled:   tt.fields.arrivalTimeScheduled,
				actualArrivalTime:      tt.fields.actualArrivalTime,
			}
			if got := fd.ScheduledArrival(); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("ScheduledArrival() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestFlightData_ScheduledDeparture(t *testing.T) {
	type fields struct {
		FlightBase          structs.FlightBase
		FlightPattern       *structs.FlightPattern
		FlightStatus        *structs.FlightStatus
		FlightDepartureDate dtutil.IntDate
	}
	type args struct {
		tzProvider TimezoneProvider
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   time.Time
	}{
		{
			"valid scheduled departure",
			fields{
				FlightBase: structs.FlightBase{
					DepartureTimeScheduled: 500,
				},
				FlightDepartureDate: 20000101,
			},
			args{&mockTimezoneProvider{}},
			time.Date(2000, 1, 1, 5, 0, 0, 0, time.UTC),
		},
		{
			"timezone NPE",
			fields{
				FlightBase:          structs.FlightBase{},
				FlightDepartureDate: 20000101,
			},
			args{},
			time.Time{},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			fd := &FlightData{
				FlightDataBase: &FlightDataBase{
					FlightBase:    tt.fields.FlightBase,
					FlightPattern: tt.fields.FlightPattern,
					FlightStatus:  tt.fields.FlightStatus,
					tzProvider:    tt.args.tzProvider,
				},
				FlightDepartureDate: tt.fields.FlightDepartureDate,
			}
			if got := fd.ScheduledDeparture(); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("ScheduledDeparture() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestFlightData_getArrivalTimezone(t *testing.T) {
	type fields struct {
		FlightBase          structs.FlightBase
		FlightPattern       *structs.FlightPattern
		FlightStatus        *structs.FlightStatus
		FlightDepartureDate dtutil.IntDate
	}
	type args struct {
		tzProvider TimezoneProvider
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   *time.Location
	}{
		{
			"no flight base = nil",
			fields{
				FlightBase: structs.FlightBase{},
			},
			args{&mockTimezoneProvider{}},
			time.UTC,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			fd := &FlightData{
				FlightDataBase: &FlightDataBase{
					FlightBase:    tt.fields.FlightBase,
					FlightPattern: tt.fields.FlightPattern,
					FlightStatus:  tt.fields.FlightStatus,
					tzProvider:    tt.args.tzProvider,
				},
				FlightDepartureDate: tt.fields.FlightDepartureDate,
			}
			if got := fd.ArrivalTimezone(); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("ArrivalTimezone() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestFlightData_getDepartureTimezone(t *testing.T) {
	type fields struct {
		FlightBase          structs.FlightBase
		FlightPattern       *structs.FlightPattern
		FlightStatus        *structs.FlightStatus
		FlightDepartureDate dtutil.IntDate
	}
	type args struct {
		tzProvider TimezoneProvider
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   *time.Location
	}{
		{
			"no flight base = nil",
			fields{
				FlightBase: structs.FlightBase{},
			},
			args{&mockTimezoneProvider{}},
			time.UTC,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			fd := &FlightData{
				FlightDataBase: &FlightDataBase{
					FlightBase:    tt.fields.FlightBase,
					FlightPattern: tt.fields.FlightPattern,
					FlightStatus:  tt.fields.FlightStatus,
					tzProvider:    tt.args.tzProvider,
				},
				FlightDepartureDate: tt.fields.FlightDepartureDate,
			}
			if got := fd.DepartureTimezone(); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("DepartureTimezone() = %v, want %v", got, tt.want)
			}
		})
	}
}
