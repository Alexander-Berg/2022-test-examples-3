package api

import (
	"github.com/labstack/echo/v4"
	"github.com/stretchr/testify/assert"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestPing(t *testing.T) {
	a := new(API)
	a.Echo = echo.New()

	req := httptest.NewRequest(http.MethodGet, "/ping", nil)
	rec := httptest.NewRecorder()
	c := a.Echo.NewContext(req, rec)

	assert.NoError(t, Ping(a)(c))
	assert.Equal(t, http.StatusOK, rec.Code)
	assert.Equal(t, "0;OK", rec.Body.String())
}
