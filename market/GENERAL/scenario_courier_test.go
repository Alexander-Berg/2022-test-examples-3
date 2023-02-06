package lite

import (
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/test/requirepb"
	"a.yandex-team.ru/market/combinator/pkg/graph"
	pb "a.yandex-team.ru/market/combinator/proto/grpc"
)

func checkCourierRouteSearchWithGap(
	t *testing.T,
	env *Env,
	graphEx *graph.Example1,
	firstDeliveryDay int,
) {
	req := createDropshipRouteRequest(
		graphEx.WarehouseDSviaSC.PartnerLmsID,
		uint32(firstDeliveryDay),
		pb.DeliveryType_COURIER,
		time.Date(2020, 6, 1, 9, 0, 0, 0, time.UTC), // First hour with disabled date
	)
	resp, err := env.Client.GetCourierOptions(env.Ctx, req)
	require.NoError(t, err, "GetCourierOptions failed")
	require.Len(t, resp.Options, 6)

	opts := resp.Options[0]
	requirepb.Equal(t, []pb.PaymentMethod{pb.PaymentMethod_PREPAYMENT, pb.PaymentMethod_CASH, pb.PaymentMethod_CARD}, opts.PaymentMethods)

	requirepb.Equal(
		t,
		&pb.Date{Year: 2020, Month: 6, Day: uint32(firstDeliveryDay)},
		opts.DateFrom,
	)
	requirepb.Equal(
		t,
		&pb.Date{Year: 2020, Month: 6, Day: uint32(firstDeliveryDay)},
		opts.DateTo,
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

	resp3, err := env.Client.GetDeliveryRoute(env.Ctx, req)
	require.NoError(t, err)
	checkResult(
		t,
		graphEx,
		resp3,
		time.Date(2020, 6, 5, 20, 55, 0, 0, time.UTC),
		time.Date(2020, 6, 5, 20, 56, 0, 0, time.UTC),
		time.Date(2020, 6, 5, 20, 57, 0, 0, time.UTC),
		time.Date(2020, 6, 6, 7, 0, 0, 0, time.UTC),
		&pb.Date{Day: uint32(firstDeliveryDay), Month: 6, Year: 2020},
		1,
	)
}

func TestCourierScenarioWithGap(t *testing.T) {
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

	firstDeliveryDay := 6
	checkCourierRouteSearchWithGap(t, env, graphEx, firstDeliveryDay)
}
