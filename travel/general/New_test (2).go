package legcache

import (
	"reflect"
	"testing"

	"go.uber.org/atomic"

	"a.yandex-team.ru/library/go/core/xerrors"
	"a.yandex-team.ru/travel/avia/shared_flights/lib/go/direction"
	"a.yandex-team.ru/travel/avia/shared_flights/lib/go/dtutil"
	"a.yandex-team.ru/travel/avia/shared_flights/status_importer/internal/objects/model"
)

type FlightLegProviderMock struct {
	mockFlightLeg func(
		carrierID int64, flightNumber string, flightDay dtutil.StringDate, direction direction.Direction, stationID model.StationID,
	) (int16, dtutil.StringDate, error)
}

func (m *FlightLegProviderMock) FlightLeg(carrierID int64, flightNumber string, flightDay dtutil.StringDate, direction direction.Direction, stationID model.StationID) (leg int16, departureDate dtutil.StringDate, err error) {
	return m.mockFlightLeg(carrierID, flightNumber, flightDay, direction, stationID)
}

func TestCache_FlightLeg(t *testing.T) {
	type args struct {
		carrierID    int64
		flightNumber string
		flightDay    dtutil.StringDate
		direction    direction.Direction
		stationID    model.StationID
	}
	var desired int16 = 16

	first := atomic.NewBool(true)
	mockFlightLegDB := FlightLegProviderMock{
		mockFlightLeg: func(int64, string, dtutil.StringDate, direction.Direction, model.StationID) (int16, dtutil.StringDate, error) {
			if first.Swap(false) {
				return desired, "", nil
			} else {
				return 0, "", xerrors.New("second call detected")
			}
		},
	}
	tests := []struct {
		name    string
		args    args
		want    int16
		wantErr bool
	}{
		{
			name: "First query should trigger a database",
			args: args{
				carrierID:    26,
				flightNumber: "6543",
				flightDay:    "2019-10-20",
				direction:    direction.DEPARTURE,
				stationID:    12345,
			},
			want:    desired,
			wantErr: false,
		},
		{
			name: "Second query should not trigger database",
			args: args{
				carrierID:    26,
				flightNumber: "6543",
				flightDay:    "2019-10-20",
				direction:    direction.DEPARTURE,
				stationID:    12345,
			},
			want:    desired,
			wantErr: false,
		},
	}
	c := New(&Config{
		FlightLegProvider: &mockFlightLegDB,
	})

	for _, tt := range tests {
		got, _, err := c.FlightLeg(tt.args.carrierID, tt.args.flightNumber, tt.args.flightDay, tt.args.direction, tt.args.stationID)
		if (err != nil) != tt.wantErr {
			t.Errorf("FlightLeg() error = %v, wantErr %v", err, tt.wantErr)
			return
		}
		if !reflect.DeepEqual(got, tt.want) {
			t.Errorf("FlightLeg() got = %v, want %v", got, tt.want)
		}
	}
}

func TestCache_putFlightLeg(t *testing.T) {
	var desired int16 = 16
	c := New(&Config{
		FlightLegProvider: &FlightLegProviderMock{
			mockFlightLeg: func(int64, string, dtutil.StringDate, direction.Direction, model.StationID,
			) (int16, dtutil.StringDate, error) {
				return desired, "", nil
			},
		},
	})

	var leg int16 = 435
	var getItem cacheItem
	var ok bool

	c.putFlightLegBatch(
		26, "123", "2019-10-12", direction.DEPARTURE, 12345,
		cacheItem{leg: leg},
	)
	c.putFlightLegOverlay(
		27, "124", "2019-10-12", direction.DEPARTURE, 12345,
		cacheItem{leg: leg},
	)
	getItem, ok = c.flightLeg(26, "123", "2019-10-12", direction.DEPARTURE, 12345)

	if !ok {
		t.Fail()
	}
	if getItem.leg != leg {
		t.Fail()
	}
	getItem, ok = c.flightLeg(27, "124", "2019-10-12", direction.DEPARTURE, 12345)

	if !ok {
		t.Fail()
	}
	if getItem.leg != leg {
		t.Fail()
	}
}
