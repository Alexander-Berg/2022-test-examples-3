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

func TestApp_Book(t *testing.T) {
	var (
		rideID     = "rideID"
		supplierID = dict.GetSuppliersList()[0]
		contacts   = &wpb.TBookContacts{
			Phone: "+7 123 456-78-90",
			Email: "test@example.org",
		}
		passengers = []*wpb.TBookPassenger{
			{FirstName: "Тест 1"},
			{FirstName: "Тест 2"},
		}
		order       = &wpb.TOrder{Id: "order"}
		explanation = &wpb.TExplanation{ConnectorResponseCode: 123}
	)

	t.Run("ok", func(t *testing.T) {
		app, teardownApp, err := NewTestApp(t, nil, nil)
		assert.NoError(t, err)
		defer teardownApp()

		clientMock, teardownClientMock := mock.SetupClientMock()
		defer teardownClientMock()

		clientMock.On("PostBook", rideID, contacts, passengers).Return(order, explanation, nil)
		response, err := app.Book(context.Background(), &wpb.TBookRequest{
			RideId:     rideID,
			SupplierId: supplierID,
			Contacts:   contacts,
			Passengers: passengers,
		})
		clientMock.AssertExpectations(t)

		if assert.NoError(t, err) {
			assertpb.Equal(t, &wpb.TBookResponse{
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

		response, err := app.Book(context.Background(), &wpb.TBookRequest{
			RideId:     rideID,
			SupplierId: testSupplierID,
			Contacts:   contacts,
			Passengers: passengers,
		})
		clientMock.AssertExpectations(t)

		if assert.NoError(t, err) {
			logEntries := logs.TakeAll()
			if assert.Equal(t, 1, len(logEntries)) {
				assert.Equal(t, uzap.ErrorLevel, logEntries[0].Level)
				assert.Equal(t, fmt.Sprintf("App.Book: %s", expectedError), logEntries[0].Message)
			}

			assertpb.Equal(t, &wpb.TBookResponse{
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
		t.Run(fmt.Sprintf("PostBook %s", testCase.Name), func(t *testing.T) {
			logger, logs := setupLogsCapture()
			app, teardownApp, err := NewTestApp(t, nil, logger)
			assert.NoError(t, err)
			defer teardownApp()

			clientMock, teardownClientMock := mock.SetupClientMock()
			defer teardownClientMock()

			var testOrder = &wpb.TOrder{}

			clientMock.On("PostBook", rideID, contacts, passengers).Return(
				testOrder, testCase.ExpectedHeader.Explanation, testCase.Error)
			response, err := app.Book(context.Background(), &wpb.TBookRequest{
				RideId:     rideID,
				SupplierId: supplierID,
				Contacts:   contacts,
				Passengers: passengers,
			})
			clientMock.AssertExpectations(t)

			if assert.NoError(t, err) {
				logEntries := logs.TakeAll()
				if assert.Equal(t, 1, len(logEntries)) {
					assert.Equal(t, uzap.ErrorLevel, logEntries[0].Level)
					assert.Equal(t, fmt.Sprintf("App.Book: %s", testCase.Error.Error()), logEntries[0].Message)
				}

				assertpb.Equal(t, &wpb.TBookResponse{Header: testCase.ExpectedHeader}, response)
			}
		})
	}
}
