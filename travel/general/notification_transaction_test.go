package database

import (
	"context"
	"sync"
	"testing"
	"time"

	"github.com/jonboulle/clockwork"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/core/log/nop"
	"a.yandex-team.ru/travel/library/go/testutil"
	"a.yandex-team.ru/travel/notifier/internal/models"
)

func TestNotificationTransaction(t *testing.T) {
	testPgClient := getTestPgClient()
	repository := NewNotificationsRepository(testPgClient, true)
	dbTestTimeout := getDBTestTimeout()
	withEmptyDB := newDBCleaner(testPgClient)
	clock := clockwork.NewFakeClockAt(time.Now())
	buildNotification := func(id uint64, recipientID int32, opts ...notificationOption) models.Notification {
		return buildNotification(clock, id, recipientID, opts...)
	}
	txOptions := TransactionOptions{
		StatementTimeout:                2 * time.Second,
		LockTimeout:                     1 * time.Second,
		IdleInTransactionSessionTimeout: 2 * time.Second,
	}

	t.Run(
		"Update/consecutive transaction see new state", withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbTestTimeout)
				defer cancelFunc()
				newStatus := models.NotificationStatusSent

				err := storeNotifications(testPgClient, []models.Notification{buildNotification(1, 1)})
				require.NoError(t, err)
				notification, err := notificationByID(testPgClient, 1)
				require.NoError(t, err)

				tx1, err := repository.BeginTransaction(ctx, notification, txOptions)
				require.NoError(t, err)
				notification.Status = newStatus
				require.NoError(t, tx1.Update(notification, &nop.Logger{}))
				require.NoError(t, tx1.Commit(&nop.Logger{}))
				require.EqualValues(t, true, tx1.(*notificationTransaction).isClosed)

				tx2, err := repository.BeginTransaction(ctx, models.Notification{ID: notification.ID}, txOptions)
				require.NoError(t, err)

				require.Equal(t, newStatus, tx2.(*notificationTransaction).notification.Status)
				require.NoError(t, tx2.Commit(&nop.Logger{}))
			},
		),
	)

	t.Run(
		"Update/locks row for update/second parallel transaction gets error", withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbTestTimeout)
				defer cancelFunc()
				newStatus := models.NotificationStatusSent
				secondTransactionStarted := sync.WaitGroup{}
				secondTransactionStarted.Add(1)

				err := storeNotifications(testPgClient, []models.Notification{buildNotification(1, 1)})
				require.NoError(t, err)
				notification, err := notificationByID(testPgClient, 1)
				require.NoError(t, err)

				tx1, err := repository.BeginTransaction(ctx, notification, txOptions)
				require.NoError(t, err)
				go func() {
					defer secondTransactionStarted.Done()
					_, err := repository.BeginTransaction(ctx, notification, txOptions)
					require.Error(t, err)
					require.Contains(t, err.Error(), "ERROR: could not obtain lock on row in relation \"notifications\" (SQLSTATE 55P03)")
				}()

				notification.Status = newStatus
				require.NoError(t, tx1.Update(notification, &nop.Logger{}))
				secondTransactionStarted.Wait() // to ensure that first tx won't be finished before second tx starts
				require.NoError(t, tx1.Commit(&nop.Logger{}))
				isTimeouted := testutil.CallWithTimeout(func() { secondTransactionStarted.Wait() }, 10*time.Second)
				require.NoError(t, isTimeouted)
			},
		),
	)

	t.Run(
		"Update/called with different notification id returns error", withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbTestTimeout)
				defer cancelFunc()

				err := storeNotifications(testPgClient, []models.Notification{buildNotification(1, 1)})
				require.NoError(t, err)
				notification, err := notificationByID(testPgClient, 1)
				require.NoError(t, err)

				tx, err := repository.BeginTransaction(ctx, notification, txOptions)
				require.NoError(t, err)
				defer tx.Commit(&nop.Logger{})

				notification.ID++
				err = tx.Update(notification, &nop.Logger{})
				require.Error(t, err)
				require.Equal(t, err, errUpdateDifferentRow)
			},
		),
	)

	txFinishTestCases := []struct {
		name         string
		finishAction func(tx NotificationTransaction) error
	}{
		{
			name: "Commit/in parallel commits exactly once and then returns ErrTxClosed",
			finishAction: func(tx NotificationTransaction) error {
				return tx.Commit(&nop.Logger{})
			},
		},
		{
			name: "Rollback/in parallel rollbacks exactly once and then returns ErrTxClosed",
			finishAction: func(tx NotificationTransaction) error {
				return tx.Rollback(&nop.Logger{})
			},
		},
	}
	for _, testCase := range txFinishTestCases {
		t.Run(
			testCase.name, withEmptyDB(
				func(t *testing.T) {
					ctx, cancelFunc := context.WithTimeout(context.Background(), dbTestTimeout)
					defer cancelFunc()

					err := storeNotifications(testPgClient, []models.Notification{buildNotification(1, 1)})
					require.NoError(t, err)
					notification, err := notificationByID(testPgClient, 1)
					require.NoError(t, err)

					tx, err := repository.BeginTransaction(ctx, notification, txOptions)
					require.NoError(t, err)

					wg := sync.WaitGroup{}
					wg.Add(2)
					errors := make(chan error, 2)
					go func() {
						defer wg.Done()
						if err := testCase.finishAction(tx); err != nil {
							errors <- err
						}
					}()
					go func() {
						defer wg.Done()
						if err := testCase.finishAction(tx); err != nil {
							errors <- err
						}
					}()
					wg.Wait()
					close(errors)

					require.Equal(t, 1, len(errors))
					require.Equal(t, ErrTxClosed, <-errors)
				},
			),
		)
	}

	t.Run(
		"BeginTransaction/locks row for update/unlocks after timeout", withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbTestTimeout)
				defer cancelFunc()
				newStatus := models.NotificationStatusSent
				secondTransactionStarted := sync.WaitGroup{}
				secondTransactionStarted.Add(1)

				err := storeNotifications(testPgClient, []models.Notification{buildNotification(1, 1)})
				require.NoError(t, err)
				notification, err := notificationByID(testPgClient, 1)
				require.NoError(t, err)

				tx1, err := repository.BeginTransaction(
					ctx,
					notification,
					TransactionOptions{
						StatementTimeout:                2 * time.Second,
						LockTimeout:                     2 * time.Second,
						IdleInTransactionSessionTimeout: 2 * time.Second,
					},
				)
				require.NoError(t, err)

				time.Sleep(2500 * time.Millisecond)
				notification.Status = newStatus
				require.NoError(t, tx1.Update(notification, &nop.Logger{}))

				tx2, err := repository.BeginTransaction(
					ctx,
					notification,
					TransactionOptions{StatementTimeout: 5 * time.Second, LockTimeout: 2 * time.Second},
				)
				require.NoError(t, err)
				require.NoError(t, tx2.Update(notification, &nop.Logger{}))
				require.NoError(t, tx2.Commit(&nop.Logger{}))
			},
		),
	)
}
