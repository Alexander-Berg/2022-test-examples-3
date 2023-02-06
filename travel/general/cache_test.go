package client

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

type TestKey struct {
	Field1 string
	Field2 int
}

func TestClientStateStorage(t *testing.T) {

	const (
		clientRequestStateUnknown = iota
		clientRequestStateRequested
		clientRequestStateRequestDone
	)

	timeout := time.Millisecond * 100
	s := NewClientStateStorage(clientRequestStateUnknown, timeout)
	ctx, ctxCancel := context.WithCancel(context.Background())
	defer ctxCancel()
	s.Run(ctx)

	t.Run("CheckSetGet", func(t *testing.T) {
		req1 := TestKey{
			Field1: "123",
			Field2: 234,
		}
		req2 := TestKey{
			Field1: "1234",
			Field2: 2345,
		}
		s.Set(req1, clientRequestStateRequested)
		assert.Equal(t, clientRequestStateRequested, s.Get(req1))
		assert.Equal(t, clientRequestStateUnknown, s.Get(req2))

		s.Set(req2, clientRequestStateRequestDone)
		assert.Equal(t, clientRequestStateRequested, s.Get(req1))
		assert.Equal(t, clientRequestStateRequestDone, s.Get(req2))

		time.Sleep(timeout * 2)
		assert.Equal(t, clientRequestStateUnknown, s.Get(req1))
		assert.Equal(t, clientRequestStateUnknown, s.Get(req2))
	})
}
