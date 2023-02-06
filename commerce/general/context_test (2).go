package middlewares

import (
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/labstack/echo/v4"
	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/commerce/blogs_pumpkin/context"
)

func TestContext_ShouldWrapContext(t *testing.T) {
	ts := httptest.NewServer(http.HandlerFunc(func(res http.ResponseWriter, req *http.Request) {
		res.WriteHeader(http.StatusOK)
	}))

	defer ts.Close()

	e := echo.New()
	request := httptest.NewRequest(http.MethodGet, "/", nil)
	recorder := httptest.NewRecorder()
	c := e.NewContext(request, recorder)

	handler := Context(func(c echo.Context) error {
		_, ok := c.(context.Context)

		assert.True(t, ok)

		return nil
	})

	assert.NoError(t, handler(c))
}
