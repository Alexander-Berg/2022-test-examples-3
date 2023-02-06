package db

import (
	"context"
	"strconv"
	"testing"

	"github.com/stretchr/testify/require"
	"golang.yandex/hasql"

	"a.yandex-team.ru/travel/komod/trips/internal/services/unprocessedorders"
	"a.yandex-team.ru/travel/komod/trips/internal/testutils"
)

func TestUnprocessedOrdersStorage_Upsert(t *testing.T) {
	testPgClient := testutils.GetTestPgClient()
	dbTestTimeout := testutils.GetDBTestTimeout()
	withEmptyDB := testutils.NewDBCleaner(testPgClient, AllModels...)
	storage := NewUnprocessedOrdersStorage(testPgClient)

	t.Run(
		"Upsert a new one",
		withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbTestTimeout)
				defer cancelFunc()

				order1 := unprocessedorders.UnprocessedOrder{ID: "order1"}
				inserted, err := storage.Upsert(ctx, &order1)
				require.True(t, inserted)
				require.NoError(t, err)

				order2 := unprocessedorders.UnprocessedOrder{ID: "order2"}
				inserted, err = storage.Upsert(ctx, &order2)
				require.True(t, inserted)
				require.NoError(t, err)

				db, err := testPgClient.GetDB(hasql.Primary)
				require.NoError(t, err)

				results := make([]string, 0)
				err = db.Model(&UnprocessedOrder{}).Select("id").Scan(&results).Error
				require.NoError(t, err)
				require.Equal(t, []string{order1.ID, order2.ID}, results)
			},
		),
	)

	t.Run(
		"Upsert a duplicate",
		withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbTestTimeout)
				defer cancelFunc()

				order1 := unprocessedorders.UnprocessedOrder{ID: "order1"}
				inserted, err := storage.Upsert(ctx, &order1)
				require.True(t, inserted)
				require.NoError(t, err)

				inserted, err = storage.Upsert(ctx, &order1)
				require.False(t, inserted)
				require.NoError(t, err)

				db, err := testPgClient.GetDB(hasql.Primary)
				require.NoError(t, err)

				results := make([]string, 0)
				err = db.Model(&UnprocessedOrder{}).Select("id").Scan(&results).Error
				require.NoError(t, err)
				require.Equal(t, []string{order1.ID}, results)
			},
		),
	)
}

func TestUnprocessedOrdersStorage_Count(t *testing.T) {
	testPgClient := testutils.GetTestPgClient()
	dbTestTimeout := testutils.GetDBTestTimeout()
	withEmptyDB := testutils.NewDBCleaner(testPgClient, AllModels...)
	storage := NewUnprocessedOrdersStorage(testPgClient)

	t.Run(
		"Empty",
		withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbTestTimeout)
				defer cancelFunc()

				count, err := storage.Count(ctx)

				require.NoError(t, err)
				require.Zero(t, count)
			},
		),
	)

	t.Run(
		"Non-empty",
		withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbTestTimeout)
				defer cancelFunc()
				const expectedCount = 10
				for i := 0; i < expectedCount; i++ {
					_, err := storage.Upsert(ctx, &unprocessedorders.UnprocessedOrder{ID: strconv.Itoa(i)})
					require.NoError(t, err)
				}

				count, err := storage.Count(ctx)

				require.NoError(t, err)
				require.Equal(t, expectedCount, count)
			},
		),
	)
}
