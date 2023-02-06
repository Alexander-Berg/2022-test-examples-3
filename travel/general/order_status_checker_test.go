package pretrip

import (
	"context"
	"fmt"
	"testing"

	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/travel/notifier/internal/models"
)

func TestOrderStatusChecker(t *testing.T) {
	testCases := []struct {
		name                string
		orderFromRepository models.Order
		repositoryError     error
		currentOrder        models.Order
		errExpected         error
		isCorrectExpected   bool
	}{
		{
			name:                "no order in database/current_order was not fulfilled",
			orderFromRepository: models.Order{},
			repositoryError:     nil,
			currentOrder:        models.Order{ID: "1", WasFulfilled: false},
			errExpected:         nil,
			isCorrectExpected:   false,
		},
		{
			name:                "no order in database/current_order was fulfilled",
			orderFromRepository: models.Order{},
			repositoryError:     nil,
			currentOrder:        models.Order{ID: "1", WasFulfilled: true},
			errExpected:         nil,
			isCorrectExpected:   true,
		},
		{
			name:                "order in database was fulfilled/current_order was not fulfilled",
			orderFromRepository: models.Order{WasFulfilled: true},
			repositoryError:     nil,
			currentOrder:        models.Order{ID: "1", WasFulfilled: false},
			errExpected:         nil,
			isCorrectExpected:   true,
		},
		{
			name:                "order in database was not fulfilled/current_order was fulfilled",
			orderFromRepository: models.Order{WasFulfilled: false},
			repositoryError:     nil,
			currentOrder:        models.Order{ID: "1", WasFulfilled: true},
			errExpected:         nil,
			isCorrectExpected:   true,
		},
		{
			name:                "order in database was not fulfilled/current_order was not fulfilled",
			orderFromRepository: models.Order{WasFulfilled: false},
			repositoryError:     nil,
			currentOrder:        models.Order{ID: "1", WasFulfilled: false},
			errExpected:         nil,
			isCorrectExpected:   false,
		},
		{
			name:                "order in database was fulfilled/current_order was fulfilled",
			orderFromRepository: models.Order{WasFulfilled: true},
			repositoryError:     nil,
			currentOrder:        models.Order{ID: "1", WasFulfilled: true},
			errExpected:         nil,
			isCorrectExpected:   true,
		},
		{
			name:                "repository err",
			orderFromRepository: models.Order{},
			repositoryError:     fmt.Errorf("some error"),
			currentOrder:        models.Order{ID: "1"},
			errExpected:         fmt.Errorf("some error"),
			isCorrectExpected:   false,
		},
	}
	for _, testCase := range testCases {
		t.Run(
			"Check/"+testCase.name, func(t *testing.T) {
				ordersRepository := &fakeOrdersRepository{}
				checker := NewOrderStatusChecker(ordersRepository)
				ordersRepository.On("GetByID", mock.Anything, testCase.currentOrder.ID).Return(
					testCase.orderFromRepository,
					testCase.repositoryError,
				)

				isCorrect, err := checker.Check(context.Background(), testCase.currentOrder)

				require.Equal(t, testCase.errExpected, err)
				require.Equal(t, testCase.isCorrectExpected, isCorrect)
			},
		)
	}
}
