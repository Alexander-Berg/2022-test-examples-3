package midleware

import (
	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/xerrors"
	"github.com/labstack/echo/v4"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

type LoggerMock struct {
	log.Logger
	mock.Mock
}

func (l *LoggerMock) Info(msg string, fields ...log.Field) {
	l.Called(msg, fields)
}

func TestLogger(t *testing.T) {
	logger := new(LoggerMock)
	logger.On("Info", mock.Anything, mock.Anything).Twice()

	e := echo.New()

	req := httptest.NewRequest(http.MethodGet, "/ping", nil)
	rec := httptest.NewRecorder()
	c := e.NewContext(req, rec)

	next := func(ctx echo.Context) error {
		return ctx.String(http.StatusOK, "")
	}

	assert.NoError(t, LoggerMiddleware(logger)(next)(c))

	req = httptest.NewRequest(http.MethodPost, "/", strings.NewReader(""))
	req.URL.Path = ""
	rec = httptest.NewRecorder()
	c = e.NewContext(req, rec)

	next = func(ctx echo.Context) error {
		return xerrors.New("test")
	}

	assert.Error(t, LoggerMiddleware(logger)(next)(c))
}
