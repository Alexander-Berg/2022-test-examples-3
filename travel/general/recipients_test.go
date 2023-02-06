package database

import (
	"context"
	"fmt"
	"testing"

	"github.com/stretchr/testify/require"
	"golang.yandex/hasql"
	"gorm.io/gorm"
)

func TestRecipientsRepository_GetOrCreate(t *testing.T) {
	testPgClient := getTestPgClient()
	repository := NewRecipientsRepository(testPgClient)
	dbOperationTimeout := getDBTestTimeout()
	withEmptyDB := newDBCleaner(testPgClient)

	t.Run(
		"Create new recipients", withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbOperationTimeout)
				defer cancelFunc()

				for i := 1; i <= 2; i++ {
					email := fmt.Sprintf("%v@", i)
					recipient, err := repository.GetOrCreateByEmail(ctx, email)
					require.NoError(t, err)
					require.EqualValues(t, email, recipient.GetEmail())

					var recipientsCount int64
					err = testPgClient.ExecuteInTransaction(
						hasql.Primary, func(db *gorm.DB) error {
							return db.Model(recipient).Count(&recipientsCount).Error
						},
					)

					require.NoError(t, err)
					require.EqualValues(t, i, recipientsCount)
				}
			},
		),
	)

	t.Run(
		"Create and get one recipient", withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbOperationTimeout)
				defer cancelFunc()

				email := "qqq@"
				for i := 1; i <= 2; i++ {
					recipient, err := repository.GetOrCreateByEmail(ctx, email)
					require.NoError(t, err)
					require.EqualValues(t, email, recipient.GetEmail())

					var recipientsCount int64
					err = testPgClient.ExecuteInTransaction(
						hasql.Primary, func(db *gorm.DB) error {
							return db.Model(recipient).Count(&recipientsCount).Error
						},
					)

					require.NoError(t, err)
					require.EqualValues(t, recipientsCount, 1)
				}
			},
		),
	)

	t.Run(
		"Get/recipient doesn't exist returns nil", withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbOperationTimeout)
				defer cancelFunc()

				email := "qqq@"

				recipient, err := repository.Get(ctx, email)

				require.NoError(t, err)
				require.Nil(t, recipient)
			},
		),
	)

	t.Run(
		"Get/recipient exists returns recipient", withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbOperationTimeout)
				defer cancelFunc()

				email := "qqq@"
				_, err := repository.GetOrCreateByEmail(ctx, email)
				require.NoError(t, err)

				recipient, err := repository.Get(ctx, email)

				require.NoError(t, err)
				require.NotNil(t, recipient)
				require.EqualValues(t, email, recipient.GetEmail())
			},
		),
	)

	t.Run(
		"GetByHash/recipient doesn't exist returns nil", withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbOperationTimeout)
				defer cancelFunc()

				hash := "hash"

				recipient, err := repository.GetByHash(ctx, hash)

				require.NoError(t, err)
				require.Nil(t, recipient)
			},
		),
	)

	t.Run(
		"GetByHash/recipient exists returns recipient", withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbOperationTimeout)
				defer cancelFunc()

				email := "qqq@"
				hash := "hash"
				recipient, err := repository.GetOrCreateByEmail(ctx, email)
				require.NoError(t, err)
				recipient.UnsubscribeHash = &hash
				_, err = repository.Update(ctx, *recipient)
				require.NoError(t, err)

				recipient, err = repository.GetByHash(ctx, hash)

				require.NoError(t, err)
				require.NotNil(t, recipient)
				require.EqualValues(t, email, recipient.GetEmail())
				require.NotNil(t, hash, *recipient.UnsubscribeHash)
			},
		),
	)

	t.Run(
		"GetByHash/hash must be unique", withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbOperationTimeout)
				defer cancelFunc()

				hash := "hash"

				email1 := "qqq@1"
				recipient1, err := repository.GetOrCreateByEmail(ctx, email1)
				require.NoError(t, err)
				recipient1.UnsubscribeHash = &hash
				_, err = repository.Update(ctx, *recipient1)
				require.NoError(t, err)

				email2 := "qqq@2"
				recipient2, err := repository.GetOrCreateByEmail(ctx, email2)
				require.NoError(t, err)
				recipient2.UnsubscribeHash = &hash

				_, err = repository.Update(ctx, *recipient2)
				require.Error(t, err)
				require.EqualError(
					t,
					err,
					"ERROR: duplicate key value violates unique constraint \"recipients_unsubscribe_hash_key\" (SQLSTATE 23505)",
				)
			},
		),
	)

	t.Run(
		"Update/updates record", withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbOperationTimeout)
				defer cancelFunc()

				hash := "hash"
				email := "qqq@1"
				recipient, err := repository.GetOrCreateByEmail(ctx, email)
				require.NoError(t, err)

				recipient.UnsubscribeHash = &hash
				_, err = repository.Update(ctx, *recipient)
				require.NoError(t, err)

				recipient, err = repository.Get(ctx, email)
				require.NoError(t, err)
				require.Equal(t, hash, *recipient.UnsubscribeHash)
			},
		),
	)
}
