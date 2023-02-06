package database

import (
	"context"
	"fmt"
	"testing"

	"github.com/stretchr/testify/require"
	"golang.yandex/hasql"
	"gorm.io/gorm"

	"a.yandex-team.ru/travel/library/go/syncutil"
	"a.yandex-team.ru/travel/notifier/internal/models"
)

func TestUsersRepository_GetOrCreate(t *testing.T) {
	testPgClient := getTestPgClient()
	repository := NewUsersRepository(testPgClient)
	dbOperationTimeout := getDBTestTimeout()
	withEmptyDB := newDBCleaner(testPgClient)

	t.Run(
		"Create new users", withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbOperationTimeout)
				defer cancelFunc()

				for i := 1; i <= 2; i++ {
					passportID := fmt.Sprintf("%v", i)
					user, err := repository.GetOrCreate(ctx, models.NewUser().WithPassportID(passportID))
					require.NoError(t, err)
					require.EqualValues(t, passportID, user.GetPassportID())

					var usersCount int64
					err = testPgClient.ExecuteInTransaction(
						hasql.Primary, func(db *gorm.DB) error {
							return db.Model(user).Count(&usersCount).Error
						},
					)

					require.NoError(t, err)
					require.EqualValues(t, i, usersCount)
				}
			},
		),
	)

	t.Run(
		"Create and get one user", withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbOperationTimeout)
				defer cancelFunc()

				passportID := "1"
				for i := 1; i <= 2; i++ {
					user, err := repository.GetOrCreate(ctx, models.NewUser().WithPassportID(passportID))
					require.NoError(t, err)
					require.EqualValues(t, passportID, user.GetPassportID())

					var usersCount int64
					err = testPgClient.ExecuteInTransaction(
						hasql.Primary, func(db *gorm.DB) error {
							return db.Model(user).Count(&usersCount).Error
						},
					)

					require.NoError(t, err)
					require.EqualValues(t, usersCount, 1)
				}
			},
		),
	)

	t.Run(
		"GetByPassportID/user doesn't exist returns nil", withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbOperationTimeout)
				defer cancelFunc()

				passportID := "1"

				user, err := repository.GetByPassportID(ctx, passportID)

				require.NoError(t, err)
				require.Nil(t, user)
			},
		),
	)

	t.Run(
		"Get/user exists returns user", withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbOperationTimeout)
				defer cancelFunc()

				passportID := "1"
				getUserAndAssert(t, repository, ctx, passportID)
			},
		),
	)
	t.Run(
		"Concurrent GetOrCreate user", withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbOperationTimeout)
				defer cancelFunc()

				for i := 0; i < 10; i++ {
					passportID := fmt.Sprintf("%d", i)
					var wg syncutil.WaitGroup
					wg.Go(func() {
						getUserAndAssert(t, repository, ctx, passportID)
					})
					wg.Go(func() {
						getUserAndAssert(t, repository, ctx, passportID)
					})
					wg.Wait()
				}
			},
		),
	)
}

func getUserAndAssert(t *testing.T, repository *UsersRepository, ctx context.Context, passportID string) {
	_, err := repository.GetOrCreate(ctx, models.NewUser().WithPassportID(passportID))
	require.NoError(t, err)

	user, err := repository.GetByPassportID(ctx, passportID)

	require.NoError(t, err)
	require.NotNil(t, user)
	require.EqualValues(t, passportID, user.GetPassportID())
}
