package cache

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"go.uber.org/goleak"

	tcpb "a.yandex-team.ru/travel/buses/backend/proto/testcontext"
)

func TestCache(t *testing.T) {

	cache := NewTestContextStorage()

	t.Run("Cache set and get", func(t *testing.T) {

		token, err := cache.Set("", &tcpb.TTestContextBookParamsPayload{})
		if !assert.NoError(t, err) ||
			!assert.NotEmpty(t, token) ||
			!assert.Equal(t, 1, cache.Len()) {
			return
		}

		_, err = cache.Set(token, &tcpb.TTestContextSetBookParamsRequest{TestContextToken: "1"})
		if !assert.NoError(t, err) ||
			!assert.Equal(t, 1, cache.Len()) {
			return
		}

		message := tcpb.TTestContextSetBookParamsRequest{}
		ok, err := cache.Get(token, &message)
		if !assert.NoError(t, err) ||
			!assert.True(t, ok) ||
			!assert.Equal(t, "1", message.TestContextToken) {
			return
		}

		ok, err = cache.Get("unknownToken", &message)
		if !assert.NoError(t, err) ||
			!assert.False(t, ok) {
			return
		}

		unknownMessage := tcpb.TTestContextGetBookParamsResponse{}
		ok, err = cache.Get(token, &unknownMessage)
		if !assert.NoError(t, err) ||
			!assert.False(t, ok) {
			return
		}
	})

	t.Run("Cache dump and load", func(t *testing.T) {
		cache2 := NewTestContextStorage()
		cnt := 0
		for dump := range cache.Iter(context.Background()) {
			cnt += 1
			cache2.Add(dump)
		}

		if !assert.Equal(t, 1, cache2.Len()) ||
			!assert.Equal(t, 2, cnt) {
			return
		}
	})

	t.Run("Interruption case", func(t *testing.T) {

		ctx, cancel := context.WithCancel(context.Background())
		cancel()

		itemsChannel := cache.Iter(ctx)
		time.Sleep(20 * time.Millisecond)

		cnt := 0
		for range itemsChannel {
			cnt += 1
		}
		assert.Equal(t, 0, cnt, "Dump capacity should be equal %d, but equal %d", 0, cnt)
		goleak.VerifyNone(t)
	})
}
