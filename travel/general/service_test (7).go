package processing

import (
	"context"
	"fmt"
	"testing"
	"time"

	"github.com/gofrs/uuid"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"go.uber.org/zap/zaptest"

	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/log/zap"
	"a.yandex-team.ru/travel/library/go/testutil"
	"a.yandex-team.ru/travel/notifier/internal/database"
	"a.yandex-team.ru/travel/notifier/internal/models"
)

func TestProcessingService(t *testing.T) {
	logger := zap.NewWithCore(zaptest.NewLogger(t).Core())
	newService := func(processor *processorMock, repository *repositoryMock) *Service {
		return NewService(
			logger, repository, WithProcessor(models.NotificationTypePretrip, processor), WithConfig(
				Config{
					SendToUnsubscribed: false,
					DBStatementTimeout: 0,
					DBLockTimeout:      0,
				},
			),
		)
	}
	ctx := context.Background()
	timeout := 2 * time.Second
	txOptions := database.TransactionOptions{}

	t.Run(
		"processSingle/recipient is not subscribed notification should be cancelled", func(t *testing.T) {
			processor := &processorMock{}
			service := newService(processor, &repositoryMock{})
			notification := &models.Notification{
				Type:      models.NotificationTypePretrip,
				Recipient: &models.Recipient{IsSubscribed: false},
			}
			tx := &transactionMock{notification: notification}
			tx.On("Update", mock.Anything).Return(nil)

			service.processSingle(ctx, notification, tx, uuid.Nil)

			tx.AssertCalled(t, "Update", *notification)
			require.Equal(t, models.NotificationStatusCancelled, tx.notification.Status)
			processor.AssertNumberOfCalls(t, "Process", 0)
		},
	)

	t.Run(
		"processSingle/unknown type", func(t *testing.T) {
			service := newService(&processorMock{}, &repositoryMock{})
			notification := &models.Notification{
				Type:      models.NotificationType{Name: "unknown"},
				Recipient: &models.Recipient{IsSubscribed: true},
			}
			tx := &transactionMock{notification: notification}
			tx.On("Update", mock.Anything).Return(nil)

			service.processSingle(ctx, notification, tx, uuid.Nil)

			tx.AssertCalled(t, "Update", *notification)
			require.Equal(t, models.NotificationStatusFailed, tx.notification.Status)
		},
	)

	t.Run(
		"process/known type", func(t *testing.T) {
			processor := &processorMock{}
			service := newService(processor, &repositoryMock{})
			notification := &models.Notification{Type: models.NotificationTypePretrip, Recipient: &models.Recipient{IsSubscribed: true}}
			tx := &transactionMock{}
			tx.On("Update", mock.Anything).Return(nil)
			processor.On("Process", ctx, notification, tx, uuid.Nil).Return()

			service.processSingle(ctx, notification, tx, uuid.Nil)

			processor.AssertCalled(t, "Process", ctx, notification, tx, uuid.Nil)
			tx.AssertNumberOfCalls(t, "Update", 0)
		},
	)

	t.Run(
		"Process/calls processSingle for each notification and calls Commit for each transactions", testutil.TestWithTimeout(
			timeout, func(t *testing.T) {
				processor := &processorMock{}
				repository := &repositoryMock{}
				service := newService(processor, repository)
				n := uint64(5)
				notifications := make([]models.Notification, 0, n)
				transactions := make([]*transactionMock, 0, n)
				for i := uint64(1); i <= n; i++ {
					notification := models.Notification{
						ID:        i,
						Type:      models.NotificationTypePretrip,
						Recipient: &models.Recipient{IsSubscribed: true},
					}
					notifications = append(notifications, notification)
					tx := &transactionMock{}
					transactions = append(transactions, tx)
					repository.On("BeginTransaction", ctx, notification, txOptions).Return(tx, nil)
					tx.On("Update", mock.Anything).Return(nil)
					tx.On("Commit").Return(nil)
					processor.On("Process", ctx, &notification, tx, uuid.Nil).Return()
				}

				waitAllProcessed := service.Process(ctx, notifications, uuid.Nil)

				repository.AssertNumberOfCalls(t, "BeginTransaction", int(n))

				waitAllProcessed()
				for _, tx := range transactions {
					tx.AssertNumberOfCalls(t, "Commit", 1)
				}
			},
		),
	)

	t.Run(
		"Process/not calls commit if failed to begin transaction", testutil.TestWithTimeout(
			timeout, func(t *testing.T) {
				processor := &processorMock{}
				repository := &repositoryMock{}
				service := newService(processor, repository)
				notification := models.Notification{
					ID:        1,
					Type:      models.NotificationTypePretrip,
					Recipient: &models.Recipient{IsSubscribed: true},
				}
				tx := &transactionMock{}
				expectedErr := fmt.Errorf("failed to begin transaction")
				repository.On("BeginTransaction", ctx, notification, txOptions).Return(tx, expectedErr)
				tx.On("Update", mock.Anything).Return(nil)
				tx.On("Commit").Return(nil)
				processor.On("Process", ctx, &notification, tx, uuid.Nil).Return()

				waitAllProcessed := service.Process(ctx, []models.Notification{notification}, uuid.Nil)

				repository.AssertNumberOfCalls(t, "BeginTransaction", 1)

				waitAllProcessed()
				tx.AssertNumberOfCalls(t, "Commit", 0)
			},
		),
	)

	t.Run(
		"Process/handles panic if BeginTransaction panics",
		testutil.TestWithTimeout(
			timeout, func(t *testing.T) {
				processor := &processorMock{}
				repository := &repositoryMock{}
				service := newService(processor, repository)
				notifications := []models.Notification{
					{ID: 1, Type: models.NotificationTypePretrip, Recipient: &models.Recipient{IsSubscribed: true}},
					{ID: 2, Type: models.NotificationTypePretrip, Recipient: &models.Recipient{IsSubscribed: true}},
				}
				tx := &transactionMock{}
				repository.On("BeginTransaction", ctx, notifications[0], txOptions).Return(tx, nil)
				repository.On("BeginTransaction", ctx, notifications[1], txOptions).Panic("something went wrong")
				tx.On("Update", mock.Anything).Return(nil)
				tx.On("Commit").Return(nil)
				processor.On("Process", ctx, &notifications[0], tx, uuid.Nil).Return()

				waitAllProcessed := service.Process(ctx, notifications, uuid.Nil)

				repository.AssertNumberOfCalls(t, "BeginTransaction", 2)
				assert.NotPanics(t, waitAllProcessed)
			},
		),
	)

	t.Run(
		"Process/handles panic from Commit", testutil.TestWithTimeout(
			timeout, func(t *testing.T) {
				processor := &processorMock{}
				repository := &repositoryMock{}
				service := newService(processor, repository)
				notifications := []models.Notification{
					{
						ID:        1,
						Type:      models.NotificationTypePretrip,
						Recipient: &models.Recipient{IsSubscribed: true},
					},
				}
				tx := &transactionMock{}
				repository.On("BeginTransaction", ctx, notifications[0], txOptions).Return(tx, nil)
				tx.On("Update", mock.Anything).Return(nil)
				tx.On("Commit").Panic("something went wrong")
				processor.On("Process", ctx, &notifications[0], tx, uuid.Nil).Return()

				waitAllProcessed := service.Process(ctx, notifications, uuid.Nil)

				assert.NotPanics(t, waitAllProcessed)
				tx.AssertNumberOfCalls(t, "Commit", 1)
			},
		),
	)
}

type repositoryMock struct {
	mock.Mock
}

func (r *repositoryMock) BeginTransaction(ctx context.Context, notification models.Notification, options database.TransactionOptions) (
	database.NotificationTransaction,
	error,
) {
	args := r.Called(ctx, notification, options)
	return args.Get(0).(*transactionMock), args.Error(1)
}

func (r *repositoryMock) GetForRecipient(
	ctx context.Context, recipientID int32, from time.Time, until time.Time,
) ([]models.Notification, error) {
	args := r.Called(ctx, recipientID, from, until)
	return args.Get(0).([]models.Notification), args.Error(1)
}

func (r *repositoryMock) AlreadySentForOrder(
	ctx context.Context, orderID string, notificationType models.NotificationType,
) ([]models.Notification, error) {
	args := r.Called(ctx, orderID, notificationType)
	return args.Get(0).([]models.Notification), args.Error(1)
}

type processorMock struct {
	mock.Mock
}

func (p *processorMock) Process(
	ctx context.Context,
	notification *models.Notification,
	notificationTx database.NotificationTransaction,
	iterationID uuid.UUID,
) {
	p.Called(ctx, notification, notificationTx, iterationID)
}

type transactionMock struct {
	mock.Mock
	notification *models.Notification
}

func (p *transactionMock) Update(notification models.Notification, logger log.Logger) error {
	args := p.Called(notification)
	return args.Error(0)
}

func (p *transactionMock) Commit(logger log.Logger) error {
	args := p.Called()
	return args.Error(0)
}

func (p *transactionMock) Rollback(logger log.Logger) error {
	args := p.Called()
	return args.Error(0)
}
