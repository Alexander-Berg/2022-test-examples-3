package handler

import (
	"context"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"google.golang.org/grpc/metadata"

	"a.yandex-team.ru/library/go/core/log/nop"
	commonApi "a.yandex-team.ru/travel/app/backend/api/commonorder/v1"
	"a.yandex-team.ru/travel/app/backend/internal/avia"
	"a.yandex-team.ru/travel/app/backend/internal/l10n"
	"a.yandex-team.ru/travel/app/backend/internal/lib/travelapiclient/models"
)

type travelAPIClientMock struct {
	mock.Mock
}

func (c *travelAPIClientMock) GetAviaOrderStatus(ctx context.Context, orderID string) (*models.GetAviaOrderStatusRsp, error) {
	args := c.Called(ctx, orderID)
	return args.Get(0).(*models.GetAviaOrderStatusRsp), args.Error(1)
}

func (c *travelAPIClientMock) GetAviaOrder(ctx context.Context, orderID string) (*models.GetAviaOrderRsp, error) {
	args := c.Called(ctx, orderID)
	return args.Get(0).(*models.GetAviaOrderRsp), args.Error(1)
}

func (c *travelAPIClientMock) GetAviaHappyPage(ctx context.Context, orderID string) (*models.GetAviaHappyPageRsp, error) {
	args := c.Called(ctx, orderID)
	return args.Get(0).(*models.GetAviaHappyPageRsp), args.Error(1)
}

func (c *travelAPIClientMock) AviaStartPayment(ctx context.Context, orderID string) error {
	args := c.Called(ctx, orderID)
	return args.Error(0)
}

type l10nServiceForTests struct{}

func (*l10nServiceForTests) Get(keyset string, language string) (*l10n.Keyset, error) {
	if keyset != l10nKeyset || language != "ru" {
		panic("implement me")
	}
	return &l10n.Keyset{
		Name:     l10nKeyset,
		Tag:      "l10n-tag",
		Language: "ru",
		Keys: map[string]string{
			string(defaultWaitingTitleKey):   "default waiting title",
			string(defaultWaitingMessageKey): "default waiting message",
			string(preparingOrderTitleKey):   "preparing order title",
			string(preparingOrderMessageKey): "preparing order message",
		},
	}, nil
}

func TestGetExpectedAction(t *testing.T) {
	ctx := prepareLanguageContext(context.Background(), "ru-RU")
	tcMock := new(travelAPIClientMock)
	orderID := "5b28b2a0-d632-4a55-93c4-ad3444474df9"
	tcMock.On("GetAviaOrderStatus", ctx, orderID).Return(&models.GetAviaOrderStatusRsp{
		DisplayOrderState: models.OrderStatusDisplayOsInProgress,
	}, nil)
	handler := NewGRPCAviaOrderHandler(&nop.Logger{}, tcMock, &l10nServiceForTests{}, &avia.OrderConfig{})

	response, err := handler.GetExpectedAction(ctx, &commonApi.GetExpectedActionReq{OrderId: orderID})

	require.NoError(t, err)
	assert.Equal(t, &commonApi.GetExpectedActionRsp_Waiting{
		Title:    "default waiting title",
		Subtitle: "default waiting message",
		DelayMs:  200,
	}, response.GetWaiting())
	tcMock.AssertExpectations(t)
}

func prepareLanguageContext(ctx context.Context, acceptLanguageValue string) context.Context {
	md := metadata.MD{
		"grpcgateway-accept-language": []string{acceptLanguageValue},
	}
	return metadata.NewIncomingContext(ctx, md)
}
