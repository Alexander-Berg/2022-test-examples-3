package client

import (
	"context"
	"encoding/json"
	"net/http"
	"testing"
	"time"

	"github.com/go-chi/chi/v5/middleware"
	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/log/zap"
)

type testRequest struct {
	Arg string `json:"arg"`
}

type testResponce struct {
	Data string `json:"data"`
}

func makeTransportMock(statusCode int, respBody []byte, callback func()) *SimpleTransportMock {
	return &SimpleTransportMock{
		statusCode: statusCode,
		respBody:   respBody,
		callback:   callback,
		requests:   []*http.Request{},
	}
}

func TestHttpClient(t *testing.T) {

	const baseURL = "https://production.pathfinder-proxy.rasp.common.yandex.net/"
	const path = "path/path"
	logger, _ := zap.New(zap.ConsoleConfig(log.InfoLevel))
	ctx := context.Background()

	t.Run("StatusOK", func(t *testing.T) {

		respBody, _ := json.Marshal(&testResponce{Data: "data"})
		transport := makeTransportMock(http.StatusOK, respBody, nil)
		client, err := NewHTTPClientWithTransport(baseURL, time.Minute, transport, 0, 0, ContentTypeJSON, logger, nil)
		assert.NoError(t, err)

		resp := testResponce{}
		err = client.Get(ctx, path, &testRequest{Arg: "4"}, &resp)
		assert.NoError(t, err)
		assert.Equal(t, "data", resp.Data)
	})

	t.Run("StatusAccepted", func(t *testing.T) {

		transport := makeTransportMock(http.StatusAccepted, []byte("{"), nil)
		client, err := NewHTTPClientWithTransport(baseURL, time.Minute, transport, 0, 0, ContentTypeJSON, logger, nil)
		assert.NoError(t, err)

		resp := testResponce{}
		err = client.Get(ctx, path, &testRequest{}, &resp)
		assert.IsType(t, &RetryableError{}, err)
		assert.Equal(t, len(transport.requests), 1)
	})

	t.Run("ContextHeaders", func(t *testing.T) {
		header := http.Header{}
		header.Add("X-YA-H1", "test-ya-h1")
		ctx = context.WithValue(ctx, middleware.RequestIDKey, "test-req-id1")
		respBody, _ := json.Marshal(&testResponce{Data: "data"})
		transport := makeTransportMock(http.StatusOK, respBody, nil)
		client, err := NewHTTPClientWithTransport(baseURL, time.Minute, transport, 0, 0, ContentTypeJSON, logger, nil)
		assert.NoError(t, err)

		resp := testResponce{}
		err = client.GetWithHeader(ctx, path, &testRequest{Arg: "4"}, header, &resp)
		assert.NoError(t, err)
		assert.Equal(t, 1, len(transport.requests))
		assert.Equal(t, "test-ya-h1", transport.requests[0].Header.Get("X-YA-H1"))
		assert.Equal(t, "test-req-id1", transport.requests[0].Header.Get("X-Request-Id"))
	})

	//t.Run("CircuitBreaker", func(t *testing.T) {
	//
	//	counter := 0
	//	transport := makeTransportMock(http.StatusInternalServerError, []byte("{"), func() {
	//		counter++
	//	})
	//	client, err := NewHTTPClientWithTransport(baseURL, time.Minute, transport, 2, 10*time.Millisecond, logger)
	//	assert.NoError(t, err)
	//
	//	resp := testResponce{}
	//	for i := 0; i < 100; i++ {
	//		err = client.Get(ctx, path, &testRequest{}, &resp)
	//		assert.IsType(t, &NotRetryableError{}, err)
	//	}
	//	assert.Equal(t, 2, counter)
	//	time.Sleep(20 * time.Millisecond)
	//	for i := 0; i < 100; i++ {
	//		err = client.Get(ctx, path, &testRequest{}, &resp)
	//		assert.IsType(t, &NotRetryableError{}, err)
	//	}
	//	assert.Equal(t, 3, counter)
	//})
}
