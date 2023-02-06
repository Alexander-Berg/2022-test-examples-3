package database

import (
	"context"
	"sort"
	"strconv"
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/travel/notifier/internal/models"
)

func TestOrdersRepository_GetByCorrelationID(t *testing.T) {
	testPgClient := getTestPgClient()
	repository := NewOrdersRepository(testPgClient)
	dbOperationTimeout := getDBTestTimeout()
	withEmptyDB := newDBCleaner(testPgClient)

	t.Run(
		"GetByCorrelationID", withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbOperationTimeout)
				defer cancelFunc()

				for i := 1; i <= 5; i++ {
					order := models.Order{
						ID:            strconv.Itoa(i),
						CorrelationID: strconv.Itoa(i % 2),
						Type:          models.OrderTrain,
					}
					err := repository.create(ctx, order)
					require.NoError(t, err)
				}

				dbOrders, err := repository.GetByCorrelationID(ctx, "0")
				require.NoError(t, err)
				require.EqualValues(t, []string{"2", "4"}, getIDs(dbOrders))

				dbOrders, err = repository.GetByCorrelationID(ctx, "1")
				require.NoError(t, err)
				require.EqualValues(t, []string{"1", "3", "5"}, getIDs(dbOrders))

				dbOrders, err = repository.GetByCorrelationID(ctx, "2")
				require.NoError(t, err)
				require.EqualValues(t, []models.Order{}, dbOrders, "non-existing correlation ID, should return an empty list")
			},
		),
	)

	t.Run(
		"Upsert", withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbOperationTimeout)
				defer cancelFunc()
				order := models.Order{
					ID:           "1",
					Type:         models.OrderTrain,
					WasFulfilled: false,
				}

				err := repository.create(ctx, order)
				require.NoError(t, err)

				dbOrder, err := repository.GetByID(ctx, "1")
				require.NoError(t, err)
				require.False(t, dbOrder.WasFulfilled)

				order.WasFulfilled = true
				err = repository.Upsert(ctx, order)
				require.NoError(t, err)

				dbOrder, err = repository.GetByID(ctx, "1")
				require.NoError(t, err)
				require.True(t, dbOrder.WasFulfilled)
			},
		),
	)
}

func getIDs(orders []models.Order) []string {
	result := []string{}
	for _, order := range orders {
		result = append(result, order.ID)
	}
	sort.Slice(
		result, func(i, j int) bool {
			return result[i] < result[j]
		},
	)
	return result
}
