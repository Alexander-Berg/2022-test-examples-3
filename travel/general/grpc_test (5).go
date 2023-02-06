package handler

import (
	"context"
	"net/http"
	"testing"

	"github.com/golang/protobuf/ptypes/wrappers"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/metadata"
	"google.golang.org/grpc/status"

	"a.yandex-team.ru/library/go/core/log/nop"
	commonApi "a.yandex-team.ru/travel/app/backend/api/commonorder/v1"
	"a.yandex-team.ru/travel/app/backend/internal/common"
	"a.yandex-team.ru/travel/app/backend/internal/hotels"
	"a.yandex-team.ru/travel/app/backend/internal/l10n"
	"a.yandex-team.ru/travel/app/backend/internal/lib/clientscommon"
	"a.yandex-team.ru/travel/app/backend/internal/lib/travelapiclient/models"
)

type travelAPIClientMock struct {
	mock.Mock
}

func (c *travelAPIClientMock) CreateOrder(ctx context.Context, req *models.CreateOrderRequest) (*models.CreateOrderResponse, error) {
	panic("implement me")
}

func (c *travelAPIClientMock) GetOrder(ctx context.Context, orderID string) (*models.GetOrderRsp, error) {
	args := c.Called(ctx, orderID)
	return args.Get(0).(*models.GetOrderRsp), args.Error(1)
}

func (c *travelAPIClientMock) GetOrderByToken(ctx context.Context, request *models.GetOrderByTokenRequest) (*models.GetOrderByTokenResponse, error) {
	panic("implement me")
}

func (c *travelAPIClientMock) GetHotelHappyPage(ctx context.Context, orderID string) (*models.GetHotelHappyPageRsp, error) {
	args := c.Called(ctx, orderID)
	return args.Get(0).(*models.GetHotelHappyPageRsp), args.Error(1)
}

func (c *travelAPIClientMock) StartPayment(ctx context.Context, orderID string, paymentTestContextToken *string) error {
	args := c.Called(ctx, orderID, paymentTestContextToken)
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
			string(defaultWaitingTitleKey):        "default waiting title",
			string(defaultWaitingMessageKey):      "default waiting message",
			string(reservedWaitingTitleKey):       "reserved waiting title",
			string(reservedWaitingMessageKey):     "reserved waiting message",
			string(promocodeErrorAlertTitleKey):   "promocode error alert title",
			string(promocodeErrorAlertMessageKey): "promocode error alert message",
			string(paymentErrorAlertTitleKey):     "payment error alert title",
			string(paymentErrorAlertMessageKey):   "payment error alert message",
		},
	}, nil
}

func TestGetOrder_WithoutOrder(t *testing.T) {
	ctx := context.Background()
	tcMock := new(travelAPIClientMock)
	orderID := "5b28b2a0-d632-4a55-93c4-ad3444474df9"
	paymentURL := "https://trust-test.yandex.ru/web/payment?purchase_token=2e2acb3553a4449a77fed5997e402559"
	tcMock.On("GetOrder", ctx, orderID).Return(&models.GetOrderRsp{
		Status: models.OrderStatusInProgress,
		Payment: &models.Payment{
			Current: &models.CurrentPayment{
				PaymentURL: &paymentURL,
			},
		},
	}, nil)
	handler := NewGRPCHotelOrderHandler(&nop.Logger{}, common.TestingEnv, tcMock, nil, nil)

	response, err := handler.GetOrderStatus(ctx, &commonApi.GetOrderReq{OrderId: orderID})

	require.NoError(t, err)
	assert.Equal(t, &commonApi.GetOrderRsp{
		Status:        commonApi.OrderStatus_ORDER_STATUS_IN_PROGRESS,
		PurchaseToken: &wrappers.StringValue{Value: "2e2acb3553a4449a77fed5997e402559"},
	}, response)
	tcMock.AssertExpectations(t)
}

func TestGetOrder_NoPaymentURL(t *testing.T) {
	ctx := context.Background()
	tcMock := new(travelAPIClientMock)
	orderID := "5b28b2a0-d632-4a55-93c4-ad3444474df9"
	tcMock.On("GetOrder", ctx, orderID).Return(&models.GetOrderRsp{
		Status:  models.OrderStatusReserved,
		Payment: nil,
	}, nil)
	handler := NewGRPCHotelOrderHandler(&nop.Logger{}, common.TestingEnv, tcMock, nil, nil)

	response, err := handler.GetOrderStatus(ctx, &commonApi.GetOrderReq{OrderId: orderID})

	require.NoError(t, err)
	assert.Equal(t, &commonApi.GetOrderRsp{
		Status:        commonApi.OrderStatus_ORDER_STATUS_WAIT_START_PAYMENT,
		PurchaseToken: nil,
	}, response)
	tcMock.AssertExpectations(t)
}

func TestGetOrder_404(t *testing.T) {
	ctx := context.Background()
	tcMock := new(travelAPIClientMock)
	orderID := "5b28b2a0-d632-4a55-93c4-ad3444474df9"
	travelAPIClientError := clientscommon.StatusError{Status: http.StatusNotFound}
	expectedGRPCStatus := codes.NotFound

	tcMock.On("GetOrder", ctx, orderID).Return((*models.GetOrderRsp)(nil), travelAPIClientError)
	handler := NewGRPCHotelOrderHandler(&nop.Logger{}, common.TestingEnv, tcMock, nil, nil)

	_, err := handler.GetOrderStatus(ctx, &commonApi.GetOrderReq{OrderId: orderID})

	var res interface{ GRPCStatus() *status.Status }
	if assert.ErrorAs(t, err, &res) {
		assert.Equal(t, expectedGRPCStatus, res.GRPCStatus().Code())
	}
	tcMock.AssertExpectations(t)
}

var errorForStatuses = []int{http.StatusBadRequest, http.StatusUnauthorized, http.StatusForbidden, http.StatusNotFound} // TODO(adurenv) дописать

var orderID = "5b28b2a0-d632-4a55-93c4-ad3444474df9"
var token *string

func TestGetExpectedAction(t *testing.T) {
	ctx := prepareLanguageContext(context.Background(), "ru-RU")
	tcMock := new(travelAPIClientMock)
	tcMock.On("GetOrder", ctx, orderID).Return(&models.GetOrderRsp{
		Status: models.OrderStatusInProgress,
		Payment: &models.Payment{
			Current: &models.CurrentPayment{
				PaymentURL: nil,
			},
		},
	}, nil)
	handler := NewGRPCHotelOrderHandler(&nop.Logger{}, common.TestingEnv, tcMock, &l10nServiceForTests{}, &hotels.DefaultConfig.Order)

	response, err := handler.GetExpectedAction(ctx, &commonApi.GetExpectedActionReq{OrderId: orderID})

	require.NoError(t, err)
	assert.Equal(t, &commonApi.GetExpectedActionRsp_Waiting{
		Title:    "default waiting title",
		Subtitle: "default waiting message",
		DelayMs:  1000,
	}, response.GetWaiting())
	tcMock.AssertExpectations(t)
}

func TestGetExpectedAction_Confirmed(t *testing.T) {
	ctx := prepareLanguageContext(context.Background(), "ru-RU")
	tcMock := new(travelAPIClientMock)
	tcMock.On("GetOrder", ctx, orderID).Return(&models.GetOrderRsp{
		Status: models.OrderStatusConfirmed,
		Payment: &models.Payment{
			Current: nil,
		},
	}, nil)
	tcMock.On("GetHotelHappyPage", ctx, orderID).Return(&models.GetHotelHappyPageRsp{
		//TODO(adurnev) fill and assert
	}, nil)
	handler := NewGRPCHotelOrderHandler(&nop.Logger{}, common.TestingEnv, tcMock, &l10nServiceForTests{}, &hotels.DefaultConfig.Order)

	_, err := handler.GetExpectedAction(ctx, &commonApi.GetExpectedActionReq{OrderId: orderID})

	require.NoError(t, err)
}

func TestGetExpectedAction_GetOrderFailed_Waiting(t *testing.T) {
	ctx := prepareLanguageContext(context.Background(), "ru-RU")
	tcMock := new(travelAPIClientMock)

	for _, status := range waitingForStatuses {
		tcMock.On("GetOrder", ctx, orderID).Return(&models.GetOrderRsp{}, clientscommon.StatusError{
			Status:      status,
			Response:    nil,
			ResponseRaw: "",
		})
		handler := NewGRPCHotelOrderHandler(&nop.Logger{}, common.TestingEnv, tcMock, &l10nServiceForTests{}, &hotels.DefaultConfig.Order)

		action, err := handler.GetExpectedAction(ctx, &commonApi.GetExpectedActionReq{OrderId: orderID})
		require.NoError(t, err)

		if _, ok := action.ExpectedAction.(*commonApi.GetExpectedActionRsp_Waiting_); !ok {
			t.Error("error")
		}
	}
}

func TestGetExpectedAction_GetOrderFailed_Error(t *testing.T) {
	ctx := prepareLanguageContext(context.Background(), "ru-RU")
	tcMock := new(travelAPIClientMock)

	for _, status := range errorForStatuses {
		tcMock.On("GetOrder", ctx, orderID).Return(&models.GetOrderRsp{}, clientscommon.StatusError{
			Status:      status,
			Response:    nil,
			ResponseRaw: "",
		})
		handler := NewGRPCHotelOrderHandler(&nop.Logger{}, common.TestingEnv, tcMock, &l10nServiceForTests{}, &hotels.DefaultConfig.Order)

		_, err := handler.GetExpectedAction(ctx, &commonApi.GetExpectedActionReq{OrderId: orderID})

		require.Error(t, err)
	}
}

var orderStatusForHappyPage = []models.OrderStatus{models.OrderStatusConfirmed, models.OrderStatusRefunded, models.OrderStatusCancelled, models.OrderStatusCancelledWithRefund, models.OrderStatusFailed, models.OrderStatusRefundFailed, models.OrderStatusPaymentFailed}

func TestGetExpectedAction_GetHotelHappyPageFailed_Waiting(t *testing.T) {
	ctx := prepareLanguageContext(context.Background(), "ru-RU")
	tcMock := new(travelAPIClientMock)

	for _, orderStatus := range orderStatusForHappyPage {
		tcMock.On("GetOrder", ctx, orderID).Return(&models.GetOrderRsp{
			Status: orderStatus,
			Payment: &models.Payment{
				Current: nil,
			},
		}, nil)

		for _, hpStatus := range waitingForStatuses {
			tcMock.On("GetHotelHappyPage", ctx, orderID).Return(&models.GetHotelHappyPageRsp{}, clientscommon.StatusError{
				Status:      hpStatus,
				Response:    nil,
				ResponseRaw: "",
			})
			handler := NewGRPCHotelOrderHandler(&nop.Logger{}, common.TestingEnv, tcMock, &l10nServiceForTests{}, &hotels.DefaultConfig.Order)

			action, err := handler.GetExpectedAction(ctx, &commonApi.GetExpectedActionReq{OrderId: orderID})
			require.NoError(t, err)

			if _, ok := action.ExpectedAction.(*commonApi.GetExpectedActionRsp_Waiting_); !ok {
				t.Errorf("orderStatus %v getHappyPageStatus %v", orderStatus, hpStatus)
			}
		}
	}
}

func TestGetExpectedAction_GetHotelHappyPageFailed_Error(t *testing.T) {
	ctx := prepareLanguageContext(context.Background(), "ru-RU")
	tcMock := new(travelAPIClientMock)

	for _, orderStatus := range orderStatusForHappyPage {
		tcMock.On("GetOrder", ctx, orderID).Return(&models.GetOrderRsp{
			Status: orderStatus,
			Payment: &models.Payment{
				Current: nil,
			},
		}, nil)

		for _, hpStatus := range errorForStatuses {
			tcMock.On("GetHotelHappyPage", ctx, orderID).Return(&models.GetHotelHappyPageRsp{}, clientscommon.StatusError{
				Status:      hpStatus,
				Response:    nil,
				ResponseRaw: "",
			})
			handler := NewGRPCHotelOrderHandler(&nop.Logger{}, common.TestingEnv, tcMock, &l10nServiceForTests{}, &hotels.DefaultConfig.Order)

			_, err := handler.GetExpectedAction(ctx, &commonApi.GetExpectedActionReq{OrderId: orderID})
			require.Error(t, err)
		}
	}
}

var waitingForStatusesWithConflict = []int{http.StatusConflict, http.StatusRequestTimeout, http.StatusInternalServerError, http.StatusBadGateway, http.StatusServiceUnavailable, http.StatusGatewayTimeout}

func TestGetExpectedAction_ReservedAndStartPaymentFailed_Waiting(t *testing.T) {
	ctx := prepareLanguageContext(context.Background(), "ru-RU")
	tcMock := new(travelAPIClientMock)

	order := models.GetOrderRsp{
		Status:  models.OrderStatusReserved,
		Payment: nil,
	}

	tcMock.On("GetOrder", ctx, orderID).Return(&order, nil)

	for _, hpStatus := range waitingForStatusesWithConflict {
		tcMock.On("StartPayment", ctx, orderID, token).Return(clientscommon.StatusError{
			Status:      hpStatus,
			Response:    nil,
			ResponseRaw: "",
		})
		handler := NewGRPCHotelOrderHandler(&nop.Logger{}, common.TestingEnv, tcMock, &l10nServiceForTests{}, &hotels.DefaultConfig.Order)

		action, err := handler.GetExpectedAction(ctx, &commonApi.GetExpectedActionReq{OrderId: orderID})
		require.NoError(t, err)

		if _, ok := action.ExpectedAction.(*commonApi.GetExpectedActionRsp_Waiting_); !ok {
			t.Errorf("order %v startPaymentStatus %v", order, hpStatus)
		}
	}
}

func TestGetExpectedAction_ReservedAndStartPaymentFailed_Error(t *testing.T) {
	ctx := prepareLanguageContext(context.Background(), "ru-RU")
	tcMock := new(travelAPIClientMock)

	order := models.GetOrderRsp{
		Status:  models.OrderStatusReserved,
		Payment: nil,
	}
	tcMock.On("GetOrder", ctx, orderID).Return(&order, nil)

	for _, hpStatus := range errorForStatuses {
		tcMock.On("StartPayment", ctx, orderID, token).Return(clientscommon.StatusError{
			Status:      hpStatus,
			Response:    nil,
			ResponseRaw: "",
		})
		handler := NewGRPCHotelOrderHandler(&nop.Logger{}, common.TestingEnv, tcMock, &l10nServiceForTests{}, &hotels.DefaultConfig.Order)

		_, err := handler.GetExpectedAction(ctx, &commonApi.GetExpectedActionReq{OrderId: orderID})
		t.Logf("%v %v", order, hpStatus)
		require.Error(t, err)
	}
}

func TestGetExpectedAction_ReservedWithRestrictionsAndStartPaymentFailed_Waiting(t *testing.T) {
	ctx := prepareLanguageContext(context.Background(), "ru-RU")
	tcMock := new(travelAPIClientMock)

	order := models.GetOrderRsp{
		Status: models.OrderStatusReservedWithRestrictions,
		Payment: &models.Payment{
			Current:    nil,
			AmountPaid: models.Price{},
			Error:      nil,
		},
	} //commonAPI.Command_COMMAND_START_PAYMENT_WITH_RESTRICTIONS

	tcMock.On("GetOrder", ctx, orderID).Return(&order, nil)

	for _, hpStatus := range waitingForStatusesWithConflict {
		tcMock.On("StartPayment", ctx, orderID, token).Return(clientscommon.StatusError{
			Status:      hpStatus,
			Response:    nil,
			ResponseRaw: "",
		})
		handler := NewGRPCHotelOrderHandler(&nop.Logger{}, common.TestingEnv, tcMock, &l10nServiceForTests{}, &hotels.DefaultConfig.Order)

		action, err := handler.GetExpectedAction(ctx, &commonApi.GetExpectedActionReq{OrderId: orderID, Command: &commonApi.GetExpectedActionReq_Payment{
			Payment: &commonApi.CommandPayment{
				Command:                 commonApi.Command_COMMAND_START_PAYMENT_WITH_RESTRICTIONS,
				PaymentTestContextToken: nil,
			},
		}})
		require.NoError(t, err)

		if _, ok := action.ExpectedAction.(*commonApi.GetExpectedActionRsp_Waiting_); !ok {
			t.Errorf("order %v startPaymentStatus %v", order, hpStatus)
		}
	}
}

func TestGetExpectedAction_ReservedWithRestrictionsAndStartPaymentFailed_Error(t *testing.T) {
	ctx := prepareLanguageContext(context.Background(), "ru-RU")
	tcMock := new(travelAPIClientMock)

	order := models.GetOrderRsp{
		Status:  models.OrderStatusReservedWithRestrictions,
		Payment: nil,
	}
	tcMock.On("GetOrder", ctx, orderID).Return(&order, nil)

	for _, hpStatus := range errorForStatuses {
		tcMock.On("StartPayment", ctx, orderID, token).Return(clientscommon.StatusError{
			Status:      hpStatus,
			Response:    nil,
			ResponseRaw: "",
		})
		handler := NewGRPCHotelOrderHandler(&nop.Logger{}, common.TestingEnv, tcMock, &l10nServiceForTests{}, &hotels.DefaultConfig.Order)

		_, err := handler.GetExpectedAction(ctx, &commonApi.GetExpectedActionReq{OrderId: orderID, Command: &commonApi.GetExpectedActionReq_Payment{
			Payment: &commonApi.CommandPayment{
				Command:                 commonApi.Command_COMMAND_START_PAYMENT_WITH_RESTRICTIONS,
				PaymentTestContextToken: nil,
			},
		}})
		t.Logf("%v %v", order, hpStatus)
		require.Error(t, err)
	}
}

var orderForStartPayment = []*models.GetOrderRsp{
	{
		Status:  models.OrderStatusPaymentFailed,
		Payment: nil,
	},
	{
		Status: models.OrderStatusPaymentFailed,
		Payment: &models.Payment{
			Error: nil,
		},
	},
	{
		Status: models.OrderStatusPaymentFailed,
		Payment: &models.Payment{
			Error: &models.PaymentError{
				Code: models.PaymentErrorCodeExpiredCard,
			},
		},
	},
}

func TestGetExpectedAction_PaymentFailedAndStartPaymentFailed_Waiting(t *testing.T) {
	ctx := prepareLanguageContext(context.Background(), "ru-RU")
	tcMock := new(travelAPIClientMock)

	for _, order := range orderForStartPayment {
		tcMock.On("GetOrder", ctx, orderID).Return(order, nil)

		for _, hpStatus := range waitingForStatusesWithConflict {
			tcMock.On("StartPayment", ctx, orderID, token).Return(clientscommon.StatusError{
				Status:      hpStatus,
				Response:    nil,
				ResponseRaw: "",
			})
			handler := NewGRPCHotelOrderHandler(&nop.Logger{}, common.TestingEnv, tcMock, &l10nServiceForTests{}, &hotels.DefaultConfig.Order)

			action, err := handler.GetExpectedAction(ctx, &commonApi.GetExpectedActionReq{OrderId: orderID})
			require.NoError(t, err)

			if _, ok := action.ExpectedAction.(*commonApi.GetExpectedActionRsp_Waiting_); !ok {
				t.Errorf("order %v startPaymentStatus %v", order, hpStatus)
			}
		}
	}
}

func TestGetExpectedAction_PaymentFailedAndStartPaymentFailed_Error(t *testing.T) {
	ctx := prepareLanguageContext(context.Background(), "ru-RU")
	tcMock := new(travelAPIClientMock)

	for _, order := range orderForStartPayment {
		tcMock.On("GetOrder", ctx, orderID).Return(order, nil)

		for _, hpStatus := range errorForStatuses {
			tcMock.On("StartPayment", ctx, orderID, token).Return(clientscommon.StatusError{
				Status:      hpStatus,
				Response:    nil,
				ResponseRaw: "",
			})
			handler := NewGRPCHotelOrderHandler(&nop.Logger{}, common.TestingEnv, tcMock, &l10nServiceForTests{}, &hotels.DefaultConfig.Order)

			_, err := handler.GetExpectedAction(ctx, &commonApi.GetExpectedActionReq{OrderId: orderID})
			t.Logf("%v %v", order, hpStatus)
			require.Error(t, err)
		}
	}
}

func prepareLanguageContext(ctx context.Context, acceptLanguageValue string) context.Context {
	md := metadata.MD{
		"grpcgateway-accept-language": []string{acceptLanguageValue},
	}
	return metadata.NewIncomingContext(ctx, md)
}
