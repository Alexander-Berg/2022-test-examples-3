package orders

import (
	"testing"
	"time"

	"github.com/golang/protobuf/proto"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/ptr"
	ordersproto "a.yandex-team.ru/travel/orders/proto/services/orders/basic_info/v1"
)

func TestOrderInfoMapper_ProcessOrderResponse(t *testing.T) {
	mapper := NewProtoToOrderInfoMapper()

	t.Run(
		"hotel order", func(t *testing.T) {
			rsp := ordersproto.BasicOrderInfo{}
			_ = proto.UnmarshalText(
				"id:\"1\" type:DT_HOTEL state:OS_FULFILLED hotel_order_items:{hotel_name:\"HN\" hotel_address:\"RU\" check_in_date:{Year:2020  Month:12  Day:18}}",
				&rsp,
			)
			expectedOrderInfo := &OrderInfo{
				ID:    "1",
				State: OrderStateFulfilled,
				Type:  OrderTypeHotel,
				HotelOrderItems: []*HotelOrderItem{
					{
						HotelName:    "HN",
						HotelAddress: "RU",
						CheckInDate:  ptr.Time(time.Date(2020, 12, 18, 0, 0, 0, 0, time.UTC)),
					},
				},
			}

			result, err := mapper.Map(&rsp)

			require.NoError(t, err)
			require.Equal(t, expectedOrderInfo, result)
		},
	)

	t.Run(
		"train order", func(t *testing.T) {
			rsp := ordersproto.BasicOrderInfo{}
			_ = proto.UnmarshalText(
				"id:\"1\" type:DT_TRAIN state:OS_FULFILLED train_order_items:{arrival_station:\"2\" departure_time:{seconds:1611265260}}",
				&rsp,
			)
			expectedOrderInfo := &OrderInfo{
				ID:    "1",
				State: OrderStateFulfilled,
				Type:  OrderTypeTrain,
				TrainOrderItems: []*TrainOrderItem{
					{
						ArrivalStation: "2",
						DepartureTime:  ptr.Time(time.Date(2021, 1, 21, 21, 41, 0, 0, time.UTC)),
					},
				},
			}

			result, err := mapper.Map(&rsp)

			require.NoError(t, err)
			require.Equal(t, expectedOrderInfo, result)
		},
	)

	t.Run(
		"correct avia order", func(t *testing.T) {
			rsp := ordersproto.BasicOrderInfo{}
			_ = proto.UnmarshalText(
				"id:\"1\" type:DT_AVIA avia_order_items:{ origin_destinations:{ departure_station: \"a\" arrival_station: \"b\" segments:{ departure_datetime:{seconds:1611265260} } } }",
				&rsp,
			)
			expectedOrderInfo := &OrderInfo{
				ID:    "1",
				State: OrderStateUnknown,
				Type:  OrderTypeAvia,
				AviaOrderItems: []*AviaOrderItem{
					{
						OriginDestinations: []*AviaOriginDestination{
							{
								DepartureStation: "a",
								ArrivalStation:   "b",
								Segments: []*AviaSegment{
									{
										DepartureDatetime: ptr.Time(time.Date(2021, 1, 21, 21, 41, 0, 0, time.UTC)),
									},
								},
							},
						},
					},
				},
			}

			result, err := mapper.Map(&rsp)

			require.NoError(t, err)
			require.Equal(t, expectedOrderInfo, result)
		},
	)
}
