package middlewares

import (
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/labstack/echo/v4"
	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/commerce/blogs_pumpkin/controllers"
)

type ProxyRecorder struct {
	*httptest.ResponseRecorder
}

func NewProxyRecorder() http.ResponseWriter {
	return &ProxyRecorder{httptest.NewRecorder()}
}

func (pr *ProxyRecorder) CloseNotify() <-chan bool {
	return nil
}

func TestNewProxy_ShouldPanicOnInvalidURL(t *testing.T) {
	assert.Panics(t, func() {
		NewProxy("definitely not a u\rl")
	})
}

func TestProxy_ShouldProxy(t *testing.T) {
	ts := httptest.NewServer(http.HandlerFunc(func(res http.ResponseWriter, req *http.Request) {
		assert.Equal(t, "/ping", req.URL.Path)
		assert.Equal(t, "tvm-ticket", req.Header.Get("x-ya-service-ticket"))
		assert.NotEmpty(t, req.Host)

		body, err := ioutil.ReadAll(req.Body)

		assert.NoError(t, err)
		assert.Equal(t, "body", string(body))

		res.WriteHeader(http.StatusOK)
	}))

	defer ts.Close()

	e := echo.New()
	request := httptest.NewRequest(http.MethodGet, "/ping", strings.NewReader("body"))
	request.Header.Set("x-ya-service-ticket", "tvm-ticket")
	recorder := NewProxyRecorder()
	c := e.NewContext(request, recorder)

	handler := NewProxy(ts.URL)(controllers.Ping)

	assert.NoError(t, handler(c))
}
