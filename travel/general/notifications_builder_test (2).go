package scheduling

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/library/go/containers"
	"a.yandex-team.ru/travel/notifier/internal/models"
)

type testableFields struct {
	recipientID int32
	orderID     string
	notifyAt    string
	status      models.NotificationStatus
	subtype     models.NotificationSubtype
}

func TestNotificationBuilder_Build(t *testing.T) {
	now := time.Date(2021, 1, 19, 12, 30, 0, 0, time.UTC)
	recipient := models.Recipient{ID: 111}.WithEmail("qqq@")

	t.Run(
		"return empty list for non-fulfilled orders", func(t *testing.T) {
			builder := NotificationsBuilder{}
			result, err := builder.Build(models.Order{State: models.OrderIncomplete}, recipient, now)
			assert.NoError(t, err)
			assert.EqualValues(t, []models.Notification{}, result)
		},
	)

	t.Run(
		"return empty list for orders with zero start time", func(t *testing.T) {
			builder := NotificationsBuilder{}
			result, err := builder.Build(models.Order{State: models.OrderFulfilled}, recipient, now)
			assert.NoError(t, err)
			assert.EqualValues(t, []models.Notification{}, result)
		},
	)

	t.Run(
		"return empty list for orders with start time less than now+3hrs", func(t *testing.T) {
			builder := NotificationsBuilder{}
			order := models.Order{
				State:     models.OrderFulfilled,
				StartDate: now.Add(time.Hour),
			}
			result, err := builder.Build(order, recipient, now)
			assert.NoError(t, err)
			assert.EqualValues(t, []models.Notification{}, result)
		},
	)

	t.Run(
		"return single notification for orders with start time between +3hrs and +8days", func(t *testing.T) {
			builder := NotificationsBuilder{}
			order := models.Order{
				ID:        "1",
				State:     models.OrderFulfilled,
				StartDate: now.Add(4 * time.Hour),
			}
			result, err := builder.Build(order, recipient, now)
			assert.NoError(t, err)
			expected := []testableFields{
				{
					recipientID: 111,
					orderID:     "1",
					notifyAt:    "19 Jan 21 15:30 UTC",
					status:      models.NotificationStatusPlanned,
					subtype:     models.NotificationAdhoc,
				},
			}
			assert.EqualValues(t, expected, toTestableFields(result))
		},
	)

	t.Run(
		"return two notifications for orders with start time after now+8days", func(t *testing.T) {
			builder := NotificationsBuilder{}
			order := models.Order{
				ID:        "1",
				State:     models.OrderFulfilled,
				StartDate: time.Date(2021, 2, 19, 12, 30, 0, 0, time.UTC),
			}
			result, err := builder.Build(order, recipient, now)
			assert.NoError(t, err)
			expected := []testableFields{
				{
					recipientID: 111,
					orderID:     "1",
					notifyAt:    "12 Feb 21 12:30 UTC",
					status:      models.NotificationStatusPlanned,
					subtype:     models.NotificationWeekBefore,
				},
				{
					recipientID: 111,
					orderID:     "1",
					notifyAt:    "18 Feb 21 12:30 UTC",
					status:      models.NotificationStatusPlanned,
					subtype:     models.NotificationDayBefore,
				},
			}
			assert.EqualValues(t, expected, toTestableFields(result))
		},
	)

	t.Run(
		"is testing and recipient email contains in testing emails - uses testing intervals for week- and day-before", func(t *testing.T) {
			builder := NewNotificationsBuilder(
				NotificationsBuilderConfig{
					IsTesting:                 true,
					TestingEmails:             containers.SetOf("qqq@"),
					WeekBeforeSendingInterval: 2 * time.Minute,
					DayBeforeSendingInterval:  4 * time.Minute,
				},
			)
			order := models.Order{
				ID:        "1",
				State:     models.OrderFulfilled,
				StartDate: time.Date(2021, 2, 19, 12, 30, 0, 0, time.UTC),
			}
			result, err := builder.Build(order, recipient, now)
			assert.NoError(t, err)
			expected := []testableFields{
				{
					recipientID: 111,
					orderID:     "1",
					notifyAt:    "19 Jan 21 12:32 UTC",
					status:      models.NotificationStatusPlanned,
					subtype:     models.NotificationWeekBefore,
				},
				{
					recipientID: 111,
					orderID:     "1",
					notifyAt:    "19 Jan 21 12:34 UTC",
					status:      models.NotificationStatusPlanned,
					subtype:     models.NotificationDayBefore,
				},
			}
			assert.EqualValues(t, expected, toTestableFields(result))
		},
	)

	t.Run(
		"is testing and recipient email contains in testing emails - uses testing intervals for adhoc", func(t *testing.T) {
			builder := NewNotificationsBuilder(
				NotificationsBuilderConfig{
					IsTesting:            true,
					TestingEmails:        containers.SetOf("qqq@"),
					AdhocSendingInterval: 2 * time.Minute,
				},
			)
			order := models.Order{
				ID:        "1",
				State:     models.OrderFulfilled,
				StartDate: now.Add(4 * time.Hour),
			}
			result, err := builder.Build(order, recipient, now)
			assert.NoError(t, err)
			expected := []testableFields{
				{
					recipientID: 111,
					orderID:     "1",
					notifyAt:    "19 Jan 21 12:32 UTC",
					status:      models.NotificationStatusPlanned,
					subtype:     models.NotificationAdhoc,
				},
			}
			assert.EqualValues(t, expected, toTestableFields(result))
		},
	)
}

func toTestableFields(notifications []models.Notification) []testableFields {
	result := make([]testableFields, 0, len(notifications))
	for _, notification := range notifications {
		testable := testableFields{
			notifyAt:    notification.NotifyAt.In(time.UTC).Format(time.RFC822),
			orderID:     notification.Order.ID,
			recipientID: notification.Recipient.ID,
			status:      models.NotificationStatusPlanned,
			subtype:     notification.Subtype,
		}
		result = append(result, testable)
	}
	return result
}
