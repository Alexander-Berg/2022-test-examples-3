package handlers

import (
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"

	"a.yandex-team.ru/library/go/core/xerrors"
	"a.yandex-team.ru/travel/avia/flight_status_receiver/pkg"
)

type defaultHandlerTestMock struct {
	mock.Mock
}

func (m *defaultHandlerTestMock) Pipeline(string) (pkg.Pipeline, error) {
	return m, nil
}
func (m *defaultHandlerTestMock) Handle(b []byte, c context.Context) error {
	args := m.Called(b, c)
	return args.Error(0)
}

func TestDefaultHandler(t *testing.T) {
	m := &defaultHandlerTestMock{}
	m.On(
		"Handle",
		mock.MatchedBy(func([]byte) bool { return true }),
		mock.MatchedBy(func(ctx context.Context) bool { return true }),
	).Return(
		xerrors.Errorf(
			"wrapped error: %w",
			pkg.NewErrorWithHTTPCode("description", xerrors.New("error"), 404),
		),
	)
	expect := assert.New(t)
	handler, err := DefaultHandler(
		m,
		"propline",
	)
	expect.NoError(err)
	expect.NotNil(handler)
	request, err := http.NewRequest("POST", "random-url", strings.NewReader("some request data"))
	expect.NoError(err)
	responseRecorder := httptest.NewRecorder()
	handler.ServeHTTP(responseRecorder, request)
	expect.Equal(404, responseRecorder.Code)
	responseBody := responseRecorder.Body.Bytes()
	var responseBodyValue string
	err = json.Unmarshal(responseBody, &responseBodyValue)
	expect.NoError(err)
	expect.Equal("description: error", responseBodyValue)

}
