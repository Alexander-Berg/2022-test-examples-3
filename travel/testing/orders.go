package testing

import (
	"context"

	"a.yandex-team.ru/travel/komod/trips/internal/orders"
)

type fakeClient struct {
	order orders.Order
}

func NewFakeClient(order orders.Order) *fakeClient {
	return &fakeClient{order: order}
}

func (c fakeClient) GetOrderNoAuth(ctx context.Context, orderID orders.ID) (orders.Order, error) {
	return c.order, nil
}

func (c fakeClient) GetOrdersByIDs(ctx context.Context, orderIDs ...orders.ID) ([]orders.Order, error) {
	//TODO implement me
	panic("implement me")
}

func (c fakeClient) GetUserOrdersWithoutExcluded(ctx context.Context, passportID string, excludedIDs ...orders.ID) ([]orders.Order, string, error) {
	//TODO implement me
	panic("implement me")
}

func (c fakeClient) GetUserOrdersWithoutExcludedNextPage(ctx context.Context, nextPageToken string) ([]orders.Order, string, error) {
	//TODO implement me
	panic("implement me")
}
