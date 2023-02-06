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

func checkHandingSkipValidation(
	t *testing.T,
	env *Env,
	graphEx *graph.Example1,
	firstDeliveryDay int,
	hasRoot bool,
) {
	req := createDropshipRouteRequest(
		graphEx.WarehouseDSviaSC.PartnerLmsID,
		uint32(firstDeliveryDay),
		pb.DeliveryType_COURIER,
		time.Date(2020, 6, 2, 2, 4, 5, 0, time.UTC),
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

	checkNoOptions(
		firstDeliveryDay,
		pb.DeliveryType_COURIER,
		t,
		env,
		graphEx,
		time.Date(2020, 6, 2, 2, 4, 5, 0, time.UTC),
	)

	resp2, err := env.Client.GetDeliveryRoute(env.Ctx, req)
	if !hasRoot {
		require.Error(t, err)
		return
	}
	require.NoError(t, err)
	checkResult(
		t,
		graphEx,
		resp2,
		time.Date(2020, 6, 6, 1, 59, 5, 0, time.UTC),
		time.Date(2020, 6, 6, 2, 0, 5, 0, time.UTC),
		time.Date(2020, 6, 6, 2, 1, 5, 0, time.UTC),
		time.Date(2020, 6, 7, 7, 0, 0, 0, time.UTC),
		&pb.Date{Day: uint32(firstDeliveryDay), Month: 6, Year: 2020},
		1,
	)
}

func TestHandingSkipValidation(t *testing.T) {
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

	shipmentSvc := graphEx.Linehaul.FindCourierServiceByCode(enums.ServiceShipment)
	require.NotNil(t, shipmentSvc)

	genData.DisabledDatesHashed.DaysOffGrouped[shipmentSvc.ID] = daysoff.NewDaysOffGroupedFromStrings([]string{"2020-06-06"})
	genData.AddServiceDaysOff(genData.DisabledDatesHashed)

	env, cancel := NewEnv(t, genData, nil)
	defer cancel()

	firstDeliveryDay := 7
	checkHandingSkipValidation(t, env, graphEx, firstDeliveryDay, true)
}
