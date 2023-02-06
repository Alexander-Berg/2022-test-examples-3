package processor

import (
	"reflect"
	"testing"

	"a.yandex-team.ru/library/go/units"
	"a.yandex-team.ru/travel/komod/trips/internal/orders"
	"a.yandex-team.ru/travel/komod/trips/internal/testutils/builders"
	tripmodels "a.yandex-team.ru/travel/komod/trips/internal/trips/models"
)

func Test_combineTrips(t *testing.T) {
	tripBuilder := builders.NewTrip()

	orderID := "1"
	orderInfo1 := tripBuilder.Descriptive(1, "2021-12-11").
		RegisterOrder(orderID, false).
		FlyTo(2, 0, orderID).
		Build("tripID", "passportID").OrderInfos[orders.ID(orderID)]

	orderID = "2"
	orderInfo2 := tripBuilder.Descriptive(1, "2021-12-11").
		RegisterOrder(orderID, false).
		FlyTo(2, 0, orderID).
		Build("tripID", "passportID").OrderInfos[orders.ID(orderID)]

	orderID = "3"
	orderInfo3 := tripBuilder.Descriptive(3, "2021-12-11").
		RegisterOrder(orderID, false).
		FlyTo(4, 0, orderID).
		Build("tripID", "passportID").OrderInfos[orders.ID(orderID)]

	orderID = "4"
	orderInfo4 := tripBuilder.Descriptive(3, "2021-12-11").
		RegisterOrder(orderID, false).
		FlyTo(4, 3*units.Day, orderID).
		Build("tripID", "passportID").OrderInfos[orders.ID(orderID)]

	trip1 := tripmodels.NewTrip("1", "1")
	trip1.UpsertOrder(orderInfo2)

	trip2 := tripmodels.NewTrip("2", "2")
	trip2.UpsertOrder(orderInfo3)
	trip2.UpsertOrder(orderInfo4)

	type args struct {
		orderInfo    tripmodels.OrderInfo
		matchedTrips []*tripmodels.Trip
	}
	tests := []struct {
		name string
		args args
		want map[orders.ID]tripmodels.OrderInfo
	}{
		{
			name: "combine",
			args: args{
				orderInfo:    orderInfo1,
				matchedTrips: []*tripmodels.Trip{trip1, trip2},
			},
			want: map[orders.ID]tripmodels.OrderInfo{
				"1": orderInfo1,
				"2": orderInfo2,
				"3": orderInfo3,
				"4": orderInfo4,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := combineTrips(tt.args.orderInfo, "1", tt.args.matchedTrips...); !reflect.DeepEqual(got.OrderInfos, tt.want) {
				t.Errorf("combineTrips() = %v, want %v", got, tt.want)
			}
		})
	}
}
