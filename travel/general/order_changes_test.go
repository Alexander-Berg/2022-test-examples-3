package orderchanges

import (
	"context"
	"fmt"
	"testing"

	"github.com/araddon/dateparse"
	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/library/go/core/log/nop"
	"a.yandex-team.ru/library/go/ptr"
	"a.yandex-team.ru/travel/notifier/internal/models"
	"a.yandex-team.ru/travel/notifier/internal/orderchanges"
	"a.yandex-team.ru/travel/notifier/internal/orders"
)

type onOrderInfo = func(context.Context, *orders.OrderInfo) orderchanges.OrderChangedResult

type testSubscriber struct {
	onOrderInfo onOrderInfo
}

type testOrderProvider struct {
}

func (t testOrderProvider) GetOrderInfoByID(ctx context.Context, s string) (*orders.OrderInfo, error) {
	return &orders.OrderInfo{
		ID:   s,
		Type: orders.OrderTypeHotel,
		Owner: &orders.UserInfo{
			PassportID: "1",
			Email:      "email",
		},
		HotelOrderItems: []*orders.HotelOrderItem{{
			HotelName:    "",
			HotelAddress: "",
			HotelPhone:   "",
			CheckInDate:  ptr.Time(dateparse.MustParse("2022-02-01")),
			CheckOutDate: ptr.Time(dateparse.MustParse("2022-03-01")),
			GeoRegions: []*orders.GeoRegion{{
				GeoID: 54,
				Type:  6,
			}},
		}},
	}, nil
}

func (t testOrderProvider) Check(ctx context.Context, order models.Order) (bool, error) {
	return true, nil
}

func newTestSubscriber(onOrderInfo onOrderInfo) *testSubscriber {
	return &testSubscriber{onOrderInfo: onOrderInfo}
}

func (s *testSubscriber) OnOrderChanged(ctx context.Context, orderInfo *orders.OrderInfo) orderchanges.OrderChangedResult {
	return s.onOrderInfo(ctx, orderInfo)
}

type testOrderRepository struct {
}

func (t testOrderRepository) GetByCorrelationID(ctx context.Context, correlationID string) ([]models.Order, error) {
	//TODO implement me
	panic("implement me")
}

func (t testOrderRepository) GetByID(ctx context.Context, orderID string) (models.Order, error) {
	//TODO implement me
	panic("implement me")
}

func (t testOrderRepository) Upsert(ctx context.Context, order models.Order) error {
	return nil
}

func (t testOrderRepository) GetOrderInfoByID(ctx context.Context, id string) (*orders.OrderInfo, error) {
	return &orders.OrderInfo{ID: id}, nil
}

func TestService(t *testing.T) {
	logger := &nop.Logger{}

	ctx := context.Background()
	orderProvider := testOrderProvider{}
	orderRepository := testOrderRepository{}
	okSubscriber := newTestSubscriber(
		func(context.Context, *orders.OrderInfo) orderchanges.OrderChangedResult {
			return orderchanges.OrderChangedResult{Status: orderchanges.OrderChangedStatusOK}
		},
	)
	tempErrorSubscriber := newTestSubscriber(
		func(context.Context, *orders.OrderInfo) orderchanges.OrderChangedResult {
			return orderchanges.OrderChangedResult{Status: orderchanges.OrderChangedStatusTemporaryError, Message: "temporary error"}
		},
	)
	failureSubscriber := newTestSubscriber(
		func(context.Context, *orders.OrderInfo) orderchanges.OrderChangedResult {
			return orderchanges.OrderChangedResult{Status: orderchanges.OrderChangedStatusFailure, Message: "failure"}
		},
	)
	panicSubscriber := newTestSubscriber(
		func(context.Context, *orders.OrderInfo) orderchanges.OrderChangedResult {
			panic(fmt.Errorf("some panic happened"))
		},
	)

	t.Run(
		"notifyAll/no subscribers returns OK", func(t *testing.T) {
			s := NewService(logger, false, orderProvider, orderRepository, orderProvider)

			result := s.notifyAll(ctx, orderchanges.OrderChangedRequest{OrderID: "1"})

			assert.Equal(t, orderchanges.OrderChangedStatusOK, result.Status)
		},
	)

	t.Run(
		"notifyAll/single OK-subscriber returns OK", func(t *testing.T) {
			s := NewService(logger, false, orderProvider, orderRepository, orderProvider, okSubscriber)

			result := s.notifyAll(ctx, orderchanges.OrderChangedRequest{OrderID: "1"})

			assert.Equal(t, orderchanges.OrderChangedStatusOK, result.Status)
		},
	)

	t.Run(
		"notifyAll/panics are handled and cause TemporaryError status", func(t *testing.T) {
			s := NewService(logger, false, orderProvider, orderRepository, orderProvider, okSubscriber, panicSubscriber)

			result := s.notifyAll(ctx, orderchanges.OrderChangedRequest{OrderID: "1"})

			assert.Equal(t, orderchanges.OrderChangedStatusTemporaryError, result.Status)
			assert.Contains(t, result.Message, "some panic happened")
		},
	)

	t.Run(
		"notifyAll/multiple subscribers returns the worst status with all messages", func(t *testing.T) {
			s := NewService(logger, false, orderProvider, orderRepository, orderProvider, okSubscriber, tempErrorSubscriber, panicSubscriber, failureSubscriber)

			result := s.notifyAll(ctx, orderchanges.OrderChangedRequest{OrderID: "1"})

			assert.Equal(t, orderchanges.OrderChangedStatusFailure, result.Status)
			assert.Contains(t, result.Message, "temporary error")
			assert.Contains(t, result.Message, "some panic happened")
			assert.Contains(t, result.Message, "failure")
		},
	)
}
