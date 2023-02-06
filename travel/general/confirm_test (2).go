package app

import (
	"context"
	"fmt"
	"testing"

	"a.yandex-team.ru/library/go/test/assertpb"
	tpb "a.yandex-team.ru/travel/proto"
	"github.com/stretchr/testify/assert"
	uzap "go.uber.org/zap"

	"a.yandex-team.ru/travel/buses/backend/internal/common/connector/mock"
	"a.yandex-team.ru/travel/buses/backend/internal/common/dict"
	wpb "a.yandex-team.ru/travel/buses/backend/proto/worker"
)

func TestApp_Confirm(t *testing.T) {
	var (
		orderID     = "orderID"
		supplierID  = dict.GetSuppliersList()[0]
		order       = &wpb.TOrder{Id: orderID}
		explanation = &wpb.TExplanation{ConnectorResponseCode: 213}
	)

	t.Run("ok", func(t *testing.T) {
		app, teardownApp, err := NewTestApp(t, nil, nil)
		assert.NoError(t, err)
		defer teardownApp()

		clientMock, teardownClientMock := mock.SetupClientMock()
		defer teardownClientMock()

		clientMock.On("PostConfirm", orderID).Return(order, explanation, nil)
		response, err := app.Confirm(context.Background(), &wpb.TConfirmRequest{
			OrderId:    orderID,
			SupplierId: supplierID,
		})
		clientMock.AssertExpectations(t)

		if assert.NoError(t, err) {
			assertpb.Equal(t, &wpb.TConfirmResponse{
				Header: &wpb.TResponseHeader{
					Code:        tpb.EErrorCode_EC_OK,
					Explanation: explanation,
				},
				Order: order,
			}, response)
		}
	})

	t.Run("NewClient error", func(t *testing.T) {
		logger, logs := setupLogsCapture()
		app, teardownApp, err := NewTestApp(t, nil, logger)
		assert.NoError(t, err)
		defer teardownApp()

		clientMock, teardownClientMock := mock.SetupClientMock()
		defer teardownClientMock()

		var (
			testSupplierID uint32 = 1000
			expectedError         = "NewClientWithTransport: no such supplier with id = 1000"
		)

		response, err := app.Confirm(context.Background(), &wpb.TConfirmRequest{
			SupplierId: testSupplierID,
			OrderId:    orderID,
		})
		clientMock.AssertExpectations(t)

		if assert.NoError(t, err) {
			logEntries := logs.TakeAll()
			if assert.Equal(t, 1, len(logEntries)) {
				assert.Equal(t, uzap.ErrorLevel, logEntries[0].Level)
				assert.Equal(t, fmt.Sprintf("App.Confirm: %s", expectedError), logEntries[0].Message)
			}

			assertpb.Equal(t, &wpb.TConfirmResponse{
				Header: &wpb.TResponseHeader{
					Code: tpb.EErrorCode_EC_GENERAL_ERROR,
					Error: &tpb.TError{
						Code:    tpb.EErrorCode_EC_GENERAL_ERROR,
						Message: expectedError,
					},
				},
			}, response)
		}
	})

	for _, testCase := range errorTestCases {
		t.Run(fmt.Sprintf("PostConfirm %s", testCase.Name), func(t *testing.T) {
			logger, logs := setupLogsCapture()
			app, teardownApp, err := NewTestApp(t, nil, logger)
			assert.NoError(t, err)
			defer teardownApp()

			clientMock, teardownClientMock := mock.SetupClientMock()
			defer teardownClientMock()

			var testOrder = &wpb.TOrder{}

			clientMock.On("PostConfirm", orderID).Return(
				testOrder, testCase.ExpectedHeader.Explanation, testCase.Error)
			response, err := app.Confirm(context.Background(), &wpb.TConfirmRequest{
				SupplierId: supplierID,
				OrderId:    orderID,
			})
			clientMock.AssertExpectations(t)

			if assert.NoError(t, err) {
				logEntries := logs.TakeAll()
				if assert.Equal(t, 1, len(logEntries)) {
					assert.Equal(t, uzap.ErrorLevel, logEntries[0].Level)
					assert.Equal(t, fmt.Sprintf("App.Confirm: %s", testCase.Error.Error()), logEntries[0].Message)
				}

				assertpb.Equal(t, &wpb.TConfirmResponse{Header: testCase.ExpectedHeader}, response)
			}
		})
	}
}
