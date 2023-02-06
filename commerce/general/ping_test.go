package controllers

import (
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/labstack/echo/v4"
	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/commerce/blogs_pumpkin/cache"
)

func TestPing_ShouldRespondWithPong(t *testing.T) {
	e := echo.New()
	request := httptest.NewRequest(http.MethodGet, "/ping", nil)
	recorder := httptest.NewRecorder()
	c := e.NewContext(request, recorder)

	assert.NoError(t, Ping(c))
	assert.Equal(t, http.StatusOK, recorder.Code)
	assert.Equal(t, "pong", recorder.Body.String())
}

func TestPingBlogs_ShouldRespondWithPong(t *testing.T) {
	e := echo.New()
	request := httptest.NewRequest(http.MethodGet, "/ping-blogs", nil)
	recorder := httptest.NewRecorder()
	c := NewWithCache(e.NewContext(request, recorder), cache.NewLocalCache())

	assert.NoError(t, PingBlogs(c))
	assert.Equal(t, http.StatusOK, recorder.Code)
	assert.Equal(t, "pong", recorder.Body.String())
}

func TestPingBlogs_ShouldRespondWithHappyHalloweenIfBlogsIsDead(t *testing.T) {
	e := echo.New()
	request := httptest.NewRequest(http.MethodGet, "/ping-blogs", nil)
	recorder := httptest.NewRecorder()
	localCache := cache.NewLocalCache()
	_ = localCache.SetBlogsStatus(cache.BlogsNotOK)
	c := NewWithCache(e.NewContext(request, recorder), localCache)

	assert.NoError(t, PingBlogs(c))
	assert.Equal(t, http.StatusInternalServerError, recorder.Code)
	assert.Equal(t, "Happy Halloween!", recorder.Body.String())
}
