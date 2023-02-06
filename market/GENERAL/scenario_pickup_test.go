package lite

import (
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/test/requirepb"
	"a.yandex-team.ru/market/combinator/pkg/graph"
	pb "a.yandex-team.ru/market/combinator/proto/grpc"
)

func checkPickupRouteSearchWithGap(
	t *testing.T,
	env *Env,
	graphEx *graph.Example1,
	firstDeliveryDay int,
	rearr string,
) {
	req := createPickupRequestGrouped(
		time.Date(2020, 6, 1, 9, 0, 0, 0, time.UTC), // First hour with disabled date
		[]uint32{213},
		graphEx.WarehouseDSviaSC.PartnerLmsID,
		rearr,
	)
	resp, err := env.Client.GetPickupPointsGrouped(env.Ctx, req)
	require.NoError(t, err, "GetPickupPointsGrouped failed")
	require.Len(t, resp.Groups, 1)

	group := resp.Groups[0]
	require.Len(t, group.Points, 1)
	requirepb.Equal(t, []pb.PaymentMethod{pb.PaymentMethod_PREPAYMENT, pb.PaymentMethod_CARD}, group.PaymentMethods)

	requirepb.Equal(
		t,
		&pb.Date{Year: 2020, Month: 6, Day: uint32(firstDeliveryDay)},
		group.DateFrom,
	)
	requirepb.Equal(
		t,
		&pb.Date{Year: 2020, Month: 6, Day: uint32(firstDeliveryDay)},
		group.DateTo,
	)

	req2 := createDropshipRouteRequest(
		graphEx.WarehouseDSviaSC.PartnerLmsID,
		uint32(2), // We have option for the 1 date
		pb.DeliveryType_COURIER,
		time.Date(2020, 6, 1, 9, 0, 0, 0, time.UTC), // First hour with disabled date
	)
	resp2, err := env.Client.GetDeliveryRoute(env.Ctx, req2)
	require.NoError(t, err)
	checkResult(
		t,
		graphEx,
		resp2,
		time.Date(2020, 6, 1, 8, 55, 0, 0, time.UTC),
		time.Date(2020, 6, 1, 8, 56, 0, 0, time.UTC),
		time.Date(2020, 6, 1, 8, 57, 0, 0, time.UTC),
		time.Date(2020, 6, 2, 7, 0, 0, 0, time.UTC),
		&pb.Date{Day: uint32(2), Month: 6, Year: 2020},
		1,
	)
	req3 := createDropshipRouteRequest(
		graphEx.WarehouseDSviaSC.PartnerLmsID,
		uint32(firstDeliveryDay),
		pb.DeliveryType_PICKUP,
		time.Date(2020, 6, 1, 9, 0, 0, 0, time.UTC), // First hour with disabled date
	)
	resp3, err := env.Client.GetDeliveryRoute(env.Ctx, req3)
	require.NoError(t, err)
	checkResult(
		t,
		graphEx,
		resp3,
		time.Date(2020, 6, 9, 20, 55, 0, 0, time.UTC), // выбрали максимально сжатый маршрут (в данном случае на 12 часов)
		time.Date(2020, 6, 9, 20, 56, 0, 0, time.UTC),
		time.Date(2020, 6, 9, 20, 57, 0, 0, time.UTC),
		time.Date(2020, 6, 10, 7, 0, 0, 0, time.UTC),
		&pb.Date{Day: uint32(firstDeliveryDay), Month: 6, Year: 2020},
		1,
	)
}

func TestPickupScenarioWithGap(t *testing.T) {
	/*
		Синхронизация логики валидации привоза в pickup
	*/
	movementDisabledDates := []string{
		"2020-06-01",
		"2020-06-02",
		"2020-06-03",
		"2020-06-04",
		"2020-06-06",
		"2020-06-07",
		"2020-06-08",
	}
	movementSchedule, _ := graph.CreateAroundTheClockSchedule(false)
	disbledStart := time.Date(2020, 6, 3, 5, 5, 5, 0, time.UTC)
	warehouseDisabledDates := createDisabledDatesSlice(disbledStart, 1)
	warehouseSchedule, _ := graph.CreateAroundTheClockSchedule(false)

	genData, graphEx := createGenData(warehouseSchedule, movementSchedule, nil, true, warehouseDisabledDates, movementDisabledDates, nil)

	env, cancel := NewEnv(t, genData, nil)
	defer cancel()

	firstDeliveryDay := 10 // Available in options and available in delivery_route
	checkPickupRouteSearchWithGap(t, env, graphEx, firstDeliveryDay, "")
}
