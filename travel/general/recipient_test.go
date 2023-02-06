package models

import (
	"testing"
	"time"

	"github.com/jonboulle/clockwork"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/ptr"
)

func TestRecipient(t *testing.T) {
	clock := clockwork.NewFakeClock()

	t.Run(
		"Subscribe/isn't subscribed", func(t *testing.T) {
			recipient := Recipient{
				Email:                ptr.String("email"),
				IsSubscribed:         false,
				SubscriptionSource:   nil,
				SubscriptionVertical: nil,
				SubscribedAt:         nil,
				UnsubscribeHash:      nil,
			}

			recipient.Subscribe(
				"source", "vertical", "nv", "lang", "UTC", clock.Now(), func(s string) string {
					return "hash"
				},
			)

			require.True(t, recipient.IsSubscribed)
			require.Equal(t, "source", *recipient.SubscriptionSource)
			require.Equal(t, "vertical", *recipient.SubscriptionVertical)
			require.Equal(t, "nv", *recipient.NationalVersion)
			require.Equal(t, "lang", *recipient.Language)
			require.Equal(t, "UTC", *recipient.Timezone)
			require.Equal(t, clock.Now(), *recipient.SubscribedAt)
			require.Equal(t, "hash", *recipient.UnsubscribeHash)
		},
	)

	t.Run(
		"Subscribe/is subscribed shouldn't change hash", func(t *testing.T) {
			recipient := Recipient{
				IsSubscribed:         true,
				Email:                ptr.String("email1"),
				SubscriptionSource:   ptr.String("source1"),
				SubscriptionVertical: ptr.String("vertical1"),
				NationalVersion:      ptr.String("nv1"),
				Language:             ptr.String("lang1"),
				Timezone:             ptr.String("tz1"),
				SubscribedAt:         ptr.Time(clock.Now()),
				UnsubscribeHash:      ptr.String("hash1"),
			}

			recipient.Subscribe(
				"source2", "vertical2", "nv2", "lang2", "tz2", clock.Now().Add(time.Hour), func(s string) string {
					return "hash2"
				},
			)

			require.True(t, recipient.IsSubscribed)
			require.Equal(t, "source2", *recipient.SubscriptionSource)
			require.Equal(t, "vertical2", *recipient.SubscriptionVertical)
			require.Equal(t, "nv2", *recipient.NationalVersion)
			require.Equal(t, "lang2", *recipient.Language)
			require.Equal(t, "tz2", *recipient.Timezone)
			require.Equal(t, clock.Now().Add(time.Hour), *recipient.SubscribedAt)
			require.Equal(t, "hash1", *recipient.UnsubscribeHash)
		},
	)
}
