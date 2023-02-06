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

func TestApp_Refund(t *testing.T) {
	var (
		ticketID   = "ticketID"
		supplierID = dict.GetSuppliersList()[0]
		refund     = &wpb.TRefund{
			Price: &tpb.TPrice{Currency: tpb.ECurrency_C_RUB, Amount: 10000},
		}
		explanation = &wpb.TExplanation{ConnectorResponseCode: 333}
	)

	t.Run("ok", func(t *testing.T) {
		app, teardownApp, err := NewTestApp(t, nil, nil)
		assert.NoError(t, err)
		defer teardownApp()

		clientMock, teardownClientMock := mock.SetupClientMock()
		defer teardownClientMock()

		clientMock.On("PostRefund", ticketID).Return(refund, explanation, nil)
		response, err := app.Refund(context.Background(), &wpb.TRefundRequest{
			SupplierId: supplierID,
			TicketId:   ticketID,
		})
		clientMock.AssertExpectations(t)

		if assert.NoError(t, err) {
			assertpb.Equal(t, &wpb.TRefundResponse{
				Header: &wpb.TResponseHeader{
					Code:        tpb.EErrorCode_EC_OK,
					Explanation: explanation,
				},
				Refund: refund,
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

		response, err := app.Refund(context.Background(), &wpb.TRefundRequest{
			SupplierId: testSupplierID,
			TicketId:   ticketID,
		})
		clientMock.AssertExpectations(t)

		if assert.NoError(t, err) {
			logEntries := logs.TakeAll()
			if assert.Equal(t, 1, len(logEntries)) {
				assert.Equal(t, uzap.ErrorLevel, logEntries[0].Level)
				assert.Equal(t, fmt.Sprintf("App.Refund: %s", expectedError), logEntries[0].Message)
			}

			assertpb.Equal(t, &wpb.TRefundResponse{
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
		t.Run(fmt.Sprintf("PostRefund %s", testCase.Name), func(t *testing.T) {
			logger, logs := setupLogsCapture()
			app, teardownApp, err := NewTestApp(t, nil, logger)
			assert.NoError(t, err)
			defer teardownApp()

			clientMock, teardownClientMock := mock.SetupClientMock()
			defer teardownClientMock()

			clientMock.On("PostRefund", ticketID).Return(
				refund, testCase.ExpectedHeader.Explanation, testCase.Error)
			response, err := app.Refund(context.Background(), &wpb.TRefundRequest{
				SupplierId: supplierID,
				TicketId:   ticketID,
			})
			clientMock.AssertExpectations(t)

			if assert.NoError(t, err) {
				logEntries := logs.TakeAll()
				if assert.Equal(t, 1, len(logEntries)) {
					assert.Equal(t, uzap.ErrorLevel, logEntries[0].Level)
					assert.Equal(t, fmt.Sprintf("App.Refund: %s", testCase.Error.Error()), logEntries[0].Message)
				}

				assertpb.Equal(t, &wpb.TRefundResponse{Header: testCase.ExpectedHeader}, response)
			}
		})
	}
}
