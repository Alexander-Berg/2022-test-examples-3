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
	pb "a.yandex-team.ru/travel/buses/backend/proto"
	wpb "a.yandex-team.ru/travel/buses/backend/proto/worker"
)

func TestApp_BookParams(t *testing.T) {
	var (
		testRideID      = "testRideID"
		testSupplierID  = dict.GetSuppliersList()[0]
		testBookParams  = &pb.TBookParams{}
		testRefinement  = &pb.TRideRefinement{}
		testExplanation = &wpb.TExplanation{ConnectorResponseCode: 123}
	)

	t.Run("ok", func(t *testing.T) {
		app, teardownApp, err := NewTestApp(t, nil, nil)
		assert.NoError(t, err)
		defer teardownApp()

		clientMock, teardownClientMock := mock.SetupClientMock()
		defer teardownClientMock()

		clientMock.On("GetBookParams", testRideID).Return(testBookParams, testRefinement, testExplanation, nil)
		response, err := app.BookParams(context.Background(), &wpb.TBookParamsRequest{
			RideId:     testRideID,
			SupplierId: testSupplierID,
		})
		clientMock.AssertExpectations(t)

		if assert.NoError(t, err) {
			assertpb.Equal(t, &wpb.TBookParamsResponse{
				Header: &wpb.TResponseHeader{
					Code:        tpb.EErrorCode_EC_OK,
					Explanation: testExplanation,
				},
				BookParams: testBookParams,
			}, response)
		}
	})

	t.Run("NewClient error", func(t *testing.T) {
		logger, logs := setupLogsCapture()
		app, teardownApp, err := NewTestApp(t, nil, logger)
		if !assert.NoError(t, err) {
			return
		}
		defer teardownApp()

		clientMock, teardownClientMock := mock.SetupClientMock()
		defer teardownClientMock()

		var (
			testSupplierID uint32 = 1000
			expectedError         = "NewClientWithTransport: no such supplier with id = 1000"
		)

		response, err := app.BookParams(context.Background(), &wpb.TBookParamsRequest{
			RideId:     testRideID,
			SupplierId: testSupplierID,
		})
		clientMock.AssertExpectations(t)

		if assert.NoError(t, err) {
			logEntries := logs.TakeAll()
			if assert.Equal(t, 1, len(logEntries)) {
				assert.Equal(t, uzap.ErrorLevel, logEntries[0].Level)
				assert.Equal(t, fmt.Sprintf("App.BookParams: %s", expectedError), logEntries[0].Message)
			}

			assertpb.Equal(t, &tpb.TError{
				Code:    tpb.EErrorCode_EC_GENERAL_ERROR,
				Message: expectedError,
			}, response.Header.Error)
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

			clientMock.On("GetBookParams", testRideID).Return(
				testBookParams, testRefinement, testCase.ExpectedHeader.Explanation, testCase.Error)
			response, err := app.BookParams(context.Background(), &wpb.TBookParamsRequest{
				RideId:     testRideID,
				SupplierId: testSupplierID,
			})
			clientMock.AssertExpectations(t)

			if assert.NoError(t, err) {
				logEntries := logs.TakeAll()
				if !assert.Equal(t, 1, len(logEntries)) ||
					!assert.Equal(t, uzap.ErrorLevel, logEntries[0].Level) ||
					!assert.Equal(t, fmt.Sprintf("App.BookParams: %s", testCase.Error.Error()), logEntries[0].Message) {
					return
				}

				if !assertpb.Equal(t, &wpb.TBookParamsResponse{Header: testCase.ExpectedHeader}, response) {
					return
				}
			}
		})
	}
}
