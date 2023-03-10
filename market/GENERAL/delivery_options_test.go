package routes

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/enums"
	"a.yandex-team.ru/market/combinator/pkg/graph"
	"a.yandex-team.ru/market/combinator/pkg/timex"
	cr "a.yandex-team.ru/market/combinator/proto/grpc"
)

func TestCalcVirtualBox(t *testing.T) {
	items := Items{
		&Item{
			DeliveryRequestItem: &cr.DeliveryRequestItem{
				Weight:     30,
				Dimensions: []uint32{3, 10, 31},
			},
			Count: 3,
		},
		&Item{
			DeliveryRequestItem: &cr.DeliveryRequestItem{
				Weight:     50,
				Dimensions: []uint32{5, 22, 23},
			},
			Count: 2,
		},
	}
	result := items.calcVirtualBox()

	require.Equal(t, uint32(3*30+2*50), result.Weight)
	require.Equal(t, [3]uint32{19, 22, 31}, result.Dimensions)
	require.Equal(t, uint32(72), result.DimSum)
}

func TestFindPickupShipmentInterval(t *testing.T) {
	{
		result, err := findPickupShipmentInterval(context.Background(), nil, nil, nil, time.Time{})
		require.Nil(t, result)
		require.Equal(t, ErrNoPickupShipmentInterval, err)
	}
	{
		scheduleShipment := graph.Schedule{
			Windows: [7][]graph.ScheduleWindow{},
		}
		scheduleShipment.Windows[1] = []graph.ScheduleWindow{
			{
				From: timex.DayTime{Hour: 10},
				To:   timex.DayTime{Hour: 12, Minute: 30},
			},
		}
		scheduleInbound := graph.Schedule{
			Windows: [7][]graph.ScheduleWindow{},
		}
		scheduleInbound.Windows[1] = []graph.ScheduleWindow{
			{
				From: timex.DayTime{Hour: 11},
				To:   timex.DayTime{Hour: 18},
			},
		}
		scheduleHanding := graph.Schedule{
			Windows: [7][]graph.ScheduleWindow{},
		}
		scheduleHanding.Windows[1] = []graph.ScheduleWindow{
			{
				From: timex.DayTime{Hour: 9},
				To:   timex.DayTime{Hour: 22},
			},
		}
		endTime := time.Date(2021, 8, 2, 10, 0, 0, 0, time.UTC)
		pb := graph.NewPathBuilder()
		pb.AddWarehouse()
		pb.AddMovement()
		linehaul := pb.AddLinehaul(
			pb.MakeShipmentService(
				pb.WithStartTime(endTime),
				pb.WithDuration(30),
				pb.WithSchedule(scheduleShipment),
			),
		)
		pickup := pb.AddPickup(
			pb.MakeInboundService(
				pb.WithSchedule(scheduleInbound),
			),
			pb.MakeHandingService(
				pb.WithSchedule(scheduleHanding),
			),
			pb.WithPointLmsID(111),
		)
		path := pb.GetSortablePath()
		shipmentService := pb.GetGraph().GetNodeByID(linehaul).FindPickupServiceByCode(enums.ServiceShipment)
		// ???????? ???????????????????? ???????????? ???? linehaul.shipment - ?????? ??????????????????
		{
			result, err := findPickupShipmentInterval(context.Background(), path, shipmentService, nil, endTime)
			require.Nil(t, result)
			require.Equal(t, ErrNoPickupShipmentInterval, err)
		}
		// ???????? ???????????????????? ???? linehaul.shipment ?? ???? pickup.inbound - ???????????????????? ???? ?? ?????????? ???????????? ????????
		{
			result, err := findPickupShipmentInterval(context.Background(), path, shipmentService, pb.GetGraph().GetNodeByID(pickup), endTime)
			require.NoError(t, err)
			require.Equal(t,
				&graph.Interval{
					From: timex.DayTime{Hour: 11},
					To:   timex.DayTime{Hour: 12, Minute: 30},
				},
				result,
			)
		}
		// linehaul.shipment ?? ???? pickup.inbound ?? ???????????? ??????
		{
			result, err := findPickupShipmentInterval(context.Background(), path, shipmentService, pb.GetGraph().GetNodeByID(pickup), endTime.Add(24*time.Hour))
			require.Equal(t, ErrShipmentInboundDateMismatch, err)
			require.Nil(t, result)
		}
		// ???????? ???????????????????? ???????????? ???? pickup.inbound - ?????????? ???????????????? pickup.Inbound
		{
			shipmentService.Schedule = nil
			result, err := findPickupShipmentInterval(context.Background(), path, shipmentService, pb.GetGraph().GetNodeByID(pickup), endTime)
			require.Equal(t,
				&graph.Interval{
					From: timex.DayTime{Hour: 11},
					To:   timex.DayTime{Hour: 18},
				},
				result,
			)
			require.Equal(t, ErrNoPickupShipmentInterval, err)
		}
		// ?????? ???????????????????? ???? ???? linehaul.shipment ???? ???? pickup.inbound - ?????? ??????????????????
		{
			pb.GetGraph().GetNodeByID(pickup).FindPickupServiceByCode(enums.ServiceInbound).Schedule = nil
			result, err := findPickupShipmentInterval(context.Background(), path, shipmentService, pb.GetGraph().GetNodeByID(pickup), endTime)
			require.Nil(t, result)
			require.Equal(t, ErrNoPickupShipmentInterval, err)
		}
		// ?????? ?????????????? ???????????????????? - ?????? ??????????????????
		{
			pb.GetGraph().GetNodeByID(pickup).FindPickupServiceByCode(enums.ServiceHanding).Schedule = nil
			result, err := findPickupShipmentInterval(context.Background(), path, shipmentService, pb.GetGraph().GetNodeByID(pickup), endTime)
			require.Nil(t, result)
			require.Equal(t, ErrNoPickupShipmentInterval, err)
		}
	}
}
