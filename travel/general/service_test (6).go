package pretrip

import (
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/notifier/internal/models"
)

func TestService_AlreadySent(t *testing.T) {
	type testData struct {
		name          string
		notifications []models.Notification
		expected      bool
	}

	tests := []testData{
		{
			name:          "nothing has been sent ever before",
			notifications: []models.Notification{},
			expected:      false,
		},
		{
			name: "any two letters have been sent",
			notifications: []models.Notification{
				{
					Subtype: models.NotificationWeekBefore,
				},
				{
					Subtype: models.NotificationWeekBefore,
				},
			},
			expected: true,
		},
		{
			name: "week-before letter has been sent",
			notifications: []models.Notification{
				{
					Subtype: models.NotificationWeekBefore,
				},
			},
			expected: false,
		},
		{
			name: "adhoc letter has been sent",
			notifications: []models.Notification{
				{
					Subtype: models.NotificationAdhoc,
				},
			},
			expected: true,
		},
		{
			name: "day-before letter has been sent",
			notifications: []models.Notification{
				{
					Subtype: models.NotificationDayBefore,
				},
			},
			expected: true,
		},
	}
	for _, test := range tests {
		t.Run(
			test.name, func(t *testing.T) {
				result := isAlreadySent(test.notifications)
				assert.Equal(t, test.expected, result)
			},
		)
	}
}
