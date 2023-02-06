package database

import (
	"context"
	"fmt"
	"strconv"
	"testing"
	"time"

	"github.com/jackc/pgtype"
	"github.com/jonboulle/clockwork"
	"github.com/stretchr/testify/require"
	"golang.yandex/hasql"
	"gorm.io/gorm"
	"gorm.io/gorm/clause"

	"a.yandex-team.ru/library/go/ptr"
	"a.yandex-team.ru/travel/notifier/internal/models"
	"a.yandex-team.ru/travel/notifier/internal/pgclient"
)

type notificationOption func(notification *models.Notification)

func withStatus(status models.NotificationStatus) notificationOption {
	return func(notification *models.Notification) {
		notification.Status = status
	}
}

func withPayload(payload []byte) notificationOption {
	return func(notification *models.Notification) {
		notification.Payload = &pgtype.JSON{}
		_ = notification.Payload.Set(payload)
	}
}

func postpone(postponedUntil time.Time) notificationOption {
	return func(notification *models.Notification) {
		notification.NotifyAt = postponedUntil
		notification.Status = models.NotificationStatusPostponed
	}
}

func notify(notifyAt time.Time) notificationOption {
	return func(notification *models.Notification) {
		notification.NotifyAt = notifyAt
	}
}

func buildNotification(clock clockwork.FakeClock, id uint64, recipientID int32, opts ...notificationOption) models.Notification {
	recipient := models.Recipient{
		ID:        recipientID,
		Email:     ptr.String(fmt.Sprintf("%v", recipientID)),
		CreatedAt: clock.Now(),
		UpdatedAt: clock.Now(),
	}
	notification := models.Notification{
		ID:           id,
		DispatchType: models.DispatchTypePush,
		Type:         models.NotificationTypePretrip,
		Channel:      models.NotificationChannelEmail,
		Recipient:    &recipient,
		NotifyAt:     clock.Now().Add(-time.Hour),
	}
	for _, opt := range opts {
		opt(&notification)
	}
	return notification
}

func TestNotificationsRepository(t *testing.T) {
	testPgClient := getTestPgClient()
	repository := NewNotificationsRepository(testPgClient, true)
	dbOperationTimeout := getDBTestTimeout()
	withEmptyDB := newDBCleaner(testPgClient)
	clock := clockwork.NewFakeClockAt(time.Now())
	buildNotification := func(id uint64, recipientID int32, opts ...notificationOption) models.Notification {
		return buildNotification(clock, id, recipientID, opts...)
	}

	t.Run(
		"Create/with payload", withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbOperationTimeout)
				defer cancelFunc()

				payload := []byte(`{"test": 1}`)
				notification, err := repository.Create(ctx, buildNotification(1, 1, withPayload(payload)))
				require.NoError(t, err)

				result := models.Notification{}
				err = testPgClient.ExecuteInTransaction(
					hasql.Primary, func(db *gorm.DB) error {
						return db.Model(notification).Where(models.Notification{ID: notification.ID}).First(&result).Error
					},
				)
				require.NoError(t, err)
				require.Equal(t, result.Payload.Bytes, payload)
			},
		),
	)

	t.Run(
		"Create/with different recipients", withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbOperationTimeout)
				defer cancelFunc()

				for i := 1; i <= 2; i++ {
					notification, err := repository.Create(ctx, buildNotification(uint64(i), int32(i)))
					require.NoError(t, err)

					var notificationsCount int64
					var recipientsCount int64
					err = testPgClient.ExecuteInTransaction(
						hasql.Primary, func(db *gorm.DB) error {
							if err := db.Model(notification.Recipient).Count(&recipientsCount).Error; err != nil {
								return err
							}
							return db.Model(notification).Count(&notificationsCount).Error
						},
					)

					require.NoError(t, err)
					require.EqualValues(t, notificationsCount, i)
					require.EqualValues(t, recipientsCount, i)
				}
			},
		),
	)

	t.Run(
		"Create/with the same recipient", withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbOperationTimeout)
				defer cancelFunc()

				for i := uint64(1); i <= 2; i++ {
					notification, err := repository.Create(ctx, buildNotification(i, 1))
					require.NoError(t, err)

					var notificationsCount int64
					var recipientsCount int64
					err = testPgClient.ExecuteInTransaction(
						hasql.Primary, func(db *gorm.DB) error {
							if err := db.Model(notification.Recipient).Count(&recipientsCount).Error; err != nil {
								return err
							}
							return db.Model(notification).Count(&notificationsCount).Error
						},
					)

					require.NoError(t, err)
					require.EqualValues(t, notificationsCount, i)
					require.EqualValues(t, recipientsCount, 1)
				}
			},
		),
	)

	t.Run(
		"Create/with the same notification id returns error", withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbOperationTimeout)
				defer cancelFunc()

				notification1, err := repository.Create(ctx, buildNotification(1, 1))
				require.NoError(t, err)

				notification2 := buildNotification(2, 1)
				notification2.ID = notification1.ID
				_, err = repository.Create(ctx, notification2)
				expectedErrorMessage := "ERROR: duplicate key value violates unique constraint \"notifications_pkey\" (SQLSTATE 23505)"
				require.Error(t, err, expectedErrorMessage)
			},
		),
	)

	t.Run(
		"CreateMany", withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbOperationTimeout)
				defer cancelFunc()

				notifications := []models.Notification{
					buildNotification(1, 1), buildNotification(2, 1), buildNotification(3, 2),
				}
				expectedNotificationsCount := len(notifications)
				expectedRecipientsCount := len(notifications) - 1

				_, err := repository.CreateMany(ctx, notifications)

				require.NoError(t, err)

				var notificationsCount int64
				var recipientsCount int64
				err = testPgClient.ExecuteInTransaction(
					hasql.Primary, func(db *gorm.DB) error {
						if err := db.Model(models.Recipient{}).Count(&recipientsCount).Error; err != nil {
							return err
						}
						return db.Model(models.Notification{}).Count(&notificationsCount).Error
					},
				)
				require.NoError(t, err)

				require.EqualValues(t, notificationsCount, expectedNotificationsCount)
				require.EqualValues(t, recipientsCount, expectedRecipientsCount)
			},
		),
	)

	type getFirstNTestCase struct {
		name                    string
		notifications           []models.Notification
		plannedLimit            uint
		postponedLimit          uint
		notSentLimit            uint
		expectedNotificationIds []int
	}

	getFirstNTestCases := []getFirstNTestCase{
		{
			name: "1 planned 1 postponed 1 not sent returns all",
			notifications: []models.Notification{
				buildNotification(1, 1, withStatus(models.NotificationStatusPlanned)),
				buildNotification(2, 2, withStatus(models.NotificationStatusPlanned), postpone(clock.Now())),
				buildNotification(3, 3, withStatus(models.NotificationStatusReadyToSend)),
			},
			plannedLimit:            1,
			postponedLimit:          1,
			notSentLimit:            1,
			expectedNotificationIds: []int{1, 2, 3},
		},
		{
			name: "1 planned 0 postponed 0 not sent returns planned",
			notifications: []models.Notification{
				buildNotification(1, 1, withStatus(models.NotificationStatusPlanned)),
			},
			plannedLimit:            1,
			postponedLimit:          1,
			notSentLimit:            1,
			expectedNotificationIds: []int{1},
		},
		{
			name: "0 planned 1 postponed 0 not sent returns postponed",
			notifications: []models.Notification{
				buildNotification(1, 1, withStatus(models.NotificationStatusPlanned), postpone(clock.Now())),
			},
			plannedLimit:            1,
			postponedLimit:          1,
			notSentLimit:            1,
			expectedNotificationIds: []int{1},
		},
		{
			name: "0 planned 0 postponed 1 not sent returns not sent",
			notifications: []models.Notification{
				buildNotification(1, 1, withStatus(models.NotificationStatusReadyToSend)),
			},
			plannedLimit:            1,
			postponedLimit:          1,
			notSentLimit:            1,
			expectedNotificationIds: []int{1},
		},
		{
			name: "notification was postponed and then not sent returns it",
			notifications: []models.Notification{
				buildNotification(1, 1, withStatus(models.NotificationStatusReadyToSend), postpone(clock.Now())),
			},
			plannedLimit:            1,
			notSentLimit:            1,
			expectedNotificationIds: []int{1},
		},
		{
			name: "more than limits",
			notifications: []models.Notification{
				buildNotification(1, 1, withStatus(models.NotificationStatusPlanned), notify(clock.Now().Add(-2*time.Second))),
				buildNotification(2, 2, withStatus(models.NotificationStatusPlanned), notify(clock.Now().Add(-1*time.Second))),
				buildNotification(3, 3, withStatus(models.NotificationStatusPlanned), postpone(clock.Now().Add(-2*time.Second))),
				buildNotification(4, 4, withStatus(models.NotificationStatusPlanned), postpone(clock.Now().Add(-1*time.Second))),
				buildNotification(5, 5, withStatus(models.NotificationStatusReadyToSend), notify(clock.Now().Add(-2*time.Second))),
				buildNotification(6, 6, withStatus(models.NotificationStatusReadyToSend), notify(clock.Now().Add(-1*time.Second))),
			},
			plannedLimit:            1,
			postponedLimit:          1,
			notSentLimit:            1,
			expectedNotificationIds: []int{1, 3, 5},
		},
	}

	for _, c := range getFirstNTestCases {
		t.Run(
			"GetFirstN/"+c.name, withEmptyDB(
				func(t *testing.T) {
					ctx, cancelFunc := context.WithTimeout(context.Background(), dbOperationTimeout)
					defer cancelFunc()

					err := storeNotifications(testPgClient, c.notifications)
					require.NoError(t, err)

					gotNotifications, err := repository.GetFirstN(ctx, clock.Now(), c.plannedLimit, c.postponedLimit, c.notSentLimit)

					require.NoError(t, err)
					require.Equal(t, len(c.expectedNotificationIds), len(gotNotifications))
					for i, expectedID := range c.expectedNotificationIds {
						require.EqualValues(t, expectedID, gotNotifications[i].ID)
					}
				},
			),
		)
	}
}

type testableFields struct {
	recipientID *int32
	orderType   models.OrderType
	status      models.NotificationStatus
	notifyAt    string
	userID      *uint64
	orderID     *string
}

func TestNotificationsRepository_GetForRecipient(t *testing.T) {
	testPgClient := getTestPgClient()
	repository := NewNotificationsRepository(testPgClient, true)
	dbOperationTimeout := getDBTestTimeout()
	withEmptyDB := newDBCleaner(testPgClient)

	buildNotification := func(recipientID int32, orderType models.OrderType, notifyAt time.Time) models.Notification {
		return models.Notification{
			Type:      models.NotificationTypePretrip,
			Subtype:   models.NotificationAdhoc,
			Order:     &models.Order{ID: fmt.Sprintf("%d", recipientID), Type: orderType},
			Channel:   models.NotificationChannelEmail,
			Status:    models.NotificationStatusPlanned,
			Recipient: &models.Recipient{ID: recipientID, Email: ptr.String(fmt.Sprintf("%d", recipientID))},
			NotifyAt:  notifyAt,
		}
	}
	now := time.Date(2021, 1, 10, 12, 15, 0, 0, time.UTC)

	t.Run(
		"shall filter by the recipient", withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbOperationTimeout)
				defer cancelFunc()

				notifyAt := now.Add(time.Hour)
				for i := int32(1); i <= 3; i++ {
					_, err := repository.Create(
						ctx,
						buildNotification(i, models.OrderHotel, notifyAt),
					)
					require.NoError(t, err)
				}

				notifications, err := repository.GetForRecipient(ctx, 1, now, notifyAt)
				require.NoError(t, err)
				expected := []testableFields{
					{
						recipientID: ptr.Int32(1),
						orderType:   models.OrderHotel,
						status:      models.NotificationStatusPlanned,
						notifyAt:    "10 Jan 21 13:15 UTC",
						orderID:     ptr.String("1"),
					},
				}
				require.EqualValues(t, expected, toTestableFields(notifications))

				notifications, err = repository.GetForOrder(ctx, "2", now, notifyAt, true)
				require.NoError(t, err)
				expected = []testableFields{
					{
						recipientID: ptr.Int32(2),
						orderType:   models.OrderHotel,
						status:      models.NotificationStatusPlanned,
						notifyAt:    "10 Jan 21 13:15 UTC",
						orderID:     ptr.String("2"),
					},
				}
				require.EqualValues(t, expected, toTestableFields(notifications))
			},
		),
	)

	t.Run(
		"shall filter by time", withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbOperationTimeout)
				defer cancelFunc()

				notifyAt := now.Add(time.Hour)
				_, err := repository.Create(
					ctx,
					buildNotification(1, models.OrderHotel, notifyAt),
				)
				require.NoError(t, err)

				notifications, err := repository.GetForRecipient(ctx, 1, now, notifyAt.Add(-time.Second))
				require.NoError(t, err)
				require.EqualValues(t, []testableFields{}, toTestableFields(notifications))
			},
		),
	)
}

func TestNotificationsRepository_AlreadySentForOrder(t *testing.T) {
	testPgClient := getTestPgClient()
	repository := NewNotificationsRepository(testPgClient, true)
	dbOperationTimeout := getDBTestTimeout()
	withEmptyDB := newDBCleaner(testPgClient)
	testOrderID := "TestOrder"

	buildNotification := func(status models.NotificationStatus, notifyAt time.Time) models.Notification {
		return models.Notification{
			Type:      models.NotificationTypePretrip,
			Subtype:   models.NotificationAdhoc,
			Order:     &models.Order{ID: testOrderID, Type: models.OrderHotel},
			Channel:   models.NotificationChannelEmail,
			Status:    status,
			Recipient: &models.Recipient{ID: 1, Email: ptr.String("1@")},
			NotifyAt:  notifyAt,
		}
	}
	now := time.Date(2021, 1, 10, 12, 15, 0, 0, time.UTC)

	t.Run(
		"shall filter by the status", withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbOperationTimeout)
				defer cancelFunc()

				notifyAt := now.Add(time.Hour)
				for _, status := range models.AllNotificationStatuses {
					_, err := repository.Create(
						ctx,
						buildNotification(status, notifyAt),
					)
					require.NoError(t, err)
				}

				notifications, err := repository.AlreadySentForOrder(ctx, testOrderID, models.NotificationTypePretrip)
				require.NoError(t, err)
				expected := []testableFields{
					{
						recipientID: ptr.Int32(1),
						orderType:   models.OrderHotel,
						status:      models.NotificationStatusSent,
						notifyAt:    "10 Jan 21 13:15 UTC",
						orderID:     &testOrderID,
					},
				}
				require.EqualValues(t, expected, toTestableFields(notifications))

				wrongOrderID := fmt.Sprintf("%s+1", testOrderID)
				notifications, err = repository.AlreadySentForOrder(ctx, wrongOrderID, models.NotificationTypePretrip)
				require.NoError(t, err)
				expected = []testableFields{}
				require.EqualValues(t, expected, toTestableFields(notifications))
			},
		),
	)
}

func TestNotificationsRepository_GetPullNotificationsByOrderIDs(t *testing.T) {
	testPgClient := getTestPgClient()
	repository := NewNotificationsRepository(testPgClient, true)
	dbOperationTimeout := getDBTestTimeout()
	withEmptyDB := newDBCleaner(testPgClient)

	now := time.Date(2021, 1, 10, 12, 15, 0, 0, time.UTC)
	notifyAt := now.Add(time.Hour)
	deadline := notifyAt.Add(time.Hour)

	buildNotification := func(orderID string, dispatchType models.DispatchType, status models.NotificationStatus) models.Notification {
		return models.Notification{
			Type:         models.NotificationTypeOnlineRegistration,
			DispatchType: dispatchType,
			User:         &models.User{ID: 1, PassportID: ptr.String("passport_id")},
			Channel:      models.NotificationChannelPullAPI,
			Status:       status,
			Order:        &models.Order{ID: orderID, Type: models.OrderAvia},
			NotifyAt:     notifyAt,
			Deadline:     deadline,
		}
	}

	t.Run(
		"shall filter by order ids", withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbOperationTimeout)
				defer cancelFunc()

				for i := 1; i <= 2; i++ {
					_, err := repository.Create(
						ctx,
						buildNotification(strconv.Itoa(i), models.DispatchTypePull, models.NotificationStatusPlanned),
					)
					require.NoError(t, err)
				}

				notifications, err := repository.GetPullNotificationsByOrderIDs(ctx, []string{"1"}, notifyAt)
				require.NoError(t, err)
				expected := []testableFields{
					{
						userID:    ptr.Uint64(1),
						orderID:   ptr.String("1"),
						orderType: models.OrderAvia,
						status:    models.NotificationStatusPlanned,
						notifyAt:  "10 Jan 21 13:15 UTC",
					},
				}
				require.EqualValues(t, expected, toTestableFields(notifications))
			},
		),
	)

	t.Run(
		"shall filter by status", withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbOperationTimeout)
				defer cancelFunc()

				_, err := repository.Create(
					ctx,
					buildNotification("1", models.DispatchTypePull, models.NotificationStatusCancelled),
				)
				require.NoError(t, err)

				notifications, err := repository.GetPullNotificationsByOrderIDs(ctx, []string{"1"}, notifyAt)
				require.NoError(t, err)
				require.EqualValues(t, []testableFields{}, toTestableFields(notifications))
			},
		),
	)

	t.Run(
		"shall filter by dispatch type", withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbOperationTimeout)
				defer cancelFunc()

				_, err := repository.Create(
					ctx,
					buildNotification("1", models.DispatchTypePush, models.NotificationStatusPlanned),
				)
				require.NoError(t, err)

				notifications, err := repository.GetPullNotificationsByOrderIDs(ctx, []string{"1"}, notifyAt)
				require.NoError(t, err)
				require.EqualValues(t, []testableFields{}, toTestableFields(notifications))
			},
		),
	)

	t.Run(
		"shall filter by time", withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbOperationTimeout)
				defer cancelFunc()

				notifyAt := now.Add(time.Hour)
				deadline := notifyAt.Add(time.Hour)
				_, err := repository.Create(
					ctx,
					buildNotification("1", models.DispatchTypePull, models.NotificationStatusPlanned),
				)
				require.NoError(t, err)

				notifications, err := repository.GetPullNotificationsByOrderIDs(ctx, []string{"1"}, now.Add(-time.Second))
				require.NoError(t, err)
				require.EqualValues(t, []testableFields{}, toTestableFields(notifications))

				notifications, err = repository.GetPullNotificationsByOrderIDs(ctx, []string{"1"}, deadline)
				require.NoError(t, err)
				require.EqualValues(t, []testableFields{}, toTestableFields(notifications))
			},
		),
	)
}

func TestNotificationsRepository_GetPullNotificationsByPassportID(t *testing.T) {
	testPgClient := getTestPgClient()
	repository := NewNotificationsRepository(testPgClient, true)
	dbOperationTimeout := getDBTestTimeout()
	withEmptyDB := newDBCleaner(testPgClient)

	now := time.Date(2021, 1, 10, 12, 15, 0, 0, time.UTC)
	notifyAt := now.Add(time.Hour)
	deadline := notifyAt.Add(time.Hour)

	buildNotification := func(passportID uint64, dispatchType models.DispatchType, status models.NotificationStatus) models.Notification {
		passportIDStr := ptr.String(fmt.Sprintf("%d", passportID))
		return models.Notification{
			Type:         models.NotificationTypeOnlineRegistration,
			DispatchType: dispatchType,
			User:         &models.User{ID: passportID, PassportID: passportIDStr},
			Channel:      models.NotificationChannelPullAPI,
			Status:       status,
			Order:        &models.Order{ID: fmt.Sprintf("%d", passportID), Type: models.OrderAvia},
			NotifyAt:     notifyAt,
			Deadline:     deadline,
		}
	}

	t.Run(
		"shall filter by the passport id", withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbOperationTimeout)
				defer cancelFunc()

				for i := uint64(1); i <= 2; i++ {
					_, err := repository.Create(
						ctx,
						buildNotification(i, models.DispatchTypePull, models.NotificationStatusPlanned),
					)
					require.NoError(t, err)
				}

				notifications, err := repository.GetPullNotificationsByPassportID(ctx, "1", notifyAt)
				require.NoError(t, err)
				expected := []testableFields{
					{
						userID:    ptr.Uint64(1),
						orderType: models.OrderAvia,
						status:    models.NotificationStatusPlanned,
						notifyAt:  "10 Jan 21 13:15 UTC",
						orderID:   ptr.String("1"),
					},
				}
				require.EqualValues(t, expected, toTestableFields(notifications))
			},
		),
	)

	t.Run(
		"shall filter by status", withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbOperationTimeout)
				defer cancelFunc()

				_, err := repository.Create(
					ctx,
					buildNotification(1, models.DispatchTypePull, models.NotificationStatusCancelled),
				)
				require.NoError(t, err)

				notifications, err := repository.GetPullNotificationsByPassportID(ctx, "1", notifyAt)
				require.NoError(t, err)
				require.EqualValues(t, []testableFields{}, toTestableFields(notifications))
			},
		),
	)

	t.Run(
		"shall filter by dispatch type", withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbOperationTimeout)
				defer cancelFunc()

				_, err := repository.Create(
					ctx,
					buildNotification(1, models.DispatchTypePush, models.NotificationStatusPlanned),
				)
				require.NoError(t, err)

				notifications, err := repository.GetPullNotificationsByPassportID(ctx, "1", notifyAt)
				require.NoError(t, err)
				require.EqualValues(t, []testableFields{}, toTestableFields(notifications))
			},
		),
	)

	t.Run(
		"shall filter by time", withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbOperationTimeout)
				defer cancelFunc()

				notifyAt := now.Add(time.Hour)
				deadline := notifyAt.Add(time.Hour)
				_, err := repository.Create(
					ctx,
					buildNotification(1, models.DispatchTypePull, models.NotificationStatusPlanned),
				)
				require.NoError(t, err)

				notifications, err := repository.GetPullNotificationsByPassportID(ctx, "1", now.Add(-time.Second))
				require.NoError(t, err)
				require.EqualValues(t, []testableFields{}, toTestableFields(notifications))

				notifications, err = repository.GetPullNotificationsByPassportID(ctx, "1", deadline)
				require.NoError(t, err)
				require.EqualValues(t, []testableFields{}, toTestableFields(notifications))
			},
		),
	)
}

func toTestableFields(notifications []models.Notification) []testableFields {
	result := make([]testableFields, 0)
	for _, notification := range notifications {
		result = append(result, testableFields{
			recipientID: notification.RecipientID,
			orderType:   notification.Order.Type,
			status:      notification.Status,
			notifyAt:    notification.NotifyAt.In(time.UTC).Format(time.RFC822),
			userID:      notification.UserID,
			orderID:     notification.OrderID,
		})
	}
	return result
}

func storeNotifications(testPgClient *pgclient.PGClient, notifications []models.Notification) error {
	return testPgClient.ExecuteInTransaction(
		hasql.Primary, func(db *gorm.DB) error {
			return db.CreateInBatches(notifications, len(notifications)).Error
		},
	)
}

func notificationByID(testPgClient *pgclient.PGClient, id uint64) (notification models.Notification, err error) {
	err = testPgClient.ExecuteInTransaction(
		hasql.Primary, func(db *gorm.DB) error {
			return db.Preload(clause.Associations).First(&notification, &models.Notification{ID: id}).Error
		},
	)
	return
}
