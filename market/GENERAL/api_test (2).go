package api

import (
	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/log/zap"
	"context"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"
)

func TestAPI(t *testing.T) {
	logger, _ := zap.New(zap.ConsoleConfig(log.InfoLevel))
	a, err := NewAPI(logger, &Config{
		ClientExpireDays:   0,
		ClientCertificates: nil,
	})
	require.NoError(t, err)
	require.NotNil(t, a)

	ts := httptest.NewTLSServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {}))
	a, err = NewAPI(logger, &Config{
		ClientExpireDays:   0,
		ClientCertificates: ts.TLS.Certificates,
	})
	require.NoError(t, err)

	a.NewEcho()

	assert.NotNil(t, a.Logger)
	assert.NotNil(t, a.Echo)

	startChan := make(chan bool, 10)
	ctx, cancel := context.WithTimeout(context.Background(), time.Second*5)
	defer cancel()

	a.StartEcho("invalid:address", func(err error) {
		assert.Error(t, err)
		startChan <- true
	})

	select {
	case <-startChan:
		cancel()
	case <-ctx.Done():
		t.Error("Expected error from a.StartEcho")
		t.Fail()
	}

	a.StopEcho()
}
