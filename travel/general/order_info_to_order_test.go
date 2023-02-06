package orders

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/library/go/ptr"
	"a.yandex-team.ru/travel/notifier/internal/models"
)

func TestOrderInfoToOrder(t *testing.T) {
	t.Run(
		"hotel order", func(t *testing.T) {
			orderInfo := &OrderInfo{
				ID:    "1",
				Type:  OrderTypeHotel,
				State: OrderStateUnknown,
				HotelOrderItems: []*HotelOrderItem{
					{
						HotelName:    "HN",
						HotelAddress: "RU",
						CheckInDate:  ptr.Time(time.Date(2020, 12, 18, 0, 0, 0, 0, time.UTC)),
					},
				},
			}
			expectedOrder := models.Order{
				ID:          "1",
				State:       models.OrderIncomplete,
				Type:        models.OrderHotel,
				StartDate:   time.Date(2020, 12, 18, 0, 0, 0, 0, time.UTC),
				ArrivalDate: time.Date(2020, 12, 18, 0, 0, 0, 0, time.UTC),
			}

			result, err := orderInfo.ToOrder()

			assert.NoError(t, err)
			assert.Equal(t, expectedOrder, result)
		},
	)

	t.Run(
		"train order", func(t *testing.T) {
			orderInfo := &OrderInfo{
				ID:    "1",
				Type:  OrderTypeTrain,
				State: OrderStateFulfilled,
				TrainOrderItems: []*TrainOrderItem{
					{
						ArrivalStation: "2",
						DepartureTime:  ptr.Time(time.Date(2021, 1, 21, 21, 41, 0, 0, time.UTC)),
						ArrivalTime:    ptr.Time(time.Date(2021, 1, 23, 21, 41, 0, 0, time.UTC)),
					},
				},
			}
			expectedOrder := models.Order{
				ID:           "1",
				State:        models.OrderFulfilled,
				Type:         models.OrderTrain,
				StartDate:    time.Date(2021, 1, 21, 21, 41, 0, 0, time.UTC),
				ArrivalDate:  time.Date(2021, 1, 23, 21, 41, 0, 0, time.UTC),
				WasFulfilled: true,
			}

			result, err := orderInfo.ToOrder()

			assert.NoError(t, err)
			assert.Equal(t, expectedOrder, result)
		},
	)

	t.Run(
		"unknown order type", func(t *testing.T) {
			orderInfo := &OrderInfo{
				ID:   "1",
				Type: OrderTypeBus,
			}

			_, err := orderInfo.ToOrder()

			assert.Error(t, err)
			var errUnknownOrderType ErrUnknownOrderType
			assert.ErrorAs(t, err, &errUnknownOrderType)
			assert.Contains(t, err.Error(), "unknown order type")
		},
	)

	t.Run(
		"nil hotel items", func(t *testing.T) {
			orderInfo := &OrderInfo{
				ID:   "1",
				Type: OrderTypeHotel,
			}

			_, err := orderInfo.ToOrder()

			assert.Error(t, err)
			var errInvalidOrder ErrInvalidOrder
			assert.ErrorAs(t, err, &errInvalidOrder)
			assert.Equal(t, models.OrderHotel, err.(ErrInvalidOrder).OrderType())
			assert.Contains(t, err.Error(), "no hotel order items")
		},
	)

	t.Run(
		"hotel item is nil", func(t *testing.T) {
			orderInfo := &OrderInfo{
				ID:              "1",
				Type:            OrderTypeHotel,
				HotelOrderItems: []*HotelOrderItem{nil},
			}

			_, err := orderInfo.ToOrder()

			assert.Error(t, err)
			var errInvalidOrder ErrInvalidOrder
			assert.ErrorAs(t, err, &errInvalidOrder)
			assert.Equal(t, models.OrderHotel, err.(ErrInvalidOrder).OrderType())
			assert.Contains(t, err.Error(), "first hotel order item is nil")
		},
	)

	t.Run(
		"nil train order items", func(t *testing.T) {
			orderInfo := &OrderInfo{
				ID:   "1",
				Type: OrderTypeTrain,
			}

			_, err := orderInfo.ToOrder()

			assert.Error(t, err)
			var errInvalidOrder ErrInvalidOrder
			assert.ErrorAs(t, err, &errInvalidOrder)
			assert.Equal(t, models.OrderTrain, err.(ErrInvalidOrder).OrderType())
			assert.Contains(t, err.Error(), "no train order items")
		},
	)

	t.Run(
		"train order item is nil", func(t *testing.T) {
			orderInfo := &OrderInfo{
				ID:              "1",
				Type:            OrderTypeTrain,
				TrainOrderItems: []*TrainOrderItem{nil},
			}

			_, err := orderInfo.ToOrder()

			assert.Error(t, err)
			var errInvalidOrder ErrInvalidOrder
			assert.ErrorAs(t, err, &errInvalidOrder)
			assert.Equal(t, models.OrderTrain, err.(ErrInvalidOrder).OrderType())
			assert.Contains(t, err.Error(), "first train order item is nil")
		},
	)

	t.Run(
		"no train departure time", func(t *testing.T) {
			orderInfo := &OrderInfo{
				ID:              "1",
				Type:            OrderTypeTrain,
				TrainOrderItems: []*TrainOrderItem{{ArrivalStation: "2", DepartureTime: nil}},
			}

			_, err := orderInfo.ToOrder()

			assert.Error(t, err)
			var errInvalidOrder ErrInvalidOrder
			assert.ErrorAs(t, err, &errInvalidOrder)
			assert.Equal(t, models.OrderTrain, err.(ErrInvalidOrder).OrderType())
			assert.Contains(t, err.Error(), "train order's item has no departure time")
		},
	)

	t.Run(
		"no avia items", func(t *testing.T) {
			orderInfo := &OrderInfo{
				ID:   "1",
				Type: OrderTypeAvia,
			}

			_, err := orderInfo.ToOrder()

			assert.Error(t, err)
			var errInvalidOrder ErrInvalidOrder
			assert.ErrorAs(t, err, &errInvalidOrder)
			assert.Equal(t, models.OrderAvia, err.(ErrInvalidOrder).OrderType())
			assert.Contains(t, err.Error(), "no avia order items")
		},
	)

	t.Run(
		"avia item is nil", func(t *testing.T) {
			orderInfo := &OrderInfo{
				ID:             "1",
				Type:           OrderTypeAvia,
				AviaOrderItems: []*AviaOrderItem{nil},
			}

			_, err := orderInfo.ToOrder()

			assert.Error(t, err)
			var errInvalidOrder ErrInvalidOrder
			assert.ErrorAs(t, err, &errInvalidOrder)
			assert.Equal(t, models.OrderAvia, err.(ErrInvalidOrder).OrderType())
			assert.Contains(t, err.Error(), "first avia order item is nil")
		},
	)

	t.Run(
		"no origin-destination entries in the avia order", func(t *testing.T) {
			orderInfo := &OrderInfo{
				ID:   "1",
				Type: OrderTypeAvia,
				AviaOrderItems: []*AviaOrderItem{
					{
						OriginDestinations: nil,
					},
				},
			}

			_, err := orderInfo.ToOrder()

			assert.Error(t, err)
			var errInvalidOrder ErrInvalidOrder
			assert.ErrorAs(t, err, &errInvalidOrder)
			assert.Equal(t, models.OrderAvia, err.(ErrInvalidOrder).OrderType())
			assert.Contains(t, err.Error(), "avia order has no origin-destination entries")
		},
	)

	t.Run(
		"avia order's origin-destination has no segments", func(t *testing.T) {
			orderInfo := &OrderInfo{
				ID:             "1",
				Type:           OrderTypeAvia,
				AviaOrderItems: []*AviaOrderItem{{OriginDestinations: []*AviaOriginDestination{{}}}},
			}

			_, err := orderInfo.ToOrder()

			assert.Error(t, err)
			var errInvalidOrder ErrInvalidOrder
			assert.ErrorAs(t, err, &errInvalidOrder)
			assert.Equal(t, models.OrderAvia, err.(ErrInvalidOrder).OrderType())
			assert.Contains(t, err.Error(), "avia order's origin-destination has no segments")
		},
	)

	t.Run(
		"avia order's first segment has no departure date/time", func(t *testing.T) {
			orderInfo := &OrderInfo{
				ID:             "1",
				Type:           OrderTypeAvia,
				AviaOrderItems: []*AviaOrderItem{{OriginDestinations: []*AviaOriginDestination{{Segments: []*AviaSegment{{}}}}}},
			}

			_, err := orderInfo.ToOrder()

			assert.Error(t, err)
			var errInvalidOrder ErrInvalidOrder
			assert.ErrorAs(t, err, &errInvalidOrder)
			assert.Equal(t, models.OrderAvia, err.(ErrInvalidOrder).OrderType())
			assert.Contains(t, err.Error(), "avia order's first segment has no departure date/time")
		},
	)

	t.Run(
		"correct avia order info", func(t *testing.T) {
			orderInfo := &OrderInfo{
				ID:   "1",
				Type: OrderTypeAvia,
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
									{
										ArrivalDatetime: ptr.Time(time.Date(2021, 1, 23, 21, 41, 0, 0, time.UTC)),
									},
								},
							},
						},
					},
				},
			}
			expectedOrder := models.Order{
				ID:          "1",
				State:       models.OrderIncomplete,
				Type:        models.OrderAvia,
				StartDate:   time.Date(2021, 1, 21, 21, 41, 0, 0, time.UTC),
				ArrivalDate: time.Date(2021, 1, 23, 21, 41, 0, 0, time.UTC),
			}

			result, err := orderInfo.ToOrder()

			assert.NoError(t, err)
			assert.Equal(t, expectedOrder, result)
		},
	)
}
