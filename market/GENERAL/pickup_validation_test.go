package lite

import (
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/test/requirepb"
	"a.yandex-team.ru/market/combinator/pkg/daysoff"
	"a.yandex-team.ru/market/combinator/pkg/enums"
	"a.yandex-team.ru/market/combinator/pkg/graph"
	pb "a.yandex-team.ru/market/combinator/proto/grpc"
)

func checkSkipValidation(
	t *testing.T,
	env *Env,
	graphEx *graph.Example1,
	firstDeliveryDay int,
	rearr string,
	hasRoot bool,
) {
	req := createPickupRequestGrouped(
		time.Date(2020, 6, 2, 2, 4, 5, 0, time.UTC),
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

	// FF + CD (pickup, delivery route)
	checkNoOptions(
		firstDeliveryDay,
		pb.DeliveryType_PICKUP,
		t,
		env,
		graphEx,
		time.Date(2020, 6, 2, 2, 4, 5, 0, time.UTC),
	)
	req2 := createDropshipRouteRequest(
		graphEx.WarehouseDSviaSC.PartnerLmsID,
		uint32(firstDeliveryDay),
		pb.DeliveryType_PICKUP,
		time.Date(2020, 6, 2, 2, 4, 5, 0, time.UTC),
	)
	resp2, err := env.Client.GetDeliveryRoute(env.Ctx, req2)
	if !hasRoot {
		require.Error(t, err)
		return
	}
	require.NoError(t, err)
	checkResult(
		t,
		graphEx,
		resp2,
		time.Date(2020, 6, 10, 13, 59, 5, 0, time.UTC), // выбрали максимально сжатый маршрут (в данном случае на 12 часов)
		time.Date(2020, 6, 10, 14, 0, 5, 0, time.UTC),
		time.Date(2020, 6, 10, 14, 1, 5, 0, time.UTC),
		time.Date(2020, 6, 11, 7, 0, 0, 0, time.UTC),
		&pb.Date{Day: uint32(firstDeliveryDay), Month: 6, Year: 2020},
		1,
	)
}

func TestGetPickupPointsGroupedSkipValidation(t *testing.T) {
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

	shipmentSvc := graphEx.Linehaul.FindPickupServiceByCode(enums.ServiceShipment)
	require.NotNil(t, shipmentSvc)

	genData.DisabledDatesHashed.DaysOffGrouped[shipmentSvc.ID] = daysoff.NewDaysOffGroupedFromStrings([]string{"2020-06-10"})
	genData.AddServiceDaysOff(genData.DisabledDatesHashed)

	env, cancel := NewEnv(t, genData, nil)
	defer cancel()

	// firstDeliveryDay := 8 // Available in options, unavailable in delivery_route
	// checkSkipValidation(t, env, graphEx, firstDeliveryDay, "", false)

	firstDeliveryDay := 11 // Available in options and available in delivery_route
	checkSkipValidation(t, env, graphEx, firstDeliveryDay, "", true)
}
