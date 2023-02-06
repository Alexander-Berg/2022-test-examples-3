package models

import (
	"testing"

	"a.yandex-team.ru/travel/komod/trips/internal/orders"
	ordercommons "a.yandex-team.ru/travel/orders/proto"
)

func TestTrip_CanceledByUser(t1 *testing.T) {
	tests := []struct {
		name string
		trip Trip
		want bool
	}{
		{
			name: "empty trip",
			trip: Trip{
				OrderInfos: map[orders.ID]OrderInfo{},
			},
			want: true,
		},
		{
			name: "one cancelled-by-user order",
			trip: Trip{
				OrderInfos: map[orders.ID]OrderInfo{
					"1": {State: ordercommons.EDisplayOrderState_OS_REFUNDED},
				},
			},
			want: true,
		},
		{
			name: "trip contains both types of orders",
			trip: Trip{
				OrderInfos: map[orders.ID]OrderInfo{
					"1": {State: ordercommons.EDisplayOrderState_OS_REFUNDED},
					"2": {State: ordercommons.EDisplayOrderState_OS_FULFILLED},
				},
			},
			want: false,
		},
		{
			name: "trip contains only cancelled-by-user orders",
			trip: Trip{
				OrderInfos: map[orders.ID]OrderInfo{
					"1": {State: ordercommons.EDisplayOrderState_OS_REFUNDED},
					"2": {State: ordercommons.EDisplayOrderState_OS_REFUNDED},
				},
			},
			want: true,
		},
		{
			name: "trip contains only active orders",
			trip: Trip{
				OrderInfos: map[orders.ID]OrderInfo{
					"1": {State: ordercommons.EDisplayOrderState_OS_FULFILLED},
					"2": {State: ordercommons.EDisplayOrderState_OS_FULFILLED},
				},
			},
			want: false,
		},
	}
	for _, tt := range tests {
		t1.Run(tt.name, func(t1 *testing.T) {
			if got := tt.trip.Cancelled(); got != tt.want {
				t1.Errorf("Cancelled() = %v, want %v", got, tt.want)
			}
		})
	}
}
