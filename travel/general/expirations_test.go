package cache

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestTariffExpirations_Empty(t *testing.T) {
	expirations := NewTariffExpirations()
	assert.True(t, expirations.Empty())

	expirations.Push(&tariffExpiration{
		expireAt: time.Time{},
		routeKey: tariffRouteKey{},
	})
	assert.False(t, expirations.Empty())

	expirations.Pop()
	assert.True(t, expirations.Empty())
}

func TestTariffExpirations_Head(t *testing.T) {
	expireAts := []time.Time{
		shortDate(2020, 1, 4),
		shortDate(2020, 1, 5),
		shortDate(2020, 1, 6),
	}
	expirations := NewTariffExpirations()

	for _, i := range []int{1, 0, 2} {
		expirations.Push(&tariffExpiration{
			expireAt: expireAts[i],
			routeKey: tariffRouteKey{},
		})
	}

	for _, expireAt := range expireAts {
		require.False(t, expirations.Empty())
		assert.Equal(t, expireAt, expirations.Head().expireAt)
		expirations.Pop()
	}
}

func TestExpirationOrder(t *testing.T) {
	expireAts := []time.Time{
		shortDate(2020, 1, 4),
		shortDate(2020, 1, 5),
		shortDate(2020, 1, 6),
	}
	expirations := NewTariffExpirations()

	for _, i := range []int{1, 0, 2} {
		expirations.Push(&tariffExpiration{
			expireAt: expireAts[i],
			routeKey: tariffRouteKey{},
		})
	}

	for _, expireAt := range expireAts {
		require.False(t, expirations.Empty())
		assert.Equal(t, expireAt, expirations.Pop().expireAt)
	}
}
